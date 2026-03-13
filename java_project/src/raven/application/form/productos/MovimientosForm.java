package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.Color;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.clases.productos.ServiceMovement;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelMovement;
import raven.datetime.component.date.DatePicker;
import raven.modal.Toast;

/**
 * Panel mejorado para visualizar y gestionar movimientos de inventario
 * con filtros avanzados, estadísticas y exportación de reportes
 */
public class MovimientosForm extends javax.swing.JPanel {

    // Estilos FlatLaf
    private static final String STYLE_PANEL = "arc:25;background:$Login.background";
    private static final String STYLE_PANEL_STATS = "arc:15;background:lighten($Login.background,5%)";
    private static final String STYLE_BUTTON = "arc:15";
    private static final String STYLE_TEXTFIELD = "arc:10;borderColor:#D0D0D0;focusWidth:1;focusColor:$Menu.foreground";

    // Servicio de movimientos
    private final ServiceMovement service = new ServiceMovement();

    // Componentes UI
    private JLabel lblTitulo;
    private JPanel panelFiltros;
    private JPanel panelEstadisticas;
    private JPanel panelTabla;
    private JTable table;
    private JScrollPane scroll;

    // Filtros
    private JFormattedTextField txtFechaInicio;
    private JFormattedTextField txtFechaFin;
    private DatePicker datePickerInicio;
    private DatePicker datePickerFin;
    private JComboBox<String> cmbTipoMovimiento;
    private JTextField txtBuscarProducto;

    // Botones
    private JButton btnFiltrar;
    private JButton btnLimpiar;
    private JButton btnExportPDF;
    private JButton btnExportExcel;

    // Estadísticas
    private JLabel lblTotal;
    private JLabel lblEntradas;
    private JLabel lblSalidas;
    private JLabel lblAjustes;

    // Iconos
    private FontIcon iconFilter;
    private FontIcon iconClear;
    private FontIcon iconPdf;
    private FontIcon iconExcel;

    // Lista actual de movimientos (para exportación)
    private List<ModelMovement> movimientosActuales;

