package raven.clases.comun;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Monitor de rendimiento del sistema.
 * Registra métricas de uso de memoria y tiempos de ejecución.
 */
public class PerformanceMonitor {
    
    private static final Logger LOGGER = Logger.getLogger(PerformanceMonitor.class.getName());
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * Registra el uso de memoria actual.
     */
    public static void logMemoryUsage() {
        long usedHeap = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxHeap = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        
        System.out.println("INFO Memoria Heap: " + usedHeap + "MB / " + maxHeap + "MB");
        
        if (usedHeap > (maxHeap * 0.85)) {
            LOGGER.log(Level.WARNING, "Alerta de memoria alta: {0}% utilizado", (usedHeap * 100 / maxHeap));
            suggestGC();
        }
    }
    
    /**
     * Sugiere una recolección de basura si la memoria está crítica.
     */
    public static void suggestGC() {
        System.out.println("INFO Sugiriendo limpieza de memoria (GC)...");
        System.gc();
    }
    
    /**
     * Registra el tiempo de ejecución de una operación.
     */
    public static void logExecutionTime(String operation, long durationMs) {
        if (durationMs > 500) {
            LOGGER.log(Level.INFO, "Tiempo: {0} completado en {1}ms", new Object[]{operation, durationMs});
        }
    }
}
