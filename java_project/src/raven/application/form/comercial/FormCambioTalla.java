package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import raven.clases.admin.UserSession;
import raven.clases.comercial.ServiceCambioTalla;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;

public class FormCambioTalla extends JPanel {

    private ServiceCambioTalla service;
    private JTextField txtBuscar;
    private JPanel panelResultados;
    private JPanel panelTallas;
    private JButton btnConfirmar;
    private JButton btnCancelar;
    private JLabel lblProducto, lblMarca, lblTallaActual, lblPrecio;
    private int idReferenciaSeleccionada = -1;
    private String tipoReferenciaSeleccionada = "";
    private int idDetalleSeleccionado = -1;
    private int idVarianteAnterior = -1;
    private int idProductoSeleccionado = -1;
    private int cantidadSeleccionada = 0;
    private int idNuevaVarianteSeleccionada = -1;
    private String nuevaTallaSeleccionada = "";
    private JTextField txtObservaciones;
    private ButtonGroup tallasGroup;

    public FormCambioTalla() {
        service = new ServiceCambioTalla();
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][][grow][bottom]"));
        
        // Header
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]"));
        JLabel title = new JLabel("Cambio Rápido de Talla");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        title.setIcon(new FlatSVGIcon("raven/icon/svg/exchange.svg", 24, 24)); // Icono sugerido
        headerPanel.add(title);
        
        JLabel subtitle = new JLabel("Proceso simplificado para cambio de talla sin devolución completa");
        subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        add(headerPanel, "growx, wrap");
        add(subtitle, "wrap 20");

        // 1. Panel de Búsqueda
        JPanel panelPaso1 = new JPanel(new MigLayout("fillx, insets 15", "[grow]10[]", "[]"));
        panelPaso1.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:darken($Panel.background,5%)");
        panelPaso1.setBorder(BorderFactory.createTitledBorder("1. Escanea o ingresa el código del producto a cambiar:"));
        
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "7700219553511");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "font:+4; arc:10; margin:5,10,5,10");
        txtBuscar.addActionListener(e -> buscar());
        
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setIcon(new FlatSVGIcon("raven/icon/svg/search.svg"));
        btnBuscar.putClientProperty(FlatClientProperties.STYLE, "font:bold; background:@accentColor; foreground:#ffffff; arc:10");
        btnBuscar.addActionListener(e -> buscar());
        
        panelPaso1.add(txtBuscar, "growx, h 50!");
        panelPaso1.add(btnBuscar, "h 50!, w 120!");
        add(panelPaso1, "growx, wrap 20");

        // 2. Panel de Resultados (Oculto inicialmente)
        panelResultados = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow]"));
        panelResultados.setVisible(false);

        // Tarjeta de Producto Encontrado
        JPanel cardProducto = new JPanel(new MigLayout("fillx, insets 20", "[][grow][]", "[]5[]5[]"));
        cardProducto.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:#1e4620"); // Verde oscuro estilo éxito
        
        JLabel lblIconoExito = new JLabel(new FlatSVGIcon("raven/icon/svg/check.svg", 32, 32)); // Icono check
        
        JPanel infoPanel = new JPanel(new MigLayout("insets 0, wrap 1"));
        infoPanel.setOpaque(false);
        
        JLabel lblEncontrado = new JLabel("PRODUCTO ENCONTRADO");
        lblEncontrado.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:#4ade80");
        
        lblProducto = new JLabel("Nombre del Producto");
        lblProducto.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:#ffffff");
        
        lblMarca = new JLabel("Ref: 000 | Fecha: 00/00/0000");
        lblMarca.putClientProperty(FlatClientProperties.STYLE, "foreground:#dcfce7");
        
        lblTallaActual = new JLabel("Talla actual: 43 (EU)");
        lblTallaActual.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:#ffffff");

        infoPanel.add(lblEncontrado);
        infoPanel.add(lblProducto);
        infoPanel.add(lblMarca);
        infoPanel.add(lblTallaActual);
        
        lblPrecio = new JLabel("$0.00");
        lblPrecio.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:#4ade80");

        cardProducto.add(lblIconoExito, "top");
        cardProducto.add(infoPanel, "growx");
        cardProducto.add(lblPrecio, "top");
        
        panelResultados.add(cardProducto, "growx, wrap 20");

        // Selección de Talla
        JPanel panelSeleccion = new JPanel(new MigLayout("fill, insets 15", "[grow]", "[][grow]"));
        panelSeleccion.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:darken($Panel.background,5%)");
        panelSeleccion.setBorder(BorderFactory.createTitledBorder("2. Selecciona la nueva talla:"));
        
        panelTallas = new JPanel(new MigLayout("wrap 8, gap 10", "[grow, fill]", "[]"));
        panelTallas.setOpaque(false);
        
        panelSeleccion.add(panelTallas, "grow");
        panelResultados.add(panelSeleccion, "grow");

        add(panelResultados, "grow, wrap 20");

        // Footer: Observaciones y Botones
        JPanel panelFooter = new JPanel(new MigLayout("fillx, insets 0", "[grow]push[][]", "[]"));
        
        txtObservaciones = new JTextField();
        txtObservaciones.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Motivo del cambio (opcional)...");
        
        btnCancelar = new JButton("Cancelar");
        btnCancelar.setIcon(new FlatSVGIcon("raven/icon/svg/cancel.svg"));
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "background:#4b5563; foreground:#ffffff; arc:10; borderWidth:0; margin:10,20,10,20");
        btnCancelar.addActionListener(e -> limpiar());
        
        btnConfirmar = new JButton("Confirmar Cambio");
        btnConfirmar.setEnabled(false);
        btnConfirmar.putClientProperty(FlatClientProperties.STYLE, "background:#0ea5e9; foreground:#ffffff; font:bold; arc:10; borderWidth:0; margin:10,20,10,20");
        btnConfirmar.setIcon(new FlatSVGIcon("raven/icon/svg/check.svg"));
        btnConfirmar.addActionListener(e -> confirmar());
        
        // panelFooter.add(txtObservaciones, "growx, w 300!"); 
        // Observaciones dentro de un diálogo o aquí? La imagen no muestra input de observaciones explícito, pero el usuario lo pidió.
        // Lo pondré sutilmente.
        
        add(new JLabel("Observaciones:"), "split 2, gapright 10");
        add(txtObservaciones, "growx, wrap 10");
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonsPanel.add(btnCancelar);
        buttonsPanel.add(btnConfirmar);
        
        add(buttonsPanel, "center");
    }

    private void buscar() {
        String criterio = txtBuscar.getText().trim();
        if (criterio.isEmpty()) return;

        try {
            List<Object[]> resultados = service.buscarParaCambio(criterio);
            if (resultados.isEmpty()) {
                Toast.show(this, Toast.Type.WARNING, "No se encontraron resultados.");
                panelResultados.setVisible(false);
                return;
            }

            if (resultados.size() > 1) {
                mostrarSeleccionResultados(resultados);
            } else {
                cargarProducto(resultados.get(0));
            }

        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al buscar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarSeleccionResultados(List<Object[]> resultados) {
        String[] cols = {"Tipo", "Ref", "Producto", "Talla", "Cant", "Fecha"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        for (Object[] row : resultados) {
            model.addRow(new Object[]{row[0], row[1], row[4], row[5], row[6], row[2]});
        }
        
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        int opt = JOptionPane.showConfirmDialog(this, scroll, "Seleccione el producto a cambiar", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            int selected = table.getSelectedRow();
            if (selected != -1) {
                cargarProducto(resultados.get(selected));
            }
        }
    }

    private void cargarProducto(Object[] row) {
        // row: [0]Tipo, [1]Ref, [2]Fecha, [3]Cli/Bod, [4]Prod, [5]Talla, [6]Cant, [7]idDetalle, [8]idVariante, [9]idProducto, [10]idBodega, [11]foto, [12]precio
        tipoReferenciaSeleccionada = (String) row[0];
        idReferenciaSeleccionada = Integer.parseInt(row[1].toString());
        idDetalleSeleccionado = (int) row[7];
        idVarianteAnterior = (int) row[8];
        idProductoSeleccionado = (int) row[9];
        cantidadSeleccionada = (int) row[6];
        BigDecimal precio = (BigDecimal) row[12];

        lblProducto.setText(row[4].toString());
        lblMarca.setText(tipoReferenciaSeleccionada + ": " + row[1] + " | Fecha: " + row[2]);
        lblTallaActual.setText("Talla actual: " + row[5]);
        
        if (precio != null) {
            DecimalFormat df = new DecimalFormat("$ #,##0.00");
            lblPrecio.setText(df.format(precio));
        } else {
            lblPrecio.setText("");
        }
        
        panelResultados.setVisible(true);
        cargarVariantes();
        revalidate();
        repaint();
    }

    private void cargarVariantes() {
        panelTallas.removeAll();
        idNuevaVarianteSeleccionada = -1;
        btnConfirmar.setEnabled(false);
        tallasGroup = new ButtonGroup();
        
        try {
            int idBodega = UserSession.getInstance().getIdBodegaUsuario();
            List<Object[]> variantes = service.obtenerVariantesProducto(idProductoSeleccionado, idBodega);
            
            for (Object[] v : variantes) {
                int id = (int) v[0];
                String talla = (String) v[1];
                int stock = ((Number) v[3]).intValue();
                
                JToggleButton btn = new JToggleButton(talla);
                btn.setToolTipText("Stock: " + stock);
                btn.putClientProperty(FlatClientProperties.STYLE, ""
                        + "font:bold; arc:10; "
                        + "margin:10,20,10,20;"
                        + "background:#2563eb; foreground:#ffffff;" // Azul base
                        + "selectedBackground:#1d4ed8; selectedForeground:#ffffff;" // Azul oscuro seleccionado
                        + "hoverBackground:#3b82f6"); 
                
                if (stock <= 0) {
                    btn.setEnabled(false);
                    btn.putClientProperty(FlatClientProperties.STYLE, "background:#374151; foreground:#9ca3af; arc:10"); // Gris deshabilitado
                }
                
                btn.addActionListener(e -> {
                    idNuevaVarianteSeleccionada = id;
                    nuevaTallaSeleccionada = talla;
                    btnConfirmar.setEnabled(true);
                });
                
                tallasGroup.add(btn);
                panelTallas.add(btn, "grow, h 50!");
            }
            
            if (variantes.isEmpty()) {
                panelTallas.add(new JLabel("No hay otras tallas disponibles en esta bodega."));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            panelTallas.add(new JLabel("Error cargando tallas."));
        }
        panelTallas.revalidate();
        panelTallas.repaint();
    }

    private void confirmar() {
        if (idNuevaVarianteSeleccionada == -1) return;
        
        String obs = txtObservaciones.getText().trim();
        if (obs.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "Ingrese una observación.");
            txtObservaciones.requestFocus();
            return;
        }

        try {
            int idBodega = UserSession.getInstance().getIdBodegaUsuario();
            if (tipoReferenciaSeleccionada.equalsIgnoreCase("VENTA")) {
                service.realizarCambioTallaVenta(idReferenciaSeleccionada, idDetalleSeleccionado, idVarianteAnterior, idNuevaVarianteSeleccionada, cantidadSeleccionada, obs, idBodega);
            } else {
                service.realizarCambioTallaTraspaso(idReferenciaSeleccionada, idDetalleSeleccionado, idVarianteAnterior, idNuevaVarianteSeleccionada, cantidadSeleccionada, obs, idBodega);
            }
            
            Toast.show(this, Toast.Type.SUCCESS, "Cambio realizado con éxito.");
            limpiar();
        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al guardar cambio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limpiar() {
        txtBuscar.setText("");
        txtObservaciones.setText("");
        panelResultados.setVisible(false);
        btnConfirmar.setEnabled(false);
        txtBuscar.requestFocus();
    }
}
