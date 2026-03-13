package raven.clases.productos.barcode;

import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.tools.UnitConv;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Configurador optimizado V3 (Adaptive) para códigos de barras de alta legibilidad.
 * 
 * MEJORAS:
 * - Cálculo adaptativo del ancho del módulo (Adaptive Module Width).
 * - Garantiza que el código ocupe todo el ancho disponible.
 * - Mantiene resolución ultra-alta (600 DPI) para evitar aliasing.
 * - Manejo robusto de códigos largos (>20 caracteres).
 */
public class BarcodeConfigOptimizado {
    
    // ============================================
    // CONSTANTES
    // ============================================
    
    /** Mínimo ancho de módulo seguro para impresoras térmicas de 203 DPI (aprox 1 pixel) */
    private static final double MODULE_WIDTH_MIN_MM = 0.17; 
    
    /** Ancho de módulo ideal para escaneo fácil */
    private static final double MODULE_WIDTH_IDEAL_MM = 0.33; 
    
    /** DPI de generación interna (Alta resolución para escalado suave) */
    private static final int GENERATION_DPI = 600;

    /**
     * Genera un código de barras optimizado para el espacio dado.
     * Utiliza lógica adaptativa para maximizar el tamaño de las barras.
     * 
     * @param code Código a generar
     * @param targetWidthPt Ancho disponible en puntos (1/72 inch)
     * @param targetHeightPt Alto disponible en puntos (1/72 inch)
     * @return BufferedImage optimizado (TYPE_BYTE_BINARY o TYPE_BYTE_GRAY)
     */
    public static BufferedImage generarCodigoOptimizado(String code, int targetWidthPt, int targetHeightPt) {
        return generarCodigoOptimizado(code, targetWidthPt, targetHeightPt, GENERATION_DPI);
    }

