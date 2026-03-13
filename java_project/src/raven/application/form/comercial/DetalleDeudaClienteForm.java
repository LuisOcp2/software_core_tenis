package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import raven.clases.admin.UserSession;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.comercial.ServiceCuentasPorCobrar;
import raven.clases.principal.MoneyFormatter;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.comercial.ModelClienteDeuda;
import raven.controlador.principal.ModelMedioPago;
import raven.modal.Toast;

/**
 * Formulario detalle de deuda de un cliente y abonos
 */
public class DetalleDeudaClienteForm extends javax.swing.JPanel {

        private final ServiceCuentasPorCobrar service = new ServiceCuentasPorCobrar();
        private ModelClienteDeuda clienteActual;

        public DetalleDeudaClienteForm() {
                initComponents();
                init();
        }

        private void init() {
                FlatRobotoFont.install();
                UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

                // Estilos
                panelInfo.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:20;"
                                + "background:$Panel.background");

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
                table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer); // Total
                table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Pagado
                table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Saldo

                // Button style
                btnAbonar.setIcon(createColoredIcon(FontAwesomeSolid.CHECK_CIRCLE, Color.WHITE));
                btnAbonar.putClientProperty(FlatClientProperties.STYLE, ""
                                + "background:#28CD41;"
                                + "foreground:#ffffff;"
                                + "font:bold 14");

                btnVerDetalle.setIcon(createColoredIcon(FontAwesomeSolid.EYE, Color.WHITE));
                btnVerDetalle.putClientProperty(FlatClientProperties.STYLE, ""
                                + "background:#007AFF;"
                                + "foreground:#ffffff;"
                                + "font:bold 14");

                btnAbonoGeneral.setIcon(createColoredIcon(FontAwesomeSolid.HAND_HOLDING_USD, Color.WHITE));
                btnAbonoGeneral.putClientProperty(FlatClientProperties.STYLE, ""
                                + "background:#FF9500;"
                                + "foreground:#ffffff;"
                                + "font:bold 14");

                btnHistorial.setIcon(createColoredIcon(FontAwesomeSolid.HISTORY, Color.WHITE));
                btnHistorial.putClientProperty(FlatClientProperties.STYLE, ""
                                + "background:#8E8E93;"
                                + "foreground:#ffffff;"
                                + "font:bold 14");

                lblSaldoTotal.setIcon(createColoredIcon(FontAwesomeSolid.MONEY_BILL_WAVE, new Color(255, 51, 51)));
                lblSaldoTotal.setIconTextGap(10);
                lblSaldoTotal.putClientProperty(FlatClientProperties.STYLE, ""
                                + "font:bold 16;");
        }

        private FontIcon createColoredIcon(org.kordamp.ikonli.Ikon icon, Color color) {
                FontIcon fontIcon = FontIcon.of(icon);
                fontIcon.setIconSize(18);
                fontIcon.setIconColor(color);
                return fontIcon;
        }

        public void loadData(ModelClienteDeuda cliente) {
                this.clienteActual = cliente;
                lblCliente.setText(cliente.getNombre());
                lblDni.setText("DNI/NIT: " + cliente.getDni());
                lblTelefono.setText("Tel: " + cliente.getTelefono());

                cargarVentasPendientes();
        }

        private void cargarVentasPendientes() {
                try {
                        System.out.println("DetalleDeudaClienteForm: Cargando ventas pendientes para cliente ID: "
                                        + clienteActual.getIdCliente());

                        // Obtener filtro de bodega
                        Integer idBodega = null;
                        if (!UserSession.getInstance().hasRole("admin")) {
                                idBodega = UserSession.getInstance().getIdBodegaUsuario();
                        }

                        List<Object[]> ventas = service.obtenerVentasPendientesPorCliente(clienteActual.getIdCliente(),
                                        idBodega);
                        System.out.println("DetalleDeudaClienteForm: Se obtuvieron " + ventas.size()
                                        + " ventas pendientes");

                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        model.setRowCount(0);

                        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                        for (Object[] row : ventas) {
                                // row: id_venta, fecha, total, pagado, saldo
                                model.addRow(new Object[] {
                                                row[0], // ID
                                                df.format(row[1]), // Fecha
                                                MoneyFormatter.format((Double) row[2]), // Total
                                                MoneyFormatter.format((Double) row[3]), // Pagado
                                                MoneyFormatter.format((Double) row[4]) // Saldo
                                });
                        }
                        System.out.println("DetalleDeudaClienteForm: Tabla actualizada correctamente");

                        // Actualizar Saldo Total
                        double saldoTotal = 0;
                        for (Object[] row : ventas) {
                                saldoTotal += (Double) row[4];
                        }
                        lblSaldoTotal.setText("Saldo Pendiente: " + MoneyFormatter.format(saldoTotal));

                } catch (Exception e) {
                        System.err.println("DetalleDeudaClienteForm: ERROR al cargar ventas pendientes");
                        e.printStackTrace();
                        // Usar SwingUtilities.invokeLater para evitar interferencia con el modal
                        javax.swing.SwingUtilities.invokeLater(() -> {
                                Toast.show(this, Toast.Type.ERROR, "Error cargando ventas: " + e.getMessage());
                        });
                }
        }

        @SuppressWarnings("unchecked")
        private void initComponents() {

                panelInfo = new javax.swing.JPanel();
                lblCliente = new javax.swing.JLabel();
                lblDni = new javax.swing.JLabel();
                lblTelefono = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                table = new javax.swing.JTable();
                btnAbonar = new javax.swing.JButton();
                btnVerDetalle = new javax.swing.JButton();
                btnAbonoGeneral = new javax.swing.JButton();
                btnHistorial = new javax.swing.JButton();
                jLabel1 = new javax.swing.JLabel();
                lblSaldoTotal = new javax.swing.JLabel();

                lblCliente.setFont(new java.awt.Font("Segoe UI", 1, 18));
                lblCliente.setText("Nombre Cliente");

                lblDni.setText("DNI: -");

                lblTelefono.setText("Tel: -");

                javax.swing.GroupLayout panelInfoLayout = new javax.swing.GroupLayout(panelInfo);
                panelInfo.setLayout(panelInfoLayout);
                panelInfoLayout.setHorizontalGroup(
                                panelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelInfoLayout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addGroup(panelInfoLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(lblCliente)
                                                                                .addGroup(panelInfoLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(lblDni,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                150,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(18, 18, 18)
                                                                                                .addComponent(lblTelefono,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                150,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelInfoLayout.setVerticalGroup(
                                panelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelInfoLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addComponent(lblCliente)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(
                                                                                panelInfoLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                .addComponent(lblDni)
                                                                                                .addComponent(lblTelefono))
                                                                .addContainerGap(10, Short.MAX_VALUE)));

                table.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {

                                },
                                new String[] {
                                                "ID Venta", "Fecha", "Total", "Pagado", "Saldo Pendiente"
                                }) {
                        boolean[] canEdit = new boolean[] {
                                        false, false, false, false, false
                        };

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit[columnIndex];
                        }
                });
                jScrollPane1.setViewportView(table);

                btnAbonar.setText("ABONAR A SELECCIÓN");
                btnAbonar.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnAbonarActionPerformed(evt);
                        }
                });

                btnVerDetalle.setText("VER DETALLE VENTA");
                btnVerDetalle.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnVerDetalleActionPerformed(evt);
                        }
                });

                btnAbonoGeneral.setText("ABONO GENERAL (FIFO)");
                btnAbonoGeneral.putClientProperty(FlatClientProperties.STYLE,
                                "background:#FF9500; foreground:#ffffff; font:bold 14");
                btnAbonoGeneral.addActionListener(e -> btnAbonoGeneralActionPerformed(e));

                btnHistorial.setText("HISTORIAL DE PAGOS");
                btnHistorial.putClientProperty(FlatClientProperties.STYLE,
                                "background:#8E8E93; foreground:#ffffff; font:bold 14");
                btnHistorial.addActionListener(e -> btnHistorialActionPerformed(e));

                jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14));
                jLabel1.setText("Ventas Pendientes");

                lblSaldoTotal.setFont(new java.awt.Font("Segoe UI", 1, 14));
                lblSaldoTotal.setForeground(new java.awt.Color(255, 51, 51));
                lblSaldoTotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                lblSaldoTotal.setText("Saldo Total: $0.00");

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
                                                                                                588,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(layout.createSequentialGroup()
                                                                                                .addComponent(jLabel1)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(lblSaldoTotal,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                250,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addGroup(layout.createSequentialGroup()
                                                                                                .addComponent(btnVerDetalle,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(btnAbonar,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(btnAbonoGeneral)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(btnHistorial)))
                                                                .addContainerGap()));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addComponent(panelInfo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(jLabel1)
                                                                                .addComponent(lblSaldoTotal))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jScrollPane1,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                246, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(btnVerDetalle,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                40,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(btnAbonar,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                40,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(btnAbonoGeneral,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                40,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(btnHistorial,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                40,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap()));
        }

        private void btnAbonarActionPerformed(java.awt.event.ActionEvent evt) {
                int row = table.getSelectedRow();
                if (row == -1) {
                        Toast.show(this, Toast.Type.WARNING, "Seleccione una venta para abonar");
                        return;
                }

                int idVenta = (int) table.getValueAt(row, 0);
                String saldoStr = (String) table.getValueAt(row, 4);

                try {
                        BigDecimal saldo = MoneyFormatter.parse(saldoStr);

                        // Mostrar dialogo para ingresar monto
                        String montoStr = JOptionPane.showInputDialog(this,
                                        "Saldo pendiente: " + saldoStr + "\nIngrese monto a abonar:",
                                        "Registrar Abono", JOptionPane.PLAIN_MESSAGE);

                        if (montoStr == null || montoStr.trim().isEmpty())
                                return;

                        BigDecimal monto = new BigDecimal(montoStr);

                        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                                Toast.show(this, Toast.Type.WARNING, "El monto debe ser mayor a 0");
                                return;
                        }
                        if (monto.compareTo(saldo) > 0) {
                                Toast.show(this, Toast.Type.WARNING, "El monto no puede superar el saldo pendiente");
                                return;
                        }

                        // Preguntar metodo de pago
                        String[] opciones = { "Efectivo", "Transferencia", "Tarjeta" };
                        int seleccion = JOptionPane.showOptionDialog(this, "Seleccione método de pago",
                                        "Método de Pago",
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones,
                                        opciones[0]);

                        if (seleccion == -1)
                                return;
                        String metodo = opciones[seleccion].toLowerCase();

                        // Confirmar
                        int confirm = JOptionPane.showConfirmDialog(this,
                                        "¿Registrar abono de " + MoneyFormatter.format(monto.doubleValue())
                                                        + " a la venta #" + idVenta
                                                        + "?",
                                        "Confirmar Abono", JOptionPane.YES_NO_OPTION);

                        if (confirm == JOptionPane.YES_OPTION) {
                                service.registrarAbono(idVenta, monto.doubleValue(), metodo,
                                                "Abono a cuenta por cobrar");
                                Toast.show(this, Toast.Type.SUCCESS, "Abono registrado correctamente");
                                cargarVentasPendientes();
                        }

                } catch (NumberFormatException e) {
                        Toast.show(this, Toast.Type.ERROR, "Monto inválido");
                } catch (Exception e) {
                        e.printStackTrace();
                        Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                }
        }

        private void btnVerDetalleActionPerformed(java.awt.event.ActionEvent evt) {
                int row = table.getSelectedRow();
                if (row == -1) {
                        Toast.show(this, Toast.Type.WARNING, "Seleccione una venta para ver detalles");
                        return;
                }

                int idVenta = (int) table.getValueAt(row, 0);

                try {
                        VerDetalleVentaPanel detallePanel = new VerDetalleVentaPanel();
                        detallePanel.loadData(idVenta);

                        raven.modal.component.SimpleModalBorder.Option[] options = new raven.modal.component.SimpleModalBorder.Option[] {
                                        new raven.modal.component.SimpleModalBorder.Option("Cerrar",
                                                        raven.modal.component.SimpleModalBorder.CLOSE_OPTION)
                        };

                        raven.modal.ModalDialog.showModal(this, new raven.modal.component.SimpleModalBorder(
                                        detallePanel, "Detalle de Venta #" + idVenta, options, (mc, i) -> {
                                                if (i == raven.modal.component.SimpleModalBorder.CLOSE_OPTION) {
                                                        mc.close();
                                                }
                                        }));
                } catch (Exception e) {
                        e.printStackTrace();
                        Toast.show(this, Toast.Type.ERROR, "Error al abrir detalle: " + e.getMessage());
                }
        }

        private void btnAbonoGeneralActionPerformed(java.awt.event.ActionEvent evt) {
                try {
                        String montoStr = JOptionPane.showInputDialog(this,
                                        "Ingrese monto total a abonar (se distribuirá en las deudas más antiguas):",
                                        "Abono General FIFO", JOptionPane.PLAIN_MESSAGE);

                        if (montoStr == null || montoStr.trim().isEmpty())
                                return;

                        double monto = Double.parseDouble(montoStr);
                        if (monto <= 0) {
                                Toast.show(this, Toast.Type.WARNING, "El monto debe ser mayor a 0");
                                return;
                        }

                        // Preguntar metodo de pago
                        String[] opciones = { "Efectivo", "Transferencia", "Tarjeta" };
                        int seleccion = JOptionPane.showOptionDialog(this, "Seleccione método de pago",
                                        "Método de Pago",
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones,
                                        opciones[0]);

                        if (seleccion == -1)
                                return;
                        String metodo = opciones[seleccion].toLowerCase();

                        // Obtener filtro de bodega
                        Integer idBodega = null;
                        if (!UserSession.getInstance().hasRole("admin")) {
                                idBodega = UserSession.getInstance().getIdBodegaUsuario();
                        }

                        service.registrarAbonoGeneral(clienteActual.getIdCliente(), monto, metodo, "Abono General FIFO",
                                        idBodega);
                        Toast.show(this, Toast.Type.SUCCESS, "Abono general realizado correctamente");
                        cargarVentasPendientes();
                } catch (Exception e) {
                        e.printStackTrace();
                        Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                }
        }

        private void btnHistorialActionPerformed(java.awt.event.ActionEvent evt) {
                int row = table.getSelectedRow();
                if (row == -1) {
                        Toast.show(this, Toast.Type.WARNING, "Seleccione una venta para ver su historial");
                        return;
                }

                int idVenta = (int) table.getValueAt(row, 0);

                HistorialPagosClienteForm historyForm = new HistorialPagosClienteForm();
                historyForm.loadData(idVenta);

                raven.modal.component.SimpleModalBorder.Option[] options = new raven.modal.component.SimpleModalBorder.Option[] {
                                new raven.modal.component.SimpleModalBorder.Option("Cerrar",
                                                raven.modal.component.SimpleModalBorder.CLOSE_OPTION)
                };

                raven.modal.ModalDialog.showModal(this, new raven.modal.component.SimpleModalBorder(
                                historyForm, "Historial de Pagos - Venta #" + idVenta, options, (mc, i) -> {
                                        if (i == raven.modal.component.SimpleModalBorder.CLOSE_OPTION) {
                                                mc.close();
                                        }
                                }));
        }

        // Variables declaration
        private javax.swing.JButton btnAbonar;
        private javax.swing.JButton btnVerDetalle;
        private javax.swing.JButton btnAbonoGeneral;
        private javax.swing.JButton btnHistorial;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JLabel lblCliente;
        private javax.swing.JLabel lblDni;
        private javax.swing.JLabel lblTelefono;
        private javax.swing.JLabel lblSaldoTotal;
        private javax.swing.JPanel panelInfo;
        private javax.swing.JTable table;
}
