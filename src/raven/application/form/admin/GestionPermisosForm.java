package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.admin.ServicePermisos;
import raven.clases.admin.ServiceRoles;
import raven.clases.admin.UserSession;
import raven.dao.PrivilegioUsuarioDAO;
import raven.dao.RolesDAO;
import raven.modelos.Permiso;
import raven.modelos.PrivilegioRol;
import raven.modelos.PrivilegioUsuario;
import raven.modelos.Rol;
import raven.modelos.Usuario;

/**
 * Formulario de gestión de roles y permisos del sistema.
 * UI mejorada con iconos FontAwesome y esquema de colores moderno.
 * 
 * @author CrisDEV
 * @version 2.0
 */
public class GestionPermisosForm extends JPanel {

    private final ServiceRoles serviceRoles;
    private final ServicePermisos servicePermisos;
    private final RolesDAO rolesDAO;
    private final PrivilegioUsuarioDAO privilegioUsuarioDAO;

    private JTabbedPane tabbedPane;
    private JTable tableRoles;
    private DefaultTableModel modelRoles;
    private JComboBox<Rol> cbxRoles;
    private Map<String, JPanel> categoriasPaneles;
    private Map<String, Map<String, PermissionCheckboxes>> permisosCheckboxes;

    // Componentes para permisos personalizados
    private JComboBox<Usuario> cbxUsuarios;
    private JTable tablePermisosUsuario;
    private DefaultTableModel modelPermisosUsuario;

