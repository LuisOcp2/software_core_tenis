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
import raven.clases.reportes.ServiceReporteClientes;
import raven.utils.ExportadorReportes;
import raven.clases.admin.UserSession;

public class ReporteClientesForm extends JPanel {

    private ServiceReporteClientes service = new ServiceReporteClientes();
    private JComboBox<String> cmbTipoReporte;
    private JSpinner spnLimite, spnPuntos;
    private DatePicker dpInicio, dpFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;

    public ReporteClientesForm() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel(" Reporte de Clientes");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> raven.application.Application.showForm(new ReportesMainForm()));
        headerPanel.add(lblTitulo, BorderLayout.WEST);
        headerPanel.add(btnVolver, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtrosPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:darken($Panel.background,3%)");
        filtrosPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbTipoReporte = new JComboBox<>(new String[] { "Top Clientes", "Por Puntos", "Clientes Nuevos" });
        spnLimite = new JSpinner(new SpinnerNumberModel(20, 5, 100, 5));
        spnPuntos = new JSpinner(new SpinnerNumberModel(100, 0, 10000, 50));
        dpInicio = new DatePicker();
        dpInicio.setDate(LocalDate.now().minusMonths(3));
        dpFin = new DatePicker();
        dpFin.setDate(LocalDate.now());
        JButton btnGenerar = new JButton("Generar");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff");
        btnGenerar.addActionListener(e -> generarReporte());
        JButton btnExcel = new JButton("Excel");
        btnExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_clientes"));

        filtrosPanel.add(new JLabel("Vista:"));
        filtrosPanel.add(cmbTipoReporte);
        filtrosPanel.add(new JLabel("Límite:"));
        filtrosPanel.add(spnLimite);
        filtrosPanel.add(new JLabel("Min. Puntos:"));
        filtrosPanel.add(spnPuntos);
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

    private int getIdBodegaFiltro() {
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() == null) {
            return -1;
        }
        String rol = session.getCurrentUser().getRol();
        boolean esAdmin = rol != null && (rol.equalsIgnoreCase("admin")
                || rol.equalsIgnoreCase("administrador")
                || rol.toLowerCase().contains("admin"));
        if (esAdmin) {
            return 0;
        }
        Integer idBodega = session.getIdBodegaUsuario();
        return idBodega != null && idBodega > 0 ? idBodega : -1;
    }

    private void generarReporte() {
        modelo.setRowCount(0);
        String tipo = cmbTipoReporte.getSelectedItem().toString();
        int idBodegaFiltro = getIdBodegaFiltro();

        switch (tipo) {
            case "Por Puntos" -> {
                int minPuntos = (int) spnPuntos.getValue();
                List<Map<String, Object>> datos = service.getClientesPorPuntos(minPuntos, idBodegaFiltro);
                modelo.setColumnIdentifiers(new String[] { "ID", "Cliente", "DNI", "Teléfono", "Puntos" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("id_cliente"), r.get("nombre"), r.get("dni"),
                            r.get("telefono"), r.get("puntos_acumulados") });
                }
            }
            case "Clientes Nuevos" -> {
                List<Map<String, Object>> datos = service.getClientesNuevos(dpInicio.getDate(), dpFin.getDate(),
                        idBodegaFiltro);
                modelo.setColumnIdentifiers(new String[] { "ID", "Cliente", "DNI", "Teléfono", "Fecha Registro" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("id_cliente"), r.get("nombre"), r.get("dni"),
                            r.get("telefono"), r.get("fecha_registro") });
                }
            }
            default -> {
                int limite = (int) spnLimite.getValue();
                List<Map<String, Object>> datos = service.getTopClientes(limite, dpInicio.getDate(),
                        dpFin.getDate(), idBodegaFiltro);
                modelo.setColumnIdentifiers(
                        new String[] { "#", "Cliente", "DNI", "Puntos", "Compras", "Monto Total", "Ticket Prom." });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(
                            new Object[] { r.get("ranking"), r.get("nombre"), r.get("dni"), r.get("puntos_acumulados"),
                                    r.get("total_compras"), formatMoney((BigDecimal) r.get("monto_total")),
                                    formatMoney((BigDecimal) r.get("ticket_promedio")) });
                }
            }
        }
        lblTotal.setText("Registros: " + modelo.getRowCount());
    }

    private String formatMoney(BigDecimal val) {
        if (val == null)
            return "$0";
        return String.format("$%,.0f", val);
    }
}

