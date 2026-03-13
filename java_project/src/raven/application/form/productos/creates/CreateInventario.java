package raven.application.form.productos.creates;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.inventario.CreateInventarioController;
import net.miginfocom.swing.MigLayout;
import raven.application.form.productos.ComboItem;
import net.miginfocom.swing.MigLayout;
import raven.application.form.productos.ComboItem;
import raven.application.form.productos.ProductoCardRenderer;
import javax.swing.table.DefaultTableCellRenderer;

public class CreateInventario extends javax.swing.JPanel {

        public javax.swing.JComboBox<ComboItem> cbxMarca;
        public javax.swing.JComboBox<ComboItem> cbxCategoria;
        public javax.swing.JCheckBox chkMostrarStock0;
        public javax.swing.JCheckBox chkMostrarNegativos;

        public CreateInventarioController controller; // Cambiado de private a public
        private static final String Camposdetexto = "arc:15;background:lighten($Menu.background,25%)";
        private Set<String> selectedProductCodes = new HashSet<>();
        private raven.dao.InventarioBodegaDAO bodegaDAO = new raven.dao.InventarioBodegaDAO(); // For loading bodegas

        public CreateInventario(boolean con) {
                initComponents();

                controller = new CreateInventarioController(this, con);

                init();
        }

        // En CreateInventario.java - modificar el método guardarConteo
        public void guardarConteo() {
                // Solo cerrar la ventana si crearConteo() devuelve true
                if (crearConteo()) {
                        // Solo cerrar la ventana si la validación fue exitosa
                        java.awt.Window ventana = javax.swing.SwingUtilities.getWindowAncestor(this);
                        if (ventana != null) {
                                ventana.dispose();
                        }
                }
                // Si la validación falló, no hacer nada y mantener el modal abierto
        }

