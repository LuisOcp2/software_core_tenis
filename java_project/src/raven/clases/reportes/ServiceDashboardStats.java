package raven.clases.reportes;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.chart.data.pie.DefaultPieDataset;
import raven.chart.data.category.DefaultCategoryDataset;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.conexion;

/**
 * Servicio para obtener estadísticas del dashboard del sistema.
 * 
 * @author Sistema Xtreme
 * @version 2.0
 */
public class ServiceDashboardStats {

    private static final Logger LOGGER = Logger.getLogger(ServiceDashboardStats.class.getName());
    private static final long CACHE_TTL_MS = 60_000L;
    private static final int DIAS_CACHE_CATEGORIAS = 30;

    private static class CategoriaStats implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        double ventas;
        double costos;

        double getGanancia() {
            return ventas - costos;
        }
    }

    private static class CatCacheRecord implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, CategoriaStats> mapa;
        LocalDate desde;
        String bodegaKey;
        long ts;
    }

    private Map<String, CategoriaStats> cacheCategoriasMapa = null;
    private LocalDate cacheDesde = null;
    private String cacheBodegaKey = null;
    private long cacheTimestampMs = 0L;

    // ==================================================================================
    // MÉTODOS DE UTILIDAD Y CACHE
    // ==================================================================================

    private Integer obtenerIdBodega(ModelUser usuario) {
        if (usuario == null)
            return null;
        return usuario.getIdBodega();
    }

    private static java.io.File getCatCacheFile() {
        String home = System.getProperty("user.home", ".");
        java.io.File dir = new java.io.File(home, ".xtreme_cache");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new java.io.File(dir, "dashboard_cat_cache.ser");
    }

    private static void saveCatCache(CatCacheRecord rec) {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream(getCatCacheFile()))) {
            oos.writeObject(rec);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo guardar cache de categorías", e);
        }
    }

    private static CatCacheRecord loadCatCache() {
        java.io.File f = getCatCacheFile();
        if (!f.exists())
            return null;
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                new java.io.FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof CatCacheRecord)
                return (CatCacheRecord) obj;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo cargar cache de categorías", e);
        }
        return null;
    }

    // ==================================================================================
    // MÉTODOS DE CARGA DE DATOS DE CATEGORÍAS
    // ==================================================================================

    private Map<String, CategoriaStats> cargarStatsCategorias(LocalDate start, LocalDate end,
            ModelUser usuario) throws SQLException {
        Map<String, CategoriaStats> mapa = new HashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.nombre AS categoria, ")
                .append("COALESCE(SUM(vd.subtotal), 0) AS total_ventas, ")
                .append("COALESCE(SUM(vd.cantidad * COALESCE(")
                .append("(SELECT pv2.precio_compra FROM producto_variantes pv2 ")
                .append("WHERE pv2.id_variante = vd.id_variante LIMIT 1), ")
                .append("p.precio_compra)), 0) AS total_costos ")
                .append("FROM ventas v ")
                .append("JOIN venta_detalles vd ON v.id_venta = vd.id_venta ")
                .append("JOIN productos p ON vd.id_producto = p.id_producto ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante ")
                .append("JOIN categorias c ON p.id_categoria = c.id_categoria ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND vd.activo = 1 ")
                .append("AND v.fecha_venta BETWEEN ? AND ?");

        Integer idBodega = obtenerIdBodega(usuario);
        if (idBodega != null) {
            sql.append(" AND u.id_bodega = ?");
        }
        sql.append(" GROUP BY c.id_categoria, c.nombre");

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setTimestamp(idx++, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(idx++, java.sql.Timestamp.valueOf(end.atTime(23, 59, 59)));
            if (idBodega != null) {
                ps.setInt(idx++, idBodega);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String categoria = rs.getString("categoria");
                    CategoriaStats stats = new CategoriaStats();
                    stats.ventas = rs.getDouble("total_ventas");
                    stats.costos = rs.getDouble("total_costos");
                    mapa.put(categoria, stats);
                }
            }
        }
        return mapa;
    }

    private Map<String, CategoriaStats> cargarStatsCategoriasUltimos30(ModelUser usuario)
            throws SQLException {
        return cargarStatsCategorias(
                LocalDate.now().minusDays(DIAS_CACHE_CATEGORIAS), LocalDate.now(), usuario);
    }

    private Map<String, CategoriaStats> getStatsCategoriasUltimos30Cached(ModelUser usuario)
            throws SQLException {
        Integer idBodega = obtenerIdBodega(usuario);
        String bodegaKey = (idBodega != null) ? String.valueOf(idBodega) : "__ALL__";

        boolean expired = (System.currentTimeMillis() - cacheTimestampMs) > CACHE_TTL_MS;
        boolean sinceChanged = cacheDesde == null ||
                !cacheDesde.equals(LocalDate.now().minusDays(DIAS_CACHE_CATEGORIAS));
        boolean ubicChanged = cacheBodegaKey == null || !cacheBodegaKey.equals(bodegaKey);

        if (cacheCategoriasMapa == null || expired || sinceChanged || ubicChanged) {
            CatCacheRecord disk = loadCatCache();
            boolean diskValid = false;
            if (disk != null) {
                boolean diskExpired = (System.currentTimeMillis() - disk.ts) > (CACHE_TTL_MS * 5);
                boolean diskSinceOk = disk.desde != null &&
                        disk.desde.equals(LocalDate.now().minusDays(DIAS_CACHE_CATEGORIAS));
                boolean diskUbicOk = (disk.bodegaKey == null && "__ALL__".equals(bodegaKey)) ||
                        (disk.bodegaKey != null && disk.bodegaKey.equals(bodegaKey));
                diskValid = !diskExpired && diskSinceOk && diskUbicOk && disk.mapa != null;
            }
            if (diskValid) {
                cacheCategoriasMapa = disk.mapa;
                cacheDesde = disk.desde;
                cacheBodegaKey = disk.bodegaKey;
                cacheTimestampMs = disk.ts;
            } else {
                cacheCategoriasMapa = cargarStatsCategoriasUltimos30(usuario);
                cacheDesde = LocalDate.now().minusDays(DIAS_CACHE_CATEGORIAS);
                cacheBodegaKey = bodegaKey;
                cacheTimestampMs = System.currentTimeMillis();
                CatCacheRecord rec = new CatCacheRecord();
                rec.mapa = cacheCategoriasMapa;
                rec.desde = cacheDesde;
                rec.bodegaKey = cacheBodegaKey;
                rec.ts = cacheTimestampMs;
                saveCatCache(rec);
            }
        }
        return cacheCategoriasMapa;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - CONSULTAS GENERALES
    // ==================================================================================

    public LocalDate getFechaServidor() {
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement("SELECT CURRENT_DATE")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date date = rs.getDate(1);
                    if (date != null)
                        return date.toLocalDate();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener fecha del servidor, usando fecha local", e);
        }
        return LocalDate.now();
    }

    public int getConteoUsuarios(ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) AS total FROM usuarios WHERE activo = 1");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append(" AND id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(1, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conteo de usuarios", e);
        }
        return 0;
    }

    public int getConteoProductos(ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(DISTINCT p.id_producto) AS total ")
                .append("FROM productos p ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("JOIN inventario i ON p.id_producto = i.id_producto ")
                    .append("WHERE p.activo = 1 AND i.id_bodega = ?");
        } else {
            sqlBuilder.append("WHERE p.activo = 1");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(1, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conteo de productos", e);
        }
        return 0;
    }

    public int getConteoClientes() {
        String sql = "SELECT COUNT(*) AS total FROM clientes WHERE activo = 1";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return rs.getInt("total");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conteo de clientes", e);
        }
        return 0;
    }

    public int getConteoVentasPendientes(ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) AS total FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado = 'pendiente' ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(1, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conteo de ventas pendientes", e);
        }
        return 0;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - VENTAS
    // ==================================================================================

    public double getVentasTotalesPeriodo(int dias, ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COALESCE(SUM(v.total), 0) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas totales del período", e);
        }
        return 0.0;
    }

    public double getVentasHoy(ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COALESCE(SUM(v.total), 0) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND DATE(v.fecha_venta) = CURRENT_DATE ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(1, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas de hoy", e);
        }
        return 0.0;
    }

    public double getVentasSemana(ModelUser usuario) {
        return getVentasTotalesPeriodo(7, usuario);
    }

    public double getVentasMes(ModelUser usuario) {
        return getVentasTotalesPeriodo(30, usuario);
    }

    public double getVentasTotales(ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COALESCE(SUM(v.total), 0) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(1, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas totales", e);
        }
        return 0.0;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - DATASETS PARA GRÁFICOS
    // ==================================================================================

    public DefaultPieDataset<String> getVentasMensuales(LocalDate start, LocalDate end,
            ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        Map<String, Double> ventasPorMes = obtenerVentasMensuales(start, end, usuario);
        for (Map.Entry<String, Double> entry : ventasPorMes.entrySet()) {
            dataset.addValue(entry.getKey(), entry.getValue());
        }
        if (dataset.getItemCount() == 0) {
            dataset.addValue("Sin ventas registradas", 0.0);
        }
        return dataset;
    }

    public DefaultPieDataset<String> getVentasMensuales(int meses) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(meses);
        return getVentasMensuales(start, end, null);
    }

    public DefaultPieDataset<String> getGastosMensuales(LocalDate start, LocalDate end,
            ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        Map<String, Double> gastosPorMes = obtenerGastosMensuales(start, end, usuario);
        for (Map.Entry<String, Double> entry : gastosPorMes.entrySet()) {
            dataset.addValue(entry.getKey(), entry.getValue());
        }
        if (dataset.getItemCount() == 0) {
            dataset.addValue("Sin gastos registrados", 0.0);
        }
        return dataset;
    }

    public DefaultPieDataset<String> getGastosMensuales(int meses, ModelUser usuario) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(meses);
        return getGastosMensuales(start, end, usuario);
    }

    // ==================================================================================
    // MÉTODOS PRIVADOS - OBTENCIÓN DE DATOS PARA DATASETS
    // ==================================================================================

    private Map<String, Double> obtenerVentasMensuales(LocalDate start, LocalDate end,
            ModelUser usuario) {
        Map<String, Double> ventasPorMes = new LinkedHashMap<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT DATE_FORMAT(v.fecha_venta, '%Y-%m') AS mes, ")
                .append("SUM(v.total) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta BETWEEN ? AND ? ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ? ");
        }
        sqlBuilder.append("GROUP BY DATE_FORMAT(v.fecha_venta, '%Y-%m') ")
                .append("ORDER BY mes ASC");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf(end.atTime(23, 59, 59)));
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                while (rs.next()) {
                    String mesAnio = rs.getString("mes");
                    double total = rs.getDouble("total");
                    YearMonth ym = YearMonth.parse(mesAnio);
                    String mesFormateado = ym.atDay(1).format(formatter);
                    ventasPorMes.put(mesFormateado, total);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas mensuales", e);
        }
        return ventasPorMes;
    }

    private Map<String, Double> obtenerGastosMensuales(LocalDate start, LocalDate end,
            ModelUser usuario) {
        Map<String, Double> gastosPorMes = new LinkedHashMap<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT DATE_FORMAT(g.fecha_registro, '%Y-%m') AS mes, ")
                .append("SUM(g.monto) AS total ")
                .append("FROM gastos g ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("WHERE g.id_bodega = ? ")
                    .append("AND g.fecha_registro BETWEEN ? AND ? ");
        } else {
            sqlBuilder.append("WHERE g.fecha_registro BETWEEN ? AND ? ");
        }
        sqlBuilder.append("GROUP BY DATE_FORMAT(g.fecha_registro, '%Y-%m') ")
                .append("ORDER BY mes ASC");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex++, usuario.getIdBodega());
            }
            ps.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf(end.atTime(23, 59, 59)));
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                while (rs.next()) {
                    String mesAnio = rs.getString("mes");
                    double total = rs.getDouble("total");
                    YearMonth ym = YearMonth.parse(mesAnio);
                    String mesFormateado = ym.atDay(1).format(formatter);
                    gastosPorMes.put(mesFormateado, total);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener gastos mensuales", e);
        }
        return gastosPorMes;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - PRODUCTOS Y CATEGORÍAS
    // ==================================================================================

    public DefaultPieDataset<String> getProductosMasVendidos(int limite, int dias,
            ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT p.nombre, COALESCE(SUM(vd.subtotal), 0) AS total_vendido ")
                .append("FROM venta_detalles vd ")
                .append("JOIN productos p ON vd.id_producto = p.id_producto ")
                .append("JOIN ventas v ON vd.id_venta = v.id_venta ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND vd.activo = 1 ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ? ");
        }
        sqlBuilder.append("GROUP BY p.id_producto, p.nombre ")
                .append("ORDER BY total_vendido DESC ")
                .append("LIMIT ?");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex++, usuario.getIdBodega());
            }
            ps.setInt(paramIndex, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    double total = rs.getDouble("total_vendido");
                    dataset.addValue(nombre, total);
                }
            }
            if (dataset.getItemCount() == 0) {
                dataset.addValue("Sin productos vendidos", 0.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener productos más vendidos", e);
            dataset.addValue("Error al cargar datos", 0.0);
        }
        return dataset;
    }

    public DefaultPieDataset<String> getVentasPorCategoria(ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        try {
            Map<String, CategoriaStats> stats = getStatsCategoriasUltimos30Cached(usuario);
            for (Map.Entry<String, CategoriaStats> entry : stats.entrySet()) {
                double ventas = entry.getValue().ventas;
                if (ventas > 0) {
                    dataset.addValue(entry.getKey(), ventas);
                }
            }
            if (dataset.getItemCount() == 0) {
                dataset.addValue("Sin ventas por categoría", 0.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas por categoría", e);
            dataset.addValue("Error al cargar datos", 0.0);
        }
        return dataset;
    }

    public DefaultPieDataset<String> getGananciasPorCategoria(ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        try {
            Map<String, CategoriaStats> stats = getStatsCategoriasUltimos30Cached(usuario);
            for (Map.Entry<String, CategoriaStats> entry : stats.entrySet()) {
                double ganancia = entry.getValue().getGanancia();
                if (ganancia > 0) {
                    dataset.addValue(entry.getKey(), ganancia);
                }
            }
            if (dataset.getItemCount() == 0) {
                dataset.addValue("Sin ganancias por categoría", 0.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ganancias por categoría", e);
            dataset.addValue("Error al cargar datos", 0.0);
        }
        return dataset;
    }

    public DefaultCategoryDataset<String, String> getTendenciaProducto(int idProducto) {
        DefaultCategoryDataset<String, String> dataset = new DefaultCategoryDataset<>();
        String sql = "SELECT DATE_FORMAT(v.fecha_venta, '%Y-%m') AS mes, " +
                "SUM(vd.cantidad) AS cantidad " +
                "FROM venta_detalles vd " +
                "JOIN ventas v ON vd.id_venta = v.id_venta " +
                "WHERE vd.id_producto = ? " +
                "AND v.estado IN ('completada', 'pendiente') " +
                "AND vd.activo = 1 " +
                "AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH) " +
                "GROUP BY DATE_FORMAT(v.fecha_venta, '%Y-%m') " +
                "ORDER BY mes ASC";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
                while (rs.next()) {
                    String mesAnio = rs.getString("mes");
                    double cantidad = rs.getDouble("cantidad");
                    YearMonth ym = YearMonth.parse(mesAnio);
                    String mesFormateado = ym.atDay(1).format(formatter);
                    dataset.addValue(cantidad, "Ventas", mesFormateado);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener tendencia de producto", e);
        }
        return dataset;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - GASTOS
    // ==================================================================================

    public double getTotalGastosOperativos(int dias, ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COALESCE(SUM(g.monto), 0) AS total ")
                .append("FROM gastos_operativos g ")
                .append("WHERE g.estado = 'registrado' ")
                .append("AND g.fecha_gasto >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND g.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener total de gastos", e);
        }
        return 0.0;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - MÉTODOS DE PAGO Y BALANCE
    // ==================================================================================

    public DefaultPieDataset<String> getVentasPorMetodoPago(int dias, ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT v.tipo_pago, SUM(v.total) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ? ");
        }
        sqlBuilder.append("GROUP BY v.tipo_pago ")
                .append("ORDER BY total DESC");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipoPago = formatearTipoPago(rs.getString("tipo_pago"));
                    double total = rs.getDouble("total");
                    dataset.addValue(tipoPago, total);
                }
            }
            if (dataset.getItemCount() == 0) {
                dataset.addValue("Sin ventas registradas", 0.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas por método de pago", e);
            dataset.addValue("Error al cargar datos", 0.0);
        }
        return dataset;
    }

    private String formatearTipoPago(String tipoPago) {
        if (tipoPago == null)
            return "Desconocido";
        switch (tipoPago.toLowerCase()) {
            case "efectivo":
                return "Efectivo";
            case "tarjeta":
                return "Tarjeta";
            case "transferencia":
                return "Transferencia";
            case "sistecredito":
                return "Sistecredito";
            case "nota_credito":
                return "Nota Crédito";
            case "mixto":
                return "Mixto";
            default:
                return tipoPago;
        }
    }

    public Map<String, Double> getBalanceIngresosEgresos(int dias, ModelUser usuario) {
        Map<String, Double> balance = new HashMap<>();
        double ingresos = getVentasTotalesPeriodo(dias, usuario);
        balance.put("ingresos", ingresos);
        double gastos = getTotalGastosOperativos(dias, usuario);
        balance.put("gastos", gastos);
        double compras = getComprasExternas(dias, usuario);
        balance.put("compras", compras);
        double balanceNeto = ingresos - gastos - compras;
        balance.put("balance_neto", balanceNeto);
        return balance;
    }

    public double getComprasExternas(int dias, ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COALESCE(SUM(ce.total), 0) AS total ")
                .append("FROM compras_externas ce ")
                .append("WHERE ce.estado IN ('recibida', 'facturada') ")
                .append("AND ce.fecha_compra >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND ce.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener compras externas", e);
        }
        return 0.0;
    }

    // ==================================================================================
    // MÉTODOS PÚBLICOS - TENDENCIAS
    // ==================================================================================

    public DefaultCategoryDataset<String, String> getVentasTendenciaSemanal(int semanas,
            ModelUser usuario) {
        DefaultCategoryDataset<String, String> dataset = new DefaultCategoryDataset<>();
        int dias = semanas * 7;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT DATE(v.fecha_venta) AS fecha, SUM(v.total) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ? ");
        }
        sqlBuilder.append("GROUP BY DATE(v.fecha_venta) ")
                .append("ORDER BY fecha ASC");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
                while (rs.next()) {
                    java.sql.Date fecha = rs.getDate("fecha");
                    double total = rs.getDouble("total");
                    if (fecha != null) {
                        String fechaFormateada = fecha.toLocalDate().format(fmt);
                        dataset.addValue(total, "Ventas", fechaFormateada);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener tendencia de ventas", e);
        }
        return dataset;
    }

    public String getProximoConteo(ModelUser usuario) {
        return "Próximamente";
    }

    // ==================================================================================
    // MÉTODOS DE COMPATIBILIDAD - PARA FORMDASHBOARD
    // ==================================================================================

    public double getGananciaTotalPeriodo(int dias, ModelUser usuario) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(dias);
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("COALESCE(SUM(vd.subtotal), 0) AS ventas, ")
                .append("COALESCE(SUM(vd.cantidad * ")
                .append("COALESCE((SELECT pv2.precio_compra FROM producto_variantes pv2 ")
                .append("WHERE pv2.id_variante = vd.id_variante LIMIT 1), p.precio_compra)), 0) AS costos ")
                .append("FROM ventas v ")
                .append("JOIN venta_detalles vd ON v.id_venta = vd.id_venta ")
                .append("JOIN productos p ON vd.id_producto = p.id_producto ")
                .append("LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND vd.activo = 1 ")
                .append("AND v.fecha_venta >= ? AND v.fecha_venta <= ?");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append(" AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf(end.atTime(23, 59, 59)));
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double ventas = rs.getDouble("ventas");
                    double costos = rs.getDouble("costos");
                    return ventas - costos;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ganancia total del período", e);
        }
        return 0.0;
    }

    public DefaultPieDataset<String> getProductosMasVendidos(int limite, ModelUser usuario) {
        return getProductosMasVendidos(limite, 30, usuario);
    }

    public DefaultPieDataset<String> getProductosMenosVendidos(int limite, int dias,
            ModelUser usuario) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT p.nombre, m.nombre AS marca, p.talla, ")
                .append("COALESCE(SUM(vd.subtotal), 0) AS total_vendido ")
                .append("FROM productos p ")
                .append("JOIN marcas m ON p.id_marca = m.id_marca ")
                .append("LEFT JOIN venta_detalles vd ON p.id_producto = vd.id_producto ")
                .append("AND vd.activo = 1 ")
                .append("LEFT JOIN ventas v ON vd.id_venta = v.id_venta ")
                .append("AND v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ")
                .append("LEFT JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE p.activo = 1 ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND (u.id_bodega = ? OR u.id_bodega IS NULL) ");
        }
        sqlBuilder.append("GROUP BY p.id_producto, p.nombre, p.talla, m.nombre ")
                .append("ORDER BY total_vendido ASC ")
                .append("LIMIT ?");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex++, usuario.getIdBodega());
            }
            ps.setInt(paramIndex, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String marca = rs.getString("marca");
                    String producto = rs.getString("nombre");
                    String talla = rs.getString("talla");
                    double totalVendido = rs.getDouble("total_vendido");
                    StringBuilder nombreCompleto = new StringBuilder();
                    nombreCompleto.append(marca).append(" ").append(producto);
                    if (talla != null && !talla.isEmpty()) {
                        nombreCompleto.append(" T").append(talla);
                    }
                    dataset.addValue(nombreCompleto.toString(), totalVendido);
                }
            }
            if (dataset.getItemCount() == 0) {
                dataset.addValue("Sin datos disponibles", 0.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener productos menos vendidos", e);
            dataset.addValue("Error al cargar datos", 0.0);
        }
        return dataset;
    }

    public List<Map<String, Object>> getRankingVendedores(int limite, int dias,
            ModelUser usuario) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("u.id_usuario, ")
                .append("u.nombre AS vendedor, ")
                .append("COUNT(v.id_venta) AS num_ventas, ")
                .append("SUM(v.total) AS total_ventas, ")
                .append("AVG(v.total) AS promedio_venta ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ? ");
        }
        sqlBuilder.append("GROUP BY u.id_usuario, u.nombre ")
                .append("ORDER BY total_ventas DESC ")
                .append("LIMIT ?");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex++, usuario.getIdBodega());
            }
            ps.setInt(paramIndex, limite);
            try (ResultSet rs = ps.executeQuery()) {
                int posicion = 1;
                while (rs.next()) {
                    Map<String, Object> vendedor = new HashMap<>();
                    vendedor.put("posicion", posicion++);
                    vendedor.put("id_usuario", rs.getInt("id_usuario"));
                    vendedor.put("nombre", rs.getString("vendedor"));
                    vendedor.put("num_ventas", rs.getInt("num_ventas"));
                    vendedor.put("total_ventas", rs.getDouble("total_ventas"));
                    vendedor.put("promedio_venta", rs.getDouble("promedio_venta"));
                    ranking.add(vendedor);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ranking de vendedores", e);
        }
        return ranking;
    }

    public List<Map<String, Object>> getAlertasInventario(int limite, ModelUser usuario) {
        List<Map<String, Object>> alertas = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT p.id_producto, ")
                .append("p.codigo_modelo, ")
                .append("p.nombre, ")
                .append("p.talla, ")
                .append("p.color, ")
                .append("p.ubicacion, ")
                .append("p.stock_minimo, ")
                .append("c.nombre AS categoria, ")
                .append("COALESCE(SUM(COALESCE(ib.Stock_par,0) + ")
                .append("(COALESCE(ib.Stock_caja,0) * p.pares_por_caja)), 0) AS stock_total ")
                .append("FROM productos p ")
                .append("LEFT JOIN producto_variantes pv ON p.id_producto = pv.id_producto AND pv.disponible = 1 ")
                .append("LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 ")
                .append("JOIN categorias c ON p.id_categoria = c.id_categoria ")
                .append("WHERE p.activo = 1 ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND ib.id_bodega = ? ");
        }
        sqlBuilder.append("GROUP BY p.id_producto, p.codigo_modelo, p.nombre, p.talla, p.color, ")
                .append("p.ubicacion, p.stock_minimo, p.pares_por_caja, c.nombre ")
                .append("HAVING stock_total <= p.stock_minimo ")
                .append("ORDER BY stock_total ASC ")
                .append("LIMIT ?");
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex++, usuario.getIdBodega());
            }
            ps.setInt(paramIndex, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> alerta = new HashMap<>();
                    String nombre = rs.getString("nombre");
                    String talla = rs.getString("talla");
                    String color = rs.getString("color");
                    StringBuilder nombreCompleto = new StringBuilder(nombre);
                    if (talla != null && !talla.isEmpty()) {
                        nombreCompleto.append(" T").append(talla);
                    }
                    if (color != null && !color.isEmpty()) {
                        nombreCompleto.append(" ").append(color);
                    }
                    alerta.put("id_producto", rs.getInt("id_producto"));
                    alerta.put("codigo_modelo", rs.getString("codigo_modelo"));
                    alerta.put("nombre", nombreCompleto.toString());
                    alerta.put("ubicacion", rs.getString("ubicacion"));
                    int stockTotal = rs.getInt("stock_total");
                    int stockMinimo = rs.getInt("stock_minimo");
                    alerta.put("stock_actual", stockTotal);
                    alerta.put("stock_minimo", stockMinimo);
                    alerta.put("categoria", rs.getString("categoria"));
                    if (stockTotal == 0) {
                        alerta.put("estado", "Sin Stock");
                    } else if (stockTotal < stockMinimo / 2) {
                        alerta.put("estado", "Crítico");
                    } else {
                        alerta.put("estado", "Bajo Stock");
                    }
                    alertas.add(alerta);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener alertas de inventario", e);
        }
        return alertas;
    }

    public Map<String, Object> getResumenDashboard(int dias, ModelUser usuario) {
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("ventas_totales", getVentasTotalesPeriodo(dias, usuario));
        resumen.put("ganancia_total", getGananciaTotalPeriodo(dias, usuario));
        double ventas = (double) resumen.get("ventas_totales");
        double ganancia = (double) resumen.get("ganancia_total");
        double margen = ventas > 0 ? (ganancia / ventas) * 100 : 0;
        resumen.put("margen_ganancia", margen);
        int numTransacciones = getNumeroTransacciones(dias, usuario);
        resumen.put("num_transacciones", numTransacciones);
        double ticketPromedio = numTransacciones > 0 ? ventas / numTransacciones : 0;
        resumen.put("ticket_promedio", ticketPromedio);
        return resumen;
    }

    private int getNumeroTransacciones(int dias, ModelUser usuario) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(DISTINCT v.id_venta) AS total ")
                .append("FROM ventas v ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlBuilder.append("AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(paramIndex, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener número de transacciones", e);
        }
        return 0;
    }

    public Map<String, Integer> getComparacionPeriodoAnterior(LocalDate start, LocalDate end,
            ModelUser usuario) {
        Map<String, Integer> variaciones = new HashMap<>();
        double ventasActual = getVentasTotales(usuario);
        double gananciaActual = getGananciaTotalPeriodo(
                (int) java.time.temporal.ChronoUnit.DAYS.between(start, end), usuario);
        long dias = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevStart = start.minusDays(dias);
        LocalDate prevEnd = end.minusDays(dias);
        double ventasPrev = getVentasTotales(usuario);
        double gananciaPrev = getGananciaTotalPeriodo((int) dias, usuario);
        variaciones.put("ventas", calcularVariacion(ventasActual, ventasPrev));
        variaciones.put("ganancia", calcularVariacion(gananciaActual, gananciaPrev));
        variaciones.put("margen", 0);
        variaciones.put("rotacion", 0);
        return variaciones;
    }

    private int calcularVariacion(double actual, double anterior) {
        if (anterior == 0) {
            return actual > 0 ? 100 : 0;
        }
        return (int) (((actual - anterior) / anterior) * 100);
    }

    // ==================================================================================
    // MÉTODOS CONTABLES - VENTAS NETAS CON DEVOLUCIONES
    // ==================================================================================

    /**
     * Obtiene las ventas netas de un período restando las devoluciones
     * aprobadas/finalizadas.
     * Usa subtotal (pre-IVA) para precisión contable.
     *
     * @param dias    Número de días hacia atrás desde hoy
     * @param usuario Usuario que solicita (filtra por bodega)
     * @return Ventas netas = SUM(vd.subtotal) - devoluciones aprobadas
     */
    public double getVentasNetasPeriodo(int dias, ModelUser usuario) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("COALESCE(SUM(vd.subtotal), 0) AS ventas_brutas, ")
                .append("COALESCE((SELECT SUM(d.subtotal_devolucion) FROM devoluciones d ")
                .append("JOIN ventas v2 ON d.id_venta = v2.id_venta ")
                .append("JOIN usuarios u2 ON v2.id_usuario = u2.id_usuario ")
                .append("WHERE d.estado IN ('aprobada','finalizada') ")
                .append("AND d.activa = 1 ")
                .append("AND d.fecha_devolucion >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND u2.id_bodega = ? ");
        }
        sql.append("), 0) AS total_devoluciones ")
                .append("FROM ventas v ")
                .append("JOIN venta_detalles vd ON v.id_venta = vd.id_venta ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE v.estado IN ('completada', 'pendiente') ")
                .append("AND vd.activo = 1 ")
                .append("AND v.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND u.id_bodega = ?");
        }

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            // Parámetro para subconsulta de devoluciones: días
            ps.setInt(idx++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                // Parámetro para subconsulta de devoluciones: bodega
                ps.setInt(idx++, usuario.getIdBodega());
            }
            // Parámetro para consulta principal: días
            ps.setInt(idx++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                // Parámetro para consulta principal: bodega
                ps.setInt(idx++, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double brutas = rs.getDouble("ventas_brutas");
                    double devoluciones = rs.getDouble("total_devoluciones");
                    return brutas - devoluciones;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas netas del período", e);
        }
        return 0.0;
    }

    /**
     * Obtiene el total de devoluciones (subtotal sin IVA) en un período.
     *
     * @param dias    Número de días hacia atrás
     * @param usuario Usuario que solicita
     * @return Monto total de devoluciones aprobadas/finalizadas
     */
    public double getTotalDevolucionesPeriodo(int dias, ModelUser usuario) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(d.subtotal_devolucion), 0) AS total ")
                .append("FROM devoluciones d ")
                .append("JOIN ventas v ON d.id_venta = v.id_venta ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE d.estado IN ('aprobada','finalizada') ")
                .append("AND d.activa = 1 ")
                .append("AND d.fecha_devolucion >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND u.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(idx, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener total de devoluciones", e);
        }
        return 0.0;
    }

    // ==================================================================================
    // MÉTODOS CONTABLES - UTILIDAD NETA
    // ==================================================================================

    /**
     * Calcula la utilidad neta de un período:
     * Utilidad Neta = Utilidad Bruta - Gastos Operativos
     *
     * La utilidad bruta se obtiene de getGananciaTotalPeriodo (vd.subtotal -
     * costos).
     * Los gastos operativos se suman de gastos_operativos con estado='registrado'.
     *
     * @param dias    Número de días hacia atrás
     * @param usuario Usuario que solicita
     * @return Utilidad neta del período
     */
    public double getUtilidadNetaPeriodo(int dias, ModelUser usuario) {
        double utilidadBruta = getGananciaTotalPeriodo(dias, usuario);
        double gastosOperativos = getGastosOperativosPeriodo(dias, usuario);
        return utilidadBruta - gastosOperativos;
    }

    /**
     * Obtiene el total de gastos operativos registrados en un período.
     * Solo incluye gastos con estado='registrado' (no anulados ni contabilizados
     * duplicados).
     *
     * @param dias    Número de días hacia atrás
     * @param usuario Usuario que solicita
     * @return Total de gastos operativos
     */
    public double getGastosOperativosPeriodo(int dias, ModelUser usuario) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(g.monto), 0) AS total ")
                .append("FROM gastos_operativos g ")
                .append("WHERE g.estado = 'registrado' ")
                .append("AND g.fecha_gasto >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND g.id_bodega = ?");
        }
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(idx, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener gastos operativos del período", e);
        }
        return 0.0;
    }

    // ==================================================================================
    // MÉTODOS CONTABLES - ANÁLISIS DE NOTAS DE CRÉDITO
    // ==================================================================================

    /**
     * Retorna métricas de uso de notas de crédito para entender cuánta venta
     * nueva es en realidad canje de deuda anterior.
     *
     * Claves retornadas:
     * - "total_generado": total nominal de notas emitidas en el período
     * - "saldo_usado": cuánto se ha canjeado en ventas
     * - "saldo_disponible": cuánto queda por canjear (notas activas)
     * - "porcentaje_uso": (saldo_usado / total_generado) * 100
     * - "cantidad_notas": cuántas notas se emitieron en el período
     *
     * @param dias    Número de días hacia atrás
     * @param usuario Usuario que solicita
     * @return Mapa con métricas de créditos
     */
    public Map<String, Double> getUsoCreditosCliente(int dias, ModelUser usuario) {
        Map<String, Double> metricas = new HashMap<>();
        metricas.put("total_generado", 0.0);
        metricas.put("saldo_usado", 0.0);
        metricas.put("saldo_disponible", 0.0);
        metricas.put("porcentaje_uso", 0.0);
        metricas.put("cantidad_notas", 0.0);

        // Consulta 1: Notas emitidas en el período
        StringBuilder sqlEmitidas = new StringBuilder();
        sqlEmitidas.append("SELECT ")
                .append("COUNT(*) AS cantidad_notas, ")
                .append("COALESCE(SUM(nc.total), 0) AS total_generado, ")
                .append("COALESCE(SUM(nc.saldo_usado), 0) AS saldo_usado ")
                .append("FROM notas_credito nc ")
                .append("JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion ")
                .append("JOIN ventas v ON d.id_venta = v.id_venta ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE nc.activa = 1 ")
                .append("AND nc.fecha_emision >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlEmitidas.append("AND u.id_bodega = ?");
        }

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlEmitidas.toString())) {
            int idx = 1;
            ps.setInt(idx++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(idx, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double totalGenerado = rs.getDouble("total_generado");
                    double saldoUsado = rs.getDouble("saldo_usado");
                    double cantidadNotas = rs.getDouble("cantidad_notas");
                    metricas.put("total_generado", totalGenerado);
                    metricas.put("saldo_usado", saldoUsado);
                    metricas.put("cantidad_notas", cantidadNotas);
                    if (totalGenerado > 0) {
                        metricas.put("porcentaje_uso", (saldoUsado / totalGenerado) * 100);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener uso de créditos de cliente", e);
        }

        // Consulta 2: Saldo disponible de notas activas (no vencidas)
        StringBuilder sqlDisponible = new StringBuilder();
        sqlDisponible.append("SELECT COALESCE(SUM(nc.saldo_disponible), 0) AS saldo_disponible ")
                .append("FROM notas_credito nc ")
                .append("JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion ")
                .append("JOIN ventas v ON d.id_venta = v.id_venta ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE nc.activa = 1 ")
                .append("AND nc.estado IN ('emitida','generada') ")
                .append("AND nc.fecha_vencimiento >= CURRENT_DATE ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sqlDisponible.append("AND u.id_bodega = ?");
        }

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sqlDisponible.toString())) {
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(1, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    metricas.put("saldo_disponible", rs.getDouble("saldo_disponible"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener saldo disponible de notas de crédito", e);
        }

        return metricas;
    }

    // ==================================================================================
    // MÉTODOS CONTABLES - DÍAS DE INVENTARIO POR CATEGORÍA
    // ==================================================================================

    /**
     * Estima cuántos días de inventario quedan por categoría.
     * Fórmula: stock_actual / promedio_salida_diaria (últimos 30 días)
     *
     * Retorna un mapa donde la clave es el nombre de categoría y el valor
     * es el número estimado de días de inventario. Si no hay ventas recientes,
     * se marca como -1 (inventario sin rotación).
     *
     * @param usuario Usuario que solicita
     * @return Mapa categoría → días de inventario estimados
     */
    public Map<String, Double> getDiasInventarioPorCategoria(ModelUser usuario) {
        Map<String, Double> diasPorCategoria = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("c.nombre AS categoria, ")
                .append("COALESCE(SUM(COALESCE(ib.Stock_par, 0) + ")
                .append("(COALESCE(ib.Stock_caja, 0) * p.pares_por_caja)), 0) AS stock_actual, ")
                .append("COALESCE((SELECT SUM(vd2.cantidad) ")
                .append("FROM venta_detalles vd2 ")
                .append("JOIN productos p2 ON vd2.id_producto = p2.id_producto ")
                .append("JOIN ventas v2 ON vd2.id_venta = v2.id_venta ")
                .append("JOIN usuarios u2 ON v2.id_usuario = u2.id_usuario ")
                .append("WHERE p2.id_categoria = c.id_categoria ")
                .append("AND v2.estado IN ('completada', 'pendiente') ")
                .append("AND vd2.activo = 1 ")
                .append("AND v2.fecha_venta >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND u2.id_bodega = ? ");
        }
        sql.append("), 0) AS ventas_30d ")
                .append("FROM categorias c ")
                .append("JOIN productos p ON c.id_categoria = p.id_categoria ")
                .append("LEFT JOIN producto_variantes pv ON p.id_producto = pv.id_producto AND pv.disponible = 1 ")
                .append("LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.activo = 1 ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND ib.id_bodega = ? ");
        }
        sql.append("WHERE p.activo = 1 ")
                .append("GROUP BY c.id_categoria, c.nombre ")
                .append("ORDER BY c.nombre");

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(idx++, usuario.getIdBodega()); // Subconsulta ventas_30d
                ps.setInt(idx, usuario.getIdBodega()); // JOIN inventario_bodega
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String categoria = rs.getString("categoria");
                    double stockActual = rs.getDouble("stock_actual");
                    double ventas30d = rs.getDouble("ventas_30d");

                    double dias;
                    if (ventas30d <= 0) {
                        // Sin ventas recientes: inventario sin rotación
                        dias = stockActual > 0 ? -1.0 : 0.0;
                    } else {
                        double promedioDiario = ventas30d / 30.0;
                        dias = Math.round((stockActual / promedioDiario) * 10.0) / 10.0;
                    }
                    diasPorCategoria.put(categoria, dias);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular días de inventario por categoría", e);
        }
        return diasPorCategoria;
    }

    // ==================================================================================
    // MÉTODOS CONTABLES - DETALLE DE DEVOLUCIONES
    // ==================================================================================

    /**
     * Retorna estadísticas detalladas de devoluciones en un período.
     *
     * Claves retornadas:
     * - "total_devuelto": monto subtotal de devoluciones
     * - "numero_devoluciones": cantidad de devoluciones
     * - "porcentaje_sobre_ventas": (total_devuelto / ventas_brutas) * 100
     * - "motivo_defecto_fabrica", "motivo_talla_incorrecta", etc.: desglose por
     * motivo
     *
     * @param dias    Número de días hacia atrás
     * @param usuario Usuario que solicita
     * @return Mapa con estadísticas de devoluciones
     */
    public Map<String, Double> getDevolucionesDetalladoPeriodo(int dias, ModelUser usuario) {
        Map<String, Double> stats = new LinkedHashMap<>();
        stats.put("total_devuelto", 0.0);
        stats.put("numero_devoluciones", 0.0);
        stats.put("porcentaje_sobre_ventas", 0.0);

        // Consulta principal: totales y desglose por motivo
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("COUNT(*) AS numero_devoluciones, ")
                .append("COALESCE(SUM(d.subtotal_devolucion), 0) AS total_devuelto, ")
                .append("SUM(CASE WHEN d.motivo = 'defecto_fabrica' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_defecto_fabrica, ")
                .append("SUM(CASE WHEN d.motivo = 'talla_incorrecta' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_talla_incorrecta, ")
                .append("SUM(CASE WHEN d.motivo = 'producto_dañado' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_producto_danado, ")
                .append("SUM(CASE WHEN d.motivo = 'insatisfaccion' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_insatisfaccion, ")
                .append("SUM(CASE WHEN d.motivo = 'error_facturacion' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_error_facturacion, ")
                .append("SUM(CASE WHEN d.motivo = 'cambio_modelo' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_cambio_modelo, ")
                .append("SUM(CASE WHEN d.motivo = 'otros' THEN d.subtotal_devolucion ELSE 0 END) AS motivo_otros ")
                .append("FROM devoluciones d ")
                .append("JOIN ventas v ON d.id_venta = v.id_venta ")
                .append("JOIN usuarios u ON v.id_usuario = u.id_usuario ")
                .append("WHERE d.estado IN ('aprobada','finalizada') ")
                .append("AND d.activa = 1 ")
                .append("AND d.fecha_devolucion >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ");
        if (usuario != null && usuario.getIdBodega() != null) {
            sql.append("AND u.id_bodega = ?");
        }

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, dias);
            if (usuario != null && usuario.getIdBodega() != null) {
                ps.setInt(idx, usuario.getIdBodega());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double totalDevuelto = rs.getDouble("total_devuelto");
                    double numDevoluciones = rs.getDouble("numero_devoluciones");
                    stats.put("total_devuelto", totalDevuelto);
                    stats.put("numero_devoluciones", numDevoluciones);
                    stats.put("motivo_defecto_fabrica", rs.getDouble("motivo_defecto_fabrica"));
                    stats.put("motivo_talla_incorrecta", rs.getDouble("motivo_talla_incorrecta"));
                    stats.put("motivo_producto_danado", rs.getDouble("motivo_producto_danado"));
                    stats.put("motivo_insatisfaccion", rs.getDouble("motivo_insatisfaccion"));
                    stats.put("motivo_error_facturacion", rs.getDouble("motivo_error_facturacion"));
                    stats.put("motivo_cambio_modelo", rs.getDouble("motivo_cambio_modelo"));
                    stats.put("motivo_otros", rs.getDouble("motivo_otros"));

                    // Calcular porcentaje sobre ventas brutas
                    double ventasBrutas = getVentasTotalesPeriodo(dias, usuario);
                    if (ventasBrutas > 0) {
                        stats.put("porcentaje_sobre_ventas",
                                Math.round((totalDevuelto / ventasBrutas) * 10000.0) / 100.0);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener detalle de devoluciones", e);
        }
        return stats;
    }}

    

    

    
    
        
            
            
            
        
    

    
    
    
        
    

    