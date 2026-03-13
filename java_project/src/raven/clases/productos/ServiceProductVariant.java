package raven.clases.productos;

import java.math.BigDecimal;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelProductVariant;
import raven.controlador.inventario.InventarioBodega;
import raven.controlador.principal.conexion;
import raven.clases.inventario.ServiceInventarioBodega;
import raven.dao.ProductoVariantesDAO;
import raven.dao.InventarioBodegaDAO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Métodos refactorizados para ServiceProductVariant con soporte multi-bodega.
 *
 * Esta clase proporciona métodos optimizados para trabajar con variantes de
 * productos considerando el inventario distribuido en múltiples bodegas.
 *
 * CAMBIO PRINCIPAL: - Antes: stock_por_pares y stock_por_cajas en tabla
 * producto_variantes - Ahora: Stock_par y Stock_caja en tabla inventario_bodega
 * por cada bodega
 *
 * Patrones implementados: - Repository Pattern: Encapsula acceso a datos - DTO
 * Pattern: Usa InventarioBodega para transferir datos - Builder Pattern:
 * Construcción gradual de objetos complejos
 *
 * @author Sistema Multi-Bodega
 * @version 1.0
 */
public class ServiceProductVariant {

    /*
     * ============================================================================
     * CONSTANTES SQL REFACTORIZADAS
     * ============================================================================
     */
    /**
     * Obtiene todas las variantes de un producto con su inventario en cada
     * bodega. Una variante puede tener múltiples registros si está en varias
     * bodegas.
     */
    private static final String SQL_GET_VARIANTES_WITH_INVENTARIO = "SELECT "
            + "    pv.id_variante, "
            + "    pv.id_producto, "
            + "    pv.id_talla, "
            + "    t.numero AS talla, "
            + "    pv.id_color, "
            + "    c.nombre AS color, "
            + "    pv.sku, "
            + "    pv.ean, "
            + "    pv.precio_compra, "
            + "    pv.precio_venta, "
            + "    pv.stock_minimo_variante, "
            + "    pv.disponible, "
            + "    pv.fecha_creacion, "
            + "    -- Información de inventario por bodega "
            + "    ib.id_inventario_bodega, "
            + "    ib.id_bodega, "
            + "    b.nombre AS nombre_bodega, "
            + "    b.codigo AS codigo_bodega, "
            + "    ib.Stock_par, "
            + "    ib.Stock_caja, "
            + "    ib.stock_reservado, "
            + "    ib.ubicacion_especifica, "
            + "    ib.fecha_ultimo_movimiento, "
            + "    -- Stock disponible (descontando reservado) "
            + "    (ib.Stock_par - COALESCE(ib.stock_reservado, 0)) AS stock_disponible "
            + "FROM producto_variantes pv "
            + "INNER JOIN tallas t ON pv.id_talla = t.id_talla "
            + "INNER JOIN colores c ON pv.id_color = c.id_color "
            + "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.activo = 1 "
            + "LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega "
            + "WHERE pv.id_producto = ? "
            + "  AND pv.disponible = 1 "
            + "ORDER BY t.numero ASC, c.nombre ASC, b.nombre ASC";

    /**
     * Obtiene variantes de un producto en una bodega específica.
     */
    private static final String SQL_GET_VARIANTES_BY_BODEGA = "SELECT "
            + "    pv.id_variante, "
            + "    pv.id_producto, "
            + "    pv.id_talla, "
            + "    t.numero AS talla, "
            + "    pv.id_color, "
            + "    c.nombre AS color, "
            + "    pv.sku, "
            + "    pv.ean, "
            + "    pv.precio_compra, "
            + "    pv.precio_venta, "
            + "    pv.stock_minimo_variante, "
            + "    pv.disponible, "
            + "    ib.id_inventario_bodega, "
            + "    ib.id_bodega, "
            + "    b.nombre AS nombre_bodega, "
            + "    ib.Stock_par, "
            + "    ib.Stock_caja, "
            + "    ib.stock_reservado, "
            + "    (ib.Stock_par - COALESCE(ib.stock_reservado, 0)) AS stock_disponible, "
            + "    ib.ubicacion_especifica "
            + "FROM producto_variantes pv "
            + "INNER JOIN tallas t ON pv.id_talla = t.id_talla "
            + "INNER JOIN colores c ON pv.id_color = c.id_color "
            + "INNER JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante "
            + "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega "
            + "WHERE pv.id_producto = ? "
            + "  AND ib.id_bodega = ? "
            + "  AND pv.disponible = 1 "
            + "  AND ib.activo = 1 "
            + "  "
            + "ORDER BY t.numero ASC, c.nombre ASC";

    /**
     * Obtiene una variante específica con todo su inventario.
     */
    private static final String SQL_GET_VARIANTE_WITH_ALL_INVENTARIO = "SELECT "
            + "    pv.id_variante, "
            + "    pv.id_producto, "
            + "    p.nombre AS nombre_producto, "
            + "    p.codigo_modelo, "
            + "    pv.id_talla, "
            + "    t.numero AS talla, "
            + "    pv.id_color, "
            + "    c.nombre AS color, "
            + "    pv.sku, "
            + "    pv.ean, "
            + "    pv.precio_compra, "
            + "    pv.precio_venta, "
            + "    pv.stock_minimo_variante, "
            + "    pv.disponible, "
            + "    -- Inventario por bodega "
            + "    ib.id_inventario_bodega, "
            + "    ib.id_bodega, "
            + "    b.nombre AS nombre_bodega, "
            + "    b.codigo AS codigo_bodega, "
            + "    b.tipo AS tipo_bodega, "
            + "    ib.Stock_par, "
            + "    ib.Stock_caja, "
            + "    ib.stock_reservado, "
            + "    (ib.Stock_par - COALESCE(ib.stock_reservado, 0)) AS stock_disponible, "
            + "    ib.ubicacion_especifica, "
            + "    -- Totales consolidados "
            + "    (SELECT COALESCE(SUM(Stock_par), 0) "
            + "     FROM inventario_bodega "
            + "     WHERE id_variante = pv.id_variante AND activo = 1) AS total_pares, "
            + "    (SELECT COALESCE(SUM(Stock_caja), 0) "
            + "     FROM inventario_bodega "
            + "     WHERE id_variante = pv.id_variante AND activo = 1) AS total_cajas "
            + "FROM producto_variantes pv "
            + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
            + "INNER JOIN tallas t ON pv.id_talla = t.id_talla "
            + "INNER JOIN colores c ON pv.id_color = c.id_color "
            + "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.activo = 1 "
            + "LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega "
            + "WHERE pv.id_variante = ? "
            + "ORDER BY b.tipo ASC, b.nombre ASC";

