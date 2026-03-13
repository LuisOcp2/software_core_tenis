package raven.clases.admin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import raven.dao.PermisosDAO;
import raven.dao.PrivilegioUsuarioDAO;
import raven.dao.RolesDAO;
import raven.modelos.Permiso;
import raven.modelos.PrivilegioRol;
import raven.modelos.PrivilegioUsuario;

/**
 * Servicio de lógica de negocio para gestión de permisos.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ServicePermisos {

    private final PermisosDAO permisosDAO;
    private final RolesDAO rolesDAO;
    private final PrivilegioUsuarioDAO privilegioUsuarioDAO;

    public ServicePermisos() {
        this.permisosDAO = new PermisosDAO();
        this.rolesDAO = new RolesDAO();
        this.privilegioUsuarioDAO = new PrivilegioUsuarioDAO();
    }

    /**
     * Verifica si un usuario tiene un permiso específico para realizar una acción.
     * 
     * @param idUsuario ID del usuario
     * @param modulo    Nombre del módulo/permiso
     * @param accion    Acción a verificar: "ver", "crear", "editar", "eliminar"
     * @return true si tiene el permiso, false en caso contrario
     */
    public boolean usuarioTienePermiso(int idUsuario, String modulo, String accion) {
        try {
            // 1. Verificar si el usuario tiene MODO PERSONALIZADO (Exclusive Mode)
            boolean tienePersonalizados = privilegioUsuarioDAO.tienePermisosAsignados(idUsuario);

            if (tienePersonalizados) {
                // MODO EXCLUSIVO: Solo valen los permisos explícitos en privilegio_usuario

                // Buscar el ID del permiso sin importar mayúsculas/minúsculas
                Permiso permiso = null;
                List<Permiso> todos = permisosDAO.obtenerTodosPermisos(); // Caché deseable aquí
                for (Permiso p : todos) {
                    if (p.getModulo().equalsIgnoreCase(modulo)) {
                        permiso = p;
                        break;
                    }
                }

                if (permiso == null)
                    return false; // El módulo no existe o no tiene permiso de nada

                PrivilegioUsuario privilegio = privilegioUsuarioDAO
                        .obtenerPrivilegioPorUsuarioYPermiso(idUsuario, permiso.getIdPermiso());

                if (privilegio == null) {
                    return false; // Tiene personalizados, pero NO para este módulo -> Acceso Denegado
                }

                switch (accion.toLowerCase()) {
                    case "ver":
                        return privilegio.isPuedeVer();
                    case "crear":
                        return privilegio.isPuedeCrear();
                    case "editar":
                        return privilegio.isPuedeEditar();
                    case "eliminar":
                        return privilegio.isPuedeEliminar();
                    default:
                        return false;
                }
            } else {
                // 2. MODO ROL (Legacy): Solo si NO tiene ningún permiso personalizado
                int idRol = obtenerIdRolDeUsuario(idUsuario);
                if (idRol <= 0) {
                    return false;
                }

                Map<String, PrivilegioRol> privilegios = rolesDAO.obtenerPrivilegiosDeRol(idRol);
                PrivilegioRol privilegio = privilegios.get(modulo.toLowerCase());

                if (privilegio == null) {
                    return false;
                }

                switch (accion.toLowerCase()) {
                    case "ver":
                        return privilegio.isPuedeVer();
                    case "crear":
                        return privilegio.isPuedeCrear();
                    case "editar":
                        return privilegio.isPuedeEditar();
                    case "eliminar":
                        return privilegio.isPuedeEliminar();
                    default:
                        return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verificando permiso: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la lista de módulos a los que un usuario tiene acceso (al menos
     * permiso de ver).
     */
    public List<String> obtenerModulosPermitidos(int idUsuario) {
        List<String> modulosPermitidos = new ArrayList<>();
        try {
            // 1. Verificar MODO EXCLUSIVO
            if (privilegioUsuarioDAO.tienePermisosAsignados(idUsuario)) {
                List<PrivilegioUsuario> personalizados = privilegioUsuarioDAO.obtenerPermisosPorUsuario(idUsuario);
                for (PrivilegioUsuario pu : personalizados) {
                    if (pu.isPuedeVer()) {
                        String modName = pu.getModulo();
                        if (modName == null && pu.getIdPermiso() > 0) {
                            Permiso p = permisosDAO.obtenerPermisoPorId(pu.getIdPermiso());
                            if (p != null)
                                modName = p.getModulo();
                        }
                        if (modName != null) {
                            modulosPermitidos.add(modName);
                        }
                    }
                }
            } else {
                // 2. MODO ROL
                int idRol = obtenerIdRolDeUsuario(idUsuario);
                if (idRol > 0) {
                    Map<String, PrivilegioRol> privilegiosRol = rolesDAO.obtenerPrivilegiosDeRol(idRol);
                    for (PrivilegioRol pr : privilegiosRol.values()) {
                        if (pr.isPuedeVer()) {
                            modulosPermitidos.add(pr.getModuloPermiso());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo módulos permitidos: " + e.getMessage());
        }
        return modulosPermitidos;
    }

    /**
     * Obtiene todos los privilegios detallados de un usuario.
     */
    /**
     * Obtiene todos los privilegios detallados de un usuario.
     * Utilizado por UserSession para cargar el caché.
     */
    public Map<String, PrivilegioRol> obtenerPermisosDetalladosUsuario(int idUsuario) {
        Map<String, PrivilegioRol> privilegios = new HashMap<>();
        try {
            // 1. Verificar MODO EXCLUSIVO
            if (privilegioUsuarioDAO.tienePermisosAsignados(idUsuario)) {
                List<PrivilegioUsuario> personalizados = privilegioUsuarioDAO.obtenerPermisosPorUsuario(idUsuario);

                for (PrivilegioUsuario pu : personalizados) {
                    PrivilegioRol pr = new PrivilegioRol();
                    // Mapear PrivilegioUsuario a PrivilegioRol para compatibilidad
                    pr.setIdRol(-1); // Indicar que no es de un rol
                    pr.setIdPermiso(pu.getIdPermiso());
                    pr.setPuedeVer(pu.isPuedeVer());
                    pr.setPuedeCrear(pu.isPuedeCrear());
                    pr.setPuedeEditar(pu.isPuedeEditar());
                    pr.setPuedeEliminar(pu.isPuedeEliminar());

                    String modName = pu.getModulo();
                    // Si el objeto no trajo el nombre (por el join), buscarlo
                    if (modName == null && pu.getIdPermiso() > 0) {
                        Permiso p = permisosDAO.obtenerPermisoPorId(pu.getIdPermiso());
                        if (p != null) {
                            modName = p.getModulo();
                            pr.setNombrePermiso(p.getNombreMostrar());
                        }
                    } else {
                        pr.setNombrePermiso(pu.getNombreMostrar());
                    }

                    if (modName != null) {
                        pr.setModuloPermiso(modName);
                        privilegios.put(modName.toLowerCase(), pr);
                    }
                }
            } else {
                // 2. MODO ROL (Legacy)
                int idRol = obtenerIdRolDeUsuario(idUsuario);
                if (idRol > 0) {
                    return rolesDAO.obtenerPrivilegiosDeRol(idRol);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo permisos detallados: " + e.getMessage());
        }
        return privilegios;
    }

    /**
     * Busca permisos por nombre o categoría.
     */
    public List<Permiso> buscarPermisos(String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            return permisosDAO.obtenerTodosPermisos();
        }

        List<Permiso> todos = permisosDAO.obtenerTodosPermisos();
        String filtroLower = filtro.toLowerCase();

        return todos.stream()
                .filter(p -> p.getModulo().toLowerCase().contains(filtroLower) ||
                        p.getNombreMostrar().toLowerCase().contains(filtroLower) ||
                        (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(filtroLower)))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los permisos agrupados por categoría.
     */
    public Map<String, List<Permiso>> obtenerPermisosAgrupadosPorCategoria() throws SQLException {
        List<Permiso> todos = permisosDAO.obtenerTodosPermisos();
        Map<String, List<Permiso>> agrupados = new HashMap<>();

        for (Permiso permiso : todos) {
            String categoria = permiso.getCategoria() != null ? permiso.getCategoria() : "Otros";
            agrupados.computeIfAbsent(categoria, k -> new ArrayList<>()).add(permiso);
        }

        return agrupados;
    }

    /**
     * Obtiene el ID del rol de un usuario desde la base de datos.
     */
    private int obtenerIdRolDeUsuario(int idUsuario) {
        try {
            raven.dao.UsuarioDAO usuarioDAO = new raven.dao.UsuarioDAO();
            return usuarioDAO.obtenerIdRolDeUsuario(idUsuario);
        } catch (Exception e) {
            System.err.println("Error obteniendo rol del usuario: " + e.getMessage());
            return -1;
        }
    }
}
