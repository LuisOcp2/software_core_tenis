package raven.utils.notificaciones.core;

import raven.controlador.admin.ModelNotificacion;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de caché inteligente para notificaciones con TTL.
 */
public class NotificationCache {
    
    private static final long DEFAULT_TTL_MS = 30000; // 30 segundos
    
    private static class CacheEntry {
        final List<ModelNotificacion> data;
        final long timestamp;
        
        CacheEntry(List<ModelNotificacion> data) {
            this.data = new ArrayList<>(data);
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isValid(long ttl) {
            return (System.currentTimeMillis() - timestamp) < ttl;
        }
    }
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    public void put(String key, List<ModelNotificacion> data) {
        if (key != null && data != null) {
            cache.put(key, new CacheEntry(data));
        }
    }
    
    public List<ModelNotificacion> get(String key) {
        if (key == null) return null;
        
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.isValid(DEFAULT_TTL_MS)) {
            return new ArrayList<>(entry.data);
        }
        
        if (entry != null) {
            cache.remove(key); // Expired
        }
        return null;
    }
    
    public void invalidate(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }
    
    public void invalidateAll() {
        cache.clear();
    }
}
