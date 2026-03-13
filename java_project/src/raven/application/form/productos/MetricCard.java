package raven.application.form.productos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Tarjeta de métrica para mostrar estadísticas en el dashboard.
 * Diseño moderno con valor grande y etiqueta descriptiva.
 * 
 * @author Kiro AI Assistant
 */
public class MetricCard extends JPanel {
    
    private JLabel valueLabel;
    private JLabel labelLabel;
    private Color accentColor;
    
    public MetricCard(String value, String label, Color accentColor) {
        this.accentColor = accentColor;
        initComponents(value, label);
        setupStyles();
    }
    
    private void initComponents(String value, String label) {
        setLayout(new BorderLayout());
        setBackground(new Color(61, 78, 95)); // #3d4e5f
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, accentColor),
                new EmptyBorder(12, 12, 12, 12)
        ));
        
        // Panel de contenido
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Valor
        valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Etiqueta
        labelLabel = new JLabel(label);
        labelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelLabel.setForeground(new Color(149, 165, 166)); // #95a5a6
        labelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel.add(valueLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(labelLabel);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private void setupStyles() {
        // Efecto hover
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setBackground(new Color(70, 90, 105));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setBackground(new Color(61, 78, 95));
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }
    
    // Métodos públicos
    
    public void setValue(String value) {
        valueLabel.setText(value);
    }
    
    public void setLabel(String label) {
        labelLabel.setText(label);
    }
    
    public String getValue() {
        return valueLabel.getText();
    }
    
    public String getLabel() {
        return labelLabel.getText();
    }
}
