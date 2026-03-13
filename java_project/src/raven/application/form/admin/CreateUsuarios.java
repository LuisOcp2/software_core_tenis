package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.admin.ModelUser;
import raven.controlador.admin.SessionManager;
import raven.clases.admin.ServiceUser;
import raven.clases.admin.UserSession;
import raven.clases.productos.Bodega;
import raven.clases.productos.TraspasoService;
import raven.modal.Toast;

/**
 * @author Raven
 */
public class CreateUsuarios extends javax.swing.JPanel {

    private final ServiceUser servicesUser = new ServiceUser();

    private void cargarBodegas() {
        try {
            // Limpiar el combo box
            cbxBodega.removeAllItems();
            cbxBodega.addItem("Seleccione la bodega");
            
            // Obtener las bodegas activas
            TraspasoService traspasoService = new TraspasoService();
            List<Bodega> bodegas = traspasoService.obtenerBodegasActivas();
            
            // Agregar las bodegas al combo box
            for (Bodega bodega : bodegas) {
                cbxBodega.addItem(bodega.getNombre());
            }
        } catch (SQLException ex) {
            Logger.getLogger(CreateUsuarios.class.getName()).log(Level.SEVERE, null, ex);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar las bodegas");
        }
    }

    public CreateUsuarios() {
        initComponents();

        cbEstado.setVisible(false);
        jLabel6.setVisible(false);
        init();
        cargarBodegas(); // Cargar las bodegas al inicializar
        configurarPermisos();
    }
    
