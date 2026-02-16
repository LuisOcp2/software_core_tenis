package raven.application.form.reportes;

import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import raven.clases.reportes.ServiceReporteTraspasos;
import raven.utils.ExportadorReportes;

public class ReporteTraspasoForm extends JPanel {

    private ServiceReporteTraspasos service = new ServiceReporteTraspasos();
    private JComboBox<String> cmbTipoReporte;
    private DatePicker dpInicio, dpFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;

    public ReporteTraspasoForm() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel(" Reporte de Traspasos");
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
                new String[] { "Historial Traspasos", "Pendientes/En Tránsito", "Flujo entre Bodegas" });
        dpInicio = new DatePicker();
        dpInicio.setDate(LocalDate.now().minusMonths(1));
        dpFin = new DatePicker();
        dpFin.setDate(LocalDate.now());
        JButton btnGenerar = new JButton("Generar");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff");
        btnGenerar.addActionListener(e -> generarReporte());
        JButton btnExcel = new JButton("Excel");
        btnExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_traspasos"));

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

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footerPanel.setOpaque(false);
        lblTotal = new JLabel("Registros: 0");
        footerPanel.add(lblTotal);

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

        switch (tipo) {
            case "Pendientes/En Tránsito" -> {
                List<Map<String, Object>> datos = service.getTraspasosPendientes();
                modelo.setColumnIdentifiers(new String[] { "#Traspaso", "Fecha", "Origen", "Destino", "Usuario",
                        "Productos", "Días", "Estado" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(
                            new Object[] { r.get("numero_traspaso"), r.get("fecha_solicitud"), r.get("bodega_origen"),
                                    r.get("bodega_destino"), r.get("usuario_solicita"), r.get("total_productos"),
                                    r.get("dias_transcurridos"), r.get("estado") });
                }
            }
            case "Flujo entre Bodegas" -> {
                List<Map<String, Object>> datos = service.getFlujoBodegas(dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(
                        new String[] { "Bodega Origen", "Bodega Destino", "Total Traspasos", "Total Productos" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("bodega_origen"), r.get("bodega_destino"),
                            r.get("total_traspasos"), r.get("total_productos") });
                }
            }
            default -> {
                List<Map<String, Object>> datos = service.getTraspasosPorPeriodo(dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "#Traspaso", "Fecha", "Origen", "Destino", "Solicita",
                        "Recibe", "Productos", "Estado" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(
                            new Object[] { r.get("numero_traspaso"), r.get("fecha_solicitud"), r.get("bodega_origen"),
                                    r.get("bodega_destino"), r.get("usuario_solicita"), r.get("usuario_recibe"),
                                    r.get("total_productos"), r.get("estado") });
                }
            }
        }
        lblTotal.setText("Registros: " + modelo.getRowCount());
    }
}

