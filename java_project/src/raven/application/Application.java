package raven.application;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.UIManager;
import raven.application.form.LoginForm;
import raven.application.form.MainForm;
import raven.application.form.other.FormDashboard;
import raven.clases.admin.ServiceUser;
import raven.clases.admin.SessionPersistence;
import raven.clases.admin.ServiceSession;
import raven.clases.admin.UserSession;
import raven.componentes.notificacion.Notification;
import java.awt.Image;
import java.awt.Toolkit;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.DatabaseInitializer;
import raven.controlador.principal.AppConfig;
import raven.controlador.update.UpdateManager;
import raven.toast.Notifications;
import raven.utils.NotificacionesService;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.WString;

/**
 *
 * @author Raven
 */
public class Application extends javax.swing.JFrame {

    public static Application app;
    private MainForm mainForm;
    private final LoginForm loginForm;
    private final boolean UNDECORATED = false;
    private volatile boolean adminResetDialogOpen = false;

    public Application() {
        // Inicializar base de datos y esperar si es necesario antes de cargar la sesión
        new Thread(() -> {
            try {
                DatabaseInitializer.initializeDatabase();
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    Notifications.getInstance().show(Notifications.Type.ERROR,
                            "Error al inicializar la base de datos: " + e.getMessage());
                });
            } finally {
                // Una vez que la BD está lista (o falló), procedemos con la sesión
                SwingUtilities.invokeLater(this::initializeSession);
            }
        }, "DB-Initializer").start();

        app = this;
        initComponents();
        configurarVentana();
        configurarIcono();

        loginForm = new LoginForm();
        setContentPane(loginForm);
        Notifications.getInstance().setJFrame(this);
        instalarAtajoAdminReset();
    }

    private void instalarAtajoAdminReset() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) {
                    return false;
                }
                if (e.getKeyCode() != KeyEvent.VK_C) {
                    return false;
                }
                if (!e.isControlDown() || !e.isShiftDown()) {
                    return false;
                }
                if (adminResetDialogOpen) {
                    return true;
                }
                SwingUtilities.invokeLater(Application.this::iniciarFlujoAdminResetPassword);
                return true;
            }
        });
    }

    private boolean esRolAdmin(String rol) {
        return rol != null && "admin".equalsIgnoreCase(rol.trim());
    }

    private JPanel crearFormularioCredenciales(String tituloUsuario, JTextField userField, JPasswordField passField) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 10);
        p.add(new JLabel(tituloUsuario), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(userField, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        p.add(new JLabel("Contraseña"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(passField, gbc);
        return p;
    }

    private void iniciarFlujoAdminResetPassword() {
        if (adminResetDialogOpen) {
            return;
        }
        adminResetDialogOpen = true;

        JTextField adminUser = new JTextField(18);
        JPasswordField adminPass = new JPasswordField(18);
        JPanel cred = crearFormularioCredenciales("Usuario admin", adminUser, adminPass);

        int opt = JOptionPane.showConfirmDialog(
                this,
                cred,
                "Iniciar sesión (Admin)",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opt != JOptionPane.OK_OPTION) {
            adminResetDialogOpen = false;
            return;
        }

        String username = adminUser.getText() == null ? "" : adminUser.getText().trim();
        char[] passChars = adminPass.getPassword();
        String password = passChars == null ? "" : new String(passChars);
        if (passChars != null) {
            java.util.Arrays.fill(passChars, '\0');
        }

        if (username.isEmpty() || password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Ingrese usuario y contraseña.", "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            adminResetDialogOpen = false;
            return;
        }

        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        new javax.swing.SwingWorker<raven.controlador.admin.ModelUser, Void>() {
            @Override
            protected raven.controlador.admin.ModelUser doInBackground() throws Exception {
                ServiceUser s = new ServiceUser();
                return s.authenticate(username, password);
            }

            @Override
            protected void done() {
                setCursor(java.awt.Cursor.getDefaultCursor());
                raven.controlador.admin.ModelUser admin;
                try {
                    admin = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Application.this, "Error validando admin: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    adminResetDialogOpen = false;
                    return;
                }

                if (admin == null || !esRolAdmin(admin.getRol())) {
                    JOptionPane.showMessageDialog(Application.this, "Solo un usuario con rol admin puede continuar.",
                            "Acceso denegado", JOptionPane.ERROR_MESSAGE);
                    adminResetDialogOpen = false;
                    return;
                }

                solicitarUsuarioObjetivoParaReset();
            }
        }.execute();
    }

    private void solicitarUsuarioObjetivoParaReset() {
        String usernameTargetInput = JOptionPane.showInputDialog(this, "Digite el usuario a buscar", "Buscar usuario",
                JOptionPane.PLAIN_MESSAGE);
        if (usernameTargetInput == null) {
            adminResetDialogOpen = false;
            return;
        }
        final String usernameTarget = usernameTargetInput.trim();
        if (usernameTarget.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe digitar un usuario.", "Dato inválido",
                    JOptionPane.WARNING_MESSAGE);
            adminResetDialogOpen = false;
            return;
        }

        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        new javax.swing.SwingWorker<raven.controlador.admin.ModelUser, Void>() {
            @Override
            protected raven.controlador.admin.ModelUser doInBackground() throws Exception {
                ServiceUser s = new ServiceUser();
                return s.obtenerUsuarioPorUsername(usernameTarget);
            }

            @Override
            protected void done() {
                setCursor(java.awt.Cursor.getDefaultCursor());
                raven.controlador.admin.ModelUser user;
                try {
                    user = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Application.this, "Error buscando usuario: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    adminResetDialogOpen = false;
                    return;
                }

                if (user == null) {
                    JOptionPane.showMessageDialog(Application.this, "Usuario no encontrado.", "Sin resultados",
                            JOptionPane.WARNING_MESSAGE);
                    adminResetDialogOpen = false;
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        Application.this,
                        "Usuario encontrado:\n\n"
                                + "Usuario: " + user.getUsername() + "\n"
                                + "Nombre: " + user.getNombre() + "\n"
                                + "Rol: " + user.getRol() + "\n\n"
                                + "¿Desea asignar una nueva contraseña?",
                        "Confirmar usuario",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

                if (confirm != JOptionPane.OK_OPTION) {
                    adminResetDialogOpen = false;
                    return;
                }

                solicitarNuevaPasswordYActualizar(user);
            }
        }.execute();
    }

    private void solicitarNuevaPasswordYActualizar(raven.controlador.admin.ModelUser user) {
        JPasswordField p1 = new JPasswordField(18);
        JPasswordField p2 = new JPasswordField(18);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 10);
        p.add(new JLabel("Nueva contraseña"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(p1, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        p.add(new JLabel("Confirmar contraseña"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(p2, gbc);

        int opt = JOptionPane.showConfirmDialog(
                this,
                p,
                "Asignar contraseña a: " + user.getUsername(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opt != JOptionPane.OK_OPTION) {
            adminResetDialogOpen = false;
            return;
        }

        char[] c1 = p1.getPassword();
        char[] c2 = p2.getPassword();
        String s1 = c1 == null ? "" : new String(c1);
        String s2 = c2 == null ? "" : new String(c2);
        if (c1 != null)
            java.util.Arrays.fill(c1, '\0');
        if (c2 != null)
            java.util.Arrays.fill(c2, '\0');

        if (s1.isBlank() || s2.isBlank()) {
            JOptionPane.showMessageDialog(this, "Debe digitar la contraseña dos veces.", "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            adminResetDialogOpen = false;
            return;
        }
        if (!s1.equals(s2)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            adminResetDialogOpen = false;
            return;
        }

        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        String newPassword = s1;
        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ServiceUser s = new ServiceUser();
                raven.controlador.admin.ModelUser dbUser = s.obtenerUsuarioPorUsername(user.getUsername());
                if (dbUser == null) {
                    throw new SQLException("Usuario no encontrado");
                }
                dbUser.setPassword(newPassword);
                s.modificarUsuario(dbUser);
                return null;
            }

            @Override
            protected void done() {
                setCursor(java.awt.Cursor.getDefaultCursor());
                try {
                    get();
                    JOptionPane.showMessageDialog(Application.this, "Contraseña actualizada correctamente.", "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Application.this, "Error actualizando contraseña: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    adminResetDialogOpen = false;
                }
            }
        }.execute();
    }

    /**
     * Configura las propiedades de la ventana principal.
     */
    private void configurarVentana() {
        setSize(new Dimension(1920, 1080));
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        if (UNDECORATED) {
            setUndecorated(true);
            setBackground(new Color(0, 0, 0, 0));
        } else {
            getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        }
    }

    /*
     * Definir Icono para barra de tareas
     */
    private void configurarIcono() {
        setTitle(AppConfig.name);
        String ruta = AppConfig.logopng;
        java.net.URL url = getClass().getResource(ruta);

        if (url == null) {
            System.err.println("WARNING  No se encontró el icono en: " + ruta);
            return;
        }

        Image icon = Toolkit.getDefaultToolkit().getImage(url);
        setIconImage(icon);
        try {
            java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
            if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(icon);
            }
        } catch (Throwable ignore) {
        }
        try {
            Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString(AppConfig.name));
        } catch (Throwable ignore) {
        }
    }

    /**
     * Verifica si existe una sesión guardada válida.
     */
    private void initializeSession() {
        try {
            if (SessionPersistence.hasStoredSession()) {
                SessionPersistence.clearSession();
                System.out.println("INFO Sesión guardada encontrada y limpiada: se requerirá usuario y contraseña");
            } else {
                System.out.println("INFO No hay sesión guardada: se requerirá usuario y contraseña");
            }
        } catch (Exception e) {
            System.err.println("WARN Error limpiando sesión guardada: " + e.getMessage());
        }
        // Siempre exigir login manual; no restaurar sesión automáticamente
    }

    /**
     * Inicia un timer para limpiar sesiones expiradas cada 5 minutos.
     */
    private void iniciarLimpiezaAutomatica() {
        java.util.Timer timer = new java.util.Timer("SessionCleaner", true);

        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    ServiceSession serviceSession = new ServiceSession();
                    int limpiadas = serviceSession.limpiarSesionesExpiradas();

                    if (limpiadas > 0) {
                        System.out.println("INFO Limpiador automático: " + limpiadas + " sesiones");
                    }
                } catch (SQLException e) {
                    System.err.println("WARN Error en limpieza automática: " + e.getMessage());
                }
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000); // Cada 5 minutos

        System.out.println("INFO Limpiador automático de sesiones iniciado");
    }

    private boolean checkExistingSession() {
        try {
            ModelUser savedUser = SessionPersistence.loadSession();
            if (savedUser != null) {
                // Verificar si la sesión es válida (por ejemplo, no ha expirado)
                if (validateSession(savedUser)) {
                    UserSession.getInstance().setCurrentUser(savedUser);
                    UserSession.getInstance().setUserLocation(savedUser.getUbicacion());
                    return true;
                } else {
                    // Si la sesión no es válida, limpiarla
                    SessionPersistence.clearSession();
                }
            }
        } catch (Exception e) {
            Notifications.getInstance().show(Notifications.Type.WARNING,
                    "Error al verificar sesión existente: " + e.getMessage());
        }
        return false;
    }

    private boolean validateSession(ModelUser user) {
        // Aquí puedes agregar lógica adicional para validar la sesión
        // Por ejemplo, verificar fecha de última actividad, token de sesión, etc.
        return user != null && user.getNombre() != null && user.getRol() != null;
    }

    /**
     * Muestra un formulario en el contenedor principal
     *
     * @param component Formulario a mostrar
     */
    public static void showForm(Component component) {
        component.applyComponentOrientation(app.getComponentOrientation());
        app.mainForm.showForm(component);
    }

    public MainForm getMainForm() {
        return mainForm;
    }

    /**
     * Realiza el login del usuario y muestra el formulario principal
     */
    public static void login() {
        // Verificar sesión
        if (!UserSession.getInstance().isLoggedIn()) {
            Notifications.getInstance().show(Notifications.Type.ERROR,
                    "No hay un usuario en sesión");
            return;
        }

        // Inicializar mainForm si es null
        if (app.mainForm == null) {
            app.mainForm = new MainForm();
        }

        // Transición animada
        FlatAnimatedLafChange.showSnapshot();
        app.setContentPane(app.mainForm);
        app.mainForm.applyComponentOrientation(app.getComponentOrientation());

        // Configurar menú
        app.mainForm.getMenu().configureMenuByUserRole();
        // Seleccionar Dashboard por defecto (índice 1 tras reordenar menú)
        setSelectedMenu(1, 0);
        app.mainForm.hideMenu();

        // Mensaje de bienvenida
        ModelUser user = UserSession.getInstance().getCurrentUser();
        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                "Bienvenido " + user.getNombre() + " (" + user.getRol() + ")");

        // Dashboard
        try {
            showForm(new FormDashboard());
        } catch (Exception ex) {
            Notifications.getInstance().show(Notifications.Type.ERROR,
                    "Error al cargar dashboard: " + ex.getMessage());
        }

        SwingUtilities.updateComponentTreeUI(app.mainForm);
        FlatAnimatedLafChange.hideSnapshotWithAnimation();

        // Iniciar servicio de notificaciones pendientes (Traspasos, Pedidos Web, Cajas)
        new Thread(() -> {
            try {
                // Pequeña espera para asegurar que la UI esté lista
                Thread.sleep(1500);

                Integer idBodega = user.getIdBodega();
                int finalBodegaId = (idBodega != null) ? idBodega : 0;

                // Limpiar el registro de notificaciones mostradas al iniciar sesión
                app.mainForm.limpiarRegistroNotificacionesMostradas();

                NotificacionesService.getInstance().setParentFrame(app);
                NotificacionesService.getInstance().mostrarAlertasInicioSesion(
                        user.getIdUsuario(),
                        finalBodegaId);

                // Iniciar monitoreo continuo de traspasos en segundo plano
                app.mainForm.iniciarNotificacionesTraspasosEnLinea();
            } catch (Exception e) {
                System.err.println("Error al cargar notificaciones de inicio: " + e.getMessage());
            }
        }, "Login-Notifications-Thread").start();
    }

    /**
     * Cierra la sesión y vuelve al login
     */
    public static void logout() {
        if (app.mainForm != null) {
            app.mainForm.detenerNotificacionesTraspasosEnLinea();
            // Limpiar el registro de notificaciones mostradas al cerrar sesión
            app.mainForm.limpiarRegistroNotificacionesMostradas();
            // Limpiar el formulario de venta persistente
            app.mainForm.limparFormularioVenta();
        }
        // Cerrar sesión
        UserSession.getInstance().logout();

        // Limpiar el contenido del mainForm para que no aparezca al volver a iniciar
        // sesión
        app.mainForm.showForm(new JPanel());

        // Transición al login
        FlatAnimatedLafChange.showSnapshot();
        app.setContentPane(app.loginForm);
        app.loginForm.resetLoginState();
        app.loginForm.applyComponentOrientation(app.getComponentOrientation());
        SwingUtilities.updateComponentTreeUI(app.loginForm);
        FlatAnimatedLafChange.hideSnapshotWithAnimation();

        Notifications.getInstance().show(Notifications.Type.INFO,
                "Sesión cerrada correctamente");
    }

    /**
     * Verifica si un usuario tiene permiso para acceder a un módulo
     *
     * @param moduleName Nombre del módulo
     * @return true si tiene permiso, false en caso contrario
     */
    public static boolean hasPermission(String moduleName) {
        return UserSession.getInstance().hasPermission(moduleName);
    }

    /**
     * Verifica si un usuario tiene un rol específico
     *
     * @param role Rol a verificar
     * @return true si tiene el rol, false en caso contrario
     */
    public static boolean hasRole(String role) {
        return UserSession.getInstance().hasRole(role);
    }

    /**
     * Establece el elemento de menú seleccionado
     *
     * @param index    Índice del menú principal
     * @param subIndex Índice del submenú
     */
    public static void setSelectedMenu(int index, int subIndex) {
        app.mainForm.setSelectedMenu(index, subIndex);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 719, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 521, Short.MAX_VALUE));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {

        FlatLaf.registerCustomDefaultsSource("raven.theme");
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("app/ui");
            String saved = prefs.get("theme", "");
            if ("dark".equals(saved)) {
                FlatMacDarkLaf.setup();
            } else if ("light".equals(saved)) {
                com.formdev.flatlaf.themes.FlatMacLightLaf.setup();
            } else {
                FlatMacDarkLaf.setup();
            }
        } catch (Exception ignore) {
            FlatMacDarkLaf.setup();
        }

        java.awt.EventQueue.invokeLater(() -> {
            Application app = new Application();
            app.setVisible(true);
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }
                raven.controlador.update.UpdateManager.checkForUpdates();
            }, "Updater-Thread") {
                {
                    setDaemon(true);
                }
            }.start();
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
