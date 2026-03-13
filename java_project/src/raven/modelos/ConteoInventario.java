package raven.modelos;

import java.util.Date;

/**
 * Modelo que representa un conteo de inventario.
 */
public class ConteoInventario {

    private int id;
    private String nombre;
    private Date fechaProgramada;
    private String horaProgramada;
    private String tipo; // cajas o pares
    private String tipoConteo; // general, parcial, ciclico, verificacion
    private String estado; // pendiente, en_proceso, completado, cerrado
    private Usuario responsable;
    private String prioridad; // alta, media, baja
    private String observaciones;
    private int totalProductos;
    private int productosContados;
    private int idBodega;

    public ConteoInventario() {
    }

    public ConteoInventario(int id, String nombre, Date fechaProgramada, String horaProgramada,
            String tipo, String tipoConteo, String estado, Usuario responsable,
            String prioridad, String observaciones) {
        this.id = id;
        this.nombre = nombre;
        this.fechaProgramada = fechaProgramada;
        this.horaProgramada = horaProgramada;
        this.tipo = tipo;
        this.tipoConteo = tipoConteo;
        this.estado = estado;
        this.responsable = responsable;
        this.prioridad = prioridad;
        this.observaciones = observaciones;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Date getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(Date fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public String getHoraProgramada() {
        return horaProgramada;
    }

    public void setHoraProgramada(String horaProgramada) {
        this.horaProgramada = horaProgramada;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipoConteo() {
        return tipoConteo;
    }

    public void setTipoConteo(String tipoConteo) {
        this.tipoConteo = tipoConteo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    public void setResponsable(Usuario responsable) {
        this.responsable = responsable;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public int getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(int totalProductos) {
        this.totalProductos = totalProductos;
    }

    public int getProductosContados() {
        return productosContados;
    }

    public void setProductosContados(int productosContados) {
        this.productosContados = productosContados;
    }

    /**
     * Calcula el progreso del conteo como porcentaje.
     * 
     * @return Porcentaje de productos contados.
     */
    public int calcularProgreso() {
        if (totalProductos <= 0) {
            return 0;
        }
        return (int) ((double) productosContados / totalProductos * 100);
    }

    public int getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(int idBodega) {
        this.idBodega = idBodega;
    }
}