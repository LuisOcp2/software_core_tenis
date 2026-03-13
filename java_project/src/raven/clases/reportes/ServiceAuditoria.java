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
 * Servicio para reportes de auditoría y trazabilidad
 * Solo accesible por admin y gerente
 */
public class ServiceAuditoria {

    private static final Logger LOGGER = Logger.getLogger(ServiceAuditoria.class.getName());

    public List<Map<String, Object>> getTrazabilidadProducto(int idVariante, LocalDate inicio, LocalDate fin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT a.id_auditoria, a.tipo_evento, a.cantidad, a.fecha_evento, a.observaciones,
                    pv.sku, p.nombre AS producto, pr.nombre AS proveedor,
                    bo.nombre AS bodega_origen, bd.nombre AS bodega_destino, u.nombre AS usuario
                FROM auditoria_trazabilidad a
                LEFT JOIN producto_variantes pv ON a.id_variante = pv.id_variante
                LEFT JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN proveedores pr ON a.id_proveedor = pr.id_proveedor
                LEFT JOIN bodegas bo ON a.id_bodega_origen = bo.id_bodega
                LEFT JOIN bodegas bd ON a.id_bodega_destino = bd.id_bodega
                LEFT JOIN usuarios u ON a.id_usuario = u.id_usuario
                WHERE (? = 0 OR a.id_variante = ?) AND DATE(a.fecha_evento) BETWEEN ? AND ?
                AND a.activo = 1 ORDER BY a.fecha_evento DESC LIMIT 500
                """;
        try (Connection con = conexion.getConnectionStatic(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVariante);
            ps.setInt(2, idVariante);
            ps.setObject(3, inicio);
            ps.setObject(4, fin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_auditoria", rs.getInt("id_auditoria"));
                    row.put("tipo_evento", rs.getString("tipo_evento"));
                    row.put("cantidad", rs.getInt("cantidad"));
                    row.put("fecha_evento", rs.getTimestamp("fecha_evento"));
                    row.put("sku", rs.getString("sku"));
                    row.put("producto", rs.getString("producto"));
                    row.put("proveedor", rs.getString("proveedor"));
                    row.put("bodega_origen", rs.getString("bodega_origen"));
                    row.put("bodega_destino", rs.getString("bodega_destino"));
                    row.put("usuario", rs.getString("usuario"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error trazabilidad", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getHistorialSesiones(int idUsuario, LocalDate inicio, LocalDate fin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT h.id_historial, h.accion, h.fecha_accion, h.detalles,
                    u.username, u.nombre, u.rol, sa.ip_address, sa.hostname
                FROM historial_sesiones h
                JOIN usuarios u ON h.id_usuario = u.id_usuario
                LEFT JOIN sesiones_activas sa ON h.id_sesion = sa.id_sesion
                WHERE (? = 0 OR h.id_usuario = ?) AND DATE(h.fecha_accion) BETWEEN ? AND ?
                ORDER BY h.fecha_accion DESC LIMIT 500
                """;
        try (Connection con = conexion.getConnectionStatic(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idUsuario);
            ps.setObject(3, inicio);
            ps.setObject(4, fin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_historial", rs.getInt("id_historial"));
                    row.put("accion", rs.getString("accion"));
                    row.put("fecha_accion", rs.getTimestamp("fecha_accion"));
                    row.put("username", rs.getString("username"));
                    row.put("nombre", rs.getString("nombre"));
                    row.put("rol", rs.getString("rol"));
                    row.put("ip_address", rs.getString("ip_address"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error historial sesiones", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getDiscrepanciasConteo() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT dc.id_detalle_conteo, c.nombre AS conteo, p.nombre AS producto,
                    dc.stock_sistema, dc.stock_contado, dc.diferencia, dc.estado, dc.fecha_conteo,
                    u.nombre AS contador
                FROM detalles_conteo_inventario dc
                JOIN conteos_inventario c ON dc.id_conteo = c.id_conteo
                JOIN productos p ON dc.id_producto = p.id_producto
                LEFT JOIN usuarios u ON dc.id_usuario_contador = u.id_usuario
                WHERE dc.diferencia != 0 ORDER BY ABS(dc.diferencia) DESC LIMIT 200
                """;
        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("conteo", rs.getString("conteo"));
                row.put("producto", rs.getString("producto"));
                row.put("stock_sistema", rs.getInt("stock_sistema"));
                row.put("stock_contado", rs.getInt("stock_contado"));
                row.put("diferencia", rs.getInt("diferencia"));
                row.put("contador", rs.getString("contador"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error discrepancias", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getAuditoriaPorProveedor(int idProveedor, LocalDate inicio, LocalDate fin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT pr.nombre AS proveedor, a.tipo_evento, COUNT(*) AS num_eventos,
                    SUM(a.cantidad) AS total_cantidad
                FROM auditoria_trazabilidad a
                JOIN proveedores pr ON a.id_proveedor = pr.id_proveedor
                WHERE (? = 0 OR a.id_proveedor = ?) AND DATE(a.fecha_evento) BETWEEN ? AND ?
                GROUP BY pr.id_proveedor, a.tipo_evento ORDER BY total_cantidad DESC
                """;
        try (Connection con = conexion.getConnectionStatic(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ps.setInt(2, idProveedor);
            ps.setObject(3, inicio);
            ps.setObject(4, fin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("proveedor", rs.getString("proveedor"));
                    row.put("tipo_evento", rs.getString("tipo_evento"));
                    row.put("num_eventos", rs.getInt("num_eventos"));
                    row.put("total_cantidad", rs.getInt("total_cantidad"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error auditoria proveedor", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getUsuarios() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = "SELECT id_usuario, username, nombre, rol FROM usuarios WHERE activo = 1 ORDER BY nombre";
        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_usuario", rs.getInt("id_usuario"));
                row.put("username", rs.getString("username"));
                row.put("nombre", rs.getString("nombre"));
                row.put("rol", rs.getString("rol"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error usuarios", e);
        }
        return resultados;
    }
}
