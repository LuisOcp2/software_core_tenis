/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.theme;

// Create a file named FlatLafConfig.java
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

public class FlatLafConfig {

    // Paleta de grises estilo iOS
    public static final Color GRAY_00 = new Color(255, 255, 255); // Blanco
    public static final Color GRAY_05 = new Color(248, 249, 252); // Fondo alterno
    public static final Color GRAY_10 = new Color(242, 244, 248); // Fondo principal
    public static final Color GRAY_20 = new Color(229, 232, 240); // Borde suave
    public static final Color GRAY_30 = new Color(214, 217, 224); // Borde definido
    public static final Color GRAY_40 = new Color(190, 194, 204); // Placeholder
    public static final Color GRAY_50 = new Color(142, 142, 147); // Texto terciario / borde fuerte
    public static final Color GRAY_60 = new Color(99, 105, 114);  // Texto secundario
    public static final Color GRAY_70 = new Color(40, 44, 52);    // Texto principal

    public static final Color GRAY_05_ALPHA = new Color(0, 0, 0, 13);  // 5% negro (hover muy sutil)
    public static final Color GRAY_08_ALPHA = new Color(0, 0, 0, 20);  // 8% negro (sombra ligera)
    public static final Color PANEL_TINT = new Color(220, 228, 246, 230); // Gris azul medio transl?cido m?s marcado

    /**
     * Colores de acento - Azul profesional con diferentes tonalidades
     */
    public static final Color ACCENT_BLUE = new Color(59, 130, 246);          // React blue-600
    public static final Color ACCENT_BLUE_PRIMARY = new Color(0, 122, 255);   // iOS Blue
    public static final Color ACCENT_BLUE_HOVER = new Color(10, 132, 255);    // Hover state
    public static final Color ACCENT_BLUE_PRESSED = new Color(0, 112, 245);   // Pressed state

    /**
     * Colores de fondo - Sistema de capas
     */
    public static final Color BACKGROUND_PRIMARY = GRAY_00;    // Fondo principal (blanco)
    public static final Color BACKGROUND_SECONDARY = PANEL_TINT;  // Tarjetas y paneles (gris azul claro)
    public static final Color BACKGROUND_TERTIARY = GRAY_10;   // ?reas alternadas (gris ligeramente m?s oscuro)

    /**
     * Colores de texto - Jerarqu?a clara
     */
    public static final Color TEXT_PRIMARY = GRAY_70;          // Texto principal
    public static final Color TEXT_SECONDARY = GRAY_60;        // Texto secundario
    public static final Color TEXT_TERTIARY = GRAY_50;         // Texto terciario
    public static final Color TEXT_PLACEHOLDER = GRAY_40;      // Placeholders

    /**
     * Colores de bordes y separadores
     */
    public static final Color BORDER_COLOR = GRAY_50;          // Bordes m?s marcados para contraste
    public static final Color SEPARATOR_COLOR = GRAY_20;       // Separadores muy sutiles

    /**
     * Colores de estado - Sem?foro claro
     */
    public static final Color SUCCESS_COLOR = new Color(52, 199, 89);         // Verde iOS
    public static final Color WARNING_COLOR = new Color(255, 149, 0);         // Naranja iOS
    public static final Color ERROR_COLOR = new Color(255, 59, 48);           // Rojo iOS
    public static final Color INFO_COLOR = new Color(90, 200, 250);           // Cyan iOS

    /**
     * Colores para acciones (botones)
     */
    public static final Color ACTION_CREATE = new Color(46, 125, 50);   // Verde crear
    public static final Color ACTION_EDIT = new Color(21, 101, 192);    // Azul editar
    public static final Color ACTION_DELETE = new Color(198, 40, 40);   // Rojo borrar
    public static final Color ACTION_PRINT = new Color(72, 85, 99);     // Gris azulado imprimir
    public static final Color ACTION_PDF = new Color(220, 53, 69);      // Rojo pdf
    public static final Color ACTION_EXCEL = new Color(30, 136, 72);    // Verde excel
    public static final Color ACTION_FILTER = new Color(13, 148, 136);  // Verde azulado filtrar
    public static final Color ACTION_RESET = new Color(120, 120, 128);  // Gris reiniciar

