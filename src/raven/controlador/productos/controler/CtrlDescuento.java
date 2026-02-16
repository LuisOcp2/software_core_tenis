package raven.controlador.productos.controler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import raven.application.form.productos.Descuento;
import raven.application.form.productos.creates.CreateDescuento;
import raven.clases.productos.ServiceDescuento;
import raven.controlador.productos.ModelPromocion;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.BorderOption;

/**
 * Controlador para manejar las operaciones relacionadas con las promociones
 * @author CrisDEV
 */
public class CtrlDescuento implements ActionListener, MouseListener, KeyListener {
    private final ServiceDescuento serviceDescuento;
    private Descuento vista;
    private DefaultTableModel modeloTabla;
    private List<ModelPromocion> listaPromociones;

    public CtrlDescuento() {
        this.serviceDescuento = new ServiceDescuento();
    }
    
    /**
     * Inicializa el controlador con la vista
     * @param vista La vista de descuentos
     */
    public void inicializar(Descuento vista) {
        this.vista = vista;
        configurarEventos();
        configurarTabla();
        cargarDatos();
    }
    
    /**
     * Configura los eventos de los componentes de la vista
     */
    private void configurarEventos() {
        vista.btn_nuevo.addActionListener(this);
        vista.btn_editar.addActionListener(this);
        vista.btn_eliminar.addActionListener(this);
        vista.txtSearch.addKeyListener(this);
        vista.table.addMouseListener(this);
    }
    
    /**
     * Configura el modelo de la tabla
     */
    private void configurarTabla() {
        modeloTabla = (DefaultTableModel) vista.table.getModel();
        // Limpiar datos existentes
        modeloTabla.setRowCount(0);
    }
    
    /**
     * Carga los datos de promociones en la tabla
     */
    public void cargarDatos() {
        try {
            listaPromociones = serviceDescuento.getAll();
            actualizarTabla(listaPromociones);
        } catch (SQLException e) {
            mostrarError("Error al cargar promociones: " + e.getMessage());
        }
    }
    
    /**
     * Actualiza la tabla con la lista de promociones
     * @param promociones Lista de promociones a mostrar
     */
    private void actualizarTabla(List<ModelPromocion> promociones) {
        modeloTabla.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (int i = 0; i < promociones.size(); i++) {
            ModelPromocion promo = promociones.get(i);
            Object[] fila = {
                false, // Checkbox
                i + 1, // Número
                promo.getNombre(),
                promo.getCodigo(),
                promo.getTipoDescuento(),
                formatearValor(promo.getValorDescuento(), promo.getTipoDescuento()),
                promo.getFechaInicio() != null ? promo.getFechaInicio().format(formatter) : "",
                promo.getFechaFin() != null ? promo.getFechaFin().format(formatter) : "",
                promo.isActiva() ? "Sí" : "No"
            };
            modeloTabla.addRow(fila);
        }
    }
    
    /**
     * Formatea el valor del descuento según su tipo
     */
    private String formatearValor(double valor, String tipo) {
        if ("PORCENTAJE".equals(tipo)) {
            return String.format("%.1f%%", valor);
        } else {
            return String.format("$%.2f", valor);
        }
    }
    
    /**
     * Busca promociones por término de búsqueda
     */
    private void buscarPromociones(String termino) {
        try {
            if (termino.trim().isEmpty()) {
                actualizarTabla(listaPromociones);
            } else {
                List<ModelPromocion> resultados = serviceDescuento.search(termino);
                actualizarTabla(resultados);
            }
        } catch (SQLException e) {
            mostrarError("Error en la búsqueda: " + e.getMessage());
        }
    }
    
