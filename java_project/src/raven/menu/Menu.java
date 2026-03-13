package raven.menu;

import raven.menu.mode.LightDarkMode;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import raven.clases.admin.UserSession;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.AppConfig;
import raven.menu.mode.ToolBarAccentColor;
import raven.utils.NotificacionesService;
import raven.utils.EventosTraspasosService;
import raven.controlador.update.UpdateManager;

public class Menu extends JPanel {

    private static final String UI_PREFS_NODE = "raven/ui";
    private static final String UI_PREFS_CUSTOM_HEADER_ICON = "custom_header_icon_path";

    private final String menuItems[][] = {
            { "~MAIN~" }, // Título de sección
            { "Notificaciones" }, // Nuevo primer elemento
            { "Dashboard" }, // Segundo elemento
            { "~PRINCIPAL~" }, // Título de sección
            { "Generar venta" }, // Elemento con submenú
            { "~COMERCIAL~" },
            { "Comercial", "Clientes", "Proveedores", "Compras", "reporte de ventas", "Devoluciones", "Pedidos Web",
                    "Cambio Talla", "Cuentas por Cobrar" },
            { "~PRODUCTOS~" },
            { "Productos", "Ver productos", "Gestion de productos", "Inventario", "Marcas", "Categorias", "Movimientos",
                    "Rotulación", "traspasos", "color", "Prestamos", "Promociones", "Consulta Detallada" },
            { "~REPORTES~" },
            { "Reportes", "Inventario", "Compras", "Gastos", "Devoluciones", "Traspasos", "Clientes", "Auditoría" },
            { "~Admin~" },
            { "Admin", "Usuarios", "Bodegas", "Cajas", "Tipos de Gasto", "Gestión de Permisos" },
            { "Cerrar Sesión" }
    };

    public boolean isMenuFull() {
        return menuFull;
    }

    /**
     * Restablece el menú a su estado original (todos los elementos visibles)
     * Este método debe llamarse antes de configurar el menú por rol
     */
    public void resetMenu() {
        // Mostrar todos los componentes del menú
        for (Component com : panelMenu.getComponents()) {
            com.setVisible(true);

            if (com instanceof MenuItem) {
                MenuItem item = (MenuItem) com;
                // Mostrar todos los submenús
                for (Component subComp : item.getComponents()) {
                    subComp.setVisible(true);
                }
            }
        }
        revalidate();
        repaint();
    }

