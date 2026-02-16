package raven.application.form.productos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.awt.print.Paper;
import java.awt.GridLayout;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.NumberUp;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.*;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.clases.productos.ImpresorTermicaPOSDIG2406T;
import raven.clases.productos.ConfiguracionImpresoraXP420B_UNIVERSAL;
import raven.clases.productos.ConfiguracionBarTender;
import raven.clases.productos.ConfiguracionUsuarioStore;
import raven.clases.productos.GestorPerfilesImpresion;
import raven.clases.productos.PerfilImpresion;
import raven.clases.productos.PrinterDetector;

/**
 * Diálogo de vista previa de impresión para Rotulación.
 * Muestra las páginas tal como se imprimirán y permite elegir impresora
 * y enviar la impresión directamente.
 */
public class RotulacionPrintPreviewDialog extends JDialog {

    private final JTable tabla;
    private ImpresorTermicaPOSDIG2406T impresor;
    private java.util.List<BufferedImage> paginas;
    private java.util.List<Integer> paginasIndex;
    private JPanel panelPreviewRef;
    private JComboBox<PrintService> cbImpresoras;
    private JSpinner spCopias;
    private JComboBox<String> cbPaginas;
    private JTextField tfRango;
    private JComboBox<Object> cbTamanoPapel;
    private JSpinner spAnchoMM;
    private JSpinner spAltoMM;
    private JSpinner spMargenMM;
    private JSpinner spEtiquetasPorTira;
    private JComboBox<String> cbOrientacion;
    private JSpinner spOffsetXMM;
    private JSpinner spOffsetYMM;
    private JComboBox<String> cbMedidasRapidas;
    private JComboBox<Integer> cbPaginasPorHoja;
    private JComboBox<String> cbEscala;
    private JComboBox<PerfilImpresion> cbPerfiles;
    private JComboBox<String> cbRotacion;
    private AbstractButton chkGrilla;
    private AbstractButton chkGuias;
    private AbstractButton chkMedidas;
    private AbstractButton chkValidacion;
    private JComboBox<String> cbPasoGrilla;
    private JComboBox<String> cbPasoAjuste;
    private JButton btnNudgeLeft;
    private JButton btnNudgeRight;
    private JButton btnNudgeUp;
    private JButton btnNudgeDown;
    private JButton btnResetVista;
    private JButton btnReporte;
    private JButton btnDiagImpresora;
    private JButton btnRefreshImpresoras;
    private JLabel lblCompat;
    private JTabbedPane tabsTipoEtiqueta;

    private PageFormat previewPageFormat;
    private final PreviewSettings previewSettings = new PreviewSettings();
    private java.util.List<BufferedImage> referencePaginas;
    private ImpresorTermicaPOSDIG2406T.ModoImpresion referenceModo;

    private static final class PreviewSettings {
        boolean grilla = true;
        boolean guias = true;
        boolean medidas = true;
        boolean validacion = true;
        double pasoGrillaMm = 5.0;
    }

    private static final class ViewportWidthPanel extends JPanel implements Scrollable {
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
            return Math.max(1, visibleRect.height - 16);
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

