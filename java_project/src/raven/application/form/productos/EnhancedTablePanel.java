package raven.application.form.productos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import raven.clases.productos.ServiceProductVariant;
import raven.controlador.productos.ModelProductVariant;

/**
 * Panel mejorado para mostrar variantes de productos en formato tabla.
 * Incluye soporte para visualización y edición de imágenes de variantes.
 */
public class EnhancedTablePanel extends JPanel {
    
    // Colores del tema oscuro - Paleta corporativa del sistema
    private static final Color BG_MODAL = new Color(52, 69, 84);         // #344554
    private static final Color BG_TABLE = new Color(55, 65, 81);         // #374151
    private static final Color BG_TABLE_ROW_ALT = new Color(38, 46, 62); // #262E3E
    private static final Color BG_HEADER = new Color(44, 62, 80);        // #2c3e50
    private static final Color TEXT_PRIMARY = new Color(236, 240, 241);  // #ecf0f1
    private static final Color TEXT_SECONDARY = new Color(160, 174, 192); // #A0AEC0
    private static final Color BORDER_COLOR = new Color(74, 85, 104);    // #4A5568
    private static final Color SELECTION_BG = new Color(66, 165, 245, 30); // #42A5F5 con transparencia
    private static final Color ACCENT_ORANGE = new Color(251, 146, 60);  // #FB923C
    
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final ImageManager imageManager;
    private ImageCellRenderer imageCellRenderer = null;
    private ImageCellEditor imageCellEditor = null;
    
    /**
     * Constructor que inicializa el panel con las variantes y el servicio.
     * 
     * @param variants Lista de variantes a mostrar en la tabla
     * @param variantService Servicio para operaciones de persistencia de variantes
     */
    public EnhancedTablePanel(List<ModelProductVariant> variants, 
                             ServiceProductVariant variantService) {
        setLayout(new BorderLayout());
        setBackground(BG_MODAL);
        
        this.imageManager = new ImageManager(variantService);
        
        // Crear modelo de tabla con columnas especificadas
        String[] columns = {"Imagen", "Talla", "Color", "SKU", 
                           "Stock", "P. Compra", "P. Venta", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Solo la columna de imagen (columna 0) es editable
                return column == 0;
            }
        };
        
        // Crear tabla con el modelo
        table = new JTable(tableModel);
        setupTable();
        loadVariants(variants);
        
        // Agregar tabla con scroll al panel
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(BG_MODAL);
        scrollPane.getViewport().setBackground(BG_TABLE);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Configura los estilos y propiedades de la tabla.
     * Establece altura de fila, fuentes, renderizadores y anchos de columna.
     */
    private void setupTable() {
        // Configurar altura de fila a 55 píxeles
        table.setRowHeight(55);
        
        // Configurar renderizador y editor para columna de imagen
        imageCellRenderer = new ImageCellRenderer();
        imageCellEditor = new ImageCellEditor(imageManager, table);
        
        TableColumn imageColumn = table.getColumnModel().getColumn(0);
        imageColumn.setCellRenderer(imageCellRenderer);
        imageColumn.setCellEditor(imageCellEditor);
        imageColumn.setPreferredWidth(70);
        imageColumn.setMaxWidth(70);
        imageColumn.setMinWidth(70);
        
        // Configurar anchos de otras columnas
        table.getColumnModel().getColumn(1).setPreferredWidth(80);  // Talla
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Color
        table.getColumnModel().getColumn(3).setPreferredWidth(120); // SKU
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Stock
        table.getColumnModel().getColumn(5).setPreferredWidth(100); // P. Compra
        table.getColumnModel().getColumn(6).setPreferredWidth(100); // P. Venta
        table.getColumnModel().getColumn(7).setPreferredWidth(120); // Estado
        
        // Aplicar estilos consistentes con tema oscuro
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_TABLE);
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_COLOR);
        
        // Estilos del header
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(BG_HEADER);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 8));
    }
    
    /**
     * Carga las variantes en la tabla.
     * Cada fila contiene el objeto ModelProductVariant completo en la columna de imagen
     * para que el renderizador pueda acceder a los datos de la imagen.
     * 
     * @param variants Lista de variantes a cargar
     */
    private void loadVariants(List<ModelProductVariant> variants) {
        tableModel.setRowCount(0);
        for (ModelProductVariant v : variants) {
            // Cargar imagen desde BD si aún no está en memoria
            if (v != null && v.getVariantId() > 0 && (v.getImageBytes() == null || v.getImageBytes().length == 0)) {
                try {
                    byte[] imageBytes = imageManager.getVariantImage(v.getVariantId());
                    if (imageBytes != null && imageBytes.length > 0) {
                        v.setImageBytes(imageBytes);
                        v.setHasImage(true);
                    }
                } catch (SQLException ex) {
                    System.err.println("No se pudo cargar la imagen de la variante " + v.getVariantId() + ": " + ex.getMessage());
                }
            }

            Object[] row = new Object[]{
                v, // La variante completa para el renderizador de imagen
                v.getSizeName(),
                v.getColorName(),
                v.getSku(),
                v.getStock(),
                String.format("$%,.0f", v.getPurchasePrice()),
                String.format("$%,.0f", v.getSalePrice()),
                v.isAvailable() ? "Disponible" : "No disponible"
            };
            tableModel.addRow(row);
        }
    }
    
    /**
     * Refresca la visualización de una fila específica después de actualizar su imagen.
     * 
     * @param row Índice de la fila a refrescar
     */
    public void refreshVariant(int row) {
        table.repaint();
    }
}
