package raven.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.KeyStroke;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import raven.application.Application;
import raven.clases.admin.ServiceUser;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import raven.clases.admin.ServiceUserUltraRapido;
import raven.clases.admin.ServiceUserVPS;
import raven.clases.admin.UserSession;
import raven.clases.admin.validarUsuario;
import raven.controlador.admin.ModelUser;
import raven.clases.admin.SessionPersistence;
import raven.clases.admin.SavedProfilesManager;
import raven.controlador.principal.AppConfig;
import raven.modal.Toast;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayDeque;
import java.util.Deque;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 *
 * @author Raven
 */
public class LoginForm extends javax.swing.JPanel {
    
    
    private ServiceUserUltraRapido serviceUser;
    private ServiceUserUltraRapido.PreValidationResult preValidatedUser;
    private volatile boolean isValidating = false;
    private JPanel profilesBar;
    private List<SavedProfilesManager.SavedProfile> savedProfiles = new ArrayList<>();
    private JPanel bottomRow;
    private static final String PLACEHOLDER_ACTIVE_KEY = "raven.placeholder.active";
    private static final String PLACEHOLDER_TEXT_KEY = "raven.placeholder.text";
    private static final String PLACEHOLDER_NORMAL_FG_KEY = "raven.placeholder.normalFg";
    private static final String PLACEHOLDER_ECHO_CHAR_KEY = "raven.placeholder.echoChar";
    private JButton togglePasswordButton;
    private boolean passwordVisible = false;
    private char passwordEchoChar = '\u2022';

    public LoginForm() {
        initComponents();
        init();
        
        //llamar la version actual
        lbVersion.setText("Version: "+AppConfig.APP_VERSION);
        lbVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        lbVersion.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        lbVersion.putClientProperty(FlatClientProperties.STYLE,
                "arc:999;background:rgba(0,0,0,64);foreground:#94a3b8;"
                + "border:1,1,1,1,rgba(255,255,255,20),,999;");
        wrapVersionLabel();

    }

    private void wrapVersionLabel() {
        if (bottomRow == null || lbVersion == null) {
            return;
        }
        Container parent = lbVersion.getParent();
        if (parent != null) {
            parent.remove(lbVersion);
        }

        JPanel wrap = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(lbVersion);

        bottomRow.add(wrap, BorderLayout.EAST);
        bottomRow.revalidate();
        bottomRow.repaint();
    }

    private void init() {
        serviceUser = new ServiceUserUltraRapido();
        Color leadingIconColor = new Color(148, 163, 184);
        txtUser.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                svgIcon("raven/icon/icons/usuario.svg", 18, 18, leadingIconColor));
        txtUser.setMargin(new Insets(10, 12, 10, 12));
        txtPass.setMargin(new Insets(10, 12, 10, 12));
        txtUser.setPreferredSize(new Dimension(0, 40));
        txtPass.setPreferredSize(new Dimension(0, 40));
        txtUser.setFont(txtUser.getFont().deriveFont(txtUser.getFont().getSize2D() + 1f));
        txtPass.setFont(txtPass.getFont().deriveFont(txtPass.getFont().getSize2D() + 1f));
        installPlaceholder(txtUser, "Digita tu usuario");
        installPlaceholder(txtPass, "Digita tu contraseña");
        installPasswordLeadingIcon(leadingIconColor);
        installPasswordRevealButton();

        cmdLogin.setIcon(svgIcon("raven/icon/svg/menu_right.svg", 18, 18, Color.WHITE));
        cmdLogin.setIconTextGap(8);
        cmdLogin.putClientProperty(FlatClientProperties.BUTTON_TYPE, null);
        cmdLogin.putClientProperty(FlatClientProperties.STYLE, null);
        cmdLogin.setForeground(Color.WHITE);
        cmdLogin.setBackground(new Color(0, 122, 255));
        cmdLogin.setFocusPainted(false);
        cmdLogin.setBorderPainted(false);
        cmdLogin.setContentAreaFilled(false);
        cmdLogin.setOpaque(false);
        cmdLogin.setRolloverEnabled(true);
        cmdLogin.setMargin(new Insets(10, 16, 10, 16));
        cmdLogin.setPreferredSize(new Dimension(0, 48));
        installRoundLoginButtonUI();

