package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import raven.application.form.productos.dto.InventarioDetalleItem;
import raven.dao.InventarioBodegaDAO;
import raven.controlador.principal.conexion;
import raven.toast.Notifications;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Formulario para consulta detallada de inventario con filtros avanzados y UI
 * Material Design 3 optimizada.
 */
public class ConsultaInventarioDetalladoForm extends JPanel {

        private InventarioBodegaDAO inventarioDAO;
        private List<InventarioDetalleItem> listaActual = new ArrayList<>();

        // Componentes UI
        private JTextField txtBuscar;
        private JComboBox<ComboItem> cbxBodega;
        private JComboBox<ComboItem> cbxMarca;
        private JComboBox<ComboItem> cbxCategoria;
        private JTextField txtColor;
        private JTextField txtTalla;
        private JTextField txtUbicacion;
        private JCheckBox chkSinUbicacion;
        private JTable tablaResultados;
        private JLabel lblTotalRegistros;
        private JLabel lblTotalStock;
        private JProgressBar progressBar;
        private JButton btnBuscar;
        private JButton btnExportar;
        private JButton btnLimpiar;

        // Pagination Fields
        private int currentPage = 1;
        private final int itemsPerPage = 8;
        private java.util.List<InventarioDetalleItem> allData = new ArrayList<>();
        private java.util.List<raven.application.form.productos.dto.ProductoAgrupado> groupedData = new ArrayList<>();
        private JButton btnPrev;
        private JButton btnNext;
        private JLabel lblPageInfo;

        // Stock Visibility Filters
        private JCheckBox chkMostrarCero;
        private JCheckBox chkMostrarNegativo;
        private JCheckBox chkAgruparVariantes;
        private JComboBox<String> cbxTipoUnidad;

        // Colores y Estilos MD3
        private static final String ROUND_STYLE = "arc:25";
        private static final Color MD_GREEN_COLOR = Color.decode("#28CD41"); // Verde usado en InventarioForm
        private static final Color MD_BLUE_COLOR = Color.decode("#3B82F6"); // Azul moderno
        private static final String PANEL_STYLE = ROUND_STYLE
                        + ";background:lighten(@background,3%);";
        private static final String TEXT_FIELD_STYLE = "arc:15;background:lighten($Menu.background,25%)";

        public ConsultaInventarioDetalladoForm() {
                initComponents();
                inventarioDAO = new InventarioBodegaDAO();
                initData();
        }

        // Método auxiliar para Iconos con color nativo AWT
        private FontIcon createIcon(org.kordamp.ikonli.Ikon ikon, Color color, int size) {
                FontIcon icon = FontIcon.of(ikon, size, color);
                return icon;
        }

        private void initComponents() {
                setLayout(new MigLayout("fill, wrap, insets 20", "[fill]", "[top]20[top]10[fill,grow]10[bottom]"));

                // 1. Título
                JLabel lbTitle = new JLabel("CONSULTA DE INVENTARIO DETALLADO");
                lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");
                lbTitle.setHorizontalAlignment(SwingConstants.CENTER);
                add(lbTitle);

                // 2. Panel de Filtros
                add(createFiltersPanel());

                // 3. Tabla de Resultados
                add(createTablePanel(), "grow");

                // 4. Footer (Totales)
                add(createFooterPanel());
        }

        private JPanel createFiltersPanel() {
                JPanel panel = new JPanel(new MigLayout("fill, insets 25", "[grow][grow][grow][grow]10[]", "[]15[]"));
                panel.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);

                // Fila 1: Combos
                cbxBodega = createComboBox("Bodega");
                cbxMarca = createComboBox("Marca");
                cbxCategoria = createComboBox("Categoría");

                panel.add(createLabel("Bodega:"), "split 2, span 1");
                panel.add(cbxBodega, "growx");

                panel.add(createLabel("Marca:"), "split 2, span 1");
                panel.add(cbxMarca, "growx");

                panel.add(createLabel("Categoría:"), "split 2, span 1");
                panel.add(cbxCategoria, "growx, wrap");

