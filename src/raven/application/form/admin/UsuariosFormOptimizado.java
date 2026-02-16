package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.conexion;
import raven.clases.admin.ServiceUser;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalController;

/**
 *
 * @author Raven
 */
public class UsuariosFormOptimizado extends javax.swing.JPanel {

    private final ServiceUser servicesUser = new ServiceUser();
    private final FontIcon iconAjustar;
    private final FontIcon iconNuevo;
    private final FontIcon iconDesactivar;

    public UsuariosFormOptimizado() {
        initComponents();

        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");

        //Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.USER_EDIT, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.USER_COG, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.USER_ALT_SLASH, tabTextColor);

        btnCrear.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41");  // Color de fondo
        btnEditar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FFCC00");  // Color de fondo
        btnDesactivar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FF453A");  // Color de fondo

        btnCrear.setIcon(iconNuevo);
        btnEditar.setIcon(iconAjustar);
        btnDesactivar.setIcon(iconDesactivar);

        //Fin de diseño de botones
        init();
    }

    // Método de inicialización personalizado para componentes UI
    private void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:$Table.background");  // Usa color de fondo de tabla

        // Estiliza el encabezado de la tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;" // Altura del encabezado
                + "hoverBackground:null;" // Desactiva efecto hover
                + "pressedBackground:null;" // Desactiva efecto al presionar
                + "separatorColor:$TableHeader.background;" // Color del separador
                + "font:bold;");  // Texto en negrita

        // Estiliza la tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:70;" // Altura de filas
                + "showHorizontalLines:true;" // Muestra líneas horizontales
                + "intercellSpacing:0,1;" // Espaciado entre celdas
                + "cellFocusColor:$TableHeader.hoverBackground;" // Color de enfoque
                + "selectionBackground:$TableHeader.hoverBackground;" // Fondo de selección
                + "selectionForeground:$Table.foreground;");  // Texto de selección

        // Estiliza la barra de desplazamiento
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;" // Barra completamente redondeada
                + "trackInsets:3,3,3,3;" // Relleno de la barra
                + "thumbInsets:3,3,3,3;" // Relleno del control deslizante
                + "background:$Table.background;");  // Color de fondo

        // Estiliza el título
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;");  // Texto en negrita y más grande

        // Configura el campo de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));  // Ícono de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;" // Esquinas redondeadas
                + "borderWidth:0;" // Sin borde
                + "focusWidth:0;" // Sin borde de enfoque
                + "innerFocusWidth:0;" // Sin enfoque interno
                + "margin:5,20,5,20;" // Márgenes
                + "background:$Panel.background");  // Color de fondo

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
        DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería mejorarse)
        // Detiene cualquier edición de celda activa
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        // Limpia las filas existentes
        model.setRowCount(0);
        // Obtiene resultados de búsqueda del servicio
        List<ModelUser> list = servicesUser.search(search);
        
        // Carga los nombres de bodega de forma optimizada
        Map<Integer, String> bodegaNombres = cargarNombresBodegas();
        
        // Añade empleados coincidentes como filas en la tabla
        for (ModelUser d : list) {
            model.addRow(d.toTableRowOptimizado(table.getRowCount() + 1, bodegaNombres));
        }
    }

    private void loadData() throws SQLException {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería mejorarse)
            // Detiene cualquier edición de celda activa
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            // Limpia las filas existentes
            model.setRowCount(0);
            // Obtiene todos los empleados del servicio
            List<ModelUser> list = servicesUser.obtenerTodosLosUsuarios();
            
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
            Logger.getLogger(UsuariosFormOptimizado.class.getName()).log(Level.SEVERE, null, ex);
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
                ModelUser user = new ModelUser(idUsuario, username, null, nombre, email, rol, "tienda", idBodega, activo);
                list.add(user);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
            new Object [][] {
                {false, null, null, null, null, null, null, null},
                {false, null, null, null, null, null, null, null},
                {false, null, null, null, null, null, null, null},
                {false, null, null, null, null, null, null, null}
            },
            new String [] {
                "Seleccionar", "Codigo", "Nombre Usuario", "Nombre", "Correo", "rol", "Bodega", "Estado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
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
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 502, Short.MAX_VALUE)
                        .addComponent(btnCrear)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEditar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDesactivar)
                        .addGap(27, 27, 27))))
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCrear)
                    .addComponent(btnEditar)
                    .addComponent(btnDesactivar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(UsuariosFormOptimizado.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_txtSearchKeyReleased

    private void btnCrearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCrearActionPerformed
        CreateUsuarios cr = new CreateUsuarios();
        // Carga los datos en la instancia 'cr' utilizando el servicio proporcionado y un valor nulo
        cr.loadData(null);

        // Crea un arreglo de opciones para el modal, que incluye un botón de "Cancelar" y otro de "Guardar"
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION), // Opción para cancelar
            new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION) // Opción para guardar
        };

        ModalDialog.showModal(this, // El contexto actual (probablemente una ventana o formulario)
                new SimpleModalBorder(cr, "Nuevo Usuario", options, (ModalController mc, int i) -> { // Crea un nuevo SimpleModalBorder
                    // Maneja la acción cuando se cierra el modal
                    if (i == SimpleModalBorder.OK_OPTION) { // Si se selecciona la opción "Guardar"
                        ModelUser userData = cr.getData();
                        try {
                            // Intenta crear un nuevo producto utilizando los datos de 'cr'
                            servicesUser.insertarUsuario(userData);
                            // Muestra un mensaje de éxito si el producto se crea correctamente
                            Toast.show(UsuariosFormOptimizado.this, Toast.Type.SUCCESS, "Usuario creado exitosamente");
                            // Recarga los datos en el formulario
                            loadData();
                        } catch (SQLException e) { // Captura cualquier excepción de SQL que ocurra
                            // Muestra un mensaje de error si hay un problema al crear el usuario
                            Toast.show(UsuariosFormOptimizado.this, Toast.Type.ERROR, "Error al crear Usuario: " + e.getMessage());
                        }
                    } else if (i == SimpleModalBorder.OPENED) { // Si el modal se abre
                        // Inicializa el formulario 'cr' para que esté listo para la entrada de datos
                        cr.init();
                    }
                })
        );
    }//GEN-LAST:event_btnCrearActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        List<ModelUser> list = getSelectedData();

        if (!list.isEmpty()) {
            if (list.size() == 1) {
                ModelUser data = list.get(0);

                // Si el usuario está desactivado, pregunta si quiere activarlo
                if (!data.isActivo()) {
                    SimpleModalBorder.Option[] optionsActivar = new SimpleModalBorder.Option[]{
                        new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                        new SimpleModalBorder.Option("Activar Usuario", SimpleModalBorder.OK_OPTION),};

                    JLabel labelActivar = new JLabel("¿Desea activar este usuario?");
                    labelActivar.setBorder(new EmptyBorder(5, 25, 5, 25));

                    ModalDialog.showModal(
                            this,
                            new SimpleModalBorder(
                                    labelActivar,
                                    "Activar Usuario: " + data.getNombre(),
                                    optionsActivar,
                                    (mc, i) -> {
                                        if (i == SimpleModalBorder.OK_OPTION) {
                                            try {
                                                // Usar el método específico para activar
                                                servicesUser.activarUsuario(data.getIdUsuario());
                                                Toast.show(this, Toast.Type.SUCCESS, "Usuario activado exitosamente");
                                                loadData();
                                            } catch (SQLException e) {
                                                Toast.show(this, Toast.Type.ERROR, "Error al activar usuario: " + e.getMessage());
                                            }
                                        }
                                    }
                            )
                    );
                    return; // Salir del método para no mostrar el diálogo de edición normal
                }

                // Continuar con el proceso normal para usuarios activos
                CreateUsuarios create = new CreateUsuarios();
                create.loadData(data);

                SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION),};

                ModalDialog.showModal(
                        this,
                        new SimpleModalBorder(
                                create,
                                "Editar Usuario: " + data.getNombre(),
                                options,
                                (mc, i) -> {
                                    if (i == SimpleModalBorder.OK_OPTION) {
                                        try {
                                            // Obtener los datos editados del formulario
                                            ModelUser dataEdit = create.getData();

                                            if (dataEdit != null) {
                                                dataEdit.setIdUsuario(data.getIdUsuario());

                                                // Si la contraseña está vacía, mantén la actual
                                                if (dataEdit.getPassword() == null || dataEdit.getPassword().isEmpty()) {
                                                    // No necesitamos hacer nada especial aquí, el servicio se encargará
                                                }

                                                servicesUser.modificarUsuario(dataEdit);
                                                Toast.show(this, Toast.Type.SUCCESS, "Usuario actualizado");
                                                loadData();
                                            }
                                            // Si dataEdit es null, no hacemos nada y el modal permanece abierto
                                        } catch (SQLException e) {
                                            Toast.show(this, Toast.Type.ERROR, "Error al actualizar: " + e.getMessage());
                                        }
                                    } else if (i == SimpleModalBorder.OPENED) {
                                        create.init();
                                    }
                                }
                        )
                );
            } else {
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo un Usuario");
            }
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un Usuario para editar");
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnDesactivarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDesactivarActionPerformed
        // Obtiene la lista de empleados seleccionados (probablemente de una tabla o vista)
        List<ModelUser> list = getSelectedData();
        if (!list.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Desactivar", SimpleModalBorder.OK_OPTION)
            };

            JLabel label = new JLabel("¿Está seguro de desactivar " + list.size() + " Usuario(s)?");
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
                                        for (ModelUser d : list) {
                                            servicesUser.desactivar(d.getIdUsuario());
                                        }
                                        Toast.show(this, Toast.Type.SUCCESS, "Usuario(s) desactivado(s)");
                                        loadData();
                                    } catch (SQLException e) {
                                        Toast.show(this, Toast.Type.ERROR, "Error al eliminar: " + e.getMessage());
                                    }
                                }
                            }
                    )
            );
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un Usuario");
        }
    }//GEN-LAST:event_btnDesactivarActionPerformed

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
                 java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT id_bodega FROM bodegas WHERE nombre = ?")) {

                stmt.setString(1, nombreBodega);
                java.sql.ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id_bodega");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(UsuariosFormOptimizado.class.getName()).log(Level.SEVERE, null, ex);
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