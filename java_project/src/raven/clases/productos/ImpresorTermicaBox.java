package raven.clases.productos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JTable;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

public class ImpresorTermicaBox implements Printable {

    public enum ModoImpresion {
        CAJA, ETIQUETA
    }

    private JTable tabla;
    private int cantidad;
    private ModoImpresion modo;

    public ImpresorTermicaBox(JTable tabla, int cantidad, ModoImpresion modo) {
        this.tabla = tabla;
        this.cantidad = cantidad;
        this.modo = modo;
    }

    public void imprimir() {
        // Verificar si hay impresoras disponibles antes de intentar imprimir
        if (!PrinterDetector.hayImpresorasDisponibles()) {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "No se detectó ninguna impresora conectada.\n\n" +
                "Por favor, verifica que:\n" +
                "  • La impresora esté conectada por USB y encendida\n" +
                "  • Los drivers estén instalados correctamente\n" +
                "  • Windows reconozca la impresora en Configuración > Dispositivos > Impresoras",
                "Error: Impresora no detectada",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );

            // Mostrar impresoras disponibles en consola para debugging
            PrinterDetector.mostrarImpresorasDisponibles();
            return;
        }

        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);

            // Crear formato personalizado
            PageFormat pf = job.defaultPage();
            Paper paper = new Paper();

            double width, height;
            if (modo == ModoImpresion.CAJA) {
                width = 110 * 2.83; // 100mm en puntos
                height = 110 * 2.83;
            } else {
                width = 80 * 2.83; // 80mm en puntos
                height = 40 * 2.83;
            }

            // Ajustar el área imprimible al total del papel
            paper.setSize(width, height);
            paper.setImageableArea(0, 0, width, height);
            pf.setPaper(paper);

            job.setPrintable(this, pf);

            if (job.printDialog()) {
                job.print();
            }
        } catch (PrinterException e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "Error al imprimir: " + e.getMessage() + "\n\n" +
                "Verifica que la impresora esté lista y correctamente configurada.",
                "Error de Impresión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }

    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        int etiquetaGlobal = 0;
        for (int fila = 0; fila < tabla.getRowCount(); fila++) {
            Boolean seleccionado = (Boolean) tabla.getValueAt(fila, 0);
            if (seleccionado != null && seleccionado) {
                int cantidadFila = Integer.parseInt(tabla.getValueAt(fila, 5).toString());
                if (pageIndex >= etiquetaGlobal && pageIndex < etiquetaGlobal + cantidadFila) {
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    if (modo == ModoImpresion.CAJA) {
                        imprimirModoCaja(g2d, fila);
                    } else {
                        imprimirModoEtiqueta(g2d, fila);
                    }
                    return PAGE_EXISTS;
                }
                etiquetaGlobal += cantidadFila;
            }
        }
        return NO_SUCH_PAGE;
    }

