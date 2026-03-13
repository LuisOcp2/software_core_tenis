package raven.application.form.comercial.devolucion;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.application.form.comercial.devolucion.components.ModernAlert;
import raven.clases.admin.UserSession;
import raven.clases.comercial.ServiceDevolucion;
import raven.controlador.principal.ModelVenta;
import raven.controlador.principal.ModelDetalleVenta;

/**
 * Pantalla de Selección de Productos para Devolución (Rediseño Card UI)
 * Reemplaza la tabla tradicional con una lista de tarjetas de productos
 * interactivas.
 * VALIDADO: Usa ModelDetalleVenta en lugar de clases inexistentes.
 */
public class SeleccionarProductos extends javax.swing.JPanel {

    // DATOS
    private ModelVenta ventaSeleccionada;
    private List<ModelDetalleVenta> productosCache;
    private nuevaDevolucion123 controller;
    private final ServiceDevolucion serviceDevolucion;

    // UI COMPONENTS
    private JPanel panelHeader;
    private JPanel panelProductosContainer;
    private JPanel panelFooter;
    private JLabel lblTotalDevolucion;
    private JButton btnContinuar;

    // STATE
    private final Map<Integer, Integer> cantidadesSeleccionadas = new HashMap<>(); // ID Detalle -> Cantidad
    private BigDecimal totalCalculado = BigDecimal.ZERO;

    public SeleccionarProductos() {
        this(null, new ArrayList<>());
    }

    public SeleccionarProductos(ModelVenta venta, List<ModelDetalleVenta> productos) {
        this.ventaSeleccionada = venta;
        // Si no se pasan productos explícitamente, usarlos de la venta
        if (productos == null || productos.isEmpty()) {
            this.productosCache = venta != null ? venta.getDetalles() : new ArrayList<>();
        } else {
            this.productosCache = productos;
        }

        this.serviceDevolucion = new ServiceDevolucion();

        initComponents();
        if (venta != null) {
            cargarDatos();
        }
    }

