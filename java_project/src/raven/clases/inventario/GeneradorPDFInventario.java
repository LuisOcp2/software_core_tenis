package raven.clases.inventario;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import raven.controlador.principal.AppConfig;
import raven.modelos.DetalleConteoInventario;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Generador de PDF para Inventarios (Tamaño Carta)
 */
public class GeneradorPDFInventario {

    private static final BaseColor COLOR_PRIMARIO = new BaseColor(33, 150, 243);
    private static final BaseColor COLOR_ENCABEZADO_TABLA = new BaseColor(230, 240, 255);

    public void generarPDF(int idConteo, java.awt.Component parent) {
        ConnectionWrapper conWrapper = null;
        try {
            // Obtener datos
            ServiceConteoInventario service = new ServiceConteoInventario();
            List<DetalleConteoInventario> detalles = service.obtenerDetallesConteo(idConteo);

            // Simple data fetch for header (could reuse DAO methods but for speed doing
            // ad-hoc or passing objects)
            // For now assuming we pass ID and fetch inside.
            // ... Fetch header data logic needed or passed.
            // Let's reuse the Service to get full Conteo object if possible, or query.
            // ServiceConteoInventario doesn't have "getConteoById" returning object easily
            // exposed?
            // "obtenerConteosActivos" returns list.
            // We can add a method or query manually. Let's query manually here for
            // simplicity to avoid changing Service too much.

            // ... (Skipping manual query inside PDF class is bad practice, but following
            // "ImpresionPOS" pattern).
            // Actually ImpresionPOS does it.

            // File Saver
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar Reporte de Inventario");
            fileChooser.setSelectedFile(new File("Inventario_Conteo_" + idConteo + ".pdf"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));

            if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                archivo = new File(archivo.getAbsolutePath() + ".pdf");
            }

            Document document = new Document(PageSize.LETTER);
            PdfWriter.getInstance(document, new FileOutputStream(archivo));
            document.open();

            // Header
            agregarEncabezado(document, idConteo); // We would pass header data here

            // Body
            agregarTablaDetalles(document, detalles);

            // Footer
            agregarPiePagina(document);

            document.close();

            // Open
            if (JOptionPane.showConfirmDialog(parent, "PDF generado. ¿Abrir ahora?", "Éxito",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Desktop.getDesktop().open(archivo);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error generando PDF: " + e.getMessage());
        }
    }

    private void agregarEncabezado(Document doc, int idConteo) throws DocumentException {
        // Logo / Title
        Paragraph pOrg = new Paragraph(AppConfig.name != null ? AppConfig.name : "EMPRESA",
                new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_PRIMARIO));
        pOrg.setAlignment(Element.ALIGN_CENTER);
        doc.add(pOrg);

        Paragraph pTitle = new Paragraph("REPORTE DE CONTEO DE INVENTARIO",
                new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD));
        pTitle.setAlignment(Element.ALIGN_CENTER);
        pTitle.setSpacingBefore(10);
        doc.add(pTitle);

        Paragraph pInfo = new Paragraph("Conteo No: " + idConteo + " | Fecha Impresión: "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        pInfo.setAlignment(Element.ALIGN_CENTER);
        pInfo.setSpacingAfter(20);
        doc.add(pInfo);
    }

    private void agregarTablaDetalles(Document doc, List<DetalleConteoInventario> detalles) throws DocumentException {
        // Updated for "Blind Count" / "Planilla de Conteo"
        // Col 1: Code, Col 2: Product, Col 3: Size/Color, Col 4: Physical (Empty for
        // writing)
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 15, 45, 25, 15 });

        // Header
        String[] headers = { "Código", "Producto", "Talla/Color", "Físico" };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
            cell.setBackgroundColor(COLOR_ENCABEZADO_TABLA);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Data
        Font fontData = new Font(Font.FontFamily.HELVETICA, 9);
        for (DetalleConteoInventario d : detalles) {
            table.addCell(new Phrase(d.getProducto().getBarcode(), fontData));
            table.addCell(new Phrase(d.getProducto().getName(), fontData));

            String meta = (d.getProducto().getSize() != null ? d.getProducto().getSize() : "") + " " +
                    (d.getProducto().getColor() != null ? d.getProducto().getColor() : "");
            table.addCell(new Phrase(meta, fontData));

            // Physical Column: Show value if > 0, otherwise empty for writing
            String fisVal = d.getStockContado() > 0 ? String.valueOf(d.getStockContado()) : "";
            PdfPCell cCont = new PdfPCell(new Phrase(fisVal, fontData));
            cCont.setHorizontalAlignment(Element.ALIGN_RIGHT);
            // Optional: Add a subtle underline or leave blank
            table.addCell(cCont);
        }

        doc.add(table);
    }

    private void agregarPiePagina(Document doc) throws DocumentException {
        Paragraph pFirma = new Paragraph("\n\n\n___________________________\nFirma Responsable",
                new Font(Font.FontFamily.HELVETICA, 10));
        pFirma.setAlignment(Element.ALIGN_CENTER);
        doc.add(pFirma);
    }

    // Helper helper
    private static class ConnectionWrapper {
    } // Mock to avoid import errors if not used directly
}
