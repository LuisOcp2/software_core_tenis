package raven.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.modelos.PrivilegioUsuario;

/**
 * DAO para operaciones CRUD sobre permisos personalizados de usuarios.
 * Gestiona la tabla privilegio_usuario.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class PrivilegioUsuarioDAO {

    private final conexion db;

    public PrivilegioUsuarioDAO() {
        this.db = conexion.getInstance();
    }

    /**
     * Obtiene todos los permisos personalizados de un usuario específico
     * 
     * @param idUsuario ID del usuario
     * @return Lista de permisos personalizados con información del permiso
     */
    public List<PrivilegioUsuario> obtenerPermisosPorUsuario(int idUsuario) throws SQLException {
        List<PrivilegioUsuario> permisos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT pu.*, p.modulo, p.nombre_mostrar " +
                    "FROM privilegio_usuario pu " +
                    "JOIN permisos p ON p.id_permiso = pu.id_permiso " +
                    "WHERE pu.id_usuario = ? " +
                    "ORDER BY p.categoria, p.nombre_mostrar";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();

            while (rs.next()) {
                permisos.add(mapearPrivilegioUsuario(rs));
            }

            return permisos;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Crea un nuevo permiso personalizado para un usuario
     * 
     * @param privilegio Objeto con datos del permiso a crear
     * @return true si se creó exitosamente
     */
    public boolean crearPermisoPersonalizado(PrivilegioUsuario privilegio) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "INSERT INTO privilegio_usuario " +
                    "(id_usuario, id_permiso, puede_ver, puede_crear, puede_editar, puede_eliminar, id_usuario_asignador) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, privilegio.getIdUsuario());
            stmt.setInt(2, privilegio.getIdPermiso());
            stmt.setBoolean(3, privilegio.isPuedeVer());
            stmt.setBoolean(4, privilegio.isPuedeCrear());
            stmt.setBoolean(5, privilegio.isPuedeEditar());
            stmt.setBoolean(6, privilegio.isPuedeEliminar());

            if (privilegio.getIdUsuarioAsignador() != null) {
                stmt.setInt(7, privilegio.getIdUsuarioAsignador());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Actualiza un permiso personalizado existente
     * 
     * @param privilegio Objeto con datos actualizados
     * @return true si se actualizó exitosamente
     */
    public boolean actualizarPermisoPersonalizado(PrivilegioUsuario privilegio) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "UPDATE privilegio_usuario " +
                    "SET puede_ver = ?, puede_crear = ?, puede_editar = ?, puede_eliminar = ? " +
                    "WHERE id_privilegio_usuario = ?";
            stmt = con.prepareStatement(sql);
            stmt.setBoolean(1, privilegio.isPuedeVer());
            stmt.setBoolean(2, privilegio.isPuedeCrear());
            stmt.setBoolean(3, privilegio.isPuedeEditar());
            stmt.setBoolean(4, privilegio.isPuedeEliminar());
            stmt.setInt(5, privilegio.getIdPrivilegioUsuario());

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Elimina un permiso personalizado por su ID
     * 
     * @param idPrivilegioUsuario ID del permiso personalizado a eliminar
     * @return true si se eliminó exitosamente
     */
    public boolean eliminarPermisoPersonalizado(int idPrivilegioUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "DELETE FROM privilegio_usuario WHERE id_privilegio_usuario = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idPrivilegioUsuario);

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Verifica si ya existe un permiso personalizado para un usuario y permiso
     * específico
     * 
     * @param idUsuario ID del usuario
     * @param idPermiso ID del permiso
     * @return true si ya existe
     */
    public boolean existePermisoPersonalizado(int idUsuario, int idPermiso) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT COUNT(*) FROM privilegio_usuario WHERE id_usuario = ? AND id_permiso = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idPermiso);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un permiso personalizado específico para un usuario y permiso.
     * 
     * @param idUsuario ID del usuario
     * @param idPermiso ID del permiso
     * @return El privilegio encontrado o null si no existe
     */
    public PrivilegioUsuario obtenerPrivilegioPorUsuarioYPermiso(int idUsuario, int idPermiso) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM privilegio_usuario WHERE id_usuario = ? AND id_permiso = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idPermiso);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearPrivilegioUsuario(rs);
            }
            return null;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Elimina todos los permisos personalizados de un usuario
     * 
     * @param idUsuario ID del usuario
     * @return Número de permisos eliminados
     */
    public int eliminarTodosPermisosUsuario(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "DELETE FROM privilegio_usuario WHERE id_usuario = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);

            return stmt.executeUpdate();
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Mapea un ResultSet a un objeto PrivilegioUsuario
     */
    /**
     * Verifica si un usuario tiene al menos un permiso personalizado asignado.
     */
    public boolean tienePermisosAsignados(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT 1 FROM privilegio_usuario WHERE id_usuario = ? LIMIT 1";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();

            return rs.next();
        } finally {
            db.close(rs, stmt, con);
        }
    }

    private PrivilegioUsuario mapearPrivilegioUsuario(ResultSet rs) throws SQLException {
        PrivilegioUsuario privilegio = new PrivilegioUsuario();
        privilegio.setIdPrivilegioUsuario(rs.getInt("id_privilegio_usuario"));
        privilegio.setIdUsuario(rs.getInt("id_usuario"));
        privilegio.setIdPermiso(rs.getInt("id_permiso"));
        privilegio.setPuedeVer(rs.getBoolean("puede_ver"));
        privilegio.setPuedeCrear(rs.getBoolean("puede_crear"));
        privilegio.setPuedeEditar(rs.getBoolean("puede_editar"));
        privilegio.setPuedeEliminar(rs.getBoolean("puede_eliminar"));
        privilegio.setFechaAsignacion(rs.getTimestamp("fecha_asignacion"));

        int idAsignador = rs.getInt("id_usuario_asignador");
        if (!rs.wasNull()) {
            privilegio.setIdUsuarioAsignador(idAsignador);
        }

        // Datos adicionales del JOIN (si existen)
        try {
            privilegio.setModulo(rs.getString("modulo"));
            privilegio.setNombreMostrar(rs.getString("nombre_mostrar"));
        } catch (SQLException e) {
            // Columnas opcionales no presentes
        }

        return privilegio;
    }
}
