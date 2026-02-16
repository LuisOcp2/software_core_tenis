package raven.application.form.reportes;

import com.formdev.flatlaf.FlatClientProperties;
import com.github.lgooddatepicker.components.DatePicker;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import raven.clases.reportes.ServiceAuditoria;
import raven.utils.ExportadorReportes;

public class ReporteAuditoriaForm extends JPanel {

    private ServiceAuditoria service = new ServiceAuditoria();
    private JComboBox<String> cmbTipoReporte, cmbUsuario;
    private DatePicker dpInicio, dpFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private List<Map<String, Object>> usuarios;

    public ReporteAuditoriaForm() {
        initComponents();
        cargarUsuarios();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Buscar Auditoría del Sistema");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        JLabel lblWarning = new JLabel("WARNING  Solo Admin/Gerente");
        lblWarning.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.warning.focusedBorderColor");
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> raven.application.Application.showForm(new ReportesMainForm()));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(lblWarning);
        rightPanel.add(btnVolver);
        headerPanel.add(lblTitulo, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtrosPanel.putClientProperty(FlatClientProperties.STYLE, "arc:10;background:darken($Panel.background,3%)");
        filtrosPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbTipoReporte = new JComboBox<>(new String[] { "Trazabilidad Productos", "Historial Sesiones",
                "Discrepancias Conteo", "Por Proveedor" });
        cmbUsuario = new JComboBox<>();
        cmbUsuario.addItem("Todos");
        dpInicio = new DatePicker();
        dpInicio.setDate(LocalDate.now().minusDays(7));
        dpFin = new DatePicker();
        dpFin.setDate(LocalDate.now());
        JButton btnGenerar = new JButton("Generar");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff");
        btnGenerar.addActionListener(e -> generarReporte());
        JButton btnExcel = new JButton("Excel");
        btnExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_auditoria"));

        filtrosPanel.add(new JLabel("Vista:"));
        filtrosPanel.add(cmbTipoReporte);
        filtrosPanel.add(new JLabel("Usuario:"));
        filtrosPanel.add(cmbUsuario);
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

    private void cargarUsuarios() {
        usuarios = service.getUsuarios();
        for (Map<String, Object> u : usuarios) {
            cmbUsuario.addItem(u.get("nombre") + " (" + u.get("rol") + ")");
        }
    }

    private int getIdUsuarioSeleccionado() {
        int idx = cmbUsuario.getSelectedIndex();
        if (idx <= 0)
            return 0;
        return (int) usuarios.get(idx - 1).get("id_usuario");
    }

    private void generarReporte() {
        modelo.setRowCount(0);
        String tipo = cmbTipoReporte.getSelectedItem().toString();

        switch (tipo) {
            case "Historial Sesiones" -> {
                List<Map<String, Object>> datos = service.getHistorialSesiones(getIdUsuarioSeleccionado(),
                        dpInicio.getDate(), dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "ID", "Usuario", "Nombre", "Rol", "Acción", "Fecha", "IP" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("id_historial"), r.get("username"), r.get("nombre"),
                            r.get("rol"), r.get("accion"), r.get("fecha_accion"), r.get("ip_address") });
                }
            }
            case "Discrepancias Conteo" -> {
                List<Map<String, Object>> datos = service.getDiscrepanciasConteo();
                modelo.setColumnIdentifiers(new String[] { "Conteo", "Producto", "Stock Sistema", "Stock Contado",
                        "Diferencia", "Contador" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("conteo"), r.get("producto"), r.get("stock_sistema"),
                            r.get("stock_contado"), r.get("diferencia"), r.get("contador") });
                }
            }
            case "Por Proveedor" -> {
                List<Map<String, Object>> datos = service.getAuditoriaPorProveedor(0, dpInicio.getDate(),
                        dpFin.getDate());
                modelo.setColumnIdentifiers(
                        new String[] { "Proveedor", "Tipo Evento", "Num. Eventos", "Total Cantidad" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("proveedor"), r.get("tipo_evento"),
                            r.get("num_eventos"), r.get("total_cantidad") });
                }
            }
            default -> {
                List<Map<String, Object>> datos = service.getTrazabilidadProducto(0, dpInicio.getDate(),
                        dpFin.getDate());
                modelo.setColumnIdentifiers(new String[] { "ID", "SKU", "Producto", "Evento", "Cantidad", "Origen",
                        "Destino", "Usuario", "Fecha" });
                for (Map<String, Object> r : datos) {
                    modelo.addRow(new Object[] { r.get("id_auditoria"), r.get("sku"), r.get("producto"),
                            r.get("tipo_evento"), r.get("cantidad"), r.get("bodega_origen"),
                            r.get("bodega_destino"), r.get("usuario"), r.get("fecha_evento") });
                }
            }
        }
        lblTotal.setText("Registros: " + modelo.getRowCount());
    }
}

