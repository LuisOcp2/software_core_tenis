package raven.application.form.other;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import raven.clases.comercial.ServiceCambioTalla;
import raven.clases.admin.UserSession;
import raven.modal.Toast;

public class ModalCambioTalla extends JDialog {

    private JTextField txtBuscar;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JComboBox<VarianteItem> comboVariantes;
    private JTextField txtObservaciones;
    private ServiceCambioTalla service;
    private JButton btnConfirmar;

    public ModalCambioTalla(Window parent, boolean modal) {
        super(parent, "Cambio Rápido de Talla", modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        service = new ServiceCambioTalla();
        initComponents();
        setSize(900, 600);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Panel Superior: Búsqueda
        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblBuscar = new JLabel("Buscar:");
        txtBuscar = new JTextField(30);
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Escanee código de barras, ingrese factura o traspaso");
        JButton btnBuscar = new JButton("Buscar");

        panelTop.add(lblBuscar);
        panelTop.add(txtBuscar);
        panelTop.add(btnBuscar);
        add(panelTop, BorderLayout.NORTH);

        // Panel Central: Tabla de Resultados
        String[] cols = {"Tipo", "Referencia", "Fecha", "Cliente/Bodega", "Producto", "Talla", "Cantidad", "ID Detalle", "ID Variante", "ID Producto"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ocultar columnas de IDs
        tabla.getColumnModel().getColumn(7).setMinWidth(0);
        tabla.getColumnModel().getColumn(7).setMaxWidth(0);
        tabla.getColumnModel().getColumn(7).setWidth(0);
        tabla.getColumnModel().getColumn(8).setMinWidth(0);
        tabla.getColumnModel().getColumn(8).setMaxWidth(0);
        tabla.getColumnModel().getColumn(8).setWidth(0);
        tabla.getColumnModel().getColumn(9).setMinWidth(0);
        tabla.getColumnModel().getColumn(9).setMaxWidth(0);
        tabla.getColumnModel().getColumn(9).setWidth(0);

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Panel Inferior: Selección de nueva talla y confirmación
        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblNuevaTalla = new JLabel("Nueva Talla:");
        comboVariantes = new JComboBox<>();
        JLabel lblObs = new JLabel("Observaciones:");
        txtObservaciones = new JTextField(20);
        btnConfirmar = new JButton("Confirmar Cambio");
        btnConfirmar.setEnabled(false);

        panelBottom.add(lblNuevaTalla);
        panelBottom.add(comboVariantes);
        panelBottom.add(lblObs);
        panelBottom.add(txtObservaciones);
        panelBottom.add(btnConfirmar);
        add(panelBottom, BorderLayout.SOUTH);

        // Listeners
        ActionListener buscarAction = e -> buscar();
        btnBuscar.addActionListener(buscarAction);
        txtBuscar.addActionListener(buscarAction);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarVariantesProducto();
            }
        });

        btnConfirmar.addActionListener(e -> confirmarCambio());
        
        // Atajo para cerrar con ESC
        getRootPane().registerKeyboardAction(e -> dispose(), 
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void buscar() {
        String criterio = txtBuscar.getText().trim();
        if (criterio.isEmpty()) return;

        try {
            List<Object[]> resultados = service.buscarParaCambio(criterio);
            modelo.setRowCount(0);
            for (Object[] row : resultados) {
                modelo.addRow(row);
            }
            if (resultados.isEmpty()) {
                Toast.show(this, Toast.Type.INFO, "No se encontraron resultados.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al buscar: " + ex.getMessage());
        }
    }

    private void cargarVariantesProducto() {
        int row = tabla.getSelectedRow();
        if (row == -1) {
            comboVariantes.removeAllItems();
            btnConfirmar.setEnabled(false);
            return;
        }

        int idProducto = (int) modelo.getValueAt(row, 9);
        try {
            int idBodega = 1; // Default fallback
            if(raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario() != null) {
                idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            }
            List<Object[]> variantes = service.obtenerVariantesProducto(idProducto, idBodega);
            comboVariantes.removeAllItems();
            for (Object[] v : variantes) {
                comboVariantes.addItem(new VarianteItem((int)v[0], (String)v[1]));
            }
            btnConfirmar.setEnabled(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar tallas: " + ex.getMessage());
        }
    }

    private void confirmarCambio() {
        int row = tabla.getSelectedRow();
        if (row == -1) return;

        VarianteItem selectedVariant = (VarianteItem) comboVariantes.getSelectedItem();
        if (selectedVariant == null) return;

        String tipo = (String) modelo.getValueAt(row, 0);
        int idRef = Integer.parseInt(modelo.getValueAt(row, 1).toString()); // Venta o Traspaso ID
        int idDetalle = (int) modelo.getValueAt(row, 7);
        int idVarianteAnterior = (int) modelo.getValueAt(row, 8);
        int cantidad = (int) modelo.getValueAt(row, 6); // Por ahora asumimos cambio total de la línea.
        // Si la cantidad es mayor a 1, idealmente preguntaríamos cuántos cambiar.
        // Simplificación: Cambiar toda la línea seleccionada.

        String observaciones = txtObservaciones.getText().trim();
        if (observaciones.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "Ingrese una observación.");
            return;
        }

        if (idVarianteAnterior == selectedVariant.id) {
            Toast.show(this, Toast.Type.WARNING, "La nueva talla es igual a la actual.");
            return;
        }

        try {
            int idBodega = 1; // Default fallback
            if(raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario() != null) {
                idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            }
            if ("VENTA".equals(tipo)) {
                service.realizarCambioTallaVenta(idRef, idDetalle, idVarianteAnterior, selectedVariant.id, cantidad, observaciones, idBodega);
            } else {
                service.realizarCambioTallaTraspaso(idRef, idDetalle, idVarianteAnterior, selectedVariant.id, cantidad, observaciones, idBodega);
            }
            Toast.show(this, Toast.Type.SUCCESS, "Cambio de talla realizado con éxito.");
            buscar(); // Recargar
            txtObservaciones.setText("");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al realizar cambio: " + ex.getMessage());
        }
    }

    private static class VarianteItem {
        int id;
        String nombre;

        public VarianteItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}
