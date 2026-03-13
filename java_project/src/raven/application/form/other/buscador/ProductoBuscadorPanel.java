/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.other.buscador;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import raven.application.form.other.buscador.dto.BusquedaCriteria;
import raven.application.form.other.buscador.dto.ProductoDTO;
import raven.application.form.other.buscador.dto.VarianteDTO;
import raven.clases.service.ProductoBusquedaService;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import raven.clases.productos.TraspasoService;
import raven.clases.productos.TraspasoConfig;

/**
 *
 * @author CrisDEV
 */
public class ProductoBuscadorPanel extends javax.swing.JPanel {
    
    private final ProductoBusquedaService busquedaService;

    // Timer para debounce en búsqueda
    private Timer busquedaTimer;
    
    // Listener para selección de variante
    private VarianteSeleccionListener seleccionListener;
    private Integer idBodegaOrigen;
    private int pageLimit = 10;
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private java.util.List<ProductoDTO> productosActuales = new java.util.ArrayList<>();
    private javax.swing.JProgressBar loader;
    private int renderedCount = 0;
    private int prefetchThreshold = 10;
    private int bufferTarget = 30;
    private javax.swing.SwingWorker<List<ProductoDTO>, Void> currentWorker;
    private javax.swing.SwingWorker<List<ProductoDTO>, Void> currentLoadWorker;
    private Map<String,Integer> bodegasMap = new LinkedHashMap<>();
    private Integer idBodegaDestino;
    
    
    /**
     * Constructor
     * @param busquedaService Servicio de búsqueda
     */
    public ProductoBuscadorPanel(ProductoBusquedaService busquedaService) {
        this.busquedaService = busquedaService;
        initComponents();
        setLayout(new java.awt.BorderLayout());
        removeAll();

        // ===== Reestructurar layout para ocupar todo el ancho/alto =====
        // Header (título + filtros)
        javax.swing.JPanel header = new javax.swing.JPanel();
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(jLabel1);
        header.add(javax.swing.Box.createVerticalStrut(8));

        // Reemplazar contenido de jPanel2 por filtros con GridBag (responsivo)
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        jPanel2.removeAll();
        jPanel2.add(crearPanelFiltros(), java.awt.BorderLayout.CENTER);

        header.add(jPanel2);

        // jPanel1 como contenedor principal con BorderLayout
        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.removeAll();
        jPanel1.add(header, java.awt.BorderLayout.NORTH);
        jPanel1.add(panelResultados, java.awt.BorderLayout.CENTER);

        add(jPanel1, java.awt.BorderLayout.CENTER);

        panelResultados.setLayout(new java.awt.BorderLayout());
        panelResultados.removeAll();
        panelResultados.add(scrollResultados, java.awt.BorderLayout.CENTER);
        configurarEstilos();
        configurarEventos();
        inicializarCombos();
        inicializarContenedorResultados();
        realizarBusqueda();
    }

    public ProductoBuscadorPanel(ProductoBusquedaService busquedaService, Integer idBodegaOrigen) {
        this(busquedaService);
        this.idBodegaOrigen = idBodegaOrigen;
    }

    public void setIdBodegaDestino(Integer id) {
        this.idBodegaDestino = id;
        seleccionarBodegaDestino();
        realizarBusqueda();
        try {
            TraspasoConfig cfg = TraspasoConfig.load();
            cfg.setIdDestino(id);
            TraspasoConfig.save(cfg);
        } catch (Exception ignore) {}
    }

    public void setTipoSeleccion(String tipo) {
        if (tipo == null) return;
        String val = ("par".equalsIgnoreCase(tipo) ? "Par" : "Caja");
        if (cmbTipo.getModel() == null || cmbTipo.getItemCount() == 0) {
            configurarCmbTipo();
        }
        cmbTipo.setSelectedItem(val);
    }

    /**
     * Crea el panel de filtros superior
     */
    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Campo de búsqueda
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 4;
        
        txtBusqueda = new JTextField();
        txtBusqueda.setPreferredSize(new Dimension(400, 35));
        panel.add(txtBusqueda, gbc);
        
        // Combo género
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        gbc.gridwidth = 1;
        
        JLabel lblGenero = new JLabel("Género:");
        panel.add(lblGenero, gbc);
        
