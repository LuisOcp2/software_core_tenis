package raven.application.form.productos.dto;

import java.util.ArrayList;
import java.util.List;

public class ProductoAgrupado {
    private int idBodega;
    private String nombreBodega;
    private int idProducto;
    private String codigoModelo;
    private String nombreProducto;
    private String nombreMarca;
    private String nombreColor;
    private String category;
    private String ubicacion;
    private javax.swing.Icon cachedIcon;
    private List<TallaInfo> tallas = new ArrayList<>();

    // Totals
    private int totalPares;
    private int totalCajas;

    public void addTalla(TallaInfo talla) {
        tallas.add(talla);
        totalPares += talla.getStockPares();
        totalCajas += talla.getStockCajas();
    }

    // Getters and Setters
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public List<TallaInfo> getTallas() {
        return tallas;
    }

    public int getTotalPares() {
        return totalPares;
    }

    public int getTotalCajas() {
        return totalCajas;
    }

    public javax.swing.Icon getCachedIcon() {
        return cachedIcon;
    }

    public void setCachedIcon(javax.swing.Icon cachedIcon) {
        this.cachedIcon = cachedIcon;
    }

    // For ProductCardRenderer compatibility, we might need a toString or adapter
    // logic,
    // but the renderer uses specific fields. We will adapt the form logic.
}
