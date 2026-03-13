package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import raven.clases.principal.MoneyFormatter;
import raven.clases.principal.ServiceVenta;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.ModelDetalleVenta;
import raven.controlador.principal.ModelVenta;
import raven.modal.Toast;

/**
 * Panel para mostrar el detalle completo de una venta
 */
public class VerDetalleVentaPanel extends javax.swing.JPanel {

        private final ServiceVenta serviceVenta = new ServiceVenta();
        private int idVenta;

        public VerDetalleVentaPanel() {
                initComponents();
                init();
        }

        private void init() {
                FlatRobotoFont.install();
                UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

                // Estilos para el panel de información
                panelInfo.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:20;"
                                + "background:$Panel.background");

                // Estilos para la tabla
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

                // Alinear columnas
                table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));
                DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
                rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
                table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Cantidad
                table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Precio Unit.
                table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Subtotal
        }

        public void loadData(int idVenta) {
                this.idVenta = idVenta;
                try {
                        System.out.println("VerDetalleVentaPanel: Cargando venta ID: " + idVenta);

                        // Obtener venta completa con detalles
                        ModelVenta venta = serviceVenta.buscarVentaPorId(idVenta);

                        if (venta == null) {
                                Toast.show(this, Toast.Type.ERROR, "No se encontró la venta");
                                return;
                        }

                        // Mostrar información de la venta
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        lblIdVenta.setText("Venta #" + venta.getIdVenta());
                        lblFecha.setText("Fecha: " + venta.getFechaVenta().format(formatter));
                        lblCliente.setText("Cliente: " + venta.getCliente().getNombre());
                        String vendedor = venta.getUsuario() != null ? venta.getUsuario().getNombre(): "N/A";
                        lblVendedor.setText("Vendedor: " + vendedor);
                        lblTotal.setText("Total: " + MoneyFormatter.format(venta.getTotal()));
                        lblEstado.setText("Estado: " + venta.getEstado());

                        // Cargar detalles en la tabla
                        cargarDetallesVenta(venta.getDetalles());

                        System.out.println("VerDetalleVentaPanel: Venta cargada correctamente");
                } catch (Exception e) {
                        System.err.println("VerDetalleVentaPanel: Error al cargar venta");
                        e.printStackTrace();
                        javax.swing.SwingUtilities.invokeLater(() -> {
                                Toast.show(this, Toast.Type.ERROR, "Error al cargar venta: " + e.getMessage());
                        });
                }
        }

        private void cargarDetallesVenta(List<ModelDetalleVenta> detalles) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);

                for (ModelDetalleVenta detalle : detalles) {
                        String nombreProducto = detalle.getProducto().getName();

                        // Obtener talla y color de las variantes del producto
                        String talla = "N/A";
                        String color = "N/A";

                        if (detalle.getIdVariante() > 0 && detalle.getProducto().getVariants() != null) {
                                for (var variante : detalle.getProducto().getVariants()) {
                                        if (variante.getVariantId() == detalle.getIdVariante()) {
                                                talla = variante.getTalla() != null ? variante.getTalla() : "N/A";
                                                color = variante.getColor() != null ? variante.getColor() : "N/A";
                                                break;
                                        }
                                }
                        }

                        int cantidad = detalle.getCantidad();
                        double precioUnitario = detalle.getPrecioUnitario();
                        double subtotal = detalle.getSubtotal();

                        model.addRow(new Object[] {
                                        nombreProducto,
                                        talla,
                                        color,
                                        cantidad,
                                        MoneyFormatter.format(precioUnitario),
                                        MoneyFormatter.format(subtotal)
                        });
                }
        }

        @SuppressWarnings("unchecked")
        private void initComponents() {

                panelInfo = new javax.swing.JPanel();
                lblIdVenta = new javax.swing.JLabel();
                lblFecha = new javax.swing.JLabel();
                lblCliente = new javax.swing.JLabel();
                lblVendedor = new javax.swing.JLabel();
                lblTotal = new javax.swing.JLabel();
                lblEstado = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                table = new javax.swing.JTable();
                jLabel1 = new javax.swing.JLabel();

                lblIdVenta.setFont(new java.awt.Font("Segoe UI", 1, 18));
                lblIdVenta.setText("Venta #");

                lblFecha.setText("Fecha: -");

                lblCliente.setText("Cliente: -");

                lblVendedor.setText("Vendedor: -");

                lblTotal.setFont(new java.awt.Font("Segoe UI", 1, 14));
                lblTotal.setText("Total: $0.00");

                lblEstado.setText("Estado: -");

                javax.swing.GroupLayout panelInfoLayout = new javax.swing.GroupLayout(panelInfo);
                panelInfo.setLayout(panelInfoLayout);
                panelInfoLayout.setHorizontalGroup(
                                panelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelInfoLayout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addGroup(panelInfoLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(lblIdVenta)
                                                                                .addComponent(lblFecha)
                                                                                .addComponent(lblCliente)
                                                                                .addComponent(lblVendedor)
                                                                                .addGroup(panelInfoLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(lblTotal)
                                                                                                .addGap(30, 30, 30)
                                                                                                .addComponent(lblEstado)))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelInfoLayout.setVerticalGroup(
                                panelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelInfoLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addComponent(lblIdVenta)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblFecha)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblCliente)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblVendedor)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelInfoLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(lblTotal)
                                                                                .addComponent(lblEstado))
                                                                .addContainerGap(10, Short.MAX_VALUE)));

                table.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {

                                },
                                new String[] {
                                                "Producto", "Talla", "Color", "Cantidad", "Precio Unit.", "Subtotal"
                                }) {
                        boolean[] canEdit = new boolean[] {
                                        false, false, false, false, false, false
                        };

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit[columnIndex];
                        }
                });
                jScrollPane1.setViewportView(table);

                jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14));
                jLabel1.setText("Productos de la Venta");

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelInfo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jScrollPane1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                688,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(layout.createSequentialGroup()
                                                                                                .addComponent(jLabel1)
                                                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                                .addContainerGap()));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addComponent(panelInfo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel1)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jScrollPane1,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                246, Short.MAX_VALUE)
                                                                .addContainerGap()));
        }

        // Variables declaration
        private javax.swing.JLabel jLabel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JLabel lblCliente;
        private javax.swing.JLabel lblEstado;
        private javax.swing.JLabel lblFecha;
        private javax.swing.JLabel lblIdVenta;
        private javax.swing.JLabel lblTotal;
        private javax.swing.JLabel lblVendedor;
        private javax.swing.JPanel panelInfo;
        private javax.swing.JTable table;
}
