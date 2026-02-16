package raven.application.form.productos.creates;

import raven.application.form.principal.*;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import raven.modal.listener.ModalController;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelTalla;

/**
 *
 * @author CrisDEV
 */
public class CreateEtiqueta extends javax.swing.JPanel {
    
    private int idProducto;
    private int idColor;
    private String tipoVenta;
    private TallasFormCallback callback;
    private List<TallaVariante> tallasDisponibles;
    private ModalController modalController;    // Controlador del modal para cierre seguro
    
    // Clase para combinar ModelTalla con datos de variante
    public static class TallaVariante {
        private ModelTalla talla;
        private int idVariante;
        private String sku;
        private String ean;
        private BigDecimal precioVenta;
        private int stockPorPares;
        private boolean disponible;
        private int cantidadSeleccionada;
        private boolean seleccionada;
        
        // Constructor
        public TallaVariante(ModelTalla talla, int idVariante, String sku, String ean, BigDecimal precioVenta, 
                            int stockPorPares, boolean disponible) {
            this.talla = talla;
            this.idVariante = idVariante;
            this.sku = sku;
            this.ean = ean;
            this.precioVenta = precioVenta;
            this.stockPorPares = stockPorPares;
            this.disponible = disponible;
            this.cantidadSeleccionada = 0;
            this.seleccionada = false;
        }
        
        // Getters y setters para campos específicos de TallaVariante
        public ModelTalla getTalla() { return talla; }
        public int getIdVariante() { return idVariante; }
        public String getSku() { return sku; }
        public String getEan() { return ean; }
        public BigDecimal getPrecioVenta() { return precioVenta; }
        public int getStockPorPares() { return stockPorPares; }
        public boolean isDisponible() { return disponible; }
        public int getCantidadSeleccionada() { return cantidadSeleccionada; }
        public void setCantidadSeleccionada(int cantidad) { this.cantidadSeleccionada = cantidad; }
        public boolean isSeleccionada() { return seleccionada; }
        public void setSeleccionada(boolean seleccionada) { this.seleccionada = seleccionada; }
    }
    
    // Interface para callback
    public interface TallasFormCallback {
        void onTallasSeleccionadas(List<TallaVariante> tallasSeleccionadas, String tipoVenta);
    }

    public CreateEtiqueta() {
        initComponents();
        initializeForm();
    }
    
    public CreateEtiqueta(int idProducto, int idColor, String tipoVenta, TallasFormCallback callback) {
        this.idProducto = idProducto;
        this.idColor = idColor;
        this.tipoVenta = tipoVenta;
        this.callback = callback;
        this.tallasDisponibles = new ArrayList<>();
        
        initComponents();
        initializeForm();
        cargarTallasDisponibles();
    }

    // Permite inyectar el controlador del modal cuando se abre
    public void setModalController(ModalController mc) {
        this.modalController = mc;
    }
    
    private void initializeForm() {
        // Configurar estilos de botones con iconos y colores
        configurarBotones();
        
        // Configurar el modelo de la tabla
        DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
        
        // Configurar centrado de datos en la tabla
        configurarTabla();
        
        // Hacer que solo las columnas de selección y cantidad sean editables
        tablaTallas.setDefaultEditor(Object.class, null); // Deshabilitar edición por defecto
        
        // Configurar editor para la columna de selección (Boolean)
        tablaTallas.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
        // Configurar editor mejorado para la columna de cantidad
        configurarEditorCantidad();
        
        // Cargar las tallas disponibles
        cargarTallasDisponibles();
    }

