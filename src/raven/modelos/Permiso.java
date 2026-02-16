package raven.modelos;

import java.io.Serializable;

/**
 * Modelo de dominio para permisos del sistema.
 * Representa un módulo o funcionalidad que puede ser controlada por permisos.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class Permiso implements Serializable {

    private int idPermiso;
    private String modulo;
    private String nombreMostrar;
    private String descripcion;
    private String categoria;
    private boolean requiereAdmin;
    private boolean activo;

    public Permiso() {
    }

    public Permiso(int idPermiso, String modulo, String nombreMostrar) {
        this.idPermiso = idPermiso;
        this.modulo = modulo;
        this.nombreMostrar = nombreMostrar;
        this.activo = true;
    }

    // Getters y Setters

    public int getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(int idPermiso) {
        this.idPermiso = idPermiso;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getNombreMostrar() {
        return nombreMostrar;
    }

    public void setNombreMostrar(String nombreMostrar) {
        this.nombreMostrar = nombreMostrar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isRequiereAdmin() {
        return requiereAdmin;
    }

    public void setRequiereAdmin(boolean requiereAdmin) {
        this.requiereAdmin = requiereAdmin;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombreMostrar != null ? nombreMostrar : modulo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Permiso permiso = (Permiso) obj;
        return idPermiso == permiso.idPermiso;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idPermiso);
    }
}
