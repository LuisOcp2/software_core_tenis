package raven.modelos;

/**
 * Singleton que mantiene los datos de la sesión activa del usuario.
 * Patrón: Singleton thread-safe.
 *
 * Uso:
 *   SesionUsuario.getInstance().setIdUsuario(id);
 *   int uid = SesionUsuario.getInstance().getIdUsuario();
 */
public class SesionUsuario {

    private static volatile SesionUsuario instance;

    private int idUsuario;
    private String nombre;
    private String rol;
    private int idBodega;
    private String nombreBodega;

    private SesionUsuario() {}

    public static SesionUsuario getInstance() {
        if (instance == null) {
            synchronized (SesionUsuario.class) {
                if (instance == null) {
                    instance = new SesionUsuario();
                }
            }
        }
        return instance;
    }

    /** Limpia todos los datos de sesión (logout) */
    public void cerrarSesion() {
        this.idUsuario = 0;
        this.nombre = null;
        this.rol = null;
        this.idBodega = 0;
        this.nombreBodega = null;
    }

    // --- Getters y Setters ---
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public int getIdBodega() { return idBodega; }
    public void setIdBodega(int idBodega) { this.idBodega = idBodega; }

    public String getNombreBodega() { return nombreBodega; }
    public void setNombreBodega(String nombreBodega) { this.nombreBodega = nombreBodega; }

    /** Verifica si hay sesión activa */
    public boolean estaLogueado() { return idUsuario > 0; }

    @Override
    public String toString() {
        return "SesionUsuario{id=" + idUsuario + ", nombre='" + nombre +
               "', rol='" + rol + "', bodega=" + idBodega + "}";
    }
}
