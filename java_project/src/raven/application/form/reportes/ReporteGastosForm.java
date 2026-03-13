package raven.application.form.reportes;

import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import raven.clases.reportes.ServiceReporteGastos;
import raven.utils.ExportadorReportes;

public class ReporteGastosForm extends JPanel {

    private ServiceReporteGastos service = new ServiceReporteGastos();
    private JComboBox<String> cmbTipoGasto, cmbTipoReporte;
    private DatePicker dpInicio, dpFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal, lblMonto;
    private List<Map<String, Object>> tiposGasto;

    public ReporteGastosForm() {
        initComponents();
        cargarTiposGasto();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Gastos Reporte de Gastos Operativos");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> raven.application.Application.showForm(new ReportesMainForm()));
        headerPanel.add(lblTitulo, BorderLayout.WEST);
        headerPanel.add(btnVolver, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtrosPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:darken($Panel.background,3%)");
        filtrosPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbTipoReporte = new JComboBox<>(new String[] { "Detalle Gastos", "Resumen por Categoría", "Por Bodega" });
        cmbTipoGasto = new JComboBox<>();
        cmbTipoGasto.addItem("Todos");
        dpInicio = new DatePicker();
        dpInicio.setDate(LocalDate.now().minusMonths(1));
        dpFin = new DatePicker();
        dpFin.setDate(LocalDate.now());
        JButton btnGenerar = new JButton("Generar");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff");
        btnGenerar.addActionListener(e -> generarReporte());
        JButton btnExcel = new JButton("Excel");
        btnExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_gastos"));

        filtrosPanel.add(new JLabel("Vista:"));
        filtrosPanel.add(cmbTipoReporte);
        filtrosPanel.add(new JLabel("Tipo Gasto:"));
        filtrosPanel.add(cmbTipoGasto);
        filtrosPanel.add(new JLabel("Desde:"));
        filtrosPanel.add(dpInicio);
        filtrosPanel.add(new JLabel("Hasta:"));
        filtrosPanel.add(dpFin);
        filtrosPanel.add(btnGenerar);
        filtrosPanel.add(btnExcel);

        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines:true;rowHeight:28");
        JScrollPane scroll = new JScrollPane(tabla);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        footerPanel.setOpaque(false);
        lblTotal = new JLabel("Registros: 0");
        lblMonto = new JLabel("Total: $0");
        lblTotal.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        lblMonto.putClientProperty(FlatClientProperties.STYLE, "font:bold;foreground:$Component.accentColor");
        footerPanel.add(lblTotal);
        footerPanel.add(lblMonto);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(filtrosPanel, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        centerPanel.add(footerPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void cargarTiposGasto() {
        tiposGasto = service.getTiposGasto();
        for (Map<String, Object> t : tiposGasto) {
            cmbTipoGasto.addItem(t.get("nombre").toString());
        }
    }

    private int getIdTipoGastoSeleccionado() {
        int idx = cmbTipoGasto.getSelectedIndex();
        if (idx <= 0)
            return 0;
        return (int) tiposGasto.get(idx - 1).get("id_tipo_gasto");
    }

    private void generarReporte() {
        modelo.setRowCount(0);
        String tipo = cmbTipoReporte.getSelectedItem().toString();
        BigDecimal montoTotal = BigDecimal.ZERO;

        switch (tipo) {
            case "Resumen por Categoría" -> {
                List<Map<String, Object>> datos = service.getResumenPorCategoria(dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(
                        new String[] { "Categoría", "Total Gastos", "Monto Total", "Promedio", "Mínimo", "Máximo" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("categoria"), r.get("total_gastos"),
                            formatMoney((BigDecimal) r.get("monto_total")),
                            formatMoney((BigDecimal) r.get("promedio_gasto")),
                            formatMoney((BigDecimal) r.get("gasto_minimo")),
                            formatMoney((BigDecimal) r.get("gasto_maximo")) });
                    if (r.get("monto_total") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("monto_total"));
                }
            }
            case "Por Bodega" -> {
                List<Map<String, Object>> datos = service.getGastosPorBodega(0, dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "Bodega", "Categoría", "Total Gastos", "Monto Total" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("bodega"), r.get("categoria"), r.get("total_gastos"),
                            formatMoney((BigDecimal) r.get("monto_total")) });
                    if (r.get("monto_total") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("monto_total"));
                }
            }
            default -> {
                List<Map<String, Object>> datos = service.getGastosPorTipo(getIdTipoGastoSeleccionado(),
                        dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "ID", "Tipo", "Categoría", "Concepto", "Monto", "Proveedor",
                        "Bodega", "Usuario", "Fecha" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("id_gasto"), r.get("tipo_gasto"), r.get("categoria"),
                            r.get("concepto"),
                            formatMoney((BigDecimal) r.get("monto")), r.get("proveedor_persona"), r.get("bodega"),
                            r.get("usuario"), r.get("fecha_gasto") });
                    if (r.get("monto") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("monto"));
                }
            }
        }
        lblTotal.setText("Registros: " + modelo.getRowCount());
        lblMonto.setText("Total: " + formatMoney(montoTotal));
    }

    private String formatMoney(BigDecimal val) {
        if (val == null)
            return "$0";
        return String.format("$%,.0f", val);
    }
}

