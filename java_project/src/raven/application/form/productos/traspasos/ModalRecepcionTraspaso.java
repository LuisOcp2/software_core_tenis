package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.productos.RecepcionTraspaso;
import raven.controlador.principal.conexion;
import raven.componentes.notificacion.Notification;
import raven.utils.tono.CorporateTone;

public class ModalRecepcionTraspaso extends JDialog {

    // Constantes de estilo CORREGIDAS
    private static String CARD_STYLE = "arc:12;background:$Login.background;border:1,1,1,1,#e2e8f0";
    private static String SECTION_STYLE = "arc:12;background:$Login.background;border:1,1,1,1,#e2e8f0";
    private static String BUTTON_PRIMARY = "arc:8;background:#10b981;foreground:#ffffff;hoverBackground:#059669";
    private static String BUTTON_SECONDARY = "arc:8;background:#f1f5f9;foreground:#475569;hoverBackground:#e2e8f0";
    private static String INPUT_STYLE = "arc:6;background:#ffffff;border:1,1,1,1,#d1d5db";

    // Campos principales
    private String numeroTraspaso;
    private RecepcionTraspaso.RecepcionCallback callback;
    private List<Map<String, Object>> productosEnviados;

    // Componentes UI
    private JPanel mainContainer;
    private JPanel headerPanel;
    private JPanel infoGridPanel;
    private JPanel timelinePanel;
    private JPanel statsPanel;
    private JPanel productsPanel;
    private JPanel actionsPanel;

    private JTable tablaProductos;
    private JCheckBox chkSelectAll;
    private JTextArea txtObservaciones;
    private JButton btnConfirmar;
    private JButton btnCancelar;

    // Labels de estadísticas
    private JLabel lblTotalEnviado;
    private JLabel lblTotalSeleccionado;
    private JLabel lblTotalUnidades;
    private JLabel lblUnidadesRecibir;

    // Datos
    private Map<String, Object> infoTraspaso;
    private Map<Integer, Integer> cantidadesOriginales = new HashMap<>();

    public ModalRecepcionTraspaso(Frame parent, String numeroTraspaso,
            RecepcionTraspaso.RecepcionCallback callback) {
        super(parent, "Recepción de Traspaso", true);
        this.numeroTraspaso = numeroTraspaso;
        this.callback = callback;

        inicializarUI();
        cargarDatos();
        configurarEventos();

        setSize(1200, 850);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void inicializarUI() {
        // Configurar look and feel
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // Container principal con scroll
        setMainContainer(new JPanel(new BorderLayout()));
        JScrollPane scrollPane = new JScrollPane(getMainContainer());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        crearHeader();
        crearInfoGrid();
        crearTimeline();
        crearSeccionEstadisticas();
        crearSeccionProductos();
        crearSeccionAcciones();

        // Ensamblar layout
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(0, 20));
        topPanel.setOpaque(false);
        topPanel.add(getHeaderPanel(), BorderLayout.NORTH);
        topPanel.add(getInfoGridPanel(), BorderLayout.CENTER);

        JPanel middlePanel = new JPanel(new BorderLayout(0, 20));
        middlePanel.setOpaque(false);
        middlePanel.add(getTimelinePanel(), BorderLayout.NORTH);
        middlePanel.add(getStatsPanel(), BorderLayout.CENTER);

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(middlePanel, BorderLayout.CENTER);
        contentPanel.add(getProductsPanel(), BorderLayout.CENTER);
        contentPanel.add(getActionsPanel(), BorderLayout.SOUTH);

        getMainContainer().add(contentPanel, BorderLayout.CENTER);
    }

