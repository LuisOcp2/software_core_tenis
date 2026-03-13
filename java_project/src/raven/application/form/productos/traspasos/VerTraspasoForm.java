package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.utils.ProductImageOptimizer;
import javax.swing.SwingUtilities;
import javax.swing.JWindow;
import javax.swing.ImageIcon;

/**
 * Formulario para visualizar detalles completos de un traspaso
 * Rediseñado con estilo Premium FlatLaf
 */
public class VerTraspasoForm extends javax.swing.JPanel {

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTILOS MODERNOS (Tomados de RotulacionForm + Sketch)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String STYLE_PANEL_MAIN = "arc:20;background:lighten($Panel.background,1%)";
    private static final String STYLE_CARD_HEADER = "arc:20;background:darken($Panel.background,2%);border:1,1,1,1,shade($Panel.background, 5%),,20";
    private static final String STYLE_CARD = "arc:20;background:$Panel.background;border:1,1,1,1,shade($Panel.background, 5%),,20";
    private static final String STYLE_INFO_MINI_CARD = "arc:15;background:lighten($Panel.background,3%);border:1,1,1,1,shade($Panel.background, 8%),,15";

    private static final String STYLE_BTN_BASE = "arc:15;borderWidth:0;focusWidth:0;innerFocusWidth:0;margin:8,16,8,16;font:bold +2";
    private static final String STYLE_BTN_PRIMARY = STYLE_BTN_BASE
            + ";background:#2563EB;foreground:#ffffff;hoverBackground:lighten(#2563EB,10%)";
    private static final String STYLE_BTN_SUCCESS = STYLE_BTN_BASE
            + ";background:#10B981;foreground:#ffffff;hoverBackground:lighten(#10B981,10%)";
    private static final String STYLE_BTN_VIOLET = STYLE_BTN_BASE
            + ";background:#8B5CF6;foreground:#ffffff;hoverBackground:lighten(#8B5CF6,10%)";
    private static final String STYLE_BTN_DANGER = STYLE_BTN_BASE
            + ";background:#DC3545;foreground:#ffffff;hoverBackground:lighten(#DC3545,10%)"; // Error Red

    private final DecimalFormat formatoNumero;

    // Componentes Públicos (Accessibles)
    private JLabel lblNumeroTraspaso;
    private JLabel lblEstadoBadge;

    // Info General Labels
    private JLabel lblBodegaOrigen;
    private JLabel lblBodegaDestino;
    private JLabel lblUsuarioSolicita;
    private JLabel lblFechaSolicitud;
    private JTextArea txtMotivo;
    private JTextArea txtObservaciones;

    // Timeline Labels
    private JLabel lblFechaAutorizacion;
    private JLabel lblUsuarioAutoriza;
    private JLabel lblFechaEnvio;
    private JLabel lblFechaRecepcion;
    private JLabel lblUsuarioRecibe;

    // Timeline Containers (para cambiar estilo activo/inactivo)
    private JPanel timeStep1, timeStep2, timeStep3, timeStep4;
    private FontIcon iconTime1, iconTime2, iconTime3, iconTime4;

    // Tabla
    private JTable tablaProductos;
    private DefaultTableModel tableModel;

    // Totales
    private JLabel lblTotalSolicitado;
    private JLabel lblTotalEnviado;
    private JLabel lblTotalRecibido;
    private JLabel lblMontoTotal;
    private JLabel lblMontoRecibido;

    // Botones (Acciones)
    public JButton btnEnviar;
    public JButton btnRecibir;
    public JButton btnExportar;
    public JButton btnImprimir;
    public JButton btnImprimirCuadre;

    public VerTraspasoForm() {
        System.out.println("LOG: Inicializando VerTraspasoForm (Nueva UI)");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        this.formatoNumero = new DecimalFormat("#,###", symbols);

        initComponentsCustom();
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // 1. Header Section
        add(crearHeader(), BorderLayout.NORTH);

        // 2. Content Grid (Info + Timeline + Tabla)
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setOpaque(false);

        // 2.1 Top Grid: Info General (Left) + Timeline (Right)
        JPanel topGrid = new JPanel(new GridLayout(1, 2, 20, 0)); // 2 Columnas
        topGrid.setOpaque(false);
        topGrid.add(crearPanelInfoGeneral());
        topGrid.add(crearPanelSeguimiento());

        // 2.2 Tabla Productos (Bottom of Content)
        contentPanel.add(topGrid, BorderLayout.NORTH);
        contentPanel.add(crearPanelProductos(), BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // 3. Action Bar (Bottom)
        add(crearActionBar(), BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Icon + Title
        JPanel titlePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);

        FontIcon iconDoc = FontIcon.of(FontAwesomeSolid.FILE_ALT, 32, new Color(37, 99, 235)); // Primary Blue
        JLabel iconLabel = new JLabel(iconDoc);

        JLabel lblTitulo = new JLabel("Detalle de Traspaso");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +12");

        titlePanel.add(iconLabel);
        titlePanel.add(lblTitulo);

        // Subtitle + Badge
        JPanel subPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 0));
        subPanel.setOpaque(false);

