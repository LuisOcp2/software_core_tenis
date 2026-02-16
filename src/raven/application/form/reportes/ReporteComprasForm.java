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
import raven.clases.reportes.ServiceReporteCompras;
import raven.utils.ExportadorReportes;

public class ReporteComprasForm extends JPanel {

    private ServiceReporteCompras service = new ServiceReporteCompras();
    private JComboBox<String> cmbProveedor, cmbTipoReporte;
    private DatePicker dpInicio, dpFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal, lblMonto;
    private List<Map<String, Object>> proveedores;

    public ReporteComprasForm() {
        initComponents();
        cargarProveedores();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Carrito Reporte de Compras");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> raven.application.Application.showForm(new ReportesMainForm()));
        headerPanel.add(lblTitulo, BorderLayout.WEST);
        headerPanel.add(btnVolver, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtrosPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:darken($Panel.background,3%)");
        filtrosPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbTipoReporte = new JComboBox<>(new String[] { "Compras Proveedores", "Compras Externas", "Top Proveedores" });
        cmbProveedor = new JComboBox<>();
        cmbProveedor.addItem("Todos");
        dpInicio = new DatePicker();
        dpInicio.setDate(LocalDate.now().minusMonths(1));
        dpFin = new DatePicker();
        dpFin.setDate(LocalDate.now());
        JButton btnGenerar = new JButton("Generar");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff");
        btnGenerar.addActionListener(e -> generarReporte());
        JButton btnExcel = new JButton("Excel");
        btnExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_compras"));

        filtrosPanel.add(new JLabel("Tipo:"));
        filtrosPanel.add(cmbTipoReporte);
        filtrosPanel.add(new JLabel("Proveedor:"));
        filtrosPanel.add(cmbProveedor);
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

    private void cargarProveedores() {
        proveedores = service.getProveedores();
        for (Map<String, Object> p : proveedores) {
            cmbProveedor.addItem(p.get("nombre").toString());
        }
    }

    private int getIdProveedorSeleccionado() {
        int idx = cmbProveedor.getSelectedIndex();
        if (idx <= 0)
            return 0;
        return (int) proveedores.get(idx - 1).get("id_proveedor");
    }

    private void generarReporte() {
        modelo.setRowCount(0);
        String tipo = cmbTipoReporte.getSelectedItem().toString();
        BigDecimal montoTotal = BigDecimal.ZERO;

        switch (tipo) {
            case "Compras Externas" -> {
                List<Map<String, Object>> datos = service.getComprasExternas(dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(
                        new String[] { "#Compra", "Tienda", "Factura", "Bodega", "Fecha", "Total", "Estado" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(
                            new Object[] { r.get("numero_compra"), r.get("tienda_proveedor"), r.get("numero_factura"),
                                    r.get("bodega"), r.get("fecha_compra"), formatMoney((BigDecimal) r.get("total")),
                                    r.get("estado") });
                    if (r.get("total") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("total"));
                }
            }
            case "Top Proveedores" -> {
                List<Map<String, Object>> datos = service.getTopProveedores(dpInicio.getDate(), dpFin.getDate(), 20);
                modelo.setColumnIdentifiers(new String[] { "Proveedor", "RUC", "Total Compras", "Monto Total",
                        "Promedio", "Última Compra" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("proveedor"), r.get("ruc"), r.get("total_compras"),
                            formatMoney((BigDecimal) r.get("monto_total")),
                            formatMoney((BigDecimal) r.get("promedio_compra")), r.get("ultima_compra") });
                    if (r.get("monto_total") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("monto_total"));
                }
            }
            default -> {
                List<Map<String, Object>> datos = service.getComprasPorProveedor(getIdProveedorSeleccionado(),
                        dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "ID", "Fecha", "Proveedor", "RUC", "Usuario", "Subtotal",
                        "IVA", "Total", "Estado" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(
                            new Object[] { r.get("id_compra"), r.get("fecha_compra"), r.get("proveedor"), r.get("ruc"),
                                    r.get("usuario"), formatMoney((BigDecimal) r.get("subtotal")),
                                    formatMoney((BigDecimal) r.get("iva")),
                                    formatMoney((BigDecimal) r.get("total")), r.get("estado") });
                    if (r.get("total") != null)
                        montoTotal = montoTotal.add((BigDecimal) r.get("total"));
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

