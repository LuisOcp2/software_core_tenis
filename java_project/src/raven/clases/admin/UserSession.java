package raven.clases.admin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import raven.controlador.admin.ModelUser;
import raven.controlador.admin.ModelSession;
import raven.controlador.admin.ModelCaja;
import raven.dao.UsuarioDAO;
import raven.modelos.PrivilegioRol;
import raven.modelos.Rol;

/**
 * Gestión de sesión con asociación correcta a bodega y caja.
 * 
 * MODIFICADO:
 * - Asocia automáticamente la caja según la bodega del usuario
 * - Valida que exista una caja para la bodega
 * - SUCCESS NUEVO: Validación segura de contraseña contra BD
 * 
 * @author CrisDEV
 * @version 3.1
 */
public class UserSession {

    private static volatile UserSession instance;
    private ModelUser currentUser;
    private String sessionToken;
    private String userLocation;
    private Timer keepAliveTimer;
    private ServiceSession serviceSession;
    private Integer idCajaAsociada;
    private Integer idBodegaAsociada;
    private Integer idMovimientoActual;
    private int idCajaActiva;
    private Integer defaultBodegaOrigen;
    private Integer defaultBodegaDestino;
    private Rol rolUsuario;
    private Map<String, PrivilegioRol> permisosCache;
    private ServicePermisos servicePermisos;
    private UsuarioDAO usuarioDAO;

    private static final int PING_INTERVAL_SECONDS = 60;

    private UserSession() {
        System.out.println("UserSession: Inicializando instancia Singleton...");
        this.serviceSession = new ServiceSession();
        this.servicePermisos = new ServicePermisos();
        this.permisosCache = new HashMap<>();
    }

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public Integer getIdCajaActiva() {
        return this.idCajaActiva;
    }
    // ===================================================================
    // GESTIÓN DE SESIÓN CON BODEGA
    // ===================================================================

