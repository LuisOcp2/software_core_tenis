package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.conexion;
import raven.modelos.Usuario;

/**
 * DAO para operaciones relacionadas con usuarios.
 */
public class UsuarioDAO {

    private final conexion db;

    public UsuarioDAO() {
        this.db = conexion.getInstance();
    }

    /**
     * Obtiene todos los usuarios activos del sistema.
     * 
     * @return Lista de usuarios activos
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<Usuario> obtenerUsuariosActivos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM usuarios WHERE activo = 1 ORDER BY nombre";
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id_usuario"));
                usuario.setUsername(rs.getString("username"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setEmail(rs.getString("email"));
                usuario.setRol(rs.getString("rol"));
                usuario.setActivo(rs.getBoolean("activo"));

                usuarios.add(usuario);
            }

            return usuarios;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un usuario por su ID.
     * 
     * @param id ID del usuario
     * @return Usuario encontrado o null si no existe
     * @throws SQLException Si ocurre un error de base de datos
     */
    public Usuario obtenerUsuarioPorId(int id) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM usuarios WHERE id_usuario = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, 1);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id_usuario"));
                usuario.setUsername(rs.getString("username"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setEmail(rs.getString("email"));
                usuario.setRol(rs.getString("rol"));
                usuario.setActivo(rs.getBoolean("activo"));

                return usuario;
            }

            return null;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene un usuario por su nombre de usuario (login)
     * 
     * @param username Nombre de usuario
     * @return Usuario encontrado o null si no existe
     * @throws SQLException Si ocurre un error de base de datos
     */
    public Usuario obtenerUsuarioPorUsername(String username) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM usuarios WHERE username = ? AND activo = 1";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id_usuario"));
                usuario.setUsername(rs.getString("username"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setEmail(rs.getString("email"));
                usuario.setRol(rs.getString("rol"));
                usuario.setActivo(rs.getBoolean("activo"));

                return usuario;
            }

            return null;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Verifica las credenciales de un usuario para el login
     * 
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return Usuario autenticado o null si las credenciales son incorrectas
     * @throws SQLException Si ocurre un error de base de datos
     */
    public Usuario autenticarUsuario(String username, String password) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM usuarios WHERE username = ? AND password = ? AND activo = 1";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id_usuario"));
                usuario.setUsername(rs.getString("username"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setEmail(rs.getString("email"));
                usuario.setRol(rs.getString("rol"));
                usuario.setActivo(rs.getBoolean("activo"));

                return usuario;
            }

            return null;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene el ID del rol asignado a un usuario.
     * 
     * @param idUsuario ID del usuario
     * @return ID del rol o -1 si no tiene rol asignado
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int obtenerIdRolDeUsuario(int idUsuario) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT id_rol FROM usuarios WHERE id_usuario = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idUsuario);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id_rol");
            }

            return -1;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Asigna un rol a un usuario.
     * 
     * @param idUsuario ID del usuario
     * @param idRol     ID del rol a asignar
     * @return true si se asignó correctamente
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean asignarRolAUsuario(int idUsuario, int idRol) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "UPDATE usuarios SET id_rol = ? WHERE id_usuario = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idRol);
            stmt.setInt(2, idUsuario);

            return stmt.executeUpdate() > 0;

        } finally {
            db.close(null, stmt, con);
        }
    }
}
