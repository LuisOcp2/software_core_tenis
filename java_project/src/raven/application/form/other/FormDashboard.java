package raven.application.form.other;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// Gráficas
import raven.chart.bar.HorizontalBarChart;
import raven.chart.line.LineChart;
import raven.chart.pie.PieChart;
import raven.chart.data.category.DefaultCategoryDataset;
import raven.chart.data.pie.DefaultPieDataset;

// Servicios y modelos
import raven.clases.admin.ServiceBodegas;
import raven.clases.reportes.ServiceDashboardStats;
import raven.componentes.notificacion.Notification;
import raven.controlador.admin.ModelBodegas;
import raven.controlador.admin.ModelUser;
import raven.clases.admin.UserSession;
import raven.controlador.principal.AppConfig;
import raven.utils.NotificacionesService;
import raven.utils.VerificadorAutomatico;

// Apache POI para Excel
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// iText para PDF
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Dashboard Estadístico Profesional con soporte responsive
 *
 * Panel principal con estadísticas diferenciadas por rol:
 * - GERENTE: Ve estadísticas de TODAS las bodegas
 * - ADMIN: Ve estadísticas de SU bodega asignada
 *
 * @author Xtreme System
 */
public class FormDashboard extends JPanel {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES Y ESTILOS
    // ═══════════════════════════════════════════════════════════════════════════
    private static final Logger LOGGER = Logger.getLogger(FormDashboard.class.getName());
    private static final DecimalFormat FORMATO_DINERO = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat FORMATO_NUMERO = new DecimalFormat("#,##0");

    // Colores profesionales
    private static final Color COLOR_POSITIVO = new Color(40, 205, 65);
    private static final Color COLOR_NEGATIVO = new Color(255, 69, 58);
    private static final Color COLOR_ADVERTENCIA = new Color(255, 159, 10);
    private static final Color COLOR_PRIMARIO = new Color(99, 102, 241);
    private static final Color COLOR_SECUNDARIO = new Color(129, 140, 248);
    private static final Color COLOR_FONDO_CARD = new Color(30, 30, 46);
    private static final Color COLOR_BORDE = new Color(49, 50, 68);
    private static final String DASHBOARD_ICON_PATH = "raven/icon/svg/dashboard/";

