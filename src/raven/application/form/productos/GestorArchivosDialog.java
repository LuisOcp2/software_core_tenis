package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Gestor de archivos completo con vista previa de imágenes, navegación de carpetas,
 * accesos directos y gestión de favoritos.
 *
 * @author CrisDEV
 */
public class GestorArchivosDialog extends JDialog {

    // Componentes principales
    private JPanel panelIzquierdo;
    private JPanel panelCentral;
    private JPanel panelDerecho;
    private JList<File> listaAccesosDirectos;
    private DefaultListModel<File> modeloAccesos;
    private JTable tablaArchivos;
    private DefaultTableModel modeloTabla;
    private JLabel lblVistaPrevia;
    private JTextField txtRutaActual;
    private JButton btnAtras;
    private JButton btnAdelante;
    private JButton btnSubir;
    private JButton btnRefrescar;
    private JButton btnNuevaCarpeta;
    private JButton btnEliminar;
    private JButton btnAgregarFavorito;
    private JComboBox<String> cbxTipoArchivo;
    private JTextField txtBuscar;

    // Variables de estado
    private File carpetaActual;
    private File archivoSeleccionado;
    private List<File> historial;
    private int posicionHistorial;
    private Preferences prefs;
    private static final String PREF_FAVORITOS = "favoritos";
    private static final String PREF_ULTIMA_CARPETA = "ultimaCarpeta";
    private static final String CARPETA_PRODUCTOS = "productos_imagenes";
    private JToggleButton btnSoloImagenes;
    private boolean mostrarSoloImagenes = false;

    // Resultado
    private boolean archivoConfirmado = false;
    private byte[] datosArchivo = null;

