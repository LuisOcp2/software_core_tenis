package raven.clases.reportes;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.principal.conexion;

/**
 * Servicio para generar reportes de gastos operativos
 */
public class ServiceReporteGastos {

    private static final Logger LOGGER = Logger.getLogger(ServiceReporteGastos.class.getName());

    /**
     * Obtiene gastos por tipo en un período
     */
    public List<Map<String, Object>> getGastosPorTipo(int idTipoGasto, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    go.id_gasto,
                    tg.nombre AS tipo_gasto,
                    tg.categoria,
                    go.concepto,
                    go.monto,
                    go.proveedor_persona,
                    go.numero_recibo,
                    b.nombre AS bodega,
                    u.nombre AS usuario,
                    go.estado,
                    go.fecha_gasto,
                    go.observaciones
                FROM gastos_operativos go
                JOIN tipos_gastos tg ON go.id_tipo_gasto = tg.id_tipo_gasto
                JOIN bodegas b ON go.id_bodega = b.id_bodega
                JOIN usuarios u ON go.id_usuario = u.id_usuario
                WHERE DATE(go.fecha_gasto) BETWEEN ? AND ?
                AND (? = 0 OR go.id_tipo_gasto = ?)
                AND go.estado != 'anulado'
                ORDER BY go.fecha_gasto DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setInt(3, idTipoGasto);
            ps.setInt(4, idTipoGasto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_gasto", rs.getInt("id_gasto"));
                    row.put("tipo_gasto", rs.getString("tipo_gasto"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("concepto", rs.getString("concepto"));
                    row.put("monto", rs.getBigDecimal("monto"));
                    row.put("proveedor_persona", rs.getString("proveedor_persona"));
                    row.put("numero_recibo", rs.getString("numero_recibo"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("usuario", rs.getString("usuario"));
                    row.put("estado", rs.getString("estado"));
                    row.put("fecha_gasto", rs.getTimestamp("fecha_gasto"));
                    row.put("observaciones", rs.getString("observaciones"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo gastos por tipo", e);
        }
        return resultados;
    }

    /**
     * Obtiene resumen de gastos por categoría
     */
    public List<Map<String, Object>> getResumenPorCategoria(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    tg.categoria,
                    COUNT(*) AS total_gastos,
                    SUM(go.monto) AS monto_total,
                    AVG(go.monto) AS promedio_gasto,
                    MIN(go.monto) AS gasto_minimo,
                    MAX(go.monto) AS gasto_maximo
                FROM gastos_operativos go
                JOIN tipos_gastos tg ON go.id_tipo_gasto = tg.id_tipo_gasto
                WHERE DATE(go.fecha_gasto) BETWEEN ? AND ?
                AND go.estado != 'anulado'
                GROUP BY tg.categoria
                ORDER BY monto_total DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("categoria", rs.getString("categoria"));
                    row.put("total_gastos", rs.getInt("total_gastos"));
                    row.put("monto_total", rs.getBigDecimal("monto_total"));
                    row.put("promedio_gasto", rs.getBigDecimal("promedio_gasto"));
                    row.put("gasto_minimo", rs.getBigDecimal("gasto_minimo"));
                    row.put("gasto_maximo", rs.getBigDecimal("gasto_maximo"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen por categoría", e);
        }
        return resultados;
    }

    /**
     * Obtiene gastos por bodega
     */
    public List<Map<String, Object>> getGastosPorBodega(int idBodega, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    b.id_bodega,
                    b.nombre AS bodega,
                    tg.categoria,
                    COUNT(*) AS total_gastos,
                    SUM(go.monto) AS monto_total
                FROM gastos_operativos go
                JOIN bodegas b ON go.id_bodega = b.id_bodega
                JOIN tipos_gastos tg ON go.id_tipo_gasto = tg.id_tipo_gasto
                WHERE DATE(go.fecha_gasto) BETWEEN ? AND ?
                AND (? = 0 OR go.id_bodega = ?)
                AND go.estado != 'anulado'
                GROUP BY b.id_bodega, b.nombre, tg.categoria
                ORDER BY b.nombre, monto_total DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setInt(3, idBodega);
            ps.setInt(4, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_bodega", rs.getInt("id_bodega"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("total_gastos", rs.getInt("total_gastos"));
                    row.put("monto_total", rs.getBigDecimal("monto_total"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo gastos por bodega", e);
        }
        return resultados;
    }

    /**
     * Obtiene resumen mensual de gastos
     */
    public Map<String, Object> getResumenGastosMensual(int year, int month) {
        Map<String, Object> resultado = new HashMap<>();
        String sql = """
                SELECT
                    COUNT(*) AS total_gastos,
                    COALESCE(SUM(monto), 0) AS monto_total,
                    COALESCE(AVG(monto), 0) AS promedio_gasto,
                    COUNT(DISTINCT id_tipo_gasto) AS tipos_distintos,
                    COUNT(DISTINCT id_bodega) AS bodegas_distintas
                FROM gastos_operativos
                WHERE YEAR(fecha_gasto) = ? AND MONTH(fecha_gasto) = ?
                AND estado != 'anulado'
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("total_gastos", rs.getInt("total_gastos"));
                    resultado.put("monto_total", rs.getBigDecimal("monto_total"));
                    resultado.put("promedio_gasto", rs.getBigDecimal("promedio_gasto"));
                    resultado.put("tipos_distintos", rs.getInt("tipos_distintos"));
                    resultado.put("bodegas_distintas", rs.getInt("bodegas_distintas"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen de gastos mensual", e);
        }
        return resultado;
    }

    /**
     * Obtiene evolución diaria de gastos en un período
     */
    public List<Map<String, Object>> getEvolucionDiaria(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    DATE(fecha_gasto) AS fecha,
                    COUNT(*) AS total_gastos,
                    SUM(monto) AS monto_total
                FROM gastos_operativos
                WHERE DATE(fecha_gasto) BETWEEN ? AND ?
                AND estado != 'anulado'
                GROUP BY DATE(fecha_gasto)
                ORDER BY fecha
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("fecha", rs.getDate("fecha"));
                    row.put("total_gastos", rs.getInt("total_gastos"));
                    row.put("monto_total", rs.getBigDecimal("monto_total"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo evolución diaria", e);
        }
        return resultados;
    }

    /**
     * Obtiene tipos de gastos para filtros
     */
    public List<Map<String, Object>> getTiposGasto() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = "SELECT id_tipo_gasto, nombre, categoria FROM tipos_gastos WHERE activo = 1 ORDER BY categoria, nombre";
        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_tipo_gasto", rs.getInt("id_tipo_gasto"));
                row.put("nombre", rs.getString("nombre"));
                row.put("categoria", rs.getString("categoria"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo tipos de gasto", e);
        }
        return resultados;
    }
}
