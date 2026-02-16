package raven.application.form.comercial.compras;

import com.formdev.flatlaf.FlatClientProperties;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import java.awt.image.BufferedImage;

import raven.clases.principal.ServiceCompra;
import raven.clases.principal.ServiceCompra.ProductoBusqueda;
import raven.clases.principal.ServiceCompra.VarianteBusqueda;
import raven.clases.productos.TraspasoService;
import raven.clases.productos.Bodega;
import raven.clases.productos.ServiceProductVariant;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.ServiceSize;
import raven.clases.productos.ServiceColor;

/**
 * Diálogo para buscar productos existentes para agregar a compra.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class BuscadorProductoCompraDialog extends JDialog {

    private static final String STYLE_CAMPO = "arc:15;background:lighten($Menu.background,25%)";
    private static final String STYLE_BTN = "arc:25;background:#007AFF;";

    private final int idBodega;
    private final int idUsuario;
    private final int idProveedor;
    private final ServiceCompra serviceCompra;

    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tablaProductos;
    private DefaultTableModel modeloProductos;
    private JTable tablaVariantes;
    private DefaultTableModel modeloVariantes;
    private VarianteBusqueda varianteSeleccionada = null;
    private ProductoBusqueda productoSeleccionado = null;
    private final AsyncImageLoader imageLoader = new AsyncImageLoader();
    private ImageIcon tablePlaceholderIcon;

    public BuscadorProductoCompraDialog(JFrame parent, int idBodega, int idUsuario, int idProveedor,
            ServiceCompra serviceCompra) {
        super(parent, "Buscar Producto", true);
        this.idBodega = idBodega;
        this.idUsuario = idUsuario;
        this.idProveedor = idProveedor;
        this.serviceCompra = serviceCompra;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(800, 600));
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de búsqueda
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelBusqueda.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:$Table.gridColor;");

        txtBuscar = new JTextField(30);
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_CAMPO);
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre, código, EAN, SKU...");
        txtBuscar.addActionListener(e -> buscarProductos());

        btnBuscar = new JButton("Buscar");
        btnBuscar.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 16, UIManager.getColor("TabbedPane.foreground")));
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN);
        btnBuscar.setToolTipText("Buscar productos por texto, código, EAN o SKU");
        btnBuscar.getAccessibleContext().setAccessibleName("Buscar");
        btnBuscar.addActionListener(e -> buscarProductos());

        panelBusqueda.add(new JLabel("Buscar:"));
        panelBusqueda.add(txtBuscar);
        panelBusqueda.add(btnBuscar);

        add(panelBusqueda, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        JPanel panelProductos = new JPanel(new BorderLayout());
        panelProductos.setBorder(BorderFactory.createTitledBorder("Productos"));
        tablePlaceholderIcon = createPlaceholder(60, 60, "");

        String[] colsProductos = { "Img", "ID", "Código", "Nombre", "Marca", "Categoría" };
        modeloProductos = new DefaultTableModel(colsProductos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? ImageIcon.class : Object.class;
            }
        };
        tablaProductos = new JTable(modeloProductos);
        configurarTabla(tablaProductos);
        tablaProductos.setRowHeight(56);
        tablaProductos.getColumnModel().getColumn(0).setPreferredWidth(64);
        tablaProductos.getColumnModel().getColumn(0).setMinWidth(64);
        tablaProductos.getColumnModel().getColumn(0).setMaxWidth(64);
        tablaProductos.getColumnModel().getColumn(1).setMinWidth(0);
        tablaProductos.getColumnModel().getColumn(1).setMaxWidth(0);
        tablaProductos.getColumnModel().getColumn(0).setCellRenderer(new IconRenderer());

        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tablaProductos.getSelectedRow();
                if (row >= 0) {
                    int idProducto = (int) modeloProductos.getValueAt(row, 1);
                    cargarVariantes(idProducto);
                }
            }
        });

        panelProductos.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);
        splitPane.setTopComponent(panelProductos);

        JPanel panelVariantes = new JPanel(new BorderLayout());
        panelVariantes.setBorder(BorderFactory.createTitledBorder("Variantes (Talla/Color)"));
        panelVariantes.setPreferredSize(new Dimension(0, 200));

        String[] colsVariantes = { "ID", "Talla", "Color", "SKU", "EAN", "Stock Pares", "Stock Cajas",
                "Precio Compra", "Proveedor" };
        modeloVariantes = new DefaultTableModel(colsVariantes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaVariantes = new JTable(modeloVariantes);
        configurarTabla(tablaVariantes);
        tablaVariantes.setRowHeight(30);

        tablaVariantes.getColumnModel().getColumn(0).setMinWidth(0);
        tablaVariantes.getColumnModel().getColumn(0).setMaxWidth(0);
        // Configurar ancho de columna de proveedor
        tablaVariantes.getColumnModel().getColumn(8).setPreferredWidth(120);

        tablaVariantes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaVariantes.getSelectedRow() >= 0) {
                    seleccionarVariante();
                }
            }
        });

        panelVariantes.add(new JScrollPane(tablaVariantes), BorderLayout.CENTER);
        splitPane.setBottomComponent(panelVariantes);

        splitPane.setDividerLocation(270);
        splitPane.setResizeWeight(0.65);
        add(splitPane, BorderLayout.CENTER);

        // Panel de acciones
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton btnCrearVariante = new JButton("Crear variante");
        btnCrearVariante.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 16, Color.WHITE));
        btnCrearVariante.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN);
        btnCrearVariante.setToolTipText("Crear una nueva variante para el producto seleccionado");
        btnCrearVariante.getAccessibleContext().setAccessibleName("Crear variante");
        btnCrearVariante.addActionListener(e -> abrirCrearVariante());

        JButton btnSeleccionar = new JButton("Seleccionar Variante");
        btnSeleccionar.setIcon(FontIcon.of(FontAwesomeSolid.CHECK, 16, Color.WHITE));
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc:25;background:#28CD41;");
        btnSeleccionar.setToolTipText("Usar variante seleccionada para agregar a la compra");
        btnSeleccionar.getAccessibleContext().setAccessibleName("Seleccionar Variante");
        btnSeleccionar.addActionListener(e -> seleccionarVariante());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setToolTipText("Cerrar el buscador");
        btnCancelar.getAccessibleContext().setAccessibleName("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        panelAcciones.add(btnCancelar);
        panelAcciones.add(btnCrearVariante);
        panelAcciones.add(btnSeleccionar);

        add(panelAcciones, BorderLayout.SOUTH);
    }

    private void configurarTabla(JTable tabla) {
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;" +
                        "showVerticalLines:false;" +
                        "rowHeight:35;" +
                        "intercellSpacing:10,5");

        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;" +
                        "height:35;" +
                        "separatorColor:$TableHeader.background;" +
                        "font:bold $h4.font");
    }

    private void buscarProductos() {
        String termino = txtBuscar.getText().trim();
        if (termino.isEmpty()) {
            return;
        }

        try {
            List<ProductoBusqueda> productos = serviceCompra.buscarProductos(termino);

            modeloProductos.setRowCount(0);
            varianteSeleccionada = null;

            int idx = 0;
            for (ProductoBusqueda p : productos) {
                modeloProductos.addRow(new Object[] {
                        tablePlaceholderIcon,
                        p.idProducto,
                        p.codigoModelo,
                        p.nombre,
                        p.marca,
                        p.categoria
                });
                final int rowIndex = idx;
                imageLoader.loadForProduct(p.idProducto, idBodega, icon -> {
                    if (icon != null && rowIndex < modeloProductos.getRowCount()) {
                        modeloProductos.setValueAt(icon, rowIndex, 0);
                    }
                });
                idx++;
            }

            if (productos.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No se encontraron productos con: " + termino,
                        "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error en búsqueda: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarVariantes(int idProducto) {
        try {
            List<VarianteBusqueda> variantes = serviceCompra.obtenerVariantesProducto(idProducto, idBodega,
                    idProveedor);

            modeloVariantes.setRowCount(0);

            for (VarianteBusqueda v : variantes) {
                modeloVariantes.addRow(new Object[] {
                        v.idVariante,
                        v.talla,
                        v.color,
                        v.sku,
                        v.ean,
                        v.stockPares,
                        v.stockCajas,
                        v.precioCompra,
                        v.nombreProveedor != null ? v.nombreProveedor : "Sin proveedor"
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar variantes: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Eliminado: carga de imagen en panel superior. Ahora empleamos columna Img por
    // fila.

    private void seleccionarVariante() {
        int filaProducto = tablaProductos.getSelectedRow();
        int filaVariante = tablaVariantes.getSelectedRow();

        if (filaProducto < 0 || filaVariante < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un producto y una variante",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int idProducto = (int) modeloProductos.getValueAt(filaProducto, 1);
            String nombreProducto = (String) modeloProductos.getValueAt(filaProducto, 3);

            int idVariante = (int) modeloVariantes.getValueAt(filaVariante, 0);
            String talla = (String) modeloVariantes.getValueAt(filaVariante, 1);
            String color = (String) modeloVariantes.getValueAt(filaVariante, 2);
            String sku = (String) modeloVariantes.getValueAt(filaVariante, 3);
            String ean = (String) modeloVariantes.getValueAt(filaVariante, 4);
            int stockPares = (int) modeloVariantes.getValueAt(filaVariante, 5);
            int stockCajas = (int) modeloVariantes.getValueAt(filaVariante, 6);
            Object precioCompraObj = modeloVariantes.getValueAt(filaVariante, 7);
            String nombreProveedor = (String) modeloVariantes.getValueAt(filaVariante, 8);

            varianteSeleccionada = new VarianteBusqueda();
            varianteSeleccionada.idVariante = idVariante;
            varianteSeleccionada.idProducto = idProducto;
            varianteSeleccionada.nombreProducto = nombreProducto;
            varianteSeleccionada.talla = talla;
            varianteSeleccionada.color = color;
            varianteSeleccionada.sku = sku;
            varianteSeleccionada.ean = ean;
            varianteSeleccionada.stockPares = stockPares;
            varianteSeleccionada.stockCajas = stockCajas;
            varianteSeleccionada.nombreProveedor = nombreProveedor;

            if (precioCompraObj instanceof java.math.BigDecimal) {
                varianteSeleccionada.precioCompra = (java.math.BigDecimal) precioCompraObj;
            }

            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al seleccionar variante: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public VarianteBusqueda getVarianteSeleccionada() {
        return varianteSeleccionada;
    }

    private void abrirCrearVariante() {
        int filaProducto = tablaProductos.getSelectedRow();
        if (filaProducto < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un producto",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (idProveedor <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar un proveedor antes de crear variantes",
                    "Proveedor requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idProducto = (int) modeloProductos.getValueAt(filaProducto, 1);
        String nombreProducto = (String) modeloProductos.getValueAt(filaProducto, 3);
        CrearVarianteDialog dlg = new CrearVarianteDialog((JFrame) SwingUtilities.getWindowAncestor(this), idProducto,
                nombreProducto, idBodega, idProveedor, idUsuario);
        dlg.setVisible(true);
        cargarVariantes(idProducto);
    }

    private ImageIcon createPlaceholder(int w, int h, String text) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(220, 220, 220));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(120, 120, 120));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        g.drawString(text, (w - tw) / 2, h / 2);
        g.dispose();
        return new ImageIcon(img);
    }

    private static class IconRenderer extends DefaultTableCellRenderer {
        @Override
        public void setValue(Object value) {
            if (value instanceof ImageIcon) {
                setIcon((ImageIcon) value);
                setText("");
            } else {
                setIcon(null);
                setText("");
            }
            setHorizontalAlignment(CENTER);
        }
    }

    private static class ImageCache {
        private final Map<String, ImageIcon> cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
                return size() > 100;
            }
        };

        public ImageIcon get(String key) {
            return cache.get(key);
        }

        public void put(String key, ImageIcon icon) {
            cache.put(key, icon);
        }
    }

    private class AsyncImageLoader {
        private final ImageCache cache = new ImageCache();
        private final ServiceProductVariant serviceVariant = new ServiceProductVariant();

        public void loadForProduct(int idProducto, int idBodegaSel, Consumer<ImageIcon> cb) {
            String key = idProducto + "-" + idBodegaSel;
            ImageIcon cached = cache.get(key);
            if (cached != null) {
                cb.accept(cached);
                return;
            }
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    java.util.List<raven.controlador.productos.ModelProductVariant> vars;
                    try {
                        vars = serviceVariant.getVariantsByProductAndWarehouse(idProducto, idBodegaSel);
                    } catch (Exception ex) {
                        return null;
                    }
                    if (vars == null || vars.isEmpty())
                        return null;
                    for (raven.controlador.productos.ModelProductVariant v : vars) {
                        try {
                            byte[] bytes = serviceVariant.getVariantImage(v.getVariantId());
                            if (bytes != null && bytes.length > 0) {
                                ImageIcon icon = new ImageIcon(bytes);
                                Image scaled = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                                ImageIcon out = new ImageIcon(scaled);
                                cache.put(key, out);
                                return out;
                            }
                        } catch (Exception ignore) {
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    ImageIcon icon = null;
                    try {
                        icon = get();
                    } catch (Exception ignore) {
                    }
                    cb.accept(icon);
                }
            }.execute();
        }
    }

    private class CrearVarianteDialog extends JDialog {
        private final int idProducto;
        private final int idBodegaDefault;
        private final int idProveedor;
        private final int idUsuario;
        private final ServiceProduct serviceProduct = new ServiceProduct();
        private final ServiceSize serviceSize = new ServiceSize();
        private final ServiceColor serviceColor = new ServiceColor();
        private final TraspasoService traspasoService = new TraspasoService();

        private JLabel lblProducto;
        private JComboBox<String> cmbTalla;
        private JComboBox<String> cmbColor;
        private JLabel lblImagen;
        private JButton btnImagen;
        private JLabel lblBodega;
        // El campo de precio se elimina según requerimiento
        private JButton btnCrear;
        private JButton btnCancelar;
        private byte[] imagenBytes;

        public CrearVarianteDialog(JFrame parent, int idProducto, String nombreProducto, int idBodegaDefault,
                int idProveedor, int idUsuario) {
            super(parent, "Crear variante", true);
            this.idProducto = idProducto;
            this.idBodegaDefault = idBodegaDefault;
            this.idProveedor = idProveedor;
            this.idUsuario = idUsuario;
            initUI(nombreProducto);
            pack();
            setLocationRelativeTo(parent);
            setMinimumSize(new Dimension(680, 460));
        }

        private void initUI(String nombreProducto) {
            setLayout(new BorderLayout(10, 10));
            JPanel top = new JPanel(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(8, 10, 8, 10);
            g.fill = GridBagConstraints.HORIZONTAL;
            g.gridx = 0;
            g.gridy = 0;
            top.add(new JLabel("Producto:"), g);
            g.gridx = 1;
            g.gridwidth = 3;
            lblProducto = new JLabel(nombreProducto);
            top.add(lblProducto, g);
            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8, 10, 8, 10);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            center.add(new JLabel("Talla:"), c);
            c.gridx = 1;
            cmbTalla = new JComboBox<>();
            center.add(cmbTalla, c);
            c.gridx = 2;
            center.add(new JLabel("Color:"), c);
            c.gridx = 3;
            cmbColor = new JComboBox<>();
            center.add(cmbColor, c);
            c.gridx = 0;
            c.gridy = 1;
            center.add(new JLabel("Imagen:"), c);
            c.gridx = 1;
            lblImagen = new JLabel();
            lblImagen.setPreferredSize(new Dimension(200, 200));
            center.add(lblImagen, c);
            c.gridx = 2;
            btnImagen = new JButton("Seleccionar imagen");
            btnImagen.addActionListener(e -> seleccionarImagenConGestor());
            center.add(btnImagen, c);
            c.gridx = 0;
            c.gridy = 2;
            center.add(new JLabel("Bodega:"), c);
            c.gridx = 1;
            lblBodega = new JLabel("-");
            center.add(lblBodega, c);
            // Sin campo de precio (requisito)
            add(center, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            btnCancelar = new JButton("Cancelar");
            btnCancelar.addActionListener(e -> dispose());
            btnCrear = new JButton("Crear");
            btnCrear.putClientProperty(FlatClientProperties.STYLE, "arc:25;background:#28CD41;");
            btnCrear.addActionListener(e -> onCrear());
            bottom.add(btnCancelar);
            bottom.add(btnCrear);
            add(bottom, BorderLayout.SOUTH);

            cargarCombos();
        }

        private void cargarCombos() {
            try {
                raven.controlador.productos.ModelProduct p = new ServiceProduct().getProductById(idProducto);
                String genero = p != null ? p.getGender() : null;
                List<raven.controlador.productos.ModelSize> tallas;
                if (genero != null && !genero.trim().isEmpty()) {
                    tallas = new ServiceSize().getTallasByGenero(genero);
                } else {
                    tallas = new ServiceSize().getAll();
                }
                List<raven.controlador.productos.ModelColor> colores = new ServiceColor().getAll();
                cmbTalla.removeAllItems();
                for (raven.controlador.productos.ModelSize t : tallas) {
                    String nombreT = (t.getSistema() != null && t.getGenero() != null)
                            ? (t.getNumero() + " " + t.getSistema() + " " + t.getGenero())
                            : t.getNumero();
                    cmbTalla.addItem(nombreT);
                }
                cmbColor.removeAllItems();
                for (raven.controlador.productos.ModelColor c : colores) {
                    cmbColor.addItem(c.getNombre());
                }
                try {
                    List<Bodega> bodegas = traspasoService.obtenerBodegasActivas();
                    for (Bodega b : bodegas) {
                        if (b.getIdBodega() == idBodegaDefault) {
                            lblBodega.setText(b.getNombre());
                            break;
                        }
                    }
                } catch (SQLException ignore) {
                }
            } catch (SQLException ignore) {
            }
        }

        private void seleccionarImagenConGestor() {
            // USAR java.awt.FileDialog (Explorador nativo de Windows)
            java.awt.FileDialog fd = new java.awt.FileDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    "Seleccionar Imagen", java.awt.FileDialog.LOAD);
            fd.setFile("*.jpg;*.jpeg;*.png;*.gif");
            fd.setVisible(true);

            if (fd.getFile() != null) {
                try {
                    java.io.File archivoSeleccionado = new java.io.File(fd.getDirectory(), fd.getFile());
                    imagenBytes = java.nio.file.Files.readAllBytes(archivoSeleccionado.toPath());

                    if (imagenBytes != null && imagenBytes.length > 0) {
                        ImageIcon icon = new ImageIcon(imagenBytes);
                        Image scaled = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                        lblImagen.setIcon(new ImageIcon(scaled));
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "No se pudo cargar la imagen: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void onCrear() {
            String tallaSel = (String) cmbTalla.getSelectedItem();
            String colorSel = (String) cmbColor.getSelectedItem();
            int idTalla;
            int idColor;
            try {
                idTalla = serviceSize.getSizeIdByName(tallaSel);
                idColor = serviceColor.getColorIdByName(colorSel);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            raven.controlador.productos.ModelProductVariant mv = new raven.controlador.productos.ModelProductVariant();
            mv.setProductId(idProducto);
            mv.setSizeId(idTalla);
            mv.setColorId(idColor);
            mv.setSizeName(tallaSel);
            mv.setColorName(colorSel);
            mv.setPurchasePrice(null); // Precio de compra se establece en la compra
            mv.setSalePrice(null); // Precio de venta se establece en el producto base
            mv.setMinStock(1);
            mv.setStockPairs(0); // Stock en 0 como solicitado
            mv.setStockBoxes(0); // Stock en 0 como solicitado
            mv.setWarehouseId(idBodegaDefault);
            mv.setSupplierId(idProveedor);
            try {
                raven.controlador.productos.ModelProduct p = new ServiceProduct().getProductById(idProducto);
                String codigoModelo = p != null ? p.getModelCode() : "TEMP";
                String skuGen = CodigoGen.generarSKU(codigoModelo, tallaSel, colorSel, "Par", 1);
                String eanGen = CodigoGen.generarEAN13(codigoModelo, tallaSel, colorSel, 1);
                mv.setSku(skuGen);
                mv.setEan(eanGen);
            } catch (Exception ignore) {
            }
            if (imagenBytes != null && imagenBytes.length > 0)
                mv.setImageBytes(imagenBytes);
            int idNueva = serviceProduct.createVariantForProduct(idProducto, mv, idProveedor, idUsuario);
            if (idNueva <= 0) {
                JOptionPane.showMessageDialog(this, "No se pudo crear la variante", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dispose();
        }
    }

    public static class CodigoGen {
        public static String generarEAN13(String modelo, String talla, String color, int secuencia) {
            String base = (modelo == null ? "" : modelo.replaceAll("[^0-9]", ""))
                    + (talla == null ? "" : talla.replaceAll("[^0-9]", ""))
                    + (color == null ? "" : Integer.toString(Math.abs(color.hashCode())));
            if (base.isEmpty())
                base = Long.toString(System.currentTimeMillis() % 1000000);

            String digits = base + String.format("%02d", secuencia);
            digits = digits.replaceAll("[^0-9]", "");
            if (digits.length() < 12) {
                digits = String.format("%012d", Long.parseLong(digits));
            } else if (digits.length() > 12) {
                digits = digits.substring(0, 12);
            }
            int checksum = calcularEAN13Checksum(digits);
            return digits + checksum;
        }

        private static int calcularEAN13Checksum(String digits) {
            int sum = 0;
            for (int i = 0; i < digits.length(); i++) {
                int d = digits.charAt(i) - '0';
                sum += (i % 2 == 0) ? d : d * 3;
            }
            int mod = sum % 10;
            return (10 - mod) % 10;
        }

        public static String generarSKU(String codigoModelo, String talla, String color, String tipo, int indice) {
            try {
                String cm = codigoModelo != null && !codigoModelo.trim().isEmpty() ? codigoModelo.trim() : "TEMP";
                String tallaCodigo = talla.replaceAll("\\s+", "").substring(0, Math.min(talla.length(), 3));
                String colorCodigo = color.substring(0, Math.min(color.length(), 3)).toUpperCase();
                String tipoCodigo = "Par".equalsIgnoreCase(tipo) ? "P" : "C";
                String baseSku = String.format("%s-%s-%s-%s-%03d", cm, tallaCodigo, colorCodigo, tipoCodigo, indice);
                return baseSku;
            } catch (Exception e) {
                long timestamp = System.currentTimeMillis() % 1000000;
                return "SKU-" + indice + "-" + timestamp;
            }
        }

        public static boolean proveedorValido(int idProv) {
            return idProv > 0;
        }

        public static raven.controlador.productos.ModelProductVariant crearVarianteModelo(int idProducto, int idTalla,
                int idColor, String talla, String color, int idBodega, String codigoModelo) {
            raven.controlador.productos.ModelProductVariant mv = new raven.controlador.productos.ModelProductVariant();
            mv.setProductId(idProducto);
            mv.setSizeId(idTalla);
            mv.setColorId(idColor);
            mv.setSizeName(talla);
            mv.setColorName(color);
            mv.setWarehouseId(idBodega);
            mv.setPurchasePrice(null);
            mv.setSalePrice(null);
            mv.setMinStock(1);
            mv.setStockPairs(0);
            mv.setStockBoxes(0);
            String skuGen = generarSKU(codigoModelo, talla, color, "Par", 1);
            String eanGen = generarEAN13(codigoModelo, talla, color, 1);
            mv.setSku(skuGen);
            mv.setEan(eanGen);
            return mv;
        }
    }
}
