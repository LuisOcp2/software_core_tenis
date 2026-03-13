package raven.application.form.productos;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.productos.ModelProductVariant;

/**
 * Renderizador personalizado para mostrar miniaturas de imágenes en celdas de tabla.
 * Muestra una miniatura de 50x50 píxeles si la variante tiene imagen,
 * o un icono placeholder si no tiene imagen.
 */
public class ImageCellRenderer extends DefaultTableCellRenderer {
    
    // Colores del tema oscuro - Paleta corporativa del sistema
    private static final Color TEXT_SECONDARY = new Color(160, 174, 192); // #A0AEC0
    private static final Color SELECTION_BG = new Color(66, 165, 245, 30); // #42A5F5 con transparencia
    private static final Color BG_TABLE = new Color(55, 65, 81);          // #374151
    
    private static final int THUMBNAIL_SIZE = 50;
    private final FontIcon placeholderIcon;
    private final Map<Integer, ImageIcon> imageCache;
    
    /**
     * Constructor que inicializa el renderizador con el icono placeholder
     * y el cache de miniaturas.
     */
    public ImageCellRenderer() {
        this.placeholderIcon = createPlaceholderIcon();
        this.imageCache = new ConcurrentHashMap<>();
        setHorizontalAlignment(CENTER);
        setBackground(BG_TABLE);
    }
    
    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        
        // Aplicar colores del tema oscuro
        if (isSelected) {
            label.setBackground(SELECTION_BG);
        } else {
            label.setBackground(BG_TABLE);
        }
        
        // El value es el objeto ModelProductVariant completo
        if (value instanceof ModelProductVariant) {
            ModelProductVariant variant = (ModelProductVariant) value;
            
            if (variant.hasImage() && variant.getImageBytes() != null) {
                ImageIcon thumbnail = getCachedThumbnail(
                        variant.getVariantId(), 
                        variant.getImageBytes()
                );
                // Si thumbnail es null, usar placeholder
                label.setIcon(thumbnail != null ? thumbnail : placeholderIcon);
            } else {
                label.setIcon(placeholderIcon);
            }
        } else {
            label.setIcon(placeholderIcon);
        }
        
        label.setText(null); // No mostrar texto, solo icono
        return label;
    }
    
    /**
     * Obtiene una miniatura desde el cache o la crea si no existe.
     * 
     * @param variantId ID de la variante para usar como clave de cache
     * @param imageBytes Bytes de la imagen original
     * @return ImageIcon con la miniatura o el placeholder si hay error
     */
    private ImageIcon getCachedThumbnail(int variantId, byte[] imageBytes) {
        // Verificar si ya está en cache
        if (imageCache.containsKey(variantId)) {
            return imageCache.get(variantId);
        }
        
        // Crear nueva miniatura
        ImageIcon thumbnail = createThumbnail(imageBytes);
        
        // Guardar en cache solo si es válida
        if (thumbnail != null) {
            imageCache.put(variantId, thumbnail);
            return thumbnail;
        }
        
        // Si falla la creación, retornar null (el llamador usará placeholder)
        return null;
    }
    
    /**
     * Crea una miniatura escalada de 50x50 píxeles desde los bytes de imagen.
     * 
     * @param imageBytes Bytes de la imagen original
     * @return ImageIcon con la miniatura o null si hay error
     */
    private ImageIcon createThumbnail(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            Image originalImage = ImageIO.read(bais);
            
            if (originalImage == null) {
                return null;
            }
            
            // Escalar imagen a 50x50 con calidad suave
            Image scaledImage = originalImage.getScaledInstance(
                    THUMBNAIL_SIZE, 
                    THUMBNAIL_SIZE, 
                    Image.SCALE_SMOOTH
            );
            
            return new ImageIcon(scaledImage);
            
        } catch (IOException e) {
            // En caso de error, retornar null para usar placeholder
            System.err.println("Error al crear miniatura: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Crea el icono placeholder usando FontAwesome IMAGE icon con color corporativo.
     * 
     * @return FontIcon con el icono placeholder
     */
    private FontIcon createPlaceholderIcon() {
        return FontIcon.of(FontAwesomeSolid.IMAGE, 40, TEXT_SECONDARY);
    }
    
    /**
     * Obtiene el icono placeholder (útil para testing).
     * 
     * @return El icono placeholder
     */
    public FontIcon getPlaceholderIcon() {
        return placeholderIcon;
    }
}
