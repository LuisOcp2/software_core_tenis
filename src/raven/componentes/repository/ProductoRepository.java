package raven.componentes.repository;


import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import raven.application.form.other.buscador.dto.BusquedaCriteria;
import raven.application.form.other.buscador.dto.ProductoDTO;
import raven.application.form.other.buscador.dto.VarianteDTO;

/**
 * Repository para operaciones de búsqueda de productos
 * Implementa el patrón Repository para encapsular la lógica de acceso a datos
 * 
 * Ventajas del patrón Repository:
 * - Abstrae la lógica de acceso a datos
 * - Facilita testing con mocks
 * - Centraliza queries optimizadas
 * - Permite cache y optimizaciones sin afectar lógica de negocio
 * 
 * Optimizaciones implementadas:
 * - PreparedStatements para prevenir SQL Injection y mejorar performance
 * - Consultas con LIMIT para evitar cargar toda la BD
 * - Índices sugeridos en comentarios
 * - Carga lazy de variantes (solo cuando se necesitan)
 * - StringBuilder para construcción dinámica de queries
 * 
 * @author CrisDEV
 */

public class ProductoRepository {
    private final DataSource dataSource;
    
    /**
     * Constructor con DataSource
     * @param dataSource Fuente de conexiones a la BD
     */
    public ProductoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Busca productos según criterios especificados
     * 
     * ÍNDICES RECOMENDADOS EN BD PARA OPTIMIZACIÓN:
     * - CREATE INDEX idx_producto_nombre ON productos(nombre);
     * - CREATE INDEX idx_producto_genero ON productos(genero);
     * - CREATE INDEX idx_producto_activo ON productos(activo);
     * - CREATE INDEX idx_variante_producto ON producto_variantes(id_producto);
     * - CREATE INDEX idx_inventario_variante ON inventario_bodega(id_variante);
     * 
     * @param criteria Criterios de búsqueda
     * @return Lista de productos que cumplen los criterios
     * @throws SQLException Si hay error en la consulta
     */
    public List<ProductoDTO> buscarProductos(BusquedaCriteria criteria) throws SQLException {
        
        // Construcción dinámica de la query según criterios
        StringBuilder query = new StringBuilder();
        query.append("SELECT DISTINCT ");
        query.append("    p.id_producto, ");
        query.append("    p.codigo_modelo, ");
        query.append("    p.nombre, ");
        query.append("    p.genero ");
        
        boolean requiereIB = Boolean.TRUE.equals(criteria.getSoloConStock())
                || criteria.getBodega() != null
                || criteria.getIdBodega() != null
                || (criteria.getTipo() != null && !criteria.getTipo().isEmpty());
        boolean hayTexto = criteria.getTextoBusqueda() != null && !criteria.getTextoBusqueda().trim().isEmpty();
        boolean requierePV = requiereIB
                || Boolean.TRUE.equals(criteria.getSoloConVariantes())
                || hayTexto;

        if (requiereIB) {
            query.append("FROM inventario_bodega ib ");
            query.append("INNER JOIN producto_variantes pv ON pv.id_variante = ib.id_variante ");
            query.append("INNER JOIN productos p ON p.id_producto = pv.id_producto ");
        } else if (requierePV) {
            query.append("FROM productos p ");
            query.append("INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto ");
        } else {
            query.append("FROM productos p ");
        }
        // Posible join a marcas cuando se filtra por nombre de marca
        if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
            query.append(" INNER JOIN marcas m ON p.id_marca = m.id_marca ");
        }
        
        // Construcción de cláusula WHERE
        List<String> condiciones = new ArrayList<>();
        
        condiciones.add("p.activo = 1");
        if (requierePV) {
            condiciones.add("pv.disponible = 1");
        }
        if (requiereIB) {
            condiciones.add("ib.activo = 1");
        }
        
