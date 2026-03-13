package raven.modelos;

/**
 * Modelo que representa el detalle de una orden de reserva del sistema.
 */
public class OrdenReservaDetalle {
    
    private int idDetalle;
    private int idOrden;
    private int idProducto;
    private int idVariante;
    private int cantidad;
    
    // Campos adicionales para mostrar información del producto
    private String nombreProducto;
    private String codigoProducto;
    private String talla;
    private String color;
    private double precio;
    private double subtotal;
    private String estado;
    
    public OrdenReservaDetalle() {
    }
    
    public OrdenReservaDetalle(int idDetalle, int idOrden, int idProducto, int idVariante, int cantidad) {
        this.idDetalle = idDetalle;
        this.idOrden = idOrden;
        this.idProducto = idProducto;
        this.idVariante = idVariante;
        this.cantidad = cantidad;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    @Override
    public String toString() {
        return "OrdenReservaDetalle{" +
                "idDetalle=" + idDetalle +
                ", idOrden=" + idOrden +
                ", idProducto=" + idProducto +
                ", idVariante=" + idVariante +
                ", cantidad=" + cantidad +
                ", nombreProducto='" + nombreProducto + '\'' +
                ", codigoProducto='" + codigoProducto + '\'' +
                ", talla='" + talla + '\'' +
                ", color='" + color + '\'' +
                ", precio=" + precio +
                ", subtotal=" + subtotal +
                ", estado='" + estado + '\'' +
                '}';
    }
}
