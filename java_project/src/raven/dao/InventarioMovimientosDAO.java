package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.application.form.productos.dto.MovimientoItem;
import raven.controlador.principal.conexion;

public class InventarioMovimientosDAO {

    public List<MovimientoItem> listarPorVariante(int idVariante) {
        List<MovimientoItem> lista = new ArrayList<>();
        // Note: Using COALESCE/LEFT JOIN to handle cases where user might be null or
        // deleted
        String sql = "SELECT m.id_auditoria as id_movimiento, m.fecha_evento as fecha_movimiento, " +
                "m.tipo_evento as tipo_movimiento, m.cantidad, 0 as cantidad_pares, " +
                "m.tipo_referencia, m.observaciones, u.username " +
                "FROM auditoria_trazabilidad m " +
                "LEFT JOIN usuarios u ON m.id_usuario = u.id_usuario " +
                "WHERE m.id_variante = ? " +
                "ORDER BY m.fecha_evento DESC";

        try (Connection con = conexion.getInstance().getConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idVariante);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    MovimientoItem item = new MovimientoItem();
                    item.setIdMovimiento(rs.getInt("id_movimiento"));
                    item.setFecha(rs.getTimestamp("fecha_movimiento"));
                    item.setTipoMovimiento(rs.getString("tipo_movimiento"));
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setCantidadPares(rs.getInt("cantidad_pares"));
                    item.setTipoReferencia(rs.getString("tipo_referencia"));

                    String user = rs.getString("username");
                    item.setUsuario(user != null ? user : "-");

                    item.setObservacion(rs.getString("observaciones"));
                    lista.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error listando movimientos: " + e.getMessage());
        }
        return lista;
    }
}
