package raven.controlador.principal;

import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.admin.ModelUser;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ModelVenta {
    private int idVenta;
    private ModelCliente cliente;
    private ModelUser usuario;
    private ModelCaja caja;
    private ModelCajaMovimiento movimiento;
    private LocalDateTime fechaVenta;
    private double subtotal;
    private double descuento;
    private double iva;
    private double total;
    private String estado;
    private String tipoPago;
    private String observaciones;
    private List<ModelDetalleVenta> detalles;
    private boolean esCotizacion = false;
    private boolean elegibleDevolucion = false;

    public boolean isElegibleDevolucion() {
        return elegibleDevolucion;
    }

    public void setElegibleDevolucion(boolean elegibleDevolucion) {
        this.elegibleDevolucion = elegibleDevolucion;
    }

    public boolean isEsCotizacion() {
        return esCotizacion;
    }

    // Getters y Setters
    public void setEsCotizacion(boolean esCotizacion) {
        this.esCotizacion = esCotizacion;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public ModelCliente getCliente() {
        return cliente;
    }

    public void setCliente(ModelCliente cliente) {
        this.cliente = cliente;
    }

    public ModelUser getUsuario() {
        return usuario;
    }

    public void setUsuario(ModelUser usuario) {
        this.usuario = usuario;
    }

    public ModelCaja getCaja() {
        return caja;
    }

    public void setCaja(ModelCaja caja) {
        this.caja = caja;
    }

    public ModelCajaMovimiento getMovimiento() {
        return movimiento;
    }

    public void setMovimiento(ModelCajaMovimiento movimiento) {
        this.movimiento = movimiento;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public double getIva() {
        return iva;
    }

    public void setIva(double iva) {
        this.iva = iva;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<ModelDetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<ModelDetalleVenta> detalles) {
        this.detalles = detalles;
    }

    // Método para establecer estado de cotización
    public void establecerEstadoCotizacion(String estado) {
        // Validar que el estado sea uno de los estados de cotización
        if (Arrays.asList("cotizacion", "cotizacion_aprobada", "cotizacion_rechazada", "cotizacion_convertida").contains(estado)) {
            this.estado = estado;
            this.esCotizacion = true;
        } else {
            throw new IllegalArgumentException("Estado de cotización no válido");
        }
    }

}
