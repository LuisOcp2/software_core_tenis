package raven.clases.principal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import raven.controlador.principal.ModelDetalleVenta;

/**
 * Servicio mejorado para gestionar detalles de venta en la base de datos
 */
public class ServiceDetalleVenta {

    /**
     * Crea un nuevo detalle de venta en la base de datos con validaciones
     * mejoradas
     *
     * @param detalle Objeto ModelDetalleVenta con los datos
     * @param con Conexión a la base de datos
     * @return ID del detalle creado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int crearDetalle(ModelDetalleVenta detalle, Connection con) throws SQLException {
        // Validar datos obligatorios
        if (detalle.getVenta() == null || detalle.getVenta().getIdVenta() <= 0) {
            throw new SQLException("Venta no válida");
        }
        if (detalle.getProducto() == null || detalle.getProducto().getProductId() <= 0) {
            throw new SQLException("Producto no válido");
        }
        if (detalle.getCantidad() <= 0) {
            throw new SQLException("Cantidad debe ser mayor que cero");
        }

        String sql = "INSERT INTO venta_detalles "
                + "(id_venta, id_producto, id_variante, cantidad, precio_unitario, descuento, subtotal, tipo_venta, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detalle.getVenta().getIdVenta());
            ps.setInt(2, detalle.getProducto().getProductId());

            // Después (obligatorio)
            if (detalle.getIdVariante() <= 0) {
                throw new SQLException("id_variante obligatorio en venta_detalles");

            }
            ps.setInt(3, detalle.getIdVariante());
            ps.setInt(4, detalle.getCantidad());
            ps.setDouble(5, detalle.getPrecioUnitario());
            ps.setDouble(6, detalle.getDescuento());
            ps.setDouble(7, detalle.getSubtotal());

            // Guardar el tipo de venta
            if (detalle.getTipoVenta() != null && !detalle.getTipoVenta().trim().isEmpty()) {
                ps.setString(8, detalle.getTipoVenta());
            } else {
                ps.setString(8, "normal"); // Valor por defecto
            }

            int rowsAffected = ps.executeUpdate();
            System.out.println("SUCCESS  Detalle insertado - Filas afectadas: " + rowsAffected);

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int idDetalle = rs.getInt(1);
                    detalle.setIdDetalle(idDetalle);
                    System.out.println("SUCCESS  Nuevo detalle creado con ID: " + idDetalle
                            + " - Variante: " + detalle.getIdVariante());
                    return idDetalle;
                } else {
                    throw new SQLException("No se pudo obtener el ID del detalle creado");
                }
            }
        }
    }

    /**
     * Actualiza un detalle de venta existente
     *
     * @param detalle Objeto ModelDetalleVenta con los datos actualizados
     * @param con Conexión a la base de datos
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public void actualizarDetalle(ModelDetalleVenta detalle, Connection con) throws SQLException {
        if (detalle.getIdDetalle() <= 0) {
            throw new SQLException("ID de detalle no válido");
        }

        String sql = "UPDATE venta_detalles SET "
                + "cantidad = ?, "
                + "precio_unitario = ?, "
                + "descuento = ?, "
                + "subtotal = ?, "
                + "id_variante = ?, "
                + "tipo_venta = ? "
                + "WHERE id_detalle = ? AND activo = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, detalle.getCantidad());
            ps.setDouble(2, detalle.getPrecioUnitario());
            ps.setDouble(3, detalle.getDescuento());
            ps.setDouble(4, detalle.getSubtotal());

            // CRÍTICO: Actualizar id_variante correctamente
            if (detalle.getIdVariante() > 0) {
                ps.setInt(5, detalle.getIdVariante());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            // Actualizar tipo de venta
            if (detalle.getTipoVenta() != null && !detalle.getTipoVenta().trim().isEmpty()) {
                ps.setString(6, detalle.getTipoVenta());
            } else {
                ps.setString(6, "normal");
            }

            ps.setInt(7, detalle.getIdDetalle());

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("No se pudo actualizar el detalle. Verifique que exista y esté activo.");
            }

            System.out.println("SUCCESS  Detalle " + detalle.getIdDetalle() + " actualizado - "
                    + "Variante: " + detalle.getIdVariante()
                    + " - Filas afectadas: " + filasAfectadas);
        }
    }

    /**
     * Obtiene un detalle de venta por su ID con toda la información
     *
     * @param idDetalle ID del detalle
     * @param con Conexión a la base de datos
     * @return Objeto ModelDetalleVenta con los datos del detalle
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public ModelDetalleVenta obtenerDetalle(int idDetalle, Connection con) throws SQLException {
        String sql = "SELECT vd.*, p.nombre as producto_nombre "
                + "FROM venta_detalles vd "
                + "LEFT JOIN productos p ON vd.id_producto = p.id_producto "
                + "WHERE vd.id_detalle = ? AND vd.activo = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ModelDetalleVenta detalle = new ModelDetalleVenta();
                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    detalle.setDescuento(rs.getDouble("descuento"));
                    detalle.setSubtotal(rs.getDouble("subtotal"));
                    detalle.setTipoVenta(rs.getString("tipo_venta"));

                    // CRÍTICO: Obtener el ID de la variante correctamente
                    int idVariante = rs.getInt("id_variante");
                    if (!rs.wasNull()) {
                        detalle.setIdVariante(idVariante);
                        System.out.println("SUCCESS  Detalle cargado - ID: " + idDetalle
                                + " - Variante: " + idVariante);
                    } else {
                        detalle.setIdVariante(0);
                        System.out.println("WARNING  Detalle cargado - ID: " + idDetalle
                                + " - Sin variante (NULL)");
                    }

                    return detalle;
                } else {
                    throw new SQLException("No se encontró el detalle con ID: " + idDetalle);
                }
            }
        }
    }

    /**
     * Marca como inactivo un detalle de venta (eliminación lógica)
     *
     * @param idDetalle ID del detalle a eliminar
     * @param con Conexión a la base de datos
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public void eliminarDetalle(int idDetalle, Connection con) throws SQLException {
        String sql = "UPDATE venta_detalles SET activo = 0 WHERE id_detalle = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("No se pudo eliminar el detalle. Verifique que exista.");
            }

            System.out.println("SUCCESS  Detalle " + idDetalle + " marcado como inactivo");
        }
    }

    /**
     * Obtiene la cantidad de un detalle de venta activo
     *
     * @param idDetalle ID del detalle
     * @param con Conexión a la base de datos
     * @return Cantidad del detalle
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int obtenerCantidadDetalle(int idDetalle, Connection con) throws SQLException {
        String sql = "SELECT cantidad FROM venta_detalles WHERE id_detalle = ? AND activo = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cantidad");
                } else {
                    throw new SQLException("No se encontró el detalle con ID: " + idDetalle);
                }
            }
        }
    }

    /**
     * Verifica si un detalle de venta existe y está activo
     *
     * @param idDetalle ID del detalle
     * @param con Conexión a la base de datos
     * @return true si existe y está activo, false en caso contrario
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public boolean existeDetalleActivo(int idDetalle, Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM venta_detalles WHERE id_detalle = ? AND activo = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Obtiene todos los detalles activos de una venta
     *
     * @param idVenta ID de la venta
     * @param con Conexión a la base de datos
     * @return Lista de detalles de la venta
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public java.util.List<ModelDetalleVenta> obtenerDetallesPorVenta(int idVenta, Connection con) throws SQLException {
        java.util.List<ModelDetalleVenta> detalles = new java.util.ArrayList<>();

        String sql = "SELECT vd.*, p.nombre as producto_nombre "
                + "FROM venta_detalles vd "
                + "LEFT JOIN productos p ON vd.id_producto = p.id_producto "
                + "WHERE vd.id_venta = ? AND vd.activo = 1 "
                + "ORDER BY vd.id_detalle";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ModelDetalleVenta detalle = new ModelDetalleVenta();
                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    detalle.setDescuento(rs.getDouble("descuento"));
                    detalle.setSubtotal(rs.getDouble("subtotal"));
                    detalle.setTipoVenta(rs.getString("tipo_venta"));

                    // Obtener el ID de la variante
                    int idVariante = rs.getInt("id_variante");
                    if (!rs.wasNull()) {
                        detalle.setIdVariante(idVariante);
                    } else {
                        detalle.setIdVariante(0);
                    }

                    detalles.add(detalle);
                }
            }
        }

        System.out.println("SUCCESS  Cargados " + detalles.size() + " detalles para venta " + idVenta);
        return detalles;
    }

    /**
     * Valida que los datos del detalle sean correctos antes de guardar
     *
     * @param detalle Detalle a validar
     * @throws SQLException Si los datos no son válidos
     */
    private void validarDetalle(ModelDetalleVenta detalle) throws SQLException {
        if (detalle == null) {
            throw new SQLException("Detalle no puede ser nulo");
        }

        if (detalle.getCantidad() <= 0) {
            throw new SQLException("Cantidad debe ser mayor que cero");
        }

        if (detalle.getPrecioUnitario() < 0) {
            throw new SQLException("Precio unitario no puede ser negativo");
        }

        if (detalle.getDescuento() < 0) {
            throw new SQLException("Descuento no puede ser negativo");
        }

        if (detalle.getSubtotal() < 0) {
            throw new SQLException("Subtotal no puede ser negativo");
        }
    }
}

