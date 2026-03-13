package raven.application.form.productos.dto;

public class TallaInfo {
    private String nombreTalla;
    private int stockPares;
    private int stockCajas;
    private int idVariante;
    private String ean;

    private String ubicacion;

    public TallaInfo(String nombreTalla, int stockPares, int stockCajas, int idVariante, String ean, String ubicacion) {
        this.nombreTalla = nombreTalla;
        this.stockPares = stockPares;
        this.stockCajas = stockCajas;
        this.idVariante = idVariante;
        this.ean = ean;
        this.ubicacion = ubicacion;
    }

    public String getNombreTalla() {
        return nombreTalla;
    }

    public int getStockPares() {
        return stockPares;
    }

    public int getStockCajas() {
        return stockCajas;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public String getEan() {
        return ean;
    }

    public String getUbicacion() {
        return ubicacion;
    }
}
