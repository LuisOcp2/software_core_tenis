package raven.componentes.cajas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;
import raven.clases.admin.UserSession;
import com.formdev.flatlaf.FlatClientProperties;


public class MonitorInactividadCaja {
    
    // ==================== CONSTANTES ====================
    private static final int TIEMPO_INACTIVIDAD_MS = 900_000; // 900 segundos
    private static final int TIEMPO_ADVERTENCIA_MS = 300_000; // 300 segundos
    private static final int MAX_INTENTOS_FALLIDOS = 3;
    
    // Constantes de blur y diseño
    private static final int BLUR_RADIUS = 10;
    private static final float OVERLAY_OPACITY = 0.65f;
    private static final Color OVERLAY_COLOR = new Color(15, 23, 42, 160);
    
    // ==================== COLORES PROFESIONALES ====================
    private static final Color COLOR_FONDO = new Color(30, 41, 59);      // Slate-800
    private static final Color COLOR_ACCENT = new Color(14, 165, 233);   // Sky-500
    private static final Color COLOR_ACCENT_HOVER = new Color(2, 132, 199); // Sky-600
    private static final Color COLOR_TEXTO_PRINCIPAL = new Color(248, 250, 252); // Slate-50
    private static final Color COLOR_TEXTO_SECUNDARIO = new Color(203, 213, 225); // Slate-300
    private static final Color COLOR_ERROR = new Color(239, 68, 68);     // Red-500
    private static final Color COLOR_EXITO = new Color(34, 197, 94);     // Green-500
    private static final Color COLOR_BORDE = new Color(71, 85, 105);     // Slate-600
    
    // ==================== INSTANCIA SINGLETON ====================
    private static MonitorInactividadCaja instance;
    
    // ==================== ATRIBUTOS ====================
    private Timer timerInactividad;
    private Timer timerAdvertencia;
    private JFrame ventanaPrincipal;
    private JDialog dialogoBloqueo;
    private boolean bloqueado = false;
    private AWTEventListener listenerEventos;
    private BloqueoCallback callback;
    
    // Blur y animaciones
    private BlurGlassPane blurGlassPane;
    
    // Contadores
    private long contadorEventos = 0;
    private long ultimoReinicio = System.currentTimeMillis();
    private int intentosFallidos = 0;
    
    // ==================== INTERFAZ DE CALLBACK ====================
    
    public interface BloqueoCallback {
        void onBloqueado();
        void onDesbloqueado();
        void onAdvertenciaBloqueo(int segundosRestantes);
    }
    
    // ==================== CONSTRUCTOR PRIVADO ====================
    
    private MonitorInactividadCaja() {
        // Constructor vacío
    }
    
    public static synchronized MonitorInactividadCaja getInstance() {
        if (instance == null) {
            instance = new MonitorInactividadCaja();
        }
        return instance;
    }
    
    // ==================== INICIALIZACIÓN ====================
    
