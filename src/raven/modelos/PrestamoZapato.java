package raven.modelos;

import java.sql.Timestamp;

/**
 * Modelo para la tabla prestamos_zapatos.
 * Incluye campos base y algunos campos de visualización relacionados.
 */
public class PrestamoZapato {
    // Campos base (tabla prestamos_zapatos)
    private int idPrestamo;
    private Integer idBodega;
    private Integer idProducto;
    private Integer idVariante;
    private String pie; // IZQUIERDO / DERECHO / AMBOS
    private String nombrePrestatario;
    private String celularPrestatario;
    private String direccionPrestatario;
    private String estado; // PRESTADO / DEVUELTO
    private Timestamp fechaPrestamo;
    private Timestamp fechaDevolucion;
    private Integer idUsuario;
    private String observaciones;

    // Campos adicionales para mostrar información relacionada
    private String nombreBodega;
    private String nombreProducto;
    private String talla;
    private String color;
    // Indicador no persistente: si el pie proviene de un par nuevo
    private boolean nuevoPar;

    public PrestamoZapato() {}

    // Getters y Setters
    public int getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(int idPrestamo) { this.idPrestamo = idPrestamo; }

    public Integer getIdBodega() { return idBodega; }
    public void setIdBodega(Integer idBodega) { this.idBodega = idBodega; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public Integer getIdVariante() { return idVariante; }
    public void setIdVariante(Integer idVariante) { this.idVariante = idVariante; }

    public String getPie() { return pie; }
    public void setPie(String pie) { this.pie = pie; }

    public String getNombrePrestatario() { return nombrePrestatario; }
    public void setNombrePrestatario(String nombrePrestatario) { this.nombrePrestatario = nombrePrestatario; }

    public String getCelularPrestatario() { return celularPrestatario; }
    public void setCelularPrestatario(String celularPrestatario) { this.celularPrestatario = celularPrestatario; }

    public String getDireccionPrestatario() { return direccionPrestatario; }
    public void setDireccionPrestatario(String direccionPrestatario) { this.direccionPrestatario = direccionPrestatario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Timestamp getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(Timestamp fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }

    public Timestamp getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(Timestamp fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getNombreBodega() { return nombreBodega; }
    public void setNombreBodega(String nombreBodega) { this.nombreBodega = nombreBodega; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isNuevoPar() { return nuevoPar; }
    public void setNuevoPar(boolean nuevoPar) { this.nuevoPar = nuevoPar; }

    // Utilidades
    public boolean estaPrestado() { return "PRESTADO".equalsIgnoreCase(estado); }
    public boolean estaDevuelto() { return "DEVUELTO".equalsIgnoreCase(estado); }
}