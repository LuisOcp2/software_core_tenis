package raven.controlador.inventario;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import raven.controlador.principal.conexion;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import raven.application.form.productos.creates.CreateInventario;
import raven.clases.inventario.ServiceConteoInventario;
import raven.clases.productos.ServiceProduct;
import raven.controlador.productos.ModelProduct;
import raven.dao.UsuarioDAO;
import raven.modelos.Usuario;
import raven.clases.admin.UserSession;
import raven.application.form.productos.ComboItem;
import raven.dao.InventarioBodegaDAO;
import raven.utils.ProductImageOptimizer;

/**
 * Controlador para el formulario de creación de inventario. Maneja la lógica
 * entre la interfaz y los servicios de negocio.
 */
public class CreateInventarioController {

    private final CreateInventario vista;
    private final ServiceConteoInventario serviceConteo;
    private final UsuarioDAO usuarioDAO;
    private final ServiceProduct serviceProduct;
    private final InventarioBodegaDAO bodegaDAO; // New DAO
    public boolean esCajas; // Por defecto conteo de cajas

    private List<ModelProduct> productosSeleccionados = new ArrayList<>();
    private Set<Integer> selectedVariantIds = new HashSet<>(); // Changed from selectedBarcodes
    private List<ModelProduct> currentProducts = new ArrayList<>(); // Track currently displayed products

    public CreateInventarioController(CreateInventario vista, boolean con) {
        this.vista = vista;
        esCajas = con;
        this.serviceConteo = new ServiceConteoInventario();
        this.usuarioDAO = new UsuarioDAO();
        this.serviceProduct = new ServiceProduct();
        this.bodegaDAO = new InventarioBodegaDAO(); // Init DAO
        inicializarFormulario();
    }

    /**
     * Inicializa el formulario con datos necesarios
     */
    private void inicializarFormulario() {
        // Cargar tipos de inventario
        JComboBox<String> comboTipoInventario = vista.getTipoInventarioCombo();
        comboTipoInventario.removeAllItems();
        comboTipoInventario.addItem("general");
        comboTipoInventario.addItem("parcial");
        comboTipoInventario.addItem("ciclico");
        comboTipoInventario.addItem("verificacion");

        // Cargar bodegas y configurar permisos
        // Cargar bodegas y configurar permisos
        cargarBodegas();
        cargarMarcas();
        cargarCategorias();

        // Cargar responsables (usuarios)

        cargarResponsables();

        // Configurar eventos
        configurarEventos();

        // Cargar productos
        cargarProductos();
    }

