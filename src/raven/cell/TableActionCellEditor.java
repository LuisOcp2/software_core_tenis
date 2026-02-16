package raven.cell;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class TableActionCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final int ICON_SIZE = 14;
    private static final Dimension BUTTON_SIZE = new Dimension(28, 28);
    private static final String STYLE_ICON_BUTTON = "arc:999;focusWidth:0;borderWidth:0;background:#00000000;"
            + "hoverBackground:#E9EEF6;pressedBackground:#D6E4FF";

    private static final Color COLOR_VIEW = new Color(33, 150, 243);
    private static final Color COLOR_EDIT = new Color(52, 152, 219);
    private static final Color COLOR_FACTURAR = new Color(46, 204, 113);
    private static final Color COLOR_CAJA = new Color(243, 156, 18);
    private static final Color COLOR_DELETE = new Color(231, 76, 60);
    private static final Color COLOR_CANCEL = new Color(192, 57, 43);

    private final JPanel panel;
    private final JButton editBtn;
    private final JButton deleteBtn;
    private final JButton viewBtn;
    private final JButton cajaBtn;
    private final JButton cancelBtn;
    private TableActionEvent event;
    private int currentRow;

    public TableActionCellEditor(TableActionEvent event) {
        this.event = event;
        panel = new JPanel(new MigLayout("insets 0, gap 4, alignx center", "[center]"));
        panel.setOpaque(true);

        editBtn = new JButton();
        deleteBtn = new JButton();
        viewBtn = new JButton();
        cajaBtn = new JButton();
        cancelBtn = new JButton();
        
        configurarBoton(viewBtn, FontAwesomeSolid.EYE, COLOR_VIEW, "Ver detalle");
        configurarBoton(editBtn, FontAwesomeSolid.EDIT, COLOR_EDIT, "Editar");
        configurarBoton(deleteBtn, FontAwesomeSolid.TRASH_ALT, COLOR_DELETE, "Cancelar (pendiente)");
        configurarBoton(cajaBtn, FontAwesomeSolid.BOX_OPEN, COLOR_CAJA, "Convertir caja a pares");
        configurarBoton(cancelBtn, FontAwesomeSolid.TIMES_CIRCLE, COLOR_CANCEL, "Cancelar en tránsito (admin)");

        editBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                event.onEdit(currentRow);
            }
        });

        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                event.onDelete(currentRow);
            }
        });

        viewBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                event.onView(currentRow);
            }
        });

        cajaBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                event.onCaja(currentRow);
            }
        });

        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                event.onCancel(currentRow);
            }
        });
    }

    private static boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String s = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "si".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    private void configurarBoton(JButton button, FontAwesomeSolid icon, Color color, String tooltip) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty(FlatClientProperties.STYLE, STYLE_ICON_BUTTON);
        button.setIcon(FontIcon.of(icon, ICON_SIZE, color));
        button.setToolTipText(tooltip);
        button.setPreferredSize(BUTTON_SIZE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        button.setFocusable(false);
    }

    private static String obtenerEstado(JTable table, int row) {
        try {
            int estadoCol = -1;
            for (int c = 0; c < table.getColumnCount(); c++) {
                String name = table.getColumnName(c);
                if (name != null && name.toLowerCase().contains("estado")) {
                    estadoCol = c;
                    break;
                }
            }
            if (estadoCol < 0) {
                estadoCol = Math.min(4, table.getColumnCount() - 1);
            }
            Object v = table.getValueAt(row, estadoCol);
            return v != null ? String.valueOf(v).trim() : "";
        } catch (Exception ignore) {
            return "";
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentRow = row;
        panel.removeAll();

        String estado = obtenerEstado(table, row);
        panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

        boolean isAdmin = false;
        try {
            isAdmin = raven.clases.admin.UserSession.getInstance().hasRole("admin");
        } catch (Exception e) {
        }

        boolean hasCajaConvertible = false;
        try {
            int modelRow = table.convertRowIndexToModel(row);
            if (table.getModel() != null && table.getModel().getColumnCount() > 7) {
                hasCajaConvertible = isTruthy(table.getModel().getValueAt(modelRow, 7));
            }
        } catch (Exception ignore) {
        }

        boolean recibido = "recibido".equalsIgnoreCase(estado);
        boolean editable = "pendiente".equalsIgnoreCase(estado) || "autorizado".equalsIgnoreCase(estado);
        boolean isRecibidosView = false;
        try {
            Object dir = table.getClientProperty("traspasos.filtroDireccion");
            isRecibidosView = dir != null && "RECIBIDO".equalsIgnoreCase(String.valueOf(dir));
        } catch (Exception ignore) {
        }
        boolean canFacturar = recibido && isRecibidosView;

        if (canFacturar) {
            editBtn.setIcon(FontIcon.of(FontAwesomeSolid.FILE_INVOICE_DOLLAR, ICON_SIZE, COLOR_FACTURAR));
            editBtn.setToolTipText("Facturar / procesar traspaso");
        } else {
            editBtn.setIcon(FontIcon.of(FontAwesomeSolid.EDIT, ICON_SIZE, COLOR_EDIT));
            editBtn.setToolTipText("Editar");
        }

        panel.add(viewBtn, "w 28!, h 28!");
        if (editable || canFacturar) {
            panel.add(editBtn, "w 28!, h 28!");
        }

        if ("pendiente".equalsIgnoreCase(estado)) {
            panel.add(deleteBtn, "w 28!, h 28!");
        }

        if (recibido && hasCajaConvertible) {
            panel.add(cajaBtn, "w 28!, h 28!");
        }

        boolean showAdminCancel = isAdmin
                && !"cancelado".equalsIgnoreCase(estado)
                && ("en_transito".equalsIgnoreCase(estado)
                    || "autorizado".equalsIgnoreCase(estado)
                    || "recibido".equalsIgnoreCase(estado));
        if (showAdminCancel) {
            panel.add(cancelBtn, "w 28!, h 28!");
        }

        panel.revalidate();
        panel.repaint();
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return "";
    }
}
