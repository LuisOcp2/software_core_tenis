package raven.controlador.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import raven.dao.CarritoDAO;
import raven.modelos.CarritoItem;
import raven.modelos.OrdenReserva;
import raven.modelos.OrdenReservaDetalle;
import raven.modelos.SesionUsuario;

/**
 * Módulo Carrito / Órdenes Web.
 *
 * UI/UX con soporte completo de tema oscuro/claro usando UIManager (FlatLaf).
 * Los colores se leen dinámicamente en cada render — no se hardcodean.
 * Sigue el mismo patrón de colores que traspasos y ConsultaDetallada.
 */
public class ModelCarritoOrdenesWeb extends JPanel {

    // ── Colores semánticos para estados (estos son de negocio, no de tema) ──
    private static final Color C_PENDING  = new Color(255, 193,   7);  // amarillo
    private static final Color C_RETIRED  = new Color( 23, 162, 184);  // cian
    private static final Color C_PAID     = new Color( 40, 167,  69);  // verde
    private static final Color C_FINISHED = new Color(102,  16, 242);  // violeta
    private static final Color C_CANCEL   = new Color(220,  53,  69);  // rojo
    private static final Color C_PRIMARY  = new Color( 67,  97, 238);  // azul

    // ── Fuentes ──────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font FONT_SUB     = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_NORMAL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD,  26);
    private static final Font FONT_KPI_LBL = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Formatters ───────────────────────────────────────────────────────────
    private static final NumberFormat   FMT_CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private static final DecimalFormat  FMT_NUM      = new DecimalFormat("#,##0");
    private static final SimpleDateFormat FMT_DATE   = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ── DAOs y datos ─────────────────────────────────────────────────────────
    private final CarritoDAO carritoDAO;
    private List<OrdenReserva> ordenesList  = new ArrayList<>();
    private List<CarritoItem>  carritoItems = new ArrayList<>();

    // ── Componentes UI principales ────────────────────────────────────────────
    private JTable tablaOrdenes;
    private DefaultTableModel modeloTabla;
    private JTable tablaCarrito;
    private DefaultTableModel modeloCarrito;
    private JTable tablaDetalleOrden;
    private DefaultTableModel modeloDetalle;

    private JTextField    txtBuscar;
    private JComboBox<String> cmbEstado;
    private JSpinner      spinFechaInicio;
    private JSpinner      spinFechaFin;

    // KPI Labels
    private JLabel lblKpiTotal, lblKpiPendientes, lblKpiPagados, lblKpiFinaliz;
    private JLabel lblKpiIngresos, lblKpiTicket;

    // CardLayout
    private CardLayout cardLayout;
    private JPanel     cardPanel;

    private OrdenReserva ordenSeleccionada;
    private final Integer idBodegaFiltro;

    // ── Helpers de color adaptados al tema ────────────────────────────────────
    /** Color de fondo del panel/formulario actual según el tema de FlatLaf */
    private static Color bgPanel()      { return UIManager.getColor("Panel.background"); }
    /** Fondo de componentes de entrada (campos, combobox) */
    private static Color bgInput()      { return UIManager.getColor("TextField.background"); }
    /** Color de texto principal */
    private static Color fgText()       { return UIManager.getColor("Label.foreground"); }
    /** Color de texto secundario/muted */
    private static Color fgMuted()      { Color c = UIManager.getColor("Label.disabledForeground"); return c != null ? c : new Color(108,117,125); }
    /** Borde sutil entre componentes */
    private static Color borderColor()  { Color c = UIManager.getColor("Component.borderColor"); return c != null ? c : new Color(200,200,200); }
    /** Fondo de la cabecera de tablas */
    private static Color bgTableHeader(){ Color c = UIManager.getColor("TableHeader.background"); return c != null ? c : new Color(245,245,245); }
    /** Fondo alterno de filas pares */
    private static Color bgTableEven()  { Color c = UIManager.getColor("Table.background"); return c != null ? c : Color.WHITE; }
    /** Fondo alterno de filas impares */
    private static Color bgTableOdd()   { Color c = UIManager.getColor("Table.alternateRowColor"); return c != null ? c : new Color(248,249,252); }
    /** Fondo de fila seleccionada */
    private static Color bgTableSel()   { Color c = UIManager.getColor("Table.selectionBackground"); return c != null ? c : new Color(67,97,238,80); }

    // =========================================================================
    public ModelCarritoOrdenesWeb() { this(null); }

    public ModelCarritoOrdenesWeb(Integer idBodega) {
        this.idBodegaFiltro = idBodega;
        this.carritoDAO = new CarritoDAO();
        initUI();
        cargarDatosIniciales();
    }

