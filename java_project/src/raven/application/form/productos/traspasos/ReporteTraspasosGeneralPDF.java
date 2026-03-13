package raven.application.form.productos.traspasos;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import raven.controlador.principal.AppConfig;

public class ReporteTraspasosGeneralPDF {

    private final Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.BOLD);
    private final Font fSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
    private final Font fNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
    private final Font fSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);
    private final Font fHeaderTable = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, BaseColor.WHITE);
    private final Font fDetailHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, BaseColor.BLACK);
    private final Font fDetail = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

    public boolean generarReporte(List<TraspasoReporteDTO> listaTraspasos, File destino, String filtrosAplicados) {
        Document doc = new Document(PageSize.LETTER.rotate()); // Horizontal para mas espacio
        doc.setMargins(30, 30, 30, 30);

        try {
            PdfWriter.getInstance(doc, new FileOutputStream(destino));
            doc.open();

            agregarEncabezado(doc, filtrosAplicados);
            agregarTabla(doc, listaTraspasos);
            agregarPiePagina(doc, listaTraspasos.size());

            doc.close();

            // Abrir automáticamente
            try {
                Desktop.getDesktop().open(destino);
            } catch (Exception ignore) {
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void agregarEncabezado(Document doc, String filtros) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 10f, 90f });

        PdfPCell celLogo = new PdfPCell();
        celLogo.setBorder(Rectangle.NO_BORDER);
        Image logo = cargarLogo();
        if (logo != null) {
            logo.scaleToFit(40, 40);
            celLogo.addElement(logo);
        }
        header.addCell(celLogo);

        PdfPCell celTitulo = new PdfPCell();
        celTitulo.setBorder(Rectangle.NO_BORDER);
        Paragraph titulo = new Paragraph("REPORTE GENERAL DE TRASPASOS", fTitulo);
        titulo.setAlignment(Element.ALIGN_LEFT);
        celTitulo.addElement(titulo);

        Paragraph sub = new Paragraph("Filtros: " + (filtros.isEmpty() ? "Ninguno (Todos)" : filtros), fSmall);
        sub.setAlignment(Element.ALIGN_LEFT);
        celTitulo.addElement(sub);

        Paragraph fecha = new Paragraph("Generado el: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()),
                fSmall);
        fecha.setAlignment(Element.ALIGN_LEFT);
        celTitulo.addElement(fecha);

        header.addCell(celTitulo);

        doc.add(header);
        doc.add(new Paragraph(" "));
    }

    private void agregarTabla(Document doc, List<TraspasoReporteDTO> lista) throws DocumentException {
        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        // Columnas: N. Traspaso, Origen, Destino, Fecha, Estado, Productos, Total Env,
        // Total Rec
        tabla.setWidths(new float[] { 10, 16, 16, 14, 12, 10, 11, 11 });

        String[] headers = { "No. Traspaso", "Bodega Origen", "Bodega Destino", "Fecha", "Estado",
                "Items", "Total Env.", "Total Rec." };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeaderTable));
            cell.setBackgroundColor(BaseColor.BLACK);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            tabla.addCell(cell);
        }

        int totalProductosGeneral = 0;
        java.math.BigDecimal totalMontoEnvGeneral = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalMontoRecGeneral = java.math.BigDecimal.ZERO;

        // Agrupar por Bodega Origen
        java.util.Map<String, List<TraspasoReporteDTO>> agrupado = new java.util.LinkedHashMap<>();
        for (TraspasoReporteDTO dto : lista) {
            String origen = (dto.getOrigen() != null) ? dto.getOrigen() : "Desconocido";
            agrupado.computeIfAbsent(origen, k -> new java.util.ArrayList<>()).add(dto);
        }

        for (java.util.Map.Entry<String, List<TraspasoReporteDTO>> entry : agrupado.entrySet()) {
            String bodegaOrigen = entry.getKey();
            List<TraspasoReporteDTO> grupo = entry.getValue();

            // Cabecera de grupo
            PdfPCell cellGrupo = new PdfPCell(new Phrase("ORIGEN: " + bodegaOrigen, fSub));
            cellGrupo.setColspan(8); // Updated colspan
            cellGrupo.setBackgroundColor(new BaseColor(240, 240, 240)); // Gris muy claro
            cellGrupo.setPadding(6);
            tabla.addCell(cellGrupo);

            int totalProductosGrupo = 0;
            java.math.BigDecimal totalMontoEnvGrupo = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalMontoRecGrupo = java.math.BigDecimal.ZERO;

            for (TraspasoReporteDTO dto : grupo) {
                // Calcular totales desde los detalles para asegurar consistencia
                java.math.BigDecimal calcTotalEnv = java.math.BigDecimal.ZERO;
                java.math.BigDecimal calcTotalRec = java.math.BigDecimal.ZERO;

                if (dto.getDetalles() != null) {
                    for (Map<String, Object> det : dto.getDetalles()) {
                        java.math.BigDecimal sub = (java.math.BigDecimal) det.get("subtotal");
                        java.math.BigDecimal precio = (java.math.BigDecimal) det.get("precio_unitario");
                        Integer rec = (Integer) det.get("cantidad_recibida");

                        if (sub != null) {
                            calcTotalEnv = calcTotalEnv.add(sub);
                        }
                        if (precio != null && rec != null) {
                            calcTotalRec = calcTotalRec.add(precio.multiply(java.math.BigDecimal.valueOf(rec)));
                        }
                    }
                }

                // Usar valores calculados si el DTO trae 0 (o siempre, para consistencia)
                java.math.BigDecimal mTotalVal = dto.getMontoTotal();
                if (mTotalVal == null || (mTotalVal.compareTo(java.math.BigDecimal.ZERO) == 0
                        && calcTotalEnv.compareTo(java.math.BigDecimal.ZERO) > 0)) {
                    mTotalVal = calcTotalEnv;
                }

                java.math.BigDecimal mRecVal = dto.getMontoRecibido();
                if (mRecVal == null || (mRecVal.compareTo(java.math.BigDecimal.ZERO) == 0
                        && calcTotalRec.compareTo(java.math.BigDecimal.ZERO) > 0)) {
                    mRecVal = calcTotalRec;
                }

                // ROW INFO
                tabla.addCell(celdaData(dto.getNumero(), Element.ALIGN_CENTER));
                tabla.addCell(celdaData(dto.getOrigen(), Element.ALIGN_LEFT));
                tabla.addCell(celdaData(dto.getDestino(), Element.ALIGN_LEFT));
                tabla.addCell(celdaData(dto.getFecha(), Element.ALIGN_CENTER));
                tabla.addCell(celdaData(dto.getEstado(), Element.ALIGN_CENTER));
                tabla.addCell(celdaData(dto.getTotalProductos(), Element.ALIGN_CENTER));

                // Financials
                String mTotal = String.format("$ %,.0f", mTotalVal);
                String mRec = String.format("$ %,.0f", mRecVal);

                tabla.addCell(celdaData(mTotal, Element.ALIGN_RIGHT));
                tabla.addCell(celdaData(mRec, Element.ALIGN_RIGHT));

                // SUMAR TOTALES
                try {
                    int p = Integer.parseInt(dto.getTotalProductos());
                    totalProductosGrupo += p;
                    totalProductosGeneral += p;
                } catch (Exception ignore) {
                }

                // Sumar financieros
                totalMontoEnvGrupo = totalMontoEnvGrupo.add(mTotalVal);
                totalMontoRecGrupo = totalMontoRecGrupo.add(mRecVal);
                totalMontoEnvGeneral = totalMontoEnvGeneral.add(mTotalVal);
                totalMontoRecGeneral = totalMontoRecGeneral.add(mRecVal);

                // AGREGAR SUBTABLA DE DETALLES IF EXISTS
                if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
                    PdfPCell celdaDetalles = new PdfPCell();
                    celdaDetalles.setColspan(8); // Updated colspan
                    celdaDetalles.setPadding(10);
                    celdaDetalles.setBorder(Rectangle.NO_BORDER);

                    PdfPTable tDet = new PdfPTable(7); // Increased columns
                    tDet.setWidthPercentage(95);
                    // Name, Color/Talla, Solic, Env, Rec, Precio, Subtotal
                    tDet.setWidths(new float[] { 30, 20, 8, 8, 8, 13, 13 });

                    // Header Detalles
                    agregarCeldaDetalleHeader(tDet, "Producto");
                    agregarCeldaDetalleHeader(tDet, "Variante");
                    agregarCeldaDetalleHeader(tDet, "Solic.");
                    agregarCeldaDetalleHeader(tDet, "Env.");
                    agregarCeldaDetalleHeader(tDet, "Rec.");
                    agregarCeldaDetalleHeader(tDet, "Val. U.");
                    agregarCeldaDetalleHeader(tDet, "Subtotal");

                    for (Map<String, Object> det : dto.getDetalles()) {
                        String prodName = (String) det.get("producto_nombre");
                        String color = (String) det.get("color_nombre");
                        String talla = (String) det.get("talla_numero");
                        String variantInfo = (color != null ? color : "") + (talla != null ? " - " + talla : "");
                        Integer sol = (Integer) det.get("cantidad_solicitada");
                        Integer env = (Integer) det.get("cantidad_enviada");
                        Integer rec = (Integer) det.get("cantidad_recibida");

                        java.math.BigDecimal precio = (java.math.BigDecimal) det.get("precio_unitario");
                        java.math.BigDecimal sub = (java.math.BigDecimal) det.get("subtotal");
                        String sPrecio = precio != null ? String.format("$ %,.0f", precio) : "$ 0";
                        String sSub = sub != null ? String.format("$ %,.0f", sub) : "$ 0";

                        tDet.addCell(celdaDetalle(prodName != null ? prodName : "", Element.ALIGN_LEFT));
                        tDet.addCell(celdaDetalle(variantInfo, Element.ALIGN_LEFT));
                        tDet.addCell(celdaDetalle(sol != null ? sol.toString() : "0", Element.ALIGN_CENTER));
                        tDet.addCell(celdaDetalle(env != null ? env.toString() : "0", Element.ALIGN_CENTER));
                        tDet.addCell(celdaDetalle(rec != null ? rec.toString() : "0", Element.ALIGN_CENTER));
                        tDet.addCell(celdaDetalle(sPrecio, Element.ALIGN_RIGHT));
                        tDet.addCell(celdaDetalle(sSub, Element.ALIGN_RIGHT));
                    }

                    celdaDetalles.addElement(tDet);
                    tabla.addCell(celdaDetalles);
                }
            }

            // Subtotal del grupo
            PdfPCell cellTotalGrupoLabel = new PdfPCell(new Phrase("Total " + bodegaOrigen + ":", fSmall));
            cellTotalGrupoLabel.setColspan(5);
            cellTotalGrupoLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTotalGrupoLabel.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(cellTotalGrupoLabel);

            PdfPCell cellTotalGrupoValue = new PdfPCell(new Phrase(String.valueOf(totalProductosGrupo), fSmall));
            cellTotalGrupoValue.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellTotalGrupoValue.setBorder(Rectangle.BOTTOM); // Solo linea abajo
            tabla.addCell(cellTotalGrupoValue);

            // Totales financieros del grupo
            PdfPCell cellTotalGrupoPresupuesto = new PdfPCell(
                    new Phrase(String.format("$ %,.0f", totalMontoEnvGrupo), fSmall));
            cellTotalGrupoPresupuesto.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTotalGrupoPresupuesto.setBorder(Rectangle.BOTTOM);
            tabla.addCell(cellTotalGrupoPresupuesto);

            PdfPCell cellTotalGrupoRecibido = new PdfPCell(
                    new Phrase(String.format("$ %,.0f", totalMontoRecGrupo), fSmall));
            cellTotalGrupoRecibido.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTotalGrupoRecibido.setBorder(Rectangle.BOTTOM);
            tabla.addCell(cellTotalGrupoRecibido);
        }

        // Fila de total GENERAL
        PdfPCell cellTotalTitulo = new PdfPCell(new Phrase("TOTAL GENERAL", fSub));
        cellTotalTitulo.setColspan(5);
        cellTotalTitulo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotalTitulo.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cellTotalTitulo.setPadding(5);
        tabla.addCell(cellTotalTitulo);

        PdfPCell cellTotalValor = new PdfPCell(new Phrase(String.valueOf(totalProductosGeneral), fSub));
        cellTotalValor.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalValor.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cellTotalValor.setPadding(5);
        tabla.addCell(cellTotalValor);

        // Totales financieros generales
        PdfPCell cellTotalEnvGeneral = new PdfPCell(new Phrase(String.format("$ %,.0f", totalMontoEnvGeneral), fSub));
        cellTotalEnvGeneral.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotalEnvGeneral.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cellTotalEnvGeneral.setPadding(5);
        tabla.addCell(cellTotalEnvGeneral);

        PdfPCell cellTotalRecGeneral = new PdfPCell(new Phrase(String.format("$ %,.0f", totalMontoRecGeneral), fSub));
        cellTotalRecGeneral.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotalRecGeneral.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cellTotalRecGeneral.setPadding(5);
        tabla.addCell(cellTotalRecGeneral);

        doc.add(tabla);
    }

    private PdfPCell celdaData(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fNormal));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private void agregarCeldaDetalleHeader(PdfPTable t, String txt) {
        PdfPCell c = new PdfPCell(new Phrase(txt, fDetailHeader));
        c.setBackgroundColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(2);
        t.addCell(c);
    }

    private PdfPCell celdaDetalle(String txt, int align) {
        PdfPCell c = new PdfPCell(new Phrase(txt, fDetail));
        c.setHorizontalAlignment(align);
        c.setPadding(2);
        return c;
    }

    private void agregarPiePagina(Document doc, int totalRegistros) throws DocumentException {
        doc.add(new Paragraph(" "));
        Paragraph p = new Paragraph("Total de registros en este reporte: " + totalRegistros, fSub);
        p.setAlignment(Element.ALIGN_RIGHT);
        doc.add(p);
    }

    private Image cargarLogo() {
        try {
            InputStream is = ReporteTraspasosGeneralPDF.class.getResourceAsStream(AppConfig.logopng);
            if (is == null)
                return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            is.close();
            return Image.getInstance(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}
