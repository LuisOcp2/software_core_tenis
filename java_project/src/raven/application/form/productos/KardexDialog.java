package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.application.form.productos.dto.MovimientoItem;
import raven.dao.InventarioMovimientosDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class KardexDialog extends JDialog {

    private final int idVariante;
    private final String tituloProducto;
    private JTable table;
    private InventarioMovimientosDAO movimientosDAO;

    public KardexDialog(Frame owner, int idVariante, String tituloProducto) {
        super(owner, "Historial de Movimientos (Kardex)", true);
        this.idVariante = idVariante;
        this.tituloProducto = tituloProducto;
        this.movimientosDAO = new InventarioMovimientosDAO();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[top]10[top]10[grow]10[bottom]"));
        setSize(950, 600);
        setLocationRelativeTo(getOwner());

        // Header
        JLabel lbTitle = new JLabel("Kardex: " + tituloProducto);
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        add(lbTitle, "wrap");

        // Summary Panel
        JPanel panelResumen = new JPanel(new MigLayout("fillx, insets 10", "[grow]10[grow]10[grow]", "[]"));
        panelResumen.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:$Panel.background");

        lblTotalEntradas = createStatLabel("Total Entradas", "0", new Color(46, 204, 113));
        lblTotalSalidas = createStatLabel("Total Salidas", "0", new Color(231, 76, 60));
        lblSaldo = createStatLabel("Saldo Periodo", "0", new Color(52, 152, 219));

        panelResumen.add(lblTotalEntradas, "grow");
        panelResumen.add(lblTotalSalidas, "grow");
        panelResumen.add(lblSaldo, "grow");
        add(panelResumen, "growx, wrap");

        // Table
        String[] columns = { "Fecha", "Evento", "Ref.", "Entrada", "Salida", "Saldo", "Usuario", "Observación" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setReorderingAllowed(false);

        // Custom Renderer
        // Custom Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Reset colors
                if (!isSelected) {
                    c.setForeground(table.getForeground());
                    c.setBackground(table.getBackground());
                }

                try {
                    String evento = (String) table.getValueAt(row, 1);
                    if (evento != null && !isSelected) {
                        boolean isEntrada = evento.toLowerCase().contains("entrada") ||
                                evento.toLowerCase().contains("compra") ||
                                evento.toLowerCase().contains("devolucion");
                        boolean isSalida = evento.toLowerCase().contains("salida") ||
                                evento.toLowerCase().contains("venta");

                        // Highlight specific columns (Evento, Entrada, Salida)
                        if (column == 1 || column == 3 || column == 4) {
                            if (isEntrada) {
                                c.setForeground(new Color(46, 204, 113)); // Green
                            } else if (isSalida) {
                                c.setForeground(new Color(231, 76, 60)); // Red
                            }
                        }
                    }
                } catch (Exception e) {
                }

                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(130); // Fecha
        table.getColumnModel().getColumn(1).setPreferredWidth(120); // Evento
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Ref
        table.getColumnModel().getColumn(3).setPreferredWidth(80); // Entrada
        table.getColumnModel().getColumn(4).setPreferredWidth(80); // Salida
        table.getColumnModel().getColumn(5).setPreferredWidth(80); // Saldo
        table.getColumnModel().getColumn(6).setPreferredWidth(120); // Usuario
        table.getColumnModel().getColumn(7).setPreferredWidth(200); // Obs

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, "grow, wrap");

        // Footer
        JButton btnClose = new JButton("Cerrar");
        btnClose.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        btnClose.addActionListener(e -> dispose());
        add(btnClose, "right, tag ok, h 40!");
    }

    private JPanel lblTotalEntradas;
    private JPanel lblTotalSalidas;
    private JPanel lblSaldo;

    private JPanel createStatLabel(String title, String value, Color color) {
        JPanel p = new JPanel(new MigLayout("insets 5", "[]push[]"));
        p.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:darken($Panel.background, 5%)");
        JLabel t = new JLabel(title);
        t.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        JLabel v = new JLabel(value);
        v.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        v.setForeground(color);
        p.add(t);
        p.add(v);
        return p;
    }

    private void loadData() {
        new Thread(() -> {
            List<MovimientoItem> list = movimientosDAO.listarPorVariante(idVariante);
            SwingUtilities.invokeLater(() -> {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                int totalEntradas = 0;
                int totalSalidas = 0;
                int saldoAcumulado = 0; // Needed starting balance usually, but let's assume 0 relative or calculate
                                        // backwards?
                // Better to just sum

                // Reverse iterate if we want to calculate running balance from start?
                // Or just display movements. Usually Kardex runs from old to new.
                // The DAO returns DESC (newest first). Let's flip it for calculation or handle
                // it.
                // A correct Kardex needs initial stock. For now, let's just show +/- delta.

                // Let's sort list by Date ASC for proper Kardex calculation
                list.sort((o1, o2) -> o1.getFecha().compareTo(o2.getFecha()));

                for (MovimientoItem item : list) {
                    boolean isEntrada = item.isEntrada();
                    boolean isSalida = item.isSalida();

                    int cantidad = item.getCantidad();

                    int entrada = 0;
                    int salida = 0;

                    if (isEntrada) {
                        entrada = cantidad;
                        totalEntradas += cantidad;
                        saldoAcumulado += cantidad;
                    } else if (isSalida) {
                        salida = cantidad;
                        totalSalidas += cantidad;
                        saldoAcumulado -= cantidad;
                    } else {
                        // Adjustment or neutral?
                        if (item.getTipoMovimiento().contains("entrada")) {
                            entrada = cantidad;
                            totalEntradas += cantidad;
                            saldoAcumulado += cantidad;
                        } else {
                            salida = cantidad;
                            totalSalidas += cantidad;
                            saldoAcumulado -= cantidad;
                        }
                    }

                    model.addRow(new Object[] {
                            sdf.format(item.getFecha()),
                            item.getTipoReferencia().toUpperCase() + " (" + item.getTipoMovimiento() + ")",
                            item.getTipoReferencia(),
                            entrada > 0 ? "+" + entrada : "-",
                            salida > 0 ? "-" + salida : "-",
                            saldoAcumulado,
                            item.getUsuario(),
                            item.getObservacion()
                    });
                }

                // Reverse table to show newest first again? No, Kardex usually reads top-down
                // chronological.
                // But user might want newest on top. If newest on top, balance calculation is
                // tricky visually.
                // Let's keep chronological (Oldest -> Newest) so balance makes sense.

                // Update stats
                // Note: The lblSaldo here is "Saldo del Periodo" (Movement), not Abs Stock.
                // To get Absolute Stock we need specific query.
                // But the user asked for "Totals".
                ((JLabel) lblTotalEntradas.getComponent(1)).setText("+" + totalEntradas);
                ((JLabel) lblTotalSalidas.getComponent(1)).setText("-" + totalSalidas);

                int net = totalEntradas - totalSalidas;
                ((JLabel) lblSaldo.getComponent(1)).setText((net >= 0 ? "+" : "") + net);
            });
        }).start();
    }
}
