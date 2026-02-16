package raven.clases.admin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.admin.ModelSession;
import raven.controlador.admin.ModelSession.EstadoSesion;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.conexion;

/**
 * Servicio de gestión de sesiones con control de concurrencia.
 * 
 * Implementa:
 * - Control de sesiones concurrentes (una sesión activa por usuario)
 * - Ping automático para keepalive
 * - Limpieza de sesiones expiradas
 * - Integración con sistema de cajas
 * 
 * @author CrisDEV
 */
public class ServiceSession {
    private static volatile String CACHED_HOSTNAME;
    private static volatile String CACHED_IP;
    private static final int QUERY_TIMEOUT_SECONDS = 3;
    
    // ===================================================================
    // CONSTANTES DE CONFIGURACIÓN
    // ===================================================================
    
    private static final int MINUTOS_INACTIVIDAD = 30; // Timeout de sesión
    private static final int INTERVALO_PING_SEGUNDOS = 300; // Ping cada 60 segundos
    
    // ===================================================================
    // MÉTODOS CRUD
    // ===================================================================
    
    /**
     * Crea una nueva sesión para un usuario.
     * 
     * IMPORTANTE: Si el usuario ya tiene una sesión activa, la cierra.
     * 
     * @param user Usuario que inicia sesión
     * @return Sesión creada con token
     * @throws SQLException Si hay error en BD
     */
    public ModelSession crearSesion(ModelUser user) throws SQLException {
        Connection con = null;
        
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);
            
            // 1. Cerrar sesiones previas del usuario
            cerrarSesionesAnteriores(con, user.getIdUsuario());
            
            // 2. Generar token único
            String token = SessionToken.generate(user.getIdUsuario());
            
            // 3. Obtener información del sistema
            String hostname = obtenerHostname();
            String ipAddress = obtenerIPAddress();
            
            // 4. Crear modelo de sesión
            ModelSession sesion = new ModelSession(user.getIdUsuario(), token);
            sesion.setHostname(hostname);
            sesion.setIpAddress(ipAddress);
            sesion.setSistemaOperativo(System.getProperty("os.name"));
            sesion.setNavegador("Java Swing Application");
            
            // 5. Insertar en BD
            String sql = "INSERT INTO sesiones_activas " +
                        "(id_usuario, token_sesion, ip_address, hostname, " +
                        "fecha_inicio, fecha_ultimo_ping, estado, navegador, sistema_operativo) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, sesion.getIdUsuario());
                ps.setString(2, sesion.getTokenSesion());
                ps.setString(3, sesion.getIpAddress());
                ps.setString(4, sesion.getHostname());
                ps.setTimestamp(5, Timestamp.valueOf(sesion.getFechaInicio()));
                ps.setTimestamp(6, Timestamp.valueOf(sesion.getFechaUltimoPing()));
                ps.setString(7, sesion.getEstado().getCodigo());
                ps.setString(8, sesion.getNavegador());
                ps.setString(9, sesion.getSistemaOperativo());
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                
                ps.executeUpdate();
                
