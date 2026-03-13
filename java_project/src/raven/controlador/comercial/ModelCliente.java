package raven.controlador.comercial;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ModelCliente {

    private int idCliente;
    private String nombre;
    private String dni;
    private String direccion;
    private String telefono;
    private String email;
    private Timestamp fechaRegistro;
    private int puntosAcumulados;
    private boolean activo;
    private int idBodega;

    public ModelCliente(int idCliente, String nombre, String dni, String direccion, String telefono, String email, Timestamp fechaRegistro, int puntosAcumulados, boolean activo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.dni = dni;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.fechaRegistro = fechaRegistro;
        this.puntosAcumulados = puntosAcumulados;
        this.activo = activo;
    }

    public ModelCliente(int idCliente, String nombre, String dni, String direccion, String telefono, String email, Timestamp fechaRegistro, boolean activo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.dni = dni;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.fechaRegistro = fechaRegistro;
        this.activo = activo;
    }

    public ModelCliente() {
    }

    // Getters y Setters
    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public int getPuntosAcumulados() {
        return puntosAcumulados;
    }

    public void setPuntosAcumulados(int puntosAcumulados) {
        this.puntosAcumulados = puntosAcumulados;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(int idBodega) {
        this.idBodega = idBodega;
    }

    public Object[] toTableRow(int rowNum) {
        
        DateFormat df = new SimpleDateFormat("dd-MMMM-yyyy");
        return new Object[]{
            false, // Checkbox selección
            rowNum,
            this,
            dni, // DNI 
            nombre, // Nombre completo
            direccion,
            telefono, // Teléfono
            email, // Email
            df.format(fechaRegistro), // Fecha original (se formateará en el renderer)
            activo, // Estado (1/0)
        };
    }
        @Override
    public String toString() {
        String id=String.valueOf(idCliente);
        return id;
    }
}
