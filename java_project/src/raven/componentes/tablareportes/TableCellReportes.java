package raven.componentes.tablareportes;

import raven.componentes.*;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Font;
import java.util.Map;
import javax.swing.ImageIcon;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelProfile;

/**
 *
 * @author RAVEN
 */
public class TableCellReportes extends javax.swing.JPanel {

    public TableCellReportes(ModelProduct product, Font font) {// Ruta de la imagen

        initComponents();

        // Configure fonts
        lbName.setFont(font);
        lbLocation.setFont(font);

        // Set product name
        lbName.setText(product.getName());

        // Format the description like in the example: Color, Talla X, Ref: XXXXX
        StringBuilder description = new StringBuilder();

        // Add color if available
        if (product.getColor() != null && !product.getColor().isEmpty()) {
            description.append(product.getColor());
        }

        // Add size/talla if available
        if (product.getSize() != null && !product.getSize().isEmpty()) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append("Talla ").append(product.getSize());
        }

        // Add barcode reference if available
        if (product.getBarcode() != null && !product.getBarcode().isEmpty()) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append("Ref: ").append(product.getBarcode());
        }

        // Set the formatted description
        lbLocation.setText(description.toString());

        // Set style for the secondary text
        lbLocation.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground");

        // Set product image from profile if available
        if (product.getProfile() != null && product.getProfile().getAvatarIcon() != null) {
            pic.setIcon(product.getProfile().getAvatarIcon());
        } else {
            // Try to load a default product icon
            tryLoadDefaultIcon();
        }

    }
    // Constructor para usar con Map (datos de la base de datos)

    public TableCellReportes(Map<String, Object> data, Font font) {
        initComponents();

        // Configure fonts
        lbName.setFont(font);
        lbLocation.setFont(font);

        // Set product name
        String productName = data.get("producto") != null ? data.get("producto").toString() : "";
        lbName.setText(productName);

        // Format the description like in the example: Color, Talla X, Ref: XXXXX
        StringBuilder description = new StringBuilder();

        // Add color if available
        String color = data.containsKey("color") && data.get("color") != null ? data.get("color").toString() : "";
        if (!color.isEmpty()) {
            description.append(color);
        }

        // Add size/talla if available
        String talla = data.containsKey("talla") && data.get("talla") != null ? data.get("talla").toString() : "";
        if (!talla.isEmpty()) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append("Talla ").append(talla);
        }

        // Add barcode reference if available
        String barcode = data.containsKey("barcode") && data.get("barcode") != null ? data.get("barcode").toString()
                : (data.containsKey("id_producto") ? data.get("id_producto").toString() : "");
        if (!barcode.isEmpty()) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append("Ref: ").append(barcode);
        }

        // Set the formatted description
        lbLocation.setText(description.toString());

        // Set style for the secondary text
        lbLocation.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground");

        // Try to load a default product icon
        tryLoadDefaultIcon();
    }

    private void tryLoadDefaultIcon() {
        try {
            // Try common asset paths - adjust these to match your project structure
            /**String[] possiblePaths = {
                "/raven/icon/png/defaultshoes.png",
                "/raven/assets/productos/default.png",
                "/raven/assets/iconos/product.png",
                "/raven/assets/product.png"
            };**/
             String[] possiblePaths = {
            "/raven/icon/defaultshoes.png",    // Existe en tu proyecto (PNG)
            "/raven/icon/logo.png",            // Existe en tu proyecto (PNG)
            "/raven/icon/search.png",          // Existe en tu proyecto (PNG)
            "/raven/icon/error.png",           // Existe en tu proyecto (PNG)
            "/raven/icon/icons/imagen.svg",    // Existe en tu proyecto (SVG)
            "/raven/icon/icons/usuario.svg",   // Existe en tu proyecto (SVG)
            "/raven/icon/icons/ojo.svg",       // Existe en tu proyecto (SVG)
            "/raven/icon/icons/agregar.svg"    // Existe en tu proyecto (SVG)
        };

            boolean iconLoaded = false;
            for (String path : possiblePaths) {
                try {
                    // Usa la variable path del bucle en lugar de una ruta fija
                    java.net.URL resourceUrl = getClass().getResource(path);
                    if (resourceUrl != null) {
                        ImageIcon productIcon = new ImageIcon(resourceUrl);
                        if (productIcon != null && productIcon.getIconWidth() > 0) {
                            pic.setIcon(productIcon);
                            System.out.println("Icono cargado exitosamente desde: " + path);
                            iconLoaded = true;
                            break;
                        }
                    }
                } catch (Exception ex) {
                    // Continue to next path
                    System.err.println("No se encontró el icono en: " + path);
                }
            }

            if (!iconLoaded) {
                System.err.println("No se pudo cargar ningún icono predeterminado");
            }
        } catch (Exception e) {
            // If no icon can be loaded, we'll just leave it blank
            System.out.println("Error general al cargar iconos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lbName = new javax.swing.JLabel();
        lbLocation = new javax.swing.JLabel();
        pic = new javax.swing.JLabel();

        lbName.setText("Name");

        lbLocation.setText("Location");

        pic.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(pic, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbName)
                    .addComponent(lbLocation))
                .addContainerGap(87, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbLocation)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(pic, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lbLocation;
    private javax.swing.JLabel lbName;
    private javax.swing.JLabel pic;
    // End of variables declaration//GEN-END:variables
}
