package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import raven.clases.reportes.ServiceReporteCaja;
import raven.clases.reportes.ReporteCierreCajaPDF;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.clases.productos.ImpresionCierreCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ResumenCierreCaja;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import raven.datetime.component.date.DatePicker;

/**
 * Formulario principal para monitoreo y reportes de cajas.
 * 
 * Tab 1: Cajas Activas - muestra cajas con movimiento abierto en tiempo real
 * Tab 2: Historial de Cierres - reportes de cuadres históricos con filtros
 * 
 * @author Sistema
 * @version 1.0
 */
public class MonitorCajasForm extends JPanel {

    // ==================== CONSTANTES ====================
    private static final String STYLE_PANEL = "arc:20;background:darken($Panel.background,3%)";
    private static final String STYLE_TABLE = "showHorizontalLines:true;showVerticalLines:false;cellMargins:5,10,5,10";
    private static final String STYLE_BUTTON = "arc:10";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== SERVICIOS ====================
    private final ServiceReporteCaja serviceReporte;
    private final ServiceCajaMovimiento serviceCajaMovimiento;
    private Timer timerActualizacion;

    // ==================== COMPONENTES - TAB CAJAS ACTIVAS ====================
    private JTable tablaCajasActivas;
    private DefaultTableModel modeloCajasActivas;
    private JLabel lblTotalCajasActivas;
    private JButton btnRefrescarActivas;

    // ==================== COMPONENTES - TAB HISTORIAL ====================
    private JTable tablaHistorial;
    private DefaultTableModel modeloHistorial;
    private DatePicker datePickerInicio;
    private DatePicker datePickerFin;
    private JFormattedTextField txtFechaInicio;
    private JFormattedTextField txtFechaFin;
    private JComboBox<String> cmbEstadoCuadre;
    private JButton btnBuscar;

    // Panel de estadísticas
    private JLabel lblTotalCierres;
    private JLabel lblPorcentajeCuadrados;
    private JLabel lblSumaDiferencias;
    private JLabel lblSumaVentas;

    // ==================== CONSTRUCTOR ====================
    public MonitorCajasForm() {
        this.serviceReporte = new ServiceReporteCaja();
        this.serviceCajaMovimiento = new ServiceCajaMovimiento();
        initComponents();
        configurarEstilos();
        cargarCajasActivas();
        iniciarActualizacionAutomatica();
    }

    // ==================== INICIALIZACIÓN ====================
    private void initComponents() {
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Título
        JLabel lblTitulo = new JLabel("Resumen Monitor de Cajas");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        add(lblTitulo, BorderLayout.NORTH);

        // TabbedPane principal
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty(FlatClientProperties.STYLE,
                "tabType:card;" +
                        "tabHeight:40;" +
                        "tabSelectionHeight:3;" +
                        "tabArc:10");

        // Tab 1: Cajas Activas
        JPanel panelCajasActivas = crearPanelCajasActivas();
        tabbedPane.addTab(" Cajas Activas", panelCajasActivas);

        // Tab 2: Historial de Cierres
        JPanel panelHistorial = crearPanelHistorial();
        tabbedPane.addTab(" Historial de Cierres", panelHistorial);

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== TAB 1: CAJAS ACTIVAS ====================
    private JPanel crearPanelCajasActivas() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Toolbar superior
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        lblTotalCajasActivas = new JLabel("Cajas abiertas: 0");
        lblTotalCajasActivas.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        btnRefrescarActivas = new JButton("Actualizando Refrescar");
        btnRefrescarActivas.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON);
        btnRefrescarActivas.addActionListener(e -> cargarCajasActivas());

        JLabel lblAutoRefresh = new JLabel("(Auto-actualización cada 30s)");
        lblAutoRefresh.setForeground(Color.GRAY);

        toolbar.add(lblTotalCajasActivas);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(btnRefrescarActivas);
        toolbar.add(lblAutoRefresh);

