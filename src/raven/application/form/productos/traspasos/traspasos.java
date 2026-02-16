package raven.application.form.productos.traspasos;

import raven.datetime.component.date.DatePicker;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import raven.clases.productos.ServiceProduct;
import raven.componentes.impresion.PanelSearch;
import raven.controlador.principal.conexion;
import raven.modal.ModalDialog;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import raven.cell.TableActionCellEditor;
import raven.cell.TableActionCellRender;
import raven.cell.TableActionEvent;
import raven.clases.productos.Bodega;
import raven.clases.productos.RecepcionTraspaso;
import raven.clases.productos.TraspasoDatos;
import raven.clases.productos.TraspasoCancelService;
import raven.clases.productos.TraspasoService;
import raven.controlador.productos.TraspasoController;
import raven.controlador.admin.SessionManager;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;
import javax.swing.ImageIcon;
import java.awt.Image;
import raven.application.Application;
import raven.application.form.principal.generarVentaFor1;
import raven.utils.ImageUtils;
import raven.application.form.productos.creates.CreateTallas;
import raven.modal.option.BorderOption;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalCallback;
import raven.modal.listener.ModalController;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import raven.componentes.notificacion.Notification;
import raven.utils.tono.CorporateTone;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import raven.clases.admin.UserSession;
import raven.clases.comun.TableRefreshManager;
import raven.clases.productos.TraspasoPermissionValidator;
import raven.componentes.LoadingOverlayHelper;

public class traspasos extends javax.swing.JPanel {
    // Instancia del servicio para operaciones con empleados

    private ServiceProduct service = new ServiceProduct();
    // Constructor del formulario de gestión de productos
    private JPopupMenu menu;
    private PanelSearch search;
    private TraspasoService traspasoService;
    private int idUsuarioActual = 1; // TODO: Obtener del sistema de sesión
    private TraspasoController controller;
    private TableRefreshManager<Object[]> refreshManager;
    // ═══════════════════════════════════════════════════════════════════════════
    // ESTILOS MODERNOS - FlatLaf
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String STYLE_PANEL = "arc:25;background:$Login.background";
    private static final String STYLE_PANEL_FILTROS = "arc:15;background:lighten($Login.background,5%)";
    private static final String STYLE_PANEL_STATS = "arc:15;background:darken($Login.background,3%)";
    private static final String STYLE_BTN_NUEVO = "arc:20;background:#28CD41";
    private static final String STYLE_BTN_AUTORIZAR = "arc:20;background:#007AFF";
    private static final String STYLE_BTN_ENVIAR = "arc:20;background:#FF9500";
    private static final String STYLE_BTN_EXPORTAR = "arc:20;background:#8349f1";
    private static final String STYLE_BTN_REFRESCAR = "arc:20;background:#5856D6";
    private static final String STYLE_BTN_REPORTE = "arc:20;background:#E91E63";
    private static final String STYLE_TEXTFIELD = "arc:15;background:lighten($Menu.background,25%)";
    private static final String FONT_HEADER_STYLE = "font:$h1.font";
    private static final String FONT_SUBHEADER_STYLE = "font:$h2.font";
    private static Logger LOGGER = Logger.getLogger(traspasos.class.getName());
    private javax.swing.JPanel loadingOverlay;
    private boolean overlayActive = false;

    // Componente de calendario
    private DatePicker datePicker;

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADÍSTICAS VISUALES
    // ═══════════════════════════════════════════════════════════════════════════
    private JLabel lblStatTotal;
    private JLabel lblStatPendientes;
    private JLabel lblStatEnTransito;
    private JLabel lblStatRecibidos;

    // Filtro de dirección (RECIBIDO/ENVIADO)
    private String filtroDireccion = "RECIBIDO"; // Por defecto RECIBIDO

    public traspasos() {
        // Inicializa componentes de la interfaz (generados automáticamente)
        initComponents();
        // Inicializa configuraciones personalizadas
        init();
        initPlaceHolders();

        // Inicializar servicio de traspasos
        this.traspasoService = new TraspasoService();
        try {
            Integer userId = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();
            this.idUsuarioActual = (userId != null) ? userId : 1;
        } catch (Exception e) {
            this.idUsuarioActual = 1; // Fallback
            System.err.println("Error obteniendo usuario actual: " + e.getMessage());
        }

        // Configurar tabla ANTES de cargar datos
        configurarTabla();

        // ═══════════════════════════════════════════════════════════════════════════
        // OPTIMIZACIÓN: Inicializar gestor de recarga inteligente
        // ═══════════════════════════════════════════════════════════════════════════
        refreshManager = new TableRefreshManager<>(
                getTablaTraspasos(),
                row -> row, // Ya es Object[]
                row -> row[0] // ID es la columna 0 (numero_traspaso)
        );

        refreshManager.setOnUpdateListener(listaTraspasos -> {
            // Recalcular estadísticas cuando cambian los datos
            int pendientes = 0, enTransito = 0, recibidos = 0;
            for (Object[] t : listaTraspasos) {
                String estado = t[4] != null ? t[4].toString().toLowerCase() : "";
                switch (estado) {
                    case "pendiente" -> pendientes++;
                    case "en_transito" -> enTransito++;
                    case "recibido" -> recibidos++;
                }
            }
            actualizarEstadisticasTraspasos(listaTraspasos.size(), pendientes, enTransito, recibidos);

            // Resaltar primera fila si es relevante (opcional)
            if (!listaTraspasos.isEmpty() && getTablaTraspasos().getRowCount() > 0) {
                // getTablaTraspasos().setRowSelectionInterval(0, 0);
            }
        });

        // Configurar listener de selección de tabla
        configurarListenerSeleccionTabla();

        // Cargar bodegas en filtros (esto puede disparar eventos)
        cargarBodegasEnFiltros();

        // ═══════════════════════════════════════════════════════════════════════════
        // CONFIGURACIÓN DE CALENDARIO (DATEPICKER) - SOLUCIÓN ROBUSTA
        // ═══════════════════════════════════════════════════════════════════════════

        // 1. CREAR Y CONFIGURAR EL DATEPICKER PRIMERO
        datePicker = new DatePicker();
        datePicker.setCloseAfterSelected(true);
        datePicker.setDateSelectionMode(DatePicker.DateSelectionMode.SINGLE_DATE_SELECTED);

        // 2. VINCULAR EL EDITOR (el DatePicker configurará automáticamente el
        // formatter)
        datePicker.setEditor(TfdFecha);

        // 3. CONFIGURAR FECHA INICIAL (HOY)
        LocalDate fechaHoy = LocalDate.now();
        datePicker.setSelectedDate(fechaHoy);
        // 4. CONFIGURAR ICONO DE CALENDARIO
        FontIcon calendarIcon = FontIcon.of(FontAwesomeSolid.CALENDAR_ALT, 16, Color.GRAY);
        JButton calendarBtn = new JButton(calendarIcon);
        calendarBtn.setBorder(null);
        calendarBtn.setContentAreaFilled(false);
        calendarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        calendarBtn.setFocusable(false);

        // 5. ACCIÓN DEL BOTÓN - MOSTRAR CALENDARIO
        calendarBtn.addActionListener(e -> {
            datePicker.showPopup();
        });

        // 6. LISTENER DEL DATEPICKER - CUANDO SE SELECCIONA UNA FECHA
        // El DatePicker actualiza automáticamente el campo TfdFecha
        // Solo necesitamos recargar la tabla
        datePicker.addDateSelectionListener((dateEvent) -> {
            LocalDate fechaSeleccionada = datePicker.getSelectedDate();

            if (fechaSeleccionada != null) {
                // Pequeño delay para asegurar que el DatePicker actualice el campo
                SwingUtilities.invokeLater(() -> {
                    // Leer el valor del campo después de que el DatePicker lo actualizó
                    String fechaTexto = TfdFecha.getText();
                    // Recargar tabla con la nueva fecha
                    cargarTraspasos();
                });
            }
        });

        // Agregar icono al campo de texto usando FlatLaf
        TfdFecha.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, calendarBtn);
        TfdFecha.putClientProperty(FlatClientProperties.STYLE, "arc:10;margin:0,5,0,5");

        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font"); // Usa estilo de fuente h1

