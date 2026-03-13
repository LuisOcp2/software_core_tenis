package raven.cell;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class TableActionCellDevolucionRender extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        Component com = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        PanelActionDevolucion action = new PanelActionDevolucion();

        if (isSelected) {
            action.setBackground(table.getSelectionBackground());
        } else {
            // Usar el color de background de la tabla para filas alternas o normal
            action.setBackground(table.getBackground());
        }

        // Obtener estado para mostrar/ocultar botones
        String estado = "";
        try {
            int colEstado = 4; // Ajustaremos esto dinámicamente si es posible
            if (table.getColumnCount() > colEstado) {
                Object val = table.getValueAt(row, colEstado);
                if (val != null)
                    estado = val.toString().toLowerCase();
            }
        } catch (Exception e) {
        }

        // Solo mostrar autorizar/rechazar si está pendiente
        action.mostrarBotonesAutorizacion("pendiente".equals(estado));

        // Mostrar anular si está aprobada o finalizada
        action.mostrarBotonAnular("aprobada".equals(estado) || "finalizada".equals(estado));

        return action;
    }
}