    // =========================================================================
    // INICIALIZACIÓN DE LA INTERFAZ
    // =========================================================================

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        // Fondo adaptativo: deja que FlatLaf maneje el fondo raíz
        putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");

        add(crearHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");

        cardPanel.add(crearPanelListaOrdenes(), "LISTA");
        cardPanel.add(crearPanelDetalleOrden(), "DETALLE");
        cardPanel.add(crearPanelCarrito(),      "CARRITO");

        add(cardPanel, BorderLayout.CENTER);
        cardLayout.show(cardPanel, "LISTA");
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.putClientProperty(FlatClientProperties.STYLE,
            "background: $TitlePane.background; border: 0,0,1,0,$Component.borderColor");
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        JLabel iconLabel = new JLabel("\uD83D\uDED2"); // 🛒
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        JLabel titulo = new JLabel("Carrito / Órdenes Web");
        titulo.setFont(FONT_TITLE);
        titulo.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.foreground");
        titlePanel.add(iconLabel);
        titlePanel.add(titulo);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnCarrito  = crearBoton("\uD83D\uDED2 Ver Carrito", C_RETIRED,  Color.WHITE);
        JButton btnRefrescar= crearBoton("\uD83D\uDD04 Actualizar",  new Color(108,117,125), Color.WHITE);
        JButton btnVolver   = crearBoton("\u25C4 Volver a Lista",    C_PRIMARY,  Color.WHITE);
        btnVolver.setVisible(false);

        btnCarrito.addActionListener(e -> {
            cardLayout.show(cardPanel, "CARRITO");
            cargarCarrito();
            btnVolver.setVisible(true);
            btnCarrito.setVisible(false);
        });
        btnRefrescar.addActionListener(e -> cargarDatosIniciales());
        btnVolver.addActionListener(e -> {
            cardLayout.show(cardPanel, "LISTA");
            btnVolver.setVisible(false);
            btnCarrito.setVisible(true);
            cargarDatosIniciales();
        });

        btnPanel.add(btnCarrito);
        btnPanel.add(btnRefrescar);
        btnPanel.add(btnVolver);
        header.add(btnPanel, BorderLayout.EAST);
        return header;
    }

    // ── PANEL LISTA DE ÓRDENES ────────────────────────────────────────────────
    private JPanel crearPanelListaOrdenes() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.add(crearPanelKPIs(),       BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(crearPanelFiltros(),       BorderLayout.NORTH);
        centro.add(crearPanelTablaOrdenes(),  BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);
        return panel;
    }

    // KPI Cards
    private JPanel crearPanelKPIs() {
        JPanel panel = new JPanel(new GridLayout(1, 6, 10, 0));
        panel.setOpaque(false);

        lblKpiTotal      = new JLabel("0");
        lblKpiPendientes = new JLabel("0");
        lblKpiPagados    = new JLabel("0");
        lblKpiFinaliz    = new JLabel("0");
        lblKpiIngresos   = new JLabel("$0");
        lblKpiTicket     = new JLabel("$0");

        panel.add(crearKpiCard("\uD83D\uDCCB Total",       lblKpiTotal,      C_PRIMARY));
        panel.add(crearKpiCard("\u23F3 Pendientes",        lblKpiPendientes, C_PENDING));
        panel.add(crearKpiCard("\u2705 Pagados",           lblKpiPagados,    C_PAID));
        panel.add(crearKpiCard("\uD83C\uDFC1 Finalizados", lblKpiFinaliz,    C_FINISHED));
        panel.add(crearKpiCard("\uD83D\uDCB0 Ingresos",    lblKpiIngresos,   C_RETIRED));
        panel.add(crearKpiCard("\uD83C\uDF9F Ticket Prom.",lblKpiTicket,     new Color(108,117,125)));
        return panel;
    }

