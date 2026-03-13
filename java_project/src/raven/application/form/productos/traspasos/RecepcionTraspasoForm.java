/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.utils.ProductImageOptimizer;

/**
 * Formulario moderno para la Recepción de Traspasos
 * Diseño premium consistente con VerTraspasoForm
 */
public class RecepcionTraspasoForm extends javax.swing.JPanel {

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTILOS MODERNOS
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
    private static final String STYLE_BTN_SECONDARY = STYLE_BTN_BASE
            + ";background:#64748B;foreground:#ffffff;hoverBackground:lighten(#64748B,10%)";

    private final DecimalFormat formatoNumero;

    // UI Components
    private JLabel lblNumeroTraspaso;
    private JLabel lblEstadoBadge;

    // Info General
    private JLabel lblBodegaOrigen;
    private JLabel lblBodegaDestino;
    private JLabel lblFechaEnvio;
    private JTextArea txtObservaciones;

    // Timeline
    private JPanel timeStep1, timeStep2, timeStep3, timeStep4;
    private FontIcon iconTime1, iconTime2, iconTime3, iconTime4;

    // Tabla & Selección
    private JTable tablaProductos;
    private DefaultTableModel tableModel;
    private JCheckBox chkSelectAll;
    private JLabel lblSelectionSummary;

    // Actions
    public JButton btnConfirmar;
    public JButton btnCancelar;

    // Data State
    private List<Map<String, Object>> productosOriginales = new ArrayList<>();

    public RecepcionTraspasoForm() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        this.formatoNumero = new DecimalFormat("#,###", symbols);

        initComponentsCustom();
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // 1. Header
        add(crearHeader(), BorderLayout.NORTH);

        // 2. Content (Grid + Table)
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setOpaque(false);

        JPanel topGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        topGrid.setOpaque(false);
        topGrid.add(crearPanelInfoGeneral());
        topGrid.add(crearPanelSeguimiento());