    /**
     * Estados de botones y elementos interactivos
     */
    public static final Color HOVER_BACKGROUND = GRAY_05;         // Hover sutil
    public static final Color PRESSED_BACKGROUND = GRAY_20;       // Pressed m?s visible
    public static final Color SELECTED_BACKGROUND = new Color(232, 240, 254); // Selecci?n con tinte azul

    // ========================================================================
    // CONFIGURACI?N PRINCIPAL
    // ========================================================================

    public static void setup() {
        // Registrar fuente Roboto
        FlatRobotoFont.install();

        // Configurar fuentes del sistema con tama?os optimizados
        configurarFuentes();

        // Configurar componentes base
        configurarComponentesBase();

        // Configurar botones
        configurarBotones();

        // Configurar campos de texto
        configurarCamposTexto();

        // Configurar tablas
        configurarTablas();

        // Configurar men?s
        configurarMenus();

        // Configurar scrollbars
        configurarScrollbars();

        // Configurar otros componentes
        configurarComponentesAdicionales();

        // Aplicar tema
        aplicarTema();
    }

    /**
     * Configura las fuentes del sistema con jerarqu?a clara
     */
    private static void configurarFuentes() {
        // Fuente base del sistema
        Font baseFont = new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13);
        UIManager.put("defaultFont", baseFont);

        // Jerarqu?a de fuentes para t?tulos
        UIManager.put("h0.font", new Font(FlatRobotoFont.FAMILY, Font.BOLD, 28));   // T?tulo principal
        UIManager.put("h1.font", new Font(FlatRobotoFont.FAMILY, Font.BOLD, 24));   // T?tulos grandes
        UIManager.put("h2.font", new Font(FlatRobotoFont.FAMILY, Font.BOLD, 20));   // Subt?tulos
        UIManager.put("h3.font", new Font(FlatRobotoFont.FAMILY, Font.BOLD, 17));   // T?tulos de secci?n
        UIManager.put("h4.font", new Font(FlatRobotoFont.FAMILY, Font.BOLD, 15));   // T?tulos peque?os

