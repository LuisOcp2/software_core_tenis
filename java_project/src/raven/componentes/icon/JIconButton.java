package raven.componentes.icon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorSupport;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

/**
 * A custom button component with FontAwesome icons integration.
 * This component can be dragged and dropped in NetBeans Swing designer
 * and supports FlatLaf UIManager properties.
 */
public class JIconButton extends JButton {
    
    // Constante con el nombre de la propiedad de UIManager para el color de icono
    private static final String ICON_COLOR_KEY = "JIconButton.iconColor";
    
    static {
        // Register FontAwesome icon font when class is loaded
        try {
            IconFontSwing.register(FontAwesome.getIconFont());
        } catch (Exception e) {
        }
        
        // Registrar colores predeterminados en UIManager si no existen
        if (UIManager.getColor(ICON_COLOR_KEY) == null) {
            UIManager.put(ICON_COLOR_KEY, Color.WHITE);
        }
    }
    
    private FontAwesome iconType = FontAwesome.HOME;
    private int iconSize = 16;
    private Color iconColor = null; // Se usará el color de UIManager si es null
    private String iconColorKey = ICON_COLOR_KEY; // Clave UIManager para el color
    private boolean useUIManagerColors = true;
    
    /**
     * Creates a new JIconButton
     */
    public JIconButton() {
        updateIcon();
        setFocusPainted(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(130, 40));
        
        // Add action listener
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
             //   JOptionPane.showMessageDialog(null, "You clicked: " + getText(), "Button Clicked", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Activar antialiasing para mejorar la calidad de renderizado
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        // Pintar fondo si es necesario
        if (isContentAreaFilled()) {
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        
        g2.dispose();
        
        // Llamar al método original para pintar el contenido
        super.paintComponent(g);
    }
    
    /**
     * Gets the icon type
     * @return the FontAwesome icon type
     */
    public FontAwesome getIconType() {
        return iconType;
    }
    
    /**
     * Sets the icon type
     * @param iconType the FontAwesome icon type to set
     */
    public void setIconType(FontAwesome iconType) {
        this.iconType = iconType;
        updateIcon();
    }
    
    /**
     * Gets the icon size
     * @return the icon size in pixels
     */
    public int getIconSize() {
        return iconSize;
    }
    
    /**
     * Sets the icon size
     * @param iconSize the icon size in pixels
     */
    public void setIconSize(int iconSize) {
        this.iconSize = iconSize;
        updateIcon();
    }
    
    /**
     * Gets the icon color
     * @return the icon color
     */
    public Color getIconColor() {
        return iconColor;
    }
    
    /**
     * Sets the icon color
     * @param iconColor the icon color to set
     */
    public void setIconColor(Color iconColor) {
        this.iconColor = iconColor;
        useUIManagerColors = (iconColor == null);
        updateIcon();
    }
    
    /**
     * Gets the UIManager key for icon color
     * @return the UIManager key for icon color
     */
    public String getIconColorKey() {
        return iconColorKey;
    }
    
    /**
     * Sets the UIManager key for icon color
     * @param iconColorKey the UIManager key to use for icon color
     */
    public void setIconColorKey(String iconColorKey) {
        this.iconColorKey = iconColorKey;
        if (useUIManagerColors) {
            updateIcon();
        }
    }
    
    /**
     * Sets whether to use UIManager colors
     * @param use true to use UIManager colors, false to use the specific color
     */
    public void setUseUIManagerColors(boolean use) {
        this.useUIManagerColors = use;
        updateIcon();
    }
    
    /**
     * Gets whether UIManager colors are being used
     * @return true if using UIManager colors
     */
    public boolean isUseUIManagerColors() {
        return useUIManagerColors;
    }
    
    /**
     * Updates the button icon based on current properties
     */
    private void updateIcon() {
        try {
            // Determinar qué color usar
            Color color = getEffectiveIconColor();
            
            // Crear el icono con el color apropiado - usar un tamaño ligeramente mayor para mejor nitidez
            int adjustedSize = (int)(iconSize * 1.2); // Aumentar un 20% para mejor claridad
            Icon iconObj = IconFontSwing.buildIcon(iconType, adjustedSize, color);
            setIcon(iconObj);
            
            // Si estamos usando colores de UIManager, también actualizar cuando cambie el L&F
            if (useUIManagerColors) {
                UIManager.addPropertyChangeListener(evt -> {
                    if ("lookAndFeel".equals(evt.getPropertyName())) {
                        updateIcon();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to update icon: " + e.getMessage());
        }
    }
    
    /**
     * Gets the effective color to use for the icon
     * @return the color to use
     */
    private Color getEffectiveIconColor() {
        if (useUIManagerColors) {
            // Intentar obtener color de UIManager usando la clave especificada
            Color uiColor = UIManager.getColor(iconColorKey);
            
            // Si no se encuentra, intentar con la clave predeterminada
            if (uiColor == null && !ICON_COLOR_KEY.equals(iconColorKey)) {
                uiColor = UIManager.getColor(ICON_COLOR_KEY);
            }
            
            // Si todavía es null, usar color predeterminado
            return uiColor != null ? uiColor : Color.WHITE;
        } else {
            // Usar el color específico establecido, o blanco como fallback
            return iconColor != null ? iconColor : Color.WHITE;
        }
    }
    
    /**
     * Property editor for FontAwesome enum type
     */
    public static class FontAwesomePropertyEditor extends PropertyEditorSupport {
        private final String[] tags;
        
        public FontAwesomePropertyEditor() {
            FontAwesome[] values = FontAwesome.values();
            tags = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                tags[i] = values[i].name();
            }
        }
        
        @Override
        public String[] getTags() {
            return tags;
        }
        
        @Override
        public String getJavaInitializationString() {
            FontAwesome value = (FontAwesome) getValue();
            return "jiconfont.icons.font_awesome.FontAwesome." + value.name();
        }
        
        @Override
        public String getAsText() {
            FontAwesome value = (FontAwesome) getValue();
            return value.name();
        }
        
        @Override
        public void setAsText(String text) {
            for (FontAwesome value : FontAwesome.values()) {
                if (value.name().equals(text)) {
                    setValue(value);
                    break;
                }
            }
        }
    }
}