package raven.utils;

import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

/**
 * Utilidad para optimizar el rendimiento de la aplicación.
 * Incluye caché, lazy loading, y gestión de recursos.
 * 
 * @author Kiro AI Assistant
 */
public class PerformanceOptimizer {
    
    private static final int CACHE_SIZE = 100;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    
    // Caché LRU para datos
    private static final Map<String, CacheEntry<?>> cache = 
            Collections.synchronizedMap(new LinkedHashMap<String, CacheEntry<?>>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<?>> eldest) {
                    return size() > CACHE_SIZE;
                }
            });
    
    // Pool de threads para operaciones asíncronas
    private static final ExecutorService executorService = 
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("PerformanceOptimizer-" + t.getId());
                return t;
            });
    
    /**
     * Entrada de caché con timestamp
     */
    private static class CacheEntry<T> {
        final T data;
        final long timestamp;
        
        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * Obtiene datos del caché o ejecuta el supplier si no existe o está expirado
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrCompute(String key, Callable<T> supplier) throws Exception {
        CacheEntry<?> entry = cache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return (T) entry.data;
        }
        
        T data = supplier.call();
        cache.put(key, new CacheEntry<>(data));
        return data;
    }
    
    /**
     * Ejecuta una tarea en background y notifica en el EDT
     */
    public static <T> void executeAsync(
            Callable<T> backgroundTask,
            java.util.function.Consumer<T> onSuccess,
            java.util.function.Consumer<Exception> onError) {
        
        executorService.submit(() -> {
            try {
                T result = backgroundTask.call();
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> onError.accept(e));
            }
        });
    }
    
    /**
     * Limpia el caché
     */
    public static void clearCache() {
        cache.clear();
    }
    
    /**
     * Limpia entradas expiradas del caché
     */
    public static void cleanExpiredCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Invalida una entrada específica del caché
     */
    public static void invalidate(String key) {
        cache.remove(key);
    }
    
    /**
     * Invalida entradas del caché que coincidan con el patrón
     */
    public static void invalidatePattern(String pattern) {
        cache.keySet().removeIf(key -> key.contains(pattern));
    }
    
    /**
     * Obtiene estadísticas del caché
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", cache.size());
        stats.put("maxSize", CACHE_SIZE);
        stats.put("ttlMs", CACHE_TTL_MS);
        
        long expired = cache.values().stream()
                .filter(CacheEntry::isExpired)
                .count();
        stats.put("expired", expired);
        
        return stats;
    }
    
    /**
     * Debounce: Ejecuta una acción solo después de que hayan pasado X ms sin nuevas llamadas
     */
    public static class Debouncer {
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture<?> future;
        
        public void debounce(Runnable action, long delayMs) {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
            
            future = scheduler.schedule(() -> {
                SwingUtilities.invokeLater(action);
            }, delayMs, TimeUnit.MILLISECONDS);
        }
        
        public void shutdown() {
            scheduler.shutdown();
        }
    }
    
    /**
     * Throttle: Limita la frecuencia de ejecución de una acción
     */
    public static class Throttler {
        private long lastExecutionTime = 0;
        private final long minIntervalMs;
        
        public Throttler(long minIntervalMs) {
            this.minIntervalMs = minIntervalMs;
        }
        
        public boolean shouldExecute() {
            long now = System.currentTimeMillis();
            if (now - lastExecutionTime >= minIntervalMs) {
                lastExecutionTime = now;
                return true;
            }
            return false;
        }
    }
    
    /**
     * Batch processor: Agrupa operaciones para ejecutarlas en lote
     */
    public static class BatchProcessor<T> {
        private final List<T> batch = new ArrayList<>();
        private final int batchSize;
        private final java.util.function.Consumer<List<T>> processor;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture<?> future;
        
        public BatchProcessor(int batchSize, java.util.function.Consumer<List<T>> processor) {
            this.batchSize = batchSize;
            this.processor = processor;
        }
        
        public synchronized void add(T item) {
            batch.add(item);
            
            if (batch.size() >= batchSize) {
                flush();
            } else {
                scheduleFlush();
            }
        }
        
        private void scheduleFlush() {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
            
            future = scheduler.schedule(this::flush, 1000, TimeUnit.MILLISECONDS);
        }
        
        public synchronized void flush() {
            if (!batch.isEmpty()) {
                List<T> toProcess = new ArrayList<>(batch);
                batch.clear();
                
                SwingUtilities.invokeLater(() -> processor.accept(toProcess));
            }
        }
        
        public void shutdown() {
            flush();
            scheduler.shutdown();
        }
    }
    
    /**
     * Memory monitor: Monitorea el uso de memoria
     */
    public static class MemoryMonitor {
        public static long getUsedMemoryMB() {
            Runtime runtime = Runtime.getRuntime();
            return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        }
        
        public static long getTotalMemoryMB() {
            return Runtime.getRuntime().totalMemory() / (1024 * 1024);
        }
        
        public static long getMaxMemoryMB() {
            return Runtime.getRuntime().maxMemory() / (1024 * 1024);
        }
        
        public static double getMemoryUsagePercent() {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            long max = runtime.maxMemory();
            return (used * 100.0) / max;
        }
        
        public static void printMemoryStats() {
            System.out.println("=== Memory Stats ===");
            System.out.println("Used: " + getUsedMemoryMB() + " MB");
            System.out.println("Total: " + getTotalMemoryMB() + " MB");
            System.out.println("Max: " + getMaxMemoryMB() + " MB");
            System.out.println("Usage: " + String.format("%.2f%%", getMemoryUsagePercent()));
        }
        
        public static void suggestGC() {
            if (getMemoryUsagePercent() > 80) {
                System.gc();
                System.out.println("INFO: Garbage collection suggested due to high memory usage");
            }
        }
    }
    
    /**
     * Limpieza de recursos al cerrar la aplicación
     */
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        clearCache();
    }
}
