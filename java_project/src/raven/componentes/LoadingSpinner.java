package raven.componentes;

import java.awt.*;
import java.awt.geom.Arc2D;
import javax.swing.*;

/**
 * Componente de loading spinner circular optimizado.
 * Diseñado para ser suave y adaptivo en equipos de bajo rendimiento.
 *
 * Características:
 * - Animación fluida con Timer de Swing
 * - Renderizado optimizado con Graphics2D
 * - Anti-aliasing para suavidad visual
 * - Configurable (color, tamaño, velocidad)
 * - Bajo consumo de recursos
 *
 * @author Sistema de Gestión Zapatos Xtreme
 * @version 1.0
 */
public class LoadingSpinner extends JComponent {

    // ============================================================
    // CONFIGURACIÓN
    // ============================================================
    private static final int DEFAULT_SIZE = 60;           // Tamaño por defecto
    private static final int DEFAULT_STROKE_WIDTH = 4;    // Grosor del círculo
    private static final int DEFAULT_FPS = 30;            // Frames por segundo (optimizado)
    private static final int DEFAULT_ROTATION_SPEED = 10; // Grados por frame

    // ============================================================
    // ESTADO
    // ============================================================
    private int angle = 0;                    // Ángulo actual de rotación
    private Timer animationTimer;             // Timer para animación
    private Color spinnerColor;               // Color del spinner
    private int strokeWidth;                  // Grosor del trazo
    private int rotationSpeed;                // Velocidad de rotación
    private boolean isRunning = false;        // Estado de la animación

    // Tamaño del componente
    private int spinnerSize = DEFAULT_SIZE;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================

    /**
     * Constructor por defecto.
     * Crea un spinner con configuración estándar.
     */
    public LoadingSpinner() {
        this(new Color(52, 152, 219), DEFAULT_SIZE, DEFAULT_STROKE_WIDTH, DEFAULT_ROTATION_SPEED);
    }

    /**
     * Constructor con color personalizado.
     *
     * @param color Color del spinner
     */
    public LoadingSpinner(Color color) {
        this(color, DEFAULT_SIZE, DEFAULT_STROKE_WIDTH, DEFAULT_ROTATION_SPEED);
    }

    /**
     * Constructor completo con todas las opciones.
     *
     * @param color Color del spinner
     * @param size Tamaño del spinner (diámetro)
     * @param strokeWidth Grosor del trazo
     * @param rotationSpeed Velocidad de rotación (grados por frame)
     */
    public LoadingSpinner(Color color, int size, int strokeWidth, int rotationSpeed) {
        this.spinnerColor = color;
        this.spinnerSize = size;
        this.strokeWidth = strokeWidth;
        this.rotationSpeed = rotationSpeed;

        // Configurar tamaño preferido
        setPreferredSize(new Dimension(spinnerSize, spinnerSize));
        setSize(spinnerSize, spinnerSize);

        // Hacer componente transparente
        setOpaque(false);

        // Inicializar timer (pero NO iniciar)
        initializeTimer();
    }

    // ============================================================
    // MÉTODOS PÚBLICOS
    // ============================================================

    /**
     * Inicia la animación del spinner.
     */
    public void start() {
        if (!isRunning && animationTimer != null) {
            isRunning = true;
            animationTimer.start();
            setVisible(true);
            System.out.println("Actualizando LoadingSpinner: Iniciado");
        }
    }

    /**
     * Detiene la animación del spinner.
     */
    public void stop() {
        if (isRunning && animationTimer != null) {
            isRunning = false;
            animationTimer.stop();
            setVisible(false);
            System.out.println(" LoadingSpinner: Detenido");
        }
    }

    /**
     * Verifica si el spinner está en ejecución.
     *
     * @return true si está corriendo, false si está detenido
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Cambia el color del spinner.
     *
     * @param color Nuevo color
     */
    public void setSpinnerColor(Color color) {
        this.spinnerColor = color;
        repaint();
    }

    /**
     * Cambia el tamaño del spinner.
     *
     * @param size Nuevo tamaño (diámetro)
     */
    public void setSpinnerSize(int size) {
        this.spinnerSize = size;
        setPreferredSize(new Dimension(spinnerSize, spinnerSize));
        setSize(spinnerSize, spinnerSize);
        revalidate();
        repaint();
    }

    // ============================================================
    // MÉTODOS PRIVADOS
    // ============================================================

    /**
     * Inicializa el timer de animación.
     */
    private void initializeTimer() {
        // Calcular delay en milisegundos basado en FPS
        int delay = 1000 / DEFAULT_FPS;

        animationTimer = new Timer(delay, e -> {
            // Incrementar ángulo
            angle = (angle + rotationSpeed) % 360;

            // Repintar solo este componente (no el padre)
            repaint();
        });

        // Configurar timer para ser ligero
        animationTimer.setRepeats(true);
        animationTimer.setCoalesce(true); // Combinar eventos si hay retraso
    }

