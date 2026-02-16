package raven.dao;

import java.sql.*;
import raven.application.form.productos.dto.InventarioDetalleItem;
import raven.controlador.principal.conexion;

/**
 * DAO mejorado para gestión de inventario por bodega.
 * 
 * PATRÓN IMPLEMENTADO: Transaction Script
 * - Cada operación se ejecuta con control transaccional
 * - Rollback automático en caso de error
 * - Prevención de race conditions con INSERT...ON DUPLICATE KEY UPDATE
 * 
 * PRINCIPIO: ACID Compliance
 * - Atomicity: Todo o nada
 * - Consistency: Estado válido siempre
 * - Isolation: Sin interferencias entre transacciones
 * - Durability: Cambios permanentes
 * 
 * MEJORAS CLAVE:
 * - Stock se REEMPLAZA, NO se SUMA (evita duplicación)
 * - Validaciones exhaustivas
 * - Transacciones completas
 * 
 * @author Ingeniero Senior
 * @version 2.2
 */
public class InventarioBodegaDAO {

    /**
     * Inserta o actualiza inventario de una variante en una bodega.
     * 
     * ESTRATEGIA:
     * - Verifica existencia previa para evitar duplicados si falta la constraint
     * UNIQUE
     * - Usa UPDATE si existe, INSERT si no
     * - Mantiene integridad de datos
     * 
     * @param idBodega   ID de la bodega
     * @param idVariante ID de la variante
     * @param stockPar   Stock en pares (cantidad EXACTA)
     * @param stockCaja  Stock en cajas (cantidad EXACTA)
     * @param ubicacion  Ubicación específica (NULL si no se usa)
     * @throws SQLException Error de base de datos
     */
    public void upsert(int idBodega, int idVariante, int stockPar, int stockCaja, String ubicacion)
            throws SQLException {

        // SUCCESS VALIDACIONES
        if (idBodega <= 0) {
            throw new IllegalArgumentException("ID de bodega inválido: " + idBodega);
        }

        if (idVariante <= 0) {
            throw new IllegalArgumentException("ID de variante inválido: " + idVariante);
        }

        if (stockPar < 0) {
            throw new IllegalArgumentException("Stock de pares no puede ser negativo: " + stockPar);
        }

        if (stockCaja < 0) {
            throw new IllegalArgumentException("Stock de cajas no puede ser negativo: " + stockCaja);
        }

        Connection con = null;
        PreparedStatement pstCheck = null;
        PreparedStatement pstAct = null;

        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false); // Iniciar transacción

            // 1. Verificar si existe registro (para evitar duplicados si no hay UNIQUE KEY)
            String sqlCheck = "SELECT id_inventario_bodega FROM inventario_bodega WHERE id_bodega = ? AND id_variante = ? LIMIT 1 FOR UPDATE";
            pstCheck = con.prepareStatement(sqlCheck);
            pstCheck.setInt(1, idBodega);
            pstCheck.setInt(2, idVariante);

            boolean existe = false;
            try (ResultSet rs = pstCheck.executeQuery()) {
                if (rs.next()) {
                    existe = true;
                }
            }

            String sqlAction;
            if (existe) {
                // UPDATE
                sqlAction = "UPDATE inventario_bodega SET " +
                        "Stock_par = ?, " +
                        "Stock_caja = ?, " +
                        "fecha_ultimo_movimiento = CURRENT_TIMESTAMP, " +
                        "ubicacion_especifica = ?, " +
                        "activo = 1 " +
                        "WHERE id_bodega = ? AND id_variante = ?";
            } else {
                // INSERT
                sqlAction = "INSERT INTO inventario_bodega (" +
                        "Stock_par, " +
                        "Stock_caja, " +
                        "ubicacion_especifica, " +
                        "id_bodega, " +
                        "id_variante, " +
                        "stock_reservado, " +
                        "fecha_ultimo_movimiento, " +
                        "activo" +
                        ") VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, 1)";
            }

