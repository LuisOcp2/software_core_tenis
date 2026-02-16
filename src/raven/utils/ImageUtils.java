package raven.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class ImageUtils {
    private static final String DEFAULT_IMAGE_PATH = "/raven/icon/svg/";
    private static boolean defaultImageWarningShown = false;

    public static byte[] getDefaultImageBytes() {
        // Primero intenta cargar desde recursos
        InputStream is = ImageUtils.class.getResourceAsStream(DEFAULT_IMAGE_PATH);
        if (is != null) {
            try {
                return is.readAllBytes();
            } catch (IOException e) {
                logWarning("Error al leer imagen: " + e.getMessage());
            }
        }
        
        // Si falla, crea imagen por defecto
        logWarning("No se encontró la imagen en: " + DEFAULT_IMAGE_PATH);
        return createDefaultImage();
    }

    private static byte[] createDefaultImage() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            
            // Dibuja imagen de placeholder
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, 50, 50);
            g.setColor(Color.BLACK);
            g.drawString("No Image", 5, 25);
            g.dispose();
            
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            logWarning("Error al crear imagen default: " + e.getMessage());
            return new byte[0];
        }
    }

    public static byte[] iconToBytes(ImageIcon icon) {
        if (icon == null) {
            return getDefaultImageBytes();
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage bi = new BufferedImage(
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, bi.getGraphics(), 0, 0);
            ImageIO.write(bi, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return getDefaultImageBytes();
        }
    }

    public static ImageIcon bytesToIcon(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return new ImageIcon(getDefaultImageBytes());
        }
        return new ImageIcon(imageData);
    }

    private static void logWarning(String message) {
        if (!defaultImageWarningShown) {
            System.err.println(message);
            defaultImageWarningShown = true;
        }
    }
    
    public static void resetWarnings() {
        defaultImageWarningShown = false;
    }
}