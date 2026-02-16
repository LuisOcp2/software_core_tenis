package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.admin.ModelBodegas;
import raven.clases.admin.ServiceBodegas;
import raven.clases.productos.Bodega;
import raven.clases.productos.TraspasoService;
import raven.modal.Toast;

/**
 * @author CrisDEV
 */
public class CreateBodegas extends javax.swing.JPanel {

    private final ServiceBodegas serviceBodegas = new ServiceBodegas();
    private boolean editMode = false;

    private void cargarBodegas() {
        try {
          
            
            // Obtener las bodegas activas
            TraspasoService traspasoService = new TraspasoService();
            List<Bodega> bodegas = traspasoService.obtenerBodegasActivas();
            
            
        } catch (SQLException ex) {
            Logger.getLogger(CreateBodegas.class.getName()).log(Level.SEVERE, null, ex);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar las bodegas");
        }
    }

    public CreateBodegas() {
        initComponents();
        init();
        cargarBodegas(); // Cargar las bodegas al inicializar
    }

    // Método de inicialización personalizado para componentes UI
    public void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:$Table.background");  // Usa color de fondo de tabla

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
        // Validar campos obligatorios para bodegas
        String codigo = txtcodigo.getText().trim();
        String nombre = txtnombre.getText().trim();
        String tipoSel = String.valueOf(cbtipo.getSelectedItem());

        if (codigo.isEmpty() || nombre.isEmpty()) {
            Toast.show(this, Toast.Type.ERROR, "Código y Nombre son obligatorios");
            return false;
        }


        // Validar tipo de bodega seleccionado
        if (cbtipo.isVisible() && (tipoSel.equals("Seleccionar") || cbtipo.getSelectedIndex() == 0)) {
            Toast.show(this, Toast.Type.ERROR, "Debe seleccionar un tipo válido");
            return false;
        }

        // Verificar que el código de bodega no exista (solo en modo creación)
        if (!editMode) {
            try {
                Integer existingId = serviceBodegas.obtenerIdPorCodigo(codigo);
                if (existingId != null) {
                    Toast.show(this, Toast.Type.ERROR, "El código de bodega ya está en uso");
                    return false;
                }
            } catch (SQLException ex) {
                Logger.getLogger(CreateBodegas.class.getName()).log(Level.SEVERE, null, ex);
                Toast.show(this, Toast.Type.ERROR, "Error al verificar disponibilidad del código");
                return false;
            }
        }
        return true;
    }





    public void loadData(ModelBodegas data) {
        if (data != null) {
            editMode = true;
            txtcodigo.setText(data.getCodigo() != null ? data.getCodigo() : "");
            txtnombre.setText(data.getNombre() != null ? data.getNombre() : "");
            txtdireccion.setText(data.getDireccion() != null ? data.getDireccion() : "");
            txtcell.setText(data.getTelefono() != null ? data.getTelefono() : "");
            txtresponsable.setText(data.getResponsable() != null ? data.getResponsable() : "");
            txtcapacidad.setText(data.getCapacidadMaxima() != null ? String.valueOf(data.getCapacidadMaxima()) : "");

            // Mapear tipo del modelo a opciones del combo
            String tipo = data.getTipo();
            if (tipo != null) {
                switch (tipo.toLowerCase()) {
                    case "principal":
                        cbtipo.setSelectedItem("Principal");
                        break;
                    case "sucursal":
                        cbtipo.setSelectedItem("Secundario");
                        break;
                    default:
                        cbtipo.setSelectedItem("Seleccionar");
                }
            } else {
                cbtipo.setSelectedItem("Seleccionar");
            }
        }
    }

    ModelBodegas getData() {
        // Validar campos obligatorios
        String codigo = txtcodigo.getText().trim();
        String nombre = txtnombre.getText().trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "Código y Nombre son obligatorios");
            return null;
        }

        String tipoSeleccion = String.valueOf(cbtipo.getSelectedItem());
        if (tipoSeleccion.equals("Seleccionar")) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione el tipo de bodega");
            return null;
        }

        ModelBodegas data = new ModelBodegas();
        data.setCodigo(codigo);
        data.setNombre(nombre);
        data.setDireccion(txtdireccion.getText().trim());
        data.setTelefono(txtcell.getText().trim());
        data.setResponsable(txtresponsable.getText().trim());

        // Mapear tipo visual a tipo en BD
        String tipoBD = tipoSeleccion.equals("Principal") ? "principal" : "sucursal";
        data.setTipo(tipoBD);

        // Capacidad opcional
        String capStr = txtcapacidad.getText().trim();
        if (!capStr.isEmpty()) {
            try {
                data.setCapacidadMaxima(Integer.parseInt(capStr));
            } catch (NumberFormatException ex) {
                Toast.show(this, Toast.Type.WARNING, "Capacidad debe ser numérica");
                return null;
            }
        }

        data.setActiva(true);
        return data;
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
        txtcodigo = new javax.swing.JTextField();
        txtcell = new javax.swing.JTextField();
        txtcapacidad = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        cbtipo = new javax.swing.JComboBox<>();
        txtdireccion = new javax.swing.JTextField();
        txtnombre = new javax.swing.JTextField();
        txtresponsable = new javax.swing.JTextField();

        jLabel1.setText("Nombre *");

        jLabel2.setText("Capacidad Máxima");

        jLabel3.setText("Teléfono");

        jLabel4.setText("Código *");

        jLabel5.setText("Responsable");

        jLabel6.setText("Tipo");

        jLabel7.setText("Dirección");

        cbtipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar", "principal", "sucursal", "deposito", "temporal" }));

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
                        .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtcapacidad)
                    .addComponent(txtcell)
                    .addComponent(txtcodigo)
                    .addComponent(cbtipo, 0, 236, Short.MAX_VALUE)
                    .addComponent(txtdireccion)
                    .addComponent(txtnombre)
                    .addComponent(txtresponsable))
                .addGap(103, 103, 103))
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addGap(58, 58, 58)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtcodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtnombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtdireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtcell, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtresponsable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbtipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtcapacidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(74, Short.MAX_VALUE))
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbtipo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel panel;
    private javax.swing.JTextField txtcapacidad;
    private javax.swing.JTextField txtcell;
    private javax.swing.JTextField txtcodigo;
    private javax.swing.JTextField txtdireccion;
    private javax.swing.JTextField txtnombre;
    private javax.swing.JTextField txtresponsable;
    // End of variables declaration//GEN-END:variables


}
