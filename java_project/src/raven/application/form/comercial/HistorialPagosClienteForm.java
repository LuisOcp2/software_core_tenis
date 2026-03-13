package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import raven.clases.comercial.ServiceCuentasPorCobrar;
import raven.clases.principal.MoneyFormatter;
import raven.componentes.TableHeaderAlignment;

/**
 * Panel para ver el historial de pagos de un cliente
 */
public class HistorialPagosClienteForm extends javax.swing.JPanel {

    private final ServiceCuentasPorCobrar service = new ServiceCuentasPorCobrar();

    public HistorialPagosClienteForm() {
        initComponents();
        init();
    }

    private void init() {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;"
                + "font:bold;");

        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:50;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        // Align Columns
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Monto
    }

    public void loadData(int idVenta) {
        try {
            List<Object[]> history = service.obtenerHistorialPagosPorVenta(idVenta);
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            for (Object[] row : history) {
                model.addRow(new Object[] {
                        row[0], // ID Venta
                        df.format(row[1]), // Fecha
                        row[2], // Tipo Pago
                        MoneyFormatter.format((Double) row[3]), // Monto
                        row[4] // Observaciones
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Venta #", "Fecha Pago", "Método", "Monto Abonado", "Observaciones"
                }) {
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                                .addContainerGap()));
    }

    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
}
