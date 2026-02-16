package raven.clases.admin;

import java.io.*;
import java.sql.SQLException;
import raven.controlador.admin.ModelSession;
import raven.controlador.admin.ModelUser;

/**
 * Gestión de persistencia de sesión mejorada con validación de token.
 * 
 * IMPORTANTE:
 * - Guarda el token de sesión en lugar del usuario completo
 * - Valida contra la base de datos en cada carga
 * - Maneja sesiones concurrentes correctamente
 * 
 * @author CrisDEV
 */
public class SessionPersistence {
    
    private static final String SESSION_FILE = "session.dat";
    private static final String SESSION_LOCK_FILE = "session.lock";
    
    /**
     * Guarda la sesión del usuario en disco.
     * 
     * MEJORADO: Ahora guarda el token en lugar del usuario completo.
     * 
     * @param user Usuario logueado
     * @param token Token de sesión
     */
    public static void saveSession(ModelUser user, String token) {
        try {
            // Crear objeto serializable con datos mínimos
            SessionData sessionData = new SessionData();
            sessionData.idUsuario = user.getIdUsuario();
            sessionData.token = token;
            sessionData.timestamp = System.currentTimeMillis();
            
            // Guardar en archivo
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(SESSION_FILE))) {
                oos.writeObject(sessionData);
            }
            
            System.out.println("Sesion guardada en disco - Usuario: " + user.getUsername());
            
        } catch (IOException e) {
            System.err.println("Error guardando sesión: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga la sesión desde disco y valida contra la base de datos.
     * 
     * CRÍTICO: Valida que la sesión siga activa en BD.
     * 
     * @return Usuario si la sesión es válida, null en caso contrario
     */
    public static ModelUser loadSession() {
        File sessionFile = new File(SESSION_FILE);
        
        if (!sessionFile.exists()) {
            System.out.println("No hay sesion guardada");
            return null;
        }
        
        try {
            // 1. Leer datos de sesión del archivo
            SessionData sessionData;
            
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(SESSION_FILE))) {
                sessionData = (SessionData) ois.readObject();
            }
            
            // 2. Validar edad de la sesión (no mayor a 7 días)
            long edadSesion = System.currentTimeMillis() - sessionData.timestamp;
            long diasEdad = edadSesion / (1000 * 60 * 60 * 24);
            
            if (diasEdad > 7) {
                System.out.println("Advertencia: Sesion guardada muy antigua (" + diasEdad + " dias) - Ignorando");
                clearSession();
                return null;
            }
            
            // 3. Validar contra base de datos
            ServiceSession serviceSession = new ServiceSession();
            ModelSession sesionActiva = serviceSession.obtenerSesionPorToken(sessionData.token);
            
            if (sesionActiva == null) {
                System.out.println("Advertencia: Token invalido o sesion cerrada");
                clearSession();
                return null;
            }
            
            if (!sesionActiva.estaActiva()) {
                System.out.println("Advertencia: Sesion no activa en BD");
                clearSession();
                return null;
            }
            
            // 4. Verificar expiración por inactividad
            if (sesionActiva.haExpirado(30)) {
                System.out.println("Advertencia: Sesion expirada por inactividad");
                serviceSession.cerrarSesion(sessionData.token);
                clearSession();
                return null;
            }
            
            // 5. Actualizar ping
            serviceSession.actualizarPing(sessionData.token);
            
            // 6. Cargar usuario completo desde BD
            ServiceUser serviceUser = new ServiceUser();
            ModelUser usuario = serviceUser.getById(sessionData.idUsuario);
            
            if (usuario == null || !usuario.isActivo()) {
                System.out.println("Advertencia: Usuario no encontrado o inactivo");
                clearSession();
                return null;
            }
            
            System.out.println("Sesion valida restaurada - Usuario: " + usuario.getUsername());
            
            // 7. Guardar token en UserSession para uso posterior
            UserSession.getInstance().setCurrentUser(usuario);
            UserSession.getInstance().setSessionToken(sessionData.token);
            
            return usuario;
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error cargando sesion: " + e.getMessage());
            clearSession();
            return null;
        } catch (SQLException e) {
            System.err.println("Error validando sesion en BD: " + e.getMessage());
            clearSession();
            return null;
        }
    }
    
    /**
     * Elimina la sesión guardada.
     */
    public static void clearSession() {
        File sessionFile = new File(SESSION_FILE);
        if (sessionFile.exists()) {
            if (sessionFile.delete()) {
                System.out.println("Sesion eliminada del disco");
            }
        }
        
        // Limpiar lock file si existe
        File lockFile = new File(SESSION_LOCK_FILE);
        if (lockFile.exists()) {
            lockFile.delete();
        }
    }
    
    /**
     * Verifica si existe una sesión guardada.
     */
    public static boolean hasStoredSession() {
        return new File(SESSION_FILE).exists();
    }
    
    // ===================================================================
    // CLASE INTERNA PARA SERIALIZACIÓN
    // ===================================================================
    
    /**
     * Datos mínimos para guardar en disco.
     * Solo ID y token, no el usuario completo (seguridad).
     */
    private static class SessionData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        int idUsuario;
        String token;
        long timestamp;
    }
}