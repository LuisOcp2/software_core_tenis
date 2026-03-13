package raven.componentes.impresion;

import java.sql.Timestamp;

public class DataSearchClient {

    private int idCliente;  // Cambiado a int (coincide con la tabla)
    private String nombre;
    private String dni;
    private String direccion;
    private String telefono;
    private String email;
    private Timestamp fechaRegistro;
    private int puntosAcumulados;
    private boolean activo;
    private boolean startsWithSearch; // Para priorizar búsquedas

    // Constructor completo
    public DataSearchClient(
        int idCliente,
        String nombre,
        String dni,
        String direccion,
        String telefono,
        String email,
        Timestamp fechaRegistro,
        int puntosAcumulados,
        boolean activo,
        boolean startsWithSearch
    ) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.dni = dni;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.fechaRegistro = fechaRegistro;
        this.puntosAcumulados = puntosAcumulados;
        this.activo = activo;
        this.startsWithSearch = startsWithSearch;
    }

    // Constructor mínimo para resultados de búsqueda
    public DataSearchClient(int idCliente, String nombre, String dni, boolean startsWithSearch) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.dni = dni;
        this.startsWithSearch = startsWithSearch;
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

    public boolean isStartsWithSearch() {
        return startsWithSearch;
    }

    public void setStartsWithSearch(boolean startsWithSearch) {
        this.startsWithSearch = startsWithSearch;
    }

    @Override
    public String toString() {
        return "ID: " + idCliente + " - " + nombre + " - DNI: " + dni;
    }
}