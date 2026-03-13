package raven.clases.productos;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JTable;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

/**
 * Generador de etiquetas en PDF - SINCRONIZADO CON ImpresorTermicaPOSDIG2406T_2
 *
 * IMPORTANTE: Las medidas y parámetros del código de barras CODE 128 deben coincidir
 * EXACTAMENTE con la clase ImpresorTermicaPOSDIG2406T_2 para garantizar que las
 * etiquetas se impriman correctamente sin cortes ni errores.
 *
 * Medidas estándar (según ImpresorTermicaPOSDIG2406T_2):
 * - Etiqueta normal (par): 90mm x 30mm (~255 x 85 puntos a 72 DPI)
 * - Etiqueta de caja: 100mm x 100mm (~283 x 283 puntos a 72 DPI) [PENDIENTE CONFIRMAR]
 *
 * @author Raven
 */
public class GeneradorEtiquetas {

    public enum ModoEtiqueta {
        ETIQUETA,  // 90mm x 30mm - Para etiquetas de par
        CAJA       // 100mm x 100mm - Para etiquetas de caja (pendiente confirmar)
    }

    // Conversión estándar mm a puntos (72 DPI)
    private static final double MM_TO_POINTS = 2.83465;

    // Dimensiones EXACTAS según ImpresorTermicaPOSDIG2406T_2
    // IMPORTANTE: Etiquetas de PAR son 90mm x 30mm
    private static final float ANCHO_ETIQUETA = (float)(90 * MM_TO_POINTS);  // ~255 puntos
    private static final float ALTO_ETIQUETA = (float)(30 * MM_TO_POINTS);   // ~85 puntos
    private static final float ANCHO_CAJA = (float)(100 * MM_TO_POINTS);     // ~283 puntos (pendiente confirmar)
    private static final float ALTO_CAJA = (float)(100 * MM_TO_POINTS);      // ~283 puntos (pendiente confirmar)

    /**
     * Genera PDF de etiquetas normales (90mm x 30mm)
     * @deprecated Use generarPDF con modo específico
     */
    @Deprecated
    public static void generarPDFDesdeJTable(JTable tabla, String logoPath) {
        generarPDF(tabla, logoPath, ModoEtiqueta.ETIQUETA, "etiquetas.pdf");
    }