    // Preseleccionar tallas y cantidades a partir de una selección previa
    // Permite que al abrir este formulario ya estén marcadas las filas y cantidades elegidas en CreateTallas
    public void preseleccionarTallas(java.util.List<raven.application.form.productos.creates.CreateTallas.TallaVariante> seleccion) {
        if (seleccion == null || seleccion.isEmpty()) {
            return;
        }
        // Asegurar que la tabla y el modelo existen
        DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
        for (int i = 0; i < tallasDisponibles.size(); i++) {
            CreateEtiqueta.TallaVariante tv = tallasDisponibles.get(i);
            for (raven.application.form.productos.creates.CreateTallas.TallaVariante sel : seleccion) {
                if (tv.getIdVariante() == sel.getIdVariante()) {
                    // Marcar selección y cantidad en el modelo de la tabla
                    model.setValueAt(Boolean.TRUE, i, 0);
                    model.setValueAt(sel.getCantidadSeleccionada(), i, 3);
                    tv.setSeleccionada(true);
                    tv.setCantidadSeleccionada(sel.getCantidadSeleccionada());
                }
            }
        }
    }
    
    private void configurarBotones() {
        // Configurar botón Agregar
        btnAgregar.addActionListener(e -> agregarTallasSeleccionadas());
        btnAgregar.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 16, Color.WHITE));
        btnAgregar.setBackground(new Color(34, 139, 34)); // Verde
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setBorderPainted(false);
        
        // Configurar botón Cancelar
        btnCancelar.setIcon(FontIcon.of(FontAwesomeSolid.TIMES, 16, Color.WHITE));
        btnCancelar.setBackground(new Color(220, 53, 69)); // Rojo
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBorderPainted(false);
    }
    
  private void configurarTabla() {
    // Crear renderer centrado básico para columnas de texto
    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    
    // Aplicar renderer centrado a TALLA y STOCK
    tablaTallas.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // TALLA
    tablaTallas.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // STOCK
    
    // ============================================================================
    // RENDERER PARA CANTIDAD: Solo muestra el valor, NO componentes interactivos
    // ============================================================================
    DefaultTableCellRenderer cantidadRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            // Llamar al método padre para obtener el componente base (JLabel)
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Centrar el texto
            setHorizontalAlignment(SwingConstants.CENTER);
            
            // Obtener la cantidad y el stock
            int cantidad = 0;
            int stock = 0;
            
            try {
                if (value != null) {
                    cantidad = Integer.parseInt(value.toString());
                }
                
                Object stockObj = table.getValueAt(row, 2); // Columna STOCK
                if (stockObj != null) {
                    stock = Integer.parseInt(stockObj.toString());
                }
            } catch (NumberFormatException e) {
                // Valores por defecto ya asignados
            }
            
            // Aplicar color rojo si la cantidad excede el stock
            if (cantidad > stock && cantidad > 0) {
                setForeground(Color.RED);
            } else if (isSelected) {
                setForeground(table.getSelectionForeground());
            } else {
                setForeground(table.getForeground());
            }
            
            // Configurar fondo según selección
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            
            return c;
        }
    };
    
    // Aplicar el renderer a la columna CANTIDAD
    tablaTallas.getColumnModel().getColumn(3).setCellRenderer(cantidadRenderer);
    
    // Configurar ancho de columnas
    tablaTallas.getColumnModel().getColumn(0).setPreferredWidth(100); // SELECCIONAR
    tablaTallas.getColumnModel().getColumn(1).setPreferredWidth(80);  // TALLA
    tablaTallas.getColumnModel().getColumn(2).setPreferredWidth(80);  // STOCK
    tablaTallas.getColumnModel().getColumn(3).setPreferredWidth(100); // CANTIDAD
    
    // Configurar altura de filas para acomodar el spinner cuando se edite
    tablaTallas.setRowHeight(30);
    
    // Permitir edición con terminación automática al perder foco
    tablaTallas.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
}
    
 private void configurarEditorCantidad() {
    // Crear el editor personalizado con spinner
    tablaTallas.getColumnModel().getColumn(3).setCellEditor(new SpinnerEditor());
}
 
  /**
 * Editor personalizado que muestra un JSpinner funcional
 * Solo se activa cuando el usuario hace clic en la celda
 */
