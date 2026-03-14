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
import java.util.ArrayList;
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
        FlatRobotoFont.install();
        lb.putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        aplicarEstiloPaneles();
        configurarEstiloEtiquetas();
        configurarEstiloTabla();
        configurarSelectorFechas();

        jPanel1.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background;");
        panelMain.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background;");

        scroll.putClientProperty(FlatClientProperties.STYLE,
                "border:0,0,0,0;background:$Panel.background;");
    }

    // =======================================================================
    // ESTILOS ADAPTATIVOS (LIGHT / DARK) — patron GestionProductosForm
    // =======================================================================

    private void aplicarEstiloPaneles() {
        // Botones de acción principal — colores semánticos via tokens FlatLaf
        btnFiltrar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;"
                + "background:$Component.accentColor;"
                + "foreground:#FFFFFF;"
                + "font:bold 12;"
                + "margin:8,15,8,15;"
                + "hoverBackground:lighten($Component.accentColor,10%);"
                + "pressedBackground:darken($Component.accentColor,10%);");

        btnReiniciar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;"
                + "background:#EF5350;"
                + "foreground:#FFFFFF;"
                + "font:bold 12;"
                + "margin:8,15,8,15;"
                + "hoverBackground:#F44336;"
                + "pressedBackground:#D32F2F;");

        BtnFacturar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;borderWidth:0;"
                + "background:#42A5F5;"
                + "foreground:#FFFFFF;"
                + "font:bold 12;"
                + "margin:8,15,8,15;"
                + "hoverBackground:#64B5F6;"
                + "pressedBackground:#1E88E5;");

        // Scrollbar moderna y minimalista (igual que GestionProductosForm)
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;"
                + "trackInsets:3,3,3,3;"
                + "thumbInsets:3,3,3,3;"
                + "background:$Panel.background;"
                + "thumb:$Component.borderColor;"
                + "hoverThumbColor:$Component.accentColor;"
                + "pressedThumbColor:darken($Component.accentColor,10%);");

        // Paneles de estadísticas con borde sutil adaptativo
        String panelEstadisticaStyle =
                "arc:18;"
                + "background:lighten($Panel.background,3%);"
                + "border:1,1,1,1,$Component.borderColor,1,15;";

        panelVentasMes.putClientProperty(FlatClientProperties.STYLE, panelEstadisticaStyle);
        PanelNventas.putClientProperty(FlatClientProperties.STYLE, panelEstadisticaStyle);
        panelprodVendidos.putClientProperty(FlatClientProperties.STYLE, panelEstadisticaStyle);
        panelProdVend.putClientProperty(FlatClientProperties.STYLE, panelEstadisticaStyle);

        // Panel de filtros con borde de acento suave
        panelfiltroVentas.putClientProperty(FlatClientProperties.STYLE,
                "arc:18;"
                + "background:lighten($Panel.background,3%);"
                + "border:1,1,1,1,lighten($Component.accentColor,30%),1,15;");

        // Panel listado con borde estándar
        panelListadoVentas.putClientProperty(FlatClientProperties.STYLE,
                "arc:18;"
                + "background:lighten($Panel.background,3%);"
                + "border:1,1,1,1,$Component.borderColor,1,15;");
    }

    private void configurarEstiloEtiquetas() {
        // Títulos de estadísticas
        String tituloEstadisticaStyle = "font:$h3.font;foreground:$Label.foreground;";
        tituloCantidadVentas.putClientProperty(FlatClientProperties.STYLE, tituloEstadisticaStyle);
        tituloVentasTot.putClientProperty(FlatClientProperties.STYLE, tituloEstadisticaStyle);
        tituloPromedio.putClientProperty(FlatClientProperties.STYLE, tituloEstadisticaStyle);
        tituloProdVENDI.putClientProperty(FlatClientProperties.STYLE, tituloEstadisticaStyle);

        // Título principal del listado con color de acento
        tituloListado.putClientProperty(FlatClientProperties.STYLE,
                "font:bold $h2.font;foreground:$Component.accentColor;");

        // Valores numéricos grandes — color accent del tema
        String valorEstadisticaStyle = "font:bold +8;foreground:$Component.accentColor;";
        txtVentaMesPanel.putClientProperty(FlatClientProperties.STYLE, valorEstadisticaStyle);
        txtnVentasMes.putClientProperty(FlatClientProperties.STYLE, valorEstadisticaStyle);
        txtPromedioVenta.putClientProperty(FlatClientProperties.STYLE, valorEstadisticaStyle);
        txtProdVen.putClientProperty(FlatClientProperties.STYLE, valorEstadisticaStyle);

        // Etiquetas de filtros
        String boldStyle = "font:bold +1;foreground:$Label.foreground;";
        lbltitle.putClientProperty(FlatClientProperties.STYLE, boldStyle);
        lbltitle2.putClientProperty(FlatClientProperties.STYLE, boldStyle);
        lbltitle3.putClientProperty(FlatClientProperties.STYLE, boldStyle);
        lbltitle4.putClientProperty(FlatClientProperties.STYLE, boldStyle);

        // Campos de texto con estilo adaptativo
        String textFieldStyle =
                "arc:10;"
                + "borderWidth:1;"
                + "borderColor:$Component.borderColor;"
                + "focusWidth:2;"
                + "focusedBorderColor:$Component.accentColor;"
                + "innerFocusWidth:0;"
                + "background:lighten($Panel.background,5%);";
        txtFechaIn.putClientProperty(FlatClientProperties.STYLE, textFieldStyle);
        txtFechaFin.putClientProperty(FlatClientProperties.STYLE, textFieldStyle);

        // ComboboxEs adaptativos
        String comboStyle =
                "arc:10;"
                + "borderWidth:1;"
                + "borderColor:$Component.borderColor;"
                + "background:lighten($Panel.background,5%);";
        cbxCliente.putClientProperty(FlatClientProperties.STYLE, comboStyle);
        cbxEstado.putClientProperty(FlatClientProperties.STYLE, comboStyle);
    }

    private void configurarEstiloTabla() {
        // Tabla principal — tokens semánticos, se adaptan a light/dark
        jTable1.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                + "showVerticalLines:false;"
                + "rowHeight:52;"
                + "intercellSpacing:0,6;"
                + "cellFocusColor:$Component.accentColor;"
                + "selectionBackground:lighten($Component.accentColor,35%);"
                + "selectionForeground:$Table.foreground;"
                + "selectionInactiveBackground:lighten($Component.accentColor,50%);"
                + "background:$Panel.background;"
                + "font:+1;");

        // Encabezado de tabla — igual que GestionProductosForm
        jTable1.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:45;"
                + "hoverBackground:lighten($Component.accentColor,40%);"
                + "pressedBackground:lighten($Component.accentColor,30%);"
                + "separatorColor:$Component.borderColor;"
                + "background:darken($Panel.background,3%);"
                + "foreground:$Label.foreground;"
                + "font:bold +2;");

        // ScrollPane de la tabla sin borde
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE,
                "border:0,0,0,0;"
                + "background:$Panel.background;");
    }

    private void configurarSelectorFechas() {
        datePicker.setCloseAfterSelected(true);
        datePicker.setEditor(txtFechaIn);
        datePicker1.setEditor(txtFechaFin);
        datePicker1.setCloseAfterSelected(true);
    }

    /**
     * Crea el renderizador de estado con badges de color adaptativo.
     * Los colores de fondo usan hex fijo para los badges (intención semántica),
     * pero el fondo de celda usa siempre los tokens del tema.
     */
    private DefaultTableCellRenderer crearRenderizadorEstado() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setOpaque(true);

                // Fondo de celda siempre del tema
                label.setBackground(isSelected
                        ? table.getSelectionBackground()
                        : table.getBackground());

                if (value != null) {
                    String estado = value.toString().toLowerCase();
                    String[] colores = getColorForStatus(estado);
                    String bgColor   = colores[0];
                    String textColor = colores[1];

                    String strikethrough = (estado.equals("cotizacion_rechazada") || estado.equals("cancelada"))
                            ? "text-decoration:line-through;" : "";

                    label.setText(
                        "<html><div style='padding:5px 12px;border-radius:10px;"
                        + "display:inline-block;font-size:11px;font-weight:600;"
                        + "letter-spacing:0.3px;"
                        + strikethrough
                        + "background-color:" + bgColor + ";"
                        + "color:" + textColor + ";'>"
                        + value.toString().replace("_", " ")
                        + "</div></html>");
                }
                return label;
            }

            /** Retorna [bgColor, textColor] según el estado */
            private String[] getColorForStatus(String status) {
                switch (status) {
                    case "retirado":             return new String[]{"#2196F3", "#FFFFFF"};
                    case "pagado":               return new String[]{"#FF9800", "#FFFFFF"};
                    case "entregado":
                    case "enviado":              return new String[]{"#03A9F4", "#FFFFFF"};
                    case "finalizado":
                    case "completada":           return new String[]{"#4CAF50", "#FFFFFF"};
                    case "cancelada":
                    case "cotizacion_rechazada": return new String[]{"#EF5350", "#FFFFFF"};
                    case "pendiente":            return new String[]{"#FFC107", "#212121"};
                    case "cotizacion":           return new String[]{"#42A5F5", "#FFFFFF"};
                    case "cotizacion_aprobada":  return new String[]{"#66BB6A", "#FFFFFF"};
                    case "cotizacion_convertida":return new String[]{"#AB47BC", "#FFFFFF"};
                    default:                     return new String[]{"#9E9E9E", "#FFFFFF"};
                }
            }
        };
    }

    /**
     * Configura el selector de estado en la tabla.
     * Implementa restricciones para impedir la edición en estados específicos.
     */
    private void configurarSelectorEstado() throws SQLException {
        try {
            conexion.getInstance().connectToDatabase();
            try (Connection conexionBD = conexion.getInstance().createConnection()) {
                if (conexionBD == null) {
                    throw new SQLException("No se pudo establecer la conexión a la base de datos");
                }
            }

            final String[] opcionesEstadoReserva = {"pendiente", "retirado", "pagado", "entregado", "enviado", "finalizado", "cancelada"};
            final String[] opcionesEstadoCotizacion = {"cotizacion", "cotizacion_aprobada", "cotizacion_rechazada", "cotizacion_convertida"};
            final String[] opcionesEstadoVenta = {"pendiente", "completada", "cancelada"};

            TableColumn columnaEstado = jTable1.getColumnModel().getColumn(5);
            final JComboBox<String> comboBoxEstado = new JComboBox<>();

            DefaultCellEditor editorEstado = new DefaultCellEditor(comboBoxEstado) {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value,
                        boolean isSelected, int row, int column) {
                    Object idVal = jTable1.getValueAt(row, 1);
                    String idStr = idVal != null ? idVal.toString() : "";
                    comboBoxEstado.removeAllItems();
                    if (idStr.startsWith("OR-")) {
                        for (String estado : opcionesEstadoReserva) comboBoxEstado.addItem(estado);
                    } else if (verificarSiEsCotizacion(row)) {
                        for (String estado : opcionesEstadoCotizacion) comboBoxEstado.addItem(estado);
                    } else {
                        for (String estado : opcionesEstadoVenta) comboBoxEstado.addItem(estado);
                    }
                    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                @Override
                public boolean isCellEditable(EventObject anEvent) {
                    if (anEvent instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) anEvent;
                        int row = jTable1.rowAtPoint(me.getPoint());
                        return isRowEditable(row) && super.isCellEditable(anEvent);
                    }
                    return super.isCellEditable(anEvent);
                }
            };

            editorEstado.addCellEditorListener(new CellEditorListener() {
                @Override
                public void editingStopped(ChangeEvent e) {
                    int filaSeleccionada = jTable1.getSelectedRow();
                    if (filaSeleccionada != -1) {
                        Object valorEstado = jTable1.getValueAt(filaSeleccionada, 5);
                        if (valorEstado == null) return;
                        String nuevoEstado = valorEstado.toString();
                        String idStr = jTable1.getValueAt(filaSeleccionada, 1).toString();
                        try {
                            if (idStr.startsWith("OR-")) {
                                int idOrden = Integer.parseInt(idStr.substring(3));
                                if (nuevoEstado.equals("cancelada")) {
                                    int respuesta = JOptionPane.showConfirmDialog(Carrito.this,
                                            "¿Está seguro de cancelar la orden " + idStr + "?",
                                            "Confirmar cancelación",
                                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                    if (respuesta != JOptionPane.YES_OPTION) {
                                        String originalEstado = obtenerEstadoOriginal(idOrden);
                                        jTable1.setValueAt(originalEstado, filaSeleccionada, 4);
                                        return;
                                    }
                                }
                                boolean exito = ordenReservaService.actualizarEstadoOrden(idOrden, nuevoEstado);
                                if (exito) {
                                    Toast.show(Carrito.this, Toast.Type.SUCCESS, "Estado actualizado a " + nuevoEstado);
                                    if ("finalizado".equals(nuevoEstado)) {
                                        int respuestaConversion = JOptionPane.showConfirmDialog(Carrito.this,
                                                "¿Desea convertir esta orden finalizada a venta?",
                                                "Convertir a Venta",
                                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                                        if (respuestaConversion == JOptionPane.YES_OPTION) {
                                            convertirOrdenAVenta(idOrden);
                                        }
                                    }
                                    cargarEstadisticasMesActual();
                                }
                            } else {
                                int idVenta = Integer.parseInt(idStr);
                                String originalEstado = null;
                                if (nuevoEstado.equals("cancelada") || nuevoEstado.equals("cotizacion_rechazada")) {
                                    originalEstado = obtenerEstadoOriginal(idVenta);
                                    int respuesta = JOptionPane.showConfirmDialog(Carrito.this,
                                            "¿Está seguro de cambiar el estado a " + nuevoEstado + "?",
                                            "Confirmar cambio de estado",
                                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                    if (respuesta != JOptionPane.YES_OPTION) {
                                        jTable1.setValueAt(originalEstado, filaSeleccionada, 4);
                                        return;
                                    }
                                }
                                actualizarEstadoDocumento(idVenta, nuevoEstado);
                                boolean esCotizacion = verificarSiEsCotizacion(filaSeleccionada);
                                if (esCotizacion && nuevoEstado.equals("cotizacion_aprobada")) {
                                    int respuestaConversion = JOptionPane.showConfirmDialog(Carrito.this,
                                            "¿Desea convertir esta cotización aprobada a venta?",
                                            "Convertir a Venta",
                                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                                    if (respuestaConversion == JOptionPane.YES_OPTION) {
                                        Toast.show(Carrito.this, Toast.Type.INFO,
                                                "Esta función se encuentra en desarrollo para cotizaciones");
                                    }
                                }
                            }
                            if (nuevoEstado.equals("cotizacion_rechazada") || nuevoEstado.equals("cancelada")
                                    || nuevoEstado.equals("cotizacion_convertida") || nuevoEstado.equals("finalizado")) {
                                jTable1.repaint();
                            }
                        } catch (SQLException ex) {
                            LOGGER.log(Level.SEVERE, "Error al procesar cambio de estado", ex);
                            Toast.show(Carrito.this, Toast.Type.ERROR, "Error: " + ex.getMessage());
                            cargarOrdenesReserva();
                        }
                    }
                }

                private String obtenerEstadoOriginal(int idOrden) throws SQLException {
                    String sql = "SELECT estado FROM ordenes_reserva WHERE id_orden = ?";
                    try (Connection con = conexion.getInstance().createConnection();
                            PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idOrden);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) return rs.getString("estado");
                            else throw new SQLException("No se encontró la orden con ID: " + idOrden);
                        }
                    }
                }

                @Override
                public void editingCanceled(ChangeEvent e) { }
            });

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al configurar selector de estado", ex);
            JOptionPane.showMessageDialog(null,
                    "Error al conectar con la base de datos: " + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            throw ex;
        }
    }

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
            Object val = jTable1.getValueAt(row, 1);
            if (val == null) return false;
            String idOrdenStr = val.toString();
            if (idOrdenStr.startsWith("OR-")) {
                int idOrden = Integer.parseInt(idOrdenStr.substring(3));
                String sql = "SELECT estado FROM ordenes_reserva WHERE id_orden = ?";
                try (Connection con = conexion.getInstance().createConnection();
                        PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, idOrden);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String estado = rs.getString("estado");
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

    private void configurarTabla() throws SQLException {
        TableActionEvent event = crearEventoAccionesTabla();
        configurarSelectorEstado();
        configurarAnchoColumnas();

        jTable1.getColumnModel().getColumn(7).setCellRenderer(new TableActionCellRender());
        jTable1.getColumnModel().getColumn(7).setCellEditor(new TableActionCellEditor(event));

        // Columna 0: checkbox (N°)
        jTable1.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
                setHorizontalAlignment(SwingConstants.CENTER);
                Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
                if (!bln) c.setBackground(jtable.getBackground());
                return c;
            }
        });

        // Columna 1: ID con badge
        jTable1.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(jtable, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    String id = value.toString();
                    label.setText("<html><div style='padding:3px 8px;border-radius:8px;"
                            + "font-size:11px;font-weight:600;"
                            + "background-color:rgba(66,165,245,0.15);"
                            + "color:#42A5F5;'>" + id + "</div></html>");
                }
                if (!isSelected) label.setBackground(jtable.getBackground());
                return label;
            }
        });

        // Columna 2: fecha centrada
        jTable1.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                if (value instanceof Date) value = dateFormat.format((Date) value);
                setHorizontalAlignment(SwingConstants.CENTER);
                Component c = super.getTableCellRendererComponent(jtable, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(jtable.getBackground());
                return c;
            }
        });

        // Columna 3: cliente con padding izquierdo
        jTable1.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
                setHorizontalAlignment(SwingConstants.LEFT);
                Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
                if (c instanceof JLabel) ((JLabel) c).setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
                if (!bln) c.setBackground(jtable.getBackground());
                return c;
            }
        });

        // Columna 4: bodega centrada
        jTable1.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
                setHorizontalAlignment(SwingConstants.CENTER);
                Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
                if (!bln) c.setBackground(jtable.getBackground());
                return c;
            }
        });

        // Columna 5: estado con badge de color
        jTable1.getColumnModel().getColumn(5).setCellRenderer(crearRenderizadorEstado());

        // Columna 6: productos con icono
        jTable1.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(jtable, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    String texto = value.toString();
                    if (!texto.contains("📦")) label.setText("📦 " + texto);
                }
                if (!isSelected) label.setBackground(jtable.getBackground());
                return label;
            }
        });
    }

    private void configurarAnchoColumnas() {
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(50);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(80);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(110);
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(200);
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(120);
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(110);
        jTable1.getColumnModel().getColumn(6).setPreferredWidth(130);
        jTable1.getColumnModel().getColumn(7).setPreferredWidth(120);
        jTable1.getColumnModel().getColumn(8).setPreferredWidth(80);

        jTable1.getColumnModel().getColumn(0).setMinWidth(30);
        jTable1.getColumnModel().getColumn(0).setMaxWidth(60);
        jTable1.getColumnModel().getColumn(1).setMinWidth(60);
        jTable1.getColumnModel().getColumn(1).setMaxWidth(100);
        jTable1.getColumnModel().getColumn(3).setMinWidth(150);
        jTable1.getColumnModel().getColumn(8).setMinWidth(70);
        jTable1.getColumnModel().getColumn(8).setMaxWidth(100);
    }

    private boolean isRowEditable(int row) {
        if (row < 0 || row >= jTable1.getRowCount()) return false;
        int estadoCol = getEstadoColumnIndex(row);
        Object cellValue = jTable1.getValueAt(row, estadoCol);
        if (cellValue == null) return true;
        String estado = cellValue.toString().toLowerCase();
        return !estado.equals("cotizacion_rechazada")
                && !estado.equals("cancelada")
                && !estado.equals("cotizacion_convertida")
                && !estado.equals("finalizado");
    }

    private int getEstadoColumnIndex(int row) { return 5; }

    private String normalizarEstadoOrdenReserva(String estado) {
        if (estado == null) return "";
        String e = estado.trim().replaceAll("\\s+", " ");
        if (e.equalsIgnoreCase("pendiente productos")) return "pendiente";
        return e.toLowerCase();
    }

    private TableActionEvent crearEventoAccionesTabla() {
        return new TableActionEvent() {
            @Override
            public void onEdit(int row) {
                try {
                    if (!isRowEditable(row)) {
                        Toast.show(panelMain, Toast.Type.WARNING, "No se puede editar un documento cancelado o rechazado");
                        return;
                    }
                    String idStr = jTable1.getValueAt(row, 0).toString();
                    if (idStr.startsWith("OR-")) {
                        int idOrden = Integer.parseInt(idStr.substring(3));
                        mostrarDetalleOrdenReserva(idOrden);
                        return;
                    }
                    int idVenta = Integer.parseInt(idStr);
                    List<Map<String, Object>> detalles = reporteVentas.generarDetalleVenta(idVenta);
                    List<Map<String, Object>> ventas = reporteVentas.filtrarVentas(null, null, -1, null);
                    Map<String, Object> ventaInfo = ventas.stream()
                            .filter(v -> (int) v.get("id_venta") == idVenta)
                            .findFirst().orElse(null);
                    mostrarReporteVenta(idVenta, ventaInfo, detalles);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Error al cargar el detalle", e);
                    Toast.show(panelMain, Toast.Type.ERROR, "Error al cargar el detalle");
                }
            }

            @Override
            public void onDelete(int row) {
                String idStr = jTable1.getValueAt(row, 1).toString();
                if (idStr.startsWith("OR-")) {
                    int idOrden = Integer.parseInt(idStr.substring(3));
                    String estado = jTable1.getValueAt(row, 5).toString();
                    String estadoNormalizado = normalizarEstadoOrdenReserva(estado);
                    if (!"finalizado".equals(estadoNormalizado) && !"cancelada".equals(estadoNormalizado)) {
                        mostrarOpcionesEstadoOrden(idOrden, row);
                    } else {
                        Toast.show(panelMain, Toast.Type.WARNING, "Este documento ya se encuentra en un estado final");
                    }
                }
            }

            @Override
            public void onView(int row) {
                try {
                    Toast.show(panelMain, Toast.Type.INFO, "Cargando el reporte...");
                    String idStr = jTable1.getValueAt(row, 1).toString();
                    if (idStr.startsWith("OR-")) {
                        int idOrden = Integer.parseInt(idStr.substring(3));
                        mostrarDetalleOrdenReserva(idOrden);
                        return;
                    }
                    int idVenta = Integer.parseInt(idStr);
                    String estado = jTable1.getValueAt(row, 5).toString().toLowerCase();
                    if (estado.contains("cotizacion")) {
                        reporteVentas.generarCotizacion(idVenta, panelProdVend);
                    } else {
                        reporteVentas.llamarInforme(idVenta);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Error al cargar el reporte", e);
                    Toast.show(panelMain, Toast.Type.ERROR, "Error al cargar el reporte");
                }
            }

            public void onCaja(int row) { /* no usado */ }
        };
    }

    private void mostrarDetalleOrdenReserva(int idOrden) {
        try {
            raven.modelos.OrdenReserva orden = ordenReservaService.obtenerOrdenPorId(idOrden);
            List<raven.modelos.OrdenReservaDetalle> detalles = ordenReservaService.obtenerDetallesOrden(idOrden);
            if (orden == null) {
                Toast.show(this, Toast.Type.ERROR, "No se encontró la orden de reserva");
                return;
            }
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
            String[] opciones = {"Cargar en formulario", "Cerrar"};
            int seleccion = JOptionPane.showOptionDialog(this, info.toString(),
                    "Detalle Orden de Reserva", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
            if (seleccion == 0) cargarOrdenEnFormularioVenta(idOrden);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al mostrar detalle de orden", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar detalle: " + e.getMessage());
        }
    }

    private void mostrarOpcionesEstadoOrden(int idOrden, int row) {
        String[] opciones = {"RETIRADO", "PAGADO", "FINALIZADO", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(this,
                "Seleccione el nuevo estado para la orden:",
                "Cambiar Estado Orden", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (seleccion >= 0 && seleccion < 3) {
            try {
                String nuevoEstado = opciones[seleccion];
                boolean exito = ordenReservaService.cambiarEstadoOrden(idOrden, nuevoEstado);
                if (exito) {
                    String estadoMostrar = normalizarEstadoOrdenReserva(nuevoEstado);
                    jTable1.setValueAt(estadoMostrar, row, 4);
                    Toast.show(this, Toast.Type.SUCCESS, "Estado actualizado a " + estadoMostrar);
                    if ("finalizado".equals(estadoMostrar)) convertirOrdenAVenta(idOrden);
                } else {
                    Toast.show(this, Toast.Type.ERROR, "Error al actualizar el estado");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cambiar estado de orden", e);
                Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
            }
        } else if (seleccion == 3) {
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de cancelar la orden?", "Cancelar Orden",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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

    private void convertirOrdenAVenta(int idOrden) {
        try {
            int idVenta = ordenReservaService.convertirOrdenAVenta(idOrden);
            if (idVenta > 0) {
                Toast.show(this, Toast.Type.SUCCESS, "Orden convertida a venta exitosamente");
                reporteVentas.llamarInforme(idVenta);
                cargarDatosIniciales();
            } else {
                Toast.show(this, Toast.Type.ERROR, "Error al convertir la orden a venta");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al convertir orden a venta", e);
            Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
        }
    }

    private void cargarOrdenEnFormularioVenta(int idOrden) {
        try {
            generarVentaFor1 form = new generarVentaFor1();
            Application.showForm(form);
            form.cargarOrdenReservaEnFormulario(idOrden);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar orden en formulario", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar en formulario: " + e.getMessage());
        }
    }

    private void cargarDatosIniciales() {
        cargarComboClientes();
        cargarEstadisticasMesActual();
        cargarOrdenesReserva();
        aplicarSeleccionInicial();
    }

    public void cargarOrdenesReserva() {
        try {
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            List<Map<String, Object>> ordenes = ordenReservaService.obtenerTodasLasOrdenesParaTabla(idBodega);
            cargarOrdenes(ordenes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar órdenes de reserva", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cargar órdenes de reserva: " + e.getMessage());
        }
    }

    public void cargarOrdenes(List<Map<String, Object>> ordenes) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (Map<String, Object> orden : ordenes) {
            if (orden.get("id") == null || orden.get("fecha") == null || orden.get("cliente") == null
                    || orden.get("estado") == null || orden.get("productos") == null) {
                LOGGER.log(Level.WARNING, "Orden con datos incompletos omitida: " + orden);
                continue;
            }
            String id = String.valueOf(orden.get("id")).trim();
            String cliente = String.valueOf(orden.get("cliente")).trim();
            String estado = normalizarEstadoOrdenReserva(String.valueOf(orden.get("estado")));
            if (id.isEmpty() || cliente.isEmpty() || estado.isEmpty()) {
                LOGGER.log(Level.WARNING, "Orden con datos vacíos omitida: " + orden);
                continue;
            }
            String fechaEstado = "";
            if ("retirado".equals(estado) && orden.get("fecha_retirado") != null) {
                fechaEstado = orden.get("fecha_retirado").toString();
            } else if ("pagado".equals(estado) && orden.get("fecha_pagado") != null) {
                fechaEstado = orden.get("fecha_pagado").toString();
            } else if ("finalizado".equals(estado) && orden.get("fecha_finalizado") != null) {
                fechaEstado = orden.get("fecha_finalizado").toString();
            } else {
                fechaEstado = orden.get("fecha").toString();
            }
            model.addRow(new Object[]{
                false,
                orden.get("id"),
                orden.get("fecha"),
                orden.get("cliente"),
                orden.get("bodega"),
                estado,
                orden.get("productos"),
                fechaEstado,
                ""
            });
        }
        aplicarSeleccionInicial();
    }

    public void seleccionarOrdenPorReferencia(int idReferencia) {
        try {
            String objetivoConPrefijo = "OR-" + idReferencia;
            int filas = jTable1.getRowCount();
            for (int i = 0; i < filas; i++) {
                Object val = jTable1.getValueAt(i, 1);
                if (val == null) continue;
                String idTabla = val.toString();
                if (objetivoConPrefijo.equalsIgnoreCase(idTabla) || String.valueOf(idReferencia).equals(idTabla)) {
                    jTable1.getSelectionModel().setSelectionInterval(i, i);
                    jTable1.scrollRectToVisible(jTable1.getCellRect(i, 1, true));
                    break;
                }
            }
        } catch (Exception ignore) { }
    }

    private void aplicarSeleccionInicial() {
        if (idReferenciaSeleccionInicial != null) {
            seleccionarOrdenPorReferencia(idReferenciaSeleccionInicial);
            idReferenciaSeleccionInicial = null;
        }
    }

    @Deprecated
    public void cargarVentas(List<Map<String, Object>> ventas) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (Map<String, Object> venta : ventas) {
            model.addRow(new Object[]{
                venta.get("id_venta"),
                venta.get("fecha_venta"),
                venta.get("cliente"),
                venta.get("estado"),
                venta.get("tipo_pago"),
                "$" + formatoMoneda.format(venta.get("total")),
                ""
            });
        }
    }

    private void cargarComboClientes() {
        cbxCliente.removeAllItems();
        try {
            cbxCliente.addItem("Seleccionar");
            for (ModelCliente client : serviceCliente.getAll()) {
                cbxCliente.addItem(client.getNombre());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar clientes", e);
            Toast.show(this, Toast.Type.INFO, "Error al cargar clientes: " + e.getMessage());
        }
    }

    public void cargarEstadisticasMesActual() {
        try {
            LocalDate now = LocalDate.now();
            LocalDate inicio = now.withDayOfMonth(1);
            LocalDate fin = now.withDayOfMonth(now.lengthOfMonth());
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            Map<String, Object> stats = ordenReservaService.obtenerEstadisticasOrdenes(inicio, fin, idBodega);
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

    private void mostrarReporteVenta(int idVenta, Map<String, Object> ventaInfo, List<Map<String, Object>> detalles) {
        viewReport reporteForm = new viewReport();
        if (ventaInfo != null) {
            if (!ventaInfo.containsKey("descuento")) {
                if (detalles != null && !detalles.isEmpty() && detalles.get(0).containsKey("descuento_venta_total")) {
                    ventaInfo.put("descuento", detalles.get(0).get("descuento_venta_total"));
                } else {
                    ventaInfo.put("descuento", 0.0);
                }
            }
            reporteForm.setVentaInfo(
                    idVenta,
                    ventaInfo.get("fecha_venta").toString(),
                    ventaInfo.get("estado").toString(),
                    Double.parseDouble(ventaInfo.get("total").toString()),
                    Double.parseDouble(ventaInfo.get("descuento").toString()),
                    ventaInfo.get("tipo_pago").toString());
            obtenerInformacionCliente(ventaInfo);
            reporteForm.setClienteInfo(
                    ventaInfo.get("cliente").toString(),
                    ventaInfo.get("dni") != null ? ventaInfo.get("dni").toString() : "",
                    ventaInfo.get("telefono") != null ? ventaInfo.get("telefono").toString() : "",
                    ventaInfo.get("email") != null ? ventaInfo.get("email").toString() : "",
                    ventaInfo.get("direccion") != null ? ventaInfo.get("direccion").toString() : "");
        }
        reporteForm.setProductos(detalles);
        JDialog dialog = new JDialog();
        dialog.setTitle("Detalles de Venta #" + idVenta);
        dialog.setModal(true);
        dialog.setSize(1000, 800);
        dialog.setLocationRelativeTo(null);
        dialog.setContentPane(reporteForm);
        dialog.setVisible(true);
    }

    private void obtenerInformacionCliente(Map<String, Object> ventaInfo) {
        if (ventaInfo == null || !ventaInfo.containsKey("cliente")) return;
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT c.id_cliente, c.nombre, c.dni, c.telefono, c.email, c.direccion "
                        + "FROM clientes c "
                        + "WHERE c.nombre = ? OR c.id_cliente = ?")) {
            stmt.setString(1, ventaInfo.get("cliente").toString());
            if (ventaInfo.containsKey("id_cliente")) {
                stmt.setInt(2, Integer.parseInt(ventaInfo.get("id_cliente").toString()));
            } else {
                stmt.setInt(2, -1);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ventaInfo.put("dni", rs.getString("dni"));
                    ventaInfo.put("telefono", rs.getString("telefono"));
                    ventaInfo.put("email", rs.getString("email"));
                    ventaInfo.put("direccion", rs.getString("direccion"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener datos del cliente", e);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new raven.datechooser.DatePicker();
        datePicker1 = new raven.datechooser.DatePicker();
        jPanel1 = new javax.swing.JPanel();
        panelMain = new javax.swing.JPanel();
        panelVentasMes = new javax.swing.JPanel();
        txtVentaMesPanel = new javax.swing.JLabel();
        tituloCantidadVentas = new javax.swing.JLabel();
        PanelNventas = new javax.swing.JPanel();
        txtnVentasMes = new javax.swing.JLabel();
        tituloVentasTot = new javax.swing.JLabel();
        panelprodVendidos = new javax.swing.JPanel();
        txtPromedioVenta = new javax.swing.JLabel();
        tituloPromedio = new javax.swing.JLabel();
        panelProdVend = new javax.swing.JPanel();
        txtProdVen = new javax.swing.JLabel();
        tituloProdVENDI = new javax.swing.JLabel();
        panelfiltroVentas = new javax.swing.JPanel();
        lbltitle = new javax.swing.JLabel();
        txtFechaIn = new javax.swing.JFormattedTextField();
        lbltitle2 = new javax.swing.JLabel();
        txtFechaFin = new javax.swing.JFormattedTextField();
        lbltitle3 = new javax.swing.JLabel();
        cbxCliente = new javax.swing.JComboBox<>();
        lbltitle4 = new javax.swing.JLabel();
        cbxEstado = new javax.swing.JComboBox<>();
        btnFiltrar = new javax.swing.JButton();
        btnReiniciar = new javax.swing.JButton();
        panelListadoVentas = new javax.swing.JPanel();
        tituloListado = new javax.swing.JLabel();
        scroll = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        BtnFacturar = new javax.swing.JButton();
        txtFecha = new javax.swing.JTextField();
        lb = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {
                "SELECT", "# Orden", "Fecha", "Cliente", "Bodega", "Estado", "Productos", "Fecha Estado", "Acciones"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class,
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, true, false, false, true
            };
            public Class getColumnClass(int columnIndex) { return types[columnIndex]; }
            public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit[columnIndex]; }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        BtnFacturar.setText("Facturar");
        BtnFacturar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnFacturarActionPerformed(evt);
            }
        });

        btnFiltrar.setText("Buscar");
        btnFiltrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFiltrarActionPerformed(evt);
            }
        });

        btnReiniciar.setText("Limpiar");
        btnReiniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReiniciarActionPerformed(evt);
            }
        });

        cbxCliente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccionar" }));
        cbxEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {
            "Todos", "pendiente", "retirado", "pagado", "entregado", "enviado", "finalizado", "cancelada",
            "cotizacion", "cotizacion_aprobada", "cotizacion_rechazada", "cotizacion_convertida", "completada"
        }));

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("Carrito / Órdenes Web");

        txtFecha.setEditable(false);

        lbltitle.setText("Desde:");
        lbltitle2.setText("Hasta:");
        lbltitle3.setText("Cliente:");
        lbltitle4.setText("Estado:");
        tituloListado.setText("◇ Listado de Órdenes Web");
        tituloCantidadVentas.setText("Total");
        tituloVentasTot.setText("Pendientes");
        tituloPromedio.setText("Pagados");
        tituloProdVENDI.setText("Finalizados");

        txtVentaMesPanel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtVentaMesPanel.setText("0");
        txtnVentasMes.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtnVentasMes.setText("0");
        txtPromedioVenta.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtPromedioVenta.setText("0");
        txtProdVen.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtProdVen.setText("0");

        // Layout panelVentasMes
        javax.swing.GroupLayout panelVentasMesLayout = new javax.swing.GroupLayout(panelVentasMes);
        panelVentasMes.setLayout(panelVentasMesLayout);
        panelVentasMesLayout.setHorizontalGroup(panelVentasMesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
            .addComponent(txtVentaMesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
            .addComponent(tituloCantidadVentas, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE));
        panelVentasMesLayout.setVerticalGroup(panelVentasMesLayout.createSequentialGroup()
            .addContainerGap(10, 10)
            .addComponent(txtVentaMesPanel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tituloCantidadVentas)
            .addContainerGap(10, 10));

        // Layout PanelNventas
        javax.swing.GroupLayout PanelNventasLayout = new javax.swing.GroupLayout(PanelNventas);
        PanelNventas.setLayout(PanelNventasLayout);
        PanelNventasLayout.setHorizontalGroup(PanelNventasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
            .addComponent(txtnVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
            .addComponent(tituloVentasTot, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE));
        PanelNventasLayout.setVerticalGroup(PanelNventasLayout.createSequentialGroup()
            .addContainerGap(10, 10)
            .addComponent(txtnVentasMes)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tituloVentasTot)
            .addContainerGap(10, 10));

        // Layout panelprodVendidos
        javax.swing.GroupLayout panelprodVendidosLayout = new javax.swing.GroupLayout(panelprodVendidos);
        panelprodVendidos.setLayout(panelprodVendidosLayout);
        panelprodVendidosLayout.setHorizontalGroup(panelprodVendidosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
            .addComponent(txtPromedioVenta, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
            .addComponent(tituloPromedio, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE));
        panelprodVendidosLayout.setVerticalGroup(panelprodVendidosLayout.createSequentialGroup()
            .addContainerGap(10, 10)
            .addComponent(txtPromedioVenta)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tituloPromedio)
            .addContainerGap(10, 10));

        // Layout panelProdVend
        javax.swing.GroupLayout panelProdVendLayout = new javax.swing.GroupLayout(panelProdVend);
        panelProdVend.setLayout(panelProdVendLayout);
        panelProdVendLayout.setHorizontalGroup(panelProdVendLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
            .addComponent(txtProdVen, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
            .addComponent(tituloProdVENDI, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE));
        panelProdVendLayout.setVerticalGroup(panelProdVendLayout.createSequentialGroup()
            .addContainerGap(10, 10)
            .addComponent(txtProdVen)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tituloProdVENDI)
            .addContainerGap(10, 10));

        // Layout panelfiltroVentas
        javax.swing.GroupLayout panelfiltroVentasLayout = new javax.swing.GroupLayout(panelfiltroVentas);
        panelfiltroVentas.setLayout(panelfiltroVentasLayout);
        panelfiltroVentasLayout.setHorizontalGroup(panelfiltroVentasLayout.createSequentialGroup()
            .addContainerGap(15, 15)
            .addGroup(panelfiltroVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelfiltroVentasLayout.createSequentialGroup()
                    .addComponent(lbltitle)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(txtFechaIn, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)
                    .addComponent(lbltitle2)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(txtFechaFin, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)
                    .addComponent(lbltitle3)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(cbxCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)
                    .addComponent(lbltitle4)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(cbxEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)
                    .addComponent(btnFiltrar)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnReiniciar)))
            .addContainerGap(15, Short.MAX_VALUE));
        panelfiltroVentasLayout.setVerticalGroup(panelfiltroVentasLayout.createSequentialGroup()
            .addContainerGap(10, 10)
            .addGroup(panelfiltroVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lbltitle)
                .addComponent(txtFechaIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lbltitle2)
                .addComponent(txtFechaFin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lbltitle3)
                .addComponent(cbxCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lbltitle4)
                .addComponent(cbxEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btnFiltrar)
                .addComponent(btnReiniciar))
            .addContainerGap(10, 10));

        // Layout panelListadoVentas
        javax.swing.GroupLayout panelListadoVentasLayout = new javax.swing.GroupLayout(panelListadoVentas);
        panelListadoVentas.setLayout(panelListadoVentasLayout);
        panelListadoVentasLayout.setHorizontalGroup(panelListadoVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelListadoVentasLayout.createSequentialGroup()
                .addContainerGap(15, 15)
                .addGroup(panelListadoVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelListadoVentasLayout.createSequentialGroup()
                        .addComponent(tituloListado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(BtnFacturar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1))
                .addContainerGap(15, 15)));
        panelListadoVentasLayout.setVerticalGroup(panelListadoVentasLayout.createSequentialGroup()
            .addContainerGap(12, 12)
            .addGroup(panelListadoVentasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(tituloListado)
                .addComponent(BtnFacturar)
                .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addContainerGap(12, 12));

        // Layout panelMain con KPIs + filtros + tabla
        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addContainerGap(15, 15)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelListadoVentas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelfiltroVentas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(panelVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(PanelNventas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelprodVendidos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelProdVend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(15, 15)));
        panelMainLayout.setVerticalGroup(panelMainLayout.createSequentialGroup()
            .addContainerGap(15, 15)
            .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(panelVentasMes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(PanelNventas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelprodVendidos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelProdVend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(panelfiltroVentas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(panelListadoVentas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addContainerGap(15, 15));

        // jPanel1 wraps panelMain with scroll
        scroll.setViewportView(panelMain);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(scroll));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(lb)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void BtnFacturarActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            List<Integer> idsSeleccionados = new ArrayList<>();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object checkVal = model.getValueAt(i, 0);
                if (checkVal instanceof Boolean && (Boolean) checkVal) {
                    Object idVal = model.getValueAt(i, 1);
                    if (idVal != null) {
                        String idStr = idVal.toString();
                        if (idStr.startsWith("OR-")) {
                            idsSeleccionados.add(Integer.parseInt(idStr.substring(3)));
                        }
                    }
                }
            }
            if (idsSeleccionados.isEmpty()) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione al menos una orden para facturar");
                return;
            }
            generarVentaFor1 form = new generarVentaFor1();
            Application.showForm(form);
            for (int idOrden : idsSeleccionados) {
                form.cargarOrdenReservaEnFormulario(idOrden);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al facturar", e);
            Toast.show(this, Toast.Type.ERROR, "Error al facturar: " + e.getMessage());
        }
    }

    private void btnFiltrarActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String fechaInicio = txtFechaIn.getText().trim();
            String fechaFin = txtFechaFin.getText().trim();
            String clienteSeleccionado = (String) cbxCliente.getSelectedItem();
            String estadoSeleccionado = (String) cbxEstado.getSelectedItem();
            Integer idBodega = UserSession.getInstance().getIdBodegaUsuario();
            List<Map<String, Object>> ordenes = ordenReservaService.filtrarOrdenes(
                    fechaInicio.isEmpty() ? null : fechaInicio,
                    fechaFin.isEmpty() ? null : fechaFin,
                    "Seleccionar".equals(clienteSeleccionado) ? null : clienteSeleccionado,
                    "Todos".equals(estadoSeleccionado) ? null : estadoSeleccionado,
                    idBodega);
            cargarOrdenes(ordenes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al filtrar", e);
            Toast.show(this, Toast.Type.ERROR, "Error al filtrar: " + e.getMessage());
        }
    }

    private void btnReiniciarActionPerformed(java.awt.event.ActionEvent evt) {
        txtFechaIn.setText("");
        txtFechaFin.setText("");
        cbxCliente.setSelectedIndex(0);
        cbxEstado.setSelectedIndex(0);
        cargarOrdenesReserva();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtnFacturar;
    private javax.swing.JPanel PanelNventas;
    private javax.swing.JButton btnFiltrar;
    private javax.swing.JButton btnReiniciar;
    private javax.swing.JComboBox<String> cbxCliente;
    private javax.swing.JComboBox<String> cbxEstado;
    private raven.datechooser.DatePicker datePicker;
    private raven.datechooser.DatePicker datePicker1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbltitle;
    private javax.swing.JLabel lbltitle2;
    private javax.swing.JLabel lbltitle3;
    private javax.swing.JLabel lbltitle4;
    private javax.swing.JPanel panelListadoVentas;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelProdVend;
    private javax.swing.JPanel panelfiltroVentas;
    private javax.swing.JPanel panelprodVendidos;
    private javax.swing.JPanel panelVentasMes;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JLabel tituloCantidadVentas;
    private javax.swing.JLabel tituloProdVENDI;
    private javax.swing.JLabel tituloPromedio;
    private javax.swing.JLabel tituloListado;
    private javax.swing.JLabel tituloVentasTot;
    private javax.swing.JTextField txtFecha;
    private javax.swing.JFormattedTextField txtFechaFin;
    private javax.swing.JFormattedTextField txtFechaIn;
    private javax.swing.JLabel txtProdVen;
    private javax.swing.JLabel txtPromedioVenta;
    private javax.swing.JLabel txtVentaMesPanel;
    private javax.swing.JLabel txtnVentasMes;
    // End of variables declaration//GEN-END:variables
}
