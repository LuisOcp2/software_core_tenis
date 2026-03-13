package raven.componentes;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalTabbedPaneUI;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class panelDeslizante extends JTabbedPane {

    private Color selectedTabColor = new Color(57, 113, 255);
    private Color unselectedTabColor = new Color(100, 100, 100);
    private final Color separatorColor = new Color(240, 240, 240);
    private int iconSize = 16;
    private int tabAlignment = SwingConstants.CENTER; // Default is CENTER
    private boolean showIcons = true;
    
    // Almacén para los tipos de íconos FontAwesome
    private Map<Integer, FontAwesome> tabIconTypes = new HashMap<>();
    
    static {
        // Register FontAwesome if not already registered
        try {
            IconFontSwing.register(FontAwesome.getIconFont());
        } catch (Exception e) {
            System.err.println("Error registering FontAwesome: " + e.getMessage());
        }
    }
    
    public panelDeslizante() {
        setOpaque(true);
        setFocusable(false);
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Usar los colores predeterminados de UIManager al inicio
        updateColorsFromUIManager();
        
        // Importante: primero configurar propiedades, luego establecer UI
        setUI(new ModernTabbedUI());
        
        // Soporte para propiedades de FlatLaf - SOLO usar propiedades estándar
        addPropertyChangeListener(FlatClientProperties.STYLE, (e) -> {
            updateStyleProperties();
        });
    }
    
    /**
     * Actualiza los colores desde UIManager
     */
    private void updateColorsFromUIManager() {
        // Usar colores del tema para inicialización
        Color foreground = UIManager.getColor("TabbedPane.foreground");
        if (foreground != null) {
            setForeground(foreground);
            unselectedTabColor = foreground;
        }
        
        Color selected = UIManager.getColor("TabbedPane.selectedForeground");
        if (selected != null) {
            selectedTabColor = selected;
        }
        
        setBackground(UIManager.getColor("TabbedPane.background"));
    }
    
    /**
     * Actualiza las propiedades de estilo desde FlatLaf
     */
    private void updateStyleProperties() {
        try {
            // Solo usar propiedades estándar de FlatLaf
            Object foreground = getClientProperty("foreground");
            if (foreground instanceof Color) {
                setForeground((Color) foreground);
                unselectedTabColor = (Color) foreground;
            }
            
            // Usar una propiedad estándar para el color de selección
            Object accentColor = UIManager.getColor("Component.accentColor");
            if (accentColor instanceof Color) {
                selectedTabColor = (Color) accentColor;
            }
            
            // Para compatibilidad, intentar obtener el color del tema
            Color themeColor = UIManager.getColor("Table.selectedForeground");
            if (themeColor != null) {
                selectedTabColor = themeColor;
            }
            
            // Actualizar iconos y repintar
            updateAllIcons();
            repaint();
        } catch (Exception e) {
            System.err.println("Error al actualizar estilos: " + e.getMessage());
        }
    }
    
    /**
     * Add a tab with icon
     * @param title Tab title
     * @param component Tab content component
     * @param iconType FontAwesome icon type
     */
    public void addTab(String title, Component component, FontAwesome iconType) {
        // Create icon from FontAwesome
        Icon icon = null;
        if (showIcons && iconType != null) {
            try {
                icon = IconFontSwing.buildIcon(iconType, iconSize, unselectedTabColor);
            } catch (Exception e) {
                System.err.println("Error creating icon: " + e.getMessage());
            }
        }
        
        // Determine tab index before adding
        int tabIndex = getTabCount();
        
        // Add tab with icon
        super.addTab(title, icon, component);
        
        // Store the icon type for future reference
        if (iconType != null) {
            tabIconTypes.put(tabIndex, iconType);
        }
    }
    
    /**
     * Add a tab without icon
     */
    @Override
    public void addTab(String title, Component component) {
        addTab(title, component, null);
    }
    
    /**
     * Set the tab alignment
     * @param alignment SwingConstants.LEFT, SwingConstants.CENTER, or SwingConstants.RIGHT
     */
    public void setTabAlignment(int alignment) {
        this.tabAlignment = alignment;
        updateUI();
    }
    
    /**
     * Set icon size for all tabs
     * @param size Icon size in pixels
     */
    public void setIconSize(int size) {
        this.iconSize = size;
        updateAllIcons();
    }
    
    /**
     * Set if icons should be displayed
     * @param show True to show icons, false to hide
     */
    public void setShowIcons(boolean show) {
        this.showIcons = show;
        updateAllIcons();
    }
    
    /**
     * Update icons for all tabs
     */
    private void updateAllIcons() {
        for (int i = 0; i < getTabCount(); i++) {
            FontAwesome iconType = tabIconTypes.get(i);
            if (!showIcons) {
                setIconAt(i, null);
            } else if (iconType != null) {
                try {
                    Icon icon = IconFontSwing.buildIcon(iconType, iconSize, unselectedTabColor);
                    setIconAt(i, icon);
                } catch (Exception e) {
                    System.err.println("Error updating icon: " + e.getMessage());
                }
            }
        }
        repaint();
    }
    
    /**
     * Set icon for specific tab
     * @param index Tab index
     * @param iconType FontAwesome icon type
     */
    public void setTabIcon(int index, FontAwesome iconType) {
        if (index >= 0 && index < getTabCount()) {
            try {
                Icon icon = IconFontSwing.buildIcon(iconType, iconSize, unselectedTabColor);
                setIconAt(index, icon);
                tabIconTypes.put(index, iconType);
            } catch (Exception e) {
                System.err.println("Error setting tab icon: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get the FontAwesome icon type for a specific tab
     * @param index Tab index
     * @return FontAwesome icon type or null if not set
     */
    public FontAwesome getTabIconType(int index) {
        return tabIconTypes.get(index);
    }
    
    /**
     * Establecer color para pestañas seleccionadas
     * @param color Color para pestañas seleccionadas
     */
    public void setSelectedTabColor(Color color) {
        this.selectedTabColor = color;
        repaint();
    }
    
    /**
     * Establecer color para pestañas no seleccionadas
     * @param color Color para pestañas no seleccionadas
     */
    public void setUnselectedTabColor(Color color) {
        this.unselectedTabColor = color;
        updateAllIcons();
        repaint();
    }

    public class ModernTabbedUI extends MetalTabbedPaneUI {
        private Rectangle currentRectangle;
        private Animator animator;
        private TimingTarget target;
        private ChangeListener changeListener;

        public ModernTabbedUI() {
            animator = new Animator(300);
            animator.setResolution(0);
            animator.setAcceleration(.5f);
            animator.setDeceleration(.5f);
        }
        
        public void setCurrentRectangle(Rectangle currentRectangle) {
            this.currentRectangle = currentRectangle;
            repaint();
        }

        @Override
        public void installUI(JComponent jc) {
            super.installUI(jc);
            
            // Instalar el listener solo después de que la UI esté completamente instalada
            changeListener = new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent ce) {
                    JTabbedPane tp = (JTabbedPane) ce.getSource();
                    if (tp == null || tp.getTabCount() == 0) return;
                    
                    int selected = tp.getSelectedIndex();
                    if (selected == -1) return;
                    
                    try {
                        Rectangle tabBounds = getTabBounds(tp, selected);
                        if (tabBounds == null) return;
                        
                        if (currentRectangle != null && animator != null) {
                            if (animator.isRunning()) {
                                animator.stop();
                            }
                            
                            if (target != null) {
                                animator.removeTarget(target);
                            }
                            
                            target = new PropertySetter(ModernTabbedUI.this, "currentRectangle", currentRectangle, tabBounds);
                            animator.addTarget(target);
                            animator.start();
                        } else {
                            currentRectangle = tabBounds;
                            repaint();
                        }
                    } catch (Exception e) {
                        System.err.println("Error en animación de pestaña: " + e.getMessage());
                    }
                }
            };
            
            tabPane.addChangeListener(changeListener);
        }
        
        @Override
        public void uninstallUI(JComponent c) {
            if (changeListener != null && tabPane != null) {
                tabPane.removeChangeListener(changeListener);
                changeListener = null;
            }
            
            if (animator != null && animator.isRunning()) {
                animator.stop();
            }
            
            currentRectangle = null;
            super.uninstallUI(c);
        }
        
        /**
         * Método seguro para obtener los límites de una pestaña
         */
        public Rectangle getTabBounds(JTabbedPane pane, int index) {
            if (pane == null || index < 0 || index >= pane.getTabCount()) {
                return null;
            }
            
            try {
                Rectangle rect = new Rectangle();
                return getTabBounds(index, rect);
            } catch (Exception e) {
                System.err.println("Error al obtener límites de pestaña: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected Insets getTabInsets(int i, int i1) {
            return new Insets(12, 20, 12, 20);
        }
        
        @Override
        protected Insets getContentBorderInsets(int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }
        
        protected int getTabRunAlignment(int tabPlacement) {
            return tabAlignment; // Utiliza la alineación configurada
        }

        protected void calculateTabRects(int tabPlacement, int tabCount) {
            // No llamamos a super.calculateTabRects(), implementamos nuestra versión
            
            if (tabCount == 0) {
                return;
            }
            
            FontMetrics metrics = getFontMetrics();
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            int selectedIndex = tabPane.getSelectedIndex();
            int tabRunOverlay = getTabRunOverlay(tabPlacement);
            int i, j;
            
            int x = tabAreaInsets.left;
            int y = tabAreaInsets.top;
            int returnAt;
            
            switch (tabPlacement) {
                case LEFT:
                case RIGHT:
                    returnAt = y + tabCount * maxTabHeight;
                    break;
                case BOTTOM:
                case TOP:
                default:
                    returnAt = tabPane.getWidth() - (tabAreaInsets.right + tabAreaInsets.left);
                    break;
            }
            
            // Calcular la posición y tamaño de cada pestaña
            for (i = 0; i < tabCount; i++) {
                Rectangle rect = rects[i];
                
                if (i > 0) {
                    rect.x = rects[i-1].x + rects[i-1].width;
                } else {
                    rect.x = x;
                }
                
                rect.y = y;
                
                String title = tabPane.getTitleAt(i);
                Icon icon = getIconForTab(i);
                
                rect.width = calculateTabWidth(tabPlacement, i, metrics);
                rect.height = calculateTabHeight(tabPlacement, i, metrics.getHeight());
                
                if (rect.x + rect.width > returnAt) {
                    // Si se excede el espacio disponible, iniciar una nueva fila
                    rect.x = x;
                    rect.y = rects[i-1].y + rects[i-1].height;
                }
            }
            
            // Centrar las pestañas si la alineación es CENTER
            if (tabAlignment == SwingConstants.CENTER) {
                centerTabs(tabPlacement, tabCount);
            }
        }
        
        private void centerTabs(int tabPlacement, int tabCount) {
            if (tabCount <= 0 || tabPane == null) {
                return;
            }
            
            try {
                int lastTab = tabCount - 1;
                int totalTabWidth = rects[lastTab].x + rects[lastTab].width - rects[0].x;
                int containerWidth = tabPane.getWidth() - getTabAreaInsets(tabPlacement).left - getTabAreaInsets(tabPlacement).right;
                
                int offset = (containerWidth - totalTabWidth) / 2;
                
                if (offset > 0) {
                    for (int i = 0; i < tabCount; i++) {
                        rects[i].x += offset;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al centrar pestañas: " + e.getMessage());
            }
        }

        @Override
        protected void paintTabBorder(Graphics grphcs, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            if (grphcs == null) return;
            
            Graphics2D g2 = (Graphics2D) grphcs.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (currentRectangle == null || (animator != null && !animator.isRunning())) {
                if (isSelected) {
                    currentRectangle = new Rectangle(x, y, w, h);
                }
            }
            
            if (isSelected) {
                g2.setColor(selectedTabColor);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD));
            } else {
                g2.setColor(unselectedTabColor);
            }
            
            if (currentRectangle != null) {
                if (tabPlacement == TOP) {
                    g2.fillRect(currentRectangle.x, currentRectangle.y + currentRectangle.height - 3, currentRectangle.width, 3);
                }
            }
            
            g2.dispose();
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            if (g == null) return;
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Usa getBackground() que tomará el color de fondo configurado por FlatLaf
            g2.setColor(tabPane.getBackground());
            g2.fillRect(x, y, w, h);
            
            g2.dispose();
        }

        @Override
        protected void paintContentBorder(Graphics grphcs, int tabPlacement, int selectedIndex) {
            if (grphcs == null || tabPane == null) return;
            
            Graphics2D g2 = (Graphics2D) grphcs.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = tabPane.getWidth();
            
            // Draw bottom separator line
            g2.setColor(separatorColor);
            if (tabPlacement == TOP) {
                int tabHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g2.fillRect(0, tabHeight, width, 1);
            }
            
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics grphcs, int i, Rectangle[] rctngls, int i1, Rectangle rctngl, Rectangle rctngl1, boolean bln) {
            // No focus indicator
        }
        
        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, 
                FontMetrics metrics, int tabIndex, String title, Rectangle textRect, 
                boolean isSelected) {
            if (g == null || metrics == null || title == null || textRect == null) return;
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            if (isSelected) {
                g2.setColor(selectedTabColor);
                g2.setFont(font.deriveFont(Font.BOLD));
            } else {
                // Usar el color configurado para texto no seleccionado
                g2.setColor(unselectedTabColor);
                g2.setFont(font);
            }
            
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            g2.dispose();
        }
        
        @Override
        protected void paintIcon(Graphics g, int tabPlacement, int tabIndex, Icon icon, Rectangle iconRect, boolean isSelected) {
            if (g == null || icon == null || iconRect == null) return;
            
            if (showIcons) {
                // Si está seleccionado, trata de crear un nuevo icono con el color de selección
                if (isSelected) {
                    try {
                        // Obtener el tipo de FontAwesome almacenado
                        FontAwesome iconType = getTabIconType(tabIndex);
                        if (iconType != null) {
                            // Crear un nuevo icono con el color de selección
                            Icon selectedIcon = IconFontSwing.buildIcon(iconType, iconSize, selectedTabColor);
                            selectedIcon.paintIcon(tabPane, g, iconRect.x, iconRect.y);
                            return;
                        }
                    } catch (Exception e) {
                        // Si falla, caer al método predeterminado
                    }
                }
                
                // Pintar el icono original si no está seleccionado o si falló la creación del nuevo icono
                icon.paintIcon(tabPane, g, iconRect.x, iconRect.y);
            }
        }
        
        @Override
        protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight) + 8;
        }
        
        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            if (g == null || tabPane == null) return;
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(tabPane.getBackground());
            g2d.fillRect(0, 0, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight));
            g2d.dispose();
            
            super.paintTabArea(g, tabPlacement, selectedIndex);
        }
        
        @Override
        protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
            return 0;
        }
        
        @Override
        protected int getTabRunOverlay(int tabPlacement) {
            return 0;
        }
    }
}