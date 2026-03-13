package raven.componentes;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

/**
 * Panel completo con tabla y bordes redondeados
 * Basado en RoundedPanel - Totalmente compatible con NetBeans Swing GUI
 * 
 * Ejemplo de uso:
 * RoundedTablePanel tablaPanel = new RoundedTablePanel();
 * tablaPanel.setLayout(new java.awt.BorderLayout());
 * 
 * DefaultTableModel modelo = new DefaultTableModel();
 * modelo.addColumn("Producto");
 * modelo.addRow(new Object[]{"Laptop"});
 * 
 * JTable tabla = new JTable(modelo);
 * JScrollPane scroll = new JScrollPane(tabla);
 * tablaPanel.add(scroll);
 * 
 * @author Desarrollador
 * @version 1.0 - Compatible NetBeans
 */
public final class RoundedTablePanel extends JPanel {
    
    private int round = 20;
    private Color shadowColor = new Color(98, 70, 234);
    private BufferedImage imageShadow;
    private final Insets shadowSize = new Insets(2, 8, 5, 2);

    /**
     * Constructor por defecto
     */
    public RoundedTablePanel() {
        setOpaque(false);
        createImageShadow();
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
        createImageShadow();
        repaint();
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
        createImageShadow();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double width = getWidth() - (shadowSize.left + shadowSize.right);
        double height = getHeight() - (shadowSize.top + shadowSize.bottom);
        double x = shadowSize.left;
        double y = shadowSize.top;

        // Dibujar la sombra
        if (imageShadow != null) {
            g2.drawImage(imageShadow, 0, 0, null);
        }

        // Crear el fondo del panel
        g2.setColor(getBackground());
        Area area = new Area(new RoundRectangle2D.Double(x, y, width, height, round, round));
        g2.fill(area);

        // Limpiar los recursos
        g2.dispose();
        super.paintComponent(grphcs);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        createImageShadow();
    }

    private void createImageShadow() {
        int height = getHeight();
        int width = getWidth();
        if (width > 0 && height > 0) {
            imageShadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = imageShadow.createGraphics();
            BufferedImage img = createShadow();
            if (img != null) {
                g2.drawImage(img, 0, 0, null);
            }
            g2.dispose();
        }
    }

    private BufferedImage createShadow() {
        int width = getWidth() - (shadowSize.left + shadowSize.right);
        int height = getHeight() - (shadowSize.top + shadowSize.bottom);
        if (width > 0 && height > 0) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Dibujar las esquinas redondeadas para las sombras de forma correcta
            g2.fill(new RoundRectangle2D.Double(0, 0, width, height, round, round));
            g2.dispose();
            // Crear sombra
            return new ShadowRenderer(5, 0.5f, shadowColor).createShadow(img);
        } else {
            return null;
        }
    }
}