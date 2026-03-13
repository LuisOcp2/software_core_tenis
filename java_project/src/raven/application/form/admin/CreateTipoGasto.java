package raven.application.form.admin;

import com.formdev.flatlaf.FlatClientProperties;
import java.math.BigDecimal;
import javax.swing.DefaultComboBoxModel;
import raven.controlador.principal.ModelTipoGasto;

/**
 * Formulario para crear/editar tipos de gasto.
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class CreateTipoGasto extends javax.swing.JPanel {

        private ModelTipoGasto tipoGastoActual;
        private boolean isEditing = false;

        public CreateTipoGasto() {
                initComponents();
                applyStyles();
        }

        public void init() {
                // Inicialización adicional si es necesario
                if (!isEditing) {
                        generateCode();
                }
        }

        private void applyStyles() {
                // Panel principal
                this.putClientProperty(FlatClientProperties.STYLE, ""
                                + "background:$Panel.background");

                // Campos de texto
                txtCodigo.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:10;"
                                + "borderWidth:1;"
                                + "focusWidth:1;"
                                + "innerFocusWidth:0;");

                txtNombre.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:10;"
                                + "borderWidth:1;"
                                + "focusWidth:1;"
                                + "innerFocusWidth:0;");

                txtDescripcion.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:10;"
                                + "borderWidth:1;"
                                + "focusWidth:1;"
                                + "innerFocusWidth:0;");

                txtMontoMaximo.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:10;"
                                + "borderWidth:1;"
                                + "focusWidth:1;"
                                + "innerFocusWidth:0;");

                txtCuentaContable.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:10;"
                                + "borderWidth:1;"
                                + "focusWidth:1;"
                                + "innerFocusWidth:0;");

                // ComboBox
                cmbCategoria.putClientProperty(FlatClientProperties.STYLE, ""
                                + "arc:10;");

                // Checkbox - no arc style for checkboxes
                // chkRequiereAutorizacion does not support arc style

                // Etiquetas
                lblTitulo.putClientProperty(FlatClientProperties.STYLE, ""
                                + "font:bold +4;");
        }

        private void generateCode() {
                // Generar código automático basado en timestamp
                long timestamp = System.currentTimeMillis() % 100000;
                txtCodigo.setText("GAS" + String.format("%05d", timestamp));
        }

        public void loadData(ModelTipoGasto tipo) {
                this.tipoGastoActual = tipo;
                this.isEditing = (tipo != null);

                if (tipo != null) {
                        txtCodigo.setText(tipo.getCodigo());
                        txtNombre.setText(tipo.getNombre());
                        txtDescripcion.setText(tipo.getDescripcion() != null ? tipo.getDescripcion() : "");

                        // Seleccionar categoría
                        for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
                                String item = (String) cmbCategoria.getItemAt(i);
                                if (item.equalsIgnoreCase(tipo.getCategoria().getValor())) {
                                        cmbCategoria.setSelectedIndex(i);
                                        break;
                                }
                        }

                        chkRequiereAutorizacion.setSelected(tipo.isRequiereAutorizacion());

                        if (tipo.getMontoMaximoSinAutorizacion() != null) {
                                txtMontoMaximo.setText(tipo.getMontoMaximoSinAutorizacion().toPlainString());
                        }

                        txtCuentaContable.setText(tipo.getCuentaContable() != null ? tipo.getCuentaContable() : "");
                } else {
                        // Modo creación - limpiar campos
                        txtCodigo.setText("");
                        txtNombre.setText("");
                        txtDescripcion.setText("");
                        cmbCategoria.setSelectedIndex(0);
                        chkRequiereAutorizacion.setSelected(false);
                        txtMontoMaximo.setText("300000");
                        txtCuentaContable.setText("");
                }

                toggleMontoMaximo();
        }

        public ModelTipoGasto getData() {
                // Validaciones
                String codigo = txtCodigo.getText().trim();
                String nombre = txtNombre.getText().trim();

                if (codigo.isEmpty()) {
                        showError("El código es requerido");
                        return null;
                }

                if (nombre.isEmpty()) {
                        showError("El nombre es requerido");
                        return null;
                }

                // Crear modelo
                ModelTipoGasto tipo = new ModelTipoGasto();
                if (tipoGastoActual != null) {
                        tipo.setIdTipoGasto(tipoGastoActual.getIdTipoGasto());
                }

                tipo.setCodigo(codigo);
                tipo.setNombre(nombre);
                tipo.setDescripcion(txtDescripcion.getText().trim());

                // Categoría
                String categoriaStr = (String) cmbCategoria.getSelectedItem();
                tipo.setCategoriaFromString(categoriaStr.toLowerCase());

                // Autorización
                tipo.setRequiereAutorizacion(chkRequiereAutorizacion.isSelected());

                // Monto máximo
                try {
                        String montoStr = txtMontoMaximo.getText().trim().replace(",", "").replace(".", "");
                        if (!montoStr.isEmpty()) {
                                tipo.setMontoMaximoSinAutorizacion(new BigDecimal(montoStr));
                        } else {
                                tipo.setMontoMaximoSinAutorizacion(new BigDecimal("300000"));
                        }
                } catch (NumberFormatException e) {
                        tipo.setMontoMaximoSinAutorizacion(new BigDecimal("300000"));
                }

                // Cuenta contable
                String cuenta = txtCuentaContable.getText().trim();
                tipo.setCuentaContable(cuenta.isEmpty() ? null : cuenta);

                tipo.setActivo(true);

                return tipo;
        }

        private void showError(String message) {
                javax.swing.JOptionPane.showMessageDialog(this, message, "Error de validación",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        private void toggleMontoMaximo() {
                boolean enabled = chkRequiereAutorizacion.isSelected();
                txtMontoMaximo.setEnabled(enabled);
                lblMontoMaximo.setEnabled(enabled);
        }

        @SuppressWarnings("unchecked")
        private void initComponents() {

                lblTitulo = new javax.swing.JLabel();
                lblCodigo = new javax.swing.JLabel();
                txtCodigo = new javax.swing.JTextField();
                lblNombre = new javax.swing.JLabel();
                txtNombre = new javax.swing.JTextField();
                lblDescripcion = new javax.swing.JLabel();
                txtDescripcion = new javax.swing.JTextField();
                lblCategoria = new javax.swing.JLabel();
                cmbCategoria = new javax.swing.JComboBox<>();
                chkRequiereAutorizacion = new javax.swing.JCheckBox();
                lblMontoMaximo = new javax.swing.JLabel();
                txtMontoMaximo = new javax.swing.JTextField();
                lblCuentaContable = new javax.swing.JLabel();
                txtCuentaContable = new javax.swing.JTextField();
                panelInfo = new javax.swing.JPanel();
                lblInfo = new javax.swing.JLabel();

                setPreferredSize(new java.awt.Dimension(450, 400));

                lblTitulo.setText("Configuración del Tipo de Gasto");
                lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                lblCodigo.setText("Código *");
                txtCodigo.setToolTipText("Código único del tipo de gasto");

                lblNombre.setText("Nombre *");
                txtNombre.setToolTipText("Nombre descriptivo del tipo de gasto");

                lblDescripcion.setText("Descripción");
                txtDescripcion.setToolTipText("Descripción detallada (opcional)");

                lblCategoria.setText("Categoría");
                cmbCategoria.setModel(new DefaultComboBoxModel<>(new String[] {
                                "Operativo", "Administrativo", "Financiero", "Otro"
                }));

                chkRequiereAutorizacion.setText("Requiere autorización para montos grandes");
                chkRequiereAutorizacion.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                chkRequiereAutorizacionActionPerformed(evt);
                        }
                });

                lblMontoMaximo.setText("Monto máximo sin autorización");
                txtMontoMaximo.setText("300000");
                txtMontoMaximo.setToolTipText("Monto máximo que se puede gastar sin requerir autorización");

                lblCuentaContable.setText("Cuenta contable");
                txtCuentaContable.setToolTipText("Código de cuenta contable (opcional)");

                // Panel informativo
                panelInfo.setBackground(new java.awt.Color(240, 248, 255));
                panelInfo.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(173, 216, 230)),
                                javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)));

                lblInfo.setText(
                                "<html><b>Información:</b><br>Si activa la autorización, los gastos que superen el monto máximo requerirán aprobación de un supervisor.</html>");
                lblInfo.setFont(new java.awt.Font("Segoe UI", 0, 11));

                javax.swing.GroupLayout panelInfoLayout = new javax.swing.GroupLayout(panelInfo);
                panelInfo.setLayout(panelInfoLayout);
                panelInfoLayout.setHorizontalGroup(
                                panelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(lblInfo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
                panelInfoLayout.setVerticalGroup(
                                panelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(lblInfo));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addGap(20, 20, 20)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(lblTitulo,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(layout.createSequentialGroup()
                                                                                                .addGroup(layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(lblCodigo)
                                                                                                                .addComponent(txtCodigo,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                120,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                .addGap(18, 18, 18)
                                                                                                .addGroup(layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(lblNombre)
                                                                                                                .addComponent(txtNombre,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                272,
                                                                                                                                Short.MAX_VALUE)))
                                                                                .addGroup(layout.createSequentialGroup()
                                                                                                .addGroup(layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(lblDescripcion)
                                                                                                                .addComponent(lblCategoria))
                                                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                                                .addComponent(txtDescripcion)
                                                                                .addComponent(cmbCategoria, 0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(chkRequiereAutorizacion,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(layout.createSequentialGroup()
                                                                                                .addGroup(layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(lblMontoMaximo)
                                                                                                                .addComponent(txtMontoMaximo,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                180,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                .addGap(18, 18, 18)
                                                                                                .addGroup(layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(lblCuentaContable)
                                                                                                                .addComponent(txtCuentaContable)))
                                                                                .addComponent(panelInfo,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(20, 20, 20)));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addComponent(lblTitulo)
                                                                .addGap(20, 20, 20)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(lblCodigo)
                                                                                .addComponent(lblNombre))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(txtCodigo,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                32,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(txtNombre,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                32,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(15, 15, 15)
                                                                .addComponent(lblDescripcion)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtDescripcion,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                32,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(15, 15, 15)
                                                                .addComponent(lblCategoria)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cmbCategoria,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                32,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(chkRequiereAutorizacion)
                                                                .addGap(12, 12, 12)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(lblMontoMaximo)
                                                                                .addComponent(lblCuentaContable))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(txtMontoMaximo,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                32,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(txtCuentaContable,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                32,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelInfo,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(20, Short.MAX_VALUE)));
        }

        private void chkRequiereAutorizacionActionPerformed(java.awt.event.ActionEvent evt) {
                toggleMontoMaximo();
        }

        // Variables declaration
        private javax.swing.JCheckBox chkRequiereAutorizacion;
        private javax.swing.JComboBox<String> cmbCategoria;
        private javax.swing.JLabel lblCategoria;
        private javax.swing.JLabel lblCodigo;
        private javax.swing.JLabel lblCuentaContable;
        private javax.swing.JLabel lblDescripcion;
        private javax.swing.JLabel lblInfo;
        private javax.swing.JLabel lblMontoMaximo;
        private javax.swing.JLabel lblNombre;
        private javax.swing.JLabel lblTitulo;
        private javax.swing.JPanel panelInfo;
        private javax.swing.JTextField txtCodigo;
        private javax.swing.JTextField txtCuentaContable;
        private javax.swing.JTextField txtDescripcion;
        private javax.swing.JTextField txtMontoMaximo;
        private javax.swing.JTextField txtNombre;
}
