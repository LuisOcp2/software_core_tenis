package raven.utils;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import raven.clases.admin.UserSession;
import raven.componentes.notificacion.Notification;
import raven.controlador.principal.conexion;
import raven.controlador.admin.ModelNotificacion;
import raven.controlador.admin.SessionManager;
import raven.utils.tono.CorporateTone;
import raven.utils.notificaciones.core.NotificationCache;
import raven.utils.notificaciones.core.NotificationQueue;
import raven.utils.notificaciones.enums.NotificationPriority;
import raven.utils.notificaciones.manager.TraspasoNotificationManager;
import raven.utils.notificaciones.ws.NotificationWebSocketClient;

/**
 * Servicio para gestionar notificaciones del sistema
 * Aplica principio SRP (Single Responsibility Principle) al enfocarse solo en gestionar notificaciones
 */
public class NotificacionesService {

    private static final Logger LOGGER = Logger.getLogger(NotificacionesService.class.getName());
    private static final int DIAS_ANTIGUEDAD_DEFAULT = 10;
    private static final int DEDUPE_WINDOW_MINUTES = 5;
    private static final String SP_NOTIF_TRASPASO = "CALL sp_notificar_traspaso_evento(?, ?, ?, ?)";
    private static final String SP_SYNC_TRASPASOS = "CALL sp_sync_notif_traspasos()";

    // Cache modularizado
    private final NotificationCache notificationCache = new NotificationCache();

    // Cola de notificaciones para priorización
    private final NotificationQueue notificationQueue = new NotificationQueue();

    // Implementación del patrón Strategy para almacenamiento de notificaciones
    private NotificacionBDPersistencia persistencia;
    private Frame parentFrame;
    private NotificationWebSocketClient wsClient;
    private Consumer<ModelNotificacion> realtimeConsumer;

    // Instancia Singleton
    private static NotificacionesService instance;

    /**
     * Obtiene la instancia única del servicio de notificaciones
     * @return Instancia de NotificacionesService
     */
    public static synchronized NotificacionesService getInstance() {
        if (instance == null) {
            instance = new NotificacionesService();
        }
        return instance;
    }

    public void setParentFrame(Frame parentFrame) {
        this.parentFrame = parentFrame;
    }

    public void mostrarAlertasInicioSesion(int idUsuario, int idBodega) {
        try {
            sincronizarNotificacionesTraspasos();
        } catch (Exception ignore) {
        }

        try {
            int count = 0;
            if (idUsuario > 0 && idBodega > 0) {
                count = contarNotificacionesActivasEventosPorBodegaYUsuario(idBodega, idUsuario);
            } else {
                count = contarNotificacionesActivasEventos();
            }

            if (count > 0) {
                Frame f = parentFrame;
                if (f == null) {
                    Frame[] frames = Frame.getFrames();
                    f = frames != null && frames.length > 0 ? frames[0] : null;
                }
                if (f != null) {
                    // Solo mostrar una notificación general en lugar de cada notificación individual
                    new Notification(
                            f,
                            Notification.Type.WARNING,
                            Notification.Location.TOP_RIGHT,
                            "Notificaciones pendientes",
                            "Tienes " + count + " notificación(es) pendiente(s)",
                            null,
                            6000
                    ).showNotification();
                    CorporateTone.playAlert();
                }
            }
        } catch (Exception ignore) {
        }
    }

    public NotificacionesService() {
        // Por defecto usamos la implementación para base de datos
        this.persistencia = new NotificacionBDPersistencia();
    }

    public NotificacionesService(Frame parentFrame) {
        this();
        this.parentFrame = parentFrame;
    }

    public NotificacionesService(NotificacionBDPersistencia persistencia) {
        this.persistencia = persistencia;
    }

