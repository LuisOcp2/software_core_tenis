package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import raven.cell.TableActionEvent;
import raven.cell.TableActionCellEditor;
import raven.clases.comercial.ServiceCliente;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.cell.TableActionCellRender;

import raven.application.Application;
import raven.application.form.principal.generarVentaFor1;
import raven.clases.admin.UserSession;
import raven.clases.comercial.OrdenReservaService;
import raven.clases.reportes.ReporteVentas;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.principal.conexion;
import raven.modal.Toast;

public class Carrito extends javax.swing.JPanel {

    LocalDate fechaZona = LocalDate.now(ZoneId.of("America/Bogota"));
    private static final Logger LOGGER = Logger.getLogger(ReporteVentas.class.getName());
    private static final String PANEL_STYLE = "arc:25;background:$Login.background;";
    private static final String FONT_HEADER_STYLE = "font:$h1.font";
    private static final String FONT_SUBHEADER_STYLE = "font:$h2.font";
    private final ReporteVentas reporteVentas;
    private final ServiceCliente serviceCliente;
    private final OrdenReservaService ordenReservaService;
    private final DecimalFormat formatoMoneda;
    // ICONOS....................................................................................................
    private final FontIcon filter;
    private final FontIcon reiniciar;

    // ID de referencia para seleccionar una orden inicial (por notificación)
    private Integer idReferenciaSeleccionInicial;

    public Carrito() throws SQLException {
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        this.reporteVentas = new ReporteVentas();
        this.serviceCliente = new ServiceCliente();
        this.ordenReservaService = new OrdenReservaService();
        filter = createColoredIcon(FontAwesomeSolid.FILTER, tabTextColor);
        reiniciar = createColoredIcon(FontAwesomeSolid.REDO, tabTextColor);
        // Configurar formato para valores monetarios
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        this.formatoMoneda = new DecimalFormat("#,###", symbols);
        // Inicializar componentes
        initComponents();
        btnFiltrar.setIcon(filter);
        btnReiniciar.setIcon(reiniciar);
        configurarUI();
        configurarTabla();
        cargarDatosIniciales();
        txtFecha.setText(fechaZona.toString());
    }

    // Constructor adicional que permite recibir un id de referencia
    public Carrito(Integer idReferencia) throws SQLException {
        this();
        this.idReferenciaSeleccionInicial = idReferencia;
        aplicarSeleccionInicial();
    }

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    private void configurarUI() {
        // Configurar fuentes
        FlatRobotoFont.install();
        lb.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // Configurar estilos de paneles
        aplicarEstiloPaneles();

        // Configurar etiquetas de título
        configurarEstiloEtiquetas();

        // Configurar tabla
        configurarEstiloTabla();

        // Configurar fecha
        configurarSelectorFechas();
    }
    // Modificar el método configurarSelectorEstado

    /**
     * Configura el editor personalizado para la columna de estado. Implementa
     * restricciones para impedir la edición en estados específicos.
     *
     * @throws SQLException Si ocurre un error en la conexión con la base de
     *                      datos
     */
    private void configurarSelectorEstado() throws SQLException {

        try {
            // Establecemos conexión con la base de datos
            conexion.getInstance().connectToDatabase();
            try (Connection conexionBD = conexion.getInstance().createConnection()) {
                if (conexionBD == null) {
                    throw new SQLException("No se pudo establecer la conexión a la base de datos");
                }
            }

            // Definimos las opciones para los diferentes tipos de documentos
            final String[] opcionesEstadoCotizacion = { "cotizacion", "cotizacion_aprobada", "cotizacion_rechazada",
                    "cotizacion_convertida" };
            final String[] opcionesEstadoVenta = { "pendiente", "completada", "cancelada" };

            // Obtenemos la columna de estado (columna 3)
            TableColumn columnaEstado = jTable1.getColumnModel().getColumn(3);
            // Creamos un combobox personalizado
            final JComboBox<String> comboBoxEstado = new JComboBox<>();

            // Implementamos un editor personalizado para controlar la edición
            DefaultCellEditor editorEstado = new DefaultCellEditor(comboBoxEstado) {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value,
                        boolean isSelected, int row, int column) {
                    // Determinamos qué opciones mostrar según el tipo de documento
                    boolean esCotizacion = verificarSiEsCotizacion(row);

                    // Limpiamos el combobox y cargamos las opciones correspondientes
                    comboBoxEstado.removeAllItems();
                    if (esCotizacion) {
                        for (String estado : opcionesEstadoCotizacion) {
                            comboBoxEstado.addItem(estado);
                        }
                    } else {
                        for (String estado : opcionesEstadoVenta) {
                            comboBoxEstado.addItem(estado);
                        }
                    }

                    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                /**
                 * Sobreescribimos el método para controlar si una celda es
                 * editable. Verifica el estado actual de la fila antes de
                 * permitir la edición.
                 */
                @Override
                public boolean isCellEditable(EventObject anEvent) {
                    if (anEvent instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) anEvent;
                        int row = jTable1.rowAtPoint(me.getPoint());

                        // Solo permitimos editar si la fila no tiene un estado restrictivo
                        return isRowEditable(row) && super.isCellEditable(anEvent);
                    }
                    return super.isCellEditable(anEvent);
                }
            };

            // Agregamos el listener para manejar cambios de estado
            editorEstado.addCellEditorListener(new CellEditorListener() {
                // En la clase Carrito, dentro del CellEditorListener donde manejas los cambios
                // de estado:
                @Override
                public void editingStopped(ChangeEvent e) {
                    int filaSeleccionada = jTable1.getSelectedRow();
                    if (filaSeleccionada != -1) {
                        String nuevoEstado = jTable1.getValueAt(filaSeleccionada, 3).toString();
                        int idVenta = Integer.parseInt(jTable1.getValueAt(filaSeleccionada, 0).toString());

                        try {
                            String originalEstado = null;

                            // Verificar si el nuevo estado requiere confirmación
                            if (nuevoEstado.equals("cancelada") || nuevoEstado.equals("cotizacion_rechazada")) {
                                // Obtener el estado original desde la base de datos
                                originalEstado = obtenerEstadoOriginal(idVenta);

                                // Mostrar diálogo de confirmación
                                int respuesta = JOptionPane.showConfirmDialog(Carrito.this,
                                        "¿Está seguro de cambiar el estado a " + nuevoEstado + "?",
                                        "Confirmar cambio de estado",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE);

                                if (respuesta != JOptionPane.YES_OPTION) {
                                    // Revertir el valor de la celda al estado original
                                    jTable1.setValueAt(originalEstado, filaSeleccionada, 3);
                                    return; // No actualizar la base de datos
                                }
                            }

                            // Actualizar el estado en la base de datos
                            actualizarEstadoDocumento(idVenta, nuevoEstado);

                            // Verificar si es una cotización aprobada
                            boolean esCotizacion = verificarSiEsCotizacion(filaSeleccionada);
                            if (esCotizacion && nuevoEstado.equals("cotizacion_aprobada")) {
                                int respuestaConversion = JOptionPane.showConfirmDialog(Carrito.this,
                                        "¿Desea convertir esta cotización aprobada a venta?",
                                        "Convertir a Venta",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE);

                                if (respuestaConversion == JOptionPane.YES_OPTION) {
                                    // Esta funcionalidad no está disponible para órdenes de reserva
                                    Toast.show(Carrito.this, Toast.Type.INFO,
                                            "Esta función no está disponible para órdenes de reserva");
                                    // Recargar órdenes de reserva
                                    cargarOrdenesReserva();
                                }
                            }

                            // Refrescar la tabla si el estado es restrictivo
                            if (nuevoEstado.equals("cotizacion_rechazada") || nuevoEstado.equals("cancelada")
                                    || nuevoEstado.equals("cotizacion_convertida")) {
                                jTable1.repaint();
                            }

                        } catch (SQLException ex) {
                            // Revertir el estado en caso de error
                            try {
                                String estadoActual = obtenerEstadoOriginal(idVenta);
                                jTable1.setValueAt(estadoActual, filaSeleccionada, 3);
                            } catch (SQLException ex2) {
                                LOGGER.log(Level.SEVERE, "Error al revertir el estado", ex2);
                            }
                            LOGGER.log(Level.SEVERE, "Error al procesar cambio de estado", ex);
                            Toast.show(Carrito.this, Toast.Type.ERROR, "Error al cambiar estado: " + ex.getMessage());
                        }
                    }
                }

                private String obtenerEstadoOriginal(int idOrden) throws SQLException {
                    String sql = "SELECT estado FROM ordenes_reserva WHERE id_orden = ?";
                    try (Connection con = conexion.getInstance().createConnection();
                            PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idOrden);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                return rs.getString("estado");
                            } else {
                                throw new SQLException("No se encontró la orden con ID: " + idOrden);
                            }
                        }
                    }
                }

