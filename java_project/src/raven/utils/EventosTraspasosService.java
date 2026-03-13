package raven.utils;

import raven.componentes.notificacion.Notification;
import raven.controlador.admin.ModelNotificacion;
import raven.clases.admin.UserSession;
import raven.utils.tono.CorporateTone;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio especializado para monitorear eventos de traspasos
 * y mostrar notificaciones emergentes en tiempo real.
 * 
 * Implementación optimizada:
 * 1. Usa ScheduledExecutorService para evitar bloquear el EDT.
 * 2. Realiza verificaciones ligeras (MAX ID) en lugar de consultas pesadas.
 * 3. Filtra estrictamente por bodega y usuario.
 */
public class EventosTraspasosService {

    private static EventosTraspasosService instance;
    private ScheduledExecutorService scheduler;
    private final AtomicInteger lastMaxId = new AtomicInteger(0);
    // Cache de notificaciones mostradas en esta sesión para evitar duplicados visuales
    private final Set<String> shownNotifications = new HashSet<>();
    private static final int POLLING_INTERVAL_SECONDS = 8;

    public static synchronized EventosTraspasosService getInstance() {
        if (instance == null) {
            instance = new EventosTraspasosService();
        }
        return instance;
    }

    /**
     * Inicia el monitoreo en segundo plano
     */
    public void iniciarMonitoreo() {
        detenerMonitoreo(); // Asegurar reinicio limpio

        if (!UserSession.getInstance().isLoggedIn()) {
            return;
        }

        // Inicializar ID máximo para evitar mostrar notificaciones antiguas al inicio
        initializeMaxId();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Traspasos-Monitor-Thread");
            t.setDaemon(true); // Permitir que la app se cierre aunque el hilo esté corriendo
            return t;
        });

        scheduler.scheduleWithFixedDelay(this::verificarNuevosEventos, 
            POLLING_INTERVAL_SECONDS, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        System.out.println("INFO: Monitoreo de traspasos iniciado (Intervalo: " + POLLING_INTERVAL_SECONDS + "s)");
    }

    /**
     * Detiene el monitoreo y limpia recursos
     */
    public void detenerMonitoreo() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
        shownNotifications.clear();
    }

    private void initializeMaxId() {
        try {
            var user = UserSession.getInstance().getCurrentUser();
            if (user != null) {
                int maxId = NotificacionesService.getInstance()
                    .obtenerMaxIdNotificacionTraspasos(user.getIdUsuario(), user.getIdBodega());
                lastMaxId.set(maxId);
            }
        } catch (Exception e) {
            System.err.println("Error initializing max ID: " + e.getMessage());
        }
    }

    private void verificarNuevosEventos() {
        try {
            if (!UserSession.getInstance().isLoggedIn()) {
                detenerMonitoreo();
                return;
            }

            var user = UserSession.getInstance().getCurrentUser();
            if (user == null) return;

            // 1. Verificación ligera: Obtener MAX ID actual
            // Esto es muy rápido y no carga la base de datos
            int currentMaxId = NotificacionesService.getInstance()
                .obtenerMaxIdNotificacionTraspasos(user.getIdUsuario(), user.getIdBodega());

            // 2. Si no hay cambios en el ID máximo, no hacer nada
            if (currentMaxId <= lastMaxId.get()) {
                return;
            }

            // 3. Obtener solo las notificaciones nuevas (ID > lastMaxId)
            List<ModelNotificacion> newNotifications = NotificacionesService.getInstance()
                .listarNuevasNotificacionesTraspasos(user.getIdUsuario(), user.getIdBodega(), lastMaxId.get());

            if (!newNotifications.isEmpty()) {
                // Programar la actualización de la UI en el EDT
                SwingUtilities.invokeLater(() -> mostrarNotificaciones(newNotifications));
                
                // Actualizar el último ID conocido
                lastMaxId.set(currentMaxId);
            }

        } catch (Exception e) {
            System.err.println("Error in background monitoring: " + e.getMessage());
        }
    }

    private void mostrarNotificaciones(List<ModelNotificacion> notifications) {
        Frame parentFrame = getActiveFrame();
        if (parentFrame == null) return;

        for (ModelNotificacion n : notifications) {
            // Clave única para evitar mostrar la misma notificación dos veces en la misma sesión
            String key = n.getIdNotificacion() + "_" + n.getEvento();
            if (shownNotifications.contains(key)) continue;

            try {
                // Determinar tipo de notificación visual
                Notification.Type type = Notification.Type.INFO;
                if ("URGENT".equalsIgnoreCase(n.getTipo()) || "ERROR".equalsIgnoreCase(n.getTipo())) {
                    type = Notification.Type.URGENT;
                } else if ("WARNING".equalsIgnoreCase(n.getTipo())) {
                    type = Notification.Type.WARNING;
                } else if ("SUCCESS".equalsIgnoreCase(n.getTipo())) {
                    type = Notification.Type.SUCCESS;
                }

                new Notification(
                    parentFrame,
                    type,
                    Notification.Location.TOP_RIGHT,
                    n.getTitulo(),
                    n.getMensaje(),
                    null,
                    8000
                ).showNotification();
                
                CorporateTone.playAlert();
                shownNotifications.add(key);
                
            } catch (Exception ignore) {}
        }
    }

    private Frame getActiveFrame() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow instanceof Frame) return (Frame) activeWindow;
        
        // Fallback: buscar el primer frame disponible
        Frame[] frames = Frame.getFrames();
        return (frames != null && frames.length > 0) ? frames[0] : null;
    }
}
