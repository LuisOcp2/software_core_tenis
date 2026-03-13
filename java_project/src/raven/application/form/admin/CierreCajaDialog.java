package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.clases.productos.ImpresionCierreCaja;
import raven.componentes.LoadingOverlayHelper;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ResumenCierreCaja;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Diálogo para cierre y cuadre de caja registradora.
 *
 * Muestra un resumen del turno y permite realizar el cuadre de caja. Aplica el
 * Patrón Template Method y Single Responsibility Principle.
 *
 * @author Sistema
 * @version 2.0
 */
public class CierreCajaDialog extends JDialog {

    // ==================== COMPONENTES UI ====================
    private JLabel lblTitulo;
    private JLabel lblInfoMovimiento;
    private JLabel lblMontoInicial;
    private JLabel lblTotalVentas;
    private JLabel lblMontoEsperado;
    private JTextField txtMontoFinal;
    private JLabel lblDiferencia;
    private JTextArea txtObservaciones;
    private JButton btnCerrar;
    private JButton btnCancelar;

    // Panel de resumen
    private JPanel panelResumen;

    // ==================== NUEVOS COMPONENTES ====================
    private JLabel lblNumeroVentas; // Cantidad de ventas
    private JLabel lblTotalPagosRecibidos; // Cantidad de pagos
    private JTable tablaDesglosePagos; // Tabla con desglose por tipo
    private JPanel panelDesglosePagos; // Panel contenedor del desglose
    private ResumenCierreCaja resumenCierre; // Datos del resumen

    // ==================== COMPONENTES EGRESOS ====================
    private JPanel panelEgresos; // Panel de egresos (gastos + compras)
    private JLabel lblGastosOperativos; // Monto de gastos
    private JLabel lblComprasExternas; // Monto de compras
    private JLabel lblTotalEgresos; // Total de egresos

    // ==================== SERVICIOS Y DATOS ====================
    private final ServiceCajaMovimiento serviceCaja;
    private final ModelCajaMovimiento movimiento;
    private boolean cierreExitoso = false;
    private BigDecimal montoEsperadoCorregido; // Monto esperado real (con egresos restados)

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

    // ==================== CONSTRUCTOR ====================
    /**
     * Constructor del diálogo de cierre.
     *
     * @param parent     Ventana padre
     * @param movimiento Movimiento de caja a cerrar
     */
    public CierreCajaDialog(Frame parent, ModelCajaMovimiento movimiento) {
        super(parent, "Cierre de Caja", true);

        this.serviceCaja = new ServiceCajaMovimiento();
        this.movimiento = movimiento;

        // Recargar datos actualizados del movimiento
        try {
            ModelCajaMovimiento actualizado = serviceCaja.obtenerMovimientoPorId(
                    movimiento.getIdMovimiento());
            if (actualizado != null) {
                this.movimiento.setTotalVentas(actualizado.getTotalVentas());
            }
        } catch (SQLException e) {
            System.err.println("Error recargando movimiento: " + e.getMessage());
        }

        initComponents();
        configurarEstilos();
        cargarDatosMovimiento();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        ajustarDialogoAlAreaVisible(parent);
        setLocationRelativeTo(parent);
        setResizable(true);
    }

    // ==================== INICIALIZACIÓN DE COMPONENTES ====================
    /**
     * Inicializa los componentes del formulario.
     */
    private void initComponents() {
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new BorderLayout(0, 15));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        // ===== PANEL SUPERIOR (Título) =====
        JPanel panelSuperior = crearPanelSuperior();

