package raven.application.form.principal;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Optional;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import raven.clases.principal.ServiceGastoOperativo;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.controlador.principal.GastosComprasController;
import raven.controlador.principal.ModelTipoGasto;
import raven.controlador.principal.ModelColor;
import raven.controlador.principal.ModelCompraExterna;
import raven.controlador.principal.ModelCompraExternaDetalle;
import raven.controlador.principal.ModelTalla;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * GASTOS FORM UI - VERSIÓN MEJORADA CON TEMAS PERSONALIZABLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Sistema de estilos centralizado con colores fáciles de cambiar.
 * Modifica la clase AppTheme para cambiar los colores globales.
 * 
 * @author CrisDEV
 * @version 2.0 - Con sistema de temas
 */
public class GastosFormUI {

    // ═══════════════════════════════════════════════════════════════════════════
    // Tema SISTEMA DE TEMAS - MODIFICAR AQUÍ PARA CAMBIAR COLORES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class AppTheme {
        // ───────────────────────────────────────────────────────
        // COLORES PRINCIPALES - MODIFICAR SEGÚN NECESIDAD
        // ───────────────────────────────────────────────────────

        // Fondos
        public static final Color BG_PRINCIPAL = new Color(31, 33, 33); // Gris oscuro principal
        public static final Color BG_PANEL = new Color(38, 40, 40); // Gris oscuro secundario
        public static final Color BG_TAB_ACTIVE = new Color(50, 184, 198); // Verde/Teal activo
        public static final Color BG_TAB_INACTIVE = new Color(60, 70, 75); // Gris tabulador inactivo

        // Botones
        public static final Color BTN_PRIMARY = new Color(50, 184, 198); // Verde/Teal primario
        public static final Color BTN_PRIMARY_HOVER = new Color(45, 170, 185); // Verde/Teal hover
        public static final Color BTN_PRIMARY_PRESS = new Color(41, 150, 161); // Verde/Teal presionado

        public static final Color BTN_SECONDARY = new Color(70, 75, 80); // Gris secundario
        public static final Color BTN_SECONDARY_HOVER = new Color(85, 90, 95); // Gris hover

        public static final Color BTN_DANGER = new Color(220, 20, 60); // Rojo peligro
        public static final Color BTN_DANGER_HOVER = new Color(200, 15, 50);

        // Texto
        public static final Color TEXT_PRIMARY = new Color(245, 245, 245); // Blanco/Gris claro
        public static final Color TEXT_SECONDARY = new Color(167, 169, 169); // Gris medio

        // Bordes
        public static final Color BORDER_COLOR = new Color(80, 85, 90); // Gris para bordes
        public static final Color BORDER_FOCUS = new Color(50, 184, 198); // Teal para focus

        // Tabla
        public static final Color TABLE_ROW_ALT = new Color(45, 47, 48); // Alternancia de filas
        public static final Color TABLE_HEADER = new Color(60, 65, 70); // Header de tabla
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFERENCIAS
    // ═══════════════════════════════════════════════════════════════════════════