        contentPanel.add(topGrid, BorderLayout.NORTH);
        contentPanel.add(crearPanelProductos(), BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // 3. Actions
        add(crearActionBar(), BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel titlePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);

        FontIcon iconDoc = FontIcon.of(FontAwesomeSolid.BOX_OPEN, 32, new Color(16, 185, 129)); // Success Color
        JLabel iconLabel = new JLabel(iconDoc);

        JLabel lblTitulo = new JLabel("Recepción de Traspaso");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +12");

        titlePanel.add(iconLabel);
        titlePanel.add(lblTitulo);

        JPanel subPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 0));
        subPanel.setOpaque(false);

        lblNumeroTraspaso = new JLabel("TR-LOADING");
        lblNumeroTraspaso.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +4;foreground:$Label.disabledForeground");

        lblEstadoBadge = new JLabel("EN TRÁNSITO");
        lblEstadoBadge.setOpaque(true);
        lblEstadoBadge.putClientProperty(FlatClientProperties.STYLE,
                "font:bold;arc:12;background:#3B82F6;foreground:#ffffff"); // Blue for Transit
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

        JLabel title = new JLabel("Datos del Envío");
        title.setIcon(FontIcon.of(FontAwesomeSolid.TRUCK, 18, new Color(37, 99, 235)));
        title.setIconTextGap(10);
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(3, 1, 10, 10)); // 3 rows
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Short.MAX_VALUE, 240));

        lblBodegaOrigen = new JLabel("-");
        lblBodegaDestino = new JLabel("-");
        lblFechaEnvio = new JLabel("-");

        grid.add(crearMiniCard("Origen", FontAwesomeSolid.WAREHOUSE, new Color(37, 99, 235), lblBodegaOrigen));
        grid.add(crearMiniCard("Destino (Aquí)", FontAwesomeSolid.MAP_MARKER_ALT, new Color(16, 185, 129),
                lblBodegaDestino));
        grid.add(crearMiniCard("Fecha Envio", FontAwesomeSolid.CLOCK, new Color(245, 158, 11), lblFechaEnvio));

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(grid);

        return panel;
    }

    private JPanel crearMiniCard(String label, FontAwesomeSolid icon, Color color, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.putClientProperty(FlatClientProperties.STYLE, STYLE_INFO_MINI_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblHeader = new JLabel(label);
        lblHeader.setIcon(FontIcon.of(icon, 14, color));
        lblHeader.putClientProperty(FlatClientProperties.STYLE, "font:bold -1;foreground:$Label.disabledForeground");
        lblHeader.setText(label.toUpperCase());

        valueLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

        card.add(lblHeader, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel crearPanelSeguimiento() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Progreso");
        title.setIcon(FontIcon.of(FontAwesomeSolid.TASKS, 18, new Color(16, 185, 129)));
        title.setIconTextGap(10);
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel timeline = new JPanel();
        timeline.setLayout(new BoxLayout(timeline, BoxLayout.Y_AXIS));
        timeline.setOpaque(false);
        timeline.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Fixed steps for reception context
        timeStep1 = crearTimelineStep("Solicitado", true, true, 1);
        timeStep2 = crearTimelineStep("Autorizado", true, true, 2);
        timeStep3 = crearTimelineStep("Enviado", true, true, 3);
        timeStep4 = crearTimelineStep("Recibiendo", false, false, 4); // Current step

        timeline.add(timeStep1);
        timeline.add(timeStep2);
        timeline.add(timeStep3);
        timeline.add(timeStep4);

        // Mark step 4 active (yellow/orange usually for in progress)
        iconTime4.setIconColor(new Color(245, 158, 11));
        iconTime4.setIkon(FontAwesomeSolid.CIRCLE);

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(timeline);

        return panel;
    }

    private JPanel crearTimelineStep(String label, boolean active, boolean showLine, int index) {
        JPanel step = new JPanel(new BorderLayout(15, 0));
        step.setOpaque(false);
        step.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JPanel iconPanel = new JPanel(null);
        iconPanel.setPreferredSize(new Dimension(40, 40));
        iconPanel.setOpaque(false);

        if (showLine) {
            JPanel line = new JPanel();
            line.setBackground(javax.swing.UIManager.getColor("Component.borderColor"));
            line.setBounds(19, 28, 2, 35);
            iconPanel.add(line);
        }

        FontIcon icon = FontIcon.of(active ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.CIRCLE, 24,
                active ? new Color(16, 185, 129) : javax.swing.UIManager.getColor("Label.disabledForeground"));

        if (index == 1)
            iconTime1 = icon;
        if (index == 2)
            iconTime2 = icon;
        if (index == 3)
            iconTime3 = icon;
        if (index == 4)
            iconTime4 = icon;

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setBounds(8, 0, 24, 24);
        iconPanel.add(iconLbl);

        JLabel lblTitle = new JLabel(label);
        lblTitle.putClientProperty(FlatClientProperties.STYLE,
                active ? "font:bold" : "foreground:$Label.disabledForeground");

        step.add(iconPanel, BorderLayout.WEST);
        step.add(lblTitle, BorderLayout.CENTER);
        return step;
    }

    private JPanel crearPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Header for Table
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Verificación de Productos");
        title.setIcon(FontIcon.of(FontAwesomeSolid.CHECK_DOUBLE, 18, new Color(245, 158, 11)));
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");

        JPanel selectionPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        selectionPanel.setOpaque(false);

        chkSelectAll = new JCheckBox("Recibir Todo");
        chkSelectAll.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        chkSelectAll.addActionListener(e -> toggleSelectAll());

        lblSelectionSummary = new JLabel("0/0 seleccionados");
        lblSelectionSummary.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");

        selectionPanel.add(chkSelectAll);
        selectionPanel.add(Box.createHorizontalStrut(10));
        selectionPanel.add(lblSelectionSummary);

        header.add(title, BorderLayout.WEST);
        header.add(selectionPanel, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] { "Recibir", "Producto", "SKU", "Enviado", "Confirmado", "Tipo", "Precio U.", "Subtotal",
                        "Observaciones" }) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Checkbox only
            }
        };

        tablaProductos = new JTable(tableModel);
        tablaProductos.setRowHeight(50);
        tablaProductos.getTableHeader().setReorderingAllowed(false);

        // Styles similar to VerTraspasoForm
        tablaProductos.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:40;font:bold;background:$Panel.background;separatorColor:$TableHeader.background;border:0,0,1,0,$TableHeader.bottomSeparatorColor");

        tablaProductos.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:50;showHorizontalLines:true;intercellSpacing:0,0;background:darken($Panel.background,5%);foreground:#FFFFFF;selectionBackground:#2563EB;selectionForeground:#FFFFFF;gridColor:shade($Panel.background, 10%)");

        configurarRenderersTabla();

        // Listeners for selection update
        tablaProductos.getModel().addTableModelListener(e -> actualizarResumen());

        JScrollPane scroll = new JScrollPane(tablaProductos);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().putClientProperty(FlatClientProperties.STYLE, "background:darken($Panel.background,5%)");

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        attachImageHoverListener(tablaProductos);

        return panel;
    }

    private void configurarRenderersTabla() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setForeground(Color.WHITE);

        // Product Renderer (Image + Text)
        DefaultTableCellRenderer productRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("");
                setIcon(null);

                if (value instanceof ProductWrapper) {
                    ProductWrapper p = (ProductWrapper) value;
                    if (p.cachedIcon != null)
                        setIcon(p.cachedIcon);
                    setText("<html><div style='padding:5px'><b>" + p.nombre + "</b><br><span style='color:#cbd5e1'>"
                            + p.variante + "</span></div></html>");
                }

                if (!isSelected) {
                    setForeground(Color.WHITE);
                    setBackground(table.getBackground());
                }
                return this;
            }
        };

        tablaProductos.getColumnModel().getColumn(1).setCellRenderer(productRenderer); // Product
        tablaProductos.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Enviado
        tablaProductos.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Confirmado

        // Price renderers
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat fmt = new DecimalFormat("$#,##0.00");

            @Override
            public void setValue(Object value) {
                setText((value == null) ? "" : fmt.format(value));
                setHorizontalAlignment(SwingConstants.RIGHT);
                setForeground(Color.WHITE);
            }
        };
        tablaProductos.getColumnModel().getColumn(6).setCellRenderer(moneyRenderer);
        tablaProductos.getColumnModel().getColumn(7).setCellRenderer(moneyRenderer);

        // Widths
        tablaProductos.getColumnModel().getColumn(0).setPreferredWidth(60); // Check
        tablaProductos.getColumnModel().getColumn(0).setMaxWidth(80);
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(250); // Product - reduced slightly
        tablaProductos.getColumnModel().getColumn(6).setPreferredWidth(80);
        tablaProductos.getColumnModel().getColumn(7).setPreferredWidth(80);
    }

    private JPanel crearActionBar() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        // Observaciones Input
        JPanel obsPanel = new JPanel(new BorderLayout(0, 5));
        obsPanel.setOpaque(false);
        JLabel l = new JLabel("Observaciones de Recepción:");
        l.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        txtObservaciones = new JTextArea(2, 30);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Panel.background,2%)");

        obsPanel.add(l, BorderLayout.NORTH);
        obsPanel.add(new JScrollPane(txtObservaciones), BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        btnCancelar = new JButton("Cancelar", FontIcon.of(FontAwesomeSolid.TIMES, 16, Color.WHITE));
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_SECONDARY);

        btnConfirmar = new JButton("Confirmar Recepción", FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 16, Color.WHITE));
        btnConfirmar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_SUCCESS);
        btnConfirmar.setEnabled(false);

        buttons.add(btnCancelar);
        buttons.add(btnConfirmar);

        panel.add(obsPanel, BorderLayout.CENTER); // Center expands
        panel.add(buttons, BorderLayout.EAST);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGICA
    // ═══════════════════════════════════════════════════════════════════════════

    public void setTraspasoInfo(String numero, String origen, String destino, String fechaEnvio) {
        lblNumeroTraspaso.setText(numero);
        lblBodegaOrigen.setText(origen);
        lblBodegaDestino.setText(destino);
        lblFechaEnvio.setText(fechaEnvio);
    }

    public void setProductos(List<Map<String, Object>> productos) {
        this.productosOriginales = productos;
        tableModel.setRowCount(0);
        List<ProductWrapper> wrappers = new ArrayList<>();

        for (Map<String, Object> p : productos) {
            int id = getInt(p.get("id_producto"));
            String nombre = getString(p.get("producto_nombre"));
            String sku = getString(p.get("sku"));
            int enviado = getInt(p.get("cantidad_enviada"));
            // For now, confirm same amount as sent if checked

            // Build wrapper desc
            String variante = "";
            if (p.get("color_nombre") != null)
                variante += p.get("color_nombre") + " ";
            if (p.get("talla_numero") != null)
                variante += "Talla " + p.get("talla_numero");

            ProductWrapper wrapper = new ProductWrapper(id, nombre, variante);

            // Get price from map
            Object priceObj = p.get("precio_unitario");
            java.math.BigDecimal precio = java.math.BigDecimal.ZERO;
            if (priceObj instanceof java.math.BigDecimal) {
                precio = (java.math.BigDecimal) priceObj;
            } else if (priceObj instanceof Number) {
                precio = new java.math.BigDecimal(((Number) priceObj).doubleValue());
            }

            java.math.BigDecimal subtotal = precio.multiply(new java.math.BigDecimal(enviado));

            tableModel.addRow(new Object[] {
                    false, // Checkbox default unchecked
                    wrapper,
                    sku,
                    enviado,
                    enviado, // Default implementation assumes full receipt if checked
                    p.get("tipo_detalle") != null ? p.get("tipo_detalle") : "par",
                    precio,
                    subtotal,
                    ""
            });
            wrappers.add(wrapper);
        }

        loadImagesInBackground(wrappers);
        actualizarResumen();
    }

    public List<Map<String, Object>> getProductosConfirmados() {
        List<Map<String, Object>> confirmados = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean checked = (Boolean) tableModel.getValueAt(i, 0);
            if (checked) {
                // Return original map but with confirmed amount
                Map<String, Object> orig = productosOriginales.get(i);
                int cantidad = (Integer) tableModel.getValueAt(i, 4); // Column 4 is confirmed amount
                orig.put("cantidad_recibida_real", cantidad);
                confirmados.add(orig);
            }
        }
        return confirmados;
    }

    public String getObservaciones() {
        return txtObservaciones.getText();
    }

    private void toggleSelectAll() {
        boolean selected = chkSelectAll.isSelected();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(selected, i, 0);
        }
    }

    private void actualizarResumen() {
        int total = tableModel.getRowCount();
        int selected = 0;
        for (int i = 0; i < total; i++) {
            if ((Boolean) tableModel.getValueAt(i, 0))
                selected++;
        }
        lblSelectionSummary.setText(selected + "/" + total + " productos");
        btnConfirmar.setEnabled(selected > 0);
        btnConfirmar.setText(selected > 0 ? "Confirmar (" + selected + ")" : "Seleccione Productos");
    }

    // Helpers
    private int getInt(Object o) {
        if (o instanceof Number)
            return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private String getString(Object o) {
        return o != null ? o.toString() : "";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMAGE HANDLING (COPIED FROM VERTRASPASOFORM)
    // ═══════════════════════════════════════════════════════════════════════════
    private static class ProductWrapper {
        int id;
        String nombre;
        String variante;
        ImageIcon cachedIcon;

        public ProductWrapper(int id, String n, String v) {
            this.id = id;
            this.nombre = n;
            this.variante = v;
        }

        public String toString() {
            return nombre;
        }
    }

    private void loadImagesInBackground(List<ProductWrapper> products) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (ProductWrapper p : products) {
                    try {
                        ImageIcon icon = ProductImageOptimizer.loadFirstImage(p.id);
                        if (icon != null) {
                            p.cachedIcon = icon;
                            SwingUtilities.invokeLater(() -> tablaProductos.repaint());
                        }
                    } catch (Exception e) {
                    }
                }
                return null;
            }
        }.execute();
    }

    private JWindow hoverWindow;
    private JLabel hoverLabel;

    private void attachImageHoverListener(JTable table) {
        hoverWindow = new JWindow(SwingUtilities.getWindowAncestor(this));
        hoverLabel = new JLabel();
        hoverLabel.setOpaque(true);
        hoverLabel.setBackground(Color.WHITE);
        hoverLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        hoverWindow.add(hoverLabel);

        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 1) { // Product Column
                    Object val = table.getValueAt(row, col);
                    if (val instanceof ProductWrapper) {
                        ProductWrapper p = (ProductWrapper) val;
                        // Show larger image if available
                        // Simplified for brevity -> In real app, load larger async or use existing
                        // cache
                        // For now, checking if we want to show it.
                    }
                } else {
                    hoverWindow.setVisible(false);
                }
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverWindow.setVisible(false);
            }
        });
    }
}