    public RotulacionPrintPreviewDialog(java.awt.Frame owner, JTable tabla, ImpresorTermicaPOSDIG2406T impresorInicial) {
        super(owner, "Vista previa de impresión", true);
        this.tabla = tabla;
        this.impresor = impresorInicial;
        setPreferredSize(new Dimension(1040, 640));
        setMinimumSize(new Dimension(960, 600));
        setResizable(true);
        setLayout(new BorderLayout());

        // Panel preview (scrollable)
        JPanel panelPreview = new JPanel();
        panelPreview.setLayout(new BoxLayout(panelPreview, BoxLayout.Y_AXIS));
        panelPreview.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelPreviewRef = panelPreview;
        JScrollPane previewScroll = new JScrollPane(panelPreview);
        previewScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Panel lateral con controles
        JPanel controls = new ViewportWidthPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        cbImpresoras = new JComboBox<>(listarServicios());
        cbImpresoras.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text;
                if (value instanceof PrintService) text = ((PrintService) value).getName();
                else text = String.valueOf(value);
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        btnRefreshImpresoras = new JButton("Actualizar");
        btnRefreshImpresoras.addActionListener(e -> reloadPrinters());
        btnDiagImpresora = new JButton("Diagnóstico");
        btnDiagImpresora.addActionListener(e -> mostrarDiagnosticoImpresion());
        spCopias = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.addActionListener(e -> imprimir());
        JButton btnGuardarPdf = new JButton("Guardar PDF");
        btnGuardarPdf.addActionListener(e -> {
            try {
                // Si está seleccionado tamaño personalizado, usar esas medidas
                if (seleccionPersonalizado()) {
                    double a = (Double) spAnchoMM.getValue();
                    double b = (Double) spAltoMM.getValue();
                    if (this.impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                        this.impresor.exportarPdfConMedidas(a, b, null);
                    } else {
                        this.impresor.setCustomPaperSizeMM(a, b);
                        this.impresor.exportarPdf(null);
                    }
                } else {
                    // Usar el método principal de exportar PDF que ya tiene mejoras
                    this.impresor.exportarPdf(null);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error guardando PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnImprimir.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnGuardarPdf.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnImprimir.putClientProperty(FlatClientProperties.STYLE,
                "arc:16;font:+1;minimumHeight:44;focusWidth:1;background:$Component.focusColor;foreground:#ffffff");
        btnGuardarPdf.putClientProperty(FlatClientProperties.STYLE,
                "arc:16;font:+1;minimumHeight:44;focusWidth:1;background:lighten($Panel.background,6%);borderWidth:1;borderColor:lighten($Panel.background,12%)");
        btnImprimir.setIconTextGap(10);
        btnGuardarPdf.setIconTextGap(10);
        btnImprimir.setHorizontalAlignment(SwingConstants.CENTER);
        btnGuardarPdf.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            FlatSVGIcon printIcon = safeSvgIcon("/raven/icon/icons/imprimir.svg", "raven/icon/icons/imprimir.svg", 18, 18);
            printIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
            btnImprimir.setIcon(printIcon);
        } catch (Exception ignored) {
        }
        try {
            FlatSVGIcon pdfIcon = safeSvgIcon("/raven/menu/icon/8.svg", "raven/menu/icon/8.svg", 18, 18);
            Color fg = UIManager.getColor("Button.foreground");
            if (fg == null) fg = Color.WHITE;
            Color finalFg = fg;
            pdfIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> finalFg));
            btnGuardarPdf.setIcon(pdfIcon);
        } catch (Exception ignored) {
        }
        JButton btnNuevaConfig = new JButton("Nueva config");
        btnNuevaConfig.addActionListener(e -> crearNuevaConfiguracion());
        JButton btnGuardarConfig = new JButton("Guardar cambios");
        btnGuardarConfig.addActionListener(e -> guardarCambiosConfiguracion());

        // Botones para perfiles
        JButton btnNuevoPerfil = new JButton("Nuevo Perfil");
        btnNuevoPerfil.addActionListener(e -> crearNuevoPerfil());
        JButton btnGuardarPerfil = new JButton("Guardar Perfil");
        btnGuardarPerfil.addActionListener(e -> guardarPerfilActual());
        JButton btnEliminarPerfil = new JButton("Eliminar Perfil");
        btnEliminarPerfil.addActionListener(e -> eliminarPerfilSeleccionado());
        JButton btnAplicarPerfil = new JButton("Aplicar Perfil");
        btnAplicarPerfil.addActionListener(e -> aplicarPerfilSeleccionado());

        cbPaginas = new JComboBox<>(new String[]{"Todas", "Sólo impares", "Sólo pares", "Personalizado"});
        tfRango = new JTextField();
        tfRango.setToolTipText("Ej: 1,3-5,8");

        cbTamanoPapel = new JComboBox<>();
        cbTamanoPapel.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text;
                if (value instanceof MediaSizeName) {
                    text = value.toString();
                } else if (value instanceof Media) {
                    text = getMediaDisplayName((Media) value);
                } else if (value instanceof ConfiguracionBarTender) {
                    text = ((ConfiguracionBarTender) value).obtenerInformacionResumen();
                } else if (value instanceof String) {
                    text = (String) value;
                } else {
                    text = String.valueOf(value);
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        // Inicializar combo de perfiles
        cbPerfiles = new JComboBox<>();
        cargarPerfilesDisponibles();

        spAnchoMM = new JSpinner(new SpinnerNumberModel(30.0, 10.0, 200.0, 1.0));
        spAltoMM = new JSpinner(new SpinnerNumberModel(34.0, 10.0, 200.0, 1.0));
        spMargenMM = new JSpinner(new SpinnerNumberModel(2.0, 0.0, 10.0, 0.5));
        spEtiquetasPorTira = new JSpinner(new SpinnerNumberModel(Math.max(1, this.impresor.getEtiquetasPorTira()), 1, 10, 1));
        cbOrientacion = new JComboBox<>(new String[]{"PORTRAIT","LANDSCAPE","VERTICAL_180","HORIZONTAL_180"});
        cbOrientacion.setSelectedItem("LANDSCAPE");
        cbPaginasPorHoja = new JComboBox<>(new Integer[]{1, 2, 4});
        cbEscala = new JComboBox<>(new String[]{"Ajuste inteligente", "10%", "25%", "30%", "40%", "50%", "75%", "100%", "125%", "150%"});

        // Eventos
        cbImpresoras.addItemListener(e -> actualizarMedias());
        cbEscala.addItemListener(e -> {
            String sel = (String) cbEscala.getSelectedItem();
            if ("Ajuste inteligente".equals(sel)) {
                impresor.setAutoFit(true);
            } else {
                impresor.setAutoFit(false);
                double s = parseScale(sel);
                impresor.setScaleFactor(s);
            }
            reconstruirPreview(panelPreviewRef);
        });
        cbRotacion = new JComboBox<>(new String[]{"0°","90°","-90°","180°"});
        cbRotacion.setSelectedItem("0°");
        cbRotacion.addItemListener(e -> {
            String v = (String) cbRotacion.getSelectedItem();
            int deg = 0;
            if ("90°".equals(v)) deg = 90; else if ("-90°".equals(v)) deg = -90; else if ("180°".equals(v)) deg = 180; else deg = 0;
            impresor.setRotationDegrees(deg);
            reconstruirPreview(panelPreviewRef);
        });
        cbPaginas.addItemListener(e -> {
            // Habilita el campo rango solo en modo personalizado
            String sel = (String) cbPaginas.getSelectedItem();
            tfRango.setEnabled("Personalizado".equals(sel));
            // Cambiar selección de páginas debe refrescar la vista previa
            reconstruirPreview(panelPreview);
        });
        tfRango.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void refreshIfCustom() {
                String sel = (String) cbPaginas.getSelectedItem();
                if ("Personalizado".equals(sel)) {
                    reconstruirPreview(panelPreviewRef);
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshIfCustom(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshIfCustom(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshIfCustom(); }
        });
        cbTamanoPapel.addItemListener(e -> {
            applySelectedMediaSize();
            reconstruirPreview(panelPreviewRef);
        });
        spAnchoMM.addChangeListener(e -> {
            if (seleccionPersonalizado()) {
                impresor.setCustomPaperSizeMM((Double) spAnchoMM.getValue(), (Double) spAltoMM.getValue());
                reconstruirPreview(panelPreviewRef);
            }
        });
        spAltoMM.addChangeListener(e -> {
            if (seleccionPersonalizado()) {
                impresor.setCustomPaperSizeMM((Double) spAnchoMM.getValue(), (Double) spAltoMM.getValue());
                reconstruirPreview(panelPreviewRef);
            }
        });
        spMargenMM.addChangeListener(e -> {
            if (seleccionPersonalizado()) {
                applySelectedMediaSize();
                reconstruirPreview(panelPreviewRef);
            }
        });
        spEtiquetasPorTira.addChangeListener(e -> {
            try {
                this.impresor.setEtiquetasPorTira((Integer) spEtiquetasPorTira.getValue());
            } catch (Exception ignored) {}
            applySelectedMediaSize();
            reconstruirPreview(panelPreviewRef);
        });
        cbOrientacion.addItemListener(e -> {
            if (seleccionPersonalizado()) {
                applySelectedMediaSize();
                reconstruirPreview(panelPreviewRef);
            }
        });

        tabsTipoEtiqueta = new JTabbedPane();
        JPanel tabCaja = new JPanel();
        tabCaja.setPreferredSize(new Dimension(0, 0));
        tabCaja.setMinimumSize(new Dimension(0, 0));
        JPanel tabPar = new JPanel();
        tabPar.setPreferredSize(new Dimension(0, 0));
        tabPar.setMinimumSize(new Dimension(0, 0));
        JPanel tabInterna = new JPanel();
        tabInterna.setPreferredSize(new Dimension(0, 0));
        tabInterna.setMinimumSize(new Dimension(0, 0));
        tabsTipoEtiqueta.addTab("Etiqueta Caja", tabCaja);
        tabsTipoEtiqueta.addTab("Etiqueta Par", tabPar);
        tabsTipoEtiqueta.addTab("Etiqueta Interna", tabInterna);
        tabsTipoEtiqueta.setEnabledAt(2, false);
        tabsTipoEtiqueta.setSelectedIndex(this.impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.CAJA ? 0 : 1);
        tabsTipoEtiqueta.addChangeListener(e -> {
            int sel = tabsTipoEtiqueta.getSelectedIndex();
            if (sel == 0) {
                cambiarModo(ImpresorTermicaPOSDIG2406T.ModoImpresion.CAJA);
            } else if (sel == 1) {
                cambiarModo(ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA);
            }
        });

        controls.add(tabsTipoEtiqueta);
        controls.add(Box.createVerticalStrut(10));

        JPanel printerActions = new JPanel(new GridLayout(1, 2, 6, 6));
        printerActions.add(btnRefreshImpresoras);
        printerActions.add(btnDiagImpresora);

        JPanel perfilButtons = new JPanel(new GridLayout(0, 2, 6, 6));
        perfilButtons.add(btnNuevoPerfil);
        perfilButtons.add(btnGuardarPerfil);
        perfilButtons.add(btnEliminarPerfil);
        perfilButtons.add(btnAplicarPerfil);

        spOffsetXMM = new JSpinner(new SpinnerNumberModel(-2.0, -20.0, 20.0, 0.5));
        spOffsetYMM = new JSpinner(new SpinnerNumberModel(0.0, -20.0, 20.0, 0.5));
        cbMedidasRapidas = new JComboBox<>(new String[]{"Ninguna","40 x 20 mm","40 x 25 mm","50 x 30 mm","105 x 25 mm"});

        chkGrilla = new JToggleButton("Grilla", true);
        chkGuias = new JToggleButton("Zona imprimible", true);
        chkMedidas = new JToggleButton("Medidas", true);
        chkValidacion = new JToggleButton("Recorte", true);
        chkGrilla.setFocusable(false);
        chkGuias.setFocusable(false);
        chkMedidas.setFocusable(false);
        chkValidacion.setFocusable(false);
        cbPasoGrilla = new JComboBox<>(new String[]{"1 mm", "2 mm", "5 mm", "10 mm"});
        cbPasoGrilla.setSelectedItem("5 mm");
        cbPasoAjuste = new JComboBox<>(new String[]{"0.1 mm", "0.25 mm", "0.5 mm", "1 mm"});
        cbPasoAjuste.setSelectedItem("0.5 mm");
        btnNudgeLeft = new JButton("←");
        btnNudgeRight = new JButton("→");
        btnNudgeUp = new JButton("↑");
        btnNudgeDown = new JButton("↓");
        btnResetVista = new JButton("Reset vista");
        btnReporte = new JButton("Reporte");
        lblCompat = new JLabel("Compatibilidad: --");

        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toggles.add(chkGrilla);
        toggles.add(chkGuias);
        toggles.add(chkMedidas);
        toggles.add(chkValidacion);

        JPanel nudges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nudges.add(btnNudgeLeft);
        nudges.add(btnNudgeRight);
        nudges.add(btnNudgeUp);
        nudges.add(btnNudgeDown);

        JButton btnAjustarMargen = new JButton("Ajustar al margen");
        btnAjustarMargen.addActionListener(e -> {
            cbEscala.setSelectedItem("Ajuste inteligente");
            impresor.setAutoFit(true);
            impresor.setRotationDegrees(0);
            try { impresor.setContenidoOffsetMM((Double) spOffsetXMM.getValue()); } catch (Exception ignored) {}
            try { impresor.setContenidoOffsetYMM((Double) spOffsetYMM.getValue()); } catch (Exception ignored) {}
            try { impresor.setPaddingYMM(3.0, 3.0); } catch (Exception ignored) {}
            try { cbOrientacion.setSelectedItem("LANDSCAPE"); } catch (Exception ignored) {}
            applySelectedMediaSize();
            reconstruirPreview(panelPreviewRef);
        });

        JPanel customSize = new JPanel(new GridLayout(0, 2, 6, 6));
        customSize.add(new JLabel("Ancho (mm)"));
        customSize.add(spAnchoMM);
        customSize.add(new JLabel("Alto (mm)"));
        customSize.add(spAltoMM);
        customSize.add(new JLabel("Margen (mm)"));
        customSize.add(spMargenMM);
        customSize.add(new JLabel("Etiquetas por fila"));
        customSize.add(spEtiquetasPorTira);
        customSize.add(new JLabel("Orientación"));
        customSize.add(cbOrientacion);
        customSize.add(new JLabel("Desplazamiento X (mm)"));
        customSize.add(spOffsetXMM);
        customSize.add(new JLabel("Desplazamiento Y (mm)"));
        customSize.add(spOffsetYMM);

        JPanel seccionImpresora = buildAccordionContent(
                buildSubGroup("Selección", cbImpresoras),
                buildSubGroup("Acciones", printerActions)
        );
        JPanel seccionPaginas = buildAccordionContent(
                buildSubGroup("Páginas", cbPaginas, tfRango),
                buildSubGroup("Copias", spCopias),
                buildSubGroup("Páginas por hoja", cbPaginasPorHoja)
        );
        JPanel seccionPerfiles = buildAccordionContent(
                buildSubGroup("Perfil", cbPerfiles),
                buildSubGroup("Acciones", perfilButtons)
        );
        JPanel seccionPapel = buildAccordionContent(
                buildSubGroup("Tamaño", cbTamanoPapel),
                buildSubGroup("Parámetros", customSize),
                buildSubGroup("Medidas rápidas", cbMedidasRapidas),
                buildSubGroup("Configuraciones", buildTwoButtons(btnNuevaConfig, btnGuardarConfig))
        );
        JPanel seccionVisualizador = buildAccordionContent(
                buildSubGroup("Opciones", toggles),
                buildSubGroup("Paso grilla", cbPasoGrilla),
                buildSubGroup("Ajuste fino", cbPasoAjuste),
                buildSubGroup("Nudge", nudges),
                buildSubGroup("Herramientas", btnResetVista, btnReporte, lblCompat),
                buildSubGroup("Auto-ajuste", btnAjustarMargen)
        );
        JPanel seccionAvanzada = buildAccordionContent(
                buildSubGroup("Escala", cbEscala),
                buildSubGroup("Rotación", cbRotacion)
        );

        java.util.List<AccordionSection> allSections = new java.util.ArrayList<>();
        allSections.add(new AccordionSection("Impresora", "IMP", seccionImpresora, true));
        allSections.add(new AccordionSection("Páginas y Copias", "PAG", seccionPaginas, false));
        allSections.add(new AccordionSection("Perfiles de Impresión", "PRF", seccionPerfiles, false));
        allSections.add(new AccordionSection("Tamaño del Papel", "PAPEL", seccionPapel, false));
        allSections.add(new AccordionSection("Visualizador", "VISTA", seccionVisualizador, false));
        allSections.add(new AccordionSection("Configuración Avanzada", "ADV", seccionAvanzada, false));

        JPanel accordionContainer = new JPanel();
        accordionContainer.setLayout(new BoxLayout(accordionContainer, BoxLayout.Y_AXIS));
        accordionContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (AccordionSection sec : allSections) {
            sec.setAlignmentX(Component.LEFT_ALIGNMENT);
            accordionContainer.add(sec);
            accordionContainer.add(Box.createVerticalStrut(15));
        }

        controls.add(accordionContainer);
        spOffsetXMM.addChangeListener(e -> {
            impresor.setContenidoOffsetMM((Double) spOffsetXMM.getValue());
            reconstruirPreview(panelPreviewRef);
        });
        spOffsetYMM.addChangeListener(e -> {
            impresor.setContenidoOffsetYMM((Double) spOffsetYMM.getValue());
            reconstruirPreview(panelPreviewRef);
        });
        chkGrilla.addActionListener(e -> { previewSettings.grilla = chkGrilla.isSelected(); repaintPreviewPanels(); });
        chkGuias.addActionListener(e -> { previewSettings.guias = chkGuias.isSelected(); repaintPreviewPanels(); });
        chkMedidas.addActionListener(e -> { previewSettings.medidas = chkMedidas.isSelected(); repaintPreviewPanels(); });
        chkValidacion.addActionListener(e -> { previewSettings.validacion = chkValidacion.isSelected(); repaintPreviewPanels(); updateCompatLabel(); });
        cbPasoGrilla.addItemListener(e -> {
            String v = (String) cbPasoGrilla.getSelectedItem();
            previewSettings.pasoGrillaMm = parseMm(v, 5.0);
            repaintPreviewPanels();
        });
        btnResetVista.addActionListener(e -> resetVistaPreview());
        btnReporte.addActionListener(e -> mostrarReporteCompatibilidad());
        btnNudgeLeft.addActionListener(e -> nudgeOffset(-1, 0));
        btnNudgeRight.addActionListener(e -> nudgeOffset(1, 0));
        btnNudgeUp.addActionListener(e -> nudgeOffset(0, -1));
        btnNudgeDown.addActionListener(e -> nudgeOffset(0, 1));
        cbMedidasRapidas.addItemListener(e -> {
            String v = (String) cbMedidasRapidas.getSelectedItem();
            if (v == null || "Ninguna".equals(v)) return;
            String[] parts = v.replace("mm"," ").replace("x"," ").trim().split("\\s+");
            if (parts.length >= 2) {
                try {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    spAnchoMM.setValue(a);
                    spAltoMM.setValue(b);
                    cbTamanoPapel.setSelectedItem("Personalizado");
                    applySelectedMediaSize();
                    reconstruirPreview(panelPreviewRef);
                } catch (Exception ignore) {}
            }
        });
        JScrollPane controlsScroll = new JScrollPane(controls);
        controlsScroll.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Reducir bordes
        controlsScroll.setViewportBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        controlsScroll.setPreferredSize(new Dimension(380, 0)); // Reducir ancho preferido
        controlsScroll.setMinimumSize(new Dimension(320, 0)); // Reducir ancho mínimo
        controlsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        controlsScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Panel de acciones con mejor alineación
        JPanel actionsFooter = new JPanel(new GridLayout(0, 1, 0, 6)); // Usar GridLayout con espacio entre botones
        actionsFooter.setOpaque(false);
        actionsFooter.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Uniformar bordes
        btnImprimir.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnGuardarPdf.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnImprimir.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnImprimir.getPreferredSize().height));
        btnGuardarPdf.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnGuardarPdf.getPreferredSize().height));
        actionsFooter.add(btnImprimir);
        actionsFooter.add(btnGuardarPdf);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); // Reducir bordes laterales
        rightPanel.add(controlsScroll, BorderLayout.CENTER);
        rightPanel.add(actionsFooter, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewScroll, rightPanel);
        split.setResizeWeight(0.60); // Equilibrar mejor la distribución: 60% vista previa, 40% controles
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setBorder(null);
        split.setDividerSize(6); // Tamaño más delgado para ahorrar espacio
        split.setDividerLocation(0.60); // Ajustar la división para equilibrar mejor el espacio
        add(split, BorderLayout.CENTER);

