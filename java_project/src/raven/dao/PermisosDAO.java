package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.modelos.Permiso;

/**
 * DAO para operaciones CRUD sobre permisos del sistema.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class PermisosDAO {

    private final conexion db;

    public PermisosDAO() {
        this.db = conexion.getInstance();
    }

    /**
     * Obtiene todos los permisos del sistema.
     */
    public List<Permiso> obtenerTodosPermisos() throws SQLException {
        List<Permiso> permisos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM permisos WHERE activo = 1 ORDER BY categoria, nombre_mostrar";
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                permisos.add(mapearPermiso(rs));
            }

            return permisos;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene permisos filtrados por categoría.
     */
    public List<Permiso> obtenerPermisosPorCategoria(String categoria) throws SQLException {
        List<Permiso> permisos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM permisos WHERE categoria = ? AND activo = 1 ORDER BY nombre_mostrar";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, categoria);
            rs = stmt.executeQuery();

            while (rs.next()) {
                permisos.add(mapearPermiso(rs));
            }

            return permisos;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un permiso por su nombre de módulo.
     */
    public Permiso obtenerPermisoPorModulo(String modulo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM permisos WHERE modulo = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, modulo);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearPermiso(rs);
            }

            return null;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un permiso por su ID.
     */
    public Permiso obtenerPermisoPorId(int idPermiso) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM permisos WHERE id_permiso = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idPermiso);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearPermiso(rs);
            }

            return null;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Crea un nuevo permiso en la base de datos.
     */
    public boolean crearPermiso(Permiso permiso) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, permiso.getModulo());
            stmt.setString(2, permiso.getNombreMostrar());
            stmt.setString(3, permiso.getDescripcion());
            stmt.setString(4, permiso.getCategoria());
            stmt.setBoolean(5, permiso.isRequiereAdmin());
            stmt.setBoolean(6, permiso.isActivo());

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Actualiza un permiso existente.
     */
    public boolean actualizarPermiso(Permiso permiso) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "UPDATE permisos SET nombre_mostrar = ?, descripcion = ?, " +
                    "categoria = ?, requiere_admin = ?, activo = ? WHERE id_permiso = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, permiso.getNombreMostrar());
            stmt.setString(2, permiso.getDescripcion());
            stmt.setString(3, permiso.getCategoria());
            stmt.setBoolean(4, permiso.isRequiereAdmin());
            stmt.setBoolean(5, permiso.isActivo());
            stmt.setInt(6, permiso.getIdPermiso());

            return stmt.executeUpdate() > 0;
        } finally {
            db.close(null, stmt, con);
        }
    }

    /**
     * Obtiene todas las categorías de permisos únicas.
     */
    public List<String> obtenerCategorias() throws SQLException {
        List<String> categorias = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT DISTINCT categoria FROM permisos WHERE activo = 1 ORDER BY categoria";
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                categorias.add(rs.getString("categoria"));
            }

            return categorias;
        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Mapea un ResultSet a un objeto Permiso.
     */
    private Permiso mapearPermiso(ResultSet rs) throws SQLException {
        Permiso permiso = new Permiso();
        permiso.setIdPermiso(rs.getInt("id_permiso"));
        permiso.setModulo(rs.getString("modulo"));
        permiso.setNombreMostrar(rs.getString("nombre_mostrar"));
        permiso.setDescripcion(rs.getString("descripcion"));
        permiso.setCategoria(rs.getString("categoria"));
        permiso.setRequiereAdmin(rs.getBoolean("requiere_admin"));
        permiso.setActivo(rs.getBoolean("activo"));
        return permiso;
    }
}
