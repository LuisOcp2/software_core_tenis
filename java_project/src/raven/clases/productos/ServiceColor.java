package raven.clases.productos;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelColor;
import raven.utils.ColorAutoHelper;

/**
 *
 * @author CrisDEV
 */

public class ServiceColor {
    
    /**
     * Obtiene todos los colores activos de la base de datos
     * @return Lista de colores disponibles
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<ModelColor> getAll2() throws SQLException {
        List<ModelColor> colores = new ArrayList<>();
        
        String sql = "SELECT id_color, nombre, codigo_hex, codigo_pantone, descripcion, activo " +
                    "FROM colores WHERE activo = 1 ORDER BY nombre";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ModelColor color = new ModelColor();
                color.setColorId(rs.getInt("id_color"));
                color.setNombre(rs.getString("nombre"));
                color.setCodigoHex(rs.getString("codigo_hex"));
                color.setCodigoPantone(rs.getString("codigo_pantone"));
                color.setDescripcion(rs.getString("descripcion"));
                color.setActivo(rs.getBoolean("activo"));
                
                colores.add(color);
            }
        }
        
        return colores;
    }
    
    /**
     * Obtiene un color por su ID
     * @param colorId ID del color
     * @return Color encontrado o null si no existe
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public ModelColor getById(int colorId) throws SQLException {
        String sql = "SELECT id_color, nombre, codigo_hex, codigo_pantone, descripcion, activo " +
                    "FROM colores WHERE id_color = ? AND activo = 1";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setInt(1, colorId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ModelColor color = new ModelColor();
                    color.setColorId(rs.getInt("id_color"));
                    color.setNombre(rs.getString("nombre"));
                    color.setCodigoHex(rs.getString("codigo_hex"));
                    color.setCodigoPantone(rs.getString("codigo_pantone"));
                    color.setDescripcion(rs.getString("descripcion"));
                    color.setActivo(rs.getBoolean("activo"));
                    return color;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Obtiene el ID de un color por su nombre (para compatibilidad con código existente)
     * @param colorName Nombre del color
     * @return ID del color o -1 si no se encuentra
     */
    public int getColorIdByName2(String colorName) throws SQLException {
        if (colorName == null || colorName.trim().isEmpty()) {
            return 1; // ID por defecto - Negro
        }
        
        String sql = "SELECT id_color FROM colores WHERE LOWER(nombre) = LOWER(?) AND activo = 1 LIMIT 1";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setString(1, colorName.trim());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_color");
                }
            }
        }
        
        // Buscar coincidencia parcial
        return findSimilarColor(colorName);
    }
    
    /**
     * Busca colores similares cuando no encuentra exacto
     */
    private int findSimilarColor(String colorName) throws SQLException {
        String sql = "SELECT id_color FROM colores WHERE LOWER(nombre) LIKE LOWER(?) AND activo = 1 LIMIT 1";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + colorName.trim() + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Color aproximado encontrado para: " + colorName);
                    return rs.getInt("id_color");
                }
            }
        }
        
        System.out.println("Advertencia: Color no encontrado - '" + colorName + "' - usando Negro (ID 1)");
        return 1; // Negro por defecto
    }
    
    /**
 * Obtiene todos los colores activos de la base de datos
 * @return Lista de todos los colores activos
 * @throws SQLException Si hay error en la consulta
 */
public List<ModelColor> getAll() throws SQLException {
    List<ModelColor> colores = new ArrayList<>();
    String sql = "SELECT id_color, nombre, codigo_hex, codigo_pantone, descripcion, activo " +
                 "FROM colores WHERE activo = 1 ORDER BY nombre";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
        
        while (rs.next()) {
            ModelColor color = new ModelColor();
            color.setColorId(rs.getInt("id_color"));
            color.setNombre(rs.getString("nombre"));
            color.setCodigoHex(rs.getString("codigo_hex"));
            color.setCodigoPantone(rs.getString("codigo_pantone"));
            color.setDescripcion(rs.getString("descripcion"));
            color.setActivo(rs.getBoolean("activo"));
            
            colores.add(color);
        }
        
        System.out.println("SUCCESS  Cargados " + colores.size() + " colores desde la base de datos");
        return colores;
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error cargando todos los colores: " + e.getMessage());
        throw e;
    }
}

