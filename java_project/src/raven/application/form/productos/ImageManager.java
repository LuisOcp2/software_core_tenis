package raven.application.form.productos;

import raven.clases.productos.ServiceProductVariant;
import raven.controlador.productos.ModelProductVariant;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona operaciones de imagen para variantes de productos.
 * Proporciona validación de archivos, carga de bytes y gestión de cache.
 */
public class ImageManager {
    
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final Set<String> SUPPORTED_FORMATS = 
        Set.of("jpg", "jpeg", "png", "webp");
    
    private final ServiceProductVariant variantService;
    private final Map<Integer, byte[]> imageCache;
    
    /**
     * Constructor que inicializa el ImageManager con el servicio de variantes.
     * 
     * @param variantService Servicio para operaciones de persistencia de variantes
     */
    public ImageManager(ServiceProductVariant variantService) {
        this.variantService = variantService;
        this.imageCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Valida un archivo de imagen verificando formato y tamaño.
     * 
     * @param file El archivo a validar
     * @return ValidationResult indicando si es válido o el error
     */
    public ValidationResult validateImageFile(File file) {
        if (file == null || !file.exists()) {
            return ValidationResult.error("El archivo no existe");
        }
        
        if (!file.isFile()) {
            return ValidationResult.error("La ruta no corresponde a un archivo");
        }
        
        // Verificar tamaño
        long fileSize = file.length();
        if (fileSize > MAX_IMAGE_SIZE_BYTES) {
            return ValidationResult.error(
                String.format("El archivo excede el tamaño máximo permitido de 5MB. Tamaño actual: %.2f MB",
                    fileSize / (1024.0 * 1024.0))
            );
        }
        
        // Verificar formato (extensión)
        String fileName = file.getName().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ValidationResult.error(
                "El archivo no tiene extensión. Formatos soportados: JPG, JPEG, PNG, WEBP"
            );
        }
        
        String extension = fileName.substring(lastDotIndex + 1);
        if (!SUPPORTED_FORMATS.contains(extension)) {
            return ValidationResult.error(
                String.format("Formato no soportado: %s. Formatos soportados: JPG, JPEG, PNG, WEBP",
                    extension.toUpperCase())
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Lee los bytes de un archivo de imagen.
     * 
     * @param file El archivo a leer
     * @return Array de bytes con el contenido del archivo
     * @throws IOException Si hay error al leer el archivo
     */
    public byte[] loadImageBytes(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("El archivo no existe");
        }
        
        return Files.readAllBytes(file.toPath());
    }
    
    /**
     * Guarda la imagen de una variante en la base de datos.
     * Actualiza los campos imageBytes y hasImage de la variante y persiste los cambios.
     * 
     * @param variant La variante a la que se le asignará la imagen
     * @param imageBytes Los bytes de la imagen a guardar
     * @throws SQLException Si hay error al guardar en la base de datos
     */
    public void saveVariantImage(ModelProductVariant variant, byte[] imageBytes) throws SQLException {
        if (variant == null) {
            throw new IllegalArgumentException("La variante no puede ser null");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Los bytes de la imagen no pueden ser null o vacíos");
        }
        
        variant.setImageBytes(imageBytes);
        variant.setHasImage(true);
        variantService.upsertVariant(variant);
        
        // Actualizar cache
        imageCache.put(variant.getVariantId(), imageBytes);
    }
    
    /**
     * Obtiene la imagen de una variante, primero verificando el cache y luego la base de datos.
     * 
     * @param variantId El ID de la variante
     * @return Los bytes de la imagen o null si no existe
     * @throws SQLException Si hay error al consultar la base de datos
     */
    public byte[] getVariantImage(int variantId) throws SQLException {
        // Verificar cache primero
        if (imageCache.containsKey(variantId)) {
            return imageCache.get(variantId);
        }
        
        // Cargar desde base de datos
        byte[] imageBytes = variantService.getVariantImage(variantId);
        
        // Guardar en cache si existe
        if (imageBytes != null && imageBytes.length > 0) {
            imageCache.put(variantId, imageBytes);
        }
        
        return imageBytes;
    }
    
    /**
     * Elimina la imagen de una variante.
     * Limpia los campos imageBytes y hasImage y actualiza la base de datos.
     * 
     * @param variant La variante de la que se eliminará la imagen
     * @throws SQLException Si hay error al actualizar la base de datos
     */
    public void removeVariantImage(ModelProductVariant variant) throws SQLException {
        if (variant == null) {
            throw new IllegalArgumentException("La variante no puede ser null");
        }
        
        variant.setImageBytes(null);
        variant.setHasImage(false);
        variantService.upsertVariant(variant);
        
        // Limpiar cache
        imageCache.remove(variant.getVariantId());
    }
    
    /**
     * Clase de resultado para operaciones de validación.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        /**
         * Crea un resultado exitoso.
         * 
         * @return ValidationResult indicando éxito
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        /**
         * Crea un resultado de error con mensaje.
         * 
         * @param message Mensaje de error descriptivo
         * @return ValidationResult indicando error
         */
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        /**
         * Indica si la validación fue exitosa.
         * 
         * @return true si es válido, false en caso contrario
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Obtiene el mensaje de error si la validación falló.
         * 
         * @return Mensaje de error o null si fue exitoso
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
