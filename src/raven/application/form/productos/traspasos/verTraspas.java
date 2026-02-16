/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Image;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import raven.controlador.principal.conexion;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.productos.RecepcionTraspaso;
import raven.modal.ModalDialog;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalCallback;
import raven.modal.listener.ModalController;

public class verTraspas extends javax.swing.JPanel {

        private final FontIcon iconStatus1;
        private final FontIcon iconStatus2;
        private final FontIcon iconBodega;
        private final FontIcon iconBodegaD;
        private final FontIcon iconUse;
        private final FontIcon iconCdr;
        // ═══════════════════════════════════════════════════════════════════════════
        // ESTILOS MODERNOS - FlatLaf Optimizados
        // ═══════════════════════════════════════════════════════════════════════════
        // ═══════════════════════════════════════════════════════════════════════════
        // ESTILOS MODERNOS - FlatLaf Optimizados (PREMIUM)
        // ═══════════════════════════════════════════════════════════════════════════
        private static final String PANEL = "arc:20;background:$Panel.background";
        private static final String CONTAINER = "arc:20;background:$Panel.background;border:1,1,1,1,shade($Panel.background, 5%),,20";
        private static final String CONTAINER1 = "arc:20;background:lighten($Panel.background,2%)";
        private static final String STYLE_HEADER = "font:bold +10";
        private static final String STYLE_SUBHEADER = "font:bold +5";
        private static final String STYLE_BTN_PRIMARY = "arc:15;background:#2563EB;foreground:#ffffff;font:bold +2";
        private static final String STYLE_BTN_SUCCESS = "arc:15;background:#10B981;foreground:#ffffff;font:bold +2"; // Emerald
                                                                                                                     // 500
        private static final String STYLE_BTN_EXPORT = "arc:15;background:#8B5CF6;foreground:#ffffff;font:bold"; // Violet
                                                                                                                 // 500
        // Increased contrast and lighter background for info cards
        private static final String STYLE_INFO_CARD = "arc:20;background:lighten($Panel.background,3%);border:1,1,1,1,shade($Panel.background, 10%),,10";

        // Timeline Styles - High Contrast
        private static final String STYLE_TIMELINE_DONE = "arc:50;background:#10B981;border:0,0,0,0";
        private static final String STYLE_TIMELINE_PENDING = "arc:50;background:$Label.disabledForeground;border:0,0,0,0";

        private String numeroTraspasoActual = "";
        private final DecimalFormat formatoNumero;
        private final SimpleDateFormat formatoFecha;
        private String estadoActual = "pendiente";

        public verTraspas() {

                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setGroupingSeparator('.');
                this.formatoNumero = new DecimalFormat("#,###", symbols);
                this.formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                initComponents();
                // Configurar formato para números
                symbols.setGroupingSeparator('.');
                // this.formatoNumero = new DecimalFormat("#,###", symbols);

                initComponents();
                Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
                iconStatus1 = createColoredIcon(FontAwesomeSolid.HOURGLASS, tabTextColor);
                iconStatus2 = createColoredIcon(FontAwesomeSolid.SHOPPING_CART, tabTextColor);
                iconBodega = createColoredIcon(FontAwesomeSolid.BUILDING, tabTextColor);
                iconBodegaD = createColoredIcon(FontAwesomeSolid.BUSINESS_TIME, tabTextColor);
                iconUse = createColoredIcon(FontAwesomeSolid.USER, tabTextColor);
                iconCdr = createColoredIcon(FontAwesomeSolid.CALENDAR_DAY, tabTextColor);

                iconos();
                initInterfaz();
                configurarTabla();
        }

        public void initInterfaz() {
                // ═══════════════════════════════════════════════════════════════════════════
                // ESTILOS DE PANELES PRINCIPALES
                // ═══════════════════════════════════════════════════════════════════════════
                panelTitulo.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
                pnlInfoGen.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
                panelSegu1.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                panelSegu2.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                panelSegu3.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                panelSegu4.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                panelSegui.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
                panelProdu.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
                panelBarr.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
                pnlInfoGen1.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                pnlInfoGen2.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                pnlInfoGen3.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                pnlInfoGen4.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_CARD);
                txtObservaciones.putClientProperty(FlatClientProperties.STYLE,
                                "background:lighten($Menu.background,25%)");
                txtMotivo.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");
                jScrollPane1.getVerticalScrollBar().setUnitIncrement(30);
                jScrollPane1.getHorizontalScrollBar().setUnitIncrement(30);

                // ═══════════════════════════════════════════════════════════════════════════
                // ESTILOS DE TÍTULOS CON ICONOS
                // ═══════════════════════════════════════════════════════════════════════════
                Color iconColor = UIManager.getColor("Label.foreground");

                // Título principal
                jLabel20.putClientProperty(FlatClientProperties.STYLE, STYLE_HEADER);
                FontIcon iconTitulo = FontIcon.of(FontAwesomeSolid.FILE_ALT);
                iconTitulo.setIconSize(28);
                iconTitulo.setIconColor(iconColor);
                jLabel20.setIcon(iconTitulo);
                jLabel20.setIconTextGap(12);

                // Título información general
                jLabel19.putClientProperty(FlatClientProperties.STYLE, STYLE_SUBHEADER);
                FontIcon iconInfo = FontIcon.of(FontAwesomeSolid.INFO_CIRCLE);
                iconInfo.setIconSize(18);
                iconInfo.setIconColor(new Color(33, 150, 243));
                jLabel19.setIcon(iconInfo);
                jLabel19.setIconTextGap(8);

                // Título seguimiento
                jLabel3.putClientProperty(FlatClientProperties.STYLE, STYLE_SUBHEADER);
                FontIcon iconTimeline = FontIcon.of(FontAwesomeSolid.ROUTE);
                iconTimeline.setIconSize(18);
                iconTimeline.setIconColor(new Color(76, 175, 80));
                jLabel3.setIcon(iconTimeline);
                jLabel3.setIconTextGap(8);

                // Título productos
                jLabel12.putClientProperty(FlatClientProperties.STYLE, STYLE_SUBHEADER);
                FontIcon iconProducts = FontIcon.of(FontAwesomeSolid.BOXES);
                iconProducts.setIconSize(18);
                iconProducts.setIconColor(new Color(255, 152, 0));
                jLabel12.setIcon(iconProducts);
                jLabel12.setIconTextGap(8);