    private void crearHeader() {
        setHeaderPanel(new JPanel(new BorderLayout()));
        // Usar color sólido en lugar de gradiente
        getHeaderPanel().setBackground(new Color(99, 102, 241)); // #6366f1
        getHeaderPanel().setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JPanel headerContent = new JPanel(new BorderLayout());
        headerContent.setOpaque(false);

        JLabel titulo = new JLabel("Recepción de Traspaso");
        titulo.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 32));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel(getNumeroTraspaso() + " - Confirmar productos recibidos");
        subtitulo.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 18));
        subtitulo.setForeground(new Color(255, 255, 255, 230)); // Usar alpha directamente

        JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusBadge.setOpaque(false);
        statusBadge.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JLabel badge = new JLabel("  EN TRÁNSITO  ");
        badge.setOpaque(true);
        badge.setBackground(new Color(255, 255, 255, 51)); // rgba(255,255,255,0.2)
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 12));
        badge.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel statusIcon = new JLabel("●");
        statusIcon.setForeground(new Color(16, 185, 129));
        statusIcon.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 12));

        JPanel badgeContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgeContainer.setOpaque(false);
        badgeContainer.add(statusIcon);
        badgeContainer.add(badge);
        statusBadge.add(badgeContainer);

        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setOpaque(false);
        textContainer.add(titulo, BorderLayout.NORTH);
        textContainer.add(subtitulo, BorderLayout.CENTER);
        textContainer.add(statusBadge, BorderLayout.SOUTH);

        headerContent.add(textContainer, BorderLayout.CENTER);
        getHeaderPanel().add(headerContent, BorderLayout.CENTER);
    }

    private void crearInfoGrid() {
        setInfoGridPanel(new JPanel(new GridLayout(1, 4, 20, 0)));
        getInfoGridPanel().setOpaque(false);

        // Crear cards de información
        crearInfoCard("Bodega Origen", "Bodega Principal", FontAwesomeSolid.BUILDING, new Color(37, 99, 235));
        crearInfoCard("Bodega Destino", "Sucursal Centro", FontAwesomeSolid.BULLSEYE, new Color(22, 163, 74));
        crearInfoCard("Fecha Envío", "26 Ago 2025, 14:30", FontAwesomeSolid.CALENDAR_DAY, new Color(217, 119, 6));
        crearInfoCard("Enviado por", "María González", FontAwesomeSolid.USER, new Color(147, 51, 234));
    }

    private void crearInfoCard(String titulo, String valor, FontAwesomeSolid icono, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE, getCARD_STYLE());
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);

        JLabel iconLabel = new JLabel();
        FontIcon icon = FontIcon.of(icono, 20, iconColor);
        iconLabel.setIcon(icon);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 32)); // 20% alpha
        //iconLabel.setBorder(BorderFactory.createEmptyPadding(10, 10, 10, 10));

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 10));
        // tituloLabel.setForeground(new Color(100, 116, 139)); // #64748b

        JLabel valorLabel = new JLabel(valor);
        valorLabel.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 18));
        //valorLabel.setForeground(new Color(30, 41, 59)); // #1e293b

        textPanel.add(tituloLabel, BorderLayout.NORTH);
        textPanel.add(valorLabel, BorderLayout.CENTER);

        header.add(iconLabel);
        header.add(textPanel);

        card.add(header, BorderLayout.CENTER);
        getInfoGridPanel().add(card);
    }

    private void crearTimeline() {
        setTimelinePanel(new JPanel(new BorderLayout()));
        getTimelinePanel().putClientProperty(FlatClientProperties.STYLE, getCARD_STYLE());
        getTimelinePanel().setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Seguimiento del Traspaso", JLabel.CENTER);
        titulo.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 24));
        //titulo.setForeground(new Color(30, 41, 59)); // #1e293b

        JLabel subtitulo = new JLabel("Estado actual del proceso de transferencia", JLabel.CENTER);
        subtitulo.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 14));
        //subtitulo.setForeground(new Color(100, 116, 139)); // #64748b

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titulo, BorderLayout.NORTH);
        header.add(subtitulo, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 32, 0));

        // Timeline steps
        JPanel timeline = new JPanel(new GridLayout(1, 4, 0, 0));
        timeline.setOpaque(false);

        timeline.add(crearTimelineStep("Solicitado", "25 Ago, 09:15", true, false));
        timeline.add(crearTimelineStep("Autorizado", "25 Ago, 10:30", true, false));
        timeline.add(crearTimelineStep("Enviado", "26 Ago, 14:30", true, false));
        timeline.add(crearTimelineStep("En Recepción", "Ahora", false, true));

        getTimelinePanel().add(header, BorderLayout.NORTH);
        getTimelinePanel().add(timeline, BorderLayout.CENTER);
    }

    private JPanel crearTimelineStep(String label, String time, boolean completed, boolean current) {
        JPanel step = new JPanel(new BorderLayout());
        step.setOpaque(false);

        JLabel circle = new JLabel(completed ? "OK" : (current ? "" : "○"), JLabel.CENTER);
        circle.setPreferredSize(new Dimension(48, 48));
        circle.setOpaque(true);
        circle.setBackground(completed ? new Color(16, 185, 129)
                : (current ? new Color(245, 158, 11) : new Color(229, 231, 235)));
        //circle.setForeground(completed || current ? Color.WHITE : new Color(156, 163, 175));
        circle.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 20));
        circle.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        JLabel labelText = new JLabel(label, JLabel.CENTER);
        labelText.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 14));
        // labelText.setForeground(new Color(55, 65, 81)); // #374151

        JLabel timeText = new JLabel(time, JLabel.CENTER);
        timeText.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 12));
        // timeText.setForeground(new Color(107, 114, 128)); // #6b7280

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(labelText, BorderLayout.NORTH);
        textPanel.add(timeText, BorderLayout.CENTER);
        textPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        step.add(circle, BorderLayout.NORTH);
        step.add(textPanel, BorderLayout.CENTER);

        return step;
    }

    private void crearSeccionEstadisticas() {
        setStatsPanel(new JPanel(new GridLayout(1, 4, 16, 0)));
        getStatsPanel().setOpaque(false);

        setLblTotalEnviado(crearStatCard("24", "Productos Enviados"));
        setLblTotalSeleccionado(crearStatCard("0", "Productos Seleccionados"));
        setLblTotalUnidades(crearStatCard("148", "Total Unidades"));
        setLblUnidadesRecibir(crearStatCard("0", "Unidades a Recibir"));

        getStatsPanel().add(crearStatCardPanel(getLblTotalEnviado(), "Productos Enviados"));
        getStatsPanel().add(crearStatCardPanel(getLblTotalSeleccionado(), "Productos Seleccionados"));
        getStatsPanel().add(crearStatCardPanel(getLblTotalUnidades(), "Total Unidades"));
        getStatsPanel().add(crearStatCardPanel(getLblUnidadesRecibir(), "Unidades a Recibir"));
    }

    private JLabel crearStatCard(String numero, String label) {
        JLabel statNumber = new JLabel(numero, JLabel.CENTER);
        statNumber.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 24));
        // statNumber.setForeground(new Color(30, 41, 59)); // #1e293b
        return statNumber;
    }

    private JPanel crearStatCardPanel(JLabel numberLabel, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE, getSECTION_STYLE());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel descLabel = new JLabel(description, JLabel.CENTER);
        descLabel.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 116, 139)); // #64748b
        descLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        card.add(numberLabel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);

        return card;
    }

  private void crearSeccionProductos() {
        setProductsPanel(new JPanel(new BorderLayout()));
        getProductsPanel().putClientProperty(FlatClientProperties.STYLE, getCARD_STYLE());
        getProductsPanel().setBorder(BorderFactory.createEmptyBorder(32, 32, 0, 32));

    // Header con filtros
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

    JLabel titulo = new JLabel("Productos para Recepción");
    titulo.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 24));

    JPanel filterTabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); // Menos espacio entre botones
    filterTabs.setOpaque(false);

    String[] filtros = {"Todos", "Pendientes", "Completos"};
    JButton[] botonesFiltro = new JButton[filtros.length];

    for (int i = 0; i < filtros.length; i++) {
        JButton tab = new JButton(filtros[i]);
        botonesFiltro[i] = tab;
        
        // Estilo base para todos los botones
        tab.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 12));
        tab.setFocusPainted(false);
        tab.setBorderPainted(true);
        tab.setContentAreaFilled(true);
        tab.setMargin(new Insets(6, 16, 6, 16)); // Padding interno
        
        // Estilo inicial: primer botón seleccionado
        if (i == 0) {
            // Botón seleccionado - estilo primario
            tab.setBackground(new Color(59, 130, 246)); // Azul más vibrante
            tab.setForeground(Color.WHITE);
            tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(37, 99, 235), 1), // Borde exterior
                BorderFactory.createEmptyBorder(6, 16, 6, 16) // Padding interno
            ));
        } else {
            // Botones no seleccionados - estilo secundario
            tab.setBackground(new Color(249, 250, 251)); // Fondo muy claro
            tab.setForeground(new Color(75, 85, 99)); // Texto gris oscuro
            tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1), // Borde gris claro
                BorderFactory.createEmptyBorder(6, 16, 6, 16) // Padding interno
            ));
        }

        // Efectos hover
        tab.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (tab.getBackground().equals(new Color(59, 130, 246))) {
                    // Si ya está seleccionado, hover más oscuro
                    tab.setBackground(new Color(37, 99, 235));
                } else {
                    // Hover para botones no seleccionados
                    tab.setBackground(new Color(243, 244, 246));
                }
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (tab == botonesFiltro[0] || 
                    (tab.getForeground().equals(Color.WHITE) && 
                     tab.getBackground().equals(new Color(37, 99, 235)))) {
                    // Restaurar color seleccionado
                    tab.setBackground(new Color(59, 130, 246));
                } else {
                    // Restaurar color no seleccionado
                    tab.setBackground(new Color(249, 250, 251));
                }
            }
        });

        // Añadir ActionListener para cada botón de filtro
        final String filtro = filtros[i];
        tab.addActionListener(e -> {
            // Aplicar filtro a la tabla
            aplicarFiltroProductos(filtro);
            
            // Actualizar estilos de TODOS los botones
            for (JButton btn : botonesFiltro) {
                if (btn == tab) {
                    // Botón seleccionado - estilo primario
                    btn.setBackground(new Color(59, 130, 246));
                    btn.setForeground(Color.WHITE);
                    btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(37, 99, 235), 1),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)
                    ));
                } else {
                    // Botones no seleccionados - estilo secundario
                    btn.setBackground(new Color(249, 250, 251));
                    btn.setForeground(new Color(75, 85, 99));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)
                    ));
                }
            }
        });

        filterTabs.add(tab);
    }

    header.add(titulo, BorderLayout.WEST);
    header.add(filterTabs, BorderLayout.EAST);

    // Tabla de productos
    crearTablaProductos();

    // Control de selección
    JPanel selectionPanel = new JPanel(new BorderLayout());
    selectionPanel.putClientProperty(FlatClientProperties.STYLE, getSECTION_STYLE());
    selectionPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        setChkSelectAll(new JCheckBox("Seleccionar todos los productos como recibidos"));
        getChkSelectAll().setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 14));

    JLabel selectionSummary = new JLabel("0 de 24 productos seleccionados");
    selectionSummary.setFont(new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 14));
    selectionSummary.setForeground(new Color(107, 114, 128));

    selectionPanel.add(getChkSelectAll(), BorderLayout.WEST);
    selectionPanel.add(selectionSummary, BorderLayout.EAST);

        getProductsPanel().add(header, BorderLayout.NORTH);
        getProductsPanel().add(new JScrollPane(getTablaProductos()), BorderLayout.CENTER);
        getProductsPanel().add(selectionPanel, BorderLayout.SOUTH);
}

   private void aplicarFiltroProductos(String tipoFiltro) {
    DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
    
    if (model.getRowCount() == 0) {
        return;
    }

    TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) getTablaProductos().getRowSorter();
    
    if (sorter == null) {
        sorter = new TableRowSorter<>(model);
            getTablaProductos().setRowSorter(sorter);
    }

    // Crear filtro según el tipo
    RowFilter<DefaultTableModel, Integer> filtro = null;
    
    switch (tipoFiltro) {
        case "Pendientes":
            filtro = RowFilter.regexFilter("(Pendiente|Parcial)", 4); // Columna 4 = Estado
            break;
        case "Completos":
            filtro = RowFilter.regexFilter("Recibido", 4); // Columna 4 = Estado
            break;
        case "Todos":
        default:
            filtro = null; // Mostrar todos
            break;
    }

    sorter.setRowFilter(filtro);
    actualizarResumenSeleccion();
}
// Método para actualizar el resumen de selección (debe considerar el filtro aplicado)

