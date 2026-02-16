package raven.application.form.comercial.devolucion;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import javax.swing.*;
import raven.clases.comercial.ServiceAutorizacion;
import raven.controlador.comercial.ModelDevolucion;
import raven.controlador.comercial.ModelNotaCredito;
import raven.clases.admin.UserSession; // Import agregado
import java.math.BigDecimal;

/**
 * Diálogo modal para autorización de devoluciones con validación de
 * credenciales
 * 
 * PATRÓN: Dialog con validación en tiempo real
 * UI: FlatLaf styling para interfaz moderna
 * 
 * @author Sistema
 * @version 1.0
 */
public class DialogoAutorizacion extends JDialog {

    private final ServiceAutorizacion serviceAutorizacion;
    private final ModelDevolucion devolucion;
    private static final BigDecimal MONTO_AUTORIZACION_STRICTA = new BigDecimal("500000.00");
    private boolean requiereAdmin;

    // Componentes UI
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextArea txtObservaciones;
    private JButton btnAprobar;
    private JButton btnRechazar;
    private JButton btnCancelar;

    private boolean autorizado = false;
    private boolean aprobada = false;
    private int idUsuarioAutoriza = 0;

    /**
     * Constructor del diálogo
     * 
     * @param parent     Frame padre
     * @param devolucion Devolución a autorizar
     */
    public DialogoAutorizacion(Frame parent, ModelDevolucion devolucion) {
        super(parent, "Autorización de Devolución", true);
        this.devolucion = devolucion;
        this.serviceAutorizacion = new ServiceAutorizacion();

        // Determinar si requiere autorización estricta
        BigDecimal total = devolucion.getTotalDevolucion() != null ? devolucion.getTotalDevolucion() : BigDecimal.ZERO;
        this.requiereAdmin = total.compareTo(MONTO_AUTORIZACION_STRICTA) >= 0;

        inicializarComponentes();
        configurarUI();
        cargarDatos();
        configurarModoAutorizacion(); // Nuevo método

        // Centrar en pantalla
        setLocationRelativeTo(parent);
    }

    private void configurarModoAutorizacion() {
        if (!requiereAdmin) {
            // Modo simplificado: Deshabilitar campos de credenciales
            txtUsername.setText(UserSession.getInstance().getCurrentUser().getUsername());
            txtUsername.setEditable(false);
            txtPassword.setText("AUTORIZADO_SISTEMA");
            txtPassword.setEnabled(false);

            // Añadir nota visual (opcional, hack usando borde)
            ((JPanel) txtUsername.getParent())
                    .setBorder(BorderFactory.createTitledBorder("Autorización Simplificada (Monto < 500k)"));
        }
    }

    /**
     * Inicializa todos los componentes del diálogo
     */
    private void inicializarComponentes() {
        setSize(500, 450);
        setLayout(new BorderLayout(10, 10));

        // Panel principal con padding
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel de información de la devolución
        JPanel panelInfo = crearPanelInformacion();

        // Panel de credenciales
        JPanel panelCredenciales = crearPanelCredenciales();

        // Panel de observaciones
        JPanel panelObservaciones = crearPanelObservaciones();

        // Panel de botones
        JPanel panelBotones = crearPanelBotones();

        // Agregar componentes
        JPanel panelSuperior = new JPanel(new GridLayout(2, 1, 10, 10));
        panelSuperior.add(panelInfo);
        panelSuperior.add(panelCredenciales);

        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);
        panelPrincipal.add(panelObservaciones, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    /**
     * Crea panel con información de la devolución
     */
    private JPanel crearPanelInformacion() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Información de la Devolución"));

        panel.add(new JLabel("Número:"));
        JLabel lblNumero = new JLabel();
        panel.add(lblNumero);

        panel.add(new JLabel("Total:"));
        JLabel lblTotal = new JLabel();
        panel.add(lblTotal);

        panel.add(new JLabel("Motivo:"));
        JLabel lblMotivo = new JLabel();
        panel.add(lblMotivo);

        // Guardar referencias para cargar datos después
        lblNumero.setName("lblNumero");
        lblTotal.setName("lblTotal");
        lblMotivo.setName("lblMotivo");

        return panel;
    }

    /**
     * Crea panel de credenciales de administrador
     */
    private JPanel crearPanelCredenciales() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Credenciales de Administrador"));

        panel.add(new JLabel("Usuario:"));
        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Ingrese usuario administrador");
        panel.add(txtUsername);

        panel.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Ingrese contraseña");
        panel.add(txtPassword);

