package raven.clases.comercial;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.comercial.ModelClienteDeuda;
import raven.controlador.principal.ModelVenta;
import raven.controlador.principal.conexion;

public class ServiceCuentasPorCobrar {

    /**
     * Obtiene clientes con deuda pendiente.
     * 
     * @param idBodega ID de la bodega para filtrar. Si es null o 0, no aplica
     *                 filtro (admin ve todo).
     * @return Lista de clientes con deuda
     */
    public List<ModelClienteDeuda> obtenerClientesConDeuda(Integer idBodega) {
        List<ModelClienteDeuda> lista = new ArrayList<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // Consulta para obtener clientes con ventas pendientes y su deuda calculada
        // Se asume que ventas con estado 'pendiente' son las que suman deuda.
        // La deuda es: SUM(v.total) - SUM(pagos) para esas ventas.
        String sql = "SELECT \n" +
                "    c.id_cliente, \n" +
                "    c.nombre, \n" +
                "    c.dni, \n" +
                "    c.telefono, \n" +
                "    c.direccion,\n" +
                "    COUNT(v.id_venta) as cant_ventas,\n" +
                "    SUM(v.total) as total_ventas,\n" +
                "    COALESCE(SUM(pagos_totales.total_pagado), 0) as total_pagado\n" +
                "FROM ventas v\n" +
                "JOIN clientes c ON v.id_cliente = c.id_cliente\n" +
                "JOIN usuarios u ON v.id_usuario = u.id_usuario\n" + // JOIN para filtrar por bodega
                "LEFT JOIN (\n" +
                "    SELECT \n" +
                "        vmp.id_venta, \n" +
                "        SUM(vmp.monto) as total_pagado\n" +
                "    FROM venta_medios_pago vmp\n" +
                "    GROUP BY vmp.id_venta\n" +
                ") pagos_totales ON v.id_venta = pagos_totales.id_venta\n" +
                "WHERE v.estado = 'pendiente'\n" +
                (idBodega != null && idBodega > 0 ? "  AND u.id_bodega = ?\n" : "") + // Filtro opcional por bodega
                "GROUP BY c.id_cliente, c.nombre, c.dni, c.telefono, c.direccion\n" +
                "HAVING (SUM(v.total) - COALESCE(SUM(pagos_totales.total_pagado), 0)) > 0.01"; // Filtrar deudas
        // insignificantes

        try {
            con = conexion.getInstance().createConnection();
            ps = con.prepareStatement(sql);

            // Setear parámetro de bodega si aplica
            if (idBodega != null && idBodega > 0) {
                ps.setInt(1, idBodega);
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                ModelClienteDeuda cliente = new ModelClienteDeuda();
                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setNombre(rs.getString("nombre"));
                cliente.setDni(rs.getString("dni"));
                cliente.setTelefono(rs.getString("telefono"));
                cliente.setDireccion(rs.getString("direccion"));

                double totalVentas = rs.getDouble("total_ventas");
                double totalPagado = rs.getDouble("total_pagado");
                BigDecimal deuda = BigDecimal.valueOf(totalVentas).subtract(BigDecimal.valueOf(totalPagado));

                cliente.setDeudaTotal(deuda);
                cliente.setCantidadVentasPendientes(rs.getInt("cant_ventas"));

                lista.add(cliente);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener clientes con deuda: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return lista;
    }

    /**
     * Obtiene las ventas pendientes de un cliente específico.
     * 
     * @param idCliente ID del cliente
     * @param idBodega  ID de la bodega para filtrar. Si es null o 0, no aplica
     *                  filtro.
     * @return Lista de ventas pendientes con sus montos
     */
    public List<Object[]> obtenerVentasPendientesPorCliente(int idCliente, Integer idBodega) {
        List<Object[]> lista = new ArrayList<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        String sql = "SELECT \n" +
                "   v.id_venta,\n" +
                "   v.fecha_venta,\n" +
                "   v.total,\n" +
                "   COALESCE(pagos.monto_pagado, 0) as pagado,\n" +
                "   (v.total - COALESCE(pagos.monto_pagado, 0)) as saldo\n" +
                "FROM ventas v\n" +
                "JOIN usuarios u ON v.id_usuario = u.id_usuario\n" + // JOIN para filtrar por bodega
                "LEFT JOIN (\n" +
                "    SELECT id_venta, SUM(monto) as monto_pagado\n" +
                "    FROM venta_medios_pago\n" +
                "    GROUP BY id_venta\n" +
                ") pagos ON v.id_venta = pagos.id_venta\n" +
                "WHERE v.id_cliente = ? AND v.estado = 'pendiente'\n" +
                (idBodega != null && idBodega > 0 ? "  AND u.id_bodega = ?\n" : "") + // Filtro opcional por bodega
                "  AND (v.total - COALESCE(pagos.monto_pagado, 0)) > 0.01";

        try {
            con = conexion.getInstance().createConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, idCliente);

            // Setear parámetro de bodega si aplica
            if (idBodega != null && idBodega > 0) {
                ps.setInt(2, idBodega);
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(new Object[] {
                        rs.getInt("id_venta"),
                        rs.getTimestamp("fecha_venta"),
                        rs.getDouble("total"),
                        rs.getDouble("pagado"),
                        rs.getDouble("saldo")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ventas pendientes de cliente " + idCliente + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return lista;
    }

    /**
     * Obtiene el resumen global de la deuda pendiente.
     * [0] Total Deuda, [1] Cantidad Clientes con Deuda
     */
    public Object[] obtenerResumenGlobal(Integer idBodega) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Object[] result = new Object[] { 0.0, 0 };

        String sql = "SELECT \n" +
                "    SUM(v.total - COALESCE(pagos.monto_pagado, 0)) as deuda_total,\n" +
                "    COUNT(DISTINCT v.id_cliente) as cant_clientes\n" +
                "FROM ventas v\n" +
                "JOIN usuarios u ON v.id_usuario = u.id_usuario\n" +
                "LEFT JOIN (\n" +
                "    SELECT id_venta, SUM(monto) as monto_pagado\n" +
                "    FROM venta_medios_pago\n" +
                "    GROUP BY id_venta\n" +
                ") pagos ON v.id_venta = pagos.id_venta\n" +
                "WHERE v.estado = 'pendiente'\n" +
                (idBodega != null && idBodega > 0 ? "  AND u.id_bodega = ?\n" : "") +
                "  AND (v.total - COALESCE(pagos.monto_pagado, 0)) > 0.01";

        try {
            con = conexion.getInstance().createConnection();
            ps = con.prepareStatement(sql);
            if (idBodega != null && idBodega > 0) {
                ps.setInt(1, idBodega);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                result[0] = rs.getDouble("deuda_total");
                result[1] = rs.getInt("cant_clientes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (con != null)
                    con.close();
            } catch (SQLException e) {
            }
        }
        return result;
    }

    /**
     * Obtiene el historial de pagos (abonos) realizados para una venta específica.
     * 
     * @param idVenta ID de la venta
     * @return Lista de objetos [id_venta, fecha, tipo_pago, monto, observaciones]
     */
    public List<Object[]> obtenerHistorialPagosPorVenta(int idVenta) {
        List<Object[]> lista = new ArrayList<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        String sql = "SELECT \n" +
                "    vmp.id_venta,\n" +
                "    vmp.fecha_registro,\n" +
                "    vmp.tipo_pago,\n" +
                "    vmp.monto,\n" +
                "    vmp.observaciones\n" +
                "FROM venta_medios_pago vmp\n" +
                "WHERE vmp.id_venta = ? AND vmp.activo = 1\n" +
                "ORDER BY vmp.fecha_registro DESC";

        try {
            con = conexion.getInstance().createConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, idVenta);
            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(new Object[] {
                        rs.getInt("id_venta"),
                        rs.getTimestamp("fecha_registro"),
                        rs.getString("tipo_pago"),
                        rs.getDouble("monto"),
                        rs.getString("observaciones")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (con != null)
                    con.close();
            } catch (SQLException e) {
            }
        }
        return lista;
    }

    /**
     * Registra un abono general distribuyendo el monto en las ventas más antiguas
     * primero (FIFO).
     */
    public void registrarAbonoGeneral(int idCliente, double montoTotal, String tipoPago, String obs, Integer idBodega)
            throws SQLException {
        List<Object[]> ventasPendientes = obtenerVentasPendientesPorCliente(idCliente, idBodega);
        double montoRestante = montoTotal;

        for (Object[] venta : ventasPendientes) {
            if (montoRestante <= 0)
                break;

            int idVenta = (int) venta[0];
            double saldo = (double) venta[4];
            double abono = Math.min(montoRestante, saldo);

            registrarAbono(idVenta, abono, tipoPago, obs + " (Abono Automático)");
            montoRestante -= abono;
        }
    }

    public void registrarAbono(int idVenta, double monto, String tipoPago, String obs) throws SQLException {
        Connection con = null;
        PreparedStatement psInsert = null;
        PreparedStatement psUpdate = null;
        PreparedStatement psCheck = null;
        ResultSet rsCheck = null;

        try {
            con = conexion.getInstance().createConnection();
            con.setAutoCommit(false);

            // 1. Insertar pago
            String sqlInsert = "INSERT INTO venta_medios_pago (id_venta, tipo_pago, monto, observaciones, fecha_registro, activo) VALUES (?, ?, ?, ?, NOW(), 1)";
            psInsert = con.prepareStatement(sqlInsert);
            psInsert.setInt(1, idVenta);
            psInsert.setString(2, tipoPago);
            psInsert.setDouble(3, monto);
            psInsert.setString(4, obs);
            psInsert.executeUpdate();

            // 2. Verificar saldo restante
            String sqlCheck = "SELECT \n" +
                    "   v.total,\n" +
                    "   COALESCE(SUM(vmp.monto), 0) as total_pagado\n" +
                    "FROM ventas v\n" +
                    "LEFT JOIN venta_medios_pago vmp ON v.id_venta = vmp.id_venta\n" +
                    "WHERE v.id_venta = ?\n" +
                    "GROUP BY v.id_venta";

            psCheck = con.prepareStatement(sqlCheck);
            psCheck.setInt(1, idVenta);
            rsCheck = psCheck.executeQuery();

            if (rsCheck.next()) {
                double total = rsCheck.getDouble("total");
                double pagado = rsCheck.getDouble("total_pagado");

                // 3. Si pagado >= total, actualizar estado a completada
                if (pagado >= (total - 0.01)) { // Tolerancia pequeña por decimales
                    String sqlUpdate = "UPDATE ventas SET estado = 'completada' WHERE id_venta = ?";
                    psUpdate = con.prepareStatement(sqlUpdate);
                    psUpdate.setInt(1, idVenta);
                    psUpdate.executeUpdate();
                }
            }

            con.commit();

        } catch (SQLException e) {
            if (con != null)
                con.rollback();
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
            if (psInsert != null)
                psInsert.close();
            if (psUpdate != null)
                psUpdate.close();
            if (psCheck != null)
                psCheck.close();
            if (rsCheck != null)
                rsCheck.close();
        }
    }
}