    static List<ModelNotificacion> organizarNotificaciones(List<ModelNotificacion> lista) {
        if (lista == null || lista.isEmpty()) {
            return lista != null ? new ArrayList<>(lista) : new ArrayList<>();
        }

        List<ModelNotificacion> porFecha = new ArrayList<>(lista);
        porFecha.sort((a, b) -> {
            LocalDateTime fa = a != null ? a.getFechaCreacion() : null;
            LocalDateTime fb = b != null ? b.getFechaCreacion() : null;
            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fb.compareTo(fa);
        });

        Map<String, LocalDateTime> ultimaPorClave = new HashMap<>();
        List<ModelNotificacion> dedup = new ArrayList<>();
        for (ModelNotificacion n : porFecha) {
            if (n == null) continue;
            String clave = construirClaveAgrupacion(n);
            LocalDateTime fc = n.getFechaCreacion();
            LocalDateTime ultima = ultimaPorClave.get(clave);
            if (ultima != null && fc != null) {
                long min = Duration.between(fc, ultima).toMinutes();
                if (min >= 0 && min <= DEDUPE_WINDOW_MINUTES) {
                    continue;
                }
            }
            dedup.add(n);
            if (fc != null) {
                ultimaPorClave.put(clave, fc);
            }
        }

        dedup.sort((a, b) -> {
            int pa = prioridadTipo(a != null ? a.getTipo() : null);
            int pb = prioridadTipo(b != null ? b.getTipo() : null);
            if (pa != pb) return Integer.compare(pa, pb);
            LocalDateTime fa = a != null ? a.getFechaCreacion() : null;
            LocalDateTime fb = b != null ? b.getFechaCreacion() : null;
            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fb.compareTo(fa);
        });

        return dedup;
    }

    private static String construirClaveAgrupacion(ModelNotificacion n) {
        String usuario = n.getIdUsuarioDestinatario() != null ? String.valueOf(n.getIdUsuarioDestinatario()) : "ALL";
        String tipoRef = normalizarTexto(n.getTipoReferencia());
        String idRef = n.getIdReferencia() != null ? String.valueOf(n.getIdReferencia()) : "0";
        String evento = normalizarTexto(n.getEvento());
        String titulo = normalizarTexto(n.getTitulo());
        String mensaje = normalizarTexto(n.getMensaje());
        return usuario + "|" + tipoRef + "|" + idRef + "|" + evento + "|" + titulo + "|" + mensaje;
    }

