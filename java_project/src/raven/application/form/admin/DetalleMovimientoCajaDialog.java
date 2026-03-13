package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import raven.controlador.admin.ResumenCierreCaja;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Diálogo para mostrar detalle completo de un movimiento de caja.
 * 
 * Soporta tanto movimientos abiertos (cajas activas) como cerrados.
 * Muestra:
 * - Estado visual ( ACTIVA /  CERRADA)
 * - Información general del movimiento
 * - Desglose de ventas por tipo de pago
 * - Resumen de egresos (gastos + compras)
 * - Diferencia y estado del cuadre (si cerrada)
 * 
 * @author Sistema
 * @version 1.0
 */
public class DetalleMovimientoCajaDialog extends JDialog {

    // ==================== CONSTANTES ====================
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String STYLE_PANEL = "arc:15;background:darken($Panel.background,3%)";
    private static final Color COLOR_ACTIVA = new Color(76, 175, 80); // Verde
    private static final Color COLOR_CERRADA = new Color(158, 158, 158); // Gris
    private static final Color COLOR_CUADRADO = new Color(76, 175, 80); // Verde
    private static final Color COLOR_SOBRANTE = new Color(33, 150, 243); // Azul
    private static final Color COLOR_FALTANTE = new Color(244, 67, 54); // Rojo

    // ==================== DATOS ====================
    private final Map<String, Object> detalle;

