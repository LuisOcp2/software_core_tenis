package raven.application.form.productos.creates;

import raven.application.form.principal.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFileChooser;
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
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelTalla;
import raven.controlador.productos.ModelProductVariant;
import raven.clases.productos.ServiceProductVariant;
import raven.clases.admin.UserSession;
import raven.modal.Toast;

/**
 *
 * @author CrisDEV
 */
public class CreateTallas extends javax.swing.JPanel {

    private int idProducto;
    private int idColor;
    private String tipoVenta;
    private TallasFormCallback callback;
    private List<TallaVariante> tallasDisponibles;
    private Integer idVarianteCajaSeleccionada; // Variante de caja detectada
    private String nombreColorSeleccionado;     // Nombre del color de la caja
    private String generoProducto;              // Género del producto para filtrar tallas
    private byte[] imagenParaNuevasVariantes;   // Imagen seleccionada para aplicar a nuevas variantes
    private String codigoModeloProducto;        // Código modelo del producto (para cabecera)
    private ModalController modalController;    // Controlador del modal para cierre seguro
    private boolean descontarCajaAlAceptar = false; // Modo conversión que descuenta una caja
    private int maxCajasConvertibles = 1;
    private int cajasAConvertir = 1;
    private Integer idProveedorForzado;
    private javax.swing.Timer uiDebounceTimer;
    private boolean conversionEnCurso;
    
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
        private boolean nuevaVariante;
        private String ubicacionEspecifica;
        
        // Constructor
        public TallaVariante(ModelTalla talla, int idVariante, String sku, String ean, BigDecimal precioVenta, 
                            int stockPorPares, boolean disponible, boolean nuevaVariante, String ubicacionEspecifica) {
            this.talla = talla;
            this.idVariante = idVariante;
            this.sku = sku;
            this.ean = ean;
            this.precioVenta = precioVenta;
            this.stockPorPares = stockPorPares;
            this.disponible = disponible;
            this.cantidadSeleccionada = 0;
            this.seleccionada = false;
            this.nuevaVariante = nuevaVariante;
            this.ubicacionEspecifica = ubicacionEspecifica;
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
        public boolean isNuevaVariante() { return nuevaVariante; }
        public void setIdVariante(int idVariante) { this.idVariante = idVariante; }
        public String getUbicacionEspecifica() { return ubicacionEspecifica; }
        public void setUbicacionEspecifica(String ubicacionEspecifica) { this.ubicacionEspecifica = ubicacionEspecifica; }
    }
    
    // Interface para callback
    public interface TallasFormCallback {
        void onTallasSeleccionadas(List<TallaVariante> tallasSeleccionadas, String tipoVenta, Integer idVarianteCaja, byte[] imagenComun, int cajasAConvertir);
    }

    public CreateTallas() {
        initComponents();
        initializeForm();
    }
    
    public CreateTallas(int idProducto, int idColor, String tipoVenta, TallasFormCallback callback) {
        this.idProducto = idProducto;
        this.idColor = idColor;
        this.tipoVenta = tipoVenta;
        this.callback = callback;
        this.tallasDisponibles = new ArrayList<>();
        
        initComponents();
        initializeForm();
        // Cargar tallas solo si ya tenemos color seleccionado
        if (this.idColor > 0) {
            cargarTallasDisponibles();
        }
    }

    // Permite inyectar el controlador del modal cuando se abre
    public void setModalController(ModalController mc) {
        this.modalController = mc;
    }

    // Configura si al aceptar se debe descontar 1 caja (modo conversión desde inventario)
    public void setDescontarCajaAlAceptar(boolean v) { this.descontarCajaAlAceptar = v; }
    public void setIdProveedorForzado(Integer id) { this.idProveedorForzado = id; }
    public void setImagenParaNuevasVariantes(byte[] imagen) { this.imagenParaNuevasVariantes = imagen; }
    
    private void initializeForm() {
        // Configurar el modelo de la tabla
        DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();

        // Configurar centrado de datos en la tabla
        configurarTabla();

        // Configurar estilos profesionales de botones
        setupButtonStyles();

        // Hacer que solo las columnas de selección y cantidad sean editables
        tablaTallas.setDefaultEditor(Object.class, null); // Deshabilitar edición por defecto
        
        // Configurar editor para la columna de selección (Boolean)
        tablaTallas.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
        // Configurar editor mejorado para la columna de cantidad
        configurarEditorCantidad();

        initDebounceTimer();

        // Acción del botón de imagen: cargar imagen desde la CAJA seleccionada (con fallback a selección manual)
        btnImagen.addActionListener(e -> mostrarOpcionesImagen());

        // CONFIGURACIÓN DEL SPINNER DE CAJAS - Actualizar cajasAConvertir cuando cambie
        spCajas.addChangeListener(e -> {
            Object valor = spCajas.getValue();
            if (valor instanceof Integer) {
                cajasAConvertir = (Integer) valor;
                System.out.println("INFO Cajas a convertir actualizado: " + cajasAConvertir);

                // Actualizar el total requerido (24 pares por caja)
                if ("caja".equalsIgnoreCase(tipoVenta)) {
                    actualizarTotalRestante();
                    actualizarBotonConvertir();
                    actualizarCabeceraDetalles();
                }
            }
        });

        // ===== CONFIGURACIÓN DE BOTÓN AJUSTES Y COMBOBOX ZUELA =====
        // Conectar el botón de ajustes con el diálogo de configuraciones
        if (btnAjustes != null) {
            btnAjustes.addActionListener(e -> abrirDialogoConfiguraciones());
            System.out.println("SUCCESS Botón Ajustes conectado");
        }

        // Cargar configuraciones en el ComboBox al iniciar
        if (cbxZuela != null) {
            cargarConfiguracionesEnComboBox();
        }

        // Cargar y mostrar el código modelo del producto en la cabecera
        try {
            codigoModeloProducto = obtenerCodigoModeloProducto(idProducto);
        } catch (Exception ignore) {}
        actualizarCabeceraDetalles();

        // Cargar las tallas disponibles basadas en el género del producto seleccionado
        if (idProducto > 0) {
            cargarTallasDisponibles();
        }
    }

    private void initDebounceTimer() {
        uiDebounceTimer = new javax.swing.Timer(75, e -> {
            actualizarTotalRestante();
            actualizarBotonConvertir();
        });
        uiDebounceTimer.setRepeats(false);
    }

    private void scheduleRecalc() {
        if (uiDebounceTimer == null) return;
        if (uiDebounceTimer.isRunning()) uiDebounceTimer.restart(); else uiDebounceTimer.start();
    }
     
  
    
  private void configurarTabla() {
    // Crear renderer centrado básico para columnas de texto
    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    
    // Aplicar renderer centrado a TALLA
    tablaTallas.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // TALLA
    
    javax.swing.table.TableCellRenderer cantidadRenderer = new javax.swing.table.TableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int cantidad = 0;
            try { if (value != null) cantidad = Integer.parseInt(String.valueOf(value)); } catch (Exception ignore) {}

            int stock;
            if ("caja".equalsIgnoreCase(tipoVenta)) {
                // USAR TOTAL REQUERIDO (24 × CAJAS) EN LUGAR DE SOLO 24
                int totalRequerido = 24 * cajasAConvertir;
                int sumaExcepto = calcularSumaCantidadesExcepto(row);
                int restante = Math.max(0, totalRequerido - sumaExcepto);
                stock = Math.max(0, restante);
            } else {
                stock = (row >= 0 && row < tallasDisponibles.size()) ? Math.max(0, tallasDisponibles.get(row).getStockPorPares()) : Integer.MAX_VALUE;
            }

            // NO LIMITAR LA CANTIDAD EN EL RENDERER - Solo mostrarla
            // El límite se maneja en el editor
            // if (cantidad > stock) cantidad = stock;  <- ELIMINADO

            SpinnerNumberModel model = new SpinnerNumberModel(cantidad, 0, Math.max(stock, cantidad), 1);
            JSpinner spinner = new JSpinner(model);
            JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) spinner.getEditor();
            JTextField tf = ed.getTextField();
            tf.setHorizontalAlignment(SwingConstants.CENTER);
            spinner.setPreferredSize(new java.awt.Dimension(120, 28));

