package raven.modelos;

/**
 * Clase singleton para mantener la información del usuario actualmente
 * logueado.
 */
public class SesionUsuario {

    private static SesionUsuario instance;
    private Usuario usuarioActual;

    private SesionUsuario() {
        usuarioActual = new Usuario(2, "vendedor1", "Carlos vendedor", "carlos@bodega.com", "vendedor", true);

    }

    public static SesionUsuario getInstance() {
        if (instance == null) {
            instance = new SesionUsuario();
        }
        return instance;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void setUsuarioActual(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    /**
     * Verifica si el usuario actual tiene el rol especificado.
     *
     * @param rol Rol a verificar
     * @return true si el usuario tiene el rol, false en caso contrario
     */
    public boolean tieneRol(String rol) {
        if (usuarioActual == null) {
            return false;
        }
        return usuarioActual.getRol().equals(rol);
    }

    /**
     * Verifica si hay un usuario logueado.
     *
     * @return true si hay un usuario logueado, false en caso contrario
     */
    public boolean hayUsuarioLogueado() {
        return usuarioActual != null;
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public void cerrarSesion() {
        usuarioActual = null;
    }
}