    // ==================== CONSTRUCTOR ====================
    public DetalleMovimientoCajaDialog(Frame parent, Map<String, Object> detalle) {
        super(parent, "Detalle de Movimiento", true);
        this.detalle = detalle;

        initComponents();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    // ==================== INICIALIZACIÓN ====================
    private void initComponents() {
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new BoxLayout(panelPrincipal, BoxLayout.Y_AXIS));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // 1. Encabezado con estado
        JPanel panelEncabezado = crearPanelEncabezado();
        panelPrincipal.add(panelEncabezado);
        panelPrincipal.add(Box.createRigidArea(new Dimension(0, 15)));

        // 2. Información general
        JPanel panelInfo = crearPanelInfoGeneral();
        panelPrincipal.add(panelInfo);
        panelPrincipal.add(Box.createRigidArea(new Dimension(0, 15)));

        // 3. Resumen financiero
        JPanel panelFinanciero = crearPanelResumenFinanciero();
        panelPrincipal.add(panelFinanciero);
        panelPrincipal.add(Box.createRigidArea(new Dimension(0, 15)));

        // 4. Desglose de pagos (si hay ventas)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> desglose = (List<Map<String, Object>>) detalle.get("desglosePagos");
        if (desglose != null && !desglose.isEmpty()) {
            JPanel panelDesglose = crearPanelDesglosePagos(desglose);
            panelPrincipal.add(panelDesglose);
            panelPrincipal.add(Box.createRigidArea(new Dimension(0, 15)));
        }

        // 5. Observaciones (si existen)
        String observaciones = (String) detalle.get("observaciones");
        if (observaciones != null && !observaciones.trim().isEmpty()) {
            JPanel panelObservaciones = crearPanelObservaciones(observaciones);
            panelPrincipal.add(panelObservaciones);
            panelPrincipal.add(Box.createRigidArea(new Dimension(0, 15)));
        }

        // 6. Botón cerrar
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setPreferredSize(new Dimension(120, 38));
        btnCerrar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCerrar.addActionListener(e -> dispose());
        panelBotones.add(btnCerrar);
        panelPrincipal.add(panelBotones);

        // Scroll por si el contenido es largo
        JScrollPane scrollPane = new JScrollPane(panelPrincipal);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane);
        setMinimumSize(new Dimension(550, 500));
        setPreferredSize(new Dimension(580, 650));
    }

    // ==================== PANELES ====================
    private JPanel crearPanelEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        Boolean estaAbierto = (Boolean) detalle.get("estaAbierto");
        boolean abierto = estaAbierto != null && estaAbierto;

        // Indicador de estado grande
        JLabel lblEstado;
        if (abierto) {
            lblEstado = new JLabel(" CAJA ACTIVA");
            lblEstado.setForeground(COLOR_ACTIVA);
        } else {
            String estadoCuadre = (String) detalle.get("estadoCuadre");
            String emoji = "";
            Color color = COLOR_CERRADA;

            if ("CUADRADO".equals(estadoCuadre)) {
                emoji = "SUCCESS ";
                color = COLOR_CUADRADO;
            } else if ("SOBRANTE".equals(estadoCuadre)) {
                emoji = "";
                color = COLOR_SOBRANTE;
            } else if ("FALTANTE".equals(estadoCuadre)) {
                emoji = "";
                color = COLOR_FALTANTE;
            }

            lblEstado = new JLabel(emoji + " CERRADA - " + (estadoCuadre != null ? estadoCuadre : ""));
            lblEstado.setForeground(color);
        }
        lblEstado.putClientProperty(FlatClientProperties.STYLE, "font:bold +6");
        panel.add(lblEstado, BorderLayout.WEST);

        // Número de movimiento
        Integer idMov = (Integer) detalle.get("idMovimiento");
        JLabel lblMovimiento = new JLabel("Movimiento #" + (idMov != null ? idMov : "--"));
        lblMovimiento.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        lblMovimiento.setForeground(Color.GRAY);
        panel.add(lblMovimiento, BorderLayout.EAST);

        return panel;
    }

    private JPanel crearPanelInfoGeneral() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 15, 8));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(" Información General"),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Caja
        agregarCampo(panel, "Caja:", (String) detalle.get("nombreCaja"));

        // Usuario
        agregarCampo(panel, "Usuario:", (String) detalle.get("nombreUsuario"));

        // Fecha Apertura
        LocalDateTime fechaApertura = (LocalDateTime) detalle.get("fechaApertura");
        agregarCampo(panel, "Apertura:",
                fechaApertura != null ? fechaApertura.format(FORMATO_FECHA_HORA) : "--");

        // Fecha Cierre (si aplica)
        LocalDateTime fechaCierre = (LocalDateTime) detalle.get("fechaCierre");
        if (fechaCierre != null) {
            agregarCampo(panel, "Cierre:", fechaCierre.format(FORMATO_FECHA_HORA));
        }

        // Duración
        Long duracionMin = (Long) detalle.get("duracionMinutos");
        if (duracionMin != null) {
            agregarCampo(panel, "Duración:", formatearDuracion(duracionMin));
        }

        // Cantidad de ventas
        Integer cantVentas = (Integer) detalle.get("cantidadVentas");
        agregarCampo(panel, "N° Ventas:", cantVentas != null ? cantVentas.toString() : "0");

        return panel;
    }

    private JPanel crearPanelResumenFinanciero() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 20, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Efectivo Resumen Financiero"),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        BigDecimal montoInicial = getBigDecimal("montoInicial");
        BigDecimal totalVentas = getBigDecimal("totalVentas");
        BigDecimal totalGastos = getBigDecimal("totalGastos");
        BigDecimal totalCompras = getBigDecimal("totalCompras");
        BigDecimal totalEgresos = getBigDecimal("totalEgresos");
        BigDecimal montoEsperado = getBigDecimal("montoEsperado");
        BigDecimal montoFinal = getBigDecimal("montoFinal");
        BigDecimal diferencia = getBigDecimal("diferencia");

        // Izquierda: Ingresos
        JPanel panelIngresos = new JPanel();
        panelIngresos.setLayout(new BoxLayout(panelIngresos, BoxLayout.Y_AXIS));
        panelIngresos.setOpaque(false);

        agregarLineaFinanciera(panelIngresos, "Dinero Monto Inicial:", montoInicial, null);
        agregarLineaFinanciera(panelIngresos, "Carrito Total Ventas:", totalVentas, COLOR_CUADRADO);
        panelIngresos.add(Box.createRigidArea(new Dimension(0, 10)));
        agregarLineaFinanciera(panelIngresos, "Resumen Monto Esperado:", montoEsperado, new Color(0, 122, 255));

        panel.add(panelIngresos);

        // Derecha: Egresos y resultado
        JPanel panelEgresos = new JPanel();
        panelEgresos.setLayout(new BoxLayout(panelEgresos, BoxLayout.Y_AXIS));
        panelEgresos.setOpaque(false);

        agregarLineaFinanciera(panelEgresos, "Recibo Gastos Operativos:", totalGastos, COLOR_FALTANTE);
        agregarLineaFinanciera(panelEgresos, "Caja Compras Externas:", totalCompras, COLOR_FALTANTE);
        panelEgresos.add(Box.createRigidArea(new Dimension(0, 10)));
        agregarLineaFinanciera(panelEgresos, " Total Egresos:", totalEgresos, new Color(200, 30, 50));

        panel.add(panelEgresos);

        // Si está cerrado, mostrar resultado
        Boolean estaAbierto = (Boolean) detalle.get("estaAbierto");
        if (estaAbierto != null && !estaAbierto) {
            panel.add(crearLineaResaltada("Dinero Monto Final Contado:", montoFinal));

            String estadoCuadre = (String) detalle.get("estadoCuadre");
            Color colorDif = COLOR_CERRADA;
            if ("CUADRADO".equals(estadoCuadre))
                colorDif = COLOR_CUADRADO;
            else if ("SOBRANTE".equals(estadoCuadre))
                colorDif = COLOR_SOBRANTE;
            else if ("FALTANTE".equals(estadoCuadre))
                colorDif = COLOR_FALTANTE;

            panel.add(crearLineaResaltada(" Diferencia:", diferencia, colorDif));
        }

        return panel;
    }

    private JPanel crearPanelDesglosePagos(List<Map<String, Object>> desglose) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(" Desglose por Tipo de Pago"),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] columnas = { "Tipo de Pago", "Cantidad", "Total" };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Map<String, Object> pago : desglose) {
            String tipo = (String) pago.get("tipoPago");
            Integer cantidad = (Integer) pago.get("cantidad");
            BigDecimal total = (BigDecimal) pago.get("total");

            modelo.addRow(new Object[] {
                    ResumenCierreCaja.obtenerDescripcionTipo(tipo),
                    cantidad,
                    formatearMoneda(total)
            });
        }

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.putClientProperty(FlatClientProperties.STYLE, "arc:8");

        // Centrar columna cantidad
        DefaultTableCellRenderer centroRenderer = new DefaultTableCellRenderer();
        centroRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(1).setCellRenderer(centroRenderer);

        // Alinear total a la derecha
        DefaultTableCellRenderer derechaRenderer = new DefaultTableCellRenderer();
        derechaRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(2).setCellRenderer(derechaRenderer);

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setPreferredSize(new Dimension(450, Math.min(150, 30 + desglose.size() * 28)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelObservaciones(String observaciones) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Nota Observaciones"),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea txtObservaciones = new JTextArea(observaciones);
        txtObservaciones.setEditable(false);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        txtObservaciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtObservaciones.setBackground(panel.getBackground());

        JScrollPane scrollPane = new JScrollPane(txtObservaciones);
        scrollPane.setPreferredSize(new Dimension(450, 80));
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== UTILIDADES ====================
    private void agregarCampo(JPanel panel, String etiqueta, String valor) {
        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEtiqueta.setForeground(Color.GRAY);
        panel.add(lblEtiqueta);

        JLabel lblValor = new JLabel(valor != null ? valor : "--");
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lblValor);
    }

    private void agregarLineaFinanciera(JPanel panel, String etiqueta, BigDecimal valor, Color color) {
        JPanel lineaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        lineaPanel.setOpaque(false);
        lineaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lineaPanel.add(lblEtiqueta);

        JLabel lblValor = new JLabel(formatearMoneda(valor));
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 14));
        if (color != null) {
            lblValor.setForeground(color);
        }
        lineaPanel.add(lblValor);

        panel.add(lineaPanel);
    }

    private JPanel crearLineaResaltada(String etiqueta, BigDecimal valor) {
        return crearLineaResaltada(etiqueta, valor, null);
    }

    private JPanel crearLineaResaltada(String etiqueta, BigDecimal valor, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEtiqueta.setForeground(Color.GRAY);
        lblEtiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValor = new JLabel(formatearMoneda(valor));
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        if (color != null) {
            lblValor.setForeground(color);
        }
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblEtiqueta);
        panel.add(lblValor);

        return panel;
    }

    private BigDecimal getBigDecimal(String key) {
        Object val = detalle.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        return BigDecimal.ZERO;
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null)
            return "$0";
        return String.format("$%,.0f", valor);
    }

    private String formatearDuracion(long minutos) {
        if (minutos < 60) {
            return minutos + " minutos";
        }
        long horas = minutos / 60;
        long mins = minutos % 60;
        return String.format("%d hora%s %d min", horas, horas == 1 ? "" : "s", mins);
    }
}

