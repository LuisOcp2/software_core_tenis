package raven.modelos;

import java.io.Serializable;

/**
 * Modelo de dominio para la relación entre roles y permisos.
 * Define qué acciones (CRUD) puede realizar un rol sobre un permiso específico.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class PrivilegioRol implements Serializable {

    private int idPrivilegio;
    private int idRol;
    private int idPermiso;
    private boolean puedeVer;
    private boolean puedeCrear;
    private boolean puedeEditar;
    private boolean puedeEliminar;

    // Para facilitar el manejo en la UI
    private String nombreRol;
    private String nombrePermiso;
    private String moduloPermiso;

    public PrivilegioRol() {
    }

    public PrivilegioRol(int idRol, int idPermiso) {
        this.idRol = idRol;
        this.idPermiso = idPermiso;
        this.puedeVer = false;
        this.puedeCrear = false;
        this.puedeEditar = false;
        this.puedeEliminar = false;
    }

    /**
     * Establece permisos completos (CRUD) para el privilegio.
     */
    public void setPermisosCompletos() {
        this.puedeVer = true;
        this.puedeCrear = true;
        this.puedeEditar = true;
        this.puedeEliminar = true;
    }

    /**
     * Establece solo permiso de lectura.
     */
    public void setSoloLectura() {
        this.puedeVer = true;
        this.puedeCrear = false;
        this.puedeEditar = false;
        this.puedeEliminar = false;
    }

    // Getters y Setters

    public int getIdPrivilegio() {
        return idPrivilegio;
    }

    public void setIdPrivilegio(int idPrivilegio) {
        this.idPrivilegio = idPrivilegio;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public int getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(int idPermiso) {
        this.idPermiso = idPermiso;
    }

    public boolean isPuedeVer() {
        return puedeVer;
    }

    public void setPuedeVer(boolean puedeVer) {
        this.puedeVer = puedeVer;
    }

    public boolean isPuedeCrear() {
        return puedeCrear;
    }

    public void setPuedeCrear(boolean puedeCrear) {
        this.puedeCrear = puedeCrear;
    }

    public boolean isPuedeEditar() {
        return puedeEditar;
    }

    public void setPuedeEditar(boolean puedeEditar) {
        this.puedeEditar = puedeEditar;
    }

    public boolean isPuedeEliminar() {
        return puedeEliminar;
    }

    public void setPuedeEliminar(boolean puedeEliminar) {
        this.puedeEliminar = puedeEliminar;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getNombrePermiso() {
        return nombrePermiso;
    }

    public void setNombrePermiso(String nombrePermiso) {
        this.nombrePermiso = nombrePermiso;
    }

    public String getModuloPermiso() {
        return moduloPermiso;
    }

    public void setModuloPermiso(String moduloPermiso) {
        this.moduloPermiso = moduloPermiso;
    }

    /**
     * Verifica si tiene algún permiso activo.
     */
    public boolean tieneAlgunPermiso() {
        return puedeVer || puedeCrear || puedeEditar || puedeEliminar;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (puedeVer)
            sb.append("Ver ");
        if (puedeCrear)
            sb.append("Crear ");
        if (puedeEditar)
            sb.append("Editar ");
        if (puedeEliminar)
            sb.append("Eliminar");
        return sb.toString().trim();
    }
}