                // Obtener ID generado
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        sesion.setIdSesion(rs.getInt(1));
                    }
                }
            }
            
            // 6. Registrar en historial
            registrarEnHistorial(con, sesion.getIdSesion(), user.getIdUsuario(), "login", null);
            
            con.commit();
            
            System.out.println("SUCCESS  Sesión creada: " + sesion);
            return sesion;
            
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Obtiene una sesión activa por token.
     */
    public ModelSession obtenerSesionPorToken(String token) throws SQLException {
        String sql = "SELECT * FROM sesiones_activas WHERE token_sesion = ? AND estado = 'activa'";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, token);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSetASesion(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Actualiza el ping de una sesión (keepalive).
     */
    public void actualizarPing(String token) throws SQLException {
        String sql = "UPDATE sesiones_activas SET fecha_ultimo_ping = ? WHERE token_sesion = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, token);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Actualizando Ping actualizado para token: " + token.substring(0, 10) + "...");
            }
        }
    }
    
    /**
     * Asocia una caja abierta a la sesión.
     */
    public void asociarCaja(String token, int idCaja, int idMovimiento) throws SQLException {
        String sql = "UPDATE sesiones_activas SET id_caja_abierta = ?, id_movimiento_caja = ? " +
                    "WHERE token_sesion = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idCaja);
            ps.setInt(2, idMovimiento);
            ps.setString(3, token);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            
            ps.executeUpdate();
            
            System.out.println("SUCCESS  Caja asociada a sesión - Movimiento: " + idMovimiento);
        }
    }
    
    /**
     * Cierra una sesión específica.
     */
    public void cerrarSesion(String token) throws SQLException {
        Connection con = null;
        
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);
            
            // 1. Obtener sesión actual
            ModelSession sesion = obtenerSesionPorToken(token);
            
            if (sesion == null) {
                System.out.println("WARNING  Sesión no encontrada para cerrar");
                return;
            }
            
            // 2. Actualizar estado
            String sql = "UPDATE sesiones_activas SET estado = 'cerrada' WHERE token_sesion = ?";
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, token);
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                ps.executeUpdate();
            }
            
            // 3. Registrar en historial
            registrarEnHistorial(con, sesion.getIdSesion(), sesion.getIdUsuario(), "logout", null);
           
            con.commit();
            
            System.out.println("SUCCESS  Sesión cerrada: " + token.substring(0, 10) + "...");
            
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Verifica si un usuario tiene una sesión activa desde otra máquina.
     */
    public boolean tieneOtraSesionActiva(int idUsuario, String tokenActual) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sesiones_activas " +
                    "WHERE id_usuario = ? AND token_sesion != ? AND estado = 'activa'";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idUsuario);
            ps.setString(2, tokenActual);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }

    public boolean tieneSesionesActivas(int idUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sesiones_activas WHERE id_usuario = ? AND estado = 'activa'";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Limpia sesiones expiradas automáticamente.
     */
    public int limpiarSesionesExpiradas() throws SQLException {
        Connection con = null;
        int sesionesLimpiadas = 0;
        
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);
            
            // 1. Obtener sesiones expiradas
            String sqlSelect = "SELECT id_sesion, id_usuario FROM sesiones_activas " +
                              "WHERE estado = 'activa' AND fecha_ultimo_ping < ?";
            
            LocalDateTime limiteExpiracion = LocalDateTime.now().minusMinutes(MINUTOS_INACTIVIDAD);
            List<int[]> sesionesExpiradas = new ArrayList<>();
            
            try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                ps.setTimestamp(1, Timestamp.valueOf(limiteExpiracion));
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        sesionesExpiradas.add(new int[]{rs.getInt(1), rs.getInt(2)});
                    }
                }
            }
            
            // 2. Cerrar sesiones expiradas
            if (!sesionesExpiradas.isEmpty()) {
                String sqlUpdate = "UPDATE sesiones_activas SET estado = 'cerrada' WHERE id_sesion = ?";
                
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    for (int[] sesion : sesionesExpiradas) {
                        ps.setInt(1, sesion[0]);
                        ps.addBatch();
                        
                        // Registrar en historial
                        registrarEnHistorial(con, sesion[0], sesion[1], "timeout", 
                            "Sesión cerrada por inactividad");
                    }
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    int[] results = ps.executeBatch();
                    sesionesLimpiadas = results.length;
                }
            }
            
            con.commit();
            
            if (sesionesLimpiadas > 0) {
                System.out.println("INFO Limpiadas " + sesionesLimpiadas + " sesiones expiradas");
            }
            
            return sesionesLimpiadas;
            
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // ===================================================================
    // MÉTODOS PRIVADOS HELPER
    // ===================================================================
    
    private void cerrarSesionesAnteriores(Connection con, int idUsuario) throws SQLException {
        String sql = "UPDATE sesiones_activas SET estado = 'cerrada' " +
                    "WHERE id_usuario = ? AND estado = 'activa'";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("INFO Cerradas " + rowsAffected + " sesiones anteriores del usuario");
            }
        }
    }
    
    private void registrarEnHistorial(Connection con, int idSesion, int idUsuario, 
                                      String accion, String detalles) throws SQLException {
        String sql = "INSERT INTO historial_sesiones (id_sesion, id_usuario, accion, detalles) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSesion);
            ps.setInt(2, idUsuario);
            ps.setString(3, accion);
            ps.setString(4, detalles);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.executeUpdate();
        }
    }
    
    private ModelSession mapearResultSetASesion(ResultSet rs) throws SQLException {
        ModelSession sesion = new ModelSession();
        
        sesion.setIdSesion(rs.getInt("id_sesion"));
        sesion.setIdUsuario(rs.getInt("id_usuario"));
        sesion.setTokenSesion(rs.getString("token_sesion"));
        sesion.setIpAddress(rs.getString("ip_address"));
        sesion.setHostname(rs.getString("hostname"));
        sesion.setFechaInicio(rs.getTimestamp("fecha_inicio").toLocalDateTime());
        sesion.setFechaUltimoPing(rs.getTimestamp("fecha_ultimo_ping").toLocalDateTime());
        
        // Campos opcionales
        int idCaja = rs.getInt("id_caja_abierta");
        if (!rs.wasNull()) {
            sesion.setIdCajaAbierta(idCaja);
        }
        
        int idMovimiento = rs.getInt("id_movimiento_caja");
        if (!rs.wasNull()) {
            sesion.setIdMovimientoCaja(idMovimiento);
        }
        
        sesion.setEstado(EstadoSesion.fromCodigo(rs.getString("estado")));
        sesion.setNavegador(rs.getString("navegador"));
        sesion.setSistemaOperativo(rs.getString("sistema_operativo"));
        
        return sesion;
    }
    
    private String obtenerHostname() {
        if (CACHED_HOSTNAME != null) return CACHED_HOSTNAME;
        try {
            CACHED_HOSTNAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            CACHED_HOSTNAME = "UNKNOWN";
        }
        return CACHED_HOSTNAME;
    }
    
    private String obtenerIPAddress() {
        if (CACHED_IP != null) return CACHED_IP;
        try {
            CACHED_IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            CACHED_IP = "0.0.0.0";
        }
        return CACHED_IP;
    }
}

