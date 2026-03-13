package raven.cell;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;

public class TableActionCellDevolucionEditor extends DefaultCellEditor {

    private final TableActionDevolucionEvent event;

    public TableActionCellDevolucionEditor(TableActionDevolucionEvent event) {
        super(new JCheckBox());
        this.event = event;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        PanelActionDevolucion action = new PanelActionDevolucion();
        action.initEvent(event, row);
        action.setBackground(table.getSelectionBackground());
        // Asegurar que el panel sea opaco para pintar el fondo
        action.setOpaque(true);

        // Obtener estado para mostrar/ocultar botones
        // Asumiendo que la columna de estado es la 4 (indice base 0)
        // Ajustar según estructura real en devolucionMainForm
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
