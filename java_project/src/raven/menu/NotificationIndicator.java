package raven.menu;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URL;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JLabel;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Indicador de notificaciones junto al logo.
 * Intenta reproducir una animación Lottie si hay soporte; de lo contrario,
 * muestra una campana con animación usando Ikonli como fallback.
 */
public class NotificationIndicator extends JComponent {

    private boolean active;
    private float pulse;
    private javax.swing.Timer pulseTimer;
    private double phase;
    private Object lottiePanel; // Usado si hay soporte JavaFX/Lottie
    private boolean lottieAvailable;
    private JLabel bellLabel;    // Fallback visual con Ikonli
    private boolean bellFallback;

    public NotificationIndicator() {
        setOpaque(false);
        setPreferredSize(new Dimension(28, 28));
        setVisible(false);

        // Preparar pulso para animación
        phase = 0d;
        pulseTimer = new javax.swing.Timer(16, e -> {
            phase += 0.08; // velocidad del pulso
            if (phase > Math.PI * 2) phase -= Math.PI * 2;
            pulse = (float) ((Math.sin(phase) + 1) / 2); // 0..1
            // Animar el color de la campana en fallback
            if (bellFallback && bellLabel != null) {
                // Oscilar entre naranja y amarillo
                int r = 255;
                int g = (int) (120 + 80 * pulse);  // 120..200
                int b = 0;
                bellLabel.setIcon(FontIcon.of(FontAwesomeSolid.BELL, 18, new Color(r, g, b)));
                bellLabel.repaint();
            }
            repaint();
        });

        // Intentar cargar Lottie (si la librería está disponible en tiempo de ejecución)
        try {
            Class.forName("javafx.embed.swing.JFXPanel");
            // Presencia de JavaFX
            // Intentar Lottie4J APIs por reflexión para no romper compilación si no existe
            lottieAvailable = isLottie4JPresent();
            if (lottieAvailable) {
                initLottiePanel();
            }
        } catch (ClassNotFoundException ex) {
            lottieAvailable = false;
        }

        // Si Lottie no está disponible, configurar fallback con campana Ikonli
        if (!lottieAvailable) {
            setupBellFallback();
        }
    }