        return panel;
    }

    /**
     * Crea panel de observaciones
     */
    private JPanel crearPanelObservaciones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Observaciones de Autorización"));

        txtObservaciones = new JTextArea(5, 30);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(txtObservaciones);
        panel.add(scroll);

        return panel;
    }

    /**
     * Crea panel de botones
     */
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnAprobar = new JButton("Aprobar");
        btnAprobar.putClientProperty(FlatClientProperties.STYLE,
                "background:#4CAF50;foreground:#FFFFFF");
        btnAprobar.addActionListener(e -> procesarAutorizacion(true));

        btnRechazar = new JButton("Rechazar");
        btnRechazar.putClientProperty(FlatClientProperties.STYLE,
                "background:#F44336;foreground:#FFFFFF");
        btnRechazar.addActionListener(e -> procesarAutorizacion(false));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        panel.add(btnAprobar);
        panel.add(btnRechazar);
        panel.add(btnCancelar);

        return panel;
    }

    /**
     * Configura estilos UI
     */
    private void configurarUI() {
        getRootPane().putClientProperty(FlatClientProperties.STYLE,
                "arc:20");
    }

    /**
     * Carga datos de la devolución en la UI
     */
    private void cargarDatos() {
        if (devolucion != null) {
            // Buscar labels por nombre
            Component[] components = ((JPanel) ((JPanel) getContentPane()
                    .getComponent(0)).getComponent(0)).getComponent(0).getParent().getComponents();

            for (Component comp : getAllComponents(getContentPane())) {
                if (comp instanceof JLabel) {
                    JLabel lbl = (JLabel) comp;
                    String name = lbl.getName();

                    if ("lblNumero".equals(name)) {
                        lbl.setText(devolucion.getNumeroDevolucion());
                    } else if ("lblTotal".equals(name)) {
                        lbl.setText(String.format("$%.2f", devolucion.getTotalDevolucion()));
                    } else if ("lblMotivo".equals(name)) {
                        lbl.setText(devolucion.getMotivo().getDescripcion());
                    }
                }
            }
        }
    }

    /**
     * Obtiene todos los componentes recursivamente
     */
    private java.util.List<Component> getAllComponents(Container container) {
        java.util.List<Component> list = new java.util.ArrayList<>();
        for (Component comp : container.getComponents()) {
            list.add(comp);
            if (comp instanceof Container) {
                list.addAll(getAllComponents((Container) comp));
            }
        }
        return list;
    }

    /**
     * Procesa la autorización (aprobar/rechazar)
     */
    /**
     * Procesa la autorización (aprobar/rechazar)
     */
    private void procesarAutorizacion(boolean aprobar) {
        try {
            // 1. Validar campos
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            String observaciones = txtObservaciones.getText().trim();

            // Validar credenciales solo si requiere admin
            if (requiereAdmin) {
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Debe ingresar usuario y contraseña de administrador",
                            "Credenciales Requeridas", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            if (observaciones.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Debe ingresar observaciones de autorización",
                        "Observaciones Requeridas", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Validar credenciales (o asignar usuario actual)
            if (requiereAdmin) {
                idUsuarioAutoriza = serviceAutorizacion.validarCredencialesAdministrador(
                        username, password);

                if (idUsuarioAutoriza == 0) {
                    JOptionPane.showMessageDialog(this,
                            "Credenciales inválidas o usuario sin permisos de autorización",
                            "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                // Modo simplificado: Usar usuario actual
                idUsuarioAutoriza = UserSession.getInstance().getCurrentUser().getIdUsuario();
            }

            // 3. Confirmar acción
            String accion = aprobar ? "APROBAR" : "RECHAZAR";
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    String.format("¿Está seguro de %s esta devolución?\n\n" +
                            "Número: %s\nTotal: $%.2f",
                            accion, devolucion.getNumeroDevolucion(),
                            devolucion.getTotalDevolucion()),
                    "Confirmar Autorización", JOptionPane.YES_NO_OPTION);

            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }

            // 4. Procesar autorización y obtener nota de crédito
            ModelNotaCredito notaCredito = serviceAutorizacion.autorizarYObtenerNotaCredito(
                    devolucion.getIdDevolucion(),
                    idUsuarioAutoriza,
                    aprobar,
                    observaciones);

            this.autorizado = true;
            this.aprobada = aprobar;

            if (aprobar && notaCredito != null) {
                // Mostrar mensaje de éxito con número de nota
                String mensaje = String.format(
                        "Devolución APROBADA exitosamente\n\n" +
                                "Se generó la Nota de Crédito:\n%s\n\n" +
                                "Saldo disponible: $%.2f\n" +
                                "Válida hasta: %s",
                        notaCredito.getNumeroNotaCredito(),
                        notaCredito.getSaldoDisponible(),
                        notaCredito.getFechaVencimiento() != null
                                ? notaCredito.getFechaVencimiento().format(
                                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : "N/A");

                JOptionPane.showMessageDialog(this, mensaje,
                        "Autorización Exitosa", JOptionPane.INFORMATION_MESSAGE);

                // CERRAR VENTANA ACTUAL ANTES DE PREGUNTAR
                dispose();

                // Preguntar si desea ver/imprimir la nota de crédito
                // Usamos null como parent para que no dependa de esta ventana ya cerrada
                int opcion = JOptionPane.showConfirmDialog(null,
                        "¿Desea ver e imprimir la Nota de Crédito?",
                        "Nota de Crédito", JOptionPane.YES_NO_OPTION);

                if (opcion == JOptionPane.YES_OPTION) {
                    // Obtener frame principal (aunque ya cerramos, podemos intentar obtenerlo o
                    // usar null)
                    // Mejor usar null o buscar la ventana activa
                    Frame parentFrame = null;
                    if (getOwner() instanceof Frame) {
                        parentFrame = (Frame) getOwner();
                    }

                    DialogoNotaCredito dialogoNC = new DialogoNotaCredito(parentFrame, notaCredito);
                    dialogoNC.setVisible(true);
                }

            } else if (!aprobar) {
                JOptionPane.showMessageDialog(this,
                        "Devolución RECHAZADA exitosamente",
                        "Autorización Exitosa", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }

            // dispose() ya fue llamado en los bloques if/else if
            // Si no entró a ninguno (caso raro), asegurarse de cerrar
            if (isVisible()) {
                dispose();
            }

        } catch (Exception e) {
            System.err.println("Error procesando autorización: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error procesando autorización: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Getters para verificar resultado
    public boolean isAutorizado() {
        return autorizado;
    }

    public boolean isAprobada() {
        return aprobada;
    }

    public int getIdUsuarioAutoriza() {
        return idUsuarioAutoriza;
    }
}