    private JPanel crearKpiCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        // Fondo adaptativo via FlatLaf property
        card.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor(), 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        // Barra accent en la parte superior
        JPanel topBar = new JPanel();
        topBar.setBackground(accentColor);
        topBar.setPreferredSize(new Dimension(0, 4));
        card.add(topBar, BorderLayout.NORTH);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_KPI_LBL);
        lbl.setForeground(fgMuted());
        card.add(lbl, BorderLayout.SOUTH);

        valueLabel.setFont(FONT_KPI_VAL);
        valueLabel.setForeground(accentColor);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // Filtros
    private JPanel crearPanelFiltros() {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor(), 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filtros.setOpaque(false);

        txtBuscar = new JTextField();
        txtBuscar.setColumns(20);
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "\uD83D\uDD0D  Buscar por cliente, # orden...");
        txtBuscar.setFont(FONT_NORMAL);
        txtBuscar.setPreferredSize(new Dimension(220, 32));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filtrarTabla(); }
        });

        cmbEstado = new JComboBox<>(new String[]{"Todos","pendiente","retirado","pagado","finalizado","cancelado"});
        cmbEstado.setFont(FONT_NORMAL);
        cmbEstado.setPreferredSize(new Dimension(150, 32));
        cmbEstado.addActionListener(e -> cargarOrdenes());

        Calendar cal = Calendar.getInstance();
        SpinnerDateModel modelInicio = new SpinnerDateModel();
        SpinnerDateModel modelFin    = new SpinnerDateModel();
        spinFechaInicio = new JSpinner(modelInicio);
        spinFechaFin    = new JSpinner(modelFin);
        spinFechaInicio.setEditor(new JSpinner.DateEditor(spinFechaInicio, "dd/MM/yyyy"));
        spinFechaFin.setEditor(new JSpinner.DateEditor(spinFechaFin,       "dd/MM/yyyy"));
        spinFechaInicio.setPreferredSize(new Dimension(110, 32));
        spinFechaFin.setPreferredSize(new Dimension(110, 32));
        spinFechaFin.setValue(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        spinFechaInicio.setValue(cal.getTime());
        spinFechaInicio.addChangeListener(e -> cargarOrdenes());
        spinFechaFin.addChangeListener(e -> cargarOrdenes());

        JButton btnBuscar  = crearBoton("Buscar",  C_PRIMARY, Color.WHITE);
        JButton btnLimpiar = crearBoton("Limpiar", new Color(108,117,125), Color.WHITE);
        btnBuscar.addActionListener(e -> cargarOrdenes());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            cmbEstado.setSelectedIndex(0);
            cargarOrdenes();
        });

        JLabel lbBusq  = new JLabel("Búsqueda:");  lbBusq.setFont(FONT_NORMAL);
        JLabel lbEst   = new JLabel("Estado:");    lbEst.setFont(FONT_NORMAL);
        JLabel lbDesde = new JLabel("Desde:");     lbDesde.setFont(FONT_NORMAL);
        JLabel lbHasta = new JLabel("Hasta:");     lbHasta.setFont(FONT_NORMAL);

        filtros.add(lbBusq);  filtros.add(txtBuscar);
        filtros.add(Box.createHorizontalStrut(4));
        filtros.add(lbEst);   filtros.add(cmbEstado);
        filtros.add(Box.createHorizontalStrut(4));
        filtros.add(lbDesde); filtros.add(spinFechaInicio);
        filtros.add(lbHasta); filtros.add(spinFechaFin);
        filtros.add(btnBuscar); filtros.add(btnLimpiar);
        card.add(filtros, BorderLayout.CENTER);
        return card;
    }

    // Tabla de órdenes
    private JPanel crearPanelTablaOrdenes() {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        card.setBorder(BorderFactory.createLineBorder(borderColor(), 1, true));

        // Encabezado
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.putClientProperty(FlatClientProperties.STYLE,
            "background: $TableHeader.background");
        tableHeader.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lblTabTitle = new JLabel("\uD83D\uDCCB  Listado de Órdenes Web");
        lblTabTitle.setFont(FONT_SUB);
        tableHeader.add(lblTabTitle, BorderLayout.WEST);
        card.add(tableHeader, BorderLayout.NORTH);

        String[] cols = {"#", "# Orden", "Cliente", "Bodega", "Fecha",
                         "Productos", "Total", "Método Pago", "Estado", "Acciones"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 9; }
        };
        tablaOrdenes = new JTable(modeloTabla);
        configurarTabla(tablaOrdenes);

        tablaOrdenes.getColumnModel().getColumn(8).setCellRenderer(new EstadoBadgeRenderer());
        tablaOrdenes.getColumnModel().getColumn(9).setCellRenderer(new AccionesCellRenderer());
        tablaOrdenes.getColumnModel().getColumn(9).setCellEditor(new AccionesCellEditor());

        int[] widths = {40, 70, 160, 130, 145, 80, 120, 120, 110, 130};
        for (int i = 0; i < widths.length; i++)
            tablaOrdenes.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        tablaOrdenes.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaOrdenes.getSelectedRow() >= 0) {
                    int fila = tablaOrdenes.getSelectedRow();
                    if (fila < ordenesList.size()) mostrarDetalleOrden(ordenesList.get(fila));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tablaOrdenes);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.putClientProperty(FlatClientProperties.STYLE, "background: $TableHeader.background");
        footer.setBorder(new EmptyBorder(4, 14, 4, 14));
        JLabel lblInfo = new JLabel("Doble clic en una orden para ver el detalle");
        lblInfo.setFont(FONT_SMALL);
        lblInfo.setForeground(fgMuted());
        footer.add(lblInfo);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    // ── PANEL DETALLE ─────────────────────────────────────────────────────────
    private JPanel crearPanelDetalleOrden() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Detalle de la Orden");
        lblTitulo.setFont(FONT_TITLE);
        topPanel.add(lblTitulo, BorderLayout.WEST);

        JButton btnVolverLista = crearBoton("\u25C4 Volver", new Color(108,117,125), Color.WHITE);
        btnVolverLista.addActionListener(e -> {
            cardLayout.show(cardPanel, "LISTA");
            cargarDatosIniciales();
        });
        topPanel.add(btnVolverLista, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(6);
        split.setResizeWeight(0.35);
        split.setOpaque(false);
        split.setTopComponent(crearInfoOrden());
        split.setBottomComponent(crearTablaProductosOrden());
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearInfoOrden() {
        JPanel card = new JPanel(new GridBagLayout());
        card.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor(), 1, true),
            new EmptyBorder(16, 20, 16, 20)
        ));
        JLabel lbl = new JLabel("Selecciona una orden para ver el detalle");
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(fgMuted());
        card.add(lbl);
        card.setName("infoOrden");
        return card;
    }

    private JPanel crearTablaProductosOrden() {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        card.setBorder(BorderFactory.createLineBorder(borderColor(), 1, true));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.putClientProperty(FlatClientProperties.STYLE, "background: $TableHeader.background");
        hdr.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lbl = new JLabel("\uD83D\uDCE6  Productos de la Orden");
        lbl.setFont(FONT_SUB);
        hdr.add(lbl, BorderLayout.WEST);
        card.add(hdr, BorderLayout.NORTH);

        String[] cols = {"#","Producto","Código","Talla","Color","Cant.","Precio Unit.","Subtotal","Estado"};
        modeloDetalle = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDetalleOrden = new JTable(modeloDetalle);
        configurarTabla(tablaDetalleOrden);
        tablaDetalleOrden.getColumnModel().getColumn(8).setCellRenderer(new EstadoBadgeRenderer());

        int[] w2 = {40,180,100,70,80,60,110,110,100};
        for (int i = 0; i < w2.length; i++)
            tablaDetalleOrden.getColumnModel().getColumn(i).setPreferredWidth(w2[i]);

        JScrollPane scroll = new JScrollPane(tablaDetalleOrden);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── PANEL CARRITO ─────────────────────────────────────────────────────────
    private JPanel crearPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel titulo = new JLabel("\uD83D\uDED2  Mi Carrito de Compras");
        titulo.setFont(FONT_TITLE);
        titulo.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(titulo, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        tableCard.setBorder(BorderFactory.createLineBorder(borderColor(), 1, true));

        String[] cols = {"#","Producto","Código","Talla","Color","Precio","Cantidad","Subtotal","Stock","Quitar"};
        modeloCarrito = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6 || c == 9; }
            @Override public Class<?> getColumnClass(int c) { return c == 6 ? Integer.class : Object.class; }
        };
        tablaCarrito = new JTable(modeloCarrito);
        configurarTabla(tablaCarrito);

        int[] cw = {35,170,90,65,80,110,80,110,70,80};
        for (int i = 0; i < cw.length; i++)
            tablaCarrito.getColumnModel().getColumn(i).setPreferredWidth(cw[i]);

        JScrollPane scroll = new JScrollPane(tablaCarrito);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        tableCard.add(scroll, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Footer del carrito
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.putClientProperty(FlatClientProperties.STYLE,
            "background: $Table.background; arc: 8");
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor(), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        totalPanel.setOpaque(false);
        JLabel lblTotalText  = new JLabel("Total del carrito:");
        lblTotalText.setFont(FONT_SUB);
        JLabel lblTotalValor = new JLabel("$0");
        lblTotalValor.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTotalValor.setForeground(C_PAID);
        totalPanel.add(lblTotalText);
        totalPanel.add(lblTotalValor);
        bottomPanel.add(totalPanel, BorderLayout.WEST);

        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botonesPanel.setOpaque(false);
        JButton btnVaciar  = crearBoton("\uD83D\uDDD1 Vaciar Carrito", C_CANCEL, Color.WHITE);
        JButton btnOrdenar = crearBoton("\u2705 Crear Orden",          C_PAID,   Color.WHITE);

        btnVaciar.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "\u00BFVaciar todo el carrito?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int uid = SesionUsuario.getInstance().getIdUsuario();
                try {
                    carritoDAO.vaciarCarrito(uid);
                    cargarCarrito();
                    lblTotalValor.setText("$0");
                    mostrarToast("Carrito vaciado correctamente", new Color(108,117,125));
                } catch (SQLException ex) {
                    mostrarError("Error al vaciar el carrito", ex);
                }
            }
        });
        btnOrdenar.addActionListener(e -> mostrarDialogoCrearOrden(lblTotalValor));
        botonesPanel.add(btnVaciar);
        botonesPanel.add(btnOrdenar);
        bottomPanel.add(botonesPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        modeloCarrito.addTableModelListener(ev -> {
            if (ev.getColumn() == 6) {
                double t = 0;
                for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                    try {
                        int cant = Integer.parseInt(modeloCarrito.getValueAt(i, 6).toString());
                        double precio = carritoItems.get(i).getPrecioUnitario();
                        t += cant * precio;
                    } catch (Exception ex) { /* ignorar */ }
                }
                lblTotalValor.setText(FMT_CURRENCY.format(t));
            }
        });
        return panel;
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================

    private void cargarDatosIniciales() { cargarOrdenes(); cargarKPIs(); }

    private void cargarOrdenes() {
        new SwingWorker<List<OrdenReserva>, Void>() {
            @Override protected List<OrdenReserva> doInBackground() throws Exception {
                String estado = cmbEstado != null ? (String) cmbEstado.getSelectedItem() : null;
                Date inicio   = spinFechaInicio != null ? (Date) spinFechaInicio.getValue() : null;
                Date fin      = spinFechaFin    != null ? (Date) spinFechaFin.getValue()    : null;
                return carritoDAO.obtenerTodasLasOrdenes(idBodegaFiltro, estado, inicio, fin);
            }
            @Override protected void done() {
                try { ordenesList = get(); actualizarTablaOrdenes(); }
                catch (Exception e) { mostrarError("Error al cargar órdenes", e); }
            }
        }.execute();
    }

    private void cargarKPIs() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                Date fin = new Date();
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                return carritoDAO.obtenerEstadisticasOrdenes(cal.getTime(), fin, idBodegaFiltro);
            }
            @Override protected void done() {
                try {
                    Map<String, Object> stats = get();
                    if (lblKpiTotal == null) return;
                    lblKpiTotal.setText(FMT_NUM.format(stats.getOrDefault("total_ordenes", 0)));
                    lblKpiPendientes.setText(FMT_NUM.format(stats.getOrDefault("pendientes", 0)));
                    lblKpiPagados.setText(FMT_NUM.format(stats.getOrDefault("pagados", 0)));
                    lblKpiFinaliz.setText(FMT_NUM.format(stats.getOrDefault("finalizados", 0)));
                    lblKpiIngresos.setText(FMT_CURRENCY.format(((Number) stats.getOrDefault("total_ingresos", 0)).doubleValue()));
                    lblKpiTicket.setText(FMT_CURRENCY.format(((Number) stats.getOrDefault("ticket_promedio", 0)).doubleValue()));
                } catch (Exception e) { /* silencioso */ }
            }
        }.execute();
    }

    private void cargarCarrito() {
        int uid = SesionUsuario.getInstance().getIdUsuario();
        new SwingWorker<List<CarritoItem>, Void>() {
            @Override protected List<CarritoItem> doInBackground() throws Exception {
                return carritoDAO.obtenerCarritoPorUsuario(uid);
            }
            @Override protected void done() {
                try { carritoItems = get(); actualizarTablaCarrito(); }
                catch (Exception e) { mostrarError("Error al cargar carrito", e); }
            }
        }.execute();
    }

    private void actualizarTablaOrdenes() {
        modeloTabla.setRowCount(0);
        int num = 1;
        for (OrdenReserva o : ordenesList) {
            modeloTabla.addRow(new Object[]{
                num++,
                "#" + o.getIdOrden(),
                o.getNombreUsuario()  != null ? o.getNombreUsuario()  : "—",
                o.getNombreBodega()   != null ? o.getNombreBodega()   : "—",
                o.getFechaCreacion()  != null ? FMT_DATE.format(o.getFechaCreacion()) : "—",
                o.getCantidadProductos() + " item(s)",
                FMT_CURRENCY.format(o.getTotal()),
                o.getMetodoPago() != null ? o.getMetodoPago() : "—",
                o.getEstado()     != null ? o.getEstado()     : "—",
                "Acciones"
            });
        }
    }

    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        int num = 1;
        for (CarritoItem item : carritoItems) {
            modeloCarrito.addRow(new Object[]{
                num++,
                item.getNombreProducto(),
                item.getCodigoModelo(),
                item.getTalla(),
                item.getColor(),
                FMT_CURRENCY.format(item.getPrecioUnitario()),
                item.getCantidad(),
                FMT_CURRENCY.format(item.getSubtotal()),
                item.getStockDisponible(),
                "Quitar"
            });
        }
    }

    private void filtrarTabla() {
        String texto = txtBuscar.getText().toLowerCase().trim();
        if (texto.isEmpty()) { actualizarTablaOrdenes(); return; }
        modeloTabla.setRowCount(0);
        int num = 1;
        for (OrdenReserva o : ordenesList) {
            boolean match =
                (o.getNombreUsuario() != null && o.getNombreUsuario().toLowerCase().contains(texto)) ||
                String.valueOf(o.getIdOrden()).contains(texto) ||
                (o.getCiudad() != null && o.getCiudad().toLowerCase().contains(texto)) ||
                (o.getEstado() != null && o.getEstado().toLowerCase().contains(texto));
            if (match) {
                modeloTabla.addRow(new Object[]{
                    num++, "#"+o.getIdOrden(),
                    o.getNombreUsuario() != null ? o.getNombreUsuario() : "—",
                    o.getNombreBodega()  != null ? o.getNombreBodega()  : "—",
                    o.getFechaCreacion() != null ? FMT_DATE.format(o.getFechaCreacion()) : "—",
                    o.getCantidadProductos() + " item(s)",
                    FMT_CURRENCY.format(o.getTotal()),
                    o.getMetodoPago() != null ? o.getMetodoPago() : "—",
                    o.getEstado()     != null ? o.getEstado()     : "—",
                    "Acciones"
                });
            }
        }
    }

    // =========================================================================
    // ACCIONES
    // =========================================================================

    private void mostrarDetalleOrden(OrdenReserva orden) {
        this.ordenSeleccionada = orden;
        cardLayout.show(cardPanel, "DETALLE");
        new SwingWorker<List<OrdenReservaDetalle>, Void>() {
            @Override protected List<OrdenReservaDetalle> doInBackground() throws Exception {
                return carritoDAO.obtenerDetallesOrden(orden.getIdOrden());
            }
            @Override protected void done() {
                try {
                    List<OrdenReservaDetalle> detalles = get();
                    modeloDetalle.setRowCount(0);
                    int num = 1;
                    for (OrdenReservaDetalle d : detalles) {
                        modeloDetalle.addRow(new Object[]{
                            num++,
                            d.getNombreProducto(), d.getCodigoProducto(),
                            d.getTalla(), d.getColor(), d.getCantidad(),
                            FMT_CURRENCY.format(d.getPrecio()),
                            FMT_CURRENCY.format(d.getSubtotal()),
                            d.getEstado() != null ? d.getEstado() : "PENDIENTE"
                        });
                    }
                } catch (Exception e) { mostrarError("Error al cargar detalle", e); }
            }
        }.execute();
    }

    private void mostrarDialogoCambiarEstado(OrdenReserva orden) {
        String[] estados = {"pendiente","retirado","pagado","finalizado","cancelado"};
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));
        JLabel lbl = new JLabel("Orden #" + orden.getIdOrden() + " — Estado: " + orden.getEstado());
        lbl.setFont(FONT_SUB);
        panel.add(lbl);
        JComboBox<String> cmbNuevo = new JComboBox<>(estados);
        cmbNuevo.setSelectedItem(orden.getEstado());
        cmbNuevo.setFont(FONT_NORMAL);
        panel.add(new JLabel("Nuevo estado:"));
        panel.add(cmbNuevo);
        JTextField txtMotivo = new JTextField();
        txtMotivo.setFont(FONT_NORMAL);
        panel.add(new JLabel("Motivo (cancelaciones):"));
        panel.add(txtMotivo);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Cambiar Estado", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String nuevoEstado = (String) cmbNuevo.getSelectedItem();
            if (nuevoEstado != null && !nuevoEstado.equals(orden.getEstado())) {
                new SwingWorker<Boolean, Void>() {
                    @Override protected Boolean doInBackground() throws Exception {
                        if ("cancelado".equals(nuevoEstado))
                            return carritoDAO.cancelarOrden(orden.getIdOrden(), txtMotivo.getText().trim());
                        return carritoDAO.actualizarEstadoOrden(orden.getIdOrden(), nuevoEstado);
                    }
                    @Override protected void done() {
                        try {
                            if (get()) { mostrarToast("Estado actualizado: " + nuevoEstado, C_PAID); cargarDatosIniciales(); }
                            else         mostrarToast("No se pudo actualizar", C_CANCEL);
                        } catch (Exception e) { mostrarError("Error al actualizar estado", e); }
                    }
                }.execute();
            }
        }
    }

    private void mostrarDialogoCrearOrden(JLabel lblTotalValor) {
        if (carritoItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío.", "Carrito vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(new EmptyBorder(8, 4, 8, 4));
        JTextField txtDireccion = new JTextField(); txtDireccion.setFont(FONT_NORMAL);
        JTextField txtCiudad    = new JTextField(); txtCiudad.setFont(FONT_NORMAL);
        JTextField txtDepto     = new JTextField(); txtDepto.setFont(FONT_NORMAL);
        JTextField txtNotas     = new JTextField(); txtNotas.setFont(FONT_NORMAL);
        JComboBox<String> cmbMetodo = new JComboBox<>(new String[]{"efectivo","transferencia","tarjeta","contraentrega"});
        cmbMetodo.setFont(FONT_NORMAL);

        form.add(new JLabel("Dirección:"));     form.add(txtDireccion);
        form.add(new JLabel("Ciudad:"));        form.add(txtCiudad);
        form.add(new JLabel("Departamento:")); form.add(txtDepto);
        form.add(new JLabel("Método pago:"));  form.add(cmbMetodo);
        form.add(new JLabel("Notas:"));         form.add(txtNotas);

        double total = carritoItems.stream().mapToDouble(CarritoItem::getSubtotal).sum();
        double iva   = total * 0.19;
        JLabel lblResumen = new JLabel(String.format(
            "<html><b>%d ítem(s)</b> &nbsp;·&nbsp; Sub: %s &nbsp;·&nbsp; IVA 19%%: %s &nbsp;·&nbsp; <b>Total: %s</b></html>",
            carritoItems.size(), FMT_CURRENCY.format(total),
            FMT_CURRENCY.format(iva), FMT_CURRENCY.format(total + iva)
        ));
        lblResumen.setFont(FONT_SMALL);
        lblResumen.setBorder(new EmptyBorder(8,10,8,10));
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.add(form, BorderLayout.CENTER);
        container.add(lblResumen, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, container,
            "Crear Orden de Reserva", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            if (txtDireccion.getText().trim().isEmpty() || txtCiudad.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Dirección y ciudad son obligatorios.",
                    "Campos requeridos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int uid     = SesionUsuario.getInstance().getIdUsuario();
            int idBodega = idBodegaFiltro != null ? idBodegaFiltro :
                (carritoItems.get(0).getIdBodega() != null ? carritoItems.get(0).getIdBodega() : 1);
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() throws Exception {
                    return carritoDAO.convertirCarritoAOrden(
                        uid, idBodega, (String) cmbMetodo.getSelectedItem(),
                        txtNotas.getText().trim(), txtDireccion.getText().trim(),
                        txtCiudad.getText().trim(), txtDepto.getText().trim());
                }
                @Override protected void done() {
                    try {
                        int idOrden = get();
                        if (idOrden > 0) {
                            mostrarToast("\u2705 Orden #" + idOrden + " creada", C_PAID);
                            carritoItems.clear();
                            actualizarTablaCarrito();
                            lblTotalValor.setText(FMT_CURRENCY.format(0));
                            cardLayout.show(cardPanel, "LISTA");
                            cargarDatosIniciales();
                        } else mostrarToast("Error al crear la orden", C_CANCEL);
                    } catch (Exception e) { mostrarError("Error al crear orden", e); }
                }
            }.execute();
        }
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private void configurarTabla(JTable table) {
        table.setFont(FONT_NORMAL);
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(borderColor());
        table.setSelectionBackground(bgTableSel());
        table.setSelectionForeground(fgText());
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true");

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BADGE);
        header.setBackground(bgTableHeader());
        header.setForeground(fgMuted());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor()));
        header.setReorderingAllowed(false);

        // Renderer alternado adaptativo al tema
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? bgTableEven() : bgTableOdd());
                    c.setForeground(fgText());
                }
                ((JLabel) c).setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
    }

    private JButton crearBoton(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto);
        btn.setFont(FONT_BADGE);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE,
            FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        return btn;
    }

    private void mostrarToast(String mensaje, Color color) {
        Window window = SwingUtilities.getWindowAncestor(this);
        JWindow toast = new JWindow(window);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(color);
        content.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel lbl = new JLabel(mensaje);
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(Color.WHITE);
        content.add(lbl, BorderLayout.CENTER);
        toast.getContentPane().add(content);
        toast.pack();
        if (window != null) {
            toast.setLocation(
                window.getX() + window.getWidth()  - toast.getWidth()  - 20,
                window.getY() + window.getHeight() - toast.getHeight() - 50);
        }
        toast.setVisible(true);
        new Timer(3000, ev -> toast.dispose()).start();
    }

    private void mostrarError(String mensaje, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
            mensaje + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    // =========================================================================
    // RENDERERS
    // =========================================================================

    /** Badge de estado con colores semánticos — fondo oscuro/claro adaptativo */
    private class EstadoBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = new JLabel(value != null ? value.toString() : "");
            lbl.setFont(FONT_BADGE);
            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBorder(new EmptyBorder(3, 10, 3, 10));

            if (isSelected) {
                lbl.setBackground(bgTableSel());
                lbl.setForeground(fgText());
                return lbl;
            }

            // Detectar si el tema actual es oscuro
            boolean dark = isDarkTheme();

            String estado = value != null ? value.toString().toLowerCase() : "";
            switch (estado) {
                case "pendiente":
                    lbl.setBackground(dark ? new Color(80,60,0)   : new Color(255,249,224));
                    lbl.setForeground(dark ? new Color(255,200,0) : new Color(180,130,0));
                    break;
                case "retirado":
                    lbl.setBackground(dark ? new Color(0,55,80)   : new Color(224,242,254));
                    lbl.setForeground(dark ? new Color(0,180,220) : new Color(2,132,199));
                    break;
                case "pagado":
                    lbl.setBackground(dark ? new Color(0,55,20)   : new Color(220,252,231));
                    lbl.setForeground(dark ? new Color(0,200,80)  : new Color(21,128,61));
                    break;
                case "finalizado":
                    lbl.setBackground(dark ? new Color(50,0,100)  : new Color(237,233,254));
                    lbl.setForeground(dark ? new Color(180,130,255): new Color(109,40,217));
                    break;
                case "cancelado":
                    lbl.setBackground(dark ? new Color(80,0,0)    : new Color(254,226,226));
                    lbl.setForeground(dark ? new Color(255,100,100): new Color(185,28,28));
                    break;
                default:
                    lbl.setBackground(row % 2 == 0 ? bgTableEven() : bgTableOdd());
                    lbl.setForeground(fgMuted());
            }
            return lbl;
        }
    }

    /** Detecta si el tema actual de FlatLaf es oscuro */
    private static boolean isDarkTheme() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) return false;
        // Luminancia: si es < 128 → oscuro
        double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return lum < 128;
    }

    private class AccionesCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            p.setBackground(isSelected ? bgTableSel() : (row % 2 == 0 ? bgTableEven() : bgTableOdd()));
            JButton btnVer    = new JButton("Ver");
            JButton btnEstado = new JButton("Estado");
            btnVer.setFont(FONT_SMALL);    btnEstado.setFont(FONT_SMALL);
            btnVer.setForeground(Color.WHITE); btnEstado.setForeground(Color.WHITE);
            btnVer.setBackground(C_RETIRED);   btnEstado.setBackground(C_PENDING.darker());
            btnVer.setOpaque(true);  btnVer.setBorderPainted(false);
            btnEstado.setOpaque(true); btnEstado.setBorderPainted(false);
            btnVer.setPreferredSize(new Dimension(48, 24));
            btnEstado.setPreferredSize(new Dimension(64, 24));
            p.add(btnVer); p.add(btnEstado);
            return p;
        }
    }

    private class AccionesCellEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton btnVer, btnEstado;
        private int currentRow = -1;

        public AccionesCellEditor() {
            super(new JCheckBox());
            setClickCountToStart(1);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            panel.setOpaque(true);
            btnVer    = new JButton("Ver");
            btnEstado = new JButton("Estado");
            btnVer.setFont(FONT_SMALL);    btnEstado.setFont(FONT_SMALL);
            btnVer.setForeground(Color.WHITE); btnEstado.setForeground(Color.WHITE);
            btnVer.setBackground(C_RETIRED);   btnEstado.setBackground(C_PENDING.darker());
            btnVer.setOpaque(true);  btnVer.setBorderPainted(false);
            btnEstado.setOpaque(true); btnEstado.setBorderPainted(false);
            btnVer.setPreferredSize(new Dimension(48, 24));
            btnEstado.setPreferredSize(new Dimension(64, 24));

            btnVer.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < ordenesList.size())
                    mostrarDetalleOrden(ordenesList.get(currentRow));
            });
            btnEstado.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < ordenesList.size())
                    mostrarDialogoCambiarEstado(ordenesList.get(currentRow));
            });
            panel.add(btnVer); panel.add(btnEstado);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {
            currentRow = row;
            panel.setBackground(bgTableSel());
            return panel;
        }
        @Override public Object getCellEditorValue() { return "Acciones"; }
    }
}
