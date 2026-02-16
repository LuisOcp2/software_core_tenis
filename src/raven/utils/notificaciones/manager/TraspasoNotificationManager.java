package raven.utils.notificaciones.manager;

import raven.controlador.principal.conexion;
import raven.utils.notificaciones.core.NotificationThrottler;
import raven.utils.notificaciones.enums.NotificationType;
import raven.utils.notificaciones.enums.NotificationCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Gestor especializado para notificaciones de traspasos.
 * Implementa lógica de negocio específica, prioridades y throttling.
 */
public class TraspasoNotificationManager {

    private static final String SP_NOTIF_TRASPASO = "CALL sp_notificar_traspaso_evento(?, ?, ?, ?)";
    private final NotificationThrottler throttler;
    
    // Singleton
    private static TraspasoNotificationManager instance;
    
    public static synchronized TraspasoNotificationManager getInstance() {
        if (instance == null) {
            instance = new TraspasoNotificationManager();
        }
        return instance;
    }

    private TraspasoNotificationManager() {
        // Cooldown de 5 minutos por defecto para evitar duplicados rápidos
        this.throttler = new NotificationThrottler(5);
    }

    /**
     * Notifica un evento de traspaso aplicando reglas de negocio.
     */
    public void notificarEvento(int idTraspaso, String evento, String titulo, String mensaje) throws SQLException {
        // Generar clave única para throttling
        String throttleKey = "TRASPASO_" + idTraspaso + "_" + evento;
        
        // Verificar si debemos enviar esta notificación (evitar spam)
        if (!throttler.shouldAllow(throttleKey)) {
            System.out.println("INFO: Notificación suprimida por throttling: " + throttleKey);
            return;
        }

        // Determinar tipo basado en evento (lógica de negocio)
        NotificationType tipo = determinarTipoPorEvento(evento);
        
        // Ejecutar procedimiento almacenado
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(SP_NOTIF_TRASPASO)) {
            ps.setInt(1, idTraspaso);
            ps.setString(2, evento);
            ps.setString(3, titulo);
            ps.setString(4, mensaje);
            // Nota: El SP actual no acepta el tipo como parámetro, lo infiere o usa default.
            // Si el SP se actualiza, podríamos pasar tipo.getDbValue()
            ps.execute();
        }
    }
    
    private NotificationType determinarTipoPorEvento(String evento) {
        if (evento == null) return NotificationType.INFO;
        
        switch (evento.toLowerCase()) {
            case "solicitud":
            case "cancelado":
                return NotificationType.URGENT;
            case "autorizado":
            case "recibido":
                return NotificationType.SUCCESS;
            case "enviado":
                return NotificationType.WARNING; // Warning para llamar atención
            default:
                return NotificationType.INFO;
        }
    }

    // Métodos helper específicos
    
    public void notificarSolicitud(int idTraspaso, String usuario) throws SQLException {
        notificarEvento(idTraspaso, "solicitud", "Nueva Solicitud de Traspaso", "Usuario " + usuario + " ha solicitado un traspaso.");
    }
    
    public void notificarAutorizacion(int idTraspaso) throws SQLException {
        notificarEvento(idTraspaso, "autorizado", "Traspaso Autorizado", "El traspaso #" + idTraspaso + " ha sido autorizado.");
    }
    
    public void notificarEnvio(int idTraspaso) throws SQLException {
        notificarEvento(idTraspaso, "enviado", "Traspaso Enviado", "El traspaso #" + idTraspaso + " ha sido enviado y está en tránsito.");
    }
    
    public void notificarRecepcion(int idTraspaso) throws SQLException {
        notificarEvento(idTraspaso, "recibido", "Traspaso Recibido", "El traspaso #" + idTraspaso + " ha sido recibido exitosamente.");
    }
    
    public void notificarCancelacion(int idTraspaso, String motivo) throws SQLException {
        notificarEvento(idTraspaso, "cancelado", "Traspaso Cancelado", "El traspaso #" + idTraspaso + " ha sido cancelado. Motivo: " + motivo);
    }
}
