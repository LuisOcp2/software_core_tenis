package raven.application.form.productos;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.Color;

public class LocationRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value != null && !value.toString().isEmpty()) {
            label.setIcon(FontIcon.of(FontAwesomeSolid.MAP_MARKER_ALT, 14,
                    isSelected ? table.getSelectionForeground() : new Color(220, 53, 69))); // Red marker
            label.setText(" " + value.toString());
        } else {
            label.setIcon(null);
            label.setText("-");
        }

        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }
}
