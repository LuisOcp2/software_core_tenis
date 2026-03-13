package raven.application.form.productos;

import raven.application.form.productos.creates.Create;
import raven.application.form.productos.creates.CreateTallas;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

import java.awt.Font;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.JWindow;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.BorderOption;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.ProductPaginationAdapter;
import raven.clases.comun.GenericPaginationService;
import raven.modal.listener.ModalController;
import raven.clases.admin.UserSession;
import raven.controlador.admin.ModelUser;
import raven.controlador.productos.ModelProductVariant;
import raven.clases.inventario.ServiceInventarioBodega;
import raven.clases.productos.ServiceProductOptimizedCache;
import raven.clases.productos.ServiceProductVariant;
import raven.controlador.admin.SessionManager;

public class GestionProductosForm extends javax.swing.JPanel {
    // ====================================================================
    // PROPIEDADES Y SERVICIOS
    // ====================================================================
    private ServiceInventarioBodega serviceInventarioBodega = new ServiceInventarioBodega();
    private ServiceProduct service = new ServiceProduct();
    private ServiceProductOptimizedCache serviceOptimizedCache = new ServiceProductOptimizedCache();
    private final ProductPaginationAdapter productAdapter = new ProductPaginationAdapter();
    private final UserSession userSession = UserSession.getInstance();

    // Sidebar
    private ProductSidebarPanel sidebar;

    // Variables para control de usuario y ubicación
    private ModelUser currentUser;
    private String userLocation;
    private boolean isUserFromStore;
    private boolean isUserFromWarehouse;

    // ID del usuario actual (se establece al inicializar la sesión)
    private int currentUserId = -1; // Se tomará de SessionManager
    private TableRowSorter<DefaultTableModel> sorter;

    private static class CacheEntry {
        List<ModelProduct> list;
        long ts;
    }

    private final java.util.Map<String, CacheEntry> productsCache = new java.util.HashMap<>();

    private static class VariantSummary {
        String colores;
        String tallas;
    }

    private final java.util.Map<Integer, VariantSummary> variantSummaryCache = new java.util.HashMap<>();

    private static class StockTotals {
        int pares;
        int cajas;
    }

    private final java.util.Map<Integer, StockTotals> stockTotalsCache = new java.util.HashMap<>();
    private static final long CACHE_TTL_MS = 60_000L;
    private int pageSize = 25;
    private int currentPage = 1;
    private int totalRows = 0;
    private int totalPages = 0;
    private boolean isLoading = false;
    private SwingWorker<GenericPaginationService.PagedResult<ModelProduct>, Void> currentWorker;
    private java.util.concurrent.Future<?> imageLoadTask;
    private final java.util.concurrent.ExecutorService pageLoader = java.util.concurrent.Executors
            .newSingleThreadExecutor();
    private final java.util.concurrent.atomic.AtomicLong pageSeq = new java.util.concurrent.atomic.AtomicLong(0);
    private javax.swing.JPanel paginationPanel;
    private javax.swing.JButton btnFirstPage;
    private javax.swing.JButton btnPreviousPage;
    private javax.swing.JButton btnNextPage;
    private javax.swing.JButton btnLastPage;
    private javax.swing.JLabel lblPageInfo;
    private javax.swing.JTextField txtGoToPage;
    private javax.swing.JComboBox<String> cbxPageSize;
    private javax.swing.JLabel lblPageSize;
    private JWindow loadingWindow;
    private javax.swing.Timer loadingTimer;
    private javax.swing.Timer searchTimer;
    private String lastSearchTerm = ""; // Para detectar cambios reales
    private javax.swing.JComponent blurOverlay;
    private boolean uiResetInProgress = false;

