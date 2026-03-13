package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import raven.controlador.principal.conexion;
import raven.modelos.Permiso;
import raven.modelos.PrivilegioRol;
import raven.modelos.Rol;

/**
 * DAO para operaciones CRUD sobre roles y gestión de permisos de roles.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class RolesDAO {

    private final conexion db;
    private final PermisosDAO permisosDAO;

    public RolesDAO() {
        this.db = conexion.getInstance();
        this.permisosDAO = new PermisosDAO();
    }

    /**
     * Obtiene todos los roles del sistema.
     */
    public List<Rol> obtenerTodosRoles() throws SQLException {
        List<Rol> roles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM roles WHERE activo = 1 ORDER BY nombre";
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                roles.add(mapearRol(rs));
            }

            return roles;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un rol por su ID.
     */
    public Rol obtenerRolPorId(int idRol) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM roles WHERE id_rol = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearRol(rs);
            }

            return null;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un rol por su nombre.
     */
    public Rol obtenerRolPorNombre(String nombre) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM roles WHERE nombre = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, nombre);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearRol(rs);
            }

            return null;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Crea un nuevo rol en la base de datos.
     */
    public int crearRol(Rol rol) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "INSERT INTO roles (nombre, descripcion, activo, es_sistema) VALUES (?, ?, ?, ?)";
            stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, rol.getNombre());
            stmt.setString(2, rol.getDescripcion());
            stmt.setBoolean(3, rol.isActivo());
            stmt.setBoolean(4, rol.isEsSistema());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return -1;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Actualiza un rol existente.
     */
    public boolean actualizarRol(Rol rol) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "UPDATE roles SET nombre = ?, descripcion = ?, activo = ? WHERE id_rol = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, rol.getNombre());
            stmt.setString(2, rol.getDescripcion());
            stmt.setBoolean(3, rol.isActivo());
            stmt.setInt(4, rol.getIdRol());

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Elimina un rol (solo si no es de sistema y no tiene usuarios asignados).
     */
    public boolean eliminarRol(int idRol) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();

            // Verificar que no sea rol de sistema
            String checkSql = "SELECT es_sistema FROM roles WHERE id_rol = ?";
            stmt = con.prepareStatement(checkSql);
            stmt.setInt(1, idRol);
            rs = stmt.getResultSet();

            if (rs.next() && rs.getBoolean("es_sistema")) {
                throw new SQLException("No se pueden eliminar roles del sistema");
            }
            db.close(rs, stmt, null);

            // Verificar que no tenga usuarios asignados
            String countSql = "SELECT COUNT(*) as total FROM usuarios WHERE id_rol = ? AND activo = 1";
            stmt = con.prepareStatement(countSql);
            stmt.setInt(1, idRol);
            rs = stmt.executeQuery();

            if (rs.next() && rs.getInt("total") > 0) {
                throw new SQLException("No se puede eliminar un rol con usuarios asignados");
            }
            db.close(rs, stmt, null);

            // Eliminar privilegios del rol
            String deletePrivSql = "DELETE FROM privilegios_rol WHERE id_rol = ?";
            stmt = con.prepareStatement(deletePrivSql);
            stmt.setInt(1, idRol);
            stmt.executeUpdate();
            db.close(null, stmt, null);

            // Eliminar el rol
            String deleteSql = "DELETE FROM roles WHERE id_rol = ?";
            stmt = con.prepareStatement(deleteSql);
            stmt.setInt(1, idRol);

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene todos los permisos asignados a un rol.
     */
    public List<Permiso> obtenerPermisosDeRol(int idRol) throws SQLException {
        List<Permiso> permisos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT p.* FROM permisos p " +
                    "INNER JOIN privilegios_rol pr ON p.id_permiso = pr.id_permiso " +
                    "WHERE pr.id_rol = ? AND p.activo = 1 " +
                    "ORDER BY p.categoria, p.nombre_mostrar";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Permiso permiso = new Permiso();
                permiso.setIdPermiso(rs.getInt("id_permiso"));
                permiso.setModulo(rs.getString("modulo"));
                permiso.setNombreMostrar(rs.getString("nombre_mostrar"));
                permiso.setDescripcion(rs.getString("descripcion"));
                permiso.setCategoria(rs.getString("categoria"));
                permiso.setRequiereAdmin(rs.getBoolean("requiere_admin"));
                permiso.setActivo(rs.getBoolean("activo"));
                permisos.add(permiso);
            }

            return permisos;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene los privilegios detallados (con flags CRUD) de un rol.
     */
    public Map<String, PrivilegioRol> obtenerPrivilegiosDeRol(int idRol) throws SQLException {
        Map<String, PrivilegioRol> privilegios = new HashMap<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT pr.*, p.modulo, p.nombre_mostrar " +
                    "FROM privilegios_rol pr " +
                    "INNER JOIN permisos p ON pr.id_permiso = p.id_permiso " +
                    "WHERE pr.id_rol = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            rs = stmt.executeQuery();

            while (rs.next()) {
                PrivilegioRol priv = new PrivilegioRol();
                priv.setIdPrivilegio(rs.getInt("id_privilegio"));
                priv.setIdRol(rs.getInt("id_rol"));
                priv.setIdPermiso(rs.getInt("id_permiso"));
                priv.setPuedeVer(rs.getBoolean("puede_ver"));
                priv.setPuedeCrear(rs.getBoolean("puede_crear"));
                priv.setPuedeEditar(rs.getBoolean("puede_editar"));
                priv.setPuedeEliminar(rs.getBoolean("puede_eliminar"));
                priv.setModuloPermiso(rs.getString("modulo"));
                priv.setNombrePermiso(rs.getString("nombre_mostrar"));

                privilegios.put(rs.getString("modulo").toLowerCase(), priv);
            }

            return privilegios;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Asigna un permiso a un rol con privilegios específicos.
     */
    public boolean asignarPermisoARol(int idRol, int idPermiso, PrivilegioRol privilegio) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();

            // Primero eliminar si ya existe
            String deleteSql = "DELETE FROM privilegios_rol WHERE id_rol = ? AND id_permiso = ?";
            stmt = con.prepareStatement(deleteSql);
            stmt.setInt(1, idRol);
            stmt.setInt(2, idPermiso);
            stmt.executeUpdate();
            db.close(null, stmt, null);

            // Insertar el nuevo privilegio
            String sql = "INSERT INTO privilegios_rol (id_rol, id_permiso, puede_ver, puede_crear, puede_editar, puede_eliminar) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            stmt.setInt(2, idPermiso);
            stmt.setBoolean(3, privilegio.isPuedeVer());
            stmt.setBoolean(4, privilegio.isPuedeCrear());
            stmt.setBoolean(5, privilegio.isPuedeEditar());
            stmt.setBoolean(6, privilegio.isPuedeEliminar());

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Remueve un permiso de un rol.
     */
    public boolean removerPermisoDeRol(int idRol, int idPermiso) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "DELETE FROM privilegios_rol WHERE id_rol = ? AND id_permiso = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            stmt.setInt(2, idPermiso);

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Cuenta cuántos usuarios tienen asignado un rol específico.
     */
    public int contarUsuariosConRol(int idRol) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT COUNT(*) as total FROM usuarios WHERE id_rol = ? AND activo = 1";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

            return 0;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Mapea un ResultSet a un objeto Rol.
     */
    private Rol mapearRol(ResultSet rs) throws SQLException {
        Rol rol = new Rol();
        rol.setIdRol(rs.getInt("id_rol"));
        rol.setNombre(rs.getString("nombre"));
        rol.setDescripcion(rs.getString("descripcion"));
        rol.setActivo(rs.getBoolean("activo"));
        rol.setEsSistema(rs.getBoolean("es_sistema"));

        Timestamp timestamp = rs.getTimestamp("fecha_creacion");
        if (timestamp != null) {
            rol.setFechaCreacion(timestamp.toLocalDateTime());
        }

        return rol;
    }
}
