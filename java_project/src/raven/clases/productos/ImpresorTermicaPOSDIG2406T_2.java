package raven.clases.productos;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

/**
 * Sistema de impresión térmica para POS GS2406T - VERSIÓN FINAL OPTIMIZADA
 * Diseñado específicamente para replicar el formato mostrado en las imágenes
 * 
 * @author Raven
 */

public class ImpresorTermicaPOSDIG2406T_2 implements Printable {

    public enum ModoImpresion {
        CAJA,     // 10cm x 10cm
        ETIQUETA  // 8cm x 4cm
    }

    // Usar dimensiones estándar de 72 DPI para mejor compatibilidad
    private static final double MM_TO_POINTS = 2.83465; // Conversión estándar mm a puntos
    
    // Dimensiones basadas en las imágenes reales
    private static final int ANCHO_ETIQUETA = (int)(80 * MM_TO_POINTS); // ~227 puntos
    private static final int ALTO_ETIQUETA = (int)(40 * MM_TO_POINTS);  // ~113 puntos
    private static final int ANCHO_CAJA = (int)(100 * MM_TO_POINTS);    // ~283 puntos
    private static final int ALTO_CAJA = (int)(100 * MM_TO_POINTS);     // ~283 puntos

    // Variables de instancia
    private JTable tabla;
    private ModoImpresion modo;
    private String nombreImpresora = "GS2406T";
    
    // Márgenes mínimos para evitar cortes
    private int margenIzquierdo = 5;
    private int margenSuperior = 5;
    private int margenDerecho = 5;
    private int margenInferior = 5;

    public ImpresorTermicaPOSDIG2406T_2(JTable tabla, ModoImpresion modo) {
        this.tabla = tabla;
        this.modo = modo;
    }

    public ImpresorTermicaPOSDIG2406T_2(JTable tabla, ModoImpresion modo, String nombreImpresora) {
        this(tabla, modo);
        this.nombreImpresora = nombreImpresora;
    }

    public ImpresorTermicaPOSDIG2406T_2(JTable tabla, ModoImpresion modo, 
                                     int margenIzquierdo, int margenSuperior, 
                                     int margenDerecho, int margenInferior) {
        this(tabla, modo);
        this.margenIzquierdo = margenIzquierdo;
        this.margenSuperior = margenSuperior;
        this.margenDerecho = margenDerecho;
        this.margenInferior = margenInferior;
    }

    public void imprimir() {
        // Verificar si hay impresoras disponibles antes de intentar imprimir
        if (!PrinterDetector.hayImpresorasDisponibles()) {
            JOptionPane.showMessageDialog(
                null,
                "No se detectó ninguna impresora conectada.\n\n" +
                "Por favor, verifica que:\n" +
                "  • La impresora esté conectada por USB y encendida\n" +
                "  • Los drivers estén instalados correctamente\n" +
                "  • Windows reconozca la impresora en Configuración > Dispositivos > Impresoras",
                "Error: Impresora no detectada",
                JOptionPane.ERROR_MESSAGE
            );

            // Mostrar impresoras disponibles en consola para debugging
            PrinterDetector.mostrarImpresorasDisponibles();
            return;
        }

        try {
            PrintService impresoraSeleccionada = buscarImpresora();

            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pageFormat = configurarFormato(job);

            job.setPrintable(this, pageFormat);

            if (impresoraSeleccionada != null) {
                job.setPrintService(impresoraSeleccionada);
            }

            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(null,
                    "Impresión enviada correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null,
                "Error al imprimir: " + e.getMessage(),
                "Error de impresión",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private PrintService buscarImpresora() {
        PrintService[] impresoras = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService impresora : impresoras) {
            String nombre = impresora.getName().toLowerCase();
            if (nombre.contains("gs2406t") || nombre.contains("gs2406") || 
                nombre.contains("gs 2406") || nombre.contains("2406t")) {
                return impresora;
            }
        }
        
        for (PrintService impresora : impresoras) {
            String nombre = impresora.getName().toLowerCase();
            if ((nombre.contains("pos") || nombre.contains("thermal") || nombre.contains("label")) && 
                (nombre.contains("dig") || nombre.contains("gs") || nombre.contains("2406"))) {
                return impresora;
            }
        }
        
        return null;
    }

    private PageFormat configurarFormato(PrinterJob job) {
        PageFormat pageFormat = job.defaultPage();
        Paper paper = new Paper();
        
        if (modo == ModoImpresion.CAJA) {
            paper.setSize(ANCHO_CAJA, ALTO_CAJA);
            paper.setImageableArea(0, 0, ANCHO_CAJA, ALTO_CAJA);
        } else {
            paper.setSize(ANCHO_ETIQUETA, ALTO_ETIQUETA);
            paper.setImageableArea(0, 0, ANCHO_ETIQUETA, ALTO_ETIQUETA);
        }
        
        pageFormat.setPaper(paper);
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        
        return pageFormat;
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
                    
                    // Configuración básica para impresión nítida
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2d.setColor(Color.BLACK);
                    
                    // Aplicar márgenes
                    g2d.translate(margenIzquierdo, margenSuperior);
                    
                    if (modo == ModoImpresion.CAJA) {
                        imprimirEtiquetaCaja(g2d, fila);
                    } else {
                        imprimirEtiquetaNormal(g2d, fila);
                    }
                    
                    return PAGE_EXISTS;
                }
                
                etiquetaGlobal += cantidadFila;
            }
        }
        