    // Colores del tema
    private static final Color ACCENT_PRIMARY = new Color(66, 133, 244); // Azul intenso
    private static final Color ACCENT_SUCCESS = new Color(52, 168, 83); // Verde
    private static final Color ACCENT_WARNING = new Color(251, 188, 4); // Amarillo
    private static final Color ACCENT_DANGER = new Color(234, 67, 53); // Rojo
    private static final Color CARD_BG = new Color(45, 52, 60); // Fondo oscuro para paneles
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255); // Texto blanco
    private static final Color TEXT_SECONDARY = new Color(200, 200, 200); // Texto gris claro

    public GestionPermisosForm() {
        this.serviceRoles = new ServiceRoles();
        this.servicePermisos = new ServicePermisos();
        this.rolesDAO = new RolesDAO();
        this.privilegioUsuarioDAO = new PrivilegioUsuarioDAO();
        this.categoriasPaneles = new HashMap<>();
        this.permisosCheckboxes = new HashMap<>();

        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Título con icono
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(FontIcon.of(FontAwesomeSolid.SHIELD_ALT, 32, ACCENT_PRIMARY));
        JLabel lblTitulo = new JLabel("Gestión de Roles y Permisos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(ACCENT_PRIMARY);

        headerPanel.add(iconLabel);
        headerPanel.add(lblTitulo);

        add(headerPanel, BorderLayout.NORTH);

        // Pestañas con estilo mejorado
        tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_HEIGHT, 45);
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabsPopupPolicy:asNeeded");

        tabbedPane.addTab("Gestión de Roles",
                FontIcon.of(FontAwesomeSolid.USERS_COG, 16, ACCENT_PRIMARY),
                crearPanelGestionRoles());
        tabbedPane.addTab("Asignación de Permisos",
                FontIcon.of(FontAwesomeSolid.KEY, 16, ACCENT_SUCCESS),
                crearPanelAsignacionPermisos());
        tabbedPane.addTab("Permisos Personalizados",
                FontIcon.of(FontAwesomeSolid.USER_SHIELD, 16, ACCENT_WARNING),
                crearPanelPermisosPersonalizados());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel crearPanelGestionRoles() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Toolbar mejorado
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        toolbar.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:$Panel.background");

        // Botón Nuevo Rol
        JButton btnNuevoRol = crearBotonAccion(
                "Nuevo Rol",
                FontIcon.of(FontAwesomeSolid.PLUS_CIRCLE, 18, Color.WHITE),
                ACCENT_SUCCESS);
        btnNuevoRol.addActionListener(e -> mostrarDialogoNuevoRol());

        // Botón Editar Rol
        JButton btnEditarRol = crearBotonAccion(
                "Editar",
                FontIcon.of(FontAwesomeSolid.EDIT, 18, Color.WHITE),
                ACCENT_PRIMARY);
        btnEditarRol.addActionListener(e -> editarRolSeleccionado());

        // Botón Clonar Rol
        JButton btnClonarRol = crearBotonAccion(
                "Clonar",
                FontIcon.of(FontAwesomeSolid.COPY, 18, Color.WHITE),
                ACCENT_WARNING);
        btnClonarRol.addActionListener(e -> clonarRolSeleccionado());

        // Botón Eliminar Rol
        JButton btnEliminarRol = crearBotonAccion(
                "Eliminar",
                FontIcon.of(FontAwesomeSolid.TRASH_ALT, 18, Color.WHITE),
                ACCENT_DANGER);
        btnEliminarRol.addActionListener(e -> eliminarRolSeleccionado());

        // Botón Refrescar
        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.setIcon(FontIcon.of(FontAwesomeSolid.SYNC_ALT, 16, ACCENT_PRIMARY));
        btnRefrescar.putClientProperty(FlatClientProperties.BUTTON_TYPE, "borderless");
        btnRefrescar.addActionListener(e -> cargarRoles());

        toolbar.add(btnNuevoRol);
        toolbar.add(btnEditarRol);
        toolbar.add(btnClonarRol);
        toolbar.add(btnEliminarRol);
        toolbar.add(Box.createHorizontalStrut(30));
        toolbar.add(btnRefrescar);

        panel.add(toolbar, BorderLayout.NORTH);

        // Tabla de roles con estilo mejorado
        String[] columnas = { "ID", "Nombre", "Descripción", "# Permisos", "# Usuarios", "Sistema" };
        modelRoles = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableRoles = new JTable(modelRoles);
        tableRoles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRoles.setRowHeight(40);
        tableRoles.setShowGrid(false);
        tableRoles.setIntercellSpacing(new Dimension(0, 5));
        tableRoles.putClientProperty(FlatClientProperties.STYLE,
                "selectionBackground:$Table.selectionBackground;" +
                        "selectionForeground:$Table.selectionForeground");

        // Configurar anchos de columnas
        tableRoles.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableRoles.getColumnModel().getColumn(1).setPreferredWidth(180);
        tableRoles.getColumnModel().getColumn(2).setPreferredWidth(350);
        tableRoles.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableRoles.getColumnModel().getColumn(4).setPreferredWidth(100);
        tableRoles.getColumnModel().getColumn(5).setPreferredWidth(80);

        // Renderer personalizado para la columna "Sistema"
        tableRoles.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                if ("Sí".equals(value)) {
                    label.setIcon(FontIcon.of(FontAwesomeSolid.LOCK, 12, ACCENT_WARNING));
                } else {
                    label.setIcon(FontIcon.of(FontAwesomeSolid.LOCK_OPEN, 12, ACCENT_SUCCESS));
                }
                label.setHorizontalAlignment(CENTER);
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tableRoles);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelAsignacionPermisos() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Selector de rol mejorado
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:$Panel.background");

        JLabel lblRol = new JLabel("Rol:");
        lblRol.setIcon(FontIcon.of(FontAwesomeSolid.USER_TAG, 18, ACCENT_PRIMARY));
        lblRol.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(lblRol);

        cbxRoles = new JComboBox<>();
        cbxRoles.setPreferredSize(new Dimension(280, 35));
        cbxRoles.putClientProperty(FlatClientProperties.STYLE,
                "arc:8");
        cbxRoles.addActionListener(e -> cargarPermisosDeRol());
        topPanel.add(cbxRoles);

        topPanel.add(Box.createHorizontalStrut(20));

        JButton btnGuardar = crearBotonAccion(
                "Guardar Cambios",
                FontIcon.of(FontAwesomeSolid.SAVE, 18, Color.WHITE),
                ACCENT_SUCCESS);
        btnGuardar.addActionListener(e -> guardarPermisos());
        topPanel.add(btnGuardar);

        panel.add(topPanel, BorderLayout.NORTH);

        // Panel de permisos con scroll
        JPanel permisosPanel = new JPanel();
        permisosPanel.setLayout(new BoxLayout(permisosPanel, BoxLayout.Y_AXIS));
        permisosPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        try {
            Map<String, List<Permiso>> permisosPorCategoria = servicePermisos.obtenerPermisosAgrupadosPorCategoria();

            for (Map.Entry<String, List<Permiso>> entry : permisosPorCategoria.entrySet()) {
                String categoria = entry.getKey();
                List<Permiso> permisos = entry.getValue();

                JPanel categoriaPanel = crearPanelCategoria(categoria, permisos);
                permisosPanel.add(categoriaPanel);
                permisosPanel.add(Box.createVerticalStrut(15));

                categoriasPaneles.put(categoria, categoriaPanel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error cargando permisos: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        JScrollPane scrollPane = new JScrollPane(permisosPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelCategoria(String categoria, List<Permiso> permisos) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(CARD_BG);
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;" +
                        "background:$Login.background");

        // Header de categoría con icono
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        headerPanel.setOpaque(false);

        FontIcon categoriaIcon = obtenerIconoCategoria(categoria);
        JLabel lblCategoria = new JLabel(categoria.toUpperCase());
        lblCategoria.setIcon(categoriaIcon);
        lblCategoria.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCategoria.setForeground(TEXT_PRIMARY);

        headerPanel.add(lblCategoria);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Grid de permisos
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        // Headers con iconos
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        JLabel lblModulo = new JLabel("Módulo");
        lblModulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblModulo.setForeground(TEXT_SECONDARY);
        gridPanel.add(lblModulo, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.125;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel lblVer = new JLabel("Ver");
        lblVer.setIcon(FontIcon.of(FontAwesomeSolid.EYE, 12, ACCENT_PRIMARY));
        lblVer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblVer.setForeground(ACCENT_PRIMARY);
        lblVer.setHorizontalAlignment(SwingConstants.CENTER);
        gridPanel.add(lblVer, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.125;
        JLabel lblCrear = new JLabel("Crear");
        lblCrear.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 12, ACCENT_SUCCESS));
        lblCrear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCrear.setForeground(ACCENT_SUCCESS);
        lblCrear.setHorizontalAlignment(SwingConstants.CENTER);
        gridPanel.add(lblCrear, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.125;
        JLabel lblEditar = new JLabel("Editar");
        lblEditar.setIcon(FontIcon.of(FontAwesomeSolid.EDIT, 12, ACCENT_WARNING));
        lblEditar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEditar.setForeground(ACCENT_WARNING);
        lblEditar.setHorizontalAlignment(SwingConstants.CENTER);
        gridPanel.add(lblEditar, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.125;
        JLabel lblEliminar = new JLabel("Eliminar");
        lblEliminar.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 12, ACCENT_DANGER));
        lblEliminar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEliminar.setForeground(ACCENT_DANGER);
        lblEliminar.setHorizontalAlignment(SwingConstants.CENTER);
        gridPanel.add(lblEliminar, gbc);

        int row = 1;
        Map<String, PermissionCheckboxes> checkboxesCategoria = new HashMap<>();

        for (Permiso permiso : permisos) {
            gbc.gridy = row;

            // Nombre del módulo
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel lblPermisoNombre = new JLabel(permiso.getNombreMostrar());
            lblPermisoNombre.setToolTipText(permiso.getDescripcion());
            lblPermisoNombre.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblPermisoNombre.setForeground(TEXT_PRIMARY);
            gridPanel.add(lblPermisoNombre, gbc);

            // Checkboxes con estilo
            PermissionCheckboxes checkboxes = new PermissionCheckboxes();
            checkboxes.permiso = permiso;

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.CENTER;
            checkboxes.cbVer = crearCheckboxEstilizado();
            gridPanel.add(checkboxes.cbVer, gbc);

            gbc.gridx = 2;
            checkboxes.cbCrear = crearCheckboxEstilizado();
            gridPanel.add(checkboxes.cbCrear, gbc);

            gbc.gridx = 3;
            checkboxes.cbEditar = crearCheckboxEstilizado();
            gridPanel.add(checkboxes.cbEditar, gbc);

            gbc.gridx = 4;
            checkboxes.cbEliminar = crearCheckboxEstilizado();
            gridPanel.add(checkboxes.cbEliminar, gbc);

            checkboxesCategoria.put(permiso.getModulo(), checkboxes);
            row++;
        }

        permisosCheckboxes.put(categoria, checkboxesCategoria);
        panel.add(gridPanel, BorderLayout.CENTER);

        return panel;
    }

    private JCheckBox crearCheckboxEstilizado() {
        JCheckBox checkbox = new JCheckBox();
        // Remover estilo arc ya que FlatLaf no lo soporta para JCheckBox
        return checkbox;
    }

    private FontIcon obtenerIconoCategoria(String categoria) {
        switch (categoria.toLowerCase()) {
            case "comercial":
                return FontIcon.of(FontAwesomeSolid.SHOPPING_CART, 16, ACCENT_PRIMARY);
            case "inventario":
                return FontIcon.of(FontAwesomeSolid.BOXES, 16, ACCENT_PRIMARY);
            case "caja":
                return FontIcon.of(FontAwesomeSolid.CASH_REGISTER, 16, ACCENT_PRIMARY);
            case "compras":
                return FontIcon.of(FontAwesomeSolid.SHOPPING_BAG, 16, ACCENT_PRIMARY);
            case "reportes":
                return FontIcon.of(FontAwesomeSolid.CHART_BAR, 16, ACCENT_PRIMARY);
            case "admin":
                return FontIcon.of(FontAwesomeSolid.COGS, 16, ACCENT_PRIMARY);
            default:
                return FontIcon.of(FontAwesomeSolid.FOLDER, 16, ACCENT_PRIMARY);
        }
    }

    private JButton crearBotonAccion(String texto, FontIcon icono, Color colorBase) {
        JButton btn = new JButton(texto);
        btn.setIcon(icono);
        btn.setBackground(colorBase);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;" +
                        "borderWidth:0;" +
                        "focusWidth:0;" +
                        "innerFocusWidth:0");

        // Efectos hover
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(colorBase.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(colorBase);
            }
        });

        return btn;
    }

    private void cargarDatos() {
        cargarRoles();
        cargarComboRoles();
    }

    private void cargarRoles() {
        try {
            modelRoles.setRowCount(0);
            List<Rol> roles = rolesDAO.obtenerTodosRoles();

            for (Rol rol : roles) {
                int numPermisos = rolesDAO.obtenerPermisosDeRol(rol.getIdRol()).size();
                int numUsuarios = rolesDAO.contarUsuariosConRol(rol.getIdRol());

                modelRoles.addRow(new Object[] {
                        rol.getIdRol(),
                        rol.getNombre(),
                        rol.getDescripcion(),
                        numPermisos,
                        numUsuarios,
                        rol.isEsSistema() ? "Sí" : "No"
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error cargando roles: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarComboRoles() {
        try {
            cbxRoles.removeAllItems();
            List<Rol> roles = rolesDAO.obtenerTodosRoles();
            for (Rol rol : roles) {
                cbxRoles.addItem(rol);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarPermisosDeRol() {
        Rol rolSeleccionado = (Rol) cbxRoles.getSelectedItem();
        if (rolSeleccionado == null)
            return;

        try {
            limpiarCheckboxes();
            Map<String, PrivilegioRol> privilegios = rolesDAO.obtenerPrivilegiosDeRol(rolSeleccionado.getIdRol());

            for (Map.Entry<String, Map<String, PermissionCheckboxes>> categoriaEntry : permisosCheckboxes.entrySet()) {
                for (Map.Entry<String, PermissionCheckboxes> permisoEntry : categoriaEntry.getValue().entrySet()) {
                    String modulo = permisoEntry.getKey();
                    PermissionCheckboxes checkboxes = permisoEntry.getValue();

                    PrivilegioRol privilegio = privilegios.get(modulo.toLowerCase());
                    if (privilegio != null) {
                        checkboxes.cbVer.setSelected(privilegio.isPuedeVer());
                        checkboxes.cbCrear.setSelected(privilegio.isPuedeCrear());
                        checkboxes.cbEditar.setSelected(privilegio.isPuedeEditar());
                        checkboxes.cbEliminar.setSelected(privilegio.isPuedeEliminar());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error cargando permisos: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCheckboxes() {
        for (Map<String, PermissionCheckboxes> categoriaMap : permisosCheckboxes.values()) {
            for (PermissionCheckboxes checkboxes : categoriaMap.values()) {
                checkboxes.cbVer.setSelected(false);
                checkboxes.cbCrear.setSelected(false);
                checkboxes.cbEditar.setSelected(false);
                checkboxes.cbEliminar.setSelected(false);
            }
        }
    }

    private void guardarPermisos() {
        Rol rolSeleccionado = (Rol) cbxRoles.getSelectedItem();
        if (rolSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un rol",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<PrivilegioRol> privilegios = new ArrayList<>();

            for (Map<String, PermissionCheckboxes> categoriaMap : permisosCheckboxes.values()) {
                for (PermissionCheckboxes checkboxes : categoriaMap.values()) {
                    if (checkboxes.tieneAlgunPermisoMarcado()) {
                        PrivilegioRol privilegio = new PrivilegioRol(
                                rolSeleccionado.getIdRol(),
                                checkboxes.permiso.getIdPermiso());
                        privilegio.setPuedeVer(checkboxes.cbVer.isSelected());
                        privilegio.setPuedeCrear(checkboxes.cbCrear.isSelected());
                        privilegio.setPuedeEditar(checkboxes.cbEditar.isSelected());
                        privilegio.setPuedeEliminar(checkboxes.cbEliminar.isSelected());
                        privilegios.add(privilegio);
                    }
                }
            }

            serviceRoles.reemplazarPermisos(rolSeleccionado.getIdRol(), privilegios);

            JOptionPane.showMessageDialog(this,
                    "Permisos guardados correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

            cargarRoles();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error guardando permisos: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoNuevoRol() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del nuevo rol:");
        if (nombre == null || nombre.trim().isEmpty())
            return;

        String descripcion = JOptionPane.showInputDialog(this, "Descripción del rol:");

        try {
            int idRol = serviceRoles.crearRolPersonalizado(nombre, descripcion);
            if (idRol > 0) {
                JOptionPane.showMessageDialog(this,
                        "Rol creado exitosamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarRolSeleccionado() {
        int selectedRow = tableRoles.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un rol para editar",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Función en desarrollo");
    }

    private void clonarRolSeleccionado() {
        int selectedRow = tableRoles.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un rol para clonar",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idRol = (int) modelRoles.getValueAt(selectedRow, 0);
        String nuevoNombre = JOptionPane.showInputDialog(this,
                "Nombre para el nuevo rol clonado:");

        if (nuevoNombre == null || nuevoNombre.trim().isEmpty())
            return;

        try {
            int nuevoId = serviceRoles.clonarRol(idRol, nuevoNombre);
            if (nuevoId > 0) {
                JOptionPane.showMessageDialog(this,
                        "Rol clonado exitosamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarRolSeleccionado() {
        int selectedRow = tableRoles.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un rol para eliminar",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idRol = (int) modelRoles.getValueAt(selectedRow, 0);
        String nombre = (String) modelRoles.getValueAt(selectedRow, 1);

        try {
            if (!serviceRoles.puedeEliminarRol(idRol)) {
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar este rol (es de sistema o tiene usuarios asignados)",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de eliminar el rol '" + nombre + "'?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                rolesDAO.eliminarRol(idRol);
                JOptionPane.showMessageDialog(this,
                        "Rol eliminado exitosamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea el panel para asignar permisos personalizados a usuarios individuales
     */
    private JPanel crearPanelPermisosPersonalizados() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel superior: Selector de usuario y botones
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:$Panel.background");

        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setIcon(FontIcon.of(FontAwesomeSolid.USER, 18, ACCENT_PRIMARY));
        lblUsuario.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(lblUsuario);

        cbxUsuarios = new JComboBox<>();
        cbxUsuarios.setPreferredSize(new Dimension(280, 35));
        cbxUsuarios.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        cbxUsuarios.addActionListener(e -> cargarPermisosPersonalizadosUsuario());
        topPanel.add(cbxUsuarios);

        topPanel.add(Box.createHorizontalStrut(20));

        // Botón para agregar permiso personalizado
        JButton btnAgregarPermiso = crearBotonAccion(
                "Agregar Permiso",
                FontIcon.of(FontAwesomeSolid.PLUS_CIRCLE, 18, Color.WHITE),
                ACCENT_SUCCESS);
        btnAgregarPermiso.addActionListener(e -> mostrarDialogoAgregarPermisoPersonalizado());
        topPanel.add(btnAgregarPermiso);

        // Botón para eliminar permiso personalizado
        JButton btnEliminarPermiso = crearBotonAccion(
                "Eliminar Permiso",
                FontIcon.of(FontAwesomeSolid.TRASH_ALT, 18, Color.WHITE),
                ACCENT_DANGER);
        btnEliminarPermiso.addActionListener(e -> eliminarPermisoPersonalizadoSeleccionado());
        topPanel.add(btnEliminarPermiso);

        panel.add(topPanel, BorderLayout.NORTH);

        // Tabla de permisos personalizados del usuario
        String[] columnas = { "Permiso", "Módulo", "Ver", "Crear", "Editar", "Eliminar", "Fecha Asignación" };
        modelPermisosUsuario = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2 && column <= 5; // Solo editable Ver, Crear, Editar, Eliminar
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 2 && columnIndex <= 5) {
                    return Boolean.class;
                }
                return String.class;
            }
        };

        tablePermisosUsuario = new JTable(modelPermisosUsuario);
        tablePermisosUsuario.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePermisosUsuario.setRowHeight(35);
        tablePermisosUsuario.putClientProperty(FlatClientProperties.STYLE,
                "selectionBackground:$Table.selectionBackground;" +
                        "selectionForeground:$Table.selectionForeground");

        // Configurar anchos de columnas
        tablePermisosUsuario.getColumnModel().getColumn(0).setPreferredWidth(200);
        tablePermisosUsuario.getColumnModel().getColumn(1).setPreferredWidth(150);
        tablePermisosUsuario.getColumnModel().getColumn(2).setPreferredWidth(60);
        tablePermisosUsuario.getColumnModel().getColumn(3).setPreferredWidth(60);
        tablePermisosUsuario.getColumnModel().getColumn(4).setPreferredWidth(60);
        tablePermisosUsuario.getColumnModel().getColumn(5).setPreferredWidth(70);
        tablePermisosUsuario.getColumnModel().getColumn(6).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(tablePermisosUsuario);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        // Panel informativo
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        infoPanel.setOpaque(false);
        JLabel lblInfo = new JLabel(
                "ℹ️ Los permisos personalizados SOBRESCRIBEN los permisos del rol asignado al usuario");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblInfo.setForeground(ACCENT_WARNING);
        infoPanel.add(lblInfo);

        // Panel de Sets Predefinidos
        JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        presetsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                "🎯 Sets Predefinidos",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)));

        JButton btnAccesoTotal = crearBotonPreset("Acceso Total",
                FontIcon.of(FontAwesomeSolid.UNLOCK, 16, Color.WHITE),
                new Color(76, 175, 80));
        btnAccesoTotal.setToolTipText("Otorga todos los permisos (Ver, Crear, Editar, Eliminar)");
        btnAccesoTotal.addActionListener(e -> aplicarPresetAccesoTotal());

        JButton btnSoloLectura = crearBotonPreset("Solo Lectura",
                FontIcon.of(FontAwesomeSolid.EYE, 16, Color.WHITE),
                new Color(33, 150, 243));
        btnSoloLectura.setToolTipText("Solo permiso de visualización");
        btnSoloLectura.addActionListener(e -> aplicarPresetSoloLectura());

        JButton btnOperativo = crearBotonPreset("Acceso Operativo",
                FontIcon.of(FontAwesomeSolid.EDIT, 16, Color.WHITE),
                new Color(255, 152, 0));
        btnOperativo.setToolTipText("Ver + Crear + Editar (sin eliminar)");
        btnOperativo.addActionListener(e -> aplicarPresetOperativo());

        JButton btnLimpiar = crearBotonPreset("Limpiar Todo",
                FontIcon.of(FontAwesomeSolid.ERASER, 16, Color.WHITE),
                new Color(244, 67, 54));
        btnLimpiar.setToolTipText("Elimina todos los permisos personalizados");
        btnLimpiar.addActionListener(e -> limpiarPermisosPersonalizados());

        presetsPanel.add(btnAccesoTotal);
        presetsPanel.add(btnSoloLectura);
        presetsPanel.add(btnOperativo);
        presetsPanel.add(btnLimpiar);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel topCenterPanel = new JPanel(new BorderLayout(5, 5));
        topCenterPanel.add(infoPanel, BorderLayout.NORTH);
        topCenterPanel.add(presetsPanel, BorderLayout.CENTER);

        centerPanel.add(topCenterPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Botón guardar cambios
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnGuardarCambios = crearBotonAccion(
                "Guardar Cambios",
                FontIcon.of(FontAwesomeSolid.SAVE, 18, Color.WHITE),
                ACCENT_SUCCESS);
        btnGuardarCambios.addActionListener(e -> guardarPermisosPersonalizados());
        bottomPanel.add(btnGuardarCambios);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Cargar usuarios
        cargarComboUsuarios();

        return panel;
    }

    /**
     * Carga la lista de usuarios en el combo box
     */
    private void cargarComboUsuarios() {
        try {
            cbxUsuarios.removeAllItems();
            raven.dao.UsuarioDAO usuarioDAO = new raven.dao.UsuarioDAO();
            List<Usuario> usuarios = usuarioDAO.obtenerUsuariosActivos();
            for (Usuario usuario : usuarios) {
                cbxUsuarios.addItem(usuario);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error cargando usuarios: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga los permisos personalizados del usuario seleccionado
     */
    private void cargarPermisosPersonalizadosUsuario() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null)
            return;

        try {
            modelPermisosUsuario.setRowCount(0);
            List<PrivilegioUsuario> permisos = privilegioUsuarioDAO.obtenerPermisosPorUsuario(
                    usuarioSeleccionado.getId());

            for (PrivilegioUsuario permiso : permisos) {
                modelPermisosUsuario.addRow(new Object[] {
                        permiso.getNombreMostrar(),
                        permiso.getModulo(),
                        permiso.isPuedeVer(),
                        permiso.isPuedeCrear(),
                        permiso.isPuedeEditar(),
                        permiso.isPuedeEliminar(),
                        permiso.getFechaAsignacion()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error cargando permisos: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra diálogo para agregar un nuevo permiso personalizado
     */
    private void mostrarDialogoAgregarPermisoPersonalizado() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario primero",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Obtener lista de permisos
            List<Permiso> permisosDisponibles = servicePermisos.buscarPermisos("");

            // Crear diálogo de selección
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Agregar Permiso Personalizado", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(500, 400);

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel lblInfo = new JLabel("Seleccione el permiso a asignar a: " + usuarioSeleccionado.getNombre());
            lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
            contentPanel.add(lblInfo, BorderLayout.NORTH);

            JList<Permiso> listPermisos = new JList<>(permisosDisponibles.toArray(new Permiso[0]));
            listPermisos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            contentPanel.add(new JScrollPane(listPermisos), BorderLayout.CENTER);

            JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            JButton btnAceptar = new JButton("Aceptar");
            JButton btnCancelar = new JButton("Cancelar");

            btnAceptar.addActionListener(e -> {
                Permiso permisoSeleccionado = listPermisos.getSelectedValue();
                if (permisoSeleccionado != null) {
                    try {
                        // Verificar si ya existe
                        if (privilegioUsuarioDAO.existePermisoPersonalizado(
                                usuarioSeleccionado.getId(),
                                permisoSeleccionado.getIdPermiso())) {
                            JOptionPane.showMessageDialog(dialog,
                                    "Este permiso ya est\u00e1 asignado a este usuario",
                                    "Advertencia",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        // Crear nuevo permiso personalizado
                        PrivilegioUsuario nuevo = new PrivilegioUsuario();
                        nuevo.setIdUsuario(usuarioSeleccionado.getId());
                        nuevo.setIdPermiso(permisoSeleccionado.getIdPermiso());
                        nuevo.setPuedeVer(true);
                        nuevo.setPuedeCrear(false);
                        nuevo.setPuedeEditar(false);
                        nuevo.setPuedeEliminar(false);

                        if (UserSession.getInstance().getCurrentUser() != null) {
                            nuevo.setIdUsuarioAsignador(
                                    UserSession.getInstance().getCurrentUser().getIdUsuario());
                        }

                        if (privilegioUsuarioDAO.crearPermisoPersonalizado(nuevo)) {
                            JOptionPane.showMessageDialog(dialog,
                                    "Permiso personalizado agregado exitosamente",
                                    "\u00c9xito",
                                    JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            cargarPermisosPersonalizadosUsuario();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(dialog,
                                "Error agregando permiso: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            botonesPanel.add(btnAceptar);
            botonesPanel.add(btnCancelar);
            contentPanel.add(botonesPanel, BorderLayout.SOUTH);

            dialog.add(contentPanel);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error obteniendo permisos: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Elimina el permiso personalizado seleccionado
     */
    private void eliminarPermisoPersonalizadoSeleccionado() {
        int selectedRow = tablePermisosUsuario.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un permiso para eliminar",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de eliminar este permiso personalizado?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Obtener el ID del privilegio desde la tabla
                Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
                if (usuarioSeleccionado != null) {
                    List<PrivilegioUsuario> permisos = privilegioUsuarioDAO.obtenerPermisosPorUsuario(
                            usuarioSeleccionado.getId());

                    if (selectedRow < permisos.size()) {
                        PrivilegioUsuario permiso = permisos.get(selectedRow);
                        if (privilegioUsuarioDAO.eliminarPermisoPersonalizado(
                                permiso.getIdPrivilegioUsuario())) {
                            modelPermisosUsuario.removeRow(selectedRow);
                            JOptionPane.showMessageDialog(this,
                                    "Permiso eliminado exitosamente",
                                    "Éxito",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error eliminando permiso: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Guarda los cambios en permisos personalizados
     */
    private void guardarPermisosPersonalizados() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Obtener permisos actuales del usuario
            List<PrivilegioUsuario> permisosActuales = privilegioUsuarioDAO.obtenerPermisosPorUsuario(
                    usuarioSeleccionado.getId());

            // Actualizar cada permiso según los valores de la tabla
            for (int i = 0; i < modelPermisosUsuario.getRowCount() && i < permisosActuales.size(); i++) {
                PrivilegioUsuario permiso = permisosActuales.get(i);
                permiso.setPuedeVer((Boolean) modelPermisosUsuario.getValueAt(i, 2));
                permiso.setPuedeCrear((Boolean) modelPermisosUsuario.getValueAt(i, 3));
                permiso.setPuedeEditar((Boolean) modelPermisosUsuario.getValueAt(i, 4));
                permiso.setPuedeEliminar((Boolean) modelPermisosUsuario.getValueAt(i, 5));

                privilegioUsuarioDAO.actualizarPermisoPersonalizado(permiso);
            }

            JOptionPane.showMessageDialog(this,
                    "Cambios guardados exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error guardando cambios: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea un botón estilizado para presets de permisos
     */
    private JButton crearBotonPreset(String texto, FontIcon icon, Color backgroundColor) {
        JButton button = new JButton(texto);
        button.setIcon(icon);
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(150, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;borderWidth:0;focusWidth:0");
        return button;
    }

    /**
     * Aplica preset de Acceso Total a todos los permisos del usuario seleccionado
     */
    private void aplicarPresetAccesoTotal() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario primero",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Aplicar Acceso Total (todos los permisos) a " + usuarioSeleccionado.getNombre() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                List<Permiso> todosPermisos = servicePermisos.buscarPermisos("");
                int agregados = 0;

                for (Permiso permiso : todosPermisos) {
                    if (!privilegioUsuarioDAO.existePermisoPersonalizado(
                            usuarioSeleccionado.getId(), permiso.getIdPermiso())) {

                        PrivilegioUsuario nuevo = new PrivilegioUsuario();
                        nuevo.setIdUsuario(usuarioSeleccionado.getId());
                        nuevo.setIdPermiso(permiso.getIdPermiso());
                        nuevo.setPuedeVer(true);
                        nuevo.setPuedeCrear(true);
                        nuevo.setPuedeEditar(true);
                        nuevo.setPuedeEliminar(true);

                        if (UserSession.getInstance().getCurrentUser() != null) {
                            nuevo.setIdUsuarioAsignador(
                                    UserSession.getInstance().getCurrentUser().getIdUsuario());
                        }

                        if (privilegioUsuarioDAO.crearPermisoPersonalizado(nuevo)) {
                            agregados++;
                        }
                    }
                }

                cargarPermisosPersonalizadosUsuario();
                JOptionPane.showMessageDialog(this,
                        agregados + " permisos personalizados agregados con acceso total",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error aplicando preset: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Aplica preset de Solo Lectura (solo ver) a todos los permisos
     */
    private void aplicarPresetSoloLectura() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario primero",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Aplicar Solo Lectura (solo ver) a " + usuarioSeleccionado.getNombre() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                List<Permiso> todosPermisos = servicePermisos.buscarPermisos("");
                int agregados = 0;

                for (Permiso permiso : todosPermisos) {
                    if (!privilegioUsuarioDAO.existePermisoPersonalizado(
                            usuarioSeleccionado.getId(), permiso.getIdPermiso())) {

                        PrivilegioUsuario nuevo = new PrivilegioUsuario();
                        nuevo.setIdUsuario(usuarioSeleccionado.getId());
                        nuevo.setIdPermiso(permiso.getIdPermiso());
                        nuevo.setPuedeVer(true);
                        nuevo.setPuedeCrear(false);
                        nuevo.setPuedeEditar(false);
                        nuevo.setPuedeEliminar(false);

                        if (UserSession.getInstance().getCurrentUser() != null) {
                            nuevo.setIdUsuarioAsignador(
                                    UserSession.getInstance().getCurrentUser().getIdUsuario());
                        }

                        if (privilegioUsuarioDAO.crearPermisoPersonalizado(nuevo)) {
                            agregados++;
                        }
                    }
                }

                cargarPermisosPersonalizadosUsuario();
                JOptionPane.showMessageDialog(this,
                        agregados + " permisos personalizados agregados (solo lectura)",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error aplicando preset: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Aplica preset Operativo (ver + crear + editar, sin eliminar)
     */
    private void aplicarPresetOperativo() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario primero",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Aplicar Acceso Operativo (ver/crear/editar) a " + usuarioSeleccionado.getNombre() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                List<Permiso> todosPermisos = servicePermisos.buscarPermisos("");
                int agregados = 0;

                for (Permiso permiso : todosPermisos) {
                    if (!privilegioUsuarioDAO.existePermisoPersonalizado(
                            usuarioSeleccionado.getId(), permiso.getIdPermiso())) {

                        PrivilegioUsuario nuevo = new PrivilegioUsuario();
                        nuevo.setIdUsuario(usuarioSeleccionado.getId());
                        nuevo.setIdPermiso(permiso.getIdPermiso());
                        nuevo.setPuedeVer(true);
                        nuevo.setPuedeCrear(true);
                        nuevo.setPuedeEditar(true);
                        nuevo.setPuedeEliminar(false);

                        if (UserSession.getInstance().getCurrentUser() != null) {
                            nuevo.setIdUsuarioAsignador(
                                    UserSession.getInstance().getCurrentUser().getIdUsuario());
                        }

                        if (privilegioUsuarioDAO.crearPermisoPersonalizado(nuevo)) {
                            agregados++;
                        }
                    }
                }

                cargarPermisosPersonalizadosUsuario();
                JOptionPane.showMessageDialog(this,
                        agregados + " permisos personalizados agregados (operativo)",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error aplicando preset: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Limpia todos los permisos personalizados del usuario seleccionado
     */
    private void limpiarPermisosPersonalizados() {
        Usuario usuarioSeleccionado = (Usuario) cbxUsuarios.getSelectedItem();
        if (usuarioSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario primero",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar TODOS los permisos personalizados de " + usuarioSeleccionado.getNombre() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int eliminados = privilegioUsuarioDAO.eliminarTodosPermisosUsuario(
                        usuarioSeleccionado.getId());

                cargarPermisosPersonalizadosUsuario();
                JOptionPane.showMessageDialog(this,
                        eliminados + " permisos personalizados eliminados",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error limpiando permisos: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class PermissionCheckboxes {
        Permiso permiso;
        JCheckBox cbVer;
        JCheckBox cbCrear;
        JCheckBox cbEditar;
        JCheckBox cbEliminar;

        boolean tieneAlgunPermisoMarcado() {
            return cbVer.isSelected() || cbCrear.isSelected() ||
                    cbEditar.isSelected() || cbEliminar.isSelected();
        }
    }
}
