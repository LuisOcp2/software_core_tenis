package raven.application.form.comercial.compras;

import com.formdev.flatlaf.FlatClientProperties;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import raven.controlador.principal.ModelCompra;
import raven.controlador.principal.ModelCompraDetalle;
import raven.clases.principal.ServiceCompra;

/**
 * Diálogo para ver el detalle de una compra.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class DetalleCompraDialog extends JDialog {

    private static final String STYLE_PANEL = "arc:15;background:$Table.gridColor;";
    private static final NumberFormat FORMATO_MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    private final ModelCompra compra;
    private final ServiceCompra serviceCompra = new ServiceCompra();

    public DetalleCompraDialog(JFrame parent, ModelCompra compra) {
        super(parent, "Detalle de Compra - " + compra.getNumeroCompra(), true);
        this.compra = compra;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(800, 600));
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de información de cabecera
        JPanel panelInfo = crearPanelInfo();
        add(panelInfo, BorderLayout.NORTH);

        // Tabla de productos
        JPanel panelProductos = crearPanelProductos();
        add(panelProductos, BorderLayout.CENTER);

        // Panel de totales y acciones
        JPanel panelInferior = crearPanelInferior();
        add(panelInferior, BorderLayout.SOUTH);
    }

    private JPanel crearPanelInfo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createTitledBorder("Información de la Compra"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        Font fontBold = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        Color colorVerde = new Color(40, 167, 69);

        // Fila 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Número de Compra:"), gbc);
        gbc.gridx = 1;
        JLabel lblNumero = new JLabel(compra.getNumeroCompra());
        lblNumero.setFont(fontBold);
        lblNumero.setForeground(colorVerde);
        panel.add(lblNumero, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Fecha:"), gbc);
        gbc.gridx = 3;
        panel.add(new JLabel(compra.getFechaCompra() != null ? compra.getFechaCompra().toString() : "N/A"), gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 5;
        JLabel lblEstado = new JLabel(compra.getEstado().getValor().toUpperCase());
        lblEstado.setFont(fontBold);
        switch (compra.getEstado()) {
            case RECIBIDA -> lblEstado.setForeground(colorVerde);
            case PENDIENTE -> lblEstado.setForeground(new Color(255, 193, 7));
            case CANCELADA -> lblEstado.setForeground(new Color(220, 53, 69));
        }
        panel.add(lblEstado, gbc);

        // Fila 2
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Proveedor:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JLabel lblProveedor = new JLabel(compra.getNombreProveedor());
        lblProveedor.setFont(fontBold);
        panel.add(lblProveedor, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Nº Factura:"), gbc);
        gbc.gridx = 4;
        gbc.gridwidth = 2;
        panel.add(new JLabel(compra.getNumeroFactura() != null ? compra.getNumeroFactura() : "N/A"), gbc);

        // Fila 3
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Bodega:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(compra.getNombreBodega()), gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 3;
        panel.add(new JLabel(compra.getNombreUsuario()), gbc);

        // Observaciones (si hay)
        if (compra.getObservaciones() != null && !compra.getObservaciones().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            panel.add(new JLabel("Observaciones:"), gbc);
            gbc.gridx = 1;
            gbc.gridwidth = 5;
            JTextArea txtObs = new JTextArea(compra.getObservaciones());
            txtObs.setEditable(false);
            txtObs.setLineWrap(true);
            txtObs.setWrapStyleWord(true);
            txtObs.setOpaque(false);
            txtObs.setRows(2);
            panel.add(txtObs, gbc);
        }

        return panel;
    }

    private JPanel crearPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Productos de la Compra"));

        String[] columnas = { "Producto", "Variante", "EAN", "Cantidad", "Tipo", "Precio Unit.", "Subtotal" };

        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ModelCompraDetalle d : compra.getDetalles()) {
            modelo.addRow(new Object[] {
                    d.getNombreProducto(),
                    d.getDescripcionVariante(),
                    d.getEan(),
                    d.getCantidad(),
                    d.getTipoUnidad().getValor(),
                    d.getPrecioUnitario(),
                    d.getSubtotal()
            });
        }

        JTable tabla = new JTable(modelo);
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;" +
                        "showVerticalLines:false;" +
                        "rowHeight:40;" +
                        "intercellSpacing:10,5");

        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;" +
                        "height:40;" +
                        "separatorColor:$TableHeader.background;" +
                        "font:bold $h4.font");

        // Renderizador moneda
        DefaultTableCellRenderer monedaRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof BigDecimal) {
                    setText(FORMATO_MONEDA.format(value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        tabla.getColumnModel().getColumn(5).setCellRenderer(monedaRenderer);
        tabla.getColumnModel().getColumn(6).setCellRenderer(monedaRenderer);

        // Anchos
        tabla.getColumnModel().getColumn(0).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(60);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(100);

        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel panelTotales = new JPanel(new GridLayout(2, 4, 20, 5));
        panelTotales.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panelTotales.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        Font fontBold = new Font(Font.SANS_SERIF, Font.BOLD, 14);

        JLabel lblItems = new JLabel("Items: " + compra.getCantidadItems());
        lblItems.setFont(fontBold);

        JLabel lblSubtotal = new JLabel("Subtotal: " + FORMATO_MONEDA.format(compra.getSubtotal()));
        lblSubtotal.setFont(fontBold);

        JLabel lblIva = new JLabel("IVA: " + FORMATO_MONEDA.format(compra.getIva()));
        lblIva.setFont(fontBold);

        JLabel lblTotal = new JLabel("TOTAL: " + FORMATO_MONEDA.format(compra.getTotal()));
        lblTotal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTotal.setForeground(new Color(40, 167, 69));

        panelTotales.add(lblItems);
        panelTotales.add(lblSubtotal);
        panelTotales.add(lblIva);
        panelTotales.add(lblTotal);

        JLabel lblAbonado = new JLabel("Abonado: " + FORMATO_MONEDA.format(compra.getTotalAbonado()));
        lblAbonado.setFont(fontBold);
        JLabel lblSaldo = new JLabel("Saldo: " + FORMATO_MONEDA.format(compra.getSaldoPendiente()));
        lblSaldo.setFont(fontBold);
        JLabel lblEstadoPago = new JLabel("Estado Pago: "
                + (compra.getEstadoPago() != null ? compra.getEstadoPago().toUpperCase() : "PENDIENTE"));
        lblEstadoPago.setFont(fontBold);
        JLabel lblVacio = new JLabel("");

        panelTotales.add(lblAbonado);
        panelTotales.add(lblSaldo);
        panelTotales.add(lblEstadoPago);
        panelTotales.add(lblVacio);

        panel.add(panelTotales, BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.setIcon(FontIcon.of(FontAwesomeSolid.PRINT, 16, UIManager.getColor("TabbedPane.foreground")));
        btnImprimir.addActionListener(e -> imprimirCompra());

        // Solo permitir abonar si no está pagado/completado y hay saldo
        boolean puedeAbonar = !(compra.getEstadoPago() != null
                && (compra.getEstadoPago().equalsIgnoreCase("pagado")
                        || compra.getEstadoPago().equalsIgnoreCase("completado"))
                || compra.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0);

        JButton btnAbonar = new JButton("Registrar Abono");
        btnAbonar.setIcon(FontIcon.of(FontAwesomeSolid.MONEY_CHECK_ALT, 16, Color.WHITE));
        btnAbonar.putClientProperty(FlatClientProperties.STYLE,
                "arc:18; background:#28CD41; foreground:#FFFFFF; borderWidth:0;");
        btnAbonar.setEnabled(puedeAbonar);
        btnAbonar.addActionListener(e -> registrarAbono(lblAbonado, lblSaldo, lblEstadoPago));

        JButton btnVerAbonos = new JButton("Ver abonos");
        btnVerAbonos.setIcon(FontIcon.of(FontAwesomeSolid.EYE, 16, UIManager.getColor("TabbedPane.foreground")));
        btnVerAbonos.putClientProperty(FlatClientProperties.STYLE,
                "arc:18; background:$Table.background; borderColor:$Table.gridColor;");
        btnVerAbonos.addActionListener(e -> abrirHistorialAbonos());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setIcon(FontIcon.of(FontAwesomeSolid.TIMES, 16, Color.WHITE));
        btnCerrar.putClientProperty(FlatClientProperties.STYLE,
                "arc:18; background:#FF3B30; foreground:#FFFFFF; borderWidth:0;");
        btnCerrar.addActionListener(e -> dispose());

        panelAcciones.add(btnImprimir);
        panelAcciones.add(btnVerAbonos);
        panelAcciones.add(btnAbonar);
        panelAcciones.add(btnCerrar);

        panel.add(panelAcciones, BorderLayout.SOUTH);

        return panel;
    }

    private void imprimirCompra() {
        // TODO: Implementar impresión/exportación a PDF
        JOptionPane.showMessageDialog(this,
                "Función de impresión en desarrollo.\n" +
                        "Se generará un PDF con el detalle de la compra.",
                "Imprimir",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void abrirHistorialAbonos() {
        HistorialAbonosDialog dialog = new HistorialAbonosDialog((JFrame) getParent(), compra.getIdCompra(),
                serviceCompra);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void registrarAbono(JLabel lblAbonado, JLabel lblSaldo, JLabel lblEstadoPago) {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField txtMonto = new JTextField(12);
        JComboBox<String> cmbMedio = new JComboBox<>(
                new String[] { "efectivo", "transferencia", "tarjeta", "cheque", "otro" });
        JTextField txtUrl = new JTextField(18);
        JButton btnArchivo = new JButton("Seleccionar evidencia");
        JLabel lblArchivo = new JLabel("");

        final byte[][] evidenciaBytesHolder = new byte[1][];
        final String[] evidenciaMimeHolder = new String[1];
        final String[] evidenciaNombreHolder = new String[1];

        btnArchivo.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                java.io.File f = fc.getSelectedFile();
                try {
                    evidenciaBytesHolder[0] = java.nio.file.Files.readAllBytes(f.toPath());
                    evidenciaNombreHolder[0] = f.getName();
                    String mime = java.nio.file.Files.probeContentType(f.toPath());
                    evidenciaMimeHolder[0] = mime != null ? mime : "application/octet-stream";
                    lblArchivo.setText(f.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "No se pudo leer el archivo: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Monto:"), gbc);
        gbc.gridx = 1;
        form.add(txtMonto, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Medio de pago:"), gbc);
        gbc.gridx = 1;
        form.add(cmbMedio, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("URL evidencia (opcional):"), gbc);
        gbc.gridx = 1;
        form.add(txtUrl, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(btnArchivo, gbc);
        gbc.gridx = 1;
        form.add(lblArchivo, gbc);

        int opt = JOptionPane.showConfirmDialog(this, form, "Registrar Abono", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
                String medio = (String) cmbMedio.getSelectedItem();
                String url = txtUrl.getText().trim();

                ServiceCompra.AbonoResultado r = serviceCompra.registrarAbonoConEvidencia(
                        compra.getIdCompra(),
                        monto,
                        medio,
                        url.isEmpty() ? null : url,
                        evidenciaBytesHolder[0],
                        evidenciaMimeHolder[0],
                        evidenciaNombreHolder[0]);

                serviceCompra.obtenerCompra(compra.getIdCompra()).ifPresent(updated -> {
                    compra.setTotalAbonado(updated.getTotalAbonado());
                    compra.setSaldoPendiente(updated.getSaldoPendiente());
                    compra.setEstadoPago(updated.getEstadoPago());
                    lblAbonado.setText("Abonado: " + FORMATO_MONEDA.format(compra.getTotalAbonado()));
                    lblSaldo.setText("Saldo: " + FORMATO_MONEDA.format(compra.getSaldoPendiente()));
                    lblEstadoPago.setText("Estado Pago: " + compra.getEstadoPago().toUpperCase());
                });

                JOptionPane.showMessageDialog(this, "Abono registrado. Comprobante #" + r.idAbono, "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
