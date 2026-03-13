package raven.componentes;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PasswordFieldView extends JPasswordField {
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
    private int round = 8;
    private Color shadowColor = new Color(98, 70, 234);
    private BufferedImage imageShadow;
    private final Insets shadowSize = new Insets(2, 5, 8, 5);
    private JButton toggleButton; // Button to toggle visibility
    private boolean isVisible = false; // Visibility state

    public PasswordFieldView() {
        setUI(new TextUI());
        setOpaque(false);
        setSelectedTextColor(new Color(255, 255, 255));
        setSelectionColor(new Color(133, 209, 255));
        setBorder(new EmptyBorder(10, 12, 15, 12));
        
        // Create and configure the toggle button with an image
        toggleButton = new JButton(new ImageIcon("src/images/eye.png")); // Load your initial image here
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setOpaque(false);
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Set cursor to hand
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePasswordVisibility();
            }
        });
        
        // Add the button to the component
        add(toggleButton);
    }

    // Method to toggle password visibility
    private void togglePasswordVisibility() {
        isVisible = !isVisible;
        if (isVisible) {
            toggleButton.setIcon(new ImageIcon("src/images/eye_hide.png")); // Change to visible icon
            setEchoChar((char) 0); // Show password
        } else {
            toggleButton.setIcon(new ImageIcon("src/images/eye.png")); // Change to hidden icon
            setEchoChar('•'); // Hide password
        }
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
        // Create shadow image
        g2.drawImage(imageShadow, 0, 0, null);
        // Create background color
        g2.setColor(getBackground());
        Area area = new Area(new RoundRectangle2D.Double(x, y, width, height, round, round));
        g2.fill(area);
        g2.dispose();
        super.paintComponent(grphcs);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        createImageShadow();
        // Position the toggle button
        toggleButton.setBounds(width - 40, height / 2 - 10, 30, 20); // Adjust size and position as needed
        toggleButton.setVisible(true); // Ensure the button is visible
    }

    private void createImageShadow() {
        int height = getHeight();
        int width = getWidth();
        if (width > 0 && height > 0) {
            imageShadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = imageShadow.createGraphics();
            BufferedImage img = createShadow();
            if (img != null) {
                g2.drawImage(createShadow(), 0, 0, null);
            }
            g2.dispose ();
        }
    }

    private BufferedImage createShadow() {
        int width = getWidth() - (shadowSize.left + shadowSize.right);
        int height = getHeight() - (shadowSize.top + shadowSize.bottom);
        if (width > 0 && height > 0) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.fill(new RoundRectangle2D.Double(0, 0, width, height, round, round));
            g2.dispose();
            return new ShadowRenderer(5, 0.7f, shadowColor).createShadow(img);
        } else {
            return null;
        }
    }

    private class TextUI extends BasicPasswordFieldUI {
        @Override
        protected void paintBackground(Graphics grphcs) {
            // Do not paint background
        }
    }
}