    private void cargarBodegas() {
        JComboBox comboBodega = vista.getBodegaCombo(); // Using raw JComboBox or cast
        comboBodega.removeAllItems();

        try {
            java.util.List<ComboItem> bodegas = bodegaDAO.obtenerBodegasCombo();
            for (ComboItem item : bodegas) {
                comboBodega.addItem(item);
            }

            // Permission Logic
            UserSession session = UserSession.getInstance();
            if (session.hasRole("admin")) {
                comboBodega.setEnabled(true);
            } else {
                comboBodega.setEnabled(false);
                // Select user's bodega
                Integer userBodegaId = session.getIdBodegaUsuario();
                if (userBodegaId != null) {
                    for (int i = 0; i < comboBodega.getItemCount(); i++) {
                        ComboItem item = (ComboItem) comboBodega.getItemAt(i);
                        if (item.getId() == userBodegaId) {
                            comboBodega.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarEstanterias() {
        JComboBox<String> comboEstanteria = vista.getEstanteriaCombo();
        comboEstanteria.removeAllItems();
        comboEstanteria.addItem("Todas");

        ComboItem selectedBodega = (ComboItem) vista.getBodegaCombo().getSelectedItem();
        if (selectedBodega != null) {
            try {
                List<String> ubicaciones = bodegaDAO.obtenerUbicacionesPorBodega(selectedBodega.getId());
                for (String ubicacion : ubicaciones) {
                    comboEstanteria.addItem(ubicacion);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarMarcas() {
        JComboBox<ComboItem> combo = vista.getMarcaCombo();
        combo.removeAllItems();
        combo.addItem(new ComboItem(0, "Todas las Marcas"));
        String sql = "SELECT id_marca, nombre FROM marcas WHERE activo = 1 ORDER BY nombre";
        cargarComboGeneric(combo, sql, "id_marca", "nombre");
    }

    private void cargarCategorias() {
        JComboBox<ComboItem> combo = vista.getCategoriaCombo();
        combo.removeAllItems();
        combo.addItem(new ComboItem(0, "Todas las Categorías"));
        String sql = "SELECT id_categoria, nombre FROM categorias WHERE activo = 1 ORDER BY nombre";
        try {
            cargarComboGeneric(combo, sql, "id_categoria", "nombre");
        } catch (Exception e) {
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

    /**
     * Carga la lista de usuarios como responsables
     */
    private void cargarResponsables() {
        try {
            JComboBox<Usuario> comboResponsable = vista.getResponsableCombo();
            comboResponsable.removeAllItems();

            List<Usuario> usuarios = usuarioDAO.obtenerUsuariosActivos();
            for (Usuario usuario : usuarios) {
                comboResponsable.addItem(usuario);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al cargar responsables: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Configura los eventos de los componentes
     */
    private void configurarEventos() {
        // Evento al cambiar entre cajas y pares
        vista.getTipoCajas().addActionListener(e -> {
            esCajas = true;
            cargarProductos();
        });

        vista.getTipoPares().addActionListener(e -> {
            esCajas = false;
            cargarProductos();
        });

        // Modificar el evento de seleccionar todos
        JCheckBox checkTodos = vista.getCheckboxSeleccionarTodos();
        checkTodos.addActionListener(e -> {
            boolean seleccionado = checkTodos.isSelected();
            DefaultTableModel modelo = (DefaultTableModel) vista.getTablaProductos().getModel();

            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.setValueAt(seleccionado, i, 0);

                // Actualizar el Set usando currentProducts
                if (i < currentProducts.size()) {
                    int variantId = currentProducts.get(i).getVariantId();
                    if (seleccionado) {
                        selectedVariantIds.add(variantId);
                    } else {
                        selectedVariantIds.remove(variantId);
                    }
                }
            }
        });
        // Agregar listener para cambios en la tabla
        vista.getTablaProductos().getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0) { // Solo columna de checkboxes
                int row = e.getFirstRow();
                if (row >= 0 && row < currentProducts.size()) {
                    boolean selected = (Boolean) vista.getTablaProductos().getModel().getValueAt(row, 0);
                    int variantId = currentProducts.get(row).getVariantId();

                    if (selected) {
                        selectedVariantIds.add(variantId);
                    } else {
                        selectedVariantIds.remove(variantId);
                    }
                }
            }
        });

        // Evento al cambiar bodega
        vista.getBodegaCombo().addActionListener(e -> {
            cargarEstanterias();
            cargarProductos();
        });

        vista.getMarcaCombo().addActionListener(e -> cargarProductos());
        vista.getCategoriaCombo().addActionListener(e -> cargarProductos());
        vista.getEstanteriaCombo().addActionListener(e -> cargarProductos());
        vista.getCheckStock0().addActionListener(e -> cargarProductos());
        vista.getCheckNegativos().addActionListener(e -> cargarProductos());

    }

    /**
     * Carga los productos en la tabla según el tipo de conteo (cajas o pares)
     */
    public void cargarProductos() {
        // Mostrar cursor de espera
        vista.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        // Limpiar tabla antes de cargar
        JTable tabla = vista.getTablaProductos();
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);

        javax.swing.SwingWorker<List<ModelProduct>, Void> worker = new javax.swing.SwingWorker<List<ModelProduct>, Void>() {
            @Override
            protected List<ModelProduct> doInBackground() throws Exception {
                ComboItem selectedBodega = (ComboItem) vista.getBodegaCombo().getSelectedItem();
                int idBodega = (selectedBodega != null) ? selectedBodega.getId() : 0;

                ComboItem selectedMarca = (ComboItem) vista.getMarcaCombo().getSelectedItem();
                int idMarca = (selectedMarca != null) ? selectedMarca.getId() : 0;

                ComboItem selectedCategoria = (ComboItem) vista.getCategoriaCombo().getSelectedItem();
                int idCategoria = (selectedCategoria != null) ? selectedCategoria.getId() : 0;

                String ubicacion = (String) vista.getEstanteriaCombo().getSelectedItem();
                if (ubicacion == null || ubicacion.equals("Todas")) {
                    ubicacion = "";
                }

                return serviceConteo.obtenerProductosParaConteoBodega(esCajas, idBodega, ubicacion,
                        "", idMarca, idCategoria);
            }

            @Override
            protected void done() {
                try {
                    List<ModelProduct> productos = get();

                    // Filter locally for stock 0 and negatives
                    boolean showStock0 = vista.getCheckStock0().isSelected();
                    boolean showNegativos = vista.getCheckNegativos().isSelected();

                    List<ModelProduct> filteredList = new ArrayList<>();
                    for (ModelProduct p : productos) {
                        int stock = esCajas ? p.getBoxesStock() : p.getPairsStock();

                        if (stock == 0 && !showStock0)
                            continue;
                        if (stock < 0 && !showNegativos)
                            continue;

                        filteredList.add(p);
                    }
                    currentProducts = filteredList; // Update current products list

                    for (ModelProduct producto : currentProducts) {
                        Object[] row = {
                                selectedVariantIds.contains(producto.getVariantId()), // Mantener selección
                                producto.getBarcode(),
                                producto, // Pass the whole object for the renderer
                                producto.getCategory().getName(),
                                producto.getBrand().getName(),
                                esCajas ? producto.getBoxesStock() : producto.getPairsStock()
                        };
                        modelo.addRow(row);
                    }
                    // Load images
                    ProductImageOptimizer.loadImagesSequential(tabla, currentProducts);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Error al cargar productos: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    vista.setCursor(java.awt.Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    public void buscarProductos(String termino) {
        // Mostrar cursor de espera
        vista.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        // Limpiar tabla antes de buscar
        JTable tabla = vista.getTablaProductos();
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);

        javax.swing.SwingWorker<List<ModelProduct>, Void> worker = new javax.swing.SwingWorker<List<ModelProduct>, Void>() {
            @Override
            protected List<ModelProduct> doInBackground() throws Exception {
                ComboItem selectedBodega = (ComboItem) vista.getBodegaCombo().getSelectedItem();
                int idBodega = (selectedBodega != null) ? selectedBodega.getId() : 0;
                String ubicacion = (String) vista.getEstanteriaCombo().getSelectedItem();
                if (ubicacion == null || ubicacion.equals("Todas")) {
                    ubicacion = "";
                }

                ComboItem selectedMarca = (ComboItem) vista.getMarcaCombo().getSelectedItem();
                int idMarca = (selectedMarca != null) ? selectedMarca.getId() : 0;

                ComboItem selectedCategoria = (ComboItem) vista.getCategoriaCombo().getSelectedItem();
                int idCategoria = (selectedCategoria != null) ? selectedCategoria.getId() : 0;

                return serviceConteo.obtenerProductosParaConteoBodega(esCajas, idBodega, ubicacion,
                        termino, idMarca, idCategoria);
            }

            @Override
            protected void done() {
                try {
                    List<ModelProduct> productos = get();

                    // Filter locally for stock 0 and negatives
                    boolean showStock0 = vista.getCheckStock0().isSelected();
                    boolean showNegativos = vista.getCheckNegativos().isSelected();

                    List<ModelProduct> filteredList = new ArrayList<>();
                    for (ModelProduct p : productos) {
                        int stock = esCajas ? p.getBoxesStock() : p.getPairsStock();

                        if (stock == 0 && !showStock0)
                            continue;
                        if (stock < 0 && !showNegativos)
                            continue;

                        filteredList.add(p);
                    }
                    currentProducts = filteredList; // Actualizar lista actual

                    for (ModelProduct producto : currentProducts) {
                        Object[] row = {
                                selectedVariantIds.contains(producto.getVariantId()), // Restaurar selección usando ID
                                                                                      // de variante
                                producto.getBarcode(),
                                producto, // Pass object
                                producto.getCategory().getName(),
                                producto.getBrand().getName(),
                                esCajas ? producto.getBoxesStock() : producto.getPairsStock()
                        };
                        modelo.addRow(row);
                    }
                    // Load images
                    ProductImageOptimizer.loadImagesSequential(tabla, currentProducts);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Error al buscar productos: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    vista.setCursor(java.awt.Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void guardarSelecciones() {
        // Since we update selectedVariantIds in real-time with TableModelListener,
        // we might not need this method unless we want to force a sync.
        // But let's keep it to be safe, iterating currentProducts.

        // Actually, with the listener, this might be redundant or needed if listeners
        // don't fire on some changes.
        // But iterating the model is safe.
        JTable tabla = vista.getTablaProductos();
        DefaultTableModel model = (DefaultTableModel) tabla.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            boolean isSelected = (boolean) model.getValueAt(i, 0);
            if (i < currentProducts.size()) {
                int variantId = currentProducts.get(i).getVariantId();
                if (isSelected) {
                    selectedVariantIds.add(variantId);
                } else {
                    selectedVariantIds.remove(variantId);
                }
            }
        }
    }

    private void restaurarSelecciones() {
        JTable tabla = vista.getTablaProductos();
        DefaultTableModel model = (DefaultTableModel) tabla.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            if (i < currentProducts.size()) {
                int variantId = currentProducts.get(i).getVariantId();
                model.setValueAt(selectedVariantIds.contains(variantId), i, 0);
            }
        }
    }

    /**
     * Obtiene los productos seleccionados en la tabla verificando que existan
     * en la base de datos
     *
     * @return Lista de productos seleccionados
     */
    private List<ModelProduct> obtenerProductosSeleccionados() {
        List<ModelProduct> seleccionados = new ArrayList<>();
        JTable tabla = vista.getTablaProductos();
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();

        // Recorrer la tabla y verificar selección usando currentProducts
        for (int i = 0; i < modelo.getRowCount(); i++) {
            boolean seleccionado = (boolean) modelo.getValueAt(i, 0);

            if (seleccionado) {
                if (i < currentProducts.size()) {
                    ModelProduct producto = currentProducts.get(i);

                    // Actualizar el stock desde la tabla (columna 5)
                    try {
                        Object stockObj = modelo.getValueAt(i, 5);
                        int stock = 0;
                        if (stockObj instanceof Number) {
                            stock = ((Number) stockObj).intValue();
                        } else {
                            stock = Integer.parseInt(stockObj.toString());
                        }

                        if (esCajas) {
                            producto.setBoxesStock(stock);
                        } else {
                            producto.setPairsStock(stock);
                        }
                        seleccionados.add(producto);
                    } catch (Exception e) {
                        System.err.println("Error parsing stock for row " + i + ": " + e.getMessage());
                    }
                }
            }
        }

        return seleccionados;
    }

    public boolean crearConteoInventario() {

        if (!validarCampos()) {
            return false;
        }

        try {
            // Obtener datos del formulario
            String nombre = vista.getNombreTextField().getText().trim();
            String tipoConteo = (String) vista.getTipoInventarioCombo().getSelectedItem();
            Date fecha = new SimpleDateFormat("dd/MM/yyyy").parse(vista.getFechaTextField().getText());
            JComboBox<Usuario> comboResponsable = vista.getResponsableCombo();
            Usuario responsable = (Usuario) comboResponsable.getSelectedItem();
            String observaciones = vista.getObservacionesTextArea().getText().trim();

            // Obtener productos seleccionados
            productosSeleccionados = obtenerProductosSeleccionados();

            if (productosSeleccionados.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "Debe seleccionar al menos un producto para el conteo.",
                        "Validación", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Obtener bodega seleccionada
            ComboItem selectedBodega = (ComboItem) vista.getBodegaCombo().getSelectedItem();
            int idBodega = (selectedBodega != null) ? selectedBodega.getId() : 0;

            if (idBodega <= 0) {
                JOptionPane.showMessageDialog(null, "Debe seleccionar una bodega", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Crear conteo con el controlador principal
            InventarioController controller = new InventarioController();
            boolean resultado = controller.crearConteoInventario(
                    nombre, tipoConteo, fecha, "12:00:00", // Hora por defecto
                    responsable.getId(), esCajas, observaciones, productosSeleccionados, idBodega);

            if (resultado) {
                JOptionPane.showMessageDialog(null,
                        "Conteo de inventario creado exitosamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(null,
                        "Error al crear el conteo de inventario.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error al crear conteo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Valida que los campos obligatorios estén completos
     *
     * @return true si los campos son válidos, false en caso contrario
     */
    public boolean validarCampos() {
        if (vista.getNombreTextField().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "El nombre del conteo es obligatorio.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            vista.getNombreTextField().requestFocus();
            return false;
        }
        if (vista.getFechaTextField().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "La fecha programada es obligatoria.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            vista.getFechaTextField().requestFocus();
            return false;
        }

        if (vista.getResponsableCombo().getSelectedItem() == null) {
            JOptionPane.showMessageDialog(null,
                    "Debe seleccionar un responsable.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            vista.getResponsableCombo().requestFocus();
            return false;
        }

        return true;
    }
}