        paginas = new java.util.ArrayList<>();

        // Inicializar combos basados en impresora y restaurar tamaño actual del impresor
        selectDefaultPrinter();
        actualizarMedias();
        try {
            java.awt.print.PageFormat pfInit = impresor.buildPageFormat(java.awt.print.PrinterJob.getPrinterJob());
            java.awt.print.Paper paper = pfInit.getPaper();
            double mmW = paper.getWidth() * 25.4 / 72.0;
            double mmH = paper.getHeight() * 25.4 / 72.0;
            boolean esEtiqueta = impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
            int colsEtiqueta = Math.max(1, impresor.getEtiquetasPorTira());
            boolean driverGrande = (mmW > 200.0 || mmH > 200.0);
            double showW = esEtiqueta ? (mmW / colsEtiqueta) : mmW;
            if (driverGrande || showW <= 0.0 || mmH <= 0.0) {
                spAnchoMM.setValue(30.0);
                spAltoMM.setValue(34.0);
                cbOrientacion.setSelectedItem("LANDSCAPE");
            } else {
                spAnchoMM.setValue(showW);
                spAltoMM.setValue(mmH);
                cbOrientacion.setSelectedItem(pfInit.getOrientation() == java.awt.print.PageFormat.LANDSCAPE ? "LANDSCAPE" : "PORTRAIT");
            }
        } catch (Exception ignored) {
            try { spAnchoMM.setValue(30.0); spAltoMM.setValue(34.0); } catch (Exception ignore2) {}
            try { cbOrientacion.setSelectedItem("LANDSCAPE"); } catch (Exception ignore2) {}
        }
        cbTamanoPapel.setSelectedItem("Personalizado");
        applySelectedMediaSize();
        cbEscala.setSelectedItem("Ajuste inteligente");
        cbPaginas.setSelectedIndex(0);
        tfRango.setEnabled(false);
        try { impresor.setContenidoOffsetMM((Double) spOffsetXMM.getValue()); } catch (Exception ignored) {}
        try { impresor.setContenidoOffsetYMM((Double) spOffsetYMM.getValue()); } catch (Exception ignored) {}
        try { impresor.setPaddingYMM(3.0, 3.0); } catch (Exception ignored) {}
        reconstruirPreview(panelPreviewRef);
        actualizarEstadoModo();
        
        PrintService servicioActual = (PrintService) cbImpresoras.getSelectedItem();
        if (servicioActual != null) {
            boolean esXP420B = detectarImpresoraXP420B(servicioActual);
            impresor.setUsarConfiguracionXP420B(esXP420B);
            if (esXP420B && impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                try { 
                    spEtiquetasPorTira.setValue(3); 
                    impresor.setEtiquetasPorTira(3);
                } catch (Exception ignored) {}
            }
        }

