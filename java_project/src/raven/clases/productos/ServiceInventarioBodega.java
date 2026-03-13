package raven.clases.productos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelInventarioBodega;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * ServiceInventarioBodega - VERSIÓN FINAL V3
 * ════════════════════════════════════════════════════════════════════════════
 * 
 * CORRECCIÓN CRÍTICA V3:
 * SUCCESS  NO parsear tallas - Mantener formato completo "42 EU H"
 * SUCCESS  JOINs correctos con tablas tallas y colores
 * SUCCESS  Formato de talla: numero + sistema + genero (ej: "42 EU H")
 * SUCCESS  Búsqueda exacta sin modificar el valor original
 * 
 * @author Sistema de Gestión de Inventarios
 * @version 3.0 - FINAL
 */
public class ServiceInventarioBodega {
    
    private static final Logger LOGGER = Logger.getLogger(ServiceInventarioBodega.class.getName());
    
    // ══════════════════════════════════════════════════════════════════════════
    // QUERIES SQL - VERSIÓN FINAL CON FORMATO COMPLETO DE TALLAS
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * SUCCESS  Query para obtener tallas en formato completo: "42 EU H"
     * Formato: numero + sistema + genero
     */
    private static final String SQL_GET_TALLAS_DISPONIBLES = 
        "SELECT DISTINCT " +
        "    CONCAT_WS(' ', t.numero, t.sistema, t.genero) AS talla_completa " +
        "FROM inventario_bodega ib " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "WHERE ib.id_bodega = ? " +
        "    AND pv.id_producto = ? " +
        "    AND ib.activo = 1 " +
        "    AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
        "ORDER BY t.numero";
    
    /**
     * SUCCESS  Query para obtener colores disponibles
     */
    private static final String SQL_GET_COLORES_DISPONIBLES = 
        "SELECT DISTINCT " +
        "    c.nombre AS color " +
        "FROM inventario_bodega ib " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_bodega = ? " +
        "    AND pv.id_producto = ? " +
        "    AND ib.activo = 1 " +
        "    AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
        "ORDER BY c.nombre";
    
    /**
     * SUCCESS  Query para obtener stock con talla en formato completo
     */
    private static final String SQL_GET_STOCK_BY_VARIANTE_BODEGA = 
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    ib.id_variante, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    ib.fecha_ultimo_movimiento, " +
        "    ib.ubicacion_especifica, " +
        "    ib.activo, " +
        "    CONCAT_WS(' ', t.numero, t.sistema, t.genero) AS talla_completa, " +
        "    c.nombre AS color, " +
        "    pv.SKU, " +
        "    pv.EAN, " +
        "    pv.precio_venta, " +
        "    p.nombre AS nombre_producto, " +
        "    p.id_producto " +
        "FROM inventario_bodega ib " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_bodega = ? " +
        "    AND ib.id_variante = ? " +
        "    AND ib.activo = 1 " +
        "    AND (ib.Stock_par > 0 OR ib.Stock_caja > 0)";
    
    /**
     * SUCCESS  Query para obtener variantes disponibles con talla completa
     */
    private static final String SQL_GET_VARIANTES_DISPONIBLES_PRODUCTO = 
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    ib.id_variante, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    CONCAT_WS(' ', t.numero, t.sistema, t.genero) AS talla_completa, " +
        "    c.nombre AS color, " +
        "    pv.SKU, " +
        "    pv.EAN, " +
        "    pv.precio_venta " +
        "FROM inventario_bodega ib " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_bodega = ? " +
        "    AND pv.id_producto = ? " +
        "    AND ib.activo = 1 " +
        "    AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
        "ORDER BY t.numero, c.nombre";
    
    /**
     * SUCCESS  Query para buscar variante por talla y color COMPLETOS
     * Búsqueda exacta sin parsear
     */
    private static final String SQL_GET_VARIANTE_BY_TALLA_COLOR = 
        "SELECT " +
        "    ib.id_inventario_bodega, " +
        "    ib.id_bodega, " +
        "    ib.id_variante, " +
        "    ib.Stock_par, " +
        "    ib.Stock_caja, " +
        "    ib.stock_reservado, " +
        "    CONCAT_WS(' ', t.numero, t.sistema, t.genero) AS talla_completa, " +
        "    c.nombre AS color, " +
        "    pv.SKU, " +
        "    pv.EAN, " +
        "    pv.precio_venta, " +
        "    pv.precio_compra, " +
        "    p.nombre AS nombre_producto, " +
        "    p.id_producto " +
        "FROM inventario_bodega ib " +
        "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
        "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
        "INNER JOIN colores c ON pv.id_color = c.id_color " +
        "WHERE ib.id_bodega = ? " +
        "    AND pv.id_producto = ? " +
        "    AND CONCAT_WS(' ', t.numero, t.sistema, t.genero) = ? " +  // SUCCESS  Búsqueda por talla completa
        "    AND c.nombre = ? " +
        "    AND ib.activo = 1";
    
