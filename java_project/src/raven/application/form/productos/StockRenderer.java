package raven.application.form.productos;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class StockRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value != null) {
            try {
                int stock = Integer.parseInt(value.toString());

                if (isSelected) {
                    label.setForeground(table.getSelectionForeground());
                } else {
                    if (stock > 10) {
                        label.setForeground(new Color(22, 163, 74)); // Green-600
                    } else if (stock > 0) {
                        label.setForeground(new Color(234, 88, 12)); // Orange-600
                    } else {
                        label.setForeground(new Color(220, 38, 38)); // Red-600
                    }
                }

                label.setFont(label.getFont().deriveFont(Font.BOLD));

            } catch (NumberFormatException e) {
                // Ignore if not a number
            }
        }

        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }
}
