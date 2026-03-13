package raven.application.form.productos.creates;


import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Component;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;
import javax.swing.BorderFactory;
import java.awt.Color;
import raven.componentes.impresion.DataSearch;
import raven.clases.productos.ServiceCategory;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.ServiceProductVariant;
import raven.componentes.impresion.EventClick;
import raven.componentes.impresion.PanelSearch;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelProductVariant;
import raven.modelos.PrestamoZapato;
import raven.controlador.admin.SessionManager;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import raven.application.form.principal.generarVentaFor1;
import raven.modal.Toast;
import raven.clases.productos.PrestamoZapatoService;
import raven.dao.PrestamoZapatoDAO;

/**
 *
 * @author CrisDEV
 */
public class CreatePrestamo extends javax.swing.JPanel {
        private DataSearch selectedProduct;
        private PanelSearch searchProd;
        private JPopupMenu menu;
        // Control de carga de combos
        private boolean isLoadingTallas = false;
        private boolean isLoadingColores = false;
        private final ServiceProduct service = new ServiceProduct();
    private final ServiceProductVariant serviceVariant = new ServiceProductVariant();

    public CreatePrestamo() {
        initComponents();
        initMenu();
        configurarDatePicker();
        panelInfoProd.putClientProperty(FlatClientProperties.STYLE, "arc:20;background:darken($Login.background,10%);");

        // Estilo de buscador para txtIngresarCodigo
        txtIngresarCodigo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtIngresarCodigo.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));
        txtIngresarCodigo.putClientProperty(FlatClientProperties.STYLE, ""+
                "arc:15;"+
                "borderWidth:0;"+
                "focusWidth:0;"+
                "innerFocusWidth:0;"+
                "margin:5,12,5,12;"+
                "background:darken($Login.background,10%)");
    }
    public void initMenu(){
        searchProd = new PanelSearch();
        menu = new JPopupMenu();
        menu.setFocusable(false);
        menu.setBorder(BorderFactory.createLineBorder(new Color(44,44,44)));
        menu.add(searchProd);
        // Configurar evento de clic para productos
        searchProd.addEventClick(new EventClick() {
            @Override
            public void itemClick(DataSearch data) {
              
                selectedProduct = data;
                seleccionarProducto(data);

                int idProducto = Integer.parseInt(data.getId_prod());
                cargarTallasProducto(idProducto);
                cargarColoresProducto(idProducto);

            }

            @Override
            public void itemRemove(Component com, DataSearch data) {
              
            }
        });
    }

    private void updateSearchResults() {
        String text = txtIngresarCodigo.getText().trim();
        List<DataSearch> resultados = search(text);
        searchProd.setData(resultados, menu);
        if (searchProd.getItemSize() > 0) {
            menu.show(txtIngresarCodigo, 0, txtIngresarCodigo.getHeight());
            menu.setPopupSize(menu.getWidth(), (searchProd.getItemSize() * 35) + 2);
        } else {
            menu.setVisible(false);
        }
    }
    private void configurarDatePicker() {
        datePicker.setCloseAfterSelected(true);
        datePicker.setEditor(ftdFecha);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        ftdFecha.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.DateFormatter(sdf)));
        if (ftdFecha.getText() == null || ftdFecha.getText().trim().isEmpty()) {
            ftdFecha.setText(sdf.format(new java.util.Date()));
        }
    }
        /**
     * Carga colores disponibles para un producto
     */
    private void cargarColoresProducto(int idProducto) {
        isLoadingColores = true;

        try {
            List<ModelProductVariant> variants = (List<ModelProductVariant>) serviceVariant.getVariantById(idProducto);

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

            System.out.println("Colores cargados para producto ID: " + idProducto + " - Total: " + coloresUnicos.size());
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
            List<ModelProductVariant> variants = (List<ModelProductVariant>) serviceVariant.getVariantById(idProducto);

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

            System.out.println("Tallas cargadas para producto ID: " + idProducto + " - Total: " + tallasUnicas.size());
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando tallas del producto");
        } finally {
            isLoadingTallas = false;
        }
    }
      /**
     * Construye el nombre completo de una talla
     */
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
    
     public PrestamoZapato getData() {
        // Validaciones básicas
        if (selectedProduct == null) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un producto antes de guardar");
            return null;
        }
        int varId = selectedProduct.getIdVariante();
        if (varId <= 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione talla y color para fijar la variante");
            return null;
        }
        // Validar pie seleccionado y disponibilidad
        String pieSeleccionado = null;
        if (CbxPie != null && CbxPie.getItemCount() > 0) {
            pieSeleccionado = String.valueOf(CbxPie.getSelectedItem());
        }
        if (pieSeleccionado == null || pieSeleccionado.toLowerCase().contains("selecionar")) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione el pie a prestar");
            return null;
        }
        if (pieSeleccionado.contains("Prestado") || pieSeleccionado.contains("No disponible")) {
            Toast.show(this, Toast.Type.WARNING, "El pie seleccionado no está disponible");
            return null;
        }
        String pieValor;
        String lower = pieSeleccionado.toLowerCase();
        if (lower.contains("derecho")) {
            pieValor = "DERECHO";
        } else if (lower.contains("izquierdo")) {
            pieValor = "IZQUIERDO";
        } else {
            // "Completo"
            pieValor = "AMBOS";
        }
        // Construcción del préstamo
        PrestamoZapato p = new PrestamoZapato();
        p.setNombrePrestatario(txtNombre.getText().trim());
        p.setCelularPrestatario(txtTelefono.getText().trim());
        // Dirección del prestatario (nuevo campo)
        p.setDireccionPrestatario(TxtDireccion != null ? TxtDireccion.getText().trim() : null);
        // Marcar si el pie proviene de un par nuevo segun selección ("Disponible - Nuevo")
        boolean esNuevoPar = lower.contains("nuevo");
        p.setNuevoPar(esNuevoPar);
        String obs = TxtDescripcion != null ? TxtDescripcion.getText().trim() : null;
        // No autoagregar "[NUEVO]" cuando observaciones está vacío
        if (esNuevoPar) {
            if (obs != null && !obs.isEmpty() && !obs.contains("[NUEVO]")) {
                obs = obs + " [NUEVO]";
            }
        }
        // Si está vacío, guardar como NULL en BD
        p.setObservaciones((obs != null && !obs.isEmpty()) ? obs : null);
        p.setIdProducto(Integer.parseInt(selectedProduct.getId_prod()));
        p.setIdVariante(Integer.valueOf(varId));
        p.setPie(pieValor);
        // Sesión
        p.setIdUsuario(SessionManager.getInstance().getCurrentUserId());
        p.setIdBodega(SessionManager.getInstance().getCurrentUserBodegaId());
        // Fecha del préstamo: fecha local del momento de creación
        p.setFechaPrestamo(new Timestamp(System.currentTimeMillis()));
        // Fecha de devolución
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date parsed = sdf.parse(ftdFecha.getText().trim());
            p.setFechaDevolucion(new Timestamp(parsed.getTime()));
        } catch (ParseException e) {
            Toast.show(this, Toast.Type.ERROR, "Formato de fecha inválido (dd/MM/yyyy)");
            return null;
        }
        return p;
    }


    public void loadData(ServiceCategory service, ModelCategory data) {

        // Si hay datos, cargar en los campos
        if (data != null) {
            txtNombre.setText(data.getName());
        }
    }

  /**
     * Realiza búsqueda inteligente de productos Búsqueda por EAN, código de
     * barras, nombre, descripción
     */
    private List<DataSearch> search(String search) {
        int limitData = 10;
        List<DataSearch> list = new ArrayList<>();

        if (search == null || search.trim().isEmpty()) {
            return getProductosSugeridos(limitData);
        }

        DataSearch productoPorEan = buscarProductoPorEanOBarcode(search.trim());
        if (productoPorEan != null) {
            list.add(productoPorEan);
            return list;
        }

        String sql = "SELECT DISTINCT p.*, "
                + "CASE "
                + "    WHEN p.codigo_modelo LIKE ? THEN 1 "
                + "    WHEN p.nombre LIKE ? THEN 2 "
                + "    WHEN p.descripcion LIKE ? THEN 3 "
                + "    ELSE 4 "
                + "END as relevancia "
                + "FROM productos p "
                + "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                + "WHERE (p.codigo_modelo LIKE ? OR p.nombre LIKE ? OR "
                + "       p.descripcion LIKE ? OR p.color LIKE ? OR p.talla LIKE ? OR "
                + "       pv.ean LIKE ?) "
                + "AND p.activo = 1 AND pv.disponible = 1 "
                + "AND (pv.stock_por_pares > 0 OR pv.stock_por_cajas > 0) "
                + "ORDER BY relevancia, p.nombre "
                + "LIMIT ?";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

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
            stmt.setString(9, searchParam);
            stmt.setInt(10, limitData);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DataSearch data = crearDataSearchDesdeResultSet(rs, search);
                    if (data != null) {
                        if (data.isStartsWithSearch()) {
                            list.add(0, data);
                        } else {
                            list.add(data);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error en búsqueda: " + e.getMessage());
        }

        return list;
    }
 private DataSearch buscarProductoPorEanOBarcode(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT p.*, pv.*, "
                + "c.nombre as color_nombre, t.numero as talla_nombre "
                + "FROM producto_variantes pv "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE (pv.ean = ?) "
                + "AND pv.disponible = 1 AND p.activo = 1 "
                + "LIMIT 1";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return crearDataSearchDesdeResultSet(rs, codigo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
 
    /**
     * Crea objeto DataSearch desde ResultSet
     */
    private boolean hasColumn(ResultSet rs, String column) {
        try {
            java.sql.ResultSetMetaData md = rs.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++) {
                String label = md.getColumnLabel(i);
                String name = md.getColumnName(i);
                if (column.equalsIgnoreCase(label) || column.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // sin acción
        }
        return false;
    }

    private DataSearch crearDataSearchDesdeResultSet(ResultSet rs, String search) {
        try {
            int idProducto = rs.getInt("id_producto");
            String codigoModelo = rs.getString("codigo_modelo");
            String nombre = rs.getString("nombre");
            // Preferir alias cuando estén disponibles
            String color = hasColumn(rs, "color_nombre") ? rs.getString("color_nombre") : rs.getString("color");
            String talla = hasColumn(rs, "talla_nombre") ? rs.getString("talla_nombre") : rs.getString("talla");
            BigDecimal precioVenta = rs.getBigDecimal("precio_venta");

            ServiceProductVariant.ProductVariantStats stats = serviceVariant.getProductStats(idProducto);
            int stockCajas = stats.totalBoxes;
            int stockPares = stats.totalPairsEquivalent;

            Blob imagen = obtenerImagenPrimeraVariante(idProducto);

            boolean startsWithSearch = search != null
                    && (codigoModelo.toLowerCase().startsWith(search.toLowerCase())
                    || nombre.toLowerCase().startsWith(search.toLowerCase()));

            int idVariante = 0;
            if (hasColumn(rs, "id_variante")) {
                idVariante = rs.getInt("id_variante");
            }

            return new DataSearch(
                    String.valueOf(idProducto), codigoModelo, nombre, color, talla,
                    startsWithSearch, rs.getString("descripcion"), rs.getInt("id_categoria"),
                    rs.getInt("id_marca"), rs.getInt("id_proveedor"),
                    rs.getBigDecimal("precio_compra"), precioVenta, stockPares,
                    rs.getInt("stock_minimo"), rs.getString("genero"),
                    rs.getBoolean("activo"), imagen, rs.getTimestamp("fecha_creacion"),
                    stockCajas, stockPares, rs.getInt("pares_por_caja"), idVariante
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Obtiene imagen de primera variante disponible
     */
    private Blob obtenerImagenPrimeraVariante(int idProducto) {
        String sql = "SELECT imagen FROM producto_variantes "
                + "WHERE id_producto = ? AND disponible = 1 AND imagen IS NOT NULL "
                + "ORDER BY id_variante LIMIT 1";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProducto);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBlob("imagen");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Obtiene imagen Blob de una variante específica por su ID.
     */
    private Blob obtenerImagenVariantePorId(int idVariante) {
        String sql = "SELECT imagen FROM producto_variantes "
                + "WHERE id_variante = ? AND disponible = 1 AND imagen IS NOT NULL";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVariante);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBlob("imagen");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Obtiene productos sugeridos cuando no hay búsqueda
     */
    private List<DataSearch> getProductosSugeridos(int limit) {
        List<DataSearch> list = new ArrayList<>();

        String sql = "SELECT p.*, "
                + "COALESCE((SELECT SUM(vd.cantidad) FROM venta_detalles vd "
                + "WHERE vd.id_producto = p.id_producto), 0) as total_vendido "
                + "FROM productos p "
                + "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                + "WHERE p.activo = 1 AND pv.disponible = 1 "
                + "AND (pv.stock_por_pares > 0 OR pv.stock_por_cajas > 0) "
                + "GROUP BY p.id_producto "
                + "ORDER BY total_vendido DESC, p.fecha_creacion DESC "
                + "LIMIT ?";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DataSearch data = crearDataSearchDesdeResultSet(rs, "");
                    if (data != null) {
                        list.add(data);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new raven.datetime.component.date.DatePicker();
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        txtTelefono = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        ftdFecha = new javax.swing.JFormattedTextField();
        jLabel5 = new javax.swing.JLabel();
        TxtDescripcion = new javax.swing.JTextField();
        panelInfoProd = new javax.swing.JPanel();
        ICON = new javax.swing.JLabel();
        txtNombreProducto = new javax.swing.JLabel();
        txtCodigo = new javax.swing.JLabel();
        txtStock = new javax.swing.JLabel();
        txtIngresarCodigo = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        comboColor = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        comboTalla = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        CbxPie = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        TxtDireccion = new javax.swing.JTextField();

        jTextField1.setText("jTextField1");

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Nombre");

        txtNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNombreActionPerformed(evt);
            }
        });

        txtTelefono.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTelefonoActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Teléfono");

        jLabel4.setText("Fecha de Devolución");

        jLabel5.setText("Descripciión");

        panelInfoProd.setBackground(new java.awt.Color(0, 153, 204));

        ICON.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ICON.setText("icono");

        txtNombreProducto.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        txtNombreProducto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtNombreProducto.setText("  ");

        txtCodigo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtCodigo.setText("Código:   ");

        txtStock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStock.setText(" Stock: 000");

        javax.swing.GroupLayout panelInfoProdLayout = new javax.swing.GroupLayout(panelInfoProd);
        panelInfoProd.setLayout(panelInfoProdLayout);
        panelInfoProdLayout.setHorizontalGroup(
            panelInfoProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInfoProdLayout.createSequentialGroup()
                .addGroup(panelInfoProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelInfoProdLayout.createSequentialGroup()
                        .addGap(92, 92, 92)
                        .addComponent(ICON, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 151, Short.MAX_VALUE))
                    .addGroup(panelInfoProdLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(txtNombreProducto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(panelInfoProdLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(txtCodigo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(txtStock, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelInfoProdLayout.setVerticalGroup(
            panelInfoProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInfoProdLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(ICON, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtNombreProducto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCodigo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtStock)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        txtIngresarCodigo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtIngresarCodigoKeyReleased(evt);
            }
        });

        jLabel6.setText("Buscar");

        jLabel7.setText("Color");

        comboColor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar" }));
        comboColor.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboColorItemStateChanged(evt);
            }
        });
        comboColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboColorActionPerformed(evt);
            }
        });

        jLabel8.setText("Talla");

        comboTalla.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar" }));
        comboTalla.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboTallaItemStateChanged(evt);
            }
        });

        jLabel9.setText("Pie");

        CbxPie.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Selecionar Pie", "Derecho", "Izquierdo", "Completo" }));

        jLabel10.setText("Dirección");

        TxtDireccion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TxtDireccionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(42, Short.MAX_VALUE)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comboColor, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboTalla, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtIngresarCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelInfoProd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(29, 29, 29))))
            .addGroup(layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(CbxPie, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ftdFecha)
                    .addComponent(TxtDescripcion)))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel10))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtTelefono)
                    .addComponent(txtNombre, javax.swing.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                    .addComponent(TxtDireccion)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(77, 77, 77)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(TxtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(ftdFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(TxtDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtIngresarCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(18, 18, 18)
                .addComponent(panelInfoProd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(comboColor, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(comboTalla))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9)
                    .addComponent(CbxPie, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    private void txtNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNombreActionPerformed

    private void txtTelefonoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTelefonoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTelefonoActionPerformed

    private void txtIngresarCodigoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtIngresarCodigoKeyReleased
        String text = txtIngresarCodigo.getText().trim();

        if (text.length() >= 8 && text.matches("\\d+")) {
            DataSearch producto = buscarProductoPorEanOBarcode(text);
            if (producto != null) {
                seleccionarProducto(producto, true);
                // Si se obtuvo una variante específica, preseleccionar talla y color
                if (producto.getIdVariante() > 0) {
                    try {
                        int idProducto = Integer.parseInt(producto.getId_prod());
                        preseleccionarTallaYColorPorVariante(idProducto, producto.getIdVariante());
                    } catch (Exception ex) {
                        // Ignorar errores de parseo o búsqueda
                    }
                }
                return;
            }
        }
        // Cuando no es EAN válido o no encontró producto, mostrar sugerencias
        updateSearchResults();
    }//GEN-LAST:event_txtIngresarCodigoKeyReleased

    private void comboColorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comboColorItemStateChanged
        // Solo procesar cuando el evento es de selección y no estamos en carga inicial
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED && !isLoadingColores) {
            if (selectedProduct != null) {
                String colorSeleccionado = comboColor.getSelectedIndex() > 0
                ? (String) comboColor.getSelectedItem() : null;

                if (colorSeleccionado != null) {
                    // Actualizar datos del producto si ya hay talla seleccionada
                    if (comboTalla.getSelectedIndex() > 0) {
                        actualizarDatosProductoCompleto();
                    }
                }
            }
        }
    }//GEN-LAST:event_comboColorItemStateChanged
 /**
     * Actualiza datos del producto cuando se completa la selección
     */
    private void actualizarDatosProductoCompleto() {
        if (selectedProduct == null) {
            return;
        }

        String tallaSeleccionada = comboTalla.getSelectedIndex() > 0
                ? (String) comboTalla.getSelectedItem() : null;
        String colorSeleccionado = comboColor.getSelectedIndex() > 0
                ? (String) comboColor.getSelectedItem() : null;

        if (tallaSeleccionada != null && colorSeleccionado != null) {
            cargarDatosVarianteEspecifica(
                    Integer.parseInt(selectedProduct.getId_prod()),
                    tallaSeleccionada,
                    colorSeleccionado
            );

            if (ICON.getIcon() == null) {
                cargarImagenProducto();
            }
        }
    }
 /**
     * Sobrecarga para cargar imagen sin parámetros
     */
    private void cargarImagenProducto() {
        if (selectedProduct != null && selectedProduct.getImagen() != null) {
            try {
                cargarImagenProducto(selectedProduct.getImagen());
            } catch (IOException ex) {
                Logger.getLogger(generarVentaFor1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
     /**
     * Carga datos específicos de una variante seleccionada
     */
    private void cargarDatosVarianteEspecifica(int idProducto, String talla, String color) {

        try {
            List<ModelProductVariant> variants = (List<ModelProductVariant>) serviceVariant.getVariantById(idProducto);

            ModelProductVariant varianteEncontrada = null;

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()) {
                    String tallaVariant = construirNombreTalla(variant);
                    String colorVariant = variant.getColorName();

                    if (talla.equals(tallaVariant) && color.equals(colorVariant)) {
                            varianteEncontrada = variant;
                            varianteEncontrada = variant;
                    }
                }
            }

            if (varianteEncontrada != null) {
                BigDecimal precioVenta = BigDecimal.valueOf(
                        varianteEncontrada.getSalePrice() != null
                        ? varianteEncontrada.getSalePrice()
                        : selectedProduct.getPrecioVenta().doubleValue()
                );

                String eanCode = varianteEncontrada.getEan();
                String codigoBarras = varianteEncontrada.getBarcode();
                String codigoMostrar = eanCode != null && !eanCode.isEmpty() ? eanCode
                        : (codigoBarras != null && !codigoBarras.isEmpty() ? codigoBarras : "Sin código");

                txtNombreProducto.setText(selectedProduct.getNombre());
                txtCodigo.setText("Código: " + codigoMostrar);

                int stockCajas = varianteEncontrada.getStockBoxes();
                int stockPares = varianteEncontrada.getStockPairs();
                int totalPares = stockPares + (stockCajas * 24);

                txtStock.setText("Stock: Cajas: " + stockCajas + " - Pares: " + stockPares
                        + " | Total: " + totalPares + " pares");

                selectedProduct.setIdVariante(varianteEncontrada.getVariantId());

                // Actualizar disponibilidad de pie según la variante seleccionada
                try {
                    actualizarDisponibilidadPiePorVariante(varianteEncontrada.getVariantId());
                } catch (SQLException ex) {
                    System.err.println("Error actualizando disponibilidad de pie: " + ex.getMessage());
                }

                if (varianteEncontrada.hasImage()) {
                    try {
                        byte[] imageBytes = varianteEncontrada.getImageBytes();
                        Blob imageBlob = new javax.sql.rowset.serial.SerialBlob(imageBytes);
                        cargarImagenProducto(imageBlob);
                    } catch (Exception e) {
                        System.err.println("Error cargando imagen de variante: " + e.getMessage());
                    }
                }
            } else {
                Toast.show(this, Toast.Type.WARNING,
                        "No se encontró stock disponible para la combinación seleccionada");
                txtStock.setText("Stock: No disponible");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando datos del producto");
        }
    }

    /**
     * Consulta préstamos activos para la variante y actualiza el combo de pie
     * mostrando qué pie está disponible o prestado.
     */
    private void actualizarDisponibilidadPiePorVariante(int idVariante) throws SQLException {
        if (idVariante <= 0) return;

        // Stock actual de la variante (pares)
        int stockPares = 0;
        try {
            ModelProductVariant variante = serviceVariant.getVariantById(idVariante, false);
            if (variante != null) {
                stockPares = variante.getStockPairs();
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo stock de variante: " + e.getMessage());
        }

        PrestamoZapatoService prestamoService = new PrestamoZapatoService();
        Integer bodegaId = SessionManager.getInstance().getCurrentUserBodegaId();
        List<PrestamoZapato> activos = prestamoService.listarPrestamos(PrestamoZapatoDAO.ESTADO_PRESTADO, bodegaId, null, idVariante);

        int prestadosDerecho = 0;
        int prestadosIzquierdo = 0;
        int prestadosAmbos = 0;

        for (PrestamoZapato z : activos) {
            String pie = z.getPie();
            if (pie == null) continue;
            String up = pie.trim().toUpperCase();
            if (up.equals("DERECHO")) prestadosDerecho++;
            else if (up.equals("IZQUIERDO")) prestadosIzquierdo++;
            else if (up.equals("AMBOS")) prestadosAmbos++;
        }

        boolean derechoDisponible = stockPares > (prestadosDerecho + prestadosAmbos);
        boolean izquierdoDisponible = stockPares > (prestadosIzquierdo + prestadosAmbos);
        int paresConsumidosParaCompleto = prestadosAmbos + Math.max(prestadosDerecho, prestadosIzquierdo);
        boolean completoDisponible = stockPares > paresConsumidosParaCompleto;
        boolean nuevoDisponible = stockPares > paresConsumidosParaCompleto; // hay al menos 1 par sin consumir

        javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
        model.addElement("Selecionar Pie");

        // Reglas:
        // - Mostrar dos opciones por pie cuando aplique:
        //   "(Disponible)" para COMPLETAR el pie opuesto si hay más del otro lado.
        //   "(Disponible - Nuevo)" siempre que quede al menos un par sin consumir.
        boolean derechoCompletarDisponible = (prestadosIzquierdo > prestadosDerecho) && derechoDisponible;
        boolean izquierdoCompletarDisponible = (prestadosDerecho > prestadosIzquierdo) && izquierdoDisponible;

        // Pie derecho
        if (derechoCompletarDisponible) {
            model.addElement("Derecho (Disponible)");
        }
        if (nuevoDisponible) {
            model.addElement("Derecho (Disponible - Nuevo)");
        }
        if (!derechoCompletarDisponible && !nuevoDisponible) {
            model.addElement((prestadosDerecho > 0 || prestadosAmbos > 0) ? "Derecho (Prestado)" : "Derecho (No disponible)");
        }

        // Pie izquierdo
        if (izquierdoCompletarDisponible) {
            model.addElement("Izquierdo (Disponible)");
        }
        if (nuevoDisponible) {
            model.addElement("Izquierdo (Disponible - Nuevo)");
        }
        if (!izquierdoCompletarDisponible && !nuevoDisponible) {
            model.addElement((prestadosIzquierdo > 0 || prestadosAmbos > 0) ? "Izquierdo (Prestado)" : "Izquierdo (No disponible)");
        }

        // Pie ambos (par completo)
        if (completoDisponible) {
            model.addElement("Ambos (Disponible)");
        } else if (prestadosAmbos > 0) {
            model.addElement("Ambos (Prestado)");
        } else {
            model.addElement("Ambos (No disponible)");
        }

        if (CbxPie != null) {
            CbxPie.setModel(model);
            CbxPie.setEnabled(true);
            CbxPie.setSelectedIndex(0);
        }

        // Mensajes informativos para el usuario
        if (!completoDisponible && prestadosAmbos > 0) {
            Toast.show(this, Toast.Type.WARNING, "Este par ya está prestado por completo para esta talla y color");
        }
    }

    /**
     * Carga imagen del producto desde Blob
     */
    private void cargarImagenProducto(Blob imgBlob) throws IOException {
        if (imgBlob == null) {
            ICON.setIcon(null);
            ICON.setText("SIN IMAGEN");
        } else {
            try {
                InputStream is = imgBlob.getBinaryStream();
                byte[] imageBytes = is.readAllBytes();

                ImageIcon imageIcon = new ImageIcon(imageBytes);

                int ancho = ICON.getWidth() > 0 ? ICON.getWidth() : 80;
                int alto = ICON.getHeight() > 0 ? ICON.getHeight() : 80;

                Image imagenEscalada = imageIcon.getImage()
                        .getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);

                ICON.setIcon(new ImageIcon(imagenEscalada));
                ICON.setText("");
            } catch (SQLException | IOException e) {
                System.err.println("Error al cargar imagen: " + e.getMessage());
                ICON.setIcon(null);
                ICON.setText("ERROR IMAGEN");
            }
        }
    }

    
        /**
     * Reemplaza seleccionarProducto() existente Ahora hace visible
     * panelInfoProd y carga todos los datos
     */
    private void seleccionarProducto(DataSearch producto) {
        seleccionarProducto(producto, false);
    }

    private void seleccionarProducto(DataSearch producto, boolean esBusquedaPorCodigo) {
        if (producto == null) {
            return;
        }

        System.out.println("Buscar Seleccionando producto: " + producto.getNombre());

        selectedProduct = producto;

        txtIngresarCodigo.setText(producto.getNombre());

        int idProducto = Integer.parseInt(producto.getId_prod());

        // NUEVO: Cargar tallas y colores disponibles
        cargarColoresProducto(idProducto);
        cargarTallasProducto(idProducto);

        // CRÍTICO: Hacer visible el panel de información
        panelInfoProd.setVisible(true);
        System.out.println("SUCCESS  panelInfoProd ahora visible");

        // Cargar información básica del producto
        txtNombreProducto.setText(producto.getNombre());

        int stockTotalPares = producto.getStockPorPares()
                + (producto.getStockPorCajas() * 24);
        txtStock.setText("Stock: Cajas: " + producto.getStockPorCajas()
                + " - Pares: " + producto.getStockPorPares()
                + " | Total: " + stockTotalPares + " pares");

        // Cargar código (priorizar EAN sobre código de barras)
        String codigoMostrar = "Sin código";
        if (producto.getIdVariante() > 0) {
            String ean = obtenerEanVarianteConValidacion(producto.getIdVariante());
            if (ean != null && !ean.isEmpty()) {
                codigoMostrar = ean;
            }
        }
        if (codigoMostrar.equals("Sin código")
                && producto.getEAN()!= null
                && !producto.getEAN().isEmpty()) {
            codigoMostrar = producto.getEAN();
        }
        txtCodigo.setText("Código: " + codigoMostrar);

        // Cargar imagen del producto
        if (producto.getImagen() != null) {
            try {
                cargarImagenProducto(producto.getImagen());
            } catch (IOException ex) {
                System.err.println("Error cargando imagen: " + ex.getMessage());
                ICON.setIcon(null);
                ICON.setText("SIN IMAGEN");
            }
        } else {
            ICON.setIcon(null);
            ICON.setText("SIN IMAGEN");
        }


        // Si la selección vino de un código y tenemos variante, preseleccionar combos
        if (esBusquedaPorCodigo && producto.getIdVariante() > 0) {
            try {
                int idProductoSel = Integer.parseInt(producto.getId_prod());
                preseleccionarTallaYColorPorVariante(idProductoSel, producto.getIdVariante());
            } catch (Exception ex) {
                // Ignorar cualquier fallo al preseleccionar
            }
        }

        System.out.println("SUCCESS  Producto seleccionado y panel actualizado");
    }
private String obtenerEanVarianteConValidacion(int idVariante) {
        String sql = "SELECT ean, codigo_barras FROM producto_variantes WHERE id_variante = ? AND disponible = 1";

        try (Connection conn = conexion.getInstance().createConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVariante);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String ean = rs.getString("ean");
                    String codigoBarras = rs.getString("codigo_barras");

                    // CRÍTICO: Siempre priorizar EAN
                    if (ean != null && !ean.trim().isEmpty()) {
                        System.out.println("SUCCESS  EAN encontrado para variante " + idVariante + ": " + ean);
                        return ean;
                    } else if (codigoBarras != null && !codigoBarras.trim().isEmpty()) {
                        System.out.println("WARNING  EAN vacío, usando código de barras para variante " + idVariante + ": " + codigoBarras);
                        return codigoBarras;
                    } else {
                        System.err.println("ERROR  Ni EAN ni código de barras disponibles para variante " + idVariante);
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR  Error obteniendo EAN de variante " + idVariante + ": " + e.getMessage());
        }

        return null;
    }
  private void cargarColoresPorTalla(int idProducto, String tallaSeleccionada) {
        isLoadingColores = true; // Activar flag ANTES de modificar

        try {
            List<ModelProductVariant> variants = (List<ModelProductVariant>) serviceVariant.getVariantById(idProducto);

            // Guardar selección actual para intentar preservarla
            String colorActualmenteSeleccionado = null;
            if (comboColor.getSelectedIndex() > 0) {
                colorActualmenteSeleccionado = (String) comboColor.getSelectedItem();
            }

            comboColor.removeAllItems();
            comboColor.addItem("Color");

            Set<String> coloresUnicos = new LinkedHashSet<>();

            // Filtrar colores disponibles para la talla seleccionada
            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()
                        && (variant.getStockPairs() > 0 || variant.getStockBoxes() > 0)) {

                    String tallaVariant = construirNombreTalla(variant);

                    if (tallaSeleccionada.equals(tallaVariant)) {
                        String color = variant.getColorName();
                        if (color != null && !color.trim().isEmpty()) {
                            coloresUnicos.add(color);
                        }
                    }
                }
            }

            // Agregar colores al combo
            int indexARestaurar = -1;
            int currentIndex = 1;
            for (String color : coloresUnicos) {
                comboColor.addItem(color);

                // Verificar si este es el color que estaba seleccionado
                if (color.equals(colorActualmenteSeleccionado)) {
                    indexARestaurar = currentIndex;
                }
                currentIndex++;
            }

            // CLAVE: Restaurar selección si el color aún está disponible
            if (indexARestaurar > 0) {
                comboColor.setSelectedIndex(indexARestaurar);
                System.out.println("SUCCESS  Color restaurado: " + colorActualmenteSeleccionado);
            }

            System.out.println("Tema Colores cargados para talla '" + tallaSeleccionada
                    + "': " + coloresUnicos.size());

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando colores");
        } finally {
            isLoadingColores = false; // Desactivar flag SIEMPRE
        }
    }

    private void comboColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboColorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboColorActionPerformed

    private void comboTallaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comboTallaItemStateChanged
        // Solo procesar cuando el evento es de selección y no estamos en carga inicial
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED && !isLoadingTallas) {
            if (selectedProduct != null) {
                String tallaSeleccionada = comboTalla.getSelectedIndex() > 0
                ? (String) comboTalla.getSelectedItem() : null;

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
    }//GEN-LAST:event_comboTallaItemStateChanged

    private void TxtDireccionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TxtDireccionActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TxtDireccionActionPerformed
    public void init() {
        txtNombre.grabFocus();
    }

    // Cargar datos existentes de un préstamo para edición
    public void cargarDesdePrestamo(PrestamoZapato p) {
        if (p == null) return;
        try {
            // Campos del prestatario
            if (p.getNombrePrestatario() != null) txtNombre.setText(p.getNombrePrestatario());
            if (p.getCelularPrestatario() != null) txtTelefono.setText(p.getCelularPrestatario());
            if (TxtDireccion != null && p.getDireccionPrestatario() != null) {
                TxtDireccion.setText(p.getDireccionPrestatario());
            }
            TxtDescripcion.setText(p.getObservaciones() != null ? p.getObservaciones() : "");

            // Fecha de devolución mostrada (si existe)
            if (p.getFechaDevolucion() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                ftdFecha.setText(sdf.format(p.getFechaDevolucion()));
            }

            // Construir DataSearch mínimo para seleccionar el producto
            String nombreProd = p.getNombreProducto() != null ? p.getNombreProducto() : "Producto";
            String color = p.getColor();
            String talla = p.getTalla();
            DataSearch ds = new DataSearch(
                    String.valueOf(p.getIdProducto()),
                    null,
                    nombreProd,
                    color,
                    talla,
                    false,
                    null,
                    0,
                    0,
                    0,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    0,
                    0,
                    null,
                    true,
                    null,
                    null,
                    0,
                    0,
                    0,
                    p.getIdVariante() != null ? p.getIdVariante() : 0
            );

            // Si tenemos id de variante, intentar cargar imagen específica
            if (p.getIdVariante() != null && p.getIdVariante() > 0) {
                Blob imgVar = obtenerImagenVariantePorId(p.getIdVariante());
                if (imgVar != null) {
                    ds.setImagen(imgVar);
                }
            }

            // Seleccionar producto y preseleccionar talla/color por variante
            seleccionarProducto(ds);
            try {
                if (p.getIdVariante() != null && p.getIdVariante() > 0) {
                    preseleccionarTallaYColorPorVariante(p.getIdProducto(), p.getIdVariante());
                    // Actualizar disponibilidad del pie para la variante y seleccionar el pie del préstamo
                    actualizarDisponibilidadPiePorVariante(p.getIdVariante());
                    String pie = p.getPie() != null ? p.getPie().trim().toUpperCase() : "";
                    String etiqueta = pie.equals("DERECHO") ? "derecho"
                            : pie.equals("IZQUIERDO") ? "izquierdo"
                            : "completo"; // AMBOS
                    for (int i = 0; i < CbxPie.getItemCount(); i++) {
                        String item = String.valueOf(CbxPie.getItemAt(i)).toLowerCase();
                        if (item.contains(etiqueta)) {
                            CbxPie.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("No se pudo preseleccionar talla/color/pie: " + ex.getMessage());
            }

            // Visibilizar panel de información del producto
            panelInfoProd.setVisible(true);
        } catch (Exception e) {
            System.err.println("Error cargando préstamo en formulario: " + e.getMessage());
        }
    }
    /**
     * Preselecciona talla y color en los combos a partir del id de variante.
     */
    private void preseleccionarTallaYColorPorVariante(int idProducto, int idVariante) throws SQLException {
        List<ModelProductVariant> variants = (List<ModelProductVariant>) serviceVariant.getVariantById(idProducto);
        ModelProductVariant target = null;
        for (ModelProductVariant v : variants) {
            if (v.getVariantId() == idVariante) {
                target = v;
                break;
            }
        }
        if (target == null) {
            return;
        }

        String tallaNombre = construirNombreTalla(target);
        String colorNombre = target.getColorName();

        isLoadingTallas = true;
        for (int i = 0; i < comboTalla.getItemCount(); i++) {
            Object item = comboTalla.getItemAt(i);
            if (tallaNombre != null && tallaNombre.equals(item)) {
                comboTalla.setSelectedIndex(i);
                break;
            }
        }
        isLoadingTallas = false;

        isLoadingColores = true;
        for (int i = 0; i < comboColor.getItemCount(); i++) {
            Object item = comboColor.getItemAt(i);
            if (colorNombre != null && colorNombre.equals(item)) {
                comboColor.setSelectedIndex(i);
                break;
            }
        }
        isLoadingColores = false;

        actualizarDatosProductoCompleto();

        // Tras fijar la variante, refrescar disponibilidad del pie
        // getIdVariante() devuelve int; no es válido comparar con null
        if (selectedProduct != null && selectedProduct.getIdVariante() > 0) {
            try {
                actualizarDisponibilidadPiePorVariante(selectedProduct.getIdVariante());
            } catch (SQLException ex) {
                System.err.println("Error actualizando disponibilidad de pie: " + ex.getMessage());
            }
        }

        // Cargar imagen de la variante seleccionada usando consulta con imagen
        try {
            ModelProductVariant varianteConImagen = serviceVariant.getVariantById(idVariante);
            if (varianteConImagen != null && varianteConImagen.hasImage()) {
                byte[] imageBytes = varianteConImagen.getImageBytes();
                if (imageBytes != null && imageBytes.length > 0) {
                    Blob imageBlob = new javax.sql.rowset.serial.SerialBlob(imageBytes);
                    cargarImagenProducto(imageBlob);
                }
            }
        } catch (Exception ex) {
            System.err.println("No se pudo cargar imagen de la variante: " + ex.getMessage());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JComboBox<String> CbxPie;
    private javax.swing.JLabel ICON;
    private javax.swing.JTextField TxtDescripcion;
    public javax.swing.JTextField TxtDireccion;
    private javax.swing.JComboBox<String> comboColor;
    private javax.swing.JComboBox<String> comboTalla;
    private raven.datetime.component.date.DatePicker datePicker;
    private javax.swing.JFormattedTextField ftdFecha;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JPanel panelInfoProd;
    private javax.swing.JLabel txtCodigo;
    private javax.swing.JTextField txtIngresarCodigo;
    public javax.swing.JTextField txtNombre;
    private javax.swing.JLabel txtNombreProducto;
    private javax.swing.JLabel txtStock;
    public javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables
}

