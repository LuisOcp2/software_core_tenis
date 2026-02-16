package raven.clases.admin;

import java.sql.SQLException;
import java.util.List;
import raven.dao.RolesDAO;
import raven.modelos.Permiso;
import raven.modelos.PrivilegioRol;
import raven.modelos.Rol;

/**
 * Servicio de lógica de negocio para gestión de roles.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ServiceRoles {

    private final RolesDAO rolesDAO;

    public ServiceRoles() {
        this.rolesDAO = new RolesDAO();
    }

    /**
     * Crea un nuevo rol personalizado.
     */
    public int crearRolPersonalizado(String nombre, String descripcion) throws SQLException {
        // Validar nombre
        ValidationResult validacion = validarNombreRol(nombre);
        if (!validacion.isValido()) {
            throw new SQLException(validacion.getMensaje());
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setActivo(true);
        rol.setEsSistema(false);

        return rolesDAO.crearRol(rol);
    }

    /**
     * Clona un rol existente con un nuevo nombre.
     */
    public int clonarRol(int idRolOrigen, String nuevoNombre) throws SQLException {
        // Validar nombre
        ValidationResult validacion = validarNombreRol(nuevoNombre);
        if (!validacion.isValido()) {
            throw new SQLException(validacion.getMensaje());
        }

        // Obtener rol origen
        Rol rolOrigen = rolesDAO.obtenerRolPorId(idRolOrigen);
        if (rolOrigen == null) {
            throw new SQLException("Rol origen no encontrado");
        }

        // Crear nuevo rol
        Rol nuevoRol = new Rol();
        nuevoRol.setNombre(nuevoNombre);
        nuevoRol.setDescripcion("Copia de " + rolOrigen.getNombre());
        nuevoRol.setActivo(true);
        nuevoRol.setEsSistema(false);

        int idNuevoRol = rolesDAO.crearRol(nuevoRol);

        // Copiar permisos
        if (idNuevoRol > 0) {
            var privilegiosOrigen = rolesDAO.obtenerPrivilegiosDeRol(idRolOrigen);
            for (PrivilegioRol priv : privilegiosOrigen.values()) {
                rolesDAO.asignarPermisoARol(idNuevoRol, priv.getIdPermiso(), priv);
            }
        }

        return idNuevoRol;
    }

    /**
     * Valida que un nombre de rol sea válido y único.
     */
    public ValidationResult validarNombreRol(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return new ValidationResult(false, "El nombre del rol no puede estar vacío");
        }

        if (nombre.length() < 3) {
            return new ValidationResult(false, "El nombre del rol debe tener al menos 3 caracteres");
        }

        if (nombre.length() > 50) {
            return new ValidationResult(false, "El nombre del rol no puede exceder 50 caracteres");
        }

        // Verificar que no exista
        try {
            Rol existente = rolesDAO.obtenerRolPorNombre(nombre);
            if (existente != null) {
                return new ValidationResult(false, "Ya existe un rol con ese nombre");
            }
        } catch (SQLException e) {
            return new ValidationResult(false, "Error validando nombre: " + e.getMessage());
        }

        return new ValidationResult(true, "Nombre válido");
    }

    /**
     * Obtiene la lista de usuarios que tienen un rol específico.
     */
    public List<String> obtenerUsuariosConRol(int idRol) throws SQLException {
        // TODO: Implementar cuando UsuarioDAO esté actualizado
        // Por ahora retorna lista vacía
        return List.of();
    }

    /**
     * Verifica si un rol puede ser eliminado.
     */
    public boolean puedeEliminarRol(int idRol) throws SQLException {
        Rol rol = rolesDAO.obtenerRolPorId(idRol);

        if (rol == null) {
            return false;
        }

        // No se pueden eliminar roles del sistema
        if (rol.isEsSistema()) {
            return false;
        }

        // No se puede eliminar si tiene usuarios asignados
        int cantidadUsuarios = rolesDAO.contarUsuariosConRol(idRol);
        return cantidadUsuarios == 0;
    }

    /**
     * Asigna múltiples permisos a un rol de una vez.
     */
    public void asignarPermisos(int idRol, List<PrivilegioRol> privilegios) throws SQLException {
        for (PrivilegioRol privilegio : privilegios) {
            rolesDAO.asignarPermisoARol(idRol, privilegio.getIdPermiso(), privilegio);
        }
    }

    /**
     * Remueve todos los permisos de un rol y asigna nuevos.
     */
    public void reemplazarPermisos(int idRol, List<PrivilegioRol> nuevosPrivilegios) throws SQLException {
        // Obtener permisos actuales
        var permisosActuales = rolesDAO.obtenerPermisosDeRol(idRol);

        // Remover todos los permisos actuales
        for (Permiso permiso : permisosActuales) {
            rolesDAO.removerPermisoDeRol(idRol, permiso.getIdPermiso());
        }

        // Asignar nuevos permisos
        asignarPermisos(idRol, nuevosPrivilegios);
    }

    /**
     * Clase interna para resultados de validación.
     */
    public static class ValidationResult {
        private final boolean valido;
        private final String mensaje;

        public ValidationResult(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

        public boolean isValido() {
            return valido;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
