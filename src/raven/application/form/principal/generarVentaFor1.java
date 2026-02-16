package raven.application.form.principal;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.application.Application;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.clases.comercial.ServiceCliente;
import raven.clases.comercial.ServiceNotaCredito;
import raven.clases.principal.ServiceVenta;
import raven.clases.productos.ServiceProduct;
import raven.clases.productos.ServiceProductVariant;
import raven.componentes.impresion.DataSearch;
import raven.componentes.impresion.DataSearchClient;
import raven.componentes.impresion.EventClick;
import raven.componentes.impresion.EventClickC;
import raven.componentes.impresion.PanelSearch;
import raven.componentes.impresion.PanelSearchC;
import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ModelUser;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.comercial.ModelNotaCredito;
import raven.controlador.principal.ModelDetalleVenta;
import raven.controlador.principal.ModelVenta;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelProductVariant;
import raven.modal.Toast;
import raven.componentes.ventas.PanelMultiplesPagos;
import raven.clases.principal.ServiceMedioPago;
import raven.controlador.principal.ModelMedioPago;
import raven.componentes.cajas.MonitorInactividadCaja;
import raven.application.form.admin.CierreCajaDialog;
import raven.application.form.admin.AperturaCajaDialog;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import javax.swing.Timer;
import static org.apache.poi.hwpf.model.FileInformationBlock.logger;
import raven.application.form.other.FormDashboard;
import raven.clases.admin.ServiceCaja;
import raven.clases.admin.UserSession;
import raven.clases.comercial.OrdenReservaService;
import raven.clases.productos.ServiceInventarioBodega;
import raven.clases.productos.ImpresionPOST;
import raven.componentes.RoundedScrollPane;
import raven.componentes.BuscadorProductoDialog;
import raven.componentes.LoadingOverlayHelper;
import raven.componentes.LoadingSpinner;
import raven.controlador.productos.ModelInventarioBodega;
import raven.modelos.OrdenReservaDetalle;
import raven.modelos.Usuario;
import raven.dao.UsuarioDAO;
import raven.utils.BlurDialog;

/**
 *
 * @author lmog2
 */
public class generarVentaFor1 extends javax.swing.JPanel {

    // ==================== CONSTANTES DE CONFIGURACIÓN ====================
    // Estilos UI con FlatLaf
    private static final String PANEL_STYLE = "arc:25;background:$Login.background;";
    private static final String PANEL_SUBSTYLE = "arc:25;background:$Login.background;";
    private static final String CAMPOS_TEXTO = "arc:15;background:lighten($Menu.background,25%)";
    private static final String FONT_HEADER_STYLE = "font:$h1.font";

    // Breakpoints responsive
    private static final int BREAKPOINT_MOBILE = 800;
    private static final int BREAKPOINT_TABLET = 1200;
    // Dimensiones base
    private static final int SIDEBAR_WIDTH = 346;
    private static final int SIDEBAR_MIN_WIDTH = 300;
    private static final int PANEL_PRODUCTOS_HEIGHT = 400;
    private static final int PANEL_FINAL_HEIGHT = 110;
    private static final int PANEL_TITULO_HEIGHT = 60;

    // Índices de columnas en tabla
    private static final int COL_CODIGO = 0;
    private static final int COL_PRODUCTO = 1;
    private static final int COL_TIPO = 2;
    private static final int COL_CANTIDAD = 3;
    private static final int COL_PRECIO = 4;
    private static final int COL_DESCUENTO = 5;
    private static final int COL_SUBTOTAL = 6;
    private static final int COL_ACCION = 7; // ← NUEVA COLUMNA
    private static final int COL_ID_DETALLE = 8; // Actualizado de 7 a 8
    private static final int COL_ID_VARIANTE = 9; // Actualizado de 8 a 9
    private static final int COL_ID_PRODUCTO = 10; // Actualizado de 9 a 10

    // ==================== SERVICIOS Y DEPENDENCIAS ====================
    private final ServiceVenta serviceVenta = new ServiceVenta();
    private final ServiceCajaMovimiento serviceCaja = new ServiceCajaMovimiento();
    private final ServiceProduct service = new ServiceProduct();
    private final ServiceProductVariant serviceVariant = new ServiceProductVariant();
    private final ServiceNotaCredito serviceNotaCredito = new ServiceNotaCredito();
    private PanelMultiplesPagos panelMultiplesPagos;
    private final ServiceMedioPago serviceMedioPago = new ServiceMedioPago();
    private MonitorInactividadCaja monitorInactividad;
    private ModelCajaMovimiento movimientoActual;
    private boolean cajaAbierta = false;
    // private JPanel panelEstadoCaja; // Panel visual de estado de caja
    private boolean origenOrdenReserva = false;
    private final ServiceInventarioBodega serviceInventarioBodega = new ServiceInventarioBodega();
    // ==================== CONSTANTES DE ESTADO ====================
    private boolean isLoadingTallas = false; // Flag para prevenir eventos mientras cargamos tallas
    private boolean isLoadingColores = false; // Flag para prevenir eventos mientras cargamos colores
    private boolean isSelectingProduct = false; // Flag para evitar recargas innecesarias durante selección
    private boolean isUpdatingProductData = false; // Flag para prevenir actualización recursiva
    private String previousTallaSelected = ""; // Guardar talla anterior para detectar cambios reales
    private String previousColorSelected = ""; // Guardar color anterior para detectar cambios reales
    private RoundedScrollPane scrollTabla;
    private javax.swing.Timer pulsoAnimacionTimer;
    private boolean listenersConfigurados = false;
    private javax.swing.Timer clientSearchDebounceTimer; // Timer for client search debouncing

    // ==================== LOADING BUTTON STATE ====================
    private boolean isProcessingVenta = false; // Flag para prevenir doble clic
    private javax.swing.Timer spinnerTimer; // Timer para animación del spinner
    private int spinnerAngle = 0; // Ángulo actual del spinner
    private String originalButtonText = ""; // Texto original del botón
    private javax.swing.Icon originalButtonIcon = null; // Icono original del botón

    // ==================== VENDEDOR SELECCIONADO ====================
    private Usuario vendedorSeleccionado = null;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private javax.swing.JComboBox<Usuario> cbxVendedor;
    private javax.swing.JLabel lblVendedor;
    // ==================== COMPONENTES DE BÚSQUEDA ====================
    private PanelSearch searchProd;
    private PanelSearchC searchClient;
    private JPopupMenu menu;
    private JPopupMenu menu1;

    private enum EstadoCaja {
        NO_INICIALIZADA,
        VALIDANDO,
        ABIERTA,
        CERRADA,
        ERROR
    }

    private EstadoCaja estadoCaja = EstadoCaja.NO_INICIALIZADA;
    // ==================== ESTADO DE LA APLICACIÓN ====================
    private DataSearch selectedProduct;
    private DataSearchClient selectClient;
    private ModelNotaCredito notaCreditoSeleccionada;

    private BigDecimal totalSub = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal saldoPendiente = BigDecimal.ZERO;
    private int idUsuarioActual;
    private int idCajaActiva;
    // Modo edición
    private boolean modoEdicion = false;
    private int idVentaEdicion = -1;
    private List<Integer> productosEliminados = new ArrayList<>();

    // Control de carga de combos
    // private boolean isLoadingTallas = false;
    // private boolean isLoadingColores = false;
    // Iconos
    private FontIcon iconVenta;
    private FontIcon iconProd;
    private FontIcon iconCliente;
    private FontIcon iconCotiz;
    private FontIcon iconLimpi;

    // ==================== CONSTRUCTOR PRINCIPAL ====================
    public generarVentaFor1() throws SQLException {
        // 1. Inicializar UI
        initComponents();
        configurarDimensionesIniciales();
        configurarResponsive();
        cargarUi();

        // 2. Configurar fuentes
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // 3. Inicializar componentes
        initMenus();
        initPlaceHolders();
        initComboBoxes();
        initBusquedaAutomatica();
        configurarIconos();
        initComboTallaListener();
        initComboColorListener();

        // 4. Configurar tabla
        configurarModeloDetalle();
        configureTableListener();

        // 5. Panel de pagos
        inicializarPanelMultiplesPagos();

        // 6. Ocultar paneles
        ocultarPanalesIniciales();

        // 7. Cargar contexto SIMPLIFICADO (sin validaciones bloqueantes)
        cargarContextoSesionSimplificado();

        // 7.5 Inicializar vendedor y cargar vendedores desde BD
        inicializarPanelVendedor();

        // 8. Configurar caja (ya sabemos que está abierta)
        configurarCajaAbierta();
        // 9. Panel visual de estado
        configurarPanelEstadoCaja();
        jLabel12.setVisible(false);
        cbxTipoPago.setVisible(false);

        // 10. NUEVO: Configurar atajos de teclado para acceso rápido
        configurarAtajosTeclado();

        revalidate();
        repaint();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATAJOS DE TECLADO PARA ACCESO RÁPIDO EN FACTURACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Configura los atajos de teclado para acceso rápido durante la facturación.
     * 
     * ATAJOS DISPONIBLES:
     * - F1: Enfocar campo de búsqueda de producto
     * - F2: Agregar producto a la tabla (si está habilitado)
     * - F3: Abrir configuración de pagos múltiples
     * - F4: Generar venta (si está habilitado)
     * - F5: Limpiar formulario completo
     * - ESC: Limpiar selección actual del producto
     * - F6: Buscar cliente
     * - F7: Bloquear caja
     * - F8: Cerrar caja
     */
    private void configurarAtajosTeclado() {
        // Obtener InputMap y ActionMap del panel
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();

        // F1 - BUSCAR PRODUCTO (Enfocar campo de búsqueda)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0), "buscarProducto");
        actionMap.put("buscarProducto", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (txtIngresarCodigo != null) {
                    txtIngresarCodigo.requestFocusInWindow();
                    txtIngresarCodigo.selectAll();
                }
            }
        });

        // F2 - AGREGAR PRODUCTO (Simula clic en btnAgregarProd)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "agregarProducto");
        actionMap.put("agregarProducto", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnAgregarProd != null && btnAgregarProd.isEnabled()) {
                    btnAgregarProd.doClick();
                } else {
                    Toast.show(generarVentaFor1.this, Toast.Type.WARNING, "Complete los datos del producto primero");
                }
            }
        });

        // F3 - CONFIGURAR PAGOS (Abrir panel de múltiples pagos)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "configurarPagos");
        actionMap.put("configurarPagos", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnConfigurarPagos != null && btnConfigurarPagos.isEnabled()) {
                    btnConfigurarPagos.doClick();
                }
            }
        });

        // F4 - GENERAR VENTA (Simula clic en btngenerarVenta)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0), "generarVenta");
        actionMap.put("generarVenta", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btngenerarVenta != null && btngenerarVenta.isEnabled()) {
                    btngenerarVenta.doClick();
                } else {
                    Toast.show(generarVentaFor1.this, Toast.Type.WARNING, "Complete el pago antes de generar la venta");
                }
            }
        });

        // F5 - LIMPIAR FORMULARIO (Simula clic en btnLimpiar)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "limpiarFormulario");
        actionMap.put("limpiarFormulario", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnLimpiar != null) {
                    btnLimpiar.doClick();
                }
            }
        });

        // CTRL + H - SELECCIONAR UNO O VARIOS TRASPASOS
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "mostrarTraspasosSeleccion");
        actionMap.put("mostrarTraspasosSeleccion", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarModalTraspasosSeleccion();
            }
        });

        // ESC - LIMPIAR SELECCIÓN ACTUAL (Sin confirmar)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "limpiarSeleccion");
        actionMap.put("limpiarSeleccion", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedProduct = null;
                limpiarCampos();
                limpiarSeleccionesVariantes();
                if (txtIngresarCodigo != null) {
                    txtIngresarCodigo.setText("");
                    txtIngresarCodigo.requestFocusInWindow();
                }
                ICON.setIcon(null);
                ICON.setText("ICON");
                panelInfoProd.setVisible(false);
            }
        });

        // F6 - BUSCAR CLIENTE (Enfocar campo de cliente)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0), "buscarCliente");
        actionMap.put("buscarCliente", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (txtCliente != null) {
                    txtCliente.requestFocusInWindow();
                    txtCliente.selectAll();
                }
            }
        });

        // F7 - BLOQUEAR CAJA
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0), "bloquearCaja");
        actionMap.put("bloquearCaja", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnBloquear != null && btnBloquear.isEnabled()) {
                    btnBloquear.doClick();
                }
            }
        });

        // F8 - CERRAR CAJA
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0), "cerrarCaja");
        actionMap.put("cerrarCaja", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnCerrarCaja != null && btnCerrarCaja.isEnabled()) {
                    btnCerrarCaja.doClick();
                }
            }
        });

        // CTRL+T - CAMBIO DE TALLA
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "mostrarModalCambioTalla");
        actionMap.put("mostrarModalCambioTalla", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarModalCambioTalla();
            }
        });

        // CTRL+G - REGISTRAR GASTO
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "registrarGasto");
        actionMap.put("registrarGasto", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnGastos != null && btnGastos.isEnabled()) {
                    btnGastos.doClick();
                }
            }
        });

    }

    /**
     * Configura el estado de caja como abierta. MainForm ya verificó que está
     * abierta.
     */
    private void configurarCajaAbierta() {
        try {
            // Obtener el movimiento abierto (ya sabemos que existe)
            movimientoActual = serviceCaja.obtenerMovimientoAbierto(
                    idCajaActiva, idUsuarioActual);

            if (movimientoActual != null) {
                cajaAbierta = true;
                estadoCaja = EstadoCaja.ABIERTA;

                // Iniciar monitor de inactividad
                iniciarMonitorInactividad();
            } else {
                // Esto no debería pasar si MainForm validó correctamente
                System.err.println("WARN: No se encontró movimiento abierto - esto no debería ocurrir");
                cajaAbierta = false;
                estadoCaja = EstadoCaja.CERRADA;
            }

        } catch (SQLException ex) {
            Logger.getLogger(generarVentaFor1.class.getName())
                    .log(Level.SEVERE, "Error configurando caja", ex);
            estadoCaja = EstadoCaja.ERROR;
        }
    }

    /**
     * Configura los iconos de FontAwesome y colores para todos los botones del
     * formulario.
     * Utiliza la librería ikonli con FontAwesomeSolid.
     */
    private void configurarIconos() {
        // ========================================
        // BOTONES DEL PANEL DE CAJA
        // ========================================
        if (btnGastos != null) {
            btnGastos.setIcon(createColoredIcon(FontAwesomeSolid.RECEIPT, Color.WHITE));
            // configurarEstiloBoton(btnGastos, new Color(155, 89, 182), Color.WHITE); //
            // Púrpura
        }
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setIcon(createColoredIcon(FontAwesomeSolid.CASH_REGISTER, Color.WHITE));
            // configurarEstiloBoton(btnCerrarCaja, new Color(231, 76, 60), Color.WHITE); //
            // Rojo
        }
        if (btnBloquear != null) {
            btnBloquear.setIcon(createColoredIcon(FontAwesomeSolid.LOCK, Color.WHITE));
            // configurarEstiloBoton(btnBloquear, new Color(241, 196, 15), Color.WHITE); //
            // Amarillo
        }

        // ========================================
        // BOTONES DE ACCIONES PRINCIPALES
        // ========================================
        if (btngenerarVenta != null) {
            btngenerarVenta.setIcon(createColoredIcon(FontAwesomeSolid.SHOPPING_CART, Color.WHITE));
            configurarEstiloBoton(btngenerarVenta, new Color(39, 174, 96), Color.WHITE); // Verde
        }
        if (btnLimpiar != null) {
            btnLimpiar.setIcon(createColoredIcon(FontAwesomeSolid.BROOM, Color.WHITE));
            configurarEstiloBoton(btnLimpiar, new Color(192, 57, 43), Color.WHITE); // Rojo oscuro
        }

        // ========================================
        // BOTONES DE CLIENTE Y PRODUCTO
        // ========================================
        if (btnSeleccionarCliente != null) {
            btnSeleccionarCliente.setIcon(createColoredIcon(FontAwesomeSolid.USER_PLUS, Color.WHITE));
            configurarEstiloBoton(btnSeleccionarCliente, new Color(52, 152, 219), Color.WHITE); // Azul
        }
        if (btnAgregarProd != null) {
            btnAgregarProd.setIcon(createColoredIcon(FontAwesomeSolid.PLUS_CIRCLE, Color.WHITE));
            configurarEstiloBoton(btnAgregarProd, new Color(46, 204, 113), Color.WHITE); // Verde claro
        }
        if (btnBuscarProducto != null) {
            btnBuscarProducto.setIcon(createColoredIcon(FontAwesomeSolid.SEARCH, Color.WHITE));
            configurarEstiloBoton(btnBuscarProducto, new Color(52, 73, 94), Color.WHITE); // Gris oscuro
        }

        // ========================================
        // BOTONES DE VALIDACIÓN Y PAGOS
        // ========================================
        if (btnValidar != null) {
            btnValidar.setIcon(createColoredIcon(FontAwesomeSolid.CHECK_CIRCLE, Color.WHITE));
            configurarEstiloBoton(btnValidar, new Color(39, 174, 96), Color.WHITE); // Verde
        }
        if (btnConfigurarPagos != null) {
            btnConfigurarPagos.setIcon(createColoredIcon(FontAwesomeSolid.CREDIT_CARD, Color.WHITE));
            configurarEstiloBoton(btnConfigurarPagos, new Color(241, 196, 15), new Color(44, 62, 80)); // Amarillo con
                                                                                                       // texto oscuro
        }
    }

    /**
     * Configura el estilo visual de un botón.
     */
    private void configurarEstiloBoton(javax.swing.AbstractButton btn, Color background, Color foreground) {
        btn.setBackground(background);
        btn.setForeground(foreground);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Crea un icono de FontAwesome con el color especificado.
     * 
     * @param icon  El icono de Ikonli a usar (ej: FontAwesomeSolid.SHOPPING_CART)
     * @param color El color del icono
     * @return FontIcon configurado con el tamaño y color especificados
     */
    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    private static FlatSVGIcon svgIcon(String path, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon(path, size, size);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return icon;
    }

    private static String toHex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String dialogButtonStyle(Color bg, Color hover, Color pressed) {
        return "arc:12;"
                + "focusWidth:0;"
                + "borderWidth:0;"
                + "background:#" + toHex(bg) + ";"
                + "hoverBackground:#" + toHex(hover) + ";"
                + "pressedBackground:#" + toHex(pressed) + ";"
                + "foreground:#FFFFFF;"
                + "font:bold 13;"
                + "margin:8,16,8,16";
    }

    private static JButton createDialogButton(String text, FlatSVGIcon icon, String style) {
        JButton btn = new JButton(text);
        btn.setIcon(icon);
        btn.setIconTextGap(8);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn.putClientProperty(FlatClientProperties.STYLE, style);
        return btn;
    }

    private int showConfirmYesNoDialog(Component parent, String message, String title, int messageType,
            JButton btnYes, JButton btnNo) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog;
        if (owner instanceof Frame) {
            dialog = new JDialog((Frame) owner, title, true);
        } else if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog) owner, title, true);
        } else {
            dialog = new JDialog((Frame) null, title, true);
        }

        JPanel content = new JPanel(new BorderLayout(16, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        Icon icon = null;
        if (messageType == JOptionPane.ERROR_MESSAGE) {
            icon = UIManager.getIcon("OptionPane.errorIcon");
        } else if (messageType == JOptionPane.WARNING_MESSAGE) {
            icon = UIManager.getIcon("OptionPane.warningIcon");
        } else if (messageType == JOptionPane.QUESTION_MESSAGE) {
            icon = UIManager.getIcon("OptionPane.questionIcon");
        } else if (messageType == JOptionPane.INFORMATION_MESSAGE) {
            icon = UIManager.getIcon("OptionPane.informationIcon");
        }

        JPanel messagePanel = new JPanel(new BorderLayout(12, 0));
        messagePanel.setOpaque(false);
        if (icon != null) {
            messagePanel.add(new JLabel(icon), BorderLayout.WEST);
        }

        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(null);
        messagePanel.add(text, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        final int[] result = { JOptionPane.NO_OPTION };
        btnYes.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });
        btnNo.addActionListener(e -> {
            result[0] = JOptionPane.NO_OPTION;
            dialog.dispose();
        });

        buttons.add(btnNo);
        buttons.add(btnYes);

        content.add(messagePanel, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getRootPane().setDefaultButton(btnYes);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }

    /**
     * Carga el contexto de sesión de forma simplificada. MainForm ya validó
     * todo, aquí solo obtenemos los IDs.
     */
    private void cargarContextoSesionSimplificado() {
        UserSession session = UserSession.getInstance();

        // Obtener datos del usuario (ya validados en MainForm)
        this.idUsuarioActual = session.getCurrentUser().getIdUsuario();

        // ═══════════════════════════════════════════════════════════════════
        // Obtener ID de caja - Prioridad: asociada > activa > fallback bodega
        // ═══════════════════════════════════════════════════════════════════
        // Opción 1: Caja asociada a la sesión (via asociarCaja())
        Integer idCajaAsociada = session.getIdCajaAsociada();
        if (idCajaAsociada != null && idCajaAsociada > 0) {
            this.idCajaActiva = idCajaAsociada;
        } else {
            // Opción 2: Caja activa (si existe)
            Integer idCajaSesion = session.getIdCajaActiva();
            if (idCajaSesion != null && idCajaSesion > 0) {
                this.idCajaActiva = idCajaSesion;
            } else {
                // Opción 3: Fallback - obtener caja de la bodega
                try {
                    Integer idBodega = session.getIdBodegaUsuario();
                    ServiceCaja sc = new ServiceCaja();
                    ModelCaja caja = sc.obtenerCajaPorBodega(idBodega);
                    if (caja != null) {
                        this.idCajaActiva = caja.getIdCaja();
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(generarVentaFor1.class.getName())
                            .log(Level.WARNING, "Error obteniendo caja de bodega", ex);
                }
            }
        }

    }

    /**
     * Oculta paneles que no se necesitan hasta que haya datos
     */
    private void ocultarPanalesIniciales() {
        cbxTipoPago.setVisible(false);
        jLabel12.setVisible(false);
        txtNumeroNotaCredito.setVisible(false);
        lblNotaCredito.setVisible(false);
        btnValidar.setVisible(false);
        panelInfoCliente.setVisible(false);
        panelInfoProd.setVisible(false);
    }

    /**
     * Carga los vendedores activos desde la base de datos de forma asíncrona.
     * Usa SwingWorker para no bloquear el EDT durante la carga.
     */
    private void cargarVendedores() {
        // Inicializar combo si no existe
        if (cbxVendedor == null) {
            cbxVendedor = new javax.swing.JComboBox<>();
        }

        cbxVendedor.removeAllItems();

        // Agregar placeholder mientras carga
        Usuario placeholder = new Usuario();
        placeholder.setId(0);
        placeholder.setNombre("Cargando vendedores...");
        cbxVendedor.addItem(placeholder);
        cbxVendedor.setEnabled(false);

        // Estilo FlatLaf
        cbxVendedor.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);

        // Cargar datos en segundo plano
        new SwingWorker<List<Usuario>, Void>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                return usuarioDAO.obtenerUsuariosActivos();
            }

            @Override
            protected void done() {
                try {
                    List<Usuario> usuarios = get();

                    cbxVendedor.removeAllItems();

                    // Agregar item por defecto
                    Usuario placeholderFinal = new Usuario();
                    placeholderFinal.setId(0);
                    placeholderFinal.setNombre("-- Seleccionar Vendedor --");
                    cbxVendedor.addItem(placeholderFinal);

                    // Agregar usuarios cargados
                    for (Usuario usuario : usuarios) {
                        cbxVendedor.addItem(usuario);
                    }

                    cbxVendedor.setEnabled(true);

                    // Configurar listener para actualizar vendedor seleccionado
                    cbxVendedor.addActionListener(evt -> {
                        Usuario selected = (Usuario) cbxVendedor.getSelectedItem();
                        if (selected != null && selected.getId() > 0) {
                            vendedorSeleccionado = selected;
                        } else {
                            vendedorSeleccionado = null;
                        }
                    });

                    // Preseleccionar vendedor actual
                    seleccionarVendedorActual();

                } catch (Exception e) {
                    Logger.getLogger(generarVentaFor1.class.getName())
                            .log(Level.WARNING, "Error cargando vendedores", e);
                    Toast.show(generarVentaFor1.this, Toast.Type.ERROR,
                            "Error al cargar vendedores");
                    cbxVendedor.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Obtiene el ID del vendedor seleccionado para registrar en la venta.
     * 
     * @return ID del vendedor seleccionado, o 0 si no hay selección
     */
    private int getIdVendedorSeleccionado() {
        if (vendedorSeleccionado != null && vendedorSeleccionado.getId() > 0) {
            return vendedorSeleccionado.getId();
        }
        // Fallback: usar ID del usuario actual si no hay vendedor seleccionado
        return idUsuarioActual;
    }

    /**
     * Inicializa el panel de vendedor con su label y combobox
     */
    private void inicializarPanelVendedor() {
        // Crear label si no existe
        if (lblVendedor == null) {
            lblVendedor = new JLabel("Vendedor");
            lblVendedor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }

        // Crear combo si no existe
        if (cbxVendedor == null) {
            cbxVendedor = new javax.swing.JComboBox<>();
        }

        // Cargar vendedores
        cargarVendedores();

        // Preseleccionar el usuario actual como vendedor por defecto
        seleccionarVendedorActual();
    }

    /**
     * Selecciona el usuario actual como vendedor por defecto
     */
    private void seleccionarVendedorActual() {
        if (cbxVendedor != null && idUsuarioActual > 0) {
            for (int i = 0; i < cbxVendedor.getItemCount(); i++) {
                Usuario u = cbxVendedor.getItemAt(i);
                if (u != null && u.getId() == idUsuarioActual) {
                    cbxVendedor.setSelectedIndex(i);
                    vendedorSeleccionado = u;
                    break;
                }
            }
        }
    }

    private boolean cargarContextoSesionYCaja() {
        // ====================================================================
        // 1. VALIDAR SESIÓN ACTIVA
        // ====================================================================
        var session = UserSession.getInstance();
        var user = session.getCurrentUser();

        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay usuario autenticado. Vuelva a iniciar sesión.",
                    "Sesión requerida",
                    JOptionPane.ERROR_MESSAGE);
            cerrarFormulario();
            return false;
        }

        this.idUsuarioActual = user.getIdUsuario();

        // ====================================================================
        // 2. VALIDAR BODEGA ASIGNADA
        // ====================================================================
        Integer idBodegaUsuario = session.getIdBodegaUsuario();
        if (idBodegaUsuario == null) {
            JOptionPane.showMessageDialog(this,
                    "Su usuario no tiene una bodega asignada.\n\n"
                            + "Contacte al administrador para asignar bodega y caja.",
                    "Configuración incompleta",
                    JOptionPane.WARNING_MESSAGE);
            cerrarFormulario();
            return false;
        }

        // ====================================================================
        // 3. OBTENER CAJA ASOCIADA (sin validar estado todavía)
        // ====================================================================
        try {
            ServiceCaja sc = new ServiceCaja();
            ModelCaja cajaAsociada = sc.obtenerCajaPorBodega(idBodegaUsuario);

            if (cajaAsociada == null) {
                JOptionPane.showMessageDialog(this,
                        "Su bodega no tiene una caja registrada.",
                        "Caja no configurada",
                        JOptionPane.WARNING_MESSAGE);
                cerrarFormulario();
                return false;
            }

            if (cajaAsociada.getIdCaja() == null || cajaAsociada.getIdCaja() <= 0) {
                JOptionPane.showMessageDialog(this,
                        "La caja tiene un ID inválido.",
                        "Error de sistema",
                        JOptionPane.ERROR_MESSAGE);
                cerrarFormulario();
                return false;
            }

            this.idCajaActiva = cajaAsociada.getIdCaja();
            // Verificar stock en segundo plano
            SwingUtilities.invokeLater(() -> verificarStockInicialBodega(idBodegaUsuario));

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error de base de datos al obtener la caja:\n" + ex.getMessage(),
                    "BD - Caja",
                    JOptionPane.ERROR_MESSAGE);
            cerrarFormulario();
            return false;
        }

        // configurarPanelEstadoCaja();
        return true;
    }

    private void initComboTallaListener() {
        comboTalla.addActionListener(evt -> {
            // FILTRO 1: Si estamos cargando, ignorar
            if (isLoadingTallas) {
                return;
            }

            // FILTRO 2: Si no hay producto seleccionado, ignorar
            if (selectedProduct == null) {
                return;
            }

            // FILTRO 3: Si es el placeholder "Seleccionar talla", ignorar
            int tallaIndex = comboTalla.getSelectedIndex();
            if (tallaIndex <= 0) {
                return;
            }

            // OBTENER VALORES
            String tallaSeleccionada = (String) comboTalla.getSelectedItem();
            if (tallaSeleccionada == null || tallaSeleccionada.isEmpty()) {
                return;
            }

            // FILTRO 4: Si la talla NO cambió, no hacer nada
            if (tallaSeleccionada.equals(previousTallaSelected)) {
                return;
            }
            previousTallaSelected = tallaSeleccionada;

            // OBTENER COLOR ACTUAL
            int colorIndex = comboColor.getSelectedIndex();
            String colorSeleccionado = null;
            if (colorIndex > 0) {
                colorSeleccionado = (String) comboColor.getSelectedItem();
            }

            // SOLO actualizar datos si AMBOS están seleccionados
            if (colorSeleccionado != null && !colorSeleccionado.isEmpty()) {

                actualizarDatosProductoCompleto(tallaSeleccionada, colorSeleccionado);
            } else {
            }
        });
    }

    private void initComboColorListener() {
        comboColor.addActionListener(evt -> {
            // FILTRO 1: Si estamos cargando, ignorar
            if (isLoadingColores) {
                return;
            }

            // FILTRO 2: Si no hay producto seleccionado, ignorar
            if (selectedProduct == null) {
                return;
            }

            // FILTRO 3: Si es el placeholder "Seleccionar color", ignorar
            int colorIndex = comboColor.getSelectedIndex();
            if (colorIndex <= 0) {
                return;
            }

            // OBTENER VALORES
            String colorSeleccionado = (String) comboColor.getSelectedItem();
            if (colorSeleccionado == null || colorSeleccionado.isEmpty()) {
                return;
            }

            // FILTRO 4: Si el color NO cambió, no hacer nada
            if (colorSeleccionado.equals(previousColorSelected)) {
                return;
            }
            previousColorSelected = colorSeleccionado;

            // OBTENER TALLA ACTUAL
            int tallaIndex = comboTalla.getSelectedIndex();
            String tallaSeleccionada = null;
            if (tallaIndex > 0) {
                tallaSeleccionada = (String) comboTalla.getSelectedItem();
            }

            // SOLO actualizar datos si AMBOS están seleccionados
            if (tallaSeleccionada != null && !tallaSeleccionada.isEmpty()) {

                actualizarDatosProductoCompleto(tallaSeleccionada, colorSeleccionado);
            } else {
            }
        });
    }

    private void actualizarDatosProductoCompleto(String tallaSeleccionada, String colorSeleccionado) {

        // PREVENIR RECURSIÓN
        if (isUpdatingProductData) {
            return;
        }

        isUpdatingProductData = true;

        try {
            // VALIDACIÓN PREVIA
            if (selectedProduct == null) {
                System.err.println("ERROR: No hay producto seleccionado");
                return;
            }

            if (tallaSeleccionada == null || tallaSeleccionada.isEmpty()
                    || colorSeleccionado == null || colorSeleccionado.isEmpty()) {
                System.err.println("ERROR: Talla o color vacíos");
                return;
            }

            // OBTENER CONTEXTO
            Integer idBodegaUsuario = UserSession.getInstance().getIdBodegaUsuario();
            if (idBodegaUsuario == null || idBodegaUsuario == 0) {
                System.err.println("ERROR: ID bodega inválido");
                Toast.show(this, Toast.Type.ERROR, "Error: ID bodega inválido");
                return;
            }

            int idProducto = Integer.parseInt(selectedProduct.getId_prod());

            // OBTENER TIPO DE VENTA SELECCIONADO
            String tipoVenta = "par"; // Default
            if (cbxTipo.getSelectedItem() != null) {
                Object tipoItem = cbxTipo.getSelectedItem();
                if (tipoItem != null) {
                    tipoVenta = tipoItem.toString().toLowerCase().trim();
                }
            }

            // BUSCAR VARIANTE EN INVENTARIO
            ModelInventarioBodega inventario = serviceInventarioBodega.obtenerVariantePorTallaColor(
                    idBodegaUsuario, // bodega
                    idProducto, // producto
                    tallaSeleccionada, // talla EXACTA como está en BD
                    colorSeleccionado // color EXACTO como está en BD
            );

            // VALIDAR RESULTADO
            if (inventario == null) {
                System.err.println("ERROR: NO SE ENCONTRÓ VARIANTE");
                System.err.println("   Esto significa:");
                System.err.println("   1. La combinación (talla + color) no existe");
                System.err.println("   2. No hay stock en bodega " + idBodegaUsuario);
                System.err.println("   3. El registro está inactivo (activo = 0)");

                Toast.show(this, Toast.Type.WARNING,
                        "No hay stock disponible de esta combinación en tu bodega");

                txtStock.setText("0 pares disponibles");
                return;
            }

            // VARIANTE ENCONTRADA

            // DETERMINAR STOCK SEGÚN TIPO DE VENTA
            int stockMostrar = 0;
            String unidad = "pares";

            if ("caja".equalsIgnoreCase(tipoVenta)) {
                stockMostrar = inventario.getStockDisponibleCajas();
                unidad = "cajas";
            } else {
                // Default: par
                stockMostrar = inventario.getStockDisponiblePares();
                unidad = "pares";
            }

            // ACTUALIZAR selectedProduct CON DATOS DEL INVENTARIO
            selectedProduct.setIdVariante(inventario.getIdVariante());
            selectedProduct.setTalla(tallaSeleccionada);
            selectedProduct.setColor(colorSeleccionado);
            selectedProduct.setStockPorPares(inventario.getStockDisponiblePares());
            selectedProduct.setStockPorCajas(inventario.getStockDisponibleCajas());
            selectedProduct.setPrecioVenta(inventario.getPrecioVenta());

            // ACTUALIZAR UI - MOSTRAR STOCK SEGÚN TIPO
            txtStock.setText(stockMostrar + " " + unidad + " disponibles");
            txtPrecio.setText(inventario.getPrecioVenta().toString());
            txtEAN.setText(inventario.getEan() != null ? inventario.getEan() : "N/A");

            // CAMBIAR COLOR DEL TEXTO SEGÚN STOCK
            if (stockMostrar <= 0) {
                txtStock.setForeground(Color.RED);
            } else if (stockMostrar < 5) {
                txtStock.setForeground(Color.ORANGE);
            } else {
                txtStock.setForeground(new Color(34, 139, 34)); // Verde
            }
            Toast.show(this, Toast.Type.SUCCESS, "Combinación válida - Stock: " + stockMostrar + " " + unidad);

        } catch (NumberFormatException e) {
            System.err.println("ERROR: Error parseando ID: " + e.getMessage());
            Toast.show(this, Toast.Type.ERROR, "Error: ID de producto inválido");
        } catch (SQLException e) {
            System.err.println("ERROR: Error SQL: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error de base de datos: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERROR: Error general: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error inesperado: " + e.getMessage());
        } finally {
            isUpdatingProductData = false;
        }
    }

    /**
     * Maneja errores durante la inicialización
     */
    private void manejarErrorInicializacion(Exception e) {
        System.err.println("ERROR: Error en inicialización: " + e.getMessage());
        e.printStackTrace();
        estadoCaja = EstadoCaja.ERROR;
        Toast.show(this, Toast.Type.ERROR, "Error al inicializar: " + e.getMessage());
    }

    // ==================== MÉTODO CORREGIDO: limpiarFormulario ====================
    /**
     * Cierra este formulario y regresa al dashboard CORRECCIÓN: Este es el
     * método que faltaba
     */
    private void regresarAlDashboard() {
        try {
            // Limpiar recursos
            limpiarRecursos();

            // Navegar al dashboard
            FormDashboard dashboard = new FormDashboard();
            Application.showForm(dashboard);
        } catch (Exception e) {
            System.err.println("ERROR: Error regresando al dashboard: " + e.getMessage());
            e.printStackTrace();

            JOptionPane.showMessageDialog(this,
                    "Error al cargar el dashboard:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cierra el formulario actual sin navegar
     */
    private void cerrarFormulario() {
        try {
            limpiarRecursos();
            regresarAlDashboard();
        } catch (Exception e) {
            System.err.println("ERROR: Error cerrando formulario: " + e.getMessage());
        }
    }

    /**
     * Limpia recursos antes de cerrar
     */
    private void limpiarRecursos() {
        // Detener monitor de inactividad
        if (monitorInactividad != null) {
            monitorInactividad.detener();
            monitorInactividad = null;
        }

        // Cerrar menús
        if (menu != null && menu.isVisible()) {
            menu.setVisible(false);
        }
        if (menu1 != null && menu1.isVisible()) {
            menu1.setVisible(false);
        }
    }

    private void verificarStockInicialBodega(int idBodega) {
        String q = "SELECT COUNT(*) FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante = pv.id_variante "
                + "WHERE ib.id_bodega = ? AND ib.activo = 1 AND pv.disponible = 1 AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(q)) {
            ps.setInt(1, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                if (rs.next()) {
                    count = rs.getInt(1);
                }
                if (count == 0) {
                    if (SwingUtilities.getWindowAncestor(this) != null) {
                        Toast.show(this, Toast.Type.WARNING, "La bodega no tiene stock suficiente para facturar");
                        abrirDash();

                    } else {
                        JOptionPane.showMessageDialog(null, "La bodega no tiene stock suficiente para facturar",
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void cargarOrdenReservaEnFormulario(int idOrden) {
        try {
            origenOrdenReserva = true;
            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
            model.setRowCount(0);

            OrdenReservaService ordenService = new OrdenReservaService();
            java.util.List<OrdenReservaDetalle> detalles = ordenService.obtenerDetallesOrden(idOrden);

            if (detalles == null || detalles.isEmpty()) {
                Toast.show(this, Toast.Type.ERROR, "La orden OR-" + idOrden + " no tiene productos");
                return;
            }

            asegurarColumnaIdVariante();

            for (OrdenReservaDetalle d : detalles) {
                String codigo = obtenerEanVarianteConValidacion(d.getIdVariante());
                if (codigo == null || codigo.trim().isEmpty()) {
                    codigo = obtenerCodigoBarrasVariante(d.getIdVariante());
                }
                if (codigo == null || codigo.trim().isEmpty()) {
                    System.err.println("ERROR: Sin EAN/código de barras para variante " + d.getIdVariante());
                    continue;
                }

                String nombre = d.getNombreProducto() != null ? d.getNombreProducto()
                        : (d.getCodigoProducto() != null ? d.getCodigoProducto() : ("VAR-" + d.getIdVariante()));
                String color = d.getColor();
                String talla = d.getTalla();
                if ((color != null && !color.isEmpty()) || (talla != null && !talla.isEmpty())) {
                    String colorTxt = color != null && !color.isEmpty() ? color : "";
                    String tallaTxt = talla != null && !talla.isEmpty() ? talla : "";
                    if (!colorTxt.isEmpty() && !tallaTxt.isEmpty()) {
                        nombre += " (" + colorTxt + " - " + tallaTxt + ")";
                    } else {
                        nombre += " (" + (colorTxt.isEmpty() ? tallaTxt : colorTxt) + ")";
                    }
                }

                String tipo = "par";
                Integer cantidad = d.getCantidad();
                java.math.BigDecimal precioUnitario = java.math.BigDecimal.valueOf(d.getPrecio());
                java.math.BigDecimal subtotal = precioUnitario.multiply(java.math.BigDecimal.valueOf(cantidad));
                int idVariante = d.getIdVariante();
                int idProducto = obtenerIdProductoPorVariante(idVariante);

                // Agregar fila con las 11 columnas correctas
                model.addRow(new Object[] {
                        codigo, // 0: Código
                        nombre, // 1: Producto
                        tipo, // 2: Tipo
                        cantidad, // 3: Cantidad
                        precioUnitario, // 4: Precio
                        java.math.BigDecimal.ZERO, // 5: Descuento
                        subtotal, // 6: Subtotal
                        null, // 7: Acción
                        null, // 8: id_detalle
                        idVariante, // 9: id_variante
                        idProducto // 10: id_producto
                });
            }

            actualizarTotales();
            seleccionarEnCombo(cbxTipoPago, "efectivo");
            Toast.show(this, Toast.Type.SUCCESS, "Orden OR-" + idOrden + " cargada en el formulario");
        } catch (Exception e) {
            Toast.show(this, Toast.Type.ERROR, "Error al cargar la orden: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void asegurarColumnaIdVariante() {
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

        // Si no existe la columna, la creamos al final
        if (model.findColumn("id_variante") == -1) {
            model.addColumn("id_variante");
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(null, i, model.getColumnCount() - 1);
            }
        }
        // Ocultar columnas "id" e "id_variante" si existen
        ocultarColumnaPorNombre("id");
        ocultarColumnaPorNombre("id_variante");
    }

    private void ocultarColumnaPorNombre(String colName) {
        int idx = ((DefaultTableModel) tablaProductos.getModel()).findColumn(colName);
        if (idx >= 0 && idx < tablaProductos.getColumnModel().getColumnCount()) {
            tablaProductos.getColumnModel().getColumn(idx).setMinWidth(0);
            tablaProductos.getColumnModel().getColumn(idx).setMaxWidth(0);
            tablaProductos.getColumnModel().getColumn(idx).setPreferredWidth(0);
        }
    }

    private void agregarProductosConfigurados(
            java.util.List<raven.componentes.BuscadorProductoDialog.ProductoConfigurado> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        asegurarColumnaIdVariante();
        int agregados = 0;
        for (raven.componentes.BuscadorProductoDialog.ProductoConfigurado pc : items) {
            raven.componentes.BuscadorProductoDialog.ProductoSeleccionado p = pc.getProducto();
            if (p == null)
                continue;
            String tipo = pc.getTipo() != null ? pc.getTipo().toLowerCase().trim() : "par";
            int cantidad = Math.max(1, pc.getCantidad());
            if ("par".equals(tipo)) {
                if (cantidad > p.getStockPares()) {
                    Toast.show(this, Toast.Type.WARNING, "Sin stock suficiente en pares para " + p.getNombre());
                    continue;
                }
            } else if ("caja".equals(tipo)) {
                if (cantidad > p.getStockCajas()) {
                    Toast.show(this, Toast.Type.WARNING, "Sin stock suficiente en cajas para " + p.getNombre());
                    continue;
                }
            } else {
                tipo = "par";
            }
            String nombre = p.getNombre();
            String color = p.getColor();
            String talla = p.getTalla();
            if ((color != null && !color.isEmpty()) || (talla != null && !talla.isEmpty())) {
                String colorTxt = color != null && !color.isEmpty() ? color : "";
                String tallaTxt = talla != null && !talla.isEmpty() ? talla : "";
                if (!colorTxt.isEmpty() && !tallaTxt.isEmpty()) {
                    nombre += " (" + colorTxt + " - " + tallaTxt + ")";
                } else {
                    nombre += " (" + (colorTxt.isEmpty() ? tallaTxt : colorTxt) + ")";
                }
            }
            java.math.BigDecimal precioUnitario = p.getPrecioVenta() != null ? p.getPrecioVenta()
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal subtotal = precioUnitario.multiply(java.math.BigDecimal.valueOf(cantidad));
            String codigo = p.getIdentificador();
            int idVariante = p.getIdVariante();
            int idProducto = p.getIdProducto();
            // Agregar fila con las 11 columnas correctas
            model.addRow(new Object[] {
                    codigo, // 0: Código
                    nombre, // 1: Producto
                    tipo, // 2: Tipo
                    cantidad, // 3: Cantidad
                    precioUnitario, // 4: Precio
                    java.math.BigDecimal.ZERO, // 5: Descuento
                    subtotal, // 6: Subtotal
                    null, // 7: Acción
                    null, // 8: id_detalle
                    idVariante, // 9: id_variante
                    idProducto // 10: id_producto
            });
            agregados++;
        }
        if (agregados > 0) {
            actualizarTotales();
            Toast.show(this, Toast.Type.SUCCESS, "Se agregaron " + agregados + " producto(s) a la venta");
        }
    }

    public void abrirDash() throws SQLException {
        regresarAlDashboard();
    }

    /**
     * NUEVO: Valida la sesión antes de procesar una venta.
     *
     * @return true si la sesión es válida, false en caso contrario
     */
    private boolean validarSesionAntesDeVenta() {
        // 1. Verificar que haya sesión activa
        if (!UserSession.getInstance().isLoggedIn()) {
            JOptionPane.showMessageDialog(this,
                    "Su sesión ha expirado.\n"
                            + "Por favor, inicie sesión nuevamente.",
                    "Sesión Expirada",
                    JOptionPane.WARNING_MESSAGE);

            Application.logout();
            return false;
        }

        // 2. Verificar que el token sea válido
        String token = UserSession.getInstance().getSessionToken();
        if (token == null) {
            JOptionPane.showMessageDialog(this,
                    "Token de sesión inválido.\n"
                            + "Por favor, inicie sesión nuevamente.",
                    "Error de Sesión",
                    JOptionPane.ERROR_MESSAGE);

            Application.logout();
            return false;
        }

        // 3. Verificar que la caja esté abierta
        if (!cajaAbierta || movimientoActual == null) {
            Toast.show(this, Toast.Type.ERROR,
                    "No hay una caja abierta. Debe abrir una caja para generar ventas.");

            JButton btnAbrir = createDialogButton(
                    "Abrir caja",
                    svgIcon("raven/icon/svg/caja.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(10, 132, 255), new Color(64, 156, 255), new Color(0, 119, 230)));
            JButton btnCancelar = createDialogButton(
                    "Cancelar",
                    svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

            int opcion = showConfirmYesNoDialog(
                    this,
                    "¿Desea abrir una caja ahora?",
                    "Caja No Abierta",
                    JOptionPane.QUESTION_MESSAGE,
                    btnAbrir,
                    btnCancelar);

            if (opcion == JOptionPane.YES_OPTION) {
                try {
                    solicitarAperturaNuevaCaja();
                } catch (SQLException ex) {
                    Toast.show(this, Toast.Type.ERROR, "Error al abrir caja: " + ex.getMessage());
                    return false;
                }
            }

            return false;
        }

        // 4. Verificar que el movimiento de caja sigue abierto en BD
        try {
            ServiceCajaMovimiento serviceCaja = new ServiceCajaMovimiento();
            ModelCajaMovimiento verificacion = serviceCaja.obtenerMovimientoPorId(
                    movimientoActual.getIdMovimiento());

            if (verificacion == null || !verificacion.estaAbierto()) {
                Toast.show(this, Toast.Type.ERROR,
                        "El movimiento de caja fue cerrado. Debe abrir una nueva caja.");

                cajaAbierta = false;
                movimientoActual = null;
                actualizarPanelEstadoCaja();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Error verificando movimiento: " + e.getMessage());
            Toast.show(this, Toast.Type.ERROR,
                    "Error verificando estado de caja: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Inicia el monitor de inactividad de la caja.
     *
     * YA IMPLEMENTADO CORRECTAMENTE No requiere cambios -
     * MonitorInactividadCaja ahora usa validación real
     */
    private void iniciarMonitorInactividad() {
        if (monitorInactividad != null) {
            monitorInactividad.detener();
        }

        monitorInactividad = MonitorInactividadCaja.getInstance();

        // SOLUCIÓN: Obtener el frame de forma más robusta
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);

        // Si no se puede obtener, buscar el frame activo de la aplicación
        if (frame == null) {
            for (Frame f : Frame.getFrames()) {
                if (f.isVisible() && f instanceof JFrame) {
                    frame = (JFrame) f;
                    break;
                }
            }
        }

        // Si aún es null, usar el frame principal de la aplicación
        if (frame == null) {
            // Ajusta esto según tu clase Application
            frame = raven.application.Application.app; // o como se llame tu frame principal
        }

        if (frame == null) {
            System.err.println("ERROR: No se pudo obtener ningún JFrame para el monitor");
            return;
        }
        monitorInactividad.iniciar(frame, new MonitorInactividadCaja.BloqueoCallback() {
            @Override
            public void onBloqueado() {
                deshabilitarInterfaz();
                actualizarPanelEstadoCaja();
            }

            @Override
            public void onDesbloqueado() {
                habilitarInterfaz();
                actualizarPanelEstadoCaja();
            }

            @Override
            public void onAdvertenciaBloqueo(int segundosRestantes) {
                java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(generarVentaFor1.this);

                if (win != null && generarVentaFor1.this.isShowing()) {
                    /*
                     * Toast.show(generarVentaFor1.this, Toast.Type.WARNING,
                     * "Sesión se bloqueará en " + segundosRestantes + " segundos por inactividad");
                     */
                } else {
                    /*
                     * javax.swing.JOptionPane.showMessageDialog(win,
                     * "Sesión se bloqueará en " + segundosRestantes + " segundos por inactividad",
                     * "Advertencia",
                     * javax.swing.JOptionPane.WARNING_MESSAGE);
                     */
                }
            }
        });
    }

    /**
     * Configura el panel visual de estado de caja.
     */
    private void configurarPanelEstadoCaja() {
        // Verificar que el panel existe (creado por NetBeans)
        if (panelEstadoCaj == null) {
            System.err.println("WARN: panelEstadoCaj es null - verificar initComponents()");
            return;
        }

        // ====================================================================
        // CONFIGURAR LAYOUT DEL PANEL (si no está configurado en NetBeans)
        // ====================================================================
        panelEstadoCaj.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelEstadoCaj.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // ====================================================================
        // CONFIGURAR LISTENERS DE BOTONES (solo una vez)
        // ====================================================================
        if (!listenersConfigurados) {
            configurarListenersBotones();
            listenersConfigurados = true;
        }

        // ====================================================================
        // ACTUALIZAR ESTADO VISUAL
        // ====================================================================
        actualizarPanelEstadoCaja();
    }

    private void configurarListenersBotones() {
        // ====================================================================
        // BOTÓN BLOQUEAR - Listener de acción
        // ====================================================================
        if (btnBloquear != null) {
            // Remover listeners previos para evitar duplicados
            for (ActionListener al : btnBloquear.getActionListeners()) {
                btnBloquear.removeActionListener(al);
            }

            btnBloquear.addActionListener(e -> bloquearCajaManualmente());
            // Configurar estilos base (SIN putClientProperty para GradientButton)
            btnBloquear.setFocusPainted(false);
            btnBloquear.setCursor(new Cursor(Cursor.HAND_CURSOR));
            // REMOVIDO: btnBloquear.putClientProperty(FlatClientProperties.STYLE,
            // "arc:8");
            // GradientButton ya tiene su propio estilo de bordes redondeados
            btnBloquear.setToolTipText("Bloquear caja manualmente");
        }
        if (btnGastos != null) {
            // BUENA PRÁCTICA: Remover listeners previos para evitar duplicados
            // Esto previene memory leaks y comportamientos inesperados
            for (ActionListener al : btnGastos.getActionListeners()) {
                btnGastos.removeActionListener(al);
            }

            // PATRÓN: Lambda expression para código más limpio y legible
            btnGastos.addActionListener(e -> abrirDialogoGastos());

            // Configurar estilos base (SIN putClientProperty para GradientButton)
            btnGastos.setFocusPainted(false);
            btnGastos.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnGastos.setToolTipText("Registrar gastos de caja");
        }

        // ====================================================================
        // BOTÓN CERRAR CAJA - Listener de acción
        // ====================================================================
        if (btnCerrarCaja != null) {
            // Remover listeners previos para evitar duplicados
            for (ActionListener al : btnCerrarCaja.getActionListeners()) {
                btnCerrarCaja.removeActionListener(al);
            }

            btnCerrarCaja.addActionListener(e -> {
                try {
                    solicitarCierreCaja();
                } catch (SQLException ex) {
                    Logger.getLogger(generarVentaFor1.class.getName())
                            .log(Level.SEVERE, "Error al cerrar caja", ex);
                }
            });

            // Configurar estilos base (SIN putClientProperty para GradientButton)
            btnCerrarCaja.setFocusPainted(false);
            btnCerrarCaja.setCursor(new Cursor(Cursor.HAND_CURSOR));
            // REMOVIDO: btnCerrarCaja.putClientProperty(FlatClientProperties.STYLE,
            // "arc:8");
            btnCerrarCaja.setToolTipText("Cerrar caja y finalizar turno");
        }
    }

    private void actualizarPanelEstadoCaja() {
        if (panelEstadoCaj == null) {
            return;
        }

        if (cajaAbierta && movimientoActual != null) {
            // ====================================================================
            // CAJA ABIERTA - Mostrar estado completo
            // ====================================================================
            actualizarEstadoCajaActiva();

            // Mostrar todos los componentes
            mostrarComponentesCajaActiva(true);

        } else {
            // ====================================================================
            // CAJA NO ABIERTA - Mostrar advertencia
            // ====================================================================
            actualizarEstadoCajaCerrada();

            // Ocultar botones, mostrar solo estado
            mostrarComponentesCajaActiva(false);
        }

        panelEstadoCaj.revalidate();
        panelEstadoCaj.repaint();
    }

    /**
     * Abre el diálogo de gastos de forma OPTIMIZADA.
     * 
     * OPTIMIZACIÓN: Muestra feedback visual inmediato (cursor de espera)
     * y deshabilita el botón para evitar doble clic. Luego usa invokeLater
     * para procesar la creación del diálogo en el siguiente ciclo del EDT,
     * permitiendo que el cursor se actualice visualmente.
     */
    private void abrirDialogoGastos() {
        // 1. VALIDAR que hay movimiento abierto (rápido, sin BD)
        if (!cajaAbierta || movimientoActual == null) {
            Toast.show(this, Toast.Type.WARNING,
                    "No hay un movimiento de caja abierto. Debe abrir una caja primero.");
            return;
        }

        final int idMovimiento = movimientoActual.getIdMovimiento();
        final java.awt.Frame parentFrame = obtenerFramePadre();
        final String username = UserSession.getInstance().getCurrentUser().getUsername();

        // 2. Mostrar feedback visual INMEDIATO
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        // Deshabilitar botón para evitar doble clic
        if (btnGastos != null) {
            btnGastos.setEnabled(false);
        }
        // 3. Usar invokeLater para permitir que el cursor se actualice ANTES de crear
        // el diálogo
        // Esto da retroalimentación visual inmediata al usuario
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // Crear el diálogo (creación de componentes Swing en EDT - correcto)
                gastosForm dialogoGastos = new gastosForm(parentFrame, true);
                dialogoGastos.cargarGastosDelMovimiento(idMovimiento);

                // Actualizar título
                dialogoGastos.getTxtMovimientoCaja()
                        .setText(String.format("Caja #1 - Mov #%d | Usuario: %s",
                                idMovimiento, username));

                long elapsed = System.currentTimeMillis() - startTime;
                // Restaurar cursor ANTES de mostrar el diálogo modal
                setCursor(java.awt.Cursor.getDefaultCursor());
                if (btnGastos != null) {
                    btnGastos.setEnabled(true);
                }

                // Centrar y mostrar (esto bloquea hasta que se cierre el diálogo)
                dialogoGastos.setLocationRelativeTo(parentFrame);
                dialogoGastos.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                setCursor(java.awt.Cursor.getDefaultCursor());
                if (btnGastos != null) {
                    btnGastos.setEnabled(true);
                }
                javax.swing.JOptionPane.showMessageDialog(
                        generarVentaFor1.this,
                        "Error al abrir gastos: " + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Obtiene el Frame padre del componente actual.
     * 
     * BUENA PRÁCTICA: Método utilitario reutilizable
     * para obtener el contenedor de nivel superior.
     * 
     * @return Frame padre o null si no existe
     */
    private java.awt.Frame obtenerFramePadre() {
        // SwingUtilities proporciona método seguro para esto
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);

        if (window instanceof java.awt.Frame) {
            return (java.awt.Frame) window;
        }

        // Fallback: retornar null (el diálogo se centrará en pantalla)
        return null;
    }

    /**
     * Actualiza los componentes cuando la caja está activa.
     */
    private void actualizarEstadoCajaActiva() {
        boolean estaBloqueada = monitorInactividad != null && monitorInactividad.estaBloqueado();

        // ====================================================================
        // ACTUALIZAR txtEstadoCaja (o txtEstado según tu diseño)
        // ====================================================================
        actualizarLabelEstado(estaBloqueada);

        // ====================================================================
        // ACTUALIZAR txtMovimientoCaja
        // ====================================================================
        if (txtMovimientoCaja != null && movimientoActual != null) {
            txtMovimientoCaja.setText(String.format("Mov. #%d - %s",
                    movimientoActual.getIdMovimiento(),
                    movimientoActual.getNombreCaja()));
            txtMovimientoCaja.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            txtMovimientoCaja.setToolTipText("Movimiento actual de caja");
            txtMovimientoCaja.setVisible(true);
        }

        // ====================================================================
        // ACTUALIZAR btnBloquear
        // ====================================================================
        actualizarBotonBloqueo(estaBloqueada);

        // ====================================================================
        // ACTUALIZAR btnCerrarCaja
        // ====================================================================
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setText("Cerrar Caja");
            btnCerrarCaja.setEnabled(true);
            btnCerrarCaja.setVisible(true);
        }
    }

    /**
     * Actualiza el label de estado según si está bloqueada o activa.
     * MEJORADO: Estilos modernos con FlatLaf y badges estilizados
     * 
     * @param estaBloqueada true si la caja está bloqueada
     */
    private void actualizarLabelEstado(boolean estaBloqueada) {
        // Detener animación anterior si existe
        detenerAnimacionPulso();

        JLabel labelEstado = txtEstadoCaja;
        if (labelEstado == null) {
            return;
        }
        // Configurar fuente moderna
        labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelEstado.setHorizontalAlignment(SwingConstants.CENTER);
        labelEstado.setOpaque(true);

        if (estaBloqueada) {
            // ========================================
            // ESTADO BLOQUEADA - Badge Rojo
            // ========================================
            labelEstado.setText("  BLOQUEADA  ");
            labelEstado.setForeground(Color.WHITE);
            labelEstado.setBackground(new Color(220, 53, 69)); // Rojo Bootstrap
            labelEstado.setIcon(FontIcon.of(FontAwesomeSolid.LOCK, 14, Color.WHITE));

            // Estilo FlatLaf para badge redondeado
            labelEstado.putClientProperty(FlatClientProperties.STYLE,
                    "arc:20;" +
                            "border:4,12,4,12;" +
                            "background:rgb(220,53,69)");

            labelEstado.setToolTipText("Caja bloqueada - Ingrese contraseña para desbloquear");

        } else {
            // ========================================
            // ESTADO ACTIVA - Badge Verde con pulso
            // ========================================
            labelEstado.setText("  ACTIVA  ");
            labelEstado.setForeground(Color.WHITE);
            labelEstado.setBackground(new Color(40, 167, 69)); // Verde Bootstrap
            labelEstado.setIcon(FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 14, Color.WHITE));

            // Estilo FlatLaf para badge redondeado
            labelEstado.putClientProperty(FlatClientProperties.STYLE,
                    "arc:20;" +
                            "border:4,12,4,12;" +
                            "background:rgb(40,167,69)");

            labelEstado.setToolTipText("Caja operativa - Lista para vender");

            // Iniciar animación de pulso sutil
            iniciarAnimacionPulso(labelEstado);
        }

        labelEstado.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        labelEstado.setVisible(true);
    }

    /**
     * Inicia la animación de pulso para el estado activo.
     * 
     * @param labelEstado el JLabel a animar
     */
    private void iniciarAnimacionPulso(JLabel labelEstado) {
        pulsoAnimacionTimer = new javax.swing.Timer(300_000, new ActionListener() {
            private boolean visible = true;

            @Override
            public void actionPerformed(ActionEvent e) {
                // Verificar que sigue activa (no bloqueada)
                if (monitorInactividad != null && !monitorInactividad.estaBloqueado()) {
                    visible = !visible;
                    labelEstado.setText("  ACTIVA  ");
                } else {
                    detenerAnimacionPulso();
                }
            }
        });
        pulsoAnimacionTimer.start();
    }

    /**
     * Detiene la animación de pulso de forma segura.
     */
    private void detenerAnimacionPulso() {
        if (pulsoAnimacionTimer != null && pulsoAnimacionTimer.isRunning()) {
            pulsoAnimacionTimer.stop();
            pulsoAnimacionTimer = null;
        }
    }

    /**
     * Actualiza el estado del botón de bloqueo.
     * 
     * @param estaBloqueada true si la caja está bloqueada
     */
    private void actualizarBotonBloqueo(boolean estaBloqueada) {
        if (btnBloquear == null) {
            return;
        }

        if (estaBloqueada) {
            btnBloquear.setText("Bloqueada");
            btnBloquear.setEnabled(false);
            // GradientButton puede tener métodos específicos para colores
            // Si no, usar setBackground o propiedades de FlatLaf
            try {

            } catch (Exception e) {
                // Si no tiene métodos de gradiente, usar background normal
                btnBloquear.setBackground(new Color(150, 150, 150));
            }
        } else {
            btnBloquear.setText("Bloquear");
            btnBloquear.setEnabled(true);
            try {
            } catch (Exception e) {
                btnBloquear.setBackground(new Color(255, 149, 0));
            }
        }

        btnBloquear.setForeground(Color.WHITE);
        btnBloquear.setVisible(true);
    }

    /**
     * Muestra u oculta los componentes según el estado de la caja.
     * 
     * @param mostrar true para mostrar, false para ocultar
     */
    private void mostrarComponentesCajaActiva(boolean mostrar) {
        if (txtMovimientoCaja != null) {
            txtMovimientoCaja.setVisible(mostrar);
        }
        if (btnBloquear != null) {
            btnBloquear.setVisible(mostrar);
        }
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setVisible(mostrar);
        }
    }

    /**
     * Actualiza el estado cuando la caja está cerrada.
     */
    private void actualizarEstadoCajaCerrada() {
        JLabel labelEstado = txtEstadoCaja;

        if (labelEstado != null) {
            // ========================================
            // ESTADO NO ABIERTA - Badge Naranja/Advertencia
            // ========================================
            labelEstado.setText("  SIN ABRIR  ");
            labelEstado.setForeground(Color.WHITE);
            labelEstado.setBackground(new Color(255, 193, 7)); // Amarillo Bootstrap
            labelEstado.setOpaque(true);
            labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 13));
            labelEstado.setHorizontalAlignment(SwingConstants.CENTER);
            labelEstado.setIcon(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 14, new Color(33, 37, 41)));
            labelEstado.setForeground(new Color(33, 37, 41)); // Texto oscuro para contraste

            // Estilo FlatLaf para badge redondeado
            labelEstado.putClientProperty(FlatClientProperties.STYLE,
                    "arc:20;" +
                            "border:4,12,4,12;" +
                            "background:rgb(255,193,7)");

            labelEstado.setToolTipText("Debe abrir una caja para operar");
            labelEstado.setVisible(true);
        }

        // Ocultar componentes no necesarios
        if (txtMovimientoCaja != null) {
            txtMovimientoCaja.setVisible(false);
        }
    }

    /**
     * Bloquea la caja manualmente.
     * 
     * <p>
     * REFACTORIZADO: Usa los componentes existentes de NetBeans.
     * </p>
     */
    // ============================================================================
    // MÉTODO CORREGIDO - bloquearCajaManualmente()
    // ============================================================================
    // Copia exacta del generarVentaFor2 que SÍ funciona
    // ============================================================================

    // ============================================================================
    // SOLUCIÓN PARA generarVentaFor1.java
    // ============================================================================
    // Reemplazar el método bloquearCajaManualmente() con esta versión simplificada
    // que es IDÉNTICA a la de generarVentaFor2 (que SÍ funciona)
    // ============================================================================

    /**
     * Bloquea la caja manualmente CON BLUR.
     * 
     * VERSIÓN CORREGIDA - Idéntica a generarVentaFor2
     */
    // generarVentaFor2 - FUNCIONA
    private void bloquearCajaManualmente() {
        JButton btnSi = createDialogButton(
                "Sí, bloquear",
                svgIcon("raven/icon/svg/dashboard/alert-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(255, 159, 10), new Color(255, 183, 77), new Color(204, 122, 0)));
        JButton btnNo = createDialogButton(
                "Cancelar",
                svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

        int confirmacion = new BlurDialog.Builder(this)
                .title("Bloquear Caja")
                .message("¿Está seguro de bloquear la caja?\n\n"
                        + "Deberá ingresar su contraseña para desbloquearla.")
                .icon(JOptionPane.QUESTION_MESSAGE)
                .customButtons(new Object[] { btnSi, btnNo }, btnNo)
                .showConfirmDialog();

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        if (monitorInactividad != null) {
            // PRIMERO forzar el bloqueo (muestra el diálogo)
            monitorInactividad.forzarBloqueo();

            // El callback onBloqueado() se encargará de:
            // - deshabilitarInterfaz()
            // - actualizarPanelEstadoCaja()
        } else {
            System.err.println("ERROR: monitorInactividad es NULL!");
            Toast.show(this, Toast.Type.ERROR, "Error: Monitor no inicializado");
        }
    }

    /**
     * Fuerza el bloqueo inmediato de la interfaz.
     */

    /**
     * Actualiza los componentes visuales para reflejar el estado de bloqueo.
     * 
     * <p>
     * Usa los componentes existentes de NetBeans.
     * </p>
     */
    private void actualizarComponentesParaBloqueo() {
        // ====================================================================
        // ACTUALIZAR LABEL DE ESTADO - Estilo Badge Moderno
        // ====================================================================
        detenerAnimacionPulso();

        if (txtEstadoCaja != null) {
            txtEstadoCaja.setText("  BLOQUEADA  ");
            txtEstadoCaja.setForeground(Color.WHITE);
            txtEstadoCaja.setBackground(new Color(220, 53, 69));
            txtEstadoCaja.setOpaque(true);
            txtEstadoCaja.setIcon(FontIcon.of(FontAwesomeSolid.LOCK, 14, Color.WHITE));
            txtEstadoCaja.putClientProperty(FlatClientProperties.STYLE,
                    "arc:20;" +
                            "border:4,12,4,12;" +
                            "background:rgb(220,53,69)");
        }

        // ====================================================================
        // ACTUALIZAR BOTÓN DE BLOQUEO
        // ====================================================================
        if (btnBloquear != null) {
            btnBloquear.setText("Bloqueada");
            btnBloquear.setEnabled(false);
            // Para GradientButton, cambiar colores directamente si tiene los métodos
            // o simplemente dejarlo deshabilitado (se verá gris automáticamente)
        }

        // ====================================================================
        // REFRESCAR PANEL
        // ====================================================================
        if (panelEstadoCaj != null) {
            panelEstadoCaj.revalidate();
            panelEstadoCaj.repaint();
        }
    }

    /**
     * Actualiza los componentes visuales después de un desbloqueo exitoso.
     * 
     * <p>
     * Este método debe ser llamado desde el callback onDesbloqueado()
     * del MonitorInactividadCaja.
     * </p>
     */
    public void actualizarComponentesDespuesDesbloqueo() {
        SwingUtilities.invokeLater(() -> {
            // ====================================================================
            // ACTUALIZAR LABEL DE ESTADO - Estilo Badge Moderno
            // ====================================================================
            if (txtEstadoCaja != null) {
                txtEstadoCaja.setText("  ACTIVA  ");
                txtEstadoCaja.setForeground(Color.WHITE);
                txtEstadoCaja.setBackground(new Color(40, 167, 69));
                txtEstadoCaja.setOpaque(true);
                txtEstadoCaja.setIcon(FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 14, Color.WHITE));
                txtEstadoCaja.putClientProperty(FlatClientProperties.STYLE,
                        "arc:20;" +
                                "border:4,12,4,12;" +
                                "background:rgb(40,167,69)");
                iniciarAnimacionPulso(txtEstadoCaja);
            }

            // ====================================================================
            // ACTUALIZAR BOTÓN DE BLOQUEO
            // ====================================================================
            if (btnBloquear != null) {
                btnBloquear.setText("Bloquear");
                btnBloquear.setEnabled(true);
            }

            // ====================================================================
            // HABILITAR INTERFAZ
            // ====================================================================
            habilitarInterfaz();

            // ====================================================================
            // REFRESCAR PANEL
            // ====================================================================
            if (panelEstadoCaj != null) {
                panelEstadoCaj.revalidate();
                panelEstadoCaj.repaint();
            }
        });
    }

    /**
     * Actualiza la información visual del estado de la caja.
     */
    /**
     * Actualiza la información visual del estado de la caja.
     *
     * MODIFICADO: Agrega botón para bloqueo manual y prueba de monitor.
     */

    /**
     * Solicita el cierre de caja al usuario.
     */
    private void solicitarCierreCaja() throws SQLException {
        // Verificar que hay una caja abierta
        if (!cajaAbierta || movimientoActual == null) {
            Toast.show(this, Toast.Type.WARNING, "No hay una caja abierta para cerrar");
            return;
        }

        // Confirmar cierre
        JButton btnCerrar = createDialogButton(
                "Sí, cerrar",
                svgIcon("raven/icon/svg/dashboard/alert-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));
        JButton btnCancelar = createDialogButton(
                "Cancelar",
                svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

        int confirmacion = showConfirmYesNoDialog(
                this,
                "¿Está seguro de cerrar la caja?\n\n"
                        + "Movimiento #" + movimientoActual.getIdMovimiento() + "\n"
                        + "Esto finalizará su turno actual.",
                "Confirmar Cierre de Caja",
                JOptionPane.QUESTION_MESSAGE,
                btnCerrar,
                btnCancelar);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        // Detener monitor de inactividad
        if (monitorInactividad != null) {
            monitorInactividad.detener();
        }

        // Mostrar diálogo de cierre
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);

        CierreCajaDialog dialogo = new CierreCajaDialog(frame, movimientoActual);
        dialogo.setVisible(true);

        // Verificar si el cierre fue exitoso
        if (dialogo.isCierreExitoso()) {
            cajaAbierta = false;
            movimientoActual = null;
            actualizarPanelEstadoCaja();

            Toast.show(this, Toast.Type.SUCCESS, "Caja cerrada exitosamente");

            // Preguntar si desea abrir nuevamente
            JButton btnAbrir = createDialogButton(
                    "Abrir caja",
                    svgIcon("raven/icon/svg/caja.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(10, 132, 255), new Color(64, 156, 255), new Color(0, 119, 230)));
            JButton btnSalir = createDialogButton(
                    "Salir",
                    svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

            int opcion = showConfirmYesNoDialog(
                    this,
                    "¿Desea abrir nuevamente la caja para un nuevo turno?",
                    "Caja Cerrada",
                    JOptionPane.QUESTION_MESSAGE,
                    btnAbrir,
                    btnSalir);

            if (opcion == JOptionPane.YES_OPTION) {
                // Solicitar apertura de nueva caja
                solicitarAperturaNuevaCaja();
            } else {
                // Cerrar el formulario
                abrirDash();

            }
        }
    }

    /**
     * Solicita la apertura de una nueva caja para un nuevo turno.
     * Se invoca después de cerrar la caja exitosamente.
     */
    private void solicitarAperturaNuevaCaja() throws SQLException {
        try {
            // Obtener la caja actual
            ModelCaja caja = obtenerCajaActual();
            if (caja == null) {
                Toast.show(this, Toast.Type.ERROR, "No se pudo obtener la información de la caja");
                abrirDash();
                return;
            }

            // Obtener datos del usuario actual
            UserSession session = UserSession.getInstance();
            ModelUser currentUser = session.getCurrentUser();
            if (currentUser == null) {
                Toast.show(this, Toast.Type.ERROR, "No hay usuario autenticado");
                Application.logout();
                return;
            }

            int idUsuario = currentUser.getIdUsuario();
            String nombreUsuario = currentUser.getUsername();

            // Obtener el JFrame padre
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);

            // Crear callback para manejar el resultado de la apertura
            AperturaCajaDialog.AperturaCajaCallback callback = new AperturaCajaDialog.AperturaCajaCallback() {
                @Override
                public void onAperturaExitosa(ModelCajaMovimiento movimiento) {
                    // Actualizar el estado de la caja
                    movimientoActual = movimiento;
                    cajaAbierta = true;
                    estadoCaja = EstadoCaja.ABIERTA;

                    // Iniciar monitor de inactividad
                    iniciarMonitorInactividad();

                    // Actualizar panel de estado
                    actualizarPanelEstadoCaja();

                    // Habilitar la interfaz
                    habilitarInterfaz();

                    Toast.show(generarVentaFor1.this, Toast.Type.SUCCESS,
                            "Caja abierta exitosamente - Movimiento #" + movimiento.getIdMovimiento());
                }

                @Override
                public void onAperturaCancelada() {
                    // El usuario canceló la apertura, ir al Dashboard
                    Toast.show(generarVentaFor1.this, Toast.Type.INFO, "Apertura de caja cancelada");
                    try {
                        abrirDash();
                    } catch (SQLException ex) {
                        System.getLogger(generarVentaFor1.class.getName()).log(System.Logger.Level.ERROR, (String) null,
                                ex);
                    }
                }
            };

            // Mostrar el diálogo de apertura
            AperturaCajaDialog dialogo = new AperturaCajaDialog(
                    frame, caja, idUsuario, nombreUsuario, callback);
            dialogo.setVisible(true);

        } catch (Exception e) {
            System.err.println("ERROR: Error solicitando apertura de caja: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al abrir caja: " + e.getMessage());
            abrirDash();
        }
    }

    /**
     * Muestra el modal de cambio de talla
     */
    private void mostrarModalCambioTalla() {
        raven.application.form.other.ModalCambioTalla modal = new raven.application.form.other.ModalCambioTalla(
                SwingUtilities.getWindowAncestor(this), true);
        modal.setVisible(true);
    }

    /**
     * Aplica efecto de vibración al diálogo
     */
    private void aplicarEfectoShake(JDialog dialogo) {
        Point location = dialogo.getLocation();
        final int[] step = new int[] { 0 };
        javax.swing.Timer timer = new javax.swing.Timer(50, e -> {
            int s = step[0]++;
            if (s >= 6) {
                dialogo.setLocation(location);
                ((javax.swing.Timer) e.getSource()).stop();
                return;
            }
            int dx = (s % 2 == 0) ? 10 : -10;
            dialogo.setLocation(location.x + dx, location.y);
        });
        timer.setRepeats(true);
        timer.start();
    }

    /**
     * Registra el evento de desbloqueo manual en la base de datos
     */
    private void registrarEventoDesbloqueo(int idUsuario) {
        // Puedes implementar esto según tu estructura de base de datos
        // Ejemplo de implementación:
        /*
         * try {
         * String sql =
         * "INSERT INTO logs_desbloqueo (id_usuario, fecha_desbloqueo, tipo) VALUES (?, NOW(), 'manual')"
         * ;
         * try (Connection conn = conexion.getInstance().createConnection();
         * PreparedStatement pstmt = conn.prepareStatement(sql)) {
         * pstmt.setInt(1, idUsuario);
         * pstmt.executeUpdate();
         * }
         * } catch (SQLException e) {
         * System.err.println("Error registrando evento de desbloqueo: " +
         * e.getMessage());
         * }
         */
    }

    /**
     * Deshabilita la interfaz durante el bloqueo.
     */
    private void deshabilitarInterfaz() {
        // Deshabilitar componentes principales
        txtIngresarCodigo.setEnabled(false);
        txtCliente.setEnabled(false);
        btnSeleccionarCliente.setEnabled(false);
        btnAgregarProd.setEnabled(false);
        btngenerarVenta.setEnabled(false);
        // btnGenerarCotizacion.setEnabled(false);

        // Deshabilitar combos
        comboTalla.setEnabled(false);
        comboColor.setEnabled(false);
        cbxTipo.setEnabled(false);

        // Deshabilitar tabla
        tablaProductos.setEnabled(false);
    }

    /**
     * Habilita la interfaz tras el desbloqueo.
     */
    private void habilitarInterfaz() {
        // Habilitar componentes principales
        txtIngresarCodigo.setEnabled(true);
        txtCliente.setEnabled(selectClient == null);
        btnSeleccionarCliente.setEnabled(selectClient == null);
        btnAgregarProd.setEnabled(true);
        btngenerarVenta.setEnabled(true);
        // btnGenerarCotizacion.setEnabled(true);

        // Habilitar combos
        comboTalla.setEnabled(true);
        comboColor.setEnabled(true);
        cbxTipo.setEnabled(true);

        // Habilitar tabla
        tablaProductos.setEnabled(true);
    }

    private void configureTableListener() {
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int fila = e.getFirstRow();
                    int columna = e.getColumn();

                    if (columna == COL_PRECIO || columna == COL_DESCUENTO) {
                        try {
                            int cantidad = Integer.parseInt(model.getValueAt(fila, COL_CANTIDAD).toString());
                            BigDecimal precio = new BigDecimal(tablaProductos.getValueAt(fila, COL_PRECIO).toString());
                            BigDecimal descuento = new BigDecimal(
                                    tablaProductos.getValueAt(fila, COL_DESCUENTO).toString());
                            String tipo = tablaProductos.getValueAt(fila, COL_TIPO).toString();

                            BigDecimal subtotal;
                            if (tipo.equals("caja")) {
                                subtotal = precio.multiply(BigDecimal.valueOf(cantidad * 24));
                            } else {
                                subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
                            }

                            subtotal = subtotal.subtract(descuento);

                            tablaProductos.setValueAt(subtotal, fila, COL_SUBTOTAL);
                            actualizarTotales();
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(null, "Valor inválido", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
    }

    /**
     * Inicializa el panel de múltiples medios de pago.
     *
     * MODIFICADO: Configura automáticamente el total de la venta
     */
    private void inicializarPanelMultiplesPagos() {
        // Crear panel de múltiples pagos
        panelMultiplesPagos = new PanelMultiplesPagos();

        // Listener para deshabilitar botón cuando el pago está completo
        panelMultiplesPagos.setOnSaldoChangeListener(saldo -> {
            // El botón se controla ahora de forma centralizada en actualizarTotales()
            // y en el botón Aceptar del diálogo para evitar duplicados.
            if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
                btnConfigurarPagos.setToolTipText("Pagos completados - Pulse Aceptar para confirmar");
            } else {
                btnConfigurarPagos.setToolTipText("Configurar múltiples medios de pago");
            }
        });

        agregarPanelMultiplesPagosAlFormulario();
        // Configurar listener para actualizar el total automáticamente
        // cuando cambie la tabla de productos
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        model.addTableModelListener(e -> {
            // Actualizar total en el panel de pagos cuando cambien los productos
            BigDecimal totalActual = parsearTextoMonetario(txtTotal.getText());
            panelMultiplesPagos.setTotalVenta(totalActual);
        });
    }

    /**
     * Agrega el panel de múltiples pagos al formulario.
     *
     * Este método coloca el panel en un JDialog modal que se abre al hacer clic
     * en un botón "Configurar Pagos".
     */
    private void agregarPanelMultiplesPagosAlFormulario() {
        btnConfigurarPagos.setToolTipText("Configurar múltiples medios de pago");
        // Icono para el botón
        btnConfigurarPagos.setIcon(FontIcon.of(FontAwesomeSolid.MONEY_BILL_WAVE, 16,
                UIManager.getColor("Button.foreground")));
        // Estilo FlatLaf
        btnConfigurarPagos.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        // Evento: Abrir diálogo de múltiples pagos
        btnConfigurarPagos.addActionListener(e -> abrirDialogoMultiplesPagos());
        // Agregar botón al panelMetod (ajusta según tu layout)
        // Ejemplo: Agregarlo después del combo de tipo de pago
        GroupLayout layout = (GroupLayout) panelMetod.getLayout();
        // Alternativa simple: Agregar al final del panel
        panelMetod.add(btnConfigurarPagos);
        panelMetod.revalidate();
    }

    /**
     * Abre el diálogo para configurar múltiples medios de pago.
     *
     * MODIFICADO: Actualiza automáticamente los totales al cerrar
     */
    private void abrirDialogoMultiplesPagos() {
        // Validar que haya productos en la venta
        if (tablaProductos.getRowCount() == 0) {
            Toast.show(this, Toast.Type.WARNING,
                    "Agregue productos a la venta antes de configurar los pagos");
            return;
        }

        // Obtener el total de la venta
        BigDecimal totalVenta = parsearTextoMonetario(txtTotal.getText());

        // Configurar el panel con el total
        panelMultiplesPagos.setTotalVenta(totalVenta);

        // Crear diálogo modal
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Configurar Medios de Pago",
                true // Modal
        );

        // Configurar diálogo
        dialog.setLayout(new BorderLayout());
        dialog.add(panelMultiplesPagos, BorderLayout.CENTER);
        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setIcon(svgIcon("raven/icon/svg/dashboard/check-circle.svg", 18, Color.WHITE));
        btnAceptar.setIconTextGap(8);
        btnAceptar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnAceptar.putClientProperty(FlatClientProperties.STYLE,
                dialogButtonStyle(new Color(40, 167, 69), new Color(52, 199, 89), new Color(30, 126, 52)));

        btnAceptar.addActionListener(e -> {
            if (panelMultiplesPagos.validarFormulario()) {
                dialog.dispose();
                actualizarTotalesConMultiplesPagos();

                // NUEVO: Actualizar observaciones automáticamente
                agregarDetallesPagoAObservaciones();
                manejarPagoCompletado();

                // LÓGICA "A PRUEBA DE TONTOS": Si el pago ya está completo,
                // inhabilitar el botón para evitar que se repitan o modifiquen sin necesidad.
                // Se re-habilitará automáticamente en actualizarTotales() si cambia el total de
                // la venta.
                BigDecimal pendienteFinal = panelMultiplesPagos.getSaldoPendiente();
                if (pendienteFinal.compareTo(BigDecimal.ZERO) <= 0) {
                    btnConfigurarPagos.setEnabled(false);
                    btnConfigurarPagos.setToolTipText(
                            "Pagos configurados (Saldo cubierto). Si añade más productos se habilitará nuevamente.");
                }

                Toast.show(this, Toast.Type.SUCCESS,
                        "Pagos configurados correctamente");
            }
        });

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setIcon(svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE));
        btnCancelar.setIconTextGap(8);
        btnCancelar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnCancelar.putClientProperty(FlatClientProperties.STYLE,
                dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));

        btnCancelar.addActionListener(e -> {
            JButton btnDescartar = createDialogButton(
                    "Descartar",
                    svgIcon("raven/icon/svg/dashboard/minus-circle.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));
            JButton btnVolver = createDialogButton(
                    "Volver",
                    svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

            int confirm = showConfirmYesNoDialog(
                    dialog,
                    "¿Descartar los pagos configurados?",
                    "Confirmar",
                    JOptionPane.QUESTION_MESSAGE,
                    btnDescartar,
                    btnVolver);
            if (confirm == JOptionPane.YES_OPTION) {
                panelMultiplesPagos.reset();
                dialog.dispose();
            }
        });

        panelBotones.add(btnCancelar);
        panelBotones.add(btnAceptar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        // Tamaño y posición
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // Mostrar diálogo
        dialog.setVisible(true);
    }

    /**
     * Actualiza los labels de totales con los pagos configurados.
     */
    private void actualizarTotalesConMultiplesPagos() {
        BigDecimal totalVenta = parsearTextoMonetario(txtTotal.getText());
        BigDecimal saldoPendiente = panelMultiplesPagos.getSaldoPendiente();

        // Actualizar label de pendiente
        txtPendiente.setText(MoneyFormatter.format(saldoPendiente));

        // Cambiar color según estado
        if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            txtPendiente.setForeground(new Color(0, 153, 51)); // Verde
            btngenerarVenta.setEnabled(true);
        } else if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
            txtPendiente.setForeground(Color.BLUE); // Azul (vuelto)
            btngenerarVenta.setEnabled(true);
        } else {
            txtPendiente.setForeground(new Color(255, 69, 58)); // Rojo
            btngenerarVenta.setEnabled(false);
        }

        // Actualizar observaciones con detalles de pago
        agregarDetallesPagoAObservaciones();
    }

    /**
     * Agrega detalles de los medios de pago a las observaciones.
     */
    private void agregarDetallesPagoAObservaciones() {
        List<ModelMedioPago> mediosPago = panelMultiplesPagos.getMediosPago();

        if (mediosPago.isEmpty()) {
            return;
        }

        StringBuilder obs = new StringBuilder();
        obs.append("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        obs.append("MEDIOS DE PAGO APLICADOS\n");
        obs.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        BigDecimal totalPagado = BigDecimal.ZERO;

        for (int i = 0; i < mediosPago.size(); i++) {
            ModelMedioPago medio = mediosPago.get(i);
            obs.append(String.format("%d. %s: %s\n",
                    i + 1,
                    medio.getTipo().getDescripcion(),
                    MoneyFormatter.format(medio.getMonto())));

            if (medio.getNumeroReferencia() != null
                    && !medio.getNumeroReferencia().isEmpty()) {
                obs.append(String.format("   Ref: %s\n", medio.getNumeroReferencia()));
            }

            if (medio.getObservaciones() != null
                    && !medio.getObservaciones().isEmpty()) {
                obs.append(String.format("   Obs: %s\n", medio.getObservaciones()));
            }

            obs.append("\n");
            totalPagado = totalPagado.add(medio.getMonto());
        }

        obs.append(String.format("TOTAL PAGADO: %s\n",
                MoneyFormatter.format(totalPagado)));

        // Agregar a las observaciones existentes
        String obsActuales = txtObservaciones.getText();
        txtObservaciones.setText(obsActuales + obs.toString());
    }

    /**
     * Actualiza los totales de la venta con formato monetario correcto
     *
     * MODIFICADO: También actualiza el panel de múltiples pagos
     */
    private void actualizarTotales() {
        BigDecimal subTotalGeneral = BigDecimal.ZERO;
        BigDecimal descuentoProductos = BigDecimal.ZERO;

        // 1. CALCULAR TOTALES DE LA TABLA
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            BigDecimal subtotalFila = new BigDecimal(
                    model.getValueAt(i, COL_SUBTOTAL).toString());
            subTotalGeneral = subTotalGeneral.add(subtotalFila);

            BigDecimal descuentoFila = new BigDecimal(
                    model.getValueAt(i, COL_DESCUENTO).toString());
            descuentoProductos = descuentoProductos.add(descuentoFila);
        }

        // 2. CALCULAR TOTAL
        BigDecimal totalInicial = subTotalGeneral;

        // 3. ACTUALIZAR LABELS
        txtSubTotal.setText(MoneyFormatter.format(subTotalGeneral));
        txtDescuento.setText(MoneyFormatter.format(descuentoProductos));
        txtTotal.setText(MoneyFormatter.format(totalInicial));

        // NUEVO: Actualizar panel de múltiples pagos
        if (panelMultiplesPagos != null) {
            panelMultiplesPagos.setTotalVenta(totalInicial);
        }

        // 4. CALCULAR PENDIENTE
        BigDecimal pendiente;
        if (panelMultiplesPagos != null && panelMultiplesPagos.tieneMediosPago()) {
            pendiente = panelMultiplesPagos.getSaldoPendiente();
        } else {
            pendiente = totalInicial;
        }

        txtPendiente.setText(MoneyFormatter.format(pendiente));

        // 5. VALIDAR ESTADO DEL BOTÓN
        validarEstadoBotonVenta(pendiente);

        // LÓGICA "A PRUEBA DE TONTOS": Si hay saldo pendiente (o vuelto pendiente),
        // habilitar configuración de pagos para que el usuario pueda cuadrar.
        if (pendiente.compareTo(BigDecimal.ZERO) != 0) {
            btnConfigurarPagos.setEnabled(true);
            btnConfigurarPagos.setToolTipText("Configurar múltiples medios de pago");
        }
    }

    /**
     * Formatea un BigDecimal como moneda colombiana Utility method para
     * reutilización
     */
    private String formatearMoneda(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);
        return formato.format(monto);
    }

    /**
     * Valida y actualiza el estado del botón de generar venta según el saldo
     * pendiente
     *
     * Principio Interface Segregation: Método específico para una validación
     */
    private void validarEstadoBotonVenta(BigDecimal pendiente) {
        boolean hayPendiente = pendiente.compareTo(BigDecimal.ZERO) > 0;

        if (hayPendiente) {
            // HAY SALDO PENDIENTE - Deshabilitar botón
            btngenerarVenta.setEnabled(false);
            btngenerarVenta.setBackground(new Color(200, 200, 200)); // Gris
            btngenerarVenta.setToolTipText(
                    "Complete el pago antes de generar la venta. "
                            + "Pendiente: " + MoneyFormatter.format(pendiente));
        } else {
            // PAGO COMPLETO - Habilitar botón
            btngenerarVenta.setEnabled(true);
            btngenerarVenta.setBackground(new Color(0, 122, 255)); // Azul
            btngenerarVenta.setToolTipText("Generar venta");
        }

        // Forzar actualización visual
        btngenerarVenta.revalidate();
        btngenerarVenta.repaint();
    }

    /**
     * Construye el texto de observaciones incluyendo detalles de pago Principio
     * Single Responsibility: Una función, una tarea específica
     */
    private String construirObservaciones() {
        StringBuilder observaciones = new StringBuilder();

        // 1. Agregar observaciones manuales del usuario (si existen)
        String obsUsuario = txtObservaciones.getText().trim();
        if (!obsUsuario.isEmpty()) {
            observaciones.append(obsUsuario);
            observaciones.append("\n\n");
        }

        // 2. Agregar separador visual
        observaciones.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        observaciones.append("DETALLES DE PAGO\n");
        observaciones.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // 3. Parsear totales actuales
        BigDecimal subtotal = MoneyFormatter.parse(txtSubTotal.getText());
        BigDecimal descuento = MoneyFormatter.parse(txtDescuento.getText());
        BigDecimal total = MoneyFormatter.parse(txtTotal.getText());
        BigDecimal pendiente = MoneyFormatter.parse(txtPendiente.getText());

        // 4. Agregar información de totales
        observaciones.append(String.format("Subtotal:    %s\n",
                MoneyFormatter.format(subtotal)));
        observaciones.append(String.format("Descuento:   %s\n",
                MoneyFormatter.format(descuento)));
        observaciones.append(String.format("Total:       %s\n\n",
                MoneyFormatter.format(total)));

        // 5. Agregar información de nota de crédito (si aplica)
        if (notaCreditoSeleccionada != null) {
            BigDecimal montoNota = notaCreditoSeleccionada.getSaldoDisponible()
                    .min(total);

            observaciones.append("NOTA DE CRÉDITO APLICADA\n");
            observaciones.append(String.format("   Número: %s\n",
                    notaCreditoSeleccionada.getNumeroNotaCredito()));
            observaciones.append(String.format("   Monto aplicado: %s\n\n",
                    MoneyFormatter.format(montoNota)));
        }

        // 6. Agregar método de pago adicional (si es pago mixto)
        String tipoPago = (String) cbxTipoPago.getSelectedItem();
        if (tipoPago != null && !tipoPago.equals("Seleccionar")) {
            if (notaCreditoSeleccionada != null && pendiente.compareTo(BigDecimal.ZERO) > 0) {
                observaciones.append("PAGO ADICIONAL\n");
                observaciones.append(String.format("   Método: %s\n",
                        tipoPago.toUpperCase()));
                observaciones.append(String.format("   Monto: %s\n\n",
                        MoneyFormatter.format(pendiente)));
            } else if (notaCreditoSeleccionada == null) {
                observaciones.append("MÉTODO DE PAGO\n");
                observaciones.append(String.format("   %s\n",
                        tipoPago.toUpperCase()));
                observaciones.append(String.format("   Monto: %s\n\n",
                        MoneyFormatter.format(total)));
            }
        }

        // 7. Estado del pago
        String estado = (String) cbxEstadoPago.getSelectedItem();
        if (estado != null && !estado.equals("Seleccionar")) {
            observaciones.append(String.format("Estado: %s\n",
                    estado.toUpperCase()));
        }

        // 8. Saldo pendiente (si existe)
        if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
            observaciones.append("\nSALDO PENDIENTE\n");
            observaciones.append(String.format("   %s\n",
                    MoneyFormatter.format(pendiente)));
        } else {
            observaciones.append("\nPAGO COMPLETADO\n");
        }

        return observaciones.toString();
    }

    /**
     * Renderer y Editor para botón de eliminar en la tabla
     */
    /**
     * Renderer para el botón de eliminar en la tabla
     *
     * Patrón Strategy: Proporciona una estrategia de renderizado específica
     * para la columna de acciones.
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);

            // Configurar icono de eliminar
            setIcon(FontIcon.of(FontAwesomeSolid.TRASH_ALT, 14, Color.WHITE));
            setText("");

            // Estilos del botón
            setBackground(new Color(255, 69, 58)); // Rojo de iOS/macOS
            setForeground(Color.WHITE);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Tooltip
            setToolTipText("Eliminar producto");

            // Estilo FlatLaf
            putClientProperty(FlatClientProperties.STYLE, ""
                    + "arc:8;"
                    + "borderWidth:0;");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // Cambiar color en hover (se maneja en el editor)
            if (isSelected) {
                setBackground(new Color(255, 59, 48)); // Rojo más oscuro
            } else {
                setBackground(new Color(255, 69, 58));
            }

            return this;
        }
    }

    /**
     * Editor para manejar clicks en el botón de eliminar
     *
     * Patrón Observer: Escucha eventos de click y notifica al sistema para
     * eliminar la fila correspondiente.
     */
    /**
     * Editor para manejar clicks en el botón de eliminar
     *
     * CORRECCIÓN: Detiene la edición ANTES de eliminar la fila para evitar
     * ArrayIndexOutOfBoundsException
     */
    private class ButtonEditor extends DefaultCellEditor {

        private JButton button;
        private int filaActual;
        private boolean clicked;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);

            button = new JButton();
            button.setOpaque(true);
            button.setIcon(FontIcon.of(FontAwesomeSolid.TRASH_ALT, 14, Color.WHITE));
            button.setText("");
            button.setBackground(new Color(255, 69, 58));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setToolTipText("Eliminar producto");

            button.putClientProperty(FlatClientProperties.STYLE, ""
                    + "arc:8;"
                    + "borderWidth:0;");

            // ========================================
            // CRÍTICO: Listener para el click
            // ========================================
            button.addActionListener(e -> {
                // IMPORTANTE: Detener edición INMEDIATAMENTE
                // para evitar que JTable intente actualizar la celda
                fireEditingCanceled(); // ← Usar Cancel en lugar de Stopped

                // Ejecutar eliminación en el Event Dispatch Thread
                // DESPUÉS de que la edición se haya cancelado
                SwingUtilities.invokeLater(() -> {
                    eliminarProductoDeFila(filaActual);
                });
            });

            // Efecto hover
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    button.setBackground(new Color(255, 59, 48)); // Más oscuro
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    button.setBackground(new Color(255, 69, 58)); // Original
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {

            filaActual = row;
            clicked = false;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            // No hacer nada aquí, la eliminación se maneja en el ActionListener
            return "";
        }

        @Override
        public boolean stopCellEditing() {
            // Cancelar la edición limpiamente
            clicked = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingCanceled() {
            super.fireEditingCanceled();
        }
    }

    /**
     * Elimina un producto de la tabla de ventas
     *
     * @param fila Índice de la fila a eliminar
     */
    /**
     * Elimina un producto de la tabla de ventas
     *
     * @param fila Índice de la fila a eliminar
     */
    private void eliminarProductoDeFila(int fila) {
        try {
            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

            // ========================================
            // VALIDACIÓN CRÍTICA: Verificar índice
            // ========================================
            if (fila < 0 || fila >= model.getRowCount()) {
                System.err
                        .println("ERROR: Índice de fila inválido: " + fila + " (Total filas: " + model.getRowCount()
                                + ")");
                return;
            }

            // Obtener información del producto antes de eliminarlo
            String nombreProducto = "";
            try {
                nombreProducto = (String) model.getValueAt(fila, COL_PRODUCTO);
            } catch (Exception e) {
                System.err.println("WARN: Error obteniendo nombre del producto: " + e.getMessage());
                nombreProducto = "Producto desconocido";
            }

            Integer idDetalle = null;

            // En modo edición, guardar el id_detalle para eliminarlo de BD
            if (modoEdicion && model.getColumnCount() > COL_ID_DETALLE) {
                try {
                    Object idDetalleObj = model.getValueAt(fila, COL_ID_DETALLE);
                    if (idDetalleObj != null && !idDetalleObj.toString().isEmpty()) {
                        idDetalle = Integer.parseInt(idDetalleObj.toString());
                    }
                } catch (Exception e) {
                    System.err.println("WARN: Error obteniendo id_detalle: " + e.getMessage());
                }
            }

            // ========================================
            // CONFIRMAR ELIMINACIÓN
            // ========================================
            JButton btnEliminar = createDialogButton(
                    "Eliminar",
                    svgIcon("raven/icon/svg/dashboard/minus-circle.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));
            JButton btnCancelar = createDialogButton(
                    "Cancelar",
                    svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                    dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

            int confirmacion = showConfirmYesNoDialog(
                    this,
                    "¿Está seguro de eliminar este producto?\n\n" + nombreProducto,
                    "Confirmar Eliminación",
                    JOptionPane.QUESTION_MESSAGE,
                    btnEliminar,
                    btnCancelar);

            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }

            // Si estamos en modo edición y el detalle existe en BD, agregarlo a la lista
            if (modoEdicion && idDetalle != null) {
                productosEliminados.add(idDetalle);
            }

            // ========================================
            // ELIMINAR LA FILA DE LA TABLA
            // ========================================
            model.removeRow(fila);
            // ========================================
            // ACTUALIZAR INTERFAZ
            // ========================================
            // Actualizar totales
            actualizarTotales();

            // Actualizar contador en el label
            actualizarContadorProductos();

            // Notificar al usuario
            Toast.show(this, Toast.Type.SUCCESS, "Producto eliminado correctamente");

        } catch (Exception e) {
            System.err.println("ERROR: Error eliminando producto: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al eliminar el producto: " + e.getMessage());
        }
    }

    /**
     * Actualiza el contador de productos en el label
     */
    private void actualizarContadorProductos() {
        try {
            // OBTENER CANTIDAD DE FILAS EN LA TABLA
            int totalProductos = tablaProductos.getRowCount();
            // VERIFICAR QUE jLabel14 EXISTE
            if (jLabel14 == null) {
                System.err.println("ERROR: jLabel14 es null - No se puede actualizar");
                return;
            }

            // ACTUALIZAR EL LABEL
            String textoLabel = "Productos Agregados ( " + totalProductos + ")";
            jLabel14.setText(textoLabel);
            // FORZAR ACTUALIZACIÓN VISUAL
            jLabel14.revalidate();
            jLabel14.repaint();

        } catch (Exception e) {
            System.err.println("ERROR: Error actualizando contador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Renderer personalizado para celdas de tabla con estilos FlatLaf
     *
     * Aplica el Patrón Decorator para extender el comportamiento del
     * DefaultTableCellRenderer sin modificar su funcionalidad base.
     */
    private class ModernTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // Llamar al renderer base
            Component cell = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // Obtener colores del tema actual de FlatLaf
            Color backgroundNormal = UIManager.getColor("Table.background");
            Color backgroundAlternate = UIManager.getColor("Table.alternateRowColor");
            Color backgroundSelected = UIManager.getColor("Table.selectionBackground");
            Color foregroundSelected = UIManager.getColor("Table.selectionForeground");
            Color foregroundNormal = UIManager.getColor("Table.foreground");

            // Aplicar colores según el estado
            if (isSelected) {
                cell.setBackground(backgroundSelected);
                cell.setForeground(foregroundSelected);
            } else {
                // Alternar colores de fondo para mejor legibilidad
                cell.setBackground(row % 2 == 0 ? backgroundNormal : backgroundAlternate);
                cell.setForeground(foregroundNormal);
            }

            // Configurar alineación según el tipo de columna
            if (value instanceof Number) {
                // Números alineados a la derecha
                ((JLabel) cell).setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (column == COL_CODIGO) {
                // Códigos centrados
                ((JLabel) cell).setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                ((JLabel) cell).setHorizontalAlignment(SwingConstants.LEFT);
            }

            // Añadir padding interno para mejorar espaciado
            ((JLabel) cell).setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            return cell;
        }
    }

    /**
     * Renderer especializado para valores monetarios Formatea números como
     * moneda y los alinea a la derecha
     */
    private class CurrencyRenderer extends DefaultTableCellRenderer {

        private final NumberFormat currencyFormat;

        public CurrencyRenderer() {
            // Configurar formato de moneda colombiano
            currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // Formatear el valor como moneda
            if (value instanceof Number) {
                value = currencyFormat.format(((Number) value).doubleValue());
            } else if (value instanceof BigDecimal) {
                value = currencyFormat.format(((BigDecimal) value).doubleValue());
            }

            Component cell = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // Aplicar estilos de FlatLaf
            aplicarEstilosSegunEstado(cell, isSelected, row);

            // Padding
            ((JLabel) cell).setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            return cell;
        }

        private void aplicarEstilosSegunEstado(Component cell, boolean isSelected, int row) {
            if (isSelected) {
                cell.setBackground(UIManager.getColor("Table.selectionBackground"));
                cell.setForeground(UIManager.getColor("Table.selectionForeground"));
            } else {
                Color bg = row % 2 == 0
                        ? UIManager.getColor("Table.background")
                        : UIManager.getColor("Table.alternateRowColor");
                cell.setBackground(bg);
                cell.setForeground(UIManager.getColor("Table.foreground"));
            }
        }
    }

    /**
     * Configura los renderers personalizados para cada columna de la tabla
     *
     * Principio Single Responsibility: Este método solo se encarga de asignar
     * renderers, no de crear la tabla ni manejar datos.
     */
    /**
     * Configura los renderers personalizados para cada columna de la tabla
     */
    private void configurarRenderersTabla() {
        // Renderer general para todas las columnas
        ModernTableCellRenderer defaultRenderer = new ModernTableCellRenderer();

        // Renderer específico para columnas monetarias
        CurrencyRenderer currencyRenderer = new CurrencyRenderer();

        // Renderer y Editor para botón de acción
        ButtonRenderer buttonRenderer = new ButtonRenderer();
        ButtonEditor buttonEditor = new ButtonEditor(new JCheckBox());

        // Asignar renderers por columna
        tablaProductos.getColumnModel().getColumn(COL_CODIGO).setCellRenderer(defaultRenderer);
        tablaProductos.getColumnModel().getColumn(COL_PRODUCTO).setCellRenderer(defaultRenderer);
        tablaProductos.getColumnModel().getColumn(COL_TIPO).setCellRenderer(defaultRenderer);
        tablaProductos.getColumnModel().getColumn(COL_CANTIDAD).setCellRenderer(defaultRenderer);

        // Columnas monetarias
        tablaProductos.getColumnModel().getColumn(COL_PRECIO).setCellRenderer(currencyRenderer);
        tablaProductos.getColumnModel().getColumn(COL_DESCUENTO).setCellRenderer(currencyRenderer);
        tablaProductos.getColumnModel().getColumn(COL_SUBTOTAL).setCellRenderer(currencyRenderer);

        // Columna de acción (botón eliminar)
        tablaProductos.getColumnModel().getColumn(COL_ACCION).setCellRenderer(buttonRenderer);
        tablaProductos.getColumnModel().getColumn(COL_ACCION).setCellEditor(buttonEditor);
    }

    /**
     * Configura el modelo y estilos visuales de la tabla de detalles
     */
    /**
     * Configura el modelo y estilos visuales de la tabla de detalles
     */
    private void configurarModeloDetalle() {
        // ===== CONFIGURACIÓN DEL MODELO =====
        DefaultTableModel m = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int r, int c) {
                // Las columnas editables son: Cantidad, Precio, Descuento y Acción
                return c == COL_CANTIDAD || c == COL_PRECIO || c == COL_DESCUENTO || c == COL_ACCION;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Definir tipos de datos por columna
                switch (columnIndex) {
                    case COL_CANTIDAD:
                        return Integer.class;
                    case COL_PRECIO:
                    case COL_DESCUENTO:
                    case COL_SUBTOTAL:
                        return BigDecimal.class;
                    case COL_ACCION:
                        return JButton.class;
                    default:
                        return String.class;
                }
            }
        };

        m.setColumnIdentifiers(new Object[] {
                "Código", "Producto", "Tipo", "Cantidad", "Precio", "Descuento", "Subtotal",
                "Acción", // ← Nueva columna
                "id_detalle", "id_variante", "id_producto"
        });

        tablaProductos.setModel(m);

        // ===== OCULTAR COLUMNAS INTERNAS =====
        ocultarColumna(COL_ID_DETALLE);
        ocultarColumna(COL_ID_VARIANTE);
        ocultarColumna(COL_ID_PRODUCTO);

        // ===== CONFIGURAR ANCHOS DE COLUMNA =====
        configurarAnchosColumnas();

        // ===== APLICAR RENDERERS PERSONALIZADOS =====
        configurarRenderersTabla();

        // ===== ESTILOS VISUALES MODERNOS =====
        aplicarEstilosModernosTabla();
    }

    /**
     * Utilidad para formateo de moneda colombiana Principio Single
     * Responsibility: Una clase, una responsabilidad
     */
    public static class MoneyFormatter {

        private static final NumberFormat FORMATTER = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        /**
         * Formatea un BigDecimal como moneda colombiana
         *
         * @param amount Monto a formatear
         * @return String formateado (ej: "$1.234.567")
         */
        public static String format(BigDecimal amount) {
            if (amount == null) {
                return "$0";
            }
            return FORMATTER.format(amount);
        }

        /**
         * Parsea texto monetario a BigDecimal
         *
         * @param text Texto con formato "$1.234.567"
         * @return BigDecimal parseado
         */
        public static BigDecimal parse(String text) {
            if (text == null || text.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            try {
                // 1. Limpieza básica: mantener solo números, puntos y comas
                String clean = text.replaceAll("[^0-9.,]", "").trim();

                if (clean.isEmpty())
                    return BigDecimal.ZERO;

                // 2. Analizar estructura para determinar formato
                int lastDot = clean.lastIndexOf('.');
                int lastComma = clean.lastIndexOf(',');

                // CASO 1: Contiene AMBOS separadores (punto y coma)
                if (lastDot > -1 && lastComma > -1) {
                    if (lastDot > lastComma) {
                        // Formato US (1,234.56) -> La coma es miles, punto es decimal
                        clean = clean.replace(",", ""); // quitar miles
                        // punto se queda como decimal
                    } else {
                        // Formato CO (1.234,56) -> Punto es miles, coma es decimal
                        clean = clean.replace(".", ""); // quitar miles
                        clean = clean.replace(",", "."); // coma a punto decimal
                    }
                }
                // CASO 2: Solo tiene PUNTOS
                else if (lastDot > -1) {
                    // En Colombia el punto es MILES (150.000 = 150 mil)
                    // Asumimos siempre que punto es miles, ya que el peso no usa decimales
                    // comúnmente con punto
                    // Si el usuario pone 150.00 (formato US), se interpretará como 15000 (15 mil)
                    // al quitar punto.
                    // PERO si ponemos 150.000 se interpretará como 150 mil. Es lo correcto.
                    clean = clean.replace(".", "");
                }
                // CASO 3: Solo tiene COMAS
                else if (lastComma > -1) {
                    // Puede ser:
                    // A) Decimales (150,50 -> 150.50)
                    // B) Miles estilo US (150,000 -> 150 mil)

                    // Heurística: Si hay exactamente 3 dígitos después de la coma final
                    // y el número total es grande... es PROBABLE que sea separador de miles US.
                    String afterComma = clean.substring(lastComma + 1);
                    if (afterComma.length() == 3) {
                        // Ambigüedad fuerte (100,000).
                        // Si el usuario escribe 150,000 queriendo decir 150 mil, esto lo salva.
                        // Si escribe 100,123 (100 pesos con 123 centavos), fallará. Pero pesos no tiene
                        // 3 decimales.
                        clean = clean.replace(",", "");
                    } else {
                        // Decimal normal (100,50)
                        clean = clean.replace(",", ".");
                    }
                }

                return new BigDecimal(clean);

            } catch (Exception e) {
                System.err.println("WARN: Error parseando moneda: " + text + " - " + e.getMessage());
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Configura los anchos óptimos para cada columna
     */
    /**
     * Configura los anchos óptimos para cada columna
     */
    private void configurarAnchosColumnas() {
        TableColumnModel columnModel = tablaProductos.getColumnModel();

        // Código: ancho fijo mediano
        columnModel.getColumn(COL_CODIGO).setPreferredWidth(120);
        columnModel.getColumn(COL_CODIGO).setMinWidth(100);
        columnModel.getColumn(COL_CODIGO).setMaxWidth(150);

        // Producto: expansible
        columnModel.getColumn(COL_PRODUCTO).setPreferredWidth(280);
        columnModel.getColumn(COL_PRODUCTO).setMinWidth(180);

        // Tipo: ancho fijo pequeño
        columnModel.getColumn(COL_TIPO).setPreferredWidth(70);
        columnModel.getColumn(COL_TIPO).setMinWidth(60);
        columnModel.getColumn(COL_TIPO).setMaxWidth(90);

        // Cantidad: ancho fijo pequeño
        columnModel.getColumn(COL_CANTIDAD).setPreferredWidth(80);
        columnModel.getColumn(COL_CANTIDAD).setMinWidth(60);
        columnModel.getColumn(COL_CANTIDAD).setMaxWidth(100);

        // Precio: ancho fijo mediano
        columnModel.getColumn(COL_PRECIO).setPreferredWidth(110);
        columnModel.getColumn(COL_PRECIO).setMinWidth(100);

        // Descuento: ancho fijo mediano
        columnModel.getColumn(COL_DESCUENTO).setPreferredWidth(110);
        columnModel.getColumn(COL_DESCUENTO).setMinWidth(100);

        // Subtotal: ancho fijo mediano
        columnModel.getColumn(COL_SUBTOTAL).setPreferredWidth(120);
        columnModel.getColumn(COL_SUBTOTAL).setMinWidth(110);

        // Acción: ancho fijo para el botón
        columnModel.getColumn(COL_ACCION).setPreferredWidth(80);
        columnModel.getColumn(COL_ACCION).setMinWidth(70);
        columnModel.getColumn(COL_ACCION).setMaxWidth(90);
    }

    /**
     * Aplica estilos visuales modernos a la tabla usando propiedades de FlatLaf
     */
    private void aplicarEstilosModernosTabla() {
        try {
            // ========== PASO 1: CONFIGURAR LA TABLA ==========

            // Altura de fila generosa para mejor legibilidad
            tablaProductos.setRowHeight(50);

            // Estilos FlatLaf client properties para la tabla
            tablaProductos.putClientProperty(FlatClientProperties.STYLE, ""
                    + "rowHeight:50;"
                    + "showHorizontalLines:true;"
                    + "showVerticalLines:false;"
                    + "intercellSpacing:0,1;"
                    + "selectionBackground:$Table.selectionBackground;"
                    + "selectionForeground:$Table.selectionForeground;");

            // ========== PASO 2: CONFIGURAR HEADER DE LA TABLA ==========

            JTableHeader header = tablaProductos.getTableHeader();
            header.setReorderingAllowed(false); // Evitar reordenamiento de columnas
            header.putClientProperty(FlatClientProperties.STYLE, ""
                    + "height:40;"
                    + "hoverBackground:darken($TableHeader.background,5%);"
                    + "pressedBackground:darken($TableHeader.background,10%);"
                    + "separatorColor:$TableHeader.background;");

            // Font del header
            header.setFont(new Font(FlatRobotoFont.FAMILY, Font.BOLD, 12));

            // ========== PASO 5: ACTUALIZAR VISUAL ==========

            panelProductos.revalidate();
            panelProductos.repaint();

        } catch (Exception e) {
            System.err.println("ERROR: Error aplicando estilos modernos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Oculta una columna de la tabla
     */
    private void ocultarColumna(int c) {
        tablaProductos.getColumnModel().getColumn(c).setMinWidth(0);
        tablaProductos.getColumnModel().getColumn(c).setMaxWidth(0);
        tablaProductos.getColumnModel().getColumn(c).setPreferredWidth(0);
    }

    /**
     * Constructor para modo edición de venta existente
     *
     * @param idVenta ID de la venta a editar
     */
    public generarVentaFor1(int idVenta) throws SQLException {
        this();

        this.modoEdicion = true;
        this.idVentaEdicion = idVenta;

        btngenerarVenta.setText("Modificar Venta");
        productosEliminados = new ArrayList<>();

        cargarDatosVenta(idVenta);
        jLabel1.setText("Editar venta #" + idVenta);
    }

    /**
     * Carga datos de una venta existente para edición
     */
    private void cargarDatosVenta(int idVenta) {
        Connection con = null;
        try {
            con = conexion.getInstance().createConnection();

            String sqlVenta = "SELECT v.*, c.nombre as cliente_nombre, c.dni, c.telefono, "
                    + "c.direccion, c.email "
                    + "FROM ventas v "
                    + "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente "
                    + "WHERE v.id_venta = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlVenta)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nombreCliente = rs.getString("cliente_nombre");
                        String dni = rs.getString("dni");
                        int idCliente = rs.getInt("id_cliente");

                        if (idCliente > 0) {
                            DataSearchClient cliente = new DataSearchClient(
                                    idCliente, nombreCliente != null ? nombreCliente : "",
                                    dni != null ? dni : "", "", "", "",
                                    rs.getTimestamp("fecha_venta"), 0, true, true);
                            selectClient = cliente;

                            txtCliente.setText(dni != null ? dni : "");
                            actualizarInfoCliente(cliente);

                            txtCliente.setEnabled(false);
                            btnSeleccionarCliente.setEnabled(false);
                        }

                        double subtotal = rs.getDouble("subtotal");
                        double descuento = rs.getDouble("descuento");
                        double total = rs.getDouble("total");

                        txtSubTotal.setText("$" + subtotal);
                        txtDescuento.setText("$" + descuento);
                        txtPendiente.setText("$" + total);

                        String observaciones = rs.getString("observaciones");
                        txtObservaciones.setText(observaciones != null ? observaciones : "");

                        String tipoPago = rs.getString("tipo_pago");
                        String estado = rs.getString("estado");

                        if (tipoPago != null) {
                            cbxTipoPago.setSelectedItem(tipoPago);
                        }
                        if (estado != null) {
                            cbxEstadoPago.setSelectedItem(estado);
                        }
                    }
                }
            }

            String sqlDetalles = "SELECT vd.id_detalle, vd.id_producto, vd.id_variante, "
                    + "vd.cantidad, vd.precio_unitario, vd.descuento, vd.subtotal, vd.tipo_venta, "
                    + "p.nombre as producto_nombre, "
                    + "COALESCE(pv.ean, p.codigo_modelo) AS codigo_identificador, "
                    + "c.nombre as color_nombre, "
                    + "CONCAT(COALESCE(t.numero, ''), ' ', COALESCE(t.sistema, '')) as talla_completa "
                    + "FROM venta_detalles vd "
                    + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                    + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "WHERE vd.id_venta = ? AND vd.activo = 1 "
                    + "ORDER BY vd.id_detalle";

            try (PreparedStatement ps = con.prepareStatement(sqlDetalles)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
                    model.setRowCount(0);

                    while (rs.next()) {
                        int idDetalle = rs.getInt("id_detalle");
                        int idProducto = rs.getInt("id_producto");
                        int idVariante = rs.getInt("id_variante");
                        String nombreProducto = rs.getString("producto_nombre");
                        String codigoIdentificador = rs.getString("codigo_identificador");

                        String colorNombre = rs.getString("color_nombre");
                        String tallaCompleta = rs.getString("talla_completa");

                        String nombreCompleto = nombreProducto != null ? nombreProducto : "";
                        if (colorNombre != null && tallaCompleta != null
                                && !colorNombre.trim().isEmpty() && !tallaCompleta.trim().isEmpty()) {
                            nombreCompleto += " (" + colorNombre + " - " + tallaCompleta.trim() + ")";
                        }

                        String tipoVentaDB = rs.getString("tipo_venta");
                        String tipoVenta = "par";
                        if (tipoVentaDB != null && !tipoVentaDB.isEmpty()) {
                            if (tipoVentaDB.toLowerCase().contains("caja")) {
                                tipoVenta = "caja";
                            } else if (tipoVentaDB.toLowerCase().contains("par")) {
                                tipoVenta = "par";
                            } else {
                                tipoVenta = tipoVentaDB.replace("salida ", "").trim();
                                if (tipoVenta.isEmpty()) {
                                    tipoVenta = "par";
                                }
                            }
                        }

                        int cantidad = rs.getInt("cantidad");
                        BigDecimal precioUnitario = rs.getBigDecimal("precio_unitario");
                        BigDecimal descuentoDetalle = rs.getBigDecimal("descuento");
                        BigDecimal subtotalDetalle = rs.getBigDecimal("subtotal");

                        if (precioUnitario == null) {
                            precioUnitario = BigDecimal.ZERO;
                        }
                        if (descuentoDetalle == null) {
                            descuentoDetalle = BigDecimal.ZERO;
                        }
                        if (subtotalDetalle == null) {
                            subtotalDetalle = BigDecimal.ZERO;
                        }
                        if (codigoIdentificador == null) {
                            codigoIdentificador = "";
                        }

                        model.addRow(new Object[] {
                                codigoIdentificador, // 0: Código
                                nombreCompleto, // 1: Producto
                                tipoVenta, // 2: Tipo
                                cantidad, // 3: Cantidad
                                precioUnitario, // 4: Precio
                                descuentoDetalle, // 5: Descuento
                                subtotalDetalle, // 6: Subtotal
                                null, // 7: Acción
                                idDetalle, // 8: id_detalle
                                idVariante, // 9: id_variante
                                idProducto // 10: id_producto
                        });
                    }
                }
            }

            jLabel1.setText("Editar venta #" + idVenta);

        } catch (SQLException e) {
            System.err.println("Error SQL al cargar la venta: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error de base de datos al cargar la venta: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    System.err.println("Error cerrando conexión: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reemplaza actualizarInfoCliente() existente Manejo más robusto de datos
     * nulos
     */
    private void actualizarInfoCliente(DataSearchClient cliente) {
        if (cliente == null) {
            txtNombreCliente.setText("");
            txtDatosCliente.setText("");
            panelInfoCliente.setVisible(false);
            return;
        }

        // Obtener datos con manejo seguro de nulos
        String nombre = cliente.getNombre() != null ? cliente.getNombre() : "Sin nombre";
        String dni = cliente.getDni() != null ? cliente.getDni() : "Sin DNI";
        String email = cliente.getEmail() != null ? cliente.getEmail() : "Sin email";
        String telefono = cliente.getTelefono() != null ? cliente.getTelefono() : "Sin teléfono";

        // Actualizar labels
        txtNombreCliente.setText(nombre);
        txtDatosCliente.setText(String.format(
                "<html>DNI: %s<br>Email: %s<br>Tel: %s</html>",
                dni, email, telefono));

        // Asegurar que el panel sea visible
        panelInfoCliente.setVisible(true);

        // Forzar actualización visual
        panelCliente.revalidate();
        panelCliente.repaint();
    }

    /**
     * Configura placeholders de campos de texto
     */
    private void initPlaceHolders() {
        txtCliente.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar cliente...");
        txtIngresarCodigo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar producto...");
        txtNumeroNotaCredito.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Número de nota de crédito");
    }

    /**
     * Crea un icono coloreado
     */

    /**
     * Configura las dimensiones iniciales de los paneles Evita que
     * panelProductos ocupe todo el espacio verticalmente
     */
    private void configurarDimensionesIniciales() {
        try {
            // Panel título: altura fija
            panelTitulo.setPreferredSize(new Dimension(1629, PANEL_TITULO_HEIGHT));
            panelTitulo.setMinimumSize(new Dimension(400, PANEL_TITULO_HEIGHT));
            panelTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, PANEL_TITULO_HEIGHT));

            // Panel de productos: altura controlada
            panelProductos.setPreferredSize(new Dimension(1258, PANEL_PRODUCTOS_HEIGHT));
            panelProductos.setMinimumSize(new Dimension(400, 300));

            // Panel final: altura fija
            panelFinal.setPreferredSize(new Dimension(1258, PANEL_FINAL_HEIGHT));
            panelFinal.setMinimumSize(new Dimension(400, PANEL_FINAL_HEIGHT));
            panelFinal.setMaximumSize(new Dimension(Integer.MAX_VALUE, PANEL_FINAL_HEIGHT));

            // Sidebar scroll: ancho fijo
            jScrollPane1.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 600));
            jScrollPane1.setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, 400));
        } catch (Exception e) {
            System.err.println("ERROR: Error configurando dimensiones: " + e.getMessage());
        }
    }

    /**
     * Configura el comportamiento responsive del panel Patrón Observer: Escucha
     * cambios de tamaño y ajusta el layout dinámicamente
     */
    private void configurarResponsive() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarLayoutResponsive();
            }
        });

        // Ejecutar ajuste inicial
        ajustarLayoutResponsive();
    }

    /**
     * Ajusta el layout según el ancho de la ventana Implementa tres
     * breakpoints: móvil, tablet y escritorio
     */
    private void ajustarLayoutResponsive() {
        int anchoActual = getWidth();
        int altoActual = getHeight();

        if (anchoActual < BREAKPOINT_MOBILE) {
            // Layout móvil: todo apilado verticalmente
            aplicarLayoutMovil(anchoActual, altoActual);
        } else if (anchoActual < BREAKPOINT_TABLET) {
            // Layout tablet: dos columnas con sidebar reducido

        } else {
            // Layout escritorio: diseño completo
            aplicarLayoutEscritorio(anchoActual, altoActual);
        }
    }

    /**
     * Layout para pantallas pequeñas (< 800px) Todo en una columna vertical con
     * scroll
     */
    private void aplicarLayoutMovil(int ancho, int alto) {
        try {
            // ScrollPane ocupa todo el ancho
            jScrollPane1.setPreferredSize(new Dimension(ancho - 20, alto - PANEL_TITULO_HEIGHT - 40));

            // Paneles secundarios se ocultan o minimizan
            panelProductos.setPreferredSize(new Dimension(ancho - 20, 200));
            panelFinal.setPreferredSize(new Dimension(ancho - 20, PANEL_FINAL_HEIGHT));

            // Opcional: Cambiar orientación del GroupLayout a vertical
            panelMain.revalidate();
            panelMain.repaint();
        } catch (Exception e) {
            System.err.println("ERROR: Error en layout móvil: " + e.getMessage());
        }
    }

    private void aplicarLayoutEscritorio(int ancho, int alto) {
        try {
            // Título: ancho completo
            panelTitulo.setPreferredSize(new Dimension(ancho - 20, PANEL_TITULO_HEIGHT));

            // Cálculos de espacio disponible
            int anchoDisponible = ancho - SIDEBAR_WIDTH - 48; // 48px de gaps
            int altoDisponible = alto - PANEL_TITULO_HEIGHT - PANEL_FINAL_HEIGHT - 60;

            // Sidebar con ancho completo
            jScrollPane1.setPreferredSize(new Dimension(SIDEBAR_WIDTH, alto - PANEL_TITULO_HEIGHT - 40));

            // Panel de productos: altura controlada, no todo el espacio
            int altoProductos = Math.min(altoDisponible, PANEL_PRODUCTOS_HEIGHT);
            panelProductos.setPreferredSize(new Dimension(anchoDisponible, altoProductos));
            panelProductos.setVisible(true);

            // Panel final: altura fija
            panelFinal.setPreferredSize(new Dimension(anchoDisponible, PANEL_FINAL_HEIGHT));
            panelFinal.setVisible(true);

            panelMain.revalidate();
            panelMain.repaint();
        } catch (Exception e) {
            System.err.println("ERROR: Error en layout escritorio: " + e.getMessage());
        }
    }

    public void cargarUi() {
        try {
            FlatRobotoFont.install();
            UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

            aplicarEstiloPaneles();
            jScrollPane1.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);

            // Estilos de campos de texto
            txtIngresarCodigo.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            txtNumeroNotaCredito.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            txtCliente.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            lblNotaCredito.setVisible(false);
            txtNumeroNotaCredito.setVisible(false);
            // Estilos de combos
            comboTalla.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            comboColor.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            cbxTipo.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            cbxEstadoPago.putClientProperty(FlatClientProperties.STYLE, CAMPOS_TEXTO);
            panelInfoProd.putClientProperty(FlatClientProperties.STYLE,
                    "arc:20;background:darken($Login.background,10%);");
            jScrollPane3.putClientProperty(FlatClientProperties.STYLE, "arc:20;");
            // Estilo de tabla
            tablaProductos.putClientProperty(FlatClientProperties.STYLE, ""
                    + "rowHeight:50;"
                    + "showHorizontalLines:true;"
                    + "intercellSpacing:0,1");
        } catch (Exception e) {
            System.err.println("ERROR: Error configurando UI: " + e.getMessage());
        }
    }

    private void aplicarEstiloPaneles() {
        try {
            // Aplicar estilo a todos los paneles principales
            javax.swing.JPanel[] paneles = {
                    panelTitulo, panelProductos, panelFinal, panelProd, panelCliente, panelMetod, panelEstadoCaj
            };

            for (javax.swing.JPanel panel : paneles) {
                if (panel != null) {
                    panel.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Error aplicando estilos: " + e.getMessage());
        }
    }

    /**
     * Inicializa los menús de búsqueda emergentes Patrón Strategy: Diferentes
     * estrategias de búsqueda
     */
    private void initMenus() {
        menu = new JPopupMenu();
        menu1 = new JPopupMenu();
        menu.setFocusable(false);
        menu1.setFocusable(false);

        searchProd = new PanelSearch();
        searchClient = new PanelSearchC();

        menu.add(searchProd);
        menu1.add(searchClient);

        searchClient.addEventClickC(new EventClickC() {
            @Override
            public void itemClickC(DataSearchClient data) {
                menu1.setVisible(false);

                // Asignar cliente seleccionado
                selectClient = data;

                // NUEVO: Actualizar campo de texto con DNI (mejor para búsqueda)
                txtCliente.setText(data.getDni());
                // CRÍTICO: Actualizar panel de información del cliente
                actualizarInfoCliente(data);

                // Hacer visible el panel y deshabilitar edición
                panelInfoCliente.setVisible(true);
                txtCliente.setEnabled(false);
                btnSeleccionarCliente.setEnabled(false);

                // Forzar actualización visual
                panelCliente.revalidate();
                panelCliente.repaint();
            }

            @Override
            public void itemRemoveC(Component com, DataSearchClient data) {
                searchClient.remove(com);
                menu1.setPopupSize(menu1.getWidth(),
                        (searchClient.getItemSize() * 35) + 2);
                if (searchClient.getItemSize() == 0) {
                    menu1.setVisible(false);
                }
            }
        });

        // Configurar evento de clic para productos
        searchProd.addEventClick(new EventClick() {
            @Override
            public void itemClick(DataSearch data) {
                menu.setVisible(false);
                selectedProduct = data;
                seleccionarProducto(data);

                int idProducto = Integer.parseInt(data.getId_prod());
                cargarTallasProducto(idProducto);
                cargarColoresProducto(idProducto);

                cbxTipo.setSelectedIndex(0);
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
    }

    /**
     * Carga colores disponibles para un producto
     */
    private void cargarColoresProducto(int idProducto) {
        isLoadingColores = true;

        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

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

            System.out
                    .println("Colores cargados para producto ID: " + idProducto + " - Total: " + coloresUnicos.size());
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando colores del producto");
        } finally {
            isLoadingColores = false;
        }
    }

    private String construirNombreTalla(ModelProductVariant variant) {
        if (variant == null) {
            return null;
        }

        try {
            // Obtener componentes de la talla
            String numero = variant.getSizeName(); // Ej: "35", "40"
            String sistema = variant.getSizeSystem(); // Ej: "EU", "USA"

            // CONSTRUIR TALLA CON FORMATO CORRECTO
            if (numero == null || numero.trim().isEmpty()) {
                return null;
            }

            StringBuilder tallaBuilder = new StringBuilder();

            // Agregar número de talla
            tallaBuilder.append(numero.trim());

            // Agregar sistema si existe y no está duplicado
            if (sistema != null && !sistema.trim().isEmpty() && !numero.contains(sistema)) {
                tallaBuilder.append(" ").append(sistema.trim());
            }

            String resultado = tallaBuilder.toString().trim();
            // VALIDACIÓN: No retornar valores como "00" o "null"
            if (resultado.equals("00") || resultado.equals("null") || resultado.isEmpty()) {
                return null;
            }

            return resultado;

        } catch (Exception e) {
            System.err.println("ERROR: Error en construirNombreTalla: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void validacionFinalAntesDeLlenarTabla() {
        try {
            // Obtener tipo seleccionado
            String tipoSeleccionado = (String) cbxTipo.getSelectedItem();
            int cantidadSolicitada = (Integer) txtCantidad.getValue();

            // Validar stock según tipo
            int stockDisponible = 0;
            if ("caja".equalsIgnoreCase(tipoSeleccionado)) {
                stockDisponible = selectedProduct.getStockPorCajas();
            } else {
                stockDisponible = selectedProduct.getStockPorPares();
            }

            // CRÍTICO: Verificar stock
            if (cantidadSolicitada > stockDisponible) {

                Toast.show(this, Toast.Type.ERROR,
                        String.format("Stock insuficiente. Disponible: %d. Solicitado: %d",
                                stockDisponible, cantidadSolicitada));

                return; // NO AGREGAR
            }
            // Aquí continúa el código de agregación

        } catch (Exception e) {
            System.err.println("ERROR: Error en validación final: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== Nueva carga de productos: limpiarFormulario
    // ====================
    private void cargarTallasDisponibles(int idProducto) {
        isLoadingTallas = true;
        previousTallaSelected = ""; // Reset

        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            if (idBodega == null || idBodega == 0) {
                System.err.println("ERROR: Bodega inválida");
                return;
            }

            // Limpiar combo
            comboTalla.removeAllItems();
            comboTalla.addItem("Seleccionar talla");

            // Obtener tallas
            List<String> tallasDisponibles = serviceInventarioBodega.obtenerTallasDisponibles(idBodega, idProducto);
            for (String talla : tallasDisponibles) {
                comboTalla.addItem(talla);
            }

            if (tallasDisponibles.isEmpty()) {
                Toast.show(this, Toast.Type.WARNING, "No hay stock disponible de este producto en tu bodega");
            }

        } catch (SQLException e) {
            System.err.println("ERROR: Error: " + e.getMessage());
            Toast.show(this, Toast.Type.ERROR, "Error al cargar tallas: " + e.getMessage());
        } finally {
            isLoadingTallas = false;
        }
    }

    // ============================================================================
    // PASO 6: MÉTODO MEJORADO - cargarColoresDisponibles()
    // ============================================================================
    private void cargarColoresDisponibles(int idProducto) {
        isLoadingColores = true;
        previousColorSelected = ""; // Reset

        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            if (idBodega == null || idBodega == 0) {
                System.err.println("ERROR: Bodega inválida");
                return;
            }

            // Limpiar combo
            comboColor.removeAllItems();
            comboColor.addItem("Seleccionar color");

            // Obtener colores
            List<String> coloresDisponibles = serviceInventarioBodega.obtenerColoresDisponibles(idBodega, idProducto);
            for (String color : coloresDisponibles) {
                comboColor.addItem(color);
            }

            if (coloresDisponibles.isEmpty()) {
                Toast.show(this, Toast.Type.WARNING, "No hay stock disponible de este producto en tu bodega");
            }

        } catch (SQLException e) {
            System.err.println("ERROR: Error: " + e.getMessage());
            Toast.show(this, Toast.Type.ERROR, "Error al cargar colores: " + e.getMessage());
        } finally {
            isLoadingColores = false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PASO 5: MÉTODO CORREGIDO PARA BUSCAR PRODUCTO POR CÓDIGO
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Busca un producto por código (EAN/SKU) y carga sus datos Corregido para
     * seleccionar correctamente la talla en el combo
     *
     * Correcciones implementadas: - Verifica stock en bodega actual antes de
     * mostrar - Selecciona correctamente la talla en el ComboBox - Carga datos
     * completos desde inventario_bodega
     */
    private void buscarProductoPorCodigo(String codigo) {
        try {
            // Obtener ID de bodega desde la sesión del usuario
            int idBodega = UserSession.getInstance().getIdBodegaUsuario();
            // Limpiar campos previos
            limpiarCamposProducto();

            // Buscar variante por EAN o SKU
            java.sql.Connection conn = conexion.getInstance().getConnection();
            String sql = "SELECT pv.*, p.nombre, p.descripcion, p.id_producto, "
                    + "t.numero AS talla_numero, t.sistema AS talla_sistema, "
                    + "c.nombre AS color_nombre "
                    + "FROM producto_variantes pv "
                    + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                    + "WHERE (pv.EAN = ? OR pv.SKU = ?) "
                    + "AND pv.disponible = 1 AND p.activo = 1";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, codigo);
            ps.setString(2, codigo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int idProducto = rs.getInt("id_producto");
                int idVariante = rs.getInt("id_variante");
                String tallaNombre = rs.getString("talla_numero");
                String tallaSistema = rs.getString("talla_sistema");
                String talla = tallaNombre != null ? tallaNombre + (tallaSistema != null ? " " + tallaSistema : "")
                        : "";
                String color = rs.getString("color_nombre");

                // ═══════════════════════════════════════════════════════════════
                // VERIFICAR STOCK EN BODEGA ANTES DE MOSTRAR
                // ═══════════════════════════════════════════════════════════════
                ModelInventarioBodega inventario = serviceInventarioBodega.obtenerStockPorVariante(
                        idBodega, idVariante);

                if (inventario == null || !inventario.tieneStockDisponible()) {
                    Toast.show(this, Toast.Type.WARNING,
                            "Este producto no tiene stock disponible en tu bodega");
                    limpiarCamposProducto();
                    rs.close();
                    ps.close();
                    conn.close();
                    return;
                }
                // ═══════════════════════════════════════════════════════════════
                // CARGAR DATOS DEL PRODUCTO
                // ═══════════════════════════════════════════════════════════════
                // Crear DataSearch con datos completos
                DataSearch producto = new DataSearch();

                producto.setId_prod(String.valueOf(idProducto));
                producto.setEAN(rs.getString("EAN"));
                producto.setSKU(rs.getString("SKU"));
                producto.setNombre(rs.getString("nombre"));
                producto.setDescripcion(rs.getString("descripcion"));
                producto.setColor(color);
                producto.setTalla(talla);
                BigDecimal precioVenta = inventario.getPrecioVenta() != null ? inventario.getPrecioVenta()
                        : BigDecimal.ZERO;
                producto.setPrecioVenta(precioVenta);
                producto.setStockPorPares(inventario.getStockDisponiblePares());
                producto.setStockPorCajas(inventario.getStockDisponibleCajas());
                producto.setIdVariante(idVariante);

                // Guardar producto seleccionado
                selectedProduct = producto;

                // Mostrar datos en UI
                txtNombreProducto.setText(rs.getString("nombre"));
                txtCodigo.setText(String.valueOf(idProducto));
                txtEAN.setText(rs.getString("EAN"));
                txtPrecio.setText("$" + precioVenta.toString());
                txtStock.setText(inventario.getStockDisponiblePares() + " pares disponibles");

                // ═══════════════════════════════════════════════════════════════
                // CARGAR Y SELECCIONAR CORRECTAMENTE LA TALLA
                // ═══════════════════════════════════════════════════════════════
                // Primero cargar todas las tallas disponibles en bodega
                cargarTallasDisponibles(idProducto);

                // Esperar un momento para que se cargue el combo
                SwingUtilities.invokeLater(() -> {
                    // Buscar y seleccionar la talla correcta
                    boolean tallaEncontrada = false;
                    for (int i = 0; i < comboTalla.getItemCount(); i++) {
                        String itemTalla = comboTalla.getItemAt(i);
                        if (itemTalla != null && itemTalla.equals(talla)) {
                            comboTalla.setSelectedIndex(i);
                            tallaEncontrada = true;
                            break;
                        }
                    }

                    if (!tallaEncontrada) {
                        System.err.println("WARN: No se pudo seleccionar la talla: " + talla);
                    }
                });

                // Cargar colores disponibles
                cargarColoresDisponibles(idProducto);

                // Seleccionar color si está disponible
                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < comboColor.getItemCount(); i++) {
                        String itemColor = comboColor.getItemAt(i);
                        if (itemColor != null && itemColor.equals(color)) {
                            comboColor.setSelectedIndex(i);
                            break;
                        }
                    }
                });

                // Actualizar estilo del botón agregar
                actualizarEstiloBtnAgregar();
            } else {
                Toast.show(this, Toast.Type.WARNING,
                        "Producto no encontrado con código: " + codigo);
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.err.println("ERROR: Error buscando producto: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR,
                    "Error al buscar producto: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PASO 6: MÉTODO PARA ACTUALIZAR DATOS COMPLETOS DEL PRODUCTO
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Actualiza los datos completos del producto cuando se selecciona talla y
     * color Corregido para usar inventario_bodega
     */
    private void actualizarDatosProductoCompleto() {

        // ========================================================================
        // VALIDACIÓN PREVIA
        // ========================================================================
        if (selectedProduct == null) {
            System.err.println("ERROR: No hay producto seleccionado");
            return;
        }

        try {
            // ====================================================================
            // PASO 1: Obtener y validar talla seleccionada
            // ====================================================================
            int tallaIndex = comboTalla.getSelectedIndex();
            String tallaSeleccionada = null;

            if (tallaIndex > 0) {
                Object tallaItem = comboTalla.getSelectedItem();
                if (tallaItem != null) {
                    tallaSeleccionada = tallaItem.toString().trim();
                }
            }

            // ====================================================================
            // PASO 2: Obtener y validar color seleccionado
            // ====================================================================
            int colorIndex = comboColor.getSelectedIndex();
            String colorSeleccionado = null;

            if (colorIndex > 0) {
                Object colorItem = comboColor.getSelectedItem();
                if (colorItem != null) {
                    colorSeleccionado = colorItem.toString().trim();
                }
            }

            // ====================================================================
            // VALIDACIÓN: Ambos deben estar seleccionados
            // ====================================================================
            if (tallaSeleccionada == null || colorSeleccionado == null) {
                return;
            }

            // ====================================================================
            // PASO 3: Obtener datos de bodega y producto
            // ====================================================================
            int idBodega = UserSession.getInstance().getIdBodegaUsuario();
            int idProducto = Integer.parseInt(selectedProduct.getId_prod());

            // ====================================================================
            // PASO 4: Buscar variante en inventario
            // ====================================================================

            ModelInventarioBodega inventario = serviceInventarioBodega.obtenerVariantePorTallaColor(
                    idBodega, idProducto, tallaSeleccionada, colorSeleccionado);

            // ====================================================================
            // VALIDACIÓN: Verificar que se encontró inventario
            // ====================================================================
            if (inventario == null) {
                System.err.println("ERROR: NO SE ENCONTRÓ INVENTARIO");
                System.err.println("   Verifica en la BD que existe un registro con:");
                System.err.println("   → Bodega: " + idBodega);
                System.err.println("   → Producto: " + idProducto);
                System.err.println("   → Talla: '" + tallaSeleccionada + "'");
                System.err.println("   → Color: '" + colorSeleccionado + "'");

                Toast.show(this, Toast.Type.WARNING,
                        "No hay stock disponible de esta combinación en tu bodega");
                txtStock.setText("0 pares disponibles");
                return;
            }

            // ====================================================================
            // PASO 5: Actualizar datos del producto
            // ====================================================================

            selectedProduct.setIdVariante(inventario.getIdVariante());
            selectedProduct.setTalla(tallaSeleccionada);
            selectedProduct.setColor(colorSeleccionado);
            selectedProduct.setStockPorPares(inventario.getStockDisponiblePares());
            selectedProduct.setStockPorCajas(inventario.getStockDisponibleCajas());
            selectedProduct.setPrecioVenta(inventario.getPrecioVenta());
            selectedProduct.setEAN(inventario.getEan());
            selectedProduct.setSKU(inventario.getSku());

            // ====================================================================
            // PASO 6: Actualizar UI
            // ====================================================================
            txtStock.setText(inventario.getStockDisponiblePares() + " pares disponibles");
            txtPrecio.setText("$" + inventario.getPrecioVenta().toString());
            txtEAN.setText(inventario.getEan());

            // ====================================================================
            // PASO 7: Actualizar estado del botón
            // ====================================================================
            actualizarEstiloBtnAgregar();

        } catch (SQLException e) {
            System.err.println("ERROR: ERROR SQL:");
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR,
                    "Error al actualizar datos del producto: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERROR: ERROR GENERAL:");
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR,
                    "Error inesperado: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PASO 7: MODIFICAR EL PANEL DE BÚSQUEDA (searchProd)
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Inicializa el panel de búsqueda de productos Corregido para filtrar solo
     * productos con stock en bodega actual
     */

    /*
     * private void inicializarPanelBusqueda() {
     * searchProd = new PanelSearch();
     * 
     * // Configurar evento de búsqueda
     * searchProd.addEventClick(new EventClick() {
     * 
     * @Override
     * public void itemClick(Component com, DataSearch item) {
     * try {
     *
     * 
     * // Obtener ID de bodega del usuario
     * int idBodega = UserSession.getInstance().getIdBodegaUsuario();
     * 
     * // Verificar que la variante tenga stock en esta bodega
     * ModelInventarioBodega inventario =
     * serviceInventarioBodega.obtenerStockPorVariante(
     * idBodega, item.getIdVariante());
     * 
     * if (inventario == null || !inventario.tieneStockDisponible()) {
     *
     * Toast.show(generarVentaFor1.this, Toast.Type.WARNING,
     * "Este producto no tiene stock disponible en tu bodega");
     * return;
     * }
     * 
     * // Actualizar stock del item con datos de bodega
     * item.setStockPorPares(inventario.getStockDisponiblePares());
     * item.setStockPorCajas(inventario.getStockDisponibleCajas());
     * // Procesar selección
     * seleccionarProductoDesdeMenu(item);
     * menu.setVisible(false);
     * 
     * } catch (SQLException e) {
     * System.err.println("ERROR: Error verificando stock en bodega: " +
     * e.getMessage());
     * e.printStackTrace();
     * }
     * }
     * 
     * @Override
     * public void itemClick(DataSearch data) {
     * throw new UnsupportedOperationException("Not supported yet."); // Generated
     * from
     * nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
     * }
     * 
     * @Override
     * public void itemRemove(Component com, DataSearch data) {
     * throw new UnsupportedOperationException("Not supported yet."); // Generated
     * from
     * nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
     * }
     * });
     * }
     */
    // ══════════════════════════════════════════════════════════════════════════
    // PASO 8: MODIFICAR LA CONSULTA SQL DEL BUSCADOR
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Busca productos disponibles en la bodega del usuario Esta consulta debe
     * reemplazar la consulta actual del buscador para filtrar solo productos
     * con stock en la bodega activa
     */
    private String obtenerQueryBusquedaPorBodega() {
        return "SELECT DISTINCT "
                + "    p.id_producto, "
                + "    pv.id_variante, "
                + "    pv.EAN, "
                + "    pv.SKU, "
                + "    p.nombre, "
                + "    pv.color, "
                + "    pv.talla, "
                + "    pv.precio_venta, "
                + "    ib.Stock_par, "
                + "    ib.Stock_caja "
                + "FROM productos p "
                + "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                + "INNER JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante "
                + "WHERE ib.id_bodega = ? "
                + // Filtro por bodega del usuario
                "    AND ib.activo = 1 "
                + "    AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) "
                + // Solo con stock
                "    AND pv.activo = 1 "
                + "    AND p.activo = 1 "
                + "    AND (p.nombre LIKE ? OR pv.EAN LIKE ? OR pv.SKU LIKE ?) "
                + "ORDER BY p.nombre, pv.talla";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PASO 9: MÉTODO AUXILIAR PARA LIMPIAR CAMPOS
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Limpia todos los campos relacionados con el producto seleccionado
     */
    private DataSearch buscarProductoPorEanOBarcode(String codigo) {
        // VALIDACIÓN INICIAL
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }

        String codigoLimpio = codigo.trim();
        Integer idBodegaUsuario = UserSession.getInstance().getIdBodegaUsuario();
        if (idBodegaUsuario == null || idBodegaUsuario == 0) {
            System.err.println("ERROR: ID de bodega no válido: " + idBodegaUsuario);
            Toast.show(this, Toast.Type.ERROR, "Error: No se pudo determinar tu bodega. Contacta al administrador.");
            return null;
        }
        String sql = "SELECT "
                + "p.id_producto AS id_producto, "
                + "p.codigo_modelo AS codigo_modelo, "
                + "pv.ean, "
                + "pv.sku, "
                + "p.nombre, "
                + "c.nombre AS color_nombre, "
                + "t.numero AS talla_numero, "
                + "t.sistema AS talla_sistema, "
                + "p.precio_venta AS precio_venta, "
                + "p.descripcion, "
                + "pv.id_variante AS id_variante, "
                + "pv.disponible, "
                +
                "COALESCE(ib.Stock_par, 0) AS stock_par, "
                + "COALESCE(ib.Stock_caja, 0) AS stock_caja, "
                + "COALESCE(ib.stock_reservado, 0) AS stock_reservado, "
                + "p.pares_por_caja AS pares_por_caja "
                + "FROM producto_variantes pv "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                +
                "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante "
                + "    AND ib.id_bodega = ? "
                +
                "    AND ib.activo = 1 "
                + "    AND (COALESCE(ib.Stock_par, 0) > 0 OR COALESCE(ib.Stock_caja, 0) > 0) "
                +
                "WHERE "
                +
                "(pv.ean = ? OR pv.sku = ?) "
                + "AND pv.disponible = 1 "
                + "AND p.activo = 1 "
                + "ORDER BY p.id_producto DESC "
                + "LIMIT 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // PREPARAR Y EJECUTAR QUERY
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);

            // Setear parámetros EN ORDEN CORRECTO
            int paramIndex = 1;
            stmt.setInt(paramIndex++, idBodegaUsuario); // Parámetro 1: id_bodega en INNER JOIN
            stmt.setString(paramIndex++, codigoLimpio); // Parámetro 2: ean en WHERE
            stmt.setString(paramIndex++, codigoLimpio); // Parámetro 3: sku en WHERE

            rs = stmt.executeQuery();

            // PROCESAR RESULTADO
            if (rs.next()) {
                // ENCONTRADO CON STOCK
                int stockPar = rs.getInt("stock_par");
                int stockCaja = rs.getInt("stock_caja");
                int stockReservado = rs.getInt("stock_reservado");
                int stockDisponible = Math.max(0, stockPar - stockReservado);

                // Mapear y retornar
                DataSearch producto = mapearResultSetADataSearch(rs, codigoLimpio);
                if (producto != null) {
                    comboTalla.setEnabled(false);
                    comboColor.setEnabled(false);
                    return producto;
                } else {
                    System.err.println("ERROR: Error al mapear producto");
                    return null;
                }
            } else {
                // NO ENCONTRADO O SIN STOCK

                // IMPORTANTE: Mostrar solo una vez
                Toast.show(this, Toast.Type.INFO,
                        "Producto no encontrado o sin stock en tu bodega");

                return null;
            }

        } catch (SQLException e) {
            System.err.println("ERROR: Error SQL buscando producto por código");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Código de error: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();

            Toast.show(this, Toast.Type.ERROR, "Error al buscar producto: " + e.getMessage());
            return null;

        } finally {
            // CERRAR RECURSOS EN ORDEN INVERSO
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
                System.err.println("Error cerrando recursos: " + e.getMessage());
            }
        }
    }

    private DataSearch mapearResultSetADataSearch(ResultSet rs, String codigoBuscado) throws SQLException {
        try {
            // EXTRAER DATOS DEL RESULTSET
            String idProducto = String.valueOf(rs.getInt("id_producto"));
            int idVariante = rs.getInt("id_variante");
            String ean = rs.getString("ean");
            String sku = rs.getString("sku");
            String codigoModelo = rs.getString("codigo_modelo");
            String nombre = rs.getString("nombre");
            String descripcion = rs.getString("descripcion");

            // Variante: Talla y Color
            String colorNombre = rs.getString("color_nombre");
            String tallaNombre = rs.getString("talla_numero");
            String tallaSistema = rs.getString("talla_sistema");

            // Formatear talla con sistema si existe
            String tallaCompleta = tallaNombre;
            if (tallaSistema != null && !tallaSistema.isEmpty()) {
                tallaCompleta = tallaNombre + " " + tallaSistema;
            }

            // Precios
            BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
            BigDecimal precioCompra = null; // No disponible en esta query

            // Stock
            int stockPar = rs.getInt("stock_par");
            int stockCaja = rs.getInt("stock_caja");
            int stockReservado = rs.getInt("stock_reservado");
            int paresPorCaja = rs.getInt("pares_por_caja");

            // Calcular stock disponible
            int stockDisponible = Math.max(0, stockPar - stockReservado);
            // CREAR OBJETO DataSearch
            DataSearch producto = new DataSearch(
                    idProducto, // idprod
                    ean, // EAN
                    sku, // SKU
                    nombre, // nombre
                    colorNombre != null ? colorNombre : "", // color
                    tallaCompleta, // talla
                    false, // startsWithSearch
                    descripcion, // descripcion
                    0, // idCategoria
                    0, // idMarca
                    0, // idProveedor
                    precioCompra != null ? precioCompra : BigDecimal.ZERO, // precioCompra
                    precioVenta, // precioVenta
                    stockDisponible, // stockPorPares (disponible)
                    0, // stockMinimo
                    "", // genero
                    true, // activo
                    null, // imagen (no disponible en esta query)
                    null, // fechaCreacion
                    stockCaja, // stockPorCajas
                    paresPorCaja // paresPorCaja
            );

            // ESTABLECER CAMPOS ADICIONALES
            producto.setIdVariante(idVariante); // CRITICO
            producto.setStock(stockDisponible);

            return producto;

        } catch (SQLException e) {
            System.err.println("ERROR: Error mapeando ResultSet a DataSearch");
            System.err.println("Mensaje: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Selecciona una talla específica en el ComboBox de tallas Busca la talla
     * exacta y la selecciona si existe
     *
     * @param talla Talla a seleccionar (ej: "42", "M", "38 (EU)")
     */
    private void cargarImagenPorVariante(int idVariante) {
        String sql = "SELECT imagen FROM producto_variantes WHERE id_variante = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Blob blob = rs.getBlob("imagen");
                    if (blob != null) {
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        cargarImagenProducto(bytes); // este es el método que te generé antes con byte[]
                        return;
                    }
                }
            }
        } catch (SQLException | IOException e) {
            System.err.println("Error cargando imagen de variante: " + e.getMessage());
        }
        // si algo falla, deja el placeholder
        ICON.setIcon(null);
        ICON.setText("SIN IMAGEN");
    }

    private void seleccionarTallaEnCombo(String talla) {
        if (talla == null || talla.trim().isEmpty()) {
            return;
        }

        if (comboTalla == null || comboTalla.getItemCount() == 0) {
            System.err.println("ERROR: ComboBox de tallas vacío o no inicializado");
            return;
        }

        try {
            boolean tallaEncontrada = false;
            String tallaBuscada = talla.trim();
            // Recorrer todos los items del combo
            for (int i = 0; i < comboTalla.getItemCount(); i++) {
                String itemTalla = comboTalla.getItemAt(i);

                if (itemTalla == null) {
                    continue;
                }

                // Comparación flexible: exacta o contenida
                if (itemTalla.equals(tallaBuscada)
                        || itemTalla.contains(tallaBuscada)
                        || tallaBuscada.contains(itemTalla)) {

                    comboTalla.setSelectedIndex(i);
                    tallaEncontrada = true;
                    break;
                }
            }

            if (!tallaEncontrada) {

                for (int i = 0; i < comboTalla.getItemCount(); i++) {
                }
            }

        } catch (Exception e) {
            System.err.println("ERROR: Error seleccionando talla: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTODO AUXILIAR: Seleccionar Color en ComboBox
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * Selecciona un color específico en el ComboBox de colores Busca el color
     * exacto y lo selecciona si existe
     *
     * @param color Color a seleccionar (ej: "Negro", "Rojo", "Azul Marino")
     */
    private void seleccionarColorEnCombo(String color) {
        if (color == null || color.trim().isEmpty()) {
            return;
        }

        if (comboColor == null || comboColor.getItemCount() == 0) {
            System.err.println("ERROR: ComboBox de colores vacío o no inicializado");
            return;
        }

        try {
            boolean colorEncontrado = false;
            String colorBuscado = color.trim();
            // Recorrer todos los items del combo
            for (int i = 0; i < comboColor.getItemCount(); i++) {
                String itemColor = comboColor.getItemAt(i);

                if (itemColor == null) {
                    continue;
                }

                // Comparación case-insensitive
                if (itemColor.equalsIgnoreCase(colorBuscado)) {
                    comboColor.setSelectedIndex(i);
                    colorEncontrado = true;
                    break;
                }
            }

            if (!colorEncontrado) {

                for (int i = 0; i < comboColor.getItemCount(); i++) {
                }
            }

        } catch (Exception e) {
            System.err.println("ERROR: Error seleccionando color: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTODO AUXILIAR: Cargar Imagen del Producto
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * Carga y muestra la imagen del producto en el componente ICON Si no hay
     * imagen, muestra una imagen por defecto
     *
     * @param producto DataSearch con la información del producto
     */
    private void cargarImagenProducto(DataSearch producto) {
        try {
            Blob imagenBlob = producto.getImagen();

            if (imagenBlob != null && ICON != null) {
                // Convertir Blob a Image
                InputStream is = imagenBlob.getBinaryStream();
                byte[] bytes = is.readAllBytes();
                is.close();

                ImageIcon imageIcon = new ImageIcon(bytes);

                // Escalar imagen para que se ajuste al componente
                Image image = imageIcon.getImage();
                Image scaledImage = image.getScaledInstance(
                        ICON.getWidth(),
                        ICON.getHeight(),
                        Image.SCALE_SMOOTH);

                ICON.setIcon(new ImageIcon(scaledImage));
            } else {
                // Cargar imagen por defecto si no hay imagen del producto
                cargarImagenPorDefecto();
            }

        } catch (Exception e) {
            System.err.println("WARN: Error cargando imagen: " + e.getMessage());
            cargarImagenPorDefecto();
        }
    }

    /**
     * Carga una imagen por defecto cuando no hay imagen del producto
     */
    private void cargarImagenPorDefecto() {
        if (ICON != null) {
            try {
                // Opción 1: Usar un ícono de FontAwesome/Ikonli
                if (ICON instanceof JLabel) {
                    FontIcon iconoProducto = FontIcon.of(
                            FontAwesomeSolid.BOX,
                            80,
                            UIManager.getColor("Component.borderColor"));
                    ICON.setIcon(iconoProducto);
                }

                // Opción 2: Limpiar el ícono
                // ICON.setIcon(null);
                // ICON.setText("Sin imagen");
            } catch (Exception e) {
                System.err.println("WARN: Error cargando imagen por defecto: " + e.getMessage());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTODO AUXILIAR: Limpiar Campos del Producto
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * Limpia todos los campos relacionados con el producto Útil para resetear
     * el formulario después de agregar un producto
     */
    private void limpiarCamposProducto() {
        try {
            // Limpiar selectedProduct
            selectedProduct = null;

            // ==========================================
            // CORRECCIÓN: Resetear cbxTipo explícitamente a "par" (índice 0)
            // ==========================================
            if (cbxTipo != null && cbxTipo.getItemCount() > 0) {
                cbxTipo.setSelectedIndex(0);
            }

            // Limpiar campos de texto
            if (txtNombreProducto != null) {
                txtNombreProducto.setText("");
            }
            if (txtCodigo != null) {
                txtCodigo.setText("");
            }
            if (txtEAN != null) {
                txtEAN.setText("");
            }
            if (txtPrecio != null) {
                txtPrecio.setText("$0.00");
            }
            if (txtStock != null) {
                txtStock.setText("0 pares disponibles");
                txtStock.setForeground(UIManager.getColor("Label.foreground"));
            }
            if (txtDescuento != null) {
                txtDescuento.setText("0%");
            }

            // Resetear cantidad
            if (txtCantidad != null) {
                txtCantidad.setValue(1);
            }

            // Limpiar combos
            if (comboTalla != null) {
                comboTalla.removeAllItems();
                comboTalla.addItem("Seleccionar talla");
            }

            if (comboColor != null) {
                comboColor.removeAllItems();
                comboColor.addItem("Seleccionar color");
            }

            // ==========================================
            // CORRECCIÓN: Resetear cbxTipo explícitamente a "par" (índice 0)
            // ==========================================
            if (cbxTipo != null && cbxTipo.getItemCount() > 0) {
                cbxTipo.setSelectedIndex(0);
            }

            // Limpiar imagen
            if (ICON != null) {
                cargarImagenPorDefecto();
            }

            // Actualizar estado del botón
            actualizarEstiloBtnAgregar();

            // Dar foco al campo de búsqueda
            if (txtIngresarCodigo != null) {
                SwingUtilities.invokeLater(() -> {
                    txtIngresarCodigo.requestFocusInWindow();
                    txtIngresarCodigo.selectAll();
                });
            }
        } catch (Exception e) {
            System.err.println("ERROR: Error limpiando campos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validarCamposAntesDeAgregar() {

        try {
            // VALIDACIÓN 1: Producto seleccionado
            if (selectedProduct == null) {
                Toast.show(this, Toast.Type.WARNING, "Debe seleccionar un producto");
                txtIngresarCodigo.requestFocusInWindow();
                return false;
            }
            // VALIDACIÓN 2: Talla seleccionada
            if (comboTalla.getSelectedIndex() <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Debe seleccionar una talla");
                comboTalla.requestFocusInWindow();
                return false;
            }
            String tallaSeleccionada = (String) comboTalla.getSelectedItem();
            // VALIDACIÓN 3: Color seleccionado
            if (comboColor.getSelectedIndex() <= 0) {
                Toast.show(this, Toast.Type.WARNING, "Debe seleccionar un color");
                comboColor.requestFocusInWindow();
                return false;
            }
            String colorSeleccionado = (String) comboColor.getSelectedItem();
            // VALIDACIÓN 4: Cantidad válida
            int cantidad = 0;
            try {
                cantidad = (Integer) txtCantidad.getValue();
            } catch (Exception e) {
                Toast.show(this, Toast.Type.WARNING, "La cantidad debe ser un número mayor a 0");
                txtCantidad.requestFocusInWindow();
                return false;
            }

            if (cantidad <= 0) {
                Toast.show(this, Toast.Type.WARNING, "La cantidad debe ser mayor a 0");
                txtCantidad.requestFocusInWindow();
                txtCantidad.setValue(1);
                return false;
            }
            // VALIDACIÓN 5: Tipo de venta seleccionado (por defecto "par")
            if (cbxTipo.getSelectedItem() == null) {
                cbxTipo.setSelectedIndex(0); // Seleccionar "par" automáticamente
            }
            String tipo = cbxTipo.getSelectedItem().toString();
            // VALIDACIÓN 6: STOCK DISPONIBLE SEGÚN TIPO
            int stockDisponible = 0;
            String unidad = "pares";

            if ("caja".equalsIgnoreCase(tipo)) {
                stockDisponible = selectedProduct.getStockPorCajas();
                unidad = "cajas";
            } else {
                // Default: par
                stockDisponible = selectedProduct.getStockPorPares();
                unidad = "pares";
            }

            if (cantidad > stockDisponible) {

                Toast.show(this, Toast.Type.WARNING,
                        String.format("Stock insuficiente.\nDisponible: %d %s\nSolicitado: %d %s",
                                stockDisponible, unidad, cantidad, unidad));

                txtCantidad.requestFocusInWindow();
                txtCantidad.setValue(Math.min(cantidad, stockDisponible));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("ERROR: Error en validarCamposAntesDeAgregar: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Nueva carga de productos: limpiarFormulario
    // ====================
    /**
     * Inicializa búsqueda automática con sugerencias
     */
    private void initBusquedaAutomatica() {
        // Initialize debounce timer for client search (300ms delay)
        clientSearchDebounceTimer = new javax.swing.Timer(300, e -> performClientSearch());
        clientSearchDebounceTimer.setRepeats(false);

        txtIngresarCodigo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtIngresarCodigo.getText().trim().isEmpty()) {
                    // updateSearchResultsProd();
                }
            }
        });
    }

    /**
     * Executes client search in a background thread using SwingWorker
     */
    private void performClientSearch() {
        String text = txtCliente.getText().trim();

        // Don't search if text is empty
        if (text.isEmpty()) {
            menu1.setVisible(false);
            return;
        }

        // Use SwingWorker to perform DB search in background
        new SwingWorker<List<DataSearchClient>, Void>() {
            @Override
            protected List<DataSearchClient> doInBackground() throws Exception {
                // This runs in a background thread
                return searchClientes(text);
            }

            @Override
            protected void done() {
                try {
                    // This runs in the Event Dispatch Thread (EDT)
                    List<DataSearchClient> clientes = get();
                    updateClientMenu(clientes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    /**
     * Updates the client search menu with results (must be called on EDT)
     */
    private void updateClientMenu(List<DataSearchClient> clientes) {
        if (clientes == null)
            return;

        searchClient.setDataCliente(clientes, menu1);

        if (searchClient.getItemSize() > 0) {
            menu1.show(txtCliente, 0, txtCliente.getHeight());

            int itemHeight = 35;
            int maxItems = 5;
            int itemsToShow = Math.min(searchClient.getItemSize(), maxItems);
            int menuHeight = (itemsToShow * itemHeight) + 2;

            menu1.setPopupSize(menu1.getWidth(), menuHeight);
        } else {
            menu1.setVisible(false);
        }
    }

    private void updateSearchResultsProd() {
        String text = txtIngresarCodigo.getText().trim().toLowerCase();
        searchProd.setData(search(text), menu);

        if (searchProd.getItemSize() > 0) {
            menu.show(txtIngresarCodigo, 0, txtIngresarCodigo.getHeight());
            menu.setPopupSize(menu.getWidth(), (searchProd.getItemSize() * 35) + 2);
        } else {
            menu.setVisible(false);
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
                + "AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.activo = 1 AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)) "
                + "ORDER BY relevancia, p.nombre "
                + "LIMIT ?";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

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

    /**
     * Crea objeto DataSearch desde ResultSet
     */
    private DataSearch crearDataSearchDesdeResultSet(ResultSet rs, String search) {
        try {
            int idProducto = rs.getInt("id_producto");
            String codigoModelo = rs.getString("codigo_modelo");
            String EAN = rs.getString("ean");
            String nombre = rs.getString("nombre");
            String color = rs.getString("color");
            String talla = rs.getString("talla");
            BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
            ServiceProductVariant.ProductVariantStats stats = serviceVariant.getProductStats(idProducto);
            int stockCajas = stats.totalBoxes;
            int stockPares = stats.totalPairsEquivalent;
            Blob imagen = obtenerImagenPrimeraVariante(idProducto);
            boolean startsWithSearch = search != null
                    && (codigoModelo.toLowerCase().startsWith(search.toLowerCase())
                            || nombre.toLowerCase().startsWith(search.toLowerCase()));
            return new DataSearch(
                    String.valueOf(idProducto), // id_prod
                    EAN, // EAN
                    codigoModelo, // SKU
                    nombre, // nombre
                    color, // color
                    talla, // talla
                    startsWithSearch, // startsWithSearch
                    rs.getString("descripcion"), // descripcion
                    rs.getInt("id_categoria"), // idCategoria
                    rs.getInt("id_marca"), // idMarca
                    rs.getInt("id_proveedor"), // idProveedor
                    rs.getBigDecimal("precio_compra"), // precioCompra
                    precioVenta, // precioVenta
                    stockPares, // stock
                    rs.getInt("stock_minimo"), // stockMinimo
                    rs.getString("genero"), // genero
                    rs.getBoolean("activo"), // activo
                    rs.getTimestamp("fecha_creacion"), // fechaCreacion
                    stockCajas, // stockPorCajas
                    stockPares, // stockPorPares
                    rs.getInt("pares_por_caja") // paresPorCaja
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

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

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

    private byte[] obtenerImagenVariante(int idVariante) {
        String sql = "SELECT imagen FROM producto_variantes WHERE id_variante = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] img = rs.getBytes("imagen");
                    return img;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo imagen de variante: " + e.getMessage());
        }
        return null;
    }

    private void cargarImagenProducto(byte[] imagenBytes) throws IOException {
        if (imagenBytes == null || imagenBytes.length == 0) {
            ICON.setIcon(null);
            ICON.setText("SIN IMAGEN");
            return;
        }

        java.awt.Image img = javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(imagenBytes));
        if (img == null) {
            ICON.setIcon(null);
            ICON.setText("SIN IMAGEN");
            return;
        }

        // Escalar a tamaño del label manteniendo calidad
        java.awt.Image scaled = img.getScaledInstance(
                ICON.getWidth() > 0 ? ICON.getWidth() : 200,
                ICON.getHeight() > 0 ? ICON.getHeight() : 200,
                java.awt.Image.SCALE_SMOOTH);

        ICON.setIcon(new javax.swing.ImageIcon(scaled));
        ICON.setText("");
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
                + "AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_variante = pv.id_variante AND ib.activo = 1 AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)) "
                + "GROUP BY p.id_producto "
                + "ORDER BY total_vendido DESC, p.fecha_creacion DESC "
                + "LIMIT ?";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

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

    private void configurarBusquedaPorCodigo() {
        if (txtIngresarCodigo == null) {
            return;
        }

        txtIngresarCodigo.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    String codigo = txtIngresarCodigo.getText().trim();
                    if (codigo.isEmpty()) {
                        Toast.show(generarVentaFor1.this, Toast.Type.WARNING,
                                "Ingrese un código de barras o EAN");
                        return;
                    }
                    DataSearch producto = buscarProductoPorEanOBarcode(codigo);

                    if (producto != null) {
                        seleccionarProducto(producto);
                        Toast.show(generarVentaFor1.this, Toast.Type.SUCCESS,
                                "Producto encontrado: " + producto.getNombre());
                    } else {
                        Toast.show(generarVentaFor1.this, Toast.Type.ERROR,
                                "Producto no encontrado con código: " + codigo);
                        txtIngresarCodigo.setText("");
                        txtIngresarCodigo.requestFocus();
                    }
                }
            }
        });
    }

    /**
     * Verifica la conexión a la base de datos
     */
    private void verificarConexionBaseDatos() {
        try {
            conexion.getInstance().connectToDatabase();

            Connection conn = conexion.getInstance().createConnection();
            String[] tablasRequeridas = { "productos", "producto_variantes", "tallas", "colores" };

            for (String tabla : tablasRequeridas) {
                String sql = "SELECT COUNT(*) FROM " + tabla + " LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeQuery();
                } catch (SQLException e) {
                    System.err.println("ERROR: Error verificando tabla " + tabla + ": " + e.getMessage());
                }
            }

            try {
                ServiceProductVariant.ProductVariantStats stats = serviceVariant.getProductStats(1);
            } catch (SQLException e) {
                System.err.println("WARN: ServiceProductVariant puede tener problemas: " + e.getMessage());
            }

            conn.close();
        } catch (SQLException e) {
            System.err.println("ERROR: Error conectando a base de datos: " + e.getMessage());
            Toast.show(this, Toast.Type.ERROR,
                    "Error de conexión a base de datos. Verifique que MySQL esté ejecutándose en puerto 3307");
        }
    }
    // ==================== GESTIÓN DE COMBOBOXES ====================

    /**
     * Inicializa los comboboxes con datos de BD
     */
    private void initComboBoxes() {
        cargarTiposPagoDesdeDB();
        cargarEstadosPagoDesdeDB();
        configurarListenerTipoPago();

        cbxEstadoPago.addActionListener(evt -> {
            String estadoSeleccionado = (String) cbxEstadoPago.getSelectedItem();

            // ===================================================================
            // VALIDACIÓN: Verificar selección válida
            // ===================================================================
            if (estadoSeleccionado == null || "Seleccionar".equals(estadoSeleccionado)) {
                return;
            }
            // ===================================================================
            // CASO 1: PAGO PENDIENTE (FIADO)
            // ===================================================================
            if ("pendiente".equalsIgnoreCase(estadoSeleccionado)) {
                manejarPagoPendiente();
            } // ===================================================================
              // CASO 2: PAGO COMPLETADO
              // ===================================================================
            else if ("completada".equalsIgnoreCase(estadoSeleccionado)) {
                manejarPagoCompletado();
            } // ===================================================================
              // CASO 3: VENTA CANCELADA
              // ===================================================================
            else if ("cancelada".equalsIgnoreCase(estadoSeleccionado)) {
                manejarVentaCancelada();
            }
        });
    }

    /**
     * Configura la interfaz para pago pendiente (fiado).
     *
     * RESPONSABILIDAD: Gestionar ventas a crédito con saldo pendiente
     */
    private void manejarPagoPendiente() {
        // ===================================================================
        // 1. VALIDAR QUE HAY PRODUCTOS EN LA VENTA
        // ===================================================================
        if (tablaProductos.getRowCount() == 0) {
            Toast.show(this, Toast.Type.WARNING,
                    "Agregue productos antes de configurar el pago como pendiente");
            cbxEstadoPago.setSelectedIndex(0); // Resetear a "Seleccionar"
            return;
        }

        // ===================================================================
        // 2. VALIDAR QUE HAY CLIENTE SELECCIONADO
        // ===================================================================
        if (selectClient == null) {
            Toast.show(this, Toast.Type.ERROR,
                    "Debe seleccionar un cliente para ventas al crédito");
            cbxEstadoPago.setSelectedIndex(0);
            return;
        }

        // ===================================================================
        // 3. CONFIGURAR INTERFAZ PARA FIADO
        // ===================================================================
        // Deshabilitar configuración de múltiples pagos
        btnConfigurarPagos.setEnabled(false);
        btnConfigurarPagos.setToolTipText("No disponible para pagos pendientes");

        // Ocultar campos de tipo de pago (no aplican en fiado)
        cbxTipoPago.setVisible(false);
        jLabel12.setVisible(false);

        // Ocultar campos de nota de crédito (incompatible con fiado)
        ocultarCamposNotaCredito();

        // ===================================================================
        // 4. ACTUALIZAR LABELS DE TOTALES
        // ===================================================================
        BigDecimal totalVenta = parsearTextoMonetario(txtTotal.getText());

        txtPendiente.setText(MoneyFormatter.format(totalVenta));
        txtPendiente.setForeground(new Color(255, 153, 0)); // Naranja
        txtPendiente.setToolTipText("Saldo pendiente de pago - FIADO");

        // ===================================================================
        // 5. ACTUALIZAR OBSERVACIONES
        // ===================================================================
        String obsActuales = txtObservaciones.getText().trim();
        String nuevaObs = obsActuales;

        if (!obsActuales.contains("VENTA AL CRÉDITO")) {
            if (!obsActuales.isEmpty()) {
                nuevaObs += "\n\n";
            }
            nuevaObs += "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
            nuevaObs += "VENTA AL CRÉDITO (FIADO)\n";
            nuevaObs += "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
            nuevaObs += String.format("Total pendiente: %s\n",
                    MoneyFormatter.format(totalVenta));
            nuevaObs += String.format("Cliente: %s\n", selectClient.getNombre());
            nuevaObs += String.format("DNI: %s\n", selectClient.getDni());
            nuevaObs += String.format("Fecha: %s\n",
                    java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            txtObservaciones.setText(nuevaObs);
        }

        // ===================================================================
        // 6. HABILITAR BOTÓN DE GENERAR VENTA
        // ===================================================================
        btngenerarVenta.setEnabled(true);
        btngenerarVenta.setBackground(new Color(255, 153, 0)); // Naranja para fiado
        btngenerarVenta.setToolTipText("Generar venta con pago pendiente (fiado)");

        // ===================================================================
        // 7. NOTIFICAR AL USUARIO
        // ===================================================================
        String mensaje = String.format(
                "VENTA CONFIGURADA COMO FIADO\n\n"
                        + "Cliente: %s\n"
                        + "Total: %s\n\n"
                        + "El saldo completo quedará pendiente de pago.\n"
                        + "Se registrará como cuenta por cobrar.",
                selectClient.getNombre(),
                MoneyFormatter.format(totalVenta));

        JOptionPane.showMessageDialog(this, mensaje,
                "Venta al Crédito", JOptionPane.INFORMATION_MESSAGE);

    }

    /**
     * Configura la interfaz para pago completado.
     *
     * RESPONSABILIDAD: Validar que el pago esté cubierto completamente
     */
    private void manejarPagoCompletado() {
        // ===================================================================
        // 1. GESTIÓN DE INTERFAZ
        // ===================================================================
        // El botón btnConfigurarPagos se habilita/inhabilita ahora en
        // actualizarTotales() y abrirDialogoMultiplesPagos()
        // ===================================================================
        // 2. VALIDAR QUE EL PAGO ESTÉ CUBIERTO
        // ===================================================================
        BigDecimal totalVenta = parsearTextoMonetario(txtTotal.getText());
        BigDecimal pendiente = parsearTextoMonetario(txtPendiente.getText());

        // Si hay saldo pendiente > 0, advertir al usuario
        if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
            String mensaje = String.format(
                    "HAY SALDO PENDIENTE\n\n"
                            + "Total: %s\n"
                            + "Pendiente: %s\n\n"
                            + "Configure los pagos antes de generar la venta.\n"
                            + "Use el botón 'Configurar Pagos' para especificar\n"
                            + "los medios de pago aplicados.",
                    MoneyFormatter.format(totalVenta),
                    MoneyFormatter.format(pendiente));
            JOptionPane.showMessageDialog(this, mensaje,
                    "Configurar Pagos", JOptionPane.WARNING_MESSAGE);
            // Deshabilitar botón de generar venta hasta que se configure el pago
            btngenerarVenta.setEnabled(false);
            btngenerarVenta.setBackground(new Color(200, 200, 200));
            btngenerarVenta.setToolTipText("Configure el pago completo antes de generar la venta");
        } else {
            // Pago completo - habilitar botón
            btngenerarVenta.setEnabled(true);
            btngenerarVenta.setBackground(new Color(0, 122, 255)); // Azul
            cbxEstadoPago.setSelectedItem("completada");
            cbxEstadoPago.setEnabled(false);
            btngenerarVenta.setToolTipText("Generar venta");
        }

        // Actualizar color del label pendiente
        txtPendiente.setForeground(
                pendiente.compareTo(BigDecimal.ZERO) > 0
                        ? new Color(255, 69, 58) // Rojo
                        : new Color(0, 153, 51) // Verde
        );
    }

    /**
     * Maneja el caso de venta cancelada.
     *
     * RESPONSABILIDAD: Limpiar y deshabilitar la generación de venta
     */
    private void manejarVentaCancelada() {
        JButton btnCancelarVenta = createDialogButton(
                "Cancelar venta",
                svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));
        JButton btnMantener = createDialogButton(
                "Mantener",
                svgIcon("raven/icon/svg/dashboard/check-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

        int confirmacion = showConfirmYesNoDialog(
                this,
                "¿Está seguro de cancelar esta venta?\n\n"
                        + "Se deshabilitará la generación de la venta.",
                "Confirmar Cancelación",
                JOptionPane.WARNING_MESSAGE,
                btnCancelarVenta,
                btnMantener);

        if (confirmacion == JOptionPane.YES_OPTION) {
            // Deshabilitar todo
            btngenerarVenta.setEnabled(false);
            btngenerarVenta.setBackground(new Color(200, 200, 200));
            btngenerarVenta.setToolTipText("Venta cancelada - No se puede generar");

            btnConfigurarPagos.setEnabled(false);

            Toast.show(this, Toast.Type.INFO, "Venta cancelada");
        } else {
            // Restaurar estado anterior
            cbxEstadoPago.setSelectedIndex(0);
        }
    }

    /**
     * Determina el tipo de pago resumen según los medios aplicados.
     *
     * CORRECCIÓN CRÍTICA: Para ventas pendientes (fiado), retorna 'efectivo'
     * porque 'pendiente' NO existe en el ENUM de tipo_pago en la base de datos.
     *
     * El estado real de la venta se determina por la columna 'estado', no
     * 'tipo_pago'.
     *
     * @return String con el tipo de pago compatible con ENUM de BD
     */
    private String determinarTipoPagoResumen() {
        // ====================================================================
        // CASO ESPECIAL: PAGO PENDIENTE (FIADO)
        // ====================================================================
        String estadoPago = (String) cbxEstadoPago.getSelectedItem();

        if ("pendiente".equalsIgnoreCase(estadoPago)) {

            // CRITICO: NO usar "pendiente" porque no existe en ENUM
            // La columna 'estado' es quien marca que es un fiado
            return "efectivo";
        }

        // ====================================================================
        // CASO NORMAL: PAGOS CON MEDIOS DE PAGO
        // ====================================================================
        List<ModelMedioPago> medios = panelMultiplesPagos.getMediosPago();

        // Sin medios de pago configurados
        if (medios == null || medios.isEmpty()) {
            return "efectivo";
        }

        // Un solo medio de pago
        if (medios.size() == 1) {
            String codigo = medios.get(0).getTipo().getCodigo();
            return codigo;
        }

        // Múltiples medios de pago
        return "mixto";
    }

    /**
     * Carga tipos de pago desde ENUM de BD
     */
    private void cargarTiposPagoDesdeDB() {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            con = conexion.getInstance().createConnection();
            String sql = "SHOW COLUMNS FROM ventas LIKE 'tipo_pago'";

            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);

            cbxTipoPago.removeAllItems();
            cbxTipoPago.addItem("Seleccionar");

            if (rs.next()) {
                String type = rs.getString("Type");
                String enumValues = type.substring(type.indexOf('(') + 1, type.indexOf(')'));
                String[] valores = enumValues.split(",");

                for (String valor : valores) {
                    String valorLimpio = valor.trim().replace("'", "");
                    cbxTipoPago.addItem(valorLimpio);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Error cargando tipos de pago: " + e.getMessage());
            cargarTiposPagoPorDefecto();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    /**
     * Carga estados de pago desde ENUM de BD
     */
    private void cargarEstadosPagoDesdeDB() {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            con = conexion.getInstance().createConnection();
            String sql = "SHOW COLUMNS FROM ventas LIKE 'estado'";

            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);

            cbxEstadoPago.removeAllItems();
            cbxEstadoPago.addItem("Seleccionar");

            if (rs.next()) {
                String type = rs.getString("Type");
                String enumValues = type.substring(type.indexOf('(') + 1, type.indexOf(')'));
                String[] valores = enumValues.split(",");

                for (String valor : valores) {
                    String valorLimpio = valor.trim().replace("'", "");
                    cbxEstadoPago.addItem(valorLimpio);
                }
            } else {
                System.err.println("WARN: No se pudo obtener la definición de estado");
                cargarEstadosPagoPorDefecto();
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Error cargando estados de pago: " + e.getMessage());
            cargarEstadosPagoPorDefecto();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    /**
     * Fallback: carga tipos de pago por defecto
     */
    private void cargarTiposPagoPorDefecto() {
        cbxTipoPago.removeAllItems();
        cbxTipoPago.addItem("Seleccionar");
        cbxTipoPago.addItem("efectivo");
        cbxTipoPago.addItem("tarjeta");
        cbxTipoPago.addItem("transferencia");
        cbxTipoPago.addItem("mixto");
    }

    /**
     * Fallback: carga estados de pago por defecto
     */
    private void cargarEstadosPagoPorDefecto() {
        cbxEstadoPago.removeAllItems();
        cbxEstadoPago.addItem("Seleccionar");
        cbxEstadoPago.addItem("pendiente");
        cbxEstadoPago.addItem("completada");
        cbxEstadoPago.addItem("cancelada");
    }

    /**
     * Inicializa listeners de combos de talla y color
     */

    /**
     * Reemplaza el método initComboListeners() existente
     *
     * PROBLEMA IDENTIFICADO: - cargarColoresPorTalla() y cargarTallasPorColor()
     * resetean los combos - Esto dispara nuevos eventos que crean un loop
     *
     * SOLUCIÓN: - Usar flags para prevenir actualizaciones cruzadas - Solo
     * actualizar cuando sea necesario
     */
    /*
     * private void initComboListeners() {
     * comboTalla.addActionListener(evt -> {
     * // CRÍTICO: Solo procesar si no estamos cargando datos
     * if (isLoadingTallas || selectedProduct == null) {
     * return;
     * }
     * int tallaIndex = comboTalla.getSelectedIndex();
     * if (tallaIndex <= 0) {
     * return; // "Talla" placeholder seleccionado
     * }
     * 
     * String tallaSeleccionada = (String) comboTalla.getSelectedItem();
     *
     * 
     * // IMPORTANTE: Guardar el color actual ANTES de recargar
     * String colorActual = null;
     * if (comboColor.getSelectedIndex() > 0) {
     * colorActual = (String) comboColor.getSelectedItem();
     * }
     * 
     * // Cargar colores disponibles para esta talla
     * int idProducto = Integer.parseInt(selectedProduct.getId_prod());
     * cargarColoresPorTalla(idProducto, tallaSeleccionada);
     * 
     * // CLAVE: Intentar restaurar el color si aún está disponible
     * if (colorActual != null) {
     * for (int i = 0; i < comboColor.getItemCount(); i++) {
     * if (comboColor.getItemAt(i).equals(colorActual)) {
     * isLoadingColores = true; // Prevenir eventos
     * comboColor.setSelectedIndex(i);
     * isLoadingColores = false;
     * break;
     * }
     * }
     * }
     * 
     * // Actualizar datos si ambos están seleccionados
     * if (comboColor.getSelectedIndex() > 0) {
     * actualizarDatosProductoCompleto();
     * }
     * 
     * // Actualizar estado del botón agregar
     * actualizarEstiloBtnAgregar();
     * });
     * 
     * comboColor.addActionListener(evt -> {
     * // CRÍTICO: Solo procesar si no estamos cargando datos
     * if (isLoadingColores || selectedProduct == null) {
     * return;
     * }
     * 
     * int colorIndex = comboColor.getSelectedIndex();
     * if (colorIndex <= 0) {
     * return; // "Color" placeholder seleccionado
     * }
     * 
     * String colorSeleccionado = (String) comboColor.getSelectedItem();
     *
     * 
     * // IMPORTANTE: Guardar la talla actual ANTES de recargar
     * String tallaActual = null;
     * if (comboTalla.getSelectedIndex() > 0) {
     * tallaActual = (String) comboTalla.getSelectedItem();
     * }
     * 
     * // Cargar tallas disponibles para este color
     * int idProducto = Integer.parseInt(selectedProduct.getId_prod());
     * cargarTallasPorColor(idProducto, colorSeleccionado);
     * 
     * // CLAVE: Intentar restaurar la talla si aún está disponible
     * if (tallaActual != null) {
     * for (int i = 0; i < comboTalla.getItemCount(); i++) {
     * if (comboTalla.getItemAt(i).equals(tallaActual)) {
     * isLoadingTallas = true; // Prevenir eventos
     * comboTalla.setSelectedIndex(i);
     * isLoadingTallas = false;
     * break;
     * }
     * }
     * }
     * 
     * // Actualizar datos si ambos están seleccionados
     * if (comboTalla.getSelectedIndex() > 0) {
     * actualizarDatosProductoCompleto();
     * }
     * 
     * // Actualizar estado del botón agregar
     * actualizarEstiloBtnAgregar();
     * });
     * }
     */
    /**
     * Reemplaza cargarTallasPorColor() existente Optimizado para evitar
     * reseteos innecesarios
     */
    /**
     * Reemplaza cargarTallasPorColor() existente Optimizado para evitar
     * reseteos innecesarios
     */
    private void cargarTallasPorColor(int idProducto, String colorSeleccionado) {
        isLoadingTallas = true; // Activar flag ANTES de modificar

        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

            // Guardar selección actual para intentar preservarla
            String tallaActualmenteSeleccionada = null;
            if (comboTalla.getSelectedIndex() > 0) {
                tallaActualmenteSeleccionada = (String) comboTalla.getSelectedItem();
            }

            comboTalla.removeAllItems();
            comboTalla.addItem("Talla");

            Set<String> tallasUnicas = new LinkedHashSet<>();

            // Filtrar tallas disponibles para el color seleccionado
            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()
                        && (variant.getStockPairs() > 0 || variant.getStockBoxes() > 0)) {

                    String colorVariant = variant.getColorName();

                    if (colorSeleccionado.equals(colorVariant)) {
                        String talla = construirNombreTalla(variant);
                        if (talla != null && !talla.trim().isEmpty()) {
                            tallasUnicas.add(talla);
                        }
                    }
                }
            }

            // Agregar tallas al combo
            int indexARestaurar = -1;
            int currentIndex = 1;
            for (String talla : tallasUnicas) {
                comboTalla.addItem(talla);

                // Verificar si esta es la talla que estaba seleccionada
                if (talla.equals(tallaActualmenteSeleccionada)) {
                    indexARestaurar = currentIndex;
                }
                currentIndex++;
            }

            // CLAVE: Restaurar selección si la talla aún está disponible
            if (indexARestaurar > 0) {
                comboTalla.setSelectedIndex(indexARestaurar);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando tallas");
        } finally {
            isLoadingTallas = false; // Desactivar flag SIEMPRE
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

    private void cargarImagenProducto(Blob imgBlob) throws IOException {
        if (imgBlob == null) {
            ICON.setIcon(null);
            ICON.setText("SIN IMAGEN");
            return;
        }

        try {
            byte[] imageBytes = imgBlob.getBytes(1, (int) imgBlob.length());
            ImageIcon imageIcon = new ImageIcon(imageBytes);

            int ancho = ICON.getWidth() > 0 ? ICON.getWidth() : 150;
            int alto = ICON.getHeight() > 0 ? ICON.getHeight() : 150;

            Image imagenEscalada = imageIcon.getImage()
                    .getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);

            ICON.setIcon(new ImageIcon(imagenEscalada));
            ICON.setText("");

        } catch (SQLException e) {
            System.err.println("ERROR Error extrayendo imagen: " + e.getMessage());
            ICON.setIcon(null);
            ICON.setText("ERROR IMAGEN");
        }
    }

    private void cargarDatosVarianteEspecifica(int idProducto, String talla, String color) {
        String tipo = (String) cbxTipo.getSelectedItem();
        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

            ModelProductVariant varianteEncontrada = null;

            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable()) {
                    String tallaVariant = construirNombreTalla(variant);
                    String colorVariant = variant.getColorName();

                    if (talla.equals(tallaVariant) && color.equals(colorVariant)) {
                        if (tipo.equals("caja") && variant.getStockBoxes() > 0) {
                            varianteEncontrada = variant;
                            break;
                        } else if (tipo.equals("par") && variant.getStockPairs() > 0) {
                            varianteEncontrada = variant;
                            break;
                        }
                    }
                }
            }

            if (varianteEncontrada != null) {
                BigDecimal precioVenta = BigDecimal.valueOf(
                        varianteEncontrada.getSalePrice() != null
                                ? varianteEncontrada.getSalePrice()
                                : selectedProduct.getPrecioVenta().doubleValue());
                txtNombreProducto.setText(selectedProduct.getNombre());
                txtPrecio.setText("$" + precioVenta);
                txtCodigo.setText("Código: " + selectedProduct.getSKU());

                int stockCajas = varianteEncontrada.getStockBoxes();
                int stockPares = varianteEncontrada.getStockPairs();
                int totalPares = stockPares + (stockCajas * 24);

                txtStock.setText("Stock: Cajas: " + stockCajas + " - Pares: " + stockPares
                        + " | Total: " + totalPares + " pares");

                selectedProduct.setIdVariante(varianteEncontrada.getVariantId());

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
     * Configura el JLabel para manejar texto multilínea usando HTML Principio
     * Single Responsibility: método dedicado a formatear texto
     *
     * @param selectedProduct Producto a mostrar
     */
    private void mostrarNombreProducto(ModelProduct selectedProduct) {
        if (selectedProduct == null || selectedProduct.getName() == null) {
            txtNombreProducto.setText("");
            return;
        }

        String nombre = selectedProduct.getName();

        // Formatear con HTML para saltos automáticos
        String textoHTML = formatearTextoMultilinea(nombre, 200); // 200px de ancho
        txtNombreProducto.setText(textoHTML);
    }

    private void configurarListenerTipoPago() {
        cbxTipoPago.addActionListener(evt -> {
            manejarCambioTipoPago();
        });
    }

    /**
     * Maneja el cambio de selección en el tipo de pago
     *
     * CORRECCIÓN: No ocultar si ya hay una nota aplicada
     */
    private void manejarCambioTipoPago() {
        String tipoPagoSeleccionado = (String) cbxTipoPago.getSelectedItem();

        // Validación de null safety
        if (tipoPagoSeleccionado == null || "Seleccionar".equals(tipoPagoSeleccionado)) {
            // CRITICO: Solo ocultar si NO hay nota aplicada
            if (notaCreditoSeleccionada == null || txtNumeroNotaCredito.isEnabled()) {
                ocultarCamposNotaCredito();
            }
            return;
        }
        // Determinar acción según el tipo de pago
        switch (tipoPagoSeleccionado.toLowerCase()) {
            case "nota_credito":
                mostrarCamposNotaCredito(true);
                break;

            case "mixto":
                btnConfigurarPagos.setVisible(true);
                ocultarCamposNotaCredito();
                break;

            default:
                // CRITICO: Solo ocultar si NO hay nota aplicada
                if (notaCreditoSeleccionada == null || txtNumeroNotaCredito.isEnabled()) {
                    ocultarCamposNotaCredito();
                    btnConfigurarPagos.setVisible(false);
                } else {
                }
                break;
        }

        // Forzar actualización visual
        panelMetod.revalidate();
        panelMetod.repaint();
    }

    /**
     * Muestra y configura los campos para ingresar nota de crédito.
     *
     * @param soloNotaCredito true si el pago es 100% con nota de crédito, false
     *                        si es pago mixto
     *
     *                        PATRÓN: Template Method - Define la estructura del
     *                        proceso de mostrar
     *                        campos
     */
    private void mostrarCamposNotaCredito(boolean soloNotaCredito) {
        // 1. Hacer visibles los campos
        lblNotaCredito.setVisible(true);
        txtNumeroNotaCredito.setVisible(true);
        btnValidar.setVisible(true);

        // 2. Habilitar edición
        txtNumeroNotaCredito.setEnabled(true);
        btnValidar.setEnabled(true);

        // 3. Resetear valores previos
        txtNumeroNotaCredito.setText("");
        notaCreditoSeleccionada = null;

        // 4. Aplicar estilos visuales
        if (soloNotaCredito) {
            // Pago 100% con nota de crédito
            lblNotaCredito.setText("Número de Nota de Crédito (Pago completo)");
            // Deshabilitar otros campos de pago
            cbxEstadoPago.setSelectedItem("completada");
            cbxEstadoPago.setEnabled(false);

        } else {
            // Pago mixto (nota + otro método)
            txtNumeroNotaCredito.setBackground(new Color(255, 255, 200)); // Amarillo claro
            lblNotaCredito.setText("Número de Nota de Crédito (Pago parcial)");
            lblNotaCredito.setForeground(new Color(255, 153, 0)); // Naranja

            // Mantener campos de pago habilitados
            cbxEstadoPago.setEnabled(true);
        }

        // 5. Establecer foco en el campo de número
        txtNumeroNotaCredito.requestFocus();
    }

    /**
     * Oculta y resetea los campos de nota de crédito.
     *
     * CORRECCIÓN: NO eliminar si ya está aplicada y confirmada
     */
    private void ocultarCamposNotaCredito() {
        // ====================================================================
        // CRÍTICO: Si ya hay una nota aplicada y confirmada, NO eliminarla
        // ====================================================================
        if (notaCreditoSeleccionada != null) {
            // Verificar si la nota ya fue aplicada (campos deshabilitados)
            if (!txtNumeroNotaCredito.isEnabled()) {
                return; // ← SALIR sin hacer nada
            }
        }

        // 1. Ocultar componentes
        lblNotaCredito.setVisible(false);
        txtNumeroNotaCredito.setVisible(false);
        btnValidar.setVisible(false);

        // 2. Resetear valores
        txtNumeroNotaCredito.setText("");
        txtNumeroNotaCredito.setEnabled(true);
        btnValidar.setEnabled(true);

        // 3. Limpiar nota seleccionada y restablecer totales
        if (notaCreditoSeleccionada != null) {
            notaCreditoSeleccionada = null;

            // Recalcular totales sin la nota de crédito
            actualizarTotales();
        }

        // 4. Restablecer estilo del label
        lblNotaCredito.setText("Número de Nota de Crédito");
        lblNotaCredito.setForeground(UIManager.getColor("Label.foreground"));
        txtNumeroNotaCredito.setBackground(UIManager.getColor("TextField.background"));

        // 5. Rehabilitar campos de estado si estaban bloqueados
        cbxEstadoPago.setEnabled(true);
    }

    /**
     * Formatea texto para JLabel con salto de línea automático Clean Code:
     * extracción de método para reutilización
     *
     * @param texto       Texto a formatear
     * @param anchoMaximo Ancho máximo en píxeles
     * @return Texto formateado con HTML
     */
    private String formatearTextoMultilinea(String texto, int anchoMaximo) {
        return String.format(
                "<html><div style='width: %dpx;'>%s</div></html>",
                anchoMaximo,
                texto);
    }

    // ==================== GESTIÓN DE COMBOBOXES ====================
    /**
     * Carga colores filtrados por talla seleccionada
     */
    /**
     * Reemplaza cargarColoresPorTalla() existente Optimizado para evitar
     * reseteos innecesarios
     */
    private void cargarColoresPorTalla(int idProducto, String tallaSeleccionada) {

        // ========================================================================
        // VALIDACIÓN CRÍTICA: Verificar que tallaSeleccionada no sea null ni vacía
        // ========================================================================
        if (tallaSeleccionada == null || tallaSeleccionada.trim().isEmpty()) {
            System.err.println("ERROR: Talla seleccionada es NULL o vacía");
            return;
        }

        isLoadingColores = true; // Activar flag ANTES de modificar

        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);
            // Guardar selección actual
            String colorActualmenteSeleccionado = null;
            if (comboColor.getSelectedIndex() > 0) {
                colorActualmenteSeleccionado = (String) comboColor.getSelectedItem();
            }

            comboColor.removeAllItems();
            comboColor.addItem("Color");

            Set<String> coloresUnicos = new LinkedHashSet<>();

            // ====================================================================
            // PROCESO DE FILTRADO CON DEBUG DETALLADO
            // ====================================================================
            int variantesAnalizadas = 0;
            int variantesConStock = 0;
            int variantesCoincidenTalla = 0;

            for (ModelProductVariant variant : variants) {
                variantesAnalizadas++;

                // Verificar disponibilidad y stock
                boolean disponible = variant.isAvailable();
                int stockPares = variant.getStockPairs();
                int stockCajas = variant.getStockBoxes();
                boolean tieneStock = (stockPares > 0 || stockCajas > 0);

                if (disponible && tieneStock) {
                    variantesConStock++;

                    // Construir nombre de talla
                    String tallaVariant = construirNombreTalla(variant);

                    // Debug de la comparación

                    // Comparación exacta
                    if (tallaSeleccionada.equals(tallaVariant)) {
                        variantesCoincidenTalla++;
                        String color = variant.getColorName();

                        if (color != null && !color.trim().isEmpty()) {
                            coloresUnicos.add(color);
                        }
                    }
                }
            }

            // ====================================================================
            // Agregar colores al combo
            // ====================================================================
            int indexARestaurar = -1;
            int currentIndex = 1;

            for (String color : coloresUnicos) {
                comboColor.addItem(color);

                if (color.equals(colorActualmenteSeleccionado)) {
                    indexARestaurar = currentIndex;
                }
                currentIndex++;
            }

            // Restaurar selección si el color aún está disponible
            if (indexARestaurar > 0) {
                comboColor.setSelectedIndex(indexARestaurar);
            } else if (comboColor.getItemCount() == 2) {
                // AUTO-SELECCIÓN: Si solo hay un color disponible (más el placeholder),
                // seleccionarlo automáticamente
                comboColor.setSelectedIndex(1);
            }

        } catch (SQLException e) {
            System.err.println("ERROR SQL cargando colores:");
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando colores");
        } finally {
            isLoadingColores = false; // Desactivar flag SIEMPRE
        }
    }

    private boolean productoExisteSinStock(String codigo) {
        String sql = "SELECT COUNT(*) as total "
                + "FROM producto_variantes pv "
                + "INNER JOIN productos p ON pv.id_producto = p.id_producto "
                + "WHERE (pv.ean = ? OR pv.sku = ?) "
                + "AND pv.disponible = 1 AND p.activo = 1";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);
            stmt.setString(2, codigo);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error verificando existencia: " + e.getMessage());
        }

        return false;
    }

    private void seleccionarProducto(DataSearch producto) {
        if (producto == null) {
            return;
        }

        // ══════════════════════════════════════════════════════════════════════════
        // RESETEAR TIPO A "PAR" AL INICIO
        // Esto asegura que siempre se inicie con "par" por defecto
        // ══════════════════════════════════════════════════════════════════════════
        if (cbxTipo != null && cbxTipo.getItemCount() > 0) {
            cbxTipo.setSelectedIndex(0); // Asegurar "par"
        }
        selectedProduct = producto;
        menu.setVisible(false);
        txtIngresarCodigo.setText(producto.getNombre());

        int idProducto = Integer.parseInt(producto.getId_prod());

        // MOSTRAR PANEL DE INFORMACIÓN
        panelInfoProd.setVisible(true);

        // CARGAR DATOS BÁSICOS
        txtNombreProducto.setText(producto.getNombre());
        txtPrecio.setText(String.format("%.2f", producto.getPrecioVenta()));

        int stockTotalPares = producto.getStockPorPares() + (producto.getStockPorCajas() * 24);
        String stockTexto = String.format("Stock: %d cajas - %d pares - Total %d pares",
                producto.getStockPorCajas(),
                producto.getStockPorPares(),
                stockTotalPares);
        txtStock.setText(stockTexto);

        // CARGAR TALLAS Y COLORES DESDE BD
        cargarTallasProducto(idProducto);
        cargarColoresProducto(idProducto);

        // PRESELECCIONAR COLOR Y TALLA SI VIENEN DEL PRODUCTO
        if (producto.getColor() != null && !producto.getColor().isEmpty()) {
            seleccionarEnCombo(comboColor, producto.getColor());
        }

        if (producto.getTalla() != null && !producto.getTalla().isEmpty()) {
            seleccionarEnCombo(comboTalla, producto.getTalla());
        }

        // INICIALIZAR CANTIDAD EN 1
        if (txtCantidad != null) {
            txtCantidad.setValue(1);
            txtCantidad.requestFocus();
        }

        // ACTUALIZAR BOTÓN AGREGAR
        actualizarEstiloBtnAgregar();

        // FORZAR ACTUALIZACIÓN VISUAL
        panelProd.revalidate();
        panelProd.repaint();
    }

    private void seleccionarProducto(DataSearch producto, boolean esBusquedaPorCodigo) {
        if (producto == null) {
            return;
        }

        // ══════════════════════════════════════════════════════════════════════════
        // 0. RESETEAR TIPO A "PAR" AL INICIO
        // Esto asegura que cualquier lógica de carga de stock vea "par" por defecto
        // ══════════════════════════════════════════════════════════════════════════
        if (cbxTipo != null && cbxTipo.getItemCount() > 0) {
            cbxTipo.setSelectedIndex(0); // Asegurar "par"
        }

        // ══════════════════════════════════════════════════════════════════════════
        // 1. ESTABLECER PRODUCTO SELECCIONADO Y OCULTAR MENÚ
        // ══════════════════════════════════════════════════════════════════════════
        selectedProduct = producto;
        menu.setVisible(false);
        txtIngresarCodigo.setText(producto.getNombre());

        // ══════════════════════════════════════════════════════════════════════════
        // 2. CARGAR TALLAS Y COLORES DISPONIBLES EN LA BODEGA
        // ══════════════════════════════════════════════════════════════════════════
        int idProducto = Integer.parseInt(producto.getId_prod());
        // IMPORTANTE: Cargar PRIMERO colores y tallas
        cargarColoresProducto(idProducto);
        cargarTallasProducto(idProducto);

        // ══════════════════════════════════════════════════════════════════════════
        // 3. MOSTRAR PANEL DE INFORMACIÓN
        // ══════════════════════════════════════════════════════════════════════════
        panelInfoProd.setVisible(true);
        // ══════════════════════════════════════════════════════════════════════════
        // 4. CARGAR INFORMACIÓN BÁSICA DEL PRODUCTO
        // ══════════════════════════════════════════════════════════════════════════
        txtNombreProducto.setText(producto.getNombre());
        txtPrecio.setText("$" + producto.getPrecioVenta());

        // Calcular stock total
        int stockTotalPares = producto.getStockPorPares()
                + (producto.getStockPorCajas() * 24);

        txtStock.setText("Stock: Cajas: " + producto.getStockPorCajas()
                + " - Pares: " + producto.getStockPorPares()
                + " | Total: " + stockTotalPares + " pares");

        // ══════════════════════════════════════════════════════════════════════════
        // 5. CARGAR CÓDIGOS (EAN Y SKU)
        // ══════════════════════════════════════════════════════════════════════════
        String codigoMostrar = "Sin código";

        if (producto.getIdVariante() > 0) {
            String ean = obtenerEanVarianteConValidacion(producto.getIdVariante());
            if (ean != null && !ean.isEmpty()) {
                codigoMostrar = ean;
            }
        }

        if (codigoMostrar.equals("Sin código")
                && producto.getEAN() != null
                && !producto.getEAN().isEmpty()) {
            codigoMostrar = producto.getEAN();
        }

        txtCodigo.setText("Código: " + producto.getSKU());
        txtEAN.setText("EAN: " + codigoMostrar);

        // 6. CARGAR IMAGEN DEL PRODUCTO
        if (producto.getIdVariante() > 0) {
            cargarImagenPorVariante(producto.getIdVariante());
        } else {
            ICON.setIcon(null);
            ICON.setText("SIN IMAGEN");
        }

        // ══════════════════════════════════════════════════════════════════════════
        // 6. CARGAR IMAGEN DEL PRODUCTO
        // ══════════════════════════════════════════════════════════════════════════
        if (producto.getImagen() != null) {
            // Caso 1: La imagen viene en el objeto DataSearch (Blob)
            try {
                cargarImagenProducto(producto.getImagen());
            } catch (IOException ex) {
                System.err.println("ERROR Error cargando imagen desde Blob: " + ex.getMessage());
                // Fallback: Intentar cargar desde BD por variante
                if (producto.getIdVariante() > 0) {
                    cargarImagenPorVariante(producto.getIdVariante());
                } else {
                    cargarImagenPorDefecto();
                }
            }
        } else if (producto.getIdVariante() > 0) {
            // Caso 2: No hay imagen en el objeto, pero tenemos ID de variante
            // Intentar cargar desde la base de datos

            cargarImagenPorVariante(producto.getIdVariante());
        } else {
            // Caso 3: No hay imagen ni variante disponible
            cargarImagenPorDefecto();
        }

        // ══════════════════════════════════════════════════════════════════════════
        // 7. PRESELECCIONAR TALLA Y COLOR (CON DELAY PARA ASEGURAR CARGA)
        // ══════════════════════════════════════════════════════════════════════════
        // CORRECCIÓN CRÍTICA: Usar invokeLater con delay suficiente
        javax.swing.Timer selectionTimer = new javax.swing.Timer(50, e -> {

            // ═════════════════════════════════════════════════════════════════════
            // SELECCIONAR COLOR
            // ═════════════════════════════════════════════════════════════════════
            if (producto.getColor() != null && !producto.getColor().isEmpty()) {

                // Mostrar todos los items disponibles
                for (int i = 0; i < comboColor.getItemCount(); i++) {
                }

                boolean colorSeleccionado = seleccionarEnComboMejorado(
                        comboColor,
                        producto.getColor(),
                        "Color");

                if (colorSeleccionado) {
                } else {
                }
            } else {
            }

            // ═════════════════════════════════════════════════════════════════════
            // SELECCIONAR TALLA
            // ═════════════════════════════════════════════════════════════════════
            if (producto.getTalla() != null && !producto.getTalla().isEmpty()) {

                // Mostrar todos los items disponibles
                for (int i = 0; i < comboTalla.getItemCount(); i++) {
                }

                boolean tallaSeleccionada = seleccionarEnComboMejorado(
                        comboTalla,
                        producto.getTalla(),
                        "Talla");

                if (tallaSeleccionada) {
                } else {
                }
            } else {
            }

        });
        selectionTimer.setRepeats(false);
        selectionTimer.start();

        // ══════════════════════════════════════════════════════════════════════════
        // 8. CONFIGURAR CANTIDAD INICIAL
        // ══════════════════════════════════════════════════════════════════════════
        txtCantidad.setValue(1);
        // El tipo ya se configuró al inicio (Paso 0)

        // Dar foco al campo de cantidad después de un delay
        javax.swing.Timer focusTimer = new javax.swing.Timer(100, e -> txtCantidad.requestFocus());
        focusTimer.setRepeats(false);
        focusTimer.start();

        // ══════════════════════════════════════════════════════════════════════════
        // 9. FORZAR ACTUALIZACIÓN VISUAL
        // ══════════════════════════════════════════════════════════════════════════
        panelProd.revalidate();
        panelProd.repaint();

    }
    // ════════════════════════════════════════════════════════════════════════════
    // MÉTODO AUXILIAR: Extraer Número de Talla
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Extrae el número de talla de un string Ejemplos: - "42 (EU)" → "42" - "42
     * EU H" → "42" - "42" → "42" - "8.5 (US)" → "8.5"
     *
     * @param talla String con la talla completa
     * @return Solo el número de talla
     */
    private String extraerNumeroTalla(String talla) {
        if (talla == null || talla.trim().isEmpty()) {
            return "";
        }

        String limpia = talla.trim();

        // Buscar el primer espacio o paréntesis
        int indexEspacio = limpia.indexOf(' ');
        int indexParentesis = limpia.indexOf('(');

        int index = -1;
        if (indexEspacio > 0 && indexParentesis > 0) {
            index = Math.min(indexEspacio, indexParentesis);
        } else if (indexEspacio > 0) {
            index = indexEspacio;
        } else if (indexParentesis > 0) {
            index = indexParentesis;
        }

        if (index > 0) {
            return limpia.substring(0, index).trim();
        }

        return limpia;
    }

    private boolean seleccionarEnComboMejorado(
            JComboBox<String> combo,
            String valorBuscado,
            String nombreCampo) {

        if (combo == null || valorBuscado == null || valorBuscado.trim().isEmpty()) {
            return false;
        }

        String valorLimpio = valorBuscado.trim();
        // ═══════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 1: Comparación exacta (case-sensitive)
        // ═══════════════════════════════════════════════════════════════════════
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item != null && item.equals(valorLimpio)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 2: Comparación sin case (case-insensitive)
        // ═══════════════════════════════════════════════════════════════════════
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item != null && item.equalsIgnoreCase(valorLimpio)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 3: Contiene el valor (para tallas tipo "42 (EU)")
        // ═══════════════════════════════════════════════════════════════════════
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item != null && item.contains(valorLimpio)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 4: Para tallas - Extraer número y comparar
        // Ej: valorBuscado="42 (EU)" → buscar items que contengan "42"
        // ═══════════════════════════════════════════════════════════════════════
        if (nombreCampo.equalsIgnoreCase("Talla")) {
            String numeroTalla = extraerNumeroTalla(valorLimpio);
            for (int i = 0; i < combo.getItemCount(); i++) {
                String item = combo.getItemAt(i);
                if (item != null) {
                    String numeroItem = extraerNumeroTalla(item);
                    if (numeroTalla.equals(numeroItem)) {
                        combo.setSelectedIndex(i);
                        System.out
                                .println("SUCCESS " + nombreCampo + " encontrado (por número) en índice " + i + ": "
                                        + item);
                        return true;
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // NO ENCONTRADO
        // ═══════════════════════════════════════════════════════════════════════

        for (int i = 0; i < combo.getItemCount(); i++) {
        }

        return false;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTODO ALTERNATIVO: Seleccionar después de cargar combos
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * MÉTODO ALTERNATIVO si el problema persiste Llama a este método DESPUÉS
     * de cargar los combos con un delay más largo
     */
    private void seleccionarTallaYColorConDelay(DataSearch producto) {
        // Timer con delay de 200ms para asegurar que combos están cargados
        javax.swing.Timer timer = new javax.swing.Timer(200, e -> {
            if (producto.getColor() != null && !producto.getColor().isEmpty()) {
                seleccionarEnComboMejorado(comboColor, producto.getColor(), "Color");
            }

            if (producto.getTalla() != null && !producto.getTalla().isEmpty()) {
                seleccionarEnComboMejorado(comboTalla, producto.getTalla(), "Talla");
            }
        });

        timer.setRepeats(false); // Solo ejecutar una vez
        timer.start();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SI EL MÉTODO ORIGINAL seleccionarEnCombo YA EXISTE, REEMPLAZARLO CON ESTE
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * Versión simplificada compatible con el código existente
     */
    private void seleccionarEnCombo(JComboBox<String> combo, String valor) {
        seleccionarEnComboMejorado(combo, valor, "Item");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EJEMPLO DE USO EN cargarTallasProducto (DEBE MOSTRAR "42 EU H")
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * ASEGURAR QUE cargarTallasProducto use el formato correcto
     *
     * Este método debe cargar las tallas en el formato completo: "42 EU H" o
     * "42 (EU)" según tu BD
     */
    private void cargarTallasProducto(int idProducto) {

        try {
            // Limpiar combo
            comboTalla.removeAllItems();
            comboTalla.addItem("Seleccionar talla");

            // Obtener tallas desde el servicio
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();

            if (idBodega == null || idBodega <= 0) {
                System.err.println("ERROR ID de bodega no válido");
                return;
            }

            List<String> tallas = serviceInventarioBodega.obtenerTallasDisponibles(
                    idBodega,
                    idProducto);
            // Agregar tallas al combo
            for (String talla : tallas) {
                comboTalla.addItem(talla);
            }

            if (tallas.isEmpty()) {
            }

        } catch (Exception e) {
            System.err.println("ERROR Error cargando tallas disponibles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EJEMPLO DE USO EN cargarTallasProducto (DEBE MOSTRAR "42 EU H")
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * ASEGURAR QUE cargarTallasProducto use el formato correcto
     *
     * Este método debe cargar las tallas en el formato completo: "42 EU H" o
     * "42 (EU)" según tu BD
     */
    /**
     * Actualiza resultados de búsqueda de clientes
     */
    /**
     * Actualiza resultados de búsqueda de clientes
     * DEPRECATED: Use performClientSearch() instead
     */
    private void updateSearchResultsClient() {
        // Restart the timer on every key press
        if (clientSearchDebounceTimer != null) {
            clientSearchDebounceTimer.restart();
        }
    }

    /**
     * Construye observaciones con detalles de todos los pagos.
     *
     * MODIFICADO: Incluye manejo de pagos pendientes (fiado)
     */
    private String construirObservacionesConPagos() {
        StringBuilder obs = new StringBuilder();

        // 1. Observaciones del usuario
        String obsUsuario = txtObservaciones.getText().trim();
        if (!obsUsuario.isEmpty()) {
            obs.append(obsUsuario).append("\n\n");
        }

        // ====================================================================
        // CASO ESPECIAL: PAGO PENDIENTE (FIADO)
        // ====================================================================
        String estadoPago = (String) cbxEstadoPago.getSelectedItem();
        if ("pendiente".equalsIgnoreCase(estadoPago)) {
            // Ya están las observaciones del fiado, no agregar más
            return obs.toString();
        }

        // ====================================================================
        // CASO NORMAL: DETALLES DE MEDIOS DE PAGO
        // ====================================================================
        List<ModelMedioPago> medios = panelMultiplesPagos.getMediosPago();

        if (!medios.isEmpty()) {
            obs.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            obs.append("MEDIOS DE PAGO\n");
            obs.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            for (int i = 0; i < medios.size(); i++) {
                ModelMedioPago medio = medios.get(i);
                obs.append(String.format("%d. %s: %s\n",
                        i + 1,
                        medio.getTipo().getDescripcion(),
                        MoneyFormatter.format(medio.getMonto())));

                if (medio.getNumeroReferencia() != null
                        && !medio.getNumeroReferencia().isEmpty()) {
                    obs.append(String.format("   Referencia: %s\n",
                            medio.getNumeroReferencia()));
                }

                if (medio.getObservaciones() != null
                        && !medio.getObservaciones().isEmpty()) {
                    obs.append(String.format("   Nota: %s\n",
                            medio.getObservaciones()));
                }

                obs.append("\n");
            }
        }

        // 3. Fecha y hora
        obs.append("Fecha: ").append(LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // 4. Vendedor que realizó la venta
        if (vendedorSeleccionado != null && vendedorSeleccionado.getId() > 0) {
            obs.append("\nVendedor: ").append(vendedorSeleccionado.getNombre());
            obs.append(" (ID: ").append(vendedorSeleccionado.getId()).append(")");
        }

        return obs.toString();
    }

    private void crearNuevaVenta() throws SQLException {
        // ===================================================================
        // VALIDACIÓN CRÍTICA: Verificar sesión ANTES de crear venta
        // ===================================================================
        if (!validarSesionAntesDeVenta()) {
            return;
        }

        Connection conn = null;

        try {
            // ========================================
            // VALIDACIÓN: Verificar caja abierta
            // ========================================
            if (!cajaAbierta || movimientoActual == null) {
                Toast.show(this, Toast.Type.ERROR,
                        "No hay una caja abierta. Debe abrir una caja para generar ventas.");

                JButton btnAbrir = createDialogButton(
                        "Abrir caja",
                        svgIcon("raven/icon/svg/caja.svg", 18, Color.WHITE),
                        dialogButtonStyle(new Color(10, 132, 255), new Color(64, 156, 255), new Color(0, 119, 230)));
                JButton btnCancelar = createDialogButton(
                        "Cancelar",
                        svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                        dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

                int opcion = showConfirmYesNoDialog(
                        this,
                        "¿Desea abrir una caja ahora?",
                        "Caja No Abierta",
                        JOptionPane.QUESTION_MESSAGE,
                        btnAbrir,
                        btnCancelar);

                if (opcion == JOptionPane.YES_OPTION) {
                    solicitarAperturaNuevaCaja();
                    if (!cajaAbierta || movimientoActual == null) {
                        return;
                    }
                }
                if (!cajaAbierta || movimientoActual == null) {
                    return;
                }
            }

            // ========================================
            // VALIDACIÓN: Verificar movimiento sigue abierto
            // ========================================
            ServiceCajaMovimiento serviceCaja = new ServiceCajaMovimiento();
            ModelCajaMovimiento verificacion = serviceCaja.obtenerMovimientoPorId(
                    movimientoActual.getIdMovimiento());

            if (verificacion == null || !verificacion.estaAbierto()) {
                Toast.show(this, Toast.Type.ERROR,
                        "El movimiento de caja fue cerrado. Debe abrir una nueva caja.");

                cajaAbierta = false;
                movimientoActual = null;
                actualizarPanelEstadoCaja();
                // solicitarAperturaCaja();
                return;
            }

            // ========================================
            // VALIDACIÓN CRÍTICA: ESTADO DEL PAGO
            // ========================================
            String estadoPago = (String) cbxEstadoPago.getSelectedItem();
            // ================================================================
            // CASO 1: PAGO PENDIENTE (FIADO) - NO VALIDAR MEDIOS DE PAGO
            // ================================================================
            if ("pendiente".equalsIgnoreCase(estadoPago)) {
                // Confirmar con el usuario
                JButton btnGenerar = createDialogButton(
                        "Generar",
                        svgIcon("raven/icon/svg/dashboard/check-circle.svg", 18, Color.WHITE),
                        dialogButtonStyle(new Color(40, 167, 69), new Color(52, 199, 89), new Color(30, 126, 52)));
                JButton btnCancelar = createDialogButton(
                        "Cancelar",
                        svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                        dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

                int confirmacion = showConfirmYesNoDialog(
                        this,
                        String.format(
                                "¿Generar venta al crédito?\n\n"
                                        + "Cliente: %s\n"
                                        + "Total: %s\n\n"
                                        + "El saldo completo quedará PENDIENTE de pago.",
                                selectClient.getNombre(),
                                MoneyFormatter.format(parsearTextoMonetario(txtTotal.getText()))),
                        "Confirmar Venta al Crédito",
                        JOptionPane.QUESTION_MESSAGE,
                        btnGenerar,
                        btnCancelar);

                if (confirmacion != JOptionPane.YES_OPTION) {
                    return;
                }

                // IMPORTANTE: Continuar sin validar medios de pago
            } // ================================================================
              // CASO 2: PAGO COMPLETADO - VALIDAR MEDIOS DE PAGO
              // ================================================================
            else if ("completada".equalsIgnoreCase(estadoPago)) {
                // Validar que el pago esté cubierto
                if (!panelMultiplesPagos.pagoCompleto()) {
                    BigDecimal pendiente = panelMultiplesPagos.getSaldoPendiente();

                    // MOSTRAR DIÁLOGO SOLO SI NO ES FIADO
                    JButton btnConfigurar = createDialogButton(
                            "Configurar pagos",
                            svgIcon("raven/icon/svg/dashboard/credit-card.svg", 18, Color.WHITE),
                            dialogButtonStyle(new Color(10, 132, 255), new Color(64, 156, 255),
                                    new Color(0, 119, 230)));
                    JButton btnCancelar = createDialogButton(
                            "Cancelar",
                            svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                            dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

                    int respuesta = showConfirmYesNoDialog(
                            this,
                            String.format(
                                    "El pago no está completo.\n\n"
                                            + "Saldo pendiente: %s\n\n"
                                            + "¿Desea abrir la ventana de configuración de pagos?",
                                    MoneyFormatter.format(pendiente)),
                            "Pago Incompleto",
                            JOptionPane.WARNING_MESSAGE,
                            btnConfigurar,
                            btnCancelar);

                    if (respuesta == JOptionPane.YES_OPTION) {
                        abrirDialogoMultiplesPagos();
                    }
                    return; // SALIR sin generar venta
                }
            } // ================================================================
              // CASO 3: VENTA CANCELADA
              // ================================================================
            else if ("cancelada".equalsIgnoreCase(estadoPago)) {
                Toast.show(this, Toast.Type.ERROR,
                        "No se puede generar una venta cancelada");
                return;
            }

            // ========================================
            // 2. INICIAR TRANSACCIÓN
            // ========================================
            conn = conexion.getInstance().getConnection();
            conn.setAutoCommit(false);

            // ========================================
            // 3. CALCULAR TOTALES
            // ========================================
            BigDecimal subtotalProductos = parsearTextoMonetario(txtSubTotal.getText());
            BigDecimal descuentoProductos = parsearTextoMonetario(txtDescuento.getText());
            BigDecimal totalVenta = parsearTextoMonetario(txtTotal.getText());

            // ========================================
            // 4. CREAR MODELO DE VENTA
            // ========================================
            ModelVenta venta = new ModelVenta();
            venta.setCliente(convertirDataSearchClientAModelCliente(selectClient));
            venta.setUsuario(obtenerUsuarioActual());
            venta.setCaja(obtenerCajaActual());
            venta.setMovimiento(movimientoActual);
            venta.setFechaVenta(LocalDateTime.now());

            venta.setSubtotal(subtotalProductos.doubleValue());
            venta.setDescuento(descuentoProductos.doubleValue());
            venta.setTotal(totalVenta.doubleValue());

            // Determinar tipo_pago
            venta.setTipoPago(determinarTipoPagoResumen());

            // Estado del pago
            venta.setEstado(estadoPago);

            // Observaciones
            venta.setObservaciones(construirObservacionesConPagos());

            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object idVar = model.getValueAt(i, COL_ID_VARIANTE);
                if (idVar == null || idVar.toString().isEmpty()) {
                    Toast.show(this, Toast.Type.ERROR,
                            "Fila " + (i + 1) + " tiene producto sin variante válida. Elimine y vuelva a agregar.");
                    return;
                }
            }
            // ========================================
            // 5. CREAR DETALLES DE VENTA
            // ========================================
            List<ModelDetalleVenta> detalles = crearDetallesVenta(venta);
            if (detalles.isEmpty()) {
                conn.rollback();
                Toast.show(this, Toast.Type.ERROR, "No hay productos en la venta");
                return;
            }
            venta.setDetalles(detalles);

            // ========================================
            // 6. GUARDAR VENTA EN BD
            // ========================================
            serviceVenta.crearVenta(venta);
            // ========================================
            // 7. GUARDAR MEDIOS DE PAGO (SOLO SI NO ES FIADO)
            // ========================================
            if (!"pendiente".equalsIgnoreCase(estadoPago)) {
                List<ModelMedioPago> mediosPago = panelMultiplesPagos.getMediosPago();

                // Asignar id_venta a cada medio de pago
                for (ModelMedioPago medio : mediosPago) {
                    medio.setIdVenta(venta.getIdVenta());
                }

                // Guardar medios de pago en BD
                serviceMedioPago.guardarMediosPago(venta.getIdVenta(), mediosPago);
                // Aplicar notas de crédito automáticamente
                for (ModelMedioPago medio : mediosPago) {
                    if (medio.getTipo() == ModelMedioPago.TipoMedioPago.NOTA_CREDITO) {
                        serviceNotaCredito.aplicarNotaCreditoAVenta(
                                medio.getIdNotaCredito(),
                                venta.getIdVenta(),
                                medio.getMonto(),
                                idUsuarioActual);
                    }
                }
            } else {
            }

            // ========================================
            // 8. CONFIRMAR TRANSACCIÓN
            // ========================================
            conn.commit();
            // ========================================
            // 9. MOSTRAR RESULTADO
            // ========================================
            if ("pendiente".equalsIgnoreCase(estadoPago)) {
                mostrarResultadoVentaFiado(venta);
            } else {
                List<ModelMedioPago> mediosPago = panelMultiplesPagos.getMediosPago();
                mostrarResultadoVentaMultiplesPagos(venta, mediosPago);
            }

            // ========================================
            // 10. GENERAR RECIBO
            // ========================================
            // ReciboTirilla reciboTirilla = new ReciboTirilla();
            // boolean exito = reciboTirilla.generarRecibo(venta.getIdVenta());
            ImpresionPOST impresora = new ImpresionPOST("POS-80");

            int idventa = venta.getIdVenta();

            if (impresora.imprimirRecibo(venta.getIdVenta())) {
                Toast.show(this, Toast.Type.SUCCESS, "Recibo de tirilla generado correctamente");
            } else {
                Toast.show(this, Toast.Type.ERROR, "Error al generar recibo de tirilla" + venta.getIdVenta());
                System.err.println("ERROR Error generando recibo para venta ID: " + venta.getIdVenta());
            }
            // Codigo para hacer pruebas de tirrilla
            // if (impresora.generarPreviewTXT(idventa,"Prueba.txt")) {
            // Toast.show(this, Toast.Type.SUCCESS, "Recibo de tirilla generado
            // correctamente");
            //
            // } else {
            // Toast.show(this, Toast.Type.ERROR, "Error al generar recibo de tirilla");
            // System.err.println("ERROR Error generando recibo para venta ID: " +
            // venta.getIdVenta());
            // }

            // ========================================
            // 11. LIMPIAR FORMULARIO
            // ========================================
            limpiarFormulario();
            panelMultiplesPagos.reset();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("ERROR Rollback ejecutado");
                } catch (SQLException ex) {
                    System.err.println("ERROR Error en rollback: " + ex.getMessage());
                }
            }

            System.err.println("ERROR Error creando venta: " + e.getMessage());
            e.printStackTrace();

            Toast.show(this, Toast.Type.ERROR,
                    "Error al crear la venta: " + e.getMessage());
            throw e;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando conexión: " + ex.getMessage());
                }
            }
        }
    }

    private void limpiarFormulario() throws SQLException {
        try {
            // Limpiar recursos
            limpiarRecursos();
            // Navegar al dashboard
            generarVentaFor1 dashboard = new generarVentaFor1();
            Application.showForm(dashboard);
        } catch (Exception e) {
            System.err.println("ERROR Error regresando al dashboard: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al cargar el dashboard:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra el resultado de una venta al crédito (fiado).
     *
     * @param venta Venta generada
     */
    private void mostrarResultadoVentaFiado(ModelVenta venta) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("VENTA AL CRÉDITO GENERADA\n\n");
        mensaje.append("Venta #").append(venta.getIdVenta()).append("\n\n");

        mensaje.append("Cliente: ").append(selectClient.getNombre()).append("\n");
        mensaje.append("DNI: ").append(selectClient.getDni()).append("\n\n");

        mensaje.append("Total: ").append(formatearMoneda(
                BigDecimal.valueOf(venta.getTotal()))).append("\n\n");

        mensaje.append("SALDO PENDIENTE DE PAGO\n");
        mensaje.append("Estado: FIADO\n\n");

        mensaje.append("El cliente debe abonar el total en pagos posteriores.");

        JOptionPane.showMessageDialog(this, mensaje.toString(),
                "Venta al Crédito Generada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarResultadoVentaMultiplesPagos(ModelVenta venta,
            List<ModelMedioPago> mediosPago) {

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("SUCCESS Venta #").append(venta.getIdVenta())
                .append(" generada exitosamente!\n\n");

        mensaje.append("Total: ").append(formatearMoneda(
                BigDecimal.valueOf(venta.getTotal()))).append("\n\n");

        mensaje.append("Medios de pago aplicados:\n");
        for (ModelMedioPago medio : mediosPago) {
            mensaje.append("• ").append(medio.getTipo().getDescripcion())
                    .append(": ").append(formatearMoneda(medio.getMonto()))
                    .append("\n");
        }

        BigDecimal saldoPendiente = panelMultiplesPagos.getSaldoPendiente();
        if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
            mensaje.append("\nVuelto: ")
                    .append(formatearMoneda(saldoPendiente.abs()));
        } else {
            mensaje.append("\nPago completado");
        }

        JOptionPane.showMessageDialog(this, mensaje.toString(),
                "Venta Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Crea los detalles de venta desde la tabla de productos. CORRECCIÓN:
     * Valida que id_variante NUNCA sea null antes de crear el detalle
     */
    /**
     * VERSIÓN MEJORADA - Usa ServiceInventarioBodega V3 Descontar stock
     * correctamente desde inventario_bodega Usa obtenerStockPorVariante() para
     * validación
     */
    private List<ModelDetalleVenta> crearDetallesVenta(ModelVenta venta)
            throws SQLException {

        List<ModelDetalleVenta> detalles = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

        // Obtener ID de bodega del usuario
        Integer idBodegaUsuario = UserSession.getInstance().getIdBodegaUsuario();
        if (idBodegaUsuario == null || idBodegaUsuario == 0) {
            throw new SQLException("ID de bodega inválido del usuario");
        }

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                // ================================================================
                // 1. OBTENER DATOS DE LA TABLA
                // ================================================================
                Object idVarianteObj = model.getValueAt(i, COL_ID_VARIANTE);
                if (idVarianteObj == null || idVarianteObj.toString().isEmpty()) {
                    throw new SQLException("ID Variante nulo en fila " + i);
                }

                int idVariante = Integer.parseInt(idVarianteObj.toString());
                Integer cantidad = (Integer) model.getValueAt(i, COL_CANTIDAD);
                String tipoVenta = (String) model.getValueAt(i, COL_TIPO);

                if (cantidad == null || cantidad <= 0) {
                    throw new SQLException("Cantidad inválida en fila " + i);
                }

                // ================================================================
                // 2. CONSULTAR STOCK EN inventario_bodega USANDO TU SERVICIO V3
                // ================================================================
                ModelInventarioBodega inventario = serviceInventarioBodega
                        .obtenerStockPorVariante(idBodegaUsuario, idVariante);

                if (inventario == null) {
                    throw new SQLException("No hay inventario para variante " + idVariante
                            + " en bodega " + idBodegaUsuario);
                }

                // ================================================================
                // 3. VALIDAR STOCK DISPONIBLE SEGÚN TIPO (PAR O CAJA)
                // ================================================================
                int stockDisponible = 0;
                if ("caja".equalsIgnoreCase(tipoVenta)) {
                    stockDisponible = inventario.getStockDisponibleCajas();
                    if (stockDisponible < cantidad) {
                        String producto = (String) model.getValueAt(i, COL_PRODUCTO);
                        throw new SQLException(
                                "Stock insuficiente de CAJAS para: " + producto
                                        + "\nDisponible: " + stockDisponible
                                        + " Solicitado: " + cantidad);
                    }
                } else {
                    // Tipo = "par" (default)
                    stockDisponible = inventario.getStockDisponiblePares();
                    if (stockDisponible < cantidad) {
                        String producto = (String) model.getValueAt(i, COL_PRODUCTO);
                        throw new SQLException(
                                "Stock insuficiente de PARES para: " + producto
                                        + "\nDisponible: " + stockDisponible
                                        + " Solicitado: " + cantidad);
                    }
                }

                // ================================================================
                // 4. CREAR DETALLE DE VENTA
                // ================================================================
                ModelDetalleVenta detalle = new ModelDetalleVenta();
                detalle.setVenta(venta);
                detalle.setIdVariante(idVariante);

                Object idProductoObj = model.getValueAt(i, COL_ID_PRODUCTO);
                int idProducto = Integer.parseInt(idProductoObj.toString());

                ModelProduct producto = service.getProductById(idProducto);
                if (producto == null) {
                    throw new SQLException("Producto no encontrado: " + idProducto);
                }

                detalle.setProducto(producto);
                detalle.setCantidad(cantidad);
                detalle.setPrecioUnitario(obtenerDoubleSeguro(
                        model.getValueAt(i, COL_PRECIO)));
                detalle.setDescuento(obtenerDoubleSeguro(
                        model.getValueAt(i, COL_DESCUENTO)));
                detalle.setSubtotal(obtenerDoubleSeguro(
                        model.getValueAt(i, COL_SUBTOTAL)));
                detalle.setTipoVenta(tipoVenta.toLowerCase().trim());

                detalles.add(detalle);
            } catch (NumberFormatException e) {
                System.err.println("ERROR Error parseando números en fila " + i + ": " + e.getMessage());
                throw new SQLException("Error en fila " + i + ": " + e.getMessage(), e);
            } catch (SQLException e) {
                System.err.println("ERROR Error SQL en fila " + i + ": " + e.getMessage());
                throw e;
            }
        }
        return detalles;
    }

    /**
     * Aplica la nota de crédito en la base de datos
     */
    private void aplicarNotaCreditoEnBD(int idVenta, BigDecimal montoAplicado) throws SQLException {
        try {
            serviceNotaCredito.aplicarNotaCreditoAVenta(
                    notaCreditoSeleccionada.getIdNotaCredito(),
                    idVenta,
                    montoAplicado,
                    idUsuarioActual);
            // Verificar actualización
            ModelNotaCredito notaVerificacion = serviceNotaCredito.buscarNotaCreditoPorNumero(
                    notaCreditoSeleccionada.getNumeroNotaCredito());

            if (notaVerificacion != null) {

            }

        } catch (SQLException e) {
            System.err.println("ERROR Error aplicando nota de crédito: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Construye observaciones con detalles completos del pago
     */
    private String construirObservacionesConDetallesPago(
            BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
            BigDecimal montoNota, BigDecimal totalPagado, String metodoPago) {

        StringBuilder obs = new StringBuilder();

        // 1. Observaciones del usuario
        String obsUsuario = txtObservaciones.getText().trim();
        if (!obsUsuario.isEmpty()) {
            obs.append(obsUsuario).append("\n\n");
        }

        // 2. Detalles del pago
        obs.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        obs.append("DETALLES DE PAGO\n");
        obs.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        obs.append(String.format("Subtotal:    %s\n", formatearMoneda(subtotal)));
        obs.append(String.format("Descuento:   %s\n", formatearMoneda(descuento)));
        obs.append(String.format("Total:       %s\n\n", formatearMoneda(total)));

        // 3. Detalles de nota de crédito (si aplica)
        if (montoNota.compareTo(BigDecimal.ZERO) > 0) {
            obs.append("NOTA DE CRÉDITO APLICADA\n");
            obs.append(String.format("   Número: %s\n", notaCreditoSeleccionada.getNumeroNotaCredito()));
            obs.append(String.format("   Monto aplicado: %s\n\n", formatearMoneda(montoNota)));
        }

        // 4. Método de pago adicional (si es mixto)
        if (montoNota.compareTo(BigDecimal.ZERO) > 0 && totalPagado.compareTo(BigDecimal.ZERO) > 0) {
            obs.append(String.format("PAGO ADICIONAL (%s)\n", metodoPago.toUpperCase()));
            obs.append(String.format("   Monto: %s\n\n", formatearMoneda(totalPagado)));
        }

        obs.append("Fecha: ").append(LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return obs.toString();
    }

    /**
     * Muestra el resultado de la venta al usuario
     */
    private void mostrarResultadoVenta(BigDecimal total, BigDecimal montoNota, BigDecimal saldoPendiente) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Venta generada exitosamente!\n\n");
        mensaje.append(String.format("Total: %s\n", formatearMoneda(total)));

        if (montoNota.compareTo(BigDecimal.ZERO) > 0) {
            mensaje.append(String.format("Nota de crédito: %s\n", formatearMoneda(montoNota)));
        }

        if (saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            mensaje.append(String.format("\nSaldo pendiente: %s", formatearMoneda(saldoPendiente)));
        } else {
            mensaje.append("\nPago completado");
        }

        JOptionPane.showMessageDialog(this, mensaje.toString(),
                "Venta Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Obtiene la caja actual del sistema CON MANEJO DE ERRORES.
     */
    private ModelCaja obtenerCajaActual() {
        try {
            ServiceCaja serviceCaja = new ServiceCaja();
            ModelCaja cajaCompleta = serviceCaja.obtenerCajaPorId(idCajaActiva);

            if (cajaCompleta != null) {
                return cajaCompleta;
            }
        } catch (SQLException e) {
            System.err.println("WARNING Error obteniendo detalles de caja: " + e.getMessage());
        }

        // Fallback: Caja básica con datos mínimos
        ModelCaja cajaFallback = new ModelCaja();
        cajaFallback.setIdCaja(idCajaActiva);
        cajaFallback.setNombre("Caja #" + idCajaActiva);
        cajaFallback.setUbicacion("Principal");
        cajaFallback.setActiva(true);
        return cajaFallback;
    }

    /**
     * Obtiene usuario actual del sistema
     */
    private ModelUser obtenerUsuarioActual() {
        ModelUser usuario = new ModelUser();
        usuario.setIdUsuario(idUsuarioActual);
        return usuario;
    }

    private ModelCliente convertirDataSearchClientAModelCliente(DataSearchClient data) {
        ModelCliente cliente = new ModelCliente();
        cliente.setIdCliente(data.getIdCliente());
        cliente.setNombre(data.getNombre());
        cliente.setDni(data.getDni());
        cliente.setDireccion(data.getDireccion());
        cliente.setTelefono(data.getTelefono());
        cliente.setEmail(data.getEmail());
        return cliente;
    }

    /**
     * Obtiene double de manera segura desde Object
     */
    private double obtenerDoubleSeguro(Object value) throws NumberFormatException {
        if (value == null) {
            throw new NumberFormatException("Valor nulo encontrado");
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else {
            throw new NumberFormatException("Formato inválido: " + value.toString());
        }
    }

    // SI NECESITAS BUSCAR POR EAN (alternativa):
    private int obtenerIdVariantePorEan(String ean) throws SQLException {
        String sql = "SELECT pv.id_variante FROM producto_variantes pv "
                + "WHERE pv.ean = ? AND pv.disponible = 1 "
                + "LIMIT 1";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, ean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_variante");
                }
            }
        }
        return -1; // No encontrado
    }

    /**
     * Busca clientes por DNI, nombre o código
     */
    private List<DataSearchClient> searchClientes(String search) {
        int limitData = 7;
        List<DataSearchClient> list = new ArrayList<>();

        try {
            ServiceCliente serviceCliente = new ServiceCliente();
            List<ModelCliente> clientesEncontrados = serviceCliente.search(search);

            int count = 0;
            for (ModelCliente cliente : clientesEncontrados) {
                if (count >= limitData) {
                    break;
                }

                boolean startsWithSearch = false;
                if (cliente.getDni() != null
                        && cliente.getDni().toLowerCase().startsWith(search.toLowerCase())) {
                    startsWithSearch = true;
                } else if (cliente.getNombre() != null
                        && cliente.getNombre().toLowerCase().startsWith(search.toLowerCase())) {
                    startsWithSearch = true;
                }

                DataSearchClient data = new DataSearchClient(
                        cliente.getIdCliente(),
                        cliente.getNombre(),
                        cliente.getDni(),
                        cliente.getDireccion(),
                        cliente.getTelefono(),
                        cliente.getEmail(),
                        cliente.getFechaRegistro(),
                        cliente.getPuntosAcumulados(),
                        cliente.isActivo(),
                        startsWithSearch);

                if (startsWithSearch) {
                    list.add(0, data);
                } else {
                    list.add(data);
                }

                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private String obtenerEanVarianteConValidacion(int idVariante) {
        String sql = "SELECT ean, sku FROM producto_variantes WHERE id_variante = ? AND disponible = 1";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVariante);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String ean = rs.getString("ean");
                    String sku = rs.getString("sku");

                    if (ean != null && !ean.trim().isEmpty()) {
                        return ean;
                    } else if (ean != null && !ean.trim().isEmpty()) {
                        return ean;
                    } else {
                        System.err.println("ERROR Ni EAN ni código de barras disponibles para variante " + idVariante);
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR Error obteniendo EAN de variante " + idVariante + ": " + e.getMessage());
        }

        return null;
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
    // Método para limpiar campos del producto seleccionado

    private void limpiarCampos() {
        txtIngresarCodigo.setText("");

        // Resetear el tipo de venta a "par" (índice 0)
        cbxTipo.setSelectedIndex(0); // Volver a "par" (predeterminado)
        cbxTipo.setEnabled(true); // Habilitar para próxima selección
        comboColor.setSelectedIndex(0);
        comboTalla.setSelectedIndex(0);
        txtCantidad.setValue(0);
        panelInfoProd.setVisible(false);
        selectedProduct = null;

    }

    private void cargarTallasPorProducto(int idProducto) {
        isLoadingTallas = true;

        try {
            // Obtener variantes del producto
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

            comboTalla.removeAllItems();
            comboTalla.addItem("Talla");

            Set<String> tallasUnicas = new LinkedHashSet<>();

            // Filtrar variantes disponibles con stock
            for (ModelProductVariant variant : variants) {
                if (variant.isAvailable() && (variant.getStockPairs() > 0 || variant.getStockBoxes() > 0)) {
                    String talla = construirNombreTalla(variant);
                    if (talla != null && !talla.trim().isEmpty()) {
                        tallasUnicas.add(talla);
                    }
                }
            }

            // Agregar tallas al combo
            for (String talla : tallasUnicas) {
                comboTalla.addItem(talla);
            }
            // Actualizar estilo del botón de agregar
            actualizarEstiloBtnAgregar();

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error cargando tallas del producto");
        } finally {
            isLoadingTallas = false;
        }
    }

    /**
     * Carga las variantes de un producto para permitir su selección
     *
     * @param idProducto ID del producto a cargar sus variantes
     */
    private void cargarVariantesProducto(int idProducto) {
        try {
            // Cargar tallas disponibles
            cargarTallasPorProducto(idProducto);

            // Cargar colores disponibles
            cargarColoresProducto(idProducto);

            // Cargar la primera imagen disponible
            Blob imagenPrimera = obtenerImagenPrimeraVariante(idProducto);
            if (imagenPrimera != null) {
                cargarImagenProducto(imagenPrimera);
            } else {
            }

            // Habilitar los combos de talla y color
            comboTalla.setEnabled(true);
            comboColor.setEnabled(true);

            // Actualizar el estilo del botón agregar
            actualizarEstiloBtnAgregar();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error cargando variantes: " + e.getMessage());
        }
    }

    /**
     * Aplica una nota de crédito validada a la venta actual Actualiza
     * correctamente los totales para pago completo y pago mixto
     *
     * @param nota Nota de crédito previamente validada
     *
     *             Principio Single Responsibility: Maneja únicamente la aplicación
     *             de la
     *             nota
     */
    private void aplicarNotaCredito(ModelNotaCredito nota) {
        notaCreditoSeleccionada = nota;

        // ====================================================================
        // 1. OBTENER VALORES ACTUALES
        // ====================================================================
        BigDecimal subtotalActual = parsearTextoMonetario(txtSubTotal.getText());
        BigDecimal descuentoActual = parsearTextoMonetario(txtDescuento.getText());
        // CORRECCIÓN: Leer DIRECTAMENTE del label, NO calcular
        BigDecimal totalActual = parsearTextoMonetario(txtTotal.getText());
        BigDecimal saldoNotaCredito = nota.getSaldoDisponible();

        // ====================================================================
        // 2. VALIDAR TOTAL > 0
        // ====================================================================
        // Validación corregida
        if (totalActual.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this,
                    "El total debe ser mayor a $0 para aplicar una nota de crédito",
                    "Total Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ====================================================================
        // 3. CALCULAR MONTOS SEGÚN COBERTURA
        // ====================================================================
        BigDecimal montoAplicado;
        BigDecimal pendienteFinal;

        // ====================================================================
        // CASO 1: Nota cubre TODO el pago (saldo >= total)
        // ====================================================================
        if (saldoNotaCredito.compareTo(totalActual) >= 0) {
            montoAplicado = totalActual;
            pendienteFinal = BigDecimal.ZERO;
            saldoPendiente = BigDecimal.ZERO;
            BigDecimal saldoRestante = saldoNotaCredito.subtract(montoAplicado);

            // Actualizar UI - Pago Completo
            txtTotal.setText(MoneyFormatter.format(totalActual));
            txtPendiente.setText(MoneyFormatter.format(pendienteFinal));

            // Deshabilitar controles
            txtNumeroNotaCredito.setEnabled(false);
            txtNumeroNotaCredito.setBackground(new Color(240, 255, 240));
            btnValidar.setEnabled(false);

            cbxTipoPago.setSelectedItem("nota_credito");
            cbxTipoPago.setEnabled(false);
            cbxEstadoPago.setSelectedItem("completada");
            cbxEstadoPago.setEnabled(false);
            lblNotaCredito.setText(String.format(
                    "Nota: %s - Restante: %s",
                    MoneyFormatter.format(montoAplicado),
                    MoneyFormatter.format(saldoRestante)));
            lblNotaCredito.setForeground(new Color(0, 153, 51));

            // Mostrar mensaje
            String mensaje = String.format(
                    "PAGO COMPLETO CON NOTA DE CRÉDITO\n\n"
                            + "Nota: %s\n\n"
                            + "Subtotal:      %s\n"
                            + "Descuento:     %s\n"
                            + "Total:         %s\n"
                            + "Nota aplicada: %s\n"
                            + "PENDIENTE:     %s\n"
                            + "Saldo restante: %s\n\n"
                            + "Venta completamente cubierta",
                    nota.getNumeroNotaCredito(),
                    MoneyFormatter.format(subtotalActual),
                    MoneyFormatter.format(descuentoActual),
                    MoneyFormatter.format(totalActual),
                    MoneyFormatter.format(montoAplicado),
                    MoneyFormatter.format(pendienteFinal),
                    MoneyFormatter.format(saldoRestante));

            JOptionPane.showMessageDialog(this, mensaje,
                    "Pago Completo", JOptionPane.INFORMATION_MESSAGE);

            Toast.show(this, Toast.Type.SUCCESS,
                    "Nota aplicada - Venta pagada");

            // ====================================================================
            // CASO 2: Nota NO cubre todo - PAGO MIXTO (saldo < total)
            // ====================================================================
        } else {
            montoAplicado = saldoNotaCredito;
            pendienteFinal = totalActual.subtract(montoAplicado);
            saldoPendiente = pendienteFinal;

            // Actualizar UI - Pago Parcial
            txtTotal.setText(MoneyFormatter.format(totalActual));
            txtPendiente.setText(MoneyFormatter.format(pendienteFinal));

            // Deshabilitar campo de nota
            txtNumeroNotaCredito.setEnabled(false);
            txtNumeroNotaCredito.setBackground(new Color(255, 255, 200));
            btnValidar.setEnabled(false);

            // Habilitar selección de método adicional
            cbxTipoPago.setEnabled(true);
            cbxTipoPago.setSelectedIndex(0);

            // Remover "nota_credito" del combo (no válido en mixto)
            for (int i = 0; i < cbxTipoPago.getItemCount(); i++) {
                if ("nota_credito".equals(cbxTipoPago.getItemAt(i))) {
                    cbxTipoPago.removeItemAt(i);
                    break;
                }
            }

            // Habilitar estado de pago
            cbxEstadoPago.setEnabled(true);
            cbxEstadoPago.setSelectedIndex(0);

            lblNotaCredito.setText(String.format(
                    "Nota: %s - Pendiente: %s",
                    MoneyFormatter.format(montoAplicado),
                    MoneyFormatter.format(pendienteFinal)));
            lblNotaCredito.setForeground(new Color(255, 153, 0));

            // Mostrar mensaje
            String mensaje = String.format(
                    "PAGO MIXTO REQUERIDO\n\n"
                            + "Nota: %s\n\n"
                            + "Subtotal:      %s\n"
                            + "Descuento:     %s\n"
                            + "Total:         %s\n"
                            + "Nota aplicada: %s\n"
                            + "PENDIENTE:     %s\n\n"
                            + "La nota se agotará completamente.\n"
                            + "Seleccione otro método para cubrir\n"
                            + "el saldo de %s\n\n"
                            + "Complete método y estado de pago",
                    nota.getNumeroNotaCredito(),
                    MoneyFormatter.format(subtotalActual),
                    MoneyFormatter.format(descuentoActual),
                    MoneyFormatter.format(totalActual),
                    MoneyFormatter.format(montoAplicado),
                    MoneyFormatter.format(pendienteFinal),
                    MoneyFormatter.format(pendienteFinal));

            JOptionPane.showMessageDialog(this, mensaje,
                    "Pago Mixto Requerido", JOptionPane.WARNING_MESSAGE);

            Toast.show(this, Toast.Type.WARNING,
                    "Pago mixto - Complete el saldo pendiente");
        }
        if (pendienteFinal.compareTo(BigDecimal.ZERO) == 0) {
            // Pago completo - bloquear tipo de pago
            cbxTipoPago.setEnabled(false);
        } else {
            // Pago mixto - mantener habilitado pero advertir
        }

        actualizarTotales(); // <- Llamada al método mejorado

        // ====================================================================
        // 5. FORZAR ACTUALIZACIÓN VISUAL
        // ====================================================================
        panelMetod.revalidate();
        panelMetod.repaint();
        panelFinal.revalidate();
        panelFinal.repaint();

    }

    /**
     * Parsea texto monetario a BigDecimal de forma segura
     *
     * CORRECCIÓN: Elimina TODOS los caracteres no numéricos (incluidos
     * invisibles) excepto dígitos y la coma decimal
     */
    private BigDecimal parsearTextoMonetario(String textoMonetario) {
        // Delegar a la clase centralizada para consistencia
        return MoneyFormatter.parse(textoMonetario);
    }

    private void actualizarEstiloBtnAgregar() {
        try {
            if (cbxTipo != null && cbxTipo.getSelectedItem() == null && cbxTipo.getItemCount() > 0) {
                cbxTipo.setSelectedIndex(0);
            }

            // VALIDACIONES BÁSICAS
            boolean productoSeleccionado = selectedProduct != null;
            boolean tallaSeleccionada = comboTalla.getSelectedIndex() > 0;
            boolean colorSeleccionado = comboColor.getSelectedIndex() > 0;
            boolean tipoValido = cbxTipo.getSelectedItem() != null;
            boolean cantidadValida = ((Integer) txtCantidad.getValue()) > 0;

            // VERIFICAR STOCK DISPONIBLE SEGÚN TIPO
            boolean stockDisponible = false;
            int stockActual = 0;
            String tipoVenta = "par";

            if (selectedProduct != null && tipoValido) {
                Object tipoItem = cbxTipo.getSelectedItem();
                if (tipoItem != null) {
                    tipoVenta = tipoItem.toString().toLowerCase().trim();
                }

                // Mostrar stock según tipo
                if ("caja".equalsIgnoreCase(tipoVenta)) {
                    stockActual = selectedProduct.getStockPorCajas();
                } else {
                    stockActual = selectedProduct.getStockPorPares();
                }

                stockDisponible = stockActual > 0;
            }

            // DETERMINAR ESTADO DEL BOTÓN
            boolean todoValido = productoSeleccionado && tallaSeleccionada && colorSeleccionado
                    && tipoValido && cantidadValida && stockDisponible;

            if (todoValido) {
                // HABILITAR - Azul
                btnAgregarProd.setEnabled(true);
                btnAgregarProd.setBackground(new Color(0, 122, 255));
                btnAgregarProd.setToolTipText("Agregar producto a la venta");
            } else {
                // DESHABILITAR - Gris
                btnAgregarProd.setEnabled(false);
                btnAgregarProd.setBackground(new Color(200, 200, 200));

                // Mostrar razón del deshabilitado
                String razon = "";
                if (!productoSeleccionado) {
                    razon += "Selecciona producto. ";
                }
                if (!tallaSeleccionada) {
                    razon += "Selecciona talla. ";
                }
                if (!colorSeleccionado) {
                    razon += "Selecciona color. ";
                }
                if (!tipoValido) {
                    razon += "Selecciona tipo. ";
                }
                if (!cantidadValida) {
                    razon += "Cantidad > 0. ";
                }
                if (!stockDisponible) {
                    razon += "Stock insuficiente en " + tipoVenta.toUpperCase() + ".";
                }

                btnAgregarProd.setToolTipText(razon.trim());
            }

        } catch (Exception e) {
            System.err.println("ERROR Error actualizando estilo: " + e.getMessage());
            btnAgregarProd.setEnabled(false);
            btnAgregarProd.setBackground(new Color(200, 200, 200));
        }
    }

    /**
     * MÉTODO AUXILIAR - Verifica si el stock cambió al cambiar tipo OPCIONAL:
     * Usar si necesitas más control granular
     */
    private void verificarCambioStockPorTipo(String tipoNuevo) {
        if (selectedProduct == null) {
            return;
        }

        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            if (idBodega == null || idBodega == 0) {
                return;
            }
            int idProducto = Integer.parseInt(selectedProduct.getId_prod());
            int idVariante = selectedProduct.getIdVariante();

            // Obtener inventario actual
            ModelInventarioBodega inventario = serviceInventarioBodega.obtenerStockPorVariante(idBodega, idVariante);

            if (inventario != null) {
                int stockPares = inventario.getStockDisponiblePares();
                int stockCajas = inventario.getStockDisponibleCajas();

                // Mostrar disponibilidad según tipo
                if ("par".equalsIgnoreCase(tipoNuevo)) {
                } else if ("caja".equalsIgnoreCase(tipoNuevo)) {
                }
            }

        } catch (SQLException e) {
            System.err.println("ERROR Error verificando stock: " + e.getMessage());
        }
    }

    private void sincronizarStockAlCambiarTipo() {
        try {
            if (selectedProduct == null) {
                return;
            }

            Object tipoObj = cbxTipo != null ? cbxTipo.getSelectedItem() : null;
            String tipoActual = tipoObj != null ? tipoObj.toString().trim() : "";
            if (tipoActual.isEmpty()) {
                if (cbxTipo != null && cbxTipo.getItemCount() > 0) {
                    cbxTipo.setSelectedIndex(0);
                }
                tipoActual = "par";
            }
            int cantidadActual = (Integer) txtCantidad.getValue();

            int stockDisponible = 0;
            String unidad = "";

            if ("caja".equalsIgnoreCase(tipoActual)) {
                stockDisponible = selectedProduct.getStockPorCajas();
                unidad = "cajas";
            } else {
                stockDisponible = selectedProduct.getStockPorPares();
                unidad = "pares";
            }
            // Si la cantidad actual excede el stock disponible, reducirla
            if (cantidadActual > stockDisponible) {

                txtCantidad.setValue(stockDisponible);
            }

            // Actualizar estilo del botón
            actualizarEstiloBtnAgregar();

        } catch (Exception e) {
            System.err.println("ERROR Error sincronizando stock: " + e.getMessage());
        }
    }

    /**
     * Valida si existen tallas disponibles para un producto y color
     * específicos.
     *
     * PRINCIPIO SRP: Este método tiene una única responsabilidad - verificar
     * disponibilidad.
     *
     * @param idProducto ID del producto a validar
     * @param idColor    ID del color seleccionado
     * @param tipoVenta  Tipo de venta ("caja" o "par")
     * @return true si hay al menos una talla disponible, false en caso
     *         contrario
     */
    private boolean hayTallasDisponibles(int idProducto, int idColor, String tipoVenta) {
        String campoStock = "caja".equalsIgnoreCase(tipoVenta) ? "COALESCE(ib.Stock_caja,0)"
                : "COALESCE(ib.Stock_par,0)";
        Integer idBodega = null;
        try {
            idBodega = UserSession.getInstance().getIdBodegaUsuario();
        } catch (Throwable ignore) {
        }

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS total FROM inventario_bodega ib "
                        + "JOIN producto_variantes pv ON ib.id_variante = pv.id_variante "
                        + "JOIN tallas t ON pv.id_talla = t.id_talla "
                        + "WHERE pv.id_producto = ? AND pv.id_color = ? AND pv.disponible = 1 AND ib.activo = 1 "
                        + "AND " + campoStock + " > 0");
        if (idBodega != null && idBodega > 0) {
            sql.append(" AND ib.id_bodega = ?");
        }

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int idx = 1;
            pst.setInt(idx++, idProducto);
            pst.setInt(idx++, idColor);
            if (idBodega != null && idBodega > 0) {
                pst.setInt(idx++, idBodega);
            }

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    return total > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR Error al validar tallas disponibles: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private int[] obtenerStockVariante(int idProducto, String talla, String color) {
        try {
            List<ModelProductVariant> variants = serviceVariant.getVariantsByProduct(idProducto);

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

    private String obtenerCodigoBarrasVariante(int idVariante) {
        String sql = "SELECT ean FROM producto_variantes WHERE id_variante = ?";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVariante);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ean");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo código de barras de variante: " + e.getMessage());
        }
        return null;
    }

    private int obtenerIdVariante(int idProducto, String talla, String color) {
        String sql = "SELECT pv.id_variante FROM producto_variantes pv "
                + "JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE pv.id_producto = ? AND t.numero = ? AND pv.color_nombre = ? "
                + "AND pv.disponible = 1 LIMIT 1";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProducto);
            stmt.setString(2, talla);
            stmt.setString(3, color);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_variante");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo ID de variante: " + e.getMessage());
        }

        return -1;
    }

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

    public void agregarLineaPorVariante(int idVariante, int cantidad, String tipo) {
        try {
            if (cantidad <= 0) {
                return;
            }
            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
            ServiceProductVariant spv = new ServiceProductVariant();
            ModelProductVariant variant = spv.getVariantById(idVariante, true);
            if (variant == null) {
                return;
            }
            String ean = obtenerEanVarianteConValidacion(idVariante);
            String codigoBarras = obtenerCodigoBarrasVariante(idVariante);
            String identificador = ean != null && !ean.isEmpty() ? ean
                    : codigoBarras != null && !codigoBarras.isEmpty() ? codigoBarras : String.valueOf(idVariante);
            String tallaNombre = variant.getSizeName();
            String tallaSistema = variant.getSizeSystem();
            String tallaGenero = variant.getGender();
            String colorNombre = variant.getColorName();
            String baseNombre = obtenerNombreProductoPorId(variant.getProductId());
            // ═══════════════════════════════════════════════════════════════════════
            // FORMATEO VISUAL MEJORADO PARA FACTURACIÓN DIARIA
            // Formato: NOMBRE - TALLA+SISTEMA GÉNERO COLOR
            // Ejemplo: NIKE AIR 1 - 43EU H NEGRA
            // ═══════════════════════════════════════════════════════════════════════

            // FORMATEO DE GENERO (HOMBRE->H, MUJER->M, NIÑO->N, UNISEX->U)
            String generoAbbr = "";
            if (tallaGenero != null && !tallaGenero.isEmpty()) {
                String g = tallaGenero.toUpperCase().trim();
                if (g.startsWith("HOMBRE") || g.equals("H")) {
                    generoAbbr = "H";
                } else if (g.startsWith("MUJER") || g.equals("M")) {
                    generoAbbr = "M";
                } else if (g.startsWith("NIÑO") || g.startsWith("NINO") || g.equals("N")) {
                    generoAbbr = "N";
                } else if (g.startsWith("NIÑA") || g.startsWith("NINA")) {
                    generoAbbr = "N";
                } else if (g.startsWith("UNISEX") || g.equals("U")) {
                    generoAbbr = "U";
                } else if (!g.isEmpty()) {
                    generoAbbr = g.substring(0, 1);
                }
            }

            // CONSTRUIR STRING OPTIMIZADO: NOMBRE - TALLAEU G COLOR
            StringBuilder sb = new StringBuilder();

            // 1. Nombre del producto en mayúsculas
            if (baseNombre != null && !baseNombre.isEmpty()) {
                sb.append(baseNombre.toUpperCase().trim());
            }

            // 2. Separador principal
            sb.append(" - ");

            // 3. Talla + Sistema (pegados: 43EU)
            boolean tieneInfoVariante = false;
            if (tallaNombre != null && !tallaNombre.isEmpty()) {
                sb.append(tallaNombre.trim());
                // Sistema pegado a la talla (EU, US, UK, etc.)
                if (tallaSistema != null && !tallaSistema.isEmpty()) {
                    sb.append(tallaSistema.toUpperCase().trim());
                }
                tieneInfoVariante = true;
            }

            // 4. Género abreviado
            if (!generoAbbr.isEmpty()) {
                if (tieneInfoVariante) {
                    sb.append(" ");
                }
                sb.append(generoAbbr);
                tieneInfoVariante = true;
            }

            // 5. Color en mayúsculas
            if (colorNombre != null && !colorNombre.isEmpty()) {
                if (tieneInfoVariante) {
                    sb.append(" ");
                }
                sb.append(colorNombre.toUpperCase().trim());
            }

            String nombreCompleto = sb.toString().trim();
            java.math.BigDecimal precioUnitario = variant.getSalePrice() != null
                    ? java.math.BigDecimal.valueOf(variant.getSalePrice())
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal subtotal = "caja".equals(tipo)
                    ? precioUnitario.multiply(java.math.BigDecimal.valueOf(cantidad * 24L))
                    : precioUnitario.multiply(java.math.BigDecimal.valueOf(cantidad));
            int idProducto = variant.getProductId();

            // Agregar fila con las 11 columnas correctas
            model.addRow(new Object[] {
                    identificador, // 0: Código
                    nombreCompleto, // 1: Producto
                    tipo != null ? tipo : "par", // 2: Tipo
                    cantidad, // 3: Cantidad
                    precioUnitario, // 4: Precio
                    java.math.BigDecimal.ZERO, // 5: Descuento
                    subtotal, // 6: Subtotal
                    null, // 7: Acción
                    null, // 8: id_detalle
                    idVariante, // 9: id_variante
                    idProducto // 10: id_producto
            });
            actualizarContadorProductos();

            actualizarTotales();
        } catch (Exception e) {
            System.err.println("ERROR ERROR AGREGANDO PRODUCTO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String obtenerNombreProductoPorId(int idProducto) {
        String sql = "SELECT nombre FROM productos WHERE id_producto = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString(1);
                    return nombre != null ? nombre : "Producto";
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo nombre de producto: " + e.getMessage());
        }
        return "Producto";
    }

    /**
     * Obtiene el id_producto desde un id_variante
     * 
     * @param idVariante ID de la variante
     * @return id_producto o 0 si no se encuentra
     */
    private int obtenerIdProductoPorVariante(int idVariante) {
        String sql = "SELECT id_producto FROM producto_variantes WHERE id_variante = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_producto");
                }
            }
        } catch (SQLException e) {
            System.err
                    .println("ERROR Error obteniendo id_producto para variante " + idVariante + ": " + e.getMessage());
        }
        return 0;
    }

    private int obtenerIdColor(String nombreColor) {
        String sql = "SELECT id_color FROM colores WHERE nombre = ?";
        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, nombreColor);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int idColor = rs.getInt("id_color");
                    return idColor;
                } else {
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // Color no encontrado
    }

    // ========================================================================
    // MÉTODOS PARA ANIMACIÓN DE LOADING EN BOTÓN GENERAR VENTA
    // ========================================================================

    /**
     * Inicia el estado de loading en el botón de generar venta con animación
     * profesional
     */
    private void iniciarLoadingButton() {
        if (btngenerarVenta == null)
            return;

        // Guardar estado original
        originalButtonText = btngenerarVenta.getText();
        originalButtonIcon = btngenerarVenta.getIcon();
        isProcessingVenta = true;

        // Deshabilitar el botón para prevenir doble clic
        btngenerarVenta.setEnabled(false);

        // Cambiar el cursor a espera
        btngenerarVenta.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // Cambiar texto del botón
        btngenerarVenta.setText("Procesando venta...");

        // Cambiar color a un tono más oscuro/profesional
        btngenerarVenta.setBackground(new Color(34, 153, 84)); // Verde más oscuro

        // Inicializar el ángulo del spinner
        spinnerAngle = 0;

        // Crear icono de spinner animado
        spinnerTimer = new javax.swing.Timer(50, e -> {
            spinnerAngle = (spinnerAngle + 15) % 360;
            btngenerarVenta.setIcon(createSpinnerIcon(spinnerAngle));
            btngenerarVenta.repaint();
        });
        spinnerTimer.start();
    }

    /**
     * Detiene el estado de loading y restaura el botón a su estado original
     */
    private void detenerLoadingButton() {
        if (btngenerarVenta == null)
            return;

        // Detener el timer de animación
        if (spinnerTimer != null && spinnerTimer.isRunning()) {
            spinnerTimer.stop();
        }

        // Restaurar estado original
        btngenerarVenta.setText(originalButtonText);
        btngenerarVenta.setIcon(originalButtonIcon);
        btngenerarVenta.setEnabled(true);
        btngenerarVenta.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Restaurar color original
        btngenerarVenta.setBackground(new Color(39, 174, 96)); // Verde original

        isProcessingVenta = false;

        btngenerarVenta.repaint();
    }

    /**
     * Crea un icono de spinner animado para mostrar en el botón
     * 
     * @param angle Ángulo actual de rotación del spinner
     * @return Icon del spinner rotado
     */
    private javax.swing.Icon createSpinnerIcon(int angle) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();

                // Activar antialiasing para mejor calidad visual
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 16;
                int centerX = x + size / 2;
                int centerY = y + size / 2;

                // Rotar el contexto gráfico
                g2.rotate(Math.toRadians(angle), centerX, centerY);

                // Dibujar el spinner circular con segmentos de diferentes opacidades
                int numBars = 8;
                for (int i = 0; i < numBars; i++) {
                    float alpha = 1.0f - (i / (float) numBars);
                    g2.setColor(new Color(255, 255, 255, (int) (alpha * 255)));

                    double angleRad = Math.toRadians(i * (360.0 / numBars));
                    int barX = (int) (centerX + Math.cos(angleRad) * (size / 2 - 2));
                    int barY = (int) (centerY + Math.sin(angleRad) * (size / 2 - 2));

                    g2.fillOval(barX - 2, barY - 2, 4, 4);
                }

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    /**
     *
     * /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        panelMain = new javax.swing.JPanel();
        panelTitulo = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtEstadoCaja = new javax.swing.JLabel();
        panelEstadoCaj = new javax.swing.JPanel();
        txtMovimientoCaja = new javax.swing.JLabel();
        btnGastos = new raven.componentes.GradientButton();
        btnCerrarCaja = new raven.componentes.GradientButton();
        btnBloquear = new raven.componentes.GradientButton();
        panelFinal = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        txtSubTotal = new javax.swing.JLabel();
        txtDescuento = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        txtPendiente = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        btnLimpiar = new javax.swing.JButton();
        btngenerarVenta = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        txtTotal = new javax.swing.JLabel();
        panelProductos = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        tablaProductos = new javax.swing.JTable();
        jLabel14 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        panelCliente = new javax.swing.JPanel();
        panelInfoCliente = new javax.swing.JPanel();
        txtNombreCliente = new javax.swing.JLabel();
        txtDatosCliente = new javax.swing.JLabel();
        txtCliente = new javax.swing.JTextField();
        btnSeleccionarCliente = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        panelProd = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txtIngresarCodigo = new javax.swing.JTextField();
        panelInfoProd = new javax.swing.JPanel();
        ICON = new javax.swing.JLabel();
        txtPrecio = new javax.swing.JLabel();
        txtNombreProducto = new javax.swing.JLabel();
        txtCodigo = new javax.swing.JLabel();
        txtStock = new javax.swing.JLabel();
        txtEAN = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        comboTalla = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        cbxTipo = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        txtCantidad = new javax.swing.JSpinner();
        btnAgregarProd = new javax.swing.JButton();
        comboColor = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        btnBuscarProducto = new javax.swing.JButton();
        panelMetod = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        jLabel11 = new javax.swing.JLabel();
        cbxTipoPago = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        cbxEstadoPago = new javax.swing.JComboBox<>();
        txtNumeroNotaCredito = new javax.swing.JTextField();
        lblNotaCredito = new javax.swing.JLabel();
        btnValidar = new javax.swing.JButton();
        txtNotaCredito = new javax.swing.JLabel();
        btnConfigurarPagos = new javax.swing.JButton();

        jButton1.setText("jButton1");

        panelTitulo.setBackground(new java.awt.Color(204, 255, 204));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel1.setText("Nueva venta");

        txtMovimientoCaja.setText("Mov: ");

        btnGastos.setText("Gastos");
        btnGastos.setColorFin(new java.awt.Color(217, 97, 25));
        btnGastos.setColorInicio(new java.awt.Color(245, 158, 11));
        btnGastos.setRippleColor(new java.awt.Color(255, 189, 13));
        btnGastos.setShadowColor(new java.awt.Color(255, 189, 13));
        btnGastos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGastosActionPerformed(evt);
            }
        });

        btnCerrarCaja.setText("Cerrar Caja");
        btnCerrarCaja.setColorFin(new java.awt.Color(124, 58, 237));
        btnCerrarCaja.setColorInicio(new java.awt.Color(0, 20, 122));
        btnCerrarCaja.setRippleColor(new java.awt.Color(255, 189, 13));
        btnCerrarCaja.setShadowColor(new java.awt.Color(255, 189, 13));
        btnCerrarCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarCajaActionPerformed(evt);
            }
        });

        btnBloquear.setText("Bloquear");
        btnBloquear.setColorFin(new java.awt.Color(217, 0, 62));
        btnBloquear.setColorInicio(new java.awt.Color(220, 38, 38));
        btnBloquear.setRippleColor(new java.awt.Color(255, 189, 13));
        btnBloquear.setShadowColor(new java.awt.Color(255, 189, 13));
        btnBloquear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBloquearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelEstadoCajLayout = new javax.swing.GroupLayout(panelEstadoCaj);
        panelEstadoCaj.setLayout(panelEstadoCajLayout);
        panelEstadoCajLayout.setHorizontalGroup(
                panelEstadoCajLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelEstadoCajLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(txtMovimientoCaja, 0, 252, Short.MAX_VALUE)
                                .addGap(24, 24, 24)
                                .addComponent(btnGastos, javax.swing.GroupLayout.PREFERRED_SIZE, 124,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnBloquear, javax.swing.GroupLayout.PREFERRED_SIZE, 124,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnCerrarCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 124,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));
        panelEstadoCajLayout.setVerticalGroup(
                panelEstadoCajLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelEstadoCajLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelEstadoCajLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelEstadoCajLayout.createSequentialGroup()
                                                .addGroup(panelEstadoCajLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(btnGastos, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnCerrarCaja,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnBloquear,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(txtMovimientoCaja, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));

        javax.swing.GroupLayout panelTituloLayout = new javax.swing.GroupLayout(panelTitulo);
        panelTitulo.setLayout(panelTituloLayout);
        panelTituloLayout.setHorizontalGroup(
                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelTituloLayout.createSequentialGroup()
                                .addGap(23, 23, 23)
                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtEstadoCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 293,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 0,
                                        Short.MAX_VALUE)
                                .addComponent(panelEstadoCaj, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));
        panelTituloLayout.setVerticalGroup(
                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelTituloLayout.createSequentialGroup()
                                .addContainerGap(15, Short.MAX_VALUE)
                                .addGroup(panelTituloLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelEstadoCaj, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtEstadoCaja, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)));

        panelFinal.setBackground(new java.awt.Color(51, 153, 255));

        jLabel15.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel15.setText("Subtotal");

        txtSubTotal.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        txtSubTotal.setText("$0");

        txtDescuento.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        txtDescuento.setText("$0");

        jLabel18.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel18.setText("Descuento");

        txtPendiente.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        txtPendiente.setText("$0");
        txtPendiente.setToolTipText("");

        jLabel20.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel20.setText("Total ");

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        btngenerarVenta.setText("Generar venta");
        btngenerarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btngenerarVentaActionPerformed(evt);
            }
        });

        jLabel13.setText("Pendiente");

        txtTotal.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        txtTotal.setText("$0");

        javax.swing.GroupLayout panelFinalLayout = new javax.swing.GroupLayout(panelFinal);
        panelFinal.setLayout(panelFinalLayout);
        panelFinalLayout.setHorizontalGroup(
                panelFinalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFinalLayout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addGroup(panelFinalLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtSubTotal, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelFinalLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtTotal, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        panelFinalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel13)
                                                .addComponent(txtPendiente, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelFinalLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtDescuento, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 197,
                                        Short.MAX_VALUE)
                                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btngenerarVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(205, 205, 205)));
        panelFinalLayout.setVerticalGroup(
                panelFinalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFinalLayout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(panelFinalLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelFinalLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 58,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btngenerarVenta, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        58, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelFinalLayout.createSequentialGroup()
                                                .addComponent(jLabel15)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSubTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 27,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel20)
                                        .addComponent(jLabel13)
                                        .addGroup(panelFinalLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(panelFinalLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelFinalLayout.createSequentialGroup()
                                                                .addGap(26, 26, 26)
                                                                .addComponent(txtDescuento))
                                                        .addComponent(jLabel18))
                                                .addGroup(panelFinalLayout.createSequentialGroup()
                                                        .addGap(26, 26, 26)
                                                        .addGroup(panelFinalLayout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(txtTotal)
                                                                .addComponent(txtPendiente)))))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        tablaProductos.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null }
                },
                new String[] {
                        "Codigo", "Producto", "Tipo", "Cantidad", "Precio", "Descuento", "Subtotal", "Id"
                }) {
            Class[] types = new Class[] {
                    java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, true, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tablaProductos.setFocusable(false);
        jScrollPane3.setViewportView(tablaProductos);
        // Configurar scrollbars para la tabla de productos
        jScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane3.getVerticalScrollBar().setUnitIncrement(20); // Optimización de scroll
        jScrollPane3.getHorizontalScrollBar().setUnitIncrement(20);
        tablaProductos.setFillsViewportHeight(true);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        jLabel14.setText("Productos Agregados ( 0)");

        javax.swing.GroupLayout panelProductosLayout = new javax.swing.GroupLayout(panelProductos);
        panelProductos.setLayout(panelProductosLayout);
        panelProductosLayout.setHorizontalGroup(
                panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProductosLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(panelProductosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelProductosLayout.createSequentialGroup()
                                                .addComponent(jLabel14)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1310,
                                                Short.MAX_VALUE))
                                .addContainerGap()));
        panelProductosLayout.setVerticalGroup(
                panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProductosLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 687, Short.MAX_VALUE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panelInfoCliente.setBackground(new java.awt.Color(0, 102, 102));

        txtNombreCliente.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtNombreCliente.setText("Nombre del cliente");

        txtDatosCliente.setText("DNI:    Email:");

        javax.swing.GroupLayout panelInfoClienteLayout = new javax.swing.GroupLayout(panelInfoCliente);
        panelInfoCliente.setLayout(panelInfoClienteLayout);
        panelInfoClienteLayout.setHorizontalGroup(
                panelInfoClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelInfoClienteLayout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addGroup(panelInfoClienteLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtDatosCliente, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelInfoClienteLayout.createSequentialGroup()
                                                .addComponent(txtNombreCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        129, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap()));
        panelInfoClienteLayout.setVerticalGroup(
                panelInfoClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelInfoClienteLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(txtNombreCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtDatosCliente)
                                .addContainerGap(8, Short.MAX_VALUE)));

        txtCliente.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtClienteKeyReleased(evt);
            }
        });

        btnSeleccionarCliente.setText("Seleccionar");
        btnSeleccionarCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSeleccionarClienteActionPerformed(evt);
            }
        });

        // Inicialización de componentes del vendedor
        lblVendedor = new javax.swing.JLabel("Vendedor");
        lblVendedor.setFont(new java.awt.Font("Segoe UI", 0, 14));
        cbxVendedor = new javax.swing.JComboBox<>();

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setText("Buscar Cliente ");

        javax.swing.GroupLayout panelClienteLayout = new javax.swing.GroupLayout(panelCliente);
        panelCliente.setLayout(panelClienteLayout);
        panelClienteLayout.setHorizontalGroup(
                panelClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelClienteLayout.createSequentialGroup()
                                .addGap(43, 43, 43)
                                .addGroup(panelClienteLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2)
                                        .addGroup(panelClienteLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(panelInfoCliente, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnSeleccionarCliente,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 263,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 263,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(lblVendedor)
                                                .addComponent(cbxVendedor, javax.swing.GroupLayout.PREFERRED_SIZE, 263,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(42, Short.MAX_VALUE)));
        panelClienteLayout.setVerticalGroup(
                panelClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelClienteLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLabel2)
                                .addGap(10, 10, 10)
                                .addComponent(panelInfoCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 42,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnSeleccionarCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblVendedor)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxVendedor, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(19, 19, 19)));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setText("Buscar Producto ");

        txtIngresarCodigo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIngresarCodigoActionPerformed(evt);
            }
        });
        txtIngresarCodigo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtIngresarCodigoKeyReleased(evt);
            }

            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtIngresarCodigoKeyTyped(evt);
            }
        });

        panelInfoProd.setBackground(new java.awt.Color(0, 153, 204));

        ICON.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ICON.setText("icono");

        txtPrecio.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        txtPrecio.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtPrecio.setText("$1.000.000 ");

        txtNombreProducto.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        txtNombreProducto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtNombreProducto.setText("Nike Air Force ");

        txtCodigo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtCodigo.setText("Código: 77005020501 ");

        txtStock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStock.setText(" Stock: 24 pares - 24 cajas");

        txtEAN.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtEAN.setText("EAN: 77005020501 ");

        javax.swing.GroupLayout panelInfoProdLayout = new javax.swing.GroupLayout(panelInfoProd);
        panelInfoProd.setLayout(panelInfoProdLayout);
        panelInfoProdLayout.setHorizontalGroup(
                panelInfoProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelInfoProdLayout.createSequentialGroup()
                                .addGroup(panelInfoProdLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelInfoProdLayout.createSequentialGroup()
                                                .addGap(92, 92, 92)
                                                .addComponent(ICON, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(panelInfoProdLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(panelInfoProdLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtCodigo,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(txtNombreProducto,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(txtPrecio,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addGroup(panelInfoProdLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(txtStock, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(panelInfoProdLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(txtEAN, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap()));
        panelInfoProdLayout.setVerticalGroup(
                panelInfoProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelInfoProdLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(ICON, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtNombreProducto)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtPrecio)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtCodigo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtEAN)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtStock)
                                .addGap(18, 18, 18)));

        jLabel5.setText("Talla");

        comboTalla.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar" }));
        comboTalla.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboTallaItemStateChanged(evt);
            }
        });

        jLabel7.setText("Tipo");

        cbxTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "par", "caja" }));
        cbxTipo.setSelectedIndex(0); // Asegurar que "par" esté seleccionado por defecto
        cbxTipo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbxTipoItemStateChanged(evt);
            }
        });
        cbxTipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxTipoActionPerformed(evt);
            }
        });

        jLabel8.setText("Cantidad");

        txtCantidad.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        txtCantidad.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }

            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                txtCantidadInputMethodTextChanged(evt);
            }
        });

        btnAgregarProd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarProdActionPerformed(evt);
            }
        });

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

        jLabel6.setText("Color");

        btnBuscarProducto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarProductoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelProdLayout = new javax.swing.GroupLayout(panelProd);
        panelProd.setLayout(panelProdLayout);
        panelProdLayout.setHorizontalGroup(
                panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProdLayout.createSequentialGroup()
                                .addGap(23, 23, 23)
                                .addGroup(panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelProdLayout.createSequentialGroup()
                                                .addComponent(txtIngresarCodigo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        263, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnBuscarProducto, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        0, Short.MAX_VALUE))
                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelInfoProd, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelProdLayout.createSequentialGroup()
                                                .addGroup(panelProdLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(comboColor,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 131,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(comboTalla,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 131,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(panelProdLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(txtCantidad)
                                                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(cbxTipo, 0, 120, Short.MAX_VALUE)))
                                        .addComponent(btnAgregarProd, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(33, 33, 33)));
        panelProdLayout.setVerticalGroup(
                panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProdLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelProdLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtIngresarCodigo, javax.swing.GroupLayout.DEFAULT_SIZE, 40,
                                                Short.MAX_VALUE)
                                        .addComponent(btnBuscarProducto, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addComponent(panelInfoProd, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(29, 29, 29)
                                .addGroup(
                                        panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel6)
                                                .addComponent(jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(comboColor, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cbxTipo))
                                .addGap(10, 10, 10)
                                .addGroup(panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        panelProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(txtCantidad, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(comboTalla, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(btnAgregarProd, javax.swing.GroupLayout.PREFERRED_SIZE, 50,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel9.setText("Detalles Adicionales");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        jLabel10.setText("Observaciones");

        txtObservaciones.setColumns(20);
        txtObservaciones.setRows(5);
        jScrollPane2.setViewportView(txtObservaciones);

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        jLabel11.setText("Estado del Pago ");

        cbxTipoPago.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbxTipoPago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxTipoPagoActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        jLabel12.setText("Método de Pago");

        cbxEstadoPago.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbxEstadoPago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxEstadoPagoActionPerformed(evt);
            }
        });

        lblNotaCredito.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        lblNotaCredito.setText("Número de Nota de Crédito");

        btnValidar.setText("Validar");
        btnValidar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnValidarActionPerformed(evt);
            }
        });

        btnConfigurarPagos.setText("Configurar Pagos");
        btnConfigurarPagos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfigurarPagosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelMetodLayout = new javax.swing.GroupLayout(panelMetod);
        panelMetod.setLayout(panelMetodLayout);
        panelMetodLayout.setHorizontalGroup(
                panelMetodLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMetodLayout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addGroup(panelMetodLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelMetodLayout.createSequentialGroup()
                                                .addGroup(panelMetodLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelMetodLayout.createSequentialGroup()
                                                                .addComponent(btnValidar)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnConfigurarPagos)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtNotaCredito,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 130,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                305, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(0, 12, Short.MAX_VALUE))
                                        .addGroup(panelMetodLayout.createSequentialGroup()
                                                .addGroup(panelMetodLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(jLabel11,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblNotaCredito,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jScrollPane2,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 305,
                                                                Short.MAX_VALUE)
                                                        .addComponent(cbxTipoPago, 0,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(cbxEstadoPago, 0,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(txtNumeroNotaCredito))
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE)))));
        panelMetodLayout.setVerticalGroup(
                panelMetodLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMetodLayout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jLabel9)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxTipoPago, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxEstadoPago, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblNotaCredito)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtNumeroNotaCredito, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addGroup(
                                        panelMetodLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnValidar)
                                                .addComponent(txtNotaCredito, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnConfigurarPagos))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(panelMetod, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelProd, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(panelCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(20, 20, 20)));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(panelCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(panelProd, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(panelMetod, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(20, 20, 20)));

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 370,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(
                                        panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(panelFinal, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(panelProductos, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(10, 10, 10))
                        .addComponent(panelTitulo, javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));
        panelMainLayout.setVerticalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(panelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                .addComponent(panelProductos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(panelFinal, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0,
                                                Short.MAX_VALUE))
                                .addGap(20, 20, 20)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    private void btnSeleccionarClienteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSeleccionarClienteActionPerformed
        if (selectClient != null) {
            panelInfoCliente.setVisible(true);
            txtCliente.setEnabled(false);
            btnSeleccionarCliente.setEnabled(false);
            revalidate();
            repaint();
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un cliente");
        }
    }// GEN-LAST:event_btnSeleccionarClienteActionPerformed

    private void txtIngresarCodigoKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtIngresarCodigoKeyReleased

        String codigo = txtIngresarCodigo.getText().trim();
        if (codigo.isEmpty()) {
            return;
        }

        // ═══════════════════════════════════════════════════════════════════════
        // CORRECCIÓN: Cambiar longitud mínima de 8 a 12 dígitos
        // Esto evita que la auto-búsqueda se dispare antes de que el usuario
        // termine de escribir el EAN completo (ej: 7700521567732 = 13 dígitos)
        // El usuario ya no será interrumpido mientras escribe
        // ═══════════════════════════════════════════════════════════════════════
        if (codigo.length() >= 12 && codigo.matches("\\d+")) {
            DataSearch producto = buscarProductoPorEanOBarcode(codigo);
            if (producto != null) {
                // Producto encontrado con stock
                selectedProduct = producto;
                seleccionarProducto(producto, true);
                txtIngresarCodigo.setText(""); // Limpiar campo
                // Opcional: Reproducir sonido de éxito
                // java.awt.Toolkit.getDefaultToolkit().beep();
            } else {
                txtIngresarCodigo.selectAll(); // Seleccionar para facilitar nueva búsqueda
                // Opcional: Verificar si existe sin stock para mensaje específico
                if (productoExisteSinStock(codigo)) {
                }
            }
        }
        // updateSearchResultsProd();
    }// GEN-LAST:event_txtIngresarCodigoKeyReleased

    private void txtClienteKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtClienteKeyReleased
        updateSearchResultsClient();
    }// GEN-LAST:event_txtClienteKeyReleased

    private void btngenerarVentaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btngenerarVentaActionPerformed
        // ================================================================
        // PREVENIR DOBLE CLIC
        // ================================================================
        if (isProcessingVenta) {
            Toast.show(this, Toast.Type.WARNING, "Ya se está procesando una venta, por favor espere...");
            return;
        }

        // ================================================================
        // 1. VALIDACIONES BÁSICAS (ANTES DE INICIAR LOADING)
        // ================================================================
        if (selectClient == null) {
            Toast.show(this, Toast.Type.ERROR, "Seleccione un cliente primero");
            return;
        }

        if (tablaProductos.getRowCount() == 0) {
            Toast.show(this, Toast.Type.ERROR, "Agregue productos a la venta");
            return;
        }

        // ================================================================
        // 2. VALIDAR ESTADO DE PAGO SELECCIONADO
        // ================================================================
        String estadoPago = (String) cbxEstadoPago.getSelectedItem();
        if (estadoPago == null || "Seleccionar".equals(estadoPago)) {
            Toast.show(this, Toast.Type.ERROR, "Seleccione un estado de pago");
            return;
        }

        // ================================================================
        // 3. INICIAR ANIMACIÓN DE LOADING
        // ================================================================
        iniciarLoadingButton();

        // ================================================================
        // 4. PROCESAR VENTA EN SEGUNDO PLANO CON SWINGWORKER
        // ================================================================
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String mensajeError = null;
            private String mensajeExito = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Procesar según tipo de venta
                    if (modoEdicion) {
                        // Modo edición (si lo implementas después)
                        // actualizarVenta();
                        mensajeExito = "Venta actualizada correctamente";
                    } else {
                        // Crear nueva venta
                        crearNuevaVenta();
                        mensajeExito = "Venta generada exitosamente";
                    }

                    return true;

                } catch (SQLException e) {
                    System.err.println("ERROR Error SQL generando venta: " + e.getMessage());
                    e.printStackTrace();
                    mensajeError = "Error de base de datos: " + e.getMessage();
                    return false;
                } catch (Exception e) {
                    System.err.println("ERROR Error inesperado: " + e.getMessage());
                    e.printStackTrace();
                    mensajeError = "Error inesperado: " + e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    // Detener animación de loading
                    detenerLoadingButton();

                    // Validar si el componente aún es visible para evitar errores de UI
                    if (!generarVentaFor1.this.isDisplayable()) {
                        System.out.println("WARN: Componente no displayable en done(), omitiendo Toast.");
                        return;
                    }

                    // Obtener resultado
                    Boolean exitoso = get();

                    if (exitoso != null && exitoso) {
                        // Mostrar mensaje de éxito
                        if (mensajeExito != null) {
                            Toast.show(generarVentaFor1.this, Toast.Type.SUCCESS, mensajeExito);
                        }
                    } else {
                        // Mostrar mensaje de error
                        if (mensajeError != null) {
                            Toast.show(generarVentaFor1.this, Toast.Type.ERROR, mensajeError);
                        }
                    }

                } catch (Exception e) {
                    detenerLoadingButton();
                    System.err.println("ERROR Error en done(): " + e.getMessage());
                    e.printStackTrace();

                    if (generarVentaFor1.this.isDisplayable()) {
                        Toast.show(generarVentaFor1.this, Toast.Type.ERROR, "Error procesando resultado");
                    }
                }
            }
        };

        // Ejecutar el worker
        worker.execute();
    }// GEN-LAST:event_btngenerarVentaActionPerformed

    private void btnAgregarProdActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAgregarProdActionPerformed

        // ========== VALIDACIÓN DE PRODUCTO SELECCIONADO ==========
        // Validar campos
        if (!validarCamposAntesDeAgregar()) {
            return;
        }

        // MOSTRAR LOADING OVERLAY
        LoadingOverlayHelper.showLoading(panelInfoProd, "Procesando producto...");

        // ========== EJECUTAR EN SEGUNDO PLANO ==========
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            private String successMessage = null;
            private boolean abrirTallasFormFlag = false;
            private int idProductoTallas = 0;
            private int idColorTallas = 0;
            private String tipoVentaTallas = "";

            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    validacionFinalAntesDeLlenarTabla();
                    if (selectedProduct == null) {
                        String textoBusqueda = txtIngresarCodigo.getText().trim();
                        if (!textoBusqueda.isEmpty()) {
                            DataSearch producto = buscarProductoPorEanOBarcode(textoBusqueda);
                            if (producto != null) {
                                selectedProduct = producto;
                                if (tieneVariantes(Integer.parseInt(selectedProduct.getId_prod()))) {
                                    cargarVariantesProducto(Integer.parseInt(selectedProduct.getId_prod()));
                                }
                            } else {
                                errorMessage = "Producto no encontrado. Seleccione un producto primero.";
                                return false;
                            }
                        } else {
                            errorMessage = "Seleccione un producto primero.";
                            return false;
                        }
                    }

                    // Validar código de barras
                    if (selectedProduct.getEAN() == null || selectedProduct.getEAN().isEmpty()) {
                        errorMessage = "El producto no tiene código de barras válido.";
                        return false;
                    }

                    // ========== OBTENER TIPO DE VENTA (por defecto "par") ==========
                    String tipoTemp = (String) cbxTipo.getSelectedItem();
                    final String tipo = (tipoTemp != null && !tipoTemp.trim().isEmpty())
                            ? tipoTemp
                            : "par";
                    if (tipoTemp == null || tipoTemp.trim().isEmpty()) {
                    }
                    // ========== CORRECCIÓN 2: Obtener cantidad desde JSpinner ==========
                    String cantidadText = txtCantidad.getValue().toString();
                    if (cantidadText.isEmpty()) {
                        errorMessage = "Ingrese una cantidad";
                        return false;
                    }

                    // ========== VALIDACIÓN DE VARIANTES ==========
                    boolean tieneVariantes = tieneVariantes(Integer.parseInt(selectedProduct.getId_prod()));
                    String tallaSeleccionada = comboTalla.getSelectedIndex() > 0
                            ? (String) comboTalla.getSelectedItem()
                            : null;
                    String colorSeleccionado = comboColor.getSelectedIndex() > 0
                            ? (String) comboColor.getSelectedItem()
                            : null;

                    if (tieneVariantes) {
                        // Validar que se haya seleccionado un color
                        if (colorSeleccionado == null || "Color".equals(colorSeleccionado)) {
                            errorMessage = "Seleccione un color";
                            return false;
                        }

                        // Si ya hay una talla seleccionada, procesar directamente sin abrir TallasForm
                        if (tallaSeleccionada != null && !"Talla".equals(tallaSeleccionada)) {
                            // Continuar con el procesamiento normal del producto individual
                        } else {
                            // ==========================================
                            // CORRECCIÓN: Validar existencia de tallas ANTES de abrir TallasForm
                            // ==========================================
                            int idProducto = Integer.parseInt(selectedProduct.getId_prod());
                            int idColor = obtenerIdColor(colorSeleccionado);

                            if (idColor == -1) {
                                errorMessage = "Error: No se pudo encontrar el color seleccionado";
                                return false;
                            }

                            // ==========================================
                            // VALIDACIÓN CRÍTICA: Verificar si hay tallas disponibles
                            // ==========================================
                            if (!hayTallasDisponibles(idProducto, idColor, tipo)) {
                                errorMessage = "INFO:No hay tallas disponibles para este producto y color.";
                                return false;
                            }

                            // Si llegamos aquí, hay tallas disponibles - proceder a abrir TallasForm
                            abrirTallasFormFlag = true;
                            idProductoTallas = idProducto;
                            idColorTallas = idColor;
                            tipoVentaTallas = tipo;
                            return true; // Salir del método, TallasForm se abrirá en done()
                        }
                    }

                    // ========== PROCESAMIENTO DEL PRODUCTO ==========
                    int cantidad = Integer.parseInt(cantidadText);
                    if (cantidad <= 0) {
                        errorMessage = "La cantidad debe ser mayor que cero";
                        return false;
                    }

                    DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

                    int filaExistente = -1;
                    int cantidadExistente = 0;
                    Integer idDetalleExistente = null;

                    // ========== OBTENER CÓDIGO EAN ==========
                    String eanCode = null;
                    String codigoBarras = null;
                    if (selectedProduct.getIdVariante() > 0) {
                        eanCode = obtenerEanVarianteConValidacion(selectedProduct.getIdVariante());
                        codigoBarras = obtenerCodigoBarrasVariante(selectedProduct.getIdVariante());
                    } else if (tieneVariantes && tallaSeleccionada != null && colorSeleccionado != null) {
                        int idVariante = obtenerIdVariante(
                                Integer.parseInt(selectedProduct.getId_prod()),
                                tallaSeleccionada,
                                colorSeleccionado);
                        if (idVariante > 0) {
                            eanCode = obtenerEanVarianteConValidacion(idVariante);
                            codigoBarras = obtenerCodigoBarrasVariante(idVariante);
                        }
                    }

                    String identificadorProducto = (eanCode != null && !eanCode.isEmpty()) ? eanCode
                            : (codigoBarras != null && !codigoBarras.isEmpty()) ? codigoBarras
                                    : selectedProduct.getEAN();

                    // Resolver id_variante
                    Integer idVarianteSeleccionada = null;
                    if (selectedProduct.getIdVariante() > 0) {
                        idVarianteSeleccionada = selectedProduct.getIdVariante();
                    } else if (tieneVariantes && tallaSeleccionada != null && colorSeleccionado != null) {
                        int tmp = obtenerIdVariante(
                                Integer.parseInt(selectedProduct.getId_prod()),
                                tallaSeleccionada,
                                colorSeleccionado);
                        if (tmp > 0) {
                            idVarianteSeleccionada = tmp;
                        }
                    }
                    // VALIDACIÓN CRÍTICA: Verificar que id_variante se esté capturando

                    if (tieneVariantes && idVarianteSeleccionada == null) {
                        System.err.println("WARN ALERTA: Producto con variantes pero id_variante es NULL");
                        // Intentar obtener id_variante por talla y color
                        if (tallaSeleccionada != null && colorSeleccionado != null) {
                            idVarianteSeleccionada = obtenerIdVariante(
                                    Integer.parseInt(selectedProduct.getId_prod()),
                                    tallaSeleccionada,
                                    colorSeleccionado);
                        }
                    }
                    // ========== BUSCAR PRODUCTO EXISTENTE EN TABLA ==========
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String codigoEnTabla = (String) model.getValueAt(i, COL_CODIGO);
                        String tipoEnTabla = (String) model.getValueAt(i, COL_TIPO);

                        // Obtener ID variante de la tabla
                        Integer idVarianteEnTabla = null;
                        Object valVariante = model.getValueAt(i, COL_ID_VARIANTE);
                        if (valVariante != null) {
                            if (valVariante instanceof Integer) {
                                idVarianteEnTabla = (Integer) valVariante;
                            } else {
                                try {
                                    idVarianteEnTabla = Integer.parseInt(valVariante.toString());
                                } catch (Exception e) {
                                    // Ignorar error de parseo
                                }
                            }
                        }

                        // LÓGICA DE COMPARACIÓN MEJORADA
                        boolean esMismoProducto = false;

                        if (idVarianteSeleccionada != null) {
                            // Si el producto a agregar es una variante, comparar IDs de variante
                            if (idVarianteEnTabla != null && idVarianteSeleccionada.equals(idVarianteEnTabla)) {
                                esMismoProducto = true;
                            }
                        } else {
                            // Si no es variante, comparar código y asegurar que en tabla tampoco sea
                            // variante
                            if (idVarianteEnTabla == null && codigoEnTabla.equals(identificadorProducto)) {
                                esMismoProducto = true;
                            }
                        }

                        // Verificar también el tipo de venta (par/caja)
                        if (esMismoProducto && tipoEnTabla.equals(tipo)) {
                            filaExistente = i;
                            cantidadExistente = (Integer) model.getValueAt(i, COL_CANTIDAD);

                            if (model.getColumnCount() > COL_ID_DETALLE
                                    && model.getValueAt(i, COL_ID_DETALLE) != null) {
                                try {
                                    idDetalleExistente = Integer
                                            .parseInt(model.getValueAt(i, COL_ID_DETALLE).toString());
                                } catch (NumberFormatException e) {
                                    idDetalleExistente = null;
                                }
                            }
                            break;
                        }
                    }

                    // ========== VERIFICAR STOCK ==========
                    int stockDisponible;

                    if (selectedProduct.getIdVariante() > 0) {
                        stockDisponible = tipo.equals("caja")
                                ? selectedProduct.getStockPorCajas()
                                : selectedProduct.getStockPorPares();
                    } else if (tieneVariantes && tallaSeleccionada != null && colorSeleccionado != null) {
                        int[] stockVariante = obtenerStockVariante(
                                Integer.parseInt(selectedProduct.getId_prod()),
                                tallaSeleccionada,
                                colorSeleccionado);
                        stockDisponible = tipo.equals("caja") ? stockVariante[0] : stockVariante[1];
                    } else {
                        stockDisponible = tipo.equals("caja")
                                ? selectedProduct.getStockPorCajas()
                                : selectedProduct.getStockPorPares();
                    }

                    // Validación de stock en modo edición
                    if (modoEdicion && idDetalleExistente != null && productosEliminados.contains(idDetalleExistente)) {
                        productosEliminados.remove(idDetalleExistente);
                        filaExistente = -1;
                        cantidadExistente = 0;
                    }

                    int cantidadTotal = cantidadExistente + cantidad;

                    if (cantidadTotal > stockDisponible) {
                        errorMessage = tipo.equals("caja")
                                ? String.format("Stock insuficiente. Disponible: %d cajas (Agregadas: %d)",
                                        stockDisponible, cantidadExistente)
                                : String.format("Stock insuficiente. Disponible: %d pares (Agregadas: %d)",
                                        stockDisponible, cantidadExistente);
                        return false;
                    }

                    // ========== OBTENER PRECIO ==========
                    java.math.BigDecimal precioUnitario;

                    if (selectedProduct.getIdVariante() > 0) {
                        precioUnitario = selectedProduct.getPrecioVenta();
                    } else if (tallaSeleccionada != null && colorSeleccionado != null) {
                        precioUnitario = obtenerPrecioVariante(
                                Integer.parseInt(selectedProduct.getId_prod()),
                                tallaSeleccionada,
                                colorSeleccionado);
                        if (precioUnitario == null) {
                            precioUnitario = selectedProduct.getPrecioVenta();
                        }
                    } else {
                        precioUnitario = selectedProduct.getPrecioVenta();
                    }

                    java.math.BigDecimal subtotal;

                    // ========== ACTUALIZAR O AGREGAR FILA (en EDT) ==========
                    final int filaExistenteFinal = filaExistente;
                    final int cantidadExistenteFinal = cantidadExistente;
                    final Integer idDetalleExistenteFinal = idDetalleExistente;
                    final String identificadorProductoFinal = identificadorProducto;
                    final Integer idVarianteSeleccionadaFinal = idVarianteSeleccionada;
                    final java.math.BigDecimal precioUnitarioFinal = precioUnitario;
                    final int cantidadFinal = cantidad;

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        DefaultTableModel modelEDT = (DefaultTableModel) tablaProductos.getModel();
                        java.math.BigDecimal subtotalCalc;

                        if (filaExistenteFinal >= 0) {
                            // Actualizar fila existente
                            int nuevaCantidad = cantidadExistenteFinal + cantidadFinal;
                            java.math.BigDecimal precioActual;
                            Object precioValue = modelEDT.getValueAt(filaExistenteFinal, COL_PRECIO);
                            if (precioValue instanceof java.math.BigDecimal) {
                                precioActual = (java.math.BigDecimal) precioValue;
                            } else {
                                precioActual = new java.math.BigDecimal(precioValue.toString());
                            }

                            if (tipo.equals("caja")) {
                                subtotalCalc = precioActual.multiply(java.math.BigDecimal.valueOf(nuevaCantidad * 24));
                            } else {
                                subtotalCalc = precioActual.multiply(java.math.BigDecimal.valueOf(nuevaCantidad));
                            }

                            modelEDT.setValueAt(nuevaCantidad, filaExistenteFinal, COL_CANTIDAD);
                            modelEDT.setValueAt(subtotalCalc, filaExistenteFinal, COL_SUBTOTAL);
                            successMessage = "Cantidad actualizada";
                        } else {
                            // Agregar nueva fila
                            if (tipo.equals("caja")) {
                                subtotalCalc = precioUnitarioFinal
                                        .multiply(java.math.BigDecimal.valueOf(cantidadFinal * 24));
                            } else {
                                subtotalCalc = precioUnitarioFinal
                                        .multiply(java.math.BigDecimal.valueOf(cantidadFinal));
                            }

                            // ═══════════════════════════════════════════════════════════════════════
                            // FORMATEO VISUAL MEJORADO PARA FACTURACIÓN DIARIA
                            // Formato: NOMBRE - TALLA+SISTEMA GÉNERO COLOR
                            // Ejemplo: NIKE AIR 1 - 43EU H NEGRA
                            // ═══════════════════════════════════════════════════════════════════════
                            String baseNombre = selectedProduct.getNombre();
                            StringBuilder sb = new StringBuilder();

                            // 1. Nombre del producto en mayúsculas
                            if (baseNombre != null && !baseNombre.isEmpty()) {
                                sb.append(baseNombre.toUpperCase().trim());
                            }

                            // 2. Separador principal
                            sb.append(" - ");

                            // 3. Talla (si está seleccionada)
                            boolean tieneInfoVariante = false;
                            if (tallaSeleccionada != null && !"Talla".equals(tallaSeleccionada)) {
                                sb.append(tallaSeleccionada.trim());
                                tieneInfoVariante = true;
                            }

                            // 4. Género abreviado del producto (si existe)
                            String generoProducto = selectedProduct.getGenero();
                            if (generoProducto != null && !generoProducto.isEmpty()) {
                                String g = generoProducto.toUpperCase().trim();
                                String generoAbbr = "";
                                if (g.startsWith("HOMBRE") || g.equals("H"))
                                    generoAbbr = "H";
                                else if (g.startsWith("MUJER") || g.equals("M"))
                                    generoAbbr = "M";
                                else if (g.startsWith("NIÑO") || g.startsWith("NINO") || g.equals("N"))
                                    generoAbbr = "N";
                                else if (g.startsWith("UNISEX") || g.equals("U"))
                                    generoAbbr = "U";
                                else if (!g.isEmpty())
                                    generoAbbr = g.substring(0, 1);

                                if (!generoAbbr.isEmpty()) {
                                    if (tieneInfoVariante)
                                        sb.append(" ");
                                    sb.append(generoAbbr);
                                    tieneInfoVariante = true;
                                }
                            }

                            // 5. Color en mayúsculas (si está seleccionado)
                            if (colorSeleccionado != null && !"Color".equals(colorSeleccionado)) {
                                if (tieneInfoVariante)
                                    sb.append(" ");
                                sb.append(colorSeleccionado.toUpperCase().trim());
                            }

                            String nombreCompleto = sb.toString().trim();
                            if (modoEdicion) {
                                modelEDT.addRow(new Object[] {
                                        identificadorProductoFinal, // 0 - Código
                                        nombreCompleto, // 1 - Producto
                                        tipo, // 2 - Tipo
                                        cantidadFinal, // 3 - Cantidad
                                        precioUnitarioFinal, // 4 - Precio
                                        java.math.BigDecimal.ZERO, // 5 - Descuento
                                        subtotalCalc, // 6 - Subtotal
                                        null, // 7 - Acción
                                        idDetalleExistenteFinal, // 8 - id_detalle
                                        idVarianteSeleccionadaFinal, // 9 - id_variante
                                        Integer.parseInt(selectedProduct.getId_prod()) // 10 - id_producto
                                });
                            } else {
                                // Al agregar la fila NUEVA, usa idVarianteSeleccionada:
                                modelEDT.addRow(new Object[] {
                                        identificadorProductoFinal, // COL_CODIGO
                                        nombreCompleto, // COL_PRODUCTO (FORMATEADO)
                                        tipo, // COL_TIPO
                                        cantidadFinal, // COL_CANTIDAD
                                        precioUnitarioFinal, // COL_PRECIO
                                        BigDecimal.ZERO, // COLDESCUENTO
                                        subtotalCalc, // COL_SUBTOTAL
                                        null, // COL_ACCION
                                        null, // COL_IDDETALLE
                                        idVarianteSeleccionadaFinal, // COL_IDVARIANTE ← CRÍTICO
                                        Integer.parseInt(selectedProduct.getId_prod()) // COL_IDPRODUCTO
                                });
                            }
                            successMessage = "Producto agregado exitosamente";
                        }

                        actualizarContadorProductos();
                        // ========== ACTUALIZAR TOTALES Y LIMPIAR ==========
                        actualizarTotales();
                        limpiarCampos();

                        selectedProduct = null;
                        limpiarSeleccionesVariantes();
                        ICON.setIcon(null);
                        ICON.setText("ICON");

                        comboColor.setEnabled(true);
                        comboTalla.setEnabled(true);
                        cbxTipo.setSelectedIndex(0); // Resetear a "par" (predeterminado)
                        txtIngresarCodigo.setText("");
                        txtIngresarCodigo.requestFocus();

                        actualizarEstiloBtnAgregar();
                    });

                    return true;

                } catch (NumberFormatException e) {
                    errorMessage = "La cantidad debe ser un número válido: " + e.getMessage();
                    return false;
                } catch (Exception e) {
                    errorMessage = "Error inesperado: " + e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                // OCULTAR LOADING SIEMPRE
                LoadingOverlayHelper.hideLoading(panelInfoProd);

                try {
                    Boolean result = get();

                    if (abrirTallasFormFlag) {
                        // Abrir TallasForm en el EDT
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            abrirTallasForm(idProductoTallas, idColorTallas, tipoVentaTallas);
                        });
                    } else if (errorMessage != null) {
                        // Mostrar mensaje de error
                        if (errorMessage.startsWith("INFO:")) {
                            String msg = errorMessage.substring(5);
                            Toast.show(generarVentaFor1.this, Toast.Type.INFO, msg);
                        } else {
                            Toast.show(generarVentaFor1.this, Toast.Type.ERROR, errorMessage);
                        }
                    } else if (successMessage != null) {
                        // Mostrar mensaje de éxito
                        Toast.show(generarVentaFor1.this, Toast.Type.SUCCESS, successMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show(generarVentaFor1.this, Toast.Type.ERROR, "Error al procesar: " + e.getMessage());
                }
            }
        };

        // ========== EJECUTAR WORKER ==========
        worker.execute();
    }// GEN-LAST:event_btnAgregarProdActionPerformed

    private void abrirTallasForm(int idProducto, int idColor, String tipoVenta) {
        // Crear el diálogo
        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                "Seleccionar Tallas", true);

        // Crear el callback para recibir las tallas seleccionadas
        TallasForm.TallasFormCallback callback = new TallasForm.TallasFormCallback() {
            @Override
            public void onTallasSeleccionadas(List<TallasForm.TallaVariante> tallasSeleccionadas, String tipo) {
                procesarTallasSeleccionadas(tallasSeleccionadas, tipo);
                dialog.dispose(); // Cerrar el diálogo
            }
        };

        // Crear el formulario TallasForm
        TallasForm tallasForm = new TallasForm(idProducto, idColor, tipoVenta, callback);

        // Configurar el diálogo
        dialog.setLayout(new BorderLayout());
        dialog.add(tallasForm, BorderLayout.CENTER);
        dialog.setSize(new Dimension(600, 400));
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Mostrar el diálogo
        dialog.setVisible(true);
    }

    private void procesarTallasSeleccionadas(List<TallasForm.TallaVariante> tallasSeleccionadas, String tipoVenta) {
        try {
            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

            int tallasAgregadas = 0;
            for (TallasForm.TallaVariante talla : tallasSeleccionadas) {
                if (talla.isSeleccionada() && talla.getCantidadSeleccionada() > 0) {
                    tallasAgregadas++;
                    // Obtener el código EAN directamente de TallaVariante
                    String identificadorProducto = talla.getEan();

                    // Validar que el EAN no esté vacío
                    if (identificadorProducto == null || identificadorProducto.trim().isEmpty()) {
                        identificadorProducto = talla.getSku();
                        System.err.println("WARN ADVERTENCIA: EAN vacío para talla " + talla.getTalla().getNombre()
                                + ", usando SKU como fallback: " + identificadorProducto);
                        Toast.show(this, Toast.Type.WARNING,
                                "Advertencia: EAN vacío para " + talla.getTalla().getNombre() + ", usando SKU");
                    } else {
                    }

                    // Buscar si ya existe en la tabla
                    int filaExistente = -1;
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String codigoEnTabla = (String) model.getValueAt(i, 0);
                        String tipoEnTabla = (String) model.getValueAt(i, 2);

                        if (codigoEnTabla.equals(identificadorProducto) && tipoEnTabla.equals(tipoVenta)) {
                            filaExistente = i;
                            break;
                        }
                    }

                    if (filaExistente != -1) {
                        // Actualizar cantidad existente
                        int cantidadExistente = (Integer) model.getValueAt(filaExistente, 3);
                        int nuevaCantidad = cantidadExistente + talla.getCantidadSeleccionada();

                        model.setValueAt(nuevaCantidad, filaExistente, 3);

                        // Recalcular subtotal
                        BigDecimal precio = talla.getPrecioVenta();
                        BigDecimal subtotal = precio.multiply(new BigDecimal(nuevaCantidad));
                        model.setValueAt(subtotal, filaExistente, 6);
                        // Asegurar que el descuento sea 0
                        model.setValueAt(BigDecimal.ZERO, filaExistente, 5);

                    } else {
                        // Agregar nueva fila
                        // ═══════════════════════════════════════════════════════════════════════
                        // FORMATEO VISUAL MEJORADO PARA FACTURACIÓN DIARIA
                        // Formato: NOMBRE - TALLA GÉNERO COLOR
                        // Ejemplo: NIKE AIR 1 - 43EU H NEGRA
                        // ═══════════════════════════════════════════════════════════════════════
                        String baseNombre = selectedProduct.getNombre();
                        String nombreColor = selectedProduct.getColor();
                        String tallaNombre = talla.getTalla().getNombre();

                        StringBuilder sb = new StringBuilder();

                        // 1. Nombre del producto en mayúsculas
                        if (baseNombre != null && !baseNombre.isEmpty()) {
                            sb.append(baseNombre.toUpperCase().trim());
                        }

                        // 2. Separador principal
                        sb.append(" - ");

                        // 3. Talla
                        boolean tieneInfoVariante = false;
                        if (tallaNombre != null && !tallaNombre.isEmpty()) {
                            sb.append(tallaNombre.trim());
                            tieneInfoVariante = true;
                        }

                        // 4. Género abreviado del producto (si existe)
                        String generoProducto = selectedProduct.getGenero();
                        if (generoProducto != null && !generoProducto.isEmpty()) {
                            String g = generoProducto.toUpperCase().trim();
                            String generoAbbr = "";
                            if (g.startsWith("HOMBRE") || g.equals("H"))
                                generoAbbr = "H";
                            else if (g.startsWith("MUJER") || g.equals("M"))
                                generoAbbr = "M";
                            else if (g.startsWith("NIÑO") || g.startsWith("NINO") || g.equals("N"))
                                generoAbbr = "N";
                            else if (g.startsWith("UNISEX") || g.equals("U"))
                                generoAbbr = "U";
                            else if (!g.isEmpty())
                                generoAbbr = g.substring(0, 1);

                            if (!generoAbbr.isEmpty()) {
                                if (tieneInfoVariante)
                                    sb.append(" ");
                                sb.append(generoAbbr);
                                tieneInfoVariante = true;
                            }
                        }

                        // 5. Color en mayúsculas
                        if (nombreColor != null && !nombreColor.trim().isEmpty()) {
                            if (tieneInfoVariante)
                                sb.append(" ");
                            sb.append(nombreColor.toUpperCase().trim());
                        }

                        String nombreProducto = sb.toString().trim();

                        BigDecimal precio = talla.getPrecioVenta();
                        int cantidad = talla.getCantidadSeleccionada();
                        BigDecimal subtotal = precio.multiply(new BigDecimal(cantidad));

                        model.addRow(new Object[] {
                                identificadorProducto,
                                nombreProducto,
                                tipoVenta,
                                cantidad,
                                precio,
                                BigDecimal.ZERO,
                                subtotal,
                                null,
                                null,
                                talla.getIdVariante(),
                                Integer.parseInt(selectedProduct.getId_prod())
                        });
                    }
                }
            }

            // Actualizar totales
            actualizarTotales();
            limpiarCampos();

            // Limpiar icono después de agregar productos desde tabla de tallas
            ICON.setIcon(null);
            ICON.setText("ICON");
            Toast.show(this, Toast.Type.SUCCESS, "Productos agregados correctamente (" + tallasAgregadas + " tallas)");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al procesar las tallas: " + e.getMessage());
        }
    }

    private void comboTallaItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_comboTallaItemStateChanged

        // ========================================================================
        // FILTRO 1: Verificar que es un evento de SELECCIÓN (no DESELECCIÓN)
        // ========================================================================
        if (evt.getStateChange() != java.awt.event.ItemEvent.SELECTED) {
            return;
        }
        // ========================================================================
        // FILTRO 2: Verificar que no estamos en proceso de carga programática
        // ========================================================================
        if (isLoadingTallas) {
            return;
        }
        // ========================================================================
        // FILTRO 3: Verificar que hay un producto seleccionado
        // ========================================================================
        if (selectedProduct == null) {
            return;
        }
        // ========================================================================
        // VALIDACIÓN 1: Obtener el índice y verificar que no sea el placeholder
        // ========================================================================
        int selectedIndex = comboTalla.getSelectedIndex();
        if (selectedIndex <= 0) {
            return;
        }
        // ========================================================================
        // VALIDACIÓN 2: Obtener el valor y verificar que no sea null ni vacío
        // ========================================================================
        Object selectedItem = comboTalla.getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        String tallaSeleccionada = selectedItem.toString().trim();
        if (tallaSeleccionada.isEmpty() || tallaSeleccionada.equals("Seleccionar talla")) {
            return;
        }

        // ========================================================================
        // PROCESO PRINCIPAL: Cargar colores y actualizar datos
        // ========================================================================
        try {

            // Cargar colores disponibles para esta talla
            int idProducto = Integer.parseInt(selectedProduct.getId_prod());
            // COMENTADO POR SOLICITUD DE USUARIO: No filtrar colores, mostrar todos
            // cargarColoresPorTalla(idProducto, tallaSeleccionada);

            // Si ya hay un color seleccionado, actualizar datos completos
            if (comboColor.getSelectedIndex() > 0) {
                if (isSelectingProduct) {
                } else {
                    actualizarDatosProductoCompleto();
                }
            } else {
            }
        } catch (Exception e) {
            System.err.println("ERROR [TALLA] Error en procesamiento:");
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al procesar selección de talla: " + e.getMessage());
        }
    }// GEN-LAST:event_comboTallaItemStateChanged

    private void btnValidarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnValidarActionPerformed
        String numeroNotaCredito = txtNumeroNotaCredito.getText().trim();

        // ====================================================================
        // 1. VALIDACIONES BÁSICAS
        // ====================================================================
        if (numeroNotaCredito.isEmpty()) {
            Toast.show(this, Toast.Type.ERROR, "Ingrese el número de nota de crédito");
            txtNumeroNotaCredito.requestFocus();
            return;
        }

        // Validar que haya productos en la venta
        if (tablaProductos.getRowCount() == 0) {
            Toast.show(this, Toast.Type.ERROR, "Agregue productos a la venta antes de aplicar una nota de crédito");
            return;
        }

        // Validar que haya un cliente seleccionado
        if (selectClient == null) {
            Toast.show(this, Toast.Type.ERROR, "Seleccione un cliente antes de aplicar una nota de crédito");
            return;
        }

        try {
            // ====================================================================
            // 2. BUSCAR NOTA DE CRÉDITO EN BASE DE DATOS
            // ====================================================================
            ModelNotaCredito nota = serviceNotaCredito.buscarNotaCreditoPorNumero(numeroNotaCredito);

            if (nota == null) {
                Toast.show(this, Toast.Type.ERROR,
                        "Nota de crédito no encontrada: " + numeroNotaCredito);
                txtNumeroNotaCredito.selectAll();
                return;
            }
            // ====================================================================
            // 3. VALIDAR ESTADO DE LA NOTA DE CRÉDITO
            // ====================================================================
            if (nota.getEstado() != ModelNotaCredito.EstadoNota.EMITIDA) {
                String mensaje = String.format(
                        "La nota de crédito no está disponible\n\n"
                                + "Estado actual: %s\n"
                                + "Solo se pueden usar notas en estado EMITIDA",
                        nota.getEstado().getDescripcion());

                JOptionPane.showMessageDialog(this, mensaje,
                        "Nota de Crédito No Disponible", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ====================================================================
            // 4. VALIDAR VIGENCIA
            // ====================================================================
            if (nota.getFechaVencimiento() != null
                    && nota.getFechaVencimiento().isBefore(java.time.LocalDateTime.now())) {

                String mensaje = String.format(
                        "La nota de crédito está vencida\n\n"
                                + "Fecha de vencimiento: %s\n"
                                + "Fecha actual: %s",
                        nota.getFechaVencimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                JOptionPane.showMessageDialog(this, mensaje,
                        "Nota de Crédito Vencida", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ====================================================================
            // 5. VALIDAR CLIENTE
            // ====================================================================
            if (nota.getIdCliente() != selectClient.getIdCliente()) {
                String mensaje = String.format(
                        "La nota de crédito pertenece a otro cliente\n\n"
                                + "Cliente de la nota: %s (DNI: %s)\n"
                                + "Cliente actual: %s (DNI: %s)\n\n"
                                + "No se puede aplicar esta nota de crédito",
                        nota.getClienteNombre(), nota.getClienteDni(),
                        selectClient.getNombre(), selectClient.getDni());

                JOptionPane.showMessageDialog(this, mensaje,
                        "Cliente No Coincide", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ====================================================================
            // 6. VALIDAR SALDO DISPONIBLE
            // ====================================================================
            if (nota.getSaldoDisponible().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                String mensaje = String.format(
                        "La nota de crédito no tiene saldo disponible\n\n"
                                + "Saldo total: $%.2f\n"
                                + "Saldo usado: $%.2f\n"
                                + "Saldo disponible: $%.2f",
                        nota.getTotal().doubleValue(),
                        nota.getSaldoUsado().doubleValue(),
                        nota.getSaldoDisponible().doubleValue());

                JOptionPane.showMessageDialog(this, mensaje,
                        "Sin Saldo Disponible", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // ====================================================================
            // 7. APLICAR NOTA DE CRÉDITO AL PAGO
            // ====================================================================
            aplicarNotaCredito(nota);

        } catch (SQLException e) {
            System.err.println("ERROR Error validando nota de crédito: " + e.getMessage());
            e.printStackTrace();

            Toast.show(this, Toast.Type.ERROR,
                    "Error al validar la nota de crédito: " + e.getMessage());
        }
    }// GEN-LAST:event_btnValidarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnLimpiarActionPerformed
        // ═══════════════════════════════════════════════════════════════════════════
        // LIMPIAR TODOS LOS CAMPOS DE SELECCIÓN DE PRODUCTO
        // ═══════════════════════════════════════════════════════════════════════════
        // 1. Limpiar producto seleccionado
        selectedProduct = null;

        // 2. Limpiar campo de código/EAN
        txtIngresarCodigo.setText("");

        // 3. Resetear combos de variantes
        if (comboTalla.getItemCount() > 0) {
            comboTalla.setSelectedIndex(0);
        }
        if (comboColor.getItemCount() > 0) {
            comboColor.setSelectedIndex(0);
        }
        comboTalla.setEnabled(true);
        comboColor.setEnabled(true);

        // 4. Resetear tipo de venta
        if (cbxTipo.getItemCount() > 0) {
            cbxTipo.setSelectedIndex(0);
        }

        // 5. Resetear cantidad a 1
        txtCantidad.setValue(1);

        // 6. Limpiar información del producto
        txtNombreProducto.setText("");
        txtPrecio.setText("");
        txtStock.setText("");
        txtEAN.setText("");
        txtCodigo.setText("");

        // 7. Limpiar icono del producto
        ICON.setIcon(null);
        ICON.setText("ICON");

        // 8. Ocultar panel de información del producto
        if (panelInfoProd != null) {
            panelInfoProd.setVisible(false);
        }

        // 9. Actualizar estado del botón agregar
        actualizarEstiloBtnAgregar();

        // 10. Enfocar campo de código para nueva entrada
        txtIngresarCodigo.requestFocus();

        // 11. Limpiar cliente
        selectClient = null;
        if (txtCliente != null) {
            txtCliente.setText("");
            txtCliente.setEnabled(true);
        }
        if (btnSeleccionarCliente != null) {
            btnSeleccionarCliente.setEnabled(true);
        }
        if (panelInfoCliente != null) {
            panelInfoCliente.setVisible(false);
        }

        Toast.show(this, Toast.Type.SUCCESS, "Campos limpiados correctamente");
    }// GEN-LAST:event_btnLimpiarActionPerformed

    private void comboColorItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_comboColorItemStateChanged

        // ========================================================================
        // FILTRO 1: Solo procesar eventos de SELECCIÓN
        // ========================================================================
        if (evt.getStateChange() != java.awt.event.ItemEvent.SELECTED) {
            return;
        }

        // ========================================================================
        // FILTRO 2: Verificar flag de carga programática
        // ========================================================================
        if (isLoadingColores) {
            return;
        }

        // ========================================================================
        // FILTRO 3: Verificar producto seleccionado
        // ========================================================================
        if (selectedProduct == null) {
            return;
        }

        // ========================================================================
        // VALIDACIÓN 1: Verificar índice válido
        // ========================================================================
        int selectedIndex = comboColor.getSelectedIndex();
        if (selectedIndex <= 0) {
            return;
        }

        // ========================================================================
        // VALIDACIÓN 2: Verificar valor no null ni vacío
        // ========================================================================
        Object selectedItem = comboColor.getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        String colorSeleccionado = selectedItem.toString().trim();
        if (colorSeleccionado.isEmpty() || colorSeleccionado.equals("Color")) {
            return;
        }

        // ========================================================================
        // VALIDACIÓN 3: Verificar que también hay talla seleccionada
        // ========================================================================
        if (comboTalla.getSelectedIndex() <= 0) {
            return;
        }

        // ========================================================================
        // PROCESO PRINCIPAL: Actualizar datos del producto
        // ========================================================================
        try {

            if (isSelectingProduct) {
            } else {
                actualizarDatosProductoCompleto();
            }

        } catch (Exception e) {
            System.err.println("ERROR [COLOR] Error en procesamiento:");
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR,
                    "Error al procesar selección de color: " + e.getMessage());
        }

    }// GEN-LAST:event_comboColorItemStateChanged

    private void comboColorActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboColorActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_comboColorActionPerformed

    private void cbxTipoItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_cbxTipoItemStateChanged

        // FILTRO 1: Solo procesar eventos de SELECCIÓN (no deselección)
        if (evt.getStateChange() != java.awt.event.ItemEvent.SELECTED) {
            return;
        }

        try {
            // OBTENER TIPO SELECCIONADO
            Object item = cbxTipo.getSelectedItem();
            if (item == null) {
                actualizarEstiloBtnAgregar();
                return;
            }

            String tipoSeleccionado = item.toString().trim();
            // VALIDACIÓN: Verificar que sea válido (no "Seleccionar")
            if (tipoSeleccionado.isEmpty() || "Seleccionar".equalsIgnoreCase(tipoSeleccionado)) {
                actualizarEstiloBtnAgregar();
                return;
            }

            // VALIDACIÓN: Verificar que hay producto seleccionado
            if (selectedProduct == null) {
                actualizarEstiloBtnAgregar();
                return;
            }
            // PASO CRÍTICO: SI AMBOS ESTÁN SELECCIONADOS (talla + color), RECARGAR DATOS
            int tallaIndex = comboTalla.getSelectedIndex();
            int colorIndex = comboColor.getSelectedIndex();

            // CASO 1: Ambos están seleccionados → Recargar datos CON EL NUEVO TIPO
            if (tallaIndex > 0 && colorIndex > 0) {
                String talla = (String) comboTalla.getSelectedItem();
                String color = (String) comboColor.getSelectedItem();

                // CRÍTICO: Recargar datos - ahora actualizarDatosProductoCompleto()
                // considerará el tipo de venta para mostrar stock correcto
                actualizarDatosProductoCompleto(talla, color);

            } else {
                // CASO 2: Incompleto → Solo actualizar estilos
            }

            // ACTUALIZAR ESTILOS DEL BOTÓN
            actualizarEstiloBtnAgregar();
        } catch (Exception e) {
            System.err.println("ERROR Error en cbxTipoItemStateChanged: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cambiar tipo de venta: " + e.getMessage());
            actualizarEstiloBtnAgregar();
        }
    }// GEN-LAST:event_cbxTipoItemStateChanged

    private void txtCantidadInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {// GEN-FIRST:event_txtCantidadInputMethodTextChanged
        actualizarEstiloBtnAgregar();
    }// GEN-LAST:event_txtCantidadInputMethodTextChanged

    private void cbxEstadoPagoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxEstadoPagoActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_cbxEstadoPagoActionPerformed

    private void txtIngresarCodigoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtIngresarCodigoActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtIngresarCodigoActionPerformed

    private void txtIngresarCodigoKeyTyped(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtIngresarCodigoKeyTyped
        // TODO add your handling code here:
    }// GEN-LAST:event_txtIngresarCodigoKeyTyped

    private void cbxTipoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxTipoActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_cbxTipoActionPerformed

    private void cbxTipoPagoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxTipoPagoActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_cbxTipoPagoActionPerformed

    private void btnConfigurarPagosActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnConfigurarPagosActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_btnConfigurarPagosActionPerformed

    private void btnGastosActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGastosActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_btnGastosActionPerformed

    private void btnCerrarCajaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCerrarCajaActionPerformed

    }// GEN-LAST:event_btnCerrarCajaActionPerformed

    private void btnBloquearActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnBloquearActionPerformed

    }// GEN-LAST:event_btnBloquearActionPerformed

    private void btnBuscarProductoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnBuscarProductoActionPerformed
        // Obtener bodega del usuario actual
        Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
        if (idBodega == null || idBodega <= 0) {
            Toast.show(this, Toast.Type.ERROR, "No se pudo obtener la bodega del usuario");
            return;
        }

        // Abrir diálogo de búsqueda
        BuscadorProductoDialog.mostrar(this, idBodega, producto -> {
            if (producto != null) {
                // Usar el identificador del producto (EAN/SKU)
                String codigo = producto.getIdentificador();

                // Usar la misma lógica que txtIngresarCodigo
                if (codigo != null && !codigo.isEmpty()) {
                    DataSearch productoEncontrado = buscarProductoPorEanOBarcode(codigo);
                    if (productoEncontrado != null) {
                        // Producto encontrado con stock
                        selectedProduct = productoEncontrado;
                        seleccionarProducto(productoEncontrado, true);
                        txtIngresarCodigo.setText(""); // Limpiar campo
                    } else {
                        // Fallback: si no encuentra por EAN, intentar cargar directamente
                        Toast.show(generarVentaFor1.this, Toast.Type.WARNING,
                                "No se pudo cargar el producto. Verifique el stock en su bodega.");
                    }
                }
            }
        }, configurados -> {
            if (configurados != null && !configurados.isEmpty()) {
                agregarProductosConfigurados(configurados);
            }
        });
    }// GEN-LAST:event_btnBuscarProductoActionPerformed

    private void mostrarModalTraspasos() {
        if (!cajaAbierta) {
            Toast.show(this, Toast.Type.WARNING, "Debe abrir caja primero");
            return;
        }

        Integer idBodega = null;
        if (UserSession.getInstance().getCurrentUser() != null) {
            idBodega = UserSession.getInstance().getIdBodegaUsuario();
        }

        if (idBodega == null) {
            Toast.show(this, Toast.Type.ERROR, "No se pudo determinar la bodega del usuario");
            return;
        }

        // Definir lógica de procesamiento para reutilizar
        java.util.function.Consumer<BuscadorProductoDialog.ProductoSeleccionado> procesarProducto = producto -> {
            if (producto != null) {
                String codigo = producto.getIdentificador();
                if (codigo != null && !codigo.isEmpty()) {
                    DataSearch productoEncontrado = buscarProductoPorEanOBarcode(codigo);
                    if (productoEncontrado != null) {
                        selectedProduct = productoEncontrado;
                        seleccionarProducto(productoEncontrado, true);
                        txtIngresarCodigo.setText("");
                    } else {
                        Toast.show(generarVentaFor1.this, Toast.Type.WARNING,
                                "No se pudo cargar el producto " + producto.getNombre()
                                        + ". Verifique el stock en su bodega.");
                    }
                }
            }
        };

        BuscadorProductoDialog.mostrarTraspasos(this, idBodega,
                procesarProducto, // Callback simple
                productosConfigurados -> { // Callback múltiple
                    if (productosConfigurados != null) {
                        for (BuscadorProductoDialog.ProductoConfigurado pc : productosConfigurados) {
                            procesarProducto.accept(pc.getProducto());
                        }
                    }
                });
    }

    private void mostrarModalTraspasosSeleccion() {
        if (!cajaAbierta) {
            Toast.show(this, Toast.Type.WARNING, "Debe abrir caja primero");
            return;
        }

        Integer idBodega = null;
        if (UserSession.getInstance().getCurrentUser() != null) {
            idBodega = UserSession.getInstance().getIdBodegaUsuario();
        }

        if (idBodega == null) {
            Toast.show(this, Toast.Type.ERROR, "No se pudo determinar la bodega del usuario");
            return;
        }

        java.util.function.Consumer<BuscadorProductoDialog.ProductoSeleccionado> procesarProducto = producto -> {
            if (producto != null) {
                String codigo = producto.getIdentificador();
                if (codigo != null && !codigo.isEmpty()) {
                    DataSearch productoEncontrado = buscarProductoPorEanOBarcode(codigo);
                    if (productoEncontrado != null) {
                        selectedProduct = productoEncontrado;
                        seleccionarProducto(productoEncontrado, true);
                        txtIngresarCodigo.setText("");
                    } else {
                        Toast.show(generarVentaFor1.this, Toast.Type.WARNING,
                                "No se pudo cargar el producto " + producto.getNombre()
                                        + ". Verifique el stock en su bodega.");
                    }
                }
            }
        };

        BuscadorProductoDialog.mostrarTraspasosSeleccion(this, idBodega,
                procesarProducto,
                productosConfigurados -> {
                    if (productosConfigurados != null) {
                        for (BuscadorProductoDialog.ProductoConfigurado pc : productosConfigurados) {
                            procesarProducto.accept(pc.getProducto());
                        }
                    }
                });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ICON;
    private javax.swing.JButton btnAgregarProd;
    private raven.componentes.GradientButton btnBloquear;
    private javax.swing.JButton btnBuscarProducto;
    private raven.componentes.GradientButton btnCerrarCaja;
    private javax.swing.JButton btnConfigurarPagos;
    private raven.componentes.GradientButton btnGastos;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnSeleccionarCliente;
    private javax.swing.JButton btnValidar;
    private javax.swing.JButton btngenerarVenta;
    private javax.swing.JComboBox<String> cbxEstadoPago;
    private javax.swing.JComboBox<String> cbxTipo;
    private javax.swing.JComboBox<String> cbxTipoPago;
    private javax.swing.JComboBox<String> comboColor;
    private javax.swing.JComboBox<String> comboTalla;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblNotaCredito;
    private javax.swing.JPanel panelCliente;
    private javax.swing.JPanel panelEstadoCaj;
    private javax.swing.JPanel panelFinal;
    private javax.swing.JPanel panelInfoCliente;
    private javax.swing.JPanel panelInfoProd;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelMetod;
    private javax.swing.JPanel panelProd;
    private javax.swing.JPanel panelProductos;
    private javax.swing.JPanel panelTitulo;
    private javax.swing.JTable tablaProductos;
    private javax.swing.JSpinner txtCantidad;
    private javax.swing.JTextField txtCliente;
    private javax.swing.JLabel txtCodigo;
    private javax.swing.JLabel txtDatosCliente;
    private javax.swing.JLabel txtDescuento;
    private javax.swing.JLabel txtEAN;
    private javax.swing.JLabel txtEstadoCaja;
    private javax.swing.JTextField txtIngresarCodigo;
    private javax.swing.JLabel txtMovimientoCaja;
    private javax.swing.JLabel txtNombreCliente;
    private javax.swing.JLabel txtNombreProducto;
    private javax.swing.JLabel txtNotaCredito;
    private javax.swing.JTextField txtNumeroNotaCredito;
    private javax.swing.JTextArea txtObservaciones;
    private javax.swing.JLabel txtPendiente;
    private javax.swing.JLabel txtPrecio;
    private javax.swing.JLabel txtStock;
    private javax.swing.JLabel txtSubTotal;
    private javax.swing.JLabel txtTotal;
    // End of variables declaration//GEN-END:variables
}
