package raven.componentes;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
/**
 *
 * @author RAVEN
 */
public class CheckBoxTableHeaderRenderer extends JCheckBox implements TableCellRenderer {
    private final JTable table;
    private final int column;
    private String label;
    
    public CheckBoxTableHeaderRenderer(JTable table, int column) {
        this.table = table;
        this.column = column;
        this.label = "seleccionar todos";
        init();
    }
    
    private void init() {
        putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Table.background");
        setHorizontalAlignment(SwingConstants.CENTER);
        setText(label);
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isLeftMouseButton(me)) {
                    int col = table.columnAtPoint(me.getPoint());
                    if (col == column) {
                        putClientProperty(FlatClientProperties.SELECTED_STATE, null);
                        setSelected(!isSelected());
                        selectedTableRow(isSelected());
                    }
                }
            }
        });
        table.getModel().addTableModelListener((tme) -> {
            if (tme.getColumn() == column || tme.getType() == TableModelEvent.DELETE) {
                checkRow();
            }
        });
    }
    
    private void checkRow() {
        // Evitar errores cuando la tabla está vacía o la columna no existe
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        if (rowCount <= 0 || column < 0 || column >= columnCount) {
            putClientProperty(FlatClientProperties.SELECTED_STATE, null);
            setSelected(false);
            table.getTableHeader().repaint();
            return;
        }

        boolean initValue = false;
        try {
            Object value = table.getValueAt(0, column);
            initValue = (value instanceof Boolean) ? (Boolean) value : false;
        } catch (RuntimeException ex) {
            // Si el modelo no está listo aún (por ejemplo tras setRowCount(0)), salir de forma segura
            putClientProperty(FlatClientProperties.SELECTED_STATE, null);
            setSelected(false);
            table.getTableHeader().repaint();
            return;
        }

        for (int i = 1; i < rowCount; i++) {
            boolean v = false;
            try {
                Object value = table.getValueAt(i, column);
                v = (value instanceof Boolean) ? (Boolean) value : false;
            } catch (RuntimeException ex) {
                // Si alguna fila no es accesible, marcar estado indeterminado y salir
                putClientProperty(FlatClientProperties.SELECTED_STATE, FlatClientProperties.SELECTED_STATE_INDETERMINATE);
                table.getTableHeader().repaint();
                return;
            }
            if (initValue != v) {
                putClientProperty(FlatClientProperties.SELECTED_STATE, FlatClientProperties.SELECTED_STATE_INDETERMINATE);
                table.getTableHeader().repaint();
                return;
            }
        }
        putClientProperty(FlatClientProperties.SELECTED_STATE, null);
        setSelected(initValue);
        table.getTableHeader().repaint();
    }
    
    private void selectedTableRow(boolean selected) {
        for (int i = 0; i < table.getRowCount(); i++) {
            table.setValueAt(selected, i, column);
        }
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
        return this;
    }
    
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setColor(UIManager.getColor("TableHeader.bottomSeparatorColor"));
        float size = UIScale.scale(1f);
        g2.fill(new Rectangle2D.Float(0, getHeight() - size, getWidth(), size));
        g2.dispose();
        super.paintComponent(grphcs);
    }
    
    public void setLabel(String label) {
        this.label = label;
        setText(label);
    }
    
    public String getLabel() {
        return label;
    }
}