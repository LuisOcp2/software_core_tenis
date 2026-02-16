package raven.application.form.comercial.compras;

import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import raven.clases.principal.ServiceCompra;
import raven.clases.principal.ServiceCompra.VarianteBusqueda;
import raven.clases.comercial.ServiceSupplier;
import raven.clases.productos.TraspasoService;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.Bodega;
import raven.controlador.productos.ModelProduct;
import raven.controlador.principal.ModelCompra;
import raven.controlador.principal.ModelCompraDetalle;
import raven.controlador.principal.ModelCompraDetalle.TipoUnidad;
import raven.controlador.comercial.ModelSupplier;
import raven.modal.Toast;

/**
 * Diálogo para registrar nueva compra a proveedor.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class NuevaCompraDialog extends JDialog {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String STYLE_CAMPO = "background:lighten($Menu.background,25%)";
    private static final String STYLE_BTN_VERDE = "arc:25;background:#28CD41;";
    private static final String STYLE_BTN_AZUL = "arc:25;background:#007AFF;";
    private static final String STYLE_BTN_ROJO = "arc:25;background:#FF3B30;";
    private static final String STYLE_PANEL = "arc:15;background:$Table.gridColor;";

    private static final NumberFormat FORMATO_MONEDA = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVICIOS Y DATOS
    // ═══════════════════════════════════════════════════════════════════════════

    private final ServiceCompra serviceCompra = new ServiceCompra();
    private final ServiceSupplier serviceSupplier = new ServiceSupplier();

    private int idBodega;
    private final int idUsuario;
    private boolean compraGuardada = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENTES UI - CABECERA
    // ═══════════════════════════════════════════════════════════════════════════

    private JComboBox<ComboItem> cmbProveedor;
    private JComboBox<ComboItem> cmbBodega;
    private DatePicker dpFechaCompra;
    private JTextField txtNumeroFactura;
    private JTextArea txtObservaciones;

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENTES UI - PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════════════

    private JTextField txtBuscarEan;
    private JButton btnBuscar;
    private JTable tablaProductos;
    private DefaultTableModel modeloTabla;
    private JButton btnEliminarItem;

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENTES UI - TOTALES
    // ═══════════════════════════════════════════════════════════════════════════

    private JLabel lblSubtotal;
    private JLabel lblIva;
    private JLabel lblTotal;
    private JLabel lblItems;

    private JButton btnGuardar;
    private JButton btnCancelar;

    // ═══════════════════════════════════════════════════════════════════════════
    // MODELO DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════

    private ModelCompra compraActual;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public NuevaCompraDialog(JFrame parent, int idBodega, int idUsuario) {
        super(parent, "Nueva Compra a Proveedor", true);
        this.idBodega = idBodega;
        this.idUsuario = idUsuario;

        compraActual = new ModelCompra();
        compraActual.setIdBodega(idBodega);
        compraActual.setIdUsuario(idUsuario);
        compraActual.setFechaCompra(LocalDate.now());

        initComponents();
        cargarProveedores();
        cargarBodegas();
        pack();
        ajustarDialogoParaPantalla(parent);
        setLocationRelativeTo(parent);
    }

    public NuevaCompraDialog(JFrame parent, ModelCompra compraEditar, int idUsuario) {
        super(parent, "Editar Compra - " + compraEditar.getNumeroCompra(), true);
        this.idUsuario = idUsuario;
        this.compraActual = compraEditar;
        this.idBodega = compraEditar.getIdBodega();

        initComponents();
        cargarProveedores();
        cargarBodegas();
        cargarDatosCompra();

        pack();
        ajustarDialogoParaPantalla(parent);
        setLocationRelativeTo(parent);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel superior (cabecera)
        JPanel panelCabecera = crearPanelCabecera();

        // Panel central (productos)
        JPanel panelProductos = crearPanelProductos();

        // Panel inferior (totales y acciones)
        JPanel panelInferior = crearPanelInferior();

        JPanel panelScrollable = new JPanel(new BorderLayout(10, 10));
        panelScrollable.setOpaque(false);
        panelScrollable.add(panelCabecera, BorderLayout.NORTH);
        panelScrollable.add(panelProductos, BorderLayout.CENTER);

        JScrollPane scrollMain = new JScrollPane(panelScrollable);
        scrollMain.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scrollMain.getVerticalScrollBar().setUnitIncrement(16);
        scrollMain.setOpaque(false);
        scrollMain.getViewport().setOpaque(false);

        add(scrollMain, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private void ajustarDialogoParaPantalla(Window parent) {
        GraphicsConfiguration gc = parent != null ? parent.getGraphicsConfiguration() : getGraphicsConfiguration();
        Rectangle bounds = gc != null ? gc.getBounds() : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Insets insets = gc != null ? Toolkit.getDefaultToolkit().getScreenInsets(gc) : new Insets(0, 0, 0, 0);

        int maxW = Math.max(600, bounds.width - insets.left - insets.right - 80);
        int maxH = Math.max(520, bounds.height - insets.top - insets.bottom - 80);

        Dimension pref = getPreferredSize();
        int w = Math.min(pref.width, maxW);
        int h = Math.min(pref.height, maxH);

        w = Math.max(Math.min(900, maxW), w);
        h = Math.max(Math.min(700, maxH), h);

        setResizable(true);
        setMinimumSize(new Dimension(Math.min(600, maxW), Math.min(520, maxH)));
        setSize(new Dimension(w, h));
    }

    private JPanel crearPanelCabecera() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createTitledBorder("Datos de la Compra"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Proveedor
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Proveedor:"), gbc);

        gbc.gridx = 1;
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setPreferredSize(new Dimension(250, 35));
        panel.add(cmbProveedor, gbc);

        // Fecha
        gbc.gridx = 2;
        panel.add(new JLabel("Fecha:"), gbc);

        gbc.gridx = 3;
        DatePickerSettings settings = new DatePickerSettings(Locale.of("es"));
        dpFechaCompra = new DatePicker(settings);
        dpFechaCompra.setDate(LocalDate.now());
        dpFechaCompra.getComponentDateTextField().putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
        panel.add(dpFechaCompra, gbc);

        // Bodega
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Bodega:"), gbc);

        gbc.gridx = 1;
        cmbBodega = new JComboBox<>();
        cmbBodega.setPreferredSize(new Dimension(250, 35));
        panel.add(cmbBodega, gbc);
        cmbBodega.addActionListener(e -> {
            ComboItem bItem = (ComboItem) cmbBodega.getSelectedItem();
            if (bItem != null && bItem.getId() > 0) {
                this.compraActual.setIdBodega(bItem.getId());
                this.idBodega = bItem.getId();
            }
        });

        // Número de factura
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Nº Factura:"), gbc);

        gbc.gridx = 1;
        txtNumeroFactura = new JTextField(20);
        txtNumeroFactura.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
        txtNumeroFactura.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Opcional");
        panel.add(txtNumeroFactura, gbc);

        // Observaciones
        gbc.gridx = 2;
        panel.add(new JLabel("Observaciones:"), gbc);

        gbc.gridx = 3;
        txtObservaciones = new JTextArea(2, 20);
        txtObservaciones.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
        JScrollPane scrollObs = new JScrollPane(txtObservaciones);
        scrollObs.setPreferredSize(new Dimension(250, 50));
        panel.add(scrollObs, gbc);

        return panel;
    }

    private JPanel crearPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Productos"));

        // Panel de búsqueda
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelBusqueda.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);

        JLabel lblEan = new JLabel("Código EAN/SKU:");
        txtBuscarEan = new JTextField(20);
        txtBuscarEan.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
        txtBuscarEan.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Escanear o escribir...");
        txtBuscarEan.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    buscarProductoPorEan();
                }
            }
        });

        btnBuscar = new JButton("Buscar Producto");
        btnBuscar.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 16, UIManager.getColor("TabbedPane.foreground")));
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
        btnBuscar.addActionListener(e -> abrirBuscadorProductos());

        panelBusqueda.add(lblEan);
        panelBusqueda.add(txtBuscarEan);
        panelBusqueda.add(btnBuscar);

        panel.add(panelBusqueda, BorderLayout.NORTH);

        // Tabla de productos
        String[] columnas = { "ID Var", "Producto", "Variante", "EAN", "Cantidad",
                "Tipo", "Precio Unit.", "Subtotal", "Pares" };

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4 || column == 5 || column == 6; // Cantidad, Tipo, Precio
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 4)
                    return Integer.class;
                if (columnIndex == 6 || columnIndex == 7)
                    return BigDecimal.class;
                if (columnIndex == 8)
                    return Integer.class;
                return String.class;
            }
        };

        tablaProductos = new JTable(modeloTabla);
        configurarTablaProductos();

        JScrollPane scrollTabla = new JScrollPane(tablaProductos);
        panel.add(scrollTabla, BorderLayout.CENTER);

        // Panel de acciones de tabla
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnEliminarItem = new JButton("Eliminar Seleccionado");
        btnEliminarItem.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 16, Color.WHITE));
        btnEliminarItem.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_ROJO);
        btnEliminarItem.addActionListener(e -> eliminarItemSeleccionado());
        panelAcciones.add(btnEliminarItem);

        panel.add(panelAcciones, BorderLayout.SOUTH);

        return panel;
    }

    private void configurarTablaProductos() {
        tablaProductos.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;" +
                        "showVerticalLines:false;" +
                        "rowHeight:40;" +
                        "intercellSpacing:10,5");

        tablaProductos.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;" +
                        "height:35;" +
                        "separatorColor:$TableHeader.background;" +
                        "font:bold $h4.font");

        // Ocultar columna ID
        tablaProductos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaProductos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaProductos.getColumnModel().getColumn(0).setWidth(0);

        // Anchos
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablaProductos.getColumnModel().getColumn(2).setPreferredWidth(120);
        tablaProductos.getColumnModel().getColumn(3).setPreferredWidth(120);
        tablaProductos.getColumnModel().getColumn(4).setPreferredWidth(70);
        tablaProductos.getColumnModel().getColumn(5).setPreferredWidth(70);
        tablaProductos.getColumnModel().getColumn(6).setPreferredWidth(100);
        tablaProductos.getColumnModel().getColumn(7).setPreferredWidth(100);
        tablaProductos.getColumnModel().getColumn(8).setPreferredWidth(80);

        // Combo para tipo de unidad
        JComboBox<String> comboTipo = new JComboBox<>(new String[] { "par", "caja" });
        tablaProductos.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(comboTipo));

        // Renderizador moneda
        DefaultTableCellRenderer monedaRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof BigDecimal) {
                    setText(FORMATO_MONEDA.format(value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        tablaProductos.getColumnModel().getColumn(6).setCellRenderer(monedaRenderer);
        tablaProductos.getColumnModel().getColumn(7).setCellRenderer(monedaRenderer);

        tablaProductos.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                String tipo = value != null ? value.toString() : "";
                Color bg = "caja".equalsIgnoreCase(tipo) ? new Color(255, 149, 0) : new Color(0, 122, 255);
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                p.setOpaque(false);
                JLabel chip = new JLabel(tipo);
                chip.setHorizontalAlignment(SwingConstants.CENTER);
                chip.setForeground(Color.WHITE);
                chip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                chip.putClientProperty(FlatClientProperties.STYLE, "arc:16;background:" + toHex(bg));
                p.add(chip);
                return p;
            }
        });

        // Listener para actualizar subtotal cuando cambia cantidad o precio
        modeloTabla.addTableModelListener(e -> {
            if (e.getColumn() == 4 || e.getColumn() == 5 || e.getColumn() == 6) {
                int row = e.getFirstRow();
                actualizarSubtotalFila(row);
                actualizarTotales();
            }
        });
    }

    private static String toHex(Color c) {
        String r = String.format("%02x", c.getRed());
        String g = String.format("%02x", c.getGreen());
        String b = String.format("%02x", c.getBlue());
        return "#" + r + g + b;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Panel de totales
        JPanel panelTotales = new JPanel(new GridLayout(2, 4, 15, 5));
        panelTotales.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panelTotales.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblItems = new JLabel("Items: 0");
        lblItems.setFont(lblItems.getFont().deriveFont(Font.BOLD, 14f));

        lblSubtotal = new JLabel("Subtotal: $0.00");
        lblSubtotal.setFont(lblSubtotal.getFont().deriveFont(Font.BOLD, 14f));

        lblIva = new JLabel("IVA: $0.00");
        lblIva.setFont(lblIva.getFont().deriveFont(Font.BOLD, 14f));

        lblTotal = new JLabel("TOTAL: $0.00");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 18f));
        lblTotal.setForeground(new Color(40, 167, 69));

        panelTotales.add(lblItems);
        panelTotales.add(lblSubtotal);
        panelTotales.add(lblIva);
        panelTotales.add(lblTotal);

        panel.add(panelTotales, BorderLayout.CENTER);

        // Panel de acciones
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        btnGuardar = new JButton("Guardar Compra");
        btnGuardar.setIcon(FontIcon.of(FontAwesomeSolid.SAVE, 16, Color.WHITE));
        btnGuardar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_VERDE);
        btnGuardar.setPreferredSize(new Dimension(160, 45));
        btnGuardar.addActionListener(e -> guardarCompra());

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setIcon(FontIcon.of(FontAwesomeSolid.TIMES, 16, UIManager.getColor("TabbedPane.foreground")));
        btnCancelar.addActionListener(e -> dispose());

        panelAcciones.add(btnCancelar);
        panelAcciones.add(btnGuardar);

        panel.add(panelAcciones, BorderLayout.SOUTH);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGA DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════

    private void cargarProveedores() {
        try {
            cmbProveedor.removeAllItems();
            cmbProveedor.addItem(new ComboItem(0, "-- Seleccionar Proveedor --"));

            List<ModelSupplier> proveedores = serviceSupplier.getAll();
            for (ModelSupplier p : proveedores) {
                cmbProveedor.addItem(new ComboItem(p.getSupplierId(), p.getName()));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar proveedores: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarBodegas() {
        try {
            cmbBodega.removeAllItems();
            cmbBodega.addItem(new ComboItem(0, "-- Seleccionar Bodega --"));

            TraspasoService ts = new TraspasoService();
            List<Bodega> bodegas = ts.obtenerBodegasActivas();
            for (Bodega b : bodegas) {
                cmbBodega.addItem(new ComboItem(b.getIdBodega(), b.getNombre()));
            }

            // seleccionar la bodega inicial
            for (int i = 0; i < cmbBodega.getItemCount(); i++) {
                ComboItem it = cmbBodega.getItemAt(i);
                if (it.getId() == this.idBodega) {
                    cmbBodega.setSelectedIndex(i);
                    break;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar bodegas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarDatosCompra() {
        // Seleccionar proveedor
        for (int i = 0; i < cmbProveedor.getItemCount(); i++) {
            ComboItem item = cmbProveedor.getItemAt(i);
            if (item.getId() == compraActual.getIdProveedor()) {
                cmbProveedor.setSelectedIndex(i);
                break;
            }
        }

        // Seleccionar bodega
        for (int i = 0; i < cmbBodega.getItemCount(); i++) {
            ComboItem item = cmbBodega.getItemAt(i);
            if (item.getId() == compraActual.getIdBodega()) {
                cmbBodega.setSelectedIndex(i);
                break;
            }
        }

        // Fecha
        dpFechaCompra.setDate(compraActual.getFechaCompra());

        // Factura y Observaciones
        txtNumeroFactura.setText(compraActual.getNumeroFactura());
        txtObservaciones.setText(compraActual.getObservaciones());

        // Cargar productos en tabla
        modeloTabla.setRowCount(0);
        for (ModelCompraDetalle d : compraActual.getDetalles()) {
            int totalPares = d.getTipoUnidad() == TipoUnidad.CAJA ? d.getCantidad() * 24 : d.getCantidad();
            String descripcion = d.getNombreTalla() + " - " + d.getNombreColor();

            modeloTabla.addRow(new Object[] {
                    d.getIdVariante(),
                    d.getNombreProducto(),
                    descripcion,
                    d.getEan(),
                    d.getCantidad(),
                    d.getTipoUnidad().getValor(),
                    d.getPrecioUnitario(),
                    d.getSubtotal(),
                    totalPares
            });
        }

        actualizarTotales();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BÚSQUEDA DE PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════════════

    private void buscarProductoPorEan() {
        ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
        ComboItem bItem = (ComboItem) cmbBodega.getSelectedItem();
        int idProvSel = provItem != null ? provItem.getId() : 0;
        int idBodSel = bItem != null ? bItem.getId() : 0;
        if (idProvSel <= 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un proveedor");
            cmbProveedor.requestFocus();
            return;
        }
        if (idBodSel <= 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una bodega");
            cmbBodega.requestFocus();
            return;
        }
        String ean = txtBuscarEan.getText().trim();
        if (ean.isEmpty()) {
            return;
        }

        try {
            Optional<VarianteBusqueda> variante = serviceCompra.buscarPorEan(ean, idBodSel);

            if (variante.isPresent()) {
                try {
                    ServiceProduct sp = new ServiceProduct();
                    raven.controlador.productos.ModelProduct mp = sp.getProductById(variante.get().idProducto);
                    int provProd = (mp != null && mp.getSupplier() != null) ? mp.getSupplier().getSupplierId() : 0;
                    if (provProd != idProvSel) {
                        Toast.show(this, Toast.Type.WARNING, "El producto pertenece a otro proveedor");
                    } else {
                        mostrarDialogoAgregarProductoDetallado(variante.get());
                    }
                } catch (Exception ex) {
                    Toast.show(this, Toast.Type.ERROR, "Error validando proveedor del producto");
                }
            } else {
                Toast.show(this, Toast.Type.WARNING, "Producto no encontrado: " + ean);
            }
        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al buscar: " + e.getMessage());
        }

        txtBuscarEan.setText("");
        txtBuscarEan.requestFocus();
    }

    private void abrirBuscadorProductos() {
        ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
        int idProv = provItem != null ? provItem.getId() : 0;
        if (idProv <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un proveedor antes de buscar/crear variantes",
                    "Proveedor requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ComboItem bItem = (ComboItem) cmbBodega.getSelectedItem();
        int idBod = bItem != null ? bItem.getId() : 0;
        if (idBod <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una bodega para buscar productos", "Bodega requerida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        BuscadorProductoCompraDialog buscador = new BuscadorProductoCompraDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), idBod, idUsuario, idProv, serviceCompra);
        buscador.setVisible(true);

        if (buscador.getVarianteSeleccionada() != null) {
            mostrarDialogoAgregarProductoDetallado(buscador.getVarianteSeleccionada());
        }
    }

    private void mostrarDialogoAgregarProducto(VarianteBusqueda variante) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));

        JLabel lblProducto = new JLabel(variante.getDescripcionCompleta());
        lblProducto.setFont(lblProducto.getFont().deriveFont(Font.BOLD));

        JLabel lblStock = new JLabel(
                "Stock actual: " + variante.stockPares + " pares / " + variante.stockCajas + " cajas");

        JSpinner spinCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        JComboBox<String> comboTipo = new JComboBox<>(new String[] { "par", "caja" });
        JTextField txtPrecio = new JTextField(variante.precioCompra != null ? variante.precioCompra.toString() : "0");

        panel.add(new JLabel("Producto:"));
        panel.add(lblProducto);
        panel.add(new JLabel("Stock:"));
        panel.add(lblStock);
        panel.add(new JLabel("Cantidad:"));
        panel.add(spinCantidad);
        panel.add(new JLabel("Tipo:"));
        panel.add(comboTipo);

        // Panel adicional para precio
        JPanel panelPrecio = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelPrecio.add(new JLabel("Precio Unitario: $"));
        panelPrecio.add(txtPrecio);

        JPanel panelCompleto = new JPanel(new BorderLayout(10, 10));
        panelCompleto.add(panel, BorderLayout.CENTER);
        panelCompleto.add(panelPrecio, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panelCompleto,
                "Agregar Producto a Compra", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int cantidad = (int) spinCantidad.getValue();
                String tipo = (String) comboTipo.getSelectedItem();
                BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());

                agregarProductoATabla(variante, cantidad, tipo, precio);
            } catch (NumberFormatException e) {
                Toast.show(this, Toast.Type.ERROR, "Precio inválido");
            }
        }
    }

    private void agregarProductoATabla(VarianteBusqueda variante, int cantidad,
            String tipo, BigDecimal precio) {

        int totalPares = "caja".equalsIgnoreCase(tipo) ? cantidad * 24 : cantidad;
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(totalPares));

        modeloTabla.addRow(new Object[] {
                variante.idVariante,
                variante.nombreProducto,
                variante.getDescripcionVariante(),
                variante.ean,
                cantidad,
                tipo,
                precio,
                subtotal,
                totalPares
        });

        // Agregar a modelo de compra
        ModelCompraDetalle detalle = ModelCompraDetalle.builder()
                .variante(variante.idVariante)
                .producto(variante.idProducto)
                .nombreProducto(variante.nombreProducto)
                .talla(variante.talla)
                .color(variante.color)
                .ean(variante.ean)
                .cantidad(cantidad)
                .tipoUnidad(TipoUnidad.fromString(tipo))
                .precioUnitario(precio)
                .build();

        compraActual.agregarDetalle(detalle);
        actualizarTotales();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GESTIÓN DE TABLA
    // ═══════════════════════════════════════════════════════════════════════════

    private void eliminarItemSeleccionado() {
        int filaSeleccionada = tablaProductos.getSelectedRow();
        if (filaSeleccionada == -1) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un producto para eliminar");
            return;
        }

        modeloTabla.removeRow(filaSeleccionada);
        compraActual.eliminarDetalle(filaSeleccionada);
        actualizarTotales();
    }

    private void actualizarSubtotalFila(int row) {
        if (row < 0 || row >= modeloTabla.getRowCount())
            return;

        try {
            Object cantObj = modeloTabla.getValueAt(row, 4);
            Object precioObj = modeloTabla.getValueAt(row, 6);

            int cantidad = cantObj instanceof Integer ? (Integer) cantObj : Integer.parseInt(cantObj.toString());
            BigDecimal precio = precioObj instanceof BigDecimal ? (BigDecimal) precioObj
                    : new BigDecimal(precioObj.toString());

            String tipo = (String) modeloTabla.getValueAt(row, 5);
            int totalPares = "caja".equalsIgnoreCase(tipo) ? cantidad * 24 : cantidad;
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(totalPares));
            modeloTabla.setValueAt(subtotal, row, 7);
            modeloTabla.setValueAt(totalPares, row, 8);

            // Actualizar detalle en modelo
            if (row < compraActual.getDetalles().size()) {
                ModelCompraDetalle detalle = compraActual.getDetalles().get(row);
                detalle.setCantidad(cantidad);
                detalle.setPrecioUnitario(precio);
                detalle.setTipoUnidad(TipoUnidad.fromString(tipo));
            }
        } catch (Exception e) {
            // Ignorar errores de conversión
        }
    }

    private void actualizarTotales() {
        compraActual.recalcularTotales();

        lblItems.setText("Items: " + compraActual.getCantidadItems());
        lblSubtotal.setText("Subtotal: " + FORMATO_MONEDA.format(compraActual.getSubtotal()));
        lblIva.setText("IVA: " + FORMATO_MONEDA.format(compraActual.getIva()));
        lblTotal.setText("TOTAL: " + FORMATO_MONEDA.format(compraActual.getTotal()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GUARDAR COMPRA
    // ═══════════════════════════════════════════════════════════════════════════

    private void guardarCompra() {
        // Validar proveedor
        ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
        if (provItem == null || provItem.getId() <= 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un proveedor");
            cmbProveedor.requestFocus();
            return;
        }

        // Validar bodega
        ComboItem bItem = (ComboItem) cmbBodega.getSelectedItem();
        if (bItem == null || bItem.getId() <= 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una bodega");
            cmbBodega.requestFocus();
            return;
        }

        // Validar productos
        if (compraActual.getCantidadItems() == 0) {
            Toast.show(this, Toast.Type.WARNING, "Agregue al menos un producto");
            return;
        }

        // Confirmar
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Confirmar el registro de la compra?\n\n" +
                        "Proveedor: " + provItem + "\n" +
                        "Items: " + compraActual.getCantidadItems() + "\n" +
                        "Total: " + FORMATO_MONEDA.format(compraActual.getTotal()),
                "Confirmar Compra",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        // Preparar compra
        compraActual.setIdProveedor(provItem.getId());
        compraActual.setIdBodega(bItem.getId());
        compraActual.setFechaCompra(dpFechaCompra.getDate());
        compraActual.setNumeroFactura(txtNumeroFactura.getText().trim());
        compraActual.setObservaciones(txtObservaciones.getText().trim());

        try {
            if (compraActual.getIdCompra() != null && compraActual.getIdCompra() > 0) {
                serviceCompra.actualizarCompra(compraActual);
                JOptionPane.showMessageDialog(this,
                        "Compra actualizada exitosamente\n" +
                                "Número: " + compraActual.getNumeroCompra(),
                        "Compra Actualizada",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                int idCompra = serviceCompra.registrarCompra(compraActual);
                JOptionPane.showMessageDialog(this,
                        "Compra registrada exitosamente\n" +
                                "Número: " + compraActual.getNumeroCompra(),
                        "Compra Guardada",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            compraGuardada = true;
            dispose();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al guardar la compra:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isCompraGuardada() {
        return compraGuardada;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLASES INTERNAS
    // ═══════════════════════════════════════════════════════════════════════════

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

    private void mostrarDialogoAgregarProductoDetallado(VarianteBusqueda variante) {
        AgregarVarianteSimpleDialog dialog = new AgregarVarianteSimpleDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this));
        dialog.prefill(variante);
        dialog.setVisible(true);
        SeleccionCompra sel = dialog.getSeleccion();
        if (sel != null && sel.variante != null && sel.precio != null && sel.tipo != null) {
            agregarProductoATabla(sel.variante, sel.cantidad, sel.tipo, sel.precio);
        }
    }

    private void mostrarDialogoAgregarProductoPorProducto(ServiceCompra.ProductoBusqueda producto) {
        AgregarProductoCompraDialog dialog = new AgregarProductoCompraDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), idBodega, idUsuario, serviceCompra);
        dialog.prefillWithProducto(producto);
        dialog.setVisible(true);
        SeleccionCompra sel = dialog.getSeleccion();
        if (sel != null && sel.variante != null && sel.precio != null && sel.tipo != null) {
            agregarProductoATabla(sel.variante, sel.cantidad, sel.tipo, sel.precio);
        }
    }

    private static class SeleccionCompra {
        VarianteBusqueda variante;
        int cantidad;
        String tipo;
        BigDecimal precio;
    }

    private class AgregarVarianteSimpleDialog extends JDialog {
        private JLabel lblProducto;
        private JSpinner spCantidad;
        private JComboBox<String> cmbTipo;
        private JTextField txtPrecio;
        private JLabel lblPreviewPares;
        private JLabel lblPreviewTotal;
        private JButton btnAceptar;
        private JButton btnCancelar;
        private SeleccionCompra seleccion;
        private VarianteBusqueda varianteBase;

        public AgregarVarianteSimpleDialog(JFrame parent) {
            super(parent, "Agregar producto", true);
            initUI();
            pack();
            setLocationRelativeTo(parent);
            setMinimumSize(new Dimension(600, 240));
        }

        private void initUI() {
            setLayout(new BorderLayout(10, 10));

            JPanel top = new JPanel(new GridLayout(1, 1));
            lblProducto = new JLabel("");
            lblProducto.setFont(lblProducto.getFont().deriveFont(Font.BOLD));
            top.add(lblProducto);
            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8, 10, 8, 10);
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridx = 0;
            c.gridy = 0;
            center.add(new JLabel("Cantidad:"), c);
            c.gridx = 1;
            spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
            center.add(spCantidad, c);

            c.gridx = 0;
            c.gridy = 1;
            center.add(new JLabel("Tipo:"), c);
            c.gridx = 1;
            cmbTipo = new JComboBox<>(new String[] { "par", "caja" });
            center.add(cmbTipo, c);

            c.gridx = 0;
            c.gridy = 2;
            center.add(new JLabel("Precio por par:"), c);
            c.gridx = 1;
            txtPrecio = new JTextField("0");
            center.add(txtPrecio, c);

            c.gridx = 0;
            c.gridy = 3;
            center.add(new JLabel("Total pares:"), c);
            c.gridx = 1;
            lblPreviewPares = new JLabel("0");
            center.add(lblPreviewPares, c);

            c.gridx = 0;
            c.gridy = 4;
            center.add(new JLabel("Total calculado:"), c);
            c.gridx = 1;
            lblPreviewTotal = new JLabel(FORMATO_MONEDA.format(BigDecimal.ZERO));
            center.add(lblPreviewTotal, c);

            add(center, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            btnCancelar = new JButton("Cancelar");
            btnCancelar.addActionListener(e -> dispose());
            btnAceptar = new JButton("Agregar");
            btnAceptar.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:25;background:#28CD41;");
            btnAceptar.addActionListener(e -> onAceptar());
            bottom.add(btnCancelar);
            bottom.add(btnAceptar);
            add(bottom, BorderLayout.SOUTH);

            spCantidad.addChangeListener(e -> updatePreview());
            cmbTipo.addActionListener(e -> updatePreview());
            txtPrecio.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    updatePreview();
                }
            });
        }

        public void prefill(VarianteBusqueda v) {
            this.varianteBase = v;
            lblProducto.setText(v.getDescripcionCompleta());
            if (v.precioCompra != null) {
                txtPrecio.setText(v.precioCompra.toString());
            }
            updatePreview();
        }

        private void onAceptar() {
            BigDecimal precio;
            try {
                precio = new BigDecimal(txtPrecio.getText().trim());
            } catch (Exception ex) {
                Toast.show(this, Toast.Type.ERROR, "Precio inválido");
                return;
            }
            String tipoSel = (String) cmbTipo.getSelectedItem();
            int cantidadSel = (Integer) spCantidad.getValue();

            VarianteBusqueda vfinal = new VarianteBusqueda();
            vfinal.idVariante = varianteBase.idVariante;
            vfinal.idProducto = varianteBase.idProducto;
            vfinal.nombreProducto = varianteBase.nombreProducto;
            vfinal.talla = varianteBase.talla;
            vfinal.color = varianteBase.color;
            vfinal.sku = varianteBase.sku;
            vfinal.ean = varianteBase.ean;
            vfinal.stockPares = varianteBase.stockPares;
            vfinal.stockCajas = varianteBase.stockCajas;
            vfinal.precioCompra = precio;

            seleccion = new SeleccionCompra();
            seleccion.variante = vfinal;
            seleccion.cantidad = cantidadSel;
            seleccion.tipo = tipoSel;
            seleccion.precio = precio;
            dispose();
        }

        private void updatePreview() {
            try {
                int cantidadSel = (Integer) spCantidad.getValue();
                String tipoSel = (String) cmbTipo.getSelectedItem();
                BigDecimal precioSel = new BigDecimal(txtPrecio.getText().trim());
                int pares = "caja".equalsIgnoreCase(tipoSel) ? cantidadSel * 24 : cantidadSel;
                BigDecimal total = precioSel.multiply(BigDecimal.valueOf(pares));
                lblPreviewPares.setText(String.valueOf(pares));
                lblPreviewTotal.setText(FORMATO_MONEDA.format(total));
            } catch (Exception ex) {
                lblPreviewPares.setText("0");
                lblPreviewTotal.setText(FORMATO_MONEDA.format(BigDecimal.ZERO));
            }
        }

        public SeleccionCompra getSeleccion() {
            return seleccion;
        }
    }

    private class AgregarProductoCompraDialog extends JDialog {
        private final int idBodegaDialog;
        private final int idUsuarioDialog;
        private final ServiceCompra serviceCompraDialog;
        private raven.clases.productos.ServiceProductVariant serviceVariantDialog;
        private raven.clases.productos.ServiceProduct serviceProductDialog;
        private raven.clases.productos.ServiceSize serviceSizeDialog;
        private raven.clases.productos.ServiceColor serviceColorDialog;

        private JTextField txtEan;
        private JButton btnBuscarEan;
        private JButton btnBuscarProducto;
        private JLabel lblProductoSel;
        private JComboBox<String> cmbTalla;
        private JComboBox<String> cmbColor;
        private JLabel lblImagen;
        private JButton btnSeleccionarImagen;
        private JSpinner spCantidad;
        private JComboBox<String> cmbTipo;
        private JTextField txtPrecioCompra;
        private JButton btnAceptar;
        private JButton btnCancelar;
        private JButton btnCrearVariante;

        private byte[] imagenSeleccionada;
        private VarianteBusqueda varianteSeleccionada;
        private int idProductoActual;
        private String nombreProductoActual;
        private SeleccionCompra seleccion;

        public AgregarProductoCompraDialog(JFrame parent, int idBodega, int idUsuario, ServiceCompra serviceCompra) {
            super(parent, "Agregar producto", true);
            this.idBodegaDialog = idBodega;
            this.idUsuarioDialog = idUsuario;
            this.serviceCompraDialog = serviceCompra;
            this.serviceVariantDialog = new raven.clases.productos.ServiceProductVariant();
            this.serviceProductDialog = new raven.clases.productos.ServiceProduct();
            this.serviceSizeDialog = new raven.clases.productos.ServiceSize();
            this.serviceColorDialog = new raven.clases.productos.ServiceColor();
            initUI();
            pack();
            setLocationRelativeTo(parent);
            setMinimumSize(new Dimension(700, 600));
        }

        public void prefillWithVariante(VarianteBusqueda v) {
            this.varianteSeleccionada = v;
            this.idProductoActual = v.idProducto;
            this.nombreProductoActual = v.nombreProducto;
            lblProductoSel.setText(v.getDescripcionCompleta());
            txtEan.setText(v.ean != null ? v.ean : "");
            cargarTallasYColoresDeProducto(v.idProducto);
            cmbTalla.setSelectedItem(v.talla);
            cmbColor.setSelectedItem(v.color);
            cargarImagenVariante(v.idVariante);
            if (v.precioCompra != null) {
                txtPrecioCompra.setText(v.precioCompra.toString());
            }
        }

        public void prefillWithProducto(ServiceCompra.ProductoBusqueda p) {
            this.varianteSeleccionada = null;
            this.idProductoActual = p.idProducto;
            this.nombreProductoActual = p.nombre;
            lblProductoSel.setText(p.getDescripcion());
            txtEan.setText("");
            cargarTallasYColoresDeProducto(p.idProducto);
            cmbTalla.setSelectedItem(null);
            cmbColor.setSelectedItem(null);
            lblImagen.setIcon(null);
        }

        public SeleccionCompra getSeleccion() {
            return seleccion;
        }

        private void initUI() {
            setLayout(new BorderLayout(10, 10));
            JPanel panelTop = new JPanel(new GridBagLayout());
            panelTop.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(8, 10, 8, 10);
            g.fill = GridBagConstraints.HORIZONTAL;
            g.gridx = 0;
            g.gridy = 0;
            panelTop.add(new JLabel("EAN/SKU:"), g);
            g.gridx = 1;
            txtEan = new JTextField(20);
            txtEan.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
            panelTop.add(txtEan, g);
            g.gridx = 2;
            btnBuscarEan = new JButton("Buscar");
            btnBuscarEan.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 16, UIManager.getColor("TabbedPane.foreground")));
            btnBuscarEan.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
            btnBuscarEan.addActionListener(e -> onBuscarEan());
            panelTop.add(btnBuscarEan, g);
            g.gridx = 3;
            btnBuscarProducto = new JButton("Lupa");
            btnBuscarProducto
                    .setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 16, UIManager.getColor("TabbedPane.foreground")));
            btnBuscarProducto.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
            btnBuscarProducto.addActionListener(e -> onAbrirBuscador());
            panelTop.add(btnBuscarProducto, g);
            g.gridx = 0;
            g.gridy = 1;
            panelTop.add(new JLabel("Producto:"), g);
            g.gridx = 1;
            g.gridwidth = 3;
            lblProductoSel = new JLabel("Sin seleccionar");
            lblProductoSel.setFont(lblProductoSel.getFont().deriveFont(Font.BOLD));
            panelTop.add(lblProductoSel, g);
            add(panelTop, BorderLayout.NORTH);

            JPanel panelCenter = new JPanel(new GridBagLayout());
            panelCenter.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8, 10, 8, 10);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            panelCenter.add(new JLabel("Talla:"), c);
            c.gridx = 1;
            cmbTalla = new JComboBox<>();
            cmbTalla.setEditable(true);
            cmbTalla.setPreferredSize(new Dimension(200, 35));
            cmbTalla.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
            panelCenter.add(cmbTalla, c);
            c.gridx = 2;
            panelCenter.add(new JLabel("Color:"), c);
            c.gridx = 3;
            cmbColor = new JComboBox<>();
            cmbColor.setEditable(true);
            cmbColor.setPreferredSize(new Dimension(200, 35));
            cmbColor.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
            panelCenter.add(cmbColor, c);
            c.gridx = 0;
            c.gridy = 1;
            panelCenter.add(new JLabel("Imagen variante:"), c);
            c.gridx = 1;
            lblImagen = new JLabel();
            lblImagen.setPreferredSize(new Dimension(200, 200));
            lblImagen.setBorder(BorderFactory.createLineBorder(UIManager.getColor("TabbedPane.darkShadow")));
            panelCenter.add(lblImagen, c);
            c.gridx = 2;
            btnSeleccionarImagen = new JButton("Seleccionar imagen");
            btnSeleccionarImagen.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
            btnSeleccionarImagen.addActionListener(e -> onSeleccionarImagen());
            panelCenter.add(btnSeleccionarImagen, c);
            c.gridx = 0;
            c.gridy = 2;
            panelCenter.add(new JLabel("Cantidad:"), c);
            c.gridx = 1;
            spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
            panelCenter.add(spCantidad, c);
            c.gridx = 2;
            panelCenter.add(new JLabel("Tipo:"), c);
            c.gridx = 3;
            cmbTipo = new JComboBox<>(new String[] { "par", "caja" });
            panelCenter.add(cmbTipo, c);
            c.gridx = 0;
            c.gridy = 3;
            panelCenter.add(new JLabel("Precio por par:"), c);
            c.gridx = 1;
            txtPrecioCompra = new JTextField("0");
            txtPrecioCompra.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
            panelCenter.add(txtPrecioCompra, c);
            c.gridx = 2;
            c.gridy = 3;
            panelCenter.add(new JLabel("Total pares:"), c);
            c.gridx = 3;
            JLabel lblParesCalc = new JLabel("0");
            panelCenter.add(lblParesCalc, c);
            c.gridx = 2;
            c.gridy = 4;
            panelCenter.add(new JLabel("Total calculado:"), c);
            c.gridx = 3;
            JLabel lblTotalCalc = new JLabel(FORMATO_MONEDA.format(BigDecimal.ZERO));
            panelCenter.add(lblTotalCalc, c);
            add(panelCenter, BorderLayout.CENTER);

            JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            btnCancelar = new JButton("Cancelar");
            btnCancelar.addActionListener(e -> dispose());
            btnCrearVariante = new JButton("Crear variante");
            btnCrearVariante.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 16, Color.WHITE));
            btnCrearVariante.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AZUL);
            btnCrearVariante.addActionListener(e -> onCrearVariante());
            btnAceptar = new JButton("Agregar");
            btnAceptar.setIcon(FontIcon.of(FontAwesomeSolid.CHECK, 16, Color.WHITE));
            btnAceptar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_VERDE);
            btnAceptar.addActionListener(e -> onAceptar());
            panelBottom.add(btnCancelar);
            panelBottom.add(btnCrearVariante);
            panelBottom.add(btnAceptar);
            add(panelBottom, BorderLayout.SOUTH);

            java.util.function.Consumer<Void> updater = v -> {
                try {
                    int cantidadSel = (Integer) spCantidad.getValue();
                    String tipoSel = (String) cmbTipo.getSelectedItem();
                    BigDecimal precioSel = new BigDecimal(txtPrecioCompra.getText().trim());
                    int pares = "caja".equalsIgnoreCase(tipoSel) ? cantidadSel * 24 : cantidadSel;
                    lblParesCalc.setText(String.valueOf(pares));
                    lblTotalCalc.setText(FORMATO_MONEDA.format(precioSel.multiply(BigDecimal.valueOf(pares))));
                } catch (Exception ex) {
                    lblParesCalc.setText("0");
                    lblTotalCalc.setText(FORMATO_MONEDA.format(BigDecimal.ZERO));
                }
                return;
            };

            spCantidad.addChangeListener(e -> updater.accept(null));
            cmbTipo.addActionListener(e -> updater.accept(null));
            txtPrecioCompra.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    updater.accept(null);
                }
            });
        }

        private void onBuscarEan() {
            String e = txtEan.getText().trim();
            if (e.isEmpty())
                return;
            ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
            ComboItem bItem = (ComboItem) cmbBodega.getSelectedItem();
            int idProvSel = provItem != null ? provItem.getId() : 0;
            int idBodSel = bItem != null ? bItem.getId() : 0;
            if (idProvSel <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione un proveedor");
                return;
            }
            if (idBodSel <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione una bodega");
                return;
            }
            try {
                Optional<VarianteBusqueda> v = serviceCompraDialog.buscarPorEan(e, idBodSel);
                if (v.isPresent()) {
                    try {
                        ServiceProduct sp = new ServiceProduct();
                        raven.controlador.productos.ModelProduct mp = sp.getProductById(v.get().idProducto);
                        int provProd = (mp != null && mp.getSupplier() != null) ? mp.getSupplier().getSupplierId() : 0;
                        if (provProd != idProvSel) {
                            Toast.show(this, Toast.Type.WARNING, "El producto pertenece a otro proveedor");
                        } else {
                            prefillWithVariante(v.get());
                        }
                    } catch (Exception ex2) {
                        Toast.show(this, Toast.Type.ERROR, "Error validando proveedor del producto");
                    }
                } else {
                    Toast.show(this, Toast.Type.WARNING, "Producto no encontrado: " + e);
                }
            } catch (SQLException ex) {
                Toast.show(this, Toast.Type.ERROR, "Error al buscar: " + ex.getMessage());
            }
        }

        private void onAbrirBuscador() {
            ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
            int idProv = provItem != null ? provItem.getId() : 0;
            if (idProv <= 0) {
                JOptionPane.showMessageDialog(this, "Seleccione un proveedor", "Proveedor requerido",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            ComboItem bItem = (ComboItem) cmbBodega.getSelectedItem();
            int idBod = bItem != null ? bItem.getId() : 0;
            if (idBod <= 0) {
                JOptionPane.showMessageDialog(this, "Seleccione una bodega", "Bodega requerida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            BuscadorProductoCompraDialog buscador = new BuscadorProductoCompraDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this), idBod, idUsuarioDialog, idProv,
                    serviceCompraDialog);
            buscador.setVisible(true);
            if (buscador.getVarianteSeleccionada() != null) {
                prefillWithVariante(buscador.getVarianteSeleccionada());
            }
        }

        private void cargarTallasYColoresDeProducto(int idProducto) {
            try {
                ModelProduct prod = serviceProductDialog.getProductById(idProducto);
                String genero = prod != null ? prod.getGender() : null;

                java.util.List<raven.controlador.productos.ModelSize> tallas;
                java.util.List<raven.controlador.productos.ModelColor> colores;

                if (genero != null && !genero.trim().isEmpty()) {
                    tallas = serviceSizeDialog.getTallasByGenero(genero);
                } else {
                    tallas = serviceSizeDialog.getAll();
                }
                colores = serviceColorDialog.getAll();

                cmbTalla.removeAllItems();
                cmbColor.removeAllItems();

                for (raven.controlador.productos.ModelSize t : tallas) {
                    String nombreTalla = (t.getSistema() != null && t.getGenero() != null)
                            ? (t.getNumero() + " " + t.getSistema() + " " + t.getGenero())
                            : t.getNumero();
                    cmbTalla.addItem(nombreTalla);
                }

                for (raven.controlador.productos.ModelColor c2 : colores) {
                    cmbColor.addItem(c2.getNombre());
                }
            } catch (SQLException ignore) {
            }
        }

        private void cargarImagenVariante(int idVar) {
            try {
                byte[] img = serviceVariantDialog.getVariantImage(idVar);
                if (img != null && img.length > 0) {
                    ImageIcon icon = new ImageIcon(img);
                    Image scaled = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    lblImagen.setIcon(new ImageIcon(scaled));
                } else {
                    lblImagen.setIcon(null);
                }
            } catch (SQLException ignore) {
            }
        }

        private void onSeleccionarImagen() {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                java.io.File f = chooser.getSelectedFile();
                try {
                    imagenSeleccionada = java.nio.file.Files.readAllBytes(f.toPath());
                    ImageIcon icon = new ImageIcon(imagenSeleccionada);
                    Image scaled = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    lblImagen.setIcon(new ImageIcon(scaled));
                } catch (Exception ex) {
                    Toast.show(this, Toast.Type.ERROR, "Error cargando imagen: " + ex.getMessage());
                }
            }
        }

        private void onAceptar() {
            if (idProductoActual <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione un producto");
                return;
            }
            ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
            int idProvSel = provItem != null ? provItem.getId() : 0;
            if (idProvSel <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione un proveedor");
                return;
            }
            String tallaSel = (String) cmbTalla.getSelectedItem();
            String colorSel = (String) cmbColor.getSelectedItem();
            int cantidadSel = (Integer) spCantidad.getValue();
            String tipoSel = (String) cmbTipo.getSelectedItem();
            BigDecimal precioSel;
            try {
                precioSel = new BigDecimal(txtPrecioCompra.getText().trim());
            } catch (Exception ex) {
                Toast.show(this, Toast.Type.ERROR, "Precio inválido");
                return;
            }

            int idProducto = idProductoActual;
            int idTalla;
            int idColor;
            try {
                idTalla = serviceSizeDialog.getSizeIdByName(tallaSel);
                idColor = serviceColorDialog.getColorIdByName(colorSel);
            } catch (SQLException ex) {
                Toast.show(this, Toast.Type.ERROR, ex.getMessage());
                return;
            }

            VarianteBusqueda varianteFinal = null;
            try {
                List<VarianteBusqueda> vars = serviceCompraDialog.obtenerVariantesProductoEnBodega(idProducto,
                        idBodegaDialog);
                for (VarianteBusqueda v : vars) {
                    if (v.idTalla == idTalla && v.idColor == idColor && v.idProveedor == idProvSel) {
                        varianteFinal = v;
                        break;
                    }
                }
            } catch (SQLException ignore) {
            }

            if (varianteFinal == null) {
                raven.controlador.productos.ModelProductVariant mv = new raven.controlador.productos.ModelProductVariant();
                mv.setProductId(idProducto);
                mv.setSizeId(idTalla);
                mv.setColorId(idColor);
                mv.setSizeName(tallaSel);
                mv.setColorName(colorSel);
                mv.setPurchasePrice(precioSel.doubleValue());
                mv.setSalePrice(0.0);
                mv.setMinStock(1);
                mv.setSupplierId(idProvSel);
                if ("par".equalsIgnoreCase(tipoSel)) {
                    mv.setStockPairs(cantidadSel);
                    mv.setStockBoxes(0);
                } else {
                    mv.setStockBoxes(cantidadSel);
                    mv.setStockPairs(0);
                }
                mv.setWarehouseId(idBodegaDialog);
                if (imagenSeleccionada != null && imagenSeleccionada.length > 0) {
                    mv.setImageBytes(imagenSeleccionada);
                }
                int idNueva = serviceProductDialog.createVariantForProduct(idProducto, mv, idProvSel, idUsuarioDialog);
                if (idNueva <= 0) {
                    Toast.show(this, Toast.Type.ERROR, "No se pudo crear la variante");
                    return;
                }
                varianteFinal = new VarianteBusqueda();
                varianteFinal.idVariante = idNueva;
                varianteFinal.idProducto = idProducto;
                varianteFinal.talla = tallaSel;
                varianteFinal.color = colorSel;
                varianteFinal.ean = mv.getEan();
                varianteFinal.sku = mv.getSku();
                varianteFinal.precioCompra = precioSel;
                varianteFinal.nombreProducto = nombreProductoActual;
            } else {
                try {
                    byte[] actual = serviceVariantDialog.getVariantImage(varianteFinal.idVariante);
                    if (imagenSeleccionada != null && actual == null) {
                        serviceVariantDialog.updateVariantImage(varianteFinal.idVariante, imagenSeleccionada);
                        cargarImagenVariante(varianteFinal.idVariante);
                    }
                } catch (SQLException ignore) {
                }
                if (varianteFinal.precioCompra == null) {
                    varianteFinal.precioCompra = precioSel;
                }
                varianteFinal.nombreProducto = nombreProductoActual;
            }

            seleccion = new SeleccionCompra();
            seleccion.variante = varianteFinal;
            seleccion.cantidad = cantidadSel;
            seleccion.tipo = tipoSel;
            seleccion.precio = precioSel;
            dispose();
        }

        private void onCrearVariante() {
            if (idProductoActual <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione un producto");
                return;
            }
            ComboItem provItem = (ComboItem) cmbProveedor.getSelectedItem();
            int idProvSel = provItem != null ? provItem.getId() : 0;
            if (idProvSel <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione un proveedor");
                return;
            }

            String tallaSel = (String) cmbTalla.getSelectedItem();
            String colorSel = (String) cmbColor.getSelectedItem();
            BigDecimal precioSel;
            try {
                precioSel = new BigDecimal(txtPrecioCompra.getText().trim());
            } catch (Exception ex) {
                Toast.show(this, Toast.Type.ERROR, "Precio inválido");
                return;
            }

            int idTalla;
            int idColor;
            try {
                idTalla = serviceSizeDialog.getSizeIdByName(tallaSel);
                idColor = serviceColorDialog.getColorIdByName(colorSel);
            } catch (SQLException ex) {
                Toast.show(this, Toast.Type.ERROR, ex.getMessage());
                return;
            }

            raven.controlador.productos.ModelProductVariant mv = new raven.controlador.productos.ModelProductVariant();
            mv.setProductId(idProductoActual);
            mv.setSizeId(idTalla);
            mv.setColorId(idColor);
            mv.setSizeName(tallaSel);
            mv.setColorName(colorSel);
            mv.setPurchasePrice(precioSel.doubleValue());
            mv.setSalePrice(0.0);
            mv.setMinStock(1);
            mv.setStockPairs(0);
            mv.setStockBoxes(0);
            mv.setWarehouseId(idBodegaDialog);
            mv.setSupplierId(idProvSel);
            if (imagenSeleccionada != null && imagenSeleccionada.length > 0) {
                mv.setImageBytes(imagenSeleccionada);
            }
            int idNueva = serviceProductDialog.createVariantForProduct(idProductoActual, mv, idProvSel,
                    idUsuarioDialog);
            if (idNueva <= 0) {
                Toast.show(this, Toast.Type.ERROR, "No se pudo crear la variante");
                return;
            }
            VarianteBusqueda v = new VarianteBusqueda();
            v.idVariante = idNueva;
            v.idProducto = idProductoActual;
            v.talla = tallaSel;
            v.color = colorSel;
            v.ean = mv.getEan();
            v.sku = mv.getSku();
            v.precioCompra = precioSel;
            v.nombreProducto = nombreProductoActual;
            varianteSeleccionada = v;

            cargarTallasYColoresDeProducto(idProductoActual);
            cargarImagenVariante(idNueva);
            Toast.show(this, Toast.Type.SUCCESS, "Variante creada");
        }
    }
}
