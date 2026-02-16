/**
 * MODELOS CORREGIDOS para manejo de devoluciones
 * Aseguran compatibilidad con la base de datos y lógica de negocio
 */

// ====================================================================
// ModelDevolucion.java CORREGIDO
// ====================================================================
package raven.controlador.comercial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ModelDevolucion {

    private int idDevolucion;
    private String numeroDevolucion;
    private int idVenta;
    private int idCliente;
    private int idUsuarioProcesa;
    private LocalDateTime fechaDevolucion;
    private TipoDevolucion tipoDevolucion;
    private MotivoDevolucion motivo;
    private EstadoDevolucion estado;
    private BigDecimal subtotalDevolucion;
    private BigDecimal ivaDevolucion;
    private BigDecimal totalDevolucion;
    private String observaciones;
    private boolean requiereAutorizacion;
    private Integer idUsuarioAutoriza;
    private LocalDateTime fechaAutorizacion;
    private String observacionesAutorizacion;
    private LocalDateTime fechaLimiteDevolucion;
    private boolean activa;

    // Campos adicionales para UI
    private String numeroNotaCredito;
    private BigDecimal saldoNotaCredito;

    // Nombres para UI (Evitar IDs)
    private String nombreCliente;
    private String nombreUsuarioProcesa; // Quien hace la devolución
    private String nombreUsuarioVenta; // Quien hizo la venta original
    private BigDecimal totalVenta;

    // Constructor
    public ModelDevolucion() {
        this.estado = EstadoDevolucion.PENDIENTE;
        this.subtotalDevolucion = BigDecimal.ZERO;
        this.ivaDevolucion = BigDecimal.ZERO;
        this.totalDevolucion = BigDecimal.ZERO;
        this.requiereAutorizacion = false;
        this.activa = true;
    }

    // Getters y Setters
    public int getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(int idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public String getNumeroDevolucion() {
        return numeroDevolucion;
    }

    public void setNumeroDevolucion(String numeroDevolucion) {
        this.numeroDevolucion = numeroDevolucion;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdUsuarioProcesa() {
        return idUsuarioProcesa;
    }

    public void setIdUsuarioProcesa(int idUsuarioProcesa) {
        this.idUsuarioProcesa = idUsuarioProcesa;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public TipoDevolucion getTipoDevolucion() {
        return tipoDevolucion;
    }

    public void setTipoDevolucion(TipoDevolucion tipoDevolucion) {
        this.tipoDevolucion = tipoDevolucion;
    }

    public MotivoDevolucion getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoDevolucion motivo) {
        this.motivo = motivo;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public void setEstado(EstadoDevolucion estado) {
        this.estado = estado;
    }

    public BigDecimal getSubtotalDevolucion() {
        return subtotalDevolucion;
    }

    public void setSubtotalDevolucion(BigDecimal subtotalDevolucion) {
        this.subtotalDevolucion = subtotalDevolucion;
    }

    public BigDecimal getIvaDevolucion() {
        return ivaDevolucion;
    }

    public void setIvaDevolucion(BigDecimal ivaDevolucion) {
        this.ivaDevolucion = ivaDevolucion;
    }

    public BigDecimal getTotalDevolucion() {
        return totalDevolucion;
    }

    public void setTotalDevolucion(BigDecimal totalDevolucion) {
        this.totalDevolucion = totalDevolucion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isRequiereAutorizacion() {
        return requiereAutorizacion;
    }

    public void setRequiereAutorizacion(boolean requiereAutorizacion) {
        this.requiereAutorizacion = requiereAutorizacion;
    }

    public Integer getIdUsuarioAutoriza() {
        return idUsuarioAutoriza;
    }

    public void setIdUsuarioAutoriza(Integer idUsuarioAutoriza) {
        this.idUsuarioAutoriza = idUsuarioAutoriza;
    }

    public LocalDateTime getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) {
        this.fechaAutorizacion = fechaAutorizacion;
    }

    public String getObservacionesAutorizacion() {
        return observacionesAutorizacion;
    }

    public void setObservacionesAutorizacion(String observacionesAutorizacion) {
        this.observacionesAutorizacion = observacionesAutorizacion;
    }

    public LocalDateTime getFechaLimiteDevolucion() {
        return fechaLimiteDevolucion;
    }

    public void setFechaLimiteDevolucion(LocalDateTime fechaLimiteDevolucion) {
        this.fechaLimiteDevolucion = fechaLimiteDevolucion;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public String getNumeroNotaCredito() {
        return numeroNotaCredito;
    }

    public void setNumeroNotaCredito(String numeroNotaCredito) {
        this.numeroNotaCredito = numeroNotaCredito;
    }

    public BigDecimal getSaldoNotaCredito() {
        return saldoNotaCredito;
    }

    public void setSaldoNotaCredito(BigDecimal saldoNotaCredito) {
        this.saldoNotaCredito = saldoNotaCredito;
    }

    // Getters y Setters para campos de UI
    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getNombreUsuarioProcesa() {
        return nombreUsuarioProcesa;
    }

    public void setNombreUsuarioProcesa(String nombreUsuarioProcesa) {
        this.nombreUsuarioProcesa = nombreUsuarioProcesa;
    }

    public String getNombreUsuarioVenta() {
        return nombreUsuarioVenta;
    }

    public void setNombreUsuarioVenta(String nombreUsuarioVenta) {
        this.nombreUsuarioVenta = nombreUsuarioVenta;
    }

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }

    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta;
    }

    // Enums
    public enum TipoDevolucion {
        TOTAL("total"),
        PARCIAL("parcial");

        private final String valor;

        TipoDevolucion(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }

        public static TipoDevolucion fromString(String valor) {
            for (TipoDevolucion tipo : TipoDevolucion.values()) {
                if (tipo.valor.equals(valor)) {
                    return tipo;
                }
            }
            return PARCIAL;
        }
    }

    public enum MotivoDevolucion {
        DEFECTO_FABRICA("defecto_fabrica", "Defecto de Fábrica"),
        TALLA_INCORRECTA("talla_incorrecta", "Talla Incorrecta"),
        PRODUCTO_DANADO("producto_dañado", "Producto Dañado"),
        INSATISFACCION("insatisfaccion", "Insatisfacción"),
        ERROR_FACTURACION("error_facturacion", "Error de Facturación"),
        CAMBIO_MODELO("cambio_modelo", "Cambio de Modelo"),
        OTROS("otros", "Otros");

        private final String valor;
        private final String descripcion;

        MotivoDevolucion(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }

        public String getValor() {
            return valor;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public static MotivoDevolucion fromString(String valor) {
            for (MotivoDevolucion motivo : MotivoDevolucion.values()) {
                if (motivo.valor.equals(valor)) {
                    return motivo;
                }
            }
            return OTROS;
        }
    }

    public enum EstadoDevolucion {
        PENDIENTE("pendiente", "Pendiente"),
        PROCESANDO("procesando", "Procesando"),
        APROBADA("aprobada", "Aprobada"),
        RECHAZADA("rechazada", "Rechazada"),
        FINALIZADA("finalizada", "Finalizada"),
        ANULADA("anulada", "Anulada");

        private final String valor;
        private final String descripcion;

        EstadoDevolucion(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }

        public String getValor() {
            return valor;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public static EstadoDevolucion fromString(String valor) {
            for (EstadoDevolucion estado : EstadoDevolucion.values()) {
                if (estado.valor.equals(valor)) {
                    return estado;
                }
            }
            return PENDIENTE;
        }
    }
}