package raven.application.form.comercial.compras;

import raven.clases.principal.ServiceCompra;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Component;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.AbstractCellEditor;

/**
 * Diálogo para listar los abonos de una compra con evidencias
 * .
 */
public class HistorialAbonosDialog extends JDialog {

    private final ServiceCompra serviceCompra;
    private final int idCompra;
    private final DefaultTableModel model;
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private List<ServiceCompra.AbonoView> data;

    public HistorialAbonosDialog(JFrame parent, int idCompra, ServiceCompra serviceCompra) {
        super(parent, "Abonos de la compra #" + idCompra, true);
        this.serviceCompra = serviceCompra;
        this.idCompra = idCompra;
        this.model = new DefaultTableModel(new String[]{
                "Fecha", "Monto", "Medio", "Comprobante", "Estado", "Evidencia"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5;
            }
        };

        initUI();
        cargarAbonos();
        setMinimumSize(new Dimension(840, 420));
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(table));
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(moneyRenderer);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void cargarAbonos() {
        try {
            model.setRowCount(0);
            data = serviceCompra.listarAbonosCompra(idCompra);
            for (ServiceCompra.AbonoView a : data) {
                model.addRow(new Object[]{
                        a.fechaAbono != null ? sdf.format(a.fechaAbono) : "",
                        formatMoney(a.monto),
                        a.medioPago,
                        a.numeroComprobante,
                        a.estado != null ? a.estado.toUpperCase() : "",
                        "Ver"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando abonos: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatMoney(BigDecimal value) {
        return value != null ? money.format(value) : "";
    }

    private void abrirEvidencia(int row) {
        if (data == null || row < 0 || row >= data.size()) return;
        ServiceCompra.AbonoView a = data.get(row);
        try {
            if (a.evidenciaBytes != null && a.evidenciaBytes.length > 0) {
                String ext = extFromMime(a.evidenciaMime);
                File tmp = File.createTempFile("abono-" + a.idAbono, ext);
                java.nio.file.Files.write(tmp.toPath(), a.evidenciaBytes);
                Desktop.getDesktop().open(tmp);
            } else if (a.evidenciaUrl != null && !a.evidenciaUrl.isBlank()) {
                Desktop.getDesktop().browse(new URI(a.evidenciaUrl));
            } else {
                JOptionPane.showMessageDialog(this, "Sin evidencia disponible para este abono.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir la evidencia: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extFromMime(String mime) {
        if (mime == null) return ".bin";
        String m = mime.toLowerCase();
        if (m.contains("png")) return ".png";
        if (m.contains("jpeg") || m.contains("jpg")) return ".jpg";
        if (m.contains("pdf")) return ".pdf";
        return ".bin";
    }
    
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setText("Ver");
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
    
    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Ver");
        private int currentRow = -1;
        public ButtonEditor(JTable table) {
            button.addActionListener(e -> {
                abrirEvidencia(currentRow);
                fireEditingStopped();
            });
        }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            return button;
        }
        @Override
        public Object getCellEditorValue() { return null; }
    }
}
