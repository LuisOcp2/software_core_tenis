package raven.application.form.productos;

import raven.application.form.productos.dto.InventarioDetalleItem;
import raven.application.form.productos.dto.ProductoAgrupado;
import net.miginfocom.swing.MigLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ProductoCardRenderer extends JPanel implements TableCellRenderer {

    private final JLabel lbTitle;
    private final JLabel lbCode;
    private final JLabel lbCategory;
    private final JLabel lbColorTalla;
    private final JLabel lbIcon;

    public ProductoCardRenderer() {
        // [70!] Fixed width for image column, 10px gap, [grow] for text info
        setLayout(new MigLayout("fill, insets 5", "[70!]10[grow]", "[]4[]2[]"));
        setOpaque(true);

        lbIcon = new JLabel();
        lbIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lbTitle = new JLabel();
        lbTitle.setFont(lbTitle.getFont().deriveFont(Font.BOLD, 15f));

        lbCode = new JLabel();
        lbCode.setFont(lbCode.getFont().deriveFont(12f));

        lbCategory = new JLabel();
        lbCategory.setFont(lbCode.getFont().deriveFont(Font.BOLD, 11f));
        lbCategory.setForeground(Color.WHITE);
        lbCategory.setBackground(new Color(0, 122, 204)); // Solid Blue Badge
        lbCategory.setOpaque(true);

        lbColorTalla = new JLabel();
        lbColorTalla.setFont(lbCode.getFont().deriveFont(Font.BOLD, 13f));

        // Add Icon in the first column, spanning 3 rows
        add(lbIcon, "cell 0 0 1 3, grow");

        // Add Text components in the second column
        add(lbTitle, "cell 1 0, wrap");
        add(lbCode, "cell 1 1, split 3");
        add(new JLabel(" • "), "cell 1 1, gapleft 5, gapright 5, aligny center");
        add(lbCategory, "cell 1 1, wrap");
        add(lbColorTalla, "cell 1 2, gaptop 2");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            lbTitle.setForeground(table.getSelectionForeground());
            lbCode.setForeground(table.getSelectionForeground());
            lbCategory.setBackground(table.getSelectionForeground());
            lbCategory.setForeground(table.getSelectionBackground());
            lbColorTalla.setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            lbTitle.setForeground(table.getForeground());
            lbCode.setForeground(new Color(200, 200, 200));
            lbCategory.setBackground(new Color(0, 122, 204));
            lbCategory.setForeground(Color.WHITE);
            lbColorTalla.setForeground(new Color(220, 220, 220));
        }

        if (value instanceof InventarioDetalleItem) {
            InventarioDetalleItem item = (InventarioDetalleItem) value;
            setCardData(item.getNombreProducto(), item.getCodigoModelo(), item.getCategoria(),
                    item.getNombreColor(), item.getNombreTalla(), item.getCachedIcon(), isSelected);
        } else if (value instanceof ProductoAgrupado) {
            ProductoAgrupado item = (ProductoAgrupado) value;
            setCardData(item.getNombreProducto(), item.getCodigoModelo(), item.getCategory(),
                    item.getNombreColor(), null, item.getCachedIcon(), isSelected);
        } else if (value instanceof raven.controlador.productos.ModelProduct) {
            raven.controlador.productos.ModelProduct item = (raven.controlador.productos.ModelProduct) value;
            String catName = (item.getCategory() != null) ? item.getCategory().getName() : "";
            // Si el nombre de la variante ya incluye el nombre del producto, usarlo tal
            // cual, sino componerlo si es necesario.
            // Para ModelProduct generalmente name es el nombre base.
            setCardData(item.getName(), item.getBarcode(), catName,
                    item.getColor(), item.getSize(), item.getCachedIcon(), isSelected);
        } else {
            lbTitle.setText(value != null ? value.toString() : "");
            lbCode.setText("");
            lbCategory.setText("");
            lbColorTalla.setText("");
            lbIcon.setIcon(null);
        }

        return this;
    }

    private void setCardData(String nombre, String codigo, String cat, String color, String talla,
            javax.swing.Icon icon, boolean isSelected) {
        lbTitle.setText(nombre);
        lbCode.setText(codigo);
        lbCategory.setText(" " + (cat != null ? cat.toUpperCase() : "") + " ");

        // Set Icon directly
        lbIcon.setIcon(icon);

        String colorHex = isSelected ? "#FFFFFF" : "#FFA500";
        String pipeHex = isSelected ? "#FFFFFF" : "#808080";

        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<font color='").append(colorHex).append("'>Color:</font> <b>").append(color).append("</b>");

        if (talla != null && !talla.isEmpty()) {
            String tallaHex = isSelected ? "#FFFFFF" : "#00FFFF";
            sb.append("  <font color='").append(pipeHex).append("'>|</font>  ");
            sb.append("<font color='").append(tallaHex).append("'>Talla:</font> <b>").append(talla).append("</b>");
        }
        sb.append("</html>");

        lbColorTalla.setText(sb.toString());
    }
}