        gbc.gridx = 1;
        cmbGenero = new JComboBox<>(new String[]{"TODOS", "HOMBRE", "MUJER", "NIÑO", "UNISEX"});
        panel.add(cmbGenero, gbc);
        
        // Combo bodega
        gbc.gridx = 2;
        JLabel lblBodega = new JLabel("Bodega:");
        panel.add(lblBodega, gbc);
        
        gbc.gridx = 3;
        cmbBodega = new JComboBox<>(new String[]{"GENERAL", "bodega", "tienda"});
        panel.add(cmbBodega, gbc);

        // Combo tipo
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        JLabel lblTipo = new JLabel("Tipo:");
        panel.add(lblTipo, gbc);
        gbc.gridx = 1;
        cmbTipo = new JComboBox<>(new String[]{"TODOS", "Par", "Caja"});
        panel.add(cmbTipo, gbc);

        // Checkbox solo con stock
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        
        chkSoloStock = new JCheckBox("Solo productos con stock");
        chkSoloStock.setSelected(true);
        panel.add(chkSoloStock, gbc);
        
        return panel;
    }
    
    /**
     * Configura estilos FlatLaf para un look moderno
     */
    private void configurarEstilos() {
        boolean dark = com.formdev.flatlaf.FlatLaf.isLafDark();
        txtBusqueda.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
            "Buscar por nombre o código de modelo...");
        txtBusqueda.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
            new FlatSVGIcon("icons/search.svg", 16, 16));
        txtBusqueda.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

        txtBusqueda.putClientProperty(FlatClientProperties.STYLE,
            "arc:20;borderWidth:1;focusWidth:1;innerFocusWidth:0;" +
            "background:lighten($Panel.background,6%);" +
            (dark ? "foreground:#FFFFFF" : "foreground:$Text.foreground"));

        cmbGenero.putClientProperty(FlatClientProperties.STYLE,
            "arc:18;background:lighten($Panel.background,4%);" +
            (dark ? "foreground:#FFFFFF" : "foreground:$Text.foreground"));
        cmbBodega.putClientProperty(FlatClientProperties.STYLE,
            "arc:18;background:lighten($Panel.background,4%);" +
            (dark ? "foreground:#FFFFFF" : "foreground:$Text.foreground"));
        cmbTipo.putClientProperty(FlatClientProperties.STYLE,
            "arc:18;background:lighten($Panel.background,4%);" +
            (dark ? "foreground:#FFFFFF" : "foreground:$Text.foreground"));
        chkSoloStock.putClientProperty(FlatClientProperties.STYLE,
            (dark ? "foreground:#FFFFFF" : "foreground:$Text.foreground"));

        jPanel1.putClientProperty(FlatClientProperties.STYLE,
            "background:$Panel.background");
        jPanel2.putClientProperty(FlatClientProperties.STYLE,
            "arc:20;background:lighten($Panel.background,2%);border:1,1,1,1,$Component.borderColor,,12");
        panelResultados.putClientProperty(FlatClientProperties.STYLE,
            "background:$Panel.background");
        scrollResultados.putClientProperty(FlatClientProperties.STYLE,
            "border:1,1,1,1,$Component.borderColor,,10;background:lighten($Panel.background,1%)");

        // Etiquetas en blanco puro en tema oscuro
        if (dark) {
            jLabel1.setForeground(Color.WHITE);
            Lbgenero.setForeground(Color.WHITE);
            lbBodega.setForeground(Color.WHITE);
            lbstock.setForeground(Color.WHITE);
        }

        // Mensajes del sistema (JOptionPane) en blanco en tema oscuro
        if (dark) {
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            UIManager.put("Label.foreground", Color.WHITE);
        }
    }
    
    /**
     * Configura eventos de los componentes
     */
    private void configurarEventos() {
        // Búsqueda con debounce (espera 400ms después de que el usuario deja de escribir)
        busquedaTimer = new Timer(400, e -> realizarBusqueda());
        busquedaTimer.setRepeats(false);
        
        txtBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                reiniciarTimer();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                reiniciarTimer();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                reiniciarTimer();
            }
            
            private void reiniciarTimer() {
                busquedaTimer.stop();
                busquedaTimer.start();
            }
        });
        
        // Búsqueda al cambiar filtros
        ActionListener filtroListener = e -> realizarBusqueda();
        cmbGenero.addActionListener(filtroListener);
        cmbBodega.addActionListener(filtroListener);
        cmbTipo.addActionListener(filtroListener);
        chkSoloStock.addActionListener(filtroListener);
    }

    private void inicializarCombos() {
        cmbGenero.setModel(new javax.swing.DefaultComboBoxModel<>(
            new String[]{"TODOS", "HOMBRE", "MUJER", "NIÑO", "UNISEX"}
        ));
        cmbGenero.setSelectedItem("TODOS");

        cargarBodegas();
        seleccionarBodegaDestino();
        bloquearCambioBodega();
        configurarCmbTipo();

        chkSoloStock.setSelected(true);
        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(
            new String[]{"TODOS", "Par", "Caja"}
        ));
        cmbTipo.setSelectedItem("TODOS");
    }

    private void inicializarContenedorResultados() {
        javax.swing.JPanel contenedor = new javax.swing.JPanel();
        contenedor.setLayout(new javax.swing.BoxLayout(contenedor, javax.swing.BoxLayout.Y_AXIS));
        contenedor.setOpaque(true);
        contenedor.setBackground(UIManager.getColor("Panel.background"));
        scrollResultados.setViewportView(contenedor);
        scrollResultados.setBorder(BorderFactory.createEmptyBorder());
        if (scrollResultados.getViewport() != null) {
            scrollResultados.getViewport().setOpaque(false);
        }
        panelResultados.putClientProperty("contenedor", contenedor);

        loader = new javax.swing.JProgressBar();
        loader.setIndeterminate(true);
        loader.putClientProperty(FlatClientProperties.STYLE,
            "arc:12; background:lighten($Panel.background,4%);foreground:@accentColor");
        loader.setVisible(false);
        contenedor.add(loader);

        javax.swing.JScrollBar vbar = scrollResultados.getVerticalScrollBar();
        vbar.addAdjustmentListener(e -> {
            if (isLoading) return;
            int value = vbar.getValue();
            int extent = vbar.getModel().getExtent();
            int max = vbar.getMaximum();
            if (value + extent >= max - 80) {
                if (renderNextFromBuffer(pageLimit) == 0 && hasMore) {
                    cargarSiguienteLote();
                }
                prefetchIfNeeded();
            }
        });
    }

    /**
     * Realiza la búsqueda con los criterios actuales
     */
    private void realizarBusqueda() {
        if (currentWorker != null) {
            currentWorker.cancel(true);
            currentWorker = null;
        }
        if (currentLoadWorker != null) {
            currentLoadWorker.cancel(true);
            currentLoadWorker = null;
        }
        if (loader != null) loader.setVisible(true);
        isLoading = true;
        // Construir criterios usando Builder pattern
        BusquedaCriteria.Builder builder = new BusquedaCriteria.Builder();
        
        String texto = txtBusqueda.getText().trim();
        boolean exactMode = false;
        if (!texto.isEmpty()) {
            // Modo exacto si el usuario usa comillas
            if ((texto.startsWith("\"") && texto.endsWith("\"")) || (texto.startsWith("'") && texto.endsWith("'"))) {
                texto = texto.substring(1, texto.length()-1);
                exactMode = true;
            }
            builder.conTextoBusqueda(texto.toLowerCase());
            String soloDigitos = texto.replaceAll("[^0-9]", "");
            if (!soloDigitos.isEmpty()) {
                try {
                    if (soloDigitos.length() <= 9) {
                        builder.conIdProducto(Integer.parseInt(soloDigitos));
                    }
                } catch (Exception ignore) {}
                if (soloDigitos.length() >= 8) {
                    builder.conEAN(soloDigitos);
                }
            }
        }
        String tipoSelTmp = (String) cmbTipo.getSelectedItem();
        boolean filtrosActivos = (cmbGenero.getSelectedItem() != null && !"TODOS".equals(cmbGenero.getSelectedItem()))
                || (cmbBodega.getSelectedItem() != null && !"GENERAL".equals(cmbBodega.getSelectedItem()))
                || (idBodegaOrigen != null && idBodegaOrigen > 0)
                || chkSoloStock.isSelected()
                || (tipoSelTmp != null && !"TODOS".equalsIgnoreCase(tipoSelTmp));
        if (texto.isEmpty() && !filtrosActivos) {
            javax.swing.JPanel contenedor = (javax.swing.JPanel) panelResultados.getClientProperty("contenedor");
            if (contenedor == null) {
                inicializarContenedorResultados();
                contenedor = (javax.swing.JPanel) panelResultados.getClientProperty("contenedor");
            }
            contenedor.removeAll();
            JLabel hint = new JLabel("Escribe para buscar o aplica un filtro");
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            hint.setBorder(new EmptyBorder(24, 16, 24, 16));
            contenedor.add(hint);
            contenedor.revalidate();
            contenedor.repaint();
            isLoading = false;
            if (loader != null) loader.setVisible(false);
            return;
        }
        
        String genero = (String) cmbGenero.getSelectedItem();
        if (genero != null && !"TODOS".equals(genero)) {
            builder.conGenero(genero);
        }
        
        String bodega = (String) cmbBodega.getSelectedItem();
        Integer idSel = bodegasMap.get(bodega);
        if (idBodegaOrigen != null && idBodegaOrigen > 0) {
            builder.enBodegaId(idBodegaOrigen);
        } else if (idSel != null && idSel > 0) {
            builder.enBodegaId(idSel);
        } else if (bodega != null && !"GENERAL".equals(bodega)) {
            builder.enBodega(bodega);
        }
        
        if (chkSoloStock.isSelected() || (idBodegaOrigen != null && idBodegaOrigen > 0)) {
            builder.soloConStock(true);
        }
        builder.soloConVariantes(true);

        String tipoSel = (String) cmbTipo.getSelectedItem();
        if (tipoSel != null && !"TODOS".equalsIgnoreCase(tipoSel)) {
            builder.conTipo(tipoSel.equalsIgnoreCase("Par") ? "par" : "caja");
        }
        
        currentOffset = 0;
        hasMore = true;
        BusquedaCriteria criteria = builder
            .conLimite(pageLimit)
            .conOffset(currentOffset)
            .conCoincidenciaExacta(exactMode)
            .conBuscarPorNombre(true)
            .conBuscarPorMarca(true)
            .build();
        
        // Realizar búsqueda en background
        SwingWorker<List<ProductoDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ProductoDTO> doInBackground() {
                return busquedaService.buscarProductos(criteria);
            }
            
            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    List<ProductoDTO> productos = get();
                    String sel = (String) cmbBodega.getSelectedItem();
                    Integer idSelMostrar = bodegasMap.get(sel);
                    String bKey = (idBodegaOrigen != null ? String.valueOf(idBodegaOrigen) : (idSelMostrar != null ? String.valueOf(idSelMostrar) : sel));
                    String tipoSel = (String) cmbTipo.getSelectedItem();
                    String tipoFiltro = (tipoSel != null && !"TODOS".equalsIgnoreCase(tipoSel)) ? (tipoSel.equalsIgnoreCase("Par") ? "par" : "caja") : null;
                    try {
                        busquedaService.cargarVariantesBatch(productos, bKey, tipoFiltro);
                        productos = productos.stream()
                                .filter(p -> p.getVariantes() != null && !p.getVariantes().isEmpty())
                                .toList();
                    } catch (Exception ignore) {}
                    productosActuales.clear();
                    productosActuales.addAll(productos);
                    renderedCount = 0;
                    mostrarResultados(productos, bKey, false);
                    renderedCount = productos.size();
                    if (productos.size() < pageLimit) hasMore = false;
                    prefetchIfNeeded();
                } catch (Exception e) {
                    mostrarError("Error al buscar productos: " + e.getMessage());
                } finally {
                    isLoading = false;
                    if (loader != null) loader.setVisible(false);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * Muestra los resultados de la búsqueda
     */
    private void mostrarResultados(List<ProductoDTO> productos, String bodega, boolean append) {
        javax.swing.JPanel contenedor = (javax.swing.JPanel) panelResultados.getClientProperty("contenedor");
        if (contenedor == null) {
            inicializarContenedorResultados();
            contenedor = (javax.swing.JPanel) panelResultados.getClientProperty("contenedor");
        }
        if (!append) {
            contenedor.removeAll();
        }

        if (productos.isEmpty() && !append) {
            JLabel lblSinResultados = new JLabel("No se encontraron productos");
            lblSinResultados.setHorizontalAlignment(SwingConstants.CENTER);
            lblSinResultados.setBorder(new EmptyBorder(50, 20, 50, 20));
            contenedor.add(lblSinResultados);
        } else {
            if (!append) {
                JLabel lblResumen = new JLabel("Resultados: " + productos.size());
                lblResumen.setBorder(new EmptyBorder(8, 12, 8, 12));
                contenedor.add(lblResumen);
                contenedor.add(Box.createVerticalStrut(8));
            }
            String tipoSel = (String) cmbTipo.getSelectedItem();
            String tipoFiltro = (tipoSel != null && !"TODOS".equalsIgnoreCase(tipoSel)) ? (tipoSel.equalsIgnoreCase("Par") ? "par" : "caja") : null;
            for (ProductoDTO producto : productos) {
                ProductoItemPanel itemPanel = new ProductoItemPanel(
                    producto,
                    bodega,
                    busquedaService
                );
                itemPanel.setTipoFiltro(tipoFiltro);
                itemPanel.setVarianteSeleccionListener(variante -> {
                    if (seleccionListener != null) {
                        seleccionListener.onVarianteSeleccionada(variante);
                    }
                });

                agregarConAnimacion(contenedor, itemPanel);
                contenedor.add(Box.createVerticalStrut(10));
            }
        }

        if (loader != null && !java.util.Arrays.asList(contenedor.getComponents()).contains(loader)) {
            contenedor.add(loader);
        }
        contenedor.revalidate();
        contenedor.repaint();
    }

    private void cargarSiguienteLote() {
        if (isLoading) return;
        isLoading = true;
        if (loader != null) loader.setVisible(true);

        BusquedaCriteria.Builder builder = new BusquedaCriteria.Builder();
        String texto = txtBusqueda.getText().trim();
        boolean exactMode = false;
        if (!texto.isEmpty()) {
            if ((texto.startsWith("\"") && texto.endsWith("\"")) || (texto.startsWith("'") && texto.endsWith("'"))) {
                texto = texto.substring(1, texto.length()-1);
                exactMode = true;
            }
            builder.conTextoBusqueda(texto.toLowerCase());
            String soloDigitos = texto.replaceAll("[^0-9]", "");
            if (!soloDigitos.isEmpty()) {
                try {
                    if (soloDigitos.length() <= 9) {
                        builder.conIdProducto(Integer.parseInt(soloDigitos));
                    }
                } catch (Exception ignore) {}
                if (soloDigitos.length() >= 8) {
                    builder.conEAN(soloDigitos);
                }
            }
        }
        String genero = (String) cmbGenero.getSelectedItem();
        if (genero != null && !"TODOS".equals(genero)) builder.conGenero(genero);
        String bodega = (String) cmbBodega.getSelectedItem();
        Integer idSel = bodegasMap.get(bodega);
        if (idBodegaOrigen != null && idBodegaOrigen > 0) builder.enBodegaId(idBodegaOrigen);
        else if (idSel != null && idSel > 0) builder.enBodegaId(idSel);
        else if (bodega != null && !"GENERAL".equals(bodega)) builder.enBodega(bodega);
        if (chkSoloStock.isSelected() || (idBodegaOrigen != null && idBodegaOrigen > 0)) builder.soloConStock(true);
        builder.soloConVariantes(true);
        String tipoSel = (String) cmbTipo.getSelectedItem();
        if (tipoSel != null && !"TODOS".equalsIgnoreCase(tipoSel)) builder.conTipo(tipoSel.equalsIgnoreCase("Par") ? "par" : "caja");

        currentOffset += pageLimit;
        BusquedaCriteria criteria = builder
            .conLimite(pageLimit)
            .conOffset(currentOffset)
            .conCoincidenciaExacta(exactMode)
            .conBuscarPorNombre(true)
            .conBuscarPorMarca(true)
            .build();

        SwingWorker<List<ProductoDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ProductoDTO> doInBackground() {
                return busquedaService.buscarProductos(criteria);
            }
            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    List<ProductoDTO> nuevos = get();
                    String sel = (String) cmbBodega.getSelectedItem();
                    Integer idSelMostrar = bodegasMap.get(sel);
                    String bKey = (idBodegaOrigen != null ? String.valueOf(idBodegaOrigen) : (idSelMostrar != null ? String.valueOf(idSelMostrar) : sel));
                    String tipoSel = (String) cmbTipo.getSelectedItem();
                    String tipoFiltro = (tipoSel != null && !"TODOS".equalsIgnoreCase(tipoSel)) ? (tipoSel.equalsIgnoreCase("Par") ? "par" : "caja") : null;
                    try {
                        busquedaService.cargarVariantesBatch(nuevos, bKey, tipoFiltro);
                        nuevos = nuevos.stream()
                                .filter(p -> p.getVariantes() != null && !p.getVariantes().isEmpty())
                                .toList();
                    } catch (Exception ignore) {}
                    productosActuales.addAll(nuevos);
                    // No renderizamos aquí; lo hará renderNextFromBuffer
                    if (nuevos.size() < pageLimit) hasMore = false;
                } catch (Exception e) {
                    mostrarError("Error al cargar más productos: " + e.getMessage());
                    currentOffset -= pageLimit;
                } finally {
                    isLoading = false;
                    if (loader != null) loader.setVisible(false);
                    // Intentar renderizar inmediatamente si hay espacio
                    renderNextFromBuffer(pageLimit);
                    prefetchIfNeeded();
                }
            }
        };
        currentLoadWorker = worker;
        worker.execute();
    }

    private int renderNextFromBuffer(int count) {
        if (renderedCount >= productosActuales.size()) return 0;
        int hasta = Math.min(renderedCount + count, productosActuales.size());
        java.util.List<ProductoDTO> slice = productosActuales.subList(renderedCount, hasta);
        String sel = (String) cmbBodega.getSelectedItem();
        Integer idSelR = bodegasMap.get(sel);
        String bKey = (idBodegaOrigen != null ? String.valueOf(idBodegaOrigen) : (idSelR != null ? String.valueOf(idSelR) : sel));
        mostrarResultados(slice, bKey, true);
        int agregados = slice.size();
        renderedCount += agregados;
        return agregados;
    }

    private void prefetchIfNeeded() {
        int buffer = productosActuales.size() - renderedCount;
        if (!hasMore || isLoading) return;
        if (buffer >= (bufferTarget - pageLimit)) return;
        // Prefetch siguiente página sin bloquear
        BusquedaCriteria.Builder builder = new BusquedaCriteria.Builder();
        String texto = txtBusqueda.getText().trim();
        boolean exactMode = false;
        if (!texto.isEmpty()) {
            if ((texto.startsWith("\"") && texto.endsWith("\"")) || (texto.startsWith("'") && texto.endsWith("'"))) {
                texto = texto.substring(1, texto.length()-1);
                exactMode = true;
            }
            builder.conTextoBusqueda(texto.toLowerCase());
            String soloDigitos = texto.replaceAll("[^0-9]", "");
            if (!soloDigitos.isEmpty()) {
                try {
                    if (soloDigitos.length() <= 9) {
                        builder.conIdProducto(Integer.parseInt(soloDigitos));
                    }
                } catch (Exception ignore) {}
                if (soloDigitos.length() >= 8) {
                    builder.conEAN(soloDigitos);
                }
            }
        }
        String genero = (String) cmbGenero.getSelectedItem();
        if (genero != null && !"TODOS".equals(genero)) builder.conGenero(genero);
        String bodega = (String) cmbBodega.getSelectedItem();
        if (idBodegaOrigen != null && idBodegaOrigen > 0) builder.enBodegaId(idBodegaOrigen);
        else {
            Integer idSel2 = bodegasMap.get(bodega);
            if (idSel2 != null && idSel2 > 0) builder.enBodegaId(idSel2);
            else if (bodega != null && !"GENERAL".equals(bodega)) builder.enBodega(bodega);
        }
        if (chkSoloStock.isSelected() || (idBodegaOrigen != null && idBodegaOrigen > 0)) builder.soloConStock(true);
        builder.soloConVariantes(true);
        String tipoSel = (String) cmbTipo.getSelectedItem();
        if (tipoSel != null && !"TODOS".equalsIgnoreCase(tipoSel)) builder.conTipo(tipoSel.equalsIgnoreCase("Par") ? "par" : "caja");

        int nextOffset = currentOffset + pageLimit;
        BusquedaCriteria criteria = builder
            .conLimite(pageLimit)
            .conOffset(nextOffset)
            .conCoincidenciaExacta(exactMode)
            .conBuscarPorNombre(true)
            .conBuscarPorMarca(true)
            .build();
        busquedaService.prefetchProductos(criteria, nuevos -> {
            // Agregar al buffer si coincide con el offset esperado
            if (nuevos != null && !nuevos.isEmpty()) {
                // Ajustar currentOffset solo cuando se confirme carga principal
                productosActuales.addAll(nuevos);
                // no renderizamos aquí
            } else {
                hasMore = false;
            }
        });
    }

    private void agregarConAnimacion(javax.swing.JPanel contenedor, JComponent itemPanel) {
        int targetHeight = Math.max(80, itemPanel.getPreferredSize().height);
        itemPanel.setPreferredSize(new Dimension(itemPanel.getPreferredSize().width, 0));
        contenedor.add(itemPanel);
        Timer timer = new Timer(12, null);
        final int pasos = 12;
        final int[] paso = {0};
        timer.addActionListener(e -> {
            paso[0]++;
            double prog = Math.min(1.0, paso[0] / (double) pasos);
            int h = (int) (targetHeight * prog);
            itemPanel.setPreferredSize(new Dimension(itemPanel.getPreferredSize().width, h));
            itemPanel.revalidate();
            if (prog >= 1.0) ((Timer) e.getSource()).stop();
        });
        timer.start();
    }
    
    /**
     * Muestra un mensaje de error
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
            this, 
            mensaje, 
            "Error", 
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Establece el listener para cuando se selecciona una variante
     */
    public void setVarianteSeleccionListener(VarianteSeleccionListener listener) {
        this.seleccionListener = listener;
    }

    public void setIdBodegaOrigen(Integer idBodegaOrigen) {
        this.idBodegaOrigen = idBodegaOrigen;
        seleccionarBodegaOrigen();
        if (cmbBodega != null) cmbBodega.setEnabled(false);
        try {
            TraspasoConfig cfg = TraspasoConfig.load();
            cfg.setIdOrigen(idBodegaOrigen);
            TraspasoConfig.save(cfg);
        } catch (Exception ignore) {}
        realizarBusqueda();
    }

    private void cargarBodegas() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        bodegasMap.clear();
        model.addElement("GENERAL");
        try {
            TraspasoService svc = new TraspasoService();
            java.util.List<raven.clases.productos.Bodega> lista = svc.obtenerBodegasActivas();
            for (raven.clases.productos.Bodega b : lista) {
                String nombre = b.getNombre();
                model.addElement(nombre);
                bodegasMap.put(nombre, b.getIdBodega());
            }
        } catch (Exception ex) {
            model.addElement("bodega");
            model.addElement("tienda");
        }
        cmbBodega.setModel(model);
        cmbBodega.setSelectedItem("GENERAL");
        seleccionarBodegaOrigen();
        if (idBodegaOrigen != null && idBodegaOrigen > 0) {
            cmbBodega.setEnabled(false);
        }
    }

    private void seleccionarBodegaDestino() {
        if (idBodegaDestino == null || bodegasMap.isEmpty()) return;
        for (Map.Entry<String,Integer> e : bodegasMap.entrySet()) {
            if (idBodegaDestino.equals(e.getValue())) {
                cmbBodega.setSelectedItem(e.getKey());
                break;
            }
        }
    }

    private void seleccionarBodegaOrigen() {
        if (idBodegaOrigen == null || bodegasMap.isEmpty()) {
            // intentar desde configuración
            try {
                TraspasoConfig cfg = TraspasoConfig.load();
                Integer idCfg = cfg.getIdOrigen();
                if (idCfg != null) idBodegaOrigen = idCfg;
            } catch (Exception ignore) {}
        }
        if (idBodegaOrigen == null) return;
        for (Map.Entry<String,Integer> e : bodegasMap.entrySet()) {
            if (idBodegaOrigen.equals(e.getValue())) {
                cmbBodega.setSelectedItem(e.getKey());
                break;
            }
        }
    }

    private void bloquearCambioBodega() {
        // Dejar visible pero en solo lectura
        if (idBodegaOrigen != null && idBodegaOrigen > 0) {
            cmbBodega.setEnabled(false);
        }
        cmbBodega.addItemListener(evt -> {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                String sel = (String) cmbBodega.getSelectedItem();
                Integer idSel = bodegasMap.get(sel);
                if (idBodegaOrigen != null && !idBodegaOrigen.equals(idSel)) {
                    seleccionarBodegaOrigen();
                }
            }
        });
    }

    private void configurarCmbTipo() {
        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Par", "Caja"}));
        try {
            TraspasoConfig cfg = TraspasoConfig.load();
            String tipoCfg = cfg.getTipoLabel();
            if (tipoCfg != null && ("Par".equalsIgnoreCase(tipoCfg) || "Caja".equalsIgnoreCase(tipoCfg))) {
                cmbTipo.setSelectedItem(tipoCfg);
            } else {
                cmbTipo.setSelectedItem("Par");
            }
        } catch (Exception e) {
            cmbTipo.setSelectedItem("Par");
        }
        cmbTipo.addItemListener(evt -> {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                String val = (String) cmbTipo.getSelectedItem();
                if (!"Par".equalsIgnoreCase(val) && !"Caja".equalsIgnoreCase(val)) {
                    cmbTipo.setSelectedItem("Par");
                    JOptionPane.showMessageDialog(
                        this,
                        "Selección no permitida. Tipo debe ser Par o Caja.",
                        "Tipo inválido",
                        JOptionPane.ERROR_MESSAGE
                    );
                } else {
                    try {
                        TraspasoConfig cfg = TraspasoConfig.load();
                        cfg.setTipo("Par".equalsIgnoreCase(val) ? "par" : "caja");
                        TraspasoConfig.save(cfg);
                    } catch (Exception ignore) {}
                }
            }
        });
    }
    
    /**
     * Interfaz para escuchar selecciones de variante
     */
    @FunctionalInterface
    public interface VarianteSeleccionListener {
        void onVarianteSeleccionada(VarianteDTO variante);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        txtBusqueda = new javax.swing.JTextField();
        Lbgenero = new javax.swing.JLabel();
        cmbGenero = new javax.swing.JComboBox<>();
        lbBodega = new javax.swing.JLabel();
        cmbBodega = new javax.swing.JComboBox<>();
        lbstock = new javax.swing.JLabel();
        chkSoloStock = new javax.swing.JCheckBox();
        cmbTipo = new javax.swing.JComboBox<>();
        lbBodega1 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        panelResultados = new javax.swing.JPanel();
        scrollResultados = new javax.swing.JScrollPane();

        jPanel2.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED, java.awt.Color.white, java.awt.Color.white, java.awt.Color.white, java.awt.Color.white));

        Lbgenero.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        Lbgenero.setText("Genero");

        cmbGenero.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lbBodega.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbBodega.setText("Bodega");

        cmbBodega.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lbstock.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbstock.setText("Opciones");

        chkSoloStock.setText("Solo con stock");
        chkSoloStock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkSoloStockActionPerformed(evt);
            }
        });

        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Par", "Caja" }));

        lbBodega1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbBodega1.setText("Tipo");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtBusqueda)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Lbgenero)
                            .addComponent(cmbGenero, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbBodega)
                            .addComponent(cmbBodega, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbBodega1)
                            .addComponent(cmbTipo, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(51, 51, 51)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lbstock, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(chkSoloStock))))
                .addContainerGap(17, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(Lbgenero)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(cmbGenero, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                            .addComponent(lbBodega)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(cmbBodega, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(lbstock)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(chkSoloStock, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lbBodega1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cmbTipo, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jLabel1.setText("Buscador de Calzado");
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel1.setAlignmentX(0.5F);
        jLabel1.setAutoscrolls(true);
        jLabel1.setDoubleBuffered(true);

        javax.swing.GroupLayout panelResultadosLayout = new javax.swing.GroupLayout(panelResultados);
        panelResultados.setLayout(panelResultadosLayout);
        panelResultadosLayout.setHorizontalGroup(
            panelResultadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelResultadosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollResultados)
                .addContainerGap())
        );
        panelResultadosLayout.setVerticalGroup(
            panelResultadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelResultadosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollResultados, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelResultados, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(panelResultados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(17, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chkSoloStockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkSoloStockActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkSoloStockActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Lbgenero;
    private javax.swing.JCheckBox chkSoloStock;
    public javax.swing.JComboBox<String> cmbBodega;
    private javax.swing.JComboBox<String> cmbGenero;
    public javax.swing.JComboBox<String> cmbTipo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel lbBodega;
    private javax.swing.JLabel lbBodega1;
    private javax.swing.JLabel lbstock;
    private javax.swing.JPanel panelResultados;
    private javax.swing.JScrollPane scrollResultados;
    private javax.swing.JTextField txtBusqueda;
    // End of variables declaration//GEN-END:variables
}
