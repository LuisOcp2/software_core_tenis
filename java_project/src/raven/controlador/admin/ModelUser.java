package raven.controlador.admin;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.principal.conexion;

public class ModelUser implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int idUsuario;
    private String username;
    private String password; // En un entorno real, deberías almacenar solo hashes de contraseñas
    private String nombre;
    private String email;
    private String rol;
    private String ubicacion;
    private Integer idBodega;
    private boolean activo;
    
    // ===================================================================
    // CONSTRUCTORES
    // ===================================================================
    
    // Constructor original (MANTENER COMPATIBILIDAD - con password)
    public ModelUser(int idUsuario, String username, String password, String nombre, String email, String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.ubicacion = "tienda"; // Valor por defecto para compatibilidad
        this.activo = activo;
    }
    
    // Constructor original (MANTENER COMPATIBILIDAD - sin password)
    public ModelUser(int idUsuario, String username, String nombre, String email, String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.password = null;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.ubicacion = "tienda"; // Valor por defecto para compatibilidad
        this.activo = activo;
    }
    
    // NUEVO Constructor con ubicación (con password)
    public ModelUser(int idUsuario, String username, String password, String nombre, String email, String rol, String ubicacion, Integer idBodega, boolean activo) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.ubicacion = ubicacion;
        this.idBodega = idBodega;
        this.activo = activo;
    }
    
  
    // Constructor vacío
    public ModelUser() {
        this.ubicacion = "tienda"; // Valor por defecto
        this.password = null;
    }
    
    // ===================================================================
    // GETTERS Y SETTERS BÁSICOS
    // ===================================================================
    
    public int getIdUsuario() {
        return idUsuario;
    }
    
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRol() {
        return rol;
    }
    
    public void setRol(String rol) {
        this.rol = rol;
    }
    
    public String getUbicacion() {
        return ubicacion;
    }
    
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
    
    public boolean isActivo() {
        return activo;
    }
    
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Integer getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }
    
    // ===================================================================
    // MÉTODOS REQUERIDOS POR SessionManager
    // ===================================================================
    

    
    /**
     * Verifica si el usuario es de tienda
     * REQUERIDO POR SessionManager.isCurrentUserFromStore() línea 89
     */
    public boolean esDeTienda() {
        return "tienda".equals(ubicacion);
    }
    
    /**
     * Verifica si el usuario es de bodega
     * REQUERIDO POR SessionManager.isCurrentUserFromWarehouse() línea 91
     */
    public boolean esDeBodega() {
        return "bodega".equals(ubicacion);
    }
    
    // ===================================================================
    // MÉTODOS HELPER PARA FILTROS (Para ServiceDashboardStats)
    // ===================================================================
    

    

    
    /**
     * Verifica si el usuario es administrador
     */
    public boolean esAdmin() {
        return "admin".equals(this.rol);
    }
    

    

    

    
    /**
     * Obtiene información completa del usuario para logs
     */
    public String getInfoCompleta() {
        return String.format("%s (%s) - %s", 
            nombre, username, rol.toUpperCase());
    }
    
    // ===================================================================
    // MÉTODOS PARA TABLAS Y DISPLAY
    // ===================================================================
    
    /**
     * Método toTableRow actualizado para incluir ubicación
     */
    public Object[] toTableRow(int rowNum) {
        // Obtener el nombre de la bodega
        String nombreBodega = "";

        // Solo buscar el nombre de la bodega si idBodega no es null
        if (idBodega != null && idBodega > 0) {
            try {
                // Obtener la instancia de conexión y asegurarse de que esté conectada
                conexion dbConnection = conexion.getInstance();
                try {
                    dbConnection.connectToDatabase();
                } catch (SQLException ex) {
                    // Si ya está conectado, ignoramos la excepción
                }

                // Realizar la consulta
                try (Connection conn = dbConnection.createConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT nombre FROM bodegas WHERE id_bodega = ?")) {

                    stmt.setInt(1, idBodega);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        nombreBodega = rs.getString("nombre");
                    }

                }
            } catch (SQLException ex) {
                Logger.getLogger(ModelUser.class.getName()).log(Level.SEVERE, null, ex);
                nombreBodega = "Error al cargar";
            }
        } else {
            nombreBodega = "Sin bodega asignada";
        }

        return new Object[]{
            false, // Checkbox para selección
            idUsuario, // ID del usuario
            username, // Nombre de usuario
            nombre, // Nombre completo
            email, // Correo electrónico
            rol, // Rol del usuario
            nombreBodega, // Nombre de la bodega
            activo ? "Activo" : "Inactivo" // Estado del usuario mostrado como texto
        };
    }

    /**
     * Método toTableRow optimizado para incluir ubicación sin realizar consultas individuales
     */
    public Object[] toTableRowOptimizado(int rowNum, java.util.Map<Integer, String> bodegaNombres) {
        // Obtener el nombre de la bodega desde el mapa pre-cargado
        String nombreBodega = "Sin bodega asignada";

        if (idBodega != null && idBodega > 0) {
            nombreBodega = bodegaNombres.getOrDefault(idBodega, "Bodega no encontrada");
        }

        return new Object[]{
            false, // Checkbox para selección
            idUsuario, // ID del usuario
            username, // Nombre de usuario
            nombre, // Nombre completo
            email, // Correo electrónico
            rol, // Rol del usuario
            nombreBodega, // Nombre de la bodega
            activo ? "Activo" : "Inactivo" // Estado del usuario mostrado como texto
        };
    }
    
    // ===================================================================
    // MÉTODOS OVERRIDE
    // ===================================================================
    
    /**
     * Método toString actualizado para debugging
     */
    @Override
    public String toString() {
        return String.format("ModelUser{idUsuario=%d, username='%s', nombre='%s', rol='%s', ubicacion='%s', activo=%s}", 
                           idUsuario, username, nombre, rol, ubicacion, activo);
    }
    
    /**
     * Método equals actualizado para incluir ubicación
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ModelUser modelUser = (ModelUser) obj;
        return idUsuario == modelUser.idUsuario &&
               activo == modelUser.activo &&
               (username != null ? username.equals(modelUser.username) : modelUser.username == null) &&
               (email != null ? email.equals(modelUser.email) : modelUser.email == null) &&
               (rol != null ? rol.equals(modelUser.rol) : modelUser.rol == null) &&
               (ubicacion != null ? ubicacion.equals(modelUser.ubicacion) : modelUser.ubicacion == null);
    }
    
    /**
     * Método hashCode actualizado
     */
    @Override
    public int hashCode() {
        int result = idUsuario;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (rol != null ? rol.hashCode() : 0);
        result = 31 * result + (ubicacion != null ? ubicacion.hashCode() : 0);
        result = 31 * result + (activo ? 1 : 0);
        return result;
    }
}