        panel.add(toolbar, BorderLayout.NORTH);

        // Tabla de cajas activas
        String[] columnas = { "Caja", "Usuario", "Apertura", "Duración", "Monto Inicial",
                "Ventas", "Egresos", "Monto Actual" };
        modeloCajasActivas = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaCajasActivas = new JTable(modeloCajasActivas);
        configurarTabla(tablaCajasActivas);

        // Doble clic para ver detalle
        tablaCajasActivas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verDetalleSeleccionadoActivas();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tablaCajasActivas);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== TAB 2: HISTORIAL DE CIERRES ====================
    private JPanel crearPanelHistorial() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Panel de filtros
        JPanel panelFiltros = crearPanelFiltros();
        panel.add(panelFiltros, BorderLayout.NORTH);

        // Panel central con tabla
        String[] columnas = { "#", "Caja", "Usuario", "Fecha Cierre", "Duración",
                "Ventas", "Egresos", "Esperado", "Final", "Diferencia", "Estado" };
        modeloHistorial = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaHistorial = new JTable(modeloHistorial);
        configurarTabla(tablaHistorial);
        configurarRenderersHistorial();

        // Doble clic para ver detalle y Click derecho para menú
        tablaHistorial.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verDetalleSeleccionadoHistorial();
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    mostrarMenuContextual(e);
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    mostrarMenuContextual(e);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tablaHistorial);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de estadísticas
        JPanel panelStats = crearPanelEstadisticas();
        panel.add(panelStats, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Fecha Inicio
        panel.add(new JLabel("Desde:"));
        txtFechaInicio = new JFormattedTextField();
        txtFechaInicio.setColumns(10);
        txtFechaInicio.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        datePickerInicio = new DatePicker();
        datePickerInicio.setDateSelectionAble(d -> !d.isAfter(LocalDate.now()));
        datePickerInicio.addDateSelectionListener(e -> {
            if (datePickerInicio.getSelectedDate() != null) {
                txtFechaInicio.setText(datePickerInicio.getSelectedDate().format(FORMATO_FECHA));
            }
        });
        datePickerInicio.setEditor(txtFechaInicio);
        txtFechaInicio.setText(LocalDate.now().minusDays(7).format(FORMATO_FECHA));
        datePickerInicio.setSelectedDate(LocalDate.now().minusDays(7));
        panel.add(txtFechaInicio);

        // Fecha Fin
        panel.add(new JLabel("Hasta:"));
        txtFechaFin = new JFormattedTextField();
        txtFechaFin.setColumns(10);
        txtFechaFin.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        datePickerFin = new DatePicker();
        datePickerFin.setDateSelectionAble(d -> !d.isAfter(LocalDate.now()));
        datePickerFin.addDateSelectionListener(e -> {
            if (datePickerFin.getSelectedDate() != null) {
                txtFechaFin.setText(datePickerFin.getSelectedDate().format(FORMATO_FECHA));
            }
        });
        datePickerFin.setEditor(txtFechaFin);
        txtFechaFin.setText(LocalDate.now().format(FORMATO_FECHA));
        datePickerFin.setSelectedDate(LocalDate.now());
        panel.add(txtFechaFin);

        // Estado de cuadre
        panel.add(new JLabel("Estado:"));
        cmbEstadoCuadre = new JComboBox<>(new String[] { "Todos", "CUADRADO", "SOBRANTE", "FALTANTE" });
        cmbEstadoCuadre.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panel.add(cmbEstadoCuadre);

        // Botón buscar
        btnBuscar = new JButton("Buscar Buscar");
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON + ";background:$Component.accentColor");
        btnBuscar.addActionListener(e -> buscarHistorial());
        panel.add(btnBuscar);

        return panel;
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        lblTotalCierres = crearLabelStat("Resumen Total Cierres:", "0");
        lblPorcentajeCuadrados = crearLabelStat("SUCCESS  Cuadrados:", "0%");
        lblSumaDiferencias = crearLabelStat(" Suma Diferencias:", "$0.00");
        lblSumaVentas = crearLabelStat("Efectivo Total Ventas:", "$0.00");

        panel.add(lblTotalCierres);
        panel.add(lblPorcentajeCuadrados);
        panel.add(lblSumaDiferencias);
        panel.add(lblSumaVentas);

        return panel;
    }

    private JLabel crearLabelStat(String titulo, String valor) {
        JLabel label = new JLabel(titulo + " " + valor);
        label.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        return label;
    }

    // ==================== CONFIGURACIÓN ====================
    private void configurarEstilos() {
        putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");
    }

    private void configurarTabla(JTable tabla) {
        tabla.setRowHeight(32);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.putClientProperty(FlatClientProperties.STYLE, STYLE_TABLE);
    }

    private void configurarRenderersHistorial() {
        // Renderer para columna de estado con colores
        DefaultTableCellRenderer estadoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null && !isSelected) {
                    String estado = value.toString();
                    switch (estado) {
                        case "CUADRADO":
                            c.setForeground(new Color(76, 175, 80)); // Verde
                            break;
                        case "SOBRANTE":
                            c.setForeground(new Color(33, 150, 243)); // Azul
                            break;
                        case "FALTANTE":
                            c.setForeground(new Color(244, 67, 54)); // Rojo
                            break;
                        default:
                            c.setForeground(table.getForeground());
                    }
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        tablaHistorial.getColumnModel().getColumn(10).setCellRenderer(estadoRenderer);

        // Renderer para columnas de moneda
        DefaultTableCellRenderer monedaRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof BigDecimal) {
                    setText(String.format("$%,.0f", value));
                } else {
                    super.setValue(value);
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };

        for (int i = 5; i <= 9; i++) {
            tablaHistorial.getColumnModel().getColumn(i).setCellRenderer(monedaRenderer);
        }
    }

    // ==================== CARGA DE DATOS ====================
    private void cargarCajasActivas() {
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return serviceReporte.obtenerCajasActivas();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> cajasActivas = get();
                    poblarTablaCajasActivas(cajasActivas);
                    lblTotalCajasActivas.setText("Cajas abiertas: " + cajasActivas.size());
                } catch (Exception e) {
                    System.err.println("Error cargando cajas activas: " + e.getMessage());
                    lblTotalCajasActivas.setText("Cajas abiertas: Error");
                }
            }
        };
        worker.execute();
    }

    private void poblarTablaCajasActivas(List<Map<String, Object>> cajasActivas) {
        modeloCajasActivas.setRowCount(0);

        for (Map<String, Object> caja : cajasActivas) {
            LocalDateTime fechaApertura = (LocalDateTime) caja.get("fechaApertura");
            Long duracionMin = (Long) caja.get("duracionMinutos");

            String duracion = duracionMin != null ? formatearDuracion(duracionMin) : "-";
            String apertura = fechaApertura != null ? fechaApertura.format(FORMATO_FECHA_HORA) : "-";

            Object[] fila = {
                    caja.get("nombreCaja"),
                    caja.get("nombreUsuario"),
                    apertura,
                    duracion,
                    formatearMoneda((BigDecimal) caja.get("montoInicial")),
                    formatearMoneda((BigDecimal) caja.get("totalVentas")),
                    formatearMoneda((BigDecimal) caja.get("totalEgresos")),
                    formatearMoneda((BigDecimal) caja.get("montoActual"))
            };
            modeloCajasActivas.addRow(fila);
        }
    }

    private void buscarHistorial() {
        btnBuscar.setEnabled(false);
        btnBuscar.setText("Buscando...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Map<String, Object>> cierres;
            Map<String, Object> stats;

            @Override
            protected Void doInBackground() throws Exception {
                LocalDate fechaInicio = datePickerInicio.getSelectedDate();
                LocalDate fechaFin = datePickerFin.getSelectedDate();
                String estado = cmbEstadoCuadre.getSelectedIndex() == 0 ? null
                        : (String) cmbEstadoCuadre.getSelectedItem();

                cierres = serviceReporte.obtenerHistorialCierres(fechaInicio, fechaFin, null, estado);
                stats = serviceReporte.obtenerEstadisticasCuadres(fechaInicio, fechaFin, null);
                return null;
            }

            @Override
            protected void done() {
                btnBuscar.setEnabled(true);
                btnBuscar.setText("Buscar");

                try {
                    get();
                    poblarTablaHistorial(cierres);
                    actualizarEstadisticas(stats);
                } catch (Exception e) {
                    System.err.println("Error buscando historial: " + e.getMessage());
                    JOptionPane.showMessageDialog(MonitorCajasForm.this,
                            "Error al buscar historial: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void poblarTablaHistorial(List<Map<String, Object>> cierres) {
        modeloHistorial.setRowCount(0);

        for (Map<String, Object> cierre : cierres) {
            LocalDateTime fechaCierre = (LocalDateTime) cierre.get("fechaCierre");
            Long duracionMin = (Long) cierre.get("duracionMinutos");

            String duracion = duracionMin != null ? formatearDuracion(duracionMin) : "-";
            String fechaCierreStr = fechaCierre != null ? fechaCierre.format(FORMATO_FECHA_HORA) : "-";

            Object[] fila = {
                    cierre.get("idMovimiento"),
                    cierre.get("nombreCaja"),
                    cierre.get("nombreUsuario"),
                    fechaCierreStr,
                    duracion,
                    cierre.get("totalVentas"),
                    cierre.get("totalEgresos"),
                    cierre.get("montoEsperado"),
                    cierre.get("montoFinal"),
                    cierre.get("diferencia"),
                    cierre.get("estadoCuadre")
            };
            modeloHistorial.addRow(fila);
        }
    }

    private void actualizarEstadisticas(Map<String, Object> stats) {
        int totalCierres = (Integer) stats.getOrDefault("totalCierres", 0);
        double porcentaje = (Double) stats.getOrDefault("porcentajeCuadrados", 0.0);
        BigDecimal sumaDif = (BigDecimal) stats.getOrDefault("sumaDiferencias", BigDecimal.ZERO);
        BigDecimal sumaVentas = (BigDecimal) stats.getOrDefault("sumaVentas", BigDecimal.ZERO);

        lblTotalCierres.setText("Total Cierres: " + totalCierres);
        lblPorcentajeCuadrados.setText(String.format("Cuadrados: %.1f%%", porcentaje));
        lblSumaDiferencias.setText("Suma Diferencias: " + formatearMoneda(sumaDif));
        lblSumaVentas.setText("Total Ventas: " + formatearMoneda(sumaVentas));
    }

    // ==================== ACCIONES ====================
    private void verDetalleSeleccionadoActivas() {
        int row = tablaCajasActivas.getSelectedRow();
        if (row < 0)
            return;

        // Obtener el idMovimiento de los datos
        try {
            List<Map<String, Object>> cajasActivas = serviceReporte.obtenerCajasActivas();
            if (row < cajasActivas.size()) {
                int idMovimiento = (Integer) cajasActivas.get(row).get("idMovimiento");
                abrirDialogoDetalle(idMovimiento);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo detalle: " + e.getMessage());
        }
    }

    private void verDetalleSeleccionadoHistorial() {
        int row = tablaHistorial.getSelectedRow();
        if (row < 0)
            return;

        Object idObj = modeloHistorial.getValueAt(row, 0);
        if (idObj instanceof Integer) {
            abrirDialogoDetalle((Integer) idObj);
        }
    }

    private void abrirDialogoDetalle(int idMovimiento) {
        try {
            Map<String, Object> detalle = serviceReporte.obtenerDetalleMovimiento(idMovimiento);
            if (detalle != null && !detalle.isEmpty()) {
                DetalleMovimientoCajaDialog dialog = new DetalleMovimientoCajaDialog(
                        (Frame) SwingUtilities.getWindowAncestor(this), detalle);
                dialog.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al obtener detalle: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== ACTUALIZACIÓN AUTOMÁTICA ====================
    private void iniciarActualizacionAutomatica() {
        timerActualizacion = new Timer(30000, e -> cargarCajasActivas()); // 30 segundos
        timerActualizacion.start();
    }

    public void detenerActualizacion() {
        if (timerActualizacion != null) {
            timerActualizacion.stop();
        }
    }

    // ==================== UTILIDADES ====================
    private String formatearMoneda(BigDecimal valor) {
        if (valor == null)
            return "$0";
        return String.format("$%,.0f", valor);
    }

    private String formatearDuracion(long minutos) {
        if (minutos < 60) {
            return minutos + " min";
        }
        long horas = minutos / 60;
        long mins = minutos % 60;
        return String.format("%dh %dm", horas, mins);
    }

    // ==================== MENÚ CONTEXTUAL E IMPRESIÓN ====================
    private void mostrarMenuContextual(java.awt.event.MouseEvent e) {
        int row = tablaHistorial.rowAtPoint(e.getPoint());
        if (row >= 0 && row < tablaHistorial.getRowCount()) {
            tablaHistorial.setRowSelectionInterval(row, row);

            JPopupMenu popup = new JPopupMenu();

            JMenuItem itemPrint = new JMenuItem("Imprimir Ticket (80mm)");
            itemPrint.addActionListener(evt -> imprimirTicketSeleccionado());

            JMenuItem itemPdf = new JMenuItem("Exportar a PDF");
            itemPdf.addActionListener(evt -> exportarPdfSeleccionado());

            popup.add(itemPrint);
            popup.add(itemPdf);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void imprimirTicketSeleccionado() {
        int row = tablaHistorial.getSelectedRow();
        if (row < 0)
            return;

        Object idObj = modeloHistorial.getValueAt(row, 0);
        if (idObj instanceof Integer) {
            int idMovimiento = (Integer) idObj;
            generarDocumento(idMovimiento, "TICKET");
        }
    }

    private void exportarPdfSeleccionado() {
        int row = tablaHistorial.getSelectedRow();
        if (row < 0)
            return;

        Object idObj = modeloHistorial.getValueAt(row, 0);
        if (idObj instanceof Integer) {
            int idMovimiento = (Integer) idObj;
            generarDocumento(idMovimiento, "PDF");
        }
    }

    private void generarDocumento(int idMovimiento, String tipo) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            ModelCajaMovimiento movimiento;
            ResumenCierreCaja resumen;

            @Override
            protected Void doInBackground() throws Exception {
                // Obtener datos completos
                movimiento = serviceCajaMovimiento.obtenerMovimientoPorId(idMovimiento);
                resumen = serviceCajaMovimiento.obtenerResumenCompletoConMovimientos(idMovimiento);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Verificar excepciones

                    if (movimiento != null && resumen != null) {
                        if ("TICKET".equals(tipo)) {
                            ImpresionCierreCaja impresor = new ImpresionCierreCaja();
                            boolean exito = impresor.imprimirCierreCaja(movimiento, resumen);
                            if (exito) {
                                JOptionPane.showMessageDialog(MonitorCajasForm.this,
                                        "Ticket enviado a la impresora.",
                                        "Impresión", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(MonitorCajasForm.this,
                                        "No se pudo imprimir. Verifique la impresora configurada.",
                                        "Error de Impresión", JOptionPane.ERROR_MESSAGE);
                            }
                        } else if ("PDF".equals(tipo)) {
                            ReporteCierreCajaPDF generador = new ReporteCierreCajaPDF();
                            generador.generarPDF(movimiento, resumen, MonitorCajasForm.this);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MonitorCajasForm.this,
                            "Error al generar documento: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
