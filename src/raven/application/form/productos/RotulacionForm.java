package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.comercial.ServiceSupplier;
import raven.clases.productos.ImpresorTermicaPOSDIG2406T;
import raven.clases.productos.ServiceProduct;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.conexion;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.option.BorderOption;
import raven.clases.productos.ConfiguracionImpresoraXP420B_UNIVERSAL;
import raven.clases.productos.ConfiguracionBarTender;
import raven.clases.inventario.ServiceInventarioBodega;
import raven.controlador.inventario.InventarioBodega;
import raven.application.form.productos.creates.CreateTallas;
import raven.utils.LoadingOverlay;

/**
 * Módulo de Rotulación de Productos - Versión Mejorada
 * Permite generar etiquetas para cajas y pares de productos con
 * vista previa, estadísticas y exportación de reportes.
 */
public class RotulacionForm extends javax.swing.JPanel {

    private static final String STYLE_PANEL_MAIN = "arc:25;background:$Login.background";
    private static final String STYLE_PANEL_HEADER = "arc:20;background:darken($Login.background,5%)";
    private static final String STYLE_PANEL_STATS = "arc:15;background:lighten($Login.background,5%)";
    private static final String STYLE_PANEL_FILTROS = "arc:15;background:darken($Login.background,8%)";
    private static final String STYLE_BUTTON_BASE = "arc:12;borderWidth:0;focusWidth:0;innerFocusWidth:0;margin:6,14,6,14;font:bold";
    private static final String STYLE_BUTTON_NEUTRAL = STYLE_BUTTON_BASE + ";background:darken($Login.background,2%);foreground:$Label.foreground;"
            + "hoverBackground:lighten(darken($Login.background,2%),5%);pressedBackground:darken(darken($Login.background,2%),5%)";
    private static final String STYLE_BUTTON_PRIMARY = STYLE_BUTTON_BASE + ";background:$Component.accentColor;foreground:#fff;"
            + "hoverBackground:lighten($Component.accentColor,8%);pressedBackground:darken($Component.accentColor,8%)";
    private static final String STYLE_BUTTON_DANGER = STYLE_BUTTON_BASE + ";background:#C62828;foreground:#fff;"
            + "hoverBackground:lighten(#C62828,8%);pressedBackground:darken(#C62828,8%)";
    private static final String STYLE_BUTTON_PDF = STYLE_BUTTON_BASE + ";background:#D32F2F;foreground:#fff;"
            + "hoverBackground:lighten(#D32F2F,8%);pressedBackground:darken(#D32F2F,8%)";
    private static final String STYLE_BUTTON_EXCEL = STYLE_BUTTON_BASE + ";background:#1D6F42;foreground:#fff;"
            + "hoverBackground:lighten(#1D6F42,8%);pressedBackground:darken(#1D6F42,8%)";
    private static final String STYLE_TEXTFIELD = "arc:12;borderWidth:0;focusWidth:0;innerFocusWidth:0;margin:5,15,5,15;background:$Panel.background";
    private static final String STYLE_COMBOBOX = "arc:10";

    // ==================== SERVICIOS Y CONFIGURACIÓN ====================
    private final ServiceProduct service = new ServiceProduct();
    private ConfiguracionBarTender configuracionSeleccionadaBarTender;
    private Integer ultimoProductoSeleccionado = null;

    // ==================== COMPONENTES UI PRINCIPALES ====================
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JPanel panelHeader;
    private JPanel panelEstadisticas;
    private JPanel panelFiltros;
    private JPanel panelAcciones;
    private JPanel panelTabla;
    private JTable tablaProd;
    private JScrollPane scroll;
    private DefaultTableModel tableModel;

    // ==================== BÚSQUEDA ====================
    private JTextField txtSearch;

    // ==================== FILTROS ====================
    private JComboBox<String> cbxTipoRotulacion;
    private JComboBox<String> cbxMarca;
    private JComboBox<ProveedorItem> cbxProveedor;
    private JButton btnBuscarVariantesProveedor;

    // ==================== BOTONES ====================
    private JButton btnImprimir;
    private JButton btnExportPDF;
    private JButton btnExportExcel;
    private JButton btnLimpiar;
    private JButton btnSeleccionarTodos;
    private JButton btnDeseleccionarTodos;

    // ==================== ESTADÍSTICAS ====================
    private JLabel lblTotalProductos;
    private JLabel lblTotalEtiquetas;
    private JLabel lblTotalCajas;
    private JLabel lblTotalPares;

    // ==================== ICONOS ====================
    private FontIcon iconPrint;
    private FontIcon iconPdf;
    private FontIcon iconExcel;
    private FontIcon iconTrash;
    private FontIcon iconCheckAll;
    private FontIcon iconUncheckAll;
    private FontIcon iconSearch;
    private FontIcon iconTag;

    public RotulacionForm() {
        initComponentsCustom();
        configurarEstilos();
        configurarEventos();
        cargarMarcas();
        cargarProveedores();
    }

    // ==================== INICIALIZACIÓN DE COMPONENTES ====================

    private void initComponentsCustom() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setOpaque(false);

        // Crear iconos
        crearIconos();

