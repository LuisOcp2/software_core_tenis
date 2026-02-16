package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.admin.UserSession;
import raven.clases.comercial.ServiceCuentasPorCobrar;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.comercial.ModelClienteDeuda;
import raven.controlador.principal.conexion;
import raven.modal.Toast;

/**
 *
 * Formulario para gestión de Cuentas por Cobrar
 */
public class CuentasPorCobrarForm extends javax.swing.JPanel {

    private final ServiceCuentasPorCobrar service = new ServiceCuentasPorCobrar();
    private final FontIcon iconVerDetalle;

    public CuentasPorCobrarForm() {
        initComponents();
        init();

        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        iconVerDetalle = createColoredIcon(FontAwesomeSolid.EYE, Color.WHITE);

        btnVerDetalle.setIcon(iconVerDetalle);
        btnVerDetalle.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#007AFF;"
                + "foreground:#FFFFFF;"
                + "font:bold;");

        // Styling summary labels
        setupSummaryLabel(lblTotalDeuda, FontAwesomeSolid.MONEY_BILL_WAVE, new Color(255, 51, 51));
        setupSummaryLabel(lblTotalClientes, FontAwesomeSolid.USERS, new Color(0, 122, 255));

        btnRefresh.setIcon(createColoredIcon(FontAwesomeSolid.SYNC_ALT, UIManager.getColor("Actions.Blue")));
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:null;"
                + "borderWidth:0;"
                + "focusWidth:0");
    }

    private void setupSummaryLabel(javax.swing.JLabel label, org.kordamp.ikonli.Ikon ikon, Color color) {
        label.setIcon(createColoredIcon(ikon, color));
        label.setIconTextGap(10);
        label.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold 14;");
    }

    private void init() {
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;"
                + "background:$Table.background");

        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;"
                + "font:bold;");

        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:60;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;"
                + "trackInsets:3,3,3,3;"
                + "thumbInsets:3,3,3,3;"
                + "background:$Table.background;");

        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;");

        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar cliente...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0;"
                + "margin:5,20,5,20;"
                + "background:$Panel.background");

        // Configuración tabla
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Alinear columnas numéricas a la derecha
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Cant Ventas
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Deuda Total

        try {
            conexion.getInstance().connectToDatabase();
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() throws SQLException {
        try {
            // Obtener filtro de bodega
            Integer idBodega = null;
            if (!UserSession.getInstance().hasPermission("ver_toda_deuda")) {
                // Solo usuarios con permiso especial ven todas las bodegas, los demás solo ven
                // su bodega
                idBodega = UserSession.getInstance().getIdBodegaUsuario();
            }

            List<ModelClienteDeuda> list = service.obtenerClientesConDeuda(idBodega);
            // Filtrar si hay busqueda
            String search = txtSearch.getText().trim().toLowerCase();
            if (!search.isEmpty()) {
                List<ModelClienteDeuda> filtered = new ArrayList<>();
                for (ModelClienteDeuda c : list) {
                    if (c.getNombre().toLowerCase().contains(search) ||
                            c.getDni().toLowerCase().contains(search)) {
                        filtered.add(c);
                    }
                }
                setTableData(filtered);
            } else {
                setTableData(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());
        }

        // Cargar Resumen
        try {
            Integer idBodega = null;
            if (!UserSession.getInstance().hasPermission("ver_toda_deuda")) {
                idBodega = UserSession.getInstance().getIdBodegaUsuario();
            }
            Object[] resumen = service.obtenerResumenGlobal(idBodega);
            lblTotalDeuda.setText("Deuda Total: " + raven.clases.principal.MoneyFormatter.format((Double) resumen[0]));
            lblTotalClientes.setText("Clientes con Deuda: " + resumen[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setTableData(List<ModelClienteDeuda> list) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        model.setRowCount(0);
        for (ModelClienteDeuda d : list) {
            model.addRow(d.toRowCuentasPorCobrar(table.getRowCount() + 1));
        }
    }

    private FontIcon createColoredIcon(org.kordamp.ikonli.Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        lbTitle = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        lblTotalDeuda = new javax.swing.JLabel();
        lblTotalClientes = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        btnVerDetalle = new javax.swing.JButton();

        lblTotalDeuda.setFont(new java.awt.Font("Segoe UI", 1, 14));
        lblTotalDeuda.setForeground(new java.awt.Color(255, 51, 51));
        lblTotalDeuda.setText("Deuda Total: $0.00");

        lblTotalClientes.setFont(new java.awt.Font("Segoe UI", 1, 14));
        lblTotalClientes.setText("Clientes con Deuda: 0");

        btnRefresh.addActionListener(e -> {
            try {
                loadData();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Cuentas por Cobrar");

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Seleccion", "#", "Cliente Obj", "DNI/NIT", "Nombre", "Teléfono", "Cant. Ventas", "Deuda Total"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    true, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });

        // Hide object column
        if (table.getColumnModel().getColumnCount() > 2) {
            table.getColumnModel().removeColumn(table.getColumnModel().getColumn(2));
        }

        scroll.setViewportView(table);

        btnVerDetalle.setText("Ver Detalle / Abonar");
        btnVerDetalle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerDetalleActionPerformed(evt);
            }
        });

        lbTitle.setText("Clientes con Deuda");

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                try {
                    loadData();
                } catch (SQLException ex) {
                    Logger.getLogger(CuentasPorCobrarForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scroll)
                                .addContainerGap())
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(10, 10, 10)
                                                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addComponent(lbTitle)
                                                .addGap(30, 30, 30)
                                                .addComponent(lblTotalDeuda, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        230, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(10, 10, 10)
                                                .addComponent(lblTotalClientes, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50,
                                        Short.MAX_VALUE)
                                .addComponent(btnVerDetalle, javax.swing.GroupLayout.PREFERRED_SIZE, 180,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnVerDetalle)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addGroup(panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(lbTitle)
                                                        .addComponent(lblTotalDeuda,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblTotalClientes,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(txtSearch)
                                                        .addComponent(btnRefresh, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                                .addContainerGap())
                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lb)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(21, 21, 21)));
    }

    private void btnVerDetalleActionPerformed(java.awt.event.ActionEvent evt) {
        List<ModelClienteDeuda> list = getSelectedData();
        if (list.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un cliente para ver detalles");
            return;
        }
        if (list.size() > 1) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione solo un cliente");
            return;
        }

        ModelClienteDeuda cliente = list.get(0);

        DetalleDeudaClienteForm detalleForm = new DetalleDeudaClienteForm();
        detalleForm.loadData(cliente);

        raven.modal.component.SimpleModalBorder.Option[] options = new raven.modal.component.SimpleModalBorder.Option[] {
                new raven.modal.component.SimpleModalBorder.Option("Cerrar",
                        raven.modal.component.SimpleModalBorder.CLOSE_OPTION)
        };

        raven.modal.ModalDialog.showModal(this, new raven.modal.component.SimpleModalBorder(
                detalleForm, "Detalle de Deuda - " + cliente.getNombre(), options, (mc, i) -> {
                    // Solo ejecutar cuando el usuario haga click en una opción
                    if (i == raven.modal.component.SimpleModalBorder.CLOSE_OPTION) {
                        // Al cerrar, recargar datos por si hubo abonos
                        try {
                            loadData();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        mc.close();
                    }
                }));
    }

    private List<ModelClienteDeuda> getSelectedData() {
        List<ModelClienteDeuda> list = new ArrayList<>();
        // Nota: Al borrar la columna 2, los índices visuales cambian, pero el modelo
        // mantiene sus datos?
        // NO, JTable model indexes match the data structure defined in setModel.
        // I removed column from ColumnModel (View), but Model indexes remain same.
        // "Cliente Obj" is at index 2 in Model.

        for (int i = 0; i < table.getRowCount(); i++) {
            if ((boolean) table.getValueAt(i, 0)) {
                // El objeto cliente está en la columna 2 del MODELO
                ModelClienteDeuda data = (ModelClienteDeuda) table.getModel().getValueAt(i, 2);
                list.add(data);
            }
        }
        return list;
    }

    // Variables declaration
    private javax.swing.JButton btnVerDetalle;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JLabel lblTotalDeuda;
    private javax.swing.JLabel lblTotalClientes;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;
}
