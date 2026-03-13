package raven.controlador.comercial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo para Notas de Crédito
 * 
 * @author Sistema
 * @version 1.0
 */
public class ModelNotaCredito {
    
    private int idNotaCredito;
    private String numeroNotaCredito;
    private int idDevolucion;
    private int idCliente;
    private int idUsuarioGenera;
    private LocalDateTime fechaEmision;
    private TipoNota tipoNota;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private EstadoNota estado;
    private LocalDateTime fechaVencimiento;
    private BigDecimal saldoDisponible;
    private BigDecimal saldoUsado;
    private String observaciones;
    private Integer idVentaAplicada;
    private LocalDateTime fechaAplicacion;
    private boolean activa;
    
    // Datos adicionales para visualización
    private String clienteNombre;
    private String clienteDni;
    private String numeroDevolucion;
    
    public ModelNotaCredito() {
        this.subtotal = BigDecimal.ZERO;
        this.iva = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.saldoDisponible = BigDecimal.ZERO;
        this.saldoUsado = BigDecimal.ZERO;
        this.activa = true;
        this.estado = EstadoNota.EMITIDA;
    }
    
    // Getters y Setters
    public int getIdNotaCredito() { return idNotaCredito; }
    public void setIdNotaCredito(int idNotaCredito) { this.idNotaCredito = idNotaCredito; }
    
    public String getNumeroNotaCredito() { return numeroNotaCredito; }
    public void setNumeroNotaCredito(String numeroNotaCredito) { this.numeroNotaCredito = numeroNotaCredito; }
    
    public int getIdDevolucion() { return idDevolucion; }
    public void setIdDevolucion(int idDevolucion) { this.idDevolucion = idDevolucion; }
    
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    
    public int getIdUsuarioGenera() { return idUsuarioGenera; }
    public void setIdUsuarioGenera(int idUsuarioGenera) { this.idUsuarioGenera = idUsuarioGenera; }
    
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    
    public TipoNota getTipoNota() { return tipoNota; }
    public void setTipoNota(TipoNota tipoNota) { this.tipoNota = tipoNota; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    
    public EstadoNota getEstado() { return estado; }
    public void setEstado(EstadoNota estado) { this.estado = estado; }
    
    public LocalDateTime getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDateTime fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    
    public BigDecimal getSaldoDisponible() { return saldoDisponible; }
    public void setSaldoDisponible(BigDecimal saldoDisponible) { this.saldoDisponible = saldoDisponible; }
    
    public BigDecimal getSaldoUsado() { return saldoUsado; }
    public void setSaldoUsado(BigDecimal saldoUsado) { this.saldoUsado = saldoUsado; }
    
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    
    public Integer getIdVentaAplicada() { return idVentaAplicada; }
    public void setIdVentaAplicada(Integer idVentaAplicada) { this.idVentaAplicada = idVentaAplicada; }
    
    public LocalDateTime getFechaAplicacion() { return fechaAplicacion; }
    public void setFechaAplicacion(LocalDateTime fechaAplicacion) { this.fechaAplicacion = fechaAplicacion; }
    
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    
    public String getClienteDni() { return clienteDni; }
    public void setClienteDni(String clienteDni) { this.clienteDni = clienteDni; }
    
    public String getNumeroDevolucion() { return numeroDevolucion; }
    public void setNumeroDevolucion(String numeroDevolucion) { this.numeroDevolucion = numeroDevolucion; }
    
    // Enums
    public enum TipoNota {
        DEVOLUCION("devolucion", "Devolución"),
        DESCUENTO("descuento", "Descuento"),
        AJUSTE("ajuste", "Ajuste"),
        ERROR_FACTURACION("error_facturacion", "Error de Facturación");
        
        private final String valor;
        private final String descripcion;
        
        TipoNota(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }
        
        public String getValor() { return valor; }
        public String getDescripcion() { return descripcion; }
        
        public static TipoNota fromString(String valor) {
            for (TipoNota tipo : TipoNota.values()) {
                if (tipo.valor.equals(valor)) {
                    return tipo;
                }
            }
            return DEVOLUCION;
        }
    }
    
    public enum EstadoNota {
        EMITIDA("emitida", "Emitida"),
        APLICADA("aplicada", "Aplicada"),
        ANULADA("anulada", "Anulada"),
        VENCIDA("vencida", "Vencida");
        
        private final String valor;
        private final String descripcion;
        
        EstadoNota(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }
        
        public String getValor() { return valor; }
        public String getDescripcion() { return descripcion; }
        
        public static EstadoNota fromString(String valor) {
            for (EstadoNota estado : EstadoNota.values()) {
                if (estado.valor.equals(valor)) {
                    return estado;
                }
            }
            return EMITIDA;
        }
    }
}