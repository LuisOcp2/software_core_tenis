package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import raven.controlador.principal.conexion;
import raven.utils.tono.CorporateTone;

/**
 * Modal para envío de traspasos - Diseño optimizado enfocado en productos
 * Patrón: Modal Dialog con Service Layer
 * Principios: Single Responsibility, Interface Segregation
 */
public class ModalEnvioTraspasoMejorado extends javax.swing.JDialog {

    // ==================== CONSTANTES DE ESTILO ====================
    private static final Color BG_MODAL = new Color(26, 32, 44); // #1A202C
    private static final Color BG_PANEL = new Color(45, 55, 72); // #2D3748
    private static final Color BG_INPUT = new Color(45, 55, 72); // #2D3748
    private static final Color BG_TABLE = new Color(45, 55, 72); // #2D3748
    private static final Color BG_TABLE_ROW_ALT = new Color(38, 46, 62); // #262E3E
    private static final Color BG_HEADER = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255); // #FFFFFF
    private static final Color TEXT_SECONDARY = new Color(160, 174, 192); // #A0AEC0
    private static final Color ACCENT_ORANGE = new Color(251, 146, 60); // #FB923C
    private static final Color ACCENT_GREEN = new Color(52, 211, 153); // #34D399
    private static final Color BORDER_COLOR = new Color(74, 85, 104); // #4A5568

    // Estilos FlatLaf
    private static final String HEADER_STYLE = "arc:0;background:#FB923C";
    private static final String PANEL_STYLE = "arc:12;background:#2D3748;border:1,1,1,1,#4A5568,1,12";
    private static final String INPUT_STYLE = "arc:8;background:#2D3748;foreground:#FFFFFF;caretColor:#FFFFFF;borderColor:#4A5568";
    private static final String BUTTON_PRIMARY = "arc:8;background:#FB923C;foreground:#FFFFFF;hoverBackground:#F97316;pressedBackground:#EA580C;borderWidth:0;font:bold 14";
    private static final String BUTTON_SECONDARY = "arc:8;background:#4A5568;foreground:#FFFFFF;hoverBackground:#718096;pressedBackground:#4A5568;borderWidth:0;font:bold 14";

    // ==================== ATRIBUTOS ====================
    private String numeroTraspaso;
    private Map<String, Object> datosTraspaso;
    private List<Map<String, Object>> productosTraspaso;
    private List<Map<String, Object>> productosSeleccionados;
    private EnvioCallback callback;
    private boolean enviado = false;
    private javax.swing.JPanel processingOverlay;
    private boolean processingActive = false;

    /**
     * Interface para callback de envío
     */
    public interface EnvioCallback {
        void onEnvioExitoso(String numeroTraspaso, List<Map<String, Object>> productosEnviados);

        void onEnvioCancelado();
    }

    // ==================== CONSTRUCTOR ====================
    public ModalEnvioTraspasoMejorado(java.awt.Frame parent, boolean modal, String numeroTraspaso) {
        super(parent, modal);
        this.numeroTraspaso = numeroTraspaso;
        this.productosSeleccionados = new ArrayList<>();

        initComponents();
        configurarInterfazMejorada();
        configurarEventos();

        // Cargar datos en segundo plano para apertura instantánea
        cargarDatosAsync();
    }

    /**
     * Carga los datos del traspaso de forma asíncrona
     */
    private void cargarDatosAsync() {
        // Mostrar estado de carga
        lblTituloProductos.setText("Productos a Enviar (Cargando...)");
        btnEnviar.setEnabled(false);

        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                cargarInformacionTraspaso();
                cargarProductosTraspasoOptimizado();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Verificar excepciones
                    actualizarLabelsInfo();
                    lblTituloProductos.setText("Productos a Enviar");
                    // Cargar imágenes en segundo plano después de mostrar datos
                    cargarImagenesAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarError("Error cargando datos: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Carga las imágenes de forma diferida sin bloquear la UI
     */
    private void cargarImagenesAsync() {
        if (productosTraspaso == null || productosTraspaso.isEmpty())
            return;

        new javax.swing.SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < productosTraspaso.size(); i++) {
                    Map<String, Object> producto = productosTraspaso.get(i);
                    Object idVariante = producto.get("id_variante");
                    if (idVariante == null)
                        continue;

                    try {
                        ImageIcon icon = cargarImagenVariante((Integer) idVariante);
                        if (icon != null) {
                            publish(new Object[] { i, icon });
                        }
                    } catch (Exception ignore) {
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
                for (Object[] data : chunks) {
                    int row = (Integer) data[0];
                    ImageIcon icon = (ImageIcon) data[1];
                    if (row < model.getRowCount()) {
                        model.setValueAt(icon, row, 1);
                    }
                }
            }
        }.execute();
    }

    /**
     * Carga la imagen de una variante específica
     */
    private ImageIcon cargarImagenVariante(int idVariante) {
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT imagen FROM producto_variantes WHERE id_variante = ?")) {
            stmt.setInt(1, idVariante);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] imgBytes = rs.getBytes("imagen");
                    if (imgBytes != null && imgBytes.length > 0) {
                        java.awt.image.BufferedImage original = javax.imageio.ImageIO
                                .read(new java.io.ByteArrayInputStream(imgBytes));
                        if (original != null) {
                            int w = 80, h = 80;
                            double scale = Math.min((double) w / original.getWidth(),
                                    (double) h / original.getHeight());
                            int nw = (int) (original.getWidth() * scale);
                            int nh = (int) (original.getHeight() * scale);
                            java.awt.image.BufferedImage thumb = new java.awt.image.BufferedImage(
                                    w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                            java.awt.Graphics2D g2 = thumb.createGraphics();
                            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g2.drawImage(original, (w - nw) / 2, (h - nh) / 2, nw, nh, null);
                            g2.dispose();
                            return new ImageIcon(thumb);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    // ==================== CONFIGURACIÓN DE INTERFAZ ====================

    /**
     * Configura la interfaz visual optimizada
     * Diseño enfocado en la tabla de productos
     */
    private void configurarInterfazMejorada() {
        // Modal principal - tamaño optimizado y adaptativo
        setTitle("Enviar Traspaso");
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int targetW = Math.min(screen.width - 80, 1350);
        int targetH = Math.min(screen.height - 120, 820);
        setSize(targetW, targetH);
        setLocationRelativeTo(getParent());
        setResizable(true);
        getContentPane().setBackground(BG_MODAL);

        // Header compacto
        configurarHeader();

        // Panel lateral izquierdo (info + envío)
        configurarPanelLateral();

        // Panel principal (tabla de productos)
        configurarPanelProductos();

        // Footer con acciones
        configurarFooter();

        // Botón por defecto para Enter
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(btnEnviar);
        }

        // Ajuste dinámico de altura de la tabla para evitar tapar el footer
        int headerH = panelHeader.getPreferredSize().height + 40;
        int leftRightH = 320; // estimación de alto de panel info y márgenes
        int confirmH = 90;
        int footerH = 75 + 24; // alto footer + margen
        int availableForTable = Math.max(220, getSize().height - (headerH + leftRightH + confirmH + footerH));
        jScrollPane2.setPreferredSize(new java.awt.Dimension(jScrollPane2.getPreferredSize().width, availableForTable));
        panelProductos.revalidate();
        panelProductos.repaint();

        // Fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lblFechaEnvio.setText(sdf.format(new Date()));
    }

    /**
     * Configura el header del modal (compacto y profesional)
     */
    private void configurarHeader() {
        panelHeader.putClientProperty(FlatClientProperties.STYLE, HEADER_STYLE);
        panelHeader.setPreferredSize(new Dimension(panelHeader.getPreferredSize().width, 75));

        lblTituloModal.setFont(new Font("Inter", Font.BOLD, 20));
        lblTituloModal.setForeground(TEXT_PRIMARY);
        lblTituloModal.setText("Confirmar Envío de Traspaso");

        lblSubtituloModal.setFont(new Font("Inter", Font.PLAIN, 13));
        lblSubtituloModal.setForeground(new Color(255, 255, 255, 180));
        lblSubtituloModal.setText(numeroTraspaso);

        btnCerrar.putClientProperty(FlatClientProperties.STYLE,
                "arc:999;background:rgba(255,255,255,38);foreground:#FFFFFF;" +
                        "hoverBackground:rgba(255,255,255,64);" +
                        "borderWidth:0;font:bold 16");
        btnCerrar.setPreferredSize(new Dimension(36, 36));
        btnCerrar.setText("");
    }

    /**
     * Configura el panel lateral con información compacta
     */
    private void configurarPanelLateral() {
        panelInfoTraspaso.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
        panelInfoTraspaso.setLayout(new GridBagLayout());
        panelInfoTraspaso.removeAll();
        panelInfoTraspaso.setPreferredSize(new Dimension(380, panelInfoTraspaso.getPreferredSize().height));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 16, 8, 16);

        // Título de sección
        JLabel lblTituloInfo = crearTituloSeccion("Información del Traspaso");
        gbc.gridy = 0;
        gbc.insets = new Insets(16, 16, 12, 16);
        panelInfoTraspaso.add(lblTituloInfo, gbc);

        // Info cards compactas
        gbc.insets = new Insets(4, 16, 4, 16);

        gbc.gridy++;
        panelInfoTraspaso.add(crearInfoRow("Origen", "Cargando..."), gbc);

        gbc.gridy++;
        panelInfoTraspaso.add(crearInfoRow("Destino", "Cargando..."), gbc);

        gbc.gridy++;
        panelInfoTraspaso.add(crearInfoRow("Solicitud", "Cargando..."), gbc);

        gbc.gridy++;
        panelInfoTraspaso.add(crearInfoRow("Estado", "AUTORIZADO"), gbc);

        // Separador
        gbc.gridy++;
        gbc.insets = new Insets(16, 16, 12, 16);
        panelInfoTraspaso.add(crearSeparador(), gbc);

        // Sección de datos de envío
        configurarSeccionEnvioCompacta(panelInfoTraspaso, gbc);
    }

    /**
     * Crea un título de sección estilizado
     */
    private JLabel crearTituloSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Inter", Font.BOLD, 16));
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    /**
     * Crea una fila de información compacta (label + valor)
     */
    private JPanel crearInfoRow(String label, String valor) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        lblLabel.setForeground(TEXT_SECONDARY);
        gbc.gridx = 0;
        gbc.weightx = 0.4;
        gbc.insets = new Insets(4, 0, 4, 8);
        panel.add(lblLabel, gbc);

        // Valor
        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Inter", Font.BOLD, 13));
        lblValor.setForeground(TEXT_PRIMARY);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gbc.insets = new Insets(4, 8, 4, 0);
        panel.add(lblValor, gbc);

        return panel;
    }

    /**
     * Crea un separador visual
     */
    private JPanel crearSeparador() {
        JPanel sep = new JPanel();
        sep.setBackground(BORDER_COLOR);
        sep.setPreferredSize(new Dimension(1, 1));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    /**
     * Configura la sección de datos de envío (compacta en panel lateral)
     */
    private void configurarSeccionEnvioCompacta(JPanel container, GridBagConstraints gbc) {
        panelDatosEnvio.putClientProperty(FlatClientProperties.STYLE, "background:#2D3748");
        panelDatosEnvio.setLayout(new GridBagLayout());
        panelDatosEnvio.setOpaque(false);

        GridBagConstraints gbcEnvio = new GridBagConstraints();
        gbcEnvio.gridx = 0;
        gbcEnvio.fill = GridBagConstraints.HORIZONTAL;
        gbcEnvio.weightx = 1.0;
        gbcEnvio.anchor = GridBagConstraints.NORTHWEST;
        gbcEnvio.insets = new Insets(8, 0, 8, 0);

        // Título
        lblTituloEnvio.setText("Datos de Envío");
        lblTituloEnvio.setFont(new Font("Inter", Font.BOLD, 16));
        lblTituloEnvio.setForeground(TEXT_PRIMARY);
        gbcEnvio.gridy = 0;
        gbcEnvio.insets = new Insets(0, 0, 12, 0);
        panelDatosEnvio.add(lblTituloEnvio, gbcEnvio);

        // Fecha de envío
        lblFechaEnvioLabel.setText("Fecha y Hora");
        lblFechaEnvioLabel.setFont(new Font("Inter", Font.BOLD, 11));
        lblFechaEnvioLabel.setForeground(TEXT_SECONDARY);
        gbcEnvio.gridy++;
        gbcEnvio.insets = new Insets(8, 0, 4, 0);
        panelDatosEnvio.add(lblFechaEnvioLabel, gbcEnvio);

        lblFechaEnvio.putClientProperty(FlatClientProperties.STYLE,
                "background:#1F2937;foreground:#A0AEC0;border:1,1,1,1,#4A5568,1,6");
        lblFechaEnvio.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        lblFechaEnvio.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        gbcEnvio.gridy++;
        gbcEnvio.insets = new Insets(0, 0, 12, 0);
        panelDatosEnvio.add(lblFechaEnvio, gbcEnvio);

        // Observaciones
        lblObservacionesLabel.setText("Observaciones (opcional)");
        lblObservacionesLabel.setFont(new Font("Inter", Font.BOLD, 11));
        lblObservacionesLabel.setForeground(TEXT_SECONDARY);
        gbcEnvio.gridy++;
        gbcEnvio.insets = new Insets(8, 0, 4, 0);
        panelDatosEnvio.add(lblObservacionesLabel, gbcEnvio);

        txtObservacionesEnvio.putClientProperty(FlatClientProperties.STYLE,
                "background:#2D3748;foreground:#FFFFFF;caretColor:#FFFFFF");
        txtObservacionesEnvio.setFont(new Font("Inter", Font.PLAIN, 12));
        txtObservacionesEnvio.setForeground(TEXT_PRIMARY);
        txtObservacionesEnvio.setCaretColor(TEXT_PRIMARY);
        txtObservacionesEnvio.setLineWrap(true);
        txtObservacionesEnvio.setWrapStyleWord(true);
        txtObservacionesEnvio.setRows(3);

        jScrollPane1.setViewportView(txtObservacionesEnvio);
        jScrollPane1.setBorder(null);
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE,
                "border:1,1,1,1,#4A5568,1,8");
        gbcEnvio.gridy++;
        gbcEnvio.insets = new Insets(0, 0, 0, 0);
        panelDatosEnvio.add(jScrollPane1, gbcEnvio);

        // Agregar panel de envío al container
        gbc.gridy++;
        gbc.insets = new Insets(0, 16, 16, 16);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        container.add(panelDatosEnvio, gbc);
    }

    /**
     * Configura la sección de productos (PRIORIDAD - ocupa mayor espacio)
     */
    private void configurarPanelProductos() {
        panelProductos.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);

        // Título de sección más prominente
        lblTituloProductos.setFont(new Font("Inter", Font.BOLD, 16));
        lblTituloProductos.setForeground(TEXT_PRIMARY);
        lblTituloProductos.setText("Productos a Enviar");

        // Checkbox seleccionar todos
        chkSeleccionarTodos.setFont(new Font("Inter", Font.PLAIN, 13));
        chkSeleccionarTodos.setForeground(TEXT_SECONDARY);
        chkSeleccionarTodos.setText("Seleccionar todos");
        chkSeleccionarTodos.setOpaque(false);
        chkSeleccionarTodos.putClientProperty(FlatClientProperties.STYLE,
                "icon.checkmarkColor:#FB923C;icon.borderColor:#4A5568;" +
                        "icon.focusedBorderColor:#FB923C;icon.selectedBorderColor:#FB923C;" +
                        "icon.background:#2D3748");

        // Label contador
        lblProductosSeleccionados.setFont(new Font("Inter", Font.BOLD, 13));
        lblProductosSeleccionados.setForeground(ACCENT_ORANGE);

        // Configurar tabla optimizada
        configurarTablaMejorada();
    }

    /**
     * Configura la tabla con estilo profesional y alta legibilidad
     * MEJORA PRINCIPAL: Mejor contraste, filas más altas, columnas optimizadas
     */
    private void configurarTablaMejorada() {
        // Configuración base de la tabla
        tablaProductos.setBackground(BG_TABLE);
        tablaProductos.setForeground(TEXT_PRIMARY);
        tablaProductos.setFont(new Font("Inter", Font.PLAIN, 13));
        tablaProductos.setRowHeight(84); // Filas más altas para mostrar thumbnail 80x80
        tablaProductos.setShowHorizontalLines(true);
        tablaProductos.setShowVerticalLines(false);
        tablaProductos.setGridColor(BORDER_COLOR);
        tablaProductos.setSelectionBackground(new Color(251, 146, 60, 30)); // Selección sutil
        tablaProductos.setSelectionForeground(TEXT_PRIMARY);
        tablaProductos.setIntercellSpacing(new Dimension(0, 1));

        // Header de tabla
        JTableHeader header = tablaProductos.getTableHeader();
        header.setBackground(BG_HEADER);
        header.setForeground(new Color(209, 213, 219)); // Texto más claro
        header.setFont(new Font("Inter", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 45));
        header.putClientProperty(FlatClientProperties.STYLE,
                "background:#1F2937;foreground:#D1D5DB;font:bold 12;" +
                        "hoverBackground:#1F2937;separatorColor:#4A5568;height:45");

        DefaultTableModel model = new DefaultTableModel(
                new String[] { "", "Imagen", "Producto", "Solicitado", "Pendiente", "Enviar", "Ubicación" },
                0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                if (columnIndex == 1)
                    return ImageIcon.class;
                if (columnIndex >= 3 && columnIndex <= 5)
                    return Integer.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 5;
            }
        };

        tablaProductos.setModel(model);
        tablaProductos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Configurar anchos de columna optimizados
        TableColumnModel cm = tablaProductos.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(50);
        cm.getColumn(1).setPreferredWidth(80);
        cm.getColumn(1).setMaxWidth(90);
        cm.getColumn(2).setPreferredWidth(480);
        cm.getColumn(3).setPreferredWidth(110);
        cm.getColumn(3).setMaxWidth(110);
        cm.getColumn(4).setPreferredWidth(110);
        cm.getColumn(4).setMaxWidth(110);
        cm.getColumn(5).setPreferredWidth(110);
        cm.getColumn(5).setMaxWidth(110);
        cm.getColumn(6).setPreferredWidth(180);

        // Renderer para checkbox (centrado)
        DefaultTableCellRenderer checkRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                aplicarEstiloFila(c, isSelected, row);
                return c;
            }
        };

        // Renderer para columna de producto (HTML con estilo mejorado)
        DefaultTableCellRenderer productRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);
                setFont(new Font("Inter", Font.PLAIN, 13));
                aplicarEstiloFila(c, isSelected, row);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                return c;
            }
        };

        // Renderer para números (centrado y resaltado)
        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Inter", Font.BOLD, 14));
                aplicarEstiloFila(c, isSelected, row);

                // Color especial para cantidad a enviar
                if (column == 5 && c instanceof JLabel) {
                    ((JLabel) c).setForeground(isSelected ? TEXT_PRIMARY : ACCENT_GREEN);
                }

                return c;
            }
        };

        // Renderer para ubicación
        DefaultTableCellRenderer ubicacionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
                aplicarEstiloFila(c, isSelected, row);

                if (c instanceof JLabel) {
                    String txt = value != null ? String.valueOf(value) : "";
                    ((JLabel) c).setText(txt.isEmpty() ? "—" : txt);
                    ((JLabel) c).setToolTipText(txt.isEmpty() ? "Sin ubicación asignada" : "Ubicación: " + txt);
                }

                return c;
            }
        };

        // Aplicar renderers
        DefaultTableCellRenderer imageRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setVerticalAlignment(SwingConstants.CENTER);
                aplicarEstiloFila(lbl, isSelected, row);
                if (value instanceof ImageIcon) {
                    lbl.setIcon((ImageIcon) value);
                    lbl.setText("");
                } else {
                    lbl.setIcon(null);
                    lbl.setText("");
                }
                return lbl;
            }
        };

        cm.getColumn(1).setCellRenderer(imageRenderer);
        cm.getColumn(2).setCellRenderer(productRenderer);
        cm.getColumn(3).setCellRenderer(numberRenderer);
        cm.getColumn(4).setCellRenderer(numberRenderer);
        cm.getColumn(5).setCellRenderer(numberRenderer);
        cm.getColumn(6).setCellRenderer(ubicacionRenderer);

        // Estilo del scrollpane
        jScrollPane2.setBorder(null);
        jScrollPane2.putClientProperty(FlatClientProperties.STYLE,
                "border:1,1,1,1,#4A5568,1,8");
    }

    /**
     * Aplica estilo alternado a las filas para mejor legibilidad
     */
    private void aplicarEstiloFila(Component c, boolean isSelected, int row) {
        if (isSelected) {
            c.setBackground(new Color(251, 146, 60, 40));
            c.setForeground(TEXT_PRIMARY);
        } else {
            c.setBackground(row % 2 == 0 ? BG_TABLE : BG_TABLE_ROW_ALT);
            c.setForeground(TEXT_PRIMARY);
        }
    }

    /**
     * Configura el footer con botones de acción
     */
    private void configurarFooter() {
        panelFooter.putClientProperty(FlatClientProperties.STYLE,
                "arc:0;background:#1F2937;border:1,0,0,0,#4A5568,1,0");
        panelFooter.setPreferredSize(new Dimension(panelFooter.getPreferredSize().width, 75));

        // Labels de totales
        lblProductosSeleccionados2.setFont(new Font("Inter", Font.PLAIN, 13));
        lblProductosSeleccionados2.setForeground(TEXT_SECONDARY);

        lblTotalEnviar.setFont(new Font("Inter", Font.BOLD, 16));
        lblTotalEnviar.setForeground(ACCENT_GREEN);

        // Botones
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, BUTTON_SECONDARY);
        btnCancelar.setPreferredSize(new Dimension(130, 40));
        btnCancelar.setText("Cancelar");

        btnEnviar.putClientProperty(FlatClientProperties.STYLE, BUTTON_PRIMARY);
        btnEnviar.setPreferredSize(new Dimension(180, 40));
        btnEnviar.setText("Confirmar Envío");
        btnEnviar.setEnabled(false);

        // Panel de confirmación (alerta azul)
        panelConfirmacion.setBackground(new Color(59, 130, 246, 20));
        panelConfirmacion.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;background:#3B82F610;border:1,1,1,1,#3B82F640,1,8");

        lblIconoAlerta.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblIconoAlerta.setForeground(new Color(96, 165, 250));

        lblTextoConfirmacion.setFont(new Font("Inter", Font.PLAIN, 12));
        lblTextoConfirmacion.setForeground(new Color(191, 219, 254));
    }

    // ==================== CARGA DE DATOS ====================

    /**
     * Carga los datos del traspaso desde la base de datos
     */
    private void cargarDatosTraspaso() {
        try {
            cargarInformacionTraspaso();
            cargarProductosTraspaso();
            actualizarLabelsInfo();
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error cargando datos del traspaso: " + e.getMessage());
        }
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
        }

        rs.close();
        stmt.close();
        conn.close();
    }

    /**
     * Carga los productos del traspaso
     * HTML mejorado para descripción de productos
     */
    private void cargarProductosTraspaso() throws SQLException {
        Integer idBodegaOrigen = null;
        try {
            Object o = datosTraspaso != null ? datosTraspaso.get("id_bodega_origen") : null;
            if (o instanceof Integer) {
                idBodegaOrigen = (Integer) o;
            }
        } catch (Exception ignore) {
        }

        String sql = "SELECT td.*, p.nombre as producto_nombre, p.codigo_modelo, "
                + "c.nombre as color_nombre, t.numero as talla_numero, "
                + "pv.sku, pv.ean, pv.imagen, "
                + "(td.cantidad_solicitada - COALESCE(td.cantidad_enviada, 0)) as pendiente_envio, "
                + "ib.ubicacion_especifica AS ubicacion_especifica "
                + "FROM traspaso_detalles td "
                + "INNER JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso "
                + "INNER JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN inventario_bodega ib ON ib.id_variante = td.id_variante "
                + (idBodegaOrigen != null ? " AND ib.id_bodega = ? " : " ")
                + "WHERE tr.numero_traspaso = ? "
                + "AND (td.cantidad_solicitada - COALESCE(td.cantidad_enviada, 0)) > 0 "
                + "ORDER BY p.nombre, c.nombre, t.numero";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        int paramIdx = 1;
        if (idBodegaOrigen != null) {
            stmt.setInt(paramIdx++, idBodegaOrigen);
        }
        stmt.setString(paramIdx++, numeroTraspaso);
        ResultSet rs = stmt.executeQuery();

        productosTraspaso = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            Map<String, Object> producto = new HashMap<>();
            producto.put("id_detalle", rs.getInt("id_detalle_traspaso"));
            producto.put("id_producto", rs.getInt("id_producto"));
            producto.put("id_variante", rs.getObject("id_variante"));
            producto.put("cantidad_solicitada", rs.getInt("cantidad_solicitada"));
            producto.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
            producto.put("pendiente_envio", rs.getInt("pendiente_envio"));

            ImageIcon icon = null;
            byte[] imgBytes = rs.getBytes("imagen");
            if (imgBytes != null && imgBytes.length > 0) {
                try {
                    java.awt.image.BufferedImage original = javax.imageio.ImageIO
                            .read(new java.io.ByteArrayInputStream(imgBytes));
                    if (original != null) {
                        int w = 80, h = 80;
                        double scale = Math.min((double) w / original.getWidth(), (double) h / original.getHeight());
                        int nw = (int) (original.getWidth() * scale);
                        int nh = (int) (original.getHeight() * scale);
                        java.awt.image.BufferedImage thumb = new java.awt.image.BufferedImage(w, h,
                                java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2 = thumb.createGraphics();
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        int x = (w - nw) / 2;
                        int y = (h - nh) / 2;
                        g2.drawImage(original, x, y, nw, nh, null);
                        g2.dispose();
                        icon = new ImageIcon(thumb);
                    }
                } catch (Exception ignore) {
                }
            }

            // Construir HTML mejorado para la columna de producto
            StringBuilder html = new StringBuilder();
            html.append("<html><div style='padding:4px;'>");

            // Nombre del producto (más grande y bold)
            html.append("<div style='font-size:13px; font-weight:bold; color:#FFFFFF; margin-bottom:4px;'>");
            html.append(rs.getString("producto_nombre"));
            html.append("</div>");

            // Variante (color + talla)
            html.append("<div style='font-size:11px; color:#A0AEC0;'>");
            boolean tieneVariante = false;
            if (rs.getString("color_nombre") != null) {
                html.append("<span style='color:#34D399;'>● </span>");
                html.append(rs.getString("color_nombre"));
                tieneVariante = true;
            }
            if (rs.getString("talla_numero") != null) {
                if (tieneVariante)
                    html.append(" • ");
                html.append("Talla: <b>").append(rs.getString("talla_numero")).append("</b>");
            }
            html.append("</div>");

            html.append("</div></html>");

            producto.put("descripcion_completa", html.toString());
            producto.put("sku", rs.getString("sku"));

            String ubicacion = rs.getString("ubicacion_especifica");
            producto.put("ubicacion_especifica", ubicacion);

            productosTraspaso.add(producto);

            model.addRow(new Object[] {
                    false,
                    icon,
                    html.toString(),
                    rs.getInt("cantidad_solicitada"),
                    rs.getInt("pendiente_envio"),
                    rs.getInt("pendiente_envio"),
                    ubicacion != null ? ubicacion : ""
            });
        }

        rs.close();
        stmt.close();
        conn.close();
    }

    /**
     * Versión optimizada que NO carga imágenes para apertura instantánea
     * Las imágenes se omiten para reducir tiempo de carga de 5s a <1s
     */
    private void cargarProductosTraspasoOptimizado() throws SQLException {
        Integer idBodegaOrigen = null;
        try {
            Object o = datosTraspaso != null ? datosTraspaso.get("id_bodega_origen") : null;
            if (o instanceof Integer) {
                idBodegaOrigen = (Integer) o;
            }
        } catch (Exception ignore) {
        }

        // Query SIN imagen para carga rápida
        // CORRECCIÓN: Se agrega td.Tipo explícitamente para asegurar que se recupera
        String sql = "SELECT td.*, td.Tipo as tipo_producto, p.nombre as producto_nombre, p.codigo_modelo, "
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
                + "LEFT JOIN inventario_bodega ib ON ib.id_variante = td.id_variante "
                + (idBodegaOrigen != null ? " AND ib.id_bodega = ? " : " ")
                + "WHERE tr.numero_traspaso = ? "
                + "AND (td.cantidad_solicitada - COALESCE(td.cantidad_enviada, 0)) > 0 "
                + "ORDER BY p.nombre, c.nombre, t.numero";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        int paramIdx = 1;
        if (idBodegaOrigen != null) {
            stmt.setInt(paramIdx++, idBodegaOrigen);
        }
        stmt.setString(paramIdx++, numeroTraspaso);
        ResultSet rs = stmt.executeQuery();

        // Recolectar datos en lista temporal
        List<Object[]> filas = new ArrayList<>();
        productosTraspaso = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> producto = new HashMap<>();
            producto.put("id_detalle", rs.getInt("id_detalle_traspaso"));
            producto.put("id_producto", rs.getInt("id_producto"));
            producto.put("id_variante", rs.getObject("id_variante"));
            producto.put("cantidad_solicitada", rs.getInt("cantidad_solicitada"));
            producto.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
            producto.put("pendiente_envio", rs.getInt("pendiente_envio"));

            // HTML simple sin imagen
            StringBuilder html = new StringBuilder();
            html.append("<html><div style='padding:4px;'>");
            html.append("<div style='font-size:13px; font-weight:bold; color:#FFFFFF; margin-bottom:4px;'>");
            html.append(rs.getString("producto_nombre"));
            html.append("</div>");
            html.append("<div style='font-size:11px; color:#A0AEC0;'>");
            boolean tieneVariante = false;
            if (rs.getString("color_nombre") != null) {
                html.append("<span style='color:#34D399;'>● </span>");
                html.append(rs.getString("color_nombre"));
                tieneVariante = true;
            }
            if (rs.getString("talla_numero") != null) {
                if (tieneVariante)
                    html.append(" • ");
                html.append("Talla: <b>").append(rs.getString("talla_numero")).append("</b>");
            }
            html.append("</div></div></html>");

            producto.put("descripcion_completa", html.toString());
            producto.put("sku", rs.getString("sku"));

            // CORRECCIÓN: Recuperar y almacenar el campo Tipo explícitamente
            String tipoProducto = rs.getString("tipo_producto");
            if (tipoProducto == null || tipoProducto.trim().isEmpty()) {
                tipoProducto = rs.getString("Tipo"); // Fallback a td.Tipo
            }
            if (tipoProducto == null || tipoProducto.trim().isEmpty()) {
                tipoProducto = "par"; // Valor por defecto
                System.out.println("⚠️ WARNING: Tipo es NULL para producto ID " + rs.getInt("id_producto")
                        + ", usando valor por defecto 'par'");
            }
            producto.put("Tipo", tipoProducto);

            String ubicacion = rs.getString("ubicacion_especifica");
            producto.put("ubicacion_especifica", ubicacion);

            productosTraspaso.add(producto);

            int cantSolicitada = rs.getInt("cantidad_solicitada");
            int pendiente = rs.getInt("pendiente_envio");

            filas.add(new Object[] {
                    false,
                    null, // Sin imagen
                    html.toString(),
                    cantSolicitada,
                    pendiente,
                    pendiente,
                    ubicacion != null ? ubicacion : ""
            });
        }

        rs.close();
        stmt.close();
        conn.close();

        // Actualizar tabla en EDT
        final List<Object[]> filasFinales = filas;
        javax.swing.SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
            model.setRowCount(0);
            for (Object[] fila : filasFinales) {
                model.addRow(fila);
            }
            actualizarContadorProductos();
        });
    }

    /**
     * Actualiza los labels informativos del panel lateral
     */
    private void actualizarLabelsInfo() {
        if (datosTraspaso != null && panelInfoTraspaso != null) {
            // Actualizar las info rows en el panel lateral
            Component[] components = panelInfoTraspaso.getComponents();
            int rowIndex = 0;

            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    Component[] innerComps = panel.getComponents();

                    // Buscar el label de valor (segundo componente)
                    if (innerComps.length >= 2 && innerComps[1] instanceof JLabel) {
                        JLabel lblValor = (JLabel) innerComps[1];

                        switch (rowIndex) {
                            case 0: // Origen
                                lblValor.setText((String) datosTraspaso.get("bodega_origen"));
                                break;
                            case 1: // Destino
                                lblValor.setText((String) datosTraspaso.get("bodega_destino"));
                                break;
                            case 2: // Fecha solicitud
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                lblValor.setText(sdf.format((Date) datosTraspaso.get("fecha_solicitud")));
                                break;
                        }
                        rowIndex++;
                    }
                }
            }
        }
        actualizarContadorProductos();
    }

    // ==================== EVENTOS ====================

    /**
     * Configura los eventos de la interfaz
     */
    private void configurarEventos() {
        // Seleccionar/deseleccionar todos
        chkSeleccionarTodos.addActionListener(e -> {
            boolean seleccionado = chkSeleccionarTodos.isSelected();
            DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(seleccionado, i, 0);
            }

            actualizarContadorProductos();
        });

        // Cambios en la tabla
        tablaProductos.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0 || e.getColumn() == 5) {
                actualizarContadorProductos();
                validarSeleccionTodos();
            }
        });
    }

    /**
     * Actualiza el contador de productos seleccionados
     */
    private void actualizarContadorProductos() {
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        int seleccionados = 0;
        int totalCantidad = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                seleccionados++;
                Integer cantidad = (Integer) model.getValueAt(i, 5);
                if (cantidad != null) {
                    totalCantidad += cantidad;
                }
            }
        }

        lblProductosSeleccionados.setText(seleccionados + " de " + model.getRowCount() + " productos");
        lblProductosSeleccionados2.setText(seleccionados + " productos seleccionados");
        lblTotalEnviar.setText("Total: " + totalCantidad + " unidades");

        btnEnviar.setEnabled(seleccionados > 0);
    }

    /**
     * Valida el estado del checkbox "Seleccionar todos"
     */
    private void validarSeleccionTodos() {
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        int totalFilas = model.getRowCount();
        int seleccionadas = 0;

        for (int i = 0; i < totalFilas; i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                seleccionadas++;
            }
        }

        chkSeleccionarTodos.setSelected(seleccionadas > 0 && seleccionadas == totalFilas);
    }

    // ==================== VALIDACIÓN Y PROCESAMIENTO ====================

    /**
     * Valida los datos del formulario antes del envío
     */
    private boolean validarDatos() {
        StringBuilder errores = new StringBuilder();

        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();
        boolean hayProductosSeleccionados = false;

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                hayProductosSeleccionados = true;

                Integer cantidad = (Integer) model.getValueAt(i, 5);
                Integer pendiente = (Integer) model.getValueAt(i, 4);

                if (cantidad == null || cantidad <= 0) {
                    errores.append("- La cantidad debe ser mayor a 0 para todos los productos seleccionados\n");
                    break;
                } else if (cantidad > pendiente) {
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

        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Confirmar envío del traspaso " + numeroTraspaso + "?\n\n" +
                        "Se actualizará el estado a EN_TRANSITO.",
                "Confirmar envío",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        btnEnviar.setEnabled(false);
        btnEnviar.setText("Procesando...");
        showProcessingOverlay("Procesando envío...");

        try {
            recopilarProductosSeleccionados();
            actualizarBaseDatos();
            enviado = true;

            JOptionPane.showMessageDialog(this,
                    "Traspaso enviado exitosamente",
                    "Envío exitoso",
                    JOptionPane.INFORMATION_MESSAGE);

            if (callback != null) {
                callback.onEnvioExitoso(numeroTraspaso, productosSeleccionados);
            }

            dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error procesando el envío: " + e.getMessage());

            btnEnviar.setEnabled(true);
            btnEnviar.setText("Confirmar Envío");
        } finally {
            hideProcessingOverlay();
        }
    }

    /**
     * Recopila los productos seleccionados con sus cantidades
     */
    private void recopilarProductosSeleccionados() {
        productosSeleccionados.clear();
        DefaultTableModel model = (DefaultTableModel) tablaProductos.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                Map<String, Object> productoOriginal = productosTraspaso.get(i);
                Map<String, Object> productoEnviado = new HashMap<>(productoOriginal);

                productoEnviado.put("cantidad_envio", model.getValueAt(i, 5));

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
            // Actualizar traspaso principal
            String sqlTraspaso = "UPDATE traspasos SET "
                    + "estado = 'en_transito', "
                    + "fecha_envio = NOW(), "
                    + "observaciones = CONCAT(COALESCE(observaciones, ''), ?) "
                    + "WHERE numero_traspaso = ?";

            PreparedStatement stmtTraspaso = conn.prepareStatement(sqlTraspaso);
            String obsEnvio = txtObservacionesEnvio.getText().trim();
            stmtTraspaso.setString(1, obsEnvio.isEmpty() ? "" : " | Envío: " + obsEnvio);
            stmtTraspaso.setString(2, numeroTraspaso);
            stmtTraspaso.executeUpdate();
            stmtTraspaso.close();

            // Actualizar detalles de productos
            String sqlDetalle = "UPDATE traspaso_detalles SET "
                    + "cantidad_enviada = COALESCE(cantidad_enviada, 0) + ?, "
                    + "estado_detalle = CASE "
                    + "    WHEN (COALESCE(cantidad_enviada, 0) + ?) >= cantidad_solicitada THEN 'enviado' "
                    + "    ELSE 'pendiente' "
                    + "END "
                    + "WHERE id_detalle_traspaso = ?";

            PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle);

            for (Map<String, Object> producto : productosSeleccionados) {
                int cantidadEnvio = (Integer) producto.get("cantidad_envio");
                int idDetalle = (Integer) producto.get("id_detalle");

                stmtDetalle.setInt(1, cantidadEnvio);
                stmtDetalle.setInt(2, cantidadEnvio);
                stmtDetalle.setInt(3, idDetalle);
                stmtDetalle.addBatch();
            }

            stmtDetalle.executeBatch();
            stmtDetalle.close();

            // ===== DESCONTAR STOCK DE BODEGA ORIGEN (OPTIMIZADO) =====
            descontarStockBodegaOrigen(conn);
            // ==========================================================

            conn.commit();

            try {
                CorporateTone.playAlert();
            } catch (Exception ex) {
                System.err.println("Error reproduciendo alerta de envío: " + ex.getMessage());
            }

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /**
     * Descuenta stock de bodega origen de forma OPTIMIZADA
     * Usa solo 2 queries (una para pares, otra para cajas) sin importar cuántos
     * productos haya
     */
    private void descontarStockBodegaOrigen(Connection conn) throws SQLException {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("🔍 INICIO DESCUENTO DE STOCK - BODEGA ORIGEN");
        System.out.println("═══════════════════════════════════════════════════════════");

        // Obtener id de bodega origen
        Integer idBodegaOrigen = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_bodega_origen FROM traspasos WHERE numero_traspaso = ?")) {
            ps.setString(1, numeroTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idBodegaOrigen = rs.getInt(1);
                }
            }
        }

        if (idBodegaOrigen == null || idBodegaOrigen <= 0) {
            throw new SQLException("No se pudo obtener la bodega origen del traspaso");
        }

        System.out.println("📦 ID Bodega Origen: " + idBodegaOrigen);
        System.out.println("📋 Total productos seleccionados: " + productosSeleccionados.size());
        System.out.println("───────────────────────────────────────────────────────────");

        // Separar productos por tipo
        List<Map<String, Object>> productosPar = new ArrayList<>();
        List<Map<String, Object>> productosCaja = new ArrayList<>();

        for (Map<String, Object> producto : productosSeleccionados) {
            // CORRECCIÓN: Manejo defensivo del campo Tipo con valor por defecto
            String tipo = (String) producto.get("Tipo");

            // Debug detallado del producto
            System.out.println("\n📦 Procesando producto:");
            System.out.println("   - ID Variante: " + producto.get("id_variante"));
            System.out.println("   - Cantidad: " + producto.get("cantidad_envio"));
            System.out.println("   - Tipo (raw): '" + tipo + "'");

            // CORRECCIÓN: Valor por defecto si Tipo es null
            if (tipo == null || tipo.trim().isEmpty()) {
                tipo = "par"; // Valor por defecto
                System.out.println("   ⚠️ WARNING: Tipo era null/vacío, usando valor por defecto 'par'");
            }

            tipo = tipo.trim().toLowerCase();
            System.out.println("   - Tipo (normalizado): '" + tipo + "'");

            if ("par".equalsIgnoreCase(tipo)) {
                productosPar.add(producto);
                System.out.println("   ✅ Agregado a lista PARES");
            } else if ("caja".equalsIgnoreCase(tipo)) {
                productosCaja.add(producto);
                System.out.println("   ✅ Agregado a lista CAJAS");
            } else {
                // Fallback adicional: si no es ni par ni caja, asumir par
                System.out.println("   ⚠️ WARNING: Tipo '" + tipo + "' no reconocido, asumiendo 'par'");
                productosPar.add(producto);
            }
        }

        System.out.println("\n───────────────────────────────────────────────────────────");
        System.out.println("📊 RESUMEN DE CLASIFICACIÓN:");
        System.out.println("   - Productos tipo PAR: " + productosPar.size());
        System.out.println("   - Productos tipo CAJA: " + productosCaja.size());
        System.out.println("───────────────────────────────────────────────────────────");

        if (productosPar.isEmpty() && productosCaja.isEmpty()) {
            System.out.println("\n❌ ERROR CRÍTICO: Ningún producto clasificado correctamente!");
            System.out.println("   Esto significa que el stock NO se descontará.");
            System.out.println("   Revisar que el campo 'Tipo' se esté recuperando de la BD.");
            System.out.println("═══════════════════════════════════════════════════════════\n");
        }

        // OPTIMIZACIÓN: 1 query para TODOS los pares (en lugar de N queries)
        if (!productosPar.isEmpty()) {
            StringBuilder sqlPar = new StringBuilder(
                    "UPDATE inventario_bodega SET Stock_par = CASE id_variante ");

            List<Integer> variantesPar = new ArrayList<>();
            List<Integer> cantidadesPar = new ArrayList<>();

            for (Map<String, Object> p : productosPar) {
                int idVariante = ((Number) p.get("id_variante")).intValue();
                int cantidad = ((Number) p.get("cantidad_envio")).intValue();
                sqlPar.append("WHEN ? THEN GREATEST(0, Stock_par - ?) ");
                variantesPar.add(idVariante);
                cantidadesPar.add(cantidad);
            }

            sqlPar.append(
                    "ELSE Stock_par END, fecha_ultimo_movimiento = NOW() WHERE id_bodega = ? AND id_variante IN (");
            for (int i = 0; i < variantesPar.size(); i++) {
                sqlPar.append(i == 0 ? "?" : ",?");
            }
            sqlPar.append(") AND activo = 1");

            try (PreparedStatement stmt = conn.prepareStatement(sqlPar.toString())) {
                int paramIndex = 1;
                // WHEN clauses
                for (int i = 0; i < variantesPar.size(); i++) {
                    stmt.setInt(paramIndex++, variantesPar.get(i));
                    stmt.setInt(paramIndex++, cantidadesPar.get(i));
                }
                // WHERE id_bodega
                stmt.setInt(paramIndex++, idBodegaOrigen);
                // IN clause
                for (Integer idVariante : variantesPar) {
                    stmt.setInt(paramIndex++, idVariante);
                }

                int updated = stmt.executeUpdate();
                System.out.println("SUCCESS Stock actualizado (PARES): " + updated + " registros en 1 query");
            }
        }

        // OPTIMIZACIÓN: 1 query para TODAS las cajas (en lugar de N queries)
        if (!productosCaja.isEmpty()) {
            StringBuilder sqlCaja = new StringBuilder(
                    "UPDATE inventario_bodega SET Stock_caja = CASE id_variante ");

            List<Integer> variantesCaja = new ArrayList<>();
            List<Integer> cantidadesCaja = new ArrayList<>();

            for (Map<String, Object> p : productosCaja) {
                int idVariante = ((Number) p.get("id_variante")).intValue();
                int cantidad = ((Number) p.get("cantidad_envio")).intValue();
                sqlCaja.append("WHEN ? THEN GREATEST(0, Stock_caja - ?) ");
                variantesCaja.add(idVariante);
                cantidadesCaja.add(cantidad);
            }

            sqlCaja.append(
                    "ELSE Stock_caja END, fecha_ultimo_movimiento = NOW() WHERE id_bodega = ? AND id_variante IN (");
            for (int i = 0; i < variantesCaja.size(); i++) {
                sqlCaja.append(i == 0 ? "?" : ",?");
            }
            sqlCaja.append(") AND activo = 1");

            try (PreparedStatement stmt = conn.prepareStatement(sqlCaja.toString())) {
                int paramIndex = 1;
                // WHEN clauses
                for (int i = 0; i < variantesCaja.size(); i++) {
                    stmt.setInt(paramIndex++, variantesCaja.get(i));
                    stmt.setInt(paramIndex++, cantidadesCaja.get(i));
                }
                // WHERE id_bodega
                stmt.setInt(paramIndex++, idBodegaOrigen);
                // IN clause
                for (Integer idVariante : variantesCaja) {
                    stmt.setInt(paramIndex++, idVariante);
                }

                int updated = stmt.executeUpdate();
                System.out.println("SUCCESS Stock actualizado (CAJAS): " + updated + " registros en 1 query");
            }
        }
    }

    /**
     * Muestra un mensaje de error
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void setCallback(EnvioCallback callback) {
        this.callback = callback;
    }

    public boolean isEnviado() {
        return enviado;
    }

    /**
     * Método estático para mostrar el modal
     */
    public static void mostrarModal(java.awt.Frame parent, String numeroTraspaso, EnvioCallback callback) {
        ModalEnvioTraspasoMejorado modal = new ModalEnvioTraspasoMejorado(parent, true, numeroTraspaso);
        modal.setCallback(callback);
        modal.setVisible(true);
    }

    /**
     * Método para usar desde formularios padre
     */
    public static void enviarTraspaso(java.awt.Frame parent, String numeroTraspaso, EnvioCallback callback) {
        try {
            if (!verificarEstadoTraspaso(numeroTraspaso)) {
                JOptionPane.showMessageDialog(parent,
                        "El traspaso debe estar en estado AUTORIZADO para poder enviarlo.",
                        "Estado incorrecto",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            mostrarModal(parent, numeroTraspaso, callback);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error verificando estado del traspaso: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Verifica que el traspaso esté en estado correcto
     */
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

    // ==================== INITCOMPONENTS (NO MODIFICAR) ====================
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        // MISMO CÓDIGO DE initComponents() QUE EN EL ORIGINAL
        // Se mantiene igual para compatibilidad con NetBeans Designer

        panelHeader = new javax.swing.JPanel();
        lblTituloModal = new javax.swing.JLabel();
        lblSubtituloModal = new javax.swing.JLabel();
        btnCerrar = new javax.swing.JButton();
        panelInfoTraspaso = new javax.swing.JPanel();
        panelDatosEnvio = new javax.swing.JPanel();
        lblTituloEnvio = new javax.swing.JLabel();
        lblFechaEnvioLabel = new javax.swing.JLabel();
        lblFechaEnvio = new javax.swing.JLabel();
        lblObservacionesLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtObservacionesEnvio = new javax.swing.JTextArea();
        panelProductos = new javax.swing.JPanel();
        lblTituloProductos = new javax.swing.JLabel();
        panelSeleccion = new javax.swing.JPanel();
        chkSeleccionarTodos = new javax.swing.JCheckBox();
        lblProductosSeleccionados = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tablaProductos = new javax.swing.JTable();
        panelConfirmacion = new javax.swing.JPanel();
        lblIconoAlerta = new javax.swing.JLabel();
        lblTextoConfirmacion = new javax.swing.JLabel();
        panelFooter = new javax.swing.JPanel();
        lblProductosSeleccionados2 = new javax.swing.JLabel();
        lblTotalEnviar = new javax.swing.JLabel();
        btnCancelar = new javax.swing.JButton();
        btnEnviar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Enviar Traspaso");

        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelHeaderLayout = new javax.swing.GroupLayout(panelHeader);
        panelHeader.setLayout(panelHeaderLayout);
        panelHeaderLayout.setHorizontalGroup(
                panelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelHeaderLayout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addGroup(
                                        panelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(lblTituloModal)
                                                .addComponent(lblSubtituloModal))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 36,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(24, 24, 24)));
        panelHeaderLayout.setVerticalGroup(
                panelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelHeaderLayout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addGroup(panelHeaderLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 36,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(panelHeaderLayout.createSequentialGroup()
                                                .addComponent(lblTituloModal)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblSubtituloModal)))
                                .addContainerGap(16, Short.MAX_VALUE)));

        javax.swing.GroupLayout panelInfoTraspasoLayout = new javax.swing.GroupLayout(panelInfoTraspaso);
        panelInfoTraspaso.setLayout(panelInfoTraspasoLayout);
        panelInfoTraspasoLayout.setHorizontalGroup(
                panelInfoTraspasoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 380, Short.MAX_VALUE));
        panelInfoTraspasoLayout.setVerticalGroup(
                panelInfoTraspasoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE));

        txtObservacionesEnvio.setColumns(20);
        txtObservacionesEnvio.setRows(3);
        jScrollPane1.setViewportView(txtObservacionesEnvio);

        javax.swing.GroupLayout panelDatosEnvioLayout = new javax.swing.GroupLayout(panelDatosEnvio);
        panelDatosEnvio.setLayout(panelDatosEnvioLayout);
        panelDatosEnvioLayout.setHorizontalGroup(
                panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE));
        panelDatosEnvioLayout.setVerticalGroup(
                panelDatosEnvioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE));

        javax.swing.GroupLayout panelSeleccionLayout = new javax.swing.GroupLayout(panelSeleccion);
        panelSeleccion.setLayout(panelSeleccionLayout);
        panelSeleccionLayout.setHorizontalGroup(
                panelSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSeleccionLayout.createSequentialGroup()
                                .addComponent(chkSeleccionarTodos)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblProductosSeleccionados)));
        panelSeleccionLayout.setVerticalGroup(
                panelSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(chkSeleccionarTodos)
                                .addComponent(lblProductosSeleccionados)));

        tablaProductos.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] { "", "Imagen", "Producto", "Solicitado", "Pendiente", "Enviar", "Ubicación" }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, javax.swing.ImageIcon.class, java.lang.String.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[] {
                    true, false, false, false, false, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tablaProductos.setRowHeight(60);
        jScrollPane2.setViewportView(tablaProductos);

        javax.swing.GroupLayout panelProductosLayout = new javax.swing.GroupLayout(panelProductos);
        panelProductos.setLayout(panelProductosLayout);
        panelProductosLayout.setHorizontalGroup(
                panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProductosLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(panelProductosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2)
                                        .addComponent(panelSeleccion, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelProductosLayout.createSequentialGroup()
                                                .addComponent(lblTituloProductos)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addGap(20, 20, 20)));
        panelProductosLayout.setVerticalGroup(
                panelProductosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelProductosLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(lblTituloProductos)
                                .addGap(15, 15, 15)
                                .addComponent(panelSeleccion, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                                .addGap(20, 20, 20)));

        lblTextoConfirmacion.setText(
                "<html><b>Atención:</b> Al confirmar, el traspaso se marcará como EN_TRANSITO y se registrará la fecha de envío.</html>");

        javax.swing.GroupLayout panelConfirmacionLayout = new javax.swing.GroupLayout(panelConfirmacion);
        panelConfirmacion.setLayout(panelConfirmacionLayout);
        panelConfirmacionLayout.setHorizontalGroup(
                panelConfirmacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelConfirmacionLayout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(lblIconoAlerta)
                                .addGap(12, 12, 12)
                                .addComponent(lblTextoConfirmacion, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(16, 16, 16)));
        panelConfirmacionLayout.setVerticalGroup(
                panelConfirmacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelConfirmacionLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(panelConfirmacionLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                        .addComponent(lblIconoAlerta)
                                        .addComponent(lblTextoConfirmacion, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(12, Short.MAX_VALUE)));

        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFooterLayout = new javax.swing.GroupLayout(panelFooter);
        panelFooter.setLayout(panelFooterLayout);
        panelFooterLayout.setHorizontalGroup(
                panelFooterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFooterLayout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addGroup(
                                        panelFooterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(lblProductosSeleccionados2)
                                                .addComponent(lblTotalEnviar))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(btnEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 180,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(24, 24, 24)));
        panelFooterLayout.setVerticalGroup(
                panelFooterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFooterLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(panelFooterLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelFooterLayout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelFooterLayout.createSequentialGroup()
                                                .addComponent(lblProductosSeleccionados2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblTotalEnviar)))
                                .addContainerGap(18, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelHeader, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(panelInfoTraspaso, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(panelProductos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(20, 20, 20))
                        .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(panelConfirmacion, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(20, 20, 20))
                        .addComponent(panelFooter, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(panelHeader, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panelInfoTraspaso, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelProductos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(12, 12, 12)
                                .addComponent(panelConfirmacion, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(panelFooter, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));

        pack();
    }// </editor-fold>

    private void btnCerrarActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Cancelar el envío del traspaso?",
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            if (callback != null) {
                callback.onEnvioCancelado();
            }
            dispose();
        }
    }

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {
        procesarEnvio();
    }

    private void showProcessingOverlay(String message) {
        try {
            javax.swing.JPanel overlay = new javax.swing.JPanel(new java.awt.GridBagLayout());
            overlay.setOpaque(true);
            overlay.setBackground(new java.awt.Color(0, 0, 0, 120));
            overlay.addMouseListener(new java.awt.event.MouseAdapter() {
            });
            overlay.addMouseMotionListener(new java.awt.event.MouseAdapter() {
            });

            javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.GridBagLayout());
            content.setOpaque(false);
            javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
            bar.setIndeterminate(true);
            javax.swing.JLabel lbl = new javax.swing.JLabel(message != null ? message : "Procesando...");
            lbl.setForeground(java.awt.Color.WHITE);
            lbl.setFont(new java.awt.Font("Inter", java.awt.Font.BOLD, 14));

            java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new java.awt.Insets(0, 0, 8, 0);
            content.add(bar, gbc);
            gbc.gridy = 1;
            content.add(lbl, gbc);

            overlay.add(content, new java.awt.GridBagConstraints());

            getRootPane().setGlassPane(overlay);
            overlay.setVisible(true);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            processingOverlay = overlay;
            processingActive = true;
        } catch (Exception ignore) {
        }
    }

    private void hideProcessingOverlay() {
        try {
            if (processingActive && processingOverlay != null) {
                processingOverlay.setVisible(false);
                processingOverlay = null;
                processingActive = false;
            }
            setCursor(java.awt.Cursor.getDefaultCursor());
        } catch (Exception ignore) {
        }
    }

    // Variables declaration - do not modify
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCerrar;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JCheckBox chkSeleccionarTodos;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblFechaEnvio;
    private javax.swing.JLabel lblFechaEnvioLabel;
    private javax.swing.JLabel lblIconoAlerta;
    private javax.swing.JLabel lblObservacionesLabel;
    private javax.swing.JLabel lblProductosSeleccionados;
    private javax.swing.JLabel lblProductosSeleccionados2;
    private javax.swing.JLabel lblSubtituloModal;
    private javax.swing.JLabel lblTextoConfirmacion;
    private javax.swing.JLabel lblTituloEnvio;
    private javax.swing.JLabel lblTituloModal;
    private javax.swing.JLabel lblTituloProductos;
    private javax.swing.JLabel lblTotalEnviar;
    private javax.swing.JPanel panelConfirmacion;
    private javax.swing.JPanel panelDatosEnvio;
    private javax.swing.JPanel panelFooter;
    private javax.swing.JPanel panelHeader;
    private javax.swing.JPanel panelInfoTraspaso;
    private javax.swing.JPanel panelProductos;
    private javax.swing.JPanel panelSeleccion;
    private javax.swing.JTable tablaProductos;
    private javax.swing.JTextArea txtObservacionesEnvio;
    // End of variables declaration
}
