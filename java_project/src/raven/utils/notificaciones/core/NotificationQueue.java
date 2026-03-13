package raven.utils.notificaciones.core;

import raven.controlador.admin.ModelNotificacion;
import raven.utils.notificaciones.enums.NotificationPriority;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.ArrayList;
import java.util.List;

/**
 * Cola de notificaciones con prioridades diferenciadas.
 * Permite procesar notificaciones urgentes antes que las informativas.
 */
public class NotificationQueue {
    
    // Comparador que ordena por prioridad (menor valor = mayor prioridad) y luego por fecha (más reciente primero)
    private static final Comparator<ModelNotificacion> PRIORITY_COMPARATOR = (n1, n2) -> {
        int p1 = NotificationPriority.fromString(n1.getTipo()).getValue();
        int p2 = NotificationPriority.fromString(n2.getTipo()).getValue();
        
        if (p1 != p2) {
            return Integer.compare(p1, p2);
        }
        
        // Si tienen la misma prioridad, la más reciente va primero
        if (n1.getFechaCreacion() != null && n2.getFechaCreacion() != null) {
            return n2.getFechaCreacion().compareTo(n1.getFechaCreacion());
        }
        return 0;
    };

    private final BlockingQueue<ModelNotificacion> queue;

    public NotificationQueue() {
        this.queue = new PriorityBlockingQueue<>(100, PRIORITY_COMPARATOR);
    }

    public void add(ModelNotificacion notificacion) {
        if (notificacion != null) {
            queue.offer(notificacion);
        }
    }

    public void addAll(List<ModelNotificacion> notificaciones) {
        if (notificaciones != null) {
            for (ModelNotificacion n : notificaciones) {
                add(n);
            }
        }
    }

    public ModelNotificacion poll() {
        return queue.poll();
    }
    
    public List<ModelNotificacion> drainAll() {
        List<ModelNotificacion> list = new ArrayList<>();
        queue.drainTo(list);
        return list;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    public int size() {
        return queue.size();
    }
    
    public void clear() {
        queue.clear();
    }
}