    /**
     * Constructor del gestor de archivos
     * @param parent Ventana padre
     */
    public GestorArchivosDialog(Frame parent) {
        super(parent, "Gestor de Archivos - Productos", true);
        this.prefs = Preferences.userNodeForPackage(GestorArchivosDialog.class);
        this.historial = new ArrayList<>();
        this.posicionHistorial = -1;

        initComponents();
        cargarFavoritos();
        inicializarCarpetaProductos();
        cargarUltimaCarpeta();
    }

    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        setSize(1200, 700);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(8, 8));

        // Panel superior con barra de herramientas
        add(crearPanelSuperior(), BorderLayout.NORTH);

        // Panel central con 3 columnas
        JPanel panelPrincipal = new JPanel(new BorderLayout(8, 8));
        panelPrincipal.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Panel izquierdo: Accesos directos y favoritos
        panelIzquierdo = crearPanelIzquierdo();
        panelPrincipal.add(panelIzquierdo, BorderLayout.WEST);

        // Panel central: Lista de archivos
        panelCentral = crearPanelCentral();
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);

        // Panel derecho: Vista previa
        panelDerecho = crearPanelDerecho();
        panelPrincipal.add(panelDerecho, BorderLayout.EAST);

        add(panelPrincipal, BorderLayout.CENTER);

        // Panel inferior con botones de acción
        add(crearPanelInferior(), BorderLayout.SOUTH);

        // Estilos modernos
        aplicarEstilos();
    }

    /**
     * Crea el panel superior con barra de navegación
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Barra de herramientas de navegación
        JPanel barraNavegacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        //  BOTONES DE NAVEGACIÓN - Azul claro
        btnAtras = new JButton("← Atrás");
        btnAtras.setToolTipText("Navegar a la carpeta anterior");
        btnAtras.addActionListener(e -> navegarAtras());

        btnAdelante = new JButton("Adelante →");
        btnAdelante.setToolTipText("Navegar a la carpeta siguiente");
        btnAdelante.addActionListener(e -> navegarAdelante());

        btnSubir = new JButton("↑ Subir");
        btnSubir.setToolTipText("Subir un nivel en la jerarquía");
        btnSubir.addActionListener(e -> subirNivel());

        btnRefrescar = new JButton("⟳ Refrescar");
        btnRefrescar.setToolTipText("Actualizar la lista de archivos");
        btnRefrescar.addActionListener(e -> cargarArchivos());

        //  BOTONES DE ACCIÓN - Colores diferenciados
        btnNuevaCarpeta = new JButton(" Nueva");
        btnNuevaCarpeta.setToolTipText("Crear nueva carpeta");
        btnNuevaCarpeta.addActionListener(e -> crearNuevaCarpeta());

        btnEliminar = new JButton(" Eliminar");
        btnEliminar.setToolTipText("Eliminar archivo o carpeta seleccionada");
        btnEliminar.addActionListener(e -> eliminarSeleccionado());

        btnAgregarFavorito = new JButton(" Favorito");
        btnAgregarFavorito.setToolTipText("Agregar carpeta actual a favoritos");
        btnAgregarFavorito.addActionListener(e -> agregarFavorito());

        //  BOTÓN TOGGLE PARA MOSTRAR SOLO IMÁGENES
        btnSoloImagenes = new JToggleButton(" Solo Imágenes");
        btnSoloImagenes.setToolTipText("Mostrar únicamente archivos de imagen");
        btnSoloImagenes.addActionListener(e -> {
            mostrarSoloImagenes = btnSoloImagenes.isSelected();
            cargarArchivos();
        });

        barraNavegacion.add(btnAtras);
        barraNavegacion.add(btnAdelante);
        barraNavegacion.add(btnSubir);
        barraNavegacion.add(btnRefrescar);
        barraNavegacion.add(new JSeparator(SwingConstants.VERTICAL));
        barraNavegacion.add(btnSoloImagenes);
        barraNavegacion.add(new JSeparator(SwingConstants.VERTICAL));
        barraNavegacion.add(btnNuevaCarpeta);
        barraNavegacion.add(btnEliminar);
        barraNavegacion.add(btnAgregarFavorito);

        panel.add(barraNavegacion, BorderLayout.WEST);

        // Campo de ruta
        JPanel panelRuta = new JPanel(new BorderLayout(4, 0));
        panelRuta.add(new JLabel("Ubicación:"), BorderLayout.WEST);

        txtRutaActual = new JTextField();
        txtRutaActual.setEditable(false);
        panelRuta.add(txtRutaActual, BorderLayout.CENTER);

        JButton btnIr = new JButton("Ir");
        btnIr.addActionListener(e -> irARuta());
        panelRuta.add(btnIr, BorderLayout.EAST);

        panel.add(panelRuta, BorderLayout.CENTER);

        // Filtros y búsqueda
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        cbxTipoArchivo = new JComboBox<>(new String[]{
            "Todos los archivos",
            "Imágenes (*.jpg, *.png, *.gif, *.bmp)",
            "Solo imágenes JPG",
            "Solo imágenes PNG"
        });
        cbxTipoArchivo.addActionListener(e -> cargarArchivos());

        txtBuscar = new JTextField(15);
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarArchivos();
            }
        });

        panelFiltros.add(new JLabel("Tipo:"));
        panelFiltros.add(cbxTipoArchivo);
        panelFiltros.add(new JLabel("Buscar:"));
        panelFiltros.add(txtBuscar);

        panel.add(panelFiltros, BorderLayout.EAST);

        return panel;
    }

    /**
     * Crea el panel izquierdo con accesos directos
     */
    private JPanel crearPanelIzquierdo() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setPreferredSize(new Dimension(220, 0));

        JLabel lblTitulo = new JLabel("Accesos Rápidos");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(lblTitulo, BorderLayout.NORTH);

        modeloAccesos = new DefaultListModel<>();
        listaAccesosDirectos = new JList<>(modeloAccesos);
        listaAccesosDirectos.setCellRenderer(new AccesoDirectoRenderer());
        listaAccesosDirectos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaAccesosDirectos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File seleccionado = listaAccesosDirectos.getSelectedValue();
                if (seleccionado != null && seleccionado.isDirectory()) {
                    navegarACarpeta(seleccionado);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(listaAccesosDirectos);
        panel.add(scroll, BorderLayout.CENTER);

        // Botón para gestionar favoritos
        JButton btnGestionarFavoritos = new JButton("Gestionar Favoritos");
        btnGestionarFavoritos.addActionListener(e -> gestionarFavoritos());
        panel.add(btnGestionarFavoritos, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea el panel central con la tabla de archivos
     */
    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        JLabel lblTitulo = new JLabel("Archivos y Carpetas");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(lblTitulo, BorderLayout.NORTH);

        String[] columnas = {"Icono", "Nombre", "Tamaño", "Tipo", "Fecha Modificación"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return ImageIcon.class;
                return String.class;
            }
        };

        tablaArchivos = new JTable(modeloTabla);
        tablaArchivos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaArchivos.setRowHeight(40);
        tablaArchivos.getColumnModel().getColumn(0).setPreferredWidth(50);
        tablaArchivos.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaArchivos.getColumnModel().getColumn(1).setPreferredWidth(250);
        tablaArchivos.getColumnModel().getColumn(2).setPreferredWidth(80);
        tablaArchivos.getColumnModel().getColumn(3).setPreferredWidth(100);
        tablaArchivos.getColumnModel().getColumn(4).setPreferredWidth(150);

        // Renderizador centrado para el icono
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tablaArchivos.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        tablaArchivos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirSeleccionado();
                } else if (e.getClickCount() == 1) {
                    mostrarVistaPrevia();
                }
            }
        });

        //  AGREGAR KEY LISTENER PARA NAVEGACIÓN CON TECLADO
        tablaArchivos.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                // Actualizar vista previa cuando se navega con flechas arriba/abajo
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN ||
                    keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN ||
                    keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END) {
                    mostrarVistaPrevia();
                }
                // Abrir con Enter
                else if (keyCode == KeyEvent.VK_ENTER) {
                    abrirSeleccionado();
                }
                // Subir nivel con Backspace
                else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                    subirNivel();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tablaArchivos);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Crea el panel derecho con vista previa
     */
    private JPanel crearPanelDerecho() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setPreferredSize(new Dimension(300, 0));

        JLabel lblTitulo = new JLabel("Vista Previa");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(lblTitulo, BorderLayout.NORTH);

        lblVistaPrevia = new JLabel("Seleccione un archivo para ver la vista previa");
        lblVistaPrevia.setHorizontalAlignment(JLabel.CENTER);
        lblVistaPrevia.setVerticalAlignment(JLabel.CENTER);
        lblVistaPrevia.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblVistaPrevia.setPreferredSize(new Dimension(280, 280));

        JScrollPane scroll = new JScrollPane(lblVistaPrevia);
        panel.add(scroll, BorderLayout.CENTER);

        // Panel de información del archivo
        JPanel panelInfo = new JPanel();
        panelInfo.setLayout(new BoxLayout(panelInfo, BoxLayout.Y_AXIS));
        panelInfo.setBorder(BorderFactory.createTitledBorder("Información"));
        panel.add(panelInfo, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea el panel inferior con botones de acción
     */
    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton btnSeleccionar = new JButton("Seleccionar");
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE,
            "arc:8;background:#0A84FF;foreground:#fff;font:bold 13");
        btnSeleccionar.addActionListener(e -> seleccionarArchivo());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnCancelar.addActionListener(e -> {
            archivoConfirmado = false;
            dispose();
        });

        panel.add(btnSeleccionar);
        panel.add(btnCancelar);

        return panel;
    }

    /**
     * Aplica estilos modernos a los componentes
     */
    private void aplicarEstilos() {
        //  BOTONES DE NAVEGACIÓN - Azul claro
        String estiloNavegacion = "arc:8;background:#E3F2FD;foreground:#1976D2;borderWidth:1;borderColor:#90CAF9;font:bold 12";
        btnAtras.putClientProperty(FlatClientProperties.STYLE, estiloNavegacion);
        btnAdelante.putClientProperty(FlatClientProperties.STYLE, estiloNavegacion);
        btnSubir.putClientProperty(FlatClientProperties.STYLE, estiloNavegacion);
        btnRefrescar.putClientProperty(FlatClientProperties.STYLE, estiloNavegacion);

        //  BOTÓN TOGGLE - Verde cuando está activo
        String estiloToggle = "arc:8;background:#FFF3E0;foreground:#F57C00;borderWidth:1;borderColor:#FFB74D;font:bold 12";
        btnSoloImagenes.putClientProperty(FlatClientProperties.STYLE, estiloToggle);

        //  BOTÓN NUEVA CARPETA - Verde
        String estiloNueva = "arc:8;background:#E8F5E9;foreground:#388E3C;borderWidth:1;borderColor:#81C784;font:bold 12";
        btnNuevaCarpeta.putClientProperty(FlatClientProperties.STYLE, estiloNueva);

        //  BOTÓN ELIMINAR - Rojo
        String estiloEliminar = "arc:8;background:#FFEBEE;foreground:#D32F2F;borderWidth:1;borderColor:#EF5350;font:bold 12";
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, estiloEliminar);

        //  BOTÓN FAVORITO - Amarillo/Dorado
        String estiloFavorito = "arc:8;background:#FFF8E1;foreground:#F57F17;borderWidth:1;borderColor:#FFD54F;font:bold 12";
        btnAgregarFavorito.putClientProperty(FlatClientProperties.STYLE, estiloFavorito);

        // Campos de texto
        txtRutaActual.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        cbxTipoArchivo.putClientProperty(FlatClientProperties.STYLE, "arc:8");
    }

    /**
     * Inicializa la carpeta de productos si no existe
     */
    private void inicializarCarpetaProductos() {
        try {
            String userHome = System.getProperty("user.home");
            Path carpetaProductos = Paths.get(userHome, "Documents", CARPETA_PRODUCTOS);

            if (!Files.exists(carpetaProductos)) {
                Files.createDirectories(carpetaProductos);
                System.out.println("SUCCESS  Carpeta de productos creada: " + carpetaProductos);
            }

            // Agregar la carpeta de productos a los accesos directos por defecto
            modeloAccesos.addElement(carpetaProductos.toFile());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga los favoritos guardados
     */
    private void cargarFavoritos() {
        // Agregar ubicaciones del sistema
        FileSystemView fsv = FileSystemView.getFileSystemView();

        modeloAccesos.addElement(fsv.getHomeDirectory()); // Documentos
        modeloAccesos.addElement(new File(System.getProperty("user.home"), "Desktop")); // Escritorio
        modeloAccesos.addElement(new File(System.getProperty("user.home"), "Pictures")); // Imágenes
        modeloAccesos.addElement(new File(System.getProperty("user.home"), "Downloads")); // Descargas

        // Cargar favoritos personalizados
        String favoritosStr = prefs.get(PREF_FAVORITOS, "");
        if (!favoritosStr.isEmpty()) {
            String[] rutas = favoritosStr.split(";");
            for (String ruta : rutas) {
                File archivo = new File(ruta);
                if (archivo.exists() && archivo.isDirectory()) {
                    modeloAccesos.addElement(archivo);
                }
            }
        }
    }

    /**
     * Carga la última carpeta visitada
     */
    private void cargarUltimaCarpeta() {
        String ultimaCarpeta = prefs.get(PREF_ULTIMA_CARPETA, "");
        File carpeta;

        if (!ultimaCarpeta.isEmpty() && new File(ultimaCarpeta).exists()) {
            carpeta = new File(ultimaCarpeta);
        } else {
            String userHome = System.getProperty("user.home");
            carpeta = Paths.get(userHome, "Documents", CARPETA_PRODUCTOS).toFile();
            if (!carpeta.exists()) {
                carpeta = new File(userHome, "Documents");
            }
        }

        navegarACarpeta(carpeta);
    }

    /**
     * Navega a una carpeta específica
     */
    private void navegarACarpeta(File carpeta) {
        if (carpeta == null || !carpeta.exists() || !carpeta.isDirectory()) {
            return;
        }

        carpetaActual = carpeta;
        txtRutaActual.setText(carpeta.getAbsolutePath());

        // Actualizar historial
        if (posicionHistorial < historial.size() - 1) {
            historial = historial.subList(0, posicionHistorial + 1);
        }
        historial.add(carpeta);
        posicionHistorial = historial.size() - 1;

        actualizarBotonesNavegacion();
        cargarArchivos();

        // Guardar última carpeta
        prefs.put(PREF_ULTIMA_CARPETA, carpeta.getAbsolutePath());
    }

    /**
     * Carga los archivos de la carpeta actual
     */
    private void cargarArchivos() {
        if (carpetaActual == null) return;

        modeloTabla.setRowCount(0);

        File[] archivos = carpetaActual.listFiles();
        if (archivos == null) return;

        // Ordenar: carpetas primero, luego archivos
        Arrays.sort(archivos, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File archivo : archivos) {
            if (archivo.isHidden()) continue;

            if (!pasaFiltro(archivo)) continue;

            Object[] fila = new Object[5];
            fila[0] = obtenerIcono(archivo);
            fila[1] = archivo.getName();
            fila[2] = archivo.isDirectory() ? "" : formatearTamanio(archivo.length());
            fila[3] = archivo.isDirectory() ? "Carpeta" : obtenerTipoArchivo(archivo);
            fila[4] = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                .format(new Date(archivo.lastModified()));

            modeloTabla.addRow(fila);
        }
    }

    /**
     * Verifica si un archivo pasa el filtro actual
     */
    private boolean pasaFiltro(File archivo) {
        // Las carpetas siempre se muestran (a menos que esté activo el toggle de solo imágenes)
        if (archivo.isDirectory()) {
            return !mostrarSoloImagenes;
        }

        String nombre = archivo.getName().toLowerCase();

        //  SI EL TOGGLE "SOLO IMÁGENES" ESTÁ ACTIVO, solo mostrar imágenes
        if (mostrarSoloImagenes) {
            return nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") ||
                   nombre.endsWith(".png") || nombre.endsWith(".gif") ||
                   nombre.endsWith(".bmp");
        }

        // Si no está el toggle, aplicar el filtro del combobox
        String filtro = (String) cbxTipoArchivo.getSelectedItem();

        switch (filtro) {
            case "Imágenes (*.jpg, *.png, *.gif, *.bmp)":
                return nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") ||
                       nombre.endsWith(".png") || nombre.endsWith(".gif") ||
                       nombre.endsWith(".bmp");
            case "Solo imágenes JPG":
                return nombre.endsWith(".jpg") || nombre.endsWith(".jpeg");
            case "Solo imágenes PNG":
                return nombre.endsWith(".png");
            default:
                return true;
        }
    }

    /**
     * Filtra archivos según el texto de búsqueda
     */
    private void filtrarArchivos() {
        String busqueda = txtBuscar.getText().toLowerCase().trim();

        if (busqueda.isEmpty()) {
            cargarArchivos();
            return;
        }

        modeloTabla.setRowCount(0);

        File[] archivos = carpetaActual.listFiles();
        if (archivos == null) return;

        for (File archivo : archivos) {
            if (archivo.isHidden()) continue;
            if (!pasaFiltro(archivo)) continue;

            if (archivo.getName().toLowerCase().contains(busqueda)) {
                Object[] fila = new Object[5];
                fila[0] = obtenerIcono(archivo);
                fila[1] = archivo.getName();
                fila[2] = archivo.isDirectory() ? "" : formatearTamanio(archivo.length());
                fila[3] = archivo.isDirectory() ? "Carpeta" : obtenerTipoArchivo(archivo);
                fila[4] = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(new Date(archivo.lastModified()));

                modeloTabla.addRow(fila);
            }
        }
    }

    /**
     * Obtiene el icono apropiado para un archivo
     */
    private ImageIcon obtenerIcono(File archivo) {
        // Usar iconos del sistema de archivos de Java
        FileSystemView fsv = FileSystemView.getFileSystemView();
        Icon iconoSistema = fsv.getSystemIcon(archivo);

        if (iconoSistema instanceof ImageIcon) {
            return (ImageIcon) iconoSistema;
        }

        // Si no hay icono del sistema, crear uno básico
        return crearIconoBasico(archivo);
    }

    /**
     * Crea un icono básico con texto para representar el archivo
     */
    private ImageIcon crearIconoBasico(File archivo) {
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Configurar renderizado
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (archivo.isDirectory()) {
            // Icono de carpeta
            g2d.setColor(new Color(255, 193, 7)); // Amarillo
            g2d.fillRect(2, 8, 28, 20);
            g2d.setColor(new Color(255, 160, 0)); // Naranja
            g2d.fillRect(2, 8, 12, 4);
        } else {
            // Icono de archivo
            String nombre = archivo.getName().toLowerCase();
            if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") ||
                nombre.endsWith(".png") || nombre.endsWith(".gif") ||
                nombre.endsWith(".bmp")) {
                // Icono de imagen
                g2d.setColor(new Color(76, 175, 80)); // Verde
            } else {
                // Archivo genérico
                g2d.setColor(new Color(158, 158, 158)); // Gris
            }
            g2d.fillRect(6, 2, 20, 28);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(8, 6, 16, 3);
            g2d.fillRect(8, 12, 16, 3);
            g2d.fillRect(8, 18, 16, 3);
        }

        g2d.dispose();
        return new ImageIcon(img);
    }

    /**
     * Formatea el tamaño del archivo
     */
    private String formatearTamanio(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Obtiene el tipo de archivo
     */
    private String obtenerTipoArchivo(File archivo) {
        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');
        if (punto > 0 && punto < nombre.length() - 1) {
            return nombre.substring(punto + 1).toUpperCase();
        }
        return "Archivo";
    }

    /**
     * Muestra la vista previa del archivo seleccionado
     */
    private void mostrarVistaPrevia() {
        int filaSeleccionada = tablaArchivos.getSelectedRow();
        if (filaSeleccionada < 0) return;

        String nombreArchivo = (String) modeloTabla.getValueAt(filaSeleccionada, 1);
        File archivo = new File(carpetaActual, nombreArchivo);
        archivoSeleccionado = archivo;

        if (archivo.isDirectory()) {
            lblVistaPrevia.setIcon(null);
            lblVistaPrevia.setText("Carpeta: " + archivo.getName());
            return;
        }

        if (esImagen(archivo)) {
            try {
                ImageIcon iconoOriginal = new ImageIcon(archivo.getAbsolutePath());
                Image imagenEscalada = iconoOriginal.getImage().getScaledInstance(
                    280, 280, Image.SCALE_SMOOTH);
                lblVistaPrevia.setIcon(new ImageIcon(imagenEscalada));
                lblVistaPrevia.setText("");
            } catch (Exception e) {
                lblVistaPrevia.setIcon(null);
                lblVistaPrevia.setText("Error al cargar imagen");
            }
        } else {
            lblVistaPrevia.setIcon(null);
            lblVistaPrevia.setText("Vista previa no disponible");
        }
    }

    /**
     * Verifica si un archivo es una imagen
     */
    private boolean esImagen(File archivo) {
        String nombre = archivo.getName().toLowerCase();
        return nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") ||
               nombre.endsWith(".png") || nombre.endsWith(".gif") ||
               nombre.endsWith(".bmp");
    }

    /**
     * Abre el archivo/carpeta seleccionado
     */
    private void abrirSeleccionado() {
        int filaSeleccionada = tablaArchivos.getSelectedRow();
        if (filaSeleccionada < 0) return;

        String nombreArchivo = (String) modeloTabla.getValueAt(filaSeleccionada, 1);
        File archivo = new File(carpetaActual, nombreArchivo);

        if (archivo.isDirectory()) {
            navegarACarpeta(archivo);
        } else {
            seleccionarArchivo();
        }
    }

    /**
     * Selecciona el archivo actual
     */
    private void seleccionarArchivo() {
        if (archivoSeleccionado == null || !archivoSeleccionado.exists()) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un archivo válido",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (archivoSeleccionado.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un archivo, no una carpeta",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            datosArchivo = Files.readAllBytes(archivoSeleccionado.toPath());
            archivoConfirmado = true;
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error al leer el archivo: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Navega hacia atrás en el historial
     */
    private void navegarAtras() {
        if (posicionHistorial > 0) {
            posicionHistorial--;
            carpetaActual = historial.get(posicionHistorial);
            txtRutaActual.setText(carpetaActual.getAbsolutePath());
            cargarArchivos();
            actualizarBotonesNavegacion();
        }
    }

    /**
     * Navega hacia adelante en el historial
     */
    private void navegarAdelante() {
        if (posicionHistorial < historial.size() - 1) {
            posicionHistorial++;
            carpetaActual = historial.get(posicionHistorial);
            txtRutaActual.setText(carpetaActual.getAbsolutePath());
            cargarArchivos();
            actualizarBotonesNavegacion();
        }
    }

    /**
     * Sube un nivel en la jerarquía de carpetas
     */
    private void subirNivel() {
        if (carpetaActual != null && carpetaActual.getParentFile() != null) {
            navegarACarpeta(carpetaActual.getParentFile());
        }
    }

    /**
     * Va a la ruta especificada
     */
    private void irARuta() {
        String ruta = JOptionPane.showInputDialog(this,
            "Ingrese la ruta:",
            txtRutaActual.getText());

        if (ruta != null && !ruta.trim().isEmpty()) {
            File carpeta = new File(ruta);
            if (carpeta.exists() && carpeta.isDirectory()) {
                navegarACarpeta(carpeta);
            } else {
                JOptionPane.showMessageDialog(this,
                    "La ruta no existe o no es una carpeta válida",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Crea una nueva carpeta
     */
    private void crearNuevaCarpeta() {
        String nombre = JOptionPane.showInputDialog(this,
            "Nombre de la nueva carpeta:");

        if (nombre != null && !nombre.trim().isEmpty()) {
            File nuevaCarpeta = new File(carpetaActual, nombre);
            if (nuevaCarpeta.mkdir()) {
                cargarArchivos();
                JOptionPane.showMessageDialog(this,
                    "Carpeta creada exitosamente",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo crear la carpeta",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Elimina el archivo/carpeta seleccionado
     */
    private void eliminarSeleccionado() {
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un archivo o carpeta",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de eliminar: " + archivoSeleccionado.getName() + "?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            if (archivoSeleccionado.delete()) {
                cargarArchivos();
                archivoSeleccionado = null;
                lblVistaPrevia.setIcon(null);
                lblVistaPrevia.setText("Seleccione un archivo para ver la vista previa");
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar el archivo",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Agrega la carpeta actual a favoritos
     */
    private void agregarFavorito() {
        if (carpetaActual != null) {
            // Verificar si ya existe
            for (int i = 0; i < modeloAccesos.size(); i++) {
                if (modeloAccesos.get(i).equals(carpetaActual)) {
                    JOptionPane.showMessageDialog(this,
                        "Esta carpeta ya está en favoritos",
                        "Aviso", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            modeloAccesos.addElement(carpetaActual);
            guardarFavoritos();
            JOptionPane.showMessageDialog(this,
                "Carpeta agregada a favoritos",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Gestiona los favoritos
     */
    private void gestionarFavoritos() {
        // Aquí puedes implementar un diálogo para gestionar favoritos
        JOptionPane.showMessageDialog(this,
            "Función de gestión de favoritos en desarrollo",
            "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Guarda los favoritos en las preferencias
     */
    private void guardarFavoritos() {
        StringBuilder sb = new StringBuilder();

        // Solo guardar favoritos personalizados (después de los del sistema)
        for (int i = 4; i < modeloAccesos.size(); i++) {
            File favorito = modeloAccesos.get(i);
            if (sb.length() > 0) sb.append(";");
            sb.append(favorito.getAbsolutePath());
        }

        prefs.put(PREF_FAVORITOS, sb.toString());
    }

    /**
     * Actualiza el estado de los botones de navegación
     */
    private void actualizarBotonesNavegacion() {
        btnAtras.setEnabled(posicionHistorial > 0);
        btnAdelante.setEnabled(posicionHistorial < historial.size() - 1);
        btnSubir.setEnabled(carpetaActual != null && carpetaActual.getParentFile() != null);
    }

    /**
     * Obtiene los datos del archivo seleccionado
     */
    public byte[] getDatosArchivo() {
        return datosArchivo;
    }

    /**
     * Verifica si se confirmó la selección
     */
    public boolean isArchivoConfirmado() {
        return archivoConfirmado;
    }

    /**
     * Obtiene el archivo seleccionado
     */
    public File getArchivoSeleccionado() {
        return archivoSeleccionado;
    }

    /**
     * Renderer personalizado para la lista de accesos directos
     */
    private class AccesoDirectoRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof File) {
                File archivo = (File) value;
                setText(archivo.getName().isEmpty() ? archivo.getAbsolutePath() : archivo.getName());

                // Usar icono del sistema
                FileSystemView fsv = FileSystemView.getFileSystemView();
                Icon iconoSistema = fsv.getSystemIcon(archivo);
                if (iconoSistema != null) {
                    setIcon(iconoSistema);
                }
            }

            return this;
        }
    }
}

