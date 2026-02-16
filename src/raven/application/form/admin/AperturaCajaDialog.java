package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.clases.admin.UserSession;
import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.modal.Toast;
import raven.application.Application;
import raven.componentes.LoadingOverlayHelper;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo para apertura de caja registradora CON VALIDACIÓN DE SESIÓN Y
 * CALLBACK.
 * 
 * MODIFICADO:
 * - Valida sesión activa antes de abrir caja
 * - Asocia automáticamente la caja a la sesión del usuario
 * - Ejecuta callback al finalizar (éxito o cancelación)
 * 
 * @author Sistema
 * @version 3.1
 */
public class AperturaCajaDialog extends JDialog {

    // ==================== COMPONENTES UI ====================
    private JLabel lblTitulo;
    private JLabel lblCaja;
    private JLabel lblUsuario;
    private JLabel lblFechaHora;
    private JLabel lblSesion;
    private JTextField txtMontoInicial;
    private JTextArea txtObservaciones;
    private JButton btnAbrir;
    private JButton btnCancelar;

    // ==================== SERVICIOS Y DATOS ====================
    private final ServiceCajaMovimiento serviceCaja;
    private final ModelCaja caja;
    private final int idUsuario;
    private final String nombreUsuario;
    private ModelCajaMovimiento movimientoCreado;
    private boolean aperturaExitosa = false;

    // SUCCESS NUEVO: Callback para manejar resultado
    private final AperturaCajaCallback callback;

    // Timer para actualizar estado de sesión
    private Timer timerSesion;

    // ==================== INTERFAZ CALLBACK ====================

    /**
     * SUCCESS NUEVO: Interfaz para callback de resultado de apertura.
     */
    public interface AperturaCajaCallback {
        /**
         * Se ejecuta cuando la apertura es exitosa.
         * 
         * @param movimiento Movimiento de caja creado
         */
        void onAperturaExitosa(ModelCajaMovimiento movimiento);

        /**
         * Se ejecuta cuando se cancela la apertura.
         */
        void onAperturaCancelada();
    }

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor CON callback.
     * 
     * @param parent        Ventana padre
     * @param caja          Caja a abrir
     * @param idUsuario     ID del usuario que abre
     * @param nombreUsuario Nombre del usuario
     * @param callback      Callback para manejar resultado
     */
    public AperturaCajaDialog(Frame parent, ModelCaja caja, int idUsuario,
            String nombreUsuario, AperturaCajaCallback callback) {
        super(parent, "Apertura de Caja", true);

        this.serviceCaja = new ServiceCajaMovimiento();
        this.caja = caja;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.callback = callback; // SUCCESS NUEVO

        // Validar sesión activa ANTES de inicializar
        if (!validarSesionActiva()) {
            JOptionPane.showMessageDialog(parent,
                    "No hay una sesión activa.\n" +
                            "Por favor, inicie sesión nuevamente.",
                    "Sesión Inválida",
                    JOptionPane.ERROR_MESSAGE);

            dispose();
            Application.logout();
            return;
        }

        initComponents();
        configurarEstilos();
        cargarDatosIniciales();
        iniciarMonitorSesion();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    /**
     * Constructor SIN callback (para compatibilidad con código existente).
     * 
     * @deprecated Usar constructor con callback
     */
    @Deprecated
    public AperturaCajaDialog(Frame parent, ModelCaja caja, int idUsuario, String nombreUsuario) {
        this(parent, caja, idUsuario, nombreUsuario, null);
    }

    // ==================== VALIDACIÓN DE SESIÓN ====================

    private boolean validarSesionActiva() {
        if (!UserSession.getInstance().isLoggedIn()) {
            System.err.println("ERROR  No hay sesión activa");
            return false;
        }

        String token = UserSession.getInstance().getSessionToken();
        if (token == null || token.isEmpty()) {
            System.err.println("ERROR  Token de sesión inválido");
            return false;
        }

        if (UserSession.getInstance().getCurrentUser().getIdUsuario() != idUsuario) {
            System.err.println("ERROR  ID de usuario no coincide con sesión actual");
            return false;
        }

        System.out.println("SUCCESS  Sesión validada correctamente");
        return true;
    }

    private void iniciarMonitorSesion() {
        timerSesion = new Timer(5000, e -> {
            if (!validarSesionActiva()) {
                detenerMonitorSesion();

                JOptionPane.showMessageDialog(this,
                        "Su sesión ha expirado.\n" +
                                "El diálogo se cerrará.",
                        "Sesión Expirada",
                        JOptionPane.WARNING_MESSAGE);

                dispose();
                Application.logout();
            } else {
                actualizarIndicadorSesion();
            }
        });

        timerSesion.start();
        System.out.println("Actualizando Monitor de sesión iniciado");
    }

    private void detenerMonitorSesion() {
        if (timerSesion != null) {
            timerSesion.stop();
            timerSesion = null;
            System.out.println(" Monitor de sesión detenido");
        }
    }

    private void actualizarIndicadorSesion() {
        if (lblSesion != null) {
            String token = UserSession.getInstance().getSessionToken();
            if (token != null && !token.isEmpty()) {
                lblSesion.setText("Sesión Activa");
                lblSesion.setForeground(new Color(0, 153, 51));
            } else {
                lblSesion.setText("Sesión Inválida");
                lblSesion.setForeground(new Color(255, 59, 48));
            }
        }
    }

    // ==================== INICIALIZACIÓN DE COMPONENTES ====================

    private void initComponents() {
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new BorderLayout(0, 20));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel panelSuperior = crearPanelSuperior();
        JPanel panelCentral = crearPanelFormulario();
        JPanel panelBotones = crearPanelBotones();

        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        lblTitulo = new JLabel("Apertura de Caja");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSesion = new JLabel("Sesión Activa");
        lblSesion.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSesion.setForeground(new Color(0, 153, 51));
        lblSesion.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblCaja = new JLabel();
        lblCaja.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblCaja.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblUsuario = new JLabel();
        lblUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblFechaHora = new JLabel();
        lblFechaHora.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblFechaHora.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator separador = new JSeparator();
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        panel.add(lblTitulo);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblSesion);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(lblCaja);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(lblUsuario);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(lblFechaHora);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(separador);