                @Override
                public void editingCanceled(ChangeEvent e) {
                    // No hacer nada
                }
            });

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al configurar selector de estado", ex);
            JOptionPane.showMessageDialog(null,
                    "Error al conectar con la base de datos: " + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            throw ex;
        }
    }

    // Método para actualizar el estado de una orden de reserva
    private void actualizarEstadoDocumento(int idDocumento, String nuevoEstado) throws SQLException {
        String sql = "UPDATE ordenes_reserva SET estado = ? WHERE id_orden = ?";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idDocumento);
            stmt.executeUpdate();
        }
    }

    private boolean verificarSiEsCotizacion(int row) {
        try {
            // Obtener el ID de la orden desde la tabla (formato "OR-123")
            String idOrdenStr = jTable1.getValueAt(row, 0).toString();

            // Extraer el número de orden del formato "OR-123"
            if (idOrdenStr.startsWith("OR-")) {
                int idOrden = Integer.parseInt(idOrdenStr.substring(3));

                String sql = "SELECT estado FROM ordenes_reserva WHERE id_orden = ?";
                try (Connection con = conexion.getInstance().createConnection();
                        PreparedStatement stmt = con.prepareStatement(sql)) {

                    stmt.setInt(1, idOrden);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String estado = rs.getString("estado");
                            // Consideramos como "cotización" las órdenes en estado PENDIENTE
                            return "pendiente".equals(normalizarEstadoOrdenReserva(estado));
                        }
                    }
                }
            }
        } catch (SQLException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar estado de la orden", e);
        }
        return false;
    }

    private void aplicarEstiloPaneles() {

        btnFiltrar.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 25px
                + "background:$App.accent.default;"); // Usa color de fondo de tabla
        btnReiniciar.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 25px
                + "background:$App.accent.red;"); // Usa color de fondo de tabla
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;"
                        + "trackInsets:3,3,3,3;"
                        + "thumbInsets:3,3,3,3;"
                        + "background:$Table.background;");

        // Aplicar estilo a todos los paneles
        panelListadoVentas.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelVentasMes.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelfiltroVentas.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelprodVendidos.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        PanelNventas.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelProdVend.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
    }

    private void configurarEstiloEtiquetas() {
        // Aplicar estilo de fuente a los títulos
        tituloCantidadVentas.putClientProperty(FlatClientProperties.STYLE, FONT_SUBHEADER_STYLE);
        tituloVentasTot.putClientProperty(FlatClientProperties.STYLE, FONT_SUBHEADER_STYLE);
        tituloListado.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);
        tituloPromedio.putClientProperty(FlatClientProperties.STYLE, FONT_SUBHEADER_STYLE);
        tituloProdVENDI.putClientProperty(FlatClientProperties.STYLE, FONT_SUBHEADER_STYLE);
        txtVentaMesPanel.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);
        txtnVentasMes.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);
        txtPromedioVenta.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);
        txtProdVen.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);

        // Configurar estilos de los títulos
        String boldStyle = "font:bold +2;";
        lbltitle.putClientProperty(FlatClientProperties.STYLE, boldStyle);
        lbltitle2.putClientProperty(FlatClientProperties.STYLE, boldStyle);

        // Configurar estilo de campos de texto
        String textFieldStyle = "arc:15;"
                + "borderColor: #D0D0D0;"
                + "focusWidth: 1;"
                + "focusColor:$Menu.foreground;"
                + "innerFocusWidth: 0;";
        txtFechaIn.putClientProperty(FlatClientProperties.STYLE, textFieldStyle);
        txtFechaFin.putClientProperty(FlatClientProperties.STYLE, textFieldStyle);
    }

    private void configurarEstiloTabla() {
        // Estilo general de la tabla
        jTable1.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");

        // Estilo del encabezado de la tabla
        jTable1.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");
    }

    private void configurarSelectorFechas() {
        datePicker.setCloseAfterSelected(true);
        datePicker.setEditor(txtFechaIn);
        datePicker1.setEditor(txtFechaFin);
        datePicker1.setCloseAfterSelected(true);
    }

    private void configurarTabla() throws SQLException {
        // Configurar evento de acciones de tabla
        TableActionEvent event = crearEventoAccionesTabla();

        // Configurar selector de estado
        configurarSelectorEstado();

        // Configurar anchos de columnas
        configurarAnchoColumnas();

        // Configurar renderizadores personalizados
        jTable1.getColumnModel().getColumn(7).setCellRenderer(new TableActionCellRender());
        jTable1.getColumnModel().getColumn(7).setCellEditor(new TableActionCellEditor(event));

        // Configurar alineación del ID (centrado)
        jTable1.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i,
                    int i1) {
                setHorizontalAlignment(SwingConstants.CENTER);
                return super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
            }
        });

        // Configurar renderizador para fechas (columna 1)
        jTable1.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                if (value instanceof Date) {
                    value = dateFormat.format((Date) value);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return super.getTableCellRendererComponent(jtable, value, isSelected, hasFocus, row, column);
            }
        });

        // Configurar renderizador para cliente (columna 2) - alineación izquierda
        jTable1.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i,
                    int i1) {
                setHorizontalAlignment(SwingConstants.LEFT);
                return super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
            }
        });

        // Configurar renderizador para estado (columna 3) - centrado
        jTable1.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i,
                    int i1) {
                setHorizontalAlignment(SwingConstants.CENTER);
                return super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
            }
        });

        // Configurar renderizador para productos (columna 4) - centrado
        jTable1.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                // Formatear el texto de productos para mejor legibilidad
                if (value != null) {
                    String texto = value.toString();
                    if (!texto.contains("productos")) {
                        texto = texto + " productos";
                    }
                    value = texto;
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return super.getTableCellRendererComponent(jtable, value, isSelected, hasFocus, row, column);
            }
        });

        // Configurar renderizador para la columna Estado
        jTable1.getColumnModel().getColumn(3).setCellRenderer(crearRenderizadorEstado());
    }

    /**
     * Configura los anchos preferidos de las columnas de la tabla
     */
    private void configurarAnchoColumnas() {
        // Configurar anchos de columnas para mejor visualización
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(80); // ID
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(100); // Fecha
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(200); // Cliente
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(120); // Bodega
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(100); // Estado
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(150); // Productos
        jTable1.getColumnModel().getColumn(6).setPreferredWidth(120); // Fecha de estado
        jTable1.getColumnModel().getColumn(7).setPreferredWidth(80); // Acciones

        // Configurar anchos mínimos y máximos para algunas columnas críticas
        jTable1.getColumnModel().getColumn(0).setMinWidth(70);
        jTable1.getColumnModel().getColumn(0).setMaxWidth(100);
        jTable1.getColumnModel().getColumn(7).setMinWidth(70);
        jTable1.getColumnModel().getColumn(7).setMaxWidth(100);

        // Permitir que la columna de cliente se expanda más
        jTable1.getColumnModel().getColumn(2).setMinWidth(150);
    }

    /**
     * Verifica si una fila debe ser editable basándose en su estado. Las filas
     * con estados "cotizacion_rechazada" o "cancelada" no serán editables.
     *
     * @param row Índice de la fila a verificar
     * @return true si la fila es editable, false en caso contrario
     */
    private boolean isRowEditable(int row) {
        // Validación de rango para evitar excepciones
        if (row < 0 || row >= jTable1.getRowCount()) {
            return false;
        }
        int estadoCol = getEstadoColumnIndex(row);
        String estado = jTable1.getValueAt(row, estadoCol).toString().toLowerCase();

        // Retornamos false si el estado es "cotizacion_rechazada", "cancelada" o
        // "cotizacion_convertida"
        return !estado.equals("cotizacion_rechazada")
                && !estado.equals("cancelada")
                && !estado.equals("cotizacion_convertida");

    }

    private int getEstadoColumnIndex(int row) {
        if (row < 0 || row >= jTable1.getRowCount()) {
            return 3;
        }
        Object idValue = jTable1.getValueAt(row, 0);
        String idStr = idValue != null ? idValue.toString() : "";
        return idStr.startsWith("OR-") ? 4 : 3;
    }

    private String normalizarEstadoOrdenReserva(String estado) {
        if (estado == null) {
            return "";
        }
        String e = estado.trim().replaceAll("\\s+", " ");
        if (e.equalsIgnoreCase("pendiente productos")) {
            return "pendiente";
        }
        return e.toLowerCase();
    }

    private TableActionEvent crearEventoAccionesTabla() {
        return new TableActionEvent() {
            @Override
            public void onEdit(int row) {
                try {
                    // Verificamos si la fila es editable según su estado
                    if (!isRowEditable(row)) {
                        // Mostramos un mensaje informativo al usuario
                        Toast.show(panelMain, Toast.Type.WARNING,
                                "No se puede editar un documento cancelado o rechazado");
                        return; // Detenemos la ejecución si no es editable
                    }

                    String idStr = jTable1.getValueAt(row, 0).toString();

                    // Verificar si es una orden de reserva (prefijo OR-)
                    if (idStr.startsWith("OR-")) {
                        int idOrden = Integer.parseInt(idStr.substring(3)); // Remover prefijo OR-
                        mostrarDetalleOrdenReserva(idOrden);
                        return;
                    }

                    // Es una venta normal
                    int idVenta = Integer.parseInt(idStr);

                    // Obtener los detalles de la venta específica
                    List<Map<String, Object>> detalles = reporteVentas.generarDetalleVenta(idVenta);

                    // Obtener información general de la venta
                    List<Map<String, Object>> ventas = reporteVentas.filtrarVentas(null, null, -1, null);
                    Map<String, Object> ventaInfo = ventas.stream()
                            .filter(v -> (int) v.get("id_venta") == idVenta)
                            .findFirst()
                            .orElse(null);

                    // Mostrar el formulario viewReport con los detalles
                    mostrarReporteVenta(idVenta, ventaInfo, detalles);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Error al cargar el detalle", e);
                    Toast.show(panelMain, Toast.Type.ERROR, "Error al cargar el detalle");
                }
            }

            @Override
            public void onDelete(int row) {
                String idStr = jTable1.getValueAt(row, 0).toString();

                // Verificar si es una orden de reserva
                if (idStr.startsWith("OR-")) {
                    int idOrden = Integer.parseInt(idStr.substring(3));
                    String estado = jTable1.getValueAt(row, 4).toString();
                    String estadoNormalizado = normalizarEstadoOrdenReserva(estado);

                    // Solo permitir cambio de estado si está en PENDIENTE
                    if ("pendiente".equals(estadoNormalizado)) {
                        mostrarOpcionesEstadoOrden(idOrden, row);
                    } else {
                        Toast.show(panelMain, Toast.Type.WARNING,
                                "Solo se pueden procesar órdenes en estado pendiente");
                    }
                }
            }

            @Override
            public void onView(int row) {
                try {
                    Toast.show(panelMain, Toast.Type.INFO, "Cargando el reporte...");
                    String idStr = jTable1.getValueAt(row, 0).toString();

                    // Verificar si es una orden de reserva
                    if (idStr.startsWith("OR-")) {
                        int idOrden = Integer.parseInt(idStr.substring(3));
                        mostrarDetalleOrdenReserva(idOrden);
                        return;
                    }

                    // Es una venta normal
                    int idVenta = Integer.parseInt(idStr);
                    String tipo = jTable1.getValueAt(row, 3).toString();
                    JOptionPane.showMessageDialog(null, tipo);
                    if (tipo.toLowerCase().equals("cotizacion")
                            || tipo.toLowerCase().equals("cotizacion_aprobada")
                            || tipo.toLowerCase().equals("cotizacion_rechazada")
                            || tipo.toLowerCase().equals("cotizacion_convertida")) {
                        reporteVentas.generarCotizacion(idVenta, panelProdVend);

                    } else {
                        reporteVentas.llamarInforme(idVenta);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Error al cargar el reporte", e);
                    Toast.show(panelMain, Toast.Type.ERROR, "Error al cargar el reporte");
                }
            }

            public void onCaja(int row) {
                /* no usado */ }
        };
    }

    /**
     * Muestra el detalle de una orden de reserva específica
     */
    private void mostrarDetalleOrdenReserva(int idOrden) {
        try {
            // Obtener la orden y sus detalles
            raven.modelos.OrdenReserva orden = ordenReservaService.obtenerOrdenPorId(idOrden);
            List<raven.modelos.OrdenReservaDetalle> detalles = ordenReservaService.obtenerDetallesOrden(idOrden);

            if (orden == null) {
                Toast.show(this, Toast.Type.ERROR, "No se encontró la orden de reserva");
                return;
            }

            // Crear un diálogo con la información
            StringBuilder info = new StringBuilder();
            info.append("ORDEN DE RESERVA #").append(idOrden).append("\n\n");
            info.append("Estado: ").append(orden.getEstado()).append("\n");
            info.append("Fecha: ").append(orden.getFechaCreacion()).append("\n");
            info.append("Usuario: ").append(orden.getIdUsuario()).append("\n");
            info.append("Bodega: ").append(orden.getIdBodega()).append("\n\n");
            info.append("PRODUCTOS:\n");

            double total = 0;
            for (raven.modelos.OrdenReservaDetalle detalle : detalles) {
                info.append("- ").append(detalle.getNombreProducto())
                        .append(" (").append(detalle.getCodigoProducto()).append(")\n");
                info.append("  Talla: ").append(detalle.getTalla())
                        .append(", Color: ").append(detalle.getColor()).append("\n");
                info.append("  Cantidad: ").append(detalle.getCantidad())
                        .append(", Precio: $").append(formatoMoneda.format(detalle.getPrecio()))
                        .append(", Subtotal: $").append(formatoMoneda.format(detalle.getSubtotal())).append("\n\n");

                total += detalle.getSubtotal();
            }

            info.append("TOTAL: $").append(formatoMoneda.format(total));

            String[] opciones = { "Cargar en formulario", "Cerrar" };
            int seleccion = JOptionPane.showOptionDialog(this, info.toString(),
                    "Detalle Orden de Reserva",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]);

            if (seleccion == 0) {
                cargarOrdenEnFormularioVenta(idOrden);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al mostrar detalle de orden", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar detalle: " + e.getMessage());
        }
    }

    /**
     * Muestra las opciones para cambiar el estado de una orden de reserva
     */
    private void mostrarOpcionesEstadoOrden(int idOrden, int row) {
        String[] opciones = { "RETIRADO", "PAGADO", "FINALIZADO", "Cancelar" };

        int seleccion = JOptionPane.showOptionDialog(this,
                "Seleccione el nuevo estado para la orden:",
                "Cambiar Estado Orden",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (seleccion >= 0 && seleccion < 3) {
            try {
                String nuevoEstado = opciones[seleccion];
                boolean exito = ordenReservaService.cambiarEstadoOrden(idOrden, nuevoEstado);

                if (exito) {
                    String estadoMostrar = normalizarEstadoOrdenReserva(nuevoEstado);
                    jTable1.setValueAt(estadoMostrar, row, 4);
                    Toast.show(this, Toast.Type.SUCCESS,
                            "Estado actualizado a " + estadoMostrar);

                    if ("finalizado".equals(estadoMostrar)) {
                        int respuesta = JOptionPane.showConfirmDialog(this,
                                "¿Desea convertir esta orden a venta?",
                                "Convertir a Venta",
                                JOptionPane.YES_NO_OPTION);

                        if (respuesta == JOptionPane.YES_OPTION) {
                            convertirOrdenAVenta(idOrden);
                        }
                    }
                } else {
                    Toast.show(this, Toast.Type.ERROR, "Error al actualizar el estado");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cambiar estado de orden", e);
                Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
            }
        } else if (seleccion == 3) {
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de cancelar la orden?",
                    "Cancelar Orden",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirmar == JOptionPane.YES_OPTION) {
                try {
                    boolean exito = ordenReservaService.cambiarEstadoOrden(idOrden, "cancelada");
                    if (exito) {
                        jTable1.setValueAt("cancelada", row, 4);
                        Toast.show(this, Toast.Type.SUCCESS, "Orden cancelada");
                        cargarEstadisticasMesActual();
                    } else {
                        Toast.show(this, Toast.Type.ERROR, "No se pudo cancelar la orden");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al cancelar orden", e);
                    Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Convierte una orden de reserva a venta
     */
    private void convertirOrdenAVenta(int idOrden) {
        try {
            int idVenta = ordenReservaService.convertirOrdenAVenta(idOrden);

            if (idVenta > 0) {
                Toast.show(this, Toast.Type.SUCCESS,
                        "Orden convertida a venta exitosamente");
                // Abrir reporte de la venta recién creada
                reporteVentas.llamarInforme(idVenta);
                // Recargar datos para reflejar los cambios
                cargarDatosIniciales();
            } else {
                Toast.show(this, Toast.Type.ERROR,
                        "Error al convertir la orden a venta");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al convertir orden a venta", e);
            Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
        }
    }

    // Abre el formulario de ventas y carga los productos de la orden de reserva
    private void cargarOrdenEnFormularioVenta(int idOrden) {
        try {
            generarVentaFor1 form = new generarVentaFor1();
            // Mostrar el formulario primero para asegurar un root válido para
            // Toast/JOptionPane
            Application.showForm(form);
            // Luego cargar la orden en el formulario ya montado
            form.cargarOrdenReservaEnFormulario(idOrden);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar orden en formulario", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar en formulario: " + e.getMessage());
        }
    }

    /**
     * Crea un renderizador personalizado para la columna de estado que muestra
     * visualmente los estados con diferentes colores y estilos.
     *
     * @return Renderizador personalizado para la columna de estado
     */
    private DefaultTableCellRenderer crearRenderizadorEstado() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(JLabel.CENTER);
                // Establece colores basados en el estado
                if (value != null) {
                    String estado = value.toString().toLowerCase();
                    String color = getColorForStatus(estado);

                    // Aplicamos un estilo diferente para estados no editables
                    String estiloAdicional = "";
                    if (estado.equals("cotizacion_rechazada") || estado.equals("cancelada")) {
                        estiloAdicional = "opacity: 0.7; font-style: italic;"; // Estilo para estados no editables
                    }

                    label.setText("<html><div style='padding: 3px 8px; border-radius: 10px; "
                            + "display: inline-block; font-size: 11px; font-weight: bold; "
                            + estiloAdicional
                            + "background-color: " + color + "; "
                            + "color: white;'>" + value.toString() + "</div></html>");
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                return label;
            }

            private String getColorForStatus(String status) {
                switch (status) {
                    // Estados de venta
                    case "completada":
                        return "#4CAF50"; // Verde
                    case "pendiente":
                        return "#FFC107"; // Amarillo
                    case "cancelada":
                        return "#F44336"; // Rojo

                    // Estados de cotización
                    case "cotizacion":
                        return "#2196F3"; // Azul
                    case "cotizacion_aprobada":
                        return "#4CAF50"; // Verde
                    case "cotizacion_rechazada":
                        return "#F44336"; // Rojo
                    case "cotizacion_convertida":
                        return "#9C27B0"; // Púrpura

                    default:
                        return "#9E9E9E"; // Gris
                }
            }
        };
    }

    /**
     * Configura el selector de estado en la tabla
     */
    /**
     * Carga los datos iniciales del panel
     */
    private void cargarDatosIniciales() {
        // Cargar combo box de clientes
        cargarComboClientes();
        // Cargar estadísticas del mes actual
        cargarEstadisticasMesActual();
        // Cargar órdenes de reserva en la tabla
        cargarOrdenesReserva();
        // Aplicar selección inicial si viene desde notificación
        aplicarSeleccionInicial();
    }

    /**
     * Carga las órdenes de reserva en la tabla
     */
    public void cargarOrdenesReserva() {
        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            List<Map<String, Object>> ordenes = ordenReservaService.obtenerTodasLasOrdenesParaTabla(idBodega);
            cargarOrdenes(ordenes); // Usar el método unificado que maneja las 8 columnas correctamente
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar órdenes de reserva", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar órdenes de reserva: " + e.getMessage());
        }
    }

    /**
     * Carga las órdenes de reserva en la tabla
     */
    public void cargarOrdenes(List<Map<String, Object>> ordenes) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // Limpiar tabla

        for (Map<String, Object> orden : ordenes) {
            // Validar que la orden tenga datos válidos antes de agregarla
            if (orden.get("id") == null || orden.get("fecha") == null || orden.get("cliente") == null ||
                    orden.get("estado") == null || orden.get("productos") == null) {
                LOGGER.log(Level.WARNING, "Orden con datos incompletos omitida: " + orden);
                continue; // Saltar filas con datos incompletos
            }

            // Validar que los valores no estén vacíos
            String id = String.valueOf(orden.get("id")).trim();
            String cliente = String.valueOf(orden.get("cliente")).trim();
            String estado = normalizarEstadoOrdenReserva(String.valueOf(orden.get("estado")));

            if (id.isEmpty() || cliente.isEmpty() || estado.isEmpty()) {
                LOGGER.log(Level.WARNING, "Orden con datos vacíos omitida: " + orden);
                continue; // Saltar filas con datos vacíos
            }

            // Determinar la fecha de estado según el estado actual
            String fechaEstado = "";
            if ("retirado".equals(estado) && orden.get("fecha_retirado") != null) {
                fechaEstado = orden.get("fecha_retirado").toString();
            } else if ("pagado".equals(estado) && orden.get("fecha_pagado") != null) {
                fechaEstado = orden.get("fecha_pagado").toString();
            } else if ("finalizado".equals(estado) && orden.get("fecha_finalizado") != null) {
                fechaEstado = orden.get("fecha_finalizado").toString();
            } else {
                fechaEstado = orden.get("fecha").toString(); // Fecha de creación por defecto
            }

            // Orden correcto según los títulos de la tabla: ID, Fecha, Cliente, Bodega,
            // Estado, Productos, Fecha de estado, Acciones
            model.addRow(new Object[] {
                    orden.get("id"), // ID - Ya viene con prefijo "OR-"
                    orden.get("fecha"), // Fecha - fecha_creacion
                    orden.get("cliente"), // Cliente
                    orden.get("bodega"), // Bodega - Información de bodega asignada
                    estado, // Estado - Estado actual del pedido
                    orden.get("productos"), // Productos - Información de productos
                    fechaEstado, // Fecha de estado - Fecha según el estado actual
                    "" // Acciones - La columna de acciones se llena con el renderizador
            });
        }
        // Intentar selección al terminar de cargar
        aplicarSeleccionInicial();
    }

    // Selecciona la fila de la tabla que corresponde al id_referencia
    public void seleccionarOrdenPorReferencia(int idReferencia) {
        try {
            String objetivoConPrefijo = "OR-" + idReferencia;
            int filas = jTable1.getRowCount();
            for (int i = 0; i < filas; i++) {
                Object val = jTable1.getValueAt(i, 0);
                if (val == null)
                    continue;
                String idTabla = val.toString();
                if (objetivoConPrefijo.equalsIgnoreCase(idTabla) || String.valueOf(idReferencia).equals(idTabla)) {
                    jTable1.getSelectionModel().setSelectionInterval(i, i);
                    jTable1.scrollRectToVisible(jTable1.getCellRect(i, 0, true));
                    break;
                }
            }
        } catch (Exception ignore) {
        }
    }

    // Aplica selección si hay un id de referencia pendiente
    private void aplicarSeleccionInicial() {
        if (idReferenciaSeleccionInicial != null) {
            seleccionarOrdenPorReferencia(idReferenciaSeleccionInicial);
            idReferenciaSeleccionInicial = null; // consumir una vez
        }
    }

    /**
     * Carga las ventas en la tabla - MÉTODO OBSOLETO PARA ÓRDENES DE RESERVA
     */
    @Deprecated
    public void cargarVentas(List<Map<String, Object>> ventas) {
        // Este método se mantiene por compatibilidad pero debería usar cargarOrdenes
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // Limpiar tabla

        for (Map<String, Object> venta : ventas) {
            model.addRow(new Object[] {
                    venta.get("id_venta"),
                    venta.get("fecha_venta"),
                    venta.get("cliente"),
                    venta.get("estado"),
                    venta.get("tipo_pago"),
                    "$" + formatoMoneda.format(venta.get("total")),
                    "" // La columna de acciones se llena con el renderizador
            });
        }
    }

    /**
     *
     * Carga los clientes en el combobox
     */
    private void cargarComboClientes() {
        cbxCliente.removeAllItems();
        try {
            // Cargar clientes disponibles
            cbxCliente.addItem("Seleccionar");
            for (ModelCliente client : serviceCliente.getAll()) {
                cbxCliente.addItem(client.getNombre());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar clientes", e);
            Toast.show(this, Toast.Type.INFO, "Error al cargar clientes: " + e.getMessage());
        }
    }

    /**
     * Carga las estadísticas del mes actual
     */
    public void cargarEstadisticasMesActual() {
        try {
            LocalDate now = LocalDate.now();
            LocalDate inicio = now.withDayOfMonth(1);
            LocalDate fin = now.withDayOfMonth(now.lengthOfMonth());
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();

            Map<String, Object> stats = ordenReservaService.obtenerEstadisticasOrdenes(inicio, fin, idBodega);

            // Formatear y mostrar valores
            txtVentaMesPanel.setText(stats.getOrDefault("pendientes", 0).toString());
            txtnVentasMes.setText(stats.getOrDefault("retirados", 0).toString());
            txtPromedioVenta.setText(stats.getOrDefault("pagados", 0).toString());

            // Usar finalizados para el cuarto panel si existe
            txtProdVen.setText(stats.getOrDefault("finalizados", 0).toString());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar estadísticas", e);
            txtVentaMesPanel.setText("0");
            txtnVentasMes.setText("0");
            txtPromedioVenta.setText("0");
            txtProdVen.setText("0");
        }
    }

    /**
     * Muestra el detalle de una venta específica
     */
    private void mostrarReporteVenta(int idVenta, Map<String, Object> ventaInfo, List<Map<String, Object>> detalles) {
        // Crear una instancia del formulario viewReport
        viewReport reporteForm = new viewReport();

        // ... resto de campos
        // Configurar la información de la venta en el formulario
        if (ventaInfo != null) {
            // Configurar información general de la venta
            // Usar descuento de venta en lugar del detalle
            if (!ventaInfo.containsKey("descuento")) {
                // Si no existe, intenta buscarlo en la primera fila de detalles
                if (detalles != null && !detalles.isEmpty() && detalles.get(0).containsKey("descuento_venta_total")) {
                    ventaInfo.put("descuento", detalles.get(0).get("descuento_venta_total"));
                } else {
                    // Si no hay datos disponibles, usa 0
                    ventaInfo.put("descuento", 0.0);
                }

            }
            reporteForm.setVentaInfo(
                    idVenta,
                    ventaInfo.get("fecha_venta").toString(),
                    ventaInfo.get("estado").toString(),
                    Double.parseDouble(ventaInfo.get("total").toString()),
                    Double.parseDouble(ventaInfo.get("descuento").toString()), // Descuento desde ventas
                    ventaInfo.get("tipo_pago").toString());

            // Obtener la información completa del cliente
            obtenerInformacionCliente(ventaInfo);

            // Configurar información del cliente con todos los datos necesarios
            reporteForm.setClienteInfo(
                    ventaInfo.get("cliente").toString(),
                    ventaInfo.get("dni") != null ? ventaInfo.get("dni").toString() : "",
                    ventaInfo.get("telefono") != null ? ventaInfo.get("telefono").toString() : "",
                    ventaInfo.get("email") != null ? ventaInfo.get("email").toString() : "",
                    ventaInfo.get("direccion") != null ? ventaInfo.get("direccion").toString() : "");
        }

        // Usar el método setProductos que ya incluye la aplicación del renderer
        reporteForm.setProductos(detalles);
        // Mostrar el formulario en un diálogo
        JDialog dialog = new JDialog();
        dialog.setTitle("Detalles de Venta #" + idVenta);
        dialog.setModal(true);
        dialog.setSize(1000, 800);
        dialog.setLocationRelativeTo(null);
        dialog.setContentPane(reporteForm);
        dialog.setVisible(true);
    }

    /**
     * Obtiene información detallada del cliente
     */
    private void obtenerInformacionCliente(Map<String, Object> ventaInfo) {
        if (ventaInfo == null || !ventaInfo.containsKey("cliente")) {
            return;
        }

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT c.id_cliente, c.nombre, c.dni, c.telefono, c.email, c.direccion "
                                + "FROM clientes c "
                                + "WHERE c.nombre = ? OR c.id_cliente = ?")) {

            stmt.setString(1, ventaInfo.get("cliente").toString());

            // Si tenemos el ID del cliente, lo usamos también
            if (ventaInfo.containsKey("id_cliente")) {
                stmt.setInt(2, Integer.parseInt(ventaInfo.get("id_cliente").toString()));
            } else {
                stmt.setInt(2, -1); // valor que no coincidirá con ningún ID
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Agregamos todos los datos del cliente al mapa de ventaInfo
                    ventaInfo.put("dni", rs.getString("dni"));
                    ventaInfo.put("telefono", rs.getString("telefono"));
                    ventaInfo.put("email", rs.getString("email"));
                    ventaInfo.put("direccion", rs.getString("direccion"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener información del cliente", e);
        }
    }

    /**
     * Actualiza las estadísticas según el período seleccionado
     */
    private void actualizarEstadisticas(LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            Map<String, Object> stats = ordenReservaService.obtenerEstadisticasOrdenes(fechaInicio, fechaFin, idBodega);

            // Formatear y mostrar valores
            txtVentaMesPanel.setText(stats.getOrDefault("pendientes", 0).toString());
            txtnVentasMes.setText(stats.getOrDefault("retirados", 0).toString());
            txtPromedioVenta.setText(stats.getOrDefault("pagados", 0).toString());
            txtProdVen.setText(stats.getOrDefault("finalizados", 0).toString());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar estadísticas", e);
            txtVentaMesPanel.setText("0");
            txtnVentasMes.setText("0");
            txtPromedioVenta.setText("0");
            txtProdVen.setText("0");
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new raven.datetime.component.date.DatePicker();
        datePicker1 = new raven.datetime.component.date.DatePicker();
        jPanel1 = new javax.swing.JPanel();
        lb = new javax.swing.JLabel();
        scroll = new javax.swing.JScrollPane();
        panelMain = new javax.swing.JPanel();
        panelVentasMes = new javax.swing.JPanel();
        tituloVentasTot = new javax.swing.JLabel();
        txtVentaMesPanel = new javax.swing.JLabel();
        PanelNventas = new javax.swing.JPanel();
        tituloCantidadVentas = new javax.swing.JLabel();
        txtnVentasMes = new javax.swing.JLabel();
        panelprodVendidos = new javax.swing.JPanel();
        tituloPromedio = new javax.swing.JLabel();
        txtPromedioVenta = new javax.swing.JLabel();
        panelfiltroVentas = new javax.swing.JPanel();
        lbltitle = new javax.swing.JLabel();
        lbltitle2 = new javax.swing.JLabel();
        txtFechaIn = new javax.swing.JFormattedTextField();
        txtFechaFin = new javax.swing.JFormattedTextField();
        lbltitle3 = new javax.swing.JLabel();
        lbltitle4 = new javax.swing.JLabel();
        cbxEstado = new javax.swing.JComboBox<>();
        cbxCliente = new javax.swing.JComboBox<>();
        btnReiniciar = new javax.swing.JButton();
        btnFiltrar = new javax.swing.JButton();
        panelListadoVentas = new javax.swing.JPanel();
        tituloListado = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        panelProdVend = new javax.swing.JPanel();
        tituloProdVENDI = new javax.swing.JLabel();
        txtProdVen = new javax.swing.JLabel();
        txtFecha = new javax.swing.JLabel();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lb.setText("ÓRDENES WEB");

        panelVentasMes.setBackground(new java.awt.Color(204, 204, 204));

        tituloVentasTot.setText("Órdenes Pendientes");
        tituloVentasTot.setRequestFocusEnabled(false);

        txtVentaMesPanel.setText("$15.068.889");

        javax.swing.GroupLayout panelVentasMesLayout = new javax.swing.GroupLayout(panelVentasMes);
        panelVentasMes.setLayout(panelVentasMesLayout);
        panelVentasMesLayout.setHorizontalGroup(
                panelVentasMesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelVentasMesLayout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addGroup(panelVentasMesLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(tituloVentasTot, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(txtVentaMesPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addContainerGap()));
        panelVentasMesLayout.setVerticalGroup(
                panelVentasMesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelVentasMesLayout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addComponent(tituloVentasTot, javax.swing.GroupLayout.DEFAULT_SIZE, 28,
                                        Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtVentaMesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 27,
                                        Short.MAX_VALUE)
                                .addGap(31, 31, 31)));

        PanelNventas.setBackground(new java.awt.Color(204, 204, 204));

        tituloCantidadVentas.setText("Órdenes Retiradas");

        txtnVentasMes.setText("$15.068.889");

        javax.swing.GroupLayout PanelNventasLayout = new javax.swing.GroupLayout(PanelNventas);
        PanelNventas.setLayout(PanelNventasLayout);
        PanelNventasLayout.setHorizontalGroup(
                PanelNventasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(PanelNventasLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(PanelNventasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(tituloCantidadVentas, javax.swing.GroupLayout.DEFAULT_SIZE, 146,
                                                Short.MAX_VALUE)
                                        .addComponent(txtnVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        PanelNventasLayout.setVerticalGroup(
                PanelNventasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(PanelNventasLayout.createSequentialGroup()
                                .addGap(31, 31, 31)
                                .addComponent(tituloCantidadVentas, javax.swing.GroupLayout.PREFERRED_SIZE, 25,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtnVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                                .addContainerGap(22, Short.MAX_VALUE)));

        panelprodVendidos.setBackground(new java.awt.Color(204, 204, 204));

        tituloPromedio.setText("Órdenes Pagadas");

        txtPromedioVenta.setText("$15.068.889");

        javax.swing.GroupLayout panelprodVendidosLayout = new javax.swing.GroupLayout(panelprodVendidos);
        panelprodVendidos.setLayout(panelprodVendidosLayout);
        panelprodVendidosLayout.setHorizontalGroup(
                panelprodVendidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelprodVendidosLayout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addGroup(panelprodVendidosLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(tituloPromedio, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(txtPromedioVenta, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGap(12, 12, 12)));
        panelprodVendidosLayout.setVerticalGroup(
                panelprodVendidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelprodVendidosLayout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addComponent(tituloPromedio, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtPromedioVenta, javax.swing.GroupLayout.DEFAULT_SIZE, 33,
                                        Short.MAX_VALUE)
                                .addGap(25, 25, 25)));

        panelfiltroVentas.setBackground(new java.awt.Color(204, 204, 204));

        lbltitle.setText("Fecha desde");

        lbltitle2.setText("Fecha hasta");

        txtFechaIn.setSelectionColor(new java.awt.Color(255, 255, 255));
        txtFechaIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFechaInActionPerformed(evt);
            }
        });

        txtFechaFin.setSelectionColor(new java.awt.Color(255, 255, 255));
        txtFechaFin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFechaFinActionPerformed(evt);
            }
        });

        lbltitle3.setText("Cliente");

        lbltitle4.setText("Estado");

        cbxEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar", "pendiente",
                "completada", "cancelada", "cotizacion", "cotizacion_rechazada" }));
        cbxEstado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxEstadoActionPerformed(evt);
            }
        });

        cbxCliente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar" }));

        btnReiniciar.setText("Reiniciar");
        btnReiniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReiniciarActionPerformed(evt);
            }
        });

        btnFiltrar.setText("Filtrar");
        btnFiltrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFiltrarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelfiltroVentasLayout = new javax.swing.GroupLayout(panelfiltroVentas);
        panelfiltroVentas.setLayout(panelfiltroVentasLayout);
        panelfiltroVentasLayout.setHorizontalGroup(
                panelfiltroVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                                .addGap(115, 115, 115)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtFechaIn, javax.swing.GroupLayout.DEFAULT_SIZE, 176,
                                                Short.MAX_VALUE)
                                        .addComponent(lbltitle))
                                .addGap(30, 30, 30)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtFechaFin, javax.swing.GroupLayout.DEFAULT_SIZE, 176,
                                                Short.MAX_VALUE)
                                        .addComponent(lbltitle2))
                                .addGap(30, 30, 30)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(cbxCliente, 0, 178, Short.MAX_VALUE)
                                        .addComponent(lbltitle3))
                                .addGap(30, 30, 30)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                                                .addGroup(panelfiltroVentasLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lbltitle4)
                                                        .addComponent(cbxEstado, 0,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(115, 115, 115))
                                        .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                                                .addComponent(btnFiltrar, javax.swing.GroupLayout.PREFERRED_SIZE, 109,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnReiniciar, javax.swing.GroupLayout.PREFERRED_SIZE, 109,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(75, 75, 75)))));
        panelfiltroVentasLayout.setVerticalGroup(
                panelfiltroVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                                .addContainerGap(56, Short.MAX_VALUE)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                                                .addGroup(panelfiltroVentasLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lbltitle2,
                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(lbltitle))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelfiltroVentasLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(txtFechaIn, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                39, Short.MAX_VALUE)
                                                        .addComponent(txtFechaFin)))
                                        .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                                                .addGroup(panelfiltroVentasLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lbltitle3,
                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(lbltitle4))
                                                .addGap(6, 6, 6)
                                                .addGroup(panelfiltroVentasLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(cbxEstado, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(cbxCliente,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 39,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(14, 14, 14)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnFiltrar, javax.swing.GroupLayout.DEFAULT_SIZE, 36,
                                                Short.MAX_VALUE)
                                        .addComponent(btnReiniciar, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(17, 17, 17)));

        panelListadoVentas.setBackground(new java.awt.Color(204, 204, 204));

        tituloListado.setText("Listado de órdenes");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null }
                },
                new String[] {
                        "ID", "Fecha", "Cliente", "Bodega", "Estado", "Productos", "Fecha de estado", "Acciones"
                }));
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(6).setPreferredWidth(40);
        }

        javax.swing.GroupLayout panelListadoVentasLayout = new javax.swing.GroupLayout(panelListadoVentas);
        panelListadoVentas.setLayout(panelListadoVentasLayout);
        panelListadoVentasLayout.setHorizontalGroup(
                panelListadoVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelListadoVentasLayout.createSequentialGroup()
                                .addGap(38, 38, 38)
                                .addComponent(tituloListado)
                                .addGap(30, 949, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelListadoVentasLayout.createSequentialGroup()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1083,
                                                Short.MAX_VALUE)
                                        .addContainerGap()));
        panelListadoVentasLayout.setVerticalGroup(
                panelListadoVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelListadoVentasLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(tituloListado, javax.swing.GroupLayout.PREFERRED_SIZE, 48,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(75, Short.MAX_VALUE)));

        panelProdVend.setBackground(new java.awt.Color(204, 204, 204));

        tituloProdVENDI.setText("Órdenes Finalizadas");

        txtProdVen.setText("$15.068.889");

        javax.swing.GroupLayout panelProdVendLayout = new javax.swing.GroupLayout(panelProdVend);
        panelProdVend.setLayout(panelProdVendLayout);
        panelProdVendLayout.setHorizontalGroup(
                panelProdVendLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelProdVendLayout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addGroup(panelProdVendLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(tituloProdVENDI, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        491, Short.MAX_VALUE)
                                                .addComponent(txtProdVen, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addContainerGap()));
        panelProdVendLayout.setVerticalGroup(
                panelProdVendLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProdVendLayout.createSequentialGroup()
                                .addGap(31, 31, 31)
                                .addComponent(tituloProdVENDI, javax.swing.GroupLayout.DEFAULT_SIZE, 26,
                                        Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtProdVen, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                                .addContainerGap(21, Short.MAX_VALUE)));

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(panelMainLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(panelListadoVentas, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelfiltroVentas, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                .addComponent(panelVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(30, 30, 30)
                                                .addComponent(PanelNventas, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(48, 48, 48)
                                                .addComponent(panelprodVendidos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(30, 30, 30)
                                                .addComponent(panelProdVend, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGap(20, 20, 20)));
        panelMainLayout.setVerticalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(
                                        panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(PanelNventas, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(panelVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(panelprodVendidos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(panelProdVend, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(30, 30, 30)
                                .addComponent(panelfiltroVentas, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(51, 51, 51)
                                .addComponent(panelListadoVentas, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34)));

        scroll.setViewportView(panelMain);

        txtFecha.setText("FECHA:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 1147,
                                                Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(lb, javax.swing.GroupLayout.PREFERRED_SIZE, 619,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(txtFecha, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap()));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(lb, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(txtFecha, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 911, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    private void txtFechaInActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtFechaInActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtFechaInActionPerformed

    private void txtFechaFinActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtFechaFinActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtFechaFinActionPerformed

    private void cbxEstadoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxEstadoActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_cbxEstadoActionPerformed

    private void btnFiltrarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnFiltrarActionPerformed
        try {
            // Obtener fechas del DatePicker y convertir a Date
            Date fechaInicio = null;
            Date fechaFin = null;

            if (datePicker.isDateSelected()) {
                fechaInicio = Date.from(datePicker.getSelectedDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            if (datePicker1.isDateSelected()) {
                fechaFin = Date.from(datePicker1.getSelectedDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            // Validar que si una fecha está seleccionada, la otra también lo esté
            if ((fechaInicio != null && fechaFin == null) || (fechaInicio == null && fechaFin != null)) {
                Toast.show(this, Toast.Type.INFO, "Debe seleccionar ambas fechas o ninguna");
                return;
            }

            // Validar rango de fechas
            if (fechaInicio != null && fechaFin != null && fechaInicio.after(fechaFin)) {
                Toast.show(this, Toast.Type.INFO, "La fecha de inicio no puede ser mayor a la fecha fin");
                return;
            }

            // Obtener cliente seleccionado
            String clienteSeleccionado = cbxCliente.getSelectedItem().toString();
            int idCliente = clienteSeleccionado.equals("Seleccionar") ? -1
                    : serviceCliente.getIdByNombre(clienteSeleccionado);

            // Obtener estado seleccionado
            String estadoSeleccionado = cbxEstado.getSelectedItem().toString();
            String estado = estadoSeleccionado.equals("Seleccionar") ? null : estadoSeleccionado;

            // Validar que al menos un filtro esté seleccionado
            if (fechaInicio == null && idCliente == -1 && estado == null) {
                Toast.show(this, Toast.Type.INFO, "Seleccione al menos un criterio de búsqueda");
                return;
            }

            // Filtrar órdenes de reserva según los parámetros
            List<Map<String, Object>> ordenesFiltradas = filtrarOrdenesReserva(fechaInicio, fechaFin, idCliente,
                    estado);

            // Cargar las órdenes en la tabla
            cargarOrdenes(ordenesFiltradas);

            // Actualizar estadísticas si hay fechas seleccionadas
            if (fechaInicio != null && fechaFin != null) {
                // Convertir Date a LocalDate para el método actualizarEstadisticas
                LocalDate localFechaInicio = fechaInicio.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate localFechaFin = fechaFin.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                actualizarEstadisticas(localFechaInicio, localFechaFin);
            } else {
                // Si no hay fechas, mostrar estadísticas del mes actual
                cargarEstadisticasMesActual();
            }

        } catch (HeadlessException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar ventas", e);
            Toast.show(this, Toast.Type.ERROR, "Error al filtrar: " + e.getMessage());
        }
    }// GEN-LAST:event_btnFiltrarActionPerformed

    private void btnReiniciarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnReiniciarActionPerformed
        // Limpiar campos de fecha
        if (datePicker.getSelectedDate() != null) {

            datePicker.setSelectedDate(null);
        }
        if (datePicker1.getSelectedDate() != null) {

            datePicker1.setSelectedDate(null);
        }

        txtFechaIn.setText("");
        txtFechaFin.setText("");
        // Restablecer comboboxes
        cbxCliente.setSelectedIndex(0);
        cbxEstado.setSelectedIndex(0);
        // Mostrar datos del mes actual nuevamente
        cargarEstadisticasMesActual();
        // Cargar todas las órdenes de reserva
        cargarOrdenesReserva();
        Toast.show(this, Toast.Type.INFO, "Filtros reiniciados correctamente");

    }// GEN-LAST:event_btnReiniciarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel PanelNventas;
    public javax.swing.JButton btnFiltrar;
    public javax.swing.JButton btnReiniciar;
    public javax.swing.JComboBox<String> cbxCliente;
    public javax.swing.JComboBox<String> cbxEstado;
    private raven.datetime.component.date.DatePicker datePicker;
    private raven.datetime.component.date.DatePicker datePicker1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTable jTable1;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbltitle;
    private javax.swing.JLabel lbltitle2;
    private javax.swing.JLabel lbltitle3;
    private javax.swing.JLabel lbltitle4;
    private javax.swing.JPanel panelListadoVentas;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelProdVend;
    private javax.swing.JPanel panelVentasMes;
    private javax.swing.JPanel panelfiltroVentas;
    private javax.swing.JPanel panelprodVendidos;
    private javax.swing.JScrollPane scroll;
    public javax.swing.JLabel tituloCantidadVentas;
    private javax.swing.JLabel tituloListado;
    public javax.swing.JLabel tituloProdVENDI;
    public javax.swing.JLabel tituloPromedio;
    public javax.swing.JLabel tituloVentasTot;
    private javax.swing.JLabel txtFecha;
    public javax.swing.JFormattedTextField txtFechaFin;
    public javax.swing.JFormattedTextField txtFechaIn;
    private javax.swing.JLabel txtProdVen;
    private javax.swing.JLabel txtPromedioVenta;
    private javax.swing.JLabel txtVentaMesPanel;
    private javax.swing.JLabel txtnVentasMes;
    // End of variables declaration//GEN-END:variables

    /**
     * Filtra las órdenes de reserva según los parámetros especificados
     */
    private List<Map<String, Object>> filtrarOrdenesReserva(Date fechaInicio, Date fechaFin, int idCliente,
            String estado) {
        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            return ordenReservaService.filtrarOrdenes(fechaInicio, fechaFin, idCliente, estado, idBodega);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar órdenes de reserva", e);
            Toast.show(this, Toast.Type.ERROR, "Error al filtrar órdenes: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