    private static String normalizarTexto(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static int prioridadTipo(String tipo) {
        return NotificationPriority.fromString(tipo).getValue();
    }

    public List<ModelNotificacion> listarNotificacionesParaUsuarioYBodega(int idUsuario, Integer idBodega, String categoriaFiltro, String periodoFiltro, String textoBusqueda) {
        if (idBodega != null && idBodega > 0) {
            return obtenerNotificaciones(idUsuario, idBodega, categoriaFiltro, periodoFiltro, textoBusqueda);
        }
        return listarNotificacionesParaUsuario(idUsuario, categoriaFiltro, periodoFiltro, textoBusqueda);
    }

    public List<ModelNotificacion> listarNotificacionesParaUsuario(int idUsuario, String categoriaFiltro, String periodoFiltro, String textoBusqueda) {
        String key = idUsuario + "_NO_BODEGA_" + categoriaFiltro + "_" + periodoFiltro + "_" + textoBusqueda;
        
        List<ModelNotificacion> cached = notificationCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<ModelNotificacion> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id_notificacion, titulo, mensaje, tipo, categoria, id_usuario_destinatario, ")
           .append("para_todos, id_referencia, tipo_referencia, leida, activa, fecha_creacion, fecha_lectura, evento ")
           .append("FROM notificaciones WHERE activa = 1 AND (")
           .append(" id_usuario_destinatario IS NOT NULL AND id_usuario_destinatario = ? )")
           .append(" AND (leida = 0 OR leida IS NULL)");

        if (categoriaFiltro != null && !categoriaFiltro.isEmpty() &&
            !"Todas las categorias".equalsIgnoreCase(categoriaFiltro)) {
            String cf = categoriaFiltro.toLowerCase();
            if ("ordenes_reserva".equals(cf) || "traspasos".equals(cf)) {
                sql.append(" AND tipo_referencia = ?");
            } else {
                sql.append(" AND categoria = ?");
            }
        }
        if (periodoFiltro != null && !periodoFiltro.isEmpty()) {
            if ("Hoy".equalsIgnoreCase(periodoFiltro)) {
                sql.append(" AND DATE(fecha_creacion) = CURDATE()");
            } else if ("Semana".equalsIgnoreCase(periodoFiltro)) {
                sql.append(" AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
            } else if ("Mes".equalsIgnoreCase(periodoFiltro)) {
                sql.append(" AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            }
        }
        if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
            sql.append(" AND (titulo LIKE ? OR mensaje LIKE ?)");
        }
        sql.append(" ORDER BY fecha_creacion DESC");

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement st = con.prepareStatement(sql.toString())) {
            int idx = 1;
            st.setInt(idx++, idUsuario);
            if (categoriaFiltro != null && !categoriaFiltro.isEmpty() &&
                !"Todas las categorias".equalsIgnoreCase(categoriaFiltro)) {
                String cf = categoriaFiltro.toLowerCase();
                st.setString(idx++, cf);
            }
            if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
                String like = "%" + textoBusqueda + "%";
                st.setString(idx++, like);
                st.setString(idx++, like);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Timestamp fc = rs.getTimestamp("fecha_creacion");
                    Timestamp fl = rs.getTimestamp("fecha_lectura");
                    ModelNotificacion n = new ModelNotificacion(
                            rs.getInt("id_notificacion"),
                            rs.getString("titulo"),
                            rs.getString("mensaje"),
                            rs.getString("tipo"),
                            rs.getString("categoria"),
                            (Integer) rs.getObject("id_usuario_destinatario"),
                            rs.getBoolean("para_todos"),
                            (Integer) rs.getObject("id_referencia"),
                            rs.getString("tipo_referencia"),
                            rs.getBoolean("leida"),
                            rs.getBoolean("activa"),
                            fc != null ? fc.toLocalDateTime() : null,
                            fl != null ? fl.toLocalDateTime() : null
                    );
                    // Establecer el evento para que pueda ser usado en el filtrado
                    n.setEvento(rs.getString("evento"));
                    lista.add(n);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error listando notificaciones por usuario", ex);
        }

        List<ModelNotificacion> organizada = organizarNotificaciones(lista);
        notificationCache.put(key, organizada);
        return organizada;
    }

    // Métodos para obtener notificaciones
    public List<ModelNotificacion> obtenerNotificaciones(int idUsuario, int idBodega) {
        return obtenerNotificaciones(idUsuario, idBodega, null, null, null);
    }

    public List<ModelNotificacion> obtenerNotificaciones(int idUsuario, int idBodega, String categoriaFiltro) {
        return obtenerNotificaciones(idUsuario, idBodega, categoriaFiltro, null, null);
    }

    public List<ModelNotificacion> obtenerNotificaciones(int idUsuario, int idBodega, String categoriaFiltro, String periodoFiltro) {
        return obtenerNotificaciones(idUsuario, idBodega, categoriaFiltro, periodoFiltro, null);
    }

    public List<ModelNotificacion> obtenerNotificaciones(int idUsuario, int idBodega, String categoriaFiltro, String periodoFiltro, String textoBusqueda) {
        // Crear clave de cache basada en parámetros
        String key = idUsuario + "_" + idBodega + "_" + categoriaFiltro + "_" + periodoFiltro + "_" + textoBusqueda;

        // Verificar si hay datos en cache válidos
        List<ModelNotificacion> cached = notificationCache.get(key);
        if (cached != null) {
            System.out.println("INFO Notificaciones obtenidas de cache: " + cached.size());
            return cached;
        }

        sincronizarNotificacionesTraspasos();
        List<ModelNotificacion> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id_notificacion, titulo, mensaje, tipo, categoria, id_usuario_destinatario, ")
           .append("para_todos, id_referencia, tipo_referencia, leida, activa, fecha_creacion, fecha_lectura, evento ")
           .append("FROM notificaciones WHERE activa = 1 AND (")
           .append(" (id_usuario_destinatario IS NOT NULL AND id_usuario_destinatario = ?) ")
           .append(" OR (para_todos = 1 AND (id_bodega_origen = ? OR id_bodega_destino = ?)) )")
           .append(" AND (leida = 0 OR leida IS NULL)");

        if (categoriaFiltro != null && !categoriaFiltro.isEmpty() &&
            !"Todas las categorias".equalsIgnoreCase(categoriaFiltro)) {
            String cf = categoriaFiltro.toLowerCase();
            if ("ordenes_reserva".equals(cf) || "traspasos".equals(cf)) {
                sql.append(" AND tipo_referencia = ?");
            } else {
                sql.append(" AND categoria = ?");
            }
        }
        if (periodoFiltro != null && !periodoFiltro.isEmpty()) {
            if ("Hoy".equalsIgnoreCase(periodoFiltro)) {
                sql.append(" AND DATE(fecha_creacion) = CURDATE()");
            } else if ("Semana".equalsIgnoreCase(periodoFiltro)) {
                sql.append(" AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
            } else if ("Mes".equalsIgnoreCase(periodoFiltro)) {
                sql.append(" AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            }
        }
        if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
            sql.append(" AND (titulo LIKE ? OR mensaje LIKE ?)");
        }
        sql.append(" ORDER BY fecha_creacion DESC");

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement st = con.prepareStatement(sql.toString())) {
            int idx = 1;
            st.setInt(idx++, idUsuario);
            st.setInt(idx++, idBodega);
            st.setInt(idx++, idBodega);
            if (categoriaFiltro != null && !categoriaFiltro.isEmpty() &&
                !"Todas las categorias".equalsIgnoreCase(categoriaFiltro)) {
                String cf = categoriaFiltro.toLowerCase();
                st.setString(idx++, cf);
            }
            if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
                String like = "%" + textoBusqueda + "%";
                st.setString(idx++, like);
                st.setString(idx++, like);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Timestamp fc = rs.getTimestamp("fecha_creacion");
                    Timestamp fl = rs.getTimestamp("fecha_lectura");
                    ModelNotificacion n = new ModelNotificacion(
                            rs.getInt("id_notificacion"),
                            rs.getString("titulo"),
                            rs.getString("mensaje"),
                            rs.getString("tipo"),
                            rs.getString("categoria"),
                            (Integer) rs.getObject("id_usuario_destinatario"),
                            rs.getBoolean("para_todos"),
                            (Integer) rs.getObject("id_referencia"),
                            rs.getString("tipo_referencia"),
                            rs.getBoolean("leida"),
                            rs.getBoolean("activa"),
                            fc != null ? fc.toLocalDateTime() : null,
                            fl != null ? fl.toLocalDateTime() : null
                    );
                    // Establecer el evento para que pueda ser usado en el filtrado
                    n.setEvento(rs.getString("evento"));
                    lista.add(n);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error listando notificaciones por bodega", ex);
        }

        List<ModelNotificacion> organizada = organizarNotificaciones(lista);

        return organizada;
    }

    public boolean marcarNotificacionLeida(int idNotificacion) {
        boolean resultado = persistencia.marcarComoLeida(idNotificacion);
        if (resultado) {
            // Invalidar caché cuando se marca como leída
            notificationCache.invalidateAll();
        }
        return resultado;
    }

    public void notificarTraspasoEvento(int idTraspaso, String evento, String titulo, String mensaje) throws SQLException {
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(SP_NOTIF_TRASPASO)) {
            ps.setInt(1, idTraspaso);
            ps.setString(2, evento);
            ps.setString(3, titulo);
            ps.setString(4, mensaje);
            ps.execute();
        } finally {
            notificationCache.invalidateAll();
        }
    }

    public int obtenerMaxIdNotificacionTraspasos(int idUsuario, Integer idBodega) {
        int maxId = 0;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(MAX(id_notificacion),0) ")
           .append("FROM notificaciones ")
           .append("WHERE activa = 1 AND (leida = 0 OR leida IS NULL) ")
           .append("AND tipo_referencia = 'traspasos' ")
           .append("AND ( (id_usuario_destinatario IS NOT NULL AND id_usuario_destinatario = ?) ");
        if (idBodega != null && idBodega > 0) {
            sql.append(" OR (para_todos = 1 AND (id_bodega_origen = ? OR id_bodega_destino = ?)) ");
        } else {
            sql.append(" OR (para_todos = 1) ");
        }
        sql.append(")");

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setInt(1, idUsuario);
            if (idBodega != null && idBodega > 0) {
                ps.setInt(2, idBodega);
                ps.setInt(3, idBodega);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    maxId = rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "Error obteniendo max id de notificaciones de traspasos", ex);
        }
        return maxId;
    }

    public List<ModelNotificacion> listarNuevasNotificacionesTraspasos(int idUsuario, Integer idBodega, int lastId) {
        List<ModelNotificacion> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id_notificacion, titulo, mensaje, tipo, categoria, id_usuario_destinatario, ")
           .append("para_todos, id_referencia, tipo_referencia, leida, activa, fecha_creacion, fecha_lectura, evento ")
           .append("FROM notificaciones ")
           .append("WHERE activa = 1 AND (leida = 0 OR leida IS NULL) ")
           .append("AND tipo_referencia = 'traspasos' ")
           .append("AND id_notificacion > ? ")
           .append("AND ( (id_usuario_destinatario IS NOT NULL AND id_usuario_destinatario = ?) ");
        if (idBodega != null && idBodega > 0) {
            // Filtrar por bodega específica según el estado del traspaso
            sql.append(" OR (para_todos = 1 AND (id_bodega_origen = ? OR id_bodega_destino = ?)) ");
        } else {
            sql.append(" OR (para_todos = 1) ");
        }
        sql.append(") ORDER BY id_notificacion ASC");

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement st = con.prepareStatement(sql.toString())) {
            int idx = 1;
            st.setInt(idx++, lastId);
            st.setInt(idx++, idUsuario);
            if (idBodega != null && idBodega > 0) {
                st.setInt(idx++, idBodega);
                st.setInt(idx++, idBodega);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Timestamp fc = rs.getTimestamp("fecha_creacion");
                    Timestamp fl = rs.getTimestamp("fecha_lectura");
                    ModelNotificacion notif = new ModelNotificacion(
                            rs.getInt("id_notificacion"),
                            rs.getString("titulo"),
                            rs.getString("mensaje"),
                            rs.getString("tipo"),
                            rs.getString("categoria"),
                            (Integer) rs.getObject("id_usuario_destinatario"),
                            rs.getBoolean("para_todos"),
                            (Integer) rs.getObject("id_referencia"),
                            rs.getString("tipo_referencia"),
                            rs.getBoolean("leida"),
                            rs.getBoolean("activa"),
                            fc != null ? fc.toLocalDateTime() : null,
                            fl != null ? fl.toLocalDateTime() : null
                    );
                    // Establecer el evento para que pueda ser usado en el filtrado
                    notif.setEvento(rs.getString("evento"));
                    lista.add(notif);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "Error listando nuevas notificaciones de traspasos", ex);
        }
        return organizarNotificaciones(lista);
    }

