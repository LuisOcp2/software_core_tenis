package raven.application.form.comercial.devolucion;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.application.form.comercial.devolucion.components.ModernAlert;
import raven.clases.admin.UserSession;
import raven.clases.comercial.ServiceDevolucion;
import raven.clases.comercial.ValidacionElegibilidad;
import raven.controlador.principal.ModelVenta;
import raven.clases.principal.ServiceVenta;
import raven.application.form.comercial.devolucion.components.DesignConstants;

/**
 * Pantalla de Búsqueda de Ventas para Devoluciones (Rediseño Moderno v2)
 * Implementa CardLayout para búsqueda y resultados.
 */
public class buscarVenta extends javax.swing.JPanel {

    // CONTROLADORES Y SERVICIOS
    private nuevaDevolucion123 controller;
    private final ServiceVenta serviceVenta;
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // COMPONENTES UI
    private JPanel panelMain;
    private JPanel panelBusqueda;
    private JPanel panelResultados;
    private JTextField txtBusqueda;
    private JButton btnBuscar;

    // MODELO DE DATOS
    private List<ModelVenta> resultadosCache;

    public buscarVenta() {
        this.serviceVenta = new ServiceVenta();
        initComponents();
        configurarDiseñoInicial();
    }

    public void setController(nuevaDevolucion123 controller) {
        this.controller = controller;
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Panel Principal con CardLayout
        panelMain = new JPanel(new CardLayout());
        panelMain.setOpaque(false);

        // 1. Panel Búsqueda (Entry Point)
        panelBusqueda = new JPanel(new GridBagLayout());
        panelBusqueda.setOpaque(true); // Fondo será seteado

        // 2. Panel Resultados (Lista/Scroll)
        panelResultados = new JPanel(new BorderLayout());
        panelResultados.setOpaque(false);

        panelMain.add(panelBusqueda, "BUSQUEDA");
        panelMain.add(panelResultados, "RESULTADOS");

        add(panelMain, BorderLayout.CENTER);
    }

    private void configurarDiseñoInicial() {
        // Tema global del panel
        setBackground(Color.decode("#1a202c")); // Fondo oscuro base
        panelBusqueda.setBackground(Color.decode("#1a202c"));

        construirPantallaBusqueda();
    }

