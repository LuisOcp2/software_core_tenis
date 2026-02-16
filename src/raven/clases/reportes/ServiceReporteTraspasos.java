package raven.clases.reportes;

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
 * Servicio para generar reportes de traspasos entre bodegas
 */
public class ServiceReporteTraspasos {

    private static final Logger LOGGER = Logger.getLogger(ServiceReporteTraspasos.class.getName());

    /**
     * Obtiene traspasos por período
     */
    public List<Map<String, Object>> getTraspasosPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    t.id_traspaso,
                    t.numero_traspaso,
                    t.fecha_solicitud,
                    t.fecha_envio,
                    t.fecha_recepcion,
                    t.estado,
                    bo.nombre AS bodega_origen,
                    bd.nombre AS bodega_destino,
                    us.nombre AS usuario_solicita,
                    ua.nombre AS usuario_autoriza,
                    ur.nombre AS usuario_recibe,
                    t.motivo,
                    t.total_productos,
                    (SELECT COUNT(*) FROM traspaso_detalles td WHERE td.id_traspaso = t.id_traspaso) AS total_items
                FROM traspasos t
                JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega
                JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega
                JOIN usuarios us ON t.id_usuario_solicita = us.id_usuario
                LEFT JOIN usuarios ua ON t.id_usuario_autoriza = ua.id_usuario
                LEFT JOIN usuarios ur ON t.id_usuario_recibe = ur.id_usuario
                WHERE DATE(t.fecha_solicitud) BETWEEN ? AND ?
                ORDER BY t.fecha_solicitud DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_traspaso", rs.getInt("id_traspaso"));
                    row.put("numero_traspaso", rs.getString("numero_traspaso"));
                    row.put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
                    row.put("fecha_envio", rs.getTimestamp("fecha_envio"));
                    row.put("fecha_recepcion", rs.getTimestamp("fecha_recepcion"));
                    row.put("estado", rs.getString("estado"));
                    row.put("bodega_origen", rs.getString("bodega_origen"));
                    row.put("bodega_destino", rs.getString("bodega_destino"));
                    row.put("usuario_solicita", rs.getString("usuario_solicita"));
                    row.put("usuario_autoriza", rs.getString("usuario_autoriza"));
                    row.put("usuario_recibe", rs.getString("usuario_recibe"));
                    row.put("motivo", rs.getString("motivo"));
                    row.put("total_productos", rs.getInt("total_productos"));
                    row.put("total_items", rs.getInt("total_items"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo traspasos por período", e);
        }
        return resultados;
    }

    /**
     * Obtiene traspasos por bodega (origen o destino)
     */
    public List<Map<String, Object>> getTraspasosPorBodega(int idBodega, String tipo) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String whereClause = tipo.equalsIgnoreCase("origen")
                ? "t.id_bodega_origen = ?"
                : tipo.equalsIgnoreCase("destino")
                        ? "t.id_bodega_destino = ?"
                        : "(t.id_bodega_origen = ? OR t.id_bodega_destino = ?)";

