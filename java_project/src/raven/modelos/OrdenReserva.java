package raven.modelos;

import java.util.Date;

/**
 * Modelo enriquecido de una orden de reserva (pedido web).
 * Incluye campos extendidos: totales, dirección, método de pago.
 */
public class OrdenReserva {

    // Campos tabla ordenes_reserva
    private int idOrden;
    private int idUsuario;
    private int idBodega;
    private Date fechaCreacion;
    private String estado;
    private Date fechaRetirado;
    private Date fechaPagado;
    private Date fechaFinalizado;
    private String motivoCancelacion;
    private double subtotal;
    private double impuestos;
    private double total;
    private String metodoPago;
    private String notas;
    private String direccion;
    private String ciudad;
    private String departamento;
    private Date fechaVencimiento;

    // Campos relacionados (JOIN)
    private String nombreUsuario;
    private String nombreBodega;
    private int cantidadProductos;

    public OrdenReserva() {}

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

    // --- Getters y Setters ---
    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdBodega() { return idBodega; }
    public void setIdBodega(int idBodega) { this.idBodega = idBodega; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Date getFechaRetirado() { return fechaRetirado; }
    public void setFechaRetirado(Date fechaRetirado) { this.fechaRetirado = fechaRetirado; }

    public Date getFechaPagado() { return fechaPagado; }
    public void setFechaPagado(Date fechaPagado) { this.fechaPagado = fechaPagado; }

    public Date getFechaFinalizado() { return fechaFinalizado; }
    public void setFechaFinalizado(Date fechaFinalizado) { this.fechaFinalizado = fechaFinalizado; }

    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getImpuestos() { return impuestos; }
    public void setImpuestos(double impuestos) { this.impuestos = impuestos; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public Date getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(Date fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombreBodega() { return nombreBodega; }
    public void setNombreBodega(String nombreBodega) { this.nombreBodega = nombreBodega; }

    public int getCantidadProductos() { return cantidadProductos; }
    public void setCantidadProductos(int cantidadProductos) { this.cantidadProductos = cantidadProductos; }

    /** Etiqueta visual para el badge de estado */
    public String getEstadoLabel() {
        if (estado == null) return "";
        switch (estado.toLowerCase()) {
            case "pendiente":  return "⏳ Pendiente";
            case "retirado":   return "📦 Retirado";
            case "pagado":     return "✅ Pagado";
            case "finalizado": return "🏁 Finalizado";
            case "cancelado":  return "❌ Cancelado";
            default: return estado;
        }
    }

    @Override
    public String toString() {
        return "OrdenReserva{idOrden=" + idOrden +
                ", idUsuario=" + idUsuario +
                ", estado='" + estado + "'" +
                ", total=" + total +
                ", nombreUsuario='" + nombreUsuario + "'" +
                ", nombreBodega='" + nombreBodega + "'" + "}";
    }
}
