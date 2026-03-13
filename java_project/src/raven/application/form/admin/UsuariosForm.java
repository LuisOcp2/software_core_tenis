package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.awt.Window;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.LoadingOverlayHelper;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.conexion;
import raven.clases.admin.ServiceUser;
import raven.clases.admin.UserSession;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalController;

/**
 *
 * @author Raven
 */
public class UsuariosForm extends javax.swing.JPanel {

    private final ServiceUser servicesUser = new ServiceUser();
    private final FontIcon iconAjustar;
    private final FontIcon iconNuevo;
    private final FontIcon iconDesactivar;

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

    public UsuariosForm() {
        initComponents();

        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");

        // Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.USER_EDIT, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.USER_COG, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.USER_ALT_SLASH, tabTextColor);

        btnCrear.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41"); // Color de fondo
        btnEditar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FFCC00"); // Color de fondo
        btnDesactivar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FF453A"); // Color de fondo

        btnCrear.setIcon(iconNuevo);
        btnEditar.setIcon(iconAjustar);
        btnDesactivar.setIcon(iconDesactivar);

        // Fin de diseño de botones
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
        // Renderizador especial para el estado (Activo/No Activo)
        // 2. Crea un renderizador personalizado solo para la columna Estado (índice 7)
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                // Convertir el valor a texto según su tipo
                String estadoTexto;

                if (value instanceof Boolean) {
                    // Si es un Boolean (lo que esperamos normalmente)
                    estadoTexto = (Boolean) value ? "Activo" : "Inactivo";
                } else if (value instanceof String) {
                    // Si ya es un String (posiblemente de una operación anterior)
                    estadoTexto = value.toString();
                } else {
                    // Valor por defecto en caso de otro tipo
                    estadoTexto = "Inactivo";
                }

                // Configurar el componente con el texto del estado
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

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
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
        List<ModelUser> list;
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && !session.hasPermission("ver_todos_usuarios")) {
            list = servicesUser.search(search, session.getIdBodegaUsuario());
        } else {
            list = servicesUser.search(search);
        }

        // Carga los nombres de bodega de forma optimizada
        Map<Integer, String> bodegaNombres = cargarNombresBodegas();

