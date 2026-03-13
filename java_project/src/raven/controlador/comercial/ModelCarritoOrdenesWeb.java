package raven.controlador.comercial;

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
 * Módulo Carrito / Órdenes Web — UI/UX de nivel profesional.
 * Arquitectura: Panel principal con CardLayout + sidebar de filtros + tabla enriquecida.
 *
 * Funcionalidades:
 * 1. Dashboard de estadísticas (KPI cards)
 * 2. Tabla de órdenes con filtros avanzados (estado, fecha, búsqueda)
 * 3. Vista detalle de orden con productos
 * 4. Gestión de carrito temporal (agregar, modificar, eliminar ítems)
 * 5. Conversión carrito → orden de reserva
 * 6. Cambio de estado de órdenes con confirmación
 * 7. Búsqueda en tiempo real
 */
public class ModelCarritoOrdenesWeb extends JPanel {

    // ── Paleta de colores ────────────────────────────────────────────────────
    private static final Color COLOR_BG          = new Color(245, 247, 250);
    private static final Color COLOR_WHITE        = Color.WHITE;
    private static final Color COLOR_PRIMARY      = new Color(67, 97, 238);
    private static final Color COLOR_PRIMARY_DARK = new Color(50, 75, 195);
    private static final Color COLOR_SUCCESS      = new Color(40, 167, 69);
    private static final Color COLOR_WARNING      = new Color(255, 193, 7);
    private static final Color COLOR_DANGER       = new Color(220, 53, 69);
    private static final Color COLOR_INFO         = new Color(23, 162, 184);
    private static final Color COLOR_SECONDARY    = new Color(108, 117, 125);
    private static final Color COLOR_FINALIZED    = new Color(102, 16, 242);
    private static final Color COLOR_TEXT_DARK    = new Color(33, 37, 41);
    private static final Color COLOR_TEXT_MUTED   = new Color(108, 117, 125);
    private static final Color COLOR_BORDER       = new Color(222, 226, 230);
    private static final Color COLOR_ROW_HOVER    = new Color(232, 240, 255);
    private static final Color COLOR_ROW_ALT      = new Color(249, 250, 252);

    // ── Fuentes ──────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_SUB     = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_NORMAL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_MONO    = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_KPI_LBL = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Formatters ───────────────────────────────────────────────────────────
    private static final NumberFormat FMT_CURRENCY =
        NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private static final DecimalFormat FMT_NUM = new DecimalFormat("#,##0");
    private static final SimpleDateFormat FMT_DATE = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ── DAOs y datos ─────────────────────────────────────────────────────────
    private final CarritoDAO carritoDAO;
    private List<OrdenReserva> ordenesList = new ArrayList<>();
    private List<CarritoItem> carritoItems = new ArrayList<>();

    // ── Componentes UI principales ────────────────────────────────────────────
    private JTable tablaOrdenes;
    private DefaultTableModel modeloTabla;
    private JTable tablaCarrito;
    private DefaultTableModel modeloCarrito;
    private JTable tablaDetalleOrden;
    private DefaultTableModel modeloDetalle;

    private JTextField txtBuscar;
    private JComboBox<String> cmbEstado;
    private JComboBox<String> cmbBodega;
    private JSpinner spinFechaInicio;
    private JSpinner spinFechaFin;

    // KPI Labels
    private JLabel lblKpiTotal, lblKpiPendientes, lblKpiPagados, lblKpiFinaliz;
    private JLabel lblKpiIngresos, lblKpiTicket;

    // Panel principal con CardLayout
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // Orden seleccionada para detalle
    private OrdenReserva ordenSeleccionada;

    // Estado de carga
    private final Integer idBodegaFiltro;

    public ModelCarritoOrdenesWeb() {
        this(null);
    }

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
        setBackground(COLOR_BG);

        // Header
        add(crearHeader(), BorderLayout.NORTH);

        // Contenido central con CardLayout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(COLOR_BG);

        // Card: Lista de órdenes
        cardPanel.add(crearPanelListaOrdenes(), "LISTA");
        // Card: Detalle de orden
        cardPanel.add(crearPanelDetalleOrden(), "DETALLE");
        // Card: Carrito
        cardPanel.add(crearPanelCarrito(), "CARRITO");

