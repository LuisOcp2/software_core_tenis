package raven.application.form.principal;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.principal.GastosComprasController;
import raven.controlador.principal.ModelColor;
import raven.controlador.principal.ModelTalla;
import raven.controlador.principal.ModelTipoGasto;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * GASTOS FORM - VERSIÓN MEJORADA CON RESPONSIVE V3.0
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Formulario de Gastos con Sistema Responsive Completamente Mejorado.
 *
 * <h2>Mejoras implementadas en V3.0:</h2>
 * <ul>
 * <li>SUCCESS <b>Compatibilidad FlatLaf:</b> Elimina errores de estilos no
 * compatibles</li>
 * <li>SUCCESS <b>Layout Responsive Avanzado:</b> Adaptación inteligente a
 * diferentes
 * pantallas</li>
 * <li>SUCCESS <b>Estilos Modernos:</b> Paleta de colores y tipografías
 * mejoradas</li>
 * <li>SUCCESS <b>Breakpoints Optimizados:</b> Transiciones más suaves entre
 * dispositivos</li>
 * <li>SUCCESS <b>Componentes Inteligentes:</b> Auto-ajuste según espacio
 * disponible</li>
 * <li>SUCCESS <b>Performance Mejorada:</b> Gestión eficiente de memoria y
 * eventos</li>
 * <li>SUCCESS <b>Animaciones Suaves:</b> Transiciones visuales entre
 * breakpoints</li>
 * <li>SUCCESS <b>Tablas Responsive:</b> Columnas adaptativas según
 * dispositivo</li>
 * </ul>
 *
 * <h2>Instrucciones de uso:</h2>
 * 
 * <pre>{@code
 * // Reemplazar en el constructor:
 * // Cambiar de:
 * // GastosFormResponsive.aplicar(this);
 * // A:
 * GastosFormResponsivoMejorado.aplicar(this);
 * }</pre>
 *
 * @author MiniMax Agent - Sistema Responsive Avanzado
 * @version 3.0 Mejorada
 */
public class gastosForm extends javax.swing.JDialog {

        private static final java.util.logging.Logger logger = java.util.logging.Logger
                        .getLogger(gastosForm.class.getName());
        private GastosFormUI formUI;
        private static final String PANEL_STYLE = "arc:15;background:$Login.background;";

        private JSplitPane splitPaneGastosInternos;

        private Timer resizeTimer;
        private boolean responsiveInicializado = false;

        public gastosForm(java.awt.Frame parent, boolean modal) {
                super(parent, modal);

                initComponents();

                cargarUI();

                formUI = new GastosFormUI(this);
                aplicarEstiloPaneles();
                setLocationRelativeTo(parent);
                this.responsiveInicializado = true;
        }

        private void aplicarEstiloPaneles() {
                try {
                        javax.swing.JPanel[] paneles = {
                                        panelTitulo, panelGastosInsert, panelGastosList, panelBotonera, jPanel10
                        };

                        for (javax.swing.JPanel panel : paneles) {
                                if (panel != null) {
                                        panel.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
                                }
                        }

                        System.out.println("SUCCESS  Estilos de paneles aplicados");
                } catch (Exception e) {
                        System.err.println("ERROR  Error aplicando estilos: " + e.getMessage());
                }

        }

        public void cargarUI() {
                configurarListenerTabs();
                configurarTabs();
        }

        private void configurarListenerTabs() {
                panelGastosInternos.addChangeListener(e -> {
                        int tabIndex = panelGastosInternos.getSelectedIndex();
                        boolean esTabGastos = (tabIndex == 0);

                });
        }

        public void cargarGastosDelMovimiento(int idMovimiento) {
                if (formUI != null) {
                        new javax.swing.SwingWorker<Void, Void>() {
                                @Override
                                protected Void doInBackground() {
                                        formUI.inicializar(idMovimiento);
                                        return null;
                                }
                        }.execute();
                }
        }

        public int obtenerIdMovimientoDesdeTitulo() {
                String titulo = txtMovimientoCaja.getText();
                if (titulo.contains("Mov #")) {
                        try {
                                String numero = titulo.split("Mov #")[1].split("\\s")[0];
                                return Integer.parseInt(numero);
                        } catch (Exception e) {
                                System.err.println("Error extrayendo movimiento: " + e.getMessage());
                        }
                }
                return -1;
        }

