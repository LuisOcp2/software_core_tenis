package raven.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verificador automático que ejecuta verificaciones periódicas. Aplica el
 * principio OCP (Open/Closed Principle).
 */
public class VerificadorAutomatico {

    private static final Logger LOGGER = Logger.getLogger(VerificadorAutomatico.class.getName());
    private static VerificadorAutomatico instance;
    private ScheduledExecutorService scheduler;
    private final NotificacionesService notificacionesService;

    // Período de verificación (en minutos) - Optimizado para reducir carga en BD
    private static final int PERIODO_VERIFICACION_MINUTOS = 5;
    private volatile boolean iniciado = false;

    private VerificadorAutomatico() {
        // No inicializamos scheduler aquí, lo haremos en el método iniciar()
        this.notificacionesService = new NotificacionesService();
    }

    public static synchronized VerificadorAutomatico getInstance() {
        if (instance == null) {
            instance = new VerificadorAutomatico();
        }
        return instance;
    }

    /**
     * Inicia el verificador automático.
     */
    public void iniciar() {

        // Si ya está iniciado, no hacemos nada
        if (iniciado) {
            LOGGER.info("El verificador automático ya está en ejecución");
            return;
        }
        // Crear un nuevo scheduler si el anterior fue terminado
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
        }

        try {
            // Ejecutar inmediatamente una primera verificación
            scheduler.execute(() -> {
                try {
                    notificacionesService.verificarSistema();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error en verificación inicial", e);
                }
            });

            // Programar verificaciones periódicas
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    notificacionesService.verificarSistema();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error en verificación periódica", e);
                }
            }, PERIODO_VERIFICACION_MINUTOS, PERIODO_VERIFICACION_MINUTOS, TimeUnit.MINUTES);

            iniciado = true;
            LOGGER.info("Verificador automático iniciado con período de "
                    + PERIODO_VERIFICACION_MINUTOS + " minutos");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar verificador automático", e);
            // Si hay error, aseguramos detener correctamente
            detener();
        }

    }
    public synchronized void detener() {
        if (!iniciado || scheduler == null || scheduler.isShutdown()) {
            // Ya está detenido
            iniciado = false;
            return;
        }
        
        try {
            scheduler.shutdown();
            // Esperar a que terminen las tareas en ejecución
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("El scheduler no terminó después de múltiples intentos");
                }
            }
        } catch (InterruptedException e) {
            // Restaurar el flag de interrupción y forzar cierre
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        } catch (Exception e) {
            // Capturar cualquier otra excepción para evitar que se propague
            LOGGER.log(Level.SEVERE, "Error al detener verificador automático", e);
        } finally {
            iniciado = false;
            LOGGER.info("Verificador automático detenido");
        }
    }
      /**
     * Reinicia el verificador automático.
     */
    public synchronized void reiniciar() {
        detener();
        iniciar();
    }
        /**
     * Verifica si el verificador está ejecutándose.
     * @return true si está ejecutándose, false en caso contrario
     */
    public boolean estaIniciado() {
        return iniciado && scheduler != null && !scheduler.isShutdown();
    }
   
}
