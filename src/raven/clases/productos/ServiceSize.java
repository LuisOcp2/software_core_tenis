package raven.clases.productos;

/**
 *
 * @author CrisDEV
 */

import java.sql.*;
import java.sql.Connection;
import java.sql.PreparedStatement;  
import java.sql.ResultSet;
import java.sql.SQLException;
import raven.controlador.principal.conexion;
import java.util.List;
import java.util.ArrayList;
import raven.controlador.productos.ModelSize;

public class ServiceSize {
    
    /**
     * Obtiene el ID de una talla por su nombre
     * @param sizeName Nombre de la talla (ej: "40 EU H")
     * @return ID de la talla o -1 si no se encuentra
     */
    public int getSizeIdByName2(String sizeName) throws SQLException {
        String sql = "SELECT id_talla FROM tallas WHERE numero = ? AND activo = 1";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            // Extraer solo el número de la talla (ej: "40" de "40 EU H")
            String numeroTalla = extractSizeNumber(sizeName);
            stmt.setString(1, numeroTalla);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_talla");
                }
            }
        }
        
        // Si no se encuentra, intentar buscar por el nombre completo
        return findOrCreateSize(sizeName);
    }
    
    /**
 * Obtiene el ID de una talla por su nombre
 * @param sizeName Nombre de la talla como "38 EU M" o solo "38"
 * @return ID de la talla en la base de datos
 * @throws SQLException Si la talla no existe o hay error en BD
 */
public int getSizeIdByName(String sizeName) throws SQLException {
    if (sizeName == null || sizeName.trim().isEmpty()) {
        throw new SQLException("El nombre de la talla no puede estar vacío");
    }
    
    // SQL que busca por el formato completo o solo por número
    String sql = "   SELECT id_talla\n" +
            "   FROM tallas\n" +
            "   WHERE (CONCAT(numero, ' ', sistema, ' ', LEFT(genero, 1)) = ?\n" + 
            "          OR numero = ?)\n" + 
            "          AND activo = 1\n" + 
            "   LIMIT 1\n";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        String sizeNameTrimmed = sizeName.trim();
        stmt.setString(1, sizeNameTrimmed);  // Buscar formato completo "38 EU M"
        
        // Extraer solo el número para segunda búsqueda
        String numeroSolo = sizeNameTrimmed.split(" ")[0]; // "38"
        stmt.setString(2, numeroSolo);
        
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            int idTalla = rs.getInt("id_talla");
            System.out.println("SUCCESS  Talla encontrada: '" + sizeName + "' → ID: " + idTalla);
            return idTalla;
        }
        
        // Si no se encuentra, mostrar tallas disponibles para debug
        mostrarTallasDisponibles();
        throw new SQLException("Talla no encontrada en la base de datos: '" + sizeName + "'");
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error obteniendo ID de talla '" + sizeName + "': " + e.getMessage());
        throw e;
    }
}
/**
 * Método para verificar si una talla existe
 * @param sizeName Nombre de la talla
 * @return true si existe, false si no existe
 */
public boolean existeTalla(String sizeName) {
    try {
        getSizeIdByName(sizeName);
        return true;
    } catch (SQLException e) {
        return false;
    }
}
   /**
 * Método auxiliar para mostrar tallas disponibles (útil para debug)
 */
