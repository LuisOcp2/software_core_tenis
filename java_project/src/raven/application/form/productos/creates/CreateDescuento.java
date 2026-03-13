/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.productos.creates;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.geom.RoundRectangle2D;
import com.formdev.flatlaf.FlatClientProperties;
import raven.controlador.principal.conexion;
import raven.datetime.component.date.DatePicker;
import raven.dao.DescuentoDAO;
import raven.controlador.productos.ModelPromocion;
import raven.controlador.productos.ModelPromocionDetalle;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import raven.clases.productos.ServiceCategory;
import raven.clases.productos.ServiceBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelBrand;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.sql.Timestamp;
import raven.controlador.productos.controler.CtrlCreateDescuento; 

/**
 *
 * @author CrisDEV
 */
public class CreateDescuento extends javax.swing.JPanel {

    private DefaultTableModel modeloTabla;
    private DatePicker datePickerInicio;
    private DatePicker datePickerFin;
    private DescuentoDAO descuentoDAO;
    public CtrlCreateDescuento controlador;

    /**
     * Configura validación numérica para los campos de límites
     * Solo permite números enteros en txtlimiteUsuario y txtlimitetotal
     */
    private void configurarValidacionNumerica() {
        // Configurar validación para txtlimiteUsuario
        txtlimiteUsuario.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                // Solo permitir números y teclas de control (backspace, delete, etc.)
                if (!Character.isDigit(c) && c != java.awt.event.KeyEvent.VK_BACK_SPACE && c != java.awt.event.KeyEvent.VK_DELETE) {
                    evt.consume(); // Ignorar el carácter
                }
            }
        });
        
        // Configurar validación para txtlimitetotal
        txtlimitetotal.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                // Solo permitir números y teclas de control (backspace, delete, etc.)
                if (!Character.isDigit(c) && c != java.awt.event.KeyEvent.VK_BACK_SPACE && c != java.awt.event.KeyEvent.VK_DELETE) {
                    evt.consume(); // Ignorar el carácter
                }
            }
        });
        
        // Aplicar estilos a los campos de límites
        String limitFieldStyle = "arc:15;background:#4a5a6c;foreground:#ffffff;borderColor:#5a6a7c;focusWidth:2;focusColor:#007acc;innerFocusWidth:0";
        txtlimiteUsuario.putClientProperty(FlatClientProperties.STYLE, limitFieldStyle);
        txtlimitetotal.putClientProperty(FlatClientProperties.STYLE, limitFieldStyle);
        
        // Establecer placeholder text
        txtlimiteUsuario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 5 (opcional)");
        txtlimitetotal.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 100 (opcional)");
    }

    /**
     * Creates new form CreateDescuento
     */
    public CreateDescuento() {
        System.out.println("DEBUG: Iniciando constructor CreateDescuento");
        try {
            System.out.println("DEBUG: Llamando a initComponents()");
            initComponents();
            System.out.println("DEBUG: initComponents() completado exitosamente");
            
            // Inicializar DAO
            descuentoDAO = new DescuentoDAO();
            
            // Inicializar controlador
            controlador = new CtrlCreateDescuento(this);
            
            // Configurar ActionListeners de los botones
            configurarActionListeners();
            
            System.out.println("DEBUG: Llamando a configurarTabla()");
            configurarTabla(); // Solo configurar la tabla en el constructor
            System.out.println("DEBUG: configurarTabla() completado exitosamente");
            
            System.out.println("DEBUG: Constructor CreateDescuento completado exitosamente");
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO en constructor CreateDescuento: " + e.getMessage());
            e.printStackTrace();
            // Re-lanzar la excepción para que sea visible
            throw new RuntimeException("Error al inicializar CreateDescuento", e);
        }
    }
    
    /**
     * Actualiza una promoción existente en la base de datos
     */
    private boolean actualizarPromocion(ModelPromocion promocion) {
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement ps = conn.prepareStatement("")) {
            
            // Verificar si el código ya existe en otra promoción
            String checkSql = "SELECT COUNT(*) FROM promociones WHERE codigo = ? AND nombre != ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, promocion.getCodigo());
                checkPs.setString(2, promocion.getNombre());
                ResultSet rs = checkPs.executeQuery();
                
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("Error: El código '" + promocion.getCodigo() + "' ya existe en otra promoción");
                    JOptionPane.showMessageDialog(this, 
                        "El código '" + promocion.getCodigo() + "' ya existe en otra promoción. Por favor, use un código diferente.", 
                        "Código Duplicado", 
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
            // SQL para actualizar la promoción
            String sql = "UPDATE promociones SET " +
                        "codigo = ?, nombre = ?, descripcion = ?, tipo_descuento = ?, " +
                        "valor_descuento = ?, fecha_inicio = ?, fecha_fin = ?, " +
                        "limite_uso_total = ?, limite_uso_por_usuario = ? " +
                        "WHERE nombre = ?";
            
            try (PreparedStatement updatePs = conn.prepareStatement(sql)) {
                updatePs.setString(1, promocion.getCodigo());
                updatePs.setString(2, promocion.getNombre());
                updatePs.setString(3, promocion.getDescripcion());
                updatePs.setString(4, promocion.getTipoDescuento());
                updatePs.setDouble(5, promocion.getValorDescuento());
                updatePs.setTimestamp(6, Timestamp.valueOf(promocion.getFechaInicio()));
                updatePs.setTimestamp(7, Timestamp.valueOf(promocion.getFechaFin()));
                updatePs.setInt(8, promocion.getLimiteUsoTotal());
                updatePs.setInt(9, promocion.getLimiteUsoPorUsuario());
                updatePs.setString(10, promocion.getNombre()); // WHERE clause - usar nombre original
                
                int filasAfectadas = updatePs.executeUpdate();
                
                if (filasAfectadas > 0) {
                    // También actualizar los detalles de la promoción
                    actualizarDetallesPromocion(promocion.getNombre());
                    return true;
                }
                
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar promoción: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Actualiza los detalles de una promoción existente
     */
    private void actualizarDetallesPromocion(String nombrePromocion) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            // Primero obtener el ID de la promoción
            String sqlSelect = "SELECT id_promocion FROM promociones WHERE nombre = ?";
            try (PreparedStatement psSelect = conn.prepareStatement(sqlSelect)) {
                psSelect.setString(1, nombrePromocion);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        int idPromocion = rs.getInt("id_promocion");
                        
                        // Eliminar detalles existentes
                        String sqlDelete = "DELETE FROM promociones_detalle WHERE id_promocion = ?";
                        try (PreparedStatement psDelete = conn.prepareStatement(sqlDelete)) {
                            psDelete.setInt(1, idPromocion);
                            psDelete.executeUpdate();
                        }
                        
                        // Insertar nuevos detalles
                        insertarDetallesPromocion(idPromocion);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar detalles de promoción: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configura los ActionListeners para los botones
     * Los botones están manejados por el SimpleModalBorder en CtrlDescuento
     */
    private void configurarActionListeners() {
        // Los botones "Guardar" y "Cancelar" están manejados por el SimpleModalBorder
        // en CtrlDescuento.java, no necesitamos configurar ActionListeners aquí
        System.out.println("DEBUG: ActionListeners configurados - manejados por SimpleModalBorder");
    }

    /**
     * Método principal para guardar la promoción
     */
    public void guardarPromocion() {
        try {
            // Validar campos
            if (!validarCampos()) {
                return;
            }
            
            // Crear objeto ModelPromocion con los datos del formulario
            ModelPromocion promocion = crearPromocionDesdeFormulario();
            
            // Verificar si es una actualización o una nueva promoción
            // Como los botones están manejados por SimpleModalBorder, verificamos si ya existe el código
            String codigoPromocion = txtcodigo.getText().trim();
            boolean esActualizacion = !codigoPromocion.isEmpty() && 
                new raven.clases.productos.ServiceDescuento().existeCodigoPromocion(codigoPromocion);
            
            if (esActualizacion) {
                // Actualizar promoción existente
                boolean actualizado = actualizarPromocion(promocion);
                
                if (actualizado) {
                    // No mostrar JOptionPane aquí, se maneja en el controlador
                    limpiarCampos();
                } else {
                    throw new RuntimeException("Error al actualizar la promoción");
                }
            } else {
                // Insertar nueva promoción en la base de datos
                int idPromocion = descuentoDAO.insertarPromocion(promocion);
                
                if (idPromocion > 0) {
                    // Insertar detalles de promoción según los productos seleccionados
                    boolean detallesInsertados = insertarDetallesPromocion(idPromocion);
                    
                    if (detallesInsertados) {
                        // No mostrar JOptionPane aquí, se maneja en el controlador
                        limpiarCampos();
                    } else {
                        throw new RuntimeException("Error al guardar los detalles de la promoción");
                    }
                } else {
                    throw new RuntimeException("Error al guardar la promoción");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error al guardar promoción: " + e.getMessage());
            e.printStackTrace();
            // Re-lanzar la excepción para que sea manejada por el controlador
            throw new RuntimeException("Error al guardar la promoción: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea una fecha desde string a LocalDateTime
     */
    public LocalDateTime parsearFecha(String fechaTexto) {
        try {
            if (fechaTexto == null || fechaTexto.trim().isEmpty()) {
                throw new IllegalArgumentException("La fecha no puede estar vacía");
            }
            
            // Limpiar el texto de fecha
            String fechaLimpia = fechaTexto.trim();
            System.out.println("DEBUG: Parseando fecha: '" + fechaLimpia + "'");
            
            // Primero intentar el formato más común: dd/MM/yyyy
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDateTime fecha = LocalDateTime.parse(fechaLimpia + " 00:00:00", 
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                System.out.println("DEBUG: Fecha parseada exitosamente: " + fecha);
                return fecha;
            } catch (DateTimeParseException e) {
                System.out.println("DEBUG: Falló formato dd/MM/yyyy, intentando otros formatos");
            }
            
            // Intentar otros formatos
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDateTime fecha;
                    if (!fechaLimpia.contains(":")) {
                        // Agregar hora por defecto si no tiene hora
                        fecha = LocalDateTime.parse(fechaLimpia + " 00:00:00", 
                            DateTimeFormatter.ofPattern(formatter.toString() + " HH:mm:ss"));
                    } else {
                        fecha = LocalDateTime.parse(fechaLimpia, formatter);
                    }
                    System.out.println("DEBUG: Fecha parseada con formato alternativo: " + fecha);
                    return fecha;
                } catch (DateTimeParseException e) {
                    // Continuar con el siguiente formato
                }
            }
            
            // Si no se pudo parsear ningún formato, lanzar excepción
            throw new IllegalArgumentException("Formato de fecha no válido: " + fechaTexto + 
                ". Use el formato dd/MM/yyyy o dd-MM-yyyy");
        } catch (Exception e) {
            System.err.println("Error al parsear fecha: " + fechaTexto + " - " + e.getMessage());
            throw new IllegalArgumentException("Error al parsear fecha: " + e.getMessage(), e);
        }
    }
    
    /**
     * Inserta los detalles de promoción según los elementos seleccionados en la tabla
     */
    private boolean insertarDetallesPromocion(int idPromocion) {
        try {
            String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
            
            // Mapear el valor del ComboBox a los valores de la base de datos
            String tipoAplicacion;
            switch (tipoSeleccionado) {
                case "Rol Usuario":
                    tipoAplicacion = "ROL_USUARIO";
                    break;
                case "Usuario":
                    tipoAplicacion = "ROL_USUARIO"; // Corregido: Usuario también usa ROL_USUARIO
                    break;
                case "Categoría":
                    tipoAplicacion = "CATEGORIA";
                    break;
                case "Marca":
                    tipoAplicacion = "MARCA";
                    break;
                case "Producto":
                    tipoAplicacion = "PRODUCTO";
                    break;
                default:
                    tipoAplicacion = "PRODUCTO"; // Valor por defecto
                    break;
            }
            
            // Obtener elementos seleccionados de la tabla
            int[] elementosSeleccionados = getProductosSeleccionados();
            int[] filasSeleccionadas = getFilasSeleccionadas();
            
            if (elementosSeleccionados.length == 0) {
                // Si no hay elementos seleccionados, crear un detalle genérico con un objetivo por defecto
                ModelPromocionDetalle detalle = new ModelPromocionDetalle();
                detalle.setIdPromocion(idPromocion);
                detalle.setTipoAplicacion(tipoAplicacion);
                detalle.setActivo(true);
                
                // Asignar un objetivo por defecto según el tipo de aplicación para cumplir con la restricción
                switch (tipoAplicacion) {
                    case "ROL_USUARIO":
                        // Para rol usuario o usuario específico, usar un rol genérico si no se especifica uno
                        if (tipoSeleccionado.equals("Usuario")) {
                            // Para usuario específico, usar el primer usuario disponible (ID 1 como ejemplo)
                            detalle.setIdUsuario(1);
                        } else {
                            // Para rol usuario, usar un rol genérico
                            detalle.setRolUsuario("cliente"); // Rol por defecto en minúsculas
                        }
                        break;
                    case "CATEGORIA":
                        // Para categoría, usar la primera categoría disponible (ID 1 como ejemplo)
                        detalle.setIdCategoria(1);
                        break;
                    case "MARCA":
                        // Para marca, usar la primera marca disponible (ID 1 como ejemplo)
                        detalle.setIdMarca(1);
                        break;
                    case "PRODUCTO":
                        // Para producto, usar el primer producto disponible (ID 1 como ejemplo)
                        detalle.setIdProducto(1);
                        break;
                    default:
                        // Valor por defecto: aplicar a todos los productos
                        detalle.setIdProducto(1);
                        break;
                }
                
                int idDetalle = descuentoDAO.insertarPromocionDetalle(detalle);
                return idDetalle > 0;
            } else {
                // Insertar un detalle por cada elemento seleccionado
                boolean todosInsertados = true;
                for (int i = 0; i < elementosSeleccionados.length; i++) {
                    int idElemento = elementosSeleccionados[i];
                    int filaSeleccionada = filasSeleccionadas[i];
                    ModelPromocionDetalle detalle = new ModelPromocionDetalle();
                    detalle.setIdPromocion(idPromocion);
                    detalle.setTipoAplicacion(tipoAplicacion);
                    detalle.setActivo(true);
                    
                    // Asignar el ID al campo correspondiente según el tipo de aplicación
                    switch (tipoAplicacion) {
                        case "ROL_USUARIO":
                            // Verificar si es un usuario específico o un rol
                            if (tipoSeleccionado.equals("Usuario")) {
                                // Para usuario específico, asignar SOLO el ID del usuario
                                if (idElemento > 0) {
                                    detalle.setIdUsuario(idElemento);
                                    // Asegurar que rolUsuario sea null
                                    detalle.setRolUsuario(null);
                                }
                            } else {
                                // Para rol usuario, asignar SOLO el nombre del rol
                                String nombreRol = obtenerNombreElemento(filaSeleccionada);
                                detalle.setRolUsuario(nombreRol);
                                // Asegurar que idUsuario sea null
                                detalle.setIdUsuario(null);
                            }
                            break;
                        case "CATEGORIA":
                            detalle.setIdCategoria(idElemento);
                            break;
                        case "MARCA":
                            detalle.setIdMarca(idElemento);
                            break;
                        case "PRODUCTO":
                            detalle.setIdProducto(idElemento);
                            break;
                    }
                    
                    int idDetalle = descuentoDAO.insertarPromocionDetalle(detalle);
                    if (idDetalle <= 0) {
                        todosInsertados = false;
                    }
                }
                return todosInsertados;
            }
            
        } catch (Exception e) {
            System.err.println("Error al insertar detalles de promoción: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Valida que todos los campos requeridos estén completos
     */
    public boolean validarCampos() {
        // Validar código
        if (txtcodigo.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El código es requerido", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtcodigo.requestFocus();
            return false;
        }
        
        // Validar nombre
        if (txtnombre.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El nombre es requerido", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtnombre.requestFocus();
            return false;
        }
        
        // Validar descripción
        if (txtdescripcion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "La descripción es requerida", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtdescripcion.requestFocus();
            return false;
        }
        
        // Validar valor de descuento
        if (txtvalor.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El valor del descuento es requerido", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtvalor.requestFocus();
            return false;
        }
        
        try {
            double valor = Double.parseDouble(txtvalor.getText().trim());
            if (valor <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "El valor del descuento debe ser mayor a 0", 
                    "Validación", 
                    JOptionPane.WARNING_MESSAGE);
                txtvalor.requestFocus();
                return false;
            }
            
            // Si es porcentaje, validar que no sea mayor a 100
            if (cbxtipo.getSelectedItem().toString().contains("Porcentaje") && valor > 100) {
                JOptionPane.showMessageDialog(this, 
                    "El porcentaje no puede ser mayor a 100%", 
                    "Validación", 
                    JOptionPane.WARNING_MESSAGE);
                txtvalor.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "El valor del descuento debe ser un número válido", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtvalor.requestFocus();
            return false;
        }
        
        // Validar fechas
        if (txtFechaIn.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "La fecha de inicio es requerida", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtFechaIn.requestFocus();
            return false;
        }
        
        if (txtFechafin.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "La fecha de fin es requerida", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtFechafin.requestFocus();
            return false;
        }
        
        // Validar que la fecha de inicio sea anterior a la fecha de fin
        LocalDateTime fechaInicio = parsearFecha(txtFechaIn.getText().trim());
        LocalDateTime fechaFin = parsearFecha(txtFechafin.getText().trim());
        
        if (fechaInicio.isAfter(fechaFin)) {
            JOptionPane.showMessageDialog(this, 
                "La fecha de inicio debe ser anterior a la fecha de fin", 
                "Validación", 
                JOptionPane.WARNING_MESSAGE);
            txtFechaIn.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Crea un objeto ModelPromocion con los datos del formulario
     */
    private ModelPromocion crearPromocionDesdeFormulario() {
        try {
            ModelPromocion promocion = new ModelPromocion();
            
            promocion.setCodigo(txtcodigo.getText().trim());
            promocion.setNombre(txtnombre.getText().trim());
            promocion.setDescripcion(txtdescripcion.getText().trim());
            
            // Mapear tipo de descuento
            String tipoSeleccionado = cbxtipo.getSelectedItem() != null ? 
                cbxtipo.getSelectedItem().toString() : "Porcentaje";
            if (tipoSeleccionado.contains("Porcentaje")) {
                promocion.setTipoDescuento("PORCENTAJE");
            } else {
                promocion.setTipoDescuento("MONTO_FIJO");
            }
            
            // Validar y parsear valor del descuento
            String valorTexto = txtvalor.getText().trim();
            if (!valorTexto.isEmpty()) {
                try {
                    promocion.setValorDescuento(Double.parseDouble(valorTexto));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El valor del descuento debe ser un número válido");
                }
            } else {
                promocion.setValorDescuento(0.0);
            }
            
            // Parsear fechas con validación
            LocalDateTime fechaInicio = parsearFecha(txtFechaIn.getText().trim());
            LocalDateTime fechaFin = parsearFecha(txtFechafin.getText().trim());
            
            if (fechaInicio == null) {
                throw new IllegalArgumentException("La fecha de inicio es requerida");
            }
            if (fechaFin == null) {
                throw new IllegalArgumentException("La fecha de fin es requerida");
            }
            
            // Validar que la fecha de fin sea posterior a la fecha de inicio
            if (!fechaFin.isAfter(fechaInicio)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }
            
            promocion.setFechaInicio(fechaInicio);
            promocion.setFechaFin(fechaFin);
            
            // Límites opcionales con validación
            String limiteUsuarioTexto = txtlimiteUsuario.getText().trim();
            if (!limiteUsuarioTexto.isEmpty()) {
                try {
                    promocion.setLimiteUsoPorUsuario(Integer.parseInt(limiteUsuarioTexto));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El límite por usuario debe ser un número entero válido");
                }
            }
            
            String limiteTotalTexto = txtlimitetotal.getText().trim();
            if (!limiteTotalTexto.isEmpty()) {
                try {
                    promocion.setLimiteUsoTotal(Integer.parseInt(limiteTotalTexto));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El límite total debe ser un número entero válido");
                }
            }
            
            promocion.setActiva(true);
            
            // Establecer minCompra como 0.0 por defecto (no hay campo específico en el formulario)
            promocion.setMinCompra(0.0);
            
            return promocion;
        } catch (Exception e) {
            System.err.println("Error al crear promoción desde formulario: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al procesar los datos del formulario: " + e.getMessage(), e);
        }
    }

    /**
     * Limpia todos los campos del formulario
     */
    private void limpiarCampos() {
        txtcodigo.setText("");
        txtnombre.setText("");
        txtdescripcion.setText("");
        txtvalor.setText("");
        txtFechaIn.setText("");
        txtFechafin.setText("");
        txtlimiteUsuario.setText("");
        txtlimitetotal.setText("");
        
        // Resetear comboboxes a su primer elemento
        if (cbxtipo.getItemCount() > 0) {
            cbxtipo.setSelectedIndex(0);
        }
        if (cbxaplicapara.getItemCount() > 0) {
            cbxaplicapara.setSelectedIndex(0);
        }
        
        // Limpiar selecciones de la tabla
        if (table != null && table.getModel() instanceof DefaultTableModel) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(false, i, 0); // Desmarcar checkbox
            }
        }
    }

    // Método cargarDatosTabla() implementado más adelante en la línea 564
    
    // Método getProductosSeleccionados() implementado más adelante en la línea 590
    public void load() {
        System.out.println("DEBUG: Iniciando método load()");
        try {
            // Configurar DatePickers
            datePickerInicio = new DatePicker();
            datePickerFin = new DatePicker();
            
            configurarEstilosCamposFecha();
            configurarListenerTipo();
            configurarListenerAplicaPara();
            
            // Aplicar esquema de colores oscuro
            aplicarEsquemaColorOscuro();
            
            configurarValidacionNumerica();
            
            cargarDatosTabla();
        } catch (Exception e) {
            System.err.println("ERROR en load(): " + e.getMessage());
            e.printStackTrace();
         
        }
    }

    /**
     * Configura los estilos de los campos de fecha txtFechaIn y txtFechafin
     * Aplica bordes redondeados y colores personalizados para tema oscuro
     */
    private void configurarEstilosCamposFecha() {
        String textFieldStyle = "arc:15;background:#4a5a6c;foreground:#ffffff;borderColor:#5a6a7c;focusWidth:2;focusColor:#007acc;innerFocusWidth:0";
        
        // Aplicar estilos a los campos de fecha
        txtFechaIn.putClientProperty(FlatClientProperties.STYLE, textFieldStyle);
        txtFechafin.putClientProperty(FlatClientProperties.STYLE, textFieldStyle);
        
        // Configurar DatePicker
        configurarDatePickers();
    }
    
    /**
     * Aplica el esquema de colores oscuro a todos los componentes
     */
    private void aplicarEsquemaColorOscuro() {
        // Estilos para paneles principales con fondo oscuro
        String panelOscuroStyle = "arc:20;background:#3a4a5c;border:1,1,1,1,#4a5a6c";
        
        // Aplicar a paneles principales
        jPanel1.putClientProperty(FlatClientProperties.STYLE, panelOscuroStyle);
        jPanel4.putClientProperty(FlatClientProperties.STYLE, panelOscuroStyle);
        panel2.putClientProperty(FlatClientProperties.STYLE, panelOscuroStyle);
        panel3.putClientProperty(FlatClientProperties.STYLE, panelOscuroStyle);
        
        // Estilos para campos de texto con fondo oscuro
        String campoTextoStyle = "arc:15;background:#4a5a6c;foreground:#ffffff;borderColor:#5a6a7c;focusWidth:2;focusColor:#007acc;innerFocusWidth:0";
        
        // Aplicar a todos los campos de texto
        txtcodigo.putClientProperty(FlatClientProperties.STYLE, campoTextoStyle);
        txtnombre.putClientProperty(FlatClientProperties.STYLE, campoTextoStyle);
        txtdescripcion.putClientProperty(FlatClientProperties.STYLE, campoTextoStyle);
        txtvalor.putClientProperty(FlatClientProperties.STYLE, campoTextoStyle);
        
        // Estilos para ComboBox
        String comboBoxStyle = "arc:15;background:#4a5a6c;foreground:#ffffff;borderColor:#5a6a7c";
        cbxtipo.putClientProperty(FlatClientProperties.STYLE, comboBoxStyle);
        cbxaplicapara.putClientProperty(FlatClientProperties.STYLE, comboBoxStyle);
        
        // Estilos para botones con colores específicos - diseño mejorado
        //btnGuardar.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:#28a745;foreground:#ffffff;borderWidth:0;focusWidth:0;font:bold 14");
        //btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:#dc3545;foreground:#ffffff;borderWidth:0;focusWidth:0;font:bold 14");
        
        // Estilos para la tabla
        String tablaStyle = "background:#3a4a5c;foreground:#ffffff;gridColor:#5a6a7c;selectionBackground:#007acc;selectionForeground:#ffffff";
        table.putClientProperty(FlatClientProperties.STYLE, tablaStyle);
        
        // Estilos para el header de la tabla
        String headerStyle = "background:#2a3a4c;foreground:#ffffff;separatorColor:#4a5a6c;font:bold";
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, headerStyle);
        
        // Estilos para el ScrollPane
        String scrollStyle = "background:#3a4a5c;border:1,1,1,1,#4a5a6c";
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, scrollStyle);
        
        // Configurar colores de las etiquetas para tema oscuro
        configurarEtiquetasOscuras();
    }
    
    /**
     * Configura las etiquetas para el tema oscuro
     */
    private void configurarEtiquetasOscuras() {
        String labelStyle = "foreground:#ffffff;font:bold";
        
        jLabel1.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel2.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel3.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel4.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel5.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel6.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel7.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel8.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel9.putClientProperty(FlatClientProperties.STYLE, labelStyle);
        jLabel10.putClientProperty(FlatClientProperties.STYLE, labelStyle);
    }
    
    /**
     * Configura los DatePicker para los campos de fecha
     */
    private void configurarDatePickers() {
        // Configurar DatePicker para fecha de inicio
        datePickerInicio.setCloseAfterSelected(true);
        datePickerInicio.setEditor(txtFechaIn);
        
        // Configurar DatePicker para fecha de fin
        datePickerFin.setCloseAfterSelected(true);
        datePickerFin.setEditor(txtFechafin);
        
        // Configurar formato de fecha por defecto
        configurarFormatoFecha();
    }
    
    /**
     * Configura el formato de fecha por defecto en los campos
     */
    private void configurarFormatoFecha() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.util.Date fechaActual = new java.util.Date();
        
        // Establecer fecha actual como valor por defecto si los campos están vacíos
        if (txtFechaIn.getText().isEmpty()) {
            txtFechaIn.setText(sdf.format(fechaActual));
        }
        if (txtFechafin.getText().isEmpty()) {
            txtFechafin.setText(sdf.format(fechaActual));
        }
    }

    /**
     * Configura el listener para el ComboBox cbxtipo
     */
    private void configurarListenerTipo() {
        cbxtipo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("DEBUG: Cambio detectado en cbxtipo: " + cbxtipo.getSelectedItem());
                cargarDatosTabla();
            }
        });
    }

    /**
     * Configura el listener para el ComboBox cbxaplicapara para cambiar el formato del campo valor
     */
    private void configurarListenerAplicaPara() {
        cbxaplicapara.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarFormatoValor();
            }
        });
        
        // Configurar formato inicial
        actualizarFormatoValor();
    }
    
    /**
     * Actualiza el formato del campo txtvalor y la etiqueta según el tipo seleccionado
     */
    private void actualizarFormatoValor() {
        String tipoSeleccionado = (String) cbxaplicapara.getSelectedItem();
        
        if (tipoSeleccionado != null) {
            if (tipoSeleccionado.contains("Porcentaje")) {
                // Formato para porcentaje
                jLabel6.setText("Valor (%)");
                configurarFormatoPorcentaje();
            } else if (tipoSeleccionado.contains("Monto fijo")) {
                // Formato para monto fijo
                jLabel6.setText("Valor ($)");
                configurarFormatoMoneda();
            }
        }
    }
    
    /**
     * Configura el formato de porcentaje para el campo txtvalor
     */
    private void configurarFormatoPorcentaje() {
        // Limpiar el campo si tiene formato de moneda
        String valorActual = txtvalor.getText().replaceAll("[^0-9.,]", "");
        txtvalor.setText(valorActual);
        
        // Agregar placeholder y estilo para porcentaje
        txtvalor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 15.5");
        txtvalor.setToolTipText("Ingrese el porcentaje de descuento (0-100)");
    }
    
    /**
     * Configura el formato de moneda para el campo txtvalor
     */
    private void configurarFormatoMoneda() {
        // Limpiar el campo si tiene formato de porcentaje
        String valorActual = txtvalor.getText().replaceAll("[^0-9.,]", "");
        txtvalor.setText(valorActual);
        
        // Agregar placeholder y estilo para moneda
        txtvalor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 1500.00");
        txtvalor.setToolTipText("Ingrese el monto fijo de descuento");
    }

    /**
     * Configura la estructura de la tabla
     */
    private void configurarTabla() {
        // Configurar columnas según el tipo seleccionado
        String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
        String[] columnas;
        
        if ("Producto".equals(tipoSeleccionado)) {
            columnas = new String[]{"Seleccione", "Imagen", "Nombre"};
        } else {
            columnas = new String[]{"Seleccione", "Nombre"};
        }
        
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                } else if ("Producto".equals(cbxtipo.getSelectedItem()) && columnIndex == 1) {
                    return javax.swing.ImageIcon.class; // Para mostrar imágenes
                }
                return String.class;
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                // Solo la columna de selección es editable
                return column == 0;
            }
        };
        table.setModel(modeloTabla);
        
        // Configurar ancho de columnas según el tipo
        if ("Producto".equals(tipoSeleccionado)) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);  // Seleccione
            table.getColumnModel().getColumn(1).setPreferredWidth(80);  // Imagen
            table.getColumnModel().getColumn(2).setPreferredWidth(300); // Nombre (más ancho sin las columnas de límites)
            
            // Configurar altura de filas para mostrar imágenes
            table.setRowHeight(60);
        } else {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);  // Seleccione
            table.getColumnModel().getColumn(1).setPreferredWidth(300); // Nombre (más ancho sin las columnas de límites)
            
            // Altura normal para otros tipos
            table.setRowHeight(45);
        }
        
        // Agregar listener para detectar cambios en la selección
        modeloTabla.addTableModelListener(e -> {
            if (e.getColumn() == 0) { // Solo para la columna de selección
                int fila = e.getFirstRow();
                if (fila >= 0 && fila < modeloTabla.getRowCount()) {
                    Boolean seleccionado = (Boolean) modeloTabla.getValueAt(fila, 0);
                    if (seleccionado != null) {
                        manejarCambioSeleccion(fila, seleccionado);
                    }
                }
            }
        });
    }

    /**
     * Carga datos de ejemplo en la tabla
     */
    private void cargarDatosTabla() {
        try {
            // Limpiar tabla
            if (modeloTabla != null) {
                modeloTabla.setRowCount(0);
            }
            
            // Reconfigurar la tabla según el tipo seleccionado
            configurarTabla();
            
            // Obtener el tipo seleccionado
            String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
            System.out.println("DEBUG: Tipo seleccionado: " + tipoSeleccionado);
            
            // Cargar datos según el tipo seleccionado
            if (tipoSeleccionado != null) {
                switch (tipoSeleccionado.toLowerCase()) {
                    case "producto":
                        cargarProductosDisponibles();
                        break;
                    case "rol usuario":
                        cargarRolesDisponibles();
                        break;
                    case "usuario":
                        cargarUsuariosDisponibles();
                        break;
                    case "categoría":
                        cargarCategoriasDisponibles();
                        break;
                    case "marca":
                        cargarMarcasDisponibles();
                        break;
                    default:
                        System.out.println("DEBUG: Tipo no reconocido, cargando productos por defecto");
                        cargarProductosDisponibles();
                        break;
                }
            } else {
                // Si no hay selección, cargar productos por defecto
                cargarProductosDisponibles();
            }
            
        } catch (Exception e) {
            System.err.println("Error al cargar datos en la tabla: " + e.getMessage());
            e.printStackTrace();
            // En caso de error, cargar datos de ejemplo
            agregarDatosEjemplo();
        }
    }

    /**
     * Carga productos disponibles para aplicar descuentos
     */
    private void cargarProductosDisponibles() {
        System.out.println("DEBUG: Iniciando cargarProductosDisponibles()");
        try (Connection conn = conexion.getInstance().createConnection()) {
            System.out.println("DEBUG: Obteniendo conexión a la base de datos");
            System.out.println("DEBUG: Conexión establecida exitosamente");
            
            String sql = "SELECT id_producto, nombre FROM productos WHERE activo = 1 ORDER BY nombre";
            System.out.println("DEBUG: Ejecutando consulta SQL: " + sql);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                int contador = 0;
                while (rs.next()) {
                    int idProducto = rs.getInt("id_producto");
                    String nombreProducto = rs.getString("nombre");
                    
                    // Obtener la imagen del producto si el tipo es "Producto"
                    javax.swing.ImageIcon imagenIcon = null;
                    String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
                    if ("Producto".equals(tipoSeleccionado)) {
                        imagenIcon = obtenerImagenProducto(idProducto);
                    }
                
                Object[] fila;
                if ("Producto".equals(tipoSeleccionado)) {
                    fila = new Object[]{
                        false, // Checkbox no seleccionado por defecto
                        imagenIcon, // Imagen del producto
                        nombreProducto
                    };
                } else {
                    fila = new Object[]{
                        false, // Checkbox no seleccionado por defecto
                        nombreProducto
                    };
                }
                
                    modeloTabla.addRow(fila);
                    contador++;
                }
                System.out.println("DEBUG: Se cargaron " + contador + " productos desde la base de datos");
                
                // Si no hay productos en la BD, agregar datos de ejemplo
                if (modeloTabla.getRowCount() == 0) {
                    System.out.println("DEBUG: No hay productos en la BD, cargando datos de ejemplo");
                    agregarDatosEjemplo();
                }
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR SQL en cargarProductosDisponibles(): " + e.getMessage());
            e.printStackTrace();
            // En caso de error, cargar datos de ejemplo
            System.out.println("DEBUG: Cargando datos de ejemplo debido al error SQL");
            agregarDatosEjemplo();
            // Re-lanzar la excepción para que sea visible
            throw new RuntimeException("Error de base de datos en cargarProductosDisponibles()", e);
        } catch (Exception e) {
            System.err.println("ERROR GENERAL en cargarProductosDisponibles(): " + e.getMessage());
            e.printStackTrace();
            // Re-lanzar la excepción para que sea visible
            throw new RuntimeException("Error general en cargarProductosDisponibles()", e);
        }
    }

    /**
     * Agrega datos de ejemplo a la tabla
     */
    private void agregarDatosEjemplo() {
        String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
        
        String[] datosEjemplo = {
            "Zapatos Deportivos Nike",
            "Botas de Cuero", 
            "Sandalias de Verano",
            "Zapatos Formales",
            "Zapatillas Casuales"
        };
        
        for (String dato : datosEjemplo) {
            Object[] fila;
            if ("Producto".equals(tipoSeleccionado)) {
                fila = new Object[]{
                    false, // Checkbox no seleccionado
                    crearImagenPorDefecto(), // Imagen por defecto
                    dato // Nombre del producto
                };
            } else {
                fila = new Object[]{
                    false, // Checkbox no seleccionado
                    dato // Nombre del producto
                };
            }
            modeloTabla.addRow(fila);
        }
    }

    /**
     * Obtiene los productos seleccionados en la tabla
     * @return Array de índices de productos seleccionados
     */
    public int[] getProductosSeleccionados() {
        java.util.List<Integer> seleccionados = new java.util.ArrayList<>();
        java.util.List<Integer> filasSeleccionadas = new java.util.ArrayList<>();
        
        String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
        
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) modeloTabla.getValueAt(i, 0);
            if (seleccionado != null && seleccionado) {
                try {
                    // Obtener el ID real del elemento según el tipo
                    int idElemento = obtenerIdElementoDeTabla(i, tipoSeleccionado);
                    
                    // Para roles de usuario, aceptamos -1 como válido (indica que se usa el nombre)
                    if (idElemento > 0 || (idElemento == -1 && "Rol Usuario".equals(tipoSeleccionado))) {
                        seleccionados.add(idElemento);
                        filasSeleccionadas.add(i); // Guardar también el índice de la fila
                    }
                } catch (Exception e) {
                    System.err.println("Error al obtener ID del elemento en fila " + i + ": " + e.getMessage());
                }
            }
        }
        
        return seleccionados.stream().mapToInt(i -> i).toArray();
    }
    
    /**
     * Obtiene las filas seleccionadas en la tabla
     * @return Array de índices de filas seleccionadas
     */
    public int[] getFilasSeleccionadas() {
        java.util.List<Integer> filasSeleccionadas = new java.util.ArrayList<>();
        
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) modeloTabla.getValueAt(i, 0);
            if (seleccionado != null && seleccionado) {
                filasSeleccionadas.add(i);
            }
        }
        
        return filasSeleccionadas.stream().mapToInt(i -> i).toArray();
    }
    
    /**
     * Obtiene el ID real del elemento de la tabla según el tipo de aplicación
     */
    private int obtenerIdElementoDeTabla(int fila, String tipoSeleccionado) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            String nombreElemento = obtenerNombreElemento(fila);
            
            // Mapear el valor del ComboBox a los valores de la base de datos
            String tipoAplicacion;
            switch (tipoSeleccionado) {
                case "Rol Usuario":
                    tipoAplicacion = "ROL_USUARIO";
                    break;
                case "Usuario":
                    tipoAplicacion = "USUARIO";
                    break;
                case "Categoría":
                    tipoAplicacion = "CATEGORIA";
                    break;
                case "Marca":
                    tipoAplicacion = "MARCA";
                    break;
                case "Producto":
                    tipoAplicacion = "PRODUCTO";
                    break;
                default:
                    tipoAplicacion = "PRODUCTO";
                    break;
            }
            
            return obtenerIdElemento(tipoAplicacion, nombreElemento, conn);
            
        } catch (SQLException e) {
            System.err.println("Error al obtener ID del elemento: " + e.getMessage());
            return -1;
        }
    }



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        panel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txtcodigo = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtnombre = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtdescripcion = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        cbxaplicapara = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        txtvalor = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        cbxtipo = new javax.swing.JComboBox<>();
        panel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        txtFechaIn = new javax.swing.JFormattedTextField();
        txtFechafin = new javax.swing.JFormattedTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtlimitetotal = new javax.swing.JTextField();
        txtlimiteUsuario = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Nueva promoción");

        panel2.setBackground(new java.awt.Color(51, 51, 255));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setText("Código");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setText("Nombre");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setText("Descripción");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel5.setText("Aplica por");

        cbxaplicapara.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Porcentaje (%)", "Monto fijo ($)" }));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel6.setText("Valor (%)");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel10.setText("Tipo de descuento");

        cbxtipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Rol Usuario", "Usuario", "Categoría", "Marca", "Producto" }));

        javax.swing.GroupLayout panel2Layout = new javax.swing.GroupLayout(panel2);
        panel2.setLayout(panel2Layout);
        panel2Layout.setHorizontalGroup(
            panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel2Layout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel2Layout.createSequentialGroup()
                        .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(panel2Layout.createSequentialGroup()
                                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtcodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(110, 110, 110)
                                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panel2Layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addGap(379, 379, 379))
                                    .addComponent(txtnombre)))
                            .addComponent(txtdescripcion, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panel2Layout.createSequentialGroup()
                                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addComponent(cbxtipo, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(cbxaplicapara, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(txtvalor, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(60, 60, 60))))
        );
        panel2Layout.setVerticalGroup(
            panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel2Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(txtcodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(txtnombre, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtdescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxtipo))
                    .addGroup(panel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxaplicapara, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panel2Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtvalor, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(34, Short.MAX_VALUE))
        );

        panel3.setBackground(new java.awt.Color(30, 173, 209));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setText("Inicio");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel8.setText("Fin");

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel9.setText("Límite total (opcional)");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel11.setText("Límite por Usuario (opcional)");

        javax.swing.GroupLayout panel3Layout = new javax.swing.GroupLayout(panel3);
        panel3.setLayout(panel3Layout);
        panel3Layout.setHorizontalGroup(
            panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel3Layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(txtFechaIn, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(txtFechafin, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(txtlimiteUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27)
                .addGroup(panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(txtlimitetotal, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(63, 63, 63))
        );
        panel3Layout.setVerticalGroup(
            panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel3Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtlimiteUsuario))
                    .addGroup(panel3Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtlimitetotal, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel3Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFechafin))
                    .addGroup(panel3Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFechaIn, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(30, 191, 234));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Selecione", "Nombre"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        table.setCellSelectionEnabled(true);
        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(27, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(panel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(63, 63, 63))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 53, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Método para manejar el evento del botón editar
     */
    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {
        int filaSeleccionada = table.getSelectedRow();
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Por favor, seleccione un descuento de la tabla para editar.", 
                "Selección requerida", 
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Obtener el ID de la promoción desde la tabla (asumiendo que está en una columna oculta o se puede obtener)
        // Por ahora, usaremos el nombre para buscar la promoción
        String nombrePromocion = (String) table.getValueAt(filaSeleccionada, 1);
        cargarDatosPromocion(nombrePromocion);
    }
    
    /**
     * Carga los datos de una promoción existente en el formulario
     */
    public void cargarDatosPromocion(String nombrePromocion) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            // Consulta para obtener los datos de la promoción
            String sql = "SELECT p.*, pd.tipo_aplicacion FROM promociones p " +
                        "LEFT JOIN promociones_detalle pd ON p.id_promocion = pd.id_promocion " +
                        "WHERE p.nombre = ? LIMIT 1";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombrePromocion);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Cargar datos básicos
                        txtcodigo.setText(rs.getString("codigo"));
                        txtnombre.setText(rs.getString("nombre"));
                        txtdescripcion.setText(rs.getString("descripcion"));
                        txtvalor.setText(String.valueOf(rs.getDouble("valor_descuento")));
                        
                        // Configurar tipo de descuento (cbxaplicapara)
                        String tipoDescuento = rs.getString("tipo_descuento");
                        if ("PORCENTAJE".equals(tipoDescuento)) {
                            cbxaplicapara.setSelectedIndex(0); // "Porcentaje (%)"
                        } else if ("MONTO_FIJO".equals(tipoDescuento)) {
                            cbxaplicapara.setSelectedIndex(1); // "Monto fijo ($)"
                        }
                        
                        // Configurar tipo de aplicación (cbxtipo) PRIMERO
                        String tipoAplicacion = rs.getString("tipo_aplicacion");
                        if (tipoAplicacion != null) {
                            switch (tipoAplicacion) {
                                case "ROL_USUARIO":
                                    cbxtipo.setSelectedItem("Rol Usuario");
                                    break;
                                case "USUARIO":
                                    cbxtipo.setSelectedItem("Usuario");
                                    break;
                                case "CATEGORIA":
                                    cbxtipo.setSelectedItem("Categoría");
                                    break;
                                case "MARCA":
                                    cbxtipo.setSelectedItem("Marca");
                                    break;
                                case "PRODUCTO":
                                    cbxtipo.setSelectedItem("Producto");
                                    break;
                            }
                        }
                        
                        // Cargar los datos de la tabla PRIMERO según el tipo configurado
                        System.out.println("DEBUG: Cargando datos de tabla para tipo: " + tipoAplicacion);
                        cargarDatosTabla();
                        
                        // Esperar un momento para que la tabla se cargue completamente
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            System.out.println("DEBUG: Cargando productos seleccionados para promoción: " + nombrePromocion);
                            cargarProductosSeleccionados(nombrePromocion);
                        });
                        
                        // Configurar fechas
                        java.sql.Date fechaInicio = rs.getDate("fecha_inicio");
                        java.sql.Date fechaFin = rs.getDate("fecha_fin");
                        
                        if (fechaInicio != null) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                            txtFechaIn.setText(sdf.format(fechaInicio));
                        }
                        
                        if (fechaFin != null) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                            txtFechafin.setText(sdf.format(fechaFin));
                        }
                        
                        // Cargar límites de uso
                        int limiteTotal = rs.getInt("limite_uso_total");
                        int limiteUsuario = rs.getInt("limite_uso_por_usuario");
                        
                        txtlimitetotal.setText(String.valueOf(limiteTotal));
                        txtlimiteUsuario.setText(String.valueOf(limiteUsuario));
                        
                        // Cambiar el texto del botón guardar para indicar que es una edición
                      //  btnGuardar.setText("Actualizar");
                        
                        javax.swing.JOptionPane.showMessageDialog(this, 
                            "Datos cargados correctamente. Puede modificar los campos y hacer clic en 'Actualizar'.", 
                            "Edición de promoción", 
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(this, 
                            "No se encontró la promoción seleccionada.", 
                            "Error", 
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al cargar datos de la promoción: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Error al cargar los datos de la promoción: " + e.getMessage(), 
                "Error de base de datos", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Carga los productos seleccionados de una promoción existente
     */
    private void cargarProductosSeleccionados(String nombrePromocion) {
        System.out.println("DEBUG: Iniciando cargarProductosSeleccionados para: " + nombrePromocion);
        try (Connection conn = conexion.getInstance().createConnection()) {
            
            // Primero, cargar los datos disponibles en la tabla según el tipo seleccionado
            cargarDatosTabla();
            System.out.println("DEBUG: Tabla cargada, filas disponibles: " + (modeloTabla != null ? modeloTabla.getRowCount() : "null"));
            System.out.println("DEBUG: Tipo seleccionado en cbxtipo: " + (cbxtipo.getSelectedItem() != null ? cbxtipo.getSelectedItem().toString() : "null"));
            
            // Luego, obtener los productos/elementos seleccionados de la promoción
            String sql = "SELECT pd.id_categoria, pd.id_marca, pd.id_producto, pd.id_usuario, pd.rol_usuario, pd.tipo_aplicacion " +
                        "FROM promociones p " +
                        "INNER JOIN promociones_detalle pd ON p.id_promocion = pd.id_promocion " +
                        "WHERE p.nombre = ? AND pd.activo = 1";
            
            System.out.println("DEBUG: Ejecutando consulta SQL: " + sql);
            System.out.println("DEBUG: Parámetro nombrePromocion: " + nombrePromocion);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombrePromocion);
                try (ResultSet rs = ps.executeQuery()) {
                    int contadorElementos = 0;
                    // Marcar los elementos seleccionados en la tabla
                    while (rs.next()) {
                        String tipoAplicacion = rs.getString("tipo_aplicacion");
                        int idProducto = rs.getInt("id_producto");
                        int idCategoria = rs.getInt("id_categoria");
                        int idMarca = rs.getInt("id_marca");
                        int idUsuario = rs.getInt("id_usuario");
                        String rolUsuario = rs.getString("rol_usuario");
                        
                        System.out.println("DEBUG: Encontrado elemento guardado - Tipo: " + tipoAplicacion + 
                                         ", ID_Producto: " + idProducto + 
                                         ", ID_Categoria: " + idCategoria + 
                                         ", ID_Marca: " + idMarca +
                                         ", ID_Usuario: " + idUsuario +
                                         ", Rol_Usuario: " + rolUsuario);
                        
                        System.out.println("DEBUG: Llamando marcarElementoSeleccionado para tipo: " + tipoAplicacion);
                        marcarElementoSeleccionado(tipoAplicacion, rs);
                        contadorElementos++;
                    }
                    System.out.println("DEBUG: Total de elementos encontrados en BD: " + contadorElementos);
                    
                    if (contadorElementos == 0) {
                        System.out.println("DEBUG: ¡ADVERTENCIA! No se encontraron elementos en promociones_detalle para la promoción: " + nombrePromocion);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al cargar productos seleccionados: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Marca un elemento como seleccionado en la tabla según el tipo de aplicación
     */
    private void marcarElementoSeleccionado(String tipoAplicacion, ResultSet rs) throws SQLException {
        if (modeloTabla == null) {
            System.out.println("DEBUG: modeloTabla es null, no se puede marcar elemento");
            return;
        }
        
        System.out.println("DEBUG: Marcando elemento de tipo: " + tipoAplicacion);
        
        switch (tipoAplicacion) {
            case "PRODUCTO":
                int idProducto = rs.getInt("id_producto");
                System.out.println("DEBUG: Marcando producto con ID: " + idProducto);
                marcarProductoPorId(idProducto);
                break;
            case "CATEGORIA":
                int idCategoria = rs.getInt("id_categoria");
                System.out.println("DEBUG: Marcando categoría con ID: " + idCategoria);
                marcarCategoriaPorId(idCategoria);
                break;
            case "MARCA":
                int idMarca = rs.getInt("id_marca");
                System.out.println("DEBUG: Marcando marca con ID: " + idMarca);
                marcarMarcaPorId(idMarca);
                break;
            case "ROL_USUARIO":
                String rolUsuario = rs.getString("rol_usuario");
                System.out.println("DEBUG: Marcando rol de usuario: " + rolUsuario);
                marcarRolUsuarioPorNombre(rolUsuario);
                break;
            case "USUARIO":
                int idUsuario = rs.getInt("id_usuario");
                System.out.println("DEBUG: Marcando usuario con ID: " + idUsuario);
                marcarUsuarioPorId(idUsuario);
                break;
            default:
                System.out.println("DEBUG: Tipo de aplicación no reconocido: " + tipoAplicacion);
        }
    }
    
    /**
     * Marca un producto como seleccionado en la tabla por su ID
     */
    private void marcarProductoPorId(int idProducto) {
        System.out.println("DEBUG: Buscando producto con ID: " + idProducto);
        try (Connection conn = conexion.getInstance().createConnection()) {
            String sql = "SELECT nombre FROM productos WHERE id_producto = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nombreProducto = rs.getString("nombre");
                        System.out.println("DEBUG: Producto encontrado: " + nombreProducto + ", marcando en columna 2");
                        // Para productos con imagen, el nombre está en la columna 2
                        marcarElementoEnTabla(nombreProducto, 2); // Columna 2 es el nombre del producto cuando hay imagen
                    } else {
                        System.out.println("DEBUG: ¡ADVERTENCIA! No se encontró producto con ID: " + idProducto);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar producto por ID: " + e.getMessage());
        }
    }
    
    /**
     * Marca una categoría como seleccionada en la tabla por su ID
     */
    private void marcarCategoriaPorId(int idCategoria) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            String sql = "SELECT nombre FROM categorias WHERE id_categoria = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCategoria);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nombreCategoria = rs.getString("nombre");
                        marcarElementoEnTabla(nombreCategoria, 1); // Columna 1 es el nombre
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar categoría por ID: " + e.getMessage());
        }
    }
    
    /**
     * Marca una marca como seleccionada en la tabla por su ID
     */
    private void marcarMarcaPorId(int idMarca) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            String sql = "SELECT nombre FROM marcas WHERE id_marca = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idMarca);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nombreMarca = rs.getString("nombre");
                        marcarElementoEnTabla(nombreMarca, 1); // Columna 1 es el nombre
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar marca por ID: " + e.getMessage());
        }
    }
    
    /**
     * Marca un rol de usuario como seleccionado en la tabla por su nombre
     */
    private void marcarRolUsuarioPorNombre(String rolUsuario) {
        System.out.println("DEBUG: Buscando rol de usuario: " + rolUsuario);
        if (modeloTabla == null) {
            System.out.println("DEBUG: modeloTabla es null, no se puede marcar rol");
            return;
        }
        
        System.out.println("DEBUG: Número de filas en tabla: " + modeloTabla.getRowCount());
        
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            Object valorCelda = modeloTabla.getValueAt(i, 1); // Columna 1 es el nombre del rol
            System.out.println("DEBUG: Fila " + i + ", valor: " + valorCelda);
            
            if (valorCelda != null && valorCelda.toString().equals(rolUsuario)) {
                System.out.println("DEBUG: Encontrado rol " + rolUsuario + " en fila " + i + ", marcando como seleccionado");
                modeloTabla.setValueAt(true, i, 0); // Marcar como seleccionado
                return;
            }
        }
        
        System.out.println("DEBUG: No se encontró el rol " + rolUsuario + " en la tabla");
    }
    
    /**
     * Marca un usuario específico como seleccionado en la tabla por su ID
     */
    private void marcarUsuarioPorId(int idUsuario) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            String sql = "SELECT nombre FROM usuarios WHERE id_usuario = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nombreUsuario = rs.getString("nombre");
                        marcarElementoEnTabla(nombreUsuario, 1); // Columna 1 es el nombre del usuario
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al marcar usuario por ID: " + e.getMessage());
        }
    }
    
    /**
     * Marca un elemento como seleccionado en la tabla buscando por nombre en la columna especificada
     */
    private void marcarElementoEnTabla(String nombre, int columna) {
        System.out.println("DEBUG: Intentando marcar elemento: " + nombre + " en columna: " + columna);
        if (modeloTabla == null || nombre == null) {
            System.out.println("DEBUG: No se puede marcar - modeloTabla es null: " + (modeloTabla == null) + ", nombre es null: " + (nombre == null));
            return;
        }
        
        System.out.println("DEBUG: Filas en tabla: " + modeloTabla.getRowCount());
        
        // Temporalmente remover el listener para evitar que se dispare durante la carga
        javax.swing.event.TableModelListener[] listeners = modeloTabla.getTableModelListeners();
        for (javax.swing.event.TableModelListener listener : listeners) {
            modeloTabla.removeTableModelListener(listener);
        }
        
        try {
            boolean encontrado = false;
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                Object valorCelda = modeloTabla.getValueAt(i, columna);
                System.out.println("DEBUG: Fila " + i + ", valor en columna " + columna + ": " + valorCelda);
                if (valorCelda != null && valorCelda.toString().equals(nombre)) {
                    modeloTabla.setValueAt(true, i, 0); // Marcar checkbox en columna 0
                    System.out.println("DEBUG: ¡Elemento marcado exitosamente en fila " + i + "!");
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                System.out.println("DEBUG: No se encontró el elemento '" + nombre + "' en la tabla");
            }
        } finally {
            // Restaurar los listeners
            for (javax.swing.event.TableModelListener listener : listeners) {
                modeloTabla.addTableModelListener(listener);
            }
        }
    }

    /**
     * Maneja los cambios de selección en la tabla y guarda/elimina automáticamente en promociones_detalle
     */
    private void manejarCambioSeleccion(int fila, boolean seleccionado) {
        // Solo procesar si tenemos un nombre de promoción válido
        String nombrePromocion = txtnombre.getText().trim();
        if (nombrePromocion.isEmpty()) {
            return; // No hacer nada si no hay nombre de promoción
        }
        
        try {
            String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
            String nombreElemento = obtenerNombreElemento(fila);
            
            if (nombreElemento == null || nombreElemento.isEmpty()) {
                return;
            }
            
            // Mapear el tipo de aplicación de la interfaz a los valores de la base de datos
            String tipoAplicacion;
            switch (tipoSeleccionado) {
                case "Rol Usuario":
                    tipoAplicacion = "ROL_USUARIO";
                    break;
                case "Usuario":
                    tipoAplicacion = "USUARIO";
                    break;
                case "Categoría":
                    tipoAplicacion = "CATEGORIA";
                    break;
                case "Marca":
                    tipoAplicacion = "MARCA";
                    break;
                case "Producto":
                    tipoAplicacion = "PRODUCTO";
                    break;
                default:
                    tipoAplicacion = "PRODUCTO"; // Valor por defecto
                    break;
            }
            
            if (seleccionado) {
                // Guardar en promociones_detalle
                guardarDetallePromocion(nombrePromocion, tipoAplicacion, nombreElemento, fila);
            } else {
                // Eliminar de promociones_detalle
                eliminarDetallePromocion(nombrePromocion, tipoAplicacion, nombreElemento);
            }
            
        } catch (Exception e) {
            System.err.println("Error al manejar cambio de selección: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el nombre del elemento en la fila especificada
     */
    private String obtenerNombreElemento(int fila) {
        String tipoSeleccionado = (String) cbxtipo.getSelectedItem();
        int columnaNombre = "Producto".equals(tipoSeleccionado) ? 2 : 1;
        
        Object valor = modeloTabla.getValueAt(fila, columnaNombre);
        return valor != null ? valor.toString() : null;
    }
    
    /**
     * Guarda un detalle de promoción en la base de datos
     */
    private void guardarDetallePromocion(String nombrePromocion, String tipoAplicacion, String nombreElemento, int fila) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            // Obtener el ID de la promoción
            int idPromocion = obtenerIdPromocion(nombrePromocion, conn);
            if (idPromocion <= 0) {
                System.out.println("DEBUG: No se encontró la promoción, no se puede guardar el detalle");
                return;
            }
            
            // Obtener el ID del elemento según el tipo
            int idElemento = obtenerIdElemento(tipoAplicacion, nombreElemento, conn);
            if (idElemento <= 0 && !"ROL_USUARIO".equals(tipoAplicacion)) {
                System.out.println("DEBUG: No se encontró el elemento " + nombreElemento + " de tipo " + tipoAplicacion);
                return;
            }
            
            // Verificar si ya existe el detalle
            if (existeDetallePromocion(idPromocion, tipoAplicacion, idElemento, nombreElemento, conn)) {
                System.out.println("DEBUG: El detalle ya existe, no se duplicará");
                return;
            }
            
            // Insertar el detalle (sin las columnas limite_por_usuario y limite_total que no existen)
            String sql = "INSERT INTO promociones_detalle (id_promocion, tipo_aplicacion, id_producto, id_categoria, id_marca, rol_usuario, activo, creado_en) VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPromocion);
                ps.setString(2, tipoAplicacion);
                
                // Configurar los IDs según el tipo
                System.out.println("DEBUG: Tipo de aplicación recibido: '" + tipoAplicacion + "'");
                switch (tipoAplicacion) {
                    case "PRODUCTO":
                        ps.setInt(3, idElemento);
                        ps.setNull(4, java.sql.Types.INTEGER);
                        ps.setNull(5, java.sql.Types.INTEGER);
                        ps.setNull(6, java.sql.Types.VARCHAR);
                        break;
                    case "CATEGORIA":
                        ps.setNull(3, java.sql.Types.INTEGER);
                        ps.setInt(4, idElemento);
                        ps.setNull(5, java.sql.Types.INTEGER);
                        ps.setNull(6, java.sql.Types.VARCHAR);
                        break;
                    case "MARCA":
                        ps.setNull(3, java.sql.Types.INTEGER);
                        ps.setNull(4, java.sql.Types.INTEGER);
                        ps.setInt(5, idElemento);
                        ps.setNull(6, java.sql.Types.VARCHAR);
                        break;
                    case "ROL_USUARIO":
                        ps.setNull(3, java.sql.Types.INTEGER);
                        ps.setNull(4, java.sql.Types.INTEGER);
                        ps.setNull(5, java.sql.Types.INTEGER);
                        ps.setString(6, nombreElemento);
                        break;
                    default:
                        System.err.println("DEBUG: Tipo de aplicación no reconocido: '" + tipoAplicacion + "'");
                        return;
                }
                
                int resultado = ps.executeUpdate();
                if (resultado > 0) {
                    System.out.println("DEBUG: Detalle de promoción guardado exitosamente para " + nombreElemento);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al guardar detalle de promoción: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Elimina un detalle de promoción de la base de datos
     */
    private void eliminarDetallePromocion(String nombrePromocion, String tipoAplicacion, String nombreElemento) {
        try (Connection conn = conexion.getInstance().createConnection()) {
            // Obtener el ID de la promoción
            int idPromocion = obtenerIdPromocion(nombrePromocion, conn);
            if (idPromocion <= 0) {
                return;
            }
            
            // Obtener el ID del elemento según el tipo
            int idElemento = obtenerIdElemento(tipoAplicacion, nombreElemento, conn);
            
            String sql = "DELETE FROM promociones_detalle WHERE id_promocion = ? AND tipo_aplicacion = ?";
            
            // Agregar condición específica según el tipo
            switch (tipoAplicacion) {
                case "PRODUCTO":
                    sql += " AND id_producto = ?";
                    break;
                case "CATEGORIA":
                    sql += " AND id_categoria = ?";
                    break;
                case "MARCA":
                    sql += " AND id_marca = ?";
                    break;
                case "ROL_USUARIO":
                    sql += " AND rol_usuario = ?";
                    break;
            }
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPromocion);
                ps.setString(2, tipoAplicacion);
                
                if ("ROL_USUARIO".equals(tipoAplicacion)) {
                    ps.setString(3, nombreElemento);
                } else {
                    ps.setInt(3, idElemento);
                }
                
                int resultado = ps.executeUpdate();
                if (resultado > 0) {
                    System.out.println("DEBUG: Detalle de promoción eliminado exitosamente para " + nombreElemento);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al eliminar detalle de promoción: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el ID de una promoción por su nombre
     */
    private int obtenerIdPromocion(String nombrePromocion, Connection conn) throws SQLException {
        String sql = "SELECT id_promocion FROM promociones WHERE nombre = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombrePromocion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_promocion");
                }
                return 0;
            }
        }
    }
    
    /**
     * Obtiene el ID de un elemento según su tipo y nombre
     */
    private int obtenerIdElemento(String tipoAplicacion, String nombreElemento, Connection conn) throws SQLException {
        String sql = "";
        switch (tipoAplicacion) {
            case "PRODUCTO":
                sql = "SELECT id_producto FROM productos WHERE nombre = ?";
                break;
            case "CATEGORIA":
                sql = "SELECT id_categoria FROM categorias WHERE nombre = ?";
                break;
            case "MARCA":
                sql = "SELECT id_marca FROM marcas WHERE nombre = ?";
                break;
            case "ROL_USUARIO":
                // Para rol de usuario, verificamos que el rol existe en la base de datos
                // y retornamos -1 para indicar que se debe usar el nombre en lugar de un ID
                sql = "SELECT DISTINCT rol FROM usuarios WHERE rol = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nombreElemento);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return -1; // Indica que el rol existe y se debe usar el nombre
                        }
                        return 0; // El rol no existe
                    }
                }
            case "USUARIO":
                sql = "SELECT id_usuario FROM usuarios WHERE nombre = ?";
                break;
        }
        
        if (sql.isEmpty()) {
            return 0;
        }
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreElemento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }
    
    /**
     * Verifica si ya existe un detalle de promoción
     */
    private boolean existeDetallePromocion(int idPromocion, String tipoAplicacion, int idElemento, String nombreElemento, Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            String sql = "SELECT COUNT(*) FROM promociones_detalle WHERE id_promocion = ? AND tipo_aplicacion = ?";
            
            switch (tipoAplicacion) {
                case "PRODUCTO":
                    sql += " AND id_producto = ?";
                    break;
                case "CATEGORIA":
                    sql += " AND id_categoria = ?";
                    break;
                case "MARCA":
                    sql += " AND id_marca = ?";
                    break;
                case "ROL_USUARIO":
                    sql += " AND rol_usuario = ?";
                    break;
                case "USUARIO":
                    sql += " AND id_usuario = ?";
                    break;
            }
            
            ps = conn.prepareStatement(sql);
            ps.setInt(1, idPromocion);
            ps.setString(2, tipoAplicacion);
            
            if ("ROL_USUARIO".equals(tipoAplicacion)) {
                ps.setString(3, nombreElemento);
            } else {
                ps.setInt(3, idElemento);
            }
            
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }
    


    /**
     * Muestra un mensaje de error
     * @param mensaje El mensaje a mostrar
     */
    private void mostrarError(String mensaje) {
        javax.swing.JOptionPane.showMessageDialog(this, mensaje, "Error de Validación", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Carga usuarios disponibles para aplicar descuentos
     */
    private void cargarUsuariosDisponibles() {
        System.out.println("DEBUG: Iniciando cargarUsuariosDisponibles()");
        try (Connection conn = conexion.getInstance().createConnection()) {
            System.out.println("DEBUG: Obteniendo conexión a la base de datos");
            System.out.println("DEBUG: Conexión establecida exitosamente");
            
            String sql = "SELECT id_usuario, nombre FROM usuarios WHERE activo = 1 ORDER BY nombre";
            System.out.println("DEBUG: Ejecutando consulta SQL: " + sql);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                int contador = 0;
                while (rs.next()) {
                    Object[] fila = {
                        false, // Checkbox no seleccionado por defecto
                        rs.getString("nombre"),
                        "Sin límite", // Límite por usuario por defecto
                        "Sin límite"  // Límite total por defecto
                    };
                    modeloTabla.addRow(fila);
                    contador++;
                }
                System.out.println("DEBUG: Se cargaron " + contador + " usuarios desde la base de datos");
                
                // Si no hay usuarios en la BD, agregar datos de ejemplo
                if (modeloTabla.getRowCount() == 0) {
                    System.out.println("DEBUG: No hay usuarios en la BD, cargando datos de ejemplo");
                    agregarDatosEjemplo();
                }
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR SQL en cargarUsuariosDisponibles(): " + e.getMessage());
            e.printStackTrace();
            // En caso de error, cargar datos de ejemplo
            System.out.println("DEBUG: Cargando datos de ejemplo debido al error SQL");
            agregarDatosEjemplo();
        } catch (Exception e) {
            System.err.println("ERROR GENERAL en cargarUsuariosDisponibles(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga roles únicos disponibles para aplicar descuentos
     */
    private void cargarRolesDisponibles() {
        System.out.println("DEBUG: Iniciando cargarRolesDisponibles()");
        try (Connection conn = conexion.getInstance().createConnection()) {
            System.out.println("DEBUG: Obteniendo conexión a la base de datos");
            System.out.println("DEBUG: Conexión establecida exitosamente");
            
            String sql = "SELECT DISTINCT rol FROM usuarios WHERE activo = 1 ORDER BY rol";
            System.out.println("DEBUG: Ejecutando consulta SQL: " + sql);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                int contador = 0;
                while (rs.next()) {
                    Object[] fila = {
                        false, // Checkbox no seleccionado por defecto
                        rs.getString("rol"),
                        "Sin límite", // Límite por usuario por defecto
                        "Sin límite"  // Límite total por defecto
                    };
                    modeloTabla.addRow(fila);
                    contador++;
                }
                System.out.println("DEBUG: Se cargaron " + contador + " roles únicos desde la base de datos");
                
                // Si no hay roles en la BD, agregar datos de ejemplo
                if (modeloTabla.getRowCount() == 0) {
                    System.out.println("DEBUG: No hay roles en la BD, cargando datos de ejemplo");
                    agregarDatosEjemplo();
                }
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR SQL en cargarRolesDisponibles(): " + e.getMessage());
            e.printStackTrace();
            // En caso de error, cargar datos de ejemplo
            System.out.println("DEBUG: Cargando datos de ejemplo debido al error SQL");
            agregarDatosEjemplo();
        } catch (Exception e) {
            System.err.println("ERROR GENERAL en cargarRolesDisponibles(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga categorías disponibles para aplicar descuentos usando ServiceCategory
     */
    private void cargarCategoriasDisponibles() {
        System.out.println("DEBUG: Iniciando cargarCategoriasDisponibles()");
        try {
            ServiceCategory serviceCategory = new ServiceCategory();
            List<ModelCategory> categorias = serviceCategory.getAll();
            
            System.out.println("DEBUG: Se obtuvieron " + categorias.size() + " categorías desde ServiceCategory");
            
            for (ModelCategory categoria : categorias) {
                Object[] fila = {
                    false, // Checkbox no seleccionado por defecto
                    categoria.getName(),
                    "Sin límite", // Límite por usuario por defecto
                    "Sin límite"  // Límite total por defecto
                };
                modeloTabla.addRow(fila);
            }
            
            System.out.println("DEBUG: Se cargaron " + categorias.size() + " categorías en la tabla");
            
            // Si no hay categorías, agregar datos de ejemplo
            if (categorias.isEmpty()) {
                System.out.println("DEBUG: No hay categorías disponibles, cargando datos de ejemplo");
                agregarDatosEjemplo();
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR SQL en cargarCategoriasDisponibles(): " + e.getMessage());
            e.printStackTrace();
            // En caso de error, cargar datos de ejemplo
            System.out.println("DEBUG: Cargando datos de ejemplo debido al error SQL");
            agregarDatosEjemplo();
        } catch (Exception e) {
            System.err.println("ERROR GENERAL en cargarCategoriasDisponibles(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga marcas disponibles para aplicar descuentos usando ServiceBrand
     */
    private void cargarMarcasDisponibles() {
        System.out.println("DEBUG: Iniciando cargarMarcasDisponibles()");
        try {
            ServiceBrand serviceBrand = new ServiceBrand();
            List<ModelBrand> marcas = serviceBrand.getAll();
            
            System.out.println("DEBUG: Se obtuvieron " + marcas.size() + " marcas desde ServiceBrand");
            
            for (ModelBrand marca : marcas) {
                Object[] fila = {
                    false, // Checkbox no seleccionado por defecto
                    marca.getName(),
                    "Sin límite", // Límite por usuario por defecto
                    "Sin límite"  // Límite total por defecto
                };
                modeloTabla.addRow(fila);
            }
            
            System.out.println("DEBUG: Se cargaron " + marcas.size() + " marcas en la tabla");
            
            // Si no hay marcas, agregar datos de ejemplo
            if (marcas.isEmpty()) {
                System.out.println("DEBUG: No hay marcas disponibles, cargando datos de ejemplo");
                agregarDatosEjemplo();
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR SQL en cargarMarcasDisponibles(): " + e.getMessage());
            e.printStackTrace();
            // En caso de error, cargar datos de ejemplo
            System.out.println("DEBUG: Cargando datos de ejemplo debido al error SQL");
            agregarDatosEjemplo();
        } catch (Exception e) {
            System.err.println("ERROR GENERAL en cargarMarcasDisponibles(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene la imagen de la primera variante disponible de un producto
     * @param idProducto ID del producto
     * @return ImageIcon con la imagen redimensionada o null si no hay imagen
     */
    private javax.swing.ImageIcon obtenerImagenProducto(int idProducto) {
        String sql = "SELECT imagen FROM producto_variantes "
                + "WHERE id_producto = ? AND disponible = 1 AND imagen IS NOT NULL "
                + "ORDER BY id_variante LIMIT 1";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idProducto);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Blob blob = rs.getBlob("imagen");
                    if (blob != null) {
                        byte[] imageBytes = blob.getBytes(1, (int) blob.length());
                        
                        // Crear ImageIcon desde los bytes
                        javax.swing.ImageIcon originalIcon = new javax.swing.ImageIcon(imageBytes);
                        
                        // Redimensionar la imagen para que se ajuste a la tabla (50x50 píxeles)
                        java.awt.Image img = originalIcon.getImage();
                        java.awt.Image scaledImg = img.getScaledInstance(50, 50, java.awt.Image.SCALE_SMOOTH);
                        
                        return new javax.swing.ImageIcon(scaledImg);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener imagen del producto " + idProducto + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Retornar imagen por defecto si no se encuentra imagen
        return crearImagenPorDefecto();
    }
    
    /**
     * Crea una imagen por defecto cuando no hay imagen disponible
     * @return ImageIcon con imagen por defecto
     */
    private javax.swing.ImageIcon crearImagenPorDefecto() {
        // Crear una imagen simple de 50x50 píxeles con texto "Sin imagen"
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(50, 50, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = img.createGraphics();
        
        // Fondo gris claro
        g2d.setColor(java.awt.Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 50, 50);
        
        // Borde
        g2d.setColor(java.awt.Color.GRAY);
        g2d.drawRect(0, 0, 49, 49);
        
        // Texto "Sin imagen"
        g2d.setColor(java.awt.Color.DARK_GRAY);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 8));
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        String texto = "Sin imagen";
        int x = (50 - fm.stringWidth(texto)) / 2;
        int y = (50 + fm.getAscent()) / 2;
        g2d.drawString(texto, x, y);
        
        g2d.dispose();
        return new javax.swing.ImageIcon(img);
    }

    /**
     * Panel personalizado con esquinas redondeadas
     */
    private static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private Color backgroundColor;

        public RoundedPanel(int cornerRadius, Color backgroundColor) {
            this.cornerRadius = cornerRadius;
            this.backgroundColor = backgroundColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Crear forma redondeada
            RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            
            // Rellenar con color de fondo
            g2d.setColor(backgroundColor);
            g2d.fill(roundedRectangle);
            
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return super.getPreferredSize();
        }
    }

    /**
     * Cierra el diálogo
     */
    private void cerrarDialogo() {
        // Cerrar el diálogo
        javax.swing.SwingUtilities.getWindowAncestor(this).dispose();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JComboBox<String> cbxaplicapara;
    public javax.swing.JComboBox<String> cbxtipo;
    public javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel panel2;
    private javax.swing.JPanel panel3;
    public javax.swing.JTable table;
    public javax.swing.JFormattedTextField txtFechaIn;
    public javax.swing.JFormattedTextField txtFechafin;
    public javax.swing.JTextField txtcodigo;
    public javax.swing.JTextField txtdescripcion;
    public javax.swing.JTextField txtlimiteUsuario;
    public javax.swing.JTextField txtlimitetotal;
    public javax.swing.JTextField txtnombre;
    public javax.swing.JTextField txtvalor;
    // End of variables declaration//GEN-END:variables
}
