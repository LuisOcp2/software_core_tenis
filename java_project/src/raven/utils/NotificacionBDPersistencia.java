package raven.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.principal.conexion;
import raven.interfaces.NotificacionPersistente;

/**
 * Implementación de persistencia de notificaciones en base de datos.
 * Aplica principio DIP (Dependency Inversion Principle).
 */
public class NotificacionBDPersistencia implements NotificacionPersistente {
    
    private static final Logger LOGGER = Logger.getLogger(NotificacionBDPersistencia.class.getName());
    private static final int LOGIN_DEDUPE_WINDOW_MINUTES = 5;

    @Override
    public boolean guardarNotificacion(String mensaje, int tipo, int idReferencia) {
        try (Connection con = conexion.getInstance().createConnection()) {
            // Mapear tipo numérico a ENUM y generar título
            String tipoEnum;
            String titulo;
            switch (tipo) {
                case 1: // ventas antiguas
                    tipoEnum = "warning";
                    titulo = "Venta Pendiente";
                    break;
                case 2: // conteos pendientes
                    tipoEnum = "warning";
                    titulo = "Conteo Pendiente";
                    break;
                case 3:
                    tipoEnum = "error";
                    titulo = "Error del Sistema";
                    break;
                case 4:
                    tipoEnum = "success";
                    titulo = "Operación Exitosa";
                    break;
                case 5:
                    tipoEnum = "urgent";
                    titulo = "Aviso Urgente";
                    break;
                default:
                    tipoEnum = "info";
                    titulo = "Notificación";
            }
            // Verificamos si ya existe una notificación similar no leída
            String checkSql = "SELECT COUNT(*) FROM notificaciones WHERE mensaje LIKE ? AND tipo = ? " +
                             "AND id_referencia = ? AND leida = 0 AND activa = 1";
            
            int count = 0;
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                // Usamos patrón para buscar similaridad en mensajes
                checkStmt.setString(1, "%" + mensaje.substring(0, Math.min(20, mensaje.length())) + "%");
                checkStmt.setString(2, tipoEnum);
                checkStmt.setInt(3, idReferencia);
                
                ResultSet rs = checkStmt.executeQuery();
                // Mover a la primera fila antes de leer
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            
            // Si ya existe una notificación similar, no creamos otra
            if (count > 0) {
                return true;
            }
            
            // Insertamos nueva notificación
            String sql = "INSERT INTO notificaciones (titulo, mensaje, tipo, id_referencia) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, titulo);
                stmt.setString(2, mensaje);
                stmt.setString(3, tipoEnum);
                stmt.setInt(4, idReferencia);
                
                return stmt.executeUpdate() > 0;
            }
            
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al guardar notificación en base de datos", ex);
            return false;
        }
    }

    @Override
    public boolean marcarComoLeida(int idNotificacion) {
        try (Connection con = conexion.getInstance().createConnection()) {
            String sql = "UPDATE notificaciones SET leida = 1 WHERE id_notificacion = ?";
            
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, idNotificacion);
                return stmt.executeUpdate() > 0;
            }
            
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al marcar notificación como leída", ex);
            return false;
        }
    }

    @Override
    public boolean eliminarNotificacion(int idNotificacion) {
        try (Connection con = conexion.getInstance().createConnection()) {
            // Realizamos borrado lógico
            String sql = "UPDATE notificaciones SET activa = 0 WHERE id_notificacion = ?";
            
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, idNotificacion);
                return stmt.executeUpdate() > 0;
            }
            
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al eliminar notificación", ex);
            return false;
        }
    }

    @Override
    public boolean guardarNotificacionDetallada(String titulo,
                                                 String mensaje,
                                                 String tipoEnum,
                                                 String categoria,
                                                 Integer idUsuarioDestinatario,
                                                 Boolean paraTodos,
                                                 Integer idReferencia,
                                                 String tipoReferencia) {
        try (Connection con = conexion.getInstance().createConnection()) {
            String tipoRef = tipoReferencia != null ? tipoReferencia.trim() : "";
            if (idUsuarioDestinatario != null && "login".equalsIgnoreCase(tipoRef)) {
                String mensajeNorm = mensaje != null ? mensaje.trim() : "";
                Integer idExistente = null;

                String buscarSql = "SELECT id_notificacion FROM notificaciones "
                        + "WHERE activa = 1 AND (leida = 0 OR leida IS NULL) "
                        + "AND id_usuario_destinatario = ? "
                        + "AND evento = 'login' "
                        + "AND mensaje = ? "
                        + "AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL ? MINUTE) "
                        + "ORDER BY fecha_creacion DESC LIMIT 1";
                try (PreparedStatement ps = con.prepareStatement(buscarSql)) {
                    ps.setInt(1, idUsuarioDestinatario);
                    ps.setString(2, mensajeNorm);
                    ps.setInt(3, LOGIN_DEDUPE_WINDOW_MINUTES);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            idExistente = rs.getInt("id_notificacion");
                        }
                    }
                }

                if (idExistente != null) {
                    String updateSql = "UPDATE notificaciones SET titulo = ?, mensaje = ?, tipo = ?, categoria = ?, para_todos = ?, "
                            + "fecha_creacion = NOW(), activa = 1 "
                            + "WHERE id_notificacion = ?";
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setString(1, titulo != null ? titulo : "Notificación");
                        ps.setString(2, mensajeNorm);
                        ps.setString(3, tipoEnum != null ? tipoEnum : "info");
                        ps.setString(4, categoria != null ? categoria : "sistema");
                        ps.setBoolean(5, paraTodos != null ? paraTodos : false);
                        ps.setInt(6, idExistente);
                        ps.executeUpdate();
                    }

                    String desactivarSql = "UPDATE notificaciones SET activa = 0 "
                            + "WHERE activa = 1 AND (leida = 0 OR leida IS NULL) "
                            + "AND id_usuario_destinatario = ? "
                            + "AND evento = 'login' "
                            + "AND mensaje = ? "
                            + "AND id_notificacion <> ? "
                            + "AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
                    try (PreparedStatement ps = con.prepareStatement(desactivarSql)) {
                        ps.setInt(1, idUsuarioDestinatario);
                        ps.setString(2, mensajeNorm);
                        ps.setInt(3, idExistente);
                        ps.setInt(4, LOGIN_DEDUPE_WINDOW_MINUTES);
                        ps.executeUpdate();
                    }
                    return true;
                }

                String insertSql = "INSERT INTO notificaciones (titulo, mensaje, tipo, categoria, id_usuario_destinatario, para_todos, id_referencia, tipo_referencia, evento) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'login')";
                try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                    stmt.setString(1, titulo != null ? titulo : "Notificación");
                    stmt.setString(2, mensajeNorm);
                    stmt.setString(3, tipoEnum != null ? tipoEnum : "info");
                    stmt.setString(4, categoria != null ? categoria : "sistema");
                    stmt.setInt(5, idUsuarioDestinatario);
                    stmt.setBoolean(6, paraTodos != null ? paraTodos : false);
                    stmt.setInt(7, idReferencia != null ? idReferencia : 0);
                    stmt.setString(8, tipoReferencia);
                    return stmt.executeUpdate() > 0;
                }
            }

            // Evitar duplicados por referencia
            String checkSql = "SELECT COUNT(*) FROM notificaciones WHERE tipo_referencia = ? AND id_referencia = ? "
                    + "AND leida = 0 AND activa = 1";
            int count = 0;
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                checkStmt.setString(1, tipoReferencia);
                checkStmt.setInt(2, idReferencia != null ? idReferencia : 0);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) count = rs.getInt(1);
                }
            }
            if (count > 0) return true;

            String sql = "INSERT INTO notificaciones (titulo, mensaje, tipo, categoria, id_usuario_destinatario, para_todos, id_referencia, tipo_referencia) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, titulo != null ? titulo : "Notificación");
                stmt.setString(2, mensaje);
                stmt.setString(3, tipoEnum != null ? tipoEnum : "info");
                stmt.setString(4, categoria != null ? categoria : "sistema");
                if (idUsuarioDestinatario != null) stmt.setInt(5, idUsuarioDestinatario); else stmt.setNull(5, java.sql.Types.INTEGER);
                stmt.setBoolean(6, paraTodos != null ? paraTodos : true);
                stmt.setInt(7, idReferencia != null ? idReferencia : 0);
                stmt.setString(8, tipoReferencia);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al guardar notificación detallada en base de datos", ex);
            return false;
        }
    }
}