private void actualizarResumenSeleccion() {
    DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
    int totalFilas = model.getRowCount();
    int filasVisibles = getTablaProductos().getRowCount(); // Filas visibles después del filtro
    int seleccionados = 0;

    // Contar productos seleccionados en las filas visibles
    for (int i = 0; i < filasVisibles; i++) {
        int modelIndex = getTablaProductos().convertRowIndexToModel(i);
        Boolean seleccionado = (Boolean) model.getValueAt(modelIndex, 0);
        
        if (seleccionado != null && seleccionado) {
            seleccionados++;
        }
    }

    // Actualizar el texto del resumen
    JPanel selectionPanel = (JPanel) getProductsPanel().getComponent(2);
    Component[] components = selectionPanel.getComponents();
    
    for (Component comp : components) {
        if (comp instanceof JLabel && comp != getChkSelectAll()) {
            ((JLabel) comp).setText(seleccionados + " de " + filasVisibles + " productos seleccionados");
            break;
        }
    }
}

   private void crearTablaProductos() {
    String[] columnas = {"select", "Producto", "Enviado", "Recibido", "Estado", "Justificación", "Observaciones", "Tipo"};

    DefaultTableModel model = new DefaultTableModel(columnas, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            if (columnIndex == 2 || columnIndex == 3) {
                return Integer.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0 || column == 3 || column == 5 || column == 6 || column == 7;
        }
    };

        setTablaProductos(new JTable(model));
        getTablaProductos().setRowHeight(40);
        getTablaProductos().putClientProperty(FlatClientProperties.STYLE,
            "showHorizontalLines:true;showVerticalLines:false;rowHeight:40;intercellSpacing:10,5");

    // Configurar columnas
        getTablaProductos().getColumnModel().getColumn(0).setPreferredWidth(60);
        getTablaProductos().getColumnModel().getColumn(1).setPreferredWidth(350);
        getTablaProductos().getColumnModel().getColumn(2).setPreferredWidth(80);
        getTablaProductos().getColumnModel().getColumn(3).setPreferredWidth(100);
        getTablaProductos().getColumnModel().getColumn(4).setPreferredWidth(120);
        getTablaProductos().getColumnModel().getColumn(5).setPreferredWidth(150);
        getTablaProductos().getColumnModel().getColumn(6).setPreferredWidth(150);
        getTablaProductos().getColumnModel().getColumn(7).setPreferredWidth(100);

    // Inicializar RowSorter
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        getTablaProductos().setRowSorter(sorter);

    // Renderer personalizado para estado
        getTablaProductos().getColumnModel().getColumn(4).setCellRenderer(new EstadoCellRenderer());

    // Editor para Tipo (par/caja)
        javax.swing.JComboBox<String> tipoEditor = new javax.swing.JComboBox<>(new String[]{"par", "caja"});
        getTablaProductos().getColumnModel().getColumn(7).setCellEditor(new javax.swing.DefaultCellEditor(tipoEditor));

    // Centrar números
    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        getTablaProductos().getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        getTablaProductos().getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
}

    private void crearSeccionAcciones() {
        setActionsPanel(new JPanel(new BorderLayout()));
        getActionsPanel().putClientProperty(FlatClientProperties.STYLE, getCARD_STYLE());
        getActionsPanel().setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        // Observaciones
        JPanel obsPanel = new JPanel(new BorderLayout());
        obsPanel.setOpaque(false);
        obsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        JLabel obsLabel = new JLabel("Observaciones Generales de Recepción");
        obsLabel.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 14));
        // obsLabel.setForeground(new Color(55, 65, 81)); // #374151
        obsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        setTxtObservaciones(new JTextArea(3, 30));
        //txtObservaciones.putClientProperty(FlatClientProperties.STYLE, ";background:#ffffff;border:1,1,1,1,#d1d5db");
        getTxtObservaciones().setLineWrap(true);
        getTxtObservaciones().setWrapStyleWord(true);
        getTxtObservaciones().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane obsScroll = new JScrollPane(getTxtObservaciones());
        obsScroll.setBorder(null);
        obsScroll.setPreferredSize(new Dimension(0, 80));

        obsPanel.add(obsLabel, BorderLayout.NORTH);
        obsPanel.add(obsScroll, BorderLayout.CENTER);

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        buttonPanel.setOpaque(false);

        setBtnCancelar(new JButton("Cancelar"));
        getBtnCancelar().putClientProperty(FlatClientProperties.STYLE, getBUTTON_SECONDARY());
        getBtnCancelar().setPreferredSize(new Dimension(120, 40));

        setBtnConfirmar(new JButton("Confirmar Recepción"));
        getBtnConfirmar().putClientProperty(FlatClientProperties.STYLE, getBUTTON_PRIMARY());
        getBtnConfirmar().setPreferredSize(new Dimension(180, 40));
        getBtnConfirmar().setEnabled(false);

        buttonPanel.add(getBtnCancelar());
        buttonPanel.add(getBtnConfirmar());

        getActionsPanel().add(obsPanel, BorderLayout.CENTER);
        getActionsPanel().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        try {
            setProductosEnviados(obtenerProductosEnviados());
            DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();

            int totalEnviado = 0;
            int totalUnidades = 0;

            for (int i = 0; i < getProductosEnviados().size(); i++) {
                Map<String, Object> producto = getProductosEnviados().get(i);
                String descripcion = producto.get("descripcion_completa").toString();
                int cantidadEnviada = (Integer) producto.get("cantidad_enviada");
                String obs = (String) producto.get("observaciones");
                String tipoDefault = null;
                Object td = producto.get("tipo_detalle");
                if (td != null) tipoDefault = String.valueOf(td);
                if (tipoDefault == null || tipoDefault.trim().isEmpty()) {
                    tipoDefault = (obs != null && obs.contains("[TIPO_ENVIO=caja]")) ? "caja" : "par";
                }

                getCantidadesOriginales().put(i, cantidadEnviada);
                totalEnviado++;
                totalUnidades += cantidadEnviada;

                model.addRow(new Object[]{
                    false,
                    descripcion,
                    cantidadEnviada,
                    cantidadEnviada,
                    "Pendiente",
                    "Recepción completa",
                    "",
                    tipoDefault
                });
            }

            getLblTotalEnviado().setText(String.valueOf(totalEnviado));
            getLblTotalUnidades().setText(String.valueOf(totalUnidades));
            actualizarEstadisticas();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error cargando datos del traspaso: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Map<String, Object>> obtenerProductosEnviados() throws SQLException {
        String sql = "SELECT td.*, p.nombre as producto_nombre, "
                + "c.nombre as color_nombre, t.numero as talla_numero, pv.sku, "
                + "CONCAT(p.nombre, "
                + "CASE WHEN c.nombre IS NOT NULL THEN CONCAT(' - ', c.nombre) ELSE '' END, "
                + "CASE WHEN t.numero IS NOT NULL THEN CONCAT(' - Talla ', t.numero) ELSE '' END, "
                + "CASE WHEN pv.sku IS NOT NULL THEN CONCAT(' (', pv.sku, ')') ELSE '' END) as descripcion_completa "
                + "FROM traspaso_detalles td "
                + "INNER JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso "
                + "INNER JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE tr.numero_traspaso = ? AND td.cantidad_enviada > 0 "
                + "ORDER BY p.nombre";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, getNumeroTraspaso());
        ResultSet rs = stmt.executeQuery();

        List<Map<String, Object>> productos = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> producto = new HashMap<>();
            producto.put("id_detalle", rs.getInt("id_detalle_traspaso"));
            producto.put("id_producto", rs.getInt("id_producto"));
            producto.put("id_variante", rs.getObject("id_variante"));
            producto.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
            producto.put("cantidad_recibida", rs.getInt("cantidad_recibida"));
            producto.put("observaciones", rs.getString("observaciones"));
            producto.put("tipo_detalle", rs.getString("Tipo"));
            producto.put("descripcion_completa", rs.getString("descripcion_completa"));
            productos.add(producto);
        }

        rs.close();
        stmt.close();
        conn.close();

        return productos;
    }

    private void configurarEventos() {
        // Checkbox seleccionar todos
          getChkSelectAll().addActionListener(e -> {
        boolean seleccionar = getChkSelectAll().isSelected();
        DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
        
        // Seleccionar solo las filas visibles (considerando el filtro)
        int filasVisibles = getTablaProductos().getRowCount();
        
        for (int i = 0; i < filasVisibles; i++) {
            int modelIndex = getTablaProductos().convertRowIndexToModel(i);
            model.setValueAt(seleccionar, modelIndex, 0);
            
            if (seleccionar) {
                Integer enviado = (Integer) model.getValueAt(modelIndex, 2);
                model.setValueAt(enviado, modelIndex, 3);
                model.setValueAt("Recibido", modelIndex, 4);
            } else {
                model.setValueAt("Pendiente", modelIndex, 4);
            }
        }
        
        actualizarEstadisticas();
        validarBotonConfirmar();
        actualizarResumenSeleccion();
    });

        // Listener para cambios en la selección que actualice el resumen
        getTablaProductos().getModel().addTableModelListener(e -> {
            SwingUtilities.invokeLater(() -> {
                actualizarEstadisticas();
                validarBotonConfirmar();
                actualizarEstadoFila(e.getFirstRow());
                actualizarResumenSeleccion(); // Actualizar el resumen de selección
            });
        });

        // Botones
        getBtnCancelar().addActionListener(e -> {
            if (getCallback() != null) {
                getCallback().onRecepcionCancelada();
            }
            dispose();
        });

        getBtnConfirmar().addActionListener(e -> confirmarRecepcion());
    }

 private void actualizarEstadisticas() {
    DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
    int seleccionados = 0;
    int unidadesRecibir = 0;
    int filasVisibles = getTablaProductos().getRowCount();

    for (int i = 0; i < filasVisibles; i++) {
        int modelIndex = getTablaProductos().convertRowIndexToModel(i);
        Boolean seleccionado = (Boolean) model.getValueAt(modelIndex, 0);
        
        if (seleccionado != null && seleccionado) {
            seleccionados++;
            Integer cantidad = (Integer) model.getValueAt(modelIndex, 3);
            if (cantidad != null) {
                unidadesRecibir += cantidad;
            }
        }
    }

        getLblTotalSeleccionado().setText(String.valueOf(seleccionados));
        getLblUnidadesRecibir().setText(String.valueOf(unidadesRecibir));
}

    private void validarBotonConfirmar() {
        DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
        boolean haySeleccion = false;

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) model.getValueAt(i, 0);
            if (seleccionado != null && seleccionado) {
                haySeleccion = true;
                break;
            }
        }

        getBtnConfirmar().setEnabled(haySeleccion);
        if (!haySeleccion) {
            getBtnConfirmar().setText("Seleccione productos para continuar");
        } else {
            getBtnConfirmar().setText("Confirmar Recepción");
        }
    }

    private void actualizarEstadoFila(int row) {
        if (row < 0) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
        Boolean seleccionado = (Boolean) model.getValueAt(row, 0);

        if (seleccionado != null && seleccionado) {
            Integer enviado = (Integer) model.getValueAt(row, 2);
            Integer recibido = (Integer) model.getValueAt(row, 3);

            String estado;
            if (recibido == null || recibido == 0) {
                estado = "Pendiente";
            } else if (recibido.equals(enviado)) {
                estado = "Recibido";
            } else if (recibido < enviado) {
                estado = "Parcial";
            } else {
                estado = "Exceso";
                // Advertencia por exceso
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "La cantidad recibida no puede ser mayor a la enviada.",
                            "Advertencia", JOptionPane.WARNING_MESSAGE);
                    model.setValueAt(enviado, row, 3);
                    model.setValueAt("Recibido", row, 4);
                });
                return;
            }

            model.setValueAt(estado, row, 4);
        } else {
            model.setValueAt("Pendiente", row, 4);
        }
    }

    private void confirmarRecepcion() {
        if (!validarRecepcion()) {
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de confirmar la recepción del traspaso " + getNumeroTraspaso() + "?\n"
                + "Esta acción actualizará el estado del traspaso a RECIBIDO.",
                "Confirmar Recepción",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            procesarRecepcionBD();

            List<Map<String, Object>> productosRecibidos = obtenerProductosRecibidos();

            // NOTA: La notificación push a bodega ORIGEN se envía automáticamente
            // mediante el trigger BD (trg_traspasos_au_estado) que llama a
            // sp_notificar_traspaso_evento y envía la notificación solo a los
            // usuarios de la bodega origen, NO a la bodega destino que recibe.

            // MARCAR NOTIFICACIONES RELACIONADAS COMO LEÍDAS
            // Obtener el ID del traspaso para marcar sus notificaciones como leídas
            try {
                Integer idTraspaso = obtenerIdTraspasoPorNumero(getNumeroTraspaso());
                if (idTraspaso != null && idTraspaso > 0) {
                    raven.utils.NotificacionesService notifService = raven.utils.NotificacionesService.getInstance();
                    Integer idBodegaDestino = obtenerIdBodegaDestinoPorIdTraspaso(idTraspaso);
                    if (idBodegaDestino != null && idBodegaDestino > 0) {
                        notifService.marcarNotificacionesTraspasoComoLeidasParaBodega(idTraspaso, idBodegaDestino);
                    } else {
                        notifService.marcarNotificacionesTraspasoComoLeidas(idTraspaso);
                    }

                    System.out.println("INFO Notificaciones del traspaso ID: " + idTraspaso + " marcadas como leídas");
                }
            } catch (Exception e) {
                System.err.println("WARNING Error marcando notificaciones como leídas: " + e.getMessage());
                e.printStackTrace();
            }

            if (getCallback() != null) {
                getCallback().onRecepcionExitosa(getNumeroTraspaso(), productosRecibidos);
            }

            dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error procesando la recepción: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Obtiene el ID del traspaso a partir de su número
     */
    private Integer obtenerIdTraspasoPorNumero(String numeroTraspaso) {
        String sql = "SELECT id_traspaso FROM traspasos WHERE numero_traspaso = ?";

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_traspaso");
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("ERROR Obteniendo ID del traspaso: " + e.getMessage());
            e.printStackTrace();
        }

        return null; // No se encontró el traspaso
    }

    private Integer obtenerIdBodegaDestinoPorIdTraspaso(int idTraspaso) {
        String sql = "SELECT id_bodega_destino FROM traspasos WHERE id_traspaso = ?";
        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idTraspaso);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("ERROR Obteniendo bodega destino del traspaso: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private boolean validarRecepcion() {
        DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();

        boolean haySeleccion = false;
        StringBuilder errores = new StringBuilder();

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean recibir = (Boolean) model.getValueAt(i, 0);
            if (recibir != null && recibir) {
                haySeleccion = true;

                Integer recibido = (Integer) model.getValueAt(i, 3);
                Integer enviado = (Integer) model.getValueAt(i, 2);
                String justificacion = (String) model.getValueAt(i, 5);
                String producto = (String) model.getValueAt(i, 1);

                // Verificar justificación para cantidades diferentes
                if (!recibido.equals(enviado)) {
                    if (justificacion == null || justificacion.trim().isEmpty()
                            || "Recepción completa".equals(justificacion.trim())) {
                        errores.append("- ").append(producto.length() > 50
                                ? producto.substring(0, 50) + "..." : producto)
                                .append(": Requiere justificación\n");
                    }
                }
            }
        }

        if (!haySeleccion) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar al menos un producto para confirmar la recepción.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (errores.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Los siguientes productos requieren justificación:\n\n" + errores.toString(),
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

    private void procesarRecepcionBD() throws SQLException {
        Connection conn = conexion.getInstance().createConnection();
        conn.setAutoCommit(false);

        try {
            // Actualizar traspaso principal
            String sqlTraspaso = "UPDATE traspasos SET "
                    + "estado = 'recibido', "
                    + "fecha_recepcion = NOW(), "
                    + "id_usuario_recibe = ?, "
                    + "observaciones = CONCAT(COALESCE(observaciones, ''), ?) "
                    + "WHERE numero_traspaso = ?";

            PreparedStatement stmtTraspaso = conn.prepareStatement(sqlTraspaso);
            String obsRecepcion = getTxtObservaciones().getText().trim();
            int uid = resolveUsuarioValidoParaFK(conn, raven.controlador.admin.SessionManager.getInstance().getCurrentUserId());
            stmtTraspaso.setInt(1, uid);
            stmtTraspaso.setString(2, obsRecepcion.isEmpty() ? "" : " - Recepción: " + obsRecepcion);
            stmtTraspaso.setString(3, getNumeroTraspaso());
            stmtTraspaso.executeUpdate();
            stmtTraspaso.close();

            // Actualizar detalles de productos seleccionados
            DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();

            String sqlDetalle = "UPDATE traspaso_detalles SET "
                    + "cantidad_recibida = ?, "
                    + "estado_detalle = CASE "
                    + "    WHEN ? >= cantidad_enviada THEN 'recibido' "
                    + "    WHEN ? > 0 THEN 'parcial' "
                    + "    ELSE 'faltante' "
                    + "END, "
                    + "observaciones = CONCAT(COALESCE(observaciones, ''), ?) "
                    + "WHERE id_detalle_traspaso = ?";

            PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle);

            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean recibir = (Boolean) model.getValueAt(i, 0);

                if (recibir != null && recibir) {
                    Integer recibido = (Integer) model.getValueAt(i, 3);
                    String justificacion = (String) model.getValueAt(i, 5);
                    String observaciones = (String) model.getValueAt(i, 6);
                    int idDetalle = (Integer) getProductosEnviados().get(i).get("id_detalle");

                    // Combinar justificación y observaciones
                    StringBuilder obsCompletas = new StringBuilder();
                    if (justificacion != null && !justificacion.trim().isEmpty()
                            && !"Recepción completa".equals(justificacion.trim())) {
                        obsCompletas.append(" - Justificación: ").append(justificacion);
                    }
                    if (observaciones != null && !observaciones.trim().isEmpty()) {
                        obsCompletas.append(" - Obs: ").append(observaciones);
                    }

                    stmtDetalle.setInt(1, recibido);
                    stmtDetalle.setInt(2, recibido);
                    stmtDetalle.setInt(3, recibido);
                    stmtDetalle.setString(4, obsCompletas.toString());
                    stmtDetalle.setInt(5, idDetalle);
                    stmtDetalle.addBatch();
                }
            }

            stmtDetalle.executeBatch();
            stmtDetalle.close();

            // Actualizar inventario en bodega destino: sumar recibido y activar registros
            Integer idBodegaDestino = null;
            try (PreparedStatement psDest = conn.prepareStatement("SELECT id_bodega_destino FROM traspasos WHERE numero_traspaso = ?")) {
                psDest.setString(1, getNumeroTraspaso());
                try (ResultSet rsDest = psDest.executeQuery()) { if (rsDest.next()) idBodegaDestino = rsDest.getInt(1); }
            }

            if (idBodegaDestino != null && idBodegaDestino > 0) {
                try (PreparedStatement psExists = conn.prepareStatement(
                         "SELECT id_inventario_bodega FROM inventario_bodega WHERE id_bodega = ? AND id_variante = ? LIMIT 1");
                     PreparedStatement psUpdatePar = conn.prepareStatement(
                         "UPDATE inventario_bodega SET Stock_par = COALESCE(Stock_par,0) + ?, stock_reservado = GREATEST(COALESCE(stock_reservado,0) - ?, 0), activo = 1, fecha_ultimo_movimiento = NOW() WHERE id_bodega = ? AND id_variante = ?");
                     PreparedStatement psUpdateCaja = conn.prepareStatement(
                         "UPDATE inventario_bodega SET Stock_caja = COALESCE(Stock_caja,0) + ?, stock_reservado = GREATEST(COALESCE(stock_reservado,0) - ?, 0), activo = 1, fecha_ultimo_movimiento = NOW() WHERE id_bodega = ? AND id_variante = ?");
                     PreparedStatement psInsert = conn.prepareStatement(
                         "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, fecha_ultimo_movimiento, activo) VALUES (?,?,?,?,0,NOW(),1)")) {

                    
                    for (int i = 0; i < model.getRowCount(); i++) {
                        Boolean recibir = (Boolean) model.getValueAt(i, 0);
                        if (recibir == null || !recibir) continue;

                        Object vObj = getProductosEnviados().get(i).get("id_variante");
                        if (vObj == null) continue;
                        int idVariante = ((Number) vObj).intValue();
                        Integer recibido = (Integer) model.getValueAt(i, 3);
                        Integer enviado = (Integer) model.getValueAt(i, 2);
                        if (recibido == null || recibido <= 0) continue;

                        String tipoSeleccion = null;
                        try {
                            tipoSeleccion = (String) model.getValueAt(i, 7);
                        } catch (Exception ignore) {}
                        boolean esCaja = tipoSeleccion != null && tipoSeleccion.trim().equalsIgnoreCase("caja");
                        boolean esPar = !esCaja;

                        int idProducto = ((Number) getProductosEnviados().get(i).get("id_producto")).intValue();
                        int paresPorCaja = 1;
                        try (PreparedStatement psPpc = conn.prepareStatement("SELECT COALESCE(pares_por_caja,1) FROM productos WHERE id_producto = ?")) {
                            psPpc.setInt(1, idProducto);
                            try (ResultSet rsPpc = psPpc.executeQuery()) { if (rsPpc.next()) paresPorCaja = Math.max(1, rsPpc.getInt(1)); }
                        }
                        int cajasRecibir = (int) Math.ceil(recibido / (double) paresPorCaja);

                        psExists.setInt(1, idBodegaDestino);
                        psExists.setInt(2, idVariante);
                        try (ResultSet rs = psExists.executeQuery()) {
                            if (rs.next()) {
                            if (esPar) {
                                psUpdatePar.setInt(1, recibido);
                                psUpdatePar.setInt(2, enviado != null ? enviado : 0);
                                psUpdatePar.setInt(3, idBodegaDestino);
                                psUpdatePar.setInt(4, idVariante);
                                psUpdatePar.addBatch();
                            } else {
                                psUpdateCaja.setInt(1, cajasRecibir);
                                psUpdateCaja.setInt(2, enviado != null ? enviado : 0);
                                psUpdateCaja.setInt(3, idBodegaDestino);
                                psUpdateCaja.setInt(4, idVariante);
                                psUpdateCaja.addBatch();
                            }
                            } else {
                            psInsert.setInt(1, idBodegaDestino);
                            psInsert.setInt(2, idVariante);
                            if (esPar) {
                                psInsert.setInt(3, recibido);
                                psInsert.setInt(4, 0);
                            } else {
                                psInsert.setInt(3, 0);
                                psInsert.setInt(4, cajasRecibir);
                            }
                            psInsert.addBatch();
                            }
                        }
                    }

                    psUpdatePar.executeBatch();
                    psUpdateCaja.executeBatch();
                    psInsert.executeBatch();
                }
            }

            conn.commit();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    private int resolveUsuarioValidoParaFK(Connection con, int candidato) throws SQLException {
        if (candidato > 0 && existsUsuario(con, candidato)) return candidato;
        int sid = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();
        if (sid > 0 && existsUsuario(con, sid)) return sid;
        try (PreparedStatement ps = con.prepareStatement("SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario ASC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Usuario recibe inválido o inexistente");
    }

    private boolean existsUsuario(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM usuarios WHERE id_usuario = ? AND activo = 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Map<String, Object>> obtenerProductosRecibidos() {
        DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();
        List<Map<String, Object>> productosRecibidos = new ArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean recibir = (Boolean) model.getValueAt(i, 0);

            if (recibir != null && recibir) {
                Integer recibido = (Integer) model.getValueAt(i, 3);
                Map<String, Object> producto = new HashMap<>(getProductosEnviados().get(i));
                producto.put("cantidad_recibida_actual", recibido);
                producto.put("justificacion_recepcion", model.getValueAt(i, 5));
                producto.put("observaciones_recepcion", model.getValueAt(i, 6));
                productosRecibidos.add(producto);
            }
        }

        return productosRecibidos;
    }

    // Renderer personalizado para la columna Estado
    private class EstadoCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(JLabel.CENTER);

            if (value != null) {
                String estado = value.toString().toLowerCase();
                Color color;

                switch (estado) {
                    case "recibido":
                        color = new Color(220, 252, 231); // #dcfce7
                        label.setForeground(new Color(22, 163, 74));
                        break;
                    case "parcial":
                        color = new Color(254, 243, 199); // #fef3c7
                        label.setForeground(new Color(146, 64, 14));
                        break;
                    case "pendiente":
                        color = new Color(254, 226, 226); // #fee2e2
                        label.setForeground(new Color(153, 27, 27));
                        break;
                    default:
                        color = new Color(243, 244, 246); // #f3f4f6
                        label.setForeground(new Color(107, 114, 128));
                        break;
                }

                label.setOpaque(true);
                label.setBackground(color);
                label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            }

            return label;
        }
    }

    /**
     * Método estático para mostrar la modal de recepción
     */
    public static void mostrarModalRecepcion(Frame parent, String numeroTraspaso,
            RecepcionTraspaso.RecepcionCallback callback) {

        SwingUtilities.invokeLater(() -> {
            try {
                ModalRecepcionTraspaso modal = new ModalRecepcionTraspaso(
                        parent, numeroTraspaso, callback);
                modal.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent,
                        "Error al abrir la modal de recepción: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);

                if (callback != null) {
                    callback.onRecepcionCancelada();
                }
            }
        });
    }

    /**
     * Método para obtener información del traspaso
     */
    private void cargarInformacionTraspaso() {
        try {
            String sql = "SELECT t.*, bo.nombre as bodega_origen, bd.nombre as bodega_destino, "
                    + "u.nombre as usuario_solicita "
                    + "FROM traspasos t "
                    + "INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega "
                    + "INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega "
                    + "INNER JOIN usuarios u ON t.id_usuario_solicita = u.id_usuario "
                    + "WHERE t.numero_traspaso = ?";

            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, getNumeroTraspaso());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                setInfoTraspaso(new HashMap<>());
                getInfoTraspaso().put("bodega_origen", rs.getString("bodega_origen"));
                getInfoTraspaso().put("bodega_destino", rs.getString("bodega_destino"));
                getInfoTraspaso().put("usuario_solicita", rs.getString("usuario_solicita"));
                getInfoTraspaso().put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
                getInfoTraspaso().put("fecha_envio", rs.getTimestamp("fecha_envio"));
                getInfoTraspaso().put("estado", rs.getString("estado"));
                getInfoTraspaso().put("motivo", rs.getString("motivo"));
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error cargando información del traspaso: " + e.getMessage());
        }
    }

    /**
     * Actualiza la información mostrada en las cards
     */
    private void actualizarInfoCards() {
        if (getInfoTraspaso() != null) {
            // Aquí puedes actualizar los labels de las cards con la información real
            // Por simplicidad, se mantiene la información estática en el ejemplo

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");

            // La actualización se haría aquí si se quisiera información dinámica
            // Por ejemplo:
            // lblBodegaOrigen.setText(infoTraspaso.get("bodega_origen").toString());
            // lblBodegaDestino.setText(infoTraspaso.get("bodega_destino").toString());
            // etc.
        }
    }

    /**
     * Agrega animaciones suaves a la interfaz
     */
    private void aplicarAnimaciones() {
        // Timer para animaciones suaves
        javax.swing.Timer animationTimer = new javax.swing.Timer(100, new ActionListener() {
            private int step = 0;
            private final int maxSteps = 10;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (step < maxSteps) {
                    float alpha = (float) step / maxSteps;
                    getMainContainer().setOpaque(true);
                    step++;
                } else {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            }
        });

        // Inicia la animación cuando se muestra el modal
        SwingUtilities.invokeLater(() -> animationTimer.start());
    }

    /**
     * Valida los datos antes de confirmar
     */
    private boolean validarDatosCompletos() {
        DefaultTableModel model = (DefaultTableModel) getTablaProductos().getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) model.getValueAt(i, 0);
            if (seleccionado != null && seleccionado) {
                Integer cantidad = (Integer) model.getValueAt(i, 3);
                if (cantidad == null || cantidad < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Las cantidades deben ser números válidos no negativos.",
                            "Error de validación", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Limpia los recursos al cerrar
     */
    @Override
    public void dispose() {
        // Limpiar recursos si es necesario
        if (getProductosEnviados() != null) {
            getProductosEnviados().clear();
        }
        if (getCantidadesOriginales() != null) {
            getCantidadesOriginales().clear();
        }

        super.dispose();
    }

    /**
     * @return the CARD_STYLE
     */
    public static String getCARD_STYLE() {
        return CARD_STYLE;
    }

    /**
     * @param aCARD_STYLE the CARD_STYLE to set
     */
    public static void setCARD_STYLE(String aCARD_STYLE) {
        CARD_STYLE = aCARD_STYLE;
    }

    /**
     * @return the SECTION_STYLE
     */
    public static String getSECTION_STYLE() {
        return SECTION_STYLE;
    }

    /**
     * @param aSECTION_STYLE the SECTION_STYLE to set
     */
    public static void setSECTION_STYLE(String aSECTION_STYLE) {
        SECTION_STYLE = aSECTION_STYLE;
    }

    /**
     * @return the BUTTON_PRIMARY
     */
    public static String getBUTTON_PRIMARY() {
        return BUTTON_PRIMARY;
    }

    /**
     * @param aBUTTON_PRIMARY the BUTTON_PRIMARY to set
     */
    public static void setBUTTON_PRIMARY(String aBUTTON_PRIMARY) {
        BUTTON_PRIMARY = aBUTTON_PRIMARY;
    }

    /**
     * @return the BUTTON_SECONDARY
     */
    public static String getBUTTON_SECONDARY() {
        return BUTTON_SECONDARY;
    }

    /**
     * @param aBUTTON_SECONDARY the BUTTON_SECONDARY to set
     */
    public static void setBUTTON_SECONDARY(String aBUTTON_SECONDARY) {
        BUTTON_SECONDARY = aBUTTON_SECONDARY;
    }

    /**
     * @return the INPUT_STYLE
     */
    public static String getINPUT_STYLE() {
        return INPUT_STYLE;
    }

    /**
     * @param aINPUT_STYLE the INPUT_STYLE to set
     */
    public static void setINPUT_STYLE(String aINPUT_STYLE) {
        INPUT_STYLE = aINPUT_STYLE;
    }

    /**
     * @return the numeroTraspaso
     */
    public String getNumeroTraspaso() {
        return numeroTraspaso;
    }

    /**
     * @param numeroTraspaso the numeroTraspaso to set
     */
    public void setNumeroTraspaso(String numeroTraspaso) {
        this.numeroTraspaso = numeroTraspaso;
    }

    /**
     * @return the callback
     */
    public RecepcionTraspaso.RecepcionCallback getCallback() {
        return callback;
    }

    /**
     * @param callback the callback to set
     */
    public void setCallback(RecepcionTraspaso.RecepcionCallback callback) {
        this.callback = callback;
    }

    /**
     * @return the productosEnviados
     */
    public List<Map<String, Object>> getProductosEnviados() {
        return productosEnviados;
    }

    /**
     * @param productosEnviados the productosEnviados to set
     */
    public void setProductosEnviados(List<Map<String, Object>> productosEnviados) {
        this.productosEnviados = productosEnviados;
    }

    /**
     * @return the mainContainer
     */
    public JPanel getMainContainer() {
        return mainContainer;
    }

    /**
     * @param mainContainer the mainContainer to set
     */
    public void setMainContainer(JPanel mainContainer) {
        this.mainContainer = mainContainer;
    }

    /**
     * @return the headerPanel
     */
    public JPanel getHeaderPanel() {
        return headerPanel;
    }

    /**
     * @param headerPanel the headerPanel to set
     */
    public void setHeaderPanel(JPanel headerPanel) {
        this.headerPanel = headerPanel;
    }

    /**
     * @return the infoGridPanel
     */
    public JPanel getInfoGridPanel() {
        return infoGridPanel;
    }

    /**
     * @param infoGridPanel the infoGridPanel to set
     */
    public void setInfoGridPanel(JPanel infoGridPanel) {
        this.infoGridPanel = infoGridPanel;
    }

    /**
     * @return the timelinePanel
     */
    public JPanel getTimelinePanel() {
        return timelinePanel;
    }

    /**
     * @param timelinePanel the timelinePanel to set
     */
    public void setTimelinePanel(JPanel timelinePanel) {
        this.timelinePanel = timelinePanel;
    }

    /**
     * @return the statsPanel
     */
    public JPanel getStatsPanel() {
        return statsPanel;
    }

    /**
     * @param statsPanel the statsPanel to set
     */
    public void setStatsPanel(JPanel statsPanel) {
        this.statsPanel = statsPanel;
    }

    /**
     * @return the productsPanel
     */
    public JPanel getProductsPanel() {
        return productsPanel;
    }

    /**
     * @param productsPanel the productsPanel to set
     */
    public void setProductsPanel(JPanel productsPanel) {
        this.productsPanel = productsPanel;
    }

    /**
     * @return the actionsPanel
     */
    public JPanel getActionsPanel() {
        return actionsPanel;
    }

    /**
     * @param actionsPanel the actionsPanel to set
     */
    public void setActionsPanel(JPanel actionsPanel) {
        this.actionsPanel = actionsPanel;
    }

    /**
     * @return the tablaProductos
     */
    public JTable getTablaProductos() {
        return tablaProductos;
    }

    /**
     * @param tablaProductos the tablaProductos to set
     */
    public void setTablaProductos(JTable tablaProductos) {
        this.tablaProductos = tablaProductos;
    }

    /**
     * @return the chkSelectAll
     */
    public JCheckBox getChkSelectAll() {
        return chkSelectAll;
    }

    /**
     * @param chkSelectAll the chkSelectAll to set
     */
    public void setChkSelectAll(JCheckBox chkSelectAll) {
        this.chkSelectAll = chkSelectAll;
    }

    /**
     * @return the txtObservaciones
     */
    public JTextArea getTxtObservaciones() {
        return txtObservaciones;
    }

    /**
     * @param txtObservaciones the txtObservaciones to set
     */
    public void setTxtObservaciones(JTextArea txtObservaciones) {
        this.txtObservaciones = txtObservaciones;
    }

    /**
     * @return the btnConfirmar
     */
    public JButton getBtnConfirmar() {
        return btnConfirmar;
    }

    /**
     * @param btnConfirmar the btnConfirmar to set
     */
    public void setBtnConfirmar(JButton btnConfirmar) {
        this.btnConfirmar = btnConfirmar;
    }

    /**
     * @return the btnCancelar
     */
    public JButton getBtnCancelar() {
        return btnCancelar;
    }

    /**
     * @param btnCancelar the btnCancelar to set
     */
    public void setBtnCancelar(JButton btnCancelar) {
        this.btnCancelar = btnCancelar;
    }

    /**
     * @return the lblTotalEnviado
     */
    public JLabel getLblTotalEnviado() {
        return lblTotalEnviado;
    }

    /**
     * @param lblTotalEnviado the lblTotalEnviado to set
     */
    public void setLblTotalEnviado(JLabel lblTotalEnviado) {
        this.lblTotalEnviado = lblTotalEnviado;
    }

    /**
     * @return the lblTotalSeleccionado
     */
    public JLabel getLblTotalSeleccionado() {
        return lblTotalSeleccionado;
    }

    /**
     * @param lblTotalSeleccionado the lblTotalSeleccionado to set
     */
    public void setLblTotalSeleccionado(JLabel lblTotalSeleccionado) {
        this.lblTotalSeleccionado = lblTotalSeleccionado;
    }

    /**
     * @return the lblTotalUnidades
     */
    public JLabel getLblTotalUnidades() {
        return lblTotalUnidades;
    }

    /**
     * @param lblTotalUnidades the lblTotalUnidades to set
     */
    public void setLblTotalUnidades(JLabel lblTotalUnidades) {
        this.lblTotalUnidades = lblTotalUnidades;
    }

    /**
     * @return the lblUnidadesRecibir
     */
    public JLabel getLblUnidadesRecibir() {
        return lblUnidadesRecibir;
    }

    /**
     * @param lblUnidadesRecibir the lblUnidadesRecibir to set
     */
    public void setLblUnidadesRecibir(JLabel lblUnidadesRecibir) {
        this.lblUnidadesRecibir = lblUnidadesRecibir;
    }

    /**
     * @return the infoTraspaso
     */
    public Map<String, Object> getInfoTraspaso() {
        return infoTraspaso;
    }

    /**
     * @param infoTraspaso the infoTraspaso to set
     */
    public void setInfoTraspaso(Map<String, Object> infoTraspaso) {
        this.infoTraspaso = infoTraspaso;
    }

    /**
     * @return the cantidadesOriginales
     */
    public Map<Integer, Integer> getCantidadesOriginales() {
        return cantidadesOriginales;
    }

    /**
     * @param cantidadesOriginales the cantidadesOriginales to set
     */
    public void setCantidadesOriginales(Map<Integer, Integer> cantidadesOriginales) {
        this.cantidadesOriginales = cantidadesOriginales;
    }
}
