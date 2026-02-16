package raven.application.form.comercial.devolucion.components;

import java.awt.Color;

/**
 * Constantes de diseño basadas en sketch HTML
 * Tema oscuro con colores vibrantes
 */
public class DesignConstants {

    // === COLORES DE FONDO ===
    public static final Color BG_DARK = new Color(30, 41, 54); // #1e2936
    public static final Color BG_CARD = new Color(45, 55, 72); // #2d3748
    public static final Color BG_ACCENT = new Color(61, 80, 102); // #3d5066
    public static final Color BG_PANEL = new Color(37, 54, 72); // #253648

    // === COLORES PRIMARIOS ===
    public static final Color BLUE_PRIMARY = new Color(59, 130, 246); // #3b82f6
    public static final Color BLUE_HOVER = new Color(37, 99, 235); // #2563eb
    public static final Color GREEN_SUCCESS = new Color(16, 185, 129); // #10b981
    public static final Color YELLOW_WARNING = new Color(251, 191, 36); // #fbbf24
    public static final Color RED_DANGER = new Color(239, 68, 68); // #ef4444

    // === COLORES DE TEXTO ===
    public static final Color TEXT_PRIMARY = new Color(255, 255, 255); // #ffffff
    public static final Color TEXT_SECONDARY = new Color(160, 174, 192); // #a0aec0
    public static final Color TEXT_MUTED = new Color(113, 128, 150); // #718096

    // === COLORES DE BORDER ===
    public static final Color BORDER_DEFAULT = new Color(74, 85, 104); // #4a5568
    public static final Color BORDER_FOCUS = BLUE_PRIMARY;

    // === ESTILOS FLATLAF ===

    public static final String PANEL_CARD = "arc:12;" +
            "background:#2d3748;" +
            "border:16,16,16,16";

    public static final String PANEL_ACCENT = "arc:8;" +
            "background:#3d5066;" +
            "border:20,20,20,20";

    public static final String BUTTON_PRIMARY = "arc:8;" +
            "background:#3b82f6;" +
            "foreground:#ffffff;" +
            "font:bold +1;" +
            "minimumHeight:44;" +
            "hoverBackground:#2563eb;" +
            "pressedBackground:#1d4ed8;" +
            "borderWidth:0";

    public static final String BUTTON_SECONDARY = "arc:6;" +
            "background:rgba(59,130,246,0.1);" +
            "foreground:#3b82f6;" +
            "font:normal;" +
            "minimumHeight:36;" +
            "border:1,1,1,1,rgba(59,130,246,0.3);" +
            "hoverBackground:rgba(59,130,246,0.2)";

    public static final String INPUT_FIELD = "arc:8;" +
            "background:#1e2936;" +
            "foreground:#ffffff;" +
            "border:2,2,2,2,#4a5568;" +
            "focusedBorder:2,2,2,2,#3b82f6;" +
            "placeholderForeground:#718096";

    public static final String SIDEBAR_PANEL = "arc:8;" +
            "background:#2d3748;" +
            "border:20,20,20,20";

    // === UTILIDADES ===

    public static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x",
                color.getRed(),
                color.getGreen(),
                color.getBlue());
    }

    public static Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        return new Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16));
    }
}
