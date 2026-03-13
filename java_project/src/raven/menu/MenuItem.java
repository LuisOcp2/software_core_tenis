package raven.menu;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author Raven
 */
public class MenuItem extends JPanel {

    public boolean isMenuShow() {
        return menuShow;
    }

    public void setMenuShow(boolean menuShow) {
        this.menuShow = menuShow;
    }

    public float getAnimate() {
        return animate;
    }

    public void setAnimate(float animate) {
        this.animate = animate;
    }

    public String[] getMenus() {
        return menus;
    }

    public int getMenuIndex() {
        return menuIndex;
    }

    private final List<MenuEvent> events;
    private final Menu menu;
    private final String menus[];
    private final int menuIndex;
    private final int menuItemHeight = 38;
    private final int subMenuItemHeight = 35;
    private final int subMenuLeftGap = 34;
    private final int firstGap = 5;
    private final int bottomGap = 5;
    private boolean menuShow;
    private float animate;

    private PopupSubmenu popup;
    // Estado para ítem Notificaciones
    private boolean isNotificacionesItem;
    private FontIcon bellIcon;
    private javax.swing.Timer bellPulseTimer;
    private double bellPhase;

    /**
     * Establece la visibilidad de un submenú específico
     *
     * @param subMenuName Nombre del submenú a modificar
     * @param visible     true para mostrar, false para ocultar
     */
    public void setSubMenuVisible(String subMenuName, boolean visible) {
        for (Component comp : getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getText().equalsIgnoreCase(subMenuName)) {
                    btn.setVisible(visible);
                    break;
                }
            }
        }
        revalidate();
        repaint();
    }

    /**
     * Establece la visibilidad de este ítem de menú
     *
     * @param visible true para mostrar, false para ocultar
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && menuShow) {
            try {
                MenuAnimation.animate(this, true);
            } catch (Throwable err) {
                setAnimate(1f);
                revalidate();
                repaint();
            }
        }
    }

    public MenuItem(Menu menu, String menus[], int menuIndex, List<MenuEvent> events) {
        this.menu = menu;
        this.menus = menus;
        this.menuIndex = menuIndex;
        this.events = events;
        this.isNotificacionesItem = menus.length > 0 && "notificaciones".equalsIgnoreCase(menus[0]);
        if (isNotificacionesItem) {
            bellPhase = 0d;
            bellPulseTimer = new javax.swing.Timer(16, e -> {
                bellPhase += 0.08;
                if (bellPhase > Math.PI * 2)
                    bellPhase -= Math.PI * 2;
                float pulse = (float) ((Math.sin(bellPhase) + 1) / 2);
                if (bellIcon != null) {
                    // Oscilar entre naranja y amarillo
                    int r = 255;
                    int g = (int) (120 + 80 * pulse); // 120..200
                    int b = 0;
                    bellIcon.setIconColor(new Color(r, g, b));
                }
            });
        }
        init();
    }

    private Icon getIcon() {
        Color iconColor = Color.decode("#969696");
        // Mapear por nombre usando FontAwesome para iconos más descriptivos
        String name = menus[0].toLowerCase();
        switch (name) {
            case "notificaciones":
                // Campana para notificaciones (con animación de pulso)
                bellIcon = FontIcon.of(FontAwesomeSolid.BELL, 18, iconColor);
                return bellIcon;
            case "dashboard":
                // Gráfico de líneas para estadísticas/dashboard
                return FontIcon.of(FontAwesomeSolid.CHART_LINE, 18, iconColor);
            case "generar venta":
                // Caja registradora para ventas
                return FontIcon.of(FontAwesomeSolid.CASH_REGISTER, 18, iconColor);
            case "comercial":
                // Maletín para área comercial/negocios
                return FontIcon.of(FontAwesomeSolid.BRIEFCASE, 18, iconColor);
            case "productos":
                // Cajas para productos/inventario
                return FontIcon.of(FontAwesomeSolid.BOXES, 18, iconColor);
            case "reportes":
                // Documento para reportes
                return FontIcon.of(FontAwesomeSolid.FILE_ALT, 18, iconColor);
            case "admin":
                // Engranajes para administración/configuración
                return FontIcon.of(FontAwesomeSolid.COGS, 18, iconColor);
            case "cerrar sesión":
                // Icono de cerrar sesión/salida
                return FontIcon.of(FontAwesomeSolid.SIGN_OUT_ALT, 18, iconColor);
            default:
                // Fallback: icono de círculo para menús sin mapeo
                return FontIcon.of(FontAwesomeSolid.CIRCLE, 18, iconColor);
        }
    }

    /**
     * Obtiene un icono para los submenús basado en su nombre
     */
    private Icon getSubMenuIcon(String subMenuName) {
        Color iconColor = Color.decode("#969696");
        String name = subMenuName.toLowerCase().trim();
        switch (name) {
            // Submenús de Comercial
            case "clientes":
                return FontIcon.of(FontAwesomeSolid.USERS, 14, iconColor);
            case "proveedores":
                return FontIcon.of(FontAwesomeSolid.TRUCK, 14, iconColor);
            case "compras":
                return FontIcon.of(FontAwesomeSolid.SHOPPING_CART, 14, iconColor);
            case "reporte de ventas":
                return FontIcon.of(FontAwesomeSolid.CHART_BAR, 14, iconColor);
            case "devoluciones":
                return FontIcon.of(FontAwesomeSolid.UNDO_ALT, 14, iconColor);
            case "pedidos web":
                return FontIcon.of(FontAwesomeSolid.GLOBE, 14, iconColor);
            // Submenús de Productos
            case "ver productos":
                return FontIcon.of(FontAwesomeSolid.EYE, 14, iconColor);
            case "gestion de productos":
                return FontIcon.of(FontAwesomeSolid.EDIT, 14, iconColor);
            case "inventario":
                return FontIcon.of(FontAwesomeSolid.WAREHOUSE, 14, iconColor);
            case "marcas":
                return FontIcon.of(FontAwesomeSolid.TAG, 14, iconColor);
            case "categorias":
                return FontIcon.of(FontAwesomeSolid.FOLDER, 14, iconColor);
            case "movimientos":
                return FontIcon.of(FontAwesomeSolid.EXCHANGE_ALT, 14, iconColor);
            case "rotulación":
                return FontIcon.of(FontAwesomeSolid.BARCODE, 14, iconColor);
            case "traspasos":
                return FontIcon.of(FontAwesomeSolid.DOLLY, 14, iconColor);
            case "color":
                return FontIcon.of(FontAwesomeSolid.PALETTE, 14, iconColor);
            case "prestamos":
                return FontIcon.of(FontAwesomeSolid.HANDSHAKE, 14, iconColor);
            case "promociones":
                return FontIcon.of(FontAwesomeSolid.PERCENT, 14, iconColor);
            // Submenús de Reportes
            case "gastos":
                return FontIcon.of(FontAwesomeSolid.MONEY_BILL_WAVE, 14, iconColor);
            case "auditoría":
                return FontIcon.of(FontAwesomeSolid.CLIPBOARD_CHECK, 14, iconColor);
            // Submenús de Admin
            case "usuarios":
                return FontIcon.of(FontAwesomeSolid.USER_COG, 14, iconColor);
            case "bodegas":
                return FontIcon.of(FontAwesomeSolid.BUILDING, 14, iconColor);
            case "cajas":
                return FontIcon.of(FontAwesomeSolid.CASH_REGISTER, 14, iconColor);
            default:
                return FontIcon.of(FontAwesomeSolid.ANGLE_RIGHT, 14, iconColor);
        }
    }

    private void init() {
        setLayout(new MenuLayout());
        putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Menu.background;"
                + "foreground:$Menu.lineColor");
        for (int i = 0; i < menus.length; i++) {
            JButton menuItem = createButtonItem(menus[i]);
            menuItem.setHorizontalAlignment(
                    menuItem.getComponentOrientation().isLeftToRight() ? JButton.LEADING : JButton.TRAILING);
            if (i == 0) {
                menuItem.setIcon(getIcon());
                menuItem.addActionListener((ActionEvent e) -> {
                    if (menus.length > 1) {
                        if (menu.isMenuFull()) {
                            try {
                                MenuAnimation.animate(MenuItem.this, !menuShow);
                            } catch (Throwable err) {
                                menuShow = !menuShow;
                                revalidate();
                                repaint();
                            }
                        } else {
                            popup.show(MenuItem.this, (int) MenuItem.this.getWidth() + UIScale.scale(5),
                                    UIScale.scale(menuItemHeight) / 2);
                        }
                    } else {
                        menu.runEvent(menuIndex, 0);
                    }
                });
            } else {
                // Agregar icono al submenú
                menuItem.setIcon(getSubMenuIcon(menus[i]));
                final int subIndex = i;
                menuItem.addActionListener((ActionEvent e) -> {
                    menu.runEvent(menuIndex, subIndex);
                });
            }
            add(menuItem);
        }
        popup = new PopupSubmenu(getComponentOrientation(), menu, menuIndex, menus);
    }

    // Activar/Desactivar pulso para ícono de Notificaciones
    public void setNotificationActive(boolean active) {
        if (!isNotificacionesItem)
            return;
        if (active) {
            if (bellPulseTimer != null && !bellPulseTimer.isRunning())
                bellPulseTimer.start();
        } else {
            if (bellPulseTimer != null && bellPulseTimer.isRunning())
                bellPulseTimer.stop();
            if (bellIcon != null)
                bellIcon.setIconColor(Color.decode("#969696"));
        }
        repaint();
    }

    protected void setSelectedIndex(int index) {
        int size = getComponentCount();
        boolean selected = false;
        for (int i = 0; i < size; i++) {
            Component com = getComponent(i);
            if (com instanceof JButton) {
                ((JButton) com).setSelected(i == index);
                if (i == index) {
                    selected = true;
                }
            }
        }
        ((JButton) getComponent(0)).setSelected(selected);
        popup.setSelectedIndex(index);
    }

    private JButton createButtonItem(String text) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Menu.background;"
                + "foreground:$Menu.foreground;"
                + "selectedBackground:$Menu.button.selectedBackground;"
                + "selectedForeground:$Menu.button.selectedForeground;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0;"
                + "arc:10;"
                + "iconTextGap:10;"
                + "margin:3,11,3,11");
        return button;
    }

    public void hideMenuItem() {
        animate = 0;
        menuShow = false;
    }

    public void setFull(boolean full) {
        if (full) {
            int size = getComponentCount();
            for (int i = 0; i < size; i++) {
                Component com = getComponent(i);
                if (com instanceof JButton) {
                    JButton button = (JButton) com;
                    button.setText(menus[i]);
                    button.setHorizontalAlignment(
                            getComponentOrientation().isLeftToRight() ? JButton.LEFT : JButton.RIGHT);
                }
            }
        } else {
            for (Component com : getComponents()) {
                if (com instanceof JButton) {
                    JButton button = (JButton) com;
                    button.setText("");
                    button.setHorizontalAlignment(JButton.CENTER);
                }
            }
            animate = 0f;
            menuShow = false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (animate > 0) {
            int ssubMenuItemHeight = UIScale.scale(subMenuItemHeight);
            int ssubMenuLeftGap = UIScale.scale(subMenuLeftGap);
            int smenuItemHeight = UIScale.scale(menuItemHeight);
            int sfirstGap = UIScale.scale(firstGap);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D.Double p = new Path2D.Double();
            int last = getComponent(getComponentCount() - 1).getY() + (ssubMenuItemHeight / 2);
            boolean ltr = getComponentOrientation().isLeftToRight();
            int round = UIScale.scale(10);
            int x = ltr ? (ssubMenuLeftGap - round) : (getWidth() - (ssubMenuLeftGap - round));
            p.moveTo(x, smenuItemHeight + sfirstGap);
            p.lineTo(x, last - round);
            for (int i = 1; i < getComponentCount(); i++) {
                int com = getComponent(i).getY() + (ssubMenuItemHeight / 2);
                p.append(createCurve(round, x, com, ltr), false);
            }
            g2.setColor(getForeground());
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setStroke(new BasicStroke(UIScale.scale(1f)));
            g2.draw(p);
            g2.dispose();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (menus.length > 1) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(FlatUIUtils.getUIColor("Menu.arrowColor", getForeground()));
            int smenuItemHeight = UIScale.scale(menuItemHeight);
            boolean ltr = getComponentOrientation().isLeftToRight();
            g2.setStroke(new BasicStroke(UIScale.scale(1f)));
            if (menu.isMenuFull()) {
                int arrowWidth = UIScale.scale(10);
                int arrowHeight = UIScale.scale(5);
                int ax = ltr ? (getWidth() - arrowWidth * 2) : arrowWidth;
                int ay = (smenuItemHeight - arrowHeight) / 2;
                Path2D p = new Path2D.Double();
                p.moveTo(0, animate * arrowHeight);
                p.lineTo(arrowWidth / 2, (1f - animate) * arrowHeight);
                p.lineTo(arrowWidth, animate * arrowHeight);
                g2.translate(ax, ay);
                g2.draw(p);
            } else {
                int arrowWidth = UIScale.scale(4);
                int arrowHeight = UIScale.scale(8);
                int ax = ltr ? (getWidth() - arrowWidth - UIScale.scale(3)) : UIScale.scale(3);
                int ay = (smenuItemHeight - arrowHeight) / 2;
                Path2D p = new Path2D.Double();
                if (ltr) {
                    p.moveTo(0, 0);
                    p.lineTo(arrowWidth, arrowHeight / 2);
                    p.lineTo(0, arrowHeight);
                } else {
                    p.moveTo(arrowWidth, 0);
                    p.lineTo(0, arrowHeight / 2);
                    p.lineTo(arrowWidth, arrowHeight);
                }
                g2.translate(ax, ay);
                g2.draw(p);
            }
            g2.dispose();
        }
    }

    private Shape createCurve(int round, int x, int y, boolean ltr) {
        Path2D p2 = new Path2D.Double();
        p2.moveTo(x, y - round);
        p2.curveTo(x, y - round, x, y, x + (ltr ? round : -round), y);
        return p2;
    }

    private class MenuLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets inset = parent.getInsets();
                int width = parent.getWidth();
                int height = inset.top + inset.bottom;
                int size = parent.getComponentCount();
                Component item = parent.getComponent(0);
                height += UIScale.scale(menuItemHeight);
                if (item.isVisible()) {
                    int subMenuHeight = size > 1 ? UIScale.scale(firstGap) + UIScale.scale(bottomGap) : 0;
                    for (int i = 1; i < size; i++) {
                        Component com = parent.getComponent(i);
                        if (com.isVisible()) {
                            subMenuHeight += UIScale.scale(subMenuItemHeight);
                        }
                    }
                    height += (subMenuHeight * animate);
                } else {
                    height = 0;
                }
                return new Dimension(width, height);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(0, 0);
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                boolean ltr = parent.getComponentOrientation().isLeftToRight();
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int width = parent.getWidth() - (insets.left + insets.right);
                int size = parent.getComponentCount();
                for (int i = 0; i < size; i++) {
                    Component com = parent.getComponent(i);
                    if (com.isVisible()) {
                        if (i == 0) {
                            int smenuItemHeight = UIScale.scale(menuItemHeight);
                            int sfirstGap = UIScale.scale(firstGap);
                            com.setBounds(x, y, width, smenuItemHeight);
                            y += smenuItemHeight + sfirstGap;
                        } else {
                            int ssubMenuLeftGap = UIScale.scale(subMenuLeftGap);
                            int subMenuX = ltr ? ssubMenuLeftGap : 0;
                            int ssubMenuItemHeight = UIScale.scale(subMenuItemHeight);
                            com.setBounds(x + subMenuX, y, width - ssubMenuLeftGap, ssubMenuItemHeight);
                            y += ssubMenuItemHeight;
                        }
                    }
                }
            }
        }
    }
}
