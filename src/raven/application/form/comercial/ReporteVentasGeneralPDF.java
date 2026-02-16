package raven.application.form.comercial;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReporteVentasGeneralPDF {

    private final DecimalFormat formatoMoneda = new DecimalFormat("$#,##0");

    public boolean generarReporte(List<VentaReporteDTO> ventas, File archivo, String filtros) {
        Document doc = new Document(PageSize.LETTER.rotate()); // Landscape para mejor visualización
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(archivo));
            doc.open();

            // Fuentes
            com.itextpdf.text.Font fTitulo = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16,
                    com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fSubTitulo = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                    12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fTexto = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                    com.itextpdf.text.Font.NORMAL);
            com.itextpdf.text.Font fCabecera = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                    10, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fDetalleCabecera = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
            com.itextpdf.text.Font fDetalleTexto = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL, BaseColor.DARK_GRAY);

            // Título
            Paragraph p = new Paragraph("REPORTE DETALLADO DE VENTAS", fTitulo);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingAfter(10);
            doc.add(p);

            // Info General
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(
                    createCell("Fecha de generación: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()),
                            fTexto, false));
            info.addCell(createCell("Filtros: " + filtros, fTexto, false));
            doc.add(info);
            doc.add(new Paragraph(" "));

            double totalGeneral = 0;

            for (VentaReporteDTO venta : ventas) {
                totalGeneral += venta.getTotal();

                // Cabecera de Venta
                PdfPTable tableVenta = new PdfPTable(7);
                tableVenta.setWidthPercentage(100);
                tableVenta.setWidths(new int[] { 10, 15, 20, 15, 10, 15, 15 });
                tableVenta.setSpacingBefore(5);

                // Encabezados Venta
                addCellHeader(tableVenta, "ID", fCabecera, BaseColor.LIGHT_GRAY);
                addCellHeader(tableVenta, "Fecha", fCabecera, BaseColor.LIGHT_GRAY);
                addCellHeader(tableVenta, "Cliente", fCabecera, BaseColor.LIGHT_GRAY);
                addCellHeader(tableVenta, "Vendedor", fCabecera, BaseColor.LIGHT_GRAY);
                addCellHeader(tableVenta, "Estado", fCabecera, BaseColor.LIGHT_GRAY);
                addCellHeader(tableVenta, "Pago", fCabecera, BaseColor.LIGHT_GRAY);
                addCellHeader(tableVenta, "Total", fCabecera, BaseColor.LIGHT_GRAY);

                // Datos Venta
                tableVenta.addCell(new Phrase(String.valueOf(venta.getIdVenta()), fTexto));
                tableVenta.addCell(new Phrase(venta.getFecha(), fTexto));
                tableVenta.addCell(new Phrase(venta.getCliente(), fTexto));
                tableVenta.addCell(new Phrase(venta.getVendedor(), fTexto));
                tableVenta.addCell(new Phrase(venta.getEstado(), fTexto));
                tableVenta.addCell(new Phrase(venta.getTipoPago(), fTexto));
                tableVenta.addCell(new Phrase(formatoMoneda.format(venta.getTotal()), fTexto));

                doc.add(tableVenta);

                // Detalles de productos (Subtabla)
                if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
                    PdfPTable details = new PdfPTable(6);
                    details.setWidthPercentage(95); // Un poco más angosta para efecto visual de anidación
                    details.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    details.setWidths(new int[] { 30, 15, 15, 10, 15, 15 });
                    details.setSpacingAfter(10);

                    // Encabezados Detalle
                    addCellHeader(details, "Producto", fDetalleCabecera, new BaseColor(230, 240, 250));
                    addCellHeader(details, "Color", fDetalleCabecera, new BaseColor(230, 240, 250));
                    addCellHeader(details, "Talla", fDetalleCabecera, new BaseColor(230, 240, 250));
                    addCellHeader(details, "Cant.", fDetalleCabecera, new BaseColor(230, 240, 250));
                    addCellHeader(details, "Precio", fDetalleCabecera, new BaseColor(230, 240, 250));
                    addCellHeader(details, "Subtotal", fDetalleCabecera, new BaseColor(230, 240, 250));

                    for (Map<String, Object> det : venta.getDetalles()) {
                        String producto = (String) det.get("producto");
                        String color = (String) det.get("color");
                        String talla = (String) det.get("talla");
                        int cantidad = (int) det.get("cantidad");
                        double precio = ((Number) det.get("precio_unitario")).doubleValue();
                        double subtotal = ((Number) det.get("subtotal")).doubleValue();

                        details.addCell(new Phrase(producto, fDetalleTexto));
                        details.addCell(new Phrase(color != null ? color : "-", fDetalleTexto));
                        details.addCell(new Phrase(talla != null ? talla : "-", fDetalleTexto));
                        details.addCell(new Phrase(String.valueOf(cantidad), fDetalleTexto));
                        details.addCell(new Phrase(formatoMoneda.format(precio), fDetalleTexto));
                        details.addCell(new Phrase(formatoMoneda.format(subtotal), fDetalleTexto));
                    }
                    doc.add(details);
                } else {
                    doc.add(new Paragraph(" ")); // Espacio si no hay detalles
                }
            }

            // Total General Final
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(createCell("TOTAL GENERAL:", fSubTitulo, true));
            totalTable.addCell(createCell(formatoMoneda.format(totalGeneral), fSubTitulo, true));
            doc.add(totalTable);

            doc.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private PdfPCell createCell(String text, com.itextpdf.text.Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (!border) {
            cell.setBorder(PdfPCell.NO_BORDER);
        }
        return cell;
    }

    private void addCellHeader(PdfPTable table, String text, com.itextpdf.text.Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
