package raven.application.form.productos;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import raven.application.form.productos.buscador.ProductoBusquedaItem;
import raven.application.form.productos.buscador.ProductoBusquedaService;
import raven.clases.admin.ServiceBodegas;
import raven.clases.admin.UserSession;
import raven.clases.comun.GenericPaginationService;
import raven.clases.productos.ProductPaginationAdapter;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.ServiceCategory;
import raven.clases.productos.ServiceBrand;
import raven.controlador.admin.ModelBodegas;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelProduct;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Desktop;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class VerProductosForm extends JPanel {
    private java.util.List<ProductoBusquedaItem> baseList;
    private final ProductPaginationAdapter productAdapter = new ProductPaginationAdapter();
    private final ProductoBusquedaService searchService = new ProductoBusquedaService();

    private int currentPage = 1;
    private int pageSize = 25;
    private long totalCount = 0;

    private final ExecutorService dataLoader = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService searchScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> searchTask;
    private volatile boolean isSearching = false;
    private final java.util.concurrent.atomic.AtomicLong requestSeq = new java.util.concurrent.atomic.AtomicLong(0);

    // Caché y Executor para carga asíncrona de imágenes
    private final Map<Integer, ImageIcon> imageCache = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> pendingImages = java.util.Collections
            .synchronizedSet(new java.util.HashSet<>());
    private final ExecutorService imageLoader = Executors.newFixedThreadPool(4);
    private final ImageIcon loadingIcon = new FlatSVGIcon("raven/icon/svg/load.svg", 30, 30);
    private final ImageIcon defaultIcon = new FlatSVGIcon("raven/icon/svg/image.svg", 30, 30);

    // Filtros avanzados
    private JComboBox<String> cbxCategoria;
    private JComboBox<String> cbxMarca;
    private javax.swing.JTextField txtPrecioMin, txtPrecioMax;
    private JCheckBox chkStockBajo, chkStockMedio, chkStockAlto;
    private JButton btnExportar, btnLimpiarFiltros;

    public VerProductosForm() {
        initComponents();
        initAdvancedFilters();
        setupActions();
        setupTable();
        reload();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        lb = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        cbxBodega = new javax.swing.JComboBox<>();
        cmxtipo = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaProductos = new javax.swing.JTable();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("ZAPATOS");

        jLabel1.setText("Buscar");

        cbxBodega.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todas" }));

        cmxtipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Pares", "Cajas" }));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGap(6, 6, 6)
                                                .addComponent(jLabel1)
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 784,
                                                                Short.MAX_VALUE)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(txtBuscar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 185,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(cbxBodega,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 166,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(26, 26, 26)
                                                                .addComponent(cmxtipo,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 166,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                .addContainerGap()))));
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(lb)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(cmxtipo, javax.swing.GroupLayout.DEFAULT_SIZE, 39,
                                                Short.MAX_VALUE)
                                        .addComponent(txtBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, 39,
                                                Short.MAX_VALUE)
                                        .addComponent(cbxBodega))
                                .addContainerGap()));

        tablaProductos.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] {
                        "ID", "EAN", "Foto", "Nombre", "Marca", "Talla", "Género", "Stock", "Tipo", "Bodega",
                        "Acciones", "IdProducto"
                }) {
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, false, false, false, false, false, false, true, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane1.setViewportView(tablaProductos);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1)
                                .addContainerGap()));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    }// </editor-fold>

    private void setupTable() {
        tablaProductos.setAutoCreateRowSorter(true);
        tablaProductos.setRowHeight(50); // Altura para imagen

        // Ocultar columnas ID e IdProducto
        tablaProductos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaProductos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaProductos.getColumnModel().getColumn(0).setPreferredWidth(0);

        tablaProductos.getColumnModel().getColumn(11).setMinWidth(0);
        tablaProductos.getColumnModel().getColumn(11).setMaxWidth(0);
        tablaProductos.getColumnModel().getColumn(11).setPreferredWidth(0);

        // Anchos de columna preferidos
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(100); // EAN
        tablaProductos.getColumnModel().getColumn(2).setPreferredWidth(60); // Foto
        tablaProductos.getColumnModel().getColumn(3).setPreferredWidth(250); // Nombre
        tablaProductos.getColumnModel().getColumn(4).setPreferredWidth(100); // Marca
        tablaProductos.getColumnModel().getColumn(5).setPreferredWidth(50); // Talla

        // Renderer para Imagen Asíncrona
        tablaProductos.getColumnModel().getColumn(2).setCellRenderer(new AsyncImageRenderer());

        // Renderer centrado para textos
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i : new int[] { 1, 4, 5, 6, 8, 9 }) { // Excluir columna 7 (Stock)
            tablaProductos.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Renderer con colores para Stock (columna 7)
        tablaProductos.getColumnModel().getColumn(7).setCellRenderer(new StockCellRenderer());

        // Renderer/Editor para columna de acciones
        tablaProductos.getColumnModel().getColumn(10).setCellRenderer(new ButtonRenderer());
        tablaProductos.getColumnModel().getColumn(10).setCellEditor(new ButtonEditor());

        // Barra de paginación simple
        javax.swing.JPanel bottom = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 6));
        javax.swing.JButton btnPrev = new javax.swing.JButton("<");
        javax.swing.JButton btnNext = new javax.swing.JButton(">");
        javax.swing.JLabel lblInfo = new javax.swing.JLabel();
        javax.swing.JComboBox<String> cbPageSize = new javax.swing.JComboBox<>(new String[] { "10", "25", "50" });
        cbPageSize.setSelectedItem(String.valueOf(pageSize));
        bottom.add(btnPrev);
        bottom.add(btnNext);
        bottom.add(lblInfo);
        bottom.add(new javax.swing.JLabel("Por página:"));
        bottom.add(cbPageSize);
        this.setLayout(new java.awt.BorderLayout());
        java.awt.Component main = this.getComponent(0);
        this.removeAll();
        this.add(main, java.awt.BorderLayout.CENTER);
        this.add(bottom, java.awt.BorderLayout.SOUTH);
        btnPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadPageAsync();
            }
        });
        btnNext.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            if (currentPage < totalPages) {
                currentPage++;
                loadPageAsync();
            }
        });
        cbPageSize.addActionListener(e -> {
            try {
                pageSize = Integer.parseInt(String.valueOf(cbPageSize.getSelectedItem()));
                currentPage = 1;
                loadPageAsync();
            } catch (Exception ignore) {
            }
        });
        this.putClientProperty("_paginationInfoLabel", lblInfo);
    }

    private void setupActions() {
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void schedule() {
                if (searchTask != null) {
                    searchTask.cancel(false);
                }
                searchTask = searchScheduler.schedule(() -> javax.swing.SwingUtilities.invokeLater(() -> {
                    currentPage = 1;
                    loadPageAsync();
                }), 300, TimeUnit.MILLISECONDS);
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                schedule();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                schedule();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                schedule();
            }
        });
        txtBuscar.addActionListener(e -> {
            currentPage = 1;
            loadPageAsync();
        });
        cbxBodega.addActionListener(e -> applyFilters());
        cmxtipo.addActionListener(e -> applyFilters());

        // Atajo Ctrl+Shift+F para buscador avanzado
        javax.swing.KeyStroke keyStroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
        this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "openSearch");
        this.getActionMap().put("openSearch", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                abrirBuscadorAvanzado();
            }
        });
    }

    private void abrirBuscadorAvanzado() {
        java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog dialog = new javax.swing.JDialog(parentWindow, "Buscador Avanzado de Productos",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        raven.application.form.productos.buscador.BuscadorProductosPanel panel = new raven.application.form.productos.buscador.BuscadorProductosPanel();
        dialog.setContentPane(panel);
        dialog.setSize(1100, 750);
        dialog.setLocationRelativeTo(parentWindow);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                panel.focusSearch();
            }
        });

        dialog.setVisible(true);
    }

    private void reload() {
        loadWarehouses();
        currentPage = 1;
        loadPageAsync();
    }

    private void applyFilters() {
        currentPage = 1;
        loadPageAsync();
    }

    private void fillTable(List<ProductoBusquedaItem> list) {
        DefaultTableModel m = (DefaultTableModel) tablaProductos.getModel();
        m.setRowCount(0);
        for (ProductoBusquedaItem p : list) {
            m.addRow(new Object[] {
                    p.getIdVariante(),
                    p.getEan(),
                    null, // La imagen se cargará asíncronamente
                    p.getNombre(),
                    p.getMarca(),
                    p.getTalla(),
                    p.getGenero(),
                    p.getStock(),
                    p.getTipo(),
                    p.getBodega(),
                    "Ver",
                    p.getIdProducto()
            });
        }
        javax.swing.JLabel info = (javax.swing.JLabel) this.getClientProperty("_paginationInfoLabel");
        if (info != null) {
            int start = (currentPage - 1) * pageSize + 1;
            int end = Math.min(currentPage * pageSize, (int) totalCount);
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            info.setText(String.format("Página %d de %d (%d-%d de %d)", currentPage, Math.max(totalPages, 1), start,
                    Math.max(end, 0), totalCount));
        }
    }

    private void loadPageAsync() {
        final long seq = requestSeq.incrementAndGet();
        if (isSearching) {
        }
        isSearching = true;
        final String q = txtBuscar.getText() != null ? txtBuscar.getText().trim() : null;
        final Integer warehouseId = getSelectedWarehouseId();
        final String tipo = String.valueOf(cmxtipo.getSelectedItem());

        // Obtener filtros avanzados
        final String categoria = cbxCategoria.getSelectedItem() != null ? String.valueOf(cbxCategoria.getSelectedItem())
                : null;
        final String marca = cbxMarca.getSelectedItem() != null ? String.valueOf(cbxMarca.getSelectedItem()) : null;

        Double minP = null;
        try {
            if (!txtPrecioMin.getText().trim().isEmpty())
                minP = Double.parseDouble(txtPrecioMin.getText().trim());
        } catch (Exception e) {
        }
        final Double minPrice = minP;

        Double maxP = null;
        try {
            if (!txtPrecioMax.getText().trim().isEmpty())
                maxP = Double.parseDouble(txtPrecioMax.getText().trim());
        } catch (Exception e) {
        }
        final Double maxPrice = maxP;

        // Filtros de Stock
        final java.util.List<String> stockLevels = new java.util.ArrayList<>();
        if (chkStockBajo.isSelected())
            stockLevels.add("Bajo");
        if (chkStockMedio.isSelected())
            stockLevels.add("Medio");
        if (chkStockAlto.isSelected())
            stockLevels.add("Alto");

        // Logs de depuración para verificar filtros y trimming
        System.out.println("--- Aplicando Filtros en VerProductosForm ---");
        System.out.println("Texto búsqueda: " + q);
        System.out.println("Bodega ID: " + warehouseId);
        System.out.println("Tipo: " + tipo);
        System.out.println("Categoría (RAW): " + categoria);
        System.out.println("Marca (RAW): " + marca);
        System.out.println("Precio Min: " + minPrice);
        System.out.println("Precio Max: " + maxPrice);
        System.out.println("Stock Levels: " + stockLevels);

        // Asegurar trimming en strings para evitar errores de espacios
        final String categoriaFinal = (categoria != null) ? categoria.trim() : null;
        final String marcaFinal = (marca != null) ? marca.trim() : null;

        dataLoader.submit(() -> {
            try {
                GenericPaginationService.PagedResult<ProductoBusquedaItem> result;
                result = productAdapter.getVariantsPaged(
                        q, warehouseId, tipo, currentPage, pageSize,
                        categoriaFinal, marcaFinal, minPrice, maxPrice, stockLevels);
                final long tc = result.getTotalCount();
                final List<ProductoBusquedaItem> data = result.getData();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (seq != requestSeq.get())
                        return;
                    totalCount = tc;
                    baseList = data;
                    fillTable(baseList);
                    if (totalCount == 0) {
                        // Opcional: Mostrar mensaje discreto en etiqueta en lugar de Popup
                    }
                    isSearching = false;
                });
            } catch (Exception ex) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (seq != requestSeq.get())
                        return;
                    baseList = java.util.Collections.emptyList();
                    totalCount = 0;
                    fillTable(baseList);
                    javax.swing.JOptionPane.showMessageDialog(VerProductosForm.this,
                            "Error al cargar productos: " + ex.getMessage(), "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    isSearching = false;
                });
            }
        });
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        try {
            if (searchTask != null) {
                searchTask.cancel(false);
                searchTask = null;
            }
        } catch (Exception ignore) {
        }
        try {
            searchScheduler.shutdownNow();
        } catch (Exception ignore) {
        }
        try {
            dataLoader.shutdown();
        } catch (Exception ignore) {
        }
        try {
            imageLoader.shutdown();
        } catch (Exception ignore) {
        }
    }

    private class ButtonRenderer extends javax.swing.JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setText("Ver");
        }

        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor, java.awt.event.ActionListener {
        private final javax.swing.JButton btn = new javax.swing.JButton("Ver");
        private int row;

        public ButtonEditor() {
            btn.addActionListener(this);
        }

        public Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected,
                int row, int column) {
            this.row = row;
            return btn;
        }

        public Object getCellEditorValue() {
            return "Ver";
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            int idProd = 0;
            try {
                int modelRow = tablaProductos.convertRowIndexToModel(row);
                javax.swing.table.TableModel tm = tablaProductos.getModel();
                int idCol = 11; // Columna IdProducto
                Object val = tm.getValueAt(modelRow, idCol);
                if (val instanceof Integer)
                    idProd = (Integer) val;
                else
                    idProd = Integer.parseInt(String.valueOf(val));
            } catch (Exception ignore) {
            }

            if (idProd > 0) {
                try {
                    ServiceProduct sp = new ServiceProduct();
                    ModelProduct prod = sp.getProductById(idProd);

                    if (prod != null) {
                        Integer warehouseId = getSelectedWarehouseId();
                        if (warehouseId == null || warehouseId <= 0) {
                            warehouseId = getCurrentBodegaId();
                            if (warehouseId != null) {
                                String nombre = null;
                                for (ModelBodegas b : bodegaList) {
                                    if (b.getIdBodega() != null && b.getIdBodega().equals(warehouseId)) {
                                        nombre = b.getNombre();
                                        break;
                                    }
                                }
                                if (nombre != null) {
                                    cbxBodega.setSelectedItem(nombre);
                                }
                            }
                        }
                        if (warehouseId == null || warehouseId <= 0) {
                            JOptionPane.showMessageDialog(VerProductosForm.this,
                                    "No se encontró bodega del usuario. Seleccione una bodega.", "Bodega requerida",
                                    JOptionPane.WARNING_MESSAGE);
                            stopCellEditing();
                            return;
                        }
                        raven.application.form.productos.VariantesDisponiblesDialog dlg = new raven.application.form.productos.VariantesDisponiblesDialog(
                                javax.swing.SwingUtilities.getWindowAncestor(VerProductosForm.this), prod, warehouseId);
                        dlg.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(VerProductosForm.this, "Producto no encontrado en base de datos",
                                "Información", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(VerProductosForm.this, "Error al cargar detalle: " + ex.getMessage(),
                            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(VerProductosForm.this, "ID de producto inválido", "Información",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            stopCellEditing();
        }
    }

    private class AsyncImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            label.setText(""); // Solo imagen
            label.setHorizontalAlignment(JLabel.CENTER);

            // Obtener ID de la variante (columna 0)
            Object idObj = table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);

            if (idObj == null || !(idObj instanceof Integer)) {
                label.setIcon(null);
                return label;
            }

            int idVariante = (Integer) idObj;

            // 1. Revisar caché
            if (imageCache.containsKey(idVariante)) {
                ImageIcon icon = imageCache.get(idVariante);
                label.setIcon(icon != null ? icon : defaultIcon);
            } else {
                // 2. Si no está, poner loading e iniciar carga
                label.setIcon(loadingIcon);

                if (!pendingImages.contains(idVariante)) {
                    pendingImages.add(idVariante);
                    startImageLoad(idVariante);
                }
            }
            return label;
        }
    }

    private void startImageLoad(int idVariante) {
        imageLoader.submit(() -> {
            try {
                if (imageCache.containsKey(idVariante))
                    return;

                ImageIcon icon = searchService.obtenerImagenVariante(idVariante);

                if (icon != null) {
                    Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(img);
                }

                imageCache.put(idVariante, icon != null ? icon : defaultIcon);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pendingImages.remove(idVariante);
                SwingUtilities.invokeLater(tablaProductos::repaint);
            }
        });
    }

    private java.util.List<ModelBodegas> bodegaList = new java.util.ArrayList<>();
    private java.util.Map<String, Integer> bodegaIdByName = new java.util.HashMap<>();

    private void loadWarehouses() {
        try {
            ServiceBodegas sb = new ServiceBodegas();
            bodegaList = sb.obtenerTodas();
            javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
            model.addElement("Todas");
            bodegaIdByName.clear();
            for (ModelBodegas b : bodegaList) {
                if (b.getActiva() == null || b.getActiva()) {
                    model.addElement(b.getNombre());
                    bodegaIdByName.put(b.getNombre(), b.getIdBodega());
                }
            }
            cbxBodega.setModel(model);
            Integer idDefault = getCurrentBodegaId();
            if (idDefault != null) {
                String nombre = null;
                for (ModelBodegas b : bodegaList) {
                    if (b.getIdBodega() != null && b.getIdBodega().equals(idDefault)) {
                        nombre = b.getNombre();
                        break;
                    }
                }
                if (nombre != null) {
                    cbxBodega.setSelectedItem(nombre);
                }
            }
        } catch (Exception ignore) {
        }
    }

    private Integer getSelectedWarehouseId() {
        Object sel = cbxBodega.getSelectedItem();
        if (sel == null)
            return null;
        String name = String.valueOf(sel);
        Integer id = bodegaIdByName.get(name);
        return id != null && id > 0 ? id : null;
    }

    private Integer getCurrentBodegaId() {
        try {
            Integer bodegaId = UserSession.getInstance().getIdBodegaUsuario();
            return (bodegaId != null && bodegaId > 0) ? bodegaId : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Variables declaration - do not modify
    public javax.swing.JComboBox<String> cbxBodega;
    public javax.swing.JComboBox<String> cmxtipo;

    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lb;
    public javax.swing.JTable tablaProductos;
    public javax.swing.JTextField txtBuscar;
    // End of variables declaration

    // ═══════════════════════════════════════════════════════════════════════════
    // FASE 1: FILTROS AVANZADOS
    // ═══════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════
    // FASE 1: FILTROS AVANZADOS (MEJORADO UI/UX & DATA)
    // ═══════════════════════════════════════════════════════════════════════════

    private void initAdvancedFilters() {
        // Panel contenedor con estilo FlatLaf
        JPanel panelFiltrosAvanzados = new JPanel();
        panelFiltrosAvanzados.setLayout(new java.awt.GridBagLayout());
        panelFiltrosAvanzados.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;"
                + "background:lighten($Panel.background, 3%);"
                + "border:10,10,10,10;");

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 10, 5, 10);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        // Inicializar componentes
        cbxCategoria = new JComboBox<>();
        cbxMarca = new JComboBox<>();
        txtPrecioMin = new JTextField(8);
        txtPrecioMax = new JTextField(8);
        chkStockBajo = new JCheckBox("Bajo (<10)");
        chkStockMedio = new JCheckBox("Medio (10-50)");
        chkStockAlto = new JCheckBox("Alto (>50)");

        btnExportar = new JButton("Exportar Excel");
        btnExportar.setIcon(new FlatSVGIcon("raven/icon/svg/excel.svg", 0.7f)); // Asumiendo icono o usar default

        btnLimpiarFiltros = new JButton("Limpiar");
        btnLimpiarFiltros.setIcon(new FlatSVGIcon("raven/icon/svg/clean.svg", 0.7f)); // Asumiendo icono o usar default

        // Estilo de inputs
        String inputStyle = "arc:10; border:1,1,1,1, #cccccc;";
        cbxCategoria.putClientProperty(FlatClientProperties.STYLE, inputStyle);
        cbxMarca.putClientProperty(FlatClientProperties.STYLE, inputStyle);
        txtPrecioMin.putClientProperty(FlatClientProperties.STYLE, inputStyle);
        txtPrecioMax.putClientProperty(FlatClientProperties.STYLE, inputStyle);
        txtPrecioMin.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Min $");
        txtPrecioMax.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Max $");

        // Cargar datos en combos de forma robusta
        cargarCategorias();
        cargarMarcas();

        // --- FILA 1: Categoría y Marca ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panelFiltrosAvanzados.add(new JLabel("🏷️ Categoría:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        panelFiltrosAvanzados.add(cbxCategoria, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panelFiltrosAvanzados.add(new JLabel("🏢 Marca:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.4;
        panelFiltrosAvanzados.add(cbxMarca, gbc);

        // --- FILA 2: Precio y Stock ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panelFiltrosAvanzados.add(new JLabel("💰 Precio:"), gbc);

        JPanel panelPrecio = new JPanel(new java.awt.GridLayout(1, 2, 5, 0));
        panelPrecio.setOpaque(false);
        panelPrecio.add(txtPrecioMin);
        panelPrecio.add(txtPrecioMax);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        panelFiltrosAvanzados.add(panelPrecio, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panelFiltrosAvanzados.add(new JLabel("📊 Stock:"), gbc);

        JPanel panelStock = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        panelStock.setOpaque(false);
        panelStock.add(chkStockBajo);
        panelStock.add(chkStockMedio);
        panelStock.add(chkStockAlto);

        gbc.gridx = 3;
        gbc.weightx = 0.4;
        panelFiltrosAvanzados.add(panelStock, gbc);

        // --- FILA 3: Botones (Full Width) ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        gbc.insets = new java.awt.Insets(15, 10, 5, 10); // Más espacio arriba

        JPanel panelBotones = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        panelBotones.add(btnLimpiarFiltros);
        panelBotones.add(javax.swing.Box.createHorizontalStrut(10));
        panelBotones.add(btnExportar);

        panelFiltrosAvanzados.add(panelBotones, gbc);

        // --- Agregar al Header (Integración UI) ---
        jPanel2.setLayout(new java.awt.BorderLayout(0, 10)); // Gap vertical
        jPanel2.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10)); // Padding contenedor

        JPanel headerTop = new JPanel(new java.awt.BorderLayout());
        headerTop.add(lb, java.awt.BorderLayout.CENTER); // Título ZAPATOS

        // Panel de búsqueda básica (existente)
        JPanel headerBasic = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 5));
        headerBasic.add(new JLabel("🔍 Buscar:"));
        headerBasic.add(txtBuscar);
        headerBasic.add(new JLabel("📦 Bodega:"));
        headerBasic.add(cbxBodega);
        headerBasic.add(new JLabel("Tipo:"));
        headerBasic.add(cmxtipo);

        // Contenedor norte unificado
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new javax.swing.BoxLayout(northContainer, javax.swing.BoxLayout.Y_AXIS));
        northContainer.add(headerTop);
        northContainer.add(javax.swing.Box.createVerticalStrut(10));
        northContainer.add(headerBasic);

        jPanel2.removeAll();
        jPanel2.add(northContainer, java.awt.BorderLayout.NORTH);
        jPanel2.add(panelFiltrosAvanzados, java.awt.BorderLayout.CENTER);

        // Configurar eventos
        cbxCategoria.addActionListener(e -> applyFilters());
        cbxMarca.addActionListener(e -> applyFilters());
        chkStockBajo.addActionListener(e -> applyFilters());
        chkStockMedio.addActionListener(e -> applyFilters());
        chkStockAlto.addActionListener(e -> applyFilters());
        txtPrecioMin.addActionListener(e -> applyFilters());
        txtPrecioMax.addActionListener(e -> applyFilters());
        btnExportar.addActionListener(e -> exportarAExcel());
        btnLimpiarFiltros.addActionListener(e -> limpiarFiltros());
    }

    private void cargarCategorias() {
        cbxCategoria.removeAllItems();
        cbxCategoria.addItem("Todas");
        try {
            ServiceCategory service = new ServiceCategory();
            java.util.List<ModelCategory> list = service.getAll();
            for (ModelCategory cat : list) {
                if (cat.getName() != null) {
                    cbxCategoria.addItem(cat.getName());
                }
            }
        } catch (Exception ex) {
            System.err.println("Error loading categories: " + ex.getMessage());
            // No mostrar popup modal para no bloquear inicio si falla, solo log
        }
    }

    private void cargarMarcas() {
        cbxMarca.removeAllItems();
        cbxMarca.addItem("Todas");
        try {
            ServiceBrand service = new ServiceBrand();
            java.util.List<ModelBrand> list = service.getAll();
            for (ModelBrand brand : list) {
                if (brand.getName() != null) {
                    cbxMarca.addItem(brand.getName());
                }
            }
        } catch (Exception ex) {
            System.err.println("Error loading brands: " + ex.getMessage());
        }
    }

    private void limpiarFiltros() {
        cbxCategoria.setSelectedIndex(0);
        cbxMarca.setSelectedIndex(0);
        txtPrecioMin.setText("");
        txtPrecioMax.setText("");
        chkStockBajo.setSelected(false);
        chkStockMedio.setSelected(false);
        chkStockAlto.setSelected(false);
        applyFilters();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FASE 3: INDICADORES VISUALES DE STOCK
    // ═══════════════════════════════════════════════════════════════════════════

    private class StockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                try {
                    int stock = Integer.parseInt(value.toString());

                    if (stock < 10) {
                        // Stock BAJO - Rojo
                        c.setBackground(new Color(255, 102, 102));
                        c.setForeground(Color.WHITE);
                        setHorizontalAlignment(CENTER);
                        setText("🔴 " + stock);
                    } else if (stock <= 50) {
                        // Stock MEDIO - Naranja/Amarillo
                        c.setBackground(new Color(255, 204, 102));
                        c.setForeground(Color.BLACK);
                        setHorizontalAlignment(CENTER);
                        setText("🟠 " + stock);
                    } else {
                        // Stock ALTO - Verde
                        c.setBackground(new Color(144, 238, 144));
                        c.setForeground(Color.BLACK);
                        setHorizontalAlignment(CENTER);
                        setText("🟢 " + stock);
                    }

                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                        c.setForeground(table.getSelectionForeground());
                    }
                } catch (NumberFormatException e) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FASE 4: EXPORTACIÓN A EXCEL
    // ═══════════════════════════════════════════════════════════════════════════

    private void exportarAExcel() {
        if (baseList == null || baseList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay datos para exportar",
                    "Exportar",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Selector de archivo
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como Excel");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx");
        fileChooser.setFileFilter(filter);
        fileChooser.setSelectedFile(new File("Productos_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath += ".xlsx";
            }

            try {
                exportarListaAExcel(baseList, filePath);

                int opcion = JOptionPane.showConfirmDialog(this,
                        "Archivo exportado exitosamente en:\n" + filePath + "\n\n¿Desea abrirlo?",
                        "Exportación Exitosa",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (opcion == JOptionPane.YES_OPTION) {
                    Desktop.getDesktop().open(new File(filePath));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error al exportar: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarListaAExcel(List<ProductoBusquedaItem> productos, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Productos");

        // Estilos
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Información de exportación
        Row infoRow = sheet.createRow(0);
        Cell infoCell = infoRow.createCell(0);
        infoCell.setCellValue("Reporte de Productos - Generado: "
                + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()));

        // Usuario
        Row userRow = sheet.createRow(1);
        Cell userCell = userRow.createCell(0);
        String username = UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getUsername()
                : "Sistema";
        userCell.setCellValue("Usuario: " + username);

        // Filtros aplicados
        Row filtrosRow = sheet.createRow(2);
        Cell filtrosCell = filtrosRow.createCell(0);
        StringBuilder filtros = new StringBuilder("Filtros: ");
        if (cbxBodega.getSelectedIndex() > 0)
            filtros.append("Bodega=").append(cbxBodega.getSelectedItem()).append("; ");
        if (cbxCategoria != null && cbxCategoria.getSelectedIndex() > 0)
            filtros.append("Categoría=").append(cbxCategoria.getSelectedItem()).append("; ");
        if (cbxMarca != null && cbxMarca.getSelectedIndex() > 0)
            filtros.append("Marca=").append(cbxMarca.getSelectedItem()).append("; ");
        filtrosCell.setCellValue(filtros.toString());

        // Encabezados
        Row headerRow = sheet.createRow(4);
        String[] columnas = { "ID", "EAN", "Nombre", "Marca", "Talla", "Género", "Stock", "Tipo", "Bodega" };
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
        int rowNum = 5;
        for (ProductoBusquedaItem p : productos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getIdVariante());
            row.createCell(1).setCellValue(p.getEan() != null ? p.getEan() : "");
            row.createCell(2).setCellValue(p.getNombre() != null ? p.getNombre() : "");
            row.createCell(3).setCellValue(p.getMarca() != null ? p.getMarca() : "");
            row.createCell(4).setCellValue(p.getTalla() != null ? p.getTalla() : "");
            row.createCell(5).setCellValue(p.getGenero() != null ? p.getGenero() : "");
            row.createCell(6).setCellValue(p.getStock());
            row.createCell(7).setCellValue(p.getTipo() != null ? p.getTipo() : "");
            row.createCell(8).setCellValue(p.getBodega() != null ? p.getBodega() : "");
        }

        // Auto-ajustar columnas
        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Guardar
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}