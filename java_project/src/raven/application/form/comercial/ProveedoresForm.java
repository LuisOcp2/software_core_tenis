package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.comercial.ServiceSupplier;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.LoadingOverlayHelper;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.comercial.ModelSupplier;
import raven.controlador.principal.conexion;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.BorderOption;
import raven.modal.listener.ModalController;

public class ProveedoresForm extends javax.swing.JPanel {
    // Instancia del servicio para operaciones con empleados

    private final ServiceSupplier service = new ServiceSupplier();
    // Constructor del formulario de gestión de productos

    private final FontIcon iconNuevo;
    private final FontIcon iconAjustar;
    private final FontIcon iconEliminar;

    public ProveedoresForm() {
        // Inicializa componentes de la interfaz (generados automáticamente)
        initComponents();

        // Aplica estilo personalizado a la etiqueta (usando propiedades FlatLaf)
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font"); // Usa estilo de fuente h1

        // Inicializa configuraciones personalizadas
        init();

        // Instala la fuente Roboto (extensión de FlatLaf)
        FlatRobotoFont.install();

        // Establece fuente predeterminada para todos los componentes
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.USER_EDIT, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.USER_COG, tabTextColor);
        iconEliminar = createColoredIcon(FontAwesomeSolid.USER_ALT_SLASH, tabTextColor);

        btn_nuevo.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41"); // Color de fondo
        btn_editar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FFCC00"); // Color de fondo
        btn_eliminar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FF453A"); // Color de fondo

        btn_nuevo.setIcon(iconNuevo);
        btn_editar.setIcon(iconAjustar);
        btn_eliminar.setIcon(iconEliminar);

