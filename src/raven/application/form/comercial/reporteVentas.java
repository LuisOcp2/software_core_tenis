package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.cell.TableActionCellEditor;
import raven.cell.TableActionCellRender;
import raven.cell.TableActionEvent;
import raven.clases.admin.UserSession;
import raven.clases.comercial.ServiceCliente;
import raven.clases.principal.ServiceVenta;
import raven.clases.productos.ImpresionPOST;
import raven.clases.reportes.ReporteVentas;
import raven.controlador.comercial.ModelCliente;
import raven.controlador.principal.conexion;
import raven.modal.Toast;
import java.awt.event.MouseAdapter;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class reporteVentas extends javax.swing.JPanel {
    // Instancia del servicio para operaciones con empleados

    LocalDate fechaZona = LocalDate.now(ZoneId.of("America/Bogota"));
    private static final Logger LOGGER = Logger.getLogger(ReporteVentas.class.getName());
    private static final String PANEL_STYLE = "arc:25;background:$Login.background;";
    private static final String FONT_HEADER_STYLE = "font:$h1.font";
    private static final String FONT_SUBHEADER_STYLE = "font:$h2.font";
    private final ReporteVentas reporteVentas;
    private final ServiceCliente serviceCliente;
    private final DecimalFormat formatoMoneda;
    // ICONOS....................................................................................................
    private final FontIcon filter;
    private final FontIcon excel;
    private final FontIcon pdf;
    private final FontIcon reiniciar;

    public reporteVentas() throws SQLException {
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        this.reporteVentas = new ReporteVentas();
        this.serviceCliente = new ServiceCliente();
        filter = createColoredIcon(FontAwesomeSolid.FILTER, tabTextColor);
        excel = createColoredIcon(FontAwesomeSolid.FILE_EXCEL, tabTextColor);
        pdf = createColoredIcon(FontAwesomeSolid.FILE_PDF, tabTextColor);
        reiniciar = createColoredIcon(FontAwesomeSolid.TRASH_ALT, tabTextColor);
        // Configurar formato para valores monetarios
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        this.formatoMoneda = new DecimalFormat("#,###", symbols);
        // Inicializar componentes
        initComponents();
        btnExportExcel.setIcon(excel);
        btnExportPdf.setIcon(pdf);
        btnFiltrar.setIcon(filter);
        btnReiniciar.setIcon(reiniciar);
        configurarUI();
        configurarTabla();
        configurarMenuContextual();
        cargarDatosIniciales();
        txtFecha.setText(fechaZona.toString());
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
                // En la clase reporteVentas, dentro del CellEditorListener donde manejas los
                // cambios de estado:
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
                                int respuesta = JOptionPane.showConfirmDialog(
                                        reporteVentas.this,
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
                                int respuestaConversion = JOptionPane.showConfirmDialog(
                                        reporteVentas.this,
                                        "¿Desea convertir esta cotización aprobada a venta?",
                                        "Convertir a Venta",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE);

                                if (respuestaConversion == JOptionPane.YES_OPTION) {
                                    try {
                                        ServiceVenta serviceVenta = new ServiceVenta();
                                        serviceVenta.convertirCotizacionAprobadaAVenta(idVenta);
                                        cargarVentas(reporteVentas.filtrarVentas(null, null, -1, null));
                                        Toast.show(reporteVentas.this, Toast.Type.SUCCESS,
                                                "Cotización convertida a venta exitosamente");
                                    } catch (SQLException ex) {
                                        LOGGER.log(Level.SEVERE, "Error al convertir cotización", ex);
                                        Toast.show(reporteVentas.this, Toast.Type.ERROR,
                                                "Error al convertir: " + ex.getMessage());
                                    }
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
                            Toast.show(reporteVentas.this, Toast.Type.ERROR,
                                    "Error al cambiar estado: " + ex.getMessage());
                        }
                    }
                }

                private String obtenerEstadoOriginal(int idVenta) throws SQLException {
                    String sql = "SELECT estado FROM ventas WHERE id_venta = ?";
                    try (Connection con = conexion.getInstance().createConnection();
                            PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idVenta);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                return rs.getString("estado");
                            } else {
                                throw new SQLException("No se encontró la venta con ID: " + idVenta);
                            }
                        }
                    }
                }

                @Override
                public void editingCanceled(ChangeEvent e) {
                    // No hacer nada
                }
            });

            // Establecemos el editor personalizado
            columnaEstado.setCellEditor(editorEstado);

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al configurar selector de estado", ex);
            JOptionPane.showMessageDialog(null,
                    "Error al conectar con la base de datos: " + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            throw ex;
        }
    }
    // Método para actualizar el estado de un documento

    private void actualizarEstadoDocumento(int idDocumento, String nuevoEstado) throws SQLException {
        String sql = "UPDATE ventas SET estado = ? WHERE id_venta = ?";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idDocumento);
            stmt.executeUpdate();
        }
    }
    // Método para convertir cotización aprobada

    private void convertirCotizacionAprobada(int idCotizacion) throws SQLException {
        ServiceVenta serviceVenta = new ServiceVenta();
        try {
            // Primero verificamos que la cotización esté en estado "cotizacion_aprobada"
            Connection con = conexion.getInstance().createConnection();
            String estadoActual = "";

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT estado FROM ventas WHERE id_venta = ? AND es_cotizacion = true")) {
                ps.setInt(1, idCotizacion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        estadoActual = rs.getString("estado");
                    }
                }
            }

            if (!estadoActual.equals("cotizacion_aprobada")) {
                throw new SQLException("La cotización debe estar en estado 'cotizacion_aprobada' para ser convertida");
            }

            // Utilizamos el método adaptado para convertir la cotización
            serviceVenta.convertirCotizacionAprobadaAVenta(idCotizacion);

            // Recargar los datos en la tabla
            cargarVentas(reporteVentas.filtrarVentas(null, null, -1, null));
            Toast.show(this, Toast.Type.SUCCESS, "Cotización convertida a venta exitosamente");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al convertir cotización", e);
            Toast.show(this, Toast.Type.ERROR, "Error al convertir: " + e.getMessage());
            throw e;
        }
    }

    private boolean verificarSiEsCotizacion(int row) {
        try {
            int idVenta = Integer.parseInt(jTable1.getValueAt(row, 0).toString());

            // Usar el campo 'estado' para verificar si es cotización
            String sql = "SELECT estado FROM ventas WHERE id_venta = ?";
            try (Connection con = conexion.getInstance().createConnection();
                    PreparedStatement stmt = con.prepareStatement(sql)) {

                stmt.setInt(1, idVenta);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String estado = rs.getString("estado");
                        // Considerar como cotización si el estado contiene "cotizacion"
                        return estado != null && estado.toLowerCase().contains("cotizacion");
                    }
                }
            }
        } catch (SQLException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar si es cotización", e);
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
        btnExportPdf.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 25px
                + "background:$Toast.error.outlineColor;"); // Usa color de fondo de tabla
        btnExportExcel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 25px
                + "background:#1D6F42;"); // Usa color de fondo de tabla
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;"
                        + "trackInsets:3,3,3,3;"
                        + "thumbInsets:3,3,3,3;"
                        + "background:$Table.background;");

        // Aplicar estilo a todos los paneles
        panelListadoVentas.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelVentasMes.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        // panelVentasPorDia.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        // panelVentasporCategoria.putClientProperty(FlatClientProperties.STYLE,
        // PANEL_STYLE);
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

        // Configurar renderizadores personalizados
        jTable1.getColumnModel().getColumn(6).setCellRenderer(new TableActionCellRender());
        jTable1.getColumnModel().getColumn(6).setCellEditor(new TableActionCellEditor(event));

        // Configurar alineación del ID
        jTable1.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i,
                    int i1) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                return super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
            }
        });

        // Configurar renderizador para la columna Estado
        jTable1.getColumnModel().getColumn(3).setCellRenderer(crearRenderizadorEstado());
    }

    /**
     * Configura el menú contextual (clic derecho) para la tabla de ventas.
     * Permite imprimir una copia de factura directamente a la impresora POS.
     */
    private void configurarMenuContextual() {
        // Crear el menú popup
        JPopupMenu popupMenu = new JPopupMenu();

        // Crear el item de menú con ícono
        JMenuItem menuItemImprimir = new JMenuItem("Imprimir copia de factura");
        menuItemImprimir.setIcon(createColoredIcon(FontAwesomeSolid.PRINT,
                UIManager.getColor("TabbedPane.foreground")));

        // Agregar listener al item
        menuItemImprimir.addActionListener(e -> {
            int row = jTable1.getSelectedRow();
            if (row != -1) {
                imprimirCopiaFactura(row);
            }
        });

        // Agregar item al menú
        popupMenu.add(menuItemImprimir);

        // Agregar listener de mouse a la tabla
        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    mostrarMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    mostrarMenu(e);
                }
            }

            private void mostrarMenu(MouseEvent e) {
                int row = jTable1.rowAtPoint(e.getPoint());
                if (row >= 0 && row < jTable1.getRowCount()) {
                    jTable1.setRowSelectionInterval(row, row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Imprime una copia de la factura en la impresora POS.
     * 
     * @param row Índice de la fila seleccionada en la tabla
     */
    private void imprimirCopiaFactura(int row) {
        try {
            // Validar que la fila sea válida
            if (row < 0 || row >= jTable1.getRowCount()) {
                Toast.show(this, Toast.Type.ERROR, "Seleccione una venta válida");
                return;
            }

            // Obtener el ID de la venta
            int idVenta = Integer.parseInt(jTable1.getValueAt(row, 0).toString());

            // Mostrar mensaje de procesamiento
            Toast.show(this, Toast.Type.INFO, "Enviando a impresora...");

            // Crear instancia de la impresora POS
            ImpresionPOST impresora = new ImpresionPOST("POS-80");

            // Imprimir el recibo
            if (impresora.imprimirRecibo(idVenta)) {
                Toast.show(this, Toast.Type.SUCCESS, "Recibo de tirilla generado correctamente");
            } else {
                Toast.show(this, Toast.Type.ERROR, "Error al generar recibo de tirilla");
                LOGGER.log(Level.SEVERE, "Error generando recibo para venta ID: " + idVenta);
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ID de venta", e);
            Toast.show(this, Toast.Type.ERROR, "Error al obtener información de la venta");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al imprimir factura", e);
            Toast.show(this, Toast.Type.ERROR, "Error al imprimir: " + e.getMessage());
        }
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
        // Obtenemos el valor del estado desde la columna 3
        String estado = jTable1.getValueAt(row, 3).toString().toLowerCase();

        // Retornamos false si el estado es "cotizacion_rechazada", "cancelada" o
        // "cotizacion_convertida"
        return !estado.equals("cotizacion_rechazada")
                && !estado.equals("cancelada")
                && !estado.equals("cotizacion_convertida");

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
                    int idVenta = Integer.parseInt(jTable1.getValueAt(row, 0).toString());

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
                    LOGGER.log(Level.WARNING, "Error al cargar el detalle de venta", e);
                    Toast.show(panelMain, Toast.Type.ERROR, "Error al cargar el detalle de venta");
                }
            }

            @Override
            public void onDelete(int row) {
                // No implementado en el código original
            }

            @Override
            public void onView(int row) {
                try {
                    Toast.show(panelMain, Toast.Type.INFO, "Cargando el reporte...");
                    int idVenta = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
                    String tipo = jTable1.getValueAt(row, 3).toString();

                    if (tipo.toLowerCase().equals("cotizacion")
                            || tipo.toLowerCase().equals("cotizacion_aprobada")
                            || tipo.toLowerCase().equals("cotizacion_rechazada")
                            || tipo.toLowerCase().equals("cotizacion_convertida")) {
                        reporteVentas.generarCotizacion(idVenta, panelProdVend);

                    } else {
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
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Error al cargar el reporte", e);
                    Toast.show(panelMain, Toast.Type.ERROR, "Error al cargar el reporte");
                }
            }

            @Override
            public void onCaja(int row) {

            }
        };
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
        // Cargar todas las ventas en la tabla
        List<Map<String, Object>> todasLasVentas = reporteVentas.filtrarVentas(null, null, -1, null);
        cargarVentas(todasLasVentas);
    }

    /**
     * Carga las ventas en la tabla
     */
    public void cargarVentas(List<Map<String, Object>> ventas) {
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
     * *****************************************************CODIGO
     * EXCEL**************************************************************** /**
     * Exporta los datos de la tabla de ventas a un archivo Excel. Utiliza la
     * biblioteca Apache POI para generar un archivo XLSX con formato
     * profesional. Permite al usuario seleccionar la ubicación donde guardar el
     * archivo.
     */
    /**
     * Exporta los datos de la tabla de ventas a un archivo Excel. Utiliza la
     * biblioteca Apache POI para generar un archivo XLSX con formato
     * profesional. Permite al usuario seleccionar la ubicación donde guardar el
     * archivo.
     */
    private void exportarExcel() {
        try {
            // Crear un cuadro de diálogo para que el usuario seleccione dónde guardar el
            // archivo
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar reporte Excel");

            // Configurar el filtro para archivos Excel
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx");
            fileChooser.setFileFilter(filter);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            // Sugerir un nombre predeterminado con la fecha actual
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String fechaActual = sdf.format(new Date());
            fileChooser.setSelectedFile(new File("Reporte_Ventas_" + fechaActual + ".xlsx"));

            // Mostrar el diálogo y procesar la selección del usuario
            int seleccion = fileChooser.showSaveDialog(this);

            if (seleccion == JFileChooser.APPROVE_OPTION) {
                File archivoSeleccionado = fileChooser.getSelectedFile();

                // Asegurar que el archivo tenga extensión .xlsx
                String rutaArchivo = archivoSeleccionado.getAbsolutePath();
                if (!rutaArchivo.toLowerCase().endsWith(".xlsx")) {
                    rutaArchivo += ".xlsx";
                    archivoSeleccionado = new File(rutaArchivo);
                }

                // Mostrar indicador de progreso
                Toast.show(this, Toast.Type.INFO, "Generando archivo Excel...");

                // Crear el libro de trabajo Excel
                try (XSSFWorkbook libro = new XSSFWorkbook()) {
                    // Crear una hoja de cálculo en el libro
                    XSSFSheet hoja = libro.createSheet("Reporte de Ventas");

                    // Configurar estilos para encabezados
                    XSSFCellStyle estiloEncabezado = crearEstiloEncabezado(libro);
                    XSSFCellStyle estiloFila = crearEstiloFila(libro);
                    XSSFCellStyle estiloMonto = crearEstiloMonto(libro);

                    // Crear la fila de encabezado
                    Row filaEncabezado = hoja.createRow(0);

                    // Obtener los encabezados de la tabla
                    DefaultTableModel modelo = (DefaultTableModel) jTable1.getModel();
                    int columnCount = modelo.getColumnCount() - 1; // Excluir columna de acciones

                    // Llenar los encabezados
                    for (int i = 0; i < columnCount; i++) {
                        Cell celda = filaEncabezado.createCell(i);
                        celda.setCellValue(modelo.getColumnName(i));
                        celda.setCellStyle(estiloEncabezado);
                    }

                    // Llenar los datos
                    int totalFilas = modelo.getRowCount();
                    for (int i = 0; i < totalFilas; i++) {
                        Row fila = hoja.createRow(i + 1);

                        for (int j = 0; j < columnCount; j++) {
                            Cell celda = fila.createCell(j);
                            Object valorCelda = modelo.getValueAt(i, j);

                            // Formatear según el tipo de dato
                            if (j == 0) { // ID (numérico)
                                celda.setCellValue(Integer.parseInt(valorCelda.toString()));
                                celda.setCellStyle(estiloFila);
                            } else if (j == 5) { // Total (monto)
                                // Extraer el valor numérico quitando el símbolo de moneda y puntos
                                String valor = valorCelda.toString().replace("$", "").replace(".", "");
                                try {
                                    double monto = Double.parseDouble(valor);
                                    celda.setCellValue(monto);
                                    celda.setCellStyle(estiloMonto);
                                } catch (NumberFormatException e) {
                                    celda.setCellValue(valorCelda.toString());
                                    celda.setCellStyle(estiloFila);
                                }
                            } else {
                                celda.setCellValue(valorCelda != null ? valorCelda.toString() : "");
                                celda.setCellStyle(estiloFila);
                            }
                        }
                    }

                    // Ajustar el ancho de las columnas automáticamente
                    for (int i = 0; i < columnCount; i++) {
                        hoja.autoSizeColumn(i);
                    }

                    // Agregar información adicional al reporte
                    agregarInformacionResumen(libro, totalFilas);

                    // Guardar el archivo
                    try (FileOutputStream fileOut = new FileOutputStream(archivoSeleccionado)) {
                        libro.write(fileOut);
                    }

                    // Notificar al usuario
                    Toast.show(this, Toast.Type.SUCCESS, "Reporte Excel generado exitosamente en: " + rutaArchivo);

                    // Abrir el archivo automáticamente si el usuario lo desea
                    int abrirArchivo = JOptionPane.showConfirmDialog(this,
                            "El archivo ha sido guardado correctamente. ¿Desea abrirlo ahora?",
                            "Exportación exitosa", JOptionPane.YES_NO_OPTION);

                    if (abrirArchivo == JOptionPane.YES_OPTION) {
                        abrirArchivo(archivoSeleccionado);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al exportar a Excel", e);
            Toast.show(this, Toast.Type.ERROR, "Error al generar el archivo Excel: " + e.getMessage());
        }
    }

    /**
     * Crea un estilo personalizado para los encabezados del Excel.
     *
     * @param libro El libro de trabajo Excel donde se aplicará el estilo
     * @return Estilo configurado para encabezados
     */
    private XSSFCellStyle crearEstiloEncabezado(XSSFWorkbook libro) {
        XSSFCellStyle estilo = libro.createCellStyle();
        // Fondo azul para encabezados
        estilo.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // Bordes
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        // Alineación
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        // Fuente
        XSSFFont fuente = libro.createFont();
        fuente.setFontName("Arial");
        fuente.setFontHeightInPoints((short) 12);
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);

        return estilo;
    }

    /**
     * Crea un estilo personalizado para las filas de datos en el Excel.
     *
     * @param libro El libro de trabajo Excel donde se aplicará el estilo
     * @return Estilo configurado para filas de datos
     */
    private XSSFCellStyle crearEstiloFila(XSSFWorkbook libro) {
        XSSFCellStyle estilo = libro.createCellStyle();
        // Bordes ligeros
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        // Alineación
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        // Fuente
        XSSFFont fuente = libro.createFont();
        fuente.setFontName("Arial");
        fuente.setFontHeightInPoints((short) 11);
        estilo.setFont(fuente);

        return estilo;
    }

    /**
     * Crea un estilo personalizado para celdas que contienen montos.
     *
     * @param libro El libro de trabajo Excel donde se aplicará el estilo
     * @return Estilo configurado para montos
     */
    private XSSFCellStyle crearEstiloMonto(XSSFWorkbook libro) {
        XSSFCellStyle estilo = libro.createCellStyle();
        // Bordes ligeros
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        // Alineación
        estilo.setAlignment(HorizontalAlignment.RIGHT);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        // Formato de número
        DataFormat formato = libro.createDataFormat();
        estilo.setDataFormat(formato.getFormat("#,##0"));
        // Fuente
        XSSFFont fuente = libro.createFont();
        fuente.setFontName("Arial");
        fuente.setFontHeightInPoints((short) 11);
        estilo.setFont(fuente);

        return estilo;
    }

    /**
     * Agrega una hoja adicional con información resumida de las ventas.
     *
     * @param libro      El libro de trabajo Excel donde se agregará la información
     * @param totalFilas El número total de registros exportados
     */
    private void agregarInformacionResumen(XSSFWorkbook libro, int totalFilas) {
        XSSFSheet hojaResumen = libro.createSheet("Resumen");

        // Estilos para el resumen
        XSSFCellStyle estiloTitulo = libro.createCellStyle();
        XSSFFont fuenteTitulo = libro.createFont();
        fuenteTitulo.setFontName("Arial");
        fuenteTitulo.setFontHeightInPoints((short) 14);
        fuenteTitulo.setBold(true);
        estiloTitulo.setFont(fuenteTitulo);

        XSSFCellStyle estiloSubtitulo = libro.createCellStyle();
        XSSFFont fuenteSubtitulo = libro.createFont();
        fuenteSubtitulo.setFontName("Arial");
        fuenteSubtitulo.setFontHeightInPoints((short) 12);
        fuenteSubtitulo.setBold(true);
        estiloSubtitulo.setFont(fuenteSubtitulo);

        XSSFCellStyle estiloValor = libro.createCellStyle();
        XSSFFont fuenteValor = libro.createFont();
        fuenteValor.setFontName("Arial");
        fuenteValor.setFontHeightInPoints((short) 12);
        estiloValor.setFont(fuenteValor);

        // Título del reporte
        Row filaTitulo = hojaResumen.createRow(0);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("RESUMEN DEL REPORTE DE VENTAS");
        celdaTitulo.setCellStyle(estiloTitulo);

        // Información general
        Row filaFecha = hojaResumen.createRow(2);
        Cell celdaFechaLabel = filaFecha.createCell(0);
        celdaFechaLabel.setCellValue("Fecha de generación:");
        celdaFechaLabel.setCellStyle(estiloSubtitulo);

        Cell celdaFechaValor = filaFecha.createCell(1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        celdaFechaValor.setCellValue(dateFormat.format(new Date()));
        celdaFechaValor.setCellStyle(estiloValor);

        // Total de registros
        Row filaRegistros = hojaResumen.createRow(3);
        Cell celdaRegistrosLabel = filaRegistros.createCell(0);
        celdaRegistrosLabel.setCellValue("Total de registros exportados:");
        celdaRegistrosLabel.setCellStyle(estiloSubtitulo);

        Cell celdaRegistrosValor = filaRegistros.createCell(1);
        celdaRegistrosValor.setCellValue(totalFilas);
        celdaRegistrosValor.setCellStyle(estiloValor);

        // Estadísticas
        Row filaEstadisticas = hojaResumen.createRow(5);
        Cell celdaEstadisticasLabel = filaEstadisticas.createCell(0);
        celdaEstadisticasLabel.setCellValue("ESTADÍSTICAS");
        celdaEstadisticasLabel.setCellStyle(estiloTitulo);

        // Ventas totales
        Row filaVentasTotales = hojaResumen.createRow(7);
        Cell celdaVentasTotalesLabel = filaVentasTotales.createCell(0);
        celdaVentasTotalesLabel.setCellValue("Ventas totales:");
        celdaVentasTotalesLabel.setCellStyle(estiloSubtitulo);

        Cell celdaVentasTotalesValor = filaVentasTotales.createCell(1);
        celdaVentasTotalesValor.setCellValue(txtVentaMesPanel.getText().replace("$", "").replace(".", ""));
        celdaVentasTotalesValor.setCellStyle(estiloValor);

        // Cantidad de ventas
        Row filaCantidadVentas = hojaResumen.createRow(8);
        Cell celdaCantidadVentasLabel = filaCantidadVentas.createCell(0);
        celdaCantidadVentasLabel.setCellValue("Cantidad de ventas:");
        celdaCantidadVentasLabel.setCellStyle(estiloSubtitulo);

        Cell celdaCantidadVentasValor = filaCantidadVentas.createCell(1);
        celdaCantidadVentasValor.setCellValue(txtnVentasMes.getText());
        celdaCantidadVentasValor.setCellStyle(estiloValor);

        // Promedio por venta
        Row filaPromedio = hojaResumen.createRow(9);
        Cell celdaPromedioLabel = filaPromedio.createCell(0);
        celdaPromedioLabel.setCellValue("Promedio por venta:");
        celdaPromedioLabel.setCellStyle(estiloSubtitulo);

        Cell celdaPromedioValor = filaPromedio.createCell(1);
        celdaPromedioValor.setCellValue(txtPromedioVenta.getText().replace("$", "").replace(".", ""));
        celdaPromedioValor.setCellStyle(estiloValor);

        // Productos vendidos
        Row filaProductos = hojaResumen.createRow(10);
        Cell celdaProductosLabel = filaProductos.createCell(0);
        celdaProductosLabel.setCellValue("Productos vendidos:");
        celdaProductosLabel.setCellStyle(estiloSubtitulo);

        Cell celdaProductosValor = filaProductos.createCell(1);
        celdaProductosValor.setCellValue(txtProdVen.getText());
        celdaProductosValor.setCellStyle(estiloValor);

        // Ajustar el ancho de las columnas
        hojaResumen.autoSizeColumn(0);
        hojaResumen.autoSizeColumn(1);
    }

    /**
     * Abre el archivo Excel generado utilizando la aplicación predeterminada
     * del sistema.
     *
     * @param archivo El archivo a abrir
     */
    private void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            } else {
                Toast.show(this, Toast.Type.WARNING, "No se puede abrir el archivo automáticamente");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al abrir el archivo", e);
            Toast.show(this, Toast.Type.WARNING, "No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    /**
     * *****************************************************CODIGO
     * EXCEL****************************************************************
     *
     * /**
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
        LocalDate now = LocalDate.now();
        Map<String, Object> stats = reporteVentas.obtenerEstadisticasVentasMes(now.getYear(), now.getMonthValue());

        // Formatear y mostrar valores
        String ventasMes = formatoMoneda.format(stats.getOrDefault("total_ventas", 0));
        txtVentaMesPanel.setText("$" + ventasMes);

        int cantidadVentas = Integer.parseInt(stats.getOrDefault("cantidad_ventas", 0).toString());
        txtnVentasMes.setText(String.valueOf(cantidadVentas));

        txtProdVen.setText(stats.getOrDefault("total_productos_vendidos", 0).toString());

        // Si están disponibles los valores de promedio
        if (stats.containsKey("promedio_venta")) {
            String promedio = formatoMoneda.format(stats.get("promedio_venta"));
            txtPromedioVenta.setText("$" + promedio);
        }
    }

    /*
     * Reporte de venta
     */
    private void exportarPDF() {
        try {
            // Configurar el cuadro de diálogo para guardar
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar reporte PDF");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf");
            fileChooser.setFileFilter(filter);

            // Sugerir nombre con fecha actual
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String fechaActual = sdf.format(new Date());
            fileChooser.setSelectedFile(new File("Reporte_Ventas_" + fechaActual + ".pdf"));

            int seleccion = fileChooser.showSaveDialog(this);
            if (seleccion != JFileChooser.APPROVE_OPTION) {
                return;
            }

            // Asegurar que el archivo tenga extensión .pdf
            String rutaArchivo = fileChooser.getSelectedFile().getAbsolutePath();
            if (!rutaArchivo.toLowerCase().endsWith(".pdf")) {
                rutaArchivo += ".pdf";
            }

            // Mostrar indicador de progreso
            Toast.show(this, Toast.Type.INFO, "Generando reporte PDF...");

            // Preparar los parámetros para el reporte
            Map<String, Object> parametros = new HashMap<>();

            // Parámetros de filtro
            LocalDate fechaInicio = null;
            LocalDate fechaFin = null;
            Integer idCliente = null;
            String estado = null;

            // Obtener filtros seleccionados
            if (datePicker.isDateSelected() && datePicker1.isDateSelected()) {
                fechaInicio = datePicker.getSelectedDate();
                fechaFin = datePicker1.getSelectedDate();
                parametros.put("fechaInicio", java.sql.Date.valueOf(fechaInicio));
                parametros.put("fechaFin", java.sql.Date.valueOf(fechaFin));
            } else {
                parametros.put("fechaInicio", null);
                parametros.put("fechaFin", null);
            }

            // Cliente seleccionado
            String clienteSeleccionado = cbxCliente.getSelectedItem().toString();
            if (!clienteSeleccionado.equals("Seleccionar")) {
                idCliente = serviceCliente.getIdByNombre(clienteSeleccionado);
                parametros.put("idCliente", idCliente);
            } else {
                parametros.put("idCliente", null);
            }

            // Estado seleccionado
            String estadoSeleccionado = cbxEstado.getSelectedItem().toString();
            if (!estadoSeleccionado.equals("Seleccionar")) {
                estado = estadoSeleccionado;
                parametros.put("estado", estado);
            } else {
                parametros.put("estado", null);
            }

            // Usuario - Bodega

            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();

            parametros.put("bodega", idBodega);

            // Construir descripción de filtros para mostrar en el reporte
            StringBuilder filtrosTexto = new StringBuilder();

            if (fechaInicio != null && fechaFin != null) {
                filtrosTexto.append("Período: ")
                        .append(fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .append(" a ")
                        .append(fechaFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }

            if (idCliente != null) {
                if (filtrosTexto.length() > 0) {
                    filtrosTexto.append(", ");
                }
                filtrosTexto.append("Cliente: ").append(clienteSeleccionado);
            }

            if (estado != null) {
                if (filtrosTexto.length() > 0) {
                    filtrosTexto.append(", ");
                }
                filtrosTexto.append("Estado: ").append(estadoSeleccionado);
            }

            if (filtrosTexto.length() == 0) {
                filtrosTexto.append("Todas las ventas");
            }

            parametros.put("filtrosDescripcion", filtrosTexto.toString());
            parametros.put("titulo", "REPORTE DE VENTAS");

            // Establecer conexión a la base de datos
            Connection conn = null;
            try {
                conn = conexion.getInstance().createConnection();

                // Cargar y compilar el reporte desde .jrxml
                InputStream jrxmlStream = getClass().getResourceAsStream("/raven/reportes/reporteVentasVenta.jrxml");
                if (jrxmlStream == null) {
                    throw new IOException("Plantilla de reporte no encontrada");
                }

                JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

                // Llenar el reporte con los parámetros y la conexión
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, conn);

                // Exportar a PDF
                JasperExportManager.exportReportToPdfFile(jasperPrint, rutaArchivo);

                // Mostrar mensaje de éxito
                Toast.show(this, Toast.Type.SUCCESS, "PDF generado en: " + rutaArchivo);

                // Abrir el archivo automáticamente
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(rutaArchivo));
                }

            } finally {
                // Cerrar conexión
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Error al cerrar conexión", e);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar PDF", e);
            Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
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
        Map<String, Object> stats = reporteVentas.generarResumenVentas(fechaInicio, fechaFin);

        // Actualizar los valores con comprobación de nulidad
        txtVentaMesPanel.setText(stats.containsKey("monto_total")
                ? "$" + formatoMoneda.format(stats.get("monto_total"))
                : "$0");

        txtnVentasMes.setText(stats.containsKey("total_ventas")
                ? formatoMoneda.format(stats.get("total_ventas"))
                : "0");

        txtPromedioVenta.setText(stats.containsKey("promedio_venta")
                ? "$" + formatoMoneda.format(stats.get("promedio_venta"))
                : "$0");

        // Actualizar productos vendidos si está disponible
        if (stats.containsKey("total_productos_vendidos")) {
            txtProdVen.setText(formatoMoneda.format(stats.get("total_productos_vendidos")));
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
        btnExportPdf = new javax.swing.JButton();
        btnExportExcel = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        panelProdVend = new javax.swing.JPanel();
        tituloProdVENDI = new javax.swing.JLabel();
        txtProdVen = new javax.swing.JLabel();
        txtFecha = new javax.swing.JLabel();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lb.setText("  REPORTE DE VENTAS");

        panelVentasMes.setBackground(new java.awt.Color(204, 204, 204));

        tituloVentasTot.setText("VENTAS TOTALES");

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

        tituloCantidadVentas.setText("CANTIDAD DE VENTAS");

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

        tituloPromedio.setText("Promedio venta");

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
                                        .addComponent(txtFechaIn, javax.swing.GroupLayout.DEFAULT_SIZE, 118,
                                                Short.MAX_VALUE)
                                        .addComponent(lbltitle))
                                .addGap(30, 30, 30)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtFechaFin, javax.swing.GroupLayout.DEFAULT_SIZE, 121,
                                                Short.MAX_VALUE)
                                        .addComponent(lbltitle2))
                                .addGap(30, 30, 30)
                                .addGroup(panelfiltroVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(cbxCliente, 0, 123, Short.MAX_VALUE)
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

        tituloListado.setText("Listado de ventas");

        btnExportPdf.setText("PDF");
        btnExportPdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportPdfActionPerformed(evt);
            }
        });

        btnExportExcel.setText("Excel");
        btnExportExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportExcelActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null }
                },
                new String[] {
                        "ID", "Fecha", "Cliente", "Estado", "Tipo Pago", "Total", "Acciones"
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
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnExportExcel, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnExportPdf, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelListadoVentasLayout.createSequentialGroup()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 860,
                                                Short.MAX_VALUE)
                                        .addContainerGap()));
        panelListadoVentasLayout.setVerticalGroup(
                panelListadoVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelListadoVentasLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(panelListadoVentasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(tituloListado, javax.swing.GroupLayout.PREFERRED_SIZE, 48,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(panelListadoVentasLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnExportPdf, javax.swing.GroupLayout.PREFERRED_SIZE, 45,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnExportExcel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(75, Short.MAX_VALUE)));

        panelProdVend.setBackground(new java.awt.Color(204, 204, 204));

        tituloProdVENDI.setText("Productos vendidos");

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
                                                        324, Short.MAX_VALUE)
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
                                        .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 895,
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
            // Obtener fechas del DatePicker
            LocalDate fechaInicio = datePicker.isDateSelected() ? datePicker.getSelectedDate() : null;
            LocalDate fechaFin = datePicker1.isDateSelected() ? datePicker1.getSelectedDate() : null;

            // Validar que si una fecha está seleccionada, la otra también lo esté
            if ((fechaInicio != null && fechaFin == null) || (fechaInicio == null && fechaFin != null)) {
                Toast.show(this, Toast.Type.INFO, "Debe seleccionar ambas fechas o ninguna");
                return;
            }

            // Validar rango de fechas
            if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
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

            // Filtrar ventas según los parámetros
            List<Map<String, Object>> ventasFiltradas = reporteVentas.filtrarVentas(fechaInicio, fechaFin, idCliente,
                    estado);

            // Cargar las ventas en la tabla
            cargarVentas(ventasFiltradas);

            // Actualizar estadísticas si hay fechas seleccionadas
            if (fechaInicio != null && fechaFin != null) {
                actualizarEstadisticas(fechaInicio, fechaFin);
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
        // Cargar todas las ventas
        List<Map<String, Object>> todasLasVentas = reporteVentas.filtrarVentas(null, null, -1, null);
        cargarVentas(todasLasVentas);
        Toast.show(this, Toast.Type.INFO, "Filtros reiniciados correctamente");

    }// GEN-LAST:event_btnReiniciarActionPerformed

    private void btnExportPdfActionPerformed(java.awt.event.ActionEvent evt) {
        generarReporteDetallado("pdf");
    }

    private void btnExportExcelActionPerformed(java.awt.event.ActionEvent evt) {
        generarReporteDetallado("excel");
    }

    private void generarReporteDetallado(String tipo) {
        try {
            // 1. Obtener filtros actuales (igual que btnFiltrar)
            LocalDate fechaInicio = datePicker.isDateSelected() ? datePicker.getSelectedDate() : null;
            LocalDate fechaFin = datePicker1.isDateSelected() ? datePicker1.getSelectedDate() : null;
            String clienteSeleccionado = cbxCliente.getSelectedItem().toString();
            int idCliente = clienteSeleccionado.equals("Seleccionar") ? -1
                    : serviceCliente.getIdByNombre(clienteSeleccionado);
            String estadoSeleccionado = cbxEstado.getSelectedItem().toString();
            String estado = estadoSeleccionado.equals("Seleccionar") ? null : estadoSeleccionado;

            // 2. Obtener ventas filtradas
            List<Map<String, Object>> ventas = reporteVentas.filtrarVentas(fechaInicio, fechaFin, idCliente, estado);

            if (ventas == null || ventas.isEmpty()) {
                Toast.show(this, Toast.Type.WARNING, "No hay datos para generar el reporte");
                return;
            }

            // 3. Preparar archivo de destino
            JFileChooser fileChooser = new JFileChooser();
            String ext = tipo.equals("excel") ? "xlsx" : "pdf";
            String desc = tipo.equals("excel") ? "Archivos Excel (*.xlsx)" : "Archivos PDF (*.pdf)";

            fileChooser.setDialogTitle("Guardar Reporte Detallado de Ventas");
            FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, ext);
            fileChooser.setFileFilter(filter);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
            String fechaActual = sdf.format(new Date());
            fileChooser.setSelectedFile(new File("Reporte_Ventas_Detallado_" + fechaActual + "." + ext));

            int seleccion = fileChooser.showSaveDialog(this);
            if (seleccion != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith("." + ext)) {
                archivo = new File(archivo.getAbsolutePath() + "." + ext);
            }

            // 4. Mostrar loading y procesar en background
            final File finalArchivo = archivo;
            final List<Map<String, Object>> finalVentas = ventas;

            // Construir string de filtros para el reporte
            StringBuilder sbFiltros = new StringBuilder();
            if (fechaInicio != null)
                sbFiltros.append("Desde: ").append(fechaInicio).append(" ");
            if (fechaFin != null)
                sbFiltros.append("Hasta: ").append(fechaFin).append(" ");
            if (idCliente != -1)
                sbFiltros.append("Cliente: ").append(clienteSeleccionado).append(" ");
            if (estado != null)
                sbFiltros.append("Estado: ").append(estado);
            if (sbFiltros.length() == 0)
                sbFiltros.append("Todos los registros");
            final String filtrosStr = sbFiltros.toString();

            Toast.show(this, Toast.Type.INFO, "Generando reporte detallado, por favor espere...");

            new javax.swing.SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    List<VentaReporteDTO> listaDTO = new java.util.ArrayList<>();

                    for (Map<String, Object> v : finalVentas) {
                        int idVenta = (int) v.get("id_venta");
                        // Obtener detalles de productos
                        List<Map<String, Object>> detalles = reporteVentas.generarDetalleVenta(idVenta);

                        // Parsear datos de la venta de forma segura
                        String fecha = v.get("fecha_venta") != null ? v.get("fecha_venta").toString() : "";
                        String cliente = v.get("cliente") != null ? v.get("cliente").toString() : "";
                        String vendedor = v.get("vendedor") != null ? v.get("vendedor").toString() : "";
                        String est = v.get("estado") != null ? v.get("estado").toString() : "";
                        String pago = v.get("tipo_pago") != null ? v.get("tipo_pago").toString() : "";
                        double total = v.get("total") instanceof Number ? ((Number) v.get("total")).doubleValue() : 0.0;

                        listaDTO.add(
                                new VentaReporteDTO(idVenta, fecha, cliente, vendedor, est, pago, total, detalles));
                    }

                    if (tipo.equals("excel")) {
                        return new ExcelVentasExporter().exportar(listaDTO, finalArchivo, filtrosStr);
                    } else {
                        return new ReporteVentasGeneralPDF().generarReporte(listaDTO, finalArchivo, filtrosStr);
                    }
                }

                @Override
                protected void done() {
                    try {
                        boolean exito = get();
                        if (exito) {
                            Toast.show(reporteVentas.this, Toast.Type.SUCCESS, "Reporte guardado exitosamente");
                            // Opción de abrir
                            int resp = JOptionPane.showConfirmDialog(reporteVentas.this,
                                    "¿Desea abrir el archivo generado?", "Reporte Generado", JOptionPane.YES_NO_OPTION);
                            if (resp == JOptionPane.YES_OPTION) {
                                try {
                                    Desktop.getDesktop().open(finalArchivo);
                                } catch (Exception ex) {
                                    // ignore
                                }
                            }
                        } else {
                            Toast.show(reporteVentas.this, Toast.Type.ERROR, "Error al generar el reporte");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.show(reporteVentas.this, Toast.Type.ERROR, "Error interno: " + e.getMessage());
                    }
                }
            }.execute();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al iniciar exportación: " + e.getMessage());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel PanelNventas;
    private javax.swing.JButton btnExportExcel;
    private javax.swing.JButton btnExportPdf;
    private javax.swing.JButton btnFiltrar;
    private javax.swing.JButton btnReiniciar;
    private javax.swing.JComboBox<String> cbxCliente;
    private javax.swing.JComboBox<String> cbxEstado;
    private raven.datetime.component.date.DatePicker datePicker;
    private raven.datetime.component.date.DatePicker datePicker1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
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
    private javax.swing.JLabel tituloCantidadVentas;
    private javax.swing.JLabel tituloListado;
    private javax.swing.JLabel tituloProdVENDI;
    private javax.swing.JLabel tituloPromedio;
    private javax.swing.JLabel tituloVentasTot;
    private javax.swing.JLabel txtFecha;
    private javax.swing.JFormattedTextField txtFechaFin;
    private javax.swing.JFormattedTextField txtFechaIn;
    private javax.swing.JLabel txtProdVen;
    private javax.swing.JLabel txtPromedioVenta;
    private javax.swing.JLabel txtVentaMesPanel;
    private javax.swing.JLabel txtnVentasMes;
    // End of variables declaration//GEN-END:variables
}