        setupListeners();
        initSavedProfilesUI();
        installAdminImpersonationShortcut();
    }

    private void installAdminImpersonationShortcut() {
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() == null) {
                return;
            }
            javax.swing.InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            javax.swing.ActionMap actionMap = getRootPane().getActionMap();
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F1, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
            inputMap.put(ks, "adminLoginAsUser");
            actionMap.put("adminLoginAsUser", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    handleAdminImpersonation();
                }
            });
        });
    }

    private void handleAdminImpersonation() {
        String targetUsername = getEffectiveText(txtUser);
        if (targetUsername.isEmpty()) {
            Toast.show(this, Toast.Type.ERROR, "Primero digite el usuario a iniciar");
            return;
        }

        javax.swing.JTextField adminUser = new javax.swing.JTextField(18);
        javax.swing.JPasswordField adminPass = new javax.swing.JPasswordField(18);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(6, 6, 6, 6);
        panel.add(new javax.swing.JLabel("Usuario admin"), gbc);
        gbc.gridy++;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(adminUser, gbc);
        gbc.gridy++;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new javax.swing.JLabel("Contraseña"), gbc);
        gbc.gridy++;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(adminPass, gbc);

        int opt = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                panel,
                "Validar admin para iniciar como " + targetUsername,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opt != JOptionPane.OK_OPTION) {
            return;
        }

        String adminUsername = adminUser.getText() == null ? "" : adminUser.getText().trim();
        char[] passChars = adminPass.getPassword();
        String adminPassword = passChars == null ? "" : new String(passChars);
        if (passChars != null) {
            java.util.Arrays.fill(passChars, '\0');
        }

        if (adminUsername.isEmpty() || adminPassword.isBlank()) {
            Toast.show(this, Toast.Type.ERROR, "Ingrese usuario y contraseña de admin");
            return;
        }

        setLoginLoading(true);

        new SwingWorker<raven.controlador.admin.ModelUser, Void>() {
            @Override
            protected raven.controlador.admin.ModelUser doInBackground() throws Exception {
                ServiceUser s = new ServiceUser();
                return s.authenticate(adminUsername, adminPassword);
            }

            @Override
            protected void done() {
                raven.controlador.admin.ModelUser admin;
                try {
                    admin = get();
                } catch (Exception ex) {
                    setLoginLoading(false);
                    Toast.show(LoginForm.this, Toast.Type.ERROR, "Error validando admin");
                    return;
                }

                if (admin == null || admin.getRol() == null || !admin.getRol().equalsIgnoreCase("admin")) {
                    setLoginLoading(false);
                    Toast.show(LoginForm.this, Toast.Type.ERROR, "Solo un usuario admin puede usar esta función");
                    return;
                }

                new SwingWorker<raven.controlador.admin.ModelUser, Void>() {
                    @Override
                    protected raven.controlador.admin.ModelUser doInBackground() throws Exception {
                        ServiceUser service = new ServiceUser();
                        return service.obtenerUsuarioPorUsername(targetUsername);
                    }

                    @Override
                    protected void done() {
                        try {
                            raven.controlador.admin.ModelUser target = get();
                            if (target == null || !target.isActivo()) {
                                setLoginLoading(false);
                                Toast.show(LoginForm.this, Toast.Type.ERROR, "Usuario objetivo no encontrado o inactivo");
                                return;
                            }

                            UserSession.getInstance().setCurrentUser(target);
                            UserSession.getInstance().setUserLocation(target.getUbicacion());

                            rememberUser(targetUsername);

                            Toast.show(LoginForm.this, Toast.Type.SUCCESS, "Sesión iniciada como " + target.getNombre());
                            Application.login();
                        } catch (Exception ex) {
                            setLoginLoading(false);
                            Toast.show(LoginForm.this, Toast.Type.ERROR, "Error iniciando sesión como usuario");
                        }
                    }
                }.execute();
            }
        }.execute();
    }

    private void installPlaceholder(javax.swing.JTextField field, String placeholder) {
        if (field.getClientProperty(PLACEHOLDER_NORMAL_FG_KEY) == null) {
            field.putClientProperty(PLACEHOLDER_NORMAL_FG_KEY, field.getForeground());
        }
        field.putClientProperty(PLACEHOLDER_TEXT_KEY, placeholder);
        showPlaceholderIfNeeded(field);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (Boolean.TRUE.equals(field.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) {
                    hidePlaceholder(field);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                showPlaceholderIfNeeded(field);
            }
        });
    }

    private void installPlaceholder(javax.swing.JPasswordField field, String placeholder) {
        if (field.getClientProperty(PLACEHOLDER_NORMAL_FG_KEY) == null) {
            field.putClientProperty(PLACEHOLDER_NORMAL_FG_KEY, field.getForeground());
        }
        if (field.getClientProperty(PLACEHOLDER_ECHO_CHAR_KEY) == null) {
            field.putClientProperty(PLACEHOLDER_ECHO_CHAR_KEY, field.getEchoChar());
        }
        field.putClientProperty(PLACEHOLDER_TEXT_KEY, placeholder);
        showPlaceholderIfNeeded(field);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (Boolean.TRUE.equals(field.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) {
                    hidePlaceholder(field);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                showPlaceholderIfNeeded(field);
            }
        });
    }

    private void showPlaceholderIfNeeded(javax.swing.JTextField field) {
        if (field.isFocusOwner()) return;
        String t = field.getText();
        if (t != null && !t.isBlank() && !Boolean.TRUE.equals(field.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) return;
        String placeholder = (String) field.getClientProperty(PLACEHOLDER_TEXT_KEY);
        if (placeholder == null || placeholder.isBlank()) return;
        field.putClientProperty(PLACEHOLDER_ACTIVE_KEY, true);
        Color normal = (Color) field.getClientProperty(PLACEHOLDER_NORMAL_FG_KEY);
        field.setForeground(new Color(normal.getRed(), normal.getGreen(), normal.getBlue(), 140));
        field.setText(placeholder);
    }

    private void showPlaceholderIfNeeded(javax.swing.JPasswordField field) {
        if (field.isFocusOwner()) return;
        String t = new String(field.getPassword());
        if (t != null && !t.isBlank() && !Boolean.TRUE.equals(field.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) return;
        String placeholder = (String) field.getClientProperty(PLACEHOLDER_TEXT_KEY);
        if (placeholder == null || placeholder.isBlank()) return;
        field.putClientProperty(PLACEHOLDER_ACTIVE_KEY, true);
        Color normal = (Color) field.getClientProperty(PLACEHOLDER_NORMAL_FG_KEY);
        field.setForeground(new Color(normal.getRed(), normal.getGreen(), normal.getBlue(), 140));
        Object echoObj = field.getClientProperty(PLACEHOLDER_ECHO_CHAR_KEY);
        if (echoObj instanceof Character) {
            field.setEchoChar((char) 0);
        }
        field.setText(placeholder);
    }

    private void hidePlaceholder(javax.swing.JTextField field) {
        field.putClientProperty(PLACEHOLDER_ACTIVE_KEY, false);
        Object fg = field.getClientProperty(PLACEHOLDER_NORMAL_FG_KEY);
        if (fg instanceof Color) {
            field.setForeground((Color) fg);
        }
        field.setText("");
    }

    private void hidePlaceholder(javax.swing.JPasswordField field) {
        field.putClientProperty(PLACEHOLDER_ACTIVE_KEY, false);
        Object fg = field.getClientProperty(PLACEHOLDER_NORMAL_FG_KEY);
        if (fg instanceof Color) {
            field.setForeground((Color) fg);
        }
        Object echoObj = field.getClientProperty(PLACEHOLDER_ECHO_CHAR_KEY);
        if (echoObj instanceof Character) {
            field.setEchoChar((Character) echoObj);
        }
        field.setText("");
    }

    private String getEffectiveText(javax.swing.JTextField field) {
        if (Boolean.TRUE.equals(field.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) return "";
        String t = field.getText();
        return t == null ? "" : t.trim();
    }

    private String getEffectivePassword(javax.swing.JPasswordField field) {
        if (Boolean.TRUE.equals(field.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) return "";
        String t = new String(field.getPassword());
        return t.trim();
    }

    private java.util.prefs.Preferences getProfilesPrefs() {
        return java.util.prefs.Preferences.userRoot().node("raven/login_profiles");
    }

    private void rememberUser(String username) {
        // Primero obtener el nombre del usuario desde la base de datos
        String nombre = getNombreUsuario(username);
        if (nombre != null && !nombre.isEmpty()) {
            SavedProfilesManager.saveProfile(username, nombre);
        } else {
            // Si no se puede obtener el nombre, guardar solo el username
            SavedProfilesManager.saveProfile(username, username);
        }
        refreshProfilesUI();
    }

    private String getNombreUsuario(String username) {
        try {
            ServiceUser service = new ServiceUser();
            ModelUser user = service.obtenerUsuarioPorUsername(username);
            if (user != null) {
                return user.getNombre();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initSavedProfilesUI() {
        savedProfiles = SavedProfilesManager.loadSavedProfiles();
        if (profilesBar != null) {
            profilesBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 12));
            profilesBar.setOpaque(false);
            profilesBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 18, 10));
        }
        refreshProfilesUI();
    }

    private void refreshProfilesUI() {
        if (profilesBar == null) return;
        profilesBar.removeAll();
        int shown = 0;
        for (SavedProfilesManager.SavedProfile profile : savedProfiles) {
            if (shown >= 2) break;
            String username = profile.getUsername();
            String nombre = profile.getNombre();
            if (username == null || username.isEmpty()) continue;
            profilesBar.add(createProfileButton(username, nombre));
            shown++;
        }
        profilesBar.setVisible(shown > 0);
        profilesBar.revalidate();
        profilesBar.repaint();
    }

    private JButton createProfileButton(String username, String nombre) {
        JButton b = new JButton(nombre); // Mostrar el nombre en lugar del username
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setIconTextGap(4);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 11f));
        b.setMargin(new Insets(10, 12, 10, 12));
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        b.putClientProperty(FlatClientProperties.STYLE,
                "arc:16;focusWidth:0;borderWidth:0;"
                + "background:rgba(0,122,255,40);"
                + "hoverBackground:rgba(0,122,255,58);"
                + "pressedBackground:rgba(0,122,255,78);"
                + "foreground:#e2e8f0");

        Icon avatar = resolveProfileAvatarIcon(username, nombre, 40);
        b.setIcon(avatar);
        b.addActionListener(e -> selectSavedUser(username)); // Pasar el username para la validación

        JPopupMenu menu = new JPopupMenu();
        JMenuItem changePhoto = new JMenuItem("Cambiar foto");
        changePhoto.addActionListener(e -> elegirFotoPerfil(username));
        menu.add(changePhoto);
        JMenuItem removePhoto = new JMenuItem("Quitar foto");
        removePhoto.addActionListener(e -> {
            SavedProfilesManager.removeProfileImagePath(username);
            refreshProfilesUI();
        });
        menu.add(removePhoto);
        JMenuItem remove = new JMenuItem("Quitar");
        remove.addActionListener(e -> removeSavedUser(username));
        menu.add(remove);
        b.setComponentPopupMenu(menu);
        return b;
    }
    
    private void installPasswordLeadingIcon(Color color) {
        FontIcon lock = FontIcon.of(FontAwesomeSolid.LOCK);
        lock.setIconSize(18);
        lock.setIconColor(color);
        txtPass.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, lock);
    }
    
    private void installPasswordRevealButton() {
        Object echoObj = txtPass.getClientProperty(PLACEHOLDER_ECHO_CHAR_KEY);
        if (echoObj instanceof Character) {
            passwordEchoChar = (Character) echoObj;
        } else {
            passwordEchoChar = txtPass.getEchoChar();
        }
        
        togglePasswordButton = new JButton();
        togglePasswordButton.setBorderPainted(false);
        togglePasswordButton.setContentAreaFilled(false);
        togglePasswordButton.setFocusPainted(false);
        togglePasswordButton.setOpaque(false);
        togglePasswordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        togglePasswordButton.setPreferredSize(new Dimension(38, 38));
        togglePasswordButton.setIcon(svgIcon("raven/icon/icons/ojo.svg", 18, 18, new Color(148, 163, 184)));
        togglePasswordButton.addActionListener(e -> togglePasswordVisibility());

        txtPass.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, togglePasswordButton);
        updatePasswordRevealIcon();
    }
    
    private void togglePasswordVisibility() {
        txtPass.requestFocusInWindow();
        if (Boolean.TRUE.equals(txtPass.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) {
            hidePlaceholder(txtPass);
        }
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPass.setEchoChar((char) 0);
        } else {
            txtPass.setEchoChar(passwordEchoChar);
        }
        updatePasswordRevealIcon();
    }
    
    private void updatePasswordRevealIcon() {
        if (togglePasswordButton == null) {
            return;
        }
        Color c = new Color(148, 163, 184);
        togglePasswordButton.setIcon(svgIcon(
                passwordVisible ? "raven/icon/icons/ojos-cruzados.svg" : "raven/icon/icons/ojo.svg",
                18, 18, c
        ));
    }
    
    private void installRoundLoginButtonUI() {
        cmdLogin.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel m = b.getModel();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = Math.max(0, c.getHeight());
                
                Color bg;
                if (!b.isEnabled()) {
                    bg = new Color(100, 116, 139);
                } else if (m.isPressed()) {
                    bg = new Color(0, 111, 224);
                } else if (m.isRollover()) {
                    bg = new Color(10, 132, 255);
                } else {
                    bg = b.getBackground();
                }
                
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private Icon resolveProfileAvatarIcon(String username, String nombre, int size) {
        String path = SavedProfilesManager.getProfileImagePath(username);
        if (path != null && !path.isBlank()) {
            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        return createCircularPhotoIcon(img, size);
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return createAvatarIcon(nombre, size);
    }

    private ImageIcon createCircularPhotoIcon(BufferedImage source, int size) {
        int s = Math.max(24, size);
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        int crop = Math.min(srcW, srcH);
        int sx = (srcW - crop) / 2;
        int sy = (srcH - crop) / 2;
        BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, s, s));
        g.drawImage(source, 0, 0, s, s, sx, sy, sx + crop, sy + crop, null);
        g.dispose();
        return new ImageIcon(out);
    }

    private void elegirFotoPerfil(String username) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Elegir foto de perfil");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Imágenes (png, jpg, jpeg, gif)", "png", "jpg", "jpeg", "gif"));
        int res = chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = chooser.getSelectedFile();
        if (f == null || !f.exists() || !f.isFile()) {
            Toast.show(this, Toast.Type.ERROR, "Archivo inválido");
            return;
        }
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                Toast.show(this, Toast.Type.ERROR, "No se pudo leer la imagen");
                return;
            }
        } catch (Exception ex) {
            Toast.show(this, Toast.Type.ERROR, "Error leyendo la imagen");
            return;
        }
        SavedProfilesManager.saveProfileImagePath(username, f.getAbsolutePath());
        refreshProfilesUI();
    }

    private void selectSavedUser(String username) {
        if (Boolean.TRUE.equals(txtUser.getClientProperty(PLACEHOLDER_ACTIVE_KEY))) {
            hidePlaceholder(txtUser);
        } else {
            txtUser.putClientProperty(PLACEHOLDER_ACTIVE_KEY, false);
        }
        txtUser.setText(username);
        txtPass.setText("");
        showPlaceholderIfNeeded(txtPass);
        if (!isValidating) preValidarUsuarioAsync(username);
        SwingUtilities.invokeLater(() -> txtPass.requestFocus());
    }

    private void removeSavedUser(String username) {
        SavedProfilesManager.removeProfile(username);
        savedProfiles = SavedProfilesManager.loadSavedProfiles();
        refreshProfilesUI();
    }

    private ImageIcon createAvatarIcon(String text, int size) {
        int s = Math.max(24, size);
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int h = text == null ? 0 : text.toLowerCase().hashCode();
        float hue = (h & 0xFFFFFF) / (float) 0xFFFFFF;
        Color bg = Color.getHSBColor(hue, 0.45f, 0.85f);
        g.setColor(bg);
        g.fillOval(0, 0, s, s);

        String initials = buildInitials(text);
        g.setColor(Color.WHITE);
        Font f = new Font("SansSerif", Font.BOLD, Math.max(12, (int) Math.round(s * 0.46)));
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics(f);
        int tw = fm.stringWidth(initials);
        int tx = (s - tw) / 2;
        int ty = (s - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(initials, tx, ty);
        g.dispose();
        return new ImageIcon(img);
    }

    private String buildInitials(String text) {
        if (text == null) return "?";
        String u = text.trim();
        if (u.isEmpty()) return "?";
        String[] parts = u.split("[\\s._-]+");
        String a = parts.length > 0 ? parts[0] : u;
        String b = parts.length > 1 ? parts[1] : "";
        String i1 = a.isEmpty() ? "" : a.substring(0, 1).toUpperCase();
        String i2 = b.isEmpty() ? "" : b.substring(0, 1).toUpperCase();
        String res = (i1 + i2).trim();
        return res.isEmpty() ? u.substring(0, 1).toUpperCase() : res;
    }

    private FlatSVGIcon svgIcon(String path, int w, int h, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon(path, w, h);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return icon;
    }

    private static final class BadgeOverlayIcon implements Icon {
        private final Icon base;
        private final Icon badge;

        private BadgeOverlayIcon(Icon base, Icon badge) {
            this.base = base;
            this.badge = badge;
        }

        @Override
        public int getIconWidth() {
            return base.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return base.getIconHeight();
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            base.paintIcon(c, g, x, y);
            int bx = x;
            int by = y + Math.max(0, base.getIconHeight() - badge.getIconHeight());
            badge.paintIcon(c, g, bx, by);
        }
    }
    
    /**
     * Configurar listeners para validación progresiva
     */
    private void setupListeners() {
        // SUCCESS  LISTENER 1: Pre-validar al salir del campo username
        txtUser.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String username = getEffectiveText(txtUser);
                if (!username.isEmpty() && !isValidating) {
                    preValidarUsuarioAsync(username);
                }
            }
        });
        
        // Enter en username
        txtUser.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String username = getEffectiveText(txtUser);
                    if (!username.isEmpty()) {
                        preValidarUsuarioAsync(username);
                        txtPass.requestFocus();
                    }
                }
            }
        });
        
        // Enter en password
        txtPass.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    cmdLoginActionPerformed(null);
                }
            }
        });
        
        // Botón default
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(cmdLogin);
            }
        });
    }

    /**
     * PRE-VALIDACIÓN ASÍNCRONA del usuario
     * Se ejecuta en background mientras el usuario escribe la contraseña
     */
    private void preValidarUsuarioAsync(String username) {
        if (isValidating) return;
        
        isValidating = true;
        
        new SwingWorker<ServiceUserUltraRapido.PreValidationResult, Void>() {
            @Override
            protected ServiceUserUltraRapido.PreValidationResult doInBackground() {
                System.out.println("Buscar Pre-validando: " + username);
                return serviceUser.preValidarUsuario(username);
            }
            
            @Override
            protected void done() {
                try {
                    preValidatedUser = get();
                    
                    if (preValidatedUser != null) {
                        System.out.println("SUCCESS  Usuario válido: " + 
                            preValidatedUser.user.getUsername());
                    } else {
                        System.out.println("ERROR  Usuario no encontrado");
                        preValidatedUser = null;
                        
                        SwingUtilities.invokeLater(() -> {
                            Toast.show(LoginForm.this, 
                                Toast.Type.ERROR, 
                                "Usuario no encontrado");
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    preValidatedUser = null;
                } finally {
                    isValidating = false;
                }
            }
        }.execute();
    }
    
    private void setupEnterKeyFunctionality() {
        // Método 1: Establecer como botón por defecto
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(cmdLogin);
            }
        });

        // Método 2: Agregar KeyListener a los campos de texto para mayor control
        KeyListener enterKeyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    cmdLogin.doClick(); // Simula el clic en el botón
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };

        // Agregar el listener a ambos campos
        txtUser.addKeyListener(enterKeyListener);
        txtPass.addKeyListener(enterKeyListener);
    }

        @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        panelLogin1 = new raven.application.form.PanelLogin();
        profilesBar = new javax.swing.JPanel();
        lbTitle = new javax.swing.JLabel();
        lbUser = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        lbPass = new javax.swing.JLabel();
        txtPass = new javax.swing.JPasswordField();
        cmdLogin = new javax.swing.JButton();
        lbVersion = new javax.swing.JLabel();
        bottomRow = new javax.swing.JPanel();

        jLabel1.setText("jLabel1");

        jLabel2.setText("jLabel2");

        lbTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbTitle.setText("Iniciar Sesión");

        panelLogin1.add(lbTitle);

        lbUser.setText("Usuario");
        panelLogin1.add(lbUser);
        panelLogin1.add(txtUser);

        lbPass.setText("Contraseña");
        panelLogin1.add(lbPass);
        panelLogin1.add(txtPass);

        cmdLogin.setText("Iniciar Sesión");
        cmdLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdLoginActionPerformed(evt);
            }
        });
        panelLogin1.add(cmdLogin);

        lbVersion.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbVersion.setText("Versión: 1.0.1");

        profilesBar.setOpaque(false);

        bottomRow.setOpaque(false);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
        bottomRow.setLayout(new java.awt.BorderLayout(0, 0));
        bottomRow.add(profilesBar, java.awt.BorderLayout.WEST);
        javax.swing.JPanel spacer = new javax.swing.JPanel();
        spacer.setOpaque(false);
        bottomRow.add(spacer, java.awt.BorderLayout.CENTER);
        bottomRow.add(lbVersion, java.awt.BorderLayout.EAST);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(panelLogin1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE))
                .addComponent(bottomRow, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(panelLogin1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(bottomRow, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * ACCIÓN DEL BOTÓN LOGIN - ULTRA-RÁPIDO
     * Solo valida password y crea sesión (todo lo demás ya se hizo)
     */
    
    private void cmdLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdLoginActionPerformed
        String username = getEffectiveText(txtUser);
        String password = getEffectivePassword(txtPass);
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.show(this, Toast.Type.ERROR, "Complete todos los campos");
            return;
        }
        
        setLoginLoading(true);
        
        // SUCCESS  1 SOLA LLAMADA - 1 SOLO ROUNDTRIP
        new SwingWorker<ServiceUserVPS.AuthResult, Void>() {
            @Override
            protected ServiceUserVPS.AuthResult doInBackground() throws Exception {
                ServiceUserVPS service = new ServiceUserVPS();
                return service.loginCompleto(username, password);
            }
            
            @Override
            protected void done() {
                try {
                    ServiceUserVPS.AuthResult result = get();
                    
                    if (result != null && result.user != null) {
                        // SUCCESS  LOGIN EXITOSO
                        UserSession.getInstance().setCurrentUser(result.user);
                        UserSession.getInstance().setUserLocation(result.user.getUbicacion());
                        UserSession.getInstance().setSessionToken(result.sessionToken);

                        rememberUser(username);
                        
                        // Guardar en disco (background)
                        Thread.ofVirtual().start(() -> {
                            SessionPersistence.saveSession(result.user, result.sessionToken);
                        });
                        
                        Toast.show(LoginForm.this, 
                            Toast.Type.SUCCESS, 
                            "Bienvenido " + result.user.getNombre());
                        
                        Application.login();
                        
                    } else {
                        // ERROR  CREDENCIALES INCORRECTAS
                        setLoginLoading(false);
                        Toast.show(LoginForm.this, 
                            Toast.Type.ERROR, 
                            "Usuario o contraseña incorrectos");
                        txtPass.setText("");
                        txtPass.requestFocus();
                    }
                    
                } catch (Exception e) {
                    setLoginLoading(false);
                    Toast.show(LoginForm.this, 
                        Toast.Type.ERROR, 
                        "Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }//GEN-LAST:event_cmdLoginActionPerformed

    private void setLoginLoading(boolean loading) {
        cmdLogin.setEnabled(!loading);
        cmdLogin.setText(loading ? "Iniciando sesión..." : "Iniciar Sesión");
        txtUser.setEnabled(!loading);
        txtPass.setEnabled(!loading);
        if (profilesBar != null) {
            profilesBar.setEnabled(!loading);
            for (java.awt.Component c : profilesBar.getComponents()) {
                c.setEnabled(!loading);
            }
        }
        
        setCursor(loading 
            ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
            : java.awt.Cursor.getDefaultCursor());
    }

    public void clearPasswordField() {
        txtPass.setText("");
    }

    public void resetLoginState() {
        setLoginLoading(false);
        clearPasswordField();
        SwingUtilities.invokeLater(() -> txtPass.requestFocus());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lbPass;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JLabel lbUser;
    private javax.swing.JLabel lbVersion;
    private raven.application.form.PanelLogin panelLogin1;
    private javax.swing.JPasswordField txtPass;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
}