        public void init() {
                // Seleccionar por defecto el conteo de cajas
                txtnombredelconteo.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);
                cbxtipodeinventario.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);
                txtFechaIn.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);
                cbxResponsable.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);
                jTextArea2.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");
                datePicker.setEditor(txtFechaIn);
                datePicker.setCloseAfterSelected(true);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                txtFechaIn.setText(sdf.format(new Date()));
                // Configurar botones de tipo de conteo
                // jPanel5.putClientProperty(FlatClientProperties.STYLE,
                // "background:lighten($Menu.background,25%)");
                // Configurar tabla de productos
                configurarTablaProductos();
                // Configurar campo de búsqueda
                configurarPlaceHolders();
                configurarEstiloTabla();
                cargarBodegas();
        }

        private void cargarBodegas() {
                cbxBodega.removeAllItems();
                // Load bodegas using a helper class or just strings for now, Controller will
                // handle better
                // But to init UI properly:
                try {
                        java.util.List<raven.application.form.productos.ComboItem> bodegas = bodegaDAO
                                        .obtenerBodegasCombo();
                        for (raven.application.form.productos.ComboItem item : bodegas) {
                                cbxBodega.addItem(item);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private void configurarEstiloTabla() {
                // Estilo general de la tabla
                tablaProd.putClientProperty(FlatClientProperties.STYLE,
                                "showHorizontalLines:true;"
                                                + "showVerticalLines:false;"
                                                + "rowHeight:90;" // Aumentado a 90 para mostrar tarjeta de producto
                                                + "intercellSpacing:0,1;"
                                                + "selectionBackground:$Accent.yellow;"
                                                + "selectionForeground:$Table.foreground;");

                // Estilo del encabezado de la tabla
                tablaProd.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                                "hoverBackground:$Table.background;"
                                                + "height:50;" // Aumentado altura header
                                                + "separatorColor:$TableHeader.background;"
                                                + "font:bold +2 $h4.font");
        }

        public void configurarPlaceHolders() {
                Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
                FontIcon boxIcon = createColoredIcon(FontAwesomeSolid.BOX, tabTextColor);
                FontIcon shoesIcon = createColoredIcon(FontAwesomeSolid.SHOE_PRINTS, tabTextColor);
                btnconteoCajas.setIcon(boxIcon);
                btnConteoPares.setIcon(shoesIcon);
                txtnombredelconteo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                                "ingresar nombre para el conteo");
                jTextFieldBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                                "Buscar por nombre, código, marca...");
                // Add search icon
                FontIcon searchIcon = FontIcon.of(FontAwesomeSolid.SEARCH, 16, Color.GRAY);
                jTextFieldBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, searchIcon);
        }

        private FontIcon createColoredIcon(Ikon icon, Color color) {
                FontIcon fontIcon = FontIcon.of(icon);
                fontIcon.setIconSize(18); // Tamaño del icono
                fontIcon.setIconColor(color);
                return fontIcon;
        }

        private void configurarTablaProductos() {
                // Asegurar que la columna de selección sea de tipo checkbox
                tablaProd.getColumnModel().getColumn(0).setCellEditor(
                                new javax.swing.DefaultCellEditor(new JCheckBox()));
                tablaProd.getColumnModel().getColumn(0).setCellRenderer(
                                new javax.swing.table.DefaultTableCellRenderer() {
                                        @Override
                                        public java.awt.Component getTableCellRendererComponent(JTable table,
                                                        Object value,
                                                        boolean isSelected, boolean hasFocus, int row, int column) {
                                                JCheckBox check = new JCheckBox();
                                                check.setSelected((boolean) value);
                                                check.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                                                check.setBackground(isSelected ? table.getSelectionBackground()
                                                                : table.getBackground());
                                                return check;
                                        }
                                });

                // Configurar Renderers
                tablaProd.getColumnModel().getColumn(2).setCellRenderer(new ProductoCardRenderer()); // Columna Producto

                DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                tablaProd.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Codigo
                tablaProd.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Stock

                // Ajustar anchos de columnas
                tablaProd.getColumnModel().getColumn(0).setPreferredWidth(50); // Select
                tablaProd.getColumnModel().getColumn(1).setPreferredWidth(100); // Codigo
                tablaProd.getColumnModel().getColumn(2).setPreferredWidth(350); // Producto (Card)
                tablaProd.getColumnModel().getColumn(3).setPreferredWidth(100); // Categoria
                tablaProd.getColumnModel().getColumn(4).setPreferredWidth(100); // Marca
                tablaProd.getColumnModel().getColumn(5).setPreferredWidth(80); // Stock

                attachImageHoverListener(tablaProd);
        }

        private void attachImageHoverListener(JTable table) {
                final javax.swing.JWindow preview = new javax.swing.JWindow();
                final javax.swing.JLabel label = new javax.swing.JLabel();
                label.setOpaque(true);
                label.setBackground(new java.awt.Color(250, 250, 250));
                label.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(60, 60, 60), 1, true),
                                javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6)));
                preview.getContentPane().add(label);
                preview.pack();

                final int imageColumn = 2; // "Producto" column
                final javax.swing.Timer showTimer = new javax.swing.Timer(150, null);
                showTimer.setRepeats(false);
                final int[] hoverProductId = new int[] { 0 };
                preview.setAlwaysOnTop(true);

                table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                        @Override
                        public void mouseMoved(java.awt.event.MouseEvent e) {
                                int row = table.rowAtPoint(e.getPoint());
                                int col = table.columnAtPoint(e.getPoint());

                                if (row < 0 || col != imageColumn) {
                                        showTimer.stop();
                                        preview.setVisible(false);
                                        return;
                                }

                                try {
                                        int modelRow = table.convertRowIndexToModel(row);
                                        Object val = table.getModel().getValueAt(modelRow, imageColumn);

                                        if (!(val instanceof raven.controlador.productos.ModelProduct)) {
                                                showTimer.stop();
                                                preview.setVisible(false);
                                                return;
                                        }

                                        raven.controlador.productos.ModelProduct p = (raven.controlador.productos.ModelProduct) val;
                                        if (p.getCachedIcon() == null) {
                                                showTimer.stop();
                                                preview.setVisible(false);
                                                return;
                                        }

                                        java.awt.Rectangle cellRect = table.getCellRect(row, imageColumn, true);
                                        int relX = e.getPoint().x - cellRect.x;
                                        if (relX > 70) { // Icon width is fixed at 70 in renderer
                                                showTimer.stop();
                                                preview.setVisible(false);
                                                return;
                                        }

                                        if (hoverProductId[0] != p.getProductId()) {
                                                hoverProductId[0] = p.getProductId();
                                                for (java.awt.event.ActionListener l : showTimer.getActionListeners())
                                                        showTimer.removeActionListener(l);

                                                showTimer.addActionListener(ev -> {
                                                        new Thread(() -> {
                                                                javax.swing.ImageIcon loaded = raven.utils.ProductImageOptimizer
                                                                                .loadLargeImage(p.getProductId(), 500);
                                                                if (loaded != null) {
                                                                        javax.swing.SwingUtilities.invokeLater(() -> {
                                                                                label.setIcon(loaded);
                                                                                preview.pack();
                                                                                preview.setLocationRelativeTo(null);
                                                                                preview.setVisible(true);
                                                                        });
                                                                }
                                                        }).start();
                                                });
                                                showTimer.restart();
                                        } else {
                                                if (!preview.isVisible()) {
                                                        preview.setLocationRelativeTo(null);
                                                        preview.setVisible(true);
                                                }
                                        }

                                } catch (Exception ignore) {
                                        preview.setVisible(false);
                                }
                        }
                });

                table.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseExited(java.awt.event.MouseEvent e) {
                                showTimer.stop();
                                preview.setVisible(false);
                        }
                });
        }

        public void selectCajasButton() {
                btnconteoCajas.setSelected(true);
                btnConteoPares.setSelected(false);
                btnconteoCajas.putClientProperty("FlatLaf.style", "background:#0A84FF;foreground:#fff;");
                btnConteoPares.putClientProperty("FlatLaf.style", "background:#f5f5f5;foreground:#333;");
        }

        public void selectParesButton() {
                btnConteoPares.setSelected(true);
                btnconteoCajas.setSelected(false);
                btnConteoPares.putClientProperty("FlatLaf.style", "background:#0A84FF;foreground:#fff;");
                btnconteoCajas.putClientProperty("FlatLaf.style", "background:#f5f5f5;foreground:#333;");

        }

        public boolean crearConteo() {
                return controller.crearConteoInventario();
        }
        // Métodos getter para los componentes

        public JTextField getNombreTextField() {
                return txtnombredelconteo;
        }

        public JComboBox<String> getTipoInventarioCombo() {
                return cbxtipodeinventario;
        }

        public JFormattedTextField getFechaTextField() {
                return txtFechaIn;
        }

        public JComboBox getResponsableCombo() {
                return cbxResponsable;
        }

        public JTextArea getObservacionesTextArea() {
                return jTextArea2;
        }

        public JComboBox getBodegaCombo() {
                return cbxBodega;
        }

        public javax.swing.JComboBox<ComboItem> getMarcaCombo() {
                return cbxMarca;
        }

        public javax.swing.JComboBox<ComboItem> getCategoriaCombo() {
                return cbxCategoria;
        }

        public javax.swing.JComboBox<String> getEstanteriaCombo() {
                return cbxEstanteria;
        }

        public javax.swing.JCheckBox getCheckStock0() {
                return chkMostrarStock0;
        }

        public javax.swing.JCheckBox getCheckNegativos() {
                return chkMostrarNegativos;
        }

        public JTable getTablaProductos() {
                return tablaProd;
        }

        public JCheckBox getCheckboxSeleccionarTodos() {
                return jCheckBox1;
        }

        public javax.swing.JButton getTipoCajas() {
                return btnconteoCajas;
        }

        public javax.swing.JButton getTipoPares() {
                return btnConteoPares;
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
                jTextArea1 = new javax.swing.JTextArea();
                jScrollPane1 = new javax.swing.JScrollPane();
                jPanel1 = new javax.swing.JPanel();
                botonera = new javax.swing.JPanel();
                btnConteoPares = new raven.componentes.icon.JIconButton();
                btnconteoCajas = new raven.componentes.icon.JIconButton();
                jPanel3 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                txtnombredelconteo = new javax.swing.JTextField();
                jLabel3 = new javax.swing.JLabel();
                cbxtipodeinventario = new javax.swing.JComboBox<>();
                jLabel2 = new javax.swing.JLabel();
                txtFechaIn = new javax.swing.JFormattedTextField();
                jLabel4 = new javax.swing.JLabel();
                cbxResponsable = new javax.swing.JComboBox<>();
                jLabel5 = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                jTextArea2 = new javax.swing.JTextArea();
                lblBodega = new javax.swing.JLabel();
                cbxBodega = new javax.swing.JComboBox<>();
                lblEstanteria = new javax.swing.JLabel();
                cbxEstanteria = new javax.swing.JComboBox<>();

                jPanel4 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                jCheckBox1 = new javax.swing.JCheckBox();
                jTextFieldBuscar = new javax.swing.JTextField();
                jScrollPane5 = new javax.swing.JScrollPane();
                tablaProd = new javax.swing.JTable();

                jTextArea1.setColumns(20);
                jTextArea1.setRows(5);

                btnConteoPares.setText("Conteo de pares");
                btnConteoPares.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnConteoParesActionPerformed(evt);
                        }
                });

                btnconteoCajas.setText("Conteo de cajas");
                btnconteoCajas.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnconteoCajasActionPerformed(evt);
                        }
                });

                javax.swing.GroupLayout botoneraLayout = new javax.swing.GroupLayout(botonera);
                botonera.setLayout(botoneraLayout);
                botoneraLayout.setHorizontalGroup(
                                botoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, botoneraLayout
                                                                .createSequentialGroup()
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(btnconteoCajas,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                230,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(btnConteoPares,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                209,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap()));
                botoneraLayout.setVerticalGroup(
                                botoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, botoneraLayout
                                                                .createSequentialGroup()
                                                                .addGap(5, 5, 5)
                                                                .addGroup(botoneraLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(btnConteoPares,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(btnconteoCajas,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));

                jLabel1.setText("Tipo de inventario");

                txtnombredelconteo.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                txtnombredelconteoActionPerformed(evt);
                        }
                });

                jLabel3.setText("Nombre del conteo");

                cbxtipodeinventario.setModel(
                                new javax.swing.DefaultComboBoxModel<>(
                                                new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
                cbxtipodeinventario.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                cbxtipodeinventarioActionPerformed(evt);
                        }
                });

                jLabel2.setText("Responsable");

                txtFechaIn.setSelectionColor(new java.awt.Color(255, 255, 255));
                txtFechaIn.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                txtFechaInActionPerformed(evt);
                        }
                });

                jLabel4.setText("Fecha programada");

                cbxResponsable.setModel(
                                new javax.swing.DefaultComboBoxModel<>(
                                                new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                lblBodega.setText("Bodega");
                lblEstanteria.setText("Estantería / Ubicación");

                jLabel5.setText("Observaciónes");

                jTextArea2.setColumns(20);
                jTextArea2.setRows(5);
                jTextArea2.setDragEnabled(true);
                jScrollPane2.setViewportView(jTextArea2);

                javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
                jPanel3.setLayout(jPanel3Layout);
                jPanel3Layout.setHorizontalGroup(
                                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel3Layout.createSequentialGroup()
                                                                .addGap(40, 40, 40)
                                                                .addGroup(jPanel3Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(jLabel3)
                                                                                .addComponent(jLabel5,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                79,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel4)
                                                                                .addComponent(jLabel1)
                                                                                .addComponent(jLabel2,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                79,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(txtFechaIn)
                                                                                .addComponent(cbxtipodeinventario, 0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(txtnombredelconteo)
                                                                                .addComponent(cbxResponsable, 0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jScrollPane2,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                304,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(lblBodega)
                                                                                .addComponent(cbxBodega, 0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(lblEstanteria)
                                                                                .addComponent(cbxEstanteria))
                                                                .addGap(40, 40, 40)));
                jPanel3Layout.setVerticalGroup(
                                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel3Layout.createSequentialGroup()
                                                                .addGap(9, 9, 9)
                                                                .addComponent(jLabel3,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                29,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(txtnombredelconteo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                41,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(jLabel1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                29,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(cbxtipodeinventario,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(jLabel4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                28,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtFechaIn,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel2,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                28,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxResponsable,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel5,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                28,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jScrollPane2,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                70,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblBodega,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                28,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxBodega,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblEstanteria,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                28,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxEstanteria,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel6.setText("Seleccionar productos");

                jCheckBox1.setText("Seleccionar todos");
                jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox1ActionPerformed(evt);
                        }
                });

                jTextFieldBuscar.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                jTextFieldBuscarKeyReleased(evt);
                        }
                });

                jPanel4.setLayout(new MigLayout("fill, insets 10", "[grow][grow][grow]10[]", "[]10[]"));
                jPanel4.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:lighten($Menu.background,5%)");

                cbxMarca = new javax.swing.JComboBox<>();
                cbxCategoria = new javax.swing.JComboBox<>();
                chkMostrarStock0 = new javax.swing.JCheckBox("Mostrar Stock 0");
                chkMostrarNegativos = new javax.swing.JCheckBox("Mostrar Negativos");

                // Initialize default selection
                chkMostrarStock0.setSelected(true);
                chkMostrarNegativos.setSelected(true);

                // Initialize placeholder/style
                // Note: Data loading will be done by controller

                jPanel4.add(new javax.swing.JLabel("Marca:"), "split 2, span 1");
                jPanel4.add(cbxMarca, "growx");

                jPanel4.add(new javax.swing.JLabel("Categoría:"), "split 2, span 1");
                jPanel4.add(cbxCategoria, "growx");

                jPanel4.add(jCheckBox1, "wrap"); // Select all button

                jPanel4.add(jTextFieldBuscar, "span 3, growx");
                jPanel4.add(new javax.swing.JLabel(""), "growx"); // spacer

                jPanel4.add(chkMostrarStock0, "span 2");
                jPanel4.add(chkMostrarNegativos, "wrap");

                tablaProd.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {

                                },
                                new String[] {
                                                "Seleccionar", "Codigo", "Producto", "Categoria", "Marca", "Sock actual"
                                }) {
                        Class[] types = new Class[] {
                                        java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class,
                                        java.lang.Object.class,
                                        java.lang.Object.class, java.lang.Object.class
                        };
                        boolean[] canEdit = new boolean[] {
                                        true, false, false, false, false, false
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types[columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit[columnIndex];
                        }
                });
                jScrollPane5.setViewportView(tablaProd);
                if (tablaProd.getColumnModel().getColumnCount() > 0) {
                        tablaProd.getColumnModel().getColumn(0).setPreferredWidth(40);
                        tablaProd.getColumnModel().getColumn(1).setPreferredWidth(70);
                        tablaProd.getColumnModel().getColumn(2).setPreferredWidth(120);
                        tablaProd.getColumnModel().getColumn(3).setPreferredWidth(60);
                        tablaProd.getColumnModel().getColumn(4).setPreferredWidth(60);
                        tablaProd.getColumnModel().getColumn(5).setPreferredWidth(40);
                }

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(jPanel1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(botonera,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGroup(jPanel1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jPanel3,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addGroup(jPanel1Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(jScrollPane5)
                                                                                                                .addComponent(jPanel4,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))))
                                                                .addContainerGap(29, Short.MAX_VALUE)));
                jPanel1Layout.setVerticalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(botonera,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(jPanel1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addGroup(jPanel1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jPanel4,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(14, 14, 14)
                                                                                                .addComponent(jScrollPane5))
                                                                                .addComponent(jPanel3,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap(26, Short.MAX_VALUE)));

                jScrollPane1.setViewportView(jPanel1);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jScrollPane1));
        }// </editor-fold>//GEN-END:initComponents

        private void btnConteoParesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnConteoParesActionPerformed
                selectParesButton();
        }// GEN-LAST:event_btnConteoParesActionPerformed

        private void btnconteoCajasActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnconteoCajasActionPerformed
                selectCajasButton(); // TODO add your handling code here:
        }// GEN-LAST:event_btnconteoCajasActionPerformed

        private void txtnombredelconteoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtnombredelconteoActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_txtnombredelconteoActionPerformed

        private void txtFechaInActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtFechaInActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_txtFechaInActionPerformed

        private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jCheckBox1ActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_jCheckBox1ActionPerformed

        private void cbxtipodeinventarioActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxtipodeinventarioActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_cbxtipodeinventarioActionPerformed

        private void jTextFieldBuscarKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_jTextFieldBuscarKeyReleased
                String termino = jTextFieldBuscar.getText().trim();
                if (!termino.isEmpty()) {
                        controller.buscarProductos(termino);
                } else {
                        // Si el campo está vacío, cargar todos los productos
                        controller.buscarProductos("");
                } // TODO add your handling code here:
        }// GEN-LAST:event_jTextFieldBuscarKeyReleased

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel botonera;
        public static raven.componentes.icon.JIconButton btnConteoPares;
        protected static raven.componentes.icon.JIconButton btnconteoCajas;
        private javax.swing.JComboBox<String> cbxResponsable;
        private javax.swing.JComboBox<String> cbxtipodeinventario;
        private raven.datetime.component.date.DatePicker datePicker;
        private javax.swing.JCheckBox jCheckBox1;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane5;
        private javax.swing.JTextArea jTextArea1;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextField jTextFieldBuscar;
        private javax.swing.JTable tablaProd;
        private javax.swing.JFormattedTextField txtFechaIn;
        private javax.swing.JTextField txtnombredelconteo;
        private javax.swing.JLabel lblBodega;
        private javax.swing.JComboBox<raven.application.form.productos.ComboItem> cbxBodega;
        private javax.swing.JLabel lblEstanteria;
        private javax.swing.JComboBox<String> cbxEstanteria;
        // End of variables declaration//GEN-END:variables
}
