package raven.application.form.reportes;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.utils.ExportadorReportes;

/**
 * Diálogo para ver detalles de un conteo de inventario
 */
public class DetalleConteoDialog extends JDialog {

    private JTable table;
    private DefaultTableModel model;
    private final List<Map<String, Object>> detalles;
    private final String tituloConteo;

    public DetalleConteoDialog(java.awt.Window parent, String tituloConteo, List<Map<String, Object>> detalles) {
        super(parent, "Detalles del Conteo", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.tituloConteo = tituloConteo;
        this.detalles = detalles;
        initComponents();
        cargarDatos();
        setSize(900, 600);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Detalles: " + tituloConteo);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Table
        String[] columns = { "Producto", "SKU", "Marca", "Categoría", "Talla", "Color", "Ubicación", "Sistema",
                "Contado", "Diferencia", "Estado" };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:25;showHorizontalLines:true;showVerticalLines:true");
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold");

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200); // Producto
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // SKU

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Footer / Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnExcel = new JButton("Excel");
        btnExcel.setIcon(FontIcon.of(FontAwesomeSolid.FILE_EXCEL, 16, Color.WHITE));
        btnExcel.putClientProperty(FlatClientProperties.STYLE, "background:#217346;foreground:#fff;font:bold");
        btnExcel.addActionListener(
                e -> ExportadorReportes.exportarExcel(table, "Detalle_Conteo_" + tituloConteo.replace(" ", "_")));

        JButton btnPdf = new JButton("PDF");
        btnPdf.setIcon(FontIcon.of(FontAwesomeSolid.FILE_PDF, 16, Color.WHITE));
        btnPdf.putClientProperty(FlatClientProperties.STYLE, "background:#c81e1e;foreground:#fff;font:bold");
        btnPdf.addActionListener(e -> ExportadorReportes.exportarPDF(table, "Detalle Conteo: " + tituloConteo));

        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dispose());

        footer.add(btnExcel);
        footer.add(btnPdf);
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        model.setRowCount(0);
        for (Map<String, Object> row : detalles) {
            model.addRow(new Object[] {
                    row.get("producto"),
                    row.get("sku"),
                    row.get("marca"),
                    row.get("categoria"),
                    row.get("talla"),
                    row.get("color"),
                    row.get("ubicacion"),
                    row.get("stock_sistema"),
                    row.get("stock_contado"),
                    row.get("diferencia"),
                    row.get("estado")
            });
        }
    }
}
