package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.productos.ServiceProductVariant;
import raven.controlador.productos.ModelProductVariant;
import raven.utils.PerformanceOptimizer;

/**
 * Panel mejorado que muestra las variantes de un producto en formato matriz (Talla × Color)
 * con colores según el nivel de stock y funcionalidades avanzadas.
 * 
 * NUEVAS FUNCIONALIDADES v2.0:
 * - Edición inline de stock
 * - Filtros de tallas y colores
 * - Resumen por fila y columna
 * - Búsqueda rápida
 * - Indicadores visuales mejorados
 * 
 * @author Kiro AI Assistant
 * @version 2.0
 */
public class VariantMatrixPanel extends JPanel {
    
    // Colores del tema oscuro - Paleta corporativa del sistema
    private static final Color BG_PANEL = new Color(55, 65, 81);         // #374151
    private static final Color BG_TABLE = new Color(55, 65, 81);         // #374151
    private static final Color BG_HEADER = new Color(44, 62, 80);        // #2c3e50
    private static final Color TEXT_PRIMARY = new Color(236, 240, 241);  // #ecf0f1
    private static final Color TEXT_SECONDARY = new Color(160, 174, 192); // #A0AEC0
    private static final Color BORDER_COLOR = new Color(74, 85, 104);    // #4A5568
    
    // Colores para niveles de stock (ajustados para tema oscuro)
    private static final Color STOCK_HIGH = new Color(34, 139, 34);       // Verde oscuro
    private static final Color STOCK_MEDIUM = new Color(218, 165, 32);    // Amarillo oscuro
    private static final Color STOCK_LOW = new Color(178, 34, 34);        // Rojo oscuro
    private static final Color STOCK_EMPTY = new Color(70, 70, 70);       // Gris oscuro
    
    private static final Color TEXT_HIGH = new Color(144, 238, 144);      // Verde claro
    private static final Color TEXT_MEDIUM = new Color(255, 215, 0);      // Amarillo claro
    private static final Color TEXT_LOW = new Color(255, 99, 71);         // Rojo claro
    private static final Color TEXT_EMPTY = new Color(160, 174, 192);     // Gris claro
    
    private JTable matrixTable;
    private DefaultTableModel tableModel;
    private JPanel legendPanel;
    private JLabel totalStockLabel;
    private JPanel toolbarPanel;
    private JTextField searchField;
    private JButton exportButton;
    private JCheckBox showEmptyCheckBox;
    
    private List<ModelProductVariant> variants;
    private Map<String, Map<String, VariantCell>> matrixData;
    private final Set<String> hiddenSizes = new HashSet<>();
    private final Set<String> hiddenColors = new HashSet<>();

    // Gestión de imágenes para miniaturas en celdas
    private final ServiceProductVariant variantService;
    private final Map<Integer, ImageIcon> imageCache = new ConcurrentHashMap<>();
    private static final int THUMBNAIL_SIZE = 32;
    private final FontIcon placeholderIcon = FontIcon.of(FontAwesomeSolid.IMAGE, 18, TEXT_SECONDARY);
    
    // Listener para cambios de stock
    private StockChangeListener stockChangeListener;
    
    // Debouncer para búsqueda optimizada
    private final PerformanceOptimizer.Debouncer searchDebouncer = new PerformanceOptimizer.Debouncer();
    
    public VariantMatrixPanel() {
        this.variantService = null;
        initComponents();
        setupStyles();
    }

