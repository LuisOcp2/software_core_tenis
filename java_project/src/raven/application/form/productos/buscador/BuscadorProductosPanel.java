package raven.application.form.productos.buscador;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class BuscadorProductosPanel extends JPanel {

    private final ProductoBusquedaService service = new ProductoBusquedaService();
    private final JTextField txtBuscar = new JTextField();
    private final JTable table = new JTable();
    private final DefaultTableModel model;
    
    // Caché y Executor para carga asíncrona de imágenes
    private final Map<Integer, ImageIcon> imageCache = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> pendingImages = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final ExecutorService imageLoader = Executors.newFixedThreadPool(4); // 4 hilos para imágenes
    private final ImageIcon loadingIcon; 
    private final ImageIcon defaultIcon;

    private java.util.function.Consumer<ProductoBusquedaItem> onProductoSeleccionado;

    public void setOnProductoSeleccionado(java.util.function.Consumer<ProductoBusquedaItem> listener) {
        this.onProductoSeleccionado = listener;
    }

    public BuscadorProductosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Iconos por defecto (puedes ajustar las rutas según tu proyecto)
        loadingIcon = new FlatSVGIcon("raven/icon/svg/load.svg", 30, 30); // Placeholder
        defaultIcon = new FlatSVGIcon("raven/icon/svg/image.svg", 30, 30); // Fallback

        // Configuración del buscador
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.add(new JLabel("Buscar (Formato: Nombre, Color, Talla) o General:"), BorderLayout.NORTH);
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: Adidas, Rojo, 40  ó  Código de barras...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:5,10,5,10");
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        topPanel.add(txtBuscar, BorderLayout.CENTER);
        
        // Listener para búsqueda
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { buscar(); }
            public void removeUpdate(DocumentEvent e) { buscar(); }
            public void changedUpdate(DocumentEvent e) { buscar(); }
        });

        add(topPanel, BorderLayout.NORTH);

        // Configuración de la tabla
        String[] cols = {"ID", "EAN", "Foto", "Nombre", "Marca", "Talla", "Género", "Stock", "Tipo", "Bodega"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return ImageIcon.class;
                if (columnIndex == 7) return Integer.class;
                return String.class;
            }
        };
        
        table.setModel(model);
        table.setRowHeight(50); // Altura suficiente para la imagen
        table.getTableHeader().setReorderingAllowed(false);
        
        // Mouse Listener: Copiar EAN y Selección (Doble Clic)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                
                // Doble clic para seleccionar
                if (e.getClickCount() == 2 && row >= 0) {
                    Object idObj = table.getValueAt(row, 0);
                    // Validar que sea un ID válido (Integer) y no una fila de mensaje
                    if (onProductoSeleccionado != null && idObj instanceof Integer) {
                        // Reconstruir el item desde la fila
                        ProductoBusquedaItem item = new ProductoBusquedaItem();
                        item.setIdVariante((Integer) idObj);
                        item.setEan((String) table.getValueAt(row, 1));
                        item.setNombre((String) table.getValueAt(row, 3));
                        item.setMarca((String) table.getValueAt(row, 4));
                        item.setTalla((String) table.getValueAt(row, 5));
                        item.setGenero((String) table.getValueAt(row, 6));
                        item.setStock((Integer) table.getValueAt(row, 7));
                        item.setTipo((String) table.getValueAt(row, 8));
                        item.setBodega((String) table.getValueAt(row, 9));
                        onProductoSeleccionado.accept(item);
                    }
                    return;
                }

                // Clic simple en columna EAN para copiar
                if (row >= 0 && col == 1) { // Columna EAN
                    Object value = table.getValueAt(row, col);
                    if (value != null) {
                        String ean = value.toString();
                        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(ean);
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        javax.swing.JOptionPane.showMessageDialog(BuscadorProductosPanel.this, "Código EAN copiado: " + ean, "Copiado", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        
        // Ocultar columna ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Anchos de columna
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // EAN
        table.getColumnModel().getColumn(2).setPreferredWidth(60);  // Foto
        table.getColumnModel().getColumn(3).setPreferredWidth(250); // Nombre
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Marca
        table.getColumnModel().getColumn(5).setPreferredWidth(50);  // Talla
        
        // Renderizador de imágenes asíncrono
        table.getColumnModel().getColumn(2).setCellRenderer(new AsyncImageRenderer());
        
        // Renderizador centrado para el resto
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i : new int[]{1, 4, 5, 6, 7, 8, 9}) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);
        
        // Carga inicial
        buscar();
    }
    
    // Método público para enfocar el buscador al abrir
    public void focusSearch() {
        txtBuscar.requestFocusInWindow();
    }

    private void buscar() {
        String query = txtBuscar.getText();

        // Cancelar búsqueda anterior si existe
        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }

        // Mostrar indicador de carga
        SwingUtilities.invokeLater(() -> {
            // Limpiar resultados anteriores
            model.setRowCount(0);
            // Agregar fila de carga si hay búsqueda activa
            if (query != null && !query.trim().isEmpty()) {
                model.addRow(new Object[]{"", "Buscando...", null, "Cargando...", "", "", "", 0, "", ""});
            }
        });

        // Ejecutar búsqueda en hilo separado
        searchWorker = new javax.swing.SwingWorker<List<ProductoBusquedaItem>, Void>() {
            @Override
            protected List<ProductoBusquedaItem> doInBackground() throws Exception {
                return service.buscarProductos(query);
            }

            @Override
            protected void done() {
                try {
                    List<ProductoBusquedaItem> resultados = get();
                    SwingUtilities.invokeLater(() -> {
                        // Limpiar cualquier fila de carga
                        model.setRowCount(0);

                        for (ProductoBusquedaItem item : resultados) {
                            model.addRow(new Object[]{
                                item.getIdVariante(),
                                item.getEan(),
                                null, // La imagen se cargará asíncronamente
                                item.getNombre(),
                                item.getMarca(),
                                item.getTalla(),
                                item.getGenero(),
                                item.getStock(),
                                item.getTipo(),
                                item.getBodega()
                            });
                        }

                        // Mostrar mensaje si no hay resultados
                        if (resultados.isEmpty() && query != null && !query.trim().isEmpty()) {
                            model.addRow(new Object[]{"", "", null, "No se encontraron productos", "", "", "", 0, "", ""});
                        }
                    });
                } catch (java.util.concurrent.CancellationException e) {
                    // Búsqueda cancelada, no hacer nada
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        model.addRow(new Object[]{"", "", null, "Error en la búsqueda", "", "", "", 0, "", ""});
                    });
                }
            }
        };

        searchWorker.execute();
    }

    private javax.swing.SwingWorker<List<ProductoBusquedaItem>, Void> searchWorker;

    // Renderizador que gestiona la carga lazy
    private class AsyncImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(""); // Solo imagen
            label.setHorizontalAlignment(JLabel.CENTER);
            
            // Obtener ID de la variante (columna 0 oculta)
            Object idObj = table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
            
            // Verificación de tipo para evitar ClassCastException (por filas de "Cargando..." o "No encontrado")
            if (idObj == null || !(idObj instanceof Integer)) {
                label.setIcon(null); // O un icono transparente si es necesario
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
                if (imageCache.containsKey(idVariante)) return;

                ImageIcon icon = service.obtenerImagenVariante(idVariante);
                
                if (icon != null) {
                    Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(img);
                }
                
                imageCache.put(idVariante, icon != null ? icon : defaultIcon);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pendingImages.remove(idVariante);
                SwingUtilities.invokeLater(table::repaint);
            }
        });
    }
}
