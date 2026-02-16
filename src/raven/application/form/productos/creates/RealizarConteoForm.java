package raven.application.form.productos.creates;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import raven.controlador.inventario.InventarioController;
import raven.clases.admin.UserSession;
import raven.modelos.DetalleConteoInventario;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Formulario optimizado para realizar conteo de inventario.
 * Soporta edición en línea, filtros rápidos y estadísticas en tiempo real.
 */
public class RealizarConteoForm extends JPanel {

    private final int idConteo;
    private final boolean esCajas;
    private final InventarioController controller;
    private JTable tablaProductos;
    private DefaultTableModel modeloTabla;

    // UI Components
    private JTextField txtBuscar;
    private JProgressBar progressConteo;
    private JLabel lblTotal;
    private JLabel lblContados;
    private JLabel lblPendientes;
    private JLabel lblTitulo;

    // Data Stats
    private int totalItems = 0;
    private int countedItems = 0;
    private int totalUnits = 0;
    private int countedUnits = 0;

    // UI Stats Extensions
    private JLabel lblTotalUnidades;
    private JLabel lblContadosUnidades;

    public RealizarConteoForm(int idConteo, boolean esCajas) {
        this.idConteo = idConteo;
        this.esCajas = esCajas;
        this.controller = new InventarioController();

        initComponents();
        setPreferredSize(new Dimension(1200, 750));

        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- TOP PANEL: Title & Stats ---
        JPanel topPanel = new JPanel(new BorderLayout(20, 15));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lblTitulo = new JLabel("Conteo de " + (esCajas ? "Cajas" : "Pares"));
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +12");
        headerPanel.add(lblTitulo);

        // Stats Container
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));

        // Item Counts
        lblTotal = createStatLabel("Total Items", "0", "State.info");
        lblContados = createStatLabel("Contados", "0", "State.success");
        lblPendientes = createStatLabel("Pendientes", "0", "State.warning");

        // Unit Counts (New)
        lblTotalUnidades = createStatLabel("Total Unidades", "0", "State.info");
        lblContadosUnidades = createStatLabel("Unid. Contadas", "0", "State.success");

        statsPanel.add(lblTotal);
        statsPanel.add(lblContados);
        statsPanel.add(lblPendientes);
        statsPanel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));
        statsPanel.add(lblTotalUnidades);
        statsPanel.add(lblContadosUnidades);

        topPanel.add(headerPanel, BorderLayout.WEST);
        topPanel.add(statsPanel, BorderLayout.EAST);

        // Progress Bar
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressConteo = new JProgressBar(0, 100);
        progressConteo.setStringPainted(true);
        progressConteo.putClientProperty(FlatClientProperties.STYLE, "arc:15");
        progressConteo.setPreferredSize(new Dimension(progressConteo.getPreferredSize().width, 10));
        progressPanel.add(progressConteo, BorderLayout.CENTER);

        topPanel.add(progressPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER PANEL: Toolbar & Table ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());

        // Search
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Escanear código o buscar...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:15;");
        Color searchIconColor = com.formdev.flatlaf.FlatLaf.isLafDark() ? new Color(200, 200, 200)
                : new Color(100, 100, 100);
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                FontIcon.of(FontAwesomeSolid.SEARCH, 18, searchIconColor));
        txtBuscar.setPreferredSize(new Dimension(350, 40));
        txtBuscar.addActionListener(e -> buscarProductos(txtBuscar.getText().trim()));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (txtBuscar.getText().trim().isEmpty()) {
                    buscarProductos("");
                }
            }
        });

        // Quick Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

        JButton btnShowAll = new JButton("Ver Todo");
        btnShowAll.putClientProperty(FlatClientProperties.STYLE,
                "arc:10; [light]background:shade(@background,5%); [dark]background:lighten(@background,5%)");

        JButton btnShowDiff = new JButton("Ver Diferencias");
        btnShowDiff.putClientProperty(FlatClientProperties.STYLE,
                "arc:10; [light]background:shade(@background,5%); [dark]background:lighten(@background,5%)");

        JButton btnSetSystem = new JButton("Igualar Todo al Sistema");
        btnSetSystem.setToolTipText("Establecer el conteo igual al sistema para todos los items filtrados");
        btnSetSystem.putClientProperty(FlatClientProperties.STYLE,
                "arc:10; [light]background:shade(@background,5%); [dark]background:lighten(@background,5%)");

        // NEW: Print/PDF Actions
        JButton btnPrint = new JButton();
        btnPrint.putClientProperty(FlatClientProperties.STYLE, "arc:10; buttonType:toolBarButton;");
        // Using FontAwesomeSolid
        Color iconColor = com.formdev.flatlaf.FlatLaf.isLafDark() ? new Color(230, 230, 230) : new Color(50, 50, 50);
        btnPrint.setIcon(FontIcon.of(FontAwesomeSolid.PRINT, 20, iconColor));
        btnPrint.setToolTipText("Imprimir Conteo");
        btnPrint.addActionListener(e -> imprimirConteo());

        JButton btnPdf = new JButton();
        btnPdf.putClientProperty(FlatClientProperties.STYLE, "arc:10; buttonType:toolBarButton;");
        btnPdf.setIcon(FontIcon.of(FontAwesomeSolid.FILE_PDF, 20, iconColor));
        btnPdf.setToolTipText("Exportar a PDF");
        btnPdf.addActionListener(e -> exportarPdf());

        btnShowAll.addActionListener(e -> buscarProductos(""));
        btnShowDiff.addActionListener(e -> filtrarDiferencias());
        btnSetSystem.addActionListener(e -> accionIgualarSistema());

        actionsPanel.add(txtBuscar);
        actionsPanel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));
        actionsPanel.add(btnShowAll);
        actionsPanel.add(btnShowDiff);
        actionsPanel.add(btnSetSystem);
        actionsPanel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));
        actionsPanel.add(btnPrint);
        actionsPanel.add(btnPdf);

        toolbar.add(actionsPanel, BorderLayout.CENTER);
        centerPanel.add(toolbar, BorderLayout.NORTH);

        // Table
        initTable();
        JScrollPane scrollPane = new JScrollPane(tablaProductos);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM PANEL: Footer Actions ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnFinalizar = new JButton("Finalizar Conteo");
        btnFinalizar.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +2; [light]background:#28a745; [dark]background:#218838; foreground:#FFF; arc:15");
        btnFinalizar.setPreferredSize(new Dimension(200, 45));
        btnFinalizar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnFinalizar.addActionListener(e -> finalizarConteo());

        bottomPanel.add(btnFinalizar);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createStatLabel(String title, String value, String stateKey) {
        JLabel lbl = new JLabel("<html><div style='text-align: center; padding: 2px 10px;'>"
                + "<span style='color: #888888; font-size: 10px;'>" + title.toUpperCase() + "</span><br>"
                + "<span style='font-size: 18px; font-weight: bold;'>" + value + "</span>"
                + "</div></html>");
        lbl.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; background:if($internal.isDark, lighten(@background,3%), shade(@background,2%))");
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        lbl.setOpaque(true);
        return lbl;
    }

    private void initTable() {
        String[] columns = {
                "ID", "Código EAN", "Producto", "Talla", "Color", "Estantería", "Stock Sistema", "Stock Contado",
                "Diferencia", "Estado"
        };

        modeloTabla = new DefaultTableModel(null, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Solo editable la columna "Stock Contado" (ahora es id 7)
                return column == 7;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Stock Sistema (6), Stock Contado (7), Diferencia (8) son enteros
                if (columnIndex == 6 || columnIndex == 7 || columnIndex == 8)
                    return Integer.class;
                return String.class;
            }
        };

        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setRowHeight(35);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProductos.getTableHeader().setReorderingAllowed(false);

        // Hide ID column
        TableColumn idCol = tablaProductos.getColumnModel().getColumn(0);
        idCol.setMinWidth(0);
        idCol.setMaxWidth(0);
        idCol.setWidth(0);

        // Ajustar anchos
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(120); // EAN
        tablaProductos.getColumnModel().getColumn(2).setPreferredWidth(250); // Producto
        tablaProductos.getColumnModel().getColumn(3).setPreferredWidth(80); // Talla
        tablaProductos.getColumnModel().getColumn(4).setPreferredWidth(100); // Color
        tablaProductos.getColumnModel().getColumn(5).setPreferredWidth(100); // Ubicación

        // Custom Editor for "Stock Contado" (Instant Save)
        JTextField editField = new JTextField();
        editField.setHorizontalAlignment(JTextField.CENTER);
        editField.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        DefaultCellEditor editor = new DefaultCellEditor(editField) {
            @Override
            public boolean stopCellEditing() {
                boolean stopped = super.stopCellEditing();
                if (stopped) {
                    guardarEdicionActual();
                }
                return stopped;
            }
        };
        editField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (tablaProductos.isEditing()) {
                    tablaProductos.getCellEditor().stopCellEditing();
                }
            }
        });

        tablaProductos.getColumnModel().getColumn(7).setCellEditor(editor);

        // Navigation Keys (Enter -> Next Row)
        tablaProductos.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        tablaProductos.getActionMap().put("Enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tablaProductos.getSelectedRow();
                int col = tablaProductos.getSelectedColumn();
                if (tablaProductos.isEditing()) {
                    tablaProductos.getCellEditor().stopCellEditing();
                }

                // Move selection down
                if (row < tablaProductos.getRowCount() - 1) {
                    tablaProductos.changeSelection(row + 1, col, false, false);
                }
            }
        });

        // Custom Renderer (Color Coding)
        tablaProductos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    try {
                        String estado = (String) table.getValueAt(row, 9); // Estado es col 9
                        int diferencia = (Integer) table.getValueAt(row, 8); // Diferencia es col 8

                        if ("contado".equals(estado)) {
                            if (diferencia == 0) {
                                c.setBackground(Color.decode("#DCEDC8").darker()); // Subtle Green
                                if (com.formdev.flatlaf.FlatLaf.isLafDark())
                                    c.setBackground(new Color(40, 60, 40));
                            } else {
                                c.setBackground(Color.decode("#FFCDD2").darker()); // Subtle Red
                                if (com.formdev.flatlaf.FlatLaf.isLafDark())
                                    c.setBackground(new Color(70, 40, 40));
                            }
                        } else {
                            c.setBackground(table.getBackground());
                        }
                    } catch (Exception e) {
                        c.setBackground(table.getBackground());
                    }
                }

                if (column == 8) { // Diferencia column
                    int val = 0;
                    if (value instanceof Integer)
                        val = (Integer) value;

                    if (val > 0)
                        setForeground(new Color(40, 167, 69)); // Bootstrap Success
                    else if (val < 0)
                        setForeground(new Color(220, 53, 69)); // Bootstrap Danger
                    else
                        setForeground(table.getForeground());

                    setHorizontalAlignment(CENTER);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (column >= 6 && column <= 7) { // Stock Sist (6) y Contado (7)
                    setHorizontalAlignment(CENTER);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(table.getForeground());
                    setHorizontalAlignment(column == 3 || column == 4 ? CENTER : LEFT); // Talla y Color centered
                    setFont(getFont().deriveFont(Font.PLAIN));
                }

                return c;
            }
        });
    }

    // --- LOGIC ---

    private void cargarDatos() {
        buscarProductos("");
    }

    private void buscarProductos(String query) {
        try {
            List<DetalleConteoInventario> lista = controller.buscarDetallesConteo(idConteo, query);
            modeloTabla.setRowCount(0);

            for (DetalleConteoInventario d : lista) {
                Object[] row = {
                        d.getId(),
                        d.getProducto().getBarcode(),
                        d.getProducto().getName(),
                        d.getProducto().getSize(), // Talla
                        d.getProducto().getColor(), // Color
                        d.getProducto().getUbicacion(),
                        d.getStockSistema(),
                        d.getStockContado(),
                        d.getDiferencia(),
                        d.getEstado()
                };
                modeloTabla.addRow(row);
            }

            actualizarStats(lista);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos: " + e.getMessage());
        }
    }

    private void filtrarDiferencias() {
        try {
            List<DetalleConteoInventario> lista = controller.obtenerDetallesConDiferencias(idConteo);
            modeloTabla.setRowCount(0);
            for (DetalleConteoInventario d : lista) {
                Object[] row = {
                        d.getId(),
                        d.getProducto().getBarcode(),
                        d.getProducto().getName(),
                        d.getProducto().getSize(),
                        d.getProducto().getColor(),
                        d.getProducto().getUbicacion(),
                        d.getStockSistema(),
                        d.getStockContado(),
                        d.getDiferencia(),
                        d.getEstado()
                };
                modeloTabla.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al filtrar: " + e.getMessage());
        }
    }

    private void actualizarStats(List<DetalleConteoInventario> lista) {
        totalItems = 0; // If filtering, this might change logic. Better to query complete list for
                        // stats?
                        // For UI simplicity, stats usually reflect "Total in List" or "Total in Conteo"
                        // Here let's query the full counts for global stats

        try {
            // Quick query to get global stats if we are filtering?
            // Ideally controller provides a summary object. For now, let's just calc from
            // current list if looking at all,
            // but if filtering, numbers are skewed.
            // Let's rely on list for now, assuming "Ver Todo" is default.

            totalItems = lista.size();
            countedItems = 0;
            totalUnits = 0;
            countedUnits = 0;

            for (DetalleConteoInventario d : lista) {
                totalUnits += d.getStockSistema();

                if (!d.getEstado().equals("pendiente")) {
                    countedItems++;
                    countedUnits += (d.getStockContado() != null ? d.getStockContado() : 0);
                }
            }

            updateStatsUI();

        } catch (Exception e) {
        }
    }

    private void updateStatsUI() {
        lblTotal.setText(
                "<html><div style='text-align: center;'><span style='color: #888888; font-size: 10px;'>TOTAL ITEMS</span><br>"
                        + "<span style='font-size: 18px; font-weight: bold;'>" + totalItems + "</span></div></html>");
        lblContados.setText(
                "<html><div style='text-align: center;'><span style='color: #28a745; font-size: 10px;'>ITEMS CONTADOS</span><br>"
                        + "<span style='font-size: 18px; font-weight: bold;'>" + countedItems + "</span></div></html>");
        lblPendientes.setText(
                "<html><div style='text-align: center;'><span style='color: #ffc107; font-size: 10px;'>PENDIENTES</span><br>"
                        + "<span style='font-size: 18px; font-weight: bold;'>" + (totalItems - countedItems)
                        + "</span></div></html>");

        lblTotalUnidades.setText(
                "<html><div style='text-align: center;'><span style='color: #888888; font-size: 10px;'>TOTAL UNIDADES</span><br>"
                        + "<span style='font-size: 18px; font-weight: bold;'>" + totalUnits + "</span></div></html>");
        lblContadosUnidades.setText(
                "<html><div style='text-align: center;'><span style='color: #007bff; font-size: 10px;'>UNID. CONTADAS</span><br>"
                        + "<span style='font-size: 18px; font-weight: bold;'>" + countedUnits + "</span></div></html>");

        int progress = (totalItems == 0) ? 0 : (int) ((double) countedItems / totalItems * 100);
        progressConteo.setValue(progress);

        if (progress == 100)
            progressConteo.setForeground(new Color(40, 167, 69));
        else
            progressConteo.setForeground(new Color(0, 123, 255));
    }

    private void guardarEdicionActual() {
        int row = tablaProductos.getSelectedRow();
        if (row == -1)
            return;

        try {
            int idDetalle = (Integer) modeloTabla.getValueAt(row, 0);
            int stockSistema = (Integer) modeloTabla.getValueAt(row, 6); // Stock Sistema es ahora col 6

            // Validate Input
            Object val = modeloTabla.getValueAt(row, 7); // Stock Contado es col 7
            int nuevoConteo = 0;
            if (val instanceof String)
                nuevoConteo = Integer.parseInt((String) val);
            else if (val instanceof Integer)
                nuevoConteo = (Integer) val;

            if (nuevoConteo < 0) {
                modeloTabla.setValueAt(0, row, 7); // Reset col 7
                return;
            }

            // Save to DB
            int usuarioId = UserSession.getInstance().getCurrentUser().getIdUsuario();
            System.out.println("DEBUG: Guardando conteo. idDetalle=" + idDetalle + ", nuevoConteo=" + nuevoConteo
                    + ", usuarioId=" + usuarioId);
            boolean ok = controller.registrarConteoProducto(idDetalle, nuevoConteo, usuarioId);

            if (ok) {
                // Update Local Model
                int diferencia = nuevoConteo - stockSistema;
                modeloTabla.setValueAt(diferencia, row, 8); // Diferencia es col 8
                modeloTabla.setValueAt("contado", row, 9); // Estado es col 9

                // Refresh Stats (Simple Increment)
                // Better to reload or smarter increment. Let's recalc visible.
                int counted = 0;
                int countedU = 0;
                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    if ("contado".equals(modeloTabla.getValueAt(i, 9))) {
                        counted++;
                        Object valContador = modeloTabla.getValueAt(i, 7);
                        if (valContador instanceof Integer) {
                            countedU += (Integer) valContador;
                        } else if (valContador instanceof String) {
                            try {
                                countedU += Integer.parseInt((String) valContador);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                countedItems = counted;
                countedUnits = countedU;
                updateStatsUI();

            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar cambio.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un número válido.");
        }
    }

    private void accionIgualarSistema() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de establecer el 'Stock Contado' igual al 'Stock Sistema' \npara TODOS los productos visibles en la tabla?",
                "Confirmar Acción Masiva", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        int usuarioId = UserSession.getInstance().getCurrentUser().getIdUsuario();
        int updated = 0;

        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String estado = (String) modeloTabla.getValueAt(i, 9); // Estado col 9
            if (estado.equals("pendiente")) {
                int idDetalle = (Integer) modeloTabla.getValueAt(i, 0);
                int stockSistema = (Integer) modeloTabla.getValueAt(i, 6); // Stock Sis col 6

                if (controller.registrarConteoProducto(idDetalle, stockSistema, usuarioId)) {
                    modeloTabla.setValueAt(stockSistema, i, 7); // update stock contado col 7
                    modeloTabla.setValueAt(0, i, 8); // Dif 0 col 8
                    modeloTabla.setValueAt("contado", i, 9); // Estado col 9
                    updated++;
                }
            }
        }

        JOptionPane.showMessageDialog(this, "Se actualizaron " + updated + " productos.");
        // Refetch to be safe?
        buscarProductos(txtBuscar.getText());
    }

    private void finalizarConteo() {
        // Validation similar to original...
        boolean hayPendientes = false;
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            if ("pendiente".equals(modeloTabla.getValueAt(i, 9))) { // Estado col 9
                hayPendientes = true;
                break;
            }
        }

        if (hayPendientes) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Hay productos pendientes. ¿Finalizar de todos modos?",
                    "Advertencia", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        }

        int usuarioId = UserSession.getInstance().getCurrentUser().getIdUsuario();
        if (controller.cerrarConteo(idConteo, usuarioId)) {
            JOptionPane.showMessageDialog(this, "Conteo Finalizado Exitosamente");
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error al finalizar conteo.");
        }
    }

    private void imprimirConteo() {
        String[] opciones = { "Ticket POS (80mm)", "Reporte Carta (PDF)", "Cancelar" };
        int seleccion = JOptionPane.showOptionDialog(this, "Seleccione formato de impresión:", "Imprimir Conteo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        if (seleccion == 0) {
            // POS
            raven.clases.inventario.ImpresionPOSInventario printer = new raven.clases.inventario.ImpresionPOSInventario();
            printer.imprimirConteo(idConteo);
        } else if (seleccion == 1) {
            // PDF
            exportarPdf();
        }
    }

    private void exportarPdf() {
        raven.clases.inventario.GeneradorPDFInventario pdfGen = new raven.clases.inventario.GeneradorPDFInventario();
        pdfGen.generarPDF(idConteo, this);
    }
}
