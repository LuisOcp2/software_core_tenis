package raven.clases.productos;

import raven.controlador.principal.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TraspasoCancelService {

    public boolean cancelarTraspaso(String numeroTraspaso, int idUsuario) throws SQLException {
        Connection conn = null;
        try {
            conn = conexion.getInstance().createConnection();
            conn.setAutoCommit(false);

            if (!validarTraspasoParaCancelacion(conn, numeroTraspaso)) {
                throw new SQLException("El traspaso no existe o no está en un estado cancelable");
            }

            List<Map<String, Object>> detalles = obtenerDetallesTraspaso(conn, numeroTraspaso);

            devolverProductosABodegaOrigen(conn, detalles, numeroTraspaso);

            actualizarEstadoTraspaso(conn, numeroTraspaso, idUsuario);

            registrarMovimientoCancelacion(conn, detalles, numeroTraspaso, idUsuario);

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private boolean validarTraspasoParaCancelacion(Connection conn, String numeroTraspaso) throws SQLException {
        String sql = "SELECT estado FROM traspasos WHERE numero_traspaso = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, numeroTraspaso);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("estado");
                    if ("en_transito".equalsIgnoreCase(estado)) {
                        return true;
                    }
                    if ("recibido".equalsIgnoreCase(estado)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private List<Map<String, Object>> obtenerDetallesTraspaso(Connection conn, String numeroTraspaso) throws SQLException {
        List<Map<String, Object>> detalles = new ArrayList<>();
        
        String sql = "SELECT td.id_detalle_traspaso, td.id_producto, td.id_variante, " +
                    "CASE WHEN tr.estado = 'recibido' THEN td.cantidad_recibida ELSE td.cantidad_enviada END AS cantidad, " +
                    "td.tipo, td.unidad_medida, " +
                    "tr.id_bodega_origen, tr.id_bodega_destino, tr.estado " +
                    "FROM traspaso_detalles td " +
                    "JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso " +
                    "WHERE tr.numero_traspaso = ? " +
                    "AND CASE WHEN tr.estado = 'recibido' THEN COALESCE(td.cantidad_recibida,0) ELSE COALESCE(td.cantidad_enviada,0) END > 0";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, numeroTraspaso);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.util.HashMap<String, Object> detalle = new java.util.HashMap<>();
                    detalle.put("id_detalle", rs.getInt("id_detalle_traspaso"));
                    detalle.put("id_producto", rs.getInt("id_producto"));
                    detalle.put("id_variante", rs.getInt("id_variante"));
                    detalle.put("cantidad", rs.getInt("cantidad"));
                    detalle.put("tipo", rs.getString("tipo"));
                    detalle.put("unidad_medida", rs.getString("unidad_medida"));
                    detalle.put("id_bodega_origen", rs.getInt("id_bodega_origen"));
                    detalle.put("id_bodega_destino", rs.getInt("id_bodega_destino"));
                    detalle.put("estado", rs.getString("estado"));
                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    private void devolverProductosABodegaOrigen(Connection conn, List<Map<String, Object>> detalles, String numeroTraspaso) throws SQLException {
        String sql = "INSERT INTO inventario_bodega (id_bodega, id_producto, id_variante, stock_actual, activo) " +
                    "VALUES (?, ?, ?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE stock_actual = stock_actual + VALUES(stock_actual)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> detalle : detalles) {
                int idBodegaOrigen = (Integer) detalle.get("id_bodega_origen");
                int idProducto = (Integer) detalle.get("id_producto");
                int idVariante = (Integer) detalle.get("id_variante");
                int cantidad = (Integer) detalle.get("cantidad");
                
                stmt.setInt(1, idBodegaOrigen);
                stmt.setInt(2, idProducto);
                stmt.setInt(3, idVariante);
                stmt.setInt(4, cantidad);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }

        String sqlDestino = "UPDATE inventario_bodega SET stock_actual = GREATEST(0, stock_actual - ?) " +
                           "WHERE id_bodega = ? AND id_producto = ? AND id_variante = ?";

        try (PreparedStatement stmtDestino = conn.prepareStatement(sqlDestino)) {
            for (Map<String, Object> detalle : detalles) {
                String estado = (String) detalle.get("estado");
                if (estado == null || !"recibido".equalsIgnoreCase(estado)) {
                    continue;
                }
                int idBodegaDestino = (Integer) detalle.get("id_bodega_destino");
                int idProducto = (Integer) detalle.get("id_producto");
                int idVariante = (Integer) detalle.get("id_variante");
                int cantidad = (Integer) detalle.get("cantidad");

                stmtDestino.setInt(1, cantidad);
                stmtDestino.setInt(2, idBodegaDestino);
                stmtDestino.setInt(3, idProducto);
                stmtDestino.setInt(4, idVariante);
                stmtDestino.addBatch();
            }
            stmtDestino.executeBatch();
        }
    }

    private void actualizarEstadoTraspaso(Connection conn, String numeroTraspaso, int idUsuario) throws SQLException {
        String sql = "UPDATE traspasos SET estado = 'cancelado', id_usuario_cancela = ?, " +
                    "fecha_cancelacion = NOW(), observaciones = CONCAT(COALESCE(observaciones, ''), ' - Cancelado por usuario: ', ?) " +
                    "WHERE numero_traspaso = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idUsuario);
            stmt.setString(3, numeroTraspaso);
            stmt.executeUpdate();
        }
    }

    private void registrarMovimientoCancelacion(Connection conn, List<Map<String, Object>> detalles, String numeroTraspaso, int idUsuario) throws SQLException {
        String sql = "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, cantidad, " +
                    "fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones) " +
                    "VALUES (?, ?, ?, ?, NOW(), ?, 'cancelacion_traspaso', ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> detalle : detalles) {
                int idProducto = (Integer) detalle.get("id_producto");
                int idVariante = (Integer) detalle.get("id_variante");
                int cantidad = (Integer) detalle.get("cantidad");
                String tipo = (String) detalle.get("tipo");

                String tipoMovimiento = "entrada par";
                if ("caja".equalsIgnoreCase(tipo)) {
                    tipoMovimiento = "entrada caja";
                }

                stmt.setInt(1, idProducto);
                stmt.setInt(2, idVariante);
                stmt.setString(3, tipoMovimiento);
                stmt.setInt(4, cantidad);
                stmt.setString(5, numeroTraspaso);
                stmt.setInt(6, idUsuario);
                stmt.setString(7, "Cancelación de traspaso: " + numeroTraspaso);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
