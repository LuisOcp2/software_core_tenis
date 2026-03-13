package raven.componentes;

import java.awt.*;
import javax.swing.*;

/**
 * Helper class para simplificar el uso de LoadingSpinner en formularios.
 * Maneja la creación, visualización y ocultación de overlays de loading.
 *
 * USO TÍPICO:
 * <pre>
 * // Al inicio del proceso
 * LoadingOverlayHelper.showLoading(panelInfoProd, "Cargando producto...");
 *
 * // Ejecutar tarea en segundo plano
 * SwingWorker worker = new SwingWorker() {
 *     protected Object doInBackground() {
 *         // Cargar producto...
 *         return producto;
 *     }
 *     protected void done() {
 *         LoadingOverlayHelper.hideLoading(panelInfoProd);
 *         // Actualizar UI...
 *     }
 * };
 * worker.execute();
 * </pre>
 *
 * @author Sistema de Gestión Zapatos Xtreme
 * @version 1.0
 */
public class LoadingOverlayHelper {

    // Clave para almacenar el overlay en las propiedades del componente
    private static final String OVERLAY_KEY = "loadingOverlay";
    private static final String LABEL_KEY = "loadingLabel";

    // Colores por defecto
    private static final Color DEFAULT_SPINNER_COLOR = new Color(52, 152, 219); // Azul
    private static final Color DEFAULT_BG_COLOR = new Color(255, 255, 255, 220); // Blanco semi-transparente

    /**
     * Muestra un overlay de loading sobre un componente.
     *
     * @param targetComponent Componente sobre el cual mostrar el loading
     */
    public static void showLoading(JComponent targetComponent) {
        showLoading(targetComponent, null, DEFAULT_SPINNER_COLOR);
    }

    /**
     * Muestra un overlay de loading con mensaje.
     *
     * @param targetComponent Componente sobre el cual mostrar el loading
     * @param message Mensaje a mostrar (puede ser null)
     */
    public static void showLoading(JComponent targetComponent, String message) {
        showLoading(targetComponent, message, DEFAULT_SPINNER_COLOR);
    }

    /**
     * Muestra un overlay de loading con mensaje y color personalizado.
     *
     * @param targetComponent Componente sobre el cual mostrar el loading
     * @param message Mensaje a mostrar (puede ser null)
     * @param spinnerColor Color del spinner
     */
    public static void showLoading(JComponent targetComponent, String message, Color spinnerColor) {
        // Verificar si ya hay un overlay activo
        if (targetComponent.getClientProperty(OVERLAY_KEY) != null) {
            System.out.println("WARNING  Ya hay un loading activo en este componente");
            return;
        }

        // Crear overlay panel
        JPanel overlayPanel = createOverlayPanel(targetComponent, message, spinnerColor);

        // Guardar referencia al overlay
        targetComponent.putClientProperty(OVERLAY_KEY, overlayPanel);

        // Mostrar overlay
        showOverlayOnComponent(targetComponent, overlayPanel);

        System.out.println("Actualizando Loading overlay mostrado en: " + targetComponent.getName());
    }

    /**
     * Oculta el overlay de loading de un componente.
     *
     * @param targetComponent Componente del cual ocultar el loading
     */
    public static void hideLoading(JComponent targetComponent) {
        // Obtener overlay
        JPanel overlayPanel = (JPanel) targetComponent.getClientProperty(OVERLAY_KEY);

        if (overlayPanel == null) {
            System.out.println("WARNING  No hay loading activo para ocultar");
            return;
        }

        // Detener spinner
        LoadingSpinner spinner = (LoadingSpinner) overlayPanel.getClientProperty("spinner");
        if (spinner != null) {
            spinner.stop();
        }

        // Remover overlay
        Container parent = overlayPanel.getParent();
        if (parent != null) {
            parent.remove(overlayPanel);
            parent.revalidate();
            parent.repaint();
        }

        // Limpiar referencia
        targetComponent.putClientProperty(OVERLAY_KEY, null);

        System.out.println(" Loading overlay ocultado");
    }

    /**
     * Verifica si hay un loading activo en un componente.
     *
     * @param targetComponent Componente a verificar
     * @return true si hay un loading activo
     */
    public static boolean isLoadingActive(JComponent targetComponent) {
        return targetComponent.getClientProperty(OVERLAY_KEY) != null;
    }

    /**
     * Actualiza el mensaje del loading activo.
     *
     * @param targetComponent Componente con loading activo
     * @param newMessage Nuevo mensaje
     */
    public static void updateMessage(JComponent targetComponent, String newMessage) {
        JPanel overlayPanel = (JPanel) targetComponent.getClientProperty(OVERLAY_KEY);

        if (overlayPanel == null) {
            System.out.println("WARNING  No hay loading activo para actualizar");
            return;
        }

        JLabel label = (JLabel) overlayPanel.getClientProperty(LABEL_KEY);
        if (label != null && newMessage != null) {
            label.setText(newMessage);
        }
    }