    public void iniciar(JFrame ventanaPrincipal, BloqueoCallback callback) {
        // Limpiar estado anterior
        if (listenerEventos != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(listenerEventos);
        }
        
        this.ventanaPrincipal = ventanaPrincipal;
        this.callback = callback;
        this.bloqueado = false;
        this.contadorEventos = 0;
        this.intentosFallidos = 0;
        this.ultimoReinicio = System.currentTimeMillis();
        
        // Listener de eventos
        listenerEventos = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                contadorEventos++;
                
                if (!bloqueado) {
                    if (contadorEventos % 100 == 0) {
                        long ahora = System.currentTimeMillis();
                        long segundosDesdeUltimoReinicio = (ahora - ultimoReinicio) / 1000;
                        
                        System.out.println(String.format(
                            "Actualizando Actividad detectada - Eventos: %d | Último reinicio: hace %d segundos",
                            contadorEventos, segundosDesdeUltimoReinicio
                        ));
                    }
                    
                    reiniciarTemporizador();
                }
            }
        };
        
        Toolkit.getDefaultToolkit().addAWTEventListener(
            listenerEventos,
            AWTEvent.MOUSE_EVENT_MASK 
            | AWTEvent.MOUSE_MOTION_EVENT_MASK
            | AWTEvent.KEY_EVENT_MASK
            | AWTEvent.MOUSE_WHEEL_EVENT_MASK
            | AWTEvent.FOCUS_EVENT_MASK
        );
        
        iniciarTemporizador();
        
        System.out.println("SUCCESS  Monitor de inactividad iniciado con blur mejorado");
    }
    
    public void detener() {
        System.out.println(" Deteniendo monitor de inactividad...");
        
        if (listenerEventos != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(listenerEventos);
            listenerEventos = null;
        }
        
        cancelarTemporizadores();
        
        if (blurGlassPane != null) {
            blurGlassPane.deactivate();
            blurGlassPane = null;
        }
        
        if (dialogoBloqueo != null && dialogoBloqueo.isVisible()) {
            dialogoBloqueo.dispose();
            dialogoBloqueo = null;
        }
        
        System.out.println("SUCCESS  Monitor detenido correctamente");
    }
    
    // ==================== TEMPORIZADORES ====================
    
    private void iniciarTemporizador() {
        cancelarTemporizadores();
        ultimoReinicio = System.currentTimeMillis();
        
        // Advertencia
        timerAdvertencia = new Timer("Timer-Advertencia-Caja", true);
        timerAdvertencia.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (!bloqueado && callback != null) {
                        callback.onAdvertenciaBloqueo(5);
                    }
                });
            }
        }, TIEMPO_ADVERTENCIA_MS);
        
        // Bloqueo
        timerInactividad = new Timer("Timer-Bloqueo-Caja", true);
        timerInactividad.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (!bloqueado) {
                        bloquearInterfaz();
                    }
                });
            }
        }, TIEMPO_INACTIVIDAD_MS);
    }
    
    private void reiniciarTemporizador() {
        if (bloqueado) return;
        
        long ahora = System.currentTimeMillis();
        long tiempoDesdeUltimoReinicio = ahora - ultimoReinicio;
        
        if (tiempoDesdeUltimoReinicio >= 1000) {
            iniciarTemporizador();
        }
    }
    
    private void cancelarTemporizadores() {
        if (timerAdvertencia != null) {
            timerAdvertencia.cancel();
            timerAdvertencia.purge();
            timerAdvertencia = null;
        }
        
        if (timerInactividad != null) {
            timerInactividad.cancel();
            timerInactividad.purge();
            timerInactividad = null;
        }
    }
    
    // ==================== BLOQUEO/DESBLOQUEO ====================
    
    private void bloquearInterfaz() {
        if (bloqueado || ventanaPrincipal == null) return;
        
        bloqueado = true;
        cancelarTemporizadores();
        intentosFallidos = 0;
        
        System.out.println(" Interfaz bloqueada por inactividad");
        
        if (callback != null) {
            callback.onBloqueado();
        }
        
        mostrarDialogoBloqueoConBlur();
    }
    
    private void desbloquearInterfaz() {
        bloqueado = false;
        intentosFallidos = 0;
        
        // Animación de fade out del diálogo
        if (dialogoBloqueo != null) {
            fadeOutDialog(dialogoBloqueo, () -> {
                dialogoBloqueo.dispose();
                dialogoBloqueo = null;
                
                // Desactivar blur
                if (blurGlassPane != null) {
                    blurGlassPane.deactivate();
                    blurGlassPane = null;
                }
            });
        }
        
        System.out.println(" Interfaz desbloqueada");
        
        if (callback != null) {
            callback.onDesbloqueado();
        }
        
        contadorEventos = 0;
        iniciarTemporizador();
    }
    
    // ==================== DIÁLOGO CON BLUR MEJORADO ====================
    
    /**
     * Muestra el diálogo de bloqueo con efecto blur y diseño profesional.
     */
    private void mostrarDialogoBloqueoConBlur() {
        // ====================================================================
        // 1. ACTIVAR BLUR EN EL FONDO
        // ====================================================================
        if (ventanaPrincipal != null) {
            blurGlassPane = new BlurGlassPane(ventanaPrincipal);
            blurGlassPane.activate();
        }
        
        // ====================================================================
        // 2. CREAR DIÁLOGO MODAL SIN DECORACIONES
        // ====================================================================
        dialogoBloqueo = new JDialog(ventanaPrincipal, "Sesión Bloqueada", true);
        dialogoBloqueo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogoBloqueo.setResizable(false);
        dialogoBloqueo.setUndecorated(true);
        
        // ====================================================================
        // 3. PANEL PRINCIPAL CON FONDO Y BORDES REDONDEADOS
        // ====================================================================
        JPanel panelPrincipal = new RoundedPanel(20, COLOR_FONDO);
        panelPrincipal.setLayout(new BorderLayout());
        
        // ====================================================================
        // 4. CONTENIDO DEL DIÁLOGO
        // ====================================================================
        JPanel panelContenido = new JPanel();
        panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
        panelContenido.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));
        panelContenido.setBackground(COLOR_FONDO);
        
        // Icono de cerradura (SVG-like usando Unicode)
        JLabel iconoLabel = new JLabel("\uD83D\uDD10");
        iconoLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 72));
        iconoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconoLabel.setForeground(COLOR_ACCENT);
        
        // Animación de pulso sutil para el icono
        Timer pulseTimer = new Timer(true);
        pulseTimer.scheduleAtFixedRate(new TimerTask() {
            boolean enlarged = false;
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    int size = enlarged ? 72 : 80;
                    iconoLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, size));
                    enlarged = !enlarged;
                });
            }
        }, 0, 1500);
        
        // Título principal
        JLabel tituloLabel = new JLabel("Sesión Bloqueada por Inactividad");
        tituloLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        tituloLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tituloLabel.setForeground(COLOR_TEXTO_PRINCIPAL);
        
        // Usuario actual
        String nombreUsuario = UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getNombre()
                : "Usuario";
        
        JLabel usuarioLabel = new JLabel("Usuario: " + nombreUsuario);
        usuarioLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usuarioLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usuarioLabel.setForeground(COLOR_TEXTO_SECUNDARIO);
        
        // Mensaje instruccional
        JLabel mensajeLabel = new JLabel("Ingrese su contraseña para continuar");
        mensajeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        mensajeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mensajeLabel.setForeground(COLOR_TEXTO_SECUNDARIO);
        
        // ====================================================================
        // 5. CAMPO DE CONTRASEÑA MEJORADO
        // ====================================================================
        JPasswordField passwordField = new ModernPasswordField(COLOR_FONDO);
        passwordField.setMaximumSize(new Dimension(380, 48));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        passwordField.setForeground(COLOR_TEXTO_PRINCIPAL);
        passwordField.setCaretColor(COLOR_ACCENT);
        
        // Label de error/success
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(COLOR_ERROR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // ====================================================================
        // 6. BOTÓN DESBLOQUEAR PROFESIONAL
        // ====================================================================
        JButton btnDesbloquear = new ModernButton("Desbloquear", COLOR_ACCENT, COLOR_ACCENT_HOVER);
        btnDesbloquear.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDesbloquear.setPreferredSize(new Dimension(240, 48));
        btnDesbloquear.setMaximumSize(new Dimension(240, 48));
        btnDesbloquear.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        // ====================================================================
        // 7. ACCIÓN DE DESBLOQUEO CON VALIDACIÓN
        // ====================================================================
        ActionListener accionDesbloquear = e -> {
            String password = new String(passwordField.getPassword());
            
            if (password.isEmpty()) {
                errorLabel.setText("Ingrese su contraseña");
                errorLabel.setForeground(COLOR_ERROR);
                passwordField.requestFocus();
                animarShake(passwordField);
                return;
            }
            
            // Validación real contra BD
            if (UserSession.getInstance().validarPasswordActual(password)) {
                // SUCCESS  CONTRASEÑA CORRECTA
                pulseTimer.cancel();
                
                errorLabel.setText("OK Acceso concedido");
                errorLabel.setForeground(COLOR_EXITO);
                
                btnDesbloquear.setEnabled(false);
                passwordField.setEnabled(false);
                
                // Desbloquear después de breve pausa
                Timer unlockTimer = new Timer(true);
                unlockTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> {
                            desbloquearInterfaz();
                            unlockTimer.cancel();
                        });
                    }
                }, 800);
                
            } else {
                // ERROR  CONTRASEÑA INCORRECTA
                intentosFallidos++;
                
                errorLabel.setText("Contraseña incorrecta (" 
                        + intentosFallidos + "/" + MAX_INTENTOS_FALLIDOS + ")");
                errorLabel.setForeground(COLOR_ERROR);
                
                passwordField.setText("");
                passwordField.requestFocus();
                
                // Efectos visuales
                animarShake(passwordField);
                flashRed(passwordField);
                
                // Bloqueo por múltiples intentos
                if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
                    pulseTimer.cancel();
                    
                    errorLabel.setText("Demasiados intentos fallidos");
                    errorLabel.setForeground(COLOR_ERROR);
                    
                    passwordField.setEnabled(false);
                    btnDesbloquear.setEnabled(false);
                    
                    Timer blockTimer = new Timer(true);
                    blockTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(dialogoBloqueo,
                                    "Demasiados intentos fallidos.\n\n"
                                    + "Por seguridad, debe contactar a un administrador.",
                                    "Bloqueo de Seguridad",
                                    JOptionPane.ERROR_MESSAGE);
                                
                                // Cerrar sesión
                                try {
                                    UserSession.getInstance().logout();
                                    raven.application.Application.logout();
                                } catch (Exception ex) {
                                    System.err.println("Error cerrando sesión: " + ex.getMessage());
                                }
                                
                                if (blurGlassPane != null) {
                                    blurGlassPane.deactivate();
                                }
                                
                                dialogoBloqueo.dispose();
                                blockTimer.cancel();
                            });
                        }
                    }, 1000);
                }
            }
        };
        
        btnDesbloquear.addActionListener(accionDesbloquear);
        passwordField.addActionListener(accionDesbloquear);
        
        // ====================================================================
        // 8. AGREGAR COMPONENTES CON ESPACIADO PROFESIONAL
        // ====================================================================
        panelContenido.add(iconoLabel);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 25)));
        panelContenido.add(tituloLabel);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 8)));
        panelContenido.add(usuarioLabel);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 8)));
        panelContenido.add(mensajeLabel);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 35)));
        panelContenido.add(passwordField);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 12)));
        panelContenido.add(errorLabel);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 28)));
        panelContenido.add(btnDesbloquear);
        
        panelPrincipal.add(panelContenido, BorderLayout.CENTER);
        dialogoBloqueo.add(panelPrincipal);
        dialogoBloqueo.pack();
        dialogoBloqueo.setLocationRelativeTo(ventanaPrincipal);
        
        // ====================================================================
        // 9. ANIMACIÓN DE FADE IN
        // ====================================================================
        fadeInDialog(dialogoBloqueo, () -> {
            SwingUtilities.invokeLater(() -> passwordField.requestFocus());
        });
        
        dialogoBloqueo.setVisible(true);
    }
    
    // ==================== COMPONENTES PERSONALIZADOS ====================
    
    /**
     * Panel con bordes redondeados personalizados.
     */
    private static class RoundedPanel extends JPanel {
        private int arcRadius;
        private Color backgroundColor;
        
        public RoundedPanel(int arcRadius, Color backgroundColor) {
            this.arcRadius = arcRadius;
            this.backgroundColor = backgroundColor;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arcRadius, arcRadius);
            
            g2.setColor(COLOR_BORDE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcRadius, arcRadius);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    /**
     * Campo de contraseña con estilo moderno.
     */
    private static class ModernPasswordField extends JPasswordField {
        private Color backgroundColor;
        private boolean focused = false;
        
        public ModernPasswordField(Color parentBg) {
            this.backgroundColor = new Color(45, 55, 72);
            setBackground(backgroundColor);
            setOpaque(false);
            setForeground(COLOR_TEXTO_PRINCIPAL);
            
            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }
                
                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Fondo redondeado
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            
            // Borde según focus
            Color borderColor = focused ? COLOR_ACCENT : COLOR_BORDE;
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    /**
     * Botón con estilo moderno.
     */
    private static class ModernButton extends JButton {
        private Color colorBase;
        private Color colorHover;
        private boolean isHovered = false;
        
        public ModernButton(String text, Color colorBase, Color colorHover) {
            super(text);
            this.colorBase = colorBase;
            this.colorHover = colorHover;
            
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color currentColor = isHovered ? colorHover : colorBase;
            g2.setColor(currentColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(getText())) / 2;
            int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(getText(), textX, textY);
            
            g2.dispose();
        }
    }
    
    /**
     * Fuerza el bloqueo inmediato de la interfaz.
     */
    public void forzarBloqueo() {
        if (!bloqueado) {
            System.out.println(" Bloqueo forzado externamente");
            cancelarTemporizadores();
            SwingUtilities.invokeLater(this::bloquearInterfaz);
        } else {
            System.out.println("WARNING  Ya está bloqueado");
        }
    }
    
    // ==================== ANIMACIONES ====================
    
    /**
     * Animación de fade in para el diálogo.
     */
    private void fadeInDialog(JDialog dialog, Runnable onComplete) {
        dialog.setOpacity(0.0f);
        
        Timer fadeTimer = new Timer(true);
        fadeTimer.scheduleAtFixedRate(new TimerTask() {
            float opacity = 0.0f;
            
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    opacity += 0.08f;
                    if (opacity >= 1.0f) {
                        opacity = 1.0f;
                        dialog.setOpacity(opacity);
                        fadeTimer.cancel();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } else {
                        dialog.setOpacity(opacity);
                    }
                });
            }
        }, 0, 40);
    }
    
    /**
     * Animación de fade out para el diálogo.
     */
    private void fadeOutDialog(JDialog dialog, Runnable onComplete) {
        Timer fadeTimer = new Timer(true);
        fadeTimer.scheduleAtFixedRate(new TimerTask() {
            float opacity = 1.0f;
            
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    opacity -= 0.08f;
                    if (opacity <= 0.0f) {
                        opacity = 0.0f;
                        dialog.setOpacity(opacity);
                        fadeTimer.cancel();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } else {
                        dialog.setOpacity(opacity);
                    }
                });
            }
        }, 0, 40);
    }
    
    /**
     * Efecto shake para componentes.
     */
    private void animarShake(Component component) {
        Point ubicacionOriginal = component.getLocation();
        
        new Thread(() -> {
            try {
                for (int i = 0; i < 4; i++) {
                    component.setLocation(ubicacionOriginal.x + 8, ubicacionOriginal.y);
                    Thread.sleep(60);
                    component.setLocation(ubicacionOriginal.x - 8, ubicacionOriginal.y);
                    Thread.sleep(60);
                }
                component.setLocation(ubicacionOriginal);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Flash rojo para campo de contraseña.
     */
    private void flashRed(Component component) {
        if (component instanceof ModernPasswordField) {
            ModernPasswordField field = (ModernPasswordField) component;
            Timer flashTimer = new Timer(true);
            flashTimer.scheduleAtFixedRate(new TimerTask() {
                int count = 0;
                
                @Override
                public void run() {
                    SwingUtilities.invokeLater(field::repaint);
                    count++;
                    
                    if (count >= 4) {
                        flashTimer.cancel();
                    }
                }
            }, 0, 150);
        }
    }
    
    // ==================== BLUR GLASS PANE ====================
    
    /**
     * Panel de vidrio con efecto blur profesional.
     */
    private class BlurGlassPane extends JComponent {
        
        private final Window window;
        private BufferedImage blurredImage;
        private Component originalGlassPane;
        
        public BlurGlassPane(Window window) {
            this.window = window;
            setOpaque(false);
        }
        
        public void activate() {
            captureWindow();
            applyBlur();
            
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                originalGlassPane = frame.getGlassPane();
                frame.setGlassPane(this);
            } else if (window instanceof JDialog) {
                JDialog dialog = (JDialog) window;
                originalGlassPane = dialog.getGlassPane();
                dialog.setGlassPane(this);
            }
            
            setVisible(true);
            window.repaint();
        }
        
        public void deactivate() {
            setVisible(false);
            
            if (window instanceof JFrame) {
                ((JFrame) window).setGlassPane(originalGlassPane);
            } else if (window instanceof JDialog) {
                ((JDialog) window).setGlassPane(originalGlassPane);
            }
            
            blurredImage = null;
            window.repaint();
        }
        
        private void captureWindow() {
            try {
                Rectangle bounds = window.getBounds();
                blurredImage = new BufferedImage(
                    bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB
                );
                
                Graphics2D g2 = blurredImage.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                window.paint(g2);
                g2.dispose();
            } catch (Exception e) {
                System.err.println("WARNING  Error capturando ventana: " + e.getMessage());
            }
        }
        
        private void applyBlur() {
            if (blurredImage == null) return;
            
            try {
                int size = BLUR_RADIUS * 2 + 1;
                float weight = 1.0f / (size * size);
                float[] kernelData = new float[size * size];
                
                for (int i = 0; i < kernelData.length; i++) {
                    kernelData[i] = weight;
                }
                
                java.awt.image.Kernel kernel = new java.awt.image.Kernel(size, size, kernelData);
                java.awt.image.ConvolveOp convolve = new java.awt.image.ConvolveOp(
                    kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null
                );
                
                blurredImage = convolve.filter(blurredImage, null);
            } catch (Exception e) {
                System.err.println("WARNING  Error aplicando blur: " + e.getMessage());
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g.create();
            
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (blurredImage != null) {
                    g2.drawImage(blurredImage, 0, 0, null);
                }
                
                g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, OVERLAY_OPACITY
                ));
                g2.setColor(OVERLAY_COLOR);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
    }
    
    // ==================== GETTERS ====================
    
    public boolean estaBloqueado() {
        return bloqueado;
    }
    
    public long getContadorEventos() {
        return contadorEventos;
    }
    
    public int getIntentosFallidos() {
        return intentosFallidos;
    } 
}