    /**
     * Genera PDF con el modo especificado (ETIQUETA o CAJA)
     */
    public static void generarPDF(JTable tabla, String logoPath, ModoEtiqueta modo, String rutaSalida) {
        try {
            // Configurar tamaño del documento según el modo
            float ancho = (modo == ModoEtiqueta.CAJA) ? ANCHO_CAJA : ANCHO_ETIQUETA;
            float alto = (modo == ModoEtiqueta.CAJA) ? ALTO_CAJA : ALTO_ETIQUETA;

            Document document = new Document(new Rectangle(ancho, alto));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(rutaSalida));
            document.open();

            // Cargar logo si existe
            Image logo = null;
            try {
                if (logoPath != null && new File(logoPath).exists()) {
                    logo = Image.getInstance(logoPath);
                }
            } catch (Exception e) {
                System.err.println("Error cargando logo: " + e.getMessage());
            }

            // Procesar tabla
            for (int i = 0; i < tabla.getRowCount(); i++) {
                Boolean seleccionado = (Boolean) tabla.getValueAt(i, 0);
                if (seleccionado != null && seleccionado) {
                    int cantidad = Integer.parseInt(tabla.getValueAt(i, 5).toString()); // Columna 5: Cantidad

                    for (int j = 0; j < cantidad; j++) {
                        if (modo == ModoEtiqueta.CAJA) {
                            crearEtiquetaCaja(document, tabla, i, logo);
                        } else {
                            crearEtiquetaNormal(document, tabla, i, logo);
                        }
                        document.newPage();
                    }
                }
            }

            document.close();
            writer.close();

            System.out.println("PDF generado exitosamente: " + rutaSalida);

        } catch (Exception e) {
            System.err.println("Error generando PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crea etiqueta normal (90mm x 30mm) - Sincronizada con ImpresorTermicaPOSDIG2406T_2.imprimirEtiquetaNormal
     */
    private static void crearEtiquetaNormal(Document document, JTable tabla, int fila, Image logo) throws Exception {
        // Márgenes mínimos (5 puntos = ~1.76mm)
        float margen = 5;
        float anchoUtil = ANCHO_ETIQUETA - (margen * 2);
        float altoUtil = ALTO_ETIQUETA - (margen * 2);

        // Obtener datos
        String ean = tabla.getValueAt(fila, 1).toString();
        String nombre = tabla.getValueAt(fila, 2).toString();
        String color = tabla.getValueAt(fila, 3).toString();
        String talla = tabla.getValueAt(fila, 4).toString();
        String id = tabla.getValueAt(fila, 6).toString();

        // Crear tabla principal con diseño personalizado
        PdfPTable tablaPrincipal = new PdfPTable(1);
        tablaPrincipal.setWidthPercentage(100);
        tablaPrincipal.getDefaultCell().setBorder(Rectangle.BOX);
        tablaPrincipal.getDefaultCell().setPadding(margen);
        tablaPrincipal.getDefaultCell().setBorderWidth(1.5f);
        tablaPrincipal.getDefaultCell().setBorderColor(BaseColor.BLACK);

        // Contenedor interno
        PdfPTable contenido = new PdfPTable(2);
        contenido.setWidthPercentage(100);
        contenido.setWidths(new float[]{70, 30}); // 70% contenido, 30% logo

        // Columna izquierda: Información del producto
        PdfPTable infoProducto = new PdfPTable(1);
        infoProducto.setWidthPercentage(100);

        // Nombre del producto
        Font fontNombre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        String nombreCorto = nombre.length() > 30 ? nombre.substring(0, 30) + "..." : nombre;
        PdfPCell celdaNombre = new PdfPCell(new Phrase(nombreCorto.toUpperCase(), fontNombre));
        celdaNombre.setBorder(Rectangle.NO_BORDER);
        celdaNombre.setPaddingBottom(2);
        infoProducto.addCell(celdaNombre);

        // Color
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8);
        PdfPCell celdaColor = new PdfPCell(new Phrase("Color: " + color.toUpperCase(), fontNormal));
        celdaColor.setBorder(Rectangle.NO_BORDER);
        celdaColor.setPaddingBottom(2);
        infoProducto.addCell(celdaColor);

        // ID
        Font fontId = FontFactory.getFont(FontFactory.HELVETICA, 7);
        PdfPCell celdaId = new PdfPCell(new Phrase("ID:" + id, fontId));
        celdaId.setBorder(Rectangle.NO_BORDER);
        infoProducto.addCell(celdaId);

        PdfPCell celdaInfo = new PdfPCell(infoProducto);
        celdaInfo.setBorder(Rectangle.NO_BORDER);
        contenido.addCell(celdaInfo);

        // Columna derecha: Logo
        if (logo != null) {
            logo.scaleToFit(30, 30);
            PdfPCell celdaLogo = new PdfPCell(logo, true);
            celdaLogo.setBorder(Rectangle.NO_BORDER);
            celdaLogo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaLogo.setVerticalAlignment(Element.ALIGN_TOP);
            contenido.addCell(celdaLogo);
        } else {
            contenido.addCell(crearCeldaVacia());
        }

        // Código de barras centrado - PARÁMETROS SINCRONIZADOS
        Image codigoBarras = generarCodigoBarras(ean, false); // false = etiqueta normal
        codigoBarras.scaleToFit(120, 35); // Tamaño según ImpresorTermicaPOSDIG2406T_2
        codigoBarras.setAlignment(Element.ALIGN_CENTER);

        PdfPCell celdaBarcode = new PdfPCell(codigoBarras, true);
        celdaBarcode.setBorder(Rectangle.NO_BORDER);
        celdaBarcode.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaBarcode.setColspan(2);
        celdaBarcode.setPaddingTop(5);
        contenido.addCell(celdaBarcode);

        // EAN debajo del código de barras
        Font fontEan = FontFactory.getFont(FontFactory.HELVETICA, 8);
        PdfPCell celdaEan = new PdfPCell(new Phrase(ean, fontEan));
        celdaEan.setBorder(Rectangle.NO_BORDER);
        celdaEan.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaEan.setColspan(2);
        contenido.addCell(celdaEan);

        // Talla grande en esquina inferior derecha
        Font fontTalla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        PdfPCell celdaTalla = new PdfPCell(new Phrase(talla, fontTalla));
        celdaTalla.setBorder(Rectangle.NO_BORDER);
        celdaTalla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaTalla.setVerticalAlignment(Element.ALIGN_BOTTOM);
        celdaTalla.setColspan(2);
        contenido.addCell(celdaTalla);

        PdfPCell celdaContenido = new PdfPCell(contenido);
        celdaContenido.setBorder(Rectangle.NO_BORDER);
        tablaPrincipal.addCell(celdaContenido);

        document.add(tablaPrincipal);
    }

    /**
     * Crea etiqueta de caja (100mm x 100mm) - Sincronizada con ImpresorTermicaPOSDIG2406T_2.imprimirEtiquetaCaja
     */
    private static void crearEtiquetaCaja(Document document, JTable tabla, int fila, Image logo) throws Exception {
        // Márgenes mínimos
        float margen = 5;
        float anchoUtil = ANCHO_CAJA - (margen * 2);
        float altoUtil = ALTO_CAJA - (margen * 2);

        // Obtener datos
        String ean = tabla.getValueAt(fila, 1).toString();
        String nombre = tabla.getValueAt(fila, 2).toString();
        String color = tabla.getValueAt(fila, 3).toString();
        String talla = tabla.getValueAt(fila, 4).toString();
        String id = tabla.getValueAt(fila, 6).toString();

        // Tabla principal con borde
        PdfPTable tablaPrincipal = new PdfPTable(1);
        tablaPrincipal.setWidthPercentage(100);
        tablaPrincipal.getDefaultCell().setBorder(Rectangle.BOX);
        tablaPrincipal.getDefaultCell().setPadding(margen);
        tablaPrincipal.getDefaultCell().setBorderWidth(2);
        tablaPrincipal.getDefaultCell().setBorderColor(BaseColor.BLACK);

        PdfPTable contenido = new PdfPTable(2);
        contenido.setWidthPercentage(100);
        contenido.setWidths(new float[]{70, 30});

        // Información del producto
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        String nombreCorto = nombre.length() > 22 ? nombre.substring(0, 22) + "..." : nombre;
        PdfPCell celdaNombre = new PdfPCell(new Phrase(nombreCorto.toUpperCase(), fontTitulo));
        celdaNombre.setBorder(Rectangle.NO_BORDER);
        celdaNombre.setPaddingBottom(5);
        contenido.addCell(celdaNombre);

        // Logo en esquina superior derecha
        if (logo != null) {
            logo.scaleToFit(60, 60);
            PdfPCell celdaLogo = new PdfPCell(logo, true);
            celdaLogo.setBorder(Rectangle.NO_BORDER);
            celdaLogo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaLogo.setVerticalAlignment(Element.ALIGN_TOP);
            contenido.addCell(celdaLogo);
        } else {
            contenido.addCell(crearCeldaVacia());
        }

        // Color
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 16);
        PdfPCell celdaColor = new PdfPCell(new Phrase("Color: " + color.toUpperCase(), fontNormal));
        celdaColor.setBorder(Rectangle.NO_BORDER);
        celdaColor.setColspan(2);
        celdaColor.setPaddingBottom(10);
        contenido.addCell(celdaColor);

        // Talla grande y centrada
        Font fontTalla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36);
        PdfPCell celdaTalla = new PdfPCell(new Phrase("TALLA: " + talla, fontTalla));
        celdaTalla.setBorder(Rectangle.NO_BORDER);
        celdaTalla.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaTalla.setColspan(2);
        celdaTalla.setPaddingTop(10);
        celdaTalla.setPaddingBottom(10);
        contenido.addCell(celdaTalla);

