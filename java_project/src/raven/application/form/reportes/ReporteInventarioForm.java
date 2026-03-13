package raven.application.form.reportes;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.admin.UserSession;
import raven.clases.reportes.ServiceReporteInventario;
import raven.utils.ExportadorReportes;
import raven.datetime.component.date.DatePicker;

public class ReporteInventarioForm extends JPanel {

    private ServiceReporteInventario service = new ServiceReporteInventario();
    private JComboBox<String> cmbBodega;
    private JComboBox<String> cmbTipoReporte;
    private JSpinner spnUmbral;
    private DatePicker dpInicio, dpFin;
    private JFormattedTextField txtFechaInicio, txtFechaFin;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private List<Map<String, Object>> bodegas;
    private boolean esAdmin;
    private Integer idBodegaUsuario;
    private final Map<Integer, Integer> rowVarianteMap = new ConcurrentHashMap<>();
    private JButton btnVerDetalles;

    public ReporteInventarioForm() {
        initComponents();
        cargarBodegas();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        putClientProperty(FlatClientProperties.STYLE, "border:15,15,15,15");

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitulo = new JLabel("Caja Reporte de Inventario");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        JButton btnVolver = new JButton("Volver");
        btnVolver.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_LEFT, 18, Color.decode("#969696")));
        btnVolver.putClientProperty(FlatClientProperties.STYLE, "iconTextGap:6");
        btnVolver.addActionListener(e -> raven.application.Application.showForm(new ReportesMainForm()));
        headerPanel.add(lblTitulo, BorderLayout.WEST);
        headerPanel.add(btnVolver, BorderLayout.EAST);
        headerPanel.add(btnVolver, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Filtros Container
        JPanel filtrosContainer = new JPanel(new BorderLayout());
        filtrosContainer.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:darken($Panel.background,3%)");
        filtrosContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de configuración (Fila 1)
        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtrosPanel.setOpaque(false);

        cmbTipoReporte = new JComboBox<>(
                new String[] { "Stock Actual", "Productos con Stock (>0)", "Stock Bajo", "Rotación", "Valorización",
                        "Sin Movimiento", "Conteos de Inventario" });
        cmbBodega = new JComboBox<>();
        cmbBodega.addItem("Todas las bodegas");
        spnUmbral = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));

        // Configurar DatePickers
        dpInicio = new DatePicker();
        dpFin = new DatePicker();

        txtFechaInicio = new JFormattedTextField();
        txtFechaFin = new JFormattedTextField();

        // Estilo para los campos de fecha
        String dateFieldStyle = "arc:10;borderWidth:1;borderColor:#D0D0D0";
        txtFechaInicio.putClientProperty(FlatClientProperties.STYLE, dateFieldStyle);
        txtFechaFin.putClientProperty(FlatClientProperties.STYLE, dateFieldStyle);
        txtFechaInicio.setPreferredSize(new Dimension(120, 25)); // Tamaño similar al combo
        txtFechaFin.setPreferredSize(new Dimension(120, 25));

        dpInicio.setEditor(txtFechaInicio);
        dpFin.setEditor(txtFechaFin);
        dpInicio.setCloseAfterSelected(true);
        dpFin.setCloseAfterSelected(true);

        dpInicio.setSelectedDate(LocalDate.now().minusMonths(1));
        dpFin.setSelectedDate(LocalDate.now());

        filtrosPanel.add(new JLabel("Tipo:"));
        filtrosPanel.add(cmbTipoReporte);
        filtrosPanel.add(new JLabel("Bodega:"));
        filtrosPanel.add(cmbBodega);
        filtrosPanel.add(new JLabel("Umbral:"));
        filtrosPanel.add(spnUmbral);
        filtrosPanel.add(new JLabel("Desde:"));
        filtrosPanel.add(txtFechaInicio);
        filtrosPanel.add(new JLabel("Hasta:"));
        filtrosPanel.add(txtFechaFin);

        // Panel de acciones (Fila 2)
        JPanel accionesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        accionesPanel.setOpaque(false);

        JButton btnGenerar = new JButton("Generar Reporte");
        btnGenerar.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 18, Color.WHITE));
        btnGenerar.putClientProperty(FlatClientProperties.STYLE,
                "background:$Component.accentColor;foreground:#fff;font:bold");
        btnGenerar.addActionListener(e -> generarReporte());

        JButton btnExportarExcel = new JButton("Excel");
        btnExportarExcel.setIcon(FontIcon.of(FontAwesomeSolid.FILE_EXCEL, 18, Color.WHITE));
        btnExportarExcel.putClientProperty(FlatClientProperties.STYLE, "background:#217346;foreground:#fff;font:bold");
        btnExportarExcel.addActionListener(e -> ExportadorReportes.exportarExcel(tabla, "reporte_inventario"));

        JButton btnExportarPDF = new JButton("PDF");
        btnExportarPDF.setIcon(FontIcon.of(FontAwesomeSolid.FILE_PDF, 18, Color.WHITE));
        btnExportarPDF.putClientProperty(FlatClientProperties.STYLE, "background:#c81e1e;foreground:#fff;font:bold");
        btnExportarPDF.addActionListener(e -> ExportadorReportes.exportarPDF(tabla, "Reporte de Inventario"));

        btnVerDetalles = new JButton("Ver Detalles");
        btnVerDetalles.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH_PLUS, 18, Color.decode("#969696")));
        btnVerDetalles.setEnabled(false);
        btnVerDetalles.addActionListener(e -> abrirDetalleConteo());

        accionesPanel.add(btnGenerar);
        accionesPanel.add(btnExportarExcel);
        accionesPanel.add(btnExportarPDF);
        accionesPanel.add(btnVerDetalles);

        filtrosContainer.add(filtrosPanel, BorderLayout.NORTH);
        filtrosContainer.add(accionesPanel, BorderLayout.SOUTH);

        // Tabla
        modelo = new DefaultTableModel(new String[] { "Producto", "SKU", "Marca", "Categoría", "Talla", "Color",
                "Stock Pares", "Stock Cajas", "Precio Compra", "Precio Venta" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;showVerticalLines:false;rowHeight:40");

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotones();
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && btnVerDetalles.isEnabled()) {
                    abrirDetalleConteo();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footerPanel.setOpaque(false);
        lblTotal = new JLabel("Total: 0 registros");
        lblTotal.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        footerPanel.add(lblTotal);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(filtrosContainer, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        centerPanel.add(footerPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void cargarBodegas() {
        bodegas = service.getBodegas();

        UserSession session = UserSession.getInstance();
        esAdmin = false;
        idBodegaUsuario = null;

        if (session != null && session.isLoggedIn()) {
            idBodegaUsuario = session.getIdBodegaUsuario();
            esAdmin = session.hasRole("admin")
                    || session.hasRole("administrador")
                    || session.hasRole("gerente");
        }

        cmbBodega.removeAllItems();

        if (esAdmin) {
            cmbBodega.addItem("Todas las bodegas");
            for (Map<String, Object> b : bodegas) {
                cmbBodega.addItem(b.get("nombre").toString());
            }
            cmbBodega.setEnabled(true);
        } else {
            if (idBodegaUsuario != null && idBodegaUsuario > 0) {
                Map<String, Object> bodegaUsuario = null;
                for (Map<String, Object> b : bodegas) {
                    Object idObj = b.get("id_bodega");
                    if (idObj instanceof Integer && ((Integer) idObj).intValue() == idBodegaUsuario.intValue()) {
                        bodegaUsuario = b;
                        break;
                    }
                }

                if (bodegaUsuario != null) {
                    bodegas.clear();
                    bodegas.add(bodegaUsuario);
                    cmbBodega.addItem(bodegaUsuario.get("nombre").toString());
                    cmbBodega.setSelectedIndex(0);
                    cmbBodega.setEnabled(false);
                } else {
                    cmbBodega.addItem("Todas las bodegas");
                    for (Map<String, Object> b : bodegas) {
                        cmbBodega.addItem(b.get("nombre").toString());
                    }
                    cmbBodega.setEnabled(true);
                }
            } else {
                cmbBodega.addItem("Todas las bodegas");
                for (Map<String, Object> b : bodegas) {
                    cmbBodega.addItem(b.get("nombre").toString());
                }
                cmbBodega.setEnabled(true);
            }
        }
    }

    private int getIdBodegaSeleccionada() {
        UserSession session = UserSession.getInstance();
        boolean usuarioAdmin = false;
        Integer idBodegaSesion = null;

        if (session != null && session.isLoggedIn()) {
            idBodegaSesion = session.getIdBodegaUsuario();
            usuarioAdmin = session.hasRole("admin")
                    || session.hasRole("administrador")
                    || session.hasRole("gerente");
        }

        if (!usuarioAdmin) {
            if (idBodegaSesion != null && idBodegaSesion > 0) {
                return idBodegaSesion;
            }
            return 0;
        }

        int idx = cmbBodega.getSelectedIndex();
        if (idx <= 0)
            return 0;
        return (int) bodegas.get(idx - 1).get("id_bodega");
    }

    private void actualizarEstadoBotones() {
        boolean esConteo = "Conteos de Inventario".equals(cmbTipoReporte.getSelectedItem());
        boolean filaSeleccionada = tabla.getSelectedRow() != -1;
        btnVerDetalles.setEnabled(esConteo && filaSeleccionada);
    }

    private void abrirDetalleConteo() {
        int row = tabla.getSelectedRow();
        if (row == -1)
            return;

        // Asumiendo que el ID está en la columna 0 y el nombre en la 1 para este
        // reporte
        Object idObj = tabla.getValueAt(row, 0);
        Object nombreObj = tabla.getValueAt(row, 1);

        if (idObj instanceof Integer) {
            int idConteo = (Integer) idObj;
            String nombre = nombreObj != null ? nombreObj.toString() : "Conteo " + idConteo;

            // Mostrar loading o cursor de espera
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingUtilities.invokeLater(() -> {
                try {
                    List<Map<String, Object>> detalles = service.getDetalleConteo(idConteo);

                    Window parent = SwingUtilities.getWindowAncestor(this);

                    DetalleConteoDialog dialog = new DetalleConteoDialog(parent, nombre, detalles);
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al mostrar detalles: " + ex.getMessage());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }
    }

    private void generarReporte() {
        try {
            modelo.setRowCount(0);
            rowVarianteMap.clear();
            int idBodega = getIdBodegaSeleccionada();
            String tipo = cmbTipoReporte.getSelectedItem().toString();
            List<Map<String, Object>> datos;

            switch (tipo) {
                case "Productos con Stock (>0)" -> {
                    datos = service.getProductosConStock(idBodega);
                    modelo.setColumnIdentifiers(
                            new String[] { "Producto", "SKU", "Marca", "Categoría", "Talla", "Color",
                                    "Stock Pares", "Stock Cajas", "P. Compra", "P. Venta" });
                    for (Map<String, Object> r : datos) {
                        int rowIndex = modelo.getRowCount();
                        modelo.addRow(
                                new Object[] { r.get("producto"), r.get("sku"), r.get("marca"), r.get("categoria"),
                                        r.get("talla"), r.get("color"), r.get("stock_pares"), r.get("stock_cajas"),
                                        formatMoney((BigDecimal) r.get("precio_compra")),
                                        formatMoney((BigDecimal) r.get("precio_venta")) });
                        Object idVarianteObj = r.get("id_variante");
                        if (idVarianteObj instanceof Integer) {
                            rowVarianteMap.put(rowIndex, (Integer) idVarianteObj);
                        }
                    }
                    tabla.getColumnModel().getColumn(0).setCellRenderer(new AsyncImageRenderer(service));
                    tabla.getColumnModel().getColumn(0).setPreferredWidth(220);
                }
                case "Stock Bajo" -> {
                    int umbral = (int) spnUmbral.getValue();
                    datos = service.getProductosStockBajo(idBodega, umbral);
                    modelo.setColumnIdentifiers(
                            new String[] { "Producto", "Marca", "Talla", "Color", "Bodega", "Stock", "Mínimo",
                                    "Estado" });
                    for (Map<String, Object> r : datos) {
                        modelo.addRow(new Object[] { r.get("producto"), r.get("marca"), r.get("talla"), r.get("color"),
                                r.get("bodega"), r.get("stock_total"), r.get("stock_minimo"), r.get("estado_stock") });
                    }
                }
                case "Rotación" -> {
                    LocalDate fechaInicio = dpInicio.isDateSelected() ? dpInicio.getSelectedDate() : null;
                    LocalDate fechaFin = dpFin.isDateSelected() ? dpFin.getSelectedDate() : null;
                    datos = service.getRotacionInventario(idBodega, fechaInicio, fechaFin);
                    modelo.setColumnIdentifiers(new String[] { "Producto", "Código", "Marca", "Vendidos", "Stock Prom.",
                            "Índice Rotación", "Días Inv." });
                    for (Map<String, Object> r : datos) {
                        modelo.addRow(new Object[] { r.get("producto"), r.get("codigo_modelo"), r.get("marca"),
                                r.get("unidades_vendidas"), r.get("stock_promedio"), r.get("indice_rotacion"),
                                r.get("dias_inventario") });
                    }
                }
                case "Valorización" -> {
                    Map<String, Object> val = service.getValorizacionInventario(idBodega);
                    modelo.setColumnIdentifiers(new String[] { "Métrica", "Valor" });
                    modelo.addRow(new Object[] { "Total Productos", val.get("total_productos") });
                    modelo.addRow(new Object[] { "Total Variantes", val.get("total_variantes") });
                    modelo.addRow(new Object[] { "Total Unidades", val.get("total_unidades") });
                    modelo.addRow(new Object[] { "Valor al Costo", formatMoney((BigDecimal) val.get("valor_costo")) });
                    modelo.addRow(new Object[] { "Valor de Venta", formatMoney((BigDecimal) val.get("valor_venta")) });
                    modelo.addRow(
                            new Object[] { "Margen Potencial", formatMoney((BigDecimal) val.get("margen_potencial")) });
                }
                case "Sin Movimiento" -> {
                    int dias = (int) spnUmbral.getValue();
                    datos = service.getProductosSinMovimiento(idBodega, dias);
                    modelo.setColumnIdentifiers(new String[] { "Producto", "Marca", "Talla", "Color", "Bodega", "Stock",
                            "Días Sin Mov.", "Valor Inmov." });
                    for (Map<String, Object> r : datos) {
                        modelo.addRow(new Object[] { r.get("producto"), r.get("marca"), r.get("talla"), r.get("color"),
                                r.get("bodega"), r.get("stock_total"), r.get("dias_sin_movimiento"),
                                formatMoney((BigDecimal) r.get("valor_inmovilizado")) });
                    }
                }
                case "Conteos de Inventario" -> {
                    LocalDate fechaInicio = dpInicio.isDateSelected() ? dpInicio.getSelectedDate() : null;
                    LocalDate fechaFin = dpFin.isDateSelected() ? dpFin.getSelectedDate() : null;
                    datos = service.getHistorialConteos(idBodega, fechaInicio, fechaFin);
                    modelo.setColumnIdentifiers(new String[] { "ID", "Nombre", "Fecha", "Tipo", "Estado", "Responsable",
                            "Total Prod.", "Contados", "Pendientes" });
                    for (Map<String, Object> r : datos) {
                        modelo.addRow(new Object[] {
                                r.get("id_conteo"),
                                r.get("nombre"),
                                r.get("fecha"),
                                r.get("tipo"),
                                r.get("estado"),
                                r.get("responsable"),
                                r.get("total_productos"),
                                r.get("productos_contados"),
                                r.get("pendientes")
                        });
                    }
                }
                default -> { // Stock Actual
                    datos = service.getStockPorBodega(idBodega);
                    modelo.setColumnIdentifiers(
                            new String[] { "Producto", "SKU", "Marca", "Categoría", "Talla", "Color",
                                    "Stock Pares", "Stock Cajas", "P. Compra", "P. Venta" });
                    for (Map<String, Object> r : datos) {
                        int rowIndex = modelo.getRowCount();
                        modelo.addRow(
                                new Object[] { r.get("producto"), r.get("sku"), r.get("marca"), r.get("categoria"),
                                        r.get("talla"), r.get("color"), r.get("stock_pares"), r.get("stock_cajas"),
                                        formatMoney((BigDecimal) r.get("precio_compra")),
                                        formatMoney((BigDecimal) r.get("precio_venta")) });
                        Object idVarianteObj = r.get("id_variante");
                        if (idVarianteObj instanceof Integer) {
                            rowVarianteMap.put(rowIndex, (Integer) idVarianteObj);
                        }
                    }
                    tabla.getColumnModel().getColumn(0).setCellRenderer(new AsyncImageRenderer(service));
                    tabla.getColumnModel().getColumn(0).setPreferredWidth(220);
                }
            }
            lblTotal.setText("Total: " + modelo.getRowCount() + " registros");
            actualizarEstadoBotones();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al generar reporte: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatMoney(BigDecimal val) {
        if (val == null)
            return "$0";
        return String.format("$%,.0f", val);
    }

    class AsyncImageRenderer extends DefaultTableCellRenderer {
        private final Map<Integer, ImageIcon> cache = new ConcurrentHashMap<>();
        private final Map<Integer, Boolean> loading = new ConcurrentHashMap<>();
        private final ServiceReporteInventario service;

        public AsyncImageRenderer(ServiceReporteInventario service) {
            this.service = service;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            label.setHorizontalAlignment(SwingConstants.LEFT);

            int modelRow = table.convertRowIndexToModel(row);
            Integer idVariante = rowVarianteMap.get(modelRow);
            if (idVariante == null) {
                return label;
            }

            ImageIcon icon = cache.get(idVariante);

            if (icon != null) {
                label.setIcon(icon);
            } else {
                label.setIcon(null);
                if (!loading.containsKey(idVariante)) {
                    loading.put(idVariante, true);
                    loadImagen(idVariante, table);
                }
            }

            return label;
        }

        private void loadImagen(int idVariante, JTable table) {
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    byte[] bytes = service.getImagenVariante(idVariante);
                    if (bytes != null && bytes.length > 0) {
                        ImageIcon ii = new ImageIcon(bytes);
                        // Scale to row height - 4 (margin)
                        int size = table.getRowHeight() - 4;
                        if (size < 1)
                            size = 20;
                        Image img = ii.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        return new ImageIcon(img);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            cache.put(idVariante, icon);
                        } else {
                            cache.put(idVariante, new ImageIcon());
                        }
                        table.repaint();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        loading.remove(idVariante);
                    }
                }
            }.execute();
        }
    }
}
