package raven.application.form.other.buscador.dto;
import java.math.BigDecimal;

    /* 
    * Data Transfer Object para Variante de Producto
    * Contiene información de talla, color y stock disponible
    *@author CrisDEV
    *@version 2.1 - Ajustado sin ubicacion_bodega/ubicacion_tienda
    */

public class VarianteDTO {
    // Identificadores
    private Integer idVariante;
    private Integer idProducto;
    
    // Características de la variante
    private String talla;
    private String color;
    private String ean; // Código de barras
    private String sku; // Stock Keeping Unit
    
    // Precios
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    
    // Stock (calculado según bodega si se filtra)
    private Integer stockDisponible; // Stock disponible para venta
    private Integer stockReservado;  // Stock reservado
    private Integer stockPares;      // Total de pares
    private Integer stockCaja;       // Total por caja
    // Ubicación específica (si se filtra por bodega)
    private String ubicacionEspecifica;
    
    // Estado
    private Boolean disponible; // Si está disponible para venta
    
    // Constructor por defecto
    public VarianteDTO() {
        this.disponible = true;
    }

    /**
     * Constructor con datos básicos
     */
    public VarianteDTO(Integer idVariante, Integer idProducto, String talla, 
                       String color, String ean, String sku) {
        this.idVariante = idVariante;
        this.idProducto = idProducto;
        this.talla = talla;
        this.color = color;
        this.ean = ean;
        this.sku = sku;
        this.disponible = true;
    }
    
    // Getters y Setters
    
    public Integer getIdVariante() {
        return idVariante;
    }
    
    public void setIdVariante(Integer idVariante) {
        this.idVariante = idVariante;
    }
    
    public Integer getIdProducto() {
        return idProducto;
    }
    
    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
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
    
    public String getEan() {
        return ean;
    }
    
    public void setEan(String ean) {
        this.ean = ean;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }
    
    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }
    
    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }
    
    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }
    
    public Integer getStockDisponible() {
        return stockDisponible;
    }
    
    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }
    
    public Integer getStockReservado() {
        return stockReservado;
    }
    
    public void setStockReservado(Integer stockReservado) {
        this.stockReservado = stockReservado;
    }
    
    public Integer getStockPares() {
        return stockPares;
    }
    
    public void setStockPares(Integer stockPares) {
        this.stockPares = stockPares;
    }
    
    public Integer getStockCaja() {
        return stockCaja;
    }
    
    public void setStockCaja(Integer stockCaja) {
        this.stockCaja = stockCaja;
    }
    
    public String getUbicacionEspecifica() {
        return ubicacionEspecifica;
    }
    
    public void setUbicacionEspecifica(String ubicacionEspecifica) {
        this.ubicacionEspecifica = ubicacionEspecifica;
    }
    
    public Boolean getDisponible() {
        return disponible;
    }
    
    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }
    
    /**
     * Verifica si hay stock disponible
     * @return true si hay stock > 0
     */
    public boolean hayStock() {
        return stockDisponible != null && stockDisponible > 0;
    }
    
    /**
     * Calcula el total de unidades considerando pares y caja
     * @return total de unidades
     */
    public int getTotalUnidades() {
        int pares = (stockPares != null) ? stockPares : 0;
        int caja = (stockCaja != null) ? stockCaja : 0;
        return pares + caja;
    }
    
    /**
     * Retorna una descripción completa de la variante
     * @return descripción en formato: "Talla: X - Color: Y"
     */
    public String getDescripcionCompleta() {
        return String.format("Talla: %s - Color: %s", 
                talla != null ? talla : "N/A", 
                color != null ? color : "N/A");
    }
    
    @Override
    public String toString() {
        return String.format("VarianteDTO[id=%d, talla=%s, color=%s, stock=%d]",
                idVariante, talla, color, stockDisponible);
    }

    
    
}
