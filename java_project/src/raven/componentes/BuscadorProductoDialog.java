package raven.componentes;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.admin.UserSession;
import raven.controlador.principal.conexion;
import raven.theme.FlatLafConfig;

/**
 * Diálogo de búsqueda rápida de productos para facturación.
 * Con filtros avanzados por stock, marca y talla.
 * Estilizado con FlatLaf.
 * 
 * @author Sistema
 * @version 4.0
 */
public class BuscadorProductoDialog extends JDialog {

    // ==================== COLORES USANDO FLATLAF ====================
    private Color accentColor;
    private Color dangerColor;
    private Color bgColor;
    private Color fgColor;
    private Color secondaryColor;
    private Color tableAltColor;

    // ==================== MODELO DE RESULTADO ====================
    public static class ProductoSeleccionado {
        private final int idVariante;
        private final int idProducto;
        private final int idBodega; // SUCCESS  Nuevo campo para validación
        private final String nombre;
        private final String codigoModelo;
        private final String ean;
        private final String sku;
        private final String talla;
        private final String color;
        private final int stockPares;
        private final int stockCajas;
        private final BigDecimal precioVenta;
        private final String marca;
        private final String numeroTraspaso;

        public ProductoSeleccionado(int idVariante, int idProducto, int idBodega, String nombre,
                String codigoModelo, String ean, String sku, String talla,
                String color, int stockPares, int stockCajas, BigDecimal precioVenta, String marca) {
            this.idVariante = idVariante;
            this.idProducto = idProducto;
            this.idBodega = idBodega;
            this.nombre = nombre;
            this.codigoModelo = codigoModelo;
            this.ean = ean;
            this.sku = sku;
            this.talla = talla;
            this.color = color;
            this.stockPares = stockPares;
            this.stockCajas = stockCajas;
            this.precioVenta = precioVenta;
            this.marca = marca;
            this.numeroTraspaso = null;
        }

        public ProductoSeleccionado(int idVariante, int idProducto, int idBodega, String nombre,
                String codigoModelo, String ean, String sku, String talla,
                String color, int stockPares, int stockCajas, BigDecimal precioVenta, String marca, String numeroTraspaso) {
            this.idVariante = idVariante;
            this.idProducto = idProducto;
            this.idBodega = idBodega;
            this.nombre = nombre;
            this.codigoModelo = codigoModelo;
            this.ean = ean;
            this.sku = sku;
            this.talla = talla;
            this.color = color;
            this.stockPares = stockPares;
            this.stockCajas = stockCajas;
            this.precioVenta = precioVenta;
            this.marca = marca;
            this.numeroTraspaso = numeroTraspaso;
        }

        public int getIdVariante() {
            return idVariante;
        }

        public int getIdProducto() {
            return idProducto;
        }

        public int getIdBodega() {
            return idBodega;
        }

        public String getNombre() {
            return nombre;
        }

        public String getCodigoModelo() {
            return codigoModelo;
        }

        public String getEan() {
            return ean;
        }

        public String getSku() {
            return sku;
        }

        public String getTalla() {
            return talla;
        }

        public String getColor() {
            return color;
        }

        public int getStockPares() {
            return stockPares;
        }

        public int getStockCajas() {
            return stockCajas;
        }

        public BigDecimal getPrecioVenta() {
            return precioVenta;
        }

        public String getMarca() {
            return marca;
        }

        public String getNumeroTraspaso() {
            return numeroTraspaso;
        }

        public String getIdentificador() {
            if (ean != null && !ean.trim().isEmpty())
                return ean;
            if (sku != null && !sku.trim().isEmpty())
                return sku;
            if (codigoModelo != null && !codigoModelo.trim().isEmpty())
                return codigoModelo;
            return String.valueOf(idVariante);
        }
    }

    // ==================== CLASE PARA PRODUCTO CONFIGURADO (BULK) ====================
    public static class ProductoConfigurado {
        private final ProductoSeleccionado producto;
        private int cantidad;
        private String tipo; // "par" o "caja"

        public ProductoConfigurado(ProductoSeleccionado producto, int cantidad, String tipo) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.tipo = tipo;
        }