        pack();
        setSize(Math.max(900, getWidth()), Math.max(600, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void selectDefaultPrinter() {
        try {
            if (cbImpresoras == null) return;
            PrintService selected = (PrintService) cbImpresoras.getSelectedItem();
            if (selected != null) return;
            PrintService def = PrintServiceLookup.lookupDefaultPrintService();
            if (def == null) {
                if (cbImpresoras.getItemCount() > 0) cbImpresoras.setSelectedIndex(0);
                return;
            }
            for (int i = 0; i < cbImpresoras.getItemCount(); i++) {
                PrintService it = cbImpresoras.getItemAt(i);
                if (it != null && def.getName().equals(it.getName())) {
                    cbImpresoras.setSelectedIndex(i);
                    return;
                }
            }
            if (cbImpresoras.getItemCount() > 0) cbImpresoras.setSelectedIndex(0);
        } catch (Exception ignored) {}
    }

    private void reloadPrinters() {
        String prev = null;
        try {
            PrintService s = (PrintService) cbImpresoras.getSelectedItem();
            if (s != null) prev = s.getName();
        } catch (Exception ignored) {}

        cbImpresoras.setModel(new DefaultComboBoxModel<>(listarServicios()));

        if (prev != null) {
            for (int i = 0; i < cbImpresoras.getItemCount(); i++) {
                PrintService it = cbImpresoras.getItemAt(i);
                if (it != null && prev.equals(it.getName())) {
                    cbImpresoras.setSelectedIndex(i);
                    prev = null;
                    break;
                }
            }
        }
        if (prev != null) selectDefaultPrinter();
        actualizarMedias();
        reconstruirPreview(panelPreviewRef);
    }

    private void mostrarDiagnosticoImpresion() {
        StringBuilder sb = new StringBuilder();
        PrintService def = null;
        try { def = PrintServiceLookup.lookupDefaultPrintService(); } catch (Exception ignored) {}
        PrintService sel = null;
        try { sel = (PrintService) cbImpresoras.getSelectedItem(); } catch (Exception ignored) {}

        sb.append("Impresora seleccionada: ").append(sel != null ? sel.getName() : "(ninguna)").append("\n");
        sb.append("Impresora predeterminada: ").append(def != null ? def.getName() : "(no disponible)").append("\n\n");

        sb.append("Servicios detectados: ").append(cbImpresoras.getItemCount()).append("\n");
        for (int i = 0; i < cbImpresoras.getItemCount(); i++) {
            PrintService ps = cbImpresoras.getItemAt(i);
            if (ps == null) continue;
            boolean okPrintable = false;
            try {
                okPrintable = ps.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PRINTABLE);
            } catch (Exception ignored) {}
            sb.append("- ").append(ps.getName());
            sb.append(okPrintable ? " [PRINTABLE]" : " [NO PRINTABLE]");
            sb.append("\n");
        }

        JTextArea ta = new JTextArea(sb.toString(), 18, 60);
        ta.setEditable(false);
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        JOptionPane.showMessageDialog(this, sp, "Diagnóstico de impresión", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cambiarModo(ImpresorTermicaPOSDIG2406T.ModoImpresion nuevoModo) {
        if (nuevoModo == null) return;
        if (impresor != null && impresor.getModo() == nuevoModo) {
            actualizarEstadoModo();
            return;
        }
        int mL = 2, mT = 2, mR = 2, mB = 2;
        try { mL = impresor.getMargenIzquierdoPoints(); } catch (Exception ignored) {}
        try { mT = impresor.getMargenSuperiorPoints(); } catch (Exception ignored) {}
        try { mR = impresor.getMargenDerechoPoints(); } catch (Exception ignored) {}
        try { mB = impresor.getMargenInferiorPoints(); } catch (Exception ignored) {}
        this.impresor = new ImpresorTermicaPOSDIG2406T(tabla, nuevoModo);
        try { this.impresor.setMargenes(mL, mT, mR, mB); } catch (Exception ignored) {}
        sincronizarImpresorDesdeUI();
        actualizarEstadoModo();
        reconstruirPreview(panelPreviewRef);
        updateCompatLabel();
    }

    private void sincronizarImpresorDesdeUI() {
        if (impresor == null) return;
        try { impresor.setEtiquetasPorTira((Integer) spEtiquetasPorTira.getValue()); } catch (Exception ignored) {}
        try { impresor.setContenidoOffsetMM((Double) spOffsetXMM.getValue()); } catch (Exception ignored) {}
        try { impresor.setContenidoOffsetYMM((Double) spOffsetYMM.getValue()); } catch (Exception ignored) {}
        try { impresor.setPaddingYMM(3.0, 3.0); } catch (Exception ignored) {}

        try {
            String rot = (String) cbRotacion.getSelectedItem();
            int deg = 0;
            if ("90°".equals(rot)) deg = 90;
            else if ("-90°".equals(rot)) deg = -90;
            else if ("180°".equals(rot)) deg = 180;
            impresor.setRotationDegrees(deg);
        } catch (Exception ignored) {}

        try {
            String sel = (String) cbEscala.getSelectedItem();
            if ("Ajuste inteligente".equals(sel)) {
                impresor.setAutoFit(true);
            } else {
                impresor.setAutoFit(false);
                impresor.setScaleFactor(parseScale(sel));
            }
        } catch (Exception ignored) {}

        applySelectedMediaSize();
    }

    private void actualizarEstadoModo() {
        boolean esEtiqueta = impresor != null && impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
        if (spEtiquetasPorTira != null) spEtiquetasPorTira.setEnabled(esEtiqueta);
    }

    private PrintService[] listarServicios() {
        try {
            PrintService[] servicios = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
            if (servicios != null && servicios.length > 0) return servicios;
            servicios = PrintServiceLookup.lookupPrintServices(null, null);
            if (servicios != null && servicios.length > 0) return servicios;
            PrintService def = PrintServiceLookup.lookupDefaultPrintService();
            return def == null ? new PrintService[0] : new PrintService[]{def};
        } catch (Exception ex) {
            return new PrintService[0];
        }
    }

    private void imprimir() {
        // Verificar si hay impresoras disponibles antes de intentar imprimir
        if (!PrinterDetector.hayImpresorasDisponibles()) {
            JOptionPane.showMessageDialog(
                this,
                "No se detectó ninguna impresora conectada.\n\n" +
                "Por favor, verifica que:\n" +
                "  • La impresora esté conectada por USB y encendida\n" +
                "  • Los drivers estén instalados correctamente\n" +
                "  • Windows reconozca la impresora en Configuración > Dispositivos > Impresoras",
                "Error: Impresora no detectada",
                JOptionPane.ERROR_MESSAGE
            );
            PrinterDetector.mostrarImpresorasDisponibles();
            return;
        }

        PrintService servicio = (PrintService) cbImpresoras.getSelectedItem();
        if (servicio == null) {
            try { servicio = PrintServiceLookup.lookupDefaultPrintService(); } catch (Exception ignored) {}
            if (servicio == null) {
                JOptionPane.showMessageDialog(this, "No hay impresoras disponibles", "Impresión", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        // Detectar si es impresora XP420B y aplicar configuración específica
        boolean esXP420B = detectarImpresoraXP420B(servicio);
        impresor.setUsarConfiguracionXP420B(esXP420B);
        
        if (esXP420B) {
            System.out.println("Detectada impresora XP420B - usando configuración optimizada");
        }
        
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(servicio);
            applySelectedMediaSize();
            PageFormat pf = impresor.buildPageFormat(job);
            impresor.prepararEtiquetasParaImpresion();
            job.setPrintable(impresor, pf);
            PrintRequestAttributeSet attrs = buildAttributes(servicio);
            job.print(attrs);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al imprimir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PrintRequestAttributeSet buildAttributes(PrintService servicio) {
        HashPrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies((Integer) spCopias.getValue()));
        try { attrs.add(PrintQuality.HIGH); } catch (Exception ignored) {}
        try { attrs.add(new PrinterResolution(300, 300, PrinterResolution.DPI)); } catch (Exception ignored) {}
        try {
            String o = (String) cbOrientacion.getSelectedItem();
            if ("LANDSCAPE".equals(o)) attrs.add(OrientationRequested.LANDSCAPE);
            else if ("PORTRAIT".equals(o)) attrs.add(OrientationRequested.PORTRAIT);
        } catch (Exception ignored) {}

        // Páginas/rango
        if (paginas != null && !paginas.isEmpty()) {
            String sel = (String) cbPaginas.getSelectedItem();
            if ("Sólo impares".equals(sel)) {
                attrs.add(new PageRanges(buildOddRanges(paginas.size())));
            } else if ("Sólo pares".equals(sel)) {
                attrs.add(new PageRanges(buildEvenRanges(paginas.size())));
            } else if ("Personalizado".equals(sel)) {
                int[][] r = parsePageRanges(tfRango.getText(), paginas.size());
                if (r != null) attrs.add(new PageRanges(r));
            }
        }

        // Tamaño de papel
        Object mediaSel = cbTamanoPapel.getSelectedItem();
        if (mediaSel instanceof Media) {
            attrs.add((Media) mediaSel);
        } else if (seleccionPersonalizado()) {
            impresor.setCustomPaperSizeMM((Double) spAnchoMM.getValue(), (Double) spAltoMM.getValue());
        } else {
            impresor.clearCustomPaperSize();
        }

        // Páginas por hoja (si la impresora soporta NumberUp)
        Integer nup = (Integer) cbPaginasPorHoja.getSelectedItem();
        if (nup != null && nup > 1) {
            attrs.add(new NumberUp(nup));
        }
        return attrs;
    }

    private void actualizarMedias() {
        cbTamanoPapel.removeAllItems();
        cbTamanoPapel.addItem("Predeterminado del controlador");
        PrintService svc = (PrintService) cbImpresoras.getSelectedItem();
        if (svc == null) return;
        try {
            Object vals = svc.getSupportedAttributeValues(Media.class, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
            if (vals instanceof Object[]) {
                for (Object v : (Object[]) vals) {
                    if (v instanceof Media) cbTamanoPapel.addItem(v);
                }
            }
        } catch (Exception ignored) {}
        try {
            if (detectarImpresoraXP420B(svc)) {
                // FORCE 3 labels for XP-420B if in label mode
                if (impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                    try { 
                        spEtiquetasPorTira.setValue(3); 
                        impresor.setEtiquetasPorTira(3);
                    } catch (Exception ignored) {}
                }
                
                ConfiguracionImpresoraXP420B_UNIVERSAL cfg = new ConfiguracionImpresoraXP420B_UNIVERSAL();
                java.util.List<ConfiguracionBarTender> list = cfg.leerTodasLasConfiguracionesDriver(svc.getName());
                if (list != null) {
                    for (ConfiguracionBarTender c : list) cbTamanoPapel.addItem(c);
                }
            }
        } catch (Exception ignored) {}
        try {
            java.util.List<ConfiguracionBarTender> user = ConfiguracionUsuarioStore.cargar();
            if (user != null) {
                for (ConfiguracionBarTender c : user) cbTamanoPapel.addItem(c);
            }
        } catch (Exception ignored) {}
        cbTamanoPapel.addItem("Personalizado");
        cbTamanoPapel.setSelectedIndex(0);
        applySelectedMediaSize();
    }

    private boolean seleccionPersonalizado() {
        Object sel = cbTamanoPapel.getSelectedItem();
        return (sel instanceof String) && "Personalizado".equals(sel);
    }

    private double parseScale(String s) {
        if (s == null) return 1.0;
        try {
            return Integer.parseInt(s.replace("%", "")) / 100.0;
        } catch (Exception e) { return 1.0; }
    }

    private void applySelectedMediaSize() {
        Object sel = cbTamanoPapel.getSelectedItem();
        boolean esEtiqueta = impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
        int colsEtiqueta = Math.max(1, impresor.getEtiquetasPorTira());
        if (sel instanceof String && "Predeterminado del controlador".equals(sel)) {
            impresor.clearCustomPaperSize();
            try {
                PrintService svc = (PrintService) cbImpresoras.getSelectedItem();
                if (svc != null) {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPrintService(svc);
                    PageFormat pf = job.defaultPage();
                    Paper paper = pf.getPaper();
                    double mmW = paper.getWidth() * 25.4 / 72.0;
                    double mmH = paper.getHeight() * 25.4 / 72.0;
                    double showW = esEtiqueta ? (mmW / colsEtiqueta) : mmW;
                    try { spAnchoMM.setValue(showW); spAltoMM.setValue(mmH); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            return;
        }
        if (sel instanceof ConfiguracionBarTender) {
            ConfiguracionBarTender c = (ConfiguracionBarTender) sel;
            double showW = esEtiqueta ? (c.anchoMm / colsEtiqueta) : c.anchoMm;
            try { spAnchoMM.setValue(showW); spAltoMM.setValue(c.altoMm); } catch (Exception ignored) {}
            try { spMargenMM.setValue(c.margenMm); } catch (Exception ignored) {}
            try { cbOrientacion.setSelectedItem(c.orientacion); } catch (Exception ignored) {}
            c.aplicarConfiguracionImpresor(impresor);
            impresor.setRotate180(c.requiereRotacionContenido());
            return;
        }
        if (sel instanceof MediaSizeName) {
            MediaSize ms = MediaSize.getMediaSizeForName((MediaSizeName) sel);
            if (ms != null) {
                double mmW = ms.getX(MediaSize.MM);
                double mmH = ms.getY(MediaSize.MM);
                double showW = esEtiqueta ? (mmW / colsEtiqueta) : mmW;
                try { spAnchoMM.setValue(showW); spAltoMM.setValue(mmH); } catch (Exception ignored) {}
                impresor.setCustomPaperSizeMM(mmW, mmH);
                return;
            }
        }
        // Si el driver no expone MediaSizeName (nombres personalizados como "USER", "2 x 4"),
        // intentamos deducir el tamaño:
        if (sel instanceof Media) {
            double[] mm = inferSizeFromMediaOrDriver(sel, (PrintService) cbImpresoras.getSelectedItem());
            if (mm != null) {
                double showW = esEtiqueta ? (mm[0] / colsEtiqueta) : mm[0];
                try { spAnchoMM.setValue(showW); spAltoMM.setValue(mm[1]); } catch (Exception ignored) {}
                impresor.setCustomPaperSizeMM(mm[0], mm[1]);
                return;
            }
        }
        if (seleccionPersonalizado()) {
            double a = (Double) spAnchoMM.getValue();
            double b = (Double) spAltoMM.getValue();
            double m = (Double) spMargenMM.getValue();
            String o = (String) cbOrientacion.getSelectedItem();
            double paperW = esEtiqueta ? (a * colsEtiqueta) : a;
            ConfiguracionBarTender c = new ConfiguracionBarTender("Personalizado", paperW, b, o, m,
                    152.40, 6, 8.0, 3.0, "TERMICA_DIRECTA", "RASGAR", "USER", false, "Usuario");
            c.aplicarConfiguracionImpresor(impresor);
            impresor.setRotate180(c.requiereRotacionContenido());
        } else {
            impresor.clearCustomPaperSize();
        }
    }

    private String getMediaDisplayName(Media media) {
        try {
            java.lang.reflect.Method m = media.getClass().getMethod("getName", java.util.Locale.class);
            Object name = m.invoke(media, java.util.Locale.getDefault());
            if (name != null) return name.toString();
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Method m = media.getClass().getMethod("getName");
            Object name = m.invoke(media);
            if (name != null) return name.toString();
        } catch (Exception ignored) {}
        return media.toString();
    }

    private void crearNuevaConfiguracion() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la configuración");
        if (nombre == null || nombre.trim().isEmpty()) return;
        double a = (Double) spAnchoMM.getValue();
        double b = (Double) spAltoMM.getValue();
        double m = (Double) spMargenMM.getValue();
        String o = (String) cbOrientacion.getSelectedItem();
        boolean esEtiqueta = impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
        int colsEtiqueta = Math.max(1, impresor.getEtiquetasPorTira());
        double paperW = esEtiqueta ? (a * colsEtiqueta) : a;
        ConfiguracionBarTender c = new ConfiguracionBarTender(nombre.trim(), paperW, b, o, m,
                152.40, 6, 8.0, 3.0, "TERMICA_DIRECTA", "RASGAR", "USER", true, "Usuario");
        ConfiguracionUsuarioStore.guardar(c);
        cbTamanoPapel.addItem(c);
        cbTamanoPapel.setSelectedItem(c);
        applySelectedMediaSize();
        reconstruirPreview(panelPreviewRef);
    }

    private void guardarCambiosConfiguracion() {
        Object sel = cbTamanoPapel.getSelectedItem();
        if (!(sel instanceof ConfiguracionBarTender)) {
            crearNuevaConfiguracion();
            return;
        }
        ConfiguracionBarTender c = (ConfiguracionBarTender) sel;
        double a = (Double) spAnchoMM.getValue();
        double b = (Double) spAltoMM.getValue();
        double m = (Double) spMargenMM.getValue();
        String o = (String) cbOrientacion.getSelectedItem();
        boolean esEtiqueta = impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA;
        int colsEtiqueta = Math.max(1, impresor.getEtiquetasPorTira());
        double paperW = esEtiqueta ? (a * colsEtiqueta) : a;
        c.anchoMm = paperW;
        c.altoMm = b;
        c.margenMm = m;
        c.orientacion = o;
        c.softwareOrigen = "Usuario";
        ConfiguracionUsuarioStore.guardar(c);
        applySelectedMediaSize();
        reconstruirPreview(panelPreviewRef);
    }

    /**
     * Carga los perfiles disponibles en el combo
     */
    private void cargarPerfilesDisponibles() {
        cbPerfiles.removeAllItems();
        GestorPerfilesImpresion gestor = GestorPerfilesImpresion.getInstancia();
        java.util.List<PerfilImpresion> perfiles = gestor.getPerfiles();
        for (PerfilImpresion perfil : perfiles) {
            cbPerfiles.addItem(perfil);
        }
    }

    /**
     * Crea un nuevo perfil basado en la configuración actual
     */
    private void crearNuevoPerfil() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del nuevo perfil:");
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        if (GestorPerfilesImpresion.getInstancia().existePerfil(nombre.trim())) {
            JOptionPane.showMessageDialog(this, "Ya existe un perfil con este nombre.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PerfilImpresion nuevoPerfil = new PerfilImpresion(
                nombre.trim(),
                (Double) spAnchoMM.getValue(),
                (Double) spAltoMM.getValue(),
                (Double) spMargenMM.getValue(),
                (String) cbOrientacion.getSelectedItem(),
                "Ajuste inteligente".equals(cbEscala.getSelectedItem()),
                parseScale((String) cbEscala.getSelectedItem()),
                parseRotation((String) cbRotacion.getSelectedItem()),
                (Double) spOffsetXMM.getValue(),
                (Double) spOffsetYMM.getValue(),
                "Par", // tipo de etiqueta no se puede determinar directamente desde aquí
                false // XP420B no se puede determinar directamente
            );

            GestorPerfilesImpresion.getInstancia().agregarPerfil(nuevoPerfil);
            cargarPerfilesDisponibles();
            cbPerfiles.setSelectedItem(nuevoPerfil);
            JOptionPane.showMessageDialog(this, "Perfil creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear perfil: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Guarda la configuración actual en el perfil seleccionado
     */
    private void guardarPerfilActual() {
        PerfilImpresion perfilSeleccionado = (PerfilImpresion) cbPerfiles.getSelectedItem();
        if (perfilSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un perfil para guardar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            perfilSeleccionado.setAnchoEtiquetaMm((Double) spAnchoMM.getValue());
            perfilSeleccionado.setAltoEtiquetaMm((Double) spAltoMM.getValue());
            perfilSeleccionado.setMargenMm((Double) spMargenMM.getValue());
            perfilSeleccionado.setOrientacion((String) cbOrientacion.getSelectedItem());
            perfilSeleccionado.setAutoFit("Ajuste inteligente".equals(cbEscala.getSelectedItem()));
            perfilSeleccionado.setEscala(parseScale((String) cbEscala.getSelectedItem()));
            perfilSeleccionado.setRotacionGrados(parseRotation((String) cbRotacion.getSelectedItem()));
            perfilSeleccionado.setOffsetXMm((Double) spOffsetXMM.getValue());
            perfilSeleccionado.setOffsetYMM((Double) spOffsetYMM.getValue());

            GestorPerfilesImpresion.getInstancia().actualizarPerfil(perfilSeleccionado);
            JOptionPane.showMessageDialog(this, "Perfil actualizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar perfil: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Elimina el perfil seleccionado
     */
    private void eliminarPerfilSeleccionado() {
        PerfilImpresion perfilSeleccionado = (PerfilImpresion) cbPerfiles.getSelectedItem();
        if (perfilSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un perfil para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de eliminar el perfil '" + perfilSeleccionado.getNombre() + "'?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            try {
                GestorPerfilesImpresion.getInstancia().eliminarPerfil(perfilSeleccionado.getNombre());
                cargarPerfilesDisponibles();
                JOptionPane.showMessageDialog(this, "Perfil eliminado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar perfil: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Aplica el perfil seleccionado a la configuración actual
     */
    private void aplicarPerfilSeleccionado() {
        PerfilImpresion perfilSeleccionado = (PerfilImpresion) cbPerfiles.getSelectedItem();
        if (perfilSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un perfil para aplicar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Aplicar al impresor
            perfilSeleccionado.aplicarA(impresor);

            // Actualizar controles de la interfaz
            spAnchoMM.setValue(perfilSeleccionado.getAnchoEtiquetaMm());
            spAltoMM.setValue(perfilSeleccionado.getAltoEtiquetaMm());
            spMargenMM.setValue(perfilSeleccionado.getMargenMm());
            cbOrientacion.setSelectedItem(perfilSeleccionado.getOrientacion());

            if (perfilSeleccionado.isAutoFit()) {
                cbEscala.setSelectedItem("Ajuste inteligente");
            } else {
                cbEscala.setSelectedItem(String.format("%.0f%%", perfilSeleccionado.getEscala() * 100));
            }

            String rotacionStr = getRotationString(perfilSeleccionado.getRotacionGrados());
            cbRotacion.setSelectedItem(rotacionStr);

            spOffsetXMM.setValue(perfilSeleccionado.getOffsetXMm());
            spOffsetYMM.setValue(perfilSeleccionado.getOffsetYMM());

            // Actualizar vista previa
            reconstruirPreview(panelPreviewRef);
            JOptionPane.showMessageDialog(this, "Perfil aplicado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al aplicar perfil: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Convierte un string de rotación a grados
     */
    private int parseRotation(String rotacionStr) {
        if ("90°".equals(rotacionStr)) return 90;
        if ("-90°".equals(rotacionStr)) return -90;
        if ("180°".equals(rotacionStr)) return 180;
        return 0; // 0°
    }

    /**
     * Convierte grados a string de rotación
     */
    private String getRotationString(int grados) {
        switch (grados) {
            case 90: return "90°";
            case -90: return "-90°";
            case 180: return "180°";
            default: return "0°";
        }
    }

    // Deduce dimensiones en mm para un Media no estándar.
    // 1) Si el nombre tiene patrón "N x M" (pulgadas), convierto a mm.
    // 2) Si no, uso el PageFormat por defecto del servicio (configuración del driver).
    private double[] inferSizeFromMediaOrDriver(Object media, PrintService svc) {
        if (media == null) return null;
        String name = media.toString();
        if (name != null) {
            String s = name.trim().toLowerCase();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*x\\s*(\\d+(?:\\.\\d+)?)").matcher(s);
            if (m.find()) {
                try {
                    double inW = Double.parseDouble(m.group(1));
                    double inH = Double.parseDouble(m.group(2));
                    double mmW = inW * 25.4;
                    double mmH = inH * 25.4;
                    return new double[]{mmW, mmH};
                } catch (Exception ignored) {}
            }
        }
        // Fallback: consultar tamaño de papel por defecto del driver
        if (svc != null) {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintService(svc);
                PageFormat pf = job.defaultPage();
                if (pf != null) {
                    Paper paper = pf.getPaper();
                    if (paper != null) {
                        double mmW = paper.getWidth() * 25.4 / 72.0;
                        double mmH = paper.getHeight() * 25.4 / 72.0;
                        return new double[]{mmW, mmH};
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private int[][] parsePageRanges(String txt, int max) {
        try {
            java.util.List<int[]> ranges = new java.util.ArrayList<>();
            for (String part : txt.split(",")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                if (part.contains("-")) {
                    String[] p = part.split("-");
                    int a = Integer.parseInt(p[0]);
                    int b = Integer.parseInt(p[1]);
                    ranges.add(new int[]{Math.max(1,a), Math.min(max,b)});
                } else {
                    int a = Integer.parseInt(part);
                    ranges.add(new int[]{Math.max(1,a), Math.min(max,a)});
                }
            }
            return ranges.toArray(new int[0][]);
        } catch (Exception e) { return null; }
    }

    private int[][] buildOddRanges(int total) {
        java.util.List<int[]> ranges = new java.util.ArrayList<>();
        for (int i=1;i<=total;i+=2) ranges.add(new int[]{i,i});
        return ranges.toArray(new int[0][]);
    }
    private int[][] buildEvenRanges(int total) {
        java.util.List<int[]> ranges = new java.util.ArrayList<>();
        for (int i=2;i<=total;i+=2) ranges.add(new int[]{i,i});
        return ranges.toArray(new int[0][]);
    }

    private double parseMm(String text, double fallback) {
        if (text == null) return fallback;
        try {
            String t = text.toLowerCase().replace("mm", "").trim();
            return Double.parseDouble(t);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void repaintPreviewPanels() {
        if (panelPreviewRef == null) return;
        for (Component c : panelPreviewRef.getComponents()) {
            if (c != null) c.repaint();
        }
    }

    private void resetVistaPreview() {
        if (panelPreviewRef == null) return;
        for (Component c : panelPreviewRef.getComponents()) {
            if (c instanceof EtiquetaPreviewPanel) {
                ((EtiquetaPreviewPanel) c).resetView();
            }
        }
        panelPreviewRef.repaint();
    }

    private void updateCompatLabel() {
        if (lblCompat == null) return;
        if (!previewSettings.validacion) {
            lblCompat.setText("Compatibilidad: --");
            return;
        }
        int total = 0;
        int pagesConRecorte = 0;
        int pixFuera = 0;
        int pagesConDiferencias = 0;
        int pixDiff = 0;
        if (panelPreviewRef != null) {
            for (Component c : panelPreviewRef.getComponents()) {
                if (c instanceof EtiquetaPreviewPanel) {
                    total++;
                    EtiquetaPreviewPanel p = (EtiquetaPreviewPanel) c;
                    int bad = p.getBadPixelCount();
                    if (bad > 0) {
                        pagesConRecorte++;
                        pixFuera += bad;
                    }
                    int diff = p.getDiffPixelCount();
                    if (diff > 0) {
                        pagesConDiferencias++;
                        pixDiff += diff;
                    }
                }
            }
        }
        if (total <= 0) {
            lblCompat.setText("Compatibilidad: --");
        } else if (pagesConRecorte == 0 && pagesConDiferencias == 0) {
            lblCompat.setText("Compatibilidad: OK");
        } else if (pagesConRecorte > 0 && pagesConDiferencias > 0) {
            lblCompat.setText("Compatibilidad: Recorte (" + pagesConRecorte + "/" + total + ", " + pixFuera + " px) | Diferencias (" + pagesConDiferencias + "/" + total + ", " + pixDiff + " px)");
        } else if (pagesConRecorte > 0) {
            lblCompat.setText("Compatibilidad: Recorte (" + pagesConRecorte + "/" + total + ", " + pixFuera + " px)");
        } else {
            lblCompat.setText("Compatibilidad: Diferencias (" + pagesConDiferencias + "/" + total + ", " + pixDiff + " px)");
        }
    }

    private void nudgeOffset(int dirX, int dirY) {
        double step = parseMm((String) cbPasoAjuste.getSelectedItem(), 0.5);
        double x = (Double) spOffsetXMM.getValue();
        double y = (Double) spOffsetYMM.getValue();
        spOffsetXMM.setValue(x + dirX * step);
        spOffsetYMM.setValue(y + dirY * step);
        impresor.setContenidoOffsetMM((Double) spOffsetXMM.getValue());
        impresor.setContenidoOffsetYMM((Double) spOffsetYMM.getValue());
        reconstruirPreview(panelPreviewRef);
    }

    private void mostrarReporteCompatibilidad() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parámetros\n");
        sb.append("Ancho: ").append(spAnchoMM.getValue()).append(" mm\n");
        sb.append("Alto: ").append(spAltoMM.getValue()).append(" mm\n");
        sb.append("Margen: ").append(spMargenMM.getValue()).append(" mm\n");
        sb.append("Orientación: ").append(cbOrientacion.getSelectedItem()).append("\n");
        sb.append("Offset X: ").append(spOffsetXMM.getValue()).append(" mm\n");
        sb.append("Offset Y: ").append(spOffsetYMM.getValue()).append(" mm\n");
        sb.append("Escala: ").append(cbEscala.getSelectedItem()).append("\n");
        sb.append("Rotación: ").append(cbRotacion.getSelectedItem()).append("\n");
        sb.append("\n");

        if (previewPageFormat != null) {
            double wMm = previewPageFormat.getWidth() * 25.4 / 72.0;
            double hMm = previewPageFormat.getHeight() * 25.4 / 72.0;
            double iwMm = previewPageFormat.getImageableWidth() * 25.4 / 72.0;
            double ihMm = previewPageFormat.getImageableHeight() * 25.4 / 72.0;
            double ixMm = previewPageFormat.getImageableX() * 25.4 / 72.0;
            double iyMm = previewPageFormat.getImageableY() * 25.4 / 72.0;
            sb.append("Papel (driver)\n");
            sb.append(String.format("Tamaño: %.2f x %.2f mm\n", wMm, hMm));
            sb.append(String.format("Área imprimible: %.2f x %.2f mm (x=%.2f, y=%.2f)\n", iwMm, ihMm, ixMm, iyMm));
            sb.append("\n");
        } else {
            sb.append("Papel (driver)\n");
            sb.append("No disponible\n\n");
        }

        int total = 0;
        int conRecorte = 0;
        int conDiferencias = 0;
        if (panelPreviewRef != null) {
            for (Component c : panelPreviewRef.getComponents()) {
                if (c instanceof EtiquetaPreviewPanel) {
                    total++;
                    EtiquetaPreviewPanel p = (EtiquetaPreviewPanel) c;
                    int bad = p.getBadPixelCount();
                    int diff = p.getDiffPixelCount();
                    if (bad > 0) conRecorte++;
                    if (diff > 0) conDiferencias++;
                    sb.append("Página ").append(total).append(": ");
                    if (bad > 0 && diff > 0) sb.append("recorte (" + bad + " px), diferencias (" + diff + " px)\n");
                    else if (bad > 0) sb.append("recorte (" + bad + " px)\n");
                    else if (diff > 0) sb.append("diferencias (" + diff + " px)\n");
                    else sb.append("ok\n");
                }
            }
        }
        sb.append("\nResumen\n");
        if (total == 0) sb.append("Sin páginas\n");
        else if (conRecorte == 0 && conDiferencias == 0) sb.append("OK\n");
        else if (conRecorte > 0 && conDiferencias > 0) sb.append("Recorte en ").append(conRecorte).append("/").append(total).append(", diferencias en ").append(conDiferencias).append("/").append(total).append("\n");
        else if (conRecorte > 0) sb.append("Recorte en ").append(conRecorte).append("/").append(total).append("\n");
        else sb.append("Diferencias en ").append(conDiferencias).append("/").append(total).append("\n");

        JTextArea ta = new JTextArea(sb.toString(), 20, 64);
        ta.setEditable(false);
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        JOptionPane.showMessageDialog(this, sp, "Reporte de compatibilidad", JOptionPane.INFORMATION_MESSAGE);
    }

    private PageFormat buildPreviewPageFormat() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PrintService svc = (PrintService) cbImpresoras.getSelectedItem();
            if (svc != null) job.setPrintService(svc);
            return impresor.buildPageFormat(job);
        } catch (Exception e) {
            try {
                return impresor.buildPageFormat(null);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private void reconstruirPreview(JPanel panelPreview) {
        try {
            previewPageFormat = buildPreviewPageFormat();
            java.util.List<BufferedImage> all = impresor.generarPreviewImagenes(previewPageFormat);
            if (referencePaginas == null || referencePaginas.isEmpty() || referenceModo != impresor.getModo() || (all != null && all.size() != referencePaginas.size())) {
                referencePaginas = all == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(all);
                referenceModo = impresor.getModo();
            }
            java.util.List<BufferedImage> filtered = new java.util.ArrayList<>();
            java.util.List<Integer> filteredIndex = new java.util.ArrayList<>();
            if (all != null) {
                String sel = (String) cbPaginas.getSelectedItem();
                if ("Sólo impares".equals(sel)) {
                    for (int i=0;i<all.size();i++) {
                        int num = i + 1;
                        if (num % 2 == 1) {
                            filtered.add(all.get(i));
                            filteredIndex.add(num);
                        }
                    }
                } else if ("Sólo pares".equals(sel)) {
                    for (int i=0;i<all.size();i++) {
                        int num = i + 1;
                        if (num % 2 == 0) {
                            filtered.add(all.get(i));
                            filteredIndex.add(num);
                        }
                    }
                } else if ("Personalizado".equals(sel)) {
                    int[][] rr = parsePageRanges(tfRango.getText(), all.size());
                    java.util.Set<Integer> keep = new java.util.HashSet<>();
                    if (rr != null) {
                        for (int[] r : rr) {
                            for (int p=r[0]; p<=r[1]; p++) keep.add(p);
                        }
                    }
                    for (int i=0;i<all.size();i++) {
                        int num = i + 1;
                        if (keep.contains(num)) {
                            filtered.add(all.get(i));
                            filteredIndex.add(num);
                        }
                    }
                } else {
                    for (int i=0;i<all.size();i++) {
                        filtered.add(all.get(i));
                        filteredIndex.add(i + 1);
                    }
                }
            }
            paginas = filtered;
            paginasIndex = filteredIndex;
            panelPreview.removeAll();
            int total = paginas == null ? 0 : paginas.size();
            int totalAll = all == null ? 0 : all.size();
            for (int i = 0; i < total; i++) {
                BufferedImage img = paginas.get(i);
                int pageNo = (paginasIndex != null && i < paginasIndex.size()) ? paginasIndex.get(i) : (i + 1);
                BufferedImage ref = (referencePaginas != null && pageNo >= 1 && pageNo <= referencePaginas.size()) ? referencePaginas.get(pageNo - 1) : null;
                EtiquetaPreviewPanel p = new EtiquetaPreviewPanel(img, ref, pageNo, totalAll);
                p.setAlignmentX(Component.LEFT_ALIGNMENT);
                panelPreview.add(p);
                if (i < total - 1) panelPreview.add(Box.createVerticalStrut(10));
            }
            panelPreview.revalidate();
            panelPreview.repaint();
            updateCompatLabel();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error regenerando vista previa: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private final class EtiquetaPreviewPanel extends JPanel {
        private final BufferedImage img;
        private final BufferedImage refImg;
        private final int pageNumber;
        private final int totalPages;
        private double zoom = 1.0;
        private double panX;
        private double panY;
        private Point dragStart;
        private double dragPanX;
        private double dragPanY;
        private double mouseImgX = Double.NaN;
        private double mouseImgY = Double.NaN;

        private int badPixelCount;
        private Rectangle2D.Double badBounds;
        private Rectangle2D.Double inkBounds;
        private int diffPixelCount;
        private Rectangle2D.Double diffBounds;

        EtiquetaPreviewPanel(BufferedImage img, BufferedImage refImg, int pageNumber, int totalPages) {
            this.img = img;
            this.refImg = refImg;
            this.pageNumber = pageNumber;
            this.totalPages = totalPages;
            setBackground(Color.DARK_GRAY);
            setOpaque(true);
            setFocusable(true);
            recomputeAnalysis();
            installInteractions();
        }

        int getBadPixelCount() { return badPixelCount; }
        int getDiffPixelCount() { return diffPixelCount; }

        void resetView() {
            zoom = 1.0;
            panX = 0;
            panY = 0;
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            int w = 560;
            double ratio = (img == null || img.getWidth() <= 0) ? 0.3 : (img.getHeight() / (double) img.getWidth());
            int h = (int) Math.round(w * ratio) + 70;
            h = Math.max(140, Math.min(h, 320));
            return new Dimension(w, h);
        }

        private void installInteractions() {
            addMouseWheelListener(this::onWheel);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                        dragStart = e.getPoint();
                        dragPanX = panX;
                        dragPanY = panY;
                    }
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        focusColumnAt(e.getX(), e.getY());
                    }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        panX = dragPanX + (e.getX() - dragStart.x);
                        panY = dragPanY + (e.getY() - dragStart.y);
                        repaint();
                    }
                }
                @Override public void mouseMoved(MouseEvent e) {
                    updateMousePosition(e.getX(), e.getY());
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    mouseImgX = Double.NaN;
                    mouseImgY = Double.NaN;
                    repaint();
                }
            });
        }

        private void focusColumnAt(int mx, int my) {
            if (previewPageFormat == null || img == null) return;
            Transform t = computeTransform();
            if (t == null) return;

            double ix = (mx - t.originX) / t.scale;
            double iy = (my - t.originY) / t.scale;
            if (ix < 0 || iy < 0 || ix > img.getWidth() || iy > img.getHeight()) return;

            double pxPerPoint = (previewPageFormat.getWidth() <= 0) ? 0 : (img.getWidth() / previewPageFormat.getWidth());
            if (pxPerPoint <= 0) return;

            Rectangle2D.Double[] rects = buildTiraRects(pxPerPoint);
            if (rects == null) return;
            for (Rectangle2D.Double r : rects) {
                if (r != null && r.contains(ix, iy)) {
                    focusOnRect(r);
                    updateMousePosition(mx, my);
                    return;
                }
            }
        }

        private Rectangle2D.Double[] buildTiraRects(double pxPerPoint) {
            if (impresor.getModo() != ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) return null;
            int cols = Math.max(1, impresor.getEtiquetasPorTira());
            Rectangle2D.Double[] rects = new Rectangle2D.Double[cols];
            double ixPt = previewPageFormat.getImageableX();
            double iyPt = previewPageFormat.getImageableY();
            double iwPt = previewPageFormat.getImageableWidth();
            double ihPt = previewPageFormat.getImageableHeight();
            double cellWpt = iwPt / cols;
            for (int c = 0; c < cols; c++) {
                double xPt = ixPt + c * cellWpt;
                rects[c] = new Rectangle2D.Double(xPt * pxPerPoint, iyPt * pxPerPoint, cellWpt * pxPerPoint, ihPt * pxPerPoint);
            }
            return rects;
        }

        private void focusOnRect(Rectangle2D.Double rectImg) {
            if (img == null) return;
            int pad = 10;
            double maxW = Math.max(1, getWidth() - pad * 2);
            double maxH = Math.max(1, getHeight() - pad * 2);
            double fit = Math.min(maxW / img.getWidth(), maxH / img.getHeight());
            if (fit <= 0) return;

            double desiredW = maxW * 0.92;
            double desiredH = maxH * 0.92;
            double zW = desiredW / Math.max(1.0, rectImg.width * fit);
            double zH = desiredH / Math.max(1.0, rectImg.height * fit);
            zoom = Math.max(0.1, Math.min(Math.min(zW, zH), 12.0));

            double s = fit * zoom;
            double baseOx = (getWidth() - img.getWidth() * s) / 2.0;
            double baseOy = (getHeight() - img.getHeight() * s) / 2.0;
            double cx = rectImg.getCenterX();
            double cy = rectImg.getCenterY();
            double targetOx = (getWidth() / 2.0) - (cx * s);
            double targetOy = (getHeight() / 2.0) - (cy * s);
            panX = targetOx - baseOx;
            panY = targetOy - baseOy;
            repaint();
        }

        private void onWheel(MouseWheelEvent e) {
            double factor = (e.getPreciseWheelRotation() < 0) ? 1.1 : 1.0 / 1.1;
            double oldZoom = zoom;
            zoom = Math.max(0.1, Math.min(zoom * factor, 12.0));
            if (Math.abs(zoom - oldZoom) < 1e-9) return;
            adjustPanForZoom(e.getX(), e.getY(), oldZoom, zoom);
            updateMousePosition(e.getX(), e.getY());
            repaint();
        }

        private void updateMousePosition(int mx, int my) {
            Transform t = computeTransform();
            if (t == null) return;
            double ix = (mx - t.originX) / t.scale;
            double iy = (my - t.originY) / t.scale;
            if (ix >= 0 && iy >= 0 && ix <= img.getWidth() && iy <= img.getHeight()) {
                mouseImgX = ix;
                mouseImgY = iy;
            } else {
                mouseImgX = Double.NaN;
                mouseImgY = Double.NaN;
            }
            repaint();
        }

        private void adjustPanForZoom(int mx, int my, double oldZoom, double newZoom) {
            Transform oldT = computeTransform(oldZoom);
            if (oldT == null) return;
            double imgX = (mx - oldT.originX) / oldT.scale;
            double imgY = (my - oldT.originY) / oldT.scale;

            Transform newT = computeTransform(newZoom);
            if (newT == null) return;

            double newOriginX = mx - imgX * newT.scale;
            double newOriginY = my - imgY * newT.scale;

            double baseOriginX = (getWidth() - img.getWidth() * newT.scale) / 2.0;
            double baseOriginY = (getHeight() - img.getHeight() * newT.scale) / 2.0;
            panX = newOriginX - baseOriginX;
            panY = newOriginY - baseOriginY;
        }

        private final class Transform {
            final double originX;
            final double originY;
            final double scale;
            Transform(double originX, double originY, double scale) {
                this.originX = originX;
                this.originY = originY;
                this.scale = scale;
            }
        }

        private Transform computeTransform() { return computeTransform(zoom); }

        private Transform computeTransform(double z) {
            if (img == null) return null;
            int pad = 10;
            int maxW = Math.max(1, getWidth() - pad * 2);
            int maxH = Math.max(1, getHeight() - pad * 2);
            double fit = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
            double s = fit * z;
            double drawW = img.getWidth() * s;
            double drawH = img.getHeight() * s;
            double ox = (getWidth() - drawW) / 2.0 + panX;
            double oy = (getHeight() - drawH) / 2.0 + panY;
            return new Transform(ox, oy, s);
        }

        private void recomputeAnalysis() {
            badPixelCount = 0;
            badBounds = null;
            inkBounds = null;
            diffPixelCount = 0;
            diffBounds = null;
            if (img == null || previewPageFormat == null) return;
            double pxPerPoint = (previewPageFormat.getWidth() <= 0) ? 0 : (img.getWidth() / previewPageFormat.getWidth());
            if (pxPerPoint <= 0) return;

            double ixPt = previewPageFormat.getImageableX();
            double iyPt = previewPageFormat.getImageableY();
            double iwPt = previewPageFormat.getImageableWidth();
            double ihPt = previewPageFormat.getImageableHeight();

            int imgX = (int) Math.round(ixPt * pxPerPoint);
            int imgY = (int) Math.round(iyPt * pxPerPoint);
            int imgW = (int) Math.round(iwPt * pxPerPoint);
            int imgH = (int) Math.round(ihPt * pxPerPoint);

            int mL = Math.max(0, impresor.getMargenIzquierdoPoints());
            int mT = Math.max(0, impresor.getMargenSuperiorPoints());
            int mR = Math.max(0, impresor.getMargenDerechoPoints());
            int mB = Math.max(0, impresor.getMargenInferiorPoints());

            int safeX = (int) Math.round((ixPt + mL) * pxPerPoint);
            int safeY = (int) Math.round((iyPt + mT) * pxPerPoint);
            int safeW = (int) Math.round(Math.max(0, (iwPt - mL - mR)) * pxPerPoint);
            int safeH = (int) Math.round(Math.max(0, (ihPt - mT - mB)) * pxPerPoint);

            int x0 = Math.max(0, imgX);
            int y0 = Math.max(0, imgY);
            int x1 = Math.min(img.getWidth(), imgX + Math.max(0, imgW));
            int y1 = Math.min(img.getHeight(), imgY + Math.max(0, imgH));

            int threshold = 245;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    if (r >= threshold && g >= threshold && b >= threshold) continue;

                    if (inkBounds == null) inkBounds = new Rectangle2D.Double(x, y, 1, 1);
                    else inkBounds.add(x, y);

                    boolean dentroSafe = (x >= safeX && x < safeX + safeW && y >= safeY && y < safeY + safeH);
                    if (!dentroSafe) {
                        badPixelCount++;
                        if (badBounds == null) badBounds = new Rectangle2D.Double(x, y, 1, 1);
                        else badBounds.add(x, y);
                    }
                }
            }

            if (refImg == null) return;
            int w = Math.min(img.getWidth(), refImg.getWidth());
            int h = Math.min(img.getHeight(), refImg.getHeight());
            int tol = 20;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int a = img.getRGB(x, y);
                    int b = refImg.getRGB(x, y);
                    if (a == b) continue;
                    int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
                    int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
                    boolean aInk = ar < threshold || ag < threshold || ab < threshold;
                    boolean bInk = br < threshold || bg < threshold || bb < threshold;
                    if (!aInk && !bInk) continue;

                    int dr = Math.abs(ar - br);
                    int dg = Math.abs(ag - bg);
                    int db = Math.abs(ab - bb);
                    if (aInk != bInk || (dr + dg + db) > tol) {
                        diffPixelCount++;
                        if (diffBounds == null) diffBounds = new Rectangle2D.Double(x, y, 1, 1);
                        else diffBounds.add(x, y);
                    }
                }
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Transform t = computeTransform();
            if (t == null) return;

            int drawW = (int) Math.round(img.getWidth() * t.scale);
            int drawH = (int) Math.round(img.getHeight() * t.scale);
            int dx = (int) Math.round(t.originX);
            int dy = (int) Math.round(t.originY);
            g2.setColor(new Color(30, 30, 30));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.drawImage(img, dx, dy, drawW, drawH, null);

            drawOverlays(g2, t);
        }

        private void drawOverlays(Graphics2D g2, Transform t) {
            if (previewPageFormat == null) return;
            double pxPerPoint = (previewPageFormat.getWidth() <= 0) ? 0 : (img.getWidth() / previewPageFormat.getWidth());
            if (pxPerPoint <= 0) return;

            if (previewSettings.grilla) {
                drawGrid(g2, t, pxPerPoint);
            }
            if (previewSettings.guias) {
                drawGuides(g2, t, pxPerPoint);
            }
            if (previewSettings.validacion) {
                drawValidation(g2, t);
            }
            if (previewSettings.medidas) {
                drawMeasurements(g2, t, pxPerPoint);
            }
        }

        private void drawGrid(Graphics2D g2, Transform t, double pxPerPoint) {
            double pxPerMm = (72.0 / 25.4) * pxPerPoint;
            double stepMm = Math.max(0.5, previewSettings.pasoGrillaMm);
            double stepPx = stepMm * pxPerMm;
            if (stepPx < 4) return;

            int w = img.getWidth();
            int h = img.getHeight();
            Color minor = new Color(0, 0, 0, 25);
            Color major = new Color(0, 0, 0, 55);
            double majorEveryMm = (stepMm <= 2.0) ? 10.0 : 20.0;
            double majorEveryPx = majorEveryMm * pxPerMm;

            for (double x = 0; x <= w + 0.5; x += stepPx) {
                boolean isMajor = (majorEveryPx > 0) && (Math.abs((x % majorEveryPx)) < 0.6 * stepPx);
                g2.setColor(isMajor ? major : minor);
                int sx = (int) Math.round(t.originX + x * t.scale);
                int sy0 = (int) Math.round(t.originY);
                int sy1 = (int) Math.round(t.originY + h * t.scale);
                g2.drawLine(sx, sy0, sx, sy1);
            }
            for (double y = 0; y <= h + 0.5; y += stepPx) {
                boolean isMajor = (majorEveryPx > 0) && (Math.abs((y % majorEveryPx)) < 0.6 * stepPx);
                g2.setColor(isMajor ? major : minor);
                int sy = (int) Math.round(t.originY + y * t.scale);
                int sx0 = (int) Math.round(t.originX);
                int sx1 = (int) Math.round(t.originX + w * t.scale);
                g2.drawLine(sx0, sy, sx1, sy);
            }
        }

        private void drawGuides(Graphics2D g2, Transform t, double pxPerPoint) {
            double ix = previewPageFormat.getImageableX();
            double iy = previewPageFormat.getImageableY();
            double iw = previewPageFormat.getImageableWidth();
            double ih = previewPageFormat.getImageableHeight();

            double imgX = ix * pxPerPoint;
            double imgY = iy * pxPerPoint;
            double imgW = iw * pxPerPoint;
            double imgH = ih * pxPerPoint;

            int sx = (int) Math.round(t.originX + imgX * t.scale);
            int sy = (int) Math.round(t.originY + imgY * t.scale);
            int sw = (int) Math.round(imgW * t.scale);
            int sh = (int) Math.round(imgH * t.scale);

            g2.setColor(new Color(0, 90, 255, 180));
            g2.drawRect(sx, sy, sw, sh);

            int mL = Math.max(0, impresor.getMargenIzquierdoPoints());
            int mT = Math.max(0, impresor.getMargenSuperiorPoints());
            int mR = Math.max(0, impresor.getMargenDerechoPoints());
            int mB = Math.max(0, impresor.getMargenInferiorPoints());

            double safeX = (ix + mL) * pxPerPoint;
            double safeY = (iy + mT) * pxPerPoint;
            double safeW = Math.max(0, (iw - mL - mR)) * pxPerPoint;
            double safeH = Math.max(0, (ih - mT - mB)) * pxPerPoint;

            int ssx = (int) Math.round(t.originX + safeX * t.scale);
            int ssy = (int) Math.round(t.originY + safeY * t.scale);
            int ssw = (int) Math.round(safeW * t.scale);
            int ssh = (int) Math.round(safeH * t.scale);
            g2.setColor(new Color(0, 180, 80, 180));
            g2.drawRect(ssx, ssy, ssw, ssh);

            if (impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                int cols = Math.max(1, impresor.getEtiquetasPorTira());
                if (cols > 1) {
                    g2.setColor(new Color(120, 120, 120, 180));
                    for (int c = 1; c < cols; c++) {
                        double xPt = ix + (iw * c / cols);
                        double xImg = xPt * pxPerPoint;
                        int lx = (int) Math.round(t.originX + xImg * t.scale);
                        g2.drawLine(lx, sy, lx, sy + sh);
                    }
                    for (int c = 0; c < cols; c++) {
                        double cxPt = ix + (iw * (c + 0.5) / cols);
                        double cxImg = cxPt * pxPerPoint;
                        int tx = (int) Math.round(t.originX + cxImg * t.scale);
                        int ty = sy + 14;
                        String label = String.valueOf(c + 1);
                        int tw = g2.getFontMetrics().stringWidth(label);
                        g2.setColor(new Color(0, 0, 0, 140));
                        g2.fillRoundRect(tx - (tw / 2) - 6, ty - 12, tw + 12, 16, 10, 10);
                        g2.setColor(Color.WHITE);
                        g2.drawString(label, tx - (tw / 2), ty);
                        g2.setColor(new Color(120, 120, 120, 180));
                    }
                }
            }
        }

        private void drawValidation(Graphics2D g2, Transform t) {
            if (badBounds != null) {
                g2.setColor(new Color(220, 30, 30, 190));
                int x = (int) Math.round(t.originX + badBounds.x * t.scale);
                int y = (int) Math.round(t.originY + badBounds.y * t.scale);
                int w = (int) Math.round(badBounds.width * t.scale);
                int h = (int) Math.round(badBounds.height * t.scale);
                g2.drawRect(x, y, w, h);
            }
            if (diffBounds != null) {
                g2.setColor(new Color(255, 140, 0, 180));
                int x = (int) Math.round(t.originX + diffBounds.x * t.scale);
                int y = (int) Math.round(t.originY + diffBounds.y * t.scale);
                int w = (int) Math.round(diffBounds.width * t.scale);
                int h = (int) Math.round(diffBounds.height * t.scale);
                g2.drawRect(x, y, w, h);
            }
            if (inkBounds != null) {
                g2.setColor(new Color(0, 0, 0, 80));
                int x = (int) Math.round(t.originX + inkBounds.x * t.scale);
                int y = (int) Math.round(t.originY + inkBounds.y * t.scale);
                int w = (int) Math.round(inkBounds.width * t.scale);
                int h = (int) Math.round(inkBounds.height * t.scale);
                g2.drawRect(x, y, w, h);
            }
        }

        private void drawMeasurements(Graphics2D g2, Transform t, double pxPerPoint) {
            double mmW = previewPageFormat.getWidth() * 25.4 / 72.0;
            double mmH = previewPageFormat.getHeight() * 25.4 / 72.0;
            int cols = impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA ? impresor.getEtiquetasPorTira() : 1;
            double etiquetaW = cols > 0 ? (mmW / cols) : mmW;
            double etiquetaH = mmH;
            String head = "Fila " + pageNumber + "/" + totalPages;
            String dim = String.format("%s  Etiqueta: %.1f x %.1f mm  (%dx por fila)  Zoom: %.0f%%",
                    head, etiquetaW, etiquetaH, cols, zoom * 100.0);

            String cursor = "";
            if (!Double.isNaN(mouseImgX) && !Double.isNaN(mouseImgY)) {
                double ptX = mouseImgX / pxPerPoint;
                double ptY = mouseImgY / pxPerPoint;
                double mmX = ptX * 25.4 / 72.0;
                double mmY = ptY * 25.4 / 72.0;
                cursor = String.format("  Cursor: %.1f, %.1f mm", mmX, mmY);
            }

            int pad = 6;
            java.util.List<String> lines = new java.util.ArrayList<>();
            lines.add(dim + cursor);
            if (impresor.getModo() == ImpresorTermicaPOSDIG2406T.ModoImpresion.ETIQUETA) {
                String[] slots = impresor.getSlotsFilaTira(pageNumber - 1);
                String slotsLine = formatSlotsLine(slots, 22);
                if (!slotsLine.isEmpty()) lines.add(slotsLine);
            }

            int lineH = g2.getFontMetrics().getHeight();
            int boxW = 0;
            for (String s : lines) boxW = Math.max(boxW, g2.getFontMetrics().stringWidth(s));
            int boxH = lineH * lines.size();

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(8, 8, boxW + pad * 2, boxH + pad * 2, 10, 10);
            g2.setColor(Color.WHITE);
            int y = 8 + pad + g2.getFontMetrics().getAscent();
            for (String s : lines) {
                g2.drawString(s, 8 + pad, y);
                y += lineH;
            }
        }

        private String formatSlotsLine(String[] slots, int maxLen) {
            if (slots == null || slots.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < slots.length; i++) {
                if (i > 0) sb.append("  |  ");
                String v = slots[i];
                if (v == null || v.trim().isEmpty()) v = "-";
                v = v.trim();
                if (maxLen > 0 && v.length() > maxLen) v = v.substring(0, Math.max(0, maxLen - 1)) + "…";
                sb.append(i + 1).append(": ").append(v);
            }
            return sb.toString();
        }
    }

    /**
     * Detecta si la impresora seleccionada es una Xprinter XP-420B
     * @param servicio Servicio de impresión a verificar
     * @return true si es una impresora XP-420B, false en caso contrario
     */
    private boolean detectarImpresoraXP420B(PrintService servicio) {
        if (servicio == null) return false;
        
        String nombreImpresora = servicio.getName().toLowerCase();
        
        // Buscar patrones que indiquen XP-420B
        return nombreImpresora.contains("xp-420b") || 
               nombreImpresora.contains("xp420b") ||
               nombreImpresora.contains("xprinter") && nombreImpresora.contains("420") ||
               nombreImpresora.contains("xp-") && nombreImpresora.contains("420") && nombreImpresora.contains("b");
    }

    private FlatSVGIcon safeSvgIcon(String resourcePath, String fallback, int w, int h) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            FlatSVGIcon ic = (url != null) ? new FlatSVGIcon(url).derive(w, h) : new FlatSVGIcon(fallback, w, h);
            BufferedImage img = new BufferedImage(Math.max(w, 1), Math.max(h, 1), BufferedImage.TYPE_INT_ARGB);
            ic.paintIcon(null, img.getGraphics(), 0, 0);
            return ic;
        } catch (Throwable t) {
            return new FlatSVGIcon(fallback, w, h);
        }
    }

    private JPanel buildAccordionContent(JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        for (int i = 0; i < components.length; i++) {
            JComponent comp = components[i];
            if (comp == null) continue;
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            Dimension pref = comp.getPreferredSize();
            if (pref != null) comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
            panel.add(comp);
            if (i < components.length - 1) panel.add(Box.createVerticalStrut(10));
        }
        return panel;
    }

    private JComponent buildSubGroup(String title, JComponent... components) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.putClientProperty(FlatClientProperties.STYLE,
                "arc:12;background:lighten($Panel.background,1%);borderWidth:1;borderColor:lighten($Panel.background,10%)");
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lbl = new JLabel(title);
        lbl.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(lbl);
        outer.add(Box.createVerticalStrut(8));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < components.length; i++) {
            JComponent comp = components[i];
            if (comp == null) continue;
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            Dimension pref = comp.getPreferredSize();
            if (pref != null) comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
            body.add(comp);
            if (i < components.length - 1) body.add(Box.createVerticalStrut(8));
        }
        outer.add(body);
        Dimension pref = outer.getPreferredSize();
        if (pref != null) outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        return outer;
    }

    private JComponent buildTwoButtons(JButton left, JButton right) {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setOpaque(false);
        if (left != null) p.add(left);
        if (right != null) p.add(right);
        return p;
    }

    private static final class AccordionSection extends JPanel {
        private final String title;
        private final String iconText;
        private final JPanel header;
        private final JLabel lblState;
        private final JLabel lblTitle;
        private final JPanel contentWrapper;
        private final JComponent content;
        private boolean expanded;
        private boolean active;
        private Timer animTimer;
        private Runnable onUserExpand;
        private Runnable onStateChange;

        AccordionSection(String title, String iconText, JComponent content, boolean expandedInitially) {
            this.title = title == null ? "" : title;
            this.iconText = iconText == null ? "" : iconText;
            this.content = content == null ? new JPanel() : content;
            this.expanded = expandedInitially;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            putClientProperty(FlatClientProperties.STYLE,
                    "arc:14;background:$Panel.background;borderWidth:1;borderColor:lighten($Panel.background,10%)");
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            header = new JPanel(new BorderLayout(10, 0));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.putClientProperty(FlatClientProperties.STYLE,
                    "arc:14;background:lighten($Panel.background,2%);borderWidth:0");
            header.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            if (!this.iconText.isBlank()) {
                JLabel lblIcon = new JLabel();
                // Asignar iconos SVG según el tipo de sección
                FlatSVGIcon svgIcon = null;
                String iconPath = "";

                switch (this.iconText.toUpperCase()) {
                    case "IMP":
                        iconPath = "raven/menu/icon/printer.svg"; // Icono de impresora
                        break;
                    case "PAG":
                        iconPath = "raven/menu/icon/pages.svg"; // Icono de páginas/copias
                        break;
                    case "PRF":
                        iconPath = "raven/menu/icon/profiles.svg"; // Icono de perfiles
                        break;
                    case "PAPEL":
                        iconPath = "raven/menu/icon/paper_size.svg"; // Icono de papel/tamaño
                        break;
                    case "VISTA":
                        iconPath = "raven/menu/icon/viewer.svg"; // Icono de vista/visualizador
                        break;
                    case "ADV":
                        iconPath = "raven/menu/icon/advanced.svg"; // Icono de configuración avanzada
                        break;
                    case "PER":
                        iconPath = "raven/menu/icon/users.svg"; // Icono de perfiles de impresión
                        break;
                    case "VIS":
                        iconPath = "raven/menu/icon/eye.svg"; // Icono de visualizador
                        break;
                    case "PG":
                        iconPath = "raven/menu/icon/file_alt.svg"; // Icono de páginas
                        break;
                    case "PRN":
                        iconPath = "raven/menu/icon/print.svg"; // Icono de impresora
                        break;
                    case "PAP":
                        iconPath = "raven/menu/icon/file.svg"; // Icono de papel
                        break;
                    default:
                        // Si no hay un icono específico, usar texto entre corchetes como fallback
                        JLabel lblFallback = new JLabel("[" + this.iconText + "]");
                        lblFallback.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:fade($Label.foreground,35%)");
                        left.add(lblFallback);
                        lblIcon = null; // No hay icono específico
                        break;
                }

                if (!iconPath.isEmpty() && lblIcon != null) {
                    try {
                        FlatSVGIcon svgIconTemp = new FlatSVGIcon(iconPath, 16, 16);
                        lblIcon.setIcon(svgIconTemp);
                        left.add(lblIcon);
                    } catch (Exception e) {
                        // Si no se puede cargar el icono SVG, usar texto entre corchetes como fallback
                        JLabel lblFallback = new JLabel("[" + this.iconText + "]");
                        lblFallback.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:fade($Label.foreground,35%)");
                        left.add(lblFallback);
                    }
                }
            }

            lblTitle = new JLabel(this.title);
            lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:+1");
            left.add(lblTitle);

            lblState = new JLabel();
            lblState.putClientProperty(FlatClientProperties.STYLE, "font:+2");
            lblState.setHorizontalAlignment(SwingConstants.RIGHT);
            updateStateIcon();

            header.add(left, BorderLayout.WEST);
            header.add(lblState, BorderLayout.EAST);

            contentWrapper = new JPanel(new BorderLayout());
            contentWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentWrapper.setOpaque(false);
            contentWrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
            contentWrapper.add(this.content, BorderLayout.CENTER);

            add(header);
            add(contentWrapper);

            if (!expandedInitially) {
                contentWrapper.setVisible(false);
                setAnimatedHeight(0);
            }

            header.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    boolean next = !AccordionSection.this.expanded;
                    setExpanded(next, true);
                    if (next && onUserExpand != null) onUserExpand.run();
                }
            });
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension pref = getPreferredSize();
            if (pref == null) return new Dimension(Integer.MAX_VALUE, super.getMaximumSize().height);
            return new Dimension(Integer.MAX_VALUE, pref.height);
        }

        String getTitle() {
            return title;
        }

        String getIconText() {
            return iconText;
        }

        boolean isExpanded() {
            return expanded;
        }

        boolean isActive() {
            return active;
        }

        void setOnUserExpand(Runnable r) {
            this.onUserExpand = r;
        }

        void setOnStateChange(Runnable r) {
            this.onStateChange = r;
        }

        void setExpanded(boolean expanded) {
            setExpanded(expanded, false);
        }

        private void setExpanded(boolean expanded, boolean animate) {
            if (this.expanded == expanded && (!animate || animTimer == null)) return;
            this.expanded = expanded;
            updateStateIcon();
            if (onStateChange != null) onStateChange.run();

            if (!animate) {
                if (expanded) {
                    contentWrapper.setVisible(true);
                    clearAnimatedHeight();
                } else {
                    setAnimatedHeight(0);
                    contentWrapper.setVisible(false);
                }
                revalidate();
                repaint();
                return;
            }

            if (animTimer != null && animTimer.isRunning()) animTimer.stop();

            int start = getCurrentAnimatedHeight();
            if (expanded) contentWrapper.setVisible(true);
            int target = expanded ? getTargetContentHeight() : 0;

            final int durationMs = 160;
            final int intervalMs = 15;
            final long startNs = System.nanoTime();

            animTimer = new Timer(intervalMs, ev -> {
                double t = (System.nanoTime() - startNs) / 1_000_000.0 / durationMs;
                if (t >= 1.0) t = 1.0;
                double ease = 1.0 - Math.pow(1.0 - t, 3);
                int h = (int) Math.round(start + (target - start) * ease);
                setAnimatedHeight(h);
                revalidate();
                repaint();
                if (t >= 1.0) {
                    ((Timer) ev.getSource()).stop();
                    if (AccordionSection.this.expanded) {
                        clearAnimatedHeight();
                    } else {
                        setAnimatedHeight(0);
                        contentWrapper.setVisible(false);
                    }
                    revalidate();
                    repaint();
                }
            });
            animTimer.start();
        }

        void setActive(boolean active) {
            if (this.active == active) return;
            this.active = active;
            if (active) {
                putClientProperty(FlatClientProperties.STYLE,
                        "arc:14;background:lighten($Panel.background,1%);borderWidth:1;borderColor:$Component.focusColor");
                header.putClientProperty(FlatClientProperties.STYLE,
                        "arc:14;background:lighten($Panel.background,4%);borderWidth:0");
                lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:+1;foreground:$Component.focusColor");
            } else {
                putClientProperty(FlatClientProperties.STYLE,
                        "arc:14;background:$Panel.background;borderWidth:1;borderColor:lighten($Panel.background,10%)");
                header.putClientProperty(FlatClientProperties.STYLE,
                        "arc:14;background:lighten($Panel.background,2%);borderWidth:0");
                lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:+1");
            }
            repaint();
        }

        private void updateStateIcon() {
            lblState.setText(expanded ? "▾" : "▸");
        }

        private int getTargetContentHeight() {
            int contentH = content.getPreferredSize() != null ? content.getPreferredSize().height : 0;
            java.awt.Insets in = contentWrapper.getInsets();
            return Math.max(0, contentH + (in != null ? in.top + in.bottom : 0));
        }

        private int getCurrentAnimatedHeight() {
            Dimension ps = contentWrapper.getPreferredSize();
            if (ps != null && ps.height >= 0) return ps.height;
            return contentWrapper.getHeight();
        }

        private void setAnimatedHeight(int h) {
            int height = Math.max(0, h);
            contentWrapper.setPreferredSize(new Dimension(0, height));
            contentWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            contentWrapper.setMinimumSize(new Dimension(0, height));
        }

        private void clearAnimatedHeight() {
            contentWrapper.setPreferredSize(null);
            contentWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            contentWrapper.setMinimumSize(new Dimension(0, 0));
        }
    }
}
