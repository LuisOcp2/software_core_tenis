package raven.clases.productos;

import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelBrand;

import java.sql.*;
import java.util.*;

public class ServiceProductOptimized {
    
    /**
     * Obtiene productos con paginación optimizada y carga eficiente de datos relacionados
     */
    public List<ModelProduct> getProductosPaginados(int offset, int limit, String searchTerm, Integer bodegaId, String stockTypeFilter) throws SQLException {
        List<ModelProduct> productos = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        sql.append("SELECT ");
        sql.append("    p.id_producto, p.codigo_modelo, p.nombre, p.descripcion, ");
        sql.append("    p.precio_compra, p.precio_venta, p.stock_minimo, p.genero, ");
        sql.append("    p.ubicacion, p.pares_por_caja, p.activo, ");
        sql.append("    c.nombre AS categoria_nombre, m.nombre AS marca_nombre, ");
        sql.append("    COALESCE(SUM(ib.Stock_par), 0) AS total_pares, ");
        sql.append("    COALESCE(SUM(ib.Stock_caja), 0) AS total_cajas ");
        
        sql.append("FROM productos p ");
        sql.append("INNER JOIN categorias c ON p.id_categoria = c.id_categoria ");
        sql.append("INNER JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("LEFT JOIN producto_variantes pv ON pv.id_producto = p.id_producto AND pv.disponible = 1 ");
        sql.append("LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 ");
        
        if (bodegaId != null && bodegaId > 0) {
            sql.append("AND ib.id_bodega = ? ");
            params.add(bodegaId);
        }
        
        sql.append("WHERE p.activo = 1 ");
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            sql.append("AND (p.nombre LIKE ? OR p.codigo_modelo LIKE ? OR m.nombre LIKE ?) ");
            String likeTerm = "%" + searchTerm + "%";
            params.add(likeTerm);
            params.add(likeTerm);
            params.add(likeTerm);
        }
        
        sql.append("GROUP BY p.id_producto ");
        
        if (stockTypeFilter != null) {
            if ("pares".equalsIgnoreCase(stockTypeFilter)) {
                sql.append("HAVING total_pares > 0 ");
            } else if ("cajas".equalsIgnoreCase(stockTypeFilter)) {
                sql.append("HAVING total_cajas > 0 ");
            }
        }
        
        sql.append("ORDER BY p.nombre ASC ");
        sql.append("LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);
        
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelProduct product = mapProductoFromResultSet(rs);
                    // Cargar colores y tallas de forma diferida para evitar sobrecarga
                    cargarVariantesBasicas(product);
                    productos.add(product);
                }
            }
        }
        
        return productos;
    }
    
    /**
     * Obtiene el conteo total de productos para paginación
     */
    public int getTotalProductos(String searchTerm, Integer bodegaId, String stockTypeFilter) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        sql.append("SELECT COUNT(DISTINCT p.id_producto) ");
        sql.append("FROM productos p ");
        sql.append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("LEFT JOIN producto_variantes pv ON pv.id_producto = p.id_producto AND pv.disponible = 1 ");
        sql.append("LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 ");
        
        if (bodegaId != null && bodegaId > 0) {
            sql.append("WHERE ib.id_bodega = ? ");
            params.add(bodegaId);
        } else {
            sql.append("WHERE 1=1 ");
        }
        
        sql.append("AND p.activo = 1 ");
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            sql.append("AND (p.nombre LIKE ? OR p.codigo_modelo LIKE ? OR m.nombre LIKE ?) ");
            String likeTerm = "%" + searchTerm + "%";
            params.add(likeTerm);
            params.add(likeTerm);
            params.add(likeTerm);
        }
        
        if (stockTypeFilter != null) {
            if ("pares".equalsIgnoreCase(stockTypeFilter)) {
                sql.append("AND COALESCE(SUM(ib.Stock_par), 0) > 0 ");
            } else if ("cajas".equalsIgnoreCase(stockTypeFilter)) {
                sql.append("AND COALESCE(SUM(ib.Stock_caja), 0) > 0 ");
            }
        }
        
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Carga solo la información básica de variantes (colores y tallas) sin imágenes
     */
    private void cargarVariantesBasicas(ModelProduct product) {
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT DISTINCT col.nombre, t.numero " +
                 "FROM producto_variantes pv " +
                 "LEFT JOIN colores col ON pv.id_color = col.id_color " +
                 "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                 "WHERE pv.id_producto = ? AND pv.disponible = 1 " +
                 "ORDER BY col.nombre, t.numero")) {
            
            stmt.setInt(1, product.getProductId());
            
            List<String> colores = new ArrayList<>();
            List<String> tallas = new ArrayList<>();
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String color = rs.getString("nombre");
                    String talla = rs.getString("numero");
                    
                    if (color != null && !colores.contains(color)) {
                        colores.add(color);
                    }
                    if (talla != null && !tallas.contains(talla)) {
                        tallas.add(talla);
                    }
                }
            }
            
            product.setColor(String.join(", ", colores));
            product.setSize(String.join(", ", tallas));
            
        } catch (SQLException e) {
            // En caso de error, asignar valores por defecto
            product.setColor("Sin variantes");
            product.setSize("Sin tallas");
            e.printStackTrace();
        }
    }
    
    private ModelProduct mapProductoFromResultSet(ResultSet rs) throws SQLException {
        ModelProduct product = new ModelProduct();
        product.setProductId(rs.getInt("id_producto"));
        product.setModelCode(rs.getString("codigo_modelo"));
        product.setName(rs.getString("nombre"));
        product.setDescription(rs.getString("descripcion"));
        product.setPurchasePrice(rs.getDouble("precio_compra"));
        product.setSalePrice(rs.getDouble("precio_venta"));
        product.setMinStock(rs.getInt("stock_minimo"));
        product.setGender(rs.getString("genero"));
        product.setUbicacion(rs.getString("ubicacion"));
        product.setPairsPerBox(rs.getInt("pares_por_caja"));
        product.setActive(rs.getBoolean("activo"));
        
        // Asignar objetos relacionados
        ModelCategory category = new ModelCategory();
        category.setName(rs.getString("categoria_nombre"));
        product.setCategory(category);
        
        ModelBrand brand = new ModelBrand();
        brand.setName(rs.getString("marca_nombre"));
        product.setBrand(brand);
        
        // Los stocks se asignan directamente desde la consulta
        product.setPairsStock(rs.getInt("total_pares"));
        product.setBoxesStock(rs.getInt("total_cajas"));
        
        return product;
    }
    
    /**
     * Busca productos de forma optimizada
     */
    public List<ModelProduct> buscarProductos(String searchTerm, Integer bodegaId, int limit) throws SQLException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getProductosPaginados(0, limit, null, bodegaId, null);
        }
        
        List<ModelProduct> productos = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        sql.append("SELECT DISTINCT ");
        sql.append("    p.id_producto, p.codigo_modelo, p.nombre, p.descripcion, ");
        sql.append("    p.precio_compra, p.precio_venta, p.stock_minimo, p.genero, ");
        sql.append("    p.ubicacion, p.pares_por_caja, p.activo, ");
        sql.append("    c.nombre AS categoria_nombre, m.nombre AS marca_nombre, ");
        sql.append("    COALESCE(SUM(ib.Stock_par), 0) AS total_pares, ");
        sql.append("    COALESCE(SUM(ib.Stock_caja), 0) AS total_cajas ");
        
        sql.append("FROM productos p ");
        sql.append("INNER JOIN categorias c ON p.id_categoria = c.id_categoria ");
        sql.append("INNER JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("LEFT JOIN producto_variantes pv ON pv.id_producto = p.id_producto AND pv.disponible = 1 ");
        sql.append("LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 ");
        
        if (bodegaId != null && bodegaId > 0) {
            sql.append("AND ib.id_bodega = ? ");
            params.add(bodegaId);
        }
        
        sql.append("WHERE p.activo = 1 ");
        sql.append("AND (p.nombre LIKE ? OR p.codigo_modelo LIKE ? OR m.nombre LIKE ?) ");
        
        String likeTerm = "%" + searchTerm + "%";
        params.add(likeTerm);
        params.add(likeTerm);
        params.add(likeTerm);
        
        sql.append("GROUP BY p.id_producto ");
        sql.append("ORDER BY p.nombre ASC ");
        sql.append("LIMIT ? ");
        params.add(limit);
        
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelProduct product = mapProductoFromResultSet(rs);
                    cargarVariantesBasicas(product);
                    productos.add(product);
                }
            }
        }
        
        return productos;
    }
}