    /**
     * Verifica disponibilidad de stock en bodega específica.
     */
    private static final String SQL_CHECK_STOCK_DISPONIBLE = "SELECT "
            + "    pv.id_variante, "
            + "    pv.sku, "
            + "    ib.id_bodega, "
            + "    b.nombre AS nombre_bodega, "
            + "    ib.Stock_par, "
            + "    ib.Stock_caja, "
            + "    ib.stock_reservado, "
            + "    (ib.Stock_par - COALESCE(ib.stock_reservado, 0)) AS stock_disponible "
            + "FROM producto_variantes pv "
            + "INNER JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante "
            + "INNER JOIN bodegas b ON ib.id_bodega = b.id_bodega "
            + "WHERE pv.id_variante = ? "
            + "  AND ib.id_bodega = ? "
            + "  AND ib.activo = 1";

    /**
     * Inserta nueva variante (sin inventario inicial).
     */
    private static final String SQL_INSERT_VARIANTE = "INSERT INTO producto_variantes "
            + "(id_producto, id_talla, id_color, imagen, ean, sku, precio_compra, precio_venta, stock_minimo_variante, disponible) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

    /**
     * Actualiza información básica de variante (sin tocar stock).
     */
    private static final String SQL_UPDATE_VARIANTE = "UPDATE producto_variantes "
            + "SET id_talla = ?, "
            + "    id_color = ?, "
            + "    precio_compra = ?, "
            + "    precio_venta = ?, "
            + "    stock_minimo_variante = ?, "
            + "    disponible = ? "
            + "WHERE id_variante = ?";

