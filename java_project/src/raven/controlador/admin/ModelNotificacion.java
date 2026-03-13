package raven.controlador.admin;

import java.time.LocalDateTime;

/**
 * Modelo de notificación para la tabla `notificaciones`.
 */
public class ModelNotificacion {

    private final int idNotificacion;
    private final String titulo;
    private final String mensaje;
    private final String tipo;           // info | warning | urgent | error | success
    private final String categoria;      // ventas | inventario | sistema, etc.
    private final Integer idUsuarioDestinatario;
    private final boolean paraTodos;
    private final Integer idReferencia;  // id de venta / orden / traspaso
    private final String tipoReferencia; // ordenes_reserva | traspasos | ventas
    private final boolean leida;
    private final boolean activa;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaLectura;
    private String evento;              // evento específico del traspaso (solicitud, enviado, recibido, etc.)

    public ModelNotificacion(int idNotificacion,
                             String titulo,
                             String mensaje,
                             String tipo,
                             String categoria,
                             Integer idUsuarioDestinatario,
                             boolean paraTodos,
                             Integer idReferencia,
                             String tipoReferencia,
                             boolean leida,
                             boolean activa,
                             LocalDateTime fechaCreacion,
                             LocalDateTime fechaLectura) {
        this.idNotificacion = idNotificacion;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.categoria = categoria;
        this.idUsuarioDestinatario = idUsuarioDestinatario;
        this.paraTodos = paraTodos;
        this.idReferencia = idReferencia;
        this.tipoReferencia = tipoReferencia;
        this.leida = leida;
        this.activa = activa;
        this.fechaCreacion = fechaCreacion;
        this.fechaLectura = fechaLectura;
        this.evento = null; // Valor por defecto
    }

    public int getIdNotificacion() { return idNotificacion; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public String getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public Integer getIdUsuarioDestinatario() { return idUsuarioDestinatario; }
    public boolean isParaTodos() { return paraTodos; }
    public Integer getIdReferencia() { return idReferencia; }
    public String getTipoReferencia() { return tipoReferencia; }
    public boolean isLeida() { return leida; }
    public boolean isActiva() { return activa; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaLectura() { return fechaLectura; }
    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }
}