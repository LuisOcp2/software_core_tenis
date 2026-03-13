package raven.application.form.productos;

import raven.clases.productos.ImpresorTermicaPOSDIG2406T;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Diálogo para mostrar una vista previa en tiempo real de una etiqueta individual
 * con posibilidad de ajustar configuración básica antes de la impresión masiva
 */
public class VistaPreviaEtiquetaDialog extends JDialog {
    private final JTable tablaConEtiqueta;
    private final ImpresorTermicaPOSDIG2406T impresor;
    private JPanel panelVistaPrevia;
    private JLabel lblDimensiones;
    private JSpinner spAnchoMM;
    private JSpinner spAltoMM;
    private JComboBox<String> cbTipoEtiqueta;
    
    public VistaPreviaEtiquetaDialog(Frame owner, JTable tabla, ImpresorTermicaPOSDIG2406T impresor) {
        super(owner, "Vista Previa de Etiqueta Individual", true);
        this.tablaConEtiqueta = tabla;
        this.impresor = impresor;
        
        configurarUI();
        actualizarVistaPrevia();
    }
    
    private void configurarUI() {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(800, 600));
        
        // Panel de controles
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelControles.setBorder(BorderFactory.createTitledBorder("Configuración de Etiqueta"));
        
        panelControles.add(new JLabel("Ancho (mm):"));
        spAnchoMM = new JSpinner(new SpinnerNumberModel(40.0, 10.0, 200.0, 1.0));
        spAnchoMM.setPreferredSize(new Dimension(80, 25));
        panelControles.add(spAnchoMM);
        
        panelControles.add(new JLabel("Alto (mm):"));
        spAltoMM = new JSpinner(new SpinnerNumberModel(20.0, 10.0, 200.0, 1.0));
        spAltoMM.setPreferredSize(new Dimension(80, 25));
        panelControles.add(spAltoMM);
        
        panelControles.add(new JLabel("Tipo:"));
        cbTipoEtiqueta = new JComboBox<>(new String[]{"Par", "Caja"});
        panelControles.add(cbTipoEtiqueta);
        
        JButton btnActualizar = new JButton("Actualizar Vista");
        btnActualizar.addActionListener(e -> actualizarVistaPrevia());
        panelControles.add(btnActualizar);
        
        lblDimensiones = new JLabel("Dimensiones: 40.0 x 20.0 mm");
        panelControles.add(lblDimensiones);
        
        // Panel de vista previa
        panelVistaPrevia = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panelVistaPrevia.setBackground(Color.LIGHT_GRAY);
        panelVistaPrevia.setPreferredSize(new Dimension(300, 200));
        
        JScrollPane scrollVistaPrevia = new JScrollPane(panelVistaPrevia);
        scrollVistaPrevia.setBorder(BorderFactory.createTitledBorder("Vista Previa"));
        
        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnImprimir = new JButton("Imprimir Esta Etiqueta");
        btnImprimir.addActionListener(e -> imprimirEtiqueta());
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        
        panelBotones.add(btnImprimir);
        panelBotones.add(btnCerrar);
        
        // Eventos para actualización automática
        spAnchoMM.addChangeListener(e -> {
            double ancho = (Double) spAnchoMM.getValue();
            double alto = (Double) spAltoMM.getValue();
            lblDimensiones.setText(String.format("Dimensiones: %.1f x %.1f mm", ancho, alto));
            actualizarVistaPrevia();
        });
        
        spAltoMM.addChangeListener(e -> {
            double ancho = (Double) spAnchoMM.getValue();
            double alto = (Double) spAltoMM.getValue();
            lblDimensiones.setText(String.format("Dimensiones: %.1f x %.1f mm", ancho, alto));
            actualizarVistaPrevia();
        });
        
        add(panelControles, BorderLayout.NORTH);
        add(scrollVistaPrevia, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getParent());
    }
    
    private void actualizarVistaPrevia() {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                // Configurar el impresor con las dimensiones seleccionadas
                double ancho = (Double) spAnchoMM.getValue();
                double alto = (Double) spAltoMM.getValue();
                impresor.setCustomPaperSizeMM(ancho, alto);
                
                // Generar la imagen de vista previa
                java.util.List<BufferedImage> imagenes = impresor.generarPreviewImagenes();
                if (imagenes != null && !imagenes.isEmpty()) {
                    return imagenes.get(0); // Tomar la primera página
                }
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    if (img != null) {
                        // Actualizar el panel de vista previa
                        panelVistaPrevia.removeAll();
                        JLabel lblImagen = new JLabel(new ImageIcon(img));
                        lblImagen.setHorizontalAlignment(JLabel.CENTER);
                        lblImagen.setVerticalAlignment(JLabel.CENTER);
                        panelVistaPrevia.add(lblImagen);
                        panelVistaPrevia.revalidate();
                        panelVistaPrevia.repaint();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(VistaPreviaEtiquetaDialog.this, 
                        "Error al generar vista previa: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void imprimirEtiqueta() {
        // Configurar el impresor con las dimensiones seleccionadas
        double ancho = (Double) spAnchoMM.getValue();
        double alto = (Double) spAltoMM.getValue();
        impresor.setCustomPaperSizeMM(ancho, alto);
        
        // Abrir diálogo de impresión
        RotulacionPrintPreviewDialog dialog = new RotulacionPrintPreviewDialog(
            (Frame) getParent(), tablaConEtiqueta, impresor);
        dialog.setVisible(true);
        dispose();
    }
}