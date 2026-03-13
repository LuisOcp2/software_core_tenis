/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.principal.conexion;

/**
 *
 * @author lmog2
 */
public class EnvioTraspaso extends javax.swing.JPanel {

    private static final String PANEL = "arc:200;background:lighten($Menu.background,25%)";
    private static final String CONTAINER = "arc:20;background:$Login.background";
    private static final String CONTAINER1 = "arc:15;background:lighten($Menu.background,25%)";
    private ModalEnvioTraspasoMejorado.EnvioCallback callback;
    private final FontIcon iconBodega;
    private final FontIcon iconBodegaD;
    private final FontIcon iconCdr;
    private final FontIcon iconCheck;
    private final FontIcon iconTit1;
    private final FontIcon iconBook;
    private final FontIcon box;
    private String numeroTraspaso;
    private Map<String, Object> datosTraspaso;
    private List<Map<String, Object>> productosTraspaso;
    private List<Map<String, Object>> productosSeleccionados;
    
   
    

    public interface EnvioCallback {

        void onEnvioExitoso(String numeroTraspaso, List<Map<String, Object>> productosEnviados);

        void onEnvioCancelado();
    }

    public EnvioTraspaso() {
        initComponents();
        this.productosSeleccionados = new ArrayList<>(); // AGREGAR ESTA LÍNEA
        interfaz();
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconBodega = createColoredIcon(FontAwesomeSolid.BUILDING, tabTextColor);
        iconBodegaD = createColoredIcon(FontAwesomeSolid.BUSINESS_TIME, tabTextColor);
        iconCdr = createColoredIcon(FontAwesomeSolid.CALENDAR_DAY, tabTextColor);
        iconCheck = createColoredIcon(FontAwesomeSolid.CHECK_CIRCLE, tabTextColor);
        iconTit1 = createColoredIcon(FontAwesomeSolid.PAPER_PLANE, tabTextColor);
        iconBook = createColoredIcon(FontAwesomeSolid.BOOK, tabTextColor);
        box = createColoredIcon(FontAwesomeSolid.BOX, tabTextColor);
        cargarIcons();
        configurarEventos(); // AGREGAR ESTA LÍNEA
    }

    /**
     * Configura los eventos de la interfaz
     */
   private void configurarEventos() {
    // Evento para seleccionar/deseleccionar todos
    cb_seleccionarTodos.addActionListener(e -> {
        boolean seleccionado = cb_seleccionarTodos.isSelected();
        DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(seleccionado, i, 0);
        }

        actualizarContadores();
    });

    // Evento para botón cancelar
    btnCancel.addActionListener(e -> cancelarEnvio());

    // Evento para botón confirmar envío
    btnConfirmarEnvio.addActionListener(e -> procesarEnvio());
    
    // NOTA: El listener de tabla se configurará después de cargar el modelo
    // en el método configurarTablaProductos()
}

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    /**
     * Valida el estado del checkbox "Seleccionar todos"
     */
    private void validarSeleccionTodos() {
        DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();
        int totalFilas = model.getRowCount();
        int seleccionadas = 0;

        for (int i = 0; i < totalFilas; i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                seleccionadas++;
            }
        }

        if (seleccionadas == 0) {
            cb_seleccionarTodos.setSelected(false);
        } else if (seleccionadas == totalFilas) {
            cb_seleccionarTodos.setSelected(true);
        } else {
            cb_seleccionarTodos.setSelected(false);
        }
    }

// ===== REEMPLAZAR EL MÉTODO configurarTraspaso EXISTENTE =====
    public void configurarTraspaso(String numeroTraspaso) {
        this.numeroTraspaso = numeroTraspaso;
        txtNumeroTraspaso.setText(numeroTraspaso + " - Confirmar envío de productos");

        try {
            cargarDatosTraspaso(numeroTraspaso);
            actualizarContadores();
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error cargando datos del traspaso: " + e.getMessage());
        }
    }

// ===== REEMPLAZAR EL MÉTODO cargarDatosTraspaso EXISTENTE =====
    private void cargarDatosTraspaso(String numeroTraspaso) throws SQLException {
        cargarInformacionTraspaso();
        cargarProductosTraspaso(numeroTraspaso);
    }

    /**
     * Carga la información general del traspaso
     */
    private void cargarInformacionTraspaso() throws SQLException {
        String sql = "SELECT t.*, "
                + "bo.nombre as bodega_origen, bd.nombre as bodega_destino, "
                + "us.nombre as usuario_solicita "
                + "FROM traspasos t "
                + "INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega "
                + "INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega "
                + "INNER JOIN usuarios us ON t.id_usuario_solicita = us.id_usuario "
                + "WHERE t.numero_traspaso = ?";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, numeroTraspaso);
        ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                datosTraspaso = new HashMap<>();
                datosTraspaso.put("id_traspaso", rs.getInt("id_traspaso"));
                datosTraspaso.put("numero_traspaso", rs.getString("numero_traspaso"));
                datosTraspaso.put("id_bodega_origen", rs.getInt("id_bodega_origen"));
                datosTraspaso.put("id_bodega_destino", rs.getInt("id_bodega_destino"));
                datosTraspaso.put("bodega_origen", rs.getString("bodega_origen"));
                datosTraspaso.put("bodega_destino", rs.getString("bodega_destino"));
                datosTraspaso.put("usuario_solicita", rs.getString("usuario_solicita"));
                datosTraspaso.put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
                datosTraspaso.put("estado", rs.getString("estado"));
                datosTraspaso.put("motivo", rs.getString("motivo"));

            // Actualizar la interfaz con los datos
            txtBodegaOrigen.setText(rs.getString("bodega_origen"));
            txtBodegaDestino.setText(rs.getString("bodega_destino"));
            txtFechaSolicitud.setText(rs.getTimestamp("fecha_solicitud").toString());
            txtEstado.setText(rs.getString("estado").toUpperCase());
        }

        rs.close();
        stmt.close();
        conn.close();
    }
