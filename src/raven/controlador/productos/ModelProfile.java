package raven.controlador.productos;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import raven.extras.AvatarIcon;

public class ModelProfile {

    private static final String DEFAULT_IMAGE_PATH = "/raven/icon/png/error.png";
    private static boolean defaultImageWarningShown = false;
    private byte[] imageBytes;
    private Icon icon;
    private Icon avatar;
    private File path;
    private static boolean warningsEnabled = true;

    public ModelProfile() {
    }

    public ModelProfile(File path) {
        this.path = path;
    }

    public ModelProfile(byte[] bytes) {
        if (bytes != null) {
            icon = new ImageIcon(bytes);
        } else {
            if (warningsEnabled) {
                System.out.println("Info: Imagen no disponible, usando placeholder");
                warningsEnabled = false;
            }
            icon = bytesToIcon(getDefaultImageBytes());
        }
    }

    public ModelProfile(Icon icon) {
        this.icon = icon;
    }

     public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }
     public byte[] getImageBytes() {
        return imageBytes;
    }
     
     public boolean hasImageBytes() {
        return imageBytes != null && imageBytes.length > 0;
    }
     public javax.swing.ImageIcon getImageIcon() {
        if (hasImageBytes()) {
            try {
                return new javax.swing.ImageIcon(imageBytes);
            } catch (Exception e) {
                System.err.println("Error creando ImageIcon: " + e.getMessage());
                return null;
            }
        }
        return null;
    }
     /**
     * @param width Ancho deseado
     * @param height Alto deseado
     * @return ImageIcon redimensionado o null si no hay imagen
     */
    public javax.swing.ImageIcon getResizedImageIcon(int width, int height) {
        if (hasImageBytes()) {
            try {
                javax.swing.ImageIcon originalIcon = new javax.swing.ImageIcon(imageBytes);
                java.awt.Image img = originalIcon.getImage();
                java.awt.Image resizedImg = img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                return new javax.swing.ImageIcon(resizedImg);
            } catch (Exception e) {
                System.err.println("Error redimensionando imagen: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    // Métodos de ImageUtils integrados
    private static byte[] getDefaultImageBytes() {
        InputStream is = ModelProfile.class.getResourceAsStream(DEFAULT_IMAGE_PATH);
        if (is != null) {
            try {
                return is.readAllBytes();
            } catch (IOException e) {
                logWarning("Error al leer imagen: " + e.getMessage());
            }
        }
        logWarning("No se encontró la imagen en: " + DEFAULT_IMAGE_PATH);
        return createDefaultImage();
    }

    private static byte[] createDefaultImage() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
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

    public Icon getIcon() {
        return icon != null ? icon : bytesToIcon(getDefaultImageBytes());
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public Icon getAvatarIcon() {
        // Priorizar imageBytes si están disponibles
        if (hasImageBytes()) {
            icon = new ImageIcon(imageBytes);
            avatar = null; // Resetear avatar para que se recree con la nueva imagen
        } else if (icon == null) {
            icon = bytesToIcon(getDefaultImageBytes());
        }

        if (avatar == null) {
            AvatarIcon ai = new AvatarIcon(icon, 50, 50, 3f);
            ai.setType(AvatarIcon.Type.MASK_SQUIRCLE);
            avatar = ai;
        }
        return avatar;
    }
}