                // Fila 2: Textos y Botones
                txtBuscar = new JTextField();
                txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por Código, Nombre, EAN...");
                txtBuscar.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
                txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                                createIcon(FontAwesomeSolid.SEARCH, Color.GRAY, 14));

                // Search on Enter
                txtBuscar.addActionListener(e -> buscar());

                txtColor = new JTextField();
                txtColor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Color");
                txtColor.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
                txtColor.addActionListener(e -> buscar()); // Search on Enter

                txtTalla = new JTextField();
                txtTalla.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Talla");
                txtTalla.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
                txtTalla.addActionListener(e -> buscar()); // Search on Enter

                txtUbicacion = new JTextField();
                txtUbicacion.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ubicación");
                txtUbicacion.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
                txtUbicacion.addActionListener(e -> buscar());

                chkSinUbicacion = new JCheckBox("Sin Ubicación");
                chkSinUbicacion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chkSinUbicacion.addActionListener(e -> {
                        txtUbicacion.setEnabled(!chkSinUbicacion.isSelected());
                        if (chkSinUbicacion.isSelected())
                                txtUbicacion.setText("");
                        buscar();
                });

                btnBuscar = new JButton("Buscar");
                btnBuscar.setIcon(createIcon(FontAwesomeSolid.SEARCH, Color.WHITE, 14));
                btnBuscar.setBackground(MD_BLUE_COLOR);
                btnBuscar.setForeground(Color.WHITE);
                btnBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:25; font:bold; borderWidth:0");
                btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnBuscar.addActionListener(e -> buscar());

                btnLimpiar = new JButton("Limpiar");
                btnLimpiar.setIcon(createIcon(FontAwesomeSolid.ERASER, Color.DARK_GRAY, 14));
                btnLimpiar.putClientProperty(FlatClientProperties.STYLE, "arc:25; font:bold");
                btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnLimpiar.addActionListener(e -> limpiarFiltros());

                // Logic for limpiarFiltros needs to clear new fields too
                // Assuming limpiarFiltros calls buscar() at the end or resets UI.
                // Let's check if there is a helper method or if I should implement/update it.
                // Looking at the file content provided earlier, I don't see the implementation
                // of limpiarFiltros()
                // It was attached: btnLimpiar.addActionListener(e -> limpiarFiltros());
                // But I didn't see the method definition in the 800 lines provided.
                // However, I should probably make sure it clears these new fields.
                // I will assume it's further down or I should add a check.
                // Wait, I can't see it. I should view the file again to find it OR just update
                // the listener here to be safe?
                // No, better to update the method if I can find it.
                // But this chunk is for createFiltersPanel.
                // I will add the clear logic to the button action directly here just in case,
                // OR better, I will assume the user considers "Limpiar" as "Reset everything"
                // and I should update that method later.
                // For now, let's stick to the UI creation.

                btnExportar = new JButton("Exportar Excel");
                btnExportar.setIcon(createIcon(FontAwesomeSolid.FILE_EXCEL, Color.WHITE, 16));
                btnExportar.setBackground(MD_GREEN_COLOR);
                btnExportar.setForeground(Color.WHITE);
                btnExportar.putClientProperty(FlatClientProperties.STYLE, "arc:25; font:bold +1; borderWidth:0");
                btnExportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnExportar.addActionListener(e -> exportarExcel());

                JButton btnKardex = new JButton("Ver Historial");
                btnKardex.setIcon(createIcon(FontAwesomeSolid.HISTORY, Color.WHITE, 14));
                btnKardex.setBackground(Color.decode("#8E44AD")); // Purple color
                btnKardex.setForeground(Color.WHITE);
                btnKardex.putClientProperty(FlatClientProperties.STYLE, "arc:25; font:bold; borderWidth:0");
                btnKardex.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnKardex.addActionListener(e -> {
                        if (tablaResultados.getSelectedRow() == -1) {
                                Notifications.getInstance().show(Notifications.Type.WARNING,
                                                "Seleccione un producto para ver su historial");
                                return;
                        }
                        abrirKardexSeleccionado();
                });

                panel.add(txtBuscar, "growx, span 1");
                panel.add(txtColor, "growx");
                panel.add(txtTalla, "growx");
                panel.add(txtUbicacion, "growx");

                // Botones agrupados
                JPanel buttonPanel = new JPanel(new MigLayout("insets 0, filly", "[]10[]10[]10[]"));
                buttonPanel.setOpaque(false);
                buttonPanel.add(btnBuscar, "h 40!, w 100!");
                buttonPanel.add(btnLimpiar, "h 40!, w 100!");
                buttonPanel.add(btnKardex, "h 40!, w 130!");
                buttonPanel.add(btnExportar, "h 40!, w 140!");

                panel.add(buttonPanel, "gapleft push, wrap");

                // Fila 3: Filtros de Vista
                chkMostrarCero = new JCheckBox("Mostrar Stock 0");
                chkMostrarCero.setSelected(true);
                chkMostrarCero.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chkMostrarCero.addActionListener(e -> applyMemoryFilters());

                chkMostrarNegativo = new JCheckBox("Mostrar Negativos");
                chkMostrarNegativo.setSelected(true);
                chkMostrarNegativo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chkMostrarNegativo.addActionListener(e -> applyMemoryFilters());

                cbxTipoUnidad = new JComboBox<>(new String[] { "Todos", "Solo Pares", "Solo Cajas" });
                cbxTipoUnidad.putClientProperty(FlatClientProperties.STYLE, "arc:15");
                cbxTipoUnidad.addActionListener(e -> applyMemoryFilters());

                chkAgruparVariantes = new JCheckBox("Agrupar Variantes");
                chkAgruparVariantes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chkAgruparVariantes.addActionListener(e -> applyMemoryFilters());

                panel.add(chkMostrarCero, "split 5");
                panel.add(chkMostrarNegativo);
                panel.add(chkSinUbicacion, "gapleft 10");
                panel.add(chkAgruparVariantes, "gapleft 10");
                panel.add(cbxTipoUnidad, "gapleft 20");

                return panel;
        }

        private JLabel createLabel(String text) {
                JLabel label = new JLabel(text);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                return label;
        }

        private JPanel createTablePanel() {
                JPanel panel = new JPanel(new MigLayout("fill, insets 0", "[fill,grow]", "[fill,grow]"));
                panel.setOpaque(false);

                // Initial setup is classic view
                String[] columnNames = { "Bodega", "ID", "EAN", "Producto", "Marca", "Stock Pares", "Stock Cajas",
                                "Ubicación" };
                DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };
                tablaResultados = new JTable(model);
                tablaResultados.setDefaultEditor(Object.class, null);
                tablaResultados.setRowHeight(90);
                tablaResultados.setFont(tablaResultados.getFont().deriveFont(14f));

                tablaResultados.getColumnModel().getColumn(3).setCellRenderer(new ProductoCardRenderer());

                DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(JLabel.CENTER);

                tablaResultados.getColumnModel().getColumn(1).setPreferredWidth(60);
                tablaResultados.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
                tablaResultados.getColumnModel().getColumn(2).setPreferredWidth(100);
                tablaResultados.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
                tablaResultados.getColumnModel().getColumn(3).setPreferredWidth(300);
                tablaResultados.getColumnModel().getColumn(5).setPreferredWidth(80);
                tablaResultados.getColumnModel().getColumn(5).setCellRenderer(new StockRenderer());
                tablaResultados.getColumnModel().getColumn(6).setPreferredWidth(80);
                tablaResultados.getColumnModel().getColumn(6).setCellRenderer(new StockRenderer());
                tablaResultados.getColumnModel().getColumn(7).setPreferredWidth(120);
                tablaResultados.getColumnModel().getColumn(7).setCellRenderer(new LocationRenderer());

                tablaResultados.getTableHeader().setReorderingAllowed(false);
                tablaResultados.setShowHorizontalLines(true);
                tablaResultados.setShowVerticalLines(false);

                tablaResultados.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "" +
                                "height:50;" +
                                "font:bold +2 $h4.font;" +
                                "background:$Panel.background;" +
                                "foreground:$Label.foreground;" +
                                "hoverBackground:$Table.background;" +
                                "separatorColor:$TableHeader.background");

                tablaResultados.putClientProperty(FlatClientProperties.STYLE, "" +
                                "rowHeight:90;" +
                                "showHorizontalLines:true;" +
                                "intercellSpacing:0,1;" +
                                "cellFocusColor:$Accent.yellow;" +
                                "selectionBackground:$Accent.yellow;" +
                                "selectionForeground:$Table.foreground;");

                // Click-to-Copy functionality for Code
                tablaResultados.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                                int row = tablaResultados.rowAtPoint(e.getPoint());
                                int col = tablaResultados.columnAtPoint(e.getPoint());
                                if (row >= 0 && col == 1) { // 1=Codigo
                                        Object value = tablaResultados.getValueAt(row, col);
                                        if (value != null && !value.toString().isEmpty()) {
                                                String textToCopy = value.toString();
                                                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                                                .setContents(new java.awt.datatransfer.StringSelection(
                                                                                textToCopy), null);
                                                Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                                                Notifications.Location.TOP_RIGHT,
                                                                "Código copiado: " + textToCopy);
                                        }
                                }
                        }
                });

                JScrollPane scroll = new JScrollPane(tablaResultados);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                panel.add(scroll, "grow");

                return panel;
        }

        private JPanel createFooterPanel() {
                JPanel panel = new JPanel(new MigLayout("fill, insets 15", "[]push[]push[]", "[]"));
                panel.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);

                lblTotalRegistros = new JLabel("Total Registros: 0");
                lblTotalRegistros.setFont(lblTotalRegistros.getFont().deriveFont(Font.BOLD, 14f));
                lblTotalRegistros.setIcon(createIcon(FontAwesomeSolid.LIST_ALT, Color.GRAY, 20));

                lblTotalStock = new JLabel("Stock Total: 0 Pares / 0 Cajas");
                lblTotalStock.setFont(lblTotalStock.getFont().deriveFont(Font.BOLD, 14f));
                lblTotalStock.setIcon(createIcon(FontAwesomeSolid.CUBES, Color.GRAY, 20));

                progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                progressBar.setVisible(false);
                progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
                progressBar.setForeground(MD_BLUE_COLOR);

                // Pagination Controls
                JPanel paginationPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]"));
                paginationPanel.setOpaque(false);

                btnPrev = new JButton("Anterior");
                btnPrev.setIcon(createIcon(FontAwesomeSolid.CHEVRON_LEFT, Color.WHITE, 12));
                btnPrev.setBackground(MD_BLUE_COLOR);
                btnPrev.setForeground(Color.WHITE);
                btnPrev.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0");
                btnPrev.addActionListener(e -> {
                        if (currentPage > 1) {
                                currentPage--;
                                refreshTablePage();
                        }
                });

                btnNext = new JButton("Siguiente");
                btnNext.setIcon(createIcon(FontAwesomeSolid.CHEVRON_RIGHT, Color.WHITE, 12));
                btnNext.setBackground(MD_BLUE_COLOR);
                btnNext.setForeground(Color.WHITE);
                btnNext.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0");
                btnNext.setHorizontalTextPosition(JButton.LEFT);
                btnNext.addActionListener(e -> {
                        int maxPage = (int) Math.ceil((double) allData.size() / itemsPerPage);
                        if (currentPage < maxPage) {
                                currentPage++;
                                refreshTablePage();
                        }
                });

                lblPageInfo = new JLabel("Página 1 de 1");
                lblPageInfo.setFont(new Font("sansserif", Font.BOLD, 13));

                paginationPanel.add(btnPrev, "w 100!");
                paginationPanel.add(lblPageInfo);
                paginationPanel.add(btnNext, "w 100!");

                panel.add(lblTotalRegistros);
                panel.add(paginationPanel); // Center
                panel.add(lblTotalStock);

                return panel;
        }

        private JComboBox<ComboItem> createComboBox(String title) {
                JComboBox<ComboItem> combo = new JComboBox<>();
                combo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccionar " + title);
                return combo;
        }

        private void initData() {
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                                cargarBodegas();
                                cargarMarcas();
                                cargarCategorias();
                                return null;
                        }
                };
                worker.execute();
        }

        // ================= LOGICA DE DATOS (Mantenida igual) =================

        private void cargarBodegas() {
                cbxBodega.removeAllItems();
                cbxBodega.addItem(new ComboItem(0, "Todas las Bodegas"));
                String sql = "SELECT id_bodega, nombre FROM bodegas WHERE activa = 1 ORDER BY nombre";
                cargarComboGeneric(cbxBodega, sql, "id_bodega", "nombre");
        }

        private void cargarMarcas() {
                cbxMarca.removeAllItems();
                cbxMarca.addItem(new ComboItem(0, "Todas las Marcas"));
                String sql = "SELECT id_marca, nombre FROM marcas WHERE activo = 1 ORDER BY nombre";
                cargarComboGeneric(cbxMarca, sql, "id_marca", "nombre");
        }

        private void cargarCategorias() {
                cbxCategoria.removeAllItems();
                cbxCategoria.addItem(new ComboItem(0, "Todas las Categorías"));
                String sql = "SELECT id_categoria, nombre FROM categorias WHERE activo = 1 ORDER BY nombre";
                try {
                        cargarComboGeneric(cbxCategoria, sql, "id_categoria", "nombre");
                } catch (Exception e) {
                        // Ignorar si no existe tabla categorias
                }
        }

        private void cargarComboGeneric(JComboBox<ComboItem> combo, String sql, String colId, String colNombre) {
                try (Connection con = conexion.getInstance().createConnection();
                                PreparedStatement pst = con.prepareStatement(sql);
                                ResultSet rs = pst.executeQuery()) {
                        while (rs.next()) {
                                combo.addItem(new ComboItem(rs.getInt(colId), rs.getString(colNombre)));
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private void buscar() {
                setLoading(true);

                int idBodega = getSelectedId(cbxBodega);
                int idMarca = getSelectedId(cbxMarca);
                int idCategoria = getSelectedId(cbxCategoria);
                String texto = txtBuscar.getText().trim();
                String color = txtColor.getText().trim();
                String talla = txtTalla.getText().trim();
                String ubicacion = txtUbicacion.getText().trim();
                boolean sinUbicacion = chkSinUbicacion.isSelected();

                SwingWorker<List<InventarioDetalleItem>, Void> worker = new SwingWorker<>() {
                        @Override
                        protected List<InventarioDetalleItem> doInBackground() throws Exception {
                                return inventarioDAO.buscarInventarioDetallado(
                                                idBodega, texto, idMarca, idCategoria, color, talla, ubicacion,
                                                sinUbicacion);
                        }

                        @Override
                        protected void done() {
                                try {
                                        List<InventarioDetalleItem> result = get();
                                        actualizarTabla(result);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                        Notifications.getInstance().show(Notifications.Type.ERROR,
                                                        Notifications.Location.TOP_RIGHT, "Error al buscar inventario");
                                } finally {
                                        setLoading(false);
                                }
                        }
                };
                worker.execute();
        }

        private List<InventarioDetalleItem> rawSearchResult = new ArrayList<>();

        private void actualizarTabla(List<InventarioDetalleItem> lista) {
                this.rawSearchResult = lista;
                applyMemoryFilters();
        }

        private void applyMemoryFilters() {
                List<InventarioDetalleItem> filteredList = new ArrayList<>();
                boolean showZero = chkMostrarCero.isSelected();
                boolean showNegative = chkMostrarNegativo.isSelected();
                String tipoUnidad = (String) cbxTipoUnidad.getSelectedItem();

                for (InventarioDetalleItem item : rawSearchResult) {
                        boolean isZero = (item.getStockPares() == 0 && item.getStockCajas() == 0);
                        boolean isNegative = (item.getStockPares() < 0 || item.getStockCajas() < 0);

                        if (isZero && !showZero)
                                continue;
                        if (isNegative && !showNegative)
                                continue;

                        // Unit Type Filter
                        if ("Solo Pares".equals(tipoUnidad)) {
                                if (item.getStockPares() <= 0 && !isNegative)
                                        continue;
                        } else if ("Solo Cajas".equals(tipoUnidad)) {
                                if (item.getStockCajas() <= 0 && !isNegative)
                                        continue;
                        }

                        filteredList.add(item);
                }

                this.listaActual = filteredList;
                this.allData = new ArrayList<>(filteredList);

                // Agrupar si esta el check activo
                if (chkAgruparVariantes.isSelected()) {
                        this.groupedData = groupData(this.allData);
                }

                this.currentPage = 1;
                setupTableColumns(chkAgruparVariantes.isSelected());
                refreshTablePage();
        }

        private void setupTableColumns(boolean grouped) {
                DefaultTableModel model;
                if (grouped) {
                        String[] columnNames = { "Bodega", "Código", "Producto", "Marca", "T. Pares", "T. Cajas",
                                        "Tallas Disponibles" };
                        model = new DefaultTableModel(columnNames, 0) {
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                        return false;
                                }
                        };
                        tablaResultados.setModel(model);

                        tablaResultados.setRowHeight(90);

                        // Custom Renderers
                        tablaResultados.getColumnModel().getColumn(2).setCellRenderer(new ProductoCardRenderer());
                        tablaResultados.getColumnModel().getColumn(6).setCellRenderer(new TallasCellRenderer());

                        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

                        // Sizing
                        tablaResultados.getColumnModel().getColumn(0).setPreferredWidth(100); // Bodega
                        tablaResultados.getColumnModel().getColumn(1).setPreferredWidth(100); // Codigo
                        tablaResultados.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
                        tablaResultados.getColumnModel().getColumn(2).setPreferredWidth(280); // Producto
                        tablaResultados.getColumnModel().getColumn(3).setPreferredWidth(100); // Marca
                        tablaResultados.getColumnModel().getColumn(4).setPreferredWidth(70); // T.Pares
                        tablaResultados.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
                        tablaResultados.getColumnModel().getColumn(5).setPreferredWidth(70); // T.Cajas
                        tablaResultados.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
                        tablaResultados.getColumnModel().getColumn(6).setPreferredWidth(350); // Tallas

                } else {
                        // Classic View
                        String[] columnNames = { "Bodega", "ID", "EAN", "Producto", "Marca", "Stock Pares",
                                        "Stock Cajas", "Ubicación" };
                        model = new DefaultTableModel(columnNames, 0) {
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                        return false;
                                }
                        };
                        tablaResultados.setModel(model);
                        tablaResultados.setRowHeight(90);

                        // Custom Renderers
                        tablaResultados.getColumnModel().getColumn(3).setCellRenderer(new ProductoCardRenderer());
                        tablaResultados.getColumnModel().getColumn(5).setCellRenderer(new StockRenderer());
                        tablaResultados.getColumnModel().getColumn(6).setCellRenderer(new StockRenderer());
                        tablaResultados.getColumnModel().getColumn(7).setCellRenderer(new LocationRenderer());

                        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

                        tablaResultados.getColumnModel().getColumn(1).setPreferredWidth(60); // ID
                        tablaResultados.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
                        tablaResultados.getColumnModel().getColumn(2).setPreferredWidth(100); // EAN
                        tablaResultados.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
                        tablaResultados.getColumnModel().getColumn(3).setPreferredWidth(300); // Producto
                        tablaResultados.getColumnModel().getColumn(5).setPreferredWidth(80); // Stock
                        tablaResultados.getColumnModel().getColumn(6).setPreferredWidth(80); // Stock
                        tablaResultados.getColumnModel().getColumn(7).setPreferredWidth(120); // Ubicacion
                }

                // Add Context Menu
                JPopupMenu popup = new JPopupMenu();
                JMenuItem itemKardex = new JMenuItem("📜 Ver Historial de Movimientos (Kardex)");
                itemKardex.addActionListener(e -> abrirKardexSeleccionado());
                popup.add(itemKardex);
                tablaResultados.setComponentPopupMenu(popup);
        }

        private void abrirKardexSeleccionado() {
                int row = tablaResultados.getSelectedRow();
                if (row == -1)
                        return;

                // Convert view index to model index in case of sorting (though we might not
                // have sorter enabled yet)
                // But relying on raw index vs list index
                // Safest is to get value from Lists based on pagination logic, BUT
                // The table model holds the objects directly in some columns or we can map
                // back.
                // Let's use the list data since we know the page.

                // BETTER: Get the object from the rendering logic mapping
                // Actually, we populate the model with objects.
                // Row index in model corresponds to index in current page list.

                try {
                        boolean isGrouped = chkAgruparVariantes.isSelected();
                        // Logic depends on pagination logic in refreshTablePage

                        // Simplified: Get data from current page list logic
                        // Or better, let's look at what we put in the table.
                        // Col 2 in grouped is ProductoAgrupado object? No, Col 2 is ProductoAgrupado in
                        // grouped view logic?
                        // refreshTablePage: model.addRow(new Object[] { ..., grupo, ... })

                        // Let's use the Lists we populate
                        // Since row 0 in table is row 0 in 'pageItems' list.

                        if (isGrouped) {
                                // Get visible page items
                                int start = (currentPage - 1) * itemsPerPage;
                                int end = Math.min(start + itemsPerPage, groupedData.size());
                                if (start < groupedData.size()) {
                                        List<raven.application.form.productos.dto.ProductoAgrupado> pageItems = groupedData
                                                        .subList(start, end);
                                        if (row < pageItems.size()) {
                                                raven.application.form.productos.dto.ProductoAgrupado grupo = pageItems
                                                                .get(row);
                                                // For grouped, we have multiple variants (sizes).
                                                // Ideally show a dialog to pick size or show all.
                                                // For now, let's pick the FIRST variant to show SOMETHING or show a
                                                // chooser.
                                                // Let's show a simple option dialog if multiple sizes
                                                if (grupo.getTallas().isEmpty())
                                                        return;

                                                if (grupo.getTallas().size() == 1) {
                                                        int idVar = grupo.getTallas().get(0).getIdVariante();
                                                        String title = grupo.getNombreProducto() + " - "
                                                                        + grupo.getNombreColor() + " - "
                                                                        + grupo.getTallas().get(0).getNombreTalla();
                                                        new KardexDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                                                        idVar, title).setVisible(true);
                                                } else {
                                                        // Ask user which size
                                                        // Create array of sizes
                                                        Object[] sizes = grupo.getTallas().stream()
                                                                        .map(t -> t.getNombreTalla() + " (ID: "
                                                                                        + t.getIdVariante() + ")")
                                                                        .toArray();

                                                        String selected = (String) JOptionPane.showInputDialog(this,
                                                                        "Seleccione la Talla para ver el Kardex:",
                                                                        "Selección de Variante",
                                                                        JOptionPane.QUESTION_MESSAGE,
                                                                        null,
                                                                        sizes,
                                                                        sizes[0]);

                                                        if (selected != null) {
                                                                // Find ID
                                                                for (raven.application.form.productos.dto.TallaInfo t : grupo
                                                                                .getTallas()) {
                                                                        if (selected.startsWith(
                                                                                        t.getNombreTalla() + " (")) {
                                                                                String title = grupo.getNombreProducto()
                                                                                                + " - "
                                                                                                + grupo.getNombreColor()
                                                                                                + " - "
                                                                                                + t.getNombreTalla();
                                                                                new KardexDialog((Frame) SwingUtilities
                                                                                                .getWindowAncestor(
                                                                                                                this),
                                                                                                t.getIdVariante(),
                                                                                                title)
                                                                                                .setVisible(true);
                                                                                break;
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } else {
                                int start = (currentPage - 1) * itemsPerPage;
                                int end = Math.min(start + itemsPerPage, allData.size());
                                if (start < allData.size()) {
                                        List<InventarioDetalleItem> pageItems = allData.subList(start, end);
                                        if (row < pageItems.size()) {
                                                InventarioDetalleItem item = pageItems.get(row);
                                                String title = item.getNombreProducto() + " - " + item.getNombreColor()
                                                                + " - " + item.getNombreTalla();
                                                new KardexDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                                                item.getIdVariante(), title).setVisible(true);
                                        }
                                }
                        }
                } catch (Exception ex) {
                        ex.printStackTrace();
                        Notifications.getInstance().show(Notifications.Type.ERROR, "Error abriendo Kardex");
                }
        }

        private List<raven.application.form.productos.dto.ProductoAgrupado> groupData(
                        List<InventarioDetalleItem> flatList) {
                java.util.Map<String, raven.application.form.productos.dto.ProductoAgrupado> map = new java.util.LinkedHashMap<>();

                for (InventarioDetalleItem item : flatList) {
                        String key = item.getIdBodega() + "-" + item.getIdProducto() + "-" + item.getNombreColor();

                        raven.application.form.productos.dto.ProductoAgrupado grupo = map.get(key);
                        if (grupo == null) {
                                grupo = new raven.application.form.productos.dto.ProductoAgrupado();
                                grupo.setIdBodega(item.getIdBodega());
                                grupo.setNombreBodega(item.getNombreBodega());
                                grupo.setIdProducto(item.getIdProducto());
                                grupo.setCodigoModelo(item.getCodigoModelo());
                                grupo.setNombreProducto(item.getNombreProducto());
                                grupo.setNombreMarca(item.getNombreMarca());
                                grupo.setNombreColor(item.getNombreColor());
                                grupo.setCategory(item.getCategoria());
                                grupo.setUbicacion(item.getUbicacion());
                                map.put(key, grupo);
                        }
                        grupo.addTalla(new raven.application.form.productos.dto.TallaInfo(
                                        item.getNombreTalla(), item.getStockPares(), item.getStockCajas(),
                                        item.getIdVariante(), item.getEan(), item.getUbicacion()));
                }
                return new ArrayList<>(map.values());
        }

        private void refreshTablePage() {
                DefaultTableModel model = (DefaultTableModel) tablaResultados.getModel();
                model.setRowCount(0);

                boolean isGrouped = chkAgruparVariantes.isSelected();

                int totalPares = 0;
                int totalCajas = 0;
                int totalCount = 0;

                if (isGrouped) {
                        for (raven.application.form.productos.dto.ProductoAgrupado group : groupedData) {
                                totalPares += group.getTotalPares();
                                totalCajas += group.getTotalCajas();
                        }
                        totalCount = groupedData.size();

                        int start = (currentPage - 1) * itemsPerPage;
                        int end = Math.min(start + itemsPerPage, groupedData.size());

                        if (start >= groupedData.size() && !groupedData.isEmpty()) {
                                start = 0;
                                currentPage = 1;
                                end = Math.min(itemsPerPage, groupedData.size());
                        }

                        List<raven.application.form.productos.dto.ProductoAgrupado> pageItems = groupedData
                                        .subList(start, end);
                        for (raven.application.form.productos.dto.ProductoAgrupado item : pageItems) {
                                model.addRow(new Object[] {
                                                item.getNombreBodega(), item.getCodigoModelo(), item,
                                                item.getNombreMarca(),
                                                item.getTotalPares(), item.getTotalCajas(), item.getTallas()
                                });
                        }
                        // Trigger Image Loading
                        raven.utils.ProductImageOptimizer.loadImagesSequentialGrouped(tablaResultados, pageItems);
                } else {
                        for (InventarioDetalleItem item : allData) {
                                totalPares += item.getStockPares();
                                totalCajas += item.getStockCajas();
                        }
                        totalCount = allData.size();

                        int start = (currentPage - 1) * itemsPerPage;
                        int end = Math.min(start + itemsPerPage, allData.size());

                        if (start >= allData.size() && !allData.isEmpty()) {
                                start = 0;
                                currentPage = 1;
                                end = Math.min(itemsPerPage, allData.size());
                        }

                        List<InventarioDetalleItem> pageItems = allData.subList(start, end);
                        for (InventarioDetalleItem item : pageItems) {
                                model.addRow(new Object[] {
                                                item.getNombreBodega(), item.getIdVariante(), item.getEan(), item,
                                                item.getNombreMarca(), item.getStockPares(), item.getStockCajas(),
                                                item.getUbicacion()
                                });
                        }
                        // Trigger Image Loading
                        raven.utils.ProductImageOptimizer.loadImagesSequentialInventory(tablaResultados, pageItems);
                }

                lblTotalRegistros.setText("Total Registros: " + totalCount);
                lblTotalStock.setText("Stock Total: " + totalPares + " Pares / " + totalCajas + " Cajas");

                int maxPage = (int) Math.ceil((double) totalCount / itemsPerPage);
                if (maxPage == 0)
                        maxPage = 1;

                lblPageInfo.setText("Página " + currentPage + " de " + maxPage);
                btnPrev.setEnabled(currentPage > 1);
                btnNext.setEnabled(currentPage < maxPage);
        }

        private void limpiarFiltros() {
                if (cbxBodega.getItemCount() > 0)
                        cbxBodega.setSelectedIndex(0);
                if (cbxMarca.getItemCount() > 0)
                        cbxMarca.setSelectedIndex(0);
                if (cbxCategoria.getItemCount() > 0)
                        cbxCategoria.setSelectedIndex(0);
                txtBuscar.setText("");
                txtColor.setText("");
                txtTalla.setText("");
                txtUbicacion.setText("");
                txtUbicacion.setEnabled(true);
                chkSinUbicacion.setSelected(false);
                DefaultTableModel model = (DefaultTableModel) tablaResultados.getModel();
                model.setRowCount(0);
                listaActual.clear();
                lblTotalRegistros.setText("Total Registros: 0");
                lblTotalStock.setText("Stock Total: 0 Pares / 0 Cajas");
        }

        private void setLoading(boolean loading) {
                progressBar.setVisible(loading);
                btnBuscar.setEnabled(!loading);
                btnExportar.setEnabled(!loading);
                btnLimpiar.setEnabled(!loading);
                txtBuscar.setEnabled(!loading);
        }

        private int getSelectedId(JComboBox<ComboItem> combo) {
                Object item = combo.getSelectedItem();
                return (item instanceof ComboItem) ? ((ComboItem) item).getId() : 0;
        }

        private void exportarExcel() {
                if (listaActual == null || listaActual.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Exportar",
                                        JOptionPane.WARNING_MESSAGE);
                        return;
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Guardar Inventario Detallado");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx"));
                fileChooser.setSelectedFile(new File("Inventario_" + System.currentTimeMillis() + ".xlsx"));

                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                                file = new File(file.getParentFile(), file.getName() + ".xlsx");
                        }

                        final File finalFile = file;
                        setLoading(true);

                        SwingWorker<Void, Void> worker = new SwingWorker<>() {
                                @Override
                                protected Void doInBackground() throws Exception {
                                        generarExcel(finalFile);
                                        return null;
                                }

                                @Override
                                protected void done() {
                                        setLoading(false);
                                        try {
                                                get();
                                                int op = JOptionPane.showConfirmDialog(
                                                                ConsultaInventarioDetalladoForm.this,
                                                                "Archivo exportado exitosamente.\n¿Desea abrirlo?",
                                                                "Éxito", JOptionPane.YES_NO_OPTION);
                                                if (op == JOptionPane.YES_OPTION) {
                                                        Desktop.getDesktop().open(finalFile);
                                                }
                                        } catch (Exception e) {
                                                e.printStackTrace();
                                                Notifications.getInstance().show(Notifications.Type.ERROR,
                                                                Notifications.Location.TOP_RIGHT,
                                                                "Error exportando: " + e.getMessage());
                                        }
                                }
                        };
                        worker.execute();
                }
        }

        private void generarExcel(File file) throws Exception {
                try (Workbook workbook = new XSSFWorkbook()) {
                        Sheet sheet = workbook.createSheet("Inventario");

                        // Estilos
                        CellStyle headerStyle = workbook.createCellStyle();
                        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                        font.setBold(true);
                        font.setColor(IndexedColors.WHITE.getIndex());
                        headerStyle.setFont(font);
                        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
                        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        headerStyle.setAlignment(HorizontalAlignment.CENTER);

                        // Headers
                        String[] headers = { "Bodega", "Código", "Producto", "Marca", "Categoría", "Color", "Talla",
                                        "Pares", "Cajas", "Ubicación" };
                        Row row = sheet.createRow(0);
                        for (int i = 0; i < headers.length; i++) {
                                Cell cell = row.createCell(i);
                                cell.setCellValue(headers[i]);
                                cell.setCellStyle(headerStyle);
                        }

                        // Data
                        int r = 1;
                        for (InventarioDetalleItem item : listaActual) {
                                row = sheet.createRow(r++);
                                row.createCell(0).setCellValue(item.getNombreBodega());
                                row.createCell(1).setCellValue(item.getCodigoModelo());
                                row.createCell(2).setCellValue(item.getNombreProducto());
                                row.createCell(3).setCellValue(item.getNombreMarca());
                                row.createCell(4).setCellValue(item.getCategoria());
                                row.createCell(5).setCellValue(item.getNombreColor());
                                row.createCell(6).setCellValue(item.getNombreTalla());
                                row.createCell(7).setCellValue(item.getStockPares());
                                row.createCell(8).setCellValue(item.getStockCajas());
                                row.createCell(9).setCellValue(item.getUbicacion());
                        }

                        // Autosize
                        for (int i = 0; i < headers.length; i++) {
                                sheet.autoSizeColumn(i);
                        }

                        try (FileOutputStream fos = new FileOutputStream(file)) {
                                workbook.write(fos);
                        }
                }
        }

        // Clase auxiliar para combos
        private static class ComboItem {
                private int id;
                private String texto;

                public ComboItem(int id, String texto) {
                        this.id = id;
                        this.texto = texto;
                }

                public int getId() {
                        return id;
                }

                @Override
                public String toString() {
                        return texto;
                }
        }
}
