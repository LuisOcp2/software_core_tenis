package raven.clases.reportes;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ResumenCierreCaja;
import raven.controlador.principal.AppConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * Clase para generar reportes PDF de cierre de caja.
 * Utiliza iText 5 para la generación del documento.
 * 
 * @author Sistema
 * @version 1.0
 */
public class ReporteCierreCajaPDF {

    private static final BaseColor COLOR_ENCABEZADO = new BaseColor(33, 150, 243); // Azul Empresa
    private static final BaseColor COLOR_FONDO_TABLA = new BaseColor(240, 248, 255); // Azul muy claro
    private static final Font FONT_TITULO = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, COLOR_ENCABEZADO);
    private static final Font FONT_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

    private final DecimalFormat formatoMoneda = new DecimalFormat("$#,##0");
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void generarPDF(ModelCajaMovimiento movimiento, ResumenCierreCaja resumen, java.awt.Component parent) {
        try {
            // Configurar diálogo de guardado
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar Reporte de Cierre de Caja");
            String nombreArchivo = "Cierre_Caja_" + movimiento.getIdMovimiento() + "_" +
                    movimiento.getFechaCierre().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            fileChooser.setSelectedFile(new File(nombreArchivo));
            fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));

            if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                archivo = new File(archivo.getAbsolutePath() + ".pdf");
            }

            // Crear documento
            Document document = new Document(PageSize.LETTER);
            PdfWriter.getInstance(document, new FileOutputStream(archivo));
            document.open();

            // 1. Agregar contenido
            agregarEncabezado(document, movimiento);
            agregarInformacionGeneral(document, movimiento);
            agregarResumenFinanciero(document, movimiento, resumen);
            agregarDesglosePagos(document, resumen);
            agregarDetalleEgresos(document, resumen);

            // 2. Pie de página
            agregarPiePagina(document);

            document.close();

            // 3. Abrir archivo
            if (JOptionPane.showConfirmDialog(parent, "Reporte generado correctamente. ¿Desea abrirlo?",
                    "Éxito", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Desktop.getDesktop().open(archivo);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error generando PDF: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarEncabezado(Document doc, ModelCajaMovimiento mov) throws DocumentException {
        // Logo y Título Principal
        Paragraph pOrg = new Paragraph(AppConfig.name != null ? AppConfig.name : "EMPRESA", FONT_TITULO);
        pOrg.setAlignment(Element.ALIGN_CENTER);
        doc.add(pOrg);

        Paragraph pTitulo = new Paragraph("REPORTE DE CIERRE DE CAJA", FONT_SUBTITULO);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingBefore(5);
        doc.add(pTitulo);

        Paragraph pId = new Paragraph("Movimiento #" + mov.getIdMovimiento(), FONT_NORMAL);
        pId.setAlignment(Element.ALIGN_CENTER);
        pId.setSpacingAfter(20);
        doc.add(pId);

        doc.add(new Paragraph(" ")); // Espacio
    }

    private void agregarInformacionGeneral(Document doc, ModelCajaMovimiento mov) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 30, 70 });
        table.setSpacingAfter(15);

        addCellHeader(table, "INFORMACIÓN GENERAL");

        addInfoRow(table, "Caja:", mov.getNombreCaja());
        addInfoRow(table, "Usuario:", mov.getNombreUsuario());
        addInfoRow(table, "Apertura:", mov.getFechaApertura().format(formatoFecha));
        addInfoRow(table, "Cierre:", mov.getFechaCierre().format(formatoFecha));

        long minutos = java.time.Duration.between(mov.getFechaApertura(), mov.getFechaCierre()).toMinutes();
        long horas = minutos / 60;
        long mins = minutos % 60;
        addInfoRow(table, "Duración:", String.format("%dh %dm", horas, mins));

        doc.add(table);
    }

    private void agregarResumenFinanciero(Document doc, ModelCajaMovimiento mov, ResumenCierreCaja resumen)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 60, 40 });
        table.setSpacingAfter(15);

        addCellHeader(table, "RESUMEN FINANCIERO");

        addMoneyRow(table, "Monto Inicial (Base):", BigDecimal.valueOf(mov.getMontoInicial()));
        addMoneyRow(table, "Total Ventas Contado:", resumen.getMontoTotalVentas());
        addMoneyRow(table, "Total Pagos Recibidos:", new BigDecimal(resumen.getTotalPagosRecibidos())); // Cantidad, no
                                                                                                        // dinero

        // Egresos
        BigDecimal totalEgresos = resumen.getMontoTotalGastos().add(resumen.getMontoTotalCompras());
        addMoneyRow(table, "(-) Total Egresos:", totalEgresos);

        // Totales
        BigDecimal montoEsperado = BigDecimal.valueOf(mov.getMontoInicial())
                .add(resumen.getMontoTotalVentas())
                .subtract(totalEgresos);

        addMoneyRow(table, "Monto Esperado en Caja:", montoEsperado);
        addMoneyRow(table, "Monto Final (Real):", BigDecimal.valueOf(mov.getMontoFinal()));

        // Diferencia
        BigDecimal diferencia = BigDecimal.valueOf(mov.getMontoFinal()).subtract(montoEsperado);
        PdfPCell cellLabel = new PdfPCell(new Phrase("Diferencia:", FONT_BOLD));
        cellLabel.setBorderWidthBottom(1);
        cellLabel.setPadding(5);
        table.addCell(cellLabel);

        Font fontDif = new Font(FONT_BOLD);
        if (diferencia.compareTo(BigDecimal.ZERO) < 0)
            fontDif.setColor(BaseColor.RED);
        else if (diferencia.compareTo(BigDecimal.ZERO) > 0)
            fontDif.setColor(BaseColor.BLUE);
        else
            fontDif.setColor(new BaseColor(0, 128, 0)); // Verde

        PdfPCell cellValue = new PdfPCell(new Phrase(formatoMoneda.format(diferencia), fontDif));
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValue.setBorderWidthBottom(1);
        cellValue.setPadding(5);
        table.addCell(cellValue);

        // Estado
        String estado = "CUADRADO";
        if (diferencia.compareTo(BigDecimal.ZERO) > 0)
            estado = "SOBRANTE";
        else if (diferencia.compareTo(BigDecimal.ZERO) < 0)
            estado = "FALTANTE";

        addInfoRow(table, "Estado:", estado);

        doc.add(table);
    }

    private void agregarDesglosePagos(Document doc, ResumenCierreCaja resumen) throws DocumentException {
        Paragraph p = new Paragraph("DESGLOSE DE MEDIOS DE PAGO", FONT_SUBTITULO);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        doc.add(p);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 50, 20, 30 }); // Tipo, Cantidad, Total
        table.setSpacingAfter(15);

        // Headers
        addTableHeader(table, "Tipo de Pago");
        addTableHeader(table, "Cant.");
        addTableHeader(table, "Monto");

        for (ResumenCierreCaja.DetallePago pago : resumen.getDetallesPorTipo().values()) {
            table.addCell(createCell(pago.getTipoPago(), Element.ALIGN_LEFT));
            table.addCell(createCell(String.valueOf(pago.getCantidadPagos()), Element.ALIGN_CENTER));
            table.addCell(createCell(formatoMoneda.format(pago.getMontoTotal()), Element.ALIGN_RIGHT));
        }

        if (resumen.getDetallesPorTipo().isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("No hay pagos registrados", FONT_NORMAL));
            cell.setColspan(3);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            table.addCell(cell);
        }

        doc.add(table);
    }

    private void agregarDetalleEgresos(Document doc, ResumenCierreCaja resumen) throws DocumentException {
        Paragraph p = new Paragraph("RESUMEN DE EGRESOS", FONT_SUBTITULO);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        doc.add(p);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 70, 30 });
        table.setSpacingAfter(15);

        addMoneyRow(table, "Gastos Operativos (" + resumen.getTotalGastos() + "):", resumen.getMontoTotalGastos());
        addMoneyRow(table, "Compras Externas (" + resumen.getTotalCompras() + "):", resumen.getMontoTotalCompras());

        doc.add(table);
    }

    private void agregarPiePagina(Document doc) throws DocumentException {
        Paragraph p = new Paragraph("\n\n\n\n___________________________\nFirma Responsable", FONT_NORMAL);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);

        String fechaImpresion = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .format(java.time.LocalDateTime.now());
        Paragraph pFecha = new Paragraph("\nGenerado el: " + fechaImpresion,
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pFecha);
    }

    // ==================== UTILIDADES ====================

    private void addCellHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setColspan(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        cell.setBorderWidthBottom(1);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FONT_BOLD));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(5);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, FONT_NORMAL));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValue.setPadding(5);
        table.addCell(cellValue);
    }

    private void addMoneyRow(PdfPTable table, String label, BigDecimal value) {
        addInfoRow(table, label, formatoMoneda.format(value));
    }

    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_NORMAL));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }
}
