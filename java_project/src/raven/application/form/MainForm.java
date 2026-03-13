package raven.application.form;

import raven.application.form.admin.CajasForm;
import raven.application.form.admin.GestionPermisosForm;
import raven.application.form.admin.MovimientosCajaForm;
import raven.application.form.admin.PromocionesForm;
import raven.application.form.admin.TiposGastoForm;
import raven.application.form.admin.UsuariosForm;
import raven.application.form.comercial.ClientesForm;
import raven.application.form.comercial.ComprasForm2;
import raven.application.form.comercial.ProveedoresForm;
import raven.application.form.productos.CategoriasForm;
import raven.application.form.productos.GestionProductosForm;
import raven.application.form.productos.InventarioForm;
import raven.application.form.productos.MarcasForm;
import raven.application.form.productos.MovimientosForm;
import raven.application.form.productos.RotulacionForm;
import raven.application.form.productos.ConsultaInventarioDetalladoForm;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashSet;
import java.util.List;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import raven.application.Application;
import raven.application.form.comercial.reporteVentas;
import raven.application.form.other.FormDashboard;
import raven.application.form.other.Notificaciones;
import raven.clases.admin.UserSession;
import raven.application.form.LoginForm;
import raven.application.form.admin.BodegasForm;
import raven.application.form.comercial.devolucionMainForm;
import raven.application.form.principal.generarVentaFor1;
import raven.application.form.productos.ColorForm;
import raven.application.form.productos.Descuento;
import raven.application.form.productos.PrestamoForm;
import raven.application.form.productos.traspasos.traspasos;
import raven.menu.Menu;
import raven.menu.MenuAction;
import raven.componentes.notificacion.Notification;
import raven.controlador.admin.ModelNotificacion;
import raven.utils.EventosTraspasosService;
import raven.utils.NotificacionesService;
import raven.utils.tono.CorporateTone;

// ═══════════════════════════════════════════════════════════════════════════
// NUEVOS IMPORTS - Validación de caja y bodega (Principio Fail Fast)
// ═══════════════════════════════════════════════════════════════════════════
import raven.clases.admin.ServiceCaja;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.application.form.admin.AperturaCajaDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import raven.application.form.admin.MonitorCajasForm;
import raven.application.form.comercial.ComprasForm;

// ═══════════════════════════════════════════════════════════════════════════
// IMPORT - Módulo Carrito / Órdenes Web
// ═══════════════════════════════════════════════════════════════════════════
import raven.controlador.comercial.ModelCarritoOrdenesWeb;

/**
 * Formulario principal de la aplicación.
 *
 * MEJORA APLICADA (Principio Fail Fast):
 * - Las validaciones de caja abierta, bodega asignada y caja registrada
 * ahora se realizan ANTES de abrir el formulario de ventas (case 2).
 *
 * @author Raven
 */
public class MainForm extends JLayeredPane {

    // ═══════════════════════════════════════════════════════════════════════════
    // NUEVOS SERVICIOS - Para validaciones de caja
    // ═══════════════════════════════════════════════════════════════════════════
    private final ServiceCaja serviceCaja = new ServiceCaja();
    private final ServiceCajaMovimiento serviceCajaMovimiento = new ServiceCajaMovimiento();

    // Instancia persistente del módulo Carrito (se crea una vez por sesión)
    private ModelCarritoOrdenesWeb carritoOrdenesWebForm;

    public MainForm() {
        init();
        this.addComponentListener(new ComponentAdapter() {
            private Timer resizeTimer;

            @Override
            public void componentResized(ComponentEvent e) {
                if (resizeTimer != null && resizeTimer.isRunning()) {
                    resizeTimer.restart();
                } else {
                    resizeTimer = new Timer(100, (ActionEvent evt) -> {
                        SwingUtilities.invokeLater(() -> {
                            if (panelBody != null) {
                                panelBody.revalidate();
                                panelBody.repaint();
                            }
                            if (menu != null) {
                                menu.revalidate();
                                menu.repaint();
                            }
                        });
                    });
                    resizeTimer.setRepeats(false);
                    resizeTimer.start();
                }
            }
        });
    }

