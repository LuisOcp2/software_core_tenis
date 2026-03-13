package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import raven.controlador.productos.ModelProduct;

/**
 * Panel lateral (sidebar) para la gestión de productos con métricas,
 * filtros avanzados y lista de productos.
 * 
 * @author Kiro AI Assistant
 */
public class ProductSidebarPanel extends JPanel {

    // Colores del tema
    private static final Color SIDEBAR_BG = new Color(52, 69, 84); // #344554
    private static final Color SIDEBAR_DARK = new Color(44, 62, 80); // #2c3e50
    private static final Color ACCENT_BLUE = new Color(66, 165, 245); // #42A5F5
    private static final Color TEXT_LIGHT = new Color(236, 240, 241); // #ecf0f1
    private static final Color TEXT_MUTED = new Color(149, 165, 166); // #95a5a6

    // Componentes principales
    private JPanel metricsPanel;
    private JPanel filtersPanel;
    private JPanel productsListPanel;
    private JScrollPane productsScrollPane;

    // Métricas
    private MetricCard sinPrecioCard;
    private MetricCard stockCriticoCard;
    private MetricCard valorInventarioCard;
    private MetricCard totalParesCard;

    // Filtros
    private JTextField searchField;
    private JComboBox<String> estadoCombo;
    private JComboBox<String> marcaCombo;
    private JComboBox<String> categoriaCombo;
    private JButton clearFiltersButton;

    // Lista de productos
    private DefaultListModel<ProductListItem> productListModel;
    private JList<ProductListItem> productList;

    // Listeners
    private ProductSelectionListener selectionListener;

    public ProductSidebarPanel() {
        initComponents();
        setupStyles();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(350, 600)); // Aumentado de 300 a 350px
        setBackground(SIDEBAR_BG);

