package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
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
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.comercial.ServiceCliente;
import raven.application.form.productos.GestionProductosForm;
import raven.componentes.LoadingOverlayHelper;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.principal.conexion;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalController;

/**
 *
 * @author Raven
 */
public class ClientesForm extends javax.swing.JPanel {

    private final ServiceCliente service = new ServiceCliente();

    private final FontIcon iconAgregar;
    private final FontIcon iconModificar;
    private final FontIcon iconDesactivar;

    public ClientesForm() {

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
        iconAgregar = createColoredIcon(FontAwesomeSolid.PLUS_SQUARE, tabTextColor);
        iconModificar = createColoredIcon(FontAwesomeSolid.EDIT, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.USER_ALT_SLASH, tabTextColor);

        btnAgregar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41"); // Color de fondo
        btnModificar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FFCC00"); // Color de fondo
        btnDesactivar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FF453A"); // Color de fondo

        btnAgregar.setIcon(iconAgregar);
        btnModificar.setIcon(iconModificar);
        btnDesactivar.setIcon(iconDesactivar);

        // Fin de diseño de botones
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
        // Renderizador especial para el estado (Activo/No Activo)
        // 2. Crea un renderizador personalizado solo para la columna Estado
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                String estadoTexto = "Inactivo";
                if (value instanceof Boolean && (Boolean) value) {
                    estadoTexto = "Activo";
                }
                return super.getTableCellRendererComponent(
                        table, estadoTexto, isSelected, hasFocus, row, column);
            }

        });
        // Conecta a la base de datos y carga datos iniciales
        try {
            conexion.getInstance().connectToDatabase();
            loadData();
        } catch (SQLException e) {
            // Manejo silencioso de errores (debería mejorarse)
        }
    }
    // Carga datos desde el servicio a la tabla

    private void loadData() throws SQLException {
        try {
            setTableData(service.getAll());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());
        }

    }

    // Busca datos según el texto ingresado
    private void searchData(String search) throws SQLException {
        setTableData(service.search(search));
    }

    private void setTableData(List<ModelCliente> list) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        model.setRowCount(0);
        for (ModelCliente d : list) {
            model.addRow(d.toTableRow(table.getRowCount() + 1));
        }
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
            Runnable onError) {
        if (button != null) {
            LoadingOverlayHelper.showLoading(button, message);
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
                if (button != null) {
                    LoadingOverlayHelper.hideLoading(button);
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

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        btnAgregar = new javax.swing.JButton();
        btnModificar = new javax.swing.JButton();
        btnDesactivar = new javax.swing.JButton();
        lbTitle = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Gestion de Clientes");

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Seleccion", "#", "Cod Cliente", "Nit/CC", "Nombre", "Dirrecion", "Telefono", "Correo",
                        "Fecha Registro", "Estado"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    true, false, false, true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        scroll.setViewportView(table);

        btnAgregar.setText("Agregar");
        btnAgregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarActionPerformed(evt);
            }
        });

        btnModificar.setText("Modificar");
        btnModificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnModificarActionPerformed(evt);
            }
        });

        btnDesactivar.setText("Desactivar");
        btnDesactivar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDesactivarActionPerformed(evt);
            }
        });

        lbTitle.setText("Clientes");

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
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
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lbTitle))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAgregar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnModificar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnDesactivar)
                                .addGap(9, 9, 9)));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnAgregar)
                                                .addComponent(btnModificar)
                                                .addComponent(btnDesactivar))
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addComponent(lbTitle)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
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
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgregarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAgregarActionPerformed
        CreateClientes cr = new CreateClientes();
        // Carga los datos en la instancia 'cr' utilizando el servicio proporcionado y
        // un valor nulo
        cr.loadData(service, null);

        // Crea un arreglo de opciones para el modal, que incluye un botón de "Cancelar"
        // y otro de "Guardar"
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION), // Opción para cancelar
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION) // Opción para guardar
        };

        JButton[] btnGuardarRef = new JButton[1];
        JButton[] btnCancelarRef = new JButton[1];

        SimpleModalBorder border = new SimpleModalBorder(cr, "Nuevo Cliente", options, (ModalController mc, int i) -> {
            if (i == SimpleModalBorder.OK_OPTION) {
                JButton btnGuardar = btnGuardarRef[0];
                JButton btnCancelar = btnCancelarRef[0];
                setEnabledSafe(btnGuardar, false);
                setEnabledSafe(btnCancelar, false);
                runModalActionWithButtonLoading(
                        btnGuardar,
                        null,
                        () -> {
                            try {
                                service.create(cr.getData());
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        },
                        () -> {
                            Toast.show(ClientesForm.this, Toast.Type.SUCCESS, "Cliente creado exitosamente");
                            try {
                                loadData();
                            } catch (SQLException ex) {
                                Toast.show(ClientesForm.this, Toast.Type.ERROR,
                                        "Error al cargar datos: " + ex.getMessage());
                            }
                            mc.close();
                        },
                        () -> {
                            setEnabledSafe(btnGuardar, true);
                            setEnabledSafe(btnCancelar, true);
                            Toast.show(ClientesForm.this, Toast.Type.ERROR, "Error al crear cliente");
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
    }// GEN-LAST:event_btnAgregarActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSearchKeyReleased
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(GestionProductosForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// GEN-LAST:event_txtSearchKeyReleased

    private void btnModificarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnModificarActionPerformed
        // Obtiene una lista de productos seleccionados llamando al método
        // getSelectedData()
        List<ModelCliente> list = getSelectedData();

        // Verifica si la lista no está vacía (si hay productos seleccionados)
        if (!list.isEmpty()) {
            // Comprueba si solo hay un producto seleccionado (tamaño de lista = 1)
            if (list.size() == 1) {
                // Obtiene el primer (y único) producto de la lista
                ModelCliente data = list.get(0);
                // Crea una nueva instancia del formulario de creación/edición
                CreateClientes create = new CreateClientes();
                // Carga los datos del producto seleccionado en el formulario de edición
                create.loadData(service, data);

                // Define las opciones para el diálogo modal (Cancelar y Actualizar)
                SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                        new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                        new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION),
                        new SimpleModalBorder.Option("Activar", SimpleModalBorder.DEFAULT_OPTION) // Opción activar
                                                                                                  // Cliente
                };

                JButton[] btnActualizarRef = new JButton[1];
                JButton[] btnCancelarRef = new JButton[1];
                JButton[] btnActivarRef = new JButton[1];

                // Muestra el diálogo modal para editar el producto
                ModalDialog.showModal(
                        this, // Componente padre (probablemente un JFrame o JPanel)
                        new SimpleModalBorder(
                                create, // Panel de contenido (el formulario de edición)
                                "Editar Cliente: " + data.getNombre(), // Título del diálogo
                                options, // Opciones de botones
                                // Manejador de eventos para los botones
                                (mc, i) -> {
                                    // Si se hace clic en el botón OK (Actualizar)
                                    if (i == SimpleModalBorder.OK_OPTION) {
                                        JButton btnActualizar = btnActualizarRef[0];
                                        JButton btnCancelar = btnCancelarRef[0];
                                        JButton btnActivar = btnActivarRef[0];
                                        setEnabledSafe(btnActualizar, false);
                                        setEnabledSafe(btnCancelar, false);
                                        setEnabledSafe(btnActivar, false);
                                        runModalActionWithButtonLoading(
                                                btnActualizar,
                                                null,
                                                () -> {
                                                    try {
                                                        ModelCliente dataEdit = create.getData();
                                                        dataEdit.setIdCliente(data.getIdCliente());
                                                        service.update(dataEdit);
                                                    } catch (SQLException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                },
                                                () -> {
                                                    Toast.show(this, Toast.Type.SUCCESS, "Cliente actualizado");
                                                    try {
                                                        loadData();
                                                    } catch (SQLException ex) {
                                                        Toast.show(this, Toast.Type.ERROR,
                                                                "Error al cargar datos: " + ex.getMessage());
                                                    }
                                                    mc.close();
                                                },
                                                () -> {
                                                    setEnabledSafe(btnActualizar, true);
                                                    setEnabledSafe(btnCancelar, true);
                                                    setEnabledSafe(btnActivar, true);
                                                    Toast.show(this, Toast.Type.ERROR, "Error al actualizar cliente");
                                                });

                                        // Cuando se desea activar el cliente de nuevo
                                    } else if (i == SimpleModalBorder.DEFAULT_OPTION) {
                                        JButton btnActivar = btnActivarRef[0];
                                        JButton btnActualizar = btnActualizarRef[0];
                                        JButton btnCancelar = btnCancelarRef[0];
                                        setEnabledSafe(btnActivar, false);
                                        setEnabledSafe(btnActualizar, false);
                                        setEnabledSafe(btnCancelar, false);
                                        runModalActionWithButtonLoading(
                                                btnActivar,
                                                null,
                                                () -> {
                                                    try {
                                                        for (ModelCliente d : list) {
                                                            service.active(d.getIdCliente());
                                                        }
                                                    } catch (SQLException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                },
                                                () -> {
                                                    Toast.show(this, Toast.Type.SUCCESS, "Cliente activado");
                                                    try {
                                                        loadData();
                                                    } catch (SQLException ex) {
                                                        Toast.show(this, Toast.Type.ERROR,
                                                                "Error al cargar datos: " + ex.getMessage());
                                                    }
                                                    mc.close();
                                                },
                                                () -> {
                                                    setEnabledSafe(btnActivar, true);
                                                    setEnabledSafe(btnActualizar, true);
                                                    setEnabledSafe(btnCancelar, true);
                                                    Toast.show(this, Toast.Type.ERROR, "Error al activar cliente");
                                                });
                                    } // Cuando el diálogo se abre (evento OPENED)
                                    else if (i == SimpleModalBorder.OPENED) {
                                        // Inicializa el formulario
                                        create.init();
                                        Window w = SwingUtilities.getWindowAncestor(create);
                                        btnActualizarRef[0] = findButtonByText(w, "Actualizar");
                                        btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                        btnActivarRef[0] = findButtonByText(w, "Activar");
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
                                        styleActionButton(
                                                btnActivarRef[0],
                                                new Color(0, 122, 255),
                                                new Color(10, 132, 255),
                                                new Color(0, 111, 224),
                                                dashboardIcon("user.svg", 16, Color.WHITE));
                                    }
                                }));
            } // Si hay más de un producto seleccionado
            else {
                // Muestra advertencia indicando que solo se puede editar un producto a la vez
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo un producto");
            }
        } // Si no hay productos seleccionados
        else {
            // Muestra advertencia indicando que se debe seleccionar un producto para editar
            Toast.show(this, Toast.Type.WARNING, "Seleccione un producto para editar");
        }
    }// GEN-LAST:event_btnModificarActionPerformed

    private void btnDesactivarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDesactivarActionPerformed
        // Obtiene la lista de empleados seleccionados (probablemente de una tabla o
        // vista)
        List<ModelCliente> list = getSelectedData();
        if (!list.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Eliminar", SimpleModalBorder.OK_OPTION)
            };

            JButton[] btnEliminarRef = new JButton[1];
            JButton[] btnCancelarRef = new JButton[1];

            JLabel label = new JLabel("¿Está seguro de desactivar " + list.size() + " cliente(s)?");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar desactivacion",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    JButton btnEliminar = btnEliminarRef[0];
                                    JButton btnCancelar = btnCancelarRef[0];
                                    setEnabledSafe(btnEliminar, false);
                                    setEnabledSafe(btnCancelar, false);
                                    runModalActionWithButtonLoading(
                                            btnEliminar,
                                            null,
                                            () -> {
                                                try {
                                                    for (ModelCliente d : list) {
                                                        service.delete(d.getIdCliente());
                                                    }
                                                } catch (SQLException ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                            },
                                            () -> {
                                                Toast.show(this, Toast.Type.SUCCESS, "Cliente(s) desactivado(s)");
                                                try {
                                                    loadData();
                                                } catch (SQLException ex) {
                                                    Toast.show(this, Toast.Type.ERROR,
                                                            "Error al cargar datos: " + ex.getMessage());
                                                }
                                                mc.close();
                                            },
                                            () -> {
                                                setEnabledSafe(btnEliminar, true);
                                                setEnabledSafe(btnCancelar, true);
                                                Toast.show(this, Toast.Type.ERROR, "Error al desactivar cliente(s)");
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
    }// GEN-LAST:event_btnDesactivarActionPerformed
     // Declara un método que devuelve una lista de objetos ModelProduct

    private List<ModelCliente> getSelectedData() {

        // Crea una nueva lista vacía para almacenar los productos seleccionados
        List<ModelCliente> list = new ArrayList<>();

        // Recorre todas las filas de la tabla (desde la fila 0 hasta la última)
        for (int i = 0; i < table.getRowCount(); i++) {

            // Verifica si el valor de la primera columna (columna 0) es true
            // (marcado/seleccionado)
            if ((boolean) table.getValueAt(i, 0)) {
                // Obtiene el objeto ModelProduct de la tercera columna (columna 2) de la fila
                // actual
                ModelCliente data = (ModelCliente) table.getValueAt(i, 2);
                // System.out.println(data);
                list.add(data);
            }
        }

        // Devuelve la lista con todos los productos que estaban seleccionados en la
        // tabla
        return list;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnDesactivar;
    private javax.swing.JButton btnModificar;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
