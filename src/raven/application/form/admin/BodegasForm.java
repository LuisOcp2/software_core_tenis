package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Window;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.LoadingOverlayHelper;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.admin.ModelBodegas;
import raven.controlador.principal.conexion;
import raven.clases.admin.ServiceBodegas;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalController;

/**
 *
 * @author CrisDEV
 */
public class BodegasForm extends javax.swing.JPanel {

    private final ServiceBodegas serviceBodegas = new ServiceBodegas();

    private static FlatSVGIcon dashboardIcon(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("raven/icon/svg/dashboard/" + name, size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }

    private static void styleActionButton(JButton button, Color bg, Color hover, Color pressed, FlatSVGIcon icon) {
        if (button == null) {
            return;
        }
        button.setIcon(icon);
        button.setIconTextGap(8);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;"
                        + "background:#" + toHex(bg) + ";"
                        + "foreground:#ffffff;"
                        + "hoverBackground:#" + toHex(hover) + ";"
                        + "pressedBackground:#" + toHex(pressed) + ";"
                        + "borderWidth:0;"
                        + "focusWidth:0;"
                        + "font:bold 13;"
                        + "margin:8,16,8,16");
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
    }

    private static String toHex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static JButton findButtonByText(Component root, String text) {
        if (root == null || text == null) {
            return null;
        }
        String target = text.trim();
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component c = stack.pop();
            if (c instanceof JButton) {
                String t = ((JButton) c).getText();
                if (t != null && t.trim().equalsIgnoreCase(target)) {
                    return (JButton) c;
                }
            }
            if (c instanceof Container) {
                Component[] children = ((Container) c).getComponents();
                for (int i = children.length - 1; i >= 0; i--) {
                    stack.push(children[i]);
                }
            }
        }
        return null;
    }

    private static void setEnabledSafe(JButton button, boolean enabled) {
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    private static void setEnabledDeep(Component root, boolean enabled) {
        if (root == null) {
            return;
        }
        root.setEnabled(enabled);
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                setEnabledDeep(child, enabled);
            }
        }
    }

    private static JComponent resolveModalOverlayTarget(Component anyInModal) {
        if (anyInModal == null) {
            return null;
        }
        JRootPane root = SwingUtilities.getRootPane(anyInModal);
        if (root != null) {
            return root.getLayeredPane();
        }
        return anyInModal instanceof JComponent ? (JComponent) anyInModal : null;
    }

    private void runModalActionWithModalLoading(Component anyInModal, String message, Runnable task, Runnable onSuccess,
            Runnable onError) {
        JComponent overlayTarget = resolveModalOverlayTarget(anyInModal);
        if (overlayTarget != null) {
            LoadingOverlayHelper.showLoading(overlayTarget, message);
        }
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    task.run();
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (overlayTarget != null) {
                    LoadingOverlayHelper.hideLoading(overlayTarget);
                }
                if (error == null) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    if (onError != null) {
                        onError.run();
                    }
                }
            }
        };
        worker.execute();
    }

    public BodegasForm() {
        initComponents();

        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");

        // Estilos de botones alineados al diseño de Gestión de Productos
        setupButtonStyles();
        init();
    }

    // Método de inicialización personalizado para componentes UI
    private void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:$Table.background"); // Usa color de fondo de tabla

        // Estiliza el encabezado de la tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;" // Altura del encabezado
                + "hoverBackground:null;" // Desactiva efecto hover
                + "pressedBackground:null;" // Desactiva efecto al presionar
                + "separatorColor:$TableHeader.background;" // Color del separador
                + "font:bold;"); // Texto en negrita

        // Estiliza la tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:70;" // Altura de filas
                + "showHorizontalLines:true;" // Muestra líneas horizontales
                + "intercellSpacing:0,1;" // Espaciado entre celdas
                + "cellFocusColor:$TableHeader.hoverBackground;" // Color de enfoque
                + "selectionBackground:$TableHeader.hoverBackground;" // Fondo de selección
                + "selectionForeground:$Table.foreground;"); // Texto de selección

        // Estiliza la barra de desplazamiento
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;" // Barra completamente redondeada
                + "trackInsets:3,3,3,3;" // Relleno de la barra
                + "thumbInsets:3,3,3,3;" // Relleno del control deslizante
                + "background:$Table.background;"); // Color de fondo

        // Estiliza el título
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;"); // Texto en negrita y más grande

        // Configura el campo de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f)); // Ícono de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;" // Esquinas redondeadas
                + "borderWidth:0;" // Sin borde
                + "focusWidth:0;" // Sin borde de enfoque
                + "innerFocusWidth:0;" // Sin enfoque interno
                + "margin:5,20,5,20;" // Márgenes
                + "background:$Panel.background"); // Color de fondo

        // Configura renderizadores personalizados para columnas
        // 1. Configuración inicial de la tabla (como ya tienes)
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));
        // No hay columna de estado en este modelo de tabla de bodegas
        // Conecta a la base de datos y carga datos iniciales
        try {
            conexion.getInstance().connectToDatabase();
            loadData();
        } catch (SQLException e) {
            // Manejo silencioso de errores (debería mejorarse)
        }
    }

    private void setupButtonStyles() {
        // Tipo de botón
        btnCrear.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnEditar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnDesactivar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);

        // Colores (consistentes con Gestión de Productos)
        btnEditar.putClientProperty(FlatClientProperties.STYLE, "background:$Accent.yellow");
        btnDesactivar.putClientProperty(FlatClientProperties.STYLE, "background:$App.accent.red");

        // Márgenes
        Insets buttonMargin = new Insets(2, 5, 2, 5);
        btnCrear.setMargin(buttonMargin);
        btnEditar.setMargin(buttonMargin);
        btnDesactivar.setMargin(buttonMargin);

        // Iconos SVG
        try {
            FlatSVGIcon iconNew = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/10.svg")).derive(16, 16);
            FlatSVGIcon iconEdit = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/11.svg")).derive(16, 16);
            FlatSVGIcon iconDelete = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/12.svg")).derive(16, 16);

            btnCrear.setIcon(iconNew);
            btnEditar.setIcon(iconEdit);
            btnDesactivar.setIcon(iconDelete);
        } catch (Exception e) {
            // Si no se cargan los iconos, continuar sin bloquear la UI
            System.err.println("No se pudieron cargar los iconos de Bodegas: " + e.getMessage());
        }
    }

    // Busca datos según el texto ingresado
    private void searchData(String search) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        model.setRowCount(0);
        List<ModelBodegas> list = serviceBodegas.search(search);
        for (ModelBodegas b : list) {
            model.addRow(b.toTableRow(table.getRowCount() + 1));
        }
    }

    private void loadData() throws SQLException {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            model.setRowCount(0);
            List<ModelBodegas> list = serviceBodegas.obtenerTodas();
            for (ModelBodegas b : list) {
                model.addRow(b.toTableRow(table.getRowCount() + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar bodegas: " + e.getMessage());
        }
    }

    private List<ModelBodegas> getSelectedBodegas() {
        List<ModelBodegas> list = new ArrayList<>();

        for (int i = 0; i < table.getRowCount(); i++) {
            if (Boolean.TRUE.equals(table.getValueAt(i, 0))) {
                String codigo = (String) table.getValueAt(i, 1);
                String nombre = (String) table.getValueAt(i, 2);
                String direccion = (String) table.getValueAt(i, 3);
                String responsable = (String) table.getValueAt(i, 4);
                String tipo = (String) table.getValueAt(i, 5);

                ModelBodegas b = new ModelBodegas();
                b.setCodigo(codigo);
                b.setNombre(nombre);
                b.setDireccion(direccion);
                b.setResponsable(responsable);
                b.setTipo(tipo);
                b.setActiva(true);

                try {
                    Integer id = serviceBodegas.obtenerIdPorCodigo(codigo);
                    b.setIdBodega(id);
                } catch (SQLException ex) {
                    Logger.getLogger(BodegasForm.class.getName()).log(Level.SEVERE, null, ex);
                }

                list.add(b);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
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
        lb.setText("Gestion de Bodegas");

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null, null, null },
                        { null, null, null, null, null, null },
                        { null, null, null, null, null, null },
                        { null, null, null, null, null, null }
                },
                new String[] {
                        "#", "Codigo", "Nombre", "Dirección", "Responsable", "Tipo"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
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

        btnDesactivar.setText("Eliminar");
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

        lbTitle.setText("Bodegas");

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
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        514, Short.MAX_VALUE)
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
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSearchKeyReleased
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(BodegasForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// GEN-LAST:event_txtSearchKeyReleased

    private void btnCrearActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCrearActionPerformed
        CreateBodegas cr = new CreateBodegas();
        cr.loadData(null);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION), // Opción para cancelar
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION) // Opción para guardar
        };

        JButton[] btnGuardarRef = new JButton[1];
        JButton[] btnCancelarRef = new JButton[1];

        ModalDialog.showModal(this,
                new SimpleModalBorder(cr, "Nueva Bodega", options, (ModalController mc, int i) -> {
                    if (i == SimpleModalBorder.OK_OPTION) {
                        ModelBodegas bodegaData = cr.getData();
                        if (bodegaData == null) {
                            return;
                        }
                        JButton btnGuardar = btnGuardarRef[0];
                        JButton btnCancelar = btnCancelarRef[0];

                        setEnabledDeep(cr, false);
                        setEnabledSafe(btnGuardar, false);
                        setEnabledSafe(btnCancelar, false);

                        String[] errorMsg = new String[1];
                        runModalActionWithModalLoading(
                                cr,
                                "Creando bodega...",
                                () -> {
                                    try {
                                        serviceBodegas.insertar(bodegaData);
                                    } catch (SQLException ex) {
                                        errorMsg[0] = ex.getMessage();
                                        throw new RuntimeException(ex);
                                    }
                                },
                                () -> {
                                    Toast.show(BodegasForm.this, Toast.Type.SUCCESS, "Bodega creada exitosamente");
                                    try {
                                        loadData();
                                    } catch (SQLException ex) {
                                        Toast.show(BodegasForm.this, Toast.Type.ERROR,
                                                "Error al cargar bodegas: " + ex.getMessage());
                                    }
                                    mc.close();
                                },
                                () -> {
                                    setEnabledDeep(cr, true);
                                    setEnabledSafe(btnGuardar, true);
                                    setEnabledSafe(btnCancelar, true);
                                    String msg = (errorMsg[0] == null || errorMsg[0].isBlank())
                                            ? "Error al crear bodega"
                                            : ("Error al crear bodega: " + errorMsg[0]);
                                    Toast.show(BodegasForm.this, Toast.Type.ERROR, msg);
                                });
                    } else if (i == SimpleModalBorder.OPENED) { // Si el modal se abre
                        cr.init();
                        Window w = SwingUtilities.getWindowAncestor(cr);
                        btnGuardarRef[0] = findButtonByText(w, "Guardar");
                        btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                        styleActionButton(
                                btnGuardarRef[0],
                                new Color(40, 205, 65),
                                new Color(52, 199, 89),
                                new Color(32, 185, 58),
                                dashboardIcon("check-circle.svg", 16, Color.WHITE));
                        styleActionButton(
                                btnCancelarRef[0],
                                new Color(75, 85, 99),
                                new Color(55, 65, 81),
                                new Color(31, 41, 55),
                                dashboardIcon("x-circle.svg", 16, Color.WHITE));
                    }
                }));
    }// GEN-LAST:event_btnCrearActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnEditarActionPerformed
        List<ModelBodegas> list = getSelectedBodegas();

        if (!list.isEmpty()) {
            if (list.size() == 1) {
                ModelBodegas data = list.get(0);
                ModelBodegas fullData = null;
                try {
                    Integer id = data.getIdBodega();
                    if (id == null) {
                        id = serviceBodegas.obtenerIdPorCodigo(data.getCodigo());
                    }
                    if (id != null) {
                        fullData = serviceBodegas.obtenerPorId(id);
                    } else {
                        // Fallback por código
                        fullData = serviceBodegas.obtenerPorCodigo(data.getCodigo());
                    }
                } catch (SQLException ex) {
                    Toast.show(this, Toast.Type.ERROR, "Error cargando datos de la bodega: " + ex.getMessage());
                }

                CreateBodegas create = new CreateBodegas();
                create.loadData(fullData != null ? fullData : data);

                SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                        new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                        new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION), };

                Integer idEditar = null;
                if (fullData != null && fullData.getIdBodega() != null) {
                    idEditar = fullData.getIdBodega();
                } else if (data.getIdBodega() != null) {
                    idEditar = data.getIdBodega();
                } else {
                    try {
                        idEditar = serviceBodegas.obtenerIdPorCodigo(data.getCodigo());
                    } catch (SQLException ex) {
                        idEditar = null;
                    }
                }
                if (idEditar == null) {
                    Toast.show(this, Toast.Type.ERROR, "No se pudo identificar la bodega para actualizar");
                    return;
                }

                JButton[] btnActualizarRef = new JButton[1];
                JButton[] btnCancelarRef = new JButton[1];

                final Integer idFinalEditar = idEditar;
                ModalDialog.showModal(
                        this,
                        new SimpleModalBorder(
                                create,
                                "Editar Bodega: " + data.getNombre(),
                                options,
                                (mc, i) -> {
                                    if (i == SimpleModalBorder.OK_OPTION) {
                                        ModelBodegas dataEdit = create.getData();
                                        if (dataEdit == null) {
                                            return;
                                        }
                                        dataEdit.setIdBodega(idFinalEditar);

                                        JButton btnActualizar = btnActualizarRef[0];
                                        JButton btnCancelar = btnCancelarRef[0];

                                        setEnabledDeep(create, false);
                                        setEnabledSafe(btnActualizar, false);
                                        setEnabledSafe(btnCancelar, false);

                                        String[] errorMsg = new String[1];
                                        runModalActionWithModalLoading(
                                                create,
                                                "Actualizando bodega...",
                                                () -> {
                                                    try {
                                                        serviceBodegas.actualizar(dataEdit);
                                                    } catch (SQLException ex) {
                                                        errorMsg[0] = ex.getMessage();
                                                        throw new RuntimeException(ex);
                                                    }
                                                },
                                                () -> {
                                                    Toast.show(this, Toast.Type.SUCCESS, "Bodega actualizada");
                                                    try {
                                                        loadData();
                                                    } catch (SQLException ex) {
                                                        Toast.show(this, Toast.Type.ERROR,
                                                                "Error al cargar bodegas: " + ex.getMessage());
                                                    }
                                                    mc.close();
                                                },
                                                () -> {
                                                    setEnabledDeep(create, true);
                                                    setEnabledSafe(btnActualizar, true);
                                                    setEnabledSafe(btnCancelar, true);
                                                    String msg = (errorMsg[0] == null || errorMsg[0].isBlank())
                                                            ? "Error al actualizar bodega"
                                                            : ("Error al actualizar bodega: " + errorMsg[0]);
                                                    Toast.show(this, Toast.Type.ERROR, msg);
                                                });
                                    } else if (i == SimpleModalBorder.OPENED) {
                                        create.init();
                                        Window w = SwingUtilities.getWindowAncestor(create);
                                        btnActualizarRef[0] = findButtonByText(w, "Actualizar");
                                        btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                        styleActionButton(
                                                btnActualizarRef[0],
                                                new Color(59, 130, 246),
                                                new Color(37, 99, 235),
                                                new Color(29, 78, 216),
                                                dashboardIcon("refresh.svg", 16, Color.WHITE));
                                        styleActionButton(
                                                btnCancelarRef[0],
                                                new Color(75, 85, 99),
                                                new Color(55, 65, 81),
                                                new Color(31, 41, 55),
                                                dashboardIcon("x-circle.svg", 16, Color.WHITE));
                                    }
                                }));
            } else {
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo una Bodega");
            }
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione una Bodega para editar");
        }
    }// GEN-LAST:event_btnEditarActionPerformed

    private void btnDesactivarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDesactivarActionPerformed
        // Obtiene la lista de empleados seleccionados (probablemente de una tabla o
        // vista)
        List<ModelBodegas> list = getSelectedBodegas();
        if (!list.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Eliminar", SimpleModalBorder.OK_OPTION)
            };

            JLabel label = new JLabel("¿Está seguro de eliminar/desactivar " + list.size() + " Bodega(s)?");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar desactivacion",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    try {
                                        for (ModelBodegas d : list) {
                                            if (d.getIdBodega() != null) {
                                                serviceBodegas.desactivar(d.getIdBodega());
                                            } else if (d.getCodigo() != null) {
                                                Integer id = serviceBodegas.obtenerIdPorCodigo(d.getCodigo());
                                                if (id != null)
                                                    serviceBodegas.desactivar(id);
                                            }
                                        }
                                        Toast.show(this, Toast.Type.SUCCESS, "Bodega(s) eliminada(s)/desactivada(s)");
                                        loadData();
                                    } catch (SQLException e) {
                                        Toast.show(this, Toast.Type.ERROR, "Error al eliminar: " + e.getMessage());
                                    }
                                }
                            }));
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos una Bodega");
        }
    }// GEN-LAST:event_btnDesactivarActionPerformed

    /**
     * Método helper para obtener el ID de bodega basado en su nombre
     */
    // Eliminado: helper por nombre. Usamos código como identificador único.

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton btnCrear;
    public javax.swing.JButton btnDesactivar;
    public javax.swing.JButton btnEditar;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    public javax.swing.JTable table;
    public javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

}