    private boolean isLottie4JPresent() {
        try {
            Class.forName("com.lottie4j.core.handler.LottieFileLoader");
            Class.forName("com.lottie4j.fxplayer.LottiePlayer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void initLottiePanel() {
        try {
            // Buscar JSON o paquete .lottie desde classpath
            URL jsonUrl = getClass().getResource("/raven/icon/json/notificacion.json");
            if (jsonUrl == null) {
                jsonUrl = getClass().getResource("/raven/icon/json/notification.json");
            }
            File jsonFile = null;
            if (jsonUrl != null) {
                jsonFile = new File(jsonUrl.getPath());
            } else {
                URL lottieUrl = getClass().getResource("/raven/icon/lottie/notification.lottie");
                if (lottieUrl == null) {
                    // También admitir que el .lottie esté ubicado bajo json como en este proyecto
                    lottieUrl = getClass().getResource("/raven/icon/json/notification.lottie");
                }
                if (lottieUrl != null) {
                    // Extraer primer JSON del paquete .lottie a archivo temporal
                    try (InputStream is = lottieUrl.openStream(); ZipInputStream zis = new ZipInputStream(is)) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".json")) {
                                File tmp = File.createTempFile("lottie", ".json");
                                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                                    byte[] buf = new byte[4096];
                                    int r;
                                    while ((r = zis.read(buf)) > 0) {
                                        fos.write(buf, 0, r);
                                    }
                                }
                                jsonFile = tmp;
                                break;
                            }
                        }
                    }
                }
            }
            if (jsonFile == null) {
                lottieAvailable = false;
                return;
            }
            final File lottieJsonFile = jsonFile; // efectivamente final para lambda
            // Crear JFXPanel y cargar la animación mediante reflexión
            // Evitar dependencia directa en compilación
            Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
            Object jfxPanel = jfxPanelClass.getConstructor().newInstance();
            lottiePanel = jfxPanel;
            // Añadir el panel JavaFX dentro de este componente Swing
            setLayout(new BorderLayout());
            add((Component) lottiePanel, BorderLayout.CENTER);
            revalidate();
            repaint();
            // Inicializar JavaFX en EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    // Iniciar plataforma JavaFX
                    Class<?> platformClass = Class.forName("javafx.application.Platform");
                    platformClass.getMethod("runLater", Runnable.class)
                            .invoke(null, (Runnable) () -> {
                                try {
                                    Class<?> loaderClass = Class.forName("com.lottie4j.core.handler.LottieFileLoader");
                                    Class<?> animationClass = Class.forName("com.lottie4j.core.model.Animation");
                                    Object animation = loaderClass.getMethod("load", java.io.File.class)
                                            .invoke(null, lottieJsonFile);

                                    Class<?> playerClass = Class.forName("com.lottie4j.fxplayer.LottiePlayer");
                                    Object player = playerClass.getConstructor(animationClass).newInstance(animation);

                                    // Crear escena y asignar al JFXPanel
                                    Class<?> sceneClass = Class.forName("javafx.scene.Scene");
                                    Class<?> groupClass = Class.forName("javafx.scene.layout.HBox");
                                    Object group = groupClass.getConstructor().newInstance();
                                    groupClass.getMethod("getChildren").invoke(group);
                                    // group.getChildren().add(player)
                                    Object children = groupClass.getMethod("getChildren").invoke(group);
                                    Class<?> observableListClass = children.getClass();
                                    observableListClass.getMethod("add", Object.class).invoke(children, player);

                                    Object scene = sceneClass.getConstructor(Class.forName("javafx.scene.Parent"), double.class, double.class)
                                            .newInstance(group, 28d, 28d);

                                    jfxPanelClass.getMethod("setScene", sceneClass).invoke(lottiePanel, scene);
                                    // Actualizar visualización tras asignar escena
                                    SwingUtilities.invokeLater(() -> {
                                        revalidate();
                                        repaint();
                                    });
                                } catch (Exception ignore) {
                                    lottieAvailable = false;
                                }
                            });
                } catch (Exception ignore) {
                    lottieAvailable = false;
                }
            });
        } catch (Exception ex) {
            lottieAvailable = false;
        }
    }

    private void setupBellFallback() {
        try {
            setLayout(new BorderLayout());
            bellLabel = new JLabel();
            bellLabel.setHorizontalAlignment(JLabel.CENTER);
            bellLabel.setVerticalAlignment(JLabel.CENTER);
            bellLabel.setIcon(FontIcon.of(FontAwesomeSolid.BELL, 18, new Color(255, 140, 0)));
            add(bellLabel, BorderLayout.CENTER);
            bellFallback = true;
        } catch (Throwable t) {
            // Si Ikonli no estuviera disponible por alguna razón, continuar con punto pulsante
            bellFallback = false;
        }
    }

    public void showIndicator() {
        active = true;
        setVisible(true);
        if (lottieAvailable) {
            // Lottie se reproduce por defecto; no requiere pulso
        } else {
            if (!pulseTimer.isRunning()) pulseTimer.start();
        }
        repaint();
    }

    public void hideIndicator() {
        active = false;
        setVisible(false);
        if (pulseTimer.isRunning()) pulseTimer.stop();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!active || lottieAvailable || bellFallback) {
            return; // No dibujar punto si hay Lottie o campana Ikonli
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight());
        int base = size - 8;
        int glow = (int) (6 * Math.abs(0.5f - pulse) * 2); // pulso 0..1
        int d = base + glow;
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;

        // Sombra/glow
        g2.setColor(new Color(255, 140, 0, 80));
        g2.fillOval(x, y, d, d);

        // Punto central
        int cx = (getWidth() - base) / 2;
        int cy = (getHeight() - base) / 2;
        g2.setColor(new Color(255, 94, 0));
        g2.fillOval(cx, cy, base, base);

        g2.dispose();
    }
}