package raven.modelos;

/**
 * Modelo que representa a un usuario del sistema.
 */
public class Usuario {
    
    private int id;
    private String username;
    private String nombre;
    private String email;
    private String rol;
    private boolean activo;
    
    public Usuario() {
    }
    
    public Usuario(int id, String username, String nombre, String email, String rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}