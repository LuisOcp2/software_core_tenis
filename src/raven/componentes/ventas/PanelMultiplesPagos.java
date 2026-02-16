package raven.componentes.ventas;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import raven.controlador.principal.ModelMedioPago;
import raven.controlador.principal.ModelMedioPago.TipoMedioPago;
import raven.clases.comercial.ServiceNotaCredito;
import raven.controlador.comercial.ModelNotaCredito;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Panel para gestionar múltiples medios de pago en una venta.
 * 
 * PATRÓN APLICADO: Component Pattern (Swing)
 * PRINCIPIO: Single Responsibility - Solo maneja la UI de pagos
 * 
 * VALIDACIONES IMPLEMENTADAS:
 * - Validación de Nota de Crédito con saldo disponible
 * - Verificación de estado EMITIDA o APLICADA (parcialmente)
 * - Integración con ServiceNotaCredito
 * 
 * @author lmog2
 */
public class PanelMultiplesPagos extends JPanel {

    // ==================== CONSTANTES ====================
    private static final String STYLE_PANEL = "arc:15;background:$Login.background";
    private static final String STYLE_BUTTON = "arc:10";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    // ==================== COMPONENTES UI ====================
    private JComboBox<TipoMedioPago> cbxTipoPago;
    private JTextField txtMonto;
    private JTextField txtReferencia;
    private JTextArea txtObservaciones;
    private JButton btnAgregar;
    private JButton btnEliminar;
    private JTable tablaPagos;
    private DefaultTableModel modeloTabla;

    private JLabel lblTotalVenta;
    private JLabel lblTotalPagado;
    private JLabel lblSaldoPendiente;

    // ==================== COMPONENTES ESPECÍFICOS PARA NOTA CRÉDITO
    // ====================
    private JPanel panelNotaCredito;
    private JTextField txtNumeroNotaCredito;
    private JButton btnBuscarNotaCredito;
    private JLabel lblSaldoNotaCredito;
    private JLabel lblEstadoNotaCredito;

    // ==================== COMPONENTES PARA DEVUELTAS (EFECTIVO)
    // ====================
    private JPanel panelDevueltas;
    private JTextField txtClienteEntrega;
    private JLabel lblDevueltas;
    private JButton btnDynamic1; // Antes 50k
    private JButton btnDynamic2; // Antes 20k
    private JButton btnDynamic3; // Antes 10k
    private JButton btnExacto;
    private JButton btnTodoEfectivo;
    private JButton btnLimpiarPagos;

    // Valores actuales de los botones dinámicos
    private BigDecimal[] dynamicValues = new BigDecimal[3];

    // ==================== PANEL RESUMEN MEJORADO ====================
    private JLabel lblVuelto;
    private JPanel panelVuelto;

    // ==================== DATOS ====================
    private BigDecimal totalVenta;
    private List<ModelMedioPago> mediosPago;
    private ServiceNotaCredito serviceNotaCredito;
    private ModelNotaCredito notaCreditoSeleccionada;

    // Listener para notificar cambios en el saldo
    private java.util.function.Consumer<BigDecimal> onSaldoChangeListener;

    public void setOnSaldoChangeListener(java.util.function.Consumer<BigDecimal> listener) {
        this.onSaldoChangeListener = listener;
    }

    // ==================== CONSTRUCTOR ====================
    public PanelMultiplesPagos() {
        this.mediosPago = new ArrayList<>();
        this.totalVenta = BigDecimal.ZERO;
        this.serviceNotaCredito = new ServiceNotaCredito();

        // Inicializar valores dinámicos por defecto
        dynamicValues[0] = new BigDecimal(50000);
        dynamicValues[1] = new BigDecimal(20000);
        dynamicValues[2] = new BigDecimal(10000);

        initComponents();
        configurarEstilos();
        configurarEventos();
    }

    // ==================== INICIALIZACIÓN ====================
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel superior: Agregar medios de pago
        add(crearPanelAgregar(), BorderLayout.NORTH);

        // Panel central: Tabla de pagos
        add(crearPanelTabla(), BorderLayout.CENTER);

