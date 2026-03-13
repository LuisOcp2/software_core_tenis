package raven.clases.admin;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.conexion;

public class ServiceUser {

    private conexion conexion;
    private static final int QUERY_TIMEOUT_SECONDS = 3;
    private static final int CACHE_SIZE = 100;
    private static final Map<Integer, ModelUser> CACHE_BY_ID = new LinkedHashMap<Integer, ModelUser>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, ModelUser> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private static final Map<String, ModelUser> CACHE_BY_USERNAME = new LinkedHashMap<String, ModelUser>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ModelUser> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public ServiceUser() {
        this.conexion = conexion.getInstance();
    }

    /**
     * Obtiene un usuario por su ID.
     */
    public ModelUser getById(int idUsuario) throws SQLException {
        ModelUser cached = CACHE_BY_ID.get(idUsuario);
        if (cached != null) return cached;
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ? AND activo = 1";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ModelUser u = mapearResultSetAUsuario(rs);
                    CACHE_BY_ID.put(idUsuario, u);
                    return u;
                }
            }
        }

        return null;
    }

    /**
     * Autentica un usuario con username y password.
     */
    public ModelUser authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND activo = 1";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setFetchSize(1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");

                    // Verificar si la contraseña está hasheada (formato BCrypt)
                    if (isBcryptHash(dbPassword)) {
                        // Comparar usando BCrypt
                        if (BCrypt.checkpw(password, dbPassword)) {
                            return mapearResultSetAUsuario(rs);
                        }
                    } else {
                        // Contraseña en texto plano (legacy) - comparar directamente
                        // Y actualizar a BCrypt
                        if (password.equals(dbPassword)) {
                            // Actualizar contraseña a BCrypt
                            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                            actualizarPasswordAHash(rs.getInt("id_usuario"), hashedPassword);
                            return mapearResultSetAUsuario(rs);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Verifica si una cadena es un hash BCrypt válido
     */
    private boolean isBcryptHash(String password) {
        return password != null
                && password.length() == 60
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private String hashPasswordIfNeeded(String password) {
        if (password == null || password.isEmpty() || isBcryptHash(password)) {
            return password;
        }
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Actualiza la contraseña de un usuario a formato hash
     */
    private void actualizarPasswordAHash(int idUsuario, String hashedPassword) {
        String sql = "UPDATE usuarios SET password = ? WHERE id_usuario = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error actualizando contraseña a hash: " + e.getMessage());
        }
    }

    private ModelUser mapearResultSetAUsuario(ResultSet rs) throws SQLException {
        return new ModelUser(
                rs.getInt("id_usuario"),
                rs.getString("username"),
                null, // No retornar password
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("rol"),
                rs.getString("ubicacion"),
                rs.getInt("id_bodega"),
                rs.getBoolean("activo")
        );
    }

    public void insertarUsuario(ModelUser usuario) throws SQLException {
        String sql = "INSERT INTO usuarios (username, password, nombre, email, rol, ubicacion, id_bodega, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getUsername());
            stmt.setString(2, hashPasswordIfNeeded(usuario.getPassword()));
            stmt.setString(3, usuario.getNombre());
            stmt.setString(4, usuario.getEmail());
            stmt.setString(5, usuario.getRol());
            stmt.setString(6, usuario.getUbicacion());
            stmt.setInt(7, usuario.getIdBodega());
            stmt.setBoolean(8, usuario.isActivo());
            stmt.executeUpdate();
        }
    }

    public void modificarUsuario(ModelUser usuario) throws SQLException {
        // Si la contraseña está vacía, obtener la contraseña actual de la base de datos
        if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            // Obtener el usuario actual
            String sql = "SELECT password FROM usuarios WHERE id_usuario = ?";
            try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, usuario.getIdUsuario());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Usar la contraseña existente
                    usuario.setPassword(rs.getString("password"));
                }
            }
        }

        usuario.setPassword(hashPasswordIfNeeded(usuario.getPassword()));

        // El resto del método existente para actualizar el usuario
        String sql = "UPDATE usuarios SET nombre = ?, password = ?, email = ?, rol = ?, ubicacion = ?, id_bodega = ?, activo = ? WHERE id_usuario = ?";
        try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getPassword());
            stmt.setString(3, usuario.getEmail());
            stmt.setString(4, usuario.getRol());
            stmt.setString(5, usuario.getUbicacion());
            stmt.setInt(6, usuario.getIdBodega());
            stmt.setBoolean(7, usuario.isActivo());
            stmt.setInt(8, usuario.getIdUsuario());
            stmt.executeUpdate();
        }

        CACHE_BY_ID.remove(usuario.getIdUsuario());
        if (usuario.getUsername() != null) {
            CACHE_BY_USERNAME.remove(usuario.getUsername());
        }
    }