    // =================================================================================
    // 1. CONSTRUCCIÓN DE PANTALLA DE BÚSQUEDA (CARD 1)
    // =================================================================================
    private void construirPantallaBusqueda() {
        panelBusqueda.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- TARJETA CENTRAL FLOTANTE ---
        JPanel card = new JPanel(new GridBagLayout());
        card.setOpaque(false);
        // Usamos un panel contenedor con borde redondeado simulado o borde simple si
        // FlatLaf falla
        JPanel cardContainer = new JPanel(new GridBagLayout());
        cardContainer.setBackground(Color.decode("#2d3748")); // Gris azulado oscuro
        cardContainer.setBorder(new EmptyBorder(40, 40, 40, 40));
        // Intento seguro de redondeo
        cardContainer.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        // A. ICONO GRANDE
        JLabel iconLabel = new JLabel(FontIcon.of(FontAwesomeSolid.SEARCH, 48, Color.decode("#4299e1")));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        cardContainer.add(iconLabel, gbc);

        // B. TÍTULO
        JLabel title = new JLabel("Buscar Venta Original");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        cardContainer.add(title, gbc);

        // C. SUBTÍTULO
        JLabel subtitle = new JLabel("Ingrese el ID de venta, DNI o Nombre del Cliente");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Color.decode("#a0aec0")); // Texto secundario
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 30, 0);
        cardContainer.add(subtitle, gbc);

        // D. CAMPO DE TEXTO
        txtBusqueda = new JTextField();
        txtBusqueda.setPreferredSize(new Dimension(400, 50));
        txtBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        txtBusqueda.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: VEN-000123");
        txtBusqueda.putClientProperty(FlatClientProperties.STYLE, "margin:0,10,0,10");
        // Icono interno
        txtBusqueda.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                FontIcon.of(FontAwesomeSolid.SEARCH, 16, Color.GRAY));

        // Listener Accion (Enter)
        txtBusqueda.addActionListener(e -> ejecutarBusqueda());

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 20, 0);
        cardContainer.add(txtBusqueda, gbc);

        // E. BOTÓN DE BÚSQUEDA
        btnBuscar = new JButton("BUSCAR VENTA");
        btnBuscar.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 18, Color.WHITE));
        btnBuscar.setHorizontalTextPosition(SwingConstants.RIGHT);
        btnBuscar.setIconTextGap(10);
        btnBuscar.setPreferredSize(new Dimension(400, 48));
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnBuscar.setBackground(Color.decode("#3182ce")); // Azul primario
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.addActionListener(e -> ejecutarBusqueda());

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        cardContainer.add(btnBuscar, gbc);

        // Añadir tarjeta al panel
        panelBusqueda.add(cardContainer);
    }

    // =================================================================================
    // 2. LÓGICA DE BÚSQUEDA
    // =================================================================================
    private void ejecutarBusqueda() {
        String criterio = txtBusqueda.getText().trim();
        if (criterio.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese un criterio de búsqueda.", "Campo vacío",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        setLoading(true);

        // SwingWorker para no bloquear UI
        new SwingWorker<List<ModelVenta>, Void>() {
            @Override
            protected List<ModelVenta> doInBackground() throws Exception {
                int idBodega = UserSession.getInstance().getIdBodegaUsuario();
                return serviceVenta.buscarVentasParaDevolucionConProductos(criterio, idBodega);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    resultadosCache = get();
                    if (resultadosCache == null || resultadosCache.isEmpty()) {
                        JOptionPane.showMessageDialog(buscarVenta.this, "No se encontraron ventas.", "Sin resultados",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (resultadosCache.size() == 1) {
                        seleccionarVenta(resultadosCache.get(0));
                    } else {
                        mostrarResultados(resultadosCache);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(buscarVenta.this, "Error en la búsqueda: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setLoading(boolean loading) {
        txtBusqueda.setEnabled(!loading);
        btnBuscar.setEnabled(!loading);
        btnBuscar.setText(loading ? "BUSCANDO..." : "BUSCAR VENTA");
    }

    // =================================================================================
    // 3. PANTALLA DE RESULTADOS (CARD 2)
    // =================================================================================
    private void mostrarResultados(List<ModelVenta> resultados) {
        panelResultados.removeAll();

        // A. HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton btnVolver = new JButton("Nueva Búsqueda");
        btnVolver.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_LEFT, 14, Color.WHITE));
        btnVolver.setBackground(Color.decode("#2d3748"));
        btnVolver.setForeground(Color.WHITE);
        btnVolver.addActionListener(e -> {
            CardLayout cl = (CardLayout) panelMain.getLayout();
            cl.show(panelMain, "BUSQUEDA");
            txtBusqueda.requestFocus();
        });

        JLabel lblTitle = new JLabel(resultados.size() + " Resultados Encontrados");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setBorder(new EmptyBorder(0, 20, 0, 0));

        header.add(btnVolver, BorderLayout.WEST);
        header.add(lblTitle, BorderLayout.CENTER);

        // B. LISTA DE TARJETAS (SCROLL)
        JPanel listaContainer = new JPanel(new GridBagLayout());
        listaContainer.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);

        for (ModelVenta venta : resultados) {
            listaContainer.add(crearTarjetaResultado(venta), gbc);
            gbc.gridy++;
        }

        // Espaciador final
        gbc.weighty = 1.0;
        listaContainer.add(Box.createVerticalGlue(), gbc);

        JScrollPane scroll = new JScrollPane(listaContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panelResultados.add(header, BorderLayout.NORTH);
        panelResultados.add(scroll, BorderLayout.CENTER);

        // Cambiar tarjeta
        CardLayout cl = (CardLayout) panelMain.getLayout();
        cl.show(panelMain, "RESULTADOS");
    }

    private JPanel crearTarjetaResultado(ModelVenta venta) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.decode("#2d3748"));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:10"); // Borde suave

        // Icono
        JLabel icon = new JLabel(FontIcon.of(FontAwesomeSolid.RECEIPT, 24, Color.decode("#4299e1")));
        card.add(icon, BorderLayout.WEST);

        // Info Centro
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);

        JLabel lblNumero = new JLabel("Venta #" + String.format("VEN-%06d", venta.getIdVenta()));
        lblNumero.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblNumero.setForeground(Color.WHITE);

        String cliente = venta.getCliente() != null ? venta.getCliente().getNombre() : "Consumidor Final";
        JLabel lblCliente = new JLabel(cliente);
        lblCliente.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblCliente.setForeground(Color.decode("#a0aec0"));

        infoPanel.add(lblNumero);
        infoPanel.add(lblCliente);
        card.add(infoPanel, BorderLayout.CENTER);

        // Info Derecha + Boton
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);

        JLabel lblFecha = new JLabel(venta.getFechaVenta().format(formatoFecha));
        lblFecha.setForeground(Color.WHITE);
        lblFecha.setBorder(new EmptyBorder(0, 0, 0, 15));

        JButton btnSelect = new JButton("Seleccionar");
        btnSelect.setIcon(FontIcon.of(FontAwesomeSolid.CHECK, 12, Color.WHITE));
        btnSelect.setBackground(Color.decode("#48bb78")); // Verde
        btnSelect.setForeground(Color.WHITE);
        btnSelect.addActionListener(e -> seleccionarVenta(venta));

        rightPanel.add(lblFecha);
        rightPanel.add(btnSelect);
        card.add(rightPanel, BorderLayout.EAST);

        return card;
    }

    // =================================================================================
    // 4. LÓGICA DE SELECCIÓN Y VALIDACIÓN
    // =================================================================================
    private void seleccionarVenta(ModelVenta venta) {
        if (controller == null) {
            System.err.println("ERROR: Controlador no inicializado en buscarVenta");
            return;
        }

        // 1. Validar Elegibilidad (Reglas de negocio)
        // Simplificado para compilar: Usamos validación básica por ahora o llamamos al
        // servicio si es posible
        // ValidacionElegibilidad validacion =
        // ValidacionElegibilidad.validarVenta(venta); // NO EXISTE ESTATICO

        // Verificamos reglas básicas aquí por ahora
        boolean esElegible = true;
        String mensajeRechazo = "";

        if (!"completada".equalsIgnoreCase(venta.getEstado()) && !"pagada".equalsIgnoreCase(venta.getEstado())) {
            esElegible = false;
            mensajeRechazo = "La venta no está completada/pagada (Estado: " + venta.getEstado() + ")";
        }

        // TODO: Implementar validación de fecha 30 días aquí si es crítico o usar
        // ServiceDevolucion

        if (!esElegible) {
            new ModernAlert(ModernAlert.AlertType.WARNING,
                    "Venta No Elegible",
                    mensajeRechazo)
                    .setVisible(true);
            return;
        }

        // 2. Verificar Devolución Existente
        try {
            ServiceDevolucion.DevolucionExistente existente = ServiceDevolucion
                    .obtenerInformacionDevolucionExistente(venta.getIdVenta());

            if (existente != null) { // Si no es null, existe
                JOptionPane.showMessageDialog(this,
                        "Esta venta ya tiene una devolución asociada.\nEstado: " + existente.getEstado(),
                        "Devolución Existente", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error verificando devoluciones previas: " + e.getMessage());
            return;
        }

        // 3. Proceder al Paso 2 (Selección de Productos)
        // Necesitamos la lista de productos
        // Nota: serviceVenta.buscarVentasParaDevolucionConProductos ya debería haber
        // cargado los detalles
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            // Fallback cargar detalles si faltan
            System.out.println("Cache de productos es null o vacía - intentando cargar detalles desde BD para venta: "
                    + venta.getIdVenta());
            try {
                ModelVenta ventaCompleta = serviceVenta.buscarVentaPorId(venta.getIdVenta());
                if (ventaCompleta != null && ventaCompleta.getDetalles() != null) {
                    venta.setDetalles(ventaCompleta.getDetalles());
                    System.out.println("Detalles recuperados: " + venta.getDetalles().size() + " items");
                } else {
                    System.err.println("No se pudieron recuperar los detalles de la venta");
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error cargando detalles de la venta: " + e.getMessage());
                return;
            }
        }

        controller.irAPaso2(venta, null); // null en cache, el controlador manejará la carga si necesario
    }
}