    public Menu getMenu() {
        return menu;
    }

    private void init() {
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new MainFormLayout());
        menu = new Menu();
        panelBody = new JPanel(new BorderLayout());
        initMenuArrowIcon();
        menuButton.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Menu.button.background;"
                + "arc:999;"
                + "focusWidth:0;"
                + "borderWidth:0");
        menuButton.addActionListener((ActionEvent e) -> {
            if (menuHide) {
                setMenuFull(true);
                setMenuHide(false);
            } else {
                setMenuHide(true);
            }
        });
        initShortcut();
        initMenuEvent();
        setLayer(menuButton, JLayeredPane.POPUP_LAYER);
        add(menuButton);
        add(menu);
        add(panelBody);
    }

    private void initShortcut() {
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == java.awt.event.KeyEvent.KEY_PRESSED) {
                if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_M) {
                    if (menuHide) {
                        setMenuFull(true);
                        setMenuHide(false);
                    } else if (menu.isMenuFull()) {
                        setMenuFull(false);
                    } else {
                        setMenuHide(true);
                    }
                    return true;
                }
            }
            return false;
        });
    }

    public void iniciarNotificacionesTraspasosEnLinea() {
        EventosTraspasosService.getInstance().iniciarMonitoreo();
    }

    public void detenerNotificacionesTraspasosEnLinea() {
        EventosTraspasosService.getInstance().detenerMonitoreo();
    }

    public void limpiarRegistroNotificacionesMostradas() {
        EventosTraspasosService.getInstance().detenerMonitoreo();
    }

    @Override
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        initMenuArrowIcon();
    }

    private void initMenuArrowIcon() {
        if (menuButton == null) {
            menuButton = new JButton();
        }
        String icon = (getComponentOrientation().isLeftToRight()) ? "menu_left.svg" : "menu_right.svg";
        menuButton.setIcon(new FlatSVGIcon("raven/icon/svg/" + icon, 24, 24));
    }

    /**
     * Inicializa los eventos del menú y configura el control de acceso por roles
     */
    private void initMenuEvent() {
        menu.addMenuEvent((int index, int subIndex, MenuAction action) -> {
            if (!UserSession.getInstance().isLoggedIn() && index != 6) {
                action.cancel();
                Application.logout();
                return;
            }

            switch (index) {
                case 0: { // Notificaciones
                    try {
                        Application.showForm(new Notificaciones());
                        setMenuHide(true);
                    } catch (Exception ex) {
                        Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
                        mostrarErrorFormulario("notificaciones");
                    }
                }
                    break;

                case 1: { // Dashboard
                    try {
                        Application.showForm(new FormDashboard());
                        setMenuHide(true);
                    } catch (Exception ex) {
                        Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
                        mostrarErrorFormulario("dashboard");
                    }
                }
                    break;

                case 2: // Generar venta
                    if (UserSession.getInstance().hasPermission("generar venta")) {
                        if (!validarBodegaAsignada()) {
                            action.cancel();
                            return;
                        }
                        ModelCaja cajaValidada = validarCajaRegistrada();
                        if (cajaValidada == null) {
                            action.cancel();
                            return;
                        }
                        if (!validarOAbrirCaja(cajaValidada)) {
                            action.cancel();
                            return;
                        }
                        try {
                            if (generarVentaForm == null) {
                                generarVentaForm = new generarVentaFor1();
                            }
                            Application.showForm(generarVentaForm);
                            setMenuHide(true);
                        } catch (SQLException ex) {
                            Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
                            mostrarErrorFormulario("generar venta");
                        }
                    } else {
                        action.cancel();
                        mostrarErrorPermisos();
                    }
                    break;

                case 3: // Comercial
                    if (subIndex == 0) {
                        // Título de sección, no hacer nada
                    } else if (subIndex == 1) { // Clientes
                        if (UserSession.getInstance().hasPermission("clientes")) {
                            Application.showForm(new ClientesForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 2) { // Proveedores
                        if (UserSession.getInstance().hasPermission("proveedores")) {
                            Application.showForm(new ProveedoresForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 3) { // Compras a Proveedores
                        if (UserSession.getInstance().hasPermission("compras")) {
                            Application.showForm(new ComprasForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 4) { // Reporte de ventas
                        if (UserSession.getInstance().hasPermission("reportes_ventas")) {
                            try {
                                Application.showForm(new reporteVentas());
                                setMenuHide(true);
                            } catch (SQLException ex) {
                                Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
                                mostrarErrorFormulario("reporte de ventas");
                            }
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 5) { // Devoluciones
                        if (UserSession.getInstance().hasPermission("devoluciones")) {
                            Application.showForm(new devolucionMainForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }

                    // ═══════════════════════════════════════════════════════════════
                    // subIndex 6 — 🛒 Pedidos Web / Carrito de Órdenes
                    // Reemplaza la clase Carrito antigua por ModelCarritoOrdenesWeb
                    // ═══════════════════════════════════════════════════════════════
                    } else if (subIndex == 6) { // Pedidos Web / Carrito
                        if (UserSession.getInstance().hasPermission("pedidos_web")) {
                            try {
                                // Obtener bodega del usuario autenticado
                                Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
                                int bodegaId = (idBodega != null && idBodega > 0) ? idBodega : 0;

                                // Crear o reutilizar instancia del panel
                                if (carritoOrdenesWebForm == null) {
                                    carritoOrdenesWebForm = new ModelCarritoOrdenesWeb(bodegaId);
                                }
                                Application.showForm(carritoOrdenesWebForm);
                                setMenuHide(true);
                            } catch (Exception ex) {
                                Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE,
                                        "Error al abrir módulo Carrito/Órdenes Web", ex);
                                mostrarErrorFormulario("Pedidos Web / Carrito");
                            }
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }

                    } else if (subIndex == 7) { // Cambio Talla
                        if (UserSession.getInstance().hasPermission("generar venta")) {
                            Application.showForm(new raven.application.form.comercial.FormCambioTalla());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 8) { // Cuentas por Cobrar
                        if (UserSession.getInstance().hasPermission("cuentas_por_cobrar")) {
                            Application.showForm(new raven.application.form.comercial.CuentasPorCobrarForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    }
                    break;

                case 4: // Productos
                    if (subIndex == 0) {
                        // Título de sección
                    } else if (subIndex == 1) {
                        if (UserSession.getInstance().hasPermission("ver_productos")) {
                            try {
                                Application.showForm(new raven.application.form.productos.VerProductosForm());
                                setMenuHide(true);
                            } catch (Exception ex) {
                                action.cancel();
                                mostrarErrorFormulario("ver productos");
                            }
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 2) {
                        if (UserSession.getInstance().hasPermission("gestion de productos")) {
                            Application.showForm(new GestionProductosForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 3) {
                        if (UserSession.getInstance().hasPermission("inventario")) {
                            Application.showForm(new InventarioForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 4) {
                        if (UserSession.getInstance().hasPermission("marcas")) {
                            Application.showForm(new MarcasForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 5) {
                        if (UserSession.getInstance().hasPermission("categorias")) {
                            Application.showForm(new CategoriasForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 6) {
                        if (UserSession.getInstance().hasPermission("movimientos")) {
                            Application.showForm(new MovimientosForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 7) {
                        if (UserSession.getInstance().hasPermission("rotulacion")) {
                            Application.showForm(new RotulacionForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 8) {
                        if (UserSession.getInstance().hasPermission("traspasos")) {
                            Application.showForm(new traspasos());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 9) {
                        if (UserSession.getInstance().hasPermission("rotulacion")) {
                            Application.showForm(new ColorForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 10) {
                        if (UserSession.getInstance().hasPermission("inventario")) {
                            Application.showForm(new PrestamoForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 11) {
                        if (UserSession.getInstance().hasPermission("promociones")) {
                            Application.showForm(new Descuento());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 12) {
                        if (UserSession.getInstance().hasPermission("consulta_detallada")) {
                            Application.showForm(new ConsultaInventarioDetalladoForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    }
                    break;

                case 5: // Reportes
                    if (!UserSession.getInstance().hasPermission("reportes")) {
                        action.cancel();
                        mostrarErrorPermisos();
                        break;
                    }
                    if (subIndex == 0) {
                        Application.showForm(new raven.application.form.reportes.ReportesMainForm());
                        setMenuHide(true);
                    } else if (subIndex == 1) {
                        if (UserSession.getInstance().hasPermission("reporte_inventario")) {
                            Application.showForm(new raven.application.form.reportes.ReporteInventarioForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 2) {
                        if (UserSession.getInstance().hasPermission("reporte_compras")) {
                            Application.showForm(new raven.application.form.reportes.ReporteComprasForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 3) {
                        if (UserSession.getInstance().hasPermission("reporte_gastos")) {
                            Application.showForm(new raven.application.form.reportes.ReporteGastosForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 4) {
                        if (UserSession.getInstance().hasPermission("reporte_devoluciones")) {
                            Application.showForm(new raven.application.form.reportes.ReporteDevolucionesForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 5) {
                        if (UserSession.getInstance().hasPermission("reporte_traspasos")) {
                            Application.showForm(new raven.application.form.reportes.ReporteTraspasoForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 6) {
                        if (UserSession.getInstance().hasPermission("reporte_clientes")) {
                            Application.showForm(new raven.application.form.reportes.ReporteClientesForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 7) {
                        if (UserSession.getInstance().hasPermission("reporte_auditoria")) {
                            Application.showForm(new raven.application.form.reportes.ReporteAuditoriaForm());
                            setMenuHide(true);
                        } else {
                            mostrarErrorPermisos();
                        }
                    }
                    break;

                case 6: // Admin
                    if (subIndex == 0) {
                        // Título de sección
                    } else if (subIndex == 1) {
                        if (UserSession.getInstance().hasPermission("usuarios")) {
                            Application.showForm(new UsuariosForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 2) {
                        if (UserSession.getInstance().hasPermission("bodegas")) {
                            Application.showForm(new BodegasForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 3) {
                        if (UserSession.getInstance().hasPermission("monitor_cajas")
                                || UserSession.getInstance().hasPermission("cajas")) {
                            Application.showForm(new MonitorCajasForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 4) {
                        if (UserSession.getInstance().hasPermission("tipos_gasto")) {
                            Application.showForm(new TiposGastoForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    } else if (subIndex == 5) {
                        if (UserSession.getInstance().hasPermission("configuracion")) {
                            Application.showForm(new GestionPermisosForm());
                            setMenuHide(true);
                        } else {
                            action.cancel();
                            mostrarErrorPermisos();
                        }
                    }
                    break;

                case 7: // Logout
                    UserSession.getInstance().logout();
                    Application.logout();
                    break;

                default:
                    action.cancel();
                    break;
            }
        });
    }

    private void mostrarErrorPermisos() {
        JOptionPane.showMessageDialog(this,
                "No tiene permisos para acceder a esta función",
                "Acceso denegado",
                JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErrorFormulario(String nombreForm) {
        JOptionPane.showMessageDialog(this,
                "Error al cargar el formulario: " + nombreForm,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDACIONES DE CAJA/BODEGA
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean validarBodegaAsignada() {
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay usuario autenticado. Vuelva a iniciar sesión.",
                    "Sesión requerida", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        Integer idBodegaUsuario = session.getIdBodegaUsuario();
        if (idBodegaUsuario == null || idBodegaUsuario <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Su usuario no tiene una bodega asignada.\n\nContacte al administrador.",
                    "Configuración incompleta", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private ModelCaja validarCajaRegistrada() {
        try {
            Integer idBodegaUsuario = UserSession.getInstance().getIdBodegaUsuario();
            ModelCaja cajaAsociada = serviceCaja.obtenerCajaPorBodega(idBodegaUsuario);
            if (cajaAsociada == null) {
                JOptionPane.showMessageDialog(this,
                        "Su bodega no tiene una caja registrada.\n\nContacte al administrador.",
                        "Caja no configurada", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            if (cajaAsociada.getIdCaja() == null || cajaAsociada.getIdCaja() <= 0) {
                JOptionPane.showMessageDialog(this,
                        "La caja tiene un ID inválido.\n\nContacte al administrador.",
                        "Error de sistema", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return cajaAsociada;
        } catch (SQLException ex) {
            Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, "Error al validar caja registrada", ex);
            JOptionPane.showMessageDialog(this,
                    "Error de base de datos al obtener la caja:\n" + ex.getMessage(),
                    "BD - Caja", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private boolean validarOAbrirCaja(ModelCaja caja) {
        try {
            int idUsuarioActual = UserSession.getInstance().getCurrentUser().getIdUsuario();
            int idCajaActiva = caja.getIdCaja();
            ModelCajaMovimiento movimientoAbierto = serviceCajaMovimiento
                    .obtenerMovimientoAbierto(idCajaActiva, idUsuarioActual);
            if (movimientoAbierto != null) {
                UserSession.getInstance().asociarCaja(idCajaActiva, movimientoAbierto.getIdMovimiento());
                return true;
            } else {
                return preguntarYAbrirCaja(caja, idUsuarioActual);
            }
        } catch (SQLException ex) {
            Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, "Error al validar estado de caja", ex);
            JOptionPane.showMessageDialog(this,
                    "Error al verificar el estado de la caja:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean preguntarYAbrirCaja(ModelCaja caja, int idUsuario) {
        String mensaje = String.format(
                "No hay una caja abierta para su sesión.\n\n"
                        + " Usuario: %s\n"
                        + " Caja ID: %d\n\n"
                        + "¿Desea abrir una caja ahora?",
                UserSession.getInstance().getCurrentUser().getNombre(), caja.getIdCaja());
        int opcion = JOptionPane.showConfirmDialog(this, mensaje, "Abrir Caja",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            return mostrarDialogoAperturaCaja(caja, idUsuario);
        }
        return false;
    }

    private boolean mostrarDialogoAperturaCaja(ModelCaja caja, int idUsuario) {
        final boolean[] resultado = { false };
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        AperturaCajaDialog dialogo = new AperturaCajaDialog(frame, caja, idUsuario,
                UserSession.getInstance().getCurrentUser().getNombre(),
                new AperturaCajaDialog.AperturaCajaCallback() {
                    @Override
                    public void onAperturaExitosa(ModelCajaMovimiento movimiento) {
                        UserSession.getInstance().asociarCaja(caja.getIdCaja(), movimiento.getIdMovimiento());
                        resultado[0] = true;
                    }

                    @Override
                    public void onAperturaCancelada() {
                        resultado[0] = false;
                    }
                });
        dialogo.setVisible(true);
        return resultado[0];
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE UI
    // ═══════════════════════════════════════════════════════════════════════════

    private void setMenuFull(boolean full) {
        String icon;
        if (getComponentOrientation().isLeftToRight()) {
            icon = (full) ? "menu_left.svg" : "menu_right.svg";
        } else {
            icon = (full) ? "menu_right.svg" : "menu_left.svg";
        }
        menuButton.setIcon(new FlatSVGIcon("raven/icon/svg/" + icon, 0.8f));
        menu.setMenuFull(full);
        revalidate();
    }

    public void setMenuHide(boolean hide) {
        this.menuHide = hide;
        if (hide) {
            menuButton.setIcon(new FlatSVGIcon("raven/icon/svg/menu_right.svg", 0.8f));
            menuButton.setText("MENÚ");
            menuButton.setToolTipText("Abrir Menú Principal");
            menuButton.putClientProperty(FlatClientProperties.STYLE, ""
                    + "background:#1a73e8;"
                    + "foreground:#FFFFFF;"
                    + "font:bold +2;"
                    + "arc:10;"
                    + "focusWidth:0;"
                    + "borderWidth:2;"
                    + "borderColor:#FFFFFF;"
                    + "margin:6,12,6,12;"
                    + "iconTextGap:8");
        } else {
            String icon;
            if (getComponentOrientation().isLeftToRight()) {
                icon = (menu.isMenuFull()) ? "menu_left.svg" : "menu_right.svg";
            } else {
                icon = (menu.isMenuFull()) ? "menu_right.svg" : "menu_left.svg";
            }
            menuButton.setIcon(new FlatSVGIcon("raven/icon/svg/" + icon, 0.8f));
            menuButton.setText(null);
            menuButton.setToolTipText(null);
            menuButton.putClientProperty(FlatClientProperties.STYLE, ""
                    + "background:$Menu.button.background;"
                    + "arc:999;"
                    + "focusWidth:0;"
                    + "borderWidth:0");
        }
        revalidate();
    }

    public void hideMenu() {
        menu.hideMenuItem();
    }

    public void showForm(Component component) {
        panelBody.removeAll();
        panelBody.add(component);
        panelBody.repaint();
        panelBody.revalidate();
    }

    public void setSelectedMenu(int index, int subIndex) {
        menu.setSelectedMenu(index, subIndex);
    }

    /**
     * Limpia el formulario de venta persistente al cerrar sesión.
     */
    public void limparFormularioVenta() {
        if (generarVentaForm != null) {
            generarVentaForm = null;
        }
        // También limpiar instancia del carrito para que se recree con la bodega correcta
        if (carritoOrdenesWebForm != null) {
            carritoOrdenesWebForm = null;
        }
    }

    private generarVentaFor1 generarVentaForm;
    private Menu menu;
    private JPanel panelBody;
    private JButton menuButton;
    private boolean menuHide;

    private class MainFormLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(5, 5);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(0, 0);
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                boolean ltr = parent.getComponentOrientation().isLeftToRight();
                Insets insets = UIScale.scale(parent.getInsets());
                int x = insets.left;
                int y = insets.top;
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int menuWidth = 0;
                if (!menuHide) {
                    menuWidth = UIScale.scale(menu.isMenuFull() ? menu.getMenuMaxWidth() : menu.getMenuMinWidth());
                }
                int menuX = ltr ? x : x + width - menuWidth;
                menu.setBounds(menuX, y, menuWidth, height);
                int menuButtonWidth = menuButton.getPreferredSize().width;
                int menuButtonHeight = menuButton.getPreferredSize().height;
                int menubX;
                if (ltr) {
                    menubX = (int) (x + menuWidth
                            - (menuButtonWidth * (menuHide ? 0.2f : (menu.isMenuFull() ? 0.5f : 0.3f))));
                } else {
                    menubX = (int) (menuX - (menuButtonWidth * (menuHide ? 0.8f : (menu.isMenuFull() ? 0.5f : 0.7f))));
                }
                menuButton.setBounds(menubX, UIScale.scale(5), menuButtonWidth, menuButtonHeight);
                int gap = UIScale.scale(5);
                int bodyWidth = width - menuWidth - gap;
                int bodyHeight = height;
                int bodyx = ltr ? (x + menuWidth + gap) : x;
                int bodyy = y;
                panelBody.setBounds(bodyx, bodyy, bodyWidth, bodyHeight);
            }
        }
    }
}