// ===== REEMPLAZAR EL MÉTODO cargarProductosTraspaso EXISTENTE =====

    private void cargarProductosTraspaso(String numeroTraspaso) throws SQLException {
        Integer idBodegaOrigen = null;
        try { Object o = datosTraspaso != null ? datosTraspaso.get("id_bodega_origen") : null; if (o instanceof Integer) { idBodegaOrigen = (Integer) o; } } catch (Exception ignore) {}
        if (idBodegaOrigen == null) {
            String sqlB = "SELECT id_bodega_origen FROM traspasos WHERE numero_traspaso = ?";
            try (Connection c2 = conexion.getInstance().createConnection(); PreparedStatement s2 = c2.prepareStatement(sqlB)) {
                s2.setString(1, numeroTraspaso);
                try (ResultSet r2 = s2.executeQuery()) { if (r2.next()) idBodegaOrigen = r2.getInt(1); }
            }
        }

        String sql = "SELECT td.*, p.nombre as producto_nombre, p.codigo_modelo, "
                + "c.nombre as color_nombre, t.numero as talla_numero, "
                + "pv.sku, pv.ean, "
                + "(td.cantidad_solicitada - COALESCE(td.cantidad_enviada, 0)) as pendiente_envio, "
                + "ib.ubicacion_especifica AS ubicacion_especifica "
                + "FROM traspaso_detalles td "
                + "INNER JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso "
                + "INNER JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN inventario_bodega ib ON ib.id_variante = td.id_variante AND ib.id_bodega = ? AND ib.activo = 1 "
                + "WHERE tr.numero_traspaso = ? "
                + "AND (td.cantidad_solicitada - COALESCE(td.cantidad_enviada, 0)) > 0 "
                + "ORDER BY p.nombre, c.nombre, t.numero";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, idBodegaOrigen != null ? idBodegaOrigen : 0);
        stmt.setString(2, numeroTraspaso);
        ResultSet rs = stmt.executeQuery();

        productosTraspaso = new ArrayList<>();

        // Configurar el modelo de la tabla
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"OK", "Producto", "Solicitado", "Pendiente", "Cantidad", "Estantería", "Observaciones"},
                0
        ) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Boolean.class;
                    case 2:
                    case 3:
                    case 4:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 4 || column == 6;
            }
        };

        while (rs.next()) {
            Map<String, Object> producto = new HashMap<>();
            producto.put("id_detalle", rs.getInt("id_detalle_traspaso"));
            producto.put("id_producto", rs.getInt("id_producto"));
            producto.put("id_variante", rs.getObject("id_variante"));
            producto.put("cantidad_solicitada", rs.getInt("cantidad_solicitada"));
            producto.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
            producto.put("pendiente_envio", rs.getInt("pendiente_envio"));
            producto.put("tipo", rs.getString("Tipo"));

            // Construir descripción completa
            StringBuilder descripcion = new StringBuilder();
            descripcion.append(rs.getString("producto_nombre"));
            if (rs.getString("color_nombre") != null) {
                descripcion.append(" - ").append(rs.getString("color_nombre"));
            }
            if (rs.getString("talla_numero") != null) {
                descripcion.append(" - Talla ").append(rs.getString("talla_numero"));
            }
            if (rs.getString("sku") != null) {
                descripcion.append(" (SKU: ").append(rs.getString("sku")).append(")");
            }

            producto.put("descripcion_completa", descripcion.toString());
            producto.put("sku", rs.getString("sku"));
            producto.put("ubicacion_especifica", rs.getString("ubicacion_especifica"));

            productosTraspaso.add(producto);

            // Agregar a la tabla
            model.addRow(new Object[]{
                false, // Checkbox no seleccionado por defecto
                descripcion.toString(),
                rs.getInt("cantidad_solicitada"),
                rs.getInt("pendiente_envio"),
                rs.getInt("pendiente_envio"), // Cantidad por defecto = pendiente
                rs.getString("ubicacion_especifica") != null ? rs.getString("ubicacion_especifica") : "",
                "" // Observaciones vacías
            });
        }

        table_produtos.setModel(model);
        configurarTablaProductos();

        rs.close();
        stmt.close();
        conn.close();
    }

    /**
     * Configura la tabla de productos
     */
/**
 * Configura la tabla de productos
 */
private void configurarTablaProductos() {
    // Configurar la columna checkbox
    table_produtos.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
    table_produtos.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(value != null && (Boolean) value);
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            return checkBox;
        }
    });

    // Configurar anchos de columna
    if (table_produtos.getColumnCount() >= 7) {
        table_produtos.getColumnModel().getColumn(0).setPreferredWidth(60);   
        table_produtos.getColumnModel().getColumn(1).setPreferredWidth(350);  
        table_produtos.getColumnModel().getColumn(2).setPreferredWidth(80);   
        table_produtos.getColumnModel().getColumn(3).setPreferredWidth(80);   
        table_produtos.getColumnModel().getColumn(4).setPreferredWidth(80);   
        table_produtos.getColumnModel().getColumn(5).setPreferredWidth(140);  
        table_produtos.getColumnModel().getColumn(6).setPreferredWidth(140);  
    }

    // Renderer para centrar números
    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    if (table_produtos.getColumnCount() >= 5) {
        table_produtos.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table_produtos.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table_produtos.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
    }

    // Tooltip para Estantería
    if (table_produtos.getColumnCount() >= 6) {
        javax.swing.table.DefaultTableCellRenderer shelfRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof javax.swing.JLabel) {
                    String txt = value != null ? String.valueOf(value) : "";
                    ((javax.swing.JLabel) c).setToolTipText(txt.isEmpty() ? null : "Ubicación: " + txt);
                }
                return c;
            }
        };
        table_produtos.getColumnModel().getColumn(5).setCellRenderer(shelfRenderer);
    }
    
    // CONFIGURAR LISTENER DE TABLA - VERSIÓN CORREGIDA
    table_produtos.getModel().addTableModelListener(e -> {
        if (e.getColumn() == 0 || e.getColumn() == 4) { // Checkbox o Cantidad
            // Usar SwingUtilities.invokeLater para asegurar que el cambio se procese
            javax.swing.SwingUtilities.invokeLater(() -> {
                actualizarContadores();
                validarSeleccionTodos();
            });
        }
    });
    
    // LISTENER ADICIONAL PARA CLICKS EN LA TABLA
    table_produtos.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            int column = table_produtos.columnAtPoint(evt.getPoint());
            if (column == 0) { // Si hicieron click en la columna de checkbox
                javax.swing.SwingUtilities.invokeLater(() -> {
                    actualizarContadores();
                    validarSeleccionTodos();
                });
            }
        }
    });
    
    System.out.println("DEBUG: Listener de tabla configurado correctamente");
}
// ===== MÉTODO ADICIONAL PARA DEBUG =====
/**
 * Método para debuggear el estado de la tabla
 */
