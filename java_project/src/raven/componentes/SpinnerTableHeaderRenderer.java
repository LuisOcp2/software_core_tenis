package raven.componentes;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.UIManager;

public class SpinnerTableHeaderRenderer extends JSpinner implements TableCellRenderer {

    private final JTable table;
    private final int column;
    private boolean updating = false;

    public SpinnerTableHeaderRenderer(JTable table, int column) {
        this.table = table;
        this.column = column;
        init();
    }

    private void init() {
        setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        putClientProperty(FlatClientProperties.STYLE, "background:$Table.background");
        
        // Actualizar filas cuando el spinner cambie
        addChangeListener(e -> {
            if (!updating) {
                int value = (int) getValue();
                updateAllRows(value);
            }
        });

        // Manejar clicks en el header para los botones del spinner
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == column) {
                    Rectangle spinnerRect = table.getTableHeader().getHeaderRect(col);
                    Rectangle upButton = ((JSpinner.DefaultEditor) getEditor()).getComponents()[0].getBounds();
                    Rectangle downButton = ((JSpinner.DefaultEditor) getEditor()).getComponents()[1].getBounds();
                    
                    // Ajustar coordenadas relativas al header
                    upButton.translate(spinnerRect.x, spinnerRect.y);
                    downButton.translate(spinnerRect.x, spinnerRect.y);
                    
                    if (upButton.contains(e.getPoint())) {
                        setValue((int) getValue() + 1);
                    } else if (downButton.contains(e.getPoint())) {
                        int newValue = (int) getValue() - 1;
                        if (newValue >= 1) {
                            setValue(newValue);
                        }
                    }
                }
            }
        });

        // Escuchar cambios en el modelo de la tabla
        table.getModel().addTableModelListener(e -> {
            if (e.getColumn() == column || e.getType() == TableModelEvent.DELETE) {
                checkAndUpdateSpinner();
            }
        });
    }

    private void updateAllRows(int value) {
        for (int i = 0; i < table.getRowCount(); i++) {
            table.setValueAt(value, i, column);
        }
    }

    private void checkAndUpdateSpinner() {
        if (table.getRowCount() == 0) {
            return;
        }

        int firstValue = (int) table.getValueAt(0, column);
        boolean allSame = true;

        for (int i = 1; i < table.getRowCount(); i++) {
            if ((int) table.getValueAt(i, column) != firstValue) {
                allSame = false;
                break;
            }
        }

        updating = true;
        if (allSame) {
            setValue(firstValue);
        }
        updating = false;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(UIManager.getColor("TableHeader.bottomSeparatorColor"));
        float size = UIScale.scale(1f);
        g2.fill(new Rectangle2D.Float(0, getHeight() - size, getWidth(), size));
        g2.dispose();
    }
}