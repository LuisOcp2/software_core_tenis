package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.modelos.CarritoItem;
import raven.modelos.OrdenReserva;
import raven.modelos.OrdenReservaDetalle;

/**
 * DAO para el módulo de Carrito de compras web (Órdenes Web).
 * Gestiona: carrito temporal, conversión a ordenes_reserva, y operaciones de la orden.
 */
public class CarritoDAO {

    private final conexion db;

    public CarritoDAO() {
        this.db = conexion.getInstance();
    }

    // =========================================================================
    // SECCIÓN 1: GESTIÓN DEL CARRITO TEMPORAL (tabla: carrito)
    // =========================================================================

    /**
     * Obtiene todos los ítems del carrito de un usuario con información completa.
     */
    public List<CarritoItem> obtenerCarritoPorUsuario(int idUsuario) throws SQLException {
        List<CarritoItem> items = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT c.id_carrito, c.usuario_id, c.session_id, c.id_producto, " +
                "       c.id_variante, c.id_bodega, c.cantidad, c.precio_unitario, " +
                "       c.fecha_agregado, c.fecha_actualizado, " +
                "       p.nombre AS nombre_producto, p.codigo_modelo, " +
                "       t.numero AS talla, col.nombre AS color, " +
                "       b.nombre AS nombre_bodega, " +
                "       COALESCE(ib.Stockpar, 0) AS stock_disponible, " +
                "       pv.sku " +
                "FROM carrito c " +
                "LEFT JOIN productos p ON c.id_producto = p.id_producto " +
                "LEFT JOIN producto_variantes pv ON c.id_variante = pv.id_variante " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores col ON pv.id_color = col.id_color " +
                "LEFT JOIN bodegas b ON c.id_bodega = b.id_bodega " +
                "LEFT JOIN inventario_bodega ib ON ib.id_variante = c.id_variante " +
                "    AND ib.id_bodega = c.id_bodega AND ib.activo = 1 " +
                "WHERE c.usuario_id = ? " +
                "ORDER BY c.fecha_agregado DESC";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapCarritoItem(rs));
            }
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return items;
    }

    /**
     * Agrega un producto al carrito. Si ya existe la combinación usuario+producto+variante,
     * incrementa la cantidad. Usa INSERT ... ON DUPLICATE KEY UPDATE.
     */
    public boolean agregarAlCarrito(int idUsuario, int idProducto, int idVariante,
                                     int idBodega, int cantidad, double precioUnitario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String sql =
                "INSERT INTO carrito (usuario_id, id_producto, id_variante, id_bodega, cantidad, precio_unitario) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "  cantidad = cantidad + VALUES(cantidad), " +
                "  precio_unitario = VALUES(precio_unitario), " +
                "  fecha_actualizado = CURRENT_TIMESTAMP";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idProducto);
            stmt.setInt(3, idVariante);
            stmt.setInt(4, idBodega);
            stmt.setInt(5, cantidad);
            stmt.setDouble(6, precioUnitario);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    /**
     * Actualiza la cantidad de un ítem en el carrito.
     */
    public boolean actualizarCantidadCarrito(int idCarrito, int nuevaCantidad) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String sql = "UPDATE carrito SET cantidad = ?, fecha_actualizado = CURRENT_TIMESTAMP " +
                         "WHERE id_carrito = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, nuevaCantidad);
            stmt.setInt(2, idCarrito);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    /**
     * Elimina un ítem específico del carrito.
     */
    public boolean eliminarItemCarrito(int idCarrito) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            stmt = con.prepareStatement("DELETE FROM carrito WHERE id_carrito = ?");
            stmt.setInt(1, idCarrito);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    /**
     * Vacía completamente el carrito de un usuario.
     */
    public boolean vaciarCarrito(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            stmt = con.prepareStatement("DELETE FROM carrito WHERE usuario_id = ?");
            stmt.setInt(1, idUsuario);
            return stmt.executeUpdate() >= 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    /**
     * Cuenta los ítems distintos en el carrito de un usuario.
     */
    public int contarItemsCarrito(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            stmt = con.prepareStatement(
                "SELECT COUNT(*) AS total FROM carrito WHERE usuario_id = ?");
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
    }

    /**
     * Obtiene el subtotal del carrito de un usuario.
     */
    public double calcularTotalCarrito(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT COALESCE(SUM(c.cantidad * c.precio_unitario), 0) AS total " +
                "FROM carrito c WHERE c.usuario_id = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
    }

    /**
     * Verifica si hay stock suficiente para todos los ítems del carrito.
     * Retorna lista de ítems con problemas de stock.
     */
    public List<CarritoItem> verificarStockCarrito(int idUsuario) throws SQLException {
        List<CarritoItem> itemsSinStock = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT c.id_carrito, c.id_producto, c.id_variante, c.id_bodega, c.cantidad, " +
                "       c.precio_unitario, p.nombre AS nombre_producto, " +
                "       t.numero AS talla, col.nombre AS color, " +
                "       COALESCE(ib.Stockpar, 0) AS stock_disponible " +
                "FROM carrito c " +
                "LEFT JOIN productos p ON c.id_producto = p.id_producto " +
                "LEFT JOIN producto_variantes pv ON c.id_variante = pv.id_variante " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores col ON pv.id_color = col.id_color " +
                "LEFT JOIN inventario_bodega ib ON ib.id_variante = c.id_variante " +
                "    AND ib.id_bodega = c.id_bodega AND ib.activo = 1 " +
                "WHERE c.usuario_id = ? " +
                "  AND COALESCE(ib.Stockpar, 0) < c.cantidad";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();
            while (rs.next()) {
                itemsSinStock.add(mapCarritoItem(rs));
            }
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return itemsSinStock;
    }

    // =========================================================================
    // SECCIÓN 2: CONVERSIÓN CARRITO → ORDEN DE RESERVA
    // =========================================================================

    /**
     * Convierte el carrito de un usuario en una orden de reserva (orden web).
     * Usa transacción atómica: inserta en ordenes_reserva + ordenes_reserva_detalle,
     * luego vacía el carrito.
     *
     * @return ID de la nueva orden creada, o -1 si falló.
     */
    public int convertirCarritoAOrden(int idUsuario, int idBodega, String metodoPago,
                                       String notas, String direccion, String ciudad,
                                       String departamento) throws SQLException {
        Connection con = null;
        PreparedStatement stmtOrden = null;
        PreparedStatement stmtDetalle = null;
        PreparedStatement stmtCarrito = null;
        ResultSet rs = null;
        int idOrdenGenerada = -1;
        try {
            con = db.createConnection();
            con.setAutoCommit(false);

            // 1. Calcular totales
            List<CarritoItem> items = obtenerCarritoPorUsuario(idUsuario);
            if (items.isEmpty()) {
                con.rollback();
                return -1;
            }
            double subtotal = items.stream().mapToDouble(CarritoItem::getSubtotal).sum();
            double impuestos = subtotal * 0.19; // IVA 19% Colombia
            double total = subtotal + impuestos;

            // 2. Insertar cabecera de la orden
            String sqlOrden =
                "INSERT INTO ordenes_reserva (id_usuario, id_bodega, direccion, ciudad, " +
                "  departamento, subtotal, impuestos, total, metodo_pago, notas, " +
                "  estado, fecha_creacion, fecha_vencimiento) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pendiente', NOW(), " +
                "  DATE_ADD(NOW(), INTERVAL 3 DAY))";
            stmtOrden = con.prepareStatement(sqlOrden, Statement.RETURN_GENERATED_KEYS);
            stmtOrden.setInt(1, idUsuario);
            stmtOrden.setInt(2, idBodega);
            stmtOrden.setString(3, direccion);
            stmtOrden.setString(4, ciudad);
            stmtOrden.setString(5, departamento);
            stmtOrden.setDouble(6, subtotal);
            stmtOrden.setDouble(7, impuestos);
            stmtOrden.setDouble(8, total);
            stmtOrden.setString(9, metodoPago);
            stmtOrden.setString(10, notas);
            stmtOrden.executeUpdate();

            rs = stmtOrden.getGeneratedKeys();
            if (!rs.next()) { con.rollback(); return -1; }
            idOrdenGenerada = rs.getInt(1);

            // 3. Insertar detalles de la orden
            String sqlDetalle =
                "INSERT INTO ordenes_reserva_detalle " +
                "  (id_orden, id_producto, id_variante, id_bodega, cantidad, estado, precio_unitario) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDIENTE', ?)";
            stmtDetalle = con.prepareStatement(sqlDetalle);
            for (CarritoItem item : items) {
                stmtDetalle.setInt(1, idOrdenGenerada);
                stmtDetalle.setInt(2, item.getIdProducto());
                stmtDetalle.setInt(3, item.getIdVariante());
                stmtDetalle.setInt(4, item.getIdBodega() != null ? item.getIdBodega() : idBodega);
                stmtDetalle.setInt(5, item.getCantidad());
                stmtDetalle.setDouble(6, item.getPrecioUnitario());
                stmtDetalle.addBatch();
            }
            stmtDetalle.executeBatch();

            // 4. Vaciar el carrito del usuario
            stmtCarrito = con.prepareStatement("DELETE FROM carrito WHERE usuario_id = ?");
            stmtCarrito.setInt(1, idUsuario);
            stmtCarrito.executeUpdate();

            con.commit();
            return idOrdenGenerada;

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (stmtOrden != null) try { stmtOrden.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (stmtDetalle != null) try { stmtDetalle.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (stmtCarrito != null) try { stmtCarrito.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // =========================================================================
    // SECCIÓN 3: GESTIÓN DE ÓRDENES WEB (ordenes_reserva)
    // =========================================================================

    /**
     * Obtiene todas las órdenes web con información enriquecida.
     * Incluye totales, cantidades y datos de usuario/bodega.
     */
    public List<OrdenReserva> obtenerTodasLasOrdenes(Integer idBodega, String estado,
                                                       java.util.Date fechaInicio,
                                                       java.util.Date fechaFin) throws SQLException {
        List<OrdenReserva> ordenes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT ord.id_orden, ord.id_usuario, ord.id_bodega, " +
                "       ord.fecha_creacion, ord.estado, " +
                "       ord.fecha_retirado, ord.fecha_pagado, ord.fecha_finalizado, " +
                "       ord.subtotal, ord.impuestos, ord.total, " +
                "       ord.metodo_pago, ord.notas, ord.direccion, " +
                "       ord.ciudad, ord.departamento, " +
                "       ord.fecha_vencimiento, " +
                "       u.nombre AS nombre_usuario, " +
                "       b.nombre AS nombre_bodega, " +
                "       COUNT(det.id_detalle) AS cantidad_productos " +
                "FROM ordenes_reserva ord " +
                "LEFT JOIN usuarios u ON ord.id_usuario = u.id_usuario " +
                "LEFT JOIN bodegas b ON ord.id_bodega = b.id_bodega " +
                "LEFT JOIN ordenes_reserva_detalle det ON ord.id_orden = det.id_orden " +
                "WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();
            if (idBodega != null) {
                sql.append("AND ord.id_bodega = ? ");
                params.add(idBodega);
            }
            if (estado != null && !estado.isEmpty() && !"Todos".equals(estado)) {
                sql.append("AND ord.estado = ? ");
                params.add(estado);
            }
            if (fechaInicio != null && fechaFin != null) {
                sql.append("AND DATE(ord.fecha_creacion) BETWEEN ? AND ? ");
                params.add(new java.sql.Date(fechaInicio.getTime()));
                params.add(new java.sql.Date(fechaFin.getTime()));
            }
            sql.append(
                "GROUP BY ord.id_orden, ord.id_usuario, ord.id_bodega, " +
                "         ord.fecha_creacion, ord.estado, ord.fecha_retirado, " +
                "         ord.fecha_pagado, ord.fecha_finalizado, " +
                "         ord.subtotal, ord.impuestos, ord.total, " +
                "         ord.metodo_pago, ord.notas, ord.direccion, " +
                "         ord.ciudad, ord.departamento, ord.fecha_vencimiento, " +
                "         u.nombre, b.nombre " +
                "ORDER BY ord.fecha_creacion DESC"
            );
            stmt = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                ordenes.add(mapOrdenReserva(rs));
            }
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return ordenes;
    }

    /**
     * Obtiene el detalle completo de una orden específica.
     */
    public OrdenReserva obtenerOrdenPorId(int idOrden) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT ord.id_orden, ord.id_usuario, ord.id_bodega, " +
                "       ord.fecha_creacion, ord.estado, " +
                "       ord.fecha_retirado, ord.fecha_pagado, ord.fecha_finalizado, " +
                "       ord.subtotal, ord.impuestos, ord.total, " +
                "       ord.metodo_pago, ord.notas, ord.direccion, " +
                "       ord.ciudad, ord.departamento, ord.fecha_vencimiento, " +
                "       u.nombre AS nombre_usuario, " +
                "       b.nombre AS nombre_bodega, " +
                "       0 AS cantidad_productos " +
                "FROM ordenes_reserva ord " +
                "LEFT JOIN usuarios u ON ord.id_usuario = u.id_usuario " +
                "LEFT JOIN bodegas b ON ord.id_bodega = b.id_bodega " +
                "WHERE ord.id_orden = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            rs = stmt.executeQuery();
            return rs.next() ? mapOrdenReserva(rs) : null;
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
    }

    /**
     * Obtiene los detalles de los productos de una orden con toda la info visual.
     */
    public List<OrdenReservaDetalle> obtenerDetallesOrden(int idOrden) throws SQLException {
        List<OrdenReservaDetalle> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT det.id_detalle, det.id_orden, det.id_producto, det.id_variante, " +
                "       det.cantidad, det.estado AS estado_detalle, " +
                "       det.cantidad_enviada, det.observacion, " +
                "       COALESCE(det.precio_unitario, pv.precio_venta, p.precio_venta) AS precio, " +
                "       (det.cantidad * COALESCE(det.precio_unitario, pv.precio_venta, p.precio_venta)) AS subtotal, " +
                "       p.nombre AS nombre_producto, p.codigo_modelo, " +
                "       t.numero AS talla, col.nombre AS color, pv.sku " +
                "FROM ordenes_reserva_detalle det " +
                "LEFT JOIN productos p ON det.id_producto = p.id_producto " +
                "LEFT JOIN producto_variantes pv ON det.id_variante = pv.id_variante " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores col ON pv.id_color = col.id_color " +
                "WHERE det.id_orden = ? " +
                "ORDER BY det.id_detalle";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            rs = stmt.executeQuery();
            while (rs.next()) {
                OrdenReservaDetalle d = new OrdenReservaDetalle();
                d.setIdDetalle(rs.getInt("id_detalle"));
                d.setIdOrden(rs.getInt("id_orden"));
                d.setIdProducto(rs.getInt("id_producto"));
                d.setIdVariante(rs.getInt("id_variante"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setEstado(rs.getString("estado_detalle"));
                d.setNombreProducto(rs.getString("nombre_producto"));
                d.setCodigoProducto(rs.getString("codigo_modelo"));
                d.setTalla(rs.getString("talla"));
                d.setColor(rs.getString("color"));
                d.setPrecio(rs.getDouble("precio"));
                d.setSubtotal(rs.getDouble("subtotal"));
                detalles.add(d);
            }
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return detalles;
    }

    /**
     * Actualiza el estado de una orden con timestamp automático según el estado.
     */
    public boolean actualizarEstadoOrden(int idOrden, String nuevoEstado) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String campoFecha = "";
            switch (nuevoEstado.toLowerCase()) {
                case "retirado":   campoFecha = ", fecha_retirado = NOW()"; break;
                case "pagado":     campoFecha = ", fecha_pagado = NOW()"; break;
                case "finalizado": campoFecha = ", fecha_finalizado = NOW()"; break;
                default: break;
            }
            String sql = "UPDATE ordenes_reserva SET estado = ?" + campoFecha +
                         " WHERE id_orden = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idOrden);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    /**
     * Cancela una orden guardando el motivo.
     */
    public boolean cancelarOrden(int idOrden, String motivo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String sql = "UPDATE ordenes_reserva SET estado = 'cancelado', " +
                         "  motivo_cancelacion = ? WHERE id_orden = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, motivo);
            stmt.setInt(2, idOrden);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    /**
     * Obtiene estadísticas del dashboard de órdenes web.
     */
    public java.util.Map<String, Object> obtenerEstadisticasOrdenes(
            java.util.Date fechaInicio, java.util.Date fechaFin, Integer idBodega) throws SQLException {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            StringBuilder sql = new StringBuilder(
                "SELECT " +
                "  COUNT(*) AS total_ordenes, " +
                "  SUM(CASE WHEN estado = 'pendiente'  THEN 1 ELSE 0 END) AS pendientes, " +
                "  SUM(CASE WHEN estado = 'retirado'   THEN 1 ELSE 0 END) AS retirados, " +
                "  SUM(CASE WHEN estado = 'pagado'     THEN 1 ELSE 0 END) AS pagados, " +
                "  SUM(CASE WHEN estado = 'finalizado' THEN 1 ELSE 0 END) AS finalizados, " +
                "  SUM(CASE WHEN estado = 'cancelado'  THEN 1 ELSE 0 END) AS cancelados, " +
                "  COALESCE(SUM(CASE WHEN estado IN ('pagado','finalizado') THEN total ELSE 0 END), 0) AS total_ingresos, " +
                "  COALESCE(AVG(CASE WHEN estado IN ('pagado','finalizado') THEN total END), 0) AS ticket_promedio " +
                "FROM ordenes_reserva " +
                "WHERE DATE(fecha_creacion) BETWEEN ? AND ? "
            );
            if (idBodega != null) sql.append("AND id_bodega = ? ");
            stmt = con.prepareStatement(sql.toString());
            stmt.setDate(1, new java.sql.Date(fechaInicio.getTime()));
            stmt.setDate(2, new java.sql.Date(fechaFin.getTime()));
            if (idBodega != null) stmt.setInt(3, idBodega);
            rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("total_ordenes",  rs.getInt("total_ordenes"));
                stats.put("pendientes",     rs.getInt("pendientes"));
                stats.put("retirados",      rs.getInt("retirados"));
                stats.put("pagados",        rs.getInt("pagados"));
                stats.put("finalizados",    rs.getInt("finalizados"));
                stats.put("cancelados",     rs.getInt("cancelados"));
                stats.put("total_ingresos", rs.getDouble("total_ingresos"));
                stats.put("ticket_promedio",rs.getDouble("ticket_promedio"));
            }
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return stats;
    }

    /**
     * Busca órdenes por texto (nombre usuario, número orden, ciudad).
     */
    public List<OrdenReserva> buscarOrdenes(String textoBusqueda, Integer idBodega) throws SQLException {
        List<OrdenReserva> ordenes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String busqueda = "%" + textoBusqueda + "%";
            StringBuilder sql = new StringBuilder(
                "SELECT ord.id_orden, ord.id_usuario, ord.id_bodega, " +
                "       ord.fecha_creacion, ord.estado, " +
                "       ord.fecha_retirado, ord.fecha_pagado, ord.fecha_finalizado, " +
                "       ord.subtotal, ord.impuestos, ord.total, " +
                "       ord.metodo_pago, ord.notas, ord.direccion, " +
                "       ord.ciudad, ord.departamento, ord.fecha_vencimiento, " +
                "       u.nombre AS nombre_usuario, " +
                "       b.nombre AS nombre_bodega, " +
                "       COUNT(det.id_detalle) AS cantidad_productos " +
                "FROM ordenes_reserva ord " +
                "LEFT JOIN usuarios u ON ord.id_usuario = u.id_usuario " +
                "LEFT JOIN bodegas b ON ord.id_bodega = b.id_bodega " +
                "LEFT JOIN ordenes_reserva_detalle det ON ord.id_orden = det.id_orden " +
                "WHERE (u.nombre LIKE ? OR CAST(ord.id_orden AS CHAR) LIKE ? " +
                "       OR ord.ciudad LIKE ? OR ord.direccion LIKE ?) "
            );
            if (idBodega != null) sql.append("AND ord.id_bodega = ? ");
            sql.append(
                "GROUP BY ord.id_orden, ord.id_usuario, ord.id_bodega, " +
                "         ord.fecha_creacion, ord.estado, ord.fecha_retirado, " +
                "         ord.fecha_pagado, ord.fecha_finalizado, " +
                "         ord.subtotal, ord.impuestos, ord.total, " +
                "         ord.metodo_pago, ord.notas, ord.direccion, " +
                "         ord.ciudad, ord.departamento, ord.fecha_vencimiento, " +
                "         u.nombre, b.nombre " +
                "ORDER BY ord.fecha_creacion DESC LIMIT 100"
            );
            stmt = con.prepareStatement(sql.toString());
            stmt.setString(1, busqueda);
            stmt.setString(2, busqueda);
            stmt.setString(3, busqueda);
            stmt.setString(4, busqueda);
            if (idBodega != null) stmt.setInt(5, idBodega);
            rs = stmt.executeQuery();
            while (rs.next()) ordenes.add(mapOrdenReserva(rs));
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return ordenes;
    }

    // =========================================================================
    // SECCIÓN 4: MÉTODOS AUXILIARES
    // =========================================================================

    private CarritoItem mapCarritoItem(ResultSet rs) throws SQLException {
        CarritoItem item = new CarritoItem();
        try { item.setIdCarrito(rs.getInt("id_carrito")); } catch (Exception e) {}
        try { item.setIdProducto(rs.getInt("id_producto")); } catch (Exception e) {}
        try { item.setIdVariante(rs.getInt("id_variante")); } catch (Exception e) {}
        try { item.setIdBodega(rs.getInt("id_bodega")); } catch (Exception e) {}
        try { item.setCantidad(rs.getInt("cantidad")); } catch (Exception e) {}
        try { item.setPrecioUnitario(rs.getDouble("precio_unitario")); } catch (Exception e) {}
        try { item.setNombreProducto(rs.getString("nombre_producto")); } catch (Exception e) {}
        try { item.setCodigoModelo(rs.getString("codigo_modelo")); } catch (Exception e) {}
        try { item.setTalla(rs.getString("talla")); } catch (Exception e) {}
        try { item.setColor(rs.getString("color")); } catch (Exception e) {}
        try { item.setNombreBodega(rs.getString("nombre_bodega")); } catch (Exception e) {}
        try { item.setStockDisponible(rs.getInt("stock_disponible")); } catch (Exception e) {}
        try { item.setSku(rs.getString("sku")); } catch (Exception e) {}
        return item;
    }

    private OrdenReserva mapOrdenReserva(ResultSet rs) throws SQLException {
        OrdenReserva orden = new OrdenReserva();
        orden.setIdOrden(rs.getInt("id_orden"));
        orden.setIdUsuario(rs.getInt("id_usuario"));
        orden.setIdBodega(rs.getInt("id_bodega"));
        orden.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        orden.setEstado(rs.getString("estado"));
        try { orden.setFechaRetirado(rs.getTimestamp("fecha_retirado")); } catch (Exception e) {}
        try { orden.setFechaPagado(rs.getTimestamp("fecha_pagado")); } catch (Exception e) {}
        try { orden.setFechaFinalizado(rs.getTimestamp("fecha_finalizado")); } catch (Exception e) {}
        orden.setNombreUsuario(rs.getString("nombre_usuario"));
        orden.setNombreBodega(rs.getString("nombre_bodega"));
        try { orden.setCantidadProductos(rs.getInt("cantidad_productos")); } catch (Exception e) {}
        // Campos extendidos del modelo enriquecido
        try { orden.setSubtotal(rs.getDouble("subtotal")); } catch (Exception e) {}
        try { orden.setImpuestos(rs.getDouble("impuestos")); } catch (Exception e) {}
        try { orden.setTotal(rs.getDouble("total")); } catch (Exception e) {}
        try { orden.setMetodoPago(rs.getString("metodo_pago")); } catch (Exception e) {}
        try { orden.setDireccion(rs.getString("direccion")); } catch (Exception e) {}
        try { orden.setCiudad(rs.getString("ciudad")); } catch (Exception e) {}
        try { orden.setDepartamento(rs.getString("departamento")); } catch (Exception e) {}
        return orden;
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement stmt, Connection con) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}
