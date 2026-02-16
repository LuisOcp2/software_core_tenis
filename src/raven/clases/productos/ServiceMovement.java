package raven.clases.productos;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelMovement;

//movimientos
public class ServiceMovement {

    public List<ModelMovement> getAll() throws SQLException {
        return getFiltered(null, null, null, null);
    }

    /**
     * Obtiene movimientos filtrados por criterios específicos
     * 
     * @param fechaInicio    Fecha inicial del rango (puede ser null)
     * @param fechaFin       Fecha final del rango (puede ser null)
     * @param tipoMovimiento Tipo de movimiento a filtrar (puede ser null para
     *                       todos)
     * @param nombreProducto Nombre del producto a buscar (búsqueda parcial, puede
     *                       ser null)
     * @return Lista de movimientos que cumplen los criterios
     */
    public List<ModelMovement> getFiltered(LocalDate fechaInicio, LocalDate fechaFin,
            String tipoMovimiento, String nombreProducto) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.id_movimiento, m.id_producto, p.nombre, p.color, p.talla, ");
        sql.append("m.tipo_movimiento, m.cantidad, m.fecha_movimiento, m.id_referencia, ");
        sql.append("m.tipo_referencia, m.id_usuario, m.observaciones ");
        sql.append("FROM inventario_movimientos m ");
        sql.append("INNER JOIN productos p ON m.id_producto = p.id_producto ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (fechaInicio != null) {
            sql.append("AND DATE(m.fecha_movimiento) >= ? ");
            params.add(Date.valueOf(fechaInicio));
        }
        if (fechaFin != null) {
            sql.append("AND DATE(m.fecha_movimiento) <= ? ");
            params.add(Date.valueOf(fechaFin));
        }
        if (tipoMovimiento != null && !tipoMovimiento.isEmpty() && !tipoMovimiento.equals("Todos")) {
            sql.append("AND m.tipo_movimiento = ? ");
            params.add(tipoMovimiento);
        }
        if (nombreProducto != null && !nombreProducto.trim().isEmpty()) {
            sql.append("AND LOWER(p.nombre) LIKE LOWER(?) ");
            params.add("%" + nombreProducto.trim() + "%");
        }

        sql.append("ORDER BY m.fecha_movimiento DESC");

        List<ModelMovement> movimientos = new ArrayList<>();

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement p = con.prepareStatement(sql.toString())) {

            // Establecer parámetros
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    ModelMovement movimiento = mapearMovimiento(r);
                    movimientos.add(movimiento);
                }
            }
        }
        return movimientos;
    }

    /**
     * Obtiene estadísticas de los movimientos filtrados
     */
    public Map<String, Integer> getEstadisticas(LocalDate fechaInicio, LocalDate fechaFin) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT tipo_movimiento, COUNT(*) as cantidad ");
        sql.append("FROM inventario_movimientos m ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (fechaInicio != null) {
            sql.append("AND DATE(fecha_movimiento) >= ? ");
            params.add(Date.valueOf(fechaInicio));
        }
        if (fechaFin != null) {
            sql.append("AND DATE(fecha_movimiento) <= ? ");
            params.add(Date.valueOf(fechaFin));
        }

        sql.append("GROUP BY tipo_movimiento");

        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("entradas", 0);
        stats.put("salidas", 0);
        stats.put("ajustes", 0);

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement p = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }

            try (ResultSet r = p.executeQuery()) {
                int total = 0;
                while (r.next()) {
                    String tipo = r.getString("tipo_movimiento").toLowerCase();
                    int count = r.getInt("cantidad");
                    total += count;

                    if (tipo.contains("entrada")) {
                        stats.put("entradas", stats.get("entradas") + count);
                    } else if (tipo.contains("salida")) {
                        stats.put("salidas", stats.get("salidas") + count);
                    } else if (tipo.contains("ajuste")) {
                        stats.put("ajustes", stats.get("ajustes") + count);
                    }
                }
                stats.put("total", total);
            }
        }
        return stats;
    }

    /**
     * Obtiene los tipos de movimiento disponibles
     */
    public List<String> getTiposMovimiento() throws SQLException {
        String sql = "SELECT DISTINCT tipo_movimiento FROM inventario_movimientos ORDER BY tipo_movimiento";
        List<String> tipos = new ArrayList<>();
        tipos.add("Todos");

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement p = con.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                tipos.add(r.getString("tipo_movimiento"));
            }
        }
        return tipos;
    }

    /**
     * Mapea un ResultSet a un ModelMovement
     */
    private ModelMovement mapearMovimiento(ResultSet r) throws SQLException {
        ModelMovement movimiento = new ModelMovement();
        movimiento.setIdMovimiento(r.getInt("id_movimiento"));
        movimiento.setIdProducto(r.getInt("id_producto"));
        movimiento.setTipoMovimiento(r.getString("tipo_movimiento"));
        movimiento.setNombreProducto(r.getString("nombre"));
        movimiento.setColor(r.getString("color"));
        movimiento.setTalla(r.getString("talla"));

        Timestamp timestamp = r.getTimestamp("fecha_movimiento");
        LocalDateTime fecha = (timestamp != null)
                ? timestamp.toLocalDateTime()
                : LocalDateTime.now();
        movimiento.setFechaMovimiento(fecha);
        movimiento.setCantidad(r.getInt("cantidad"));
        movimiento.setTipoReferencia(r.getString("tipo_referencia"));
        movimiento.setObservaciones(r.getString("observaciones"));
        movimiento.setIdUsuario(r.getInt("id_usuario"));

        return movimiento;
    }
}