        // Código de barras centrado - PARÁMETROS SINCRONIZADOS
        Image codigoBarras = generarCodigoBarras(ean, true); // true = etiqueta de caja
        codigoBarras.scaleToFit(200, 60); // Tamaño según ImpresorTermicaPOSDIG2406T_2
        codigoBarras.setAlignment(Element.ALIGN_CENTER);

        PdfPCell celdaBarcode = new PdfPCell(codigoBarras, true);
        celdaBarcode.setBorder(Rectangle.NO_BORDER);
        celdaBarcode.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaBarcode.setColspan(2);
        celdaBarcode.setPaddingTop(10);
        contenido.addCell(celdaBarcode);

        // EAN debajo del código
        Font fontEan = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        PdfPCell celdaEan = new PdfPCell(new Phrase(ean, fontEan));
        celdaEan.setBorder(Rectangle.NO_BORDER);
        celdaEan.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaEan.setColspan(2);
        contenido.addCell(celdaEan);

        // ID en la parte inferior
        Font fontId = FontFactory.getFont(FontFactory.HELVETICA, 12);
        PdfPCell celdaId = new PdfPCell(new Phrase("ID: " + id, fontId));
        celdaId.setBorder(Rectangle.NO_BORDER);
        celdaId.setHorizontalAlignment(Element.ALIGN_LEFT);
        celdaId.setColspan(2);
        celdaId.setPaddingTop(10);
        contenido.addCell(celdaId);