        // Fuentes especiales
        UIManager.put("large.font", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 15));
        UIManager.put("medium.font", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        UIManager.put("small.font", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 11));
        UIManager.put("mini.font", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 10));
    }

    /**
     * Configura los componentes base del sistema
     */
    private static void configurarComponentesBase() {
        // Colores base
        UIManager.put("background", BACKGROUND_PRIMARY);
        UIManager.put("foreground", TEXT_PRIMARY);

        // Paneles y contenedores
        UIManager.put("Panel.background", BACKGROUND_SECONDARY);
        UIManager.put("Panel.foreground", TEXT_PRIMARY);

        // Bordes y separadores
        UIManager.put("Component.borderColor", BORDER_COLOR);
        UIManager.put("Component.focusColor", ACCENT_BLUE_PRIMARY);
        UIManager.put("Component.focusWidth", 2);  // Foco m?s visible
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.arc", 8);  // Bordes redondeados estilo iOS
        UIManager.put("Component.iconColor", Color.WHITE);
        UIManager.put("Component.disabledIconColor", GRAY_40);

        UIManager.put("Separator.foreground", SEPARATOR_COLOR);
        UIManager.put("Separator.background", BACKGROUND_SECONDARY);

        // Sombras sutiles para dar profundidad
        UIManager.put("Popup.dropShadowPainted", true);
        UIManager.put("Popup.dropShadowOpacity", 0.08f);
    }

    /**
     * Configura los botones con estados claros
     */
    private static void configurarBotones() {
        // Forma y espaciado
        UIManager.put("Button.arc", 8);
        UIManager.put("Button.margin", new javax.swing.plaf.InsetsUIResource(8, 16, 8, 16));

        // Bot?n primario (con acento)
        UIManager.put("Button.default.background", ACCENT_BLUE_PRIMARY);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.default.hoverBackground", ACCENT_BLUE_HOVER);
        UIManager.put("Button.default.pressedBackground", ACCENT_BLUE_PRESSED);
        UIManager.put("Button.default.borderWidth", 0);

        // Botón por defecto (tema blanco): azul con texto/icono blanco
        UIManager.put("Button.background", ACCENT_BLUE_PRIMARY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.iconColor", Color.WHITE);
        UIManager.put("Button.disabledIconColor", GRAY_40);
        UIManager.put("Button.hoverBackground", ACCENT_BLUE_HOVER);
        UIManager.put("Button.pressedBackground", ACCENT_BLUE_PRESSED);
        UIManager.put("Button.borderColor", ACCENT_BLUE_PRIMARY);
        UIManager.put("Button.borderWidth", 1);

        // Estados de foco
        UIManager.put("Button.focusedBackground", ACCENT_BLUE_PRIMARY);
        UIManager.put("Button.focusedForeground", Color.WHITE);
        UIManager.put("Button.focusedBorderColor", ACCENT_BLUE_PRIMARY);

        // Sombra sutil en botones
        UIManager.put("Button.shadowColor", GRAY_05_ALPHA);
    }

    /**
     * Configura los campos de texto con mejor contraste
     */
    private static void configurarCamposTexto() {
        // Forma
        UIManager.put("TextComponent.arc", 8);

        // TextField
        UIManager.put("TextField.background", GRAY_00);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.inactiveForeground", TEXT_TERTIARY);
        UIManager.put("TextField.placeholderForeground", TEXT_PLACEHOLDER);
        UIManager.put("TextField.margin", new javax.swing.plaf.InsetsUIResource(8, 12, 8, 12));
        UIManager.put("TextField.borderColor", BORDER_COLOR);
        UIManager.put("TextField.focusedBorderColor", ACCENT_BLUE_PRIMARY);
        UIManager.put("TextField.disabledBackground", GRAY_05);
        UIManager.put("TextField.disabledForeground", TEXT_TERTIARY);

        // PasswordField
        UIManager.put("PasswordField.background", GRAY_00);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.margin", new javax.swing.plaf.InsetsUIResource(8, 12, 8, 12));
        UIManager.put("PasswordField.borderColor", BORDER_COLOR);
        UIManager.put("PasswordField.focusedBorderColor", ACCENT_BLUE_PRIMARY);

        // TextArea
        UIManager.put("TextArea.background", GRAY_00);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.margin", new javax.swing.plaf.InsetsUIResource(8, 12, 8, 12));

        // ComboBox
        UIManager.put("ComboBox.background", GRAY_00);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.buttonBackground", GRAY_00);
        UIManager.put("ComboBox.padding", new javax.swing.plaf.InsetsUIResource(8, 12, 8, 12));
        UIManager.put("ComboBox.borderColor", BORDER_COLOR);
        UIManager.put("ComboBox.focusedBorderColor", ACCENT_BLUE_PRIMARY);

        // Selecci?n en ComboBox
        UIManager.put("ComboBox.selectionBackground", SELECTED_BACKGROUND);
        UIManager.put("ComboBox.selectionForeground", ACCENT_BLUE_PRIMARY);
    }

    /**
     * Configura las tablas con mejor legibilidad
     */
    private static void configurarTablas() {
        // Colores base
        UIManager.put("Table.background", GRAY_05);
        UIManager.put("Table.foreground", TEXT_SECONDARY);
        UIManager.put("Table.gridColor", GRAY_30);

        // Filas alternadas para mejor legibilidad
        UIManager.put("Table.alternateRowColor", GRAY_10);

        // Selecci?n
        UIManager.put("Table.selectionBackground", new Color(34, 119, 230));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.selectionInactiveBackground", new Color(229, 238, 255));
        UIManager.put("Table.selectionInactiveForeground", TEXT_PRIMARY);

        // Encabezado de tabla
        UIManager.put("TableHeader.background", GRAY_10);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("TableHeader.separatorColor", GRAY_30);
        UIManager.put("TableHeader.bottomSeparatorColor", GRAY_30);
        UIManager.put("TableHeader.hoverBackground", GRAY_05);
        UIManager.put("TableHeader.font", new Font(FlatRobotoFont.FAMILY, Font.BOLD, 12));

        // Celdas
        UIManager.put("Table.cellMargins", new javax.swing.plaf.InsetsUIResource(6, 12, 6, 12));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);  // Solo l?neas horizontales para limpieza
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
    }

    /**
     * Configura los men?s laterales
     */
    private static void configurarMenus() {
        // Men? principal (paleta azul existente)
        UIManager.put("Menu.background", new Color(30, 58, 95));
        UIManager.put("Menu.foreground", new Color(248, 250, 252));
        UIManager.put("Menu.selectionBackground", new Color(40, 68, 105));
        UIManager.put("Menu.selectionForeground", Color.WHITE);

        // Items del men?
        UIManager.put("MenuItem.background", new Color(30, 58, 95));
        UIManager.put("MenuItem.foreground", new Color(248, 250, 252));
        UIManager.put("MenuItem.selectionBackground", new Color(40, 68, 105));
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);

        // Scrollbar del men?
        UIManager.put("Menu.ScrollBar.background", new Color(30, 58, 95));
        UIManager.put("Menu.ScrollBar.thumb", new Color(60, 88, 125));
        UIManager.put("Menu.ScrollBar.hoverThumbColor", new Color(80, 108, 145));
    }

    /**
     * Configura los scrollbars con estilo moderno
     */
    private static void configurarScrollbars() {
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);  // Completamente redondeado
        UIManager.put("ScrollBar.thumbInsets", new javax.swing.plaf.InsetsUIResource(2, 2, 2, 2));

        UIManager.put("ScrollBar.background", BACKGROUND_PRIMARY);
        UIManager.put("ScrollBar.thumb", GRAY_30);
        UIManager.put("ScrollBar.hoverThumbColor", GRAY_40);
        UIManager.put("ScrollBar.pressedThumbColor", GRAY_50);

        UIManager.put("ScrollBar.track", BACKGROUND_PRIMARY);
        UIManager.put("ScrollBar.trackArc", 999);

        // ScrollPane
        UIManager.put("ScrollPane.arc", 8);
        UIManager.put("ScrollPane.background", BACKGROUND_SECONDARY);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER_COLOR, 1));
    }

    /**
     * Configura componentes adicionales
     */
    private static void configurarComponentesAdicionales() {
        // TabbedPane - Estilo iOS
        UIManager.put("TabbedPane.selectedBackground", ACCENT_BLUE_PRIMARY);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
        UIManager.put("TabbedPane.hoverColor", HOVER_BACKGROUND);
        UIManager.put("TabbedPane.underlineColor", ACCENT_BLUE_PRIMARY);
        UIManager.put("TabbedPane.tabHeight", 44);
        UIManager.put("TabbedPane.contentAreaColor", BACKGROUND_SECONDARY);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.tabInsets", new javax.swing.plaf.InsetsUIResource(8, 20, 8, 20));

        // ProgressBar
        UIManager.put("ProgressBar.background", GRAY_05);
        UIManager.put("ProgressBar.foreground", ACCENT_BLUE_PRIMARY);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ProgressBar.trackArc", 999);

        // CheckBox y RadioButton
        UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
        UIManager.put("CheckBox.icon.selectedBackground", ACCENT_BLUE_PRIMARY);
        UIManager.put("CheckBox.icon.selectedBorderColor", ACCENT_BLUE_PRIMARY);
        UIManager.put("CheckBox.icon.borderColor", BORDER_COLOR);
        UIManager.put("CheckBox.icon.hoverBorderColor", ACCENT_BLUE_PRIMARY);

        UIManager.put("RadioButton.icon.selectedBackground", ACCENT_BLUE_PRIMARY);
        UIManager.put("RadioButton.icon.selectedBorderColor", ACCENT_BLUE_PRIMARY);
        UIManager.put("RadioButton.icon.borderColor", BORDER_COLOR);
        UIManager.put("RadioButton.icon.hoverBorderColor", ACCENT_BLUE_PRIMARY);

        // Toast notifications
        UIManager.put("Toast.background", BACKGROUND_SECONDARY);
        UIManager.put("Toast.foreground", TEXT_PRIMARY);
        UIManager.put("Toast.borderColor", BORDER_COLOR);
        UIManager.put("Toast.arc", 12);

        // Tooltips
        UIManager.put("ToolTip.background", new Color(50, 50, 50));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    /**
     * Aplica el tema FlatLaf
     */
    private static void aplicarTema() {
        try {
            FlatLightLaf.setup();
            System.out.println("V FlatLaf Mejorado configurado correctamente");
        } catch (Exception ex) {
            System.err.println("? Error al inicializar FlatLaf: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ========================================================================
    // M?TODOS DE UTILIDAD PARA USO EN COMPONENTES
    // ========================================================================

    /**
     * Aplica estilo de tarjeta a un panel
     *
     * @param panel Panel a estilizar
     */
    public static void aplicarEstiloTarjeta(javax.swing.JPanel panel) {
        panel.setBackground(BACKGROUND_SECONDARY);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
    }

    /**
     * Aplica estilo de badge/etiqueta de estado
     *
     * @param label Label a estilizar
     * @param tipo Tipo de estado: "success", "warning", "error", "info"
     */
    public static void aplicarEstiloEstado(javax.swing.JLabel label, String tipo) {
        Color bgColor, fgColor;

        switch (tipo.toLowerCase()) {
            case "success":
            case "recibido":
                bgColor = new Color(220, 252, 231);  // Verde claro
                fgColor = new Color(22, 101, 52);     // Verde oscuro
                break;
            case "warning":
            case "en_transito":
                bgColor = new Color(254, 243, 199);  // Amarillo claro
                fgColor = new Color(146, 64, 14);     // Amarillo oscuro
                break;
            case "error":
            case "cancelado":
                bgColor = new Color(254, 226, 226);  // Rojo claro
                fgColor = new Color(153, 27, 27);     // Rojo oscuro
                break;
            case "info":
            case "autorizado":
                bgColor = new Color(219, 234, 254);  // Azul claro
                fgColor = new Color(30, 64, 175);     // Azul oscuro
                break;
            default:
                bgColor = BACKGROUND_TERTIARY;
                fgColor = TEXT_SECONDARY;
        }

        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(fgColor);
        label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        label.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
    }

    /**
     * Aplica estilo de bot?n de acci?n (color + texto/icono blancos)
     * tipos: crear, editar, borrar, imprimir, pdf, excel, filtrar, reiniciar
     */
    public static void aplicarBotonAccion(javax.swing.JButton boton, String tipo) {
        String key = tipo == null ? "" : tipo.toLowerCase().trim();
        Color base;
        switch (key) {
            case "crear":
            case "create":
                base = ACTION_CREATE;
                break;
            case "editar":
            case "edit":
                base = ACTION_EDIT;
                break;
            case "borrar":
            case "eliminar":
            case "delete":
                base = ACTION_DELETE;
                break;
            case "imprimir":
            case "print":
                base = ACTION_PRINT;
                break;
            case "pdf":
                base = ACTION_PDF;
                break;
            case "excel":
                base = ACTION_EXCEL;
                break;
            case "filtrar":
            case "filter":
                base = ACTION_FILTER;
                break;
            case "reiniciar":
            case "reset":
                base = ACTION_RESET;
                break;
            default:
                base = ACCENT_BLUE_PRIMARY;
        }

        Color hover = shade(base, 1.08f);
        Color pressed = shade(base, 0.92f);
        Color border = shade(base, 0.85f);

        String style = String.format(
            "background:%s; foreground:#ffffff; iconColor:#ffffff; hoverBackground:%s; pressedBackground:%s; borderColor:%s; borderWidth:1; focusColor:#007aff; arc:8;",
            toHex(base), toHex(hover), toHex(pressed), toHex(border)
        );

        boton.setOpaque(true);
        boton.setBackground(base);
        boton.setForeground(Color.WHITE);
        boton.putClientProperty(FlatClientProperties.STYLE, style);
    }

    private static Color shade(Color color, float factor) {
        int r = Math.min(255, Math.max(0, Math.round(color.getRed() * factor)));
        int g = Math.min(255, Math.max(0, Math.round(color.getGreen() * factor)));
        int b = Math.min(255, Math.max(0, Math.round(color.getBlue() * factor)));
        return new Color(r, g, b, color.getAlpha());
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Obtiene el color de acento actual
     */
    public static Color getAccentColor() {
        return ACCENT_BLUE_PRIMARY;
    }
}
