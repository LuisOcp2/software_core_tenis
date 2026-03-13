package raven.clases.comercial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.clases.admin.UserSession;

public class ServiceCambioTalla {

    public List<Object[]> buscarParaCambio(String criterio) throws SQLException {
        List<Object[]> resultados = new ArrayList<>();
        // En ventas usamos la bodega actual del usuario (donde se realiza el cambio)
        int currentBodega = UserSession.getInstance().getIdBodegaUsuario();

        // 1. Buscar en VENTAS
        String sqlVenta = "SELECT " +
                "'VENTA' as tipo, " +
                "v.id_venta as referencia, " +
                "v.fecha_venta as fecha, " +
                "c.nombre as cliente_bodega, " +
                "p.nombre as producto, " +
                "t.numero as talla, " +
                "vd.cantidad, " +
                "vd.id_detalle, " +
                "pv.id_variante, " +
                "p.id_producto, " +
                "? as id_bodega_afectada, " +
                "pv.imagen as foto, " +
                "vd.precio_unitario as precio " +
                "FROM ventas v " +
                "JOIN venta_detalles vd ON v.id_venta = vd.id_venta " +
                "JOIN producto_variantes pv ON vd.id_variante = pv.id_variante " +
                "JOIN productos p ON pv.id_producto = p.id_producto " +
                "JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                "WHERE (v.id_venta LIKE ? OR p.codigo_modelo LIKE ? OR pv.ean LIKE ? OR pv.sku LIKE ?) " +
                "AND v.estado = 'COMPLETADA'";

        // 2. Buscar en TRASPASOS
        String sqlTraspaso = "SELECT " +
                "'TRASPASO' as tipo, " +
                "tr.id_traspaso as referencia, " +
                "tr.fecha_solicitud as fecha, " +
                "b.nombre as cliente_bodega, " +
                "p.nombre as producto, " +
                "t.numero as talla, " +
                "td.cantidad_recibida, " +
                "td.id_detalle_traspaso as id_detalle, " +
                "pv.id_variante, " +
                "p.id_producto, " +
                "tr.id_bodega_destino as id_bodega_afectada, " +
                "pv.imagen as foto, " +
                "0 as precio " +
                "FROM traspasos tr " +
                "JOIN traspaso_detalles td ON tr.id_traspaso = td.id_traspaso " +
                "JOIN producto_variantes pv ON td.id_variante = pv.id_variante " +
                "JOIN productos p ON pv.id_producto = p.id_producto " +
                "JOIN tallas t ON pv.id_talla = t.id_talla " +
                "JOIN bodegas b ON tr.id_bodega_destino = b.id_bodega " +
                "WHERE (tr.id_traspaso LIKE ? OR p.codigo_modelo LIKE ? OR pv.ean LIKE ? OR pv.sku LIKE ?) " +
                "AND tr.estado = 'RECIBIDO'";

        String sql = sqlVenta + " UNION ALL " + sqlTraspaso;

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String searchPattern = "%" + criterio + "%";
            
            // Parametros Venta
            ps.setInt(1, currentBodega);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
            ps.setString(5, searchPattern);
            
            // Parametros Traspaso
            ps.setString(6, searchPattern);
            ps.setString(7, searchPattern);
            ps.setString(8, searchPattern);
            ps.setString(9, searchPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.add(new Object[]{
                        rs.getString("tipo"),             // 0
                        rs.getString("referencia"),       // 1
                        rs.getTimestamp("fecha"),         // 2
                        rs.getString("cliente_bodega"),   // 3
                        rs.getString("producto"),         // 4
                        rs.getString("talla"),            // 5
                        rs.getInt("cantidad"),            // 6
                        rs.getInt("id_detalle"),          // 7
                        rs.getInt("id_variante"),         // 8
                        rs.getInt("id_producto"),         // 9
                        rs.getInt("id_bodega_afectada"),  // 10
                        rs.getBytes("foto"),              // 11
                        rs.getBigDecimal("precio")        // 12
                    });
                }
            }
        }
        return resultados;
    }

    public List<Object[]> obtenerVariantesProducto(int idProducto, int idBodega) throws SQLException {
        List<Object[]> variantes = new ArrayList<>();
        // Mostrar stock disponible en la bodega afectada
        String sql = "SELECT pv.id_variante, t.numero as talla, pv.sku, COALESCE(ib.Stock_par, 0) as stock " +
                     "FROM producto_variantes pv " +
                     "JOIN tallas t ON pv.id_talla = t.id_talla " +
                     "LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.id_bodega = ? " +
                     "WHERE pv.id_producto = ? AND pv.disponible = 1 " +
                     "ORDER BY t.numero"; // Ordenar por nombre de talla puede requerir lógica numérica si son números
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    variantes.add(new Object[]{
                        rs.getInt("id_variante"),
                        rs.getString("talla"),
                        rs.getString("sku"),
                        rs.getInt("stock")
                    });
                }
            }
        }
        return variantes;
    }

    public void realizarCambioTallaVenta(int idVenta, int idDetalle, int idVarianteAnterior, int idVarianteNueva, int cantidad, String observaciones, int idBodega) throws SQLException {
        realizarCambioGenerico(idDetalle, idVarianteAnterior, idVarianteNueva, cantidad, observaciones, idBodega, idVenta, "venta", "venta_detalles", "id_detalle");
    }

    public void realizarCambioTallaTraspaso(int idTraspaso, int idDetalle, int idVarianteAnterior, int idVarianteNueva, int cantidad, String observaciones, int idBodega) throws SQLException {
        realizarCambioGenerico(idDetalle, idVarianteAnterior, idVarianteNueva, cantidad, observaciones, idBodega, idTraspaso, "traspaso", "traspaso_detalles", "id_detalle_traspaso");
    }

    private void realizarCambioGenerico(int idDetalle, int idVarianteAnterior, int idVarianteNueva, int cantidad, String observaciones, int idBodega, int idRef, String tipoRef, String tablaDetalle, String colIdDetalle) throws SQLException {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false); // Inicio Transacción

            // 1. Actualizar el detalle (Venta o Traspaso) con la nueva variante
            String sqlUpdate = "UPDATE " + tablaDetalle + " SET id_variante = ? WHERE " + colIdDetalle + " = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                ps.setInt(1, idVarianteNueva);
                ps.setInt(2, idDetalle);
                ps.executeUpdate();
            }

            int idUsuario = UserSession.getInstance().getCurrentUser().getIdUsuario();

            // 2. Devolver al stock la talla anterior (Entrada)
            actualizarStockPar(con, idVarianteAnterior, cantidad, true, idBodega); 
            registrarMovimiento(con, obtenerIdProducto(con, idVarianteAnterior), idVarianteAnterior, cantidad, idUsuario, tipoRef, idRef, "Entrada por cambio: " + observaciones, "entrada par");

            // 3. Sacar del stock la talla nueva (Salida)
            actualizarStockPar(con, idVarianteNueva, cantidad, false, idBodega);
            registrarMovimiento(con, obtenerIdProducto(con, idVarianteNueva), idVarianteNueva, cantidad, idUsuario, tipoRef, idRef, "Salida por cambio: " + observaciones, "salida par");

            con.commit(); // Confirmar Transacción
        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    private void actualizarStockPar(Connection con, int idVariante, int cantidad, boolean esEntrada, int idBodega) throws SQLException {
        String sql = esEntrada ? 
            "UPDATE inventario_bodega SET Stock_par = Stock_par + ? WHERE id_variante = ? AND id_bodega = ?" :
            "UPDATE inventario_bodega SET Stock_par = Stock_par - ? WHERE id_variante = ? AND id_bodega = ?";
            
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idVariante);
            ps.setInt(3, idBodega);
            int rows = ps.executeUpdate();
            if (rows == 0 && esEntrada) {
                // Si no existe y es entrada, insertar
                String insert = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, activo) VALUES (?, ?, ?, 0, 1)";
                try(PreparedStatement psInsert = con.prepareStatement(insert)){
                    psInsert.setInt(1, idBodega);
                    psInsert.setInt(2, idVariante);
                    psInsert.setInt(3, cantidad);
                    psInsert.executeUpdate();
                }
            } else if (rows == 0 && !esEntrada) {
                 throw new SQLException("Sin stock suficiente en bodega " + idBodega + " para variante " + idVariante);
            }
        }
    }

    private void registrarMovimiento(Connection con, int idProducto, int idVariante, int cantidad, int idUsuario, String tipoRef, int idRef, String observaciones, String tipoMov) throws SQLException {
        // Asegurarse de que tipoMov sea uno de los valores permitidos en el ENUM, incluido 'cambio'
        String sql = "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, cantidad, fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones) VALUES (?, ?, ?, ?, CURDATE(), ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idVariante);
            ps.setString(3, tipoMov); 
            ps.setInt(4, cantidad);
            ps.setInt(5, idRef);
            ps.setString(6, tipoRef);
            ps.setInt(7, idUsuario);
            ps.setString(8, observaciones);
            ps.executeUpdate();
        }
    }

    private int obtenerIdProducto(Connection con, int idVariante) throws SQLException {
        String sql = "SELECT id_producto FROM producto_variantes WHERE id_variante = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_producto");
                }
            }
        }
        return 0;
    }
}