        // ===== PANEL CENTRAL (Resumen, Desglose y Formulario) =====
        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));

        panelResumen = crearPanelResumen();
        panelDesglosePagos = crearPanelDesglosePagos();
        panelEgresos = crearPanelEgresos(); // NUEVO: Panel de egresos
        JPanel panelFormulario = crearPanelFormulario();

        panelCentral.add(panelResumen);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(panelDesglosePagos);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(panelEgresos); // NUEVO: Agregar panel de egresos
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(panelFormulario);

        // Usar JScrollPane para contenido largo
        JScrollPane scrollPane = new JScrollPane(panelCentral);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // ===== PANEL INFERIOR (Botones) =====
        JPanel panelBotones = crearPanelBotones();

        // Agregar paneles al panel principal
        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);
        panelPrincipal.add(scrollPane, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        add(panelPrincipal);

        // Aumentar tamaño mínimo por contenido adicional
        setMinimumSize(new Dimension(640, 560));
    }

    private void ajustarDialogoAlAreaVisible(Window parent) {
        Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int maxW = Math.max(640, maxBounds.width - 40);
        int maxH = Math.max(560, maxBounds.height - 40);

        int newW = Math.min(getWidth(), maxW);
        int newH = Math.min(getHeight(), maxH);

        if (newW != getWidth() || newH != getHeight()) {
            setSize(new Dimension(newW, newH));
        }

        if (parent != null) {
            Rectangle p = parent.getBounds();
            int x = p.x + (p.width - getWidth()) / 2;
            int y = p.y + (p.height - getHeight()) / 2;
            setLocation(Math.max(maxBounds.x, x), Math.max(maxBounds.y, y));
        } else {
            setLocation(Math.max(maxBounds.x, getX()), Math.max(maxBounds.y, getY()));
        }
    }

    /**
     * Crea el panel superior con título.
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        lblTitulo = new JLabel("Efectivo Cierre de Caja");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblTitulo.setIcon(svgIcon("raven/icon/svg/caja.svg", 22, UIManager.getColor("Label.foreground")));
        lblTitulo.setIconTextGap(10);

        lblInfoMovimiento = new JLabel();
        lblInfoMovimiento.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblInfoMovimiento.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator separador = new JSeparator();
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        panel.add(lblTitulo);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblInfoMovimiento);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(separador);

        return panel;
    }

    /**
     * Crea el panel de resumen del turno.
     */
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 3, 20, 15)); // 2 filas, 3 columnas
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                " Resumen del Turno",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));

        // Fila 1: Monto Inicial, Total Ventas, Monto Esperado
        JPanel panelMontoInicial = crearItemResumen("Dinero Monto Inicial:", "");
        lblMontoInicial = (JLabel) panelMontoInicial.getComponent(1);

        JPanel panelTotalVentas = crearItemResumen("� Total Ventas:", "");
        lblTotalVentas = (JLabel) panelTotalVentas.getComponent(1);

        JPanel panelMontoEsperado = crearItemResumen("Resumen Monto Esperado:", "");
        lblMontoEsperado = (JLabel) panelMontoEsperado.getComponent(1);
        lblMontoEsperado.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMontoEsperado.setForeground(new Color(0, 122, 255));

        // Fila 2: Número de Ventas, Pagos Recibidos, (vacío)
        JPanel panelNumeroVentas = crearItemResumen("Carrito N° de Ventas:", "0");
        lblNumeroVentas = (JLabel) panelNumeroVentas.getComponent(1);
        lblNumeroVentas.setForeground(new Color(76, 175, 80)); // Verde

        JPanel panelPagosRecibidos = crearItemResumen(" Pagos Recibidos:", "0");
        lblTotalPagosRecibidos = (JLabel) panelPagosRecibidos.getComponent(1);
        lblTotalPagosRecibidos.setForeground(new Color(156, 39, 176)); // Púrpura

        // Panel vacío para balancear el grid
        JPanel panelVacio = new JPanel();
        panelVacio.setOpaque(false);

        panel.add(panelMontoInicial);
        panel.add(panelTotalVentas);
        panel.add(panelMontoEsperado);
        panel.add(panelNumeroVentas);
        panel.add(panelPagosRecibidos);
        panel.add(panelVacio);

        return panel;
    }

    /**
     * Crea un item de resumen con etiqueta y valor.
     */
    private JPanel crearItemResumen(String etiqueta, String valor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEtiqueta.setForeground(Color.GRAY);
        lblEtiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblEtiqueta);
        panel.add(lblValor);

        return panel;
    }

    /**
     * Crea el panel de desglose de pagos por tipo.
     * Muestra una tabla con: Tipo de Pago | # Pagos | Total
     */
    private JPanel crearPanelDesglosePagos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                "Resumen Desglose por Método de Pago",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));

        // Crear modelo de tabla no editable
        String[] columnas = { "Método de Pago", "# Pagos", "Total" };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaDesglosePagos = new JTable(modelo);
        tablaDesglosePagos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaDesglosePagos.setRowHeight(28);
        tablaDesglosePagos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaDesglosePagos.getTableHeader().setReorderingAllowed(false);

        // Configurar ancho de columnas
        tablaDesglosePagos.getColumnModel().getColumn(0).setPreferredWidth(200);
        tablaDesglosePagos.getColumnModel().getColumn(1).setPreferredWidth(80);
        tablaDesglosePagos.getColumnModel().getColumn(2).setPreferredWidth(120);

        // Alinear columna # Pagos al centro y Total a la derecha
        DefaultTableCellRenderer centroRenderer = new DefaultTableCellRenderer();
        centroRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tablaDesglosePagos.getColumnModel().getColumn(1).setCellRenderer(centroRenderer);

        DefaultTableCellRenderer derechaRenderer = new DefaultTableCellRenderer();
        derechaRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tablaDesglosePagos.getColumnModel().getColumn(2).setCellRenderer(derechaRenderer);

        // Scroll pane con altura fija
        JScrollPane scrollPane = new JScrollPane(tablaDesglosePagos);
        scrollPane.setPreferredSize(new Dimension(400, 120));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Crea el panel de egresos del turno (gastos operativos + compras externas).
     */
    private JPanel crearPanelEgresos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                " Egresos del Turno",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 14)));

        // Grid de 3 columnas para mostrar gastos, compras y total
        JPanel gridPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Gastos Operativos
        JPanel panelGastos = new JPanel();
        panelGastos.setLayout(new BoxLayout(panelGastos, BoxLayout.Y_AXIS));
        panelGastos.setOpaque(false);

        JLabel lblEtiquetaGastos = new JLabel("Recibo Gastos Operativos:");
        lblEtiquetaGastos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEtiquetaGastos.setForeground(Color.GRAY);
        lblEtiquetaGastos.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblGastosOperativos = new JLabel("$0.00");
        lblGastosOperativos.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblGastosOperativos.setForeground(new Color(220, 53, 69)); // Rojo para gastos
        lblGastosOperativos.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelGastos.add(lblEtiquetaGastos);
        panelGastos.add(lblGastosOperativos);

        // Compras Externas
        JPanel panelCompras = new JPanel();
        panelCompras.setLayout(new BoxLayout(panelCompras, BoxLayout.Y_AXIS));
        panelCompras.setOpaque(false);

        JLabel lblEtiquetaCompras = new JLabel("Carrito Compras Externas:");
        lblEtiquetaCompras.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEtiquetaCompras.setForeground(Color.GRAY);
        lblEtiquetaCompras.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblComprasExternas = new JLabel("$0.00");
        lblComprasExternas.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblComprasExternas.setForeground(new Color(220, 53, 69)); // Rojo para compras
        lblComprasExternas.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelCompras.add(lblEtiquetaCompras);
        panelCompras.add(lblComprasExternas);

        // Total Egresos
        JPanel panelTotalEgresos = new JPanel();
        panelTotalEgresos.setLayout(new BoxLayout(panelTotalEgresos, BoxLayout.Y_AXIS));
        panelTotalEgresos.setOpaque(false);

        JLabel lblEtiquetaTotal = new JLabel(" Total Egresos:");
        lblEtiquetaTotal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEtiquetaTotal.setForeground(Color.GRAY);
        lblEtiquetaTotal.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblTotalEgresos = new JLabel("$0.00");
        lblTotalEgresos.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotalEgresos.setForeground(new Color(200, 30, 50)); // Rojo oscuro para total
        lblTotalEgresos.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelTotalEgresos.add(lblEtiquetaTotal);
        panelTotalEgresos.add(lblTotalEgresos);

        gridPanel.add(panelGastos);
        gridPanel.add(panelCompras);
        gridPanel.add(panelTotalEgresos);

        panel.add(gridPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Crea el panel del formulario de cierre.
     */
    private JPanel crearPanelFormulario() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        // ===== MONTO FINAL CONTADO =====
        JLabel lblMontoFinal = new JLabel("Efectivo Monto Final Contado:");
        lblMontoFinal.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtMontoFinal = new JTextField(15);
        txtMontoFinal.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        txtMontoFinal.setText("0");

        // Validación y cálculo automático de diferencia
        txtMontoFinal.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c) && c != '.' && c != '\b') {
                    evt.consume();
                }
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                calcularDiferencia();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(lblMontoFinal, gbc);

        gbc.gridy = 1;
        panel.add(txtMontoFinal, gbc);

        // ===== DIFERENCIA =====
        JLabel lblEtiquetaDiferencia = new JLabel(" Diferencia:");
        lblEtiquetaDiferencia.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblDiferencia = new JLabel("$0.00");
        lblDiferencia.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblDiferencia.setForeground(Color.GRAY);

        gbc.gridy = 2;
        panel.add(lblEtiquetaDiferencia, gbc);

        gbc.gridy = 3;
        panel.add(lblDiferencia, gbc);

        // ===== OBSERVACIONES =====
        JLabel lblObservaciones = new JLabel("Nota Observaciones:");
        lblObservaciones.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtObservaciones = new JTextArea(4, 30);
        txtObservaciones.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        JScrollPane scrollObservaciones = new JScrollPane(txtObservaciones);

        gbc.gridy = 4;
        panel.add(lblObservaciones, gbc);

        gbc.gridy = 5;
        panel.add(scrollObservaciones, gbc);

        return panel;
    }

    /**
     * Crea el panel de botones de acción.
     */
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(120, 40));
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancelar.addActionListener(e -> dispose());

        btnCerrar = new JButton("Cerrar Caja");
        btnCerrar.setPreferredSize(new Dimension(140, 40));
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCerrar.setBackground(new Color(255, 59, 48));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.addActionListener(e -> cerrarCaja());

        panel.add(btnCancelar);
        panel.add(btnCerrar);

        return panel;
    }

    // ==================== CONFIGURACIÓN DE ESTILOS ====================
    private void configurarEstilos() {
        txtMontoFinal.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "Ingrese el monto final contado");
        txtMontoFinal.putClientProperty(FlatClientProperties.STYLE, "arc:10");

        txtObservaciones.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "Ej: Cierre turno mañana - Sin novedades");
        // Note: 'arc' style is not supported for JTextArea in FlatLaf

        btnCancelar.setIcon(svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE));
        btnCancelar.setIconTextGap(8);
        btnCancelar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnCancelar.putClientProperty(FlatClientProperties.STYLE,
                dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

        btnCerrar.setIcon(svgIcon("raven/icon/svg/dashboard/alert-circle.svg", 18, Color.WHITE));
        btnCerrar.setIconTextGap(8);
        btnCerrar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnCerrar.putClientProperty(FlatClientProperties.STYLE,
                dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));
    }

    // ==================== CARGA DE DATOS ====================
    /**
     * Carga los datos del movimiento en el formulario.
     */
    private void cargarDatosMovimiento() {
        // Información del movimiento
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String infoMovimiento = String.format(
                "Movimiento #%d - %s - Apertura: %s",
                movimiento.getIdMovimiento(),
                movimiento.getNombreCaja(),
                movimiento.getFechaApertura().format(formatter));
        lblInfoMovimiento.setText(infoMovimiento);

        // Resumen financiero básico
        BigDecimal montoInicial = BigDecimal.valueOf(movimiento.getMontoInicial());
        BigDecimal totalVentas = movimiento.getTotalVentas();

        // Inicialmente el monto esperado es el del movimiento (sin egresos)
        // Se corregirá después de cargar el resumen detallado
        montoEsperadoCorregido = movimiento.getMontoEsperado();

        lblMontoInicial.setText(formatearMoneda(montoInicial));
        lblTotalVentas.setText(formatearMoneda(totalVentas));
        lblMontoEsperado.setText(formatearMoneda(montoEsperadoCorregido));

        // ========================================
        // NUEVO: Cargar resumen detallado de cierre
        // ========================================
        cargarResumenDetallado();

        // Pre-cargar monto esperado CORREGIDO como sugerencia
        txtMontoFinal.setText(montoEsperadoCorregido.toString());

        // Foco inicial
        SwingUtilities.invokeLater(() -> {
            txtMontoFinal.requestFocus();
            txtMontoFinal.selectAll();
        });

        calcularDiferencia();
    }

    /**
     * Carga el resumen detallado de ventas y pagos del movimiento.
     */
    private void cargarResumenDetallado() {
        try {
            // Obtener resumen completo del servicio (incluye gastos y compras)
            resumenCierre = serviceCaja.obtenerResumenCompletoConMovimientos(
                    movimiento.getIdMovimiento());

            // Actualizar labels de conteo
            lblNumeroVentas.setText(String.valueOf(resumenCierre.getTotalVentas()));

            int totalPagos = resumenCierre.getTotalPagosRecibidos();
            if (totalPagos == 0) {
                totalPagos = resumenCierre.getTotalVentas(); // Fallback
            }
            lblTotalPagosRecibidos.setText(String.valueOf(totalPagos));

            // Poblar tabla de desglose
            poblarTablaDesglose();

            // ========================================
            // NUEVO: Actualizar panel de egresos
            // ========================================
            actualizarPanelEgresos();

        } catch (SQLException e) {
            System.err.println("Error cargando resumen detallado: " + e.getMessage());
            // Valores por defecto si falla
            lblNumeroVentas.setText("--");
            lblTotalPagosRecibidos.setText("--");
            lblGastosOperativos.setText("--");
            lblComprasExternas.setText("--");
            lblTotalEgresos.setText("--");
        }
    }

    /**
     * Actualiza el panel de egresos con los datos del resumen.
     * También recalcula el monto esperado restando los egresos.
     */
    private void actualizarPanelEgresos() {
        if (resumenCierre == null) {
            return;
        }

        // Formatear y mostrar gastos operativos
        BigDecimal montoGastos = resumenCierre.getMontoTotalGastos() != null
                ? resumenCierre.getMontoTotalGastos()
                : BigDecimal.ZERO;
        lblGastosOperativos.setText(String.format("%d → %s",
                resumenCierre.getTotalGastos(),
                formatearMoneda(montoGastos)));

        // Formatear y mostrar compras externas
        BigDecimal montoCompras = resumenCierre.getMontoTotalCompras() != null
                ? resumenCierre.getMontoTotalCompras()
                : BigDecimal.ZERO;
        lblComprasExternas.setText(String.format("%d → %s",
                resumenCierre.getTotalCompras(),
                formatearMoneda(montoCompras)));

        // Calcular y mostrar total de egresos
        BigDecimal totalEgresos = montoGastos.add(montoCompras);
        lblTotalEgresos.setText(formatearMoneda(totalEgresos));

        // ========================================
        // RECALCULAR MONTO ESPERADO CON EGRESOS
        // ========================================
        // Monto Esperado Real = Monto Base - Egresos
        BigDecimal montoBase = movimiento.getMontoEsperado();
        montoEsperadoCorregido = montoBase.subtract(totalEgresos);

        // Actualizar UI con el monto corregido
        lblMontoEsperado.setText(formatearMoneda(montoEsperadoCorregido));
        txtMontoFinal.setText(montoEsperadoCorregido.toString());

        // Recalcular diferencia con el nuevo valor
        calcularDiferencia();

        System.out.println("SUCCESS  Monto Esperado Corregido: " + montoEsperadoCorregido);
        System.out.println("   Base: " + montoBase + " - Egresos: " + totalEgresos);
    }

    /**
     * Pobla la tabla de desglose con los datos del resumen.
     */
    private void poblarTablaDesglose() {
        if (resumenCierre == null || tablaDesglosePagos == null) {
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) tablaDesglosePagos.getModel();
        modelo.setRowCount(0); // Limpiar tabla

        Map<String, ResumenCierreCaja.DetallePago> detalles = resumenCierre.getDetallesPorTipo();

        if (detalles.isEmpty()) {
            // No hay ventas - mostrar mensaje
            modelo.addRow(new Object[] { "Sin ventas registradas", "-", "-" });
            return;
        }

        // Agregar cada tipo de pago a la tabla
        for (ResumenCierreCaja.DetallePago detalle : detalles.values()) {
            modelo.addRow(new Object[] {
                    detalle.getDescripcion(),
                    detalle.getCantidadPagos(),
                    formatearMoneda(detalle.getMontoTotal())
            });
        }

        // Agregar fila de totales
        modelo.addRow(new Object[] {
                " TOTAL",
                resumenCierre.getTotalVentas(),
                formatearMoneda(resumenCierre.getMontoTotalVentas())
        });
    }

    /**
     * Calcula y muestra la diferencia entre el monto final y el esperado.
     */
    private void calcularDiferencia() {
        try {
            String montoTexto = txtMontoFinal.getText().trim();
            if (montoTexto.isEmpty()) {
                lblDiferencia.setText("$0.00");
                lblDiferencia.setForeground(Color.GRAY);
                return;
            }

            BigDecimal montoFinal = new BigDecimal(montoTexto);
            BigDecimal diferencia = montoFinal.subtract(montoEsperadoCorregido);

            lblDiferencia.setText(formatearMoneda(diferencia));

            // Colorear según el resultado
            if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                lblDiferencia.setForeground(new Color(0, 153, 51)); // Verde (sobrante)
            } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                lblDiferencia.setForeground(new Color(255, 59, 48)); // Rojo (faltante)
            } else {
                lblDiferencia.setForeground(Color.GRAY); // Gris (cuadrado)
            }

        } catch (NumberFormatException e) {
            lblDiferencia.setText("Valor inválido");
            lblDiferencia.setForeground(Color.RED);
        }
    }

    // ==================== LÓGICA DE CIERRE ====================
    /**
     * Ejecuta el cierre de la caja.
     */
    private void cerrarCaja() {
        // Validar monto final
        String montoTexto = txtMontoFinal.getText().trim();
        if (montoTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe ingresar el monto final contado",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            txtMontoFinal.requestFocus();
            return;
        }

        BigDecimal montoFinal;
        try {
            montoFinal = new BigDecimal(montoTexto);

            if (montoFinal.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this,
                        "El monto final no puede ser negativo",
                        "Validación", JOptionPane.WARNING_MESSAGE);
                txtMontoFinal.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "El monto final debe ser un número válido",
                    "Validación", JOptionPane.ERROR_MESSAGE);
            txtMontoFinal.requestFocus();
            return;
        }

        // Calcular diferencia
        BigDecimal diferencia = montoFinal.subtract(movimiento.getMontoEsperado());

        // Mensaje de confirmación
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("¿Confirma el cierre de la caja?\n\n");
        mensaje.append(String.format("Monto esperado: %s\n",
                formatearMoneda(movimiento.getMontoEsperado())));
        mensaje.append(String.format("Monto final:    %s\n",
                formatearMoneda(montoFinal)));
        mensaje.append(String.format("Diferencia:     %s\n\n",
                formatearMoneda(diferencia)));

        if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
            String tipo = diferencia.compareTo(BigDecimal.ZERO) > 0 ? "SOBRANTE" : "FALTANTE";
            mensaje.append(String.format("WARNING  Hay un %s de %s", tipo, formatearMoneda(diferencia.abs())));
        }

        JButton btnSi = createDialogButton(
                "Sí, cerrar",
                svgIcon("raven/icon/svg/dashboard/alert-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(220, 53, 69), new Color(232, 74, 88), new Color(176, 42, 55)));
        JButton btnNo = createDialogButton(
                "No",
                svgIcon("raven/icon/svg/dashboard/x-circle.svg", 18, Color.WHITE),
                dialogButtonStyle(new Color(55, 65, 81), new Color(75, 85, 99), new Color(31, 41, 55)));

        int confirmacion = showConfirmYesNoDialog(
                this,
                mensaje.toString(),
                "Confirmar Cierre",
                JOptionPane.QUESTION_MESSAGE,
                btnSi,
                btnNo);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        // Deshabilitar botones durante el proceso
        btnCerrar.setEnabled(false);
        btnCancelar.setEnabled(false);
        LoadingOverlayHelper.showLoading(getRootPane(), "Cerrando caja...");

        // Ejecutar cierre en background
        // Ejecutar cierre en background
        SwingWorker<ModelCajaMovimiento, Void> worker = new SwingWorker<ModelCajaMovimiento, Void>() {
            @Override
            protected ModelCajaMovimiento doInBackground() throws Exception {
                return serviceCaja.cerrarCaja(
                        movimiento.getIdMovimiento(),
                        montoFinal,
                        txtObservaciones.getText().trim());
            }

            @Override
            protected void done() {
                LoadingOverlayHelper.hideLoading(getRootPane());
                try {
                    ModelCajaMovimiento movimientoCerrado = get();
                    cierreExitoso = true;

                    mostrarResumenCierre(movimientoCerrado);

                } catch (Exception e) {
                    btnCerrar.setEnabled(true);
                    btnCancelar.setEnabled(true);

                    JOptionPane.showMessageDialog(CierreCajaDialog.this,
                            "ERROR  Error al cerrar la caja:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * Muestra el resumen del cierre de caja.
     */
    private void mostrarResumenCierre(ModelCajaMovimiento movimiento) {
        StringBuilder resumen = new StringBuilder();
        resumen.append("SUCCESS  CAJA CERRADA EXITOSAMENTE\n\n");
        resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        resumen.append(String.format("Movimiento #%d\n", movimiento.getIdMovimiento()));
        resumen.append(String.format("Caja: %s\n", movimiento.getNombreCaja()));
        resumen.append(String.format("Usuario: %s\n\n", movimiento.getNombreUsuario()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        resumen.append(String.format("Apertura:   %s\n",
                movimiento.getFechaApertura().format(formatter)));
        resumen.append(String.format("Cierre:     %s\n",
                movimiento.getFechaCierre().format(formatter)));
        resumen.append(String.format("Duración:   %d minutos\n\n",
                movimiento.getDuracionEnMinutos()));

        resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        resumen.append("Resumen RESUMEN FINANCIERO\n");
        resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        resumen.append(String.format("Monto inicial:   %s\n",
                formatearMoneda(movimiento.getMontoInicial())));

        // Mostrar número de ventas y monto total de ventas usando resumenCierre
        if (resumenCierre != null) {
            resumen.append(String.format("N° de ventas:    %d\n",
                    resumenCierre.getTotalVentas()));
            resumen.append(String.format("Ingresos ventas: %s\n",
                    formatearMoneda(resumenCierre.getMontoTotalVentas())));
        } else {
            resumen.append(String.format("Ingresos ventas: %s\n",
                    formatearMoneda(movimiento.getTotalVentas())));
        }

        // Usar el monto esperado corregido (después de egresos)
        resumen.append(String.format("Monto esperado:  %s\n",
                formatearMoneda(montoEsperadoCorregido)));
        resumen.append(String.format("Monto final:     %s\n",
                formatearMoneda(movimiento.getMontoFinal())));

        // Calcular diferencia usando el monto corregido (convertir double a BigDecimal)
        BigDecimal montoFinalBD = BigDecimal.valueOf(movimiento.getMontoFinal());
        BigDecimal diferenciaReal = montoFinalBD.subtract(montoEsperadoCorregido);
        resumen.append(String.format("Diferencia:      %s\n\n",
                formatearMoneda(diferenciaReal)));

        if (resumenCierre != null && !resumenCierre.getDetallesPorTipo().isEmpty()) {
            resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            resumen.append(" DESGLOSE POR MÉTODO DE PAGO\n");
            resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            resumen.append(String.format("N° de Ventas:    %d\n",
                    resumenCierre.getTotalVentas()));
            resumen.append(String.format("Pagos Recibidos: %d\n\n",
                    resumenCierre.getTotalPagosRecibidos() > 0
                            ? resumenCierre.getTotalPagosRecibidos()
                            : resumenCierre.getTotalVentas()));

            for (ResumenCierreCaja.DetallePago detalle : resumenCierre.getDetallesPorTipo().values()) {
                resumen.append(String.format("  %s: %d pagos → %s\n",
                        detalle.getDescripcion(),
                        detalle.getCantidadPagos(),
                        formatearMoneda(detalle.getMontoTotal())));
            }
        }

        // ========================================
        // NUEVO: Agregar resumen de productos vendidos
        // ========================================
        if (resumenCierre != null && !resumenCierre.getProductosVendidos().isEmpty()) {
            resumen.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            resumen.append(" RESUMEN DE PRODUCTOS VENDIDOS\n");
            resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            for (ResumenCierreCaja.DetalleProducto prod : resumenCierre.getProductosVendidos()) {
                resumen.append(String.format("  %d %s %s → %s\n",
                        prod.getCantidad(),
                        prod.getUnidad(),
                        prod.getNombreProducto(),
                        formatearMoneda(prod.getMontoTotal())));
            }
        }

        // ========================================
        // NUEVO: Agregar gastos operativos y compras externas
        // ========================================
        if (resumenCierre != null) {
            resumen.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            resumen.append(" EGRESOS DEL TURNO\n");
            resumen.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

            // Gastos operativos
            resumen.append(String.format("Recibo Gastos Operativos:  %d → %s\n",
                    resumenCierre.getTotalGastos(),
                    formatearMoneda(resumenCierre.getMontoTotalGastos())));

            // Compras externas
            resumen.append(String.format("Carrito Compras Externas:   %d → %s\n",
                    resumenCierre.getTotalCompras(),
                    formatearMoneda(resumenCierre.getMontoTotalCompras())));

            // Total egresos
            BigDecimal totalEgresos = resumenCierre.getMontoTotalGastos()
                    .add(resumenCierre.getMontoTotalCompras());
            resumen.append(String.format(" Total Egresos:      %s\n",
                    formatearMoneda(totalEgresos)));
        }

        JOptionPane.showMessageDialog(this,
                resumen.toString(),
                "Cierre de Caja Completado",
                JOptionPane.INFORMATION_MESSAGE);

        // Imprimir tirilla de cierre de caja
        try {
            new ImpresionCierreCaja("POS-80").imprimirCierreCaja(movimiento, resumenCierre);
        } catch (Exception e) {
            System.err.println("Error al imprimir tirilla de cierre: " + e.getMessage());
        }

        dispose();
    }

    // ==================== UTILIDADES ====================
    /**
     * Formatea un BigDecimal como moneda colombiana.
     */
    private String formatearMoneda(BigDecimal monto) {
        return String.format("$%,.2f", monto.doubleValue());
    }

    private String formatearMoneda(double monto) {
        return String.format("$%,.2f", monto);
    }

    // ==================== GETTERS ====================
    public boolean isCierreExitoso() {
        return cierreExitoso;
    }
}