    public void setController(nuevaDevolucion123 controller) {
        this.controller = controller;
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#1a202c")); // Fondo oscuro consistente

        // 1. HEADER
        panelHeader = new JPanel(new BorderLayout());
        panelHeader.setOpaque(false);
        panelHeader.setBorder(new EmptyBorder(20, 20, 10, 20));
        add(panelHeader, BorderLayout.NORTH);

        // 2. CONTENIDO
        panelProductosContainer = new JPanel();
        panelProductosContainer.setLayout(new BoxLayout(panelProductosContainer, BoxLayout.Y_AXIS));
        panelProductosContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(panelProductosContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // 3. FOOTER
        panelFooter = new JPanel(new BorderLayout());
        panelFooter.setBackground(Color.decode("#2d3748"));
        panelFooter.setBorder(new EmptyBorder(20, 30, 20, 30));
        add(panelFooter, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        // --- HEADER ---
        JPanel headerInfo = new JPanel(new GridLayout(2, 1));
        headerInfo.setOpaque(false);

        JLabel lblTitulo = new JLabel("Seleccionar Productos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);

        String cliente = ventaSeleccionada.getCliente() != null ? ventaSeleccionada.getCliente().getNombre()
                : "Cliente General";
        JLabel lblSubtitulo = new JLabel(
                "Venta #" + String.format("VEN-%06d", ventaSeleccionada.getIdVenta()) + " - " + cliente);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(Color.decode("#a0aec0"));

        headerInfo.add(lblTitulo);
        headerInfo.add(lblSubtitulo);
        panelHeader.add(headerInfo, BorderLayout.CENTER);

        // --- LISTA DE PRODUCTOS ---
        panelProductosContainer.removeAll();

        if (productosCache == null || productosCache.isEmpty()) {
            mostrarEstadoVacio();
        } else {
            for (ModelDetalleVenta prod : productosCache) {
                panelProductosContainer.add(crearTarjetaProducto(prod));
                panelProductosContainer.add(Box.createVerticalStrut(10));
            }
        }

        // --- FOOTER ---
        construirFooter();

        revalidate();
        repaint();
    }

    private JPanel crearTarjetaProducto(ModelDetalleVenta prod) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.decode("#2d3748"));
        card.setBorder(new EmptyBorder(15, 20, 15, 20));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:15");
        card.setMaximumSize(new Dimension(2000, 100));

        // A. Icono e Info Principal
        JPanel leftPanel = new JPanel(new BorderLayout(15, 0));
        leftPanel.setOpaque(false);

        JLabel icon = new JLabel(FontIcon.of(FontAwesomeSolid.BOX_OPEN, 32, Color.decode("#4299e1")));
        leftPanel.add(icon, BorderLayout.WEST);

        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);

        // Obtener nombre del producto y detalles
        String nombreProd = (prod.getProducto() != null) ? prod.getProducto().getName() : "Producto Desconocido";

        // Logica robusta para obtener detalles (Soporte para variantes)
        String ean = "N/A";
        String talla = "N/A";
        String color = "N/A";

        if (prod.getProducto() != null) {
            // Intento 1: Obtener de la variante si existe y está cargada
            if (prod.getIdVariante() > 0 && prod.getProducto().getVariants() != null
                    && !prod.getProducto().getVariants().isEmpty()) {
                raven.controlador.productos.ModelProductVariant variante = prod.getProducto().getVariants().get(0);
                ean = (variante.getBarcode() != null && !variante.getBarcode().isEmpty()) ? variante.getBarcode()
                        : (variante.getEan() != null ? variante.getEan() : "N/A");

                // Talla desde variante
                if (variante.getSizeName() != null) {
                    talla = variante.getSizeName();
                } else if (prod.getProducto().getSize() != null) {
                    talla = prod.getProducto().getSize();
                }

                // Color desde variante
                if (variante.getColorName() != null) {
                    color = variante.getColorName();
                } else if (prod.getProducto().getColor() != null) {
                    color = prod.getProducto().getColor();
                }

            } else {
                // Intento 2: Obtener del producto base
                ean = (prod.getProducto().getBarcode() != null) ? prod.getProducto().getBarcode() : "N/A";
                talla = (prod.getProducto().getSize() != null) ? prod.getProducto().getSize() : "N/A";
                color = (prod.getProducto().getColor() != null) ? prod.getProducto().getColor() : "N/A";
            }
        }

        JLabel lblNombre = new JLabel(nombreProd);
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblNombre.setForeground(Color.WHITE);

        // Formato HTML para detalles enriquecidos
        String detallesHtml = String.format("<html><body style='width: 300px'>" +
                "<span style='color: #a0aec0'>EAN:</span> <span style='color: #e2e8f0'>%s</span> &nbsp;|&nbsp; " +
                "<span style='color: #a0aec0'>Talla:</span> <span style='color: #e2e8f0'>%s</span> &nbsp;|&nbsp; " +
                "<span style='color: #a0aec0'>Color:</span> <span style='color: #e2e8f0'>%s</span><br>" +
                "<span style='color: #a0aec0'>Precio:</span> <span style='color: #48bb78'>$%.2f</span> &nbsp;|&nbsp; " +
                "<span style='color: #a0aec0'>Comprado:</span> <span style='color: #e2e8f0'>%d un.</span>" +
                "</body></html>",
                ean, talla, color, prod.getPrecioUnitario(), prod.getCantidad());

        JLabel lblDetalle = new JLabel(detallesHtml);
        lblDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        texts.add(lblNombre);
        texts.add(lblDetalle);
        leftPanel.add(texts, BorderLayout.CENTER);

        card.add(leftPanel, BorderLayout.CENTER);

        // B. Controles de Cantidad
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // Boton Menos
        JButton btnMinus = new JButton(FontIcon.of(FontAwesomeSolid.MINUS, 12, Color.WHITE));
        btnMinus.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:#4a5568;borderWidth:0;margin:5,10,5,10");

        // Input Cantidad
        JTextField txtQty = new JTextField("0");
        txtQty.setHorizontalAlignment(SwingConstants.CENTER);
        txtQty.setEditable(false);
        txtQty.setPreferredSize(new Dimension(50, 35));
        txtQty.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:#1a202c;foreground:#ffffff;borderWidth:0");

        // Boton Mas
        JButton btnPlus = new JButton(FontIcon.of(FontAwesomeSolid.PLUS, 12, Color.WHITE));
        btnPlus.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:#4a5568;borderWidth:0;margin:5,10,5,10");

        // Label Subtotal Dinámico
        JLabel lblSubtotal = new JLabel("$0.00");
        lblSubtotal.setPreferredSize(new Dimension(80, 35));
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblSubtotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSubtotal.setForeground(Color.decode("#48bb78")); // Verde

        // Lógica de botones
        btnMinus.addActionListener(e -> {
            int current = Integer.parseInt(txtQty.getText());
            if (current > 0) {
                actualizarCantidad(prod, current - 1, txtQty, lblSubtotal);
            }
        });

        btnPlus.addActionListener(e -> {
            int current = Integer.parseInt(txtQty.getText());
            if (current < prod.getCantidad()) {
                actualizarCantidad(prod, current + 1, txtQty, lblSubtotal);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        });

        rightPanel.add(new JLabel("Devolver:"));
        rightPanel.add(btnMinus);
        rightPanel.add(txtQty);
        rightPanel.add(btnPlus);
        rightPanel.add(Box.createHorizontalStrut(15));
        rightPanel.add(lblSubtotal);

        card.add(rightPanel, BorderLayout.EAST);

        return card;
    }

    private void actualizarCantidad(ModelDetalleVenta prod, int newQty, JTextField display, JLabel subtotalDisplay) {
        display.setText(String.valueOf(newQty));
        cantidadesSeleccionadas.put(prod.getIdDetalle(), newQty);

        // Calcular subtotal item
        BigDecimal precio = BigDecimal.valueOf(prod.getPrecioUnitario());
        BigDecimal sub = precio.multiply(new BigDecimal(newQty));

        if (prod.getDescuento() > 0 && prod.getCantidad() > 0) {
            BigDecimal descTotal = BigDecimal.valueOf(prod.getDescuento());
            BigDecimal descUnit = descTotal.divide(new BigDecimal(prod.getCantidad()), 2,
                    java.math.RoundingMode.HALF_UP);
            sub = sub.subtract(descUnit.multiply(new BigDecimal(newQty)));
        }
        subtotalDisplay.setText(String.format("$%.2f", sub));

        recalcularTotalGeneral();
    }

    private void recalcularTotalGeneral() {
        totalCalculado = BigDecimal.ZERO;

        for (ModelDetalleVenta prod : productosCache) {
            int qty = cantidadesSeleccionadas.getOrDefault(prod.getIdDetalle(), 0);
            if (qty > 0) {
                BigDecimal precio = BigDecimal.valueOf(prod.getPrecioUnitario());
                BigDecimal sub = precio.multiply(new BigDecimal(qty));

                if (prod.getDescuento() > 0 && prod.getCantidad() > 0) {
                    BigDecimal descTotal = BigDecimal.valueOf(prod.getDescuento());
                    BigDecimal descUnit = descTotal.divide(new BigDecimal(prod.getCantidad()), 2,
                            java.math.RoundingMode.HALF_UP);
                    sub = sub.subtract(descUnit.multiply(new BigDecimal(qty)));
                }
                totalCalculado = totalCalculado.add(sub);
            }
        }

        // Actualizar Footer
        lblTotalDevolucion.setText("Total a Devolver: " + String.format("$%.2f", totalCalculado));
        btnContinuar.setEnabled(totalCalculado.compareTo(BigDecimal.ZERO) > 0);
    }

    private void construirFooter() {
        panelFooter.removeAll();

        // Total
        lblTotalDevolucion = new JLabel("Total a Devolver: $0.00");
        lblTotalDevolucion.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotalDevolucion.setForeground(Color.decode("#48bb78"));

        // Botones
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE,
                "background:#e53e3e;foreground:#ffffff;borderWidth:0;font:bold;arc:10");
        btnCancelar.setPreferredSize(new Dimension(120, 40));
        btnCancelar.addActionListener(e -> {
            if (controller != null)
                controller.irAPaso1();
        });

        btnContinuar = new JButton("Continuar");
        btnContinuar.putClientProperty(FlatClientProperties.STYLE,
                "background:#3182ce;foreground:#ffffff;borderWidth:0;font:bold;arc:10");
        btnContinuar.setPreferredSize(new Dimension(150, 40));
        btnContinuar.setEnabled(false); // Deshabilitado hasta seleccionar algo
        btnContinuar.addActionListener(e -> procesarSeleccion());

        buttons.add(btnCancelar);
        buttons.add(btnContinuar);

        panelFooter.add(lblTotalDevolucion, BorderLayout.WEST);
        panelFooter.add(buttons, BorderLayout.EAST);
    }

