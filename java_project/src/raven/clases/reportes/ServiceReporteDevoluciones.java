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
 * Servicio para generar reportes de devoluciones y notas de crédito
 */
public class ServiceReporteDevoluciones {

    private static final Logger LOGGER = Logger.getLogger(ServiceReporteDevoluciones.class.getName());

    /**
     * Obtiene devoluciones por período
     */
    public List<Map<String, Object>> getDevolucionesPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    d.id_devolucion,
                    d.numero_devolucion,
                    d.fecha_devolucion,
                    d.tipo_devolucion,
                    d.motivo,
                    d.estado,
                    d.total_devolucion,
                    c.nombre AS cliente,
                    c.dni,
                    u.nombre AS usuario_procesa,
                    v.id_venta,
                    v.total AS total_venta_original,
                    nc.numero_nota_credito,
                    nc.saldo_disponible,
                    nc.estado AS estado_nota
                FROM devoluciones d
                JOIN clientes c ON d.id_cliente = c.id_cliente
                JOIN usuarios u ON d.id_usuario_procesa = u.id_usuario
                JOIN ventas v ON d.id_venta = v.id_venta
                LEFT JOIN notas_credito nc ON d.id_devolucion = nc.id_devolucion
                WHERE DATE(d.fecha_devolucion) BETWEEN ? AND ?
                AND d.activa = 1
                ORDER BY d.fecha_devolucion DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_devolucion", rs.getInt("id_devolucion"));
                    row.put("numero_devolucion", rs.getString("numero_devolucion"));
                    row.put("fecha_devolucion", rs.getTimestamp("fecha_devolucion"));
                    row.put("tipo_devolucion", rs.getString("tipo_devolucion"));
                    row.put("motivo", rs.getString("motivo"));
                    row.put("estado", rs.getString("estado"));
                    row.put("total_devolucion", rs.getBigDecimal("total_devolucion"));
                    row.put("cliente", rs.getString("cliente"));
                    row.put("dni", rs.getString("dni"));
                    row.put("usuario_procesa", rs.getString("usuario_procesa"));
                    row.put("id_venta", rs.getInt("id_venta"));
                    row.put("total_venta_original", rs.getBigDecimal("total_venta_original"));
                    row.put("numero_nota_credito", rs.getString("numero_nota_credito"));
                    row.put("saldo_disponible", rs.getBigDecimal("saldo_disponible"));
                    row.put("estado_nota", rs.getString("estado_nota"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo devoluciones por período", e);
        }
        return resultados;
    }