        lblNumeroTraspaso = new JLabel("TR-LOADING");
        lblNumeroTraspaso.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +4;foreground:$Label.disabledForeground");

        lblEstadoBadge = new JLabel("PENDIENTE");
        lblEstadoBadge.setOpaque(true);
        lblEstadoBadge.putClientProperty(FlatClientProperties.STYLE,
                "font:bold;arc:12;background:#F59E0B;foreground:#000000"); // Warning default
        lblEstadoBadge.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));

        subPanel.add(lblNumeroTraspaso);
        subPanel.add(lblEstadoBadge);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(subPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel crearPanelInfoGeneral() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("Información General");
        title.setIcon(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 18, new Color(37, 99, 235)));
        title.setIconTextGap(10);
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Grid of Mini Cards (2x2)
        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Short.MAX_VALUE, 180));

        lblBodegaOrigen = new JLabel("-");
        lblBodegaDestino = new JLabel("-");
        lblUsuarioSolicita = new JLabel("-");
        lblFechaSolicitud = new JLabel("-");

        grid.add(crearMiniCard("Bodega Origen", FontAwesomeSolid.BUILDING, new Color(37, 99, 235), lblBodegaOrigen));
        grid.add(crearMiniCard("Bodega Destino", FontAwesomeSolid.BUILDING, new Color(16, 185, 129), lblBodegaDestino));
        grid.add(crearMiniCard("Solicitado Por", FontAwesomeSolid.USER, new Color(245, 158, 11), lblUsuarioSolicita));
        grid.add(crearMiniCard("Fecha Solicitud", FontAwesomeSolid.CALENDAR_DAY, new Color(139, 92, 246),
                lblFechaSolicitud));

        // Text Areas
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtMotivo = crearTextArea("Motivo");
        txtObservaciones = crearTextArea("Observaciones");

        textPanel.add(crearLabeledPanel("Motivo", txtMotivo));
        textPanel.add(crearLabeledPanel("Observaciones", txtObservaciones));

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(grid);
        panel.add(Box.createVerticalStrut(20));
        panel.add(textPanel);

        return panel;
    }

    private JPanel crearMiniCard(String label, FontAwesomeSolid icon, Color color, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_MINI_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblHeader = new JLabel(label);
        lblHeader.setIcon(FontIcon.of(icon, 14, color));
        lblHeader.putClientProperty(FlatClientProperties.STYLE,
                "font:bold -1;foreground:$Label.disabledForeground");
        lblHeader.setText(label.toUpperCase());

        valueLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

        card.add(lblHeader, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JTextArea crearTextArea(String placeholder) {
        JTextArea area = new JTextArea();
        area.setRows(3);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Panel.background,2%);border:0,0,0,0");
        return area;
    }

    private JPanel crearLabeledPanel(String labelText, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(labelText);
        l.setText(labelText.toUpperCase());
        l.putClientProperty(FlatClientProperties.STYLE,
                "font:bold -1;foreground:$Label.disabledForeground");

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(javax.swing.UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        p.add(l, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearPanelSeguimiento() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("Seguimiento");
        title.setIcon(FontIcon.of(FontAwesomeSolid.ROUTE, 18, new Color(16, 185, 129))); // Success Green
        title.setIconTextGap(10);
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Timeline Container
        JPanel timeline = new JPanel();
        timeline.setLayout(new BoxLayout(timeline, BoxLayout.Y_AXIS));
        timeline.setOpaque(false);
        timeline.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Steps
        timeStep1 = crearTimelineStep("Solicitud creada", "-", true, true, 1);
        timeStep2 = crearTimelineStep("Autorizado", "-", false, true, 2);
        timeStep3 = crearTimelineStep("Enviado", "-", false, true, 3);
        timeStep4 = crearTimelineStep("Recibido", "-", false, false, 4); // Last one no connector line

        timeline.add(timeStep1);
        timeline.add(timeStep2);
        timeline.add(timeStep3);
        timeline.add(timeStep4);

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(timeline);

        return panel;
    }

    private JPanel crearTimelineStep(String label, String date, boolean active, boolean showLine, int index) {
        JPanel step = new JPanel(new BorderLayout(15, 0));
        step.setOpaque(false);
        step.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Icon Container
        JPanel iconPanel = new JPanel(null); // Absolute layout for line + icon
        iconPanel.setPreferredSize(new Dimension(40, 50));
        iconPanel.setOpaque(false);

        // Line
        if (showLine) {
            JPanel line = new JPanel();
            line.setBackground(javax.swing.UIManager.getColor("Component.borderColor"));
            line.setBounds(19, 30, 2, 40); // Centered line
            iconPanel.add(line);
        }

        // Icon
        FontIcon icon = FontIcon.of(active ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.CIRCLE, 24,
                active ? new Color(16, 185, 129) : javax.swing.UIManager.getColor("Label.disabledForeground"));

        // Save ref to icon to update later
        if (index == 1)
            iconTime1 = icon;
        else if (index == 2)
            iconTime2 = icon;
        else if (index == 3)
            iconTime3 = icon;
        else if (index == 4)
            iconTime4 = icon;

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setBounds(8, 0, 24, 24);
        iconPanel.add(iconLbl);

        // Content
        JPanel content = new JPanel(new GridLayout(2, 1));
        content.setOpaque(false);

        JLabel lblTitle = new JLabel(label);
        lblTitle.putClientProperty(FlatClientProperties.STYLE,
                active ? "font:bold" : "foreground:$Label.disabledForeground");

        JLabel lblDate = new JLabel(date);
        lblDate.putClientProperty(FlatClientProperties.STYLE, "font:small;foreground:$Label.disabledForeground");

        // Save refs
        if (index == 2) {
            lblUsuarioAutoriza = lblTitle;
            lblFechaAutorizacion = lblDate;
        } else if (index == 3) {
            lblFechaEnvio = lblDate;
        } // Only date for envio usually
        else if (index == 4) {
            lblUsuarioRecibe = lblTitle;
            lblFechaRecepcion = lblDate;
        }

        content.add(lblTitle);
        content.add(lblDate);

        step.add(iconPanel, BorderLayout.WEST);
        step.add(content, BorderLayout.CENTER);

        return step;
    }

    private JPanel crearPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout(10, 5)); // Reduced vertical gap
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Reduced vertical padding

        JLabel title = new JLabel("Productos del Traspaso");
        title.setIcon(FontIcon.of(FontAwesomeSolid.BOXES, 18, new Color(245, 158, 11))); // Orange
        title.setIconTextGap(10);
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");

        // Table
        tableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] { "Producto", "SKU", "Solicitada", "Enviada", "Recibida", "Precio U.", "Subtotal",
                        "Estado", "Observaciones" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProductos = new JTable(tableModel);
        tablaProductos.setRowHeight(50); // Increased row height for images
        tablaProductos.setFocusable(false);
        tablaProductos.setIntercellSpacing(new Dimension(0, 0));
        tablaProductos.setShowVerticalLines(false);
        tablaProductos.getTableHeader().setReorderingAllowed(false);

        // Styles - High Contrast Solid Background
        tablaProductos.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:40;font:bold;background:$Panel.background;separatorColor:$TableHeader.background;border:0,0,1,0,$TableHeader.bottomSeparatorColor");

        tablaProductos.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:50;" + // Increased row height for images
                        "showHorizontalLines:true;" +
                        "intercellSpacing:0,0;" +
                        "background:darken($Panel.background,5%);" + // Darker Background
                        "foreground:#FFFFFF;" + // Explicit White Text
                        "selectionBackground:#2563EB;" +
                        "selectionForeground:#FFFFFF;" +
                        "gridColor:shade($Panel.background, 10%)");

        configurarRenderersTabla();

        JScrollPane scroll = new JScrollPane(tablaProductos);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;trackInsets:3,3,3,3;thumbInsets:3,3,3,3");
        // Ensure viewport matches darker background
        scroll.getViewport().putClientProperty(FlatClientProperties.STYLE, "background:darken($Panel.background,5%)");

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        attachImageHoverListener(tablaProductos);

        return panel;
    }

    private void configurarRenderersTabla() {
        // Center Renderer with Explicit White/Light Text - High Contrast
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setForeground(new Color(255, 255, 255)); // Explicit White
                }
                return c;
            }
        };
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Padding

        // Product Renderer (Image + Text) - High Contrast
        DefaultTableCellRenderer productRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("");
                setIcon(null);
                setToolTipText(null);

                if (value instanceof TraspasoProductWrapper) {
                    TraspasoProductWrapper p = (TraspasoProductWrapper) value;
                    if (p.cachedIcon != null) {
                        setIcon(p.cachedIcon);
                    }
                    // Use description which is "Color - Size" (passed in wrapper)
                    String descHtml = "";
                    if (p.descripcion != null && !p.descripcion.isEmpty()) {
                        descHtml = "<br><span style='font-size:90%;color:#cbd5e1'>" + p.descripcion + "</span>"; // Lighter
                                                                                                                 // gray
                                                                                                                 // for
                                                                                                                 // subtext
                    }
                    setText("<html><div style='padding:5px'><b>" + p.nombre + "</b>" + descHtml + "</div></html>");
                } else if (value != null) {
                    setText(value.toString());
                }

                if (!isSelected) {
                    setForeground(new Color(255, 255, 255)); // Explicit White
                    setBackground(table.getBackground());
                }
                return this;
            }
        };
        productRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        // Text Renderer (Left) - High Contrast
        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setForeground(new Color(255, 255, 255)); // Explicit White
                }
                return c;
            }
        };
        textRenderer.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        tablaProductos.getColumnModel().getColumn(0).setCellRenderer(productRenderer); // Prod (Custom)
        tablaProductos.getColumnModel().getColumn(1).setCellRenderer(textRenderer); // SKU
        tablaProductos.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Sol
        tablaProductos.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Env
        tablaProductos.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Rec

        // Price and Subtotal (Right Aligned)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setForeground(new Color(255, 255, 255));

        tablaProductos.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Precio U.
        tablaProductos.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Subtotal

        tablaProductos.getColumnModel().getColumn(8).setCellRenderer(textRenderer); // Obs

        // Status Renderer - Vivid and Visible
        tablaProductos.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                l.setHorizontalAlignment(JLabel.CENTER);
                if (value != null) {
                    String s = value.toString().toLowerCase();
                    String color = "#64748B"; // Slate
                    String textColor = "#ffffff";

                    if (s.contains("pendiente")) {
                        color = "#F59E0B"; // Vivid Amber
                    } else if (s.contains("enviado")) {
                        color = "#0EA5E9"; // Vivid Sky
                    } else if (s.contains("recibido")) {
                        color = "#10B981"; // Vivid Emerald
                    } else if (s.contains("faltante")) {
                        color = "#EF4444"; // Vivid Red
                    }

                    l.setText("<html><div style='padding:4px 12px;border-radius:12px;background-color:" + color
                            + ";color:" + textColor + ";font-weight:bold;font-size:10px;text-transform:uppercase'>"
                            + value + "</div></html>");
                }
                l.setForeground(Color.WHITE);
                return l;
            }
        });

        // Widths
        tablaProductos.getColumnModel().getColumn(0).setPreferredWidth(250); // Prod
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(100); // SKU
        tablaProductos.getColumnModel().getColumn(5).setPreferredWidth(80); // Precio
        tablaProductos.getColumnModel().getColumn(6).setPreferredWidth(90); // Subtotal
        tablaProductos.getColumnModel().getColumn(8).setPreferredWidth(200); // Obs
    }

    private JPanel crearActionBar() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        // Totals (Left)
        JPanel totals = new JPanel(new GridLayout(1, 5, 15, 0)); // Increased columns
        totals.setOpaque(false);

        lblTotalSolicitado = new JLabel("0");
        lblTotalEnviado = new JLabel("0");
        lblTotalRecibido = new JLabel("0");
        lblMontoTotal = new JLabel("$ 0");
        lblMontoRecibido = new JLabel("$ 0");

        totals.add(crearTotalItem("Solicitado", lblTotalSolicitado, "#2563EB"));
        totals.add(crearTotalItem("Enviado", lblTotalEnviado, "#F59E0B"));
        totals.add(crearTotalItem("Recibido", lblTotalRecibido, "#10B981"));
        totals.add(crearTotalItem("Monto Env.", lblMontoTotal, "#6366F1")); // Indigo
        totals.add(crearTotalItem("Monto Rec.", lblMontoRecibido, "#8B5CF6")); // Violet

        // Buttons (Right)
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        btnImprimir = new JButton("Imprimir", FontIcon.of(FontAwesomeSolid.PRINT, 16, Color.WHITE));
        btnImprimir.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_PRIMARY);

        btnImprimirCuadre = new JButton("Cuadre", FontIcon.of(FontAwesomeSolid.BALANCE_SCALE, 16, Color.WHITE));
        btnImprimirCuadre.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_VIOLET);

        btnExportar = new JButton("Exportar", FontIcon.of(FontAwesomeSolid.FILE_EXPORT, 16, Color.WHITE));
        btnExportar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_VIOLET);

        btnEnviar = new JButton("Enviar Traspaso", FontIcon.of(FontAwesomeSolid.PAPER_PLANE, 16, Color.WHITE));
        btnEnviar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_PRIMARY);
        btnEnviar.setVisible(false);

        btnRecibir = new JButton("Recibir Traspaso", FontIcon.of(FontAwesomeSolid.BOX_OPEN, 16, Color.WHITE));
        btnRecibir.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_SUCCESS);
        btnRecibir.setVisible(false);

        buttons.add(btnImprimir);
        buttons.add(btnImprimirCuadre);
        buttons.add(btnExportar);
        buttons.add(btnEnviar);
        buttons.add(btnRecibir);

        panel.add(totals, BorderLayout.WEST);
        panel.add(buttons, BorderLayout.EAST);

        return panel;
    }

    private JPanel crearTotalItem(String label, JLabel valueLbl, String colorHex) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setText(label.toUpperCase());
        l.putClientProperty(FlatClientProperties.STYLE,
                "font:bold -1;foreground:$Label.disabledForeground");
        valueLbl.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;foreground:" + colorHex);
        p.add(l);
        p.add(valueLbl);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA SETTERS (METHODS TO POPULATE UI)
    // ═══════════════════════════════════════════════════════════════════════════

    public void setTraspasoInfo(String numero, String fecha, String estado, String origen, String destino,
            String solicita, String motivo, String obs) {
        lblNumeroTraspaso.setText(numero != null ? numero : "-");
        lblFechaSolicitud.setText(extraerHora(fecha));
        lblBodegaOrigen.setText(origen);
        lblBodegaDestino.setText(destino);
        lblUsuarioSolicita.setText(solicita);
        txtMotivo.setText(motivo != null ? motivo : "");
        txtObservaciones.setText(obs != null ? obs : "");

        actualizarEstadoBadge(estado);
        actualizarTimeline(estado, fecha);

        // Reset buttons by status
        btnEnviar.setVisible("autorizado".equalsIgnoreCase(estado));
        btnRecibir.setVisible("en_transito".equalsIgnoreCase(estado));
    }

    // Compatibilidad con los nombres viejos para Timeline
    public void setFechaAutorizacion(String fecha) {
        System.out.println("LOG: setFechaAutorizacion " + fecha);
        setStepInfo(2, fecha, null);
    }

    public void setFechaAutorizacion(String fecha, String usuario) {
        System.out.println("LOG: setFechaAutorizacion (2 args) " + fecha + " " + usuario);
        setStepInfo(2, fecha, usuario != null ? "Autorizado por " + usuario : null);
    }

    public void setUsuarioAutoriza(String usuario) {
        setStepInfo(2, null, "Autorizado por " + usuario);
    }

    public void setFechaEnvio(String fecha) {
        setStepInfo(3, fecha, null);
    }

    public void setFechaRecepcion(String fecha) {
        setStepInfo(4, fecha, null);
    }

    public void setFechaRecepcion(String fecha, String usuario) {
        setStepInfo(4, fecha, usuario != null ? "Recibido por " + usuario : null);
    }

    public void setUsuarioRecibe(String usuario) {
        setStepInfo(4, null, "Recibido por " + usuario);
    }

    private void setStepInfo(int step, String date, String title) {
        // Helper to update labels dynamically
        if (step == 2) {
            if (date != null)
                lblFechaAutorizacion.setText(extraerHora(date));
            if (title != null)
                lblUsuarioAutoriza.setText(title);
        } else if (step == 3) {
            if (date != null)
                lblFechaEnvio.setText(extraerHora(date));
        } else if (step == 4) {
            if (date != null)
                lblFechaRecepcion.setText(extraerHora(date));
            if (title != null)
                lblUsuarioRecibe.setText(title);
        }
    }

    public void setProductos(List<Map<String, Object>> productos) {
        System.out.println(
                "DEBUG: setProductos called with " + (productos != null ? productos.size() : "null") + " items");
        tableModel.setRowCount(0);
        int tSol = 0, tEnv = 0, tRec = 0;
        List<TraspasoProductWrapper> wrappers = new java.util.ArrayList<>();

        double tMonto = 0;
        double tMontoRec = 0;

        for (Map<String, Object> p : productos) {
            int sol = getInt(p.get("cantidad_solicitada"));
            int env = getInt(p.get("cantidad_enviada"));
            int rec = getInt(p.get("cantidad_recibida"));

            double precio = p.get("precio_unitario") != null ? ((Number) p.get("precio_unitario")).doubleValue() : 0;
            double subtotal = precio * env;
            double subtotalRec = precio * rec;

            tSol += sol;
            tEnv += env;
            tRec += rec;
            tMonto += subtotal;
            tMontoRec += subtotalRec;

            tableModel.addRow(new Object[] {
                    new TraspasoProductWrapper(getInt(p.get("id_producto")),
                            p.get("producto_nombre").toString(),
                            p.get("variante_texto") != null ? p.get("variante_texto").toString() : ""),
                    p.get("sku"),
                    sol, env, rec,
                    String.format("$ %,.0f", precio),
                    String.format("$ %,.0f", subtotal),
                    p.get("estado_detalle"),
                    p.get("observaciones")
            });

            // Collect mapping for background loading
            wrappers.add((TraspasoProductWrapper) tableModel.getValueAt(tableModel.getRowCount() - 1, 0));
        }

        lblTotalSolicitado.setText(formatoNumero.format(tSol));
        lblTotalEnviado.setText(formatoNumero.format(tEnv));
        lblTotalRecibido.setText(formatoNumero.format(tRec));
        lblMontoTotal.setText(String.format("$ %,.0f", tMonto));
        lblMontoRecibido.setText(String.format("$ %,.0f", tMontoRec));

        // Start background loading
        loadImagesInBackground(wrappers);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void actualizarEstadoBadge(String estado) {
        String s = (estado != null ? estado : "").toUpperCase();
        lblEstadoBadge.setText(s);

        String color = "#64748B"; // Slate
        if (s.contains("PENDIENTE"))
            color = "#F59E0B"; // Vivid Amber
        else if (s.contains("AUTORIZADO"))
            color = "#06B6D4"; // Cyan
        else if (s.contains("TRANSITO"))
            color = "#3B82F6"; // Blue
        else if (s.contains("RECIBIDO"))
            color = "#10B981"; // Emerald
        else if (s.contains("CANCELADO"))
            color = "#EF4444"; // Vivid Red

        lblEstadoBadge.putClientProperty(FlatClientProperties.STYLE,
                "font:bold;arc:12;foreground:#ffffff;background:" + color);
    }

    private void actualizarTimeline(String estado, String fechaCreacion) {
        String s = (estado != null ? estado : "").toLowerCase();

        // Reset all
        setStepActive(1, true); // Always created
        setStepActive(2, false);
        setStepActive(3, false);
        setStepActive(4, false);

        if (s.equals("autorizado") || s.equals("en_transito") || s.equals("recibido"))
            setStepActive(2, true);
        if (s.equals("en_transito") || s.equals("recibido"))
            setStepActive(3, true);
        if (s.equals("recibido"))
            setStepActive(4, true);
    }

    private void setStepActive(int step, boolean active) {
        Color c = active ? new Color(16, 185, 129) : javax.swing.UIManager.getColor("Label.disabledForeground");
        if (step == 1)
            iconTime1.setIconColor(c);
        if (step == 2)
            iconTime2.setIconColor(c);
        if (step == 3)
            iconTime3.setIconColor(c);
        if (step == 4)
            iconTime4.setIconColor(c);

        // Update check icon
        if (active) {
            if (step == 1)
                iconTime1.setIkon(FontAwesomeSolid.CHECK_CIRCLE);
            if (step == 2)
                iconTime2.setIkon(FontAwesomeSolid.CHECK_CIRCLE);
            if (step == 3)
                iconTime3.setIkon(FontAwesomeSolid.CHECK_CIRCLE);
            if (step == 4)
                iconTime4.setIkon(FontAwesomeSolid.CHECK_CIRCLE);
        }
    }

    private String extraerHora(String val) {
        if (val == null)
            return "-";
        try {
            if (val.contains("."))
                val = val.substring(0, val.indexOf("."));
            return val;
        } catch (Exception e) {
            return val;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE CLASSES & HELPERS FOR IMAGE HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    private static class TraspasoProductWrapper {
        int id;
        String nombre;
        String descripcion;
        ImageIcon cachedIcon;

        public TraspasoProductWrapper(int id, String nombre, String descripcion) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
        }

        @Override
        public String toString() {
            return nombre; // Fallback
        }
    }

    private void loadImagesInBackground(List<TraspasoProductWrapper> items) {
        new Thread(() -> {
            for (int i = 0; i < items.size(); i++) {
                TraspasoProductWrapper item = items.get(i);
                if (item.cachedIcon == null) {
                    // Load small icon (40px high)
                    item.cachedIcon = ProductImageOptimizer.loadFirstImage(item.id);
                    if (item.cachedIcon != null) {
                        SwingUtilities.invokeLater(() -> {
                            // Repaint table to show image
                            if (tablaProductos != null && tablaProductos.isDisplayable()) {
                                tablaProductos.repaint();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void attachImageHoverListener(JTable table) {
        final javax.swing.JWindow preview = new javax.swing.JWindow();
        final javax.swing.JLabel label = new javax.swing.JLabel();
        label.setOpaque(true);
        label.setBackground(new Color(250, 250, 250));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        preview.getContentPane().add(label);
        preview.pack();

        final int imageColumn = 0; // "Producto" column
        final javax.swing.Timer showTimer = new javax.swing.Timer(150, null);
        showTimer.setRepeats(false);
        final int[] hoverProductId = new int[] { 0 };
        preview.setAlwaysOnTop(true); // Ensure it is in front

        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row < 0 || col != imageColumn) {
                    showTimer.stop();
                    preview.setVisible(false);
                    return;
                }

                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object val = table.getModel().getValueAt(modelRow, imageColumn);

                    if (!(val instanceof TraspasoProductWrapper)) {
                        showTimer.stop();
                        preview.setVisible(false);
                        return;
                    }

                    TraspasoProductWrapper p = (TraspasoProductWrapper) val;
                    if (p.cachedIcon == null) {
                        // No image to zoom
                        showTimer.stop();
                        preview.setVisible(false);
                        return;
                    }

                    // Check if mouse is over the icon part (approx first 50px)
                    java.awt.Rectangle cellRect = table.getCellRect(row, imageColumn, true);
                    int relX = e.getPoint().x - cellRect.x;
                    if (relX > 60) { // Icon is likely within first 50-60px
                        showTimer.stop();
                        preview.setVisible(false);
                        return;
                    }

                    if (hoverProductId[0] != p.id) {
                        hoverProductId[0] = p.id;
                        // Clean listeners
                        for (java.awt.event.ActionListener l : showTimer.getActionListeners())
                            showTimer.removeActionListener(l);

                        showTimer.addActionListener(ev -> {
                            new Thread(() -> {
                                // Load larger image (500px)
                                ImageIcon loaded = ProductImageOptimizer.loadLargeImage(p.id, 500);
                                if (loaded != null) {
                                    SwingUtilities.invokeLater(() -> {
                                        label.setIcon(loaded);
                                        preview.pack();
                                        preview.setLocationRelativeTo(null); // Center on screen
                                        preview.setVisible(true);
                                    });
                                }
                            }).start();
                        });
                        showTimer.restart();
                    } else {
                        // Same product, if visible just keep it there (centered)
                        if (!preview.isVisible()) {
                            preview.setLocationRelativeTo(null);
                            preview.setVisible(true);
                        }
                    }

                } catch (Exception ignore) {
                    preview.setVisible(false);
                }
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                showTimer.stop();
                preview.setVisible(false);
            }
        });
    }

    // Simplified positioning not needed if centered, but keeping method sig if
    // referenced elsewhere
    private void positionPreview(java.awt.event.MouseEvent e, JWindow preview) {
        preview.setLocationRelativeTo(null);
    }

    private int getInt(Object o) {
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}