/**
 * Obtiene el ID de un color por su nombre
 * @param colorName Nombre del color
 * @return ID del color en la base de datos
 * @throws SQLException Si el color no existe o hay error en BD
 */
public int getColorIdByName(String colorName) throws SQLException {
    if (colorName == null || colorName.trim().isEmpty()) {
        throw new SQLException("El nombre del color no puede estar vacío");
    }
    
    String sql = "SELECT id_color FROM colores WHERE nombre = ? AND activo = 1 LIMIT 1";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setString(1, colorName.trim());
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int idColor = rs.getInt("id_color");
                System.out.println("SUCCESS  Color encontrado: '" + colorName + "' → ID: " + idColor);
                return idColor;
            }
        }
        
        // Si no se encuentra, mostrar colores disponibles para debug
        mostrarColoresDisponibles();
        throw new SQLException("Color no encontrado en la base de datos: '" + colorName + "'");
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error obteniendo ID de color '" + colorName + "': " + e.getMessage());
        throw e;
    }
}

/**
 * Obtiene un color específico por su ID
 * @param idColor ID del color
 * @return ModelColor o null si no se encuentra
 * @throws SQLException Si hay error en la consulta
 */
public ModelColor getColorById(int idColor) throws SQLException {
    String sql = "SELECT id_color, nombre, codigo_hex, codigo_pantone, descripcion, activo " +
                 "FROM colores WHERE id_color = ? AND activo = 1";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setInt(1, idColor);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                ModelColor color = new ModelColor();
                color.setColorId(rs.getInt("id_color"));
                color.setNombre(rs.getString("nombre"));
                color.setCodigoHex(rs.getString("codigo_hex"));
                color.setCodigoPantone(rs.getString("codigo_pantone"));
                color.setDescripcion(rs.getString("descripcion"));
                color.setActivo(rs.getBoolean("activo"));
                
                return color;
            }
        }
    } catch (SQLException e) {
        System.err.println("ERROR  Error obteniendo color por ID " + idColor + ": " + e.getMessage());
        throw e;
    }
    
    return null;
}

/**
 * Verifica si existe un color con un nombre específico
 * @param colorName Nombre del color a verificar
 * @return true si existe, false si no
 */
public boolean existeColor(String colorName) {
    try {
        return getColorIdByName(colorName) > 0;
    } catch (SQLException e) {
        return false;
    }
}

/**
 * Método auxiliar para mostrar colores disponibles (útil para debug)
 */
private void mostrarColoresDisponibles() {
    try {
        String sql = "SELECT nombre FROM colores WHERE activo = 1 ORDER BY nombre";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            System.out.println(" Colores disponibles en la base de datos:");
            while (rs.next()) {
                System.out.println("   - " + rs.getString("nombre"));
            }
        }
    } catch (SQLException e) {
        System.err.println("Error mostrando colores disponibles: " + e.getMessage());
    }
}

/**
 * Crea un nuevo color en la base de datos
 * @param color ModelColor con los datos del nuevo color
 * @return true si se creó exitosamente, false en caso contrario
 * @throws SQLException Si ocurre un error en la base de datos
 */
