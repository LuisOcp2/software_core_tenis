package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import raven.clases.productos.ServiceProduct;
import raven.controlador.principal.conexion;
import raven.toast.Notifications;

/**
 * Panel mejorado para visualizar el historial de cambios de un producto.
 * Incluye mapeo de IDs a nombres y filtros.
 */
public class HistorialCambiosPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private final ServiceProduct service;

    // Filtros
    private JTextField txtBuscar;
    private JComboBox<String> cbxFiltrarCampo;
    private TableRowSorter<DefaultTableModel> sorter;

    // Mapas para caché de nombres
    private Map<String, String> marcasMap = new HashMap<>();
    private Map<String, String> categoriasMap = new HashMap<>();
    private Map<String, String> proveedoresMap = new HashMap<>();
    private Map<String, String> bodegasMap = new HashMap<>();

    private List<Object[]> rawHistory = new ArrayList<>();
    private boolean mapsLoaded = false;

    public HistorialCambiosPanel() {
        service = new ServiceProduct();
        initUI();
    }

    private void initUI() {
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][grow]"));

        // Header Panel (Filtros y Acciones)
        JPanel header = new JPanel(new MigLayout("insets 5, fillx", "[]10[]push[]", "[]"));
        header.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        // Buscador
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:15;");

        // Filtro por campo
        cbxFiltrarCampo = new JComboBox<>(
                new String[] { "Todos", "Nombre", "Precio", "Marca", "Categoría", "Proveedor", "Ubicación", "Stock" });

        JButton btnExportar = new JButton("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCSV());

        header.add(new JLabel("Buscar:"));
        header.add(txtBuscar, "w 200!");
        header.add(new JLabel("Filtrar Campo:"));
        header.add(cbxFiltrarCampo, "w 150!");
        header.add(btnExportar);

        add(header, "wrap");

        // Tabla setup
        String[] columns = { "Fecha/Hora", "Usuario", "Campo Modificado", "Valor Anterior", "Valor Nuevo" };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(35);
        table.setFont(table.getFont().deriveFont(13f));

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);

        // Sorting & Filtering
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Listeners para filtros
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }
        });
        cbxFiltrarCampo.addActionListener(e -> applyFilters());

        JScrollPane scroll = new JScrollPane(table);
        scroll.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");

        add(scroll, "grow");
    }

    private void applyFilters() {
        String texto = txtBuscar.getText().trim();
        String campo = (String) cbxFiltrarCampo.getSelectedItem();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // Text Filter
        if (!texto.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + texto));
        }

        // Field Filter
        if (campo != null && !"Todos".equals(campo)) {
            // "Nombre", "Precio" in combo might match "Nombre" in table?
            // In DB it is "Nombre", "Precio Compra" etc.
            // Let's match loosely
            filters.add(RowFilter.regexFilter("(?i)" + campo, 2)); // Column 2 = Campo
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private void loadMaps() {
        if (mapsLoaded)
            return;

        try (Connection con = conexion.getInstance().getConnection()) {
            // Marcas
            try (PreparedStatement ps = con.prepareStatement("SELECT id_marca, nombre FROM marcas");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    marcasMap.put(String.valueOf(rs.getInt(1)), rs.getString(2));
            }
            // Categorias
            try (PreparedStatement ps = con.prepareStatement("SELECT id_categoria, nombre FROM categorias");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    categoriasMap.put(String.valueOf(rs.getInt(1)), rs.getString(2));
            }
            // Proveedores
            try (PreparedStatement ps = con.prepareStatement("SELECT id_proveedor, nombre FROM proveedores");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    proveedoresMap.put(String.valueOf(rs.getInt(1)), rs.getString(2));
            }
            // Bodegas
            try (PreparedStatement ps = con.prepareStatement("SELECT id_bodega, nombre FROM bodegas");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    bodegasMap.put(String.valueOf(rs.getInt(1)), rs.getString(2));
            }
            mapsLoaded = true;
        } catch (Exception e) {
            System.err.println("Error cargando mapas: " + e.getMessage());
        }
    }

    // Método helper para traducir valores
    private String resolverValor(String campo, String valorRaw) {
        if (valorRaw == null || valorRaw.equals("0") || valorRaw.isEmpty())
            return " - ";

        try {
            if (campo.toLowerCase().contains("marca") && marcasMap.containsKey(valorRaw)) {
                return marcasMap.get(valorRaw);
            }
            if (campo.toLowerCase().contains("categor") && categoriasMap.containsKey(valorRaw)) {
                return categoriasMap.get(valorRaw);
            }
            if (campo.toLowerCase().contains("proveedor") && proveedoresMap.containsKey(valorRaw)) {
                return proveedoresMap.get(valorRaw);
            }
            if (campo.toLowerCase().contains("ubicaci") && bodegasMap.containsKey(valorRaw)) {
                return bodegasMap.get(valorRaw);
            }
        } catch (Exception e) {
            // Fallback
        }
        return valorRaw;
    }

    public void loadHistory(int productId) {
        model.setRowCount(0);
        rawHistory.clear();

        // Mostrar cargando
        model.addRow(new Object[] { "Cargando...", "", "", "", "" });
        table.setEnabled(false);

        new Thread(() -> {
            try {
                loadMaps();
                List<Object[]> history = service.getHistorialProducto(productId);

                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0);
                    table.setEnabled(true);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    if (history.isEmpty()) {
                        model.addRow(new Object[] { "", "", "Sin cambios registrados", "", "" });
                    } else {
                        for (Object[] row : history) {
                            String fecha = (row[0] != null) ? sdf.format(row[0]) : "";
                            String usuario = (String) row[1];
                            String campo = (String) row[2];
                            String valAntRaw = (String) row[3];
                            String valNueRaw = (String) row[4];

                            // Traducir valores
                            String valAnt = resolverValor(campo, valAntRaw);
                            String valNue = resolverValor(campo, valNueRaw);

                            Object[] displayRow = new Object[] { fecha, usuario, campo, valAnt, valNue };
                            model.addRow(displayRow);
                            rawHistory.add(displayRow);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0);
                    model.addRow(new Object[] { "Error", "", e.getMessage(), "", "" });
                    table.setEnabled(true);
                });
            }
        }).start();
    }

    private void exportarCSV() {
        if (table.getRowCount() == 0)
            return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Historial como CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }

            try (FileWriter fw = new FileWriter(file)) {
                // BOM for Excel
                fw.write("\ufeff");
                fw.write("Fecha,Usuario,Campo,Valor Anterior,Valor Nuevo\n");

                for (int i = 0; i < table.getRowCount(); i++) {
                    for (int j = 0; j < table.getColumnCount(); j++) {
                        Object val = table.getValueAt(i, j);
                        String str = (val == null) ? "" : val.toString().replace("\"", "\"\"");
                        fw.write("\"" + str + "\"");
                        if (j < table.getColumnCount() - 1)
                            fw.write(",");
                    }
                    fw.write("\n");
                }

                Notifications.getInstance().show(Notifications.Type.SUCCESS, "Historial exportado exitosamente");

                // Intentar abrir el archivo
                java.awt.Desktop.getDesktop().open(file);

            } catch (Exception e) {
                Notifications.getInstance().show(Notifications.Type.ERROR, "Error al exportar: " + e.getMessage());
            }
        }
    }
}