        add(cardPanel, BorderLayout.CENTER);
        cardLayout.show(cardPanel, "LISTA");
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
            new EmptyBorder(14, 20, 14, 20)
        ));

        // Título
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        JLabel iconLabel = new JLabel("🛒");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        JLabel titulo = new JLabel("Carrito / Órdenes Web");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(COLOR_TEXT_DARK);
        titlePanel.add(iconLabel);
        titlePanel.add(titulo);
        header.add(titlePanel, BorderLayout.WEST);

        // Botones de acción
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnCarrito = crearBoton("🛒 Ver Carrito", COLOR_INFO, COLOR_WHITE);
        JButton btnRefrescar = crearBoton("🔄 Actualizar", COLOR_SECONDARY, COLOR_WHITE);
        JButton btnVolver = crearBoton("◀ Volver a Lista", COLOR_PRIMARY, COLOR_WHITE);
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
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // KPI Cards
        panel.add(crearPanelKPIs(), BorderLayout.NORTH);

        // Contenido central: filtros + tabla
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(crearPanelFiltros(), BorderLayout.NORTH);
        centro.add(crearPanelTablaOrdenes(), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        return panel;
    }

    // KPI Cards
    private JPanel crearPanelKPIs() {
        JPanel panel = new JPanel(new GridLayout(1, 6, 10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        lblKpiTotal     = new JLabel("0");
        lblKpiPendientes = new JLabel("0");
        lblKpiPagados   = new JLabel("0");
        lblKpiFinaliz   = new JLabel("0");
        lblKpiIngresos  = new JLabel("$0");
        lblKpiTicket    = new JLabel("$0");

        panel.add(crearKpiCard("📋 Total",      lblKpiTotal,     COLOR_PRIMARY));
        panel.add(crearKpiCard("⏳ Pendientes", lblKpiPendientes, COLOR_WARNING));
        panel.add(crearKpiCard("✅ Pagados",    lblKpiPagados,   COLOR_SUCCESS));
        panel.add(crearKpiCard("🏁 Finalizados",lblKpiFinaliz,   COLOR_FINALIZED));
        panel.add(crearKpiCard("💰 Ingresos",   lblKpiIngresos,  COLOR_INFO));
        panel.add(crearKpiCard("🎫 Ticket Prom.",lblKpiTicket,   COLOR_SECONDARY));

        return panel;
    }

    private JPanel crearKpiCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(COLOR_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        // Barra de color en la parte superior
        JPanel topBar = new JPanel();
        topBar.setBackground(accentColor);
        topBar.setPreferredSize(new Dimension(0, 4));
        card.add(topBar, BorderLayout.NORTH);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_KPI_LBL);
        lbl.setForeground(COLOR_TEXT_MUTED);
        card.add(lbl, BorderLayout.SOUTH);

        valueLabel.setFont(FONT_KPI_VAL);
        valueLabel.setForeground(accentColor);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // Panel de filtros
    private JPanel crearPanelFiltros() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COLOR_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filtros.setOpaque(false);

        // Búsqueda
        txtBuscar = crearTextField("🔍  Buscar por cliente, # orden, ciudad...", 220);
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                filtrarTabla();
            }
        });

        // Estado
        cmbEstado = new JComboBox<>(new String[]{"Todos","pendiente","retirado","pagado","finalizado","cancelado"});
        cmbEstado.setFont(FONT_NORMAL);
        cmbEstado.setPreferredSize(new Dimension(150, 32));
        cmbEstado.addActionListener(e -> cargarOrdenes());

        // Fechas
        SpinnerDateModel modelInicio = new SpinnerDateModel();
        SpinnerDateModel modelFin    = new SpinnerDateModel();
        spinFechaInicio = new JSpinner(modelInicio);
        spinFechaFin    = new JSpinner(modelFin);
        JSpinner.DateEditor edInicio = new JSpinner.DateEditor(spinFechaInicio, "dd/MM/yyyy");
        JSpinner.DateEditor edFin    = new JSpinner.DateEditor(spinFechaFin,    "dd/MM/yyyy");
        spinFechaInicio.setEditor(edInicio);
        spinFechaFin.setEditor(edFin);
        spinFechaInicio.setPreferredSize(new Dimension(110, 32));
        spinFechaFin.setPreferredSize(new Dimension(110, 32));

        // Fechas por defecto: inicio del mes hasta hoy
        Calendar cal = Calendar.getInstance();
        spinFechaFin.setValue(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        spinFechaInicio.setValue(cal.getTime());

        spinFechaInicio.addChangeListener(e -> cargarOrdenes());
        spinFechaFin.addChangeListener(e -> cargarOrdenes());

        JButton btnBuscar = crearBoton("Buscar", COLOR_PRIMARY, COLOR_WHITE);
        btnBuscar.addActionListener(e -> cargarOrdenes());

        JButton btnLimpiar = crearBoton("Limpiar", COLOR_SECONDARY, COLOR_WHITE);
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            cmbEstado.setSelectedIndex(0);
            cargarOrdenes();
        });

        filtros.add(new JLabel("Búsqueda:"));
        filtros.add(txtBuscar);
        filtros.add(Box.createHorizontalStrut(4));
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbEstado);
        filtros.add(Box.createHorizontalStrut(4));
        filtros.add(new JLabel("Desde:"));
        filtros.add(spinFechaInicio);
        filtros.add(new JLabel("Hasta:"));
        filtros.add(spinFechaFin);
        filtros.add(btnBuscar);
        filtros.add(btnLimpiar);

        card.add(filtros, BorderLayout.CENTER);
        return card;
    }

    // Panel tabla de órdenes
    private JPanel crearPanelTablaOrdenes() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COLOR_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(0, 0, 0, 0)
        ));

        // Encabezado de la tabla
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(new Color(248, 249, 250));
        tableHeader.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lblTabTitle = new JLabel("📋  Listado de Órdenes Web");
        lblTabTitle.setFont(FONT_SUB);
        lblTabTitle.setForeground(COLOR_TEXT_DARK);
        tableHeader.add(lblTabTitle, BorderLayout.WEST);
        card.add(tableHeader, BorderLayout.NORTH);

        // Columnas
        String[] cols = {"#", "# Orden", "Cliente", "Bodega", "Fecha",
                         "Productos", "Total", "Método Pago", "Estado", "Acciones"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 9; }
        };
        tablaOrdenes = new JTable(modeloTabla);
        configurarTabla(tablaOrdenes);

        // Renderizador de estado (badge)
        tablaOrdenes.getColumnModel().getColumn(8).setCellRenderer(new EstadoBadgeRenderer());

        // Renderizador de acciones
        tablaOrdenes.getColumnModel().getColumn(9).setCellRenderer(new AccionesCellRenderer());
        tablaOrdenes.getColumnModel().getColumn(9).setCellEditor(new AccionesCellEditor());

        // Anchos de columna
        int[] widths = {40, 70, 160, 120, 130, 80, 110, 110, 100, 120};
        for (int i = 0; i < widths.length; i++) {
            tablaOrdenes.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Doble click → detalle
        tablaOrdenes.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaOrdenes.getSelectedRow() >= 0) {
                    int fila = tablaOrdenes.getSelectedRow();
                    if (fila < ordenesList.size()) {
                        mostrarDetalleOrden(ordenesList.get(fila));
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tablaOrdenes);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_WHITE);
        card.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(new Color(248, 249, 250));
        footer.setBorder(new EmptyBorder(4, 14, 4, 14));
        JLabel lblInfo = new JLabel("Doble clic en una orden para ver el detalle");
        lblInfo.setFont(FONT_SMALL);
        lblInfo.setForeground(COLOR_TEXT_MUTED);
        footer.add(lblInfo);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    // ── PANEL DETALLE DE ORDEN ────────────────────────────────────────────────
    private JPanel crearPanelDetalleOrden() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Título del detalle
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Detalle de la Orden");
        lblTitulo.setFont(FONT_TITLE);
        lblTitulo.setForeground(COLOR_TEXT_DARK);
        topPanel.add(lblTitulo, BorderLayout.WEST);

        JButton btnVolverLista = crearBoton("◀ Volver", COLOR_SECONDARY, COLOR_WHITE);
        btnVolverLista.addActionListener(e -> {
            cardLayout.show(cardPanel, "LISTA");
            cargarDatosIniciales();
        });
        topPanel.add(btnVolverLista, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        // Info de la orden + tabla productos
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
        card.setBackground(COLOR_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(16, 20, 16, 20)
        ));
        // Placeholder — se rellena en mostrarDetalleOrden()
        JLabel lbl = new JLabel("Selecciona una orden para ver el detalle");
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(COLOR_TEXT_MUTED);
        card.add(lbl);
        card.setName("infoOrden");
        return card;
    }

    private JPanel crearTablaProductosOrden() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COLOR_WHITE);
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(248, 249, 250));
        hdr.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lbl = new JLabel("📦  Productos de la Orden");
        lbl.setFont(FONT_SUB);
        hdr.add(lbl, BorderLayout.WEST);
        card.add(hdr, BorderLayout.NORTH);

        String[] cols = {"#", "Producto", "Código", "Talla", "Color", "Cant.", "Precio Unit.", "Subtotal", "Estado"};
        modeloDetalle = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDetalleOrden = new JTable(modeloDetalle);
        configurarTabla(tablaDetalleOrden);
        tablaDetalleOrden.getColumnModel().getColumn(8).setCellRenderer(new EstadoBadgeRenderer());

        int[] widths2 = {40, 180, 100, 70, 80, 60, 110, 110, 100};
        for (int i = 0; i < widths2.length; i++) {
            tablaDetalleOrden.getColumnModel().getColumn(i).setPreferredWidth(widths2[i]);
        }

        JScrollPane scroll = new JScrollPane(tablaDetalleOrden);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── PANEL CARRITO ─────────────────────────────────────────────────────────
    private JPanel crearPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel titulo = new JLabel("🛒  Mi Carrito de Compras");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(COLOR_TEXT_DARK);
        titulo.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(titulo, BorderLayout.NORTH);

        // Tabla carrito
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(COLOR_WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));

        String[] cols = {"#", "Producto", "Código", "Talla", "Color", "Precio", "Cantidad", "Subtotal", "Stock", "Quitar"};
        modeloCarrito = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6 || c == 9; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 6) ? Integer.class : Object.class;
            }
        };
        tablaCarrito = new JTable(modeloCarrito);
        configurarTabla(tablaCarrito);

        int[] cw = {35, 170, 90, 65, 80, 100, 80, 100, 70, 80};
        for (int i = 0; i < cw.length; i++) {
            tablaCarrito.getColumnModel().getColumn(i).setPreferredWidth(cw[i]);
        }

        JScrollPane scroll = new JScrollPane(tablaCarrito);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        tableCard.add(scroll, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Panel inferior: total + botones
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(COLOR_WHITE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        totalPanel.setOpaque(false);
        JLabel lblTotalText = new JLabel("Total del carrito:");
        lblTotalText.setFont(FONT_SUB);
        JLabel lblTotalValor = new JLabel("$0");
        lblTotalValor.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTotalValor.setForeground(COLOR_SUCCESS);
        totalPanel.add(lblTotalText);
        totalPanel.add(lblTotalValor);
        bottomPanel.add(totalPanel, BorderLayout.WEST);

        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botonesPanel.setOpaque(false);
        JButton btnVaciar = crearBoton("🗑 Vaciar Carrito", COLOR_DANGER, COLOR_WHITE);
        JButton btnOrdenar = crearBoton("✅ Crear Orden", COLOR_SUCCESS, COLOR_WHITE);

        btnVaciar.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "¿Vaciar todo el carrito?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int uid = SesionUsuario.getInstance().getIdUsuario();
                try {
                    carritoDAO.vaciarCarrito(uid);
                    cargarCarrito();
                    lblTotalValor.setText("$0");
                    mostrarToast("Carrito vaciado correctamente", COLOR_SECONDARY);
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

        // Actualizar total al cambiar cantidades
        modeloCarrito.addTableModelListener(ev -> {
            if (ev.getColumn() == 6) {
                double t = 0;
                for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                    try {
                        int cant = Integer.parseInt(modeloCarrito.getValueAt(i, 6).toString());
                        double precio = Double.parseDouble(modeloCarrito.getValueAt(i, 5).toString()
                            .replace("$", "").replace(",", "").replace(".", "").trim()) / 100.0;
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

    private void cargarDatosIniciales() {
        cargarOrdenes();
        cargarKPIs();
    }

    private void cargarOrdenes() {
        SwingWorker<List<OrdenReserva>, Void> worker = new SwingWorker<List<OrdenReserva>, Void>() {
            @Override
            protected List<OrdenReserva> doInBackground() throws Exception {
                String estado = cmbEstado != null ?
                    (String) cmbEstado.getSelectedItem() : null;
                Date inicio = spinFechaInicio != null ?
                    (Date) spinFechaInicio.getValue() : null;
                Date fin = spinFechaFin != null ?
                    (Date) spinFechaFin.getValue() : null;
                return carritoDAO.obtenerTodasLasOrdenes(
                    idBodegaFiltro, estado, inicio, fin);
            }

            @Override
            protected void done() {
                try {
                    ordenesList = get();
                    actualizarTablaOrdenes();
                } catch (Exception e) {
                    mostrarError("Error al cargar órdenes", e);
                }
            }
        };
        worker.execute();
    }

    private void cargarKPIs() {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Date fin = new Date();
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date inicio = cal.getTime();
                return carritoDAO.obtenerEstadisticasOrdenes(inicio, fin, idBodegaFiltro);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> stats = get();
                    if (lblKpiTotal != null) {
                        lblKpiTotal.setText(FMT_NUM.format(stats.getOrDefault("total_ordenes", 0)));
                        lblKpiPendientes.setText(FMT_NUM.format(stats.getOrDefault("pendientes", 0)));
                        lblKpiPagados.setText(FMT_NUM.format(stats.getOrDefault("pagados", 0)));
                        lblKpiFinaliz.setText(FMT_NUM.format(stats.getOrDefault("finalizados", 0)));
                        lblKpiIngresos.setText(FMT_CURRENCY.format(
                            ((Number) stats.getOrDefault("total_ingresos", 0)).doubleValue()));
                        lblKpiTicket.setText(FMT_CURRENCY.format(
                            ((Number) stats.getOrDefault("ticket_promedio", 0)).doubleValue()));
                    }
                } catch (Exception e) { /* silencioso */ }
            }
        };
        worker.execute();
    }

    private void cargarCarrito() {
        int uid = SesionUsuario.getInstance().getIdUsuario();
        SwingWorker<List<CarritoItem>, Void> worker = new SwingWorker<List<CarritoItem>, Void>() {
            @Override
            protected List<CarritoItem> doInBackground() throws Exception {
                return carritoDAO.obtenerCarritoPorUsuario(uid);
            }
            @Override
            protected void done() {
                try {
                    carritoItems = get();
                    actualizarTablaCarrito();
                } catch (Exception e) {
                    mostrarError("Error al cargar carrito", e);
                }
            }
        };
        worker.execute();
    }

    private void actualizarTablaOrdenes() {
        modeloTabla.setRowCount(0);
        int num = 1;
        for (OrdenReserva o : ordenesList) {
            modeloTabla.addRow(new Object[]{
                num++,
                "#" + o.getIdOrden(),
                o.getNombreUsuario() != null ? o.getNombreUsuario() : "—",
                o.getNombreBodega() != null ? o.getNombreBodega() : "—",
                o.getFechaCreacion() != null ? FMT_DATE.format(o.getFechaCreacion()) : "—",
                o.getCantidadProductos() + " item(s)",
                FMT_CURRENCY.format(o.getTotal()),
                o.getMetodoPago() != null ? o.getMetodoPago() : "—",
                o.getEstado() != null ? o.getEstado() : "—",
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
        if (texto.isEmpty()) {
            actualizarTablaOrdenes();
            return;
        }
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
                    num++,
                    "#" + o.getIdOrden(),
                    o.getNombreUsuario() != null ? o.getNombreUsuario() : "—",
                    o.getNombreBodega() != null ? o.getNombreBodega() : "—",
                    o.getFechaCreacion() != null ? FMT_DATE.format(o.getFechaCreacion()) : "—",
                    o.getCantidadProductos() + " item(s)",
                    FMT_CURRENCY.format(o.getTotal()),
                    o.getMetodoPago() != null ? o.getMetodoPago() : "—",
                    o.getEstado() != null ? o.getEstado() : "—",
                    "Acciones"
                });
            }
        }
    }

    // =========================================================================
    // ACCIONES DE USUARIO
    // =========================================================================

    private void mostrarDetalleOrden(OrdenReserva orden) {
        this.ordenSeleccionada = orden;
        cardLayout.show(cardPanel, "DETALLE");

        // Reconstruir panel de info de la orden
        Component[] comps = cardPanel.getComponents();
        for (Component comp : comps) {
            if (comp instanceof JPanel) {
                JPanel card = (JPanel) comp;
                // Buscar panel de info
            }
        }

        // Cargar detalles de productos
        SwingWorker<List<OrdenReservaDetalle>, Void> worker =
            new SwingWorker<List<OrdenReservaDetalle>, Void>() {
            @Override
            protected List<OrdenReservaDetalle> doInBackground() throws Exception {
                return carritoDAO.obtenerDetallesOrden(orden.getIdOrden());
            }
            @Override
            protected void done() {
                try {
                    List<OrdenReservaDetalle> detalles = get();
                    modeloDetalle.setRowCount(0);
                    int num = 1;
                    for (OrdenReservaDetalle d : detalles) {
                        modeloDetalle.addRow(new Object[]{
                            num++,
                            d.getNombreProducto(),
                            d.getCodigoProducto(),
                            d.getTalla(),
                            d.getColor(),
                            d.getCantidad(),
                            FMT_CURRENCY.format(d.getPrecio()),
                            FMT_CURRENCY.format(d.getSubtotal()),
                            d.getEstado() != null ? d.getEstado() : "PENDIENTE"
                        });
                    }
                } catch (Exception e) {
                    mostrarError("Error al cargar detalle", e);
                }
            }
        };
        worker.execute();
    }

    private void mostrarDialogoCambiarEstado(OrdenReserva orden) {
        String[] estados = {"pendiente", "retirado", "pagado", "finalizado", "cancelado"};
        String estadoActual = orden.getEstado();

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));
        JLabel lbl = new JLabel("Orden #" + orden.getIdOrden() + " — Estado actual: " + estadoActual);
        lbl.setFont(FONT_SUB);
        panel.add(lbl);

        JComboBox<String> cmbNuevoEstado = new JComboBox<>(estados);
        cmbNuevoEstado.setSelectedItem(estadoActual);
        cmbNuevoEstado.setFont(FONT_NORMAL);
        panel.add(new JLabel("Nuevo estado:"));
        panel.add(cmbNuevoEstado);

        JTextField txtMotivo = new JTextField();
        txtMotivo.setFont(FONT_NORMAL);
        panel.add(new JLabel("Motivo (para cancelaciones):"));
        panel.add(txtMotivo);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Cambiar Estado de Orden",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String nuevoEstado = (String) cmbNuevoEstado.getSelectedItem();
            if (nuevoEstado != null && !nuevoEstado.equals(estadoActual)) {
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        if ("cancelado".equals(nuevoEstado)) {
                            return carritoDAO.cancelarOrden(orden.getIdOrden(), txtMotivo.getText().trim());
                        }
                        return carritoDAO.actualizarEstadoOrden(orden.getIdOrden(), nuevoEstado);
                    }
                    @Override
                    protected void done() {
                        try {
                            boolean ok = get();
                            if (ok) {
                                mostrarToast("Estado actualizado a: " + nuevoEstado, COLOR_SUCCESS);
                                cargarDatosIniciales();
                            } else {
                                mostrarToast("No se pudo actualizar el estado", COLOR_DANGER);
                            }
                        } catch (Exception e) {
                            mostrarError("Error al actualizar estado", e);
                        }
                    }
                };
                worker.execute();
            }
        }
    }

    private void mostrarDialogoCrearOrden(JLabel lblTotalValor) {
        if (carritoItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío.",
                "Carrito vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(new EmptyBorder(8, 4, 8, 4));

        JTextField txtDireccion  = crearTextField("Dirección de entrega", 200);
        JTextField txtCiudad     = crearTextField("Ciudad", 150);
        JTextField txtDepto      = crearTextField("Departamento", 150);
        JTextField txtNotas      = crearTextField("Notas adicionales (opcional)", 200);
        String[] metodos = {"efectivo", "transferencia", "tarjeta", "contraentrega"};
        JComboBox<String> cmbMetodo = new JComboBox<>(metodos);
        cmbMetodo.setFont(FONT_NORMAL);

        form.add(new JLabel("Dirección:")); form.add(txtDireccion);
        form.add(new JLabel("Ciudad:"));    form.add(txtCiudad);
        form.add(new JLabel("Departamento:")); form.add(txtDepto);
        form.add(new JLabel("Método de pago:")); form.add(cmbMetodo);
        form.add(new JLabel("Notas:")); form.add(txtNotas);

        // Resumen del carrito
        JPanel resumen = new JPanel(new BorderLayout());
        resumen.setBackground(new Color(240, 247, 240));
        resumen.setBorder(new EmptyBorder(8, 10, 8, 10));
        double total = carritoItems.stream().mapToDouble(CarritoItem::getSubtotal).sum();
        double iva = total * 0.19;
        JLabel lblResumen = new JLabel(String.format(
            "<html><b>Resumen:</b> %d ítem(s) · Subtotal: %s · IVA 19%%: %s · <b>Total: %s</b></html>",
            carritoItems.size(),
            FMT_CURRENCY.format(total),
            FMT_CURRENCY.format(iva),
            FMT_CURRENCY.format(total + iva)
        ));
        lblResumen.setFont(FONT_SMALL);
        resumen.add(lblResumen, BorderLayout.CENTER);

        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.add(form, BorderLayout.CENTER);
        container.add(resumen, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
            this, container, "Crear Orden de Reserva",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            if (txtDireccion.getText().trim().isEmpty() || txtCiudad.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Dirección y ciudad son obligatorios.", "Campos requeridos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int uid = SesionUsuario.getInstance().getIdUsuario();
            int idBodega = idBodegaFiltro != null ? idBodegaFiltro :
                (carritoItems.get(0).getIdBodega() != null ? carritoItems.get(0).getIdBodega() : 1);

            SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return carritoDAO.convertirCarritoAOrden(
                        uid, idBodega,
                        (String) cmbMetodo.getSelectedItem(),
                        txtNotas.getText().trim(),
                        txtDireccion.getText().trim(),
                        txtCiudad.getText().trim(),
                        txtDepto.getText().trim()
                    );
                }
                @Override
                protected void done() {
                    try {
                        int idOrden = get();
                        if (idOrden > 0) {
                            mostrarToast("✅ Orden #" + idOrden + " creada exitosamente", COLOR_SUCCESS);
                            carritoItems.clear();
                            actualizarTablaCarrito();
                            lblTotalValor.setText(FMT_CURRENCY.format(0));
                            cardLayout.show(cardPanel, "LISTA");
                            cargarDatosIniciales();
                        } else {
                            mostrarToast("Error al crear la orden", COLOR_DANGER);
                        }
                    } catch (Exception e) {
                        mostrarError("Error al crear orden", e);
                    }
                }
            };
            worker.execute();
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
        table.setGridColor(COLOR_BORDER);
        table.setSelectionBackground(COLOR_ROW_HOVER);
        table.setSelectionForeground(COLOR_TEXT_DARK);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Header estilizado
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BADGE);
        header.setBackground(new Color(248, 249, 250));
        header.setForeground(COLOR_TEXT_MUTED);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        header.setReorderingAllowed(false);

        // Renderer alternado
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? COLOR_WHITE : COLOR_ROW_ALT);
                }
                ((JLabel) c).setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
    }

    private JButton crearBoton(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() :
                    getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BADGE);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16,
            btn.getPreferredSize().height + 4));
        return btn;
    }

    private JTextField crearTextField(String placeholder, int width) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(COLOR_TEXT_MUTED);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Insets insets = getInsets();
                    g2.drawString(placeholder, insets.left + 2,
                        getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                    g2.dispose();
                }
            }
        };
        field.setFont(FONT_NORMAL);
        field.setPreferredSize(new Dimension(width, 32));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            new EmptyBorder(2, 8, 2, 8)
        ));
        return field;
    }

    private void mostrarToast(String mensaje, Color color) {
        Window window = SwingUtilities.getWindowAncestor(this);
        JWindow toast = new JWindow(window);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(color);
        content.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel lbl = new JLabel(mensaje);
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(COLOR_WHITE);
        content.add(lbl, BorderLayout.CENTER);
        toast.getContentPane().add(content);
        toast.pack();
        if (window != null) {
            toast.setLocation(
                window.getX() + window.getWidth() - toast.getWidth() - 20,
                window.getY() + window.getHeight() - toast.getHeight() - 50);
        }
        toast.setVisible(true);
        new Timer(3000, ev -> toast.dispose()).start();
    }

    private void mostrarError(String mensaje, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
            mensaje + "\n" + e.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    // =========================================================================
    // RENDERERS INTERNOS
    // =========================================================================

    /** Renderizador de badge de estado con color */
    private class EstadoBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = new JLabel(value != null ? value.toString() : "");
            lbl.setFont(FONT_BADGE);
            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBorder(new EmptyBorder(3, 10, 3, 10));

            String estado = value != null ? value.toString().toLowerCase() : "";
            switch (estado) {
                case "pendiente":  lbl.setBackground(new Color(255, 249, 224)); lbl.setForeground(new Color(183, 129, 0)); break;
                case "retirado":   lbl.setBackground(new Color(224, 242, 254)); lbl.setForeground(new Color(2, 132, 199)); break;
                case "pagado":     lbl.setBackground(new Color(220, 252, 231)); lbl.setForeground(new Color(21, 128, 61)); break;
                case "finalizado": lbl.setBackground(new Color(237, 233, 254)); lbl.setForeground(new Color(109, 40, 217)); break;
                case "cancelado":  lbl.setBackground(new Color(254, 226, 226)); lbl.setForeground(new Color(185, 28, 28)); break;
                default:           lbl.setBackground(new Color(241, 245, 249)); lbl.setForeground(COLOR_TEXT_MUTED);
            }

            if (isSelected) { lbl.setBackground(COLOR_ROW_HOVER); }
            return lbl;
        }
    }

    /** Renderizador de botones de acciones en la tabla */
    private class AccionesCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            p.setBackground(isSelected ? COLOR_ROW_HOVER : (row % 2 == 0 ? COLOR_WHITE : COLOR_ROW_ALT));
            JButton btnVer = new JButton("Ver");
            JButton btnEstado = new JButton("Estado");
            btnVer.setFont(FONT_SMALL);
            btnEstado.setFont(FONT_SMALL);
            btnVer.setForeground(COLOR_WHITE);
            btnEstado.setForeground(COLOR_WHITE);
            btnVer.setBackground(COLOR_INFO);
            btnEstado.setBackground(COLOR_WARNING);
            btnVer.setPreferredSize(new Dimension(46, 22));
            btnEstado.setPreferredSize(new Dimension(58, 22));
            p.add(btnVer);
            p.add(btnEstado);
            return p;
        }
    }

    /** Editor de botones de acciones */
    private class AccionesCellEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton btnVer, btnEstado;
        private int currentRow = -1;

        public AccionesCellEditor() {
            super(new JCheckBox());
            setClickCountToStart(1);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            panel.setBackground(COLOR_ROW_HOVER);
            btnVer    = new JButton("Ver");
            btnEstado = new JButton("Estado");
            btnVer.setFont(FONT_SMALL);
            btnEstado.setFont(FONT_SMALL);
            btnVer.setForeground(COLOR_WHITE);
            btnEstado.setForeground(COLOR_WHITE);
            btnVer.setBackground(COLOR_INFO);
            btnEstado.setBackground(new Color(230, 160, 0));
            btnVer.setPreferredSize(new Dimension(46, 22));
            btnEstado.setPreferredSize(new Dimension(58, 22));

            btnVer.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < ordenesList.size()) {
                    mostrarDetalleOrden(ordenesList.get(currentRow));
                }
            });
            btnEstado.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < ordenesList.size()) {
                    mostrarDialogoCambiarEstado(ordenesList.get(currentRow));
                }
            });
            panel.add(btnVer);
            panel.add(btnEstado);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {
            currentRow = row;
            return panel;
        }

        @Override public Object getCellEditorValue() { return "Acciones"; }
    }
}