    /**
     * Genera un código de barras optimizado con DPI específico.
     * 
     * @param code Código a generar
     * @param targetWidthPt Ancho disponible en puntos (1/72 inch)
     * @param targetHeightPt Alto disponible en puntos (1/72 inch)
     * @param targetDPI Resolución de generación (DPI)
     * @return BufferedImage optimizado
     */
    public static BufferedImage generarCodigoOptimizado(String code, int targetWidthPt, int targetHeightPt, int targetDPI) {
        if (code == null || code.trim().isEmpty()) {
            return generarImagenVacia(targetWidthPt, targetHeightPt);
        }
        
        String codigoLimpio = code.trim();
        
        // Log para debugging
        System.out.println(String.format("[BARCODE V3] Generando: '%s' | Target: %dx%d pt | DPI: %d", 
                codigoLimpio, targetWidthPt, targetHeightPt, targetDPI));

        try {
            // 1. Determinar el Bean (EAN13 o Code128)
            org.krysalis.barcode4j.impl.AbstractBarcodeBean bean;
            boolean esEan13 = isEAN13Valido(codigoLimpio);
            
            if (esEan13) {
                bean = new EAN13Bean();
            } else {
                bean = new Code128Bean();
            }

            // 2. Configuración Base
            bean.setMsgPosition(HumanReadablePlacement.HRP_NONE); // Texto dibujado externamente
            bean.setFontSize(0);
            bean.doQuietZone(true);
            
            // 3. Cálculo Adaptativo del Ancho de Módulo
            // Convertir ancho objetivo a mm
            double targetWidthMM = UnitConv.pt2mm(targetWidthPt);
            
            // Configurar quiet zone (2.5mm a cada lado es estándar, total 5mm)
            // Para códigos muy largos, reducimos un poco si es necesario, pero mantenemos mínimo 10 módulos
            double quietZoneMM = 2.5; 
            bean.setQuietZone(quietZoneMM);
            
            // Calcular ancho de módulo óptimo
            // Primero, calculamos cuánto ocuparía con 1mm de módulo
            bean.setModuleWidth(1.0);
            
            // Fix: Create Graphics2D from image for CanvasProvider
            BufferedImage dummyImg = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D dummyG2d = dummyImg.createGraphics();
            try {
                org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider dummyCanvas = 
                    new org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider(
                        dummyG2d, 0
                    );
                bean.generateBarcode(dummyCanvas, codigoLimpio);
            } finally {
                dummyG2d.dispose();
            }
            
            org.krysalis.barcode4j.BarcodeDimension dim = bean.calcDimensions(codigoLimpio);
            
            double baseWidthMM = dim.getWidthPlusQuiet(); // Ancho en mm con módulo 1.0
            
            // Factor de escala = Ancho Disponible / Ancho Base
            double optimalModuleWidth = targetWidthMM / baseWidthMM;
            
            // Debug info
            System.out.println(String.format("[BARCODE] Ancho Disponible: %.2f mm | Base(mod=1): %.2f mm | Calc Mod: %.4f mm", 
                    targetWidthMM, baseWidthMM, optimalModuleWidth));

            // 4. Validar y Clampear Ancho de Módulo
            if (optimalModuleWidth < MODULE_WIDTH_MIN_MM) {
                System.out.println("[BARCODE] ALERTA: Código muy largo para el espacio. Forzando mínimo legible.");
                optimalModuleWidth = MODULE_WIDTH_MIN_MM; // Forzamos el mínimo aunque se salga del ancho
            } else if (optimalModuleWidth > 0.8) {
                optimalModuleWidth = 0.8; // No hacer barras absurdamente gordas
            }
            
            // Ajustar para que encaje exactamente en píxeles enteros del DPI objetivo (Snap to Grid)
            // Esto es CRÍTICO para impresoras térmicas de baja resolución (203 DPI)
            double pixelWidthMM = 25.4 / targetDPI;
            // Usamos floor para asegurar que no exceda el ancho disponible
            int pixelsPerModule = (int) Math.floor(optimalModuleWidth / pixelWidthMM);
            if (pixelsPerModule < 1) pixelsPerModule = 1; // Mínimo 1 pixel
            
            double snappedModuleWidth = pixelsPerModule * pixelWidthMM;
            System.out.println(String.format("[BARCODE] Snap to Grid (%d DPI): %.4f mm -> %d px -> %.4f mm", 
                    targetDPI, optimalModuleWidth, pixelsPerModule, snappedModuleWidth));
            
            bean.setModuleWidth(snappedModuleWidth);
            
            // Ajustar altura (mantener proporción o llenar alto)
            double targetHeightMM = UnitConv.pt2mm(targetHeightPt);
            // Altura de barras: Usar el 80% del alto disponible o mínimo 15mm
            double barHeight = Math.max(12.0, targetHeightMM * 0.9);
            bean.setBarHeight(barHeight);
            
            // Recalcular dimensiones finales para el canvas
            // Nota: Al usar snappedModuleWidth, el ancho total puede variar ligeramente del target
            
            // 5. Generar Imagen en la Resolución Objetivo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(
                    baos, 
                    "image/png", 
                    targetDPI, 
                    BufferedImage.TYPE_BYTE_BINARY, 
                    false, 
                    0
            );
            
            bean.generateBarcode(canvas, codigoLimpio);
            canvas.finish();
            
            BufferedImage finalImage = ImageIO.read(new java.io.ByteArrayInputStream(baos.toByteArray()));
            
            return finalImage;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[BARCODE ERROR] " + e.getMessage());
            return generarImagenVacia(targetWidthPt, targetHeightPt);
        }
    }
    
    // ============================================
    // UTILIDADES
    // ============================================
    
    private static boolean isEAN13Valido(String code) {
        if (code == null || code.length() != 13) return false;
        for (char c : code.toCharArray()) if (!Character.isDigit(c)) return false;
        // Checksum simple
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = code.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == (code.charAt(12) - '0');
    }
    
    private static BufferedImage generarImagenVacia(int width, int height) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.RED);
        g.drawLine(0, 0, width, height);
        g.drawLine(0, height, width, 0);
        g.dispose();
        return img;
    }
    
    /**
     * Método de compatibilidad para logs
     */
    public static String obtenerInfoConfiguracion(String code) {
        return "Adaptive V3: " + (code != null ? code.length() : 0) + " chars";
    }
}