        // Crear panel principal con todos los componentes
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_MAIN);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header con título e icono
        panelHeader = crearPanelHeader();

        // Panel de estadísticas
        panelEstadisticas = crearPanelEstadisticas();

        // Panel de filtros y búsqueda
        panelFiltros = crearPanelFiltros();

        // Panel de acciones (botones)
        panelAcciones = crearPanelAcciones();

        // Panel de tabla
        panelTabla = crearPanelTabla();

        // Panel superior (header + stats + filtros + acciones)
        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new BoxLayout(panelSuperior, BoxLayout.Y_AXIS));
        panelSuperior.setOpaque(false);
        panelSuperior.add(panelHeader);
        panelSuperior.add(Box.createVerticalStrut(15));
        panelSuperior.add(panelEstadisticas);
        panelSuperior.add(Box.createVerticalStrut(10));
        panelSuperior.add(panelFiltros);
        panelSuperior.add(Box.createVerticalStrut(10));
        panelSuperior.add(panelAcciones);

        // Agregar al panel principal
        mainPanel.add(panelSuperior, BorderLayout.NORTH);
        mainPanel.add(panelTabla, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void crearIconos() {
        Color iconColor = UIManager.getColor("Label.foreground");
        iconPrint = FontIcon.of(FontAwesomeSolid.PRINT);
        iconPrint.setIconSize(16);
        iconPrint.setIconColor(Color.WHITE);

        iconPdf = FontIcon.of(FontAwesomeSolid.FILE_PDF);
        iconPdf.setIconSize(16);
        iconPdf.setIconColor(Color.WHITE);

        iconExcel = FontIcon.of(FontAwesomeSolid.FILE_EXCEL);
        iconExcel.setIconSize(16);
        iconExcel.setIconColor(Color.WHITE);

        iconTrash = FontIcon.of(FontAwesomeSolid.TRASH_ALT);
        iconTrash.setIconSize(16);
        iconTrash.setIconColor(Color.WHITE);

        iconCheckAll = FontIcon.of(FontAwesomeSolid.CHECK_DOUBLE);
        iconCheckAll.setIconSize(14);
        iconCheckAll.setIconColor(iconColor);

        iconUncheckAll = FontIcon.of(FontAwesomeSolid.TIMES);
        iconUncheckAll.setIconSize(14);
        iconUncheckAll.setIconColor(iconColor);

        iconSearch = FontIcon.of(FontAwesomeSolid.SEARCH);
        iconSearch.setIconSize(14);
        iconSearch.setIconColor(iconColor);

        iconTag = FontIcon.of(FontAwesomeSolid.TAGS);
        iconTag.setIconSize(28);
        iconTag.setIconColor(new Color(33, 150, 243));
    }

    private JPanel crearPanelHeader() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_HEADER);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Icono grande
        JLabel lblIcon = new JLabel(iconTag);

        // Panel de textos
        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);

        lblTitulo = new JLabel("ROTULACIÓN DE PRODUCTOS");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");

        lblSubtitulo = new JLabel("Genere etiquetas para cajas y pares de productos");
        lblSubtitulo.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");

        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(lblSubtitulo);

        panel.add(lblIcon, BorderLayout.WEST);
        panel.add(textos, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_STATS);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Card: Total Productos
        lblTotalProductos = crearStatCard("Caja", "Productos", "0", new Color(33, 150, 243));

        // Card: Total Etiquetas
        lblTotalEtiquetas = crearStatCard("", "Etiquetas", "0", new Color(156, 39, 176));

        // Card: Cajas
        lblTotalCajas = crearStatCard("", "Cajas", "0", new Color(0, 150, 136));

        // Card: Pares
        lblTotalPares = crearStatCard("Zapatos", "Pares", "0", new Color(255, 152, 0));

        panel.add(crearCardPanel(lblTotalProductos, new Color(33, 150, 243)));
        panel.add(crearCardPanel(lblTotalEtiquetas, new Color(156, 39, 176)));
        panel.add(crearCardPanel(lblTotalCajas, new Color(0, 150, 136)));
        panel.add(crearCardPanel(lblTotalPares, new Color(255, 152, 0)));

        return panel;
    }

    private JLabel crearStatCard(String emoji, String titulo, String valor, Color color) {
        String html = String.format(
                "<html><div style='text-align:center;'>" +
                        "<span style='font-size:20px;'>%s</span><br/>" +
                        "<span style='font-size:11px;color:#888;'>%s</span><br/>" +
                        "<span style='font-size:18px;font-weight:bold;color:#%02x%02x%02x;'>%s</span>" +
                        "</div></html>",
                emoji, titulo, color.getRed(), color.getGreen(), color.getBlue(), valor);
        JLabel label = new JLabel(html, SwingConstants.CENTER);
        return label;
    }

    private JPanel crearCardPanel(JLabel label, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE, "arc:12;background:darken($Login.background,3%)");
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_FILTROS);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Campo de búsqueda
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar producto...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));
        txtSearch.setPreferredSize(new Dimension(240, 40));

        // Combo tipo rotulación
        cbxTipoRotulacion = new JComboBox<>(new String[] { "Seleccionar Tipo", "Caja", "Par" });
        cbxTipoRotulacion.putClientProperty(FlatClientProperties.STYLE, STYLE_COMBOBOX);
        cbxTipoRotulacion.setPreferredSize(new Dimension(140, 40));

        // Combo marca
        cbxMarca = new JComboBox<>(new String[] { "Todas las Marcas" });
        cbxMarca.putClientProperty(FlatClientProperties.STYLE, STYLE_COMBOBOX);
        cbxMarca.setPreferredSize(new Dimension(160, 40));

        cbxProveedor = new JComboBox<>(new DefaultComboBoxModel<>(new ProveedorItem[] { new ProveedorItem(0, "Seleccione Proveedor") }));
        cbxProveedor.putClientProperty(FlatClientProperties.STYLE, STYLE_COMBOBOX);
        cbxProveedor.setPreferredSize(new Dimension(220, 40));

        btnBuscarVariantesProveedor = new JButton("Buscar variantes", iconSearch);
        btnBuscarVariantesProveedor.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_PRIMARY);
        configurarBoton(btnBuscarVariantesProveedor);

        // Botones de selección
        btnSeleccionarTodos = new JButton("Todos", iconCheckAll);
        btnSeleccionarTodos.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_NEUTRAL);
        btnSeleccionarTodos.setToolTipText("Seleccionar todos los productos");
        configurarBoton(btnSeleccionarTodos);

        btnDeseleccionarTodos = new JButton("Ninguno", iconUncheckAll);
        btnDeseleccionarTodos.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_NEUTRAL);
        btnDeseleccionarTodos.setToolTipText("Deseleccionar todos los productos");
        configurarBoton(btnDeseleccionarTodos);

        panel.add(new JLabel("Buscar:"));
        panel.add(txtSearch);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(new JLabel("Tipo:"));
        panel.add(cbxTipoRotulacion);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(new JLabel("Marca:"));
        panel.add(cbxMarca);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(new JLabel("Proveedor:"));
        panel.add(cbxProveedor);
        panel.add(btnBuscarVariantesProveedor);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btnSeleccionarTodos);
        panel.add(btnDeseleccionarTodos);

        return panel;
    }

    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_FILTROS);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Botón Imprimir (principal)
        btnImprimir = new JButton("Imprimir Etiquetas", iconPrint);
        btnImprimir.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_PRIMARY);
        configurarBoton(btnImprimir);

        // Botón Vista Previa Individual
        JButton btnVistaPrevia = new JButton("Vista Previa", iconPrint);
        btnVistaPrevia.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_NEUTRAL);
        btnVistaPrevia.setToolTipText("Ver vista previa de una etiqueta");
        configurarBoton(btnVistaPrevia);
        btnVistaPrevia.addActionListener(e -> mostrarVistaPreviaEtiqueta());

        // Botón Exportar PDF
        btnExportPDF = new JButton("PDF", iconPdf);
        btnExportPDF.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_PDF);
        btnExportPDF.setToolTipText("Exportar lista a PDF");
        configurarBoton(btnExportPDF);

        // Botón Exportar Excel
        btnExportExcel = new JButton("Excel", iconExcel);
        btnExportExcel.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_EXCEL);
        btnExportExcel.setToolTipText("Exportar lista a Excel");
        configurarBoton(btnExportExcel);

        // Botón Limpiar
        btnLimpiar = new JButton("Limpiar Todo", iconTrash);
        btnLimpiar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_DANGER);
        btnLimpiar.setToolTipText("Vaciar toda la tabla");
        configurarBoton(btnLimpiar);

        panel.add(btnImprimir);
        panel.add(btnVistaPrevia);
        panel.add(btnExportPDF);
        panel.add(btnExportExcel);
        panel.add(btnLimpiar);

        Dimension pref = panel.getPreferredSize();
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        return panel;
    }

    private void configurarBoton(JButton button) {
        button.setPreferredSize(new Dimension(0, 40));
        button.setIconTextGap(8);
        button.setFocusable(false);
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Modelo de tabla
        String[] columnas = { "SELECT", "EAN", "NOMBRE", "COLOR", "TALLA", "CANTIDAD", "ID", "TIPO" };
        tableModel = new DefaultTableModel(columnas, 0) {
            Class[] types = new Class[] {
                    Boolean.class, String.class, String.class, String.class,
                    String.class, Integer.class, Integer.class, String.class
            };
            boolean[] canEdit = new boolean[] { true, false, false, false, false, true, false, false };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        tablaProd = new JTable(tableModel);
        tablaProd.setRowHeight(55);
        tablaProd.getTableHeader().setReorderingAllowed(false);

        // Ocultar columna ID
        tablaProd.getColumnModel().getColumn(6).setMinWidth(0);
        tablaProd.getColumnModel().getColumn(6).setMaxWidth(0);
        tablaProd.getColumnModel().getColumn(6).setWidth(0);

        // Configurar anchos de columnas
        tablaProd.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablaProd.getColumnModel().getColumn(1).setPreferredWidth(130);
        tablaProd.getColumnModel().getColumn(2).setPreferredWidth(200);
        tablaProd.getColumnModel().getColumn(3).setPreferredWidth(100);
        tablaProd.getColumnModel().getColumn(4).setPreferredWidth(80);
        tablaProd.getColumnModel().getColumn(5).setPreferredWidth(80);
        tablaProd.getColumnModel().getColumn(7).setPreferredWidth(80);

        // Estilos de tabla
        tablaProd.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:35;hoverBackground:null;pressedBackground:null;separatorColor:$Login.background;font:bold");

        tablaProd.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:55;showHorizontalLines:true;intercellSpacing:0,1;" +
                        "cellFocusColor:$TableHeader.hoverBackground;selectionBackground:$TableHeader.hoverBackground;"
                        +
                        "selectionForeground:$Table.foreground;background:$Login.background");

        // Renderizadores personalizados
        tablaProd.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(tablaProd, 0));
        tablaProd.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(tablaProd));
        tablaProd.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxCellRenderer());
        tablaProd.getColumnModel().getColumn(5).setCellRenderer(new CantidadCellRenderer());
        tablaProd.getColumnModel().getColumn(5).setCellEditor(new CantidadCellEditor());
        tablaProd.getColumnModel().getColumn(7).setCellRenderer(new TipoRotulacionCellRenderer());

        // Listener para actualizar estadísticas al cambiar selección o cantidad
        tableModel.addTableModelListener(e -> actualizarEstadisticas());

        scroll = new JScrollPane(tablaProd);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;trackInsets:3,3,3,3;thumbInsets:3,3,3,3;background:$Table.background");

        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== CONFIGURACIÓN DE ESTILOS ====================

    private void configurarEstilos() {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new java.awt.Font(FlatRobotoFont.FAMILY, java.awt.Font.PLAIN, 13));

        ModalDialog.getDefaultOption()
                .setOpacity(0.3f)
                .getLayoutOption().setAnimateScale(0.1f);
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM);

        try {
            conexion.getInstance().connectToDatabase();
            conexion.getInstance().close();
        } catch (SQLException e) {
            // Silencioso
        }
    }

    // ==================== CONFIGURACIÓN DE EVENTOS ====================

    private void configurarEventos() {
        txtSearch.addActionListener(e -> abrirBuscadorVariantesPorProveedor(txtSearch.getText().trim()));

        // Cambio de tipo rotulación
        cbxTipoRotulacion.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (cbxTipoRotulacion.getSelectedIndex() == 0)
                    return;
                if (ultimoProductoSeleccionado != null) {
                    try {
                        cargarVariantesDeProducto(ultimoProductoSeleccionado);
                    } catch (SQLException ex) {
                        Toast.show(this, Toast.Type.ERROR, "Error al recargar variantes: " + ex.getMessage());
                    }
                }
            }
        });

        // Filtrar por marca
        cbxMarca.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Futuro: implementar filtrado por marca
            }
        });

        // Seleccionar todos
        btnSeleccionarTodos.addActionListener(e -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(true, i, 0);
            }
        });

        // Deseleccionar todos
        btnDeseleccionarTodos.addActionListener(e -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(false, i, 0);
            }
        });

        // Limpiar tabla
        btnLimpiar.addActionListener(e -> {
            if (tableModel.getRowCount() == 0) {
                Toast.show(this, Toast.Type.INFO, "La tabla ya está vacía");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de limpiar toda la tabla?",
                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                tableModel.setRowCount(0);
                actualizarEstadisticas();
                Toast.show(this, Toast.Type.SUCCESS, "Tabla limpiada correctamente");
            }
        });

        // Imprimir
        btnImprimir.addActionListener(e -> imprimirEtiquetas());

        // Exportar PDF
        btnExportPDF.addActionListener(e -> exportarPDF());

        // Exportar Excel
        btnExportExcel.addActionListener(e -> exportarExcel());

        btnBuscarVariantesProveedor.addActionListener(e -> abrirBuscadorVariantesPorProveedor());

        configurarAtajoProductosSinEan();
        configurarAtajoConversionesCajaAPares();
        configurarAtajoImpresionEstanteria();
    }

    private void configurarAtajoProductosSinEan() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        String key = "rotulacion.productosSinEan";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarDialogoProductosSinEan();
            }
        });
    }

    private void mostrarDialogoProductosSinEan() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Productos sin código de barras", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        JLabel title = new JLabel("Productos con EAN vacío o sin llenar");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        JLabel subtitle = new JLabel("Ctrl+Shift+B");
        subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        dialog.add(header, BorderLayout.NORTH);

        String[] cols = { "SEL", "ID_VARIANTE", "MODELO", "NOMBRE", "COLOR", "TALLA", "PROVEEDOR", "EAN" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            Class[] types = new Class[] { Boolean.class, Integer.class, String.class, String.class, String.class, String.class, String.class,
                    String.class };
            boolean[] canEdit = new boolean[] { true, false, false, false, false, false, false, true };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setWidth(0);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        dialog.add(sp, BorderLayout.CENTER);

        JButton btnRecargar = new JButton("Recargar");
        JButton btnGenerarSel = new JButton("Generar seleccionados");
        JButton btnGenerarTodos = new JButton("Generar todos");
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCerrar = new JButton("Cerrar");

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        footer.add(btnRecargar);
        footer.add(btnGenerarSel);
        footer.add(btnGenerarTodos);
        footer.add(btnGuardar);
        footer.add(btnCerrar);
        dialog.add(footer, BorderLayout.SOUTH);

        Runnable recargar = () -> {
            try {
                cargarProductosSinEan(model);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error cargando productos sin EAN: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };

        btnRecargar.addActionListener(e -> recargar.run());

        btnGenerarSel.addActionListener(e -> {
            try (Connection conn = conexion.getInstance().createConnection()) {
                AtomicInteger seq = new AtomicInteger(1);
                Set<String> reservados = eansEnTabla(model, 7);

                for (int r = 0; r < model.getRowCount(); r++) {
                    if (!Boolean.TRUE.equals(model.getValueAt(r, 0))) {
                        continue;
                    }
                    String actual = str(model.getValueAt(r, 7)).trim();
                    if (!actual.isEmpty()) {
                        continue;
                    }

                    String modelo = str(model.getValueAt(r, 2));
                    String nombre = str(model.getValueAt(r, 3));
                    String color = str(model.getValueAt(r, 4));
                    String talla = str(model.getValueAt(r, 5));

                    String ean = generarEanUnico(conn, modelo, talla, color, seq.getAndIncrement(), reservados);
                    if (ean == null) {
                        JOptionPane.showMessageDialog(dialog, "No se pudo generar EAN único para: " + nombre, "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    model.setValueAt(ean, r, 7);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error generando EAN: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        btnGenerarTodos.addActionListener(e -> {
            for (int r = 0; r < model.getRowCount(); r++) {
                model.setValueAt(Boolean.TRUE, r, 0);
            }
            btnGenerarSel.doClick();
        });

        btnGuardar.addActionListener(e -> {
            Pattern eanPattern = Pattern.compile("^\\d{13}$");
            Set<String> vistos = new HashSet<>();
            for (int r = 0; r < model.getRowCount(); r++) {
                String ean = str(model.getValueAt(r, 7)).trim();
                if (ean.isEmpty()) {
                    continue;
                }
                if (!eanPattern.matcher(ean).matches()) {
                    JOptionPane.showMessageDialog(dialog, "EAN inválido en fila " + (r + 1) + ": " + ean, "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!vistos.add(ean)) {
                    JOptionPane.showMessageDialog(dialog, "EAN duplicado en la tabla: " + ean, "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try (Connection conn = conexion.getInstance().createConnection()) {
                conn.setAutoCommit(false);
                int actualizados = 0;

                try (PreparedStatement psCheck = conn.prepareStatement("SELECT COUNT(*) AS existe FROM producto_variantes WHERE ean = ?");
                        PreparedStatement psUpdate = conn.prepareStatement(
                                "UPDATE producto_variantes SET ean=?, fecha_actualizacion=CURRENT_TIMESTAMP WHERE id_variante=? AND TRIM(COALESCE(ean,''))=''")) {

                    for (int r = 0; r < model.getRowCount(); r++) {
                        String ean = str(model.getValueAt(r, 7)).trim();
                        if (ean.isEmpty()) {
                            continue;
                        }

                        psCheck.setString(1, ean);
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next() && rs.getInt("existe") > 0) {
                                conn.rollback();
                                JOptionPane.showMessageDialog(dialog, "El EAN ya existe en la base de datos: " + ean,
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }

                        int idVariante = (Integer) model.getValueAt(r, 1);
                        psUpdate.setString(1, ean);
                        psUpdate.setInt(2, idVariante);
                        actualizados += psUpdate.executeUpdate();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(dialog, "EAN guardados: " + actualizados, "Listo",
                        JOptionPane.INFORMATION_MESSAGE);
                recargar.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error guardando EAN: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCerrar.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(btnGuardar);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        recargar.run();
        if (model.getRowCount() == 0) {
            Toast.show(this, Toast.Type.SUCCESS, "No hay productos sin EAN");
            dialog.dispose();
            return;
        }

        dialog.setSize(1000, 520);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void configurarAtajoConversionesCajaAPares() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);
        String key = "rotulacion.conversionesCajaAPares";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarDialogoConversionesCajaAPares();
            }
        });
    }

    private void mostrarDialogoConversionesCajaAPares() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Conversiones caja→pares", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        JLabel title = new JLabel("Conversiones caja→pares (inventario)");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        JLabel subtitle = new JLabel("Ctrl+H");
        subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JLabel lblBuscar = new JLabel("Buscar:");
        JTextField txtBuscar = new JTextField(28);
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        JButton btnBuscar = new JButton("Buscar", iconSearch);
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_PRIMARY);
        btnBuscar.setPreferredSize(new Dimension(130, 40));
        top.add(lblBuscar);
        top.add(txtBuscar);
        top.add(btnBuscar);
        
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        north.add(header);
        north.add(top);
        dialog.add(north, BorderLayout.NORTH);

        String[] cols = { "ID_REF", "ID_VARIANTE_CAJA", "CONVERSION", "FECHA", "PRODUCTO", "COLOR", "TALLA", "CAJAS", "PARES" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            Class[] types = new Class[] { Integer.class, Integer.class, String.class, Timestamp.class, String.class, String.class,
                    String.class, Integer.class, Integer.class };
            boolean[] canEdit = new boolean[] { false, false, false, false, false, false, false, false, false };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        for (int col : new int[] { 0, 1 }) {
            table.getColumnModel().getColumn(col).setMinWidth(0);
            table.getColumnModel().getColumn(col).setMaxWidth(0);
            table.getColumnModel().getColumn(col).setWidth(0);
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        dialog.add(sp, BorderLayout.CENTER);

        JButton btnCargar = new JButton("Cargar productos");
        JButton btnCargarImprimir = new JButton("Cargar e imprimir");
        JButton btnCerrar = new JButton("Cerrar");

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        footer.add(btnCargar);
        footer.add(btnCargarImprimir);
        footer.add(btnCerrar);
        dialog.add(footer, BorderLayout.SOUTH);

        Runnable recargar = () -> {
            try {
                cargarConversionesCajaAPares(model, txtBuscar.getText().trim());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error cargando conversiones: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        btnBuscar.addActionListener(e -> recargar.run());
        txtBuscar.addActionListener(e -> recargar.run());

        ActionListener cargarSeleccion = e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                Toast.show(dialog, Toast.Type.WARNING, "Seleccione una conversión");
                return;
            }
            int row = viewRow;
            try {
                row = table.convertRowIndexToModel(viewRow);
            } catch (Exception ignore) {
            }

            Integer idRef = (Integer) model.getValueAt(row, 0);
            Integer idVarianteCaja = (Integer) model.getValueAt(row, 1);
            Timestamp fecha = (Timestamp) model.getValueAt(row, 3);
            String conversion = String.valueOf(model.getValueAt(row, 2));
            if (idRef == null || idRef <= 0) {
                Toast.show(dialog, Toast.Type.ERROR, "Conversión inválida");
                return;
            }

            try {
                cargarProductosDeConversion(idRef, idVarianteCaja != null ? idVarianteCaja : 0, fecha, conversion);
                dialog.dispose();
                if (e.getSource() == btnCargarImprimir) {
                    imprimirEtiquetas();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error cargando productos: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };

        btnCargar.addActionListener(cargarSeleccion);
        btnCargarImprimir.addActionListener(cargarSeleccion);
        btnCerrar.addActionListener(e -> dialog.dispose());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    btnCargar.doClick();
                }
            }
        });

        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        recargar.run();
        dialog.setSize(new Dimension(1150, 560));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void cargarConversionesCajaAPares(DefaultTableModel model, String term) throws SQLException {
        model.setRowCount(0);

        boolean hasTerm = term != null && !term.isBlank();
        String sql = "SELECT im.id_referencia AS id_ref, im.id_variante AS id_variante_caja, "
                + "im.fecha_movimiento AS fecha_conversion, "
                + "COALESCE(p.nombre,'') AS producto, COALESCE(c.nombre,'') AS color, "
                + "COALESCE(CONCAT(t.numero, ' ', t.sistema),'') AS talla, "
                + "ABS(COALESCE(im.cantidad,0)) AS cajas_convertidas, "
                + "COALESCE(SUM(CASE WHEN ip.tipo_movimiento = 'entrada par' THEN COALESCE(ip.cantidad_pares, ip.cantidad, 0) ELSE 0 END),0) AS pares_generados "
                + "FROM inventario_movimientos im "
                + "INNER JOIN producto_variantes pv ON pv.id_variante = im.id_variante "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN inventario_movimientos ip ON ip.tipo_referencia = 'conversion_caja_pares' AND ip.id_referencia = im.id_referencia "
                + "WHERE im.tipo_referencia = 'conversion_caja_pares' AND im.tipo_movimiento = 'salida caja' "
                + (hasTerm ? "AND (p.nombre LIKE ? OR CONCAT(im.id_referencia,'') LIKE ?) " : "")
                + "GROUP BY im.id_referencia, im.id_variante, im.fecha_movimiento, p.nombre, c.nombre, t.numero, t.sistema, im.cantidad "
                + "ORDER BY im.fecha_movimiento DESC "
                + "LIMIT 200";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasTerm) {
                String like = "%" + term + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idRef = rs.getInt("id_ref");
                    String conv = "CONVERSIÓN #" + idRef;
                    model.addRow(new Object[] { idRef, rs.getInt("id_variante_caja"), conv, rs.getTimestamp("fecha_conversion"),
                            rs.getString("producto"), rs.getString("color"), rs.getString("talla"),
                            rs.getInt("cajas_convertidas"), rs.getInt("pares_generados") });
                }
            }
        }
    }

    private void cargarProductosDeConversion(int idDetalleTraspaso, int idVarianteCaja, Timestamp fechaConversion,
            String numeroTraspaso) throws SQLException {
        if (tableModel.getRowCount() > 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Esto reemplazará la lista actual de rotulación.\n¿Desea continuar?",
                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        cbxTipoRotulacion.setSelectedIndex(2);
        tableModel.setRowCount(0);

        String sql = "SELECT pv.id_producto, COALESCE(pv.ean,'') AS ean, COALESCE(p.nombre,'') AS nombre, "
                + "COALESCE(c.nombre,'') AS color, COALESCE(CONCAT(t.numero, ' ', t.sistema),'') AS talla, "
                + "COALESCE(SUM(COALESCE(im.cantidad_pares, im.cantidad, 0)),0) AS cantidad "
                + "FROM inventario_movimientos im "
                + "INNER JOIN producto_variantes pv ON im.id_variante = pv.id_variante "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE im.tipo_referencia = 'conversion_caja_pares' AND im.id_referencia = ? "
                + "AND im.tipo_movimiento = 'entrada par' "
                + "GROUP BY pv.id_producto, pv.ean, p.nombre, c.nombre, t.numero, t.sistema "
                + "HAVING COALESCE(SUM(COALESCE(im.cantidad_pares, im.cantidad, 0)),0) > 0 "
                + "ORDER BY p.nombre, c.nombre, t.numero";

        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetalleTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int cant = Math.max(0, rs.getInt("cantidad"));
                    if (cant <= 0) continue;
                    rows.add(new Object[] { Boolean.TRUE, rs.getString("ean"), rs.getString("nombre"), rs.getString("color"),
                            rs.getString("talla"), cant, rs.getInt("id_producto"), "Par" });
                }
            }
        }

        if (rows.isEmpty()) {
            tableModel.setRowCount(0);
            actualizarEstadisticas();
            Toast.show(this, Toast.Type.WARNING, "No se encontraron productos para esta conversión: " + numeroTraspaso);
            return;
        }

        for (Object[] r : rows) {
            tableModel.addRow(r);
        }
        actualizarEstadisticas();
        Toast.show(this, Toast.Type.SUCCESS, "Productos cargados: " + rows.size());
    }

    private void cargarProductosSinEan(DefaultTableModel model) throws SQLException {
        model.setRowCount(0);

        String sql = "SELECT pv.id_variante, p.codigo_modelo, p.nombre, " +
                "COALESCE(c.nombre,'') AS color, " +
                "CONCAT(COALESCE(t.numero,''), ' ', COALESCE(t.sistema,'')) AS talla, " +
                "COALESCE(prov.nombre,'') AS proveedor " +
                "FROM producto_variantes pv " +
                "INNER JOIN productos p ON p.id_producto = pv.id_producto " +
                "LEFT JOIN colores c ON c.id_color = pv.id_color " +
                "LEFT JOIN tallas t ON t.id_talla = pv.id_talla " +
                "LEFT JOIN proveedores prov ON prov.id_proveedor = pv.id_proveedor " +
                "WHERE pv.disponible = 1 AND p.activo = 1 AND TRIM(COALESCE(pv.ean,'')) = '' " +
                "ORDER BY p.nombre, c.nombre, t.numero";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[] {
                        Boolean.TRUE,
                        rs.getInt("id_variante"),
                        rs.getString("codigo_modelo"),
                        rs.getString("nombre"),
                        rs.getString("color"),
                        rs.getString("talla"),
                        rs.getString("proveedor"),
                        ""
                });
            }
        }
    }

    private Set<String> eansEnTabla(DefaultTableModel model, int colEan) {
        Set<String> s = new HashSet<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String ean = str(model.getValueAt(r, colEan)).trim();
            if (!ean.isEmpty()) {
                s.add(ean);
            }
        }
        return s;
    }

    private String generarEanUnico(Connection conn, String modelo, String talla, String color, int secuenciaInicial, Set<String> reservados)
            throws SQLException {
        int secuencia = Math.max(1, secuenciaInicial);
        for (int i = 0; i < 5000; i++) {
            String candidate = generarEAN13(modelo, talla, color, secuencia++);
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (reservados.contains(candidate)) {
                continue;
            }
            if (existeEan(conn, candidate)) {
                continue;
            }
            reservados.add(candidate);
            return candidate;
        }
        return null;
    }

    private boolean existeEan(Connection conn, String ean) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS existe FROM producto_variantes WHERE ean = ?")) {
            ps.setString(1, ean);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("existe") > 0;
            }
        }
    }

    private String generarEAN13(String modelo, String talla, String color, int secuencia) {
        String base = (modelo == null ? "" : modelo.replaceAll("[^0-9]", ""))
                + (talla == null ? "" : talla.replaceAll("[^0-9]", ""))
                + (color == null ? "" : Integer.toString(Math.abs(color.hashCode())));
        if (base.isEmpty()) {
            base = Long.toString(System.currentTimeMillis() % 1000000);
        }

        String seqStr = String.format("%02d", secuencia);
        String baseDigits = base.replaceAll("[^0-9]", "");
        
        // Construir los primeros 12 dígitos asegurando incluir la secuencia
        String digits;
        int targetLen = 12;
        int seqLen = seqStr.length();
        
        if (baseDigits.length() + seqLen > targetLen) {
            // Si excede, truncamos la base pero conservamos la secuencia (que varía)
            int keepBase = Math.max(0, targetLen - seqLen);
            digits = baseDigits.substring(0, keepBase) + seqStr;
        } else {
            digits = baseDigits + seqStr;
        }

        // Rellenar con ceros a la izquierda si es muy corto
        if (digits.length() < 12) {
             // Usar StringBuilder para evitar parsear a Long si no es necesario y manejar strings vacíos
             StringBuilder sb = new StringBuilder(digits);
             while (sb.length() < 12) {
                 sb.insert(0, "0");
             }
             digits = sb.toString();
        }
        
        int checksum = calcularEAN13Checksum(digits);
        return digits + checksum;
    }

    private int calcularEAN13Checksum(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = twelveDigits.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : (10 - mod);
    }

    private String str(Object v) {
        return v == null ? "" : v.toString();
    }

    /**
     * Muestra una vista previa de cómo se vería una etiqueta con los datos actuales
     */
    public void mostrarVistaPreviaEtiqueta() {
        java.util.List<Integer> filas = new java.util.ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0))) {
                filas.add(i);
            }
        }
        if (filas.isEmpty()) {
            int[] selected = tablaProd.getSelectedRows();
            if (selected != null && selected.length > 0) {
                for (int idx : selected) {
                    if (idx >= 0 && idx < tableModel.getRowCount()) filas.add(idx);
                }
            }
        }
        if (filas.isEmpty()) {
            if (tableModel.getRowCount() > 0) {
                filas.add(0);
            } else {
                Toast.show(this, Toast.Type.INFO, "Agregue al menos un producto para ver la vista previa");
                return;
            }
        }

        // Crear una tabla temporal con las filas seleccionadas
        DefaultTableModel modeloTemp = new DefaultTableModel(
            new String[] { "SELECT", "EAN", "NOMBRE", "COLOR", "TALLA", "CANTIDAD", "ID", "TIPO" },
            0
        ) {
            Class[] types = new Class[] {
                Boolean.class, String.class, String.class, String.class,
                String.class, Integer.class, Integer.class, String.class
            };
            boolean[] canEdit = new boolean[] { true, false, false, false, false, true, false, false };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        for (Integer filaSeleccionada : filas) {
            Object[] fila = new Object[8];
            fila[0] = true;
            for (int col = 1; col < 8; col++) {
                fila[col] = tableModel.getValueAt(filaSeleccionada, col);
            }
            modeloTemp.addRow(fila);
        }

        JTable tablaTemp = new JTable(modeloTemp);
        ImpresorTermicaPOSDIG2406T.ModoImpresion modo = ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
        if (cbxTipoRotulacion.getSelectedIndex() == 1) {
            modo = ImpresorTermicaPOSDIG2406T.ModoImpresion.CAJA;
        }

        ImpresorTermicaPOSDIG2406T impresor = new ImpresorTermicaPOSDIG2406T(tablaTemp, modo);
        impresor.setMargenes(2, 1, 2, 1);

        java.awt.Window win = SwingUtilities.getWindowAncestor(this);
        java.awt.Frame owner = win instanceof java.awt.Frame ? (java.awt.Frame) win : null;

        // Mostrar vista previa
        RotulacionPrintPreviewDialog dialog = new RotulacionPrintPreviewDialog(owner, tablaTemp, impresor);
        dialog.setVisible(true);
    }

    // ==================== CARGA DE DATOS ====================

    private void cargarMarcas() {
        String sql = "SELECT nombre FROM marcas WHERE activo = 1 ORDER BY nombre";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cbxMarca.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            // Silencioso
        }
    }

    private void cargarProveedores() {
        if (cbxProveedor == null) {
            return;
        }
        DefaultComboBoxModel<ProveedorItem> model = new DefaultComboBoxModel<>();
        model.addElement(new ProveedorItem(0, "Seleccione Proveedor"));

        String sql = "SELECT id_proveedor, nombre FROM proveedores WHERE activo = 1 ORDER BY nombre";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                model.addElement(new ProveedorItem(rs.getInt("id_proveedor"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            // Silencioso
        }

        cbxProveedor.setModel(model);
        cbxProveedor.setSelectedIndex(0);
    }

    private void cargarVariantesDeProducto(int idProducto) throws SQLException {
        boolean modoCaja = cbxTipoRotulacion.getSelectedIndex() == 1;
        boolean modoPar = cbxTipoRotulacion.getSelectedIndex() == 2;
        if (!modoCaja && !modoPar) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione Caja o Par antes de buscar");
            return;
        }

        tableModel.setRowCount(0);

        String campoStock = modoCaja ? "COALESCE(ib.Stock_caja,0)" : "COALESCE(ib.Stock_par,0)";
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS color, " +
                "CONCAT(t.numero, ' ', t.sistema) AS talla, " +
                "COALESCE(pv.ean, '') AS codigo_barras " +
                "FROM producto_variantes pv " +
                "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo=1 " +
                "WHERE pv.id_producto = ? AND pv.disponible = 1 AND " + campoStock + " > 0 " +
                "ORDER BY c.nombre, t.numero";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[] {
                            Boolean.TRUE,
                            rs.getString("codigo_barras"),
                            rs.getString("nombre"),
                            rs.getString("color"),
                            rs.getString("talla"),
                            1,
                            rs.getInt("id_producto"),
                            modoCaja ? "Caja" : "Par"
                    };
                    tableModel.addRow(row);
                }
            }
        }

        actualizarEstadisticas();
        cbxTipoRotulacion.setSelectedIndex(modoCaja ? 1 : 2);
    }

    private int detectarTipoDisponible(int idProducto) {
        String sql = "SELECT " +
                "SUM(CASE WHEN pv.disponible=1 AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante=pv.id_variante AND ib.activo=1 AND COALESCE(ib.Stock_caja,0)>0) THEN 1 ELSE 0 END) AS tiene_caja, "
                +
                "SUM(CASE WHEN pv.disponible=1 AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante=pv.id_variante AND ib.activo=1 AND COALESCE(ib.Stock_par,0)>0) THEN 1 ELSE 0 END) AS tiene_par "
                +
                "FROM producto_variantes pv WHERE pv.id_producto = ?";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idProducto);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    int cajas = rs.getInt("tiene_caja");
                    int pares = rs.getInt("tiene_par");
                    if (cajas > 0 && pares == 0)
                        return 1;
                    if (pares > 0 && cajas == 0)
                        return 2;
                    return 0;
                }
            }
        } catch (SQLException e) {
            // Ignorar
        }
        return 0;
    }

    // ==================== ESTADÍSTICAS ====================

    private void actualizarEstadisticas() {
        int totalProductos = tableModel.getRowCount();
        int totalEtiquetas = 0;
        int totalCajas = 0;
        int totalPares = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) tableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(seleccionado)) {
                int cantidad = 1;
                try {
                    cantidad = Integer.parseInt(tableModel.getValueAt(i, 5).toString());
                } catch (Exception e) {
                    cantidad = 1;
                }
                totalEtiquetas += cantidad;

                String tipo = (String) tableModel.getValueAt(i, 7);
                if ("Caja".equals(tipo)) {
                    totalCajas += cantidad;
                } else {
                    totalPares += cantidad;
                }
            }
        }

        actualizarStatCard(lblTotalProductos, "", "Productos", String.valueOf(totalProductos),
                new Color(33, 150, 243));
        actualizarStatCard(lblTotalEtiquetas, "", "Etiquetas", String.valueOf(totalEtiquetas),
                new Color(156, 39, 176));
        actualizarStatCard(lblTotalCajas, "", "Cajas", String.valueOf(totalCajas), new Color(0, 150, 136));
        actualizarStatCard(lblTotalPares, "", "Pares", String.valueOf(totalPares), new Color(255, 152, 0));
    }

    private void actualizarStatCard(JLabel label, String emoji, String titulo, String valor, Color color) {
        String iconHtml = (emoji != null && !emoji.trim().isEmpty())
                ? String.format("<span style='font-size:20px;'>%s</span><br/>", emoji)
                : "";
        String html = String.format(
                "<html><div style='text-align:center;'>" +
                        "%s" +
                        "<span style='font-size:11px;color:#888;'>%s</span><br/>" +
                        "<span style='font-size:18px;font-weight:bold;color:#%02x%02x%02x;'>%s</span>" +
                        "</div></html>",
                iconHtml, titulo, color.getRed(), color.getGreen(), color.getBlue(), valor);
        label.setText(html);
    }

    private void commitTablaEdicion() {
        try {
            if (tablaProd != null && tablaProd.isEditing() && tablaProd.getCellEditor() != null) {
                tablaProd.getCellEditor().stopCellEditing();
            }
        } catch (Exception ignore) {
        }
    }

    private JTable buildTablaParaEtiquetas() {
        java.util.List<Integer> filas = new java.util.ArrayList<>();
        boolean hayCheckbox = false;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0))) {
                filas.add(i);
                hayCheckbox = true;
            }
        }

        if (!hayCheckbox) {
            int[] selected = tablaProd != null ? tablaProd.getSelectedRows() : null;
            if (selected != null && selected.length > 0) {
                java.util.Set<Integer> unique = new java.util.LinkedHashSet<>();
                for (int viewRow : selected) {
                    int modelRow = viewRow;
                    try {
                        if (tablaProd != null) modelRow = tablaProd.convertRowIndexToModel(viewRow);
                    } catch (Exception ignore) {
                    }
                    if (modelRow >= 0 && modelRow < tableModel.getRowCount()) unique.add(modelRow);
                }
                filas.addAll(unique);
            }
        }

        if (filas.isEmpty()) return null;

        DefaultTableModel modeloTemp = new DefaultTableModel(
                new String[]{"SELECT", "EAN", "NOMBRE", "COLOR", "TALLA", "CANTIDAD", "ID", "TIPO"},
                0
        ) {
            Class[] types = new Class[] {
                    Boolean.class, String.class, String.class, String.class,
                    String.class, Integer.class, Integer.class, String.class
            };
            boolean[] canEdit = new boolean[] { true, false, false, false, false, true, false, false };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        for (Integer filaSeleccionada : filas) {
            Object[] fila = new Object[8];
            fila[0] = true;
            for (int col = 1; col < 8; col++) {
                fila[col] = tableModel.getValueAt(filaSeleccionada, col);
            }
            modeloTemp.addRow(fila);
        }

        return new JTable(modeloTemp);
    }

    // ==================== IMPRESIÓN ====================

    private void imprimirEtiquetas() {
        commitTablaEdicion();
        ImpresorTermicaPOSDIG2406T.ModoImpresion modo = null;
        if (cbxTipoRotulacion.getSelectedIndex() == 1) {
            modo = ImpresorTermicaPOSDIG2406T.ModoImpresion.CAJA;
        } else if (cbxTipoRotulacion.getSelectedIndex() == 2) {
            modo = ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
        }
        if (modo == null) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una opción de rotulación válida");
            return;
        }

        JTable tablaParaEtiquetas = buildTablaParaEtiquetas();
        if (tablaParaEtiquetas == null) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un producto para imprimir");
            return;
        }

        try {
            ImpresorTermicaPOSDIG2406T impresor = new ImpresorTermicaPOSDIG2406T(tablaParaEtiquetas, modo);
            impresor.setMargenes(2, 1, 2, 1);   
            configuracionSeleccionadaBarTender = null;

            // Intentar detectar impresora y configurar según sea necesario
            try {
                javax.print.PrintService def = javax.print.PrintServiceLookup.lookupDefaultPrintService();
                String nombre = def != null ? def.getName() : null;
                if (nombre != null && ConfiguracionImpresoraXP420B_UNIVERSAL.detectarImpresoraXP420B(nombre)) {
                    ConfiguracionImpresoraXP420B_UNIVERSAL cfg = new ConfiguracionImpresoraXP420B_UNIVERSAL();
                    cfg.leerConfiguracionesBarTender(nombre);
                    configuracionSeleccionadaBarTender = cfg.getConfiguracionSeleccionada();
                    if (configuracionSeleccionadaBarTender != null) {
                        impresor.setCustomPaperSizeMM(
                                configuracionSeleccionadaBarTender.anchoMm,
                                configuracionSeleccionadaBarTender.altoMm
                                        + (modo == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA ? 7.0 : 0.0));
                        impresor.setRotate180("VERTICAL_180".equals(configuracionSeleccionadaBarTender.orientacion));
                    }
                }
            } catch (Exception ignored) {
            }

            if (configuracionSeleccionadaBarTender == null && modo == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                impresor.setEtiquetaSizeMM(30.0, 34.0);
            }

            java.awt.Window win = SwingUtilities.getWindowAncestor(this);
            java.awt.Frame owner = win instanceof java.awt.Frame ? (java.awt.Frame) win : null;

            // Verificar si hay impresoras disponibles
            javax.print.PrintService[] servicios = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
            boolean hayImpresoras = servicios != null && servicios.length > 0;

            // Si no hay impresoras, mostrar mensaje al usuario antes de abrir la vista previa
            if (!hayImpresoras) {
                int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "No se detectaron impresoras en el sistema.\n¿Desea generar un archivo PDF en su lugar?",
                    "Impresora no encontrada",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (respuesta == JOptionPane.YES_OPTION) {
                    // Abrir diálogo de vista previa que permita guardar como PDF
                    RotulacionPrintPreviewDialog dialog = new RotulacionPrintPreviewDialog(owner, tablaParaEtiquetas, impresor);
                    dialog.setVisible(true);
                } else {
                    // Si elige no, mostrar vista previa normal para que pueda imprimir manualmente
                    RotulacionPrintPreviewDialog dialog = new RotulacionPrintPreviewDialog(owner, tablaParaEtiquetas, impresor);
                    dialog.setVisible(true);
                }
            } else {
                // Si hay impresoras, abrir diálogo normal
                RotulacionPrintPreviewDialog dialog = new RotulacionPrintPreviewDialog(owner, tablaParaEtiquetas, impresor);
                dialog.setVisible(true);
            }
        } catch (Exception ex) {
            Toast.show(this, Toast.Type.ERROR, "Error abriendo vista previa: " + ex.getMessage());
        }
    }

    // ==================== EXPORTACIÓN PDF ====================

    private void exportarPDF() {
        commitTablaEdicion();
        if (tableModel.getRowCount() == 0) {
            Toast.show(this, Toast.Type.WARNING, "No hay datos para exportar");
            return;
        }

        // Mostrar diálogo para elegir tipo de PDF
        Object[] opciones = {"PDF de Etiquetas", "PDF de Reporte", "Cancelar"};
        int eleccion = JOptionPane.showOptionDialog(
            this,
            "Seleccione el tipo de PDF a generar:",
            "Tipo de PDF",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opciones,
            opciones[0]
        );

        if (eleccion == 2 || eleccion == JOptionPane.CLOSED_OPTION) {
            return; // Cancelar
        }

        if (eleccion == 0) { // PDF de Etiquetas
            JTable tablaParaEtiquetas = buildTablaParaEtiquetas();
            if (tablaParaEtiquetas == null) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un producto para exportar");
                return;
            }
            // Abrir diálogo de vista previa para generar etiquetas como PDF
            ImpresorTermicaPOSDIG2406T.ModoImpresion modo = null;
            if (cbxTipoRotulacion.getSelectedIndex() == 1) {
                modo = ImpresorTermicaPOSDIG2406T.ModoImpresion.CAJA;
            } else if (cbxTipoRotulacion.getSelectedIndex() == 2) {
                modo = ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
            } else {
                Toast.show(this, Toast.Type.WARNING, "Seleccione Caja o Par para generar etiquetas");
                return;
            }

            // Crear impresor con tabla actualizada
            ImpresorTermicaPOSDIG2406T impresor = new ImpresorTermicaPOSDIG2406T(tablaParaEtiquetas, modo);
            impresor.setMargenes(2, 1, 2, 1);
            configuracionSeleccionadaBarTender = null;

            try {
                javax.print.PrintService def = javax.print.PrintServiceLookup.lookupDefaultPrintService();
                String nombre = def != null ? def.getName() : null;
                if (nombre != null && ConfiguracionImpresoraXP420B_UNIVERSAL.detectarImpresoraXP420B(nombre)) {
                    ConfiguracionImpresoraXP420B_UNIVERSAL cfg = new ConfiguracionImpresoraXP420B_UNIVERSAL();
                    cfg.leerConfiguracionesBarTender(nombre);
                    configuracionSeleccionadaBarTender = cfg.getConfiguracionSeleccionada();
                    if (configuracionSeleccionadaBarTender != null) {
                        impresor.setCustomPaperSizeMM(
                                configuracionSeleccionadaBarTender.anchoMm,
                                configuracionSeleccionadaBarTender.altoMm
                                        + (modo == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA ? 4.0 : 0.0));
                        impresor.setRotate180("VERTICAL_180".equals(configuracionSeleccionadaBarTender.orientacion));
                    }
                }
            } catch (Exception ignored) {
            }

            if (configuracionSeleccionadaBarTender == null && modo == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                impresor.setEtiquetaSizeMM(30.0, 34.0);
            }

            java.awt.Window win = SwingUtilities.getWindowAncestor(this);
            java.awt.Frame owner = win instanceof java.awt.Frame ? (java.awt.Frame) win : null;

            // Actualizar el impresor antes de abrir el diálogo
            impresor.prepararEtiquetasParaImpresion();

            RotulacionPrintPreviewDialog dialog = new RotulacionPrintPreviewDialog(owner, tablaParaEtiquetas, impresor);
            dialog.setVisible(true);
        } else { // PDF de Reporte
            // Código original para generar reporte
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar reporte PDF");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            fileChooser.setSelectedFile(new File("Rotulacion_" + sdf.format(new Date()) + ".pdf"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }

                try {
                    generarPDF(file);
                    Toast.show(this, Toast.Type.SUCCESS, "PDF de reporte generado exitosamente");
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception e) {
                    Toast.show(this, Toast.Type.ERROR, "Error al generar PDF de reporte: " + e.getMessage());
                }
            }
        }
    }

    private void generarPDF(File file) throws DocumentException, FileNotFoundException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph title = new Paragraph("Lista de Rotulación de Productos", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Paragraph fecha = new Paragraph("Generado: " + sdf.format(new Date()), normalFont);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        fecha.setSpacingAfter(15);
        document.add(fecha);

        PdfPTable pdfTable = new PdfPTable(6);
        pdfTable.setWidthPercentage(100);
        pdfTable.setWidths(new float[] { 2f, 2.5f, 1.2f, 1f, 0.8f, 1f });

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        String[] headers = { "EAN", "Nombre", "Color", "Talla", "Cant.", "Tipo" };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(33, 150, 243));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            pdfTable.addCell(cell);
        }

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean sel = (Boolean) tableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(sel)) {
                pdfTable.addCell(new Phrase(String.valueOf(tableModel.getValueAt(i, 1)), dataFont));
                pdfTable.addCell(new Phrase(String.valueOf(tableModel.getValueAt(i, 2)), dataFont));
                pdfTable.addCell(new Phrase(String.valueOf(tableModel.getValueAt(i, 3)), dataFont));
                pdfTable.addCell(new Phrase(String.valueOf(tableModel.getValueAt(i, 4)), dataFont));
                pdfTable.addCell(new Phrase(String.valueOf(tableModel.getValueAt(i, 5)), dataFont));
                pdfTable.addCell(new Phrase(String.valueOf(tableModel.getValueAt(i, 7)), dataFont));
            }
        }

        document.add(pdfTable);
        document.close();
    }

    // ==================== EXPORTACIÓN EXCEL ====================

    private void exportarExcel() {
        if (tableModel.getRowCount() == 0) {
            Toast.show(this, Toast.Type.WARNING, "No hay datos para exportar");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar reporte Excel");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        fileChooser.setSelectedFile(new File("Rotulacion_" + sdf.format(new Date()) + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            try {
                generarExcel(file);
                Toast.show(this, Toast.Type.SUCCESS, "Excel generado exitosamente");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception e) {
                Toast.show(this, Toast.Type.ERROR, "Error al generar Excel: " + e.getMessage());
            }
        }
    }

    private void generarExcel(File file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Rotulación");

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = { "EAN", "Nombre", "Color", "Talla", "Cantidad", "Tipo" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Boolean sel = (Boolean) tableModel.getValueAt(i, 0);
                if (Boolean.TRUE.equals(sel)) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(String.valueOf(tableModel.getValueAt(i, 1)));
                    row.createCell(1).setCellValue(String.valueOf(tableModel.getValueAt(i, 2)));
                    row.createCell(2).setCellValue(String.valueOf(tableModel.getValueAt(i, 3)));
                    row.createCell(3).setCellValue(String.valueOf(tableModel.getValueAt(i, 4)));
                    row.createCell(4).setCellValue(Integer.parseInt(tableModel.getValueAt(i, 5).toString()));
                    row.createCell(5).setCellValue(String.valueOf(tableModel.getValueAt(i, 7)));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void cargarDesdeTallasSeleccionadas(List<CreateTallas.TallaVariante> tallas) {
        if (tallas == null || tallas.isEmpty())
            return;
        for (CreateTallas.TallaVariante tv : tallas) {
            String sql = "SELECT p.id_producto, p.nombre, c.nombre AS color, " +
                    "CONCAT(t.numero, ' ', t.sistema) AS talla, " +
                    "COALESCE(pv.ean, '') AS codigo_barras " +
                    "FROM producto_variantes pv " +
                    "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
                    "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                    "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                    "WHERE pv.id_variante = ? LIMIT 1";
            try (Connection conn = conexion.getInstance().createConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, tv.getIdVariante());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Object[] row = new Object[] {
                                Boolean.TRUE,
                                rs.getString("codigo_barras"),
                                rs.getString("nombre"),
                                rs.getString("color"),
                                rs.getString("talla"),
                                tv.getCantidadSeleccionada(),
                                rs.getInt("id_producto"),
                                "Par"
                        };
                        tableModel.addRow(row);
                    }
                }
            } catch (SQLException e) {
                Object[] row = new Object[] {
                        Boolean.TRUE,
                        tv.getEan(),
                        "",
                        "",
                        tv.getTalla() != null ? tv.getTalla().getNombre() : "",
                        tv.getCantidadSeleccionada(),
                        0,
                        "Par"
                };
                tableModel.addRow(row);
            }
        }
        cbxTipoRotulacion.setSelectedIndex(2);
        actualizarEstadisticas();
    }

    private void abrirBuscadorVariantesPorProveedor() {
        abrirBuscadorVariantesPorProveedor("");
    }

    private void abrirBuscadorVariantesPorProveedor(String initialTerm) {
        ProveedorItem proveedor = cbxProveedor != null ? (ProveedorItem) cbxProveedor.getSelectedItem() : null;

        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        java.awt.Frame ownerFrame = owner instanceof java.awt.Frame ? (java.awt.Frame) owner : null;
        JDialog dialog = new JDialog(ownerFrame, "Buscar variantes por proveedor", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        LoadingOverlay overlay = new LoadingOverlay();
        dialog.setGlassPane(overlay);
        overlay.setVisible(false);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        header.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.setOpaque(false);
        JLabel lblProveedor = new JLabel("Proveedor:");
        JComboBox<ProveedorItem> cbProveedorModal = new JComboBox<>();
        DefaultComboBoxModel<ProveedorItem> proveedorModalModel = new DefaultComboBoxModel<>();
        proveedorModalModel.addElement(new ProveedorItem(0, "Todos los Proveedores"));
        if (cbxProveedor != null) {
            ComboBoxModel<ProveedorItem> base = cbxProveedor.getModel();
            for (int i = 0; i < base.getSize(); i++) {
                ProveedorItem it = base.getElementAt(i);
                if (it != null && it.idProveedor > 0) {
                    proveedorModalModel.addElement(it);
                }
            }
        }
        cbProveedorModal.setModel(proveedorModalModel);
        if (proveedor != null && proveedor.idProveedor > 0) {
            cbProveedorModal.setSelectedItem(proveedor);
        } else {
            cbProveedorModal.setSelectedIndex(0);
        }
        cbProveedorModal.putClientProperty(FlatClientProperties.STYLE, STYLE_COMBOBOX);
        cbProveedorModal.setPreferredSize(new Dimension(260, 40));

        JLabel lblBuscar = new JLabel("Buscar:");
        JTextField txtBuscarDb = new JTextField(22);
        txtBuscarDb.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        txtBuscarDb.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre (case-sensitive)");
        if (initialTerm != null && !initialTerm.isBlank()) {
            txtBuscarDb.setText(initialTerm.trim());
        }

        JButton btnBuscar = new JButton("Buscar", iconSearch);
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_PRIMARY);
        btnBuscar.setPreferredSize(new Dimension(130, 40));

        row1.add(lblProveedor);
        row1.add(cbProveedorModal);
        row1.add(lblBuscar);
        row1.add(txtBuscarDb);
        row1.add(btnBuscar);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row2.setOpaque(false);
        JLabel lblFiltrar = new JLabel("Filtrar:");
        JTextField txtFiltrarTabla = new JTextField(22);
        txtFiltrarTabla.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        txtFiltrarTabla.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filtrar resultados mostrados");
        row2.add(lblFiltrar);
        row2.add(txtFiltrarTabla);

        header.add(row1);
        header.add(row2);
        dialog.add(header, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new String[] { "Producto", "Color", "Género", "Proveedor", "EAN", "Talla/Zuela", "ID Producto",
                        "ID Variante", "Tipo" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ProductoImagenCell.class;
                }
                if (columnIndex == 6 || columnIndex == 7) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(44);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.putClientProperty(FlatClientProperties.STYLE,
                "selectionBackground:$Component.accentColor;selectionForeground:$Button.default.foreground;showHorizontalLines:false;showVerticalLines:false");

        JTableHeader th = table.getTableHeader();
        th.putClientProperty(FlatClientProperties.STYLE, "height:36;hoverBackground:null;font:bold");
        th.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                if (column == 0 && value instanceof ProductoImagenCell) {
                    ProductoImagenCell cell = (ProductoImagenCell) value;
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, cell.nombre, isSelected, hasFocus,
                            row, column);
                    label.setIcon(cell.icon);
                    label.setIconTextGap(10);
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    if (!isSelected) {
                        label.setBackground(row % 2 == 0 ? table.getBackground()
                                : UIManager.getColor("Table.alternateRowColor"));
                        label.setForeground(UIManager.getColor("Label.foreground"));
                    }
                    return label;
                }

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    ((JLabel) c).setIcon(null);
                    ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
                }
                return c;
            }
        });

        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        sorter.setComparator(0, (a, b) -> {
            String sa = a != null ? a.toString() : "";
            String sb = b != null ? b.toString() : "";
            return sa.compareTo(sb);
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        dialog.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        bottom.setOpaque(false);

        JButton btnPrev = new JButton("Anterior");
        btnPrev.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_NEUTRAL);
        JButton btnNext = new JButton("Siguiente");
        btnNext.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_NEUTRAL);
        JLabel lblPagina = new JLabel("Página 1/1 (0)");

        JComboBox<Integer> cmbPageSize = new JComboBox<>(new Integer[] { 25, 50, 100, 200 });
        cmbPageSize.putClientProperty(FlatClientProperties.STYLE, STYLE_COMBOBOX);
        cmbPageSize.setSelectedItem(50);
        cmbPageSize.setPreferredSize(new Dimension(90, 32));

        JPanel paging = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        paging.setOpaque(false);
        paging.add(btnPrev);
        paging.add(btnNext);
        paging.add(lblPagina);
        paging.add(new JLabel("Mostrar:"));
        paging.add(cmbPageSize);

        JButton btnAgregar = new JButton("Agregar");
        btnAgregar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_PRIMARY);
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON_NEUTRAL);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(btnAgregar);
        actions.add(btnCerrar);

        bottom.add(paging, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);
        dialog.add(bottom, BorderLayout.SOUTH);

        table.removeColumn(table.getColumn("EAN"));
        table.removeColumn(table.getColumn("ID Producto"));
        table.removeColumn(table.getColumn("ID Variante"));
        table.removeColumn(table.getColumn("Tipo"));

        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(360); // Producto
        tcm.getColumn(1).setPreferredWidth(160); // Color
        tcm.getColumn(2).setPreferredWidth(120); // Género
        tcm.getColumn(3).setPreferredWidth(160); // Proveedor
        tcm.getColumn(4).setPreferredWidth(110); // Talla

        AtomicInteger modalVersion = new AtomicInteger(0);
        int[] currentPage = new int[] { 0 };
        int[] totalRows = new int[] { 0 };
        int[] pageSize = new int[] { (Integer) cmbPageSize.getSelectedItem() };

        Runnable updatePagingControls = () -> {
            int totalPages = (int) Math.ceil(totalRows[0] / (double) pageSize[0]);
            totalPages = Math.max(1, totalPages);
            int pageDisplay = Math.min(currentPage[0] + 1, totalPages);
            lblPagina.setText("Página " + pageDisplay + "/" + totalPages + " (" + totalRows[0] + ")");
            btnPrev.setEnabled(currentPage[0] > 0);
            btnNext.setEnabled(currentPage[0] + 1 < totalPages);
        };

        Runnable loadPage = () -> {
            ProveedorItem provSel = (ProveedorItem) cbProveedorModal.getSelectedItem();
            if (provSel == null) {
                return;
            }

            final int version = modalVersion.incrementAndGet();
            final int page = currentPage[0];
            final int limit = pageSize[0];
            final String term = txtBuscarDb.getText().trim();

            overlay.setMensaje("Cargando resultados...");
            overlay.start();

            SwingWorker<BusquedaVariantesPage, Void> worker = new SwingWorker<>() {
                @Override
                protected BusquedaVariantesPage doInBackground() {
                    return buscarVariantesProveedorPage(provSel.idProveedor, term, page, limit);
                }

                @Override
                protected void done() {
                    overlay.stop();
                    if (version != modalVersion.get()) {
                        return;
                    }
                    try {
                        BusquedaVariantesPage result = get();
                        totalRows[0] = result.total;
                        model.setRowCount(0);
                        for (VarianteProveedorRow r : result.rows) {
                            model.addRow(new Object[] {
                                    new ProductoImagenCell(r.nombreProducto, r.icon),
                                    r.color,
                                    r.genero,
                                    r.proveedor,
                                    r.ean,
                                    r.talla,
                                    r.idProducto,
                                    r.idVariante,
                                    r.tipoDetectado
                            });
                        }
                        updatePagingControls.run();
                    } catch (Exception e) {
                        totalRows[0] = 0;
                        model.setRowCount(0);
                        updatePagingControls.run();
                    }
                }
            };
            worker.execute();
        };

        txtFiltrarTabla.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String text = txtFiltrarTabla.getText();
                if (text == null || text.isBlank()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter(Pattern.quote(text.trim())));
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply();
            }
        });

        btnBuscar.addActionListener(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });

        txtBuscarDb.addActionListener(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });

        btnPrev.addActionListener(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });

        btnNext.addActionListener(e -> {
            currentPage[0]++;
            loadPage.run();
        });

        cmbPageSize.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                pageSize[0] = (Integer) cmbPageSize.getSelectedItem();
                currentPage[0] = 0;
                loadPage.run();
            }
        });

        cbProveedorModal.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentPage[0] = 0;
                loadPage.run();
            }
        });

        Runnable addSelected = () -> {
            int[] viewRows = table.getSelectedRows();
            if (viewRows == null || viewRows.length == 0) {
                Toast.show(this, Toast.Type.INFO, "Seleccione al menos un resultado");
                return;
            }

            int added = 0;
            for (int viewRow : viewRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                String ean = String.valueOf(model.getValueAt(modelRow, 4));
                String talla = String.valueOf(model.getValueAt(modelRow, 5));
                String tipo = String.valueOf(model.getValueAt(modelRow, 8));
                int idProducto = 0;
                try {
                    idProducto = (Integer) model.getValueAt(modelRow, 6);
                } catch (Exception ex) {
                    idProducto = 0;
                }
                ProductoImagenCell prodCell = (ProductoImagenCell) model.getValueAt(modelRow, 0);
                String nombre = prodCell != null ? prodCell.nombre : "";
                String color = String.valueOf(model.getValueAt(modelRow, 1));

                if (yaExisteEnTabla(ean, tipo)) {
                    continue;
                }

                tableModel.addRow(new Object[] { Boolean.TRUE, ean, nombre, color, talla, 1, idProducto, tipo });
                added++;
            }
            if (added > 0) {
                actualizarEstadisticas();
                Toast.show(this, Toast.Type.SUCCESS, "Agregado: " + added);
            } else {
                Toast.show(this, Toast.Type.INFO, "No se agregaron nuevos registros");
            }
        };

        btnAgregar.addActionListener(e -> addSelected.run());
        btnCerrar.addActionListener(e -> dialog.dispose());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
                    addSelected.run();
                }
            }
        });

        dialog.setMinimumSize(new Dimension(900, 520));
        dialog.setSize(new Dimension(1100, 650));
        dialog.setLocationRelativeTo(this);
        loadPage.run();
        dialog.setVisible(true);
    }

    private boolean yaExisteEnTabla(String ean, String tipo) {
        if (ean == null) {
            ean = "";
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String existingEan = String.valueOf(tableModel.getValueAt(i, 1));
            String existingTipo = String.valueOf(tableModel.getValueAt(i, 7));
            if (ean.equals(existingEan) && tipo.equals(existingTipo)) {
                return true;
            }
        }
        return false;
    }

    private BusquedaVariantesPage buscarVariantesProveedorPage(int idProveedor, String term, int page, int pageSize) {
        List<VarianteProveedorRow> rows = new ArrayList<>();
        int total = 0;

        StringBuilder where = new StringBuilder();
        where.append(" WHERE pv.disponible = 1 AND p.activo = 1 ");
        if (idProveedor > 0) {
            where.append(" AND pv.id_proveedor = ? ");
        }
        boolean hasTerm = term != null && !term.isBlank();
        if (hasTerm) {
            where.append(" AND (p.nombre COLLATE utf8mb4_bin LIKE ?) ");
        }

        String sqlCount = "SELECT COUNT(*) AS total "
                + "FROM producto_variantes pv "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + where;

        String sqlData = "SELECT pv.id_variante, pv.id_producto, COALESCE(pv.ean,'') AS ean, "
                + "p.nombre AS nombre_producto, pv.imagen, "
                + "COALESCE(c.nombre,'') AS color, COALESCE(t.genero,'') AS genero, "
                + "COALESCE(CONCAT(t.numero, ' ', t.sistema),'') AS talla, "
                + "prov.nombre AS proveedor, "
                + "CASE "
                + "WHEN COALESCE(SUM(ib.Stock_caja),0) > 0 THEN 'Caja' "
                + "WHEN COALESCE(SUM(ib.Stock_par),0) > 0 THEN 'Par' "
                + "ELSE '' END AS tipo_detectado "
                + "FROM producto_variantes pv "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + "INNER JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 "
                + where
                + " GROUP BY pv.id_variante, pv.id_producto, pv.ean, p.nombre, pv.imagen, c.nombre, t.genero, t.numero, t.sistema, prov.nombre "
                + " ORDER BY p.nombre, c.nombre, t.numero "
                + " LIMIT ? OFFSET ? ";

        try (Connection conn = conexion.getInstance().createConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                int idx = 1;
                if (idProveedor > 0) {
                    ps.setInt(idx++, idProveedor);
                }
                if (hasTerm) {
                    ps.setString(idx++, "%" + term + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt("total");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlData)) {
                int idx = 1;
                if (idProveedor > 0) {
                    ps.setInt(idx++, idProveedor);
                }
                if (hasTerm) {
                    ps.setString(idx++, "%" + term + "%");
                }
                ps.setInt(idx++, pageSize);
                ps.setInt(idx++, page * pageSize);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        byte[] imgBytes = rs.getBytes("imagen");
                        ImageIcon icon = null;
                        if (imgBytes != null && imgBytes.length > 0) {
                            try {
                                ImageIcon raw = new ImageIcon(imgBytes);
                                java.awt.Image scaled = raw.getImage().getScaledInstance(34, 34, java.awt.Image.SCALE_SMOOTH);
                                icon = new ImageIcon(scaled);
                            } catch (Exception ignored) {
                                icon = null;
                            }
                        }
                        VarianteProveedorRow row = new VarianteProveedorRow(
                                rs.getInt("id_variante"),
                                rs.getInt("id_producto"),
                                rs.getString("ean"),
                                rs.getString("nombre_producto"),
                                rs.getString("color"),
                                rs.getString("genero"),
                                rs.getString("proveedor"),
                                rs.getString("talla"),
                                rs.getString("tipo_detectado"),
                                icon);
                        rows.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            total = 0;
            rows.clear();
        }

        return new BusquedaVariantesPage(total, rows);
    }

    private static final class ProveedorItem {
        private final int idProveedor;
        private final String nombre;

        private ProveedorItem(int idProveedor, String nombre) {
            this.idProveedor = idProveedor;
            this.nombre = nombre != null ? nombre : "";
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static final class ProductoImagenCell {
        private final String nombre;
        private final ImageIcon icon;

        private ProductoImagenCell(String nombre, ImageIcon icon) {
            this.nombre = nombre != null ? nombre : "";
            this.icon = icon;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static final class VarianteProveedorRow {
        private final int idVariante;
        private final int idProducto;
        private final String ean;
        private final String nombreProducto;
        private final String color;
        private final String genero;
        private final String proveedor;
        private final String talla;
        private final String tipoDetectado;
        private final ImageIcon icon;

        private VarianteProveedorRow(int idVariante, int idProducto, String ean, String nombreProducto, String color,
                String genero, String proveedor, String talla, String tipoDetectado, ImageIcon icon) {
            this.idVariante = idVariante;
            this.idProducto = idProducto;
            this.ean = ean != null ? ean : "";
            this.nombreProducto = nombreProducto != null ? nombreProducto : "";
            this.color = color != null ? color : "";
            this.genero = genero != null ? genero : "";
            this.proveedor = proveedor != null ? proveedor : "";
            this.talla = talla != null ? talla : "";
            this.tipoDetectado = tipoDetectado != null ? tipoDetectado : "";
            this.icon = icon;
        }
    }

    private static final class BusquedaVariantesPage {
        private final int total;
        private final List<VarianteProveedorRow> rows;

        private BusquedaVariantesPage(int total, List<VarianteProveedorRow> rows) {
            this.total = total;
            this.rows = rows != null ? rows : java.util.Collections.emptyList();
        }
    }

    public void imprimirSeleccionActual() {
        imprimirEtiquetas();
    }

    private void configurarAtajoImpresionEstanteria() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
        String key = "rotulacion.imprimirEstanteria";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirSeleccionEstanteria();
            }
        });
    }

    private void abrirSeleccionEstanteria() {
        SeleccionEstanteriaDialog dialog = new SeleccionEstanteriaDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.getSelectedUbicacion() != null && !dialog.getSelectedUbicacion().isEmpty()) {
            String ubicacion = dialog.getSelectedUbicacion();
            cargarProductosPorEstanteria(ubicacion);
        }
    }

    private void cargarProductosPorEstanteria(String ubicacion) {
        LoadingOverlay loading = new LoadingOverlay();
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof RootPaneContainer) {
            ((RootPaneContainer) win).setGlassPane(loading);
        }
        loading.start();

        new Thread(() -> {
            try {
                ServiceInventarioBodega serviceInv = new ServiceInventarioBodega();
                List<InventarioBodega> productos = serviceInv.listarProductosPorUbicacion(ubicacion);

                SwingUtilities.invokeLater(() -> {
                    loading.stop();
                    if (productos.isEmpty()) {
                        Toast.show(this, Toast.Type.WARNING, "No se encontraron productos en la estantería: " + ubicacion);
                        return;
                    }
                    
                    if (tableModel.getRowCount() > 0) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Esto reemplazará la lista actual de rotulación.\n¿Desea continuar?",
                                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }

                    cbxTipoRotulacion.setSelectedIndex(2); // "Par"
                    tableModel.setRowCount(0);
                    
                    int count = 0;
                    for (InventarioBodega inv : productos) {
                         int cantidad = (inv.getStockPar() != null ? inv.getStockPar() : 0); 
                         if (cantidad > 0) {
                             tableModel.addRow(new Object[] {
                                 Boolean.TRUE,
                                 inv.getEan(),
                                 inv.getNombreProducto(),
                                 inv.getColor(),
                                 inv.getTalla(),
                                 cantidad,
                                 inv.getIdProducto(),
                                 "Par"
                             });
                             count++;
                         }
                    }
                    
                    actualizarEstadisticas();
                    Toast.show(this, Toast.Type.SUCCESS, "Cargados " + count + " productos de estantería " + ubicacion);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loading.stop();
                    Toast.show(this, Toast.Type.ERROR, "Error cargando estantería: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
}

// ==================== RENDERIZADORES Y EDITORES PERSONALIZADOS
// ====================

/**
 * Renderizador para checkbox grandes y centrados
 */

// ==================== CLASES INTERNAS DE RENDERIZADORES ====================

/**
 * Renderizador para checkbox grandes y centrados
 */
class CheckBoxCellRenderer implements TableCellRenderer {
    private final JCheckBox check = new JCheckBox();

    public CheckBoxCellRenderer() {
        check.setHorizontalAlignment(SwingConstants.CENTER);
        check.setOpaque(false);
        check.putClientProperty(FlatClientProperties.STYLE, "font:$h2.font;icon.borderWidth:0;icon.focusWidth:0;");
        check.setPreferredSize(new Dimension(32, 32));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        check.setSelected(Boolean.TRUE.equals(value));
        if (isSelected) {
            check.setBackground(table.getSelectionBackground());
        } else {
            check.setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
        }
        return check;
    }
}

/**
 * Renderizador para la columna de cantidad con estilo numérico
 */
class CantidadCellRenderer extends DefaultTableCellRenderer {
    public CantidadCellRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value != null) {
            setText("<html><b>" + value.toString() + "</b></html>");
            setForeground(new Color(33, 150, 243));
        }

        if (!isSelected) {
            setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
        }

        return this;
    }
}

/**
 * Editor de celda para la columna cantidad con spinner
 */
class CantidadCellEditor extends DefaultCellEditor {
    private JSpinner spinner;
    private SpinnerNumberModel model;

    public CantidadCellEditor() {
        super(new JTextField());
        model = new SpinnerNumberModel(1, 1, 99, 1);
        spinner = new JSpinner(model);
        spinner.putClientProperty(FlatClientProperties.STYLE, "arc:8");
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        int val = 1;
        try {
            val = Integer.parseInt(value.toString());
        } catch (Exception e) {
            val = 1;
        }
        spinner.setValue(val);
        return spinner;
    }

    @Override
    public Object getCellEditorValue() {
        return spinner.getValue();
    }
}

/**
 * Renderizador para la columna de tipo de rotulación con colores
 */
class TipoRotulacionCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        if (value != null && !isSelected) {
            String tipo = value.toString();
            String bgColor;
            String textColor = "white";

            if ("Caja".equals(tipo)) {
                bgColor = "#0277BD"; // Azul oscuro
            } else if ("Par".equals(tipo)) {
                bgColor = "#2E7D32"; // Verde oscuro
            } else {
                bgColor = "#757575"; // Gris
            }

            label.setText("<html><div style='padding:4px 10px;border-radius:10px;background-color:" +
                    bgColor + ";color:" + textColor + ";font-weight:bold;'>" + tipo + "</div></html>");
        }

        if (!isSelected) {
            label.setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
        }

        return label;
    }
}
