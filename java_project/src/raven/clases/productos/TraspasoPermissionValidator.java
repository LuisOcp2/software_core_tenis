package raven.clases.productos;

import raven.clases.admin.UserSession;

/**
 * Validador centralizado de permisos para operaciones de traspasos.
 * Verifica tanto los permisos del sistema como las reglas de negocio (Bodega
 * Origen/Destino).
 */
public class TraspasoPermissionValidator {

    // Nombres de los módulos de permiso (deben coincidir con la BD)
    public static final String PERM_CREAR = "crear_traspaso";
    public static final String PERM_AUTORIZAR = "autorizar_traspaso";
    public static final String PERM_ENVIAR = "enviar_traspaso";
    public static final String PERM_RECIBIR = "recibir_traspaso";
    public static final String PERM_CANCELAR = "cancelar_traspaso";
    public static final String PERM_EDITAR = "editar_traspaso";
    public static final String PERM_ADMIN = "admin_traspasos";

    // Estados de traspaso
    private static final String ESTADO_PENDIENTE = "pendiente";
    private static final String ESTADO_AUTORIZADO = "autorizado";
    private static final String ESTADO_ENVIADO = "enviado";
    private static final String ESTADO_RECIBIDO = "recibido";
    private static final String ESTADO_EN_TRANSITO = "en_transito";
    private static final String ESTADO_CANCELADO = "cancelado";

    private UserSession session;

    public TraspasoPermissionValidator() {
        this.session = UserSession.getInstance();
    }

    /**
     * Verifica si el usuario puede CREAR un traspaso.
     * Regla: Tener permiso 'crear_traspaso'.
     */
    public boolean canCreate() {
        return hasSystemPermission(PERM_CREAR);
    }

    /**
     * Verifica si el usuario puede AUTORIZAR un traspaso.
     * Reglas:
     * 1. Tener permiso 'autorizar_traspaso'
     * 2. Estado debe ser 'pendiente'
     * 3. Usuario debe pertenecer a la Bodega de DESTINO (o ser Admin de Traspasos)
     */
    public boolean canAuthorize(String estado, int idBodegaDestino) {
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(estado)) {
            return false;
        }

        if (!hasSystemPermission(PERM_AUTORIZAR)) {
            return false;
        }

        // Si es admin global de traspasos, puede autorizar de cualquier bodega
        if (isAdminTraspasos()) {
            return true;
        }

