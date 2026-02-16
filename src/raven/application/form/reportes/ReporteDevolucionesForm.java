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
import raven.clases.reportes.ServiceReporteDevoluciones;
import raven.utils.ExportadorReportes;

public class ReporteDevolucionesForm extends JPanel {

    private ServiceReporteDevoluciones service = new ServiceReporteDevoluciones();
    private JComboBox<String> cmbTipoReporte;
    private DatePicker dpInicio, dpFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal, lblMonto;

    public ReporteDevolucionesForm() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Actualizando Reporte de Devoluciones");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> raven.application.Application.showForm(new ReportesMainForm()));
        headerPanel.add(lblTitulo, BorderLayout.WEST);
        headerPanel.add(btnVolver, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtrosPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:darken($Panel.background,3%)");
        filtrosPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbTipoReporte = new JComboBox<>(
                new String[] { "Devoluciones", "Análisis por Motivo", "Notas Crédito Pendientes" });
        dpInicio = new DatePicker();
        dpInicio.setDate(LocalDate.now().minusMonths(1));
        dpFin = new DatePicker();
        dpFin.setDate(LocalDate.now());
        JButton btnGenerar = new JButton("Generar");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff");
        btnGenerar.addActionListener(e -> generarReporte());
        JButton btnExcel = new JButton("Excel");
        btnExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_devoluciones"));

        filtrosPanel.add(new JLabel("Vista:"));
        filtrosPanel.add(cmbTipoReporte);
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
        footerPanel.add(lblTotal);
        footerPanel.add(lblMonto);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(filtrosPanel, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        centerPanel.add(footerPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void generarReporte() {
        modelo.setRowCount(0);
        String tipo = cmbTipoReporte.getSelectedItem().toString();
        BigDecimal montoTotal = BigDecimal.ZERO;

        switch (tipo) {
            case "Análisis por Motivo" -> {
                List<Map<String, Object>> datos = service.getAnalisisPorMotivo(dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(
                        new String[] { "Motivo", "Total", "Monto Total", "Promedio", "Clientes", "Porcentaje" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("motivo"), r.get("total_devoluciones"),
                            formatMoney((BigDecimal) r.get("monto_total")),
                            formatMoney((BigDecimal) r.get("promedio_devolucion")),
                            r.get("clientes_distintos"), r.get("porcentaje") + "%" });
                    if (r.get("monto_total") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("monto_total"));
                }
            }
            case "Notas Crédito Pendientes" -> {
                List<Map<String, Object>> datos = service.getNotasCreditoPendientes();
                modelo.setColumnIdentifiers(new String[] { "#Nota", "Cliente", "DNI", "Total", "Saldo Disponible",
                        "Días x Vencer", "Estado" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("numero_nota_credito"), r.get("cliente"), r.get("dni"),
                            formatMoney((BigDecimal) r.get("total")),
                            formatMoney((BigDecimal) r.get("saldo_disponible")),
                            r.get("dias_para_vencer"), r.get("estado") });
                    if (r.get("saldo_disponible") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("saldo_disponible"));
                }
            }
            default -> {
                List<Map<String, Object>> datos = service.getDevolucionesPorPeriodo(dpInicio.getDate(),
                        dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "#Devolución", "Fecha", "Tipo", "Motivo", "Cliente", "Total",
                        "Estado", "#Nota Crédito" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("numero_devolucion"), r.get("fecha_devolucion"),
                            r.get("tipo_devolucion"),
                            r.get("motivo"), r.get("cliente"), formatMoney((BigDecimal) r.get("total_devolucion")),
                            r.get("estado"), r.get("numero_nota_credito") });
                    if (r.get("total_devolucion") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("total_devolucion"));
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

