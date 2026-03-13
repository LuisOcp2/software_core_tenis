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
 * Servicio para generar reportes de clientes
 */
public class ServiceReporteClientes {

    private static final Logger LOGGER = Logger.getLogger(ServiceReporteClientes.class.getName());

    public List<Map<String, Object>> getTopClientes(int limite, LocalDate fechaInicio, LocalDate fechaFin) {
        return getTopClientes(limite, fechaInicio, fechaFin, 0);
    }

    public List<Map<String, Object>> getTopClientes(int limite, LocalDate fechaInicio, LocalDate fechaFin,
            int idBodega) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT c.id_cliente, c.nombre, c.dni, c.telefono, c.email, c.puntos_acumulados,
                    COUNT(v.id_venta) AS total_compras, SUM(v.total) AS monto_total, AVG(v.total) AS ticket_promedio
                FROM clientes c JOIN ventas v ON c.id_cliente = v.id_cliente
                WHERE v.estado = 'completada' AND v.fecha_venta BETWEEN ? AND ?
                AND (? = 0 OR c.id_bodega = ?)
                GROUP BY c.id_cliente ORDER BY monto_total DESC LIMIT ?
                """;
        try (Connection con = conexion.getConnectionStatic(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setInt(3, idBodega);
            ps.setInt(4, idBodega);
            ps.setInt(5, limite);
            try (ResultSet rs = ps.executeQuery()) {
                int ranking = 1;
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("ranking", ranking++);
                    row.put("id_cliente", rs.getInt("id_cliente"));
                    row.put("nombre", rs.getString("nombre"));
                    row.put("dni", rs.getString("dni"));
                    row.put("puntos_acumulados", rs.getInt("puntos_acumulados"));
                    row.put("total_compras", rs.getInt("total_compras"));
                    row.put("monto_total", rs.getBigDecimal("monto_total"));
                    row.put("ticket_promedio", rs.getBigDecimal("ticket_promedio"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo top clientes", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getClientesPorPuntos(int minPuntos) {
        return getClientesPorPuntos(minPuntos, 0);
    }

    public List<Map<String, Object>> getClientesPorPuntos(int minPuntos, int idBodega) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT c.id_cliente, c.nombre, c.dni, c.telefono, c.puntos_acumulados, c.fecha_registro
                FROM clientes c WHERE c.activo = 1 AND c.puntos_acumulados >= ?
                AND (? = 0 OR c.id_bodega = ?)
                ORDER BY c.puntos_acumulados DESC
                """;
        try (Connection con = conexion.getConnectionStatic(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, minPuntos);
            ps.setInt(2, idBodega);
            ps.setInt(3, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_cliente", rs.getInt("id_cliente"));
                    row.put("nombre", rs.getString("nombre"));
                    row.put("puntos_acumulados", rs.getInt("puntos_acumulados"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo clientes por puntos", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getClientesNuevos(LocalDate fechaInicio, LocalDate fechaFin) {
        return getClientesNuevos(fechaInicio, fechaFin, 0);
    }

    public List<Map<String, Object>> getClientesNuevos(LocalDate fechaInicio, LocalDate fechaFin, int idBodega) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT c.id_cliente, c.nombre, c.dni, c.telefono, c.fecha_registro
                FROM clientes c WHERE c.activo = 1 AND DATE(c.fecha_registro) BETWEEN ? AND ?
                AND (? = 0 OR c.id_bodega = ?)
                ORDER BY c.fecha_registro DESC
                """;
        try (Connection con = conexion.getConnectionStatic(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaInicio);
            ps.setObject(2, fechaFin);
            ps.setInt(3, idBodega);
            ps.setInt(4, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_cliente", rs.getInt("id_cliente"));
                    row.put("nombre", rs.getString("nombre"));
                    row.put("fecha_registro", rs.getTimestamp("fecha_registro"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo clientes nuevos", e);
        }
        return resultados;
    }

    public Map<String, Object> getResumenClientes() {
        Map<String, Object> resultado = new HashMap<>();
        String sql = "SELECT COUNT(*) AS total, COALESCE(SUM(puntos_acumulados), 0) AS puntos FROM clientes WHERE activo = 1";
        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                resultado.put("total_clientes", rs.getInt("total"));
                resultado.put("total_puntos", rs.getBigDecimal("puntos"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen", e);
        }
        return resultado;
    }
}