private void debugTablaEstado() {
    DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();
    System.out.println("=== DEBUG TABLA ===");
    System.out.println("Filas: " + model.getRowCount());
    System.out.println("Columnas: " + model.getColumnCount());
    
    for (int i = 0; i < model.getRowCount(); i++) {
        Boolean selected = (Boolean) model.getValueAt(i, 0);
        System.out.println("Fila " + i + " - Seleccionado: " + selected);
    }
    System.out.println("==================");
}
// ===== REEMPLAZAR EL MÉTODO actualizarContadores() PARA AGREGAR DEBUG =====
private void actualizarContadores() {
    DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();
    int totalProductos = model.getRowCount();
    int productosSeleccionados = 0;
    int totalUnidades = 0;

    for (int i = 0; i < totalProductos; i++) {
        Boolean seleccionado = (Boolean) model.getValueAt(i, 0);
        if (seleccionado != null && seleccionado) {
            productosSeleccionados++;
            Object cantidadObj = model.getValueAt(i, 4); // Columna "A Enviar"
            if (cantidadObj != null) {
                totalUnidades += (Integer) cantidadObj;
            }
        }
    }

    txtCantidSeleccion.setText(productosSeleccionados + " de " + totalProductos + " productos seleccionados");
    txtSeleccionados.setText(productosSeleccionados + " productos seleccionados");
    txtTotalUnidades.setText("Total a enviar: " + totalUnidades + " unidades");

    // Habilitar/deshabilitar botón de envío
    boolean habilitar = productosSeleccionados > 0;
    btnConfirmarEnvio.setEnabled(habilitar);
    
    // DEBUG
    System.out.println("DEBUG: Productos seleccionados: " + productosSeleccionados + 
                      " - Botón habilitado: " + habilitar);
}

// ===== MÉTODO ALTERNATIVO SI EL PROBLEMA PERSISTE =====
/**
 * Método alternativo para detectar cambios en checkboxes
 * Usar SOLO si el TableModelListener no funciona
 */
private void configurarListenerAlternativo() {
    // Configurar listener en cada checkbox individualmente
    DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();
    
    // Usar un timer para verificar cambios cada 500ms (solo para debug)
    javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
        actualizarContadores();
        validarSeleccionTodos();
    });
    
    // Solo activar si es necesario para debug
    // timer.start(); // Descomentar solo si es necesario
}

// ===== MÉTODO PARA FORZAR ACTUALIZACIÓN MANUAL =====
/**
 * Método para forzar actualización cuando se hace clic en tabla
 */
