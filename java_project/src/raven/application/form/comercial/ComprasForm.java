package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import raven.application.form.comercial.compras.NuevaCompraDialog;
import raven.application.form.comercial.compras.DetalleCompraDialog;
import raven.clases.admin.UserSession;
import raven.clases.principal.ServiceCompra;
import raven.clases.comercial.ServiceSupplier;
import raven.controlador.principal.ModelCompra;
import raven.controlador.comercial.ModelSupplier;
import raven.modelos.SesionUsuario;
import raven.controlador.admin.SessionManager;
import raven.modal.Toast;
import raven.utils.ModalDialog;

/**
 * Panel principal del módulo de Compras a Proveedores.
 *
 * Funcionalidades: - Listado de compras con filtros - Nueva compra - Ver
 * detalle de compra - Anular compra
 *
 * @author CrisDEV
 * @version 1.0
 */
public class ComprasForm extends javax.swing.JPanel {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String STYLE_PANEL = "arc:25;background:$Login.background;";
    private static final String STYLE_PANEL_FILTROS = "arc:15;background:$Table.gridColor;";
    private static final String STYLE_CAMPO_TEXTO = "background:lighten($Menu.background,25%)";
    private static final String STYLE_BTN_VERDE = "arc:25;background:#28CD41;";
    private static final String STYLE_BTN_AZUL = "arc:25;background:#007AFF;";
    private static final String STYLE_BTN_ROJO = "arc:25;background:#FF3B30;";

    private static final NumberFormat FORMATO_MONEDA = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVICIOS
    // ═══════════════════════════════════════════════════════════════════════════
    private final ServiceCompra serviceCompra = new ServiceCompra();
    private final ServiceSupplier serviceSupplier = new ServiceSupplier();

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENTES UI
    // ═══════════════════════════════════════════════════════════════════════════
    private JLabel lblTitulo;
    private JPanel panelFiltros;
    private DatePicker dpFechaInicio;
    private DatePicker dpFechaFin;
    private JComboBox<ComboItem> cmbProveedor;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JButton btnNuevaCompra;

    private JTable tablaCompras;
    private DefaultTableModel modeloTabla;
    private JScrollPane scrollTabla;

    private JButton btnVerDetalle;
    private JButton btnEditar;
    private JButton btnRegistrarAbono;
    private JButton btnAnular;

    private JPanel panelEstadisticas;
    private JLabel lblTotalCompras;
    private JLabel lblTotalInvertido;
    private JLabel lblProductosIngresados;

    // ═══════════════════════════════════════════════════════════════════════════
    // ICONOS
    // ═══════════════════════════════════════════════════════════════════════════
    private FontIcon iconNuevo;
    private FontIcon iconBuscar;
    private FontIcon iconVer;
    private FontIcon iconEditar;
    private FontIcon iconAnular;
    private FontIcon iconRefresh;
    private FontIcon iconAbono;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    public ComprasForm() {
        initComponents();
        configurarEstilos();
        cargarProveedores();
        // Cargar compras después de que el componente esté visible
        javax.swing.SwingUtilities.invokeLater(this::cargarCompras);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Crear iconos
        Color iconColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.PLUS, iconColor);
        iconBuscar = createColoredIcon(FontAwesomeSolid.SEARCH, iconColor);
        iconVer = createColoredIcon(FontAwesomeSolid.EYE, iconColor);
        iconEditar = createColoredIcon(FontAwesomeSolid.EDIT, iconColor);
        iconAnular = createColoredIcon(FontAwesomeSolid.TIMES, iconColor);
        iconRefresh = createColoredIcon(FontAwesomeSolid.SYNC, iconColor);
        iconAbono = createColoredIcon(FontAwesomeSolid.MONEY_CHECK_ALT, Color.WHITE);

        // Panel superior (título y botón nueva compra)
        JPanel panelSuperior = crearPanelSuperior();
        add(panelSuperior, BorderLayout.NORTH);

