package raven.application.form.productos.creates;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import jnafilechooser.api.JnaFileChooser;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelProfile;
import raven.controlador.comercial.ModelSupplier;
import raven.clases.productos.ServiceProduct;
import raven.modal.Toast;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import raven.dao.ProductoVariantesDAO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import raven.clases.productos.ServiceColor;
import raven.clases.productos.ServiceSize;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelColor;
import raven.controlador.productos.ModelProductVariant;
import raven.controlador.productos.ModelSize;
import raven.controlador.admin.SessionManager;
import raven.clases.admin.ServiceBodegas;
import raven.clases.productos.ServiceProductVariant;
import raven.controlador.admin.ModelBodegas;
import raven.application.form.productos.EdicionRapidaPreciosDialog;

/**
 *
 * @author RAVEN
 */
public class Create extends javax.swing.JPanel {
    private boolean isSaving = false;

    // ===================================================================
    // CONSTANTES Y CONFIGURACI??N
    // ===================================================================
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] SUPPORTED_FORMATS = { "png", "jpg", "jpeg" };
    private static final String PANEL_STYLE = "arc:35;background:$Login.background;";
    private static final String PANEL_BAJO = "background:lighten($Menu.background,25%)";

    // ===================================================================
    // VARIABLES DE ESTADO
    // ===================================================================
    private boolean isEditing = false;
    private int editingRow = -1;
    private ModelProfile profile;
    private int currentProductId = -1;
    private ModelProduct currentProduct;
    private raven.application.form.productos.GestionProductosForm parentForm;

    // ===================================================================
    // CACHÉ ESTÁTICO PARA COMBOS (OPTIMIZACIÓN)
    // ===================================================================
    private static class ComboCache {
        java.util.List<ModelBrand> brands;
        java.util.List<ModelCategory> categories;
        java.util.List<ModelSupplier> suppliers;
        long timestamp;
        private static final long CACHE_TTL_MS = 300_000L; // 5 minutos

        boolean isValid() {
            return brands != null && categories != null && suppliers != null
                    && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS;
        }
    }

    private static ComboCache comboCache = null;

    public void setParentForm(raven.application.form.productos.GestionProductosForm parent) {
        this.parentForm = parent;
    }

    // Gestión de imágenes por variantes
    private Map<Integer, File> archivosPorFila = new HashMap<>();
    private File archivoImagenSeleccionada = null;
    private ImageIcon imagenFilaActual = null;
    private Map<Integer, Integer> variantIdPorFila = new HashMap<>();
    private Map<Integer, Integer> bodegaIdPorFila = new HashMap<>();
    private Map<Integer, Integer> inventarioBodegaIdPorFila = new HashMap<>();// para editar por bodega
    private Map<Integer, String> ubicacionEspecificaPorFila = new HashMap<>();
    private Map<Integer, String> tipoPreferidoPorVariante = new HashMap<>();
    private final java.util.List<Integer> idsCaja = new java.util.ArrayList<>();
    private final java.util.List<Integer> idsPar = new java.util.ArrayList<>();
    private javax.swing.Timer debouncePar;
    private javax.swing.Timer debounceCompra;
    private javax.swing.JLabel jLabelBodega;
    private java.util.List<ModelBodegas> bodegaList = new java.util.ArrayList<>();
    private java.util.Set<String> skusGeneradosTabla = new java.util.LinkedHashSet<>();
    private boolean tablaModificada = false;
    private boolean preciosModificados = false;

    private raven.utils.ChangeTracker changeTracker = new raven.utils.ChangeTracker();
    private boolean isEditMode = false; // Nuevo flag
    private boolean isLoadingVariants = false; // Flag para evitar cargas duplicadas
    private javax.swing.JPanel loadingOverlay; // Overlay de carga para la tabla

    // Variables para agrupación
    private boolean isGrouped = false;
    private Object[][] originalData;
    private Map<Integer, Integer> originalVariantIdMap;
    private Map<Integer, List<Integer>> groupedVariantIdsMap = new HashMap<>();

    // ===================================================================
    // CONSTRUCTOR Y INICIALIZACI??N
    // ===================================================================
    public Create() {
        initComponents();

        // 3. Configurar todo
        setupFormConfiguration();
        loadInitialData();
    }

    private void aplicarEstiloPaneles() {
        panelInfoprod.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelVariante.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelPrecios.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelTit1.putClientProperty(FlatClientProperties.STYLE, PANEL_BAJO);
        paneltit2.putClientProperty(FlatClientProperties.STYLE, PANEL_BAJO);
        paneltit3.putClientProperty(FlatClientProperties.STYLE, PANEL_BAJO);

    }

    /**
     * Configuraci??n general del formulario
     */
    private void setupFormConfiguration() {
        setupTableConfiguration();
        aplicarEstiloPaneles();
        configurarBodegaCombo();
        configurarBtnCrearVarianteModal();
        setupGroupingButtons(); // NUEVA LLAMADA
        applyPermissions();
    }

    private void applyPermissions() {
        raven.clases.admin.UserSession userSession = raven.clases.admin.UserSession.getInstance();

        if (!userSession.hasPermission("productos_info")) {
            txtNombre.setEditable(false);
            txtModelo.setEditable(false);
            txtDescripcion.setEditable(false);
            comboCategoria.setEnabled(false);
            comboMarca.setEnabled(false);
            comboProveedor.setEnabled(false);
            comboGenero.setEnabled(false);

            // También precios base si son parte de info general
            if (txtPrecioCompra != null)
                txtPrecioCompra.setEditable(false);
            if (txtPrecioVenta != null)
                txtPrecioVenta.setEditable(false);
        }

        if (!userSession.hasPermission("productos_variantes")) {
            if (BtnCrear_Variante != null) {
                BtnCrear_Variante.setEnabled(false);
                BtnCrear_Variante.setToolTipText("No tiene permiso para agregar variantes");
                for (ActionListener al : BtnCrear_Variante.getActionListeners()) {
                    BtnCrear_Variante.removeActionListener(al);
                }
            }
        }
    }

    /**
     * Obtiene el ID de la talla a partir de su nombre completo ("numero sistema
     * g??nero").
     * Por ejemplo: "39 EU H" devolver?? el id_talla correspondiente al n??mero 39,
     * sistema EU y g??nero HOMBRE.
     * Si no se encuentra un id v??lido, retorna -1.
     */
    private int obtenerIdTallaPorString(String sizeName) {
        if (sizeName == null || sizeName.trim().isEmpty()) {
            return -1;
        }
        try {
            String[] parts = sizeName.trim().split("\\s+");
            if (parts.length < 3) {
                return -1;
            }
            String numero = parts[0];
            String sistema = parts[1];
            String generoCode = parts[2].toUpperCase();

            // Convertir la abreviatura de g??nero a su nombre completo
            String genero;
            switch (generoCode) {
                case "M":
                    genero = "MUJER";
                    break;
                case "H":
                    genero = "HOMBRE";
                    break;
                case "N":
                    genero = "NI??O";
                    break;
                case "U":
                    genero = "UNISEX";
                    break;
                default:
                    genero = generoCode;
                    break;
            }

            try {
                Integer idTalla = new raven.dao.TallasDAO().findIdTalla(numero, sistema, genero);
                if (idTalla != null)
                    return idTalla;
            } catch (Exception e) {
                System.err.println("?????? Error al buscar id de talla: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("?????? Error al parsear talla '" + sizeName + "': " + e.getMessage());
        }
        return -1;
    }

    private boolean esImagenValidaPorBytes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 10) {
            return false;
        }

        try {
            // JPEG: FF D8 FF
            if (imageBytes.length >= 3
                    && (imageBytes[0] & 0xFF) == 0xFF
                    && (imageBytes[1] & 0xFF) == 0xD8
                    && (imageBytes[2] & 0xFF) == 0xFF) {
                return true;
            }

            // PNG: 89 50 4E 47
            if (imageBytes.length >= 4
                    && (imageBytes[0] & 0xFF) == 0x89
                    && (imageBytes[1] & 0xFF) == 0x50
                    && (imageBytes[2] & 0xFF) == 0x4E
                    && (imageBytes[3] & 0xFF) == 0x47) {
                return true;
            }

            // GIF: 47 49 46 38
            if (imageBytes.length >= 4
                    && (imageBytes[0] & 0xFF) == 0x47
                    && (imageBytes[1] & 0xFF) == 0x49
                    && (imageBytes[2] & 0xFF) == 0x46
                    && (imageBytes[3] & 0xFF) == 0x38) {
                return true;
            }

            // BMP: 42 4D
            if (imageBytes.length >= 2
                    && (imageBytes[0] & 0xFF) == 0x42
                    && (imageBytes[1] & 0xFF) == 0x4D) {
                return true;
            }

            // WebP: 52 49 46 46 ... 57 45 42 50
            if (imageBytes.length >= 12
                    && (imageBytes[0] & 0xFF) == 0x52
                    && (imageBytes[1] & 0xFF) == 0x49
                    && (imageBytes[2] & 0xFF) == 0x46
                    && (imageBytes[3] & 0xFF) == 0x46
                    && (imageBytes[8] & 0xFF) == 0x57
                    && (imageBytes[9] & 0xFF) == 0x45
                    && (imageBytes[10] & 0xFF) == 0x42
                    && (imageBytes[11] & 0xFF) == 0x50) {
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error validando imagen: " + e.getMessage());
            return false;
        }
    }

    /*
     * Carga datos iniciales necesarios
     */
    private void loadInitialData() {
        try {
            cargarBodegasEnCombo();
        } catch (Exception e) {
            showError("Error cargando datos iniciales", e);
        }
    }

    // ===================================================================
    // CONFIGURACI??N DE COMPONENTES
    // ===================================================================
    /**
     * Configura la tabla de variantes
     */
    private void setupTableConfiguration() {
        if (tablaProd == null) {
            System.err.println("??? ERROR: tablaProd es null");
            return;
        }

        configurarModeloTabla();
        configurarAparienciaTabla();
        configurarRenderersTabla();
        configurarColumnasTabla();
    }

    private void configurarModeloTabla() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][] {},
                new String[] { "Talla", "Color", "Proveedor", "Cantidad", "Tipo", "Bodega", "Ubicación", "Imagen",
                        "Acciones" }) {
            Class[] types = new Class[] {
                    String.class, String.class, String.class, Integer.class, String.class, String.class, String.class,
                    ImageIcon.class, Object.class
            };
            boolean[] canEdit = new boolean[] { false, false, false, true, false, false, true, false, true };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        tablaProd.setModel(model);
        System.out.println("✅ Modelo de tabla configurado con soporte para imágenes y acciones");
    }

    private void configurarAparienciaTabla() {
        tablaProd.setRowHeight(50);
        tablaProd.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tablaProd.setSelectionBackground(new Color(72, 133, 237));
        tablaProd.setSelectionForeground(Color.WHITE);
        tablaProd.setGridColor(new Color(70, 70, 70));
        tablaProd.setShowGrid(true);
        tablaProd.setBackground(new Color(60, 63, 65));
        tablaProd.setForeground(Color.WHITE);
        tablaProd.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Configurar encabezados
        tablaProd.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaProd.getTableHeader().setBackground(new Color(50, 53, 55));
        tablaProd.getTableHeader().setForeground(Color.WHITE);
        tablaProd.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarRenderersTabla() {
        // Renderer para celdas de texto
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                setHorizontalAlignment(SwingConstants.CENTER);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(60, 63, 65) : new Color(65, 68, 70));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(new Color(72, 133, 237));
                    c.setForeground(Color.WHITE);
                }

                setBorder(null);
                return c;
            }
        };

        // Renderer para imágenes
        DefaultTableCellRenderer imageRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);

                if (value instanceof ImageIcon) {
                    ImageIcon icon = (ImageIcon) value;
                    Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    setIcon(new ImageIcon(img));
                    if (archivosPorFila.containsKey(row)) {
                        setText("Editada");
                    } else {
                        setText("");
                    }
                } else {
                    setIcon(null);
                    setText("Sin imagen");
                }

                setHorizontalAlignment(SwingConstants.CENTER);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(60, 63, 65) : new Color(65, 68, 70));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(new Color(72, 133, 237));
                    c.setForeground(Color.WHITE);
                }

                setBorder(null);
                return c;
            }
        };

        // Aplicar renderers
        for (int i = 0; i < 6; i++) {
            tablaProd.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        // Columna 6 es Ubicación, también texto centrado
        tablaProd.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        // Columna 7 es Imagen
        tablaProd.getColumnModel().getColumn(7).setCellRenderer(imageRenderer);

        // Columna de acciones (si existe)
        if (tablaProd.getColumnModel().getColumnCount() > 8) {
            tablaProd.getColumnModel().getColumn(8).setCellRenderer(new AccionesCellRenderer());
            tablaProd.getColumnModel().getColumn(8).setCellEditor(new AccionesCellEditor());
        }

        tablaProd.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tablaProd.getSelectedRow();
                if (row >= 0) {
                    editingRow = row;
                    Object val = tablaProd.getValueAt(row, 7); // Imagen es ahora índice 7
                    if (val instanceof ImageIcon) {
                        imagenFilaActual = (ImageIcon) val;
                    } else {
                        imagenFilaActual = null;
                    }
                    File f = archivosPorFila.get(row);
                    archivoImagenSeleccionada = f;
                    Object bodegaNombreObj = tablaProd.getValueAt(row, 5);
                    String bodegaNombre = bodegaNombreObj != null ? bodegaNombreObj.toString() : null;
                    if (cbxBodega != null && bodegaNombre != null) {
                        try {
                            cbxBodega.setSelectedItem(bodegaNombre);
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        });

        // Agregar listener para detectar cambios en la tabla
        tablaProd.getModel().addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();

                // Si se edita Cantidad (3) o Ubicación (6)
                if (row >= 0 && (column == 3 || column == 6)) {
                    editingRow = row;
                    DefaultTableModel tm = (DefaultTableModel) e.getSource();
                    String talla = (String) tm.getValueAt(row, 0);
                    String color = (String) tm.getValueAt(row, 1);
                    Object cantObj = tm.getValueAt(row, 3);
                    Integer cantidad = (cantObj instanceof Integer) ? (Integer) cantObj
                            : Integer.parseInt(String.valueOf(cantObj));
                    String tipo = (String) tm.getValueAt(row, 4);
                    String ubicacion = (String) tm.getValueAt(row, 6);

                    actualizarVarianteExistente(tm, talla, color, cantidad, tipo, ubicacion);
                }
            }
        });
    }

    private void configurarColumnasTabla() {
        tablaProd.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaProd.getColumnModel().getColumn(1).setPreferredWidth(70);
        tablaProd.getColumnModel().getColumn(2).setPreferredWidth(140); // Proveedor
        tablaProd.getColumnModel().getColumn(3).setPreferredWidth(70);
        tablaProd.getColumnModel().getColumn(4).setPreferredWidth(90);
        tablaProd.getColumnModel().getColumn(5).setPreferredWidth(120);
        tablaProd.getColumnModel().getColumn(6).setPreferredWidth(100); // Ubicación
        tablaProd.getColumnModel().getColumn(7).setPreferredWidth(80); // Imagen
        if (tablaProd.getColumnModel().getColumnCount() > 8) {
            tablaProd.getColumnModel().getColumn(8).setPreferredWidth(140);
        }
    }

    private void setupGroupingButtons() {
        // Estilo para BtnAgrupar
        BtnAgrupar.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;"
                + "background:#3498db;"
                + "foreground:#FFFFFF;"
                + "font:bold;");
        BtnAgrupar.setIcon(FontIcon.of(FontAwesomeSolid.OBJECT_GROUP, 16, Color.WHITE));

        // Estilo para BtnImagen
        BtnImagen.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;"
                + "background:#2ecc71;"
                + "foreground:#FFFFFF;"
                + "font:bold;");
        BtnImagen.setIcon(FontIcon.of(FontAwesomeSolid.IMAGE, 16, Color.WHITE));
        BtnImagen.setVisible(false); // Oculto inicialmente

        // Estilo para BtnPrecioVenta
        BtnPrecioVenta.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;"
                + "background:#f1c40f;"
                + "foreground:#FFFFFF;"
                + "font:bold;");
        BtnPrecioVenta.setIcon(FontIcon.of(FontAwesomeSolid.TAGS, 16, Color.WHITE));
        BtnPrecioVenta.setVisible(false); // Oculto inicialmente

        // Action Listeners
        BtnAgrupar.addActionListener(e -> toggleGrouping());
        BtnImagen.addActionListener(e -> editarImagenGrupo());
        BtnPrecioVenta.addActionListener(e -> editarPrecioGrupo());
    }

    private void toggleGrouping() {
        if (!isGrouped) {
            aplicarAgrupacion();
        } else {
            desaplicarAgrupacion();
        }
    }

    private void aplicarAgrupacion() {
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        int rowCount = model.getRowCount();
        if (rowCount == 0)
            return;

        // Guardar estado original
        originalData = new Object[rowCount][model.getColumnCount()];
        originalVariantIdMap = new HashMap<>(variantIdPorFila);
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                originalData[i][j] = model.getValueAt(i, j);
            }
        }

        // Agrupar por Proveedor (col 2) y Color (col 1)
        Map<String, List<Integer>> grupos = new LinkedHashMap<>();
        groupedVariantIdsMap.clear();

        for (int i = 0; i < rowCount; i++) {
            String color = String.valueOf(model.getValueAt(i, 1));
            String proveedor = String.valueOf(model.getValueAt(i, 2));
            String key = proveedor + "|" + color;

            if (!grupos.containsKey(key)) {
                grupos.put(key, new ArrayList<>());
            }
            grupos.get(key).add(i);
        }

        // Crear nuevo modelo agrupado
        model.setRowCount(0);
        int newRow = 0;
        for (Map.Entry<String, List<Integer>> entry : grupos.entrySet()) {
            List<Integer> rows = entry.getValue();
            int firstRow = rows.get(0);

            // Combinar tallas
            StringBuilder tallas = new StringBuilder();
            List<Integer> ids = new ArrayList<>();
            for (int r : rows) {
                if (tallas.length() > 0)
                    tallas.append(", ");
                tallas.append(originalData[r][0]);
                Integer id = originalVariantIdMap.get(r);
                if (id != null)
                    ids.add(id);
            }

            Object[] rowData = new Object[model.getColumnCount()];
            System.arraycopy(originalData[firstRow], 0, rowData, 0, model.getColumnCount());
            rowData[0] = tallas.toString(); // Tallas combinadas
            rowData[3] = ""; // Cantidad no aplica a grupo (o suma?)
            rowData[8] = null; // Quitar botones de acciones individuales

            model.addRow(rowData);
            groupedVariantIdsMap.put(newRow, ids);
            newRow++;
        }

        isGrouped = true;
        BtnAgrupar.setText("Desagrupar");
        BtnAgrupar.setBackground(Color.RED);
        BtnImagen.setVisible(true);
        BtnPrecioVenta.setVisible(true);
        BtnCrear_Variante.setEnabled(false);
    }

    private void desaplicarAgrupacion() {
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        model.setRowCount(0);
        variantIdPorFila.clear();

        if (originalData != null) {
            for (int i = 0; i < originalData.length; i++) {
                model.addRow(originalData[i]);
                Integer id = originalVariantIdMap.get(i);
                if (id != null)
                    variantIdPorFila.put(i, id);
            }
        }

        isGrouped = false;
        BtnAgrupar.setText("Agrupar");
        BtnAgrupar.putClientProperty(FlatClientProperties.STYLE, "background:#3498db;");
        BtnImagen.setVisible(false);
        BtnPrecioVenta.setVisible(false);
        BtnCrear_Variante.setEnabled(true);
    }

    private void editarImagenGrupo() {
        int row = tablaProd.getSelectedRow();
        if (row < 0) {
            showWarning("Seleccione un grupo para editar su imagen");
            return;
        }

        List<Integer> ids = groupedVariantIdsMap.get(row);
        if (ids == null || ids.isEmpty()) {
            showWarning("El grupo seleccionado no tiene variantes válidas");
            return;
        }

        JnaFileChooser ch = new JnaFileChooser();
        ch.setMode(JnaFileChooser.Mode.Files);
        ch.addFilter("Imágenes", "png", "jpg", "jpeg");
        if (ch.showOpenDialog(SwingUtilities.getWindowAncestor(this))) {
            File selectedFile = ch.getSelectedFile();
            try {
                byte[] bytes = Files.readAllBytes(selectedFile.toPath());
                if (!esImagenValidaPorBytes(bytes)) {
                    showError("El archivo seleccionado no es una imagen válida", null);
                    return;
                }

                // Actualizar en BD
                ServiceProductVariant spv = new ServiceProductVariant();
                for (int id : ids) {
                    spv.updateVariantImage(id, bytes);
                }

                // Actualizar UI
                ImageIcon icon = new ImageIcon(bytes);
                tablaProd.setValueAt(icon, row, 7);
                showSuccess("Imagen actualizada para " + ids.size() + " variantes");

            } catch (Exception e) {
                showError("Error actualizando imágenes del grupo", e);
            }
        }
    }

    private void editarPrecioGrupo() {
        int row = tablaProd.getSelectedRow();
        if (row < 0) {
            showWarning("Seleccione un grupo para editar su precio");
            return;
        }

        List<Integer> ids = groupedVariantIdsMap.get(row);
        if (ids == null || ids.isEmpty()) {
            showWarning("El grupo seleccionado no tiene variantes válidas");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Ingrese el nuevo precio de venta para el grupo:",
                "Editar Precio Grupo", JOptionPane.QUESTION_MESSAGE);
        if (input != null && !input.trim().isEmpty()) {
            try {
                double nuevoPrecio = Double.parseDouble(input.replace(",", "."));

                // Actualizar en BD
                ServiceProduct service = new ServiceProduct();
                int updated = service.updatePrecioVentaByIds(ids, nuevoPrecio);

                if (updated > 0) {
                    showSuccess("Precio actualizado para " + updated + " variantes");
                    // Opcional: refrescar tabla si fuera necesario, pero el precio no se muestra en
                    // la tabla agrupada actual
                }
            } catch (NumberFormatException e) {
                showError("Precio inválido", null);
            } catch (Exception e) {
                showError("Error actualizando precios del grupo", e);
            }
        }
    }

    private void configurarBtnCrearVarianteModal() {
        if (BtnCrear_Variante != null) {
            for (ActionListener al : BtnCrear_Variante.getActionListeners()) {
                BtnCrear_Variante.removeActionListener(al);
            }
            BtnCrear_Variante.setToolTipText("Crear variante usando modal iOS");
            BtnCrear_Variante.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 16, java.awt.Color.WHITE));
            BtnCrear_Variante.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            BtnCrear_Variante.setIconTextGap(8);
            BtnCrear_Variante.addActionListener(e -> {
                bloquearBotonTemporalmente(BtnCrear_Variante);
                try {
                    if (currentProductId > 0 && currentProduct != null) {
                        if (parentForm != null) {
                            parentForm.openVariantModalForProduct(currentProduct, null);
                        } else {
                            showWarning("No se encontr?? el formulario padre para abrir el modal");
                        }
                    } else {
                        showWarning("Cargue un producto antes de crear variante");
                    }
                } catch (Exception ex) {
                    showError("Error abriendo modal de creaci??n de variante", ex);
                }
            });
        }
    }

    private class AccionesCellRenderer extends javax.swing.JPanel implements TableCellRenderer {
        private final javax.swing.JButton btnEditarAccion = new javax.swing.JButton();
        private final javax.swing.JButton btnEliminarAccion = new javax.swing.JButton();

        AccionesCellRenderer() {
            setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 6, 0));
            btnEditarAccion.setText("");
            btnEditarAccion.setIcon(FontIcon.of(FontAwesomeSolid.PEN, 14, java.awt.Color.WHITE));
            btnEditarAccion.setBackground(new java.awt.Color(52, 152, 219));
            btnEditarAccion.setForeground(java.awt.Color.WHITE);
            btnEditarAccion.setFocusPainted(false);
            btnEditarAccion.setBorderPainted(false);
            btnEditarAccion.setPreferredSize(new java.awt.Dimension(30, 26));
            btnEliminarAccion.setText("");
            btnEliminarAccion.setIcon(FontIcon.of(FontAwesomeSolid.TRASH_ALT, 14, java.awt.Color.WHITE));
            btnEliminarAccion.setBackground(new java.awt.Color(231, 76, 60));
            btnEliminarAccion.setForeground(java.awt.Color.WHITE);
            btnEliminarAccion.setFocusPainted(false);
            btnEliminarAccion.setBorderPainted(false);
            btnEliminarAccion.setPreferredSize(new java.awt.Dimension(30, 26));
            add(btnEditarAccion);
            add(btnEliminarAccion);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            raven.clases.admin.UserSession userSession = raven.clases.admin.UserSession.getInstance();
            btnEditarAccion.setVisible(userSession.hasPermission("variante_editar"));
            btnEliminarAccion.setVisible(userSession.hasPermission("variante_eliminar"));

            setOpaque(true);
            setBackground(isSelected ? new java.awt.Color(72, 133, 237) : new java.awt.Color(60, 63, 65));
            return this;
        }
    }

    private void bloquearBotonTemporalmente(javax.swing.JButton btn) {
        if (btn == null)
            return;
        btn.setEnabled(false);
        new javax.swing.Timer(1000, e -> {
            if (btn.isVisible()) {
                btn.setEnabled(true);
            }
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
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

        javax.swing.JLabel lbl = new javax.swing.JLabel("Procesando...", javax.swing.SwingConstants.CENTER);
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

    private class AccionesCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final javax.swing.JPanel panel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 6, 0));
        private final javax.swing.JButton btnEditarAccion = new javax.swing.JButton();
        private final javax.swing.JButton btnEliminarAccion = new javax.swing.JButton();
        private int editingRowIndex = -1;

        AccionesCellEditor() {
            btnEditarAccion.setText("");
            btnEditarAccion.setIcon(FontIcon.of(FontAwesomeSolid.PEN, 14, java.awt.Color.WHITE));
            btnEditarAccion.setBackground(new java.awt.Color(52, 152, 219));
            btnEditarAccion.setForeground(java.awt.Color.WHITE);
            btnEditarAccion.setFocusPainted(false);
            btnEditarAccion.setBorderPainted(false);
            btnEditarAccion.setPreferredSize(new java.awt.Dimension(30, 26));
            btnEliminarAccion.setText("");
            btnEliminarAccion.setIcon(FontIcon.of(FontAwesomeSolid.TRASH_ALT, 14, java.awt.Color.WHITE));
            btnEliminarAccion.setBackground(new java.awt.Color(231, 76, 60));
            btnEliminarAccion.setForeground(java.awt.Color.WHITE);
            btnEliminarAccion.setFocusPainted(false);
            btnEliminarAccion.setBorderPainted(false);
            btnEliminarAccion.setPreferredSize(new java.awt.Dimension(30, 26));
            panel.add(btnEditarAccion);
            panel.add(btnEliminarAccion);

            // Verificar permisos para habilitar/deshabilitar listener o visibilidad
            raven.clases.admin.UserSession userSession = raven.clases.admin.UserSession.getInstance();
            boolean canEdit = userSession.hasPermission("variante_editar");
            boolean canDelete = userSession.hasPermission("variante_eliminar");

            btnEditarAccion.setVisible(canEdit);
            btnEliminarAccion.setVisible(canDelete);

            if (canEdit) {
                btnEditarAccion.addActionListener(e -> {
                    bloquearBotonTemporalmente(btnEditarAccion);

                    javax.swing.JDialog loading = createLoadingDialog(panel);

                    new javax.swing.SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            // Simular pequeña pausa para dar feedback visual
                            // Thread.sleep(400); // Eliminado para mejorar velocidad
                            return null;
                        }

                        @Override
                        protected void done() {
                            loading.dispose();
                            try {
                                Integer varId = variantIdPorFila.get(editingRowIndex);
                                if (currentProductId > 0 && varId != null && varId > 0) {
                                    if (parentForm != null) {
                                        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
                                        String tipoFila = String.valueOf(model.getValueAt(editingRowIndex, 4));
                                        Integer bodId = bodegaIdPorFila.get(editingRowIndex);
                                        Integer cantidad = 0;
                                        try {
                                            Object v = model.getValueAt(editingRowIndex, 3);
                                            if (v instanceof Number)
                                                cantidad = ((Number) v).intValue();
                                            else
                                                cantidad = Integer.parseInt(String.valueOf(v));
                                        } catch (Exception ignore) {
                                        }
                                        parentForm.openVariantModalForProduct(currentProduct, varId, tipoFila, bodId,
                                                cantidad);
                                    } else {
                                        showWarning("No se encontró el formulario padre para abrir el modal");
                                    }
                                } else {
                                    showWarning("Fila sin variante válida para editar");
                                }
                            } catch (Exception ex) {
                                showError("Error abriendo modal de edición", ex);
                            } finally {
                                fireEditingStopped();
                            }
                        }
                    }.execute();

                    loading.setVisible(true);
                });
            }

            if (canDelete) {
                btnEliminarAccion.addActionListener(e -> {
                    bloquearBotonTemporalmente(btnEliminarAccion);
                    try {
                        if (editingRowIndex >= 0) {
                            tablaProd.setRowSelectionInterval(editingRowIndex, editingRowIndex);
                            eliminarVarianteSeleccionada();
                        }
                    } catch (Exception ex) {
                        showError("Error eliminando variante", ex);
                    } finally {
                        fireEditingStopped();
                    }
                });
            }
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value,
                boolean isSelected, int row, int column) {
            editingRowIndex = row;
            panel.setOpaque(true);
            panel.setBackground(isSelected ? new java.awt.Color(72, 133, 237) : new java.awt.Color(60, 63, 65));
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    public void cargarDatosProducto(ServiceProduct service, ModelProduct data) {
        try {
            System.out.println("???? Cargando datos del producto para edici??n...");

            cargarCombosDesdeServicio(service, data);

            if (data != null) {
                currentProduct = data;
                currentProductId = data.getProductId();
                isEditMode = true; // ??? ACTIVAR MODO EDICI??N

                System.out.println("???? Cargando campos del producto: " + data.getName());
                cargarCamposProducto(data);

                // OPTIMIZACIÓN: Este método está deprecado. Usar cargarDatosProductoCompletos()
                // en su lugar
                // cargarVariantesDesdeDBConImagenes(data.getProductId());
                // cargarPreciosPorTipoDesdeDB(data.getProductId());

                System.out.println(
                        "⚠️ ADVERTENCIA: cargarDatosProducto() está deprecado. Usar cargarDatosProductoBasicos() + cargarDatosProductoCompletos()");

                System.out.println("??? Carga completa finalizada para producto ID: " + data.getProductId());
            } else {
                isEditMode = false; // Modo creaci??n
                System.out.println("?????? Nuevo producto - no hay datos que cargar");
            }

        } catch (SQLException e) {
            String errorMsg = "Error cargando datos del producto: " + e.getMessage();
            showError(errorMsg, e);
            System.err.println("??? " + errorMsg);
            e.printStackTrace();
        } catch (Exception e) {
            String errorMsg = "Error inesperado cargando producto: " + e.getMessage();
            showError(errorMsg, e);
            System.err.println("??? " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * OPTIMIZACIÓN: Carga solo los datos básicos del producto de forma síncrona.
     * Esto permite que el modal se abra rápidamente con información visible.
     * Los datos pesados (combos, variantes, precios) se cargan después con
     * cargarDatosProductoCompletos.
     */
    public void cargarDatosProductoBasicos(ModelProduct data) {
        try {
            if (data != null) {
                currentProduct = data;
                currentProductId = data.getProductId();
                isEditMode = true;

                // Cargar campos básicos del producto (rápido, síncrono)
                cargarCamposProducto(data);

                // OPTIMIZACIÓN: Asegurar que el panel de variantes sea visible
                SwingUtilities.invokeLater(() -> {
                    if (panelVariante != null) {
                        panelVariante.setVisible(true);
                        if (tablaProd != null) {
                            tablaProd.setVisible(true);
                        }
                        if (jScrollPane5 != null) {
                            jScrollPane5.setVisible(true);
                        }
                        panelVariante.revalidate();
                        panelVariante.repaint();
                    }
                });

                System.out.println("✅ Datos básicos cargados para producto: " + data.getName());
            } else {
                isEditMode = false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error cargando datos básicos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * OPTIMIZACIÓN: Carga los datos pesados (combos, variantes, precios) de forma
     * asíncrona.
     * Se ejecuta después de abrir el modal para no bloquear la UI.
     */
    public void cargarDatosProductoCompletos(ServiceProduct service, ModelProduct data) {
        try {
            System.out.println("🔄 Cargando datos completos del producto (async)...");

            // Cargar combos de forma asíncrona
            cargarCombosDesdeServicio(service, data);

            if (data != null && data.getProductId() > 0) {
                // Cargar variantes con imágenes de forma asíncrona (sin diálogo modal
                // bloqueante)
                cargarVariantesDesdeDBConImagenesOptimizado(data.getProductId());

                // Cargar precios por tipo de forma asíncrona
                cargarPreciosPorTipoDesdeDB(data.getProductId());

                // Capturar estado inicial después de cargar variantes
                SwingUtilities.invokeLater(() -> {
                    try {
                        changeTracker.captureInitialState(
                                (DefaultTableModel) tablaProd.getModel(),
                                variantIdPorFila,
                                archivosPorFila,
                                bodegaIdPorFila);
                    } catch (Exception e) {
                        System.err.println("⚠️ Error capturando estado inicial: " + e.getMessage());
                    }
                });

                System.out.println("✅ Carga completa iniciada para producto ID: " + data.getProductId());
            }
        } catch (Exception e) {
            String errorMsg = "Error cargando datos completos del producto: " + e.getMessage();
            showError(errorMsg, e);
            System.err.println("❌ " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * @deprecated Este método muestra un diálogo modal bloqueante.
     *             Usar cargarVariantesDesdeDBConImagenesOptimizado() en su lugar.
     */
    @Deprecated
    private void cargarVariantesDesdeDBConImagenes(int productId) {
        // OPTIMIZACIÓN: Redirigir al método optimizado para evitar bloqueos
        System.out.println(
                "⚠️ ADVERTENCIA: cargarVariantesDesdeDBConImagenes() está deprecado. Usando versión optimizada.");
        cargarVariantesDesdeDBConImagenesOptimizado(productId);
    }

    /**
     * OPTIMIZACIÓN: Versión optimizada que carga variantes sin mostrar diálogo
     * modal bloqueante.
     * En su lugar, muestra un indicador de carga dentro del formulario.
     */
    private void cargarVariantesDesdeDBConImagenesOptimizado(int productId) {
        // Evitar cargas duplicadas
        if (isLoadingVariants) {
            System.out.println("⚠️ Carga de variantes ya en progreso, ignorando solicitud duplicada");
            return;
        }

        isLoadingVariants = true;
        // OPTIMIZACIÓN: Deshabilitar overlay temporalmente para evitar problemas de
        // layout
        // mostrarOverlayCarga();

        // Ejecutar en segundo plano sin diálogo modal bloqueante
        new javax.swing.SwingWorker<java.util.List<Object[]>, Void>() {
            private final Integer idBodegaSel = obtenerIdBodegaSeleccionada();
            private java.util.List<raven.clases.productos.ServiceProduct.VariantBodegaItem> items;

            @Override
            protected java.util.List<Object[]> doInBackground() throws Exception {
                // Preparar datos para la tabla
                java.util.List<Object[]> filasTabla = new java.util.ArrayList<>();
                java.util.Set<String> presentKeys = new java.util.HashSet<>();

                System.out.println("🚀 Cargando variantes (Async Optimizado) para producto ID: " + productId);

                // Limpiar mapas previos (se hará efectivo en done())
                raven.clases.productos.ServiceProduct sp = new raven.clases.productos.ServiceProduct();
                items = sp.getVariantesConBodegaPorProducto(productId, idBodegaSel);

                // Procesar items y preparar objetos para la UI
                for (raven.clases.productos.ServiceProduct.VariantBodegaItem it : items) {
                    // Evitar duplicados
                    if (presentKeys.contains(String.valueOf(it.idVariante))) {
                        continue;
                    }

                    // OPTIMIZACIÓN: Procesar imagen de forma más eficiente
                    // Usar imagen del item si está disponible, evitar consulta extra si es posible
                    ImageIcon imagen = null;
                    byte[] imgBytes = it.imagen; // Usar imagen del item directamente

                    // Solo consultar BD si no hay imagen en el item
                    if (imgBytes == null || imgBytes.length == 0) {
                        imgBytes = sp.getImagenDeVariante(it.idVariante);
                    }

                    if (imgBytes != null && imgBytes.length > 0) {
                        try {
                            // OPTIMIZACIÓN: Procesar imagen de forma más rápida
                            // Usar ImageIO para mejor rendimiento con imágenes grandes
                            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imgBytes);
                            BufferedImage bufferedImage = ImageIO.read(bais);
                            if (bufferedImage != null) {
                                // Redimensionar de forma más eficiente
                                Image scaledImage = bufferedImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                imagen = new ImageIcon(scaledImage);
                            }
                        } catch (Exception e) {
                            // Fallback al método anterior si falla
                            try {
                                ImageIcon io = new ImageIcon(imgBytes);
                                if (io.getImage() != null) {
                                    Image sc = io.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                    imagen = new ImageIcon(sc);
                                }
                            } catch (Exception e2) {
                                System.err.println("Error procesando imagen variante " + it.idVariante);
                            }
                        }
                    }

                    String nombreBodega = it.bodegaNombre != null ? it.bodegaNombre : obtenerNombreBodegaSeleccionada();

                    Integer idInventario = it.idInventarioBodega;
                    if (idInventario == 0) {
                        idInventario = obtenerIdInventarioBodega(it.idVariante, it.bodegaId);
                    }

                    // Lógica para determinar si mostrar Par o Caja
                    Boolean mostrarParesObj = null;
                    String tipoPreferido = null;
                    try {
                        tipoPreferido = tipoPreferidoPorVariante.get(it.idVariante);
                    } catch (Exception ignore) {
                    }

                    if (tipoPreferido != null) {
                        if ("Par".equalsIgnoreCase(tipoPreferido)) {
                            mostrarParesObj = Boolean.TRUE;
                        } else if ("Caja".equalsIgnoreCase(tipoPreferido)) {
                            mostrarParesObj = Boolean.FALSE;
                        }
                    }

                    if (mostrarParesObj == null) {
                        if (it.stockPares > 0 && it.stockCajas == 0) {
                            mostrarParesObj = Boolean.TRUE;
                        } else if (it.stockCajas > 0 && it.stockPares == 0) {
                            mostrarParesObj = Boolean.FALSE;
                        } else {
                            mostrarParesObj = Boolean.TRUE;
                        }
                    }

                    boolean mostrarPares = mostrarParesObj;
                    String ubicacionEspItem = it.ubicacionEspecifica;

                    if (mostrarPares) {
                        filasTabla.add(new Object[] {
                                it.talla, // 0 - Talla
                                it.color, // 1 - Color
                                it.proveedorNombre != null ? it.proveedorNombre : "Sin proveedor", // 2 - Proveedor
                                it.stockPares, // 3 - Cantidad
                                "Par", // 4 - Tipo
                                nombreBodega, // 5 - Bodega
                                ubicacionEspItem, // 6 - Ubicación
                                imagen, // 7 - Imagen
                                null, // 8 - Acciones
                                it.idVariante, // 9 - ID Variante (metadata)
                                idInventario, // 10 - ID Inventario (metadata)
                                it.bodegaId, // 11 - ID Bodega (metadata)
                                imgBytes, // 12 - Bytes Imagen (metadata)
                                ubicacionEspItem // 13 - Ubicación (metadata)
                        });
                        if (it.idVariante > 0) {
                            tipoPreferidoPorVariante.put(it.idVariante, "Par");
                        }
                    } else {
                        filasTabla.add(new Object[] {
                                it.talla, // 0 - Talla
                                it.color, // 1 - Color
                                it.proveedorNombre != null ? it.proveedorNombre : "Sin proveedor", // 2 - Proveedor
                                it.stockCajas, // 3 - Cantidad
                                "Caja", // 4 - Tipo
                                nombreBodega, // 5 - Bodega
                                ubicacionEspItem, // 6 - Ubicación
                                imagen, // 7 - Imagen
                                null, // 8 - Acciones
                                it.idVariante, // 9 - ID Variante (metadata)
                                idInventario, // 10 - ID Inventario (metadata)
                                it.bodegaId, // 11 - ID Bodega (metadata)
                                imgBytes, // 12 - Bytes Imagen (metadata)
                                ubicacionEspItem // 13 - Ubicación (metadata)
                        });
                        if (it.idVariante > 0) {
                            tipoPreferidoPorVariante.put(it.idVariante, "Caja");
                        }
                    }
                    presentKeys.add(String.valueOf(it.idVariante));
                }

                // Fallback: asegurar incluir variantes con stock 0
                try {
                    java.util.List<Object[]> faltantes = cargarVariantesConCeroFallback(productId, idBodegaSel);
                    for (Object[] datos : faltantes) {
                        Integer varId = (Integer) datos[8];
                        String key = String.valueOf(varId);
                        if (!presentKeys.contains(key)) {
                            filasTabla.add(datos);
                            presentKeys.add(key);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("WARN Fallback variantes con cero: " + ex.getMessage());
                }
                return filasTabla;
            }

            @Override
            protected void done() {
                try {
                    java.util.List<Object[]> filas = get();

                    java.util.Map<String, Object[]> mapaFilas = new java.util.LinkedHashMap<>();
                    int contadorNuevos = 0;
                    for (Object[] datos : filas) {
                        Integer varId = null;
                        if (datos != null && datos.length > 9 && datos[9] instanceof Integer) {
                            varId = (Integer) datos[9];
                        }
                        String key;
                        if (varId != null && varId > 0) {
                            key = "V" + varId;
                        } else {
                            contadorNuevos++;
                            key = "N" + contadorNuevos;
                        }
                        if (!mapaFilas.containsKey(key)) {
                            mapaFilas.put(key, datos);
                        }
                    }

                    DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
                    model.setRowCount(0);

                    // Limpiar mapas
                    archivosPorFila.clear();
                    variantIdPorFila.clear();
                    bodegaIdPorFila.clear();
                    inventarioBodegaIdPorFila.clear();
                    ubicacionEspecificaPorFila.clear();

                    int fila = 0;
                    for (Object[] datos : mapaFilas.values()) {
                        // Agregar a tabla (solo las columnas visibles: 0..8)
                        Object[] rowData = new Object[9];
                        System.arraycopy(datos, 0, rowData, 0, 9);
                        model.addRow(rowData);

                        // Recuperar metadata (a partir de índice 9)
                        Integer varId = (Integer) datos[9];
                        Integer invId = (Integer) datos[10];
                        Integer bodId = (Integer) datos[11];
                        byte[] imgBytes = (byte[]) datos[12];
                        String ubicacionEsp = (datos.length > 13) ? (String) datos[13] : null;

                        // Actualizar mapas
                        variantIdPorFila.put(fila, varId);
                        inventarioBodegaIdPorFila.put(fila, invId);
                        bodegaIdPorFila.put(fila, bodId != null ? bodId : getCurrentBodegaId());
                        ubicacionEspecificaPorFila.put(fila, ubicacionEsp);

                        // Crear archivo temporal si hay imagen
                        if (imgBytes != null) {
                            crearArchivoTemporalDesdeBytes(imgBytes, fila, varId);
                        }

                        fila++;
                    }

                    System.out.println("✅ Variantes cargadas exitosamente (optimizado): " + fila);

                    // OPTIMIZACIÓN: Asegurar que todo sea visible después de cargar
                    if (tablaProd != null) {
                        tablaProd.setVisible(true);
                        tablaProd.revalidate();
                        tablaProd.repaint();
                    }
                    if (jScrollPane5 != null) {
                        jScrollPane5.setVisible(true);
                        jScrollPane5.revalidate();
                        jScrollPane5.repaint();
                    }
                    if (panelVariante != null) {
                        panelVariante.setVisible(true);
                        panelVariante.revalidate();
                        panelVariante.repaint();
                    }

                } catch (Exception e) {
                    showError("Error cargando variantes", e);
                    e.printStackTrace();
                } finally {
                    isLoadingVariants = false;
                    // OPTIMIZACIÓN: Asegurar que la tabla sea visible después de cargar
                    SwingUtilities.invokeLater(() -> {
                        if (tablaProd != null) {
                            tablaProd.setVisible(true);
                            java.awt.Container parent = tablaProd.getParent();
                            if (parent != null) {
                                parent.setVisible(true);
                                java.awt.Container grandParent = parent.getParent();
                                if (grandParent != null && grandParent instanceof javax.swing.JPanel) {
                                    javax.swing.JPanel panelVariante = (javax.swing.JPanel) grandParent;
                                    panelVariante.setVisible(true);
                                    panelVariante.revalidate();
                                    panelVariante.repaint();
                                }
                            }
                        }
                    });
                    // ocultarOverlayCarga();
                }
            }
        }.execute();
    }

    /**
     * OPTIMIZACIÓN: Muestra un overlay de carga sobre la tabla de variantes.
     * Usa un enfoque que no interfiere con el layout existente.
     */
    private void mostrarOverlayCarga() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (loadingOverlay == null) {
                    loadingOverlay = new javax.swing.JPanel() {
                        @Override
                        protected void paintComponent(java.awt.Graphics g) {
                            super.paintComponent(g);
                            java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                            g2d.setComposite(java.awt.AlphaComposite.getInstance(
                                    java.awt.AlphaComposite.SRC_OVER, 0.7f));
                            g2d.setColor(java.awt.Color.BLACK);
                            g2d.fillRect(0, 0, getWidth(), getHeight());
                            g2d.dispose();
                        }
                    };
                    loadingOverlay.setLayout(new java.awt.BorderLayout());
                    loadingOverlay.setOpaque(false);

                    javax.swing.JPanel contentPanel = new javax.swing.JPanel();
                    contentPanel.setLayout(new java.awt.BorderLayout(10, 10));
                    contentPanel.setOpaque(false);
                    contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

                    javax.swing.JLabel label = new javax.swing.JLabel("Cargando variantes...",
                            javax.swing.SwingConstants.CENTER);
                    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 14f));
                    label.setForeground(java.awt.Color.WHITE);

                    javax.swing.JProgressBar progressBar = new javax.swing.JProgressBar();
                    progressBar.setIndeterminate(true);
                    progressBar.setPreferredSize(new java.awt.Dimension(200, 20));

                    contentPanel.add(label, java.awt.BorderLayout.NORTH);
                    contentPanel.add(progressBar, java.awt.BorderLayout.CENTER);

                    loadingOverlay.add(contentPanel, java.awt.BorderLayout.CENTER);
                }

                // Buscar el JScrollPane que contiene la tabla
                java.awt.Container parent = tablaProd.getParent();
                while (parent != null && !(parent instanceof javax.swing.JScrollPane)) {
                    parent = parent.getParent();
                }

                if (parent instanceof javax.swing.JScrollPane) {
                    javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) parent;

                    // Usar un enfoque con JLayeredPane para no interferir con el layout
                    java.awt.Container scrollParent = scrollPane.getParent();
                    if (scrollParent != null) {
                        // Convertir a JLayeredPane si es posible, o usar un enfoque alternativo
                        if (scrollParent instanceof javax.swing.JLayeredPane) {
                            javax.swing.JLayeredPane layeredPane = (javax.swing.JLayeredPane) scrollParent;
                            layeredPane.add(loadingOverlay, javax.swing.JLayeredPane.POPUP_LAYER);
                            loadingOverlay.setBounds(scrollPane.getBounds());
                            loadingOverlay.setVisible(true);
                        } else {
                            // OPTIMIZACIÓN: Simplificar - solo mostrar overlay sin cambiar layouts
                            // Agregar el overlay directamente sobre el scrollPane usando setBounds
                            if (loadingOverlay.getParent() == null) {
                                scrollParent.add(loadingOverlay, 0); // Agregar al inicio para que esté encima
                            }
                            // Asegurar que el overlay tenga el mismo tamaño que el scrollPane
                            java.awt.Rectangle bounds = scrollPane.getBounds();
                            loadingOverlay.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
                            loadingOverlay.setVisible(true);
                            loadingOverlay.revalidate();
                            loadingOverlay.repaint();
                            scrollParent.revalidate();
                            scrollParent.repaint();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error mostrando overlay de carga: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * OPTIMIZACIÓN: Oculta el overlay de carga.
     */
    private void ocultarOverlayCarga() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisible(false);
                    java.awt.Container parent = loadingOverlay.getParent();
                    if (parent != null) {
                        parent.remove(loadingOverlay);
                        // Asegurar que el layout se restaure correctamente
                        parent.revalidate();
                        parent.repaint();
                    }
                    // También asegurar que la tabla sea visible
                    if (tablaProd != null) {
                        tablaProd.setVisible(true);
                        java.awt.Container tableParent = tablaProd.getParent();
                        if (tableParent != null) {
                            tableParent.setVisible(true);
                            tableParent.revalidate();
                            tableParent.repaint();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error ocultando overlay de carga: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void verificarEstadoCarga() {
        System.out.println("\n???? VERIFICACI??N DE ESTADO DESPU??S DE CARGA:");
        System.out.println("=".repeat(50));

        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        System.out.println("???? Filas en tabla: " + model.getRowCount());
        System.out.println("???? Archivos temporales: " + archivosPorFila.size());

        for (int i = 0; i < model.getRowCount(); i++) {
            String talla = (String) model.getValueAt(i, 0);
            String color = (String) model.getValueAt(i, 1);
            Object imagen = model.getValueAt(i, 6);
            File archivo = archivosPorFila.get(i);

            System.out.printf("Fila %d: %s - %s | Imagen: %s | Archivo: %s%n",
                    i, talla, color,
                    (imagen != null ? "S??" : "NO"),
                    (archivo != null && archivo.exists() ? "S??" : "NO"));
        }

        System.out.println("=".repeat(50));
    }

    // ===================================================================
    // GESTI??N DE DATOS - COLORES Y TALLAS
    // ===================================================================

    // ===================================================================
    // GESTI??N DE VARIANTES
    // ===================================================================

    private void actualizarVarianteExistente(DefaultTableModel model, String talla,
            String color, int cantidad, String tipo, String ubicacionEspecifica) {
        try {
            if (editingRow < 0 || editingRow >= model.getRowCount()) {
                showError("Error: Fila de edición inválida", null);
                return;
            }

            System.out.println("🚀 Actualizando variante en fila " + editingRow);

            // ===================================================================
            // ACTUALIZAR TABLA VISUAL
            // ===================================================================
            // No es necesario actualizar model.setValueAt porque esto se llama DESPUÉS de
            // la edición
            // pero si hay lógica adicional visual, iría aquí.

            // Actualizar imagen si hay una nueva
            if (imagenFilaActual != null) {
                model.setValueAt(imagenFilaActual, editingRow, 7); // Imagen es índice 7

                if (archivoImagenSeleccionada != null) {
                    archivosPorFila.put(editingRow, archivoImagenSeleccionada);
                }
            }

            // ===================================================================
            // 🚀 ACTUALIZAR EN BASE DE DATOS (CRÍTICO)
            // ===================================================================
            Integer idVariante = variantIdPorFila.get(editingRow);
            Integer idBodegaActual = obtenerIdBodegaSeleccionada();
            Integer idInventarioBodega = inventarioBodegaIdPorFila.get(editingRow); // 🚀 NUEVO

            if (idVariante != null && idVariante > 0 && idBodegaActual != null && idBodegaActual > 0) {

                if (tipo != null && !tipo.trim().isEmpty()) {
                    tipoPreferidoPorVariante.put(idVariante, tipo.trim());
                }

                System.out.println("🔄 Actualizando inventario en BD:");
                System.out.println("   id_variante: " + idVariante);
                System.out.println("   id_bodega: " + idBodegaActual);
                System.out.println("   id_inventario_bodega: " + idInventarioBodega);
                System.out.println("   tipo: " + tipo);
                System.out.println("   cantidad: " + cantidad);
                System.out.println("   ubicación: " + ubicacionEspecifica);

                // =====================================================================
                // ACTUALIZAR inventario_bodega
                // =====================================================================
                String sqlUpdate = "UPDATE inventario_bodega SET " +
                        "Stock_par = ?, " +
                        "Stock_caja = ?, " +
                        "ubicacion_especifica = ?, " +
                        "fecha_ultimo_movimiento = NOW() " +
                        "WHERE id_variante = ? AND id_bodega = ?";

                try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                        java.sql.PreparedStatement pst = conn.prepareStatement(sqlUpdate)) {

                    // Determinar stock según tipo
                    int stockPar = 0;
                    int stockCaja = 0;

                    if ("Par".equalsIgnoreCase(tipo)) {
                        stockPar = cantidad;
                    } else if ("Caja".equalsIgnoreCase(tipo)) {
                        stockCaja = cantidad;
                    }

                    pst.setInt(1, stockPar);
                    pst.setInt(2, stockCaja);
                    pst.setString(3, ubicacionEspecifica != null ? ubicacionEspecifica : "");
                    pst.setInt(4, idVariante);
                    pst.setInt(5, idBodegaActual);

                    int rowsUpdated = pst.executeUpdate();

                    if (rowsUpdated > 0) {
                        System.out.println("✅ Inventario actualizado en BD correctamente");

                        // Actualizar bodega en el mapa
                        bodegaIdPorFila.put(editingRow, idBodegaActual);
                        // Actualizar mapa de ubicación
                        ubicacionEspecificaPorFila.put(editingRow, ubicacionEspecifica);

                    } else {
                        System.err.println("⚠️ No se actualizó ningún registro (puede que no exista)");

                        // =========================================================================================
                        // Si no existe, INSERTAR nuevo registro
                        // =========================================================================================
                        String sqlInsert = "INSERT INTO inventario_bodega " +
                                "(id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, activo, fecha_ultimo_movimiento, ubicacion_especifica) "
                                +
                                "VALUES (?, ?, ?, ?, 0, 1, NOW(), ?)";

                        try (java.sql.PreparedStatement pstInsert = conn.prepareStatement(sqlInsert,
                                java.sql.Statement.RETURN_GENERATED_KEYS)) {

                            pstInsert.setInt(1, idBodegaActual);
                            pstInsert.setInt(2, idVariante);
                            pstInsert.setInt(3, stockPar);
                            pstInsert.setInt(4, stockCaja);
                            pstInsert.setString(5, ubicacionEspecifica != null ? ubicacionEspecifica : "");

                            int insertedRows = pstInsert.executeUpdate();

                            if (insertedRows > 0) {
                                // Obtener el ID generado
                                try (java.sql.ResultSet rs = pstInsert.getGeneratedKeys()) {
                                    if (rs.next()) {
                                        int newId = rs.getInt(1);
                                        inventarioBodegaIdPorFila.put(editingRow, newId);
                                        ubicacionEspecificaPorFila.put(editingRow, ubicacionEspecifica);
                                        System.out.println("✅ Nuevo registro de inventario creado con ID: " + newId);
                                    }
                                }
                            }
                        }
                    }

                } catch (java.sql.SQLException sqlEx) {
                    System.err.println("❌ Error SQL actualizando inventario: " + sqlEx.getMessage());
                    sqlEx.printStackTrace();
                    showError("Error actualizando inventario en base de datos", sqlEx);
                    return;
                }

            } else {
                System.err.println("⚠️ Datos insuficientes para actualizar BD:");
                System.err.println("   idVariante: " + idVariante);
                System.err.println("   idBodega: " + idBodegaActual);
            }

            // ===================================================================
            // FINALIZAR EDICIÓN
            // ===================================================================
            // tablaProd.revalidate(); // Puede causar flickering excesivo
            // tablaProd.repaint();

            // showSuccess("✅ Variante actualizada"); // Demasiado intrusivo para cada
            // edición
            System.out.println("✅ Actualización completada exitosamente");

            reconstruirGruposDesdeTabla();
            validarRequerimientoPreciosSegunTabla();
            tablaModificada = true;

        } catch (Exception e) {
            showError("Error actualizando variante", e);
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * AGREGAR NUEVA VARIANTE - VERSI??N CORREGIDA
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private void agregarNuevaVariante(DefaultTableModel model, String talla,
            String color, int cantidad, String tipo) {
        try {
            System.out.println("???? Agregando nueva variante: " + talla + " - " + color);

            // ??? OBTENER BODEGA SELECCIONADA
            String bodegaNombre = obtenerNombreBodegaSeleccionada();
            Integer idBodega = obtenerIdBodegaSeleccionada();

            System.out.println("???? Bodega seleccionada: " + bodegaNombre + " (ID: " + idBodega + ")");

            // ??? VALIDAR BODEGA
            if (idBodega == null || idBodega <= 0) {
                showWarning("Debe seleccionar una bodega v??lida antes de agregar la variante");
                return;
            }

            if (existeVarianteEnTabla(talla, color, tipo, bodegaNombre)) {
                showWarning("Esta variante ya existe en la tabla para esta bodega");
                return;
            }

            String proveedorNombre = "Sin proveedor";
            if (comboProveedor != null && comboProveedor.getSelectedItem() instanceof ModelSupplier supSel) {
                proveedorNombre = supSel.toString();
            }

            // Crear fila con imagen y columna de ubicación vacía
            Object[] nuevaFila = {
                    talla,
                    color,
                    proveedorNombre,
                    cantidad,
                    tipo,
                    bodegaNombre,
                    "", // Ubicación específica (editable)
                    imagenFilaActual,
                    null // Acciones
            };
            int indiceFila = model.getRowCount();

            // Agregar a la tabla
            model.addRow(nuevaFila);

            // ??? CR??TICO: GUARDAR ID DE BODEGA EN EL MAPA
            bodegaIdPorFila.put(indiceFila, idBodega);
            ubicacionEspecificaPorFila.put(indiceFila, "");

            // Asociar archivo de imagen si existe
            if (archivoImagenSeleccionada != null) {
                archivosPorFila.put(indiceFila, archivoImagenSeleccionada);
                System.out.println("???? Archivo asociado a fila " + indiceFila);
            }

            System.out.println("??? Variante agregada en fila " + indiceFila + " con bodega ID " + idBodega);

            // ??? FORZAR ACTUALIZACI??N VISUAL
            tablaProd.revalidate();
            tablaProd.repaint();

            showSuccess("Variante agregada: " + talla + " - " + color + " en " + bodegaNombre);
            reconstruirGruposDesdeTabla();
            validarRequerimientoPreciosSegunTabla();
            tablaModificada = true;

        } catch (Exception e) {
            showError("Error agregando variante", e);
            System.err.println("??? Error en agregarNuevaVariante: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean existeVarianteEnTabla(String talla, String color, String tipo, String bodegaNombre) {
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object t = model.getValueAt(i, 0);
            Object c = model.getValueAt(i, 1);
            Object tp = model.getValueAt(i, 4);
            Object b = model.getValueAt(i, 5);

            boolean igualTalla = (talla == null ? t == null : talla.equals(t));
            boolean igualColor = (color == null ? c == null : color.equals(c));
            boolean igualTipo = (tipo == null ? tp == null : tipo.equals(tp));
            boolean igualBodega = (bodegaNombre == null ? b == null : bodegaNombre.equals(b));

            if (igualTalla && igualColor && igualTipo && igualBodega) {
                return true;
            }
        }
        return false;
    }

    private void seleccionarImagenConJFileChooser() {
        System.out.println("???? Usando FileDialog nativo...");
        try {
            java.awt.FileDialog fd = new java.awt.FileDialog(
                    (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Seleccionar Imagen",
                    java.awt.FileDialog.LOAD);
            fd.setFile("*.jpg;*.jpeg;*.png;*.gif");
            fd.setVisible(true);

            if (fd.getFile() != null) {
                java.io.File file = new java.io.File(fd.getDirectory(), fd.getFile());
                if (file.exists()) {
                    System.out.println("???? Archivo seleccionado con FileDialog: " + file.getName());
                    // Asignar a variable de clase si es necesario
                    archivoImagenSeleccionada = file;
                    // Actualizar preview si existe
                    // (Lógica adicional dependería de componentes visuales no identificados
                    // claramente)
                }
            }
        } catch (Exception e) {
            System.err.println("??? Error con FileDialog: " + e.getMessage());
            showError("Error al seleccionar imagen", e);
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ELIMINACI??N COMPLETA DE VARIANTE (VISUAL + BASE DE DATOS)
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * 
     * PROCESO:
     * 1. Obtener datos de la fila (id_variante, id_bodega)
     * 2. Desactivar registro en inventario_bodega (soft delete)
     * 3. Si no tiene stock en otras bodegas, marcar variante como no disponible
     * 4. Eliminar fila de la tabla visual
     * 5. Actualizar mapas de seguimiento
     * 
     */
    private void eliminarVarianteSeleccionada() {
        int selectedRow = tablaProd.getSelectedRow();

        if (selectedRow < 0) {
            showWarning("Debe seleccionar una fila para eliminar");
            return;
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // OBTENER DATOS DE LA FILA
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        String talla = (String) model.getValueAt(selectedRow, 0);
        String color = (String) model.getValueAt(selectedRow, 1);
        Integer cantidad = (Integer) model.getValueAt(selectedRow, 3);
        String tipo = (String) model.getValueAt(selectedRow, 4);
        String bodegaNombre = (String) model.getValueAt(selectedRow, 5);

        Integer idVariante = variantIdPorFila.get(selectedRow);
        Integer idBodega = bodegaIdPorFila.get(selectedRow);
        Integer idInventarioBodega = inventarioBodegaIdPorFila.get(selectedRow);

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // INFORMACI??N PARA EL USUARIO
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        String info = String.format(
                "??Est?? seguro de eliminar esta variante?\n\n" +
                        "??? Talla: %s\n" +
                        "??? Color: %s\n" +
                        "??? Cantidad: %d %s\n" +
                        "??? Bodega: %s\n\n" +
                        "?????? ADVERTENCIA: Esta acci??n desactivar?? el inventario en la bodega.\n" +
                        "Si es la ??nica bodega con stock, la variante se marcar?? como NO DISPONIBLE.",
                talla, color, cantidad, tipo, bodegaNombre);

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                info,
                "?????? Confirmar eliminaci??n",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            System.out.println("??? Usuario cancel?? la eliminaci??n");
            return;
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // LOGS DE DEBUG
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        System.out.println("\n??????? INICIANDO ELIMINACI??N DE VARIANTE");
        System.out.println("???".repeat(60));
        System.out.println("Fila seleccionada: " + selectedRow);
        System.out.println("ID Variante: " + idVariante);
        System.out.println("ID Bodega: " + idBodega);
        System.out.println("ID Inventario Bodega: " + idInventarioBodega);
        System.out.println("Talla: " + talla + " | Color: " + color);
        System.out.println("Tipo: " + tipo + " | Cantidad: " + cantidad);
        System.out.println("???".repeat(60));

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // VALIDACIONES
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (idVariante == null || idVariante <= 0) {
            System.err.println("?????? ID de variante inv??lido: " + idVariante);
            showWarning("No se puede eliminar: variante sin ID v??lido.\n" +
                    "Esta variante a??n no est?? guardada en la base de datos.");

            // Eliminar solo de la tabla visual
            model.removeRow(selectedRow);
            limpiarMapasPorFila(selectedRow);
            showSuccess("Variante eliminada de la tabla (no estaba en BD)");
            return;
        }

        if (idBodega == null || idBodega <= 0) {
            System.err.println("?????? ID de bodega inv??lido: " + idBodega);
            showWarning("No se puede eliminar: bodega sin ID v??lido");
            return;
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // ELIMINACI??N EN BASE DE DATOS
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        boolean exitoBD = eliminarVarianteDeBD(idVariante, idBodega, idInventarioBodega);

        if (!exitoBD) {
            showError("Error al eliminar la variante de la base de datos", null);
            return;
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // ELIMINACI??N DE LA TABLA VISUAL
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        try {
            model.removeRow(selectedRow);
            System.out.println("??? Fila eliminada de la tabla visual");
        } catch (Exception e) {
            System.err.println("??? Error eliminando fila de tabla: " + e.getMessage());
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // LIMPIAR MAPAS
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        limpiarMapasPorFila(selectedRow);

        // Actualizar grupos y precios
        reconstruirGruposDesdeTabla();
        validarRequerimientoPreciosSegunTabla();
        tablaModificada = true;

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // MENSAJE FINAL
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        String mensaje = String.format(
                "??? Variante eliminada exitosamente\n\n" +
                        "Talla: %s - Color: %s\n" +
                        "Tipo: %s - Cantidad: %d",
                talla, color, tipo, cantidad);

        showSuccess(mensaje);

        System.out.println("??? ELIMINACI??N COMPLETADA\n");
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ELIMINACI??N EN BASE DE DATOS
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private boolean eliminarVarianteDeBD(int idVariante, int idBodega, Integer idInventarioBodega) {
        java.sql.Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // INICIAR TRANSACCI??N
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            conn = raven.controlador.principal.conexion.getInstance().createConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            System.out.println("???? Iniciando transacci??n de eliminaci??n...");

            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // PASO 1: DESACTIVAR INVENTARIO EN BODEGA (SOFT DELETE)
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            String sqlDesactivarInventario = "UPDATE inventario_bodega SET " +
                    "activo = 0, " +
                    "fecha_ultimo_movimiento = NOW() " +
                    "WHERE id_variante = ? AND id_bodega = ?";

            try (java.sql.PreparedStatement pst = conn.prepareStatement(sqlDesactivarInventario)) {
                pst.setInt(1, idVariante);
                pst.setInt(2, idBodega);

                int rowsInventario = pst.executeUpdate();

                if (rowsInventario > 0) {
                    System.out.println("??? Inventario desactivado en bodega " + idBodega +
                            " (" + rowsInventario + " registro(s))");
                } else {
                    System.out.println("?????? No se encontr?? inventario activo para desactivar");
                }
            }

            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // PASO 2: VERIFICAR SI TIENE STOCK EN OTRAS BODEGAS
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            String sqlCheckOtrasBodegas = "SELECT COUNT(*) FROM inventario_bodega " +
                    "WHERE id_variante = ? " +
                    "  AND id_bodega != ? " +
                    "  AND activo = 1 " +
                    "  AND (Stock_par > 0 OR Stock_caja > 0)";

            boolean tieneStockEnOtrasBodegas = false;

            try (java.sql.PreparedStatement pst = conn.prepareStatement(sqlCheckOtrasBodegas)) {
                pst.setInt(1, idVariante);
                pst.setInt(2, idBodega);

                try (java.sql.ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        tieneStockEnOtrasBodegas = (count > 0);

                        if (tieneStockEnOtrasBodegas) {
                            System.out
                                    .println("?????? La variante tiene stock en " + count + " bodega(s) adicional(es)");
                        }
                    }
                }
            }

            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // PASO 3: SI NO TIENE STOCK EN OTRAS BODEGAS, MARCAR COMO NO DISPONIBLE
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (!tieneStockEnOtrasBodegas) {
                System.out.println("?????? No hay stock en otras bodegas - Marcando variante como NO DISPONIBLE");

                String sqlDesactivarVariante = "UPDATE producto_variantes SET " +
                        "disponible = 0, " +
                        "fecha_actualizacion = NOW() " +
                        "WHERE id_variante = ?";

                try (java.sql.PreparedStatement pst = conn.prepareStatement(sqlDesactivarVariante)) {
                    pst.setInt(1, idVariante);

                    int rowsVariante = pst.executeUpdate();

                    if (rowsVariante > 0) {
                        System.out.println("??? Variante marcada como NO DISPONIBLE");
                    } else {
                        System.out.println("?????? No se pudo marcar variante como no disponible");
                    }
                }
            } else {
                System.out.println("?????? Variante sigue disponible en otras bodegas");
            }

            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // COMMIT
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            conn.commit();
            System.out.println("??? Transacci??n completada exitosamente");

            return true;

        } catch (java.sql.SQLException e) {
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // ROLLBACK EN CASO DE ERROR
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            System.err.println("??? Error SQL durante eliminaci??n: " + e.getMessage());
            e.printStackTrace();

            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("?????? Rollback ejecutado - cambios revertidos");
                } catch (java.sql.SQLException rollbackEx) {
                    System.err.println("??? Error en rollback: " + rollbackEx.getMessage());
                }
            }

            return false;

        } finally {
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // RESTAURAR AUTOCOMMIT Y CERRAR CONEXI??N
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (java.sql.SQLException closeEx) {
                    System.err.println("?????? Error cerrando conexi??n: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * LIMPIEZA DE MAPAS DESPU??S DE ELIMINAR FILA
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private void limpiarMapasPorFila(int filaEliminada) {
        System.out.println("???? Limpiando mapas para fila " + filaEliminada);

        // Eliminar entradas de la fila eliminada
        variantIdPorFila.remove(filaEliminada);
        bodegaIdPorFila.remove(filaEliminada);
        inventarioBodegaIdPorFila.remove(filaEliminada);
        archivosPorFila.remove(filaEliminada);

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // REINDEXAR MAPAS (las filas siguientes bajan un ??ndice)
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        Map<Integer, Integer> tempVariantId = new HashMap<>();
        Map<Integer, Integer> tempBodegaId = new HashMap<>();
        Map<Integer, Integer> tempInventarioId = new HashMap<>();
        Map<Integer, java.io.File> tempArchivos = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : variantIdPorFila.entrySet()) {
            int oldIndex = entry.getKey();

            if (oldIndex > filaEliminada) {
                int newIndex = oldIndex - 1;
                tempVariantId.put(newIndex, entry.getValue());

                // Copiar valores de otros mapas
                if (bodegaIdPorFila.containsKey(oldIndex)) {
                    tempBodegaId.put(newIndex, bodegaIdPorFila.get(oldIndex));
                }
                if (inventarioBodegaIdPorFila.containsKey(oldIndex)) {
                    tempInventarioId.put(newIndex, inventarioBodegaIdPorFila.get(oldIndex));
                }
                if (archivosPorFila.containsKey(oldIndex)) {
                    tempArchivos.put(newIndex, archivosPorFila.get(oldIndex));
                }
            } else if (oldIndex < filaEliminada) {
                tempVariantId.put(oldIndex, entry.getValue());

                if (bodegaIdPorFila.containsKey(oldIndex)) {
                    tempBodegaId.put(oldIndex, bodegaIdPorFila.get(oldIndex));
                }
                if (inventarioBodegaIdPorFila.containsKey(oldIndex)) {
                    tempInventarioId.put(oldIndex, inventarioBodegaIdPorFila.get(oldIndex));
                }
                if (archivosPorFila.containsKey(oldIndex)) {
                    tempArchivos.put(oldIndex, archivosPorFila.get(oldIndex));
                }
            }
        }

        // Reemplazar mapas originales
        variantIdPorFila.clear();
        variantIdPorFila.putAll(tempVariantId);

        bodegaIdPorFila.clear();
        bodegaIdPorFila.putAll(tempBodegaId);

        inventarioBodegaIdPorFila.clear();
        inventarioBodegaIdPorFila.putAll(tempInventarioId);

        archivosPorFila.clear();
        archivosPorFila.putAll(tempArchivos);

        System.out.println("??? Mapas reindexados correctamente");
        System.out.println("   variantIdPorFila: " + variantIdPorFila.size() + " entradas");
        System.out.println("   bodegaIdPorFila: " + bodegaIdPorFila.size() + " entradas");
        System.out.println("   inventarioBodegaIdPorFila: " + inventarioBodegaIdPorFila.size() + " entradas");
        System.out.println("   archivosPorFila: " + archivosPorFila.size() + " entradas");
    }

    // ===================================================================
    // GENERACI??N DE C??DIGOS
    // ===================================================================
    /**
     * Genera c??digo de modelo autom??ticamente
     */
    private void generarCodigoModelo() {
        try {
            if (!validarCamposParaGenerarCodigo()) {
                return;
            }

            String codigo = generarCodigoCompleto();
            txtModelo.setText(codigo);
            showSuccess("C??digo generado: " + codigo);

        } catch (Exception e) {
            showError("Error al generar c??digo", e);
            generarCodigoFallback();
        }
    }

    private boolean validarCamposParaGenerarCodigo() {
        if (comboMarca.getSelectedItem() == null) {
            showWarning("Debe seleccionar una marca para generar el c??digo");
            comboMarca.requestFocus();
            return false;
        }

        if (comboGenero.getSelectedItem() == null
                || comboGenero.getSelectedItem().toString().equals("SELECCIONAR")) {
            showWarning("Debe seleccionar un g??nero para generar el c??digo");
            comboGenero.requestFocus();
            return false;
        }

        return true;
    }

    private String generarCodigoCompleto() throws SQLException {
        ModelBrand marca = (ModelBrand) comboMarca.getSelectedItem();
        String genero = comboGenero.getSelectedItem().toString();

        String prefijoMarca = obtenerPrefijoMarca(marca.getName());
        String codigoGenero = obtenerCodigoGenero(genero);
        int siguienteId = obtenerSiguienteIdProducto();

        return String.format("%s-%s-%03d", prefijoMarca, codigoGenero, siguienteId).toUpperCase();
    }

    private String obtenerPrefijoMarca(String nombreMarca) {
        if (nombreMarca == null || nombreMarca.trim().isEmpty()) {
            return "PROD";
        }

        String marca = nombreMarca.trim().toUpperCase().replaceAll("\\s+", "");

        if (marca.length() >= 4) {
            return marca.substring(0, 4);
        } else {
            return marca + "X".repeat(4 - marca.length());
        }
    }

    private String obtenerCodigoGenero(String genero) {
        if (genero == null) {
            return "X";
        }

        switch (genero.toUpperCase()) {
            case "MUJER":
                return "M";
            case "HOMBRE":
                return "H";
            case "NI??O":
                return "N";
            case "UNISEX":
                return "U";
            default:
                return "X";
        }
    }

    private int obtenerSiguienteIdProducto() throws SQLException {
        try {
            int maxId = new raven.dao.ProductosDAO().getMaxProductId();
            return maxId + 1;
        } catch (SQLException e) {
            System.err.println("Error obteniendo siguiente ID: " + e.getMessage());
            return (int) (System.currentTimeMillis() % 9999) + 1000;
        }
    }

    private void generarCodigoFallback() {
        try {
            ModelBrand marca = (ModelBrand) comboMarca.getSelectedItem();
            String genero = comboGenero.getSelectedItem().toString();
            String prefijo = obtenerPrefijoMarca(marca.getName());
            String codGen = obtenerCodigoGenero(genero);
            long timestamp = System.currentTimeMillis() % 9999;
            String codigoFallback = String.format("%s-%s-%04d", prefijo, codGen, timestamp);

            txtModelo.setText(codigoFallback);
            showWarning("C??digo generado con timestamp: " + codigoFallback);

        } catch (Exception ex) {
            showError("Error cr??tico generando c??digo", ex);
        }
    }

    // ===================================================================
    // GESTI??N DE DATOS DEL FORMULARIO
    // ===================================================================
    /**
     * Obtiene los datos del formulario como ModelProduct
     */
    public ModelProduct obtenerDatosProducto() {
        if (!validarFormulario()) {
            return null;
        }

        if (tablaProd.getRowCount() == 0) {
            showWarning("Debe agregar al menos una variante en la tabla");
            return null;
        }

        try {
            return construirModeloProducto();
        } catch (Exception e) {
            showError("Error al procesar los datos", e);
            return null;
        }
    }

    private ModelProduct construirModeloProducto() throws Exception {
        ModelProduct product = new ModelProduct();

        // Datos b??sicos del producto (SIN c??digo de barras - ahora va en variantes)
        product.setModelCode(txtModelo.getText().trim());
        product.setName(txtNombre.getText().trim());
        product.setDescription(txtDescripcion.getText().trim());
        product.setCategory((ModelCategory) comboCategoria.getSelectedItem());
        product.setBrand((ModelBrand) comboMarca.getSelectedItem());
        product.setSupplier((ModelSupplier) comboProveedor.getSelectedItem());

        // Precios base
        product.setPurchasePrice(limitarRangoPrecio(parsearPrecio(txtPrecioCompra.getText())));
        // Si existen variantes Par, el precio base puede reflejar el precio por par.
        // Si solo hay variantes Caja, no guardamos el precio de txtPrecioVenta en el
        // producto.
        boolean hayPar = false;
        boolean hayCaja = false;
        DefaultTableModel modeloVariantesParaPrecio = (DefaultTableModel) tablaProd.getModel();
        for (int i = 0; i < modeloVariantesParaPrecio.getRowCount(); i++) {
            String tipo = (String) modeloVariantesParaPrecio.getValueAt(i, 4);
            if ("Par".equalsIgnoreCase(tipo)) {
                hayPar = true;
            } else if ("Caja".equalsIgnoreCase(tipo)) {
                hayCaja = true;
            }
        }
        // Precio de venta base tomado de txtPrecioVenta
        String sVenta = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
        if (sVenta != null && !sVenta.trim().isEmpty()) {
            product.setSalePrice(limitarRangoPrecio(parsearPrecio(sVenta)));
        } else {
            product.setSalePrice(0);
        }

        // Otros campos
        product.setMinStock(1);
        product.setGender(comboGenero.getSelectedItem().toString());
        product.setPairsPerBox(24); // Siempre 24 pares por caja
        product.setProfile(profile);

        // Ubicaci??n seg??n sesi??n
        try {
            raven.controlador.admin.SessionManager sm = raven.controlador.admin.SessionManager.getInstance();
            if (sm != null && sm.getCurrentUser() != null) {
                String ubic = sm.getCurrentUser().getUbicacion();
                if (ubic != null && !ubic.trim().isEmpty()) {
                    product.setUbicacion(ubic);
                }
            }
        } catch (Exception ignore) {
        }

        // Datos consolidados de variantes
        Map<String, String> datosConsolidados = consolidarDatosTabla();
        product.setSize(datosConsolidados.get("tallaConsolidada"));
        product.setColor(datosConsolidados.get("colorConsolidado"));

        // ??? NUEVO: Configurar como activo
        product.setActive(true);

        // Crear variantes con c??digos autom??ticos
        List<ModelProductVariant> variants = construirVariantes(product);
        product.setVariants(variants);

        return product;
    }

    private double parsearPrecio(String precioTexto) throws NumberFormatException {
        if (precioTexto == null)
            throw new NumberFormatException("Precio nulo");
        String s = precioTexto.trim();
        if (s.isEmpty())
            throw new NumberFormatException("Precio vac??o");

        s = s.replaceAll("[^0-9.,]", "");

        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');
        int sep = Math.max(lastComma, lastDot);

        String normalized;
        if (sep == -1) {
            normalized = s.replaceAll("[^0-9]", "");
        } else {
            String intPart = s.substring(0, sep).replaceAll("[^0-9]", "");
            String decPart = s.substring(sep + 1).replaceAll("[^0-9]", "");
            normalized = intPart + "." + decPart;
        }

        java.math.BigDecimal bd = new java.math.BigDecimal(normalized);
        return bd.doubleValue();
    }

    // Limitar al rango DECIMAL(10,2) y redondear a 2 decimales
    private double limitarRangoPrecio(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new NumberFormatException("Precio no num??rico");
        }
        double max = 99999999.99;
        double min = -99999999.99;
        if (valor > max)
            valor = max;
        if (valor < min)
            valor = min;
        return Math.round(valor * 100.0) / 100.0;
    }

    private List<ModelProductVariant> construirVariantes(ModelProduct product) throws Exception {
        List<ModelProductVariant> variants = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        ServiceSize serviceSize = new ServiceSize();
        ServiceColor serviceColor = new ServiceColor();

        for (int i = 0; i < model.getRowCount(); i++) {
            ModelProductVariant variant = construirVariante(model, i, product, serviceSize, serviceColor);
            variants.add(variant);
        }

        archivosPorFila.clear();
        return variants;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * CONSTRUIR VARIANTE - VERSI??N CORREGIDA CON BODEGA DE LA FILA
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private ModelProductVariant construirVariante(DefaultTableModel model, int row, ModelProduct product,
            ServiceSize serviceSize, ServiceColor serviceColor) throws Exception {

        String sizeName = (String) model.getValueAt(row, 0);
        String colorName = (String) model.getValueAt(row, 1);
        Integer quantity = (Integer) model.getValueAt(row, 3);
        String type = (String) model.getValueAt(row, 4);
        String bodegaNombre = (String) model.getValueAt(row, 5); // ??? COLUMNA BODEGA

        System.out.println(
                "???? Construyendo variante fila " + row + ": " + sizeName + " - " + colorName + " en " + bodegaNombre);

        ModelProductVariant variant = new ModelProductVariant();

        // ??? RECUPERAR ID EXISTENTE SI EST?? EN EDICI??N
        try {
            Integer existingId = variantIdPorFila.get(row);
            if (existingId != null && existingId > 0) {
                variant.setVariantId(existingId);
                System.out.println("?????? Usando ID de variante existente: " + existingId);
            }
        } catch (Exception ignore) {
        }

        // IDs de talla y color
        try {
            int tallaId = obtenerIdTallaPorString(sizeName);
            if (tallaId > 0) {
                variant.setSizeId(tallaId);
            } else {
                variant.setSizeId(serviceSize.getSizeIdByName(sizeName));
            }
            variant.setColorId(serviceColor.getColorIdByName(colorName));
        } catch (SQLException e) {
            System.err.println("?????? Error obteniendo IDs: " + e.getMessage());
        }

        variant.setSizeName(sizeName);
        variant.setColorName(colorName);

        // Stock seg??n tipo y precios
        double precioPar;
        double precioCaja;
        try {
            String txtPar = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
            precioPar = (txtPar != null && !txtPar.trim().isEmpty()) ? parsearPrecio(txtPar) : product.getSalePrice();
            precioCaja = product.getSalePrice();
        } catch (Exception ex) {
            precioPar = product.getSalePrice();
            precioCaja = product.getSalePrice();
        }

        String tipoNormalizado = (type != null) ? type.trim() : "Par";

        if ("Par".equalsIgnoreCase(tipoNormalizado)) {
            variant.setStockPairs(quantity);
            variant.setStockBoxes(0);
            variant.setSalePrice(limitarRangoPrecio(precioPar));
        } else if ("Caja".equalsIgnoreCase(tipoNormalizado)) {
            variant.setStockBoxes(quantity);
            variant.setStockPairs(0);
            variant.setSalePrice(limitarRangoPrecio(precioCaja));
        } else {
            variant.setStockPairs(quantity);
            variant.setStockBoxes(0);
            variant.setSalePrice(limitarRangoPrecio(precioPar));
        }

        variant.setPurchasePrice(product.getPurchasePrice());
        variant.setMinStock(Math.max(1, product.getMinStock()));
        variant.setAvailable(true);

        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // ??? CR??TICO: USAR BODEGA DE LA FILA, NO LA DEL USUARIO
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        try {
            // 1?????? PRIMERO: Intentar obtener del mapa (si ya fue guardada)
            Integer idBodegaFila = bodegaIdPorFila.get(row);

            if (idBodegaFila != null && idBodegaFila > 0) {
                variant.setWarehouseId(idBodegaFila);
                System.out.println("??? Usando bodega del mapa para fila " + row + ": " + idBodegaFila);
            } else {
                // 2?????? SEGUNDO: Obtener por nombre de la columna de la tabla
                Integer idBodegaPorNombre = obtenerIdBodegaPorNombre(bodegaNombre);

                if (idBodegaPorNombre != null && idBodegaPorNombre > 0) {
                    variant.setWarehouseId(idBodegaPorNombre);
                    bodegaIdPorFila.put(row, idBodegaPorNombre); // Guardar en mapa
                    System.out.println("??? Usando bodega por nombre para fila " + row + ": " + idBodegaPorNombre + " ("
                            + bodegaNombre + ")");
                } else {
                    // 3?????? ??LTIMO RECURSO: Usar bodega actual del usuario
                    Integer idBodegaUsuario = null;
                    try {
                        idBodegaUsuario = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
                    } catch (Throwable ignore) {
                    }

                    if (idBodegaUsuario == null || idBodegaUsuario <= 0) {
                        try {
                            idBodegaUsuario = raven.controlador.admin.SessionManager.getInstance()
                                    .getCurrentUserBodegaId();
                        } catch (Throwable ignore) {
                        }
                    }

                    if (idBodegaUsuario != null && idBodegaUsuario > 0) {
                        variant.setWarehouseId(idBodegaUsuario);
                        System.out.println("?????? Usando bodega del usuario como fallback para fila " + row + ": "
                                + idBodegaUsuario);
                    } else {
                        System.err.println("??? No se pudo determinar bodega para fila " + row);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("??? Error configurando bodega: " + e.getMessage());
        }

        // Generar c??digos autom??ticos
        try {
            String skuGenerado = generarSKU(sizeName != null ? sizeName : "", colorName != null ? colorName : "",
                    tipoNormalizado, row + 1);
            variant.setSku(skuGenerado);
        } catch (Exception ignore) {
        }

        try {
            String eanGenerado = generarEAN13(txtModelo.getText().trim(), sizeName, colorName, row + 1);
            variant.setEan(eanGenerado);
        } catch (Exception ignore) {
        }

        variant.setBarcode(null);

        // Imagen
        procesarImagenVariante(variant, row);

        return variant;
    }

    private String generarEAN13(String modelo, String talla, String color, int secuencia) {
        String base = (modelo == null ? "" : modelo.replaceAll("[^0-9]", ""))
                + (talla == null ? "" : talla.replaceAll("[^0-9]", ""))
                + (color == null ? "" : Integer.toString(Math.abs(color.hashCode())));
        if (base.isEmpty())
            base = Long.toString(System.currentTimeMillis() % 1000000);

        String digits = base + String.format("%02d", secuencia);
        digits = digits.replaceAll("[^0-9]", "");
        if (digits.length() < 12) {
            digits = String.format("%012d", Long.parseLong(digits));
        } else if (digits.length() > 12) {
            digits = digits.substring(0, 12);
        }
        int checksum = calcularEAN13Checksum(digits);
        return digits + checksum;
    }

    private int calcularEAN13Checksum(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = twelveDigits.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : (10 - mod);
    }

    private void procesarImagenVariante(ModelProductVariant variant, int row) {
        File archivoImagen = archivosPorFila.get(row);
        if (archivoImagen != null && archivoImagen.exists()) {
            try {
                byte[] imageBytes = Files.readAllBytes(archivoImagen.toPath());
                variant.setImageBytes(imageBytes);
                System.out.println("??? Imagen cargada para variante: " + variant.getSizeName()
                        + " - " + variant.getColorName() + " (" + (imageBytes.length / 1024) + " KB)");
            } catch (Exception e) {
                System.err.println("??? Error cargando imagen: " + e.getMessage());
            }
        }
    }

    /**
     * Genera un SKU ??nico para una variante espec??fica
     * MEJORADO: Incluye contador secuencial para evitar duplicados en productos del
     * mismo color
     */
    private String generarSKU(String talla, String color, String tipo, int indice) {
        try {
            String codigoModelo = txtModelo.getText().trim();
            if (codigoModelo.isEmpty()) {
                codigoModelo = "TEMP";
            }

            // Generar c??digos m??s cortos y limpios
            String tallaCodigo = talla.replaceAll("\\s+", "").substring(0, Math.min(talla.length(), 3));
            String colorCodigo = color.substring(0, Math.min(color.length(), 3)).toUpperCase();
            String tipoCodigo = tipo.equals("Par") ? "P" : "C";

            // Usar ??ndice como contador secuencial en lugar de timestamp
            String baseSku = String.format("%s-%s-%s-%s-%03d",
                    codigoModelo, tallaCodigo, colorCodigo, tipoCodigo, indice);

            // Verificar unicidad en la tabla actual
            return validarSKUUnicoEnTabla(baseSku, indice);

        } catch (Exception e) {
            System.err.println("??? Error generando SKU: " + e.getMessage());
            long timestamp = System.currentTimeMillis() % 1000000;
            return "SKU-" + indice + "-" + timestamp;
        }
    }

    /**
     * Valida que el SKU sea ??nico en la tabla actual de variantes
     */
    private String validarSKUUnicoEnTabla(String baseSku, int indice) {
        java.util.Set<String> skusExistentes = new java.util.HashSet<>(skusGeneradosTabla);
        String sku = baseSku;
        int contador = 1;
        while (skusExistentes.contains(sku)) {
            sku = baseSku + "-" + String.format("%02d", contador);
            contador++;
            if (contador > 99) {
                long timestamp = System.currentTimeMillis() % 100000;
                sku = baseSku + "-" + timestamp;
                break;
            }
        }
        skusGeneradosTabla.add(sku);
        return sku;
    }

    // ===================================================================
    // UTILIDADES Y HELPERS
    // ===================================================================
    /**
     * Consolida los datos de la tabla para generar res??menes
     */
    private Map<String, String> consolidarDatosTabla() {
        Map<String, String> resultado = new HashMap<>();
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        if (model.getRowCount() == 0) {
            resultado.put("tallaConsolidada", "Sin talla");
            resultado.put("colorConsolidado", "Sin color");
            return resultado;
        }

        Set<String> tallasUnicas = new LinkedHashSet<>();
        Set<String> coloresUnicos = new LinkedHashSet<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String talla = (String) model.getValueAt(i, 0);
            String color = (String) model.getValueAt(i, 1);

            if (talla != null && !talla.trim().isEmpty()) {
                tallasUnicas.add(talla.trim());
            }
            if (color != null && !color.trim().isEmpty()) {
                coloresUnicos.add(color.trim());
            }
        }

        resultado.put("tallaConsolidada", procesarTallas(tallasUnicas));
        resultado.put("colorConsolidado", procesarColores(coloresUnicos));

        return resultado;
    }

    private String procesarTallas(Set<String> tallasUnicas) {
        if (tallasUnicas.isEmpty()) {
            return "Sin talla";
        }
        if (tallasUnicas.size() == 1) {
            return tallasUnicas.iterator().next();
        }

        try {
            List<String> listaOrdenada = new ArrayList<>(tallasUnicas);
            listaOrdenada.sort((t1, t2) -> {
                Integer num1 = extraerNumeroTalla(t1);
                Integer num2 = extraerNumeroTalla(t2);

                if (num1 != null && num2 != null) {
                    return num1.compareTo(num2);
                }
                return t1.compareTo(t2);
            });

            String tallaMenor = listaOrdenada.get(0);
            String tallaMayor = listaOrdenada.get(listaOrdenada.size() - 1);

            Integer numMenor = extraerNumeroTalla(tallaMenor);
            Integer numMayor = extraerNumeroTalla(tallaMayor);
            String sufijo = extraerSufijoTalla(tallaMenor);

            if (numMenor != null && numMayor != null && !numMenor.equals(numMayor)) {
                return numMenor + "-" + numMayor + " " + sufijo;
            } else {
                return "Variadas";
            }

        } catch (Exception e) {
            return "Variadas";
        }
    }

    private String procesarColores(Set<String> coloresUnicos) {
        if (coloresUnicos.isEmpty()) {
            return "Sin color";
        }
        if (coloresUnicos.size() == 1) {
            return coloresUnicos.iterator().next();
        }
        if (coloresUnicos.size() == 2) {
            List<String> lista = new ArrayList<>(coloresUnicos);
            return lista.get(0) + "/" + lista.get(1);
        } else {
            return "Mixto";
        }
    }

    private Integer extraerNumeroTalla(String talla) {
        if (talla == null) {
            return null;
        }

        try {
            String[] partes = talla.split("\\s+");
            for (String parte : partes) {
                if (parte.matches("\\d+")) {
                    return Integer.parseInt(parte);
                }
            }
        } catch (Exception e) {
            // Ignorar errores
        }

        return null;
    }

    private String extraerSufijoTalla(String talla) {
        if (talla == null) {
            return "";
        }

        try {
            String resultado = talla.replaceFirst("^\\d+\\s*", "").trim();
            return resultado.isEmpty() ? "EU" : resultado;
        } catch (Exception e) {
            return "EU";
        }
    }

    private boolean validarCodigoModeloUnico(String codigoModelo) {
        if (codigoModelo == null) {
            return true;
        }
        String codigo = codigoModelo.trim();
        if (codigo.isEmpty()) {
            return true;
        }

        String sql = "SELECT COUNT(*) FROM productos WHERE codigo_modelo=? AND id_producto<>?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, codigo);
            int id = currentProductId > 0 ? currentProductId : -1;
            pst.setInt(2, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count == 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error validando codigo_modelo: " + e.getMessage());
        }
        return true;
    }

    // ===================================================================
    // VALIDACIONES
    // ===================================================================
    /**
     * Valida todos los campos del formulario
     */
    private boolean validarFormulario() {
        return validarCamposBasicos() && validarPrecios() && validarSelecciones();
    }

    private boolean validarCamposBasicos() {
        if (txtNombre.getText().trim().isEmpty()) {
            showWarning("El nombre del producto no puede estar vac??o");
            txtNombre.requestFocus();
            return false;
        }

        if (txtModelo.getText().trim().isEmpty()) {
            showWarning("Debe generar un c??digo de modelo");
            btnModelo.requestFocus();
            return false;
        }

        if (txtDescripcion.getText().trim().isEmpty()) {
            showWarning("La descripci??n no puede estar vac??a");
            txtDescripcion.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validarPrecios() {
        try {
            // ==========================
            // 1) PRECIO DE COMPRA
            // ==========================
            String precioCompraText = (txtPrecioCompra.getText() != null)
                    ? txtPrecioCompra.getText().trim()
                    : "";

            // Si est?? vac??o, pedirlo por JOptionPane
            if (precioCompraText.isEmpty()) {
                String input = JOptionPane.showInputDialog(
                        this,
                        "El precio de COMPRA est?? vac??o.\n\n" +
                                "Por favor ingrese el precio de compra (obligatorio):",
                        "Precio de compra",
                        JOptionPane.QUESTION_MESSAGE);

                // Si cancela, no seguimos con el guardado
                if (input == null) {
                    showInfo("Guardado cancelado. Debe ingresar un precio de compra.");
                    return false;
                }

                input = input.trim();
                if (input.isEmpty()) {
                    showWarning("El precio de compra no puede estar vac??o");
                    txtPrecioCompra.requestFocus();
                    return false;
                }

                // Validar que sea num??rico
                double precioCompraInput = limitarRangoPrecio(parsearPrecio(input));
                // Colocarlo en el campo formateado (puedes ajustar el formato si quieres)
                txtPrecioCompra.setText(String.valueOf(precioCompraInput));
                precioCompraText = String.valueOf(precioCompraInput);
            }

            double precioCompra = parsearPrecio(precioCompraText);
            if (precioCompra < 0) {
                showWarning("El precio de compra no puede ser negativo");
                txtPrecioCompra.requestFocus();
                return false;
            }

            // ==========================
            // 2) REVISAR SI HAY VARIANTES TIPO PAR
            // ==========================
            boolean hayPar = false;
            DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String tipo = (String) model.getValueAt(i, 4); // Columna "Tipo"
                if ("Par".equalsIgnoreCase(tipo)) {
                    hayPar = true;
                    break;
                }
            }

            // ==========================
            // 3) PRECIO DE VENTA (solo si hay PAR)
            // ==========================
            if (hayPar) {
                String precioVentaParText = (txtPrecioVenta != null && txtPrecioVenta.getText() != null)
                        ? txtPrecioVenta.getText().trim()
                        : "";

                // Si est?? vac??o, pedirlo por JOptionPane
                if (precioVentaParText.isEmpty()) {
                    String input = JOptionPane.showInputDialog(
                            this,
                            "Hay variantes tipo PAR pero el precio de VENTA est?? vac??o.\n\n" +
                                    "Por favor ingrese el precio de venta por PAR (obligatorio):",
                            "Precio de venta por PAR",
                            JOptionPane.QUESTION_MESSAGE);

                    // Si cancela, no seguimos con el guardado
                    if (input == null) {
                        showInfo("Guardado cancelado. Debe ingresar un precio de venta por PAR.");
                        return false;
                    }

                    input = input.trim();
                    if (input.isEmpty()) {
                        showWarning("El precio de venta por PAR no puede estar vac??o");
                        txtPrecioVenta.requestFocus();
                        return false;
                    }

                    // Validar que sea num??rico
                    double precioVentaInput = limitarRangoPrecio(parsearPrecio(input));
                    txtPrecioVenta.setText(String.valueOf(precioVentaInput));
                    precioVentaParText = String.valueOf(precioVentaInput);
                }

                double precioVentaPar = parsearPrecio(precioVentaParText);
                if (precioVentaPar < 0) {
                    showWarning("El precio de venta no puede ser negativo");
                    txtPrecioVenta.requestFocus();
                    return false;
                }
            }

            // Si todo est?? bien:
            return true;

        } catch (NumberFormatException e) {
            showWarning("Los precios deben ser valores num??ricos v??lidos");
            return false;
        }
    }

    private boolean validarSelecciones() {
        if (comboCategoria.getSelectedItem() == null) {
            showWarning("Debe seleccionar una categor??a");
            comboCategoria.requestFocus();
            return false;
        }

        if (comboMarca.getSelectedItem() == null) {
            showWarning("Debe seleccionar una marca");
            comboMarca.requestFocus();
            return false;
        }

        if (comboProveedor.getSelectedItem() == null) {
            showWarning("Debe seleccionar un proveedor");
            comboProveedor.requestFocus();
            return false;
        }

        if (comboGenero.getSelectedItem() == null
                || comboGenero.getSelectedItem().toString().equals("SELECCIONAR")) {
            showWarning("Debe seleccionar un g??nero");
            comboGenero.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================================
    // CARGA Y LIMPIEZA DE DATOS
    // ===================================================================
    /**
     * Carga datos de un producto existente para edici??n
     */
    public void cargarDatosProducto2(ServiceProduct service, ModelProduct data) {
        try {
            cargarCombosDesdeServicio(service, data);

            if (data != null) {
                currentProductId = data.getProductId();
                cargarCamposProducto(data);

                // ??? CARGAR VARIANTES COMPLETAS CON IM??GENES DESDE BD
                cargarVariantesCompletasDesdeDB(data.getProductId());

                // ??? CARGAR PRECIOS POR TIPO (Par/Caja) DESDE VARIANTES
                cargarPreciosPorTipoDesdeDB(data.getProductId());
            }
        } catch (SQLException e) {
            showError("Error cargando datos del producto", e);
        }
    }

    private void cargarVariantesCompletasDesdeDB(int productId) {
        // Mostrar diálogo de carga
        javax.swing.JDialog loading = createLoadingDialog(this);

        // Ejecutar en segundo plano
        new javax.swing.SwingWorker<java.util.List<Object[]>, Void>() {
            private final Integer idBodegaSel = obtenerIdBodegaSeleccionada();
            private java.util.List<raven.clases.productos.ServiceProduct.VariantBodegaItem> items;

            @Override
            protected java.util.List<Object[]> doInBackground() throws Exception {
                // Preparar datos para la tabla
                java.util.List<Object[]> filasTabla = new java.util.ArrayList<>();
                java.util.Set<String> presentKeys = new java.util.HashSet<>();

                System.out.println("🚀 Cargando variantes (Async) para producto ID: " + productId);

                // Limpiar mapas previos (se hará efectivo en done())
                raven.clases.productos.ServiceProduct sp = new raven.clases.productos.ServiceProduct();
                items = sp.getVariantesConBodegaPorProducto(productId, idBodegaSel);

                // Procesar items y preparar objetos para la UI
                for (raven.clases.productos.ServiceProduct.VariantBodegaItem it : items) {

                    // Evitar duplicados
                    if (presentKeys.contains(String.valueOf(it.idVariante))) {
                        continue;
                    }

                    // Procesar imagen
                    ImageIcon imagen = null;
                    byte[] imgBytes = it.imagen != null ? it.imagen : sp.getImagenDeVariante(it.idVariante);

                    if (imgBytes != null && imgBytes.length > 0) {
                        try {
                            ImageIcon io = new ImageIcon(imgBytes);
                            if (io.getImage() != null) {
                                Image sc = io.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                imagen = new ImageIcon(sc);
                            }
                        } catch (Exception e) {
                            System.err.println("Error procesando imagen variante " + it.idVariante);
                        }
                    }

                    String nombreBodega = it.bodegaNombre != null ? it.bodegaNombre : obtenerNombreBodegaSeleccionada();

                    // Ya no consultamos la BD por id_inventario_bodega, usamos el del objeto
                    // Si el objeto no lo tiene (0), intentamos recuperarlo
                    Integer idInventario = it.idInventarioBodega;
                    if (idInventario == 0) {
                        idInventario = obtenerIdInventarioBodega(it.idVariante, it.bodegaId);
                    }

                    // Lógica para determinar si mostrar Par o Caja (SOLO UNO)
                    // 1) Respetar el tipo preferido si existe en memoria
                    // 2) Si no hay preferencia, usar heurística basada en stock
                    Boolean mostrarParesObj = null;
                    String tipoPreferido = null;
                    try {
                        tipoPreferido = tipoPreferidoPorVariante.get(it.idVariante);
                    } catch (Exception ignore) {
                    }

                    if (tipoPreferido != null) {
                        if ("Par".equalsIgnoreCase(tipoPreferido)) {
                            mostrarParesObj = Boolean.TRUE;
                        } else if ("Caja".equalsIgnoreCase(tipoPreferido)) {
                            mostrarParesObj = Boolean.FALSE;
                        }
                    }

                    if (mostrarParesObj == null) {
                        if (it.stockPares > 0 && it.stockCajas == 0) {
                            mostrarParesObj = Boolean.TRUE;
                        } else if (it.stockCajas > 0 && it.stockPares == 0) {
                            mostrarParesObj = Boolean.FALSE;
                        } else {
                            mostrarParesObj = Boolean.TRUE;
                        }
                    }

                    boolean mostrarPares = mostrarParesObj;

                    String ubicacionEspItem = it.ubicacionEspecifica;

                    if (mostrarPares) {
                        filasTabla.add(new Object[] {
                                it.talla, // 0 - Talla
                                it.color, // 1 - Color
                                it.proveedorNombre != null ? it.proveedorNombre : "Sin proveedor", // 2 - Proveedor
                                it.stockPares, // 3 - Cantidad
                                "Par", // 4 - Tipo
                                nombreBodega, // 5 - Bodega
                                ubicacionEspItem, // 6 - Ubicación
                                imagen, // 7 - Imagen
                                null, // 8 - Acciones
                                it.idVariante, // 9 - ID Variante (metadata)
                                idInventario, // 10 - ID Inventario (metadata)
                                it.bodegaId, // 11 - ID Bodega (metadata)
                                imgBytes, // 12 - Bytes Imagen (metadata)
                                ubicacionEspItem // 13 - Ubicación (metadata)
                        });
                        if (it.idVariante > 0) {
                            tipoPreferidoPorVariante.put(it.idVariante, "Par");
                        }
                    } else {
                        // Mostrar Caja (si no hay pares, o si ambos son 0)
                        filasTabla.add(new Object[] {
                                it.talla, // 0 - Talla
                                it.color, // 1 - Color
                                it.proveedorNombre != null ? it.proveedorNombre : "Sin proveedor", // 2 - Proveedor
                                it.stockCajas, // 3 - Cantidad
                                "Caja", // 4 - Tipo
                                nombreBodega, // 5 - Bodega
                                ubicacionEspItem, // 6 - Ubicación
                                imagen, // 7 - Imagen
                                null, // 8 - Acciones
                                it.idVariante, // 9 - ID Variante (metadata)
                                idInventario, // 10 - ID Inventario (metadata)
                                it.bodegaId, // 11 - ID Bodega (metadata)
                                imgBytes, // 12 - Bytes Imagen (metadata)
                                ubicacionEspItem // 13 - Ubicación (metadata)
                        });
                        if (it.idVariante > 0) {
                            tipoPreferidoPorVariante.put(it.idVariante, "Caja");
                        }
                    }
                    presentKeys.add(String.valueOf(it.idVariante));
                }
                // Fallback: asegurar incluir variantes con stock 0 si no vinieron del servicio
                try {
                    java.util.List<Object[]> faltantes = cargarVariantesConCeroFallback(productId, idBodegaSel);
                    for (Object[] datos : faltantes) {
                        Integer varId = (Integer) datos[8];
                        String key = String.valueOf(varId);
                        if (!presentKeys.contains(key)) {
                            filasTabla.add(datos);
                            presentKeys.add(key);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("WARN Fallback variantes con cero: " + ex.getMessage());
                }
                return filasTabla;
            }

            @Override
            protected void done() {
                loading.dispose();
                try {
                    java.util.List<Object[]> filas = get();

                    java.util.Map<String, Object[]> mapaFilas = new java.util.LinkedHashMap<>();
                    int contadorNuevos = 0;
                    for (Object[] datos : filas) {
                        Integer varId = null;
                        if (datos != null && datos.length > 9 && datos[9] instanceof Integer) {
                            varId = (Integer) datos[9];
                        }
                        String key;
                        if (varId != null && varId > 0) {
                            key = "V" + varId;
                        } else {
                            contadorNuevos++;
                            key = "N" + contadorNuevos;
                        }
                        if (!mapaFilas.containsKey(key)) {
                            mapaFilas.put(key, datos);
                        }
                    }

                    DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
                    model.setRowCount(0);

                    // Limpiar mapas
                    archivosPorFila.clear();
                    variantIdPorFila.clear();
                    bodegaIdPorFila.clear();
                    inventarioBodegaIdPorFila.clear();
                    ubicacionEspecificaPorFila.clear();

                    int fila = 0;
                    for (Object[] datos : mapaFilas.values()) {
                        // Agregar a tabla (solo las columnas visibles: 0..8)
                        Object[] rowData = new Object[9];
                        System.arraycopy(datos, 0, rowData, 0, 9);
                        model.addRow(rowData);

                        // Recuperar metadata (a partir de índice 9)
                        Integer varId = (Integer) datos[9];
                        Integer invId = (Integer) datos[10];
                        Integer bodId = (Integer) datos[11];
                        byte[] imgBytes = (byte[]) datos[12];
                        String ubicacionEsp = (datos.length > 13) ? (String) datos[13] : null;

                        // Actualizar mapas
                        variantIdPorFila.put(fila, varId);
                        inventarioBodegaIdPorFila.put(fila, invId);
                        bodegaIdPorFila.put(fila, bodId != null ? bodId : getCurrentBodegaId());
                        ubicacionEspecificaPorFila.put(fila, ubicacionEsp);

                        // Crear archivo temporal si hay imagen
                        if (imgBytes != null) {
                            crearArchivoTemporalDesdeBytes(imgBytes, fila, varId);
                        }

                        fila++;
                    }

                    System.out.println("✅ Variantes cargadas exitosamente: " + fila);
                    tablaProd.revalidate();
                    tablaProd.repaint();

                } catch (Exception e) {
                    showError("Error cargando variantes", e);
                    e.printStackTrace();
                }
            }
        }.execute();

        loading.setVisible(true);
    }

    private Integer obtenerIdInventarioBodega(int idVariante, Integer idBodega) {
        if (idBodega == null || idBodega <= 0) {
            return null;
        }

        String sql = "SELECT id_inventario_bodega FROM inventario_bodega " +
                "WHERE id_variante = ? AND id_bodega = ? AND activo = 1 LIMIT 1";

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idVariante);
            pst.setInt(2, idBodega);

            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id_inventario_bodega");
                    System.out.println("??? id_inventario_bodega encontrado: " + id +
                            " (variante=" + idVariante + ", bodega=" + idBodega + ")");
                    return id;
                }
            }

        } catch (java.sql.SQLException e) {
            System.err.println("?????? Error obteniendo id_inventario_bodega: " + e.getMessage());
        }

        System.out.println("?????? No se encontr?? id_inventario_bodega para variante=" +
                idVariante + ", bodega=" + idBodega);
        return null;
    }

    private java.util.List<Object[]> cargarVariantesConCeroFallback(int productId, Integer idBodegaSel)
            throws SQLException {
        java.util.List<Object[]> filasTabla = new java.util.ArrayList<>();
        String sql = "SELECT pv.id_variante, " +
                "       t.numero AS talla, " +
                "       col.nombre AS color, " +
                "       COALESCE(SUM(CASE WHEN ib.activo=1 THEN ib.Stock_par ELSE 0 END),0) AS stock_par, " +
                "       COALESCE(SUM(CASE WHEN ib.activo=1 THEN ib.Stock_caja ELSE 0 END),0) AS stock_caja, " +
                "       COALESCE(MAX(CASE WHEN ib.activo=1 THEN ib.id_inventario_bodega END),0) AS id_inventario, " +
                "       COALESCE(MAX(ib.id_bodega), ?) AS id_bodega, " +
                "       (SELECT nombre FROM bodegas WHERE id_bodega = COALESCE(MAX(ib.id_bodega), ?)) AS bodega_nombre, "
                +
                "       pv.imagen " +
                "FROM producto_variantes pv " +
                "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                (idBodegaSel != null && idBodegaSel > 0 ? " AND ib.id_bodega = ? " : "") +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores col ON pv.id_color = col.id_color " +
                "WHERE pv.id_producto = ? AND pv.disponible = 1 " +
                "GROUP BY pv.id_variante, t.numero, col.nombre";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement pst = conn.prepareStatement(sql)) {
            int idx = 1;
            Integer bodegaIdFallback = idBodegaSel != null && idBodegaSel > 0 ? idBodegaSel : getCurrentBodegaId();
            if (bodegaIdFallback == null || bodegaIdFallback <= 0)
                bodegaIdFallback = 0;
            pst.setInt(idx++, bodegaIdFallback);
            pst.setInt(idx++, bodegaIdFallback);
            if (idBodegaSel != null && idBodegaSel > 0)
                pst.setInt(idx++, idBodegaSel);
            pst.setInt(idx++, productId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int varId = rs.getInt("id_variante");
                    String talla = rs.getString("talla");
                    String color = rs.getString("color");
                    int stockPar = rs.getInt("stock_par");
                    int stockCaja = rs.getInt("stock_caja");
                    int idInventario = rs.getInt("id_inventario");
                    int bodegaId = rs.getInt("id_bodega");
                    String bodegaNombre = rs.getString("bodega_nombre");
                    byte[] imgBytes = rs.getBytes("imagen");

                    ImageIcon imagen = null;
                    if (imgBytes != null && imgBytes.length > 0) {
                        try {
                            ImageIcon io = new ImageIcon(imgBytes);
                            if (io.getImage() != null) {
                                Image sc = io.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                                imagen = new ImageIcon(sc);
                            }
                        } catch (Exception ignore) {
                        }
                    }

                    // Siempre agregar una sola fila por variante
                    // 1) Respetar tipo preferido por variante si existe en memoria
                    // 2) Si no hay preferencia, usar heurística basada en stock
                    Boolean mostrarParesObj = null;
                    String tipoPreferido = null;
                    try {
                        tipoPreferido = tipoPreferidoPorVariante.get(varId);
                    } catch (Exception ignore) {
                    }

                    if (tipoPreferido != null) {
                        if ("Par".equalsIgnoreCase(tipoPreferido)) {
                            mostrarParesObj = Boolean.TRUE;
                        } else if ("Caja".equalsIgnoreCase(tipoPreferido)) {
                            mostrarParesObj = Boolean.FALSE;
                        }
                    }

                    if (mostrarParesObj == null) {
                        if (stockPar > 0 && stockCaja == 0) {
                            mostrarParesObj = Boolean.TRUE;
                        } else if (stockCaja > 0 && stockPar == 0) {
                            mostrarParesObj = Boolean.FALSE;
                        } else {
                            mostrarParesObj = Boolean.TRUE;
                        }
                    }

                    boolean mostrarPares = mostrarParesObj;

                    String ubicacionEsp = null;
                    if (mostrarPares) {
                        filasTabla.add(new Object[] {
                                talla, // 0
                                color, // 1
                                "Sin proveedor", // 2
                                stockPar, // 3
                                "Par", // 4
                                bodegaNombre, // 5
                                ubicacionEsp, // 6
                                imagen, // 7
                                null, // 8
                                varId, // 9
                                idInventario, // 10
                                bodegaId, // 11
                                imgBytes, // 12
                                ubicacionEsp // 13
                        });
                        if (varId > 0) {
                            tipoPreferidoPorVariante.put(varId, "Par");
                        }
                    } else {
                        filasTabla.add(new Object[] {
                                talla, // 0
                                color, // 1
                                "Sin proveedor", // 2
                                stockCaja, // 3
                                "Caja", // 4
                                bodegaNombre, // 5
                                ubicacionEsp, // 6
                                imagen, // 7
                                null, // 8
                                varId, // 9
                                idInventario, // 10
                                bodegaId, // 11
                                imgBytes, // 12
                                ubicacionEsp // 13
                        });
                        if (varId > 0) {
                            tipoPreferidoPorVariante.put(varId, "Caja");
                        }
                    }
                }
            }
        }
        return filasTabla;
    }

    private void configurarBodegaCombo() {
        if (cbxBodega == null) {
            cbxBodega = new javax.swing.JComboBox<>();
        }
        if (jLabelBodega == null) {
            jLabelBodega = new javax.swing.JLabel();
            jLabelBodega.setText("Bodega");
        }
        cbxBodega.addActionListener(e -> {
            if (currentProductId > 0) {
                cargarVariantesCompletasDesdeDB(currentProductId);
                cargarPreciosPorTipoDesdeDB(currentProductId);
            }
        });
    }

    private void cargarBodegasEnCombo() {
        new javax.swing.SwingWorker<java.util.List<ModelBodegas>, Void>() {
            @Override
            protected java.util.List<ModelBodegas> doInBackground() throws Exception {
                ServiceBodegas sb = new ServiceBodegas();
                return sb.obtenerTodas();
            }

            @Override
            protected void done() {
                try {
                    bodegaList = get();
                    javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
                    model.addElement("Todas");
                    for (ModelBodegas b : bodegaList) {
                        if (b.getNombre() != null)
                            model.addElement(b.getNombre());
                    }
                    cbxBodega.setModel(model);

                    Integer idBodegaUsuario = null;
                    try {
                        idBodegaUsuario = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
                    } catch (Throwable ignore) {
                    }
                    if (idBodegaUsuario == null || idBodegaUsuario <= 0) {
                        try {
                            idBodegaUsuario = SessionManager.getInstance().getCurrentUserBodegaId();
                        } catch (Throwable ignore) {
                        }
                    }
                    if (idBodegaUsuario != null && idBodegaUsuario > 0) {
                        String nombre = null;
                        for (ModelBodegas b : bodegaList) {
                            if (b.getIdBodega() != null && b.getIdBodega().equals(idBodegaUsuario)) {
                                nombre = b.getNombre();
                                break;
                            }
                        }
                        if (nombre != null)
                            cbxBodega.setSelectedItem(nombre);
                    } else {
                        cbxBodega.setSelectedItem("Todas");
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando bodegas: " + e.getMessage());
                }
            }
        }.execute();
    }

    private Integer obtenerIdBodegaSeleccionada() {
        Object sel = cbxBodega != null ? cbxBodega.getSelectedItem() : null;
        if (sel == null)
            return null;
        String nombre = sel.toString();
        for (ModelBodegas b : bodegaList) {
            if (nombre.equals(b.getNombre()))
                return b.getIdBodega();
        }
        return null;
    }

    private String obtenerNombreBodegaSeleccionada() {
        Object sel = cbxBodega != null ? cbxBodega.getSelectedItem() : null;
        return sel != null ? sel.toString() : "";
    }

    /**
     * Carga en los campos de precio los primeros valores de venta por tipo
     * (Par y Caja) tomados desde las variantes del producto.
     */
    private void cargarPreciosPorTipoDesdeDB(int productId) {
        new javax.swing.SwingWorker<BigDecimal[], Void>() {
            @Override
            protected BigDecimal[] doInBackground() throws Exception {
                Integer idBodega = obtenerIdBodegaSeleccionada();
                if (idBodega == null || idBodega <= 0) {
                    try {
                        idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
                    } catch (Throwable ignore) {
                    }
                    if (idBodega == null || idBodega <= 0) {
                        try {
                            idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId();
                        } catch (Throwable ignore) {
                        }
                    }
                }

                String sql = "SELECT pv.id_variante, pv.precio_venta, " +
                        "COALESCE(SUM(GREATEST(ib.Stock_par - COALESCE(ib.stock_reservado,0),0)),0) AS stock_par_disponible, "
                        +
                        "COALESCE(SUM(ib.Stock_caja),0) AS stock_caja " +
                        "FROM producto_variantes pv " +
                        "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 " +
                        (idBodega != null && idBodega > 0 ? "AND ib.id_bodega = ? " : "") +
                        "WHERE pv.id_producto = ? AND pv.disponible = 1 " +
                        "GROUP BY pv.id_variante, pv.precio_venta " +
                        "ORDER BY pv.id_variante ASC";

                try (Connection conn = conexion.getInstance().createConnection();
                        PreparedStatement pst = conn.prepareStatement(sql)) {

                    int idx = 1;
                    if (idBodega != null && idBodega > 0)
                        pst.setInt(idx++, idBodega);
                    pst.setInt(idx++, productId);

                    BigDecimal parPrice = null;
                    BigDecimal cajaPrice = null;
                    BigDecimal anyPrice = null;

                    try (ResultSet rs = pst.executeQuery()) {
                        while (rs.next()) {
                            BigDecimal price = rs.getBigDecimal("precio_venta");
                            if (price != null && anyPrice == null)
                                anyPrice = price;
                            int stockParDisp = rs.getInt("stock_par_disponible");
                            int stockCaja = rs.getInt("stock_caja");

                            if (stockParDisp > 0 && parPrice == null)
                                parPrice = price;
                            if (stockCaja > 0 && cajaPrice == null)
                                cajaPrice = price;
                            if (parPrice != null && cajaPrice != null)
                                break;
                        }
                    }
                    return new BigDecimal[] { parPrice, cajaPrice, anyPrice };
                }
            }

            @Override
            protected void done() {
                try {
                    BigDecimal[] prices = get();
                    BigDecimal parPrice = prices[0];
                    BigDecimal cajaPrice = prices[1];
                    BigDecimal anyPrice = prices[2];

                    DecimalFormat df = new DecimalFormat("#,##0.##");
                    df.setRoundingMode(RoundingMode.DOWN);

                    if (parPrice != null) {
                        try {
                            txtPrecioVenta.setValue(parPrice.doubleValue());
                        } catch (Exception ex) {
                            txtPrecioVenta.setText(parPrice.toPlainString());
                        }
                    } else if (cajaPrice != null) {
                        try {
                            txtPrecioVenta.setValue(cajaPrice.doubleValue());
                        } catch (Exception ex) {
                            txtPrecioVenta.setText(cajaPrice.toPlainString());
                        }
                    } else if (anyPrice != null) {
                        try {
                            txtPrecioVenta.setValue(anyPrice.doubleValue());
                        } catch (Exception ex) {
                            txtPrecioVenta.setText(anyPrice.toPlainString());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando precios por tipo: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * cargarImagenDeVarianteOptimizada
     */
    private ImageIcon cargarImagenDeVarianteOptimizada(ResultSet rs, int variantId, int fila) {
        try {
            byte[] imageBytes = rs.getBytes("imagen");

            if (imageBytes != null && imageBytes.length > 0 && esImagenValida(imageBytes)) {

                // ??? CREAR ImageIcon desde bytes
                ImageIcon iconOriginal = new ImageIcon(imageBytes);

                // Verificar que la imagen es v??lida
                if (iconOriginal.getImage() == null) {
                    System.err.println("?????? Imagen corrupta para variante " + variantId);
                    return null;
                }

                // ??? OBTENER DIMENSIONES CORRECTAMENTE
                int width = iconOriginal.getIconWidth();
                int height = iconOriginal.getIconHeight();

                // Verificar dimensiones v??lidas
                if (width <= 0 || height <= 0) {
                    System.err.println(
                            "?????? Dimensiones inv??lidas para variante " + variantId + ": " + width + "x" + height);
                    return null;
                }

                System.out.println("???? Imagen original: " + width + "x" + height);

                // ??? OPTIMIZAR SI ES MUY GRANDE
                ImageIcon iconOptimizado = optimizarImagenSiEsNecesario(iconOriginal, width, height);

                // Redimensionar para la tabla (40x40)
                Image scaledImage = iconOptimizado.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);

                // Crear archivo temporal para edici??n futura
                crearArchivoTemporalParaEdicion(imageBytes, fila, variantId);

                System.out.println("???? Imagen cargada para variante ID: " + variantId
                        + " (" + (imageBytes.length / 1024) + " KB)");

                return new ImageIcon(scaledImage);
            }

        } catch (Exception e) {
            System.err.println("??? Error cargando imagen de variante " + variantId + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * M??TODO AUXILIAR: optimizarImagenSiEsNecesario
     * Optimiza la imagen solo si es necesario
     */
    private ImageIcon optimizarImagenSiEsNecesario(ImageIcon iconOriginal, int width, int height) {
        try {
            // Si la imagen es muy grande, redimensionarla manteniendo aspecto
            if (width > 800 || height > 600) {
                double ratio = Math.min(800.0 / width, 600.0 / height);
                int newWidth = (int) (width * ratio);
                int newHeight = (int) (height * ratio);

                Image scaledImage = iconOriginal.getImage().getScaledInstance(
                        newWidth, newHeight, Image.SCALE_SMOOTH);

                System.out.println("???? Imagen redimensionada de " + width + "x" + height +
                        " a " + newWidth + "x" + newHeight);

                return new ImageIcon(scaledImage);
            }

            // Si no necesita optimizaci??n, devolver original
            return iconOriginal;

        } catch (Exception e) {
            System.err.println("?????? Error optimizando imagen: " + e.getMessage());
            return iconOriginal;
        }
    }

    /**
     * M??TODO AUXILIAR: esImagenValida
     * Verifica si los bytes corresponden a una imagen v??lida
     */
    private boolean esImagenValida2(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 10) {
            return false;
        }

        try {
            // Verificar headers de formatos comunes
            // JPEG: FF D8 FF
            if (imageBytes.length >= 3
                    && (imageBytes[0] & 0xFF) == 0xFF
                    && (imageBytes[1] & 0xFF) == 0xD8
                    && (imageBytes[2] & 0xFF) == 0xFF) {
                return true;
            }

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (imageBytes.length >= 8
                    && (imageBytes[0] & 0xFF) == 0x89
                    && (imageBytes[1] & 0xFF) == 0x50
                    && (imageBytes[2] & 0xFF) == 0x4E
                    && (imageBytes[3] & 0xFF) == 0x47) {
                return true;
            }

            // GIF: 47 49 46 38
            if (imageBytes.length >= 4
                    && (imageBytes[0] & 0xFF) == 0x47
                    && (imageBytes[1] & 0xFF) == 0x49
                    && (imageBytes[2] & 0xFF) == 0x46
                    && (imageBytes[3] & 0xFF) == 0x38) {
                return true;
            }

            // BMP: 42 4D
            if (imageBytes.length >= 2
                    && (imageBytes[0] & 0xFF) == 0x42
                    && (imageBytes[1] & 0xFF) == 0x4D) {
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error validando imagen: " + e.getMessage());
            return false;
        }
    }

    private void crearArchivoTemporalParaEdicion(byte[] imageBytes, int fila, int variantId) {
        try {
            // Determinar extensi??n
            String extension = determinarExtensionImagen(imageBytes);

            // Crear archivo temporal
            File tempFile = File.createTempFile("edit_variant_" + variantId + "_", "." + extension);

            // Escribir bytes
            Files.write(tempFile.toPath(), imageBytes);

            // Asociar con la fila
            archivosPorFila.put(fila, tempFile);

            // Marcar para eliminaci??n autom??tica
            tempFile.deleteOnExit();

            System.out.println("???? Archivo temporal creado para edici??n: " + tempFile.getName());

        } catch (Exception e) {
            System.err.println("?????? Error creando archivo temporal: " + e.getMessage());
        }
    }

    private ImageIcon optimizarImagenParaStorage(ImageIcon iconOriginal) {
        try {
            int width = iconOriginal.getIconWidth();
            int height = iconOriginal.getIconHeight();

            // Si la imagen es muy grande, redimensionarla manteniendo aspecto
            if (width > 800 || height > 600) {
                double ratio = Math.min(800.0 / width, 600.0 / height);
                int newWidth = (int) (width * ratio);
                int newHeight = (int) (height * ratio);

                Image scaledImage = iconOriginal.getImage().getScaledInstance(
                        newWidth, newHeight, Image.SCALE_SMOOTH);

                System.out.println("???? Imagen redimensionada de " + width + "x" + height +
                        " a " + newWidth + "x" + newHeight);

                return new ImageIcon(scaledImage);
            }

            return iconOriginal;

        } catch (Exception e) {
            System.err.println("?????? Error optimizando imagen: " + e.getMessage());
            return iconOriginal;
        }
    }

    private boolean esImagenValida(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 10) {
            return false;
        }

        try {
            // Verificar headers de formatos comunes
            // JPEG: FF D8 FF
            if (imageBytes.length >= 3
                    && (imageBytes[0] & 0xFF) == 0xFF
                    && (imageBytes[1] & 0xFF) == 0xD8
                    && (imageBytes[2] & 0xFF) == 0xFF) {
                return true;
            }

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (imageBytes.length >= 8
                    && (imageBytes[0] & 0xFF) == 0x89
                    && (imageBytes[1] & 0xFF) == 0x50
                    && (imageBytes[2] & 0xFF) == 0x4E
                    && (imageBytes[3] & 0xFF) == 0x47) {
                return true;
            }

            // GIF: 47 49 46 38
            if (imageBytes.length >= 4
                    && (imageBytes[0] & 0xFF) == 0x47
                    && (imageBytes[1] & 0xFF) == 0x49
                    && (imageBytes[2] & 0xFF) == 0x46
                    && (imageBytes[3] & 0xFF) == 0x38) {
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error validando imagen: " + e.getMessage());
            return false;
        }
    }

    private String determinarExtensionImagen(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return "jpg"; // Por defecto
        }

        try {
            // JPEG
            if (imageBytes.length >= 3
                    && (imageBytes[0] & 0xFF) == 0xFF
                    && (imageBytes[1] & 0xFF) == 0xD8
                    && (imageBytes[2] & 0xFF) == 0xFF) {
                return "jpg";
            }

            // PNG
            if (imageBytes.length >= 8
                    && (imageBytes[0] & 0xFF) == 0x89
                    && (imageBytes[1] & 0xFF) == 0x50
                    && (imageBytes[2] & 0xFF) == 0x4E
                    && (imageBytes[3] & 0xFF) == 0x47) {
                return "png";
            }

            // GIF
            if (imageBytes.length >= 4
                    && (imageBytes[0] & 0xFF) == 0x47
                    && (imageBytes[1] & 0xFF) == 0x49
                    && (imageBytes[2] & 0xFF) == 0x46
                    && (imageBytes[3] & 0xFF) == 0x38) {
                return "gif";
            }

            return "jpg"; // Por defecto

        } catch (Exception e) {
            return "jpg"; // Por defecto si hay error
        }
    }

    private void crearArchivoTemporalDesdeBytes(byte[] imageBytes, int filaIndex, int variantId) {
        try {
            // Crear archivo temporal
            File tempFile = File.createTempFile("edit_variant_" + variantId + "_", ".jpg");

            // Escribir bytes al archivo
            java.nio.file.Files.write(tempFile.toPath(), imageBytes);

            // Asociar con la fila
            archivosPorFila.put(filaIndex, tempFile);

            // Marcar para eliminaci??n al salir
            tempFile.deleteOnExit();

            System.out.println("???? Archivo temporal creado para edici??n: " + tempFile.getName()
                    + " (" + (imageBytes.length / 1024) + " KB)");

        } catch (Exception e) {
            System.err.println("?????? Error creando archivo temporal desde bytes: " + e.getMessage());
        }
    }

    private void cargarCombosDesdeServicio(ServiceProduct service, ModelProduct data) throws SQLException {
        // OPTIMIZACIÓN: Usar caché si está disponible y válido
        if (comboCache != null && comboCache.isValid()) {
            // Cargar desde caché de forma síncrona (rápido)
            SwingUtilities.invokeLater(() -> {
                try {
                    cargarCombosDesdeCache(comboCache, data);
                } catch (Exception e) {
                    System.err.println("Error cargando combos desde caché: " + e.getMessage());
                    // Fallback: cargar desde servicio
                    cargarCombosDesdeServicioSinCache(service, data);
                }
            });
            return;
        }

        // Ejecutar carga asíncrona desde servicio y actualizar caché
        cargarCombosDesdeServicioSinCache(service, data);
    }

    private void cargarCombosDesdeServicioSinCache(ServiceProduct service, ModelProduct data) {
        new javax.swing.SwingWorker<Map<String, java.util.List<?>>, Void>() {
            @Override
            protected Map<String, java.util.List<?>> doInBackground() throws Exception {
                Map<String, java.util.List<?>> result = new HashMap<>();
                result.put("brands", service.getServiceBrand().getAll());
                result.put("categories", service.getServiceCategory().getAll());
                result.put("suppliers", service.getServiceSupplier().getAll());

                // OPTIMIZACIÓN: Actualizar caché estático
                ComboCache newCache = new ComboCache();
                newCache.brands = (java.util.List<ModelBrand>) result.get("brands");
                newCache.categories = (java.util.List<ModelCategory>) result.get("categories");
                newCache.suppliers = (java.util.List<ModelSupplier>) result.get("suppliers");
                newCache.timestamp = System.currentTimeMillis();
                comboCache = newCache;

                return result;
            }

            @Override
            protected void done() {
                try {
                    Map<String, java.util.List<?>> result = get();
                    cargarCombosEnUI(result, data);
                } catch (Exception e) {
                    showError("Error cargando listas desplegables", e);
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    /**
     * OPTIMIZACIÓN: Método auxiliar para cargar combos en la UI desde datos en
     * memoria.
     * Se usa tanto para datos del caché como para datos del servicio.
     */
    private void cargarCombosEnUI(Map<String, java.util.List<?>> result, ModelProduct data) {
        // Limpiar combos (en EDT)
        comboCategoria.removeAllItems();
        comboMarca.removeAllItems();
        comboProveedor.removeAllItems();

        // Cargar marcas
        for (Object item : result.get("brands")) {
            ModelBrand brand = (ModelBrand) item;
            comboMarca.addItem(brand);
            if (data != null && data.getBrand() != null) {
                boolean matchId = data.getBrand().getBrandId() > 0
                        && data.getBrand().getBrandId() == brand.getBrandId();
                boolean matchName = (data.getBrand().getName() != null && brand.getName() != null
                        && data.getBrand().getName().equalsIgnoreCase(brand.getName()));
                if (matchId || matchName)
                    comboMarca.setSelectedItem(brand);
            }
        }

        // Cargar categorías
        for (Object item : result.get("categories")) {
            ModelCategory cat = (ModelCategory) item;
            comboCategoria.addItem(cat);
            if (data != null && data.getCategory() != null) {
                boolean matchId = data.getCategory().getCategoryId() > 0
                        && data.getCategory().getCategoryId() == cat.getCategoryId();
                boolean matchName = (data.getCategory().getName() != null && cat.getName() != null
                        && data.getCategory().getName().equalsIgnoreCase(cat.getName()));
                if (matchId || matchName)
                    comboCategoria.setSelectedItem(cat);
            }
        }

        // Cargar proveedores
        for (Object item : result.get("suppliers")) {
            ModelSupplier sup = (ModelSupplier) item;
            comboProveedor.addItem(sup);
            if (data != null && data.getSupplier() != null) {
                boolean matchId = data.getSupplier().getSupplierId() > 0
                        && data.getSupplier().getSupplierId() == sup.getSupplierId();
                boolean matchName = (sup.getName() != null && data.getSupplier().getName() != null
                        && sup.getName().equalsIgnoreCase(data.getSupplier().getName()));
                if (matchId || matchName)
                    comboProveedor.setSelectedItem(sup);
            }
        }

        if (comboMarca.getSelectedItem() == null && comboMarca.getItemCount() > 0)
            comboMarca.setSelectedIndex(0);
        if (comboCategoria.getSelectedItem() == null && comboCategoria.getItemCount() > 0)
            comboCategoria.setSelectedIndex(0);
        if (comboProveedor.getSelectedItem() == null && comboProveedor.getItemCount() > 0)
            comboProveedor.setSelectedIndex(0);
    }

    /**
     * OPTIMIZACIÓN: Carga combos desde caché estático (rápido, síncrono).
     */
    private void cargarCombosDesdeCache(ComboCache cache, ModelProduct data) {
        Map<String, java.util.List<?>> result = new HashMap<>();
        result.put("brands", cache.brands);
        result.put("categories", cache.categories);
        result.put("suppliers", cache.suppliers);
        cargarCombosEnUI(result, data);
        System.out.println("✅ Combos cargados desde caché (rápido)");
    }

    private void cargarCamposProducto(ModelProduct data) {
        if (data.getModelCode() != null) {
            txtModelo.setText(data.getModelCode());
        }
        txtNombre.setText(data.getName());
        String gen = data.getGender();
        if (gen != null) {
            boolean found = false;
            for (int i = 0; i < comboGenero.getItemCount(); i++) {
                Object it = comboGenero.getItemAt(i);
                if (it != null && gen.equalsIgnoreCase(it.toString())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                comboGenero.addItem(gen);
            comboGenero.setSelectedItem(gen);
        }

        DecimalFormat df = new DecimalFormat("#,##0");
        df.setRoundingMode(RoundingMode.DOWN);

        try {
            java.math.BigDecimal bdCompra = new java.math.BigDecimal(String.valueOf(data.getPurchasePrice()));
            txtPrecioCompra.setText(bdCompra.toPlainString());
        } catch (Exception ex) {
            txtPrecioCompra.setText(String.valueOf(data.getPurchasePrice()));
        }
        try {
            java.math.BigDecimal bdVenta = new java.math.BigDecimal(String.valueOf(data.getSalePrice()));
            if (bdVenta.compareTo(java.math.BigDecimal.ZERO) > 0) {
                txtPrecioVenta.setText(bdVenta.toPlainString());
            } else {
                txtPrecioVenta.setText("");
            }
        } catch (Exception ex) {
            txtPrecioVenta.setText(String.valueOf(data.getSalePrice()));
        }
        txtDescripcion.setText(data.getDescription());

        // OPTIMIZACIÓN: No cargar precios aquí cuando se está en modo carga básica
        // Los precios se cargarán en cargarDatosProductoCompletos
        // if (txtPrecioVenta.getText() == null ||
        // txtPrecioVenta.getText().trim().isEmpty()) {
        // cargarPreciosPorTipoDesdeDB(data.getProductId());
        // }
    }

    private ImageIcon cargarImagenDeVariante(int variantId) {
        try {
            raven.dao.ProductoVariantesDAO dao = new raven.dao.ProductoVariantesDAO();
            byte[] imageBytes = dao.getImageBytes(variantId);
            if (imageBytes != null && imageBytes.length > 0) {
                ImageIcon iconOriginal = new ImageIcon(imageBytes);
                Image scaledImage = iconOriginal.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * Limpia completamente el formulario
     */
    public void limpiarFormulario() {
        // Limpiar campos de texto
        txtNombre.setText("");
        txtModelo.setText("");
        txtDescripcion.setText("");
        txtPrecioCompra.setText("");
        txtPrecioVenta.setText("");
        // Resetear combos
        comboMarca.setSelectedIndex(-1);
        comboCategoria.setSelectedIndex(-1);
        comboProveedor.setSelectedIndex(-1);
        comboGenero.setSelectedIndex(0);

        // Limpiar tabla
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        model.setRowCount(0);

        profile = null;

        // Limpiar archivos de im??genes
        archivosPorFila.clear();

        txtNombre.requestFocus();
        showInfo("Formulario limpiado - Listo para nuevo producto");
    }

    // ===================================================================
    // MANEJO DE EVENTOS
    // ===================================================================
    /**
     * Maneja el cambio de g??nero para actualizar tallas
     */
    private void onGeneroChanged(ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            if (comboGenero.getSelectedIndex() >= 0) {
                String generoSeleccionado = comboGenero.getSelectedItem().toString();
            }
        }
    }

    // ===================================================================
    // GUARDADO DE PRODUCTOS CON SISTEMA COMPLETO
    // ===================================================================
    public void guardarProducto() {
        if (!tablaModificada && !preciosModificados) {
            showWarning("No hay cambios para guardar");
            return;
        }

        if (tablaProd.getRowCount() == 0) {
            showWarning("Debe agregar al menos una variante antes de guardar");
            return;
        }

        if (!validarFormulario()) {
            showWarning("Complete todos los campos requeridos");
            return;
        }

        // Mostrar confirmaci??n
        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                String.format("??Confirma guardar el producto?\n\n"
                        + "??? Nombre: %s\n"
                        + "??? C??digo: %s\n"
                        + "??? Variantes: %d\n"
                        + "??? Im??genes: %d\n\n"
                        + "Se generar??n autom??ticamente los c??digos SKU, c??digo de barras y EAN",
                        txtNombre.getText().trim(),
                        txtModelo.getText().trim(),
                        tablaProd.getRowCount(),
                        contarVariantesConImagen()),
                "Confirmar guardado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            ejecutarGuardadoConImagenes();
        }
    }

    private int contarVariantesConImagen() {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            // Verificar archivo asociado
            if (archivosPorFila.containsKey(i)) {
                count++;
                continue;
            }

            // Verificar imagen en tabla
            if (model.getColumnCount() > 6) {
                Object imageObj = model.getValueAt(i, 6);
                if (imageObj instanceof ImageIcon) {
                    count++;
                }
            }
        }

        return count;
    }

    private void ejecutarGuardadoConImagenes() {
        try {
            System.out.println("\n???? INICIANDO GUARDADO CON IM??GENES...");

            // 1. Crear el producto base
            ModelProduct product = construirModeloProductoBase();
            if (product == null) {
                showError("Error procesando datos del producto", null);
                return;
            }

            // =================================================================================
            // ??? MODO EDICI??N: SOLO ACTUALIZAR DATOS BASE (SOLICITUD USUARIO)
            // =================================================================================
            if (currentProductId > 0) {
                System.out.println("??? MODO EDICI??N DETECTADO - Actualizando solo tabla productos");

                // Asegurar ID correcto
                product.setProductId(currentProductId);

                // Actualizar solo datos base (sin tocar variantes)
                raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
                service.update(product, obtenerIdUsuarioActual());

                showSuccess("Producto actualizado correctamente (Datos base)");
                return; // ??? DETENER AQU?? - No tocar variantes
            }
            // =================================================================================

            // 2. Crear variantes con im??genes
            List<ModelProductVariant> variants = construirVariantesConImagenes(product);
            product.setVariants(variants);

            // 3. Consolidaci??n de precios se realizar?? al final tras guardar
            try {
                String compraTxt = (txtPrecioCompra != null) ? txtPrecioCompra.getText() : "";
                if (compraTxt != null && !compraTxt.trim().isEmpty() && currentProductId > 0) {
                    double precioCompraCaja = limitarRangoPrecio(parsearPrecio(compraTxt));
                    raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
                    int totalCompraCaja = service.actualizarCompraCajaPorProducto(currentProductId, precioCompraCaja);
                    if (totalCompraCaja > 0) {
                        showSuccess("Precio compra Caja aplicado a " + totalCompraCaja + " variantes");
                    }
                }
            } catch (Exception ex) {
                showError("Error aplicando precio de compra a cajas", ex);
            }

            // 4. Guardar usando ServiceProductVariant mejorado
            boolean exito = guardarProductoCompleto(product);

            if (exito) {
                currentProductId = product.getProductId();
                aplicarPrecioCompraDesdeCampo();
                aplicarPrecioVentaDesdeCampo();
                guardarImagenesVariantesPorProducto(product);
                mostrarResultadoGuardado(product);

                // RECARGA OPTIMIZADA: En lugar de limpiar, recargamos las variantes para ver
                // los cambios
                tablaModificada = false;
                preciosModificados = false;
                cargarVariantesCompletasDesdeDB(currentProductId);
                showSuccess("Producto actualizado y recargado correctamente");
            } else {
                showError("Error al guardar el producto", null);
            }

        } catch (Exception e) {
            showError("Error durante el guardado", e);
            System.err.println("??? Error completo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ModelProduct construirModeloProductoBase() throws Exception {
        ModelProduct product = new ModelProduct();

        String codigoModelo = txtModelo.getText() != null ? txtModelo.getText().trim() : "";
        product.setModelCode(codigoModelo);

        if (!validarCodigoModeloUnico(codigoModelo)) {
            showWarning("El código modelo ya existe en otro producto. Use uno diferente.");
            return null;
        }

        product.setName(txtNombre.getText().trim());
        product.setDescription(txtDescripcion.getText().trim());
        product.setCategory((ModelCategory) comboCategoria.getSelectedItem());
        product.setBrand((ModelBrand) comboMarca.getSelectedItem());
        product.setSupplier((ModelSupplier) comboProveedor.getSelectedItem());

        {
            String pv = (txtPrecioCompra != null && txtPrecioCompra.getText() != null)
                    ? txtPrecioCompra.getText().trim()
                    : "";
            double compra = pv.isEmpty() ? 0 : limitarRangoPrecio(parsearPrecio(pv));
            product.setPurchasePrice(compra);
        }

        String txtParBase = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
        if (txtParBase != null && !txtParBase.trim().isEmpty()) {
            product.setSalePrice(limitarRangoPrecio(parsearPrecio(txtParBase)));
        } else {
            product.setSalePrice(0);
        }

        product.setMinStock(1);
        product.setGender(comboGenero.getSelectedItem().toString());
        product.setPairsPerBox(24);
        product.setProfile(profile);

        Map<String, String> datosConsolidados = consolidarDatosTabla();
        product.setSize(datosConsolidados.get("tallaConsolidada"));
        product.setColor(datosConsolidados.get("colorConsolidado"));

        product.setActive(true);

        return product;
    }

    private void limpiarFormularioCompleto() {
        // Limpiar campos b??sicos
        limpiarFormulario();

        // Limpiar datos de im??genes
        archivosPorFila.clear();
        imagenFilaActual = null;
        archivoImagenSeleccionada = null;

        System.out.println("???? Formulario limpiado completamente");
    }

    private void mostrarResultadoGuardado(ModelProduct product) {
        int imagenesGuardadas = contarVariantesConImagen();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("??? PRODUCTO GUARDADO EXITOSAMENTE!\n\n");
        mensaje.append("???? INFORMACI??N:\n");
        mensaje.append("??? Nombre: ").append(product.getName()).append("\n");
        mensaje.append("??? C??digo: ").append(product.getModelCode()).append("\n");
        mensaje.append("??? Variantes: ").append(product.getVariants().size()).append("\n");
        mensaje.append("??? Im??genes: ").append(imagenesGuardadas).append("\n");
        mensaje.append("??? G??nero: ").append(product.getGender()).append("\n\n");

        mensaje.append("???? GUARDADO AUTOM??TICO:\n");
        mensaje.append("??? C??digos SKU generados autom??ticamente\n");
        mensaje.append("??? C??digos de barras (770...) creados\n");
        mensaje.append("??? C??digos EAN-13 con d??gito de control\n");
        mensaje.append("??? Im??genes guardadas en campo LONGBLOB\n");

        System.out.println(mensaje.toString());
        showSuccess("Producto guardado con " + imagenesGuardadas + " im??genes");
    }

    private List<ModelProductVariant> construirVariantesConImagenes(ModelProduct product) throws Exception {
        List<ModelProductVariant> variants = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        ServiceSize serviceSize = new ServiceSize();
        ServiceColor serviceColor = new ServiceColor();

        System.out.println("???? Construyendo variantes con im??genes...");

        for (int i = 0; i < model.getRowCount(); i++) {
            String sizeName = (String) model.getValueAt(i, 0);
            String colorName = (String) model.getValueAt(i, 1);
            Integer quantity = (Integer) model.getValueAt(i, 3);
            String type = (String) model.getValueAt(i, 4);

            ModelProductVariant variant = new ModelProductVariant();

            // Configurar datos b??sicos
            configurarDatosBasicosVariante(variant, sizeName, colorName, quantity, type, product, serviceSize,
                    serviceColor);

            // ??? PROCESAR IMAGEN - MEJORADO
            procesarImagenVarianteCompleta(variant, i);

            try {
                Object bodegaCell = model.getValueAt(i, 5);
                if (bodegaCell != null) {
                    String nombre = bodegaCell.toString();
                    Integer idBod = null;
                    for (ModelBodegas b : bodegaList) {
                        if (nombre.equals(b.getNombre())) {
                            idBod = b.getIdBodega();
                            break;
                        }
                    }
                    if (idBod != null && idBod > 0) {
                        variant.setWarehouseId(idBod);
                    }
                }
            } catch (Exception ignore) {
            }

            variants.add(variant);

            System.out.println(String.format("??? Variante %d: %s - %s (Stock: %d) %s",
                    i + 1, sizeName, colorName, calcularTotalParesVariante(variant),
                    variant.hasImage() ? "CON IMAGEN" : "sin imagen"));
        }

        return variants;
    }

    private void configurarDatosBasicosVariante(ModelProductVariant variant, String sizeName,
            String colorName, Integer quantity, String type, ModelProduct product,
            ServiceSize serviceSize, ServiceColor serviceColor) throws Exception {

        // Normalizar valores potencialmente nulos
        final String sizeNameSafe = (sizeName != null) ? sizeName.trim() : "";
        final String colorNameSafe = (colorName != null) ? colorName.trim() : "";
        final int cantidad = (quantity != null) ? quantity.intValue() : 0;
        final String tipoNormalizado = (type != null) ? type.trim() : "Par";

        // IDs de talla y color con defensiva ante nulos
        try {
            if (!sizeNameSafe.isEmpty()) {
                int tallaId = obtenerIdTallaPorString(sizeNameSafe);
                if (tallaId > 0) {
                    variant.setSizeId(tallaId);
                } else {
                    // Fallback a b??squeda simple por nombre si no se encuentra la talla
                    // espec??fica
                    variant.setSizeId(serviceSize.getSizeIdByName(sizeNameSafe));
                }
            } else {
                // Valor por defecto si la talla viene vac??a/nula
                variant.setSizeId(1);
            }

            if (!colorNameSafe.isEmpty()) {
                variant.setColorId(serviceColor.getColorIdByName(colorNameSafe));
            } else {
                // Valor por defecto si el color viene vac??o/nulo
                variant.setColorId(1);
            }
        } catch (SQLException e) {
            System.err.println("?????? Error obteniendo IDs: " + e.getMessage());
            // Valores por defecto si hay error en la consulta
            variant.setSizeId(1);
            variant.setColorId(1);
        }

        // Nombres para mostrar (evitar nulos)
        variant.setSizeName(!sizeNameSafe.isEmpty() ? sizeNameSafe : "Sin talla");
        variant.setColorName(!colorNameSafe.isEmpty() ? colorNameSafe : "Sin color");

        // Stock seg??n tipo con cantidad defensiva y precio de venta por tipo
        double precioPar;
        double precioCaja;
        try {
            String txtPar = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
            precioPar = (txtPar != null && !txtPar.trim().isEmpty()) ? parsearPrecio(txtPar) : product.getSalePrice();
            precioCaja = product.getSalePrice();
        } catch (Exception ex) {
            precioPar = product.getSalePrice();
            precioCaja = product.getSalePrice();
        }

        if ("Caja".equalsIgnoreCase(tipoNormalizado)) {
            variant.setStockBoxes(cantidad);
            variant.setStockPairs(0);
            variant.setSalePrice(limitarRangoPrecio(precioCaja));
        } else {
            variant.setStockPairs(cantidad);
            variant.setStockBoxes(0);
            variant.setSalePrice(limitarRangoPrecio(precioPar));
        }
        variant.setPurchasePrice(product.getPurchasePrice());
        variant.setMinStock(1);
        variant.setAvailable(true);

        try {
            Integer idBodegaSel = obtenerIdBodegaSeleccionada();
            if (idBodegaSel == null || idBodegaSel <= 0) {
                Integer idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId();
                idBodegaSel = idBodega;
            }
            if (idBodegaSel != null && idBodegaSel > 0) {
                variant.setWarehouseId(idBodegaSel);
            }
        } catch (Exception ignore) {
        }
    }

    private void procesarImagenVarianteCompleta(ModelProductVariant variant, int filaIndex) {
        try {
            byte[] imageBytes = null;

            // Opci??n 1: Buscar archivo asociado a la fila
            File archivoImagen = archivosPorFila.get(filaIndex);
            if (archivoImagen != null && archivoImagen.exists()) {
                imageBytes = Files.readAllBytes(archivoImagen.toPath());
                System.out.println(String.format("???? Imagen desde archivo para fila %d: %s (%d KB)",
                        filaIndex, archivoImagen.getName(), imageBytes.length / 1024));
            }
            // Opci??n 2: Extraer desde ImageIcon en la tabla
            else {
                DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
                if (model.getColumnCount() > 6) {
                    Object imageObj = model.getValueAt(filaIndex, 6);
                    if (imageObj instanceof ImageIcon) {
                        imageBytes = convertirImageIconABytesOptimizado((ImageIcon) imageObj);
                        System.out.println(String.format("??????? Imagen desde tabla para fila %d (%d KB)",
                                filaIndex, imageBytes != null ? imageBytes.length / 1024 : 0));
                    }
                }
            }

            // Asignar imagen al variant
            if (imageBytes != null && imageBytes.length > 0) {
                variant.setImageBytes(imageBytes);
            }

        } catch (Exception e) {
            System.err.println("?????? Error procesando imagen para fila " + filaIndex + ": " + e.getMessage());
            // No lanzar excepci??n, solo registrar el error
        }
    }

    private byte[] convertirImageIconABytesOptimizado(ImageIcon icon) {
        try {
            if (icon == null || icon.getImage() == null) {
                return null;
            }

            // Crear BufferedImage optimizada
            BufferedImage bufferedImage = new BufferedImage(
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = bufferedImage.createGraphics();

            // Mejorar calidad del renderizado
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Pintar fondo blanco
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());

            // Pintar la imagen
            icon.paintIcon(null, g2d, 0, 0);
            g2d.dispose();

            // Convertir a bytes con calidad optimizada
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("Error convirtiendo ImageIcon: " + e.getMessage());
            return null;
        }
    }

    public ModelProduct obtenerDatosProductoConImagenes() {
        if (!validarFormulario()) {
            return null;
        }

        if (tablaProd.getRowCount() == 0) {
            showWarning("Debe agregar al menos una variante en la tabla");
            return null;
        }

        try {
            ModelProduct product = construirModeloProducto();

            // ??? CREAR VARIANTES CON IM??GENES PROCESADAS
            List<ModelProductVariant> variants = construirVariantesConImagenesProcesadas(product);
            product.setVariants(variants);

            System.out.println("??? Producto construido con " + variants.size() + " variantes");
            return product;

        } catch (Exception e) {
            showError("Error al procesar los datos", e);
            return null;
        }
    }

    private List<ModelProductVariant> construirVariantesConImagenesProcesadas(ModelProduct product) throws Exception {
        List<ModelProductVariant> variants = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        ServiceSize serviceSize = new ServiceSize();
        ServiceColor serviceColor = new ServiceColor();

        System.out.println("???? Construyendo variantes con im??genes...");

        for (int i = 0; i < model.getRowCount(); i++) {
            String sizeName = (String) model.getValueAt(i, 0);
            String colorName = (String) model.getValueAt(i, 1);
            Integer quantity = (Integer) model.getValueAt(i, 3);
            String type = (String) model.getValueAt(i, 4);

            ModelProductVariant variant = new ModelProductVariant();

            // IDs de talla y color
            try {
                // Obtener id de talla considerando el g??nero incluido en el nombre
                int tallaId = obtenerIdTallaPorString(sizeName);
                if (tallaId > 0) {
                    variant.setSizeId(tallaId);
                } else {
                    variant.setSizeId(serviceSize.getSizeIdByName(sizeName));
                }
                // Obtener id del color por nombre
                variant.setColorId(serviceColor.getColorIdByName(colorName));
            } catch (SQLException e) {
                System.err.println("?????? Error obteniendo IDs: " + e.getMessage());
                // Valores por defecto si hay error
                variant.setSizeId(1);
                variant.setColorId(1);
            }

            // Nombres para mostrar
            variant.setSizeName(sizeName);
            variant.setColorName(colorName);

            // Stock seg??n tipo (case-insensitive)
            String tipoNormalizado = (type != null) ? type.trim() : "Par";
            if ("Par".equalsIgnoreCase(tipoNormalizado)) {
                variant.setStockPairs(quantity);
                variant.setStockBoxes(0);
            } else if ("Caja".equalsIgnoreCase(tipoNormalizado)) {
                variant.setStockBoxes(quantity);
                variant.setStockPairs(0);
            }

            // Precio de compra heredado y precio de venta seg??n tipo
            double precioPar;
            double precioCaja;
            try {
                String txtPar = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
                precioPar = (txtPar != null && !txtPar.trim().isEmpty()) ? parsearPrecio(txtPar)
                        : product.getSalePrice();
                precioCaja = product.getSalePrice();
            } catch (Exception ex) {
                precioPar = product.getSalePrice();
                precioCaja = product.getSalePrice();
            }

            if ("Caja".equalsIgnoreCase(tipoNormalizado)) {
                variant.setSalePrice(limitarRangoPrecio(precioCaja));
            } else { // Par por defecto
                variant.setSalePrice(limitarRangoPrecio(precioPar));
            }
            variant.setPurchasePrice(product.getPurchasePrice());
            variant.setMinStock(1);
            variant.setAvailable(true);

            // ??? PROCESAR IMAGEN ESPEC??FICA DE ESTA VARIANTE
            procesarImagenDeVariante(variant, i);

            variants.add(variant);

            System.out.println(String.format("??? Variante %d: %s - %s (Stock: %d pares) %s",
                    i + 1, sizeName, colorName, calcularTotalParesVariante(variant),
                    variant.hasImage() ? "CON IMAGEN" : "sin imagen"));
        }

        // Limpiar archivos temporales
        archivosPorFila.clear();

        return variants;
    }

    private void procesarImagenDeVariante(ModelProductVariant variant, int filaIndex) {
        try {
            // Verificar archivo asociado a esta fila
            File archivoImagen = archivosPorFila.get(filaIndex);

            if (archivoImagen != null && archivoImagen.exists()) {
                // Leer archivo y convertir a bytes
                byte[] imageBytes = Files.readAllBytes(archivoImagen.toPath());

                if (imageBytes != null && imageBytes.length > 0) {
                    variant.setImageBytes(imageBytes);
                    System.out.println(String.format("???? Imagen cargada para fila %d: %s (%d KB)",
                            filaIndex, archivoImagen.getName(), imageBytes.length / 1024));
                }
            } else {
                // Verificar imagen en la tabla (ImageIcon)
                DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

                if (model.getColumnCount() > 6) {
                    Object imageObj = model.getValueAt(filaIndex, 6);

                    if (imageObj instanceof ImageIcon) {
                        ImageIcon icon = (ImageIcon) imageObj;
                        byte[] imageBytes = convertirImageIconABytes(icon);
                        if (imageBytes != null) {
                            variant.setImageBytes(imageBytes);
                            System.out.println(String.format("???? Imagen de tabla cargada para fila %d", filaIndex));
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("?????? Error procesando imagen para fila " + filaIndex + ": " + e.getMessage());
            // No lanzar excepci??n, solo registrar el error
        }
    }

    private byte[] convertirImageIconABytes(ImageIcon icon) {
        try {
            BufferedImage bufferedImage = new BufferedImage(
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = bufferedImage.createGraphics();
            icon.paintIcon(null, g2d, 0, 0);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("Error convirtiendo ImageIcon: " + e.getMessage());
            return null;
        }
    }

    private int calcularTotalParesVariante(raven.controlador.productos.ModelProductVariant variant) {
        raven.controlador.inventario.InventarioBodega inv = new raven.controlador.inventario.InventarioBodega();
        inv.setStockPar(variant.getStockPairs());
        inv.setStockCaja(variant.getStockBoxes());
        return inv.getTotalStockInPairs();
    }

    private byte[] obtenerBytesImagenFila(int filaIndex) {
        try {
            File archivo = archivosPorFila.get(filaIndex);
            if (archivo != null && archivo.exists()) {
                return java.nio.file.Files.readAllBytes(archivo.toPath());
            }
            DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
            if (model.getColumnCount() > 6) {
                Object imageObj = model.getValueAt(filaIndex, 6);
                if (imageObj instanceof ImageIcon) {
                    return convertirImageIconABytesOptimizado((ImageIcon) imageObj);
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private void guardarImagenesVariantesPorProducto(ModelProduct product) {
        try {
            if (product == null || product.getVariants() == null)
                return;
            raven.clases.productos.ServiceProductVariant svc = new raven.clases.productos.ServiceProductVariant();
            DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
            byte[] fallback = null;
            for (int j = 0; j < model.getRowCount(); j++) {
                fallback = obtenerBytesImagenFila(j);
                if (fallback != null && fallback.length > 0)
                    break;
            }
            if ((fallback == null || fallback.length == 0) && product.getProfile() != null
                    && product.getProfile().getImageBytes() != null) {
                fallback = product.getProfile().getImageBytes();
            }
            for (int i = 0; i < product.getVariants().size(); i++) {
                ModelProductVariant v = product.getVariants().get(i);
                int variantId = v.getVariantId();
                if (variantId <= 0 && i < model.getRowCount()) {
                    Integer idFromMap = variantIdPorFila.get(i);
                    if (idFromMap != null && idFromMap > 0)
                        variantId = idFromMap;
                }
                if (variantId <= 0)
                    continue;
                byte[] bytes = obtenerBytesImagenFila(i);
                if (bytes == null || bytes.length == 0) {
                    bytes = v.getImageBytes();
                    if (bytes == null || bytes.length == 0)
                        bytes = fallback;
                }
                if (bytes != null && bytes.length > 0) {
                    try {
                        svc.updateVariantImage(variantId, bytes);
                    } catch (Exception e) {
                        System.err.println("?????? No se pudo actualizar imagen de variante ID " + variantId + ": "
                                + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("?????? Error guardando im??genes por producto: " + e.getMessage());
        }
    }

    /**
     * Guarda un producto completo con todas sus variantes usando el sistema
     * integrado
     */
    private boolean guardarProductoCompleto(ModelProduct product) throws Exception {
        // Usar ServiceProduct que ahora integra con ServiceProductVariant
        ServiceProduct serviceProduct = new ServiceProduct();

        // El ServiceProduct maneja autom??ticamente:
        // - Guardar producto base
        // - Guardar variantes con im??genes en campo imagen de producto_variantes
        // - Generar c??digos autom??ticamente
        int userId = obtenerIdUsuarioActual();

        return serviceProduct.guardarProductoCompleto(product, userId);
    }

    /**
     * Guarda un producto completo especificando el usuario
     */
    public boolean guardarProductoConUsuario(int userId) {
        if (isSaving)
            return false;
        isSaving = true;
        try {
            // Validar datos b??sicos
            if (!validarFormulario()) {
                return false;
            }

            if (tablaProd.getRowCount() == 0) {
                showError("Debe agregar al menos una variante", null);
                return false;
            }

            // ??? DETECTAR CAMBIOS SI ESTAMOS EN MODO EDICI??N
            if (isEditMode && currentProductId > 0) {
                changeTracker.detectChanges(
                        (DefaultTableModel) tablaProd.getModel(),
                        variantIdPorFila,
                        archivosPorFila,
                        bodegaIdPorFila);

                if (!changeTracker.hasChanges()) {
                    showInfo("No hay cambios que guardar");
                    return false;
                }

                // Guardar solo cambios detectados
                return guardarCambiosInteligente(userId);
            } else {
                // Modo creaci??n normal
                return guardarProductoNuevo(userId);
            }

        } catch (Exception e) {
            showError("Error guardando producto", e);
            return false;
        } finally {
            isSaving = false;
        }
    }

    // ========================================================================
    // NUEVO M??TODO: Guardado inteligente de cambios
    // ========================================================================
    private boolean guardarCambiosInteligente(int userId) throws Exception {
        System.out.println("\n???? GUARDADO INTELIGENTE - SOLO CAMBIOS DETECTADOS");

        ServiceProduct serviceProduct = new ServiceProduct();
        raven.clases.productos.ServiceProductVariant variantService = new raven.clases.productos.ServiceProductVariant();

        boolean exito = true;
        int variantesActualizadas = 0;
        int variantesCreadas = 0;
        int variantesEliminadas = 0;

        // 1?????? ACTUALIZAR PRODUCTO BASE (solo si cambi??)
        if (cambioDatosBasicosProducto()) {
            ModelProduct productBase = construirModeloProductoBase();
            productBase.setProductId(currentProductId);
            serviceProduct.update(productBase, userId);
            System.out.println("??? Producto base actualizado");
        }

        // 2?????? PROCESAR VARIANTES MODIFICADAS
        for (String key : changeTracker.getModifiedVariants()) {
            try {
                actualizarVarianteExistente(key, userId, variantService);
                variantesActualizadas++;
            } catch (Exception e) {
                System.err.println("??? Error actualizando variante " + key + ": " + e.getMessage());
                exito = false;
            }
        }

        // 3?????? PROCESAR VARIANTES NUEVAS
        for (String key : changeTracker.getNewVariants()) {
            try {
                crearNuevaVariante(key, userId, variantService);
                variantesCreadas++;
            } catch (Exception e) {
                System.err.println("??? Error creando variante " + key + ": " + e.getMessage());
                exito = false;
            }
        }

        // 4?????? PROCESAR VARIANTES ELIMINADAS (batch para mayor rendimiento)
        try {
            int eliminadas = eliminarVariantesBatch(changeTracker.getDeletedVariants());
            variantesEliminadas += eliminadas;
        } catch (Exception e) {
            System.err.println("??? Error eliminando variantes en lote: " + e.getMessage());
            exito = false;
        }

        // 5?????? APLICAR PRECIOS SI CAMBIARON
        if (preciosModificados) {
            aplicarCambiosPrecios();
        }

        // Reporte final
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("??? CAMBIOS GUARDADOS:\n\n");
        if (variantesActualizadas > 0)
            mensaje.append("??? Variantes actualizadas: ").append(variantesActualizadas).append("\n");
        if (variantesCreadas > 0)
            mensaje.append("??? Variantes creadas: ").append(variantesCreadas).append("\n");
        if (variantesEliminadas > 0)
            mensaje.append("??? Variantes eliminadas: ").append(variantesEliminadas).append("\n");

        showSuccess(mensaje.toString());

        // Limpiar flags
        tablaModificada = false;
        preciosModificados = false;

        return exito;
    }

    private void crearNuevaVariante(String key, int userId,
            raven.clases.productos.ServiceProductVariant variantService) throws Exception {

        // Encontrar la fila correspondiente en la tabla
        int fila = encontrarFilaPorKey(key);
        if (fila < 0) {
            System.err.println("?????? No se encontr?? fila para key: " + key);
            return;
        }

        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

        // Extraer datos de la tabla
        String talla = (String) model.getValueAt(fila, 0);
        String color = (String) model.getValueAt(fila, 1);
        int cantidad = (Integer) model.getValueAt(fila, 3);
        String tipo = (String) model.getValueAt(fila, 4);
        String bodegaNombre = (String) model.getValueAt(fila, 5);
        String ubicacion = null;
        if (model.getColumnCount() > 6) {
            Object ubicacionObj = model.getValueAt(fila, 6);
            if (ubicacionObj != null) {
                ubicacion = ubicacionObj.toString();
            }
        }
        if ((ubicacion == null || ubicacion.isEmpty()) && ubicacionEspecificaPorFila != null) {
            String ubicacionMapa = ubicacionEspecificaPorFila.get(fila);
            if (ubicacionMapa != null && !ubicacionMapa.isEmpty()) {
                ubicacion = ubicacionMapa;
            }
        }

        System.out.println(String.format("???? Creando nueva variante: %s - %s (%d %s)",
                talla, color, cantidad, tipo));

        // Obtener IDs necesarios
        raven.clases.productos.ServiceSize serviceSize = new raven.clases.productos.ServiceSize();
        raven.clases.productos.ServiceColor serviceColor = new raven.clases.productos.ServiceColor();

        int idTalla = obtenerIdTallaPorString(talla);
        if (idTalla <= 0) {
            idTalla = serviceSize.getSizeIdByName(talla);
        }

        int idColor = serviceColor.getColorIdByName(color);

        Integer idBodega = obtenerIdBodegaPorNombre(bodegaNombre);
        if (idBodega == null || idBodega <= 0) {
            idBodega = getCurrentBodegaId();
        }

        // Construir ModelProductVariant
        raven.controlador.productos.ModelProductVariant variant = new raven.controlador.productos.ModelProductVariant();

        variant.setProductId(currentProductId);
        variant.setSizeId(idTalla);
        variant.setColorId(idColor);
        variant.setSizeName(talla);
        variant.setColorName(color);

        // Configurar stock seg??n tipo
        if ("Par".equalsIgnoreCase(tipo)) {
            variant.setStockPairs(cantidad);
            variant.setStockBoxes(0);
        } else {
            variant.setStockPairs(0);
            variant.setStockBoxes(cantidad);
        }

        // Configurar precios
        try {
            String precioVentaTxt = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
            String precioCompraTxt = (txtPrecioCompra != null) ? txtPrecioCompra.getText() : "";

            if (precioVentaTxt != null && !precioVentaTxt.trim().isEmpty()) {
                variant.setSalePrice(limitarRangoPrecio(parsearPrecio(precioVentaTxt)));
            }

            if (precioCompraTxt != null && !precioCompraTxt.trim().isEmpty()) {
                variant.setPurchasePrice(limitarRangoPrecio(parsearPrecio(precioCompraTxt)));
            }
        } catch (Exception e) {
            System.err.println("?????? Error configurando precios: " + e.getMessage());
        }

        variant.setMinStock(1);
        variant.setAvailable(true);
        variant.setWarehouseId(idBodega);

        // Generar c??digos autom??ticos
        try {
            String skuGenerado = generarSKU(talla, color, tipo, fila + 1);
            variant.setSku(skuGenerado);

            String eanGenerado = generarEAN13(txtModelo.getText().trim(), talla, color, fila + 1);
            variant.setEan(eanGenerado);
        } catch (Exception e) {
            System.err.println("?????? Error generando c??digos: " + e.getMessage());
        }

        // Procesar imagen si existe
        java.io.File archivoImagen = archivosPorFila.get(fila);
        if (archivoImagen != null && archivoImagen.exists()) {
            try {
                byte[] imageBytes = java.nio.file.Files.readAllBytes(archivoImagen.toPath());
                variant.setImageBytes(imageBytes);
                System.out.println("???? Imagen agregada a nueva variante");
            } catch (Exception e) {
                System.err.println("?????? Error cargando imagen: " + e.getMessage());
            }
        }

        // Crear variante en BD
        raven.clases.productos.ServiceProduct serviceProduct = new raven.clases.productos.ServiceProduct();
        int idVarianteNueva = serviceProduct.createVariantForProduct(currentProductId, variant, userId);

        if (idVarianteNueva > 0) {
            variantIdPorFila.put(fila, idVarianteNueva);
            crearInventarioBodega(idVarianteNueva, idBodega, cantidad, tipo, ubicacion);
            System.out.println("??? Variante creada con ID: " + idVarianteNueva);
        } else {
            throw new Exception("No se pudo crear la variante en BD");
        }
    }

    /**
     * Elimina una variante y su inventario asociado
     */
    private void eliminarVariante(String key, int userId,
            raven.clases.productos.ServiceProductVariant variantService) throws Exception {

        // Extraer idVariante y idBodega del key
        String[] parts = key.split("_");
        if (parts.length != 2) {
            System.err.println("?????? Key inv??lido para eliminar: " + key);
            return;
        }

        int idVariante = Integer.parseInt(parts[0]);
        int idBodega = Integer.parseInt(parts[1]);

        System.out.println(String.format("??????? Eliminando variante ID %d de bodega %d",
                idVariante, idBodega));

        // 1. Primero desactivar el inventario (soft delete)
        String sqlInventario = "UPDATE inventario_bodega SET activo = 0, fecha_ultimo_movimiento = NOW() " +
                "WHERE id_variante = ? AND id_bodega = ?";

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sqlInventario)) {

            pst.setInt(1, idVariante);
            pst.setInt(2, idBodega);

            int rowsInventario = pst.executeUpdate();
            System.out.println("??? Inventario desactivado: " + rowsInventario + " registro(s)");
        }

        // 2. Verificar si esta variante tiene inventario en otras bodegas
        boolean tieneOtroInventario = verificarInventarioEnOtrasBodegas(idVariante, idBodega);

        // 3. Si no tiene inventario en otras bodegas, marcar variante como no
        // disponible
        if (!tieneOtroInventario) {
            String sqlVariante = "UPDATE producto_variantes SET disponible = 0 WHERE id_variante = ?";

            try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                    java.sql.PreparedStatement pst = conn.prepareStatement(sqlVariante)) {

                pst.setInt(1, idVariante);
                pst.executeUpdate();
                System.out.println("??? Variante marcada como no disponible");
            }
        }
    }

    /**
     * Crea un nuevo registro de inventario en bodega
     */
    private void crearInventarioBodega(int idVariante, int idBodega, int cantidad, String tipo,
            String ubicacionEspecifica)
            throws Exception {

        String sql = "INSERT INTO inventario_bodega " +
                "(id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, activo, fecha_ultimo_movimiento, ubicacion_especifica) "
                +
                "VALUES (?, ?, ?, ?, 0, 1, NOW(), ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "Stock_par = VALUES(Stock_par), " +
                "Stock_caja = VALUES(Stock_caja), " +
                "fecha_ultimo_movimiento = NOW(), " +
                "ubicacion_especifica = VALUES(ubicacion_especifica)";

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idBodega);
            pst.setInt(2, idVariante);

            if ("Par".equalsIgnoreCase(tipo)) {
                pst.setInt(3, cantidad); // Stock_par
                pst.setInt(4, 0); // Stock_caja
            } else {
                pst.setInt(3, 0); // Stock_par
                pst.setInt(4, cantidad); // Stock_caja
            }
            if (ubicacionEspecifica != null && !ubicacionEspecifica.trim().isEmpty()) {
                pst.setString(5, ubicacionEspecifica.trim());
            } else {
                pst.setNull(5, java.sql.Types.VARCHAR);
            }

            int rows = pst.executeUpdate();

            if (rows > 0) {
                System.out.println(String.format("??? Inventario creado: variante=%d, bodega=%d, %d %s",
                        idVariante, idBodega, cantidad, tipo));
            }
        }
    }

    /**
     * Verifica si una variante tiene inventario activo en otras bodegas
     */
    private boolean verificarInventarioEnOtrasBodegas(int idVariante, int idBodegaExcluir) {
        String sql = "SELECT COUNT(*) FROM inventario_bodega " +
                "WHERE id_variante = ? AND id_bodega != ? AND activo = 1 " +
                "AND (Stock_par > 0 OR Stock_caja > 0)";

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idVariante);
            pst.setInt(2, idBodegaExcluir);

            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("?????? Error verificando inventario: " + e.getMessage());
        }

        return false;
    }

    /**
     * SOBRECARGA: Encontrar fila por key (mejora la versi??n anterior)
     */
    private int encontrarFilaPorKey(String key) {
        // Key format: "idVariante_idBodega" o para nuevas: hash ??nico

        // Primero intentar buscar por idVariante_idBodega conocido
        for (Map.Entry<Integer, Integer> entry : variantIdPorFila.entrySet()) {
            Integer idVariante = entry.getValue();
            if (idVariante != null && idVariante > 0) {
                Integer idBodega = bodegaIdPorFila.get(entry.getKey());
                if (idBodega == null || idBodega <= 0)
                    idBodega = obtenerIdBodegaDeTabla(entry.getKey());
                String currentKey = idVariante + "_" + (idBodega != null ? idBodega : getCurrentBodegaId());
                if (currentKey.equals(key)) {
                    return entry.getKey();
                }
            }
        }

        // Si no se encuentra, buscar en nuevas variantes por contenido
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            // Construir key temporal para variantes nuevas
            String talla = (String) model.getValueAt(i, 0);
            String color = (String) model.getValueAt(i, 1);
            String tipo = (String) model.getValueAt(i, 4);

            // Generar hash temporal
            String tempKey = (talla + color + tipo).hashCode() + "_0";

            if (tempKey.equals(key)) {
                return i;
            }
        }

        return -1;
    }

    private Integer obtenerIdBodegaDeTabla(int fila) {
        try {
            DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();

            if (fila < 0 || fila >= model.getRowCount()) {
                return null;
            }

            if (model.getColumnCount() <= 5) {
                return getCurrentBodegaId();
            }

            String bodegaNombre = (String) model.getValueAt(fila, 5);
            return obtenerIdBodegaPorNombre(bodegaNombre);

        } catch (Exception e) {
            System.err.println("?????? Error obteniendo bodega de fila " + fila + ": " + e.getMessage());
            return getCurrentBodegaId();
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * OBTENER ID DE BODEGA POR NOMBRE - VERSI??N MEJORADA
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private Integer obtenerIdBodegaPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            System.err.println("?????? Nombre de bodega vac??o");
            return getCurrentBodegaId();
        }

        String nombreBuscado = nombre.trim();

        System.out.println("???? Buscando bodega por nombre: '" + nombreBuscado + "'");

        // ??? CASO ESPECIAL: "Todas" = bodega del usuario actual
        if ("Todas".equalsIgnoreCase(nombreBuscado)) {
            Integer idBodega = getCurrentBodegaId();
            System.out.println("   'Todas' -> Usando bodega del usuario: " + idBodega);
            return idBodega;
        }

        // ??? BUSCAR EN LA LISTA DE BODEGAS CARGADAS
        for (ModelBodegas bodega : bodegaList) {
            if (bodega.getNombre() == null)
                continue;

            String nombreBodega = bodega.getNombre().trim();

            if (nombreBodega.equalsIgnoreCase(nombreBuscado)) {
                System.out.println("??? Bodega encontrada: " + nombreBodega + " (ID: " + bodega.getIdBodega() + ")");
                return bodega.getIdBodega();
            }
        }

        System.err.println("??? Bodega '" + nombreBuscado + "' NO encontrada en bodegaList");
        System.err.println("   Bodegas disponibles:");
        for (ModelBodegas b : bodegaList) {
            System.err.println("     - " + b.getNombre() + " (ID: " + b.getIdBodega() + ")");
        }

        // ??? FALLBACK: bodega actual del usuario
        Integer fallbackId = getCurrentBodegaId();
        System.out.println("?????? Usando bodega del usuario como fallback: " + fallbackId);
        return fallbackId;
    }

    private Integer getCurrentBodegaId() {
        // 1. Intentar obtener de la selecci??n del combo
        Integer idBodega = obtenerIdBodegaSeleccionada();

        if (idBodega != null && idBodega > 0) {
            return idBodega;
        }

        // 2. Intentar obtener de UserSession
        try {
            idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            if (idBodega != null && idBodega > 0) {
                return idBodega;
            }
        } catch (Exception ignore) {
        }

        // 3. Intentar obtener de SessionManager
        try {
            idBodega = SessionManager.getInstance().getCurrentUserBodegaId();
            if (idBodega != null && idBodega > 0) {
                return idBodega;
            }
        } catch (Exception ignore) {
        }

        // 4. Fallback: buscar primera bodega activa en BD
        try {
            String sql = "SELECT id_bodega FROM bodegas WHERE activo = 1 ORDER BY id_bodega LIMIT 1";

            try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                    java.sql.PreparedStatement pst = conn.prepareStatement(sql);
                    java.sql.ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("id_bodega");
                }
            }
        } catch (Exception e) {
            System.err.println("?????? Error obteniendo bodega por defecto: " + e.getMessage());
        }

        // ??ltimo recurso: retornar null
        return null;
    }

    // ========================================================================
    // M??TODO AUXILIAR: Actualizar variante existente
    // ========================================================================
    private void actualizarVarianteExistente(String key, int userId,
            ServiceProductVariant variantService) throws Exception {

        // Obtener datos de la fila actual
        int fila = encontrarFilaPorKey(key);
        if (fila < 0)
            return;

        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        Integer idVariante = variantIdPorFila.get(fila);

        if (idVariante == null || idVariante <= 0) {
            System.err.println("?????? No se encontr?? id_variante para key: " + key);
            return;
        }

        // Construir datos actualizados
        String talla = (String) model.getValueAt(fila, 0);
        String color = (String) model.getValueAt(fila, 1);
        int cantidad = (Integer) model.getValueAt(fila, 3);
        String tipo = (String) model.getValueAt(fila, 4);
        String bodegaNombre = (String) model.getValueAt(fila, 5);

        // Obtener id_bodega
        Integer idBodega = obtenerIdBodegaPorNombre(bodegaNombre);
        if (idBodega == null || idBodega <= 0) {
            idBodega = getCurrentBodegaId();
        }

        System.out.println(String.format("???? Actualizando variante ID %d en bodega %d",
                idVariante, idBodega));

        // ??? ACTUALIZAR INVENTARIO SIN DUPLICAR
        actualizarInventarioBodegaSinDuplicar(idVariante, idBodega, cantidad, tipo);

        // Actualizar imagen si cambi??
        java.io.File archivoImagen = archivosPorFila.get(fila);
        if (archivoImagen != null && archivoImagen.exists()) {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(archivoImagen.toPath());
            variantService.updateVariantImage(idVariante, imageBytes);
            System.out.println("???? Imagen actualizada para variante " + idVariante);
        }
    }

    // ========================================================================
    // M??TODO CR??TICO: Actualizar inventario sin duplicar
    // ========================================================================
    private void actualizarInventarioBodegaSinDuplicar(int idVariante, int idBodega,
            int cantidad, String tipo) throws Exception {

        String sql = "INSERT INTO inventario_bodega " +
                "(id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, activo, fecha_ultimo_movimiento) " +
                "VALUES (?, ?, ?, ?, 0, 1, NOW()) " +
                "ON DUPLICATE KEY UPDATE " +
                "Stock_par = VALUES(Stock_par), " +
                "Stock_caja = VALUES(Stock_caja), " +
                "fecha_ultimo_movimiento = NOW()";

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, idBodega);
            pst.setInt(2, idVariante);

            if ("Par".equalsIgnoreCase(tipo)) {
                pst.setInt(3, cantidad); // Stock_par
                pst.setInt(4, 0); // Stock_caja
            } else {
                pst.setInt(3, 0); // Stock_par
                pst.setInt(4, cantidad); // Stock_caja
            }

            int rows = pst.executeUpdate();
            System.out.println(String.format("??? Inventario actualizado: variante=%d, bodega=%d, cantidad=%d %s",
                    idVariante, idBodega, cantidad, tipo));
        }
    }

    // ========================================================================
    // M??TODO AUXILIAR: Verificar cambios en datos b??sicos
    // ========================================================================
    private boolean cambioDatosBasicosProducto() {
        // Implementar l??gica para detectar si cambi?? nombre, descripci??n, etc.
        // Por ahora retornamos true para siempre actualizar
        return true;
    }

    // ========================================================================
    // M??TODO AUXILIAR: Aplicar cambios de precios
    // ========================================================================
    private void aplicarCambiosPrecios() {
        try {
            aplicarPrecioCompraDesdeCampo();
            aplicarPrecioVentaDesdeCampo();
            System.out.println("???? Precios actualizados");
        } catch (Exception e) {
            System.err.println("??? Error aplicando precios: " + e.getMessage());
        }
    }

    // ========================================================================
    // MANTENER M??TODO ORIGINAL PARA CREACI??N
    // ========================================================================
    private boolean guardarProductoNuevo(int userId) throws Exception {
        // Usar la l??gica existente de ejecutarGuardadoConImagenes()
        ModelProduct product = construirModeloProductoBase();
        if (product == null) {
            showError("Error procesando datos del producto", null);
            return false;
        }

        List<ModelProductVariant> variants = construirVariantesConImagenes(product);
        product.setVariants(variants);

        boolean exito = guardarProductoCompleto(product);

        if (exito) {
            currentProductId = product.getProductId();
            aplicarPrecioCompraDesdeCampo();
            aplicarPrecioVentaDesdeCampo();
            guardarImagenesVariantesPorProducto(product);
            mostrarResultadoGuardado(product);
            limpiarFormularioCompleto();
            tablaModificada = false;
            preciosModificados = false;
        }

        return exito;
    }

    /**
     * Obtiene la ubicaci??n del usuario actual
     */
    private String obtenerUbicacionUsuario(int userId) {
        try {
            raven.dao.UsuariosDAO dao = new raven.dao.UsuariosDAO();
            String ubicacion = dao.getUbicacionById(userId);
            return (ubicacion != null) ? ubicacion : "bodega";
        } catch (SQLException e) {
            System.err.println("Error obteniendo ubicaci??n del usuario: " + e.getMessage());
            return "bodega";
        }
    }

    /**
     * Configura la ubicaci??n del producto y sus variantes
     */
    private ModelProduct configurarUbicacionProducto(ModelProduct product, String ubicacion) {
        // Establecer ubicaci??n en el producto base
        // Nota: Necesitar??s agregar este campo al ModelProduct
        // product.setUbicacion(ubicacion);

        // Configurar ubicaciones espec??ficas en las variantes
        for (ModelProductVariant variant : product.getVariants()) {
            if ("bodega".equals(ubicacion)) {
                variant.setWarehouseLocation("BODEGA-PRINCIPAL");
                variant.setStoreLocation(null);
            } else if ("tienda".equals(ubicacion)) {
                variant.setStoreLocation("TIENDA-PRINCIPAL");
                variant.setWarehouseLocation(null);
            }
        }

        return product;
    }

    /**
     * Obtiene el ID del usuario actual del sistema NOTA: Debes implementar esto
     * seg??n tu sistema de autenticaci??n
     */
    private int obtenerIdUsuarioActual() {
        // Primero intentar desde SessionManager
        int id = SessionManager.getInstance().getCurrentUserId();
        if (id > 0) {
            return id;
        }
        // Fallback: buscar primer usuario activo en BD
        try {
            return new raven.dao.UsuariosDAO().findFirstActiveUserId();
        } catch (SQLException e) {
            System.err.println("Error obteniendo id_usuario actual: " + e.getMessage());
            return -1;
        }
    }

    // ===================================================================
    // M??TODOS DE UTILIDAD PARA MENSAJES
    // ===================================================================
    private void showSuccess(String message) {
        try {
            Toast.show(this, Toast.Type.SUCCESS, message);
        } catch (Exception e) {
            System.out.println("??? " + message);
        }
    }

    private void showError(String message, Exception e) {
        try {
            Toast.show(this, Toast.Type.ERROR, message + (e != null ? ": " + e.getMessage() : ""));
        } catch (Exception ex) {
            System.err.println("??? " + message + (e != null ? ": " + e.getMessage() : ""));
        }
        if (e != null) {
            e.printStackTrace();
        }
    }

    private void showWarning(String message) {
        try {
            Toast.show(this, Toast.Type.WARNING, message);
        } catch (Exception e) {
            System.out.println("?????? " + message);
        }
    }

    private void showInfo(String message) {
        try {
            Toast.show(this, Toast.Type.INFO, message);
        } catch (Exception e) {
            System.out.println("?????? " + message);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new raven.datetime.component.date.DatePicker();
        jTextField1 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        panelInfoprod = new javax.swing.JPanel();
        txtNombre = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        comboProveedor = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        comboGenero = new javax.swing.JComboBox<>();
        comboCategoria = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        comboMarca = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtModelo = new javax.swing.JFormattedTextField();
        btnModelo = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        txtDescripcion = new javax.swing.JFormattedTextField();
        panelTit1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        panelVariante = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        tablaProd = new javax.swing.JTable();
        paneltit2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        BtnCrear_Variante = new javax.swing.JButton();
        cbxBodega = new javax.swing.JComboBox<>();
        Btn = new javax.swing.JButton();
        BtnAgrupar = new javax.swing.JButton();
        BtnImagen = new javax.swing.JButton();
        BtnPrecioVenta = new javax.swing.JButton();
        panelPrecios = new javax.swing.JPanel();
        label = new javax.swing.JLabel();
        txtPrecioCompra = new javax.swing.JFormattedTextField();
        jLabel9 = new javax.swing.JLabel();
        txtPrecioVenta = new javax.swing.JFormattedTextField();
        paneltit3 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();

        jTextField1.setText("jTextField1");

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Cantidad");

        txtNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNombreActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Nombre del Producto *");

        comboProveedor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboProveedorActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Marca");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Categoria");

        comboGenero.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "SELECCIONAR", "MUJER", "HOMBRE", "NIÑO", "UNISEX" }));
        comboGenero.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboGeneroItemStateChanged(evt);
            }
        });
        comboGenero.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboGeneroActionPerformed(evt);
            }
        });

        comboCategoria.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboCategoriaActionPerformed(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("genero");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Proveedor");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Código Modelo");

        txtModelo.setFocusable(false);
        txtModelo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtModeloActionPerformed(evt);
            }
        });

        btnModelo.setText("generar");
        btnModelo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnModeloActionPerformed(evt);
            }
        });

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Descripcion");

        txtDescripcion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDescripcionActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        jLabel5.setText("Información de producto");

        javax.swing.GroupLayout panelTit1Layout = new javax.swing.GroupLayout(panelTit1);
        panelTit1.setLayout(panelTit1Layout);
        panelTit1Layout.setHorizontalGroup(
                panelTit1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelTit1Layout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 225,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        panelTit1Layout.setVerticalGroup(
                panelTit1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelTit1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout panelInfoprodLayout = new javax.swing.GroupLayout(panelInfoprod);
        panelInfoprod.setLayout(panelInfoprodLayout);
        panelInfoprodLayout.setHorizontalGroup(
                panelInfoprodLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelTit1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelInfoprodLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(panelInfoprodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelInfoprodLayout.createSequentialGroup()
                                                .addGroup(panelInfoprodLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel2)
                                                        .addComponent(comboMarca,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 562,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(20, 20, 20)
                                                .addGroup(panelInfoprodLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(comboCategoria,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 325,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelInfoprodLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addGroup(panelInfoprodLayout.createSequentialGroup()
                                                        .addComponent(txtModelo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                717, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGap(20, 20, 20)
                                                        .addComponent(btnModelo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addComponent(jLabel6)
                                                .addComponent(jLabel15)
                                                .addGroup(panelInfoprodLayout.createSequentialGroup()
                                                        .addGroup(panelInfoprodLayout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jLabel4)
                                                                .addComponent(comboProveedor,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 562,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGap(20, 20, 20)
                                                        .addGroup(panelInfoprodLayout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(comboGenero, 0,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        Short.MAX_VALUE)
                                                                .addGroup(panelInfoprodLayout.createSequentialGroup()
                                                                        .addComponent(jLabel13)
                                                                        .addGap(0, 286, Short.MAX_VALUE))))
                                                .addComponent(txtNombre)
                                                .addComponent(txtDescripcion)
                                                .addComponent(jLabel1)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        panelInfoprodLayout.setVerticalGroup(
                panelInfoprodLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelInfoprodLayout.createSequentialGroup()
                                .addComponent(panelTit1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 37,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelInfoprodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelInfoprodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(comboCategoria)
                                        .addComponent(comboMarca, javax.swing.GroupLayout.PREFERRED_SIZE, 36,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelInfoprodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(jLabel13))
                                .addGap(10, 10, 10)
                                .addGroup(panelInfoprodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(comboGenero)
                                        .addComponent(comboProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelInfoprodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtModelo, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnModelo, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, 39,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)));

        panelVariante.setBackground(new java.awt.Color(102, 153, 255));

        tablaProd.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Talla", "Color", "Cantidad", "Tipo"
                }) {
            Class[] types = new Class[] {
                    java.lang.String.class, // Talla - String
                    java.lang.String.class, // Color - String
                    java.lang.Integer.class, // Cantidad - Integer
                    java.lang.String.class, // Tipo - String
                    java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    false, // Talla - no editable
                    false, // color
                    true, // Cantidad - editable
                    false, // Tipo - no editable
                    false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane5.setViewportView(tablaProd);

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        jLabel8.setText("Variantes del Producto ");

        javax.swing.GroupLayout paneltit2Layout = new javax.swing.GroupLayout(paneltit2);
        paneltit2.setLayout(paneltit2Layout);
        paneltit2Layout.setHorizontalGroup(
                paneltit2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(paneltit2Layout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 225,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(579, Short.MAX_VALUE)));
        paneltit2Layout.setVerticalGroup(
                paneltit2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(paneltit2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()));

        BtnCrear_Variante.setText("Nueva");

        cbxBodega.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Bodega", "Item 1", "Item 2", "Item 3", "Item 4" }));

        Btn.setText("SECUNDARIO");

        BtnAgrupar.setText("Agrupar");

        BtnImagen.setText("Imagen Grupo");

        BtnPrecioVenta.setText("Precio Grupo");

        javax.swing.GroupLayout panelVarianteLayout = new javax.swing.GroupLayout(panelVariante);
        panelVariante.setLayout(panelVarianteLayout);
        panelVarianteLayout.setHorizontalGroup(
                panelVarianteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelVarianteLayout.createSequentialGroup()
                                .addGroup(panelVarianteLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelVarianteLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jScrollPane5))
                                        .addGroup(panelVarianteLayout.createSequentialGroup()
                                                .addComponent(paneltit2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxBodega, 0, 111, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                panelVarianteLayout.createSequentialGroup()
                                                        .addGap(0, 0, Short.MAX_VALUE)
                                                        .addComponent(BtnPrecioVenta)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(BtnImagen)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(BtnAgrupar)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(Btn)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(BtnCrear_Variante)))
                                .addContainerGap()));
        panelVarianteLayout.setVerticalGroup(
                panelVarianteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelVarianteLayout.createSequentialGroup()
                                .addGroup(panelVarianteLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(paneltit2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxBodega))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelVarianteLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(BtnCrear_Variante)
                                        .addComponent(Btn)
                                        .addComponent(BtnAgrupar)
                                        .addComponent(BtnImagen)
                                        .addComponent(BtnPrecioVenta))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 202,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(21, Short.MAX_VALUE)));

        label.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        label.setText("precio compra");

        txtPrecioCompra.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
                new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0"))));
        txtPrecioCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioCompraActionPerformed(evt);
            }
        });
        txtPrecioCompra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPrecioCompraKeyPressed(evt);
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtPrecioCompraKeyReleased(evt);
            }
        });

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Precio Venta");

        txtPrecioVenta.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
                new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0"))));
        txtPrecioVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioVentaActionPerformed(evt);
            }
        });

        paneltit3.setBackground(new java.awt.Color(255, 153, 153));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        jLabel10.setText("Información de Precios ");

        javax.swing.GroupLayout paneltit3Layout = new javax.swing.GroupLayout(paneltit3);
        paneltit3.setLayout(paneltit3Layout);
        paneltit3Layout.setHorizontalGroup(
                paneltit3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(paneltit3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 856,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(81, Short.MAX_VALUE)));
        paneltit3Layout.setVerticalGroup(
                paneltit3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(paneltit3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout panelPreciosLayout = new javax.swing.GroupLayout(panelPrecios);
        panelPrecios.setLayout(panelPreciosLayout);
        panelPreciosLayout.setHorizontalGroup(
                panelPreciosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(paneltit3, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelPreciosLayout.createSequentialGroup()
                                .addComponent(label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtPrecioCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 244,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 77,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtPrecioVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 314,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(77, 77, 77)));
        panelPreciosLayout.setVerticalGroup(
                panelPreciosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPreciosLayout.createSequentialGroup()
                                .addComponent(paneltit3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelPreciosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelPreciosLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(txtPrecioVenta, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel9))
                                        .addGroup(panelPreciosLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(txtPrecioCompra, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(label)))
                                .addGap(9, 9, 9)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(panelPrecios, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelInfoprod, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelVariante, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(35, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelInfoprod, javax.swing.GroupLayout.PREFERRED_SIZE, 391,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30)
                                .addComponent(panelVariante, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(32, 32, 32)
                                .addComponent(panelPrecios, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(10, 10, 10)));
    }// </editor-fold>//GEN-END:initComponents

    private void txtNombreActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtNombreActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtNombreActionPerformed

    private void txtPrecioVentaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtPrecioVentaActionPerformed
        aplicarPrecios();
        preciosModificados = true;
    }// GEN-LAST:event_txtPrecioVentaActionPerformed

    private void txtDescripcionActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtDescripcionActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtDescripcionActionPerformed

    private void comboGeneroItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_comboGeneroItemStateChanged
        onGeneroChanged(evt);
    }// GEN-LAST:event_comboGeneroItemStateChanged

    private void txtPrecioCompraActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtPrecioCompraActionPerformed
        actualizarPrecioCompraCajaEnBD();
        preciosModificados = true;
    }// GEN-LAST:event_txtPrecioCompraActionPerformed

    private void comboGeneroActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboGeneroActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_comboGeneroActionPerformed

    private void txtPrecioCompraKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtPrecioCompraKeyReleased
        preciosModificados = true;
    }// GEN-LAST:event_txtPrecioCompraKeyReleased

    private void txtPrecioCompraKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtPrecioCompraKeyPressed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtPrecioCompraKeyPressed

    private void btnModeloActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnModeloActionPerformed
        bloquearBotonTemporalmente(btnModelo);
        generarCodigoModelo();
    }// GEN-LAST:event_btnModeloActionPerformed

    private void comboCategoriaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboCategoriaActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_comboCategoriaActionPerformed

    private void comboProveedorActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboProveedorActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_comboProveedorActionPerformed

    private void txtModeloActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtModeloActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtModeloActionPerformed

    private static class ResumenBodega {
        int pares;
        int cajas;
    }

    private void BtnActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_BtnActionPerformed
        if (currentProductId <= 0 || currentProduct == null) {
            mostrarResumenRapidoPorColorProveedorBodega();
            return;
        }

        String[] options = new String[] { "Editar precios", "Ver resumen", "Cancelar" };
        int choice = javax.swing.JOptionPane.showOptionDialog(
                this,
                "Seleccione la acción a realizar para este producto",
                "Acción rápida",
                javax.swing.JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            abrirEdicionRapidaPrecios();
        } else if (choice == 1) {
            mostrarResumenRapidoPorColorProveedorBodega();
        }
    }// GEN-LAST:event_BtnActionPerformed

    private void abrirEdicionRapidaPrecios() {
        try {
            if (currentProductId <= 0 || currentProduct == null) {
                showWarning("Guarde el producto antes de editar precios");
                return;
            }

            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            java.awt.Frame parentFrame = window instanceof java.awt.Frame ? (java.awt.Frame) window : null;

            EdicionRapidaPreciosDialog dialogo = new EdicionRapidaPreciosDialog(
                    parentFrame,
                    currentProductId,
                    currentProduct.getName());
            dialogo.setLocationRelativeTo(this);
            dialogo.setVisible(true);

            if (dialogo.isCambiosGuardados()) {
                cargarVariantesCompletasDesdeDB(currentProductId);
                cargarPreciosPorTipoDesdeDB(currentProductId);
                showSuccess("Precios actualizados correctamente");
            }
        } catch (Exception e) {
            showError("Error al abrir el editor de precios", e);
        }
    }

    private void mostrarResumenRapidoPorColorProveedorBodega() {
        if (tablaProd == null || tablaProd.getRowCount() == 0) {
            showInfo("No hay variantes en la tabla para resumir");
            return;
        }

        int paresPorCaja = 24;
        try {
            if (currentProduct != null && currentProduct.getPairsPerBox() > 0) {
                paresPorCaja = currentProduct.getPairsPerBox();
            }
        } catch (Exception ignore) {
        }

        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tablaProd.getModel();
        java.util.Map<String, java.util.Map<String, ResumenBodega>> resumen = new java.util.LinkedHashMap<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            Object colorObj = model.getValueAt(i, 1);
            Object proveedorObj = model.getValueAt(i, 2);
            Object cantidadObj = model.getValueAt(i, 3);
            Object tipoObj = model.getValueAt(i, 4);
            Object bodegaObj = model.getValueAt(i, 5);

            if (colorObj == null || proveedorObj == null || bodegaObj == null || cantidadObj == null
                    || tipoObj == null) {
                continue;
            }

            String color = colorObj.toString().trim();
            String proveedor = proveedorObj.toString().trim();
            String bodega = bodegaObj.toString().trim();
            String tipo = tipoObj.toString().trim();

            if (color.isEmpty() || proveedor.isEmpty() || bodega.isEmpty()) {
                continue;
            }

            int cantidad;
            try {
                if (cantidadObj instanceof Number) {
                    cantidad = ((Number) cantidadObj).intValue();
                } else {
                    cantidad = Integer.parseInt(cantidadObj.toString());
                }
            } catch (Exception ignore) {
                continue;
            }

            if (cantidad <= 0) {
                continue;
            }

            String claveGrupo = color + "||" + proveedor;
            java.util.Map<String, ResumenBodega> porBodega = resumen.get(claveGrupo);
            if (porBodega == null) {
                porBodega = new java.util.LinkedHashMap<>();
                resumen.put(claveGrupo, porBodega);
            }

            ResumenBodega rb = porBodega.get(bodega);
            if (rb == null) {
                rb = new ResumenBodega();
                porBodega.put(bodega, rb);
            }

            if ("Par".equalsIgnoreCase(tipo)) {
                rb.pares += cantidad;
            } else if ("Caja".equalsIgnoreCase(tipo)) {
                rb.cajas += cantidad;
            } else {
                rb.pares += cantidad;
            }
        }

        if (resumen.isEmpty()) {
            showInfo("No hay datos válidos para resumir");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, java.util.Map<String, ResumenBodega>> entryGrupo : resumen.entrySet()) {
            String claveGrupo = entryGrupo.getKey();
            String[] partes = claveGrupo.split("\\|\\|", 2);
            String color = partes.length > 0 ? partes[0] : "";
            String proveedor = partes.length > 1 ? partes[1] : "";

            sb.append("Color: ").append(color)
                    .append(" | Proveedor: ").append(proveedor).append("\n");

            int totalParesEquivalentes = 0;

            for (java.util.Map.Entry<String, ResumenBodega> entryBodega : entryGrupo.getValue().entrySet()) {
                String bodega = entryBodega.getKey();
                ResumenBodega rb = entryBodega.getValue();

                int pares = rb.pares;
                int cajas = rb.cajas;
                int paresEquivalentes = pares + (cajas * paresPorCaja);
                totalParesEquivalentes += paresEquivalentes;

                sb.append("  Bodega: ").append(bodega)
                        .append(" -> Pares: ").append(pares)
                        .append(", Cajas: ").append(cajas)
                        .append(", Total pares equivalentes: ").append(paresEquivalentes)
                        .append("\n");
            }

            sb.append("  Total zapatos (pares equivalentes): ")
                    .append(totalParesEquivalentes)
                    .append("\n\n");
        }

        javax.swing.JTextArea area = new javax.swing.JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        area.setCaretPosition(0);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(700, 400));

        javax.swing.JOptionPane.showMessageDialog(
                this,
                scroll,
                "Resumen rápido por color, proveedor y bodega",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    private void actualizarPrecioVentaPorTipoEnBD(String tipo) {
        try {
            if (currentProductId <= 0)
                return;
            String texto = (txtPrecioVenta != null ? txtPrecioVenta.getText() : "");
            if (texto == null || texto.trim().isEmpty())
                return;
            double nuevoPrecio = limitarRangoPrecio(parsearPrecio(texto));
            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            int rows = 0;
            if ("Par".equalsIgnoreCase(tipo)) {
                if (service.existePar(currentProductId))
                    rows = service.actualizarVentaParPorProducto(currentProductId, nuevoPrecio);
            } else {
                if (service.existeCaja(currentProductId))
                    rows = service.actualizarVentaCajaPorProducto(currentProductId, nuevoPrecio);
            }
            if (rows > 0)
                showSuccess("Precios " + tipo + " actualizados en " + rows + " variantes");
        } catch (Exception e) {
            showError("Error actualizando precios", e);
        }
    }

    private void actualizarPrecioCompraCajaEnBD() {
        try {
            if (currentProductId <= 0)
                return;
            String texto = (txtPrecioCompra != null ? txtPrecioCompra.getText() : "");
            if (texto == null || texto.trim().isEmpty())
                return;
            double nuevoPrecio = limitarRangoPrecio(parsearPrecio(texto));
            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            int rows = service.actualizarCompraCajaPorProducto(currentProductId, nuevoPrecio);
            if (rows > 0)
                showSuccess("Precio compra Caja actualizado en " + rows + " variantes");
        } catch (Exception e) {
            showError("Error actualizando precio compra Caja", e);
        }
    }

    private void aplicarPrecioVentaDesdeCampo() {
        try {
            if (currentProductId <= 0)
                return;
            String texto = (txtPrecioVenta != null ? txtPrecioVenta.getText() : "");
            if (texto == null || texto.trim().isEmpty())
                return;
            double nuevoPrecio = limitarRangoPrecio(parsearPrecio(texto));
            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            for (int i = 0; i < tablaProd.getRowCount(); i++) {
                Integer id = variantIdPorFila.get(i);
                if (id != null && id > 0)
                    ids.add(id);
            }
            int rows = ids.isEmpty() ? 0 : service.updatePrecioVentaByIds(ids, nuevoPrecio);
            if (rows > 0)
                showSuccess("Precio venta actualizado en " + rows + " variantes");
        } catch (Exception e) {
            showError("Error aplicando precio venta", e);
        }
    }

    private void aplicarPrecioCompraDesdeCampo() {
        try {
            if (currentProductId <= 0)
                return;
            String texto = (txtPrecioCompra != null ? txtPrecioCompra.getText() : "");
            if (texto == null || texto.trim().isEmpty())
                return;
            double nuevoPrecio = limitarRangoPrecio(parsearPrecio(texto));
            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            for (int i = 0; i < tablaProd.getRowCount(); i++) {
                Integer id = variantIdPorFila.get(i);
                if (id != null && id > 0)
                    ids.add(id);
            }
            int rows = ids.isEmpty() ? 0 : service.updatePrecioCompraByIds(ids, nuevoPrecio);
            if (rows > 0)
                showSuccess("Precio compra actualizado en " + rows + " variantes");
        } catch (Exception e) {
            showError("Error aplicando precio compra", e);
        }
    }

    private void aplicarPreciosPorTipoATodasLasVariantes() {
        try {
            if (currentProductId <= 0) {
                return;
            }

            String parTxt = (txtPrecioVenta != null) ? txtPrecioVenta.getText() : "";
            String cajaTxt = "";
            String compraTxt = (txtPrecioCompra != null) ? txtPrecioCompra.getText() : "";

            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            boolean hayPar = service.existePar(currentProductId);
            boolean hayCaja = service.existeCaja(currentProductId);

            if (hayPar && parTxt != null && !parTxt.trim().isEmpty()) {
                double precioPar = limitarRangoPrecio(parsearPrecio(parTxt.trim()));
                int n = service.actualizarVentaParPorProducto(currentProductId, precioPar);
                if (n > 0)
                    showSuccess("Precio PAR aplicado a " + n + " variantes");
            }
            if (hayCaja && cajaTxt != null && !cajaTxt.trim().isEmpty()) {
                double precioCaja = limitarRangoPrecio(parsearPrecio(cajaTxt.trim()));
                int n = service.actualizarVentaCajaPorProducto(currentProductId, precioCaja);
                if (n > 0)
                    showSuccess("Precio CAJA aplicado a " + n + " variantes");
            }
            if (hayCaja && compraTxt != null && !compraTxt.trim().isEmpty()) {
                double compraCaja = limitarRangoPrecio(parsearPrecio(compraTxt.trim()));
                int n = service.actualizarCompraCajaPorProducto(currentProductId, compraCaja);
                if (n > 0)
                    showSuccess("Precio COMPRA (caja) aplicado a " + n + " variantes");
            }
        } catch (Exception e) {
            showError("Error aplicando precios por tipo", e);
        }
    }

    private Map<String, List<Integer>> agruparIdsVariantePorTipo() {
        Map<String, List<Integer>> grupos = new HashMap<>();
        grupos.put("Par", new ArrayList<>());
        grupos.put("Caja", new ArrayList<>());
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Integer variantId = variantIdPorFila.get(i);
            if (variantId == null || variantId <= 0)
                continue;
            String tipo = (String) model.getValueAt(i, 4);
            if ("Caja".equalsIgnoreCase(tipo)) {
                grupos.get("Caja").add(variantId);
            } else {
                grupos.get("Par").add(variantId);
            }
        }
        return grupos;
    }

    private void aplicarPreciosPorTipoDesdeTabla() {
        try {
            if (currentProductId <= 0)
                return;
            String sPar = txtPrecioVenta != null && txtPrecioVenta.getText() != null ? txtPrecioVenta.getText().trim()
                    : "";
            String sCaja = sPar;
            String sCompra = txtPrecioCompra != null && txtPrecioCompra.getText() != null
                    ? txtPrecioCompra.getText().trim()
                    : "";
            Map<String, List<Integer>> grupos = agruparIdsVariantePorTipo();
            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            if (!sPar.isEmpty() && !grupos.get("Par").isEmpty()) {
                double precioPar = limitarRangoPrecio(parsearPrecio(sPar));
                service.updatePrecioVentaByIds(grupos.get("Par"), precioPar);
            }
            if (!sCaja.isEmpty() && !grupos.get("Caja").isEmpty()) {
                double precioCaja = limitarRangoPrecio(parsearPrecio(sCaja));
                service.updatePrecioVentaByIds(grupos.get("Caja"), precioCaja);
            }
            if (!sCompra.isEmpty() && !grupos.get("Caja").isEmpty()) {
                double precioCompra = limitarRangoPrecio(parsearPrecio(sCompra));
                java.util.List<Integer> idsCompra = new java.util.ArrayList<>(grupos.get("Caja"));
                idsCompra.addAll(grupos.get("Par"));
                service.updatePrecioCompraByIds(idsCompra, precioCompra);
            }
        } catch (Exception e) {
            showError("Error aplicando precios por tabla", e);
        }
    }

    private void reconstruirGruposDesdeTabla() {
        idsCaja.clear();
        idsPar.clear();
        DefaultTableModel tm = (DefaultTableModel) tablaProd.getModel();
        for (int r = 0; r < tm.getRowCount(); r++) {
            Integer idVar = variantIdPorFila.get(r);
            String tipo = (String) tm.getValueAt(r, 4);
            if (idVar != null && idVar > 0) {
                if (tipo != null && tipo.equalsIgnoreCase("Caja")) {
                    idsCaja.add(idVar);
                } else if (tipo != null && tipo.equalsIgnoreCase("Par")) {
                    idsPar.add(idVar);
                }
            }
        }
        txtPrecioVenta.setEnabled(true);
        txtPrecioCompra.setEnabled(true);
    }

    private void aplicarPrecios() {
        String sPar = txtPrecioVenta != null && txtPrecioVenta.getText() != null ? txtPrecioVenta.getText().trim() : "";
        String sCaja = sPar;
        String sCompra = txtPrecioCompra != null && txtPrecioCompra.getText() != null ? txtPrecioCompra.getText().trim()
                : "";

        try {
            Double ventaPar = sPar.isEmpty() ? null : limitarRangoPrecio(parsearPrecio(sPar));
            Double ventaCaja = sCaja.isEmpty() ? null : limitarRangoPrecio(parsearPrecio(sCaja));
            Double compraCaja = sCompra.isEmpty() ? null : limitarRangoPrecio(parsearPrecio(sCompra));

            ServiceProduct service = new ServiceProduct();
            int nPar = (ventaPar != null && !idsPar.isEmpty()) ? service.updatePrecioVentaByIds(idsPar, ventaPar) : 0;
            int nCaja = (ventaCaja != null && !idsCaja.isEmpty()) ? service.updatePrecioVentaByIds(idsCaja, ventaCaja)
                    : 0;
            // Aplicar compra a todos (Par y Caja): combinar listas
            java.util.List<Integer> allIds = new java.util.ArrayList<>();
            allIds.addAll(idsPar);
            allIds.addAll(idsCaja);
            int nCmp = (compraCaja != null && !allIds.isEmpty()) ? service.updatePrecioCompraByIds(allIds, compraCaja)
                    : 0;

            if (nPar > 0)
                showSuccess("Precios Par aplicados a " + nPar + " variantes");
            if (nCaja > 0)
                showSuccess("Precios Caja aplicados a " + nCaja + " variantes");
            if (nCmp > 0)
                showSuccess("Precio compra Caja aplicado a " + nCmp + " variantes");
        } catch (Exception ex) {
            showError("Error aplicando precios", null);
        }
    }

    private boolean hayTipoEnTabla(String tipo) {
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String t = (String) model.getValueAt(i, 4);
            if (t != null && tipo.equalsIgnoreCase(t)) {
                return true;
            }
        }
        return false;
    }

    private void validarRequerimientoPreciosSegunTabla() {
        // Sin avisos durante creaci??n/edici??n; el precio se aplica al final
    }

    private List<Integer> obtenerIdsVariantePorTipo(String tipo) {
        List<Integer> ids = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String t = (String) model.getValueAt(i, 4);
            if (t != null && tipo.equalsIgnoreCase(t)) {
                Integer idVar = variantIdPorFila.get(i);
                if (idVar != null && idVar > 0) {
                    ids.add(idVar);
                }
            }
        }
        return ids;
    }

    private void actualizarPrecioVentaGrupoDesdeTabla(String tipo) {
        try {
            if (currentProductId <= 0)
                return;
            String texto = "Par".equalsIgnoreCase(tipo) ? (txtPrecioVenta != null ? txtPrecioVenta.getText() : "") : "";
            if (texto == null || texto.trim().isEmpty())
                return;
            double nuevoPrecio = limitarRangoPrecio(parsearPrecio(texto.trim()));
            java.util.List<Integer> ids = obtenerIdsVariantePorTipo(tipo);
            raven.clases.productos.ServiceProduct service = new raven.clases.productos.ServiceProduct();
            if (!ids.isEmpty()) {
                service.updatePrecioVentaByIds(ids, nuevoPrecio);
                showSuccess(("Par".equalsIgnoreCase(tipo) ? "Precios Par" : "Precios Caja") + " actualizados en "
                        + ids.size() + " variantes");
                return;
            }
            if ("Par".equalsIgnoreCase(tipo)) {
                if (service.existePar(currentProductId)) {
                    int n = service.actualizarVentaParPorProducto(currentProductId, nuevoPrecio);
                    if (n > 0)
                        showSuccess("Precios Par actualizados en " + n + " variantes (por producto)");
                }
            } else {
                if (service.existeCaja(currentProductId)) {
                    int n = service.actualizarVentaCajaPorProducto(currentProductId, nuevoPrecio);
                    if (n > 0)
                        showSuccess("Precios Caja actualizados en " + n + " variantes (por producto)");
                }
            }
        } catch (Exception e) {
            showError("Error actualizando precios por grupo", e);
        }
    }

    public void init() {
        txtNombre.grabFocus();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton Btn;
    public javax.swing.JButton BtnAgrupar;
    public javax.swing.JButton BtnCrear_Variante;
    public javax.swing.JButton BtnImagen;
    public javax.swing.JButton BtnPrecioVenta;
    private javax.swing.JButton btnModelo;
    private javax.swing.JComboBox<String> cbxBodega;
    private javax.swing.JComboBox<Object> comboCategoria;
    private javax.swing.JComboBox<Object> comboGenero;
    private javax.swing.JComboBox<Object> comboMarca;
    private javax.swing.JComboBox<Object> comboProveedor;
    private raven.datetime.component.date.DatePicker datePicker;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel label;
    private javax.swing.JPanel panelInfoprod;
    private javax.swing.JPanel panelPrecios;
    private javax.swing.JPanel panelTit1;
    private javax.swing.JPanel panelVariante;
    private javax.swing.JPanel paneltit2;
    private javax.swing.JPanel paneltit3;
    private javax.swing.JTable tablaProd;
    private javax.swing.JFormattedTextField txtDescripcion;
    private javax.swing.JFormattedTextField txtModelo;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JFormattedTextField txtPrecioCompra;
    private javax.swing.JFormattedTextField txtPrecioVenta;
    // End of variables declaration//GEN-END:variables

    private int eliminarVariantesBatch(java.util.Set<String> keys) throws Exception {
        if (keys == null || keys.isEmpty())
            return 0;
        int count = 0;

        try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().createConnection()) {
            conn.setAutoCommit(false);

            String sqlInvPorBodega = "UPDATE inventario_bodega SET activo = 0, fecha_ultimo_movimiento = NOW() WHERE id_variante = ? AND id_bodega = ?";
            String sqlInvTodasBodegas = "UPDATE inventario_bodega SET activo = 0, fecha_ultimo_movimiento = NOW() WHERE id_variante = ? AND activo = 1";
            String sqlCheckActivo = "SELECT COUNT(*) FROM inventario_bodega WHERE id_variante = ? AND activo = 1 AND (Stock_par > 0 OR Stock_caja > 0)";
            String sqlVarianteNoDisp = "UPDATE producto_variantes SET disponible = 0 WHERE id_variante = ?";

            try (java.sql.PreparedStatement pstInvBod = conn.prepareStatement(sqlInvPorBodega);
                    java.sql.PreparedStatement pstInvAll = conn.prepareStatement(sqlInvTodasBodegas);
                    java.sql.PreparedStatement pstChk = conn.prepareStatement(sqlCheckActivo);
                    java.sql.PreparedStatement pstVar = conn.prepareStatement(sqlVarianteNoDisp)) {

                for (String key : keys) {
                    String[] parts = key.split("_");
                    if (parts.length != 2)
                        continue;
                    int idVariante = Integer.parseInt(parts[0]);
                    int idBodega = Integer.parseInt(parts[1]);

                    int rowsInv = 0;
                    if (idBodega > 0) {
                        pstInvBod.setInt(1, idVariante);
                        pstInvBod.setInt(2, idBodega);
                        rowsInv = pstInvBod.executeUpdate();
                    }
                    if (rowsInv == 0) {
                        pstInvAll.setInt(1, idVariante);
                        pstInvAll.executeUpdate();
                    }

                    pstChk.setInt(1, idVariante);
                    try (java.sql.ResultSet rs = pstChk.executeQuery()) {
                        boolean sigueActivo = false;
                        if (rs.next())
                            sigueActivo = rs.getInt(1) > 0;
                        if (!sigueActivo) {
                            pstVar.setInt(1, idVariante);
                            pstVar.executeUpdate();
                        }
                    }
                    count++;
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ignore) {
                }
            }
        }

        System.out.println("??? Variantes eliminadas en lote: " + count);
        return count;
    }

}