// Método adicional para obtener un usuario por ID

    public ModelUser obtenerUsuarioPorId(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ModelUser(
                        rs.getInt("id_usuario"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("rol"),
                        rs.getString("ubicacion"),
                        rs.getInt("id_bodega"),
                        rs.getBoolean("activo")
                );
            }
        }
        return null;
    }

    public void desactivar(int idUser) throws SQLException {
        String sql = "UPDATE usuarios SET activo = 0 WHERE id_usuario = ?";

        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {

            p.setInt(1, idUser);
            p.executeUpdate();
        }
    }

    public ModelUser obtenerUsuarioPorUsername(String username) throws SQLException {
        ModelUser cached = CACHE_BY_USERNAME.get(username);
        if (cached != null) return cached;
        String sql = "SELECT * FROM usuarios WHERE username = ?";
        try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ModelUser u = new ModelUser(
                        rs.getInt("id_usuario"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("rol"),
                        rs.getString("ubicacion"),
                        rs.getInt("id_bodega"),
                        rs.getBoolean("activo")
                );
                CACHE_BY_USERNAME.put(username, u);
                CACHE_BY_ID.put(u.getIdUsuario(), u);
                return u;
            }
        }
        return null;
    }

    // ==============================================
    // LOGIN OPTIMIZADO VIA PROCEDIMIENTO ALMACENADO
    // ==============================================
    public static final class AuthResult {
        public final ModelUser user;
        public final String sessionToken;
        public final int sessionId;

        public AuthResult(ModelUser user, String sessionToken, int sessionId) {
            this.user = user;
            this.sessionToken = sessionToken;
            this.sessionId = sessionId;
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, AttemptWindow> RATE_LIMIT = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L;

    private static final class AttemptWindow {
        int count;
        long firstTs;
    }

    private boolean permitidoIntento(String username) {
        long now = System.currentTimeMillis();
        AttemptWindow w = RATE_LIMIT.computeIfAbsent(username.toLowerCase(), k -> {
            AttemptWindow aw = new AttemptWindow();
            aw.count = 0;
            aw.firstTs = now;
            return aw;
        });
        if (now - w.firstTs > WINDOW_MILLIS) {
            w.firstTs = now;
            w.count = 0;
        }
        if (w.count >= MAX_ATTEMPTS) {
            return false;
        }
        w.count++;
        return true;
    }

    private void resetIntentos(String username) {
        RATE_LIMIT.remove(username.toLowerCase());
    }

    public AuthResult authenticateOptimizado(String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty() || password == null) {
            return null;
        }

        if (!permitidoIntento(username)) {
            throw new SQLException("Demasiados intentos de login para usuario");
        }

        long start = System.nanoTime();

        String sql = "CALL sp_login_optimizado(?, ?)";
        try (Connection con = conexion.getInstance().createConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, username);
            cs.setString(2, password);

            boolean hasResult = cs.execute();
            ModelUser user = null;
            int sessionId = 0;
            String token = null;
            if (hasResult) {
                ResultSet rs = cs.getResultSet();
                // Avanzar hasta un ResultSet válido
                while (rs == null && cs.getMoreResults()) {
                    rs = cs.getResultSet();
                }
                if (rs != null && rs.next()) {
                    user = mapearResultSetAUsuario(rs);
                    try {
                        sessionId = rs.getInt("session_id");
                        if (rs.wasNull()) sessionId = rs.getInt("id_sesion");
                    } catch (SQLException ignore) {}
                    try {
                        token = rs.getString("token_sesion");
                        if (token == null || token.isEmpty()) token = rs.getString("token");
                    } catch (SQLException ignore) {}
                    rs.close();
                }
            }

            long durMs = (System.nanoTime() - start) / 1_000_000;
            System.out.println(" Login SP duración: " + durMs + " ms");

            if (user != null && token != null && !token.isEmpty()) {
                resetIntentos(username);
                return new AuthResult(user, token, sessionId);
            }
        }

        return null;
    }

    public List<ModelUser> obtenerTodosLosUsuarios() throws SQLException {
        List<ModelUser> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";
        try (Connection conn = conexion.createConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ModelUser usuario = new ModelUser(
                        rs.getInt("id_usuario"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("rol"),
                        rs.getString("ubicacion"),
                        rs.getInt("id_bodega"),
                        rs.getBoolean("activo")
                );
                usuarios.add(usuario);
            }
        }
        return usuarios;
    }

    public List<ModelUser> obtenerUsuariosPorBodega(int idBodega) throws SQLException {
        List<ModelUser> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE id_bodega = ?";
        try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idBodega);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ModelUser usuario = new ModelUser(
                            rs.getInt("id_usuario"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("rol"),
                            rs.getString("ubicacion"),
                            rs.getInt("id_bodega"),
                            rs.getBoolean("activo")
                    );
                    usuarios.add(usuario);
                }
            }
        }
        return usuarios;
    }

    public List<ModelUser> search(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM usuarios "
                + "WHERE activo = 1 AND ("
                + "    LOWER(username) LIKE LOWER(?) OR "
                + "    LOWER(nombre) LIKE LOWER(?) OR "
                + "    LOWER(email) LIKE LOWER(?) OR "
                + "    CAST(id_usuario AS CHAR) LIKE ?) "
                + "ORDER BY nombre";
        try (Connection con = conexion.createConnection(); PreparedStatement p = con.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            String numericSearch = "%" + searchTerm + "%";
            p.setString(1, searchPattern);
            p.setString(2, searchPattern);
            p.setString(3, searchPattern);
            p.setString(4, numericSearch);
            try (ResultSet r = p.executeQuery()) {
                List<ModelUser> usuarios = new ArrayList<>();
                while (r.next()) {
                    usuarios.add(new ModelUser(
                            r.getInt("id_usuario"),
                            r.getString("username"),
                            r.getString("password"),
                            r.getString("nombre"),
                            r.getString("email"),
                            r.getString("rol"),
                            r.getString("ubicacion"),
                            r.getInt("id_bodega"),
                            r.getBoolean("activo")
                    ));
                }
                return usuarios;
            }
        }
    }

    public List<ModelUser> search(String searchTerm, int idBodega) throws SQLException {
        String sql = "SELECT * FROM usuarios "
                + "WHERE activo = 1 AND id_bodega = ? AND ("
                + "    LOWER(username) LIKE LOWER(?) OR "
                + "    LOWER(nombre) LIKE LOWER(?) OR "
                + "    LOWER(email) LIKE LOWER(?) OR "
                + "    CAST(id_usuario AS CHAR) LIKE ?) "
                + "ORDER BY nombre";
        try (Connection con = conexion.createConnection(); PreparedStatement p = con.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            String numericSearch = "%" + searchTerm + "%";
            p.setInt(1, idBodega);
            p.setString(2, searchPattern);
            p.setString(3, searchPattern);
            p.setString(4, searchPattern);
            p.setString(5, numericSearch);
            try (ResultSet r = p.executeQuery()) {
                List<ModelUser> usuarios = new ArrayList<>();
                while (r.next()) {
                    usuarios.add(new ModelUser(
                            r.getInt("id_usuario"),
                            r.getString("username"),
                            r.getString("password"),
                            r.getString("nombre"),
                            r.getString("email"),
                            r.getString("rol"),
                            r.getString("ubicacion"),
                            r.getInt("id_bodega"),
                            r.getBoolean("activo")
                    ));
                }
                return usuarios;
            }
        }
    }

    public ModelUser obtenerUsuario(int idUsuario, Connection con) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ModelUser(
                            rs.getInt("id_usuario"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("rol"),
                            rs.getString("ubicacion"),
                            rs.getInt("id_bodega"),
                            rs.getBoolean("activo")
                    );
                }
            }
        }

        return null; // O lanzar una excepción si prefieres
    }

    public void activarUsuario(int idUsuario) throws SQLException {
        String sql = "UPDATE usuarios SET activo = 1 WHERE id_usuario = ?";
        try (Connection con = conexion.getInstance().createConnection(); PreparedStatement p = con.prepareStatement(sql)) {
            p.setInt(1, idUsuario);
            p.executeUpdate();
        }
    }

    public boolean existeUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = conexion.createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}