private void configurarClickListener() {
    table_produtos.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            // Pequeña pausa para que el checkbox se actualice
            javax.swing.SwingUtilities.invokeLater(() -> {
                actualizarContadores();
                validarSeleccionTodos();
            });
        }
    });
}



    // ===== MODIFICAR EL MÉTODO interfaz() EXISTENTE AGREGANDO ESTAS LÍNEAS =====
    public void interfaz() {
        panelTitulo.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
        panelDescripcionBodegas.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
        panelDatosEnvio.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
        panelMensajeAlerta.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
        panelBotonera.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
        panelProductos.putClientProperty(FlatClientProperties.STYLE, CONTAINER);
        txtFechaHoraEnvio.putClientProperty(FlatClientProperties.STYLE, "background:lighten($Menu.background,25%)");

        // AGREGAR ESTAS LÍNEAS:
        // Configurar fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        txtFechaHoraEnvio.setText(sdf.format(new Date()));
        txtFechaHoraEnvio.setEditable(false);

        // Deshabilitar botón inicialmente
        btnConfirmarEnvio.setEnabled(false);
    }
    
    

    public void cargarIcons() {
        btnOrigne.setIcon(iconBodega);
        btnDesti.setIcon(iconBodegaD);
        btnFecha.setIcon(iconCdr);
        btnStatus.setIcon(iconCheck);
        txtTItulo1.setIcon(iconTit1);
        txtTit2.setIcon(iconBook);
        txtTit3.setIcon(box);
        btnOrigne.putClientProperty(FlatClientProperties.STYLE, PANEL);
        btnDesti.putClientProperty(FlatClientProperties.STYLE, PANEL);
        btnFecha.putClientProperty(FlatClientProperties.STYLE, PANEL);
        btnStatus.putClientProperty(FlatClientProperties.STYLE, PANEL);

    }

    /**
     * Valida los datos del formulario antes del envío
     */
    private boolean validarDatos() {
        StringBuilder errores = new StringBuilder();

        DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();
        boolean hayProductosSeleccionados = false;

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                hayProductosSeleccionados = true;

                Object cantidadObj = model.getValueAt(i, 4);
                Object pendienteObj = model.getValueAt(i, 3);

                if (cantidadObj == null || (Integer) cantidadObj <= 0) {
                    errores.append("- La cantidad debe ser mayor a 0 para todos los productos seleccionados\n");
                    break;
                } else if ((Integer) cantidadObj > (Integer) pendienteObj) {
                    errores.append("- La cantidad no puede ser mayor a la pendiente\n");
                    break;
                }
            }
        }

        if (!hayProductosSeleccionados) {
            errores.append("- Debe seleccionar al menos un producto para enviar\n");
        }

        if (errores.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "Por favor corrija los siguientes errores:\n\n" + errores.toString(),
                    "Errores de validación",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * Procesa el envío del traspaso
     */
    private void procesarEnvio() {
        if (!validarDatos()) {
            return;
        }

        // Confirmar envío
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de procesar el envío del traspaso " + numeroTraspaso + "?\n"
                + "Esta acción no se puede deshacer.",
                "Confirmar envío",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        // Deshabilitar botón y mostrar progreso
        btnConfirmarEnvio.setEnabled(false);
        btnConfirmarEnvio.setText("Procesando...");

        try {
            // Recopilar productos seleccionados
            recopilarProductosSeleccionados();

            // Actualizar base de datos
            actualizarBaseDatos();

            // Mostrar mensaje de éxito
            JOptionPane.showMessageDialog(this,
                    "Traspaso enviado exitosamente.",
                    "Envío exitoso",
                    JOptionPane.INFORMATION_MESSAGE);

            // Notificar al callback
            if (callback != null) {
                callback.onEnvioExitoso(numeroTraspaso, productosSeleccionados);
            }

            // Cerrar el dialog padre
            javax.swing.SwingUtilities.getWindowAncestor(this).dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error procesando el envío: " + e.getMessage());

            // Restaurar botón
            btnConfirmarEnvio.setEnabled(true);
            btnConfirmarEnvio.setText("Confirmar envío");
        }
    }

    /**
     * Cancela el envío
     */
    private void cancelarEnvio() {
        // Confirmar cancelación si hay cambios
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de cancelar el envío?\nSe perderán todos los datos ingresados.",
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            if (callback != null) {
                callback.onEnvioCancelado();
            }
            javax.swing.SwingUtilities.getWindowAncestor(this).dispose();
        }
    }

    /**
     * Recopila los productos seleccionados con sus cantidades
     */
    private void recopilarProductosSeleccionados() {
        productosSeleccionados.clear();
        DefaultTableModel model = (DefaultTableModel) table_produtos.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                Map<String, Object> productoOriginal = productosTraspaso.get(i);
                Map<String, Object> productoEnviado = new HashMap<>(productoOriginal);

                // Actualizar con datos del envío
                productoEnviado.put("cantidad_envio", model.getValueAt(i, 4));
                productoEnviado.put("observaciones_envio", model.getValueAt(i, 5));

                productosSeleccionados.add(productoEnviado);
            }
        }
    }

    /**
     * Actualiza la base de datos con el envío
     */

   private void actualizarBaseDatos() throws SQLException {
       Connection conn = conexion.getInstance().createConnection();
       conn.setAutoCommit(false);

       try {
           System.out.println("INFO Iniciando actualización de base de datos para envío...");

           // 1. Actualizar estado del traspaso principal
           String sqlTraspaso = "UPDATE traspasos SET "
               + "estado = 'en_transito', "
               + "fecha_envio = NOW(), "
               + "observaciones = CONCAT(COALESCE(observaciones, ''), ?) "
               + "WHERE numero_traspaso = ?";

           PreparedStatement stmtTraspaso = conn.prepareStatement(sqlTraspaso);
           String obsEnvio = txtObservacion.getText().trim();
           stmtTraspaso.setString(1, obsEnvio.isEmpty() ? "" : " - Envío: " + obsEnvio);
           stmtTraspaso.setString(2, numeroTraspaso);
           stmtTraspaso.executeUpdate();
           stmtTraspaso.close();

           System.out.println("SUCCESS Traspaso actualizado a EN_TRANSITO");

          // 2. Obtener IDs de bodegas
          Integer idBodegaOrigen = null;
          Integer idBodegaDestino = null;
          String sqlBodega = "SELECT id_bodega_origen, id_bodega_destino FROM traspasos WHERE numero_traspaso = ?";
          PreparedStatement stmtBodega = conn.prepareStatement(sqlBodega);
          stmtBodega.setString(1, numeroTraspaso);
          ResultSet rsBodega = stmtBodega.executeQuery();

          if (rsBodega.next()) {
              idBodegaOrigen = rsBodega.getInt("id_bodega_origen");
              idBodegaDestino = rsBodega.getInt("id_bodega_destino");
              System.out.println("INFO Bodega origen ID: " + idBodegaOrigen + ", destino ID: " + idBodegaDestino);
          }
          rsBodega.close();
          stmtBodega.close();

          if (idBodegaOrigen == null) {
              throw new SQLException("No se pudo obtener la bodega origen");
          }
          if (idBodegaDestino == null) {
              throw new SQLException("No se pudo obtener la bodega destino");
          }

           // 3. Actualizar detalles del traspaso Y descontar inventario
          String sqlDetalle = "UPDATE traspaso_detalles SET "
              + "cantidad_enviada = COALESCE(cantidad_enviada, 0) + ?, "
              + "estado_detalle = CASE "
              + "    WHEN (COALESCE(cantidad_enviada, 0) + ?) >= cantidad_solicitada THEN 'enviado' "
              + "    ELSE 'pendiente' "
              + "END, "
              + "observaciones = CONCAT(COALESCE(observaciones, ''), ?) "
              + "WHERE id_detalle_traspaso = ?";

           PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle);

           // 4. Preparar statements para actualizar inventario
           // CRÍTICO: Descontar de bodega ORIGEN
          String sqlDescontarInventarioPar = 
              "UPDATE inventario_bodega SET "
              + "Stock_par = Stock_par - ?, "
              + "fecha_ultimo_movimiento = NOW() "
              + "WHERE id_bodega = ? AND id_variante = ? AND activo = 1 AND Stock_par >= ?";
          String sqlDescontarInventarioCaja = 
              "UPDATE inventario_bodega SET "
              + "Stock_caja = Stock_caja - ?, "
              + "fecha_ultimo_movimiento = NOW() "
              + "WHERE id_bodega = ? AND id_variante = ? AND activo = 1 AND Stock_caja >= ?";

          PreparedStatement stmtDescontarPar = conn.prepareStatement(sqlDescontarInventarioPar);
          PreparedStatement stmtDescontarCaja = conn.prepareStatement(sqlDescontarInventarioCaja);

           // 5. Registrar movimientos de inventario
         String sqlMovimiento = 
             "INSERT INTO inventario_movimientos "
             + "(id_producto, id_variante, tipo_movimiento, cantidad, fecha_movimiento, "
             + "id_referencia, tipo_referencia, id_usuario, observaciones) "
             + "VALUES (?, ?, ?, ?, CURDATE(), ?, 'traspaso', ?, ?)";

           PreparedStatement stmtMovimiento = conn.prepareStatement(sqlMovimiento);
          String sqlUpsertDestinoPar = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, activo, fecha_ultimo_movimiento) " +
              "VALUES (?,?,?,?,1,NOW()) ON DUPLICATE KEY UPDATE Stock_par = Stock_par + VALUES(Stock_par), activo=1, fecha_ultimo_movimiento = NOW()";
          String sqlUpsertDestinoCaja = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, activo, fecha_ultimo_movimiento) " +
              "VALUES (?,?,?,?,1,NOW()) ON DUPLICATE KEY UPDATE Stock_caja = Stock_caja + VALUES(Stock_caja), activo=1, fecha_ultimo_movimiento = NOW()";
          PreparedStatement pstUpsertPar = conn.prepareStatement(sqlUpsertDestinoPar);
          PreparedStatement pstUpsertCaja = conn.prepareStatement(sqlUpsertDestinoCaja);
         String sqlMovimientoDestino = 
             "INSERT INTO inventario_movimientos "
             + "(id_producto, id_variante, tipo_movimiento, cantidad, fecha_movimiento, "
             + "id_referencia, tipo_referencia, id_usuario, observaciones) "
             + "VALUES (?, ?, ?, ?, CURDATE(), ?, 'traspaso', ?, ?)";
          PreparedStatement stmtMovimientoDestino = conn.prepareStatement(sqlMovimientoDestino);

           // 6. Procesar cada producto seleccionado
           for (Map<String, Object> producto : productosSeleccionados) {
               int cantidadEnvio = (Integer) producto.get("cantidad_envio");
               String observaciones = (String) producto.get("observaciones_envio");
               int idDetalle = (Integer) producto.get("id_detalle");
               int idProducto = (Integer) producto.get("id_producto");
               Object idVarianteObj = producto.get("id_variante");

               System.out.println("INFO Procesando producto ID: " + idProducto + 
                                ", Variante: " + idVarianteObj + 
                                ", Cantidad: " + cantidadEnvio);

               // 6.1 Actualizar detalle del traspaso
               stmtDetalle.setInt(1, cantidadEnvio);
               stmtDetalle.setInt(2, cantidadEnvio);
               stmtDetalle.setString(3, observaciones != null && !observaciones.trim().isEmpty() 
                   ? " - Envío: " + observaciones : "");
               stmtDetalle.setInt(4, idDetalle);
               stmtDetalle.addBatch();

               // 6.2 CRÍTICO: Descontar del inventario de bodega ORIGEN
              if (idVarianteObj != null) {
                  int idVariante = (Integer) idVarianteObj;

                   // Verificar stock disponible antes de descontar
              String tipo = (String) producto.get("tipo");
              boolean esCaja = tipo != null && tipo.equalsIgnoreCase("caja");

              String sqlVerificarStock = 
                  "SELECT COALESCE(SUM(" + (esCaja ? "Stock_caja" : "Stock_par") + "),0) AS stock FROM inventario_bodega "
                  + "WHERE id_bodega = ? AND id_variante = ? AND activo = 1 FOR UPDATE";

                   PreparedStatement stmtVerificar = conn.prepareStatement(sqlVerificarStock);
                   stmtVerificar.setInt(1, idBodegaOrigen);
                   stmtVerificar.setInt(2, idVariante);
                   ResultSet rsStock = stmtVerificar.executeQuery();

              int stockActual = 0;
              if (rsStock.next()) {
                  stockActual = rsStock.getInt("stock");
              }
              rsStock.close();
              stmtVerificar.close();

              String sqlExiste = "SELECT COUNT(*) FROM inventario_bodega WHERE id_bodega = ? AND id_variante = ?";
              PreparedStatement stmtExiste = conn.prepareStatement(sqlExiste);
              stmtExiste.setInt(1, idBodegaOrigen);
              stmtExiste.setInt(2, idVariante);
              ResultSet rsExiste = stmtExiste.executeQuery();
              int existeCount = 0;
              if (rsExiste.next()) existeCount = rsExiste.getInt(1);
              rsExiste.close();
              stmtExiste.close();

              if (existeCount == 0) {
                  String sqlInsertInv = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, activo, fecha_ultimo_movimiento) " +
                      "VALUES (?, ?, 0, 0, 1, NOW())";
                  PreparedStatement stmtInsertInv = conn.prepareStatement(sqlInsertInv);
                  stmtInsertInv.setInt(1, idBodegaOrigen);
                  stmtInsertInv.setInt(2, idVariante);
                  stmtInsertInv.executeUpdate();
                  stmtInsertInv.close();
                  stockActual = 0;
              }

                   System.out.println("   Stock actual en bodega origen: " + stockActual);

              if (stockActual < cantidadEnvio) {
                  throw new SQLException(
                      "Stock insuficiente en bodega origen. " +
                      "Producto ID: " + idProducto + 
                      ", Variante: " + idVariante + 
                      ", Stock disponible: " + stockActual + 
                      ", Cantidad a enviar: " + cantidadEnvio
                  );
              }

              int updated;
              if (esCaja) {
                  stmtDescontarCaja.setInt(1, cantidadEnvio);
                  stmtDescontarCaja.setInt(2, idBodegaOrigen);
                  stmtDescontarCaja.setInt(3, idVariante);
                  stmtDescontarCaja.setInt(4, cantidadEnvio);
                  updated = stmtDescontarCaja.executeUpdate();
              } else {
                  stmtDescontarPar.setInt(1, cantidadEnvio);
                  stmtDescontarPar.setInt(2, idBodegaOrigen);
                  stmtDescontarPar.setInt(3, idVariante);
                  stmtDescontarPar.setInt(4, cantidadEnvio);
                  updated = stmtDescontarPar.executeUpdate();
              }
              if (updated == 0) {
                  throw new SQLException(
                      "Stock insuficiente (concurrencia) en bodega origen. Producto ID: " + idProducto +
                      ", Variante: " + idVariante +
                      ", Intento de enviar: " + cantidadEnvio
                  );
              }

              System.out.println("   SUCCESS Stock descontado: -" + cantidadEnvio);

              // ===== CORREGIDO: NO sumar a destino aquí =====
              // El stock de destino se suma cuando se RECIBE, no cuando se ENVÍA
              // Esto evita duplicación (antes sumaba aquí + al recibir = DOBLE)
              /*
              // CÓDIGO COMENTADO - Causaba duplicación
              if (esCaja) {
                  pstUpsertCaja.setInt(1, idBodegaDestino);
                  pstUpsertCaja.setInt(2, idVariante);
                  pstUpsertCaja.setInt(3, 0);
                  pstUpsertCaja.setInt(4, cantidadEnvio);
                  pstUpsertCaja.executeUpdate();
              } else {
                  pstUpsertPar.setInt(1, idBodegaDestino);
                  pstUpsertPar.setInt(2, idVariante);
                  pstUpsertPar.setInt(3, cantidadEnvio);
                  pstUpsertPar.setInt(4, 0);
                  pstUpsertPar.executeUpdate();
              }
              */
              // ================================================

              // Registrar movimiento de inventario en ORIGEN
              String tipoMovimientoOrigen = esCaja ? "salida caja" : "salida par";
              stmtMovimiento.setInt(1, idProducto);
              stmtMovimiento.setInt(2, idVariante);
              stmtMovimiento.setString(3, tipoMovimientoOrigen);
              stmtMovimiento.setInt(4, cantidadEnvio);
              stmtMovimiento.setInt(5, idBodegaOrigen);

                   // Obtener usuario actual
                   Integer idUsuario = null;
                   try {
                       raven.clases.admin.UserSession userSession = 
                           raven.clases.admin.UserSession.getInstance();
                       if (userSession != null && userSession.getCurrentUser() != null) {
                  idUsuario = userSession.getCurrentUser().getIdUsuario();
                       }
                   } catch (Exception e) {
                       System.out.println("WARNING No se pudo obtener usuario actual");
                   }

                   if (idUsuario != null) {
                  stmtMovimiento.setInt(6, idUsuario);
                  } else {
                      stmtMovimiento.setNull(6, java.sql.Types.INTEGER);
                  }

             stmtMovimiento.setString(7, 
                 "Salida por envío de traspaso: " + numeroTraspaso);
             stmtMovimiento.addBatch();

              // Registrar movimiento de inventario en DESTINO
              String tipoMovimientoDestino = esCaja ? "entrada caja" : "entrada par";
              stmtMovimientoDestino.setInt(1, idProducto);
              stmtMovimientoDestino.setInt(2, idVariante);
              stmtMovimientoDestino.setString(3, tipoMovimientoDestino);
              stmtMovimientoDestino.setInt(4, cantidadEnvio);
              stmtMovimientoDestino.setInt(5, idBodegaDestino);
              if (idUsuario != null) {
                  stmtMovimientoDestino.setInt(6, idUsuario);
              } else {
                  stmtMovimientoDestino.setNull(6, java.sql.Types.INTEGER);
              }
              stmtMovimientoDestino.setString(7, "Entrada por envío de traspaso: " + numeroTraspaso);
              stmtMovimientoDestino.addBatch();

               } else {
                   System.out.println("   WARNING Producto sin variante, no se descuenta inventario");
               }
           }

           // 7. Ejecutar todos los updates en lote
           System.out.println("SAVE Ejecutando actualizaciones en lote...");

           stmtDetalle.executeBatch();
           System.out.println("   SUCCESS Detalles actualizados");

          System.out.println("   SUCCESS Inventarios descontados (envío)");

          stmtMovimiento.executeBatch();
          stmtMovimientoDestino.executeBatch();
          System.out.println("   SUCCESS Movimientos registrados");

           // Cerrar statements
           stmtDetalle.close();
          stmtDescontarPar.close();
          stmtDescontarCaja.close();
          stmtMovimiento.close();
          stmtMovimientoDestino.close();
          pstUpsertPar.close();
          pstUpsertCaja.close();

           // 8. Confirmar transacción
           conn.commit();
           System.out.println("SUCCESS Transacción completada exitosamente");

       } catch (SQLException e) {
           System.err.println("ERROR Error en actualización de base de datos: " + e.getMessage());
           e.printStackTrace();
           conn.rollback();
           throw e;
       } finally {
           conn.setAutoCommit(true);
           conn.close();
       }
   }

    /**
     * Muestra un mensaje de error
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

// Modificar el método estático enviarTraspaso para mostrar el panel EnvioTraspaso
    public static void enviarTraspaso(java.awt.Frame parent, String numeroTraspaso,
            EnvioTraspaso.EnvioCallback callback) {

        // Verificar que el traspaso esté en estado autorizado
        try {
            if (!verificarEstadoTraspaso(numeroTraspaso)) {
                JOptionPane.showMessageDialog(parent,
                        "El traspaso debe estar en estado AUTORIZADO para poder enviarlo.",
                        "Estado incorrecto",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Configurar el callback - convertir el callback de EnvioTraspaso a ModalEnvioTraspaso
            ModalEnvioTraspasoMejorado.EnvioCallback adaptedCallback = new ModalEnvioTraspasoMejorado.EnvioCallback() {
                @Override
                public void onEnvioExitoso(String numeroTraspaso, List<Map<String, Object>> productosEnviados) {
                    callback.onEnvioExitoso(numeroTraspaso, productosEnviados);
                }

                @Override
                public void onEnvioCancelado() {
                    callback.onEnvioCancelado();
                }
            };

            // Mostrar el modal consistente
            ModalEnvioTraspasoMejorado.mostrarModal(parent, numeroTraspaso, adaptedCallback);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error verificando estado del traspaso: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
// Agregar método para configurar el traspaso en el panel

    private static boolean verificarEstadoTraspaso(String numeroTraspaso) throws SQLException {
        String sql = "SELECT estado FROM traspasos WHERE numero_traspaso = ?";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, numeroTraspaso);
        ResultSet rs = stmt.executeQuery();

        boolean esValido = false;
        if (rs.next()) {
            String estado = rs.getString("estado");
            esValido = "autorizado".equalsIgnoreCase(estado);
        }

        rs.close();
        stmt.close();
        conn.close();

        return esValido;
    }

    public void setCallback(ModalEnvioTraspasoMejorado.EnvioCallback callback) {
        this.callback = callback;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelMain = new javax.swing.JPanel();
        panelTitulo = new javax.swing.JPanel();
        txtTItulo1 = new javax.swing.JLabel();
        txtNumeroTraspaso = new javax.swing.JLabel();
        panelDescripcionBodegas = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        btnOrigne = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        txtBodegaOrigen = new javax.swing.JLabel();
        panelMensajeAlerta = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        panelBotonera = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        txtSeleccionados = new javax.swing.JLabel();
        txtTotalUnidades = new javax.swing.JLabel();
        btnCancel = new javax.swing.JButton();
        btnConfirmarEnvio = new javax.swing.JButton();
        panelProductos = new javax.swing.JPanel();
        txtTit3 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        cb_seleccionarTodos = new javax.swing.JCheckBox();
        txtCantidSeleccion = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        table_produtos = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        btnDesti = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        txtBodegaDestino = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        btnFecha = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        txtFechaSolicitud = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        btnStatus = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        txtEstado = new javax.swing.JLabel();
        panelDatosEnvio = new javax.swing.JPanel();
        txtTit2 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        txtFechaHoraEnvio = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtObservacion = new javax.swing.JTextArea();

        setMaximumSize(new java.awt.Dimension(1200, 900));
        setPreferredSize(new java.awt.Dimension(1210, 900));

        panelMain.setPreferredSize(new java.awt.Dimension(1200, 900));

        panelTitulo.setBackground(new java.awt.Color(255, 204, 204));

        txtTItulo1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        txtTItulo1.setText("Enviar traspaso");

        txtNumeroTraspaso.setText("TR000014 - Confirmar envío de productos ");

        javax.swing.GroupLayout panelTituloLayout = new javax.swing.GroupLayout(panelTitulo);
        panelTitulo.setLayout(panelTituloLayout);
        panelTituloLayout.setHorizontalGroup(
            panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTituloLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtTItulo1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtNumeroTraspaso, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelTituloLayout.setVerticalGroup(
            panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTituloLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtTItulo1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNumeroTraspaso)
                .addGap(12, 12, 12))
        );

        panelDescripcionBodegas.setBackground(new java.awt.Color(255, 204, 204));

        btnOrigne.setEnabled(false);
        btnOrigne.setFocusPainted(false);
        btnOrigne.setFocusable(false);
        btnOrigne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOrigneActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 8)); // NOI18N
        jLabel3.setText("Origen");

        txtBodegaOrigen.setText("Bodega 50años ");

        panelMensajeAlerta.setBackground(new java.awt.Color(255, 204, 204));

        jLabel17.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("Confirmación requerida: Una vez enviado el traspaso, se actualizará el estado a \"EN_TRANSITO\" y se registrará la fecha de envío automáticamente. ");

        javax.swing.GroupLayout panelMensajeAlertaLayout = new javax.swing.GroupLayout(panelMensajeAlerta);
        panelMensajeAlerta.setLayout(panelMensajeAlertaLayout);
        panelMensajeAlertaLayout.setHorizontalGroup(
            panelMensajeAlertaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMensajeAlertaLayout.createSequentialGroup()
                .addGap(161, 161, 161)
                .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 824, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelMensajeAlertaLayout.setVerticalGroup(
            panelMensajeAlertaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMensajeAlertaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel17)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        panelBotonera.setBackground(new java.awt.Color(255, 204, 204));

        txtSeleccionados.setText("0 productos seleccionados ");

        txtTotalUnidades.setText("Total a enviar: 0 unidades ");

        btnCancel.setText("X Cancelar");

        btnConfirmarEnvio.setText("Confirmar envio");
        btnConfirmarEnvio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarEnvioActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBotoneraLayout = new javax.swing.GroupLayout(panelBotonera);
        panelBotonera.setLayout(panelBotoneraLayout);
        panelBotoneraLayout.setHorizontalGroup(
            panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotoneraLayout.createSequentialGroup()
                .addGroup(panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBotoneraLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel16))
                    .addGroup(panelBotoneraLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(txtSeleccionados)
                        .addGap(69, 69, 69)
                        .addComponent(txtTotalUnidades)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 499, Short.MAX_VALUE)
                .addComponent(btnCancel)
                .addGap(18, 18, 18)
                .addComponent(btnConfirmarEnvio)
                .addGap(17, 17, 17))
        );
        panelBotoneraLayout.setVerticalGroup(
            panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotoneraLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBotoneraLayout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(btnCancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(panelBotoneraLayout.createSequentialGroup()
                        .addGroup(panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnConfirmarEnvio, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelBotoneraLayout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtTotalUnidades)
                                    .addComponent(txtSeleccionados))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelProductos.setBackground(new java.awt.Color(255, 204, 204));

        txtTit3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        txtTit3.setText("Productos a Enviar ");

        cb_seleccionarTodos.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        cb_seleccionarTodos.setText("Seleccionar todos los productos");

        txtCantidSeleccion.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        txtCantidSeleccion.setText("0 de 3 productos seleccionados ");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(cb_seleccionarTodos, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtCantidSeleccion)
                .addGap(75, 75, 75))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(cb_seleccionarTodos, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                .addComponent(txtCantidSeleccion))
        );

        table_produtos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Seleccionar", "Producto", "Solicitado", "pendiente", "Cantidad", "Observaciones"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane3.setViewportView(table_produtos);

        javax.swing.GroupLayout panelProductosLayout = new javax.swing.GroupLayout(panelProductos);
        panelProductos.setLayout(panelProductosLayout);
        panelProductosLayout.setHorizontalGroup(
            panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProductosLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtTit3, javax.swing.GroupLayout.PREFERRED_SIZE, 431, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1067, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelProductosLayout.setVerticalGroup(
            panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProductosLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(txtTit3, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnOrigne, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtBodegaOrigen, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelMensajeAlerta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelProductos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnOrigne, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtBodegaOrigen))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(panelProductos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(panelMensajeAlerta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(panelBotonera, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        btnDesti.setEnabled(false);
        btnDesti.setFocusPainted(false);
        btnDesti.setFocusable(false);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 8)); // NOI18N
        jLabel5.setText("Desitno");

        txtBodegaDestino.setText("Bodega 50años ");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnDesti, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtBodegaDestino, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE))
                .addGap(0, 64, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnDesti, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtBodegaDestino)))
                .addContainerGap())
        );

        btnFecha.setEnabled(false);
        btnFecha.setFocusPainted(false);
        btnFecha.setFocusable(false);
        btnFecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFechaActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 8)); // NOI18N
        jLabel7.setText("Fecha Solicitud ");

        txtFechaSolicitud.setText("2025-08-19 19:14  ");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtFechaSolicitud, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE))
                .addGap(0, 64, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnFecha, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtFechaSolicitud)))
                .addContainerGap())
        );

        btnStatus.setEnabled(false);
        btnStatus.setFocusPainted(false);
        btnStatus.setFocusable(false);

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 8)); // NOI18N
        jLabel9.setText("Estado ");

        txtEstado.setText("AUTORIZADO  ");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtEstado, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE))
                .addGap(0, 64, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtEstado)))
                .addContainerGap())
        );

        javax.swing.GroupLayout panelDescripcionBodegasLayout = new javax.swing.GroupLayout(panelDescripcionBodegas);
        panelDescripcionBodegas.setLayout(panelDescripcionBodegasLayout);
        panelDescripcionBodegasLayout.setHorizontalGroup(
            panelDescripcionBodegasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDescripcionBodegasLayout.createSequentialGroup()
                .addGap(127, 127, 127)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(85, Short.MAX_VALUE))
        );
        panelDescripcionBodegasLayout.setVerticalGroup(
            panelDescripcionBodegasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelDescripcionBodegasLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(panelDescripcionBodegasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10))
        );

        panelDatosEnvio.setBackground(new java.awt.Color(255, 204, 204));

        txtTit2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        txtTit2.setText(" Datos de Envío ");

        jLabel12.setText("Fecha y hora de envio");

        jLabel13.setText("Observaciones de envio");

        txtObservacion.setColumns(20);
        txtObservacion.setRows(5);
        jScrollPane2.setViewportView(txtObservacion);

        javax.swing.GroupLayout panelDatosEnvioLayout = new javax.swing.GroupLayout(panelDatosEnvio);
        panelDatosEnvio.setLayout(panelDatosEnvioLayout);
        panelDatosEnvioLayout.setHorizontalGroup(
            panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDatosEnvioLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(txtTit2, javax.swing.GroupLayout.PREFERRED_SIZE, 431, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFechaHoraEnvio, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE))
                .addGap(347, 347, 347))
        );
        panelDatosEnvioLayout.setVerticalGroup(
            panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDatosEnvioLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelDatosEnvioLayout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelDatosEnvioLayout.createSequentialGroup()
                        .addComponent(txtTit2, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFechaHoraEnvio, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelDatosEnvio, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelDescripcionBodegas, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelTitulo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        panelMainLayout.setVerticalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(panelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(panelDescripcionBodegas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(panelDatosEnvio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE, 1158, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 548, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnFechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFechaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnFechaActionPerformed

    private void btnConfirmarEnvioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarEnvioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnConfirmarEnvioActionPerformed

    private void btnOrigneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOrigneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnOrigneActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnConfirmarEnvio;
    private javax.swing.JButton btnDesti;
    private javax.swing.JButton btnFecha;
    private javax.swing.JButton btnOrigne;
    private javax.swing.JButton btnStatus;
    private javax.swing.JCheckBox cb_seleccionarTodos;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel panelBotonera;
    private javax.swing.JPanel panelDatosEnvio;
    private javax.swing.JPanel panelDescripcionBodegas;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelMensajeAlerta;
    private javax.swing.JPanel panelProductos;
    private javax.swing.JPanel panelTitulo;
    private javax.swing.JTable table_produtos;
    private javax.swing.JLabel txtBodegaDestino;
    private javax.swing.JLabel txtBodegaOrigen;
    private javax.swing.JLabel txtCantidSeleccion;
    private javax.swing.JLabel txtEstado;
    private javax.swing.JTextField txtFechaHoraEnvio;
    private javax.swing.JLabel txtFechaSolicitud;
    private javax.swing.JLabel txtNumeroTraspaso;
    private javax.swing.JTextArea txtObservacion;
    private javax.swing.JLabel txtSeleccionados;
    private javax.swing.JLabel txtTItulo1;
    private javax.swing.JLabel txtTit2;
    private javax.swing.JLabel txtTit3;
    private javax.swing.JLabel txtTotalUnidades;
    // End of variables declaration//GEN-END:variables
}

  