            pstAct = con.prepareStatement(sqlAction);
            pstAct.setInt(1, stockPar);
            pstAct.setInt(2, stockCaja);
            pstAct.setString(3, ubicacion);
            pstAct.setInt(4, idBodega);
            pstAct.setInt(5, idVariante);

            int affectedRows = pstAct.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo insertar/actualizar inventario");
            }

            con.commit();

            System.out.println("SUCCESS  Inventario actualizado (Safe Mode): Bodega=" + idBodega +
                    ", Variante=" + idVariante +
                    ", Pares=" + stockPar +
                    ", Cajas=" + stockCaja +
                    ", Activo=1");

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("ERROR  Error en upsert inventario: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (pstCheck != null)
                pstCheck.close();
            if (pstAct != null)
                pstAct.close();
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    public Integer findExistingBodegaForVariante(int idVariante) throws SQLException {
        String sql = "SELECT id_bodega FROM inventario_bodega WHERE id_variante=? AND activo=1 ORDER BY fecha_ultimo_movimiento DESC, id_inventario_bodega ASC LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idVariante);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        return null;
    }

    /**
     * Obtiene el stock total de un producto en todas las bodegas activas.
     * 
     * @param idBodega   ID de la bodega (null para todas)
     * @param idProducto ID del producto
     * @return Array [stockPares, stockCajas]
     * @throws SQLException Error de base de datos
     */
    public int[] getTotalsByProduct(Integer idBodega, int idProducto) throws SQLException {
        if (idProducto <= 0) {
            throw new IllegalArgumentException("ID de producto inválido: " + idProducto);
        }

        // SUCCESS Filtrar por inventario activo
        String sql = "SELECT " +
                "COALESCE(SUM(ib.Stock_par), 0) AS pares, " +
                "COALESCE(SUM(ib.Stock_caja), 0) AS cajas " +
                "FROM inventario_bodega ib " +
                "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
                "WHERE pv.id_producto = ? " +
                "AND ib.activo = 1"; // SUCCESS Solo inventario activo

        if (idBodega != null && idBodega > 0) {
            sql += " AND ib.id_bodega = ?";
        }

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idProducto);

            if (idBodega != null && idBodega > 0) {
                pst.setInt(2, idBodega);
            }

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new int[] { rs.getInt("pares"), rs.getInt("cajas") };
                }
            }
        }

        return new int[] { 0, 0 };
    }

    /**
     * Decrementa una caja del stock de forma transaccional.
     * 
     * @param idVariante ID de la variante
     * @throws SQLException          Error de base de datos
     * @throws IllegalStateException Si no hay stock disponible
     */
    public void decrementOneBox(int idVariante) throws SQLException {
        if (idVariante <= 0) {
            throw new IllegalArgumentException("ID de variante inválido: " + idVariante);
        }

        Connection con = null;
        PreparedStatement pstCheck = null;
        PreparedStatement pstUpdate = null;

        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            // Verificar stock disponible
            String sqlCheck = "SELECT Stock_caja " +
                    "FROM inventario_bodega " +
                    "WHERE id_variante = ? " +
                    "AND activo = 1 " + // SUCCESS Solo inventario activo
                    "AND Stock_caja > 0 " +
                    "ORDER BY Stock_caja DESC " +
                    "LIMIT 1 " +
                    "FOR UPDATE";

            pstCheck = con.prepareStatement(sqlCheck);
            pstCheck.setInt(1, idVariante);

            int stockDisponible = 0;
            try (ResultSet rs = pstCheck.executeQuery()) {
                if (rs.next()) {
                    stockDisponible = rs.getInt("Stock_caja");
                } else {
                    throw new IllegalStateException(
                            "No hay stock de cajas disponible para la variante: " + idVariante);
                }
            }

            // Decrementar stock
            String sqlUpdate = "UPDATE inventario_bodega SET " +
                    "Stock_caja = Stock_caja - 1, " +
                    "fecha_ultimo_movimiento = CURRENT_TIMESTAMP " +
                    "WHERE id_variante = ? " +
                    "AND activo = 1 " +
                    "AND Stock_caja > 0 " +
                    "ORDER BY Stock_caja DESC " +
                    "LIMIT 1";

            pstUpdate = con.prepareStatement(sqlUpdate);
            pstUpdate.setInt(1, idVariante);

            int affectedRows = pstUpdate.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo decrementar el stock");
            }

            con.commit();

            System.out.println("SUCCESS  Stock decrementado: Variante=" + idVariante +
                    ", Stock anterior=" + stockDisponible +
                    ", Stock actual=" + (stockDisponible - 1));

        } catch (SQLException | IllegalStateException e) {
            if (con != null) {
                try {
                    con.rollback();
                    System.err.println("WARNING  Rollback ejecutado");
                } catch (SQLException rollbackEx) {
                    System.err.println("ERROR  Error en rollback: " + rollbackEx.getMessage());
                }
            }
            throw e;

        } finally {
            try {
                if (pstCheck != null)
                    pstCheck.close();
                if (pstUpdate != null)
                    pstUpdate.close();
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException closeEx) {
                System.err.println("WARNING  Error cerrando conexión: " + closeEx.getMessage());
            }
        }
    }

    /**
     * Incrementa el stock (SUMA al existente).
     * 
     * @param idBodega        ID de la bodega
     * @param idVariante      ID de la variante
     * @param incrementoPares Pares a incrementar
     * @param incrementoCajas Cajas a incrementar
     * @throws SQLException Error de base de datos
     */
    public void incrementStock(int idBodega, int idVariante, int incrementoPares, int incrementoCajas)
            throws SQLException {

        if (idBodega <= 0 || idVariante <= 0) {
            throw new IllegalArgumentException("IDs inválidos");
        }

        if (incrementoPares < 0 || incrementoCajas < 0) {
            throw new IllegalArgumentException("Los incrementos no pueden ser negativos");
        }

        String sql = "UPDATE inventario_bodega SET " +
                "Stock_par = Stock_par + ?, " +
                "Stock_caja = Stock_caja + ?, " +
                "fecha_ultimo_movimiento = CURRENT_TIMESTAMP " +
                "WHERE id_bodega = ? " +
                "AND id_variante = ? " +
                "AND activo = 1"; // SUCCESS Solo actualizar si está activo

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, incrementoPares);
            pst.setInt(2, incrementoCajas);
            pst.setInt(3, idBodega);
            pst.setInt(4, idVariante);

            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "No se encontró inventario activo para incrementar (bodega=" + idBodega +
                                ", variante=" + idVariante + ")");
            }

            System.out.println("SUCCESS  Stock incrementado: Bodega=" + idBodega +
                    ", Variante=" + idVariante +
                    ", +Pares=" + incrementoPares +
                    ", +Cajas=" + incrementoCajas);
        }
    }

    public java.util.List<raven.controlador.inventario.InventarioBodega> listarInventarioPorBodegaYTipo(
            int idBodega,
            String tipoUnidad) throws SQLException {
        if (idBodega <= 0) {
            throw new IllegalArgumentException("ID de bodega inválido");
        }
        if (tipoUnidad == null || tipoUnidad.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de unidad requerido");
        }

        String colStock = ("Pares".equalsIgnoreCase(tipoUnidad)) ? "COALESCE(i.Stock_par,0)"
                : "COALESCE(i.Stock_caja,0)";

        String sql = "SELECT " +
                "   p.id_producto, p.codigo_modelo, p.nombre, p.genero, p.id_marca, " +
                "   v.id_variante, v.sku, v.id_talla, v.id_color, " +
                "   t.numero AS talla_nombre, c.nombre AS color_nombre, " +
                "   " + colStock + " AS stock, " +
                "   i.ubicacion_especifica " +
                "FROM inventario_bodega i " +
                "JOIN producto_variantes v ON v.id_variante = i.id_variante AND v.disponible = 1 " +
                "JOIN productos p          ON p.id_producto  = v.id_producto AND p.activo = 1 " +
                "LEFT JOIN tallas t        ON v.id_talla = t.id_talla " +
                "LEFT JOIN colores c       ON v.id_color = c.id_color " +
                "WHERE i.id_bodega = ? AND i.activo = 1 AND " + colStock + " > 0 " +
                "ORDER BY p.codigo_modelo, v.id_variante";

        java.util.List<raven.controlador.inventario.InventarioBodega> lista = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    raven.controlador.inventario.InventarioBodega m = new raven.controlador.inventario.InventarioBodega();
                    m.setIdProducto(rs.getInt("id_producto"));
                    m.setCodigoModelo(rs.getString("codigo_modelo"));
                    m.setNombreProducto(rs.getString("nombre"));
                    m.setIdVariante(rs.getInt("id_variante"));
                    m.setSku(rs.getString("sku"));
                    m.setTalla(rs.getString("talla_nombre"));
                    m.setColor(rs.getString("color_nombre"));
                    if ("Pares".equalsIgnoreCase(tipoUnidad)) {
                        m.setStockPar(rs.getInt("stock"));
                    } else {
                        m.setStockCaja(rs.getInt("stock"));
                    }
                    m.setUbicacionEspecifica(rs.getString("ubicacion_especifica"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    public java.util.List<raven.controlador.productos.ModelProduct> listarProductosPorBodegaYTipo(
            int idBodega,
            String tipoUnidad) throws SQLException {
        if (idBodega <= 0) {
            throw new IllegalArgumentException("ID de bodega inválido");
        }
        if (tipoUnidad == null || tipoUnidad.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de unidad requerido");
        }

        String colStock = ("Pares".equalsIgnoreCase(tipoUnidad)) ? "i.Stock_par" : "i.Stock_caja";

        String sql = "SELECT " +
                "   p.id_producto, p.codigo_modelo, p.nombre, p.genero, p.id_marca, " +
                "   SUM(" + colStock + ") AS stock_total " +
                "FROM inventario_bodega i " +
                "INNER JOIN producto_variantes v ON v.id_variante = i.id_variante AND v.disponible = 1 " +
                "INNER JOIN productos p          ON p.id_producto  = v.id_producto AND p.activo = 1 " +
                "WHERE i.id_bodega = ? AND i.activo = 1 " +
                "GROUP BY p.id_producto, p.codigo_modelo, p.nombre, p.genero, p.id_marca " +
                "HAVING SUM(" + colStock + ") > 0 " +
                "ORDER BY p.codigo_modelo";

        java.util.List<raven.controlador.productos.ModelProduct> lista = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    raven.controlador.productos.ModelProduct p = new raven.controlador.productos.ModelProduct();
                    p.setProductId(rs.getInt("id_producto"));
                    p.setModelCode(rs.getString("codigo_modelo"));
                    p.setName(rs.getString("nombre"));
                    p.setGender(rs.getString("genero"));
                    int total = rs.getInt("stock_total");
                    if ("Pares".equalsIgnoreCase(tipoUnidad)) {
                        p.setPairsStock(total);
                    } else {
                        p.setBoxesStock(total);
                    }
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    public raven.clases.comun.GenericPaginationService.PagedResult<raven.controlador.productos.ModelProduct> listarProductosPorBodegaYTipoPaginado(
            int idBodega,
            String tipoUnidad,
            String searchTerm,
            int page,
            int pageSize) throws SQLException {
        if (idBodega <= 0) {
            throw new IllegalArgumentException("ID de bodega inválido");
        }
        if (tipoUnidad == null || tipoUnidad.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de unidad requerido");
        }

        String colStock = ("Pares".equalsIgnoreCase(tipoUnidad)) ? "i.Stock_par" : "i.Stock_caja";

        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(idBodega);
        StringBuilder where = new StringBuilder(
                "WHERE i.id_bodega = ? AND i.activo = 1 AND v.disponible = 1 AND p.activo = 1");
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            where.append(
                    " AND (LOWER(p.nombre) LIKE ? OR LOWER(p.codigo_modelo) LIKE ? OR LOWER(i.ubicacion_especifica) LIKE ?)");
            String pat = "%" + searchTerm.toLowerCase() + "%";
            params.add(pat);
            params.add(pat);
            params.add(pat);
        }

        String select = "p.id_producto, p.codigo_modelo, p.nombre, p.genero, p.id_marca, " +
                "m.nombre AS marca_nombre, " +
                "GROUP_CONCAT(DISTINCT i.ubicacion_especifica ORDER BY i.ubicacion_especifica SEPARATOR ', ') AS ubicacion, "
                +
                "SUM(" + colStock + ") AS stock_total";
        String from = "inventario_bodega i INNER JOIN producto_variantes v ON v.id_variante = i.id_variante AND v.disponible = 1 INNER JOIN productos p ON p.id_producto = v.id_producto AND p.activo = 1 LEFT JOIN marcas m ON p.id_marca = m.id_marca";
        String groupBy = " GROUP BY p.id_producto, p.codigo_modelo, p.nombre, p.genero, p.id_marca, m.nombre";
        String having = " HAVING SUM(" + colStock + ") > 0";
        String orderBy = " ORDER BY p.codigo_modelo";

        raven.clases.comun.GenericPaginationService.QueryConfig cfg = raven.clases.comun.GenericPaginationService.QueryConfig
                .builder(
                        "SELECT " + select + " FROM " + from + " " + where + groupBy + having,
                        "SELECT COUNT(*) FROM (SELECT p.id_producto FROM " + from + " " + where + groupBy + having
                                + ") sub",
                        (rs, idx) -> {
                            try {
                                raven.controlador.productos.ModelProduct p = new raven.controlador.productos.ModelProduct();
                                p.setProductId(rs.getInt("id_producto"));
                                p.setModelCode(rs.getString("codigo_modelo"));
                                p.setName(rs.getString("nombre"));
                                p.setGender(rs.getString("genero"));
                                String marcaNombre = rs.getString("marca_nombre");
                                if (marcaNombre != null) {
                                    raven.controlador.productos.ModelBrand b = new raven.controlador.productos.ModelBrand();
                                    b.setName(marcaNombre);
                                    p.setBrand(b);
                                }
                                try {
                                    String ubic = rs.getString("ubicacion");
                                    if (ubic != null) {
                                        p.setUbicacion(ubic);
                                    }
                                } catch (java.sql.SQLException ignore) {
                                }
                                int total = rs.getInt("stock_total");
                                if ("Pares".equalsIgnoreCase(tipoUnidad)) {
                                    p.setPairsStock(total);
                                } else {
                                    p.setBoxesStock(total);
                                }
                                return p;
                            } catch (java.sql.SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .orderBy(orderBy).withParams(params.toArray()).build();

        raven.clases.comun.GenericPaginationService cacheService = new raven.clases.comun.GenericPaginationService(
                new raven.clases.comun.GenericPaginationService.CacheStrategy() {
                    @Override
                    public String generateKey(String entityType, java.util.Map<String, Object> filters, int pg,
                            int ps) {
                        // Generar clave única para caché
                        StringBuilder sb = new StringBuilder();
                        sb.append(entityType).append("_");
                        if (filters != null) {
                            sb.append(filters.hashCode());
                        }
                        sb.append("_").append(pg).append("_").append(ps);
                        return sb.toString();
                    }

                    @Override
                    public Object get(String key) {
                        return raven.util.ClientCacheManager.getInstance().get(key);
                    }

                    @Override
                    public void put(String key, Object data, long ttlMs) {
                        // Usar caché del cliente (30 segundos por defecto si no se especifica)
                        raven.util.ClientCacheManager.getInstance().put(key, data, ttlMs > 0 ? ttlMs : 30000);
                    }

                    @Override
                    public void invalidate(String key) {
                        raven.util.ClientCacheManager.getInstance().clear(key);
                    }

                    @Override
                    public void invalidatePattern(String pattern) {
                        raven.util.ClientCacheManager.getInstance().invalidatePattern(pattern);
                    }
                });
        return cacheService.executePagedQuery(cfg, page, pageSize);
    }

    /**
     * Desactiva un registro de inventario (soft delete).
     * 
     * @param idBodega   ID de la bodega
     * @param idVariante ID de la variante
     * @throws SQLException Error de base de datos
     */
    public void desactivar(int idBodega, int idVariante) throws SQLException {
        if (idBodega <= 0 || idVariante <= 0) {
            throw new IllegalArgumentException("IDs inválidos");
        }

        String sql = "UPDATE inventario_bodega SET " +
                "activo = 0, " +
                "fecha_ultimo_movimiento = CURRENT_TIMESTAMP " +
                "WHERE id_bodega = ? " +
                "AND id_variante = ?";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idBodega);
            pst.setInt(2, idVariante);

            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "No se encontró inventario para desactivar (bodega=" + idBodega +
                                ", variante=" + idVariante + ")");
            }

            System.out.println("SUCCESS  Inventario desactivado: Bodega=" + idBodega +
                    ", Variante=" + idVariante);
        }
    }

    /**
     * Reactiva un registro de inventario.
     * 
     * @param idBodega   ID de la bodega
     * @param idVariante ID de la variante
     * @throws SQLException Error de base de datos
     */
    public void reactivar(int idBodega, int idVariante) throws SQLException {
        if (idBodega <= 0 || idVariante <= 0) {
            throw new IllegalArgumentException("IDs inválidos");
        }

        String sql = "UPDATE inventario_bodega SET " +
                "activo = 1, " +
                "fecha_ultimo_movimiento = CURRENT_TIMESTAMP " +
                "WHERE id_bodega = ? " +
                "AND id_variante = ?";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idBodega);
            pst.setInt(2, idVariante);

            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "No se encontró inventario para reactivar (bodega=" + idBodega +
                                ", variante=" + idVariante + ")");
            }

            System.out.println("SUCCESS  Inventario reactivado: Bodega=" + idBodega +
                    ", Variante=" + idVariante);
        }
    }

    /**
     * Busca inventario detallado con filtros avanzados.
     * 
     * @param idBodega    ID de la bodega (opcional, 0 o null para todas)
     * @param busqueda    Texto a buscar (nombre, codigo, ean)
     * @param idMarca     ID de la marca (opcional, 0 o null para todas)
     * @param idCategoria ID de la categoria (opcional, 0 o null para todas)
     * @param color       Nombre del color (opcional, null o vacio para todos)
     * @param talla       Nombre de la talla (opcional, null o vacio para todas)
     * @return Lista de InventarioDetalleItem
     * @throws SQLException Error de base de datos
     */
    public java.util.List<InventarioDetalleItem> buscarInventarioDetallado(
            Integer idBodega,
            String busqueda,
            Integer idMarca,
            Integer idCategoria,
            String color,
            String talla,
            String ubicacion,
            boolean sinUbicacion) throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("b.id_bodega, b.nombre AS nombre_bodega, ");
        sql.append("p.id_producto, p.codigo_modelo, p.nombre AS nombre_producto, ");
        sql.append("m.nombre AS nombre_marca, ");
        sql.append("cat.nombre AS nombre_categoria, ");
        sql.append("c.nombre AS nombre_color, ");
        sql.append("CONCAT(t.numero, ' ', COALESCE(t.sistema,'')) AS nombre_talla, ");
        // Agregación para unificar duplicados y mostrar stock real
        sql.append("SUM(i.Stock_par) AS Stock_par, SUM(i.Stock_caja) AS Stock_caja, ");
        sql.append("GROUP_CONCAT(DISTINCT i.ubicacion_especifica SEPARATOR ', ') AS ubicacion_especifica, ");
        sql.append("v.id_variante, v.ean ");
        sql.append("FROM inventario_bodega i ");
        sql.append("INNER JOIN bodegas b ON i.id_bodega = b.id_bodega ");
        sql.append("INNER JOIN producto_variantes v ON i.id_variante = v.id_variante ");
        sql.append("INNER JOIN productos p ON v.id_producto = p.id_producto ");
        sql.append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("LEFT JOIN categorias cat ON p.id_categoria = cat.id_categoria ");
        sql.append("LEFT JOIN colores c ON v.id_color = c.id_color ");
        sql.append("LEFT JOIN tallas t ON v.id_talla = t.id_talla ");
        sql.append("WHERE i.activo = 1 AND v.disponible = 1 AND p.activo = 1 ");

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (idBodega != null && idBodega > 0) {
            sql.append("AND i.id_bodega = ? ");
            params.add(idBodega);
        }

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append("AND (p.nombre LIKE ? OR p.codigo_modelo LIKE ? OR v.ean LIKE ? OR v.sku LIKE ?) ");
            String pat = "%" + busqueda.trim() + "%";
            params.add(pat);
            params.add(pat);
            params.add(pat);
            params.add(pat);
        }

        if (idMarca != null && idMarca > 0) {
            sql.append("AND p.id_marca = ? ");
            params.add(idMarca);
        }

        if (idCategoria != null && idCategoria > 0) {
            sql.append("AND p.id_categoria = ? ");
            params.add(idCategoria);
        }

        if (color != null && !color.trim().isEmpty()) {
            sql.append("AND c.nombre LIKE ? ");
            params.add("%" + color.trim() + "%");
        }

        if (talla != null && !talla.trim().isEmpty()) {
            sql.append("AND t.numero LIKE ? ");
            params.add("%" + talla.trim() + "%");
        }

        if (sinUbicacion) {
            sql.append("AND (i.ubicacion_especifica IS NULL OR i.ubicacion_especifica = '') ");
        } else if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            sql.append("AND i.ubicacion_especifica LIKE ? ");
            params.add("%" + ubicacion.trim() + "%");
        }

        // Agrupamiento para evitar duplicados
        sql.append("GROUP BY b.id_bodega, b.nombre, p.id_producto, p.codigo_modelo, p.nombre, ");
        sql.append("m.nombre, cat.nombre, c.nombre, t.numero, t.sistema, v.id_variante, v.ean ");

        // Ordenamiento
        sql.append("ORDER BY b.nombre, p.nombre, c.nombre, t.numero");

        java.util.List<InventarioDetalleItem> lista = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pst.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    InventarioDetalleItem item = new InventarioDetalleItem();
                    item.setIdBodega(rs.getInt("id_bodega"));
                    item.setNombreBodega(rs.getString("nombre_bodega"));
                    item.setIdProducto(rs.getInt("id_producto"));
                    item.setCodigoModelo(rs.getString("codigo_modelo"));
                    item.setNombreProducto(rs.getString("nombre_producto"));
                    item.setNombreMarca(rs.getString("nombre_marca"));
                    item.setCategoria(rs.getString("nombre_categoria"));
                    item.setNombreColor(rs.getString("nombre_color"));
                    item.setNombreTalla(rs.getString("nombre_talla"));
                    item.setStockPares(rs.getInt("Stock_par"));
                    item.setStockCajas(rs.getInt("Stock_caja"));
                    item.setUbicacion(rs.getString("ubicacion_especifica"));
                    item.setIdVariante(rs.getInt("id_variante")); // Set ID
                    item.setEan(rs.getString("ean")); // Set EAN
                    lista.add(item);
                }
            }
        }
        return lista;
    }

    public java.util.List<String> obtenerUbicacionesPorBodega(int idBodega) throws SQLException {
        String sql = "SELECT DISTINCT ubicacion_especifica FROM inventario_bodega " +
                "WHERE id_bodega = ? AND ubicacion_especifica IS NOT NULL " +
                "AND ubicacion_especifica != '' AND activo = 1 " +
                "ORDER BY ubicacion_especifica";

        java.util.List<String> list = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idBodega);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("ubicacion_especifica"));
                }
            }
        }
        return list;
    }

    public java.util.List<raven.application.form.productos.ComboItem> obtenerBodegasCombo() throws SQLException {
        // CORREGIDO: Usar 'activa' en lugar de 'estado'
        String sql = "SELECT id_bodega, nombre FROM bodegas WHERE activa = 1 ORDER BY nombre";
        java.util.List<raven.application.form.productos.ComboItem> list = new java.util.ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql);
                ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(
                        new raven.application.form.productos.ComboItem(rs.getInt("id_bodega"), rs.getString("nombre")));
            }
        }
        return list;
    }
}
