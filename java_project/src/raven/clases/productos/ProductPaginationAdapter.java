package raven.clases.productos;

import raven.application.form.productos.buscador.ProductoBusquedaItem;
import java.sql.*;
import java.util.*;
import raven.clases.comun.GenericPaginationService;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelBrand;
import raven.controlador.comercial.ModelSupplier;

/**
 * Adaptador para manejar la paginación de productos de forma eficiente.
 * 
 * CAMBIOS REALIZADOS (Optimización y Corrección):
 * 1. Implementación de patrón Fallback: Intenta usar Stored Procedure
 * optimizado, y si falla, usa SQL nativo.
 * 2. Integración con GenericPaginationService para manejo centralizado de
 * PagedResult.
 * 3. Soporte para filtros avanzados (Categoría, Marca, Color, Talla) en ambos
 * métodos (SP y SQL).
 * 4. Optimización de conteo de filas y cálculo de páginas.
 * 5. Uso de caché para evitar consultas repetitivas (implementado en
 * GenericPaginationService).
 * 
 * @author Raven
 */
public class ProductPaginationAdapter {
    private final GenericPaginationService paginationService;
    private final ServiceProductOptimized serviceOptimized;

    public ProductPaginationAdapter() {
        this.paginationService = new GenericPaginationService(new GenericPaginationService.CacheStrategy() {
            private final java.util.Map<String, CachedItem> cache = new java.util.concurrent.ConcurrentHashMap<>();
            private static final long DEFAULT_TTL = 60000; // 1 minute

            class CachedItem {
                Object data;
                long timestamp;
                long ttl;

                CachedItem(Object data, long ttl) {
                    this.data = data;
                    this.ttl = ttl > 0 ? ttl : DEFAULT_TTL;
                    this.timestamp = System.currentTimeMillis();
                }

                boolean isExpired() {
                    return System.currentTimeMillis() - timestamp > ttl;
                }
            }

            @Override
            public String generateKey(String entityType, java.util.Map<String, Object> filters, int pageN,
                    int pageSizeN) {
                StringBuilder sb = new StringBuilder();
                sb.append(entityType).append(":");
                sb.append(pageN).append(":").append(pageSizeN).append(":");
                if (filters != null) {
                    filters.entrySet().stream()
                            .filter(e -> e.getValue() != null)
                            .sorted(java.util.Map.Entry.comparingByKey())
                            .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append(";"));
                }
                return sb.toString();
            }

            @Override
            public Object get(String key) {
                CachedItem item = cache.get(key);
                if (item != null) {
                    if (item.isExpired()) {
                        cache.remove(key);
                        return null;
                    }
                    return item.data;
                }
                return null;
            }

            @Override
            public void put(String key, Object data, long ttlMs) {
                cache.put(key, new CachedItem(data, ttlMs));
            }

            @Override
            public void invalidate(String key) {
                cache.remove(key);
            }

            @Override
            public void invalidatePattern(String pattern) {
                if (pattern == null || pattern.equals("*")) {
                    cache.clear();
                } else {
                    cache.keySet().removeIf(k -> k.contains(pattern));
                }
            }
        });
        this.serviceOptimized = new ServiceProductOptimized();
    }

    public void clearCache() {
        paginationService.invalidateCache("*");
    }

    public GenericPaginationService.PagedResult<ModelProduct> getProductsPagedAdvanced(
            String searchTerm,
            Integer categoryId,
            Integer brandId,
            Integer colorId,
            Integer sizeId,
            Integer warehouseId,
            boolean activeOnly,
            String stockTypeFilter,
            int page,
            int pageSize) throws SQLException {

        try {
            return getProductsPagedInternal(searchTerm, categoryId, brandId, colorId, sizeId, warehouseId, activeOnly,
                    stockTypeFilter, page, pageSize, true);
        } catch (SQLException e) {
            // Si falla el procedimiento almacenado o búsqueda FULLTEXT, intentar fallback
            System.err.println(
                    "Advertencia: Falló búsqueda optimizada (" + e.getMessage() + "). Intentando método alternativo.");
            try {
                return getProductsPagedInternal(searchTerm, categoryId, brandId, colorId, sizeId, warehouseId,
                        activeOnly, stockTypeFilter, page, pageSize, false);
            } catch (SQLException ex2) {
                // Si también falla el fallback, lanzar la excepción original
                throw e;
            }
        }
    }

    private GenericPaginationService.PagedResult<ModelProduct> getProductsPagedInternal(
            String searchTerm,
            Integer categoryId,
            Integer brandId,
            Integer colorId,
            Integer sizeId,
            Integer warehouseId,
            boolean activeOnly,
            String stockTypeFilter,
            int page,
            int pageSize,
            boolean useStoredProcedure) throws SQLException {

        // FORZAMOS el uso de la búsqueda avanzada en Java (Intento 2)
        // Esto corrige el problema de que el SP filtra productos sin variantes (INNER
        // JOIN)
        // y necesitamos mostrar productos aunque no tengan variantes ni stock.
        if (false && useStoredProcedure) {
            // INTENTO 1: Usar Procedimiento Almacenado (Más rápido)
            try {
                return executeStoredProcedureSearch(searchTerm, categoryId, brandId, colorId, sizeId, warehouseId,
                        activeOnly, stockTypeFilter, page, pageSize);
            } catch (SQLException e) {
                // Si falla el SP (por ejemplo, no existe o error de sintaxis), lanzar para que
                // el caller haga fallback
                throw e;
            }
        } else {
            // INTENTO 2: Usar SQL Construido en Java (Fallback seguro y permite productos
            // sin variantes)
            // Nota: Aquí pasamos useFullText=false para evitar errores de índices
            return executeAdvancedSearch(searchTerm, categoryId, brandId, colorId, sizeId, warehouseId, activeOnly,
                    stockTypeFilter, page, pageSize, false);
        }
    }

    private GenericPaginationService.PagedResult<ModelProduct> executeStoredProcedureSearch(
            String searchTerm, Integer categoryId, Integer brandId,
            Integer colorId, Integer sizeId, Integer warehouseId,
            boolean activeOnly, String stockTypeFilter,
            int page, int pageSize) throws SQLException {

        // Generar clave de caché
        Map<String, Object> filters = new HashMap<>();
        if (searchTerm != null)
            filters.put("search", searchTerm);
        if (categoryId != null)
            filters.put("category", categoryId);
        if (brandId != null)
            filters.put("brand", brandId);
        if (colorId != null)
            filters.put("color", colorId);
        if (sizeId != null)
            filters.put("size", sizeId);
        if (warehouseId != null)
            filters.put("warehouse", warehouseId);
        filters.put("active", activeOnly);
        if (stockTypeFilter != null)
            filters.put("stockType", stockTypeFilter);

        String cacheKey = paginationService.getCacheStrategy().generateKey("product_sp", filters, page, pageSize);

        // Verificar caché
        @SuppressWarnings("unchecked")
        GenericPaginationService.PagedResult<ModelProduct> cached = (GenericPaginationService.PagedResult<ModelProduct>) paginationService
                .getCacheStrategy().get(cacheKey);
        if (cached != null)
            return cached;

        List<ModelProduct> list = new ArrayList<>();
        int totalRows = 0;

        try (Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                CallableStatement cs = con
                        .prepareCall("{call sp_buscar_productos_bodega_paginado(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {

            // Configurar parámetros
            cs.setObject(1, (warehouseId != null && warehouseId > 0) ? warehouseId : null, Types.INTEGER);
            cs.setString(2, (searchTerm != null && !searchTerm.trim().isEmpty()) ? searchTerm : null);
            cs.setString(3, (stockTypeFilter != null && !stockTypeFilter.trim().isEmpty()) ? stockTypeFilter : null);
            cs.setObject(4, (categoryId != null && categoryId > 0) ? categoryId : null, Types.INTEGER);
            cs.setObject(5, (brandId != null && brandId > 0) ? brandId : null, Types.INTEGER);
            cs.setObject(6, (colorId != null && colorId > 0) ? colorId : null, Types.INTEGER);
            cs.setObject(7, (sizeId != null && sizeId > 0) ? sizeId : null, Types.INTEGER);
            cs.setInt(8, pageSize);
            cs.setInt(9, (page - 1) * pageSize);
            cs.registerOutParameter(10, Types.INTEGER);

            boolean hasResults = cs.execute();

            // Obtener total de filas
            totalRows = cs.getInt(10);

            if (hasResults) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        list.add(mapOptimizedProductFromResultSet(rs));
                    }
                }
            }
        }

        GenericPaginationService.PagedResult<ModelProduct> result = new GenericPaginationService.PagedResult<>(list,
                totalRows, page, pageSize, false, filters);

        // Guardar en caché (1 minuto)
        paginationService.getCacheStrategy().put(cacheKey, result, 60000);

        return result;
    }

    private GenericPaginationService.PagedResult<ModelProduct> executeOptimizedSimpleSearch(
            String searchTerm, Integer warehouseId, boolean activeOnly, int page, int pageSize, boolean useFullText)
            throws SQLException {

        List<Object> params = new ArrayList<>();
        boolean hasSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty());

        StringBuilder selectQuery = new StringBuilder();
        selectQuery.append("SELECT ");
        selectQuery.append("    p.id_producto, p.codigo_modelo, p.nombre, p.descripcion, ");
        selectQuery.append("    p.precio_compra, p.precio_venta, p.stock_minimo, p.genero, ");
        selectQuery.append("    p.ubicacion, p.pares_por_caja, p.activo, ");
        selectQuery.append("    c.nombre AS categoria_nombre, m.nombre AS marca_nombre, ");
        selectQuery.append(
                "    (SELECT GROUP_CONCAT(DISTINCT col.nombre ORDER BY col.nombre SEPARATOR ', ') FROM producto_variantes pv2 JOIN colores col ON pv2.id_color = col.id_color WHERE pv2.id_producto = p.id_producto AND pv2.disponible = 1) AS colores, ");
        selectQuery.append(
                "    (SELECT GROUP_CONCAT(DISTINCT t.numero ORDER BY CAST(t.numero AS UNSIGNED) SEPARATOR ', ') FROM producto_variantes pv3 JOIN tallas t ON pv3.id_talla = t.id_talla WHERE pv3.id_producto = p.id_producto AND pv3.disponible = 1) AS tallas, ");

        // OPTIMIZACION: Subqueries para stock en lugar de JOINs masivos
        String warehouseCondition = "";
        if (warehouseId != null && warehouseId > 0) {
            warehouseCondition = " AND ib.id_bodega = ? ";
        }

        selectQuery.append(
                "    (SELECT COALESCE(SUM(ib.Stock_par), 0) FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND pv.disponible = 1")
                .append(warehouseCondition).append(") AS total_pares, ");
        if (warehouseId != null && warehouseId > 0)
            params.add(warehouseId);

        selectQuery.append(
                "    (SELECT COALESCE(SUM(ib.Stock_caja), 0) FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND pv.disponible = 1")
                .append(warehouseCondition).append(") AS total_cajas");
        if (warehouseId != null && warehouseId > 0)
            params.add(warehouseId);

        if (hasSearchTerm) {
            if (useFullText) {
                selectQuery.append(
                        ", MATCH(p.nombre, p.descripcion, p.codigo_modelo) AGAINST(? IN NATURAL LANGUAGE MODE) AS relevance ");
                params.add(searchTerm);
            } else {
                selectQuery.append(", 0 AS relevance ");
            }
        }

        selectQuery.append(" FROM productos p ");
        selectQuery.append("INNER JOIN categorias c ON p.id_categoria = c.id_categoria ");
        selectQuery.append("INNER JOIN marcas m ON p.id_marca = m.id_marca ");
        // OPTIMIZACION: Eliminados JOINs masivos que causaban lentitud

        selectQuery.append("WHERE p.activo = ? ");
        params.add(activeOnly);

        if (hasSearchTerm) {
            selectQuery.append("AND (");
            if (useFullText) {
                selectQuery.append(
                        "    MATCH(p.nombre, p.descripcion, p.codigo_modelo) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                params.add(searchTerm);
                selectQuery.append("    OR MATCH(m.nombre) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                params.add(searchTerm);
                selectQuery.append("    OR p.codigo_modelo LIKE ? ");
                params.add("%" + searchTerm + "%");
            } else {
                selectQuery.append("    p.nombre LIKE ? ");
                params.add("%" + searchTerm + "%");
                selectQuery.append("    OR p.descripcion LIKE ? ");
                params.add("%" + searchTerm + "%");
                selectQuery.append("    OR p.codigo_modelo LIKE ? ");
                params.add("%" + searchTerm + "%");
                selectQuery.append("    OR m.nombre LIKE ? ");
                params.add("%" + searchTerm + "%");
            }
            selectQuery.append(") ");
        }

        // GROUP BY ya no es necesario sin los JOINs 1:N
        // selectQuery.append("GROUP BY p.id_producto ");

        String orderBy = (hasSearchTerm && useFullText) ? "ORDER BY relevance DESC, p.nombre ASC"
                : "ORDER BY p.nombre ASC";

        // --- QUERY COUNT OPTIMIZED ---
        StringBuilder countQuery = new StringBuilder();
        List<Object> countParams = new ArrayList<>();

        // If no search term and no warehouse filter, use simple count
        if (!hasSearchTerm && (warehouseId == null || warehouseId <= 0)) {
            countQuery.append("SELECT COUNT(*) FROM productos p WHERE p.activo = ?");
            countParams.add(activeOnly);
        } else {
            countQuery.append("SELECT COUNT(DISTINCT p.id_producto) FROM productos p ");
            // Only join what is necessary for filtering

            if (hasSearchTerm) {
                countQuery.append("INNER JOIN marcas m ON p.id_marca = m.id_marca ");
            }

            if (warehouseId != null && warehouseId > 0) {
                countQuery.append(
                        "INNER JOIN producto_variantes pv ON pv.id_producto = p.id_producto AND pv.disponible = 1 ");
                countQuery.append(
                        "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 AND ib.id_bodega = ? ");
                countParams.add(warehouseId);
            }

            countQuery.append("WHERE p.activo = ? ");
            countParams.add(activeOnly);

            if (hasSearchTerm) {
                countQuery.append("AND (");
                if (useFullText) {
                    countQuery.append(
                            "    MATCH(p.nombre, p.descripcion, p.codigo_modelo) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                    countParams.add(searchTerm);
                    countQuery.append("    OR MATCH(m.nombre) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                    countParams.add(searchTerm);
                    countQuery.append("    OR p.codigo_modelo LIKE ? ");
                    countParams.add("%" + searchTerm + "%");
                } else {
                    countQuery.append("    p.nombre LIKE ? ");
                    countParams.add("%" + searchTerm + "%");
                    countQuery.append("    OR p.descripcion LIKE ? ");
                    countParams.add("%" + searchTerm + "%");
                    countQuery.append("    OR p.codigo_modelo LIKE ? ");
                    countParams.add("%" + searchTerm + "%");
                    countQuery.append("    OR m.nombre LIKE ? ");
                    countParams.add("%" + searchTerm + "%");
                }
                countQuery.append(") ");
            }
        }

        GenericPaginationService.QueryConfig config = GenericPaginationService.QueryConfig.builder(
                selectQuery.toString(),
                countQuery.toString(),
                (rs, index) -> {
                    try {
                        return mapOptimizedProductFromResultSet(rs);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }).withParams(params.toArray())
                .withCountParams(countParams.toArray())
                .orderBy(orderBy)
                .build();

        return paginationService.executePagedQuery(config, page, pageSize);
    }

    /**
     * Búsqueda paginada de variantes (estilo Buscador Avanzado)
     */
    public GenericPaginationService.PagedResult<ProductoBusquedaItem> getVariantsPaged(
            String query, Integer warehouseId, String stockType, int page, int pageSize) throws SQLException {
        return getVariantsPaged(query, warehouseId, stockType, page, pageSize, null, null, null, null, null);
    }

    public GenericPaginationService.PagedResult<ProductoBusquedaItem> getVariantsPaged(
            String query, Integer warehouseId, String stockType, int page, int pageSize,
            String categoryName, String brandName, Double minPrice, Double maxPrice, List<String> stockLevels)
            throws SQLException {

        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();

        // Base Query
        StringBuilder selectSql = new StringBuilder(
                "SELECT DISTINCT pv.id_variante, pv.ean, p.nombre, t.numero AS talla, p.genero, " +
                        "COALESCE(ib.Stock_par, 0) as stock_pares, COALESCE(ib.Stock_caja, 0) as stock_cajas, " +
                        "b.nombre as nombre_bodega, m.nombre as marca, c.nombre as color, cat.nombre as categoria, p.id_producto "
                        +
                        "FROM productos p " +
                        "JOIN producto_variantes pv ON pv.id_producto = p.id_producto " +
                        "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                        "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                        "LEFT JOIN marcas m ON p.id_marca = m.id_marca " +
                        "LEFT JOIN categorias cat ON p.id_categoria = cat.id_categoria " +
                        "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.activo = 1 " +
                        "LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
                        "WHERE p.activo = 1 AND pv.disponible = 1 ");

        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(DISTINCT pv.id_variante) " +
                        "FROM productos p " +
                        "JOIN producto_variantes pv ON pv.id_producto = p.id_producto " +
                        "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                        "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                        "LEFT JOIN marcas m ON p.id_marca = m.id_marca " +
                        "LEFT JOIN categorias cat ON p.id_categoria = cat.id_categoria " +
                        "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.activo = 1 " +
                        "LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega " +
                        "WHERE p.activo = 1 AND pv.disponible = 1 ");

        // Warehouse Filter
        if (warehouseId != null && warehouseId > 0) {
            String wFilter = " AND ib.id_bodega = ? ";
            selectSql.append(wFilter);
            countSql.append(wFilter);
            params.add(warehouseId);
            countParams.add(warehouseId);
        }

        // Category Filter
        if (categoryName != null && !categoryName.isEmpty() && !"Todas".equalsIgnoreCase(categoryName)) {
            String catFilter = " AND cat.nombre = ? ";
            selectSql.append(catFilter);
            countSql.append(catFilter);
            params.add(categoryName);
            countParams.add(categoryName);
        }

        // Brand Filter
        if (brandName != null && !brandName.isEmpty() && !"Todas".equalsIgnoreCase(brandName)) {
            String brandFilter = " AND m.nombre = ? ";
            selectSql.append(brandFilter);
            countSql.append(brandFilter);
            params.add(brandName);
            countParams.add(brandName);
        }

        // Price Filter
        if (minPrice != null) {
            String minFilter = " AND pv.precio_venta >= ? ";
            selectSql.append(minFilter);
            countSql.append(minFilter);
            params.add(minPrice);
            countParams.add(minPrice);
        }
        if (maxPrice != null) {
            String maxFilter = " AND pv.precio_venta <= ? ";
            selectSql.append(maxFilter);
            countSql.append(maxFilter);
            params.add(maxPrice);
            countParams.add(maxPrice);
        }

        // Stock Levels Filter
        if (stockLevels != null && !stockLevels.isEmpty()) {
            StringBuilder stockFilter = new StringBuilder(" AND (");
            boolean first = true;
            for (String level : stockLevels) {
                if (!first)
                    stockFilter.append(" OR ");
                if ("Bajo".equalsIgnoreCase(level)) {
                    stockFilter.append("(COALESCE(ib.Stock_par, 0) < 10 AND COALESCE(ib.Stock_caja, 0) < 10)");
                } else if ("Medio".equalsIgnoreCase(level)) {
                    stockFilter.append(
                            "(COALESCE(ib.Stock_par, 0) BETWEEN 10 AND 50 OR COALESCE(ib.Stock_caja, 0) BETWEEN 10 AND 50)");
                } else if ("Alto".equalsIgnoreCase(level)) {
                    stockFilter.append("(COALESCE(ib.Stock_par, 0) > 50 OR COALESCE(ib.Stock_caja, 0) > 50)");
                }
                first = false;
            }
            stockFilter.append(") ");
            selectSql.append(stockFilter);
            countSql.append(stockFilter);
        }

        // Search Logic
        if (query != null && !query.trim().isEmpty()) {
            String[] parts = query.split(",");

            // Part 1: Name/General
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                String p1 = "%" + parts[0].trim() + "%";
                String filter = " AND (p.nombre LIKE ? OR m.nombre LIKE ? OR pv.ean LIKE ?) ";
                selectSql.append(filter);
                countSql.append(filter);
                params.add(p1);
                params.add(p1);
                params.add(p1);
                countParams.add(p1);
                countParams.add(p1);
                countParams.add(p1);
            }

            // Part 2: Color
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                String p2 = "%" + parts[1].trim() + "%";
                String filter = " AND (c.nombre LIKE ?) ";
                selectSql.append(filter);
                countSql.append(filter);
                params.add(p2);
                countParams.add(p2);
            }

            // Part 3: Size
            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                String p3 = "%" + parts[2].trim() + "%";
                String filter = " AND (t.numero LIKE ?) ";
                selectSql.append(filter);
                countSql.append(filter);
                params.add(p3);
                countParams.add(p3);
            }
        }

        String orderBy = " ORDER BY p.nombre ASC, t.numero ASC ";

        GenericPaginationService.QueryConfig config = GenericPaginationService.QueryConfig.builder(
                selectSql.toString(),
                countSql.toString(),
                (rs, rowNum) -> {
                    try {
                        int stockPares = rs.getInt("stock_pares");
                        int stockCajas = rs.getInt("stock_cajas");
                        int stock = stockPares > 0 ? stockPares : stockCajas;
                        String tipo = stockPares > 0 ? "Pares" : "Cajas";

                        if (stockType != null && "Cajas".equalsIgnoreCase(stockType)) {
                            stock = stockCajas;
                            tipo = "Cajas";
                        } else if (stockType != null && "Pares".equalsIgnoreCase(stockType)) {
                            stock = stockPares;
                            tipo = "Pares";
                        }

                        return new ProductoBusquedaItem(
                                rs.getInt("id_variante"),
                                rs.getString("ean"),
                                rs.getString("nombre"),
                                rs.getString("talla"),
                                rs.getString("genero"),
                                stock,
                                tipo,
                                rs.getString("nombre_bodega"),
                                rs.getString("marca"),
                                rs.getString("color"),
                                rs.getInt("id_producto"));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .withParams(params.toArray())
                .withCountParams(countParams.toArray())
                .orderBy(orderBy)
                .build();

        // Log de Query Generada
        System.out.println("SQL Generated for Variants Search:");
        System.out.println(selectSql.toString());
        System.out.println("Parameters: " + params);

        return paginationService.executePagedQuery(config, page, pageSize);
    }

    private GenericPaginationService.PagedResult<ModelProduct> executeAdvancedSearch(
            String searchTerm, Integer categoryId, Integer brandId,
            Integer colorId, Integer sizeId, Integer warehouseId,
            boolean activeOnly, String stockTypeFilter,
            int page, int pageSize, boolean useFullText) throws SQLException {

        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();

        boolean hasSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty());
        boolean hasStockFilter = (stockTypeFilter != null && !stockTypeFilter.trim().isEmpty());

        // --- SELECT QUERY BUILDER ---
        StringBuilder selectQuery = new StringBuilder();
        selectQuery.append("SELECT ");
        selectQuery.append("    p.id_producto, p.codigo_modelo, p.nombre, p.descripcion, ");
        selectQuery.append("    p.precio_compra, p.precio_venta, p.stock_minimo, p.genero, ");
        selectQuery.append("    p.ubicacion, p.pares_por_caja, p.activo, ");
        selectQuery.append("    p.id_categoria, p.id_marca, p.id_proveedor, ");
        selectQuery.append(
                "    c.nombre AS categoria_nombre, m.nombre AS marca_nombre, prov.nombre AS proveedor_nombre, ");

        // Subqueries para evitar GROUP BY y explosión de filas
        selectQuery.append(
                "    (SELECT GROUP_CONCAT(DISTINCT col.nombre ORDER BY col.nombre SEPARATOR ', ') FROM producto_variantes pv2 JOIN colores col ON pv2.id_color = col.id_color WHERE pv2.id_producto = p.id_producto AND pv2.disponible = 1) AS colores, ");
        selectQuery.append(
                "    (SELECT GROUP_CONCAT(DISTINCT t.numero ORDER BY CAST(t.numero AS UNSIGNED) SEPARATOR ', ') FROM producto_variantes pv3 JOIN tallas t ON pv3.id_talla = t.id_talla WHERE pv3.id_producto = p.id_producto AND pv3.disponible = 1) AS tallas, ");

        String warehouseCondition = "";
        if (warehouseId != null && warehouseId > 0) {
            warehouseCondition = " AND ib.id_bodega = ? ";
        }

        selectQuery.append(
                "    (SELECT COALESCE(SUM(ib.Stock_par), 0) FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND pv.disponible = 1")
                .append(warehouseCondition).append(") AS total_pares, ");
        if (warehouseId != null && warehouseId > 0)
            params.add(warehouseId);

        selectQuery.append(
                "    (SELECT COALESCE(SUM(ib.Stock_caja), 0) FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND pv.disponible = 1")
                .append(warehouseCondition).append(") AS total_cajas");
        if (warehouseId != null && warehouseId > 0)
            params.add(warehouseId);

        if (hasSearchTerm) {
            if (useFullText) {
                selectQuery.append(
                        ", MATCH(p.nombre, p.descripcion, p.codigo_modelo) AGAINST(? IN NATURAL LANGUAGE MODE) AS relevance ");
                params.add(searchTerm);
            } else {
                selectQuery.append(", 0 AS relevance ");
            }
        }

        selectQuery.append(" FROM productos p ");
        selectQuery.append("INNER JOIN categorias c ON p.id_categoria = c.id_categoria ");
        selectQuery.append("INNER JOIN marcas m ON p.id_marca = m.id_marca ");
        selectQuery.append("LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor ");
        // No JOINs con variantes/inventario en el nivel principal

        selectQuery.append("WHERE p.activo = ? ");
        params.add(activeOnly);

        if (categoryId != null && categoryId > 0) {
            selectQuery.append("AND p.id_categoria = ? ");
            params.add(categoryId);
        }

        if (brandId != null && brandId > 0) {
            selectQuery.append("AND p.id_marca = ? ");
            params.add(brandId);
        }

        // Filtros con EXISTS (más eficientes que JOINs)
        if (colorId != null && colorId > 0) {
            selectQuery.append(
                    "AND EXISTS (SELECT 1 FROM producto_variantes pv WHERE pv.id_producto = p.id_producto AND pv.id_color = ? AND pv.disponible = 1) ");
            params.add(colorId);
        }

        if (sizeId != null && sizeId > 0) {
            selectQuery.append(
                    "AND EXISTS (SELECT 1 FROM producto_variantes pv WHERE pv.id_producto = p.id_producto AND pv.id_talla = ? AND pv.disponible = 1) ");
            params.add(sizeId);
        }

        if (hasSearchTerm) {
            selectQuery.append("AND (");
            if (useFullText) {
                selectQuery.append(
                        "    MATCH(p.nombre, p.descripcion, p.codigo_modelo) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                params.add(searchTerm);
                selectQuery.append("    OR MATCH(m.nombre) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                params.add(searchTerm);
                selectQuery.append("    OR p.codigo_modelo LIKE ? ");
                params.add("%" + searchTerm + "%");
            } else {
                selectQuery.append("    p.nombre LIKE ? ");
                params.add("%" + searchTerm + "%");
                selectQuery.append("    OR p.descripcion LIKE ? ");
                params.add("%" + searchTerm + "%");
                selectQuery.append("    OR p.codigo_modelo LIKE ? ");
                params.add("%" + searchTerm + "%");
                selectQuery.append("    OR m.nombre LIKE ? ");
                params.add("%" + searchTerm + "%");
            }
            selectQuery.append(") ");
        }

        if (hasStockFilter) {
            String t = stockTypeFilter.trim().toLowerCase();
            // Usar EXISTS para verificar stock positivo es más rápido que sumar
            if ("pares".equals(t) || "par".equals(t)) {
                selectQuery.append(
                        "AND EXISTS (SELECT 1 FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND ib.Stock_par > 0");
                if (warehouseId != null && warehouseId > 0) {
                    selectQuery.append(" AND ib.id_bodega = ?");
                    params.add(warehouseId);
                }
                selectQuery.append(") ");
            } else if ("cajas".equals(t) || "caja".equals(t)) {
                selectQuery.append(
                        "AND EXISTS (SELECT 1 FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND ib.Stock_caja > 0");
                if (warehouseId != null && warehouseId > 0) {
                    selectQuery.append(" AND ib.id_bodega = ?");
                    params.add(warehouseId);
                }
                selectQuery.append(") ");
            }
        }

        String orderBy = (hasSearchTerm && useFullText) ? "ORDER BY relevance DESC, p.nombre ASC"
                : "ORDER BY p.nombre ASC";

        // --- COUNT QUERY BUILDER ---
        StringBuilder countQuery = new StringBuilder();
        // Usamos COUNT(*) simple porque ya no duplicamos filas
        countQuery.append("SELECT COUNT(*) FROM productos p ");
        countQuery.append("INNER JOIN marcas m ON p.id_marca = m.id_marca "); // Necesario si filtramos por marca o
                                                                              // buscamos en marca

        countQuery.append("WHERE p.activo = ? ");
        countParams.add(activeOnly);

        if (categoryId != null && categoryId > 0) {
            countQuery.append("AND p.id_categoria = ? ");
            countParams.add(categoryId);
        }

        if (brandId != null && brandId > 0) {
            countQuery.append("AND p.id_marca = ? ");
            countParams.add(brandId);
        }

        if (colorId != null && colorId > 0) {
            countQuery.append(
                    "AND EXISTS (SELECT 1 FROM producto_variantes pv WHERE pv.id_producto = p.id_producto AND pv.id_color = ? AND pv.disponible = 1) ");
            countParams.add(colorId);
        }

        if (sizeId != null && sizeId > 0) {
            countQuery.append(
                    "AND EXISTS (SELECT 1 FROM producto_variantes pv WHERE pv.id_producto = p.id_producto AND pv.id_talla = ? AND pv.disponible = 1) ");
            countParams.add(sizeId);
        }

        if (hasSearchTerm) {
            countQuery.append("AND (");
            if (useFullText) {
                countQuery.append(
                        "    MATCH(p.nombre, p.descripcion, p.codigo_modelo) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                countParams.add(searchTerm);
                countQuery.append("    OR MATCH(m.nombre) AGAINST(? IN NATURAL LANGUAGE MODE) ");
                countParams.add(searchTerm);
                countQuery.append("    OR p.codigo_modelo LIKE ? ");
                countParams.add("%" + searchTerm + "%");
            } else {
                countQuery.append("    p.nombre LIKE ? ");
                countParams.add("%" + searchTerm + "%");
                countQuery.append("    OR p.descripcion LIKE ? ");
                countParams.add("%" + searchTerm + "%");
                countQuery.append("    OR p.codigo_modelo LIKE ? ");
                countParams.add("%" + searchTerm + "%");
                countQuery.append("    OR m.nombre LIKE ? ");
                countParams.add("%" + searchTerm + "%");
            }
            countQuery.append(") ");
        }

        if (hasStockFilter) {
            String t = stockTypeFilter.trim().toLowerCase();
            if ("pares".equals(t) || "par".equals(t)) {
                countQuery.append(
                        "AND EXISTS (SELECT 1 FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND ib.Stock_par > 0");
                if (warehouseId != null && warehouseId > 0) {
                    countQuery.append(" AND ib.id_bodega = ?");
                    countParams.add(warehouseId);
                }
                countQuery.append(") ");
            } else if ("cajas".equals(t) || "caja".equals(t)) {
                countQuery.append(
                        "AND EXISTS (SELECT 1 FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante WHERE pv.id_producto = p.id_producto AND ib.activo = 1 AND ib.Stock_caja > 0");
                if (warehouseId != null && warehouseId > 0) {
                    countQuery.append(" AND ib.id_bodega = ?");
                    countParams.add(warehouseId);
                }
                countQuery.append(") ");
            }
        }

        GenericPaginationService.QueryConfig config = GenericPaginationService.QueryConfig.builder(
                selectQuery.toString(),
                countQuery.toString(),
                (rs, index) -> {
                    try {
                        return mapOptimizedProductFromResultSet(rs);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }).withParams(params.toArray())
                .withCountParams(countParams.toArray())
                .orderBy(orderBy)
                .build();

        return paginationService.executePagedQuery(config, page, pageSize);
    }

    private ModelProduct mapOptimizedProductFromResultSet(ResultSet rs) throws SQLException {
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

        // Mapeo completo de objetos relacionados para evitar consultas extra al editar
        product.setCategory(new ModelCategory(rs.getInt("id_categoria"), rs.getString("categoria_nombre"), "", true));
        product.setBrand(new ModelBrand(rs.getInt("id_marca"), rs.getString("marca_nombre"), "", true));

        int idProv = rs.getInt("id_proveedor");
        if (idProv > 0) {
            ModelSupplier sup = new ModelSupplier();
            sup.setSupplierId(idProv);
            sup.setName(rs.getString("proveedor_nombre"));
            product.setSupplier(sup);
        }

        // Asignamos los strings pre-concatenados directamente a los campos que usamos
        // en la tabla
        product.setColor(rs.getString("colores"));
        product.setSize(rs.getString("tallas"));

        // Stocks totales precalculados
        product.setPairsStock(rs.getInt("total_pares"));
        product.setBoxesStock(rs.getInt("total_cajas"));

        return product;
    }
}
