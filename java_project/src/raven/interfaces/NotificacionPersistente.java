package raven.interfaces;

/**
 * Interfaz para la persistencia de notificaciones.
 * Aplica principio ISP (Interface Segregation Principle).
 */
public interface NotificacionPersistente {
    
    /**
     * Guarda una notificación en el sistema de persistencia
     * 
     * @param mensaje Mensaje de la notificación
     * @param tipo Tipo de notificación (1 = venta antigua, 2 = conteo pendiente, etc.)
     * @param idReferencia ID del objeto asociado
     * @return verdadero si se guarda correctamente
     */
    boolean guardarNotificacion(String mensaje, int tipo, int idReferencia);
    
    /**
     * Marca una notificación como leída
     * 
     * @param idNotificacion ID de la notificación
     * @return verdadero si se actualiza correctamente
     */
    boolean marcarComoLeida(int idNotificacion);
    
    /**
     * Elimina una notificación
     * 
     * @param idNotificacion ID de la notificación
     * @return verdadero si se elimina correctamente
     */
    boolean eliminarNotificacion(int idNotificacion);

    /**
     * Guarda una notificación con detalles adicionales (tipo/categoría/destino).
     * Esta implementación por defecto degrada a guardarNotificacion sencilla para
     * mantener compatibilidad si el backend no soporta columnas extras.
     */
    default boolean guardarNotificacionDetallada(String titulo,
                                                 String mensaje,
                                                 String tipoEnum,
                                                 String categoria,
                                                 Integer idUsuarioDestinatario,
                                                 Boolean paraTodos,
                                                 Integer idReferencia,
                                                 String tipoReferencia) {
        int tipo;
        if ("warning".equalsIgnoreCase(tipoEnum)) tipo = 1;
        else if ("error".equalsIgnoreCase(tipoEnum)) tipo = 3;
        else if ("success".equalsIgnoreCase(tipoEnum)) tipo = 4;
        else if ("urgent".equalsIgnoreCase(tipoEnum)) tipo = 5;
        else tipo = 0; // info
        return guardarNotificacion(mensaje, tipo, idReferencia);
    }
}