package raven.modelos;

import java.util.Date;

/**
 * Modelo que representa una orden de reserva del sistema.
 */
public class OrdenReserva {
    
    private int idOrden;
    private int idUsuario;
    private int idBodega;
    private Date fechaCreacion;
    private String estado;
    private Date fechaRetirado;
    private Date fechaPagado;
    private Date fechaFinalizado;
    private String motivoCancelacion;
    
    // Campos adicionales para mostrar información relacionada
    private String nombreUsuario;
    private String nombreBodega;
    private int cantidadProductos;
    
    public OrdenReserva() {
    }
    
    public OrdenReserva(int idOrden, int idUsuario, int idBodega, Date fechaCreacion, 
                       String estado, Date fechaRetirado, Date fechaPagado, Date fechaFinalizado) {
        this.idOrden = idOrden;
        this.idUsuario = idUsuario;
        this.idBodega = idBodega;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.fechaRetirado = fechaRetirado;
        this.fechaPagado = fechaPagado;
        this.fechaFinalizado = fechaFinalizado;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(int idBodega) {
        this.idBodega = idBodega;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaRetirado() {
        return fechaRetirado;
    }

    public void setFechaRetirado(Date fechaRetirado) {
        this.fechaRetirado = fechaRetirado;
    }

    public Date getFechaPagado() {
        return fechaPagado;
    }

    public void setFechaPagado(Date fechaPagado) {
        this.fechaPagado = fechaPagado;
    }

    public Date getFechaFinalizado() {
        return fechaFinalizado;
    }

    public void setFechaFinalizado(Date fechaFinalizado) {
        this.fechaFinalizado = fechaFinalizado;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreBodega() {
        return nombreBodega;
    }

    public void setNombreBodega(String nombreBodega) {
        this.nombreBodega = nombreBodega;
    }

    public int getCantidadProductos() {
        return cantidadProductos;
    }

    public void setCantidadProductos(int cantidadProductos) {
        this.cantidadProductos = cantidadProductos;
    }
    
    @Override
    public String toString() {
        return "OrdenReserva{" +
                "idOrden=" + idOrden +
                ", idUsuario=" + idUsuario +
                ", idBodega=" + idBodega +
                ", fechaCreacion=" + fechaCreacion +
                ", estado='" + estado + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                ", nombreBodega='" + nombreBodega + '\'' +
                ", cantidadProductos=" + cantidadProductos +
                '}';
    }
}