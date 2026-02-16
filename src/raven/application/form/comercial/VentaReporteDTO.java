package raven.application.form.comercial;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object para el reporte de ventas
 */
public class VentaReporteDTO {
    private int idVenta;
    private String fecha;
    private String cliente;
    private String vendedor;
    private String estado;
    private String tipoPago;
    private double total;
    private List<Map<String, Object>> detalles;

    public VentaReporteDTO(int idVenta, String fecha, String cliente, String vendedor,
            String estado, String tipoPago, double total,
            List<Map<String, Object>> detalles) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.cliente = cliente;
        this.vendedor = vendedor;
        this.estado = estado;
        this.tipoPago = tipoPago;
        this.total = total;
        this.detalles = detalles;
    }

    // Getters y Setters

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<Map<String, Object>> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<Map<String, Object>> detalles) {
        this.detalles = detalles;
    }
}
