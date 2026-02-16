package raven.controlador.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import raven.clases.admin.ServiceUser;
import raven.controlador.principal.conexion;

/**
 * Gestor de sesión de usuario para mantener información del usuario actual
 *
 * @author CrisDEV
 */
public class SessionManager {
    
    // ===================================================================
    // SINGLETON PATTERN
    // ===================================================================
    private static SessionManager instance;
    private ModelUser currentUser;
    private boolean sessionActive;
    
    private SessionManager() {
        this.sessionActive = false;
        this.currentUser = null;
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }
    
    // ===================================================================
    // MÉTODOS DE GESTIÓN DE SESIÓN
    // ===================================================================
    
    /**
     * Inicia sesión con las credenciales del usuario
     */
    public boolean login(String username, String password) {
        try {
            ModelUser user = authenticateUser(username, password);
            if (user != null) {
                this.currentUser = user;
                this.sessionActive = true;
                System.out.println("SUCCESS  Sesión iniciada para: " + user.getNombre() + " (" + user.getUbicacion() + ")");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR  Error durante autenticación: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Cierra la sesión actual
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println(" Cerrando sesión de: " + currentUser.getNombre());
        }
        this.currentUser = null;
        this.sessionActive = false;
    }
    
    /**
     * Verifica si hay una sesión activa
     */
    public boolean isSessionActive() {
        return sessionActive && currentUser != null;
    }
    
    /**
     * Obtiene el usuario actual
     */
    public ModelUser getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Obtiene el ID del usuario actual
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getIdUsuario() : -1;
    }
    
    /**
     * Obtiene la ubicación del usuario actual
     */
    public String getCurrentUserLocation() {
        return currentUser != null ? currentUser.getUbicacion() : null;
    }
    
    /**
     * Obtiene el ID de bodega del usuario actual
     */
    public Integer getCurrentUserBodegaId() {
        return currentUser != null ? currentUser.getIdBodega() : null;
    }
    
    /**
     * Verifica si el usuario actual es de tienda
     */
    public boolean isCurrentUserFromStore() {
        return currentUser != null && currentUser.esDeTienda();
    }
    
    /**
     * Verifica si el usuario actual es de bodega
     */
    public boolean isCurrentUserFromWarehouse() {
        return currentUser != null && currentUser.esDeBodega();
    }
    
    /**
     * Obtiene el rol del usuario actual
     */
    public String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRol() : null;
    }
    
    /**
     * Verifica si el usuario actual tiene un rol específico
     */
    public boolean hasRole(String role) {
        return currentUser != null && role.equalsIgnoreCase(currentUser.getRol());
    }
    
    /**
     * Verifica si el usuario actual es administrador
     */
    public boolean isCurrentUserAdmin() {
        return hasRole("admin");
    }
    
    // ===================================================================
    // MÉTODOS PRIVADOS DE AUTENTICACIÓN
    // ===================================================================
    
    /**
     * Autentica un usuario contra la base de datos
     */
    private ModelUser authenticateUser(String username, String password) throws SQLException {
        ServiceUser serviceUser = new ServiceUser();
        return serviceUser.authenticate(username, password);
    }
    
    // ===================================================================
    // MÉTODOS DE UTILIDAD PARA DEBUGGING
    // ===================================================================
    
    /**
     * Información de la sesión actual para debugging
     */
    public String getSessionInfo() {
        if (!isSessionActive()) {
            return "No hay sesión activa";
        }
        
        return String.format("Usuario: %s | Rol: %s | Ubicación: %s | ID: %d",
                           currentUser.getNombre(),
                           currentUser.getRol(),
                           currentUser.getUbicacion(),
                           currentUser.getIdUsuario());
    }
    
    /**
     * Método temporal para simular login (SOLO PARA DESARROLLO)
     */
    public void loginForDevelopment(int userId) {
        try {
            String sql = "SELECT id_usuario, username, nombre, email, rol, ubicacion, id_bodega, activo " +
                        "FROM usuarios WHERE id_usuario = ? AND activo = 1";

            try (Connection con = conexion.getInstance().createConnection();
                 PreparedStatement stmt = con.prepareStatement(sql)) {

                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Integer idBodega = rs.getInt("id_bodega");
                        if (rs.wasNull() || idBodega == null || idBodega <= 0) {
                            // Fallback: tomar cualquier bodega válida existente
                            idBodega = fetchDefaultBodegaId(con);
                        }

                        this.currentUser = new ModelUser(
                            rs.getInt("id_usuario"),
                            rs.getString("username"),
                            null,
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("rol"),
                            rs.getString("ubicacion"),
                            idBodega,
                            rs.getBoolean("activo")
                        );
                        this.sessionActive = true;
                        java.util.logging.Logger.getLogger(SessionManager.class.getName())
                                .log(java.util.logging.Level.INFO, "[DEV] Sesion simulada para: " + getSessionInfo() + " | Bodega: " + idBodega);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR  Error en login de desarrollo: " + e.getMessage());
        }
    }

    /**
     * Obtiene una bodega válida para usar como fallback.
     */
    private Integer fetchDefaultBodegaId(Connection con) {
        String q = "SELECT id_bodega FROM bodegas ORDER BY id_bodega ASC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("WARNING  No fue posible obtener bodega por defecto: " + ex.getMessage());
        }
        return null; // retornará null si no hay bodegas
    }
}
