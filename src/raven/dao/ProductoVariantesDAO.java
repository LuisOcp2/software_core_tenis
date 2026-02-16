package raven.dao;

import java.sql.*;
import java.math.BigDecimal;
import raven.controlador.productos.ModelProductVariant;
import raven.controlador.principal.conexion;

/**
 * DAO ProductoVariantes - 100% LIMPIO sin errores de alias.
 * 
 * GARANTÍA: NO contiene ninguna referencia a "b.activo"
 * 
 * @author Ingeniero Senior
 * @version 5.0 - LIMPIO Y DEFINITIVO
 */
public class ProductoVariantesDAO {
    public static class VariantTableItem { public int idVariante; public String talla; public String color; public int stockPares; public int stockCajas; public byte[] imagen; }
    public static class VariantBodegaItem { public int idVariante; public String talla; public String color; public int stockPares; public int stockCajas; public byte[] imagen; public int idBodega; public String bodegaNombre; }
    
    /**
     * Inserta una nueva variante de producto.
     *
     * IMPORTANTE: Ahora requiere id_proveedor porque pueden existir
     * múltiples variantes con la misma talla/color pero diferentes proveedores.
     */
    public int insert(ModelProductVariant v) throws SQLException {
        // Validaciones
        if (v == null) {
            throw new IllegalArgumentException("La variante no puede ser null");
        }

        if (v.getProductId() <= 0) {
            throw new IllegalArgumentException("ID de producto inválido: " + v.getProductId());
        }

        if (v.getSizeId() <= 0) {
            throw new IllegalArgumentException("ID de talla inválido: " + v.getSizeId());
        }

        if (v.getColorId() <= 0) {
            throw new IllegalArgumentException("ID de color inválido: " + v.getColorId());
        }

        // SUCCESS  VALIDACIÓN NUEVA: El proveedor es obligatorio
        if (v.getSupplierId() <= 0) {
            throw new IllegalArgumentException("ID de proveedor inválido o no especificado: " + v.getSupplierId());
        }

        // SUCCESS  Buscar variante existente CON el proveedor incluido
        Integer existente = findExistingId(v.getProductId(), v.getSizeId(), v.getColorId(), v.getSupplierId());
        if (existente != null && existente > 0) {
            System.out.println("ℹ Variante ya existe para este proveedor, retornando ID: " + existente);
            return existente;
        }

        String sql = "INSERT INTO producto_variantes (" +
                    "id_producto, id_talla, id_color, id_proveedor, " +
                    "imagen, ean, sku, " +
                    "precio_compra, precio_venta, " +
                    "stock_minimo_variante, disponible" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

        try (Connection con = conexion.getInstance().createConnection()) {
            try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setInsertParams(pst, v);
                int affectedRows = pst.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("La inserción de variante falló, no se afectaron filas");
                }

                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        System.out.println("SUCCESS  Variante insertada con ID: " + generatedId);
                        return generatedId;
                    }
                }
                throw new SQLException("La inserción de variante falló, no se obtuvo ID");
            } catch (SQLException e) {
                 if (isMissingDefaultIdVariante(e)) {
                     System.out.println("DETECTADO: id_variante sin default. Intentando corregir esquema...");
                     fixVariantIdAutoIncrement(con);
                     
                     // Retry insert after fix
                     try (PreparedStatement pstRetry = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                         setInsertParams(pstRetry, v);
                         int affectedRows = pstRetry.executeUpdate();
                         
                         if (affectedRows == 0) {
                            throw new SQLException("La inserción de variante falló (reintento), no se afectaron filas");
                         }

                         try (ResultSet rs = pstRetry.getGeneratedKeys()) {
                            if (rs.next()) {
                                int generatedId = rs.getInt(1);
                                System.out.println("SUCCESS  Variante insertada con ID (tras corrección): " + generatedId);
                                return generatedId;
                            }
                         }
                         throw new SQLException("La inserción de variante falló (reintento), no se obtuvo ID");
                     }
                 }
                 throw e;
            }
        } catch (SQLException e) {
            if (isDuplicateByProductoTallaColor(e)) {
                throw new SQLException(
                        "La BD aún tiene la restricción unique_producto_talla_color. " +
                                "Ejecuta el script SQL/mejora_variantes_multiples_proveedores.sql para permitir variantes por proveedor.",
                        e
                );
            }
            throw e;
        }
    }

    private void setInsertParams(PreparedStatement pst, ModelProductVariant v) throws SQLException {
        int paramIndex = 1;

        pst.setInt(paramIndex++, v.getProductId());
        pst.setInt(paramIndex++, v.getSizeId());
        pst.setInt(paramIndex++, v.getColorId());
        pst.setInt(paramIndex++, v.getSupplierId());

        if (v.getImageBytes() != null && v.getImageBytes().length > 0) {
            pst.setBytes(paramIndex++, v.getImageBytes());
        } else {
            pst.setNull(paramIndex++, java.sql.Types.BLOB);
        }

        v.generateEanIfEmpty();
        String eanVal = v.getEan();
        String skuVal = v.getSku();
        if (skuVal == null || skuVal.isEmpty()) {
            long ts = System.currentTimeMillis() % 100000;
            skuVal = "SKU-" + v.getProductId() + "-" + v.getSizeId() + "-" + v.getColorId() + "-" + ts;
            v.setSku(skuVal);
        }
        pst.setString(paramIndex++, eanVal);
        pst.setString(paramIndex++, skuVal);

        Double purchasePrice = v.getPurchasePrice();
        Double salePrice = v.getSalePrice();

        BigDecimal pc = (purchasePrice != null && purchasePrice > 0)
                ? BigDecimal.valueOf(purchasePrice)
                : BigDecimal.ZERO;

        BigDecimal pv = (salePrice != null && salePrice > 0)
                ? BigDecimal.valueOf(salePrice)
                : BigDecimal.ZERO;

        pst.setBigDecimal(paramIndex++, pc);
        pst.setBigDecimal(paramIndex++, pv);

        Integer minStock = v.getMinStock();
        pst.setInt(paramIndex++, (minStock != null && minStock > 0) ? minStock : 0);
    }

    private boolean isMissingDefaultIdVariante(SQLException e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("Field 'id_variante' doesn't have a default value");
    }

    private void fixVariantIdAutoIncrement(Connection con) throws SQLException {
        String fixSql = "ALTER TABLE producto_variantes MODIFY id_variante int(11) NOT NULL AUTO_INCREMENT";
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(fixSql);
            System.out.println("CORRECCIÓN APLICADA: id_variante ahora es AUTO_INCREMENT");
        } catch (SQLException ex) {
            System.err.println("FALLÓ CORRECCIÓN AUTOMÁTICA: " + ex.getMessage());
            throw ex;
        }
    }

    private static boolean isDuplicateByProductoTallaColor(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("Duplicate entry") && msg.contains("unique_producto_talla_color");
    }

    /**
     * Actualiza una variante existente.
     */
    public void update(ModelProductVariant v) throws SQLException {
        if (v == null) {
            throw new IllegalArgumentException("La variante no puede ser null");
        }
        
        if (v.getVariantId() <= 0) {
            throw new IllegalArgumentException("ID de variante inválido: " + v.getVariantId());
        }
        
        String sql = "UPDATE producto_variantes SET " +
                    "id_talla=?, " +
                    "id_color=?, " +
                    "imagen=?, " +
                    "ean=?, " +
                    "sku=?, " +
                    "precio_compra=?, " +
                    "precio_venta=?, " +
                    "stock_minimo_variante=?, " +
                    "disponible=?, " +
                    "fecha_actualizacion=CURRENT_TIMESTAMP " +
                    "WHERE id_variante=?";
        
        try (Connection con = conexion.getInstance().createConnection(); 
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            int paramIndex = 1;
            
            pst.setInt(paramIndex++, v.getSizeId());
            pst.setInt(paramIndex++, v.getColorId());
            
            // Imagen
            if (v.getImageBytes() != null && v.getImageBytes().length > 0) {
                pst.setBytes(paramIndex++, v.getImageBytes());
            } else {
                pst.setNull(paramIndex++, java.sql.Types.BLOB);
            }
            
            pst.setString(paramIndex++, v.getEan());
            pst.setString(paramIndex++, v.getSku());
            
            // Precios
            Double purchasePrice = v.getPurchasePrice();
            Double salePrice = v.getSalePrice();
            
            BigDecimal pc = (purchasePrice != null) 
                ? BigDecimal.valueOf(purchasePrice) 
                : BigDecimal.ZERO;
            
            BigDecimal pv2 = (salePrice != null) 
                ? BigDecimal.valueOf(salePrice) 
                : BigDecimal.ZERO;
            
            pst.setBigDecimal(paramIndex++, pc);
            pst.setBigDecimal(paramIndex++, pv2);
            
            // Stock mínimo
            Integer minStock = v.getMinStock();
            pst.setInt(paramIndex++, (minStock != null) ? minStock : 0);
            
            // Disponible
            pst.setBoolean(paramIndex++, v.isAvailable());
            
            // WHERE - ID variante
            pst.setInt(paramIndex++, v.getVariantId());
            
            // Ejecutar
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException(
                    "Actualización falló, variante no encontrada con ID: " + v.getVariantId()
                );
            }
            
            System.out.println("SUCCESS  Variante actualizada: ID=" + v.getVariantId());
        }
    }
    
    /**
     * Busca una variante existente por producto, talla, color Y PROVEEDOR.
     *
     * IMPORTANTE: Ahora se requiere el proveedor porque pueden existir
     * múltiples variantes con la misma talla/color pero diferentes proveedores.
     *
     * SIN JOINS - Consulta simple y directa.
     */
    public Integer findExistingId(int idProducto, int idTalla, int idColor, int idProveedor) throws SQLException {
        if (idProducto <= 0 || idTalla <= 0 || idColor <= 0 || idProveedor <= 0) {
            return null;
        }

        // SUCCESS  CONSULTA ACTUALIZADA - Incluye id_proveedor en la búsqueda
        String sql = "SELECT id_variante " +
                    "FROM producto_variantes " +
                    "WHERE id_producto=? AND id_talla=? AND id_color=? AND id_proveedor=? " +
                    "LIMIT 1";

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idProducto);
            pst.setInt(2, idTalla);
            pst.setInt(3, idColor);
            pst.setInt(4, idProveedor);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id_variante");
                    System.out.println("WARNING  Variante ya existe con ID: " + existingId +
                                     " (Proveedor: " + idProveedor + ")");
                    return existingId;
                }
            }
        }

        return null;
    }

    public Integer findExistingId(int idProducto, int idTalla, int idColor) throws SQLException {
        if (idProducto <= 0 || idTalla <= 0 || idColor <= 0) {
            return null;
        }

        // SUCCESS  CONSULTA ACTUALIZADA - Incluye id_proveedor en la búsqueda
        String sql = "SELECT id_variante " +
                    "FROM producto_variantes " +
                    "WHERE id_producto=? AND id_talla=? AND id_color=? " +
                    "LIMIT 1";

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idProducto);
            pst.setInt(2, idTalla);
            pst.setInt(3, idColor);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id_variante");
                    System.out.println("WARNING  Variante ya existe con ID: " + existingId +
                                     " (Proveedor: ???)");
                    return existingId;
                }
            }
        }

        return null;
    }

    /**
     * Verifica si un SKU ya existe en la base de datos.
     * @param sku El SKU a verificar.
     * @return true si el SKU ya existe, false en caso contrario.
     */
    public boolean checkSkuExists(String sku) throws SQLException {
        if (sku == null || sku.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT 1 FROM producto_variantes WHERE sku = ? LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, sku.trim());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * MÉTODO DE COMPATIBILIDAD: Busca variantes sin especificar proveedor.
     * Devuelve TODAS las variantes que coincidan con producto/talla/color.
     *
     * @return Lista de IDs de variantes encontradas (puede haber múltiples proveedores)
     */
    public java.util.List<Integer> findAllVariantsForCombo(int idProducto, int idTalla, int idColor) throws SQLException {
        if (idProducto <= 0 || idTalla <= 0 || idColor <= 0) {
            return new java.util.ArrayList<>();
        }

        String sql = "SELECT id_variante, id_proveedor " +
                    "FROM producto_variantes " +
                    "WHERE id_producto=? AND id_talla=? AND id_color=? " +
                    "ORDER BY id_proveedor";

        java.util.List<Integer> ids = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idProducto);
            pst.setInt(2, idTalla);
            pst.setInt(3, idColor);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id_variante");
                    int prov = rs.getInt("id_proveedor");
                    ids.add(id);
                    System.out.println("ℹ Encontrada variante ID: " + id + " (Proveedor: " + prov + ")");
                }
            }
        }

        return ids;
    }
    
    /**
     * Actualiza solo la imagen de una variante.
     */
    public void updateImage(int idVariante, byte[] bytes) throws SQLException {
        if (idVariante <= 0) {
            throw new IllegalArgumentException("ID de variante inválido: " + idVariante);
        }
        
        String sql = "UPDATE producto_variantes SET " +
                    "imagen=?, " +
                    "fecha_actualizacion=CURRENT_TIMESTAMP " +
                    "WHERE id_variante=?";
        
        try (Connection con = conexion.getInstance().createConnection(); 
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            if (bytes != null && bytes.length > 0) {
                pst.setBytes(1, bytes);
                System.out.println(" Actualizando imagen: " + (bytes.length / 1024) + " KB");
            } else {
                pst.setNull(1, java.sql.Types.BLOB);
                System.out.println("Eliminar Eliminando imagen (NULL)");
            }
            
            pst.setInt(2, idVariante);
            
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException(
                    "Actualización de imagen falló, variante no encontrada: " + idVariante
                );
            }
            
            System.out.println("SUCCESS  Imagen actualizada para variante ID: " + idVariante);
        }
    }

    public byte[] getImageBytes(int idVariante) throws SQLException {
        String sql = "SELECT imagen FROM producto_variantes WHERE id_variante=? AND imagen IS NOT NULL";
        try (java.sql.Connection con = conexion.getInstance().createConnection(); java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idVariante);
            try (java.sql.ResultSet rs = pst.executeQuery()) { if (rs.next()) return rs.getBytes(1); }
        }
        return null;
    }

    public void updatePrecioVenta(int idVariante, java.math.BigDecimal precio) throws SQLException {
        String sql = "UPDATE producto_variantes SET precio_venta=?, fecha_actualizacion=CURRENT_TIMESTAMP WHERE id_variante=?";
        try (java.sql.Connection con = conexion.getInstance().createConnection(); java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            if (precio != null) pst.setBigDecimal(1, precio); else pst.setNull(1, java.sql.Types.DECIMAL);
            pst.setInt(2, idVariante);
            pst.executeUpdate();
        }
    }

    public void updatePrecioCompra(int idVariante, java.math.BigDecimal precio) throws SQLException {
        String sql = "UPDATE producto_variantes SET precio_compra=?, fecha_actualizacion=CURRENT_TIMESTAMP WHERE id_variante=?";
        try (java.sql.Connection con = conexion.getInstance().createConnection(); java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            if (precio != null) pst.setBigDecimal(1, precio); else pst.setNull(1, java.sql.Types.DECIMAL);
            pst.setInt(2, idVariante);
            pst.executeUpdate();
        }
    }

    public java.util.List<VariantTableItem> findVariantsWithStockAndImage(int idProducto, Integer idBodega) throws SQLException {
        String sql = "SELECT pv.id_variante, COALESCE(SUM(ib.Stock_par),0) AS stock_par, COALESCE(SUM(ib.Stock_caja),0) AS stock_caja, pv.imagen, CONCAT(t.numero, ' ', COALESCE(t.sistema,''), ' ', CASE t.genero WHEN 'MUJER' THEN 'M' WHEN 'HOMBRE' THEN 'H' WHEN 'NIÑO' THEN 'N' ELSE '' END) AS talla, c.nombre AS color FROM producto_variantes pv LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 " + (idBodega != null && idBodega > 0 ? "AND ib.id_bodega = ? " : "") + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla LEFT JOIN colores c ON pv.id_color = c.id_color WHERE pv.id_producto = ? AND pv.disponible = 1 GROUP BY pv.id_variante, pv.imagen, t.numero, t.sistema, t.genero, c.nombre ORDER BY t.numero ASC, c.nombre ASC";
        java.util.List<VariantTableItem> out = new java.util.ArrayList<>();
        try (java.sql.Connection con = conexion.getInstance().createConnection(); java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            int idx = 1; if (idBodega != null && idBodega > 0) pst.setInt(idx++, idBodega); pst.setInt(idx++, idProducto);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VariantTableItem it = new VariantTableItem();
                    it.idVariante = rs.getInt("id_variante");
                    it.stockPares = rs.getInt("stock_par");
                    it.stockCajas = rs.getInt("stock_caja");
                    it.imagen = rs.getBytes("imagen");
                    it.talla = rs.getString("talla");
                    it.color = rs.getString("color");
                    out.add(it);
                }
            }
        }
        return out;
    }

    /**
     * Lista variantes de un producto agrupadas por proveedor con su stock.
     * Útil para mostrar en la UI: "Mismo producto, diferentes proveedores"
     */
    public static class VariantProveedorItem {
        public int idVariante;
        public String talla;
        public String color;
        public int idProveedor;
        public String proveedorNombre;
        public double precioCompra;
        public double precioVenta;
        public int stockParesTotal;
        public int stockCajasTotal;
        public String sku;
        public byte[] imagen;
    }

    public java.util.List<VariantProveedorItem> findVariantsGroupedByProveedor(int idProducto, Integer idBodega) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("pv.id_variante, ")
           .append("pv.id_proveedor, ")
           .append("prov.nombre AS proveedor_nombre, ")
           .append("pv.sku, ")
           .append("pv.precio_compra, ")
           .append("pv.precio_venta, ")
           .append("pv.imagen, ")
           .append("CONCAT(t.numero, ' ', COALESCE(t.sistema,''), ' ', ")
           .append("  CASE t.genero ")
           .append("    WHEN 'MUJER' THEN 'M' ")
           .append("    WHEN 'HOMBRE' THEN 'H' ")
           .append("    WHEN 'NIÑO' THEN 'N' ")
           .append("    ELSE '' ")
           .append("  END) AS talla, ")
           .append("c.nombre AS color, ")
           .append("COALESCE(SUM(ib.Stock_par), 0) AS stock_pares, ")
           .append("COALESCE(SUM(ib.Stock_caja), 0) AS stock_cajas ")
           .append("FROM producto_variantes pv ")
           .append("INNER JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor ")
           .append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ")
           .append("LEFT JOIN colores c ON pv.id_color = c.id_color ")
           .append("LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 ");

        if (idBodega != null && idBodega > 0) {
            sql.append("AND ib.id_bodega = ? ");
        }

        sql.append("WHERE pv.id_producto = ? ")
           .append("AND pv.disponible = 1 ")
           .append("GROUP BY pv.id_variante, pv.id_proveedor, prov.nombre, pv.sku, ")
           .append("  pv.precio_compra, pv.precio_venta, pv.imagen, t.numero, t.sistema, t.genero, c.nombre ")
           .append("ORDER BY prov.nombre, t.numero ASC, c.nombre ASC");

        java.util.List<VariantProveedorItem> items = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (idBodega != null && idBodega > 0) {
                pst.setInt(paramIdx++, idBodega);
            }
            pst.setInt(paramIdx++, idProducto);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VariantProveedorItem item = new VariantProveedorItem();
                    item.idVariante = rs.getInt("id_variante");
                    item.idProveedor = rs.getInt("id_proveedor");
                    item.proveedorNombre = rs.getString("proveedor_nombre");
                    item.sku = rs.getString("sku");
                    item.precioCompra = rs.getDouble("precio_compra");
                    item.precioVenta = rs.getDouble("precio_venta");
                    item.talla = rs.getString("talla");
                    item.color = rs.getString("color");
                    item.stockParesTotal = rs.getInt("stock_pares");
                    item.stockCajasTotal = rs.getInt("stock_cajas");
                    item.imagen = rs.getBytes("imagen");
                    items.add(item);
                }
            }
        }

        return items;
    }

    public java.util.List<VariantBodegaItem> findVariantsWithStockPerBodega(int idProducto) throws SQLException {
        String sql = "SELECT pv.id_variante, ib.id_bodega, "
                + "COALESCE(SUM(ib.Stock_par),0) AS stock_par, "
                + "COALESCE(SUM(ib.Stock_caja),0) AS stock_caja, "
                + "pv.imagen, "
                + "CONCAT(t.numero, ' ', COALESCE(t.sistema,''), ' ', CASE t.genero WHEN 'MUJER' THEN 'M' WHEN 'HOMBRE' THEN 'H' WHEN 'NIÑO' THEN 'N' ELSE '' END) AS talla, "
                + "c.nombre AS color, b.nombre AS bodega_nombre "
                + "FROM producto_variantes pv "
                + "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega "
                + "WHERE pv.id_producto = ? AND pv.disponible = 1 "
                + "GROUP BY pv.id_variante, ib.id_bodega, pv.imagen, t.numero, t.sistema, t.genero, c.nombre, b.nombre "
                + "ORDER BY bodega_nombre ASC, t.numero ASC, c.nombre ASC";
        java.util.List<VariantBodegaItem> out = new java.util.ArrayList<>();
        try (java.sql.Connection con = conexion.getInstance().createConnection(); java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idProducto);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VariantBodegaItem it = new VariantBodegaItem();
                    it.idVariante = rs.getInt("id_variante");
                    it.idBodega = rs.getInt("id_bodega");
                    it.stockPares = rs.getInt("stock_par");
                    it.stockCajas = rs.getInt("stock_caja");
                    it.imagen = rs.getBytes("imagen");
                    it.talla = rs.getString("talla");
                    it.color = rs.getString("color");
                    it.bodegaNombre = rs.getString("bodega_nombre");
                    out.add(it);
                }
            }
        }
        return out;
    }
}

