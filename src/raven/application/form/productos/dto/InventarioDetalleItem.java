package raven.application.form.productos.dto;

public class InventarioDetalleItem {
    private int idBodega;
    private String nombreBodega;
    private int idProducto;
    private String codigoModelo;
    private String nombreProducto;
    private String nombreMarca;
    private String nombreColor;
    private String nombreTalla;
    private int stockPares;
    private int stockCajas;
    private String ubicacion;
    private String categoria;
    private int idVariante;
    private String ean;

    public InventarioDetalleItem() {
    }

    public InventarioDetalleItem(int idBodega, String nombreBodega, int idProducto, String codigoModelo,
            String nombreProducto, String nombreMarca, String nombreColor, String nombreTalla, int stockPares,
            int stockCajas, String ubicacion, String categoria, int idVariante, String ean) {
        this.idBodega = idBodega;
        this.nombreBodega = nombreBodega;
        this.idProducto = idProducto;
        this.codigoModelo = codigoModelo;
        this.nombreProducto = nombreProducto;
        this.nombreMarca = nombreMarca;
        this.nombreColor = nombreColor;
        this.nombreTalla = nombreTalla;
        this.stockPares = stockPares;
        this.stockCajas = stockCajas;
        this.ubicacion = ubicacion;
        this.categoria = categoria;
        this.idVariante = idVariante;
        this.ean = ean;
    }

    public int getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(int idBodega) {
        this.idBodega = idBodega;
    }

    public String getNombreBodega() {
        return nombreBodega;
    }

    public void setNombreBodega(String nombreBodega) {
        this.nombreBodega = nombreBodega;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoModelo() {
        return codigoModelo;
    }

    public void setCodigoModelo(String codigoModelo) {
        this.codigoModelo = codigoModelo;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getNombreMarca() {
        return nombreMarca;
    }

    public void setNombreMarca(String nombreMarca) {
        this.nombreMarca = nombreMarca;
    }

    public String getNombreColor() {
        return nombreColor;
    }

    public void setNombreColor(String nombreColor) {
        this.nombreColor = nombreColor;
    }

    public String getNombreTalla() {
        return nombreTalla;
    }

    public void setNombreTalla(String nombreTalla) {
        this.nombreTalla = nombreTalla;
    }

    public int getStockPares() {
        return stockPares;
    }

    public void setStockPares(int stockPares) {
        this.stockPares = stockPares;
    }

    public int getStockCajas() {
        return stockCajas;
    }

    public void setStockCajas(int stockCajas) {
        this.stockCajas = stockCajas;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    private javax.swing.Icon cachedIcon;

    public javax.swing.Icon getCachedIcon() {
        return cachedIcon;
    }

    public void setCachedIcon(javax.swing.Icon cachedIcon) {
        this.cachedIcon = cachedIcon;
    }

    @Override
    public String toString() {
        return nombreProducto;
    }
}
