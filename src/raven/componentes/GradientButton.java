package raven.componentes;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Botón con gradiente configurable, sombra y efecto ripple.
 * Basado en la estructura profesional de Material Design.
 * 
 * Ejemplo de uso:
 * GradientButton btn = new GradientButton("Cerrar Caja");
 * btn.setGradientColors(new Color(0xf59e0b), new Color(0xd97706));
 * btn.setAngulo(135);  // Diagonal
 * btn.setRound(20);
 * 
 * @author Desarrollador
 * @version 2.1 - CORREGIDO
 */
public class GradientButton extends JButton {
    
    // ========== PROPIEDADES DEL GRADIENTE ==========
    private Color colorInicio;
    private Color colorFin;
    private double angulo = 135; // Ángulo del gradiente en grados
    
    // ========== PROPIEDADES DE HOVER ==========
    private Color colorInicioHover;
    private Color colorFinHover;
    private boolean enHover = false;
    
    // ========== PROPIEDADES DE SOMBRA ==========
    private int round = 20;
    private Color shadowColor = new Color(0, 0, 0, 100);
    private BufferedImage imageShadow;
    private final Insets shadowSize = new Insets(3, 5, 8, 5);
    
    // ========== EFECTO RIPPLE ==========
    private RippleEffect rippleEffect;
    
    // ========== CONSTANTES ==========
    private static final float BRILLO_HOVER = 1.20f;
    
    /**
     * Constructor por defecto
     */
    public GradientButton() {
        this("Botón");
    }
    
    /**
     * Constructor con texto
     */
    public GradientButton(String texto) {
        super(texto);
        // Colores por defecto: gradiente azul
        this.colorInicio = new Color(59, 130, 246);   // Azul claro
        this.colorFin = new Color(37, 99, 235);       // Azul oscuro
        this.colorInicioHover = hacerMasBrillante(colorInicio, BRILLO_HOVER);
        this.colorFinHover = hacerMasBrillante(colorFin, BRILLO_HOVER);
        
        rippleEffect = new RippleEffect(this);
        configurarEstilo();
    }
    
    /**
     * Constructor con texto y colores
     */
    public GradientButton(String texto, Color colorInicio, Color colorFin) {
        super(texto);
        this.colorInicio = colorInicio;
        this.colorFin = colorFin;
        this.colorInicioHover = hacerMasBrillante(colorInicio, BRILLO_HOVER);
        this.colorFinHover = hacerMasBrillante(colorFin, BRILLO_HOVER);
        
        rippleEffect = new RippleEffect(this);
        configurarEstilo();
    }
    
    /**
     * Constructor con texto, colores, ángulo y radio
     */
    public GradientButton(String texto, Color colorInicio, Color colorFin, double angulo, int round) {
        super(texto);
        this.colorInicio = colorInicio;
        this.colorFin = colorFin;
        this.angulo = angulo;
        this.round = round;
        this.colorInicioHover = hacerMasBrillante(colorInicio, BRILLO_HOVER);
        this.colorFinHover = hacerMasBrillante(colorFin, BRILLO_HOVER);
        
        rippleEffect = new RippleEffect(this);
        configurarEstilo();
    }
    
    /**
     * Constructor con colores hexadecimales
     */
    public GradientButton(String texto, String colorInicioHex, String colorFinHex) {
        this(texto, Color.decode(colorInicioHex), Color.decode(colorFinHex));
    }
    