public boolean create(ModelColor color) throws SQLException {
    // Generar automáticamente códigos hex y pantone
    Map<String, String> autoCodes = ColorAutoHelper.autoColorCodes(color.getNombre());
    String codigoHex = autoCodes.get("codigo_hex");
    String codigoPantone = autoCodes.get("codigo_pantone");
    
    String sql = "INSERT INTO colores (nombre, codigo_hex, codigo_pantone, descripcion, activo) VALUES (?, ?, ?, ?, 1)";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setString(1, color.getNombre());
        stmt.setString(2, codigoHex);
        stmt.setString(3, codigoPantone);
        stmt.setString(4, color.getDescripcion());
        
        int rowsAffected = stmt.executeUpdate();
        
        if (rowsAffected > 0) {
            System.out.println("SUCCESS  Color creado: " + color.getNombre() + 
                             " | Hex: " + codigoHex + 
                             " | Pantone: " + codigoPantone);
        }
        
        return rowsAffected > 0;
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error creando color: " + e.getMessage());
        throw e;
    }
}

/**
 * Actualiza un color existente en la base de datos
 * @param color ModelColor con los datos actualizados
 * @return true si se actualizó exitosamente, false en caso contrario
 * @throws SQLException Si ocurre un error en la base de datos
 */
public boolean update(ModelColor color) throws SQLException {
    // Generar automáticamente códigos hex y pantone actualizados
    Map<String, String> autoCodes = ColorAutoHelper.autoColorCodes(color.getNombre());
    String codigoHex = autoCodes.get("codigo_hex");
    String codigoPantone = autoCodes.get("codigo_pantone");
    
    String sql = "UPDATE colores SET nombre = ?, codigo_hex = ?, codigo_pantone = ?, descripcion = ? WHERE id_color = ? AND activo = 1";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setString(1, color.getNombre());
        stmt.setString(2, codigoHex);
        stmt.setString(3, codigoPantone);
        stmt.setString(4, color.getDescripcion());
        stmt.setInt(5, color.getColorId());
        
        int rowsAffected = stmt.executeUpdate();
        
        if (rowsAffected > 0) {
            System.out.println("SUCCESS  Color actualizado: " + color.getNombre() + 
                             " | Hex: " + codigoHex + 
                             " | Pantone: " + codigoPantone);
        }
        
        return rowsAffected > 0;
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error actualizando color: " + e.getMessage());
        throw e;
    }
}

/**
 * Elimina (desactiva) un color de la base de datos
 * @param colorId ID del color a eliminar
 * @return true si se eliminó exitosamente, false en caso contrario
 * @throws SQLException Si ocurre un error en la base de datos
 */
public boolean delete(int colorId) throws SQLException {
    String sql = "UPDATE colores SET activo = 0 WHERE id_color = ?";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setInt(1, colorId);
        
        int rowsAffected = stmt.executeUpdate();
        return rowsAffected > 0;
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error eliminando color: " + e.getMessage());
        throw e;
    }
}

/**
 * Busca colores por nombre o descripción
 * @param searchTerm Término de búsqueda
 * @return Lista de colores que coinciden con la búsqueda
 * @throws SQLException Si ocurre un error en la base de datos
 */
public List<ModelColor> search(String searchTerm) throws SQLException {
    String sql = "SELECT id_color, nombre, codigo_hex, codigo_pantone, descripcion, activo " +
                "FROM colores WHERE activo = 1 " +
                "AND (LOWER(nombre) LIKE LOWER(?) OR LOWER(descripcion) LIKE LOWER(?)) " +
                "ORDER BY nombre";
    
    List<ModelColor> colores = new ArrayList<>();
    String searchPattern = "%" + searchTerm.toLowerCase() + "%";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setString(1, searchPattern);
        stmt.setString(2, searchPattern);
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ModelColor color = new ModelColor();
                color.setColorId(rs.getInt("id_color"));
                color.setNombre(rs.getString("nombre"));
                color.setCodigoHex(rs.getString("codigo_hex"));
                color.setCodigoPantone(rs.getString("codigo_pantone"));
                color.setDescripcion(rs.getString("descripcion"));
                color.setActivo(rs.getBoolean("activo"));
                
                colores.add(color);
            }
        }
    } catch (SQLException e) {
        System.err.println("ERROR  Error buscando colores: " + e.getMessage());
        throw e;
    }
    
    return colores;
}
}
