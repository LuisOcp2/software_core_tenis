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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.principal.AppConfig;
import raven.controlador.principal.conexion;

public class reciboPDF {

    private final Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.BOLD);
    private final Font fSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
    private final Font fNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
    private final Font fSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

    public boolean generar(String numeroTraspaso) {
        Document doc = new Document(PageSize.LETTER); // Corregido: usar PageSize.LETTER o PageSize.A4
        doc.setMargins(30, 30, 30, 30); // márgenes: izq, der, arr, abajo
        String nombreArchivo = "recibo_traspaso_" + numeroTraspaso + ".pdf";
        Connection con = null;
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
            doc.open();

            con = conexion.getInstance().createConnection();
            DatosTraspaso datos = obtenerDatosTraspaso(con, numeroTraspaso);
            if (datos == null) {
                throw new SQLException("No se encontró traspaso " + numeroTraspaso);
            }
            List<ItemDetalle> items = obtenerItems(con, datos.idTraspaso);

            agregarEncabezado(doc, datos);
            agregarInfoGeneral(doc, datos);
            agregarMotivo(doc, datos);
            agregarTablaDetalle(doc, items);
            agregarObservaciones(doc, datos);
            agregarPieFirmas(doc, datos);

            doc.close();
            
            // Abrir el PDF automáticamente
            try { 
                Desktop.getDesktop().open(new File(nombreArchivo)); 
            } catch (Exception ignore) {
                System.out.println("No se pudo abrir el PDF automáticamente");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if (doc.isOpen()) doc.close(); } catch (Exception ignore) {}
            return false;
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignore) {}
            }
        }
    }
    
    public boolean generar(String numeroTraspaso, File destino) {
        Document doc = new Document(PageSize.LETTER);
        doc.setMargins(30, 30, 30, 30);
        Connection con = null;
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(destino));
            doc.open();
            
            con = conexion.getInstance().createConnection();
            DatosTraspaso datos = obtenerDatosTraspaso(con, numeroTraspaso);
            if (datos == null) {
                throw new SQLException("No se encontró traspaso " + numeroTraspaso);
            }
            List<ItemDetalle> items = obtenerItems(con, datos.idTraspaso);
            
            agregarEncabezado(doc, datos);
            agregarInfoGeneral(doc, datos);
            agregarMotivo(doc, datos);
            agregarTablaDetalle(doc, items);
            agregarObservaciones(doc, datos);
            agregarPieFirmas(doc, datos);
            
            doc.close();
            try { 
                Desktop.getDesktop().open(destino); 
            } catch (Exception ignore) {
                System.out.println("No se pudo abrir el PDF automáticamente");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if (doc.isOpen()) doc.close(); } catch (Exception ignore) {}
            return false;
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignore) {}
            }
        }
    }

    private void agregarEncabezado(Document doc, DatosTraspaso dt) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{15f, 85f});

        PdfPCell celLogo = new PdfPCell();
        celLogo.setBorder(Rectangle.NO_BORDER);
        celLogo.setPadding(0);
        Image logo = cargarLogo();
        if (logo != null) {
            logo.scaleToFit(48, 48);
            logo.setAlignment(Image.ALIGN_LEFT);
            celLogo.addElement(logo);
        }
        header.addCell(celLogo);

        PdfPCell celTitulo = new PdfPCell();
        celTitulo.setBorder(Rectangle.NO_BORDER);
        celTitulo.setPadding(0);
        Paragraph titulo = new Paragraph("RECIBO DE TRASPASO", fTitulo);
        titulo.setAlignment(Element.ALIGN_LEFT);
        Paragraph num = new Paragraph("No. " + dt.numeroTraspaso, fNormal);
        num.setAlignment(Element.ALIGN_LEFT);
        celTitulo.addElement(titulo);
        celTitulo.addElement(num);
        header.addCell(celTitulo);

        doc.add(header);
        doc.add(new Paragraph(" ", fSmall));
    }

    private void agregarInfoGeneral(Document doc, DatosTraspaso dt) throws DocumentException {
        PdfPTable info = new PdfPTable(4);
        info.setWidthPercentage(100);
        info.setWidths(new float[]{25, 25, 25, 25});

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        addInfo(info, "Bodega Origen:", dt.bodegaOrigen);
        addInfo(info, "Bodega Destino:", dt.bodegaDestino);
        addInfo(info, "Solicitado por:", dt.usuarioSolicita);
        addInfo(info, "Fecha Solicitud:", dt.fechaSolicitud != null ? sdf.format(dt.fechaSolicitud) : "");
        addInfo(info, "Estado:", dt.estado != null ? dt.estado.toUpperCase() : "");
        addInfo(info, "Fecha Recepción:", dt.fechaRecepcion != null ? sdf.format(dt.fechaRecepcion) : "");

        doc.add(info);
        doc.add(new Paragraph(" ", fSmall)); // espacio
    }

    private void agregarMotivo(Document doc, DatosTraspaso dt) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        
        PdfPCell title = new PdfPCell(new Phrase("MOTIVO:", fSub));
        title.setBackgroundColor(new BaseColor(0, 0, 0));
        title.setPadding(4);
        title.setHorizontalAlignment(Element.ALIGN_LEFT);
        Font fBlanco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, BaseColor.WHITE);
        title.setPhrase(new Phrase("MOTIVO:", fBlanco));
        t.addCell(title);
        
        PdfPCell contenido = new PdfPCell(new Phrase(dt.motivo != null ? dt.motivo : "", fNormal));
        contenido.setPadding(6);
        t.addCell(contenido);
        
        doc.add(t);
        doc.add(new Paragraph(" ", fSmall)); // espacio
    }

    private void agregarTablaDetalle(Document doc, List<ItemDetalle> items) throws DocumentException {
        // Título de la sección
        PdfPTable tituloTabla = new PdfPTable(1);
        tituloTabla.setWidthPercentage(100);
        PdfPCell tituloCell = new PdfPCell(new Phrase("DETALLE DE MERCANCÍA", fSub));
        tituloCell.setBackgroundColor(new BaseColor(0, 0, 0));
        tituloCell.setPadding(4);
        Font fBlanco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, BaseColor.WHITE);
        tituloCell.setPhrase(new Phrase("DETALLE DE MERCANCÍA", fBlanco));
        tituloTabla.addCell(tituloCell);
        doc.add(tituloTabla);
        
        // Tabla de productos
        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{5, 12, 40, 8, 12, 10, 13});

        addHeader(tabla, "Item");
        addHeader(tabla, "Código");
        addHeader(tabla, "Descripción");
        addHeader(tabla, "Talla");
        addHeader(tabla, "Color");
        addHeader(tabla, "Cantidad");
        addHeader(tabla, "Referencia");

        int idx = 1; 
        int total = 0;
        for (ItemDetalle it : items) {
            tabla.addCell(cellCentrado(String.valueOf(idx++)));
            tabla.addCell(cellCentrado(it.codigo != null ? it.codigo : ""));
            tabla.addCell(cellIzquierda(it.descripcion != null ? it.descripcion : ""));
            tabla.addCell(cellCentrado(it.talla != null ? it.talla : ""));
            tabla.addCell(cellCentrado(it.color != null ? it.color : ""));
            tabla.addCell(cellCentrado(String.valueOf(it.cantidad)));
            tabla.addCell(cellCentrado(it.referencia != null ? it.referencia : ""));
            total += it.cantidad;
        }

        PdfPCell totalCell = new PdfPCell(new Phrase("TOTAL DE PARES: " + total, fSub));
        totalCell.setColspan(7);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setBackgroundColor(new BaseColor(224, 224, 224));
        totalCell.setPadding(5);
        tabla.addCell(totalCell);

        doc.add(tabla);
        doc.add(new Paragraph(" ", fSmall)); // espacio
    }

    private void agregarObservaciones(Document doc, DatosTraspaso dt) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        
        PdfPCell title = new PdfPCell(new Phrase("OBSERVACIONES:", fSub));
        title.setBackgroundColor(new BaseColor(0, 0, 0));
        title.setPadding(4);
        Font fBlanco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, BaseColor.WHITE);
        title.setPhrase(new Phrase("OBSERVACIONES:", fBlanco));
        t.addCell(title);
        
        PdfPCell contenido = new PdfPCell(new Phrase(dt.observaciones != null ? dt.observaciones : "", fNormal));
        contenido.setPadding(6);
        contenido.setMinimumHeight(40);
        t.addCell(contenido);
        
        doc.add(t);
        doc.add(new Paragraph(" ", fSmall)); // espacio
    }

    private void agregarPieFirmas(Document doc, DatosTraspaso dt) throws DocumentException {
        // Agregar espacio para las firmas
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        
        PdfPTable firmas = new PdfPTable(2);
        firmas.setWidthPercentage(100);
        firmas.setWidths(new float[]{50, 50});

        // Celda ENTREGADO POR
        PdfPCell entregado = new PdfPCell();
        entregado.setBorder(Rectangle.NO_BORDER);
        entregado.setPaddingTop(30);
        
        // Línea de firma
        PdfPTable lineaEntrega = new PdfPTable(1);
        lineaEntrega.setWidthPercentage(80);
        PdfPCell lineaE = new PdfPCell(new Phrase(""));
        lineaE.setBorder(Rectangle.TOP);
        lineaE.setBorderWidthTop(1);
        lineaE.setPaddingTop(0);
        lineaEntrega.addCell(lineaE);
        entregado.addElement(lineaEntrega);
        
        Paragraph p1 = new Paragraph("ENTREGADO POR", fSub);
        p1.setAlignment(Element.ALIGN_CENTER);
        entregado.addElement(p1);
        
        Paragraph p1b = new Paragraph(dt.bodegaOrigen != null ? dt.bodegaOrigen : "", fSmall);
        p1b.setAlignment(Element.ALIGN_CENTER);
        entregado.addElement(p1b);
        
        firmas.addCell(entregado);

        // Celda RECIBIDO POR
        PdfPCell recibido = new PdfPCell();
        recibido.setBorder(Rectangle.NO_BORDER);
        recibido.setPaddingTop(30);
        
        // Línea de firma
        PdfPTable lineaRecibe = new PdfPTable(1);
        lineaRecibe.setWidthPercentage(80);
        PdfPCell lineaR = new PdfPCell(new Phrase(""));
        lineaR.setBorder(Rectangle.TOP);
        lineaR.setBorderWidthTop(1);
        lineaR.setPaddingTop(0);
        lineaRecibe.addCell(lineaR);
        recibido.addElement(lineaRecibe);
        
        Paragraph p2 = new Paragraph("RECIBIDO POR", fSub);
        p2.setAlignment(Element.ALIGN_CENTER);
        recibido.addElement(p2);
        
        Paragraph p2b = new Paragraph(dt.bodegaDestino != null ? dt.bodegaDestino : "", fSmall);
        p2b.setAlignment(Element.ALIGN_CENTER);
        recibido.addElement(p2b);
        
        firmas.addCell(recibido);

        doc.add(firmas);
        
        // Pie de página
        doc.add(new Paragraph(" "));
        Paragraph gen = new Paragraph(
            "Documento generado el " + 
            new SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()) + 
            " | Sistema de Gestión de Inventarios", 
            fSmall
        );
        gen.setAlignment(Element.ALIGN_CENTER);
        doc.add(gen);
    }

    private void addInfo(PdfPTable t, String label, String value) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fSub));
        c1.setBackgroundColor(new BaseColor(245, 245, 245));
        c1.setBorder(Rectangle.BOX);
        c1.setPadding(4);
        t.addCell(c1);
        
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "", fNormal));
        c2.setBorder(Rectangle.BOX);
        c2.setPadding(4);
        t.addCell(c2);
    }

    private void addHeader(PdfPTable t, String text) {
        PdfPCell h = new PdfPCell(new Phrase(text, fSub));
        h.setHorizontalAlignment(Element.ALIGN_CENTER);
        h.setBackgroundColor(new BaseColor(0, 0, 0));
        h.setPadding(5);
        Font fBlanco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, BaseColor.WHITE);
        h.setPhrase(new Phrase(text, fBlanco));
        t.addCell(h);
    }

    private PdfPCell cellCentrado(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, fNormal));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4);
        c.setBorder(Rectangle.BOX);
        return c;
    }
    
    private PdfPCell cellIzquierda(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, fNormal));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4);
        c.setBorder(Rectangle.BOX);
        return c;
    }

    private Image cargarLogo() {
        try {
            InputStream is = reciboPDF.class.getResourceAsStream(AppConfig.logopng);
            if (is == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
            is.close();
            return Image.getInstance(baos.toByteArray());
        } catch (Exception e) {
            System.out.println("No se pudo cargar el logo: " + e.getMessage());
            return null;
        }
    }

    private DatosTraspaso obtenerDatosTraspaso(Connection con, String numero) throws SQLException {
        String sql = "SELECT t.id_traspaso, t.numero_traspaso, bo.nombre AS bodega_origen, bd.nombre AS bodega_destino, " +
                "u.nombre AS usuario_solicita, t.fecha_solicitud, t.fecha_recepcion, t.estado, t.motivo, t.observaciones " +
                "FROM traspasos t " +
                "INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega " +
                "INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega " +
                "INNER JOIN usuarios u ON t.id_usuario_solicita = u.id_usuario " +
                "WHERE t.numero_traspaso = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosTraspaso d = new DatosTraspaso();
                    d.idTraspaso = rs.getInt("id_traspaso");
                    d.numeroTraspaso = rs.getString("numero_traspaso");
                    d.bodegaOrigen = rs.getString("bodega_origen");
                    d.bodegaDestino = rs.getString("bodega_destino");
                    d.usuarioSolicita = rs.getString("usuario_solicita");
                    d.fechaSolicitud = rs.getTimestamp("fecha_solicitud");
                    d.fechaRecepcion = rs.getTimestamp("fecha_recepcion");
                    d.estado = rs.getString("estado");
                    d.motivo = rs.getString("motivo");
                    d.observaciones = rs.getString("observaciones");
                    return d;
                }
            }
        }
        return null;
    }

    private List<ItemDetalle> obtenerItems(Connection con, int idTraspaso) throws SQLException {
        List<ItemDetalle> out = new ArrayList<>();
        String sql = "SELECT td.id_detalle_traspaso, p.codigo_modelo AS codigo, p.nombre AS descripcion, " +
                "t.numero AS talla, c.nombre AS color, " +
                "COALESCE(td.cantidad_recibida, td.cantidad_enviada, td.cantidad_solicitada) AS cantidad, " +
                "pv.sku AS referencia " +
                "FROM traspaso_detalles td " +
                "INNER JOIN productos p ON td.id_producto = p.id_producto " +
                "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "WHERE td.id_traspaso = ? ORDER BY td.id_detalle_traspaso";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemDetalle it = new ItemDetalle();
                    it.codigo = rs.getString("codigo");
                    it.descripcion = rs.getString("descripcion");
                    it.talla = rs.getString("talla");
                    it.color = rs.getString("color");
                    it.cantidad = rs.getInt("cantidad");
                    it.referencia = rs.getString("referencia");
                    out.add(it);
                }
            }
        }
        return out;
    }

    // Clases internas para datos
    private static class DatosTraspaso {
        int idTraspaso;
        String numeroTraspaso;
        String bodegaOrigen;
        String bodegaDestino;
        String usuarioSolicita;
        java.util.Date fechaSolicitud;
        java.util.Date fechaRecepcion;
        String estado;
        String motivo;
        String observaciones;
    }

    private static class ItemDetalle {
        String codigo;
        String descripcion;
        String talla;
        String color;
        int cantidad;
        String referencia;
    }
}
