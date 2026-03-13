package raven.modelos;

/**
 * Modelo que representa un ítem del detalle de una orden de reserva.
 * Tabla: ordenes_reserva_detalle
 */
public class OrdenReservaDetalle {

    private int idDetalle;
    private int idOrden;
    private int idProducto;
    private int idVariante;
    private int idBodega;
    private int cantidad;
    private int cantidadEnviada;
    private String estado;
    private String observacion;

    // Campos extendidos via JOIN
    private String nombreProducto;
    private String codigoProducto;
    private String talla;
    private String color;
    private String sku;
    private double precio;
    private double subtotal;

    public OrdenReservaDetalle() {}

    // --- Getters y Setters ---
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public int getIdVariante() { return idVariante; }
    public void setIdVariante(int idVariante) { this.idVariante = idVariante; }

    public int getIdBodega() { return idBodega; }
    public void setIdBodega(int idBodega) { this.idBodega = idBodega; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getCantidadEnviada() { return cantidadEnviada; }
    public void setCantidadEnviada(int cantidadEnviada) { this.cantidadEnviada = cantidadEnviada; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    @Override
    public String toString() {
        return "OrdenDetalle{idDetalle=" + idDetalle +
                ", producto='" + nombreProducto + "'" +
                ", talla='" + talla + "'" +
                ", color='" + color + "'" +
                ", cantidad=" + cantidad +
                ", precio=" + precio + "}";
    }
}