    /**
     * Abre el diálogo para crear una nueva promoción
     */
    private void crearPromocion() {
        
        try {
            CreateDescuento createForm = new CreateDescuento();
            
            // Configurar ComboBox de tipos
            createForm.cbxaplicapara.removeAllItems();
            createForm.cbxaplicapara.addItem("Porcentaje (%)");
            createForm.cbxaplicapara.addItem("Monto fijo ($)");
            
            // Configurar ComboBox de tipo de descuento
            createForm.cbxtipo.removeAllItems();
            createForm.cbxtipo.addItem("Rol Usuario");
            createForm.cbxtipo.addItem("Usuario");
            createForm.cbxtipo.addItem("Categoría");
            createForm.cbxtipo.addItem("Marca");
            createForm.cbxtipo.addItem("Producto");
            
            // Asegurar que la tabla esté cargada
            createForm.load();
            
            SimpleModalBorder.Option[] options = {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION)
            };
            
            // Crear el modal border
            SimpleModalBorder border = new SimpleModalBorder(
                createForm, "Nueva Promoción", options,
                (controller, action) -> {
                    if (action == SimpleModalBorder.OK_OPTION) {
                        if (validarYGuardarPromocion(createForm)) {
                            controller.close();
                            cargarDatos();
                            Toast.show(vista, Toast.Type.SUCCESS, "Promoción creada exitosamente");
                        } else {
                            // No cerrar si la validación falla
                        }
                    } else if (action == SimpleModalBorder.CANCEL_OPTION) {
                        controller.close();  // cerrar solo si fue cancelar
                    } else {
                        // Ignorar cualquier otro evento de acción para que NO cierre el modal.
                        System.out.println("acción ignorada (no cierra modal)");
                    }
                }
            );
            
            // Mostrar el modal
            ModalDialog.showModal(vista, border);
            
        } catch (Exception e) {
            System.err.println("Error al crear promoción: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al abrir el formulario de nueva promoción: " + e.getMessage());
        }
    }



    
    /**
     * Valida y guarda una nueva promoción
     */
    private boolean validarYGuardarPromocion(CreateDescuento form) {
        try {
            // Delegar la validación y guardado al método del formulario
            // que ya tiene toda la lógica implementada correctamente
            form.guardarPromocion();
            return true; // Si llegamos aquí, el guardado fue exitoso
            
        } catch (Exception e) {
            mostrarError("Error al guardar la promoción: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene las promociones seleccionadas en la tabla
     */
    private List<ModelPromocion> obtenerSeleccionados() {
        java.util.List<ModelPromocion> seleccionados = new java.util.ArrayList<>();
        
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) modeloTabla.getValueAt(i, 0);
            if (seleccionado != null && seleccionado) {
                seleccionados.add(listaPromociones.get(i));
            }
        }
        
        return seleccionados;
    }
    
    /**
     * Elimina las promociones seleccionadas
     */
    private void eliminarSeleccionados() {
        List<ModelPromocion> seleccionados = obtenerSeleccionados();
        
        if (seleccionados.isEmpty()) {
            mostrarError("Seleccione al menos una promoción para eliminar");
            return;
        }
        
        int confirmacion = JOptionPane.showConfirmDialog(
            vista,
            "¿Está seguro de eliminar " + seleccionados.size() + " promoción(es)?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                int eliminados = 0;
                for (ModelPromocion promo : seleccionados) {
                    if (serviceDescuento.eliminarPromocion(promo.getIdPromocion())) {
                        eliminados++;
                    }
                }
                
                cargarDatos();
                Toast.show(vista, Toast.Type.SUCCESS, 
                    "Se eliminaron " + eliminados + " promoción(es) exitosamente");
                    
            } catch (SQLException e) {
                mostrarError("Error al eliminar promociones: " + e.getMessage());
            }
        }
    }
    
    /**
     * Edita la promoción seleccionada
     */
    private void editarPromocion() {
        List<ModelPromocion> seleccionados = obtenerSeleccionados();
        
        if (seleccionados.isEmpty()) {
            mostrarError("Seleccione una promoción para editar");
            return;
        }
        
        if (seleccionados.size() > 1) {
            mostrarError("Seleccione solo una promoción para editar");
            return;
        }
        
        ModelPromocion promocionAEditar = seleccionados.get(0);
        
        try {
            CreateDescuento editForm = new CreateDescuento();
            
            // Configurar ComboBox de tipos
            editForm.cbxaplicapara.removeAllItems();
            editForm.cbxaplicapara.addItem("Porcentaje (%)");
            editForm.cbxaplicapara.addItem("Monto fijo ($)");
            
            // Configurar ComboBox de tipo de descuento
            editForm.cbxtipo.removeAllItems();
            editForm.cbxtipo.addItem("Rol Usuario");
            editForm.cbxtipo.addItem("Usuario");
            editForm.cbxtipo.addItem("Categoría");
            editForm.cbxtipo.addItem("Marca");
            editForm.cbxtipo.addItem("Producto");
            
            // Asegurar que la tabla esté cargada
            editForm.load();
            
            // Cargar datos de la promoción seleccionada
            editForm.cargarDatosPromocion(promocionAEditar.getNombre());
            
            SimpleModalBorder.Option[] options = {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION)
            };
            
            // Crear el modal border
            SimpleModalBorder border = new SimpleModalBorder(
                editForm, "Editar Promoción", options,
                (controller, action) -> {
                    if (action == SimpleModalBorder.OK_OPTION) {
                        if (validarYActualizarPromocion(editForm, promocionAEditar)) {
                            controller.close();
                            cargarDatos();
                            Toast.show(vista, Toast.Type.SUCCESS, "Promoción actualizada exitosamente");
                        } else {
                            // No cerrar si la validación falla
                        }
                    } else if (action == SimpleModalBorder.CANCEL_OPTION) {
                        controller.close();  // cerrar solo si fue cancelar
                    } else {
                        // Ignorar cualquier otro evento de acción para que NO cierre el modal.
                        System.out.println("acción ignorada (no cierra modal)");
                    }
                }
            );
            
            // Mostrar el modal
            ModalDialog.showModal(vista, border);
            
        } catch (Exception e) {
            System.err.println("Error al editar promoción: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al abrir el formulario de edición: " + e.getMessage());
        }
    }
    
    /**
     * Valida y actualiza una promoción existente
     */
    private boolean validarYActualizarPromocion(CreateDescuento form, ModelPromocion promocionOriginal) {
        try {
            // Usar las validaciones del formulario CreateDescuento
            if (!form.validarCampos()) {
                return false; // Los errores ya se muestran en validarCampos()
            }
            
            // Verificar si el código ya existe (solo si cambió)
            String nuevocodigo = form.txtcodigo.getText().trim();
            if (!nuevocodigo.equals(promocionOriginal.getCodigo()) && 
                serviceDescuento.existeCodigoPromocion(nuevocodigo)) {
                mostrarError("El código ya existe");
                return false;
            }
            
            // Actualizar datos de la promoción
            promocionOriginal.setNombre(form.txtnombre.getText().trim());
            promocionOriginal.setCodigo(nuevocodigo);
            promocionOriginal.setDescripcion(form.txtdescripcion.getText().trim());
            
            // Convertir el valor del ComboBox al formato de base de datos
            String tipoSeleccionado = (String) form.cbxaplicapara.getSelectedItem();
            if (tipoSeleccionado.contains("Porcentaje")) {
                promocionOriginal.setTipoDescuento("PORCENTAJE");
            } else {
                promocionOriginal.setTipoDescuento("MONTO_FIJO");
            }
            
            promocionOriginal.setValorDescuento(Double.parseDouble(form.txtvalor.getText().trim()));
            
            // Obtener los límites de uso de los campos del formulario
            int limiteTotal = 0;
            int limiteUsuario = 0;
            
            // Parsear límite por usuario
            String limiteUsuarioTexto = form.txtlimiteUsuario.getText().trim();
            if (!limiteUsuarioTexto.isEmpty()) {
                try {
                    limiteUsuario = Integer.parseInt(limiteUsuarioTexto);
                } catch (NumberFormatException e) {
                    mostrarError("El límite por usuario debe ser un número válido");
                    return false;
                }
            }
            
            // Parsear límite total
            String limiteTotalTexto = form.txtlimitetotal.getText().trim();
            if (!limiteTotalTexto.isEmpty()) {
                try {
                    limiteTotal = Integer.parseInt(limiteTotalTexto);
                } catch (NumberFormatException e) {
                    mostrarError("El límite total debe ser un número válido");
                    return false;
                }
            }
            
            promocionOriginal.setLimiteUsoTotal(limiteTotal);
            promocionOriginal.setLimiteUsoPorUsuario(limiteUsuario);
            
            // Parsear fechas
            LocalDateTime fechaInicio = form.parsearFecha(form.txtFechaIn.getText().trim());
            LocalDateTime fechaFin = form.parsearFecha(form.txtFechafin.getText().trim());
            
            // Validar que las fechas sean válidas
            if (fechaInicio == null) {
                mostrarError("Fecha de inicio inválida. Use el formato dd/MM/yyyy");
                return false;
            }
            if (fechaFin == null) {
                mostrarError("Fecha de fin inválida. Use el formato dd/MM/yyyy");
                return false;
            }
            
            // Validar fechas
            if (fechaInicio.isAfter(fechaFin) || fechaInicio.isEqual(fechaFin)) {
                mostrarError("La fecha de inicio debe ser anterior a la fecha de fin");
                return false;
            }
            
            promocionOriginal.setFechaInicio(fechaInicio);
            promocionOriginal.setFechaFin(fechaFin);
            promocionOriginal.setActualizadoEn(LocalDateTime.now());
            
            // Actualizar en base de datos
            boolean resultado = serviceDescuento.actualizarPromocion(promocionOriginal);
            return resultado; // Retorna true si se actualizó correctamente
            
        } catch (SQLException e) {
            mostrarError("Error al actualizar la promoción: " + e.getMessage());
            return false;
        } catch (Exception e) {
            mostrarError("Error en los datos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Muestra un mensaje de error
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.btn_nuevo) {
            crearPromocion();
        } else if (e.getSource() == vista.btn_editar) {
            editarPromocion();
        } else if (e.getSource() == vista.btn_eliminar) {
            eliminarSeleccionados();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource() == vista.txtSearch) {
            buscarPromociones(vista.txtSearch.getText());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Implementar si es necesario
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // No implementado
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No implementado
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // No implementado
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // No implementado
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No implementado
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // No implementado
    }
}