    // ============================================================
    // MÉTODOS PRIVADOS
    // ============================================================

    /**
     * Crea el panel overlay con spinner y mensaje.
     */
    private static JPanel createOverlayPanel(JComponent targetComponent, String message, Color spinnerColor) {
        // Crear panel overlay con fondo semi-transparente
        JPanel overlayPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Fondo semi-transparente con gradiente suave
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradiente sutil desde el centro
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = Math.max(getWidth(), getHeight());

                RadialGradientPaint gradient = new RadialGradientPaint(
                    centerX, centerY, radius,
                    new float[]{0.0f, 1.0f},
                    new Color[]{
                        new Color(255, 255, 255, 240),
                        new Color(255, 255, 255, 200)
                    }
                );

                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };

        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(targetComponent.getBounds());

        // Crear contenedor vertical para spinner y mensaje
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Crear spinner
        LoadingSpinner spinner = new LoadingSpinner(spinnerColor, 50, 4, 12);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Agregar spinner
        contentPanel.add(spinner);
        contentPanel.add(Box.createVerticalStrut(15)); // Espaciado

        // Agregar mensaje si existe
        JLabel messageLabel = null;
        if (message != null && !message.trim().isEmpty()) {
            messageLabel = new JLabel(message);
            messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            messageLabel.setForeground(new Color(60, 60, 60));
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(messageLabel);
        }

        // Configurar GridBagConstraints para centrar
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        overlayPanel.add(contentPanel, gbc);

        // Guardar referencias
        overlayPanel.putClientProperty("spinner", spinner);
        if (messageLabel != null) {
            overlayPanel.putClientProperty(LABEL_KEY, messageLabel);
        }

        return overlayPanel;
    }

    /**
     * Muestra el overlay sobre el componente objetivo.
     */
    private static void showOverlayOnComponent(JComponent targetComponent, JPanel overlayPanel) {
        // Obtener el contenedor padre
        Container parent = targetComponent.getParent();

        if (parent == null) {
            System.err.println("ERROR  El componente objetivo no tiene padre");
            return;
        }

        // Calcular posición y tamaño del overlay
        Point location = targetComponent.getLocation();
        Dimension size = targetComponent.getSize();
        overlayPanel.setBounds(location.x, location.y, size.width, size.height);

        // Estrategia 1: Intentar usar JLayeredPane
        if (parent instanceof JLayeredPane) {
            JLayeredPane layeredPane = (JLayeredPane) parent;
            layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
            layeredPane.revalidate();
            layeredPane.repaint();

        } else {
            // Estrategia 2: Buscar JLayeredPane en la jerarquía
            Component current = parent;
            JLayeredPane layeredPane = null;

            while (current != null && layeredPane == null) {
                if (current instanceof JLayeredPane) {
                    // Evitar JLayeredPane con CardLayout que causa conflictos con constraints
                    if (!(((JLayeredPane) current).getLayout() instanceof CardLayout)) {
                        layeredPane = (JLayeredPane) current;
                    }
                } else if (current instanceof JRootPane) {
                    layeredPane = ((JRootPane) current).getLayeredPane();
                }
                current = current.getParent();
            }

            if (layeredPane != null) {
                // Convertir coordenadas al JLayeredPane
                Point componentLocation = SwingUtilities.convertPoint(
                    parent, location, layeredPane
                );
                overlayPanel.setBounds(componentLocation.x, componentLocation.y,
                                     size.width, size.height);

                layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
                layeredPane.revalidate();
                layeredPane.repaint();

            } else {
                // Estrategia 3: Agregar directamente al padre (última opción)
                parent.add(overlayPanel);
                parent.setComponentZOrder(overlayPanel, 0); // Traer al frente
                parent.revalidate();
                parent.repaint();
            }
        }

        // Iniciar spinner
        LoadingSpinner spinner = (LoadingSpinner) overlayPanel.getClientProperty("spinner");
        if (spinner != null) {
            spinner.start();
        }
    }

    /**
     * Muestra loading y ejecuta una tarea en segundo plano.
     * Oculta automáticamente el loading cuando termina.
     *
     * @param targetComponent Componente sobre el cual mostrar el loading
     * @param message Mensaje del loading
     * @param task Tarea a ejecutar en segundo plano
     * @param onComplete Callback cuando termina (en EDT)
     */
    public static void runWithLoading(
        JComponent targetComponent,
        String message,
        Runnable task,
        Runnable onComplete
    ) {
        // Mostrar loading
        showLoading(targetComponent, message);

        // Ejecutar tarea en SwingWorker
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Ejecutar tarea
                task.run();
                return null;
            }

            @Override
            protected void done() {
                // Ocultar loading
                hideLoading(targetComponent);

                // Ejecutar callback
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        };

        worker.execute();
    }
}

