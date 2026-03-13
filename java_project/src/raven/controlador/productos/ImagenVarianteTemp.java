package raven.controlador.productos;

import java.io.File;

/**
 * Clase temporal para almacenar información de imágenes de variantes
 * antes de guardarlas en la base de datos
 * 
 * @author CrisDEV
 */
public class ImagenVarianteTemp {
    
    public File archivoImagen;        // Archivo de imagen seleccionado
    public int indiceVariante;        // Índice de la variante en la lista
    public String sizeName;           // Nombre de la talla (para logs)
    public String colorName;          // Nombre del color (para logs)
    public String type;               // Tipo: "Par" o "Caja"
    
    // Constructor vacío
    public ImagenVarianteTemp() {
    }
    
    // Constructor con datos
    public ImagenVarianteTemp(File archivoImagen, int indiceVariante, String sizeName, String colorName, String type) {
        this.archivoImagen = archivoImagen;
        this.indiceVariante = indiceVariante;
        this.sizeName = sizeName;
        this.colorName = colorName;
        this.type = type;
    }
    
    // Métodos de utilidad
    public boolean tieneArchivo() {
        return archivoImagen != null && archivoImagen.exists();
    }
    
    public String getNombreArchivo() {
        return archivoImagen != null ? archivoImagen.getName() : "Sin archivo";
    }
    
    public long getTamañoArchivo() {
        return archivoImagen != null ? archivoImagen.length() : 0;
    }
    
    public String getDescripcionCompleta() {
        return String.format("Variante %d: %s - %s (%s) - Archivo: %s", 
                           indiceVariante, sizeName, colorName, type, getNombreArchivo());
    }
    
    @Override
    public String toString() {
        return getDescripcionCompleta();
    }
}