    /**
     * Obtiene análisis de devoluciones por motivo
     */
    public List<Map<String, Object>> getAnalisisPorMotivo(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    d.motivo,
                    COUNT(*) AS total_devoluciones,
                    SUM(d.total_devolucion) AS monto_total,
                    AVG(d.total_devolucion) AS promedio_devolucion,
                    COUNT(DISTINCT d.id_cliente) AS clientes_distintos,
                    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM devoluciones
                        WHERE DATE(fecha_devolucion) BETWEEN ? AND ? AND activa = 1), 2) AS porcentaje
                FROM devoluciones d
                WHERE DATE(d.fecha_devolucion) BETWEEN ? AND ?
                AND d.activa = 1
                GROUP BY d.motivo
                ORDER BY total_devoluciones DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setObject(3, fechaInicio);
            ps.setObject(4, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("motivo", rs.getString("motivo"));
                    row.put("total_devoluciones", rs.getInt("total_devoluciones"));
                    row.put("monto_total", rs.getBigDecimal("monto_total"));
                    row.put("promedio_devolucion", rs.getBigDecimal("promedio_devolucion"));
                    row.put("clientes_distintos", rs.getInt("clientes_distintos"));
                    row.put("porcentaje", rs.getBigDecimal("porcentaje"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo análisis por motivo", e);
        }
        return resultados;
    }

    /**
     * Obtiene notas de crédito pendientes
     */
    public List<Map<String, Object>> getNotasCreditoPendientes() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    nc.id_nota_credito,
                    nc.numero_nota_credito,
                    nc.fecha_emision,
                    nc.fecha_vencimiento,
                    nc.total,
                    nc.saldo_disponible,
                    nc.saldo_usado,
                    nc.estado,
                    c.nombre AS cliente,
                    c.dni,
                    c.telefono,
                    d.numero_devolucion,
                    DATEDIFF(nc.fecha_vencimiento, CURDATE()) AS dias_para_vencer
                FROM notas_credito nc
                JOIN clientes c ON nc.id_cliente = c.id_cliente
                JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion
                WHERE nc.estado IN ('emitida')
                AND nc.activa = 1
                AND nc.saldo_disponible > 0
                ORDER BY nc.fecha_vencimiento ASC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_nota_credito", rs.getInt("id_nota_credito"));
                row.put("numero_nota_credito", rs.getString("numero_nota_credito"));
                row.put("fecha_emision", rs.getTimestamp("fecha_emision"));
                row.put("fecha_vencimiento", rs.getTimestamp("fecha_vencimiento"));
                row.put("total", rs.getBigDecimal("total"));
                row.put("saldo_disponible", rs.getBigDecimal("saldo_disponible"));
                row.put("saldo_usado", rs.getBigDecimal("saldo_usado"));
                row.put("estado", rs.getString("estado"));
                row.put("cliente", rs.getString("cliente"));
                row.put("dni", rs.getString("dni"));
                row.put("telefono", rs.getString("telefono"));
                row.put("numero_devolucion", rs.getString("numero_devolucion"));
                row.put("dias_para_vencer", rs.getInt("dias_para_vencer"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo notas de crédito pendientes", e);
        }
        return resultados;
    }

    /**
     * Obtiene resumen de devoluciones
     */
    public Map<String, Object> getResumenDevoluciones(LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, Object> resultado = new HashMap<>();
        String sql = """
                SELECT
                    COUNT(*) AS total_devoluciones,
                    COALESCE(SUM(total_devolucion), 0) AS monto_total,
                    COUNT(CASE WHEN tipo_devolucion = 'total' THEN 1 END) AS devoluciones_totales,
                    COUNT(CASE WHEN tipo_devolucion = 'parcial' THEN 1 END) AS devoluciones_parciales,
                    COUNT(CASE WHEN estado = 'pendiente' THEN 1 END) AS pendientes,
                    COUNT(CASE WHEN estado = 'finalizada' THEN 1 END) AS finalizadas
                FROM devoluciones
                WHERE DATE(fecha_devolucion) BETWEEN ? AND ?
                AND activa = 1
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("total_devoluciones", rs.getInt("total_devoluciones"));
                    resultado.put("monto_total", rs.getBigDecimal("monto_total"));
                    resultado.put("devoluciones_totales", rs.getInt("devoluciones_totales"));
                    resultado.put("devoluciones_parciales", rs.getInt("devoluciones_parciales"));
                    resultado.put("pendientes", rs.getInt("pendientes"));
                    resultado.put("finalizadas", rs.getInt("finalizadas"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen de devoluciones", e);
        }
        return resultado;
    }

    /**
     * Obtiene detalle de una devolución
     */
    public List<Map<String, Object>> getDetalleDevolucion(int idDevolucion) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    dd.id_detalle_devolucion,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    t.numero AS talla,
                    col.nombre AS color,
                    dd.cantidad_devuelta,
                    dd.cantidad_original,
                    dd.precio_unitario_original,
                    dd.subtotal_devolucion,
                    dd.motivo_detalle,
                    dd.condicion_producto,
                    dd.accion_producto,
                    dd.observaciones_detalle
                FROM devolucion_detalles dd
                JOIN producto_variantes pv ON dd.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE dd.id_devolucion = ?
                AND dd.activo = 1
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDevolucion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_detalle", rs.getInt("id_detalle_devolucion"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("cantidad_devuelta", rs.getInt("cantidad_devuelta"));
                    row.put("cantidad_original", rs.getInt("cantidad_original"));
                    row.put("precio_unitario", rs.getBigDecimal("precio_unitario_original"));
                    row.put("subtotal", rs.getBigDecimal("subtotal_devolucion"));
                    row.put("motivo_detalle", rs.getString("motivo_detalle"));
                    row.put("condicion_producto", rs.getString("condicion_producto"));
                    row.put("accion_producto", rs.getString("accion_producto"));
                    row.put("observaciones", rs.getString("observaciones_detalle"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo detalle de devolución", e);
        }
        return resultados;
    }
}