    // ====================================================================
    // CONSTRUCTOR
    // ====================================================================
    public GestionProductosForm() {
        initComponents();
        initializeUserSession();
        lb.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");

        // SEPARAR LA INICIALIZACIÓN DE LA CARGA DE DATOS
        buildPaginationPanel(); // PRIMERO: Construir panel y componentes de paginación
        initializeUI(); // SEGUNDO: Configurar estilos (ahora los botones ya existen)
        setupPaginationControls(); // TERCERO: Agregar listeners
        restorePaginationStateFromPreferences(); // Restaurar estado de paginación persistido
        setupSidebarListeners(); // CUARTO: Conectar listeners del sidebar

        btn_nuevo.setVisible(false);

        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // CARGAR DATOS DESPUÉS DE QUE EL COMPONENTE ESTÉ LISTO
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) {
                loadFirstPage();
            }
        });
    }

    /**
     * Inicializa la información del usuario actual
     */
    private void initializeUI() {
        setupPanelStyles();
        setupTableStyles();
        setupButtonStyles();
        setupModalDefaults();
        setupTableColumns();
    }

    private void buildPaginationPanel() {
        // Panel de paginación con diseño moderno y espaciado
        paginationPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 10));
        paginationPanel.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Panel.background,2%);arc:15;border:1,1,0,1,$Component.borderColor");

        // Helper para cargar iconos de forma segura
        java.util.function.Function<String, javax.swing.Icon> loadIcon = path -> {
            try {
                return new com.formdev.flatlaf.extras.FlatSVGIcon(path, 0.6f);
            } catch (Exception e) {
                System.err.println("Error cargando icono " + path + ": " + e.getMessage());
                return null;
            }
        };

        // Botones con iconos SVG 3D y texto de respaldo
        javax.swing.Icon iconFirst = loadIcon.apply("raven/icon/svg/nav_first_3d.svg");
        javax.swing.Icon iconPrev = loadIcon.apply("raven/icon/svg/nav_prev_3d.svg");
        javax.swing.Icon iconNext = loadIcon.apply("raven/icon/svg/nav_next_3d.svg");
        javax.swing.Icon iconLast = loadIcon.apply("raven/icon/svg/nav_last_3d.svg");

        btnFirstPage = new javax.swing.JButton(iconFirst != null ? "" : "|<");
        if (iconFirst != null)
            btnFirstPage.setIcon(iconFirst);

        btnPreviousPage = new javax.swing.JButton(iconPrev != null ? "" : "<");
        if (iconPrev != null)
            btnPreviousPage.setIcon(iconPrev);

        btnNextPage = new javax.swing.JButton(iconNext != null ? "" : ">");
        if (iconNext != null)
            btnNextPage.setIcon(iconNext);

        btnLastPage = new javax.swing.JButton(iconLast != null ? "" : ">|");
        if (iconLast != null)
            btnLastPage.setIcon(iconLast);

        // Tamaño optimizado para mejor UX
        java.awt.Dimension btnSize = new java.awt.Dimension(48, 40);
        btnFirstPage.setPreferredSize(btnSize);
        btnPreviousPage.setPreferredSize(btnSize);
        btnNextPage.setPreferredSize(btnSize);
        btnLastPage.setPreferredSize(btnSize);

        // Estilos modernos con colores sólidos
        String baseStyleBlue = "arc:12;borderWidth:0;background:#42A5F5;" +
                "foreground:#ffffff;hoverBackground:#64B5F6;" +
                "pressedBackground:#1E88E5";

        String baseStyleGray = "arc:12;borderWidth:0;background:$Component.borderColor;" +
                "foreground:$Label.disabledForeground";

        btnFirstPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, baseStyleBlue);
        btnPreviousPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, baseStyleBlue);
        btnNextPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, baseStyleBlue);
        btnLastPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, baseStyleBlue);

        // Tooltips descriptivos
        btnFirstPage.setToolTipText("⏮ Primera página");
        btnPreviousPage.setToolTipText("◀ Página anterior");
        btnNextPage.setToolTipText("▶ Página siguiente");
        btnLastPage.setToolTipText("⏭ Última página");

        // Label de información con estilo moderno
        lblPageInfo = new javax.swing.JLabel("Inicializando...");
        lblPageInfo.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                "font:bold +1;foreground:$Label.foreground");

        // Campo de texto para ir a página específica
        txtGoToPage = new javax.swing.JTextField(4);
        txtGoToPage.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;borderWidth:1;borderColor:$Component.borderColor;" +
                        "focusedBorderColor:$Component.accentColor;margin:4,8,4,8");
        txtGoToPage.setToolTipText("Ingrese número de página");

        // ComboBox de tamaño de página con estilo
        cbxPageSize = new javax.swing.JComboBox<>(new String[] { "10", "25", "50", "100" });
        cbxPageSize.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;borderWidth:1;borderColor:$Component.borderColor");
        cbxPageSize.setToolTipText("Productos por página");

        lblPageSize = new javax.swing.JLabel("📄 Por página:");
        lblPageSize.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        // Agregar componentes con separadores visuales
        paginationPanel.add(btnFirstPage);
        paginationPanel.add(btnPreviousPage);

        // Separador sutil
        javax.swing.JSeparator sep1 = new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL);
        sep1.setPreferredSize(new java.awt.Dimension(1, 30));
        paginationPanel.add(sep1);

        paginationPanel.add(lblPageInfo);

        javax.swing.JSeparator sep2 = new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL);
        sep2.setPreferredSize(new java.awt.Dimension(1, 30));
        paginationPanel.add(sep2);

        paginationPanel.add(btnNextPage);
        paginationPanel.add(btnLastPage);

        // Separador más amplio
        paginationPanel.add(javax.swing.Box.createHorizontalStrut(20));

        paginationPanel.add(new javax.swing.JLabel("🔍 Ir a:"));
        paginationPanel.add(txtGoToPage);

        paginationPanel.add(javax.swing.Box.createHorizontalStrut(15));

        paginationPanel.add(lblPageSize);
        paginationPanel.add(cbxPageSize);

        // ====================================================================
        // NUEVO: INTEGRACIÓN DEL SIDEBAR
        // ====================================================================

        // Crear sidebar
        sidebar = new ProductSidebarPanel();

        // Configurar listener de selección de productos
        sidebar.setProductSelectionListener(product -> {
            selectProductInTable(product.getProductId());
        });

        // ====================================================================
        // OPTIMIZACIÓN DE LAYOUT (Eliminar buscador interno y reorganizar botones)
        // ====================================================================

        // 1. Configurar el panel de contenido (tabla)
        panel.setLayout(new java.awt.BorderLayout());
        panel.removeAll(); // Eliminar layout anterior (GroupLayout)

        // Barra de herramientas simplificada
        javax.swing.JPanel toolBar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 5));
        toolBar.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        // Botones de Acción Principal
        toolBar.add(btn_nuevo);
        toolBar.add(btn_editar);
        toolBar.add(btn_eliminar);
        toolBar.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));

        // Gestión y Filtros
        toolBar.add(btnCargarPorCaja);
        toolBar.add(cbxFiltro);
        toolBar.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));

        // Vistas y Navegación
        toolBar.add(btn_Prducto);
        toolBar.add(btn_Variable);
        toolBar.add(btn_Precios);
        toolBar.add(btn_historial);
        toolBar.add(btn_SinPrecioVenta);

        // Info Bodega a la derecha
        javax.swing.JPanel topBar = new javax.swing.JPanel(new java.awt.BorderLayout());
        topBar.add(toolBar, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel infoPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        infoPanel.add(LbBodega);
        topBar.add(infoPanel, java.awt.BorderLayout.EAST);

        // Agregar componentes al panel principal de contenido
        panel.add(topBar, java.awt.BorderLayout.NORTH);
        panel.add(scroll, java.awt.BorderLayout.CENTER);

        // 2. Configurar layout global del formulario
        this.setLayout(new java.awt.BorderLayout());
        this.removeAll();

        // Panel contenedor central
        javax.swing.JPanel mainContentPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        // Quitar el label grande "GESTION DE PRODUCTOS" para ganar espacio vertical
        // (optimización 100%)
        // mainContentPanel.add(lb, java.awt.BorderLayout.NORTH);
        mainContentPanel.add(panel, java.awt.BorderLayout.CENTER);
        mainContentPanel.add(paginationPanel, java.awt.BorderLayout.SOUTH);

        // Sidebar a la izquierda, Contenido al centro
        this.add(sidebar, java.awt.BorderLayout.WEST);
        this.add(mainContentPanel, java.awt.BorderLayout.CENTER);

        // Revalidar para asegurar que se muestre
        this.revalidate();
        this.repaint();
    }

    private void updateRowFilters() {
        // MÉTODO DEPRECADO: El filtrado se realiza ahora en el backend (ver
        // loadCurrentPage y updateProductData)
        // Se mantiene vacío para evitar errores de compilación si es llamado desde
        // código legado
        if (sorter != null) {
            sorter.setRowFilter(null);
        }
    }

    private int toInt(Object v) {
        try {
            if (v == null)
                return 0;
            if (v instanceof Number)
                return ((Number) v).intValue();
            String s = String.valueOf(v).replaceAll("[^0-9-]", "").trim();
            if (s.isEmpty())
                return 0;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void initializeUserSession() {
        // Verificar si hay sesión activa
        if (!userSession.isLoggedIn()) {
            System.err.println("WARNING: No se detectó sesión activa en GestionProductosForm");
        }

        // Obtener información del usuario actual
        currentUser = userSession.getCurrentUser();
        if (currentUser != null) {
            // Establecer el id de usuario actual desde la sesión
            currentUserId = currentUser.getIdUsuario();
            userLocation = currentUser.getUbicacion();
            isUserFromStore = currentUser.esDeTienda();
            isUserFromWarehouse = currentUser.esDeBodega();

            updateUIForUserLocation();

            System.out.println(
                    "SUCCESS Usuario inicializado: " + currentUser.getNombre() + " (ID: " + currentUserId + ")");
        } else {
            showError("Error: No se pudo obtener información del usuario", null);
            // Establecer valores por defecto pero SIN cargar un usuario random
            userLocation = "bodega";
            isUserFromStore = false;
            isUserFromWarehouse = true;
            // No cargamos fallback para no ensuciar auditoría
        }
    }

    /**
     * Obtiene el primer usuario activo como fallback para desarrollo.
     */
    private int fetchDefaultActiveUserId() {
        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = con.prepareStatement(
                        "SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario ASC LIMIT 1");
                java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("WARNING No fue posible obtener un usuario activo por defecto: " + e.getMessage());
        }
        return -1;
    }

    private void fillTableFromList(java.util.List<raven.controlador.productos.ModelProduct> list) {
        resetUIStateBeforeReload();
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) table.getModel();
        model.setRowCount(0);
        int rowNum = 1;
        for (raven.controlador.productos.ModelProduct product : list) {
            model.addRow(createTableRow(product, rowNum++));
        }
        updateRowFilters();

        // Actualizar sidebar con la lista de productos
        if (sidebar != null && list != null) {
            sidebar.setProducts(list);
        }
    }

    private javax.swing.Timer cacheTimer;

    private void detenerVerificadorCache() {
        if (cacheTimer != null) {
            cacheTimer.stop();
            cacheTimer = null;
        }
    }

    private void showLoading(String text) {
        if (loadingTimer != null) {
            loadingTimer.stop();
        }

        // Indicador de carga moderno con animación en el cursor y label
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        // Animación de puntos suspensivos en el texto
        lblPageInfo.setText("⏳ " + text);
        lblPageInfo.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +1;foreground:$Component.accentColor");

        // Timer de seguridad para restaurar cursor si algo falla
        loadingTimer = new javax.swing.Timer(5000, e -> hideLoading());
        loadingTimer.setRepeats(false);
        loadingTimer.start();
    }

    private void hideLoading() {
        if (loadingTimer != null) {
            loadingTimer.stop();
            loadingTimer = null;
        }
        setCursor(java.awt.Cursor.getDefaultCursor());

        // Restaurar estilo normal del label
        lblPageInfo.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +1;foreground:$Label.foreground");
        // Restaurar texto de paginación si es necesario (updatePaginationControls lo
        // hará)
    }

    // Solo cargar cuando esté visible
    @Override
    public void addNotify() {
        super.addNotify();
        // SOLO cargar si el componente está visible
        // MODIFICADO: Se comenta para evitar recargas automáticas que hacen perder el
        // estado
        /*
         * if (isDisplayable() && isShowing()) {
         * SwingUtilities.invokeLater(this::loadFirstPage);
         * }
         */
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        try {
            pageLoader.shutdownNow();
        } catch (Exception ignore) {
        }
        try {
            raven.utils.ProductImageOptimizer.clearCache();
        } catch (Exception ignore) {
        }
    }

    private void resetUIStateBeforeReload() {
        uiResetInProgress = true;
        try {
            if (table != null) {
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
                table.clearSelection();
                table.setRowSorter(null);
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
            }
        } catch (Exception ignore) {
        }
        uiResetInProgress = false;
    }

    private static class ProductLoadResult {
        List<ModelProduct> list;
        int totalRows;
        String bodegaText;
        String cacheKey;

        public ProductLoadResult(List<ModelProduct> list, int totalRows, String bodegaText, String cacheKey) {
            this.list = list;
            this.totalRows = totalRows;
            this.bodegaText = bodegaText;
            this.cacheKey = cacheKey;
        }
    }

    private void updateTableFromList(List<ModelProduct> list) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (ModelProduct product : list) {
            model.addRow(createTableRow(product, table.getRowCount() + 1));
        }
        updateRowFilters();

        // Actualizar sidebar con la lista de productos
        if (sidebar != null && list != null) {
            sidebar.setProducts(list);
        }
    }

    /**
     * Selecciona un producto en la tabla por su ID (usado por el sidebar)
     */
    private void selectProductInTable(int productId) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            Object cellValue = model.getValueAt(i, 3); // Columna ID (índice 3)
            if (cellValue != null) {
                int rowProductId = 0;
                if (cellValue instanceof Integer) {
                    rowProductId = (Integer) cellValue;
                } else if (cellValue instanceof String) {
                    try {
                        rowProductId = Integer.parseInt(cellValue.toString());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                if (rowProductId == productId) {
                    table.setRowSelectionInterval(i, i);
                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                    break;
                }
            }
        }
    }

    /**
     * Actualiza las métricas del sidebar con datos actuales
     */
    private void updateSidebarMetrics() {
        if (sidebar == null)
            return;

        try {
            Integer bodegaId = getCurrentBodegaId();

            // Calcular métricas reales desde la base de datos
            int sinPrecio = 0;
            int stockCritico = 0;
            double valorInventario = 0.0;
            int totalPares = 0;

            try (java.sql.Connection con = conexion.getInstance().createConnection()) {
                // Contar productos sin precio de venta
                String sqlSinPrecio = "SELECT COUNT(DISTINCT pv.id_producto) " +
                        "FROM producto_variantes pv " +
                        "INNER JOIN productos p ON p.id_producto = pv.id_producto " +
                        "WHERE p.activo = 1 AND (pv.precio_venta IS NULL OR pv.precio_venta <= 0)";
                try (java.sql.PreparedStatement ps = con.prepareStatement(sqlSinPrecio);
                        java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sinPrecio = rs.getInt(1);
                    }
                }

                // Contar productos con stock crítico (menos de 10 pares)
                String sqlStockCritico = "SELECT COUNT(DISTINCT p.id_producto) " +
                        "FROM productos p " +
                        "INNER JOIN producto_variantes pv ON pv.id_producto = p.id_producto " +
                        "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                        (bodegaId != null ? "AND ib.id_bodega = ? " : "") +
                        "WHERE p.activo = 1 " +
                        "GROUP BY p.id_producto " +
                        "HAVING SUM(COALESCE(ib.Stock_par, 0)) < 10";
                try (java.sql.PreparedStatement ps = con.prepareStatement(sqlStockCritico)) {
                    if (bodegaId != null)
                        ps.setInt(1, bodegaId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            stockCritico++;
                        }
                    }
                }

                // Calcular valor total del inventario y total de pares
                String sqlInventario = "SELECT " +
                        "SUM(COALESCE(ib.Stock_par, 0)) AS total_pares, " +
                        "SUM(COALESCE(ib.Stock_par, 0) * COALESCE(pv.precio_venta, 0)) AS valor_total " +
                        "FROM inventario_bodega ib " +
                        "INNER JOIN producto_variantes pv ON pv.id_variante = ib.id_variante " +
                        "WHERE ib.activo = 1" +
                        (bodegaId != null ? " AND ib.id_bodega = ?" : "");
                try (java.sql.PreparedStatement ps = con.prepareStatement(sqlInventario)) {
                    if (bodegaId != null)
                        ps.setInt(1, bodegaId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalPares = rs.getInt("total_pares");
                            valorInventario = rs.getDouble("valor_total");
                        }
                    }
                }
            }

            // Actualizar sidebar con métricas reales
            String valorInventarioStr;
            if (valorInventario >= 1_000_000) {
                valorInventarioStr = String.format("$%.1fM", valorInventario / 1_000_000.0);
            } else if (valorInventario >= 1_000) {
                valorInventarioStr = String.format("$%.1fK", valorInventario / 1_000.0);
            } else {
                valorInventarioStr = String.format("$%.0f", valorInventario);
            }

            sidebar.updateMetrics(sinPrecio, stockCritico, valorInventarioStr, totalPares);

        } catch (Exception e) {
            System.err.println("Error actualizando métricas del sidebar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void nextPage() {
        if (currentPage < totalPages && !isLoading) {
            currentPage++;
            loadCurrentPage();
        }
    }

    public void prevPage() {
        if (currentPage > 1 && !isLoading) {
            currentPage--;
            loadCurrentPage();
        }
    }

    private Object[] createTableRow(ModelProduct product, int rowNumber) {
        // Obtener nombres de marca y categoría
        String marcaNombre = (product.getBrand() != null) ? product.getBrand().getName() : "Sin marca";
        int totalStockPares = product.getPairsStock();
        int totalStockCajas = product.getBoxesStock();

        // OBTENER COLORES Y TALLAS (desde variantes o cache)
        String coloresDisponibles = (product.getColor() != null && !product.getColor().isEmpty()) ? product.getColor()
                : "Sin variantes";
        String tallasDisponibles = (product.getSize() != null && !product.getSize().isEmpty()) ? product.getSize()
                : "Sin variantes";

        // CORRECCIÓN: Si los colores/tallas vienen nulos del objeto principal pero hay
        // variantes cargadas,
        // intentar obtenerlos de la lista de variantes para asegurar que se muestren en
        // la tabla.
        if ((coloresDisponibles.equals("Sin variantes") || tallasDisponibles.equals("Sin variantes"))
                && product.getVariants() != null && !product.getVariants().isEmpty()) {

            if (coloresDisponibles.equals("Sin variantes")) {
                coloresDisponibles = product.getVariants().stream()
                        .map(ModelProductVariant::getColorName)
                        .filter(color -> color != null && !color.trim().isEmpty())
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                if (coloresDisponibles.isEmpty())
                    coloresDisponibles = "Varios colores";
            }

            if (tallasDisponibles.equals("Sin variantes")) {
                tallasDisponibles = product.getVariants().stream()
                        .map(ModelProductVariant::getSizeName)
                        .filter(talla -> talla != null && !talla.trim().isEmpty())
                        .distinct()
                        .sorted(new java.util.Comparator<String>() {
                            @Override
                            public int compare(String s1, String s2) {
                                try {
                                    return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
                                } catch (NumberFormatException e) {
                                    return s1.compareTo(s2);
                                }
                            }
                        })
                        .collect(Collectors.joining(", "));
                if (tallasDisponibles.isEmpty())
                    tallasDisponibles = "Varias tallas";
            }
        }

        if (totalStockPares == 0 && totalStockCajas == 0
                && (product.getVariants() == null || product.getVariants().isEmpty())) {
            totalStockPares = product.getPairsStock();
            totalStockCajas = product.getBoxesStock();
        }

        return new Object[] {
                false, // SELECT (checkbox)
                rowNumber, // N°
                product, // NOMBRE (objeto completo para el renderer)
                product.getProductId(), // ID
                product.getModelCode(), // MODELO
                marcaNombre, // MARCA
                coloresDisponibles, // COLORES DE VARIANTES
                tallasDisponibles, // TALLAS DE VARIANTES
                totalStockPares, // STOCK PARES (suma de variantes)
                totalStockCajas, // STOCK CAJAS (suma de variantes)
                userSession.hasPermission("variante_editar"), // PERMISO EDITAR
                userSession.hasPermission("variante_eliminar") // PERMISO ELIMINAR
        };
    }

    /**
     * Actualiza la UI según la ubicación del usuario - DISEÑO MODERNO
     */
    private void updateUIForUserLocation() {
        String originalTitle = "PRODUCTOS";
        lbTitle.setText("📦 " + originalTitle);
        lb.setText("GESTIÓN DE PRODUCTOS");

        try {
            Integer idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            if (idBodega == null || idBodega <= 0) {
                // Fallback usando UserSession si getIdBodegaUsuario() retornó null arriba (poco
                // probable si está logueado)
                idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            }
            if (idBodega != null && idBodega > 0) {
                raven.clases.admin.ServiceBodegas sb = new raven.clases.admin.ServiceBodegas();
                raven.controlador.admin.ModelBodegas b = sb.obtenerPorId(idBodega);
                int totalPares = service.getTotalPairsByBodega(idBodega);

                // Formato moderno con iconos y separadores
                String bodegaInfo = String.format("🏢 %s  •  👟 %,d pares",
                        (b != null ? b.getNombre() : ("Bodega #" + idBodega)),
                        totalPares);
                LbBodega.setText(bodegaInfo);
            } else {
                LbBodega.setText("🌐 Todas las bodegas");
            }
        } catch (Exception e) {
            LbBodega.setText("🏢 Bodega");
        }

        System.out.println("INFO Usuario: " + currentUser.getNombre() + " (" + currentUser.getRol() + ")");
    }

    // ====================================================================
    // MÉTODOS DE MENSAJES
    // ====================================================================

    private void showSuccess(String message) {
        System.out.println("SUCCESS " + message);

        if (isDisplayable() && getParent() != null) {
            try {
                Toast.show(this, Toast.Type.SUCCESS, message);
            } catch (Exception e) {
                System.err.println("WARNING [PRODUCTOS] No se pudo mostrar Toast de éxito: " + e.getMessage());
            }
        }
    }

    private void showError(String message, Exception e) {
        String fullMessage = message + (e != null ? ": " + e.getMessage() : "");
        System.err.println("ERROR " + fullMessage);

        if (e != null) {
            e.printStackTrace();
        }

        // MOSTRAR TOAST SOLO SI EL COMPONENTE ESTÁ LISTO
        if (isDisplayable() && getParent() != null) {
            try {
                Toast.show(this, Toast.Type.ERROR, fullMessage);
            } catch (Exception toastError) {
                System.err.println("WARNING [PRODUCTOS] No se pudo mostrar Toast: " + toastError.getMessage());
            }
        }
    }

    private void showErrorSafely(String message, Exception e) {
        String fullMessage = message + (e != null ? ": " + e.getMessage() : "");
        System.err.println("ERROR [PRODUCTOS] " + fullMessage);

        if (e != null) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                if (isDisplayable() && getParent() != null) {
                    Toast.show(this, Toast.Type.ERROR, fullMessage);
                } else {
                    System.err.println("WARNING [PRODUCTOS] Toast no disponible, mensaje mostrado en consola");
                }
            } catch (Exception toastError) {
                System.err.println("WARNING [PRODUCTOS] Error mostrando Toast: " + toastError.getMessage());
            }
        });
    }

    private void showWarning(String message) {
        System.out.println("WARNING " + message);

        if (isDisplayable() && getParent() != null) {
            try {
                Toast.show(this, Toast.Type.WARNING, message);
            } catch (Exception e) {
                System.err.println("WARNING [PRODUCTOS] No se pudo mostrar Toast de advertencia: " + e.getMessage());
            }
        }
    }

    private void showInfo(String message) {
        System.out.println("INFO " + message);

        if (isDisplayable() && getParent() != null) {
            try {
                Toast.show(this, Toast.Type.INFO, message);
            } catch (Exception e) {
                System.err.println("WARNING [PRODUCTOS] No se pudo mostrar Toast de información: " + e.getMessage());
            }
        }
    }

    // ====================================================================
    // MÉTODO PÚBLICO PARA REFRESCAR DATOS
    // ====================================================================

    public void refreshData() {
        if (isDisplayable()) {
            // Limpiar cache y recargar página actual
            serviceOptimizedCache.clearCache();
            productAdapter.clearCache();
            // También limpiar caché de imágenes para asegurar que se recarguen si hubo
            // errores
            try {
                raven.utils.ProductImageOptimizer.clearCache();
            } catch (Exception ignore) {
            }
            loadCurrentPage();
        }
    }

    // ====================================================================
    // MÉTODO OVERRIDE PARA DETECTAR CUÁNDO EL COMPONENTE ESTÁ LISTO
    // ====================================================================

    /**
     * NUEVO: Se ejecuta cuando el componente se agrega a la ventana
     */
    // El lifecycle addNotify ya está definido arriba para iniciar el verificador de
    // cache

    /**
     * Configura los estilos del panel principal - DISEÑO MODERNO
     */
    private void setupPanelStyles() {
        // Panel principal con gradiente sutil y sombra
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:20;background:lighten($Panel.background,3%);border:1,1,1,1,$Component.borderColor,1,12");

        // Título con estilo moderno y color accent
        lbTitle.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +8;foreground:$Component.accentColor");

        // Barra de búsqueda con diseño glassmorphism (OCULTA - se usa solo el sidebar)
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar por nombre, modelo, marca...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.9f));
        txtSearch.putClientProperty(FlatClientProperties.STYLE,
                "arc:20;borderWidth:2;focusWidth:0;innerFocusWidth:0;margin:8,20,8,20;" +
                        "background:lighten($Panel.background,5%);focusedBorderColor:$Component.accentColor;" +
                        "borderColor:$Component.borderColor");
        txtSearch.setVisible(false); // Ocultar - se usa solo búsqueda del sidebar

        // Label de bodega con estilo badge
        LbBodega.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +1;foreground:$Component.accentColor");

        // ComboBox de filtros con estilo moderno
        cbxFiltro.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:1;borderColor:$Component.borderColor;" +
                        "focusedBorderColor:$Component.accentColor;background:lighten($Panel.background,5%)");
    }

    /**
     * Configura los estilos de la tabla - DISEÑO MODERNO CON CARDS
     */
    private void setupTableStyles() {
        // Header de tabla con estilo moderno
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:45;hoverBackground:lighten($Component.accentColor,40%);" +
                        "pressedBackground:lighten($Component.accentColor,30%);" +
                        "separatorColor:$Component.borderColor;font:bold +1;" +
                        "background:darken($Panel.background,3%);" +
                        "foreground:$Label.foreground");

        // Tabla con efecto de tarjetas elevadas
        table.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:85;showHorizontalLines:true;intercellSpacing:0,4;" +
                        "cellFocusColor:$Component.accentColor;" +
                        "selectionBackground:lighten($Component.accentColor,35%);" +
                        "selectionForeground:$Table.foreground;" +
                        "background:$Panel.background");

        // Scrollbar moderno y minimalista
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;trackInsets:3,3,3,3;thumbInsets:3,3,3,3;" +
                        "background:$Panel.background;thumb:$Component.borderColor;" +
                        "hoverThumbColor:$Component.accentColor;pressedThumbColor:darken($Component.accentColor,10%)");

        scroll.putClientProperty(FlatClientProperties.STYLE,
                "border:0,0,0,0");

        try {
            raven.utils.ProductImageOptimizer.TablePerformanceOptimizer.optimizeTable(table);
        } catch (Exception ignore) {
        }
    }

    /**
     * Configura los estilos de los botones - DISEÑO MODERNO
     */
    private void setupButtonStyles() {
        // Configurar tipos de botón con bordes redondeados
        btnCargarPorCaja.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_nuevo.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_editar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_eliminar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);

        // Configurar botones de Productos, Variantes y Precios
        btn_Prducto.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_Variable.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_Precios.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_SinPrecioVenta.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);

        // BOTONES PRINCIPALES CON COLORES VIBRANTES
        btn_editar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#FFA726;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#FFB74D;" +
                        "pressedBackground:#FB8C00");

        btn_eliminar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#EF5350;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#F44336;" +
                        "pressedBackground:#D32F2F");

        // BOTONES DE CATEGORÍAS CON ESTILO MODERNO
        btn_Prducto.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#42A5F5;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#64B5F6;" +
                        "pressedBackground:#1E88E5");

        btn_Variable.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#AB47BC;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#BA68C8;" +
                        "pressedBackground:#8E24AA");

        btn_Precios.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#FFA726;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#FFB74D;" +
                        "pressedBackground:#FB8C00");

        btn_SinPrecioVenta.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#EF5350;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#F44336;" +
                        "pressedBackground:#D32F2F");

        // BOTÓN CARGAR CAJA CON ESTILO SUCCESS
        btnCargarPorCaja.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;background:#66BB6A;" +
                        "foreground:#FFFFFF;font:bold 12;hoverBackground:#81C784;" +
                        "pressedBackground:#43A047");

        // Estilos para botones de paginación con efecto hover suave
        String paginationStyle = "arc:12;borderWidth:1;borderColor:$Component.borderColor;" +
                "focusWidth:0;innerFocusWidth:0;background:$Panel.background;" +
                "hoverBackground:lighten($Component.accentColor,40%);" +
                "pressedBackground:lighten($Component.accentColor,30%)";

        btnFirstPage.putClientProperty(FlatClientProperties.STYLE, paginationStyle);
        btnPreviousPage.putClientProperty(FlatClientProperties.STYLE, paginationStyle);
        btnNextPage.putClientProperty(FlatClientProperties.STYLE, paginationStyle);
        btnLastPage.putClientProperty(FlatClientProperties.STYLE, paginationStyle);

        // Iconos
        try {
            // Ruta correcta del recurso: el ícono "caja.svg" está en raven/icon/svg/
            java.net.URL cajaUrl = getClass().getResource("/raven/icon/svg/caja.svg");
            FlatSVGIcon caja = (cajaUrl != null)
                    ? new FlatSVGIcon(cajaUrl).derive(18, 18)
                    : new FlatSVGIcon("raven/icon/svg/caja.svg").derive(18, 18);
            FlatSVGIcon iconNew = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/10.svg")).derive(18, 18);
            FlatSVGIcon iconEdit = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/11.svg")).derive(18, 18);
            FlatSVGIcon iconDelete = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/12.svg")).derive(18, 18);

            // Iconos para Productos, Variantes y Precios
            FlatSVGIcon iconProducto = new FlatSVGIcon("raven/icon/svg/caja.svg", 18, 18);
            FlatSVGIcon iconVariante = new FlatSVGIcon("raven/icon/icons/imagen.svg", 18, 18);
            FlatSVGIcon iconPrecios = new FlatSVGIcon("raven/icon/icons/usd-circulo.svg", 18, 18);
            FlatSVGIcon iconSinPrecio = new FlatSVGIcon("raven/icon/icons/circulo-cruzado.svg", 18, 18);

            btnCargarPorCaja.setIcon(caja);
            btn_nuevo.setIcon(iconNew);
            btn_editar.setIcon(iconEdit);
            btn_eliminar.setIcon(iconDelete);

            btn_Prducto.setIcon(iconProducto);
            btn_Variable.setIcon(iconVariante);
            btn_Precios.setIcon(iconPrecios);
            btn_SinPrecioVenta.setIcon(iconSinPrecio);

            btnCargarPorCaja.setIconTextGap(8);
            btn_Prducto.setIconTextGap(8);
            btn_Variable.setIconTextGap(8);
            btn_Precios.setIconTextGap(8);
            btn_SinPrecioVenta.setIconTextGap(8);
        } catch (Exception e) {
            System.err.println("No se pudieron cargar los iconos: " + e.getMessage());
        }

        // Márgenes más generosos para mejor UX
        Insets buttonMargin = new Insets(8, 12, 8, 12);
        btnCargarPorCaja.setMargin(buttonMargin);
        btn_nuevo.setMargin(buttonMargin);
        btn_editar.setMargin(buttonMargin);
        btn_eliminar.setMargin(buttonMargin);
        btn_Prducto.setMargin(buttonMargin);
        btn_Variable.setMargin(buttonMargin);
        btn_Precios.setMargin(buttonMargin);
        btn_SinPrecioVenta.setMargin(buttonMargin);
    }

    /**
     * Configura las columnas de la tabla para el nuevo sistema
     */
    private void setupTableColumns() {
        // Configurar header con checkbox para seleccionar todos
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));

        // Configurar renderer y editor para la columna de selección (checkbox)
        // Forzamos el uso de renderers de Boolean aunque el modelo devuelva Object
        table.getColumnModel().getColumn(0).setCellRenderer(table.getDefaultRenderer(Boolean.class));
        table.getColumnModel().getColumn(0).setCellEditor(table.getDefaultEditor(Boolean.class));

        // Configurar alineación de headers
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Configurar renderer personalizado para la columna NOMBRE (columna 2)
        // NOTA: OptimizedProductRenderer ya maneja la carga de imágenes,
        // pero necesitamos asegurarnos de que la columna sea editable para la selección
        // o que el evento de clic se maneje correctamente.
        table.getColumnModel().getColumn(2)
                .setCellRenderer(new raven.utils.ProductImageOptimizer.OptimizedProductRenderer());

        // Configurar previsualización de imágenes (Hover)
        raven.utils.ProductImageOptimizer.ImagePreviewHandler.attach(table);

        // Comportamiento: el checkbox (columna 0) se marca solo al hacer clic en su
        // celda.
        // Un clic en otras columnas solo selecciona la fila (comportamiento por defecto
        // de JTable).
    }

    /**
     * Configura las opciones por defecto de los modales
     */
    private void setupModalDefaults() {
        ModalDialog.getDefaultOption()
                .setOpacity(0.3f)
                .getLayoutOption().setAnimateScale(0.1f);
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM);
    }

    // ====================================================================
    // GESTIÓN DE DATOS
    // ====================================================================

    /**
     * Busca productos según el término ingresado
     */
    private void searchData(String searchTerm) throws SQLException {
        if (!isDisplayable()) {
            return;
        }
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        currentPage = 1;
        String term = (searchTerm != null) ? searchTerm.trim() : "";
        String searchTermFinal = term.isEmpty() ? null : term;
        final Integer bodegaId = getCurrentBodegaId();

        // Capturar filtro de UI antes de entrar al hilo
        final String filtroSeleccionado = (String) cbxFiltro.getSelectedItem();
        final String tipoFiltro;
        if ("Pares".equals(filtroSeleccionado)) {
            tipoFiltro = "pares";
        } else if ("Cajas".equals(filtroSeleccionado)) {
            tipoFiltro = "cajas";
        } else {
            tipoFiltro = null;
        }

        showLoading("Buscando productos...");
        new Thread(() -> {
            try {
                GenericPaginationService.PagedResult<ModelProduct> result = productAdapter.getProductsPagedAdvanced(
                        searchTermFinal,
                        null,
                        null,
                        null,
                        null,
                        bodegaId,
                        true,
                        tipoFiltro,
                        currentPage,
                        pageSize);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        updateProductData(result);
                    } finally {
                        hideLoading();
                        updatePaginationControls();
                    }
                });
            } catch (Exception ex) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showErrorSafely("Error en la búsqueda", ex);
                });
            }
        }, "SearchProducts").start();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();
        btn_nuevo = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btnCargarPorCaja = new javax.swing.JButton();
        cbxFiltro = new javax.swing.JComboBox<>();
        LbBodega = new javax.swing.JLabel();
        btn_Variable = new javax.swing.JButton();
        btn_Prducto = new javax.swing.JButton();
        btn_Precios = new javax.swing.JButton();
        btn_historial = new javax.swing.JButton();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("GESTION DE PRODUCTOS");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "SELECT", "N°", "NOMBRE", "ID", "MODELO", "MARCA", "COLOR", "TALLA", "STOCK PAR", "STOCK CAJA"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    true, false, false, true, true, false, true, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(20);
            table.getColumnModel().getColumn(1).setPreferredWidth(30);
            table.getColumnModel().getColumn(2).setPreferredWidth(160);
            table.getColumnModel().getColumn(3).setPreferredWidth(30);
            table.getColumnModel().getColumn(4).setPreferredWidth(100);
            table.getColumnModel().getColumn(5).setPreferredWidth(90);
            table.getColumnModel().getColumn(6).setPreferredWidth(90);
            table.getColumnModel().getColumn(7).setPreferredWidth(50);
            table.getColumnModel().getColumn(8).setPreferredWidth(50);
            table.getColumnModel().getColumn(9).setPreferredWidth(50);
        }

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("PRODUCTOS");

        btn_nuevo.setText("NUEVO");
        btn_nuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_nuevoActionPerformed(evt);
            }
        });

        btn_editar.setText("EDITAR");
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        btn_eliminar.setText("ELIMINAR");
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        btnCargarPorCaja.setText("Cargar Caja");
        btnCargarPorCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCargarPorCajaActionPerformed(evt);
            }
        });

        cbxFiltro.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Pares", "Cajas" }));

        LbBodega.setText("Bodega");

        btn_Variable.setText("Variable");
        btn_Variable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_VariableActionPerformed(evt);
            }
        });

        btn_Prducto.setText("Productos");
        btn_Prducto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_PrductoActionPerformed(evt);
            }
        });

        btn_Precios.setText("Precios");
        btn_Precios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_PreciosActionPerformed(evt);
            }
        });

        btn_historial.setText("HISTORIAL");
        btn_historial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_historialActionPerformed(evt);
            }
        });

        btn_SinPrecioVenta = new javax.swing.JButton();
        btn_SinPrecioVenta.setText("Sin Precio");
        btn_SinPrecioVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_SinPrecioVentaActionPerformed(evt);
            }
        });

        btn_SinPrecioVenta = new javax.swing.JButton();
        btn_SinPrecioVenta.setText("Sin Precio");
        btn_SinPrecioVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_SinPrecioVentaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jSeparator1)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addGap(20, 20, 20)
                                                .addGroup(panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelLayout.createSequentialGroup()
                                                                .addComponent(txtSearch,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 239,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(cbxFiltro,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                        53, Short.MAX_VALUE)
                                                                .addComponent(btnCargarPorCaja,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 151,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btn_nuevo,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(panelLayout.createSequentialGroup()
                                                                .addComponent(lbTitle)
                                                                .addGap(17, 17, 17)
                                                                .addComponent(LbBodega,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 174,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        Short.MAX_VALUE)
                                                                .addComponent(btn_Precios,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(panelLayout.createSequentialGroup()
                                                                .addComponent(btn_editar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btn_eliminar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btn_historial,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(panelLayout.createSequentialGroup()
                                                                .addComponent(btn_SinPrecioVenta,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btn_Prducto,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btn_Variable,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGap(6, 6, 6))
                                        .addComponent(scroll))
                                .addContainerGap()));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addGap(26, 26, 26)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lbTitle)
                                        .addComponent(LbBodega)
                                        .addComponent(btn_Variable)
                                        .addComponent(btn_Prducto)
                                        .addComponent(btn_Precios)
                                        .addComponent(btn_SinPrecioVenta))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_nuevo)
                                        .addComponent(btn_editar)
                                        .addComponent(btn_eliminar)
                                        .addComponent(btn_historial)
                                        .addComponent(btnCargarPorCaja)
                                        .addComponent(cbxFiltro, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 884, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addContainerGap())));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lb)
                                .addGap(0, 732, Short.MAX_VALUE))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(31, 31, 31)
                                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addContainerGap())));
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSearchKeyReleased
        scheduleDebouncedSearch();
    }// GEN-LAST:event_txtSearchKeyReleased

    private void btn_nuevoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_nuevoActionPerformed
        Create createForm = new Create();
        createForm.setParentForm(this);
        createForm.cargarDatosProducto(service, null); // Método actualizado

        // Envolver el formulario en un scroll para que no tape el footer de botones
        javax.swing.JScrollPane formScrollNuevo = new javax.swing.JScrollPane(createForm);
        formScrollNuevo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        formScrollNuevo.setOpaque(false);
        formScrollNuevo.getViewport().setOpaque(false);
        formScrollNuevo.getVerticalScrollBar().setUnitIncrement(16);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar Producto", SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(formScrollNuevo, "Nuevo Producto", options,
                        (ModalController mc, int option) -> {
                            if (option == SimpleModalBorder.OK_OPTION) {
                                try {
                                    // USAR EL NUEVO SISTEMA DE GUARDADO
                                    boolean guardado = createForm.guardarProductoConUsuario(currentUserId);

                                    if (guardado) {
                                        showSuccess("Producto creado exitosamente con todas sus variantes");
                                        serviceOptimizedCache.clearCache();
                                        try {
                                            productAdapter.clearCache();
                                        } catch (Exception ignore) {
                                        }
                                        variantSummaryCache.clear();
                                        stockTotalsCache.clear();
                                        loadCurrentPage();
                                        mc.close(); // Cerrar modal
                                    } else {
                                        showError("No se pudo guardar el producto", null);
                                    }

                                } catch (Exception e) {
                                    showError("Error al crear producto", e);
                                }
                            } else if (option == SimpleModalBorder.OPENED) {
                                createForm.init();
                            }
                        })); // TODO add your handling code here:
    }// GEN-LAST:event_btn_nuevoActionPerformed

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_editarActionPerformed
        List<ModelProduct> selectedProducts = getSelectedData();

        if (selectedProducts.isEmpty()) {
            showWarning("Seleccione un producto para editar");
            return;
        }

        if (selectedProducts.size() > 1) {
            showWarning("Seleccione solo un producto para editar");
            return;
        }

        ModelProduct selectedProduct = selectedProducts.get(0);

        // OPTIMIZACIÓN: Usar datos ya cargados en la tabla (vienen del SP con todos los
        // campos necesarios)
        // Evitamos una consulta extra a la BD (getFullById)
        openEditModal(selectedProduct);
    }

    private void openEditModal(ModelProduct product) {
        try {
            // OPTIMIZACIÓN: Crear formulario y abrir modal inmediatamente
            // Los datos pesados se cargarán de forma asíncrona después de abrir el modal
            Create editForm = new Create();
            editForm.setParentForm(GestionProductosForm.this);
            
            // Cargar datos básicos del producto de forma síncrona (rápido)
            // Esto permite que el modal se abra inmediatamente con información visible
            editForm.cargarDatosProductoBasicos(product);

            javax.swing.JScrollPane formScrollEditar = new javax.swing.JScrollPane(editForm);
            formScrollEditar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
            formScrollEditar.setOpaque(false);
            formScrollEditar.getViewport().setOpaque(false);
            formScrollEditar.getVerticalScrollBar().setUnitIncrement(16);

            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Actualizar Producto", SimpleModalBorder.OK_OPTION)
            };

            ModalDialog.showModal(
                    GestionProductosForm.this,
                    new SimpleModalBorder(formScrollEditar, "Editar: " + product.getName(), options,
                            (ModalController mc, int option) -> {
                                if (option == SimpleModalBorder.OK_OPTION) {
                                    try {
                                        ModelProduct updatedProduct = editForm.obtenerDatosProducto();
                                        if (updatedProduct != null) {
                                            updatedProduct.setProductId(product.getProductId());
                                            service.updateWithVariants(updatedProduct, currentUserId);
                                            showSuccess("Producto actualizado exitosamente");
                                            serviceOptimizedCache.clearCache();
                                            try {
                                                productAdapter.clearCache();
                                            } catch (Exception ignore) {
                                            }
                                            variantSummaryCache.clear();
                                            stockTotalsCache.clear();
                                            loadCurrentPage();
                                            mc.close();
                                        } else {
                                            showError("Error obteniendo datos del formulario", null);
                                        }
                                    } catch (Exception e) {
                                        showError("Error al actualizar producto", e);
                                    }
                                } else if (option == SimpleModalBorder.OPENED) {
                                    editForm.init();
                                    // OPTIMIZACIÓN: Cargar datos pesados después de abrir el modal
                                    // Esto permite que el modal se abra rápidamente y luego cargue los datos pesados
                                    SwingUtilities.invokeLater(() -> {
                                        editForm.cargarDatosProductoCompletos(service, product);
                                    });
                                }
                            }));
        } catch (Exception e) {
            showError("Error preparando edición", e);
        }
    }// GEN-LAST:event_btn_editarActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_eliminarActionPerformed
        List<ModelProduct> selectedProducts = getSelectedData();

        if (selectedProducts.isEmpty()) {
            showWarning("Seleccione al menos un producto para eliminar");
            return;
        }

        String message = selectedProducts.size() == 1
                ? "¿Está seguro de eliminar el producto '" + selectedProducts.get(0).getName() + "'?"
                : "¿Está seguro de eliminar " + selectedProducts.size() + " productos?";

        JLabel confirmLabel = new JLabel("<html><center>" + message + "<br><br>" +
                "<i>Nota: Esto también eliminará todas las variantes asociadas</i></center></html>");
        confirmLabel.setBorder(new EmptyBorder(10, 25, 10, 25));

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Eliminar", SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(confirmLabel, "Confirmar Eliminación", options,
                        (ModalController mc, int option) -> {
                            if (option == SimpleModalBorder.OK_OPTION) {
                                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                                new javax.swing.SwingWorker<Integer, Void>() {
                                    int errores = 0;

                                    @Override
                                    protected Integer doInBackground() throws Exception {
                                        int eliminados = 0;
                                        for (ModelProduct product : selectedProducts) {
                                            boolean ok = service.delete(product.getProductId(), currentUserId);
                                            if (ok) {
                                                eliminados++;
                                                System.out.println("SUCCESS Producto eliminado: " + product.getName());
                                            } else {
                                                errores++;
                                                System.err.println("ERROR Error eliminando " + product.getName());
                                            }
                                        }
                                        return eliminados;
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            int eliminados = get();
                                            if (eliminados > 0) {
                                                showSuccess(eliminados + " producto(s) eliminado(s) exitosamente" +
                                                        (errores > 0 ? " (" + errores + " errores)" : ""));
                                                serviceOptimizedCache.clearCache();
                                                try {
                                                    productAdapter.clearCache();
                                                } catch (Exception ignore) {
                                                }
                                                variantSummaryCache.clear();
                                                stockTotalsCache.clear();
                                                loadCurrentPage();
                                            } else {
                                                showError("No se pudo eliminar ningún producto", null);
                                            }
                                            mc.close();
                                        } catch (Exception e) {
                                            showError("Error durante la eliminación", e);
                                        } finally {
                                            setCursor(java.awt.Cursor.getDefaultCursor());
                                        }
                                    }
                                }.execute();
                            }
                        }));
    }// GEN-LAST:event_btn_eliminarActionPerformed

    private void btn_historialActionPerformed(java.awt.event.ActionEvent evt) {
        List<ModelProduct> selectedProducts = getSelectedData();
        if (selectedProducts.isEmpty()) {
            showWarning("Seleccione un producto para ver su historial");
            return;
        }
        if (selectedProducts.size() > 1) {
            showWarning("Seleccione solo un producto para ver historial");
            return;
        }

        ModelProduct product = selectedProducts.get(0);

        raven.application.form.productos.HistorialCambiosPanel panel = new raven.application.form.productos.HistorialCambiosPanel();
        panel.loadHistory(product.getProductId());
        panel.setPreferredSize(new java.awt.Dimension(800, 500));

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cerrar", SimpleModalBorder.CANCEL_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(panel, "Historial de Cambios: " + product.getName(), options,
                        (ModalController mc, int option) -> {
                            if (option == SimpleModalBorder.CANCEL_OPTION) {
                                mc.close();
                            }
                        }));
    }

    private void btn_VariableActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_VariableActionPerformed
        List<ModelProduct> selectedProducts = getSelectedData();
        if (selectedProducts == null || selectedProducts.isEmpty()) {
            // Si no hay producto seleccionado, abrir gestor de variantes sin imagen
            openMissingImageVariantsManager();
            return;
        }
        if (selectedProducts.size() > 1) {
            showWarning("Seleccione solo un producto");
            return;
        }

        // Mostrar opciones: Vista Tradicional o Vista Matriz
        Object[] options = { "Vista Matriz 🔲", "Vista Tradicional 📋", "Cancelar" };
        int choice = JOptionPane.showOptionDialog(
                this,
                "Seleccione el tipo de vista para gestionar las variantes:",
                "Gestión de Variantes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        ModelProduct product = selectedProducts.get(0);

        if (choice == 0) {
            // Vista Matriz (NUEVA)
            openVariantMatrixView(product);
        } else if (choice == 1) {
            // Vista Tradicional (EXISTENTE)
            openVariantModalForProduct(product, null);
        }
    }// GEN-LAST:event_btn_VariableActionPerformed

    /**
     * OPTIMIZACIÓN: Abre la vista matriz de variantes de forma asíncrona para mejor rendimiento
     */
    private void openVariantMatrixView(ModelProduct product) {
        // Mostrar indicador de carga
        showLoading("Cargando variantes...");
        
        // Cargar variantes de forma asíncrona
        new SwingWorker<List<ModelProductVariant>, Void>() {
            @Override
            protected List<ModelProductVariant> doInBackground() throws Exception {
                return service.getVariantsByProductId(product.getProductId());
            }
            
            @Override
            protected void done() {
                hideLoading();
                try {
                    List<ModelProductVariant> variants = get();
                    
                    if (variants == null || variants.isEmpty()) {
                        showWarning("Este producto no tiene variantes registradas");
                        return;
                    }

                    // Crear diálogo con vista matriz en EDT
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(GestionProductosForm.this);
                            ServiceProductVariant variantService = new ServiceProductVariant();
                            VariantMatrixDialog dialog = new VariantMatrixDialog(parentFrame, product.getName(), variants, variantService);

                            // Configurar listener para guardar cambios de stock
                            dialog.getMatrixPanel().setStockChangeListener((variant, newStock) -> {
                                try {
                                    // Actualizar en base de datos
                                    variant.setStock(newStock);
                                    variantService.upsertVariant(variant);

                                    // Mostrar confirmación
                                    showSuccess("Stock actualizado correctamente");

                                    // Actualizar sidebar si existe
                                    if (sidebar != null) {
                                        updateSidebarMetrics();
                                    }

                                } catch (Exception e) {
                                    showError("Error al actualizar stock: " + e.getMessage(), e);
                                }
                            });

                            // Mostrar diálogo
                            dialog.setVisible(true);

                            // Refrescar datos después de cerrar el diálogo
                            refreshData();
                        } catch (Exception e) {
                            showError("Error al abrir la vista matriz de variantes", e);
                        }
                    });
                } catch (Exception e) {
                    hideLoading();
                    showError("Error al cargar variantes", e);
                }
            }
        }.execute();
    }

    private void openMissingImageVariantsManager() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "Variantes sin Imagen", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(java.awt.Color.WHITE);

        // Título
        javax.swing.JLabel lblTitle = new javax.swing.JLabel("Gestor de Imágenes Faltantes");
        lblTitle.setFont(lblTitle.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        lblTitle.setForeground(new java.awt.Color(50, 50, 50));
        panel.add(lblTitle, java.awt.BorderLayout.NORTH);

        // Table
        String[] columns = { "ID", "Producto", "Género", "Color", "Proveedor" };
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable table = new javax.swing.JTable(model);
        // Hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.setRowHeight(35);
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowGrid(true);
        table.setGridColor(new java.awt.Color(230, 230, 230));

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(table);
        panel.add(scroll, java.awt.BorderLayout.CENTER);

        // Buttons
        javax.swing.JPanel btnPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnPanel.setBackground(java.awt.Color.WHITE);

        javax.swing.JButton btnUpload = new javax.swing.JButton("Subir Imagen");
        btnUpload.setEnabled(false);

        javax.swing.JButton btnClose = new javax.swing.JButton("Cerrar");

        btnPanel.add(btnUpload);
        btnPanel.add(btnClose);
        panel.add(btnPanel, java.awt.BorderLayout.SOUTH);

        // Load Data Runnable
        Runnable loadData = () -> {
            model.setRowCount(0);
            try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                    java.sql.PreparedStatement ps = con.prepareStatement(
                            "SELECT v.id_variante, p.nombre, p.genero, c.nombre as color, prov.nombre as proveedor " +
                                    "FROM producto_variantes v " +
                                    "JOIN productos p ON v.id_producto = p.id_producto " +
                                    "JOIN colores c ON v.id_color = c.id_color " +
                                    "LEFT JOIN proveedores prov ON v.id_proveedor = prov.id_proveedor " +
                                    "WHERE v.imagen IS NULL OR LENGTH(v.imagen) = 0 " +
                                    "ORDER BY p.nombre, c.nombre")) {

                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[] {
                                rs.getInt("id_variante"),
                                rs.getString("nombre"),
                                rs.getString("genero"),
                                rs.getString("color"),
                                rs.getString("proveedor")
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(dialog, "Error cargando variantes: " + e.getMessage(),
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };

        loadData.run();

        // Listeners
        table.getSelectionModel().addListSelectionListener(e -> {
            btnUpload.setEnabled(table.getSelectedRow() != -1);
        });

        btnClose.addActionListener(e -> dialog.dispose());

        btnUpload.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1)
                return;

            int variantId = (int) model.getValueAt(row, 0);

            java.awt.FileDialog fd = new java.awt.FileDialog(dialog, "Seleccionar Imagen", java.awt.FileDialog.LOAD);
            fd.setFile("*.jpg;*.jpeg;*.png;*.gif");
            fd.setVisible(true);

            if (fd.getFile() != null) {
                java.io.File file = new java.io.File(fd.getDirectory(), fd.getFile());
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());

                    try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                            .createConnection();
                            java.sql.PreparedStatement ps = con.prepareStatement(
                                    "UPDATE producto_variantes SET imagen = ? WHERE id_variante = ?")) {
                        ps.setBytes(1, bytes);
                        ps.setInt(2, variantId);
                        ps.executeUpdate();

                        javax.swing.JOptionPane.showMessageDialog(dialog, "Imagen subida exitosamente");
                        loadData.run(); // Refresh
                        btnUpload.setEnabled(false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(dialog, "Error subiendo imagen: " + ex.getMessage(),
                            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private javax.swing.JDialog createLoadingDialog(java.awt.Component parent) {
        javax.swing.JDialog loading = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(parent),
                "Cargando", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        loading.setUndecorated(true);
        javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
        p.setBackground(java.awt.Color.WHITE);
        p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)),
                javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        javax.swing.JLabel lbl = new javax.swing.JLabel("Guardando...", javax.swing.SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        lbl.setForeground(new java.awt.Color(80, 80, 80));

        javax.swing.JProgressBar pb = new javax.swing.JProgressBar();
        pb.setIndeterminate(true);

        p.add(lbl, java.awt.BorderLayout.NORTH);
        p.add(pb, java.awt.BorderLayout.CENTER);

        loading.setContentPane(p);
        loading.pack();
        loading.setLocationRelativeTo(parent);
        return loading;
    }

    public void openVariantModalForProduct(ModelProduct product, Integer variantId) {
        System.out.println("\n========================================");
        System.out.println("INFO ABRIENDO MODAL DE VARIANTE");
        System.out.println("   Producto: " + product.getName() + " (ID: " + product.getProductId() + ")");
        System.out.println("   Variante ID: " + (variantId != null ? variantId : "NUEVA"));
        System.out.println("========================================\n");

        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(6, 8, 6, 8);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        javax.swing.JLabel lblInfo = new javax.swing.JLabel("Conectado a: " + product.getName() + " - "
                + (product.getModelCode() != null ? product.getModelCode() : "SIN MODELO") + " (ID "
                + product.getProductId() + ")");
        lblInfo.putClientProperty(FlatClientProperties.STYLE, "font:bold $h3.font;foreground:#0A84FF");

        javax.swing.JLabel lblProveedor = new javax.swing.JLabel("Proveedor");
        javax.swing.JComboBox<IdName> cbProveedor = new javax.swing.JComboBox<>();
        cbProveedor.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblTipo = new javax.swing.JLabel("Tipo");
        javax.swing.JComboBox<String> cbTipo = new javax.swing.JComboBox<>(new String[] { "Par", "Caja" });
        cbTipo.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblColor = new javax.swing.JLabel("Color");
        javax.swing.JComboBox<IdName> cbColor = new javax.swing.JComboBox<>();
        // OPTIMIZACIÓN: Remover 'arc' que causa errores en FlatLaf
        cbColor.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblTalla = new javax.swing.JLabel("Talla");
        javax.swing.JComboBox<IdName> cbTalla = new javax.swing.JComboBox<>();
        cbTalla.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblStock = new javax.swing.JLabel("Stock");
        javax.swing.JTextField txtStock = new javax.swing.JTextField(8);
        txtStock.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0");
        txtStock.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        // PERMISO: variante_stock
        if (!userSession.hasPermission("variante_stock")) {
            txtStock.setEditable(false);
            txtStock.setToolTipText("No tienes permiso para modificar el stock manualmente");
        }

        javax.swing.JLabel lblBodega = new javax.swing.JLabel("Bodega");
        javax.swing.JComboBox<IdName> cbBodega = new javax.swing.JComboBox<>();
        cbBodega.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblImagen = new javax.swing.JLabel("Imagen");
        javax.swing.JButton btnImagen = new javax.swing.JButton("Seleccionar");
        btnImagen.putClientProperty(FlatClientProperties.STYLE, "background:#0A84FF;foreground:#fff");
        final byte[][] imagenSeleccionada = new byte[1][];
        final javax.swing.JLabel imgPreview = new javax.swing.JLabel();
        imgPreview.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imgPreview.setPreferredSize(new java.awt.Dimension(160, 120));
        // FlatLaf: JLabel no soporta 'margin'/'padding' en STYLE.
        // Usar border real para el padding interno y mantener el estilo solo para fondo/borde.
        imgPreview.setOpaque(true);
        imgPreview.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        imgPreview.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Menu.background,15%);border:1,1,1,1,darken($Component.borderColor,10%)");
        btnImagen.addActionListener(e -> {
            // USAR java.awt.FileDialog (Explorador nativo de Windows)
            try {
                java.awt.FileDialog fd = new java.awt.FileDialog(
                        (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Seleccionar Imagen",
                        java.awt.FileDialog.LOAD);
                fd.setFile("*.jpg;*.jpeg;*.png;*.gif");
                fd.setVisible(true);

                if (fd.getFile() != null) {
                    java.io.File archivoSeleccionado = new java.io.File(fd.getDirectory(), fd.getFile());

                    // Leer bytes del archivo
                    byte[] fileBytes = java.nio.file.Files.readAllBytes(archivoSeleccionado.toPath());
                    imagenSeleccionada[0] = fileBytes;

                    javax.swing.ImageIcon raw = new javax.swing.ImageIcon(archivoSeleccionado.getAbsolutePath());
                    java.awt.Image scaled = raw.getImage().getScaledInstance(160, 120, java.awt.Image.SCALE_SMOOTH);
                    imgPreview.setIcon(new javax.swing.ImageIcon(scaled));
                    System.out.println("SUCCESS Imagen seleccionada: " + archivoSeleccionado.getName());
                }
            } catch (Exception ex) {
                showError("No se pudo abrir el explorador de archivos", ex);
            }
        });

        javax.swing.JLabel lblEst = new javax.swing.JLabel("Estantería");
        javax.swing.JTextField txtEst = new javax.swing.JTextField(18);
        txtEst.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ubicación específica");
        txtEst.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        // ═══════════════════════════════════════════════════════════════════════════
        // CAMPOS DE PRECIO DE COMPRA Y VENTA
        // ═══════════════════════════════════════════════════════════════════════════
        javax.swing.JLabel lblPrecioCompra = new javax.swing.JLabel("Precio Compra");
        javax.swing.JTextField txtPrecioCompra = new javax.swing.JTextField(12);
        txtPrecioCompra.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        txtPrecioCompra.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Menu.background,25%)");
        txtPrecioCompra.setText(product.getPurchasePrice() > 0 ? String.valueOf(product.getPurchasePrice()) : "");

        javax.swing.JLabel lblPrecioVenta = new javax.swing.JLabel("Precio Venta");
        javax.swing.JTextField txtPrecioVenta = new javax.swing.JTextField(12);
        txtPrecioVenta.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        txtPrecioVenta.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");
        txtPrecioVenta.setText(product.getSalePrice() > 0 ? String.valueOf(product.getSalePrice()) : "");

        // Label para mostrar sugerencias de precio
        javax.swing.JLabel lblSugerencia = new javax.swing.JLabel("");
        lblSugerencia.putClientProperty(FlatClientProperties.STYLE, "font:italic;foreground:#FF9500;");

        // Label de carga
        javax.swing.JLabel lblLoading = new javax.swing.JLabel("Cargando datos...");
        lblLoading.putClientProperty(FlatClientProperties.STYLE, "foreground:#888");
        lblLoading.setVisible(false);

        // SwingWorker para carga asíncrona de datos
        new javax.swing.SwingWorker<Void, Void>() {
            java.util.List<raven.controlador.comercial.ModelSupplier> proveedores;
            java.util.List<IdName> colores = new java.util.ArrayList<>();
            java.util.List<IdName> bodegas = new java.util.ArrayList<>();
            java.util.List<IdName> tallas = new java.util.ArrayList<>();
            raven.controlador.productos.ModelProductVariant v = null;
            Integer idProveedorCargado = null;
            java.util.List<raven.controlador.inventario.InventarioBodega> invs = null;
            byte[] imgBytes = null;

            @Override
            protected Void doInBackground() throws Exception {
                // 1. Cargar listas básicas
                proveedores = new raven.clases.comercial.ServiceSupplier().getAll();

                try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                        java.sql.PreparedStatement psC = con
                                .prepareStatement("SELECT id_color, nombre FROM colores ORDER BY nombre");
                        java.sql.PreparedStatement psB = con.prepareStatement(
                                "SELECT id_bodega, nombre FROM bodegas WHERE activa=1 ORDER BY nombre");) {

                    try (java.sql.ResultSet rs = psC.executeQuery()) {
                        while (rs.next())
                            colores.add(new IdName(rs.getInt(1), rs.getString(2)));
                    }

                    String gProd = product.getGender();
                    String sqlT = "SELECT id_talla, CONCAT_WS(' ', numero, sistema, genero) AS nombre FROM tallas ";
                    if (gProd != null) {
                        String g = gProd.trim().toUpperCase();
                        if ("HOMBRE".equals(g)) {
                            sqlT += "WHERE UPPER(genero) = 'HOMBRE' ";
                        } else if ("MUJER".equals(g)) {
                            sqlT += "WHERE UPPER(genero) = 'MUJER' ";
                        } else if ("NIÑO".equals(g)) {
                            sqlT += "WHERE UPPER(genero) = 'NIÑO' ";
                        } else if ("UNISEX".equals(g)) {
                            sqlT += "WHERE UPPER(genero) IN ('HOMBRE','MUJER') ";
                        }
                    }
                    sqlT += "ORDER BY CAST(numero AS UNSIGNED), sistema, genero";

                    try (java.sql.PreparedStatement psT = con.prepareStatement(sqlT);
                            java.sql.ResultSet rs = psT.executeQuery()) {
                        while (rs.next())
                            tallas.add(new IdName(rs.getInt(1), rs.getString(2)));
                    }

                    try (java.sql.ResultSet rs = psB.executeQuery()) {
                        while (rs.next())
                            bodegas.add(new IdName(rs.getInt(1), rs.getString(2)));
                    }
                }

                // 2. Si es edición, cargar datos de la variante
                if (variantId != null && variantId > 0) {
                    raven.clases.productos.ServiceProductVariant sv = new raven.clases.productos.ServiceProductVariant();
                    v = sv.getVariantById(variantId, false);

                    if (v != null) {
                        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                                .createConnection();
                                java.sql.PreparedStatement pst = con.prepareStatement(
                                        "SELECT id_proveedor FROM producto_variantes WHERE id_variante=?")) {
                            pst.setInt(1, variantId);
                            try (java.sql.ResultSet rs = pst.executeQuery()) {
                                if (rs.next()) {
                                    idProveedorCargado = rs.getInt("id_proveedor");
                                }
                            }
                        }

                        invs = new raven.clases.inventario.ServiceInventarioBodega().getInventarioByVariante(variantId);

                        // Cargar imagen
                        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                                .createConnection();
                                java.sql.PreparedStatement pst = con.prepareStatement(
                                        "SELECT imagen FROM producto_variantes WHERE id_variante=?")) {
                            pst.setInt(1, variantId);
                            try (java.sql.ResultSet rs = pst.executeQuery()) {
                                if (rs.next()) {
                                    java.sql.Blob blob = rs.getBlob("imagen");
                                    if (blob != null && blob.length() > 0) {
                                        imgBytes = blob.getBytes(1, (int) blob.length());
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check exceptions

                    // Llenar combos
                    for (raven.controlador.comercial.ModelSupplier p : proveedores)
                        cbProveedor.addItem(new IdName(p.getSupplierId(), p.getName()));
                    if (product.getSupplier() != null) {
                        for (int i = 0; i < cbProveedor.getItemCount(); i++) {
                            if (cbProveedor.getItemAt(i).id == product.getSupplier().getSupplierId()) {
                                cbProveedor.setSelectedIndex(i);
                                break;
                            }
                        }
                    }

                    for (IdName c : colores)
                        cbColor.addItem(c);
                    for (IdName t : tallas)
                        cbTalla.addItem(t);
                    for (IdName b : bodegas)
                        cbBodega.addItem(b);

                    // Si es edición, setear valores
                    if (v != null) {
                        for (int i = 0; i < cbColor.getItemCount(); i++) {
                            if (cbColor.getItemAt(i).id == v.getColorId()) {
                                cbColor.setSelectedIndex(i);
                                break;
                            }
                        }
                        for (int i = 0; i < cbTalla.getItemCount(); i++) {
                            if (cbTalla.getItemAt(i).id == v.getSizeId()) {
                                cbTalla.setSelectedIndex(i);
                                break;
                            }
                        }

                        if (v.getPurchasePrice() != null && v.getPurchasePrice() > 0)
                            txtPrecioCompra.setText(String.format("%.2f", v.getPurchasePrice()));
                        if (v.getSalePrice() != null && v.getSalePrice() > 0)
                            txtPrecioVenta.setText(String.format("%.2f", v.getSalePrice()));

                        if (idProveedorCargado != null && idProveedorCargado > 0) {
                            for (int i = 0; i < cbProveedor.getItemCount(); i++) {
                                if (cbProveedor.getItemAt(i).id == idProveedorCargado) {
                                    cbProveedor.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }

                        if (invs != null && !invs.isEmpty()) {
                            raven.controlador.inventario.InventarioBodega inv = invs.stream()
                                    .filter(i -> (i.getStockPar() != null && i.getStockPar() > 0)
                                            || (i.getStockCaja() != null && i.getStockCaja() > 0))
                                    .findFirst()
                                    .orElse(invs.get(0));

                            for (int i = 0; i < cbBodega.getItemCount(); i++) {
                                if (cbBodega.getItemAt(i).id == inv.getIdBodega()) {
                                    cbBodega.setSelectedIndex(i);
                                    break;
                                }
                            }

                            int stockPar = inv.getStockPar() != null ? inv.getStockPar() : 0;
                            int stockCaja = inv.getStockCaja() != null ? inv.getStockCaja() : 0;

                            if (stockCaja > 0) {
                                cbTipo.setSelectedItem("Caja");
                                txtStock.setText(String.valueOf(stockCaja));
                            } else if (stockPar > 0) {
                                cbTipo.setSelectedItem("Par");
                                txtStock.setText(String.valueOf(stockPar));
                            } else {
                                cbTipo.setSelectedItem("Par");
                                txtStock.setText("0");
                            }

                            if (inv.getUbicacionEspecifica() != null)
                                txtEst.setText(inv.getUbicacionEspecifica());
                        }

                        if (imgBytes != null && imgBytes.length > 0) {
                            imagenSeleccionada[0] = imgBytes;
                            javax.swing.ImageIcon raw = new javax.swing.ImageIcon(imgBytes);
                            java.awt.Image scaled = raw.getImage().getScaledInstance(160, 120,
                                    java.awt.Image.SCALE_SMOOTH);
                            imgPreview.setIcon(new javax.swing.ImageIcon(scaled));
                        }
                    }

                    lblLoading.setVisible(false);

                    java.util.List<IdName> allColores = new java.util.ArrayList<>(colores);
                    configurarBuscadorColor(cbColor, allColores);

                    java.lang.Runnable refreshStock = new java.lang.Runnable() {
                        @Override
                        public void run() {
                            try {
                                String tipoSel = (String) cbTipo.getSelectedItem();
                                IdName bSel = (IdName) cbBodega.getSelectedItem();
                                int sp = 0, sc = 0;
                                if (variantId != null && bSel != null && bSel.id > 0) {
                                    try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                                            .createConnection();
                                            java.sql.PreparedStatement pst = con.prepareStatement(
                                                    "SELECT COALESCE(Stock_par,0), COALESCE(Stock_caja,0) FROM inventario_bodega WHERE id_variante=? AND id_bodega=? AND activo=1 LIMIT 1")) {
                                        pst.setInt(1, variantId);
                                        pst.setInt(2, bSel.id);
                                        try (java.sql.ResultSet rs = pst.executeQuery()) {
                                            if (rs.next()) {
                                                sp = rs.getInt(1);
                                                sc = rs.getInt(2);
                                            }
                                        }
                                    }
                                }
                                if ("Caja".equalsIgnoreCase(tipoSel)) {
                                    txtStock.setText(String.valueOf(sc));
                                } else {
                                    txtStock.setText(String.valueOf(sp));
                                }
                            } catch (Exception ex) {
                                txtStock.setText("0");
                            }
                        }
                    };

                    cbTipo.addActionListener(e -> refreshStock.run());
                    cbBodega.addActionListener(e -> refreshStock.run());
                    refreshStock.run();
                } catch (Exception ex) {
                    showError("Error cargando datos", ex);
                }
            }
        }.execute();

        // UI Layout
        form.add(lblInfo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblProveedor, gbc);
        gbc.gridx = 1;
        form.add(cbProveedor, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblTipo, gbc);
        gbc.gridx = 1;
        form.add(cbTipo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblColor, gbc);
        gbc.gridx = 1;
        form.add(cbColor, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        form.add(lblSugerencia, gbc);
        gbc.gridwidth = 1; // Mostrar sugerencia de precio
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblTalla, gbc);
        gbc.gridx = 1;
        form.add(cbTalla, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblPrecioCompra, gbc);
        gbc.gridx = 1;
        form.add(txtPrecioCompra, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblPrecioVenta, gbc);
        gbc.gridx = 1;
        form.add(txtPrecioVenta, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblStock, gbc);
        gbc.gridx = 1;
        form.add(txtStock, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblBodega, gbc);
        gbc.gridx = 1;
        form.add(cbBodega, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblImagen, gbc);
        gbc.gridx = 1;
        form.add(btnImagen, gbc);
        gbc.gridx = 2;
        form.add(imgPreview, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblEst, gbc);
        gbc.gridx = 1;
        form.add(txtEst, gbc);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option(variantId != null ? "Guardar Cambios" : "Guardar Variante",
                        SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(form, variantId != null ? "Editar Variante" : "Nueva Variante", options,
                        (raven.modal.listener.ModalController mc, int opt) -> {
                            if (opt == SimpleModalBorder.OK_OPTION) {
                                try {
                                    IdName prov = (IdName) cbProveedor.getSelectedItem();
                                    IdName color = (IdName) cbColor.getSelectedItem();
                                    IdName talla = (IdName) cbTalla.getSelectedItem();
                                    IdName bodega = (IdName) cbBodega.getSelectedItem();
                                    String tipo = (String) cbTipo.getSelectedItem();
                                    int stockVal = 0;
                                    try {
                                        stockVal = Integer.parseInt(txtStock.getText().trim());
                                    } catch (Exception ignore) {
                                    }
                                    final int stock = stockVal;

                                    if (!userSession.hasPermission("variante_stock")) {
                                        // Validar que el stock no haya cambiado si no tiene permiso
                                        // (Aunque el campo esté deshabilitado, validación extra)
                                        // En este caso confiamos en la UI deshabilitada
                                    }

                                    // Validar proveedor obligatorio
                                    if (color == null || talla == null || prov == null || bodega == null || stock < 0) {
                                        showWarning("Complete color, talla, PROVEEDOR, bodega y stock");
                                        return;
                                    }

                                    // Parsear precios de los campos de texto
                                    double precioCompraVal = 0.0;
                                    double precioVentaVal = 0.0;
                                    try {
                                        precioCompraVal = Double.parseDouble(txtPrecioCompra.getText().trim());
                                    } catch (Exception ignore) {
                                    }
                                    try {
                                        precioVentaVal = Double.parseDouble(txtPrecioVenta.getText().trim());
                                    } catch (Exception ignore) {
                                    }
                                    final double precioCompra = precioCompraVal;
                                    final double precioVenta = precioVentaVal;
                                    final String ubicacion = txtEst.getText() != null ? txtEst.getText().trim() : null;

                                    // Mostrar loading
                                    javax.swing.JDialog loading = createLoadingDialog(form);

                                    new javax.swing.SwingWorker<Void, Void>() {
                                        @Override
                                        protected Void doInBackground() throws Exception {
                                            // Thread.sleep(500); // Eliminado para optimización

                                            if (variantId == null) {
                                                raven.controlador.productos.ModelProductVariant v = new raven.controlador.productos.ModelProductVariant();
                                                v.setProductId(product.getProductId());
                                                v.setSizeId(talla.id);
                                                v.setColorId(color.id);
                                                v.setSupplierId(prov.id);
                                                String skuGenerado = generarSKUVariante(product.getModelCode(),
                                                        talla.name != null ? talla.name : "",
                                                        color.name != null ? color.name : "", tipo, 1);
                                                v.setSku(skuGenerado);
                                                v.generateEanIfEmpty();
                                                v.setPurchasePrice(
                                                        precioCompra > 0 ? precioCompra : product.getPurchasePrice());
                                                v.setSalePrice(precioVenta > 0 ? precioVenta : product.getSalePrice());
                                                if (imagenSeleccionada[0] != null && imagenSeleccionada[0].length > 0)
                                                    v.setImageBytes(imagenSeleccionada[0]);
                                                raven.dao.ProductoVariantesDAO daoVar = new raven.dao.ProductoVariantesDAO();
                                                int idVar = daoVar.insert(v);
                                                v.setVariantId(idVar);

                                                raven.clases.inventario.ServiceInventarioBodega invSrv = new raven.clases.inventario.ServiceInventarioBodega();
                                                invSrv.crearOActualizarInventario(bodega.id, idVar, Math.max(0, stock),
                                                        tipo, ubicacion);
                                            } else {
                                                raven.clases.productos.ServiceProductVariant sv = new raven.clases.productos.ServiceProductVariant();
                                                raven.controlador.productos.ModelProductVariant vExist = sv
                                                        .getVariantById(variantId);
                                                if (vExist == null) {
                                                    throw new Exception("Variante no encontrada");
                                                }
                                                vExist.setSizeId(talla.id);
                                                vExist.setColorId(color.id);
                                                vExist.setPurchasePrice(
                                                        precioCompra > 0 ? precioCompra : product.getPurchasePrice());
                                                vExist.setSalePrice(
                                                        precioVenta > 0 ? precioVenta : product.getSalePrice());

                                                // Solo actualizar imagen si se seleccionó una nueva
                                                if (imagenSeleccionada[0] != null && imagenSeleccionada[0].length > 0) {
                                                    vExist.setImageBytes(imagenSeleccionada[0]);
                                                }

                                                raven.dao.ProductoVariantesDAO daoVar = new raven.dao.ProductoVariantesDAO();
                                                daoVar.update(vExist);
                                                if (prov != null && prov.id > 0) {
                                                    try (java.sql.Connection con = raven.controlador.principal.conexion
                                                            .getInstance().createConnection();
                                                            java.sql.PreparedStatement pst = con.prepareStatement(
                                                                    "UPDATE producto_variantes SET id_proveedor=? WHERE id_variante=?")) {
                                                        pst.setInt(1, prov.id);
                                                        pst.setInt(2, variantId);
                                                        pst.executeUpdate();
                                                    } catch (Exception ignore) {
                                                    }
                                                }

                                                raven.clases.inventario.ServiceInventarioBodega invSrv = new raven.clases.inventario.ServiceInventarioBodega();
                                                invSrv.crearOActualizarInventario(bodega.id, variantId,
                                                        Math.max(0, stock), tipo, ubicacion);
                                            }
                                            return null;
                                        }

                                        @Override
                                        protected void done() {
                                            loading.dispose();
                                            try {
                                                get(); // Check for errors
                                                showSuccess(variantId == null ? "Variante creada exitosamente"
                                                        : "Variante actualizada exitosamente");
                                                serviceOptimizedCache.clearCache();
                                                try {
                                                    productAdapter.clearCache();
                                                } catch (Exception ignore) {
                                                }
                                                variantSummaryCache.clear();
                                                stockTotalsCache.clear();
                                                loadCurrentPage();
                                                mc.close();
                                            } catch (Exception e) {
                                                showError("Error guardando variante: " + e.getMessage(), e);
                                            }
                                        }
                                    }.execute();

                                    loading.setVisible(true);

                                } catch (Exception e) {
                                    showError("Error preparando guardado", e);
                                }
                            }
                        }));
    }

    public void openVariantModalForProduct(ModelProduct product, Integer variantId, String initialType,
            Integer initialBodegaId, Integer initialStock) {
        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(6, 8, 6, 8);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        javax.swing.JLabel lblInfo = new javax.swing.JLabel("Conectado a: " + product.getName() + " - "
                + (product.getModelCode() != null ? product.getModelCode() : "SIN MODELO") + " (ID "
                + product.getProductId() + ")");
        lblInfo.putClientProperty(FlatClientProperties.STYLE, "font:bold $h3.font;foreground:#0A84FF");

        javax.swing.JLabel lblProveedor = new javax.swing.JLabel("Proveedor");
        javax.swing.JComboBox<IdName> cbProveedor = new javax.swing.JComboBox<>();
        cbProveedor.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblTipo = new javax.swing.JLabel("Tipo");
        javax.swing.JComboBox<String> cbTipo = new javax.swing.JComboBox<>(new String[] { "Par", "Caja" });
        cbTipo.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblColor = new javax.swing.JLabel("Color");
        javax.swing.JComboBox<IdName> cbColor = new javax.swing.JComboBox<>();
        cbColor.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblTalla = new javax.swing.JLabel("Talla");
        javax.swing.JComboBox<IdName> cbTalla = new javax.swing.JComboBox<>();
        cbTalla.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblStock = new javax.swing.JLabel("Stock");
        javax.swing.JTextField txtStock = new javax.swing.JTextField(8);
        txtStock.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0");
        txtStock.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblBodega = new javax.swing.JLabel("Bodega");
        javax.swing.JComboBox<IdName> cbBodega = new javax.swing.JComboBox<>();
        cbBodega.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblImagen = new javax.swing.JLabel("Imagen");
        javax.swing.JButton btnImagen = new javax.swing.JButton("Seleccionar");
        btnImagen.putClientProperty(FlatClientProperties.STYLE, "background:#0A84FF;foreground:#fff");
        final byte[][] imagenSeleccionada = new byte[1][];
        final javax.swing.JLabel imgPreview = new javax.swing.JLabel();
        imgPreview.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imgPreview.setPreferredSize(new java.awt.Dimension(160, 120));
        imgPreview.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Menu.background,15%);border:5,5,5,5");
        btnImagen.addActionListener(e -> {
            try {
                java.awt.FileDialog fd = new java.awt.FileDialog(
                        (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Seleccionar Imagen",
                        java.awt.FileDialog.LOAD);
                fd.setFile("*.jpg;*.jpeg;*.png;*.gif");
                fd.setVisible(true);
                if (fd.getFile() != null) {
                    java.io.File archivoSeleccionado = new java.io.File(fd.getDirectory(), fd.getFile());
                    byte[] fileBytes = java.nio.file.Files.readAllBytes(archivoSeleccionado.toPath());
                    imagenSeleccionada[0] = fileBytes;
                    javax.swing.ImageIcon raw = new javax.swing.ImageIcon(archivoSeleccionado.getAbsolutePath());
                    java.awt.Image scaled = raw.getImage().getScaledInstance(160, 120, java.awt.Image.SCALE_SMOOTH);
                    imgPreview.setIcon(new javax.swing.ImageIcon(scaled));
                }
            } catch (Exception ex) {
                showError("No se pudo abrir el explorador de archivos", ex);
            }
        });

        javax.swing.JLabel lblEst = new javax.swing.JLabel("Estantería");
        javax.swing.JTextField txtEst = new javax.swing.JTextField(18);
        txtEst.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ubicación específica");
        txtEst.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        javax.swing.JLabel lblPrecioCompra = new javax.swing.JLabel("Precio Compra");
        javax.swing.JTextField txtPrecioCompra = new javax.swing.JTextField(12);
        txtPrecioCompra.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        txtPrecioCompra.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Menu.background,25%)");
        txtPrecioCompra.setText(product.getPurchasePrice() > 0 ? String.valueOf(product.getPurchasePrice()) : "");

        javax.swing.JLabel lblPrecioVenta = new javax.swing.JLabel("Precio Venta");
        javax.swing.JTextField txtPrecioVenta = new javax.swing.JTextField(12);
        txtPrecioVenta.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        txtPrecioVenta.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");
        txtPrecioVenta.setText(product.getSalePrice() > 0 ? String.valueOf(product.getSalePrice()) : "");

        javax.swing.JLabel lblSugerencia = new javax.swing.JLabel("");
        lblSugerencia.putClientProperty(FlatClientProperties.STYLE, "font:italic;foreground:#FF9500;");

        javax.swing.JLabel lblLoading = new javax.swing.JLabel("Cargando datos...");
        lblLoading.putClientProperty(FlatClientProperties.STYLE, "foreground:#888");
        lblLoading.setVisible(false);

        new javax.swing.SwingWorker<Void, Void>() {
            java.util.List<raven.controlador.comercial.ModelSupplier> proveedores;
            java.util.List<IdName> colores = new java.util.ArrayList<>();
            java.util.List<IdName> bodegas = new java.util.ArrayList<>();
            java.util.List<IdName> tallas = new java.util.ArrayList<>();
            raven.controlador.productos.ModelProductVariant v = null;
            Integer idProveedorCargado = null;
            java.util.List<raven.controlador.inventario.InventarioBodega> invs = null;
            byte[] imgBytes = null;

            @Override
            protected Void doInBackground() throws Exception {
                proveedores = new raven.clases.comercial.ServiceSupplier().getAll();
                try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                        java.sql.PreparedStatement psC = con
                                .prepareStatement("SELECT id_color, nombre FROM colores ORDER BY nombre");
                        java.sql.PreparedStatement psB = con.prepareStatement(
                                "SELECT id_bodega, nombre FROM bodegas WHERE activa=1 ORDER BY nombre");) {
                    try (java.sql.ResultSet rs = psC.executeQuery()) {
                        while (rs.next())
                            colores.add(new IdName(rs.getInt(1), rs.getString(2)));
                    }
                    String gProd = product.getGender();
                    String sqlT = "SELECT id_talla, CONCAT_WS(' ', numero, sistema, genero) AS nombre FROM tallas ";
                    if (gProd != null) {
                        String g = gProd.trim().toUpperCase();
                        if ("HOMBRE".equals(g)) {
                            sqlT += "WHERE UPPER(genero) = 'HOMBRE' ";
                        } else if ("MUJER".equals(g)) {
                            sqlT += "WHERE UPPER(genero) = 'MUJER' ";
                        } else if ("NIÑO".equals(g)) {
                            sqlT += "WHERE UPPER(genero) = 'NIÑO' ";
                        } else if ("UNISEX".equals(g)) {
                            sqlT += "WHERE UPPER(genero) IN ('HOMBRE','MUJER') ";
                        }
                    }
                    sqlT += "ORDER BY CAST(numero AS UNSIGNED), sistema, genero";
                    try (java.sql.PreparedStatement psT = con.prepareStatement(sqlT);
                            java.sql.ResultSet rs = psT.executeQuery()) {
                        while (rs.next())
                            tallas.add(new IdName(rs.getInt(1), rs.getString(2)));
                    }
                    try (java.sql.ResultSet rs = psB.executeQuery()) {
                        while (rs.next())
                            bodegas.add(new IdName(rs.getInt(1), rs.getString(2)));
                    }
                }
                if (variantId != null && variantId > 0) {
                    raven.clases.productos.ServiceProductVariant sv = new raven.clases.productos.ServiceProductVariant();
                    v = sv.getVariantById(variantId, false);
                    if (v != null) {
                        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                                .createConnection()) {
                            try (java.sql.PreparedStatement pst = con.prepareStatement(
                                    "SELECT id_proveedor FROM producto_variantes WHERE id_variante=?")) {
                                pst.setInt(1, variantId);
                                try (java.sql.ResultSet rs = pst.executeQuery()) {
                                    if (rs.next()) {
                                        idProveedorCargado = rs.getInt("id_proveedor");
                                    }
                                }
                            }

                            // 1. Validar si el Color actual está en la lista (puede faltar si cambió filtro
                            // de género o está inactivo)
                            boolean colorExists = false;
                            for (IdName c : colores) {
                                if (c.id == v.getColorId()) {
                                    colorExists = true;
                                    break;
                                }
                            }
                            if (!colorExists) {
                                try (java.sql.PreparedStatement pst = con.prepareStatement(
                                        "SELECT id_color, nombre FROM colores WHERE id_color=?")) {
                                    pst.setInt(1, v.getColorId());
                                    try (java.sql.ResultSet rs = pst.executeQuery()) {
                                        if (rs.next()) {
                                            colores.add(new IdName(rs.getInt(1), rs.getString(2)));
                                        }
                                    }
                                }
                            }

                            // 2. Validar si la Talla actual está en la lista
                            boolean tallaExists = false;
                            for (IdName t : tallas) {
                                if (t.id == v.getSizeId()) {
                                    tallaExists = true;
                                    break;
                                }
                            }
                            if (!tallaExists) {
                                try (java.sql.PreparedStatement pst = con.prepareStatement(
                                        "SELECT id_talla, CONCAT_WS(' ', numero, sistema, genero) AS nombre FROM tallas WHERE id_talla=?")) {
                                    pst.setInt(1, v.getSizeId());
                                    try (java.sql.ResultSet rs = pst.executeQuery()) {
                                        if (rs.next()) {
                                            tallas.add(new IdName(rs.getInt(1), rs.getString(2)));
                                        }
                                    }
                                }
                            }

                            // 3. Validar si el Proveedor actual está en la lista
                            if (idProveedorCargado != null) {
                                boolean provExists = false;
                                for (raven.controlador.comercial.ModelSupplier p : proveedores) {
                                    if (p.getSupplierId() == idProveedorCargado) {
                                        provExists = true;
                                        break;
                                    }
                                }
                                if (!provExists) {
                                    try (java.sql.PreparedStatement pst = con.prepareStatement(
                                            "SELECT id_proveedor, nombre, ruc, direccion, telefono, email, activo FROM proveedores WHERE id_proveedor=?")) {
                                        pst.setInt(1, idProveedorCargado);
                                        try (java.sql.ResultSet rs = pst.executeQuery()) {
                                            if (rs.next()) {
                                                raven.controlador.comercial.ModelSupplier s = new raven.controlador.comercial.ModelSupplier();
                                                s.setSupplierId(rs.getInt("id_proveedor"));
                                                s.setName(rs.getString("nombre"));
                                                s.setRuc(rs.getString("ruc"));
                                                s.setAddress(rs.getString("direccion"));
                                                s.setPhone(rs.getString("telefono"));
                                                s.setEmail(rs.getString("email"));
                                                s.setActive(rs.getBoolean("activo"));
                                                proveedores.add(s);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        invs = new raven.clases.inventario.ServiceInventarioBodega().getInventarioByVariante(variantId);
                        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                                .createConnection();
                                java.sql.PreparedStatement pst = con.prepareStatement(
                                        "SELECT imagen FROM producto_variantes WHERE id_variante=?")) {
                            pst.setInt(1, variantId);
                            try (java.sql.ResultSet rs = pst.executeQuery()) {
                                if (rs.next()) {
                                    java.sql.Blob blob = rs.getBlob("imagen");
                                    if (blob != null && blob.length() > 0) {
                                        imgBytes = blob.getBytes(1, (int) blob.length());
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    for (raven.controlador.comercial.ModelSupplier p : proveedores)
                        cbProveedor.addItem(new IdName(p.getSupplierId(), p.getName()));
                    if (product.getSupplier() != null) {
                        for (int i = 0; i < cbProveedor.getItemCount(); i++) {
                            if (cbProveedor.getItemAt(i).id == product.getSupplier().getSupplierId()) {
                                cbProveedor.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    for (IdName c : colores)
                        cbColor.addItem(c);
                    for (IdName t : tallas)
                        cbTalla.addItem(t);
                    for (IdName b : bodegas)
                        cbBodega.addItem(b);

                    if (v != null) {
                        for (int i = 0; i < cbColor.getItemCount(); i++) {
                            if (cbColor.getItemAt(i).id == v.getColorId()) {
                                cbColor.setSelectedIndex(i);
                                break;
                            }
                        }
                        for (int i = 0; i < cbTalla.getItemCount(); i++) {
                            if (cbTalla.getItemAt(i).id == v.getSizeId()) {
                                cbTalla.setSelectedIndex(i);
                                break;
                            }
                        }
                        if (v.getPurchasePrice() != null && v.getPurchasePrice() > 0)
                            txtPrecioCompra.setText(String.format("%.2f", v.getPurchasePrice()));
                        if (v.getSalePrice() != null && v.getSalePrice() > 0)
                            txtPrecioVenta.setText(String.format("%.2f", v.getSalePrice()));
                        if (idProveedorCargado != null && idProveedorCargado > 0) {
                            for (int i = 0; i < cbProveedor.getItemCount(); i++) {
                                if (cbProveedor.getItemAt(i).id == idProveedorCargado) {
                                    cbProveedor.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                        if (invs != null && !invs.isEmpty()) {
                            raven.controlador.inventario.InventarioBodega inv = null;
                            if (initialBodegaId != null && initialBodegaId > 0) {
                                for (raven.controlador.inventario.InventarioBodega i : invs) {
                                    if (i.getIdBodega() != null && i.getIdBodega().equals(initialBodegaId)) {
                                        inv = i;
                                        break;
                                    }
                                }
                            }
                            if (inv == null) {
                                inv = invs.stream().findFirst().orElse(null);
                            }
                            if (inv != null) {
                                for (int i = 0; i < cbBodega.getItemCount(); i++) {
                                    if (cbBodega.getItemAt(i).id == inv.getIdBodega()) {
                                        cbBodega.setSelectedIndex(i);
                                        break;
                                    }
                                }
                                int stockPar = inv.getStockPar() != null ? inv.getStockPar() : 0;
                                int stockCaja = inv.getStockCaja() != null ? inv.getStockCaja() : 0;
                                if (initialType != null) {
                                    cbTipo.setSelectedItem(initialType);
                                    if (initialStock != null) {
                                        txtStock.setText(String.valueOf(initialStock));
                                    } else {
                                        if ("Caja".equalsIgnoreCase(initialType))
                                            txtStock.setText(String.valueOf(stockCaja));
                                        else
                                            txtStock.setText(String.valueOf(stockPar));
                                    }
                                } else {
                                    if (stockCaja > 0) {
                                        cbTipo.setSelectedItem("Caja");
                                        txtStock.setText(String.valueOf(stockCaja));
                                    } else {
                                        cbTipo.setSelectedItem("Par");
                                        txtStock.setText(String.valueOf(stockPar));
                                    }
                                }
                                if (inv.getUbicacionEspecifica() != null)
                                    txtEst.setText(inv.getUbicacionEspecifica());
                            } else {
                                if (initialType != null)
                                    cbTipo.setSelectedItem(initialType);
                                txtStock.setText(initialStock != null ? String.valueOf(initialStock) : "0");
                            }
                        } else {
                            if (initialType != null)
                                cbTipo.setSelectedItem(initialType);
                            txtStock.setText(initialStock != null ? String.valueOf(initialStock) : "0");
                            if (initialBodegaId != null) {
                                for (int i = 0; i < cbBodega.getItemCount(); i++) {
                                    if (cbBodega.getItemAt(i).id == initialBodegaId) {
                                        cbBodega.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                        }
                        if (imgBytes != null && imgBytes.length > 0) {
                            imagenSeleccionada[0] = imgBytes;
                            javax.swing.ImageIcon raw = new javax.swing.ImageIcon(imgBytes);
                            java.awt.Image scaled = raw.getImage().getScaledInstance(160, 120,
                                    java.awt.Image.SCALE_SMOOTH);
                            imgPreview.setIcon(new javax.swing.ImageIcon(scaled));
                        }
                    }
                    lblLoading.setVisible(false);

                    java.util.List<IdName> allColores = new java.util.ArrayList<>(colores);
                    configurarBuscadorColor(cbColor, allColores);
                } catch (Exception ex) {
                    showError("Error cargando datos", ex);
                }
            }
        }.execute();

        form.add(lblInfo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblProveedor, gbc);
        gbc.gridx = 1;
        form.add(cbProveedor, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblTipo, gbc);
        gbc.gridx = 1;
        form.add(cbTipo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblColor, gbc);
        gbc.gridx = 1;
        form.add(cbColor, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        form.add(lblSugerencia, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblTalla, gbc);
        gbc.gridx = 1;
        form.add(cbTalla, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblPrecioCompra, gbc);
        gbc.gridx = 1;
        form.add(txtPrecioCompra, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblPrecioVenta, gbc);
        gbc.gridx = 1;
        form.add(txtPrecioVenta, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblStock, gbc);
        gbc.gridx = 1;
        form.add(txtStock, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblBodega, gbc);
        gbc.gridx = 1;
        form.add(cbBodega, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblImagen, gbc);
        gbc.gridx = 1;
        form.add(btnImagen, gbc);
        gbc.gridx = 2;
        form.add(imgPreview, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblEst, gbc);
        gbc.gridx = 1;
        form.add(txtEst, gbc);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option(variantId != null ? "Guardar Cambios" : "Guardar Variante",
                        SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(form, variantId != null ? "Editar Variante" : "Nueva Variante", options,
                        (raven.modal.listener.ModalController mc, int opt) -> {
                            if (opt == SimpleModalBorder.OK_OPTION) {
                                try {
                                    IdName prov = (IdName) cbProveedor.getSelectedItem();
                                    IdName color = (IdName) cbColor.getSelectedItem();
                                    IdName talla = (IdName) cbTalla.getSelectedItem();
                                    IdName bodega = (IdName) cbBodega.getSelectedItem();
                                    String tipo = (String) cbTipo.getSelectedItem();
                                    int stockVal = 0;
                                    try {
                                        stockVal = Integer.parseInt(txtStock.getText().trim());
                                    } catch (Exception ignore) {
                                    }
                                    final int stock = stockVal;
                                    final String ubicacion = txtEst.getText() != null ? txtEst.getText().trim() : null;
                                    if (color == null || talla == null || prov == null || bodega == null || stock < 0) {
                                        showWarning("Complete color, talla, PROVEEDOR, bodega y stock");
                                        return;
                                    }

                                    int bodegaId = bodega.id;
                                    int stockPar = "Par".equalsIgnoreCase(tipo) ? stock : 0;
                                    int stockCaja = "Caja".equalsIgnoreCase(tipo) ? stock : 0;

                                    try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                                            .createConnection()) {
                                        try {
                                            con.setAutoCommit(false);

                                            try (java.sql.PreparedStatement pstPv = con.prepareStatement(
                                                    "UPDATE producto_variantes SET id_color=?, id_talla=?, id_proveedor=? WHERE id_variante=?")) {
                                                pstPv.setInt(1, color.id);
                                                pstPv.setInt(2, talla.id);
                                                pstPv.setInt(3, prov.id);
                                                pstPv.setInt(4, variantId);
                                                pstPv.executeUpdate();
                                            }

                                            try (java.sql.PreparedStatement pstInvUpd = con.prepareStatement(
                                                    "UPDATE inventario_bodega SET Stock_par=?, Stock_caja=?, ubicacion_especifica=?, fecha_ultimo_movimiento=NOW() WHERE id_variante=? AND id_bodega=?")) {
                                                pstInvUpd.setInt(1, stockPar);
                                                pstInvUpd.setInt(2, stockCaja);
                                                pstInvUpd.setString(3, ubicacion != null ? ubicacion : "");
                                                pstInvUpd.setInt(4, variantId);
                                                pstInvUpd.setInt(5, bodegaId);
                                                int rows = pstInvUpd.executeUpdate();
                                                if (rows == 0) {
                                                    try (java.sql.PreparedStatement pstInvIns = con.prepareStatement(
                                                            "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, activo, fecha_ultimo_movimiento, ubicacion_especifica) VALUES (?, ?, ?, ?, 0, 1, NOW(), ?)")) {
                                                        pstInvIns.setInt(1, bodegaId);
                                                        pstInvIns.setInt(2, variantId);
                                                        pstInvIns.setInt(3, stockPar);
                                                        pstInvIns.setInt(4, stockCaja);
                                                        pstInvIns.setString(5, ubicacion != null ? ubicacion : "");
                                                        pstInvIns.executeUpdate();
                                                    }
                                                }
                                            }

                                            if (imagenSeleccionada[0] != null && imagenSeleccionada[0].length > 0) {
                                                try (java.sql.PreparedStatement pstImg = con.prepareStatement(
                                                        "UPDATE producto_variantes SET imagen=? WHERE id_variante=?")) {
                                                    pstImg.setBytes(1, imagenSeleccionada[0]);
                                                    pstImg.setInt(2, variantId);
                                                    pstImg.executeUpdate();
                                                }
                                            }

                                            con.commit();
                                        } catch (Exception txEx) {
                                            try {
                                                con.rollback();
                                            } catch (Exception ignore) {
                                            }
                                            throw txEx;
                                        } finally {
                                            try {
                                                con.setAutoCommit(true);
                                            } catch (Exception ignore) {
                                            }
                                        }
                                    }

                                    showSuccess("Variante actualizada");
                                    mc.close();
                                } catch (Exception ex) {
                                    showError("Error al guardar", ex);
                                }
                            }
                        }));
    }

    private void configurarBuscadorColor(javax.swing.JComboBox<IdName> cbColor, java.util.List<IdName> allColores) {
        if (cbColor == null || allColores == null)
            return;
        cbColor.setEditable(true);
        java.awt.Component editorComponent = cbColor.getEditor().getEditorComponent();
        if (editorComponent instanceof javax.swing.JTextField) {
            javax.swing.JTextField txt = (javax.swing.JTextField) editorComponent;
            txt.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    String text = txt.getText();
                    String search = text != null ? text.trim().toLowerCase() : "";
                    cbColor.removeAllItems();
                    for (IdName color : allColores) {
                        String name = color != null && color.name != null ? color.name.toLowerCase() : "";
                        if (search.isEmpty() || name.contains(search)) {
                            cbColor.addItem(color);
                        }
                    }
                    cbColor.getEditor().setItem(text);
                    if (!cbColor.isPopupVisible()) {
                        cbColor.setPopupVisible(true);
                    }
                }
            });
        }
    }

    private final java.util.Set<String> skusGeneradosVariante = new java.util.LinkedHashSet<>();

    private String generarSKUVariante(String codigoModelo, String talla, String color, String tipo, int indice) {
        try {
            String cm = codigoModelo != null && !codigoModelo.trim().isEmpty() ? codigoModelo.trim() : "TEMP";
            String tallaCodigo = talla.replaceAll("\\s+", "").substring(0, Math.min(talla.length(), 3));
            String colorCodigo = color.substring(0, Math.min(color.length(), 3)).toUpperCase();
            String tipoCodigo = "Par".equalsIgnoreCase(tipo) ? "P" : "C";
            String baseSku = String.format("%s-%s-%s-%s-%03d", cm, tallaCodigo, colorCodigo, tipoCodigo, indice);
            return validarSKUUnicoVariante(baseSku);
        } catch (Exception e) {
            long timestamp = System.currentTimeMillis() % 1000000;
            return "SKU-" + indice + "-" + timestamp;
        }
    }

    private String validarSKUUnicoVariante(String baseSku) {
        String sku = baseSku;
        int contador = 1;
        raven.dao.ProductoVariantesDAO dao = new raven.dao.ProductoVariantesDAO();

        while (true) {
            boolean existsLocally = skusGeneradosVariante.contains(sku);
            boolean existsInDb = false;
            try {
                existsInDb = dao.checkSkuExists(sku);
            } catch (Exception e) {
                System.err.println("Error verificando SKU en DB: " + e.getMessage());
                // En caso de error de conexión, asumimos que existe para forzar un nuevo
                // intento
                // o podríamos romper el ciclo. Para seguridad, intentamos generar otro.
            }

            if (!existsLocally && !existsInDb) {
                break;
            }

            sku = baseSku + "-" + String.format("%02d", contador);
            contador++;

            // Si fallamos demasiadas veces, usar timestamp
            if (contador > 99) {
                long ts = System.currentTimeMillis() % 100000;
                sku = baseSku + "-" + ts;
                // Verificar el timestamp también por si acaso
                if (!skusGeneradosVariante.contains(sku)) {
                    try {
                        if (!dao.checkSkuExists(sku))
                            break;
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        skusGeneradosVariante.add(sku);
        return sku;
    }

    private void btn_PrductoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_PrductoActionPerformed
        showProductCreateFlow();
    }// GEN-LAST:event_btn_PrductoActionPerformed

    private void btn_PreciosActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_PreciosActionPerformed
        // EDICIÓN RÁPIDA DE PRECIOS DE VARIANTES
        List<ModelProduct> selectedProducts = getSelectedData();

        if (selectedProducts.isEmpty()) {
            abrirModalProductosSinPrecioVenta();
            return;
        }

        if (selectedProducts.size() > 1) {
            showWarning("Seleccione solo un producto para editar precios");
            return;
        }

        ModelProduct selectedProduct = selectedProducts.get(0);

        // Abrir el diálogo de edición rápida de precios
        try {
            EdicionRapidaPreciosDialog dialogo = new EdicionRapidaPreciosDialog(
                    (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                    selectedProduct.getProductId(),
                    selectedProduct.getName());
            dialogo.setVisible(true);

            // Si se guardaron cambios, recargar la tabla
            if (dialogo.isCambiosGuardados()) {
                loadCurrentPage();
                showSuccess("Precios actualizados correctamente");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error al abrir el editor de precios", e);
        }
    }// GEN-LAST:event_btn_PreciosActionPerformed

    private void btn_SinPrecioVentaActionPerformed(java.awt.event.ActionEvent evt) {
        abrirModalProductosSinPrecioVenta();
    }

    private void abrirModalProductosSinPrecioVenta() {
        javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.BorderLayout(0, 10));
        content.setOpaque(false);

        javax.swing.JLabel lblTitulo = new javax.swing.JLabel("Productos con variantes sin precio de venta (PV = 0)");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        javax.swing.JLabel lblEstado = new javax.swing.JLabel("Cargando...");
        lblEstado.putClientProperty(FlatClientProperties.STYLE, "foreground:lighten($Label.foreground,20%)");

        javax.swing.JPanel header = new javax.swing.JPanel(new java.awt.BorderLayout(10, 0));
        header.setOpaque(false);
        header.add(lblTitulo, java.awt.BorderLayout.WEST);
        header.add(lblEstado, java.awt.BorderLayout.EAST);

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] { "ID", "CÓDIGO", "PRODUCTO", "VARIANTES SIN PV" }) {
            Class[] types = new Class[] { Integer.class, String.class, raven.controlador.productos.ModelProduct.class,
                    Integer.class };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        javax.swing.JTable tabla = new javax.swing.JTable(model);
        tabla.setRowHeight(70);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:30;font:bold;");
        tabla.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines:true;intercellSpacing:0,1;");
        try {
            tabla.getColumnModel().getColumn(2)
                    .setCellRenderer(new raven.utils.ProductImageOptimizer.OptimizedProductRenderer());
        } catch (Exception ignore) {
        }
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tabla);
        sp.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        javax.swing.JLabel lblHint = new javax.swing.JLabel("Tip: doble clic sobre un producto para editar precios.");
        lblHint.putClientProperty(FlatClientProperties.STYLE, "foreground:lighten($Label.foreground,25%)");

        javax.swing.JPanel footer = new javax.swing.JPanel(new java.awt.BorderLayout());
        footer.setOpaque(false);
        footer.add(lblHint, java.awt.BorderLayout.WEST);

        content.add(header, java.awt.BorderLayout.NORTH);
        content.add(sp, java.awt.BorderLayout.CENTER);
        content.add(footer, java.awt.BorderLayout.SOUTH);

        java.util.function.Consumer<Boolean> cargar = (Boolean mostrarCargando) -> {
            javax.swing.SwingWorker<java.util.List<Object[]>, Void> worker = new javax.swing.SwingWorker<java.util.List<Object[]>, Void>() {
                @Override
                protected java.util.List<Object[]> doInBackground() throws Exception {
                    java.util.List<Object[]> rows = new java.util.ArrayList<>();
                    String sql = ""
                            + "SELECT p.id_producto, p.codigo_modelo, p.nombre, "
                            + "SUM(CASE WHEN pv.precio_venta IS NULL OR pv.precio_venta <= 0 THEN 1 ELSE 0 END) AS sin_pv "
                            + "FROM productos p "
                            + "INNER JOIN producto_variantes pv ON pv.id_producto = p.id_producto "
                            + "WHERE p.activo = 1 AND (pv.disponible = 1 OR pv.disponible IS NULL) "
                            + "GROUP BY p.id_producto, p.codigo_modelo, p.nombre "
                            + "HAVING sin_pv > 0 "
                            + "ORDER BY sin_pv DESC, p.nombre ASC "
                            + "LIMIT 500";

                    try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance()
                            .createConnection();
                            java.sql.PreparedStatement ps = con.prepareStatement(sql);
                            java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int idProducto = rs.getInt(1);
                            String codigo = rs.getString(2);
                            String nombre = rs.getString(3);
                            int sinPv = rs.getInt(4);
                            raven.controlador.productos.ModelProduct p = new raven.controlador.productos.ModelProduct();
                            p.setProductId(idProducto);
                            p.setName(nombre);
                            p.setModelCode(codigo);
                            rows.add(new Object[] { idProducto, codigo, p, sinPv });
                        }
                    }
                    return rows;
                }

                @Override
                protected void done() {
                    try {
                        java.util.List<Object[]> rows = get();
                        model.setRowCount(0);
                        for (Object[] r : rows) {
                            model.addRow(r);
                        }
                        lblEstado.setText("Total: " + rows.size());
                    } catch (Exception ex) {
                        lblEstado.setText("Error");
                        showError("No se pudo cargar el listado", ex);
                    }
                }
            };

            if (mostrarCargando) {
                lblEstado.setText("Cargando...");
            }
            worker.execute();
        };

        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    int viewRow = tabla.getSelectedRow();
                    int modelRow = tabla.convertRowIndexToModel(viewRow);
                    Integer idProducto = (Integer) model.getValueAt(modelRow, 0);
                    Object prodObj = model.getValueAt(modelRow, 2);
                    String nombre = (prodObj instanceof raven.controlador.productos.ModelProduct)
                            ? ((raven.controlador.productos.ModelProduct) prodObj).getName()
                            : String.valueOf(prodObj);
                    try {
                        EdicionRapidaPreciosDialog dialogo = new EdicionRapidaPreciosDialog(
                                (java.awt.Frame) javax.swing.SwingUtilities
                                        .getWindowAncestor(GestionProductosForm.this),
                                idProducto,
                                nombre);
                        dialogo.setVisible(true);
                        if (dialogo.isCambiosGuardados()) {
                            cargar.accept(false);
                            loadCurrentPage();
                        }
                    } catch (Exception ex) {
                        showError("Error al abrir el editor de precios", ex);
                    }
                }
            }
        });

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cerrar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Editar precios", SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(content, "Productos sin PV", options,
                        (raven.modal.listener.ModalController mc, int opt) -> {
                            if (opt == SimpleModalBorder.OPENED) {
                                cargar.accept(true);
                            } else if (opt == SimpleModalBorder.OK_OPTION) {
                                int viewRow = tabla.getSelectedRow();
                                if (viewRow < 0) {
                                    showWarning("Seleccione un producto");
                                    return;
                                }
                                int modelRow = tabla.convertRowIndexToModel(viewRow);
                                Integer idProducto = (Integer) model.getValueAt(modelRow, 0);
                                Object prodObj = model.getValueAt(modelRow, 2);
                                String nombre = (prodObj instanceof raven.controlador.productos.ModelProduct)
                                        ? ((raven.controlador.productos.ModelProduct) prodObj).getName()
                                        : String.valueOf(prodObj);
                                try {
                                    EdicionRapidaPreciosDialog dialogo = new EdicionRapidaPreciosDialog(
                                            (java.awt.Frame) javax.swing.SwingUtilities
                                                    .getWindowAncestor(GestionProductosForm.this),
                                            idProducto,
                                            nombre);
                                    dialogo.setVisible(true);
                                    if (dialogo.isCambiosGuardados()) {
                                        cargar.accept(false);
                                        loadCurrentPage();
                                    }
                                } catch (Exception ex) {
                                    showError("Error al abrir el editor de precios", ex);
                                }
                            }
                        }));
    }

    /**
     * Maneja el cambio de filtro en el combobox
     */
    private void cbxFiltroActionPerformed(java.awt.event.ActionEvent evt) {
        // Recargar la página actual con el nuevo filtro
        loadCurrentPage();
    }

    private static class IdName {
        int id;
        String name;

        IdName(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public String toString() {
            return name != null ? name : String.valueOf(id);
        }
    }

    private static class ProductInput {
        String nombre;
        IdName marca;
        IdName categoria;
        IdName proveedor;
        String genero;
        String descripcion;
        java.math.BigDecimal precioCompra;
        java.math.BigDecimal precioVenta;
    }

    private void showProductCreateFlow() {
        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(6, 8, 6, 8);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        javax.swing.JLabel lblNombre = new javax.swing.JLabel("Nombre");
        javax.swing.JTextField txtNombre = new javax.swing.JTextField(24);
        txtNombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre del producto");
        txtNombre.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblMarca = new javax.swing.JLabel("Marca");
        javax.swing.JComboBox<IdName> cbMarca = new javax.swing.JComboBox<>();
        cbMarca.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblCategoria = new javax.swing.JLabel("Categoría");
        javax.swing.JComboBox<IdName> cbCategoria = new javax.swing.JComboBox<>();
        cbCategoria.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblProveedor = new javax.swing.JLabel("Proveedor");
        javax.swing.JComboBox<IdName> cbProveedor = new javax.swing.JComboBox<>();
        cbProveedor.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblGenero = new javax.swing.JLabel("Género");
        javax.swing.JComboBox<String> cbGenero = new javax.swing.JComboBox<>(
                new String[] { "Hombre", "Mujer", "Niño", "Unisex" });
        cbGenero.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblDescripcion = new javax.swing.JLabel("Descripción");
        javax.swing.JTextField txtDescripcion = new javax.swing.JTextField(28);
        txtDescripcion.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Descripción corta");
        txtDescripcion.putClientProperty(FlatClientProperties.STYLE,
                "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblPC = new javax.swing.JLabel("Precio Compra");
        javax.swing.JTextField txtPC = new javax.swing.JTextField(10);
        txtPC.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0");
        txtPC.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JLabel lblPV = new javax.swing.JLabel("Precio Venta");
        javax.swing.JTextField txtPV = new javax.swing.JTextField(10);
        txtPV.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0");
        txtPV.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");

        try {
            java.util.List<raven.controlador.productos.ModelBrand> marcas = new raven.clases.productos.ServiceBrand()
                    .getAll();
            for (raven.controlador.productos.ModelBrand m : marcas)
                cbMarca.addItem(new IdName(m.getBrandId(), m.getName()));
        } catch (Exception ignore) {
        }
        try {
            java.util.List<raven.controlador.productos.ModelCategory> categorias = new raven.clases.productos.ServiceCategory()
                    .getAll();
            for (raven.controlador.productos.ModelCategory c : categorias)
                cbCategoria.addItem(new IdName(c.getCategoryId(), c.getName()));
        } catch (Exception ignore) {
        }
        try {
            java.util.List<raven.controlador.comercial.ModelSupplier> proveedores = new raven.clases.comercial.ServiceSupplier()
                    .getAll();
            for (raven.controlador.comercial.ModelSupplier p : proveedores)
                cbProveedor.addItem(new IdName(p.getSupplierId(), p.getName()));
        } catch (Exception ignore) {
        }

        form.add(lblNombre, gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblMarca, gbc);
        gbc.gridx = 1;
        form.add(cbMarca, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblCategoria, gbc);
        gbc.gridx = 1;
        form.add(cbCategoria, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblProveedor, gbc);
        gbc.gridx = 1;
        form.add(cbProveedor, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblGenero, gbc);
        gbc.gridx = 1;
        form.add(cbGenero, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblDescripcion, gbc);
        gbc.gridx = 1;
        form.add(txtDescripcion, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblPC, gbc);
        gbc.gridx = 1;
        form.add(txtPC, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(lblPV, gbc);
        gbc.gridx = 1;
        form.add(txtPV, gbc);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Siguiente", SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(form, "Nuevo Producto", options,
                        (raven.modal.listener.ModalController mc, int opt) -> {
                            if (opt == SimpleModalBorder.OK_OPTION) {
                                ProductInput data = new ProductInput();
                                data.nombre = txtNombre.getText();
                                data.marca = (IdName) cbMarca.getSelectedItem();
                                data.categoria = (IdName) cbCategoria.getSelectedItem();
                                data.proveedor = (IdName) cbProveedor.getSelectedItem();
                                data.genero = (String) cbGenero.getSelectedItem();
                                data.descripcion = txtDescripcion.getText();
                                try {
                                    data.precioCompra = new java.math.BigDecimal(txtPC.getText().trim());
                                } catch (Exception ex) {
                                    data.precioCompra = java.math.BigDecimal.ZERO;
                                }
                                try {
                                    data.precioVenta = new java.math.BigDecimal(txtPV.getText().trim());
                                } catch (Exception ex) {
                                    data.precioVenta = java.math.BigDecimal.ZERO;
                                }
                                showModelCodeModal(data);
                            }
                        }));
    }

    private String generateModelCode(ProductInput d) {
        try {
            if (d.marca == null || d.genero == null || d.genero.trim().isEmpty()) {
                throw new IllegalArgumentException("faltan datos");
            }
            String prefijo = obtenerPrefijoMarca(d.marca.name);
            String codGen = obtenerCodigoGenero(d.genero);
            int siguienteId = obtenerSiguienteIdProducto();
            return String.format("%s-%s-%03d", prefijo, codGen, siguienteId).toUpperCase();
        } catch (Throwable ex) {
            String prefijo = obtenerPrefijoMarca(d.marca != null ? d.marca.name : null);
            String codGen = obtenerCodigoGenero(d.genero);
            long ts = System.currentTimeMillis() % 9999;
            return String.format("%s-%s-%04d", prefijo, codGen, ts).toUpperCase();
        }
    }

    private String obtenerPrefijoMarca(String nombreMarca) {
        if (nombreMarca == null || nombreMarca.trim().isEmpty()) {
            return "PROD";
        }
        String marca = nombreMarca.trim().toUpperCase().replaceAll("\\s+", "");
        if (marca.length() >= 4) {
            return marca.substring(0, 4);
        } else {
            StringBuilder sb = new StringBuilder(marca);
            while (sb.length() < 4)
                sb.append('X');
            return sb.toString();
        }
    }

    private String obtenerCodigoGenero(String genero) {
        if (genero == null)
            return "X";
        String g = genero.toUpperCase();
        if ("MUJER".equals(g))
            return "M";
        if ("HOMBRE".equals(g))
            return "H";
        if ("NIÑO".equals(g))
            return "N";
        if ("UNISEX".equals(g))
            return "U";
        return "X";
    }

    private int obtenerSiguienteIdProducto() {
        try {
            int maxId = new raven.dao.ProductosDAO().getMaxProductId();
            return maxId + 1;
        } catch (Exception e) {
            return (int) (System.currentTimeMillis() % 9999) + 1000;
        }
    }

    private void showModelCodeModal(ProductInput data) {
        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 10));
        javax.swing.JLabel lbl = new javax.swing.JLabel("Código modelo");
        javax.swing.JTextField txtCodigo = new javax.swing.JTextField(12);
        txtCodigo.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:lighten($Menu.background,25%);");
        javax.swing.JButton btnGen = new javax.swing.JButton("Generar");
        btnGen.putClientProperty(FlatClientProperties.STYLE, "arc:18;background:#0A84FF;foreground:#fff;");
        btnGen.addActionListener(e -> txtCodigo.setText(generateModelCode(data)));
        if (txtCodigo.getText() == null || txtCodigo.getText().isEmpty())
            txtCodigo.setText(generateModelCode(data));
        form.add(lbl);
        form.add(txtCodigo);
        form.add(btnGen);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Crear Producto", SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(form, "Confirmar", options,
                        (raven.modal.listener.ModalController mc, int opt) -> {
                            if (opt == SimpleModalBorder.OK_OPTION) {
                                try {
                                    if (data.nombre == null || data.nombre.trim().isEmpty()) {
                                        Toast.show(this, Toast.Type.WARNING, "Nombre es requerido");
                                        return;
                                    }
                                    raven.controlador.productos.ModelProduct p = new raven.controlador.productos.ModelProduct();
                                    p.setModelCode(txtCodigo.getText() != null ? txtCodigo.getText().trim() : null);
                                    p.setName(data.nombre);
                                    p.setDescription(data.descripcion);
                                    if (data.categoria != null) {
                                        raven.controlador.productos.ModelCategory c = new raven.controlador.productos.ModelCategory();
                                        c.setCategoryId(data.categoria.id);
                                        p.setCategory(c);
                                    }
                                    if (data.marca != null) {
                                        raven.controlador.productos.ModelBrand m = new raven.controlador.productos.ModelBrand();
                                        m.setBrandId(data.marca.id);
                                        p.setBrand(m);
                                    }
                                    if (data.proveedor != null) {
                                        raven.controlador.comercial.ModelSupplier s = new raven.controlador.comercial.ModelSupplier();
                                        s.setSupplierId(data.proveedor.id);
                                        p.setSupplier(s);
                                    }
                                    p.setGender(data.genero);
                                    p.setPurchasePrice(data.precioCompra != null ? data.precioCompra.doubleValue() : 0);
                                    p.setSalePrice(data.precioVenta != null ? data.precioVenta.doubleValue() : 0);
                                    p.setMinStock(1);
                                    p.setUbicacion(null);
                                    p.setPairsPerBox(24);
                                    int id = new raven.dao.ProductosDAO().insert(p);
                                    if (id > 0) {
                                        Toast.show(this, Toast.Type.SUCCESS, "Producto creado");
                                        serviceOptimizedCache.clearCache();
                                        try {
                                            productAdapter.clearCache();
                                        } catch (Exception ignore) {
                                        }
                                        currentPage = 1;
                                        loadCurrentPage();
                                    } else {
                                        Toast.show(this, Toast.Type.ERROR, "No se pudo crear");
                                    }
                                } catch (Exception ex) {
                                    showError("Error al crear producto", ex);
                                }
                            }
                        }));
    }

    /**
     * OPTIMIZACIÓN: Método optimizado para obtener productos seleccionados.
     * Usa getSelectedRows() para mejor rendimiento cuando hay muchas selecciones.
     */
    private List<ModelProduct> getSelectedData() {
        List<ModelProduct> list = new ArrayList<>();
        
        // OPTIMIZACIÓN: Usar getSelectedRows() en lugar de iterar todas las filas
        int[] selectedRows = table.getSelectedRows();
        
        if (selectedRows.length > 0) {
            // Si hay filas seleccionadas explícitamente, usar esas
            for (int row : selectedRows) {
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < table.getRowCount()) {
                    Object val = table.getValueAt(modelRow, 2);
                    if (val instanceof ModelProduct) {
                        list.add((ModelProduct) val);
                    }
                }
            }
        } else {
            // Fallback: Recorrer todas las filas buscando checkboxes marcados
            for (int i = 0; i < table.getRowCount(); i++) {
                // Verifica si el checkbox está marcado (columna 0)
                Object checkboxValue = table.getValueAt(i, 0);
                if (checkboxValue instanceof Boolean && (Boolean) checkboxValue) {
                    // Obtiene el objeto ModelProduct de la columna 2 (NOMBRE)
                    Object val = table.getValueAt(i, 2);
                    if (val instanceof ModelProduct) {
                        list.add((ModelProduct) val);
                    }
                }
            }
        }

        return list;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LbBodega;
    private javax.swing.JButton btnCargarPorCaja;
    private javax.swing.JButton btn_Prducto;
    private javax.swing.JButton btn_Precios;
    private javax.swing.JButton btn_SinPrecioVenta;
    private javax.swing.JButton btn_Variable;
    private javax.swing.JButton btn_editar;
    private javax.swing.JButton btn_eliminar;
    private javax.swing.JButton btn_nuevo;
    private javax.swing.JComboBox<String> cbxFiltro;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;

    // End of variables declaration//GEN-END:variables
    private void btnCargarPorCajaActionPerformed(java.awt.event.ActionEvent evt) {
        List<ModelProduct> selectedProducts = getSelectedData();
        if (selectedProducts == null || selectedProducts.isEmpty()) {
            abrirModalCajasEnBodega();
            return;
        }
        if (selectedProducts.size() > 1) {
            showWarning("Seleccione solo un producto para cargar caja");
            return;
        }
        abrirDialogoCargarCaja(selectedProducts.get(0));
    }

    private void abrirDialogoCargarCaja(ModelProduct product) {
        java.util.List<CajaVarianteInventario> variantesCaja = obtenerVariantesCajaInventario(product.getProductId());
        if (variantesCaja.isEmpty()) {
            showWarning("Este producto no tiene stock de CAJA disponible");
            return;
        }

        CajaVarianteInventario cajaSeleccionada = null;

        // Selección explícita de la variante CAJA cuando hay más de una
        if (variantesCaja.size() == 1) {
            cajaSeleccionada = variantesCaja.get(0);
            showInfo("Se usará la CAJA color: "
                    + (cajaSeleccionada.colorNombre != null ? cajaSeleccionada.colorNombre : "N/A"));
        } else {
            // Construir opciones legibles por color y stock
            java.util.List<String> opciones = new java.util.ArrayList<>();
            for (CajaVarianteInventario v : variantesCaja) {
                String nombreColor = v.colorNombre != null ? v.colorNombre : ("Color #" + v.idColor);
                opciones.add(nombreColor + " (cajas: " + v.stockCaja + ")");
            }
            String seleccion = (String) javax.swing.JOptionPane.showInputDialog(
                    this,
                    "Seleccione la variante CAJA a convertir a pares",
                    "Seleccionar Caja",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones.toArray(new String[0]),
                    opciones.get(0));
            if (seleccion == null) {
                // Usuario canceló la selección
                showInfo("Conversión cancelada: no se seleccionó variante de CAJA");
                return;
            }
            int idx = opciones.indexOf(seleccion);
            if (idx >= 0) {
                cajaSeleccionada = variantesCaja.get(idx);
                showInfo("CAJA seleccionada: "
                        + (cajaSeleccionada.colorNombre != null ? cajaSeleccionada.colorNombre : "N/A"));
            }
        }

        if (cajaSeleccionada == null)
            return;

        // VALIDACIÓN: El proveedor es obligatorio para convertir cajas
        if (cajaSeleccionada.idProveedor == null || cajaSeleccionada.idProveedor <= 0) {
            showError(
                    "La caja seleccionada NO tiene proveedor asignado.\nDebe asignar un proveedor a la variante de caja antes de convertirla.",
                    null);
            return;
        }

        // Capture idProveedor for use in callback
        final Integer idProveedorFinal = cajaSeleccionada.idProveedor;
        final CajaVarianteInventario cajaFinal = cajaSeleccionada;

        CreateTallas.TallasFormCallback callback = (tallasSeleccionadas, tipoVenta, idVarianteCaja, imagenComun,
                cajasAConvertir) -> {
            try {
                // 1) Realizar conversión completa con registro de movimientos
                Integer idBodega = getCurrentBodegaId();
                if (idBodega == null && idVarianteCaja != null) {
                    idBodega = obtenerBodegaPreferidaParaCaja(idVarianteCaja);
                }

                System.out.println("INFO Iniciando conversión desde GestionProductos:");
                System.out.println("   - Producto ID: " + product.getProductId());
                System.out.println("   - Bodega: " + idBodega);
                System.out.println("   - Variante Caja: " + idVarianteCaja);
                System.out.println("   - Cajas a convertir: " + cajasAConvertir);
                System.out.println("   - Proveedor: " + idProveedorFinal);

                // Crear instancia temporal de CreateTallas para usar su método de conversión
                CreateTallas tempForm = new CreateTallas(product.getProductId(),
                        cajaFinal.idColor,
                        "caja",
                        null);
                tempForm.setDescontarCajaAlAceptar(true);
                if (imagenComun != null && imagenComun.length > 0) {
                    tempForm.setImagenParaNuevasVariantes(imagenComun);
                }

                // Llamar al método que registra movimientos en inventario_movimientos
                tempForm.convertirCajaAParesEnBodega(idBodega, idVarianteCaja,
                        tallasSeleccionadas, cajasAConvertir);

                System.out.println("SUCCESS Conversión completada con registro de movimientos");

                // 2) Abrir directamente Rotulación con las tallas seleccionadas
                RotulacionForm rotulo = new RotulacionForm();
                rotulo.cargarDesdeTallasSeleccionadas(tallasSeleccionadas);
                setRotuloModoPar(rotulo); // asegurar modo "Par" en el combo

                // 3) Navegar al formulario de Rotulación
                raven.application.Application.showForm(rotulo);

                // Opcional: si se desea imprimir automáticamente, descomentar la siguiente
                // línea
                // rotulo.imprimirSeleccionActual();

            } catch (Exception e) {
                System.err.println("ERROR Error en conversión: " + e.getMessage());
                e.printStackTrace();
                showError("Error en conversión de caja y rotulación", e);
            }
        };

        raven.application.form.productos.creates.CreateTallas form = new raven.application.form.productos.creates.CreateTallas(
                product.getProductId(),
                0, // color se seleccionará al buscar caja
                "caja",
                callback);
        // En Gestión de productos sí se descuenta 1 caja al convertir
        form.setDescontarCajaAlAceptar(true);
        form.setCajaSeleccionada(cajaSeleccionada.idVariante, cajaSeleccionada.idColor, cajaSeleccionada.colorNombre);

        // CONFIGURAR LÍMITE DE CAJAS SEGÚN STOCK DISPONIBLE
        form.setMaxCajasConvertibles(cajaSeleccionada.stockCaja);

        // Sin botones externos - CreateTallas maneja sus propios botones internamente
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {};

        ModalDialog.showModal(
                this,
                new SimpleModalBorder(form, "Cargar Caja: " + product.getName(), options,
                        (raven.modal.listener.ModalController mc, int option) -> {
                            if (option == SimpleModalBorder.OPENED) {
                                // Inyectar el controlador para que el panel cierre el modal correctamente
                                form.setModalController(mc);
                            }
                        }));
    }

    private void abrirModalCajasEnBodega() {
        Integer idBodega = getCurrentBodegaId();
        javax.swing.JDialog dlg = new javax.swing.JDialog();
        dlg.setTitle("Cajas disponibles en bodega");
        dlg.setModal(true);
        dlg.setSize(960, 600);
        dlg.setLocationRelativeTo(this);

        javax.swing.JPanel root = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
        javax.swing.JPanel top = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 10));
        javax.swing.JTextField txtFiltro = new javax.swing.JTextField(28);
        top.add(new javax.swing.JLabel("Filtrar:"));
        top.add(txtFiltro);
        root.add(top, java.awt.BorderLayout.NORTH);

        String[] cols = { "Seleccionar", "Producto", "Color", "Género", "Proveedor", "Stock" };
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return c == 0;
            }
        };
        javax.swing.JTable tabla = new javax.swing.JTable(model);
        tabla.setRowHeight(56);
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tabla);
        root.add(sp, java.awt.BorderLayout.CENTER);

        class ProductoCell {
            final String nombre;
            final javax.swing.Icon icon;

            ProductoCell(String n, javax.swing.Icon i) {
                nombre = n;
                icon = i;
            }
        }
        javax.swing.table.TableCellRenderer prodRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
                javax.swing.JLabel li = new javax.swing.JLabel();
                javax.swing.JLabel lt = new javax.swing.JLabel();
                lt.setFont(lt.getFont().deriveFont(java.awt.Font.BOLD, 14f));
                if (value instanceof ProductoCell) {
                    ProductoCell pc = (ProductoCell) value;
                    lt.setText(pc.nombre);
                    li.setIcon(pc.icon);
                    lt.setToolTipText(pc.nombre);
                } else {
                    lt.setText(value != null ? value.toString() : "");
                }
                p.add(li, java.awt.BorderLayout.WEST);
                p.add(lt, java.awt.BorderLayout.CENTER);
                if (isSelected) {
                    p.setBackground(table.getSelectionBackground());
                    lt.setForeground(table.getSelectionForeground());
                } else {
                    p.setBackground(table.getBackground());
                    lt.setForeground(table.getForeground());
                }
                return p;
            }
        };
        tabla.getColumnModel().getColumn(1).setCellRenderer(prodRenderer);

        javax.swing.table.TableCellRenderer btnRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JButton b = new javax.swing.JButton("Seleccionar");
                return b;
            }
        };
        tabla.getColumnModel().getColumn(0).setCellRenderer(btnRenderer);

        tabla.getColumnModel().getColumn(1).setPreferredWidth(300);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(80);

        java.util.List<int[]> datos = new java.util.ArrayList<>();
        java.util.Map<Integer, javax.swing.Icon> iconCache = new java.util.HashMap<>();
        java.util.concurrent.ExecutorService imageLoader = java.util.concurrent.Executors.newFixedThreadPool(3);

        java.util.function.Consumer<String> cargar = (term) -> {
            model.setRowCount(0);
            datos.clear();
            String texto = term != null ? term.trim().toLowerCase() : "";

            // PASO 1: Cargar SOLO datos (SIN imágenes) - RÁPIDO
            StringBuilder sql = new StringBuilder();
            sql.append(
                    "SELECT p.id_producto, p.nombre, p.genero, pv.id_variante, pv.id_color, pv.id_proveedor, c.nombre AS color_nombre, pr.nombre AS proveedor_nombre, ");
            sql.append("COALESCE(SUM(ib.Stock_caja),0) AS stock_caja ");
            sql.append("FROM inventario_bodega ib ");
            sql.append("INNER JOIN producto_variantes pv ON pv.id_variante=ib.id_variante AND pv.disponible=1 ");
            sql.append("INNER JOIN productos p ON p.id_producto=pv.id_producto AND p.activo=1 ");
            sql.append("LEFT JOIN colores c ON c.id_color=pv.id_color ");
            sql.append("LEFT JOIN proveedores pr ON pr.id_proveedor=pv.id_proveedor ");
            sql.append("WHERE ib.activo=1 ");
            if (idBodega != null)
                sql.append("AND ib.id_bodega=? ");
            if (!texto.isEmpty())
                sql.append(
                        "AND (LOWER(p.nombre) LIKE ? OR LOWER(COALESCE(p.codigo_modelo,'')) LIKE ? OR LOWER(COALESCE(c.nombre,'')) LIKE ?) ");
            sql.append("GROUP BY p.id_producto, pv.id_variante, pv.id_color, pv.id_proveedor, c.nombre, pr.nombre ");
            sql.append("HAVING stock_caja>0 ");
            sql.append("ORDER BY p.nombre ASC LIMIT 200");

            try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                    java.sql.PreparedStatement pst = con.prepareStatement(sql.toString())) {
                int idx = 1;
                if (idBodega != null)
                    pst.setInt(idx++, idBodega);
                if (!texto.isEmpty()) {
                    String like = "%" + texto + "%";
                    pst.setString(idx++, like);
                    pst.setString(idx++, like);
                    pst.setString(idx++, like);
                }
                try (java.sql.ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int idProd = rs.getInt("id_producto");
                        String nombre = rs.getString("nombre");
                        String genero = rs.getString("genero");
                        int idVar = rs.getInt("id_variante");
                        int idColor = rs.getInt("id_color");
                        int idProveedor = rs.getInt("id_proveedor");
                        String colorNombre = rs.getString("color_nombre");
                        String proveedorNombre = rs.getString("proveedor_nombre");
                        int stockCaja = rs.getInt("stock_caja");

                        // Primero agregar fila con icono desde caché o null
                        javax.swing.Icon icon = iconCache.get(idProd);
                        int rowIndex = model.getRowCount();
                        model.addRow(new Object[] { "Seleccionar", new ProductoCell(nombre, icon), colorNombre, genero,
                                proveedorNombre, stockCaja });
                        datos.add(new int[] { idProd, idVar, idColor, stockCaja, idProveedor });

                        // PASO 2: Si no hay icono en caché, cargar imagen ASÍNCRONAMENTE
                        if (icon == null) {
                            final int finalRow = rowIndex;
                            final int finalIdProd = idProd;
                            final String finalNombre = nombre;
                            imageLoader.submit(() -> {
                                try {
                                    javax.swing.Icon loadedIcon = cargarImagenProducto(finalIdProd);
                                    if (loadedIcon != null) {
                                        iconCache.put(finalIdProd, loadedIcon);
                                        javax.swing.SwingUtilities.invokeLater(() -> {
                                            if (finalRow < model.getRowCount()) {
                                                model.setValueAt(new ProductoCell(finalNombre, loadedIcon), finalRow,
                                                        1);
                                            }
                                        });
                                    }
                                } catch (Exception ignore) {
                                }
                            });
                        }
                    }
                }
            } catch (java.sql.SQLException ex) {
                model.setRowCount(0);
            }
        };

        final java.util.function.Consumer<String> cargarRef = cargar;
        final javax.swing.JTextField txtRef = txtFiltro;
        final javax.swing.Timer tmr = new javax.swing.Timer(250, e -> cargarRef.accept(txtRef.getText()));
        tmr.setRepeats(false);
        txtFiltro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (tmr.isRunning())
                    tmr.stop();
                tmr.start();
            }
        });

        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tabla.rowAtPoint(e.getPoint());
                int col = tabla.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 0) {
                    int[] d = datos.get(row);
                    int idProd = d[0];
                    int idVar = d[1];
                    int idColor = d[2];
                    int stockCaja = d[3];
                    int idProveedor = d[4];

                    if (idProveedor <= 0) {
                        showError(
                                "La caja seleccionada NO tiene proveedor asignado.\nDebe asignar un proveedor antes de convertirla.",
                                null);
                        return;
                    }

                    CreateTallas.TallasFormCallback cb = (tallas, tipoVenta, idVarCaja, imagenComun,
                            cajasAConvertir) -> {
                        try {
                            // Obtener bodega actual
                            Integer idBodega = getCurrentBodegaId();
                            if (idBodega == null && idVarCaja != null) {
                                idBodega = obtenerBodegaPreferidaParaCaja(idVarCaja);
                            }

                            System.out.println("INFO Conversión masiva - Registro de movimientos:");
                            System.out.println("   - Producto: " + idProd);
                            System.out.println("   - Bodega: " + idBodega);
                            System.out.println("   - Variante Caja: " + idVarCaja);

                            // Crear instancia temporal de CreateTallas para usar su método de conversión
                            CreateTallas tempForm = new CreateTallas(idProd, idColor, "caja", null);
                            tempForm.setDescontarCajaAlAceptar(true);
                            if (imagenComun != null && imagenComun.length > 0) {
                                tempForm.setImagenParaNuevasVariantes(imagenComun);
                            }

                            // Llamar al método que registra movimientos en inventario_movimientos
                            tempForm.convertirCajaAParesEnBodega(idBodega, idVarCaja, tallas, cajasAConvertir);

                            System.out.println("SUCCESS Conversión completada con registro de movimientos");

                            int opt = javax.swing.JOptionPane.showOptionDialog(dlg,
                                    "Conversión completada. ¿Ir a rotulación ahora?", "Conversión",
                                    javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE,
                                    null, new Object[] { "Ir a rotulación", "Seguir convirtiendo" }, "Ir a rotulación");
                            if (opt == javax.swing.JOptionPane.YES_OPTION) {
                                dlg.dispose();
                                RotulacionForm rot = new RotulacionForm();
                                rot.cargarDesdeTallasSeleccionadas(tallas);
                                raven.application.Application.showForm(rot);
                            } else {
                                cargarRef.accept(txtRef.getText());
                            }
                        } catch (Exception ex) {
                            System.err.println("ERROR Error en conversión masiva: " + ex.getMessage());
                            ex.printStackTrace();
                            showError("Error en conversión", ex);
                        }
                    };
                    CreateTallas form = new CreateTallas(idProd, idColor, "caja", cb);
                    form.setDescontarCajaAlAceptar(true);
                    form.setCajaSeleccionada(idVar, idColor, (String) model.getValueAt(row, 2));
                    form.setMaxCajasConvertibles(stockCaja);
                    javax.swing.JDialog modalConv = new javax.swing.JDialog(dlg, "Convertir Caja", true);
                    modalConv.setSize(820, 640);
                    modalConv.setLocationRelativeTo(dlg);
                    javax.swing.JPanel container = new javax.swing.JPanel(new java.awt.BorderLayout());
                    container.add(form, java.awt.BorderLayout.CENTER);
                    javax.swing.JPanel south = new javax.swing.JPanel(
                            new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
                    javax.swing.JButton btnCancel = new javax.swing.JButton("Cancelar");
                    javax.swing.JButton btnOk = new javax.swing.JButton("Aceptar");
                    btnOk.addActionListener(ev -> {
                        try {
                            form.confirmarAceptar();
                            modalConv.dispose();
                        } catch (Throwable ignore) {
                        }
                    });
                    btnCancel.addActionListener(ev -> modalConv.dispose());
                    south.add(btnCancel);
                    south.add(btnOk);
                    container.add(south, java.awt.BorderLayout.SOUTH);
                    modalConv.setContentPane(container);
                    modalConv.setVisible(true);
                }
            }
        });

        dlg.setContentPane(root);
        cargar.accept("");
        dlg.setVisible(true);
    }

    /**
     * Método auxiliar para cargar imagen de un producto de forma asíncrona
     * 
     * @param idProducto ID del producto
     * @return Icon con la imagen redimensionada o null si no existe
     */
    private javax.swing.Icon cargarImagenProducto(int idProducto) {
        String sql = "SELECT pv.imagen FROM producto_variantes pv WHERE pv.id_producto = ? AND pv.imagen IS NOT NULL LIMIT 1";
        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idProducto);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    java.sql.Blob b = rs.getBlob("imagen");
                    if (b != null) {
                        java.awt.Image img = javax.imageio.ImageIO.read(b.getBinaryStream());
                        if (img != null) {
                            int h = 50;
                            int w = (int) (img.getWidth(null) * (h / (double) img.getHeight(null)));
                            return new javax.swing.ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private static class CajaVarianteInventario {
        Integer idVariante;
        Integer idColor;
        String colorNombre;
        int stockCaja;
        Integer idProveedor;
    }

    private java.util.List<CajaVarianteInventario> obtenerVariantesCajaInventario(int idProducto) {
        java.util.List<CajaVarianteInventario> list = new java.util.ArrayList<>();
        Integer idBodega = getCurrentBodegaId();
        String base = "SELECT pv.id_variante, pv.id_color, c.nombre AS color_nombre, pv.id_proveedor, COALESCE(SUM(ib.Stock_caja),0) AS stock_caja FROM producto_variantes pv "
                +
                "LEFT JOIN colores c ON pv.id_color=c.id_color " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.activo=1 WHERE pv.id_producto=?";
        String sql = base + (idBodega != null ? " AND ib.id_bodega=?" : "")
                + " GROUP BY pv.id_variante, pv.id_color, c.nombre, pv.id_proveedor HAVING stock_caja>0 ORDER BY stock_caja DESC";
        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            int idx = 1;
            pst.setInt(idx++, idProducto);
            if (idBodega != null)
                pst.setInt(idx++, idBodega);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    CajaVarianteInventario cv = new CajaVarianteInventario();
                    cv.idVariante = rs.getInt("id_variante");
                    cv.idColor = rs.getInt("id_color");
                    cv.colorNombre = rs.getString("color_nombre");
                    cv.stockCaja = rs.getInt("stock_caja");
                    cv.idProveedor = (Integer) rs.getObject("id_proveedor");
                    list.add(cv);
                }
            }
        } catch (java.sql.SQLException ignore) {
        }
        return list;
    }

    // Datos simples para la caja preseleccionada
    private static class CajaPreseleccionada {
        Integer idVariante;
        Integer idColor;
        String nombreColor;
    }

    /**
     * Obtiene la variante de CAJA con mayor stock para el producto dado.
     * Retorna su id_variante, id_color y nombre del color.
     */
    private CajaPreseleccionada obtenerCajaPreseleccionada(int idProducto) throws java.sql.SQLException {
        String sql = "SELECT pv.id_variante, pv.id_color, c.nombre AS color_nombre, COALESCE(SUM(ib.Stock_caja),0) AS cajas "
                +
                "FROM producto_variantes pv " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 " +
                "WHERE pv.id_producto = ? AND pv.disponible = 1 " +
                "GROUP BY pv.id_variante, pv.id_color, c.nombre " +
                "ORDER BY cajas DESC LIMIT 1";
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idProducto);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    CajaPreseleccionada cp = new CajaPreseleccionada();
                    cp.idVariante = rs.getInt("id_variante");
                    cp.idColor = rs.getInt("id_color");
                    cp.nombreColor = rs.getString("color_nombre");
                    return cp;
                }
            }
        }
        return null;
    }

    private javax.swing.JTable getRotuloTable(RotulacionForm rotulo) {
        try {
            java.lang.reflect.Field f = RotulacionForm.class.getDeclaredField("tablaProd");
            f.setAccessible(true);
            return (javax.swing.JTable) f.get(rotulo);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void setRotuloModoPar(RotulacionForm rotulo) {
        try {
            java.lang.reflect.Field f = RotulacionForm.class.getDeclaredField("cbxTipoRotulacion");
            f.setAccessible(true);
            javax.swing.JComboBox<?> cbx = (javax.swing.JComboBox<?>) f.get(rotulo);
            cbx.setSelectedIndex(2); // Par
        } catch (Exception ignore) {
        }
    }

    void actualizarStockPorConversion(int idProducto,
            List<raven.application.form.productos.creates.CreateTallas.TallaVariante> tallasSeleccionadas,
            Integer idVarianteCaja, int cajasAConvertir, Integer idProveedor) throws SQLException {
        Integer idBodega = getCurrentBodegaId();
        if (idBodega == null && idVarianteCaja != null) {
            idBodega = obtenerBodegaPreferidaParaCaja(idVarianteCaja);
        }
        Integer idColorConversion = null;
        if (idVarianteCaja != null) {
            idColorConversion = obtenerColorPorVariante(idVarianteCaja);
        }
        ServiceInventarioBodega invService = new ServiceInventarioBodega();
        boolean allOk = true;
        for (raven.application.form.productos.creates.CreateTallas.TallaVariante tv : tallasSeleccionadas) {
            Integer colorParaCrear = idColorConversion;
            if (colorParaCrear == null && tv.getIdVariante() > 0) {
                colorParaCrear = obtenerColorPorVariante(tv.getIdVariante());
            }
            if (colorParaCrear == null) {
                allOk = false;
                break;
            }
            int idVariante = tv.getIdVariante();
            if (idVariante <= 0) {
                idVariante = findOrCreateVariant(idProducto, tv.getTalla().getTallaId(), colorParaCrear, tv.getSku(),
                        tv.getEan(), tv.getPrecioVenta(), idProveedor);
                if (idVariante <= 0) {
                    allOk = false;
                    break;
                }
            } else {
                // Si la variante ya existe, asegurarnos de que tenga el proveedor asignado si
                // falta
                if (idProveedor != null && idProveedor > 0) {
                    actualizarProveedorVariante(idVariante, idProveedor);
                }
            }
            try {
                int bodegaTarget = (idBodega != null) ? idBodega : obtenerBodegaPreferidaParaCaja(idVariante);
                // Usar incrementarStock para no sobrescribir el stock de cajas si coinciden las
                // variantes
                // y para sumar al stock existente en lugar de reemplazarlo
                invService.incrementarStock(bodegaTarget, idVariante, Math.max(0, tv.getCantidadSeleccionada()), "Par");
            } catch (SQLException ex) {
                allOk = false;
                throw ex;
            }
        }
        if (allOk && idVarianteCaja != null) {
            if (cajasAConvertir <= 0)
                cajasAConvertir = 1;
            try (java.sql.Connection con = conexion.getInstance().createConnection();
                    java.sql.PreparedStatement psCheck = con.prepareStatement(
                            "SELECT COALESCE(Stock_caja,0) FROM inventario_bodega WHERE id_variante=? AND activo=1 ORDER BY Stock_caja DESC LIMIT 1");
                    java.sql.PreparedStatement pstCaja = con.prepareStatement(
                            "UPDATE inventario_bodega SET Stock_caja = Stock_caja - ?, fecha_ultimo_movimiento = NOW() WHERE id_variante = ? AND activo = 1 AND Stock_caja >= ? ORDER BY Stock_caja DESC LIMIT 1")) {
                psCheck.setInt(1, idVarianteCaja);
                try (java.sql.ResultSet rs = psCheck.executeQuery()) {
                    int stockCaja = rs.next() ? rs.getInt(1) : 0;
                    if (stockCaja < cajasAConvertir)
                        throw new SQLException("No hay cajas suficientes para convertir");
                }
                pstCaja.setInt(1, cajasAConvertir);
                pstCaja.setInt(2, idVarianteCaja);
                pstCaja.setInt(3, cajasAConvertir);
                pstCaja.executeUpdate();
            }
        }
    }

    int findOrCreateVariant(int idProducto, int idTalla, int idColor, String sku, String ean,
            java.math.BigDecimal precioVenta, Integer idProveedor) throws java.sql.SQLException {
        String qSel = "SELECT id_variante, id_proveedor FROM producto_variantes WHERE id_producto=? AND id_talla=? AND id_color=? LIMIT 1";
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement psSel = con.prepareStatement(qSel)) {
            psSel.setInt(1, idProducto);
            psSel.setInt(2, idTalla);
            psSel.setInt(3, idColor);
            try (java.sql.ResultSet rs = psSel.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id_variante");
                    int existingProv = rs.getInt("id_proveedor");

                    // Si la variante existe pero no tiene proveedor (0) y estamos asignando uno,
                    // actualizarlo
                    if (idProveedor != null && idProveedor > 0 && existingProv == 0) {
                        try (java.sql.PreparedStatement pstUpd = con.prepareStatement(
                                "UPDATE producto_variantes SET id_proveedor = ? WHERE id_variante = ?")) {
                            pstUpd.setInt(1, idProveedor);
                            pstUpd.setInt(2, existingId);
                            pstUpd.executeUpdate();
                        }
                    }
                    return existingId;
                }
            }
        }
        String sqlIns = "INSERT INTO producto_variantes (id_producto, id_talla, id_color, imagen, ean, sku, precio_compra, precio_venta, stock_minimo_variante, disponible, id_proveedor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)";
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sqlIns,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, idProducto);
            pst.setInt(2, idTalla);
            pst.setInt(3, idColor);
            pst.setNull(4, java.sql.Types.BLOB);
            if (ean != null && !ean.isEmpty())
                pst.setString(5, ean);
            else
                pst.setNull(5, java.sql.Types.VARCHAR);
            String skuVal = (sku != null && !sku.trim().isEmpty()) ? sku
                    : ("SKU-" + idProducto + "-" + idTalla + "-" + idColor);
            pst.setString(6, skuVal);
            pst.setNull(7, java.sql.Types.DECIMAL);
            if (precioVenta != null)
                pst.setBigDecimal(8, precioVenta);
            else
                pst.setNull(8, java.sql.Types.DECIMAL);
            pst.setInt(9, 0);
            if (idProveedor != null)
                pst.setInt(10, idProveedor);
            else
                pst.setNull(10, java.sql.Types.INTEGER);
            pst.executeUpdate();
            try (java.sql.ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        return 0;
    }

    private void actualizarProveedorVariante(int idVariante, int idProveedor) {
        String sqlCheck = "SELECT id_proveedor FROM producto_variantes WHERE id_variante = ?";
        String sqlUpdate = "UPDATE producto_variantes SET id_proveedor = ? WHERE id_variante = ?";

        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {

            psCheck.setInt(1, idVariante);
            try (java.sql.ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    int currentProv = rs.getInt("id_proveedor");
                    // Solo actualizar si no tiene proveedor (0) o es diferente (opcional, aqui
                    // forzamos si es 0)
                    // El requisito dice: "asigne correctamente el mismo proveedor... a todas las
                    // tallas"
                    // Si ya tiene uno diferente, ¿lo sobrescribimos?
                    // Asumamos que si viene de "Cargar Caja", queremos unificar.
                    // Pero para ser seguros, solo si es 0.
                    // Wait, the user said "garantizar que al seleccionar una caja se asigne
                    // correctamente el mismo proveedor... a todas las tallas".
                    // If I select a box with provider X, all variants should have provider X.
                    // Even if they had provider Y? Maybe. But definitely if they had 0.
                    // In the test, it was 0.

                    // if (currentProv == 0) {
                    try (java.sql.PreparedStatement psUpd = con.prepareStatement(sqlUpdate)) {
                        psUpd.setInt(1, idProveedor);
                        psUpd.setInt(2, idVariante);
                        psUpd.executeUpdate();
                    }
                    // }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error actualizando proveedor de variante: " + e.getMessage());
        }
    }

    private Integer obtenerBodegaPreferidaParaCaja(int idVariante) {
        String sql = "SELECT id_bodega FROM inventario_bodega WHERE id_variante=? AND activo=1 ORDER BY Stock_caja DESC LIMIT 1";
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idVariante);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (java.sql.SQLException ignore) {
        }
        return null;
    }

    // Obtiene id_color para una variante específica (necesario para cargar
    // CreateEtiqueta)
    private Integer obtenerColorPorVariante(int idVariante) {
        String sql = "SELECT id_color FROM producto_variantes WHERE id_variante = ?";
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idVariante);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_color");
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    // Convierte la selección de CreateEtiqueta.TallaVariante al tipo
    // CreateTallas.TallaVariante para RotulacionForm
    private java.util.List<raven.application.form.productos.creates.CreateTallas.TallaVariante> convertirEtiquetasATallas(
            java.util.List<raven.application.form.productos.creates.CreateEtiqueta.TallaVariante> tallasEtiq) {
        java.util.List<raven.application.form.productos.creates.CreateTallas.TallaVariante> result = new java.util.ArrayList<>();
        if (tallasEtiq == null)
            return result;
        for (raven.application.form.productos.creates.CreateEtiqueta.TallaVariante te : tallasEtiq) {
            raven.application.form.productos.creates.CreateTallas.TallaVariante ct = new raven.application.form.productos.creates.CreateTallas.TallaVariante(
                    te.getTalla(),
                    te.getIdVariante(),
                    te.getSku(),
                    te.getEan(),
                    te.getPrecioVenta(),
                    te.getStockPorPares(),
                    te.isDisponible(),
                    false,
                    null // Ubicacion especifica no disponible en etiquetas
            );
            ct.setCantidadSeleccionada(te.getCantidadSeleccionada());
            ct.setSeleccionada(te.isSeleccionada());
            result.add(ct);
        }
        return result;
    }

    // ================================================================
    // FILTRO DE PRODUCTOS: Todos / Pares / Cajas
    // Usa los totales en las columnas 8 (pares) y 9 (cajas)
    // ================================================================
    private void aplicarFiltroProductos(String tipo) {
        try {
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) table.getModel();
            javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel> sorter = (javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel>) table
                    .getRowSorter();

            if (sorter == null || sorter.getModel() != model) {
                sorter = new javax.swing.table.TableRowSorter<>(model);
                table.setRowSorter(sorter);
            }

            if (tipo == null || "Todos".equalsIgnoreCase(tipo)) {
                sorter.setRowFilter(null);
                return;
            }

            final int COL_PARES = 8;
            final int COL_CAJAS = 9;

            javax.swing.RowFilter<javax.swing.table.DefaultTableModel, Integer> filter;
            if ("Pares".equalsIgnoreCase(tipo)) {
                filter = new javax.swing.RowFilter<javax.swing.table.DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(
                            javax.swing.RowFilter.Entry<? extends javax.swing.table.DefaultTableModel, ? extends Integer> entry) {
                        Object v = entry.getValue(COL_PARES);
                        int val = 0;
                        if (v instanceof Number) {
                            val = ((Number) v).intValue();
                        } else {
                            try {
                                val = Integer.parseInt(String.valueOf(v));
                            } catch (Exception ignore) {
                            }
                        }
                        return val > 0;
                    }
                };
            } else if ("Cajas".equalsIgnoreCase(tipo)) {
                filter = new javax.swing.RowFilter<javax.swing.table.DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(
                            javax.swing.RowFilter.Entry<? extends javax.swing.table.DefaultTableModel, ? extends Integer> entry) {
                        Object v = entry.getValue(COL_CAJAS);
                        int val = 0;
                        if (v instanceof Number) {
                            val = ((Number) v).intValue();
                        } else {
                            try {
                                val = Integer.parseInt(String.valueOf(v));
                            } catch (Exception ignore) {
                            }
                        }
                        return val > 0;
                    }
                };
            } else {
                filter = null;
            }

            sorter.setRowFilter(filter);
        } catch (Exception ignore) {
            // Evitar romper la UI por excepciones de filtrado
        }
    }

    private void setupPaginationControls() {
        // Limpiar listeners previos para evitar duplicados
        for (java.awt.event.ActionListener al : btnFirstPage.getActionListeners())
            btnFirstPage.removeActionListener(al);
        for (java.awt.event.ActionListener al : btnPreviousPage.getActionListeners())
            btnPreviousPage.removeActionListener(al);
        for (java.awt.event.ActionListener al : btnNextPage.getActionListeners())
            btnNextPage.removeActionListener(al);
        for (java.awt.event.ActionListener al : btnLastPage.getActionListeners())
            btnLastPage.removeActionListener(al);
        for (java.awt.event.ActionListener al : cbxPageSize.getActionListeners())
            cbxPageSize.removeActionListener(al);
        for (java.awt.event.ActionListener al : txtGoToPage.getActionListeners())
            txtGoToPage.removeActionListener(al);

        btnFirstPage.addActionListener(e -> {
            System.out.println("CLICK: FirstPage");
            goToFirstPage();
        });
        btnPreviousPage.addActionListener(e -> {
            System.out.println("CLICK: PreviousPage");
            goToPreviousPage();
        });
        btnNextPage.addActionListener(e -> {
            System.out.println("CLICK: NextPage");
            goToNextPage();
        });
        btnLastPage.addActionListener(e -> {
            System.out.println("CLICK: LastPage");
            goToLastPage();
        });

        cbxPageSize.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "10", "25", "50" }));
        cbxPageSize.setSelectedItem(String.valueOf(pageSize));
        cbxPageSize.addActionListener(e -> changePageSize());
        txtGoToPage.addActionListener(e -> goToSpecificPage());

        // Listener para filtro de productos (Todos/Pares/Cajas)
        // Eliminar listeners previos para evitar duplicados si se llama múltiples veces
        for (java.awt.event.ActionListener al : cbxFiltro.getActionListeners()) {
            cbxFiltro.removeActionListener(al);
        }
        cbxFiltro.addActionListener(e -> {
            if (uiResetInProgress)
                return;
            currentPage = 1;
            serviceOptimizedCache.clearCache();
            productAdapter.clearCache();
            loadCurrentPage();
        });

        // Listener para Enter en el buscador (búsqueda inmediata)
        for (java.awt.event.ActionListener al : txtSearch.getActionListeners()) {
            txtSearch.removeActionListener(al);
        }
        txtSearch.addActionListener(e -> {
            if (searchTimer != null && searchTimer.isRunning()) {
                searchTimer.stop();
            }
            executeSearch();
        });

        updatePaginationControls();
    }

    /**
     * Configura los listeners del sidebar para sincronizar con la tabla principal
     */
    private void setupSidebarListeners() {
        if (sidebar == null) {
            System.err.println("WARNING: Sidebar no inicializado, no se pueden configurar listeners");
            return;
        }

        try {
            // 1. Sincronizar búsqueda del sidebar con búsqueda principal
            sidebar.getSearchField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    syncSidebarSearchWithMainSearch();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    syncSidebarSearchWithMainSearch();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    syncSidebarSearchWithMainSearch();
                }
            });

            // 2. Conectar filtros del sidebar con recarga de datos
            sidebar.getEstadoCombo().addActionListener(e -> {
                if (!uiResetInProgress) {
                    currentPage = 1;
                    loadCurrentPage();
                }
            });

            sidebar.getMarcaCombo().addActionListener(e -> {
                if (!uiResetInProgress) {
                    String selectedMarca = (String) sidebar.getMarcaCombo().getSelectedItem();
                    if (selectedMarca != null && !selectedMarca.equals("Todas")) {
                        // Actualizar la búsqueda del textfield principal con la marca seleccionada
                        txtSearch.setText(selectedMarca);
                    } else {
                        // Limpiar búsqueda si se selecciona "Todas"
                        if (!txtSearch.getText().isEmpty()) {
                            txtSearch.setText("");
                        }
                    }
                }
            });

            sidebar.getCategoriaCombo().addActionListener(e -> {
                if (!uiResetInProgress) {
                    // Por ahora, solo recarga - en futuro se puede implementar filtro complejo
                    currentPage = 1;
                    loadCurrentPage();
                }
            });

            // 3. Botón limpiar filtros
            sidebar.getClearFiltersButton().addActionListener(e -> {
                if (!uiResetInProgress) {
                    clearAllFilters();
                }
            });

            // 4. Cargar marcas y categorías en los combos del sidebar
            loadSidebarFiltersData();

            // 5. Inicializar métricas
            updateSidebarMetrics();

            System.out.println("SUCCESS Sidebar listeners configurados correctamente");

        } catch (Exception e) {
            System.err.println("ERROR configurando sidebar listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sincroniza el campo de búsqueda del sidebar con el campo principal
     */
    private void syncSidebarSearchWithMainSearch() {
        if (sidebar == null || uiResetInProgress)
            return;

        SwingUtilities.invokeLater(() -> {
            String sidebarSearch = sidebar.getSearchField().getText();
            if (!sidebarSearch.equals(txtSearch.getText())) {
                txtSearch.setText(sidebarSearch);
                scheduleDebouncedSearch();
            }
        });
    }

    /**
     * Limpia todos los filtros del sidebar y de la tabla
     */
    private void clearAllFilters() {
        uiResetInProgress = true;
        try {
            // Limpiar búsqueda
            txtSearch.setText("");
            sidebar.getSearchField().setText("");

            // Restablecer combos del sidebar
            sidebar.getEstadoCombo().setSelectedIndex(0); // "Todos"
            sidebar.getMarcaCombo().setSelectedIndex(0); // "Todas"
            sidebar.getCategoriaCombo().setSelectedIndex(0); // "Todas"

            // Restablecer filtro principal
            cbxFiltro.setSelectedIndex(0); // "Todos"

            // Recargar primera página
            currentPage = 1;

        } finally {
            uiResetInProgress = false;
            loadCurrentPage();
        }
    }

    /**
     * Carga las marcas y categorías en los combos del sidebar
     */
    private void loadSidebarFiltersData() {
        if (sidebar == null)
            return;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            java.util.List<String> marcas = new java.util.ArrayList<>();
            java.util.List<String> categorias = new java.util.ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                try (java.sql.Connection con = conexion.getInstance().createConnection()) {
                    // Cargar marcas
                    String sqlMarcas = "SELECT DISTINCT m.nombre FROM marcas m " +
                            "INNER JOIN productos p ON p.id_marca = m.id_marca " +
                            "WHERE p.activo = 1 ORDER BY m.nombre";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlMarcas);
                            java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            marcas.add(rs.getString(1));
                        }
                    }

                    // Cargar categorías
                    String sqlCategorias = "SELECT DISTINCT c.nombre FROM categorias c " +
                            "INNER JOIN productos p ON p.id_categoria = c.id_categoria " +
                            "WHERE p.activo = 1 ORDER BY c.nombre";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlCategorias);
                            java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            categorias.add(rs.getString(1));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Verificar excepciones

                    // Actualizar combos del sidebar
                    sidebar.setMarcas(marcas);
                    sidebar.setCategorias(categorias);

                    System.out.println("SUCCESS Cargados " + marcas.size() + " marcas y " +
                            categorias.size() + " categorías en sidebar");

                } catch (Exception ex) {
                    System.err.println("ERROR cargando filtros del sidebar: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private void loadFirstPage() {
        currentPage = 1;
        loadCurrentPage();
    }

    /**
     * Carga la página actual de productos (con o sin búsqueda)
     */
    private void loadCurrentPage() {
        final long startTs = System.currentTimeMillis();
        final long seq = pageSeq.incrementAndGet();

        String searchTerm = txtSearch.getText();
        searchTerm = (searchTerm != null) ? searchTerm.trim() : "";

        // Validar búsqueda mínima
        if (!searchTerm.isEmpty() && searchTerm.length() < 2) {
            hideLoading();
            isLoading = false;
            return;
        }

        // ESTABLECER ESTADO DE CARGA
        isLoading = true;
        updatePaginationControls(); // Actualizar botones inmediatamente

        // Obtener bodega del usuario
        final Integer bodegaId = getCurrentBodegaId();

        // Obtener el filtro seleccionado del combobox
        final String filtroSeleccionado = (String) cbxFiltro.getSelectedItem();

        final String tipoFiltro;
        if ("Pares".equals(filtroSeleccionado)) {
            tipoFiltro = "pares";
        } else if ("Cajas".equals(filtroSeleccionado)) {
            tipoFiltro = "cajas";
        } else {
            tipoFiltro = null; // Todos
        }

        // Declarar variables como efectivamente finales para usar en la clase interna
        final Integer finalBodegaId = bodegaId;
        final String finalSearchTerm = searchTerm;
        final String finalTipoFiltro = tipoFiltro;
        final int finalCurrentPage = currentPage;
        final int finalPageSize = pageSize;

        showLoading("Cargando página " + currentPage + "...");

        pageLoader.submit(() -> {
            try {
                GenericPaginationService.PagedResult<ModelProduct> result = serviceOptimizedCache.getPagedResult(
                        finalCurrentPage,
                        finalPageSize,
                        finalSearchTerm,
                        finalBodegaId,
                        finalTipoFiltro,
                        null, // categoryId
                        null, // brandId
                        null, // colorId
                        null // sizeId
                );
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (seq != pageSeq.get())
                        return; // Respuesta obsoleta
                    updateProductData(result);
                    long dur = System.currentTimeMillis() - startTs;
                    System.out.println("INFO [Paginacion] Página " + currentPage + " cargada en " + dur + " ms. Items: "
                            + (result != null ? result.getData().size() : 0));
                    hideLoading();
                    isLoading = false;
                    updatePaginationControls();
                });
            } catch (Exception ex) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (seq != pageSeq.get())
                        return;
                    showErrorSafely("Error cargando productos", ex);
                    hideLoading();
                    isLoading = false;
                    updatePaginationControls();
                });
            }
        });
    }

    // 4. OPTIMIZAR updateProductData()
    private void updateProductData(GenericPaginationService.PagedResult<ModelProduct> result) {
        if (result == null)
            return;

        // ACTUALIZAR TOTALES AL INICIO (Para asegurar consistencia aunque falle el
        // renderizado)
        totalRows = (int) result.getTotalCount();
        totalPages = result.getTotalPages();
        System.out.println("DEBUG: updateProductData - Rows: " + totalRows + ", Pages: " + totalPages);

        // PRESERVAR SELECCIÓN: Guardar IDs seleccionados antes de limpiar
        java.util.Set<Integer> selectedIds = new java.util.HashSet<>();
        try {
            List<ModelProduct> currentSelection = getSelectedData();
            for (ModelProduct p : currentSelection) {
                if (p != null)
                    selectedIds.add(p.getProductId());
            }
        } catch (Exception ignore) {
        }

        resetUIStateBeforeReload();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        java.util.List<ModelProduct> products = result.getData();
        int rowNum = (currentPage - 1) * pageSize + 1; // Corregir numeración global

        for (ModelProduct p : products) {
            try {
                Object[] rowData = createOptimizedTableRow(p, rowNum++);
                // RESTAURAR SELECCIÓN: Si el producto estaba seleccionado, marcarlo
                if (p != null && selectedIds.contains(p.getProductId())) {
                    rowData[0] = true;
                }
                model.addRow(rowData);
            } catch (Exception e) {
                System.err.println("Error renderizando fila de producto " + (p != null ? p.getProductId() : "null")
                        + ": " + e.getMessage());
                // Agregar fila vacía o placeholder para no romper la tabla
            }
        }

        // RESTAURAR SELECCIÓN VISUAL DE LA TABLA
        if (!selectedIds.isEmpty()) {
            try {
                javax.swing.ListSelectionModel selectionModel = table.getSelectionModel();
                selectionModel.clearSelection();
                for (int i = 0; i < table.getRowCount(); i++) {
                    Object val = table.getValueAt(i, 2);
                    if (val instanceof ModelProduct) {
                        ModelProduct p = (ModelProduct) val;
                        if (p != null && selectedIds.contains(p.getProductId())) {
                            selectionModel.addSelectionInterval(i, i);
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }

        // IMPORTANTE: Limpiar filtros del sorter local porque ya filtramos en BD
        if (sorter != null) {
            sorter.setRowFilter(null);
        }

        updateBodegaInfo();

        // OPTIMIZACIÓN: Iniciar carga secuencial de imágenes
        // Esto reduce drásticamente las conexiones a BD y mejora la velocidad
        if (!products.isEmpty()) {
            raven.utils.ProductImageOptimizer.attachLazyLoader(table, products);
            raven.utils.ProductImageOptimizer.prefetchAllForPage(table, products);
            raven.utils.ProductImageOptimizer.ImagePreviewHandler.attach(table);
        }

        // Actualizar información de paginación
        updatePaginationControls();

        // NUEVO: Actualizar sidebar con los productos cargados
        if (sidebar != null && products != null && !products.isEmpty()) {
            sidebar.setProducts(products);
            updateSidebarMetrics();
        }

        prefetchAdjacentPages();
    }

    // 5. OPTIMIZAR createOptimizedTableRow()
    private Object[] createOptimizedTableRow(ModelProduct product, int rowNumber) {
        String marcaNombre = (product.getBrand() != null) ? product.getBrand().getName() : "Sin marca";

        // USAR STOCK DIRECTO DEL PRODUCTO (ya viene calculado desde la query)
        int totalStockPares = product.getPairsStock();
        int totalStockCajas = product.getBoxesStock();

        // USAR COLORES Y TALLAS YA CONSOLIDADOS (vienen del query)
        String coloresDisponibles = (product.getColor() != null && !product.getColor().isEmpty())
                ? product.getColor()
                : "Sin variantes";
        String tallasDisponibles = (product.getSize() != null && !product.getSize().isEmpty())
                ? product.getSize()
                : "Sin variantes";

        // CORRECCIÓN: Fallback a variantes si la información consolidada está vacía
        if ((coloresDisponibles.equals("Sin variantes") || tallasDisponibles.equals("Sin variantes"))
                && product.getVariants() != null && !product.getVariants().isEmpty()) {

            if (coloresDisponibles.equals("Sin variantes")) {
                coloresDisponibles = product.getVariants().stream()
                        .map(ModelProductVariant::getColorName)
                        .filter(color -> color != null && !color.trim().isEmpty())
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                if (coloresDisponibles.isEmpty())
                    coloresDisponibles = "Varios colores";
            }

            if (tallasDisponibles.equals("Sin variantes")) {
                tallasDisponibles = product.getVariants().stream()
                        .map(ModelProductVariant::getSizeName)
                        .filter(talla -> talla != null && !talla.trim().isEmpty())
                        .distinct()
                        .sorted(new java.util.Comparator<String>() {
                            @Override
                            public int compare(String s1, String s2) {
                                try {
                                    return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
                                } catch (NumberFormatException e) {
                                    return s1.compareTo(s2);
                                }
                            }
                        })
                        .collect(Collectors.joining(", "));
                if (tallasDisponibles.isEmpty())
                    tallasDisponibles = "Varias tallas";
            }
        }

        return new Object[] {
                false, // SELECT
                rowNumber, // N°
                product, // NOMBRE (objeto completo)
                product.getProductId(), // ID
                product.getModelCode(), // MODELO
                marcaNombre, // MARCA
                coloresDisponibles, // COLORES (ya consolidados)
                tallasDisponibles, // TALLAS (ya consolidadas)
                totalStockPares, // STOCK PARES
                totalStockCajas // STOCK CAJAS
        };
    }

    private void goToFirstPage() {
        System.out.println("DEBUG: goToFirstPage - Current: " + currentPage + ", Total: " + totalPages + ", Loading: "
                + isLoading);
        if (currentPage > 1) {
            currentPage = 1;
            loadCurrentPage();
        }
    }

    private void goToPreviousPage() {
        System.out.println("DEBUG: goToPreviousPage - Current: " + currentPage + ", Total: " + totalPages
                + ", Loading: " + isLoading);
        if (currentPage > 1) {
            currentPage--;
            loadCurrentPage();
        }
    }

    private void goToNextPage() {
        System.out.println(
                "DEBUG: goToNextPage - Current: " + currentPage + ", Total: " + totalPages + ", Loading: " + isLoading);
        if (currentPage < totalPages) {
            currentPage++;
            loadCurrentPage();
        }
    }

    private void goToLastPage() {
        System.out.println(
                "DEBUG: goToLastPage - Current: " + currentPage + ", Total: " + totalPages + ", Loading: " + isLoading);
        if (currentPage < totalPages) {
            currentPage = totalPages;
            loadCurrentPage();
        }
    }

    private void goToSpecificPage() {
        try {
            int page = Integer.parseInt(txtGoToPage.getText().trim());
            System.out.println("DEBUG: goToSpecificPage - Target: " + page + ", Current: " + currentPage + ", Total: "
                    + totalPages + ", Loading: " + isLoading);
            if (page >= 1 && page <= totalPages && page != currentPage) {
                currentPage = page;
                loadCurrentPage();
                txtGoToPage.setText("");
            } else {
                showWarning("Página inválida. Rango: 1 - " + totalPages);
                txtGoToPage.setText(String.valueOf(currentPage));
            }
        } catch (NumberFormatException e) {
            showWarning("Ingrese un número de página válido");
            txtGoToPage.setText(String.valueOf(currentPage));
        }
    }

    private void changePageSize() {
        String size = (String) cbxPageSize.getSelectedItem();
        int newSize = Integer.parseInt(size);
        if (newSize != pageSize) {
            pageSize = newSize;
            currentPage = 1;
            loadCurrentPage();
        }
    }

    private void updatePaginationControls() {
        System.out.println("DEBUG: updatePaginationControls - Current: " + currentPage + ", Total: " + totalPages
                + ", Loading: " + isLoading);
        if (isLoading) {
            lblPageInfo.setText("Cargando...");
        }

        // MEJORA: Manejo explícito de 0 resultados para mejor UX
        if (totalRows == 0) {
            lblPageInfo.setText("0 productos encontrados");
            btnFirstPage.setEnabled(false);
            btnPreviousPage.setEnabled(false);
            btnNextPage.setEnabled(false);
            btnLastPage.setEnabled(false);
            txtGoToPage.setEnabled(false);
            applyNavButtonStyles(false, false);
        } else {
            int displayTotalPages = Math.max(1, totalPages);
            lblPageInfo.setText(
                    String.format("Página %d de %d (%d productos)", currentPage, displayTotalPages, totalRows));

            boolean canGoBack = currentPage > 1;
            boolean canGoForward = currentPage < displayTotalPages;

            btnFirstPage.setEnabled(canGoBack);
            btnPreviousPage.setEnabled(canGoBack);
            btnNextPage.setEnabled(canGoForward);
            btnLastPage.setEnabled(canGoForward);
            applyNavButtonStyles(canGoBack, canGoForward);

            txtGoToPage.setEnabled(displayTotalPages > 1);
        }

        if (!txtGoToPage.hasFocus()) {
            txtGoToPage.setText(String.valueOf(currentPage));
        }

        if (!cbxPageSize.getSelectedItem().equals(String.valueOf(pageSize))) {
            cbxPageSize.setSelectedItem(String.valueOf(pageSize));
        }
        savePaginationStateToPreferences(); // Persistir estado actual
    }

    private void applyNavButtonStyles(boolean canGoBack, boolean canGoForward) {
        String styleBlue = "arc:18; borderWidth:1; borderColor:darken(#0A84FF,10%); background:#0A84FF; foreground:#ffffff;";
        String styleGray = "arc:18; borderWidth:1; borderColor:darken(#8892a6,10%); background:#8892a6; foreground:#ffffff;";
        btnFirstPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                canGoBack ? styleBlue : styleGray);
        btnPreviousPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                canGoBack ? styleBlue : styleGray);
        btnNextPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                canGoForward ? styleBlue : styleGray);
        btnLastPage.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                canGoForward ? styleBlue : styleGray);
    }

    private void savePaginationStateToPreferences() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("productos.paginacion");
            prefs.putInt("currentPage", currentPage);
            prefs.putInt("pageSize", pageSize);
            String searchTerm = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
            prefs.put("searchTerm", searchTerm);
            String filtroSeleccionado = (String) cbxFiltro.getSelectedItem();
            prefs.put("filter", filtroSeleccionado != null ? filtroSeleccionado : "");
            Integer bodegaId = getCurrentBodegaId();
            prefs.putInt("bodegaId", bodegaId != null ? bodegaId : 0);
            prefs.put("deepLink", buildDeepLink());
        } catch (Exception ignore) {
        }
    }

    private void restorePaginationStateFromPreferences() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("productos.paginacion");
            int savedPageSize = prefs.getInt("pageSize", pageSize);
            int savedPage = prefs.getInt("currentPage", currentPage);
            String savedSearch = prefs.get("searchTerm", "");
            String savedFilter = prefs.get("filter", "");

            // Aplicar valores si tienen sentido
            if (savedPageSize == 10 || savedPageSize == 25 || savedPageSize == 50) {
                pageSize = savedPageSize;
                cbxPageSize.setSelectedItem(String.valueOf(pageSize));
            }
            if (savedPage >= 1) {
                currentPage = savedPage;
            }
            if (savedSearch != null) {
                txtSearch.setText(savedSearch);
            }
            if (savedFilter != null && !savedFilter.isEmpty()) {
                cbxFiltro.setSelectedItem(savedFilter);
            }
        } catch (Exception ignore) {
        }
    }

    private String buildDeepLink() {
        String searchTerm = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
        String filtroSeleccionado = (String) cbxFiltro.getSelectedItem();
        Integer bodegaId = getCurrentBodegaId();
        StringBuilder sb = new StringBuilder("productos?");
        sb.append("page=").append(currentPage);
        sb.append("&size=").append(pageSize);
        if (searchTerm != null && !searchTerm.isEmpty())
            sb.append("&q=").append(urlEncode(searchTerm));
        if (filtroSeleccionado != null && !filtroSeleccionado.isEmpty())
            sb.append("&filter=").append(urlEncode(filtroSeleccionado));
        if (bodegaId != null && bodegaId > 0)
            sb.append("&bodega=").append(bodegaId);
        return sb.toString();
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    private void prefetchAdjacentPages() {
        int next = currentPage + 1;
        int prev = currentPage - 1;
        Integer bodegaId = getCurrentBodegaId();
        String searchTerm = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
        String filtroSeleccionado = (String) cbxFiltro.getSelectedItem();
        String tipoFiltro = "Pares".equals(filtroSeleccionado) ? "pares"
                : ("Cajas".equals(filtroSeleccionado) ? "cajas" : null);
        if (next <= totalPages) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    serviceOptimizedCache.getPagedResult(next, pageSize, searchTerm, bodegaId, tipoFiltro, null, null,
                            null, null);
                    return null;
                }
            }.execute();
        }
        if (prev >= 1) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    serviceOptimizedCache.getPagedResult(prev, pageSize, searchTerm, bodegaId, tipoFiltro, null, null,
                            null, null);
                    return null;
                }
            }.execute();
        }
    }

    private Integer getCurrentBodegaId() {
        try {
            Integer bodegaId = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            // Fallback redundante eliminado porque sessionManager ya no existe
            return (bodegaId != null && bodegaId > 0) ? bodegaId : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateBodegaInfo() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    Integer idBodega = getCurrentBodegaId();
                    if (idBodega != null) {
                        raven.clases.admin.ServiceBodegas sb = new raven.clases.admin.ServiceBodegas();
                        raven.controlador.admin.ModelBodegas b = sb.obtenerPorId(idBodega);
                        int totalPares = service.getTotalPairsByBodega(idBodega);
                        return (b != null ? b.getNombre() : ("Bodega #" + idBodega)) + " • Pares: " + totalPares;
                    } else {
                        return "Todas las bodegas";
                    }
                } catch (Exception e) {
                    return "Bodega";
                }
            }

            @Override
            protected void done() {
                try {
                    LbBodega.setText(get());
                } catch (Exception ignore) {
                    LbBodega.setText("Bodega");
                }
            }
        }.execute();
    }

    private void scheduleDebouncedSearch() {
        if (searchTimer != null && searchTimer.isRunning()) {
            searchTimer.stop();
        }

        // REDUCIDO A 50ms PARA RESPUESTA "INMEDIATA" (según solicitud del usuario)
        // Se mantiene un mínimo delay para evitar bloqueos si se escribe extremadamente
        // rápido
        searchTimer = new javax.swing.Timer(50, e -> executeSearch());
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    /**
     * Ejecuta la búsqueda de forma segura y eficiente
     */
    private void executeSearch() {
        String searchTerm = txtSearch.getText();
        searchTerm = (searchTerm != null) ? searchTerm.trim() : "";

        // Detectar si cambió el término
        if (searchTerm.equals(lastSearchTerm)) {
            return;
        }

        lastSearchTerm = searchTerm;

        // Si está vacío, resetear
        if (searchTerm.isEmpty()) {
            resetSearch();
            return;
        }

        // Resetear a página 1 y cargar
        currentPage = 1;
        serviceOptimizedCache.clearCache();
        productAdapter.clearCache();
        loadCurrentPage();
    }

    /**
     * Parsea el término de búsqueda para extraer texto y color opcional
     * Formato: "nombre, color" o "nombre"
     * 
     * @return Array [nombre, colorName] (elementos pueden ser null)
     */
    private String[] parseSearchTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return new String[] { null, null };
        }

        if (term.contains(",")) {
            String[] parts = term.split(",", 2);
            String name = parts[0].trim();
            String color = parts[1].trim();

            return new String[] {
                    name.isEmpty() ? null : name,
                    color.isEmpty() ? null : color
            };
        }

        return new String[] { term.trim(), null };
    }

    /**
     * Resetea la búsqueda y vuelve a la primera página
     */
    private void resetSearch() {
        lastSearchTerm = "";
        currentPage = 1;
        loadCurrentPage();
    }

    /**
     * Limpia completamente el campo de búsqueda y recarga datos
     */
    public void clearSearch() {
        SwingUtilities.invokeLater(() -> {
            txtSearch.setText("");
            resetSearch();
        });
    }

    /**
     * Normaliza texto para comparaciones (quita acentos, convierte a minúsculas)
     */
    private static String normalizeText(String s) {
        if (s == null || s.trim().isEmpty())
            return "";

        // Normalizar y quitar acentos
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convertir a minúsculas y trim
        return normalized.toLowerCase().trim();
    }

    private javax.swing.JButton btn_historial;
}
