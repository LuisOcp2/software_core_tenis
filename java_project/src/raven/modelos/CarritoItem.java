package raven.modelos;

import java.util.Date;

/**
 * Modelo que representa un ítem en el carrito de compras web.
 * Tabla: carrito
 */
public class CarritoItem {

    private int idCarrito;
    private Integer usuarioId;
    private String sessionId;
    private int idProducto;
    private int idVariante;
    private Integer idBodega;
    private int cantidad;
    private double precioUnitario;
    private Date fechaAgregado;
    private Date fechaActualizado;

    // Campos relacionados (JOIN)
    private String nombreProducto;
    private String codigoModelo;
    private String talla;
    private String color;
    private String nombreBodega;
    private int stockDisponible;
    private String sku;

    public CarritoItem() {}

    // --- Getters y Setters ---
    public int getIdCarrito() { return idCarrito; }
    public void setIdCarrito(int idCarrito) { this.idCarrito = idCarrito; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public int getIdVariante() { return idVariante; }
    public void setIdVariante(int idVariante) { this.idVariante = idVariante; }

    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Date getFechaAgregado() { return fechaAgregado; }
    public void setFechaAgregado(Date fechaAgregado) { this.fechaAgregado = fechaAgregado; }

    public Date getFechaActualizado() { return fechaActualizado; }
    public void setFechaActualizado(Date fechaActualizado) { this.fechaActualizado = fechaActualizado; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getCodigoModelo() { return codigoModelo; }
    public void setCodigoModelo(String codigoModelo) { this.codigoModelo = codigoModelo; }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getNombreBodega() { return nombreBodega; }
    public void setNombreBodega(String nombreBodega) { this.nombreBodega = nombreBodega; }

    public int getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(int stockDisponible) { this.stockDisponible = stockDisponible; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    /** Subtotal calculado dinámicamente */
    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    @Override
    public String toString() {
        return "CarritoItem{idCarrito=" + idCarrito +
                ", producto='" + nombreProducto + "'" +
                ", talla='" + talla + "'" +
                ", color='" + color + "'" +
                ", cantidad=" + cantidad +
                ", precio=" + precioUnitario + "}";
    }
}