        String sql = """
                SELECT
                    t.id_traspaso,
                    t.numero_traspaso,
                    t.fecha_solicitud,
                    t.estado,
                    bo.nombre AS bodega_origen,
                    bd.nombre AS bodega_destino,
                    t.total_productos
                FROM traspasos t
                JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega
                JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega
                WHERE """ + whereClause + """
                ORDER BY t.fecha_solicitud DESC
                LIMIT 100
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            if (whereClause.contains("OR")) {
                ps.setInt(2, idBodega);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_traspaso", rs.getInt("id_traspaso"));
                    row.put("numero_traspaso", rs.getString("numero_traspaso"));
                    row.put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
                    row.put("estado", rs.getString("estado"));
                    row.put("bodega_origen", rs.getString("bodega_origen"));
                    row.put("bodega_destino", rs.getString("bodega_destino"));
                    row.put("total_productos", rs.getInt("total_productos"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo traspasos por bodega", e);
        }
        return resultados;
    }

    /**
     * Obtiene traspasos pendientes o en tránsito
     */
    public List<Map<String, Object>> getTraspasosPendientes() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    t.id_traspaso,
                    t.numero_traspaso,
                    t.fecha_solicitud,
                    t.fecha_envio,
                    t.estado,
                    bo.nombre AS bodega_origen,
                    bd.nombre AS bodega_destino,
                    us.nombre AS usuario_solicita,
                    t.motivo,
                    t.total_productos,
                    DATEDIFF(CURDATE(), t.fecha_solicitud) AS dias_transcurridos
                FROM traspasos t
                JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega
                JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega
                JOIN usuarios us ON t.id_usuario_solicita = us.id_usuario
                WHERE t.estado IN ('pendiente', 'autorizado', 'en_transito')
                ORDER BY t.fecha_solicitud ASC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_traspaso", rs.getInt("id_traspaso"));
                row.put("numero_traspaso", rs.getString("numero_traspaso"));
                row.put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
                row.put("fecha_envio", rs.getTimestamp("fecha_envio"));
                row.put("estado", rs.getString("estado"));
                row.put("bodega_origen", rs.getString("bodega_origen"));
                row.put("bodega_destino", rs.getString("bodega_destino"));
                row.put("usuario_solicita", rs.getString("usuario_solicita"));
                row.put("motivo", rs.getString("motivo"));
                row.put("total_productos", rs.getInt("total_productos"));
                row.put("dias_transcurridos", rs.getInt("dias_transcurridos"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo traspasos pendientes", e);
        }
        return resultados;
    }

    /**
     * Obtiene resumen de traspasos
     */
    public Map<String, Object> getResumenTraspasos(LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, Object> resultado = new HashMap<>();
        String sql = """
                SELECT
                    COUNT(*) AS total_traspasos,
                    COUNT(CASE WHEN estado = 'pendiente' THEN 1 END) AS pendientes,
                    COUNT(CASE WHEN estado = 'autorizado' THEN 1 END) AS autorizados,
                    COUNT(CASE WHEN estado = 'en_transito' THEN 1 END) AS en_transito,
                    COUNT(CASE WHEN estado = 'recibido' THEN 1 END) AS recibidos,
                    COUNT(CASE WHEN estado = 'cancelado' THEN 1 END) AS cancelados,
                    COALESCE(SUM(total_productos), 0) AS total_productos_movidos
                FROM traspasos
                WHERE DATE(fecha_solicitud) BETWEEN ? AND ?
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("total_traspasos", rs.getInt("total_traspasos"));
                    resultado.put("pendientes", rs.getInt("pendientes"));
                    resultado.put("autorizados", rs.getInt("autorizados"));
                    resultado.put("en_transito", rs.getInt("en_transito"));
                    resultado.put("recibidos", rs.getInt("recibidos"));
                    resultado.put("cancelados", rs.getInt("cancelados"));
                    resultado.put("total_productos_movidos", rs.getInt("total_productos_movidos"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen de traspasos", e);
        }
        return resultado;
    }

    /**
     * Obtiene detalle de un traspaso
     */
    public List<Map<String, Object>> getDetalleTraspaso(int idTraspaso) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    td.id_detalle_traspaso,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    t.numero AS talla,
                    col.nombre AS color,
                    td.Tipo,
                    td.cantidad_solicitada,
                    td.cantidad_enviada,
                    td.cantidad_recibida,
                    td.estado_detalle,
                    td.observaciones
                FROM traspaso_detalles td
                JOIN producto_variantes pv ON td.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE td.id_traspaso = ?
                ORDER BY p.nombre, t.numero
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_detalle", rs.getInt("id_detalle_traspaso"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("tipo", rs.getString("Tipo"));
                    row.put("cantidad_solicitada", rs.getInt("cantidad_solicitada"));
                    row.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
                    row.put("cantidad_recibida", rs.getInt("cantidad_recibida"));
                    row.put("estado_detalle", rs.getString("estado_detalle"));
                    row.put("observaciones", rs.getString("observaciones"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo detalle de traspaso", e);
        }
        return resultados;
    }

    /**
     * Obtiene flujo de productos entre bodegas
     */
    public List<Map<String, Object>> getFlujoBodegas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    bo.nombre AS bodega_origen,
                    bd.nombre AS bodega_destino,
                    COUNT(*) AS total_traspasos,
                    SUM(t.total_productos) AS total_productos
                FROM traspasos t
                JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega
                JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega
                WHERE DATE(t.fecha_solicitud) BETWEEN ? AND ?
                AND t.estado NOT IN ('cancelado')
                GROUP BY bo.id_bodega, bd.id_bodega
                ORDER BY total_productos DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("bodega_origen", rs.getString("bodega_origen"));
                    row.put("bodega_destino", rs.getString("bodega_destino"));
                    row.put("total_traspasos", rs.getInt("total_traspasos"));
                    row.put("total_productos", rs.getInt("total_productos"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo flujo de bodegas", e);
        }
        return resultados;
    }
}