    // ══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Obtiene el stock disponible de una variante específica en una bodega
     */
    public ModelInventarioBodega obtenerStockPorVariante(int idBodega, int idVariante) throws SQLException {
        LOGGER.log(Level.INFO, "Consultando stock - Bodega: {0}, Variante: {1}", 
                new Object[]{idBodega, idVariante});
        
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_STOCK_BY_VARIANTE_BODEGA)) {
            
            ps.setInt(1, idBodega);
            ps.setInt(2, idVariante);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSetAModelo(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener stock por variante", e);
            throw e;
        }
        
        return null;
    }
    
    /**
     * Obtiene todas las variantes disponibles de un producto en una bodega
     */
    public List<ModelInventarioBodega> obtenerVariantesDisponibles(int idBodega, int idProducto) throws SQLException {
        LOGGER.log(Level.INFO, "Consultando variantes disponibles - Bodega: {0}, Producto: {1}", 
                new Object[]{idBodega, idProducto});
        
        List<ModelInventarioBodega> variantes = new ArrayList<>();
        
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_VARIANTES_DISPONIBLES_PRODUCTO)) {
            
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    variantes.add(mapearResultSetSimplificado(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener variantes disponibles", e);
            throw e;
        }
        
        LOGGER.log(Level.INFO, "Variantes disponibles encontradas: {0}", variantes.size());
        return variantes;
    }
    
    /**
     * SUCCESS  CORREGIDO V3: Obtiene variante por talla y color COMPLETOS
     * NO parsea - busca exactamente como viene: "42 EU H"
     * 
     * @param idBodega ID de la bodega
     * @param idProducto ID del producto
     * @param tallaCompleta Talla COMPLETA sin parsear (ej: "42 EU H")
     * @param color Color del producto
     * @return ModelInventarioBodega o null
     */
    public ModelInventarioBodega obtenerVariantePorTallaColor(
            int idBodega, int idProducto, String tallaCompleta, String color) throws SQLException {
        
        // SUCCESS  NO PARSEAR - Usar la talla tal cual viene
        LOGGER.log(Level.INFO, 
                "Buscando variante - Bodega: {0}, Producto: {1}, Talla: {2}, Color: {3}", 
                new Object[]{idBodega, idProducto, tallaCompleta, color});
        
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_VARIANTE_BY_TALLA_COLOR)) {
            
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            ps.setString(3, tallaCompleta);  // SUCCESS  Talla completa sin parsear
            ps.setString(4, color);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ModelInventarioBodega modelo = mapearResultSetCompleto(rs);
                    
                    if (modelo.getStockPar() > 0 || modelo.getStockCaja() > 0) {
                        LOGGER.log(Level.INFO, "[EXITO] Variante encontrada - Stock Pares: {0}, Cajas: {1}", 
                            new Object[]{modelo.getStockPar(), modelo.getStockCaja()});
                        return modelo;
                    } else {
                        LOGGER.log(Level.WARNING, "[AVISO] Variante encontrada pero sin stock disponible (Pares: 0, Cajas: 0)");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener variante por talla y color", e);
            throw e;
        }
        
        LOGGER.log(Level.WARNING, "ERROR  No se encontró variante con talla: {0} y color: {1}", 
                new Object[]{tallaCompleta, color});
        
        return null;
    }
    
    /**
     * SUCCESS  CORREGIDO V3: Obtiene tallas en formato completo "42 EU H"
     * Retorna exactamente como está en BD, SIN formatear ni parsear
     */
    public List<String> obtenerTallasDisponibles(int idBodega, int idProducto) throws SQLException {
        LOGGER.log(Level.INFO, "Consultando tallas disponibles - Bodega: {0}, Producto: {1}", 
                new Object[]{idBodega, idProducto});
        
        List<String> tallas = new ArrayList<>();
        
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_TALLAS_DISPONIBLES)) {
            
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // SUCCESS  Obtener talla completa directamente del CONCAT_WS
                    String tallaCompleta = rs.getString("talla_completa");
                    
                    // SUCCESS  NO PARSEAR - Agregar tal cual viene
                    tallas.add(tallaCompleta);
                    
                    LOGGER.log(Level.FINE, "Talla encontrada: {0}", tallaCompleta);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener tallas disponibles", e);
            throw e;
        }
        
        LOGGER.log(Level.INFO, "SUCCESS  Tallas disponibles encontradas: {0}", tallas.size());
        
        // Log detallado de las tallas
        if (!tallas.isEmpty()) {
            System.out.println(" Lista de tallas obtenidas:");
            for (int i = 0; i < tallas.size(); i++) {
                System.out.println("   [" + i + "] " + tallas.get(i));
            }
        }
        
        return tallas;
    }
    
    /**
     * Obtiene lista de colores únicos disponibles
     */
    public List<String> obtenerColoresDisponibles(int idBodega, int idProducto) throws SQLException {
        LOGGER.log(Level.INFO, "Consultando colores disponibles - Bodega: {0}, Producto: {1}", 
                new Object[]{idBodega, idProducto});
        
        List<String> colores = new ArrayList<>();
        
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_COLORES_DISPONIBLES)) {
            
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String color = rs.getString("color");
                    colores.add(color);
                    LOGGER.log(Level.FINE, "Color encontrado: {0}", color);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener colores disponibles", e);
            throw e;
        }
        
        LOGGER.log(Level.INFO, "SUCCESS  Colores disponibles encontrados: {0}", colores.size());
        
        // Log detallado de los colores
        if (!colores.isEmpty()) {
            System.out.println("Tema Lista de colores obtenidos:");
            for (int i = 0; i < colores.size(); i++) {
                System.out.println("   [" + i + "] " + colores.get(i));
            }
        }
        
        return colores;
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS - MAPEO DE RESULTADOS
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * SUCCESS  Mapea ResultSet completo con talla completa
     */
    private ModelInventarioBodega mapearResultSetCompleto(ResultSet rs) throws SQLException {
        ModelInventarioBodega modelo = new ModelInventarioBodega();
        
        modelo.setIdInventarioBodega(rs.getInt("id_inventario_bodega"));
        modelo.setIdBodega(rs.getInt("id_bodega"));
        modelo.setIdVariante(rs.getInt("id_variante"));
        modelo.setStockPar(rs.getInt("Stock_par"));
        modelo.setStockCaja(rs.getInt("Stock_caja"));
        modelo.setStockReservado(rs.getInt("stock_reservado"));
        
        // SUCCESS  Talla completa sin parsear
        modelo.setTalla(rs.getString("talla_completa"));
        modelo.setColor(rs.getString("color"));
        
        modelo.setSku(rs.getString("SKU"));
        modelo.setEan(rs.getString("EAN"));
        modelo.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        modelo.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        modelo.setNombreProducto(rs.getString("nombre_producto"));
        modelo.setIdProducto(rs.getInt("id_producto"));
        
        return modelo;
    }
    
    /**
     * SUCCESS  Mapea ResultSet básico
     */
    private ModelInventarioBodega mapearResultSetAModelo(ResultSet rs) throws SQLException {
        ModelInventarioBodega modelo = new ModelInventarioBodega();
        
        modelo.setIdInventarioBodega(rs.getInt("id_inventario_bodega"));
        modelo.setIdBodega(rs.getInt("id_bodega"));
        modelo.setIdVariante(rs.getInt("id_variante"));
        modelo.setStockPar(rs.getInt("Stock_par"));
        modelo.setStockCaja(rs.getInt("Stock_caja"));
        modelo.setStockReservado(rs.getInt("stock_reservado"));
        
        // SUCCESS  Talla completa sin parsear
        modelo.setTalla(rs.getString("talla_completa"));
        modelo.setColor(rs.getString("color"));
        
        modelo.setSku(rs.getString("SKU"));
        modelo.setEan(rs.getString("EAN"));
        modelo.setNombreProducto(rs.getString("nombre_producto"));
        modelo.setIdProducto(rs.getInt("id_producto"));
        
        return modelo;
    }
    
    /**
     * SUCCESS  Mapea ResultSet simplificado
     */
    private ModelInventarioBodega mapearResultSetSimplificado(ResultSet rs) throws SQLException {
        ModelInventarioBodega modelo = new ModelInventarioBodega();
        
        modelo.setIdInventarioBodega(rs.getInt("id_inventario_bodega"));
        modelo.setIdBodega(rs.getInt("id_bodega"));
        modelo.setIdVariante(rs.getInt("id_variante"));
        modelo.setStockPar(rs.getInt("Stock_par"));
        modelo.setStockCaja(rs.getInt("Stock_caja"));
        modelo.setStockReservado(rs.getInt("stock_reservado"));
        
        // SUCCESS  Talla completa sin parsear
        modelo.setTalla(rs.getString("talla_completa"));
        modelo.setColor(rs.getString("color"));
        
        modelo.setSku(rs.getString("SKU"));
        modelo.setEan(rs.getString("EAN"));
        modelo.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        
        return modelo;
    }
}
