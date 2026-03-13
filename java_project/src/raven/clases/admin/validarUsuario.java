package raven.clases.admin;

import java.sql.SQLException;
import javax.swing.JOptionPane;
import raven.controlador.admin.ModelUser;

/**
 * Clase que maneja la validación de usuarios y contraseñas
 * @author Tu_Nombre
 */
public class validarUsuario {
    private ServiceUser usuarioDAO;
    
    /**
     * Constructor que inicializa el servicio de usuarios
     */
    public validarUsuario() {
        this.usuarioDAO = new ServiceUser();
    }
    
    /**
     * Obtiene el DAO de usuarios
     * @return Instancia del DAO de usuarios
     */
    public ServiceUser getUsuarioDAO() {
        return usuarioDAO;
    }
    
    /**
     * Retorna la contraseña en texto plano (sin cifrado)
     * @param password Contraseña en texto plano
     * @return La misma contraseña sin modificar
     */
    public String hashPassword(String password) {
        return password; // Retornar contraseña sin cifrado
    }
    
    /**
     * Valida las credenciales de un usuario
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano
     * @return Rol del usuario si las credenciales son válidas, null en caso contrario
     */
    public String validarLogin(String username, String password) {
        try {
            // Intentamos obtener el usuario por su nombre de usuario
            ModelUser usuario = usuarioDAO.obtenerUsuarioPorUsername(username);
            
            // Verificamos que el usuario exista y esté activo
            if (usuario != null && usuario.isActivo()) {
                String passwordAlmacenado = usuario.getPassword();
                
                // Comparar contraseñas en texto plano
                if (password.equals(passwordAlmacenado)) {
                    // Si la autenticación es exitosa, devolvemos el rol
                    return usuario.getRol();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en la validación del login: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                    "Error al verificar las credenciales. Contacte al administrador.",
                    "Error de autenticación", 
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
    /**
     * Obtiene un usuario completo por su nombre de usuario (para guardar en la sesión)
     * @param username Nombre de usuario
     * @return Usuario completo o null si no existe
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public ModelUser obtenerUsuarioCompleto(String username) throws SQLException {
        return usuarioDAO.obtenerUsuarioPorUsername(username);
    }
}