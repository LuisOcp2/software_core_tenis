package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.ModelTipoGasto;
import raven.controlador.principal.conexion;
import raven.clases.principal.ServiceGastoOperativo;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;

/**
 * Formulario de administración para Tipos de Gasto.
 * Permite gestionar los tipos de gastos operativos, sus límites y
 * configuraciones.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class TiposGastoForm extends javax.swing.JPanel {

    private final ServiceGastoOperativo serviceGasto = new ServiceGastoOperativo();
    private final FontIcon iconNuevo;
    private final FontIcon iconEditar;
    private final FontIcon iconDesactivar;
    private final NumberFormat formatoMoneda;

    public TiposGastoForm() {
        initComponents();

        // Formato de moneda para Colombia
        formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        formatoMoneda.setMaximumFractionDigits(0);

        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");

        // Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.PLUS_CIRCLE, tabTextColor);
        iconEditar = createColoredIcon(FontAwesomeSolid.EDIT, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.BAN, tabTextColor);

        btnCrear.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41");
        btnEditar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FFCC00");
        btnDesactivar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FF453A");

        btnCrear.setIcon(iconNuevo);
        btnEditar.setIcon(iconEditar);
        btnDesactivar.setIcon(iconDesactivar);

        init();
    }

    private void init() {
        // Estiliza el panel principal
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;"
                + "background:$Table.background");

        // Estiliza el encabezado de la tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;"
                + "font:bold;");

        // Estiliza la tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:50;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        // Barra de desplazamiento
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;"
                + "trackInsets:3,3,3,3;"
                + "thumbInsets:3,3,3,3;"
                + "background:$Table.background;");

        // Título
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;");

        // Campo de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por código o nombre...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0;"
                + "margin:5,20,5,20;"
                + "background:$Panel.background");

        // Configurar renderizadores de columnas
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Renderizador para columna Estado (índice 6)
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                String estadoTexto;
                if (value instanceof Boolean) {
                    estadoTexto = (Boolean) value ? "Activo" : "Inactivo";
                } else if (value instanceof String) {
                    estadoTexto = value.toString();
                } else {
                    estadoTexto = "Inactivo";
                }
                return super.getTableCellRendererComponent(
                        table, estadoTexto, isSelected, hasFocus, row, column);
            }
        });

        // Renderizador para columna Requiere Autorización (índice 4)
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                String texto;
                if (value instanceof Boolean) {
                    texto = (Boolean) value ? "Sí" : "No";
                } else {
                    texto = "No";
                }
                setHorizontalAlignment(CENTER);
                return super.getTableCellRendererComponent(
                        table, texto, isSelected, hasFocus, row, column);
            }
        });

        // Renderizador para columna Monto Máximo (índice 5)
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                String texto = "";
                if (value instanceof BigDecimal) {
                    texto = formatoMoneda.format(value);
                } else if (value != null) {
                    texto = value.toString();
                }
                setHorizontalAlignment(RIGHT);
                return super.getTableCellRendererComponent(
                        table, texto, isSelected, hasFocus, row, column);
            }
        });

        // Conectar y cargar datos
        try {
            conexion.getInstance().connectToDatabase();
            loadData();
        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al conectar: " + e.getMessage());
        }
    }

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    private void searchData(String search) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        model.setRowCount(0);

        List<ModelTipoGasto> list = search.isEmpty()
                ? serviceGasto.listarTodosTiposGasto()
                : serviceGasto.buscarTiposGasto(search);

        for (ModelTipoGasto tipo : list) {
            model.addRow(toTableRow(tipo));
        }
    }

    private void loadData() throws SQLException {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            model.setRowCount(0);

            List<ModelTipoGasto> list = serviceGasto.listarTodosTiposGasto();
            for (ModelTipoGasto tipo : list) {
                model.addRow(toTableRow(tipo));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());
        }
    }

    private Object[] toTableRow(ModelTipoGasto tipo) {
        return new Object[] {
                false, // Checkbox
                tipo.getIdTipoGasto(),
                tipo.getCodigo(),
                tipo.getNombre(),
                tipo.isRequiereAutorizacion(),
                tipo.getMontoMaximoSinAutorizacion(),
                tipo.isActivo()
        };
    }

    private List<ModelTipoGasto> getSelectedData() {
        List<ModelTipoGasto> list = new ArrayList<>();

        for (int i = 0; i < table.getRowCount(); i++) {
            if ((boolean) table.getValueAt(i, 0)) {
                Integer id = (Integer) table.getValueAt(i, 1);
                String codigo = (String) table.getValueAt(i, 2);
                String nombre = (String) table.getValueAt(i, 3);
                boolean reqAuth = table.getValueAt(i, 4) instanceof Boolean
                        ? (Boolean) table.getValueAt(i, 4)
                        : false;
                BigDecimal montoMax = table.getValueAt(i, 5) instanceof BigDecimal
                        ? (BigDecimal) table.getValueAt(i, 5)
                        : BigDecimal.ZERO;

                Object estadoObj = table.getValueAt(i, 6);
                boolean activo = estadoObj instanceof Boolean
                        ? (Boolean) estadoObj
                        : (estadoObj != null && estadoObj.toString().equals("Activo"));

                ModelTipoGasto tipo = new ModelTipoGasto(id, codigo, nombre);
                tipo.setRequiereAutorizacion(reqAuth);
                tipo.setMontoMaximoSinAutorizacion(montoMax);
                tipo.setActivo(activo);
                list.add(tipo);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        btnCrear = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnDesactivar = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Gestión de Tipos de Gasto");

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] {
                        "Seleccionar", "ID", "Código", "Nombre", "Req. Autorización", "Monto Máximo", "Estado"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class,
                    java.lang.String.class, java.lang.Boolean.class, java.math.BigDecimal.class,
                    java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean[] {
                    true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });

        // Ajustar anchos de columnas
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);

        scroll.setViewportView(table);

        btnCrear.setText("Crear");
        btnCrear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearActionPerformed(evt);
            }
        });

        btnEditar.setText("Editar");
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });

        btnDesactivar.setText("Desactivar");
        btnDesactivar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDesactivarActionPerformed(evt);
            }
        });

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("Tipos de Gasto");

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addComponent(scroll)
                                .addContainerGap())
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addComponent(lbTitle)
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 280,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        400, Short.MAX_VALUE)
                                                .addComponent(btnCrear)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnEditar)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnDesactivar)
                                                .addGap(27, 27, 27)))));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lbTitle)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnCrear)
                                        .addComponent(btnEditar)
                                        .addComponent(btnDesactivar))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lb, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()));
    }

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(TiposGastoForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void btnCrearActionPerformed(java.awt.event.ActionEvent evt) {
        CreateTipoGasto cr = new CreateTipoGasto();
        cr.loadData(null);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION)
        };

        ModalDialog.showModal(this,
                new SimpleModalBorder(cr, "Nuevo Tipo de Gasto", options, (mc, i) -> {
                    if (i == SimpleModalBorder.OK_OPTION) {
                        ModelTipoGasto tipoData = cr.getData();
                        if (tipoData != null) {
                            try {
                                serviceGasto.crearTipoGasto(tipoData);
                                Toast.show(TiposGastoForm.this, Toast.Type.SUCCESS,
                                        "Tipo de gasto creado exitosamente");
                                loadData();
                            } catch (SQLException e) {
                                Toast.show(TiposGastoForm.this, Toast.Type.ERROR, "Error al crear: " + e.getMessage());
                            }
                        }
                    } else if (i == SimpleModalBorder.OPENED) {
                        cr.init();
                    }
                }));
    }

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {
        List<ModelTipoGasto> list = getSelectedData();

        if (!list.isEmpty()) {
            if (list.size() == 1) {
                ModelTipoGasto data = list.get(0);

                // Si está desactivado, ofrecer activar
                if (!data.isActivo()) {
                    SimpleModalBorder.Option[] optionsActivar = new SimpleModalBorder.Option[] {
                            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                            new SimpleModalBorder.Option("Activar", SimpleModalBorder.OK_OPTION),
                    };

                    JLabel labelActivar = new JLabel("¿Desea activar este tipo de gasto?");
                    labelActivar.setBorder(new EmptyBorder(5, 25, 5, 25));

                    ModalDialog.showModal(
                            this,
                            new SimpleModalBorder(
                                    labelActivar,
                                    "Activar: " + data.getNombre(),
                                    optionsActivar,
                                    (mc, i) -> {
                                        if (i == SimpleModalBorder.OK_OPTION) {
                                            try {
                                                serviceGasto.activarTipoGasto(data.getIdTipoGasto());
                                                Toast.show(this, Toast.Type.SUCCESS, "Tipo de gasto activado");
                                                loadData();
                                            } catch (SQLException e) {
                                                Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                                            }
                                        }
                                    }));
                    return;
                }

                // Cargar datos completos del tipo de gasto
                try {
                    ModelTipoGasto tipoCompleto = serviceGasto.obtenerTipoGasto(data.getIdTipoGasto()).orElse(data);

                    CreateTipoGasto create = new CreateTipoGasto();
                    create.loadData(tipoCompleto);

                    SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                            new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION),
                    };

                    ModalDialog.showModal(
                            this,
                            new SimpleModalBorder(
                                    create,
                                    "Editar: " + data.getNombre(),
                                    options,
                                    (mc, i) -> {
                                        if (i == SimpleModalBorder.OK_OPTION) {
                                            try {
                                                ModelTipoGasto dataEdit = create.getData();
                                                if (dataEdit != null) {
                                                    dataEdit.setIdTipoGasto(data.getIdTipoGasto());
                                                    serviceGasto.actualizarTipoGasto(dataEdit);
                                                    Toast.show(this, Toast.Type.SUCCESS, "Tipo de gasto actualizado");
                                                    loadData();
                                                }
                                            } catch (SQLException e) {
                                                Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                                            }
                                        } else if (i == SimpleModalBorder.OPENED) {
                                            create.init();
                                        }
                                    }));
                } catch (SQLException ex) {
                    Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + ex.getMessage());
                }
            } else {
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo un tipo de gasto");
            }
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un tipo de gasto para editar");
        }
    }

    private void btnDesactivarActionPerformed(java.awt.event.ActionEvent evt) {
        List<ModelTipoGasto> list = getSelectedData();
        if (!list.isEmpty()) {
            // Filtrar solo los activos
            List<ModelTipoGasto> activos = list.stream()
                    .filter(ModelTipoGasto::isActivo)
                    .toList();

            if (activos.isEmpty()) {
                Toast.show(this, Toast.Type.WARNING, "Los tipos seleccionados ya están inactivos");
                return;
            }

            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Desactivar", SimpleModalBorder.OK_OPTION)
            };

            JLabel label = new JLabel("¿Está seguro de desactivar " + activos.size() + " tipo(s) de gasto?");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar desactivación",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    try {
                                        for (ModelTipoGasto d : activos) {
                                            serviceGasto.desactivarTipoGasto(d.getIdTipoGasto());
                                        }
                                        Toast.show(this, Toast.Type.SUCCESS, "Tipo(s) de gasto desactivado(s)");
                                        loadData();
                                    } catch (SQLException e) {
                                        Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                                    }
                                }
                            }));
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un tipo de gasto");
        }
    }

    // Variables declaration
    private javax.swing.JButton btnCrear;
    private javax.swing.JButton btnDesactivar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;
}