    public void setMenuFull(boolean menuFull) {
        this.menuFull = menuFull;
        if (menuFull) {
            header.setText(buildHeaderText());
            header.setHorizontalAlignment(getComponentOrientation().isLeftToRight() ? JLabel.LEFT : JLabel.RIGHT);
        } else {
            header.setText("");
            header.setHorizontalAlignment(JLabel.CENTER);
        }
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                ((MenuItem) com).setFull(menuFull);
            }
        }
        lightDarkMode.setMenuFull(menuFull);
        toolBarAccentColor.setMenuFull(menuFull);
    }

    private final List<MenuEvent> events = new ArrayList<>();
    private boolean menuFull = true;
    private final String headerName = "";

    protected final boolean hideMenuTitleOnMinimum = true;
    protected final int menuTitleLeftInset = 5;
    protected final int menuTitleVgap = 5;
    protected final int menuMaxWidth = 250;
    protected final int menuMinWidth = 60;
    protected final int headerFullHgap = 5;
    private Timer notificationTimer;
    private NotificacionesService notificacionesService;
    private MenuItem notificacionesItem;
    private volatile boolean notifWorkerRunning = false;

    public Menu() {
        init();
    }

    private void init() {
        setLayout(new MenuLayout());
        putClientProperty(FlatClientProperties.STYLE, ""
                + "border:20,2,2,2;"
                + "background:$Menu.background;"
                + "arc:10");
        Icon headerIcon = buildHeaderIcon();
        header = new JLabel(buildHeaderText());
        header.setIcon(headerIcon);
        header.setIconTextGap(8);
        header.setPreferredSize(new Dimension(menuMaxWidth, 80));
        header.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.header.font;"
                + "foreground:$Menu.foreground");
        header.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) {
                    return;
                }
                elegirIconoPersonalizado();
            }
        });

        // Menu
        scroll = new JScrollPane();
        panelMenu = new JPanel(new MenuItemLayout(this));
        panelMenu.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5;"
                + "background:$Menu.background");

        scroll.setViewportView(panelMenu);
        scroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:null");
        JScrollBar vscroll = scroll.getVerticalScrollBar();
        vscroll.setUnitIncrement(10);
        vscroll.putClientProperty(FlatClientProperties.STYLE, ""
                + "width:$Menu.scroll.width;"
                + "trackInsets:$Menu.scroll.trackInsets;"
                + "thumbInsets:$Menu.scroll.thumbInsets;"
                + "background:$Menu.ScrollBar.background;"
                + "thumb:$Menu.ScrollBar.thumb");
        createMenu();
        setUpdateItemVisible(UpdateManager.hasPreparedUpdate());
        lightDarkMode = new LightDarkMode();
        toolBarAccentColor = new ToolBarAccentColor(this);
        toolBarAccentColor.setVisible(FlatUIUtils.getUIBoolean("AccentControl.show", false));
        add(header);

        add(scroll);
        add(lightDarkMode);
        add(toolBarAccentColor);

        // Servicio y timer para verificar eventos (pedidos web / traspasos)
        notificacionesService = new NotificacionesService();
        // Iniciar verificador automático (baja frecuencia) para registro en BD
        try {
            raven.utils.VerificadorAutomatico.getInstance().iniciar();
        } catch (Throwable ignore) {
        }

        notificationTimer = new Timer(10000, e -> {
            if (notifWorkerRunning) {
                return;
            }
            notifWorkerRunning = true;
            new javax.swing.SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        Integer bodegaId = UserSession.getInstance().getIdBodegaUsuario();
                        ModelUser user = UserSession.getInstance().getCurrentUser();
                        if (bodegaId != null && bodegaId > 0 && user != null) {
                            return notificacionesService.hayEventosPedidosWebOTRaspasosOptimizadoPorBodega(bodegaId,
                                    user.getIdUsuario());
                        }
                        return notificacionesService.hayEventosPedidosWebOTRaspasosOptimizado();
                    } catch (Exception ex) {
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        boolean active = get();
                        if (notificacionesItem != null) {
                            notificacionesItem.setNotificationActive(active);
                        }
                        notifWorkerRunning = false;
                    } catch (Exception ignore) {
                        if (notificacionesItem != null) {
                            notificacionesItem.setNotificationActive(false);
                        }
                        notifWorkerRunning = false;
                    }
                }
            }.execute();
        });
        notificationTimer.setRepeats(true);
        notificationTimer.start();

        new javax.swing.SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    Integer bodegaId = UserSession.getInstance().getIdBodegaUsuario();
                    ModelUser user = UserSession.getInstance().getCurrentUser();
                    if (bodegaId != null && bodegaId > 0 && user != null) {
                        return notificacionesService.hayEventosPedidosWebOTRaspasosOptimizadoPorBodega(bodegaId,
                                user.getIdUsuario());
                    }
                    return notificacionesService.hayEventosPedidosWebOTRaspasosOptimizado();
                } catch (Exception ex) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean active = get();
                    if (notificacionesItem != null) {
                        notificacionesItem.setNotificationActive(active);
                    }
                } catch (Exception ignore) {
                    if (notificacionesItem != null) {
                        notificacionesItem.setNotificationActive(false);
                    }
                }
            }
        }.execute();
    }

    private String buildHeaderText() {
        ModelUser u = UserSession.getInstance().getCurrentUser();
        if (u != null) {
            String nombre = u.getNombre();
            if (nombre != null && !nombre.isEmpty())
                return nombre;
            String username = u.getUsername();
            if (username != null && !username.isEmpty())
                return username;
        }
        return "";
    }

    private Icon buildHeaderIcon() {
        final int size = 72;
        try {
            String customPath = Preferences.userRoot().node(UI_PREFS_NODE).get(UI_PREFS_CUSTOM_HEADER_ICON, "");
            if (customPath != null && !customPath.isBlank()) {
                File f = new File(customPath.trim());
                if (f.exists() && f.isFile()) {
                    String p = f.getName().toLowerCase();
                    if (p.endsWith(".svg")) {
                        FlatSVGIcon svg = new FlatSVGIcon(f.toURI().toURL());
                        return svg.derive(size, size);
                    } else {
                        java.awt.image.BufferedImage img = ImageIO.read(f);
                        if (img != null) {
                            java.awt.Image scaled = img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
                            return new javax.swing.ImageIcon(scaled);
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        try {
            java.net.URL res = getClass().getResource(AppConfig.logo);
            if (res != null) {
                String p = res.toString().toLowerCase();
                if (p.endsWith(".svg")) {
                    FlatSVGIcon svg = new FlatSVGIcon(res);
                    return svg.derive(size, size);
                } else {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(res);
                    if (img != null) {
                        java.awt.Image scaled = img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
                        return new javax.swing.ImageIcon(scaled);
                    }
                }
            }
            java.net.URL pngUrl = getClass().getResource("/raven/icon/xtreme.png");
            if (pngUrl != null) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(pngUrl);
                if (img != null) {
                    java.awt.Image scaled = img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
                    return new javax.swing.ImageIcon(scaled);
                }
            }
            FlatSVGIcon fallback = new FlatSVGIcon(getClass().getResource("/raven/icon/svg/xtreme.svg"));
            return fallback.derive(size, size);
        } catch (Exception e) {
            FlatSVGIcon fallback = new FlatSVGIcon(getClass().getResource("/raven/icon/svg/xtreme.svg"));
            return fallback.derive(size, size);
        }
    }

    private void elegirIconoPersonalizado() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Elegir icono");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Imágenes (png, jpg, jpeg, gif, svg)", "png", "jpg", "jpeg", "gif", "svg"));
        int res = chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = chooser.getSelectedFile();
        if (f == null || !f.exists() || !f.isFile()) {
            return;
        }
        try {
            Preferences.userRoot().node(UI_PREFS_NODE).put(UI_PREFS_CUSTOM_HEADER_ICON, f.getAbsolutePath());
        } catch (Exception ignore) {
        }
        header.setIcon(buildHeaderIcon());
        revalidate();
        repaint();
    }

    private void createMenu() {
        int index = 0;
        for (int i = 0; i < menuItems.length; i++) {
            String menuName = menuItems[i][0];
            if (menuName.startsWith("~") && menuName.endsWith("~")) {
                panelMenu.add(createTitle(menuName));
            } else {
                MenuItem menuItem = new MenuItem(this, menuItems[i], index++, events);
                if ("Notificaciones".equalsIgnoreCase(menuName)) {
                    notificacionesItem = menuItem;
                }
                panelMenu.add(menuItem);
            }
        }
    }

    private JLabel createTitle(String title) {
        String menuName = title.substring(1, title.length() - 1);
        JLabel lbTitle = new JLabel(menuName);
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$Menu.label.font;"
                + "foreground:$Menu.title.foreground");
        return lbTitle;
    }

    public void setSelectedMenu(int index, int subIndex) {
        runEvent(index, subIndex);
    }

    protected void setSelected(int index, int subIndex) {
        int size = panelMenu.getComponentCount();
        for (int i = 0; i < size; i++) {
            Component com = panelMenu.getComponent(i);
            if (com instanceof MenuItem) {
                MenuItem item = (MenuItem) com;
                if (item.getMenuIndex() == index) {
                    item.setSelectedIndex(subIndex);
                } else {
                    item.setSelectedIndex(-1);
                }
            }
        }
    }

    protected void runEvent(int index, int subIndex) {
        MenuAction menuAction = new MenuAction();
        for (MenuEvent event : events) {
            event.menuSelected(index, subIndex, menuAction);
        }
        if (!menuAction.isCancel()) {
            setSelected(index, subIndex);
        }
    }

    public void addMenuEvent(MenuEvent event) {
        events.add(event);
    }

    public void setUpdateItemVisible(boolean visible) {
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                MenuItem item = (MenuItem) com;
                String[] menus = item.getMenus();
                if (menus != null && menus.length > 0 && "Actualizar".equalsIgnoreCase(menus[0])) {
                    item.setVisible(visible);
                }
            }
        }
        panelMenu.revalidate();
        panelMenu.repaint();
    }

    public void hideMenuItem() {
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                ((MenuItem) com).hideMenuItem();
            }
        }
        revalidate();
    }

    public boolean isHideMenuTitleOnMinimum() {
        return hideMenuTitleOnMinimum;
    }

    public int getMenuTitleLeftInset() {
        return menuTitleLeftInset;
    }

    public int getMenuTitleVgap() {
        return menuTitleVgap;
    }

    public int getMenuMaxWidth() {
        return menuMaxWidth;
    }

    public int getMenuMinWidth() {
        return menuMinWidth;
    }

    private JLabel header;
    private JScrollPane scroll;
    private JPanel panelMenu;
    private LightDarkMode lightDarkMode;
    private ToolBarAccentColor toolBarAccentColor;

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
                return new Dimension(5, 5);
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
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int gap = UIScale.scale(5);
                int sheaderFullHgap = UIScale.scale(headerFullHgap);
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int iconWidth = width;
                int iconHeight = header.getPreferredSize().height;
                int hgap = menuFull ? sheaderFullHgap : 0;
                int accentColorHeight = 0;
                if (toolBarAccentColor.isVisible()) {
                    accentColorHeight = toolBarAccentColor.getPreferredSize().height + gap;
                }

                header.setBounds(x + hgap, y, iconWidth - (hgap * 2), iconHeight);
                int ldgap = UIScale.scale(10);
                int ldWidth = width - ldgap * 2;
                int ldHeight = lightDarkMode.getPreferredSize().height;
                int ldx = x + ldgap;
                int ldy = y + height - ldHeight - ldgap - accentColorHeight;

                int menux = x;
                int menuy = y + iconHeight + gap;
                int menuWidth = width;
                int menuHeight = height - (iconHeight + gap) - (ldHeight + ldgap * 2) - (accentColorHeight);
                scroll.setBounds(menux, menuy, menuWidth, menuHeight);

                lightDarkMode.setBounds(ldx, ldy, ldWidth, ldHeight);

                if (toolBarAccentColor.isVisible()) {
                    int tbheight = toolBarAccentColor.getPreferredSize().height;
                    int tbwidth = Math.min(toolBarAccentColor.getPreferredSize().width, ldWidth);
                    int tby = y + height - tbheight - ldgap;
                    int tbx = ldx + ((ldWidth - tbwidth) / 2);
                    toolBarAccentColor.setBounds(tbx, tby, tbwidth, tbheight);
                }
            }
        }
    }

    /**
     * Configura la visibilidad de los elementos del menú según el rol del
     * usuario
     */
    public void configureMenuByUserRole() {
        // Verificar si hay un usuario en sesión
        resetMenu();
        ModelUser currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        header.setText(buildHeaderText());

        String rol = currentUser.getRol().toLowerCase();

        // Filtrar los componentes del menú para mostrar solo los permitidos
        for (Component com : panelMenu.getComponents()) {
            if (com instanceof MenuItem) {
                MenuItem item = (MenuItem) com;
                String[] menus = item.getMenus();

                // Si es título de sección (~SOMETHING~), siempre lo mostramos
                if (menus[0].startsWith("~") && menus[0].endsWith("~")) {
                    continue;
                }

                // Admin y Gerente pueden ver todo
                if (rol.equals("admin") || rol.equals("gerente")) {
                    item.setVisible(true);
                    continue;
                }

                boolean visibleForCurrentRole = false;

                // Verificar visibilidad para el rol vendedor
                if (rol.equals("vendedor")) {
                    String menuName = menus[0].toLowerCase();
                    // Los vendedores pueden ver: Dashboard, Generar venta, Clientes, Movimientos
                    if (menuName.equals("dashboard")
                            || menuName.equals("notificaciones")
                            || menuName.equals("generar venta")
                            || menuName.equals("comercial")
                            || menuName.equals("productos")
                            || menuName.equals("cerrar sesión")) {
                        // || (menuName.contains("productos") && (menus.length > 1 &&
                        // (menus[5].toLowerCase().equals("movimientos")
                        // || menus[6].toLowerCase().equals("rotulación"))))
                        // || menuName.equals("logout")) {

                        visibleForCurrentRole = true;

                        // Para submenús, ocultamos los que no corresponden
                        if (menuName.equals("comercial") && menus.length > 2) {
                            // En Comercial, vendedores solo ven Clientes
                            ocultarSubmenuEspecifico(item, "proveedores");
                            ocultarSubmenuEspecifico(item, "compras");
                        }

                        if (menuName.contains("productos") && menus.length > 1) {
                            // En Productos, vendedores solo ven Movimientos y Rotulación
                            ocultarSubmenuEspecifico(item, "gestion de productos");
                            ocultarSubmenuEspecifico(item, "inventario");
                            ocultarSubmenuEspecifico(item, "marcas");
                            ocultarSubmenuEspecifico(item, "categorias");
                            ocultarSubmenuEspecifico(item, "Movimientos");
                            ocultarSubmenuEspecifico(item, "Rotulación");
                            ocultarSubmenuEspecifico(item, "color");
                            ocultarSubmenuEspecifico(item, "Prestamos");
                            ocultarSubmenuEspecifico(item, "Promociones");
                        }
                    }
                }

                // Verificar visibilidad para el rol almacén
                if (rol.equals("almacen")) {
                    String menuName = menus[0].toLowerCase();
                    // Almacén puede ver: Dashboard, Generar venta, Gestión productos, Inventario,
                    // Rotulación
                    if (menuName.equals("dashboard")
                            || menuName.equals("notificaciones")
                            || menuName.equals("generar venta")
                            || menuName.contains("productos")
                            || menuName.equals("cerrar sesión")) {

                        visibleForCurrentRole = true;

                        // Para el submenú de Productos, almacén solo ve algunos
                        if (menuName.contains("productos") && menus.length > 1) {
                            // En Productos, almacén ve Gestión productos, Inventario y Rotulación
                            ocultarSubmenuEspecifico(item, "marcas");
                            ocultarSubmenuEspecifico(item, "categorias");
                            ocultarSubmenuEspecifico(item, "movimientos");
                        }
                    }
                }

                // Aplicar visibilidad según el rol
                item.setVisible(visibleForCurrentRole);
            }
        }

        // Revalidar el panel
        panelMenu.revalidate();
        panelMenu.repaint();
    }

    /**
     * Oculta un submenú específico dentro de un MenuItem
     *
     * @param menuItem    El MenuItem que contiene los submenús
     * @param submenuName Nombre del submenú a ocultar (case insensitive)
     */
    private void ocultarSubmenuEspecifico(MenuItem menuItem, String submenuName) {
        // Iterar por los componentes del MenuItem (que son JButton)
        for (Component comp : menuItem.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                // Si el texto del botón coincide con el submenú a ocultar
                if (btn.getText().toLowerCase().equals(submenuName.toLowerCase())) {
                    btn.setVisible(false);
                }
            }
        }
    }
}
