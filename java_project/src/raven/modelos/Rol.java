package raven.modelos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de dominio para roles del sistema.
 * Representa un rol que agrupa permisos y se asigna a usuarios.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class Rol implements Serializable {

    private int idRol;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private boolean esSistema;
    private LocalDateTime fechaCreacion;
    private List<Permiso> permisos;

    public Rol() {
        this.activo = true;
        this.esSistema = false;
        this.permisos = new ArrayList<>();
    }

    public Rol(int idRol, String nombre, String descripcion) {
        this();
        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Getters y Setters

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isEsSistema() {
        return esSistema;
    }

    public void setEsSistema(boolean esSistema) {
        this.esSistema = esSistema;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<Permiso> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<Permiso> permisos) {
        this.permisos = permisos;
    }

    /**
     * Agrega un permiso a la lista de permisos del rol.
     */
    public void agregarPermiso(Permiso permiso) {
        if (!this.permisos.contains(permiso)) {
            this.permisos.add(permiso);
        }
    }

    /**
     * Verifica si el rol tiene un permiso específico.
     */
    public boolean tienePermiso(String modulo) {
        return permisos.stream()
                .anyMatch(p -> p.getModulo().equalsIgnoreCase(modulo));
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Rol rol = (Rol) obj;
        return idRol == rol.idRol;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idRol);
    }
}