        PdfPCell celdaContenido = new PdfPCell(contenido);
        celdaContenido.setBorder(Rectangle.NO_BORDER);
        tablaPrincipal.addCell(celdaContenido);

        document.add(tablaPrincipal);
    }

    /**
     * Genera código de barras CODE 128 con parámetros SINCRONIZADOS con ImpresorTermicaPOSDIG2406T_2
     *
     * IMPORTANTE: Estos parámetros deben ser EXACTAMENTE los mismos que en ImpresorTermicaPOSDIG2406T_2
     * para garantizar que el código de barras se imprima correctamente.
     *
     * @param codigo Código EAN a codificar
     * @param esEtiquetaCaja true para etiqueta de caja (100x100mm), false para etiqueta normal (90x30mm)
     * @return Imagen del código de barras
     */
    private static Image generarCodigoBarras(String codigo, boolean esEtiquetaCaja) throws Exception {
        try {
            Code128Bean bean = new Code128Bean();

            if (esEtiquetaCaja) {
                // PARÁMETROS PARA ETIQUETA DE CAJA - Sincronizados con ImpresorTermicaPOSDIG2406T_2
                bean.setModuleWidth(0.40);  // 0.40 mm - Óptimo para etiquetas grandes
                bean.setHeight(25);         // 25 mm - Altura generosa
                bean.setFontSize(5);        // Texto legible del código
                bean.setQuietZone(3.5);     // Zona de silencio amplia
            } else {
                // PARÁMETROS PARA ETIQUETA NORMAL - Sincronizados con ImpresorTermicaPOSDIG2406T_2
                bean.setModuleWidth(0.33);  // 0.33 mm - Mínimo recomendado GS1
                bean.setHeight(15);         // 15 mm - Altura estándar
                bean.setFontSize(4);        // Texto visible
                bean.setQuietZone(2.5);     // Zona de silencio adecuada
            }

            bean.doQuietZone(true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // DPI ÓPTIMO para impresoras térmicas - 300 DPI
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(
                    out, "image/png", 300, BufferedImage.TYPE_BYTE_BINARY, false, 0
            );

            bean.generateBarcode(canvas, codigo);
            canvas.finish();

            Image codigoBarras = Image.getInstance(out.toByteArray());
            codigoBarras.setAlignment(Image.ALIGN_CENTER);

            return codigoBarras;

        } catch (Exception e) {
            throw new Exception("Error generando código de barras CODE 128: " + e.getMessage());
        }
    }

    private static PdfPCell crearCeldaVacia() {
        PdfPCell celda = new PdfPCell(new Phrase(""));
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    // ============= MÉTODOS DE CONVENIENCIA =============

    /**
     * Genera PDF de etiquetas normales (90mm x 30mm)
     */
    public static void generarPDFEtiquetasNormales(JTable tabla, String logoPath, String rutaSalida) {
        generarPDF(tabla, logoPath, ModoEtiqueta.ETIQUETA, rutaSalida);
    }

    /**
     * Genera PDF de etiquetas de caja (100mm x 100mm)
     */
    public static void generarPDFEtiquetasCaja(JTable tabla, String logoPath, String rutaSalida) {
        generarPDF(tabla, logoPath, ModoEtiqueta.CAJA, rutaSalida);
    }
}
