package raven.application.form.comercial.devolucion.components;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import javax.swing.*;

/**
 * Alerta moderna con icono, título y mensaje.
 * Tipos: WARNING (amarillo), SUCCESS (verde), INFO (azul)
 * Basado en diseño HTML sketch - compacta y semántica
 */
public class ModernAlert extends JPanel {

    public enum AlertType {
        WARNING("#fbbf24", "#fbf22426", "#fbf2244d", "⚠️"), // 26=15%, 4D=30%
        SUCCESS("#34d399", "#10b98126", "#10b9814d", "✓"),
        INFO("#60a5fa", "#3b82f626", "#3b82f64d", "ℹ️"),
        ERROR("#f87171", "#ef444426", "#ef44444d", "❌");

        final String textColor;
        final String bgColor;
        final String borderColor;
        final String icon;

        AlertType(String textColor, String bgColor, String borderColor, String icon) {
            this.textColor = textColor;
            this.bgColor = bgColor;
            this.borderColor = borderColor;
            this.icon = icon;
        }
    }

    public ModernAlert(AlertType type, String title, String message) {
        initComponents(type, title, message);
    }

    private void initComponents(AlertType type, String title, String message) {
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
        setPreferredSize(new Dimension(0, 60));

        // Apply styled background
        putClientProperty(FlatClientProperties.STYLE,
                "background:" + type.bgColor + ";" +
                        "border:1,1,1,1," + type.borderColor);

        // Icon
        JLabel iconLabel = new JLabel(type.icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        // Content panel
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>");
        titleLabel.setForeground(hexToColor(type.textColor));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Message
        JLabel msgLabel = new JLabel("<html>" + message + "</html>");
        msgLabel.setForeground(new Color(160, 174, 192)); // #a0aec0
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(msgLabel);

        add(iconLabel, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    private static Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        return new Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16));
    }
}
