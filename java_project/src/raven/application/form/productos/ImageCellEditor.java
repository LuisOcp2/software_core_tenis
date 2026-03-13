package raven.application.form.productos;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.controlador.productos.ModelProductVariant;

/**
 * Editor personalizado para la columna de imagen en la tabla de variantes.
 * Permite seleccionar y subir imágenes para variantes de productos.
 */
public class ImageCellEditor extends AbstractCellEditor implements TableCellEditor {
    
    // Colores del tema oscuro - Paleta corporativa del sistema
    private static final Color ACCENT_ORANGE = new Color(251, 146, 60);  // #FB923C
    private static final Color BG_TABLE = new Color(55, 65, 81);         // #374151
    private static final Color TEXT_PRIMARY = new Color(236, 240, 241);  // #ecf0f1
    
    private final JButton editorButton;
    private ModelProductVariant currentVariant;
    private final ImageManager imageManager;
    private final JTable parentTable;
    
    /**
     * Constructor que inicializa el editor con el ImageManager y la tabla padre.
     * 
     * @param imageManager Gestor de imágenes para validación y persistencia
     * @param table Tabla padre que contiene este editor
     */
    public ImageCellEditor(ImageManager imageManager, JTable table) {
        this.imageManager = imageManager;
        this.parentTable = table;
        
        // Crear botón con icono de cámara y estilos corporativos oscuros
        this.editorButton = new JButton();
        this.editorButton.setIcon(FontIcon.of(FontAwesomeSolid.CAMERA, 16, ACCENT_ORANGE));
        this.editorButton.setForeground(TEXT_PRIMARY);
        this.editorButton.setBackground(BG_TABLE);
        this.editorButton.setFocusPainted(false);
        this.editorButton.setBorderPainted(true);
        this.editorButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        this.editorButton.addActionListener(e -> selectImage());
    }
    
    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {
        
        // Guardar la variante actual
        if (value instanceof ModelProductVariant) {
            this.currentVariant = (ModelProductVariant) value;
        }
        
        return editorButton;
    }
    
    @Override
    public Object getCellEditorValue() {
        return currentVariant;
    }
    
    /**
     * Abre un diálogo de selección de archivo para elegir una imagen.
     * Si se selecciona un archivo válido, lo procesa y actualiza la variante.
     */
    private void selectImage() {
        JFileChooser fileChooser = createImageFileChooser();
        int result = fileChooser.showOpenDialog(parentTable);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processImageFile(selectedFile);
        }
        
        // Detener la edición
        fireEditingStopped();
    }
    
    /**
     * Procesa el archivo de imagen seleccionado: valida, carga y guarda.
     * Muestra mensajes de éxito o error según el resultado.
     * 
     * @param file El archivo de imagen a procesar
     */
    private void processImageFile(File file) {
        if (currentVariant == null) {
            showError("No se ha seleccionado una variante");
            return;
        }
        
        // Validar archivo
        ImageManager.ValidationResult validation = imageManager.validateImageFile(file);
        if (!validation.isValid()) {
            showError(validation.getErrorMessage());
            return;
        }
        
        try {
            // Cargar bytes de la imagen
            byte[] imageBytes = imageManager.loadImageBytes(file);
            
            // Guardar imagen en la variante
            imageManager.saveVariantImage(currentVariant, imageBytes);
            
            // Mostrar mensaje de éxito
            showSuccess("Imagen actualizada correctamente");
            
            // Refrescar la tabla para mostrar la nueva imagen
            parentTable.repaint();
            
        } catch (IOException e) {
            showError("Error al leer el archivo de imagen: " + e.getMessage());
            System.err.println("Error al cargar imagen: " + e.getMessage());
            
        } catch (SQLException e) {
            showError("Error al guardar la imagen en la base de datos");
            System.err.println("Error al guardar imagen en BD: " + e.getMessage());
        }
    }
    
    /**
     * Crea un JFileChooser configurado para seleccionar archivos de imagen.
     * Incluye filtro para formatos soportados: JPG, JPEG, PNG, WEBP.
     * 
     * @return JFileChooser configurado
     */
    private JFileChooser createImageFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar imagen para variante");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        // Crear filtro para imágenes
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Imágenes (JPG, JPEG, PNG, WEBP)", 
                "jpg", "jpeg", "png", "webp"
        );
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        return fileChooser;
    }
    
    /**
     * Muestra un mensaje de error al usuario.
     * 
     * @param message Mensaje de error a mostrar
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                parentTable,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
    
    /**
     * Muestra un mensaje de éxito al usuario.
     * 
     * @param message Mensaje de éxito a mostrar
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
                parentTable,
                message,
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