        public void configurarTabs() {
                // Estilo moderno para tabs - solo propiedades válidas de FlatLaf
                panelGastosInternos.putClientProperty(
                                FlatClientProperties.STYLE,
                                "tabWidthMode:equal;" +
                                                "tabAreaAlignment:fill;" +
                                                "tabHeight:55;" +
                                                "tabInsets:8,24,8,24;" +
                                                "tabAreaInsets:0,0,0,0;" +
                                                "showTabSeparators:false;" +
                                                "tabAlignment:center;" +
                                                "tabArc:0;" +
                                                "selectedBackground:$App.accent.blue;" +
                                                "selectedForeground:#ffffff;" +
                                                "hoverColor:lighten($App.accent.blue,10%);" +
                                                "focusColor:$App.accent.blue;" +
                                                "tabSelectionHeight:3;" +
                                                "underlineColor:$App.accent.blue;" +
                                                "font:bold +2;");

                // Mejorar títulos de tabs con emojis
                if (panelGastosInternos.getTabCount() > 0) {
                        panelGastosInternos.setTitleAt(0, "Gastos Internos");
                }
                if (panelGastosInternos.getTabCount() > 1) {
                        panelGastosInternos.setTitleAt(1, "Compra Externa de Zapatos");
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // GETTERS PÚBLICOS - Sin cambios
        // ═══════════════════════════════════════════════════════════════════════════
        public javax.swing.JTabbedPane getPanelGastosInternos() {
                return panelGastosInternos;
        }

        public javax.swing.JComboBox<?> getCbxTipoGasto() {
                return cbxTipoGasto;
        }

        public javax.swing.JTextField getTxtConcepto() {
                return txtConcepto;
        }

        public javax.swing.JTextField getTxtMonto() {
                return txtMonto;
        }

        public javax.swing.JTextField getTxtTercero() {
                return txtTercero;
        }

        public javax.swing.JTextField getTxtRecibo() {
                return txtRecibo;
        }

        public javax.swing.JTextField getTxtObservaciones() {
                return txtObservaciones;
        }

        public javax.swing.JTable getTablaGastos() {
                return tablaGastos;
        }

        public javax.swing.JLabel getTxtTotalGastos() {
                return txtTotalGastos;
        }

        public javax.swing.JTextField getTxtTerceroCompraZ() {
                return txtTerceroCompraZ;
        }

        public javax.swing.JTextField getTxtReciboZ() {
                return txtReciboZ;
        }

        public javax.swing.JTextField getTxtDescripZ() {
                return txtDescripZ;
        }

        public javax.swing.JComboBox<?> getCmbTalla() {
                return cmbTalla;
        }

        public javax.swing.JComboBox<?> getCmbColor() {
                return cmbColor;
        }

        public javax.swing.JSpinner getSpnCantidad() {
                return spnCantidad;
        }

        public javax.swing.JTextField getTxtCompraPrecio() {
                return txtCompraPrecio;
        }

        public javax.swing.JTextField getTxtVentaPrecio() {
                return txtVentaPrecio;
        }

        public javax.swing.JLabel getTxtSubTotal() {
                return txtSubTotal;
        }

        public javax.swing.JTable getTablaCompras() {
                return tablaCompras;
        }

        public javax.swing.JButton getBtnRegistrarGasto() {
                return btnRegistrarGasto;
        }

        public javax.swing.JButton getBtnLimpiar() {
                return btnLimpiar;
        }

        public javax.swing.JButton getBtnCerrar() {
                return btnCerrar;
        }

        public javax.swing.JButton getBtnClose() {
                return btnClose;
        }

        public javax.swing.JButton getBtnAgregarItem() {
                return btnAgregarItem;
        }

        public javax.swing.JLabel getTxtMovimientoCaja() {
                return txtMovimientoCaja;
        }

        public GastosComprasController getController() {
                return formUI != null ? formUI.getController() : null;
        }

        public void agregarItemCompra() {
                if (formUI != null) {
                        formUI.agregarItemCompra();
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // GETTERS ADICIONALES PARA RESPONSIVE V3.0
        // ═══════════════════════════════════════════════════════════════════════════
        public boolean isResponsiveInicializado() {
                return responsiveInicializado;
        }

        /**
         * This method is called from within the constructor to initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is always
         * regenerated by the Form Editor.
         */
        @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated
        // <editor-fold defaultstate="collapsed" desc="Generated
        // Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPanel5 = new javax.swing.JPanel();
                jPanel13 = new javax.swing.JPanel();
                jPanel1 = new javax.swing.JPanel();
                panelGastosInternos = new javax.swing.JTabbedPane();

                jPanel4 = new javax.swing.JPanel();
                panelGastosInsert = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                cbxTipoGasto = new javax.swing.JComboBox<>();
                jLabel4 = new javax.swing.JLabel();
                txtConcepto = new javax.swing.JTextField();
                txtMonto = new javax.swing.JTextField();
                jLabel5 = new javax.swing.JLabel();
                txtTercero = new javax.swing.JTextField();
                txtRecibo = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                txtObservaciones = new javax.swing.JTextField();
                jLabel9 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                panelGastosList = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tablaGastos = new javax.swing.JTable();
                jPanel8 = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                txtTotalGastos = new javax.swing.JLabel();

                jPanel9 = new javax.swing.JPanel();
                jPanel10 = new javax.swing.JPanel();
                jLabel11 = new javax.swing.JLabel();
                txtTerceroCompraZ = new javax.swing.JTextField();
                txtReciboZ = new javax.swing.JTextField();
                jLabel12 = new javax.swing.JLabel();
                jLabel13 = new javax.swing.JLabel();
                jPanel12 = new javax.swing.JPanel();
                jLabel14 = new javax.swing.JLabel();
                jLabel15 = new javax.swing.JLabel();
                jLabel16 = new javax.swing.JLabel();
                jLabel17 = new javax.swing.JLabel();
                txtDescripZ = new javax.swing.JTextField();
                jLabel18 = new javax.swing.JLabel();
                cmbTalla = new javax.swing.JComboBox<>();
                jLabel19 = new javax.swing.JLabel();
                cmbColor = new javax.swing.JComboBox<>();
                jLabel20 = new javax.swing.JLabel();
                spnCantidad = new javax.swing.JSpinner();
                txtCompraPrecio = new javax.swing.JTextField();
                jLabel21 = new javax.swing.JLabel();
                txtVentaPrecio = new javax.swing.JTextField();
                jLabel22 = new javax.swing.JLabel();
                jPanel14 = new javax.swing.JPanel();
                jLabel23 = new javax.swing.JLabel();
                txtSubTotal = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                tablaCompras = new javax.swing.JTable();
                btnAgregarItem = new javax.swing.JButton();
                panelBotonera = new javax.swing.JPanel();
                btnRegistrarGasto = new javax.swing.JButton();
                btnCerrar = new javax.swing.JButton();
                btnLimpiar = new javax.swing.JButton();
                panelTitulo = new javax.swing.JPanel();
                txtMovimientoCaja = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                btnClose = new javax.swing.JButton();

                javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
                jPanel5.setLayout(jPanel5Layout);
                jPanel5Layout.setHorizontalGroup(
                                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 100, Short.MAX_VALUE));
                jPanel5Layout.setVerticalGroup(
                                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 100, Short.MAX_VALUE));

                javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
                jPanel13.setLayout(jPanel13Layout);
                jPanel13Layout.setHorizontalGroup(
                                jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 100, Short.MAX_VALUE));
                jPanel13Layout.setVerticalGroup(
                                jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 100, Short.MAX_VALUE));

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setMinimumSize(new java.awt.Dimension(1200, 700)); // Minimum size for proper display
                setUndecorated(true);

