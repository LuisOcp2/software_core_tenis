package raven.application.form.other;

import raven.application.form.productos.*;
import raven.application.form.productos.traspasos.traspasos;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.conexion;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.BorderOption;
import raven.cell.TableActionCellRender;
import raven.cell.TableActionCellEditor;
import raven.cell.TableActionEvent;
import raven.controlador.admin.ModelNotificacion;
import raven.utils.NotificacionesService;
import raven.application.Application;
import raven.application.form.comercial.Carrito;
import raven.application.form.comercial.reporteVentas;
import raven.clases.admin.UserSession;
import raven.controlador.admin.SessionManager;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.Toolkit;
import javax.swing.Timer;
import java.util.HashSet;
import java.util.Set;
import raven.componentes.notificacion.Notification;
import raven.utils.tono.CorporateTone;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import com.formdev.flatlaf.FlatLaf;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 *
 * @author CrisDEV
 * se debe conectar con la tabla notificaciones
 */
public class Notificaciones extends javax.swing.JPanel {


    
// Constructor del formulario de gestión de productos

    private final FontIcon iconAjustar;
    private final FontIcon iconNuevo;
    private final FontIcon iconDesactivar;
    private NotificacionesService notificacionesService;
    private List<ModelNotificacion> notificacionesActuales = new ArrayList<>();
    private DefaultTableModel modeloTabla;
    private Set<Integer> idsVistos = new HashSet<>();
    private Timer timerAvisos;
    private Set<Integer> expandedIds = new HashSet<>();
    private int defaultRowHeight = 70;
    
