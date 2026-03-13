package raven.application.form.productos.traspasos;

import java.util.List;
import java.util.Map;

public class TraspasoReporteDTO {
    private String numero;
    private String origen;
    private String destino;
    private String fecha;
    private String estado;
    private String totalProductos;
    private java.math.BigDecimal montoTotal;
    private java.math.BigDecimal montoRecibido;
    private List<Map<String, Object>> detalles;

    public TraspasoReporteDTO() {
    }

    public TraspasoReporteDTO(String numero, String origen, String destino, String fecha, String estado,
            String totalProductos, List<Map<String, Object>> detalles) {
        this.numero = numero;
        this.origen = origen;
        this.destino = destino;
        this.fecha = fecha;
        this.estado = estado;
        this.totalProductos = totalProductos;
        this.detalles = detalles;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(String totalProductos) {
        this.totalProductos = totalProductos;
    }

    public List<Map<String, Object>> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<Map<String, Object>> detalles) {
        this.detalles = detalles;
    }

    public java.math.BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(java.math.BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public java.math.BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(java.math.BigDecimal montoRecibido) {
        this.montoRecibido = montoRecibido;
    }
}