    public VariantMatrixPanel(ServiceProductVariant variantService) {
        this.variantService = variantService;
        initComponents();
        setupStyles();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(BG_PANEL);
        
        // Toolbar superior con búsqueda y opciones
        toolbarPanel = createToolbarPanel();
        add(toolbarPanel, BorderLayout.NORTH);
        
        // Tabla matriz
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Solo las celdas de datos son editables (no la columna de tallas ni totales)
                if (column == 0 || column == tableModel.getColumnCount() - 1) {
                    return false;
                }
                // No editar fila de totales
                if (row == tableModel.getRowCount() - 1) {
                    return false;
                }
                Object value = getValueAt(row, column);
                return value instanceof VariantCell && ((VariantCell) value).exists();
            }
        };
        
        matrixTable = new JTable(tableModel);
        matrixTable.setRowHeight(70);
        matrixTable.setShowGrid(false);
        matrixTable.setIntercellSpacing(new Dimension(4, 4));
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Renderer personalizado para las celdas
        MatrixCellRenderer cellRenderer = new MatrixCellRenderer();
        matrixTable.setDefaultRenderer(Object.class, cellRenderer);
        
        // Editor personalizado para edición inline
        matrixTable.setDefaultEditor(Object.class, new MatrixCellEditor());
        
        // Renderer para el header
        JTableHeader header = matrixTable.getTableHeader();
        header.setDefaultRenderer(new MatrixHeaderRenderer());
        
        JScrollPane scrollPane = new JScrollPane(matrixTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(BG_TABLE);
        scrollPane.setBackground(BG_PANEL);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con leyenda y totales
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BG_PANEL);
        
        legendPanel = createLegendPanel();
        bottomPanel.add(legendPanel, BorderLayout.CENTER);
        
        totalStockLabel = new JLabel("Total: 0 pares");
        totalStockLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalStockLabel.setForeground(TEXT_PRIMARY);
        totalStockLabel.setBorder(new EmptyBorder(10, 15, 10, 15));
        bottomPanel.add(totalStockLabel, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Listener para clicks en celdas
        matrixTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    handleCellDoubleClick();
                }
            }
        });
    }
    
    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        // Panel izquierdo con búsqueda
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(BG_PANEL);
        
        JLabel searchLabel = new JLabel("🔍");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        searchField = new JTextField(20);
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar talla o color...");
        searchField.setBackground(BG_TABLE);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                // Aplicar debounce de 300ms para evitar filtrados excesivos
                searchDebouncer.debounce(() -> filterMatrix(), 300);
            }
        });
        
        leftPanel.add(searchLabel);
        leftPanel.add(searchField);
        
        // Panel derecho con opciones
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(BG_PANEL);
        
        showEmptyCheckBox = new JCheckBox("Mostrar vacíos", true);
        showEmptyCheckBox.setBackground(BG_PANEL);
        showEmptyCheckBox.setForeground(TEXT_PRIMARY);
        showEmptyCheckBox.addActionListener(e -> rebuildMatrix());
        
        exportButton = new JButton("📊 Exportar");
        exportButton.setBackground(BG_TABLE);
        exportButton.setForeground(TEXT_PRIMARY);
        exportButton.addActionListener(e -> exportToExcel());
        
        JButton refreshButton = new JButton("🔄 Actualizar");
        refreshButton.setBackground(BG_TABLE);
        refreshButton.setForeground(TEXT_PRIMARY);
        refreshButton.addActionListener(e -> rebuildMatrix());
        
        rightPanel.add(showEmptyCheckBox);
        rightPanel.add(refreshButton);
        rightPanel.add(exportButton);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void setupStyles() {
        matrixTable.putClientProperty(FlatClientProperties.STYLE,
                "selectionBackground:lighten($Component.accentColor,35%);" +
                "selectionForeground:$Table.foreground");
        
        matrixTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:40;font:bold;background:#f8f9fa;foreground:#5a6c7d");
        
        searchField.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;borderWidth:1;margin:4,8,4,8");
        
        String buttonStyle = "arc:8;borderWidth:0;background:#42A5F5;foreground:#ffffff;" +
                "hoverBackground:#64B5F6;pressedBackground:#1E88E5";
        exportButton.putClientProperty(FlatClientProperties.STYLE, buttonStyle);
    }
    
    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        panel.add(createLegendItem("🟢 Stock Alto (>20)", STOCK_HIGH));
        panel.add(createLegendItem("🟡 Stock Medio (10-20)", STOCK_MEDIUM));
        panel.add(createLegendItem("🔴 Stock Bajo (<10)", STOCK_LOW));
        panel.add(createLegendItem("⚪ Sin Stock", STOCK_EMPTY));
        
        return panel;
    }
    
    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setBackground(BG_PANEL);
        
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(30, 20));
        colorBox.setBackground(color);
        colorBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_PRIMARY);
        
        item.add(colorBox);
        item.add(label);
        
        return item;
    }
    
    /**
     * Establece las variantes a mostrar en la matriz
     */
    public void setVariants(List<ModelProductVariant> variants) {
        this.variants = variants;
        preloadVariantImages();
        buildMatrix();
        updateTotalStock();
    }

    /**
     * Precarga imágenes de las variantes desde la BD (si hay servicio disponible)
     * y construye miniaturas cacheadas para usarlas en el renderer.
     */
    private void preloadVariantImages() {
        if (variants == null || variants.isEmpty() || variantService == null) {
            return;
        }

        for (ModelProductVariant v : variants) {
            if (v == null || v.getVariantId() <= 0) {
                continue;
            }
            try {
                // Cargar bytes de imagen si aún no están presentes
                if (v.getImageBytes() == null || v.getImageBytes().length == 0) {
                    byte[] bytes = variantService.getVariantImage(v.getVariantId());
                    if (bytes != null && bytes.length > 0) {
                        v.setImageBytes(bytes);
                        v.setHasImage(true);
                    }
                }

                if (v.getImageBytes() != null && v.getImageBytes().length > 0) {
                    imageCache.computeIfAbsent(v.getVariantId(), id -> createThumbnail(v.getImageBytes()));
                }
            } catch (SQLException ex) {
                System.err.println("No se pudo cargar la imagen de la variante " + v.getVariantId() + ": " + ex.getMessage());
            }
        }
    }

    private ImageIcon createThumbnail(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage original = ImageIO.read(bais);
            if (original == null) {
                return null;
            }
            Image scaled = original.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) {
            System.err.println("Error creando miniatura de variante: " + e.getMessage());
            return null;
        }
    }

    private ImageIcon getThumbnailForVariant(ModelProductVariant variant) {
        if (variant == null || variant.getVariantId() <= 0) {
            return null;
        }
        ImageIcon cached = imageCache.get(variant.getVariantId());
        if (cached != null) {
            return cached;
        }
        if (variant.getImageBytes() != null && variant.getImageBytes().length > 0) {
            ImageIcon thumb = createThumbnail(variant.getImageBytes());
            if (thumb != null) {
                imageCache.put(variant.getVariantId(), thumb);
            }
            return thumb;
        }
        return null;
    }
    
    private void buildMatrix() {
        // Limpiar tabla
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        
        if (variants == null || variants.isEmpty()) {
            return;
        }
        
        // Organizar datos en estructura de matriz
        matrixData = new LinkedHashMap<>();
        Set<String> coloresSet = new LinkedHashSet<>();
        
        for (ModelProductVariant variant : variants) {
            String talla = variant.getSizeName();
            String color = variant.getColorName();
            int stock = variant.getStock();
            
            if (talla == null || color == null) continue;
            
            // Aplicar filtros
            if (!showEmptyCheckBox.isSelected() && stock == 0) continue;
            if (hiddenSizes.contains(talla) || hiddenColors.contains(color)) continue;
            
            matrixData.putIfAbsent(talla, new LinkedHashMap<>());
            matrixData.get(talla).put(color, new VariantCell(variant, stock));
            coloresSet.add(color);
        }
        
        // Ordenar tallas numéricamente
        List<String> tallasOrdenadas = new ArrayList<>(matrixData.keySet());
        tallasOrdenadas.sort((t1, t2) -> {
            try {
                String num1 = t1.replaceAll("[^0-9.]", "");
                String num2 = t2.replaceAll("[^0-9.]", "");
                return Double.compare(Double.parseDouble(num1), Double.parseDouble(num2));
            } catch (NumberFormatException e) {
                return t1.compareTo(t2);
            }
        });
        
        List<String> coloresOrdenados = new ArrayList<>(coloresSet);
        
        // Configurar columnas
        tableModel.addColumn("Talla");
        for (String color : coloresOrdenados) {
            tableModel.addColumn(color);
        }
        tableModel.addColumn("Total");
        
        // Llenar filas
        for (String talla : tallasOrdenadas) {
            Object[] row = new Object[coloresOrdenados.size() + 2];
            row[0] = talla;
            
            Map<String, VariantCell> coloresMap = matrixData.get(talla);
            int totalFila = 0;
            
            for (int i = 0; i < coloresOrdenados.size(); i++) {
                String color = coloresOrdenados.get(i);
                VariantCell cell = coloresMap.getOrDefault(color, new VariantCell(null, 0));
                row[i + 1] = cell;
                if (cell.exists()) {
                    totalFila += cell.stock;
                }
            }
            
            row[row.length - 1] = totalFila;
            tableModel.addRow(row);
        }
        
        // Agregar fila de totales
        Object[] totalRow = new Object[coloresOrdenados.size() + 2];
        totalRow[0] = "TOTAL";
        
        int granTotal = 0;
        for (int i = 0; i < coloresOrdenados.size(); i++) {
            String color = coloresOrdenados.get(i);
            int totalColumna = 0;
            
            for (String talla : tallasOrdenadas) {
                VariantCell cell = matrixData.get(talla).get(color);
                if (cell != null && cell.exists()) {
                    totalColumna += cell.stock;
                }
            }
            
            totalRow[i + 1] = totalColumna;
            granTotal += totalColumna;
        }
        
        totalRow[totalRow.length - 1] = granTotal;
        tableModel.addRow(totalRow);
        
        // Ajustar ancho de columnas
        matrixTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        matrixTable.getColumnModel().getColumn(0).setMinWidth(100);
        
        for (int i = 1; i < matrixTable.getColumnCount() - 1; i++) {
            matrixTable.getColumnModel().getColumn(i).setPreferredWidth(100);
            matrixTable.getColumnModel().getColumn(i).setMinWidth(80);
        }
        
        matrixTable.getColumnModel().getColumn(matrixTable.getColumnCount() - 1).setPreferredWidth(100);
        matrixTable.getColumnModel().getColumn(matrixTable.getColumnCount() - 1).setMinWidth(80);
    }
    
    private void rebuildMatrix() {
        buildMatrix();
        updateTotalStock();
    }
    
    private void filterMatrix() {
        String searchText = searchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            hiddenSizes.clear();
            hiddenColors.clear();
        } else {
            // Filtrar tallas y colores que no coincidan con la búsqueda
            hiddenSizes.clear();
            hiddenColors.clear();
            
            if (matrixData != null) {
                for (String talla : matrixData.keySet()) {
                    if (!talla.toLowerCase().contains(searchText)) {
                        hiddenSizes.add(talla);
                    }
                }
                
                for (Map<String, VariantCell> coloresMap : matrixData.values()) {
                    for (String color : coloresMap.keySet()) {
                        if (!color.toLowerCase().contains(searchText)) {
                            hiddenColors.add(color);
                        }
                    }
                }
            }
        }
        
        rebuildMatrix();
    }
    
    private void updateTotalStock() {
        if (variants == null || variants.isEmpty()) {
            totalStockLabel.setText("Total: 0 pares");
            return;
        }
        
        int total = variants.stream()
                .mapToInt(ModelProductVariant::getStock)
                .sum();
        
        totalStockLabel.setText(String.format("Total: %,d pares", total));
    }
    
    private void handleCellDoubleClick() {
        int row = matrixTable.getSelectedRow();
        int col = matrixTable.getSelectedColumn();
        
        if (row < 0 || col <= 0 || row == matrixTable.getRowCount() - 1) return;
        
        Object cellValue = matrixTable.getValueAt(row, col);
        if (cellValue instanceof VariantCell) {
            VariantCell cell = (VariantCell) cellValue;
            if (cell.variant != null) {
                showVariantEditDialog(cell.variant);
            }
        }
    }
    
    private void showVariantEditDialog(ModelProductVariant variant) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        panel.add(new JLabel("Talla:"));
        panel.add(new JLabel(variant.getSizeName()));
        
        panel.add(new JLabel("Color:"));
        panel.add(new JLabel(variant.getColorName()));
        
        panel.add(new JLabel("Stock actual:"));
        JSpinner stockSpinner = new JSpinner(new SpinnerNumberModel(variant.getStock(), 0, 9999, 1));
        panel.add(stockSpinner);
        
        panel.add(new JLabel("SKU:"));
        panel.add(new JLabel(variant.getSku()));
        
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Editar Variante",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            int newStock = (Integer) stockSpinner.getValue();
            variant.setStock(newStock);
            
            // Notificar cambio
            if (stockChangeListener != null) {
                stockChangeListener.onStockChanged(variant, newStock);
            }
            
            rebuildMatrix();
        }
    }
    
    private void exportToExcel() {
        JOptionPane.showMessageDialog(this,
                "Funcionalidad de exportación a Excel\n" +
                "Se implementará próximamente",
                "Exportar a Excel",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Métodos públicos
    
    public void setStockChangeListener(StockChangeListener listener) {
        this.stockChangeListener = listener;
    }
    
    /**
     * Clase interna para almacenar datos de celda
     */
    private static class VariantCell {
        ModelProductVariant variant;
        int stock;
        
        VariantCell(ModelProductVariant variant, int stock) {
            this.variant = variant;
            this.stock = stock;
        }
        
        boolean exists() {
            return variant != null;
        }
    }
    
    /**
     * Renderer personalizado para las celdas de la matriz
     */
    private class MatrixCellRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            // Primera columna (tallas) o fila de totales
            if (column == 0 || row == table.getRowCount() - 1) {
                panel.setBackground(BG_HEADER);
                JLabel label = new JLabel(value.toString());
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setForeground(TEXT_PRIMARY);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(label, BorderLayout.CENTER);
                return panel;
            }
            
            // Columna de totales
            if (column == table.getColumnCount() - 1) {
                panel.setBackground(BG_HEADER);
                JLabel label = new JLabel(value.toString() + " pares");
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setForeground(new Color(66, 165, 245)); // Azul claro
                label.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(label, BorderLayout.CENTER);
                return panel;
            }
            
            // Celdas de datos
            if (value instanceof VariantCell) {
                VariantCell cell = (VariantCell) value;
                
                if (!cell.exists()) {
                    panel.setBackground(STOCK_EMPTY);
                    JLabel label = new JLabel("-");
                    label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    label.setForeground(TEXT_EMPTY);
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    JLabel sublabel = new JLabel("no existe");
                    sublabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    sublabel.setForeground(TEXT_EMPTY);
                    sublabel.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                    contentPanel.setOpaque(false);
                    contentPanel.add(label);
                    contentPanel.add(sublabel);
                    
                    panel.add(contentPanel, BorderLayout.CENTER);
                } else {
                    int stock = cell.stock;
                    Color bgColor, textColor;
                    String icon;
                    
                    if (stock == 0) {
                        bgColor = STOCK_EMPTY;
                        textColor = TEXT_EMPTY;
                        icon = "⚪";
                    } else if (stock < 10) {
                        bgColor = STOCK_LOW;
                        textColor = TEXT_LOW;
                        icon = "🔴";
                    } else if (stock <= 20) {
                        bgColor = STOCK_MEDIUM;
                        textColor = TEXT_MEDIUM;
                        icon = "🟡";
                    } else {
                        bgColor = STOCK_HIGH;
                        textColor = TEXT_HIGH;
                        icon = "🟢";
                    }
                    
                    panel.setBackground(bgColor);
                    
                    JLabel stockLabel = new JLabel(icon + " " + stock);
                    stockLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    stockLabel.setForeground(textColor);
                    stockLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    JLabel paresLabel = new JLabel("pares");
                    paresLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    paresLabel.setForeground(textColor);
                    paresLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                    textPanel.setOpaque(false);
                    textPanel.add(stockLabel);
                    textPanel.add(paresLabel);

                    // Miniatura de imagen (si existe) o placeholder
                    JLabel imageLabel = new JLabel();
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    ImageIcon thumb = getThumbnailForVariant(cell.variant);
                    if (thumb != null) {
                        imageLabel.setIcon(thumb);
                    } else {
                        imageLabel.setIcon(placeholderIcon);
                    }

                    JPanel contentPanel = new JPanel(new BorderLayout(4, 0));
                    contentPanel.setOpaque(false);
                    contentPanel.add(imageLabel, BorderLayout.WEST);
                    contentPanel.add(textPanel, BorderLayout.CENTER);
                    
                    panel.add(contentPanel, BorderLayout.CENTER);
                }
                
                if (isSelected) {
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(74, 144, 226), 2),
                            BorderFactory.createEmptyBorder(6, 6, 6, 6)
                    ));
                }
                
                if (cell.exists()) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            } else if (value instanceof Integer) {
                // Totales de columna
                panel.setBackground(new Color(248, 249, 250));
                JLabel label = new JLabel(value.toString());
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setForeground(new Color(52, 152, 219));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(label, BorderLayout.CENTER);
            }
            
            return panel;
        }
    }
    
    /**
     * Editor personalizado para edición inline
     */
    private class MatrixCellEditor extends DefaultCellEditor {
        private final JSpinner spinner;
        private VariantCell currentCell;
        
        public MatrixCellEditor() {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            
            if (value instanceof VariantCell) {
                currentCell = (VariantCell) value;
                spinner.setValue(currentCell.stock);
                return spinner;
            }
            
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        
        @Override
        public Object getCellEditorValue() {
            if (currentCell != null) {
                int newStock = (Integer) spinner.getValue();
                currentCell.stock = newStock;
                currentCell.variant.setStock(newStock);
                
                if (stockChangeListener != null) {
                    stockChangeListener.onStockChanged(currentCell.variant, newStock);
                }
            }
            return currentCell;
        }
    }
    
    /**
     * Renderer para el header de la tabla
     */
    private class MatrixHeaderRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = new JLabel(value.toString());
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setForeground(TEXT_PRIMARY);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBackground(BG_HEADER);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(10, 8, 10, 8)
            ));
            
            return label;
        }
    }
    
    /**
     * Interface para notificar cambios de stock
     */
    public interface StockChangeListener {
        void onStockChanged(ModelProductVariant variant, int newStock);
    }
}