        // Panel inferior: Resumen de totales
        add(crearPanelResumen(), BorderLayout.SOUTH);
    }

    /**
     * Crea el panel superior para agregar medios de pago.
     */
    private JPanel crearPanelAgregar() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createTitledBorder("Agregar Pago"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Fila 1: Tipo y Monto
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Medio de Pago:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        cbxTipoPago = new JComboBox<>(TipoMedioPago.values());
        panel.add(cbxTipoPago, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Monto:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        txtMonto = new JTextField();
        txtMonto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "$0.00");
        panel.add(txtMonto, gbc);

        // Fila 2: Referencia y Botón Agregar
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Referencia:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        txtReferencia = new JTextField();
        txtReferencia.setEnabled(false);
        panel.add(txtReferencia, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2; // Span 2
        btnAgregar = new JButton("Agregar Pago");
        btnAgregar.putClientProperty(FlatClientProperties.STYLE,
                "background:$Component.accentColor;foreground:#FFFFFF;font:bold");
        panel.add(btnAgregar, gbc);

        // Panel de Nota de Crédito (oculto por defecto)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(0, 0, 0, 0); // Reset insets

        panelNotaCredito = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelNotaCredito.setVisible(false);

        panelNotaCredito.add(new JLabel("N° Nota Crédito:"));
        txtNumeroNotaCredito = new JTextField(10);
        panelNotaCredito.add(txtNumeroNotaCredito);

        btnBuscarNotaCredito = new JButton("Buscar");
        btnBuscarNotaCredito.setPreferredSize(new Dimension(80, 25));
        panelNotaCredito.add(btnBuscarNotaCredito);

        panelNotaCredito.add(new JLabel("Saldo:"));
        lblSaldoNotaCredito = new JLabel("$0");
        panelNotaCredito.add(lblSaldoNotaCredito);

        panelNotaCredito.add(new JLabel("Estado:"));
        lblEstadoNotaCredito = new JLabel("---");
        panelNotaCredito.add(lblEstadoNotaCredito);

        panel.add(panelNotaCredito, gbc);

        // Campo observaciones no visible pero requerido por lógica
        txtObservaciones = new JTextArea();

        return panel;
    }

    /**
     * Crea el panel central con la tabla de pagos.
     */
    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createTitledBorder("Pagos Agregados"));

        String[] columnas = { "Tipo", "Referencia", "Monto" };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaPagos = new JTable(modeloTabla);
        tablaPagos.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                if (value instanceof BigDecimal) {
                    value = CURRENCY_FORMAT.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        JScrollPane scroll = new JScrollPane(tablaPagos);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnEliminar = new JButton("Eliminar Seleccionado");
        btnEliminar.setEnabled(false);

        btnLimpiarPagos = new JButton("Limpiar Todo");

        panelBotones.add(btnLimpiarPagos);
        panelBotones.add(btnEliminar);
        panel.add(panelBotones, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea el panel inferior con el resumen de totales.
     */
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        // Panel de Devueltas (Se agrega aquí)
        panelDevueltas = crearPanelDevueltas();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 5; // Ocupa altura (ahora 5 por el panel de vuelto)
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(panelDevueltas, gbc);

        // Totales (Columna derecha)
        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.EAST;

        // Espaciador
        gbc.gridy = 0;
        panel.add(Box.createVerticalStrut(10), gbc);

        // Total Venta
        gbc.gridy = 1;
        JPanel pTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pTotal.setOpaque(false);
        pTotal.add(new JLabel("Total Venta:"));
        lblTotalVenta = new JLabel("$0");
        lblTotalVenta.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        pTotal.add(lblTotalVenta);
        panel.add(pTotal, gbc);

        // Total Pagado
        gbc.gridy = 2;
        JPanel pPagado = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pPagado.setOpaque(false);
        pPagado.add(new JLabel("Total Pagado:"));
        lblTotalPagado = new JLabel("$0");
        lblTotalPagado.setForeground(new Color(0, 153, 51));
        pPagado.add(lblTotalPagado);
        panel.add(pPagado, gbc);

        // Saldo Pendiente
        gbc.gridy = 3;
        JPanel pSaldo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pSaldo.setOpaque(false);
        pSaldo.add(new JLabel("Saldo Pendiente:"));
        lblSaldoPendiente = new JLabel("$0");
        lblSaldoPendiente.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        lblSaldoPendiente.setForeground(new Color(204, 0, 0));
        pSaldo.add(lblSaldoPendiente);
        panel.add(pSaldo, gbc);

        // Vuelto (Cambio)
        gbc.gridy = 4;
        panelVuelto = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelVuelto.setOpaque(false);
        panelVuelto.setVisible(false); // Oculto por defecto

        JLabel lblVueltoTitle = new JLabel("Vuelto:");
        lblVueltoTitle.setFont(lblVueltoTitle.getFont().deriveFont(Font.BOLD));
        lblVueltoTitle.setForeground(new Color(0, 102, 204));
        panelVuelto.add(lblVueltoTitle);

        lblVuelto = new JLabel("$0");
        lblVuelto.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        lblVuelto.setForeground(new Color(0, 102, 204));
        panelVuelto.add(lblVuelto);

        panel.add(panelVuelto, gbc);

        return panel;
    }

    /**
     * Crea el panel de cálculo de devueltas para pagos en efectivo.
     */
    private JPanel crearPanelDevueltas() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Cálculo de Devueltas"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:darken($Login.background,3%)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: Cliente entrega
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Cliente Entrega:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 4;
        txtClienteEntrega = new JTextField(12);
        txtClienteEntrega.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        txtClienteEntrega.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "$0");
        txtClienteEntrega.setFont(txtClienteEntrega.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(txtClienteEntrega, gbc);

        // Fila 2: Botones de denominaciones (Dinámicos)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        btnDynamic1 = new JButton("$50k");
        btnDynamic1.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panel.add(btnDynamic1, gbc);

        gbc.gridx = 1;
        btnDynamic2 = new JButton("$20k");
        btnDynamic2.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panel.add(btnDynamic2, gbc);

        gbc.gridx = 2;
        btnDynamic3 = new JButton("$10k");
        btnDynamic3.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panel.add(btnDynamic3, gbc);

        gbc.gridx = 3;
        btnExacto = new JButton("Exacto");
        btnExacto.putClientProperty(FlatClientProperties.STYLE, "arc:8;background:$Component.accentColor");
        btnExacto.setToolTipText("Usar monto exacto del saldo");
        panel.add(btnExacto, gbc);

        gbc.gridx = 4;
        btnTodoEfectivo = new JButton("Todo Efectivo");
        btnTodoEfectivo.putClientProperty(FlatClientProperties.STYLE, "arc:8;background:#28a745;foreground:#ffffff");
        btnTodoEfectivo.setToolTipText("Pagar todo el saldo pendiente con efectivo");
        panel.add(btnTodoEfectivo, gbc);

        // Fila 3: Resultado de devueltas
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel lblDevueltasTitulo = new JLabel("Devueltas:");
        lblDevueltasTitulo.setFont(lblDevueltasTitulo.getFont().deriveFont(Font.BOLD));
        panel.add(lblDevueltasTitulo, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        lblDevueltas = new JLabel("$0");
        lblDevueltas.setFont(lblDevueltas.getFont().deriveFont(Font.BOLD, 18f));
        lblDevueltas.setForeground(new Color(0, 102, 204));
        panel.add(lblDevueltas, gbc);

        return panel;
    }

    // ==================== CONFIGURACIÓN ====================
    private void configurarEstilos() {
        putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        btnAgregar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON);
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON);
    }

    private void configurarEventos() {
        // Evento: Cambio de tipo de pago
        cbxTipoPago.addActionListener(e -> onTipoPagoChanged());

        // Evento: Buscar Nota de Crédito
        btnBuscarNotaCredito.addActionListener(e -> buscarNotaCredito());

        // Evento: Enter en campo de número de NC
        txtNumeroNotaCredito.addActionListener(e -> buscarNotaCredito());

        // Evento: Agregar pago
        btnAgregar.addActionListener(e -> agregarMedioPago());

        // Evento: Eliminar pago
        btnEliminar.addActionListener(e -> eliminarMedioPago());

        // Evento: Selección en tabla
        tablaPagos.getSelectionModel().addListSelectionListener(e -> {
            btnEliminar.setEnabled(tablaPagos.getSelectedRow() != -1);
        });

        // ==================== EVENTOS PARA DEVUELTAS ====================

        // Calcular devueltas cuando cambia el monto que entrega el cliente
        txtClienteEntrega.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                calcularDevueltas();
            }
        });

        // Botones de denominaciones dinámicas
        btnDynamic1.addActionListener(e -> agregarMontoDinamico(0));
        btnDynamic2.addActionListener(e -> agregarMontoDinamico(1));
        btnDynamic3.addActionListener(e -> agregarMontoDinamico(2));

        // Botón Exacto
        btnExacto.addActionListener(e -> {
            BigDecimal saldo = calcularSaldoPendiente();
            if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                txtClienteEntrega.setText(saldo.toString());
                txtMonto.setText(saldo.toString());
                calcularDevueltas();
            }
        });

        // Botón Todo Efectivo
        btnTodoEfectivo.addActionListener(e -> agregarTodoEfectivo());

        // Botón Limpiar Pagos
        btnLimpiarPagos.addActionListener(e -> limpiarTodosPagos());

        // Enter en campo monto para agregar rápido
        txtMonto.addActionListener(e -> agregarMedioPago());
    }

    /**
     * Maneja el cambio de tipo de pago.
     * 
     * RESPONSABILIDAD:
     * - Mostrar/ocultar panel de Nota de Crédito
     * - Mostrar/ocultar panel de Devueltas para efectivo
     * - Habilitar/deshabilitar campo de referencia según tipo
     * - Resetear estado de validación
     */
    private void onTipoPagoChanged() {
        TipoMedioPago tipo = (TipoMedioPago) cbxTipoPago.getSelectedItem();

        if (tipo == null)
            return;

        // Mostrar panel de Nota de Crédito solo si se selecciona ese tipo
        boolean esNotaCredito = tipo == TipoMedioPago.NOTA_CREDITO;
        panelNotaCredito.setVisible(esNotaCredito);

        // Mostrar panel de devueltas solo para efectivo
        boolean esEfectivo = tipo == TipoMedioPago.EFECTIVO;
        panelDevueltas.setVisible(esEfectivo);

        // Habilitar referencia para tipos que lo requieren
        txtReferencia.setEnabled(ModelMedioPago.TipoMedioPago.requiereReferencia(tipo));

        // Resetear nota de crédito si se cambia de tipo
        if (!esNotaCredito && tipo != TipoMedioPago.CREDITO) {
            resetearNotaCredito();
        }

        // Resetear panel de devueltas si no es efectivo
        if (!esEfectivo) {
            txtClienteEntrega.setText("");
            lblDevueltas.setText("$0");
            txtMonto.setEnabled(true);
        } else {
            txtMonto.setEnabled(false);
        }

        // Revalidar y repintar para ajustar el layout
        revalidate();
        repaint();
    }

    // ==================== MÉTODOS PARA DEVUELTAS ====================

    /**
     * Usa el monto dinámico del botón seleccionado.
     * 
     * @param index Índice del botón (0-2)
     */
    private void agregarMontoDinamico(int index) {
        if (index >= 0 && index < dynamicValues.length) {
            BigDecimal monto = dynamicValues[index];
            txtClienteEntrega.setText(String.valueOf(monto.longValue()));
            txtMonto.setText(String.valueOf(monto.longValue()));
            calcularDevueltas();
        }
    }

    /**
     * Calcula y actualiza los botones dinámicos basado en el saldo pendiente.
     */
    private void actualizarBotonesDinamo() {
        BigDecimal saldo = calcularSaldoPendiente();

        // Si no hay saldo (o es negativo/cero), usar defaults
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            setBotonesDefault();
            return;
        }

        long saldoVal = saldo.longValue();
        Set<Long> candidatos = new TreeSet<>();

        // 1. Siguiente múltiplo de 10k/20k (cercano)
        candidatos.add(roundUpToMultiple(saldoVal, 10000));
        candidatos.add(roundUpToMultiple(saldoVal, 20000));

        // 2. Siguiente múltiplo de 50k
        candidatos.add(roundUpToMultiple(saldoVal, 50000));

        // 3. Siguiente múltiplo de 100k
        candidatos.add(roundUpToMultiple(saldoVal, 100000));

        // 4. Saldo exacto + 50k (común: "tenga 50 mil mas para el cambio")
        candidatos.add(saldoVal + 50000);

        // Filtrar solo valores mayores o iguales al saldo
        List<Long> finales = new ArrayList<>();
        for (Long val : candidatos) {
            if (val >= saldoVal) {
                finales.add(val);
            }
        }

        // Ordenar
        Collections.sort(finales);

        // Asegurar tener 3 opciones distintas
        // Opción 1: Lo más cercano (pero >= saldo)
        // Opción 2: Un poco más
        // Opción 3: Billete grande

        long op1, op2, op3;

        if (!finales.isEmpty()) {
            op1 = finales.get(0);
            // Buscar siguiente distinto
            op2 = findNextDistinct(finales, op1);
            if (op2 == -1)
                op2 = op1 + 10000; // Fallback

            op3 = findNextDistinct(finales, op2);
            if (op3 == -1)
                op3 = op2 + 20000; // Fallback
        } else {
            // Fallback total
            op1 = roundUpToMultiple(saldoVal, 50000);
            op2 = op1 + 20000;
            op3 = op2 + 50000;
        }

        // Asignar a botones
        actualizarBoton(btnDynamic1, 0, new BigDecimal(op1));
        actualizarBoton(btnDynamic2, 1, new BigDecimal(op2));
        actualizarBoton(btnDynamic3, 2, new BigDecimal(op3));
    }

    private long roundUpToMultiple(long value, long multiple) {
        if (multiple == 0)
            return value;
        long remainder = value % multiple;
        if (remainder == 0)
            return value;
        return value + multiple - remainder;
    }

    private long findNextDistinct(List<Long> values, long current) {
        for (Long val : values) {
            if (val > current)
                return val;
        }
        return -1;
    }

    private void setBotonesDefault() {
        actualizarBoton(btnDynamic1, 0, new BigDecimal(50000));
        actualizarBoton(btnDynamic2, 1, new BigDecimal(20000));
        actualizarBoton(btnDynamic3, 2, new BigDecimal(10000));
    }

    private void actualizarBoton(JButton btn, int index, BigDecimal valor) {
        dynamicValues[index] = valor;
        // Formato amigable: 50000 -> $50k
        double val = valor.doubleValue();
        String text;
        if (val >= 1000000) {
            text = String.format("$%.1fm", val / 1000000);
        } else if (val >= 1000) {
            text = String.format("$%.0fk", val / 1000);
        } else {
            text = String.format("$%.0f", val);
        }
        btn.setText(text);
        btn.setToolTipText("Usar monto: " + CURRENCY_FORMAT.format(valor));
    }

    /**
     * Calcula las devueltas basado en el monto que entrega el cliente.
     */
    private void calcularDevueltas() {
        try {
            String textoCliente = txtClienteEntrega.getText().trim().replaceAll("[^0-9.]", "");
            if (textoCliente.isEmpty()) {
                lblDevueltas.setText("$0");
                lblDevueltas.setForeground(new Color(0, 102, 204));
                return;
            }

            BigDecimal montoCliente = new BigDecimal(textoCliente);
            BigDecimal saldoPendiente = calcularSaldoPendiente();

            // Lógica para actualizar txtMonto
            // El monto del pago será lo que entrega el cliente, capado al saldo pendiente
            // (Si entrega menos, paga parcial. Si entrega más, paga el total pendiente)
            BigDecimal montoPago = montoCliente.min(saldoPendiente);
            txtMonto.setText(montoPago.toString());

            BigDecimal devueltas = montoCliente.subtract(saldoPendiente);

            if (devueltas.compareTo(BigDecimal.ZERO) >= 0) {
                lblDevueltas.setText(CURRENCY_FORMAT.format(devueltas));
                lblDevueltas.setForeground(new Color(0, 153, 51)); // Verde
            } else {
                lblDevueltas.setText("Falta: " + CURRENCY_FORMAT.format(devueltas.abs()));
                lblDevueltas.setForeground(new Color(255, 69, 58)); // Rojo
            }
        } catch (NumberFormatException ex) {
            lblDevueltas.setText("$0");
            lblDevueltas.setForeground(new Color(0, 102, 204));
            // Si hay error de formato, limpiar monto si es efectivo (ya que está
            // deshabilitado)
            TipoMedioPago tipo = (TipoMedioPago) cbxTipoPago.getSelectedItem();
            if (tipo == TipoMedioPago.EFECTIVO) {
                txtMonto.setText("");
            }
        }
    }

    /**
     * Agrega todo el saldo pendiente como efectivo.
     */
    private void agregarTodoEfectivo() {
        BigDecimal saldo = calcularSaldoPendiente();
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay saldo pendiente para agregar",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Seleccionar efectivo
        cbxTipoPago.setSelectedItem(TipoMedioPago.EFECTIVO);

        // Establecer monto
        txtMonto.setText(saldo.toString());
        txtClienteEntrega.setText(saldo.toString());

        // Agregar el pago
        agregarMedioPago();
    }

    /**
     * Elimina todos los pagos agregados.
     */
    private void limpiarTodosPagos() {
        if (mediosPago.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay pagos para eliminar",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar todos los pagos agregados?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            mediosPago.clear();
            modeloTabla.setRowCount(0);
            actualizarResumen();
            limpiarCampos();
        }
    }

    /**
     * Busca y valida una Nota de Crédito.
     */
    private void buscarNotaCredito() {
        String numeroNC = txtNumeroNotaCredito.getText().trim().toUpperCase();

        // Validar que se ingresó un número
        if (numeroNC.isEmpty()) {
            mostrarError("Por favor ingrese el número de la nota de crédito");
            txtNumeroNotaCredito.requestFocus();
            return;
        }

        try {
            // Buscar nota de crédito en la base de datos
            ModelNotaCredito nc = serviceNotaCredito.buscarNotaCreditoPorNumero(numeroNC);

            // VALIDACIÓN 1: Verificar que existe
            if (nc == null) {
                mostrarError(
                        "No se encontró ninguna nota de crédito activa con el número: " + numeroNC);
                resetearNotaCredito();
                return;
            }

            // VALIDACIÓN 2: Verificar estado (EMITIDA o APLICADA parcialmente)
            ModelNotaCredito.EstadoNota estado = nc.getEstado();
            if (estado != ModelNotaCredito.EstadoNota.EMITIDA
                    && estado != ModelNotaCredito.EstadoNota.APLICADA) {

                mostrarError(String.format(
                        "La nota de crédito está en estado: %s\n\n" +
                                "Solo se pueden aplicar notas en estado EMITIDA o APLICADA (parcialmente)",
                        estado.getDescripcion()));
                resetearNotaCredito();
                return;
            }

            // VALIDACIÓN 3: Verificar saldo disponible
            BigDecimal saldoDisponible = nc.getSaldoDisponible();
            if (saldoDisponible.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarError(String.format(
                        "La nota de crédito no tiene saldo disponible\n\n" +
                                "Saldo actual: %s\n" +
                                "Saldo usado: %s",
                        CURRENCY_FORMAT.format(saldoDisponible),
                        CURRENCY_FORMAT.format(nc.getSaldoUsado())));
                resetearNotaCredito();
                return;
            }

            // Nota de crédito válida - Almacenar y mostrar información
            notaCreditoSeleccionada = nc;

            // Actualizar UI
            lblEstadoNotaCredito.setText(estado.getDescripcion());
            lblEstadoNotaCredito.setForeground(
                    estado == ModelNotaCredito.EstadoNota.EMITIDA
                            ? new Color(0, 153, 51) // Verde
                            : new Color(255, 165, 0) // Naranja
            );

            lblSaldoNotaCredito.setText(CURRENCY_FORMAT.format(saldoDisponible));
            lblSaldoNotaCredito.setForeground(new Color(0, 153, 51)); // Verde

            // Calcular monto sugerido (el menor entre saldo NC y saldo pendiente venta)
            BigDecimal saldoPendiente = calcularSaldoPendiente();
            BigDecimal montoSugerido = saldoDisponible.min(saldoPendiente);
            txtMonto.setText(montoSugerido.toString());

            // Mostrar información al usuario
            JOptionPane.showMessageDialog(
                    this,
                    String.format(
                            "Nota de Crédito Válida\n\n" +
                                    "Número: %s\n" +
                                    "Cliente: %s (%s)\n" +
                                    "Total Original: %s\n" +
                                    "Saldo Usado: %s\n" +
                                    "Saldo Disponible: %s\n" +
                                    "Estado: %s\n\n" +
                                    "Monto sugerido: %s",
                            nc.getNumeroNotaCredito(),
                            nc.getClienteNombre(),
                            nc.getClienteDni(),
                            CURRENCY_FORMAT.format(nc.getTotal()),
                            CURRENCY_FORMAT.format(nc.getSaldoUsado()),
                            CURRENCY_FORMAT.format(saldoDisponible),
                            estado.getDescripcion(),
                            CURRENCY_FORMAT.format(montoSugerido)),
                    "Nota de Crédito Encontrada",
                    JOptionPane.INFORMATION_MESSAGE);

            // Enfocar campo de monto
            txtMonto.requestFocus();
            txtMonto.selectAll();

            System.out.println("SUCCESS NC validada: " + nc.getNumeroNotaCredito() +
                    " | Saldo: " + saldoDisponible);

        } catch (Exception ex) {
            mostrarError("Error al buscar la nota de crédito:\n" + ex.getMessage());
            ex.printStackTrace();
            resetearNotaCredito();
        }
    }

    /**
     * Resetea el estado de la Nota de Crédito.
     */
    private void resetearNotaCredito() {
        notaCreditoSeleccionada = null;
        lblSaldoNotaCredito.setText("$0");
        lblSaldoNotaCredito.setForeground(Color.GRAY);
        lblEstadoNotaCredito.setText("---");
        lblEstadoNotaCredito.setForeground(Color.GRAY);
        txtNumeroNotaCredito.setText("");
    }

    // ==================== LÓGICA DE NEGOCIO ====================
    /**
     * Agrega un nuevo medio de pago a la lista.
     */
    private void agregarMedioPago() {
        try {
            // Validar tipo
            TipoMedioPago tipo = (TipoMedioPago) cbxTipoPago.getSelectedItem();
            if (tipo == null) {
                mostrarError("Seleccione un tipo de pago");
                return;
            }

            // VALIDACIÓN ESPECÍFICA PARA NOTA DE CRÉDITO
            if (tipo == TipoMedioPago.NOTA_CREDITO) {
                if (!validarNotaCreditoParaAgregar()) {
                    return; // La validación ya mostró el error
                }
            }

            // Validar monto
            String montoTexto = txtMonto.getText().trim().replaceAll("[^0-9.]", "");
            if (montoTexto.isEmpty()) {
                mostrarError("Ingrese un monto");
                txtMonto.requestFocus();
                return;
            }

            BigDecimal monto = new BigDecimal(montoTexto);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarError("El monto debe ser mayor a $0");
                txtMonto.requestFocus();
                return;
            }

            // Validar que no exceda el saldo pendiente
            BigDecimal saldoPendiente = calcularSaldoPendiente();
            if (monto.compareTo(saldoPendiente) > 0) {
                mostrarError(String.format(
                        "El monto (%s) excede el saldo pendiente (%s)",
                        CURRENCY_FORMAT.format(monto),
                        CURRENCY_FORMAT.format(saldoPendiente)));
                return;
            }

            // VALIDACIÓN ADICIONAL: Monto no debe exceder saldo de NC
            if (tipo == TipoMedioPago.NOTA_CREDITO) {
                BigDecimal saldoNC = notaCreditoSeleccionada.getSaldoDisponible();
                if (monto.compareTo(saldoNC) > 0) {
                    mostrarError(String.format(
                            "El monto (%s) excede el saldo disponible de la NC (%s)",
                            CURRENCY_FORMAT.format(monto),
                            CURRENCY_FORMAT.format(saldoNC)));
                    return;
                }
            }

            // Crear medio de pago
            ModelMedioPago medioPago = new ModelMedioPago(tipo, monto);

            // Configurar referencia según el tipo
            if (tipo == TipoMedioPago.NOTA_CREDITO) {
                // Para NC, usar el número como referencia
                medioPago.setNumeroReferencia(notaCreditoSeleccionada.getNumeroNotaCredito());
                medioPago.setIdNotaCredito(notaCreditoSeleccionada.getIdNotaCredito());
            } else {
                medioPago.setNumeroReferencia(txtReferencia.getText().trim());
            }

            medioPago.setObservaciones(txtObservaciones.getText().trim());

            // Agregar a la lista
            mediosPago.add(medioPago);

            // Agregar a la tabla
            String referenciaTabla = tipo == TipoMedioPago.NOTA_CREDITO
                    ? notaCreditoSeleccionada.getNumeroNotaCredito()
                    : medioPago.getNumeroReferencia();

            modeloTabla.addRow(new Object[] {
                    tipo.getDescripcion(),
                    referenciaTabla,
                    monto
            });

            // Actualizar resumen
            actualizarResumen();

            // Limpiar campos
            limpiarCampos();

            System.out.println("SUCCESS Medio de pago agregado: " + tipo.getDescripcion() +
                    " - " + CURRENCY_FORMAT.format(monto));

        } catch (NumberFormatException e) {
            mostrarError("Formato de monto inválido. Use solo números y punto decimal.");
        } catch (Exception e) {
            mostrarError("Error al agregar medio de pago:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Valida que se haya buscado y validado correctamente una Nota de Crédito.
     */
    private boolean validarNotaCreditoParaAgregar() {
        // VALIDACIÓN 1: Verificar que se haya buscado una NC
        if (notaCreditoSeleccionada == null) {
            mostrarError(
                    "Debe buscar y validar una nota de crédito\n\n" +
                            "1. Ingrese el número de la NC\n" +
                            "2. Presione el botón 'Buscar'\n" +
                            "3. Verifique que aparezca la información\n" +
                            "4. Luego agregue el pago");
            txtNumeroNotaCredito.requestFocus();
            return false;
        }

        // VALIDACIÓN 2: Verificar estado (por si cambió desde la búsqueda)
        ModelNotaCredito.EstadoNota estado = notaCreditoSeleccionada.getEstado();
        if (estado != ModelNotaCredito.EstadoNota.EMITIDA
                && estado != ModelNotaCredito.EstadoNota.APLICADA) {

            mostrarError(String.format(
                    "Estado de Nota de Crédito inválido\n\n" +
                            "Estado actual: %s\n\n" +
                            "Solo se permiten estados:\n" +
                            "• EMITIDA (sin uso previo)\n" +
                            "• APLICADA (parcialmente usada con saldo)",
                    estado.getDescripcion()));
            resetearNotaCredito();
            return false;
        }

        // VALIDACIÓN 3: Verificar saldo disponible
        BigDecimal saldoDisponible = notaCreditoSeleccionada.getSaldoDisponible();
        if (saldoDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarError(String.format(
                    "Saldo Insuficiente\n\n" +
                            "La nota de crédito no tiene saldo disponible\n\n" +
                            "Saldo actual: %s\n" +
                            "Total original: %s\n" +
                            "Ya usado: %s",
                    CURRENCY_FORMAT.format(saldoDisponible),
                    CURRENCY_FORMAT.format(notaCreditoSeleccionada.getTotal()),
                    CURRENCY_FORMAT.format(notaCreditoSeleccionada.getSaldoUsado())));
            resetearNotaCredito();
            return false;
        }

        // VALIDACIÓN 4: Verificar que está activa
        if (!notaCreditoSeleccionada.isActiva()) {
            mostrarError(
                    "Nota de Crédito Inactiva\n\n" +
                            "Esta nota de crédito ha sido desactivada\n" +
                            "y no puede ser utilizada");
            resetearNotaCredito();
            return false;
        }

        return true;
    }

    /**
     * Elimina el medio de pago seleccionado.
     */
    private void eliminarMedioPago() {
        int filaSeleccionada = tablaPagos.getSelectedRow();
        if (filaSeleccionada == -1)
            return;

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de eliminar este medio de pago?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            mediosPago.remove(filaSeleccionada);
            modeloTabla.removeRow(filaSeleccionada);
            actualizarResumen();

            System.out.println("INFO Medio de pago eliminado de la posición: " + filaSeleccionada);
        }
    }

    /**
     * Actualiza los labels de resumen con los totales actuales.
     */
    private void actualizarResumen() {
        BigDecimal totalPagado = calcularTotalPagado();
        BigDecimal saldoPendiente = totalVenta.subtract(totalPagado);

        lblTotalVenta.setText(CURRENCY_FORMAT.format(totalVenta));
        lblTotalPagado.setText(CURRENCY_FORMAT.format(totalPagado));
        lblSaldoPendiente.setText(CURRENCY_FORMAT.format(saldoPendiente));

        // Cambiar color del saldo según estado
        if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            lblSaldoPendiente.setForeground(new Color(0, 153, 51)); // Verde - Pago completo
            panelVuelto.setVisible(false);
        } else if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
            lblSaldoPendiente.setForeground(new Color(0, 102, 204)); // Azul - Vuelto/Cambio
            // Mostrar panel de vuelto
            panelVuelto.setVisible(true);
            lblVuelto.setText(CURRENCY_FORMAT.format(saldoPendiente.abs()));
        } else {
            lblSaldoPendiente.setForeground(new Color(255, 69, 58)); // Rojo - Saldo pendiente
            panelVuelto.setVisible(false);
        }

        // Actualizar cálculo de devueltas
        calcularDevueltas();

        // Actualizar botones dinámicos
        actualizarBotonesDinamo();

        // Notificar cambio de saldo (para habilitar/deshabilitar botón en padre)
        if (onSaldoChangeListener != null) {
            onSaldoChangeListener.accept(saldoPendiente);
        }
    }

    /**
     * Calcula el total pagado sumando todos los medios de pago.
     * 
     * PATRÓN: Streams API para operaciones funcionales
     */
    private BigDecimal calcularTotalPagado() {
        return mediosPago.stream()
                .map(ModelMedioPago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el saldo pendiente.
     */
    private BigDecimal calcularSaldoPendiente() {
        return totalVenta.subtract(calcularTotalPagado());
    }

    /**
     * Limpia los campos del formulario.
     */
    private void limpiarCampos() {
        txtMonto.setText("");
        txtReferencia.setText("");
        txtObservaciones.setText("");
        resetearNotaCredito();
        txtMonto.requestFocus();
    }

    /**
     * Muestra un mensaje de error con formato consistente.
     * 
     * @param mensaje Mensaje a mostrar
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Error de Validación",
                JOptionPane.ERROR_MESSAGE);
    }

    // ==================== API PÚBLICA ====================

    /**
     * Establece el total de la venta.
     * 
     * @param totalVenta Total a pagar
     */
    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta != null ? totalVenta : BigDecimal.ZERO;
        actualizarResumen();
    }

    /**
     * Obtiene la lista de medios de pago agregados.
     * 
     * PATRÓN: Defensive Copy para inmutabilidad
     * 
     * @return Lista inmutable de medios de pago
     */
    public List<ModelMedioPago> getMediosPago() {
        return Collections.unmodifiableList(mediosPago);
    }

    /**
     * Establece los medios de pago y actualiza la UI.
     * 
     * @param medios Lista de medios de pago a restaurar
     */
    public void setMediosPago(List<ModelMedioPago> medios) {
        this.mediosPago.clear();
        this.modeloTabla.setRowCount(0);
        if (medios != null) {
            for (ModelMedioPago mp : medios) {
                // Defensive copy
                ModelMedioPago clon = mp.clonar();
                this.mediosPago.add(clon);

                modeloTabla.addRow(new Object[] {
                        clon.getTipo().getDescripcion(),
                        clon.getNumeroReferencia(),
                        clon.getMonto()
                });
            }
        }
        actualizarResumen();
    }

    /**
     * Verifica si el pago está completo.
     * 
     * @return true si el saldo pendiente es 0 o negativo (hay vuelto)
     */
    public boolean pagoCompleto() {
        return calcularSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Verifica si hay al menos un medio de pago agregado.
     * 
     * @return true si hay medios de pago
     */
    public boolean tieneMediosPago() {
        return !mediosPago.isEmpty();
    }

    /**
     * Resetea el panel a su estado inicial.
     * 
     * IMPORTANTE: Limpia todos los datos y vuelve al estado por defecto
     */
    public void reset() {
        mediosPago.clear();
        modeloTabla.setRowCount(0);
        limpiarCampos();
        totalVenta = BigDecimal.ZERO;
        actualizarResumen();
        panelNotaCredito.setVisible(false);
        cbxTipoPago.setSelectedIndex(0);

        System.out.println("Actualizando Panel de múltiples pagos reseteado");
    }

    /**
     * Obtiene el saldo pendiente actual.
     * 
     * @return Saldo pendiente (puede ser negativo si hay vuelto)
     */
    public BigDecimal getSaldoPendiente() {
        return calcularSaldoPendiente();
    }

    /**
     * Obtiene el vuelto/cambio a entregar al cliente.
     * 
     * @return Monto del vuelto (0 si no hay vuelto)
     */
    public BigDecimal getVuelto() {
        BigDecimal saldo = calcularSaldoPendiente();
        return saldo.compareTo(BigDecimal.ZERO) < 0
                ? saldo.abs()
                : BigDecimal.ZERO;
    }

    /**
     * Obtiene el total pagado hasta el momento.
     * 
     * @return Total pagado
     */
    public BigDecimal getTotalPagado() {
        return calcularTotalPagado();
    }

    /**
     * Verifica si algún medio de pago es una Nota de Crédito.
     * 
     * @return true si hay al menos una NC en los pagos
     */
    public boolean tieneNotaCredito() {
        return mediosPago.stream()
                .anyMatch(mp -> mp.getTipo() == TipoMedioPago.NOTA_CREDITO);
    }

    /**
     * Obtiene todas las Notas de Crédito aplicadas.
     * 
     * @return Lista de medios de pago tipo NOTA_CREDITO
     */
    public List<ModelMedioPago> getNotasCreditoAplicadas() {
        return mediosPago.stream()
                .filter(mp -> mp.getTipo() == TipoMedioPago.NOTA_CREDITO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Inyecta el servicio de Nota de Crédito (Dependency Injection).
     * 
     * PATRÓN: Dependency Injection
     * Permite usar diferentes implementaciones o mocks para testing
     * 
     * @param service Servicio de NC
     */
    public void setServiceNotaCredito(ServiceNotaCredito service) {
        this.serviceNotaCredito = service != null ? service : new ServiceNotaCredito();
    }

    /**
     * Valida el formulario completo antes de procesar la venta.
     * 
     * @return true si todo está correcto para proceder
     */
    public boolean validarFormulario() {
        // Validar que hay medios de pago
        if (!tieneMediosPago()) {
            mostrarError("Debe agregar al menos un medio de pago");
            return false;
        }

        // Validar que el pago está completo
        if (!pagoCompleto()) {
            BigDecimal saldo = calcularSaldoPendiente();
            mostrarError(String.format(
                    "El pago no está completo\n\n" +
                            "Saldo pendiente: %s",
                    CURRENCY_FORMAT.format(saldo)));
            return false;
        }

        return true;
    }

    /**
     * Obtiene un resumen textual de los medios de pago.
     * Útil para confirmaciones o impresión.
     * 
     * @return Resumen de medios de pago
     */
    public String obtenerResumenPagos() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══ RESUMEN DE PAGOS ═══\n\n");

        for (int i = 0; i < mediosPago.size(); i++) {
            ModelMedioPago mp = mediosPago.get(i);
            sb.append(String.format("%d. %s: %s\n",
                    i + 1,
                    mp.getTipo().getDescripcion(),
                    CURRENCY_FORMAT.format(mp.getMonto())));

            if (mp.getNumeroReferencia() != null && !mp.getNumeroReferencia().isEmpty()) {
                sb.append(String.format("   Ref: %s\n", mp.getNumeroReferencia()));
            }
        }

        sb.append("\n───────────────────────\n");
        sb.append(String.format("Total Venta:    %s\n", CURRENCY_FORMAT.format(totalVenta)));
        sb.append(String.format("Total Pagado:   %s\n", CURRENCY_FORMAT.format(getTotalPagado())));

        BigDecimal saldo = getSaldoPendiente();
        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            sb.append(String.format("Vuelto:         %s\n", CURRENCY_FORMAT.format(getVuelto())));
        } else if (saldo.compareTo(BigDecimal.ZERO) == 0) {
            sb.append("Estado:         PAGADO\n");
        } else {
            sb.append(String.format("Pendiente:      %s\n", CURRENCY_FORMAT.format(saldo)));
        }

        sb.append("═══════════════════════");

        return sb.toString();
    }
}
