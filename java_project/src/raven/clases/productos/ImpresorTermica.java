    package raven.clases.productos;

    import java.awt.Color;
    import java.awt.Font;
    import java.awt.Graphics;
    import java.awt.Graphics2D;
    import java.awt.Image;
    import java.awt.geom.AffineTransform;
    import java.awt.image.BufferedImage;
    import java.awt.print.PageFormat;
    import java.awt.print.Printable;
    import java.awt.print.PrinterException;
    import java.awt.print.PrinterJob;
    import java.io.ByteArrayInputStream;
    import java.io.ByteArrayOutputStream;
    import java.io.File;
    import java.io.FileInputStream;
    import java.io.FileNotFoundException;
    import java.io.InputStream;
    import javax.imageio.ImageIO;
    import javax.swing.JTable;
    import org.apache.batik.transcoder.TranscoderInput;
    import org.apache.batik.transcoder.TranscoderOutput;
    import org.apache.batik.transcoder.image.PNGTranscoder;
    import org.krysalis.barcode4j.impl.code128.Code128Bean;
    import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

    public class ImpresorTermica implements Printable {

        private JTable tabla;
        private Image logo;
        private int cantidad;

        public ImpresorTermica(JTable tabla, int cantidad) {
            this.tabla = tabla;
            this.cantidad = cantidad;
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
        // En lugar de usar totalEtiquetas, vamos a mapear directamente el pageIndex a la fila y etiqueta correspondiente
        int etiquetaGlobal = 0;

        // Recorremos la tabla para encontrar la etiqueta correspondiente al pageIndex
        for (int fila = 0; fila < tabla.getRowCount(); fila++) {
            Boolean seleccionado = (Boolean) tabla.getValueAt(fila, 0);
            if (seleccionado != null && seleccionado) {
                int cantidadFila = Integer.parseInt(tabla.getValueAt(fila, 5).toString());

                // Comprobamos si el pageIndex está en el rango de esta fila
                if (pageIndex >= etiquetaGlobal && pageIndex < etiquetaGlobal + cantidadFila) {
                    // Hemos encontrado la etiqueta correspondiente a este pageIndex
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    // Configuración para tamaño 6cm x 4cm (aproximadamente 170 x 113 puntos)
                    g2d.setColor(Color.WHITE);
                    g2d.fillRoundRect(0, 0, 200, 113, 10, 10);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRoundRect(0, 0, 200, 113, 10, 10);

                    // Recuperar datos de la tabla
                    String ean = tabla.getValueAt(fila, 1).toString();
                    String nombre = tabla.getValueAt(fila, 2).toString();
                    String color = tabla.getValueAt(fila, 3).toString();
                    String talla = tabla.getValueAt(fila, 4).toString();
                    String id = tabla.getValueAt(fila, 6).toString();

                    // Nombre del producto
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    g2d.drawString(nombre.toUpperCase(), 10, 20);

                    // Color
                    g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                    g2d.drawString("Color:" + color.toUpperCase(), 10, 35);

                    // Código ID rotado
                    AffineTransform original = g2d.getTransform();
                    g2d.rotate(Math.toRadians(-90));
                    g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                    g2d.drawString("Codigo: " + id.toUpperCase(), -100, 10);
                    g2d.setTransform(original);

                    // Talla
                    g2d.setFont(new Font("Arial", Font.BOLD, 20));
                    g2d.drawString(talla, 120, 100);

                    // Código de barras
                    try {
                        BufferedImage barcode = generarCodigoBarrasImpresion(ean);
                        g2d.drawImage(barcode, 15, 45, 100, 50, null);
                    } catch (Exception e) {
                        throw new PrinterException("Error generando código de barras: " + e.getMessage());
                    }

                    // Logo
                    try {
                        BufferedImage logo1 = cargarSVG("raven/icon/svg/logo.svg");
                        g2d.drawImage(logo1, 140, 10, 40, 35, null);
                    } catch (Exception ex) {
                        System.err.println("Error detallado al cargar logo: " + ex);
                    }

                    return PAGE_EXISTS;
                }

                // Incrementamos el contador global de etiquetas
                etiquetaGlobal += cantidadFila;
            }
        }

        // Si llegamos aquí, es que no hay más etiquetas para imprimir
        return NO_SUCH_PAGE;
    }
    // Método optimizado para generar códigos de barras legibles por pistolas lectoras
        private BufferedImage generarCodigoBarrasImpresion(String codigo) throws Exception {
            Code128Bean bean = new Code128Bean();

            // OPTIMIZADO: Ancho de módulo según estándar GS1 para mejor lectura
            bean.setModuleWidth(0.33);  // 0.33 mm - Mínimo recomendado para buena legibilidad

            // OPTIMIZADO: Altura aumentada para mayor margen de error en lectura
            bean.setHeight(15);  // 15 mm - Altura estándar recomendada

            // Zona de silencio (quiet zone) - Crítico para lectura correcta
            bean.doQuietZone(true);
            bean.setQuietZone(2.5);  // 2.5 mm de margen a cada lado

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // OPTIMIZADO: DPI aumentado a 300 para mejor calidad
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(
                    out, "image/png", 300, BufferedImage.TYPE_BYTE_BINARY, false, 0
            );

            bean.generateBarcode(canvas, codigo);
            canvas.finish();

            return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        }

        private BufferedImage cargarSVG(String nombreRecurso) throws Exception {
            // En lugar de usar File, usa getResourceAsStream
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(nombreRecurso);
            if (inputStream == null) {
                throw new FileNotFoundException("No se pudo encontrar el recurso: " + nombreRecurso);
            }

            // El resto del código sigue igual
            PNGTranscoder transcoder = new PNGTranscoder();
            TranscoderInput input = new TranscoderInput(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);
            transcoder.transcode(input, output);

            inputStream.close();
            return ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
        }

    }