            for (Component comp : spinner.getComponents()) {
                if (comp instanceof javax.swing.JButton) {
                    javax.swing.JButton b = (javax.swing.JButton) comp;
                    b.setBackground(new Color(70, 130, 180));
                    b.setForeground(Color.WHITE);
                    b.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(50, 100, 150), 1));
                    b.setFocusPainted(false);
                    b.setOpaque(true);
                    b.setFocusable(false);
                }
            }

            if (isSelected) {
                spinner.setBackground(table.getSelectionBackground());
            } else {
                spinner.setBackground(table.getBackground());
            }
            spinner.setEnabled(false);
            return spinner;
        }
    };
    
    // Aplicar el renderer a la columna CANTIDAD
    tablaTallas.getColumnModel().getColumn(2).setCellRenderer(cantidadRenderer);
    
    // Configurar ancho de columnas
    tablaTallas.getColumnModel().getColumn(0).setPreferredWidth(110);
    tablaTallas.getColumnModel().getColumn(1).setPreferredWidth(100);
    tablaTallas.getColumnModel().getColumn(2).setPreferredWidth(140);
    
    // Configurar altura de filas para acomodar el spinner cuando se edite
    tablaTallas.setRowHeight(42);
    tablaTallas.setIntercellSpacing(new java.awt.Dimension(0, 10));
    tablaTallas.setFont(tablaTallas.getFont().deriveFont(14f));
    tablaTallas.getTableHeader().setFont(tablaTallas.getTableHeader().getFont().deriveFont(14f));
    
    // Permitir edición con terminación automática al perder foco
    tablaTallas.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
}

    /**
     * Configura los estilos profesionales de los botones con diseño moderno
     */
    private void setupButtonStyles() {
        // Configurar tipos de botón con bordes redondeados
        btnImagen.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnConvertir.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnAjustes.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);

        // DISEÑO PROFESIONAL CON COLORES MODERNOS Y ARCO SUAVE
        // Botón Imagen - Azul brillante con borde suave
        btnImagen.putClientProperty(FlatClientProperties.STYLE,
            "arc:24;background:#0A84FF;foreground:#FFFFFF;font:bold 13;borderWidth:0");

        // Botón Convertir - Verde esmeralda (acción principal)
        btnConvertir.putClientProperty(FlatClientProperties.STYLE,
            "arc:24;background:#34C759;foreground:#FFFFFF;font:bold 14;borderWidth:0");

        // Botón Ajustes - Naranja suave
        btnAjustes.putClientProperty(FlatClientProperties.STYLE,
            "arc:24;background:#FF9F0A;foreground:#FFFFFF;font:bold 13;borderWidth:0");

        // Iconos profesionales con FontAwesome
        try {
            // Icono de imagen/cámara - Intenta cargar SVG, fallback a texto
            try {
                FlatSVGIcon iconImagen = new FlatSVGIcon("raven/icon/icons/imagen.svg", 20, 20);
                iconImagen.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
                btnImagen.setIcon(iconImagen);
                btnImagen.setText(" Imagen");
            } catch (Exception e) {
                // Fallback: usar FontIcon de IkonLi
                FontIcon iconImagen = FontIcon.of(FontAwesomeSolid.IMAGE, 18, Color.WHITE);
                btnImagen.setIcon(iconImagen);
                btnImagen.setText(" Imagen");
            }

            // Icono de convertir/transferir
            try {
                FlatSVGIcon iconConvertir = new FlatSVGIcon("raven/icon/svg/caja.svg", 20, 20);
                iconConvertir.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
                btnConvertir.setIcon(iconConvertir);
                btnConvertir.setText(" Convertir");
            } catch (Exception e) {
                // Fallback: usar FontIcon
                FontIcon iconConvertir = FontIcon.of(FontAwesomeSolid.EXCHANGE_ALT, 18, Color.WHITE);
                btnConvertir.setIcon(iconConvertir);
                btnConvertir.setText(" Convertir");
            }

            // Icono de ajustes/configuración
            try {
                FlatSVGIcon iconAjustes = new FlatSVGIcon("raven/icon/icons/ajustes.svg", 20, 20);
                iconAjustes.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
                btnAjustes.setIcon(iconAjustes);
                btnAjustes.setText(" Ajustes");
            } catch (Exception e) {
                // Fallback: usar FontIcon
                FontIcon iconAjustes = FontIcon.of(FontAwesomeSolid.COG, 18, Color.WHITE);
                btnAjustes.setIcon(iconAjustes);
                btnAjustes.setText(" Ajustes");
            }

            // Espaciado entre icono y texto
            btnImagen.setIconTextGap(8);
            btnConvertir.setIconTextGap(10);
            btnAjustes.setIconTextGap(8);

        } catch (Exception e) {
            System.err.println("WARNING No se pudieron cargar los iconos: " + e.getMessage());
            btnImagen.setText(" Imagen");
            btnConvertir.setText(" Convertir");
            btnAjustes.setText(" Ajustes");
        }

        // Márgenes para mejor apariencia y área de clic
        Insets buttonMargin = new Insets(10, 16, 10, 16);
        btnImagen.setMargin(buttonMargin);
        btnConvertir.setMargin(buttonMargin);
        btnAjustes.setMargin(buttonMargin);

        // Tamaño preferido para consistencia
        btnImagen.setPreferredSize(new java.awt.Dimension(130, 42));
        btnConvertir.setPreferredSize(new java.awt.Dimension(140, 42));
        btnAjustes.setPreferredSize(new java.awt.Dimension(130, 42));

        // Tooltips descriptivos para mejor UX
        btnImagen.setToolTipText("Seleccionar imagen usando el gestor de archivos (con vista previa)");
        btnConvertir.setToolTipText("Convertir las cajas seleccionadas a pares individuales");
        btnAjustes.setToolTipText("Configurar opciones avanzadas de conversión");

        // Cursor pointer para indicar que son clicables
        btnImagen.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnConvertir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAjustes.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void configurarEditorCantidad() {
        // Crear el editor personalizado con spinner
        tablaTallas.getColumnModel().getColumn(2).setCellEditor(new SpinnerEditor());
        // Recalcular con debounce cuando cambie el modelo
        ((DefaultTableModel) tablaTallas.getModel()).addTableModelListener(e -> {
            if ("caja".equalsIgnoreCase(tipoVenta)) {
                scheduleRecalc();
            }
        });
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
        
        // Obtener el límite del spinner según el tipo de venta
        int stock = 0;
        try {
            if ("caja".equalsIgnoreCase(tipoVenta)) {
                //  MULTIPLICAR POR NÚMERO DE CAJAS: 1 caja = 24, 3 cajas = 72, etc.
                stock = 24 * cajasAConvertir;
            } else {
                // Usar el stock disponible de la lista de tallas
                if (row >= 0 && row < tallasDisponibles.size()) {
                    stock = Math.max(0, tallasDisponibles.get(row).getStockPorPares());
                } else {
                    stock = Integer.MAX_VALUE; // fallback sin límite
                }
            }
        } catch (NumberFormatException e) {
            stock = Integer.MAX_VALUE; // Sin límite si hay error
        }

        // Calcular máximo permitido respetando el total (24 × cajas) en modo caja
        int maxPermitido;
        if ("caja".equalsIgnoreCase(tipoVenta)) {
            int sumaExcepto = calcularSumaCantidadesExcepto(row);
            int totalRequerido = 24 * cajasAConvertir;
            maxPermitido = Math.max(0, totalRequerido - sumaExcepto);
        } else {
            maxPermitido = stock;
        }

        // Crear modelo del spinner con límite calculado
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, Math.min(stock, maxPermitido), 1);
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
            int nuevaCantidad = (Integer) spinner.getValue();
            DefaultTableModel model = (DefaultTableModel) table.getModel();

            // En modo caja, actualizar dinámicamente el máximo sin forzar setValue
            if ("caja".equalsIgnoreCase(tipoVenta)) {
                int sumaExcepto = calcularSumaCantidadesExcepto(filaActual);
                int totalRequerido = 24 * cajasAConvertir;
                int restante = Math.max(0, totalRequerido - sumaExcepto);
                Integer nuevoMax = Integer.valueOf(Math.min(stockFinal, restante));
                ((SpinnerNumberModel) spinner.getModel()).setMaximum(nuevoMax);
                // Clamp si por alguna razón superó el máximo
                if (nuevaCantidad > nuevoMax) {
                    nuevaCantidad = nuevoMax;
                    spinner.getModel().setValue(nuevoMax);
                }
            }

            // Auto-marcar selección si cantidad > 0
            model.setValueAt(nuevaCantidad > 0, filaActual, 0);
            // Actualizar valor en el modelo para reflejar el cambio
            model.setValueAt(nuevaCantidad, filaActual, 2);
            // Debounce de recálculo de UI
            scheduleRecalc();
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
                    model.setValueAt(valor, filaActual, 2);
                }
                
            } catch (java.text.ParseException e) {
                // Mantener valor actual
            }
        }
        
        // Forzar repintado y re-cálculo con debounce
        tablaTallas.repaint();
        if ("caja".equalsIgnoreCase(tipoVenta)) {
            scheduleRecalc();
        }
        
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
        lbdetalles = new javax.swing.JLabel();
        btnImagen = new javax.swing.JButton();
        lbTotal = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        TxtUbuEspecifica = new javax.swing.JTextField();
        LbUbicacionEspecifica = new javax.swing.JLabel();
        btnConvertir = new javax.swing.JButton();
        spCajas = new javax.swing.JSpinner();
        btnAjustes = new javax.swing.JButton();
        cbxZuela = new javax.swing.JComboBox<>();

        jTextField1.setText("jTextField1");

        tablaTallas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "SELECIONAR", "TALLA", "PROVEEDOR", "CANTIDAD"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tablaTallas.setFocusable(false);
        jScrollPane1.setViewportView(tablaTallas);

        lbdetalles.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lbdetalles.setText("Entrada producto por caja");

        btnImagen.setText("Imagen");

        lbTotal.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbTotal.setText("24");

        jLabel3.setText("Total:");

        LbUbicacionEspecifica.setText("Estanteria:");

        btnConvertir.setText("Convertir");

        btnAjustes.setText("Ajustes");

        cbxZuela.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Zuela" }));
        cbxZuela.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbxZuelaItemStateChanged(evt);
            }
        });
        cbxZuela.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxZuelaActionPerformed(evt);
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
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 652, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(lbdetalles)
                        .addGap(237, 237, 237))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(lbTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAjustes, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnConvertir, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(16, 16, 16))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(LbUbicacionEspecifica, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TxtUbuEspecifica, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cbxZuela, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spCajas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lbdetalles)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(TxtUbuEspecifica, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                        .addComponent(btnImagen, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                        .addComponent(spCajas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cbxZuela))
                    .addComponent(LbUbicacionEspecifica, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbTotal)
                        .addComponent(jLabel3))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnConvertir)
                        .addComponent(btnAjustes)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cbxZuelaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxZuelaItemStateChanged
        // Solo procesar eventos de SELECCIÓN
        if (evt.getStateChange() != java.awt.event.ItemEvent.SELECTED) {
            return;
        }

        Object selected = cbxZuela.getSelectedItem();
        if (selected != null && !selected.toString().equals("Seleccionar configuración")) {
            // Aplicar la configuración seleccionada
            aplicarConfiguracion(selected.toString());
        }
    }//GEN-LAST:event_cbxZuelaItemStateChanged

    private void cbxZuelaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxZuelaActionPerformed
        // Evento alternativo para aplicar configuración
        Object selected = cbxZuela.getSelectedItem();
        if (selected != null && !selected.toString().equals("Seleccionar configuración")) {
            System.out.println("INFO Configuración seleccionada: " + selected.toString());
        }
    }//GEN-LAST:event_cbxZuelaActionPerformed
    
    /**
     * Carga las tallas disponibles para el producto y color seleccionados
     */
    private void cargarTallasDisponibles() {

        // Determinar qué campo de stock usar según el tipo de venta
        // IMPORTANTE: al usar DISTINCT, los campos del ORDER BY deben estar en el SELECT.
        // Ya seleccionamos los COALESCE(...) con alias 'stock_por_cajas' y 'stock_por_pares',
        // por lo tanto el ORDER BY debe usar esos alias y no 'pv.*'.
        String campoStock = "caja".equalsIgnoreCase(tipoVenta) ? "stock_por_cajas" : "stock_por_pares";
        String generoFiltro = obtenerGeneroProducto(idProducto);

        // DEBUG: Imprimir género obtenido
        System.out.println("DEBUG CreateTallas - Género del producto: '" + generoFiltro + "'");

        java.util.List<String> generosPermitidos = new java.util.ArrayList<>();
        if (generoFiltro != null && !generoFiltro.trim().isEmpty()) {
            String g = normalizarGenero(generoFiltro);
            System.out.println("DEBUG CreateTallas - Género normalizado: '" + g + "'");

            switch (g) {
                case "UNISEX":
                    generosPermitidos.add("MUJER");
                    generosPermitidos.add("HOMBRE");
                    generosPermitidos.add("UNISEX");
                    break;
                case "HOMBRE":
                    generosPermitidos.add("HOMBRE");
                    generosPermitidos.add("UNISEX");
                    break;
                case "MUJER":
                    generosPermitidos.add("MUJER");
                    generosPermitidos.add("UNISEX");
                    break;
                case "NIÑO":
                    generosPermitidos.add("NIÑO");
                    generosPermitidos.add("UNISEX");  // Incluir tallas unisex para niños
                    break;
                default:
                    System.out.println("WARNING DEBUG CreateTallas - Género NO reconocido, usando: '" + g + "'");
                    generosPermitidos.add(g);
            }
        } else {
            System.out.println("WARNING DEBUG CreateTallas - Género vacío o nulo");
        }

        System.out.println("DEBUG CreateTallas - Géneros permitidos: " + generosPermitidos);
        
        // Obtener bodega del usuario para evitar duplicados
        Integer idBodegaUsuario = UserSession.getInstance().getIdBodegaUsuario();

        String sql = "SELECT " +
                    "t.id_talla, " +
                    "pv.id_variante, " +
                    "pv.sku, " +
                    "pv.ean, " +
                    "pv.precio_venta, " +
                    "COALESCE(ib.Stock_par, 0) AS stock_por_pares, " +
                    "COALESCE(ib.Stock_caja, 0) AS stock_por_cajas, " +
                    "COALESCE(pv.disponible, 1) AS disponible, " +
                    "t.numero, " +
                    "t.sistema, " +
                    "t.genero, " +
                    "t.activo, " +
                    "ib.ubicacion_especifica " +
                    "FROM tallas t " +
                    "LEFT JOIN producto_variantes pv ON pv.id_talla = t.id_talla AND pv.id_producto = ? " +
                    (idColor > 0 ? " AND pv.id_color = ? " : " ") +
                    "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                    "  AND ib.activo = 1 " +
                    (idBodegaUsuario != null ? " AND ib.id_bodega = ? " : "") +
                    "WHERE t.activo = 1 " +
                    "  AND TRIM(t.numero) <> '00' " +
                    (generosPermitidos.isEmpty() ? "" : "  AND t.genero IN (" + generosPermitidos.stream().map(s -> "?").collect(java.util.stream.Collectors.joining(", ")) + ") ") +
                    "GROUP BY t.id_talla, pv.id_variante, pv.sku, pv.ean, pv.precio_venta, " +
                    "  pv.disponible, t.numero, t.sistema, t.genero, t.activo, ib.Stock_par, ib.Stock_caja, ib.ubicacion_especifica " +
                    "ORDER BY " +
                    "    COALESCE(pv.disponible, 1) DESC, " +
                    "    " + campoStock + " DESC, " +
                    "    CAST(t.numero AS UNSIGNED) ASC";
        
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            int paramIndex = 1;
            pst.setInt(paramIndex++, idProducto);
            if (idColor > 0) {
                pst.setInt(paramIndex++, idColor);
            }
            if (idBodegaUsuario != null) {
                pst.setInt(paramIndex++, idBodegaUsuario);
            }
            for (String gen : generosPermitidos) {
                pst.setString(paramIndex++, gen);
            }
            
            
            System.out.println("DEBUG CreateTallas - Ejecutando consulta SQL...");

            try (ResultSet rs = pst.executeQuery()) {
                tallasDisponibles.clear();
                DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
                model.setRowCount(0); // Limpiar tabla

                int contador = 0;
                while (rs.next()) {
                    contador++;

                    // DEBUG: Imprimir género de cada talla encontrada
                    String generoTalla = rs.getString("genero");
                    System.out.println("DEBUG - Talla encontrada: Número=" + rs.getString("numero") +
                                     ", Género='" + generoTalla + "', ID=" + rs.getInt("id_talla"));

                    // Obtener el stock según el tipo de venta
                    int stockSegunTipo = "caja".equalsIgnoreCase(tipoVenta) ?
                        rs.getInt("stock_por_cajas") : rs.getInt("stock_por_pares");

                    // Crear ModelTalla usando los campos correctos de la tabla tallas
                    ModelTalla modelTalla = new ModelTalla(
                        rs.getInt("id_talla"),
                        rs.getString("numero"),  // usar numero como nombre
                        generoTalla,  // usar genero como categoria
                        rs.getBoolean("activo")
                    );
                    modelTalla.setDescripcion(rs.getString("sistema")); // usar sistema como descripcion

                    // Determinar si la talla aún no tiene variante
                    boolean esNueva = (rs.getObject("id_variante") == null);

                    String ubicacion = rs.getString("ubicacion_especifica");

                    if (TxtUbuEspecifica != null) {
                        String actual = null;
                        try {
                            actual = TxtUbuEspecifica.getText();
                        } catch (Exception ignore) {}
                        if ((actual == null || actual.trim().isEmpty()) && ubicacion != null && !ubicacion.trim().isEmpty()) {
                            TxtUbuEspecifica.setText(ubicacion);
                        }
                    }

                    // Crear TallaVariante usando ModelTalla
                    TallaVariante talla = new TallaVariante(
                        modelTalla,
                        esNueva ? 0 : rs.getInt("id_variante"),
                        rs.getString("sku"),
                        rs.getString("ean"),
                        rs.getBigDecimal("precio_venta"),
                        stockSegunTipo,
                        rs.getBoolean("disponible"),
                        esNueva,
                        ubicacion
                    );

                    System.out.println("   INFO Talla " + contador + ": " + talla.getTalla().getNombre() +
                                     " (Género: " + generoTalla + ", Stock " + tipoVenta + ": " + stockSegunTipo +
                                     ", Disponible: " + talla.isDisponible() + ")");
                    
                    tallasDisponibles.add(talla);

                    // Formatear visualización de talla como en Create.java ("numero sistema abreviatura")
                    // Usar la variable generoTalla que ya fue definida arriba
                    String genAbbr;
                    if (generoTalla == null) {
                        genAbbr = "";
                    } else {
                        switch (generoTalla.toUpperCase()) {
                            case "MUJER": genAbbr = "M"; break;
                            case "HOMBRE": genAbbr = "H"; break;
                            case "NIÑO": genAbbr = "N"; break;
                            case "UNISEX": genAbbr = "U"; break;
                            default: genAbbr = generoTalla; break;
                        }
                    }
                    String tallaDisplay = rs.getString("numero") + " " + rs.getString("sistema") + (genAbbr.isEmpty() ? "" : (" " + genAbbr));

                    // Agregar fila a la tabla (sin columna STOCK)
                    model.addRow(new Object[]{
                        false, // SELECCIONAR
                        tallaDisplay, // TALLA con formato
                        0 // CANTIDAD (inicialmente 0)
                    });
                }
                
                
                if (tallasDisponibles.isEmpty()) {
                    System.out.println("ERROR No se encontraron tallas para idProducto=" + idProducto + ", idColor=" + idColor);
                    JOptionPane.showMessageDialog(this, 
                        "No hay tallas disponibles para este producto y color.", 
                        "Sin tallas", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("ERROR Error SQL en cargarTallasDisponibles: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error al cargar las tallas: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }

        // Inicializar total restante a 24 en modo caja
        if ("caja".equalsIgnoreCase(tipoVenta)) {
            lbTotal.setText(String.valueOf(24 * cajasAConvertir));
        }

        // Refrescar cabecera por si cambió el estado
        actualizarCabeceraDetalles();
        configurarBotonConvertir();
        actualizarBotonConvertir();
    }

    private String normalizarGenero(String genero) {
        if (genero == null) return "";
        String g = genero.trim().toUpperCase();

        System.out.println("Buscar DEBUG - Género ORIGINAL (bytes): " + java.util.Arrays.toString(g.getBytes()));

        // SOLUCIÓN DEFINITIVA: Eliminar TODOS los caracteres no-ASCII (incluye Ñ corrupta)
        // Mantener solo letras A-Z, espacios y números
        String gLimpio = g.replaceAll("[^A-Z0-9 ]", "");

        System.out.println("Actualizando Normalización paso 1: '" + genero + "' → '" + gLimpio + "' (solo ASCII)");

        // NIÑO: Buscar patrón NI*O (con cualquier carácter en medio que fue eliminado)
        if (gLimpio.matches("NI.*O") || gLimpio.equals("NINO") || gLimpio.equals("NINA") ||
            gLimpio.equals("N") || gLimpio.contains("NINO")) {
            System.out.println("SUCCESS  Normalización paso 2: Reconocido como NIÑO");
            return "NIÑO";
        }
        if (gLimpio.equals("INFANTE") || gLimpio.equals("INFANTIL")) return "NIÑO";
        if (gLimpio.equals("KIDS") || gLimpio.equals("KID")) return "NIÑO";
        if (gLimpio.equals("CHILDREN") || gLimpio.equals("CHILD")) return "NIÑO";
        if (gLimpio.equals("JUNIOR")) return "NIÑO";

        // HOMBRE y variantes
        if (gLimpio.contains("HOMBRE") || gLimpio.equals("CABALLERO") ||
            gLimpio.equals("VARON") || gLimpio.equals("H")) {
            return "HOMBRE";
        }
        if (gLimpio.equals("MASCULINO") || gLimpio.equals("MEN") || gLimpio.equals("MALE")) {
            return "HOMBRE";
        }

        // MUJER y variantes
        if (gLimpio.contains("MUJER") || gLimpio.contains("DAMA") || gLimpio.equals("M")) {
            return "MUJER";
        }
        if (gLimpio.equals("FEMENINO") || gLimpio.equals("WOMEN") || gLimpio.equals("FEMALE")) {
            return "MUJER";
        }

        // UNISEX
        if (gLimpio.equals("U") || gLimpio.equals("UNISEX")) return "UNISEX";

        System.out.println("WARNING  Normalización: Género no reconocido: '" + gLimpio + "'");
        return gLimpio.isEmpty() ? g : gLimpio;
    }

    /**
     * Permite seleccionar una imagen para aplicar a las variantes nuevas usando el explorador de archivos nativo
     */
    private void seleccionarImagenParaNuevasVariantes() {
        try {
            // USAR java.awt.FileDialog (Explorador nativo de Windows)
            java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
            java.awt.FileDialog fd = new java.awt.FileDialog(parentFrame, "Seleccionar Imagen", java.awt.FileDialog.LOAD);
            fd.setFile("*.jpg;*.jpeg;*.png;*.gif");
            fd.setVisible(true);

            if (fd.getFile() != null) {
                File archivoSeleccionado = new File(fd.getDirectory(), fd.getFile());
                byte[] imageBytes = Files.readAllBytes(archivoSeleccionado.toPath());
                
                // Guardar para aplicar a nuevas variantes
                imagenParaNuevasVariantes = imageBytes;

                // Previsualizar en el botón
                try {
                    ImageIcon icon = new ImageIcon(imageBytes);
                    Image scaled = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    btnImagen.setIcon(new ImageIcon(scaled));
                    btnImagen.setText(" Img");
                    btnImagen.setToolTipText("Imagen: " + archivoSeleccionado.getName() + " (" + (imageBytes.length / 1024) + " KB)");
                } catch (Exception ignore) {
                    // Si falla la previsualización, mantener solo el texto
                    btnImagen.setIcon(null);
                    btnImagen.setText("Imagen");
                    btnImagen.setToolTipText("Imagen seleccionada");
                }

                // Si hay variante de CAJA detectada, actualizar su imagen en BD
                if (idVarianteCajaSeleccionada != null && idVarianteCajaSeleccionada > 0) {
                    try {
                        ServiceProductVariant service = new ServiceProductVariant();
                        boolean ok = service.updateVariantImage(idVarianteCajaSeleccionada, imageBytes);
                        if (ok) {
                            JOptionPane.showMessageDialog(this,
                                "Imagen actualizada en la variante de CAJA (ID: " + idVarianteCajaSeleccionada + ").\n" +
                                "También se aplicará a las nuevas variantes PAR.",
                                "Imagen aplicada",
                                JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                "No se pudo actualizar la imagen de la variante de CAJA.",
                                "Advertencia",
                                JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Error SQL actualizando imagen de CAJA: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this,
                                "Error actualizando imagen de CAJA: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Imagen cargada (" + (imageBytes.length / 1024) + " KB). Se aplicará a variantes nuevas PAR.",
                        "Imagen lista",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar la imagen: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga la imagen desde la variante de CAJA seleccionada y la aplica como
     * previsualización y como imagen por defecto para las nuevas variantes PAR.
     * Si la CAJA no tiene imagen, ofrece seleccionar una imagen manualmente.
     */
    private void cargarImagenDeCajaOSeleccion() {
        try {
            if (idVarianteCajaSeleccionada == null || idVarianteCajaSeleccionada <= 0) {
                if (descontarCajaAlAceptar) {
                    JOptionPane.showMessageDialog(this,
                        "Primero busque y seleccione la caja a convertir.",
                        "Caja no seleccionada",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                } else {
                    seleccionarImagenParaNuevasVariantes();
                    return;
                }
            }

            ServiceProductVariant service = new ServiceProductVariant();
            byte[] imageBytes = service.getVariantImage(idVarianteCajaSeleccionada);

            if (imageBytes != null && imageBytes.length > 0) {
                // Guardar para aplicar a nuevas variantes
                imagenParaNuevasVariantes = imageBytes;

                // Previsualizar en el botón
                try {
                    ImageIcon icon = new ImageIcon(imageBytes);
                    Image scaled = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                    btnImagen.setIcon(new ImageIcon(scaled));
                    btnImagen.setText(" Caja");
                    btnImagen.setToolTipText("Imagen tomada de la CAJA (ID: " + idVarianteCajaSeleccionada + ")");
                } catch (Exception ignore) {
                    btnImagen.setIcon(null);
                    btnImagen.setText("Imagen");
                    btnImagen.setToolTipText("Imagen de CAJA cargada");
                }

                JOptionPane.showMessageDialog(this,
                    "Se cargó la imagen de la CAJA seleccionada.\nSe usará para las variantes PAR nuevas.",
                    "Imagen aplicada",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                int opt = JOptionPane.showConfirmDialog(this,
                        "La CAJA seleccionada no tiene imagen guardada.\n¿Desea seleccionar una imagen manualmente?",
                        "Sin imagen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    seleccionarImagenParaNuevasVariantes();
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error SQL obteniendo imagen de CAJA: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar la imagen de CAJA: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra opciones para establecer o reemplazar la imagen:
     * - Usar la imagen guardada en la CAJA
     * - Seleccionar una imagen desde archivos (actualiza CAJA y aplica a nuevas PAR)
     */
    private void mostrarOpcionesImagen() {
        try {
            // Si no hay caja seleccionada, ir directo a seleccionar archivo
            if (idVarianteCajaSeleccionada == null || idVarianteCajaSeleccionada <= 0) {
                seleccionarImagenParaNuevasVariantes();
                return;
            }

            String[] opciones = {"Usar imagen de CAJA", "Seleccionar desde archivos"};
            int choice = javax.swing.JOptionPane.showOptionDialog(
                    this,
                    "¿Cómo quieres establecer la imagen?",
                    "Imagen de variantes",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if (choice == 0) {
                cargarImagenDeCajaOSeleccion();
            } else if (choice == 1) {
                seleccionarImagenParaNuevasVariantes();
            }
        } catch (Exception ignore) {
            // En caso de cualquier problema, caer a seleccionar archivo
            seleccionarImagenParaNuevasVariantes();
        }
    }

    /**
     * Actualiza el texto del encabezado (lbdetalles) con la información clave
     * requerida por el flujo de conversión de caja → pares.
     */
    private void actualizarCabeceraDetalles() {
        String modelo = (codigoModeloProducto != null && !codigoModeloProducto.trim().isEmpty())
                ? codigoModeloProducto : "(sin modelo)";
        String idVarCaja = (idVarianteCajaSeleccionada != null && idVarianteCajaSeleccionada > 0)
                ? String.valueOf(idVarianteCajaSeleccionada) : "—";
        String detalle;
        if (descontarCajaAlAceptar) {
            detalle = String.format(
                "Modelo: %s | ID Var Caja: %s | Al convertir: -%d caja(s) = +%d pares",
                modelo, idVarCaja, cajasAConvertir, 24 * cajasAConvertir);
        } else {
            detalle = String.format(
                "Modelo: %s | ID Var Caja: %s | Distribuya %d pares (no se descuenta caja)",
                modelo, idVarCaja, 24 * cajasAConvertir);
        }
        lbdetalles.setText(detalle);
    }

    /**
     * Setter público para establecer la variante de CAJA seleccionada y el color.
     * Úsalo desde el formulario padre una vez el usuario elija la caja.
     * También actualiza el encabezado y carga la imagen asociada a la caja.
     *
     * @param idVarianteCaja ID de la variante que representa la caja seleccionada
     * @param idColorCaja    ID del color de la caja (se usa para crear variantes PAR)
     * @param nombreColor    Nombre del color (solo informativo)
     */
    public void setCajaSeleccionada(int idVarianteCaja, int idColorCaja, String nombreColor) {
        this.idVarianteCajaSeleccionada = idVarianteCaja;
        this.idColor = idColorCaja;
        this.nombreColorSeleccionado = nombreColor;
        // Recargar tallas para el color de la CAJA seleccionado
        cargarTallasDisponibles();
        actualizarCabeceraDetalles();
        // Intentar precargar imagen de la variante de caja
        cargarImagenDeCajaOSeleccion();
        actualizarBotonConvertir();
    }

    public void setMaxCajasConvertibles(int max) {
        if (max <= 0) max = 1;
        maxCajasConvertibles = max;
        cajasAConvertir = Math.min(cajasAConvertir, maxCajasConvertibles);
        spCajas.setModel(new javax.swing.SpinnerNumberModel(cajasAConvertir, 1, maxCajasConvertibles, 1));
        if ("caja".equalsIgnoreCase(tipoVenta)) {
            lbTotal.setText(String.valueOf(Math.max(0, 24 * cajasAConvertir - calcularSumaCantidades())));
            actualizarBotonConvertir();
            actualizarCabeceraDetalles();
        }
    }

    /**
     * Obtiene el código modelo del producto
     */
    private String obtenerCodigoModeloProducto(int idProd) {
        String codigo = null;
        String sql = "SELECT codigo_modelo FROM productos WHERE id_producto = ? LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idProd);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    codigo = rs.getString("codigo_modelo");
                }
            }
        } catch (SQLException e) {
            System.out.println("WARNING  No se pudo obtener código modelo: " + e.getMessage());
        }
        return codigo;
    }

    /**
     * Obtiene el género del producto desde la tabla productos
     */
    private String obtenerGeneroProducto(int idProd) {
        if (generoProducto != null) return generoProducto; // cache simple
        String gen = null;
        String sql = "SELECT genero FROM productos WHERE id_producto = ? LIMIT 1";
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idProd);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    gen = rs.getString("genero");
                }
            }
        } catch (SQLException e) {
            System.out.println("WARNING  No se pudo obtener género del producto: " + e.getMessage());
        }
        generoProducto = gen;
        return gen;
    }
    
    /**
    * CORRECCIÓN PRINCIPAL: Método mejorado para agregar tallas seleccionadas
    * Ahora NO crea variantes nuevas, solo valida que existan
    */
   private void agregarTallasSeleccionadas() {
       // Asegurar que cualquier edición activa del spinner se aplique al modelo
       if (tablaTallas != null && tablaTallas.getCellEditor() != null) {
           try { tablaTallas.getCellEditor().stopCellEditing(); } catch (Exception ignore) {}
       }

       DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
       List<TallaVariante> tallasSeleccionadas = new ArrayList<>();

       // Validar selecciones
       boolean haySeleccion = false;
       for (int i = 0; i < model.getRowCount(); i++) {
           Boolean seleccionada = (Boolean) model.getValueAt(i, 0);
           if (seleccionada != null && seleccionada) {
               haySeleccion = true;

               // Obtener cantidad
               Object cantidadObj = model.getValueAt(i, 2);
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

               TallaVariante talla = tallasDisponibles.get(i);

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

       // Validar total de pares SOLO en modo "caja"
       int totalPares = tallasSeleccionadas.stream()
               .mapToInt(TallaVariante::getCantidadSeleccionada)
               .sum();

       if ("caja".equalsIgnoreCase(tipoVenta)) {
           int requerido = 24 * cajasAConvertir;
           if (totalPares != requerido) {
               int restante = Math.max(0, requerido - totalPares);
               JOptionPane.showMessageDialog(this,
                   "Debe distribuir exactamente " + requerido + " pares. Restantes: " + restante,
                   "Cantidad inválida",
                   JOptionPane.WARNING_MESSAGE);
               return;
           }
       }

       // Validar caja solo si se va a descontar
       if (descontarCajaAlAceptar && idVarianteCajaSeleccionada == null) {
           JOptionPane.showMessageDialog(this,
               "Debe buscar y seleccionar una caja primero en el campo de búsqueda.",
               "Caja no seleccionada",
               JOptionPane.WARNING_MESSAGE);
           return;
       }

       // Validar color
       if (idColor <= 0) {
           JOptionPane.showMessageDialog(this,
               "Debe seleccionar un color válido para crear variantes nuevas.",
               "Color requerido",
               JOptionPane.WARNING_MESSAGE);
           return;
       }

       // WARNING  CAMBIO CRÍTICO: NO crear variantes aquí
       // Solo verificar que existan las variantes necesarias
       // La creación se hará dentro de convertirCajaAParesEnBodega() 
       // SOLO si no existe registro en inventario_bodega

       System.out.println("SUCCESS  Validación completa. Enviando al callback...");
       System.out.println("   - Tallas seleccionadas: " + tallasSeleccionadas.size());
       System.out.println("   - Total pares: " + totalPares);
       System.out.println("   - Cajas a convertir: " + cajasAConvertir);

       // Enviar datos al formulario padre SIN crear variantes previamente
       if (callback != null) {
           callback.onTallasSeleccionadas(
                   tallasSeleccionadas, 
                   tipoVenta, 
                   idVarianteCajaSeleccionada, 
                   imagenParaNuevasVariantes, 
                   cajasAConvertir
           );
           cerrarModal();
       }
   }

   /**
    * MÉTODO MEJORADO: Conversión de caja a pares con validación robusta
    * 
    * Flujo corregido:
    * 1. Bloquear caja(s) en inventario_bodega
    * 2. Descontar Stock_caja
    * 3. Para cada talla seleccionada:
    *    a) Verificar si existe variante PAR (producto_variantes)
    *    b) Si NO existe: crearla con SKU, EAN e imagen
    *    c) Verificar si existe en inventario_bodega de la bodega destino
    *    d) Si existe: UPDATE Stock_par += cantidad
    *    e) Si NO existe: INSERT con Stock_par = cantidad
    * 
    * @param idBodegaDestino ID de la bodega donde se realiza la conversión
    * @param idVarianteCaja ID de la variante tipo CAJA a descontar
    * @param tallasSeleccionadas Lista de tallas PAR a incrementar
    * @param cajasAConvertir Número de cajas a convertir (default 1)
    */
   public void convertirCajaAParesEnBodega(
           Integer idBodegaDestino, 
           Integer idVarianteCaja, 
           List<CreateTallas.TallaVariante> tallasSeleccionadas, 
           int cajasAConvertir) throws SQLException {

       if (idBodegaDestino == null || idVarianteCaja == null) {
           throw new SQLException("Datos incompletos: bodega=" + idBodegaDestino + 
                                  ", caja=" + idVarianteCaja);
       }

       if (cajasAConvertir <= 0) cajasAConvertir = 1;

       System.out.println("Actualizando Iniciando conversión de caja a pares:");
       System.out.println("   - Bodega destino: " + idBodegaDestino);
       System.out.println("   - Variante caja: " + idVarianteCaja);
       System.out.println("   - Cajas a convertir: " + cajasAConvertir);
       System.out.println("   - Tallas seleccionadas: " + tallasSeleccionadas.size());

       Connection con = conexion.getInstance().createConnection();
       con.setAutoCommit(false);

       try {
           // ========================================================================
           // PASO 1: Validar y descontar Stock_caja en inventario_bodega
           // ========================================================================
           System.out.println("Caja PASO 1: Validando stock de cajas...");

           int stockCajaDisponible = 0;
           String sqlCheckCaja = 
               "SELECT COALESCE(SUM(Stock_caja), 0) AS stock " +
               "FROM inventario_bodega " +
               "WHERE id_bodega = ? AND id_variante = ? AND activo = 1 " +
               "FOR UPDATE"; // Bloqueo pesimista

           try (PreparedStatement psCheck = con.prepareStatement(sqlCheckCaja)) {
               psCheck.setInt(1, idBodegaDestino);
               psCheck.setInt(2, idVarianteCaja);

               try (ResultSet rs = psCheck.executeQuery()) {
                   if (rs.next()) {
                       stockCajaDisponible = rs.getInt("stock");
                   }
               }
           }

           System.out.println("   OK Stock caja disponible: " + stockCajaDisponible + " (bodega=" + idBodegaDestino + ", variante=" + idVarianteCaja + ")");
           if (stockCajaDisponible < cajasAConvertir) {
               throw new SQLException(
                   "Stock insuficiente de cajas. Disponible: " + stockCajaDisponible + 
                   ", Requerido: " + cajasAConvertir
               );
           }

           // Límite adicional por configuración de UI (spCajas)
           if (maxCajasConvertibles > 0 && cajasAConvertir > maxCajasConvertibles) {
               throw new SQLException("Límite de conversión excedido. Máximo permitido: " + maxCajasConvertibles);
           }

           // Descontar cajas
           String sqlDecrementCaja = 
               "UPDATE inventario_bodega " +
               "SET Stock_caja = Stock_caja - ?, " +
               "    fecha_ultimo_movimiento = NOW() " +
               "WHERE id_bodega = ? AND id_variante = ? AND activo = 1 " +
               "  AND Stock_caja >= ?";

           try (PreparedStatement psDec = con.prepareStatement(sqlDecrementCaja)) {
               psDec.setInt(1, cajasAConvertir);
               psDec.setInt(2, idBodegaDestino);
               psDec.setInt(3, idVarianteCaja);
               psDec.setInt(4, cajasAConvertir);

               int affected = psDec.executeUpdate();
               if (affected == 0) {
                   throw new SQLException("No se pudo descontar las cajas (condición de carrera)");
               }

           System.out.println("   OK Cajas descontadas: " + cajasAConvertir);
           }

           // Generar ID de referencia único para agrupar toda la conversión
           int idReferenciaConversion = 0;
           String sqlGetNextId = "SELECT COALESCE(MAX(id_referencia), 0) + 1 AS next_id FROM inventario_movimientos " +
                                "WHERE tipo_referencia = 'conversion_caja_pares'";
           try (PreparedStatement psNextId = con.prepareStatement(sqlGetNextId);
                ResultSet rsNextId = psNextId.executeQuery()) {
               if (rsNextId.next()) {
                   idReferenciaConversion = rsNextId.getInt("next_id");
               }
           } catch (SQLException e) {
               // Si falla, usar timestamp como ID
               idReferenciaConversion = (int) (System.currentTimeMillis() / 1000);
           }

           System.out.println("   Nota ID Referencia Conversión: " + idReferenciaConversion);

           // Obtener ID de usuario
           Integer idUsuario = null;
           try {
               idUsuario = UserSession.getInstance().getCurrentUser().getIdUsuario();
           } catch (Exception e) {
               System.err.println("   WARNING  No se pudo obtener ID de usuario: " + e.getMessage());
           }

           // Registrar movimiento de salida de cajas
           String sqlMovSalidaCaja = "INSERT INTO inventario_movimientos " +
               "(id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares, fecha_movimiento, " +
               "id_referencia, tipo_referencia, id_usuario, observaciones) " +
               "VALUES (?,?,?,?,?,NOW(),?,?,?,?)";

           try (PreparedStatement psMovCaja = con.prepareStatement(sqlMovSalidaCaja)) {
               psMovCaja.setInt(1, idProducto);
               psMovCaja.setInt(2, idVarianteCaja);
               psMovCaja.setString(3, "salida caja");
               psMovCaja.setInt(4, cajasAConvertir);
               psMovCaja.setNull(5, java.sql.Types.INTEGER);
               psMovCaja.setInt(6, idReferenciaConversion);
               psMovCaja.setString(7, "conversion_caja_pares");
               if (idUsuario != null) {
                   psMovCaja.setInt(8, idUsuario);
               } else {
                   psMovCaja.setNull(8, java.sql.Types.INTEGER);
               }

               // Obtener info de la variante caja para el registro
               String nombreVarianteCaja = "";
               try (PreparedStatement psInfo = con.prepareStatement(
                   "SELECT CONCAT(t.numero, ' ', t.sistema, ' - ', COALESCE(c.nombre, 'Sin color')) AS nombre " +
                   "FROM producto_variantes pv " +
                   "LEFT JOIN tallas t ON t.id_talla = pv.id_talla " +
                   "LEFT JOIN colores c ON c.id_color = pv.id_color " +
                   "WHERE pv.id_variante = ?")) {
                   psInfo.setInt(1, idVarianteCaja);
                   try (ResultSet rsInfo = psInfo.executeQuery()) {
                       if (rsInfo.next()) {
                           nombreVarianteCaja = rsInfo.getString("nombre");
                       }
                   }
               }

               psMovCaja.setString(9, "CONVERSIÓN #" + idReferenciaConversion +
                                   " - Salida de " + cajasAConvertir + " caja(s): " + nombreVarianteCaja);
               psMovCaja.executeUpdate();
               System.out.println("   OK Movimiento de salida de cajas registrado (Ref: " + idReferenciaConversion + ")");
           } catch (SQLException e) {
               System.err.println("   WARNING  Error registrando movimiento de salida de cajas: " + e.getMessage());
               e.printStackTrace();
           }

           // Obtener id_proveedor desde la variante CAJA (prioridad) o del producto
            int idProveedorOrigen = 0;
            
            // PRIORIDAD 1: Usar proveedor forzado si existe
            if (idProveedorForzado != null && idProveedorForzado > 0) {
                idProveedorOrigen = idProveedorForzado;
                System.out.println("   OK Usando proveedor forzado: " + idProveedorOrigen);
            } else {
                // PRIORIDAD 2: Buscar en BD
                String sqlProv = "SELECT COALESCE(pv.id_proveedor, p.id_proveedor, 0) AS id_proveedor " +
                               "FROM producto_variantes pv " +
                               "INNER JOIN productos p ON p.id_producto = pv.id_producto " +
                               "WHERE pv.id_variante = ?";
                try (PreparedStatement psProv = con.prepareStatement(sqlProv)) {
                    psProv.setInt(1, idVarianteCaja);
                    try (ResultSet rsProv = psProv.executeQuery()) {
                        if (rsProv.next()) idProveedorOrigen = rsProv.getInt("id_proveedor");
                    }
                }
                System.out.println("   OK id_proveedor origen (BD): " + idProveedorOrigen);
            }

           // ========================================================================
           // PASO 2: Procesar cada talla PAR seleccionada
           // ========================================================================
           System.out.println("\nZapatos PASO 2: Procesando tallas PAR...");

           ServiceProductVariant serviceVariant = new ServiceProductVariant();

           String ubicacion = null;
           try {
               if (TxtUbuEspecifica != null) {
                   String t = TxtUbuEspecifica.getText();
                   if (t != null && !t.trim().isEmpty()) ubicacion = t.trim();
               }
           } catch (Exception ignore) {}

           for (TallaVariante tallaVar : tallasSeleccionadas) {
               int cantidadPares = Math.max(0, tallaVar.getCantidadSeleccionada());
               if (cantidadPares == 0) {
                   System.out.println("   WARNING  Talla " + tallaVar.getTalla().getNombre() +  " sin cantidad, omitiendo...");
                   continue;
               }

               int idTalla = tallaVar.getTalla().getTallaId();
               int idVariantePar = tallaVar.getIdVariante();

               System.out.println("\n   Nota Talla: " + tallaVar.getTalla().getNombre() + 
                                " (" + cantidadPares + " pares)");
               System.out.println("      - ID Talla: " + idTalla);
               System.out.println("      - ID Variante actual: " + idVariantePar);
               System.out.println("      - ¿Es nueva?: " + tallaVar.isNuevaVariante());

               // ================================================================
               // PASO 2.1: Verificar/Crear variante PAR en producto_variantes
               // ================================================================
               if (tallaVar.isNuevaVariante() || idVariantePar <= 0) {
                   System.out.println("       Creando variante PAR en producto_variantes...");

                   // Crear nueva variante
                   ModelProductVariant nuevaVariante = new ModelProductVariant(
                       idProducto,
                       idTalla,
                       idColor,
                       0 // Stock inicial en 0, se actualizará en inventario_bodega
                   );

                   // Configurar propiedades adicionales
                   Integer idBodegaUsuario = UserSession.getInstance().getIdBodegaUsuario();
                   if (idBodegaUsuario != null) {
                       nuevaVariante.setWarehouseId(idBodegaUsuario);
                   }

                   if (tallaVar.getPrecioVenta() != null) {
                       try {
                           nuevaVariante.setSalePrice(tallaVar.getPrecioVenta().doubleValue());
                       } catch (Exception ignore) {}
                   }

                   // CRÍTICO: Asignar id_proveedor ANTES del insert
                   if (idProveedorOrigen > 0) {
                       nuevaVariante.setSupplierId(idProveedorOrigen);
                       System.out.println("      OK ID Proveedor asignado: " + idProveedorOrigen);
                   }

                   // Asignar imagen si está disponible
                   if (imagenParaNuevasVariantes != null && imagenParaNuevasVariantes.length > 0) {
                       nuevaVariante.setImageBytes(imagenParaNuevasVariantes);
                       nuevaVariante.setHasImage(true);
                       System.out.println("      OK Imagen asignada (" +
                                        (imagenParaNuevasVariantes.length / 1024) + " KB)");
                   }

                   nuevaVariante.generateSku(codigoModeloProducto != null ? codigoModeloProducto.trim() : null);
                   nuevaVariante.generateEanIfEmpty();

                   int idUpsert = serviceVariant.upsertVariant(nuevaVariante);
                   if (idUpsert <= 0) {
                       throw new SQLException("No se pudo crear/actualizar variante PAR para talla " + tallaVar.getTalla().getNombre());
                   }
                   idVariantePar = idUpsert;
                   tallaVar.setIdVariante(idVariantePar);

                   System.out.println("      SUCCESS  Variante PAR lista con ID: " + idVariantePar);
                   System.out.println("         - SKU: " + nuevaVariante.getSku());
                   System.out.println("         - EAN: " + nuevaVariante.getEan());
               } else {
                   System.out.println("      OK Variante PAR ya existe, ID: " + idVariantePar);

                   // Actualizar proveedor en variante existente
                   if (idProveedorOrigen > 0) {
                       try (PreparedStatement psUpdProv = con.prepareStatement("UPDATE producto_variantes SET id_proveedor=? WHERE id_variante=?")) {
                           psUpdProv.setInt(1, idProveedorOrigen);
                           psUpdProv.setInt(2, idVariantePar);
                           psUpdProv.executeUpdate();
                           System.out.println("      OK Proveedor actualizado: " + idProveedorOrigen);
                       } catch (SQLException e) {
                           System.err.println("      WARNING  Error actualizando proveedor: " + e.getMessage());
                       }
                   }

                   // Actualizar imagen en variante existente
                   if (imagenParaNuevasVariantes != null && imagenParaNuevasVariantes.length > 0) {
                       try (PreparedStatement psUpdImg = con.prepareStatement(
                           "UPDATE producto_variantes SET imagen=?, fecha_actualizacion=CURRENT_TIMESTAMP WHERE id_variante=?")) {
                           psUpdImg.setBytes(1, imagenParaNuevasVariantes);
                           psUpdImg.setInt(2, idVariantePar);
                           psUpdImg.executeUpdate();
                           System.out.println("      OK Imagen actualizada (" +
                                            (imagenParaNuevasVariantes.length / 1024) + " KB)");
                       } catch (SQLException e) {
                           System.err.println("      WARNING  Error actualizando imagen: " + e.getMessage());
                       }
                   }
               }

               // ================================================================
               // PASO 2.2: Verificar si existe en inventario_bodega
               // ================================================================
               System.out.println("      Buscar Verificando registro en inventario_bodega...");

               boolean existeEnInventario = false;
                String sqlCheckInventario = 
                    "SELECT 1 FROM inventario_bodega " +
                    "WHERE id_bodega = ? AND id_variante = ? AND activo = 1 " +
                    "FOR UPDATE";

               try (PreparedStatement psCheckInv = con.prepareStatement(sqlCheckInventario)) {
                   psCheckInv.setInt(1, idBodegaDestino);
                   psCheckInv.setInt(2, idVariantePar);

                   try (ResultSet rsInv = psCheckInv.executeQuery()) {
                       existeEnInventario = rsInv.next();
                   }
               }

               System.out.println("      " + (existeEnInventario ? "OK" : "ERROR ") + 
                                " Registro en inventario_bodega: " + 
                                (existeEnInventario ? "EXISTE" : "NO EXISTE"));

               // ================================================================
               // PASO 2.3: INSERT o UPDATE en inventario_bodega
               // ================================================================
               if (existeEnInventario) {
                   // UPDATE: Incrementar Stock_par
                   System.out.println("       Actualizando stock existente...");

                   String sqlUpdate = 
                       "UPDATE inventario_bodega " +
                       "SET Stock_par = Stock_par + ?, " +
                       "    fecha_ultimo_movimiento = NOW(), " +
                       "    ubicacion_especifica = COALESCE(?, ubicacion_especifica) " +
                       "WHERE id_bodega = ? AND id_variante = ? AND activo = 1";

                   try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                       psUpdate.setInt(1, cantidadPares);
                       if (ubicacion != null) psUpdate.setString(2, ubicacion); else psUpdate.setNull(2, java.sql.Types.VARCHAR);
                       psUpdate.setInt(3, idBodegaDestino);
                       psUpdate.setInt(4, idVariantePar);

                       int rowsUpdated = psUpdate.executeUpdate();
                       if (rowsUpdated == 0) {
                           throw new SQLException(
                               "No se pudo actualizar stock para variante " + idVariantePar
                           );
                       }

                       System.out.println("      SUCCESS  Stock incrementado en +" + cantidadPares + " pares");
                   }

               } else {
                   // INSERT: Crear nuevo registro
                   System.out.println("      Nota Creando nuevo registro en inventario...");

                   String sqlInsert = 
                       "INSERT INTO inventario_bodega " +
                       "(id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, " +
                       " fecha_ultimo_movimiento, activo, ubicacion_especifica) " +
                       "VALUES (?, ?, ?, 0, 0, NOW(), 1, ?)";

                   try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                       psInsert.setInt(1, idBodegaDestino);
                       psInsert.setInt(2, idVariantePar);
                       psInsert.setInt(3, cantidadPares);
                       if (ubicacion != null) psInsert.setString(4, ubicacion); else psInsert.setNull(4, java.sql.Types.VARCHAR);

                       int rowsInserted = psInsert.executeUpdate();
                       if (rowsInserted == 0) {
                           throw new SQLException(
                               "No se pudo crear registro de inventario para variante " + idVariantePar
                           );
                       }

                       System.out.println("      SUCCESS  Registro creado con " + cantidadPares + " pares");
                   }
               }

               // Registrar auditoría (opcional, si existe la tabla)
               try (PreparedStatement psAud = con.prepareStatement(
                       "INSERT INTO auditoria_trazabilidad (tipo_evento, id_variante, cantidad, id_proveedor, id_bodega_destino, fecha_evento, activo) " +
                       "VALUES ('CONVERSION_CAJA_A_PARES', ?, ?, ?, ?, NOW(), 1)")) {
                   psAud.setInt(1, idVariantePar);
                   psAud.setInt(2, cantidadPares);
                   if (idProveedorOrigen > 0) psAud.setInt(3, idProveedorOrigen); else psAud.setNull(3, java.sql.Types.INTEGER);
                   psAud.setInt(4, idBodegaDestino);
                   psAud.executeUpdate();
               } catch (SQLException ignore) {}

               // ================================================================
               // PASO 2.4: Registrar movimiento de entrada de pares
               // ================================================================
               String sqlMovEntradaPar = "INSERT INTO inventario_movimientos " +
                   "(id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares, fecha_movimiento, " +
                   "id_referencia, tipo_referencia, id_usuario, observaciones) " +
                   "VALUES (?,?,?,?,?,NOW(),?,?,?,?)";

               try (PreparedStatement psMovPar = con.prepareStatement(sqlMovEntradaPar)) {
                   psMovPar.setInt(1, idProducto);
                   psMovPar.setInt(2, idVariantePar);
                   psMovPar.setString(3, "entrada par");
                   psMovPar.setInt(4, cantidadPares);
                   psMovPar.setInt(5, cantidadPares);
                   psMovPar.setInt(6, idReferenciaConversion);
                   psMovPar.setString(7, "conversion_caja_pares");
                   if (idUsuario != null) {
                       psMovPar.setInt(8, idUsuario);
                   } else {
                       psMovPar.setNull(8, java.sql.Types.INTEGER);
                   }
                   psMovPar.setString(9, "CONVERSIÓN #" + idReferenciaConversion +
                                      " - Entrada de " + cantidadPares + " pares: " +
                                      tallaVar.getTalla().getNombre() + " " +
                                      tallaVar.getTalla().getDescripcion());
                   psMovPar.executeUpdate();
                   System.out.println("      OK Movimiento de entrada de pares registrado (Ref: " + idReferenciaConversion + ")");
               } catch (SQLException e) {
                   System.err.println("      WARNING  Error registrando movimiento de entrada de pares: " + e.getMessage());
                   e.printStackTrace();
               }
           }

           // ========================================================================
           // PASO 3: Commit de la transacción
           // ========================================================================
           con.commit();
           System.out.println("\nSUCCESS  Conversión completada exitosamente");
           System.out.println("   - Cajas descontadas: " + cajasAConvertir);
           System.out.println("   - Pares distribuidos: " + 
                            (tallasSeleccionadas.stream()
                             .mapToInt(TallaVariante::getCantidadSeleccionada)
                             .sum()));

           try { javax.swing.JOptionPane.showMessageDialog(this, "Conversión completada correctamente", "Éxito", javax.swing.JOptionPane.INFORMATION_MESSAGE); } catch (Throwable ignore) {}

       } catch (SQLException e) {
           // Rollback en caso de error
           try { 
               con.rollback(); 
               System.out.println("ERROR  Rollback ejecutado por error: " + e.getMessage());
           } catch (SQLException rbEx) {
               System.out.println("WARNING  Error en rollback: " + rbEx.getMessage());
           }
           throw e;

       } finally {
           try { 
               con.setAutoCommit(true); 
               con.close(); 
           } catch (SQLException closeEx) {
               System.out.println("WARNING  Error cerrando conexión: " + closeEx.getMessage());
           }
       }
   }
   
   /**
 * Valida que no haya duplicados antes de convertir
 * 
 * @param numeroTraspaso Número del traspaso
 * @param items Lista de [idVariante, cantidad] a facturar
 * @return true si la validación es exitosa
 */
private boolean validarStockEnBodegaDestino(
        String numeroTraspaso, 
        java.util.List<int[]> items) throws SQLException {
    
    Integer idBodegaDestino = null;
    
    // Obtener bodega destino del traspaso
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement pst = con.prepareStatement(
                 "SELECT id_bodega_destino FROM traspasos WHERE numero_traspaso = ?")) {
        pst.setString(1, numeroTraspaso);
        try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                idBodegaDestino = rs.getInt(1);
            }
        }
    }
    
    if (idBodegaDestino == null || idBodegaDestino <= 0) {
        Toast.show(this, Toast.Type.ERROR, 
                "No se pudo determinar la bodega destino del traspaso");
        return false;
    }
    
    // Validar stock disponible para cada item
    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement pst = con.prepareStatement(
                 "SELECT COALESCE(Stock_par, 0) AS stock " +
                 "FROM inventario_bodega " +
                 "WHERE id_bodega = ? AND id_variante = ? AND activo = 1")) {
        
        for (int[] item : items) {
            int idVariante = item[0];
            int cantidadRequerida = item[1];
            
            pst.setInt(1, idBodegaDestino);
            pst.setInt(2, idVariante);
            
            try (ResultSet rs = pst.executeQuery()) {
                int stockDisponible = 0;
                if (rs.next()) {
                    stockDisponible = rs.getInt("stock");
                }
                
                if (cantidadRequerida > stockDisponible) {
                    Toast.show(this, Toast.Type.WARNING, 
                            "Stock insuficiente para variante " + idVariante + 
                            ". Disponible: " + stockDisponible + 
                            ", Requerido: " + cantidadRequerida);
                    return false;
                }
            }
        }
    }
    
    return true;
}

    // Método público para disparar la confirmación desde el botón "Aceptar" del modal contenedor
    public void confirmarAceptar() {
        if (conversionEnCurso) return;
        conversionEnCurso = true;
        try {
            btnConvertir.setEnabled(false);
            agregarTallasSeleccionadas();
        } finally {
            conversionEnCurso = false;
            btnConvertir.setEnabled(true);
        }
    }

    private void cerrarModal() {
        if (modalController != null) {
            modalController.close();
        } else {
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        }
    }

    // ====== Utilidades de cálculo y actualización del total ======
    private int calcularSumaCantidades() {
        DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
        int sum = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object val = model.getValueAt(i, 2);
            if (val != null) {
                try { sum += Integer.parseInt(val.toString()); } catch (NumberFormatException ignore) {}
            }
        }
        return sum;
    }

    private int calcularSumaCantidadesExcepto(int filaExcepto) {
        DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
        int sum = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (i == filaExcepto) continue;
            Object val = model.getValueAt(i, 2);
            if (val != null) {
                try { sum += Integer.parseInt(val.toString()); } catch (NumberFormatException ignore) {}
            }
        }
        return sum;
    }

    private void actualizarTotalRestante() {
        int sum = calcularSumaCantidades();
        int requerido = 24 * cajasAConvertir;
        int restante = Math.max(0, requerido - sum);
        lbTotal.setText(String.valueOf(restante));
        // Mantener el encabezado informativo siempre actualizado
        actualizarCabeceraDetalles();
        actualizarBotonConvertir();
    }

    private void configurarBotonConvertir() {
        boolean visible = "caja".equalsIgnoreCase(tipoVenta);
        btnConvertir.setVisible(visible);
        for (java.awt.event.ActionListener al : btnConvertir.getActionListeners()) { btnConvertir.removeActionListener(al); }
        btnConvertir.addActionListener(e -> confirmarAceptar());
    }

    private void actualizarBotonConvertir() {
        boolean habilitado = false;
        if ("caja".equalsIgnoreCase(tipoVenta)) {
            int total = calcularSumaCantidades();
            boolean haySeleccion = false;
            DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object sel = model.getValueAt(i, 0);
                Object cant = model.getValueAt(i, 2);
                int c = 0;
                if (cant != null) {
                    try { c = Integer.parseInt(cant.toString()); } catch (Exception ignore) {}
                }
                if (sel instanceof Boolean && ((Boolean) sel) && c > 0) { haySeleccion = true; break; }
            }
            boolean cajaOk = !descontarCajaAlAceptar || (idVarianteCajaSeleccionada != null && idVarianteCajaSeleccionada > 0);
            int totalRequerido = 24 * cajasAConvertir;
            habilitado = haySeleccion && total == totalRequerido && cajaOk;

            // Buscar DEPURACIÓN: Mostrar por qué el botón está deshabilitado
            if (!habilitado) {
                System.out.println("WARNING  Botón CONVERTIR deshabilitado:");
                System.out.println("   - Hay selección: " + haySeleccion);
                System.out.println("   - Total pares seleccionados: " + total);
                System.out.println("   - Total requerido: " + totalRequerido);
                System.out.println("   - Caja seleccionada OK: " + cajaOk);
                System.out.println("   - ID Variante Caja: " + idVarianteCajaSeleccionada);
                System.out.println("   - Descontar caja al aceptar: " + descontarCajaAlAceptar);
            } else {
                System.out.println("SUCCESS  Botón CONVERTIR habilitado - Total OK: " + total + " pares");
            }
        }
        btnConvertir.setEnabled(habilitado);
    }

 

    // ====== MÉTODOS PARA GESTIÓN DE CONFIGURACIONES DE TALLAS ======

    /**
     * Carga las configuraciones de tallas disponibles en el ComboBox cbxZuela
     */
    private void cargarConfiguracionesEnComboBox() {
        if (cbxZuela == null) return;

        cbxZuela.removeAllItems();
        cbxZuela.addItem("Seleccionar configuración");

        String sql = "SELECT id_configuracion, nombre_configuracion, genero " +
                    "FROM configuraciones_tallas " +
                    "WHERE activo = 1 " +
                    "ORDER BY genero, nombre_configuracion";

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_configuracion");
                String nombre = rs.getString("nombre_configuracion");
                String genero = rs.getString("genero");

                // Crear item con ID embebido
                ConfiguracionItem item = new ConfiguracionItem(id, nombre, genero);
                cbxZuela.addItem(item.toString());
            }

            System.out.println("SUCCESS  Configuraciones cargadas en ComboBox");

        } catch (SQLException e) {
            System.err.println("ERROR  Error cargando configuraciones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para representar un item de configuración en el ComboBox
     */
    private static class ConfiguracionItem {
        int id;
        String nombre;
        String genero;

        public ConfiguracionItem(int id, String nombre, String genero) {
            this.id = id;
            this.nombre = nombre;
            this.genero = genero;
        }

        @Override
        public String toString() {
            return nombre + " (" + genero + ")";
        }
    }

    /**
     * Aplica una configuración seleccionada a la tabla de tallas
     */
    private void aplicarConfiguracion(String nombreConfiguracion) {
        if (nombreConfiguracion == null || nombreConfiguracion.equals("Seleccionar configuración")) {
            return;
        }

        // Extraer el nombre real (eliminar el género entre paréntesis)
        String nombreReal = nombreConfiguracion;
        if (nombreConfiguracion.contains("(")) {
            nombreReal = nombreConfiguracion.substring(0, nombreConfiguracion.lastIndexOf("(")).trim();
        }

        System.out.println("Actualizando Aplicando configuración: " + nombreReal);

        String sql = "SELECT ct.id_configuracion, ct.genero, " +
                    "       ctd.id_talla, ctd.cantidad_pares " +
                    "FROM configuraciones_tallas ct " +
                    "INNER JOIN configuraciones_tallas_detalle ctd ON ct.id_configuracion = ctd.id_configuracion " +
                    "WHERE ct.nombre_configuracion = ? AND ct.activo = 1 " +
                    "ORDER BY ctd.orden";

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, nombreReal);

            try (ResultSet rs = pst.executeQuery()) {
                // Limpiar selecciones actuales
                DefaultTableModel model = (DefaultTableModel) tablaTallas.getModel();
                for (int i = 0; i < model.getRowCount(); i++) {
                    model.setValueAt(false, i, 0); // Deseleccionar
                    model.setValueAt(0, i, 2); // Cantidad = 0
                }

                int totalAplicado = 0;
                int tallasAplicadas = 0;

                while (rs.next()) {
                    int idTalla = rs.getInt("id_talla");
                    int cantidadPares = rs.getInt("cantidad_pares");

                    //  MULTIPLICAR POR EL NÚMERO DE CAJAS SELECCIONADAS
                    // Si el usuario selecciona 2 cajas y la config dice "2 pares", aplicará 4 pares
                    int cantidadFinal = cantidadPares * cajasAConvertir;

                    // Buscar la talla en la lista actual
                    boolean tallaEncontrada = false;
                    for (int i = 0; i < tallasDisponibles.size(); i++) {
                        TallaVariante tv = tallasDisponibles.get(i);
                        if (tv.getTalla().getTallaId() == idTalla) {
                            // Aplicar la configuración con cantidades multiplicadas
                            model.setValueAt(true, i, 0); // Seleccionar
                            model.setValueAt(cantidadFinal, i, 2); // Cantidad × cajas

                            totalAplicado += cantidadFinal;
                            tallasAplicadas++;
                            tallaEncontrada = true;

                            System.out.println("   OK Talla " + tv.getTalla().getNombre() +
                                             " (ID:" + idTalla + "): " + cantidadPares +
                                             " pares × " + cajasAConvertir + " cajas = " +
                                             cantidadFinal + " pares");
                            break;
                        }
                    }
                    if (!tallaEncontrada) {
                        System.out.println("   WARNING  Talla ID " + idTalla + " NO encontrada en lista actual");
                    }
                }

                if (tallasAplicadas > 0) {
                    System.out.println("SUCCESS  Configuración aplicada:");
                    System.out.println("   - Tallas configuradas: " + tallasAplicadas);
                    System.out.println("   - Total pares: " + totalAplicado);
                    System.out.println("   - Cajas: " + cajasAConvertir);

                    // Actualizar UI
                    SwingUtilities.invokeLater(() -> {
                        tablaTallas.repaint();
                        actualizarTotalRestante();
                        actualizarBotonConvertir();
                    });

                    // Mensaje personalizado según número de cajas
                    String mensaje = cajasAConvertir == 1
                        ? "Configuración aplicada: " + tallasAplicadas + " tallas, " + totalAplicado + " pares"
                        : "Configuración aplicada: " + tallasAplicadas + " tallas × " + cajasAConvertir + " cajas = " + totalAplicado + " pares";

                    Toast.show(this, Toast.Type.SUCCESS, mensaje);
                } else {
                    Toast.show(this, Toast.Type.WARNING,
                              "No se encontraron tallas compatibles con esta configuración");
                }
            }

        } catch (SQLException e) {
            System.err.println("ERROR  Error aplicando configuración: " + e.getMessage());
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR,
                      "Error al aplicar configuración: " + e.getMessage());
        }
    }

    /**
     * Abre el diálogo para crear o gestionar configuraciones
     */
    private void abrirDialogoConfiguraciones() {
        try {
            // Obtener la ventana padre (puede ser Frame o Dialog)
            java.awt.Window window = SwingUtilities.getWindowAncestor(this);
            Frame parentFrame = null;

            // Intentar obtener Frame si es posible
            if (window instanceof Frame) {
                parentFrame = (Frame) window;
            }
            // Si no es Frame, pasar null (el diálogo se centrará en la pantalla)

            boolean guardado = ConfiguracionTallasDialog.mostrarDialogoNuevo(parentFrame);

            if (guardado) {
                // Recargar el ComboBox con las configuraciones actualizadas
                cargarConfiguracionesEnComboBox();
                Toast.show(this, Toast.Type.SUCCESS,
                          "Configuración guardada. Selecciónela del ComboBox para aplicarla.");
            }

        } catch (Exception e) {
            System.err.println("ERROR  Error abriendo diálogo de configuraciones: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error al abrir el diálogo: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LbUbicacionEspecifica;
    private javax.swing.JTextField TxtUbuEspecifica;
    private javax.swing.JButton btnAjustes;
    private javax.swing.JButton btnConvertir;
    private javax.swing.JButton btnImagen;
    private javax.swing.JComboBox<String> cbxZuela;
    private raven.datetime.component.date.DatePicker datePicker;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel lbTotal;
    private javax.swing.JLabel lbdetalles;
    private javax.swing.JSpinner spCajas;
    private javax.swing.JTable tablaTallas;
    // End of variables declaration//GEN-END:variables
}
