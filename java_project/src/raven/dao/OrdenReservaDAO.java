package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.modelos.OrdenReserva;
import raven.modelos.OrdenReservaDetalle;

/**
 * DAO para operaciones relacionadas con órdenes de reserva.
 */
public class OrdenReservaDAO {
    
    private final conexion db;
    
    public OrdenReservaDAO() {
        this.db = conexion.getInstance();
    }
    
    /**
     * Obtiene todas las órdenes de reserva con información relacionada.
     * @return Lista de órdenes de reserva
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerTodasLasOrdenes() throws SQLException {
        return obtenerTodasLasOrdenes(null);
    }

    /**
     * Obtiene todas las órdenes de reserva con información relacionada, filtrada opcionalmente por bodega.
     * @param idBodega ID de la bodega para filtrar (null para todas)
     * @return Lista de órdenes de reserva
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerTodasLasOrdenes(Integer idBodega) throws SQLException {
        List<OrdenReserva> ordenes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            sql.append("    ord_res.id_orden, ");
            sql.append("    ord_res.id_usuario, ");
            sql.append("    ord_res.id_bodega, ");
            sql.append("    ord_res.fecha_creacion, ");
            sql.append("    ord_res.estado, ");
            sql.append("    ord_res.fecha_retirado, ");
            sql.append("    ord_res.fecha_pagado, ");
            sql.append("    ord_res.fecha_finalizado, ");
            sql.append("    u.nombre as nombre_usuario, ");
            sql.append("    b.nombre as nombre_bodega, ");
            sql.append("    COUNT(ord.id_detalle) as cantidad_productos ");
            sql.append("FROM ordenes_reserva ord_res ");
            sql.append("LEFT JOIN usuarios u ON ord_res.id_usuario = u.id_usuario ");
            sql.append("LEFT JOIN bodegas b ON ord_res.id_bodega = b.id_bodega ");
            sql.append("LEFT JOIN ordenes_reserva_detalle ord ON ord_res.id_orden = ord.id_orden ");
            
            if (idBodega != null) {
                sql.append("WHERE ord_res.id_bodega = ? ");
            }
            
            sql.append("GROUP BY ord_res.id_orden, ord_res.id_usuario, ord_res.id_bodega, ord_res.fecha_creacion, ");
            sql.append("         ord_res.estado, ord_res.fecha_retirado, ord_res.fecha_pagado, ord_res.fecha_finalizado, ");
            sql.append("         u.nombre, b.nombre ");
            sql.append("ORDER BY ord_res.fecha_creacion DESC");
            
            stmt = con.prepareStatement(sql.toString());
            
            if (idBodega != null) {
                stmt.setInt(1, idBodega);
            }
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrdenReserva orden = new OrdenReserva();
                orden.setIdOrden(rs.getInt("id_orden"));
                orden.setIdUsuario(rs.getInt("id_usuario"));
                orden.setIdBodega(rs.getInt("id_bodega"));
                orden.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                orden.setEstado(rs.getString("estado"));
                orden.setFechaRetirado(rs.getTimestamp("fecha_retirado"));
                orden.setFechaPagado(rs.getTimestamp("fecha_pagado"));
                orden.setFechaFinalizado(rs.getTimestamp("fecha_finalizado"));
                orden.setNombreUsuario(rs.getString("nombre_usuario"));
                orden.setNombreBodega(rs.getString("nombre_bodega"));
                orden.setCantidadProductos(rs.getInt("cantidad_productos"));
                
                ordenes.add(orden);
            }
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        
        return ordenes;
    }
    
    /**
     * Obtiene las órdenes de reserva filtradas por estado.
     * @param estado Estado de la orden
     * @return Lista de órdenes de reserva
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReserva> obtenerOrdenesPorEstado(String estado) throws SQLException {
        List<OrdenReserva> ordenes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT\n" + 
                "    ord_res.id_orden,\n" + 
                "    ord_res.id_usuario,\n" + 
                "    ord_res.id_bodega,\n" + 
                "    ord_res.fecha_creacion,\n" + 
                "    ord_res.estado,\n" + 
                "    ord_res.fecha_retirado,\n" + 
                "    ord_res.fecha_pagado,\n" + 
                "    ord_res.fecha_finalizado,\n" + 
                "    u.nombre as nombre_usuario,\n" + 
                "    b.nombre as nombre_bodega,\n" + 
                "    COUNT(ord.id_detalle) as cantidad_productos\n" + 
                "FROM ordenes_reserva ord_res\n" + 
                "LEFT JOIN usuarios u ON ord_res.id_usuario = u.id_usuario\n" + 
                "LEFT JOIN bodegas b ON ord_res.id_bodega = b.id_bodega\n" + 
                "LEFT JOIN ordenes_reserva_detalle ord ON ord_res.id_orden = ord.id_orden\n" + 
                "WHERE ord_res.estado = ?\n" + 
                "GROUP BY ord_res.id_orden, ord_res.id_usuario, ord_res.id_bodega, ord_res.fecha_creacion,\n" + 
                "         ord_res.estado, ord_res.fecha_retirado, ord_res.fecha_pagado, ord_res.fecha_finalizado,\n" + 
                "         u.nombre, b.nombre\n" + 
                "ORDER BY ord_res.fecha_creacion DESC\n";
            
            stmt = con.prepareStatement(sql);
            stmt.setString(1, estado);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrdenReserva orden = new OrdenReserva();
                orden.setIdOrden(rs.getInt("id_orden"));
                orden.setIdUsuario(rs.getInt("id_usuario"));
                orden.setIdBodega(rs.getInt("id_bodega"));
                orden.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                orden.setEstado(rs.getString("estado"));
                orden.setFechaRetirado(rs.getTimestamp("fecha_retirado"));
                orden.setFechaPagado(rs.getTimestamp("fecha_pagado"));
                orden.setFechaFinalizado(rs.getTimestamp("fecha_finalizado"));
                orden.setNombreUsuario(rs.getString("nombre_usuario"));
                orden.setNombreBodega(rs.getString("nombre_bodega"));
                orden.setCantidadProductos(rs.getInt("cantidad_productos"));
                
                ordenes.add(orden);
            }
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        
        return ordenes;
    }
    
    /**
     * Filtra las órdenes de reserva según múltiples criterios.
     */
    public List<OrdenReserva> filtrarOrdenes(java.util.Date fechaInicio, java.util.Date fechaFin, Integer idUsuario, String estado, Integer idBodega) throws SQLException {
        List<OrdenReserva> ordenes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            sql.append("    ord_res.id_orden, ");
            sql.append("    ord_res.id_usuario, ");
            sql.append("    ord_res.id_bodega, ");
            sql.append("    ord_res.fecha_creacion, ");
            sql.append("    ord_res.estado, ");
            sql.append("    ord_res.fecha_retirado, ");
            sql.append("    ord_res.fecha_pagado, ");
            sql.append("    ord_res.fecha_finalizado, ");
            sql.append("    u.nombre as nombre_usuario, ");
            sql.append("    b.nombre as nombre_bodega, ");
            sql.append("    COUNT(ord.id_detalle) as cantidad_productos ");
            sql.append("FROM ordenes_reserva ord_res ");
            sql.append("LEFT JOIN usuarios u ON ord_res.id_usuario = u.id_usuario ");
            sql.append("LEFT JOIN bodegas b ON ord_res.id_bodega = b.id_bodega ");
            sql.append("LEFT JOIN ordenes_reserva_detalle ord ON ord_res.id_orden = ord.id_orden ");
            sql.append("WHERE 1=1 ");

            if (fechaInicio != null && fechaFin != null) {
                sql.append("AND DATE(ord_res.fecha_creacion) BETWEEN ? AND ? ");
            }
            if (idUsuario != null && idUsuario > 0) {
                sql.append("AND ord_res.id_usuario = ? ");
            }
            if (estado != null && !estado.isEmpty() && !"Todos".equals(estado)) {
                sql.append("AND ord_res.estado = ? ");
            }
            if (idBodega != null) {
                sql.append("AND ord_res.id_bodega = ? ");
            }
            
            sql.append("GROUP BY ord_res.id_orden, ord_res.id_usuario, ord_res.id_bodega, ord_res.fecha_creacion, ");
            sql.append("         ord_res.estado, ord_res.fecha_retirado, ord_res.fecha_pagado, ord_res.fecha_finalizado, ");
            sql.append("         u.nombre, b.nombre ");
            sql.append("ORDER BY ord_res.fecha_creacion DESC");
            
            stmt = con.prepareStatement(sql.toString());
            
            int index = 1;
            if (fechaInicio != null && fechaFin != null) {
                stmt.setDate(index++, new java.sql.Date(fechaInicio.getTime()));
                stmt.setDate(index++, new java.sql.Date(fechaFin.getTime()));
            }
            if (idUsuario != null && idUsuario > 0) {
                stmt.setInt(index++, idUsuario);
            }
            if (estado != null && !estado.isEmpty() && !"Todos".equals(estado)) {
                stmt.setString(index++, estado);
            }
            if (idBodega != null) {
                stmt.setInt(index++, idBodega);
            }
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrdenReserva orden = new OrdenReserva();
                orden.setIdOrden(rs.getInt("id_orden"));
                orden.setIdUsuario(rs.getInt("id_usuario"));
                orden.setIdBodega(rs.getInt("id_bodega"));
                orden.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                orden.setEstado(rs.getString("estado"));
                orden.setFechaRetirado(rs.getTimestamp("fecha_retirado"));
                orden.setFechaPagado(rs.getTimestamp("fecha_pagado"));
                orden.setFechaFinalizado(rs.getTimestamp("fecha_finalizado"));
                orden.setNombreUsuario(rs.getString("nombre_usuario"));
                orden.setNombreBodega(rs.getString("nombre_bodega"));
                orden.setCantidadProductos(rs.getInt("cantidad_productos"));
                
                ordenes.add(orden);
            }
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        
        return ordenes;
    }

    /**
     * Obtiene una orden de reserva por su ID.
     * @param idOrden ID de la orden
     * @return Orden de reserva o null si no existe
     * @throws SQLException Si ocurre un error de base de datos
     */
    public OrdenReserva obtenerOrdenPorId(int idOrden) throws SQLException {
        OrdenReserva orden = null;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT\n" + 
                "    ord_res.id_orden,\n" + 
                "    ord_res.id_usuario,\n" + 
                "    ord_res.id_bodega,\n" + 
                "    ord_res.fecha_creacion,\n" + 
                "    ord_res.estado,\n" + 
                "    ord_res.fecha_retirado,\n" + 
                "    ord_res.fecha_pagado,\n" + 
                "    ord_res.fecha_finalizado,\n" + 
                "    u.nombre as nombre_usuario,\n" + 
                "    b.nombre as nombre_bodega\n" + 
                "FROM ordenes_reserva ord_res\n" + 
                "LEFT JOIN usuarios u ON ord_res.id_usuario = u.id_usuario\n" + 
                "LEFT JOIN bodegas b ON ord_res.id_bodega = b.id_bodega\n" + 
                "WHERE ord_res.id_orden = ?\n";
            
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                orden = new OrdenReserva();
                orden.setIdOrden(rs.getInt("id_orden"));
                orden.setIdUsuario(rs.getInt("id_usuario"));
                orden.setIdBodega(rs.getInt("id_bodega"));
                orden.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                orden.setEstado(rs.getString("estado"));
                orden.setFechaRetirado(rs.getTimestamp("fecha_retirado"));
                orden.setFechaPagado(rs.getTimestamp("fecha_pagado"));
                orden.setFechaFinalizado(rs.getTimestamp("fecha_finalizado"));
                orden.setNombreUsuario(rs.getString("nombre_usuario"));
                orden.setNombreBodega(rs.getString("nombre_bodega"));
            }
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        
        return orden;
    }
    
    /**
     * Obtiene los detalles de una orden de reserva.
     * @param idOrden ID de la orden
     * @return Lista de detalles de la orden
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<OrdenReservaDetalle> obtenerDetallesOrden(int idOrden) throws SQLException {
        List<OrdenReservaDetalle> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT\n" + 
                "    ord.id_detalle,\n" + 
                "    ord.id_orden,\n" + 
                "    ord.id_producto,\n" + 
                "    ord.id_variante,\n" + 
                "    ord.cantidad,\n" + 
                "    ord.estado as estado_detalle,\n" + 
                "    p.nombre as nombre_producto,\n" + 
                "    p.codigo_modelo as codigo_producto,\n" + 
                "    t.numero as talla,\n" + 
                "    c.nombre as color,\n" + 
                "    pv.precio_venta as precio,\n" + 
                "    (ord.cantidad * pv.precio_venta) as subtotal\n" + 
                "FROM ordenes_reserva_detalle ord\n" + 
                "LEFT JOIN productos p ON ord.id_producto = p.id_producto\n" + 
                "LEFT JOIN producto_variantes pv ON ord.id_variante = pv.id_variante\n" + 
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla\n" + 
                "LEFT JOIN colores c ON pv.id_color = c.id_color\n" + 
                "WHERE ord.id_orden = ?\n" + 
                "ORDER BY ord.id_detalle\n";
            
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOrden);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrdenReservaDetalle detalle = new OrdenReservaDetalle();
                detalle.setIdDetalle(rs.getInt("id_detalle"));
                detalle.setIdOrden(rs.getInt("id_orden"));
                detalle.setIdProducto(rs.getInt("id_producto"));
                detalle.setIdVariante(rs.getInt("id_variante"));
                detalle.setCantidad(rs.getInt("cantidad"));
                detalle.setEstado(rs.getString("estado_detalle"));
                detalle.setNombreProducto(rs.getString("nombre_producto"));
                detalle.setCodigoProducto(rs.getString("codigo_producto"));
                detalle.setTalla(rs.getString("talla"));
                detalle.setColor(rs.getString("color"));
                detalle.setPrecio(rs.getDouble("precio"));
                detalle.setSubtotal(rs.getDouble("subtotal"));
                
                detalles.add(detalle);
            }
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        
        return detalles;
    }
    
    /**
     * Actualiza el estado de una orden de reserva.
     * @param idOrden ID de la orden
     * @param nuevoEstado Nuevo estado de la orden
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean actualizarEstadoOrden(int idOrden, String nuevoEstado) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        
        try {
            con = db.createConnection();
            String sql = "UPDATE ordenes_reserva SET estado = ? WHERE id_orden = ?";
            
            stmt = con.prepareStatement(sql);
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idOrden);
            
            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;
            
        } finally {
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
    }

    /**
     * Actualiza el estado de una orden con un motivo (ej. cancelación).
     */
    public boolean actualizarEstadoOrdenConMotivo(int idOrden, String nuevoEstado, String motivo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        
        try {
            con = db.createConnection();
            String sql = "UPDATE ordenes_reserva SET estado = ?, motivo_cancelacion = ? WHERE id_orden = ?";
            
            stmt = con.prepareStatement(sql);
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, motivo);
            stmt.setInt(3, idOrden);
            
            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;
            
        } finally {
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
    }

    /**
     * Obtiene estadísticas de órdenes.
     */
    public java.util.Map<String, Object> obtenerEstadisticasOrdenes(java.time.LocalDate inicio, java.time.LocalDate fin, Integer idBodega) throws SQLException {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            sql.append("COUNT(*) as total_ordenes, ");
            sql.append("SUM(CASE WHEN estado LIKE 'pendiente%' THEN 1 ELSE 0 END) as pendientes, ");
            sql.append("SUM(CASE WHEN estado LIKE 'retirado%' THEN 1 ELSE 0 END) as retirados, ");
            sql.append("SUM(CASE WHEN estado LIKE 'pagado%' THEN 1 ELSE 0 END) as pagados, ");
            sql.append("SUM(CASE WHEN estado LIKE 'finalizado%' THEN 1 ELSE 0 END) as finalizados, ");
            sql.append("SUM(CASE WHEN estado LIKE 'cancelada%' THEN 1 ELSE 0 END) as canceladas ");
            sql.append("FROM ordenes_reserva ");
            sql.append("WHERE DATE(fecha_creacion) BETWEEN ? AND ? ");
            
            if (idBodega != null) {
                sql.append("AND id_bodega = ? ");
            }

            stmt = con.prepareStatement(sql.toString());
            stmt.setDate(1, java.sql.Date.valueOf(inicio));
            stmt.setDate(2, java.sql.Date.valueOf(fin));
            if (idBodega != null) {
                stmt.setInt(3, idBodega);
            }

            rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("total_ordenes", rs.getInt("total_ordenes"));
                stats.put("pendientes", rs.getInt("pendientes"));
                stats.put("retirados", rs.getInt("retirados"));
                stats.put("pagados", rs.getInt("pagados"));
                stats.put("finalizados", rs.getInt("finalizados"));
                stats.put("canceladas", rs.getInt("canceladas"));
            }
            
        } finally {
             if (rs != null) rs.close();
             if (stmt != null) stmt.close();
             if (con != null) con.close();
        }
        return stats;
    }
}
