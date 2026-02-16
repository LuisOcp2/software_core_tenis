package raven.clases.productos;

import java.sql.*;
import java.util.*;
import raven.clases.comun.GenericPaginationService;
import raven.controlador.productos.ModelProductVariant;

public class VariantPaginationAdapter {
    private final GenericPaginationService paginationService;

    public VariantPaginationAdapter() { this.paginationService = new GenericPaginationService(); }

    public GenericPaginationService.PagedResult<ModelProductVariant> getVariantsPagedAdvanced(
            Integer productId, Integer colorId, Integer sizeId, Integer warehouseId,
            String searchTerm, Boolean hasStock, Boolean activeOnly,
            int page, int pageSize) throws SQLException {

        Map<String, Object> filters = new HashMap<>();
        List<Object> params = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");

        if (productId != null && productId > 0) { whereClause.append(" AND pv.id_producto = ?"); params.add(productId); filters.put("product", productId); }
        if (colorId != null && colorId > 0) { whereClause.append(" AND pv.id_color = ?"); params.add(colorId); filters.put("color", colorId); }
        if (sizeId != null && sizeId > 0) { whereClause.append(" AND pv.id_talla = ?"); params.add(sizeId); filters.put("size", sizeId); }
        if (warehouseId != null && warehouseId > 0) { whereClause.append(" AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1)"); params.add(warehouseId); filters.put("warehouse", warehouseId); }
        if (activeOnly != null) { whereClause.append(" AND pv.disponible = ?"); params.add(activeOnly); filters.put("active", activeOnly); }
        if (hasStock != null) {
            if (hasStock) {
                whereClause.append(" AND EXISTS (SELECT 1 FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1 AND (COALESCE(ib2.Stock_par,0) > 0 OR COALESCE(ib2.Stock_caja,0) > 0))");
            } else {
                whereClause.append(" AND NOT EXISTS (SELECT 1 FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1 AND (COALESCE(ib2.Stock_par,0) > 0 OR COALESCE(ib2.Stock_caja,0) > 0))");
            }
            filters.put("hasStock", hasStock);
        }
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            whereClause.append(" AND (LOWER(pv.sku) LIKE ? OR LOWER(pv.ean) LIKE ? OR LOWER(c.nombre) LIKE ? OR LOWER(t.numero) LIKE ?)");
            String sp = "%" + searchTerm.toLowerCase() + "%"; params.add(sp); params.add(sp); params.add(sp); params.add(sp); filters.put("search", searchTerm);
        }

        GenericPaginationService.QueryConfig config = GenericPaginationService.QueryConfig.builder(
                buildVariantQuery(whereClause.toString()),
                buildVariantCountQuery(whereClause.toString()),
                (rs, index) -> {
                    try { return mapVariantFromResultSet(rs); } catch (SQLException ex) { throw new RuntimeException(ex); }
                }
        ).withParams(params.toArray()).withFilter("advanced", true).orderBy("ORDER BY pv.sku ASC").build();

