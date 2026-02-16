/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.admin;

/**
 *
 * @author CrisDEV
 * Modelo de bodegas
 * 
 */
public class ModelBodegas {
    private Integer idBodega;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String responsable;
    private String tipo; // principal, sucursal, deposito, temporal
    private Integer capacidadMaxima; // puede ser NULL
    private Boolean activa; // 1/0

    public Integer getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    // =============================================================
    // Métodos auxiliares para UI
    // =============================================================
    public Object[] toTableRow(int rowNum) {
        return new Object[]{
            Boolean.FALSE, // columna de selección
            codigo,
            nombre,
            direccion,
            responsable,
            tipo
        };
    }
    @Override
    public String toString() {
        return nombre != null ? nombre : "Bodega";
    }
}