private class SpinnerEditor extends DefaultCellEditor {
    private JSpinner spinner;
    private int filaActual = -1;
    
    public SpinnerEditor() {
        super(new JTextField());
        setClickCountToStart(1); // Activar con un solo clic
    }
    
    @Override
    public Component getTableCellEditorComponent(javax.swing.JTable table, Object value, 
            boolean isSelected, int row, int column) {
        
        filaActual = row;
        
        // Obtener el stock de la fila actual
        int stock = 0;
        try {
            Object stockObj = table.getValueAt(row, 2);
            if (stockObj != null) {
                stock = Integer.parseInt(stockObj.toString());
            }
        } catch (NumberFormatException e) {
            stock = Integer.MAX_VALUE; // Sin límite si hay error
        }
        
        // Crear modelo del spinner con límite de stock
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, stock, 1);
        spinner = new JSpinner(spinnerModel);
        
        // Establecer el valor actual
        int cantidad = 0;
        try {
            if (value != null) {
                cantidad = Integer.parseInt(value.toString());
            }
            spinner.setValue(Math.min(cantidad, stock)); // No exceder el stock
        } catch (NumberFormatException e) {
            spinner.setValue(0);
        }
        
        // Configurar el editor de texto del spinner
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        JTextField textField = editor.getTextField();
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Aplicar color según el valor vs stock
        final int stockFinal = stock;
        spinner.addChangeListener(e -> {
            int valorActual = (Integer) spinner.getValue();
            if (valorActual > stockFinal && valorActual > 0) {
                textField.setForeground(Color.RED);
            } else {
                textField.setForeground(table.getForeground());
            }
        });
        
        // Configurar tamaño del spinner
        spinner.setPreferredSize(new java.awt.Dimension(100, 25));
        
        // Personalizar los botones del spinner (flechitas)
        personalizarBotonesSpinner(spinner);
        
        // Listener para auto-seleccionar cuando cantidad > 0
        spinner.addChangeListener(e -> {
            SwingUtilities.invokeLater(() -> {
                int nuevaCantidad = (Integer) spinner.getValue();
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                
                // Auto-marcar selección si cantidad > 0
                if (nuevaCantidad > 0) {
                    model.setValueAt(true, filaActual, 0);
                } else {
                    model.setValueAt(false, filaActual, 0);
                }
            });
        });
        
        // Solicitar foco para activar inmediatamente
        SwingUtilities.invokeLater(() -> {
            spinner.requestFocusInWindow();
            textField.selectAll(); // Seleccionar texto para facilitar escritura
        });
        