    private final gastosForm form;
    private final GastosComprasController controller;
    private static final DecimalFormat FORMAT_MONTO = new DecimalFormat("$#,##0");

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public GastosFormUI(gastosForm form) {
        this.form = form;
        this.controller = new GastosComprasController();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    public void inicializar() {
        inicializar(null);
    }

    public void inicializar(Integer idMovimientoCaja) {
        configurarCallbacks();

        if (idMovimientoCaja != null) {
            controller.inicializar(idMovimientoCaja);
        } else {
            controller.inicializar();
        }

        cargarComboBoxes();
        configurarListeners();
        configurarTablaGastos();
        actualizarInfoCaja();
        configurarEstilosUI();
        limpiarFormularioGasto();
        limpiarFormularioCompra();

        System.out.println("SUCCESS  GastosFormUI inicializado correctamente");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE UI CON TEMAS CENTRALIZADOS
    // ═══════════════════════════════════════════════════════════════════════════

    private void configurarEstilosUI() {
        try {
            // ═══════════════════════════════════════════════════════════════════════
            // CAMPOS DE TEXTO - Estilo moderno con esquinas redondeadas
            // ═══════════════════════════════════════════════════════════════════════
            String estiloTexto = "arc:8;" +
                    "focusWidth:1;" +
                    "innerFocusWidth:0;" +
                    "borderWidth:1;";

            // Gastos internos
            aplicarEstiloTextField(form.getTxtConcepto(), estiloTexto, "Ej: Envío de paquete a cliente");
            aplicarEstiloTextField(form.getTxtMonto(), estiloTexto, "$ 25,000");
            aplicarEstiloTextField(form.getTxtTercero(), estiloTexto, "Ej: Servientrega");
            aplicarEstiloTextField(form.getTxtRecibo(), estiloTexto, "Ej: REC-4521");
            aplicarEstiloTextField(form.getTxtObservaciones(), estiloTexto, "Ej: Envío express para cliente VIP");

            // Compras externas
            aplicarEstiloTextField(form.getTxtTerceroCompraZ(), estiloTexto, "Ej: Calzado Express Centro");
            aplicarEstiloTextField(form.getTxtReciboZ(), estiloTexto, "Ej: FAC-2024-1123");
            aplicarEstiloTextField(form.getTxtDescripZ(), estiloTexto, "Ej: Tenis Deportivo Negro/Blanco");
            aplicarEstiloTextField(form.getTxtCompraPrecio(), estiloTexto, "$ 85,000");
            aplicarEstiloTextField(form.getTxtVentaPrecio(), estiloTexto, "$ 120,000");

            // ═══════════════════════════════════════════════════════════════════════
            // COMBOBOXES - Estilo consistente con campos de texto
            // ═══════════════════════════════════════════════════════════════════════
            String estiloCombo = "arc:8;" +
                    "focusWidth:1;" +
                    "borderWidth:1;";

            form.getCbxTipoGasto().putClientProperty(FlatClientProperties.STYLE, estiloCombo);
            form.getCmbTalla().putClientProperty(FlatClientProperties.STYLE, estiloCombo);
            form.getCmbColor().putClientProperty(FlatClientProperties.STYLE, estiloCombo);

            // ═══════════════════════════════════════════════════════════════════════
            // BOTÓN PRINCIPAL - Registrar Gasto/Compra (Verde llamativo)
            // ═══════════════════════════════════════════════════════════════════════
            FontIcon iconGuardar = createColoredIcon(FontAwesomeSolid.SAVE, Color.WHITE);
            form.getBtnRegistrarGasto().setIcon(iconGuardar);
            form.getBtnRegistrarGasto().setIconTextGap(8);
            form.getBtnRegistrarGasto().putClientProperty(FlatClientProperties.STYLE,
                    "arc:8;" +
                            "focusWidth:0;" +
                            "borderWidth:0;" +
                            "background:$App.accent.blue;" +
                            "foreground:#ffffff;" +
                            "hoverBackground:darken($App.accent.blue,10%);" +
                            "pressedBackground:darken($App.accent.blue,20%);");

            // ═══════════════════════════════════════════════════════════════════════
            // BOTÓN AGREGAR ITEM (Verde con ícono +)
            // ═══════════════════════════════════════════════════════════════════════
            if (form.getBtnAgregarItem() != null) {
                FontIcon iconAgregar = createColoredIcon(FontAwesomeSolid.PLUS, Color.WHITE);
                form.getBtnAgregarItem().setIcon(iconAgregar);
                form.getBtnAgregarItem().setIconTextGap(6);
                form.getBtnAgregarItem().putClientProperty(FlatClientProperties.STYLE,
                        "arc:8;" +
                                "focusWidth:0;" +
                                "borderWidth:0;" +
                                "background:#22c55e;" +
                                "foreground:#ffffff;" +
                                "hoverBackground:#16a34a;" +
                                "pressedBackground:#15803d;");
            }

            // ═══════════════════════════════════════════════════════════════════════
            // BOTÓN LIMPIAR (Gris neutro)
            // ═══════════════════════════════════════════════════════════════════════
            FontIcon iconLimpiar = createColoredIcon(FontAwesomeSolid.ERASER, Color.WHITE);
            form.getBtnLimpiar().setIcon(iconLimpiar);
            form.getBtnLimpiar().setIconTextGap(6);
            form.getBtnLimpiar().putClientProperty(FlatClientProperties.STYLE,
                    "arc:8;" +
                            "focusWidth:0;" +
                            "borderWidth:0;" +
                            "background:#4b5563;" +
                            "foreground:#ffffff;" +
                            "hoverBackground:#6b7280;" +
                            "pressedBackground:#374151;");

            // ═══════════════════════════════════════════════════════════════════════
            // BOTÓN CERRAR (Gris oscuro)
            // ═══════════════════════════════════════════════════════════════════════
            FontIcon iconCerrar = createColoredIcon(FontAwesomeSolid.TIMES, Color.WHITE);
            form.getBtnCerrar().setIcon(iconCerrar);
            form.getBtnCerrar().setIconTextGap(6);
            form.getBtnCerrar().putClientProperty(FlatClientProperties.STYLE,
                    "arc:8;" +
                            "focusWidth:0;" +
                            "borderWidth:0;" +
                            "background:#374151;" +
                            "foreground:#ffffff;" +
                            "hoverBackground:#4b5563;" +
                            "pressedBackground:#1f2937;");

            // Botón X de cerrar en esquina
            FontIcon iconClose = createColoredIcon(FontAwesomeSolid.TIMES, new Color(239, 68, 68));
            form.getBtnClose().setIcon(iconClose);
            form.getBtnClose().putClientProperty(FlatClientProperties.STYLE,
                    "arc:8;" +
                            "focusWidth:0;" +
                            "borderWidth:0;");

            // ═══════════════════════════════════════════════════════════════════════
            // SPINNER - Cantidad con estilo moderno
            // ═══════════════════════════════════════════════════════════════════════
            form.getSpnCantidad().putClientProperty(FlatClientProperties.STYLE,
                    "arc:8;" +
                            "focusWidth:1;" +
                            "borderWidth:1;" +
                            "buttonStyle:chevron;");

            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 999, 1);
            form.getSpnCantidad().setModel(spinnerModel);

            // ═══════════════════════════════════════════════════════════════════════
            // TABLAS - Estilo moderno sin líneas verticales
            // ═══════════════════════════════════════════════════════════════════════
            String estiloTabla = "showHorizontalLines:true;" +
                    "showVerticalLines:false;" +
                    "intercellSpacing:0,1;" +
                    "rowHeight:40;" +
                    "selectionArc:8;";

            String estiloHeader = "font:bold +1;" +
                    "separatorColor:$Table.gridColor;" +
                    "bottomSeparatorColor:$Table.gridColor;";

            form.getTablaGastos().putClientProperty(FlatClientProperties.STYLE, estiloTabla);
            form.getTablaGastos().getTableHeader().putClientProperty(FlatClientProperties.STYLE, estiloHeader);

            if (form.getTablaCompras() != null) {
                form.getTablaCompras().putClientProperty(FlatClientProperties.STYLE, estiloTabla);
                form.getTablaCompras().getTableHeader().putClientProperty(FlatClientProperties.STYLE, estiloHeader);
            }

            // ═══════════════════════════════════════════════════════════════════════
            // LABELS DE TOTALES - Fuente grande y color verde para montos
            // ═══════════════════════════════════════════════════════════════════════
            String estiloLabelTotal = "font:bold +4;";
            form.getTxtTotalGastos().putClientProperty(FlatClientProperties.STYLE, estiloLabelTotal);
            form.getTxtTotalGastos().setForeground(new Color(34, 197, 94)); // Verde para montos

            form.getTxtSubTotal().putClientProperty(FlatClientProperties.STYLE, estiloLabelTotal);
            form.getTxtSubTotal().setForeground(new Color(34, 197, 94)); // Verde para montos

            // ═══════════════════════════════════════════════════════════════════════
            // LABEL DE INFO MOVIMIENTO CAJA
            // ═══════════════════════════════════════════════════════════════════════
            form.getTxtMovimientoCaja().putClientProperty(FlatClientProperties.STYLE,
                    "font:-1;");
            form.getTxtMovimientoCaja().setForeground(new Color(156, 163, 175)); // Gris suave

            System.out.println("SUCCESS  Estilos FlatLaf + Iconos FontAwesome aplicados correctamente");

        } catch (Exception ex) {
            System.err.println("WARNING  Error aplicando estilos: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Crea un icono FontAwesome coloreado
     */
    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18); // Tamaño del icono
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    /**
     * Aplica estilos a un JTextField con placeholder
     */
    private void aplicarEstiloTextField(JTextField campo, String estilo, String placeholder) {
        campo.putClientProperty(FlatClientProperties.STYLE, estilo);
        campo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        campo.setForeground(AppTheme.TEXT_PRIMARY);
        campo.setCaretColor(AppTheme.BG_TAB_ACTIVE);
    }

    /**
     * Convierte un Color a formato hexadecimal para FlatLaf
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES DE ESTILOS
    // ═══════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════

    private void configurarCallbacks() {
        // Errores
        controller.setOnError(mensaje -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(form, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
            });
        });

        // Éxito
        controller.setOnSuccess(mensaje -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(form, mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            });
        });

        // Actualización de gastos
        controller.setOnGastosUpdated(gastos -> {
            SwingUtilities.invokeLater(this::actualizarTablaGastos);
        });

        // Total de gastos
        controller.setOnTotalGastosUpdated(total -> {
            SwingUtilities.invokeLater(() -> {
                form.getTxtTotalGastos().setText(FORMAT_MONTO.format(total));
            });
        });

        // Detalles de compra actualizados
        controller.setOnDetallesCompraUpdated(detalles -> {
            SwingUtilities.invokeLater(this::actualizarTablaCompra);
        });

        // Total de compra
        controller.setOnTotalCompraUpdated(total -> {
            SwingUtilities.invokeLater(() -> {
                form.getTxtSubTotal().setText(FORMAT_MONTO.format(total));
            });
        });

        // Limpiar formulario
        controller.setOnFormCleared(() -> {
            SwingUtilities.invokeLater(() -> {
                int tabActual = form.getPanelGastosInternos().getSelectedIndex();
                if (tabActual == 0) {
                    limpiarFormularioGasto();
                } else {
                    limpiarItemCompra();
                }
            });
        });

        // Compra registrada
        controller.setOnCompraRegistrada((idCompraExterna) -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    Optional<ModelCompraExterna> compraOpt = controller.getService().obtenerCompra(idCompraExterna);
                    if (compraOpt.isPresent()) {
                        ModelCompraExterna compra = compraOpt.get();
                        mostrarResumenCompraExterna(
                                compra.getTiendaProveedor(),
                                compra.getNumeroFacturaRecibo(),
                                compra.getDetalles());
                    } else {
                        JOptionPane.showMessageDialog(form,
                                "Compra registrada pero no se pudo recuperar.",
                                "WARNING  Advertencia",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(form,
                            "Error al recuperar compra: " + ex.getMessage(),
                            "ERROR  Error",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGA DE COMBOS
    // ═══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void cargarComboBoxes() {
        // Tipos de gasto
        JComboBox<ModelTipoGasto> cbxTipo = (JComboBox<ModelTipoGasto>) form.getCbxTipoGasto();
        cbxTipo.removeAllItems();
        cbxTipo.addItem(null);
        for (ModelTipoGasto tipo : controller.getTiposGasto()) {
            cbxTipo.addItem(tipo);
        }

        // Tallas
        JComboBox<ModelTalla> cmbTalla = (JComboBox<ModelTalla>) form.getCmbTalla();
        cmbTalla.removeAllItems();
        cmbTalla.addItem(null);
        for (ModelTalla talla : controller.getTallas()) {
            cmbTalla.addItem(talla);
        }

        // Colores
        JComboBox<ModelColor> cmbColor = (JComboBox<ModelColor>) form.getCmbColor();
        cmbColor.removeAllItems();
        cmbColor.addItem(null);
        for (ModelColor color : controller.getColores()) {
            cmbColor.addItem(color);
        }

        // Configurar renderers
        configurarRendererComboBox(cbxTipo, "Seleccione tipo de gasto");
        configurarRendererComboBox(cmbTalla, "Seleccione talla");
        configurarRendererComboBox(cmbColor, "Seleccione color");
    }

    private void configurarRendererComboBox(JComboBox combo, String placeholder) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value == null) {
                    setText(placeholder);
                    setForeground(AppTheme.TEXT_SECONDARY);
                } else {
                    setText(value.toString());
                    setForeground(isSelected ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_PRIMARY);
                }

                setBackground(isSelected ? AppTheme.BG_TAB_ACTIVE : AppTheme.BG_PANEL);
                return this;
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void configurarListeners() {
        // BOTÓN REGISTRAR
        form.getBtnRegistrarGasto().addActionListener(e -> {
            int tabActual = form.getPanelGastosInternos().getSelectedIndex();
            if (tabActual == 0) {
                registrarGasto();
            } else {
                registrarCompraExterna();
            }
        });

        // BOTÓN AGREGAR ITEM
        if (form.getBtnAgregarItem() != null) {
            form.getBtnAgregarItem().addActionListener(e -> agregarItemCompra());
        }

        // BOTÓN LIMPIAR
        form.getBtnLimpiar().addActionListener(e -> {
            int tabActual = form.getPanelGastosInternos().getSelectedIndex();
            if (tabActual == 0) {
                limpiarFormularioGasto();
            } else {
                limpiarFormularioCompra();
            }
        });

        // BOTONES CERRAR
        form.getBtnCerrar().addActionListener(e -> form.dispose());
        form.getBtnClose().addActionListener(e -> form.dispose());

        // CAMBIO DE TAB
        form.getPanelGastosInternos().addChangeListener(e -> {
            int tabActual = form.getPanelGastosInternos().getSelectedIndex();
            actualizarBotonPrincipal(tabActual);
        });

        // VALIDACIÓN DE MONTO
        configurarValidacionMonto(form.getTxtMonto());
        configurarValidacionMonto(form.getTxtCompraPrecio());
        configurarValidacionMonto(form.getTxtVentaPrecio());

        // CÁLCULO AUTOMÁTICO
        form.getSpnCantidad().addChangeListener(e -> calcularSubtotalItem());
        form.getTxtCompraPrecio().addActionListener(e -> calcularSubtotalItem());
    }

    private void configurarValidacionMonto(JTextField campo) {
        campo.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != '.' && c != ',' && c != java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
                // Allow one dot OR one comma as decimal separator?
                // For simplicity, just allow both but maybe prevent multiple
                if ((c == '.' && campo.getText().contains(".")) || (c == ',' && campo.getText().contains(","))) {
                    e.consume();
                }
            }
        });
    }

    private void actualizarBotonPrincipal(int tabIndex) {
        if (tabIndex == 0) {
            form.getBtnRegistrarGasto().setText("Registrar Gasto");
        } else {
            form.getBtnRegistrarGasto().setText("Registrar Compra");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCIONES DE GASTOS
    // ═══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void registrarGasto() {
        JComboBox<ModelTipoGasto> cbxTipo = (JComboBox<ModelTipoGasto>) form.getCbxTipoGasto();
        ModelTipoGasto tipoSeleccionado = (ModelTipoGasto) cbxTipo.getSelectedItem();
        Integer idTipoGasto = tipoSeleccionado != null ? tipoSeleccionado.getIdTipoGasto() : null;

        String concepto = form.getTxtConcepto().getText().trim();
        BigDecimal monto = parsearMonto(form.getTxtMonto().getText());
        String proveedor = form.getTxtTercero().getText().trim();
        String numeroRecibo = form.getTxtRecibo().getText().trim();
        String observaciones = form.getTxtObservaciones().getText().trim();

        controller.registrarGasto(
                idTipoGasto,
                concepto,
                monto,
                proveedor.isEmpty() ? null : proveedor,
                numeroRecibo.isEmpty() ? null : numeroRecibo,
                observaciones.isEmpty() ? null : observaciones);
    }

    private void limpiarFormularioGasto() {
        form.getCbxTipoGasto().setSelectedIndex(0);
        form.getTxtConcepto().setText("");
        form.getTxtMonto().setText("");
        form.getTxtTercero().setText("");
        form.getTxtRecibo().setText("");
        form.getTxtObservaciones().setText("");
        form.getTxtConcepto().requestFocus();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCIONES DE COMPRAS EXTERNAS
    // ═══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public void agregarItemCompra() {
        JComboBox<ModelTalla> cmbTalla = (JComboBox<ModelTalla>) form.getCmbTalla();
        JComboBox<ModelColor> cmbColor = (JComboBox<ModelColor>) form.getCmbColor();

        ModelTalla talla = (ModelTalla) cmbTalla.getSelectedItem();
        ModelColor color = (ModelColor) cmbColor.getSelectedItem();
        Integer idTalla = talla != null ? talla.getIdTalla() : null;
        Integer idColor = color != null ? color.getIdColor() : null;

        String descripcion = form.getTxtDescripZ().getText().trim();
        int cantidad = (Integer) form.getSpnCantidad().getValue();
        BigDecimal precioCompra = parsearMonto(form.getTxtCompraPrecio().getText());
        BigDecimal precioVenta = parsearMonto(form.getTxtVentaPrecio().getText());

        controller.agregarItemCompra(
                idTalla,
                idColor,
                descripcion.isEmpty() ? null : descripcion,
                cantidad,
                precioCompra,
                precioVenta);

        limpiarItemCompra();
    }

    private void registrarCompraExterna() {
        String tiendaProveedor = form.getTxtTerceroCompraZ().getText().trim();
        String numeroFactura = form.getTxtReciboZ().getText().trim();

        try {
            controller.registrarCompraExterna(
                    tiendaProveedor,
                    numeroFactura.isEmpty() ? null : numeroFactura,
                    null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    form,
                    "Error al registrar la compra externa:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarItemCompra() {
        form.getCmbTalla().setSelectedIndex(0);
        form.getCmbColor().setSelectedIndex(0);
        form.getTxtDescripZ().setText("");
        form.getSpnCantidad().setValue(1);
        form.getTxtCompraPrecio().setText("");
        form.getTxtVentaPrecio().setText("");
    }

    private void limpiarFormularioCompra() {
        form.getTxtTerceroCompraZ().setText("");
        form.getTxtReciboZ().setText("");
        limpiarItemCompra();
        controller.limpiarCompraActual();
    }

    private void calcularSubtotalItem() {
        int cantidad = (Integer) form.getSpnCantidad().getValue();
        BigDecimal precio = parsearMonto(form.getTxtCompraPrecio().getText());
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
        System.out.println("Resumen Subtotal item: " + FORMAT_MONTO.format(subtotal));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GESTIÓN DE TABLAS
    // ═══════════════════════════════════════════════════════════════════════════

    private void configurarTablaGastos() {
        String[] columnas = { "ID", "Tipo", "Concepto", "Monto", "Proveedor", "Estado" };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        form.getTablaGastos().setModel(modelo);
        form.getTablaGastos().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        form.getTablaGastos().setRowSelectionAllowed(true);

        // Configurar anchos
        form.getTablaGastos().getColumnModel().getColumn(0).setPreferredWidth(50);
        form.getTablaGastos().getColumnModel().getColumn(1).setPreferredWidth(120);
        form.getTablaGastos().getColumnModel().getColumn(2).setPreferredWidth(100);
        form.getTablaGastos().getColumnModel().getColumn(3).setPreferredWidth(100);
        form.getTablaGastos().getColumnModel().getColumn(4).setPreferredWidth(120);
        form.getTablaGastos().getColumnModel().getColumn(5).setPreferredWidth(80);

        // Menú contextual
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuAnular = new JMenuItem("Anular Gasto");

        menuAnular.addActionListener(e -> {
            int filaSeleccionada = form.getTablaGastos().getSelectedRow();
            if (filaSeleccionada >= 0) {
                int idGasto = (Integer) form.getTablaGastos().getValueAt(filaSeleccionada, 0);
                controller.anularGasto(idGasto);
            }
        });

        popupMenu.add(menuAnular);
        form.getTablaGastos().setComponentPopupMenu(popupMenu);
    }

    private void actualizarTablaGastos() {
        DefaultTableModel modelo = controller.crearModeloTablaGastos();
        form.getTablaGastos().setModel(modelo);

        if (form.getTablaGastos().getColumnCount() >= 6) {
            form.getTablaGastos().getColumnModel().getColumn(0).setPreferredWidth(50);
            form.getTablaGastos().getColumnModel().getColumn(1).setPreferredWidth(100);
            form.getTablaGastos().getColumnModel().getColumn(2).setPreferredWidth(100);
            form.getTablaGastos().getColumnModel().getColumn(3).setPreferredWidth(100);
            form.getTablaGastos().getColumnModel().getColumn(4).setPreferredWidth(100);
            form.getTablaGastos().getColumnModel().getColumn(5).setPreferredWidth(80);
        }
    }

    private void actualizarTablaCompra() {
        if (form.getTablaCompras() != null) {
            DefaultTableModel modelo = controller.crearModeloTablaCompra();
            form.getTablaCompras().setModel(modelo);
        }

        form.getTxtSubTotal().setText(FORMAT_MONTO.format(controller.calcularTotalCompraActual()));
    }

    private void mostrarResumenCompraExterna(
            String tienda,
            String numeroFactura,
            java.util.List<ModelCompraExternaDetalle> detalles) {

        if (detalles == null || detalles.isEmpty()) {
            JOptionPane.showMessageDialog(form,
                    "Compra registrada pero sin detalles.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("COMPRA EXTERNA REGISTRADA EXITOSAMENTE\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append("Tienda / Proveedor: ")
                .append(tienda == null || tienda.isBlank() ? "-" : tienda).append('\n');
        sb.append("Factura / Recibo: ")
                .append(numeroFactura == null || numeroFactura.isBlank() ? "-" : numeroFactura)
                .append('\n');
        sb.append('\n');
        sb.append("DETALLES:\n");
        sb.append("───────────────────────────────────────────────────\n");

        for (ModelCompraExternaDetalle d : detalles) {
            sb.append("\n- ").append(d.getDescripcionCompleta()).append('\n');
            sb.append(" Cantidad: ").append(d.getCantidad()).append(" pares\n");
            sb.append(" Precio: ").append(FORMAT_MONTO.format(d.getPrecioUnitario())).append('\n');
            sb.append(" Subtotal: ").append(FORMAT_MONTO.format(d.getSubtotal())).append('\n');

            if (d.getEan() != null && !d.getEan().isEmpty()) {
                sb.append(" EAN EAN: ").append(d.getEan()).append('\n');
            }

            if (d.getSku() != null && !d.getSku().isEmpty()) {
                sb.append(" SKU: ").append(d.getSku()).append('\n');
            }
        }

        sb.append("\n───────────────────────────────────────────────────\n");
        sb.append("Total: ").append(FORMAT_MONTO.format(
                detalles.stream()
                        .map(ModelCompraExternaDetalle::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .append('\n');

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(new Font("Courier New", Font.PLAIN, 11));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(650, 450));

        JOptionPane.showMessageDialog(
                form,
                scroll,
                "SUCCESS  Resumen de Compra Externa",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════

    private void actualizarInfoCaja() {
        String infoCaja = controller.getInfoMovimientoCaja();

        // Agregar información de efectivo disponible si hay movimiento activo
        try {
            ServiceCajaMovimiento serviceCaja = new ServiceCajaMovimiento();
            Integer idMovimiento = raven.clases.admin.UserSession.getInstance().getIdMovimientoActual();
            if (idMovimiento != null && idMovimiento > 0) {
                java.math.BigDecimal efectivo = serviceCaja.obtenerEfectivoDisponible(idMovimiento);
                infoCaja += String.format(" | Efectivo Efectivo disponible: $%,.0f", efectivo);
            }
        } catch (Exception e) {
            System.err.println("WARNING  Error obteniendo efectivo disponible: " + e.getMessage());
        }

        form.getTxtMovimientoCaja().setText(infoCaja);
    }

    private BigDecimal parsearMonto(String texto) {
        return raven.utils.MoneyHelper.parse(texto);
    }

    public GastosComprasController getController() {
        return controller;
    }
}
