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
 *
 * CORRECCIÓN APLICADA:
 * Los nombres de tabla/columna ahora coinciden exactamente con el esquema real:
 *   inventariobodega  (no inventario_bodega)
 *   productovariantes (no producto_variantes)
 *   ordenes_reserva    (con guión bajo — nombre real en el servidor)
 *   ordenes_reserva_detalle (con guión bajo — nombre real en el servidor)
 *   Stockpar          (S mayúscula, p minúscula — tal como está en la BD)
 *   idusuario, idproducto, idvariante, idbodega  (camelCase sin guiones)
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
     * Nombres de tabla/columna corregidos para coincidir con el schema real.
     */
    public List<CarritoItem> obtenerCarritoPorUsuario(int idUsuario) throws SQLException {
        List<CarritoItem> items = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT c.idcarrito, c.usuarioid, c.sessionid, c.idproducto, " +
                "       c.idvariante, c.idbodega, c.cantidad, c.preciounitario, " +
                "       c.fechaagregado, c.fechaactualizado, " +
                "       p.nombre AS nombre_producto, p.codigomodelo AS codigo_modelo, " +
                "       t.numero AS talla, col.nombre AS color, " +
                "       b.nombre AS nombre_bodega, " +
                "       COALESCE(ib.Stockpar, 0) AS stock_disponible, " +
                "       pv.sku " +
                "FROM carrito c " +
                "LEFT JOIN productos p       ON c.idproducto = p.idproducto " +
                "LEFT JOIN productovariantes pv ON c.idvariante = pv.idvariante " +
                "LEFT JOIN tallas t          ON pv.idtalla = t.idtalla " +
                "LEFT JOIN colores col       ON pv.idcolor = col.idcolor " +
                "LEFT JOIN bodegas b         ON c.idbodega = b.idbodega " +
                "LEFT JOIN inventariobodega ib ON ib.idvariante = c.idvariante " +
                "    AND ib.idbodega = c.idbodega AND ib.activo = 1 " +
                "WHERE c.usuarioid = ? " +
                "ORDER BY c.fechaagregado DESC";
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
                "INSERT INTO carrito (usuarioid, idproducto, idvariante, idbodega, cantidad, preciounitario) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "  cantidad = cantidad + VALUES(cantidad), " +
                "  preciounitario = VALUES(preciounitario), " +
                "  fechaactualizado = CURRENT_TIMESTAMP";
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

    public boolean actualizarCantidadCarrito(int idCarrito, int nuevaCantidad) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String sql = "UPDATE carrito SET cantidad = ?, fechaactualizado = CURRENT_TIMESTAMP " +
                         "WHERE idcarrito = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, nuevaCantidad);
            stmt.setInt(2, idCarrito);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    public boolean eliminarItemCarrito(int idCarrito) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            stmt = con.prepareStatement("DELETE FROM carrito WHERE idcarrito = ?");
            stmt.setInt(1, idCarrito);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    public boolean vaciarCarrito(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            stmt = con.prepareStatement("DELETE FROM carrito WHERE usuarioid = ?");
            stmt.setInt(1, idUsuario);
            return stmt.executeUpdate() >= 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    public int contarItemsCarrito(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            stmt = con.prepareStatement(
                "SELECT COUNT(*) AS total FROM carrito WHERE usuarioid = ?");
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
    }

    public double calcularTotalCarrito(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT COALESCE(SUM(c.cantidad * c.preciounitario), 0) AS total " +
                "FROM carrito c WHERE c.usuarioid = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
    }

    public List<CarritoItem> verificarStockCarrito(int idUsuario) throws SQLException {
        List<CarritoItem> itemsSinStock = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT c.idcarrito, c.idproducto, c.idvariante, c.idbodega, c.cantidad, " +
                "       c.preciounitario, p.nombre AS nombre_producto, " +
                "       t.numero AS talla, col.nombre AS color, " +
                "       COALESCE(ib.Stockpar, 0) AS stock_disponible " +
                "FROM carrito c " +
                "LEFT JOIN productos p           ON c.idproducto = p.idproducto " +
                "LEFT JOIN productovariantes pv  ON c.idvariante = pv.idvariante " +
                "LEFT JOIN tallas t              ON pv.idtalla = t.idtalla " +
                "LEFT JOIN colores col           ON pv.idcolor = col.idcolor " +
                "LEFT JOIN inventariobodega ib   ON ib.idvariante = c.idvariante " +
                "    AND ib.idbodega = c.idbodega AND ib.activo = 1 " +
                "WHERE c.usuarioid = ? " +
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

            List<CarritoItem> items = obtenerCarritoPorUsuario(idUsuario);
            if (items.isEmpty()) {
                con.rollback();
                return -1;
            }
            double subtotal = items.stream().mapToDouble(CarritoItem::getSubtotal).sum();
            double impuestos = subtotal * 0.19;
            double total = subtotal + impuestos;

            String sqlOrden =
                "INSERT INTO ordenes_reserva (idusuario, idbodega, direccion, ciudad, " +
                "  departamento, subtotal, impuestos, total, metodopago, notas, " +
                "  estado, fechacreacion, fechavencimiento) " +
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

            String sqlDetalle =
                "INSERT INTO ordenes_reserva_detalle " +
                "  (idorden, idproducto, idvariante, idbodega, cantidad, estado) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDIENTE')";
            stmtDetalle = con.prepareStatement(sqlDetalle);
            for (CarritoItem item : items) {
                stmtDetalle.setInt(1, idOrdenGenerada);
                stmtDetalle.setInt(2, item.getIdProducto());
                stmtDetalle.setInt(3, item.getIdVariante());
                stmtDetalle.setInt(4, item.getIdBodega() != null ? item.getIdBodega() : idBodega);
                stmtDetalle.setInt(5, item.getCantidad());
                stmtDetalle.addBatch();
            }
            stmtDetalle.executeBatch();

            stmtCarrito = con.prepareStatement("DELETE FROM carrito WHERE usuarioid = ?");
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
    // SECCIÓN 3: GESTIÓN DE ÓRDENES WEB
    // =========================================================================

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
                "SELECT ord.idorden, ord.idusuario, ord.idbodega, " +
                "       ord.fechacreacion, ord.estado, " +
                "       ord.fecharetirado, ord.fechapagado, ord.fechafinalizado, " +
                "       ord.subtotal, ord.impuestos, ord.total, " +
                "       ord.metodopago, ord.notas, ord.direccion, " +
                "       ord.ciudad, ord.departamento, " +
                "       ord.fechavencimiento, " +
                "       u.nombre AS nombre_usuario, " +
                "       b.nombre AS nombre_bodega, " +
                "       COUNT(det.iddetalle) AS cantidad_productos " +
                "FROM ordenes_reserva ord " +
                "LEFT JOIN usuarios u    ON ord.idusuario = u.idusuario " +
                "LEFT JOIN bodegas b     ON ord.idbodega  = b.idbodega " +
                "LEFT JOIN ordenes_reserva_detalle det ON ord.idorden = det.idorden " +
                "WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();
            if (idBodega != null) {
                sql.append("AND ord.idbodega = ? ");
                params.add(idBodega);
            }
            if (estado != null && !estado.isEmpty() && !"Todos".equals(estado)) {
                sql.append("AND ord.estado = ? ");
                params.add(estado);
            }
            if (fechaInicio != null && fechaFin != null) {
                sql.append("AND DATE(ord.fechacreacion) BETWEEN ? AND ? ");
                params.add(new java.sql.Date(fechaInicio.getTime()));
                params.add(new java.sql.Date(fechaFin.getTime()));
            }
            sql.append(
                "GROUP BY ord.idorden, ord.idusuario, ord.idbodega, " +
                "         ord.fechacreacion, ord.estado, ord.fecharetirado, " +
                "         ord.fechapagado, ord.fechafinalizado, " +
                "         ord.subtotal, ord.impuestos, ord.total, " +
                "         ord.metodopago, ord.notas, ord.direccion, " +
                "         ord.ciudad, ord.departamento, ord.fechavencimiento, " +
                "         u.nombre, b.nombre " +
                "ORDER BY ord.fechacreacion DESC"
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

    public OrdenReserva obtenerOrdenPorId(int idOrden) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT ord.idorden, ord.idusuario, ord.idbodega, " +
                "       ord.fechacreacion, ord.estado, " +
                "       ord.fecharetirado, ord.fechapagado, ord.fechafinalizado, " +
                "       ord.subtotal, ord.impuestos, ord.total, " +
                "       ord.metodopago, ord.notas, ord.direccion, " +
                "       ord.ciudad, ord.departamento, ord.fechavencimiento, " +
                "       u.nombre AS nombre_usuario, " +
                "       b.nombre AS nombre_bodega, " +
                "       0 AS cantidad_productos " +
                "FROM ordenes_reserva ord " +
                "LEFT JOIN usuarios u ON ord.idusuario = u.idusuario " +
                "LEFT JOIN bodegas b  ON ord.idbodega  = b.idbodega " +
                "WHERE ord.idorden = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            rs = stmt.executeQuery();
            return rs.next() ? mapOrdenReserva(rs) : null;
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
    }

    public List<OrdenReservaDetalle> obtenerDetallesOrden(int idOrden) throws SQLException {
        List<OrdenReservaDetalle> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String sql =
                "SELECT det.iddetalle, det.idorden, det.idproducto, det.idvariante, " +
                "       det.cantidad, det.estado AS estado_detalle, " +
                "       det.cantidadenviada, det.observacion, " +
                "       COALESCE(pv.precioventa, p.precioventa) AS precio, " +
                "       (det.cantidad * COALESCE(pv.precioventa, p.precioventa)) AS subtotal, " +
                "       p.nombre AS nombre_producto, p.codigomodelo AS codigo_modelo, " +
                "       t.numero AS talla, col.nombre AS color, pv.sku " +
                "FROM ordenes_reserva_detalle det " +
                "LEFT JOIN productos p           ON det.idproducto = p.idproducto " +
                "LEFT JOIN productovariantes pv  ON det.idvariante = pv.idvariante " +
                "LEFT JOIN tallas t              ON pv.idtalla = t.idtalla " +
                "LEFT JOIN colores col           ON pv.idcolor = col.idcolor " +
                "WHERE det.idorden = ? " +
                "ORDER BY det.iddetalle";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            rs = stmt.executeQuery();
            while (rs.next()) {
                OrdenReservaDetalle d = new OrdenReservaDetalle();
                d.setIdDetalle(rs.getInt("iddetalle"));
                d.setIdOrden(rs.getInt("idorden"));
                d.setIdProducto(rs.getInt("idproducto"));
                d.setIdVariante(rs.getInt("idvariante"));
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

    public boolean actualizarEstadoOrden(int idOrden, String nuevoEstado) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String campoFecha = "";
            switch (nuevoEstado.toLowerCase()) {
                case "retirado":   campoFecha = ", fecharetirado = NOW()"; break;
                case "pagado":     campoFecha = ", fechapagado = NOW()"; break;
                case "finalizado": campoFecha = ", fechafinalizado = NOW()"; break;
                default: break;
            }
            String sql = "UPDATE ordenes_reserva SET estado = ?" + campoFecha +
                         " WHERE idorden = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idOrden);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

    public boolean cancelarOrden(int idOrden, String motivo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = db.createConnection();
            String sql = "UPDATE ordenes_reserva SET estado = 'cancelado' WHERE idorden = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            return stmt.executeUpdate() > 0;
        } finally {
            cerrarRecursos(null, stmt, con);
        }
    }

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
                "WHERE DATE(fechacreacion) BETWEEN ? AND ? "
            );
            if (idBodega != null) sql.append("AND idbodega = ? ");
            stmt = con.prepareStatement(sql.toString());
            stmt.setDate(1, new java.sql.Date(fechaInicio.getTime()));
            stmt.setDate(2, new java.sql.Date(fechaFin.getTime()));
            if (idBodega != null) stmt.setInt(3, idBodega);
            rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("total_ordenes",   rs.getInt("total_ordenes"));
                stats.put("pendientes",      rs.getInt("pendientes"));
                stats.put("retirados",       rs.getInt("retirados"));
                stats.put("pagados",         rs.getInt("pagados"));
                stats.put("finalizados",     rs.getInt("finalizados"));
                stats.put("cancelados",      rs.getInt("cancelados"));
                stats.put("total_ingresos",  rs.getDouble("total_ingresos"));
                stats.put("ticket_promedio", rs.getDouble("ticket_promedio"));
            }
        } finally {
            cerrarRecursos(rs, stmt, con);
        }
        return stats;
    }

    public List<OrdenReserva> buscarOrdenes(String textoBusqueda, Integer idBodega) throws SQLException {
        List<OrdenReserva> ordenes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = db.createConnection();
            String busqueda = "%" + textoBusqueda + "%";
            StringBuilder sql = new StringBuilder(
                "SELECT ord.idorden, ord.idusuario, ord.idbodega, " +
                "       ord.fechacreacion, ord.estado, " +
                "       ord.fecharetirado, ord.fechapagado, ord.fechafinalizado, " +
                "       ord.subtotal, ord.impuestos, ord.total, " +
                "       ord.metodopago, ord.notas, ord.direccion, " +
                "       ord.ciudad, ord.departamento, ord.fechavencimiento, " +
                "       u.nombre AS nombre_usuario, " +
                "       b.nombre AS nombre_bodega, " +
                "       COUNT(det.iddetalle) AS cantidad_productos " +
                "FROM ordenes_reserva ord " +
                "LEFT JOIN usuarios u ON ord.idusuario = u.idusuario " +
                "LEFT JOIN bodegas b  ON ord.idbodega  = b.idbodega " +
                "LEFT JOIN ordenes_reserva_detalle det ON ord.idorden = det.idorden " +
                "WHERE (u.nombre LIKE ? OR CAST(ord.idorden AS CHAR) LIKE ? " +
                "       OR ord.ciudad LIKE ? OR ord.direccion LIKE ?) "
            );
            if (idBodega != null) sql.append("AND ord.idbodega = ? ");
            sql.append(
                "GROUP BY ord.idorden, ord.idusuario, ord.idbodega, " +
                "         ord.fechacreacion, ord.estado, ord.fecharetirado, " +
                "         ord.fechapagado, ord.fechafinalizado, " +
                "         ord.subtotal, ord.impuestos, ord.total, " +
                "         ord.metodopago, ord.notas, ord.direccion, " +
                "         ord.ciudad, ord.departamento, ord.fechavencimiento, " +
                "         u.nombre, b.nombre " +
                "ORDER BY ord.fechacreacion DESC LIMIT 100"
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
    // MAPPERS
    // =========================================================================

    private CarritoItem mapCarritoItem(ResultSet rs) throws SQLException {
        CarritoItem item = new CarritoItem();
        try { item.setIdCarrito(rs.getInt("idcarrito")); }    catch (Exception e) {}
        try { item.setIdProducto(rs.getInt("idproducto")); }  catch (Exception e) {}
        try { item.setIdVariante(rs.getInt("idvariante")); }  catch (Exception e) {}
        try { item.setIdBodega(rs.getInt("idbodega")); }      catch (Exception e) {}
        try { item.setCantidad(rs.getInt("cantidad")); }      catch (Exception e) {}
        try { item.setPrecioUnitario(rs.getDouble("preciounitario")); } catch (Exception e) {}
        try { item.setNombreProducto(rs.getString("nombre_producto")); } catch (Exception e) {}
        try { item.setCodigoModelo(rs.getString("codigo_modelo")); }    catch (Exception e) {}
        try { item.setTalla(rs.getString("talla")); }         catch (Exception e) {}
        try { item.setColor(rs.getString("color")); }         catch (Exception e) {}
        try { item.setNombreBodega(rs.getString("nombre_bodega")); }   catch (Exception e) {}
        try { item.setStockDisponible(rs.getInt("stock_disponible")); } catch (Exception e) {}
        try { item.setSku(rs.getString("sku")); }             catch (Exception e) {}
        return item;
    }

    private OrdenReserva mapOrdenReserva(ResultSet rs) throws SQLException {
        OrdenReserva orden = new OrdenReserva();
        orden.setIdOrden(rs.getInt("idorden"));
        orden.setIdUsuario(rs.getInt("idusuario"));
        orden.setIdBodega(rs.getInt("idbodega"));
        orden.setFechaCreacion(rs.getTimestamp("fechacreacion"));
        orden.setEstado(rs.getString("estado"));
        try { orden.setFechaRetirado(rs.getTimestamp("fecharetirado")); }   catch (Exception e) {}
        try { orden.setFechaPagado(rs.getTimestamp("fechapagado")); }       catch (Exception e) {}
        try { orden.setFechaFinalizado(rs.getTimestamp("fechafinalizado")); } catch (Exception e) {}
        orden.setNombreUsuario(rs.getString("nombre_usuario"));
        orden.setNombreBodega(rs.getString("nombre_bodega"));
        try { orden.setCantidadProductos(rs.getInt("cantidad_productos")); } catch (Exception e) {}
        try { orden.setSubtotal(rs.getDouble("subtotal")); }    catch (Exception e) {}
        try { orden.setImpuestos(rs.getDouble("impuestos")); }  catch (Exception e) {}
        try { orden.setTotal(rs.getDouble("total")); }          catch (Exception e) {}
        try { orden.setMetodoPago(rs.getString("metodopago")); } catch (Exception e) {}
        try { orden.setDireccion(rs.getString("direccion")); }  catch (Exception e) {}
        try { orden.setCiudad(rs.getString("ciudad")); }        catch (Exception e) {}
        try { orden.setDepartamento(rs.getString("departamento")); } catch (Exception e) {}
        return orden;
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement stmt, Connection con) {
        try { if (rs != null)   rs.close(); }   catch (SQLException e) { e.printStackTrace(); }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (con != null)  con.close(); }  catch (SQLException e) { e.printStackTrace(); }
    }
}
