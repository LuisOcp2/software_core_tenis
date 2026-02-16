package raven.application.form.productos.dto;

import java.util.Date;

public class MovimientoItem {
    private int idMovimiento;
    private Date fecha;
    private String tipoMovimiento; // "entrada par", "salida caja", etc.
    private int cantidad;
    private int cantidadPares;
    private String tipoReferencia; // "venta", "compra", "ajuste"
    private String usuario;
    private String observacion;

    public MovimientoItem() {
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getCantidadPares() {
        return cantidadPares;
    }

    public void setCantidadPares(int cantidadPares) {
        this.cantidadPares = cantidadPares;
    }

    public String getTipoReferencia() {
        return tipoReferencia;
    }

    public void setTipoReferencia(String tipoReferencia) {
        this.tipoReferencia = tipoReferencia;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public boolean isEntrada() {
        if (tipoReferencia == null)
            return false;
        String type = tipoReferencia.toLowerCase();
        return type.contains("compra") || type.contains("ajuste_entrada") ||
                type.contains("devolucion") || // Devolucion de cliente a nosotros es entrada
                type.contains("traspaso_entrada");
    }

    public boolean isSalida() {
        if (tipoReferencia == null)
            return false;
        String type = tipoReferencia.toLowerCase();
        return type.contains("venta") || type.contains("ajuste_salida") ||
                type.contains("traspaso_salida");
    }
}
