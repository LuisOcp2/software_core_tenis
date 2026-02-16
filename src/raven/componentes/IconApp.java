package raven.componentes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

/**
 * A custom button component with FontAwesome icons integration.
 * This component can be dragged and dropped in NetBeans Swing designer.
 * 
 * @author Raven
 */
public class IconApp extends JButton {
    
    static {
        // Register FontAwesome icon font when class is loaded
        try {
            IconFontSwing.register(FontAwesome.getIconFont());
        } catch (Exception e) {
            System.err.println("Failed to register FontAwesome: " + e.getMessage());
        }
    }
    
    private FontAwesome iconType = FontAwesome.HOME;
    private int iconSize = 16;
    private Color iconColor = Color.WHITE;
    private Color buttonBackground = new Color(52, 152, 219);
    
    /**
     * Creates a new JIconButton
     */
    public IconApp() {
        updateIcon();
        setFocusPainted(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(130, 40));
        
        // Add action listener
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            //    JOptionPane.showMessageDialog(null, "You clicked: " + getText(), "Button Clicked", JOptionPane.INFORMATION_MESSAGE);
            }
        });
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
        updateIcon();
    }
    
    /**
     * Gets the button background color
     * @return the button background color
     */
    @Override
    public Color getBackground() {
        return buttonBackground;
    }
    
    /**
     * Sets the button background color
     * @param buttonBackground the button background color to set
     */
    @Override
    public void setBackground(Color buttonBackground) {
        this.buttonBackground = buttonBackground;
        super.setBackground(buttonBackground);
    }
    
    /**
     * Updates the button icon based on current properties
     */
    private void updateIcon() {
        try {
            Icon iconObj = IconFontSwing.buildIcon(iconType, iconSize, iconColor);
            setIcon(iconObj);
            setForeground(iconColor);
            super.setBackground(buttonBackground);
        } catch (Exception e) {
            System.err.println("Failed to update icon: " + e.getMessage());
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