        // Panel central con filtros y tabla
        JPanel panelCentral = crearPanelCentral();
        add(panelCentral, BorderLayout.CENTER);

        // Panel inferior con estadísticas
        panelEstadisticas = crearPanelEstadisticas();
        add(panelEstadisticas, BorderLayout.SOUTH);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Título
        lblTitulo = new JLabel("COMPRAS A PROVEEDORES");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");
        lblTitulo.setIcon(createColoredIcon(FontAwesomeSolid.TRUCK_LOADING,
                UIManager.getColor("TabbedPane.foreground")));

        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotones.setOpaque(false);

        btnNuevaCompra = new JButton("Nueva Compra");
        btnNuevaCompra.setIcon(iconNuevo);
        btnNuevaCompra.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_VERDE);
        btnNuevaCompra.setPreferredSize(new Dimension(160, 40));
        btnNuevaCompra.addActionListener(e -> abrirNuevaCompra());

        panelBotones.add(btnNuevaCompra);

        panel.add(lblTitulo, BorderLayout.WEST);
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }

    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Panel de filtros
        panelFiltros = crearPanelFiltros();
        panel.add(panelFiltros, BorderLayout.NORTH);

        // Tabla de compras
        crearTablaCompras();
        scrollTabla = new JScrollPane(tablaCompras);
        panel.add(scrollTabla, BorderLayout.CENTER);

        // Panel de acciones
        JPanel panelAcciones = crearPanelAcciones();
        panel.add(panelAcciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_FILTROS);

        // Fecha inicio
        JLabel lblDesde = new JLabel("Desde:");
        DatePickerSettings settingsInicio = new DatePickerSettings(Locale.of("es"));
        dpFechaInicio = new DatePicker(settingsInicio);
        dpFechaInicio.setDate(LocalDate.now().minusMonths(1));
        dpFechaInicio.getComponentDateTextField().putClientProperty(
                FlatClientProperties.STYLE, STYLE_CAMPO_TEXTO);

        // Fecha fin
        JLabel lblHasta = new JLabel("Hasta:");
        DatePickerSettings settingsFin = new DatePickerSettings(Locale.of("es"));
        dpFechaFin = new DatePicker(settingsFin);
        dpFechaFin.setDate(LocalDate.now());
        dpFechaFin.getComponentDateTextField().putClientProperty(
                FlatClientProperties.STYLE, STYLE_CAMPO_TEXTO);

        // Proveedor
        JLabel lblProveedor = new JLabel("Proveedor:");
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setPreferredSize(new Dimension(180, 35));

        // Búsqueda
        JLabel lblBuscar = new JLabel("Buscar:");
        txtBuscar = new JTextField(15);
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO_TEXTO);
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nº compra...");
        txtBuscar.addActionListener(e -> cargarCompras());

        // Botón buscar
        btnBuscar = new JButton("Buscar");
        btnBuscar.setIcon(iconBuscar);
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
        btnBuscar.addActionListener(e -> cargarCompras());

        panel.add(lblDesde);
        panel.add(dpFechaInicio);
        panel.add(lblHasta);
        panel.add(dpFechaFin);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblProveedor);
        panel.add(cmbProveedor);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblBuscar);
        panel.add(txtBuscar);
        panel.add(btnBuscar);

        return panel;
    }

    private void crearTablaCompras() {
        String[] columnas = { "ID", "Nº Compra", "Fecha", "Proveedor", "Bodega",
                "Unidades", "Total", "Abonado", "Saldo", "Estado Pago", "Estado" };

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 5) {
                    return Integer.class;
                }
                if (columnIndex == 6 || columnIndex == 7 || columnIndex == 8) {
                    return BigDecimal.class;
                }
                return String.class;
            }
        };

        tablaCompras = new JTable(modeloTabla);
        configurarTabla();
    }

    private void configurarTabla() {
        tablaCompras.setAutoCreateRowSorter(true);
        tablaCompras.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");

        tablaCompras.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");

        // Ocultar columna ID
        tablaCompras.getColumnModel().getColumn(0).setMinWidth(0);
        tablaCompras.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaCompras.getColumnModel().getColumn(0).setWidth(0);

        // Anchos de columnas
        tablaCompras.getColumnModel().getColumn(1).setPreferredWidth(130); // Nº Compra
        tablaCompras.getColumnModel().getColumn(2).setPreferredWidth(100); // Fecha
        tablaCompras.getColumnModel().getColumn(3).setPreferredWidth(180); // Proveedor
        tablaCompras.getColumnModel().getColumn(4).setPreferredWidth(120); // Bodega
        tablaCompras.getColumnModel().getColumn(5).setPreferredWidth(60);
        tablaCompras.getColumnModel().getColumn(6).setPreferredWidth(120);
        tablaCompras.getColumnModel().getColumn(7).setPreferredWidth(120);
        tablaCompras.getColumnModel().getColumn(8).setPreferredWidth(120);
        tablaCompras.getColumnModel().getColumn(9).setPreferredWidth(110);
        tablaCompras.getColumnModel().getColumn(10).setPreferredWidth(100);

        // Renderizador para total (formato moneda)
        tablaCompras.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (value instanceof BigDecimal) {
                    setText(FORMATO_MONEDA.format(value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });
        tablaCompras.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (value instanceof BigDecimal) {
                    setText(FORMATO_MONEDA.format(value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });
        tablaCompras.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (value instanceof BigDecimal) {
                    setText(FORMATO_MONEDA.format(value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });

        tablaCompras.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String estado = value != null ? value.toString() : "";
                Color bg;
                switch (estado.toLowerCase()) {
                    case "completado":
                        bg = new Color(40, 167, 69);
                        break;
                    case "parcial":
                        bg = new Color(0, 122, 255);
                        break;
                    case "pendiente":
                        bg = new Color(255, 193, 7);
                        break;
                    case "cancelado":
                        bg = new Color(220, 53, 69);
                        break;
                    default:
                        bg = table.getBackground();
                }
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                p.setOpaque(false);
                JLabel chip = new JLabel(estado);
                chip.setHorizontalAlignment(SwingConstants.CENTER);
                chip.setForeground(Color.WHITE);
                chip.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                chip.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:" + toHex(bg));
                p.add(chip);
                return p;
            }
        });

        // Renderizador para estado (color)
        tablaCompras.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                String estado = value != null ? value.toString() : "";

                if (!isSelected) {
                    switch (estado.toLowerCase()) {
                        case "recibida" ->
                            setForeground(new Color(40, 167, 69));
                        case "pendiente" ->
                            setForeground(new Color(255, 193, 7));
                        case "cancelada" ->
                            setForeground(new Color(220, 53, 69));
                        default ->
                            setForeground(table.getForeground());
                    }
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // Doble clic para ver detalle
        tablaCompras.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && tablaCompras.getSelectedRow() != -1) {
                    verDetalleCompra();
                }
            }
        });

        // Habilitar/deshabilitar abono segÃºn selecciÃ³n
        tablaCompras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonAbono();
            }
        });
    }

    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setOpaque(false);

        btnVerDetalle = new JButton("Ver Detalle");
        btnVerDetalle.setIcon(iconVer);
        btnVerDetalle.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
        btnVerDetalle.addActionListener(e -> verDetalleCompra());

        btnEditar = new JButton("Editar");
        btnEditar.setIcon(iconEditar);
        btnEditar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
        btnEditar.addActionListener(e -> editarCompra());

        btnRegistrarAbono = new JButton("Registrar Abono");
        btnRegistrarAbono.setIcon(iconAbono);
        btnRegistrarAbono.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_VERDE + "foreground:#FFFFFF;");
        btnRegistrarAbono.setPreferredSize(new Dimension(160, 40));
        btnRegistrarAbono.addActionListener(e -> registrarAbonoSeleccionado());

        btnAnular = new JButton("Anular Compra");
        btnAnular.setIcon(iconAnular);
        btnAnular.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_ROJO);
        btnAnular.addActionListener(e -> anularCompra());

        panel.add(btnVerDetalle);
        panel.add(btnEditar);
        panel.add(btnRegistrarAbono);
        panel.add(btnAnular);

        return panel;
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_FILTROS);

        lblTotalCompras = new JLabel("Total Compras: 0");
        lblTotalCompras.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 14));

        lblTotalInvertido = new JLabel("Total Invertido: $0");
        lblTotalInvertido.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 14));
        lblTotalInvertido.setForeground(new Color(40, 167, 69));

        lblProductosIngresados = new JLabel("Productos Ingresados: 0");
        lblProductosIngresados.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 14));

        panel.add(lblTotalCompras);
        panel.add(lblTotalInvertido);
        panel.add(lblProductosIngresados);

        return panel;
    }

    private void configurarEstilos() {
        putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGA DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════
    private void cargarProveedores() {
        try {
            cmbProveedor.removeAllItems();
            cmbProveedor.addItem(new ComboItem(0, "-- Todos --"));

            List<ModelSupplier> proveedores = serviceSupplier.getAll();
            for (ModelSupplier p : proveedores) {
                cmbProveedor.addItem(new ComboItem(p.getSupplierId(), p.getName()));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar proveedores: " + e.getMessage());
        }
    }

    private void cargarCompras() {
        try {
            LocalDate fechaInicio = dpFechaInicio.getDate();
            LocalDate fechaFin = dpFechaFin.getDate();

            ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
            Integer idProveedor = (provItem != null && provItem.getId() > 0) ? provItem.getId() : null;

            List<ModelCompra> compras = serviceCompra.listarCompras(
                    fechaInicio, fechaFin, idProveedor, null, null);

            actualizarTabla(compras);
            actualizarEstadisticas(compras);

        } catch (SQLException e) {
            System.err.println("Error al cargar compras: " + e.getMessage());
            // Solo mostrar Toast si el componente ya tiene un ancestor válido
            if (SwingUtilities.getWindowAncestor(this) != null) {
                Toast.show(this, Toast.Type.ERROR, "Error al cargar compras: " + e.getMessage());
            }
        }
    }

    private void actualizarTabla(List<ModelCompra> compras) {
        modeloTabla.setRowCount(0);

        for (ModelCompra c : compras) {
            modeloTabla.addRow(new Object[] {
                    c.getIdCompra(),
                    c.getNumeroCompra(),
                    c.getFechaCompra() != null ? c.getFechaCompra().toString() : "",
                    c.getNombreProveedor(),
                    c.getNombreBodega(),
                    c.getTotalUnidadesResumen() > 0 ? c.getTotalUnidadesResumen() : c.getCantidadTotalUnidades(),
                    c.getTotal(),
                    c.getTotalAbonado(),
                    c.getSaldoPendiente(),
                    c.getEstadoPago(),
                    c.getEstado().getValor().toUpperCase()
            });
        }

        actualizarEstadoBotonAbono();
    }

    private void actualizarEstadisticas(List<ModelCompra> compras) {
        int totalCompras = compras.size();
        BigDecimal totalInvertido = compras.stream()
                .map(ModelCompra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int productosIngresados = compras.stream()
                .mapToInt(c -> c.getTotalUnidadesResumen() > 0 ? c.getTotalUnidadesResumen() : c.getCantidadTotalUnidades())
                .sum();

        lblTotalCompras.setText("Total Compras: " + totalCompras);
        lblTotalInvertido.setText("Total Invertido: " + FORMATO_MONEDA.format(totalInvertido));
        lblProductosIngresados.setText("Productos Ingresados: " + productosIngresados);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCIONES
    // ═══════════════════════════════════════════════════════════════════════════
    private void abrirNuevaCompra() {
        var session = UserSession.getInstance();
        int idBodega = session.getIdBodegaUsuario();
        int idUsuario = SessionManager.getInstance().getCurrentUserId();
        if (idUsuario <= 0) {
            var usr = session.getCurrentUser();
            if (usr != null) {
                idUsuario = usr.getIdUsuario();
            } else {
                idUsuario = SesionUsuario.getInstance().getIdUsuario();
            }
        }

        NuevaCompraDialog dialog = new NuevaCompraDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                idBodega, idUsuario);

        dialog.setVisible(true);

        if (dialog.isCompraGuardada()) {
            cargarCompras();
            Toast.show(this, Toast.Type.SUCCESS, "Compra registrada exitosamente");
        }
    }

    private void verDetalleCompra() {
        int filaSeleccionada = tablaCompras.getSelectedRow();
        if (filaSeleccionada == -1) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una compra para ver el detalle");
            return;
        }

        int idCompra = (int) modeloTabla.getValueAt(filaSeleccionada, 0);

        try {
            serviceCompra.obtenerCompra(idCompra).ifPresent(compra -> {
                DetalleCompraDialog dialog = new DetalleCompraDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(this), compra);
                dialog.setVisible(true);
            });
        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al cargar detalle: " + e.getMessage());
        }
    }

    private void editarCompra() {
        int filaSeleccionada = tablaCompras.getSelectedRow();
        if (filaSeleccionada == -1) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una compra para editar");
            return;
        }

        int idCompra = (int) modeloTabla.getValueAt(filaSeleccionada, 0);

        try {
            java.util.Optional<ModelCompra> optCompra = serviceCompra.obtenerCompra(idCompra);
            if (optCompra.isPresent()) {
                ModelCompra compra = optCompra.get();

                if ("CANCELADA".equalsIgnoreCase(compra.getEstado().getValor())) {
                    Toast.show(this, Toast.Type.WARNING, "No se puede editar una compra anulada");
                    return;
                }

                var session = UserSession.getInstance();
                int idUsuario = SessionManager.getInstance().getCurrentUserId();
                if (idUsuario <= 0) {
                    var usr = session.getCurrentUser();
                    if (usr != null) {
                        idUsuario = usr.getIdUsuario();
                    } else {
                        idUsuario = SesionUsuario.getInstance().getIdUsuario();
                    }
                }

                NuevaCompraDialog dialog = new NuevaCompraDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(this),
                        compra, idUsuario);

                dialog.setVisible(true);

                if (dialog.isCompraGuardada()) {
                    cargarCompras();
                    Toast.show(this, Toast.Type.SUCCESS, "Compra actualizada exitosamente");
                }
            }
        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al cargar la compra: " + e.getMessage());
        }
    }

    private void anularCompra() {
        int filaSeleccionada = tablaCompras.getSelectedRow();
        if (filaSeleccionada == -1) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una compra para anular");
            return;
        }

        String estado = (String) modeloTabla.getValueAt(filaSeleccionada, 10);
        if ("CANCELADA".equalsIgnoreCase(estado)) {
            Toast.show(this, Toast.Type.WARNING, "La compra ya está anulada");
            return;
        }

        int idCompra = (int) modeloTabla.getValueAt(filaSeleccionada, 0);
        String numeroCompra = (String) modeloTabla.getValueAt(filaSeleccionada, 1);

        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de anular la compra " + numeroCompra + "?\n"
                        + "Esta acción revertirá el stock de inventario.",
                "Confirmar Anulación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmar == JOptionPane.YES_OPTION) {
            try {
                int idUsuario = SesionUsuario.getInstance().getIdUsuario();
                serviceCompra.anularCompra(idCompra, idUsuario);
                cargarCompras();
                Toast.show(this, Toast.Type.SUCCESS, "Compra anulada exitosamente");
            } catch (SQLException e) {
                Toast.show(this, Toast.Type.ERROR, "Error al anular: " + e.getMessage());
            }
        }
    }

    private void registrarAbonoSeleccionado() {
        int filaSeleccionada = tablaCompras.getSelectedRow();
        if (filaSeleccionada == -1) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una compra para registrar abono");
            return;
        }
        int idCompra = (int) modeloTabla.getValueAt(filaSeleccionada, 0);
        String numeroCompra = (String) modeloTabla.getValueAt(filaSeleccionada, 1);
        BigDecimal saldo = (BigDecimal) modeloTabla.getValueAt(filaSeleccionada, 8);
        String estadoPago = (String) modeloTabla.getValueAt(filaSeleccionada, 9);

        boolean saldoDisponible = saldo != null && saldo.compareTo(BigDecimal.ZERO) > 0;
        boolean estadoPermite = estadoPago == null || !estadoPago.equalsIgnoreCase("completado");
        if (!saldoDisponible || !estadoPermite) {
            Toast.show(this, Toast.Type.WARNING, "La compra ya está completada o sin saldo pendiente");
            return;
        }
        AbonoDialog dialog = new AbonoDialog((JFrame) SwingUtilities.getWindowAncestor(this), idCompra, numeroCompra, saldo);
        dialog.setVisible(true);
        if (dialog.isRegistrado()) {
            cargarCompras();
            Toast.show(this, Toast.Type.SUCCESS, "Abono registrado correctamente");
        }
    }

    private class AbonoDialog extends JDialog {
        private final int idCompra;
        private boolean registrado = false;
        private JTextField txtMonto;
        private ButtonGroup grpMedio;
        private JToggleButton btnEfectivo;
        private JToggleButton btnTransferencia;
        private JToggleButton btnTarjeta;
        private JToggleButton btnCheque;
        private JToggleButton btnOtro;
        private JTextField txtUrl;
        private JLabel lblPreview;
        private byte[] evidenciaBytes;
        private String evidenciaMime;
        private String evidenciaNombre;

        public AbonoDialog(JFrame parent, int idCompra, String numeroCompra, BigDecimal saldo) {
            super(parent, "Registrar Abono", true);
            this.idCompra = idCompra;
            initUI(numeroCompra, saldo);
            pack();
            setLocationRelativeTo(parent);
            setMinimumSize(new Dimension(520, 520));
        }

        public boolean isRegistrado() { return registrado; }

        private void initUI(String numeroCompra, BigDecimal saldo) {
            setLayout(new BorderLayout(0, 0));

            JPanel header = new JPanel(new BorderLayout());
            header.putClientProperty(FlatClientProperties.STYLE,
                    "background:lighten($Panel.background,4%);arc:28;border:1,1,1,1,$Component.borderColor,,12");
            JLabel title = new JLabel("Registrar Abono");
            title.putClientProperty(FlatClientProperties.STYLE, "font:$h2.font;foreground:$Text.foreground");
            JLabel subtitle = new JLabel("Compra " + numeroCompra + " • Saldo: " + NumberFormat.getCurrencyInstance(Locale.of("es","CO")).format(saldo));
            subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Text.secondary");
            JPanel headerText = new JPanel(new GridLayout(2,1));
            headerText.setOpaque(false);
            headerText.add(title);
            headerText.add(subtitle);
            header.add(headerText, BorderLayout.CENTER);
            add(header, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            form.putClientProperty(FlatClientProperties.STYLE,
                    "background:lighten($Panel.background,2%);arc:20;border:1,1,1,1,$Component.borderColor,,10");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(12, 16, 12, 16);
            gbc.anchor = GridBagConstraints.WEST;

            JLabel lbMonto = new JLabel("Monto");
            txtMonto = new JTextField(14);
            txtMonto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");
            txtMonto.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:lighten($Panel.background,6%)");

            JLabel lbMedio = new JLabel("Medio de pago");
            JPanel medioPanel = new JPanel(new GridLayout(1, 5, 8, 0));
            medioPanel.putClientProperty(FlatClientProperties.STYLE,
                    "background:lighten($Panel.background,6%);arc:16;border:1,1,1,1,$Component.borderColor,,8;padding:6,6,6,6");
            grpMedio = new ButtonGroup();
            btnEfectivo = new JToggleButton("Efectivo", FontIcon.of(FontAwesomeSolid.MONEY_BILL, 16, UIManager.getColor("TabbedPane.foreground")));
            btnTransferencia = new JToggleButton("Transferencia", FontIcon.of(FontAwesomeSolid.EXCHANGE_ALT, 16, UIManager.getColor("TabbedPane.foreground")));
            btnTarjeta = new JToggleButton("Tarjeta", FontIcon.of(FontAwesomeSolid.CREDIT_CARD, 16, UIManager.getColor("TabbedPane.foreground")));
            btnCheque = new JToggleButton("Cheque", FontIcon.of(FontAwesomeSolid.MONEY_CHECK_ALT, 16, UIManager.getColor("TabbedPane.foreground")));
            btnOtro = new JToggleButton("Otro", FontIcon.of(FontAwesomeSolid.ELLIPSIS_H, 16, UIManager.getColor("TabbedPane.foreground")));
            btnEfectivo.putClientProperty(FlatClientProperties.STYLE, "buttonType: segmented;segmentedPosition:first");
            btnTransferencia.putClientProperty(FlatClientProperties.STYLE, "buttonType: segmented;segmentedPosition:middle");
            btnTarjeta.putClientProperty(FlatClientProperties.STYLE, "buttonType: segmented;segmentedPosition:middle");
            btnCheque.putClientProperty(FlatClientProperties.STYLE, "buttonType: segmented;segmentedPosition:middle");
            btnOtro.putClientProperty(FlatClientProperties.STYLE, "buttonType: segmented;segmentedPosition:last");
            grpMedio.add(btnEfectivo);
            grpMedio.add(btnTransferencia);
            grpMedio.add(btnTarjeta);
            grpMedio.add(btnCheque);
            grpMedio.add(btnOtro);
            btnEfectivo.setSelected(true);
            medioPanel.add(btnEfectivo);
            medioPanel.add(btnTransferencia);
            medioPanel.add(btnTarjeta);
            medioPanel.add(btnCheque);
            medioPanel.add(btnOtro);

            JLabel lbUrl = new JLabel("URL evidencia (opcional)");
            txtUrl = new JTextField(18);
            txtUrl.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "https://...");
            txtUrl.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:lighten($Panel.background,6%)");

            JButton btnArchivo = new JButton("Seleccionar archivo");
            btnArchivo.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:@accentColor;foreground:#ffffff;borderWidth:0");
            lblPreview = new JLabel();
            lblPreview.setHorizontalAlignment(SwingConstants.CENTER);
            lblPreview.putClientProperty(FlatClientProperties.STYLE,
                    "background:lighten($Panel.background,8%);arc:16;border:1,1,1,1,$Component.borderColor,,8");
            lblPreview.setPreferredSize(new Dimension(440, 200));

            btnArchivo.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                int res = fc.showOpenDialog(this);
                if (res == JFileChooser.APPROVE_OPTION) {
                    java.io.File f = fc.getSelectedFile();
                    try {
                        evidenciaBytes = Files.readAllBytes(f.toPath());
                        evidenciaNombre = f.getName();
                        String mime = Files.probeContentType(f.toPath());
                        evidenciaMime = mime != null ? mime : "application/octet-stream";
                        if (evidenciaBytes.length > 0) {
                            try {
                                BufferedImage img = ImageIO.read(f);
                                if (img != null) {
                                    int w = lblPreview.getWidth();
                                    int h = lblPreview.getHeight();
                                    Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                                    lblPreview.setIcon(new ImageIcon(scaled));
                                } else {
                                    lblPreview.setText(evidenciaNombre);
                                }
                            } catch (Exception ignore) {
                                lblPreview.setText(evidenciaNombre);
                            }
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "No se pudo leer el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            gbc.gridx=0; gbc.gridy=0; form.add(lbMonto, gbc);
            gbc.gridx=1; form.add(txtMonto, gbc);
            gbc.gridx=0; gbc.gridy=1; form.add(lbMedio, gbc);
            gbc.gridx=1; form.add(medioPanel, gbc);
            gbc.gridx=0; gbc.gridy=2; form.add(lbUrl, gbc);
            gbc.gridx=1; form.add(txtUrl, gbc);
            gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2; form.add(btnArchivo, gbc);
            gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; gbc.fill=GridBagConstraints.HORIZONTAL; form.add(lblPreview, gbc);

            add(form, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            JButton btnCancelar = new JButton("Cancelar");
            btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:lighten($Panel.background,6%)");
            JButton btnRegistrar = new JButton("Registrar");
            btnRegistrar.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:@accentColor;foreground:#ffffff;borderWidth:0");

            btnCancelar.addActionListener(e -> dispose());
            btnRegistrar.addActionListener(e -> onRegistrar());

            actions.add(btnCancelar);
            actions.add(btnRegistrar);
            add(actions, BorderLayout.SOUTH);
        }

        private void onRegistrar() {
            try {
                BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
                String medio = obtenerMedioSeleccionado();
                String url = txtUrl.getText().trim();

                ServiceCompra.AbonoResultado r = serviceCompra.registrarAbonoConEvidencia(
                        idCompra,
                        monto,
                        medio,
                        url.isEmpty() ? null : url,
                        evidenciaBytes,
                        evidenciaMime,
                        evidenciaNombre
                );
                registrado = true;
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private Ikon metodoIcon(String m) {
            if (m == null) return FontAwesomeSolid.ELLIPSIS_H;
            switch (m) {
                case "efectivo":
                    return FontAwesomeSolid.MONEY_BILL;
                case "transferencia":
                    return FontAwesomeSolid.EXCHANGE_ALT;
                case "tarjeta":
                    return FontAwesomeSolid.CREDIT_CARD;
                case "cheque":
                    return FontAwesomeSolid.MONEY_CHECK_ALT;
                default:
                    return FontAwesomeSolid.ELLIPSIS_H;
            }
        }

        private String obtenerMedioSeleccionado() {
            if (btnEfectivo.isSelected()) return "efectivo";
            if (btnTransferencia.isSelected()) return "transferencia";
            if (btnTarjeta.isSelected()) return "tarjeta";
            if (btnCheque.isSelected()) return "cheque";
            return "otro";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════
    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    /**
     * Controla si el botÃ³n de registrar abono debe estar activo segÃºn la fila seleccionada.
     */
    private void actualizarEstadoBotonAbono() {
        if (btnRegistrarAbono == null) return;

        int fila = tablaCompras.getSelectedRow();
        if (fila == -1) {
            btnRegistrarAbono.setEnabled(false);
            return;
        }

        BigDecimal saldo = (BigDecimal) modeloTabla.getValueAt(fila, 8);
        String estadoPago = (String) modeloTabla.getValueAt(fila, 9);

        boolean saldoDisponible = saldo != null && saldo.compareTo(BigDecimal.ZERO) > 0;
        boolean estadoPermite = estadoPago == null || !estadoPago.equalsIgnoreCase("completado");

        btnRegistrarAbono.setEnabled(saldoDisponible && estadoPermite);
    }

    private static String toHex(Color c) {
        String r = String.format("%02x", c.getRed());
        String g = String.format("%02x", c.getGreen());
        String b = String.format("%02x", c.getBlue());
        return "#" + r + g + b;
    }

    /**
     * Clase interna para items del combo de proveedores.
     */
    private static class ComboItem {

        private final int id;
        private final String nombre;

        public ComboItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}
