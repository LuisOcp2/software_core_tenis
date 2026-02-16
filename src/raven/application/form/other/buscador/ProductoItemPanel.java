/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.other.buscador;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JPanel;
import raven.application.form.other.buscador.dto.ProductoDTO;
import raven.clases.service.ProductoBusquedaService;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import raven.application.form.other.buscador.dto.VarianteDTO;

/**
 *
 * @author CrisDEV
 */
public class ProductoItemPanel extends javax.swing.JPanel {
    
    private final ProductoDTO producto;
    private final String bodega;
    private final ProductoBusquedaService busquedaService;
    
    // Componentes visuales
    private JPanel panelHeader;
    private JPanel panelVariantes;
    private JButton btnExpandir;
    
    // Estado
    private boolean expandido = false;
    private boolean variantesCargadas = false;
    private String tipoFiltro;
    
    // Listener
    private ProductoBuscadorPanel.VarianteSeleccionListener varianteListener;
    
    // Colores y estilos
    private static final Color COLOR_NORMAL = UIManager.getColor("Panel.background");
    private static final Color COLOR_HOVER = adjustColor(COLOR_NORMAL, 0.06f);
    private static final int ALTURA_HEADER = 80;

   /**
     * Constructor
     */
    public ProductoItemPanel(ProductoDTO producto, String bodega, 
                            ProductoBusquedaService busquedaService) {
        this.producto = producto;
        this.bodega = bodega;
        this.busquedaService = busquedaService;
        
        initComponents2();
        configurarEstilos();
        configurarEventos();
    }

    public void setTipoFiltro(String tipoFiltro) {
        this.tipoFiltro = tipoFiltro;
    }
    