                // ═══════════════════════════════════════════════════════════════════════════
                // ESTILOS DE BOTONES DE ACCIÓN
                // ═══════════════════════════════════════════════════════════════════════════
                btnEnviar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_PRIMARY);
                btnEnviar.setForeground(Color.WHITE);
                FontIcon iconEnviar = FontIcon.of(FontAwesomeSolid.PAPER_PLANE);
                iconEnviar.setIconSize(14);
                iconEnviar.setIconColor(Color.WHITE);
                btnEnviar.setIcon(iconEnviar);
                btnEnviar.setIconTextGap(8);

                btnExportar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_EXPORT);
                btnExportar.setForeground(Color.WHITE);
                FontIcon iconExport = FontIcon.of(FontAwesomeSolid.FILE_EXPORT);
                iconExport.setIconSize(14);
                iconExport.setIconColor(Color.WHITE);
                btnExportar.setIcon(iconExport);
                btnExportar.setIconTextGap(8);

                // ═══════════════════════════════════════════════════════════════════════════
                // ESTILOS DE ETIQUETAS DE TOTALES
                // ═══════════════════════════════════════════════════════════════════════════
                jLabel13.putClientProperty(FlatClientProperties.STYLE, "font:bold");
                jLabel16.putClientProperty(FlatClientProperties.STYLE, "font:bold");
                jLabel18.putClientProperty(FlatClientProperties.STYLE, "font:bold");
                jLabel14.setForeground(new Color(33, 150, 243)); // Azul - solicitado
                jLabel15.setForeground(new Color(255, 152, 0)); // Naranja - enviado
                jLabel17.setForeground(new Color(76, 175, 80)); // Verde - recibido
                jLabel14.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
                jLabel15.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
                jLabel17.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        }

        public void iconos() {
                btnStatus1.setIcon(iconStatus1);
                iconOrigen.setIcon(iconBodega);
                iconDesti.setIcon(iconBodegaD);
                IconUser.setIcon(iconUse);
                iconCalendar.setIcon(iconCdr);
                btnStatus2.setIcon(iconStatus1);
                btnStatus3.setIcon(iconStatus1);
                btnStatus4.setIcon(iconStatus1);
                imgStatus1.putClientProperty(FlatClientProperties.STYLE, PANEL);
                btnStatus1.putClientProperty(FlatClientProperties.STYLE, PANEL);
                imgStatus2.putClientProperty(FlatClientProperties.STYLE, PANEL);
                btnStatus2.putClientProperty(FlatClientProperties.STYLE, PANEL);
                imgStatus3.putClientProperty(FlatClientProperties.STYLE, PANEL);
                btnStatus3.putClientProperty(FlatClientProperties.STYLE, PANEL);
                imgStatus4.putClientProperty(FlatClientProperties.STYLE, PANEL);
                btnStatus4.putClientProperty(FlatClientProperties.STYLE, PANEL);

        }

        private FontIcon createColoredIcon(Ikon icon, Color color) {
                FontIcon fontIcon = FontIcon.of(icon);
                fontIcon.setIconSize(18);
                fontIcon.setIconColor(color);
                return fontIcon;
        }

        /**
         * Configura la información básica del traspaso
         */
        public void setTraspasoInfo(String numeroTraspaso, String fechaSolicitud, String estado,
                        String bodegaOrigen, String bodegaDestino, String usuarioSolicita,
                        String motivo, String observaciones) {

                // Configurar header
                jLabel20.setText("Detalle de Traspaso");

                // Guardar datos importantes
                this.numeroTraspasoActual = numeroTraspaso;
                this.estadoActual = estado;

                // Configurar información general
                jLabel23.setText(bodegaOrigen);
                jLabel24.setText(bodegaDestino);
                jLabel26.setText(usuarioSolicita);
                jLabel28.setText(extraerHora(fechaSolicitud));
                // FIX: Update Timeline Date for Created Status
                if (fechaSolicitud != null) {
                        jLabel5.setText(extraerHora(fechaSolicitud));
                } else {
                        jLabel5.setText("-");
                }

                // Configurar campos de texto
                txtMotivo.setText(motivo != null ? motivo : "");
                txtObservaciones.setText(observaciones != null ? observaciones : "");

                // Hacer campos de solo lectura
                txtMotivo.setEditable(false);
                txtObservaciones.setEditable(false);

                // Configurar estado, timeline y botones
                configurarEstadoYTimeline(estado);
                configurarBotones(estado);
                configurarBadgeEstado(estado);
        }
        // ===== AGREGAR AL FINAL DE LA CLASE =====

        /**
         * Listener para eventos de actualización del traspaso
         */
        public interface TraspasoUpdateListener {

                void onTraspasoActualizado(String numeroTraspaso, String nuevoEstado);
        }

        private TraspasoUpdateListener updateListener;

        public void setUpdateListener(TraspasoUpdateListener listener) {
                this.updateListener = listener;
        }

        /**
         * Notifica cambios en el traspaso
         */
        private void notificarActualizacion(String nuevoEstado) {
                if (updateListener != null) {
                        updateListener.onTraspasoActualizado(numeroTraspasoActual, nuevoEstado);
                }
        }

        public void setFechaAutorizacion(String fechaAutorizacion, String usuarioAutoriza) {
                if (fechaAutorizacion != null && !fechaAutorizacion.isEmpty()) {
                        jLabel7.setText(extraerHora(fechaAutorizacion));
                        if (usuarioAutoriza != null) {
                                jLabel6.setText("Autorizado por " + usuarioAutoriza);
                        }
                        actualizarIconoEstado(2, true);
                }
        }

        public void setFechaEnvio(String fechaEnvio) {
                if (fechaEnvio != null && !fechaEnvio.isEmpty()) {
                        jLabel9.setText(extraerHora(fechaEnvio));
                        actualizarIconoEstado(3, true);
                }
        }

        public void setFechaRecepcion(String fechaRecepcion, String usuarioRecibe) {
                if (fechaRecepcion != null && !fechaRecepcion.isEmpty()) {
                        jLabel11.setText(extraerHora(fechaRecepcion));
                        if (usuarioRecibe != null) {
                                jLabel10.setText("Recibido por " + usuarioRecibe);
                        }
                        actualizarIconoEstado(4, true);
                }
        }

        /**
         * Configura los productos del traspaso
         */
        public void setProductos(List<Map<String, Object>> productos) {
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                model.setRowCount(0);

                int totalSolicitado = 0;
                int totalEnviado = 0;
                int totalRecibido = 0;

                for (Map<String, Object> producto : productos) {
                        int solicitada = Integer.parseInt(producto.get("cantidad_solicitada").toString());
                        int enviada = Integer.parseInt(producto.get("cantidad_enviada").toString());
                        int recibida = Integer.parseInt(producto.get("cantidad_recibida").toString());

                        totalSolicitado += solicitada;
                        totalEnviado += enviada;
                        totalRecibido += recibida;

                        // Construir descripción completa
                        String descripcion = producto.get("descripcion_completa") != null
                                        ? producto.get("descripcion_completa").toString()
                                        : producto.get("producto_nombre").toString();

                        Integer idVariante = null;
                        Object idvObj = producto.get("id_variante");
                        if (idvObj instanceof Number) {
                                idVariante = ((Number) idvObj).intValue();
                        } else {
                                try {
                                        idVariante = Integer.parseInt(String.valueOf(idvObj));
                                } catch (Exception ignore) {
                                }
                        }

                        ImageIcon icono = loadVarianteIcon(idVariante, 32);
                        ProductoCellData cellData = new ProductoCellData(descripcion, idVariante, icono);

                        String genero = producto.get("genero") != null ? String.valueOf(producto.get("genero")) : "";

                        model.addRow(new Object[] {
                                        cellData,
                                        genero,
                                        solicitada,
                                        enviada,
                                        recibida,
                                        producto.get("estado_detalle"),
                                        producto.get("observaciones")
                        });
                }

                // Actualizar totales
                jLabel14.setText(formatoNumero.format(totalSolicitado));
                jLabel15.setText(formatoNumero.format(totalEnviado));
                jLabel17.setText(formatoNumero.format(totalRecibido));
        }

        /**
         * ===== MÉTODOS AUXILIARES =====
         */
        private void configurarEstadoYTimeline(String estado) {
                // Configurar todos los iconos según el estado
                switch (estado.toLowerCase()) {
                        case "pendiente":
                                actualizarIconoEstado(1, true);
                                actualizarIconoEstado(2, false);
                                actualizarIconoEstado(3, false);
                                actualizarIconoEstado(4, false);
                                break;
                        case "autorizado":
                                actualizarIconoEstado(1, true);
                                actualizarIconoEstado(2, true);
                                actualizarIconoEstado(3, false);
                                actualizarIconoEstado(4, false);
                                break;
                        case "en_transito":
                                actualizarIconoEstado(1, true);
                                actualizarIconoEstado(2, true);
                                actualizarIconoEstado(3, true);
                                actualizarIconoEstado(4, false);
                                break;
                        case "recibido":
                                actualizarIconoEstado(1, true);
                                actualizarIconoEstado(2, true);
                                actualizarIconoEstado(3, true);
                                actualizarIconoEstado(4, true);
                                break;
                }
        }

        private void configurarBadgeEstado(String estado) {
                String numeroTraspaso = this.numeroTraspasoActual;
                String estadoFormateado = estado.toUpperCase();

                // Cambiar texto del subtítulo según el estado
                switch (estado.toLowerCase()) {
                        case "pendiente":
                                jLabel21.setText("Número: " + numeroTraspaso + " -  " + estadoFormateado);
                                break;
                        case "autorizado":
                                jLabel21.setText("Número: " + numeroTraspaso + " - SUCCESS  " + estadoFormateado);
                                break;
                        case "en_transito":
                                jLabel21.setText("Número: " + numeroTraspaso + " -  " + estadoFormateado);
                                break;
                        case "recibido":
                                jLabel21.setText("Número: " + numeroTraspaso + " - SUCCESS  " + estadoFormateado);
                                break;
                        case "cancelado":
                                jLabel21.setText("Número: " + numeroTraspaso + " - ERROR  " + estadoFormateado);
                                break;
                        default:
                                jLabel21.setText("Número: " + numeroTraspaso + " - " + estadoFormateado);
                                break;
                }
        }

        private ImageIcon loadVarianteIcon(Integer idVariante, int alturaMaxima) {
                if (idVariante == null || idVariante <= 0)
                        return null;
                String sql = "SELECT imagen FROM producto_variantes WHERE id_variante = ? AND imagen IS NOT NULL";
                try (Connection con = conexion.getInstance().createConnection();
                                PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, idVariante);
                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        byte[] bytes = rs.getBytes("imagen");
                                        if (bytes != null && bytes.length > 0) {
                                                ImageIcon original = new ImageIcon(bytes);
                                                Image img = original.getImage();
                                                int h = alturaMaxima;
                                                int w = img.getWidth(null);
                                                int oh = img.getHeight(null);
                                                if (w > 0 && oh > 0) {
                                                        int newW = (int) Math.round((double) w * h / (double) oh);
                                                        Image scaled = img.getScaledInstance(newW, h,
                                                                        Image.SCALE_SMOOTH);
                                                        return new ImageIcon(scaled);
                                                }
                                                return original;
                                        }
                                }
                        }
                } catch (SQLException ignore) {
                }
                return null;
        }

        private static class ProductoCellData {
                final String texto;
                final Integer idVariante;
                final ImageIcon icon;

                ProductoCellData(String texto, Integer idVariante, ImageIcon icon) {
                        this.texto = texto;
                        this.idVariante = idVariante;
                        this.icon = icon;
                }
        }

        private void actualizarIconoEstado(int paso, boolean completado) {
                // Colores más vibrantes y modernos
                Color color = completado ? new Color(16, 185, 129) : new Color(158, 158, 158); // Emerald o gris
                Ikon iconType = completado ? FontAwesomeSolid.CHECK : FontAwesomeSolid.CIRCLE;

                FontIcon icono = FontIcon.of(iconType);
                icono.setIconSize(24); // Más grande
                icono.setIconColor(color);

                // Aplicar estilos al panel contenedor
                String panelStyle = completado ? STYLE_TIMELINE_DONE : STYLE_TIMELINE_PENDING;

                switch (paso) {
                        case 1:
                                btnStatus1.setIcon(icono);
                                imgStatus1.putClientProperty(FlatClientProperties.STYLE, panelStyle);
                                break;
                        case 2:
                                btnStatus2.setIcon(icono);
                                imgStatus2.putClientProperty(FlatClientProperties.STYLE, panelStyle);
                                break;
                        case 3:
                                btnStatus3.setIcon(icono);
                                imgStatus3.putClientProperty(FlatClientProperties.STYLE, panelStyle);
                                break;
                        case 4:
                                btnStatus4.setIcon(icono);
                                imgStatus4.putClientProperty(FlatClientProperties.STYLE, panelStyle);
                                break;
                }
        }

        private String extraerHora(String fechaCompleta) {
                // CORRECCIÓN: Devolver fecha y hora completa formateada
                try {
                        if (fechaCompleta == null || fechaCompleta.isEmpty())
                                return "-";

                        // Limpiar string de posibles milisegundos extra o formatos raros
                        String fechaLimpia = fechaCompleta;
                        if (fechaLimpia.contains(".")) {
                                fechaLimpia = fechaLimpia.substring(0, fechaLimpia.indexOf("."));
                        }

                        // Si viene en formato SQL yyyy-MM-dd HH:mm:ss
                        if (fechaLimpia.contains("-") && fechaLimpia.contains(":")) {
                                try {
                                        java.util.Date date = java.sql.Timestamp.valueOf(fechaLimpia);
                                        // Formato amigable: 23/01/2026 13:45
                                        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
                                } catch (Exception e) {
                                        // Fallback manual si Timestamp falla
                                        return fechaLimpia;
                                }
                        }
                        return fechaCompleta;
                } catch (Exception e) {
                        return fechaCompleta;
                }
        }

        private void configurarTabla() {
                // ═══════════════════════════════════════════════════════════════════════════
                // ESTILOS MODERNOS DE TABLA - Optimizado para uso diario
                // ═══════════════════════════════════════════════════════════════════════════
                jTable1.putClientProperty(FlatClientProperties.STYLE,
                                "showHorizontalLines:true;"
                                                + "showVerticalLines:false;"
                                                + "rowHeight:48;"
                                                + "intercellSpacing:8,8;"
                                                + "selectionBackground:$TableHeader.hoverBackground;"
                                                + "selectionForeground:$Table.foreground");

                // Encabezado con estilo premium
                jTable1.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                                "hoverBackground:$Table.background;"
                                                + "height:45;"
                                                + "separatorColor:$TableHeader.background;"
                                                + "font:bold $h4.font");

                // Configurar modelo de tabla
                DefaultTableModel model = new DefaultTableModel(
                                new Object[][] {},
                                new String[] { "Producto", "Género", "Solicitada", "Enviada", "Recibida", "Estado",
                                                "Observaciones" }) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };
                jTable1.setModel(model);

                // Configurar renderers
                configurarRenderersTabla();
        }

        private void configurarRenderersTabla() {
                // Renderer para cantidades (centrado)
                DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

                // Renderer para primera columna: imagen + texto
                jTable1.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                        boolean hasFocus, int row, int column) {
                                JLabel label = (JLabel) super.getTableCellRendererComponent(table, null, isSelected,
                                                hasFocus, row,
                                                column);
                                label.setHorizontalAlignment(JLabel.LEFT);
                                String texto = "";
                                ImageIcon icon = null;
                                if (value instanceof ProductoCellData) {
                                        ProductoCellData data = (ProductoCellData) value;
                                        texto = data.texto != null ? data.texto : "";
                                        icon = data.icon;
                                } else if (value != null) {
                                        texto = String.valueOf(value);
                                }
                                label.setText(texto);
                                label.setIcon(icon);
                                label.setIconTextGap(8);
                                return label;
                        }
                });

                // Aplicar renderer a columnas de cantidades
                jTable1.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Solicitada
                jTable1.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Enviada
                jTable1.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Recibida

                // Renderer para estado con colores
                jTable1.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value,
                                        boolean isSelected, boolean hasFocus, int row, int column) {
                                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                                                isSelected, hasFocus, row, column);
                                label.setHorizontalAlignment(JLabel.CENTER);

                                if (value != null) {
                                        String estado = value.toString().toLowerCase();
                                        String color = getColorForDetalleStatus(estado);

                                        label.setText("<html><div style='padding: 2px 6px; border-radius: 8px; "
                                                        + "display: inline-block; font-size: 10px; font-weight: bold; "
                                                        + "background-color: " + color + "; "
                                                        + "color: white;'>" + value.toString() + "</div></html>");
                                }
                                return label;
                        }

                        private String getColorForDetalleStatus(String status) {
                                switch (status) {
                                        case "pendiente":
                                                return "#FFC107";
                                        case "enviado":
                                                return "#17A2B8";
                                        case "recibido":
                                                return "#28A745";
                                        case "faltante":
                                                return "#DC3545";
                                        default:
                                                return "#6C757D";
                                }
                        }
                });

                if (jTable1.getColumnModel().getColumnCount() > 0) {
                        jTable1.getColumnModel().getColumn(0).setPreferredWidth(320); // Producto (con imagen)
                        jTable1.getColumnModel().getColumn(1).setPreferredWidth(90); // Género
                        jTable1.getColumnModel().getColumn(2).setPreferredWidth(80); // Solicitada
                        jTable1.getColumnModel().getColumn(3).setPreferredWidth(80); // Enviada
                        jTable1.getColumnModel().getColumn(4).setPreferredWidth(80); // Recibida
                        jTable1.getColumnModel().getColumn(5).setPreferredWidth(100); // Estado
                        jTable1.getColumnModel().getColumn(6).setPreferredWidth(150); // Observaciones
                }

                // Configurar anchos de columna
                if (jTable1.getColumnModel().getColumnCount() > 0) {
                        jTable1.getColumnModel().getColumn(0).setPreferredWidth(320); // Producto (con imagen)
                        jTable1.getColumnModel().getColumn(1).setPreferredWidth(90); // Género
                        jTable1.getColumnModel().getColumn(2).setPreferredWidth(80); // Solicitada
                        jTable1.getColumnModel().getColumn(3).setPreferredWidth(80); // Enviada
                        jTable1.getColumnModel().getColumn(4).setPreferredWidth(80); // Recibida
                        jTable1.getColumnModel().getColumn(5).setPreferredWidth(100); // Estado
                        jTable1.getColumnModel().getColumn(6).setPreferredWidth(150); // Observaciones
                }
        }

        private boolean verificarEstadoTraspaso(String numeroTraspaso) throws SQLException {
                String sql = "SELECT estado FROM traspasos WHERE numero_traspaso = ?";

                Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, numeroTraspaso);
                ResultSet rs = stmt.executeQuery();

                boolean esValido = false;
                if (rs.next()) {
                        String estado = rs.getString("estado");
                        esValido = "autorizado".equalsIgnoreCase(estado);
                }

                rs.close();
                stmt.close();
                conn.close();

                return esValido;
        }

        private String determinarAccionBoton() {
                switch (estadoActual.toLowerCase()) {
                        case "autorizado":
                                return "enviar";
                        case "en_transito":
                                return "recibir";
                        default:
                                return "ninguna";
                }
        }

        /**
         * Procesa el envío del traspaso usando EnvioTraspaso
         */
        private void procesarEnvioTraspaso() {
                // Obtener el contenedor padre de forma segura
                java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
                java.awt.Frame parentFrame = obtenerFramePadre(parentWindow);

                // Mostrar modal de envío usando EnvioTraspaso
                try {
                        EnvioTraspaso.enviarTraspaso(parentFrame, numeroTraspasoActual,
                                        new EnvioTraspaso.EnvioCallback() {
                                                @Override
                                                public void onEnvioExitoso(String numeroTraspaso,
                                                                List<Map<String, Object>> productosEnviados) {

                                                        System.out.println(
                                                                        "SUCCESS  Traspaso enviado: " + numeroTraspaso);
                                                        System.out.println("Caja Productos enviados: "
                                                                        + productosEnviados.size());

                                                        // Actualizar estado local
                                                        estadoActual = "en_transito";

                                                        // Actualizar interfaz
                                                        actualizarInterfazDespuesDeEnvio();

                                                        // Mostrar mensaje de éxito
                                                        javax.swing.JOptionPane.showMessageDialog(verTraspas.this,
                                                                        "Traspaso enviado exitosamente.\n"
                                                                                        + "Productos enviados: "
                                                                                        + productosEnviados.size(),
                                                                        "Envío Exitoso",
                                                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

                                                        // Opcional: Recargar datos desde BD para sincronizar
                                                        recargarDatosTraspaso();
                                                }

                                                @Override
                                                public void onEnvioCancelado() {
                                                        System.out.println("ERROR  Envío cancelado por el usuario");
                                                }
                                        });
                } catch (Exception e) {
                        e.printStackTrace();
                        javax.swing.JOptionPane.showMessageDialog(this,
                                        "Error al abrir modal de envío: " + e.getMessage(),
                                        "Error",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
        }

        /**
         * Procesa la recepción del traspaso usando RecepcionTraspaso
         */
        private void procesarRecepcionTraspaso() {
                // Obtener el contenedor padre de forma segura
                java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
                java.awt.Frame parentFrame = obtenerFramePadre(parentWindow);

                // Mostrar modal de recepción usando RecepcionTraspaso
                try {
                        RecepcionTraspaso.recibirTraspaso(parentFrame, numeroTraspasoActual,
                                        new RecepcionTraspaso.RecepcionCallback() {
                                                @Override
                                                public void onRecepcionExitosa(String numeroTraspaso,
                                                                List<Map<String, Object>> productosRecibidos) {

                                                        System.out.println("SUCCESS  Traspaso recibido: "
                                                                        + numeroTraspaso);
                                                        System.out.println("Caja Productos recibidos: "
                                                                        + productosRecibidos.size());

                                                        // Actualizar estado local
                                                        estadoActual = "recibido";

                                                        // Actualizar interfaz
                                                        actualizarInterfazDespuesDeRecepcion();

                                                        // Mostrar mensaje de éxito
                                                        javax.swing.JOptionPane.showMessageDialog(verTraspas.this,
                                                                        "Traspaso recibido exitosamente.\n"
                                                                                        + "Productos recibidos: "
                                                                                        + productosRecibidos.size(),
                                                                        "Recepción Exitosa",
                                                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

                                                        // Notificar actualización
                                                        notificarActualizacion("recibido");

                                                        // Opcional: Recargar datos desde BD para sincronizar
                                                        recargarDatosTraspaso();
                                                }

                                                @Override
                                                public void onRecepcionCancelada() {
                                                        System.out.println("ERROR  Recepción cancelada por el usuario");
                                                }
                                        });
                } catch (Exception e) {
                        e.printStackTrace();
                        javax.swing.JOptionPane.showMessageDialog(this,
                                        "Error al abrir modal de recepción: " + e.getMessage(),
                                        "Error",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
        }

        /**
         * Actualiza la interfaz después de la recepción exitosa
         */
        private void actualizarInterfazDespuesDeRecepcion() {
                // Actualizar timeline y iconos
                configurarEstadoYTimeline("recibido");

                // Actualizar botones
                configurarBotones("recibido");

                // Actualizar badge de estado
                configurarBadgeEstado("recibido");

                // Establecer fecha de recepción actual
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                setFechaRecepcion(sdf.format(new java.util.Date()), "Usuario Actual");

                // Repintar componentes
                this.revalidate();
                this.repaint();
        }

        /**
         * Obtiene el Frame padre de forma segura
         */
        private java.awt.Frame obtenerFramePadre(java.awt.Window parentWindow) {
                java.awt.Frame parentFrame = null;

                if (parentWindow instanceof java.awt.Frame) {
                        parentFrame = (java.awt.Frame) parentWindow;
                } else if (parentWindow instanceof java.awt.Dialog) {
                        // Si está en un Dialog, buscar el Frame owner
                        java.awt.Dialog dialog = (java.awt.Dialog) parentWindow;
                        java.awt.Window owner = dialog.getOwner();
                        if (owner instanceof java.awt.Frame) {
                                parentFrame = (java.awt.Frame) owner;
                        }
                }

                if (parentFrame == null) {
                        System.out.println("WARNING  No se encontró Frame padre, usando null");
                }

                return parentFrame;
        }

        /**
         * Actualiza la interfaz después del envío exitoso
         */
        /**
         * Configura la visibilidad y texto de los botones según el estado
         */
        private void configurarBotones(String estado) {
                switch (estado.toLowerCase()) {
                        case "pendiente":
                                btnEnviar.setVisible(false);
                                btnExportar.setVisible(true);
                                break;

                        case "autorizado":
                                btnEnviar.setVisible(true);
                                btnEnviar.setText("Enviar Traspaso");
                                btnEnviar.setEnabled(true);
                                btnEnviar.setToolTipText("Procesar envío del traspaso autorizado");
                                btnExportar.setVisible(true);
                                break;

                        case "en_transito":
                                btnEnviar.setVisible(true);
                                btnEnviar.setText("Recibir Traspaso");
                                btnEnviar.setEnabled(true);
                                btnEnviar.setToolTipText("Marcar traspaso como recibido");
                                btnExportar.setVisible(true);
                                break;

                        case "recibido":
                                btnEnviar.setVisible(false);
                                btnExportar.setVisible(true);
                                btnExportar.setToolTipText("Exportar detalles del traspaso completado");
                                break;

                        case "cancelado":
                                btnEnviar.setVisible(false);
                                btnExportar.setVisible(true);
                                btnExportar.setToolTipText("Exportar detalles del traspaso cancelado");
                                break;

                        default:
                                btnEnviar.setVisible(false);
                                btnExportar.setVisible(true);
                                break;
                }
        }

        private void actualizarInterfazDespuesDeEnvio() {
                // Actualizar timeline y iconos
                configurarEstadoYTimeline("en_transito");

                // Actualizar botones
                configurarBotones("en_transito");

                // Actualizar badge de estado
                configurarBadgeEstado("en_transito");

                // Establecer fecha de envío actual
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                setFechaEnvio(sdf.format(new java.util.Date()));

                // Repintar componentes
                this.revalidate();
                this.repaint();
        }

        /**
         * Recarga los datos del traspaso desde la base de datos
         */
        private void recargarDatosTraspaso() {
                // Este método debe ser llamado desde el formulario padre que tiene acceso a la
                // BD
                // Por ahora solo mostramos un mensaje de debug
                System.out.println("Actualizando Recargando datos del traspaso: " + numeroTraspasoActual);

                // Opcional: Emitir evento o callback para que el formulario padre recargue
                java.awt.EventQueue.invokeLater(() -> {
                        // Aquí puedes agregar lógica para notificar al formulario padre
                        // que debe recargar los datos del traspaso
                        firePropertyChange("traspasoActualizado", null, numeroTraspasoActual);
                });
        }

        /**
         * This method is called from within the constructor to initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is always
         * regenerated by the Form Editor.
         */
        @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated
        // Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jScrollPane1 = new javax.swing.JScrollPane();
                jPanel1 = new javax.swing.JPanel();
                panelTitulo = new javax.swing.JPanel();
                jLabel20 = new javax.swing.JLabel();
                jLabel21 = new javax.swing.JLabel();
                pnlInfoGen = new javax.swing.JPanel();
                pnlInfoGen1 = new javax.swing.JPanel();
                iconOrigen = new javax.swing.JLabel();
                jLabel22 = new javax.swing.JLabel();
                jLabel23 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                txtMotivo = new javax.swing.JTextArea();
                pnlInfoGen2 = new javax.swing.JPanel();
                jLabel24 = new javax.swing.JLabel();
                iconDesti = new javax.swing.JLabel();
                jLabel25 = new javax.swing.JLabel();
                pnlInfoGen3 = new javax.swing.JPanel();
                jLabel26 = new javax.swing.JLabel();
                IconUser = new javax.swing.JLabel();
                jLabel27 = new javax.swing.JLabel();
                pnlInfoGen4 = new javax.swing.JPanel();
                jLabel28 = new javax.swing.JLabel();
                iconCalendar = new javax.swing.JLabel();
                jLabel29 = new javax.swing.JLabel();
                jScrollPane3 = new javax.swing.JScrollPane();
                txtObservaciones = new javax.swing.JTextArea();
                jLabel2 = new javax.swing.JLabel();
                jLabel19 = new javax.swing.JLabel();
                panelSegui = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                imgStatus1 = new javax.swing.JPanel();
                btnStatus1 = new javax.swing.JButton();
                panelSegu1 = new javax.swing.JPanel();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                imgStatus2 = new javax.swing.JPanel();
                btnStatus2 = new javax.swing.JButton();
                panelSegu2 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                jLabel7 = new javax.swing.JLabel();
                panelSegu3 = new javax.swing.JPanel();
                jLabel8 = new javax.swing.JLabel();
                jLabel9 = new javax.swing.JLabel();
                panelSegu4 = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                jLabel11 = new javax.swing.JLabel();
                imgStatus3 = new javax.swing.JPanel();
                btnStatus3 = new javax.swing.JButton();
                imgStatus4 = new javax.swing.JPanel();
                btnStatus4 = new javax.swing.JButton();
                panelProdu = new javax.swing.JPanel();
                jLabel12 = new javax.swing.JLabel();
                jScrollPane4 = new javax.swing.JScrollPane();
                jTable1 = new javax.swing.JTable();
                panelBarr = new javax.swing.JPanel();
                jLabel13 = new javax.swing.JLabel();
                jLabel14 = new javax.swing.JLabel();
                jLabel15 = new javax.swing.JLabel();
                jLabel16 = new javax.swing.JLabel();
                jLabel17 = new javax.swing.JLabel();
                jLabel18 = new javax.swing.JLabel();
                btnEnviar = new javax.swing.JButton();
                btnExportar = new javax.swing.JButton();

                setPreferredSize(new java.awt.Dimension(1200, 800));

                jLabel20.setFont(new java.awt.Font("Arial", 1, 36)); // NOI18N
                jLabel20.setText("Detalle de Traspaso ");

                jLabel21.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
                jLabel21.setText("Número: TR000014 ");

                javax.swing.GroupLayout panelTituloLayout = new javax.swing.GroupLayout(panelTitulo);
                panelTitulo.setLayout(panelTituloLayout);
                panelTituloLayout.setHorizontalGroup(
                                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTituloLayout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(
                                                                                panelTituloLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel21,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                308,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(jLabel20,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                373,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelTituloLayout.setVerticalGroup(
                                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTituloLayout.createSequentialGroup()
                                                                .addGap(20, 20, 20)
                                                                .addComponent(jLabel20,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                26,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel21,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                26,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(16, Short.MAX_VALUE)));

                jLabel22.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel22.setText("Bodega origen");

                jLabel23.setText("Bodega 50años ");

                javax.swing.GroupLayout pnlInfoGen1Layout = new javax.swing.GroupLayout(pnlInfoGen1);
                pnlInfoGen1.setLayout(pnlInfoGen1Layout);
                pnlInfoGen1Layout.setHorizontalGroup(
                                pnlInfoGen1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen1Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addComponent(iconOrigen,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                                pnlInfoGen1Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel22)
                                                                                                .addComponent(jLabel23))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                pnlInfoGen1Layout.setVerticalGroup(
                                pnlInfoGen1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(iconOrigen,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap())
                                                .addGroup(pnlInfoGen1Layout.createSequentialGroup()
                                                                .addGap(14, 14, 14)
                                                                .addComponent(jLabel22)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel23)
                                                                .addContainerGap(8, Short.MAX_VALUE)));

                jLabel1.setText("Motivo");

                txtMotivo.setColumns(20);
                txtMotivo.setRows(5);
                txtMotivo.setPreferredSize(new java.awt.Dimension(232, 90));
                jScrollPane2.setViewportView(txtMotivo);

                jLabel24.setText("Bodega 50años ");

                jLabel25.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel25.setText("Bodega destino");

                javax.swing.GroupLayout pnlInfoGen2Layout = new javax.swing.GroupLayout(pnlInfoGen2);
                pnlInfoGen2.setLayout(pnlInfoGen2Layout);
                pnlInfoGen2Layout.setHorizontalGroup(
                                pnlInfoGen2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen2Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addComponent(iconDesti,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                                pnlInfoGen2Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel25)
                                                                                                .addComponent(jLabel24))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                pnlInfoGen2Layout.setVerticalGroup(
                                pnlInfoGen2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen2Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(iconDesti,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap())
                                                .addGroup(pnlInfoGen2Layout.createSequentialGroup()
                                                                .addGap(14, 14, 14)
                                                                .addComponent(jLabel25)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel24)
                                                                .addContainerGap(8, Short.MAX_VALUE)));

                jLabel26.setText("Bodega 50años ");

                jLabel27.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel27.setText("Solicitado por ");

                javax.swing.GroupLayout pnlInfoGen3Layout = new javax.swing.GroupLayout(pnlInfoGen3);
                pnlInfoGen3.setLayout(pnlInfoGen3Layout);
                pnlInfoGen3Layout.setHorizontalGroup(
                                pnlInfoGen3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen3Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addComponent(IconUser,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                                pnlInfoGen3Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel27)
                                                                                                .addComponent(jLabel26))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                pnlInfoGen3Layout.setVerticalGroup(
                                pnlInfoGen3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen3Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(IconUser,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap())
                                                .addGroup(pnlInfoGen3Layout.createSequentialGroup()
                                                                .addGap(14, 14, 14)
                                                                .addComponent(jLabel27)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel26)
                                                                .addContainerGap(8, Short.MAX_VALUE)));

                jLabel28.setText("Bodega 50años ");

                jLabel29.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel29.setText("Fecha Solicitud");

                javax.swing.GroupLayout pnlInfoGen4Layout = new javax.swing.GroupLayout(pnlInfoGen4);
                pnlInfoGen4.setLayout(pnlInfoGen4Layout);
                pnlInfoGen4Layout.setHorizontalGroup(
                                pnlInfoGen4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen4Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addComponent(iconCalendar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                                pnlInfoGen4Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel29)
                                                                                                .addComponent(jLabel28))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                pnlInfoGen4Layout.setVerticalGroup(
                                pnlInfoGen4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGen4Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(iconCalendar,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap())
                                                .addGroup(pnlInfoGen4Layout.createSequentialGroup()
                                                                .addGap(14, 14, 14)
                                                                .addComponent(jLabel29)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel28)
                                                                .addContainerGap(8, Short.MAX_VALUE)));

                txtObservaciones.setColumns(20);
                txtObservaciones.setRows(5);
                txtObservaciones.setPreferredSize(new java.awt.Dimension(232, 90));
                jScrollPane3.setViewportView(txtObservaciones);

                jLabel2.setText("Observaciones");

                jLabel19.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
                jLabel19.setText("Información General");

                javax.swing.GroupLayout pnlInfoGenLayout = new javax.swing.GroupLayout(pnlInfoGen);
                pnlInfoGen.setLayout(pnlInfoGenLayout);
                pnlInfoGenLayout.setHorizontalGroup(
                                pnlInfoGenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(pnlInfoGenLayout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addGroup(pnlInfoGenLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel19,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                308,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel2)
                                                                                .addComponent(jScrollPane3,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                694,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGroup(pnlInfoGenLayout
                                                                                                .createParallelGroup(
                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                false)
                                                                                                .addComponent(jLabel1)
                                                                                                .addComponent(pnlInfoGen1,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(jScrollPane2,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                694,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(pnlInfoGen2,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(pnlInfoGen3,
                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(pnlInfoGen4,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)))
                                                                .addContainerGap(18, Short.MAX_VALUE)));
                pnlInfoGenLayout.setVerticalGroup(
                                pnlInfoGenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlInfoGenLayout
                                                                .createSequentialGroup()
                                                                .addGap(21, 21, 21)
                                                                .addComponent(jLabel19,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                26,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                28,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(pnlInfoGen1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(pnlInfoGen2,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(pnlInfoGen3,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(pnlInfoGen4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel1)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jScrollPane2,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel2)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jScrollPane3,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(33, 33, 33)));

                jLabel3.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
                jLabel3.setText("Seguimiento");

                imgStatus1.setBackground(new java.awt.Color(204, 204, 204));

                btnStatus1.setBorderPainted(false);
                btnStatus1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                btnStatus1.setFocusable(false);

                javax.swing.GroupLayout imgStatus1Layout = new javax.swing.GroupLayout(imgStatus1);
                imgStatus1.setLayout(imgStatus1Layout);
                imgStatus1Layout.setHorizontalGroup(
                                imgStatus1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btnStatus1, javax.swing.GroupLayout.PREFERRED_SIZE, 46,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE));
                imgStatus1Layout.setVerticalGroup(
                                imgStatus1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btnStatus1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

                jLabel4.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel4.setText("Solicitud creada");

                jLabel5.setText("19:14");

                javax.swing.GroupLayout panelSegu1Layout = new javax.swing.GroupLayout(panelSegu1);
                panelSegu1.setLayout(panelSegu1Layout);
                panelSegu1Layout.setHorizontalGroup(
                                panelSegu1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(
                                                                                panelSegu1Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel4)
                                                                                                .addComponent(jLabel5))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelSegu1Layout.setVerticalGroup(
                                panelSegu1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel4)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(jLabel5)
                                                                .addContainerGap()));

                imgStatus2.setBackground(new java.awt.Color(204, 204, 204));

                btnStatus2.setBorderPainted(false);
                btnStatus2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                btnStatus2.setFocusable(false);
                btnStatus2.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnStatus2ActionPerformed(evt);
                        }
                });

                javax.swing.GroupLayout imgStatus2Layout = new javax.swing.GroupLayout(imgStatus2);
                imgStatus2.setLayout(imgStatus2Layout);
                imgStatus2Layout.setHorizontalGroup(
                                imgStatus2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(imgStatus2Layout.createSequentialGroup()
                                                                .addComponent(btnStatus2,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, 0)));
                imgStatus2Layout.setVerticalGroup(
                                imgStatus2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btnStatus2, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 46,
                                                                Short.MAX_VALUE));

                jLabel6.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel6.setText("Autorizado por ");

                jLabel7.setText("19:14");

                javax.swing.GroupLayout panelSegu2Layout = new javax.swing.GroupLayout(panelSegu2);
                panelSegu2.setLayout(panelSegu2Layout);
                panelSegu2Layout.setHorizontalGroup(
                                panelSegu2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu2Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(
                                                                                panelSegu2Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel6)
                                                                                                .addComponent(jLabel7))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelSegu2Layout.setVerticalGroup(
                                panelSegu2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu2Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel6)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel7)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel8.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel8.setText("Enviado");

                jLabel9.setText("19:14");

                javax.swing.GroupLayout panelSegu3Layout = new javax.swing.GroupLayout(panelSegu3);
                panelSegu3.setLayout(panelSegu3Layout);
                panelSegu3Layout.setHorizontalGroup(
                                panelSegu3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu3Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(
                                                                                panelSegu3Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel8)
                                                                                                .addComponent(jLabel9))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelSegu3Layout.setVerticalGroup(
                                panelSegu3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu3Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel8)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel9,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                14,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel10.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
                jLabel10.setText("Recibido");

                jLabel11.setText("19:14");

                javax.swing.GroupLayout panelSegu4Layout = new javax.swing.GroupLayout(panelSegu4);
                panelSegu4.setLayout(panelSegu4Layout);
                panelSegu4Layout.setHorizontalGroup(
                                panelSegu4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu4Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(
                                                                                panelSegu4Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel10)
                                                                                                .addComponent(jLabel11))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelSegu4Layout.setVerticalGroup(
                                panelSegu4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSegu4Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel10)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel11)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                imgStatus3.setBackground(new java.awt.Color(204, 204, 204));

                btnStatus3.setBorderPainted(false);
                btnStatus3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                btnStatus3.setFocusable(false);
                btnStatus3.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnStatus3ActionPerformed(evt);
                        }
                });

                javax.swing.GroupLayout imgStatus3Layout = new javax.swing.GroupLayout(imgStatus3);
                imgStatus3.setLayout(imgStatus3Layout);
                imgStatus3Layout.setHorizontalGroup(
                                imgStatus3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(imgStatus3Layout.createSequentialGroup()
                                                                .addComponent(btnStatus3,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, 0)));
                imgStatus3Layout.setVerticalGroup(
                                imgStatus3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btnStatus3, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 46,
                                                                Short.MAX_VALUE));

                imgStatus4.setBackground(new java.awt.Color(204, 204, 204));

                btnStatus4.setBorderPainted(false);
                btnStatus4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                btnStatus4.setFocusable(false);
                btnStatus4.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnStatus4ActionPerformed(evt);
                        }
                });

                javax.swing.GroupLayout imgStatus4Layout = new javax.swing.GroupLayout(imgStatus4);
                imgStatus4.setLayout(imgStatus4Layout);
                imgStatus4Layout.setHorizontalGroup(
                                imgStatus4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(imgStatus4Layout.createSequentialGroup()
                                                                .addComponent(btnStatus4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, 0)));
                imgStatus4Layout.setVerticalGroup(
                                imgStatus4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btnStatus4, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 46,
                                                                Short.MAX_VALUE));

                javax.swing.GroupLayout panelSeguiLayout = new javax.swing.GroupLayout(panelSegui);
                panelSegui.setLayout(panelSeguiLayout);
                panelSeguiLayout.setHorizontalGroup(
                                panelSeguiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSeguiLayout
                                                                .createSequentialGroup()
                                                                .addContainerGap(25, Short.MAX_VALUE)
                                                                .addGroup(panelSeguiLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(jLabel3,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                308,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGroup(panelSeguiLayout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(panelSeguiLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(imgStatus2,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(imgStatus1,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(imgStatus3,
                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(imgStatus4,
                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addGroup(panelSeguiLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(panelSegu4,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelSegu3,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelSegu1,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelSegu2,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))))
                                                                .addGap(29, 29, 29)));
                panelSeguiLayout.setVerticalGroup(
                                panelSeguiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSeguiLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelSeguiLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                .addGroup(panelSeguiLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel3,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                26,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(38, 38, 38)
                                                                                                .addGroup(panelSeguiLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(panelSegu1,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(imgStatus1,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addGroup(panelSeguiLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(panelSegu2,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(imgStatus2,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                .addGap(12, 12, 12)
                                                                                                .addComponent(panelSegu3,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addComponent(imgStatus3,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                                panelSeguiLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                .addComponent(panelSegu4,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(imgStatus4,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel12.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
                jLabel12.setText("Productos del traspaso");

                jTable1.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {
                                                { null, null, null, null, null, null, null },
                                                { null, null, null, null, null, null, null },
                                                { null, null, null, null, null, null, null },
                                                { null, null, null, null, null, null, null }
                                },
                                new String[] {
                                                "Producto", "Género", "Solicitada", "Enviada", "Recibida", "Estado",
                                                "Observaciones"
                                }));
                jScrollPane4.setViewportView(jTable1);

                javax.swing.GroupLayout panelProduLayout = new javax.swing.GroupLayout(panelProdu);
                panelProdu.setLayout(panelProduLayout);
                panelProduLayout.setHorizontalGroup(
                                panelProduLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelProduLayout.createSequentialGroup()
                                                                .addGap(30, 30, 30)
                                                                .addGroup(
                                                                                panelProduLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jScrollPane4,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                1071,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(jLabel12,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                308,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap(27, Short.MAX_VALUE)));
                panelProduLayout.setVerticalGroup(
                                panelProduLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelProduLayout.createSequentialGroup()
                                                                .addGap(17, 17, 17)
                                                                .addComponent(jLabel12,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                26,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(30, 30, 30)
                                                                .addComponent(jScrollPane4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                230,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(20, 20, 20)));

                jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel13.setText("Total solicitado");

                jLabel14.setText("12123211232131");

                jLabel15.setText("12123211232131");

                jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel16.setText("Total enviado");

                jLabel17.setText("12123211232131");

                jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel18.setText("Total recibido");

                btnEnviar.setText("Enviar");
                btnEnviar.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnEnviarActionPerformed(evt);
                        }
                });

                btnExportar.setText("Exportar");

                javax.swing.GroupLayout panelBarrLayout = new javax.swing.GroupLayout(panelBarr);
                panelBarr.setLayout(panelBarrLayout);
                panelBarrLayout.setHorizontalGroup(
                                panelBarrLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelBarrLayout.createSequentialGroup()
                                                                .addGap(24, 24, 24)
                                                                .addGroup(panelBarrLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(jLabel13,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel14,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(18, 18, 18)
                                                                .addGroup(panelBarrLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(jLabel16,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel15))
                                                                .addGap(18, 18, 18)
                                                                .addGroup(panelBarrLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(jLabel18,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel17))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(btnEnviar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                149,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(185, 185, 185))
                                                .addGroup(panelBarrLayout.createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                panelBarrLayout.createSequentialGroup()
                                                                                                .addContainerGap(959,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(btnExportar,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                149,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(20, 20, 20))));
                panelBarrLayout.setVerticalGroup(
                                panelBarrLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelBarrLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelBarrLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(btnEnviar,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                panelBarrLayout.createSequentialGroup()
                                                                                                                .addComponent(jLabel18)
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addComponent(jLabel17,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                27,
                                                                                                                                Short.MAX_VALUE))
                                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                panelBarrLayout.createSequentialGroup()
                                                                                                                .addComponent(jLabel16)
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addComponent(jLabel15,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                27,
                                                                                                                                Short.MAX_VALUE))
                                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                panelBarrLayout.createSequentialGroup()
                                                                                                                .addComponent(jLabel13)
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addComponent(jLabel14,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)))
                                                                .addContainerGap())
                                                .addGroup(panelBarrLayout.createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(panelBarrLayout.createSequentialGroup()
                                                                                .addContainerGap()
                                                                                .addComponent(btnExportar,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addContainerGap())));

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(jPanel1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(panelTitulo,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                jPanel1Layout
                                                                                                                .createSequentialGroup()
                                                                                                                .addComponent(pnlInfoGen,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelSegui,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addComponent(panelProdu,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelBarr,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                jPanel1Layout.setVerticalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addGap(21, 21, 21)
                                                                .addComponent(panelTitulo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(30, 30, 30)
                                                                .addGroup(jPanel1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(pnlInfoGen,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelSegui,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelProdu,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelBarr,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(56, Short.MAX_VALUE)));

                jScrollPane1.setViewportView(jPanel1);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jScrollPane1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                1200,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, 0)));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jScrollPane1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                800,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, 0)));
        }// </editor-fold>//GEN-END:initComponents

        private void btnStatus2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnStatus2ActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnStatus2ActionPerformed

        private void btnStatus3ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnStatus3ActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnStatus3ActionPerformed

        private void btnStatus4ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnStatus4ActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnStatus4ActionPerformed

        private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnEnviarActionPerformed
                showLoadingOverlay("Preparando acción...");
                // Verificar que tenemos un número de traspaso
                if (numeroTraspasoActual == null || numeroTraspasoActual.isEmpty()) {
                        javax.swing.JOptionPane.showMessageDialog(this,
                                        "No se puede procesar el envío. Número de traspaso no disponible.",
                                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        hideLoadingOverlay();
                        return;
                }

                // Verificar estado según el botón
                String accion = determinarAccionBoton();

                switch (accion) {
                        case "enviar":
                                try {
                                        procesarEnvioTraspaso();
                                } finally {
                                        hideLoadingOverlay();
                                }
                                break;
                        case "recibir":
                                try {
                                        procesarRecepcionTraspaso();
                                } finally {
                                        hideLoadingOverlay();
                                }
                                break;
                        default:
                                javax.swing.JOptionPane.showMessageDialog(this,
                                                "Acción no disponible para el estado actual: " + estadoActual,
                                                "Información", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                hideLoadingOverlay();
                                break;
                }

        }// GEN-LAST:event_btnEnviarActionPerformed

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel IconUser;
        private javax.swing.JButton btnEnviar;
        private javax.swing.JButton btnExportar;
        private javax.swing.JButton btnStatus1;
        private javax.swing.JButton btnStatus2;
        private javax.swing.JButton btnStatus3;
        private javax.swing.JButton btnStatus4;
        private javax.swing.JLabel iconCalendar;
        private javax.swing.JLabel iconDesti;
        private javax.swing.JLabel iconOrigen;
        private javax.swing.JPanel imgStatus1;
        private javax.swing.JPanel imgStatus2;
        private javax.swing.JPanel imgStatus3;
        private javax.swing.JPanel imgStatus4;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel17;
        private javax.swing.JLabel jLabel18;
        private javax.swing.JLabel jLabel19;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel20;
        private javax.swing.JLabel jLabel21;
        private javax.swing.JLabel jLabel22;
        private javax.swing.JLabel jLabel23;
        private javax.swing.JLabel jLabel24;
        private javax.swing.JLabel jLabel25;
        private javax.swing.JLabel jLabel26;
        private javax.swing.JLabel jLabel27;
        private javax.swing.JLabel jLabel28;
        private javax.swing.JLabel jLabel29;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JScrollPane jScrollPane4;
        private javax.swing.JTable jTable1;
        private javax.swing.JPanel panelBarr;
        private javax.swing.JPanel panelProdu;
        private javax.swing.JPanel panelSegu1;
        private javax.swing.JPanel panelSegu2;
        private javax.swing.JPanel panelSegu3;
        private javax.swing.JPanel panelSegu4;
        private javax.swing.JPanel panelSegui;
        private javax.swing.JPanel panelTitulo;
        private javax.swing.JPanel pnlInfoGen;
        private javax.swing.JPanel pnlInfoGen1;
        private javax.swing.JPanel pnlInfoGen2;
        private javax.swing.JPanel pnlInfoGen3;
        private javax.swing.JPanel pnlInfoGen4;
        private javax.swing.JTextArea txtMotivo;
        private javax.swing.JTextArea txtObservaciones;
        private javax.swing.JPanel loadingOverlay;
        private boolean overlayActive = false;
        // End of variables declaration//GEN-END:variables

        private void showLoadingOverlay(String message) {
                try {
                        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
                        if (win instanceof javax.swing.RootPaneContainer) {
                                javax.swing.JPanel overlay = new javax.swing.JPanel(new java.awt.GridBagLayout());
                                overlay.setOpaque(true);
                                overlay.setBackground(new java.awt.Color(0, 0, 0, 120));
                                overlay.addMouseListener(new java.awt.event.MouseAdapter() {
                                });
                                overlay.addMouseMotionListener(new java.awt.event.MouseAdapter() {
                                });

                                javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.GridBagLayout());
                                content.setOpaque(false);
                                javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
                                bar.setIndeterminate(true);
                                javax.swing.JLabel lbl = new javax.swing.JLabel(
                                                message != null ? message : "Procesando...");
                                lbl.setForeground(java.awt.Color.WHITE);
                                lbl.setFont(new java.awt.Font("Inter", java.awt.Font.BOLD, 14));

                                java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
                                gbc.gridx = 0;
                                gbc.gridy = 0;
                                gbc.insets = new java.awt.Insets(0, 0, 8, 0);
                                content.add(bar, gbc);
                                gbc.gridy = 1;
                                content.add(lbl, gbc);

                                overlay.add(content, new java.awt.GridBagConstraints());

                                javax.swing.RootPaneContainer rpc = (javax.swing.RootPaneContainer) win;
                                rpc.getRootPane().setGlassPane(overlay);
                                overlay.setVisible(true);
                                win.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                                loadingOverlay = overlay;
                                overlayActive = true;
                        }
                } catch (Exception ignore) {
                }
        }

        private void hideLoadingOverlay() {
                try {
                        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
                        if (overlayActive && loadingOverlay != null) {
                                loadingOverlay.setVisible(false);
                                loadingOverlay = null;
                                overlayActive = false;
                        }
                        if (win != null) {
                                win.setCursor(java.awt.Cursor.getDefaultCursor());
                        }
                } catch (Exception ignore) {
                }
        }
}