private void mostrarTallasDisponibles() {
    try {
        String sql = "SELECT numero, sistema, genero FROM tallas WHERE activo = 1 ORDER BY numero";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            System.out.println(" Tallas disponibles en la base de datos:");
            
            while (rs.next()) {
                String talla = rs.getString("numero") + " " + 
                             rs.getString("sistema") + " " + 
                             (rs.getString("genero") != null ? rs.getString("genero").substring(0, 1) : "");
                System.out.println("   - " + talla.trim());
            }
            
        }
    } catch (SQLException e) {
        System.err.println("Error mostrando tallas disponibles: " + e.getMessage());
    }
} 
    /**
     * Extrae el número de talla del formato "40 EU H"
     */
    private String extractSizeNumber(String fullSizeName) {
        if (fullSizeName == null) return "";
        
        // Buscar el primer espacio y tomar todo antes de él
        int spaceIndex = fullSizeName.indexOf(' ');
        if (spaceIndex > 0) {
            return fullSizeName.substring(0, spaceIndex);
        }
        
        return fullSizeName; // Si no hay espacio, devolver todo
    }
    
    /**
     * Busca o crea una talla si no existe
     */
    private int findOrCreateSize(String sizeName) throws SQLException {
        System.out.println("Advertencia: Talla no encontrada en BD: " + sizeName);
        // Por ahora devolver un ID por defecto
        // En el futuro podrías implementar creación automática de tallas
        return 1; // ID de talla por defecto
    }
    
    /**
 * Obtiene todas las tallas activas de la base de datos
 * @return Lista de todas las tallas activas
 * @throws SQLException Si hay error en la consulta
 */
public List<ModelSize> getAll() throws SQLException {
    List<ModelSize> tallas = new ArrayList<>();
    String sql = "SELECT id_talla, numero, sistema, equivalencia_eu, equivalencia_us, " +
                 "equivalencia_uk, equivalencia_cm, genero, activo " +
                 "FROM tallas WHERE activo = 1 ORDER BY genero, numero";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
        
        while (rs.next()) {
            ModelSize talla = new ModelSize();
            talla.setIdTalla(rs.getInt("id_talla"));
            talla.setNumero(rs.getString("numero"));
            talla.setSistema(rs.getString("sistema"));
            
            // Manejar posibles valores null en las equivalencias
            talla.setEquivalenciaEu(rs.getObject("equivalencia_eu") != null ? rs.getDouble("equivalencia_eu") : null);
            talla.setEquivalenciaUs(rs.getObject("equivalencia_us") != null ? rs.getDouble("equivalencia_us") : null);
            talla.setEquivalenciaUk(rs.getObject("equivalencia_uk") != null ? rs.getDouble("equivalencia_uk") : null);
            talla.setEquivalenciaCm(rs.getObject("equivalencia_cm") != null ? rs.getDouble("equivalencia_cm") : null);
            
            talla.setGenero(rs.getString("genero"));
            talla.setActivo(rs.getBoolean("activo"));
            
            tallas.add(talla);
        }
        
        System.out.println("SUCCESS  Cargadas " + tallas.size() + " tallas desde la base de datos");
        return tallas;
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error cargando todas las tallas: " + e.getMessage());
        throw e;
    }
}
/**
 * Obtiene tallas filtradas por género
 * @param genero Género a filtrar (MUJER, HOMBRE, NIÑO, etc.)
 * @return Lista de tallas para el género especificado
 * @throws SQLException Si hay error en la consulta
 */
