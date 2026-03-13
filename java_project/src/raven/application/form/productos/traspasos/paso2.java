package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.ServiceProductVariant;
import raven.componentes.impresion.DataSearch;
import raven.componentes.impresion.EventClick;
import raven.componentes.impresion.PanelSearch;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProductVariant;
import raven.modal.Toast;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import javax.swing.JOptionPane;
import raven.clases.productos.ProductoTraspasoItem;
import raven.clases.productos.TraspasoDatos;
import raven.controlador.productos.TraspasoController;
import raven.clases.admin.UserSession;
import raven.controlador.admin.SessionManager;
import raven.application.form.other.buscador.ProductoBuscadorPanel;
import raven.componentes.repository.ProductoRepository;
import raven.clases.service.ProductoBusquedaService;
import raven.application.form.other.buscador.dto.VarianteDTO;
import raven.clases.service.ProductoBusquedaOptimizadoService;
import raven.componentes.BuscadorProductoDialog;

/**
 *
 * @author lmog2
 */
public class paso2 extends javax.swing.JPanel {

    paso1 vpaso1;
    private DataSearch selectedProduct;
    private JPopupMenu menu; // Declaración correcta
    private final ServiceProduct service = new ServiceProduct();
    private ServiceProductVariant serviceVariant = new ServiceProductVariant();
    private TraspasoController controller;
    private boolean isLoadingColores = false;
    private boolean isLoadingTallas = false;
    private PanelSearch searchProd; // Para productos
    private ProductoBusquedaOptimizadoService servicioOptimizado;
    private static final String PANEL = "arc:35;background:$Login.background";
    private static final String Camposdetexto = "arc:15;background:lighten($Menu.background,25%)";
    private javax.swing.Timer debounceSearch;
    private final java.util.Map<String, java.util.List<DataSearch>> cacheBusqueda = new java.util.LinkedHashMap<String, java.util.List<DataSearch>>() {
        protected boolean removeEldestEntry(java.util.Map.Entry<String, java.util.List<DataSearch>> eldest) {
            return size() > 64;
        }
    };
    private long cacheTimestampMs = 0L;
    private static final long CACHE_TTL_MS = 10_000L;

    public paso2() {
        initComponents();
        init();
        initMenus();
        try {
            conexion.getInstance().connectToDatabase();
            javax.sql.DataSource ds = conexion.getInstance().getDataSource();
            ProductoRepository repo = new ProductoRepository(ds);
            servicioOptimizado = new ProductoBusquedaOptimizadoService(repo);
        } catch (Exception ignore) {
        }
    }

    public void setPaso2(paso1 vpaso1) {
        this.vpaso1 = vpaso1;

    }

    public paso2(TraspasoController controller) {
        this.controller = controller;
        initComponents();
        init();
        initMenus();
        cargarProductosExistentes();
        try {
            conexion.getInstance().connectToDatabase();
            javax.sql.DataSource ds = conexion.getInstance().getDataSource();
            ProductoRepository repo = new ProductoRepository(ds);
            servicioOptimizado = new ProductoBusquedaOptimizadoService(repo);
        } catch (Exception ignore) {
        }
    }

    private boolean validarControllerCompleto() {
        if (controller == null) {
            Toast.show(this, Toast.Type.ERROR, "Error: Sistema no inicializado");
            return false;
        }

        if (controller.getTraspasoActual() == null) {
            Toast.show(this, Toast.Type.ERROR, "Error: Traspaso no inicializado");
            return false;
        }

        if (!controller.tieneDatosBasicos()) {
            Toast.show(this, Toast.Type.WARNING, "Complete los datos del Paso 1 antes de agregar productos");
            return false;
        }

        return true;
    }

    public void setController(TraspasoController controller) {
        this.controller = controller;
        cargarProductosExistentes();
    }

