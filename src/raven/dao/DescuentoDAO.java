/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelPromocion;
import raven.controlador.productos.ModelPromocionDetalle;

/**
 *
 * @author CrisDEV
 * Clase para consultas de Descuento en la tabla promociones y promociones_detalle
 */
public class DescuentoDAO {
    
    private final conexion db;
    
    public DescuentoDAO() {
        this.db = conexion.getInstance();
    }
    
    /**
     * Obtiene todas las promociones activas con información relacionada.
     * @return Lista de promociones
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelPromocion> obtenerTodasLasPromociones() throws SQLException {
        List<ModelPromocion> promociones = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT p.*, " +
                        "COUNT(pd.id_detalle) as total_detalles " +
                        "FROM promociones p " +
                        "LEFT JOIN promociones_detalle pd ON p.id_promocion = pd.id_promocion " +
                        "WHERE p.activa = 1 " +
                        "GROUP BY p.id_promocion " +
                        "ORDER BY p.creado_en DESC";
            
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                ModelPromocion promocion = mapearPromocionDesdeResultSet(rs);
                promociones.add(promocion);
            }
            
            return promociones;
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Busca promociones por criterios específicos.
     * @param termino Término de búsqueda (nombre, código o descripción)
     * @return Lista de promociones que coinciden con el criterio
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelPromocion> buscarPromociones(String termino) throws SQLException {
        List<ModelPromocion> promociones = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT p.*, " +
                        "COUNT(pd.id_detalle) as total_detalles " +
                        "FROM promociones p " +
                        "LEFT JOIN promociones_detalle pd ON p.id_promocion = pd.id_promocion " +
                        "WHERE p.activa = 1 AND (" +
                        "p.nombre LIKE ? OR " +
                        "p.codigo LIKE ? OR " +
                        "p.descripcion LIKE ?) " +
                        "GROUP BY p.id_promocion " +
                        "ORDER BY p.creado_en DESC";
            
            stmt = con.prepareStatement(sql);
            String searchTerm = "%" + termino + "%";
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            stmt.setString(3, searchTerm);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                ModelPromocion promocion = mapearPromocionDesdeResultSet(rs);
                promociones.add(promocion);
            }
            
            return promociones;
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Obtiene una promoción por su ID.
     * @param idPromocion ID de la promoción
     * @return Promoción encontrada o null si no existe
     * @throws SQLException Si ocurre un error de base de datos
     */
    public ModelPromocion obtenerPromocionPorId(int idPromocion) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT p.*, " +
                        "COUNT(pd.id_detalle) as total_detalles " +
                        "FROM promociones p " +
                        "LEFT JOIN promociones_detalle pd ON p.id_promocion = pd.id_promocion " +
                        "WHERE p.id_promocion = ? " +
                        "GROUP BY p.id_promocion";
            
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idPromocion);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapearPromocionDesdeResultSet(rs);
            }
            
            return null;
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Inserta una nueva promoción en la base de datos.
     * @param promocion Promoción a insertar
     * @return ID de la promoción insertada
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int insertarPromocion(ModelPromocion promocion) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            
            // Verificar que la fecha de inicio sea anterior a la fecha de fin
            if (promocion.getFechaInicio().isAfter(promocion.getFechaFin())) {
                throw new SQLException("La fecha de inicio debe ser anterior a la fecha de fin");
            }
            
            String sql = "INSERT INTO promociones (codigo, nombre, descripcion, tipo_descuento, " +
                        "valor_descuento, fecha_inicio, fecha_fin, min_compra, limite_uso_total, " +
                        "limite_uso_por_usuario, activa) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, promocion.getCodigo());
            stmt.setString(2, promocion.getNombre());
            stmt.setString(3, promocion.getDescripcion());
            stmt.setString(4, promocion.getTipoDescuento());
            stmt.setDouble(5, promocion.getValorDescuento());
            stmt.setTimestamp(6, Timestamp.valueOf(promocion.getFechaInicio()));
            stmt.setTimestamp(7, Timestamp.valueOf(promocion.getFechaFin()));
            stmt.setDouble(8, promocion.getMinCompra());
            stmt.setInt(9, promocion.getLimiteUsoTotal());
            stmt.setInt(10, promocion.getLimiteUsoPorUsuario());
            stmt.setBoolean(11, promocion.isActiva());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Error al insertar promoción, no se afectaron filas.");
            }
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Error al insertar promoción, no se obtuvo el ID.");
            }
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Actualiza una promoción existente.
     * @param promocion Promoción con los datos actualizados
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean actualizarPromocion(ModelPromocion promocion) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        
        try {
            con = db.createConnection();
            String sql = "UPDATE promociones SET codigo = ?, nombre = ?, descripcion = ?, " +
                        "tipo_descuento = ?, valor_descuento = ?, fecha_inicio = ?, fecha_fin = ?, " +
                        "min_compra = ?, limite_uso_total = ?, limite_uso_por_usuario = ?, " +
                        "activa = ?, actualizado_en = NOW() " +
                        "WHERE id_promocion = ?";
            
            stmt = con.prepareStatement(sql);
            stmt.setString(1, promocion.getCodigo());
            stmt.setString(2, promocion.getNombre());
            stmt.setString(3, promocion.getDescripcion());
            stmt.setString(4, promocion.getTipoDescuento());
            stmt.setDouble(5, promocion.getValorDescuento());
            stmt.setTimestamp(6, Timestamp.valueOf(promocion.getFechaInicio()));
            stmt.setTimestamp(7, Timestamp.valueOf(promocion.getFechaFin()));
            stmt.setDouble(8, promocion.getMinCompra());
            stmt.setInt(9, promocion.getLimiteUsoTotal());
            stmt.setInt(10, promocion.getLimiteUsoPorUsuario());
            stmt.setBoolean(11, promocion.isActiva());
            stmt.setInt(12, promocion.getIdPromocion());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } finally {
            db.close(stmt, con);
        }
    }
    
    /**
     * Elimina una promoción (marcándola como inactiva).
     * @param idPromocion ID de la promoción a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean eliminarPromocion(int idPromocion) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        
        try {
            con = db.createConnection();
            String sql = "UPDATE promociones SET activa = 0, actualizado_en = NOW() WHERE id_promocion = ?";
            
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idPromocion);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } finally {
            db.close(stmt, con);
        }
    }
    
    /**
     * Obtiene los detalles de una promoción específica.
     * @param idPromocion ID de la promoción
     * @return Lista de detalles de la promoción
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelPromocionDetalle> obtenerDetallesPromocion(int idPromocion) throws SQLException {
        List<ModelPromocionDetalle> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "SELECT pd.*, " +
                        "c.nombre as nombre_categoria, " +
                        "m.nombre as nombre_marca, " +
                        "p.nombre as nombre_producto, " +
                        "u.nombre as nombre_usuario " +
                        "FROM promociones_detalle pd " +
                        "LEFT JOIN categorias c ON pd.id_categoria = c.id_categoria " +
                        "LEFT JOIN marcas m ON pd.id_marca = m.id_marca " +
                        "LEFT JOIN productos p ON pd.id_producto = p.id_producto " +
                        "LEFT JOIN usuarios u ON pd.id_usuario = u.id_usuario " +
                        "WHERE pd.id_promocion = ? AND pd.activo = 1 " +
                        "ORDER BY pd.creado_en DESC";
            
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idPromocion);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                ModelPromocionDetalle detalle = mapearDetalleDesdeResultSet(rs);
                detalles.add(detalle);
            }
            
            return detalles;
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Inserta un detalle de promoción en la base de datos.
     * @param detalle Detalle de promoción a insertar
     * @return ID del detalle insertado
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int insertarPromocionDetalle(ModelPromocionDetalle detalle) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql = "INSERT INTO promociones_detalle (id_promocion, tipo_aplicacion, " +
                        "id_categoria, id_marca, id_producto, id_usuario, rol_usuario, activo, creado_en) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            
            stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, detalle.getIdPromocion());
            stmt.setString(2, detalle.getTipoAplicacion());
            
            // Manejar valores nulos para los IDs
            if (detalle.getIdCategoria() != null) {
                stmt.setInt(3, detalle.getIdCategoria());
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            
            if (detalle.getIdMarca() != null) {
                stmt.setInt(4, detalle.getIdMarca());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            
            if (detalle.getIdProducto() != null) {
                stmt.setInt(5, detalle.getIdProducto());
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }
            
            if (detalle.getIdUsuario() != null) {
                stmt.setInt(6, detalle.getIdUsuario());
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }
            
            stmt.setString(7, detalle.getRolUsuario());
            stmt.setBoolean(8, detalle.isActivo());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Error al insertar detalle de promoción, no se afectaron filas.");
            }
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Error al insertar detalle de promoción, no se obtuvo el ID.");
            }
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Mapea un ResultSet a un objeto ModelPromocion.
     * @param rs ResultSet con los datos de la promoción
     * @return Objeto ModelPromocion mapeado
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private ModelPromocion mapearPromocionDesdeResultSet(ResultSet rs) throws SQLException {
        ModelPromocion promocion = new ModelPromocion();
        
        promocion.setIdPromocion(rs.getInt("id_promocion"));
        promocion.setCodigo(rs.getString("codigo"));
        promocion.setNombre(rs.getString("nombre"));
        promocion.setDescripcion(rs.getString("descripcion"));
        promocion.setTipoDescuento(rs.getString("tipo_descuento"));
        promocion.setValorDescuento(rs.getDouble("valor_descuento"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaInicioTs = rs.getTimestamp("fecha_inicio");
        if (fechaInicioTs != null) {
            promocion.setFechaInicio(fechaInicioTs.toLocalDateTime());
        }
        
        Timestamp fechaFinTs = rs.getTimestamp("fecha_fin");
        if (fechaFinTs != null) {
            promocion.setFechaFin(fechaFinTs.toLocalDateTime());
        }
        
        promocion.setMinCompra(rs.getDouble("min_compra"));
        promocion.setLimiteUsoTotal(rs.getInt("limite_uso_total"));
        promocion.setLimiteUsoPorUsuario(rs.getInt("limite_uso_por_usuario"));
        promocion.setActiva(rs.getBoolean("activa"));
        
        // Convertir Timestamp a LocalDateTime para campos de auditoría
        Timestamp creadoEnTs = rs.getTimestamp("creado_en");
        if (creadoEnTs != null) {
            promocion.setCreadoEn(creadoEnTs.toLocalDateTime());
        }
        
        Timestamp actualizadoEnTs = rs.getTimestamp("actualizado_en");
        if (actualizadoEnTs != null) {
            promocion.setActualizadoEn(actualizadoEnTs.toLocalDateTime());
        }
        
        return promocion;
    }
    
    /**
     * Mapea un ResultSet a un objeto ModelPromocionDetalle.
     * @param rs ResultSet con los datos del detalle
     * @return Objeto ModelPromocionDetalle mapeado
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private ModelPromocionDetalle mapearDetalleDesdeResultSet(ResultSet rs) throws SQLException {
        ModelPromocionDetalle detalle = new ModelPromocionDetalle();
        
        detalle.setIdDetalle(rs.getInt("id_detalle"));
        detalle.setIdPromocion(rs.getInt("id_promocion"));
        detalle.setTipoAplicacion(rs.getString("tipo_aplicacion"));
        detalle.setIdCategoria(rs.getInt("id_categoria"));
        detalle.setIdMarca(rs.getInt("id_marca"));
        detalle.setIdProducto(rs.getInt("id_producto"));
        detalle.setIdUsuario(rs.getInt("id_usuario"));
        detalle.setRolUsuario(rs.getString("rol_usuario"));
        detalle.setActivo(rs.getBoolean("activo"));
        
        // Campos adicionales para mostrar información relacionada
        detalle.setNombreCategoria(rs.getString("nombre_categoria"));
        detalle.setNombreMarca(rs.getString("nombre_marca"));
        detalle.setNombreProducto(rs.getString("nombre_producto"));
        detalle.setNombreUsuario(rs.getString("nombre_usuario"));
        
        return detalle;
    }
}