        // Cargar traspasos inicialmente (solo si no hay errores)
        SwingUtilities.invokeLater(() -> {
            try {
                cargarTraspasos();

                // Inicializar estado de botones
                actualizarBotonesSegunEstado();
                actualizarEstiloBotonesFiltro();

            } catch (Exception e) {
                System.err.println("Error en carga inicial de traspasos: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Instala la fuente Roboto (extensión de FlatLaf)
        FlatRobotoFont.install();

        // Establece fuente predeterminada para todos los componentes
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        menu = new JPopupMenu();
        search = new PanelSearch();
        FlatSVGIcon iconoAjustes = new FlatSVGIcon("raven/icon/svg/ajuste.svg", 24, 24 // tamaño deseado del icono
        );

        BtnAjustes.setIcon(iconoAjustes);
        BtnAjustes.setHorizontalTextPosition(SwingConstants.RIGHT); // texto a la derecha del icono
        BtnAjustes.setIconTextGap(8); // espacio entre icono y texto

        // Opcional: estilo más limpio
        BtnAjustes.setFocusPainted(false);
        BtnAjustes.setBorderPainted(false);
        BtnAjustes.setContentAreaFilled(false); // si estás usando un botón tipo flat

        BtnAjustes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirAjustesTraspasoConfig();
            }
        });

        CheAjuste.setSelected(raven.clases.productos.TraspasoConfig.getAutoApplyPref());
        CheAjuste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                raven.clases.productos.TraspasoConfig.setAutoApplyPref(CheAjuste.isSelected());
            }
        });

        // Configurar atajos de teclado
        setupKeyboardShortcuts();
    }

    private void abrirBuscadorTraspasos() {
        BuscadorTraspasoPanel panel = new BuscadorTraspasoPanel();
        panel.setPreferredSize(new Dimension(900, 600));

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cerrar", SimpleModalBorder.CANCEL_OPTION)
        };

        ModalDialog.showModal(this,
                new SimpleModalBorder(panel, "Buscar Producto en Traspasos", options, (controller, action) -> {
                }));
    }

    /**
     * Configura los atajos de teclado para el módulo de traspasos
     */
    private void setupKeyboardShortcuts() {
        // Obtener el InputMap y ActionMap del panel
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();

        // ═══════════════════════════════════════════════════════════════════════════
        // CTRL + SHIFT + F: Buscar traspasos por producto
        // ═══════════════════════════════════════════════════════════════════════════
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                "buscarTraspasoProducto");
        actionMap.put("buscarTraspasoProducto", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                abrirBuscadorTraspasos();
            }
        });

        // ═══════════════════════════════════════════════════════════════════════════
        // CTRL + N: Crear nuevo traspaso
        // ═══════════════════════════════════════════════════════════════════════════
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "nuevoTraspaso");
        actionMap.put("nuevoTraspaso", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                btnNuevoActionPerformed(e);
            }
        });

        // ═══════════════════════════════════════════════════════════════════════════
        // CTRL + R: Recargar/actualizar tabla
        // ═══════════════════════════════════════════════════════════════════════════
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "recargarTabla");
        actionMap.put("recargarTabla", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cargarTraspasos();
                Toast.show(traspasos.this, Toast.Type.SUCCESS, "Tabla actualizada");
            }
        });

        // ═══════════════════════════════════════════════════════════════════════════
        // F5: Recargar/actualizar tabla (alternativa)
        // ═══════════════════════════════════════════════════════════════════════════
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "recargarTablaF5");
        actionMap.put("recargarTablaF5", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cargarTraspasos();
                Toast.show(traspasos.this, Toast.Type.SUCCESS, "Tabla actualizada");
            }
        });

    }

    // Método de inicialización personalizado para componentes UI

    private void init() {
        // ═══════════════════════════════════════════════════════════════════════════
        // CONFIGURACIÓN DE FUENTES
        // ═══════════════════════════════════════════════════════════════════════════
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // ═══════════════════════════════════════════════════════════════════════════
        // TÍTULO CON ICONO
        // ═══════════════════════════════════════════════════════════════════════════
        Color iconColor = UIManager.getColor("Label.foreground");
        FontIcon iconTitulo = FontIcon.of(FontAwesomeSolid.EXCHANGE_ALT);
        iconTitulo.setIconSize(24);
        iconTitulo.setIconColor(iconColor);
        getLb().setIcon(iconTitulo);
        getLb().setIconTextGap(12);
        getLb().putClientProperty(FlatClientProperties.STYLE, FONT_HEADER_STYLE);

        // ═══════════════════════════════════════════════════════════════════════════
        // ESTILOS DE PANELES
        // ═══════════════════════════════════════════════════════════════════════════
        getPanelAcciones().putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        getPanelFiltros().putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_FILTROS);

        // ═══════════════════════════════════════════════════════════════════════════
        // TABLA OPTIMIZADA
        // ═══════════════════════════════════════════════════════════════════════════
        getTablaTraspasos().putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;" +
                        "showVerticalLines:false;" +
                        "rowHeight:45;" +
                        "intercellSpacing:10,5;" +
                        "selectionBackground:$TableHeader.hoverBackground;" +
                        "selectionForeground:$Table.foreground");

        // ═══════════════════════════════════════════════════════════════════════════
        // CAMPO DE BÚSQUEDA
        // ═══════════════════════════════════════════════════════════════════════════
        getTxtBuscarN().putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        FontIcon iconBuscar = FontIcon.of(FontAwesomeSolid.SEARCH);
        iconBuscar.setIconSize(14);
        iconBuscar.setIconColor(iconColor);
        getTxtBuscarN().putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, iconBuscar);

        // ═══════════════════════════════════════════════════════════════════════════
        // BOTONES CON ICONOS Y ESTILOS MODERNOS
        // ═══════════════════════════════════════════════════════════════════════════
        // Botón Nuevo
        FontIcon iconNuevo = FontIcon.of(FontAwesomeSolid.PLUS);
        iconNuevo.setIconSize(14);
        iconNuevo.setIconColor(Color.WHITE);
        getBtnNuevo().setIcon(iconNuevo);
        getBtnNuevo().putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_NUEVO);
        getBtnNuevo().setPreferredSize(new Dimension(140, 36));

        // Botón Autorizar
        FontIcon iconAutorizar = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE);
        iconAutorizar.setIconSize(14);
        iconAutorizar.setIconColor(Color.WHITE);
        getBtnAutori().setIcon(iconAutorizar);
        getBtnAutori().putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_AUTORIZAR);
        getBtnAutori().setPreferredSize(new Dimension(120, 36));

        // Botón Enviar
        FontIcon iconEnviar = FontIcon.of(FontAwesomeSolid.TRUCK);
        iconEnviar.setIconSize(14);
        iconEnviar.setIconColor(Color.WHITE);
        getBtnEnviar().setIcon(iconEnviar);
        getBtnEnviar().putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_ENVIAR);
        getBtnEnviar().setPreferredSize(new Dimension(140, 36));

        // Botón Exportar
        FontIcon iconExportar = FontIcon.of(FontAwesomeSolid.FILE_EXPORT);
        iconExportar.setIconSize(14);
        iconExportar.setIconColor(Color.WHITE);
        getBtnExport().setIcon(iconExportar);
        getBtnExport().putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_EXPORTAR);
        getBtnExport().setPreferredSize(new Dimension(120, 36));

        getBtnExport().setPreferredSize(new Dimension(120, 36));

        // Botón Reporte Diario
        FontIcon iconReporte = FontIcon.of(FontAwesomeSolid.FILE_PDF);
        iconReporte.setIconSize(14);
        iconReporte.setIconColor(Color.WHITE);
        btnReporte = new javax.swing.JButton("Reporte");
        btnReporte.setForeground(Color.WHITE);
        btnReporte.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReporte.setIcon(iconReporte);
        btnReporte.putClientProperty(FlatClientProperties.STYLE, STYLE_BTN_REPORTE);
        btnReporte.setPreferredSize(new Dimension(120, 36));
        btnReporte.addActionListener(e -> generarReporteDiario());

        // Agregar botón al panel de acciones (asegurar que el container tenga layout
        // adecuado)
        // Buscamos donde se agregan los botones para insertar el nuevo
        // Nota: Como es un panel generado, lo agregamos al mismo padre que BtnExport
        if (getBtnExport().getParent() != null) {
            getBtnExport().getParent().add(btnReporte);
        }
        getBtnExport().addActionListener(e -> exportarSeleccionadoAPDF());

        // ═══════════════════════════════════════════════════════════════════════════
        // CONFIGURACIÓN MODAL
        // ═══════════════════════════════════════════════════════════════════════════
        ModalDialog.getDefaultOption()
                .setOpacity(0.3f)
                .getLayoutOption().setAnimateScale(0.1f);
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM);

        // ═══════════════════════════════════════════════════════════════════════════
        // PANEL DE ESTADÍSTICAS RÁPIDAS
        // ═══════════════════════════════════════════════════════════════════════════
        javax.swing.JPanel panelStats = new javax.swing.JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
        panelStats.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL_STATS);
        panelStats.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Crear etiquetas de estadísticas con estilos
        lblStatTotal = crearEtiquetaStat("Total:", "0", new Color(33, 150, 243));
        lblStatPendientes = crearEtiquetaStat("Pendientes:", "0", new Color(255, 193, 7));
        lblStatEnTransito = crearEtiquetaStat("En Tránsito:", "0", new Color(255, 152, 0));
        lblStatRecibidos = crearEtiquetaStat("Recibidos:", "0", new Color(76, 175, 80));

        panelStats.add(lblStatTotal);
        panelStats.add(Box.createHorizontalStrut(10));
        panelStats.add(lblStatPendientes);
        panelStats.add(Box.createHorizontalStrut(10));
        panelStats.add(lblStatEnTransito);
        panelStats.add(Box.createHorizontalStrut(10));
        panelStats.add(lblStatRecibidos);

        // Agregar panel de stats al final del panel de filtros
        getPanelFiltros().add(panelStats);
        getPanelFiltros().revalidate();

        // ═══════════════════════════════════════════════════════════════════════════
        // CONFIGURACIÓN DE PESTAÑAS (ENVIADOS/RECIBIDOS)
        // ═══════════════════════════════════════════════════════════════════════════
        // Configurar layout grid 1x2 para distribución 50/50
        panelhead2.setLayout(new java.awt.GridLayout(1, 2, 20, 0)); // 1 fila, 2 cols, 20px gap
        panelhead2.setOpaque(false); // Transparente para integrar con el fondo

        // Quitar bordes o fondos predeterminados si existen
        panelhead2.putClientProperty(FlatClientProperties.STYLE, "background:null; border:0,0,0,0");

        // IMPORTANTE: Agregar los botones al panel porque GroupLayout (NetBeans) no usa
        // add()
        // y al cambiar el layout, el panel quedaría vacío.
        panelhead2.removeAll(); // Limpiar por si acaso
        panelhead2.add(btnEnviado);
        panelhead2.add(btnRecibido);

        // Inicializar estilos de los botones
        actualizarEstiloBotonesFiltro();

        // ═══════════════════════════════════════════════════════════════════════════
        // CONEXIÓN A BD
        // ═══════════════════════════════════════════════════════════════════════════
        try {
            conexion.getInstance().connectToDatabase();
            conexion.getInstance().close();
        } catch (SQLException e) {
            getLOGGER().log(Level.SEVERE, "Error de conexión a la base de datos", e);
        }
    }

    /**
     * Crea una etiqueta de estadística con formato HTML estilizado.
     */
    private JLabel crearEtiquetaStat(String titulo, String valor, Color color) {
        String colorHex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        JLabel label = new JLabel("<html><b>" + titulo + "</b> <span style='color:" + colorHex
                + ";font-size:13px;font-weight:bold;'>" + valor + "</span></html>");
        label.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        return label;
    }

    /**
     * Actualiza las etiquetas de estadísticas con los valores actuales.
     */
    private void actualizarEstadisticasTraspasos(int total, int pendientes, int enTransito, int recibidos) {
        if (lblStatTotal != null) {
            lblStatTotal.setText("<html><b>Total:</b> <span style='color:#2196F3;font-size:13px;font-weight:bold;'>"
                    + total + "</span></html>");
        }
        if (lblStatPendientes != null) {
            lblStatPendientes
                    .setText("<html><b>Pendientes:</b> <span style='color:#FFC107;font-size:13px;font-weight:bold;'>"
                            + pendientes + "</span></html>");
        }
        if (lblStatEnTransito != null) {
            lblStatEnTransito.setText(
                    "<html><b>En Tránsito:</b> <span style='color:#FF9800;font-size:13px;font-weight:bold;'>"
                            + enTransito + "</span></html>");
        }
        if (lblStatRecibidos != null) {
            lblStatRecibidos
                    .setText("<html><b>Recibidos:</b> <span style='color:#4CAF50;font-size:13px;font-weight:bold;'>"
                            + recibidos + "</span></html>");
        }
    }

    private void configurarListenerSeleccionTabla() {
        getTablaTraspasos().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotonesSegunEstado();
            }
        });
    }

    private void initPlaceHolders() {

        getTxtBuscarN().putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar traspaso...");

    }

    private void configurarTabla() {
        // Configurar evento de acciones de tabla
        TableActionEvent event = crearEventoAccionesTraspaso();

        // Configurar renderizador y editor personalizados para la columna de acciones
        getTablaTraspasos().getColumnModel().getColumn(6).setCellRenderer(new TableActionCellRender());
        getTablaTraspasos().getColumnModel().getColumn(6).setCellEditor(new TableActionCellEditor(event));
        getTablaTraspasos().setName("tablaTraspasos");
        getTablaTraspasos().putClientProperty("traspasos.filtroDireccion", filtroDireccion);

        // Configurar alineación del número de traspaso (columna 0)
        getTablaTraspasos().getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i,
                    int i1) {
                setHorizontalAlignment(SwingConstants.CENTER);
                return super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
            }
        });

        // Configurar renderizador para la columna Estado (columna 4)
        getTablaTraspasos().getColumnModel().getColumn(4).setCellRenderer(crearRenderizadorEstadoTraspaso());

        // Configurar estilo de la tabla
        getTablaTraspasos().putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");

        // Ocultar columna auxiliar 'hasCaja' (índice 7)
        try {
            javax.swing.table.TableColumn col = getTablaTraspasos().getColumnModel().getColumn(7);
            col.setMinWidth(0);
            col.setMaxWidth(0);
            col.setPreferredWidth(0);
            getTablaTraspasos().getTableHeader().getColumnModel().getColumn(7).setMinWidth(0);
            getTablaTraspasos().getTableHeader().getColumnModel().getColumn(7).setMaxWidth(0);
        } catch (Exception ignore) {
        }

        // Configurar encabezado de la tabla
        getTablaTraspasos().getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");

        // Configurar indicadores de carga visual
        if (refreshManager != null) {
            Container scrollParent = getTablaTraspasos().getParent();
            if (scrollParent != null && scrollParent.getParent() instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) scrollParent.getParent();
                refreshManager.setLoadingHandlers(
                        () -> LoadingOverlayHelper.showLoading(scrollPane, "Cargando traspasos..."),
                        () -> LoadingOverlayHelper.hideLoading(scrollPane));
            }
        }
    }

    private TableActionEvent crearEventoAccionesTraspaso() {
        return new TableActionEvent() {
            @Override
            public void onEdit(int row) {
                if (row < 0 || row >= getTablaTraspasos().getRowCount()) {
                    return;
                }

                String numeroTraspaso = getTablaTraspasos().getValueAt(row, 0).toString();
                String estado = getTablaTraspasos().getValueAt(row, 4).toString();

                // En estado recibido, el botón de edición funciona como "Pasar a facturar"
                if ("recibido".equalsIgnoreCase(estado)) {
                    if (!"RECIBIDO".equalsIgnoreCase(filtroDireccion)) {
                        Toast.show(getPanel(), Toast.Type.WARNING,
                                "La opción de facturar solo está disponible en la pestaña Recibidos");
                        return;
                    }
                    try {
                        mostrarDialogoFacturarRecibidos(numeroTraspaso);
                    } catch (Exception e) {
                        getLOGGER().log(Level.SEVERE, "Error al abrir facturar recibidos", e);
                        Toast.show(getPanel(), Toast.Type.ERROR,
                                "Error al abrir facturar recibidos: " + e.getMessage());
                    }
                    return;
                }

                // Para otros estados, mantener flujo de edición
                // Verificar si el traspaso es editable según su estado
                if (!isTraspasosEditable(estado)) {
                    Toast.show(getPanel(), Toast.Type.WARNING,
                            "No se puede editar un traspaso en estado: " + estado);
                    return;
                }

                // ===========================================================================
                // VALIDACION DE PERMISOS PARA EDITAR
                // ===========================================================================
                Integer bodegaOrigen = obtenerBodegaOrigenDeTraspaso(numeroTraspaso);
                TraspasoPermissionValidator validator = new TraspasoPermissionValidator();
                if (!validator.canEdit(estado, bodegaOrigen != null ? bodegaOrigen : 0)) {
                    Toast.show(getPanel(), Toast.Type.WARNING,
                            "No tienes permiso para editar este traspaso. Debes pertenecer a la bodega origen.");
                    return;
                }

                try {
                    editarTraspaso(numeroTraspaso);
                } catch (Exception e) {
                    getLOGGER().log(Level.SEVERE, "Error al editar traspaso", e);
                    Toast.show(getPanel(), Toast.Type.ERROR,
                            "Error al editar traspaso: " + e.getMessage());
                }
            }

            @Override
            public void onDelete(int row) {
                if (row < 0 || row >= getTablaTraspasos().getRowCount()) {
                    return;
                }

                String numeroTraspaso = getTablaTraspasos().getValueAt(row, 0).toString();
                String estado = getTablaTraspasos().getValueAt(row, 4).toString();

                // Solo permitir cancelar traspasos en estado pendiente
                if ("pendiente".equals(estado)) {
                    cancelarTraspaso(numeroTraspaso);
                } else {
                    Toast.show(getPanel(), Toast.Type.WARNING,
                            "Solo se pueden cancelar traspasos en estado pendiente");
                }
            }

            @Override
            public void onView(int row) {
                if (row < 0 || row >= getTablaTraspasos().getRowCount()) {
                    return;
                }

                try {
                    String numeroTraspaso = getTablaTraspasos().getValueAt(row, 0).toString();
                    verDetalleTraspaso(numeroTraspaso);
                } catch (Exception e) {
                    getLOGGER().log(Level.SEVERE, "Error al ver detalle del traspaso", e);
                    Toast.show(getPanel(), Toast.Type.ERROR,
                            "Error al cargar detalle: " + e.getMessage());
                }
            }

            @Override
            public void onCaja(int row) {
                if (row < 0 || row >= getTablaTraspasos().getRowCount()) {
                    return;
                }
                try {
                    String numeroTraspaso = getTablaTraspasos().getValueAt(row, 0).toString();
                    abrirModalConvertirCajaAPares(numeroTraspaso);
                } catch (Exception e) {
                    getLOGGER().log(Level.SEVERE, "Error al abrir conversión de caja", e);
                    Toast.show(getPanel(), Toast.Type.ERROR,
                            "Error al abrir conversión: " + e.getMessage());
                }
            }

            @Override
            public void onCancel(int row) {
                if (row < 0 || row >= getTablaTraspasos().getRowCount()) {
                    return;
                }

                String numeroTraspaso = getTablaTraspasos().getValueAt(row, 0).toString();
                String estado = getTablaTraspasos().getValueAt(row, 4).toString();

                // ===========================================================================
                // VALIDACION DE PERMISOS CON BODEGA
                // ===========================================================================
                Integer bodegaOrigen = obtenerBodegaOrigenDeTraspaso(numeroTraspaso);
                Integer bodegaDestino = obtenerBodegaDestinoDeTraspaso(numeroTraspaso);

                TraspasoPermissionValidator validator = new TraspasoPermissionValidator();
                if (!validator.canCancel(estado, bodegaOrigen != null ? bodegaOrigen : 0,
                        bodegaDestino != null ? bodegaDestino : 0)) {
                    Toast.show(getPanel(), Toast.Type.WARNING,
                            "No tienes permiso para cancelar este traspaso");
                    return;
                }

                if ("en_transito".equalsIgnoreCase(estado)) {
                    cancelarTraspasoEnTransito(numeroTraspaso);
                } else if ("recibido".equalsIgnoreCase(estado)) {
                    cancelarTraspasoEnTransito(numeroTraspaso);
                } else if ("autorizado".equalsIgnoreCase(estado) || "pendiente".equalsIgnoreCase(estado)) {
                    cancelarTraspaso(numeroTraspaso);
                } else {
                    Toast.show(getPanel(), Toast.Type.WARNING,
                            "No es posible cancelar este traspaso en el estado actual");
                }
            }

        };
    }

    private void abrirModalConvertirCajaAPares(String numeroTraspaso) throws SQLException {
        java.util.List<Object[]> cajas = new java.util.ArrayList<>();
        Integer idBodegaDestino = null;
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = con.prepareStatement(
                        "SELECT tr.id_bodega_destino, " +
                                "       td.id_variante AS id_variante_caja, " +
                                "       c.id_color, " +
                                "       c.nombre AS color_nombre, " +
                                "       p.id_producto, " +
                                "       COALESCE(td.cantidad_recibida, 0) AS cajas_recibidas, " +
                                "       (COALESCE(td.cantidad_recibida, 0) - COALESCE(conv.convertidas, 0)) AS cajas_convertibles, "
                                +
                                "       p.nombre AS producto_nombre, " +
                                "       t.genero, " +
                                "       t.numero AS talla_numero, " +
                                "       td.id_detalle_traspaso " +
                                "FROM traspasos tr " +
                                "INNER JOIN traspaso_detalles td ON tr.id_traspaso = td.id_traspaso " +
                                "INNER JOIN producto_variantes pv ON td.id_variante = pv.id_variante " +
                                "INNER JOIN productos p ON pv.id_producto = p.id_producto " +
                                "INNER JOIN tallas t ON pv.id_talla = t.id_talla " +
                                "INNER JOIN colores c ON pv.id_color = c.id_color " +
                                "LEFT JOIN ( " +
                                "    SELECT id_detalle_traspaso, id_variante_caja, SUM(cajas_convertidas) AS convertidas "
                                +
                                "    FROM conversion_caja_traspaso " +
                                "    GROUP BY id_detalle_traspaso, id_variante_caja " +
                                ") conv ON conv.id_detalle_traspaso = td.id_detalle_traspaso AND conv.id_variante_caja = td.id_variante "
                                +
                                "WHERE tr.numero_traspaso = ? " +
                                "  AND LOWER(td.tipo) = 'caja' " +
                                "  AND (COALESCE(td.cantidad_recibida, 0) - COALESCE(conv.convertidas, 0)) > 0 " +
                                "  AND EXISTS (SELECT 1 FROM inventario_bodega ib " +
                                "              WHERE ib.id_bodega = tr.id_bodega_destino " +
                                "                AND ib.id_variante = td.id_variante " +
                                "                AND ib.activo = 1 " +
                                "                AND COALESCE(ib.Stock_caja,0) > 0)")) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idBodegaDestino = rs.getInt("id_bodega_destino");
                    int idVar = rs.getInt("id_variante_caja");
                    int idColor = rs.getInt("id_color");
                    String nombreColor = rs.getString("color_nombre");
                    int idProd = rs.getInt("id_producto");
                    int cajasRecibidas = Math.max(0, rs.getInt("cajas_recibidas"));
                    int cajasDisp = Math.max(0, rs.getInt("cajas_convertibles"));
                    String productoNombre = rs.getString("producto_nombre");
                    String genero = rs.getString("genero");
                    String tallaNumero = rs.getString("talla_numero");
                    int idDetalle = rs.getInt("id_detalle_traspaso");
                    cajas.add(new Object[] { idVar, idColor, idProd, nombreColor, productoNombre, genero, cajasDisp,
                            tallaNumero, idDetalle, cajasRecibidas });
                }
            }
        }

        if (cajas.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "No hay cajas disponibles para convertir en este traspaso");
            return;
        }

        Object[] seleccion = mostrarSelectorDeCajas(numeroTraspaso, cajas);
        if (seleccion == null) {
            return;
        }

        int idVarianteCaja = (int) seleccion[0];
        int idColorCaja = (int) seleccion[1];
        int idProducto = (int) seleccion[2];
        String nombreColor = (String) seleccion[3];
        int idDetalleTraspaso = (int) seleccion[4];
        int maxCajas = (int) seleccion[5];

        final Integer bodegaDestinoFinal = idBodegaDestino;
        final Integer varianteCajaFinal = idVarianteCaja;

        final int idDetalleFinal = idDetalleTraspaso;
        if (maxCajas <= 0) {
            String mensajeDetallado;
            mensajeDetallado = "No hay cajas disponibles para convertir.";
            Toast.show(this, Toast.Type.WARNING, mensajeDetallado);
            return;
        }
        CreateTallas.TallasFormCallback callback = (tallasSeleccionadas, tipoVenta, idVarCaja, imagenComun,
                cajasAConvertir) -> {
            try {
                convertirCajaAParesEnBodega(numeroTraspaso, idDetalleFinal, idProducto, idColorCaja, bodegaDestinoFinal,
                        varianteCajaFinal, tallasSeleccionadas, cajasAConvertir, imagenComun);
                Toast.show(this, Toast.Type.SUCCESS, "Caja convertida a pares en bodega destino");
            } catch (Exception e) {
                getLOGGER().log(Level.SEVERE, "Error convirtiendo caja a pares", e);
                Toast.show(this, Toast.Type.ERROR, "Error convirtiendo caja: " + e.getMessage());
            }
        };

        CreateTallas ct = new CreateTallas(idProducto, idColorCaja, "caja", callback);
        ct.setDescontarCajaAlAceptar(true);
        ct.setCajaSeleccionada(idVarianteCaja, idColorCaja, nombreColor != null ? nombreColor : "");
        ct.setMaxCajasConvertibles(maxCajas);

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {};
        ModalDialog.showModal(this,
                new SimpleModalBorder(ct, "Convertir caja a pares", options, new ModalCallback() {
                    @Override
                    public void action(ModalController mc, int i) {
                        if (i == SimpleModalBorder.OPENED) {
                            ct.setModalController(mc);
                        }
                    }
                }));
    }

    private Object[] mostrarSelectorDeCajas(String numeroTraspaso, java.util.List<Object[]> cajas) {
        javax.swing.JDialog dialog = new javax.swing.JDialog();
        dialog.setTitle("Seleccionar caja: " + numeroTraspaso);
        dialog.setModal(true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
        javax.swing.JTextField filtro = new javax.swing.JTextField();
        javax.swing.JTable tabla = new javax.swing.JTable();
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tabla);
        javax.swing.JButton btnAceptar = new javax.swing.JButton("Aceptar");
        javax.swing.JButton btnCancelar = new javax.swing.JButton("Cancelar");

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(new Object[] {
                "Seleccionar", "Producto", "Talla", "Color", "Género", "Cajas recibidas", "Cajas disponibles"
        }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Boolean.class;
                    case 5:
                        return Integer.class;
                    case 6:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };
        tabla.setModel(model);
        for (Object[] c : cajas) {
            String productoNombre = String.valueOf(c[4]);
            String tallaNumero = String.valueOf(c[7]);
            String nombreColor = String.valueOf(c[3]);
            String genero = String.valueOf(c[5]);
            int cajasDisp = (Integer) c[6];
            int cajasRecibidas = (Integer) c[9];
            model.addRow(new Object[] { false, productoNombre, tallaNumero, nombreColor, genero, cajasRecibidas,
                    cajasDisp });
        }

        final java.util.Map<Integer, javax.swing.ImageIcon> imageCache = new java.util.HashMap<>();
        tabla.getColumnModel().getColumn(1).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

                javax.swing.JTextArea text = new javax.swing.JTextArea(String.valueOf(value));
                text.setWrapStyleWord(true);
                text.setLineWrap(true);
                text.setOpaque(false);
                text.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 20));
                text.setFont(table.getFont());

                javax.swing.JLabel img = new javax.swing.JLabel();
                img.setOpaque(false);
                img.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                img.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 20, 0, 10));

                try {
                    Object[] data = cajas.get(table.convertRowIndexToModel(row));
                    int idVar = (Integer) data[0];
                    javax.swing.ImageIcon icon = imageCache.get(idVar);
                    int tableWidth = table.getWidth();
                    int size = (tableWidth < 600 ? 60 : 100);
                    if (icon == null) {
                        raven.clases.productos.ServiceProductVariant svc = new raven.clases.productos.ServiceProductVariant();
                        byte[] bytes = svc.getVariantImage(idVar);
                        if (bytes != null && bytes.length > 0) {
                            javax.swing.ImageIcon ic = new javax.swing.ImageIcon(bytes);
                            java.awt.Image scaled = ic.getImage().getScaledInstance(size, size,
                                    java.awt.Image.SCALE_SMOOTH);
                            icon = new javax.swing.ImageIcon(scaled);
                            imageCache.put(idVar, icon);
                        }
                    }
                    if (icon != null) {
                        java.awt.Image scaled = icon.getImage().getScaledInstance(size, size,
                                java.awt.Image.SCALE_SMOOTH);
                        img.setIcon(new javax.swing.ImageIcon(scaled));
                    }
                } catch (Exception ignore) {
                }

                javax.swing.JPanel right = new javax.swing.JPanel(
                        new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
                right.setOpaque(false);
                right.add(img);

                panel.add(text, java.awt.BorderLayout.CENTER);
                panel.add(right, java.awt.BorderLayout.EAST);
                return panel;
            }
        });

        tabla.setRowHeight(110);
        tabla.setIntercellSpacing(new java.awt.Dimension(10, 25));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new java.awt.Color(0, 0, 0, 0));

        int totalWidth = dialog.getWidth();
        int productPref = (int) (totalWidth * 0.5); // aumentar ~30% sobre estándar
        try {
            tabla.getColumnModel().getColumn(1).setPreferredWidth(productPref);
        } catch (Exception ignore) {
        }
        try {
            tabla.getColumnModel().getColumn(2).setPreferredWidth(80);
        } catch (Exception ignore) {
        }
        javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(
                model);
        tabla.setRowSorter(sorter);
        filtro.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void apply() {
                String t = filtro.getText();
                if (t == null || t.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                    return;
                }
                sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t)));
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                apply();
            }
        });

        javax.swing.JPanel south = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        south.add(new javax.swing.JLabel("Filtrar:"));
        south.add(filtro);
        south.add(btnCancelar);
        south.add(btnAceptar);

        panel.add(sp, java.awt.BorderLayout.CENTER);
        panel.add(south, java.awt.BorderLayout.SOUTH);
        dialog.setContentPane(panel);

        final Object[][] seleccion = new Object[1][];
        btnCancelar.addActionListener(e -> {
            dialog.dispose();
        });
        btnAceptar.addActionListener(e -> {
            int selCount = 0;
            int selRowModel = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean b = (Boolean) model.getValueAt(i, 0);
                if (b != null && b) {
                    selCount++;
                    selRowModel = i;
                }
            }
            if (selCount == 0) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione al menos una caja");
                return;
            }
            if (selCount > 1) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo una caja");
                return;
            }
            Object[] data = cajas.get(selRowModel);
            int idVar = (Integer) data[0];
            int idColor = (Integer) data[1];
            int idProd = (Integer) data[2];
            String nombreColor = String.valueOf(data[3]);
            int idDetalle = (Integer) data[8];
            int cajasDisp = (Integer) data[6];
            seleccion[0] = new Object[] { idVar, idColor, idProd, nombreColor, idDetalle, cajasDisp };
            dialog.dispose();
        });

        dialog.setVisible(true);
        return seleccion[0];
    }

    private String obtenerNombreColor(int idColor) {
        String nombre = "";
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = con
                        .prepareStatement("SELECT nombre FROM colores WHERE id_color=? LIMIT 1")) {
            ps.setInt(1, idColor);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    nombre = rs.getString(1);
            }
        } catch (Exception ignore) {
        }
        return nombre;
    }

    private void convertirCajaAParesEnBodega(String numeroTraspaso, Integer idDetalleTraspaso, Integer idProducto,
            Integer idColor, Integer idBodegaDestino, Integer idVarianteCaja,
            List<raven.application.form.productos.creates.CreateTallas.TallaVariante> tallasSeleccionadas,
            int cajasAConvertir, byte[] imagenComun) throws SQLException {
        if (numeroTraspaso == null || idDetalleTraspaso == null || idProducto == null || idColor == null
                || idBodegaDestino == null || idVarianteCaja == null)
            throw new SQLException("Datos incompletos para conversión");
        if (cajasAConvertir <= 0)
            cajasAConvertir = 1;
        CreateTallas ct = new CreateTallas(idProducto, idColor, "caja", null);
        ct.setDescontarCajaAlAceptar(true);
        if (imagenComun != null && imagenComun.length > 0) {
            ct.setImagenParaNuevasVariantes(imagenComun);
        }
        ct.convertirCajaAParesEnBodega(idBodegaDestino, idVarianteCaja, tallasSeleccionadas, cajasAConvertir);

        // Registrar conversión y movimientos de inventario
        try (java.sql.Connection con = conexion.getInstance().createConnection();
                java.sql.PreparedStatement psT = con
                        .prepareStatement("SELECT id_traspaso FROM traspasos WHERE numero_traspaso=? LIMIT 1");
                java.sql.PreparedStatement psI = con.prepareStatement(
                        "INSERT INTO conversion_caja_traspaso (id_traspaso, id_detalle_traspaso, id_bodega_destino, id_variante_caja, cajas_convertidas, pares_generados, fecha_conversion, id_usuario, observaciones) "
                                +
                                "VALUES (?,?,?,?,?,?,NOW(),?,?)");
                java.sql.PreparedStatement psMovCaja = con.prepareStatement(
                        "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares, fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones) "
                                +
                                "VALUES (?, ?, 'ajuste caja', ?, NULL, CURDATE(), ?, 'conversion', ?, ?)")) {
            con.setAutoCommit(false);
            psT.setString(1, numeroTraspaso);
            int idTraspaso = 0;
            try (java.sql.ResultSet rs = psT.executeQuery()) {
                if (rs.next())
                    idTraspaso = rs.getInt(1);
            }
            int paresGenerados = cajasAConvertir * 24;
            Integer idUsuario = null;
            try {
                raven.clases.admin.UserSession us = raven.clases.admin.UserSession.getInstance();
                if (us != null && us.getCurrentUser() != null)
                    idUsuario = us.getCurrentUser().getIdUsuario();
            } catch (Exception ignore) {
            }
            // Verificar capacidad disponible antes de registrar
            try (java.sql.PreparedStatement psCap = con.prepareStatement(
                    "SELECT (COALESCE(td.cantidad_recibida,0) - COALESCE(cv.convertidas,0)) AS disponibles " +
                            "FROM traspaso_detalles td " +
                            "LEFT JOIN (SELECT id_detalle_traspaso, id_variante_caja, SUM(cajas_convertidas) AS convertidas "
                            +
                            "           FROM conversion_caja_traspaso " +
                            "           GROUP BY id_detalle_traspaso, id_variante_caja) cv " +
                            "       ON cv.id_detalle_traspaso = td.id_detalle_traspaso AND cv.id_variante_caja = td.id_variante "
                            +
                            "WHERE td.id_detalle_traspaso = ?")) {
                psCap.setInt(1, idDetalleTraspaso);
                try (java.sql.ResultSet rs = psCap.executeQuery()) {
                    if (rs.next()) {
                        int disponibles = Math.max(0, rs.getInt(1));
                        if (cajasAConvertir > disponibles) {
                            con.rollback();
                            throw new SQLException("La conversión excede las cajas disponibles (" + disponibles + ")");
                        }
                    }
                }
            }
            // Anti-duplicados: evitar doble clic registrando el mismo evento en segundos
            // recientes
            try (java.sql.PreparedStatement checkPs = con.prepareStatement(
                    "SELECT COUNT(*) FROM conversion_caja_traspaso " +
                            "WHERE id_detalle_traspaso = ? AND id_variante_caja = ? AND cajas_convertidas = ? " +
                            "AND pares_generados = ? " +
                            "AND (id_usuario IS NULL OR id_usuario = COALESCE(?, id_usuario)) " +
                            "AND TIMESTAMPDIFF(SECOND, fecha_conversion, NOW()) < 5")) {
                checkPs.setInt(1, idDetalleTraspaso);
                checkPs.setInt(2, idVarianteCaja);
                checkPs.setInt(3, cajasAConvertir);
                checkPs.setInt(4, paresGenerados);
                if (idUsuario != null)
                    checkPs.setInt(5, idUsuario);
                else
                    checkPs.setNull(5, java.sql.Types.INTEGER);
                try (java.sql.ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        con.rollback();
                        throw new SQLException("Conversión duplicada detectada. Intenta nuevamente en unos segundos.");
                    }
                }
            }
            psI.setInt(1, idTraspaso);
            psI.setInt(2, idDetalleTraspaso);
            psI.setInt(3, idBodegaDestino);
            psI.setInt(4, idVarianteCaja);
            psI.setInt(5, cajasAConvertir);
            psI.setInt(6, paresGenerados);
            if (idUsuario != null)
                psI.setInt(7, idUsuario);
            else
                psI.setNull(7, java.sql.Types.INTEGER);
            psI.setString(8, "Conversion caja→pares desde traspasos");
            psI.executeUpdate();

            // Movimiento de inventario: ajuste caja (descuento de cajas)
            psMovCaja.setInt(1, idProducto);
            psMovCaja.setInt(2, idVarianteCaja);
            psMovCaja.setInt(3, cajasAConvertir);
            psMovCaja.setInt(4, idDetalleTraspaso);
            if (idUsuario != null)
                psMovCaja.setInt(5, idUsuario);
            else
                psMovCaja.setNull(5, java.sql.Types.INTEGER);
            psMovCaja.setString(6, "Conversión caja→pares en bodega destino: " + numeroTraspaso);
            psMovCaja.executeUpdate();

            // Movimientos de inventario: ajuste par (incremento por cada talla)
            try (java.sql.PreparedStatement psMovPar = con.prepareStatement(
                    "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares, fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones) "
                            +
                            "VALUES (?, ?, 'ajuste par', ?, NULL, CURDATE(), ?, 'conversion', ?, ?)")) {
                for (raven.application.form.productos.creates.CreateTallas.TallaVariante tv : tallasSeleccionadas) {
                    int cant = Math.max(0, tv.getCantidadSeleccionada());
                    if (cant <= 0)
                        continue;
                    int idVarPar = tv.getIdVariante();
                    if (idVarPar <= 0)
                        continue; // variante creada arriba, ya debe venir seteada
                    psMovPar.setInt(1, idProducto);
                    psMovPar.setInt(2, idVarPar);
                    psMovPar.setInt(3, cant);
                    psMovPar.setInt(4, idDetalleTraspaso);
                    if (idUsuario != null)
                        psMovPar.setInt(5, idUsuario);
                    else
                        psMovPar.setNull(5, java.sql.Types.INTEGER);
                    psMovPar.setString(6, "Conversión caja→pares en bodega destino: " + numeroTraspaso);
                    psMovPar.addBatch();
                }
                psMovPar.executeBatch();
            }
            con.commit();
        } catch (SQLException ex) {
            throw ex;
        }
    }

    private DefaultTableCellRenderer crearRenderizadorEstadoTraspaso() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                // Usar super para obtener configuración básica (fuente, selección, etc)
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Limpiar texto para pintar manualmente si es necesario, o configurar label
                setText(value != null ? value.toString() : "");
                setHorizontalAlignment(SwingConstants.CENTER);

                // Si hay valor, configuramos el componente para pintar el "badge"
                if (value != null) {
                    String estado = value.toString().toLowerCase();
                    Color bg = Color.decode(getColorForTraspasoStatus(estado));

                    // Configurar colores
                    if (!isSelected) {
                        setBackground(table.getBackground()); // Fondo de celda normal
                    }

                    // Usar un borde vacío para dar espacio interno
                    setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 10, 2, 10));

                    // Guardar el color del badge en una propiedad del cliente para usarlo en
                    // paintComponent
                    putClientProperty("badgeColor", bg);
                } else {
                    putClientProperty("badgeColor", null);
                }

                return this;
            }

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Pintado personalizado de alto rendimiento (sin HTML)
                Color badgeColor = (Color) getClientProperty("badgeColor");

                if (badgeColor != null && !getText().isEmpty()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                    // Calcular dimensiones del badge
                    int w = getWidth();
                    int h = getHeight();
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(getText());
                    int textHeight = fm.getHeight();

                    int badgeWidth = textWidth + 20; // Padding horizontal
                    int badgeHeight = textHeight + 4; // Padding vertical

                    // Centrar badge
                    int x = (w - badgeWidth) / 2;
                    int y = (h - badgeHeight) / 2;

                    // Pintar fondo redondeado
                    g2.setColor(badgeColor);
                    g2.fillRoundRect(x, y, badgeWidth, badgeHeight, 12, 12);

                    // Pintar texto blanco centrado
                    g2.setColor(Color.WHITE);
                    int textX = x + (badgeWidth - textWidth) / 2;
                    int textY = y + ((badgeHeight - textHeight) / 2) + fm.getAscent();
                    g2.drawString(getText(), textX, textY);

                    g2.dispose();
                } else {
                    super.paintComponent(g);
                }
            }

            private String getColorForTraspasoStatus(String status) {
                switch (status) {
                    case "pendiente":
                        return "#FFC107"; // Amarillo
                    case "autorizado":
                        return "#17A2B8"; // Azul
                    case "en_transito":
                        return "#FF9800"; // Naranja
                    case "recibido":
                        return "#28A745"; // Verde
                    case "cancelado":
                        return "#DC3545"; // Rojo
                    default:
                        return "#6C757D"; // Gris
                }
            }
        };
    }

    private boolean isTraspasosEditable(String estado) {
        return "pendiente".equals(estado.toLowerCase())
                || "autorizado".equals(estado.toLowerCase());
    }

    private void cargarBodegasEnFiltros() {
        setCargandoCombos(true); // Evitar eventos durante carga

        @SuppressWarnings({ "unchecked", "rawtypes" })
        JComboBox combo1 = getjComboBox1();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        JComboBox combo3 = getjComboBox3();

        // Placeholder mientras carga
        combo1.removeAllItems();
        combo3.removeAllItems();
        combo1.addItem("Cargando bodegas...");
        combo3.addItem("Cargando bodegas...");
        combo1.setEnabled(false);
        combo3.setEnabled(false);

        // Cargar en background
        new SwingWorker<List<Bodega>, Void>() {
            @Override
            protected List<Bodega> doInBackground() throws Exception {
                return obtenerBodegasDesdeDB();
            }

            @Override
            protected void done() {
                try {
                    List<Bodega> bodegas = get();
                    combo1.removeAllItems();
                    combo3.removeAllItems();
                    combo1.addItem("Todas las bodegas");
                    combo3.addItem("Todas las bodegas");

                    for (Bodega bodega : bodegas) {
                        combo1.addItem(bodega);
                        combo3.addItem(bodega);
                    }

                    combo1.setEnabled(true);
                    combo3.setEnabled(true);
                    configurarRendererBodegas();

                    // Seleccionar bodegas por defecto
                    raven.clases.admin.UserSession us = raven.clases.admin.UserSession.getInstance();
                    Integer defOrigen = us.getDefaultBodegaOrigen();
                    Integer defDestino = us.getDefaultBodegaDestino();
                    if (defOrigen != null)
                        seleccionarBodegaEnCombo(combo1, defOrigen);
                    if (defDestino != null)
                        seleccionarBodegaEnCombo(combo3, defDestino);

                } catch (Exception e) {
                    Logger.getLogger(traspasos.class.getName()).log(Level.WARNING, "Error cargando bodegas", e);
                    Toast.show(traspasos.this, Toast.Type.ERROR, "Error al cargar bodegas");
                    combo1.setEnabled(true);
                    combo3.setEnabled(true);
                } finally {
                    setCargandoCombos(false);
                }
            }
        }.execute();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void configurarRendererBodegas() {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                String displayText;
                if (value instanceof Bodega) {
                    displayText = ((Bodega) value).toString();
                } else {
                    displayText = value != null ? value.toString() : "";
                }
                return super.getListCellRendererComponent(list, displayText, index, isSelected, cellHasFocus);
            }
        };

        JComboBox combo1 = getjComboBox1();
        JComboBox combo3 = getjComboBox3();
        combo1.setRenderer(renderer);
        combo3.setRenderer(renderer);
    }

    private void seleccionarBodegaEnCombo(javax.swing.JComboBox combo, Integer idBodega) {
        if (idBodega == null)
            return;
        int count = combo.getItemCount();
        for (int i = 0; i < count; i++) {
            Object it = combo.getItemAt(i);
            if (it instanceof raven.clases.productos.Bodega) {
                raven.clases.productos.Bodega b = (raven.clases.productos.Bodega) it;
                if (b.getIdBodega() != null && b.getIdBodega().intValue() == idBodega.intValue()) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void abrirAjustesBodegas() {
        java.util.List<raven.clases.productos.Bodega> lista;
        try {
            lista = obtenerBodegasDesdeDB();
        } catch (java.sql.SQLException ex) {
            Toast.show(this, Toast.Type.ERROR, "Error cargando bodegas");
            return;
        }
        javax.swing.JComboBox<raven.clases.productos.Bodega> cbOrigen = new javax.swing.JComboBox<>();
        javax.swing.JComboBox<raven.clases.productos.Bodega> cbDestino = new javax.swing.JComboBox<>();
        cbOrigen.addItem(null);
        cbDestino.addItem(null);
        for (raven.clases.productos.Bodega b : lista) {
            cbOrigen.addItem(b);
            cbDestino.addItem(b);
        }
        raven.clases.admin.UserSession us = raven.clases.admin.UserSession.getInstance();
        Integer defO = us.getDefaultBodegaOrigen();
        Integer defD = us.getDefaultBodegaDestino();
        if (defO != null)
            seleccionarBodegaEnCombo(cbOrigen, defO);
        if (defD != null)
            seleccionarBodegaEnCombo(cbDestino, defD);
        javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        p.add(new javax.swing.JLabel("Bodega origen"));
        p.add(cbOrigen);
        p.add(new javax.swing.JLabel("Bodega destino"));
        p.add(cbDestino);
        int r = javax.swing.JOptionPane.showConfirmDialog(this, p, "Ajustes de bodegas",
                javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (r == javax.swing.JOptionPane.OK_OPTION) {
            raven.clases.productos.Bodega selO = (raven.clases.productos.Bodega) cbOrigen.getSelectedItem();
            raven.clases.productos.Bodega selD = (raven.clases.productos.Bodega) cbDestino.getSelectedItem();
            Integer idO = selO != null ? selO.getIdBodega() : null;
            Integer idD = selD != null ? selD.getIdBodega() : null;
            us.setDefaultBodegas(idO, idD);
            if (idO != null)
                seleccionarBodegaEnCombo(getjComboBox1(), idO);
            if (idD != null)
                seleccionarBodegaEnCombo(getjComboBox3(), idD);
            Toast.show(this, Toast.Type.SUCCESS, "Ajustes guardados");
        }
    }

    private static class UsuarioItem {
        int id;
        String nombre;
        String username;

        UsuarioItem(int id, String nombre, String username) {
            this.id = id;
            this.nombre = nombre;
            this.username = username;
        }

        public String toString() {
            return nombre + (username != null ? " (" + username + ")" : "");
        }
    }

    private java.util.List<UsuarioItem> obtenerUsuariosActivos() throws java.sql.SQLException {
        java.util.List<UsuarioItem> list = new java.util.ArrayList<>();
        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        java.sql.ResultSet rs = null;
        try {
            Integer currentId = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();
            con = raven.controlador.principal.conexion.getInstance().createConnection();
            String sql = "SELECT id_usuario, nombre, username FROM usuarios WHERE activo = 1 " +
                    (currentId != null && currentId > 0 ? "AND id_usuario <> ? " : "") + "ORDER BY nombre";
            ps = con.prepareStatement(sql);
            int idx = 1;
            if (currentId != null && currentId > 0)
                ps.setInt(idx++, currentId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new UsuarioItem(rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("username")));
            }
        } finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();
            if (con != null)
                con.close();
        }
        return list;
    }

    private void abrirAjustesTraspasoConfig() {
        java.util.List<raven.clases.productos.Bodega> bodegas;
        java.util.List<UsuarioItem> usuarios;
        try {
            bodegas = obtenerBodegasDesdeDB();
            usuarios = obtenerUsuariosActivos();
        } catch (java.sql.SQLException ex) {
            Toast.show(this, Toast.Type.ERROR, "Error cargando datos");
            return;
        }

        javax.swing.JDialog dialog = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "Ajustes de traspaso", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.GridBagLayout());
        content.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:15");
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(8, 12, 8, 12);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        javax.swing.JLabel lblTitle = new javax.swing.JLabel("Ajustes de traspaso");
        lblTitle.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "font:$h2.font");
        gbc.gridx = 0;
        gbc.gridy = 0;
        content.add(lblTitle, gbc);

        javax.swing.JComboBox<String> cbTipo = new javax.swing.JComboBox<>();
        cbTipo.addItem("Seleccionar");
        cbTipo.addItem("Envio a venta");
        cbTipo.addItem("Reposición de stock");
        cbTipo.addItem("Reorganización");
        cbTipo.addItem("Urgente");
        cbTipo.addItem("Temporada");
        cbTipo.addItem("Liquidación");
        cbTipo.setMaximumRowCount(7);
        cbTipo.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:15");
        cbTipo.setPreferredSize(new java.awt.Dimension(320, 36));
        javax.swing.JLabel lblTipo = new javax.swing.JLabel("TIPO DE TRASPASO");
        gbc.gridy++;
        content.add(lblTipo, gbc);
        gbc.gridy++;
        content.add(cbTipo, gbc);

        javax.swing.JComboBox<raven.clases.productos.Bodega> cbOrigen = new javax.swing.JComboBox<>();
        javax.swing.JComboBox<raven.clases.productos.Bodega> cbDestino = new javax.swing.JComboBox<>();
        cbOrigen.addItem(null);
        cbDestino.addItem(null);
        for (raven.clases.productos.Bodega b : bodegas) {
            cbOrigen.addItem(b);
            cbDestino.addItem(b);
        }
        cbOrigen.setMaximumRowCount(8);
        cbDestino.setMaximumRowCount(8);
        cbOrigen.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:15");
        cbDestino.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:15");
        cbOrigen.setPreferredSize(new java.awt.Dimension(320, 36));
        cbDestino.setPreferredSize(new java.awt.Dimension(320, 36));
        javax.swing.JLabel lblOrigen = new javax.swing.JLabel("BODEGA ORIGEN");
        javax.swing.JLabel lblDestino = new javax.swing.JLabel("BODEGA DE DESTINO");
        gbc.gridy++;
        content.add(lblOrigen, gbc);
        gbc.gridy++;
        content.add(cbOrigen, gbc);
        gbc.gridy++;
        content.add(lblDestino, gbc);
        gbc.gridy++;
        content.add(cbDestino, gbc);

        javax.swing.JComboBox<UsuarioItem> cbUsuario = new javax.swing.JComboBox<>();
        cbUsuario.addItem(null);
        for (UsuarioItem u : usuarios) {
            cbUsuario.addItem(u);
        }
        cbUsuario.setMaximumRowCount(8);
        cbUsuario.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:15");
        cbUsuario.setPreferredSize(new java.awt.Dimension(320, 36));
        javax.swing.JLabel lblUsuario = new javax.swing.JLabel("SOLICITADO POR");
        gbc.gridy++;
        content.add(lblUsuario, gbc);
        gbc.gridy++;
        content.add(cbUsuario, gbc);

        javax.swing.JTextArea taMotivo = new javax.swing.JTextArea(3, 22);
        taMotivo.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Escribe el motivo...");
        javax.swing.JScrollPane spMotivo = new javax.swing.JScrollPane(taMotivo);
        spMotivo.setPreferredSize(new java.awt.Dimension(320, 68));
        javax.swing.JLabel lblMotivo = new javax.swing.JLabel("MOTIVO");
        gbc.gridy++;
        content.add(lblMotivo, gbc);
        gbc.gridy++;
        content.add(spMotivo, gbc);

        javax.swing.JCheckBox chAuto = new javax.swing.JCheckBox("Aplicar automáticamente");
        // No aplicar 'arc' a JCheckBox - no es soportado por FlatLaf
        gbc.gridy++;
        content.add(chAuto, gbc);

        raven.clases.productos.TraspasoConfig cfg = raven.clases.productos.TraspasoConfig.load();
        if (cfg.getTipo() != null) {
            for (int i = 0; i < cbTipo.getItemCount(); i++) {
                if (cfg.getTipo().equals(cbTipo.getItemAt(i))) {
                    cbTipo.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (cfg.getIdOrigen() != null)
            seleccionarBodegaEnCombo(cbOrigen, cfg.getIdOrigen());
        if (cfg.getIdDestino() != null)
            seleccionarBodegaEnCombo(cbDestino, cfg.getIdDestino());
        if (cfg.getIdUsuarioSolicita() != null) {
            for (int i = 0; i < cbUsuario.getItemCount(); i++) {
                Object it = cbUsuario.getItemAt(i);
                if (it instanceof UsuarioItem && ((UsuarioItem) it).id == cfg.getIdUsuarioSolicita()) {
                    cbUsuario.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (cfg.getMotivo() != null)
            taMotivo.setText(cfg.getMotivo());
        chAuto.setSelected(raven.clases.productos.TraspasoConfig.getAutoApplyPref());

        javax.swing.JPanel footer = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 8));
        javax.swing.JButton btnCancelar = new javax.swing.JButton("Cancelar");
        javax.swing.JButton btnAceptar = new javax.swing.JButton("Aceptar");
        footer.add(btnCancelar);
        footer.add(btnAceptar);

        javax.swing.JPanel wrapper = new javax.swing.JPanel();
        wrapper.setLayout(new javax.swing.BoxLayout(wrapper, javax.swing.BoxLayout.Y_AXIS));
        wrapper.add(content);
        wrapper.add(footer);
        dialog.setContentPane(wrapper);
        dialog.pack();
        dialog.setSize(420, 540);
        dialog.setLocationRelativeTo(this);

        btnCancelar.addActionListener(e -> dialog.dispose());
        btnAceptar.addActionListener(e -> {
            raven.clases.productos.TraspasoConfig nc = new raven.clases.productos.TraspasoConfig();
            Object so = cbOrigen.getSelectedItem();
            Object sd = cbDestino.getSelectedItem();
            Object su = cbUsuario.getSelectedItem();
            nc.setIdOrigen(
                    so instanceof raven.clases.productos.Bodega ? ((raven.clases.productos.Bodega) so).getIdBodega()
                            : null);
            nc.setIdDestino(
                    sd instanceof raven.clases.productos.Bodega ? ((raven.clases.productos.Bodega) sd).getIdBodega()
                            : null);
            nc.setIdUsuarioSolicita(su instanceof UsuarioItem ? ((UsuarioItem) su).id : null);
            nc.setTipo((String) cbTipo.getSelectedItem());
            nc.setMotivo(taMotivo.getText());
            nc.setAutoApply(chAuto.isSelected());
            raven.clases.productos.TraspasoConfig.save(nc);
            CheAjuste.setSelected(raven.clases.productos.TraspasoConfig.getAutoApplyPref());
            Toast.show(this, Toast.Type.SUCCESS, "Ajustes guardados");
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void cargarDatos() {
        if (getController() == null) {
            mostrarError("Controller no inicializado");
            return;
        }

        try {
            // Generar número de traspaso automáticamente
            String numeroTraspaso = getController().generarNumeroTraspaso();
            if (numeroTraspaso != null && !numeroTraspaso.isEmpty()) {
                getjTextField2().setText(numeroTraspaso);
            } else {
                mostrarError("No se pudo generar el número de traspaso");
                return;
            }

            // Cargar bodegas con manejo de errores
            cargarBodegas();

            // Cargar datos previos si existen
            cargarDatosPrevios();

        } catch (Exception e) {
            mostrarError("Error al cargar datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarBodegas() {
        try {
            // Si no tienes traspasoService inicializado, usar conexión directa
            if (getTraspasoService() == null) {
                setTraspasoService(new TraspasoService());
            }

            List<Bodega> bodegas = getTraspasoService().obtenerBodegasActivas();

            if (bodegas != null && !bodegas.isEmpty()) {
                // Aquí puedes agregar lógica adicional si necesitas cargar las bodegas
                // en algún otro combo específico diferente a los filtros
            } else {
                Toast.show(this, Toast.Type.WARNING, "No se encontraron bodegas activas");
            }

        } catch (Exception e) {
            mostrarError("Error al cargar bodegas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para cargar datos previos
    private void cargarDatosPrevios() {
        try {
            // Si tienes un controller, cargar datos del traspaso actual
            if (getController() != null && getController().getTraspasoActual() != null) {
                TraspasoDatos traspaso = getController().getTraspasoActual();

                // Cargar número de traspaso si existe
                if (traspaso.getNumeroTraspaso() != null && !traspaso.getNumeroTraspaso().isEmpty()) {
                    // Si tienes algún campo para mostrar el número actual, actualizarlo aquí
                }

                // Cargar otros datos del traspaso si es necesario
            } else {
            }

        } catch (Exception e) {
            System.err.println("WARNING Error al cargar datos previos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Método para mostrar errores de forma consistente

    private void editarTraspaso(String numeroTraspaso) {
        try {
            // Obtener datos completos del traspaso
            Map<String, Object> traspasoInfo = obtenerInformacionTraspaso(numeroTraspaso);
            List<Map<String, Object>> detalles = obtenerDetallesTraspaso(numeroTraspaso);

            if (traspasoInfo == null) {
                Toast.show(getPanel(), Toast.Type.ERROR, "No se encontró el traspaso");
                return;
            }

            // Verificar que el traspaso sea editable
            String estado = traspasoInfo.get("estado").toString();
            if (!isTraspasosEditable(estado)) {
                Toast.show(getPanel(), Toast.Type.WARNING,
                        "No se puede editar un traspaso en estado: " + estado);
                return;
            }

            // Crear formulario en modo edición
            creartraspaso editarForm = new creartraspaso();
            editarForm.settraspasos(this); // Establecer referencia al formulario padre
            editarForm.setModoEdicion(true); // Activar modo edición

            // Agregar listener para actualizar tabla cuando se edite el traspaso
            editarForm.addPropertyChangeListener("traspasoConfirmado", new java.beans.PropertyChangeListener() {
                @Override
                public void propertyChange(java.beans.PropertyChangeEvent evt) {
                    if (Boolean.TRUE.equals(evt.getNewValue())) {
                        // Actualizar la tabla inmediatamente
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                cargarTraspasos();
                            }
                        });
                    }
                }
            });

            // Crear opciones del modal
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {};

            // Mostrar modal
            ModalDialog.showModal(this,
                    new SimpleModalBorder(editarForm, "Editar Traspaso: " + numeroTraspaso, options,
                            new ModalCallback() {
                                @Override
                                public void action(ModalController mc, int i) {
                                    if (i == SimpleModalBorder.OPENED) {
                                        // Configurar el modal controller
                                        editarForm.setModalController(mc);

                                        // Inicializar el formulario
                                        editarForm.init();

                                        // PASO CRÍTICO: Cargar datos existentes en el formulario
                                        boolean datosEncontrados = editarForm.cargarTraspasoExistente(
                                                numeroTraspaso, traspasoInfo, detalles);

                                        if (datosEncontrados) {
                                            // Configurar específicamente para edición
                                            editarForm.configurarParaEdicion();

                                            // Crear y configurar paso1 con los datos cargados
                                            paso1 p1 = new paso1(editarForm.controller);
                                            p1.setSize(728, 640);
                                            p1.setLocation(0, 0);

                                            // IMPORTANTE: El paso1 ya debería cargar automáticamente los datos
                                            // desde el controller en su constructor
                                            editarForm.panelPasos.removeAll();
                                            editarForm.panelPasos.add(p1, BorderLayout.CENTER);
                                            editarForm.panelPasos.revalidate();
                                            editarForm.panelPasos.repaint();

                                            editarForm.paso = 1;
                                            editarForm.panelPaso1 = p1;

                                            // Actualizar botones para modo edición
                                            editarForm.configurarBotonesEdicion();
                                            Toast.show(getPanel(), Toast.Type.SUCCESS, "Traspaso cargado para edición");

                                        } else {
                                            System.err.println("ERROR Error cargando datos del traspaso");
                                            Toast.show(getPanel(), Toast.Type.ERROR,
                                                    "Error cargando datos del traspaso");
                                            mc.close();
                                        }
                                    } else if (i == SimpleModalBorder.CANCEL_OPTION) {
                                        // Verificar si hay cambios pendientes antes de cerrar
                                        if (editarForm.confirmarCierreActual()) {
                                            SwingUtilities.invokeLater(() -> cargarTraspasos());
                                        }
                                        // Si el usuario cancela la confirmación, no se cierra el modal
                                    }
                                }
                            }));

        } catch (SQLException e) {
            getLOGGER().log(Level.SEVERE, "Error al editar traspaso", e);
            Toast.show(getPanel(), Toast.Type.ERROR, "Error de base de datos: " + e.getMessage());
        } catch (Exception e) {
            getLOGGER().log(Level.SEVERE, "Error inesperado al editar traspaso", e);
            Toast.show(getPanel(), Toast.Type.ERROR, "Error inesperado: " + e.getMessage());
        }
    }

    private void verDetalleTraspaso(String numeroTraspaso) {
        try {
            Toast.show(getPanel(), Toast.Type.INFO, "Cargando detalle del traspaso...");

            // Obtener información completa del traspaso
            Map<String, Object> traspasoInfo = obtenerInformacionTraspaso(numeroTraspaso);
            List<Map<String, Object>> detalles = obtenerDetallesTraspaso(numeroTraspaso);

            if (traspasoInfo == null) {
                Toast.show(getPanel(), Toast.Type.ERROR, "No se encontró el traspaso");
                return;
            }

            // Crear formulario de visualización
            VerTraspasoForm verForm = new VerTraspasoForm();
            // verTraspas verForm = new verTraspas();

            // Configurar información del traspaso
            verForm.setTraspasoInfo(
                    traspasoInfo.get("numero_traspaso").toString(),
                    traspasoInfo.get("fecha_solicitud").toString(),
                    traspasoInfo.get("estado").toString(),
                    traspasoInfo.get("bodega_origen").toString(),
                    traspasoInfo.get("bodega_destino").toString(),
                    traspasoInfo.get("usuario_solicita").toString(),
                    traspasoInfo.get("motivo") != null ? traspasoInfo.get("motivo").toString() : "",
                    traspasoInfo.get("observaciones") != null ? traspasoInfo.get("observaciones").toString() : "");

            // Configurar fechas adicionales según el estado
            if (traspasoInfo.containsKey("fecha_autorizacion") && traspasoInfo.get("fecha_autorizacion") != null) {
                String usuarioAuth = traspasoInfo.get("usuario_autoriza") != null
                        ? traspasoInfo.get("usuario_autoriza").toString()
                        : null;
                verForm.setFechaAutorizacion(traspasoInfo.get("fecha_autorizacion").toString(), usuarioAuth);
            }

            if (traspasoInfo.containsKey("fecha_envio") && traspasoInfo.get("fecha_envio") != null) {
                verForm.setFechaEnvio(traspasoInfo.get("fecha_envio").toString());
            }

            if (traspasoInfo.containsKey("fecha_recepcion") && traspasoInfo.get("fecha_recepcion") != null) {
                String usuarioRec = traspasoInfo.get("usuario_recibe") != null
                        ? traspasoInfo.get("usuario_recibe").toString()
                        : null;
                verForm.setFechaRecepcion(traspasoInfo.get("fecha_recepcion").toString(), usuarioRec);
            }

            // Configurar productos del traspaso
            verForm.setProductos(detalles);

            // =================================================================================
            // LOGICA DE IMPRESIÓN Y EXPORTACIÓN
            // =================================================================================
            int idTraspaso = (int) traspasoInfo.get("id_traspaso");

            // BOTÓN IMPRIMIR (Tirilla 80mm)
            if (verForm.btnImprimir != null) {
                verForm.btnImprimir.addActionListener(e -> {
                    new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            raven.clases.productos.ImpresionTraspasoPOST printer = new raven.clases.productos.ImpresionTraspasoPOST();
                            return printer.imprimirTraspaso(idTraspaso);
                        }

                        @Override
                        protected void done() {
                            try {
                                if (get()) {
                                    Toast.show(getPanel(), Toast.Type.SUCCESS, "Impresión enviada correctamente");
                                } else {
                                    Toast.show(getPanel(), Toast.Type.WARNING, "No se pudo imprimir el comprobante");
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Toast.show(getPanel(), Toast.Type.ERROR, "Error al imprimir: " + ex.getMessage());
                            }
                        }
                    }.execute();
                });
            }

            // BOTÓN IMPRIMIR CUADRE (PDF)
            if (verForm.btnImprimirCuadre != null) {
                verForm.btnImprimirCuadre.addActionListener(e -> {
                    try {
                        // Construir objeto TraspasoDatos desde el Map
                        raven.clases.productos.TraspasoDatos td = new raven.clases.productos.TraspasoDatos();
                        td.setNumeroTraspaso(traspasoInfo.get("numero_traspaso").toString());
                        td.setNombreBodegaOrigen(traspasoInfo.get("bodega_origen").toString());
                        td.setNombreBodegaDestino(traspasoInfo.get("bodega_destino").toString());
                        td.setFechaSolicitud(java.time.LocalDateTime
                                .parse(traspasoInfo.get("fecha_solicitud").toString().replace(" ", "T")));
                        td.setEstado(traspasoInfo.get("estado").toString());

                        // Montos
                        if (traspasoInfo.get("monto_total") != null) {
                            td.setMontoTotal((java.math.BigDecimal) traspasoInfo.get("monto_total"));
                        }
                        if (traspasoInfo.get("monto_recibido") != null) {
                            td.setMontoRecibido((java.math.BigDecimal) traspasoInfo.get("monto_recibido"));
                        }

                        // Productos
                        for (Map<String, Object> det : detalles) {
                            raven.clases.productos.ProductoTraspasoItem item = new raven.clases.productos.ProductoTraspasoItem();
                            item.setNombreProducto(det.get("producto_nombre").toString());
                            item.setSku(det.get("sku") != null ? det.get("sku").toString() : "");
                            item.setCantidadSolicitada(((Number) det.get("cantidad_enviada")).intValue());
                            // Cantidad recibida needs fetching if available or map it
                            if (det.get("cantidad_recibida") != null) {
                                item.setCantidadRecibida(((Number) det.get("cantidad_recibida")).intValue());
                            }

                            if (det.get("precio_unitario") != null) {
                                item.setPrecioUnitario((java.math.BigDecimal) det.get("precio_unitario"));
                            }
                            if (det.get("subtotal") != null) {
                                item.setSubtotal((java.math.BigDecimal) det.get("subtotal"));
                            }

                            td.agregarProducto(item);
                        }

                        raven.clases.reportes.ReporteCuadreTraspasoPDF pdf = new raven.clases.reportes.ReporteCuadreTraspasoPDF();
                        pdf.generarPDF(td, traspasoInfo, verForm);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Toast.show(getPanel(), Toast.Type.ERROR, "Error generando reporte: " + ex.getMessage());
                    }
                });
            }

            // BOTÓN EXPORTAR (Excel)
            if (verForm.btnExportar != null) {
                verForm.btnExportar.addActionListener(e -> {
                    javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                    fileChooser.setDialogTitle("Guardar Traspaso");
                    fileChooser.setSelectedFile(new java.io.File("Traspaso_" + numeroTraspaso + ".xlsx"));

                    if (fileChooser.showSaveDialog(verForm) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        java.io.File file = fileChooser.getSelectedFile();
                        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                            file = new java.io.File(file.getParentFile(), file.getName() + ".xlsx");
                        }

                        final java.io.File finalFile = file;
                        new SwingWorker<Boolean, Void>() {
                            @Override
                            protected Boolean doInBackground() throws Exception {
                                // Construir DTO único
                                TraspasoReporteDTO dto = new TraspasoReporteDTO();
                                dto.setNumero(traspasoInfo.get("numero_traspaso").toString());
                                dto.setOrigen(traspasoInfo.get("bodega_origen").toString());
                                dto.setDestino(traspasoInfo.get("bodega_destino").toString());
                                dto.setFecha(traspasoInfo.get("fecha_solicitud").toString());
                                dto.setEstado(traspasoInfo.get("estado").toString());
                                dto.setTotalProductos(traspasoInfo.get("total_productos").toString());
                                dto.setDetalles(detalles); // Pasar detalles crudos (List<Map>)

                                ExcelTraspasosExporter exporter = new ExcelTraspasosExporter();
                                return exporter.exportar(java.util.Collections.singletonList(dto), finalFile,
                                        "Traspaso Individual");
                            }

                            @Override
                            protected void done() {
                                try {
                                    if (get()) {
                                        Toast.show(getPanel(), Toast.Type.SUCCESS, "Exportado correctamente");
                                    } else {
                                        Toast.show(getPanel(), Toast.Type.ERROR, "Error al exportar");
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Toast.show(getPanel(), Toast.Type.ERROR, "Error: " + ex.getMessage());
                                }
                            }
                        }.execute();
                    }
                });
            }

            // BOTÓN RECIBIR TRASPASO
            if (verForm.btnRecibir != null) {
                verForm.btnRecibir.addActionListener(e -> {
                    // Cerrar el diálogo de detalles para evitar superposición o confusión,
                    // o mantenerlo abierto y cerrarlo si la recepción es exitosa.
                    // Para ser consistentes con la UX, lo cerramos si es exitoso o dejamos que el
                    // modal de recepción maneje todo.

                    // Invocar helper de recepción
                    raven.clases.productos.RecepcionTraspaso.recibirTraspaso(
                            (java.awt.Window) javax.swing.SwingUtilities.getWindowAncestor(verForm),
                            numeroTraspaso,
                            new raven.clases.productos.RecepcionTraspaso.RecepcionCallback() {
                                @Override
                                public void onRecepcionExitosa(String numeroTraspaso,
                                        java.util.List<java.util.Map<String, Object>> productosRecibidos) {
                                    Toast.show(getPanel(), Toast.Type.SUCCESS, "Traspaso recibido exitosamente");
                                    cargarTraspasos(); // Recargar tabla

                                    // Cerrar el diálogo de "Ver Detalles" si está abierto
                                    java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(verForm);
                                    if (w != null)
                                        w.dispose();
                                }

                                @Override
                                public void onRecepcionCancelada() {
                                    // No action needed
                                }
                            });
                });
            }

            // Mostrar el formulario en un diálogo
            JDialog dialog = new JDialog();
            dialog.setTitle("Detalle Traspaso: " + numeroTraspaso);
            dialog.setModal(true);
            dialog.setSize(1000, 900); // Increased size
            dialog.setLocationRelativeTo(null);
            dialog.setContentPane(verForm);
            dialog.setVisible(true);

        } catch (SQLException e) {
            getLOGGER().log(Level.SEVERE, "Error al obtener detalle del traspaso", e);
            Toast.show(getPanel(), Toast.Type.ERROR, "Error al cargar detalle: " + e.getMessage());
        }
    }

    private void cancelarTraspaso(String numeroTraspaso) {
        // ═══════════════════════════════════════════════════════════════════════════
        // DIÁLOGO DE CANCELACIÓN MODERNO - FlatLaf
        // ═══════════════════════════════════════════════════════════════════════════

        javax.swing.JPanel panelConfirmacion = new javax.swing.JPanel();
        panelConfirmacion.setLayout(new java.awt.BorderLayout(15, 15));
        panelConfirmacion.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panelConfirmacion.putClientProperty(FlatClientProperties.STYLE, "arc:20;background:$Login.background");

        // Icono de advertencia
        FontIcon iconWarning = FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        iconWarning.setIconSize(48);
        iconWarning.setIconColor(new Color(255, 152, 0)); // Naranja advertencia
        javax.swing.JLabel lblIcono = new javax.swing.JLabel(iconWarning);
        lblIcono.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        // Texto de confirmación
        javax.swing.JLabel lblMensaje = new javax.swing.JLabel(
                "<html><div style='text-align:center;'>" +
                        "<b style='font-size:14px;'>¿Cancelar traspaso?</b><br><br>" +
                        "<span style='font-size:12px;color:#888;'>Número: " + numeroTraspaso + "</span><br>" +
                        "<span style='font-size:11px;color:#FF6B6B;'>ADVERTENCIA: Esta acción no se puede deshacer</span>"
                        +
                        "</div></html>");
        lblMensaje.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMensaje.putClientProperty(FlatClientProperties.STYLE, "font:+1");

        // Panel de botones
        javax.swing.JPanel panelBotones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 0));
        panelBotones.setOpaque(false);

        javax.swing.JButton btnSi = new javax.swing.JButton("Sí, Cancelar");
        btnSi.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:#DC3545"); // Rojo
        btnSi.setForeground(Color.WHITE);
        btnSi.setPreferredSize(new Dimension(130, 38));
        FontIcon iconX = FontIcon.of(FontAwesomeSolid.TIMES);
        iconX.setIconSize(14);
        iconX.setIconColor(Color.WHITE);
        btnSi.setIcon(iconX);

        javax.swing.JButton btnNo = new javax.swing.JButton("Volver");
        btnNo.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:lighten($Menu.background,15%)");
        btnNo.setPreferredSize(new Dimension(100, 38));

        panelBotones.add(btnSi);
        panelBotones.add(btnNo);

        panelConfirmacion.add(lblIcono, java.awt.BorderLayout.NORTH);
        panelConfirmacion.add(lblMensaje, java.awt.BorderLayout.CENTER);
        panelConfirmacion.add(panelBotones, java.awt.BorderLayout.SOUTH);
        panelConfirmacion.setPreferredSize(new Dimension(380, 220));

        ModalDialog.showModal(this,
                new SimpleModalBorder(panelConfirmacion, "Confirmar Cancelación",
                        new SimpleModalBorder.Option[] {},
                        new ModalCallback() {
                            @Override
                            public void action(ModalController mc, int i) {
                                btnSi.addActionListener(e -> {
                                    mc.close();
                                    ejecutarCancelacion(numeroTraspaso);
                                });
                                btnNo.addActionListener(e -> mc.close());
                            }
                        }));
    }

    /**
     * Cancela un traspaso en estado "en_transito" y devuelve los productos a la
     * bodega de origen
     */
    private void cancelarTraspasoEnTransito(String numeroTraspaso) {
        // ═══════════════════════════════════════════════════════════════════════════
        // DIÁLOGO DE CANCELACIÓN DE TRASPASO EN TRÁNSITO - FlatLaf
        // ═══════════════════════════════════════════════════════════════════════════

        javax.swing.JPanel panelConfirmacion = new javax.swing.JPanel();
        panelConfirmacion.setLayout(new java.awt.BorderLayout(15, 15));
        panelConfirmacion.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panelConfirmacion.putClientProperty(FlatClientProperties.STYLE, "arc:20;background:$Login.background");

        // Icono de advertencia
        FontIcon iconWarning = FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        iconWarning.setIconSize(48);
        iconWarning.setIconColor(new Color(255, 152, 0)); // Naranja advertencia
        javax.swing.JLabel lblIcono = new javax.swing.JLabel(iconWarning);
        lblIcono.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        // Texto de confirmación
        javax.swing.JLabel lblMensaje = new javax.swing.JLabel(
                "<html><div style='text-align:center;'>" +
                        "<b style='font-size:14px;'>¿Cancelar traspaso en tránsito?</b><br><br>" +
                        "<span style='font-size:12px;color:#888;'>Número: " + numeroTraspaso + "</span><br>" +
                        "<span style='font-size:11px;color:#FF6B6B;'>ADVERTENCIA: Esta acción devolverá los productos a la bodega de origen</span>"
                        +
                        "</div></html>");
        lblMensaje.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMensaje.putClientProperty(FlatClientProperties.STYLE, "font:+1");

        // Panel de botones
        javax.swing.JPanel panelBotones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 0));
        panelBotones.setOpaque(false);

        javax.swing.JButton btnSi = new javax.swing.JButton("Sí, Cancelar");
        btnSi.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:#DC3545"); // Rojo
        btnSi.setForeground(Color.WHITE);
        btnSi.setPreferredSize(new Dimension(130, 38));
        FontIcon iconX = FontIcon.of(FontAwesomeSolid.TIMES);
        iconX.setIconSize(14);
        iconX.setIconColor(Color.WHITE);
        btnSi.setIcon(iconX);

        javax.swing.JButton btnNo = new javax.swing.JButton("Volver");
        btnNo.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:lighten($Menu.background,15%)");
        btnNo.setPreferredSize(new Dimension(100, 38));

        panelBotones.add(btnSi);
        panelBotones.add(btnNo);

        panelConfirmacion.add(lblIcono, java.awt.BorderLayout.NORTH);
        panelConfirmacion.add(lblMensaje, java.awt.BorderLayout.CENTER);
        panelConfirmacion.add(panelBotones, java.awt.BorderLayout.SOUTH);
        panelConfirmacion.setPreferredSize(new Dimension(400, 240));

        ModalDialog.showModal(this,
                new SimpleModalBorder(panelConfirmacion, "Confirmar Cancelación de Traspaso en Tránsito",
                        new SimpleModalBorder.Option[] {},
                        new ModalCallback() {
                            @Override
                            public void action(ModalController mc, int i) {
                                btnSi.addActionListener(e -> {
                                    mc.close();
                                    ejecutarCancelacionTraspasoEnTransito(numeroTraspaso);
                                });
                                btnNo.addActionListener(e -> mc.close());
                            }
                        }));
    }

    /**
     * Ejecuta la cancelación del traspaso en la base de datos
     */
    private void ejecutarCancelacion(String numeroTraspaso) {
        try {
            String sql = "UPDATE traspasos SET estado = 'cancelado', "
                    + "observaciones = CONCAT(COALESCE(observaciones, ''), ' - Cancelado el: ', NOW()) "
                    + "WHERE numero_traspaso = ?";

            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, numeroTraspaso);

            int filasAfectadas = stmt.executeUpdate();

            stmt.close();
            conn.close();

            if (filasAfectadas > 0) {
                Toast.show(this, Toast.Type.SUCCESS, "Traspaso cancelado exitosamente");

                // NOTA: La notificación push a bodega ORIGEN se envía automáticamente
                // mediante el trigger BD (trg_traspasos_au_estado) que llama a
                // sp_notificar_traspaso_evento y envía la notificación solo a los
                // usuarios de la bodega origen.

                cargarTraspasos();
            } else {
                Toast.show(this, Toast.Type.WARNING, "No se pudo cancelar el traspaso");
            }

        } catch (SQLException e) {
            getLOGGER().log(Level.SEVERE, "Error al cancelar traspaso", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cancelar: " + e.getMessage());
        }
    }

    /**
     * Ejecuta la cancelación del traspaso en tránsito y devuelve los productos a la
     * bodega de origen
     */
    private void ejecutarCancelacionTraspasoEnTransito(String numeroTraspaso) {
        try {
            // Obtener el ID del usuario actual
            raven.clases.admin.UserSession session = raven.clases.admin.UserSession.getInstance();
            int idUsuario = session.getCurrentUser().getIdUsuario();

            // Crear instancia del servicio de cancelación
            TraspasoCancelService cancelService = new TraspasoCancelService();

            // Ejecutar la cancelación
            boolean exito = cancelService.cancelarTraspaso(numeroTraspaso, idUsuario);

            if (exito) {
                Toast.show(this, Toast.Type.SUCCESS,
                        "Traspaso cancelado y productos devueltos a bodega de origen exitosamente");
                cargarTraspasos(); // Recargar la tabla
            } else {
                Toast.show(this, Toast.Type.WARNING, "No se pudo cancelar el traspaso");
            }

        } catch (SQLException e) {
            getLOGGER().log(Level.SEVERE, "Error al cancelar traspaso en tránsito", e);
            Toast.show(this, Toast.Type.ERROR, "Error al cancelar traspaso: " + e.getMessage());
        }
    }

    private Map<String, Object> obtenerInformacionTraspaso(String numeroTraspaso) throws SQLException {
        String sql = "SELECT t.*, "
                + "bo.nombre as bodega_origen, bd.nombre as bodega_destino, "
                + "us.nombre as usuario_solicita, ua.nombre as usuario_autoriza, "
                + "ur.nombre as usuario_recibe "
                + "FROM traspasos t "
                + "INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega "
                + "INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega "
                + "INNER JOIN usuarios us ON t.id_usuario_solicita = us.id_usuario "
                + "LEFT JOIN usuarios ua ON t.id_usuario_autoriza = ua.id_usuario "
                + "LEFT JOIN usuarios ur ON t.id_usuario_recibe = ur.id_usuario "
                + "WHERE t.numero_traspaso = ?";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, numeroTraspaso);

        ResultSet rs = stmt.executeQuery();
        Map<String, Object> traspasoInfo = null;

        if (rs.next()) {
            traspasoInfo = new HashMap<>();
            traspasoInfo.put("id_traspaso", rs.getInt("id_traspaso"));
            traspasoInfo.put("numero_traspaso", rs.getString("numero_traspaso"));
            traspasoInfo.put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
            traspasoInfo.put("fecha_autorizacion", rs.getTimestamp("fecha_autorizacion"));
            traspasoInfo.put("fecha_envio", rs.getTimestamp("fecha_envio"));
            traspasoInfo.put("fecha_recepcion", rs.getTimestamp("fecha_recepcion"));
            traspasoInfo.put("estado", rs.getString("estado"));
            traspasoInfo.put("motivo", rs.getString("motivo"));
            traspasoInfo.put("observaciones", rs.getString("observaciones"));
            traspasoInfo.put("total_productos", rs.getInt("total_productos"));
            traspasoInfo.put("bodega_origen", rs.getString("bodega_origen"));
            traspasoInfo.put("bodega_destino", rs.getString("bodega_destino"));

            // AGREGADO: IDs necesarios para la edición
            traspasoInfo.put("id_bodega_origen", rs.getInt("id_bodega_origen"));
            traspasoInfo.put("id_bodega_destino", rs.getInt("id_bodega_destino"));
            traspasoInfo.put("id_usuario_solicita", rs.getInt("id_usuario_solicita"));

            traspasoInfo.put("usuario_solicita", rs.getString("usuario_solicita"));
            traspasoInfo.put("usuario_autoriza", rs.getString("usuario_autoriza"));
            traspasoInfo.put("usuario_recibe", rs.getString("usuario_recibe"));
        }

        rs.close();
        stmt.close();
        conn.close();

        return traspasoInfo;
    }

    /**
     * Obtiene los detalles de productos de un traspaso
     */
    private List<Map<String, Object>> obtenerDetallesTraspaso(String numeroTraspaso) throws SQLException {
        String sql = "SELECT td.*, td.id_variante AS id_variante, p.nombre AS producto_nombre, p.codigo_modelo, p.genero, "
                + "c.nombre AS color_nombre, t.numero AS talla_numero, "
                + "pv.sku, pv.ean "
                + "FROM traspaso_detalles td "
                + "INNER JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso "
                + "INNER JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE tr.numero_traspaso = ? "
                + "ORDER BY p.nombre, c.nombre, t.numero";

        Connection conn = conexion.getInstance().createConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, numeroTraspaso);

        ResultSet rs = stmt.executeQuery();
        List<Map<String, Object>> detalles = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> detalle = new HashMap<>();
            detalle.put("id_producto", rs.getInt("id_producto")); // Added for image loading
            detalle.put("id_detalle", rs.getInt("id_detalle_traspaso"));
            detalle.put("producto_nombre", rs.getString("producto_nombre"));
            detalle.put("codigo_modelo", rs.getString("codigo_modelo"));
            detalle.put("genero", rs.getString("genero"));
            detalle.put("id_variante", rs.getObject("id_variante") != null ? rs.getInt("id_variante") : 0);
            detalle.put("sku", rs.getString("sku"));
            detalle.put("ean", rs.getString("ean"));
            detalle.put("color_nombre", rs.getString("color_nombre"));
            detalle.put("talla_numero", rs.getString("talla_numero"));
            detalle.put("cantidad_solicitada", rs.getInt("cantidad_solicitada"));
            detalle.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
            detalle.put("cantidad_recibida", rs.getInt("cantidad_recibida"));
            detalle.put("estado_detalle", rs.getString("estado_detalle"));
            detalle.put("observaciones", rs.getString("observaciones"));

            // Added price fields
            detalle.put("precio_unitario", rs.getBigDecimal("precio_unitario"));
            detalle.put("subtotal", rs.getBigDecimal("subtotal"));

            // Construir descripción completa del producto
            // Construir descripción completa del producto
            StringBuilder descripcion = new StringBuilder();
            descripcion.append(rs.getString("producto_nombre"));

            // Construir texto de variante (solo color y talla)
            StringBuilder varianteTexto = new StringBuilder();
            boolean hasVariant = false;

            if (rs.getString("color_nombre") != null) {
                descripcion.append(" - ").append(rs.getString("color_nombre"));
                varianteTexto.append(rs.getString("color_nombre"));
                hasVariant = true;
            }
            if (rs.getString("talla_numero") != null) {
                descripcion.append(" - Talla ").append(rs.getString("talla_numero"));
                if (hasVariant)
                    varianteTexto.append(" - ");
                varianteTexto.append("Talla ").append(rs.getString("talla_numero"));
            }

            detalle.put("descripcion_completa", descripcion.toString());
            detalle.put("variante_texto", varianteTexto.toString()); // New field

            detalles.add(detalle);
        }

        rs.close();
        stmt.close();
        conn.close();

        return detalles;
    }

    /**
     * Actualizar el método autorizarTraspaso existente
     * Usa ModalDialog para mejor experiencia visual
     */
    private void autorizarTraspaso(String numeroTraspaso) {
        // ═══════════════════════════════════════════════════════════════════════════
        // DIÁLOGO DE CONFIRMACIÓN MODERNO - FlatLaf
        // ═══════════════════════════════════════════════════════════════════════════

        // Crear panel de confirmación personalizado
        javax.swing.JPanel panelConfirmacion = new javax.swing.JPanel();
        panelConfirmacion.setLayout(new java.awt.BorderLayout(15, 15));
        panelConfirmacion.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panelConfirmacion.putClientProperty(FlatClientProperties.STYLE, "arc:20;background:$Login.background");

        // Icono de pregunta
        FontIcon iconPregunta = FontIcon.of(FontAwesomeSolid.QUESTION_CIRCLE);
        iconPregunta.setIconSize(48);
        iconPregunta.setIconColor(new Color(33, 150, 243)); // Azul
        javax.swing.JLabel lblIcono = new javax.swing.JLabel(iconPregunta);
        lblIcono.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        // Texto de confirmación
        javax.swing.JLabel lblMensaje = new javax.swing.JLabel(
                "<html><div style='text-align:center;'>" +
                        "<b style='font-size:14px;'>¿Autorizar el traspaso?</b><br><br>" +
                        "<span style='font-size:12px;color:#888;'>Número: " + numeroTraspaso + "</span>" +
                        "</div></html>");
        lblMensaje.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMensaje.putClientProperty(FlatClientProperties.STYLE, "font:+1");

        // Panel de botones
        javax.swing.JPanel panelBotones = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 0));
        panelBotones.setOpaque(false);

        javax.swing.JButton btnSi = new javax.swing.JButton("Sí, Autorizar");
        btnSi.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:#28CD41");
        btnSi.setForeground(Color.WHITE);
        btnSi.setPreferredSize(new Dimension(130, 38));
        FontIcon iconCheck = FontIcon.of(FontAwesomeSolid.CHECK);
        iconCheck.setIconSize(14);
        iconCheck.setIconColor(Color.WHITE);
        btnSi.setIcon(iconCheck);

        javax.swing.JButton btnNo = new javax.swing.JButton("Cancelar");
        btnNo.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:lighten($Menu.background,15%)");
        btnNo.setPreferredSize(new Dimension(100, 38));

        panelBotones.add(btnSi);
        panelBotones.add(btnNo);

        // Agregar componentes al panel principal
        panelConfirmacion.add(lblIcono, java.awt.BorderLayout.NORTH);
        panelConfirmacion.add(lblMensaje, java.awt.BorderLayout.CENTER);
        panelConfirmacion.add(panelBotones, java.awt.BorderLayout.SOUTH);

        // Configurar tamaño del panel
        panelConfirmacion.setPreferredSize(new Dimension(350, 200));

        // Mostrar modal
        ModalDialog.showModal(this,
                new SimpleModalBorder(panelConfirmacion, "Confirmar Autorización",
                        new SimpleModalBorder.Option[] {},
                        new ModalCallback() {
                            @Override
                            public void action(ModalController mc, int i) {
                                // Configurar listeners de botones
                                btnSi.addActionListener(e -> {
                                    mc.close();
                                    ejecutarAutorizacion(numeroTraspaso);
                                });
                                btnNo.addActionListener(e -> mc.close());
                            }
                        }));
    }

    /**
     * Ejecuta la autorización del traspaso en la base de datos
     */
    private void ejecutarAutorizacion(String numeroTraspaso) {
        try {
            String sql = "UPDATE traspasos SET estado = 'autorizado', "
                    + "id_usuario_autoriza = ?, fecha_autorizacion = NOW() "
                    + "WHERE numero_traspaso = ? AND estado = 'pendiente'";

            Connection conn = conexion.getInstance().createConnection();

            // CORREGIDO: Obtener ID de usuario desde UserSession en lugar de
            // getIdUsuarioActual()
            UserSession session = UserSession.getInstance();
            int idUsuarioLogueado = session.getCurrentUser().getIdUsuario();

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idUsuarioLogueado);
            stmt.setString(2, numeroTraspaso);

            int filasAfectadas = stmt.executeUpdate();

            stmt.close();
            conn.close();

            if (filasAfectadas > 0) {
                Toast.show(this, Toast.Type.SUCCESS, "Traspaso autorizado exitosamente");

                // NOTA: La notificación push a bodega DESTINO se envía automáticamente
                // mediante el trigger BD (trg_traspasos_au_estado) que llama a
                // sp_notificar_traspaso_evento y envía la notificación solo a los
                // usuarios de la bodega destino, NO a la bodega origen que autoriza.

                cargarTraspasos();
            } else {
                Toast.show(this, Toast.Type.WARNING,
                        "No se pudo autorizar. Verifique que esté en estado pendiente.");
            }

        } catch (SQLException e) {
            getLOGGER().log(Level.SEVERE, "Error al autorizar traspaso", e);
            Toast.show(this, Toast.Type.ERROR, "Error al autorizar: " + e.getMessage());
        }
    }

    private int resolveUsuarioValidoParaFK(Connection con, int candidato) throws SQLException {
        if (candidato > 0 && existsUsuario(con, candidato))
            return candidato;
        int sid = SessionManager.getInstance().getCurrentUserId();
        if (sid > 0 && existsUsuario(con, sid))
            return sid;
        try (PreparedStatement ps = con
                .prepareStatement("SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario ASC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return rs.getInt(1);
        }
        throw new SQLException("Usuario autoriza inválido o inexistente");
    }

    private boolean existsUsuario(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con
                .prepareStatement("SELECT 1 FROM usuarios WHERE id_usuario = ? AND activo = 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Obtiene la bodega del usuario logueado; si no está en memoria se lee desde BD
     * para evitar validaciones fallidas cuando la sesión no tiene id_bodega
     * cargado.
     */
    private Integer obtenerBodegaUsuarioSegura() {
        try {
            // Lógica robusta similar a GestionProductosForm
            Integer idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
            if (idBodega == null || idBodega <= 0) {
                idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId();
            }

            if (idBodega != null && idBodega > 0) {
                return idBodega;
            }

            int userId = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();
            if (userId > 0) {
                try (Connection con = conexion.getInstance().createConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT id_bodega FROM usuarios WHERE id_usuario = ? AND activo = 1 LIMIT 1")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int dbBodega = rs.getInt(1);
                            if (dbBodega > 0) {
                                if (raven.controlador.admin.SessionManager.getInstance().getCurrentUser() != null) {
                                    raven.controlador.admin.SessionManager.getInstance().getCurrentUser()
                                            .setIdBodega(dbBodega);
                                }
                                return dbBodega;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo bodega del usuario: " + e.getMessage());
        }
        return null;
    }

    /**
     * Método para enviar traspaso usando la funcionalidad de EnvioTraspaso
     */
    private void enviarTraspaso(String numeroTraspaso) {
        try {
            // Obtener el Frame padre
            java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
            java.awt.Frame parentFrame = obtenerFramePadre(parentWindow);

            // Llamar al método estático de EnvioTraspaso
            EnvioTraspaso.enviarTraspaso(parentFrame, numeroTraspaso,
                    new EnvioTraspaso.EnvioCallback() {
                        @Override
                        public void onEnvioExitoso(String numeroTraspaso,
                                List<Map<String, Object>> productosEnviados) {
                            // Mostrar mensaje de éxito
                            Toast.show(getBtnEnviar().getParent(), Toast.Type.SUCCESS,
                                    "Traspaso enviado exitosamente.\n" +
                                            "Productos enviados: " + productosEnviados.size());

                            // Recargar la tabla de traspasos para reflejar el cambio de estado
                            cargarTraspasos(); // Asume que tienes este método para recargar la tabla

                            // Opcional: Seleccionar nuevamente la fila actualizada
                            seleccionarTraspasoEnTabla(numeroTraspaso);
                        }

                        @Override
                        public void onEnvioCancelado() {
                            Toast.show(getBtnEnviar().getParent(), Toast.Type.INFO,
                                    "Envío cancelado");
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR,
                    "Error al procesar el envío: " + e.getMessage());
        }

    }

    /**
     * Selecciona el traspaso en la tabla por número.
     */
    public void seleccionarTraspasoEnTabla(String numeroTraspaso) {
        try {
            for (int i = 0; i < getTablaTraspasos().getRowCount(); i++) {
                String numeroEnTabla = (String) getTablaTraspasos().getValueAt(i, 0);
                if (numeroTraspaso.equals(numeroEnTabla)) {
                    getTablaTraspasos().setRowSelectionInterval(i, i);
                    getTablaTraspasos().scrollRectToVisible(getTablaTraspasos().getCellRect(i, 0, true));
                    break;
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Resalta el traspaso por id_traspaso obtenido desde la base de datos.
     * Si la tabla aún no está cargada, agenda la selección en el EDT.
     */
    public void resaltarTraspasoPorId(int idTraspaso) {
        String numero = null;
        try {
            String sql = "SELECT numero_traspaso FROM traspasos WHERE id_traspaso = ?";
            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idTraspaso);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                numero = rs.getString(1);
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception e) {
        }

        if (numero != null && !numero.isEmpty()) {
            final String numeroFinal = numero;
            SwingUtilities.invokeLater(() -> {
                // Asegurar que los datos estén cargados
                if (getTablaTraspasos() != null && getTablaTraspasos().getRowCount() == 0) {
                    try {
                        cargarTraspasos();
                    } catch (Exception ignore) {
                    }
                }
                seleccionarTraspasoEnTabla(numeroFinal);
            });
        }
    }

    private java.awt.Frame obtenerFramePadre(java.awt.Window parentWindow) {
        java.awt.Frame parentFrame = null;

        if (parentWindow instanceof java.awt.Frame) {
            parentFrame = (java.awt.Frame) parentWindow;
        } else if (parentWindow instanceof java.awt.Dialog) {
            // Si está en un Dialog, buscar el Frame owner
            java.awt.Dialog dialog = (java.awt.Dialog) parentWindow;
            java.awt.Window owner = dialog.getOwner();
            if (owner instanceof java.awt.Frame) {
                parentFrame = (java.awt.Frame) owner;
            }
        }

        if (parentFrame == null) {
        }

        return parentFrame;
    }

    /**
     * Actualizar el método recibirTraspaso existente
     */
    // IMPLEMENTACIÓN ALTERNATIVA SI NO TIENES TraspasoService
    // Usar solo si no existe la clase TraspasoService
    private void cargarBodegasDirectamenteDB() {
        try {
            List<Bodega> bodegas = obtenerBodegasDesdeDB();

            if (bodegas != null && !bodegas.isEmpty()) {
                // Usar raw types para evitar problemas de casting
                @SuppressWarnings({ "unchecked", "rawtypes" })
                JComboBox combo1 = getjComboBox1(); // Bodega origen

                @SuppressWarnings({ "unchecked", "rawtypes" })
                JComboBox combo3 = getjComboBox3(); // Bodega destino

                // Limpiar combos
                combo1.removeAllItems();
                combo3.removeAllItems();

                // Agregar opción por defecto
                combo1.addItem("Todas las bodegas");
                combo3.addItem("Todas las bodegas");

                // Agregar bodegas
                for (Bodega bodega : bodegas) {
                    combo1.addItem(bodega);
                    combo3.addItem(bodega);
                }

                // Configurar renderers
                configurarRendererBodegas();
            } else {
                mostrarError("No se encontraron bodegas en la base de datos");
            }

        } catch (SQLException e) {
            mostrarError("Error al cargar bodegas desde DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Bodega> obtenerBodegasDesdeDB() throws SQLException {
        List<Bodega> bodegas = new ArrayList<>();

        String sql = "SELECT id_bodega, codigo, nombre, tipo, activa "
                + "FROM bodegas WHERE activa = 1 ORDER BY nombre";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Bodega bodega = new Bodega();
                bodega.setIdBodega(rs.getInt("id_bodega"));
                bodega.setCodigo(rs.getString("codigo"));
                bodega.setNombre(rs.getString("nombre"));
                bodega.setTipo(rs.getString("tipo"));
                bodega.setActiva(rs.getBoolean("activa"));

                bodegas.add(bodega);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }

        return bodegas;
    }

    // INICIALIZACIÓN COMPLETA EN EL CONSTRUCTOR
    // Agregar esto al final del constructor de traspasos después de
    // initComponents()
    private void inicializarFormulario() {
        try {
            // Inicializar servicios si no están inicializados
            if (getTraspasoService() == null) {
                setTraspasoService(new TraspasoService());
            }

            // Configurar tabla
            configurarTabla();

            // Cargar datos iniciales
            cargarDatos();
        } catch (Exception e) {
            mostrarError("Error al inicializar formulario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método actualizado para validar datos
    @SuppressWarnings("rawtypes")
    public boolean validarDatos() {
        StringBuilder errores = new StringBuilder();

        // Validar número de traspaso
        String numeroTraspaso = getjTextField2().getText().trim();
        if (numeroTraspaso.isEmpty()) {
            errores.append("- El número de traspaso es obligatorio\n");
        }

        // Validar bodega origen
        Bodega bodegaOrigen = obtenerBodegaSeleccionada(getjComboBox1());
        if (bodegaOrigen == null) {
            errores.append("- Debe seleccionar una bodega de origen\n");
        }

        // Validar bodega destino
        Bodega bodegaDestino = obtenerBodegaSeleccionada(getjComboBox3());
        if (bodegaDestino == null) {
            errores.append("- Debe seleccionar una bodega de destino\n");
        }

        // Validar que origen y destino sean diferentes
        if (bodegaOrigen != null && bodegaDestino != null) {
            // Comparar usando el método equals de Bodega o sus IDs
            if (bodegaOrigen.equals(bodegaDestino)
                    || (bodegaOrigen.getIdBodega() != null
                            && bodegaOrigen.getIdBodega().equals(bodegaDestino.getIdBodega()))) {
                errores.append("- La bodega de origen y destino deben ser diferentes\n");
            }
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

    // Método actualizado para obtener bodega seleccionada
    @SuppressWarnings("rawtypes")
    private Bodega obtenerBodegaSeleccionada(JComboBox combo) {
        if (combo.getSelectedIndex() > 0) {
            Object item = combo.getSelectedItem();
            if (item instanceof Bodega) {
                return (Bodega) item;
            }
        }
        return null;
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void forzarActualizacionTraspasos() {
        setCargandoCombos(false); // Asegurar que no esté bloqueado

        SwingUtilities.invokeLater(() -> {
            try {
                cargarTraspasos();
            } catch (Exception e) {
                System.err.println("ERROR Error en actualización forzada: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void cargarTraspasos() {
        // CORRECCIÓN: Verificar si los componentes están inicializados
        if (getTablaTraspasos() == null) {
            SwingUtilities.invokeLater(() -> cargarTraspasos());
            return;
        }

        if (isCargandoCombos()) {
            return; // No cargar durante inicialización de combos
        }
        // Capturar filtros en el EDT (Thread-safe)
        // Usamos variables finales o efectivamente finales para la lambda
        final String estadoFiltro = (getjComboBox2() != null) ? (String) getjComboBox2().getSelectedItem() : null;
        final Object bodegaOrigenFiltro = (getjComboBox1() != null) ? getjComboBox1().getSelectedItem() : null;
        final Object bodegaDestinoFiltro = (getjComboBox3() != null) ? getjComboBox3().getSelectedItem() : null;
        final String fechaFiltro = (getjTextField2() != null) ? getjTextField2().getText().trim() : null;

        // DEBUG: Mostrar fecha que se está usando para filtrar

        // Delegar al gestor inteligente
        // Se pasa un Supplier que se ejecutará en segundo plano
        if (refreshManager != null) {
            refreshManager.reload(() -> {
                return obtenerTraspasos(estadoFiltro, bodegaOrigenFiltro, bodegaDestinoFiltro, fechaFiltro);
            }, false);
        } else {
            System.err.println("WARNING RefreshManager no inicializado");
        }
    }

    private List<Object[]> obtenerTraspasos(String estado, Object bodegaOrigen, Object bodegaDestino, String fecha) {
        List<Object[]> traspasos = new ArrayList<>();

        try {
            // ═══════════════════════════════════════════════════════════════════════════
            // OBTENER BODEGA DEL USUARIO ACTUAL
            // ═══════════════════════════════════════════════════════════════════════════
            Integer idBodegaUsuario = null;
            try {
                idBodegaUsuario = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario();
                if (idBodegaUsuario == null || idBodegaUsuario <= 0) {
                    idBodegaUsuario = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId();
                }
            } catch (Exception e) {
                System.err.println("WARNING Error obteniendo bodega del usuario: " + e.getMessage());
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT t.numero_traspaso, bo.nombre as origen, bd.nombre as destino, "
                            + "t.fecha_solicitud, t.estado, "
                            + "(SELECT COUNT(*) FROM traspaso_detalles td WHERE td.id_traspaso = t.id_traspaso) as productos, "
                            + "t.id_traspaso, "
                            + "EXISTS( "
                            + "  SELECT 1 "
                            + "  FROM traspaso_detalles td2 "
                            + "  LEFT JOIN ( "
                            + "     SELECT id_detalle_traspaso, id_variante_caja, SUM(cajas_convertidas) AS convertidas "
                            + "     FROM conversion_caja_traspaso "
                            + "     GROUP BY id_detalle_traspaso, id_variante_caja "
                            + "  ) cv ON cv.id_detalle_traspaso = td2.id_detalle_traspaso AND cv.id_variante_caja = td2.id_variante "
                            + "  WHERE td2.id_traspaso = t.id_traspaso "
                            + "    AND LOWER(td2.tipo) = 'caja' "
                            + "    AND (COALESCE(td2.cantidad_recibida,0) - COALESCE(cv.convertidas,0)) > 0 "
                            + "    AND EXISTS (SELECT 1 FROM inventario_bodega ib WHERE ib.id_bodega = t.id_bodega_destino AND ib.id_variante = td2.id_variante AND ib.activo = 1 AND COALESCE(ib.Stock_caja,0) > 0) "
                            + ") AS has_caja_convertible "
                            + "FROM traspasos t "
                            + "INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega "
                            + "INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega "
                            + "WHERE 1=1");

            List<Object> parametros = new ArrayList<>();

            // ═══════════════════════════════════════════════════════════════════════════
            // FILTRO AUTOMÁTICO POR BODEGA DEL USUARIO Y DIRECCIÓN
            // ═══════════════════════════════════════════════════════════════════════════
            if (idBodegaUsuario != null && idBodegaUsuario > 0) {
                if ("ENVIADO".equals(filtroDireccion)) {
                    // ENVIADO: Origen = Usuario
                    // Mostrar traspasos donde yo soy el origen
                    sql.append(" AND t.id_bodega_origen = ?");
                    parametros.add(idBodegaUsuario);
                    // Si hay un filtro manual de Destino, aplicarlo
                    if (bodegaDestino instanceof Bodega) {
                        sql.append(" AND t.id_bodega_destino = ?");
                        parametros.add(((Bodega) bodegaDestino).getIdBodega());
                    }
                    // Ignoramos filtro manual de Origen porque ya está fijo

                } else {
                    // RECIBIDO (o defecto): Destino = Usuario
                    // Mostrar traspasos que vienen hacia mí
                    sql.append(" AND t.id_bodega_destino = ?");
                    parametros.add(idBodegaUsuario);
                    // Si hay un filtro manual de Origen, aplicarlo
                    if (bodegaOrigen instanceof Bodega) {
                        sql.append(" AND t.id_bodega_origen = ?");
                        parametros.add(((Bodega) bodegaOrigen).getIdBodega());
                    }
                    // Ignoramos filtro manual de Destino porque ya está fijo
                }
            } else {
                // Si no hay usuario bodega, aplicamos filtros manuales normales
                if (bodegaOrigen instanceof Bodega) {
                    sql.append(" AND t.id_bodega_origen = ?");
                    parametros.add(((Bodega) bodegaOrigen).getIdBodega());
                }
                if (bodegaDestino instanceof Bodega) {
                    sql.append(" AND t.id_bodega_destino = ?");
                    parametros.add(((Bodega) bodegaDestino).getIdBodega());
                }
            }

            // Aplicar filtro de estado
            if (estado != null && !estado.equals("Seleccionar")) {
                sql.append(" AND t.estado = ?");
                parametros.add(estado);
            }

            // NOTA: Los filtros de bodega manuales ya se manejaron arriba dentro del bloque
            // de dirección
            // para evitar duplicidad o conflictos.
            // Solo aplicamos búsquedas por texto si son Strings (búsqueda abierta)
            if (bodegaOrigen instanceof String && !"Todas las bodegas".equals(bodegaOrigen)) {
                sql.append(" AND bo.nombre LIKE ?");
                parametros.add("%" + bodegaOrigen + "%");
            }
            if (bodegaDestino instanceof String && !"Todas las bodegas".equals(bodegaDestino)) {
                sql.append(" AND bd.nombre LIKE ?");
                parametros.add("%" + bodegaDestino + "%");
            }

            // Aplicar filtro de fecha (Rango: Desde fecha seleccionada hasta hoy)
            if (fecha != null && !fecha.trim().isEmpty() && !"fecha".equals(fecha)) {
                try {
                    // Validar formato de fecha (dd/MM/yyyy)
                    java.time.format.DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter
                            .ofPattern("dd/MM/yyyy");
                    java.time.LocalDate date = java.time.LocalDate.parse(fecha, inputFormatter);

                    // Convertir a formato SQL estándar (YYYY-MM-DD) para log/debug
                    String sqlDate = date.toString();
                    // Filtra desde la fecha indicada hasta la fecha actual
                    // Usamos parámetros seguros en lugar de concatenación
                    sql.append(" AND DATE(t.fecha_solicitud) >= ?");
                    sql.append(" AND DATE(t.fecha_solicitud) <= CURDATE()");

                    // Pasar java.sql.Date al PreparedStatement es más seguro y estándar
                    parametros.add(java.sql.Date.valueOf(date));
                } catch (java.time.format.DateTimeParseException e) {
                    System.err.println("WARNING Formato de fecha inválido para filtro: " + fecha);
                    // No aplicar filtro si la fecha es inválida
                }
            }

            sql.append(" ORDER BY t.fecha_solicitud DESC LIMIT 100");

            // Ejecutar consulta
            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                traspasos.add(new Object[] {
                        rs.getString("numero_traspaso"),
                        rs.getString("origen"),
                        rs.getString("destino"),
                        rs.getTimestamp("fecha_solicitud"),
                        rs.getString("estado"),
                        rs.getInt("productos"),
                        "Ver/Editar", // Acciones
                        rs.getBoolean("has_caja_convertible")
                });
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error en consulta de traspasos: " + e.getMessage());

            // Agregar datos de ejemplo en caso de error
            // agregarDatosEjemplo(traspasos);
        }

        return traspasos;
    }

    private boolean cargandoCombos = false;

    private boolean verificarPermisosCrearTraspaso() {
        // TODO: Implementar verificación real de permisos
        // Por ahora, siempre devolver true
        return true;
    }

    private void manejarAccionTabla(int fila, String accion) {
        if (fila < 0) {
            return;
        }

        String numeroTraspaso = (String) getTablaTraspasos().getValueAt(fila, 0);
        String estado = (String) getTablaTraspasos().getValueAt(fila, 4);

        switch (accion.toLowerCase()) {
            case "ver":
                verDetalleTraspaso(numeroTraspaso);
                break;
            case "editar":
                editarTraspaso(numeroTraspaso);
                break;
            case "autorizar":
                autorizarTraspaso(numeroTraspaso);
                break;
            case "enviar":
                enviarTraspaso(numeroTraspaso);
                break;
            case "recibir":
                recibirTraspaso(numeroTraspaso);
                break;
        }
    }

    /**
     * Actualiza la visibilidad y texto de los botones según el estado seleccionado
     * MODIFICADO: Integra validación de permisos y bodega
     */
    private void actualizarBotonesSegunEstado() {
        int filaSeleccionada = getTablaTraspasos().getSelectedRow();

        if (filaSeleccionada < 0) {
            // Ninguna fila seleccionada - mostrar botones por defecto
            getBtnAutori().setText("Autorizar");
            getBtnEnviar().setText("Enviar/Recibir");
            getBtnAutori().setEnabled(false);
            getBtnEnviar().setEnabled(false);
            return;
        }

        String numeroTraspaso = (String) getTablaTraspasos().getValueAt(filaSeleccionada, 0);
        String estado = (String) getTablaTraspasos().getValueAt(filaSeleccionada, 4);

        // Obtener bodegas del traspaso para validación
        Integer bodegaOrigen = obtenerBodegaOrigenDeTraspaso(numeroTraspaso);
        Integer bodegaDestino = obtenerBodegaDestinoDeTraspaso(numeroTraspaso);

        // Crear validador de permisos
        TraspasoPermissionValidator validator = new TraspasoPermissionValidator();

        switch (estado.toLowerCase()) {
            case "pendiente":
                getBtnAutori().setText("Autorizar");
                getBtnEnviar().setText("Enviar/Recibir");
                // Solo habilitar si tiene permiso Y es de la bodega origen
                getBtnAutori().setEnabled(
                        bodegaOrigen != null && validator.canAuthorize(estado, bodegaOrigen));
                getBtnEnviar().setEnabled(false);
                break;

            case "autorizado":
                getBtnAutori().setText("Autorizado");
                getBtnEnviar().setText("Enviar");
                getBtnAutori().setEnabled(false);
                // Solo habilitar si tiene permiso Y es de la bodega origen
                getBtnEnviar().setEnabled(
                        bodegaOrigen != null && validator.canSend(estado, bodegaOrigen));
                break;

            case "en_transito":
                getBtnAutori().setText("Autorizado");
                getBtnEnviar().setText("Recibir");
                getBtnAutori().setEnabled(false);
                // Solo habilitar si tiene permiso Y es de la bodega destino
                getBtnEnviar().setEnabled(
                        bodegaDestino != null && validator.canReceive(estado, bodegaDestino));
                break;

            case "recibido":
                getBtnAutori().setText("Completado");
                getBtnEnviar().setText("Recibido");
                getBtnAutori().setEnabled(false);
                getBtnEnviar().setEnabled(false);
                break;

            case "cancelado":
                getBtnAutori().setText("Cancelado");
                getBtnEnviar().setText("Cancelado");
                getBtnAutori().setEnabled(false);
                getBtnEnviar().setEnabled(false);
                break;

            default:
                getBtnAutori().setText("Autorizar");
                getBtnEnviar().setText("Enviar/Recibir");
                getBtnAutori().setEnabled(false);
                getBtnEnviar().setEnabled(false);
                break;
        }
    }

    /**
     * Obtiene el ID de la bodega origen de un traspaso desde la base de datos
     */
    private Integer obtenerBodegaOrigenDeTraspaso(String numeroTraspaso) {
        try (java.sql.Connection conn = conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_bodega_origen FROM traspasos WHERE numero_traspaso = ?")) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_bodega_origen");
                }
            }
        } catch (Exception e) {
            getLOGGER().log(java.util.logging.Level.WARNING,
                    "Error obteniendo bodega origen: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene el ID de la bodega destino de un traspaso desde la base de datos
     */
    private Integer obtenerBodegaDestinoDeTraspaso(String numeroTraspaso) {
        try (java.sql.Connection conn = conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_bodega_destino FROM traspasos WHERE numero_traspaso = ?")) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_bodega_destino");
                }
            }
        } catch (Exception e) {
            getLOGGER().log(java.util.logging.Level.WARNING,
                    "Error obteniendo bodega destino: " + e.getMessage());
        }
        return null;
    }

    /**
     * Actualiza el estado visual de los botones de filtro (Enviado/Recibido)
     */
    private void actualizarEstiloBotonesFiltro() {
        if (btnRecibido == null || btnEnviado == null)
            return;

        // Estilos para estado Activo e Inactivo
        // Activo: Fondo azul brillante, texto blanco, sin borde, fuente grande
        String styleActive = "background:#007AFF; foreground:#FFFFFF; font:bold +2; arc:15; border:0,0,0,0";
        // Inactivo: Fondo gris claro, texto oscuro, borde suave
        String styleInactive = "background:#F2F2F7; foreground:#8E8E93; font:bold; arc:15; border:1,1,1,1,#D1D1D6";

        if ("RECIBIDO".equals(filtroDireccion)) {
            // Recibido activo
            btnRecibido.putClientProperty(FlatClientProperties.STYLE, styleActive);
            btnEnviado.putClientProperty(FlatClientProperties.STYLE, styleInactive);

            // Asegurar que ambos estén habilitados para permitir el cambio
            btnRecibido.setEnabled(true);
            btnEnviado.setEnabled(true);

            // Texto claro
            btnRecibido.setText("Recibidos");
            btnEnviado.setText("Enviados");
        } else {
            // Enviado activo
            btnEnviado.putClientProperty(FlatClientProperties.STYLE, styleActive);
            btnRecibido.putClientProperty(FlatClientProperties.STYLE, styleInactive);

            btnRecibido.setEnabled(true);
            btnEnviado.setEnabled(true);

            btnRecibido.setText("Recibidos");
            btnEnviado.setText("Enviados");
        }
    }

    private void recibirTraspaso(String numeroTraspaso) {
        // Obtener el contenedor padre de forma segura
        java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
        java.awt.Frame parentFrame = obtenerFramePadre(parentWindow);

        // Mostrar modal actualizarBotonesSegunEstadode recepción usando
        // RecepcionTraspaso
        try {
            RecepcionTraspaso.recibirTraspaso(parentFrame, numeroTraspaso,
                    new RecepcionTraspaso.RecepcionCallback() {
                        @Override
                        public void onRecepcionExitosa(String numeroTraspaso,
                                List<Map<String, Object>> productosRecibidos) {

                            // Mostrar mensaje de éxito
                            Toast.show(traspasos.this, Toast.Type.SUCCESS,
                                    "Traspaso " + numeroTraspaso + " recibido exitosamente.\n"
                                            + "Productos recibidos: " + productosRecibidos.size());

                            // ACTUALIZAR TABLA AUTOMÁTICAMENTE
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    cargarTraspasos();
                                } catch (Exception e) {
                                    System.err.println("ERROR Error actualizando tabla: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });

                            // TODO: Opcional - Actualizar inventarios si es necesario
                            // actualizarInventariosTraspaso(numeroTraspaso, productosRecibidos);
                        }

                        @Override
                        public void onRecepcionCancelada() {
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            getLOGGER().log(Level.SEVERE, "Error al abrir modal de recepción", e);
            Toast.show(this, Toast.Type.ERROR,
                    "Error al abrir modal de recepción: " + e.getMessage());
        }
    }

    public void insertarDatosPruebaBodegas() {
        String sql = "INSERT IGNORE INTO bodegas (codigo, nombre, tipo, activa) VALUES "
                + "('BOD001', 'Bodega Principal', 'principal', 1), "
                + "('SUC001', 'Sucursal Centro', 'sucursal', 1), "
                + "('SUC002', 'Sucursal Norte', 'sucursal', 1), "
                + "('DEP001', 'Depósito Temporal', 'deposito', 1)";

        try {
            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            int filasAfectadas = stmt.executeUpdate();

            stmt.close();
            conn.close();

            if (filasAfectadas > 0) {
                // Recargar bodegas después de insertar
                SwingUtilities.invokeLater(() -> {
                    cargarBodegasEnFiltros();
                });
            } else {
            }

        } catch (SQLException e) {
            System.err.println("Error insertando datos de prueba: " + e.getMessage());
        }
    }

    public void insertarTraspasosDePrueba() {
        // Primero insertar bodegas si no existen
        insertarDatosPruebaBodegas();

        String sql = "INSERT IGNORE INTO traspasos (numero_traspaso, id_bodega_origen, id_bodega_destino, "
                + "id_usuario_solicita, fecha_solicitud, estado, motivo, total_productos) "
                + "SELECT ?, bo.id_bodega, bd.id_bodega, 1, NOW(), ?, ?, 0 "
                + "FROM bodegas bo, bodegas bd "
                + "WHERE bo.codigo = ? AND bd.codigo = ? "
                + "AND NOT EXISTS (SELECT 1 FROM traspasos WHERE numero_traspaso = ?)";

        try {
            Connection conn = conexion.getInstance().createConnection();

            // Traspaso 1
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "TR000001");
            stmt.setString(2, "pendiente");
            stmt.setString(3, "Reposición de stock urgente");
            stmt.setString(4, "BOD001");
            stmt.setString(5, "SUC001");
            stmt.setString(6, "TR000001");
            stmt.executeUpdate();

            // Traspaso 2
            stmt.setString(1, "TR000002");
            stmt.setString(2, "autorizado");
            stmt.setString(3, "Reorganización de inventario");
            stmt.setString(4, "SUC002");
            stmt.setString(5, "BOD001");
            stmt.setString(6, "TR000002");
            stmt.executeUpdate();

            stmt.close();
            conn.close();
            // Recargar traspasos
            SwingUtilities.invokeLater(() -> {
                cargarTraspasos();
            });

        } catch (SQLException e) {
            System.err.println("Error insertando traspasos de prueba: " + e.getMessage());
        }
    }

    /**
     * Método para actualizar inventarios después de recepción (opcional)
     * Implementar según las reglas de negocio de tu sistema
     */
    private void actualizarInventariosTraspaso(String numeroTraspaso,
            List<Map<String, Object>> productosRecibidos) {
        try {
            Connection conn = conexion.getInstance().createConnection();
            conn.setAutoCommit(false);

            // Obtener información del traspaso
            String sqlTraspaso = "SELECT id_bodega_origen, id_bodega_destino FROM traspasos " +
                    "WHERE numero_traspaso = ?";

            PreparedStatement stmtTraspaso = conn.prepareStatement(sqlTraspaso);
            stmtTraspaso.setString(1, numeroTraspaso);
            ResultSet rsTraspaso = stmtTraspaso.executeQuery();

            if (rsTraspaso.next()) {
                int bodegaOrigen = rsTraspaso.getInt("id_bodega_origen");
                int bodegaDestino = rsTraspaso.getInt("id_bodega_destino");

                // Actualizar inventario para cada producto recibido
                String sqlInventario = "INSERT INTO inventario_bodega " +
                        "(id_bodega, id_producto, id_variante, stock_actual) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE stock_actual = stock_actual + VALUES(stock_actual)";

                PreparedStatement stmtInventario = conn.prepareStatement(sqlInventario);

                for (Map<String, Object> producto : productosRecibidos) {
                    int cantidadRecibida = (Integer) producto.get("cantidad_recibida_actual");
                    int idProducto = (Integer) producto.get("id_producto");
                    Object idVariante = producto.get("id_variante");

                    // Agregar al inventario de bodega destino
                    stmtInventario.setInt(1, bodegaDestino);
                    stmtInventario.setInt(2, idProducto);
                    if (idVariante != null) {
                        stmtInventario.setInt(3, (Integer) idVariante);
                    } else {
                        stmtInventario.setNull(3, java.sql.Types.INTEGER);
                    }
                    stmtInventario.setInt(4, cantidadRecibida);
                    stmtInventario.addBatch();
                }

                stmtInventario.executeBatch();
                stmtInventario.close();

                // Registrar movimientos de inventario
                String sqlMovimiento = "INSERT INTO inventario_movimientos " +
                        "(id_producto, id_variante, tipo_movimiento, cantidad, fecha_movimiento, " +
                        "id_referencia, tipo_referencia, id_usuario, observaciones) " +
                        "VALUES (?, ?, 'entrada par', ?, NOW(), ?, 'traspaso', ?, ?)";

                PreparedStatement stmtMovimiento = conn.prepareStatement(sqlMovimiento);

                for (Map<String, Object> producto : productosRecibidos) {
                    int cantidadRecibida = (Integer) producto.get("cantidad_recibida_actual");
                    int idProducto = (Integer) producto.get("id_producto");
                    Object idVariante = producto.get("id_variante");

                    stmtMovimiento.setInt(1, idProducto);
                    if (idVariante != null) {
                        stmtMovimiento.setInt(2, (Integer) idVariante);
                    } else {
                        stmtMovimiento.setNull(2, java.sql.Types.INTEGER);
                    }
                    stmtMovimiento.setInt(3, cantidadRecibida);
                    stmtMovimiento.setInt(4, bodegaDestino); // ID referencia = bodega destino
                    stmtMovimiento.setInt(5, 1); // Usuario actual
                    stmtMovimiento.setString(6, "Recepcion traspaso: " + numeroTraspaso);
                    stmtMovimiento.addBatch();
                }

                stmtMovimiento.executeBatch();
                stmtMovimiento.close();
            }

            rsTraspaso.close();
            stmtTraspaso.close();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            System.err.println("Error actualizando inventarios: " + e.getMessage());
            e.printStackTrace();

            // Mostrar notificación de error pero no bloquear la operación principal
            SwingUtilities.invokeLater(() -> {
                Toast.show(traspasos.this, Toast.Type.WARNING,
                        "Traspaso recibido, pero hubo un problema actualizando inventarios");
            });
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        panelAcciones = new javax.swing.JPanel();
        txtBuscarN = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        btnAutori = new javax.swing.JButton();
        btnEnviar = new javax.swing.JButton();
        btnExport = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaTraspasos = new javax.swing.JTable();
        panelFiltros = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        cbxOrigen = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        cbxdestino = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        TfdFecha = new javax.swing.JFormattedTextField();
        BtnAjustes = new javax.swing.JButton();
        CheAjuste = new javax.swing.JCheckBox();
        panelhead2 = new javax.swing.JPanel();
        btnEnviado = new javax.swing.JButton();
        btnRecibido = new javax.swing.JButton();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("TRASPASOS");

        panelAcciones.setBackground(new java.awt.Color(204, 255, 204));

        btnNuevo.setText("Nuevo traspaso");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });

        btnAutori.setText("Autorizar");
        btnAutori.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAutoriActionPerformed(evt);
            }
        });

        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        btnExport.setText("Reporte");
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generarReporteDiario();
            }
        });

        javax.swing.GroupLayout panelAccionesLayout = new javax.swing.GroupLayout(panelAcciones);
        panelAcciones.setLayout(panelAccionesLayout);
        panelAccionesLayout.setHorizontalGroup(
                panelAccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAccionesLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(txtBuscarN)
                                .addGap(40, 40, 40)
                                .addComponent(btnNuevo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAutori, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnEnviar, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnExport, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(40, 40, 40)));
        panelAccionesLayout.setVerticalGroup(
                panelAccionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAccionesLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(panelAccionesLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtBuscarN, javax.swing.GroupLayout.DEFAULT_SIZE, 31,
                                                Short.MAX_VALUE)
                                        .addComponent(btnAutori, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnEnviar, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnExport, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnNuevo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(20, 20, 20)));

        tablaTraspasos.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Número", "Origen", "Destino	", "Fecha Solicitud", "Estado", "Productos", "Acciones"
                }));
        tablaTraspasos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tablaTraspasosMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tablaTraspasos);

        panelFiltros.setBackground(new java.awt.Color(153, 255, 153));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("filtros");

        jLabel2.setText("Estado");

        cbxOrigen.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Seleccionar", "sucursal", "deposito", "temporal", " ", " " }));
        cbxOrigen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxOrigenActionPerformed(evt);
            }
        });

        jLabel3.setText("Bodega de origen");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Seleccionar", "pendiente", "autorizado", "en transito", "recibido", "cancelado" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jLabel4.setText("Bodega de destino");

        cbxdestino.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Seleccionar", "sucursal", "deposito", "temporal" }));
        cbxdestino.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxdestinoActionPerformed(evt);
            }
        });

        jLabel5.setText("fecha");

        TfdFecha.setText("fecha");
        TfdFecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TfdFechaActionPerformed(evt);
            }
        });

        BtnAjustes.setText("Ajuste");
        BtnAjustes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnAjustesActionPerformed(evt);
            }
        });

        CheAjuste.setText("Ajuste");

        javax.swing.GroupLayout panelFiltrosLayout = new javax.swing.GroupLayout(panelFiltros);
        panelFiltros.setLayout(panelFiltrosLayout);
        panelFiltrosLayout.setHorizontalGroup(
                panelFiltrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFiltrosLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelFiltrosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxOrigen, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jComboBox2, 0, 176, Short.MAX_VALUE)
                                        .addComponent(cbxdestino, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(TfdFecha)
                                        .addGroup(panelFiltrosLayout.createSequentialGroup()
                                                .addComponent(BtnAjustes)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(CheAjuste)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap()));
        panelFiltrosLayout.setVerticalGroup(
                panelFiltrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFiltrosLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxOrigen, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxdestino, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TfdFecha, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelFiltrosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(BtnAjustes)
                                        .addComponent(CheAjuste))
                                .addContainerGap(33, Short.MAX_VALUE)));

        panelhead2.setBackground(new java.awt.Color(204, 255, 204));

        btnEnviado.setText("Envio");
        btnEnviado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviadoActionPerformed(evt);
            }
        });

        btnRecibido.setText("Recibido");
        btnRecibido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRecibidoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelhead2Layout = new javax.swing.GroupLayout(panelhead2);
        panelhead2.setLayout(panelhead2Layout);
        panelhead2Layout.setHorizontalGroup(
                panelhead2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelhead2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnEnviado, javax.swing.GroupLayout.PREFERRED_SIZE, 419,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRecibido, javax.swing.GroupLayout.PREFERRED_SIZE, 427,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));
        panelhead2Layout.setVerticalGroup(
                panelhead2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelhead2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        panelhead2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btnRecibido, javax.swing.GroupLayout.DEFAULT_SIZE, 43,
                                                        Short.MAX_VALUE)
                                                .addComponent(btnEnviado, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addComponent(panelFiltros, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 800,
                                                        Short.MAX_VALUE))
                                        .addComponent(panelAcciones, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelhead2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(20, 20, 20)));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(panelAcciones, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelhead2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(41, 41, 41)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panelFiltros, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 537,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(27, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lb)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnviadoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnEnviadoActionPerformed
        filtroDireccion = "ENVIADO";
        getTablaTraspasos().putClientProperty("traspasos.filtroDireccion", filtroDireccion);
        cargarTraspasos();
        actualizarEstiloBotonesFiltro();
    }// GEN-LAST:event_btnEnviadoActionPerformed

    private void btnRecibidoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnRecibidoActionPerformed
        filtroDireccion = "RECIBIDO";
        getTablaTraspasos().putClientProperty("traspasos.filtroDireccion", filtroDireccion);
        cargarTraspasos();
        actualizarEstiloBotonesFiltro();
    }// GEN-LAST:event_btnRecibidoActionPerformed

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnNuevoActionPerformed
        // ================================================================
        // VALIDACIÓN DE PERMISOS
        // ================================================================
        if (!verificarPermisosCrearTraspaso()) {
            Toast.show(this, Toast.Type.WARNING,
                    "No tiene permisos para crear traspasos");
            return;
        }

        // ================================================================
        // MOSTRAR LOADING Y PROCESAR EN SEGUNDO PLANO
        // ================================================================
        showLoadingOverlay("Preparando formulario de traspaso...");

        // Usar SwingWorker para no bloquear la UI
        SwingWorker<creartraspaso, Void> worker = new SwingWorker<creartraspaso, Void>() {
            @Override
            protected creartraspaso doInBackground() throws Exception {
                // Crear instancia del formulario
                creartraspaso cr = new creartraspaso();
                cr.settraspasos(traspasos.this);

                // Agregar listener para actualizar tabla cuando se confirme el traspaso
                cr.addPropertyChangeListener("traspasoConfirmado", evt1 -> {
                    if (Boolean.TRUE.equals(evt1.getNewValue())) {
                        SwingUtilities.invokeLater(() -> {
                            cargarTraspasos();
                        });
                    }
                });

                return cr;
            }

            @Override
            protected void done() {
                try {
                    // Obtener el formulario creado
                    creartraspaso cr = get();

                    // Ocultar loading antes de mostrar el modal
                    hideLoadingOverlay();

                    // Mostrar el modal en el EDT con mejor manejo de renderizado
                    SwingUtilities.invokeLater(() -> {
                        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {};

                        // Crear el modal con manejo mejorado de renderizado
                        SimpleModalBorder modal = new SimpleModalBorder(cr, "Nuevo traspaso", options,
                                new ModalCallback() {
                                    @Override
                                    public void action(ModalController mc, int i) {
                                        if (i == SimpleModalBorder.OPENED) {
                                            // Configurar el modal controller
                                            cr.setModalController(mc);

                                            // Interceptar clic en overlay para confirmación
                                            SwingUtilities.invokeLater(() -> {
                                                try {
                                                    java.awt.Component root = SwingUtilities.getRoot(cr);
                                                    if (root instanceof javax.swing.JFrame) {
                                                        javax.swing.JFrame frame = (javax.swing.JFrame) root;
                                                        java.awt.Component glassPane = frame.getGlassPane();

                                                        // 1. Guardar listeners originales
                                                        java.awt.event.MouseListener[] originalListeners = glassPane
                                                                .getMouseListeners();

                                                        // 2. Remover listeners originales (para detener el cierre
                                                        // automático)
                                                        for (java.awt.event.MouseListener l : originalListeners) {
                                                            glassPane.removeMouseListener(l);
                                                        }

                                                        // 3. Agregar nuestro listener personalizado
                                                        java.awt.event.MouseAdapter overlayListener = new java.awt.event.MouseAdapter() {
                                                            @Override
                                                            public void mousePressed(java.awt.event.MouseEvent e) {
                                                                // Verificar si el clic fue fuera del modal
                                                                // El modal real está contenido en el glassPane o es
                                                                // hijo de él
                                                                // Buscamos el componente SimpleModalBorder (padre de
                                                                // cr)
                                                                java.awt.Component modalContainer = cr.getParent();
                                                                if (modalContainer == null)
                                                                    return;

                                                                java.awt.Point p = SwingUtilities.convertPoint(
                                                                        glassPane, e.getPoint(), modalContainer);

                                                                // Si el punto NO está dentro del contenedor del modal,
                                                                // es un clic en el overlay
                                                                if (!modalContainer.contains(p)) {
                                                                    e.consume(); // Consumir evento

                                                                    // Mostrar confirmación
                                                                    int opt = JOptionPane.showConfirmDialog(cr,
                                                                            "¿Desea cancelar la creación del transpaso?",
                                                                            "Confirmar cierre",
                                                                            JOptionPane.YES_NO_OPTION,
                                                                            JOptionPane.WARNING_MESSAGE);

                                                                    if (opt == JOptionPane.YES_OPTION) {
                                                                        cr.setOmitirConfirmacion(true);
                                                                        mc.close();
                                                                    }
                                                                    // Si es NO, no hacemos nada (el modal sigue abierto
                                                                    // y el evento fue consumido)
                                                                }
                                                            }
                                                        };
                                                        glassPane.addMouseListener(overlayListener);

                                                        // 4. Restaurar listeners cuando el modal se cierre (se oculte)
                                                        cr.addHierarchyListener(new java.awt.event.HierarchyListener() {
                                                            @Override
                                                            public void hierarchyChanged(
                                                                    java.awt.event.HierarchyEvent e) {
                                                                if ((e.getChangeFlags()
                                                                        & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                                                                    if (!cr.isShowing()) {
                                                                        // El modal se cerró/ocultó
                                                                        glassPane.removeMouseListener(overlayListener);
                                                                        for (java.awt.event.MouseListener l : originalListeners) {
                                                                            glassPane.addMouseListener(l);
                                                                        }
                                                                        cr.removeHierarchyListener(this);
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                } catch (Exception e) {
                                                    System.err.println("Error configurando listener de overlay: "
                                                            + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            });

                                            // Inicializar el formulario
                                            cr.init();

                                            if (!cr.iniciarConAutoConfig()) {
                                                paso1 p1 = new paso1(cr.controller);
                                                p1.setSize(728, 640);
                                                p1.setLocation(0, 0);

                                                // Mejor manejo del renderizado para evitar problemas visuales
                                                SwingUtilities.invokeLater(() -> {
                                                    cr.panelPasos.removeAll();
                                                    cr.panelPasos.add(p1, BorderLayout.CENTER);

                                                    // Forzar actualización del layout de forma ordenada
                                                    cr.panelPasos.invalidate();
                                                    cr.panelPasos.validate();
                                                    cr.panelPasos.repaint();

                                                    // Forzar actualización del contenedor padre también
                                                    Container parent = cr.panelPasos.getParent();
                                                    if (parent != null) {
                                                        parent.invalidate();
                                                        parent.validate();
                                                        parent.repaint();
                                                    }
                                                });

                                                cr.paso = 1;
                                                cr.panelPaso1 = p1;
                                            }
                                        } else if (i == SimpleModalBorder.CANCEL_OPTION) {
                                            // Verificar si hay cambios pendientes en el formulario antes de cerrar
                                            // Si hay datos, preguntar. Si no, cerrar directo.
                                            if (cr.confirmarCierreActual()) {
                                                Object[] options = { "Confirmar", "Cancelar" };
                                                JOptionPane optionPane = new JOptionPane(
                                                        "¿Desea cancelar la creación del transpaso?",
                                                        JOptionPane.WARNING_MESSAGE,
                                                        JOptionPane.YES_NO_OPTION,
                                                        null,
                                                        options,
                                                        options[1]);

                                                // Configuración de accesibilidad
                                                optionPane.getAccessibleContext()
                                                        .setAccessibleName("Confirmar cancelación");
                                                optionPane.getAccessibleContext().setAccessibleDescription(
                                                        "Diálogo de confirmación para cancelar la creación del traspaso y perder los cambios.");

                                                JDialog dialog = optionPane.createDialog(traspasos.this,
                                                        "Confirmar cierre");
                                                dialog.setVisible(true);

                                                Object selectedValue = optionPane.getValue();
                                                // Mapear la selección al índice
                                                int opcion = -1;
                                                if (selectedValue != null) {
                                                    for (int k = 0; k < options.length; k++) {
                                                        if (options[k].equals(selectedValue)) {
                                                            opcion = k;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (opcion == 0) { // 0 es Confirmar
                                                    try {
                                                        cr.onModalClosing();
                                                    } catch (Throwable ignore) {
                                                    }
                                                    mc.close();
                                                    SwingUtilities.invokeLater(() -> cargarTraspasos());
                                                }
                                            } else {
                                                // Si no hay datos que confirmar, cerrar directamente
                                                mc.close();
                                            }
                                        }
                                    }
                                });

                        // Mostrar el modal con una pequeña demora para asegurar el renderizado correcto
                        Timer timer = new Timer(50, e -> {
                            ModalDialog.showModal(traspasos.this, modal);

                            // Configurar listeners después de mostrar el modal para asegurar que tenga
                            // padre
                            SwingUtilities.invokeLater(() -> {
                                // 1. Detectar clic en el overlay (padre del modal)
                                Container parent = modal.getParent();
                                if (parent != null) {
                                    parent.addMouseListener(new MouseAdapter() {
                                        @Override
                                        public void mousePressed(MouseEvent e) {
                                            // Solo si se hace clic directamente en el overlay (no en el modal)
                                            if (e.getSource() == parent) {
                                                if (cr.confirmarCierreActual()) {
                                                    Object[] options = { "Confirmar", "Cancelar" };
                                                    JOptionPane optionPane = new JOptionPane(
                                                            "¿Desea cancelar la creación del transpaso?",
                                                            JOptionPane.WARNING_MESSAGE,
                                                            JOptionPane.YES_NO_OPTION,
                                                            null,
                                                            options,
                                                            options[1]);
                                                    optionPane.getAccessibleContext()
                                                            .setAccessibleName("Confirmar cancelación");
                                                    optionPane.getAccessibleContext().setAccessibleDescription(
                                                            "Diálogo de confirmación para cancelar la creación del traspaso y perder los cambios.");
                                                    JDialog dialog = optionPane.createDialog(traspasos.this,
                                                            "Confirmar cierre");
                                                    dialog.setVisible(true);
                                                    Object selectedValue = optionPane.getValue();
                                                    int opcion = -1;
                                                    if (selectedValue != null) {
                                                        for (int k = 0; k < options.length; k++) {
                                                            if (options[k].equals(selectedValue)) {
                                                                opcion = k;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (opcion == 0) { // Confirmar
                                                        try {
                                                            cr.onModalClosing();
                                                        } catch (Throwable ignore) {
                                                        }
                                                        if (cr.getModalController() != null) {
                                                            cr.getModalController().close();
                                                        }
                                                        SwingUtilities.invokeLater(() -> cargarTraspasos());
                                                    }
                                                } else {
                                                    if (cr.getModalController() != null) {
                                                        cr.getModalController().close();
                                                    }
                                                }
                                            }
                                        }
                                    });
                                }

                                // 2. Detectar botones de cierre para omitir confirmación
                                java.util.Queue<java.awt.Component> queue = new java.util.LinkedList<>();
                                queue.add(modal);
                                while (!queue.isEmpty()) {
                                    java.awt.Component c = queue.poll();
                                    if (c instanceof javax.swing.JButton) {
                                        if (!SwingUtilities.isDescendingFrom(c, cr)) {
                                            ((javax.swing.JButton) c)
                                                    .addMouseListener(new java.awt.event.MouseAdapter() {
                                                        @Override
                                                        public void mousePressed(java.awt.event.MouseEvent e) {
                                                            cr.setOmitirConfirmacion(true);
                                                        }
                                                    });
                                        }
                                    }
                                    if (c instanceof java.awt.Container) {
                                        for (java.awt.Component child : ((java.awt.Container) c).getComponents()) {
                                            queue.add(child);
                                        }
                                    }
                                }
                            });

                            ((Timer) e.getSource()).stop();
                        });
                        timer.setRepeats(false);
                        timer.start();
                    });

                } catch (Exception e) {
                    hideLoadingOverlay();
                    System.err.println("ERROR Error abriendo formulario de traspaso: " + e.getMessage());
                    e.printStackTrace();
                    Toast.show(traspasos.this, Toast.Type.ERROR,
                            "Error al abrir formulario: " + e.getMessage());
                }
            }
        };

        // Ejecutar el worker
        worker.execute();
    }// GEN-LAST:event_btnNuevoActionPerformed

    private void btnAutoriActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAutoriActionPerformed
        showLoadingOverlay("Autorizando traspaso...");
        int filaSeleccionada = getTablaTraspasos().getSelectedRow();
        if (filaSeleccionada < 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un traspaso para autorizar");
            hideLoadingOverlay();
            return;
        }

        String numeroTraspaso = (String) getTablaTraspasos().getValueAt(filaSeleccionada, 0);
        String estado = (String) getTablaTraspasos().getValueAt(filaSeleccionada, 4);

        if (!"pendiente".equals(estado)) {
            Toast.show(this, Toast.Type.WARNING,
                    "Solo se pueden autorizar traspasos en estado pendiente");
            return;
        }

        try {
            autorizarTraspaso(numeroTraspaso);
        } finally {
            hideLoadingOverlay();
        }
    }// GEN-LAST:event_btnAutoriActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jComboBox1ActionPerformed
        if (!isCargandoCombos()) {
            cargarTraspasos();
        }
    }// GEN-LAST:event_jComboBox1ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jComboBox2ActionPerformed
        if (!isCargandoCombos()) {
            cargarTraspasos();
        }
    }// GEN-LAST:event_jComboBox2ActionPerformed

    private void jComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jComboBox3ActionPerformed
        if (!isCargandoCombos()) {
            cargarTraspasos();
        }
    }// GEN-LAST:event_jComboBox3ActionPerformed

    private void TfdFechaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_TfdFechaActionPerformed
        // Este evento se dispara cuando se presiona Enter en el campo de fecha
        // o cuando se pierde el foco después de editar manualmente
        try {
            String fechaTexto = TfdFecha.getText();

            // Validar que no esté vacío o sea el placeholder
            if (fechaTexto == null || fechaTexto.trim().isEmpty() || "fecha".equalsIgnoreCase(fechaTexto.trim())) {
                TfdFecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                fechaTexto = TfdFecha.getText();
            }

            // Validar formato de fecha (dd/MM/yyyy)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fechaSeleccionada = LocalDate.parse(fechaTexto, formatter);

            // Actualizar el DatePicker con la fecha ingresada manualmente
            datePicker.setSelectedDate(fechaSeleccionada);

            // Recargar la tabla con la nueva fecha
            cargarTraspasos();

        } catch (java.time.format.DateTimeParseException e) {
            // Error de formato, mostrar mensaje al usuario
            System.err.println("ERROR Formato de fecha inválido: " + TfdFecha.getText());
            Toast.show(this, Toast.Type.WARNING,
                    "Formato de fecha inválido. Use: dd/MM/yyyy (ejemplo: 18/12/2025)");

            // Restaurar fecha actual
            TfdFecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            datePicker.setSelectedDate(LocalDate.now());
            cargarTraspasos();
        } catch (Exception e) {
            System.err.println("ERROR Error procesando fecha: " + e.getMessage());
            e.printStackTrace();
            cargarTraspasos();
        }
    }// GEN-LAST:event_TfdFechaActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jTextField2ActionPerformed
        cargarTraspasos();
    }// GEN-LAST:event_jTextField2ActionPerformed

    private void tablaTraspasosMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_tablaTraspasosMouseClicked
        if (evt.getClickCount() == 2) { // Doble clic
            int fila = getTablaTraspasos().getSelectedRow();
            if (fila >= 0) {
                String numeroTraspaso = (String) getTablaTraspasos().getValueAt(fila, 0);
                verDetalleTraspaso(numeroTraspaso);
            }
        } else if (evt.getClickCount() == 1) { // Un solo clic
            // Actualizar botones según el estado seleccionado
            actualizarBotonesSegunEstado();
        }

    }// GEN-LAST:event_tablaTraspasosMouseClicked

    private void abrirDialogoFacturarRecibidos() {
        int fila = getTablaTraspasos().getSelectedRow();
        if (fila < 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un traspaso recibido");
            return;
        }
        String estado = (String) getTablaTraspasos().getValueAt(fila, 4);
        if (!"recibido".equalsIgnoreCase(estado)) {
            Toast.show(this, Toast.Type.WARNING, "Solo disponible para traspasos recibidos");
            return;
        }
        String numero = (String) getTablaTraspasos().getValueAt(fila, 0);
        mostrarDialogoFacturarRecibidos(numero);
    }

    private void mostrarDialogoFacturarRecibidos(String numeroTraspaso) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Facturar recibidos: " + numeroTraspaso);
        dialog.setModal(true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        javax.swing.JPanel contenido = new javax.swing.JPanel(new java.awt.BorderLayout());
        javax.swing.JTable tabla = new javax.swing.JTable();
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tabla);

        DefaultTableModel model = new DefaultTableModel(new Object[] {
                "Seleccionar", "ID Variante", "Foto", "Producto", "Llegaron", "Facturar"
        }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Boolean.class;
                    case 1:
                        return Integer.class;
                    case 2:
                        return ImageIcon.class;
                    case 4:
                        return Integer.class;
                    case 5:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 5;
            }
        };
        tabla.setModel(model);

        javax.swing.table.TableColumn colFoto = tabla.getColumnModel().getColumn(2);
        colFoto.setMinWidth(0);
        colFoto.setMaxWidth(0);
        colFoto.setPreferredWidth(0);
        javax.swing.table.TableColumn colProducto = tabla.getColumnModel().getColumn(3);
        colProducto.setPreferredWidth(420);
        tabla.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        tabla.setRowHeight(60);
        javax.swing.table.DefaultTableCellRenderer productoRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                javax.swing.JLabel img = new javax.swing.JLabel();
                Object iconObj = table.getValueAt(row, 2);
                if (iconObj instanceof javax.swing.ImageIcon) {
                    javax.swing.ImageIcon ic = (javax.swing.ImageIcon) iconObj;
                    Image scaled = ic.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
                    img.setIcon(new ImageIcon(scaled));
                }
                javax.swing.JLabel lbl = new javax.swing.JLabel(value != null ? value.toString() : "");
                lbl.setOpaque(false);
                lbl.setToolTipText(value != null ? value.toString() : "");
                javax.swing.JPanel left = new javax.swing.JPanel();
                left.setOpaque(false);
                left.add(img);
                panel.add(left, java.awt.BorderLayout.WEST);
                panel.add(lbl, java.awt.BorderLayout.CENTER);
                return panel;
            }
        };
        tabla.getColumnModel().getColumn(3).setCellRenderer(productoRenderer);

        javax.swing.JButton btnPasar = new javax.swing.JButton("Pasar a facturar");
        btnPasar.addActionListener(e -> {
            try {
                java.util.List<int[]> items = new java.util.ArrayList<>();
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object sel = model.getValueAt(i, 0);
                    if (sel instanceof Boolean && (Boolean) sel) {
                        int idVar = (Integer) model.getValueAt(i, 1);
                        int llegaron = (Integer) model.getValueAt(i, 4);
                        Object fv = model.getValueAt(i, 5);
                        int facturar = fv instanceof Integer ? (Integer) fv : 0;
                        if (facturar > 0 && facturar <= llegaron) {
                            items.add(new int[] { idVar, facturar });
                        }
                    }
                }
                if (items.isEmpty()) {
                    Toast.show(dialog, Toast.Type.WARNING, "Seleccione productos y cantidades válidas");
                    return;
                }
                try {
                    if (!validarStockEnBodegaDestino(numeroTraspaso, items)) {
                        return;
                    }
                } catch (Exception ex) {
                    Toast.show(dialog, Toast.Type.ERROR, "Error validando stock: " + ex.getMessage());
                    return;
                }
                generarVentaFor1 gf = new generarVentaFor1();
                for (int[] it : items) {
                    gf.agregarLineaPorVariante(it[0], it[1], "par");
                }
                Application.showForm(gf);
                dialog.dispose();
                java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
                if (win != null && this.isShowing()) {
                    Toast.show(this, Toast.Type.SUCCESS, "Productos pasados a factura");
                } else {
                    javax.swing.JOptionPane.showMessageDialog(win != null ? win : null,
                            "Productos pasados a factura",
                            "Información",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException ex) {
                System.getLogger(traspasos.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });

        contenido.add(sp, java.awt.BorderLayout.CENTER);
        javax.swing.JPanel south = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        south.add(btnPasar);
        contenido.add(south, java.awt.BorderLayout.SOUTH);
        dialog.setContentPane(contenido);

        try {
            Connection conn = conexion.getInstance().createConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT td.id_variante, td.cantidad_recibida AS llegaron, p.nombre AS producto, " +
                            "c.nombre AS color, CONCAT(t.numero, ' ', COALESCE(t.sistema,'')) AS talla, pv.imagen " +
                            "FROM traspaso_detalles td " +
                            "JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso " +
                            "JOIN productos p ON td.id_producto = p.id_producto " +
                            "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante " +
                            "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                            "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                            "WHERE tr.numero_traspaso = ? AND td.cantidad_recibida > 0");
            ps.setString(1, numeroTraspaso);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idVar = rs.getInt("id_variante");
                int llegaron = rs.getInt("llegaron");
                String nombre = rs.getString("producto");
                String color = rs.getString("color");
                String talla = rs.getString("talla");
                String nomFull = nombre + (color != null ? " (" + color + (talla != null ? " - " + talla : "") + ")"
                        : (talla != null ? " (" + talla + ")" : ""));
                byte[] img = rs.getBytes("imagen");
                javax.swing.ImageIcon icon;
                if (img != null && img.length > 0) {
                    icon = new ImageIcon(new ImageIcon(img).getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH));
                } else {
                    // Fallback: obtener imagen por id_variante desde servicio
                    try {
                        raven.clases.productos.ServiceProductVariant spv = new raven.clases.productos.ServiceProductVariant();
                        byte[] bytes = spv.getVariantImage(idVar);
                        if (bytes != null && bytes.length > 0) {
                            icon = new ImageIcon(
                                    new ImageIcon(bytes).getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH));
                        } else {
                            icon = ImageUtils.bytesToIcon(null);
                        }
                    } catch (Exception ex) {
                        icon = ImageUtils.bytesToIcon(null);
                    }
                }
                model.addRow(new Object[] { false, idVar, icon, nomFull, llegaron, 0 });
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception ex) {
            Toast.show(this, Toast.Type.ERROR, "Error cargando recibidos: " + ex.getMessage());
        }

        if (model.getRowCount() == 0) {
            Toast.show(dialog, Toast.Type.WARNING, "No hay productos recibidos para facturar");
            dialog.dispose();
            return;
        }

        dialog.setVisible(true);
    }

    private boolean validarStockEnBodegaDestino(String numeroTraspaso, java.util.List<int[]> items)
            throws java.sql.SQLException {

        // PASO 1: Obtener bodega destino del traspaso
        Integer idBodegaDestino = null;
        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con
                        .prepareStatement("SELECT id_bodega_destino FROM traspasos WHERE numero_traspaso = ?")) {
            pst.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    idBodegaDestino = rs.getInt(1);
                }
            }
        }

        if (idBodegaDestino == null || idBodegaDestino <= 0) {
            Toast.show(this, Toast.Type.WARNING, "No se encontró bodega destino del traspaso");
            return false;
        }

        // PASO 2: Obtener bodega del usuario actual
        Integer idBodegaUsuario = obtenerBodegaUsuarioSegura();

        // PASO 3: Validar que la bodega del usuario coincida con la bodega destino
        if (idBodegaUsuario == null || !idBodegaUsuario.equals(idBodegaDestino)) {
            String mensajeError = "No puede facturar este traspaso. ";
            if (idBodegaUsuario == null) {
                mensajeError += "No tiene bodega asignada.";
            } else {
                try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection()) {
                    String bodegaUsuario = obtenerNombreBodega(con, idBodegaUsuario);
                    String bodegaDestino = obtenerNombreBodega(con, idBodegaDestino);
                    mensajeError += String.format("Su bodega es '%s' pero el traspaso va a '%s'",
                            bodegaUsuario, bodegaDestino);
                }
            }
            Toast.show(this, Toast.Type.WARNING, mensajeError);

            return false;
        }
        // PASO 4: Validar que haya stock disponible (verificar AMBOS: Stock_par Y
        // Stock_caja)
        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(
                        "SELECT COALESCE(Stock_par,0) AS stock_par, COALESCE(Stock_caja,0) AS stock_caja " +
                                "FROM inventario_bodega WHERE id_bodega = ? AND id_variante = ? AND activo = 1")) {
            for (int[] it : items) {
                int idVar = it[0];
                int cant = it[1];
                pst.setInt(1, idBodegaDestino);
                pst.setInt(2, idVar);
                try (java.sql.ResultSet rs = pst.executeQuery()) {
                    int stockPar = 0;
                    int stockCaja = 0;
                    if (rs.next()) {
                        stockPar = rs.getInt("stock_par");
                        stockCaja = rs.getInt("stock_caja");
                    }

                    // El stock total es la suma de pares y cajas (convertidas a pares)
                    // Asumiendo que el tipo es "par" (línea 3313), verificar Stock_par
                    int stockDisponible = stockPar;
                    if (cant > stockDisponible) {
                        Toast.show(this, Toast.Type.WARNING,
                                "Stock insuficiente en bodega destino para variante " + idVar +
                                        " (Disponible: " + stockDisponible + ", Solicitado: " + cant + ")");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Método auxiliar para obtener nombre de bodega
    private String obtenerNombreBodega(java.sql.Connection con, int idBodega) {
        try (java.sql.PreparedStatement pst = con.prepareStatement("SELECT nombre FROM bodegas WHERE id_bodega = ?")) {
            pst.setInt(1, idBodega);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }
        } catch (Exception e) {
            return "Bodega #" + idBodega;
        }
        return "Desconocida";
    }

    private void exportarSeleccionadoAPDF() {
        int fila = getTablaTraspasos().getSelectedRow();
        if (fila < 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un traspaso");
            return;
        }

        String numeroTraspaso = (String) getTablaTraspasos().getValueAt(fila, 0);

        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Guardar PDF de traspaso");
        fileChooser.setSelectedFile(new java.io.File("recibo_traspaso_" + numeroTraspaso + ".pdf"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF (*.pdf)", "pdf"));

        int res = fileChooser.showSaveDialog(this);
        if (res != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File destino = fileChooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new java.io.File(destino.getAbsolutePath() + ".pdf");
        }

        reciboPDF generador = new reciboPDF();
        boolean exito = generador.generar(numeroTraspaso, destino);

        if (exito) {
            Toast.show(this, Toast.Type.SUCCESS, "PDF guardado en: " + destino.getAbsolutePath());
        } else {
            Toast.show(this, Toast.Type.ERROR, "Error al generar PDF");
        }
    }

    private javax.swing.JButton btnReporte;

    private void generarReporteDiario() {
        if (getTablaTraspasos().getRowCount() == 0) {
            Toast.show(this, Toast.Type.WARNING, "No hay datos para generar el reporte");
            return;
        }

        // Preguntar formato
        Object[] options = { "PDF", "Excel", "Cancelar" };
        int eleccion = javax.swing.JOptionPane.showOptionDialog(this,
                "Seleccione el formato de reporte:",
                "Generar Reporte",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (eleccion == 2 || eleccion == -1) // Cancelar
            return;

        boolean esExcel = (eleccion == 1);
        String ext = esExcel ? "xlsx" : "pdf";
        String filterName = esExcel ? "Excel (*.xlsx)" : "PDF (*.pdf)";

        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte de Traspasos");
        String fechaStr = new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date());
        fileChooser.setSelectedFile(new java.io.File("Reporte_Traspasos_" + fechaStr + "." + ext));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(filterName, ext));

        int res = fileChooser.showSaveDialog(this);
        if (res != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File tempDestino = fileChooser.getSelectedFile();
        if (!tempDestino.getName().toLowerCase().endsWith("." + ext)) {
            tempDestino = new java.io.File(tempDestino.getAbsolutePath() + "." + ext);
        }

        // Variables finales para el worker
        final java.io.File finalDestino = tempDestino;
        final boolean finalEsExcel = esExcel;

        // Recolectar datos UI en el EDT (thread actual)
        List<TraspasoReporteDTO> listaInicial = new ArrayList<>();
        for (int i = 0; i < getTablaTraspasos().getRowCount(); i++) {
            Object numeroObj = getTablaTraspasos().getValueAt(i, 0);
            String numero = numeroObj != null ? numeroObj.toString() : "";

            Object origenObj = getTablaTraspasos().getValueAt(i, 1);
            String origen = origenObj != null ? origenObj.toString() : "";

            Object destinoObj = getTablaTraspasos().getValueAt(i, 2);
            String destino = destinoObj != null ? destinoObj.toString() : "";

            Object fechaObj = getTablaTraspasos().getValueAt(i, 3);
            String fecha = "";
            if (fechaObj instanceof java.sql.Timestamp) {
                fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fechaObj);
            } else if (fechaObj != null) {
                fecha = fechaObj.toString();
            }

            Object estadoObj = getTablaTraspasos().getValueAt(i, 4);
            String estado = estadoObj != null ? estadoObj.toString() : "";

            Object totalObj = getTablaTraspasos().getValueAt(i, 5);
            String total = totalObj != null ? totalObj.toString() : "0";

            // Creamos DTO con detalles null por ahora
            listaInicial.add(new TraspasoReporteDTO(numero, origen, destino, fecha, estado, total, null));
        }

        final List<TraspasoReporteDTO> finalLista = listaInicial;

        // Construir string de filtros
        StringBuilder filtrosBuilder = new StringBuilder();
        String estado = (getjComboBox2() != null) ? (String) getjComboBox2().getSelectedItem() : "Todos";
        String fecha = (getjTextField2() != null) ? getjTextField2().getText() : "";

        Object origenObj = (getjComboBox1() != null) ? getjComboBox1().getSelectedItem() : null;
        Object destinoObj = (getjComboBox3() != null) ? getjComboBox3().getSelectedItem() : null;
        String origenStr = (origenObj instanceof Bodega) ? ((Bodega) origenObj).getNombre() : "Todas";
        String nombreDestino = (destinoObj instanceof Bodega) ? ((Bodega) destinoObj).getNombre() : "Todas";

        if (!"Seleccionar".equals(estado) && !"Todos".equals(estado))
            filtrosBuilder.append("Estado: ").append(estado).append("; ");
        if (!fecha.isEmpty())
            filtrosBuilder.append("Fecha: ").append(fecha).append("; ");
        if (!"Todas".equals(origenStr))
            filtrosBuilder.append("Origen: ").append(origenStr).append("; ");
        if (!"Todas".equals(nombreDestino))
            filtrosBuilder.append("Destino: ").append(nombreDestino).append("; ");

        final String finalFiltros = filtrosBuilder.toString();

        showLoadingOverlay("Generando reporte...");

        // Worker para tareas pesadas (BD y Exportación)
        new javax.swing.SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Completar detalles en background
                for (TraspasoReporteDTO dto : finalLista) {
                    try {
                        // Fetch detail list (products)
                        List<java.util.Map<String, Object>> detalles = obtenerDetallesTraspaso(dto.getNumero());
                        dto.setDetalles(detalles);

                        // Fetch header info (financials)
                        java.util.Map<String, Object> info = obtenerInformacionTraspaso(dto.getNumero());
                        if (info != null) {
                            if (info.get("monto_total") != null) {
                                dto.setMontoTotal((java.math.BigDecimal) info.get("monto_total"));
                            }
                            if (info.get("monto_recibido") != null) {
                                dto.setMontoRecibido((java.math.BigDecimal) info.get("monto_recibido"));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error obteniendo detalles para reporte: " + e.getMessage());
                    }
                }

                if (finalEsExcel) {
                    ExcelTraspasosExporter exporter = new ExcelTraspasosExporter();
                    return exporter.exportar(finalLista, finalDestino, finalFiltros);
                } else {
                    ReporteTraspasosGeneralPDF generador = new ReporteTraspasosGeneralPDF();
                    return generador.generarReporte(finalLista, finalDestino, finalFiltros);
                }
            }

            @Override
            protected void done() {
                hideLoadingOverlay();
                try {
                    boolean exito = get();
                    if (exito) {
                        Toast.show(traspasos.this, Toast.Type.SUCCESS, "Reporte generado correctamente");
                    } else {
                        Toast.show(traspasos.this, Toast.Type.ERROR, "Error al generar el reporte");
                    }
                } catch (Exception e) {
                    Toast.show(traspasos.this, Toast.Type.ERROR, "Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void generarPDFTraspaso(String numeroTraspaso, File archivo) throws Exception {
        Document doc = new Document(PageSize.LETTER);
        PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();

        com.itextpdf.text.Font fTitulo = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16,
                com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font fLabel = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font fValor = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.NORMAL);

        Paragraph pT = new Paragraph("TRASPASO " + numeroTraspaso, fTitulo);
        pT.setAlignment(Element.ALIGN_CENTER);
        pT.setSpacingAfter(10);
        doc.add(pT);

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingAfter(10);

        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = con.prepareStatement(
                        "SELECT t.numero_traspaso, t.estado, t.motivo, t.observaciones, " +
                                "DATE_FORMAT(t.fecha_solicitud, '%d/%m/%Y %H:%i') AS fecha_sol, " +
                                "bo.nombre AS origen, bd.nombre AS destino, u.nombre AS usuario " +
                                "FROM traspasos t " +
                                "LEFT JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega " +
                                "LEFT JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega " +
                                "LEFT JOIN usuarios u ON t.id_usuario_solicita = u.id_usuario " +
                                "WHERE t.numero_traspaso = ?")) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    agregarFila(info, "Estado:", rs.getString("estado"), fLabel, fValor);
                    agregarFila(info, "Tipo:", rs.getString("motivo"), fLabel, fValor);
                    agregarFila(info, "Fecha solicitud:", rs.getString("fecha_sol"), fLabel, fValor);
                    agregarFila(info, "Origen:", rs.getString("origen"), fLabel, fValor);
                    agregarFila(info, "Destino:", rs.getString("destino"), fLabel, fValor);
                    agregarFila(info, "Solicita:", rs.getString("usuario"), fLabel, fValor);
                    agregarFila(info, "Motivo:", rs.getString("motivo"), fLabel, fValor);
                    agregarFila(info, "Observaciones:", rs.getString("observaciones"), fLabel, fValor);
                }
            }
        }
        doc.add(info);

        PdfPTable det = new PdfPTable(6);
        det.setWidthPercentage(100);
        det.setWidths(new int[] { 40, 12, 12, 12, 12, 12 });
        det.setSpacingBefore(10);
        det.setSpacingAfter(10);
        agregarHeader(det, new String[] { "Producto", "Solic.", "Env.", "Rec.", "Color", "Talla" });

        try (java.sql.Connection con = raven.controlador.principal.conexion.getInstance().createConnection();
                java.sql.PreparedStatement ps = con.prepareStatement(
                        "SELECT p.nombre AS producto, td.cantidad_solicitada, td.cantidad_enviada, td.cantidad_recibida, "
                                +
                                "c.nombre AS color, CONCAT(t.numero, ' ', t.sistema) AS talla " +
                                "FROM traspaso_detalles td " +
                                "JOIN traspasos t ON td.id_traspaso = t.id_traspaso " +
                                "JOIN productos p ON td.id_producto = p.id_producto " +
                                "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante " +
                                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                                "WHERE t.numero_traspaso = ?")) {
            ps.setString(1, numeroTraspaso);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    det.addCell(new Phrase(rs.getString("producto"), fValor));
                    det.addCell(new Phrase(String.valueOf(rs.getInt("cantidad_solicitada")), fValor));
                    det.addCell(new Phrase(String.valueOf(rs.getInt("cantidad_enviada")), fValor));
                    det.addCell(new Phrase(String.valueOf(rs.getInt("cantidad_recibida")), fValor));
                    det.addCell(new Phrase(rs.getString("color"), fValor));
                    det.addCell(new Phrase(rs.getString("talla"), fValor));
                }
            }
        }
        doc.add(det);

        doc.close();
        Toast.show(this, Toast.Type.SUCCESS, "PDF generado");
    }

    private void agregarHeader(PdfPTable t, String[] headers) {
        com.itextpdf.text.Font fH = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.BOLD);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(new BaseColor(230, 230, 230));
            t.addCell(c);
        }
    }

    private void agregarFila(PdfPTable tabla, String label, String valor, com.itextpdf.text.Font fLabel,
            com.itextpdf.text.Font fValor) {
        tabla.addCell(new Phrase(label, fLabel));
        tabla.addCell(new Phrase(valor != null ? valor : "", fValor));
    }

    public void actualizarTraspasosPorEvento(String numeroTraspaso, String nuevoEstado) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Recargar todos los traspasos
                cargarTraspasos();

                // Buscar y seleccionar el traspaso actualizado si está visible
                for (int i = 0; i < getTablaTraspasos().getRowCount(); i++) {
                    String numero = (String) getTablaTraspasos().getValueAt(i, 0);
                    if (numeroTraspaso.equals(numero)) {
                        getTablaTraspasos().setRowSelectionInterval(i, i);
                        getTablaTraspasos().scrollRectToVisible(getTablaTraspasos().getCellRect(i, 0, true));
                        break;
                    }
                }

                // Actualizar botones
                actualizarBotonesSegunEstado();

                // Mostrar notificación
                Toast.show(this, Toast.Type.SUCCESS,
                        "Traspaso " + numeroTraspaso + " actualizado a: " + nuevoEstado);

            } catch (Exception e) {
                System.err.println("Error en actualización por evento: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnEnviarActionPerformed
        showLoadingOverlay("Abriendo acción de traspaso...");
        // Verificar que hay una fila seleccionada
        int filaSeleccionada = getTablaTraspasos().getSelectedRow();
        if (filaSeleccionada < 0) {
            Toast.show(this, Toast.Type.WARNING, "Seleccione un traspaso para enviar/recibir");
            hideLoadingOverlay();
            return;
        }

        // Obtener datos de la fila seleccionada
        String numeroTraspaso = (String) getTablaTraspasos().getValueAt(filaSeleccionada, 0);
        String estado = (String) getTablaTraspasos().getValueAt(filaSeleccionada, 4);

        // Verificar estado y ejecutar acción correspondiente
        switch (estado.toLowerCase()) {
            case "autorizado":
                // Ejecutar funcionalidad de envío
                try {
                    enviarTraspaso(numeroTraspaso);
                } finally {
                    hideLoadingOverlay();
                }
                break;

            case "en_transito":
                // Usar el método de recepción existente
                try {
                    recibirTraspaso(numeroTraspaso);
                } finally {
                    hideLoadingOverlay();
                }
                break;

            default:
                Toast.show(this, Toast.Type.WARNING,
                        "El traspaso debe estar autorizado para enviar o en tránsito para recibir.\n"
                                + "Estado actual: " + estado);
                hideLoadingOverlay();
                break;
        }
    }// GEN-LAST:event_btnEnviarActionPerformed

    private void BtnAjustesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_BtnAjustesActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_BtnAjustesActionPerformed

    private void cbxOrigenActionPerformed(java.awt.event.ActionEvent evt) {
        if (!isCargandoCombos()) {
            cargarTraspasos();
        }
    }

    private void cbxdestinoActionPerformed(java.awt.event.ActionEvent evt) {
        if (!isCargandoCombos()) {
            cargarTraspasos();
        }
    }
    // Declara un método que devuelve una lista de objetos ModelProduct

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtnAjustes;
    private javax.swing.JCheckBox CheAjuste;
    public javax.swing.JFormattedTextField TfdFecha;
    private javax.swing.JButton btnAutori;
    private javax.swing.JButton btnEnviado;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JButton btnRecibido;
    private javax.swing.JComboBox<String> cbxOrigen;
    private javax.swing.JComboBox<String> cbxdestino;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lb;
    private javax.swing.JPanel panel;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JPanel panelFiltros;
    private javax.swing.JPanel panelhead2;
    private javax.swing.JTable tablaTraspasos;
    private javax.swing.JTextField txtBuscarN;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the service
     */
    public ServiceProduct getService() {
        return service;
    }

    /**
     * @param service the service to set
     */
    public void setService(ServiceProduct service) {
        this.service = service;
    }

    /**
     * @return the menu
     */
    public JPopupMenu getMenu() {
        return menu;
    }

    /**
     * @param menu the menu to set
     */
    public void setMenu(JPopupMenu menu) {
        this.menu = menu;
    }

    /**
     * @return the search
     */
    public PanelSearch getSearch() {
        return search;
    }

    /**
     * @param search the search to set
     */
    public void setSearch(PanelSearch search) {
        this.search = search;
    }

    /**
     * @return the traspasoService
     */
    public TraspasoService getTraspasoService() {
        return traspasoService;
    }

    /**
     * @param traspasoService the traspasoService to set
     */
    public void setTraspasoService(TraspasoService traspasoService) {
        this.traspasoService = traspasoService;
    }

    /**
     * @return the idUsuarioActual
     */
    public int getIdUsuarioActual() {
        return idUsuarioActual;
    }

    /**
     * @param idUsuarioActual the idUsuarioActual to set
     */
    public void setIdUsuarioActual(int idUsuarioActual) {
        this.idUsuarioActual = idUsuarioActual;
    }

    /**
     * @return the controller
     */
    public TraspasoController getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    public void setController(TraspasoController controller) {
        this.controller = controller;
    }

    /**
     * @return the STYLE_PANEL
     */
    public static String getPANEL() {
        return STYLE_PANEL;
    }

    /**
     * @return the STYLE_PANEL
     */
    public static String getPANEL_STYLE() {
        return STYLE_PANEL;
    }

    /**
     * @return the FONT_HEADER_STYLE
     */
    public static String getFONT_HEADER_STYLE() {
        return FONT_HEADER_STYLE;
    }

    /**
     * @return the FONT_SUBHEADER_STYLE
     */
    public static String getFONT_SUBHEADER_STYLE() {
        return FONT_SUBHEADER_STYLE;
    }

    /**
     * @return the STYLE_TEXTFIELD
     */
    public static String getCamposdetexto() {
        return STYLE_TEXTFIELD;
    }

    /**
     * @return the LOGGER
     */
    public static Logger getLOGGER() {
        return LOGGER;
    }

    /**
     * @param aLOGGER the LOGGER to set
     */
    public static void setLOGGER(Logger aLOGGER) {
        LOGGER = aLOGGER;
    }

    /**
     * @return the cargandoCombos
     */
    public boolean isCargandoCombos() {
        return cargandoCombos;
    }

    /**
     * @param cargandoCombos the cargandoCombos to set
     */
    public void setCargandoCombos(boolean cargandoCombos) {
        this.cargandoCombos = cargandoCombos;
    }

    /**
     * @return the btnAutori
     */
    public javax.swing.JButton getBtnAutori() {
        return btnAutori;
    }

    /**
     * @param btnAutori the btnAutori to set
     */
    public void setBtnAutori(javax.swing.JButton btnAutori) {
        this.btnAutori = btnAutori;
    }

    /**
     * @return the btnEnviar
     */
    public javax.swing.JButton getBtnEnviar() {
        return btnEnviar;
    }

    /**
     * @param btnEnviar the btnEnviar to set
     */
    public void setBtnEnviar(javax.swing.JButton btnEnviar) {
        this.btnEnviar = btnEnviar;
    }

    /**
     * @return the btnExport
     */
    public javax.swing.JButton getBtnExport() {
        return btnExport;
    }

    /**
     * @param btnExport the btnExport to set
     */
    public void setBtnExport(javax.swing.JButton btnExport) {
        this.btnExport = btnExport;
    }

    /**
     * @return the btnNuevo
     */
    public javax.swing.JButton getBtnNuevo() {
        return btnNuevo;
    }

    /**
     * @param btnNuevo the btnNuevo to set
     */
    public void setBtnNuevo(javax.swing.JButton btnNuevo) {
        this.btnNuevo = btnNuevo;
    }

    /**
     * @return the jComboBox1
     */
    public javax.swing.JComboBox<String> getjComboBox1() {
        return cbxOrigen;
    }

    /**
     * @param jComboBox1 the jComboBox1 to set
     */
    public void setjComboBox1(javax.swing.JComboBox<String> jComboBox1) {
        this.cbxOrigen = jComboBox1;
    }

    /**
     * @return the jComboBox2
     */
    public javax.swing.JComboBox<String> getjComboBox2() {
        return jComboBox2;
    }

    /**
     * @param jComboBox2 the jComboBox2 to set
     */
    public void setjComboBox2(javax.swing.JComboBox<String> jComboBox2) {
        this.jComboBox2 = jComboBox2;
    }

    /**
     * @return the jComboBox3
     */
    public javax.swing.JComboBox<String> getjComboBox3() {
        return cbxdestino;
    }

    /**
     * @param jComboBox3 the jComboBox3 to set
     */
    public void setjComboBox3(javax.swing.JComboBox<String> jComboBox3) {
        this.cbxdestino = jComboBox3;
    }

    /**
     * @return the jLabel1
     */
    public javax.swing.JLabel getjLabel1() {
        return jLabel1;
    }

    /**
     * @param jLabel1 the jLabel1 to set
     */
    public void setjLabel1(javax.swing.JLabel jLabel1) {
        this.jLabel1 = jLabel1;
    }

    /**
     * @return the jLabel2
     */
    public javax.swing.JLabel getjLabel2() {
        return jLabel2;
    }

    /**
     * @param jLabel2 the jLabel2 to set
     */
    public void setjLabel2(javax.swing.JLabel jLabel2) {
        this.jLabel2 = jLabel2;
    }

    /**
     * @return the jLabel3
     */
    public javax.swing.JLabel getjLabel3() {
        return jLabel3;
    }

    /**
     * @param jLabel3 the jLabel3 to set
     */
    public void setjLabel3(javax.swing.JLabel jLabel3) {
        this.jLabel3 = jLabel3;
    }

    /**
     * @return the jLabel4
     */
    public javax.swing.JLabel getjLabel4() {
        return jLabel4;
    }

    /**
     * @param jLabel4 the jLabel4 to set
     */
    public void setjLabel4(javax.swing.JLabel jLabel4) {
        this.jLabel4 = jLabel4;
    }

    /**
     * @return the jLabel5
     */
    public javax.swing.JLabel getjLabel5() {
        return jLabel5;
    }

    /**
     * @param jLabel5 the jLabel5 to set
     */
    public void setjLabel5(javax.swing.JLabel jLabel5) {
        this.jLabel5 = jLabel5;
    }

    /**
     * @return the jScrollPane1
     */
    public javax.swing.JScrollPane getjScrollPane1() {
        return jScrollPane1;
    }

    /**
     * @param jScrollPane1 the jScrollPane1 to set
     */
    public void setjScrollPane1(javax.swing.JScrollPane jScrollPane1) {
        this.jScrollPane1 = jScrollPane1;
    }

    /**
     * @return the jTextField2
     */
    public javax.swing.JFormattedTextField getjTextField2() {
        return TfdFecha;
    }

    /**
     * @param jTextField2 the jTextField2 to set
     */
    public void setjTextField2(javax.swing.JFormattedTextField jTextField2) {
        this.TfdFecha = jTextField2;
    }

    /**
     * @return the lb
     */
    public javax.swing.JLabel getLb() {
        return lb;
    }

    /**
     * @param lb the lb to set
     */
    public void setLb(javax.swing.JLabel lb) {
        this.lb = lb;
    }

    /**
     * @return the panel
     */
    public javax.swing.JPanel getPanel() {
        return panel;
    }

    /**
     * @param panel the panel to set
     */
    public void setPanel(javax.swing.JPanel panel) {
        this.panel = panel;
    }

    /**
     * @return the panelAcciones
     */
    public javax.swing.JPanel getPanelAcciones() {
        return panelAcciones;
    }

    /**
     * @param panelAcciones the panelAcciones to set
     */
    public void setPanelAcciones(javax.swing.JPanel panelAcciones) {
        this.panelAcciones = panelAcciones;
    }

    /**
     * @return the panelFiltros
     */
    public javax.swing.JPanel getPanelFiltros() {
        return panelFiltros;
    }

    /**
     * @param panelFiltros the panelFiltros to set
     */
    public void setPanelFiltros(javax.swing.JPanel panelFiltros) {
        this.panelFiltros = panelFiltros;
    }

    /**
     * @return the tablaTraspasos
     */
    public javax.swing.JTable getTablaTraspasos() {
        return tablaTraspasos;
    }

    /**
     * @param tablaTraspasos the tablaTraspasos to set
     */
    public void setTablaTraspasos(javax.swing.JTable tablaTraspasos) {
        this.tablaTraspasos = tablaTraspasos;
    }

    /**
     * @return the txtBuscarN
     */
    public javax.swing.JTextField getTxtBuscarN() {
        return txtBuscarN;
    }

    /**
     * @param txtBuscarN the txtBuscarN to set
     */
    public void setTxtBuscarN(javax.swing.JTextField txtBuscarN) {
        this.txtBuscarN = txtBuscarN;
    }

    private void showLoadingOverlay(String message) {
        try {
            java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (win instanceof javax.swing.RootPaneContainer) {
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

                javax.swing.RootPaneContainer rpc = (javax.swing.RootPaneContainer) win;
                rpc.getRootPane().setGlassPane(overlay);
                overlay.setVisible(true);
                win.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                loadingOverlay = overlay;
                overlayActive = true;
            }
        } catch (Exception ignore) {
        }
    }

    private void hideLoadingOverlay() {
        try {
            java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (overlayActive && loadingOverlay != null) {
                loadingOverlay.setVisible(false);
                loadingOverlay = null;
                overlayActive = false;
            }
            if (win != null) {
                win.setCursor(java.awt.Cursor.getDefaultCursor());
            }
        } catch (Exception ignore) {
        }
    }
}