public List<ModelSize> getTallasByGenero(String genero) throws SQLException {
    List<ModelSize> tallas = new ArrayList<>();
    
    if (genero == null || genero.trim().isEmpty() || genero.equals("SELECCIONAR")) {
        return tallas; // Retornar lista vacía
    }
    
    // Si es UNISEX, cargar tallas de HOMBRE y MUJER
    if (genero.equalsIgnoreCase("UNISEX")) {
        try {
            // Primero cargamos las tallas de HOMBRE
            List<ModelSize> tallasHombre = getTallasByGenero("HOMBRE");
            tallas.addAll(tallasHombre);
            
            // Luego cargamos las tallas de MUJER
            List<ModelSize> tallasMujer = getTallasByGenero("MUJER");
            tallas.addAll(tallasMujer);
            
            System.out.println("SUCCESS  Cargadas " + tallas.size() + " tallas para género UNISEX (combinación de HOMBRE y MUJER)");
            return tallas;
        } catch (SQLException e) {
            System.err.println("ERROR  Error cargando tallas para UNISEX: " + e.getMessage());
            throw e;
        }
    }
    
    // Para los demás géneros, seguir con la lógica original
    String sql = "SELECT id_talla, numero, sistema, equivalencia_eu, equivalencia_us, " +
                 "equivalencia_uk, equivalencia_cm, genero, activo " +
                 "FROM tallas WHERE genero = ? AND activo = 1 " +
                 "ORDER BY CAST(numero AS UNSIGNED), numero";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setString(1, genero.toUpperCase());
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ModelSize talla = new ModelSize();
                talla.setIdTalla(rs.getInt("id_talla"));
                talla.setNumero(rs.getString("numero"));
                talla.setSistema(rs.getString("sistema"));
                
                // Manejar posibles valores null
                talla.setEquivalenciaEu(rs.getObject("equivalencia_eu") != null ? rs.getDouble("equivalencia_eu") : null);
                talla.setEquivalenciaUs(rs.getObject("equivalencia_us") != null ? rs.getDouble("equivalencia_us") : null);
                talla.setEquivalenciaUk(rs.getObject("equivalencia_uk") != null ? rs.getDouble("equivalencia_uk") : null);
                talla.setEquivalenciaCm(rs.getObject("equivalencia_cm") != null ? rs.getDouble("equivalencia_cm") : null);
                
                talla.setGenero(rs.getString("genero"));
                talla.setActivo(rs.getBoolean("activo"));
                
                tallas.add(talla);
            }
        }
        
        System.out.println("SUCCESS  Cargadas " + tallas.size() + " tallas para género: " + genero);
        return tallas;
        
    } catch (SQLException e) {
        System.err.println("ERROR  Error cargando tallas por género '" + genero + "': " + e.getMessage());
        throw e;
    }
}

/**
 * Obtiene una talla específica por su ID
 * @param idTalla ID de la talla
 * @return ModelSize o null si no se encuentra
 * @throws SQLException Si hay error en la consulta
 */
public ModelSize getTallaById(int idTalla) throws SQLException {
    String sql = "SELECT id_talla, numero, sistema, equivalencia_eu, equivalencia_us, " +
                 "equivalencia_uk, equivalencia_cm, genero, activo " +
                 "FROM tallas WHERE id_talla = ? AND activo = 1";
    
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {
        
        stmt.setInt(1, idTalla);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                ModelSize talla = new ModelSize();
                talla.setIdTalla(rs.getInt("id_talla"));
                talla.setNumero(rs.getString("numero"));
                talla.setSistema(rs.getString("sistema"));
                
                // Manejar posibles valores null
                talla.setEquivalenciaEu(rs.getObject("equivalencia_eu") != null ? rs.getDouble("equivalencia_eu") : null);
                talla.setEquivalenciaUs(rs.getObject("equivalencia_us") != null ? rs.getDouble("equivalencia_us") : null);
                talla.setEquivalenciaUk(rs.getObject("equivalencia_uk") != null ? rs.getDouble("equivalencia_uk") : null);
                talla.setEquivalenciaCm(rs.getObject("equivalencia_cm") != null ? rs.getDouble("equivalencia_cm") : null);
                
                talla.setGenero(rs.getString("genero"));
                talla.setActivo(rs.getBoolean("activo"));
                
                return talla;
            }
        }
    } catch (SQLException e) {
        System.err.println("ERROR  Error obteniendo talla por ID " + idTalla + ": " + e.getMessage());
        throw e;
    }
    
    return null;
}

/**
 * Verifica si existe una talla con un nombre específico
 * @param nombreTalla Nombre de la talla a verificar
 * @return true si existe, false si no
 */
public boolean existeTallaPorNombre(String nombreTalla) {
    try {
        return getSizeIdByName(nombreTalla) > 0;
    } catch (SQLException e) {
        return false;
    }
}
}




