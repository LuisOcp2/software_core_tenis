package raven.componentes;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;

/**
 * Gestor de archivos profesional con vista de galería, previews y navegación completa
 * @author CrisDEV
 */
public class ImageFileManager extends JPanel {

    private File currentDirectory;
    private File selectedFile;
    private byte[] selectedImageBytes;

    // Componentes UI
    private JPanel thumbnailsPanel;
    private JLabel previewLabel;
    private JLabel fileInfoLabel;
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JLabel pathLabel;
    private JScrollPane thumbnailScroll;
    private JButton btnAccept;
    private JButton btnCancel;

    // Configuración
    private static final int THUMBNAIL_SIZE = 120;
    private static final int PREVIEW_SIZE = 400;
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};

    // Cache de thumbnails para mejorar rendimiento
    private Map<String, ImageIcon> thumbnailCache = new HashMap<>();

    // Callback
    private ImageSelectionCallback callback;

    public interface ImageSelectionCallback {
        void onImageSelected(File file, byte[] imageBytes);
    }

    public ImageFileManager() {
        this(getDefaultImageDirectory());
    }

    public ImageFileManager(String initialPath) {
        this.currentDirectory = new File(initialPath);
        if (!currentDirectory.exists() || !currentDirectory.isDirectory()) {
            currentDirectory = new File(System.getProperty("user.home"));
        }

        initComponents();
        loadDirectory(currentDirectory);
    }

    private static String getDefaultImageDirectory() {
        // Intentar obtener el directorio de imágenes del usuario
        FileSystemView filesys = FileSystemView.getFileSystemView();
        File homeDir = filesys.getHomeDirectory();

        // Buscar carpeta de imágenes
        String[] possiblePaths = {
            System.getProperty("user.home") + "\\Pictures",
            System.getProperty("user.home") + "\\Imágenes",
            System.getProperty("user.home") + "\\OneDrive\\Pictures",
            System.getProperty("user.home")
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                return path;
            }
        }

        return System.getProperty("user.home");
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        putClientProperty(FlatClientProperties.STYLE, "arc:24;background:$Panel.background");

        // Panel superior - Navegación y búsqueda
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Panel central - Galería y Preview
        JSplitPane centerSplit = createCenterPanel();
        add(centerSplit, BorderLayout.CENTER);

        // Panel inferior - Información y botones
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        // Barra de navegación
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        navPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        JButton btnBack = new JButton("←");
        btnBack.setToolTipText("Atrás");
        btnBack.putClientProperty(FlatClientProperties.STYLE, "arc:12;borderWidth:0;background:lighten($Panel.background,10%)");
        btnBack.addActionListener(e -> navigateBack());

        JButton btnUp = new JButton("↑");
        btnUp.setToolTipText("Carpeta superior");
        btnUp.putClientProperty(FlatClientProperties.STYLE, "arc:12;borderWidth:0;background:lighten($Panel.background,10%)");
        btnUp.addActionListener(e -> navigateUp());

        JButton btnHome = new JButton("Inicio");
        btnHome.setToolTipText("Inicio");
        btnHome.putClientProperty(FlatClientProperties.STYLE, "arc:12;borderWidth:0;background:lighten($Panel.background,10%)");
        btnHome.addActionListener(e -> navigateHome());

        JButton btnRefresh = new JButton("⟳");
        btnRefresh.setToolTipText("Actualizar");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc:12;borderWidth:0;background:lighten($Panel.background,10%)");
        btnRefresh.addActionListener(e -> refreshDirectory());

        pathLabel = new JLabel();
        pathLabel.putClientProperty(FlatClientProperties.STYLE,
            "font:bold +1;foreground:$Component.accentColor");

        navPanel.add(btnBack);
        navPanel.add(btnUp);
        navPanel.add(btnHome);
        navPanel.add(btnRefresh);
        navPanel.add(Box.createHorizontalStrut(10));
        navPanel.add(new JLabel("Ruta: "));
        navPanel.add(pathLabel);

        // Barra de búsqueda y filtros
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        searchField = new JTextField(20);
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar archivos...");
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc:12");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterFiles();
            }
        });

        filterCombo = new JComboBox<>(new String[]{"Todas las imágenes", "JPG/JPEG", "PNG", "GIF", "BMP"});
        filterCombo.putClientProperty(FlatClientProperties.STYLE, "arc:12");
        filterCombo.addActionListener(e -> filterFiles());

        searchPanel.add(new JLabel("Buscar: "));
        searchPanel.add(searchField);
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(new JLabel("Filtro: "));
        searchPanel.add(filterCombo);

        panel.add(navPanel, BorderLayout.WEST);
        panel.add(searchPanel, BorderLayout.EAST);

        return panel;
    }

    private JSplitPane createCenterPanel() {
        // Panel izquierdo - Galería de thumbnails
        thumbnailsPanel = new JPanel();
        thumbnailsPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));
        thumbnailsPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        thumbnailScroll = new JScrollPane(thumbnailsPanel);
        thumbnailScroll.setBorder(BorderFactory.createTitledBorder("Galería de Imágenes"));
        thumbnailScroll.getVerticalScrollBar().setUnitIncrement(16);
        thumbnailScroll.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;borderColor:$Component.borderColor");

        // Panel derecho - Preview grande
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Vista Previa"));
        previewPanel.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;background:$Panel.background;borderColor:$Component.borderColor");

        previewLabel = new JLabel("Selecciona una imagen para ver la vista previa", SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        previewLabel.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;background:lighten($Panel.background,5%)");

        previewPanel.add(previewLabel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, thumbnailScroll, previewPanel);
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(0.6);
        splitPane.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        return splitPane;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        // Información del archivo
        fileInfoLabel = new JLabel("Ningún archivo seleccionado");
        fileInfoLabel.putClientProperty(FlatClientProperties.STYLE, "font:+0");

        // Botones de acción
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        btnAccept = new JButton("Seleccionar");
        btnAccept.setEnabled(false);
        btnAccept.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;background:#0A84FF;foreground:#fff;borderWidth:0;focusWidth:0;innerFocusWidth:0");
        btnAccept.addActionListener(e -> acceptSelection());

        btnCancel = new JButton("Cancelar");
        btnCancel.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;borderWidth:0;focusWidth:0;innerFocusWidth:0");
        btnCancel.addActionListener(e -> cancelSelection());

        JButton btnUploadNew = new JButton(" Subir Nueva Imagen");
        btnUploadNew.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;background:#34C759;foreground:#fff;borderWidth:0;focusWidth:0;innerFocusWidth:0");
        btnUploadNew.addActionListener(e -> uploadNewImage());

        buttonsPanel.add(btnUploadNew);
        buttonsPanel.add(btnCancel);
        buttonsPanel.add(btnAccept);

        panel.add(fileInfoLabel, BorderLayout.WEST);
        panel.add(buttonsPanel, BorderLayout.EAST);

        return panel;
    }

    private void loadDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        currentDirectory = directory;
        pathLabel.setText(directory.getAbsolutePath());
        thumbnailsPanel.removeAll();

        // Mostrar mensaje de carga
        JLabel loadingLabel = new JLabel("Cargando imágenes...", SwingConstants.CENTER);
        thumbnailsPanel.add(loadingLabel);
        thumbnailsPanel.revalidate();
        thumbnailsPanel.repaint();

        // Cargar archivos en segundo plano
        SwingWorker<Void, ThumbnailItem> worker = new SwingWorker<Void, ThumbnailItem>() {
            @Override
            protected Void doInBackground() throws Exception {
                File[] files = directory.listFiles();
                if (files != null) {
                    // Ordenar: primero carpetas, luego archivos
                    Arrays.sort(files, (f1, f2) -> {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    });

                    for (File file : files) {
                        if (file.isDirectory()) {
                            publish(new ThumbnailItem(file, null, true));
                        } else if (isImageFile(file)) {
                            ImageIcon thumbnail = createThumbnail(file);
                            publish(new ThumbnailItem(file, thumbnail, false));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ThumbnailItem> chunks) {
                if (thumbnailsPanel.getComponentCount() == 1 &&
                    thumbnailsPanel.getComponent(0) instanceof JLabel) {
                    thumbnailsPanel.removeAll();
                }

                for (ThumbnailItem item : chunks) {
                    thumbnailsPanel.add(createThumbnailPanel(item.file, item.thumbnail, item.isDirectory));
                }
                thumbnailsPanel.revalidate();
                thumbnailsPanel.repaint();
            }

            @Override
            protected void done() {
                if (thumbnailsPanel.getComponentCount() == 0) {
                    JLabel emptyLabel = new JLabel("No se encontraron imágenes en esta carpeta", SwingConstants.CENTER);
                    thumbnailsPanel.add(emptyLabel);
                }
                thumbnailsPanel.revalidate();
                thumbnailsPanel.repaint();
            }
        };

        worker.execute();
    }

    private JPanel createThumbnailPanel(File file, ImageIcon thumbnail, boolean isDirectory) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(THUMBNAIL_SIZE + 20, THUMBNAIL_SIZE + 50));
        panel.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;background:lighten($Panel.background,5%);border:1,1,1,1,$Component.borderColor,12");

        // Imagen o icono de carpeta
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(THUMBNAIL_SIZE, THUMBNAIL_SIZE));

        if (isDirectory) {
            Icon icon = UIManager.getIcon("FileView.directoryIcon");
            if (icon == null) icon = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(file);
            imageLabel.setIcon(icon);
        } else if (thumbnail != null) {
            imageLabel.setIcon(thumbnail);
        } else {
            Icon icon = UIManager.getIcon("FileView.fileIcon");
            if (icon == null) icon = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(file);
            imageLabel.setIcon(icon);
        }

        // Nombre del archivo (truncado si es muy largo)
        String displayName = file.getName();
        if (displayName.length() > 15) {
            displayName = displayName.substring(0, 12) + "...";
        }
        JLabel nameLabel = new JLabel(displayName, SwingConstants.CENTER);
        nameLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1");
        nameLabel.setToolTipText(file.getName());

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);

        // Eventos de clic
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Doble clic
                    if (isDirectory) {
                        loadDirectory(file);
                    } else {
                        acceptSelection();
                    }
                } else {
                    // Un solo clic
                    if (!isDirectory) {
                        selectFile(file);
                        highlightPanel(panel);
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.putClientProperty(FlatClientProperties.STYLE,
                    "arc:12;background:lighten($Panel.background,10%);border:2,2,2,2,$Component.accentColor,12");
                panel.revalidate();
                panel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedFile == null || !selectedFile.equals(file)) {
                    panel.putClientProperty(FlatClientProperties.STYLE,
                        "arc:12;background:lighten($Panel.background,5%);border:1,1,1,1,$Component.borderColor,12");
                    panel.revalidate();
                    panel.repaint();
                }
            }
        };

        panel.addMouseListener(clickListener);
        imageLabel.addMouseListener(clickListener);
        nameLabel.addMouseListener(clickListener);

        return panel;
    }

    private void highlightPanel(JPanel selectedPanel) {
        // Quitar highlight de todos los paneles
        for (Component comp : thumbnailsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel p = (JPanel) comp;
                p.putClientProperty(FlatClientProperties.STYLE,
                    "arc:12;background:lighten($Panel.background,5%);border:1,1,1,1,$Component.borderColor,12");
            }
        }

        // Highlight al panel seleccionado
        selectedPanel.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;background:lighten($Component.accentColor,40%);border:2,2,2,2,$Component.accentColor,12");

        thumbnailsPanel.revalidate();
        thumbnailsPanel.repaint();
    }

    private void selectFile(File file) {
        selectedFile = file;
        btnAccept.setEnabled(true);

        // Mostrar preview grande
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                // Escalar manteniendo proporción
                int width = img.getWidth();
                int height = img.getHeight();
                double scale = Math.min((double)PREVIEW_SIZE / width, (double)PREVIEW_SIZE / height);
                int scaledWidth = (int)(width * scale);
                int scaledHeight = (int)(height * scale);

                Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaledImg));
                previewLabel.setText("");

                // Cargar bytes de la imagen
                selectedImageBytes = Files.readAllBytes(file.toPath());
            }
        } catch (IOException ex) {
            previewLabel.setIcon(null);
            previewLabel.setText("Error al cargar la imagen");
        }

        // Mostrar información del archivo
        try {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            long sizeKB = attrs.size() / 1024;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String date = sdf.format(new Date(attrs.lastModifiedTime().toMillis()));

            fileInfoLabel.setText(String.format("Archivo: %s | Tamaño: %d KB | Fecha: %s",
                file.getName(), sizeKB, date));
        } catch (IOException ex) {
            fileInfoLabel.setText("Archivo: " + file.getName());
        }
    }

    private ImageIcon createThumbnail(File file) {
        // Revisar caché primero
        String cacheKey = file.getAbsolutePath();
        if (thumbnailCache.containsKey(cacheKey)) {
            return thumbnailCache.get(cacheKey);
        }

        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                // Escalar manteniendo proporción
                int width = img.getWidth();
                int height = img.getHeight();
                double scale = Math.min((double)THUMBNAIL_SIZE / width, (double)THUMBNAIL_SIZE / height);
                int scaledWidth = (int)(width * scale);
                int scaledHeight = (int)(height * scale);

                Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaledImg);

                // Guardar en caché
                thumbnailCache.put(cacheKey, icon);

                return icon;
            }
        } catch (IOException ex) {
            // Ignorar errores de lectura
        }

        return null;
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void filterFiles() {
        // TODO: Implementar filtrado por búsqueda y tipo
        // Por ahora recargar el directorio
        loadDirectory(currentDirectory);
    }

    private void navigateBack() {
        // TODO: Implementar historial de navegación
        navigateUp();
    }

    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null && parent.exists()) {
            loadDirectory(parent);
        }
    }

    private void navigateHome() {
        loadDirectory(new File(getDefaultImageDirectory()));
    }

    private void refreshDirectory() {
        thumbnailCache.clear();
        loadDirectory(currentDirectory);
    }

    private void uploadNewImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || isImageFile(f);
            }

            @Override
            public String getDescription() {
                return "Archivos de imagen (JPG, PNG, GIF, BMP)";
            }
        });

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File sourceFile = chooser.getSelectedFile();
            try {
                // Copiar archivo al directorio actual
                Path targetPath = Paths.get(currentDirectory.getAbsolutePath(), sourceFile.getName());
                Files.copy(sourceFile.toPath(), targetPath);

                // Recargar directorio
                refreshDirectory();

                JOptionPane.showMessageDialog(this,
                    "Imagen subida correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al subir la imagen: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void acceptSelection() {
        if (selectedFile != null && callback != null) {
            callback.onImageSelected(selectedFile, selectedImageBytes);
        }
        closeDialog();
    }

    private void cancelSelection() {
        selectedFile = null;
        selectedImageBytes = null;
        closeDialog();
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    public void setCallback(ImageSelectionCallback callback) {
        this.callback = callback;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public byte[] getSelectedImageBytes() {
        return selectedImageBytes;
    }

    // Clase auxiliar para items de thumbnail
    private static class ThumbnailItem {
        File file;
        ImageIcon thumbnail;
        boolean isDirectory;

        ThumbnailItem(File file, ImageIcon thumbnail, boolean isDirectory) {
            this.file = file;
            this.thumbnail = thumbnail;
            this.isDirectory = isDirectory;
        }
    }

    // Layout personalizado para wrap (flow que salta de línea)
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();

                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);

                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                        if (rowWidth + d.width > maxWidth) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }

                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }

                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }

                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                if (scrollPane != null && target.isValid()) {
                    dim.width -= (hgap + 1);
                }

                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);

            if (dim.height > 0) {
                dim.height += getVgap();
            }

            dim.height += rowHeight;
        }
    }

    /**
     * Muestra el gestor de archivos en un diálogo modal
     * @param parent Ventana padre
     * @param title Título del diálogo
     * @param callback Callback cuando se selecciona una imagen
     */
    public static void showDialog(Window parent, String title, ImageSelectionCallback callback) {
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        ImageFileManager manager = new ImageFileManager();
        manager.setCallback(callback);

        dialog.setContentPane(manager);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}

