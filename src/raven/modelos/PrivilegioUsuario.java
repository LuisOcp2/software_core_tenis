package raven.modelos;

import java.sql.Timestamp;

/**
 * Modelo para representar permisos personalizados asignados a usuarios
 * individuales.
 * Estos permisos sobrescriben los permisos del rol asignado al usuario.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class PrivilegioUsuario {
    private int idPrivilegioUsuario;
    private int idUsuario;
    private int idPermiso;
    private boolean puedeVer;
    private boolean puedeCrear;
    private boolean puedeEditar;
    private boolean puedeEliminar;
    private Timestamp fechaAsignacion;
    private Integer idUsuarioAsignador;

    // Campos adicionales para mostrar en UI (cargados mediante JOIN)
    private String modulo;
    private String nombreMostrar;

    public PrivilegioUsuario() {
    }

    public PrivilegioUsuario(int idUsuario, int idPermiso) {
        this.idUsuario = idUsuario;
        this.idPermiso = idPermiso;
    }

    // Getters y Setters
    public int getIdPrivilegioUsuario() {
        return idPrivilegioUsuario;
    }

    public void setIdPrivilegioUsuario(int idPrivilegioUsuario) {
        this.idPrivilegioUsuario = idPrivilegioUsuario;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
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

    public Timestamp getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(Timestamp fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public Integer getIdUsuarioAsignador() {
        return idUsuarioAsignador;
    }

    public void setIdUsuarioAsignador(Integer idUsuarioAsignador) {
        this.idUsuarioAsignador = idUsuarioAsignador;
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

    @Override
    public String toString() {
        return nombreMostrar != null ? nombreMostrar : "Permiso #" + idPermiso;
    }
}