        return panel;
    }

    private JPanel crearPanelFormulario() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel lblMontoInicial = new JLabel("Monto Inicial Base:");
        lblMontoInicial.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtMontoInicial = new JTextField(15);
        txtMontoInicial.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        txtMontoInicial.setText("0");

        txtMontoInicial.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c) && c != '.' && c != '\b') {
                    evt.consume();
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(lblMontoInicial, gbc);

        gbc.gridy = 1;
        panel.add(txtMontoInicial, gbc);

        JLabel lblObservaciones = new JLabel("Observaciones (Opcional):");
        lblObservaciones.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtObservaciones = new JTextArea(4, 30);
        txtObservaciones.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        JScrollPane scrollObservaciones = new JScrollPane(txtObservaciones);

        gbc.gridy = 2;
        panel.add(lblObservaciones, gbc);

        gbc.gridy = 3;
        panel.add(scrollObservaciones, gbc);

        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(120, 40));
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancelar.addActionListener(e -> cancelarApertura()); // SUCCESS MODIFICADO

        btnAbrir = new JButton("Abrir Caja");
        btnAbrir.setPreferredSize(new Dimension(140, 40));
        btnAbrir.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAbrir.addActionListener(e -> abrirCaja());

        panel.add(btnCancelar);
        panel.add(btnAbrir);

        return panel;
    }

    // ==================== CONFIGURACIÓN DE ESTILOS ====================

    private void configurarEstilos() {
        txtMontoInicial.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "Ingrese el monto base inicial");
        txtMontoInicial.putClientProperty(
                FlatClientProperties.STYLE,
                "arc:10");

        txtObservaciones.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                "Ej: Apertura turno mañana");
        // Nota: FlatTextAreaUI no soporta la propiedad 'arc'
        // Se elimina para evitar UnknownStyleException

        FlatSVGIcon iconAbrir = new FlatSVGIcon("raven/icon/svg/caja.svg", 18, 18);
        iconAbrir.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
        btnAbrir.setIcon(iconAbrir);
        btnAbrir.setIconTextGap(8);
        btnAbrir.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnAbrir.putClientProperty(FlatClientProperties.STYLE,
                "arc:12;"
                        + "focusWidth:0;"
                        + "borderWidth:0;"
                        + "background:#0A84FF;"
                        + "hoverBackground:#409CFF;"
                        + "pressedBackground:#0077E6;"
                        + "foreground:#FFFFFF;"
                        + "font:bold 13;"
                        + "margin:8,16,8,16");

        FlatSVGIcon iconCancelar = new FlatSVGIcon("raven/icon/svg/dashboard/x-circle.svg", 18, 18);
        iconCancelar.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
        btnCancelar.setIcon(iconCancelar);
        btnCancelar.setIconTextGap(8);
        btnCancelar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnCancelar.putClientProperty(FlatClientProperties.STYLE,
                "arc:12;"
                        + "focusWidth:0;"
                        + "borderWidth:0;"
                        + "background:#374151;"
                        + "hoverBackground:#4b5563;"
                        + "pressedBackground:#1f2937;"
                        + "foreground:#FFFFFF;"
                        + "font:bold 13;"
                        + "margin:8,16,8,16");
    }

    // ==================== CARGA DE DATOS ====================

    private void cargarDatosIniciales() {
        lblCaja.setText("Caja: " + caja.toDisplayString());
        lblUsuario.setText("Usuario: " + nombreUsuario);

        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();
        actualizarFechaHora();

        SwingUtilities.invokeLater(() -> {
            txtMontoInicial.requestFocus();
            txtMontoInicial.selectAll();
        });
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        lblFechaHora.setText("Fecha/Hora: " + ahora.format(formatter));
    }

    // ==================== LÓGICA DE APERTURA ====================

    /**
     * SUCCESS NUEVO: Maneja la cancelación de apertura.
     */
    private void cancelarApertura() {
        System.out.println("ERROR  Usuario canceló la apertura de caja");

        detenerMonitorSesion();

        // SUCCESS CORREGIDO: Ejecutar callback ANTES de dispose para que MainForm
        // reciba la
        // señal
        if (callback != null) {
            callback.onAperturaCancelada();
        }

        dispose();
    }

    /**
     * Ejecuta la apertura de la caja CON CALLBACK.
     * 
     * MODIFICADO: Ejecuta callback al finalizar.
     */
    private void abrirCaja() {
        if (!validarSesionActiva()) {
            JOptionPane.showMessageDialog(this,
                    "Su sesión ha expirado.\n" +
                            "Por favor, inicie sesión nuevamente.",
                    "Sesión Expirada",
                    JOptionPane.WARNING_MESSAGE);

            detenerMonitorSesion();
            dispose();
            Application.logout();
            return;
        }

        String montoTexto = txtMontoInicial.getText().trim();
        if (montoTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe ingresar un monto inicial",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            txtMontoInicial.requestFocus();
            return;
        }

        BigDecimal montoInicial;
        try {
            montoInicial = new BigDecimal(montoTexto);

            if (montoInicial.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this,
                        "El monto inicial no puede ser negativo",
                        "Validación", JOptionPane.WARNING_MESSAGE);
                txtMontoInicial.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "El monto inicial debe ser un número válido",
                    "Validación", JOptionPane.ERROR_MESSAGE);
            txtMontoInicial.requestFocus();
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                String.format("¿Confirma la apertura de la caja con $%,.2f?",
                        montoInicial.doubleValue()),
                "Confirmar Apertura",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        btnAbrir.setEnabled(false);
        btnCancelar.setEnabled(false);
        LoadingOverlayHelper.showLoading(getRootPane(), "Abriendo caja...");

        SwingWorker<ModelCajaMovimiento, Void> worker = new SwingWorker<ModelCajaMovimiento, Void>() {
            @Override
            protected ModelCajaMovimiento doInBackground() throws Exception {
                if (!validarSesionActiva()) {
                    throw new IllegalStateException("Sesión expirada durante el proceso");
                }

                return serviceCaja.abrirCaja(
                        caja.getIdCaja(),
                        idUsuario,
                        montoInicial,
                        txtObservaciones.getText().trim());
            }

            @Override
            protected void done() {
                LoadingOverlayHelper.hideLoading(getRootPane());
                try {
                    movimientoCreado = get();
                    aperturaExitosa = true;

                    // Asociar caja a la sesión
                    UserSession.getInstance().asociarCaja(
                            caja.getIdCaja(),
                            movimientoCreado.getIdMovimiento());

                    JOptionPane.showMessageDialog(AperturaCajaDialog.this,
                            "SUCCESS  Caja abierta exitosamente\n\n" +
                                    "Movimiento #" + movimientoCreado.getIdMovimiento(),
                            "Apertura Exitosa",
                            JOptionPane.INFORMATION_MESSAGE);

                    detenerMonitorSesion();

                    // SUCCESS CORREGIDO: Ejecutar callback ANTES de dispose()
                    // Esto asegura que MainForm reciba la señal de éxito antes de que el diálogo
                    // modal se cierre
                    if (callback != null) {
                        callback.onAperturaExitosa(movimientoCreado);
                    }

                    dispose();

                } catch (Exception e) {
                    btnAbrir.setEnabled(true);
                    btnCancelar.setEnabled(true);

                    String mensaje = e.getMessage();
                    if (mensaje != null && mensaje.contains("Sesión expirada")) {
                        JOptionPane.showMessageDialog(AperturaCajaDialog.this,
                                "WARNING  Su sesión expiró durante el proceso.\n" +
                                        "Por favor, inicie sesión nuevamente.",
                                "Sesión Expirada", JOptionPane.WARNING_MESSAGE);

                        detenerMonitorSesion();
                        dispose();
                        Application.logout();
                    } else {
                        JOptionPane.showMessageDialog(AperturaCajaDialog.this,
                                "ERROR  Error al abrir la caja:\n" + mensaje,
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };

        worker.execute();
    }

    // ==================== CLEANUP ====================

    @Override
    public void dispose() {
        detenerMonitorSesion();
        super.dispose();
    }

    // ==================== GETTERS ====================

    public boolean isAperturaExitosa() {
        return aperturaExitosa;
    }

    public ModelCajaMovimiento getMovimientoCreado() {
        return movimientoCreado;
    }
}
