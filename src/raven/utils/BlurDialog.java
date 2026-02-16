package raven.utils;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Utilidad para crear diálogos modales con efecto blur en el fondo.
 * 
 * Patrón: Builder + Strategy
 * Principios: Single Responsibility, Open/Closed
 * 
 * @author Sistema de Ventas
 */
public class BlurDialog {
    
    // ==================== CONSTANTES ====================
    private static final int BLUR_RADIUS = 10;
    private static final float OVERLAY_OPACITY = 0.6f;
    private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 150);
    
    // ==================== BUILDER ====================
    
    /**
     * Builder para configurar y crear diálogos con blur.
     * 
     * Ejemplo de uso:
     * <pre>
     * int resultado = new BlurDialog.Builder(parentFrame)
     *     .title("Confirmar Acción")
     *     .message("¿Está seguro?")
     *     .icon(JOptionPane.QUESTION_MESSAGE)
     *     .buttons(JOptionPane.YES_NO_OPTION)
     *     .showConfirmDialog();
     * </pre>
     */
    public static class Builder {
        // Parámetros obligatorios
        private final Component parent;
        
        // Parámetros opcionales con valores por defecto
        private String title = "Confirmación";
        private String message = "";
        private int messageType = JOptionPane.INFORMATION_MESSAGE;
        private int optionType = JOptionPane.DEFAULT_OPTION;
        private Icon customIcon = null;
        private Object[] options = null;
        private Object initialValue = null;
        
        /**
         * Constructor del Builder.
         * 
         * @param parent Componente padre (JFrame, JPanel, etc.)
         */
        public Builder(Component parent) {
            this.parent = parent;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder icon(int messageType) {
            this.messageType = messageType;
            return this;
        }
        
        public Builder customIcon(Icon icon) {
            this.customIcon = icon;
            return this;
        }
        
        public Builder buttons(int optionType) {
            this.optionType = optionType;
            return this;
        }
        
        public Builder customButtons(Object[] options, Object initialValue) {
            this.options = options;
            this.initialValue = initialValue;
            return this;
        }
        
        /**
         * Muestra un diálogo de confirmación con blur.
         * 
         * @return Opción seleccionada por el usuario
         */
        public int showConfirmDialog() {
            return showDialog();
        }
        
        /**
         * Muestra un diálogo de mensaje con blur.
         */
        public void showMessageDialog() {
            showDialog();
        }
        
        /**
         * Muestra el diálogo con blur aplicado.
         */
        private int showDialog() {
            Window window = getWindow(parent);
            if (window == null) {
                // Fallback sin blur si no hay ventana padre
                return showStandardDialog();
            }
            
            // Crear overlay con blur
            BlurGlassPane glassPane = new BlurGlassPane(window);
            
            try {
                // Activar blur
                glassPane.activate();
                
                // Mostrar diálogo
                return showStandardDialog();
                
            } finally {
                // Desactivar blur automáticamente
                glassPane.deactivate();
            }
        }
        
        /**
         * Muestra el diálogo estándar de Swing.
         */
        private int showStandardDialog() {
            if (options != null) {
                boolean hasComponent = false;
                for (Object o : options) {
                    if (o instanceof Component) {
                        hasComponent = true;
                        break;
                    }
                }
                if (hasComponent) {
                    JOptionPane optionPane = new JOptionPane(
                            message,
                            messageType,
                            optionType,
                            customIcon,
                            null,
                            initialValue
                    );
                    optionPane.setOptions(options);

                    JDialog dialog = optionPane.createDialog(parent, title);
                    dialog.setModal(true);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                    for (Object opt : options) {
                        if (opt instanceof AbstractButton) {
                            ((AbstractButton) opt).addActionListener(e -> {
                                optionPane.setValue(opt);
                                dialog.dispose();
                            });
                        }
                    }

                    dialog.setVisible(true);

                    Object value = optionPane.getValue();
                    if (value == null || value == JOptionPane.UNINITIALIZED_VALUE) {
                        return JOptionPane.CLOSED_OPTION;
                    }
                    for (int i = 0; i < options.length; i++) {
                        if (options[i] == value || (options[i] != null && options[i].equals(value))) {
                            return i;
                        }
                    }
                    return JOptionPane.CLOSED_OPTION;
                }
                return JOptionPane.showOptionDialog(
                    parent,
                    message,
                    title,
                    optionType,
                    messageType,
                    customIcon,
                    options,
                    initialValue
                );
            } else {
                return JOptionPane.showConfirmDialog(
                    parent,
                    message,
                    title,
                    optionType,
                    messageType
                );
            }
        }
        
        /**
         * Obtiene la ventana padre del componente.
         */
        private Window getWindow(Component component) {
            if (component == null) {
                return null;
            }
            
            if (component instanceof Window) {
                return (Window) component;
            }
            
            return SwingUtilities.getWindowAncestor(component);
        }
    }
    
    // ==================== GLASS PANE CON BLUR ====================
    
    /**
     * Panel de vidrio que aplica efecto blur al fondo.
     * 
     * Patrón: Decorator (decora la ventana con blur)
     */
    private static class BlurGlassPane extends JComponent {
        
        private final Window window;
        private BufferedImage blurredImage;
        private Component originalGlassPane;
        
        public BlurGlassPane(Window window) {
            this.window = window;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        
        /**
         * Activa el efecto blur.
         */
        public void activate() {
            // Capturar imagen de la ventana
            captureWindow();
            
            // Aplicar blur a la imagen
            applyBlur();
            
            // Guardar glass pane original
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
            
            // Forzar repintado
            window.repaint();
        }
        
        /**
         * Desactiva el efecto blur.
         */
        public void deactivate() {
            setVisible(false);
            
            // Restaurar glass pane original
            if (window instanceof JFrame) {
                ((JFrame) window).setGlassPane(originalGlassPane);
            } else if (window instanceof JDialog) {
                ((JDialog) window).setGlassPane(originalGlassPane);
            }
            
            // Limpiar imagen
            blurredImage = null;
            
            window.repaint();
        }
        
        /**
         * Captura una imagen de la ventana actual.
         */
        private void captureWindow() {
            try {
                Rectangle bounds = window.getBounds();
                blurredImage = new BufferedImage(
                    bounds.width,
                    bounds.height,
                    BufferedImage.TYPE_INT_ARGB
                );
                
                Graphics2D g2 = blurredImage.createGraphics();
                
                // Configurar renderizado de calidad
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                );
                
                // Pintar contenido de la ventana
                window.paint(g2);
                g2.dispose();
                
            } catch (Exception e) {
                System.err.println("WARNING  Error capturando ventana: " + e.getMessage());
                blurredImage = null;
            }
        }
        
        /**
         * Aplica efecto blur a la imagen capturada.
         * 
         * Usa convolución con kernel gaussiano para blur suave.
         */
        private void applyBlur() {
            if (blurredImage == null) return;
            
            try {
                // Crear kernel gaussiano para blur
                int size = BLUR_RADIUS * 2 + 1;
                float weight = 1.0f / (size * size);
                float[] kernelData = new float[size * size];
                
                for (int i = 0; i < kernelData.length; i++) {
                    kernelData[i] = weight;
                }
                
                java.awt.image.Kernel kernel = new java.awt.image.Kernel(
                    size, size, kernelData
                );
                
                // Aplicar convolución
                java.awt.image.ConvolveOp convolve = new java.awt.image.ConvolveOp(
                    kernel,
                    java.awt.image.ConvolveOp.EDGE_NO_OP,
                    null
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
                // Configurar calidad de renderizado
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                
                // 1. Dibujar imagen blurred
                if (blurredImage != null) {
                    g2.drawImage(blurredImage, 0, 0, null);
                }
                
                // 2. Aplicar overlay semi-transparente
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
}