    // ============================================================
    // RENDERIZADO
    // ============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!isVisible() || !isRunning) {
            return;
        }

        // Configurar Graphics2D para renderizado suave
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            // ========== CONFIGURACIÓN DE CALIDAD ==========
            // Anti-aliasing para bordes suaves
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            // Renderizado de calidad (pero no el más alto para rendimiento)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_QUALITY);

            // Interpolación suave
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // ========== CALCULAR DIMENSIONES ==========
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            // Tamaño del círculo (dejar margen para el grosor del trazo)
            int diameter = Math.min(width, height) - (strokeWidth * 2);
            int radius = diameter / 2;

            // Posición del círculo (centrado)
            int x = centerX - radius;
            int y = centerY - radius;

            // ========== DIBUJAR SPINNER ==========
            // Configurar trazo
            g2d.setStroke(new BasicStroke(
                strokeWidth,
                BasicStroke.CAP_ROUND,  // Bordes redondeados
                BasicStroke.JOIN_ROUND
            ));

            // Fondo del círculo (círculo completo con transparencia)
            g2d.setColor(new Color(
                spinnerColor.getRed(),
                spinnerColor.getGreen(),
                spinnerColor.getBlue(),
                30  // Muy transparente
            ));
            g2d.drawOval(x, y, diameter, diameter);

            // Arco animado (270 grados)
            g2d.setColor(spinnerColor);
            Arc2D.Double arc = new Arc2D.Double(
                x, y, diameter, diameter,
                angle,      // Ángulo de inicio
                270,        // Extensión del arco (270 grados)
                Arc2D.OPEN
            );
            g2d.draw(arc);

        } finally {
            g2d.dispose();
        }
    }

    // ============================================================
    // MÉTODOS ESTÁTICOS AUXILIARES
    // ============================================================

    /**
     * Crea un panel overlay transparente con el spinner centrado.
     * Útil para superponer sobre otro componente.
     *
     * @param targetComponent Componente sobre el cual se mostrará el overlay
     * @param spinnerColor Color del spinner
     * @return JPanel configurado como overlay con spinner
     */
    public static JPanel createOverlay(JComponent targetComponent, Color spinnerColor) {
        // Crear panel overlay
        JPanel overlayPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Fondo semi-transparente
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(255, 255, 255, 200)); // Blanco semi-transparente
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };

        // Configurar overlay
        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(targetComponent.getBounds());

        // Crear spinner
        LoadingSpinner spinner = new LoadingSpinner(spinnerColor);

        // Agregar spinner al centro del overlay
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        overlayPanel.add(spinner, gbc);

        // Guardar referencia al spinner en el overlay
        overlayPanel.putClientProperty("spinner", spinner);

        return overlayPanel;
    }

    /**
     * Muestra el overlay de loading sobre un componente.
     *
     * @param targetComponent Componente sobre el cual mostrar el loading
     * @param overlayPanel Panel overlay creado con createOverlay()
     */
    public static void showOverlay(JComponent targetComponent, JPanel overlayPanel) {
        // Obtener el layered pane del componente
        Container parent = targetComponent.getParent();

        if (parent instanceof JLayeredPane) {
            JLayeredPane layeredPane = (JLayeredPane) parent;

            // Ajustar posición y tamaño del overlay
            overlayPanel.setBounds(targetComponent.getBounds());

            // Agregar overlay en la capa superior
            layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
            layeredPane.revalidate();
            layeredPane.repaint();

        } else {
            // Si no hay JLayeredPane, agregar directamente al padre
            overlayPanel.setBounds(targetComponent.getBounds());
            parent.add(overlayPanel);
            parent.setComponentZOrder(overlayPanel, 0); // Traer al frente
            parent.revalidate();
            parent.repaint();
        }

        // Iniciar spinner
        LoadingSpinner spinner = (LoadingSpinner) overlayPanel.getClientProperty("spinner");
        if (spinner != null) {
            spinner.start();
        }
    }

    /**
     * Oculta el overlay de loading.
     *
     * @param overlayPanel Panel overlay a ocultar
     */
    public static void hideOverlay(JPanel overlayPanel) {
        // Detener spinner
        LoadingSpinner spinner = (LoadingSpinner) overlayPanel.getClientProperty("spinner");
        if (spinner != null) {
            spinner.stop();
        }

        // Remover overlay de su padre
        Container parent = overlayPanel.getParent();
        if (parent != null) {
            parent.remove(overlayPanel);
            parent.revalidate();
            parent.repaint();
        }
    }
}