    public Notificaciones() {
       initComponents();

        // Instala la fuente Roboto (extensión de FlatLaf)
        FlatRobotoFont.install();

        // Establece fuente predeterminada para todos los componentes
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // Aplica estilo personalizado a la etiqueta (usando propiedades FlatLaf)
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");  // Usa estilo de fuente h1

        // Inicializa configuraciones personalizadas
        init();
        //Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.PLUS_SQUARE, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.EDIT, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.TRASH_ALT, tabTextColor);

       

        //Fin de diseño de botones
        
        // Inicializar y configurar el controlador

        // Servicio de notificaciones
        notificacionesService = new NotificacionesService();

        // Configurar modelo de tabla
        modeloTabla = (DefaultTableModel) table.getModel();
        configurarColumnaAcciones();
        configurarRenderersNotificaciones();
        actualizarUsuarioUI();
        cargarNotificaciones();
        
        try {
            int idUsuario = obtenerIdUsuarioActual();
            Integer idBodega = obtenerIdBodegaActual();
            notificacionesService.setParentFrame((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this));
            notificacionesService.startRealtime(n -> {
                // Aplicar filtros actuales
                String categoriaSel = mapCategoriaFiltro((String) categoria.getSelectedItem());
                String catMsg = n.getCategoria() != null ? n.getCategoria().toLowerCase() : "";
                String tipoRef = n.getTipoReferencia() != null ? n.getTipoReferencia().toLowerCase() : "";
                boolean pasaFiltro = true;
                if (categoriaSel != null && !"Todas las categorias".equalsIgnoreCase(categoriaSel)) {
                    if ("ordenes_reserva".equalsIgnoreCase(categoriaSel) || "traspasos".equalsIgnoreCase(categoriaSel)) {
                        pasaFiltro = categoriaSel.equalsIgnoreCase(tipoRef);
                    } else {
                        pasaFiltro = categoriaSel.equalsIgnoreCase(catMsg);
                    }
                }
                if (!pasaFiltro) return;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    // Añadir a lista y tabla
                    notificacionesActuales.add(0, n);
                    modeloTabla.insertRow(0, new Object[]{
                        n.getIdNotificacion(),
                        n.getTitulo(),
                        n.getMensaje(),
                        n.getTipo(),
                        ""
                    });
                    aplicarAlturaFilas();
                    try {
                        String t = n.getTipo() != null ? n.getTipo().toLowerCase() : "";
                        if ("urgent".equals(t) || "error".equals(t)) {
                            CorporateTone.playAlert();
                        } else {
                            CorporateTone.playInfo();
                        }
                    } catch (Throwable ignore) {}
                });
            });
        } catch (Exception ignore) {}
    }
    
    private void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 20px
                + "background:@background;"  // Usa color de fondo estándar
                + "border:0,0,0,0;"); // Sin márgenes

        // Estiliza el encabezado de la tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;" // Altura del encabezado
                + "hoverBackground:null;" // Desactiva efecto hover
                + "pressedBackground:null;" // Desactiva efecto al presionar
                + "separatorColor:@background;" // Color del separador
                + "font:bold;");  // Texto en negrita

        // Estiliza la tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:70;" // Altura de filas
                + "showHorizontalLines:true;" // Muestra líneas horizontales
                + "intercellSpacing:0,1;" // Espaciado entre celdas
                + "cellFocusColor:$Component.focusColor;" // Color de enfoque
                + "selectionBackground:$Table.selectionBackground;" // Fondo de selección
                + "selectionForeground:$Table.selectionForeground;"
                + "background:@background;");  // Texto de selección

        // Estiliza la barra de desplazamiento
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;" // Barra completamente redondeada
                + "trackInsets:3,3,3,3;" // Relleno de la barra
                + "thumbInsets:3,3,3,3;" // Relleno del control deslizante
                + "background:@background;");  // Color de fondo

        // Estiliza el título
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;");  // Texto en negrita y más grande

        // Configura el campo de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));  // Ícono de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;" // Esquinas redondeadas
                + "borderWidth:0;" // Sin borde
                + "focusWidth:0;" // Sin borde de enfoque
                + "innerFocusWidth:0;" // Sin enfoque interno
                + "margin:5,20,5,20;" // Márgenes
                + "background:@background");  // Color de fondo

        // Configura renderizadores personalizados para columnas (sin checkbox "seleccionar todos")
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Configuración predeterminada para diálogos modales
        ModalDialog.getDefaultOption()
                .setOpacity(0.3f) // Opacidad del fondo
                .getLayoutOption().setAnimateScale(0.1f);  // Escala de animación
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM);  // Sombra

        // Conecta a la base de datos y carga datos iniciales
        try {
            conexion.getInstance().connectToDatabase();
            
            conexion.getInstance().close();

        } catch (SQLException e) {
            // Manejo silencioso de errores (debería mejorarse)
        }
    }

    private void configurarRenderersNotificaciones() {
        table.setDefaultRenderer(Object.class, new NotificacionRowRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new TipoChipRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new MensajeCellRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && e.getClickCount() == 2) {
                    toggleExpand(row);
                }
            }
        });
    }

    private void toggleExpand(int row) {
        Object idObj = table.getValueAt(row, 0);
        if (idObj == null) return;
        int id = Integer.parseInt(String.valueOf(idObj));
        if (expandedIds.contains(id)) {
            expandedIds.remove(id);
            table.setRowHeight(row, defaultRowHeight);
        } else {
            expandedIds.add(id);
            table.setRowHeight(row, defaultRowHeight * 2);
        }
        table.repaint();
    }

    /**
     *  COLORES PROFESIONALES POR TIPO Y ESTADO DE NOTIFICACIÓN
     * Retorna un color específico según el tipo y contexto de la notificación
     */
    private Color colorBasePorTipo(ModelNotificacion notif) {
        if (notif == null) return new Color(96, 125, 139); // Gris

        String tipo = notif.getTipo();
        String titulo = notif.getTitulo();

        if (tipo == null) return new Color(96, 125, 139); // Gris

        String t = tipo.trim().toLowerCase();
        String tit = titulo != null ? titulo.toLowerCase() : "";

        // WARNING  ALERTAS URGENTES (Rojo brillante)
        if (t.contains("urgent") || t.contains("alert")) {
            return new Color(255, 82, 82); // Rojo urgente
        }

        // ERROR  ERRORES (Rojo oscuro)
        if (t.contains("error")) {
            return new Color(255, 69, 58); // Rojo error
        }

        //  ADVERTENCIAS / PENDIENTES (Amarillo)
        if (t.contains("warning") || tit.contains("pendiente")) {
            return new Color(255, 193, 7); // Amarillo
        }

        // SUCCESS  ÉXITOS / AUTORIZADOS (Verde éxito)
        if (t.contains("success") && (tit.contains("autorizado") || tit.contains("aprobado"))) {
            return new Color(52, 199, 89); // Verde éxito
        }

        //  TRASPASOS COMPLETADOS / RECIBIDOS (Verde oscuro)
        if (t.contains("success") && (tit.contains("completado") || tit.contains("recibido"))) {
            return new Color(76, 175, 80); // Verde oscuro
        }

        // Caja TRASPASOS EN TRÁNSITO (Cyan/Azul claro)
        if (t.contains("info") && (tit.contains("tránsito") || tit.contains("enviado"))) {
            return new Color(0, 188, 212); // Cyan
        }

        // Carrito PEDIDOS WEB (Índigo)
        if (tit.contains("pedido") || tit.contains("orden")) {
            return new Color(63, 81, 181); // Índigo
        }

        // Efectivo VENTAS (Verde menta)
        if (tit.contains("venta") || tit.contains("sale")) {
            return new Color(0, 150, 136); // Verde menta
        }

        // Nota RECORDATORIOS (Púrpura)
        if (tit.contains("recordatorio") || tit.contains("reminder")) {
            return new Color(156, 39, 176); // Púrpura
        }

        // SUCCESS  SUCCESS genérico (Verde)
        if (t.contains("success")) {
            return new Color(52, 199, 89); // Verde
        }

        // ℹ INFO / DEFAULT (Azul)
        return new Color(0, 122, 255); // Azul info (iOS style)
    }

    private Color blend(Color c1, Color c2, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int rr = (int) (c1.getRed() * (1 - r) + c2.getRed() * r);
        int gg = (int) (c1.getGreen() * (1 - r) + c2.getGreen() * r);
        int bb = (int) (c1.getBlue() * (1 - r) + c2.getBlue() * r);
        return new Color(rr, gg, bb);
    }

    private String toHex(Color c) {
        String r = String.format("%02x", c.getRed());
        String g = String.format("%02x", c.getGreen());
        String b = String.format("%02x", c.getBlue());
        return "#" + r + g + b;
    }

    private class NotificacionRowRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            ModelNotificacion n = row >= 0 && row < notificacionesActuales.size() ? notificacionesActuales.get(row) : null;
            Color base = colorBasePorTipo(n);
            Color mixWith = UIManager.getColor("Table.background");
            if (mixWith == null) {
                mixWith = FlatLaf.isLafDark() ? new Color(49,62,74) : Color.WHITE;
            }
            Color bg = blend(base, mixWith, 0.85f);
            if (!isSelected) {
                Color alt = UIManager.getColor("Table.alternateRowColor");
                Color use = row % 2 == 0 ? bg : (alt != null ? blend(base, alt, 0.85f) : blend(bg, mixWith, 0.90f));
                c.setBackground(use);
            }
            if (n != null && !n.isLeida()) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                c.setBorder(new javax.swing.border.MatteBorder(0, 6, 0, 0, base));
            } else {
                c.setBorder(null);
            }
            return c;
        }
    }

    private class TipoChipRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String tipo = value != null ? String.valueOf(value) : "";
            ModelNotificacion n = row >= 0 && row < notificacionesActuales.size() ? notificacionesActuales.get(row) : null;
            Color base = colorBasePorTipo(n);
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            p.setOpaque(false);
            JLabel chip = new JLabel(tipo);
            chip.setHorizontalAlignment(SwingConstants.CENTER);
            chip.setForeground(Color.WHITE);
            chip.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            chip.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:" + toHex(base));
            p.add(chip);
            return p;
        }
    }

    private class MensajeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            c.setVerticalAlignment(SwingConstants.TOP);
            ModelNotificacion n = row >= 0 && row < notificacionesActuales.size() ? notificacionesActuales.get(row) : null;
            if (n != null) {
                int id = n.getIdNotificacion();
                boolean expanded = expandedIds.contains(id);
                String titulo = n.getTitulo() != null ? n.getTitulo() : "";
                String mensaje = n.getMensaje() != null ? n.getMensaje() : "";
                if (expanded) {
                    String html = "<html><div style='font-weight:bold;margin-bottom:4px;'>" + escapeHtml(titulo) + "</div>"
                            + "<div style='line-height:1.4;'>" + escapeHtml(mensaje) + "</div></html>";
                    c.setText(html);
                } else {
                    String shortMsg = mensaje.length() > 120 ? mensaje.substring(0, 120) + "…" : mensaje;
                    String html = "<html><div style='font-weight:bold;margin-bottom:2px;'>" + escapeHtml(titulo) + "</div>"
                            + "<div>" + escapeHtml(shortMsg) + "</div></html>";
                    c.setText(html);
                }
                Color base = colorBasePorTipo(n);
                Color mixWith = UIManager.getColor("Table.background");
                if (mixWith == null) {
                    mixWith = FlatLaf.isLafDark() ? new Color(49,62,74) : Color.WHITE;
                }
                Color bg = blend(base, mixWith, 0.85f);
                if (!isSelected) {
                    Color alt = UIManager.getColor("Table.alternateRowColor");
                    Color use = row % 2 == 0 ? bg : (alt != null ? blend(base, alt, 0.85f) : blend(bg, mixWith, 0.90f));
                    c.setBackground(use);
                }
            }
            return c;
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void actualizarUsuarioUI() {
        String nombreUsuario = null;
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                nombreUsuario = UserSession.getInstance().getCurrentUser().getNombre();
            }
        } catch (Throwable ignore) {}
        if (nombreUsuario == null) {
            try {
                if (SessionManager.getInstance().isSessionActive() && SessionManager.getInstance().getCurrentUser() != null) {
                    nombreUsuario = SessionManager.getInstance().getCurrentUser().getNombre();
                }
            } catch (Throwable ignore) {}
        }
        LbUser.setText(nombreUsuario != null ? nombreUsuario : "Usuario");
    }

    private int obtenerIdUsuarioActual() {
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                return UserSession.getInstance().getCurrentUser().getIdUsuario();
            }
        } catch (Throwable ignore) {}
        try {
            if (SessionManager.getInstance().isSessionActive() && SessionManager.getInstance().getCurrentUser() != null) {
                return SessionManager.getInstance().getCurrentUser().getIdUsuario();
            }
        } catch (Throwable ignore) {}
        return 1; // fallback para entorno de desarrollo
    }

    private void configurarColumnaAcciones() {
        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onEdit(int row) {
                ejecutarAccion(row, true);
            }

            @Override
            public void onDelete(int row) { /* no usado */ }

            @Override
            public void onView(int row) {
                ejecutarAccion(row, true);
            }

            @Override
            public void onCaja(int row) {
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(new TableActionCellRender());
        table.getColumnModel().getColumn(4).setCellEditor(new TableActionCellEditor(event));
    }

    private void cargarNotificaciones() {
        int idUsuario = obtenerIdUsuarioActual();
        Integer idBodega = obtenerIdBodegaActual();
        String categoriaSel = mapCategoriaFiltro((String) categoria.getSelectedItem());
        String periodoSel = (String) CbxFiltro.getSelectedItem();
        String texto = txtSearch.getText();

        if (idBodega != null && idBodega > 0) {
            notificacionesActuales = notificacionesService.listarNotificacionesParaUsuarioYBodega(
                    idUsuario,
                    idBodega,
                    categoriaSel,
                    periodoSel,
                    texto
            );
        } else {
            notificacionesActuales = notificacionesService.listarNotificacionesParaUsuario(
                    idUsuario,
                    categoriaSel,
                    periodoSel,
                    texto
            );
        }

        // Limpiar tabla
        modeloTabla.setRowCount(0);
        // Poblar filas
        for (ModelNotificacion n : notificacionesActuales) {
            modeloTabla.addRow(new Object[]{
                n.getIdNotificacion(),
                n.getTitulo(),
                n.getMensaje(),
                n.getTipo(),
                ""
            });
            if (n.getIdNotificacion() > 0) {
                idsVistos.add(n.getIdNotificacion());
            }
        }
        aplicarAlturaFilas();
    }

    private void aplicarAlturaFilas() {
        int rows = table.getRowCount();
        for (int i = 0; i < rows; i++) {
            Object idObj = table.getValueAt(i, 0);
            if (idObj == null) {
                table.setRowHeight(i, defaultRowHeight);
                continue;
            }
            int id = Integer.parseInt(String.valueOf(idObj));
            if (expandedIds.contains(id)) {
                table.setRowHeight(i, defaultRowHeight * 2);
            } else {
                table.setRowHeight(i, defaultRowHeight);
            }
        }
    }

    private void iniciarMonitorNuevas() {
        if (timerAvisos != null) {
            timerAvisos.stop();
        }
        // Verificar cada 5 segundos para notificaciones en tiempo real de traspasos
        timerAvisos = new Timer(5000, e -> {
            verificarNuevasNotificaciones();
        });
        timerAvisos.setInitialDelay(5000); // Iniciar verificación más rápida
        timerAvisos.start();
    }

    /**
     * Verifica eventos de traspasos en tiempo real y muestra notificaciones emergentes
     */
    private void verificarEventosTraspasosEnTiempoReal() {
        try {
            int idUsuario = obtenerIdUsuarioActual();
            Integer idBodega = obtenerIdBodegaActual();

            // Verificar si hay traspasos nuevos o actualizados
            boolean hayEventosTraspasos = false;

            // Consultar traspasos pendientes/recientes que no han sido notificados
            String sql = "SELECT t.id_traspaso, t.numero_traspaso, t.estado, t.fecha_creacion, " +
                         "       b_origen.nombre as origen, b_destino.nombre as destino " +
                         "FROM traspasos t " +
                         "JOIN bodegas b_origen ON t.id_bodega_origen = b_origen.id_bodega " +
                         "JOIN bodegas b_destino ON t.id_bodega_destino = b_destino.id_bodega " +
                         "WHERE t.fecha_creacion >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
                         "  AND NOT EXISTS (SELECT 1 FROM notificaciones n WHERE n.tipo_referencia = 'traspasos' " +
                         "       AND n.id_referencia = t.id_traspaso AND n.activa = 1)";

            if (idBodega != null) {
                sql += " AND (t.id_bodega_origen = ? OR t.id_bodega_destino = ?) ";
            }

            try (Connection conn = conexion.getInstance().createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                if (idBodega != null) {
                    stmt.setInt(1, idBodega);
                    stmt.setInt(2, idBodega);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        // Solo procesar el primer traspaso para evitar spam de notificaciones
                        if (count == 1) {
                            String numeroTraspaso = rs.getString("numero_traspaso");
                            String estado = rs.getString("estado");
                            String origen = rs.getString("origen");
                            String destino = rs.getString("destino");

                            String titulo = "Nuevo Traspaso";
                            String mensaje = String.format("Traspaso #%s (%s → %s) - Estado: %s",
                                    numeroTraspaso, origen, destino, estado);

                            // Mostrar notificación emergente para el evento de traspaso
                            mostrarNotificacionEmergente(titulo, mensaje, "traspasos");
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error verificando eventos de traspasos en tiempo real: " + e.getMessage());
        }
    }

    /**
     * Muestra una notificación emergente para eventos importantes
     */
    private void mostrarNotificacionEmergente(String titulo, String mensaje, String tipo) {
        SwingUtilities.invokeLater(() -> {
            Notification.Type tipoNotif = Notification.Type.INFO;
            if ("traspasos".equalsIgnoreCase(tipo) || "urgent".equalsIgnoreCase(tipo)) {
                tipoNotif = Notification.Type.WARNING;
            }

            new Notification((Frame) SwingUtilities.getWindowAncestor(this),
                    tipoNotif,
                    Notification.Location.TOP_RIGHT,
                    titulo,
                    mensaje,
                    null,
                    5000).showNotification();
        });
    }

    private void verificarNuevasNotificaciones() {
        int idUsuario = obtenerIdUsuarioActual();
        Integer idBodega = obtenerIdBodegaActual();
        String categoriaSel = mapCategoriaFiltro((String) categoria.getSelectedItem());
        String periodoSel = (String) CbxFiltro.getSelectedItem();
        String texto = txtSearch.getText();

        List<ModelNotificacion> lista;
        if (idBodega != null && idBodega > 0) {
            lista = notificacionesService.listarNotificacionesParaUsuarioYBodega(
                    idUsuario, idBodega, categoriaSel, periodoSel, texto);
        } else {
            lista = notificacionesService.listarNotificacionesParaUsuario(
                    idUsuario, categoriaSel, periodoSel, texto);
        }

        List<ModelNotificacion> nuevas = new ArrayList<>();
        for (ModelNotificacion n : lista) {
            int id = n.getIdNotificacion();
            if (id > 0 && !idsVistos.contains(id)) {
                nuevas.add(n);
            }
        }
        if (!nuevas.isEmpty()) {
            emitirSonidoNotificacion(nuevas);
            mostrarPopupNuevas(nuevas.size());
            for (ModelNotificacion n : nuevas) {
                int id = n.getIdNotificacion();
                if (id > 0) {
                    idsVistos.add(id);
                }
            }
            cargarNotificaciones();
        }
    }

    private void emitirSonidoNotificacion(List<ModelNotificacion> nuevas) {
        boolean hayUrgentes = false;
        for (ModelNotificacion n : nuevas) {
            String t = n.getTipo() != null ? n.getTipo().toLowerCase() : "";
            if ("urgent".equals(t) || "alert".equals(t)) {
                hayUrgentes = true;
                break;
            }
        }
        try {
            if (hayUrgentes) {
                CorporateTone.playAlert();
            } else {
                CorporateTone.playInfo();
            }
        } catch (Throwable ignore) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void mostrarPopupNuevas(int cantidad) {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        if (frame == null) return;
        ModelNotificacion primera = notificacionesActuales.isEmpty() ? null : notificacionesActuales.get(0);
        boolean urgente = false;
        String titulo = "Nueva notificación";
        String contenido = "Nuevas notificaciones: " + cantidad;
        org.kordamp.ikonli.Ikon icon = org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.BELL;
        if (primera != null) {
            String t = primera.getTipo() != null ? primera.getTipo().toLowerCase() : "";
            urgente = "urgent".equals(t) || "alert".equals(t) || "warning".equals(t);
            titulo = primera.getTitulo() != null ? primera.getTitulo() : titulo;
            contenido = primera.getMensaje() != null ? primera.getMensaje() : contenido;
            icon = urgente ? org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_TRIANGLE
                           : org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.BELL;
        }
        Notification.Type tipo = urgente ? Notification.Type.WARNING : Notification.Type.INFO;
        new Notification(frame, tipo, Notification.Location.TOP_RIGHT, titulo, contenido, icon, 3000).showNotification();
    }

    private Integer obtenerIdBodegaActual() {
        try {
            Integer bodegaId = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            if (bodegaId != null && bodegaId > 0) return bodegaId;
        } catch (Throwable ignore) {}
        try {
            Integer bodegaId = SessionManager.getInstance().getCurrentUserBodegaId();
            if (bodegaId != null && bodegaId > 0) return bodegaId;
        } catch (Throwable ignore) {}
        return null;
    }

    private String mapCategoriaFiltro(String seleccion) {
        if (seleccion == null) return null;
        String sel = seleccion.trim().toLowerCase();
        switch (sel) {
            case "ventas":
            case "venta":
                return "ventas";
            case "inventario":
                return "inventario";
            case "sistema":
                return "sistema";
            case "orden de venta":
                return "ordenes_reserva";
            case "transpasos":
            case "traspasos":
                return "traspasos";
            case "todas las categorias":
                return ""; // sin filtro
            default:
                return ""; // por defecto, sin filtro
        }
    }

    private void ejecutarAccion(int row, boolean marcarLeidaPrimero) {
        if (row < 0 || row >= notificacionesActuales.size()) return;
        ModelNotificacion n = notificacionesActuales.get(row);
        String ref = n.getTipoReferencia() != null ? n.getTipoReferencia() : "";
        if (marcarLeidaPrimero) {
            try {
                if (!"traspasos".equalsIgnoreCase(ref)) {
                    notificacionesService.marcarNotificacionLeida(n.getIdNotificacion());
                    cargarNotificaciones();
                } else {
                    Integer idTraspaso = n.getIdReferencia();
                    if (traspasoEstaRecibido(idTraspaso)) {
                        notificacionesService.marcarNotificacionLeida(n.getIdNotificacion());
                        cargarNotificaciones();
                    }
                }
            } catch (Exception ignore) {}
        }
        // Seleccionar la fila en la tabla visualmente
        try {
            table.getSelectionModel().setSelectionInterval(row, row);
        } catch (Exception ignore) {}
        // Redirigir
        redirigirSegunNotificacion(n);
    }

    private boolean traspasoEstaRecibido(Integer idTraspaso) {
        if (idTraspaso == null || idTraspaso <= 0) return false;
        String sql = "SELECT estado FROM traspasos WHERE id_traspaso = ? LIMIT 1";
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString(1);
                    return "recibido".equalsIgnoreCase(estado);
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     *  REDIRECCIÓN MEJORADA Y PRECISA POR TIPO DE NOTIFICACIÓN
     * Redirige según el tipo de notificación y su referencia específica
     */
    private void redirigirSegunNotificacion(ModelNotificacion n) {
        String ref = n.getTipoReferencia() != null ? n.getTipoReferencia() : "";
        String titulo = n.getTitulo() != null ? n.getTitulo().toLowerCase() : "";
        String categoria = n.getCategoria() != null ? n.getCategoria().toLowerCase() : "";
        Integer idRef = n.getIdReferencia();

        System.out.println(" Redirigiendo notificación:");
        System.out.println("   - Tipo Ref: " + ref);
        System.out.println("   - Categoría: " + categoria);
        System.out.println("   - Título: " + titulo);
        System.out.println("   - ID Ref: " + idRef);

        // ============================================================
        // 1. TRASPASOS (autorizados, enviados, pendientes, recibidos)
        // ============================================================
        if ("traspasos".equalsIgnoreCase(ref)) {
            Application.setSelectedMenu(4, 7); // Productos -> Traspasos
            try {
                traspasos form = new traspasos();
                Application.showForm(form);

                if (idRef != null && idRef > 0) {
                    // Esperar a que la UI se cargue antes de resaltar
                    SwingUtilities.invokeLater(() -> {
                        try {
                            form.resaltarTraspasoPorId(idRef);
                            System.out.println("SUCCESS  Traspaso #" + idRef + " resaltado");
                        } catch (Exception ex) {
                            System.err.println("WARNING  No se pudo resaltar traspaso: " + ex.getMessage());
                        }
                    });
                }
            } catch (Exception ex) {
                System.err.println("ERROR  Error al abrir traspasos: " + ex.getMessage());
                raven.modal.Toast.show(this, raven.modal.Toast.Type.ERROR,
                    "Error al abrir traspasos: " + ex.getMessage());
            }
            return;
        }

        // ============================================================
        // 2. PEDIDOS WEB / ÓRDENES DE RESERVA
        // ============================================================
        if ("ordenes_reserva".equalsIgnoreCase(ref) ||
            titulo.contains("pedido web") ||
            titulo.contains("orden de compra")) {

            Application.setSelectedMenu(3, 5); // Comercial -> Pedidos Web
            try {
                if (idRef != null && idRef > 0) {
                    // Abrir Carrito con la orden específica
                    Carrito carrito = new Carrito(idRef);
                    Application.showForm(carrito);
                    System.out.println("SUCCESS  Orden #" + idRef + " abierta en Carrito");
                } else {
                    // Sin ID, abrir Carrito vacío
                    Carrito carrito = new Carrito();
                    Application.showForm(carrito);
                }
            } catch (SQLException ex) {
                System.err.println("ERROR  Error al abrir orden: " + ex.getMessage());
                raven.modal.Toast.show(this, raven.modal.Toast.Type.ERROR,
                    "Error al abrir orden: " + ex.getMessage());
            } catch (Exception ex) {
                System.err.println("ERROR  Error inesperado: " + ex.getMessage());
                raven.modal.Toast.show(this, raven.modal.Toast.Type.ERROR,
                    "Error al abrir orden: " + ex.getMessage());
            }
            return;
        }

        // ============================================================
        // 3. VENTAS / REPORTES DE VENTAS
        // ============================================================
        if ("ventas".equalsIgnoreCase(ref) ||
            "venta".equalsIgnoreCase(ref) ||
            categoria.contains("ventas")) {

            Application.setSelectedMenu(3, 3); // Comercial -> Reporte de Ventas
            try {
                reporteVentas form = new reporteVentas();
                Application.showForm(form);
                System.out.println("SUCCESS  Reporte de ventas abierto");
            } catch (Exception ex) {
                System.err.println("ERROR  Error al abrir ventas: " + ex.getMessage());
            }
            return;
        }

        // ============================================================
        // 4. ALERTAS DE CAJA
        // ============================================================
        if ("caja_alerta".equalsIgnoreCase(ref) || titulo.contains("caja abierta")) {
            // Ir a Dashboard o Caja según disponibilidad
            Application.setSelectedMenu(0, 0); // Dashboard
            raven.modal.Toast.show(this, raven.modal.Toast.Type.WARNING,
                "Recordatorio: Tienes una caja abierta. Ciérrala cuando termines.");
            return;
        }

        // ============================================================
        // 5. INVENTARIO GENERAL
        // ============================================================
        if (categoria.contains("inventario")) {
            Application.setSelectedMenu(4, 0); // Productos -> Gestión
            try {
                GestionProductosForm form = new GestionProductosForm();
                Application.showForm(form);
                System.out.println("SUCCESS  Gestión de productos abierta");
            } catch (Exception ex) {
                System.err.println("ERROR  Error al abrir productos: " + ex.getMessage());
            }
            return;
        }

        // ============================================================
        // 6. SISTEMA / ALERTAS GENERALES
        // ============================================================
        if (categoria.contains("sistema")) {
            Application.setSelectedMenu(0, 0); // Dashboard
            System.out.println("ℹ Notificación de sistema, redirigiendo a Dashboard");
            return;
        }

        // ============================================================
        // 7. DEFAULT - IR A DASHBOARD
        // ============================================================
        System.out.println("WARNING  Tipo de notificación no reconocido, ir a Dashboard");
        Application.setSelectedMenu(0, 0);
        raven.modal.Toast.show(this, raven.modal.Toast.Type.INFO,
            "Notificación: " + n.getTitulo());
    }
    
    /**
     * Inicializa el controlador y conecta la vista
     */
    // Controladores específicos de Notificaciones no requeridos aquí
    
     private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }


     


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();
        CbxFiltro = new javax.swing.JComboBox<>();
        categoria = new javax.swing.JComboBox<>();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        LbUser = new javax.swing.JLabel();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Notificaciones");

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("Notificaciones");

        CbxFiltro.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hoy", "Semana", "Mes" }));

        categoria.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todas las categorias", "Venta", "Inventario", "Sistema", "Orden de venta", "Transpasos" }));

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(panelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLayout.createSequentialGroup()
                        .addComponent(lbTitle)
                        .addGap(0, 615, Short.MAX_VALUE))
                    .addGroup(panelLayout.createSequentialGroup()
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(CbxFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(categoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CbxFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(categoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "N°", "Titulo", "Mensaje", "Tipo", "Acciones"
            }
        ) {
            boolean[] canEdit = new boolean [] {

                //al final true para que funcione el boton
                false, false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setCellSelectionEnabled(true);
        table.setDragEnabled(true);
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);

        LbUser.setFont(new java.awt.Font("Segoe UI Historic", 1, 14)); // NOI18N
        LbUser.setText("Crisdev");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scroll)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LbUser, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(101, 101, 101)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lb)
                    .addComponent(LbUser))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 656, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        cargarNotificaciones();
    }//GEN-LAST:event_txtSearchKeyReleased

    // Se agregan listeners simples para recargar por filtros
    @Override
    public void addNotify() {
        super.addNotify();
        CbxFiltro.addActionListener(e -> cargarNotificaciones());
        categoria.addActionListener(e -> cargarNotificaciones());
    }
    
//    @Override
//    public void removeNotify() {
//        super.removeNotify();
//        try { notificacionesService.stopRealtime(); } catch (Exception ignore) {}
//    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (timerAvisos != null) {
            try { timerAvisos.stop(); } catch (Throwable ignore) {}
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> CbxFiltro;
    private javax.swing.JLabel LbUser;
    private javax.swing.JComboBox<String> categoria;
    private javax.swing.JSeparator jSeparator1;
    public javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    public javax.swing.JTable table;
    public javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