        return spinner;
    }
    
    /**
     * Personaliza los botones (flechitas) del spinner para mejor visibilidad
     */
    private void personalizarBotonesSpinner(JSpinner spinner) {
        for (Component comp : spinner.getComponents()) {
            if (comp instanceof javax.swing.JButton) {
                javax.swing.JButton boton = (javax.swing.JButton) comp;
                boton.setBackground(new Color(70, 130, 180)); // Azul acero
                boton.setForeground(Color.WHITE);
                boton.setBorder(javax.swing.BorderFactory.createLineBorder(
                    new Color(50, 100, 150), 1));
                boton.setFocusPainted(false);
                boton.setOpaque(true);
                boton.setFocusable(false);
            }
        }
    }
    
    @Override
    public Object getCellEditorValue() {
        if (spinner != null) {
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                // Usar valor actual en caso de error
            }
            return spinner.getValue();
        }
        return 0;
    }
    
    @Override
    public boolean stopCellEditing() {
        if (spinner != null) {
            try {
                spinner.commitEdit();
                
                // Actualizar el modelo con el valor final
                int valor = (Integer) spinner.getValue();
                DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
                if (filaActual >= 0) {
                    model.setValueAt(valor, filaActual, 3);
                }
                
            } catch (java.text.ParseException e) {
                // Mantener valor actual
            }
        }
        
        // Forzar repintado para actualizar el renderer
        SwingUtilities.invokeLater(() -> {
            tablaTallas.repaint();
        });
        
        return super.stopCellEditing();
    }
    
    @Override
    public void cancelCellEditing() {
        super.cancelCellEditing();
        SwingUtilities.invokeLater(() -> {
            tablaTallas.repaint();
        });
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

        datePicker = new raven.datetime.component.date.DatePicker();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaTallas = new javax.swing.JTable();
        btnAgregar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        jTextField1.setText("jTextField1");

        // Modelo inicial con las 4 columnas esperadas por configurarTabla y la lógica de negocio
        tablaTallas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
            },
            new String [] {
                "SELECCIONAR", "TALLA", "STOCK", "CANTIDAD"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaTallas.setFocusable(false);
        jScrollPane1.setViewportView(tablaTallas);

        btnAgregar.setText("Aceptar");

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 479, Short.MAX_VALUE)
                        .addComponent(btnCancelar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnAgregar)
                        .addGap(6, 6, 6))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancelar)
                    .addComponent(btnAgregar))
                .addGap(0, 40, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        // Cerrar el modal de forma segura usando su controlador si está disponible
        if (modalController != null) {
            modalController.close();
        } else {
            // Fallback: cerrar ventana contenedora en caso de uso fuera de ModalContainer
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        }
    }//GEN-LAST:event_btnCancelarActionPerformed
    
    /**
     * Carga las tallas disponibles para el producto y color seleccionados
     */
    private void cargarTallasDisponibles() {
        
        // Determinar qué campo de stock usar según el tipo de venta (desde inventario_bodega)
        String campoStock = "caja".equals(tipoVenta) ? "COALESCE(ib.Stock_caja,0)" : "COALESCE(ib.Stock_par,0)";
        
        String sql = "SELECT DISTINCT " +
                    "pv.id_talla, " +
                    "pv.id_variante, " +
                    "pv.sku, " +
                    "pv.ean, " +
                    "pv.precio_venta, " +
                    "COALESCE(ib.Stock_par,0) AS stock_por_pares, " +
                    "COALESCE(ib.Stock_caja,0) AS stock_por_cajas, " +
                    "pv.disponible, " +
                    "t.numero, " +
                    "t.sistema, " +
                    "t.genero, " +
                    "t.activo, " +
                    "CONCAT(t.numero, ' ', t.sistema) as talla_display " +
                    "FROM producto_variantes pv " +
                    "JOIN tallas t ON pv.id_talla = t.id_talla " +
                    "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 " +
                    "WHERE pv.id_producto = ? " +
                    "  AND pv.id_color = ? " +
                    "  AND t.numero <> '00' " +
                    "  AND " + campoStock + " > 0 " +
                    "  AND pv.disponible = 1 " +
                    "ORDER BY " +
                    "    pv.disponible DESC, " +
                    "    " + campoStock + " DESC, " +
                    "    CAST(t.numero AS UNSIGNED) ASC";
        
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            pst.setInt(1, idProducto);
            pst.setInt(2, idColor);
            
            
            try (ResultSet rs = pst.executeQuery()) {
                tallasDisponibles.clear();
                DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
                model.setRowCount(0); // Limpiar tabla
                
                int contador = 0;
                while (rs.next()) {
                    contador++;
                    
                    // Obtener el stock según el tipo de venta
                    int stockSegunTipo = "caja".equals(tipoVenta) ? 
                        rs.getInt("stock_por_cajas") : rs.getInt("stock_por_pares");
                    
                    // Crear ModelTalla usando los campos correctos de la tabla tallas
                    ModelTalla modelTalla = new ModelTalla(
                        rs.getInt("id_talla"),
                        rs.getString("numero"),  // usar numero como nombre
                        rs.getString("genero"),  // usar genero como categoria
                        rs.getBoolean("activo")
                    );
                    modelTalla.setDescripcion(rs.getString("sistema")); // usar sistema como descripcion
                    
                    // Crear TallaVariante usando ModelTalla
                    TallaVariante talla = new TallaVariante(
                        modelTalla,
                        rs.getInt("id_variante"),
                        rs.getString("sku"),
                        rs.getString("ean"),
                        rs.getBigDecimal("precio_venta"),
                        stockSegunTipo, // Usar el stock según el tipo
                        rs.getBoolean("disponible")
                    );
                    
                    System.out.println("   Talla " + contador + ": " + talla.getTalla().getNombre() + 
                                     " (Stock " + tipoVenta + ": " + stockSegunTipo + 
                                     ", Disponible: " + talla.isDisponible() + ")");
                    
                    tallasDisponibles.add(talla);
                    
                    // Agregar fila a la tabla
                    model.addRow(new Object[]{
                        false, // SELECCIONAR
                        talla.getTalla().getNombre(), // TALLA
                        stockSegunTipo, // STOCK según tipo
                        0 // CANTIDAD (inicialmente 0)
                    });
                }
                
                
                if (tallasDisponibles.isEmpty()) {
                    System.out.println("ERROR  No se encontraron tallas para idProducto=" + idProducto + ", idColor=" + idColor);
                    JOptionPane.showMessageDialog(this, 
                        "No hay tallas disponibles para este producto y color.", 
                        "Sin tallas", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("ERROR  Error SQL en cargarTallasDisponibles: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error al cargar las tallas: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Procesa las tallas seleccionadas y las envía al formulario padre
     */
    private void agregarTallasSeleccionadas() {
        DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
        List<TallaVariante> tallasSeleccionadas = new ArrayList<>();
        
        // Validar selecciones
        boolean haySeleccion = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean seleccionada = (Boolean) model.getValueAt(i, 0);
            if (seleccionada != null && seleccionada) {
                haySeleccion = true;
                
                // Obtener cantidad
                Object cantidadObj = model.getValueAt(i, 3);
                int cantidad = 0;
                if (cantidadObj != null) {
                    try {
                        cantidad = Integer.parseInt(cantidadObj.toString());
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, 
                            "La cantidad en la fila " + (i + 1) + " debe ser un número válido.", 
                            "Error de validación", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                if (cantidad <= 0) {
                    JOptionPane.showMessageDialog(this, 
                        "La cantidad en la fila " + (i + 1) + " debe ser mayor que cero.", 
                        "Error de validación", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Validar stock
                TallaVariante talla = tallasDisponibles.get(i);
                if (cantidad > talla.getStockPorPares()) {
                    JOptionPane.showMessageDialog(this, 
                        "La cantidad solicitada (" + cantidad + ") excede el stock disponible (" + 
                        talla.getStockPorPares() + ") para la talla " + talla.getTalla().getNombre() + ".", 
                        "Stock insuficiente", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Configurar talla seleccionada
                talla.setSeleccionada(true);
                talla.setCantidadSeleccionada(cantidad);
                tallasSeleccionadas.add(talla);
            }
        }
        
        if (!haySeleccion) {
            JOptionPane.showMessageDialog(this, 
                "Debe seleccionar al menos una talla.", 
                "Sin selección", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Enviar datos al formulario padre
        if (callback != null) {
            callback.onTallasSeleccionadas(tallasSeleccionadas, tipoVenta);
        }
        
        // Cerrar el modal de forma segura usando su controlador si está disponible
        if (modalController != null) {
            modalController.close();
        } else {
            // Fallback: cerrar ventana contenedora en caso de uso fuera de ModalContainer
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        }
    }

 

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnCancelar;
    private raven.datetime.component.date.DatePicker datePicker;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTable tablaTallas;
    // End of variables declaration//GEN-END:variables
}

