package raven.componentes;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JPanel;

public final class panelArc extends JPanel {
    
    private int round = 25; // Radio de redondeo para las esquinas inferiores
    
    public panelArc() {
        setOpaque(false); // Para control total sobre la pintura del componente
    }
    
    public int getRound() {
        return round;
    }
    
    public void setRound(int round) {
        this.round = round;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Crear forma con solo las esquinas inferiores redondeadas
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0); // Esquina superior izquierda (cuadrada)
        path.lineTo(getWidth(), 0); // Esquina superior derecha (cuadrada)
        path.lineTo(getWidth(), getHeight() - round); // Borde derecho hasta inicio de curva
        path.quadTo(getWidth(), getHeight(), getWidth() - round, getHeight()); // Esquina inferior derecha (redondeada)
        path.lineTo(round, getHeight()); // Borde inferior
        path.quadTo(0, getHeight(), 0, getHeight() - round); // Esquina inferior izquierda (redondeada)
        path.closePath();
        
        // Dibujar el fondo del panel
        g2.setColor(getBackground());
        g2.fill(path);
        
        g2.dispose();
        
        // Llamamos a paintComponent de la superclase después de dibujar nuestro fondo personalizado
        // para asegurar que los componentes hijos se pinten correctamente
        super.paintComponent(grphcs);
    }
}