private void imprimirModoCaja(Graphics2D g2d, int fila) throws PrinterException {
    int anchura = 340;  // 120mm ≈ 340 px
    int altura = 226;   // 80mm ≈ 226 px

    // Fondo
    g2d.setColor(Color.WHITE);
    g2d.fillRoundRect(0, 0, anchura, altura, 20, 20);
    g2d.setColor(Color.BLACK);
    g2d.drawRoundRect(0, 0, anchura, altura, 20, 20);

    String ean = tabla.getValueAt(fila, 1).toString();
    String nombre = tabla.getValueAt(fila, 2).toString();
    String color = tabla.getValueAt(fila, 3).toString();
    String id = tabla.getValueAt(fila, 6).toString();

    // Título
    g2d.setFont(new Font("Arial", Font.BOLD, 18));
    g2d.drawString(nombre.toUpperCase(), 20, 35);

    // Color
    g2d.setFont(new Font("Arial", Font.PLAIN, 14));
    g2d.drawString("Color: " + color.toUpperCase(), 20, 60);

    // Código ID rotado
    AffineTransform original = g2d.getTransform();
    g2d.rotate(Math.toRadians(-90));
    g2d.setFont(new Font("Arial", Font.PLAIN, 14));
    g2d.drawString("Codigo: " + id.toUpperCase(), -altura + 20, 20);
    g2d.setTransform(original);

    // Código de barras
    try {
        BufferedImage barcode = generarCodigoBarrasImpresion(ean, ModoImpresion.CAJA);
        int barcodeWidth = 240;  // AJUSTA AQUÍ
        int barcodeHeight = 130;  // AJUSTA AQUÍ
        int barcodeX = (anchura - barcodeWidth) / 2;  // centrado horizontal
        int barcodeY = 80;                            // distancia desde arriba

        g2d.drawImage(barcode, barcodeX, barcodeY, barcodeWidth, barcodeHeight, null);
    } catch (Exception e) {
        throw new PrinterException("Error generando código de barras: " + e.getMessage());
    }

    // EAN debajo del código de barras
    g2d.setFont(new Font("Arial", Font.BOLD, 14));
    int eanWidth = g2d.getFontMetrics().stringWidth(ean);
    int eanX = (anchura - eanWidth) / 2;
    g2d.drawString(ean, eanX, altura - 10);

    // Logo
    try {
        BufferedImage logo = cargarSVG("raven/icon/svg/logo.svg");
        int logoWidth = 70;  // AJUSTA AQUÍ
        int logoHeight = 50; // AJUSTA AQUÍ
        int logoX = anchura - logoWidth - 90;  // margen derecho
        int logoY = 10;                        // margen superior

        g2d.drawImage(logo, logoX, logoY, logoWidth, logoHeight, null);
    } catch (Exception ex) {
        System.err.println("Error cargando logo: " + ex);
    }
}

    private void imprimirModoEtiqueta(Graphics2D g2d, int fila) throws PrinterException {
        int anchura = 227; // 80mm
        int altura = 113;  // 40mm

        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(0, 0, anchura, altura, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(0, 0, anchura, altura, 10, 10);

        String ean = tabla.getValueAt(fila, 1).toString();
        String nombre = tabla.getValueAt(fila, 2).toString();
        String color = tabla.getValueAt(fila, 3).toString();
        String talla = tabla.getValueAt(fila, 4).toString();
        String id = tabla.getValueAt(fila, 6).toString();

        // Nombre
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString(nombre.toUpperCase(), 10, 15);

        // Color
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.drawString("Color:" + color.toUpperCase(), 10, 30);

        // Código ID rotado
        AffineTransform original = g2d.getTransform();
        g2d.rotate(Math.toRadians(-90));
        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
        g2d.drawString("Codigo: " + id.toUpperCase(), -100, 8);
        g2d.setTransform(original);

        // Talla
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(talla, anchura - 60, altura - 10);

        // Código de barras
        try {
            BufferedImage barcode = generarCodigoBarrasImpresion(ean, ModoImpresion.ETIQUETA);
            g2d.drawImage(barcode, 15, 45, 140, 65, null);
        } catch (Exception e) {
            throw new PrinterException("Error generando código de barras: " + e.getMessage());
        }

        // Logo
        try {
            BufferedImage logo = cargarSVG("raven/icon/svg/logo.svg");
            g2d.drawImage(logo, anchura - 65, 5, 60, 60, null);
        } catch (Exception ex) {
            System.err.println("Error cargando logo: " + ex);
        }
    }

    private BufferedImage generarCodigoBarrasImpresion(String codigo, ModoImpresion modo) throws Exception {
        Code128Bean bean = new Code128Bean();

        if (modo == ModoImpresion.CAJA) {
            // OPTIMIZADO: Configuración para etiquetas de caja - Mayor tamaño
            bean.setModuleWidth(0.40);  // 0.40 mm - Óptimo para etiquetas grandes
            bean.setHeight(25);         // 25 mm - Altura generosa para fácil lectura
            bean.setFontSize(6);        // Texto más legible
        } else {
            // OPTIMIZADO: Configuración para etiquetas pequeñas
            bean.setModuleWidth(0.33);  // 0.33 mm - Mínimo estándar GS1
            bean.setHeight(15);         // 15 mm - Altura estándar recomendada
            bean.setFontSize(4);        // Texto visible
        }

        // Zona de silencio (quiet zone) - Esencial para lectura rápida
        bean.doQuietZone(true);
        bean.setQuietZone(2.5);  // 2.5 mm de margen blanco a cada lado

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // DPI alto para impresoras térmicas - Mayor nitidez
        BitmapCanvasProvider canvas = new BitmapCanvasProvider(
                out, "image/png", 300, BufferedImage.TYPE_BYTE_BINARY, false, 0
        );

        bean.generateBarcode(canvas, codigo);
        canvas.finish();

        return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
    }

    private BufferedImage cargarSVG(String nombreRecurso) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(nombreRecurso);
        if (inputStream == null) {
            throw new FileNotFoundException("Recurso no encontrado: " + nombreRecurso);
        }

        PNGTranscoder transcoder = new PNGTranscoder();
        TranscoderInput input = new TranscoderInput(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);
        transcoder.transcode(input, output);

        inputStream.close();
        return ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
    }
}