    private void mostrarEstadoVacio() {
        JLabel empty = new JLabel("No se encontraron productos en esta venta.");
        empty.setForeground(Color.WHITE);
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        panelProductosContainer.add(empty);
    }

    private void procesarSeleccion() {
        List<ServiceDevolucion.ProductoDevolucion> productosADevolver = new ArrayList<>();

        for (ModelDetalleVenta prod : productosCache) {
            int qty = cantidadesSeleccionadas.getOrDefault(prod.getIdDetalle(), 0);
            if (qty > 0) {
                productosADevolver.add(new ServiceDevolucion.ProductoDevolucion(
                        prod.getIdDetalle(),
                        prod.getProducto().getProductId(),
                        prod.getIdVariante(),
                        prod.getCantidad(),
                        qty,
                        BigDecimal.valueOf(prod.getPrecioUnitario()),
                        BigDecimal.valueOf(prod.getDescuento()),
                        "" // Observaciones
                ));
            }
        }

        if (productosADevolver.isEmpty()) {
            new ModernAlert(ModernAlert.AlertType.WARNING, "Selección Requerida",
                    "Debe seleccionar al menos un producto.").setVisible(true);
            return;
        }

        try {
            int idUsuario = UserSession.getInstance().getCurrentUser().getIdUsuario();
            raven.controlador.comercial.ModelDevolucion devolucion = serviceDevolucion.procesarDevolucion(
                    this,
                    ventaSeleccionada,
                    productosADevolver,
                    "Devolución Cliente",
                    idUsuario);

            if (devolucion != null) {
                // Verificar si fue auto-aprobada
                if (raven.controlador.comercial.ModelDevolucion.EstadoDevolucion.APROBADA
                        .equals(devolucion.getEstado())) {
                    try {
                        // Obtener la Nota de Crédito completa
                        raven.controlador.comercial.ModelNotaCredito nc = serviceDevolucion
                                .obtenerNotaCreditoPorDevolucion(devolucion.getIdDevolucion());

                        if (nc != null) {
                            // Mostrar diálogo de nota de crédito
                            Window parentWindow = SwingUtilities.getWindowAncestor(this);
                            if (parentWindow instanceof Frame) {
                                DialogoNotaCredito dialogo = new DialogoNotaCredito((Frame) parentWindow, nc);
                                dialogo.setVisible(true);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error mostrando nota de crédito: " + e.getMessage());
                    }
                }

                // Redireccionar siempre al inicio después del proceso
                if (controller != null) {
                    controller.irAPaso1();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error procesando devolución: " + e.getMessage());
        }
    }
}
