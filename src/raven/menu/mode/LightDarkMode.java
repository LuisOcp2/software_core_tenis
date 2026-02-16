// Declara el paquete al que pertenece la clase
package raven.menu.mode;

// Importaciones de librerías FlatLaf y Swing
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.util.prefs.Preferences;
import raven.toast.Notifications;

/**
 * Clase para manejar el cambio de tema claro/oscuro
 * 
 * @author Raven
 */
public class LightDarkMode extends JPanel {

    // Método para cambiar la visualización del menú (expandido/contraído)
    public void setMenuFull(boolean menuFull) {
        this.menuFull = menuFull;
        if (menuFull) {
            buttonLight.setVisible(true);
            buttonDark.setVisible(true);
            buttonLighDark.setVisible(false);
        } else {
            buttonLight.setVisible(false);
            buttonDark.setVisible(false);
            buttonLighDark.setVisible(true);
        }
    }

    private boolean menuFull = true; // Estado del menú (expandido por defecto)

    // Constructor de la clase
    public LightDarkMode() {
        init(); // Inicializa los componentes
    }

    // Método de inicialización de componentes
    private void init() {
        setBorder(new EmptyBorder(2, 2, 2, 2)); // Borde vacío de 2px
        setLayout(new LightDarkModeLayout()); // Usa un layout personalizado

        // Estilo FlatLaf para el panel principal
        putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:999;" // Bordes completamente redondeados
                + "background:$Menu.lightdark.background"); // Color de fondo

        // Botones con iconos SVG
        buttonLight = new JButton("Light", new FlatSVGIcon("raven/menu/mode/light.svg"));
        buttonDark = new JButton("Dark", new FlatSVGIcon("raven/menu/mode/dark.svg"));
        buttonLighDark = new JButton(); // Botón combinado para menú contraído

        // Estilo del botón combinado
        buttonLighDark.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:999;" // Forma circular
                + "background:$Menu.lightdark.button.background;"
                + "foreground:$Menu.foreground;"
                + "focusWidth:0;" // Elimina el borde de enfoque
                + "borderWidth:0;" // Sin borde
                + "innerFocusWidth:0"); // Sin borde interno

        // Acción para cambiar el modo al hacer clic
        buttonLighDark.addActionListener((ActionEvent e) -> {
            changeMode(!FlatLaf.isLafDark());
        });

        checkStyle(); // Aplica estilos iniciales

        // Listeners para los botones individuales
        buttonDark.addActionListener((ActionEvent e) -> {
            changeMode(true); // Activa modo oscuro
        });
        buttonLight.addActionListener((ActionEvent e) -> {
            changeMode(false); // Activa modo claro
        });

        // Agrega los componentes al panel
        add(buttonLight);
        add(buttonDark);
        add(buttonLighDark);
        applySavedTheme();
    }

    // Método para cambiar el tema
    private void changeMode(boolean dark) {
        if (FlatLaf.isLafDark() != dark) {
            Preferences prefs = Preferences.userRoot().node("app/ui");
            if (dark) {
                EventQueue.invokeLater(() -> {
                    FlatAnimatedLafChange.showSnapshot(); // Animación de transición
                    FlatMacDarkLaf.setup(); // Aplica tema oscuro
                    FlatLaf.updateUI(); // Actualiza la interfaz
                    checkStyle(); // Revisa estilos
                    FlatAnimatedLafChange.hideSnapshotWithAnimation(); // Completa animación
                    try { prefs.put("theme", "dark"); Notifications.getInstance().show(Notifications.Type.INFO, "Tema oscuro activado"); } catch (Exception ex) { Notifications.getInstance().show(Notifications.Type.WARNING, "No se pudo guardar preferencia de tema"); }
                });
            } else {
                EventQueue.invokeLater(() -> {
                    FlatAnimatedLafChange.showSnapshot();
                    FlatMacLightLaf.setup(); // Aplica tema claro
                    FlatLaf.updateUI();
                    checkStyle();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();

                    try { prefs.put("theme", "light"); Notifications.getInstance().show(Notifications.Type.INFO, "Tema claro activado"); } catch (Exception ex) { Notifications.getInstance().show(Notifications.Type.WARNING, "No se pudo guardar preferencia de tema"); }
                });
            }
        }
    }

    // Carga el último tema guardado
    private void applySavedTheme() {
        Preferences prefs = Preferences.userRoot().node("app/ui");
        String saved = prefs.get("theme", "");
        if ("dark".equals(saved)) {
            FlatMacDarkLaf.setup();
        } else if ("light".equals(saved)) {
            FlatMacLightLaf.setup();
        } else if ("custom".equals(saved)) {
            FlatMacLightLaf.setup();
        }
        FlatLaf.updateUI();
        checkStyle();
    }

    // Actualiza los estilos de los botones según el tema actual
    private void checkStyle() {
        boolean isDark = FlatLaf.isLafDark();
        addStyle(buttonLight, !isDark); // Estilo activo/inactivo
        addStyle(buttonDark, isDark);
        // Cambia el icono del botón combinado
        if (isDark) {
            buttonLighDark.setIcon(new FlatSVGIcon("raven/menu/mode/dark.svg"));
        } else {
            buttonLighDark.setIcon(new FlatSVGIcon("raven/menu/mode/light.svg"));
        }
    }

    // Aplica estilos a los botones individuales
    private void addStyle(JButton button, boolean style) {
        if (style) {
            // Estilo cuando el botón está activo
            button.putClientProperty(FlatClientProperties.STYLE, ""
                    + "arc:999;"
                    + "background:$Menu.lightdark.button.background;"
                    + "foreground:$Menu.foreground;"
                    + "focusWidth:0;"
                    + "borderWidth:0;"
                    + "innerFocusWidth:0");
        } else {
            // Estilo cuando el botón está inactivo
            button.putClientProperty(FlatClientProperties.STYLE, ""
                    + "arc:999;"
                    + "background:$Menu.lightdark.button.background;"
                    + "foreground:$Menu.foreground;"
                    + "focusWidth:0;"
                    + "borderWidth:0;"
                    + "innerFocusWidth:0;"
                    + "background:null"); // Fondo transparente
        }
    }

    // Variables de los componentes
    private JButton buttonLight;
    private JButton buttonDark;
    private JButton buttonLighDark;

    // Clase interna para el layout personalizado
    private class LightDarkModeLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // No se implementa
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // No se implementa
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                // Calcula el tamaño preferido basado en el estado del menú
                return new Dimension(5, buttonDark.getPreferredSize().height + (menuFull ? 0 : 5));
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return new Dimension(0, 0); // Tamaño mínimo cero
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                // Lógica de posicionamiento de componentes
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int gap = 5; // Espacio entre botones
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                int buttonWidth = (width - gap) / 2; // Ancho para 2 botones en modo expandido

                if (menuFull) {
                    // Posicionamiento para menú expandido
                    buttonLight.setBounds(x, y, buttonWidth, height);
                    buttonDark.setBounds(x + buttonWidth + gap, y, buttonWidth, height);
                } else {
                    // Posicionamiento para menú contraído
                    buttonLighDark.setBounds(x, y, width, height);
                }
            }
        }
    }
}
