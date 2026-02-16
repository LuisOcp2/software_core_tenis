package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import raven.clases.productos.TraspasoService;
import raven.modal.Toast;

public class BuscadorTraspasoPanel extends JPanel {

    private JTextField txtBusqueda;
    private JButton btnBuscar;
    private JTable table;
    private TraspasoService traspasoService;

    public BuscadorTraspasoPanel() {
        traspasoService = new TraspasoService();
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));
        
        // Search Panel
        JPanel panelSearch = new JPanel(new MigLayout("fill, insets 0", "[grow]push[]", "[]"));
        txtBusqueda = new JTextField();
        txtBusqueda.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre, código de barras o modelo...");
        txtBusqueda.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    buscar();
                }
            }
        });
        
        btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        
        panelSearch.add(txtBusqueda, "growx");
        panelSearch.add(btnBuscar);
        
        add(panelSearch, "growx, wrap");
        
        // Table
        table = new JTable();
        table.setModel(new DefaultTableModel(
            new Object [][] {},
            new String [] {
                "N° Traspaso", "Fecha", "Origen", "Destino", "Producto", "EAN", "Cant."
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        
        add(new JScrollPane(table), "grow, push");
    }

    private void buscar() {
        String texto = txtBusqueda.getText().trim();
        if (texto.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "Ingrese un texto para buscar");
            return;
        }

        btnBuscar.setEnabled(false);
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return traspasoService.buscarTraspasosPorProducto(texto);
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> resultados = get();
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    model.setRowCount(0);
                    
                    if (resultados.isEmpty()) {
                        Toast.show(BuscadorTraspasoPanel.this, Toast.Type.INFO, "No se encontraron resultados");
                    } else {
                        for (Object[] row : resultados) {
                            model.addRow(row);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show(BuscadorTraspasoPanel.this, Toast.Type.ERROR, "Error al buscar: " + e.getMessage());
                } finally {
                    btnBuscar.setEnabled(true);
                }
            }
        }.execute();
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        txtBusqueda.requestFocusInWindow();
    }
}