    private void configurarPermisos() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && session.hasRole("gerente")) {
            // Restringir roles
            cbRoles.removeAllItems();
            cbRoles.addItem("Seleccionar");
            cbRoles.addItem("vendedor");
            
            // Restringir bodega
            Integer idBodega = session.getIdBodegaUsuario();
            if (idBodega != null) {
                try {
                    TraspasoService traspasoService = new TraspasoService();
                    String nombreBodega = traspasoService.obtenerNombreBodegaPorId(idBodega);
                    if (nombreBodega != null) {
                        cbxBodega.setSelectedItem(nombreBodega);
                        cbxBodega.setEnabled(false);
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(CreateUsuarios.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    // Método de inicialización personalizado para componentes UI
    public void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:$Table.background");  // Usa color de fondo de tabla

        // Configurar campo de contraseña
        jtextContraseña.putClientProperty(FlatClientProperties.STYLE, ""
                + "showRevealButton:true;"
                + "showCapsLock:true");

        // Agregar tooltip para mostrar los requisitos de la contraseña
        jtextContraseña.setToolTipText("La contraseña debe tener al menos 8 caracteres, "
                + "incluir al menos una letra mayúscula, "
                + "una letra minúscula y un número.");

        // Configurar campo de verificación de contraseña
        jtextVerificaContraseña.putClientProperty(FlatClientProperties.STYLE, ""
                + "showRevealButton:true;"
                + "showCapsLock:true");

        jtextVerificaContraseña.setToolTipText("Repita la contraseña exactamente igual");

    }

    public void initEstado() {
        cbEstado.setVisible(true);
        jLabel6.setVisible(true);
        txtNomUser.setEditable(false);
    }

    // Método para validar que la contraseña cumpla con los requisitos
    private boolean validarContraseña(String password) {
        // Verificar longitud mínima
        if (password.length() < 8) {
            Toast.show(this, Toast.Type.ERROR, "La contraseña debe tener al menos 8 caracteres");
            return false;
        }

        // Verificar que contenga al menos una letra mayúscula
        if (!password.matches(".*[A-Z].*")) {
            Toast.show(this, Toast.Type.ERROR, "La contraseña debe contener al menos una letra mayúscula");
            return false;
        }

        // Verificar que contenga al menos una letra minúscula
        if (!password.matches(".*[a-z].*")) {
            Toast.show(this, Toast.Type.ERROR, "La contraseña debe contener al menos una letra minúscula");
            return false;
        }

        // Verificar que contenga al menos un número
        if (!password.matches(".*[0-9].*")) {
            Toast.show(this, Toast.Type.ERROR, "La contraseña debe contener al menos un número");
            return false;
        }

        return true;
    }

    public boolean validarCampos() {
        // Determinar si estamos en modo edición
        boolean esEdicion = !txtNomUser.isEditable();

        // Validar campos obligatorios (manteniendo tu validación existente)
        if (txtNombre.getText().trim().isEmpty()
                || txtCorreo.getText().trim().isEmpty()
                || txtNomUser.getText().trim().isEmpty()) {
            Toast.show(this, Toast.Type.ERROR, "Todos los campos son obligatorios");
            return false;
        }

        // Validar formato de correo electrónico (manteniendo tu validación existente)
        if (!validarCorreo(txtCorreo.getText())) {
            Toast.show(this, Toast.Type.ERROR, "Formato de correo electrónico inválido");
            return false;
        }

        // Validar que el nombre de usuario no contenga caracteres especiales (manteniendo tu validación existente)
        if (!validarNombreUsuario(txtNomUser.getText())) {
            Toast.show(this, Toast.Type.ERROR, "El nombre de usuario no debe contener caracteres especiales");
            return false;
        }

        // Solo en modo edición, omitir la validación de contraseña si ambos campos están vacíos
        String pass1 = String.valueOf(jtextContraseña.getPassword());
        String pass2 = String.valueOf(jtextVerificaContraseña.getPassword());

        if (esEdicion && pass1.isEmpty() && pass2.isEmpty()) {
            // Contraseñas vacías en modo edición, no validamos (mantendremos la contraseña actual)
        } else {
            // Validar contraseña (usando tu método existente)
            if (!validarContraseña(pass1)) {
                return false;
            }

            // Validar que las contraseñas coincidan
            if (!pass1.equals(pass2)) {
                Toast.show(this, Toast.Type.ERROR, "Las contraseñas no coinciden");
                return false;
            }
        }

        // Validar que se haya seleccionado un rol válido
        if (cbRoles.getSelectedIndex() == 0) {
            Toast.show(this, Toast.Type.ERROR, "Debe seleccionar un rol válido");
            return false;
        }

        // Validar que se haya seleccionado una bodega
        if (cbxBodega.getSelectedIndex() == 0) {
            Toast.show(this, Toast.Type.ERROR, "Debe seleccionar una bodega");
            return false;
        }

        // Validar el estado en modo edición
        if (cbEstado.isVisible() && cbEstado.getSelectedIndex() == 0) {
            Toast.show(this, Toast.Type.ERROR, "Debe seleccionar un estado válido");
            return false;
        }

        // Verificar que el nombre de usuario no exista (solo en modo creación)
        if (!esEdicion) {
            try {
                if (servicesUser.existeUsername(txtNomUser.getText().trim())) {
                    Toast.show(this, Toast.Type.ERROR, "El nombre de usuario ya está en uso");
                    return false;
                }
            } catch (SQLException ex) {
                Logger.getLogger(CreateUsuarios.class.getName()).log(Level.SEVERE, null, ex);
                Toast.show(this, Toast.Type.ERROR, "Error al verificar disponibilidad del usuario");
                return false;
            }
        }
        return true;
    }

// Método para validar el formato del correo electrónico
    private boolean validarCorreo(String correo) {
        // Expresión regular para validar correo
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return correo.matches(regex);
    }

// Método para validar que el nombre de usuario no tenga caracteres especiales
    private boolean validarNombreUsuario(String nombreUsuario) {
        // Expresión regular que solo permite letras, números y guiones
        String regex = "^[a-zA-Z0-9_-]+$";
        return nombreUsuario.matches(regex);
    }



    public void loadData(ModelUser data) {
        if (data != null) {
            txtNombre.setText(data.getNombre());
            txtCorreo.setText(data.getEmail());
            txtNomUser.setText(data.getUsername());
            cbRoles.setSelectedItem(data.getRol());
            
            // Hacer visible el combo de estado y la etiqueta, y deshabilitar edición del nombre de usuario
            initEstado();
            
            // Establecer el estado según el valor booleano
            cbEstado.setSelectedIndex(data.isActivo() ? 1 : 2); // 1 para "Activo", 2 para "Inactivo"
            
            // Asegurar que las bodegas estén cargadas antes de seleccionar
            cargarBodegas();
            
            // Seleccionar la bodega correspondiente buscando por nombre
            Integer idBodega = data.getIdBodega();
            if (idBodega != null && idBodega > 0) {
                // Buscar el nombre de la bodega por ID
                try {
                    TraspasoService traspasoService = new TraspasoService();
                    String nombreBodega = traspasoService.obtenerNombreBodegaPorId(idBodega);
                    
                    if (nombreBodega != null) {
                        // Buscar el índice del nombre en el combo box
                        for (int i = 0; i < cbxBodega.getItemCount(); i++) {
                            if (nombreBodega.equals(cbxBodega.getItemAt(i))) {
                                cbxBodega.setSelectedIndex(i);
                                break;
                            }
                        }
                    } else {
                        cbxBodega.setSelectedIndex(0); // "Seleccione la bodega"
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(CreateUsuarios.class.getName()).log(Level.SEVERE, null, ex);
                    cbxBodega.setSelectedIndex(0); // "Seleccione la bodega"
                }
            } else {
                cbxBodega.setSelectedIndex(0); // "Seleccione la bodega"
            }
        }
    }

    ModelUser getData() {
        ModelUser data = new ModelUser();
        data.setNombre(txtNombre.getText().trim());
        data.setEmail(txtCorreo.getText().trim());
        data.setUsername(txtNomUser.getText().trim());
        
        // Obtener la contraseña solo si se ha ingresado una nueva
        String password = String.valueOf(jtextContraseña.getPassword());
        if (!password.isEmpty()) {
            data.setPassword(password); // Guardar contraseña en texto plano
        }
        
        // Establecer el rol seleccionado
        data.setRol(cbRoles.getSelectedItem().toString());
        
        // Establecer el estado del usuario
        if (cbEstado.isVisible()) {
            // En modo edición, usar el valor seleccionado del combo
            String estadoSeleccionado = cbEstado.getSelectedItem().toString().toLowerCase();
            data.setActivo(estadoSeleccionado.equals("activo"));
        } else {
            // En modo creación, el usuario siempre se crea activo
            data.setActivo(true);
        }
        
        // Establecer automáticamente la bodega del usuario logueado
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isSessionActive()) {
            Integer idBodegaUsuarioLogueado = sessionManager.getCurrentUserBodegaId();
            data.setIdBodega(idBodegaUsuarioLogueado);
        } else {
            // Fallback: usar la bodega seleccionada manualmente si no hay sesión activa
            int selectedIndex = cbxBodega.getSelectedIndex();
            if (selectedIndex > 0) {
                String nombreBodegaSeleccionada = cbxBodega.getSelectedItem().toString();
                Integer idBodega = obtenerIdBodegaPorNombre(nombreBodegaSeleccionada);
                data.setIdBodega(idBodega);
            } else {
                data.setIdBodega(null); // No se seleccionó bodega
            }
        }
        
        // Establecer ubicación fija como "tienda"
        data.setUbicacion("tienda");
        
        return data;
    }

    /**
     * Método helper para obtener el ID de bodega basado en su nombre
     */
    private Integer obtenerIdBodegaPorNombre(String nombreBodega) {
        if (nombreBodega == null || nombreBodega.equals("Seleccione la bodega")) {
            return null;
        }
        
        try {
            TraspasoService traspasoService = new TraspasoService();
            List<Bodega> bodegas = traspasoService.obtenerBodegasActivas();
            
            for (Bodega bodega : bodegas) {
                if (bodega.getNombre().equals(nombreBodega)) {
                    return bodega.getIdBodega();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(CreateUsuarios.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    // Busca datos según el texto ingresado
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        txtCorreo = new javax.swing.JTextField();
        txtNomUser = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        cbRoles = new javax.swing.JComboBox<>();
        cbEstado = new javax.swing.JComboBox<>();
        jtextContraseña = new javax.swing.JPasswordField();
        jtextVerificaContraseña = new javax.swing.JPasswordField();
        cbxBodega = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();

        jLabel1.setText("Contraseña");

        jLabel2.setText("Nombre Usuario");

        jLabel3.setText("Correo");

        jLabel4.setText("Nombre");

        jLabel5.setText("Rol");

        jLabel6.setText("Estado");

        jLabel7.setText("Confirmar Contraseña");

        cbRoles.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar", "admin", "vendedor", "almacen", "gerente" }));
        cbRoles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbRolesActionPerformed(evt);
            }
        });

        cbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar", "Activo", "Inactivo" }));

        jtextVerificaContraseña.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                jtextVerificaContraseñaInputMethodTextChanged(evt);
            }
        });
        jtextVerificaContraseña.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtextVerificaContraseñaActionPerformed(evt);
            }
        });

        cbxBodega.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Selecione la bodega", "bodega 1", "bodega 2" }));

        jLabel8.setText("Bodega:");

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                .addGap(122, 122, 122)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelLayout.createSequentialGroup()
                        .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(cbxBodega, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtNomUser)
                    .addComponent(cbRoles, 0, 236, Short.MAX_VALUE)
                    .addComponent(txtCorreo)
                    .addComponent(txtNombre)
                    .addComponent(cbEstado, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jtextContraseña)
                    .addComponent(jtextVerificaContraseña))
                .addGap(103, 103, 103))
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addGap(58, 58, 58)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtextContraseña, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtextVerificaContraseña, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbRoles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtNomUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxBodega, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addContainerGap(36, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cbRolesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbRolesActionPerformed

    }//GEN-LAST:event_cbRolesActionPerformed

    private void jtextVerificaContraseñaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtextVerificaContraseñaActionPerformed

    }//GEN-LAST:event_jtextVerificaContraseñaActionPerformed

    private void jtextVerificaContraseñaInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_jtextVerificaContraseñaInputMethodTextChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_jtextVerificaContraseñaInputMethodTextChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbEstado;
    private javax.swing.JComboBox<String> cbRoles;
    private javax.swing.JComboBox<String> cbxBodega;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPasswordField jtextContraseña;
    private javax.swing.JPasswordField jtextVerificaContraseña;
    private javax.swing.JPanel panel;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JTextField txtNomUser;
    private javax.swing.JTextField txtNombre;
    // End of variables declaration//GEN-END:variables


}