        return paginationService.executePagedQuery(config, page, pageSize);
    }

    public GenericPaginationService.PagedResult<ModelProductVariant> searchVariantsFast(
            String searchTerm, Integer warehouseId, int page, int pageSize) throws SQLException {

        List<Object> params = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder("WHERE pv.disponible = 1");

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            whereClause.append(" AND (LOWER(pv.sku) LIKE ? OR LOWER(pv.ean) LIKE ?)");
            String sp = "%" + searchTerm.toLowerCase() + "%"; params.add(sp); params.add(sp);
        }
        if (warehouseId != null && warehouseId > 0) { whereClause.append(" AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1)"); params.add(warehouseId); }

        String selectFields = "pv.*, " +
                "(SELECT COALESCE(SUM(ib2.Stock_par),0) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1) AS stock_por_pares, " +
                "(SELECT COALESCE(SUM(ib3.Stock_caja),0) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1) AS stock_por_cajas, " +
                "t.numero AS talla_nombre, c.nombre AS color_nombre, p.nombre AS producto_nombre";

        return paginationService.paginate(
                "producto_variantes pv LEFT JOIN tallas t ON pv.id_talla = t.id_talla LEFT JOIN colores c ON pv.id_color = c.id_color LEFT JOIN productos p ON pv.id_producto = p.id_producto",
                selectFields,
                whereClause.toString(),
                "ORDER BY pv.sku ASC",
                params,
                (rs, index) -> {
                    try { return mapVariantFromResultSet(rs); } catch (SQLException ex) { throw new RuntimeException(ex); }
                },
                page,
                pageSize
        );
    }

    public GenericPaginationService.PagedResult<ModelProductVariant> getLowStockVariantsPaged(
            Integer productId, Integer warehouseId, int minStock, int page, int pageSize) throws SQLException {

        List<Object> params = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(
                "WHERE pv.disponible = 1 AND (" +
                "COALESCE((SELECT SUM(ib2.Stock_par) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1),0) + " +
                "COALESCE((SELECT SUM(ib3.Stock_caja) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1),0) * 24" +
                ") <= ?");
        params.add(minStock);
        if (productId != null && productId > 0) { whereClause.append(" AND pv.id_producto = ?"); params.add(productId); }
        if (warehouseId != null && warehouseId > 0) { whereClause.append(" AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1)"); params.add(warehouseId); }

        String selectFields = "pv.*, " +
                "(SELECT COALESCE(SUM(ib2.Stock_par),0) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1) AS stock_por_pares, " +
                "(SELECT COALESCE(SUM(ib3.Stock_caja),0) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1) AS stock_por_cajas, " +
                "t.numero AS talla_nombre, c.nombre AS color_nombre, p.nombre AS producto_nombre";

        return paginationService.paginate(
                "producto_variantes pv LEFT JOIN tallas t ON pv.id_talla = t.id_talla LEFT JOIN colores c ON pv.id_color = c.id_color LEFT JOIN productos p ON pv.id_producto = p.id_producto",
                selectFields,
                whereClause.toString(),
                "ORDER BY (" +
                "COALESCE((SELECT SUM(ib2.Stock_par) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1),0) + " +
                "COALESCE((SELECT SUM(ib3.Stock_caja) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1),0) * 24" +
                ") ASC",
                params,
                (rs, index) -> {
                    try { return mapVariantFromResultSet(rs); } catch (SQLException ex) { throw new RuntimeException(ex); }
                },
                page,
                pageSize
        );
    }

    public GenericPaginationService.PagedResult<ModelProductVariant> getVariantsWithoutImagePaged(
            Integer productId, Integer warehouseId, int page, int pageSize) throws SQLException {

        List<Object> params = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder("WHERE pv.disponible = 1 AND (pv.imagen IS NULL OR LENGTH(pv.imagen) = 0)");
        if (productId != null && productId > 0) { whereClause.append(" AND pv.id_producto = ?"); params.add(productId); }
        if (warehouseId != null && warehouseId > 0) { whereClause.append(" AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1)"); params.add(warehouseId); }

        String selectFields = "pv.*, " +
                "(SELECT COALESCE(SUM(ib2.Stock_par),0) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1) AS stock_por_pares, " +
                "(SELECT COALESCE(SUM(ib3.Stock_caja),0) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1) AS stock_por_cajas, " +
                "t.numero AS talla_nombre, c.nombre AS color_nombre, p.nombre AS producto_nombre";

        return paginationService.paginate(
                "producto_variantes pv LEFT JOIN tallas t ON pv.id_talla = t.id_talla LEFT JOIN colores c ON pv.id_color = c.id_color LEFT JOIN productos p ON pv.id_producto = p.id_producto",
                selectFields,
                whereClause.toString(),
                "ORDER BY p.nombre, t.numero, c.nombre",
                params,
                (rs, index) -> {
                    try { return mapVariantFromResultSet(rs); } catch (SQLException ex) { throw new RuntimeException(ex); }
                },
                page,
                pageSize
        );
    }

    public GenericPaginationService.PagedResult<StockReportItem> getStockReportPaged(
            Integer warehouseId, boolean groupByProduct, int page, int pageSize) throws SQLException {

        List<Object> params = new ArrayList<>();
        String selectFields; String groupBy;
        if (groupByProduct) {
            selectFields = "pv.id_producto, p.nombre AS producto_nombre, p.codigo_modelo, COUNT(*) AS total_variants, " +
                    "SUM((SELECT COALESCE(SUM(ib2.Stock_par),0) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1)) AS total_pairs, " +
                    "SUM((SELECT COALESCE(SUM(ib3.Stock_caja),0) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1)) AS total_boxes, " +
                    "AVG(pv.precio_venta) AS avg_price";
            groupBy = "GROUP BY pv.id_producto, p.nombre, p.codigo_modelo ORDER BY p.nombre";
        } else {
            selectFields = "pv.*, " +
                    "(SELECT COALESCE(SUM(ib2.Stock_par),0) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1) AS stock_por_pares, " +
                    "(SELECT COALESCE(SUM(ib3.Stock_caja),0) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1) AS stock_por_cajas, " +
                    "t.numero AS talla_nombre, c.nombre AS color_nombre, p.nombre AS producto_nombre, p.codigo_modelo AS producto_codigo";
            groupBy = "ORDER BY p.nombre, t.numero, c.nombre";
        }
        StringBuilder whereClause = new StringBuilder("WHERE pv.disponible = 1");
        if (warehouseId != null && warehouseId > 0) { whereClause.append(" AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1)"); params.add(warehouseId); }

        GenericPaginationService.QueryConfig config = GenericPaginationService.QueryConfig.builder(
                buildStockQuery(whereClause.toString(), selectFields, groupBy),
                buildStockCountQuery(whereClause.toString(), groupByProduct),
                (rs, index) -> {
                    try { return mapStockReportFromResultSet(rs, groupByProduct); } catch (SQLException ex) { throw new RuntimeException(ex); }
                }
        ).withParams(params.toArray()).withFilter("stockReport", true).orderBy("ORDER BY p.nombre, t.numero, c.nombre").build();

        return paginationService.executePagedQuery(config, page, pageSize);
    }

    public List<ModelProductVariant> getVariantsByProductLegacy(int productId, int page, int pageSize) throws SQLException {
        GenericPaginationService.PagedResult<ModelProductVariant> result = getVariantsPagedAdvanced(
                productId, null, null, null, null, null, true, page, pageSize
        );
        return result.getData();
    }

    public static class StockReportItem {
        private final Integer productId; private final String productName; private final String modelCode; private final Integer variantId; private final String colorName; private final String sizeName; private final int totalPairs; private final int totalBoxes; private final double averagePrice; private final int totalVariants;
        public StockReportItem(Integer productId, String productName, String modelCode, Integer variantId,
                               String colorName, String sizeName, int totalPairs, int totalBoxes, double averagePrice) {
            this.productId = productId; this.productName = productName; this.modelCode = modelCode; this.variantId = variantId; this.colorName = colorName; this.sizeName = sizeName; this.totalPairs = totalPairs; this.totalBoxes = totalBoxes; this.averagePrice = averagePrice; this.totalVariants = 1; }
        public StockReportItem(Integer productId, String productName, String modelCode, int totalVariants,
                               int totalPairs, int totalBoxes, double averagePrice) {
            this.productId = productId; this.productName = productName; this.modelCode = modelCode; this.variantId = null; this.colorName = null; this.sizeName = null; this.totalPairs = totalPairs; this.totalBoxes = totalBoxes; this.averagePrice = averagePrice; this.totalVariants = totalVariants; }
        public Integer getProductId() { return productId; } public String getProductName() { return productName; } public String getModelCode() { return modelCode; } public Integer getVariantId() { return variantId; } public String getColorName() { return colorName; } public String getSizeName() { return sizeName; } public int getTotalPairs() { return totalPairs; } public int getTotalBoxes() { return totalBoxes; } public double getAveragePrice() { return averagePrice; } public int getTotalVariants() { return totalVariants; }
        public String getDisplayName() { if (variantId == null) { return String.format("%s (%d variantes)", productName, totalVariants); } else { return String.format("%s - %s %s", productName, sizeName, colorName); } }
        @Override public String toString() { return String.format("StockReportItem{product=%s, pairs=%d, boxes=%d, variants=%d}", productName, totalPairs, totalBoxes, totalVariants); }
    }

    private String buildVariantQuery(String whereClause) {
        return "SELECT pv.*, " +
                "(SELECT COALESCE(SUM(ib2.Stock_par),0) FROM inventario_bodega ib2 WHERE ib2.id_variante = pv.id_variante AND ib2.activo = 1) AS stock_por_pares, " +
                "(SELECT COALESCE(SUM(ib3.Stock_caja),0) FROM inventario_bodega ib3 WHERE ib3.id_variante = pv.id_variante AND ib3.activo = 1) AS stock_por_cajas, " +
                "t.numero AS talla_nombre, c.nombre AS color_nombre, p.nombre AS producto_nombre, p.codigo_modelo AS producto_codigo " +
                "FROM producto_variantes pv " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "LEFT JOIN productos p ON pv.id_producto = p.id_producto " +
                whereClause;
    }
    private String buildVariantCountQuery(String whereClause) {
        return "SELECT COUNT(*) FROM producto_variantes pv LEFT JOIN productos p ON pv.id_producto = p.id_producto " + whereClause;
    }
    private String buildStockQuery(String whereClause, String selectFields, String groupBy) {
        return "SELECT " + selectFields + " FROM producto_variantes pv LEFT JOIN tallas t ON pv.id_talla = t.id_talla LEFT JOIN colores c ON pv.id_color = c.id_color LEFT JOIN productos p ON pv.id_producto = p.id_producto " + whereClause + " " + groupBy;
    }
    private String buildStockCountQuery(String whereClause, boolean groupByProduct) {
        if (groupByProduct) { return "SELECT COUNT(DISTINCT pv.id_producto) FROM producto_variantes pv LEFT JOIN productos p ON pv.id_producto = p.id_producto " + whereClause; }
        else { return "SELECT COUNT(*) FROM producto_variantes pv LEFT JOIN productos p ON pv.id_producto = p.id_producto " + whereClause; }
    }
    private ModelProductVariant mapVariantFromResultSet(ResultSet rs) throws SQLException {
        ModelProductVariant variant = new ModelProductVariant();
        variant.setVariantId(rs.getInt("id_variante"));
        variant.setProductId(rs.getInt("id_producto"));
        variant.setSizeId(rs.getInt("id_talla"));
        variant.setColorId(rs.getInt("id_color"));
        variant.setSku(rs.getString("sku"));
        variant.setEan(rs.getString("ean"));
        variant.setStockPairs(rs.getInt("stock_por_pares"));
        variant.setStockBoxes(rs.getInt("stock_por_cajas"));
        variant.setAvailable(rs.getBoolean("disponible"));
        try { variant.setWarehouseId(rs.getInt("id_bodega")); } catch (SQLException ignore) {}
        try { variant.setPurchasePrice(rs.getDouble("precio_compra")); } catch (SQLException e) { variant.setPurchasePrice(0.0); }
        try { variant.setSalePrice(rs.getDouble("precio_venta")); } catch (SQLException e) { variant.setSalePrice(0.0); }
        variant.setSizeName(rs.getString("talla_nombre"));
        variant.setColorName(rs.getString("color_nombre"));
        return variant;
    }
    private StockReportItem mapStockReportFromResultSet(ResultSet rs, boolean groupByProduct) throws SQLException {
        if (groupByProduct) { return new StockReportItem(rs.getInt("id_producto"), rs.getString("producto_nombre"), rs.getString("codigo_modelo"), rs.getInt("total_variants"), rs.getInt("total_pairs"), rs.getInt("total_boxes"), rs.getDouble("avg_price")); }
        else { return new StockReportItem(rs.getInt("id_producto"), rs.getString("producto_nombre"), rs.getString("producto_codigo"), rs.getInt("id_variante"), rs.getString("color_nombre"), rs.getString("talla_nombre"), rs.getInt("stock_por_pares"), rs.getInt("stock_por_cajas"), rs.getDouble("precio_venta")); }
    }

    public void clearCache() { paginationService.clearCache(); }
    public String getPaginationStats() { return paginationService.getCacheStats(); }
}
