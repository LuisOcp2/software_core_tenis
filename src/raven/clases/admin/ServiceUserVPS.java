package raven.clases.admin;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import raven.controlador.admin.ModelUser;
import raven.controlador.admin.ModelSession;
import raven.controlador.principal.conexion;

/**
 * ServiceUser OPTIMIZADO para VPS remoto
 * Estrategia: 1 sola llamada al SP = 1 solo roundtrip
 */
/**
 *
 * @author CrisDEV
 */
public class ServiceUserVPS {

    private static final int QUERY_TIMEOUT_SECONDS = 5;

    // Cache simple (5 minutos TTL)
    private static final Map<String, CachedUser> CACHE = new ConcurrentHashMap<>();

    private static class CachedUser {
        final ModelUser user;
        final long timestamp;

        CachedUser(ModelUser user) {
            this.user = user;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > 300_000;
        }
    }

    /**
     * ════════════════════════════════════════════════════════════
     * LOGIN COMPLETO EN 1 SOLA LLAMADA (1 roundtrip)
     * Target: ~300ms (latencia de red inevitable)
     * ════════════════════════════════════════════════════════════
     */
    public AuthResult loginCompleto(String username, String password) throws SQLException {
        long start = System.nanoTime();

        if (username == null || username.trim().isEmpty() || password == null) {
            return null;
        }

        ServiceUser serviceUser = new ServiceUser();
        ModelUser user = serviceUser.authenticate(username.trim(), password);
        if (user == null) {
            long durTotal = (System.nanoTime() - start) / 1_000_000;
            System.out.println(" Login TOTAL (fallido): " + durTotal + "ms");
            return null;
        }

        ServiceSession serviceSession = new ServiceSession();
        ModelSession sesion = serviceSession.crearSesion(user);

        CACHE.put(username.toLowerCase(), new CachedUser(user));

        long durTotal = (System.nanoTime() - start) / 1_000_000;
        System.out.println(" ========== LOGIN TOTAL: " + durTotal + "ms ==========");

        return new AuthResult(user, sesion.getTokenSesion(), sesion.getIdSesion());
    }

    /**
     * ════════════════════════════════════════════════════════════
     * PRE-VALIDACIÓN LIGERA (solo para UX - opcional)
     * Verifica si el usuario existe en cache o BD
     * ════════════════════════════════════════════════════════════
     */
    public boolean existeUsuario(String username) {
        // Check cache primero
        CachedUser cached = CACHE.get(username.toLowerCase());
        if (cached != null && !cached.isExpired()) {
            System.out.println(" [CACHE] Usuario existe: 0ms");
            return true;
        }

        // Query simple a BD
        String sql = "SELECT 1 FROM usuarios WHERE username = ? AND activo = 1 LIMIT 1";

        try (Connection con = conexion.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setQueryTimeout(2);

            long start = System.nanoTime();
            ResultSet rs = ps.executeQuery();
            boolean existe = rs.next();
            rs.close();

            long durMs = (System.nanoTime() - start) / 1_000_000;
            System.out.println(" [BD] Verificar usuario: " + durMs + "ms");

            return existe;

        } catch (SQLException e) {
            System.err.println("ERROR  Error verificando usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtener IP local
     */
    private String obtenerIPLocal() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Obtener hostname
     */
    private String obtenerHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ════════════════════════════════════════════════════════════
    // CLASES DE RESULTADO
    // ════════════════════════════════════════════════════════════

    public static class AuthResult {
        public final ModelUser user;
        public final String sessionToken;
        public final int sessionId;

        public AuthResult(ModelUser user, String sessionToken, int sessionId) {
            this.user = user;
            this.sessionToken = sessionToken;
            this.sessionId = sessionId;
        }
    }
}