    // Gradientes para tarjetas KPI
    private static final Color[][] GRADIENTES_KPI = {
            { new Color(99, 102, 241), new Color(168, 85, 247) }, // Ventas - Púrpura
            { new Color(34, 197, 94), new Color(16, 185, 129) }, // Ganancia - Verde
            { new Color(59, 130, 246), new Color(99, 102, 241) }, // Transacciones - Azul
            { new Color(249, 115, 22), new Color(234, 88, 12) }, // Ticket - Naranja
            { new Color(239, 68, 68), new Color(220, 38, 38) }, // Gastos - Rojo
            { new Color(20, 184, 166), new Color(6, 182, 212) } // Balance - Cyan
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // VARIABLES RESPONSIVE
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int DESKTOP_WIDTH = 1400;
    private static final int TABLET_WIDTH = 1024;
    private static final int MOBILE_WIDTH = 768;

    // Variables para controlar el layout responsive
    private boolean isDesktop = true;
    private boolean isTablet = false;
    private boolean isMobile = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // SESIÓN Y USUARIO
    // ═══════════════════════════════════════════════════════════════════════════
    private final UserSession session = UserSession.getInstance();
    private ModelUser currentUser;
    private boolean esAdmin = false; // Admin ve TODAS las bodegas, Gerente ve solo su bodega
    private Integer idBodegaFiltro = null;
    private boolean inicializacionCompleta = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVICIOS
    // ═══════════════════════════════════════════════════════════════════════════
    private final ServiceDashboardStats statsService = new ServiceDashboardStats();

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTROS
    // ═══════════════════════════════════════════════════════════════════════════
    private JComboBox<String> cmbPeriodo;
    private JComboBox<Object> cmbBodega;
    private JSpinner spnLimiteProductos;
    private int diasFiltro = 30;
    private int limiteProductos = 10;

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENTES KPI
    // ═══════════════════════════════════════════════════════════════════════════
    private JLabel lblVentasTotales, lblVentasVariacion;
    private JLabel lblGananciaNeta, lblGananciaVariacion;
    private JLabel lblTransacciones, lblTransaccionesVariacion;
    private JLabel lblTicketPromedio, lblTicketVariacion;
    private JLabel lblGastosOperativos;
    private JLabel lblBalanceNeto;

    // ═══════════════════════════════════════════════════════════════════════════
    // GRÁFICOS
    // ═══════════════════════════════════════════════════════════════════════════
    private PieChart pieCategoria;
    private PieChart pieMetodoPago;
    private HorizontalBarChart barProductosTop;
    private HorizontalBarChart barProductosMenos;
    private LineChart lineTendencia;

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLAS
    // ═══════════════════════════════════════════════════════════════════════════
    private JTable tablaVendedores;
    private DefaultTableModel modeloVendedores;

    // Tabla de stock bajo
    private JTable tablaStockBajo;
    private DefaultTableModel modeloStockBajo;

    // ═══════════════════════════════════════════════════════════════════════════
    // OTROS COMPONENTES
    // ═══════════════════════════════════════════════════════════════════════════
    private JProgressBar progressBar;
    private JLabel lblTitulo;
    private JLabel lblUsuarioInfo;
    private javax.swing.Timer timerVerificacion;

    // Loading overlay
    private JPanel loadingOverlay;
    private JLabel lblLoadingText;
    private JLabel lblLoadingSpinner;
    private javax.swing.Timer spinnerTimer;
    private JScrollPane mainScrollPane;
    private JPanel mainContentPanel;
    private JPanel panelHeader;
    private JPanel headerLeftPanel;
    private JPanel headerButtonsPanel;
    private JPanel panelFiltros;
    private JLabel lblPeriodo;
    private JLabel lblBodega;
    private JLabel lblLimite;
    private JPanel panelKPIs;
    private JPanel panelGraficos1;
    private JPanel panelGraficos2;
    private javax.swing.Timer responsiveTimer;

    private static FlatSVGIcon dashboardIcon(String name, int size) {
        return new FlatSVGIcon(DASHBOARD_ICON_PATH + name, size, size);
    }

    private static FlatSVGIcon dashboardIcon(String name, int width, int height) {
        return new FlatSVGIcon(DASHBOARD_ICON_PATH + name, width, height);
    }

    private static FlatSVGIcon dashboardIcon(String name, int size, Color color) {
        FlatSVGIcon icon = dashboardIcon(name, size);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return icon;
    }

    private static FlatSVGIcon dashboardIcon(String name, int width, int height, Color color) {
        FlatSVGIcon icon = dashboardIcon(name, width, height);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return icon;
    }

    private static Color mix(Color a, Color b, float t) {
        if (a == null) return b;
        if (b == null) return a;
        float k = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * k);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, bl)));
    }

    private static Color accentColorOrDefault() {
        Color c = UIManager.getColor("Component.accentColor");
        return c != null ? c : COLOR_PRIMARIO;
    }

    private int contentWidth() {
        try {
            if (mainScrollPane != null && mainScrollPane.getViewport() != null) {
                int w = mainScrollPane.getViewport().getWidth();
                if (w > 0) {
                    return w;
                }
            }
        } catch (Exception ignore) {
        }
        return Math.max(0, getWidth());
    }

    private int computeColumns(int minItemWidth, int maxCols, int fallback) {
        int w = contentWidth();
        if (w <= 0) {
            return fallback;
        }
        int cols = (int) Math.floor((double) w / (double) Math.max(1, minItemWidth));
        return Math.max(1, Math.min(maxCols, cols));
    }

    private void scheduleResponsiveUpdate() {
        if (responsiveTimer == null) {
            responsiveTimer = new javax.swing.Timer(150, e -> actualizarLayoutResponsive());
            responsiveTimer.setRepeats(false);
        }
        responsiveTimer.restart();
    }

    private static float luminance(Color c) {
        if (c == null) {
            return 0f;
        }
        return (0.2126f * c.getRed() + 0.7152f * c.getGreen() + 0.0722f * c.getBlue()) / 255f;
    }

    private static Color readableTextColor(Color background) {
        return luminance(background) > 0.65f ? new Color(15, 23, 42) : Color.WHITE;
    }

    private static Icon rankMedalIcon(int rank, int size) {
        Color medalColor = switch (rank) {
            case 1 -> new Color(245, 158, 11);
            case 2 -> new Color(148, 163, 184);
            case 3 -> new Color(217, 119, 6);
            default -> new Color(100, 116, 139);
        };
        FlatSVGIcon base = dashboardIcon("award.svg", size, medalColor);
        return new TextOverlayIcon(base, String.valueOf(rank), readableTextColor(medalColor));
    }

    private static final class TextOverlayIcon implements Icon {
        private final Icon base;
        private final String text;
        private final Color textColor;

        private TextOverlayIcon(Icon base, String text, Color textColor) {
            this.base = base;
            this.text = text;
            this.textColor = textColor;
        }

        @Override
        public int getIconWidth() {
            return base.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return base.getIconHeight();
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            base.paintIcon(c, g, x, y);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getIconWidth();
                int h = getIconHeight();

                Font baseFont = UIManager.getFont("Label.font");
                float fontSize = Math.max(10f, h * 0.62f);
                Font font = baseFont != null ? baseFont.deriveFont(Font.BOLD, fontSize)
                        : new Font("SansSerif", Font.BOLD, Math.round(fontSize));
                g2.setFont(font);

                var fm = g2.getFontMetrics();
                int textW = fm.stringWidth(text);
                int tx = x + (w - textW) / 2;
                int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();

                Color outline = textColor.equals(Color.WHITE) ? new Color(0, 0, 0, 160) : new Color(255, 255, 255, 160);
                g2.setColor(outline);
                g2.drawString(text, tx + 1, ty + 1);
                g2.drawString(text, tx - 1, ty + 1);
                g2.drawString(text, tx + 1, ty - 1);
                g2.drawString(text, tx - 1, ty - 1);

                g2.setColor(textColor);
                g2.drawString(text, tx, ty);
            } finally {
                g2.dispose();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    public FormDashboard() {
        // IMPORTANTE: Primero inicializar usuario, DESPUÉS construir UI
        initUserSession();
        initComponents();
        configurarFiltros();
        agregarListenersFiltros(); // Listeners DESPUÉS de configurar
        inicializacionCompleta = true;
        cargarDatosAsync();
        iniciarVerificaciones();

        // Agregar listener para detectar cambios de tamaño
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scheduleResponsiveUpdate();
            }
        });
    }

    /**
     * Actualiza el layout según el tamaño actual de la ventana
     */
    private void actualizarLayoutResponsive() {
        int width = Math.max(getWidth(), contentWidth());
        isDesktop = width >= DESKTOP_WIDTH;
        isTablet = width >= MOBILE_WIDTH && width < DESKTOP_WIDTH;
        isMobile = width < MOBILE_WIDTH;

        applyResponsiveLayout();
        revalidate();
        repaint();
    }

    private void applyResponsiveLayout() {
        int w = contentWidth();

        if (mainContentPanel != null) {
            int margin = w < 700 ? 10 : w < 1100 ? 15 : 25;
            mainContentPanel.setBorder(new EmptyBorder(margin, margin, margin, margin));
        }

        if (panelHeader != null) {
            panelHeader.removeAll();
            if (w < 720) {
                panelHeader.setLayout(new BorderLayout(0, 10));
                panelHeader.add(headerLeftPanel, BorderLayout.NORTH);
                panelHeader.add(headerButtonsPanel, BorderLayout.SOUTH);
            } else {
                panelHeader.setLayout(new BorderLayout(20, 0));
                panelHeader.add(headerLeftPanel, BorderLayout.WEST);
                panelHeader.add(headerButtonsPanel, BorderLayout.EAST);
            }
        }

        if (panelFiltros != null && lblPeriodo != null && lblBodega != null && lblLimite != null) {
            panelFiltros.removeAll();
            panelFiltros.setLayout(new GridBagLayout());

            int iconSize = w < 700 ? 14 : 16;
            lblPeriodo.setIcon(dashboardIcon("calendar.svg", iconSize, COLOR_PRIMARIO));
            lblBodega.setIcon(dashboardIcon("warehouse.svg", iconSize, new Color(100, 116, 139)));
            lblLimite.setIcon(dashboardIcon("bar-chart.svg", iconSize, COLOR_SECUNDARIO));

            int vgap = w < 700 ? 8 : 0;
            int hgap = w < 700 ? 10 : 12;

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.insets = new Insets(0, 0, vgap, hgap);
            gbc.anchor = GridBagConstraints.LINE_START;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            if (w < 720) {
                addFiltroFila(panelFiltros, gbc, 0, lblPeriodo, cmbPeriodo, true);
                addFiltroFila(panelFiltros, gbc, 1, lblBodega, cmbBodega, true);
                addFiltroFila(panelFiltros, gbc, 2, lblLimite, spnLimiteProductos, false);
            } else {
                gbc.gridy = 0;

                gbc.gridx = 0;
                gbc.weightx = 0;
                panelFiltros.add(lblPeriodo, gbc);
                gbc.gridx = 1;
                gbc.weightx = 0.33;
                panelFiltros.add(cmbPeriodo, gbc);

                gbc.gridx = 2;
                gbc.weightx = 0;
                panelFiltros.add(lblBodega, gbc);
                gbc.gridx = 3;
                gbc.weightx = 0.67;
                panelFiltros.add(cmbBodega, gbc);

                gbc.gridx = 4;
                gbc.weightx = 0;
                gbc.insets = new Insets(0, 0, 0, hgap);
                panelFiltros.add(lblLimite, gbc);
                gbc.gridx = 5;
                gbc.weightx = 0;
                gbc.insets = new Insets(0, 0, 0, 0);
                panelFiltros.add(spnLimiteProductos, gbc);
            }

            panelFiltros.revalidate();
            panelFiltros.repaint();
        }

        if (panelKPIs != null && panelKPIs.getLayout() instanceof GridLayout gl) {
            int cols = computeColumns(280, 3, 3);
            int gap = cols == 1 ? 12 : 20;
            gl.setRows(0);
            gl.setColumns(cols);
            gl.setHgap(gap);
            gl.setVgap(gap);
            panelKPIs.revalidate();
        }

        if (panelGraficos1 != null && panelGraficos1.getLayout() instanceof GridLayout gl) {
            int cols = computeColumns(360, 3, isMobile ? 1 : 3);
            int gap = cols == 1 ? 15 : 20;
            gl.setRows(0);
            gl.setColumns(cols);
            gl.setHgap(gap);
            gl.setVgap(gap);
            panelGraficos1.revalidate();
        }

        if (panelGraficos2 != null && panelGraficos2.getLayout() instanceof GridLayout gl) {
            int cols = computeColumns(460, 2, isMobile ? 1 : 2);
            int gap = cols == 1 ? 15 : 20;
            gl.setRows(0);
            gl.setColumns(cols);
            gl.setHgap(gap);
            gl.setVgap(gap);
            panelGraficos2.revalidate();
        }

        if (mainContentPanel != null) {
            for (java.awt.Component c : mainContentPanel.getComponents()) {
                if (c instanceof ResponsiveVGap gap) {
                    gap.apply(w);
                }
            }
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
        }
    }

    private void addFiltroFila(JPanel panel, GridBagConstraints base, int row, JLabel label, java.awt.Component field, boolean growField) {
        GridBagConstraints gbc = (GridBagConstraints) base.clone();
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = growField ? 1 : 0;
        gbc.insets = new Insets(base.insets.top, base.insets.left, base.insets.bottom, 0);
        panel.add(field, gbc);
    }

    private static final class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        private ScrollablePanel() {
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 32, 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class ResponsiveVGap extends Box.Filler {
        private final int mobile;
        private final int tablet;
        private final int desktop;

        private ResponsiveVGap(int mobile, int tablet, int desktop) {
            super(new Dimension(0, desktop), new Dimension(0, desktop), new Dimension(Short.MAX_VALUE, desktop));
            this.mobile = mobile;
            this.tablet = tablet;
            this.desktop = desktop;
        }

        private void apply(int width) {
            int h = width < 700 ? mobile : width < 1100 ? tablet : desktop;
            Dimension d = new Dimension(0, h);
            changeShape(d, d, new Dimension(Short.MAX_VALUE, h));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SESIÓN DE USUARIO (PRIMERO)
    // ═══════════════════════════════════════════════════════════════════════════
    private void initUserSession() {
        try {
            currentUser = session.getCurrentUser();
            Integer idBodegaUsuario = session.getIdBodegaUsuario();

            if (currentUser != null) {
                String rol = currentUser.getRol();
                System.out.println("Buscar [DASHBOARD] Usuario: " + currentUser.getNombre() +
                        " | Rol: '" + rol + "' | IdBodega (session): " + idBodegaUsuario);

                // ADMIN = ve TODAS las bodegas | GERENTE/OTROS = ve solo su bodega
                esAdmin = rol != null && (rol.equalsIgnoreCase("admin") ||
                        rol.equalsIgnoreCase("administrador") ||
                        rol.toLowerCase().contains("admin"));

                // Si es ADMIN: ver todas (null), si no: filtrar por bodega del usuario
                idBodegaFiltro = esAdmin ? null : idBodegaUsuario;

                System.out.println("Buscar [DASHBOARD] esAdmin=" + esAdmin + " | idBodegaFiltro=" + idBodegaFiltro);
            } else {
                System.out.println("WARNING  [DASHBOARD] No hay usuario en sesión");
                esAdmin = false;
                idBodegaFiltro = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inicializando sesión", e);
            esAdmin = false;
            idBodegaFiltro = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN DE COMPONENTES
    // ═══════════════════════════════════════════════════════════════════════════
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        // Panel principal con scroll
        mainContentPanel = new ScrollablePanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setOpaque(false);

        // Header con gradiente
        mainContentPanel.add(crearPanelHeader());
        mainContentPanel.add(new ResponsiveVGap(10, 15, 20));

        // Filtros avanzados
        mainContentPanel.add(crearPanelFiltros());
        mainContentPanel.add(new ResponsiveVGap(15, 20, 25));

        // KPIs con gradientes
        mainContentPanel.add(crearPanelKPIs());
        mainContentPanel.add(new ResponsiveVGap(15, 20, 25));

        // Gráficos principales
        mainContentPanel.add(crearFilaGraficos1());
        mainContentPanel.add(new ResponsiveVGap(15, 20, 25));

        // Gráficos de productos
        mainContentPanel.add(crearFilaGraficos2());
        mainContentPanel.add(new ResponsiveVGap(15, 20, 25));

        // Panel de Stock Bajo
        mainContentPanel.add(crearPanelStockBajo());
        mainContentPanel.add(new ResponsiveVGap(15, 20, 25));

        // Ranking vendedores
        mainContentPanel.add(crearPanelVendedores());

        // Scroll suave
        mainScrollPane = new JScrollPane(mainContentPanel);
        mainScrollPane.setBorder(null);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        mainScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scheduleResponsiveUpdate();
            }
        });

        // Progress bar elegante
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:$Panel.background");

        // ═══════════════════════════════════════════════════════════════════════
        // LOADING OVERLAY CON EFECTO BLUR
        // ═══════════════════════════════════════════════════════════════════════
        loadingOverlay = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fondo semi-transparente oscuro (efecto blur simulado)
                g2.setColor(new Color(15, 23, 42, 220));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        loadingOverlay.setOpaque(false);

        // Panel central de carga
        JPanel centerLoadingPanel = new JPanel();
        centerLoadingPanel.setLayout(new BoxLayout(centerLoadingPanel, BoxLayout.Y_AXIS));
        centerLoadingPanel.setOpaque(false);
        int loadingMargin = isMobile ? 20 : isTablet ? 30 : 40;
        centerLoadingPanel.setBorder(new EmptyBorder(loadingMargin, loadingMargin, loadingMargin, loadingMargin));

        // Spinner animado con iconos SVG
        lblLoadingSpinner = new JLabel();
        int spinnerSize = isMobile ? 48 : 64;
        FlatSVGIcon loadingIcon = dashboardIcon("activity.svg", spinnerSize, COLOR_SECUNDARIO);
        lblLoadingSpinner.setIcon(loadingIcon);
        lblLoadingSpinner.setAlignmentX(CENTER_ALIGNMENT);

        // Texto de carga
        lblLoadingText = new JLabel("Cargando Dashboard...");
        int fontSize = isMobile ? 16 : 20;
        lblLoadingText.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        lblLoadingText.setForeground(Color.WHITE);
        lblLoadingText.setAlignmentX(CENTER_ALIGNMENT);

        // Subtitulo
        JLabel lblLoadingSubtext = new JLabel("Preparando estadísticas y gráficos");
        int subtextSize = isMobile ? 12 : 14;
        lblLoadingSubtext.setFont(new Font("Segoe UI", Font.PLAIN, subtextSize));
        lblLoadingSubtext.setForeground(new Color(148, 163, 184));
        lblLoadingSubtext.setAlignmentX(CENTER_ALIGNMENT);

        // Progress bar dentro del overlay
        JProgressBar overlayProgress = new JProgressBar();
        overlayProgress.setIndeterminate(true);
        overlayProgress.setMaximumSize(new Dimension(250, 6));
        overlayProgress.setAlignmentX(CENTER_ALIGNMENT);
        overlayProgress.putClientProperty(FlatClientProperties.STYLE,
                "arc:3;background:#64748B4D");

        centerLoadingPanel.add(lblLoadingSpinner);
        centerLoadingPanel.add(Box.createVerticalStrut(isMobile ? 10 : 20));
        centerLoadingPanel.add(lblLoadingText);
        centerLoadingPanel.add(Box.createVerticalStrut(isMobile ? 4 : 8));
        centerLoadingPanel.add(lblLoadingSubtext);
        centerLoadingPanel.add(Box.createVerticalStrut(isMobile ? 15 : 25));
        centerLoadingPanel.add(overlayProgress);

        // Panel con estilo de tarjeta
        JPanel loadingCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fondo con gradiente oscuro
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(30, 41, 59),
                        getWidth(), getHeight(), new Color(15, 23, 42));
                g2.setPaint(gp);
                int cornerRadius = isMobile ? 12 : 24;
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                // Borde sutil
                g2.setColor(new Color(51, 65, 85));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
                g2.dispose();
            }
        };
        loadingCard.setOpaque(false);
        loadingCard.add(centerLoadingPanel, BorderLayout.CENTER);

        // Wrapper para centrar
        JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(loadingCard);

        loadingOverlay.add(wrapperPanel, BorderLayout.CENTER);
        loadingOverlay.setVisible(true); // Empezar visible

        // Animación del spinner
        String[] spinnerIcons = {
                DASHBOARD_ICON_PATH + "activity.svg",
                DASHBOARD_ICON_PATH + "refresh.svg",
                DASHBOARD_ICON_PATH + "bar-chart.svg",
                DASHBOARD_ICON_PATH + "trending-up.svg",
                DASHBOARD_ICON_PATH + "dollar.svg",
                DASHBOARD_ICON_PATH + "package.svg"
        };
        final int[] frameIndex = { 0 };
        spinnerTimer = new javax.swing.Timer(300, e -> {
            frameIndex[0] = (frameIndex[0] + 1) % spinnerIcons.length;
            int iconSize = isMobile ? 48 : 64;
            FlatSVGIcon icon = new FlatSVGIcon(spinnerIcons[frameIndex[0]], iconSize, iconSize);
            lblLoadingSpinner.setIcon(icon);
        });
        spinnerTimer.start();

        // Usar JLayeredPane para superponer
        javax.swing.JLayeredPane layeredPane = new javax.swing.JLayeredPane() {
            @Override
            public void doLayout() {
                // Hacer que ambos paneles ocupen todo el espacio
                for (java.awt.Component c : getComponents()) {
                    c.setBounds(0, 0, getWidth(), getHeight());
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return mainScrollPane.getPreferredSize();
            }
        };

        layeredPane.add(mainScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(loadingOverlay, javax.swing.JLayeredPane.MODAL_LAYER);

        add(layeredPane, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        applyResponsiveLayout();
    }

    private JPanel crearPanelHeader() {
        panelHeader = new JPanel(new BorderLayout(20, 0));
        panelHeader.setOpaque(false);
        panelHeader.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Icono y título
        lblTitulo = new JLabel();
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +12");
        lblTitulo.setIcon(dashboardIcon("dashboard.svg", 24, accentColorOrDefault()));
        lblTitulo.setText(" Dashboard Ejecutivo");
        lblTitulo.setIconTextGap(10);

        // Subtítulo con info de empresa
        JLabel lblSubtitulo = new JLabel(AppConfig.name + " - Control de Gestión");
        lblSubtitulo.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground;font:plain +2");

        // Info usuario
        lblUsuarioInfo = new JLabel("Cargando...");
        lblUsuarioInfo.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Component.accentColor;font:bold");
        if (currentUser != null) {
            lblUsuarioInfo.setIcon(dashboardIcon("user.svg", 16, accentColorOrDefault()));
            lblUsuarioInfo.setText(String.format(" %s | %s | %s",
                    currentUser.getNombre(),
                    currentUser.getRol().toUpperCase(),
                    esAdmin ? "Todas las bodegas" : "Bodega #" + idBodegaFiltro));
            lblUsuarioInfo.setIconTextGap(5);
        }

        // Panel izquierdo
        headerLeftPanel = new JPanel();
        headerLeftPanel.setLayout(new BoxLayout(headerLeftPanel, BoxLayout.Y_AXIS));
        headerLeftPanel.setOpaque(false);
        headerLeftPanel.add(lblTitulo);
        headerLeftPanel.add(Box.createVerticalStrut(5));
        headerLeftPanel.add(lblSubtitulo);
        headerLeftPanel.add(Box.createVerticalStrut(5));
        headerLeftPanel.add(lblUsuarioInfo);

        // Botones de exportación con estilo premium
        headerButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerButtonsPanel.setOpaque(false);

        JButton btnExportPDF = crearBotonExport("Exportar PDF", COLOR_NEGATIVO);
        btnExportPDF.setIcon(dashboardIcon("file-text.svg", 16, Color.WHITE));
        btnExportPDF.addActionListener(e -> exportarPDF());

        JButton btnExportExcel = crearBotonExport("Exportar Excel", COLOR_POSITIVO);
        btnExportExcel.setIcon(dashboardIcon("sheet.svg", 16, Color.WHITE));
        btnExportExcel.addActionListener(e -> exportarExcel());

        JButton btnRefresh = crearBotonExport("Actualizar", COLOR_PRIMARIO);
        btnRefresh.setIcon(dashboardIcon("refresh.svg", 16, Color.WHITE));
        btnRefresh.addActionListener(e -> cargarDatosAsync());

        headerButtonsPanel.add(btnExportPDF);
        headerButtonsPanel.add(btnExportExcel);
        headerButtonsPanel.add(btnRefresh);

        panelHeader.add(headerLeftPanel, BorderLayout.WEST);
        panelHeader.add(headerButtonsPanel, BorderLayout.EAST);

        return panelHeader;
    }

    private JButton crearBotonExport(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.putClientProperty(FlatClientProperties.STYLE, String.format(
                "arc:12;" +
                        "background:#%02x%02x%02x;" +
                        "foreground:#ffffff;" +
                        "font:bold;" +
                        "hoverBackground:lighten(#%02x%02x%02x,10%%);" +
                        "pressedBackground:darken(#%02x%02x%02x,10%%)",
                color.getRed(), color.getGreen(), color.getBlue(),
                color.getRed(), color.getGreen(), color.getBlue(),
                color.getRed(), color.getGreen(), color.getBlue()));
        return btn;
    }

    private JPanel crearPanelFiltros() {
        int cornerRadius = 15;
        int padding = 10;
        int hPadding = 20;

        panelFiltros = new JPanel();
        panelFiltros.setOpaque(false);
        panelFiltros.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + cornerRadius + ";background:$Panel.background;border:1,1,1,1,$Component.borderColor");
        panelFiltros.setBorder(BorderFactory.createCompoundBorder(
                panelFiltros.getBorder(),
                new EmptyBorder(padding, hPadding, padding, hPadding)));

        lblPeriodo = new JLabel("Período:");
        lblPeriodo.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        cmbPeriodo = new JComboBox<>(new String[] {
                "Hoy", "Última semana", "Último mes", "Último trimestre", "Último año"
        });
        cmbPeriodo.setSelectedIndex(2);
        cmbPeriodo.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        cmbPeriodo.setPrototypeDisplayValue("Último trimestre");

        lblBodega = new JLabel("Bodega:");
        lblBodega.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        cmbBodega = new JComboBox<>();
        cmbBodega.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        cmbBodega.setPrototypeDisplayValue("Resumen Todas las bodegas");

        lblLimite = new JLabel("Top Productos:");
        lblLimite.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        spnLimiteProductos = new JSpinner(new SpinnerNumberModel(10, 5, 50, 5));
        spnLimiteProductos.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        try {
            Object editor = spnLimiteProductos.getEditor();
            if (editor instanceof JSpinner.DefaultEditor de) {
                de.getTextField().setColumns(3);
            }
        } catch (Exception ignore) {
        }

        applyResponsiveLayout();
        return panelFiltros;
    }

    private void configurarFiltros() {
        try {
            // Limpiar combo sin disparar eventos
            cmbBodega.removeAllItems();

            System.out.println("Buscar [DASHBOARD] configurarFiltros: esAdmin=" + esAdmin);

            if (esAdmin) {
                cmbBodega.addItem("Resumen Todas las bodegas");
                ServiceBodegas serviceBodegas = new ServiceBodegas();
                List<ModelBodegas> bodegas = serviceBodegas.obtenerTodas();
                for (ModelBodegas b : bodegas) {
                    if (b.getActiva() != null && b.getActiva()) {
                        cmbBodega.addItem(b);
                    }
                }
                cmbBodega.setEnabled(true);
            } else {
                cmbBodega.addItem(" Mi bodega asignada");
                cmbBodega.setEnabled(false);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error configurando filtros", e);
            cmbBodega.addItem("Error cargando bodegas");
        }
    }

    private void agregarListenersFiltros() {
        // Agregar listeners DESPUÉS de configurar los combos
        cmbPeriodo.addActionListener(e -> {
            if (inicializacionCompleta)
                onPeriodoChange();
        });

        cmbBodega.addActionListener(e -> {
            if (inicializacionCompleta)
                onBodegaChange();
        });

        spnLimiteProductos.addChangeListener(e -> {
            if (inicializacionCompleta) {
                limiteProductos = (int) spnLimiteProductos.getValue();
                cargarDatosAsync();
            }
        });
    }

    private JPanel crearPanelKPIs() {
        int cols = computeColumns(280, 3, isMobile ? 2 : 3);
        int gap = cols == 1 ? 12 : 20;

        panelKPIs = new JPanel(new GridLayout(0, cols, gap, gap));
        panelKPIs.setOpaque(false);

        // Crear KPIs con gradientes
        panelKPIs.add(crearTarjetaKPIGradiente("VENTAS TOTALES",
                lblVentasTotales = new JLabel("$0"),
                lblVentasVariacion = new JLabel("0%"), 0));

        panelKPIs.add(crearTarjetaKPIGradiente("GANANCIA NETA",
                lblGananciaNeta = new JLabel("$0"),
                lblGananciaVariacion = new JLabel("0%"), 1));

        panelKPIs.add(crearTarjetaKPIGradiente("TRANSACCIONES",
                lblTransacciones = new JLabel("0"),
                lblTransaccionesVariacion = new JLabel("0%"), 2));

        panelKPIs.add(crearTarjetaKPIGradiente("TICKET PROMEDIO",
                lblTicketPromedio = new JLabel("$0"),
                lblTicketVariacion = new JLabel("0%"), 3));

        panelKPIs.add(crearTarjetaKPIGradiente("GASTOS OPERATIVOS",
                lblGastosOperativos = new JLabel("$0"),
                null, 4));

        panelKPIs.add(crearTarjetaKPIGradiente("BALANCE NETO",
                lblBalanceNeto = new JLabel("$0"),
                null, 5));

        return panelKPIs;
    }

    private JPanel crearTarjetaKPIGradiente(String titulo, JLabel lblValor, JLabel lblVariacion, int colorIndex) {
        Color[] gradiente = GRADIENTES_KPI[colorIndex % GRADIENTES_KPI.length];

        // Ajustar bordes según el tamaño de pantalla
        int borderPadding = isMobile ? 12 : isTablet ? 18 : 20;
        int gap = isMobile ? 6 : 10;
        int cornerRadius = isMobile ? 12 : 20;

        JPanel card = new JPanel(new BorderLayout(gap, gap)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, 0, gradiente[0],
                        getWidth(), getHeight(), gradiente[1]);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));

        // Crear un panel para el título con icono
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        tituloPanel.setOpaque(false);

        // Asignar iconos según el tipo de KPI
        FlatSVGIcon icon = null;
        Color iconColor = mix(gradiente[0], Color.WHITE, 0.60f);
        int iconSize = isMobile ? 16 : 20;
        if (titulo.contains("VENTAS")) {
            icon = dashboardIcon("dollar.svg", iconSize, iconColor);
        } else if (titulo.contains("GANANCIA")) {
            icon = dashboardIcon("trending-up.svg", iconSize, iconColor);
        } else if (titulo.contains("TRANSACCIONES")) {
            icon = dashboardIcon("activity.svg", iconSize, iconColor);
        } else if (titulo.contains("TICKET")) {
            icon = dashboardIcon("tag.svg", iconSize, iconColor);
        } else if (titulo.contains("GASTOS")) {
            icon = dashboardIcon("trending-down.svg", iconSize, iconColor);
        } else if (titulo.contains("BALANCE")) {
            icon = dashboardIcon("scale.svg", iconSize, iconColor);
        }

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(new Color(255, 255, 255, 200));
        int tituloFontSize = isMobile ? 10 : 12;
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, tituloFontSize));

        if (icon != null) {
            lblTitulo.setIcon(icon);
            lblTitulo.setIconTextGap(gap);
        }

        tituloPanel.add(lblTitulo);

        // Ajustar tamaño de fuente del valor según el tamaño de pantalla
        int valorFontSize = isMobile ? 24 : isTablet ? 28 : 32;
        lblValor.setForeground(Color.WHITE);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, valorFontSize));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(tituloPanel, BorderLayout.WEST);

        if (lblVariacion != null) {
            lblVariacion.setForeground(Color.WHITE);
            int variacionFontSize = isMobile ? 12 : 14;
            lblVariacion.setFont(new Font("Segoe UI", Font.BOLD, variacionFontSize));
            topPanel.add(lblVariacion, BorderLayout.EAST);
        }

        card.add(topPanel, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);

        return card;
    }

    private JPanel crearFilaGraficos1() {
        int cols = computeColumns(360, 3, isMobile ? 1 : 3);
        int gap = cols == 1 ? 15 : 20;

        panelGraficos1 = new JPanel(new GridLayout(0, cols, gap, gap));
        panelGraficos1.setOpaque(false);

        // Pie: Ventas por categoría
        pieCategoria = new PieChart();
        int headerSize = isMobile ? 20 : 24;
        JLabel headerCat = new JLabel("Ventas por Categoría");
        headerCat.setIcon(dashboardIcon("package.svg", headerSize, COLOR_PRIMARIO));
        headerCat.setIconTextGap(isMobile ? 6 : 10);
        headerCat.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        pieCategoria.setHeader(headerCat);
        panelGraficos1.add(wrapChartProfesional(pieCategoria, "Distribución por categorías de producto"));

        // Pie: Ventas por método de pago
        pieMetodoPago = new PieChart();
        JLabel headerPago = new JLabel("Métodos de Pago");
        headerPago.setIcon(dashboardIcon("credit-card.svg", headerSize, new Color(6, 182, 212)));
        headerPago.setIconTextGap(isMobile ? 6 : 10);
        headerPago.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        pieMetodoPago.setHeader(headerPago);
        panelGraficos1.add(wrapChartProfesional(pieMetodoPago, "Distribución por forma de pago"));

        // Línea: Tendencia
        lineTendencia = new LineChart();
        JLabel headerTend = new JLabel("Tendencia de Ventas");
        headerTend.setIcon(dashboardIcon("trending-up.svg", headerSize, COLOR_POSITIVO));
        headerTend.setIconTextGap(isMobile ? 6 : 10);
        headerTend.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        lineTendencia.setHeader(headerTend);
        panelGraficos1.add(wrapChartProfesional(lineTendencia, "Evolución histórica de ventas"));

        return panelGraficos1;
    }

    private JPanel crearFilaGraficos2() {
        int cols = computeColumns(460, 2, isMobile ? 1 : 2);
        int gap = cols == 1 ? 15 : 20;

        panelGraficos2 = new JPanel(new GridLayout(0, cols, gap, gap));
        panelGraficos2.setOpaque(false);

        // Bar: Top productos
        barProductosTop = new HorizontalBarChart();
        barProductosTop.setBarColor(COLOR_PRIMARIO);
        int headerSize = isMobile ? 20 : 24;
        JLabel headerTop = new JLabel("Top Productos Más Vendidos");
        headerTop.setIcon(dashboardIcon("award.svg", headerSize, new Color(245, 158, 11)));
        headerTop.setIconTextGap(isMobile ? 6 : 10);
        headerTop.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        barProductosTop.setHeader(headerTop);
        panelGraficos2.add(wrapChartProfesional(barProductosTop, "Productos con mayor volumen de ventas"));

        // Bar: Productos menos vendidos
        barProductosMenos = new HorizontalBarChart();
        barProductosMenos.setBarColor(COLOR_ADVERTENCIA);
        JLabel headerMenos = new JLabel("Productos con Bajo Rendimiento");
        headerMenos.setIcon(dashboardIcon("trending-down.svg", headerSize, COLOR_ADVERTENCIA));
        headerMenos.setIconTextGap(isMobile ? 6 : 10);
        headerMenos.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        barProductosMenos.setHeader(headerMenos);
        panelGraficos2.add(wrapChartProfesional(barProductosMenos, "Productos que necesitan atención"));

        return panelGraficos2;
    }

    private JPanel wrapChartProfesional(JComponent chart, String descripcion) {
        // Ajustar espaciado según el tamaño de pantalla
        int headerGap = isMobile ? 8 : 10;
        int cornerRadius = isMobile ? 12 : 15;
        int padding = isMobile ? 12 : 20;

        JPanel wrapper = new JPanel(new BorderLayout(0, headerGap));
        wrapper.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + cornerRadius + ";background:$Panel.background;border:1,1,1,1,$Component.borderColor");
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                wrapper.getBorder(),
                new EmptyBorder(padding, padding, padding, padding)));

        JLabel lblDesc = new JLabel(descripcion);
        lblDesc.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground;font:italic");

        // Ajustar tamaño de fuente según el tamaño de pantalla
        if (isMobile) {
            lblDesc.setFont(lblDesc.getFont().deriveFont(10f));
        }

        wrapper.add(chart, BorderLayout.CENTER);
        wrapper.add(lblDesc, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel crearPanelStockBajo() {
        // Ajustar espaciado según el tamaño de pantalla
        int headerGap = isMobile ? 10 : 15;
        int cornerRadius = isMobile ? 12 : 15;
        int padding = isMobile ? 12 : 20;

        JPanel panel = new JPanel(new BorderLayout(0, headerGap));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + cornerRadius + ";background:$Panel.background;border:1,1,1,1,$Component.borderColor");
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                new EmptyBorder(padding, padding, padding, padding)));

        // Ajustar tamaño máximo según el tamaño de pantalla
        int maxHeight = isMobile ? 300 : 350;
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxHeight));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        int headerIconSize = isMobile ? 24 : 28;
        JLabel lblHeader = new JLabel("Alertas de Stock Bajo");
        lblHeader.setIcon(dashboardIcon("alert-circle.svg", headerIconSize, COLOR_ADVERTENCIA));
        lblHeader.setIconTextGap(isMobile ? 6 : 10);
        int headerFontSize = isMobile ? 14 : 18;
        lblHeader.putClientProperty(FlatClientProperties.STYLE, "font:bold +" + (headerFontSize - 10));

        JLabel lblSubheader = new JLabel("Productos que necesitan reabastecimiento");
        lblSubheader.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground");

        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);
        leftHeader.add(lblHeader);
        leftHeader.add(lblSubheader);

        headerPanel.add(leftHeader, BorderLayout.WEST);

        // Tabla de stock bajo
        String[] columnas = { "Estado", "Producto", "Categoría", "Stock Actual", "Stock Mínimo", "Ubicación" };
        modeloStockBajo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaStockBajo = new JTable(modeloStockBajo);

        // Ajustar altura de fila según el tamaño de pantalla
        int rowHeight = isMobile ? 32 : 40;
        tablaStockBajo.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;showVerticalLines:false;rowHeight:" + rowHeight + ";" +
                        "selectionBackground:$Component.accentColor;selectionForeground:#ffffff");
        tablaStockBajo.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "font:bold;height:" + (isMobile ? 32 : 40) + ";background:$Panel.background");

        // Renderer para estado con badge de colores
        tablaStockBajo.getColumnModel().getColumn(0)
                .setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
                    JLabel lbl = new JLabel(value != null ? value.toString() : "", SwingConstants.CENTER);
                    lbl.setOpaque(true);
                    int fontSize = isMobile ? 9 : 11;
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, fontSize));

                    if (value != null) {
                        String estado = value.toString();
                        if (estado.equals("Sin Stock")) {
                            lbl.setBackground(new Color(239, 68, 68)); // Rojo
                            lbl.setForeground(Color.WHITE);
                            int iconSize = isMobile ? 10 : 12;
                            lbl.setIcon(dashboardIcon("x-circle.svg", iconSize, Color.WHITE));
                            lbl.setText(estado);
                            lbl.setIconTextGap(3);
                        } else if (estado.equals("Crítico")) {
                            lbl.setBackground(new Color(249, 115, 22)); // Naranja
                            lbl.setForeground(Color.WHITE);
                            int iconSize = isMobile ? 10 : 12;
                            lbl.setIcon(dashboardIcon("alert-circle.svg", iconSize, Color.WHITE));
                            lbl.setText(estado);
                            lbl.setIconTextGap(3);
                        } else if (estado.equals("Bajo")) {
                            lbl.setBackground(new Color(234, 179, 8)); // Amarillo
                            lbl.setForeground(Color.BLACK);
                            int iconSize = isMobile ? 10 : 12;
                            lbl.setIcon(dashboardIcon("minus-circle.svg", iconSize, Color.BLACK));
                            lbl.setText(estado);
                            lbl.setIconTextGap(3);
                        } else {
                            lbl.setBackground(new Color(34, 197, 94)); // Verde para OK
                            lbl.setForeground(Color.WHITE);
                            int iconSize = isMobile ? 10 : 12;
                            lbl.setIcon(dashboardIcon("check-circle.svg", iconSize, Color.WHITE));
                            lbl.setText(estado);
                            lbl.setIconTextGap(3);
                        }
                    }

                    if (isSelected) {
                        lbl.setBackground(table.getSelectionBackground());
                        lbl.setForeground(table.getSelectionForeground());
                    }
                    return lbl;
                });

        // Ajustar anchos de columna según el tamaño de pantalla
        int estadoWidth = isMobile ? 80 : 100;
        int productoWidth = isMobile ? 150 : 250;
        int categoriaWidth = isMobile ? 100 : 120;
        int stockWidth = isMobile ? 80 : 100;
        int ubicacionWidth = isMobile ? 100 : 120;

        tablaStockBajo.getColumnModel().getColumn(0).setPreferredWidth(estadoWidth);
        tablaStockBajo.getColumnModel().getColumn(1).setPreferredWidth(productoWidth);
        tablaStockBajo.getColumnModel().getColumn(2).setPreferredWidth(categoriaWidth);
        tablaStockBajo.getColumnModel().getColumn(3).setPreferredWidth(stockWidth);
        tablaStockBajo.getColumnModel().getColumn(4).setPreferredWidth(stockWidth);
        tablaStockBajo.getColumnModel().getColumn(5).setPreferredWidth(ubicacionWidth);

        JScrollPane scrollTabla = new JScrollPane(tablaStockBajo);
        scrollTabla.setBorder(null);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollTabla, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelVendedores() {
        // Ajustar espaciado según el tamaño de pantalla
        int headerGap = isMobile ? 10 : 15;
        int cornerRadius = isMobile ? 12 : 15;
        int padding = isMobile ? 12 : 20;

        JPanel panel = new JPanel(new BorderLayout(0, headerGap));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + cornerRadius + ";background:$Panel.background;border:1,1,1,1,$Component.borderColor");
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                new EmptyBorder(padding, padding, padding, padding)));

        // Ajustar tamaño máximo según el tamaño de pantalla
        int maxHeight = isMobile ? 350 : 400;
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxHeight));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        int headerIconSize = isMobile ? 24 : 28;
        JLabel lblHeader = new JLabel("Ranking de Vendedores");
        lblHeader.setIcon(dashboardIcon("users.svg", headerIconSize, COLOR_PRIMARIO));
        lblHeader.setIconTextGap(isMobile ? 6 : 10);
        int headerFontSize = isMobile ? 14 : 18;
        lblHeader.putClientProperty(FlatClientProperties.STYLE, "font:bold +" + (headerFontSize - 10));

        JLabel lblSubheader = new JLabel("Desempeño del equipo de ventas");
        lblSubheader.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground");

        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);
        leftHeader.add(lblHeader);
        leftHeader.add(lblSubheader);

        // Ajustar botones según el tamaño de pantalla
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, isMobile ? 5 : 10, 0));
        btnPanel.setOpaque(false);

        JButton btnOrdenDesc = new JButton(isMobile ? "↓ Mayor" : "↓ Mayor a Menor");
        btnOrdenDesc.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnOrdenDesc.addActionListener(e -> ordenarVendedores(false));

        JButton btnOrdenAsc = new JButton(isMobile ? "↑ Menor" : "↑ Menor a Mayor");
        btnOrdenAsc.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnOrdenAsc.addActionListener(e -> ordenarVendedores(true));

        btnPanel.add(btnOrdenDesc);
        btnPanel.add(btnOrdenAsc);

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        // Tabla con estilo premium
        String[] columnas = { "#", "Vendedor", "Total Ventas", "Cantidad", "Ticket Promedio" };
        modeloVendedores = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaVendedores = new JTable(modeloVendedores);

        // Ajustar altura de fila según el tamaño de pantalla
        int rowHeight = isMobile ? 36 : 45;
        tablaVendedores.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;showVerticalLines:false;rowHeight:" + rowHeight + ";" +
                        "selectionBackground:$Component.accentColor;selectionForeground:#ffffff");
        tablaVendedores.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "font:bold;height:" + (isMobile ? 32 : 40) + ";background:$Panel.background");

        // Renderer para medallas
        tablaVendedores.getColumnModel().getColumn(0)
                .setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
                    JLabel lbl = new JLabel("", SwingConstants.CENTER);
                    int rank = row + 1;
                    int iconSize = isMobile ? 16 : 20;
                    lbl.setIcon(rankMedalIcon(rank, iconSize));
                    lbl.setToolTipText("Puesto " + rank);
                    lbl.setOpaque(true);
                    lbl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    lbl.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                    return lbl;
                });

        // Ajustar anchos de columna según el tamaño de pantalla
        int posicionWidth = isMobile ? 40 : 60;
        int vendedorWidth = isMobile ? 120 : 250;
        int totalVentasWidth = isMobile ? 100 : 150;
        int cantidadWidth = isMobile ? 70 : 100;
        int ticketWidth = isMobile ? 100 : 150;

        tablaVendedores.getColumnModel().getColumn(0).setPreferredWidth(posicionWidth);
        tablaVendedores.getColumnModel().getColumn(1).setPreferredWidth(vendedorWidth);
        tablaVendedores.getColumnModel().getColumn(2).setPreferredWidth(totalVentasWidth);
        tablaVendedores.getColumnModel().getColumn(3).setPreferredWidth(cantidadWidth);
        tablaVendedores.getColumnModel().getColumn(4).setPreferredWidth(ticketWidth);

        JScrollPane scrollTabla = new JScrollPane(tablaVendedores);
        scrollTabla.setBorder(null);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollTabla, BorderLayout.CENTER);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EVENTOS DE FILTROS
    // ═══════════════════════════════════════════════════════════════════════════
    private void onPeriodoChange() {
        String periodo = (String) cmbPeriodo.getSelectedItem();
        if (periodo == null)
            return;

        switch (periodo) {
            case "Hoy" -> diasFiltro = 1;
            case "Última semana" -> diasFiltro = 7;
            case "Último mes" -> diasFiltro = 30;
            case "Último trimestre" -> diasFiltro = 90;
            case "Último año" -> diasFiltro = 365;
            default -> diasFiltro = 30;
        }
        cargarDatosAsync();
    }

    private void onBodegaChange() {
        Object selected = cmbBodega.getSelectedItem();
        if (selected instanceof ModelBodegas bodega) {
            idBodegaFiltro = bodega.getIdBodega();
        } else {
            // "Todas las bodegas" o "Mi bodega"
            if (esAdmin) {
                idBodegaFiltro = null; // Ver todas
            } else if (currentUser != null) {
                idBodegaFiltro = currentUser.getIdBodega();
            } else {
                idBodegaFiltro = null;
            }
        }
        cargarDatosAsync();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGA ASÍNCRONA DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════
    private void cargarDatosAsync() {
        if (!inicializacionCompleta)
            return;

        showLoading(true);

        SwingWorker<Void, Runnable> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Crear usuario filtro temporal
                ModelUser usuarioFiltro = new ModelUser();
                if (currentUser != null) {
                    usuarioFiltro.setIdUsuario(currentUser.getIdUsuario());
                    usuarioFiltro.setNombre(currentUser.getNombre());
                    usuarioFiltro.setRol(currentUser.getRol());
                }
                usuarioFiltro.setIdBodega(idBodegaFiltro);

                // === CARGAR KPIs ===
                try {
                    double ventas = statsService.getVentasTotalesPeriodo(diasFiltro, usuarioFiltro);
                    double ganancia = statsService.getGananciaTotalPeriodo(diasFiltro, usuarioFiltro);
                    double gastos = statsService.getTotalGastosOperativos(diasFiltro, usuarioFiltro);
                    Map<String, Double> balance = statsService.getBalanceIngresosEgresos(diasFiltro, usuarioFiltro);

                    Map<String, Object> resumen = statsService.getResumenDashboard(diasFiltro, usuarioFiltro);
                    int transacciones = resumen.get("num_transacciones") != null
                            ? ((Number) resumen.get("num_transacciones")).intValue()
                            : 0;
                    double ticketPromedio = transacciones > 0 ? ventas / transacciones : 0;
                    double balanceNeto = balance.get("balance_neto") != null
                            ? balance.get("balance_neto")
                            : 0;

                    Map<String, Integer> variaciones = statsService.getComparacionPeriodoAnterior(
                            LocalDate.now().minusDays(diasFiltro), LocalDate.now(), usuarioFiltro);

                    publish(() -> actualizarKPIs(ventas, ganancia, transacciones, ticketPromedio,
                            gastos, balanceNeto, variaciones));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cargando KPIs", e);
                }

                // === CARGAR GRÁFICOS ===
                try {
                    DefaultPieDataset<String> dataCat = statsService.getVentasPorCategoria(usuarioFiltro);
                    DefaultPieDataset<String> dataPago = statsService.getVentasPorMetodoPago(diasFiltro, usuarioFiltro);
                    DefaultCategoryDataset<String, String> dataTendencia = statsService
                            .getVentasTendenciaSemanal(Math.max(diasFiltro / 7, 1), usuarioFiltro);
                    DefaultPieDataset<String> dataTop = statsService.getProductosMasVendidos(limiteProductos,
                            usuarioFiltro);
                    DefaultPieDataset<String> dataMenos = statsService.getProductosMenosVendidos(limiteProductos,
                            diasFiltro, usuarioFiltro);

                    publish(() -> {
                        if (dataCat != null)
                            pieCategoria.setDataset(dataCat);
                        if (dataPago != null)
                            pieMetodoPago.setDataset(dataPago);
                        if (dataTendencia != null)
                            lineTendencia.setCategoryDataset(dataTendencia);
                        if (dataTop != null)
                            barProductosTop.setDataset(dataTop);
                        if (dataMenos != null)
                            barProductosMenos.setDataset(dataMenos);
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cargando gráficos", e);
                }

                // === CARGAR RANKING ===
                try {
                    List<Map<String, Object>> ranking = statsService.getRankingVendedores(15, diasFiltro,
                            usuarioFiltro);
                    publish(() -> actualizarTablaVendedores(ranking));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cargando ranking", e);
                }

                // === CARGAR STOCK BAJO ===
                try {
                    List<Map<String, Object>> stockBajo = statsService.getAlertasInventario(15, usuarioFiltro);
                    publish(() -> actualizarTablaStockBajo(stockBajo));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cargando alertas de stock", e);
                }

                return null;
            }

            @Override
            protected void process(List<Runnable> chunks) {
                for (Runnable r : chunks)
                    r.run();
            }

            @Override
            protected void done() {
                try {
                    get();
                    animarGraficos();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error cargando dashboard", e);
                } finally {
                    showLoading(false);
                }
            }
        };
        worker.execute();
    }

    private void actualizarKPIs(double ventas, double ganancia, int transacciones,
            double ticketPromedio, double gastos, double balanceNeto, Map<String, Integer> variaciones) {

        lblVentasTotales.setText(FORMATO_DINERO.format(ventas));
        lblGananciaNeta.setText(FORMATO_DINERO.format(ganancia));
        lblTransacciones.setText(FORMATO_NUMERO.format(transacciones));
        lblTicketPromedio.setText(FORMATO_DINERO.format(ticketPromedio));
        lblGastosOperativos.setText(FORMATO_DINERO.format(gastos));
        lblBalanceNeto.setText(FORMATO_DINERO.format(balanceNeto));

        // Variaciones
        if (variaciones != null) {
            actualizarVariacion(lblVentasVariacion, variaciones.getOrDefault("ventas", 0));
            actualizarVariacion(lblGananciaVariacion, variaciones.getOrDefault("ganancia", 0));
            actualizarVariacion(lblTransaccionesVariacion, variaciones.getOrDefault("margen", 0));
            actualizarVariacion(lblTicketVariacion, variaciones.getOrDefault("rotacion", 0));
        }
    }

    private void actualizarVariacion(JLabel label, int porcentaje) {
        if (label == null)
            return;
        String simbolo = porcentaje >= 0 ? "▲ +" : "▼ ";
        label.setText(simbolo + Math.abs(porcentaje) + "%");
    }

    private void actualizarTablaVendedores(List<Map<String, Object>> ranking) {
        modeloVendedores.setRowCount(0);
        if (ranking == null)
            return;

        for (Map<String, Object> v : ranking) {
            Object totalVentas = v.get("total_ventas");
            Object numVentas = v.get("num_ventas");
            Object promedioVenta = v.get("promedio_venta");

            modeloVendedores.addRow(new Object[] {
                    v.get("posicion"),
                    v.get("nombre"),
                    totalVentas != null ? FORMATO_DINERO.format(((Number) totalVentas).doubleValue()) : "$0",
                    numVentas,
                    promedioVenta != null ? FORMATO_DINERO.format(((Number) promedioVenta).doubleValue()) : "$0"
            });
        }
    }

    private void actualizarTablaStockBajo(List<Map<String, Object>> alertas) {
        modeloStockBajo.setRowCount(0);
        if (alertas == null || alertas.isEmpty()) {
            // Mostrar mensaje si no hay alertas
            modeloStockBajo.addRow(new Object[] {
                    "OK", "No hay productos con stock bajo", "-", "-", "-", "-"
            });
            return;
        }

        for (Map<String, Object> alerta : alertas) {
            modeloStockBajo.addRow(new Object[] {
                    alerta.get("estado"),
                    alerta.get("nombre"),
                    alerta.get("categoria"),
                    alerta.get("stock_actual"),
                    alerta.get("stock_minimo"),
                    alerta.get("ubicacion") != null ? alerta.get("ubicacion") : "-"
            });
        }
    }

    private void ordenarVendedores(boolean ascendente) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modeloVendedores);
        tablaVendedores.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(2,
                ascendente ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
    }

    private void animarGraficos() {
        try {
            pieCategoria.startAnimation();
            pieMetodoPago.startAnimation();
            barProductosTop.startAnimation();
            barProductosMenos.startAnimation();
            lineTendencia.startAnimation();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error animando gráficos", e);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisible(show);
        setCursor(show ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());

        // Controlar overlay
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(show);
        }

        // Controlar animación del spinner
        if (spinnerTimer != null) {
            if (show) {
                if (!spinnerTimer.isRunning()) {
                    spinnerTimer.start();
                }
            } else {
                spinnerTimer.stop();
            }
        }

        // Actualizar texto de carga
        if (lblLoadingText != null) {
            lblLoadingText.setText(show ? "Cargando Dashboard..." : "¡Listo!");
            if (!show) {
                lblLoadingText.setIcon(dashboardIcon("check-circle.svg", 16, COLOR_POSITIVO));
                lblLoadingText.setIconTextGap(8);
            } else {
                lblLoadingText.setIcon(null);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORTACIÓN PDF
    // ═══════════════════════════════════════════════════════════════════════════
    private void exportarPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Dashboard como PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
        fileChooser.setSelectedFile(new File("Dashboard_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }

            try {
                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();

                // Título
                com.itextpdf.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
                Paragraph titulo = new Paragraph("Dashboard Ejecutivo - " + AppConfig.name, fontTitulo);
                titulo.setAlignment(Element.ALIGN_CENTER);
                titulo.setSpacingAfter(10);
                doc.add(titulo);

                com.itextpdf.text.Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12);
                Paragraph subtitulo = new Paragraph("Generado: " +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        " | Período: " + cmbPeriodo.getSelectedItem() +
                        " | Bodega: " + cmbBodega.getSelectedItem(), fontSubtitulo);
                subtitulo.setAlignment(Element.ALIGN_CENTER);
                subtitulo.setSpacingAfter(20);
                doc.add(subtitulo);

                // KPIs
                PdfPTable tablaKPIs = new PdfPTable(6);
                tablaKPIs.setWidthPercentage(100);
                tablaKPIs.setSpacingAfter(20);

                com.itextpdf.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10,
                        BaseColor.WHITE);
                com.itextpdf.text.Font fontValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

                String[] titles = { "Ventas", "Ganancia", "Transacciones", "Ticket Prom.", "Gastos", "Balance" };
                String[] values = {
                        lblVentasTotales.getText(), lblGananciaNeta.getText(),
                        lblTransacciones.getText(), lblTicketPromedio.getText(),
                        lblGastosOperativos.getText(), lblBalanceNeto.getText()
                };

                for (String t : titles) {
                    PdfPCell cell = new PdfPCell(new Phrase(t, fontHeader));
                    cell.setBackgroundColor(new BaseColor(99, 102, 241));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(10);
                    tablaKPIs.addCell(cell);
                }
                for (String v : values) {
                    PdfPCell cell = new PdfPCell(new Phrase(v, fontValue));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(12);
                    tablaKPIs.addCell(cell);
                }
                doc.add(tablaKPIs);

                // Vendedores
                Paragraph tituloVend = new Paragraph("Ranking de Vendedores",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
                tituloVend.setSpacingAfter(10);
                doc.add(tituloVend);

                PdfPTable tablaVend = new PdfPTable(5);
                tablaVend.setWidthPercentage(100);
                String[] cols = { "#", "Vendedor", "Total Ventas", "Cantidad", "Promedio" };
                for (String c : cols) {
                    PdfPCell cell = new PdfPCell(new Phrase(c, fontHeader));
                    cell.setBackgroundColor(new BaseColor(99, 102, 241));
                    cell.setPadding(8);
                    tablaVend.addCell(cell);
                }
                for (int i = 0; i < modeloVendedores.getRowCount(); i++) {
                    for (int j = 0; j < modeloVendedores.getColumnCount(); j++) {
                        Object val = modeloVendedores.getValueAt(i, j);
                        tablaVend.addCell(new PdfPCell(new Phrase(val != null ? val.toString() : "")));
                    }
                }
                doc.add(tablaVend);

                doc.close();
                Desktop.getDesktop().open(file);

                JOptionPane.showMessageDialog(this,
                        "PDF exportado exitosamente:\n" + file.getAbsolutePath(),
                        "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error exportando PDF", e);
                JOptionPane.showMessageDialog(this,
                        "ERROR  Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORTACIÓN EXCEL
    // ═══════════════════════════════════════════════════════════════════════════
    private void exportarExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Dashboard como Excel");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("Dashboard_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                // Hoja Resumen
                XSSFSheet sheetResumen = workbook.createSheet("Resumen Ejecutivo");

                XSSFCellStyle headerStyle = workbook.createCellStyle();
                XSSFFont headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                Row titleRow = sheetResumen.createRow(0);
                titleRow.createCell(0).setCellValue("Dashboard Ejecutivo - " + AppConfig.name);

                Row dateRow = sheetResumen.createRow(1);
                dateRow.createCell(0).setCellValue("Generado: " + LocalDate.now());
                dateRow.createCell(2).setCellValue("Período: " + cmbPeriodo.getSelectedItem());

                Row kpiHeaderRow = sheetResumen.createRow(3);
                String[] kpiTitles = { "Métrica", "Valor" };
                for (int i = 0; i < kpiTitles.length; i++) {
                    Cell cell = kpiHeaderRow.createCell(i);
                    cell.setCellValue(kpiTitles[i]);
                    cell.setCellStyle(headerStyle);
                }

                String[][] kpis = {
                        { "Ventas Totales", lblVentasTotales.getText() },
                        { "Ganancia Neta", lblGananciaNeta.getText() },
                        { "Transacciones", lblTransacciones.getText() },
                        { "Ticket Promedio", lblTicketPromedio.getText() },
                        { "Gastos Operativos", lblGastosOperativos.getText() },
                        { "Balance Neto", lblBalanceNeto.getText() }
                };
                for (int i = 0; i < kpis.length; i++) {
                    Row row = sheetResumen.createRow(4 + i);
                    row.createCell(0).setCellValue(kpis[i][0]);
                    row.createCell(1).setCellValue(kpis[i][1]);
                }

                // Hoja Vendedores
                XSSFSheet sheetVend = workbook.createSheet("Ranking Vendedores");
                Row vendHeaderRow = sheetVend.createRow(0);
                for (int i = 0; i < modeloVendedores.getColumnCount(); i++) {
                    Cell cell = vendHeaderRow.createCell(i);
                    cell.setCellValue(modeloVendedores.getColumnName(i));
                    cell.setCellStyle(headerStyle);
                }
                for (int i = 0; i < modeloVendedores.getRowCount(); i++) {
                    Row row = sheetVend.createRow(i + 1);
                    for (int j = 0; j < modeloVendedores.getColumnCount(); j++) {
                        Object val = modeloVendedores.getValueAt(i, j);
                        row.createCell(j).setCellValue(val != null ? val.toString() : "");
                    }
                }

                for (int i = 0; i < 6; i++)
                    sheetResumen.autoSizeColumn(i);
                for (int i = 0; i < 5; i++)
                    sheetVend.autoSizeColumn(i);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }

                Desktop.getDesktop().open(file);
                JOptionPane.showMessageDialog(this,
                        "Excel exportado exitosamente:\n" + file.getAbsolutePath(),
                        "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error exportando Excel", e);
                JOptionPane.showMessageDialog(this,
                        "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERIFICACIONES AUTOMÁTICAS
    // ═══════════════════════════════════════════════════════════════════════════
    private void iniciarVerificaciones() {
        SwingUtilities.invokeLater(() -> {
            verificarAlertas();
        });
    }

    private void verificarAlertas() {
        try {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            if (parentFrame == null)
                return;

            NotificacionesService service = new NotificacionesService(parentFrame);
            var ventasAntiguas = service.obtenerVentasAntiguas(10);

            if (!ventasAntiguas.isEmpty()) {
                new Notification(parentFrame, Notification.Type.WARNING,
                        Notification.Location.BOTTOM_RIGHT,
                        "¡Atención! " + ventasAntiguas.size() + " ventas pendientes con más de 10 días")
                        .showNotification();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error verificando alertas", e);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (timerVerificacion != null)
            timerVerificacion.stop();
    }
}
