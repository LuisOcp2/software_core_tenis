package raven.application.form.productos;

import raven.application.form.productos.dto.TallaInfo;
import net.miginfocom.swing.MigLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class TallasCellRenderer extends JPanel implements TableCellRenderer {

    public TallasCellRenderer() {
        // Technical Layout: 3 columns of data items
        // simple grid, efficient spacing
        setLayout(new MigLayout("wrap 3, insets 4, gap 15 8, fillx"));
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        removeAll();

        // Use table background logic
        // For a technical view, we want high contrast
        if (isSelected) {
            setBackground(table.getSelectionBackground());
        } else {
            setBackground(table.getBackground());
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object obj : list) {
                if (obj instanceof TallaInfo) {
                    add(createTechnicalItem((TallaInfo) obj, isSelected));
                }
            }
        }

        return this;
    }

    private Component createTechnicalItem(TallaInfo info, boolean isSelected) {
        // A simple panel holding: Size | Stock | Location
        // Designed for readability
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 5, fill"));
        panel.setOpaque(false);

        // 1. Size: Bold, clear
        String sizeName = info.getNombreTalla();
        JLabel lbSize = new JLabel(sizeName);
        lbSize.setFont(getFont().deriveFont(Font.BOLD, 12f));
        if (isSelected) {
            lbSize.setForeground(Color.WHITE);
        } else {
            lbSize.setForeground(new Color(220, 220, 220)); // Off-white for dark mode
        }

        // 2. Stock: Colored strictly by status
        int stockP = info.getStockPares();
        int stockC = info.getStockCajas();
        int total = stockP + stockC;

        Color stockColor;
        if (total == 0)
            stockColor = new Color(150, 150, 150); // Grey
        else if (total < 4)
            stockColor = new Color(255, 193, 7); // Warning Amber
        else
            stockColor = new Color(76, 175, 80); // Success Green

        StringBuilder sb = new StringBuilder();
        if (stockP > 0)
            sb.append(stockP).append(" Prs");
        if (stockC > 0) {
            if (sb.length() > 0)
                sb.append(" / ");
            sb.append(stockC).append(" Cjs");
        }
        if (total == 0)
            sb.append("0");

        JLabel lbStock = new JLabel(sb.toString());
        lbStock.setFont(getFont().deriveFont(12f));
        lbStock.setForeground(stockColor);

        // 3. Location: Monospaced or distinct
        String loc = (info.getUbicacion() != null && !info.getUbicacion().isEmpty()) ? "[" + info.getUbicacion() + "]"
                : "";
        JLabel lbLoc = new JLabel(loc);
        lbLoc.setFont(getFont().deriveFont(11f));
        lbLoc.setForeground(new Color(100, 181, 246)); // Blue-ish for technical info

        // Add to panel (Flow-like)
        // Format: 38EU 5 Prs [A-1]
        panel.add(lbSize);

        // Separator
        JLabel sep = new JLabel("|");
        sep.setForeground(Color.GRAY);
        panel.add(sep);

        panel.add(lbStock);

        if (!loc.isEmpty()) {
            panel.add(lbLoc);
        }

        return panel;
    }
}