        // Añade empleados coincidentes como filas en la tabla
        for (ModelUser d : list) {
            model.addRow(d.toTableRowOptimizado(table.getRowCount() + 1, bodegaNombres));
        }
    }

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
            List<ModelUser> list;
            UserSession session = UserSession.getInstance();
            if (session.isLoggedIn() && !session.hasPermission("ver_todos_usuarios")) {
                list = servicesUser.obtenerUsuariosPorBodega(session.getIdBodegaUsuario());
            } else {
                list = servicesUser.obtenerTodosLosUsuarios();
            }

            // Carga los nombres de bodega de forma optimizada
            Map<Integer, String> bodegaNombres = cargarNombresBodegas();

            // Añade cada empleado como fila en la tabla
            for (ModelUser d : list) {
                model.addRow(d.toTableRowOptimizado(table.getRowCount() + 1, bodegaNombres));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());
        }
    }

    /**
     * Método optimizado para cargar todos los nombres de bodega de una sola vez
     */
    private Map<Integer, String> cargarNombresBodegas() {
        Map<Integer, String> bodegaNombres = new HashMap<>();
        try {
            conexion dbConnection = conexion.getInstance();
            try {
                dbConnection.connectToDatabase();
            } catch (SQLException ex) {
                // Si ya está conectado, ignoramos la excepción
            }

            try (java.sql.Connection conn = dbConnection.createConnection();
                    java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT id_bodega, nombre FROM bodegas");
                    java.sql.ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int idBodega = rs.getInt("id_bodega");
                    String nombreBodega = rs.getString("nombre");
                    bodegaNombres.put(idBodega, nombreBodega);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(UsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Añadir valor por defecto para bodegas no encontradas
        bodegaNombres.put(0, "Sin bodega asignada");
        bodegaNombres.put(-1, "Sin bodega asignada");

        return bodegaNombres;
    }

    private List<ModelUser> getSelectedData() {
        List<ModelUser> list = new ArrayList<>();

        for (int i = 0; i < table.getRowCount(); i++) {
            if ((boolean) table.getValueAt(i, 0)) {
                // Obtener los datos individuales de cada columna (índices actualizados)
                int idUsuario = (Integer) table.getValueAt(i, 1);
                String username = (String) table.getValueAt(i, 2);
                String nombre = (String) table.getValueAt(i, 3);
                String email = (String) table.getValueAt(i, 4);
                String rol = (String) table.getValueAt(i, 5);
                String nombreBodega = (String) table.getValueAt(i, 6); // Columna 6 para bodega

                // Determinar el estado según el valor en la tabla (columna 7)
                boolean activo;
                Object estadoObj = table.getValueAt(i, 7);

                if (estadoObj instanceof Boolean) {
                    activo = (Boolean) estadoObj;
                } else if (estadoObj instanceof String) {
                    activo = estadoObj.toString().equals("Activo");
                } else {
                    activo = false; // Valor por defecto
                }

                // Obtener idBodega basado en el nombre de la bodega
                Integer idBodega = obtenerIdBodegaPorNombre(nombreBodega);

                // Crear un nuevo objeto ModelUser con los datos obtenidos incluyendo idBodega
                ModelUser user = new ModelUser(idUsuario, username, null, nombre, email, rol, "tienda", idBodega,
                        activo);
                list.add(user);
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
        lb.setText("Gestion de Usuarios");

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { false, null, null, null, null, null, null, null },
                        { false, null, null, null, null, null, null, null },
                        { false, null, null, null, null, null, null, null },
                        { false, null, null, null, null, null, null, null }
                },
                new String[] {
                        "Seleccionar", "Codigo", "Nombre Usuario", "Nombre", "Correo", "rol", "Bodega", "Estado"
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

        lbTitle.setText("Usuarios");

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
                                                        502, Short.MAX_VALUE)
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
            Logger.getLogger(UsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// GEN-LAST:event_txtSearchKeyReleased

    private void btnCrearActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCrearActionPerformed
        CreateUsuarios cr = new CreateUsuarios();
        // Carga los datos en la instancia 'cr' utilizando el servicio proporcionado y
        // un valor nulo
        cr.loadData(null);

        // Crea un arreglo de opciones para el modal, que incluye un botón de "Cancelar"
        // y otro de "Guardar"
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION), // Opción para cancelar
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION) // Opción para guardar
        };

        JButton[] btnGuardarRef = new JButton[1];
        JButton[] btnCancelarRef = new JButton[1];

        ModalDialog.showModal(this,
                new SimpleModalBorder(cr, "Nuevo Usuario", options, (ModalController mc, int i) -> {
                    if (i == SimpleModalBorder.OK_OPTION) {
                        if (!cr.validarCampos()) {
                            return;
                        }
                        JButton btnGuardar = btnGuardarRef[0];
                        JButton btnCancelar = btnCancelarRef[0];

                        setEnabledDeep(cr, false);
                        setEnabledSafe(btnGuardar, false);
                        setEnabledSafe(btnCancelar, false);

                        runModalActionWithModalLoading(
                                cr,
                                "Creando usuario...",
                                () -> {
                                    try {
                                        servicesUser.insertarUsuario(cr.getData());
                                    } catch (SQLException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                },
                                () -> {
                                    Toast.show(UsuariosForm.this, Toast.Type.SUCCESS, "Usuario creado exitosamente");
                                    try {
                                        loadData();
                                    } catch (SQLException ex) {
                                        Toast.show(UsuariosForm.this, Toast.Type.ERROR,
                                                "Error al cargar datos: " + ex.getMessage());
                                    }
                                    mc.close();
                                },
                                () -> {
                                    setEnabledDeep(cr, true);
                                    setEnabledSafe(btnGuardar, true);
                                    setEnabledSafe(btnCancelar, true);
                                    Toast.show(UsuariosForm.this, Toast.Type.ERROR, "Error al crear Usuario");
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
                                new Color(75, 85, 99),
                                new Color(55, 65, 81),
                                new Color(31, 41, 55),
                                dashboardIcon("x-circle.svg", 16, Color.WHITE));
                    }
                }));
    }// GEN-LAST:event_btnCrearActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnEditarActionPerformed
        List<ModelUser> list = getSelectedData();

        if (!list.isEmpty()) {
            if (list.size() == 1) {
                ModelUser data = list.get(0);

                // Si el usuario está desactivado, pregunta si quiere activarlo
                if (!data.isActivo()) {
                    SimpleModalBorder.Option[] optionsActivar = new SimpleModalBorder.Option[] {
                            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                            new SimpleModalBorder.Option("Activar Usuario", SimpleModalBorder.OK_OPTION), };

                    JLabel labelActivar = new JLabel("¿Desea activar este usuario?");
                    labelActivar.setBorder(new EmptyBorder(5, 25, 5, 25));

                    JButton[] btnActivarRef = new JButton[1];
                    JButton[] btnCancelarRef = new JButton[1];

                    ModalDialog.showModal(
                            this,
                            new SimpleModalBorder(
                                    labelActivar,
                                    "Activar Usuario: " + data.getNombre(),
                                    optionsActivar,
                                    (mc, i) -> {
                                        if (i == SimpleModalBorder.OK_OPTION) {
                                            JButton btnActivar = btnActivarRef[0];
                                            JButton btnCancelar = btnCancelarRef[0];
                                            setEnabledSafe(btnActivar, false);
                                            setEnabledSafe(btnCancelar, false);
                                            runModalActionWithModalLoading(
                                                    labelActivar,
                                                    "Activando usuario...",
                                                    () -> {
                                                        try {
                                                            servicesUser.activarUsuario(data.getIdUsuario());
                                                        } catch (SQLException ex) {
                                                            throw new RuntimeException(ex);
                                                        }
                                                    },
                                                    () -> {
                                                        Toast.show(this, Toast.Type.SUCCESS,
                                                                "Usuario activado exitosamente");
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
                                                        setEnabledSafe(btnCancelar, true);
                                                        Toast.show(this, Toast.Type.ERROR, "Error al activar usuario");
                                                    });
                                        } else if (i == SimpleModalBorder.OPENED) {
                                            Window w = SwingUtilities.getWindowAncestor(labelActivar);
                                            btnActivarRef[0] = findButtonByText(w, "Activar Usuario");
                                            btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                            styleActionButton(
                                                    btnActivarRef[0],
                                                    new Color(40, 205, 65),
                                                    new Color(52, 199, 89),
                                                    new Color(32, 185, 58),
                                                    dashboardIcon("user.svg", 16, Color.WHITE));
                                            styleActionButton(
                                                    btnCancelarRef[0],
                                                    new Color(75, 85, 99),
                                                    new Color(55, 65, 81),
                                                    new Color(31, 41, 55),
                                                    dashboardIcon("x-circle.svg", 16, Color.WHITE));
                                        }
                                    }));
                    return; // Salir del método para no mostrar el diálogo de edición normal
                }

                // Continuar con el proceso normal para usuarios activos
                CreateUsuarios create = new CreateUsuarios();
                create.loadData(data);

                SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                        new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                        new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION), };

                JButton[] btnActualizarRef = new JButton[1];
                JButton[] btnCancelarRef = new JButton[1];

                ModalDialog.showModal(
                        this,
                        new SimpleModalBorder(
                                create,
                                "Editar Usuario: " + data.getNombre(),
                                options,
                                (mc, i) -> {
                                    if (i == SimpleModalBorder.OK_OPTION) {
                                        if (!create.validarCampos()) {
                                            return;
                                        }
                                        JButton btnActualizar = btnActualizarRef[0];
                                        JButton btnCancelar = btnCancelarRef[0];

                                        setEnabledDeep(create, false);
                                        setEnabledSafe(btnActualizar, false);
                                        setEnabledSafe(btnCancelar, false);

                                        runModalActionWithModalLoading(
                                                create,
                                                "Actualizando usuario...",
                                                () -> {
                                                    try {
                                                        ModelUser dataEdit = create.getData();
                                                        dataEdit.setIdUsuario(data.getIdUsuario());
                                                        servicesUser.modificarUsuario(dataEdit);
                                                    } catch (SQLException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                },
                                                () -> {
                                                    Toast.show(this, Toast.Type.SUCCESS, "Usuario actualizado");
                                                    try {
                                                        loadData();
                                                    } catch (SQLException ex) {
                                                        Toast.show(this, Toast.Type.ERROR,
                                                                "Error al cargar datos: " + ex.getMessage());
                                                    }
                                                    mc.close();
                                                },
                                                () -> {
                                                    setEnabledDeep(create, true);
                                                    setEnabledSafe(btnActualizar, true);
                                                    setEnabledSafe(btnCancelar, true);
                                                    Toast.show(this, Toast.Type.ERROR, "Error al actualizar usuario");
                                                });
                                    } else if (i == SimpleModalBorder.OPENED) {
                                        create.init();
                                        Window w = SwingUtilities.getWindowAncestor(create);
                                        btnActualizarRef[0] = findButtonByText(w, "Actualizar");
                                        btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                        styleActionButton(
                                                btnActualizarRef[0],
                                                new Color(255, 204, 0),
                                                new Color(255, 214, 10),
                                                new Color(236, 180, 0),
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
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo un Usuario");
            }
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un Usuario para editar");
        }
    }// GEN-LAST:event_btnEditarActionPerformed

    private void btnDesactivarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDesactivarActionPerformed
        // Obtiene la lista de empleados seleccionados (probablemente de una tabla o
        // vista)
        List<ModelUser> list = getSelectedData();
        if (!list.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Desactivar", SimpleModalBorder.OK_OPTION)
            };

            JLabel label = new JLabel("¿Está seguro de desactivar " + list.size() + " Usuario(s)?");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            JButton[] btnDesactivarRef = new JButton[1];
            JButton[] btnCancelarRef = new JButton[1];

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar desactivacion",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    JButton btnDesactivar = btnDesactivarRef[0];
                                    JButton btnCancelar = btnCancelarRef[0];
                                    setEnabledSafe(btnDesactivar, false);
                                    setEnabledSafe(btnCancelar, false);
                                    runModalActionWithModalLoading(
                                            label,
                                            "Desactivando usuario(s)...",
                                            () -> {
                                                try {
                                                    for (ModelUser d : list) {
                                                        servicesUser.desactivar(d.getIdUsuario());
                                                    }
                                                } catch (SQLException ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                            },
                                            () -> {
                                                Toast.show(this, Toast.Type.SUCCESS, "Usuario(s) desactivado(s)");
                                                try {
                                                    loadData();
                                                } catch (SQLException ex) {
                                                    Toast.show(this, Toast.Type.ERROR,
                                                            "Error al cargar datos: " + ex.getMessage());
                                                }
                                                mc.close();
                                            },
                                            () -> {
                                                setEnabledSafe(btnDesactivar, true);
                                                setEnabledSafe(btnCancelar, true);
                                                Toast.show(this, Toast.Type.ERROR, "Error al desactivar usuario(s)");
                                            });
                                } else if (i == SimpleModalBorder.OPENED) {
                                    Window w = SwingUtilities.getWindowAncestor(label);
                                    btnDesactivarRef[0] = findButtonByText(w, "Desactivar");
                                    btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                    styleActionButton(
                                            btnDesactivarRef[0],
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
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un Usuario");
        }
    }// GEN-LAST:event_btnDesactivarActionPerformed

    /**
     * Método helper para obtener el ID de bodega basado en su nombre
     */
    private Integer obtenerIdBodegaPorNombre(String nombreBodega) {
        if (nombreBodega == null || nombreBodega.equals("Sin bodega asignada")) {
            return null;
        }

        try {
            conexion dbConnection = conexion.getInstance();
            try {
                dbConnection.connectToDatabase();
            } catch (SQLException ex) {
                // Si ya está conectado, ignoramos la excepción
            }

            try (java.sql.Connection conn = dbConnection.createConnection();
                    java.sql.PreparedStatement stmt = conn
                            .prepareStatement("SELECT id_bodega FROM bodegas WHERE nombre = ?")) {

                stmt.setString(1, nombreBodega);
                java.sql.ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id_bodega");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(UsuariosForm.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCrear;
    private javax.swing.JButton btnDesactivar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

}
