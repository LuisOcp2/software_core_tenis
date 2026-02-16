package raven.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.modelos.PrestamoZapato;

/**
 * DAO para la tabla prestamos_zapatos.
 * Provee métodos para añadir, ver y actualizar préstamos.
 */
public class PrestamoZapatoDAO {

    public static final String ESTADO_PRESTADO = "PRESTADO";
    public static final String ESTADO_DEVUELTO = "DEVUELTO";
    public static final String ESTADO_PERDIDO = "PERDIDO";
    public static final String ESTADO_DANADO = "DANADO";

    private final conexion db;

    public PrestamoZapatoDAO() {
        this.db = conexion.getInstance();
    }

    /** Inserta un préstamo y retorna el id generado */
    public int insertarPrestamo(PrestamoZapato p) throws SQLException {
        String sql = "INSERT INTO prestamos_zapatos (" +
                "id_bodega, id_producto, id_variante, pie, nombre_prestatario, " +
                "celular_prestatario, direccion_prestatario, estado, fecha_prestamo, " +
                "id_usuario, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = db.createConnection();
             PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setObject(1, p.getIdBodega(), Types.INTEGER);
            stmt.setObject(2, p.getIdProducto(), Types.INTEGER);
            stmt.setObject(3, p.getIdVariante(), Types.INTEGER);
            stmt.setString(4, p.getPie());
            stmt.setString(5, p.getNombrePrestatario());
            stmt.setString(6, p.getCelularPrestatario());
            stmt.setString(7, p.getDireccionPrestatario());
            stmt.setString(8, p.getEstado() != null ? p.getEstado() : ESTADO_PRESTADO);
            stmt.setTimestamp(9, p.getFechaPrestamo() != null ? p.getFechaPrestamo() : new Timestamp(System.currentTimeMillis()));
            stmt.setObject(10, p.getIdUsuario(), Types.INTEGER);
            stmt.setString(11, p.getObservaciones());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se pudo insertar el préstamo");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setIdPrestamo(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se obtuvo ID generado");
    }

    /** Obtiene un préstamo por id con información relacionada */
    public PrestamoZapato obtenerPrestamoPorId(int idPrestamo) throws SQLException {
        String sql = "SELECT pz.*, b.nombre AS nombre_bodega, pr.nombre AS nombre_producto, " +
                     "t.numero AS talla, c.nombre AS color " +
                     "FROM prestamos_zapatos pz " +
                     "LEFT JOIN bodegas b ON pz.id_bodega = b.id_bodega " +
                     "LEFT JOIN productos pr ON pz.id_producto = pr.id_producto " +
                     "LEFT JOIN producto_variantes pv ON pz.id_variante = pv.id_variante " +
                     "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                     "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                     "WHERE pz.id_prestamo = ?";

        try (Connection con = db.createConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idPrestamo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearPrestamo(rs);
                }
            }
        }
        return null;
    }

    /** Lista préstamos con filtros opcionales */
    public List<PrestamoZapato> listarPrestamos(String estado, Integer idBodega, Integer idProducto, Integer idVariante) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pz.*, b.nombre AS nombre_bodega, pr.nombre AS nombre_producto, ")
           .append("t.numero AS talla, c.nombre AS color ")
           .append("FROM prestamos_zapatos pz ")
           .append("LEFT JOIN bodegas b ON pz.id_bodega = b.id_bodega ")
           .append("LEFT JOIN productos pr ON pz.id_producto = pr.id_producto ")
           .append("LEFT JOIN producto_variantes pv ON pz.id_variante = pv.id_variante ")
           .append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ")
           .append("LEFT JOIN colores c ON pv.id_color = c.id_color ")
           .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        List<Integer> types = new ArrayList<>();

        if (estado != null && !estado.isBlank()) { sql.append("AND pz.estado = ? "); params.add(estado); types.add(Types.VARCHAR); }
        if (idBodega != null) { sql.append("AND pz.id_bodega = ? "); params.add(idBodega); types.add(Types.INTEGER); }
        if (idProducto != null) { sql.append("AND pz.id_producto = ? "); params.add(idProducto); types.add(Types.INTEGER); }
        if (idVariante != null) { sql.append("AND pz.id_variante = ? "); params.add(idVariante); types.add(Types.INTEGER); }

        sql.append("ORDER BY pz.fecha_prestamo DESC");

        List<PrestamoZapato> lista = new ArrayList<>();
        try (Connection con = db.createConnection(); PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                int type = types.get(i);
                if (val == null) {
                    stmt.setNull(i + 1, type);
                } else {
                    if (type == Types.INTEGER) {
                        stmt.setInt(i + 1, (Integer) val);
                    } else if (type == Types.VARCHAR) {
                        stmt.setString(i + 1, (String) val);
                    } else {
                        stmt.setObject(i + 1, val);
                    }
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPrestamo(rs));
                }
            }
        }
        return lista;
    }

    /** Actualiza datos del préstamo (sin cambiar estado por defecto) */
    public boolean actualizarDatosPrestamo(PrestamoZapato p) throws SQLException {
        String sql = "UPDATE prestamos_zapatos SET " +
                "id_bodega = ?, id_producto = ?, id_variante = ?, pie = ?, " +
                "nombre_prestatario = ?, celular_prestatario = ?, direccion_prestatario = ?, " +
                "fecha_prestamo = ?, id_usuario = ?, observaciones = ? " +
                "WHERE id_prestamo = ?";

        try (Connection con = db.createConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setObject(1, p.getIdBodega(), Types.INTEGER);
            stmt.setObject(2, p.getIdProducto(), Types.INTEGER);
            stmt.setObject(3, p.getIdVariante(), Types.INTEGER);
            stmt.setString(4, p.getPie());
            stmt.setString(5, p.getNombrePrestatario());
            stmt.setString(6, p.getCelularPrestatario());
            stmt.setString(7, p.getDireccionPrestatario());
            stmt.setTimestamp(8, p.getFechaPrestamo());
            stmt.setObject(9, p.getIdUsuario(), Types.INTEGER);
            stmt.setString(10, p.getObservaciones());
            stmt.setInt(11, p.getIdPrestamo());
            return stmt.executeUpdate() > 0;
        }
    }

    /** Cambia estado del préstamo; permite marcar devolución */
    public boolean actualizarEstadoPrestamo(int idPrestamo, String nuevoEstado, Timestamp fechaDevolucion, String observaciones, Integer idUsuario) throws SQLException {
        String sql = "UPDATE prestamos_zapatos SET estado = ?, fecha_devolucion = ?, observaciones = COALESCE(?, observaciones), id_usuario = COALESCE(?, id_usuario) WHERE id_prestamo = ?";

        try (Connection con = db.createConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setTimestamp(2, fechaDevolucion);
            if (observaciones != null) stmt.setString(3, observaciones); else stmt.setNull(3, Types.VARCHAR);
            if (idUsuario != null) stmt.setInt(4, idUsuario); else stmt.setNull(4, Types.INTEGER);
            stmt.setInt(5, idPrestamo);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Marca el préstamo como devuelto con fecha actual */
    public boolean marcarDevuelto(int idPrestamo, String observaciones, Integer idUsuario) throws SQLException {
        return actualizarEstadoPrestamo(idPrestamo, ESTADO_DEVUELTO, new Timestamp(System.currentTimeMillis()), observaciones, idUsuario);
    }

    /** Elimina físicamente un préstamo por su ID */
    public boolean eliminarPrestamo(int idPrestamo) throws SQLException {
        String sql = "DELETE FROM prestamos_zapatos WHERE id_prestamo = ?";

        try (Connection con = db.createConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idPrestamo);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Verifica si para una variante existe ya un préstamo DEVUELTO del pie complementario.
     * Ejemplo: si se devuelve IZQUIERDO, busca si hay algún DERECHO devuelto; y viceversa.
     */
    public boolean existeComplementoDevueltoParaVariante(int idVariante, String pieActual) throws SQLException {
        if (idVariante <= 0 || pieActual == null) return false;

        String pie = pieActual.trim().toUpperCase();
        String complemento;
        if ("IZQUIERDO".equals(pie)) {
            complemento = "DERECHO";
        } else if ("DERECHO".equals(pie)) {
            complemento = "IZQUIERDO";
        } else {
            // Para AMBOS u otro valor no aplica esta verificación
            return false;
        }

        String sql = "SELECT COUNT(*) AS cnt FROM prestamos_zapatos " +
                "WHERE id_variante = ? AND pie = ? AND estado = ?";

        try (Connection con = db.createConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idVariante);
            stmt.setString(2, complemento);
            stmt.setString(3, ESTADO_DEVUELTO);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
        }
        return false;
    }

    /** Lista fija de estados disponibles para préstamo */
    public List<String> listarEstadosDisponibles() throws SQLException {
        List<String> estados = new ArrayList<>();
        estados.add(ESTADO_PRESTADO);
        estados.add(ESTADO_DEVUELTO);
        estados.add(ESTADO_PERDIDO);
        estados.add(ESTADO_DANADO);
        return estados;
    }

    private PrestamoZapato mapearPrestamo(ResultSet rs) throws SQLException {
        PrestamoZapato p = new PrestamoZapato();
        p.setIdPrestamo(rs.getInt("id_prestamo"));
        p.setIdBodega((Integer) rs.getObject("id_bodega"));
        p.setIdProducto((Integer) rs.getObject("id_producto"));
        p.setIdVariante((Integer) rs.getObject("id_variante"));
        p.setPie(rs.getString("pie"));
        p.setNombrePrestatario(rs.getString("nombre_prestatario"));
        p.setCelularPrestatario(rs.getString("celular_prestatario"));
        p.setDireccionPrestatario(rs.getString("direccion_prestatario"));
        p.setEstado(rs.getString("estado"));
        p.setFechaPrestamo(rs.getTimestamp("fecha_prestamo"));
        p.setFechaDevolucion(rs.getTimestamp("fecha_devolucion"));
        p.setIdUsuario((Integer) rs.getObject("id_usuario"));
        p.setObservaciones(rs.getString("observaciones"));
        // relacionados
        try { p.setNombreBodega(rs.getString("nombre_bodega")); } catch (SQLException ignore) {}
        try { p.setNombreProducto(rs.getString("nombre_producto")); } catch (SQLException ignore) {}
        try { p.setTalla(rs.getString("talla")); } catch (SQLException ignore) {}
        try { p.setColor(rs.getString("color")); } catch (SQLException ignore) {}
        return p;
    }
}