        // Fin de diseño de botones
    }

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

    private void runModalActionWithButtonLoading(JButton button, String message, Runnable task, Runnable onSuccess,
            Consumer<Exception> onError) {
        if (button instanceof JComponent) {
            LoadingOverlayHelper.showLoading((JComponent) button, message);
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
                if (button instanceof JComponent) {
                    LoadingOverlayHelper.hideLoading((JComponent) button);
                }
                if (error == null) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    if (onError != null) {
                        onError.accept(error);
                    }
                }
            }
        };
        worker.execute();
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
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
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
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Configuración predeterminada para diálogos modales
        ModalDialog.getDefaultOption()
                .setOpacity(0.3f) // Opacidad del fondo
                .getLayoutOption().setAnimateScale(0.1f); // Escala de animación
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM); // Sombra

        // Conecta a la base de datos y carga datos iniciales
        try {
            conexion.getInstance().connectToDatabase();
            loadData();
            conexion.getInstance().close();

        } catch (SQLException e) {
            // Manejo silencioso de errores (debería mejorarse)
        }
    }

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    // Carga datos desde el servicio a la tabla
    private void loadData() throws SQLException {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería
                                                                            // mejorarse)
            // Detiene cualquier edición de celda activa
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            // Limpia las filas existentes
            model.setRowCount(0);
            // Obtiene todos los empleados del servicio
            List<ModelSupplier> list = service.getAll();

            // Añade cada empleado como fila en la tabla
            for (ModelSupplier d : list) {
                model.addRow(d.toTableRow(table.getRowCount() + 1));

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());

        }

    }

    // Busca datos según el texto ingresado
    private void searchData(String search) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería
                                                                        // mejorarse)
        // Detiene cualquier edición de celda activa
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        // Limpia las filas existentes
        model.setRowCount(0);
        // Obtiene resultados de búsqueda del servicio
        List<ModelSupplier> list = service.search(search);
        // Añade empleados coincidentes como filas en la tabla
        for (ModelSupplier d : list) {
            model.addRow(d.toTableRow(table.getRowCount() + 1));
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();
        btn_nuevo = new raven.componentes.ButtonAction();
        btn_editar = new raven.componentes.ButtonAction();
        btn_eliminar = new raven.componentes.ButtonAction();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Gestion de proveedores");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "SELECT", "N°", "NOMBRE", "NIT", "Direccion", "Telefono", "Email"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
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
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(10);
            table.getColumnModel().getColumn(1).setPreferredWidth(20);
            table.getColumnModel().getColumn(2).setPreferredWidth(140);
            table.getColumnModel().getColumn(3).setPreferredWidth(60);
            table.getColumnModel().getColumn(4).setPreferredWidth(90);
        }

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("Categorias");

        btn_nuevo.setText("CREAR");
        btn_nuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_nuevoActionPerformed(evt);
            }
        });

        btn_editar.setText("EDITAR");
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        btn_eliminar.setText("ELIMINAR");
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jSeparator1)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addGap(20, 20, 20)
                                                .addGroup(panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelLayout.createSequentialGroup()
                                                                .addComponent(txtSearch,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 239,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                        202, Short.MAX_VALUE)
                                                                .addComponent(btn_nuevo,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(btn_editar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(btn_eliminar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(lbTitle)))
                                        .addComponent(scroll))
                                .addContainerGap()));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(lbTitle)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_nuevo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_editar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addContainerGap())));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lb)
                                .addGap(0, 732, Short.MAX_VALUE))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(31, 31, 31)
                                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addContainerGap())));
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSearchKeyReleased
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(ProveedoresForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// GEN-LAST:event_txtSearchKeyReleased

    private void btn_nuevoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_nuevoActionPerformed
        CreateSupplier cr = new CreateSupplier();
        cr.loadData(service, null);
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION)
        };

        JButton[] btnGuardarRef = new JButton[1];
        JButton[] btnCancelarRef = new JButton[1];

        SimpleModalBorder border = new SimpleModalBorder(cr, "Nuevo Producto", options, (ModalController mc, int i) -> {
            if (i == SimpleModalBorder.OK_OPTION) {
                ModelSupplier data = cr.getData();
                if (data.getName().isEmpty()) {
                    Toast.show(ProveedoresForm.this, Toast.Type.WARNING, "El nombre es obligatorio");
                    return;
                }

                JButton btnGuardar = btnGuardarRef[0];
                JButton btnCancelar = btnCancelarRef[0];
                setEnabledSafe(btnGuardar, false);
                setEnabledSafe(btnCancelar, false);
                runModalActionWithButtonLoading(
                        btnGuardar,
                        "Creando...",
                        () -> {
                            try {
                                service.create(data);
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        },
                        () -> {
                            Toast.show(ProveedoresForm.this, Toast.Type.SUCCESS, "Proveedor creado exitosamente");
                            try {
                                loadData();
                            } catch (SQLException ex) {
                                Toast.show(ProveedoresForm.this, Toast.Type.ERROR,
                                        "Error al cargar datos: " + ex.getMessage());
                            }
                            mc.close();
                        },
                        (ex) -> {
                            setEnabledSafe(btnGuardar, true);
                            setEnabledSafe(btnCancelar, true);
                            // Unwrap RuntimeException if needed
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            Toast.show(ProveedoresForm.this, Toast.Type.ERROR, "Error: " + cause.getMessage());
                        });
            } else if (i == SimpleModalBorder.OPENED) {
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
                        new Color(255, 69, 58),
                        new Color(255, 92, 84),
                        new Color(233, 58, 50),
                        dashboardIcon("x-circle.svg", 16, Color.WHITE));
            }
        });

        ModalDialog.showModal(this, border);
    }// GEN-LAST:event_btn_nuevoActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_eliminarActionPerformed
        List<ModelSupplier> list = getSelectedData();
        if (!list.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Eliminar", SimpleModalBorder.OK_OPTION)
            };

            JButton[] btnEliminarRef = new JButton[1];
            JButton[] btnCancelarRef = new JButton[1];

            JLabel label = new JLabel("¿Está seguro de eliminar " + list.size() + " producto(s)?");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar Eliminación",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    JButton btnEliminar = btnEliminarRef[0];
                                    JButton btnCancelar = btnCancelarRef[0];
                                    setEnabledSafe(btnEliminar, false);
                                    setEnabledSafe(btnCancelar, false);
                                    runModalActionWithButtonLoading(
                                            btnEliminar,
                                            "Eliminando...",
                                            () -> {
                                                try {
                                                    for (ModelSupplier d : list) {
                                                        service.delete(d.getSupplierId());
                                                    }
                                                } catch (SQLException ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                            },
                                            () -> {
                                                Toast.show(this, Toast.Type.SUCCESS, "Proveedor(es) eliminado(s)");
                                                try {
                                                    loadData();
                                                } catch (SQLException ex) {
                                                    Toast.show(this, Toast.Type.ERROR,
                                                            "Error al cargar datos: " + ex.getMessage());
                                                }
                                                mc.close();
                                            },
                                            (ex) -> {
                                                setEnabledSafe(btnEliminar, true);
                                                setEnabledSafe(btnCancelar, true);
                                                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                                                Toast.show(this, Toast.Type.ERROR, "Error: " + cause.getMessage());
                                            });
                                } else if (i == SimpleModalBorder.OPENED) {
                                    Window w = SwingUtilities.getWindowAncestor(label);
                                    btnEliminarRef[0] = findButtonByText(w, "Eliminar");
                                    btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                    styleActionButton(
                                            btnEliminarRef[0],
                                            new Color(255, 69, 58),
                                            new Color(255, 92, 84),
                                            new Color(233, 58, 50),
                                            dashboardIcon("minus-circle.svg", 16, Color.WHITE));
                                    styleActionButton(
                                            btnCancelarRef[0],
                                            new Color(75, 85, 99),
                                            new Color(55, 65, 81),
                                            new Color(31, 41, 55),
                                            dashboardIcon("x-circle.svg", 16, Color.WHITE));
                                }
                            }));
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un producto");
        }
    }// GEN-LAST:event_btn_eliminarActionPerformed

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_editarActionPerformed
        List<ModelSupplier> list = getSelectedData();

        if (!list.isEmpty()) {
            if (list.size() == 1) {
                ModelSupplier data = list.get(0);
                CreateSupplier creates = new CreateSupplier();
                creates.loadData(service, data);

                SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                        new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                        new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION)
                };

                JButton[] btnActualizarRef = new JButton[1];
                JButton[] btnCancelarRef = new JButton[1];

                ModalDialog.showModal(this, // Componente padre (probablemente un JFrame o JPanel)
                        new SimpleModalBorder(
                                creates, // Panel de contenido (el formulario de edición)
                                "Editar Categoria: " + data.getName(), // Título del diálogo
                                options, // Opciones de botones
                                (mc, i) -> {
                                    if (i == SimpleModalBorder.OK_OPTION) {
                                        ModelSupplier dataEdit = creates.getData();
                                        if (dataEdit.getName().isEmpty()) {
                                            Toast.show(ProveedoresForm.this, Toast.Type.WARNING,
                                                    "El nombre es obligatorio");
                                            return;
                                        }

                                        JButton btnActualizar = btnActualizarRef[0];
                                        JButton btnCancelar = btnCancelarRef[0];
                                        setEnabledSafe(btnActualizar, false);
                                        setEnabledSafe(btnCancelar, false);
                                        runModalActionWithButtonLoading(
                                                btnActualizar,
                                                "Actualizando...",
                                                () -> {
                                                    try {
                                                        dataEdit.setSupplierId(data.getSupplierId());
                                                        service.update(dataEdit);
                                                    } catch (SQLException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                },
                                                () -> {
                                                    Toast.show(this, Toast.Type.SUCCESS, "Proveedor actualizado");
                                                    try {
                                                        loadData();
                                                    } catch (SQLException ex) {
                                                        Toast.show(this, Toast.Type.ERROR,
                                                                "Error al cargar datos: " + ex.getMessage());
                                                    }
                                                    mc.close();
                                                },
                                                (ex) -> {
                                                    setEnabledSafe(btnActualizar, true);
                                                    setEnabledSafe(btnCancelar, true);
                                                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                                                    Toast.show(this, Toast.Type.ERROR, "Error: " + cause.getMessage());
                                                });
                                    } else if (i == SimpleModalBorder.OPENED) {
                                        creates.init();
                                        Window w = SwingUtilities.getWindowAncestor(creates);
                                        btnActualizarRef[0] = findButtonByText(w, "Actualizar");
                                        btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                        styleActionButton(
                                                btnActualizarRef[0],
                                                new Color(40, 205, 65),
                                                new Color(52, 199, 89),
                                                new Color(32, 185, 58),
                                                dashboardIcon("refresh.svg", 16, Color.WHITE));
                                        styleActionButton(
                                                btnCancelarRef[0],
                                                new Color(255, 69, 58),
                                                new Color(255, 92, 84),
                                                new Color(233, 58, 50),
                                                dashboardIcon("x-circle.svg", 16, Color.WHITE));
                                    }
                                }));
            } else {
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo un producto");
            }
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un producto para editar");
        }
    }// GEN-LAST:event_btn_editarActionPerformed
    // Declara un método que devuelve una lista de objetos ModelProduct

    private List<ModelSupplier> getSelectedData() {

        // Crea una nueva lista vacía para almacenar los productos seleccionados
        List<ModelSupplier> list = new ArrayList<>();

        // Recorre todas las filas de la tabla (desde la fila 0 hasta la última)
        for (int i = 0; i < table.getRowCount(); i++) {

            // Verifica si el valor de la primera columna (columna 0) es true
            // (marcado/seleccionado)
            if ((boolean) table.getValueAt(i, 0)) {

                // Obtiene el objeto ModelProduct de la tercera columna (columna 2) de la fila
                // actual
                ModelSupplier data = (ModelSupplier) table.getValueAt(i, 2);
                list.add(data);
            }
        }

        // Devuelve la lista con todos los productos que estaban seleccionados en la
        // tabla
        return list;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private raven.componentes.ButtonAction btn_editar;
    private raven.componentes.ButtonAction btn_eliminar;
    private raven.componentes.ButtonAction btn_nuevo;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