    private void inicializarServiciosAsync() {
        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    conexion.getInstance().connectToDatabase();
                    javax.sql.DataSource ds = conexion.getInstance().getDataSource();
                    ProductoRepository repo = new ProductoRepository(ds);
                    servicioOptimizado = new ProductoBusquedaOptimizadoService(repo);
                } catch (Exception e) {
                    System.err.println("Error inicializando servicios de búsqueda: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    public void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panelProducto.putClientProperty(FlatClientProperties.STYLE, PANEL);
        table.putClientProperty(FlatClientProperties.STYLE, "" + "background:$Login.background;");
        txtSearchProd.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);

        // Configurar ícono SVG de lupa para el botón de búsqueda
        try {
            FlatSVGIcon iconoLupa = new FlatSVGIcon("raven/icon/svg/lupa.svg", 20, 20);
            BtnBuscar.setIcon(iconoLupa);
            BtnBuscar.setText(""); // Quitar el texto emoji
            BtnBuscar.setToolTipText("Buscar producto");
            BtnBuscar.putClientProperty(FlatClientProperties.STYLE,
                    "arc:10;" +
                            "borderWidth:1;" +
                            "focusWidth:0");
        } catch (Exception e) {
            System.err.println("Error al cargar ícono de lupa: " + e.getMessage());
            BtnBuscar.setText("Buscar");
        }

        configurarTablaEventos();
        setupKeyboardShortcuts();

        // Inicializar timer de debounce
        debounceSearch = new javax.swing.Timer(500, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ejecutarBusquedaAsincrona(txtSearchProd.getText().trim());
            }
        });
        debounceSearch.setRepeats(false);
    }

    private void setupKeyboardShortcuts() {
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();

        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "focusBarcodeSearch");
        actionMap.put("focusBarcodeSearch", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (txtSearchProd != null) {
                    txtSearchProd.requestFocusInWindow();
                    txtSearchProd.selectAll();
                }
            }
        });

        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                "openManualSearch");
        actionMap.put("openManualSearch", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Ctrl+Shift+F abre el NUEVO buscador avanzado solicitado
                abrirModalBusquedaAvanzada();
            }
        });
    }

    private void initMenus() {
        menu = new JPopupMenu();

        menu.setFocusable(false); // Impide que el popup robe foco

        searchProd = new PanelSearch();

        menu.add(searchProd);

        searchProd.addEventClick(new EventClick() {
            @Override
            public void itemClick(DataSearch data) {
                menu.setVisible(false);
                selectedProduct = data;

                // Usar el nuevo método para seleccionar el producto
                seleccionarProducto(data);

                // Cargar tallas y colores
                int idProducto = Integer.parseInt(data.getId_prod());
                cargarTallasProducto(idProducto);
                cargarColoresProducto(idProducto);

                jComboBox1.setSelectedIndex(0);
            }

            @Override
            public void itemRemove(Component com, DataSearch data) {
                searchProd.remove(com);
                menu.setPopupSize(menu.getWidth(), (searchProd.getItemSize() * 35) + 2);
                if (searchProd.getItemSize() == 0) {
                    menu.setVisible(false);
                }
            }
        });

        try {
            comboTalla.addActionListener(e -> verificarSeleccionVarianteContraBodegaOrigen());
            comboColor.addActionListener(e -> verificarSeleccionVarianteContraBodegaOrigen());
        } catch (Throwable ignore) {
        }

        try {
            // Restaurado a abrirModalBusquedaAnterior (el original) para el botón de lupa
            BtnBuscar.addActionListener(e -> abrirModalBusquedaAnterior());
        } catch (Throwable ignore) {
        }
    }
    // MÉTODO CORREGIDO - Actualiza tabla con datos del controller

    private void actualizarTablaProductos() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0); // Limpiar tabla

        if (controller != null && controller.getTraspasoActual() != null) {
            List<ProductoTraspasoItem> productos = controller.getTraspasoActual().getProductos();

            System.out.println("INFO Actualizando tabla con " + productos.size() + " productos");

            for (ProductoTraspasoItem producto : productos) {
                String nombreCompleto = producto.getNombreCompleto();

                // Calcular subtotal
                java.math.BigDecimal subtotalNum = producto.getPrecioUnitario() != null
                        ? producto.getPrecioUnitario()
                                .multiply(java.math.BigDecimal.valueOf(producto.getCantidadSolicitada()))
                        : java.math.BigDecimal.ZERO;
                
                java.math.BigDecimal precioF = producto.getPrecioUnitario() != null 
                        ? producto.getPrecioUnitario().setScale(2, java.math.RoundingMode.HALF_UP) 
                        : java.math.BigDecimal.ZERO.setScale(2);
                
                java.math.BigDecimal decf = java.math.BigDecimal.ZERO.setScale(2);
                java.math.BigDecimal subF = subtotalNum.setScale(2, java.math.RoundingMode.HALF_UP);

                model.addRow(new Object[] {
                        producto.getCodigoProducto() != null ? producto.getCodigoProducto() : "N/A",
                        nombreCompleto,
                        producto.getTipo(),
                        producto.getCantidadSolicitada(),
                        precioF,
                        decf, // Descuento
                        subF,
                        producto.getIdVariante() != null ? producto.getIdVariante() : 0
                });
            }

            System.out.println("SUCCESS Tabla actualizada exitosamente");
        }
    }

    private void cargarProductosExistentes() {
        if (controller != null && controller.getTraspasoActual() != null) {
            // Usar SwingWorker para procesar en segundo plano
            new javax.swing.SwingWorker<List<Object[]>, Void>() {
                @Override
                protected List<Object[]> doInBackground() throws Exception {
                    List<ProductoTraspasoItem> productos = controller.getTraspasoActual().getProductos();
                    List<Object[]> filas = new ArrayList<>(productos.size());

                    for (ProductoTraspasoItem producto : productos) {
                        String nombreCompleto = producto.getNombreCompleto();
                        // Calcular subtotal
                        java.math.BigDecimal subtotalNum = producto.getPrecioUnitario() != null
                                ? producto.getPrecioUnitario()
                                        .multiply(java.math.BigDecimal.valueOf(producto.getCantidadSolicitada()))
                                : java.math.BigDecimal.ZERO;

                        java.math.BigDecimal precioF = producto.getPrecioUnitario() != null 
                                ? producto.getPrecioUnitario().setScale(2, java.math.RoundingMode.HALF_UP) 
                                : java.math.BigDecimal.ZERO.setScale(2);
                        
                        java.math.BigDecimal decf = java.math.BigDecimal.ZERO.setScale(2);
                        java.math.BigDecimal subF = subtotalNum.setScale(2, java.math.RoundingMode.HALF_UP);

                        filas.add(new Object[] {
                                producto.getCodigoProducto() != null ? producto.getCodigoProducto() : "N/A",
                                nombreCompleto != null ? nombreCompleto : producto.getNombreProducto(),
                                producto.getTipo() != null ? producto.getTipo() : "par",
                                producto.getCantidadSolicitada() != null ? producto.getCantidadSolicitada() : 0,
                                precioF,
                                decf, // Descuento
                                subF,
                                producto.getIdVariante() != null ? producto.getIdVariante() : 0
                        });
                    }
                    return filas;
                }

                @Override
                protected void done() {
                    try {
                        List<Object[]> filas = get();
                        DefaultTableModel model = (DefaultTableModel) table.getModel();

                        // Optimización: Limpiar y agregar todo de una vez
                        model.setRowCount(0);
                        for (Object[] fila : filas) {
                            model.addRow(fila);
                        }

                        // Refrescar solo al final
                        table.revalidate();
                        table.repaint();

                    } catch (Exception e) {
                        System.err.println("Error al cargar productos en tabla: " + e.getMessage());
                    }
                }
            }.execute();

        } else {
            System.out.println("WARNING No hay controller o traspaso actual para cargar productos");
        }
    }

    public boolean verificarProductosCargados() {
        if (controller == null || controller.getTraspasoActual() == null) {
            return false;
        }

        List<ProductoTraspasoItem> productos = controller.getTraspasoActual().getProductos();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        boolean integridadOK = true;

        // Verificar que la cantidad en tabla coincida con la del controller
        if (model.getRowCount() != productos.size()) {
            System.out.println("WARNING Discrepancia: Tabla tiene " + model.getRowCount() +
                    " filas, controller tiene " + productos.size() + " productos");
            integridadOK = false;
        }

        // Verificar que todos los productos tengan datos válidos
        for (ProductoTraspasoItem producto : productos) {
            if (producto.getIdProducto() == null || producto.getIdProducto() <= 0) {
                System.out.println("WARNING Producto sin ID válido: " + producto.getNombreCompleto());
                integridadOK = false;
            }

            if (producto.getCantidadSolicitada() == null || producto.getCantidadSolicitada() <= 0) {
                System.out.println("WARNING Producto sin cantidad válida: " + producto.getNombreCompleto());
                integridadOK = false;
            }

            if (producto.getTipo() == null || producto.getTipo().trim().isEmpty()) {
                System.out.println("WARNING Producto sin tipo válido: " + producto.getNombreCompleto());
                integridadOK = false;
            }
        }

        if (integridadOK) {
            System.out.println("SUCCESS Integridad de productos verificada correctamente");
        } else {
            System.out.println("ERROR Problemas de integridad detectados en productos");
        }

        return integridadOK;
    }

    public void refrescarTablaProductos() {
        System.out.println("INFO Refrescando tabla de productos...");
        cargarProductosExistentes();
        verificarProductosCargados();
        validarProductosContraBodegaOrigenYBloquear();
    }

    // MÉTODO CORREGIDO - Remover producto del controller también
    private void removerProductoSeleccionado() {
        int filaSeleccionada = table.getSelectedRow();
        if (filaSeleccionada >= 0 && controller != null) {
            // Confirmar eliminación
            int opcion = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de que desea eliminar este producto del traspaso?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (opcion == JOptionPane.YES_OPTION) {
                boolean eliminado = controller.removerProducto(filaSeleccionada);

                if (eliminado) {
                    actualizarTablaProductos();
                    Toast.show(this, Toast.Type.SUCCESS, "Producto removido del traspaso");
                    System.out.println(
                            "INFO Producto eliminado. Total restante: " + controller.getCantidadTotalProductos());
                } else {
                    Toast.show(this, Toast.Type.ERROR, "No se pudo eliminar el producto");
                }
            }
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un producto para eliminar");
        }
    }

    private List<DataSearch> searchOptimizado(String search) {
        int limitData = 10;
        List<DataSearch> list = new ArrayList<>();

        if (controller == null || controller.getTraspasoActual().getIdBodegaOrigen() == null) {
            return searchGeneral(search, limitData);
        }

        Integer idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();

        if (search == null || search.trim().isEmpty()) {
            return obtenerSugerenciasBodega(idBodegaOrigen, limitData);
        }

        String textoBusqueda = search.trim();
        if (textoBusqueda.matches("\\d{8,20}")) {
            DataSearch productoPorEAN = buscarPorEANDirecto(textoBusqueda, idBodegaOrigen);
            if (productoPorEAN != null) {
                list.add(productoPorEAN);
                return list;
            }
        }

        if (textoBusqueda.matches("\\d{1,10}")) {
            try {
                int idProducto = Integer.parseInt(textoBusqueda);
                DataSearch productoPorID = buscarPorIDDirecto(idProducto, idBodegaOrigen);
                if (productoPorID != null) {
                    list.add(productoPorID);
                    return list;
                }
            } catch (NumberFormatException e) {
            }
        }

        return buscarConScoringOptimizado(textoBusqueda, idBodegaOrigen, limitData);
    }

    private DataSearch buscarPorEANDirecto(String ean, Integer idBodega) {
        String sql = "SELECT DISTINCT " +
                "    p.*, " +
                "    pv.id_variante, " +
                "    pv.ean, " +
                "    pv.sku, " +
                "    c.nombre AS color, " +
                "    t.numero AS talla, " +
                "    ib.Stock_par, " +
                "    ib.Stock_caja " +
                "FROM producto_variantes pv " +
                "INNER JOIN productos p ON p.id_producto = pv.id_producto " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "WHERE pv.ean = ? " +
                "  AND ib.id_bodega = ? " +
                "  AND ib.activo = 1 " +
                "  AND p.activo = 1 " +
                "  AND pv.disponible = 1 " +
                "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                "LIMIT 1";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, ean);
            stmt.setInt(2, idBodega);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return crearDataSearchDesdeResultSet(rs, ean);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cerrarRecursos(rs, stmt, conn);
        }
        return null;
    }

    private DataSearch buscarPorIDDirecto(int idProducto, Integer idBodega) {
        String sql = "SELECT DISTINCT " +
                "    p.*, " +
                "    MIN(pv.id_variante) AS id_variante, " +
                "    MIN(pv.ean) AS ean, " +
                "    MIN(pv.sku) AS sku, " +
                "    '' AS color, " +
                "    '' AS talla, " +
                "    SUM(ib.Stock_par) AS Stock_par, " +
                "    SUM(ib.Stock_caja) AS Stock_caja " +
                "FROM productos p " +
                "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                "WHERE p.id_producto = ? " +
                "  AND ib.id_bodega = ? " +
                "  AND ib.activo = 1 " +
                "  AND p.activo = 1 " +
                "  AND pv.disponible = 1 " +
                "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                "GROUP BY p.id_producto " +
                "LIMIT 1";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            stmt.setInt(2, idBodega);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return crearDataSearchDesdeResultSet(rs, String.valueOf(idProducto));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cerrarRecursos(rs, stmt, conn);
        }
        return null;
    }

    private List<DataSearch> buscarConScoringOptimizado(String texto, Integer idBodega, int limite) {
        List<DataSearch> list = new ArrayList<>();
        String textoLower = texto.toLowerCase().trim();
        String textoLike = "%" + textoLower + "%";
        String textoInicio = textoLower + "%";
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ");
        sql.append("    p.*, ");
        sql.append("    MIN(pv.id_variante) AS id_variante, ");
        sql.append("    MIN(pv.ean) AS ean, ");
        sql.append("    MIN(pv.sku) AS sku, ");
        sql.append("    '' AS color, ");
        sql.append("    '' AS talla, ");
        sql.append("    SUM(ib.Stock_par) AS Stock_par, ");
        sql.append("    SUM(ib.Stock_caja) AS Stock_caja, ");
        sql.append("    CASE ");
        sql.append("        WHEN LOWER(p.codigo_modelo) = ? THEN 100 ");
        sql.append("        WHEN LOWER(p.nombre) = ? THEN 95 ");
        sql.append("        WHEN LOWER(p.codigo_modelo) LIKE ? THEN 90 ");
        sql.append("        WHEN LOWER(p.nombre) LIKE ? THEN 85 ");
        sql.append("        WHEN LOWER(m.nombre) LIKE ? THEN 75 ");
        sql.append("        WHEN LOWER(p.genero) LIKE ? THEN 70 ");
        sql.append("        WHEN LOWER(pv.sku) LIKE ? THEN 65 ");
        sql.append("        ELSE 50 ");
        sql.append("    END AS relevancia ");
        sql.append("FROM productos p ");
        sql.append("INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto ");
        sql.append("INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante ");
        sql.append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("WHERE ib.id_bodega = ? ");
        sql.append("  AND ib.activo = 1 ");
        sql.append("  AND p.activo = 1 ");
        sql.append("  AND pv.disponible = 1 ");
        sql.append("  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) ");
        sql.append("  AND ( ");
        sql.append("      LOWER(p.codigo_modelo) LIKE ? ");
        sql.append("   OR LOWER(p.nombre) LIKE ? ");
        sql.append("   OR LOWER(p.descripcion) LIKE ? ");
        sql.append("   OR LOWER(m.nombre) LIKE ? ");
        sql.append("   OR LOWER(p.genero) LIKE ? ");
        sql.append("   OR LOWER(pv.sku) LIKE ? ");
        sql.append("   OR LOWER(pv.ean) LIKE ? ");
        sql.append("  ) ");
        sql.append("GROUP BY p.id_producto ");
        sql.append("ORDER BY relevancia DESC, p.nombre ASC ");
        sql.append("LIMIT ?");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql.toString());
            int idx = 1;
            stmt.setString(idx++, textoLower);
            stmt.setString(idx++, textoLower);
            stmt.setString(idx++, textoInicio);
            stmt.setString(idx++, textoInicio);
            stmt.setString(idx++, textoLike);
            stmt.setString(idx++, textoLike);
            stmt.setString(idx++, textoLike);
            stmt.setInt(idx++, idBodega);
            stmt.setString(idx++, textoLike); // p.codigo_modelo
            stmt.setString(idx++, textoLike); // p.nombre
            stmt.setString(idx++, textoLike); // p.descripcion
            stmt.setString(idx++, textoLike); // m.nombre
            stmt.setString(idx++, textoLike); // p.genero
            stmt.setString(idx++, textoLike); // pv.sku
            stmt.setString(idx++, textoLike); // pv.ean
            stmt.setInt(idx++, limite);
            rs = stmt.executeQuery();
            while (rs.next()) {
                DataSearch producto = crearDataSearchDesdeResultSet(rs, texto);
                if (producto != null) {
                    list.add(producto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cerrarRecursos(rs, stmt, conn);
        }
        return list;
    }

    private List<DataSearch> obtenerSugerenciasBodega(Integer idBodega, int limite) {
        List<DataSearch> list = new ArrayList<>();
        String sql = "SELECT DISTINCT " +
                "    p.*, " +
                "    MIN(pv.id_variante) AS id_variante, " +
                "    MIN(pv.ean) AS ean, " +
                "    MIN(pv.sku) AS sku, " +
                "    '' AS color, " +
                "    '' AS talla, " +
                "    SUM(ib.Stock_par) AS Stock_par, " +
                "    SUM(ib.Stock_caja) AS Stock_caja " +
                "FROM productos p " +
                "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                "WHERE ib.id_bodega = ? " +
                "  AND ib.activo = 1 " +
                "  AND p.activo = 1 " +
                "  AND pv.disponible = 1 " +
                "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                "GROUP BY p.id_producto " +
                "ORDER BY p.fecha_creacion DESC " +
                "LIMIT ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idBodega);
            stmt.setInt(2, limite);
            rs = stmt.executeQuery();
            while (rs.next()) {
                DataSearch producto = crearDataSearchDesdeResultSet(rs, "");
                if (producto != null) {
                    list.add(producto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cerrarRecursos(rs, stmt, conn);
        }
        return list;
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement stmt, Connection conn) {
        try {
            if (rs != null)
                rs.close();
        } catch (SQLException ignore) {
        }
        try {
            if (stmt != null)
                stmt.close();
        } catch (SQLException ignore) {
        }
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException ignore) {
        }
    }

    // NUEVO MÉTODO - Búsqueda general sin filtro de bodega
    private List<DataSearch> searchGeneral(String search, int limitData) {
        List<DataSearch> list = new ArrayList<>();

        if (search == null || search.trim().isEmpty()) {
            return getProductosSugeridos(limitData);
        }

        String s = search.trim();
        if (s.length() >= 8 && s.matches("\\d+")) {
            DataSearch productoPorEan = buscarProductoPorEanOBarcode(s);
            if (productoPorEan != null) {
                list.add(productoPorEan);
                return list;
            }
        }

        String sql = "SELECT p.*, MIN(pv.id_variante) AS id_variante, " // Un solo registro por producto
                + "CASE "
                + "    WHEN p.codigo_modelo LIKE ? THEN 1 "
                + "    WHEN p.nombre LIKE ? THEN 2 "
                + "    WHEN p.descripcion LIKE ? THEN 3 "
                + "    ELSE 4 "
                + "END as relevancia "
                + "FROM productos p "
                + "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                + "WHERE (LOWER(p.codigo_modelo) LIKE ? OR LOWER(p.nombre) LIKE ? OR "
                + "       LOWER(p.descripcion) LIKE ? OR LOWER(pv.ean) LIKE ? OR LOWER(pv.ean) LIKE ? OR LOWER(pv.sku) LIKE ?) "
                + "AND p.activo = 1 AND pv.disponible = 1 "
                + "GROUP BY p.id_producto "
                + "ORDER BY relevancia, p.nombre "
                + "LIMIT ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);

            String searchParam = "%" + search.toLowerCase() + "%";
            String exactStartParam = search.toLowerCase() + "%";

            stmt.setString(1, exactStartParam);
            stmt.setString(2, exactStartParam);
            stmt.setString(3, exactStartParam);
            stmt.setString(4, searchParam);
            stmt.setString(5, searchParam);
            stmt.setString(6, searchParam);
            stmt.setString(7, searchParam);
            stmt.setString(8, searchParam);
            stmt.setInt(9, limitData);

            rs = stmt.executeQuery();

            while (rs.next()) {
                DataSearch data = crearDataSearchDesdeResultSet(rs, search);
                if (data != null) {
                    list.add(data);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    // NUEVO MÉTODO - Productos sugeridos filtrados por bodega
    // NUEVO MÉTODO - Productos sugeridos filtrados por bodega
    private List<DataSearch> getProductosSugeridosPorBodega(int limit, Integer idBodega) {
        List<DataSearch> list = new ArrayList<>();
        String sql = "SELECT p.*, MIN(pv.id_variante) AS id_variante "
                + "FROM productos p "
                + "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                + "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 "
                + "WHERE p.activo = 1 AND pv.disponible = 1 "
                + "AND ib.id_bodega = ? "
                + "AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0) "
                + "GROUP BY p.id_producto ORDER BY p.fecha_creacion DESC "
                + "LIMIT ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idBodega);
            stmt.setInt(2, limit);
            rs = stmt.executeQuery();
            while (rs.next()) {
                DataSearch data = crearDataSearchDesdeResultSet(rs, "");
                if (data != null) {
                    list.add(data);
                }
            }
            System.out.println(" Sugerencias para bodega " + idBodega + ": " + list.size() + " productos");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
    // MÉTODO MEJORADO - Mensaje cuando no hay bodega seleccionada

    public void mostrarMensajeSinBodega() {
        txt_resulP.setText("Complete el Paso 1 para buscar productos");
        txt_resulP1.setText("Seleccione bodega de origen primero");
        ICON.setIcon(null);
        ICON.setText("PASO 1");

        // Deshabilitar búsqueda hasta completar paso 1
        txtSearchProd.setEnabled(false);
        btnAgregarProd.setEnabled(false);
    }

    // MÉTODO MEJORADO - Habilitar búsqueda cuando hay bodega
    public void habilitarBusqueda() {
        txtSearchProd.setEnabled(true);
        btnAgregarProd.setEnabled(true);
        txt_resulP.setText("NOMBRE - PRECIO:$$$");
        txt_resulP1.setText("Stock: Cajas:  - Pares:  | 24 pares/caja");
        ICON.setText("ICON");
    }

    // Agregar listener para detectar eventos de eliminar en la tabla
    // (Agregar este código en initComponents o configurarComponentes)
    private void configurarTablaEventos() {
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    removerProductoSeleccionado();
                }
            }
        });

        // También se puede agregar un menú contextual
        javax.swing.JPopupMenu popupMenu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem eliminarItem = new javax.swing.JMenuItem("Eliminar producto");
        eliminarItem.addActionListener(e -> removerProductoSeleccionado());
        popupMenu.add(eliminarItem);

        table.setComponentPopupMenu(popupMenu);
    }

    private void verificarVariantesDisponibles(int idProducto) {
        String sql = "SELECT pv.id_variante, c.nombre as color, t.numero as talla "
                + "FROM producto_variantes pv "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE pv.id_producto = ? AND pv.disponible = 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            rs = stmt.executeQuery();

            System.out.println("DEBUG Variantes disponibles para producto " + idProducto + ":");
            while (rs.next()) {
                System.out.println("   ID Variante: " + rs.getInt("id_variante")
                        + ", Color: '" + rs.getString("color")
                        + "', Talla: '" + rs.getString("talla") + "'");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    // CORRECCIÓN ADICIONAL: Validar que el controller esté completo antes de
    // agregar productos

    // =========================================================================
    // IMPORTACIÓN DE EXCEL
    // =========================================================================
    private void importarExcel() {
        if (!validarControllerCompleto()) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo para importar");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Datos (*.xlsx, *.csv)", "xlsx", "csv"));

        int seleccion = fileChooser.showOpenDialog(this);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            if (archivo.getName().toLowerCase().endsWith(".csv")) {
                procesarArchivoCSV(archivo);
            } else {
                procesarArchivoExcel(archivo);
            }
        }
    }

    private void procesarArchivoCSV(File archivo) {
        System.out.println("INFO Iniciando importación desde CSV: " + archivo.getName());
        Toast.show(this, Toast.Type.INFO, "Procesando archivo CSV en segundo plano...");

        // Deshabilitar UI durante la carga
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        btnAgregarProd.setEnabled(false);

        new javax.swing.SwingWorker<String, Integer>() {
            @Override
            protected String doInBackground() throws Exception {
                int productosAgregados = 0;
                int errores = 0;
                StringBuilder reporteErrores = new StringBuilder();

                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo))) {
                    String line;
                    int rowNum = 0;
                    while ((line = br.readLine()) != null) {
                        rowNum++;

                        // Reportar progreso
                        if (rowNum % 50 == 0)
                            publish(rowNum);

                        // Asumimos separador coma o punto y coma
                        String[] values = line.split("[,;]");

                        if (values.length < 2)
                            continue; // Línea vacía o inválida

                        // Columna A: Código
                        String codigo = values[0].trim().replace("\"", "");

                        // Saltar cabecera si parece texto
                        if (rowNum == 1 && codigo.equalsIgnoreCase("Codigo"))
                            continue;

                        // Columna B: Cantidad
                        String cantStr = values[1].trim().replace("\"", "");
                        int cantidad = 0;
                        try {
                            cantidad = Integer.parseInt(cantStr);
                        } catch (NumberFormatException e) {
                            cantidad = 0;
                        }

                        if (codigo.isEmpty())
                            continue;

                        if (cantidad <= 0) {
                            errores++;
                            reporteErrores.append("- Fila ").append(rowNum).append(": Cantidad inválida\n");
                            continue;
                        }

                        // Buscar y agregar
                        DataSearch producto = buscarProductoPorEanOBarcode(codigo);
                        if (producto != null) {
                            boolean exito = agregarProductoDesdeImportacion(producto, cantidad);
                            if (exito) {
                                productosAgregados++;
                            } else {
                                errores++;
                                reporteErrores.append("- Fila ").append(rowNum).append(": Error al agregar\n");
                            }
                        } else {
                            errores++;
                            reporteErrores.append("- Fila ").append(rowNum).append(": Producto no encontrado\n");
                        }
                    }

                    String resultado = "Importados: " + productosAgregados + "\nErrores: " + errores;
                    if (errores > 0)
                        resultado += "\n\nDetalle:\n" + reporteErrores.toString();
                    return resultado;

                } catch (IOException e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String resultado = get();
                    setCursor(java.awt.Cursor.getDefaultCursor());
                    btnAgregarProd.setEnabled(true);

                    actualizarTablaProductos();

                    if (resultado.startsWith("Error")) {
                        Toast.show(paso2.this, Toast.Type.ERROR, resultado);
                    } else {
                        if (resultado.contains("Errores: 0")) {
                            Toast.show(paso2.this, Toast.Type.SUCCESS, "Importación CSV completada.");
                        } else {
                            JOptionPane.showMessageDialog(paso2.this, resultado, "Reporte CSV",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void procesarArchivoExcel(File archivo) {
        System.out.println("INFO Iniciando importación desde Excel: " + archivo.getName());
        Toast.show(this, Toast.Type.INFO, "Procesando archivo Excel en segundo plano...");

        // Deshabilitar UI durante la carga
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        btnAgregarProd.setEnabled(false);

        new javax.swing.SwingWorker<String, Integer>() {
            @Override
            protected String doInBackground() throws Exception {
                int productosAgregados = 0;
                int errores = 0;
                StringBuilder reporteErrores = new StringBuilder();

                try (FileInputStream fis = new FileInputStream(archivo);
                        Workbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    Iterator<Row> rowIterator = sheet.iterator();

                    // Saltar cabecera
                    if (rowIterator.hasNext()) {
                        rowIterator.next();
                    }

                    int rowCount = 0;
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        rowCount++;

                        // Reportar progreso cada 10 filas
                        if (rowCount % 10 == 0)
                            publish(rowCount);

                        // Columna A: Código
                        Cell cellCodigo = row.getCell(0);
                        String codigo = obtenerValorCelda(cellCodigo);

                        // Columna B: Cantidad
                        Cell cellCantidad = row.getCell(1);
                        int cantidad = 0;
                        try {
                            String cantStr = obtenerValorCelda(cellCantidad);
                            if (cantStr != null && !cantStr.isEmpty()) {
                                cantidad = (int) Double.parseDouble(cantStr);
                            }
                        } catch (NumberFormatException e) {
                            cantidad = 0;
                        }

                        if (codigo == null || codigo.isEmpty())
                            continue;

                        if (cantidad <= 0) {
                            errores++;
                            reporteErrores.append("- Fila ").append(row.getRowNum() + 1)
                                    .append(": Cantidad inválida\n");
                            continue;
                        }

                        // Buscar producto (sigue siendo síncrono pero en background thread)
                        DataSearch producto = buscarProductoPorEanOBarcode(codigo);
                        if (producto != null) {
                            boolean exito = agregarProductoDesdeImportacion(producto, cantidad);
                            if (exito) {
                                productosAgregados++;
                            } else {
                                errores++;
                                reporteErrores.append("- Fila ").append(row.getRowNum() + 1)
                                        .append(": Error al agregar (Stock?)\n");
                            }
                        } else {
                            errores++;
                            reporteErrores.append("- Fila ").append(row.getRowNum() + 1)
                                    .append(": Producto no encontrado ").append(codigo).append("\n");
                        }
                    }

                    String resultado = "Importados: " + productosAgregados + "\nErrores: " + errores;
                    if (errores > 0)
                        resultado += "\n\nDetalle:\n" + reporteErrores.toString();
                    return resultado;

                } catch (IOException e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String resultado = get();
                    setCursor(java.awt.Cursor.getDefaultCursor());
                    btnAgregarProd.setEnabled(true);

                    // Actualizar tabla UI
                    actualizarTablaProductos();

                    if (resultado.startsWith("Error")) {
                        Toast.show(paso2.this, Toast.Type.ERROR, resultado);
                    } else {
                        if (resultado.contains("Errores: 0")) {
                            Toast.show(paso2.this, Toast.Type.SUCCESS, "Importación completada con éxito.");
                        } else {
                            JOptionPane.showMessageDialog(paso2.this, resultado, "Reporte de Importación",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private String obtenerValorCelda(Cell cell) {
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Convertir a entero si es un número entero (para códigos como EAN)
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private boolean agregarProductoDesdeImportacion(DataSearch producto, int cantidad) {
        try {
            ProductoTraspasoItem item = new ProductoTraspasoItem();
            item.setIdProducto(Integer.parseInt(producto.getId_prod()));

            String codigoProducto = producto.getEAN();
            if (codigoProducto == null || codigoProducto.trim().isEmpty()) {
                codigoProducto = producto.getSKU();
            }
            item.setCodigoProducto(codigoProducto);
            item.setEan(producto.getEAN());
            item.setSku(producto.getSKU());
            item.setNombreProducto(producto.getNombre());
            item.setCantidadSolicitada(cantidad);

            // Usar ID Variante directo si existe (prioridad máxima para importación exacta)
            if (producto.getIdVariante() > 0) {
                item.setIdVariante(producto.getIdVariante());
                item.setColor(producto.getColor());
                item.setTalla(producto.getTalla());
            } else {
                // Si no tiene variante directa (raro si viene de buscarProductoPorEanOBarcode),
                // intentar resolver
                // Esto podría pasar si el producto base se busca por SKU genérico
                System.out.println("WARNING Producto importado sin variante específica: " + producto.getNombre());
                return false; // Por seguridad, requerimos variante exacta para importación masiva
            }

            // Validar bodega origen
            Integer idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
            if (!existeInventarioActivoEnBodega(item.getIdVariante(), idBodegaOrigen)) {
                return false;
            }

            // Determinar tipo (par/caja)
            String tipo = determinarTipoVariantePorBodega(item.getIdProducto(), item.getIdVariante(), idBodegaOrigen);
            item.setTipo(tipo);

            // Validar stock
            int stockDisponible = obtenerStockEnBodegaOrigen(item.getIdProducto(), item.getIdVariante(), tipo);
            item.setStockDisponible(stockDisponible);

            if (stockDisponible < cantidad) {
                return false;
            }

            // Precio
            item.setPrecioUnitario(
                    producto.getPrecioVenta() != null ? producto.getPrecioVenta() : java.math.BigDecimal.ZERO);

            return controller.agregarProducto(item);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void seleccionarProducto(DataSearch producto) {
        seleccionarProducto(producto, false);
    }

    private DataSearch buscarProductoPorEanOBarcode(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            System.out.println("WARNING Código EAN/Barcode vacío o nulo");
            limpiarCamposProducto();
            return null;
        }

        System.out.println("SEARCHING Buscando producto con EAN/Barcode: " + codigo);

        Integer idBodegaOrigen = null;
        if (controller != null && controller.getTraspasoActual() != null) {
            idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
        }

        String baseSql = "SELECT p.*, pv.*, "
                + "c.nombre as color_nombre, t.numero as talla_nombre, "
                + "cat.nombre as categoria_nombre, m.nombre as marca_nombre, "
                + "prov.nombre as proveedor_nombre, "
                + "COALESCE(ib.Stock_par, 0) AS stock_por_pares, "
                + "COALESCE(ib.Stock_caja, 0) AS stock_por_cajas "
                + "FROM producto_variantes pv "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN categorias cat ON p.id_categoria = cat.id_categoria "
                + "LEFT JOIN marcas m ON p.id_marca = m.id_marca "
                + "LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor ";

        // Agregar JOIN con inventario_bodega si hay bodega origen
        if (idBodegaOrigen != null) {
            baseSql += "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1 ";
        } else {
            baseSql += "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 ";
        }

        baseSql += "WHERE (pv.ean = ? OR pv.sku = ?) AND pv.disponible = 1 AND p.activo = 1 ";
        String sql = baseSql + "LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);

            int paramIndex = 1;

            // Si hay bodega origen, es el primer parámetro (para el LEFT JOIN)
            if (idBodegaOrigen != null) {
                stmt.setInt(paramIndex++, idBodegaOrigen);
                System.out.println("INFO Filtrando por bodega origen: " + idBodegaOrigen);
            }

            // Luego el código (EAN o SKU) para el WHERE
            stmt.setString(paramIndex++, codigo);
            stmt.setString(paramIndex++, codigo);

            rs = stmt.executeQuery();

            if (rs.next()) {
                limpiarCamposProducto();

                // Datos del producto principal
                int idProducto = rs.getInt("id_producto");
                String codigoModelo = rs.getString("codigo_modelo");
                String nombre = rs.getString("nombre");
                String descripcion = rs.getString("descripcion");
                int idCategoria = rs.getInt("id_categoria");
                String categoriaNombre = rs.getString("categoria_nombre");
                int idMarca = rs.getInt("id_marca");
                String marcaNombre = rs.getString("marca_nombre");
                int idProveedor = rs.getInt("id_proveedor");
                String proveedorNombre = rs.getString("proveedor_nombre");
                BigDecimal precioCompra = rs.getBigDecimal("precio_compra");
                BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
                int stockMinimo = rs.getInt("stock_minimo");
                String genero = rs.getString("genero");
                boolean activo = rs.getBoolean("activo");
                Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
                String ubicacion = rs.getString("ubicacion");
                int paresPorCaja = rs.getInt("pares_por_caja");

                // CORRECCIÓN CRÍTICA: Obtener ID de variante específica
                int idVariante = rs.getInt("id_variante");
                String colorNombre = rs.getString("color_nombre");
                String tallaNombre = rs.getString("talla_nombre");
                String ean = rs.getString("ean");
                String sku = rs.getString("sku");
                if (sku == null || sku.trim().isEmpty()) {
                    sku = codigoModelo;
                }

                int stockPorCajas = rs.getInt("stock_por_cajas");
                int stockPorPares = rs.getInt("stock_por_pares");
                int stock = stockPorPares;

                Blob imagen = null;
                try {
                    imagen = rs.getBlob("imagen");
                } catch (SQLException e) {
                    System.out.println("WARNING No se pudo obtener la imagen directamente");
                    imagen = obtenerImagenVariante(idVariante);
                }

                String idpString = String.valueOf(idProducto);

                // CORRECCIÓN CRÍTICA: Crear DataSearch con ID de variante
                DataSearch data = new DataSearch(
                        idpString, ean, sku, nombre, colorNombre, tallaNombre, true,
                        descripcion, idCategoria, idMarca, idProveedor, precioCompra,
                        precioVenta, stock, stockMinimo, genero, activo, imagen,
                        fechaCreacion, stockPorCajas, stockPorPares, paresPorCaja,
                        idVariante // ESTE ES EL CAMPO CRÍTICO
                );

                System.out.println("SUCCESS Producto encontrado por código EAN/Barras: " + codigo);
                System.out.println("   ID Producto: " + idProducto + ", Nombre: " + nombre);
                System.out.println("   ID VARIANTE: " + idVariante + " <- CRITICO");
                System.out.println("   Variante: Color " + colorNombre + ", Talla " + tallaNombre);
                System.out.println("   Stock: " + stockPorPares + " pares, " + stockPorCajas + " cajas");

                return data;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error buscando por EAN/Barcode: " + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private Blob obtenerImagenVariante(int idVariante) {
        String sql = "SELECT imagen FROM producto_variantes "
                + "WHERE id_variante = ? AND disponible = 1 AND imagen IS NOT NULL";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idVariante);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBlob("imagen");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void limpiarCamposProducto() {
        // Limpiar campos de texto
        // No limpiamos txtSearchProd aquí para mantener el texto de búsqueda
        txt_resulP.setText("");
        txt_resulP1.setText("");
        txtCantidad.setText("");

        // Limpiar combos
        comboColor.setSelectedIndex(-1);
        comboTalla.setSelectedIndex(-1);

        // Limpiar imagen
        ICON.setIcon(null);
        ICON.setText("");

        // Actualizar estilo del botón de agregar
        actualizarEstiloBtnAgregar();

        // Ocultar menú de sugerencias si está visible
        if (menu.isVisible()) {
            menu.setVisible(false);
        }

        // Actualizar estilo del botón de agregar
        actualizarEstiloBtnAgregar();

        System.out.println("INFO Campos de producto limpiados");
    }

    private void actualizarEstiloBtnAgregar() {
        boolean seleccionCompleta = selectedProduct != null
                && comboColor.getSelectedIndex() > 0
                && comboTalla.getSelectedIndex() > 0
                && !txtCantidad.getText().trim().isEmpty();

        if (seleccionCompleta) {
            // Estilo activo - azul vivo
            btnAgregarProd.setBackground(new Color(0, 122, 255)); // Azul vivo
            btnAgregarProd.setForeground(Color.WHITE);
        } else {
            // Estilo inactivo - azul opaco/gris
            btnAgregarProd.setBackground(new Color(100, 149, 237, 150)); // Azul opaco
            btnAgregarProd.setForeground(new Color(240, 240, 240));
        }

        System.out.println("INFO Estilo del botón actualizado: " + (seleccionCompleta ? "Activo" : "Inactivo"));
    }

    private void cargarColoresYTallas() {
        try {
            // Verificar si los combos ya tienen elementos
            if (comboColor.getItemCount() == 0 || comboTalla.getItemCount() == 0) {
                System.out.println("Cargando colores y tallas en los combos");

                // Cargar colores
                Connection conn = conexion.getInstance().createConnection();
                String sqlColores = "SELECT nombre FROM colores ORDER BY nombre";
                PreparedStatement stmtColores = conn.prepareStatement(sqlColores);
                ResultSet rsColores = stmtColores.executeQuery();

                comboColor.removeAllItems();
                comboColor.addItem("Seleccionar"); // Ítem por defecto
                while (rsColores.next()) {
                    comboColor.addItem(rsColores.getString("nombre"));
                }

                // Cargar tallas
                String sqlTallas = "SELECT numero FROM tallas ORDER BY numero";
                PreparedStatement stmtTallas = conn.prepareStatement(sqlTallas);
                ResultSet rsTallas = stmtTallas.executeQuery();

                comboTalla.removeAllItems();
                comboTalla.addItem("Seleccionar"); // Ítem por defecto
                while (rsTallas.next()) {
                    comboTalla.addItem(rsTallas.getString("numero"));
                }

                // Cerrar recursos
                rsColores.close();
                stmtColores.close();
                rsTallas.close();
                stmtTallas.close();
                conn.close();

                System.out.println("Colores y tallas cargados correctamente");
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar colores y tallas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seleccionarProducto(DataSearch producto, boolean esBusquedaPorCodigo) {
        // Primero limpiamos todos los campos para evitar datos de productos anteriores
        limpiarCamposProducto();

        if (producto == null) {
            System.out.println("WARNING Error: Producto nulo en seleccionarProducto");
            return;
        }

        // Guardar el producto seleccionado en la variable global
        selectedProduct = producto;

        System.out.println("INFO Seleccionando producto: " + producto.getNombre()
                + " - Color: " + producto.getColor()
                + " - Talla: " + producto.getTalla()
                + " - ID Variante: " + producto.getIdVariante());

        // Ocultar menú de sugerencias
        menu.setVisible(false);

        // Actualizar campo de búsqueda con el nombre del producto
        txtSearchProd.setText(producto.getNombre());

        // Asegurar que los combos estén cargados antes de seleccionar
        cargarColoresYTallas();

        try {
            // Cargar colores y tallas específicos para este producto
            int idProducto = Integer.parseInt(producto.getId_prod());
            System.out.println("Cargando colores y tallas para producto ID: " + idProducto);
            cargarColoresProducto(idProducto);
            cargarTallasProducto(idProducto);

            // Esperar un momento para que los combos se actualicen
            SwingUtilities.invokeLater(() -> {
                try {
                    // Imprimir todos los elementos disponibles en los combos para depuración
                    System.out.println("Elementos en comboColor:");
                    for (int i = 0; i < comboColor.getItemCount(); i++) {
                        System.out.println("  [" + i + "] " + comboColor.getItemAt(i));
                    }

                    System.out.println("Elementos en comboTalla:");
                    for (int i = 0; i < comboTalla.getItemCount(); i++) {
                        System.out.println("  [" + i + "] " + comboTalla.getItemAt(i));
                    }

                    // Seleccionar color y talla en los combos usando el método mejorado
                    System.out.println("Seleccionando color: '" + producto.getColor() + "'");
                    seleccionarPorTextoExacto(comboColor, producto.getColor());

                    System.out.println("Seleccionando talla: '" + producto.getTalla() + "'");
                    seleccionarPorTextoExacto(comboTalla, producto.getTalla());

                    // Verificar que se hayan seleccionado correctamente
                    System.out.println("Color seleccionado: " + comboColor.getSelectedItem());
                    System.out.println("Talla seleccionada: " + comboTalla.getSelectedItem());

                    // Si no se seleccionaron correctamente, intentar de nuevo con el método
                    // original
                    if (comboColor.getSelectedIndex() <= 0) {
                        System.out.println("WARNING Reintentando selección de color con método alternativo");
                        seleccionarEnCombo(comboColor, producto.getColor());
                    }

                    if (comboTalla.getSelectedIndex() <= 0) {
                        System.out.println("WARNING Reintentando selección de talla con método alternativo");
                        seleccionarEnCombo(comboTalla, producto.getTalla());
                    }

                    // Actualizar datos de la variante después de seleccionar color y talla
                    if (comboColor.getSelectedIndex() > 0 && comboTalla.getSelectedIndex() > 0) {
                        System.out.println(
                                "SUCCESS Color y talla seleccionados correctamente, actualizando datos de variante");
                        actualizarDatosProductoCompleto();
                    }
                } catch (Exception e) {
                    System.out.println("WARNING Error en selección de color/talla: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.out.println("WARNING Error cargando colores y tallas: " + e.getMessage());
            e.printStackTrace();
        }

        // Actualizar información adicional
        txt_resulP.setText(producto.getNombre() + " - Precio: $" + producto.getPrecioVenta());

        Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                ? controller.getTraspasoActual().getIdBodegaOrigen()
                : null;
        int paresPorCaja = producto.getParesPorCaja() > 0 ? producto.getParesPorCaja() : 24;
        int stockPorCajas;
        int stockPorPares;
        if (idBodegaOrigen != null) {
            int[] stockBP = obtenerStockProductoPorBodega(Integer.parseInt(producto.getId_prod()), idBodegaOrigen);
            stockPorCajas = stockBP[0];
            stockPorPares = stockBP[1];
        } else {
            stockPorPares = producto.getStockPorPares();
            stockPorCajas = producto.getStockPorCajas();
        }
        int stockTotalPares = stockPorPares + (stockPorCajas * paresPorCaja);
        txt_resulP1.setText("Stock: Cajas: " + stockPorCajas + " - Pares: " + stockPorPares + " | Total: "
                + stockTotalPares + " pares (" + paresPorCaja + " pares/caja)");

        // Si es búsqueda por código de barras, bloquear los combos de color y talla
        // pero solo después de que se hayan seleccionado correctamente
        SwingUtilities.invokeLater(() -> {
            if (esBusquedaPorCodigo) {
                // Verificar que los combos tengan valores seleccionados antes de
                // deshabilitarlos
                if (comboColor.getSelectedIndex() > 0 && comboTalla.getSelectedIndex() > 0) {
                    comboColor.setEnabled(false);
                    comboTalla.setEnabled(false);
                    System.out.println("Combos de color y talla bloqueados por búsqueda por código");
                } else {
                    // Si no se seleccionaron correctamente, mantenerlos habilitados
                    comboColor.setEnabled(true);
                    comboTalla.setEnabled(true);
                    System.out.println("WARNING Combos mantenidos habilitados porque no se seleccionaron valores");
                }
            } else {
                comboColor.setEnabled(true);
                comboTalla.setEnabled(true);
                System.out.println("Combos de color y talla habilitados para búsqueda manual");
            }

            // Actualizar imagen si está disponible y es búsqueda por código o si ya se
            // seleccionó color y talla
            if (esBusquedaPorCodigo) {
                // Para búsqueda por código, mostrar imagen inmediatamente
                if (producto.getImagen() != null) {
                    System.out.println("Cargando imagen del producto (búsqueda por código)");
                    cargarImagenProducto(producto.getImagen());
                } else {
                    System.out.println("INFO El producto no tiene imagen");
                    ICON.setIcon(null);
                    ICON.setText("SIN IMAGEN");
                }
            } else if (comboColor.getSelectedIndex() > 0 && comboTalla.getSelectedIndex() > 0) {
                // Para búsqueda manual, mostrar imagen de la variante específica cuando se
                // hayan seleccionado color y talla
                try {
                    // Intentar cargar la imagen específica de la variante
                    String talla = (String) comboTalla.getSelectedItem();
                    String color = (String) comboColor.getSelectedItem();
                    int idProducto = Integer.parseInt(producto.getId_prod());

                    List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);
                    boolean imagenCargada = false;

                    for (ModelProductVariant variant : variants) {
                        if (variant.isAvailable()) {
                            String tallaVariant = construirNombreTalla(variant);
                            String colorVariant = variant.getColorName();

                            if (talla.equals(tallaVariant) && color.equals(colorVariant)) {
                                if (variant.hasImage()) {
                                    System.out.println("Cargando imagen de la variante específica");
                                    byte[] imageBytes = variant.getImageBytes();
                                    Blob imageBlob = new javax.sql.rowset.serial.SerialBlob(imageBytes);
                                    cargarImagenProducto(imageBlob);
                                    imagenCargada = true;
                                    break;
                                }
                            }
                        }
                    }

                    // Si no se encontró imagen de la variante, usar la imagen del producto
                    if (!imagenCargada) {
                        if (producto.getImagen() != null) {
                            System.out.println("Cargando imagen general del producto");
                            cargarImagenProducto(producto.getImagen());
                        } else {
                            System.out.println("INFO El producto no tiene imagen");
                            ICON.setIcon(null);
                            ICON.setText("SIN IMAGEN");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("WARNING Error cargando imagen de variante: " + e.getMessage());
                    if (producto.getImagen() != null) {
                        cargarImagenProducto(producto.getImagen());
                    } else {
                        ICON.setIcon(null);
                        ICON.setText("ERROR IMAGEN");
                    }
                }
            } else {
                // En búsqueda manual sin selección completa, mostrar mensaje
                ICON.setIcon(null);
                ICON.setText("Seleccione talla y color");
            }

            // Actualizar estilo del botón de agregar según si la selección está completa
            actualizarEstiloBtnAgregar();
        });

        // Establecer cantidad por defecto
        txtCantidad.setText("1");

        // Enfocar el campo de cantidad para facilitar la entrada
        txtCantidad.requestFocus();
        txtCantidad.selectAll();

        System.out.println("SUCCESS Producto seleccionado: " + producto.getNombre()
                + " (" + producto.getColor() + " - " + producto.getTalla() + ")");
    }

    private void cargarColoresProducto(int idProducto) {
        isLoadingColores = true;

        try {
            Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                    ? controller.getTraspasoActual().getIdBodegaOrigen()
                    : null;
            List<ModelProductVariant> variants = (idBodegaOrigen != null)
                    ? serviceVariant.getVariantsByProductAndWarehouse(idProducto, idBodegaOrigen)
                    : serviceVariant.getVariantsByProduct(idProducto);

            comboColor.removeAllItems();
            comboColor.addItem("Color");

            Set<String> coloresUnicos = new LinkedHashSet<>();

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable() && (variant.getStockPairs() > 0 || variant.getStockBoxes() > 0)) {
                    String color = variant.getColorName();
                    if (color != null && !color.trim().isEmpty()) {
                        coloresUnicos.add(color);
                    }
                }
            }

            for (String color : coloresUnicos) {
                comboColor.addItem(color);
            }

            System.out.println("Colores cargados para producto ID: " + idProducto
                    + " - Total: " + coloresUnicos.size());

            // Actualizar estilo del botón de agregar
            actualizarEstiloBtnAgregar();

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando colores del producto");
        } finally {
            isLoadingColores = false;
        }
    }

    private void cargarTallasProducto(int idProducto) {
        isLoadingTallas = true;

        try {
            Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                    ? controller.getTraspasoActual().getIdBodegaOrigen()
                    : null;
            List<ModelProductVariant> variants = (idBodegaOrigen != null)
                    ? serviceVariant.getVariantsByProductAndWarehouse(idProducto, idBodegaOrigen)
                    : serviceVariant.getVariantsByProduct(idProducto);

            comboTalla.removeAllItems();
            comboTalla.addItem("Talla");

            Set<String> tallasUnicas = new LinkedHashSet<>();

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable() && (variant.getStockPairs() > 0 || variant.getStockBoxes() > 0)) {
                    String talla = construirNombreTalla(variant);
                    if (talla != null && !talla.trim().isEmpty()) {
                        tallasUnicas.add(talla);
                    }
                }
            }

            for (String talla : tallasUnicas) {
                comboTalla.addItem(talla);
            }

            System.out.println("Tallas cargadas para producto ID: " + idProducto
                    + " - Total: " + tallasUnicas.size());

            // Actualizar estilo del botón de agregar
            actualizarEstiloBtnAgregar();

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando tallas del producto");
        } finally {
            isLoadingTallas = false;
        }
    }

    private void seleccionarPorTextoExacto(JComboBox combo, String texto) {
        if (combo == null || texto == null || texto.isEmpty()) {
            return;
        }

        // Imprimir todos los elementos del combo para depuración
        System.out.println("Elementos en combo " + combo.getName() + ":");
        for (int i = 0; i < combo.getItemCount(); i++) {
            System.out.println("  [" + i + "] " + combo.getItemAt(i));
        }

        // Estrategia 1: Buscar coincidencia exacta
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).toString();
            if (item.equals(texto)) {
                System.out.println("Coincidencia exacta encontrada para '" + texto + "' en índice " + i);
                combo.setSelectedIndex(i);
                combo.repaint(); // Forzar repintado del combo
                return;
            }
        }

        // Estrategia 2: Buscar coincidencia ignorando mayúsculas/minúsculas
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).toString();
            if (item.equalsIgnoreCase(texto)) {
                System.out.println("Coincidencia ignorando mayúsculas encontrada para '" + texto + "' en índice " + i);
                combo.setSelectedIndex(i);
                combo.repaint(); // Forzar repintado del combo
                return;
            }
        }

        // Estrategia 3: Buscar coincidencia parcial (contiene)
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).toString();
            if (item.toLowerCase().contains(texto.toLowerCase())) {
                System.out.println("Coincidencia parcial encontrada para '" + texto + "' en índice " + i);
                combo.setSelectedIndex(i);
                combo.repaint(); // Forzar repintado del combo
                return;
            }
        }

        // Si no se encontró, seleccionar el primer elemento no vacío
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).toString();
            if (!item.isEmpty() && !item.equals("Seleccione")) {
                System.out.println("Seleccionando primer elemento no vacío: '" + item + "' en índice " + i);
                combo.setSelectedIndex(i);
                combo.repaint(); // Forzar repintado del combo
                return;
            }
        }

        // Si todo falla, seleccionar el primer elemento
        if (combo.getItemCount() > 0) {
            System.out.println("Seleccionando primer elemento por defecto en índice 0");
            combo.setSelectedIndex(0);
            combo.repaint(); // Forzar repintado del combo
        }
    }

    private void seleccionarEnCombo(JComboBox combo, String valor) {
        if (valor == null || valor.isEmpty()) {
            combo.setSelectedIndex(0); // Seleccionar el primer ítem (por defecto)
            return;
        }

        System.out.println("Buscando valor en combo: '" + valor + "'");

        // Forzar actualización del combo antes de buscar
        combo.updateUI();

        // Primero intentar encontrar una coincidencia exacta
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).toString();
            if (item.equalsIgnoreCase(valor)) {
                System.out.println("Coincidencia exacta encontrada en índice: " + i);
                combo.setSelectedIndex(i);
                combo.repaint(); // Forzar repintado del combo
                return;
            }
        }

        // Si no hay coincidencia exacta, buscar coincidencia parcial
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).toString();
            if (item.toLowerCase().contains(valor.toLowerCase())
                    || valor.toLowerCase().contains(item.toLowerCase())) {
                System.out.println("Coincidencia parcial encontrada en índice: " + i);
                combo.setSelectedIndex(i);
                combo.repaint(); // Forzar repintado del combo
                return;
            }
        }

        // Si no se encuentra el valor, seleccionar el primer ítem
        System.out.println("No se encontró coincidencia para: '" + valor + "', seleccionando primer ítem");
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
            combo.repaint(); // Forzar repintado del combo
        }
    }

    private void actualizarDatosProductoCompleto() {
        if (selectedProduct == null) {
            return;
        }

        String tallaSeleccionada = comboTalla.getSelectedIndex() > 0
                ? (String) comboTalla.getSelectedItem()
                : null;
        String colorSeleccionado = comboColor.getSelectedIndex() > 0
                ? (String) comboColor.getSelectedItem()
                : null;

        if (tallaSeleccionada != null && colorSeleccionado != null) {
            // Cargar datos específicos de la variante seleccionada
            cargarDatosVarianteEspecifica(Integer.parseInt(selectedProduct.getId_prod()), tallaSeleccionada,
                    colorSeleccionado);

            // Mostrar la imagen solo cuando se han seleccionado tanto talla como color
            System.out.println("Talla y color seleccionados, mostrando imagen del producto");

            // Actualizar la imagen en la búsqueda manual
            if (comboColor.isEnabled() && comboTalla.isEnabled()) {
                try {
                    // Intentar cargar la imagen específica de la variante
                    List<ModelProductVariant> variants = serviceVariant
                            .getVariantsByProduct(Integer.parseInt(selectedProduct.getId_prod()));

                    for (ModelProductVariant variant : variants) {
                        if (variant.isAvailable()) {
                            String tallaVariant = construirNombreTalla(variant);
                            String colorVariant = variant.getColorName();

                            if (tallaSeleccionada.equals(tallaVariant) && colorSeleccionado.equals(colorVariant)) {
                                if (variant.hasImage()) {
                                    byte[] imageBytes = variant.getImageBytes();
                                    Blob imageBlob = new javax.sql.rowset.serial.SerialBlob(imageBytes);
                                    cargarImagenProducto(imageBlob);
                                    return;
                                }
                                break;
                            }
                        }
                    }

                    // Si no se encontró imagen específica, usar la imagen del producto
                    if (selectedProduct.getImagen() != null) {
                        cargarImagenProducto(selectedProduct.getImagen());
                    } else {
                        ICON.setIcon(null);
                        ICON.setText("SIN IMAGEN");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    ICON.setIcon(null);
                    ICON.setText("ERROR DE IMAGEN");
                }
            }
        } else {
            // Si no se han seleccionado ambos, mostrar mensaje
            ICON.setIcon(null);
            ICON.setText("Seleccione talla y color");
        }

        // Actualizar estilo del botón de agregar
        actualizarEstiloBtnAgregar();
    }

    private void cargarDatosVarianteEspecifica(int idProducto, String talla, String color) {
        try {
            Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                    ? controller.getTraspasoActual().getIdBodegaOrigen()
                    : null;
            List<ModelProductVariant> variants = (idBodegaOrigen != null)
                    ? serviceVariant.getVariantsByProductAndWarehouse(idProducto, idBodegaOrigen)
                    : serviceVariant.getVariantsByProduct(idProducto);

            ModelProductVariant varianteEncontrada = null;

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()) {
                    String tallaVariant = construirNombreTalla(variant);
                    String colorVariant = variant.getColorName();

                    if (talla.equals(tallaVariant) && color.equals(colorVariant)) {
                        varianteEncontrada = variant;
                        break;
                    }
                }
            }

            if (varianteEncontrada != null) {
                BigDecimal precioVenta = BigDecimal.valueOf(
                        varianteEncontrada.getSalePrice() != null
                                ? varianteEncontrada.getSalePrice()
                                : selectedProduct.getPrecioVenta().doubleValue());

                txt_resulP.setText(selectedProduct.getNombre() + " - Precio: $" + precioVenta);

                // Mostrar claramente la equivalencia: 1 caja = 24 pares
                int stockCajas = varianteEncontrada.getStockBoxes();
                int stockPares = varianteEncontrada.getStockPairs();
                int totalPares = stockPares + (stockCajas * 24); // 1 caja = 24 pares

                txt_resulP1.setText("Stock: Cajas: " + stockCajas
                        + " - Pares: " + stockPares
                        + " | Total: " + totalPares + " pares (1 caja = 24 pares)");

                // CORRECCIÓN: No intentar modificar selectedProduct directamente
                // En su lugar, crear variables temporales para usar en la venta
                // No cargar imagen aquí, se maneja en actualizarDatosProductoCompleto
                // para controlar cuándo mostrar la imagen según el tipo de búsqueda
                // Actualizar colores de stock
                actualizarVisualizacionStock();

            } else {
                Toast.show(this, Toast.Type.WARNING,
                        "No se encontró stock disponible para la combinación seleccionada");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando datos del producto");
        }
    }

    private void actualizarVisualizacionStock() {
        if (selectedProduct != null
                && comboTalla.getSelectedIndex() > 0
                && comboColor.getSelectedIndex() > 0) {

            String talla = (String) comboTalla.getSelectedItem();
            String color = (String) comboColor.getSelectedItem();

            int[] stock = obtenerStockVariante(Integer.parseInt(selectedProduct.getId_prod()), talla, color);

            // Mostrar claramente la equivalencia: 1 caja = 24 pares
            int stockCajas = stock[0];
            int stockPares = stock[1];
            int totalPares = stockPares + (stockCajas * 24); // 1 caja = 24 pares

            txt_resulP1.setText("Stock: Cajas: " + stockCajas
                    + " - Pares: " + stockPares
                    + " | Total: " + totalPares + " pares (1 caja = 24 pares)");

            // Cambiar color del texto según disponibilidad
            if (stock[0] == 0 && stock[1] == 0) {
                txt_resulP1.setForeground(java.awt.Color.RED);
            } else if (stock[1] < 10) {
                txt_resulP1.setForeground(java.awt.Color.ORANGE);
            } else {
                txt_resulP1.setForeground(javax.swing.UIManager.getColor("Label.foreground"));
            }
        }
    }

    private int[] obtenerStockVariante(int idProducto, String talla, String color) {
        try {
            Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                    ? controller.getTraspasoActual().getIdBodegaOrigen()
                    : null;

            List<ModelProductVariant> variants = (idBodegaOrigen != null)
                    ? serviceVariant.getVariantsByProductAndWarehouse(idProducto, idBodegaOrigen)
                    : serviceVariant.getVariantsByProduct(idProducto);

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()) {
                    String tallaVariant = construirNombreTalla(variant);
                    String colorVariant = variant.getColorName();

                    if (talla.equals(tallaVariant) && color.equals(colorVariant)) {
                        return new int[] { variant.getStockBoxes(), variant.getStockPairs() };
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new int[] { 0, 0 };
    }

    private int[] obtenerStockProductoPorBodega(int idProducto, int idBodega) {
        String sql = "SELECT COALESCE(SUM(ib.Stock_caja),0) AS cajas, COALESCE(SUM(ib.Stock_par),0) AS pares "
                + "FROM inventario_bodega ib "
                + "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante "
                + "WHERE pv.id_producto = ? AND ib.id_bodega = ? AND ib.activo = 1";

        java.sql.Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            conn = raven.controlador.principal.conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            stmt.setInt(2, idBodega);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new int[] { rs.getInt("cajas"), rs.getInt("pares") };
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignore) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignore) {
            }
        }
        return new int[] { 0, 0 };
    }

    private void cargarImagenProducto(Blob imgBlob) {
        if (imgBlob == null) {
            System.out.println("No hay imágenes subidas del producto");
            ICON.setIcon(null); // Limpiar el icono
            ICON.setText("SIN IMAGEN");
        } else {
            try {
                // Convertir Blob a byte[]
                InputStream is = imgBlob.getBinaryStream();
                byte[] imageBytes = is.readAllBytes();

                // Crear ImageIcon desde los bytes
                ImageIcon imageIcon = new ImageIcon(imageBytes);

                // Escalar la imagen (verificar dimensiones)
                int ancho = ICON.getWidth() > 0 ? ICON.getWidth() : 200; // Valor por defecto
                int alto = ICON.getHeight() > 0 ? ICON.getHeight() : 200;

                Image imagenEscalada = imageIcon.getImage()
                        .getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);

                // Asignar al componente
                ICON.setIcon(new ImageIcon(imagenEscalada));
                ICON.setText("");

            } catch (SQLException | IOException e) {
                System.err.println("Error al cargar imagen: " + e.getMessage());
                ICON.setIcon(null);
                ICON.setText("ERROR IMAGEN");
            }
        }

        // Actualizar estilo del botón de agregar después de cargar la imagen
        actualizarEstiloBtnAgregar();
    }

    private String construirNombreTalla(ModelProductVariant variant) {
        String nombre = variant.getSizeName();
        String sistema = variant.getSizeSystem();

        if (nombre != null && !nombre.trim().isEmpty()) {
            if (sistema != null && !sistema.trim().isEmpty() && !nombre.contains(sistema)) {
                return nombre + " " + sistema;
            }
            return nombre;
        }

        return null;
    }

    private void updateSearchResultsProd() {
        menu.setVisible(false);
    }

    private void abrirModalBusquedaRapida() {
        javax.swing.JDialog dialog = new javax.swing.JDialog();
        dialog.setTitle("Buscador de productos");
        dialog.setModal(true);
        dialog.setSize(980, 640);
        dialog.setLocationRelativeTo(this);

        try {
            conexion.getInstance().connectToDatabase();
            javax.sql.DataSource ds = conexion.getInstance().getDataSource();
            ProductoRepository repo = new ProductoRepository(ds);
            ProductoBusquedaService svc = new ProductoBusquedaService(repo);

            try {
                boolean connected = conexion.getInstance().isConnected();
                System.out.println("[Diagnóstico Buscador] ConectadoBD=" + connected);
            } catch (Exception ignore) {
            }

            try {
                raven.clases.admin.UserSession us = raven.clases.admin.UserSession.getInstance();
                raven.controlador.admin.ModelUser u = us.getCurrentUser();
                String rol = (u != null ? u.getRol() : "");
                boolean canView = raven.clases.admin.PrivilegeManager.getInstance().canView(rol, "productos");
                System.out.println("[Diagnóstico Buscador] Rol='" + rol + "' canView(productos)=" + canView);
            } catch (Exception ignore) {
            }

            Integer idBodegaOrigen = null;
            if (controller != null && controller.getTraspasoActual() != null) {
                idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
                System.out.println("Filtrando por bodega origen: " + idBodegaOrigen);
            }

            ProductoBuscadorPanel buscador = new ProductoBuscadorPanel(svc, idBodegaOrigen);
            Integer idBodegaDestinoCfg = null;
            try {
                raven.clases.productos.TraspasoConfig cfg = raven.clases.productos.TraspasoConfig.load();
                idBodegaDestinoCfg = cfg.getIdDestino();
            } catch (Exception ignore) {
            }
            Integer idBodegaDestino = null;
            if (controller != null && controller.getTraspasoActual() != null) {
                try {
                    idBodegaDestino = controller.getTraspasoActual().getIdBodegaDestino();
                } catch (Exception ignore) {
                }
            }
            if (idBodegaDestino == null)
                idBodegaDestino = idBodegaDestinoCfg;
            if (idBodegaDestino != null && idBodegaDestino > 0) {
                buscador.setIdBodegaDestino(idBodegaDestino);
            }
            String tipoSel = null;
            try {
                if (jComboBox1 != null && jComboBox1.getSelectedIndex() > 0) {
                    tipoSel = (String) jComboBox1.getSelectedItem();
                }
            } catch (Throwable ignore) {
            }
            buscador.setTipoSeleccion(tipoSel != null ? tipoSel : "par");
            buscador.setVarianteSeleccionListener((VarianteDTO variante) -> {
                if (controller == null || controller.getTraspasoActual() == null)
                    return;
                ProductoTraspasoItem item = mapearVarianteParaTraspaso(variante);
                if (item != null && controller.agregarProducto(item)) {
                    cargarProductosExistentes();
                    dialog.dispose();
                    Toast.show(this, Toast.Type.SUCCESS, "Producto agregado al traspaso");
                }
            });

            dialog.setContentPane(buscador);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error abriendo buscador: " + e.getMessage());
        }
    }

    private void buscarPorBarcodeDirecto(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return;
        }

        // Limpiar búsqueda anterior
        limpiarCamposProducto();

        // Buscar producto
        DataSearch producto = buscarProductoPorEanOBarcode(codigo);

        if (producto != null) {
            // Seleccionar producto y poblar campos
            seleccionarProducto(producto, true);
            // Enfocar cantidad para agilizar el ingreso
            txtCantidad.requestFocus();
            txtCantidad.selectAll();
        } else {
            // Producto no encontrado
            Toast.show(this, Toast.Type.WARNING, "Producto no encontrado: " + codigo);
            txtSearchProd.requestFocus();
            txtSearchProd.selectAll();
        }
    }

    private void abrirModalBusquedaAvanzada() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "Buscador Avanzado", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        raven.application.form.productos.buscador.BuscadorProductosPanel panel = new raven.application.form.productos.buscador.BuscadorProductosPanel();
        panel.setOnProductoSeleccionado(item -> {
            dialog.dispose();
            // Usamos el EAN o el ID para buscar y cargar en paso2
            if (item.getEan() != null && !item.getEan().isEmpty()) {
                buscarPorBarcodeDirecto(item.getEan());
            } else {
                // Si no tiene EAN, intentamos con el ID de la variante
                buscarPorBarcodeDirecto(String.valueOf(item.getIdVariante()));
            }
        });

        dialog.add(panel);
        panel.focusSearch();
        dialog.setVisible(true);
    }

    private void abrirModalBusquedaAnterior() {
        // Obtener la bodega origen del traspaso
        Integer idBodegaOrigen = null;
        if (controller != null && controller.getTraspasoActual() != null) {
            idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
        }

        // Validar que existe una bodega origen
        if (idBodegaOrigen == null || idBodegaOrigen <= 0) {
            Toast.show(this, Toast.Type.ERROR, "No se pudo obtener la bodega origen del traspaso");
            return;
        }

        System.out.println("INFO Abriendo buscador de productos para bodega origen: " + idBodegaOrigen);

        // Abrir el diálogo de búsqueda de productos
        BuscadorProductoDialog.mostrar(this, idBodegaOrigen, producto -> {
            if (producto != null) {
                System.out.println("INFO Producto seleccionado desde buscador: " + producto.getNombre());

                // Usar el identificador del producto (EAN/SKU/Código)
                String codigo = producto.getIdentificador();

                // Buscar el producto usando la misma lógica que txtSearchProd
                if (codigo != null && !codigo.isEmpty()) {
                    DataSearch productoEncontrado = buscarProductoPorEanOBarcode(codigo);
                    if (productoEncontrado != null) {
                        // Producto encontrado con stock en bodega origen
                        selectedProduct = productoEncontrado;
                        seleccionarProducto(productoEncontrado, true);
                        txtSearchProd.setText(""); // Limpiar campo
                        System.out.println("SUCCESS Producto cargado correctamente desde buscador");
                    } else {
                        // Si no encuentra por EAN, mostrar mensaje
                        Toast.show(paso2.this, Toast.Type.WARNING,
                                "No se pudo cargar el producto. Verifique el stock en la bodega origen.");
                        System.out.println("WARNING Producto no encontrado en bodega origen con código: " + codigo);
                    }
                }
            }
        }, configurados -> {
            // Callback para selección múltiple (Ctrl+Click)
            if (configurados != null && !configurados.isEmpty()) {
                agregarProductosConfigurados(configurados);
            }
        }, this::importarExcel, this::descargarPlantilla);
    }

    /**
     * Agrega múltiples productos configurados desde el buscador (selección con
     * Ctrl+Click)
     */
    private void agregarProductosConfigurados(java.util.List<BuscadorProductoDialog.ProductoConfigurado> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        if (!validarControllerCompleto()) {
            return;
        }

        int agregados = 0;
        int errores = 0;
        StringBuilder mensajesError = new StringBuilder();

        System.out.println("INFO Procesando " + items.size() + " productos desde selección múltiple");

        for (BuscadorProductoDialog.ProductoConfigurado pc : items) {
            BuscadorProductoDialog.ProductoSeleccionado p = pc.getProducto();
            if (p == null) {
                errores++;
                continue;
            }

            try {
                String tipo = pc.getTipo() != null ? pc.getTipo().toLowerCase().trim() : "par";
                int cantidad = Math.max(1, pc.getCantidad());

                // Validar stock según tipo
                if ("par".equals(tipo)) {
                    if (cantidad > p.getStockPares()) {
                        mensajesError.append("• ").append(p.getNombre())
                                .append(": Stock insuficiente (pares)\n");
                        errores++;
                        continue;
                    }
                } else if ("caja".equals(tipo)) {
                    if (cantidad > p.getStockCajas()) {
                        mensajesError.append("• ").append(p.getNombre())
                                .append(": Stock insuficiente (cajas)\n");
                        errores++;
                        continue;
                    }
                }

                // Buscar el producto completo con todos sus datos
                String codigo = p.getIdentificador();
                DataSearch productoCompleto = buscarProductoPorEanOBarcode(codigo);

                if (productoCompleto == null) {
                    mensajesError.append("• ").append(p.getNombre())
                            .append(": No encontrado en bodega origen\n");
                    errores++;
                    continue;
                }

                // Crear item de traspaso
                ProductoTraspasoItem productoTraspaso = new ProductoTraspasoItem();
                productoTraspaso.setIdProducto(p.getIdProducto());
                productoTraspaso.setIdVariante(p.getIdVariante());
                productoTraspaso.setCodigoProducto(codigo);
                productoTraspaso.setNombreProducto(p.getNombre());

                // Establecer talla y color (getNombreCompleto() los usará automáticamente)
                String color = p.getColor();
                String talla = p.getTalla();
                productoTraspaso.setColor(color);
                productoTraspaso.setTalla(talla);
                productoTraspaso.setTipo(tipo);
                productoTraspaso.setCantidadSolicitada(cantidad);
                productoTraspaso.setPrecioUnitario(p.getPrecioVenta() != null ? p.getPrecioVenta() : BigDecimal.ZERO);

                // El nombre completo se construye automáticamente con getNombreCompleto()
                String nombreCompleto = productoTraspaso.getNombreCompleto();

                // Agregar al controller
                boolean agregadoExitosamente = controller.agregarProducto(productoTraspaso);

                if (agregadoExitosamente) {
                    agregados++;
                    System.out.println("SUCCESS Agregado: " + nombreCompleto + " - " + cantidad + " " + tipo);
                } else {
                    mensajesError.append("• ").append(p.getNombre())
                            .append(": Error al agregar\n");
                    errores++;
                }

            } catch (Exception e) {
                errores++;
                mensajesError.append("• ").append(p.getNombre())
                        .append(": ").append(e.getMessage()).append("\n");
                System.err.println("ERROR Error procesando producto: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Actualizar tabla si se agregó al menos uno
        if (agregados > 0) {
            actualizarTablaProductos();
        }

        // Mostrar resultado
        if (agregados > 0 && errores == 0) {
            Toast.show(this, Toast.Type.SUCCESS,
                    agregados + " producto" + (agregados > 1 ? "s" : "") + " agregado" + (agregados > 1 ? "s" : "")
                            + " al traspaso");
        } else if (agregados > 0 && errores > 0) {
            Toast.show(this, Toast.Type.WARNING,
                    agregados + " agregados, " + errores + " con errores");
            if (mensajesError.length() > 0) {
                JOptionPane.showMessageDialog(this,
                        "Productos con errores:\n" + mensajesError.toString(),
                        "Errores en selección múltiple",
                        JOptionPane.WARNING_MESSAGE);
            }
        } else {
            Toast.show(this, Toast.Type.ERROR,
                    "No se pudo agregar ningún producto");
            if (mensajesError.length() > 0) {
                JOptionPane.showMessageDialog(this,
                        mensajesError.toString(),
                        "Errores",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        System.out.println("INFO Resultado: " + agregados + " agregados, " + errores + " errores");
    }

    private java.util.List<DataSearch> buscarProductosExactosEnBodega(String termino, Integer idBodegaOrigen,
            int limit) {
        java.util.List<DataSearch> list = new java.util.ArrayList<>();
        if (termino == null || termino.trim().isEmpty() || idBodegaOrigen == null || idBodegaOrigen <= 0) {
            return list;
        }
        String texto = termino.trim().toLowerCase();
        String[] tokens = texto.split("\\s+");
        String soloDigitos = texto.replaceAll("[^0-9]", "");
        boolean tieneId = !soloDigitos.isEmpty() && soloDigitos.length() <= 9;
        boolean tieneEAN = !soloDigitos.isEmpty() && soloDigitos.length() >= 8;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.*, MIN(pv.id_variante) AS id_variante, MIN(pv.imagen) AS imagen ");
        sql.append("FROM inventario_bodega ib ");
        sql.append("INNER JOIN producto_variantes pv ON pv.id_variante = ib.id_variante AND pv.disponible = 1 ");
        sql.append("INNER JOIN productos p ON p.id_producto = pv.id_producto ");
        sql.append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("WHERE ib.id_bodega = ? AND ib.activo = 1 AND p.activo = 1 ");
        sql.append("  AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0) ");
        sql.append("  AND (");
        int groups = 0;
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.isEmpty())
                continue;
            if (groups++ > 0)
                sql.append(" OR ");
            sql.append(
                    " (LOWER(p.nombre) LIKE ? OR LOWER(COALESCE(m.nombre,'')) LIKE ? OR LOWER(COALESCE(p.codigo_modelo,'')) LIKE ? OR LOWER(COALESCE(pv.ean,'')) LIKE ?) ");
        }
        if (tieneId) {
            if (groups++ > 0)
                sql.append(" OR ");
            sql.append(" p.id_producto = ? ");
        }
        sql.append(") ");
        sql.append("GROUP BY p.id_producto ");
        sql.append("ORDER BY (LOWER(p.nombre) LIKE ?) DESC, p.nombre ASC ");
        sql.append("LIMIT ?");

        java.sql.Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql.toString());
            int idx = 1;
            stmt.setInt(idx++, idBodegaOrigen);
            for (String t : tokens) {
                if (t.isEmpty())
                    continue;
                String likeAny = "%" + t + "%";
                stmt.setString(idx++, likeAny); // p.nombre
                stmt.setString(idx++, likeAny); // m.nombre
                stmt.setString(idx++, likeAny); // p.codigo_modelo
                stmt.setString(idx++, likeAny); // pv.ean
            }
            if (tieneId) {
                stmt.setInt(idx++, Integer.parseInt(soloDigitos));
            }
            stmt.setString(idx++, texto + "%"); // boost por prefijo
            stmt.setInt(idx++, limit);
            rs = stmt.executeQuery();
            while (rs.next()) {
                DataSearch data = crearDataSearchDesdeResultSet(rs, texto);
                if (data != null)
                    list.add(data);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignore) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignore) {
            }
        }
        return list;
    }

    private ProductoTraspasoItem mapearVarianteParaTraspaso(VarianteDTO variante) {
        if (variante == null)
            return null;
        ProductoTraspasoItem item = new ProductoTraspasoItem();
        item.setIdProducto(variante.getIdProducto());
        item.setIdVariante(variante.getIdVariante());
        item.setColor(variante.getColor());
        item.setTalla(variante.getTalla());
        item.setSku(variante.getSku());
        item.setEan(variante.getEan());
        item.setTipo("par");
        item.setCantidadSolicitada(1);
        java.math.BigDecimal precio = variante.getPrecioVenta() != null ? variante.getPrecioVenta()
                : java.math.BigDecimal.ZERO;
        item.setPrecioUnitario(precio);
        Integer stock = variante.getStockPares() != null ? variante.getStockPares() : 0;
        item.setStockDisponible(stock);
        item.setParesPorCaja(24);
        try {
            ServiceProduct sp = new ServiceProduct();
            raven.controlador.productos.ModelProduct p = sp.getProductById(variante.getIdProducto());
            if (p != null) {
                item.setNombreProducto(p.getName());
                item.setCodigoProducto(p.getModelCode());
                if (p.getPairsPerBox() > 0)
                    item.setParesPorCaja(p.getPairsPerBox());
            }
        } catch (Exception ignore) {
        }
        return item;
    }

    private static String normalizarGeneroSimple(String genero) {
        if (genero == null)
            return "";
        String g = genero.trim().toUpperCase();
        if ("CABALLERO".equals(g) || "HOMBRE".equals(g) || "MASCULINO".equals(g) || "H".equals(g))
            return "HOMBRE";
        if ("DAMAS".equals(g) || "MUJER".equals(g) || "FEMENINO".equals(g) || "M".equals(g))
            return "MUJER";
        if ("UNISEX".equals(g) || "U".equals(g))
            return "UNISEX";
        if ("NINO".equals(g) || "NIÑO".equals(g) || "N".equals(g))
            return "NIÑO";
        return genero;
    }

    private static class ImageCellRenderer extends javax.swing.JPanel implements javax.swing.table.TableCellRenderer {
        private final javax.swing.JLabel img = new javax.swing.JLabel();
        private final javax.swing.JLabel loading = new javax.swing.JLabel("Cargando...");
        private final javax.swing.JLabel unavailable = new javax.swing.JLabel("Imagen no disponible");
        private final java.util.Map<Integer, EstadoCarga> estados = new java.util.HashMap<>();
        private final javax.swing.JTable tableRef;
        private final int w = 80;
        private final int h = 60;

        private static class EstadoCarga {
            enum Status {
                LOADING, LOADED, TIMEOUT, FAILED
            }

            Status status;
            javax.swing.ImageIcon icon;
            long startMs;
            javax.swing.Timer timeout;
        }

        ImageCellRenderer(javax.swing.JTable table) {
            super(new java.awt.BorderLayout());
            this.tableRef = table;
            img.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            loading.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            unavailable.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            setOpaque(true);
            setPreferredSize(new java.awt.Dimension(w, h));
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            removeAll();
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            DataSearch d = (value instanceof DataSearch) ? (DataSearch) value : null;
            int pid = d != null ? Integer.parseInt(d.getId_prod()) : -1;
            EstadoCarga st = estados.get(pid);
            if (st == null) {
                st = new EstadoCarga();
                st.status = EstadoCarga.Status.LOADING;
                st.startMs = System.currentTimeMillis();
                estados.put(pid, st);
                add(loading, java.awt.BorderLayout.CENTER);
                iniciarCargaAsync(d, pid, st, row);
            } else {
                switch (st.status) {
                    case LOADED:
                        img.setIcon(st.icon);
                        add(img, java.awt.BorderLayout.CENTER);
                        break;
                    case TIMEOUT:
                    case FAILED:
                        add(unavailable, java.awt.BorderLayout.CENTER);
                        break;
                    case LOADING:
                        add(loading, java.awt.BorderLayout.CENTER);
                        break;
                }
            }
            return this;
        }

        private void iniciarCargaAsync(DataSearch d, int pid, EstadoCarga st, int row) {
            st.timeout = new javax.swing.Timer(2000, e -> {
                if (st.status == EstadoCarga.Status.LOADING) {
                    st.status = EstadoCarga.Status.TIMEOUT;
                    javax.swing.SwingUtilities.invokeLater(() -> tableRef.repaint(tableRef.getCellRect(row, 0, true)));
                }
            });
            st.timeout.setRepeats(false);
            st.timeout.start();

            new Thread(() -> {
                try {
                    javax.swing.ImageIcon icon = null;
                    if (d != null && d.getImagen() != null) {
                        java.io.InputStream is = d.getImagen().getBinaryStream();
                        byte[] bytes = is.readAllBytes();
                        javax.swing.ImageIcon raw = new javax.swing.ImageIcon(bytes);
                        java.awt.Image scaled = raw.getImage().getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
                        icon = new javax.swing.ImageIcon(scaled);
                    }
                    if (icon != null && st.status == EstadoCarga.Status.LOADING
                            && System.currentTimeMillis() - st.startMs < 2000) {
                        st.icon = icon;
                        st.status = EstadoCarga.Status.LOADED;
                        javax.swing.SwingUtilities
                                .invokeLater(() -> tableRef.repaint(tableRef.getCellRect(row, 0, true)));
                    }
                } catch (Exception ex) {
                    st.status = EstadoCarga.Status.FAILED;
                    javax.swing.SwingUtilities.invokeLater(() -> tableRef.repaint(tableRef.getCellRect(row, 0, true)));
                }
            }, "ImgLoad-" + pid).start();
        }
    }

    private java.util.List<DataSearch> searchFast(String text) {
        String key;
        if (controller != null && controller.getTraspasoActual() != null
                && controller.getTraspasoActual().getIdBodegaOrigen() != null) {
            key = "bodega:" + controller.getTraspasoActual().getIdBodegaOrigen() + "|" + text;
        } else {
            key = "all|" + text;
        }
        long now = System.currentTimeMillis();
        if (cacheBusqueda.containsKey(key) && (now - cacheTimestampMs) < CACHE_TTL_MS) {
            return cacheBusqueda.get(key);
        }
        java.util.List<DataSearch> resultado = searchOptimizado(text);
        cacheBusqueda.put(key, resultado);
        cacheTimestampMs = now;
        return resultado;
    }

    private void cargarColoresPorTalla(int idProducto, String tallaSeleccionada) {
        isLoadingColores = true;

        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

            comboColor.removeAllItems();
            comboColor.addItem("Color");

            Set<String> coloresUnicos = new LinkedHashSet<>();

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable() && (variant.getStockPairs() > 0 || variant.getStockBoxes() > 0)) {
                    String tallaVariant = construirNombreTalla(variant);

                    if (tallaSeleccionada.equals(tallaVariant)) {
                        String color = variant.getColorName();
                        if (color != null && !color.trim().isEmpty()) {
                            coloresUnicos.add(color);
                        }
                    }
                }
            }

            for (String color : coloresUnicos) {
                comboColor.addItem(color);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            isLoadingColores = false;
        }
    }

    private DataSearch crearDataSearchDesdeResultSet(ResultSet rs, String search) {
        try {
            int idProducto = rs.getInt("id_producto");
            String codigoModelo = rs.getString("codigo_modelo");
            String nombre = rs.getString("nombre");
            String color = null;
            String talla = null;
            String descripcion = rs.getString("descripcion");
            int idCategoria = rs.getInt("id_categoria");
            int idMarca = rs.getInt("id_marca");
            int idProveedor = rs.getInt("id_proveedor");
            BigDecimal precioCompra = rs.getBigDecimal("precio_compra");
            BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
            int stockMinimo = rs.getInt("stock_minimo");
            String genero = rs.getString("genero");
            boolean activo = rs.getBoolean("activo");
            Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
            String ubicacion = rs.getString("ubicacion");
            int paresPorCaja = rs.getInt("pares_por_caja");

            int idVariante = 0;
            try {
                idVariante = rs.getInt("id_variante");
            } catch (SQLException ignore) {
            }

            Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                    ? controller.getTraspasoActual().getIdBodegaOrigen()
                    : null;
            String colorSel = null;
            String tallaSel = null;
            if (comboColor != null && comboColor.getSelectedItem() != null) {
                String c = comboColor.getSelectedItem().toString();
                if (c != null && !c.trim().isEmpty() && !"Color".equalsIgnoreCase(c)) {
                    colorSel = c.trim();
                }
            }
            if (comboTalla != null && comboTalla.getSelectedItem() != null) {
                String t = comboTalla.getSelectedItem().toString();
                if (t != null && !t.trim().isEmpty() && !"Talla".equalsIgnoreCase(t)) {
                    tallaSel = t.trim();
                }
            }
            if (idBodegaOrigen != null && idBodegaOrigen > 0 && colorSel != null && tallaSel != null) {
                Integer idVarianteFiltrado = obtenerIdVariantePorFiltros(idProducto, idBodegaOrigen, colorSel,
                        tallaSel);
                if (idVarianteFiltrado != null && idVarianteFiltrado > 0) {
                    idVariante = idVarianteFiltrado.intValue();
                }
            }

            int stockPorCajas = 0;
            int stockPorPares = 0;
            int stock = 0;
            Blob imagen = null;
            try {
                java.sql.ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    if ("imagen".equalsIgnoreCase(md.getColumnLabel(i))) {
                        imagen = rs.getBlob("imagen");
                        break;
                    }
                }
            } catch (SQLException ignore) {
            }
            if (imagen == null) {
                imagen = obtenerImagenPrimeraVariante(idProducto);
            }

            String idpString = String.valueOf(idProducto);
            boolean startsWithSearch = search != null && (codigoModelo.toLowerCase().startsWith(search.toLowerCase())
                    || nombre.toLowerCase().startsWith(search.toLowerCase()));

            return new DataSearch(
                    idpString, codigoModelo, nombre, color, talla, startsWithSearch,
                    descripcion, idCategoria, idMarca, idProveedor, precioCompra,
                    precioVenta, stock, stockMinimo, genero, activo, imagen,
                    fechaCreacion, stockPorCajas, stockPorPares, paresPorCaja,
                    idVariante);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Blob obtenerImagenPrimeraVariante(int idProducto) {
        String sql = "SELECT imagen FROM producto_variantes "
                + "WHERE id_producto = ? AND disponible = 1 AND imagen IS NOT NULL "
                + "ORDER BY id_variante LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // USAR TU CLASE conexion
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBlob("imagen");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // CERRAR RECURSOS CORRECTAMENTE
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private List<DataSearch> getProductosSugeridos(int limit) {
        List<DataSearch> list = new ArrayList<>();

        String sql = "SELECT p.*, pv.id_variante, " // Añadir id_variante
                + "COALESCE((SELECT SUM(vd.cantidad) FROM venta_detalles vd WHERE vd.id_producto = p.id_producto), 0) as total_vendido "
                + "FROM productos p "
                + "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                + "WHERE p.activo = 1 AND pv.disponible = 1 "
                + "AND (pv.stock_por_pares > 0 OR pv.stock_por_cajas > 0) "
                + "GROUP BY p.id_producto, pv.id_variante " // Incluir id_variante en GROUP BY
                + "ORDER BY total_vendido DESC, p.fecha_creacion DESC "
                + "LIMIT ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // USAR TU CLASE conexion
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);
            rs = stmt.executeQuery();

            while (rs.next()) {
                DataSearch data = crearDataSearchDesdeResultSet(rs, "");
                if (data != null) {
                    list.add(data);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error obteniendo productos sugeridos: " + e.getMessage());
        } finally {
            // CERRAR RECURSOS CORRECTAMENTE
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void limpiarSeleccionesVariantes() {
        isLoadingTallas = true;
        isLoadingColores = true;

        comboTalla.removeAllItems();
        comboTalla.addItem("Talla");

        comboColor.removeAllItems();
        comboColor.addItem("Color");

        isLoadingTallas = false;
        isLoadingColores = false;
    }

    private boolean tieneVariantes(int idProducto) {
        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);
            return variants.size() > 1; // Si tiene más de una variante
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean existeInventarioActivoEnBodega(int idVariante, int idBodega) {
        String sql = "SELECT COUNT(*) FROM inventario_bodega WHERE id_variante = ? AND id_bodega = ? AND activo = 1";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVariante);
            stmt.setInt(2, idBodega);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String determinarTipoVariantePorBodega(int idProducto, int idVariante, int idBodega) {
        String sql = "SELECT Stock_caja, Stock_par FROM inventario_bodega WHERE id_variante = ? AND id_bodega = ?";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVariante);
            stmt.setInt(2, idBodega);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int cajas = rs.getInt("Stock_caja");
                    int pares = rs.getInt("Stock_par");
                    if (cajas > 0 && pares == 0)
                        return "caja";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "par"; // Default
    }

    private int obtenerStockEnBodegaOrigen(int idProducto, int idVariante, String tipo) {
        if (controller == null || controller.getTraspasoActual() == null)
            return 0;
        int idBodega = controller.getTraspasoActual().getIdBodegaOrigen();

        String sql = "SELECT Stock_caja, Stock_par FROM inventario_bodega WHERE id_variante = ? AND id_bodega = ?";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVariante);
            stmt.setInt(2, idBodega);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if ("caja".equalsIgnoreCase(tipo)) {
                        return rs.getInt("Stock_caja");
                    } else {
                        return rs.getInt("Stock_par");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelMain = new javax.swing.JPanel();
        panelProducto = new javax.swing.JPanel();
        titulo2 = new javax.swing.JLabel();
        txtSearchProd = new javax.swing.JTextField();
        txt_resulP = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        txtCantidad = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txt_resulP1 = new javax.swing.JLabel();
        ICON = new javax.swing.JLabel();
        btnAgregarProd = new raven.componentes.icon.JIconButton();
        comboTalla = new javax.swing.JComboBox<>();
        comboColor = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        BtnBuscar = new javax.swing.JButton();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        panelProducto.setForeground(new java.awt.Color(255, 255, 255));

        titulo2.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        titulo2.setText("Buscar producto");

        txtSearchProd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtSearchProdMouseClicked(evt);
            }
        });
        txtSearchProd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchProdActionPerformed(evt);
            }
        });
        txtSearchProd.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchProdKeyReleased(evt);
            }
        });

        txt_resulP.setText("NOMBRE - PRECIO:$$$$$");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar", "caja", "par" }));

        jLabel5.setText("Tipo:");

        txtCantidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCantidadActionPerformed(evt);
            }
        });

        jLabel6.setText("Cantidad:");

        txt_resulP1.setText("Stock: Cajas:  - Pares:  | 24 pares/caja");

        ICON.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ICON.setText("ICON");

        btnAgregarProd.setIconType(jiconfont.icons.font_awesome.FontAwesome.SHARE);
        btnAgregarProd.setInheritsPopupMenu(true);
        btnAgregarProd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarProdActionPerformed(evt);
            }
        });

        comboTalla.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        comboTalla.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Talla", "35 EU M", "36 EU M",
                "37 EU M", "38 EU M", "39 EU M", "40 EU M", "41 EU M", "42 EU M", "39 EU H", "40 EU H", "41 EU H",
                "42 EU H", "43 EU H", "44 EU H", "45 EU H", "46 EU H", "24 EU N", "25 EU N", "26 EU N", "27 EU N",
                "28 EU N", "29 EU N", "30 EU N", "31 EU N", "32 EU N", "33 EU N" }));
        comboTalla.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboTallaItemStateChanged(evt);
            }
        });
        comboTalla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTallaActionPerformed(evt);
            }
        });

        comboColor.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboColorItemStateChanged(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Talla");

        jLabel12.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Color");

        // BtnBuscar.setText(""); // Ahora usa ícono SVG configurado en init()

        javax.swing.GroupLayout panelProductoLayout = new javax.swing.GroupLayout(panelProducto);
        panelProducto.setLayout(panelProductoLayout);
        panelProductoLayout.setHorizontalGroup(
                panelProductoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProductoLayout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addGroup(panelProductoLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                                .addComponent(titulo2,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 190,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                                .addComponent(txtSearchProd,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 229,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(BtnBuscar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 55,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                        28, Short.MAX_VALUE)))
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(comboTalla,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 160,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel11))
                                                .addGap(15, 15, 15)
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(comboColor,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 160,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                                .addGap(28, 28, 28)
                                                                .addComponent(jComboBox1,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 228,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel6)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(txtCantidad,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 145,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(txt_resulP1,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 473,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txt_resulP,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 473,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel5))
                                                .addGap(18, 18, 18)
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(ICON, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                                .addComponent(btnAgregarProd,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 49,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE)))))
                                .addGap(26, 26, 26)));
        panelProductoLayout.setVerticalGroup(
                panelProductoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelProductoLayout
                                .createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addGroup(panelProductoLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(titulo2)
                                                        .addComponent(jLabel12))
                                                .addGap(9, 9, 9)
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(comboColor,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 40,
                                                                Short.MAX_VALUE)
                                                        .addComponent(txtSearchProd,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addComponent(jLabel11)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(comboTalla, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                44, Short.MAX_VALUE)
                                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                                .addGroup(panelProductoLayout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(BtnBuscar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                43,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(0, 0, Short.MAX_VALUE)))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelProductoLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addComponent(txt_resulP, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txt_resulP1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(ICON, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(panelProductoLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addGap(13, 13, 13)
                                                .addComponent(jLabel5))
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addGap(3, 3, 3)
                                                .addComponent(jLabel6))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                panelProductoLayout.createSequentialGroup()
                                                        .addGap(3, 3, 3)
                                                        .addComponent(jComboBox1,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 43,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelProductoLayout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelProductoLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(btnAgregarProd,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtCantidad,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(18, 18, 18)));

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Codigo", "Producto", "Tipo", "Cantidad", "Precio", "Descuento", "Subtotal", "id"
                }) {
            Class[] types = new Class[] {
                    java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.Integer.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, true, true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setUpdateSelectionOnSort(false);
        table.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
            }

            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }

            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
                tableAncestorRemoved(evt);
            }
        });
        scroll.setViewportView(table);

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                .addGap(39, 39, 39)
                                .addGroup(panelMainLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(panelProducto, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(scroll))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        panelMainLayout.setVerticalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(panelProducto, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 337,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(24, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelMain, javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE));
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchProdKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSearchProdKeyReleased
        // Re-habilitada búsqueda con debounce para no congelar la UI
        // Ignorar teclas de navegación
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP ||
                evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN ||
                evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            return;
        }

        String text = txtSearchProd.getText().trim();

        if (text.isEmpty()) {
            if (menu.isVisible()) {
                menu.setVisible(false);
            }
            return;
        }

        // Reiniciar timer
        if (debounceSearch != null) {
            debounceSearch.restart();
        }
    }// GEN-LAST:event_txtSearchProdKeyReleased

    private void txtSearchProdActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtSearchProdActionPerformed
        String text = txtSearchProd.getText().trim();
        if (text.isEmpty()) {
            limpiarCamposProducto();
            return;
        }
        buscarPorBarcodeDirecto(text);
    }// GEN-LAST:event_txtSearchProdActionPerformed

    private void txtSearchProdMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_txtSearchProdMouseClicked
        // Sin sugerencias automáticas al hacer clic
    }// GEN-LAST:event_txtSearchProdMouseClicked

    private void comboTallaItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_comboTallaItemStateChanged
        // Solo procesar cuando el evento es de selección y no estamos en carga inicial
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED && !isLoadingTallas) {
            if (selectedProduct != null) {
                String tallaSeleccionada = comboTalla.getSelectedIndex() > 0
                        ? (String) comboTalla.getSelectedItem()
                        : null;

                if (tallaSeleccionada != null) {
                    // Cargar colores disponibles para esta talla
                    cargarColoresPorTalla(Integer.parseInt(selectedProduct.getId_prod()), tallaSeleccionada);

                    // Actualizar datos del producto si ya hay color seleccionado
                    if (comboColor.getSelectedIndex() > 0) {
                        actualizarDatosProductoCompleto();
                    }
                }
            }
        }
    }// GEN-LAST:event_comboTallaItemStateChanged

    private void limpiarCampos() {
        txtSearchProd.setText("");
        txt_resulP.setText("NOMBRE - PRECIO:$$$$$");
        txt_resulP1.setText("Stock: Cajas:  - Pares:  | 24 pares/caja");

        // Restablecer color normal del texto
        txt_resulP1.setForeground(javax.swing.UIManager.getColor("Label.foreground"));

        txtCantidad.setText("");
        jComboBox1.setSelectedIndex(0);
        ICON.setIcon(null);
        ICON.setText("ICON");
        selectedProduct = null;

        // Limpiar combos de variantes
        limpiarSeleccionesVariantes();
    }

    private void comboColorItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_comboColorItemStateChanged
        // Solo procesar cuando el evento es de selección y no estamos en carga inicial
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED && !isLoadingColores) {
            if (selectedProduct != null) {
                String colorSeleccionado = comboColor.getSelectedIndex() > 0
                        ? (String) comboColor.getSelectedItem()
                        : null;

                if (colorSeleccionado != null) {
                    // Actualizar datos del producto si ya hay talla seleccionada
                    if (comboTalla.getSelectedIndex() > 0) {
                        actualizarDatosProductoCompleto();
                    }
                }
            }
        }
    }// GEN-LAST:event_comboColorItemStateChanged

    private java.math.BigDecimal obtenerPrecioVariante(int idProducto, String talla, String color) {
        if (talla == null || color == null) {
            return null;
        }

        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()) {
                    String tallaVariant = construirNombreTalla(variant);
                    String colorVariant = variant.getColorName();

                    if (talla.equals(tallaVariant) && color.equals(colorVariant)) {
                        Double precio = variant.getSalePrice();
                        if (precio != null && precio > 0) {
                            return java.math.BigDecimal.valueOf(precio);
                        }
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Integer obtenerIdVariantePorColorYTalla(int idProducto, String color, String talla) {
        // CORRECCIÓN: Verificar que color y talla no sean valores por defecto
        if (color == null || talla == null
                || "Color".equals(color) || "Talla".equals(talla)
                || color.trim().isEmpty() || talla.trim().isEmpty()) {
            System.out.println("WARNING Color o talla inválidos: color='" + color + "', talla='" + talla + "'");
            return null;
        }

        String sql = "SELECT pv.id_variante "
                + "FROM producto_variantes pv "
                + "INNER JOIN colores c ON pv.id_color = c.id_color "
                + "INNER JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE pv.id_producto = ? AND c.nombre = ? AND t.numero = ? AND pv.disponible = 1 "
                + "AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            stmt.setString(2, color.trim());
            stmt.setString(3, talla.trim());
            Integer idBodegaOrigen = controller != null && controller.getTraspasoActual() != null
                    ? controller.getTraspasoActual().getIdBodegaOrigen()
                    : null;
            stmt.setInt(4, idBodegaOrigen != null ? idBodegaOrigen : 0);

            System.out.println("DEBUG Buscando variante para producto " + idProducto
                    + ", color: '" + color + "', talla: '" + talla + "'");

            rs = stmt.executeQuery();

            if (rs.next()) {
                int idVariante = rs.getInt("id_variante");
                System.out.println("SUCCESS ID Variante encontrado: " + idVariante
                        + " para " + color + " - " + talla);
                return idVariante;
            } else {
                System.out.println("WARNING No se encontró variante para " + color + " - " + talla);

                // DEBUG: Verificar qué variantes existen para este producto
                verificarVariantesDisponibles(idProducto);
            }

        } catch (SQLException e) {
            System.err.println("Error obteniendo ID de variante: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private Integer obtenerIdVariantePorFiltros(int idProducto, int idBodega, String colorUi, String tallaUi) {
        if (colorUi == null || tallaUi == null
                || "Color".equals(colorUi) || "Talla".equals(tallaUi)
                || colorUi.trim().isEmpty() || tallaUi.trim().isEmpty()) {
            System.out.println("WARNING Color o talla inválidos: color='" + colorUi + "', talla='" + tallaUi + "'");
            return null;
        }

        String tallaNormalizada = tallaUi.trim();
        String sql = "SELECT pv.id_variante "
                + "FROM producto_variantes pv "
                + "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 "
                + "INNER JOIN colores c ON pv.id_color = c.id_color "
                + "INNER JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE pv.id_producto = ? AND ib.id_bodega = ? AND pv.disponible = 1 "
                + "AND c.nombre = ? AND (t.numero = ? OR TRIM(CONCAT(t.numero, ' ', COALESCE(t.sistema, ''))) = ?) "
                + "LIMIT 1";

        java.sql.Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            conn = raven.controlador.principal.conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            stmt.setInt(2, idBodega);
            stmt.setString(3, colorUi.trim());
            stmt.setString(4, tallaNormalizada);
            stmt.setString(5, tallaNormalizada);

            System.out.println("DEBUG Buscando variante con filtros:");
            System.out.println("    idProducto = " + idProducto);
            System.out.println("    idBodega   = " + idBodega);
            System.out.println("    color      = '" + colorUi + "'");
            System.out.println("    talla      = '" + tallaNormalizada + "'");

            rs = stmt.executeQuery();
            if (rs.next()) {
                int idVariante = rs.getInt("id_variante");
                System.out.println("SUCCESS Variante encontrada: id_variante = " + idVariante);
                return idVariante;
            } else {
                System.out.println("WARNING No se encontró variante con esos filtros");
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error obteniendo id_variante por filtros: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignore) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private int obtenerStockFallbackProductoVariantes(int idProducto, Integer idVariante, String tipo,
            Integer idBodegaOrigen) {
        String sql;

        String columna = "caja".equalsIgnoreCase(tipo) ? "ib.Stock_caja" : "ib.Stock_par";
        if (idVariante != null) {
            sql = "SELECT COALESCE(" + columna + ",0) AS stock FROM inventario_bodega ib "
                    + "WHERE ib.activo = 1 AND ib.id_variante IN (SELECT pv.id_variante FROM producto_variantes pv WHERE pv.id_producto = ? AND pv.id_variante = ?) "
                    + (idBodegaOrigen != null ? "AND ib.id_bodega = ?" : "");
        } else {
            sql = "SELECT COALESCE(SUM(" + columna + "),0) AS stock FROM inventario_bodega ib "
                    + "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante "
                    + "WHERE ib.activo = 1 AND pv.id_producto = ? "
                    + (idBodegaOrigen != null ? "AND ib.id_bodega = ?" : "");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            int idx = 1;
            stmt.setInt(idx++, idProducto);
            if (idVariante != null) {
                stmt.setInt(idx++, idVariante);
            }
            if (idBodegaOrigen != null) {
                stmt.setInt(idx++, idBodegaOrigen);
            }

            rs = stmt.executeQuery();

            if (rs.next()) {
                int stock = rs.getInt("stock");
                System.out.println("DEBUG Stock fallback(bodega=" + idBodegaOrigen + "): " + stock + " " + tipo);
                return stock;
            }

        } catch (SQLException e) {
            System.err.println("Error en fallback de stock: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private void btnAgregarProdActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAgregarProdActionPerformed
        System.out.println("INFO Iniciando proceso de agregar producto...");

        if (!validarControllerCompleto()) {
            return;
        }

        if (selectedProduct == null) {
            String textoBusqueda = txtSearchProd.getText().trim();
            if (!textoBusqueda.isEmpty()) {
                DataSearch producto = buscarProductoPorEanOBarcode(textoBusqueda);
                if (producto != null) {
                    selectedProduct = producto;
                } else {
                    Toast.show(this, Toast.Type.ERROR, "Producto no encontrado. Seleccione un producto primero.");
                    return;
                }
            } else {
                Toast.show(this, Toast.Type.ERROR, "Seleccione un producto primero.");
                return;
            }
        }

        String tipo = (String) jComboBox1.getSelectedItem();
        if (tipo == null || tipo.equals("Seleccionar")) {
            Toast.show(this, Toast.Type.ERROR, "Seleccione un tipo de venta (caja o par)");
            return;
        }

        String cantidadStr = txtCantidad.getText().trim();
        if (cantidadStr.isEmpty()) {
            Toast.show(this, Toast.Type.ERROR, "Ingrese una cantidad");
            return;
        }

        int cantidad = 0;
        try {
            cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) {
                Toast.show(this, Toast.Type.ERROR, "La cantidad debe ser mayor a 0");
                return;
            }
        } catch (NumberFormatException e) {
            Toast.show(this, Toast.Type.ERROR, "La cantidad debe ser un número válido");
            return;
        }

        String tallaSeleccionada = comboTalla.getSelectedIndex() > 0 ? (String) comboTalla.getSelectedItem() : null;
        String colorSeleccionado = comboColor.getSelectedIndex() > 0 ? (String) comboColor.getSelectedItem() : null;
        if (tallaSeleccionada != null && "Talla".equals(tallaSeleccionada))
            tallaSeleccionada = null;
        if (colorSeleccionado != null && "Color".equals(colorSeleccionado))
            colorSeleccionado = null;

        if (comboTalla.getItemCount() > 1 && tallaSeleccionada == null) {
            Toast.show(this, Toast.Type.ERROR, "Seleccione una talla");
            return;
        }
        if (comboColor.getItemCount() > 1 && colorSeleccionado == null) {
            Toast.show(this, Toast.Type.ERROR, "Seleccione un color");
            return;
        }

        final int cantFinal = cantidad;
        final String tallaFinal = tallaSeleccionada;
        final String colorFinal = colorSeleccionado;
        final String tipoFinal = tipo;
        final DataSearch prodFinal = selectedProduct;
        final TraspasoController myController = this.controller;

        btnAgregarProd.setEnabled(false);
        Toast.show(this, Toast.Type.INFO, "Verificando disponibilidad...");

        new javax.swing.SwingWorker<TraspasoController.StockVerificationResult, Void>() {
            private ProductoTraspasoItem item;

            @Override
            protected TraspasoController.StockVerificationResult doInBackground() throws Exception {
                item = new ProductoTraspasoItem();
                item.setIdProducto(Integer.parseInt(prodFinal.getId_prod()));

                String cod = prodFinal.getEAN();
                if (cod == null || cod.isEmpty())
                    cod = prodFinal.getSKU();
                item.setCodigoProducto(cod);
                item.setEan(prodFinal.getEAN());
                item.setSku(prodFinal.getSKU());
                item.setNombreProducto(prodFinal.getNombre());

                item.setCantidadSolicitada(cantFinal);
                item.setTipo(tipoFinal);
                item.setColor(colorFinal);
                item.setTalla(tallaFinal);

                Integer idBodega = myController.getTraspasoActual().getIdBodegaOrigen();
                Integer idVariante = null;

                boolean esBusquedaPorCodigo = !comboColor.isEnabled() && !comboTalla.isEnabled();
                if (esBusquedaPorCodigo && prodFinal.getIdVariante() > 0) {
                    idVariante = prodFinal.getIdVariante();
                } else if (tallaFinal != null && colorFinal != null) {
                    idVariante = obtenerIdVariantePorFiltros(item.getIdProducto(), idBodega, colorFinal, tallaFinal);
                } else if (prodFinal.getIdVariante() > 0) {
                    idVariante = prodFinal.getIdVariante();
                }

                if ((idVariante == null || idVariante <= 0) && tallaFinal != null && colorFinal != null) {
                    java.util.List<raven.controlador.productos.ModelProductVariant> variants = serviceVariant
                            .getVariantsByProductAndWarehouse(item.getIdProducto(), idBodega);
                    for (raven.controlador.productos.ModelProductVariant v : variants) {
                        if (!v.isAvailable())
                            continue;
                        if (tallaFinal.equals(construirNombreTalla(v)) && colorFinal.equals(v.getColorName())) {
                            idVariante = v.getVariantId();
                            break;
                        }
                    }
                }

                if (idVariante == null || idVariante <= 0) {
                    return new TraspasoController.StockVerificationResult(false,
                            "No se encontró variante válida en bodega origen", 0);
                }
                item.setIdVariante(idVariante);

                return myController.verificarStockDisponible(item.getIdProducto(), item.getIdVariante(), item.getTipo(),
                        item.getCantidadSolicitada());
            }

            @Override
            protected void done() {
                try {
                    TraspasoController.StockVerificationResult result = get();
                    if (result.isExito()) {
                        ProductoTraspasoItem existente = myController.buscarProductoExistente(item);

                        java.math.BigDecimal precio = obtenerPrecioProductoDesdeDB(item.getIdProducto(),
                                item.getIdVariante());
                        item.setPrecioUnitario(precio);
                        if (item.getPrecioUnitario() == null
                                || item.getPrecioUnitario().equals(java.math.BigDecimal.ZERO)) {
                            java.math.BigDecimal pv = obtenerPrecioVariante(item.getIdProducto(), item.getTalla(),
                                    item.getColor());
                            if (pv != null)
                                item.setPrecioUnitario(pv);
                        }

                        if (existente != null) {
                            int opcion = JOptionPane.showConfirmDialog(paso2.this,
                                    "El producto ya existe en el traspaso.\n¿Desea actualizar la cantidad?",
                                    "Producto Existente",
                                    JOptionPane.YES_NO_OPTION);
                            if (opcion == JOptionPane.YES_OPTION) {
                                int nuevaCant = existente.getCantidadSolicitada() + cantFinal;
                                myController.actualizarCantidadProducto(existente, nuevaCant);
                                finalizarAgregado("Cantidad actualizada correctamente");
                            }
                        } else {
                            myController.agregarProductoDirecto(item);
                            finalizarAgregado("Producto agregado exitosamente");
                        }
                    } else {
                        Toast.show(paso2.this, Toast.Type.ERROR, result.getMensaje());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show(paso2.this, Toast.Type.ERROR, "Error al agregar: " + e.getMessage());
                } finally {
                    btnAgregarProd.setEnabled(true);
                }
            }

            private void finalizarAgregado(String msg) {
                actualizarTablaProductos();
                limpiarCampos();
                Toast.show(paso2.this, Toast.Type.SUCCESS, msg);
            }
        }.execute();
    }// GEN-LAST:event_btnAgregarProdActionPerformed

    private void comboTallaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboTallaActionPerformed
        verificarSeleccionVarianteContraBodegaOrigen();
    }// GEN-LAST:event_comboTallaActionPerformed

    private void txtCantidadActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtCantidadActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtCantidadActionPerformed

    private void tableAncestorRemoved(javax.swing.event.AncestorEvent evt) {// GEN-FIRST:event_tableAncestorRemoved
        verificarProductosCargados();
    }// GEN-LAST:event_tableAncestorRemoved

    private List<DataSearch> searchProductosBasicos(String texto, Integer idBodegaOrigen, int limit) {
        List<DataSearch> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            String textoLower = texto == null ? "" : texto.toLowerCase().trim();
            if (textoLower.isEmpty()) {
                return list;
            }
            String[] palabras = textoLower.split("\\s+");

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.*, MIN(pv.id_variante) AS id_variante ")
                    .append("FROM inventario_bodega ib ")
                    .append("INNER JOIN producto_variantes pv ON pv.id_variante = ib.id_variante ")
                    .append("INNER JOIN productos p ON p.id_producto = pv.id_producto ")
                    .append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ")
                    .append("WHERE ib.id_bodega = ? AND ib.activo = 1 ")
                    .append("  AND p.activo = 1 AND pv.disponible = 1 ")
                    .append("  AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0) ")
                    .append("  AND (");
            for (int i = 0; i < palabras.length; i++) {
                if (i > 0)
                    sql.append(" OR ");
                sql.append(
                        " (LOWER(p.codigo_modelo) LIKE ? OR LOWER(p.nombre) LIKE ? OR LOWER(p.descripcion) LIKE ? OR LOWER(COALESCE(pv.sku,'')) LIKE ? OR LOWER(COALESCE(pv.ean,'')) LIKE ? OR LOWER(COALESCE(m.nombre,'')) LIKE ?) ");
            }
            sql.append(") ");
            sql.append("GROUP BY p.id_producto ");
            sql.append("ORDER BY (LOWER(p.codigo_modelo) = ?) DESC, ")
                    .append("         (LOWER(p.nombre) = ?) DESC, ")
                    .append("         (LOWER(p.codigo_modelo) LIKE ?) DESC, ")
                    .append("         (LOWER(p.nombre) LIKE ?) DESC, ")
                    .append("         p.nombre ASC ")
                    .append("LIMIT ?");

            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, idBodegaOrigen != null ? idBodegaOrigen : 0);
            for (String palabra : palabras) {
                String likeAny = "%" + palabra + "%";
                ps.setString(idx++, likeAny); // p.codigo_modelo
                ps.setString(idx++, likeAny); // p.nombre
                ps.setString(idx++, likeAny); // p.descripcion
                ps.setString(idx++, likeAny); // pv.sku
                ps.setString(idx++, likeAny); // pv.ean
                ps.setString(idx++, likeAny); // m.nombre
            }
            String likePrefix = textoLower + "%";
            ps.setString(idx++, textoLower);
            ps.setString(idx++, textoLower);
            ps.setString(idx++, likePrefix);
            ps.setString(idx++, likePrefix);
            ps.setInt(idx++, limit);

            rs = ps.executeQuery();
            while (rs.next()) {
                DataSearch data = crearDataSearchDesdeResultSet(rs, textoLower);
                if (data != null)
                    list.add(data);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (Exception ignore) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignore) {
            }
        }
        return list;
    }

    private java.math.BigDecimal obtenerPrecioProductoDesdeDB(Integer idProducto, Integer idVariante) {
        String sql = "SELECT precio_venta FROM productos WHERE id_producto = ?";
        if (idVariante != null) {
            sql = "SELECT COALESCE(pv.precio_venta, p.precio_venta) as precio_venta "
                    + "FROM productos p "
                    + "LEFT JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                    + "WHERE p.id_producto = ? AND pv.id_variante = ?";
        }

        java.sql.Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            conn = raven.controlador.principal.conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            if (idVariante != null) {
                stmt.setInt(2, idVariante);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                java.math.BigDecimal pv = rs.getBigDecimal("precio_venta");
                return pv != null ? pv : java.math.BigDecimal.ZERO;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error obteniendo precio: " + e.getMessage());
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignore) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignore) {
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    private void verificarSeleccionVarianteContraBodegaOrigen() {
        try {
            if (selectedProduct == null || controller == null || controller.getTraspasoActual() == null) {
                return;
            }
            Integer idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
            if (idBodegaOrigen == null || idBodegaOrigen <= 0) {
                return;
            }
            String tallaSel = comboTalla.getSelectedIndex() > 0 ? (String) comboTalla.getSelectedItem() : null;
            String colorSel = comboColor.getSelectedIndex() > 0 ? (String) comboColor.getSelectedItem() : null;
            if (tallaSel == null || colorSel == null) {
                return;
            }
            Integer idVariante = obtenerIdVariantePorFiltros(Integer.parseInt(selectedProduct.getId_prod()),
                    idBodegaOrigen, colorSel, tallaSel);
            if (idVariante == null || idVariante <= 0) {
                btnAgregarProd.setEnabled(false);
                Toast.show(this, Toast.Type.ERROR, "No se encontró variante válida en la bodega de origen");
                registrarIntentoInvalidoSimple("seleccion_sin_variante", idBodegaOrigen);
                return;
            }
            boolean ok = existeInventarioActivoEnBodega(idVariante, idBodegaOrigen);
            btnAgregarProd.setEnabled(ok);
            if (!ok) {
                String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                String usuario = obtenerNombreUsuarioActual();
                String detalle = "Variante " + idVariante + " no pertenece a bodega origen " + idBodegaOrigen + " ("
                        + ts + ", usuario: " + usuario + ")";
                System.err.println("ERROR " + detalle);
                Toast.show(this, Toast.Type.ERROR, "La variante seleccionada no corresponde a la bodega de origen");
            }
        } catch (Exception e) {
            System.err.println("Error en verificación de selección de variante: " + e.getMessage());
        }
    }

    public boolean validarProductosContraBodegaOrigenYBloquear() {
        if (controller == null || controller.getTraspasoActual() == null) {
            return false;
        }
        Integer idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
        if (idBodegaOrigen == null || idBodegaOrigen <= 0) {
            return false;
        }
        java.util.List<String> discrepancias = generarReporteDiscrepanciasBodega();
        if (!discrepancias.isEmpty()) {
            String mensaje = "Se detectaron productos con bodega incorrecta. El envío será bloqueado hasta corregir: \n"
                    + String.join("\n", discrepancias);
            JOptionPane.showMessageDialog(this, mensaje, "Validación de Bodega", JOptionPane.ERROR_MESSAGE);
            Toast.show(this, Toast.Type.ERROR, "Inconsistencias de bodega detectadas");
            return false;
        }
        return true;
    }

    private java.util.List<String> generarReporteDiscrepanciasBodega() {
        java.util.List<String> rep = new java.util.ArrayList<>();
        try {
            if (controller == null || controller.getTraspasoActual() == null)
                return rep;
            Integer idBodegaOrigen = controller.getTraspasoActual().getIdBodegaOrigen();
            java.util.List<ProductoTraspasoItem> productos = controller.getTraspasoActual().getProductos();
            for (ProductoTraspasoItem p : productos) {
                Integer idVar = p.getIdVariante();
                if (idVar == null || idVar <= 0)
                    continue;
                if (!existeInventarioActivoEnBodega(idVar, idBodegaOrigen)) {
                    java.util.List<Integer> bodegas = obtenerBodegasConVariante(idVar);
                    rep.add("Prod:" + p.getNombreCompleto() + " Var:" + idVar + " BodegaOrigen:" + idBodegaOrigen
                            + " BodegasVariante:" + bodegas);
                    registrarIntentoInvalidoBodega("reporte_discrepancia", p, idBodegaOrigen);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generando reporte de discrepancias: " + e.getMessage());
        }
        return rep;
    }

    private java.util.List<Integer> obtenerBodegasConVariante(int idVariante) {
        java.util.List<Integer> bodegas = new java.util.ArrayList<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = conexion.getInstance().createConnection();
            ps = con.prepareStatement(
                    "SELECT id_bodega FROM inventario_bodega WHERE id_variante = ? AND activo = 1 AND (COALESCE(Stock_par,0) > 0 OR COALESCE(Stock_caja,0) > 0)");
            ps.setInt(1, idVariante);
            rs = ps.executeQuery();
            while (rs.next())
                bodegas.add(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("Error obteniendo bodegas de variante: " + e.getMessage());
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (Exception ignore) {
            }
            try {
                if (con != null)
                    con.close();
            } catch (Exception ignore) {
            }
        }
        return bodegas;
    }

    private void registrarIntentoInvalidoBodega(String tipo, ProductoTraspasoItem producto, Integer idBodegaOrigen) {
        try {
            String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            Integer uid = SessionManager.getInstance().getCurrentUserId();
            String uname = obtenerNombreUsuarioActual();
            System.out.println("LOG|" + ts + "|usuario:" + uid + "(" + uname + ")|tipo:" + tipo + "|producto:"
                    + producto.getNombreCompleto() + "|variante:" + producto.getIdVariante() + "|bodega_origen:"
                    + idBodegaOrigen);
        } catch (Throwable ignore) {
        }
    }

    private void registrarIntentoInvalidoSimple(String tipo, Integer idBodegaOrigen) {
        try {
            String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            Integer uid = SessionManager.getInstance().getCurrentUserId();
            String uname = obtenerNombreUsuarioActual();
            System.out.println("LOG|" + ts + "|usuario:" + uid + "(" + uname + ")|tipo:" + tipo + "|bodega_origen:"
                    + idBodegaOrigen);
        } catch (Throwable ignore) {
        }
    }

    private String obtenerNombreUsuarioActual() {
        try {
            raven.controlador.admin.ModelUser cu = UserSession.getInstance().getCurrentUser();
            if (cu != null)
                return cu.getNombre();
        } catch (Throwable ignore) {
        }
        return "N/A";
    }

    private void descargarPlantilla() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar plantilla de Excel");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("plantilla_traspasos.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Plantilla");

                // Estilo para el encabezado
                XSSFCellStyle headerStyle = workbook.createCellStyle();
                XSSFFont font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.THIN);

                // Crear encabezado
                Row headerRow = sheet.createRow(0);
                String[] columns = { "Codigo (SKU/EAN)", "Cantidad" };

                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Agregar datos de ejemplo
                Row exampleRow = sheet.createRow(1);
                exampleRow.createCell(0).setCellValue("1234567890123");
                exampleRow.createCell(1).setCellValue(10);

                // Ajustar ancho de columnas
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Agregar comentario explicativo
                Row infoRow = sheet.createRow(3);
                Cell infoCell = infoRow.createCell(0);
                infoCell.setCellValue("Nota: Columna A para el código del producto, Columna B para la cantidad.");

                try (FileOutputStream outputStream = new FileOutputStream(fileToSave)) {
                    workbook.write(outputStream);
                    Toast.show(this, Toast.Type.SUCCESS, "Plantilla guardada exitosamente");

                    // Intentar abrir el archivo
                    try {
                        Desktop.getDesktop().open(fileToSave);
                    } catch (Exception ex) {
                        // Ignorar si no se puede abrir
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.show(this, Toast.Type.ERROR, "Error al guardar la plantilla: " + e.getMessage());
            }
        }
    }

    /**
     * Método para verificar si hay productos agregados antes de cerrar
     */
    public boolean hayProductosAgregados() {
        if (controller == null || controller.getTraspasoActual() == null) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            return model.getRowCount() > 0;
        }

        return controller.getCantidadTotalProductos() > 0;
    }

    /**
     * Método para mostrar confirmación de cierre si hay productos agregados
     */
    public boolean confirmarCierre() {
        if (hayProductosAgregados()) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Está seguro de que desea salir?\n" +
                            "Hay productos agregados al traspaso que se perderán.",
                    "Confirmar salida",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return opcion == JOptionPane.YES_OPTION;
        }
        return true; // No hay productos, se puede cerrar directamente
    }

    private void ejecutarBusquedaAsincrona(String text) {
        if (text.isEmpty()) {
            if (menu.isVisible()) {
                menu.setVisible(false);
            }
            return;
        }

        new javax.swing.SwingWorker<List<DataSearch>, Void>() {
            @Override
            protected List<DataSearch> doInBackground() throws Exception {
                // Use searchFast which includes caching and optimization
                return searchFast(text);
            }

            @Override
            protected void done() {
                try {
                    List<DataSearch> data = get();
                    if (data != null && !data.isEmpty()) {
                        searchProd.setData(data, menu); // Pass menu as required by PanelSearch
                        menu.setPopupSize(menu.getWidth(), (searchProd.getItemSize() * 35) + 2);
                        if (!menu.isVisible()) {
                            menu.show(txtSearchProd, 0, txtSearchProd.getHeight());
                            txtSearchProd.requestFocus();
                        }
                    } else {
                        menu.setVisible(false);
                    }
                } catch (Exception e) {
                    System.err.println("Error en búsqueda asíncrona: " + e.getMessage());
                }
            }
        }.execute();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtnBuscar;
    private javax.swing.JLabel ICON;
    private raven.componentes.icon.JIconButton btnAgregarProd;
    private javax.swing.JComboBox<String> comboColor;
    private javax.swing.JComboBox<Object> comboTalla;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelProducto;
    private javax.swing.JScrollPane scroll;
    public javax.swing.JTable table;
    private javax.swing.JLabel titulo2;
    private javax.swing.JTextField txtCantidad;
    private javax.swing.JTextField txtSearchProd;
    private javax.swing.JLabel txt_resulP;
    private javax.swing.JLabel txt_resulP1;
    // End of variables declaration//GEN-END:variables

}
