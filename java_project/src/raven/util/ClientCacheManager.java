package raven.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de caché del lado del cliente para optimizar la aplicación.
 * Permite almacenar resultados de consultas frecuentes para reducir la carga
 * en la base de datos y mejorar la latencia percibida.
 */
public class ClientCacheManager {
    private static ClientCacheManager instance;
    private final Map<String, Object> cache;
    private final Map<String, Long> expiry;

    private ClientCacheManager() {
        cache = new ConcurrentHashMap<>();
        expiry = new ConcurrentHashMap<>();
    }

    public static synchronized ClientCacheManager getInstance() {
        if (instance == null) {
            instance = new ClientCacheManager();
        }
        return instance;
    }

    /**
     * Guarda un objeto en caché con un tiempo de vida (TTL).
     * @param key Clave única
     * @param value Objeto a guardar
     * @param ttlMillis Tiempo de vida en milisegundos
     */
    public void put(String key, Object value, long ttlMillis) {
        if (key == null || value == null) return;
        cache.put(key, value);
        expiry.put(key, System.currentTimeMillis() + ttlMillis);
    }

    /**
     * Obtiene un objeto de la caché.
     * @param key Clave única
     * @return El objeto si existe y no ha expirado, null en caso contrario.
     */
    public Object get(String key) {
        if (!cache.containsKey(key)) return null;
        
        Long expireTime = expiry.get(key);
        if (expireTime != null && System.currentTimeMillis() > expireTime) {
            cache.remove(key);
            expiry.remove(key);
            return null;
        }
        return cache.get(key);
    }

    /**
     * Invalida (borra) una entrada específica.
     */
    public void clear(String key) {
        cache.remove(key);
        expiry.remove(key);
    }
    
    /**
     * Invalida todas las entradas que comiencen con un prefijo.
     * Útil para limpiar caché de una entidad completa (ej: "producto_*")
     */
    public void invalidatePattern(String prefix) {
        for (String key : cache.keySet()) {
            if (key.startsWith(prefix)) {
                cache.remove(key);
                expiry.remove(key);
            }
        }
    }

    /**
     * Limpia toda la caché.
     */
    public void clearAll() {
        cache.clear();
        expiry.clear();
    }
}