        public ProductoSeleccionado getProducto() { return producto; }
        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }

    // ==================== COMPONENTES ====================
    private JTextField txtBuscar;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private Consumer<ProductoSeleccionado> callback;
    private Consumer<List<ProductoConfigurado>> callbackMulti; // Callback para selección múltiple
    private Runnable onImportExcel;
    private Runnable onDownloadTemplate;
    private Integer idBodega;
    private Timer timerBusqueda;
    private JLabel lblContador;
    private JButton btnVerSeleccion; // Botón para ver selección múltiple

    // ==================== FILTROS ====================
    private JCheckBox chkSoloConStock;
    private JComboBox<ComboItem> cmbMarca;
    private JComboBox<ComboItem> cmbTalla;
     private JComboBox<ComboItem> cmbTraspaso; // Nuevo filtro de traspasos
     private boolean modoTraspasos = false;
     private JButton btnSeleccionarTodos; // Nuevo botón seleccionar todos
     private JButton btnElegirTraspasos;
     private boolean abrirSelectorTraspasos = false;
     private final List<Integer> traspasosSeleccionados = new ArrayList<>();
     private List<ComboItem> traspasosDisponibles = new ArrayList<>();

    public void setModoTraspasos(boolean modoTraspasos) {
        this.modoTraspasos = modoTraspasos;
        if (modoTraspasos) {
            setTitle("Buscador de Productos (Modo Traspasos)");
            chkSoloConStock.setSelected(true);
            chkSoloConStock.setEnabled(false); // Forzar stock > 0
            lblContador.setText("Modo Traspasos: Solo productos con stock recibido.");
            
            // Mostrar controles específicos de traspasos
            if (cmbTraspaso != null) cmbTraspaso.setVisible(true);
            if (btnElegirTraspasos != null) btnElegirTraspasos.setVisible(true);
            if (btnSeleccionarTodos != null) btnSeleccionarTodos.setVisible(true);
            cargarTraspasos();
            reiniciarTimer(); // Iniciar búsqueda automática

            if (abrirSelectorTraspasos) {
                SwingUtilities.invokeLater(() -> {
                    abrirSelectorTraspasos = false;
                    mostrarSelectorTraspasos();
                });
            }
        }
    }

    // ==================== COLUMNAS ====================
    private static final String[] COLUMNAS = {
            "Código", "Producto", "Marca", "Talla", "Color", "Stock Par", "Stock Caja", "Precio"
    };

    private List<ProductoSeleccionado> productosEnLista = new ArrayList<>();
    private List<ProductoSeleccionado> seleccionadosMulti = new ArrayList<>(); // Lista para selección múltiple

    // ==================== CACHE DE IMÁGENES ====================
    private final java.util.Map<Integer, ImageIcon> imagenCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final ImageIcon placeholderIcon;
    private final java.util.concurrent.ExecutorService imageLoader = java.util.concurrent.Executors.newFixedThreadPool(3);
    private final List<java.util.concurrent.Future<?>> activeImageTasks = java.util.Collections.synchronizedList(new ArrayList<>());

    // ==================== CLASE AUXILIAR PARA PRODUCTO CON IMAGEN ====================
    private static class ProductoConImagen {
        private ImageIcon imagen;
        private final String nombre;

        public ProductoConImagen(ImageIcon imagen, String nombre) {
            this.imagen = imagen;
            this.nombre = nombre;
        }

        public ImageIcon getImagen() {
            return imagen;
        }

        public void setImagen(ImageIcon imagen) {
            this.imagen = imagen;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // ==================== CLASE AUXILIAR PARA COMBOS ====================
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

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // ==================== CONSTRUCTOR ====================
    private BuscadorProductoDialog(Window parent, Integer idBodega, Consumer<ProductoSeleccionado> callback, Consumer<List<ProductoConfigurado>> callbackMulti, Runnable onImportExcel, Runnable onDownloadTemplate) {
        super(parent, "Buscar Producto", ModalityType.APPLICATION_MODAL);
        this.idBodega = idBodega;
        this.callback = callback;
        this.callbackMulti = callbackMulti;
        this.onImportExcel = onImportExcel;
        this.onDownloadTemplate = onDownloadTemplate;

        // Crear placeholder icon (imagen gris simple)
        placeholderIcon = crearPlaceholderIcon();

        initColors();
        initComponents();
        configurarEventos();
        cargarFiltros();

        setSize(1050, 650);
        setLocationRelativeTo(parent);
    }

    /**
     * Crea un ícono placeholder simple para mostrar mientras se cargan las imágenes
     */
    private ImageIcon crearPlaceholderIcon() {
        int width = 50;
        int height = 50;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = img.createGraphics();

        // Fondo gris claro
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(0, 0, width, height);

        // Borde
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRect(0, 0, width - 1, height - 1);

        // Icono de imagen simple (rectángulo y círculo)
        g2d.setColor(new Color(180, 180, 180));
        g2d.fillRect(15, 20, 20, 15);
        g2d.fillOval(20, 12, 8, 8);

        g2d.dispose();
        return new ImageIcon(img);
    }

    private void initColors() {
        accentColor = FlatLafConfig.ACCENT_BLUE;
        dangerColor = new Color(220, 53, 69);

        // Obtener colores del tema actual (respetando Dark/Light mode)
        bgColor = UIManager.getColor("Table.background");
        fgColor = UIManager.getColor("Table.foreground");
        secondaryColor = UIManager.getColor("Label.disabledForeground");
        tableAltColor = UIManager.getColor("Table.alternateRowColor"); // FlatLaf maneja esto automáticamente

        // Fallbacks seguros que no rompen el tema oscuro
        if (bgColor == null)
            bgColor = Color.WHITE;
        if (fgColor == null)
            fgColor = Color.BLACK;
        if (secondaryColor == null)
            secondaryColor = Color.GRAY;
        // Si no hay color alternativo definido, usar el mismo fondo (sin alternancia
        // forzada)
        if (tableAltColor == null)
            tableAltColor = bgColor;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ========== HEADER CON TÍTULO ==========
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(FontIcon.of(FontAwesomeSolid.BOXES, 28, accentColor));
        JLabel titleLabel = new JLabel("Buscar Productos");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        titleLabel.setForeground(fgColor);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // ========== PANEL DE BÚSQUEDA Y FILTROS ==========
        JPanel searchFilterPanel = new JPanel(new BorderLayout(0, 10));
        searchFilterPanel.setOpaque(false);

        // Campo de búsqueda
        JPanel searchPanel = new JPanel(new BorderLayout(15, 0));
        searchPanel.setOpaque(false);

        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Escriba nombre, código, EAN o SKU...");
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                FontIcon.of(FontAwesomeSolid.SEARCH, 16, secondaryColor));
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtBuscar.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;minimumWidth:400;margin:8,12,8,12");
        txtBuscar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        lblContador = new JLabel("0 resultados");
        lblContador.setForeground(secondaryColor);
        lblContador.putClientProperty(FlatClientProperties.STYLE, "font:-1");
        lblContador.setIcon(FontIcon.of(FontAwesomeSolid.LIST, 14, secondaryColor));

        searchPanel.add(txtBuscar, BorderLayout.CENTER);
        searchPanel.add(lblContador, BorderLayout.EAST);

        // Panel de filtros
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterPanel.setOpaque(false);
        filterPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:$Table.alternateRowColor");
        filterPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Checkbox solo con stock
        chkSoloConStock = new JCheckBox("Solo con stock");
        chkSoloConStock.setSelected(true);
        chkSoloConStock.setOpaque(false);
        chkSoloConStock.setIcon(FontIcon.of(FontAwesomeSolid.CUBES, 16, accentColor));
        chkSoloConStock.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, accentColor));
        chkSoloConStock.putClientProperty(FlatClientProperties.STYLE,
                "font:bold;" +
                        "iconTextGap:8");

        // ComboBox marca
        JLabel lblMarca = new JLabel("Marca:");
        lblMarca.setIcon(FontIcon.of(FontAwesomeSolid.TAG, 16, accentColor));
        lblMarca.putClientProperty(FlatClientProperties.STYLE, "font:bold;iconTextGap:6");

        cmbMarca = new JComboBox<>();
        cmbMarca.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;" +
                        "minimumWidth:180;" +
                        "buttonStyle:none");
        cmbMarca.addItem(new ComboItem(0, "Todas las marcas"));

        // ComboBox talla
        JLabel lblTalla = new JLabel("Talla:");
        lblTalla.setIcon(FontIcon.of(FontAwesomeSolid.RULER, 16, accentColor));
        lblTalla.putClientProperty(FlatClientProperties.STYLE, "font:bold;iconTextGap:6");

        cmbTalla = new JComboBox<>();
        cmbTalla.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;" +
                        "minimumWidth:130;" +
                        "buttonStyle:none");
        cmbTalla.addItem(new ComboItem(0, "Todas"));

        // ComboBox Traspaso (Oculto por defecto)
        cmbTraspaso = new JComboBox<>();
        cmbTraspaso.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;" +
                        "minimumWidth:200;" +
                        "buttonStyle:none");
        cmbTraspaso.addItem(new ComboItem(0, "Todos los traspasos"));
        cmbTraspaso.setVisible(false);
        cmbTraspaso.addActionListener(e -> {
            if (cmbTraspaso.getSelectedItem() instanceof ComboItem) {
                ComboItem sel = (ComboItem) cmbTraspaso.getSelectedItem();
                if (sel.getId() > 0) {
                    traspasosSeleccionados.clear();
                    actualizarTextoTraspasosSeleccionados();
                }
            }
            reiniciarTimer();
        });

        btnElegirTraspasos = new JButton("Elegir traspasos");
        btnElegirTraspasos.setVisible(false);
        btnElegirTraspasos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnElegirTraspasos.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;" +
                        "margin:6,14,6,14;" +
                        "font:bold -1;" +
                        "hoverBackground:darken($Button.background,10%)");
        btnElegirTraspasos.setIcon(FontIcon.of(FontAwesomeSolid.TRUCK_LOADING, 14, Color.WHITE));
        btnElegirTraspasos.setBackground(new Color(23, 162, 184));
        btnElegirTraspasos.setForeground(Color.WHITE);
        btnElegirTraspasos.addActionListener(e -> mostrarSelectorTraspasos());

        // Botón limpiar filtros con estilo mejorado
        JButton btnLimpiarFiltros = new JButton("Limpiar filtros");

        btnLimpiarFiltros.setIcon(FontIcon.of(FontAwesomeSolid.SYNC_ALT, 14, Color.WHITE));
        btnLimpiarFiltros.setBackground(new Color(108, 117, 125));
        btnLimpiarFiltros.setForeground(Color.WHITE);
        btnLimpiarFiltros.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiarFiltros.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;" +
                        "margin:6,16,6,16;" +
                        "font:bold -1;" +
                        "hoverBackground:darken($Button.background,10%)");
        btnLimpiarFiltros.addActionListener(e -> limpiarFiltros());

        filterPanel.add(chkSoloConStock);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(lblMarca);
        filterPanel.add(cmbMarca);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(lblTalla);
        filterPanel.add(cmbTalla);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(cmbTraspaso);
        filterPanel.add(btnElegirTraspasos);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(btnLimpiarFiltros);

        searchFilterPanel.add(searchPanel, BorderLayout.NORTH);
        searchFilterPanel.add(filterPanel, BorderLayout.CENTER);

        // Header completo
        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setOpaque(false);
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(searchFilterPanel, BorderLayout.CENTER);

        // ========== TABLA DE RESULTADOS ==========
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaResultados = new JTable(modeloTabla);
        tablaResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaResultados.setShowGrid(false);
        tablaResultados.setIntercellSpacing(new Dimension(0, 0));
        tablaResultados.putClientProperty(FlatClientProperties.STYLE,
                "selectionBackground:$Component.accentColor;" +
                        "selectionForeground:$Button.default.foreground;" +
                        "showHorizontalLines:false;showVerticalLines:false");

        JTableHeader header = tablaResultados.getTableHeader();
        header.putClientProperty(FlatClientProperties.STYLE, "height:36;hoverBackground:null;font:bold");
        header.setReorderingAllowed(false);

        tablaResultados.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                // Manejo especial para columna de Producto (columna 1) con imagen
                if (column == 1 && value instanceof ProductoConImagen) {
                    ProductoConImagen prod = (ProductoConImagen) value;
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, prod.getNombre(),
                            isSelected, hasFocus, row, column);

                    // Establecer imagen (al lado izquierdo del texto)
                    label.setIcon(prod.getImagen());
                    label.setIconTextGap(8); // Espacio entre imagen y texto
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                    // Aplicar colores
                    if (!isSelected) {
                        if (row < productosEnLista.size() && seleccionadosMulti.contains(productosEnLista.get(row))) {
                            label.setBackground(new Color(209, 236, 241));
                            label.setForeground(Color.BLACK);
                        } else {
                            label.setBackground(row % 2 == 0 ? bgColor : tableAltColor);
                            label.setForeground(fgColor);
                        }
                    }

                    return label;
                }

                // Renderer normal para otras columnas
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // IMPORTANTE: Limpiar ícono para todas las columnas que NO sean Producto
                if (c instanceof JLabel) {
                    ((JLabel) c).setIcon(null);
                }

                if (!isSelected) {
                    if (row < productosEnLista.size() && seleccionadosMulti.contains(productosEnLista.get(row))) {
                        c.setBackground(new Color(209, 236, 241)); // Azul claro selección múltiple
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(row % 2 == 0 ? bgColor : tableAltColor);
                        c.setForeground(fgColor);
                    }
                }

                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                // Alineación según columna (ajustado sin columna de imagen)
                if (column >= 5 && column <= 7) {
                    // Columnas numéricas (Stock Par, Stock Caja, Precio) alineadas a la derecha
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    // Resto de columnas a la izquierda
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        // Anchos de columnas (sin columna separada de imagen)
        tablaResultados.getColumnModel().getColumn(0).setPreferredWidth(110); // Código
        tablaResultados.getColumnModel().getColumn(1).setPreferredWidth(280); // Producto (más ancho para imagen + texto)
        tablaResultados.getColumnModel().getColumn(2).setPreferredWidth(100); // Marca
        tablaResultados.getColumnModel().getColumn(3).setPreferredWidth(70);  // Talla
        tablaResultados.getColumnModel().getColumn(4).setPreferredWidth(90);  // Color
        tablaResultados.getColumnModel().getColumn(5).setPreferredWidth(70);  // Stock Par
        tablaResultados.getColumnModel().getColumn(6).setPreferredWidth(70);  // Stock Caja
        tablaResultados.getColumnModel().getColumn(7).setPreferredWidth(90);  // Precio

        // Establecer altura de fila más grande para acomodar la imagen
        tablaResultados.setRowHeight(55);

        JScrollPane scroll = new JScrollPane(tablaResultados);
        scroll.putClientProperty(FlatClientProperties.STYLE, "arc:12;borderWidth:1");
        scroll.getViewport().setBackground(bgColor);

        // ========== PANEL DE BOTONES (FOOTER) ==========
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Panel de acciones extra (izquierda)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        if (onImportExcel != null) {
            JButton btnExcel = crearBoton("Excel", FontAwesomeSolid.FILE_EXCEL, new Color(33, 115, 70));
            btnExcel.setToolTipText("Importar desde Excel");
            btnExcel.addActionListener(e -> onImportExcel.run());
            leftPanel.add(btnExcel);
        }

        if (onDownloadTemplate != null) {
            JButton btnTemplate = crearBoton("Plantilla", FontAwesomeSolid.DOWNLOAD, new Color(108, 117, 125));
            btnTemplate.setToolTipText("Descargar plantilla");
            btnTemplate.addActionListener(e -> onDownloadTemplate.run());
            leftPanel.add(btnTemplate);
        }

        // Panel de acciones principales (derecha)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        btnSeleccionarTodos = crearBoton("Todos", FontAwesomeSolid.CHECK_DOUBLE, new Color(40, 167, 69));
        btnSeleccionarTodos.setToolTipText("Seleccionar todos los productos visibles");
        btnSeleccionarTodos.setVisible(false);
        btnSeleccionarTodos.addActionListener(e -> seleccionarTodosVisibles());

        JButton btnSeleccionar = crearBoton("Seleccionar", FontAwesomeSolid.CHECK, accentColor);
        btnSeleccionar.addActionListener(e -> seleccionarProducto());

        btnVerSeleccion = crearBoton("Ver Selección (0)", FontAwesomeSolid.LIST_UL, new Color(23, 162, 184));
        btnVerSeleccion.setVisible(false);
        btnVerSeleccion.addActionListener(e -> mostrarModalConfiguracionMulti());

        JButton btnCancelar = crearBoton("Cancelar", FontAwesomeSolid.TIMES, dangerColor);
        btnCancelar.addActionListener(e -> dispose());

        rightPanel.add(btnSeleccionarTodos);
        rightPanel.add(btnVerSeleccion);
        rightPanel.add(btnSeleccionar);
        rightPanel.add(btnCancelar);

        footerPanel.add(leftPanel, BorderLayout.WEST);
        footerPanel.add(rightPanel, BorderLayout.EAST);

        // ========== ENSAMBLAR ==========
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        getRootPane().putClientProperty(FlatClientProperties.STYLE, "arc:12");
    }

    private JButton crearBoton(String texto, org.kordamp.ikonli.Ikon icono, Color color) {
        JButton btn = new JButton(texto);
        btn.setIcon(FontIcon.of(icono, 16, Color.WHITE));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE,
                "arc:12;" +
                        "margin:12,28,12,28;" +
                        "font:bold +1;" +
                        "iconTextGap:10;" +
                        "hoverBackground:darken($Button.background,10%);" +
                        "pressedBackground:darken($Button.background,20%)");
        return btn;
    }

    private void seleccionarTodosVisibles() {
        if (productosEnLista.isEmpty()) return;
        
        boolean huboCambios = false;
        for (ProductoSeleccionado p : productosEnLista) {
            if (!seleccionadosMulti.contains(p)) {
                seleccionadosMulti.add(p);
                huboCambios = true;
            }
        }
        
        if (huboCambios) {
            modeloTabla.fireTableDataChanged();
            actualizarBotonSeleccion();
        }
    }

    private void cargarTraspasos() {
        SwingWorker<List<ComboItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ComboItem> doInBackground() throws Exception {
                List<ComboItem> traspasos = new ArrayList<>();
                traspasos.add(new ComboItem(0, "Todos los traspasos"));
                
                try (Connection conn = conexion.getInstance().createConnection()) {
                    String sql = "SELECT id_traspaso, numero_traspaso, fecha_recepcion " +
                                 "FROM traspasos " +
                                 "WHERE id_bodega_destino = ? AND estado = 'recibido' " +
                                 "ORDER BY fecha_recepcion DESC LIMIT 200";
                                  
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, idBodega != null ? idBodega : 0);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String etiqueta = String.format("%s - %s", 
                                    rs.getString("numero_traspaso"),
                                    rs.getDate("fecha_recepcion"));
                                traspasos.add(new ComboItem(rs.getInt("id_traspaso"), etiqueta));
                            }
                        }
                    }
                }
                return traspasos;
            }

            @Override
            protected void done() {
                try {
                    List<ComboItem> items = get();
                    traspasosDisponibles = items;
                    cmbTraspaso.removeAllItems();
                    for (ComboItem item : items) {
                        cmbTraspaso.addItem(item);
                    }
                    actualizarTextoTraspasosSeleccionados();
                } catch (Exception e) {
                    System.err.println("Error cargando traspasos: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void actualizarTextoTraspasosSeleccionados() {
        if (btnElegirTraspasos == null) return;
        int n = traspasosSeleccionados.size();
        if (n <= 0) {
            btnElegirTraspasos.setText("Elegir traspasos");
            btnElegirTraspasos.setToolTipText(null);
            return;
        }
        btnElegirTraspasos.setText("Traspasos (" + n + ")");
        btnElegirTraspasos.setToolTipText("Filtrando por " + n + " traspaso(s)");
    }

    private void mostrarSelectorTraspasos() {
        if (traspasosDisponibles == null || traspasosDisponibles.size() <= 1) {
            cargarTraspasos();
            return;
        }

        JDialog dialog = new JDialog(this, "Seleccionar Traspasos", ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.setBackground(bgColor);

        DefaultListModel<ComboItem> model = new DefaultListModel<>();
        for (ComboItem item : traspasosDisponibles) {
            if (item.getId() > 0) model.addElement(item);
        }

        JList<ComboItem> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(14);

        java.util.List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            if (traspasosSeleccionados.contains(model.get(i).getId())) {
                indices.add(i);
            }
        }
        if (!indices.isEmpty()) {
            int[] idx = indices.stream().mapToInt(Integer::intValue).toArray();
            list.setSelectedIndices(idx);
        }

        JScrollPane scroll = new JScrollPane(list);
        content.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> list.clearSelection());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());

        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBackground(accentColor);
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.putClientProperty(FlatClientProperties.STYLE, "arc:10;font:bold");
        btnAceptar.addActionListener(e -> {
            traspasosSeleccionados.clear();
            for (ComboItem item : list.getSelectedValuesList()) {
                if (item.getId() > 0) traspasosSeleccionados.add(item.getId());
            }
            if (!traspasosSeleccionados.isEmpty()) {
                cmbTraspaso.setSelectedIndex(0);
            }
            actualizarTextoTraspasosSeleccionados();
            dialog.dispose();
            reiniciarTimer();
        });

        bottom.add(btnLimpiar);
        bottom.add(btnCancelar);
        bottom.add(btnAceptar);
        content.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void cargarFiltros() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<ComboItem> marcas = new ArrayList<>();
            private List<ComboItem> tallas = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = conexion.getInstance().createConnection()) {
                    // Cargar marcas
                    String sqlMarcas = "SELECT id_marca, nombre FROM marcas WHERE activo = 1 ORDER BY nombre";
                    try (PreparedStatement ps = conn.prepareStatement(sqlMarcas);
                            ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            marcas.add(new ComboItem(rs.getInt("id_marca"), rs.getString("nombre")));
                        }
                    }

                    // Cargar tallas con género (H)/(M)/(N)
                    String sqlTallas = "SELECT DISTINCT id_talla, " +
                            "CONCAT(numero, ' ', COALESCE(sistema,''), " +
                            "CASE genero WHEN 'HOMBRE' THEN ' (H)' WHEN 'MUJER' THEN ' (M)' WHEN 'NIÑO' THEN ' (N)' ELSE '' END) AS talla "
                            +
                            "FROM tallas WHERE activo = 1 ORDER BY numero, genero";
                    try (PreparedStatement ps = conn.prepareStatement(sqlTallas);
                            ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            tallas.add(new ComboItem(rs.getInt("id_talla"), rs.getString("talla").trim()));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                for (ComboItem item : marcas) {
                    cmbMarca.addItem(item);
                }
                for (ComboItem item : tallas) {
                    cmbTalla.addItem(item);
                }
            }
        };
        worker.execute();
    }

    private void limpiarFiltros() {
        chkSoloConStock.setSelected(true);
        cmbMarca.setSelectedIndex(0);
        cmbTalla.setSelectedIndex(0);
        txtBuscar.setText("");
        modeloTabla.setRowCount(0);
        productosEnLista.clear();
        lblContador.setText("0 resultados");
    }

    private void configurarEventos() {
        timerBusqueda = new Timer(300, e -> ejecutarBusqueda());
        timerBusqueda.setRepeats(false);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reiniciarTimer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reiniciarTimer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                reiniciarTimer();
            }
        });

        // Filtros disparan búsqueda
        chkSoloConStock.addActionListener(e -> reiniciarTimer());
        cmbMarca.addActionListener(e -> reiniciarTimer());
        cmbTalla.addActionListener(e -> reiniciarTimer());

        tablaResultados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isControlDown()) {
                    toggleSeleccionMultiple(tablaResultados.rowAtPoint(e.getPoint()));
                } else if (e.getClickCount() == 2) {
                    seleccionarProducto();
                }
            }
        });

        tablaResultados.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    seleccionarProducto();
            }
        });

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && modeloTabla.getRowCount() > 0) {
                    tablaResultados.requestFocus();
                    tablaResultados.setRowSelectionInterval(0, 0);
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && modeloTabla.getRowCount() > 0) {
                    tablaResultados.setRowSelectionInterval(0, 0);
                    seleccionarProducto();
                }
            }
        });

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void reiniciarTimer() {
        timerBusqueda.restart();
    }

    private void ejecutarBusqueda() {
        String texto = txtBuscar.getText().trim();

        // Verificar si hay filtros activos (marca o talla seleccionados)
        ComboItem marcaSel = (ComboItem) cmbMarca.getSelectedItem();
        ComboItem tallaSel = (ComboItem) cmbTalla.getSelectedItem();
        boolean hayFiltroActivo = (marcaSel != null && marcaSel.getId() > 0) ||
                (tallaSel != null && tallaSel.getId() > 0) || modoTraspasos; // Modo traspasos cuenta como filtro activo

        // Si no hay texto y no hay filtros activos, limpiar tabla
        if (texto.length() < 2 && !hayFiltroActivo) {
            modeloTabla.setRowCount(0);
            productosEnLista.clear();
            lblContador.setText("Escriba para buscar o seleccione un filtro");
            lblContador.setIcon(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 14, secondaryColor));
            return;
        }

        lblContador.setText("Buscando...");
        lblContador.setIcon(FontIcon.of(FontAwesomeSolid.SPINNER, 14, accentColor));

        SwingWorker<List<ProductoSeleccionado>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ProductoSeleccionado> doInBackground() throws Exception {
                return buscarEnBaseDatos(texto);
            }

            @Override
            protected void done() {
                try {
                    actualizarTabla(get());
                } catch (Exception e) {
                    System.err.println("ERROR  Error en búsqueda: " + e.getMessage());
                    lblContador.setText("Error de búsqueda");
                    lblContador.setIcon(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 14, dangerColor));
                }
            }
        };
        worker.execute();
    }

    private List<ProductoSeleccionado> buscarEnBaseDatos(String texto) throws SQLException {
        List<ProductoSeleccionado> resultados = new ArrayList<>();

        // Construir SQL dinámico según filtros
        StringBuilder sql = new StringBuilder();
        if (modoTraspasos) {
             sql.append("SELECT pv.id_variante, pv.id_producto, p.nombre, p.codigo_modelo, ");
        } else {
             sql.append("SELECT DISTINCT pv.id_variante, pv.id_producto, p.nombre, p.codigo_modelo, ");
        }
        sql.append("COALESCE(pv.ean, '') AS ean, COALESCE(pv.sku, '') AS sku, ");
        sql.append("COALESCE(pv.precio_venta, p.precio_venta) AS precio_venta, ");
        sql.append("CONCAT(COALESCE(t.numero, ''), ' ', COALESCE(t.sistema,'')) AS talla, ");
        sql.append("COALESCE(c.nombre, '') AS color, ");
        sql.append("COALESCE(ib.Stock_par, 0) AS Stock_par, ");
        sql.append("COALESCE(ib.Stock_caja, 0) AS Stock_caja, ");
        sql.append("COALESCE(m.nombre, '') AS marca, ");
        sql.append("pv.id_talla, p.id_marca ");
        if (modoTraspasos) {
            sql.append(", MAX(tr.numero_traspaso) AS numero_traspaso, MAX(tr.id_traspaso) AS id_traspaso_reciente ");
        }
        sql.append("FROM producto_variantes pv ");
        sql.append("INNER JOIN productos p ON pv.id_producto = p.id_producto AND p.activo = 1 ");
        sql.append(
                "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1 ");
        sql.append("LEFT JOIN tallas t ON pv.id_talla = t.id_talla ");
        sql.append("LEFT JOIN colores c ON pv.id_color = c.id_color ");
        sql.append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ");
        
        if (modoTraspasos) {
            sql.append("INNER JOIN traspaso_detalles td ON pv.id_variante = td.id_variante ");
            sql.append("INNER JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso AND tr.id_bodega_destino = ? AND tr.estado = 'recibido' ");
        }

        sql.append("WHERE 1=1 ");

        // Lista para parámetros dinámicos
        List<Object> params = new ArrayList<>();
        params.add(idBodega != null ? idBodega : 0); // Primer parámetro siempre es idBodega
        
        if (modoTraspasos) {
            params.add(idBodega != null ? idBodega : 0);
        }

        // Filtro de texto (Soporte para "Nombre, Color, Talla")
        boolean hayTexto = texto != null && texto.length() >= 1;
        if (hayTexto) {
            String[] partes = texto.split(",");
            
            // Parte 1: Nombre / General
            if (partes.length > 0 && !partes[0].trim().isEmpty()) {
                String p1 = "%" + partes[0].trim() + "%";
                sql.append("AND (LOWER(p.nombre) LIKE LOWER(?) ");
                sql.append("OR LOWER(p.codigo_modelo) LIKE LOWER(?) ");
                sql.append("OR LOWER(COALESCE(pv.ean,'')) LIKE LOWER(?) ");
                sql.append("OR LOWER(COALESCE(pv.sku,'')) LIKE LOWER(?)) ");
                params.add(p1);
                params.add(p1);
                params.add(p1);
                params.add(p1);
            }
            
            // Parte 2: Color (Opcional)
            if (partes.length > 1 && !partes[1].trim().isEmpty()) {
                String p2 = "%" + partes[1].trim() + "%";
                sql.append("AND LOWER(c.nombre) LIKE LOWER(?) ");
                params.add(p2);
            }
            
            // Parte 3: Talla (Opcional)
            if (partes.length > 2 && !partes[2].trim().isEmpty()) {
                String p3 = "%" + partes[2].trim() + "%";
                sql.append("AND (LOWER(t.numero) LIKE LOWER(?) OR LOWER(t.sistema) LIKE LOWER(?)) ");
                params.add(p3);
                params.add(p3);
            }
        }

        // Filtro de stock
        if (chkSoloConStock.isSelected()) {
            sql.append("AND (COALESCE(ib.Stock_par, 0) > 0 OR COALESCE(ib.Stock_caja, 0) > 0) ");
        }

        if (modoTraspasos) {
            if (!traspasosSeleccionados.isEmpty()) {
                sql.append("AND tr.id_traspaso IN (");
                for (int i = 0; i < traspasosSeleccionados.size(); i++) {
                    if (i > 0) sql.append(",");
                    sql.append("?");
                    params.add(traspasosSeleccionados.get(i));
                }
                sql.append(") ");
            } else if (cmbTraspaso != null && cmbTraspaso.getSelectedItem() instanceof ComboItem) {
                ComboItem selTr = (ComboItem) cmbTraspaso.getSelectedItem();
                if (selTr.getId() > 0) {
                    sql.append("AND tr.id_traspaso = ? ");
                    params.add(selTr.getId());
                }
            }
        }

        // Filtro de marca
        ComboItem marcaSeleccionada = (ComboItem) cmbMarca.getSelectedItem();
        if (marcaSeleccionada != null && marcaSeleccionada.getId() > 0) {
            sql.append("AND p.id_marca = ? ");
            params.add(marcaSeleccionada.getId());
        }

        // Filtro de talla
        ComboItem tallaSeleccionada = (ComboItem) cmbTalla.getSelectedItem();
        if (tallaSeleccionada != null && tallaSeleccionada.getId() > 0) {
            sql.append("AND pv.id_talla = ? ");
            params.add(tallaSeleccionada.getId());
        }

        if (modoTraspasos) {
            sql.append("GROUP BY pv.id_variante ");
            sql.append("ORDER BY MAX(tr.id_traspaso) DESC, p.nombre, t.numero LIMIT 150");
        } else {
            sql.append("ORDER BY p.nombre, t.numero LIMIT 100");
        }

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Asignar parámetros dinámicamente
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (modoTraspasos) {
                        resultados.add(new ProductoSeleccionado(
                                rs.getInt("id_variante"),
                                rs.getInt("id_producto"),
                                idBodega != null ? idBodega : 0,
                                rs.getString("nombre"),
                                rs.getString("codigo_modelo"),
                                rs.getString("ean"),
                                rs.getString("sku"),
                                rs.getString("talla"),
                                rs.getString("color"),
                                rs.getInt("Stock_par"),
                                rs.getInt("Stock_caja"),
                                rs.getBigDecimal("precio_venta"),
                                rs.getString("marca"),
                                rs.getString("numero_traspaso")));
                    } else {
                        resultados.add(new ProductoSeleccionado(
                                rs.getInt("id_variante"),
                                rs.getInt("id_producto"),
                                idBodega != null ? idBodega : 0,
                                rs.getString("nombre"),
                                rs.getString("codigo_modelo"),
                                rs.getString("ean"),
                                rs.getString("sku"),
                                rs.getString("talla"),
                                rs.getString("color"),
                                rs.getInt("Stock_par"),
                                rs.getInt("Stock_caja"),
                                rs.getBigDecimal("precio_venta"),
                                rs.getString("marca")));
                    }
                }
            }
        }

        return resultados;
    }

    private void actualizarTabla(List<ProductoSeleccionado> resultados) {
        // Cancelar tareas de imágenes pendientes para evitar saturación y errores
        cancelarCargasPendientes();

        modeloTabla.setRowCount(0);
        productosEnLista.clear();
        productosEnLista.addAll(resultados);

        // Agregar todas las filas con placeholder en la columna de Producto
        for (ProductoSeleccionado p : resultados) {
            // Crear objeto ProductoConImagen con placeholder inicial
            String nombreMostrar = p.getNombre();
            if (modoTraspasos && p.getNumeroTraspaso() != null && !p.getNumeroTraspaso().isEmpty()) {
                nombreMostrar = "[" + p.getNumeroTraspaso() + "] " + nombreMostrar;
            }
            ProductoConImagen prodConImagen = new ProductoConImagen(placeholderIcon, nombreMostrar);

            modeloTabla.addRow(new Object[] {
                    p.getIdentificador(),
                    prodConImagen,  // Producto con imagen (inicialmente placeholder)
                    p.getMarca(),
                    p.getTalla() != null ? p.getTalla().trim() : "",
                    p.getColor(),
                    p.getStockPares(),
                    p.getStockCajas(),
                    p.getPrecioVenta() != null ? String.format("$%,.0f", p.getPrecioVenta()) : "$0"
            });
        }

        lblContador.setText(resultados.size() + " resultado" + (resultados.size() != 1 ? "s" : ""));
        lblContador.setIcon(FontIcon.of(resultados.isEmpty() ? FontAwesomeSolid.INBOX : FontAwesomeSolid.CHECK_CIRCLE,
                14, resultados.isEmpty() ? secondaryColor : accentColor));

        if (!resultados.isEmpty()) {
            tablaResultados.setRowSelectionInterval(0, 0);
        }

        // Cargar imágenes asíncronamente después de mostrar la tabla
        cargarImagenesAsync(resultados);
    }

    private void cancelarCargasPendientes() {
        synchronized (activeImageTasks) {
            for (java.util.concurrent.Future<?> task : activeImageTasks) {
                if (!task.isDone() && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
            activeImageTasks.clear();
        }
    }

    /**
     * Carga las imágenes de los productos de forma asíncrona, una por una
     */
    private void cargarImagenesAsync(List<ProductoSeleccionado> productos) {
        for (ProductoSeleccionado producto : productos) {
            final int idVariante = producto.getIdVariante();

            // Enviar tarea al pool de hilos y guardarla para poder cancelarla
            java.util.concurrent.Future<?> task = imageLoader.submit(() -> {
                if (Thread.currentThread().isInterrupted()) return;
                
                try {
                    ImageIcon imagen = obtenerImagenProducto(idVariante);

                    if (Thread.currentThread().isInterrupted()) return;

                    // Actualizar tabla en el EDT (Event Dispatch Thread)
                    SwingUtilities.invokeLater(() -> {
                        // Buscar todas las filas que correspondan a esta variante
                        // Esto evita problemas si la tabla cambió de orden o contenido mientras cargaba
                        for (int i = 0; i < productosEnLista.size(); i++) {
                            if (productosEnLista.get(i).getIdVariante() == idVariante) {
                                // Obtener el objeto ProductoConImagen de la columna Producto (columna 1)
                                Object valorActual = modeloTabla.getValueAt(i, 1);
                                if (valorActual instanceof ProductoConImagen) {
                                    ProductoConImagen prodConImagen = (ProductoConImagen) valorActual;
                                    // Actualizar solo la imagen
                                    prodConImagen.setImagen(imagen);
                                    // Notificar a la tabla que el valor cambió
                                    modeloTabla.fireTableCellUpdated(i, 1);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    // Ignorar interrupciones normales
                }
            });
            
            activeImageTasks.add(task);
        }
    }

    /**
     * Obtiene la imagen de un producto desde la base de datos o cache
     */
    private ImageIcon obtenerImagenProducto(int idVariante) {
        // Verificar cache primero
        if (imagenCache.containsKey(idVariante)) {
            return imagenCache.get(idVariante);
        }

        ImageIcon icono = placeholderIcon;

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT imagen FROM producto_variantes WHERE id_variante = ? AND imagen IS NOT NULL")) {

            ps.setInt(1, idVariante);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Blob blob = rs.getBlob("imagen");
                    if (blob != null) {
                        byte[] imageBytes = blob.getBytes(1, (int) blob.length());
                        Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));

                        if (img != null) {
                            // Redimensionar imagen a 50x50
                            Image scaledImg = img.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                            icono = new ImageIcon(scaledImg);

                            // Guardar en cache
                            imagenCache.put(idVariante, icono);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo imagen de BD para variante " + idVariante + ": " + e.getMessage());
        }

        return icono;
    }

    private void seleccionarProducto() {
        int fila = tablaResultados.getSelectedRow();
        if (fila < 0 || fila >= productosEnLista.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la lista",
                    "Selección requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ProductoSeleccionado seleccionado = productosEnLista.get(fila);

        // ========== VALIDACIONES ROBUSTAS (Solicitadas por Usuario) ==========

        // 1. Validar parámetros obligatorios del producto
        if (seleccionado.getIdProducto() <= 0 || seleccionado.getIdVariante() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Error crítico: El producto seleccionado tiene datos inconsistentes (ID inválido).\nContacte a soporte.",
                    "Error de Datos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Validar contexto de Bodega
        if (this.idBodega == null || this.idBodega <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Error de sesión: No se ha identificado la bodega del usuario actual.",
                    "Error de Contexto", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Validar coincidencia de Bodega (Seguridad)
        if (seleccionado.getIdBodega() != this.idBodega) {
            JOptionPane.showMessageDialog(this,
                    "Error de seguridad: Intentando agregar un producto que no pertenece a la bodega actual.\n" +
                            "Bodega Usuario: " + this.idBodega + "\n" +
                            "Bodega Producto: " + seleccionado.getIdBodega(),
                    "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Validar Permisos de Usuario (UserSession)
        boolean tienePermisoVenta = UserSession.getInstance().hasPermission("generar venta");
        boolean tienePermisoGestion = UserSession.getInstance().hasPermission("gestion de productos");

        if (!tienePermisoVenta && !tienePermisoGestion) {
            JOptionPane.showMessageDialog(this,
                    "Error de seguridad: No tiene permisos suficientes (Venta o Gestión) para seleccionar productos.",
                    "Permiso Denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Validar disponibilidad (Opcional según flujo, pero recomendado)
        boolean sinStock = seleccionado.getStockPares() <= 0 && seleccionado.getStockCajas() <= 0;
        if (sinStock) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "El producto seleccionado NO tiene stock disponible en esta bodega.\n¿Desea continuar de todos modos?",
                    "Sin Stock", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // ========== SELECCIÓN EXITOSA ==========
        System.out.println("SUCCESS  [EXITO] Producto seleccionado y validado:");
        System.out.println("   Producto: " + seleccionado.getNombre());
        System.out.println("   ID Variante: " + seleccionado.getIdVariante());
        System.out.println("   Bodega: " + seleccionado.getIdBodega());
        System.out.println("   Stock Pares: " + seleccionado.getStockPares());

        dispose();

        if (callback != null) {
            callback.accept(seleccionado);
        }
    }

    private void toggleSeleccionMultiple(int row) {
        if (row < 0 || row >= productosEnLista.size()) return;

        ProductoSeleccionado p = productosEnLista.get(row);
        if (seleccionadosMulti.contains(p)) {
            seleccionadosMulti.remove(p);
        } else {
            seleccionadosMulti.add(p);
        }

        modeloTabla.fireTableRowsUpdated(row, row);
        actualizarBotonSeleccion();
    }

    private void actualizarBotonSeleccion() {
        int count = seleccionadosMulti.size();
        btnVerSeleccion.setText("Ver Selección (" + count + ")");
        btnVerSeleccion.setVisible(count > 0);
    }

    private void mostrarModalConfiguracionMulti() {
        if (seleccionadosMulti.isEmpty()) return;

        JDialog dialog = new JDialog(this, "Configurar Productos Seleccionados", ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        content.setBackground(bgColor);

        // Header
        JLabel lblTitulo = new JLabel("Configurar " + seleccionadosMulti.size() + " productos");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        content.add(lblTitulo, BorderLayout.NORTH);

        // Tabla de configuración
        String[] colNames = {"Producto", "Marca", "Talla", "Color", "Stock", "Tipo", "Cantidad", "Acción"};
        DefaultTableModel model = new DefaultTableModel(colNames, 0) {
             @Override
             public boolean isCellEditable(int row, int column) {
                 return column == 5 || column == 6 || column == 7; // Tipo, Cantidad, Accion editable
             }
        };
        
        List<ProductoConfigurado> configurados = new ArrayList<>();
        for (ProductoSeleccionado p : seleccionadosMulti) {
            configurados.add(new ProductoConfigurado(p, 1, "par")); // Default 1 par
        }

        // Fill table
        Runnable refreshTable = () -> {
            model.setRowCount(0);
            for (ProductoConfigurado pc : configurados) {
                model.addRow(new Object[]{
                    pc.getProducto().getNombre(),
                    pc.getProducto().getMarca(),
                    pc.getProducto().getTalla(),
                    pc.getProducto().getColor(),
                    pc.getProducto().getStockPares() + " P / " + pc.getProducto().getStockCajas() + " C",
                    pc.getTipo(),
                    pc.getCantidad(),
                    "Eliminar"
                });
            }
        };
        refreshTable.run();

        JTable table = new JTable(model);
        table.setRowHeight(30);
        
        // Editors for Type and Quantity
        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"par", "caja"});
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(cmbTipo));
        
        table.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JTextField()));

        // Update configurados list on change
        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && row < configurados.size()) {
                    ProductoConfigurado pc = configurados.get(row);
                    if (col == 5) { // Tipo
                         pc.setTipo((String) model.getValueAt(row, col));
                    } else if (col == 6) { // Cantidad
                        try {
                            int cant = Integer.parseInt(model.getValueAt(row, col).toString());
                            if (cant > 0) pc.setCantidad(cant);
                        } catch (NumberFormatException ex) {
                             // Ignore invalid number
                        }
                    }
                }
            }
        });
        
        // Remove action
        table.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 int row = table.rowAtPoint(e.getPoint());
                 int col = table.columnAtPoint(e.getPoint());
                 if (col == 7) { // Accion
                     configurados.remove(row);
                     refreshTable.run();
                     actualizarBotonSeleccion();
                     if (configurados.isEmpty()) dialog.dispose();
                 }
             }
        });

        content.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Panel: Bulk Actions & Confirm
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        
        JPanel bulkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bulkPanel.setOpaque(false);
        bulkPanel.setBorder(BorderFactory.createTitledBorder("Aplicar a todos"));
        
        JComboBox<String> cmbBulkTipo = new JComboBox<>(new String[]{"-", "par", "caja"});
        JTextField txtBulkCant = new JTextField(5);
        JButton btnApply = new JButton("Aplicar");
        
        btnApply.addActionListener(e -> {
            String tipo = (String) cmbBulkTipo.getSelectedItem();
            String cantStr = txtBulkCant.getText().trim();
            
            for (ProductoConfigurado pc : configurados) {
                if (!"-".equals(tipo)) pc.setTipo(tipo);
                if (!cantStr.isEmpty()) {
                     try {
                         int c = Integer.parseInt(cantStr);
                         if (c > 0) pc.setCantidad(c);
                     } catch(NumberFormatException ex) {}
                }
            }
            refreshTable.run();
        });
        
        bulkPanel.add(new JLabel("Tipo:"));
        bulkPanel.add(cmbBulkTipo);
        bulkPanel.add(new JLabel("Cantidad:"));
        bulkPanel.add(txtBulkCant);
        bulkPanel.add(btnApply);
        
        JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        confirmPanel.setOpaque(false);
        JButton btnConfirm = new JButton("Agregar Todos a Venta");
        btnConfirm.setBackground(accentColor);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFont(btnConfirm.getFont().deriveFont(Font.BOLD, 14f));
        
        btnConfirm.addActionListener(e -> {
             if (callbackMulti != null) {
                 callbackMulti.accept(configurados);
             }
             dialog.dispose();
             dispose(); // Close main dialog too
        });
        
        confirmPanel.add(btnConfirm);
        
        bottomPanel.add(bulkPanel, BorderLayout.WEST);
        bottomPanel.add(confirmPanel, BorderLayout.EAST);
        
        content.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    // ==================== MÉTODO ESTÁTICO PARA MOSTRAR ====================
    public static void mostrar(Component parent, Integer idBodega, Consumer<ProductoSeleccionado> callback) {
        mostrar(parent, idBodega, callback, null);
    }

    public static void mostrar(Component parent, Integer idBodega, Consumer<ProductoSeleccionado> callback, Consumer<List<ProductoConfigurado>> callbackMulti) {
        mostrar(parent, idBodega, callback, callbackMulti, null, null);
    }

    public static void mostrar(Component parent, Integer idBodega, Consumer<ProductoSeleccionado> callback, Consumer<List<ProductoConfigurado>> callbackMulti, Runnable onImportExcel, Runnable onDownloadTemplate) {
        System.out.println("Buscar Abriendo buscador para bodega: " + idBodega);
        Window window = SwingUtilities.getWindowAncestor(parent);
        BuscadorProductoDialog dialog = new BuscadorProductoDialog(window, idBodega, callback, callbackMulti, onImportExcel, onDownloadTemplate);

        // Agregar listener para limpiar recursos cuando se cierre el diálogo
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                // Apagar el executor de imágenes
                dialog.imageLoader.shutdown();
                try {
                    if (!dialog.imageLoader.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        dialog.imageLoader.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    dialog.imageLoader.shutdownNow();
                }
                System.out.println("SUCCESS  Recursos del buscador liberados");
            }
        });

        dialog.txtBuscar.requestFocusInWindow();
        dialog.setVisible(true);
    }

    public static void mostrarTraspasos(Component parent, Integer idBodega, Consumer<ProductoSeleccionado> callback) {
        mostrarTraspasos(parent, idBodega, callback, null);
    }

    public static void mostrarTraspasos(Component parent, Integer idBodega, Consumer<ProductoSeleccionado> callback, Consumer<List<ProductoConfigurado>> callbackMulti) {
        System.out.println("Buscar Abriendo buscador de traspasos para bodega: " + idBodega);
        Window window = SwingUtilities.getWindowAncestor(parent);
        BuscadorProductoDialog dialog = new BuscadorProductoDialog(window, idBodega, callback, callbackMulti, null, null);
        dialog.setModoTraspasos(true);

        // Agregar listener para limpiar recursos cuando se cierre el diálogo
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                dialog.imageLoader.shutdown();
                try {
                    if (!dialog.imageLoader.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        dialog.imageLoader.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    dialog.imageLoader.shutdownNow();
                }
            }
        });

        dialog.txtBuscar.requestFocusInWindow();
        dialog.setVisible(true);
    }

    public static void mostrarTraspasosSeleccion(Component parent, Integer idBodega, Consumer<ProductoSeleccionado> callback, Consumer<List<ProductoConfigurado>> callbackMulti) {
        System.out.println("Buscar Abriendo selector de traspasos para bodega: " + idBodega);
        Window window = SwingUtilities.getWindowAncestor(parent);
        BuscadorProductoDialog dialog = new BuscadorProductoDialog(window, idBodega, callback, callbackMulti, null, null);
        dialog.abrirSelectorTraspasos = true;
        dialog.setModoTraspasos(true);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                dialog.imageLoader.shutdown();
                try {
                    if (!dialog.imageLoader.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        dialog.imageLoader.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    dialog.imageLoader.shutdownNow();
                }
            }
        });

        dialog.txtBuscar.requestFocusInWindow();
        dialog.setVisible(true);
    }
}

