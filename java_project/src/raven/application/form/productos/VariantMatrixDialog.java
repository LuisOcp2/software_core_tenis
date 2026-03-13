package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import raven.controlador.productos.ModelProductVariant;
import raven.clases.productos.ServiceProductVariant;

/**
 * Diálogo mejorado que muestra la vista matriz de variantes con diseño moderno.
 * Incluye breadcrumb, toolbar y toggle entre vistas.
 * 
 * @author Kiro AI Assistant
 */
public class VariantMatrixDialog extends JDialog {
    
    // Colores del tema oscuro - Paleta corporativa del sistema
    private static final Color BG_MODAL = new Color(52, 69, 84);         // #344554 (sidebar)
    private static final Color BG_PANEL = new Color(55, 65, 81);         // #374151 (fondos)
    private static final Color BG_HEADER = new Color(44, 62, 80);        // #2c3e50 (header oscuro)
    private static final Color TOOLBAR_BG = new Color(55, 65, 81);       // #374151
    private static final Color ACCENT_BLUE = new Color(66, 165, 245);    // #42A5F5
    private static final Color ACCENT_ORANGE = new Color(251, 146, 60);  // #FB923C
    private static final Color TEXT_PRIMARY = new Color(236, 240, 241);  // #ecf0f1 (texto claro)
    private static final Color TEXT_SECONDARY = new Color(160, 174, 192); // #A0AEC0
    private static final Color BORDER_COLOR = new Color(74, 85, 104);    // #4A5568
    
    private VariantMatrixPanel matrixPanel;
    private JPanel tablePanel;
    private JToggleButton matrixViewButton;
    private JToggleButton tableViewButton;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JLabel breadcrumbLabel;
    private ServiceProductVariant variantService;

    // Referencias a las pestañas simuladas
    private JButton infoTabButton;
    private JButton variantesTabButton;
    private JButton inventarioTabButton;
    
    private final List<ModelProductVariant> variants;
    private final String productName;
    
    public VariantMatrixDialog(Frame parent, String productName, List<ModelProductVariant> variants, ServiceProductVariant variantService) {
        super(parent, "Gestión de Variantes - " + productName, true);
        this.variants = variants;
        this.productName = productName;
        this.variantService = variantService;
        
        initComponents();
        setupStyles();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setSize(1200, 750);
        setBackground(BG_MODAL);
        
        // Panel principal con fondo oscuro
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_MODAL);
        
