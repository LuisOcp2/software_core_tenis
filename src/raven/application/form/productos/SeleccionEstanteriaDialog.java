package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import raven.clases.inventario.ServiceInventarioBodega;
import raven.modal.Toast;

/**
 * Dialogo para seleccionar una estantería o ubicación en el inventario.
 */
public class SeleccionEstanteriaDialog extends JDialog {

    private String selectedUbicacion;
    private final ServiceInventarioBodega service = new ServiceInventarioBodega();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;

    public SeleccionEstanteriaDialog(Window owner) {
        super(owner, "Seleccionar Estantería", ModalityType.APPLICATION_MODAL);
        initComponents();
        loadData();
    }

    public String getSelectedUbicacion() {
        return selectedUbicacion;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 450);
        setLocationRelativeTo(getOwner());

        // Header
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        
        JLabel lbTitle = new JLabel("Seleccionar Estantería / Ubicación");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        
        txtSearch = new JTextField();
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar ubicación...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("raven/icon/svg/search.svg", 0.4f));
        txtSearch.setPreferredSize(new Dimension(200, 30));
        
        panelHeader.add(lbTitle, BorderLayout.WEST);
        panelHeader.add(txtSearch, BorderLayout.EAST);
        
        add(panelHeader, BorderLayout.NORTH);

        // Table
        String[] columns = {"Ubicación", "Total Pares", "Total Cajas"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(35);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultEditor(Object.class, null);
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = txtSearch.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    confirmSelection();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel panelFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelFooter.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        
        JButton btnOk = new JButton("Seleccionar");
        btnOk.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;foreground:#fff;font:bold");
        btnOk.addActionListener(e -> confirmSelection());
        
        panelFooter.add(btnCancel);
        panelFooter.add(btnOk);
        
        add(panelFooter, BorderLayout.SOUTH);
        
        // Ctrl+E shortcut to close (optional, but good UX) or Escape
        ((JComponent) getContentPane()).registerKeyboardAction(e -> dispose(), 
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<Object[]> data = service.listarUbicacionesAgrupadas();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Object[] row : data) {
                        tableModel.addRow(row);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    Toast.show(this, Toast.Type.ERROR, "Error al cargar ubicaciones: " + e.getMessage());
                });
            }
        }).start();
    }

    private void confirmSelection() {
        int row = table.getSelectedRow();
        if (row != -1) {
            selectedUbicacion = (String) table.getValueAt(table.convertRowIndexToModel(row), 0);
            dispose();
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una ubicación");
        }
    }
}
