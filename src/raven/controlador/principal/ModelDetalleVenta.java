package raven.controlador.principal;

import raven.controlador.productos.ModelProduct;

public class ModelDetalleVenta {

    private int idDetalle;
    private ModelVenta venta;
    private ModelProduct producto;
    private int cantidad;
    private double precioUnitario;
    private double descuento;
    private double subtotal;
    private String tipoVenta;
    private int idVariante; // ID de la variante del producto
    
    
    // Getters y Setters
    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public ModelVenta getVenta() {
        return venta;
    }

    public void setVenta(ModelVenta venta) {
        this.venta = venta;
    }

    public ModelProduct getProducto() {
        return producto;
    }

    public void setProducto(ModelProduct producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getTipoVenta() {
        return tipoVenta;
    }

    public void setTipoVenta(String tipoVenta) {
        this.tipoVenta = tipoVenta;
    }
    
    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }
    

    @Override
    public String toString() {
        return "ModelDetalleVenta{" + "idDetalle=" + idDetalle + ", venta=" + venta + ", producto=" + producto + ", cantidad=" + cantidad + ", precioUnitario=" + precioUnitario + ", descuento=" + descuento + ", subtotal=" + subtotal + ", idVariante=" + idVariante + "}";
    }

    
}
