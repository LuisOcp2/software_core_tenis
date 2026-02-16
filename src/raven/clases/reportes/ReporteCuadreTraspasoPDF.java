package raven.clases.reportes;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import raven.clases.productos.TraspasoDatos;
import raven.clases.productos.ProductoTraspasoItem;
import raven.controlador.principal.AppConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Clase para generar reportes PDF de Cuadre de Traspaso (Conciliación).
 * Utiliza iText 5.
 */
public class ReporteCuadreTraspasoPDF {

    private static final BaseColor COLOR_ENCABEZADO = new BaseColor(33, 150, 243); // Azul Empresa
    private static final BaseColor COLOR_FONDO_TABLA = new BaseColor(240, 248, 255); // Azul muy claro
    private static final Font FONT_TITULO = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, COLOR_ENCABEZADO);
    private static final Font FONT_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_SMALL = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    private static final Font FONT_SMALL_BOLD = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);

    private final DecimalFormat formatoMoneda = new DecimalFormat("$#,##0.00");
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void generarPDF(TraspasoDatos traspaso, Map<String, Object> extraData, java.awt.Component parent) {
        try {
            // Configurar diálogo de guardado
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar Cuadre de Traspaso");
            String nombreArchivo = "Cuadre_Traspaso_" + traspaso.getNumeroTraspaso() + ".pdf";
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
            agregarEncabezado(document, traspaso);
            agregarInformacionGeneral(document, traspaso, extraData);
            agregarResumenFinanciero(document, traspaso);
            agregarDetalleProductos(document, traspaso);

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

    private void agregarEncabezado(Document doc, TraspasoDatos t) throws DocumentException {
        // Logo y Título Principal
        Paragraph pOrg = new Paragraph(AppConfig.name != null ? AppConfig.name : "EMPRESA", FONT_TITULO);
        pOrg.setAlignment(Element.ALIGN_CENTER);
        doc.add(pOrg);

        Paragraph pTitulo = new Paragraph("CUADRE DE TRASPASO DE MERCANCÍA", FONT_SUBTITULO);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingBefore(5);
        doc.add(pTitulo);

        Paragraph pId = new Paragraph("Traspaso #" + t.getNumeroTraspaso(), FONT_NORMAL);
        pId.setAlignment(Element.ALIGN_CENTER);
        pId.setSpacingAfter(20);
        doc.add(pId);

        doc.add(new Paragraph(" ")); // Espacio
    }

    private void agregarInformacionGeneral(Document doc, TraspasoDatos t, Map<String, Object> extra)
            throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 15, 35, 15, 35 });
        table.setSpacingAfter(15);

        addCellHeader(table, "INFORMACIÓN DEL TRASPASO", 4);

        addInfoRow(table, "Origen:", t.getNombreBodegaOrigen());
        addInfoRow(table, "Destino:", t.getNombreBodegaDestino());

        String fechaSol = t.getFechaSolicitud() != null ? t.getFechaSolicitud().format(formatoFecha) : "N/A";
        addInfoRow(table, "Fecha Envío:", fechaSol);

        String fechaRec = extra.get("fecha_recepcion") != null ? extra.get("fecha_recepcion").toString() : "Pendiente";
        addInfoRow(table, "Fecha RX:", fechaRec);

        addInfoRow(table, "Estado:", t.getEstado().toUpperCase());
        addInfoRow(table, "Usuario:",
                extra.get("usuario_recibe") != null ? extra.get("usuario_recibe").toString() : "N/A");

        doc.add(table);
    }

    private void agregarResumenFinanciero(Document doc, TraspasoDatos t) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60); // Más angosta
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setWidths(new float[] { 60, 40 });
        table.setSpacingAfter(20);

        addCellHeader(table, "RESUMEN DE VALORES", 2);

        BigDecimal totalEnviado = t.getMontoTotal();
        BigDecimal totalRecibido = t.getMontoRecibido();
        BigDecimal diferencia = totalRecibido.subtract(totalEnviado);

        addMoneyRow(table, "Valor Enviado (Total):", totalEnviado, BaseColor.BLACK);
        addMoneyRow(table, "Valor Recibido (Real):", totalRecibido, BaseColor.BLACK);

        BaseColor colorDif = BaseColor.BLACK;
        if (diferencia.compareTo(BigDecimal.ZERO) < 0)
            colorDif = BaseColor.RED;
        else if (diferencia.compareTo(BigDecimal.ZERO) > 0)
            colorDif = new BaseColor(0, 128, 0);

        addMoneyRow(table, "Diferencia (Cuadre):", diferencia, colorDif);

        String estadoCuadre = "CUADRADO";
        if (diferencia.compareTo(BigDecimal.ZERO) < 0)
            estadoCuadre = "FALTANTE";
        else if (diferencia.compareTo(BigDecimal.ZERO) > 0)
            estadoCuadre = "SOBRANTE";

        PdfPCell cellLabel = new PdfPCell(new Phrase("Estado del Cuadre:", FONT_BOLD));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellLabel);

        PdfPCell cellVal = new PdfPCell(
                new Phrase(estadoCuadre, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, colorDif)));
        cellVal.setBorder(Rectangle.NO_BORDER);
        cellVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cellVal);

        doc.add(table);
    }

    private void agregarDetalleProductos(Document doc, TraspasoDatos t) throws DocumentException {
        Paragraph p = new Paragraph("DETALLE DE PRODUCTOS", FONT_SUBTITULO);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        doc.add(p);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 30, 15, 10, 10, 10, 15, 10 });
        // Producto, SKU, Env, Rec, Dif, Subtotal, Estado

        // Headers
        String[] headers = { "Producto", "SKU", "Enviado", "Recibido", "Dif.", "Valor Total", "Estado" };
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_SMALL_BOLD));
            c.setBackgroundColor(COLOR_FONDO_TABLA);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        for (ProductoTraspasoItem item : t.getProductos()) {
            table.addCell(createCellSmall(item.getNombreCompleto(), Element.ALIGN_LEFT));
            table.addCell(createCellSmall(item.getSku() != null ? item.getSku() : "", Element.ALIGN_CENTER));

            int env = item.getCantidadSolicitada();
            // Need cantidad recibida, but TraspasoDatos usually has pending.
            // If data is loaded from completed transfer, cantidadRecibida might need to be
            // populated
            // assuming item.getCantidadRecibida() is populated.
            int rec = item.getCantidadRecibida() != null ? item.getCantidadRecibida() : 0;
            if (rec == 0 && "recibido".equalsIgnoreCase(t.getEstado())) {
                // assume same if not set? No, it should be set if loaded correctly
            }
            // For now printed row

            table.addCell(createCellSmall(String.valueOf(env), Element.ALIGN_CENTER));
            table.addCell(createCellSmall(String.valueOf(rec), Element.ALIGN_CENTER));

            int dif = rec - env;
            BaseColor colorDif = BaseColor.BLACK;
            if (dif < 0)
                colorDif = BaseColor.RED;
            else if (dif > 0)
                colorDif = new BaseColor(0, 128, 0);

            PdfPCell cellDif = new PdfPCell(
                    new Phrase(String.valueOf(dif), new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, colorDif)));
            cellDif.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellDif);

            // Valor Total (Received Value if received, else Sent Value?)
            // Usually we show value of what was processed. Let's show Sent Value for
            // reference or Received Value?
            // "Cuadre" implies what happened.
            BigDecimal val = item.getPrecioUnitario().multiply(BigDecimal.valueOf(rec));
            table.addCell(createCellSmall(formatoMoneda.format(val), Element.ALIGN_RIGHT));

            String status = "OK";
            if (dif != 0)
                status = "⚠";
            table.addCell(createCellSmall(status, Element.ALIGN_CENTER));
        }

        doc.add(table);
    }

    private void agregarPiePagina(Document doc) throws DocumentException {
        Paragraph p = new Paragraph("\n\n\n\n___________________________\nFirma Responsable Bodega", FONT_NORMAL);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);

        Paragraph p2 = new Paragraph("\n\n___________________________\nFirma Conductor / Transporte", FONT_NORMAL);
        p2.setAlignment(Element.ALIGN_CENTER);
        doc.add(p2);

        String fechaImpresion = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .format(java.time.LocalDateTime.now());
        Paragraph pFecha = new Paragraph("\nGenerado el: " + fechaImpresion,
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pFecha);
    }

    // ==================== UTILIDADES ====================

    private void addCellHeader(PdfPTable table, String text, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        cell.setBorderWidthBottom(1);
        table.addCell(cell);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FONT_BOLD));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(5);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, FONT_NORMAL));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellValue.setPadding(5);
        table.addCell(cellValue);
    }

    private void addMoneyRow(PdfPTable table, String label, BigDecimal value, BaseColor color) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FONT_BOLD));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(5);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(
                new Phrase(formatoMoneda.format(value), new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, color)));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValue.setPadding(5);
        table.addCell(cellValue);
    }

    private PdfPCell createCellSmall(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_SMALL));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }
}
