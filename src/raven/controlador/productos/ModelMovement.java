package raven.controlador.productos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModelMovement {

    private int idMovimiento;
    private int idProducto;
    private String tipoMovimiento;
    private int cantidad;
    private int cantidadPares;
    private LocalDateTime fechaMovimiento;
    private Integer idReferencia;
    private String tipoReferencia;
    private int idUsuario;
    private String observaciones;
    private String nombreProducto;
    private String color;
    private String talla;

    public ModelMovement() {
    }

    public enum TipoMovimiento {
        ENTRADA_CAJA,
        SALIDA_CAJA,
        AJUSTE_CAJA,
        ENTRADA_PAR,
        SALIDA_PAR,
        AJUSTE_PAR
    }

    public ModelMovement(int idMovimiento, int idProducto, String tipoMovimiento, int cantidad, int cantidadPares, LocalDateTime fechaMovimiento, Integer idReferencia, String tipoReferencia, int idUsuario, String observaciones, String nombreProducto, String color, String talla) {
        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.cantidadPares = cantidadPares;
        this.fechaMovimiento = fechaMovimiento;
        this.idReferencia = idReferencia;
        this.tipoReferencia = tipoReferencia;
        this.idUsuario = idUsuario;
        this.observaciones = observaciones;
        this.nombreProducto = nombreProducto;
        this.color = color;
        this.talla = talla;
    }

    public int getCantidadPares() {
        return cantidadPares;
    }

    public void setCantidadPares(int cantidadPares) {
        this.cantidadPares = cantidadPares;
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public Integer getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(Integer idReferencia) {
        this.idReferencia = idReferencia;
    }

    public String getTipoReferencia() {
        return tipoReferencia;
    }

    public void setTipoReferencia(String tipoReferencia) {
        this.tipoReferencia = tipoReferencia;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public Object[] toTableRow(int rowNum) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy ");

        String fechaFormateada = (fechaMovimiento != null)
                ? fechaMovimiento.format(formatter)
                : "N/A"; // Texto alternativo
        return new Object[]{
            nombreProducto, // Muestra el nombre
            color, // Muestra el color
            talla, // Muestra la talla
            tipoMovimiento,
            cantidad,
            fechaMovimiento.format(formatter),
            tipoReferencia,
            observaciones
            };

    }
}
