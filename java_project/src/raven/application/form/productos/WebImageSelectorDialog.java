package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import raven.utils.WebImageExtractor;
import raven.componentes.GraphicsUtilities;

/**
 * Dialog to extract and select images from a web URL.
 */
public class WebImageSelectorDialog extends JDialog {

    private JTextField txtUrl;
    private JButton btnAnalizar;
    private JPanel panelImages;
    private JButton btnDescargar;
    private JLabel lblStatus;
    private BufferedImage selectedImage;
    private String selectedImageUrl;
    
    private boolean confirmed = false;

    public WebImageSelectorDialog(java.awt.Frame parent) {
        super(parent, "Extractor de Imágenes Web", true);
        initComponents();
        setSize(900, 600);
        setLocationRelativeTo(parent);
    }
    
    public WebImageSelectorDialog(java.awt.Dialog parent) {
        super(parent, "Extractor de Imágenes Web", true);
        initComponents();
        setSize(900, 600);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel panelTop = new JPanel(new BorderLayout(5, 5));
        panelTop.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel lblUrl = new JLabel("URL del Producto:");
        lblUrl.setFont(lblUrl.getFont().deriveFont(Font.BOLD));
        
        txtUrl = new JTextField();
        txtUrl.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Pegue aquí el enlace (ej. Adidas, Nike, etc.)");
        
        btnAnalizar = new JButton("Analizar Página");
        btnAnalizar.putClientProperty(FlatClientProperties.STYLE, "background:#007AFF;foreground:#fff;font:bold");
        btnAnalizar.addActionListener(e -> analizarUrl());
        
        JPanel panelInput = new JPanel(new BorderLayout(5, 0));
        panelInput.add(txtUrl, BorderLayout.CENTER);
        panelInput.add(btnAnalizar, BorderLayout.EAST);
        
        panelTop.add(lblUrl, BorderLayout.NORTH);
        panelTop.add(panelInput, BorderLayout.CENTER);
        
        add(panelTop, BorderLayout.NORTH);
        
        // Center Panel (Images Grid)
        panelImages = new JPanel(new GridLayout(0, 4, 10, 10)); // 4 columns
        panelImages.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scroll = new JScrollPane(panelImages);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        
        // Bottom Panel
        JPanel panelBottom = new JPanel(new BorderLayout(5, 5));
        panelBottom.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        lblStatus = new JLabel("Listo para analizar.");
        lblStatus.setForeground(Color.GRAY);
        
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        
        btnDescargar = new JButton("Descargar Seleccionada");
        btnDescargar.setEnabled(false);
        btnDescargar.putClientProperty(FlatClientProperties.STYLE, "background:#28a745;foreground:#fff;font:bold");
        btnDescargar.addActionListener(e -> confirmarDescarga());
        
        panelButtons.add(btnCancelar);
        panelButtons.add(btnDescargar);
        
        panelBottom.add(lblStatus, BorderLayout.WEST);
        panelBottom.add(panelButtons, BorderLayout.EAST);
        
        add(panelBottom, BorderLayout.SOUTH);
    }
    
    private void analizarUrl() {
        String url = txtUrl.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese una URL válida.");
            return;
        }
        
        panelImages.removeAll();
        panelImages.revalidate();
        panelImages.repaint();
        lblStatus.setText("Analizando página y buscando imágenes...");
        btnAnalizar.setEnabled(false);
        
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return WebImageExtractor.extractImages(url);
            }

            @Override
            protected void done() {
                try {
                    List<String> images = get();
                    if (images.isEmpty()) {
                        lblStatus.setText("No se encontraron imágenes grandes.");
                        JOptionPane.showMessageDialog(WebImageSelectorDialog.this, "No se encontraron imágenes válidas en la página.");
                    } else {
                        lblStatus.setText("Encontradas " + images.size() + " imágenes. Cargando vistas previas...");
                        cargarVistasPrevias(images);
                    }
                } catch (Exception e) {
                    lblStatus.setText("Error: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(WebImageSelectorDialog.this, "Error al analizar la página: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnAnalizar.setEnabled(true);
                }
            }
        }.execute();
    }
    
    private void cargarVistasPrevias(List<String> imageUrls) {
        new SwingWorker<Void, JPanel>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (String imgUrl : imageUrls) {
                    try {
                        // Skip very small images (icons) or duplicates based on URL check if needed
                        // Load thumbnail
                        BufferedImage img = ImageIO.read(new URL(imgUrl));
                        if (img != null && img.getWidth() > 100 && img.getHeight() > 100) { // Filter small icons
                            JPanel thumbPanel = createThumbnailPanel(img, imgUrl);
                            publish(thumbPanel);
                        }
                    } catch (Exception e) {
                        // Ignore failed images
                    }
                }
                return null;
            }

            @Override
            protected void process(List<JPanel> chunks) {
                for (JPanel p : chunks) {
                    panelImages.add(p);
                }
                panelImages.revalidate();
                panelImages.repaint();
            }

            @Override
            protected void done() {
                lblStatus.setText("Proceso completado.");
            }
        }.execute();
    }
    
    private JPanel createThumbnailPanel(BufferedImage img, String url) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        p.setBackground(Color.WHITE);
        
        // Scale for thumbnail
        Image scaled = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        JLabel lblImg = new JLabel(new ImageIcon(scaled));
        lblImg.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblSize = new JLabel(img.getWidth() + "x" + img.getHeight());
        lblSize.setHorizontalAlignment(SwingConstants.CENTER);
        lblSize.setFont(lblSize.getFont().deriveFont(10f));
        
        p.add(lblImg, BorderLayout.CENTER);
        p.add(lblSize, BorderLayout.SOUTH);
        
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Deselect others
                for (Component c : panelImages.getComponents()) {
                    if (c instanceof JPanel) {
                        ((JPanel)c).setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                        c.setBackground(Color.WHITE);
                    }
                }
                // Select this
                p.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255), 3));
                p.setBackground(new Color(240, 248, 255));
                selectedImage = img;
                selectedImageUrl = url;
                btnDescargar.setEnabled(true);
            }
        });
        
        return p;
    }
    
    private void confirmarDescarga() {
        if (selectedImage != null) {
            confirmed = true;
            dispose();
        }
    }
    
    public BufferedImage getSelectedImage() {
        return confirmed ? selectedImage : null;
    }
    
    public String getSelectedImageUrl() {
        return confirmed ? selectedImageUrl : null;
    }

    public File getSelectedFile() {
        if (confirmed && selectedImage != null) {
            try {
                String ext = "png";
                if (selectedImageUrl != null) {
                    String lower = selectedImageUrl.toLowerCase();
                    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                        ext = "jpg";
                    }
                }
                File tempFile = File.createTempFile("web_img_", "." + ext);
                tempFile.deleteOnExit();
                ImageIO.write(selectedImage, ext, tempFile);
                return tempFile;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
