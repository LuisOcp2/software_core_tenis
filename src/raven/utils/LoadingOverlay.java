/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.utils;

import java.awt.*;
import javax.swing.*;
/**
 *
 * @author CrisDEV
 * animacion cargando
 */

public class LoadingOverlay extends JComponent {

    private final Timer timer;
    private int angle = 0;
    private boolean running = false;
    private String mensaje = "Cargando...";

    public LoadingOverlay() {
        setOpaque(false);

        // Timer para animar el círculo
        timer = new Timer(30, e -> {  // 30 ms ~ 33 fps
            angle += 5;
            if (angle >= 360) {
                angle = 0;
            }
            repaint();
        });
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
        repaint();
    }

    /** Mostrar y empezar animación */
    public void start() {
        if (!running) {
            running = true;
            timer.start();
            setVisible(true);
        }
    }

    /** Ocultar y detener animación */
    public void stop() {
        running = false;
        timer.stop();
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!running) {
            return;  // si no está corriendo, no dibuja nada
        }

        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // Suavizado
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // 1) Fondo semitransparente (efecto borroso suave)
        g2.setColor(new Color(0, 0, 0, 100));  // negro con alfa
        g2.fillRect(0, 0, w, h);

        // 2) Panel central (como tarjetica)
        int panelWidth  = 220;
        int panelHeight = 180;
        int panelX = (w - panelWidth) / 2;
        int panelY = (h - panelHeight) / 2;

        g2.setColor(new Color(255, 255, 255, 230)); // blanco casi opaco
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 25, 25);

        // 3) Círculo animado tipo loading
        int circleSize = 80;
        int stroke = 10;
        int circleX = panelX + (panelWidth - circleSize) / 2;
        int circleY = panelY + 25;

        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Gradiente azul → transparente (similar a la imagen)
        GradientPaint gp = new GradientPaint(
                circleX, circleY,
                new Color(0, 191, 255),              // azul
                circleX + circleSize, circleY + circleSize,
                new Color(0, 191, 255, 0), true);    // azul transparente
        g2.setPaint(gp);

        // Dibujar arco (no el círculo completo para que parezca que gira)
        g2.drawArc(circleX, circleY, circleSize, circleSize, angle, 270);

        // 4) Texto "Cargando..."
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(mensaje);
        int textX = panelX + (panelWidth - textWidth) / 2;
        int textY = circleY + circleSize + 35;

        g2.setColor(new Color(30, 30, 30));
        g2.drawString(mensaje, textX, textY);

        g2.dispose();
    }

    // Helper estático para instalar el overlay como glassPane de un JFrame
    public static LoadingOverlay installOn(JFrame frame) {
        LoadingOverlay overlay = new LoadingOverlay();
        frame.setGlassPane(overlay);
        overlay.setVisible(false); // oculto inicialmente
        return overlay;
    }
}

