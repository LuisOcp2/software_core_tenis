package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.productos.ModelMovement;

/**
 * Modal para mostrar detalles completos de un movimiento de inventario
 */
public class DetalleMovimientoDialog extends JDialog {

    private static final String STYLE_PANEL = "arc:20;background:$Login.background";
    private static final String STYLE_LABEL_TITULO = "font:bold +4";
    private static final String STYLE_LABEL_HEADER = "font:bold +1";
    private static final String STYLE_LABEL_VALUE = "foreground:$Menu.foreground";

    private final ModelMovement movimiento;

    public DetalleMovimientoDialog(Frame parent, ModelMovement movimiento) {
        super(parent, "Detalle del Movimiento", true);
        this.movimiento = movimiento;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setSize(500, 450);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        // Header con icono y título
        JPanel headerPanel = crearHeaderPanel();

        // Panel de información
        JPanel infoPanel = crearInfoPanel();

        // Botón cerrar
        JPanel buttonPanel = crearButtonPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(infoPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel crearHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setOpaque(false);

        // Icono según tipo de movimiento
        FontIcon icono = crearIconoTipo();
        JLabel lblIcono = new JLabel(icono);

        // Título
        JLabel lblTitulo = new JLabel("Movimiento #" + movimiento.getIdMovimiento());
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, STYLE_LABEL_TITULO);

        panel.add(lblIcono);
        panel.add(lblTitulo);

        return panel;
    }

    private FontIcon crearIconoTipo() {
        String tipo = movimiento.getTipoMovimiento().toLowerCase();
        FontIcon icono;
        Color color;

        if (tipo.contains("entrada")) {
            icono = FontIcon.of(FontAwesomeSolid.ARROW_DOWN);
            color = new Color(76, 175, 80); // Verde
        } else if (tipo.contains("salida")) {
            icono = FontIcon.of(FontAwesomeSolid.ARROW_UP);
            color = new Color(244, 67, 54); // Rojo
        } else {
            icono = FontIcon.of(FontAwesomeSolid.SYNC_ALT);
            color = new Color(255, 193, 7); // Amarillo
        }

        icono.setIconSize(32);
        icono.setIconColor(color);
        return icono;
    }

    private JPanel crearInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Sección: Producto
        addSectionHeader(panel, gbc, row++, "Caja INFORMACIÓN DEL PRODUCTO");
        addInfoRow(panel, gbc, row++, "Producto:", movimiento.getNombreProducto());
        addInfoRow(panel, gbc, row++, "Color:", movimiento.getColor());
        addInfoRow(panel, gbc, row++, "Talla:", movimiento.getTalla());

        // Separador
        row = addSeparator(panel, gbc, row);

        // Sección: Movimiento
        addSectionHeader(panel, gbc, row++, " DETALLES DEL MOVIMIENTO");
        addInfoRow(panel, gbc, row++, "Tipo:", movimiento.getTipoMovimiento());
        addInfoRow(panel, gbc, row++, "Cantidad:", formatearCantidad());
        addInfoRow(panel, gbc, row++, "Fecha:", formatearFecha());

        // Separador
        row = addSeparator(panel, gbc, row);

        // Sección: Referencia
        addSectionHeader(panel, gbc, row++, " REFERENCIA");
        addInfoRow(panel, gbc, row++, "Tipo Ref.:",
                movimiento.getTipoReferencia() != null ? movimiento.getTipoReferencia() : "N/A");
        addInfoRow(panel, gbc, row++, "Observaciones:",
                movimiento.getObservaciones() != null && !movimiento.getObservaciones().isEmpty()
                        ? movimiento.getObservaciones()
                        : "Sin observaciones");

        return panel;
    }

    private void addSectionHeader(JPanel panel, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, STYLE_LABEL_HEADER + ";foreground:$Component.accentColor");
        panel.add(label, gbc);
        gbc.gridwidth = 1;
    }

    private void addInfoRow(JPanel panel, GridBagConstraints gbc, int row, String header, String value) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0.3;
        JLabel lblHeader = new JLabel(header);
        lblHeader.putClientProperty(FlatClientProperties.STYLE, STYLE_LABEL_HEADER);
        panel.add(lblHeader, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JLabel lblValue = new JLabel(value);
        lblValue.putClientProperty(FlatClientProperties.STYLE, STYLE_LABEL_VALUE);
        panel.add(lblValue, gbc);
    }

    private int addSeparator(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator sep = new JSeparator();
        panel.add(sep, gbc);
        gbc.gridwidth = 1;
        return row + 1;
    }

    private String formatearCantidad() {
        String tipo = movimiento.getTipoMovimiento().toLowerCase();
        int cantidad = movimiento.getCantidad();

        if (tipo.contains("entrada")) {
            return "+" + cantidad + " unidades";
        } else if (tipo.contains("salida")) {
            return "-" + cantidad + " unidades";
        } else {
            return cantidad + " unidades";
        }
    }

    private String formatearFecha() {
        if (movimiento.getFechaMovimiento() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return movimiento.getFechaMovimiento().format(formatter);
        }
        return "N/A";
    }

    private JPanel crearButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;background:$Component.accentColor;foreground:$Button.foreground");
        btnCerrar.setPreferredSize(new Dimension(120, 35));
        btnCerrar.addActionListener(e -> dispose());

        panel.add(btnCerrar);
        return panel;
    }
}