    public MovimientosForm() {
        initComponents();
        configurarEstilos();
        cargarDatosIniciales();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Crear iconos
        var iconColor = UIManager.getColor("Label.foreground");
        iconFilter = createIcon(FontAwesomeSolid.FILTER, iconColor);
        iconClear = createIcon(FontAwesomeSolid.TRASH_ALT, iconColor);
        iconPdf = createIcon(FontAwesomeSolid.FILE_PDF, iconColor);
        iconExcel = createIcon(FontAwesomeSolid.FILE_EXCEL, iconColor);

        // Título principal
        lblTitulo = new JLabel("MOVIMIENTOS DE INVENTARIO", SwingConstants.CENTER);
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");

        // Panel superior (filtros + estadísticas)
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.setOpaque(false);

        panelFiltros = crearPanelFiltros();
        panelEstadisticas = crearPanelEstadisticas();

        panelSuperior.add(panelFiltros, BorderLayout.NORTH);
        panelSuperior.add(panelEstadisticas, BorderLayout.SOUTH);

        // Panel de tabla
        panelTabla = crearPanelTabla();

        // Agregar componentes al panel principal
        add(lblTitulo, BorderLayout.NORTH);
        add(panelSuperior, BorderLayout.CENTER);

        // Wrap la tabla en un panel para mejor control de layout
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(panelSuperior, BorderLayout.NORTH);
        centerPanel.add(panelTabla, BorderLayout.CENTER);

        add(lblTitulo, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private FontIcon createIcon(org.kordamp.ikonli.Ikon ikon, Color color) {
        FontIcon icon = FontIcon.of(ikon);
        icon.setIconSize(16);
        icon.setIconColor(color);
        return icon;
    }

    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Fila 1: Fechas y tipo de movimiento
        gbc.gridy = 0;

        // Fecha Inicio
        gbc.gridx = 0;
        panel.add(new JLabel("Desde:"), gbc);

        gbc.gridx = 1;
        txtFechaInicio = new JFormattedTextField();
        txtFechaInicio.setColumns(10);
        txtFechaInicio.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        datePickerInicio = new DatePicker();
        datePickerInicio.setCloseAfterSelected(true);
        datePickerInicio.setEditor(txtFechaInicio);
        datePickerInicio.setDateSelectionAble(d -> !d.isAfter(LocalDate.now()));
        panel.add(txtFechaInicio, gbc);

        // Fecha Fin
        gbc.gridx = 2;
        panel.add(new JLabel("Hasta:"), gbc);

        gbc.gridx = 3;
        txtFechaFin = new JFormattedTextField();
        txtFechaFin.setColumns(10);
        txtFechaFin.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        datePickerFin = new DatePicker();
        datePickerFin.setCloseAfterSelected(true);
        datePickerFin.setEditor(txtFechaFin);
        datePickerFin.setDateSelectionAble(d -> !d.isAfter(LocalDate.now()));
        panel.add(txtFechaFin, gbc);

        // Tipo de movimiento
        gbc.gridx = 4;
        panel.add(new JLabel("Tipo:"), gbc);

        gbc.gridx = 5;
        cmbTipoMovimiento = new JComboBox<>();
        cmbTipoMovimiento.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        cmbTipoMovimiento.setPreferredSize(new Dimension(150, 30));
        panel.add(cmbTipoMovimiento, gbc);

        // Fila 2: Búsqueda y botones
        gbc.gridy = 1;

        // Búsqueda de producto
        gbc.gridx = 0;
        panel.add(new JLabel("Producto:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        txtBuscarProducto = new JTextField();
        txtBuscarProducto.setColumns(20);
        txtBuscarProducto.putClientProperty(FlatClientProperties.STYLE, STYLE_TEXTFIELD);
        txtBuscarProducto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre...");
        panel.add(txtBuscarProducto, gbc);
        gbc.gridwidth = 1;

        // Botones
        gbc.gridx = 3;
        btnFiltrar = new JButton("Filtrar", iconFilter);
        btnFiltrar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON + ";background:$Component.accentColor");
        btnFiltrar.addActionListener(e -> aplicarFiltros());
        panel.add(btnFiltrar, gbc);

        gbc.gridx = 4;
        btnLimpiar = new JButton("Limpiar", iconClear);
        btnLimpiar.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON + ";background:$App.accent.red");
        btnLimpiar.addActionListener(e -> limpiarFiltros());
        panel.add(btnLimpiar, gbc);

        gbc.gridx = 5;
        btnExportPDF = new JButton("PDF", iconPdf);
        btnExportPDF.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON + ";background:#D32F2F");
        btnExportPDF.addActionListener(e -> exportarPDF());
        panel.add(btnExportPDF, gbc);

        gbc.gridx = 6;
        btnExportExcel = new JButton("Excel", iconExcel);
        btnExportExcel.putClientProperty(FlatClientProperties.STYLE, STYLE_BUTTON + ";background:#1D6F42");
        btnExportExcel.addActionListener(e -> exportarExcel());
        panel.add(btnExportExcel, gbc);

        // Enter para buscar
        txtBuscarProducto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    aplicarFiltros();
                }
            }
        });

        return panel;
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Crear cards de estadísticas
        lblTotal = crearStatLabel("Resumen Total:", "0", new Color(33, 150, 243));
        lblEntradas = crearStatLabel(" Entradas:", "0", new Color(76, 175, 80));
        lblSalidas = crearStatLabel(" Salidas:", "0", new Color(244, 67, 54));
        lblAjustes = crearStatLabel("Actualizando Ajustes:", "0", new Color(255, 193, 7));

        panel.add(lblTotal);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(lblEntradas);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(lblSalidas);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(lblAjustes);

        return panel;
    }

    private JLabel crearStatLabel(String titulo, String valor, Color color) {
        JLabel label = new JLabel("<html><b>" + titulo + "</b> <span style='color:" +
                String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()) +
                ";font-size:14px;'>" + valor + "</span></html>");
        label.putClientProperty(FlatClientProperties.STYLE, "font:+2");
        return label;
    }

    private void actualizarEstadisticas(String total, String entradas, String salidas, String ajustes) {
        lblTotal.setText(
                "<html><b>Resumen Total:</b> <span style='color:#2196F3;font-size:14px;'>" + total + "</span></html>");
        lblEntradas.setText(
                "<html><b> Entradas:</b> <span style='color:#4CAF50;font-size:14px;'>" + entradas + "</span></html>");
        lblSalidas.setText(
                "<html><b> Salidas:</b> <span style='color:#F44336;font-size:14px;'>" + salidas + "</span></html>");
        lblAjustes.setText(
                "<html><b>Actualizando Ajustes:</b> <span style='color:#FFC107;font-size:14px;'>" + ajustes + "</span></html>");
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.putClientProperty(FlatClientProperties.STYLE, STYLE_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Crear tabla
        String[] columnas = { "Producto", "Color", "Talla", "Tipo Movimiento", "Cantidad", "Fecha", "Referencia",
                "Observaciones" };
        DefaultTableModel model = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        table.getTableHeader().setReorderingAllowed(false);

        // Configurar ancho de columnas
        configurarColumnasTabla();

        // Renderizador para tipo de movimiento con colores
        table.getColumnModel().getColumn(3).setCellRenderer(new TipoMovimientoCellRenderer());

        // Renderizador para cantidad con signo
        table.getColumnModel().getColumn(4).setCellRenderer(new CantidadCellRenderer());

        // Renderizador centrado para otras celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 3 && i != 4) { // Excepto tipo y cantidad que tienen renderizadores propios
                table.getColumnModel().getColumn(i).setCellRenderer(new AlternatingRowRenderer());
            }
        }

        // Doble clic para ver detalles
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    mostrarDetalleMovimiento(table.getSelectedRow());
                }
            }
        });

        scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999;trackInsets:3,3,3,3;thumbInsets:3,3,3,3");

        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void configurarColumnasTabla() {
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(150); // Producto
        columnModel.getColumn(1).setPreferredWidth(80); // Color
        columnModel.getColumn(2).setPreferredWidth(60); // Talla
        columnModel.getColumn(3).setPreferredWidth(120); // Tipo
        columnModel.getColumn(4).setPreferredWidth(80); // Cantidad
        columnModel.getColumn(5).setPreferredWidth(100); // Fecha
        columnModel.getColumn(6).setPreferredWidth(100); // Referencia
        columnModel.getColumn(7).setPreferredWidth(150); // Observaciones
    }

    private void configurarEstilos() {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new java.awt.Font(FlatRobotoFont.FAMILY, java.awt.Font.PLAIN, 13));

        // Estilo del encabezado de tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:35;hoverBackground:null;pressedBackground:null;separatorColor:$Login.background;font:bold");

        // Estilo de la tabla
        table.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;intercellSpacing:0,1;cellFocusColor:$TableHeader.hoverBackground;" +
                        "selectionBackground:$TableHeader.hoverBackground;selectionForeground:$Table.foreground;background:$Login.background");
    }

    private void cargarDatosIniciales() {
        try {
            conexion.getInstance().connectToDatabase();

            // Cargar tipos de movimiento en combo
            List<String> tipos = service.getTiposMovimiento();
            for (String tipo : tipos) {
                cmbTipoMovimiento.addItem(tipo);
            }

            // Cargar todos los movimientos
            cargarMovimientos(null, null, null, null);

            conexion.getInstance().close();
        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarMovimientos(LocalDate fechaInicio, LocalDate fechaFin,
            String tipoMovimiento, String nombreProducto) {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            movimientosActuales = service.getFiltered(fechaInicio, fechaFin, tipoMovimiento, nombreProducto);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (ModelMovement m : movimientosActuales) {
                model.addRow(new Object[] {
                        m.getNombreProducto(),
                        m.getColor(),
                        m.getTalla(),
                        m.getTipoMovimiento(),
                        m.getCantidad(),
                        m.getFechaMovimiento() != null ? m.getFechaMovimiento().format(formatter) : "N/A",
                        m.getTipoReferencia() != null ? m.getTipoReferencia() : "",
                        m.getObservaciones() != null ? m.getObservaciones() : ""
                });
            }

            // Actualizar estadísticas
            Map<String, Integer> stats = service.getEstadisticas(fechaInicio, fechaFin);
            actualizarEstadisticas(
                    String.valueOf(stats.get("total")),
                    String.valueOf(stats.get("entradas")),
                    String.valueOf(stats.get("salidas")),
                    String.valueOf(stats.get("ajustes")));

        } catch (SQLException e) {
            Toast.show(this, Toast.Type.ERROR, "Error al cargar movimientos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void aplicarFiltros() {
        LocalDate fechaInicio = null;
        LocalDate fechaFin = null;

        if (datePickerInicio.isDateSelected()) {
            fechaInicio = datePickerInicio.getSelectedDate();
        }
        if (datePickerFin.isDateSelected()) {
            fechaFin = datePickerFin.getSelectedDate();
        }

        String tipoMovimiento = (String) cmbTipoMovimiento.getSelectedItem();
        String nombreProducto = txtBuscarProducto.getText().trim();

        cargarMovimientos(fechaInicio, fechaFin, tipoMovimiento, nombreProducto);
        Toast.show(this, Toast.Type.SUCCESS, "Filtros aplicados");
    }

    private void limpiarFiltros() {
        datePickerInicio.clearSelectedDate();
        datePickerFin.clearSelectedDate();
        txtFechaInicio.setText("");
        txtFechaFin.setText("");
        cmbTipoMovimiento.setSelectedIndex(0);
        txtBuscarProducto.setText("");

        cargarMovimientos(null, null, null, null);
        Toast.show(this, Toast.Type.INFO, "Filtros limpiados");
    }

    private void mostrarDetalleMovimiento(int rowIndex) {
        if (movimientosActuales != null && rowIndex >= 0 && rowIndex < movimientosActuales.size()) {
            ModelMovement movimiento = movimientosActuales.get(rowIndex);
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            DetalleMovimientoDialog dialog = new DetalleMovimientoDialog(parent, movimiento);
            dialog.setVisible(true);
        }
    }

    // ==================== EXPORTACIÓN PDF ====================

    private void exportarPDF() {
        if (movimientosActuales == null || movimientosActuales.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "No hay datos para exportar");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar reporte PDF");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        fileChooser.setSelectedFile(new File("Movimientos_" + sdf.format(new Date()) + ".pdf"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }

            try {
                generarPDF(file);
                Toast.show(this, Toast.Type.SUCCESS, "PDF generado exitosamente");

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            } catch (Exception e) {
                Toast.show(this, Toast.Type.ERROR, "Error al generar PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void generarPDF(File file) throws DocumentException, FileNotFoundException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Título
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Reporte de Movimientos de Inventario", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Paragraph fecha = new Paragraph("Generado: " + sdf.format(new Date()), normalFont);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        fecha.setSpacingAfter(15);
        document.add(fecha);

        // Tabla
        PdfPTable pdfTable = new PdfPTable(8);
        pdfTable.setWidthPercentage(100);
        pdfTable.setWidths(new float[] { 2f, 1f, 0.8f, 1.5f, 1f, 1.2f, 1.2f, 2f });

        // Encabezados
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        String[] headers = { "Producto", "Color", "Talla", "Tipo", "Cantidad", "Fecha", "Referencia", "Observaciones" };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(33, 150, 243));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            pdfTable.addCell(cell);
        }

        // Datos
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (ModelMovement m : movimientosActuales) {
            pdfTable.addCell(new Phrase(m.getNombreProducto(), dataFont));
            pdfTable.addCell(new Phrase(m.getColor() != null ? m.getColor() : "", dataFont));
            pdfTable.addCell(new Phrase(m.getTalla() != null ? m.getTalla() : "", dataFont));
            pdfTable.addCell(new Phrase(m.getTipoMovimiento(), dataFont));
            pdfTable.addCell(new Phrase(String.valueOf(m.getCantidad()), dataFont));
            pdfTable.addCell(new Phrase(m.getFechaMovimiento() != null ? m.getFechaMovimiento().format(formatter) : "",
                    dataFont));
            pdfTable.addCell(new Phrase(m.getTipoReferencia() != null ? m.getTipoReferencia() : "", dataFont));
            pdfTable.addCell(new Phrase(m.getObservaciones() != null ? m.getObservaciones() : "", dataFont));
        }

        document.add(pdfTable);

        // Resumen
        Paragraph resumen = new Paragraph("\nTotal de registros: " + movimientosActuales.size(), normalFont);
        document.add(resumen);

        document.close();
    }

    // ==================== EXPORTACIÓN EXCEL ====================

    private void exportarExcel() {
        if (movimientosActuales == null || movimientosActuales.isEmpty()) {
            Toast.show(this, Toast.Type.WARNING, "No hay datos para exportar");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar reporte Excel");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        fileChooser.setSelectedFile(new File("Movimientos_" + sdf.format(new Date()) + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            try {
                generarExcel(file);
                Toast.show(this, Toast.Type.SUCCESS, "Excel generado exitosamente");

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            } catch (Exception e) {
                Toast.show(this, Toast.Type.ERROR, "Error al generar Excel: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void generarExcel(File file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Movimientos");

            // Estilo de encabezado
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Producto", "Color", "Talla", "Tipo Movimiento", "Cantidad", "Fecha", "Referencia",
                    "Observaciones" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int rowNum = 1;

            for (ModelMovement m : movimientosActuales) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getNombreProducto());
                row.createCell(1).setCellValue(m.getColor() != null ? m.getColor() : "");
                row.createCell(2).setCellValue(m.getTalla() != null ? m.getTalla() : "");
                row.createCell(3).setCellValue(m.getTipoMovimiento());
                row.createCell(4).setCellValue(m.getCantidad());
                row.createCell(5)
                        .setCellValue(m.getFechaMovimiento() != null ? m.getFechaMovimiento().format(formatter) : "");
                row.createCell(6).setCellValue(m.getTipoReferencia() != null ? m.getTipoReferencia() : "");
                row.createCell(7).setCellValue(m.getObservaciones() != null ? m.getObservaciones() : "");
            }

            // Auto-size columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Guardar
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    // ==================== RENDERIZADORES ====================

    /**
     * Renderizador para colorear el tipo de movimiento
     */
    private class TipoMovimientoCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            if (value != null && !isSelected) {
                String tipo = value.toString().toLowerCase();
                String color;

                if (tipo.contains("entrada")) {
                    color = "#4CAF50"; // Verde
                } else if (tipo.contains("salida")) {
                    color = "#F44336"; // Rojo
                } else if (tipo.contains("ajuste")) {
                    color = "#FFC107"; // Amarillo
                } else {
                    color = "#2196F3"; // Azul
                }

                label.setText("<html><div style='padding:3px 8px;border-radius:8px;background-color:" +
                        color + ";color:white;font-weight:bold;'>" + value + "</div></html>");
            }

            // Alternar colores de fila
            if (!isSelected) {
                label.setBackground(
                        row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
            }

            return label;
        }
    }

    /**
     * Renderizador para mostrar cantidad con signo y color
     */
    private class CantidadCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            if (value != null) {
                // Obtener el tipo de movimiento de la columna 3
                Object tipoObj = table.getValueAt(row, 3);
                String tipo = tipoObj != null ? tipoObj.toString().toLowerCase() : "";
                int cantidad = Integer.parseInt(value.toString());

                if (tipo.contains("entrada")) {
                    label.setForeground(new Color(76, 175, 80));
                    label.setText("+" + cantidad);
                } else if (tipo.contains("salida")) {
                    label.setForeground(new Color(244, 67, 54));
                    label.setText("-" + cantidad);
                } else {
                    label.setForeground(new Color(255, 193, 7));
                    label.setText(String.valueOf(cantidad));
                }

                if (isSelected) {
                    label.setForeground(table.getSelectionForeground());
                }
            }

            // Alternar colores de fila
            if (!isSelected) {
                label.setBackground(
                        row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
            }

            return label;
        }
    }

    /**
     * Renderizador con filas alternadas
     */
    private class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (!isSelected) {
                setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
            }

            return this;
        }
    }
}