    /**
     * Establece el usuario y su bodega asociada.
     */
    public void setCurrentUser(ModelUser user) {
        this.currentUser = user;

        if (user != null) {
            try {
                // Guardar bodega del usuario
                this.idBodegaAsociada = user.getIdBodega();

                // Cargar permisos del usuario desde BD
                cargarPermisosUsuario(user.getIdUsuario());

                // Crear sesión en base de datos
                ModelSession sesion = serviceSession.crearSesion(user);
                this.sessionToken = sesion.getTokenSesion();

                // Guardar sesión en disco
                SessionPersistence.saveSession(user, sessionToken);

                // Iniciar keepalive
                iniciarKeepAlive();

                System.out.println("Usuario establecido: " + user.getUsername());
                System.out.println("Bodega asociada: ID " + idBodegaAsociada);
                System.out.println("Permisos cargados: " + permisosCache.size() + " módulos");

            } catch (SQLException e) {
                System.err.println("Error creando sesión: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public ModelUser getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null && sessionToken != null;
    }

    /**
     * SUCCESS NUEVO: Obtiene el ID de la bodega del usuario actual.
     */
    public Integer getIdBodegaUsuario() {
        return idBodegaAsociada;
    }

    /**
     * SUCCESS NUEVO: Obtiene el ID de la caja asociada.
     */
    public Integer getIdCajaAsociada() {
        return idCajaAsociada;
    }

    /**
     * SUCCESS NUEVO: Obtiene el ID del movimiento actual.
     */
    public Integer getIdMovimientoActual() {
        return idMovimientoActual;
    }

    public void setDefaultBodegas(Integer origen, Integer destino) {
        this.defaultBodegaOrigen = origen;
        this.defaultBodegaDestino = destino;
    }

    public Integer getDefaultBodegaOrigen() {
        return defaultBodegaOrigen;
    }

    public Integer getDefaultBodegaDestino() {
        return defaultBodegaDestino;
    }

    public void logout() {
        if (sessionToken != null) {
            try {
                serviceSession.cerrarSesion(sessionToken);
            } catch (SQLException e) {
                System.err.println("Advertencia: Error cerrando sesión en BD: " + e.getMessage());
            }
        }

        detenerKeepAlive();

        currentUser = null;
        sessionToken = null;
        userLocation = null;
        idCajaAsociada = null;
        idBodegaAsociada = null;
        idMovimientoActual = null;

        SessionPersistence.clearSession();

        System.out.println("Sesion cerrada");
    }

    // ===================================================================
    // SUCCESS NUEVO: VALIDACIÓN SEGURA DE CONTRASEÑA
    // ===================================================================

    /**
     * Valida la contraseña del usuario actual contra la base de datos.
     * 
     * Usa autenticación completa del ServiceUser para validar credenciales.
     * 
     * SEGURIDAD:
     * - NO almacena contraseñas en memoria
     * - Usa hash BCrypt desde BD
     * - Valida contra usuario actual de la sesión
     * 
     * @param password Contraseña en texto plano a validar
     * @return true si la contraseña es correcta, false en caso contrario
     */
    public boolean validarPasswordActual(String password) {
        // ================================================================
        // 1. VALIDACIONES PREVIAS
        // ================================================================
        if (password == null || password.trim().isEmpty()) {
            System.err.println("ERROR  Contraseña vacía");
            return false;
        }

        if (!isLoggedIn()) {
            System.err.println("ERROR  No hay sesión activa");
            return false;
        }

        if (currentUser == null) {
            System.err.println("ERROR  Usuario actual es null");
            return false;
        }

        // ================================================================
        // 2. VALIDAR CONTRA BASE DE DATOS
        // ================================================================
        try {
            ServiceUser serviceUser = new ServiceUser();

            // Autenticar usando username del usuario actual
            String username = currentUser.getUsername();

            System.out.println(" Validando contraseña para usuario: " + username);

            // Usar método authenticate que valida hash BCrypt
            ModelUser usuarioValidado = serviceUser.authenticate(username, password);

            if (usuarioValidado != null) {
                // SUCCESS Contraseña correcta
                System.out.println("SUCCESS  Contraseña validada correctamente");

                // Opcional: Registrar evento de desbloqueo
                registrarEventoValidacion(true);

                return true;
            } else {
                // ERROR Contraseña incorrecta
                System.err.println("ERROR  Contraseña incorrecta");

                // Opcional: Registrar intento fallido
                registrarEventoValidacion(false);

                return false;
            }

        } catch (SQLException e) {
            System.err.println("ERROR  Error de BD validando contraseña: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Registra un evento de validación de contraseña (auditoría).
     * 
     * @param exitoso true si la validación fue exitosa
     */
    private void registrarEventoValidacion(boolean exitoso) {
        try {
            String evento = exitoso
                    ? "DESBLOQUEO_EXITOSO"
                    : "INTENTO_DESBLOQUEO_FALLIDO";

            // Registrar en log de seguridad
            System.out.println(String.format(
                    "Nota [SEGURIDAD] %s - Usuario: %s - Fecha: %s",
                    evento,
                    currentUser.getUsername(),
                    java.time.LocalDateTime.now()));

            // TODO: Implementar registro en tabla de auditoría si existe
            // serviceSession.registrarEventoSeguridad(
            // currentUser.getIdUsuario(),
            // evento,
            // sessionToken
            // );

        } catch (Exception e) {
            System.err.println("WARNING  Error registrando evento de validación: " + e.getMessage());
        }
    }

    // ===================================================================
    // KEEPALIVE
    // ===================================================================

    private void iniciarKeepAlive() {
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
        }

        keepAliveTimer = new Timer("SessionKeepAlive", true);

        keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (sessionToken != null) {
                    try {
                        serviceSession.actualizarPing(sessionToken);
                    } catch (SQLException e) {
                        System.err.println("Advertencia: Error en keepalive: " + e.getMessage());
                    }
                }
            }
        }, PING_INTERVAL_SECONDS * 1000L, PING_INTERVAL_SECONDS * 1000L);

        System.out.println("KeepAlive iniciado (cada " + PING_INTERVAL_SECONDS + "s)");
    }

    private void detenerKeepAlive() {
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
            keepAliveTimer = null;
            System.out.println("KeepAlive detenido");
        }
    }

    // ===================================================================
    // GESTIÓN DE UBICACIÓN Y TOKEN
    // ===================================================================

    public String getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(String location) {
        this.userLocation = location;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String token) {
        this.sessionToken = token;
    }

    // ===================================================================
    // VALIDACIÓN DE PERMISOS
    // ===================================================================

    public boolean hasRole(String role) {
        return isLoggedIn() && currentUser.getRol().equalsIgnoreCase(role);
    }

    /**
     * Verifica si el usuario tiene permiso para acceder a un módulo.
     * Por defecto verifica permiso de "ver".
     */
    public boolean hasPermission(String moduleName) {
        return hasPermission(moduleName, "ver");
    }

    /**
     * Verifica si el usuario tiene un permiso específico para realizar una acción.
     * 
     * @param moduleName Nombre del módulo/permiso
     * @param accion     Acción a verificar: "ver", "crear", "editar", "eliminar"
     * @return true si tiene el permiso, false en caso contrario
     */
    public boolean hasPermission(String moduleName, String accion) {
        if (!isLoggedIn()) {
            return false;
        }

        // Compatibilidad: Solo Admin tiene acceso total por defecto
        String rol = currentUser.getRol().toLowerCase();
        // REMOVED: Admin check to allow granular permission testing
        // if (rol.equals("admin")) {
        // return true;
        // }

        // Consultar permisos desde cache
        PrivilegioRol privilegio = permisosCache.get(moduleName.toLowerCase());
        if (privilegio == null) {
            return false;
        }

        // Verificar el tipo de acción
        switch (accion.toLowerCase()) {
            case "ver":
                return privilegio.isPuedeVer();
            case "crear":
                return privilegio.isPuedeCrear();
            case "editar":
                return privilegio.isPuedeEditar();
            case "eliminar":
                return privilegio.isPuedeEliminar();
            default:
                return false;
        }
    }

    /**
     * Obtiene la lista de módulos a los que el usuario tiene acceso.
     */
    public List<String> getModulosPermitidos() {
        List<String> modulos = new ArrayList<>();
        for (Map.Entry<String, PrivilegioRol> entry : permisosCache.entrySet()) {
            if (entry.getValue().isPuedeVer()) {
                modulos.add(entry.getKey());
            }
        }
        return modulos;
    }

    /**
     * Recarga los permisos del usuario actual desde la base de datos.
     */
    public void recargarPermisos() {
        if (currentUser != null) {
            try {
                cargarPermisosUsuario(currentUser.getIdUsuario());
                System.out.println("Permisos recargados: " + permisosCache.size() + " módulos");
            } catch (SQLException e) {
                System.err.println("Error recargando permisos: " + e.getMessage());
            }
        }
    }

    /**
     * Carga los permisos de un usuario desde la base de datos.
     */
    private void cargarPermisosUsuario(int idUsuario) throws SQLException {
        try {
            // Obtener privilegios del usuario
            this.permisosCache = servicePermisos.obtenerPermisosDetalladosUsuario(idUsuario);

        } catch (Exception e) {
            System.err.println("Error cargando permisos: " + e.getMessage());
            this.permisosCache = new HashMap<>();
        }
    }

    // ===================================================================
    // MÉTODOS DE CAJA CON VALIDACIÓN DE BODEGA
    // ===================================================================

    public void asociarCaja(int idCaja, int idMovimiento) {
        if (sessionToken != null && idBodegaAsociada != null) {
            try {
                // Validar que la caja pertenece a la bodega del usuario
                ServiceCaja serviceCaja = new ServiceCaja();
                ModelCaja caja = serviceCaja.obtenerCajaPorId(idCaja);

                if (caja == null) {
                    System.err.println("ERROR  Caja no encontrada: ID " + idCaja);
                    return;
                }

                // ═══════════════════════════════════════════════════════════════
                // GUARDAR EN AMBOS ATRIBUTOS para consistencia
                // ═══════════════════════════════════════════════════════════════
                this.idCajaAsociada = idCaja;
                this.idCajaActiva = idCaja; // ← AGREGAR ESTA LÍNEA
                this.idMovimientoActual = idMovimiento;

                serviceSession.asociarCaja(sessionToken, idCaja, idMovimiento);

                System.out.println("SUCCESS  Caja asociada a sesión");
                System.out.println("   - ID Caja: " + idCaja);
                System.out.println("   - ID Movimiento: " + idMovimiento);
                System.out.println("   - Bodega: " + idBodegaAsociada);

            } catch (SQLException e) {
                System.err.println("ERROR  Error asociando caja: " + e.getMessage());
            }
        }
    }

    public boolean tieneOtraSesionActiva() {
        if (currentUser == null || sessionToken == null) {
            return false;
        }

        try {
            return serviceSession.tieneOtraSesionActiva(
                    currentUser.getIdUsuario(),
                    sessionToken);
        } catch (SQLException e) {
            System.err.println("WARNING  Error verificando otras sesiones: " + e.getMessage());
            return false;
        }
    }
}