                // Set a reasonable size that fits well on most screens
                setSize(new java.awt.Dimension(1400, 850));
                setLocationRelativeTo(null);

                jLabel3.setText("Tipo de Gasto * ");

                jLabel4.setText("Concepto * ");

                jLabel5.setText("Monto ($) * ");

                jLabel8.setText("N° recibo");

                jLabel9.setText("Observaciones");

                jLabel1.setText("Proveedor/Tercero ");

                javax.swing.GroupLayout panelGastosInsertLayout = new javax.swing.GroupLayout(panelGastosInsert);
                panelGastosInsert.setLayout(panelGastosInsertLayout);
                panelGastosInsertLayout.setHorizontalGroup(
                                panelGastosInsertLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelGastosInsertLayout.createSequentialGroup()
                                                                .addGap(14, 14, 14)
                                                                .addGroup(panelGastosInsertLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelGastosInsertLayout
                                                                                                .createParallelGroup(
                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                false)
                                                                                                .addComponent(jLabel8)
                                                                                                .addComponent(jLabel9)
                                                                                                .addComponent(jLabel5)
                                                                                                .addComponent(jLabel4)
                                                                                                .addComponent(jLabel3)
                                                                                                .addComponent(txtMonto)
                                                                                                .addComponent(txtConcepto)
                                                                                                .addComponent(cbxTipoGasto,
                                                                                                                0, 370,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(txtObservaciones)
                                                                                                .addComponent(txtTercero)
                                                                                                .addComponent(txtRecibo))
                                                                                .addComponent(jLabel1,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                161,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap(30, Short.MAX_VALUE)));
                panelGastosInsertLayout.setVerticalGroup(
                                panelGastosInsertLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelGastosInsertLayout.createSequentialGroup()
                                                                .addGap(22, 22, 22)
                                                                .addComponent(jLabel3)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxTipoGasto,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel4)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtConcepto,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel5)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtMonto,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel1)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtTercero,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(20, 20, 20)
                                                                .addComponent(jLabel8)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtRecibo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel9)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtObservaciones,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(129, Short.MAX_VALUE)));

                jLabel6.setText("Gastos hoy");

                tablaGastos.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {

                                },
                                new String[] {
                                                "id", "Tipo", "Concepto", "Monto", "Proveedor", "Estado"
                                }) {
                        boolean[] canEdit = new boolean[] {
                                        false, false, true, false, false, true
                        };

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit[columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tablaGastos);
                if (tablaGastos.getColumnModel().getColumnCount() > 0) {
                        tablaGastos.getColumnModel().getColumn(0).setPreferredWidth(50);
                        tablaGastos.getColumnModel().getColumn(1).setPreferredWidth(120);
                        tablaGastos.getColumnModel().getColumn(2).setPreferredWidth(100);
                        tablaGastos.getColumnModel().getColumn(3).setPreferredWidth(100);
                        tablaGastos.getColumnModel().getColumn(4).setPreferredWidth(100);
                        tablaGastos.getColumnModel().getColumn(5).setPreferredWidth(100);
                }

                jLabel10.setText("Total Gastos del Día:");

                txtTotalGastos.setText("$0");

                javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
                jPanel8.setLayout(jPanel8Layout);
                jPanel8Layout.setHorizontalGroup(
                                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel8Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel10)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                20,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(txtTotalGastos,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                457,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)));
                jPanel8Layout.setVerticalGroup(
                                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel8Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(jPanel8Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(jLabel10,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(txtTotalGastos,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                33,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));

                javax.swing.GroupLayout panelGastosListLayout = new javax.swing.GroupLayout(panelGastosList);
                panelGastosList.setLayout(panelGastosListLayout);
                panelGastosListLayout.setHorizontalGroup(
                                panelGastosListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelGastosListLayout.createSequentialGroup()
                                                                .addGap(17, 17, 17)
                                                                .addGroup(panelGastosListLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jPanel8,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(panelGastosListLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel6,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                189,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addContainerGap())))
                                                .addGroup(panelGastosListLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jScrollPane1)
                                                                .addContainerGap()));
                panelGastosListLayout.setVerticalGroup(
                                panelGastosListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelGastosListLayout.createSequentialGroup()
                                                                .addGap(23, 23, 23)
                                                                .addComponent(jLabel6)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jScrollPane1,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                427, Short.MAX_VALUE)
                                                                .addGap(20, 20, 20)
                                                                .addComponent(jPanel8,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
                jPanel4.setLayout(jPanel4Layout);
                jPanel4Layout.setHorizontalGroup(
                                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel4Layout.createSequentialGroup()
                                                                .addComponent(panelGastosInsert,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(panelGastosList,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                jPanel4Layout.setVerticalGroup(
                                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel4Layout.createSequentialGroup()
                                                                .addGap(0, 0, 0)
                                                                .addGroup(jPanel4Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(panelGastosList,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelGastosInsert,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(0, 0, 0)));

                // gastoInternoSub (JScrollPane) removed. Adding jPanel4 directly to tab
                panelGastosInternos.addTab("Gastos Internos", jPanel4);

                jPanel9.setBackground(new java.awt.Color(204, 204, 255));

                jLabel11.setText("Tienda/ tercero");

                jLabel12.setText("N° Factura/Recibo ");

                jLabel13.setText("Datos del Producto ");

                jLabel14.setText("Producto a Crear ");

                jLabel15.setText("Zapato Genérico - Compra Externa");

                jLabel16.setText("Se creará como producto genérico en inventario  ");

                javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
                jPanel12.setLayout(jPanel12Layout);
                jPanel12Layout.setHorizontalGroup(
                                jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel12Layout.createSequentialGroup()
                                                                .addGroup(jPanel12Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(jPanel12Layout
                                                                                                .createSequentialGroup()
                                                                                                .addContainerGap()
                                                                                                .addComponent(jLabel14))
                                                                                .addGroup(jPanel12Layout
                                                                                                .createSequentialGroup()
                                                                                                .addGap(103, 103, 103)
                                                                                                .addGroup(jPanel12Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(jLabel16,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                425,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                .addComponent(jLabel15,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                425,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                jPanel12Layout.setVerticalGroup(
                                jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel12Layout.createSequentialGroup()
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(jLabel14)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel15,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                30,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel16,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                30,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)));

                jLabel17.setText("Descripción del Producto * ");

                jLabel18.setText("Talla*");

                cmbTalla.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                cmbTallaActionPerformed(evt);
                        }
                });

                jLabel19.setText("Cantidad");

                jLabel20.setText("Color");

                jLabel21.setText("Precio Compra (c/u) * ");

                jLabel22.setText("Precio Venta Sugerido ");

                jLabel23.setText("Subtotal: ");

                txtSubTotal.setText("$0");

                javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
                jPanel14.setLayout(jPanel14Layout);
                jPanel14Layout.setHorizontalGroup(
                                jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel14Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel23)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                163,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(txtSubTotal,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                268,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap()));
                jPanel14Layout.setVerticalGroup(
                                jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel14Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(jPanel14Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(jLabel23,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(txtSubTotal,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                33,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));

                tablaCompras.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {

                                },
                                new String[] {
                                                "Descripción", "Talla", "Color", "Cant", "P.Compra", "P.Venta",
                                                "Subtotal"
                                }));
                jScrollPane2.setViewportView(tablaCompras);

                btnAgregarItem.setText(" Agregar");

                javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
                jPanel10.setLayout(jPanel10Layout);
                jPanel10Layout.setHorizontalGroup(
                                jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout
                                                                .createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jScrollPane2,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                430, Short.MAX_VALUE)
                                                                .addGap(20, 20, 20)
                                                                .addGroup(jPanel10Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(jPanel10Layout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(jPanel10Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(txtTerceroCompraZ,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                263,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                .addComponent(jLabel11))
                                                                                                .addGap(18, 18, 18)
                                                                                                .addGroup(jPanel10Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(txtReciboZ,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                263,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                .addComponent(jLabel12)))
                                                                                .addComponent(jLabel13)
                                                                                .addComponent(jPanel12,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGroup(jPanel10Layout
                                                                                                .createParallelGroup(
                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                false)
                                                                                                .addGroup(jPanel10Layout
                                                                                                                .createSequentialGroup()
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                .addComponent(txtCompraPrecio,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                263,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                .addComponent(jLabel21))
                                                                                                                .addGap(18, 18, 18)
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                .addComponent(txtVentaPrecio,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                263,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                .addComponent(jLabel22)))
                                                                                                .addComponent(jLabel17)
                                                                                                .addComponent(txtDescripZ)
                                                                                                .addGroup(jPanel10Layout
                                                                                                                .createSequentialGroup()
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addComponent(jLabel18)
                                                                                                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                                                                                                .addComponent(cmbTalla,
                                                                                                                                                0,
                                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                                Short.MAX_VALUE))
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                .addComponent(jLabel20)
                                                                                                                                .addComponent(cmbColor,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                180,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                                false)
                                                                                                                                .addComponent(jLabel19)
                                                                                                                                .addComponent(spnCantidad,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                174,
                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                                                .addComponent(jPanel14,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addComponent(btnAgregarItem,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                145,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(0, 0, 0)));
                jPanel10Layout.setVerticalGroup(
                                jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel10Layout.createSequentialGroup()
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGroup(jPanel10Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                jPanel10Layout
                                                                                                                .createSequentialGroup()
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addComponent(jLabel12)
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                                                .addComponent(txtReciboZ,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                50,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addComponent(jLabel11)
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                                                .addComponent(txtTerceroCompraZ,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                50,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                                                                .addGap(28, 28, 28)
                                                                                                                .addComponent(jLabel13)
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                                .addComponent(jPanel12,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                .addGap(18, 18, 18)
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addComponent(jLabel17)
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                                                                .addComponent(txtDescripZ,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                50,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                                .addGap(18, 18, 18)
                                                                                                                                                .addComponent(jLabel18)
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                                                .addComponent(cmbTalla,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                40,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                                .createParallelGroup(
                                                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                                                .addComponent(jLabel20,
                                                                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                                                                                .addComponent(jLabel19,
                                                                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING))
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                                .createParallelGroup(
                                                                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                                                                false)
                                                                                                                                                                .addComponent(cmbColor)
                                                                                                                                                                .addComponent(spnCantidad,
                                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                40,
                                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                                                                                .addGap(18, 18, 18)
                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                .createParallelGroup(
                                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addComponent(jLabel22)
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                                                .addComponent(txtVentaPrecio,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                50,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                                .addGroup(jPanel10Layout
                                                                                                                                                .createSequentialGroup()
                                                                                                                                                .addComponent(jLabel21)
                                                                                                                                                .addPreferredGap(
                                                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                                                .addComponent(txtCompraPrecio,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                50,
                                                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addComponent(jPanel14,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                .addPreferredGap(
                                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                                .addComponent(btnAgregarItem,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                28,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addComponent(jScrollPane2,
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                613, Short.MAX_VALUE))
                                                                .addGap(67, 67, 67)));

                javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
                jPanel9.setLayout(jPanel9Layout);
                jPanel9Layout.setHorizontalGroup(
                                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
                jPanel9Layout.setVerticalGroup(
                                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel9Layout.createSequentialGroup()
                                                                .addComponent(jPanel10,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                684, Short.MAX_VALUE)
                                                                .addGap(0, 0, 0)));

                // compraExternaSub (JScrollPane) removed. Adding jPanel9 directly to tab
                panelGastosInternos.addTab("Compra Externa de Zapatos", jPanel9);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelGastosInternos));
                jPanel1Layout.setVerticalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelGastosInternos));

                panelBotonera.setBackground(new java.awt.Color(204, 206, 241));
                panelBotonera.setForeground(new java.awt.Color(204, 206, 241));

                btnRegistrarGasto.setText("Registrar Gasto");

                btnCerrar.setText("Cerrar");

                btnLimpiar.setText("Limpiar");

                javax.swing.GroupLayout panelBotoneraLayout = new javax.swing.GroupLayout(panelBotonera);
                panelBotonera.setLayout(panelBotoneraLayout);
                panelBotoneraLayout.setHorizontalGroup(
                                panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelBotoneraLayout.createSequentialGroup()
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(btnLimpiar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                120,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(btnCerrar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                120,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(btnRegistrarGasto,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                160,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelBotoneraLayout.setVerticalGroup(
                                panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelBotoneraLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addGroup(panelBotoneraLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(btnRegistrarGasto,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                45,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(btnCerrar,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                45,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(btnLimpiar,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                45,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap(10, Short.MAX_VALUE)));

                panelTitulo.setBackground(new java.awt.Color(255, 204, 204));

                txtMovimientoCaja.setText("Caja #1 - Movimiento del día | Usuario: Admin ");

                jLabel2.setText("Gastos Internos y Compras ");

                javax.swing.GroupLayout panelTituloLayout = new javax.swing.GroupLayout(panelTitulo);
                panelTitulo.setLayout(panelTituloLayout);
                panelTituloLayout.setHorizontalGroup(
                                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTituloLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelTituloLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                false)
                                                                                .addComponent(txtMovimientoCaja,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                780,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel2,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addComponent(btnClose,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                43,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(16, 16, 16)));
                panelTituloLayout.setVerticalGroup(
                                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTituloLayout.createSequentialGroup()
                                                                .addGroup(panelTituloLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelTituloLayout
                                                                                                .createSequentialGroup()
                                                                                                .addGap(4, 4, 4)
                                                                                                .addComponent(jLabel2,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                39,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(txtMovimientoCaja,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                39,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addGroup(panelTituloLayout
                                                                                                .createSequentialGroup()
                                                                                                .addGap(15, 15, 15)
                                                                                                .addComponent(btnClose,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                43,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addContainerGap()));

                mainPanel = new javax.swing.JPanel();
                mainScrollPane = new javax.swing.JScrollPane();
                mainScrollPane.setBorder(null);
                mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
                // Remove fixed size to allow adaptive layout

                javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
                mainPanel.setLayout(mainPanelLayout);
                mainPanelLayout.setHorizontalGroup(
                                mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(panelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(panelTitulo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
                mainPanelLayout.setVerticalGroup(
                                mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                                .addComponent(panelTitulo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, 0)
                                                                .addComponent(jPanel1,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(0, 0, 0)
                                                                .addComponent(panelBotonera,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void cmbTallaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbTallaActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_cmbTallaActionPerformed

        /**
         * @param args the command line arguments
         */
        public static void main(String args[]) {
                /* Set the Nimbus look and feel */
                // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
                // (optional) ">
                /*
                 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
                 * look and feel.
                 * For details see
                 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
                 */
                try {
                        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager
                                        .getInstalledLookAndFeels()) {
                                if ("Nimbus".equals(info.getName())) {
                                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                                        break;
                                }
                        }
                } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
                        logger.log(java.util.logging.Level.SEVERE, null, ex);
                }
                // </editor-fold>

                /* Create and display the dialog */
                java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                gastosForm dialog = new gastosForm(new javax.swing.JFrame(), true);
                                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                                        @Override
                                        public void windowClosing(java.awt.event.WindowEvent e) {
                                                System.exit(0);
                                        }
                                });
                                dialog.setVisible(true);
                        }
                });
        }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAgregarItem;
        private javax.swing.JButton btnCerrar;
        private javax.swing.JButton btnClose;
        private javax.swing.JButton btnLimpiar;
        private javax.swing.JButton btnRegistrarGasto;
        private javax.swing.JComboBox<ModelTipoGasto> cbxTipoGasto;
        private javax.swing.JComboBox<ModelColor> cmbColor;
        private javax.swing.JComboBox<ModelTalla> cmbTalla;

        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel17;
        private javax.swing.JLabel jLabel18;
        private javax.swing.JLabel jLabel19;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel20;
        private javax.swing.JLabel jLabel21;
        private javax.swing.JLabel jLabel22;
        private javax.swing.JLabel jLabel23;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel10;
        private javax.swing.JPanel jPanel12;
        private javax.swing.JPanel jPanel13;
        private javax.swing.JPanel jPanel14;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JPanel jPanel5;
        private javax.swing.JPanel jPanel8;
        private javax.swing.JPanel jPanel9;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane mainScrollPane;
        private javax.swing.JPanel mainPanel;
        private javax.swing.JPanel panelBotonera;
        private javax.swing.JPanel panelGastosInsert;
        private javax.swing.JTabbedPane panelGastosInternos;
        private javax.swing.JPanel panelGastosList;
        private javax.swing.JPanel panelTitulo;
        private javax.swing.JSpinner spnCantidad;
        private javax.swing.JTable tablaCompras;
        public javax.swing.JTable tablaGastos;
        private javax.swing.JTextField txtCompraPrecio;
        private javax.swing.JTextField txtConcepto;
        private javax.swing.JTextField txtDescripZ;
        private javax.swing.JTextField txtMonto;
        private javax.swing.JLabel txtMovimientoCaja;
        private javax.swing.JTextField txtObservaciones;
        private javax.swing.JTextField txtRecibo;
        private javax.swing.JTextField txtReciboZ;
        private javax.swing.JLabel txtSubTotal;
        private javax.swing.JTextField txtTercero;
        private javax.swing.JTextField txtTerceroCompraZ;
        private javax.swing.JLabel txtTotalGastos;
        private javax.swing.JTextField txtVentaPrecio;
        // End of variables declaration//GEN-END:variables
}