    /**
     * Inicializa componentes
     */
    private void initComponents2() {
        setLayout(new BorderLayout());
        setBackground(COLOR_NORMAL);
        setBorder(new CompoundBorder(
            new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
            new EmptyBorder(0, 0, 0, 0)
        ));
        
        // Header (siempre visible)
        panelHeader = crearPanelHeader();
        add(panelHeader, BorderLayout.NORTH);
        
        // Panel de variantes (inicialmente oculto)
        panelVariantes = new JPanel();
        panelVariantes.setLayout(new BoxLayout(panelVariantes, BoxLayout.Y_AXIS));
        panelVariantes.setVisible(false);
        panelVariantes.setBackground(adjustColor(COLOR_NORMAL, 0.03f));
        panelVariantes.setBorder(new EmptyBorder(10, 15, 10, 15));
        add(panelVariantes, BorderLayout.CENTER);
        
        // Tamaño inicial
        setPreferredSize(new Dimension(0, ALTURA_HEADER));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTURA_HEADER));
    }
    
    /**
     * Crea el panel header con información básica del producto
     */
    private JPanel crearPanelHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(COLOR_NORMAL);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));
        panel.setPreferredSize(new Dimension(0, ALTURA_HEADER));
        
        // Panel izquierdo: Imagen + Info
        JPanel panelInfo = new JPanel(new BorderLayout(10, 5));
        panelInfo.setOpaque(false);
        
        // Placeholder para imagen (60x60)
        JLabel lblImagen = new JLabel();
        lblImagen.setPreferredSize(new Dimension(60, 60));
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagen.setVerticalAlignment(SwingConstants.CENTER);
        lblImagen.setBorder(new LineBorder(UIManager.getColor("Component.borderColor"), 1, true));
        lblImagen.setOpaque(true);
        lblImagen.setBackground(adjustColor(COLOR_NORMAL, 0.04f));
        cargarImagenProducto(lblImagen, producto.getIdProducto());
        panelInfo.add(lblImagen, BorderLayout.WEST);
        
        // Información del producto
        JPanel panelTexto = new JPanel();
        panelTexto.setLayout(new BoxLayout(panelTexto, BoxLayout.Y_AXIS));
        panelTexto.setOpaque(false);
        
        boolean dark = com.formdev.flatlaf.FlatLaf.isLafDark();
        JLabel lblNombre = new JLabel(producto.getNombre());
        lblNombre.setFont(lblNombre.getFont().deriveFont(Font.BOLD, 15f));
        lblNombre.setForeground(dark ? Color.WHITE : new Color(40,40,40));
        
        JLabel lblCodigo = new JLabel("Código: " + producto.getCodigoModelo());
        lblCodigo.setFont(lblCodigo.getFont().deriveFont(12f));
        lblCodigo.setForeground(dark ? new Color(220,225,230) : new Color(90,90,90));
        
        JLabel lblGenero = new JLabel("Género: " + producto.getGenero());
        lblGenero.setFont(lblGenero.getFont().deriveFont(12f));
        lblGenero.setForeground(dark ? new Color(220,225,230) : new Color(90,90,90));
        
        panelTexto.add(lblNombre);
        panelTexto.add(Box.createVerticalStrut(3));
        panelTexto.add(lblCodigo);
        panelTexto.add(lblGenero);
        
        panelInfo.add(panelTexto, BorderLayout.CENTER);
        panel.add(panelInfo, BorderLayout.CENTER);
        
        // Botón expandir/colapsar
        btnExpandir = new JButton("▼ Ver variantes");
        btnExpandir.setFocusPainted(false);
        btnExpandir.putClientProperty(FlatClientProperties.STYLE,
            "arc:16;background:@accentColor;foreground:#ffffff;borderWidth:0;focusWidth:1");
        panel.add(btnExpandir, BorderLayout.EAST);
        
        return panel;
    }

    private void cargarImagenProducto(JLabel destino, int idProducto) {
        try {
            String sql = "SELECT imagen FROM producto_variantes WHERE id_producto = ? AND disponible = 1 AND imagen IS NOT NULL ORDER BY id_variante LIMIT 1";
            try (java.sql.Connection conn = raven.controlador.principal.conexion.getInstance().getConnection();
                 java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, idProducto);
                try (java.sql.ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        byte[] bytes = rs.getBytes("imagen");
                        if (bytes != null && bytes.length > 0) {
                            Image img = new ImageIcon(bytes).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                            destino.setIcon(new ImageIcon(img));
                            destino.setText("");
                        } else {
                            destino.setText("IMG");
                        }
                    } else {
                        destino.setText("IMG");
                    }
                }
            }
        } catch (Exception ignore) {
            destino.setText("IMG");
        }
    }
    
    /**
     * Configura estilos FlatLaf
     */
    private void configurarEstilos() {
        // Estilo del botón expandir
        btnExpandir.putClientProperty(FlatClientProperties.BUTTON_TYPE, 
            FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        btnExpandir.putClientProperty(FlatClientProperties.COMPONENT_ROUND_RECT, true);
        
        // Border redondeado del panel
        putClientProperty(FlatClientProperties.STYLE, 
            "arc: 10; background:$Panel.background; border:1,1,1,1,$Component.borderColor,,10");
    }

    private static Color adjustColor(Color base, float ratio) {
        boolean dark = com.formdev.flatlaf.FlatLaf.isLafDark();
        Color mix = dark ? Color.WHITE : Color.BLACK;
        int r = (int) (base.getRed() * (1 - ratio) + mix.getRed() * ratio);
        int g = (int) (base.getGreen() * (1 - ratio) + mix.getGreen() * ratio);
        int b = (int) (base.getBlue() * (1 - ratio) + mix.getBlue() * ratio);
        return new Color(r, g, b, base.getAlpha());
    }
    
    /**
     * Configura eventos
     */
    private void configurarEventos() {
        // Hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!expandido) {
                    setBackground(COLOR_HOVER);
                    panelHeader.setBackground(COLOR_HOVER);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!expandido) {
                    setBackground(COLOR_NORMAL);
                    panelHeader.setBackground(COLOR_NORMAL);
                }
            }
        });
        
        // Click en header para expandir
        panelHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpansion();
            }
        });
        
        // Click en botón para expandir
        btnExpandir.addActionListener(e -> toggleExpansion());
    }
    
    /**
     * Alterna entre expandido y colapsado
     */
    private void toggleExpansion() {
        if (expandido) {
            colapsar();
        } else {
            expandir();
        }
    }
    
    /**
     * Expande el panel mostrando variantes
     */
    private void expandir() {
        expandido = true;
        btnExpandir.setText("▲ Ocultar variantes");
        panelVariantes.setVisible(true);
        
        // Cargar variantes si no están cargadas
        if (!variantesCargadas) {
            cargarVariantes();
        }
        
        // Animar expansión
        animarAltura(ALTURA_HEADER, calcularAlturaExpandida());
    }
    
    /**
     * Colapsa el panel ocultando variantes
     */
    private void colapsar() {
        expandido = false;
        btnExpandir.setText("▼ Ver variantes");
        
        // Animar colapso
        animarAltura(getPreferredSize().height, ALTURA_HEADER);
    }
    
    /**
     * Carga las variantes del producto
     */
    private void cargarVariantes() {
        // Mostrar indicador de carga
        JLabel lblCargando = new JLabel("Cargando variantes...");
        lblCargando.setHorizontalAlignment(SwingConstants.CENTER);
        lblCargando.setBorder(new EmptyBorder(20, 0, 20, 0));
        panelVariantes.add(lblCargando);
        panelVariantes.revalidate();
        
        // Cargar de forma asíncrona
        busquedaService.cargarVariantesAsync(producto, bodega, tipoFiltro, variantes -> {
            panelVariantes.removeAll();
            mostrarVariantes(variantes);
            variantesCargadas = true;
            
            // Re-calcular altura
            if (expandido) {
                animarAltura(getPreferredSize().height, calcularAlturaExpandida());
            }
        });
    }
    
    /**
     * Muestra las variantes en el panel
     */
    private void mostrarVariantes(List<VarianteDTO> variantes) {
        if (variantes.isEmpty()) {
            JLabel lblSinVariantes = new JLabel("No hay variantes disponibles");
            lblSinVariantes.setHorizontalAlignment(SwingConstants.CENTER);
            lblSinVariantes.setBorder(new EmptyBorder(20, 0, 20, 0));
            panelVariantes.add(lblSinVariantes);
        } else {
            for (VarianteDTO variante : variantes) {
                if (tipoFiltro != null) {
                    if ("par".equalsIgnoreCase(tipoFiltro) && (variante.getStockPares() == null || variante.getStockPares() <= 0)) {
                        continue;
                    }
                    if ("caja".equalsIgnoreCase(tipoFiltro) && (variante.getStockCaja() == null || variante.getStockCaja() <= 0)) {
                        continue;
                    }
                }
                VarianteItemPanel variantePanel = new VarianteItemPanel(variante);
                
                // Listener de selección
                variantePanel.setSeleccionListener(() -> {
                    if (varianteListener != null) {
                        varianteListener.onVarianteSeleccionada(variante);
                    }
                });
                
                panelVariantes.add(variantePanel);
                panelVariantes.add(Box.createVerticalStrut(5));
            }
        }
        
        panelVariantes.revalidate();
        panelVariantes.repaint();
    }
    
    /**
     * Calcula la altura cuando está expandido
     */
    private int calcularAlturaExpandida() {
        int alturaVariantes = 0;
        if (panelVariantes.getComponentCount() > 0) {
            for (Component comp : panelVariantes.getComponents()) {
                alturaVariantes += comp.getPreferredSize().height;
            }
        } else {
            alturaVariantes = 100; // Altura estimada
        }
        
        return ALTURA_HEADER + alturaVariantes + 20; // +20 para padding
    }
    
    /**
     * Anima el cambio de altura del panel
     */
    private void animarAltura(int alturaInicial, int alturaFinal) {
        Timer timer = new Timer(10, null);
        final int pasos = 15;
        final int diferencia = alturaFinal - alturaInicial;
        final int[] paso = {0};
        
        timer.addActionListener(e -> {
            paso[0]++;
            
            if (paso[0] <= pasos) {
                // Interpolación suave (ease-out)
                double progreso = (double) paso[0] / pasos;
                double factor = 1 - Math.pow(1 - progreso, 3); // Cubic ease-out
                
                int nuevaAltura = alturaInicial + (int) (diferencia * factor);
                
                setPreferredSize(new Dimension(0, nuevaAltura));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, nuevaAltura));
                revalidate();
            } else {
                timer.stop();
                
                // Altura final
                setPreferredSize(new Dimension(0, alturaFinal));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, alturaFinal));
                
                // Ocultar panel de variantes si se colapsó
                if (!expandido) {
                    panelVariantes.setVisible(false);
                }
                
                revalidate();
                repaint();
            }
        });
        
        timer.start();
    }
    
    /**
     * Establece el listener de selección de variante
     */
    public void setVarianteSeleccionListener(ProductoBuscadorPanel.VarianteSeleccionListener listener) {
        this.varianteListener = listener;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