    /**
     * Configura los estilos básicos del botón
     */
    private void configurarEstilo() {
        setBorder(new EmptyBorder(12, 16, 14, 16));
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        
        // Cursor de mano
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Fuente - NEGRITA para mejor legibilidad
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setFocusPainted(false);
        
        // Crear sombra inicial
        createImageShadow();
        
        // Efectos de hover y click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                enHover = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                enHover = false;
                repaint();
            }
        });
    }
    
    /**
     * Pinta el componente con gradiente y sombra
     */
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int width = getWidth();
        int height = getHeight();
        
        // ========== DIBUJAR SOMBRA ==========
        if (imageShadow != null) {
            g2.drawImage(imageShadow, 0, 0, null);
        }
        
        // ========== CALCULAR ÁREA ÚTIL ==========
        double areaWidth = width - (shadowSize.left + shadowSize.right);
        double areaHeight = height - (shadowSize.top + shadowSize.bottom);
        double x = shadowSize.left;
        double y = shadowSize.top;
        
        // ========== CREAR FORMA REDONDEADA ==========
        RoundRectangle2D.Double forma = new RoundRectangle2D.Double(x, y, areaWidth, areaHeight, round, round);
        Area area = new Area(forma);
        
        // ========== SELECCIONAR COLORES ==========
        Color inicio = enHover ? colorInicioHover : colorInicio;
        Color fin = enHover ? colorFinHover : colorFin;
        
        // ========== CREAR Y APLICAR GRADIENTE ==========
        Paint gradiente = crearGradiente((int)areaWidth, (int)areaHeight, inicio, fin, angulo);
        g2.setPaint(gradiente);
        g2.fill(area);
        
        // ========== BORDE SUTIL ==========
        g2.setStroke(new BasicStroke(0.5f));
        g2.setColor(new Color(255, 255, 255, 30));
        g2.draw(area);
        
        // ========== EFECTO RIPPLE ==========
        if (rippleEffect != null) {
            rippleEffect.render(g2, area);
        }
        
        g2.dispose();
        super.paintComponent(grphcs);
    }
    
    /**
     * Crea el gradiente según el ángulo especificado
     */
    private Paint crearGradiente(int ancho, int alto, Color colorInicio, Color colorFin, double angulo) {
        // Convertir ángulo a radianes
        double radianes = Math.toRadians(angulo);
        
        // Calcular distancia diagonal
        double longitud = Math.sqrt(ancho * ancho + alto * alto);
        
        // Calcular puntos del gradiente desde el centro
        float x1 = (float) (ancho / 2.0 - (longitud / 2.0) * Math.cos(radianes));
        float y1 = (float) (alto / 2.0 - (longitud / 2.0) * Math.sin(radianes));
        
        float x2 = (float) (ancho / 2.0 + (longitud / 2.0) * Math.cos(radianes));
        float y2 = (float) (alto / 2.0 + (longitud / 2.0) * Math.sin(radianes));
        
        return new GradientPaint(x1, y1, colorInicio, x2, y2, colorFin, true);
    }
    
    /**
     * Crea la sombra del botón con efecto gaussiano
     */
    private void createImageShadow() {
        int height = getHeight();
        int width = getWidth();
        
        if (width > 0 && height > 0) {
            imageShadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = imageShadow.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // ========== CREAR FORMA DE SOMBRA ==========
            int shadowWidth = width - (shadowSize.left + shadowSize.right);
            int shadowHeight = height - (shadowSize.top + shadowSize.bottom);
            
            if (shadowWidth > 0 && shadowHeight > 0) {
                // Crear forma con sombra
                RoundRectangle2D.Double forma = new RoundRectangle2D.Double(
                    shadowSize.left + 1,
                    shadowSize.top + 1,
                    shadowWidth - 2,
                    shadowHeight - 2,
                    round,
                    round
                );
                
                g2.setColor(shadowColor);
                g2.fill(forma);
                
                // Aplicar efecto de desenfoque
                applyGaussianBlur(imageShadow, 5);
            }
            
            g2.dispose();
        }
    }
    
    /**
     * Aplica desenfoque gaussiano simple a la imagen
     */
    private void applyGaussianBlur(BufferedImage img, int radius) {
        // Desenfoque horizontal y vertical
        int[] kernel = createGaussianKernel(radius);
        
        int width = img.getWidth();
        int height = img.getHeight();
        
        // Aplicar desenfoque horizontal
        BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = getBlurPixel(img, x, y, kernel, true);
                temp.setRGB(x, y, color);
            }
        }
        
        // Aplicar desenfoque vertical
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = getBlurPixel(temp, x, y, kernel, false);
                img.setRGB(x, y, color);
            }
        }
    }
    
    /**
     * Obtiene el color desenfocado de un píxel
     */
    private int getBlurPixel(BufferedImage img, int x, int y, int[] kernel, boolean horizontal) {
        int r = 0, g = 0, b = 0, a = 0;
        int kernelSize = kernel.length;
        int offset = kernelSize / 2;
        
        for (int i = 0; i < kernelSize; i++) {
            int xx = horizontal ? x + i - offset : x;
            int yy = horizontal ? y : y + i - offset;
            
            // Asegurar que está dentro de los límites
            xx = Math.max(0, Math.min(xx, img.getWidth() - 1));
            yy = Math.max(0, Math.min(yy, img.getHeight() - 1));
            
            int rgb = img.getRGB(xx, yy);
            int weight = kernel[i];
            
            a += ((rgb >> 24) & 0xFF) * weight;
            r += ((rgb >> 16) & 0xFF) * weight;
            g += ((rgb >> 8) & 0xFF) * weight;
            b += (rgb & 0xFF) * weight;
        }
        
        int totalWeight = 256;
        return ((a / totalWeight) << 24) | ((r / totalWeight) << 16) | ((g / totalWeight) << 8) | (b / totalWeight);
    }
    
    /**
     * Crea kernel gaussiano
     */
    private int[] createGaussianKernel(int radius) {
        int size = radius * 2 + 1;
        int[] kernel = new int[size];
        
        double sigma = radius / 2.0;
        double sum = 0;
        
        for (int i = 0; i < size; i++) {
            int x = i - radius;
            double value = Math.exp(-(x * x) / (2 * sigma * sigma)) / (Math.sqrt(2 * Math.PI) * sigma);
            kernel[i] = (int) (value * 256);
            sum += kernel[i];
        }
        
        // Normalizar kernel
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = (int) (kernel[i] * 256 / sum);
        }
        
        return kernel;
    }
    
    /**
     * Aumenta el brillo de un color
     */
    private Color hacerMasBrillante(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() * factor));
        int g = Math.min(255, (int) (color.getGreen() * factor));
        int b = Math.min(255, (int) (color.getBlue() * factor));
        int a = color.getAlpha();
        return new Color(r, g, b, a);
    }
    
    /**
     * Oscurece un color
     */
    private Color hacerMasOscuro(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() / factor));
        int g = Math.max(0, (int) (color.getGreen() / factor));
        int b = Math.max(0, (int) (color.getBlue() / factor));
        int a = color.getAlpha();
        return new Color(r, g, b, a);
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        createImageShadow();
    }
    
    // ========== GETTERS Y SETTERS ==========
    
    public void setGradientColors(Color colorInicio, Color colorFin) {
        this.colorInicio = colorInicio;
        this.colorFin = colorFin;
        this.colorInicioHover = hacerMasBrillante(colorInicio, BRILLO_HOVER);
        this.colorFinHover = hacerMasBrillante(colorFin, BRILLO_HOVER);
        repaint();
    }
    
    public void setGradientColors(String colorInicioHex, String colorFinHex) {
        setGradientColors(Color.decode(colorInicioHex), Color.decode(colorFinHex));
    }
    
    public Color getColorInicio() {
        return colorInicio;
    }
    
    public void setColorInicio(Color colorInicio) {
        this.colorInicio = colorInicio;
        this.colorInicioHover = hacerMasBrillante(colorInicio, BRILLO_HOVER);
        repaint();
    }
    
    public Color getColorFin() {
        return colorFin;
    }
    
    public void setColorFin(Color colorFin) {
        this.colorFin = colorFin;
        this.colorFinHover = hacerMasBrillante(colorFin, BRILLO_HOVER);
        repaint();
    }
    
    public double getAngulo() {
        return angulo;
    }
    
    public void setAngulo(double angulo) {
        this.angulo = angulo;
        repaint();
    }
    
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
    
    public void setRippleColor(Color color) {
        if (rippleEffect != null) {
            rippleEffect.setRippleColor(color);
        }
    }
    
    public Color getRippleColor() {
        if (rippleEffect != null) {
            return rippleEffect.getRippleColor();
        }
        return Color.WHITE;
    }
    
    public Color getColorInicioHover() {
        return colorInicioHover;
    }
    
    public void setColorInicioHover(Color colorInicioHover) {
        this.colorInicioHover = colorInicioHover;
        repaint();
    }
    
    public Color getColorFinHover() {
        return colorFinHover;
    }
    
    public void setColorFinHover(Color colorFinHover) {
        this.colorFinHover = colorFinHover;
        repaint();
    }
}