    /*
     * ============================================================================
     * MÉTODOS PÚBLICOS - OPERACIONES DE CONSULTA
     * ============================================================================
     */
    /**
     * Obtiene todas las variantes de un producto con su inventario en cada
     * bodega.
     *
     * IMPORTANTE: Una variante puede aparecer múltiples veces (una por bodega).
     *
     * Ejemplo de uso:
     * 
     * <pre>
     * int idProducto = 17;
     * List<VarianteConInventario> variantes = service.getVariantesWithInventario(idProducto);
     *
     * for (VarianteConInventario v : variantes) {
     *     System.out.println(v.getTalla() + " - " + v.getColor() +
     *             " en " + v.getNombreBodega() +
     *             ": " + v.getStockDisponible() + " pares");
     * }
     * </pre>
     *
     * @param idProducto ID del producto
     * @return Lista de variantes con información de inventario por bodega
     * @throws SQLException Si hay error en la consulta
     */
    public List<VarianteConInventario> getVariantesWithInventario(int idProducto)
            throws SQLException {

        List<VarianteConInventario> variantes = new ArrayList<>();

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(SQL_GET_VARIANTES_WITH_INVENTARIO)) {

            pst.setInt(1, idProducto);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VarianteConInventario variante = mapearVarianteConInventario(rs);
                    variantes.add(variante);
                }
            }
        }

        return variantes;
    }

    /**
     * Obtiene variantes de un producto disponibles en una bodega específica.
     * Solo retorna variantes con stock en esa bodega.
     *
     * Ejemplo de uso:
     * 
     * <pre>
     * int idProducto = 17;
     * int idBodegaLocal = 7; // XTRME SHOES
     * List<VarianteConInventario> variantes = service.getVariantesByBodega(idProducto, idBodegaLocal);
     * </pre>
     *
     * @param idProducto ID del producto
     * @param idBodega   ID de la bodega
     * @return Lista de variantes con stock en esa bodega
     * @throws SQLException Si hay error en la consulta
     */
    public List<VarianteConInventario> getVariantesByBodega(int idProducto, int idBodega)
            throws SQLException {

        List<VarianteConInventario> variantes = new ArrayList<>();

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(SQL_GET_VARIANTES_BY_BODEGA)) {

            pst.setInt(1, idProducto);
            pst.setInt(2, idBodega);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VarianteConInventario variante = mapearVarianteConInventario(rs);
                    variantes.add(variante);
                }
            }
        }

        return variantes;
    }

    /**
     * Obtiene información completa de una variante incluyendo su inventario en
     * todas las bodegas. Útil para ver la distribución completa de una
     * variante.
     *
     * @param idVariante ID de la variante
     * @return Objeto con información completa de la variante e inventario
     * @throws SQLException Si hay error en la consulta
     */
    public VarianteCompleta getVarianteCompleta(int idVariante) throws SQLException {
        VarianteCompleta varianteCompleta = new VarianteCompleta();
        List<InventarioBodega> inventarios = new ArrayList<>();

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(SQL_GET_VARIANTE_WITH_ALL_INVENTARIO)) {

            pst.setInt(1, idVariante);

            try (ResultSet rs = pst.executeQuery()) {
                boolean first = true;

                while (rs.next()) {
                    // La primera vez, llenar datos de la variante
                    if (first) {
                        varianteCompleta.setIdVariante(rs.getInt("id_variante"));
                        varianteCompleta.setIdProducto(rs.getInt("id_producto"));
                        varianteCompleta.setNombreProducto(rs.getString("nombre_producto"));
                        varianteCompleta.setCodigoModelo(rs.getString("codigo_modelo"));
                        varianteCompleta.setTalla(rs.getString("talla"));
                        varianteCompleta.setColor(rs.getString("color"));
                        varianteCompleta.setSku(rs.getString("sku"));
                        varianteCompleta.setEan(rs.getString("ean"));
                        varianteCompleta.setPrecioCompra(rs.getBigDecimal("precio_compra"));
                        varianteCompleta.setPrecioVenta(rs.getBigDecimal("precio_venta"));
                        varianteCompleta.setStockMinimo(rs.getInt("stock_minimo_variante"));
                        varianteCompleta.setTotalPares(rs.getInt("total_pares"));
                        varianteCompleta.setTotalCajas(rs.getInt("total_cajas"));

                        first = false;
                    }

                    // Agregar inventario de esta bodega (si existe)
                    Integer idInventario = rs.getInt("id_inventario_bodega");
                    if (!rs.wasNull()) {
                        InventarioBodega inv = new InventarioBodega();
                        inv.setIdInventarioBodega(idInventario);
                        inv.setIdBodega(rs.getInt("id_bodega"));
                        inv.setNombreBodega(rs.getString("nombre_bodega"));
                        inv.setCodigoBodega(rs.getString("codigo_bodega"));
                        inv.setStockPar(rs.getInt("Stock_par"));
                        inv.setStockCaja(rs.getInt("Stock_caja"));
                        inv.setStockReservado(rs.getInt("stock_reservado"));
                        inv.setUbicacionEspecifica(rs.getString("ubicacion_especifica"));

                        inventarios.add(inv);
                    }
                }
            }
        }

        varianteCompleta.setInventarios(inventarios);
        return varianteCompleta;
    }

    /**
     * Verifica si una variante tiene stock disponible en una bodega. Considera
     * el stock reservado.
     *
     * @param idVariante        ID de la variante
     * @param idBodega          ID de la bodega
     * @param cantidadRequerida Cantidad necesaria
     * @return true si hay stock suficiente, false en caso contrario
     * @throws SQLException Si hay error en la consulta
     */
    public boolean verificarStockDisponible(int idVariante, int idBodega, int cantidadRequerida)
            throws SQLException {

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(SQL_CHECK_STOCK_DISPONIBLE)) {

            pst.setInt(1, idVariante);
            pst.setInt(2, idBodega);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int stockDisponible = rs.getInt("stock_disponible");
                    return stockDisponible >= cantidadRequerida;
                }
            }
        }

        return false;
    }

    // Compatibilidad: obtener variante por id
    public raven.controlador.productos.ModelProductVariant getVariantById(int idVariante, boolean withImage)
            throws SQLException {
        String sql = "SELECT pv.id_variante, pv.id_producto, pv.id_talla, pv.id_color, pv.sku, pv.ean, pv.precio_compra, pv.precio_venta, pv.stock_minimo_variante, pv.disponible, "
                + "t.numero AS nombre_talla, t.sistema AS sistema_talla, t.genero AS genero_talla, "
                + "col.nombre AS nombre_color, pv.imagen "
                + "FROM producto_variantes pv "
                + "LEFT JOIN tallas t ON pv.id_talla=t.id_talla "
                + "LEFT JOIN colores col ON pv.id_color=col.id_color "
                + "WHERE pv.id_variante=?";
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idVariante);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    raven.controlador.productos.ModelProductVariant v = new raven.controlador.productos.ModelProductVariant();
                    v.setVariantId(rs.getInt("id_variante"));
                    v.setProductId(rs.getInt("id_producto"));
                    v.setSizeId(rs.getInt("id_talla"));
                    v.setColorId(rs.getInt("id_color"));
                    v.setSku(rs.getString("sku"));
                    v.setBarcode(""); // Campo eliminado de BD
                    v.setEan(rs.getString("ean"));
                    Double pc = rs.getDouble("precio_compra");
                    v.setPurchasePrice(rs.wasNull() ? null : pc);
                    Double pv = rs.getDouble("precio_venta");
                    v.setSalePrice(rs.wasNull() ? null : pv);
                    int sm = rs.getInt("stock_minimo_variante");
                    v.setMinStock(rs.wasNull() ? null : sm);
                    v.setAvailable(rs.getBoolean("disponible"));
                    v.setSizeName(rs.getString("nombre_talla"));
                    v.setSizeSystem(rs.getString("sistema_talla")); // Fetch System
                    v.setGender(rs.getString("genero_talla")); // Fetch Gender
                    v.setColorName(rs.getString("nombre_color"));
                    //  MEJORADO: Cargar imagen usando getBlob() para mayor compatibilidad
                    if (withImage) {
                        try {
                            java.sql.Blob blob = rs.getBlob("imagen");
                            if (blob != null && blob.length() > 0) {
                                v.setImageBytes(blob.getBytes(1, (int) blob.length()));
                            }
                        } catch (Exception imgEx) {
                            // Fallback a getBytes() si getBlob() falla
                            try {
                                v.setImageBytes(rs.getBytes("imagen"));
                            } catch (Exception e) {
                                System.err.println("WARNING  Error cargando imagen: " + e.getMessage());
                            }
                        }
                    }
                    return v;
                }
            }
        }
        return null;
    }

    public raven.controlador.productos.ModelProductVariant getVariantById(int idVariante) throws SQLException {
        return getVariantById(idVariante, true);
    }

    public java.util.List<ModelProductVariant> getVariantsByProduct(int idProducto) throws java.sql.SQLException {
        java.util.List<raven.controlador.productos.ModelProductVariant> list = new java.util.ArrayList<>();

        // WARNING  CORRECCIÓN: Eliminado 'pv.codigo_barras' del SELECT
        String sql = "SELECT "
                + "pv.id_variante, "
                + "pv.id_producto, "
                + "pv.id_talla, "
                + "pv.id_color, "
                + "pv.sku, "
                // + "pv.codigo_barras, " // ERROR  ELIMINADO - Campo no existe en BD
                + "pv.ean, "
                + "pv.precio_compra, "
                + "pv.precio_venta, "
                + "pv.stock_minimo_variante, "
                + "pv.disponible, "
                + "t.numero AS nombre_talla, "
                + "col.nombre AS nombre_color, "
                + "COALESCE(SUM(ib.Stock_par),0) AS stock_por_pares, "
                + "COALESCE(SUM(ib.Stock_caja),0) AS stock_por_cajas "
                + "FROM producto_variantes pv "
                + "LEFT JOIN tallas t ON pv.id_talla=t.id_talla "
                + "LEFT JOIN colores col ON pv.id_color=col.id_color "
                + "LEFT JOIN inventario_bodega ib ON pv.id_variante=ib.id_variante "
                + "WHERE pv.id_producto=? "
                + "GROUP BY pv.id_variante "
                + "ORDER BY t.numero, col.nombre";

        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idProducto);

            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    raven.controlador.productos.ModelProductVariant v = new raven.controlador.productos.ModelProductVariant();

                    v.setVariantId(rs.getInt("id_variante"));
                    v.setProductId(rs.getInt("id_producto"));
                    v.setSizeId(rs.getInt("id_talla"));
                    v.setColorId(rs.getInt("id_color"));
                    v.setSku(rs.getString("sku"));

                    // WARNING  CORRECCIÓN: Campo codigo_barras no existe, usar EAN o null
                    // OPCIÓN A: Asignar null
                    v.setBarcode(null);
                    // OPCIÓN B: Usar EAN como código de barras
                    // v.setBarcode(rs.getString("ean"));

                    v.setEan(rs.getString("ean"));

                    Double pc = rs.getDouble("precio_compra");
                    v.setPurchasePrice(rs.wasNull() ? null : pc);

                    Double pv = rs.getDouble("precio_venta");
                    v.setSalePrice(rs.wasNull() ? null : pv);

                    int sm = rs.getInt("stock_minimo_variante");
                    v.setMinStock(rs.wasNull() ? null : sm);

                    v.setAvailable(rs.getBoolean("disponible"));
                    v.setSizeName(rs.getString("nombre_talla"));
                    v.setColorName(rs.getString("nombre_color"));
                    v.setStockPairs(rs.getInt("stock_por_pares"));
                    v.setStockBoxes(rs.getInt("stock_por_cajas"));

                    list.add(v);
                }
            }
        }
        return list;
    }

    public java.util.List<ModelProductVariant> getVariantsByProductAndWarehouse(int idProducto, int idBodega)
            throws java.sql.SQLException {
        java.util.List<raven.controlador.productos.ModelProductVariant> list = new java.util.ArrayList<>();

        // WARNING  CORRECCIÓN: Eliminado 'pv.codigo_barras' del SELECT
        String sql = "SELECT "
                + "pv.id_variante, "
                + "pv.id_producto, "
                + "pv.id_talla, "
                + "pv.id_color, "
                + "pv.sku, "
                // + "pv.codigo_barras, " // ERROR  ELIMINADO - Campo no existe en BD
                + "pv.ean, "
                + "pv.precio_compra, "
                + "pv.precio_venta, "
                + "pv.stock_minimo_variante, "
                + "pv.disponible, "
                + "t.numero AS nombre_talla, "
                + "col.nombre AS nombre_color, "
                + "COALESCE(SUM(ib.Stock_par),0) AS stock_por_pares, "
                + "COALESCE(SUM(ib.Stock_caja),0) AS stock_por_cajas "
                + "FROM producto_variantes pv "
                + "LEFT JOIN tallas t ON pv.id_talla=t.id_talla "
                + "LEFT JOIN colores col ON pv.id_color=col.id_color "
                + "LEFT JOIN inventario_bodega ib ON pv.id_variante=ib.id_variante "
                + "  AND ib.activo=1 "
                + "  AND ib.id_bodega=? "
                + "WHERE pv.id_producto=? "
                + "GROUP BY pv.id_variante "
                + "ORDER BY t.numero, col.nombre";

        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idBodega);
            pst.setInt(2, idProducto);

            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    raven.controlador.productos.ModelProductVariant v = new raven.controlador.productos.ModelProductVariant();

                    v.setVariantId(rs.getInt("id_variante"));
                    v.setProductId(rs.getInt("id_producto"));
                    v.setSizeId(rs.getInt("id_talla"));
                    v.setColorId(rs.getInt("id_color"));
                    v.setSku(rs.getString("sku"));

                    // WARNING  CORRECCIÓN: Campo codigo_barras no existe, usar EAN o null
                    v.setBarcode(null);
                    // v.setBarcode(rs.getString("ean")); // Alternativa

                    v.setEan(rs.getString("ean"));

                    Double pc = rs.getDouble("precio_compra");
                    v.setPurchasePrice(rs.wasNull() ? null : pc);

                    Double pv = rs.getDouble("precio_venta");
                    v.setSalePrice(rs.wasNull() ? null : pv);

                    int sm = rs.getInt("stock_minimo_variante");
                    v.setMinStock(rs.wasNull() ? null : sm);

                    v.setAvailable(rs.getBoolean("disponible"));
                    v.setSizeName(rs.getString("nombre_talla"));
                    v.setColorName(rs.getString("nombre_color"));
                    v.setStockPairs(rs.getInt("stock_por_pares"));
                    v.setStockBoxes(rs.getInt("stock_por_cajas"));

                    list.add(v);
                }
            }
        }
        return list;
    }

    // Compatibilidad: estadísticas de variantes por producto
    public static class ProductVariantStats {

        public int variantes;
        public int totalPares;
        public int totalCajas;
        public int totalBoxes;
        public int totalPairsEquivalent;
    }

    /**
     * Obtiene estadísticas consolidadas de variantes de un producto. Calcula
     * totales de stock en todas las bodegas.
     *
     * @param idProducto ID del producto
     * @return ProductVariantStats con las estadísticas consolidadas
     * @throws SQLException Si ocurre un error en la consulta
     */
    public ProductVariantStats getProductStats(int idProducto) throws SQLException {
        ProductVariantStats stats = new ProductVariantStats();

        String sql = "SELECT "
                + "    COUNT(*) AS variantes, "
                + "    COALESCE(SUM(ib.Stock_par), 0) AS pares, "
                + "    COALESCE(SUM(ib.Stock_caja), 0) AS cajas "
                + "FROM producto_variantes pv "
                + "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante "
                + "WHERE pv.id_producto = ?";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idProducto);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    stats.variantes = rs.getInt("variantes");
                    stats.totalPares = rs.getInt("pares");
                    stats.totalCajas = rs.getInt("cajas");
                    stats.totalBoxes = stats.totalCajas;
                    stats.totalPairsEquivalent = stats.totalPares + (stats.totalCajas * 24);
                }
            }
        }

        return stats; // SUCCESS  CORRECTO: return dentro del método, antes de la llave final
    } // SUCCESS  Cierre correcto del método getProductStats

    /**
     * Inserta una variante CON su inventario de forma ATÓMICA.
     *
     * GARANTÍAS: - Si falla la variante, NO se crea inventario - Si falla el
     * inventario, se REVIERTE la variante - Todo o nada (ACID)
     *
     * @param v Variante a insertar
     * @return true si se insertó correctamente
     * @throws SQLException Error de base de datos
     */
    public boolean insertVariant(ModelProductVariant v) throws SQLException {
        // SUCCESS  VALIDACIONES
        if (v == null) {
            System.err.println("ERROR  Variante es null");
            return false;
        }

        if (v.getProductId() <= 0) {
            System.err.println("ERROR  ID de producto inválido: " + v.getProductId());
            return false;
        }

        if (v.getSizeId() <= 0 || v.getColorId() <= 0) {
            System.err.println("ERROR  IDs de talla/color inválidos: "
                    + v.getSizeId() + "/" + v.getColorId());
            return false;
        }

        Connection con = null;

        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false); // SUCCESS  Inicio transacción

            System.out.println("Actualizando Iniciando transacción para variante: "
                    + v.getSizeName() + " - " + v.getColorName());

            // ===============================================================
            // PASO 1: INSERTAR VARIANTE
            // ===============================================================
            ProductoVariantesDAO variantDAO = new ProductoVariantesDAO();
            int idVariante = variantDAO.insert(v);

            if (idVariante <= 0) {
                System.err.println("ERROR  Falló inserción de variante");
                throw new SQLException("No se pudo insertar la variante");
            }

            v.setVariantId(idVariante);
            System.out.println("SUCCESS  Variante insertada con ID: " + idVariante);

            // ===============================================================
            // PASO 2: INSERTAR INVENTARIO
            // ===============================================================
            if (v.getWarehouseId() != null && v.getWarehouseId() > 0) {

                int stockPares = Math.max(0, v.getStockPairs());
                int stockCajas = Math.max(0, v.getStockBoxes());

                System.out.println("Caja Creando inventario en bodega " + v.getWarehouseId()
                        + ": " + stockPares + " pares, " + stockCajas + " cajas");

                InventarioBodegaDAO inventarioDAO = new InventarioBodegaDAO();
                inventarioDAO.upsert(
                        v.getWarehouseId(),
                        idVariante,
                        stockPares,
                        stockCajas,
                        v.getWarehouseLocation()
                );

                System.out.println("SUCCESS  Inventario creado correctamente");
            } else {
                System.out.println("ℹ No se especificó bodega, variante sin inventario inicial");
            }

            // ===============================================================
            // COMMIT
            // ===============================================================
            con.commit();
            System.out.println("SUCCESS  Transacción completada exitosamente\n");

            return true;

        } catch (SQLException e) {
            // ===============================================================
            // ROLLBACK
            // ===============================================================
            System.err.println("ERROR  Error insertando variante: " + e.getMessage());

            if (con != null) {
                try {
                    con.rollback();
                    System.err.println("WARNING  Rollback ejecutado - cambios revertidos");
                } catch (SQLException rollbackEx) {
                    System.err.println("ERROR  Error en rollback: " + rollbackEx.getMessage());
                }
            }

            throw e;

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException closeEx) {
                    System.err.println("WARNING  Error cerrando conexión: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Inserta o actualiza una variante (upsert) con inventario.
     *
     * LÓGICA: 1. Si variantId > 0 → Actualizar 2. Si existe
     * (producto+talla+color) → Actualizar 3. Si no existe → Insertar 4.
     * Sincronizar inventario en bodega
     *
     * TODO en una TRANSACCIÓN ATÓMICA.
     *
     * @param v Variante a guardar
     * @return ID de la variante
     * @throws SQLException Error de base de datos
     */
    public int upsertVariant(ModelProductVariant v) throws SQLException {
        if (v == null) {
            throw new IllegalArgumentException("Variante es null");
        }

        Connection con = null;

        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            ProductoVariantesDAO variantDAO = new ProductoVariantesDAO();
            InventarioBodegaDAO inventarioDAO = new InventarioBodegaDAO();

            Integer idVariante = null;

            // ===============================================================
            // PASO 1: DETERMINAR SI EXISTE
            // ===============================================================
            if (v.getVariantId() > 0) {
                idVariante = v.getVariantId();
                System.out.println("Buscar Variante con ID existente: " + idVariante);
            } else {
                idVariante = variantDAO.findExistingId(
                        v.getProductId(),
                        v.getSizeId(),
                        v.getColorId(),
                        v.getSupplierId());

                if (idVariante != null) {
                    System.out.println("Buscar Variante encontrada por clave compuesta: " + idVariante);
                }
            }

            // ===============================================================
            // PASO 2: INSERTAR O ACTUALIZAR
            // ===============================================================
            if (idVariante == null) {
                // INSERTAR
                System.out.println("Nota Insertando nueva variante");

                idVariante = variantDAO.insert(v);
                if (idVariante <= 0) {
                    throw new SQLException("Falló inserción de variante");
                }

                v.setVariantId(idVariante);

            } else {
                // ACTUALIZAR
                System.out.println("Actualizando Actualizando variante existente ID: " + idVariante);

                v.setVariantId(idVariante);
                variantDAO.update(v);

                // Actualizar imagen si existe
                if (v.getImageBytes() != null && v.getImageBytes().length > 0) {
                    variantDAO.updateImage(idVariante, v.getImageBytes());
                    System.out.println(" Imagen actualizada");
                }
            }

            // ===============================================================
            // PASO 3: SINCRONIZAR INVENTARIO (sin cambiar id_bodega existente)
            // ===============================================================
            Integer bodegaTarget = inventarioDAO.findExistingBodegaForVariante(idVariante);
            if (bodegaTarget == null) {
                bodegaTarget = v.getWarehouseId();
            }
            if (bodegaTarget != null && bodegaTarget > 0) {
                inventarioDAO.upsert(
                        bodegaTarget,
                        idVariante,
                        Math.max(0, v.getStockPairs()),
                        Math.max(0, v.getStockBoxes()),
                        v.getWarehouseLocation());
                System.out.println("Caja Inventario sincronizado en bodega " + bodegaTarget);
            }

            con.commit();
            System.out.println("SUCCESS  Upsert completado para variante ID: " + idVariante + "\n");

            return idVariante;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                    System.err.println("WARNING  Rollback ejecutado en upsert");
                } catch (SQLException rollbackEx) {
                    System.err.println("ERROR  Error en rollback: " + rollbackEx.getMessage());
                }
            }
            throw e;

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException closeEx) {
                    System.err.println("WARNING  Error cerrando conexión: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Convierte un Double nullable a BigDecimal de forma segura. Implementa el
     * patrón Null Object para evitar NullPointerExceptions.
     *
     * Este método es parte de una estrategia defensive programming para manejar
     * valores null de forma elegante sin lanzar excepciones.
     *
     * @param value Valor Double a convertir (puede ser null)
     * @return BigDecimal.ZERO si value es null, caso contrario el valor
     *         convertido
     */
    private BigDecimal convertToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    /**
     * Convierte un Integer nullable a int primitivo de forma segura.
     *
     * Patrón aplicado: Null Object Pattern Evita NullPointerException al
     * trabajar con tipos wrapper
     *
     * @param value Valor Integer a convertir (puede ser null)
     * @return 0 si value es null, caso contrario el valor convertido
     */
    private int convertToInt(Integer value) {
        return value != null ? value : 0;
    }

    // Compatibilidad: búsqueda de variantes
    public static class VariantSearchResult {

        public int idVariante;
        public int idProducto;
        public String talla;
        public String color;
        public String sku;
        public String ean;
        public byte[] imageBytes;
        public String nombre;
        public String colorNombre;
        public String tallaNombre;
    }

    public java.util.List<VariantSearchResult> searchVariantsByTerm(String term, Integer idBodega, int limit)
            throws SQLException {
        String base = "SELECT pv.id_variante, pv.id_producto, t.numero AS talla, c.nombre AS color, pv.sku, pv.ean, pv.imagen, p.nombre AS nombre_producto FROM producto_variantes pv LEFT JOIN tallas t ON pv.id_talla=t.id_talla LEFT JOIN colores c ON pv.id_color=c.id_color LEFT JOIN productos p ON pv.id_producto=p.id_producto";
        String where = " WHERE (pv.sku LIKE ? OR pv.ean LIKE ? OR c.nombre LIKE ? OR t.numero LIKE ?)"
                + (idBodega != null
                        ? " AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante=pv.id_variante AND ib.id_bodega=? AND ib.activo=1)"
                        : "");
        String sql = base + where + " ORDER BY pv.id_producto, t.numero LIMIT ?";
        java.util.List<VariantSearchResult> res = new java.util.ArrayList<>();
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {
            String pat = "%" + (term == null ? "" : term.trim()) + "%";
            pst.setString(1, pat);
            pst.setString(2, pat);
            pst.setString(3, pat);
            pst.setString(4, pat);
            int idx = 5;
            if (idBodega != null) {
                pst.setInt(idx++, idBodega);
            }
            pst.setInt(idx, Math.max(1, limit));
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    VariantSearchResult v = new VariantSearchResult();
                    v.idVariante = rs.getInt("id_variante");
                    v.idProducto = rs.getInt("id_producto");
                    v.talla = rs.getString("talla");
                    v.color = rs.getString("color");
                    v.sku = rs.getString("sku");
                    v.ean = rs.getString("ean");
                    v.imageBytes = rs.getBytes("imagen");
                    v.nombre = rs.getString("nombre_producto");
                    v.colorNombre = v.color;
                    v.tallaNombre = v.talla;
                    res.add(v);
                }
            }
        }
        return res;
    }

    /**
     * Actualiza solo la imagen de una variante.
     *
     * Operación independiente y simple, no requiere transacción compleja.
     *
     * @param idVariante ID de la variante
     * @param imageBytes Bytes de la imagen
     * @return true si se actualizó correctamente
     * @throws SQLException Error de base de datos
     */
    public boolean updateVariantImage(int idVariante, byte[] imageBytes) throws SQLException {
        if (idVariante <= 0) {
            throw new IllegalArgumentException("ID de variante inválido");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            System.out.println("WARNING  No hay imagen para actualizar");
            return false;
        }

        try {
            ProductoVariantesDAO dao = new ProductoVariantesDAO();
            dao.updateImage(idVariante, imageBytes);

            System.out.println("SUCCESS  Imagen actualizada para variante ID: " + idVariante
                    + " (" + (imageBytes.length / 1024) + " KB)");

            return true;

        } catch (SQLException e) {
            System.err.println("ERROR  Error actualizando imagen: " + e.getMessage());
            throw e;
        }
    }

    public byte[] getVariantImage(Integer idVariante) throws SQLException {
        String sql = "SELECT imagen FROM producto_variantes WHERE id_variante=?";
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idVariante);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes(1);
                }
            }
        }
        return null;
    }

    // Compatibilidad: ajustar stock de pares (restar/sumar) en cualquier bodega
    // activa
    public boolean reducePairsStock(Integer idVariante, int cantidad) throws SQLException {
        if (idVariante == null || cantidad <= 0) {
            return false;
        }
        String sqlSel = "SELECT id_inventario_bodega, Stock_par FROM inventario_bodega WHERE id_variante=? AND activo=1 ORDER BY Stock_par DESC";
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement psSel = conn.prepareStatement(sqlSel)) {
            psSel.setInt(1, idVariante);
            try (java.sql.ResultSet rs = psSel.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                int idInv = rs.getInt(1), sp = rs.getInt(2);
                if (sp < cantidad) {
                    return false;
                }
                String sqlUpd = "UPDATE inventario_bodega SET Stock_par=GREATEST(0, Stock_par-?), fecha_ultimo_movimiento=NOW() WHERE id_inventario_bodega=?";
                try (java.sql.PreparedStatement psUpd = conexion.getInstance().getConnection()
                        .prepareStatement(sqlUpd)) {
                    psUpd.setInt(1, cantidad);
                    psUpd.setInt(2, idInv);
                    return psUpd.executeUpdate() > 0;
                }
            }
        }
    }

    public boolean addPairsStock(Integer idVariante, int cantidad) throws SQLException {
        if (idVariante == null || cantidad <= 0) {
            return false;
        }
        String sqlSel = "SELECT id_inventario_bodega FROM inventario_bodega WHERE id_variante=? AND activo=1 ORDER BY id_inventario_bodega ASC";
        try (java.sql.Connection conn = conexion.getInstance().getConnection();
                java.sql.PreparedStatement psSel = conn.prepareStatement(sqlSel)) {
            psSel.setInt(1, idVariante);
            try (java.sql.ResultSet rs = psSel.executeQuery()) {
                if (rs.next()) {
                    int idInv = rs.getInt(1);
                    String sqlUpd = "UPDATE inventario_bodega SET Stock_par=Stock_par+?, fecha_ultimo_movimiento=NOW() WHERE id_inventario_bodega=?";
                    try (java.sql.PreparedStatement psUpd = conexion.getInstance().getConnection()
                            .prepareStatement(sqlUpd)) {
                        psUpd.setInt(1, cantidad);
                        psUpd.setInt(2, idInv);
                        return psUpd.executeUpdate() > 0;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Obtiene el stock consolidado de una variante (suma de todas las bodegas).
     *
     * @param idVariante ID de la variante
     * @return Map con totales: "pares", "cajas", "reservado", "disponible"
     * @throws SQLException Si hay error en la consulta
     */
    public Map<String, Integer> getStockConsolidado(int idVariante) throws SQLException {
        Map<String, Integer> stockMap = new HashMap<>();

        String sql = "SELECT "
                + "    COALESCE(SUM(Stock_par), 0) AS total_pares, "
                + "    COALESCE(SUM(Stock_caja), 0) AS total_cajas, "
                + "    COALESCE(SUM(stock_reservado), 0) AS total_reservado, "
                + "    COALESCE(SUM(Stock_par - stock_reservado), 0) AS total_disponible "
                + "FROM inventario_bodega "
                + "WHERE id_variante = ? AND activo = 1";

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idVariante);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    stockMap.put("pares", rs.getInt("total_pares"));
                    stockMap.put("cajas", rs.getInt("total_cajas"));
                    stockMap.put("reservado", rs.getInt("total_reservado"));
                    stockMap.put("disponible", rs.getInt("total_disponible"));
                }
            }
        }

        return stockMap;
    }

    /*
     * ============================================================================
     * MÉTODOS DE MODIFICACIÓN
     * ============================================================================
     */
    /**
     * Crea una nueva variante con inventario inicial en una bodega. Operación
     * transaccional: crea variante y registro de inventario.
     *
     * Ejemplo:
     * 
     * <pre>
     * int idVariante = service.crearVarianteConInventario(
     *         17, // id_producto
     *         5, // id_talla (talla 40 EU)
     *         3, // id_color
     *         "PROD17-40-3", // sku
     *         "7701234567890", // ean
     *         new BigDecimal("50000"), // precio compra
     *         new BigDecimal("80000"), // precio venta
     *         10, // stock mínimo
     *         4, // id_bodega principal
     *         24, // stock inicial pares
     *         2 // stock inicial cajas
     * );
     * </pre>
     *
     * @return ID de la variante creada
     * @throws SQLException Si hay error en la operación
     */
    public int crearVarianteConInventario(int idProducto, int idTalla, int idColor,
            String sku, String ean, BigDecimal precioCompra,
            BigDecimal precioVenta, int stockMinimo,
            int idBodega, int stockInicialPares,
            int stockInicialCajas) throws SQLException {

        Connection conn = null;
        int idVariante = 0;

        try {
            conn = conexion.getInstance().getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Insertar variante
            try (PreparedStatement pstVariante = conn.prepareStatement(SQL_INSERT_VARIANTE,
                    Statement.RETURN_GENERATED_KEYS)) {

                pstVariante.setInt(1, idProducto);
                pstVariante.setInt(2, idTalla);
                pstVariante.setInt(3, idColor);
                pstVariante.setNull(4, java.sql.Types.BLOB);
                pstVariante.setString(5, ean);
                pstVariante.setString(6, sku);
                pstVariante.setBigDecimal(7, precioCompra);
                pstVariante.setBigDecimal(8, precioVenta);
                pstVariante.setInt(9, stockMinimo);

                pstVariante.executeUpdate();

                try (ResultSet rs = pstVariante.getGeneratedKeys()) {
                    if (rs.next()) {
                        idVariante = rs.getInt(1);
                    }
                }
            }

            // 2. Crear registro de inventario inicial
            if (idVariante > 0) {
                String sqlInventario = "INSERT INTO inventario_bodega "
                        + "(id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, "
                        + "fecha_ultimo_movimiento, activo) "
                        + "VALUES (?, ?, ?, ?, 0, NOW(), 1)";

                try (PreparedStatement pstInv = conn.prepareStatement(sqlInventario)) {
                    pstInv.setInt(1, idBodega);
                    pstInv.setInt(2, idVariante);
                    pstInv.setInt(3, stockInicialPares);
                    pstInv.setInt(4, stockInicialCajas);

                    pstInv.executeUpdate();
                }
            }

            conn.commit(); // Confirmar transacción
            return idVariante;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Actualiza información de una variante (sin modificar stock). Para
     * modificar stock, usar ServiceInventarioBodega.
     *
     * @param idVariante   ID de la variante
     * @param idTalla      Nueva talla
     * @param idColor      Nuevo color
     * @param precioCompra Nuevo precio de compra
     * @param precioVenta  Nuevo precio de venta
     * @param stockMinimo  Nuevo stock mínimo
     * @param disponible   Si está disponible
     * @throws SQLException Si hay error en la operación
     */
    public void actualizarVariante(int idVariante, int idTalla, int idColor,
            BigDecimal precioCompra, BigDecimal precioVenta,
            int stockMinimo, boolean disponible) throws SQLException {

        try (Connection conn = conexion.getInstance().getConnection();
                PreparedStatement pst = conn.prepareStatement(SQL_UPDATE_VARIANTE)) {

            pst.setInt(1, idTalla);
            pst.setInt(2, idColor);
            pst.setBigDecimal(3, precioCompra);
            pst.setBigDecimal(4, precioVenta);
            pst.setInt(5, stockMinimo);
            pst.setBoolean(6, disponible);
            pst.setInt(7, idVariante);

            pst.executeUpdate();
        }
    }

    /*
     * ============================================================================
     * CLASES INTERNAS - DTOs
     * ============================================================================
     */
    /**
     * DTO que representa una variante con su inventario en una bodega
     * específica. Útil para mostrar en tablas y listas.
     *
     * ADAPTADO: Para trabajar con ModelProductVariant
     */
    public static class VarianteConInventario {

        private ModelProductVariant variante; // Usar modelo existente

        // Datos adicionales de inventario en bodega específica
        private Integer idInventarioBodega;
        private Integer idBodega;
        private String nombreBodega;
        private String codigoBodega;
        private Integer stockPar;
        private Integer stockCaja;
        private Integer stockReservado;
        private Integer stockDisponible;
        private String ubicacionEspecifica;

        // Constructor
        public VarianteConInventario() {
            this.variante = new ModelProductVariant();
        }

        // Constructor con variante
        public VarianteConInventario(ModelProductVariant variante) {
            this.variante = variante;
        }

        // Getters y setters delegados a ModelProductVariant
        public int getIdVariante() {
            return variante.getVariantId();
        }

        public void setIdVariante(int idVariante) {
            variante.setVariantId(idVariante);
        }

        public int getIdProducto() {
            return variante.getProductId();
        }

        public void setIdProducto(int idProducto) {
            variante.setProductId(idProducto);
        }

        public String getTalla() {
            return variante.getSizeName();
        }

        public void setTalla(String talla) {
            variante.setSizeName(talla);
        }

        public String getColor() {
            return variante.getColorName();
        }

        public void setColor(String color) {
            variante.setColorName(color);
        }

        public String getSku() {
            return variante.getSku();
        }

        public void setSku(String sku) {
            variante.setSku(sku);
        }

        public String getEan() {
            return variante.getEan();
        }

        public void setEan(String ean) {
            variante.setEan(ean);
        }

        public Double getPrecioCompra() {
            return variante.getPurchasePrice();
        }

        public void setPrecioCompra(Double precio) {
            variante.setPurchasePrice(precio);
        }

        public Double getPrecioVenta() {
            return variante.getSalePrice();
        }

        public void setPrecioVenta(Double precio) {
            variante.setSalePrice(precio);
        }

        public Integer getStockMinimo() {
            return variante.getMinStock();
        }

        public void setStockMinimo(Integer stock) {
            variante.setMinStock(stock);
        }

        public boolean isDisponible() {
            return variante.isAvailable();
        }

        public void setDisponible(boolean disponible) {
            variante.setAvailable(disponible);
        }

        // Getters y setters de inventario
        public Integer getIdInventarioBodega() {
            return idInventarioBodega;
        }

        public void setIdInventarioBodega(Integer id) {
            this.idInventarioBodega = id;
        }

        public Integer getIdBodega() {
            return idBodega;
        }

        public void setIdBodega(Integer id) {
            this.idBodega = id;
        }

        public String getNombreBodega() {
            return nombreBodega;
        }

        public void setNombreBodega(String nombre) {
            this.nombreBodega = nombre;
        }

        public String getCodigoBodega() {
            return codigoBodega;
        }

        public void setCodigoBodega(String codigo) {
            this.codigoBodega = codigo;
        }

        public Integer getStockPar() {
            return stockPar;
        }

        public void setStockPar(Integer stock) {
            this.stockPar = stock;
        }

        public Integer getStockCaja() {
            return stockCaja;
        }

        public void setStockCaja(Integer stock) {
            this.stockCaja = stock;
        }

        public Integer getStockReservado() {
            return stockReservado;
        }

        public void setStockReservado(Integer stock) {
            this.stockReservado = stock;
        }

        public Integer getStockDisponible() {
            return stockDisponible;
        }

        public void setStockDisponible(Integer stock) {
            this.stockDisponible = stock;
        }

        public String getUbicacionEspecifica() {
            return ubicacionEspecifica;
        }

        public void setUbicacionEspecifica(String ubicacion) {
            this.ubicacionEspecifica = ubicacion;
        }

        // Acceso a la variante completa
        public ModelProductVariant getVariante() {
            return variante;
        }

        public void setVariante(ModelProductVariant variante) {
            this.variante = variante;
        }
    }

    /**
     * DTO que representa una variante con su inventario en todas las bodegas.
     */
    public static class VarianteCompleta {

        private int idVariante;
        private int idProducto;
        private String nombreProducto;
        private String codigoModelo;
        private String talla;
        private String color;
        private String sku;
        private String ean;
        private BigDecimal precioCompra;
        private BigDecimal precioVenta;
        private int stockMinimo;
        private int totalPares;
        private int totalCajas;

        private List<InventarioBodega> inventarios;

        public VarianteCompleta() {
            inventarios = new ArrayList<>();
        }

        public int getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(int idProducto) {
            this.idProducto = idProducto;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public void setNombreProducto(String nombreProducto) {
            this.nombreProducto = nombreProducto;
        }

        public String getCodigoModelo() {
            return codigoModelo;
        }

        public void setCodigoModelo(String codigoModelo) {
            this.codigoModelo = codigoModelo;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getEan() {
            return ean;
        }

        public void setEan(String ean) {
            this.ean = ean;
        }

        public BigDecimal getPrecioCompra() {
            return precioCompra;
        }

        public void setPrecioCompra(BigDecimal precioCompra) {
            this.precioCompra = precioCompra;
        }

        public BigDecimal getPrecioVenta() {
            return precioVenta;
        }

        public void setPrecioVenta(BigDecimal precioVenta) {
            this.precioVenta = precioVenta;
        }

        public int getStockMinimo() {
            return stockMinimo;
        }

        public void setStockMinimo(int stockMinimo) {
            this.stockMinimo = stockMinimo;
        }

        public int getTotalPares() {
            return totalPares;
        }

        public void setTotalPares(int totalPares) {
            this.totalPares = totalPares;
        }

        public int getTotalCajas() {
            return totalCajas;
        }

        public void setTotalCajas(int totalCajas) {
            this.totalCajas = totalCajas;
        }

        // Getters y setters...
        public int getIdVariante() {
            return idVariante;
        }

        public void setIdVariante(int idVariante) {
            this.idVariante = idVariante;
        }

        public String getTalla() {
            return talla;
        }

        public void setTalla(String talla) {
            this.talla = talla;
        }

        public List<InventarioBodega> getInventarios() {
            return inventarios;
        }

        public void setInventarios(List<InventarioBodega> inventarios) {
            this.inventarios = inventarios;
        }

        // ... más getters/setters
    }

    /*
     * ============================================================================
     * MÉTODOS AUXILIARES
     * ============================================================================
     */
    /**
     * Mapea un ResultSet a VarianteConInventario.
     */
    /**
     * Mapea un ResultSet a un VarianteConInventario (que internamente usa
     * ModelProductVariant). Este método se usa cuando se consultan variantes
     * con información de inventario.
     *
     * @param rs ResultSet de la consulta
     * @return VarianteConInventario con datos de la variante e inventario
     * @throws SQLException Si hay error al leer los datos
     */
    private VarianteConInventario mapearVarianteConInventario(ResultSet rs) throws SQLException {
        // Crear la variante base usando ModelProductVariant
        ModelProductVariant variante = new ModelProductVariant();

        // Mapear campos principales de la variante
        variante.setVariantId(rs.getInt("id_variante"));
        variante.setProductId(rs.getInt("id_producto"));
        variante.setSku(rs.getString("sku"));
        variante.setEan(rs.getString("ean"));
        variante.setBarcode(rs.getString("codigo_barras"));

        // Talla y color (IDs y nombres si están disponibles)
        if (hasColumn(rs, "id_talla")) {
            variante.setSizeId(rs.getInt("id_talla"));
        }
        if (hasColumn(rs, "id_color")) {
            variante.setColorId(rs.getInt("id_color"));
        }
        if (hasColumn(rs, "talla") || hasColumn(rs, "nombre_talla")) {
            String talla = hasColumn(rs, "talla") ? rs.getString("talla") : rs.getString("nombre_talla");
            variante.setSizeName(talla);
        }
        if (hasColumn(rs, "color") || hasColumn(rs, "nombre_color")) {
            String color = hasColumn(rs, "color") ? rs.getString("color") : rs.getString("nombre_color");
            variante.setColorName(color);
        }

        // Precios
        Double precioCompra = rs.getDouble("precio_compra");
        variante.setPurchasePrice(rs.wasNull() ? null : precioCompra);

        Double precioVenta = rs.getDouble("precio_venta");
        variante.setSalePrice(rs.wasNull() ? null : precioVenta);

        // Stock mínimo
        int stockMinimo = rs.getInt("stock_minimo_variante");
        variante.setMinStock(rs.wasNull() ? null : stockMinimo);

        // Disponibilidad
        variante.setAvailable(rs.getBoolean("disponible"));

        // Crear el wrapper con la variante
        VarianteConInventario v = new VarianteConInventario(variante);

        // Mapear datos de inventario (pueden ser null si no hay stock en bodega)
        Integer idInv = rs.getInt("id_inventario_bodega");
        if (!rs.wasNull()) {
            v.setIdInventarioBodega(idInv);
            v.setIdBodega(rs.getInt("id_bodega"));

            if (hasColumn(rs, "nombre_bodega")) {
                v.setNombreBodega(rs.getString("nombre_bodega"));
            }
            if (hasColumn(rs, "codigo_bodega")) {
                v.setCodigoBodega(rs.getString("codigo_bodega"));
            }

            v.setStockPar(rs.getInt("Stock_par"));
            v.setStockCaja(rs.getInt("Stock_caja"));
            v.setStockReservado(rs.getInt("stock_reservado"));
            v.setStockDisponible(rs.getInt("stock_disponible"));

            if (hasColumn(rs, "ubicacion_especifica")) {
                v.setUbicacionEspecifica(rs.getString("ubicacion_especifica"));
            }
        }

        return v;
    }

    /**
     * Verifica si una columna existe en el ResultSet. Útil para queries
     * dinámicas con diferentes conjuntos de columnas.
     *
     * z * @param rs ResultSet a verificar
     *
     * @param columnName Nombre de la columna
     * @return true si existe, false si no
     */
    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}