        // Breadcrumb y toolbar
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Panel de contenido con CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_MODAL);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Vista Matriz (con soporte de imágenes por variante)
        matrixPanel = new VariantMatrixPanel(variantService);
        matrixPanel.setVariants(variants);
        
        // Vista Tabla
        tablePanel = createTableView();
        
        contentPanel.add(matrixPanel, "MATRIX");
        contentPanel.add(tablePanel, "TABLE");
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_HEADER);
        
        // Breadcrumb
        JPanel breadcrumbPanel = createBreadcrumbPanel();
        panel.add(breadcrumbPanel, BorderLayout.NORTH);
        
        // Toolbar
        JPanel toolbarPanel = createToolbarPanel();
        panel.add(toolbarPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBreadcrumbPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(BG_HEADER);
        panel.setBorder(new EmptyBorder(15, 20, 5, 20));
        
        // Crear breadcrumb con iconos FontAwesome
        JLabel boxIcon = new JLabel(FontIcon.of(FontAwesomeSolid.BOX, 14, TEXT_SECONDARY));
        JLabel separator1 = new JLabel(" > ");
        separator1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        separator1.setForeground(TEXT_SECONDARY);
        
        JLabel productosLabel = new JLabel("Productos");
        productosLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productosLabel.setForeground(TEXT_SECONDARY);
        
        JLabel separator2 = new JLabel(" > ");
        separator2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        separator2.setForeground(TEXT_SECONDARY);
        
        JLabel categoryLabel = new JLabel("Calzado Deportivo");
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoryLabel.setForeground(TEXT_SECONDARY);
        
        JLabel separator3 = new JLabel(" > ");
        separator3.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        separator3.setForeground(TEXT_SECONDARY);
        
        breadcrumbLabel = new JLabel(productName);
        breadcrumbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        breadcrumbLabel.setForeground(TEXT_SECONDARY);
        
        panel.add(boxIcon);
        panel.add(productosLabel);
        panel.add(separator1);
        panel.add(categoryLabel);
        panel.add(separator2);
        panel.add(breadcrumbLabel);
        
        return panel;
    }
    
    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(15, 20, 15, 20)
        ));
        
        // Panel izquierdo con título y tabs
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(BG_PANEL);
        
        // Título
        JLabel titleLabel = new JLabel("Gestión de Variantes (Talla × Color)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        // Tabs simulados
        JPanel tabsPanel = createTabsPanel();
        
        leftPanel.add(titleLabel, BorderLayout.NORTH);
        leftPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        leftPanel.add(tabsPanel, BorderLayout.SOUTH);
        
        // Panel derecho con toggle de vistas
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(BG_PANEL);
        
        ButtonGroup viewGroup = new ButtonGroup();
        
        matrixViewButton = new JToggleButton("Vista Matriz");
        matrixViewButton.setIcon(FontIcon.of(FontAwesomeSolid.TH, 16, ACCENT_ORANGE));
        matrixViewButton.setSelected(true);
        matrixViewButton.addActionListener(e -> showMatrixView());
        
        tableViewButton = new JToggleButton("Vista Tabla");
        tableViewButton.setIcon(FontIcon.of(FontAwesomeSolid.TABLE, 16, ACCENT_ORANGE));
        tableViewButton.addActionListener(e -> showTableView());
        
        viewGroup.add(matrixViewButton);
        viewGroup.add(tableViewButton);
        
        rightPanel.add(matrixViewButton);
        rightPanel.add(tableViewButton);
        
        panel.add(leftPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createTabsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(BG_PANEL);
        
        // Tabs con iconos FontAwesome
        infoTabButton = createTab(FontAwesomeSolid.INFO_CIRCLE, "Información General", false);
        variantesTabButton = createTab(FontAwesomeSolid.PALETTE, "Variantes del Producto", true);
        inventarioTabButton = createTab(FontAwesomeSolid.WAREHOUSE, "Inventario por Bodega", false);

        // Acciones de las pestañas
        infoTabButton.addActionListener(e -> onInfoTabSelected());
        variantesTabButton.addActionListener(e -> onVariantesTabSelected());
        inventarioTabButton.addActionListener(e -> onInventarioTabSelected());
        
        panel.add(infoTabButton);
        panel.add(variantesTabButton);
        panel.add(inventarioTabButton);
        
        return panel;
    }
    
    private JButton createTab(FontAwesomeSolid icon, String text, boolean active) {
        JButton tab = new JButton(text);
        tab.setIcon(FontIcon.of(icon, 13, active ? Color.WHITE : TEXT_SECONDARY));
        tab.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tab.setBorderPainted(false);
        tab.setFocusPainted(false);
        tab.setContentAreaFilled(false);
        tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tab.setBorder(new EmptyBorder(8, 15, 8, 15));
        
        if (active) {
            setTabActive(tab, true);
        } else {
            setTabActive(tab, false);
        }
        
        return tab;
    }

    /**
     * Actualiza los estilos de una pestaña para estado activo/inactivo.
     */
    private void setTabActive(JButton tab, boolean active) {
        if (active) {
            tab.setForeground(Color.WHITE);
            tab.setBackground(ACCENT_ORANGE);
            tab.setOpaque(true);
            tab.setContentAreaFilled(true);
        } else {
            tab.setForeground(TEXT_SECONDARY);
            tab.setOpaque(false);
            tab.setContentAreaFilled(false);
        }
    }

    /**
     * Marca una de las pestañas como activa y el resto como inactivas.
     */
    private void selectTab(JButton activeTab) {
        if (infoTabButton != null) {
            setTabActive(infoTabButton, infoTabButton == activeTab);
        }
        if (variantesTabButton != null) {
            setTabActive(variantesTabButton, variantesTabButton == activeTab);
        }
        if (inventarioTabButton != null) {
            setTabActive(inventarioTabButton, inventarioTabButton == activeTab);
        }
    }

    private void onInfoTabSelected() {
        selectTab(infoTabButton);

        // Resumen rápido del producto usando las variantes cargadas
        if (variants == null || variants.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No hay variantes cargadas para este producto.",
                "Información General",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int totalVariantes = variants.size();
        int totalPares = variants.stream().mapToInt(ModelProductVariant::getStock).sum();

        java.util.Set<String> tallas = new java.util.TreeSet<>();
        java.util.Set<String> colores = new java.util.TreeSet<>();
        for (ModelProductVariant v : variants) {
            if (v.getSizeName() != null) tallas.add(v.getSizeName());
            if (v.getColorName() != null) colores.add(v.getColorName());
        }

        StringBuilder info = new StringBuilder();
        info.append("Producto: ").append(productName).append("\n\n");
        info.append("Variantes registradas: ").append(totalVariantes).append("\n");
        info.append("Total pares (todas las variantes): ").append(totalPares).append("\n\n");
        info.append("Tallas: ").append(String.join(", ", tallas)).append("\n");
        info.append("Colores: ").append(String.join(", ", colores)).append("\n");

        JOptionPane.showMessageDialog(
            this,
            info.toString(),
            "Información General del Producto",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void onVariantesTabSelected() {
        selectTab(variantesTabButton);
        // Esta pestaña corresponde a la vista actual (matriz/tabla)
        // No cambiamos el contenido, solo aseguramos que se vea alguna vista válida.
        cardLayout.show(contentPanel, "MATRIX");
        matrixViewButton.setSelected(true);
    }

    private void onInventarioTabSelected() {
        selectTab(inventarioTabButton);

        if (variants == null || variants.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No hay información de inventario porque no hay variantes cargadas.",
                "Inventario por Bodega",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Intentar obtener estadísticas consolidadas desde el servicio
        String mensaje;
        try {
            int productId = variants.get(0).getProductId();
            ServiceProductVariant.ProductVariantStats stats = variantService.getProductStats(productId);
            mensaje = String.format(
                "Inventario consolidado para el producto:\n\n" +
                "Variantes totales: %d\n" +
                "Pares en inventario (todas las bodegas): %d\n" +
                "Cajas en inventario (todas las bodegas): %d\n" +
                "Equivalente total en pares (pares + cajas*24): %d",
                stats.variantes,
                stats.totalPares,
                stats.totalCajas,
                stats.totalPairsEquivalent
            );
        } catch (Exception ex) {
            mensaje = "No se pudo obtener el inventario consolidado.\n\nDetalle técnico: " + ex.getMessage();
        }

        JOptionPane.showMessageDialog(
            this,
            mensaje,
            "Inventario por Bodega (Resumen)",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    private JPanel createTableView() {
        return new EnhancedTablePanel(variants, variantService);
    }
    
    private void setupStyles() {
        // Estilos para botones de toggle de vista (sin usar propiedades no soportadas)
        if (matrixViewButton != null) {
            matrixViewButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            matrixViewButton.setForeground(TEXT_PRIMARY);
        }
        if (tableViewButton != null) {
            tableViewButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            tableViewButton.setForeground(TEXT_PRIMARY);
        }
    }
    
    private void showMatrixView() {
        cardLayout.show(contentPanel, "MATRIX");
    }
    
    private void showTableView() {
        cardLayout.show(contentPanel, "TABLE");
    }
    
    /**
     * Obtiene el panel de matriz para configurar listeners
     */
    public VariantMatrixPanel getMatrixPanel() {
        return matrixPanel;
    }
    
    /**
     * Muestra un mensaje de error al usuario
     * @param message Mensaje de error a mostrar
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    /**
     * Muestra un mensaje de éxito al usuario
     * @param message Mensaje de éxito a mostrar
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Éxito",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Método estático para mostrar el diálogo
     */
    public static void showDialog(Frame parent, String productName, List<ModelProductVariant> variants, ServiceProductVariant variantService) {
        VariantMatrixDialog dialog = new VariantMatrixDialog(parent, productName, variants, variantService);
        dialog.setVisible(true);
    }
}

