package raven.application.form.productos;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.text.NumberFormat;
import java.util.Locale;
import com.formdev.flatlaf.FlatClientProperties;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelProductVariant;
import raven.clases.admin.ServiceBodegas;
import raven.controlador.admin.ModelBodegas;
import raven.utils.ProductCacheManager;
import raven.clases.productos.VariantPaginationAdapter;
import raven.clases.comun.GenericPaginationService;
import raven.modal.Toast;

public class VariantesDisponiblesDialog extends JDialog {
    private final ModelProduct product;
    private final Integer warehouseId;
    private JTable table;
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private javax.swing.JLabel imageLabel;
    private int headerIconSize =80;

    public VariantesDisponiblesDialog(java.awt.Window owner, ModelProduct product, Integer warehouseId) {
        super(owner, "Variantes disponibles", ModalityType.APPLICATION_MODAL);
        this.product = product;
        this.warehouseId = warehouseId;
        setLayout(new BorderLayout(10, 10));
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(103, 80, 164), getWidth(), 0, new Color(3, 218, 198));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.putClientProperty(FlatClientProperties.STYLE, "arc:25");

        JLabel title = new JLabel(product.getName());
        title.setForeground(Color.WHITE);
        title.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");

        JLabel subtitle = new JLabel(product.getModelCode() != null ? product.getModelCode() : "");
        subtitle.setForeground(new Color(230, 230, 230));
        subtitle.putClientProperty(FlatClientProperties.STYLE, "font:$h3.font");

        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);
        imageLabel = new javax.swing.JLabel();
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(0,0,0,40));
        imageLabel.setBorder(new EmptyBorder(6,6,6,6));
        imageLabel.putClientProperty(FlatClientProperties.STYLE, "arc:16");
        try {
            javax.swing.ImageIcon icon = product.getProfile() != null ? product.getProfile().getResizedImageIcon(headerIconSize, headerIconSize) : null;
            if (icon != null) imageLabel.setIcon(icon);
        } catch (Exception ignore) {}

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        left.setOpaque(false);
        left.add(imageLabel);
        left.add(titleBox);
        header.add(left, BorderLayout.WEST);

        setHeaderIconSize(headerIconSize);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        chips.setOpaque(false);

        JLabel chipMarca = createChip(product.getBrand() != null ? product.getBrand().getName() : "");
        JLabel chipGenero = createChip(product.getGender() != null ? product.getGender() : "");
        JLabel chipBodega = createChip(resolveWarehouseName());
        String precioTxt;
        try {
            double p = product.getSalePrice();
            if (p <= 0 && product.getVariants() != null && !product.getVariants().isEmpty()) {
                p = product.getVariants().get(0).getSalePrice();
            }
            precioTxt = "Precio " + money.format(p);
        } catch (Exception ex) {
            precioTxt = "Precio";
        }
        JLabel chipPrecio = createChipAccent(precioTxt, new Color(0, 150, 136));
        chips.add(chipMarca);
        chips.add(chipGenero);
        chips.add(chipBodega);
        chips.add(chipPrecio);
        header.add(chips, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        table = new JTable(new DefaultTableModel(new Object[]{"COLOR", "TALLA", "STOCK", "EAN", "STOCK CAJA", "ESTADO"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        });

        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:30;font:bold;separatorColor:$TableHeader.background");
        table.putClientProperty(FlatClientProperties.STYLE, "rowHeight:48;showHorizontalLines:true;intercellSpacing:0,1;cellFocusColor:$TableHeader.hoverBackground;selectionBackground:$TableHeader.hoverBackground;selectionForeground:$Table.foreground");

        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Cerrar");
        actions.add(close);
        add(actions, BorderLayout.SOUTH);
        close.addActionListener(e -> dispose());
        fillVariants();
        configureRenderers();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 3) {
                    java.awt.Rectangle rect = table.getCellRect(row, col, true);
                    int iconLeft = rect.x + rect.width - 24;
                    if (e.getX() >= iconLeft) {
                        StringSelection sel = new StringSelection(String.valueOf(table.getValueAt(row, col)));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                        try { Toast.show(VariantesDisponiblesDialog.this, Toast.Type.INFO, "Copiado"); } catch (Exception ignore) {}
                    }
                }
            }
        });
        setSize(980, 560);
        setLocationRelativeTo(owner);
    }

    private void fillVariants() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String sql = "SELECT pv.ean, pv.sku, c.nombre AS color, CONCAT(t.numero, ' ', t.sistema) AS talla, " +
                     "COALESCE(ib.Stock_par,0) AS stock_par, COALESCE(ib.Stock_caja,0) AS stock_caja " +
                     "FROM producto_variantes pv " +
                     "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.id_bodega = ? AND ib.activo = 1 " +
                     "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                     "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                     "WHERE pv.id_producto = ? AND pv.disponible = 1 " +
                     "ORDER BY c.nombre, CAST(t.numero AS UNSIGNED)";
        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, warehouseId != null ? warehouseId : 0);
            ps.setInt(2, product.getProductId());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int stockPar = rs.getInt("stock_par");
                    int stockCaja = rs.getInt("stock_caja");
                    model.addRow(new Object[]{
                            rs.getString("color"),
                            rs.getString("talla"),
                            stockPar,
                            rs.getString("ean"),
                            stockCaja,
                            (stockPar > 0 || stockCaja > 0) ? "Disponible" : "Agotado"
                    });
                }
            }
        } catch (Exception ignore) {}
    }

    private void configureRenderers() {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(1).setCellRenderer(new ChipRenderer(new Color(33, 150, 243)));
        table.getColumnModel().getColumn(2).setCellRenderer(new StockRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new EanRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(new EstadoRenderer());

        table.getColumnModel().getColumn(0).setCellRenderer(new IconTextRenderer());
    }

    private JLabel createChip(String text) {
        JLabel lb = new JLabel(text);
        lb.setOpaque(true);
        lb.setBackground(new Color(53, 53, 53));
        lb.setForeground(Color.WHITE);
        lb.setBorder(new EmptyBorder(6, 12, 6, 12));
        lb.putClientProperty(FlatClientProperties.STYLE, "arc:20");
        return lb;
    }
    private JLabel createChipAccent(String text, Color bg) {
        JLabel lb = new JLabel(text);
        lb.setOpaque(true);
        lb.setBackground(bg);
        lb.setForeground(Color.WHITE);
        lb.setBorder(new EmptyBorder(6, 12, 6, 12));
        lb.putClientProperty(FlatClientProperties.STYLE, "arc:20;font:bold");
        return lb;
    }

    private String resolveWarehouseName() {
        if (warehouseId == null) return "Bodega";
        try {
            ServiceBodegas sb = new ServiceBodegas();
            ModelBodegas b = sb.obtenerPorId(warehouseId);
            if (b != null && b.getNombre() != null) return b.getNombre();
        } catch (Exception ignore) {}
        return "Bodega";
    }

    private class IconTextRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setIcon(FontIcon.of(FontAwesomeSolid.TSHIRT, 16, c.getForeground()));
            c.setBorder(new EmptyBorder(6, 12, 6, 12));
            return c;
        }
    }

    private class ChipRenderer extends DefaultTableCellRenderer {
        private final Color color;
        ChipRenderer(Color color) { this.color = color; }
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, String.valueOf(value), isSelected, hasFocus, row, column);
            c.setOpaque(true);
            c.setBackground(color);
            c.setForeground(Color.WHITE);
            c.setHorizontalAlignment(SwingConstants.CENTER);
            c.setBorder(new EmptyBorder(6, 12, 6, 12));
            c.putClientProperty(FlatClientProperties.STYLE, "arc:20");
            return c;
        }
    }

    private class StockRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int v = 0;
            try { v = Integer.parseInt(String.valueOf(value)); } catch (Exception ignore) {}
            Color bg = v == 0 ? new Color(183, 28, 28) : (v <= 4 ? new Color(255, 179, 0) : new Color(27, 94, 32));
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, String.valueOf(value), isSelected, hasFocus, row, column);
            c.setOpaque(true);
            c.setBackground(bg);
            c.setForeground(Color.WHITE);
            c.setHorizontalAlignment(SwingConstants.CENTER);
            c.setBorder(new EmptyBorder(6, 12, 6, 12));
            c.putClientProperty(FlatClientProperties.STYLE, "arc:20");
            return c;
        }
    }

    private class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String t = String.valueOf(value);
            Color bg = "Agotado".equalsIgnoreCase(t) ? new Color(183, 28, 28) : new Color(27, 94, 32);
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, t, isSelected, hasFocus, row, column);
            c.setOpaque(true);
            c.setBackground(bg);
            c.setForeground(Color.WHITE);
            c.setHorizontalAlignment(SwingConstants.CENTER);
            c.setBorder(new EmptyBorder(6, 12, 6, 12));
            c.putClientProperty(FlatClientProperties.STYLE, "arc:20");
            return c;
        }
    }

    private class MoneyRenderer extends DefaultTableCellRenderer {
        private final boolean accent;
        MoneyRenderer(boolean accent) { this.accent = accent; }
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String txt;
            try {
                double d = Double.parseDouble(String.valueOf(value));
                txt = money.format(d);
            } catch (Exception e) {
                txt = String.valueOf(value);
            }
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, txt, isSelected, hasFocus, row, column);
            if (accent) c.setForeground(new Color(0, 200, 83));
            c.setHorizontalAlignment(SwingConstants.LEFT);
            c.setBorder(new EmptyBorder(0, 12, 0, 12));
            return c;
        }
    }

    private class EanRenderer implements javax.swing.table.TableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new BorderLayout(6, 0));
            panel.setOpaque(true);
            DefaultTableCellRenderer base = new DefaultTableCellRenderer();
            JLabel lbl = (JLabel) base.getTableCellRendererComponent(table, String.valueOf(value), isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
            lbl.setBorder(new EmptyBorder(0, 12, 0, 0));
            JLabel copy = new JLabel(FontIcon.of(FontAwesomeSolid.COPY, 14, new Color(180, 180, 180)));
            copy.setToolTipText("Copiar EAN");
            copy.setBorder(new EmptyBorder(0, 0, 0, 8));
            copy.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    StringSelection sel = new StringSelection(String.valueOf(value));
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                    try { Toast.show(VariantesDisponiblesDialog.this, Toast.Type.INFO, "Copiado"); } catch (Exception ignore) {}
                }
            });
            panel.setBackground(lbl.getBackground());
            panel.add(lbl, BorderLayout.CENTER);
            panel.add(copy, BorderLayout.EAST);
            return panel;
        }
    }

    public void setHeaderIconSize(int size) {
        headerIconSize = Math.max(24, size);
        if (product != null && imageLabel != null && product.getProfile() != null) {
            try {
                javax.swing.ImageIcon icon = product.getProfile().getResizedImageIcon(headerIconSize, headerIconSize);
                imageLabel.setIcon(icon);
            } catch (Exception ignore) {}
        }
    }
}