    public List<VentaAntigua> obtenerVentasAntiguas(int dias) throws SQLException {
        List<VentaAntigua> lista = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.fecha_venta " +
                     "FROM ventas v " +
                     "WHERE v.estado = 'pendiente' " +
                     "AND DATEDIFF(CURDATE(), v.fecha_venta) >= ? " +
                     "ORDER BY v.fecha_venta ASC";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, dias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_venta");
                    lista.add(new VentaAntigua(rs.getInt("id_venta"), ts != null ? ts.toLocalDateTime() : null));
                }
            }
        }
        return lista;
    }

    /**
     * Muestra una notificación con las ventas antiguas detectadas
     */
    private void verificarVentasAntiguas() {
        try {
            List<VentaAntigua> ventasAntiguas = obtenerVentasAntiguas(DIAS_ANTIGUEDAD_DEFAULT);
            if (!ventasAntiguas.isEmpty()) {
                mostrarNotificacionVentasAntiguas(null, ventasAntiguas);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando ventas antiguas", e);
        }
    }

    /**
     * Verifica conteos pendientes y muestra notificación si hay más de X días
     */
    private void verificarConteosPendientes() {
        try {
            List<ConteoPendiente> conteosPendientes = obtenerConteosPendientes();
            if (!conteosPendientes.isEmpty()) {
                mostrarNotificacionConteosPendientes(null, conteosPendientes);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando conteos pendientes", e);
        }
    }

    /**
     * Verifica si hay pedidos web pendientes
     */
    private void verificarPedidosWeb() {
        try {
            List<PedidoWebPendiente> pedidos = obtenerPedidosWebPendientes();
            if (!pedidos.isEmpty()) {
                mostrarNotificacionPedidosWeb(pedidos);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando pedidos web", e);
        }
    }

    /**
     * Verifica si hay traspasos pendientes
     */
    private void verificarTraspasos() {
        try {
            List<TraspasoPendiente> traspasos = obtenerTraspasosPendientes();
            if (!traspasos.isEmpty()) {
                mostrarNotificacionTraspasos(traspasos);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando traspasos", e);
        }
    }

    /**
     * Inicia verificación de sistemas pendientes
     */
    public void verificarSistema() {
        try {
            // Sincronizar notificaciones de traspasos con el estado real en BD
            sincronizarNotificacionesTraspasos();

            // Verificar ventas antiguas
            verificarVentasAntiguas();

            // Verificar conteos pendientes
            verificarConteosPendientes();

            // Verificar pedidos web
            verificarPedidosWeb();

            // Verificar traspasos
            verificarTraspasos();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error general en verificación de sistema", e);
        }
    }

    // ============================================================
    // WEBSOCKET REALTIME
    // ============================================================
    public void startRealtime(Consumer<ModelNotificacion> consumer) {
        this.realtimeConsumer = consumer;
        try {
            int idUsuario = 0;
            Integer idBodega = null;
            try {
                idUsuario = UserSession.getInstance().getCurrentUser().getIdUsuario();
                idBodega = UserSession.getInstance().getIdBodegaUsuario();
            } catch (Throwable ignore) {}
            if (idUsuario <= 0) {
                try { idUsuario = SessionManager.getInstance().getCurrentUser().getIdUsuario(); } catch (Throwable ignore) {}
            }
            if (idBodega == null || idBodega <= 0) {
                try { idBodega = SessionManager.getInstance().getCurrentUserBodegaId(); } catch (Throwable ignore) {}
            }
            String base = System.getProperty("xtreme.ws.url", System.getenv("XTREME_WS_URL"));
            if (base == null || base.isEmpty()) {
                base = "ws://localhost:8080/ws/notifications";
            }
            String url = base + "?userId=" + idUsuario + (idBodega != null ? "&bodegaId=" + idBodega : "");
            wsClient = new NotificationWebSocketClient(url, idUsuario, idBodega, n -> {
                try {
                    if (realtimeConsumer != null) {
                        realtimeConsumer.accept(n);
                    }
                    // Mostrar pop y sonido básico
                    Frame f = parentFrame;
                    if (f == null) {
                        Frame[] frames = Frame.getFrames();
                        f = frames != null && frames.length > 0 ? frames[0] : null;
                    }
                    if (f != null) {
                        Notification.Type type = Notification.Type.INFO;
                        String t = n.getTipo() != null ? n.getTipo().toLowerCase() : "";
                        if ("urgent".equals(t) || "error".equals(t)) type = Notification.Type.URGENT;
                        else if ("warning".equals(t)) type = Notification.Type.WARNING;
                        else if ("success".equals(t)) type = Notification.Type.SUCCESS;
                        new Notification(f, type, Notification.Location.TOP_RIGHT, n.getTitulo(), n.getMensaje(), null, 7000).showNotification();
                        if ("urgent".equals(t) || "error".equals(t)) { CorporateTone.playAlert(); } else { CorporateTone.playInfo(); }
                    }
                } catch (Exception ignore) {}
            });
            wsClient.connect();
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "No fue posible iniciar WebSocket realtime", ex);
        }
    }
    public void stopRealtime() {
        try {
            if (wsClient != null) wsClient.close();
        } catch (Exception ignore) {}
        wsClient = null;
        realtimeConsumer = null;
    }

    /**
     * Sincroniza las notificaciones de traspasos con el estado real en la base de datos
     */
    public void sincronizarNotificacionesTraspasos() {
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(SP_SYNC_TRASPASOS)) {
            ps.execute();
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "No se pudo sincronizar notificaciones de traspasos", ex);
        }
    }

    /**
     * Cuenta notificaciones activas vinculadas a pedidos web o traspasos.
     */
    public int contarNotificacionesActivasEventos() {
        int count = 0;
        try (Connection con = conexion.getInstance().createConnection()) {
            String sql = "SELECT COUNT(*) FROM notificaciones WHERE activa = 1 AND (leida = 0 OR leida IS NULL) "
                    + "AND tipo_referencia IN ('ordenes_reserva','traspasos')";
            try (PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) count = rs.getInt(1);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error contando notificaciones activas de eventos", ex);
        }
        return count;
    }

    public int contarNotificacionesActivasEventosPorBodegaYUsuario(int idBodega, int idUsuario) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM notificaciones WHERE activa = 1 AND (leida = 0 OR leida IS NULL) "
                + "AND tipo_referencia IN ('ordenes_reserva','traspasos') "
                + "AND ( (id_usuario_destinatario IS NOT NULL AND id_usuario_destinatario = ?) "
                + "OR (para_todos = 1 AND (id_bodega_origen = ? OR id_bodega_destino = ?)) )";

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idBodega);
            ps.setInt(3, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) count = rs.getInt(1);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error contando notificaciones activas de eventos por bodega/usuario", ex);
        }
        return count;
    }

    public boolean hayEventosPedidosWebOTRaspasosOptimizado() {
        return contarNotificacionesActivasEventos() > 0;
    }

    public boolean hayEventosPedidosWebOTRaspasosOptimizadoPorBodega(Integer idBodega, int idUsuario) {
        if (idBodega == null || idBodega <= 0) {
            return hayEventosPedidosWebOTRaspasosOptimizado();
        }
        return contarNotificacionesActivasEventosPorBodegaYUsuario(idBodega, idUsuario) > 0;
    }

    /**
     * Marca como leídas todas las notificaciones relacionadas con un traspaso específico
     * cuando cambia a estado 'recibido'
     */
    public void marcarNotificacionesTraspasoComoLeidas(int idTraspaso) {
        try {
            Integer idBodega = null;
            try {
                idBodega = UserSession.getInstance().getIdBodegaUsuario();
            } catch (Throwable ignore) {}
            if (idBodega == null || idBodega <= 0) {
                try {
                    idBodega = SessionManager.getInstance().getCurrentUserBodegaId();
                } catch (Throwable ignore) {}
            }

            if (idBodega != null && idBodega > 0) {
                marcarNotificacionesTraspasoComoLeidasParaBodega(idTraspaso, idBodega);
                return;
            }

            String sql = "UPDATE notificaciones SET leida = 1, fecha_lectura = NOW() " +
                        "WHERE tipo_referencia = 'traspasos' AND id_referencia = ? AND (leida = 0 OR leida IS NULL)";

            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idTraspaso);
            int filasActualizadas = stmt.executeUpdate();

            stmt.close();
            conn.close();

            System.out.println("INFO Marcadas " + filasActualizadas + " notificaciones como leídas para traspaso ID: " + idTraspaso);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marcando notificaciones de traspaso como leídas", e);
        } finally {
            notificationCache.invalidateAll();
        }
    }

    public void marcarNotificacionesTraspasoComoLeidasParaBodega(int idTraspaso, int idBodega) {
        String sql = "UPDATE notificaciones n " +
                     "JOIN usuarios u ON u.id_usuario = n.id_usuario_destinatario " +
                     "SET n.leida = 1, n.fecha_lectura = NOW() " +
                     "WHERE n.tipo_referencia = 'traspasos' AND n.id_referencia = ? " +
                     "AND u.id_bodega = ? AND (n.leida = 0 OR n.leida IS NULL)";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idTraspaso);
            stmt.setInt(2, idBodega);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marcando notificaciones de traspaso como leídas por bodega", e);
        } finally {
            notificationCache.invalidateAll();
        }
    }

    private List<ConteoPendiente> obtenerConteosPendientes() throws SQLException {
        return new ArrayList<>();
    }

    private List<PedidoWebPendiente> obtenerPedidosWebPendientes() throws SQLException {
        return new ArrayList<>();
    }

    private List<TraspasoPendiente> obtenerTraspasosPendientes() throws SQLException {
        return new ArrayList<>();
    }

    private void mostrarNotificacionVentasAntiguas(Frame frame, List<VentaAntigua> ventas) {
        Frame f = frame != null ? frame : parentFrame;
        if (f == null || ventas == null || ventas.isEmpty()) return;
        new Notification(f, Notification.Type.WARNING, Notification.Location.TOP_RIGHT,
                "Ventas pendientes", "Hay " + ventas.size() + " venta(s) pendiente(s) antiguas", null, 4000)
                .showNotification();
        CorporateTone.playAlert();
    }

    private void mostrarNotificacionConteosPendientes(Frame frame, List<ConteoPendiente> conteos) {
    }

    private void mostrarNotificacionPedidosWeb(List<PedidoWebPendiente> pedidos) {
    }

    private void mostrarNotificacionTraspasos(List<TraspasoPendiente> traspasos) {
    }

    public static class VentaAntigua {
        private final int idVenta;
        private final LocalDateTime fechaVenta;

        public VentaAntigua(int idVenta, LocalDateTime fechaVenta) {
            this.idVenta = idVenta;
            this.fechaVenta = fechaVenta;
        }

        public int getIdVenta() {
            return idVenta;
        }

        public LocalDateTime getFechaVenta() {
            return fechaVenta;
        }
    }

    private static class ConteoPendiente {
    }

    private static class PedidoWebPendiente {
    }

    private static class TraspasoPendiente {
    }
}