        // Panel principal con scroll
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(SIDEBAR_BG);

        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel);

        // Métricas
        metricsPanel = createMetricsPanel();
        mainPanel.add(metricsPanel);

        // Filtros
        filtersPanel = createFiltersPanel();
        mainPanel.add(filtersPanel);

        // Lista de productos
        productsListPanel = createProductsListPanel();
        mainPanel.add(productsListPanel);

        // Scroll para todo el sidebar
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SIDEBAR_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("GESTIÓN DE PRODUCTOS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_LIGHT);

        JLabel subtitleLabel = new JLabel("Bodega Bello • 202 productos");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(TEXT_MUTED);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(SIDEBAR_DARK);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitleLabel);

        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Crear tarjetas de métricas
        sinPrecioCard = new MetricCard("15", "Sin Precio", new Color(231, 76, 60));
        stockCriticoCard = new MetricCard("23", "Stock Crítico", new Color(230, 126, 34));
        valorInventarioCard = new MetricCard("$45.2M", "Valor Inventario", new Color(46, 204, 113));
        totalParesCard = new MetricCard("2,049", "Total Pares", new Color(52, 152, 219));

        panel.add(sinPrecioCard);
        panel.add(stockCriticoCard);
        panel.add(valorInventarioCard);
        panel.add(totalParesCard);

        return panel;
    }

    private JPanel createFiltersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));

        // Título de filtros
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(SIDEBAR_BG);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel titleLabel = new JLabel("FILTROS AVANZADOS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(TEXT_MUTED);

        clearFiltersButton = new JButton("Limpiar");
        clearFiltersButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        clearFiltersButton.setForeground(ACCENT_BLUE);
        clearFiltersButton.setBorderPainted(false);
        clearFiltersButton.setContentAreaFilled(false);
        clearFiltersButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(clearFiltersButton, BorderLayout.EAST);

        panel.add(titlePanel);
        panel.add(Box.createVerticalStrut(10));

        // Campo de búsqueda
        searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar producto...");
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panel.add(searchField);
        panel.add(Box.createVerticalStrut(10));

        // Filtro de estado
        JLabel estadoLabel = new JLabel("Estado");
        estadoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        estadoLabel.setForeground(TEXT_LIGHT);
        estadoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        estadoCombo = new JComboBox<>(new String[] { "Todos", "Con Stock", "Sin Stock", "Stock Crítico" });
        estadoCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        panel.add(estadoLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(estadoCombo);
        panel.add(Box.createVerticalStrut(10));

        // Filtro de marca
        JLabel marcaLabel = new JLabel("Marca");
        marcaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        marcaLabel.setForeground(TEXT_LIGHT);
        marcaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        marcaCombo = new JComboBox<>(new String[] { "Todas" });
        marcaCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        panel.add(marcaLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(marcaCombo);
        panel.add(Box.createVerticalStrut(10));

        // Filtro de categoría
        JLabel categoriaLabel = new JLabel("Categoría");
        categoriaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        categoriaLabel.setForeground(TEXT_LIGHT);
        categoriaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        categoriaCombo = new JComboBox<>(new String[] { "Todas" });
        categoriaCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        panel.add(categoriaLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(categoriaCombo);

        return panel;
    }

    private JPanel createProductsListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));

        // Título
        JLabel titleLabel = new JLabel("PRODUCTOS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(TEXT_MUTED);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        panel.add(titleLabel, BorderLayout.NORTH);

        // Lista de productos
        productListModel = new DefaultListModel<>();
        productList = new JList<>(productListModel);
        productList.setCellRenderer(new ProductListCellRenderer());
        productList.setBackground(SIDEBAR_BG);
        productList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        productList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectionListener != null) {
                ProductListItem selected = productList.getSelectedValue();
                if (selected != null) {
                    selectionListener.onProductSelected(selected.product);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(productList);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(270, 300));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupStyles() {
        // Estilos para campos de texto y combos
        searchField.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;background:#3d4e5f;foreground:#ecf0f1;borderWidth:0");

        String comboStyle = "arc:8;background:#3d4e5f;foreground:#ecf0f1;borderWidth:0";
        estadoCombo.putClientProperty(FlatClientProperties.STYLE, comboStyle);
        marcaCombo.putClientProperty(FlatClientProperties.STYLE, comboStyle);
        categoriaCombo.putClientProperty(FlatClientProperties.STYLE, comboStyle);
    }

    // Métodos públicos para actualizar datos

    public void updateMetrics(int sinPrecio, int stockCritico, String valorInventario, int totalPares) {
        sinPrecioCard.setValue(String.valueOf(sinPrecio));
        stockCriticoCard.setValue(String.valueOf(stockCritico));
        valorInventarioCard.setValue(valorInventario);
        totalParesCard.setValue(String.format("%,d", totalPares));
    }

    public void setProducts(List<ModelProduct> products) {
        productListModel.clear();
        for (ModelProduct product : products) {
            productListModel.addElement(new ProductListItem(product));
        }
    }

    public void setProductSelectionListener(ProductSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setMarcas(List<String> marcas) {
        marcaCombo.removeAllItems();
        marcaCombo.addItem("Todas");
        for (String marca : marcas) {
            marcaCombo.addItem(marca);
        }
    }

    public void setCategorias(List<String> categorias) {
        categoriaCombo.removeAllItems();
        categoriaCombo.addItem("Todas");
        for (String categoria : categorias) {
            categoriaCombo.addItem(categoria);
        }
    }

    // Getters para acceso a componentes de filtro

    public JTextField getSearchField() {
        return searchField;
    }

    public JComboBox<String> getEstadoCombo() {
        return estadoCombo;
    }

    public JComboBox<String> getMarcaCombo() {
        return marcaCombo;
    }

    public JComboBox<String> getCategoriaCombo() {
        return categoriaCombo;
    }

    public JButton getClearFiltersButton() {
        return clearFiltersButton;
    }

    // Clases internas

    public interface ProductSelectionListener {
        void onProductSelected(ModelProduct product);
    }

    private static class ProductListItem {
        ModelProduct product;

        ProductListItem(ModelProduct product) {
            this.product = product;
        }

        @Override
        public String toString() {
            return product.getName();
        }
    }

    private class ProductListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));

            if (value instanceof ProductListItem) {
                ProductListItem item = (ProductListItem) value;
                ModelProduct product = item.product;

                // Nombre del producto
                JLabel nameLabel = new JLabel(product.getName());
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                nameLabel.setForeground(TEXT_LIGHT);

                // Código y stock
                String info = String.format("%s • %d pares",
                        product.getModelCode(),
                        product.getPairsStock());
                JLabel infoLabel = new JLabel(info);
                infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                infoLabel.setForeground(TEXT_MUTED);

                JPanel textPanel = new JPanel();
                textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                textPanel.setOpaque(false);
                textPanel.add(nameLabel);
                textPanel.add(infoLabel);

                // Badge de precio
                JLabel priceLabel = new JLabel(String.format("$%,.0f", product.getSalePrice()));
                priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
                priceLabel.setForeground(Color.WHITE);
                priceLabel.setOpaque(true);
                priceLabel.setBackground(ACCENT_BLUE);
                priceLabel.setBorder(new EmptyBorder(3, 8, 3, 8));

                panel.add(textPanel, BorderLayout.CENTER);
                panel.add(priceLabel, BorderLayout.EAST);
            }

            if (isSelected) {
                panel.setBackground(new Color(61, 78, 95));
            } else {
                panel.setBackground(SIDEBAR_BG);
            }

            return panel;
        }
    }
}