        return NO_SUCH_PAGE;
    }

    private void imprimirEtiquetaCaja(Graphics2D g2d, int fila) throws PrinterException {
        int ancho = ANCHO_CAJA - margenIzquierdo - margenDerecho;
        int alto = ALTO_CAJA - margenSuperior - margenInferior;
        
        // Borde principal
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(0, 0, ancho, alto, 15, 15);

        // Obtener datos
        String ean = tabla.getValueAt(fila, 1).toString();
        String nombre = tabla.getValueAt(fila, 2).toString();
        String color = tabla.getValueAt(fila, 3).toString();
        String talla = tabla.getValueAt(fila, 4).toString();
        String id = tabla.getValueAt(fila, 6).toString();

        // Logo en esquina superior derecha
        try {
            BufferedImage logo = cargarSVG("raven/icon/svg/logo.svg");
            if (logo != null) {
                g2d.drawImage(logo, ancho - 70, 8, 60, 60, null);
            }
        } catch (Exception ex) {
            System.err.println("Error cargando logo: " + ex.getMessage());
        }

        // Título del producto
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String nombreCorto = nombre.length() > 22 ? nombre.substring(0, 22) + "..." : nombre;
        g2d.drawString(nombreCorto.toUpperCase(), 10, 30);

        // Color
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Color: " + color.toUpperCase(), 10, 50);

        // Talla grande y centrada
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String tallaText = "TALLA: " + talla;
        FontMetrics fm = g2d.getFontMetrics();
        int tallaWidth = fm.stringWidth(tallaText);
        int tallaX = (ancho - tallaWidth) / 2;
        g2d.drawString(tallaText, tallaX, 100);

        // Código de barras centrado
        try {
            BufferedImage barcode = generarCodigoBarras(ean, true);
            if (barcode != null) {
                int barcodeWidth = 200;
                int barcodeHeight = 60;
                int barcodeX = (ancho - barcodeWidth) / 2;
                int barcodeY = 120;
                g2d.drawImage(barcode, barcodeX, barcodeY, barcodeWidth, barcodeHeight, null);
                
                // EAN debajo del código
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fmEan = g2d.getFontMetrics();
                int eanWidth = fmEan.stringWidth(ean);
                int eanX = (ancho - eanWidth) / 2;
                g2d.drawString(ean, eanX, barcodeY + barcodeHeight + 15);
            }
        } catch (Exception e) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("EAN: " + ean, 10, 150);
        }

        // ID rotado en lateral izquierdo
        AffineTransform original = g2d.getTransform();
        g2d.rotate(Math.toRadians(-90), 8, alto / 2);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("ID: " + id, 8 - 60, alto / 2 + 4);
        g2d.setTransform(original);
    }

    private void imprimirEtiquetaNormal(Graphics2D g2d, int fila) throws PrinterException {
        // Calcular área efectiva después de márgenes
        int ancho = ANCHO_ETIQUETA - margenIzquierdo - margenDerecho;
        int alto = ALTO_ETIQUETA - margenSuperior - margenInferior;
        
        // Borde principal - similar a la imagen de referencia
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, ancho, alto, 8, 8);

        // Obtener datos de la tabla
        String ean = tabla.getValueAt(fila, 1).toString();
        String nombre = tabla.getValueAt(fila, 2).toString();
        String color = tabla.getValueAt(fila, 3).toString();
        String talla = tabla.getValueAt(fila, 4).toString();
        String id = tabla.getValueAt(fila, 6).toString();

        // Logo en esquina superior derecha - como en la imagen
        try {
            BufferedImage logo = cargarSVG("raven/icon/svg/logo.svg");
            if (logo != null) {
                g2d.drawImage(logo, ancho - 35, 3, 30, 30, null);
            }
        } catch (Exception ex) {
            // Dibujar placeholder del logo
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(ancho - 35, 3, 30, 30);
            g2d.setFont(new Font("Arial", Font.PLAIN, 6));
            g2d.drawString("LOGO", ancho - 25, 20);
        }

        // Nombre del producto - línea superior
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        String nombreCorto = nombre.length() > 30 ? nombre.substring(0, 30) + "..." : nombre;
        g2d.drawString(nombreCorto.toUpperCase(), 5, 15);

        // Color - segunda línea
        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
        g2d.drawString("Color: " + color.toUpperCase(), 5, 26);

        // ID en la esquina superior izquierda
        g2d.setFont(new Font("Arial", Font.PLAIN, 7));
        g2d.drawString("ID:" + id, 5, 38);

        // Código de barras - centrado horizontalmente, como en la imagen
        try {
            BufferedImage barcode = generarCodigoBarras(ean, false);
            if (barcode != null) {
                int barcodeWidth = 120;
                int barcodeHeight = 35;
                int barcodeX = (ancho - barcodeWidth) / 2;
                int barcodeY = 45;
                g2d.drawImage(barcode, barcodeX, barcodeY, barcodeWidth, barcodeHeight, null);
                
                // EAN debajo del código de barras - centrado
                g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                FontMetrics fm = g2d.getFontMetrics();
                int eanWidth = fm.stringWidth(ean);
                int eanX = (ancho - eanWidth) / 2;
                g2d.drawString(ean, eanX, barcodeY + barcodeHeight + 10);
            }
        } catch (Exception e) {
            // Fallback si no se puede generar el código de barras
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("EAN: " + ean, 5, 60);
        }

        // Talla grande en esquina inferior derecha - como en la imagen
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fmTalla = g2d.getFontMetrics();
        int tallaWidth = fmTalla.stringWidth(talla);
        g2d.drawString(talla, ancho - tallaWidth - 5, alto - 5);
    }

    private BufferedImage generarCodigoBarras(String codigo, boolean esEtiquetaCaja) throws Exception {
        Code128Bean bean = new Code128Bean();

        if (esEtiquetaCaja) {
            // OPTIMIZADO: Etiquetas de caja - Máxima legibilidad
            bean.setModuleWidth(0.40);  // 0.40 mm - Óptimo para etiquetas grandes
            bean.setHeight(25);         // 25 mm - Altura generosa
            bean.setFontSize(5);        // Texto legible del código
            bean.setQuietZone(3.5);     // Zona de silencio amplia
        } else {
            // OPTIMIZADO: Etiquetas pequeñas - Estándar GS1
            bean.setModuleWidth(0.33);  // 0.33 mm - Mínimo recomendado
            bean.setHeight(15);         // 15 mm - Altura estándar
            bean.setFontSize(4);        // Texto visible
            bean.setQuietZone(2.5);     // Zona de silencio adecuada
        }

        bean.doQuietZone(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // DPI óptimo para impresoras térmicas
        BitmapCanvasProvider canvas = new BitmapCanvasProvider(
                out, "image/png", 300, BufferedImage.TYPE_BYTE_BINARY, false, 0
        );

        bean.generateBarcode(canvas, codigo);
        canvas.finish();

        return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
    }

    private BufferedImage cargarSVG(String rutaRecurso) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(rutaRecurso);
        
        if (inputStream == null) {
            inputStream = getClass().getResourceAsStream("/" + rutaRecurso);
            if (inputStream == null) {
                return null; // Retornar null si no se encuentra
            }
        }

        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 100f);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 100f);
        
        TranscoderInput input = new TranscoderInput(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);
        
        transcoder.transcode(input, output);
        
        inputStream.close();
        outputStream.flush();
        
        return ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    // Métodos estáticos de conveniencia
    public static void imprimirEtiquetasCaja(JTable tabla) {
        ImpresorTermicaPOSDIG2406T_2 impresor = new ImpresorTermicaPOSDIG2406T_2(tabla, ModoImpresion.CAJA);
        impresor.imprimir();
    }

    public static void imprimirEtiquetasNormales(JTable tabla) {
        ImpresorTermicaPOSDIG2406T_2 impresor = new ImpresorTermicaPOSDIG2406T_2(tabla, ModoImpresion.ETIQUETA);
        impresor.imprimir();
    }

    public void imprimirDirecto() {
        try {
            PrintService impresoraSeleccionada = buscarImpresora();
            
            if (impresoraSeleccionada == null) {
                throw new PrinterException("No se encontró la impresora " + nombreImpresora);
            }

            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pageFormat = configurarFormato(job);
            
            job.setPrintable(this, pageFormat);
            job.setPrintService(impresoraSeleccionada);
            
            job.print();
            
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, 
                "Error al imprimir: " + e.getMessage(), 
                "Error de impresión", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Métodos para ajustar configuración
    public void setMargenes(int izquierdo, int superior, int derecho, int inferior) {
        this.margenIzquierdo = izquierdo;
        this.margenSuperior = superior;
        this.margenDerecho = derecho;
        this.margenInferior = inferior;
    }

    public void setNombreImpresora(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
    }
}

/**
 * Clase de prueba optimizada
 */
class TestImpresora {
    public static void main(String[] args) {
        Object[][] datos = {
            {true, "9876543210987", "PANTALON JEAN", "NEGRO", "32", 1, "CP002"},
            {true, "1234567890123", "CAMISA POLO", "AZUL", "M", 2, "CP001"}
        };
        
        String[] columnas = {"Selección", "EAN", "Nombre", "Color", "Talla", "Cantidad", "ID"};
        JTable tabla = new JTable(datos, columnas);
        
        // Usar con márgenes mínimos
        ImpresorTermicaPOSDIG2406T_2 impresor = new ImpresorTermicaPOSDIG2406T_2(tabla, 
            ImpresorTermicaPOSDIG2406T_2.ModoImpresion.ETIQUETA);
        
        // Márgenes mínimos para aprovechar toda el área
        impresor.setMargenes(3, 3, 15, 3);
        
        impresor.imprimir();
    }
}