        // Filtro por texto de búsqueda
        if (hayTexto) {
            if (Boolean.TRUE.equals(criteria.getCoincidenciaExacta())) {
                List<String> sub = new ArrayList<>();
                if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                    sub.add("LOWER(p.nombre) = ?");
                    sub.add("LOWER(p.codigo_modelo) = ?");
                }
                if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                    sub.add("LOWER(m.nombre) = ?");
                }
                sub.add("LOWER(COALESCE(pv.ean,'')) = ?");
                sub.add("LOWER(COALESCE(pv.sku,'')) = ?");
                condiciones.add("(" + String.join(" OR ", sub) + ")");
            } else {
                String texto = criteria.getTextoBusqueda().trim();
                String[] tokens = texto.split("\\s+");
                if (tokens.length > 1) {
                    List<String> grupos = new ArrayList<>();
                    for (int i = 0; i < tokens.length; i++) {
                        List<String> g = new ArrayList<>();
                        if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                            g.add("LOWER(p.nombre) LIKE ?");
                            g.add("LOWER(p.codigo_modelo) LIKE ?");
                        }
                        if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                            g.add("LOWER(m.nombre) LIKE ?");
                        }
                        g.add("LOWER(COALESCE(pv.ean,'')) LIKE ?");
                        g.add("LOWER(COALESCE(pv.sku,'')) LIKE ?");
                        grupos.add("(" + String.join(" OR ", g) + ")");
                    }
                    condiciones.add(String.join(" OR ", grupos));
                } else {
                    List<String> sub = new ArrayList<>();
                    if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                        sub.add("LOWER(p.nombre) LIKE ?");
                        sub.add("LOWER(p.codigo_modelo) LIKE ?");
                    }
                    if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                        sub.add("LOWER(m.nombre) LIKE ?");
                    }
                    sub.add("LOWER(COALESCE(pv.ean,'')) LIKE ?");
                    sub.add("LOWER(COALESCE(pv.sku,'')) LIKE ?");
                    condiciones.add("(" + String.join(" OR ", sub) + ")");
                }
            }
        }
        
        // Filtro por género
        if (criteria.getGenero() != null && !criteria.getGenero().isEmpty()) {
            condiciones.add("p.genero = ?");
        }
        
        // Filtro por categoría
        if (criteria.getIdCategoria() != null) {
            condiciones.add("p.id_categoria = ?");
        }
        
        // Filtro por marca
        if (criteria.getIdMarca() != null) {
            condiciones.add("p.id_marca = ?");
        }
        
        // Filtro por bodega
        if (criteria.getIdBodega() != null) {
            condiciones.add("ib.id_bodega = ?");
        } else if (criteria.getBodega() != null && !criteria.getBodega().isEmpty()) {
            condiciones.add("ib.ubicacion_especifica = ?");
        }
        
        // Filtro solo con stock
        if (Boolean.TRUE.equals(criteria.getSoloConStock())) {
            condiciones.add("(COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)");
        }

        // Filtro por tipo (par/caja)
        if (criteria.getTipo() != null && !criteria.getTipo().isEmpty()) {
            if ("par".equalsIgnoreCase(criteria.getTipo())) {
                condiciones.add("COALESCE(ib.Stock_par,0) > 0");
            } else if ("caja".equalsIgnoreCase(criteria.getTipo())) {
                condiciones.add("COALESCE(ib.Stock_caja,0) > 0");
            }
        }
        
        // Agregar WHERE si hay condiciones
        if (!condiciones.isEmpty()) {
            query.append(" WHERE ");
            query.append(String.join(" AND ", condiciones));
        }
        
        // Ordenamiento con ponderación: exactas primero, luego parciales
        if (criteria.getTextoBusqueda() != null && !criteria.getTextoBusqueda().trim().isEmpty()) {
            query.append(" ORDER BY ");
            query.append(" CASE ");
            // Exact matches
            if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                query.append(" WHEN LOWER(p.nombre) = ? THEN 0 ");
                query.append(" WHEN LOWER(p.codigo_modelo) = ? THEN 0 ");
            }
            if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                query.append(" WHEN LOWER(m.nombre) = ? THEN 0 ");
            }
            // Partial matches
            if (!Boolean.TRUE.equals(criteria.getCoincidenciaExacta())) {
                if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                    query.append(" WHEN LOWER(p.nombre) LIKE ? THEN 1 ");
                    query.append(" WHEN LOWER(p.codigo_modelo) LIKE ? THEN 1 ");
                }
                if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                    query.append(" WHEN LOWER(m.nombre) LIKE ? THEN 1 ");
                }
            }
            query.append(" ELSE 2 END, p.nombre ASC ");
        } else {
            query.append(" ORDER BY p.nombre ASC ");
        }
        query.append(" LIMIT ? OFFSET ? ");
        
        // Ejecutar query
        List<ProductoDTO> productos = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            // Asignar parámetros
            int paramIndex = 1;
            
            System.out.println("[BuscarProductos] idBodega=" + criteria.getIdBodega() + 
                               ", bodega='" + criteria.getBodega() + "'" +
                               ", texto='" + (criteria.getTextoBusqueda() != null ? criteria.getTextoBusqueda() : "") + "'" +
                               ", soloStock=" + criteria.getSoloConStock() +
                               ", tipo='" + (criteria.getTipo() != null ? criteria.getTipo() : "") + "'" +
                               ", limit=" + criteria.getLimite() + ", offset=" + criteria.getOffset());
            System.out.println("[BuscarProductos] SQL=" + query.toString());

        if (hayTexto) {
            String texto = criteria.getTextoBusqueda().trim();
            if (Boolean.TRUE.equals(criteria.getCoincidenciaExacta())) {
                String tLower = texto.toLowerCase();
                if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                    stmt.setString(paramIndex++, tLower);
                    stmt.setString(paramIndex++, tLower);
                }
                if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                    stmt.setString(paramIndex++, tLower);
                }
                stmt.setString(paramIndex++, tLower);
                stmt.setString(paramIndex++, tLower);
            } else {
                String[] tokens = texto.split("\\s+");
                if (tokens.length > 1) {
                    for (String t : tokens) {
                        String pat = "%" + t.toLowerCase() + "%";
                        if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                            stmt.setString(paramIndex++, pat);
                            stmt.setString(paramIndex++, pat);
                        }
                        if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                            stmt.setString(paramIndex++, pat);
                        }
                        stmt.setString(paramIndex++, pat);
                        stmt.setString(paramIndex++, pat);
                    }
                } else {
                    String pat = "%" + texto.toLowerCase() + "%";
                    if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                        stmt.setString(paramIndex++, pat);
                        stmt.setString(paramIndex++, pat);
                    }
                    if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                        stmt.setString(paramIndex++, pat);
                    }
                    stmt.setString(paramIndex++, pat);
                    stmt.setString(paramIndex++, pat);
                }
            }
        }
            
            if (criteria.getGenero() != null && !criteria.getGenero().isEmpty()) {
                stmt.setString(paramIndex++, criteria.getGenero());
            }
            
            if (criteria.getIdCategoria() != null) {
                stmt.setInt(paramIndex++, criteria.getIdCategoria());
            }
            
            if (criteria.getIdMarca() != null) {
                stmt.setInt(paramIndex++, criteria.getIdMarca());
            }
            
            if (criteria.getIdBodega() != null) {
                stmt.setInt(paramIndex++, criteria.getIdBodega());
            } else if (criteria.getBodega() != null && !criteria.getBodega().isEmpty()) {
                stmt.setString(paramIndex++, criteria.getBodega());
            }
            
            // Parámetros adicionales para ORDER BY ponderado
            if (criteria.getTextoBusqueda() != null && !criteria.getTextoBusqueda().trim().isEmpty()) {
                String texto = criteria.getTextoBusqueda().trim().toLowerCase();
                String searchPattern = "%" + texto + "%";
                if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                    stmt.setString(paramIndex++, texto);
                    stmt.setString(paramIndex++, texto);
                }
                if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                    stmt.setString(paramIndex++, texto);
                }
                if (!Boolean.TRUE.equals(criteria.getCoincidenciaExacta())) {
                    if (Boolean.TRUE.equals(criteria.getBuscarPorNombre())) {
                        stmt.setString(paramIndex++, searchPattern);
                        stmt.setString(paramIndex++, searchPattern);
                    }
                    if (Boolean.TRUE.equals(criteria.getBuscarPorMarca())) {
                        stmt.setString(paramIndex++, searchPattern);
                    }
                }
            }
            // Límite y offset
            stmt.setInt(paramIndex++, criteria.getLimite());
            stmt.setInt(paramIndex++, criteria.getOffset());
            
            // Ejecutar y mapear resultados
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProductoDTO producto = mapearProducto(rs);
                    productos.add(producto);
                }
            }

            if (productos.isEmpty() && criteria.getIdBodega() != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM inventario_bodega ib " +
                        "JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
                        "WHERE ib.id_bodega = ? AND ib.activo = 1 AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)")) {
                    ps.setInt(1, criteria.getIdBodega());
                    try (ResultSet r = ps.executeQuery()) {
                        if (r.next()) {
                            int cnt = r.getInt(1);
                            System.out.println("[Diagnóstico Buscador] Stock positivo en bodega=" + criteria.getIdBodega() + ": " + cnt + " variantes");
                        }
                    }
                }
            }
        }
        
        return productos;
    }
    
    /**
     * Carga las variantes de un producto específico
     * Esta carga es lazy - solo cuando se necesitan las variantes
     * 
     * @param idProducto ID del producto
     * @param bodega Bodega específica (null para todas)
     * @return Lista de variantes con stock
     * @throws SQLException Si hay error en la consulta
     */
    public List<VarianteDTO> cargarVariantes(Integer idProducto, String bodega) throws SQLException {
        return cargarVariantes(idProducto, bodega, null);
    }

    public List<VarianteDTO> cargarVariantes(Integer idProducto, String bodega, String tipo) throws SQLException {

        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("    pv.id_variante, ");
        query.append("    pv.id_producto, ");
        query.append("    pv.id_talla, ");
        query.append("    pv.id_color, ");
        query.append("    pv.ean, ");
        query.append("    pv.sku, ");
        query.append("    pv.precio_compra, ");
        query.append("    pv.precio_venta, ");
        query.append("    pv.disponible, ");
        // Información de talla y color usando joins reales
        query.append("    COALESCE(t.numero, CAST(pv.id_talla AS CHAR)) AS talla, ");
        query.append("    COALESCE(c.nombre, CAST(pv.id_color AS CHAR)) AS color, ");
        
        // Stock agregado o por bodega
        if (bodega != null && !bodega.isEmpty()) {
            query.append("    ib.Stock_par, ");
            query.append("    ib.Stock_caja, ");
            query.append("    ib.stock_reservado, ");
            query.append("    ib.ubicacion_especifica ");
            query.append("FROM producto_variantes pv ");
            query.append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ");
            query.append("LEFT JOIN colores c ON pv.id_color = c.id_color ");
            query.append("INNER JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante ");
            query.append("WHERE pv.id_producto = ? ");
            if (esNumero(bodega)) {
                query.append("  AND ib.id_bodega = ? ");
            } else {
                query.append("  AND ib.ubicacion_especifica = ? ");
            }
            query.append("  AND pv.disponible = 1 ");
            if (tipo != null) {
                if ("par".equalsIgnoreCase(tipo)) {
                    query.append("  AND COALESCE(ib.Stock_par,0) > 0 ");
                } else if ("caja".equalsIgnoreCase(tipo)) {
                    query.append("  AND COALESCE(ib.Stock_caja,0) > 0 ");
                }
            }
        } else {
            query.append("    COALESCE(SUM(ib.Stock_par), 0) as Stock_par, ");
            query.append("    COALESCE(SUM(ib.Stock_caja), 0) as Stock_caja, ");
            query.append("    COALESCE(SUM(ib.stock_reservado), 0) as stock_reservado, ");
            query.append("    'GENERAL' as ubicacion_especifica ");
            query.append("FROM producto_variantes pv ");
            query.append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ");
            query.append("LEFT JOIN colores c ON pv.id_color = c.id_color ");
            query.append("LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante ");
            query.append("WHERE pv.id_producto = ? ");
            query.append("  AND pv.disponible = 1 ");
            query.append("GROUP BY pv.id_variante, pv.id_producto, pv.id_talla, pv.id_color, ");
            query.append("         pv.ean, pv.sku, pv.precio_compra, pv.precio_venta, pv.disponible, t.numero, c.nombre ");
            if (tipo != null) {
                if ("par".equalsIgnoreCase(tipo)) {
                    query.append(" HAVING COALESCE(SUM(ib.Stock_par),0) > 0 ");
                } else if ("caja".equalsIgnoreCase(tipo)) {
                    query.append(" HAVING COALESCE(SUM(ib.Stock_caja),0) > 0 ");
                }
            }
        }
        
        query.append("ORDER BY pv.id_talla, pv.id_color ");
        
        List<VarianteDTO> variantes = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            stmt.setInt(1, idProducto);
            if (bodega != null && !bodega.isEmpty()) {
                if (esNumero(bodega)) {
                    stmt.setInt(2, Integer.parseInt(bodega));
                } else {
                    stmt.setString(2, bodega);
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    VarianteDTO variante = mapearVariante(rs);
                    variantes.add(variante);
                }
            }
        }
        
        return variantes;
    }
    
    /**
     * Carga variantes de múltiples productos en una sola query
     * Optimización para cuando se cargan muchos productos
     * 
     * @param idsProductos Lista de IDs de productos
     * @param bodega Bodega específica (null para todas)
     * @return Mapa de ID producto -> Lista de variantes
     * @throws SQLException Si hay error en la consulta
     */
    public Map<Integer, List<VarianteDTO>> cargarVariantesBatch(List<Integer> idsProductos, String bodega) 
            throws SQLException {
        return cargarVariantesBatch(idsProductos, bodega, null);
    }

    public Map<Integer, List<VarianteDTO>> cargarVariantesBatch(List<Integer> idsProductos, String bodega, String tipo) 
            throws SQLException {
        
        if (idsProductos == null || idsProductos.isEmpty()) {
            return new HashMap<>();
        }
        
        // Crear placeholders para IN clause (?, ?, ?)
        String placeholders = String.join(",", 
                idsProductos.stream().map(id -> "?").toArray(String[]::new));
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("    pv.id_variante, ");
        query.append("    pv.id_producto, ");
        query.append("    pv.id_talla, ");
        query.append("    pv.id_color, ");
        query.append("    pv.ean, ");
        query.append("    pv.sku, ");
        query.append("    pv.precio_compra, ");
        query.append("    pv.precio_venta, ");
        query.append("    pv.disponible, ");
        query.append("    COALESCE(t.numero, CAST(pv.id_talla AS CHAR)) AS talla, ");
        query.append("    COALESCE(c.nombre, CAST(pv.id_color AS CHAR)) AS color, ");
        
        if (bodega != null && !bodega.isEmpty()) {
            query.append("    ib.Stock_par, ");
            query.append("    ib.Stock_caja, ");
            query.append("    ib.stock_reservado, ");
            query.append("    ib.ubicacion_especifica ");
            query.append("FROM producto_variantes pv ");
            query.append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ");
            query.append("LEFT JOIN colores c ON pv.id_color = c.id_color ");
            query.append("INNER JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante ");
            query.append("WHERE pv.id_producto IN (").append(placeholders).append(") ");
            if (esNumero(bodega)) {
                query.append("  AND ib.id_bodega = ? ");
            } else {
                query.append("  AND ib.ubicacion_especifica = ? ");
            }
            query.append("  AND pv.disponible = 1 ");
            if (tipo != null) {
                if ("par".equalsIgnoreCase(tipo)) {
                    query.append("  AND COALESCE(ib.Stock_par,0) > 0 ");
                } else if ("caja".equalsIgnoreCase(tipo)) {
                    query.append("  AND COALESCE(ib.Stock_caja,0) > 0 ");
                }
            }
        } else {
            query.append("    COALESCE(SUM(ib.Stock_par), 0) as Stock_par, ");
            query.append("    COALESCE(SUM(ib.Stock_caja), 0) as Stock_caja, ");
            query.append("    COALESCE(SUM(ib.stock_reservado), 0) as stock_reservado, ");
            query.append("    'GENERAL' as ubicacion_especifica ");
            query.append("FROM producto_variantes pv ");
            query.append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ");
            query.append("LEFT JOIN colores c ON pv.id_color = c.id_color ");
            query.append("LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante ");
            query.append("WHERE pv.id_producto IN (").append(placeholders).append(") ");
            query.append("  AND pv.disponible = 1 ");
            query.append("GROUP BY pv.id_variante, pv.id_producto, pv.id_talla, pv.id_color, ");
            query.append("         pv.ean, pv.sku, pv.precio_compra, pv.precio_venta, pv.disponible, t.numero, c.nombre ");
            if (tipo != null) {
                if ("par".equalsIgnoreCase(tipo)) {
                    query.append(" HAVING COALESCE(SUM(ib.Stock_par),0) > 0 ");
                } else if ("caja".equalsIgnoreCase(tipo)) {
                    query.append(" HAVING COALESCE(SUM(ib.Stock_caja),0) > 0 ");
                }
            }
        }
        
        query.append("ORDER BY pv.id_producto, pv.id_talla, pv.id_color ");
        
        Map<Integer, List<VarianteDTO>> mapaVariantes = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            // Asignar IDs de productos
            int paramIndex = 1;
            for (Integer idProducto : idsProductos) {
                stmt.setInt(paramIndex++, idProducto);
            }
            
            // Asignar bodega si aplica
            if (bodega != null && !bodega.isEmpty()) {
                if (esNumero(bodega)) {
                    stmt.setInt(paramIndex, Integer.parseInt(bodega));
                } else {
                    stmt.setString(paramIndex, bodega);
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    VarianteDTO variante = mapearVariante(rs);
                    Integer idProducto = variante.getIdProducto();
                    
                    mapaVariantes.computeIfAbsent(idProducto, k -> new ArrayList<>())
                                 .add(variante);
                }
            }
        }
        
        return mapaVariantes;
    }
    
    /**
     * Mapea un ResultSet a ProductoDTO
     */
    private ProductoDTO mapearProducto(ResultSet rs) throws SQLException {
        ProductoDTO producto = new ProductoDTO();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCodigoModelo(rs.getString("codigo_modelo"));
        producto.setNombre(rs.getString("nombre"));
        producto.setGenero(rs.getString("genero"));
        // Nota: La imagen se carga bajo demanda para no impactar performance
        return producto;
    }
    
    /**
     * Mapea un ResultSet a VarianteDTO
     */
    private VarianteDTO mapearVariante(ResultSet rs) throws SQLException {
        VarianteDTO variante = new VarianteDTO();
        variante.setIdVariante(rs.getInt("id_variante"));
        variante.setIdProducto(rs.getInt("id_producto"));
        variante.setTalla(rs.getString("talla"));
        variante.setColor(rs.getString("color"));
        variante.setEan(rs.getString("ean"));
        variante.setSku(rs.getString("sku"));
        variante.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        variante.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        variante.setDisponible(rs.getBoolean("disponible"));
        
        // Stock
        int stockPar = rs.getInt("Stock_par");
        int stockCaja = rs.getInt("Stock_caja");
        int stockReservado = rs.getInt("stock_reservado");
        
        variante.setStockPares(stockPar);
        variante.setStockCaja(stockCaja);
        variante.setStockReservado(stockReservado);
        variante.setStockDisponible((stockPar + stockCaja) - stockReservado);
        variante.setUbicacionEspecifica(rs.getString("ubicacion_especifica"));
        
        return variante;
    }

    private boolean esNumero(String s) {
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }
    
    /**
     * Cuenta el total de productos que cumplen los criterios
     * Útil para paginación
     * 
     * @param criteria Criterios de búsqueda
     * @return Cantidad total de productos
     * @throws SQLException Si hay error en la consulta
     */
    public int contarProductos(BusquedaCriteria criteria) throws SQLException {
        // Similar a buscarProductos pero con COUNT(*)
        // Implementación simplificada por brevedad
        return 0; // TODO: Implementar
    }
}
