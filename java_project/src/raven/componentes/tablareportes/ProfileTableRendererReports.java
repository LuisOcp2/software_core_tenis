package raven.componentes.tablareportes;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.util.Map;
import raven.controlador.productos.ModelProduct;

/**
 *
 * @author RAVEN
 */
public class ProfileTableRendererReports implements TableCellRenderer {

    private final TableCellRenderer oldCellRenderer;

    public ProfileTableRendererReports(JTable table) {
        oldCellRenderer = table.getDefaultRenderer(Object.class);
    }

    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
        Component com = oldCellRenderer.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);

        // Check the type of the object and create appropriate cell renderer
        if (o instanceof ModelProduct) {
            // Si es un ModelProduct, usar directamente
            ModelProduct product = (ModelProduct) o;
            TableCellReportes cell = new TableCellReportes(product, com.getFont());
            cell.setBackground(com.getBackground());
            return cell;
        } else if (o instanceof Map) {
            // Si es un Map (de la base de datos), convertir a formato compatible
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) o;
            TableCellReportes cell = new TableCellReportes(data, com.getFont());
            cell.setBackground(com.getBackground());
            return cell;
        } else {
            // Si no es ninguno de los tipos esperados, usar el renderer original
            return com;
        }
    }
}
