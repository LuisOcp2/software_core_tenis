package raven.componentes;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * ScrollPane con bordes redondeados y estilo moderno
 * 
 * Características:
 * - Bordes completamente redondeados
 * - Scrollbar personalizado con estilo moderno
 * - Sombra suave alrededor
 * - Compatible con cualquier componente (tabla, panel, etc)
 * 
 * Ejemplo de uso:
 * RoundedScrollPane scrollPane = new RoundedScrollPane(table);
 * scrollPane.setRound(15);
 * scrollPane.setBackground(new Color(245, 245, 245));
 * 
 * @author Desarrollador
 * @version 2.0
 */
public class RoundedScrollPane extends JScrollPane {
    
    private int round = 20;
    private Color shadowColor = new Color(0, 0, 0, 60);
    private Color borderColor = new Color(200, 200, 200, 100);
    private BufferedImage imageShadow;
    private final Insets shadowSize = new Insets(3, 8, 8, 8);
    
    /**
     * Constructor con componente
     */
    public RoundedScrollPane(Component view) {
        super(view);
        configurarEstilo();
    }
    
    /**
     * Constructor vacío
     */
    public RoundedScrollPane() {
        super();
        configurarEstilo();
    }
    
    /**
     * Configura los estilos iniciales
     */
    private void configurarEstilo() {
        // Hacer transparente el scroll pane
        setOpaque(false);
        setBorder(new EmptyBorder(shadowSize.top, shadowSize.left, shadowSize.bottom, shadowSize.right));
        
        // Configurar viewport
        getViewport().setOpaque(false);
        
        // Personalizar scrollbars vertical
        JScrollBar vBar = getVerticalScrollBar();
        vBar.setUI(new ModernScrollBarUI());
        vBar.setPreferredSize(new Dimension(8, 0));
        vBar.setOpaque(false);
        
        // Personalizar scrollbars horizontal
        JScrollBar hBar = getHorizontalScrollBar();
        hBar.setUI(new ModernScrollBarUI());
        hBar.setPreferredSize(new Dimension(0, 8));
        hBar.setOpaque(false);
        
        // Corner
        setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        });
        
        createImageShadow();
    }
    
    /**
     * ScrollBar UI personalizado moderno
     */
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        private Color thumbColor = new Color(150, 150, 150, 200);
        private Color thumbHoverColor = new Color(100, 100, 100, 250);
        private Color thumbPressedColor = new Color(80, 80, 80, 250);
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (thumbBounds.isEmpty() || !c.isEnabled()) {
                return;
            }
            
            // Determinar color según estado
            Color color;
            if (isDragging) {
                color = thumbPressedColor;
            } else if (isThumbRollover()) {
                color = thumbHoverColor;
            } else {
                color = thumbColor;
            }
            
            // Dibujar thumb redondeado
            int radius = Math.min(thumbBounds.width, thumbBounds.height) / 2;
            g2.setColor(color);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, radius, radius);
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // No dibujar track
        }
        
        @Override
        protected void paintDecreaseHighlight(Graphics g) {
            // No dibujar
        }
        
        @Override
        protected void paintIncreaseHighlight(Graphics g) {
            // No dibujar
        }
    }
    
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Dibujar sombra
        if (imageShadow != null) {
            g2.drawImage(imageShadow, 0, 0, null);
        }
        
        // Calcular área útil (sin sombra)
        double areaWidth = width - (shadowSize.left + shadowSize.right);
        double areaHeight = height - (shadowSize.top + shadowSize.bottom);
        double x = shadowSize.left;
        double y = shadowSize.top;
        
        // Crear forma redondeada
        RoundRectangle2D.Double forma = new RoundRectangle2D.Double(x, y, areaWidth, areaHeight, round, round);
        Area area = new Area(forma);
        
        // Aplicar clip para que el contenido respete los bordes redondeados
        g2.setClip(area);
        
        // Dibujar fondo
        g2.setColor(getBackground());
        g2.fillRect(0, 0, width, height);
        
        g2.dispose();
        super.paintComponent(grphcs);
    }
    
    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Calcular área útil
        double areaWidth = width - (shadowSize.left + shadowSize.right);
        double areaHeight = height - (shadowSize.top + shadowSize.bottom);
        double x = shadowSize.left;
        double y = shadowSize.top;
        
        // Crear forma redondeada
        RoundRectangle2D.Double forma = new RoundRectangle2D.Double(x, y, areaWidth, areaHeight, round, round);
        Area area = new Area(forma);
        
        // Aplicar clip
        g2.setClip(area);
        
        g2.dispose();
        super.paint(g);
        
        // Dibujar borde
        Graphics2D g2Border = (Graphics2D) g.create();
        g2Border.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2Border.setStroke(new BasicStroke(0.5f));
        g2Border.setColor(borderColor);
        g2Border.drawRoundRect((int)x, (int)y, (int)areaWidth, (int)areaHeight, round, round);
        g2Border.dispose();
    }
    
    private void createImageShadow() {
        int height = getHeight();
        int width = getWidth();
        
        if (width > 0 && height > 0) {
            imageShadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = imageShadow.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int shadowWidth = width - (shadowSize.left + shadowSize.right);
            int shadowHeight = height - (shadowSize.top + shadowSize.bottom);
            
            if (shadowWidth > 0 && shadowHeight > 0) {
                // Crear sombra más suave
                for (int i = 5; i > 0; i--) {
                    float alpha = 0.1f * (6 - i);
                    g2.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
                    g2.fillRoundRect(
                        shadowSize.left + i,
                        shadowSize.top + i,
                        shadowWidth - i * 2,
                        shadowHeight - i * 2,
                        round,
                        round
                    );
                }
            }
            
            g2.dispose();
        }
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        createImageShadow();
    }
    
    // ==================== GETTERS Y SETTERS ====================
    
    public int getRound() {
        return round;
    }
    
    public void setRound(int round) {
        this.round = round;
        createImageShadow();
        repaint();
    }
    
    public Color getShadowColor() {
        return shadowColor;
    }
    
    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
        createImageShadow();
        repaint();
    }
    
    public Color getBorderColor() {
        return borderColor;
    }
    
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }
    
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (getViewport() != null) {
            getViewport().setBackground(bg);
        }
    }
}
