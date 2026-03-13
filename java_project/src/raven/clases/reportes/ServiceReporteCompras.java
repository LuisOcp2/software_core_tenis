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
 * Servicio para generar reportes de compras
 * Incluye compras a proveedores y compras externas
 */
public class ServiceReporteCompras {

    private static final Logger LOGGER = Logger.getLogger(ServiceReporteCompras.class.getName());

    /**
     * Obtiene compras por proveedor en un período
     */
    public List<Map<String, Object>> getComprasPorProveedor(int idProveedor, LocalDate fechaInicio,
            LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    c.id_compra,
                    c.fecha_compra,
                    pr.id_proveedor,
                    pr.nombre AS proveedor,
                    pr.ruc,
                    u.nombre AS usuario,
                    c.subtotal,
                    c.iva,
                    c.total,
                    c.estado,
                    c.observaciones,
                    COUNT(cd.id_detalle_compra) AS total_items
                FROM compras c
                JOIN proveedores pr ON c.id_proveedor = pr.id_proveedor
                JOIN usuarios u ON c.id_usuario = u.id_usuario
                LEFT JOIN compra_detalles cd ON c.id_compra = cd.id_compra
                WHERE c.fecha_compra BETWEEN ? AND ?
                AND (? = 0 OR c.id_proveedor = ?)
                GROUP BY c.id_compra
                ORDER BY c.fecha_compra DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setInt(3, idProveedor);
            ps.setInt(4, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_compra", rs.getInt("id_compra"));
                    row.put("fecha_compra", rs.getDate("fecha_compra"));
                    row.put("id_proveedor", rs.getInt("id_proveedor"));
                    row.put("proveedor", rs.getString("proveedor"));
                    row.put("ruc", rs.getString("ruc"));
                    row.put("usuario", rs.getString("usuario"));
                    row.put("subtotal", rs.getBigDecimal("subtotal"));
                    row.put("iva", rs.getBigDecimal("iva"));
                    row.put("total", rs.getBigDecimal("total"));
                    row.put("estado", rs.getString("estado"));
                    row.put("observaciones", rs.getString("observaciones"));
                    row.put("total_items", rs.getInt("total_items"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo compras por proveedor", e);
        }
        return resultados;
    }

    /**
     * Obtiene compras externas (tiendas externas)
     */
    public List<Map<String, Object>> getComprasExternas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    ce.id_compra_externa,
                    ce.numero_compra,
                    ce.tienda_proveedor,
                    ce.numero_factura_recibo,
                    b.nombre AS bodega,
                    u.nombre AS usuario,
                    ce.subtotal,
                    ce.iva,
                    ce.total,
                    ce.estado,
                    ce.fecha_compra,
                    ce.observaciones,
                    COUNT(ced.id_detalle_compra_externa) AS total_items
                FROM compras_externas ce
                JOIN bodegas b ON ce.id_bodega = b.id_bodega
                JOIN usuarios u ON ce.id_usuario = u.id_usuario
                LEFT JOIN compras_externas_detalles ced ON ce.id_compra_externa = ced.id_compra_externa
                WHERE DATE(ce.fecha_compra) BETWEEN ? AND ?
                GROUP BY ce.id_compra_externa
                ORDER BY ce.fecha_compra DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_compra_externa", rs.getInt("id_compra_externa"));
                    row.put("numero_compra", rs.getString("numero_compra"));
                    row.put("tienda_proveedor", rs.getString("tienda_proveedor"));
                    row.put("numero_factura", rs.getString("numero_factura_recibo"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("usuario", rs.getString("usuario"));
                    row.put("subtotal", rs.getBigDecimal("subtotal"));
                    row.put("iva", rs.getBigDecimal("iva"));
                    row.put("total", rs.getBigDecimal("total"));
                    row.put("estado", rs.getString("estado"));
                    row.put("fecha_compra", rs.getTimestamp("fecha_compra"));
                    row.put("observaciones", rs.getString("observaciones"));
                    row.put("total_items", rs.getInt("total_items"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo compras externas", e);
        }
        return resultados;
    }

    /**
     * Obtiene resumen mensual de compras
     */
    public Map<String, Object> getResumenComprasMensual(int year, int month) {
        Map<String, Object> resultado = new HashMap<>();

        // Compras a proveedores
        String sqlProveedores = """
                SELECT
                    COUNT(*) AS total_compras,
                    COALESCE(SUM(total), 0) AS monto_total,
                    COUNT(DISTINCT id_proveedor) AS proveedores_distintos
                FROM compras
                WHERE YEAR(fecha_compra) = ? AND MONTH(fecha_compra) = ?
                AND estado != 'cancelada'
                """;

        // Compras externas
        String sqlExternas = """
                SELECT
                    COUNT(*) AS total_compras_ext,
                    COALESCE(SUM(total), 0) AS monto_total_ext
                FROM compras_externas
                WHERE YEAR(fecha_compra) = ? AND MONTH(fecha_compra) = ?
                """;

        try (Connection con = conexion.getConnectionStatic()) {
            try (PreparedStatement ps = con.prepareStatement(sqlProveedores)) {
                ps.setInt(1, year);
                ps.setInt(2, month);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resultado.put("total_compras_proveedores", rs.getInt("total_compras"));
                        resultado.put("monto_compras_proveedores", rs.getBigDecimal("monto_total"));
                        resultado.put("proveedores_distintos", rs.getInt("proveedores_distintos"));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sqlExternas)) {
                ps.setInt(1, year);
                ps.setInt(2, month);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resultado.put("total_compras_externas", rs.getInt("total_compras_ext"));
                        resultado.put("monto_compras_externas", rs.getBigDecimal("monto_total_ext"));
                    }
                }
            }

            // Calcular totales
            BigDecimal montoProveedores = (BigDecimal) resultado.getOrDefault("monto_compras_proveedores",
                    BigDecimal.ZERO);
            BigDecimal montoExternas = (BigDecimal) resultado.getOrDefault("monto_compras_externas", BigDecimal.ZERO);
            resultado.put("monto_total_compras", montoProveedores.add(montoExternas));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen de compras mensual", e);
        }
        return resultado;
    }

    /**
     * Obtiene análisis de compras por proveedor (ranking)
     */
    public List<Map<String, Object>> getTopProveedores(LocalDate fechaInicio, LocalDate fechaFin, int limite) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    pr.id_proveedor,
                    pr.nombre AS proveedor,
                    pr.ruc,
                    COUNT(c.id_compra) AS total_compras,
                    SUM(c.total) AS monto_total,
                    AVG(c.total) AS promedio_compra,
                    MIN(c.fecha_compra) AS primera_compra,
                    MAX(c.fecha_compra) AS ultima_compra
                FROM proveedores pr
                JOIN compras c ON pr.id_proveedor = c.id_proveedor
                WHERE c.fecha_compra BETWEEN ? AND ?
                AND c.estado != 'cancelada'
                GROUP BY pr.id_proveedor
                ORDER BY monto_total DESC
                LIMIT ?
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setInt(3, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_proveedor", rs.getInt("id_proveedor"));
                    row.put("proveedor", rs.getString("proveedor"));
                    row.put("ruc", rs.getString("ruc"));
                    row.put("total_compras", rs.getInt("total_compras"));
                    row.put("monto_total", rs.getBigDecimal("monto_total"));
                    row.put("promedio_compra", rs.getBigDecimal("promedio_compra"));
                    row.put("primera_compra", rs.getDate("primera_compra"));
                    row.put("ultima_compra", rs.getDate("ultima_compra"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo top proveedores", e);
        }
        return resultados;
    }

    /**
     * Obtiene detalle de una compra específica
     */
    public List<Map<String, Object>> getDetalleCompra(int idCompra) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    cd.id_detalle_compra,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    t.numero AS talla,
                    col.nombre AS color,
                    pv.sku,
                    cd.cantidad,
                    cd.precio_unitario,
                    cd.subtotal
                FROM compra_detalles cd
                JOIN producto_variantes pv ON cd.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE cd.id_compra = ?
                ORDER BY p.nombre, t.numero
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_detalle", rs.getInt("id_detalle_compra"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("sku", rs.getString("sku"));
                    row.put("cantidad", rs.getInt("cantidad"));
                    row.put("precio_unitario", rs.getBigDecimal("precio_unitario"));
                    row.put("subtotal", rs.getBigDecimal("subtotal"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo detalle de compra", e);
        }
        return resultados;
    }

    /**
     * Obtiene lista de proveedores para filtros
     */
    public List<Map<String, Object>> getProveedores() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = "SELECT id_proveedor, nombre, ruc FROM proveedores WHERE activo = 1 ORDER BY nombre";
        try (Connection con = conexion.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_proveedor", rs.getInt("id_proveedor"));
                row.put("nombre", rs.getString("nombre"));
                row.put("ruc", rs.getString("ruc"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo proveedores", e);
        }
        return resultados;
    }
}
