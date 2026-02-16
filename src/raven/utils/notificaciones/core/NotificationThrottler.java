package raven.utils.notificaciones.core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla la frecuencia de notificaciones para evitar saturación.
 */
public class NotificationThrottler {
    
    private final Map<String, LocalDateTime> lastNotificationTimes = new ConcurrentHashMap<>();
    private final long defaultCooldownMinutes;
    
    public NotificationThrottler(long defaultCooldownMinutes) {
        this.defaultCooldownMinutes = defaultCooldownMinutes;
    }
    
    /**
     * Verifica si se debe permitir una notificación basada en su clave única.
     * @param key Clave única de la notificación (ej: USER_1_TRASPASO_123_UPDATE)
     * @return true si se permite, false si está en periodo de enfriamiento
     */
    public boolean shouldAllow(String key) {
        return shouldAllow(key, defaultCooldownMinutes);
    }
    
    public boolean shouldAllow(String key, long cooldownMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastNotificationTimes.get(key);
        
        if (lastTime == null) {
            lastNotificationTimes.put(key, now);
            return true;
        }
        
        long minutesSinceLast = Duration.between(lastTime, now).toMinutes();
        if (minutesSinceLast >= cooldownMinutes) {
            lastNotificationTimes.put(key, now);
            return true;
        }
        
        return false;
    }
    
    public void reset(String key) {
        lastNotificationTimes.remove(key);
    }
    
    public void clear() {
        lastNotificationTimes.clear();
    }
}