        // Validar bodega destino (según regla: Bodega Destino: Autorizador)
        return isUserInBodega(idBodegaDestino);
    }

    /**
     * Verifica si el usuario puede ENVIAR un traspaso.
     * Reglas:
     * 1. Tener permiso 'enviar_traspaso'
     * 2. Estado debe ser 'autorizado'
     * 3. Usuario debe pertenecer a la Bodega de ORIGEN
     */
    public boolean canSend(String estado, int idBodegaOrigen) {
        if (!ESTADO_AUTORIZADO.equalsIgnoreCase(estado)) {
            return false;
        }

        if (!hasSystemPermission(PERM_ENVIAR)) {
            return false;
        }

        if (isAdminTraspasos()) {
            return true;
        }

        return isUserInBodega(idBodegaOrigen);
    }

    /**
     * Verifica si el usuario puede RECIBIR un traspaso.
     * Reglas:
     * 1. Tener permiso 'recibir_traspaso'
     * 2. Estado debe ser 'enviado' o 'en_transito'
     * 3. Usuario debe pertenecer a la Bodega de DESTINO
     */
    public boolean canReceive(String estado, int idBodegaDestino) {
        boolean estadoValido = ESTADO_ENVIADO.equalsIgnoreCase(estado) ||
                ESTADO_EN_TRANSITO.equalsIgnoreCase(estado);

        if (!estadoValido) {
            return false;
        }

        if (!hasSystemPermission(PERM_RECIBIR)) {
            return false;
        }

        if (isAdminTraspasos()) {
            return true;
        }

        return isUserInBodega(idBodegaDestino);
    }

    /**
     * Verifica si el usuario puede EDITAR un traspaso.
     * Reglas:
     * 1. Tener permiso 'editar_traspaso'
     * 2. Estado debe ser 'pendiente'
     * 3. Usuario debe pertenecer a la Bodega de ORIGEN
     */
    public boolean canEdit(String estado, int idBodegaOrigen) {
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(estado)) {
            return false;
        }

        if (!hasSystemPermission(PERM_EDITAR)) {
            return false;
        }

        if (isAdminTraspasos()) {
            return true;
        }

        return isUserInBodega(idBodegaOrigen);
    }

    /**
     * Verifica si el usuario puede CANCELAR un traspaso.
     * Reglas:
     * 1. Tener permiso 'cancelar_traspaso'
     * 2. Estado NO debe ser 'recibido' ni 'cancelado' (generalmente 'pendiente' o
     * 'autorizado')
     * 3. Usuario debe pertenecer a Bodega ORIGEN o DESTINO (cualquiera de los dos
     * involucrados)
     */
    public boolean canCancel(String estado, int idBodegaOrigen, int idBodegaDestino) {
        if (ESTADO_RECIBIDO.equalsIgnoreCase(estado) || ESTADO_CANCELADO.equalsIgnoreCase(estado)) {
            return false;
        }

        if (!hasSystemPermission(PERM_CANCELAR)) {
            return false;
        }

        if (isAdminTraspasos()) {
            return true;
        }

        return isUserInBodega(idBodegaOrigen) || isUserInBodega(idBodegaDestino);
    }

    // =================================================================================
    // MÉTODOS AUXILIARES
    // =================================================================================

    /**
     * Verifica si el usuario actual tiene un permiso del sistema.
     * MODIFICADO: Ya no da bypass automático a admin/gerente.
     * Solo el permiso admin_traspasos tiene bypass completo.
     */
    private boolean hasSystemPermission(String permissionModule) {
        // Verificar SOLO el permiso específico solicitado
        // Ya no damos bypass automático a admin/gerente aquí
        // El bypass se maneja en isAdminTraspasos() que se verifica en cada método
        return session.hasPermission(permissionModule);
    }

    private boolean isAdminTraspasos() {
        return session.hasPermission(PERM_ADMIN);
    }

    /**
     * Verifica si el usuario actual pertenece a una bodega específica.
     */
    private boolean isUserInBodega(int idBodega) {
        Integer userBodega = session.getIdBodegaUsuario();
        return userBodega != null && userBodega == idBodega;
    }

    // =================================================================================
    // MÉTODOS ESTÁTICOS DE CONVENIENCIA
    // =================================================================================

    public static boolean puedeCrearTraspaso() {
        return new TraspasoPermissionValidator().canCreate();
    }

    public static boolean puedeAutorizarTraspaso(String estado, int idBodegaDestino) {
        return new TraspasoPermissionValidator().canAuthorize(estado, idBodegaDestino);
    }

    public static boolean puedeEnviarTraspaso(String estado, int idBodegaOrigen) {
        return new TraspasoPermissionValidator().canSend(estado, idBodegaOrigen);
    }

    public static boolean puedeRecibirTraspaso(String estado, int idBodegaDestino) {
        return new TraspasoPermissionValidator().canReceive(estado, idBodegaDestino);
    }

    public static boolean puedeCancelarTraspaso(String estado, int idBodegaOrigen, int idBodegaDestino) {
        return new TraspasoPermissionValidator().canCancel(estado, idBodegaOrigen, idBodegaDestino);
    }

    public static boolean puedeEditarTraspaso(String estado, int idBodegaOrigen) {
        return new TraspasoPermissionValidator().canEdit(estado, idBodegaOrigen);
    }

    /**
     * Versión estática que consulta la base de datos para verificar si se puede editar.
     */
    public static boolean puedeEditarTraspaso(String numeroTraspaso, java.sql.Connection conn) throws java.sql.SQLException {
        String sql = "SELECT estado, id_bodega_origen FROM traspasos WHERE numero_traspaso = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return puedeEditarTraspaso(rs.getString("estado"), rs.getInt("id_bodega_origen"));
                }
            }
        }
        return false;
    }

    /**
     * Versión estática que consulta la base de datos para verificar si se puede anular/cancelar.
     */
    public static boolean puedeAnularTraspaso(String numeroTraspaso, java.sql.Connection conn) throws java.sql.SQLException {
        String sql = "SELECT estado, id_bodega_origen, id_bodega_destino FROM traspasos WHERE numero_traspaso = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return puedeCancelarTraspaso(rs.getString("estado"), 
                            rs.getInt("id_bodega_origen"), rs.getInt("id_bodega_destino"));
                }
            }
        }
        return false;
    }
}
