package raven.utils;

import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ProductImageOptimizer {

    // Thread para carga secuencial (simple background thread)
    private static Thread loadingThread;
    private static volatile java.util.concurrent.ExecutorService IMAGE_EXECUTOR;
    private static final int MAX_CACHE_SIZE = 100;
    private static final int PREFETCH_BUFFER = 8; // filas adicionales fuera del viewport
    private static final int THROTTLE_DELAY_MS = 100;
    private static javax.swing.Timer throttleTimer;
    private static volatile boolean throttleScheduled;
    private static final java.util.LinkedHashMap<Integer, ImageIcon> ICON_CACHE = new java.util.LinkedHashMap<Integer, ImageIcon>(
            16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Integer, ImageIcon> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private static final java.util.LinkedHashMap<Integer, ImageIcon> LARGE_ICON_CACHE = new java.util.LinkedHashMap<Integer, ImageIcon>(
            16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Integer, ImageIcon> eldest) {
            return size() > 50;
        }
    };

    static {
        // Configuración global única para deshabilitar caché de disco
        try {
            javax.imageio.ImageIO.setUseCache(false);
        } catch (Exception ignore) {
        }
        ensureExecutor();
    }

    private static synchronized void ensureExecutor() {
        if (IMAGE_EXECUTOR == null || IMAGE_EXECUTOR.isShutdown() || IMAGE_EXECUTOR.isTerminated()) {
            int cores = Runtime.getRuntime().availableProcessors();
            int poolSize = Math.min(4, Math.max(2, cores / 2));
            IMAGE_EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(poolSize, r -> {
                Thread t = new Thread(r, "ImagePool");
                t.setDaemon(true);
                return t;
            });
        }
    }

    /**
     * Carga las imágenes secuencialmente en segundo plano y actualiza la tabla.
     * Busca la primera imagen disponible en las variantes del producto.
     */
    public static void loadImagesSequential(JTable table, List<ModelProduct> products) {
        // Cancelar hilo anterior si existe para evitar conflictos
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }

        loadingThread = new Thread(() -> {
            try {
                for (int i = 0; i < products.size(); i++) {
                    // Verificar si se canceló el hilo
                    if (Thread.currentThread().isInterrupted())
                        return;

                    ModelProduct product = products.get(i);

                    // Solo cargar si no tiene icono ya asignado
                    if (product.getCachedIcon() == null) {
                        ImageIcon cached = ICON_CACHE.get(product.getProductId());
                        ImageIcon icon = cached != null ? cached : loadFirstImage(product.getProductId());

                        if (icon != null) {
                            product.setCachedIcon(icon);
                            ICON_CACHE.put(product.getProductId(), icon);

                            // Actualizar la interfaz gráfica de manera optimizada
                            final int modelRowIndex = i;
                            SwingUtilities.invokeLater(() -> {
                                if (table != null && table.isDisplayable()) {
                                    try {
                                        // Convertir índice del modelo a la vista (por si hay ordenamiento)
                                        int viewRow = table.convertRowIndexToView(modelRowIndex);
                                        if (viewRow != -1) {
                                            // Repintar SOLO la celda de la imagen (Columna 2)
                                            // Esto elimina el parpadeo y la sobrecarga de repintar toda la tabla
                                            java.awt.Rectangle rect = table.getCellRect(viewRow, 2, true);
                                            table.repaint(rect);
                                        }
                                    } catch (Exception ignore) {
                                        // Fallback seguro por si la tabla cambia mientras cargamos
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error en carga de imágenes secuencial: " + e.getMessage());
            }
        });

        loadingThread.setName("ImageLoader-Sequential");
        loadingThread.setDaemon(true); // Permitir que la app se cierre si este hilo sigue corriendo
        loadingThread.start();
    }

    /**
     * Carga imágenes para la lista de inventario detallado.
     */
    public static void loadImagesSequentialInventory(JTable table,
            List<raven.application.form.productos.dto.InventarioDetalleItem> items) {
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }

        loadingThread = new Thread(() -> {
            try {
                for (int i = 0; i < items.size(); i++) {
                    if (Thread.currentThread().isInterrupted())
                        return;
                    raven.application.form.productos.dto.InventarioDetalleItem item = items.get(i);

                    if (item.getCachedIcon() == null) {
                        ImageIcon cached = ICON_CACHE.get(item.getIdProducto());
                        ImageIcon icon = cached != null ? cached : loadFirstImage(item.getIdProducto());

                        if (icon != null) {
                            item.setCachedIcon(icon);
                            ICON_CACHE.put(item.getIdProducto(), icon);
                            final int idx = i;
                            SwingUtilities.invokeLater(() -> {
                                if (table != null && table.isDisplayable()) {
                                    try {
                                        int viewRow = table.convertRowIndexToView(idx);
                                        if (viewRow != -1) {
                                            java.awt.Rectangle rect = table.getCellRect(viewRow, 2, true);
                                            table.repaint(rect);
                                        }
                                    } catch (Exception ignore) {
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
            }
        });
        loadingThread.setDaemon(true);
        loadingThread.start();
    }

    /**
     * Carga imágenes para la lista agrupada.
     */
    public static void loadImagesSequentialGrouped(JTable table,
            List<raven.application.form.productos.dto.ProductoAgrupado> items) {
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }

        loadingThread = new Thread(() -> {
            try {
                for (int i = 0; i < items.size(); i++) {
                    if (Thread.currentThread().isInterrupted())
                        return;
                    raven.application.form.productos.dto.ProductoAgrupado item = items.get(i);

                    if (item.getCachedIcon() == null) {
                        ImageIcon cached = ICON_CACHE.get(item.getIdProducto());
                        ImageIcon icon = cached != null ? cached : loadFirstImage(item.getIdProducto());

                        if (icon != null) {
                            item.setCachedIcon(icon);
                            ICON_CACHE.put(item.getIdProducto(), icon);
                            final int idx = i;
                            SwingUtilities.invokeLater(() -> {
                                if (table != null && table.isDisplayable()) {
                                    try {
                                        int viewRow = table.convertRowIndexToView(idx);
                                        if (viewRow != -1) {
                                            java.awt.Rectangle rect = table.getCellRect(viewRow, 2, true);
                                            table.repaint(rect);
                                        }
                                    } catch (Exception ignore) {
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
            }
        });
        loadingThread.setDaemon(true);
        loadingThread.start();
    }

    public static void prefetchAllForPage(JTable table, List<ModelProduct> products) {
        if (products == null || products.isEmpty())
            return;
        // Evitar prefetch repetido para la misma página/lista
        try {
            String key = buildPrefetchKey(products);
            Object prev = table.getClientProperty("_imgPrefetchKey");
            if (prev != null && key.equals(String.valueOf(prev))) {
                return;
            }
            table.putClientProperty("_imgPrefetchKey", key);
        } catch (Exception ignore) {
        }
        ensureExecutor();
        for (int i = 0; i < products.size(); i++) {
            final int idx = i;
            ModelProduct p = products.get(i);
            if (p == null)
                continue;
            if (p.getCachedIcon() != null)
                continue;
            try {
                IMAGE_EXECUTOR.submit(() -> {
                    try {
                        ImageIcon cached = ICON_CACHE.get(p.getProductId());
                        ImageIcon icon = cached != null ? cached : loadFirstImage(p.getProductId());
                        if (icon != null) {
                            p.setCachedIcon(icon);
                            ICON_CACHE.put(p.getProductId(), icon);
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                if (table != null && table.isDisplayable()) {
                                    try {
                                        int viewRow = table.convertRowIndexToView(idx);
                                        if (viewRow != -1) {
                                            java.awt.Rectangle rect = table.getCellRect(viewRow, 2, true);
                                            table.repaint(rect);
                                        }
                                    } catch (Exception ignore) {
                                    }
                                }
                            });
                        }
                    } catch (Exception ignore) {
                    }
                });
            } catch (java.util.concurrent.RejectedExecutionException rex) {
                ensureExecutor();
                try {
                    IMAGE_EXECUTOR.submit(() -> {
                        try {
                            ImageIcon cached = ICON_CACHE.get(p.getProductId());
                            ImageIcon icon = cached != null ? cached : loadFirstImage(p.getProductId());
                            if (icon != null) {
                                p.setCachedIcon(icon);
                                ICON_CACHE.put(p.getProductId(), icon);
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    if (table != null && table.isDisplayable()) {
                                        try {
                                            int viewRow = table.convertRowIndexToView(idx);
                                            if (viewRow != -1) {
                                                java.awt.Rectangle rect = table.getCellRect(viewRow, 2, true);
                                                table.repaint(rect);
                                            }
                                        } catch (Exception ignore) {
                                        }
                                    }
                                });
                            }
                        } catch (Exception ignore) {
                        }
                    });
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static String buildPrefetchKey(List<ModelProduct> products) {
        int size = products.size();
        int first = 0;
        int last = 0;
        try {
            if (size > 0 && products.get(0) != null)
                first = products.get(0).getProductId();
        } catch (Exception ignore) {
        }
        try {
            if (size > 1 && products.get(size - 1) != null)
                last = products.get(size - 1).getProductId();
        } catch (Exception ignore) {
        }
        return size + ":" + first + ":" + last;
    }

    /**
     * Adjunta un lazy loader que solo carga imágenes para filas visibles con un
     * pequeño buffer.
     * Implementa throttling para evitar sobrecarga al desplazarse.
     */
    public static void attachLazyLoader(JTable table, List<ModelProduct> products) {
        if (table == null || products == null || products.isEmpty())
            return;

        // Cancelar cualquier carga previa
        clearCache();

        // Cargar inmediatamente el rango visible inicial
        requestLoadForVisibleRange(table, products);

        // Agregar listeners de scroll con throttle
        javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities
                .getAncestorOfClass(javax.swing.JViewport.class, table);
        if (viewport != null) {
            viewport.addChangeListener(e -> {
                if (throttleTimer == null) {
                    throttleTimer = new javax.swing.Timer(THROTTLE_DELAY_MS, ev -> {
                        throttleScheduled = false;
                        requestLoadForVisibleRange(table, products);
                    });
                    throttleTimer.setRepeats(false);
                }
                if (!throttleScheduled) {
                    throttleScheduled = true;
                    throttleTimer.restart();
                }
            });
        }
    }

    private static void requestLoadForVisibleRange(JTable table, List<ModelProduct> products) {
        java.awt.Rectangle visible = table.getVisibleRect();
        int first = table.rowAtPoint(new java.awt.Point(0, visible.y));
        int last = table.rowAtPoint(new java.awt.Point(0, visible.y + visible.height - 1));
        if (first < 0)
            first = 0;
        if (last < 0)
            last = table.getRowCount() - 1;

        // Buffer para prefetch
        first = Math.max(0, first - PREFETCH_BUFFER);
        last = Math.min(table.getRowCount() - 1, last + PREFETCH_BUFFER);

        // Traducir índices de vista a índices de modelo
        final int modelFirst = table.convertRowIndexToModel(first);
        final int modelLast = table.convertRowIndexToModel(last);

        // Lanzar carga en segundo plano
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
        loadingThread = new Thread(() -> {
            try {
                int start = Math.min(modelFirst, modelLast);
                int end = Math.max(modelFirst, modelLast);
                for (int i = start; i <= end; i++) {
                    if (Thread.currentThread().isInterrupted())
                        return;
                    if (i < 0 || i >= products.size())
                        continue;
                    ModelProduct p = products.get(i);
                    if (p == null)
                        continue;
                    if (p.getCachedIcon() != null)
                        continue;

                    // Usar caché LRU si disponible
                    ImageIcon cached = ICON_CACHE.get(p.getProductId());
                    ImageIcon icon = cached != null ? cached : loadFirstImage(p.getProductId());
                    if (icon != null) {
                        p.setCachedIcon(icon);
                        ICON_CACHE.put(p.getProductId(), icon);
                        final int modelRowIndex = i;
                        SwingUtilities.invokeLater(() -> {
                            if (table != null && table.isDisplayable()) {
                                try {
                                    int viewRow = table.convertRowIndexToView(modelRowIndex);
                                    if (viewRow != -1) {
                                        java.awt.Rectangle rect = table.getCellRect(viewRow, 2, true);
                                        table.repaint(rect);
                                    }
                                } catch (Exception ignore) {
                                }
                            }
                        });
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error en lazy load: " + ex.getMessage());
            }
        });
        loadingThread.setName("ImageLoader-Lazy");
        loadingThread.setDaemon(true);
        loadingThread.start();
    }

    public static void clearCache() {
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
        throttleScheduled = false;
        // No cerrar permanentemente el executor; solo limpiar caches y permitir reuso
        // Si se desea reiniciar, se hará automáticamente con ensureExecutor() en
        // próxima tarea
    }

    public static ImageIcon loadFirstImage(int productId) {
        // Consulta simple para obtener la primera imagen encontrada
        String sql = "SELECT imagen FROM producto_variantes WHERE id_producto = ? AND imagen IS NOT NULL AND imagen != '' LIMIT 1";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("imagen");
                    if (bytes != null && bytes.length > 0) {
                        // Procesar imagen
                        try {
                            java.awt.Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                            if (img != null) {
                                // Escalar a 40px alto
                                int h = 40;
                                int w = (int) (img.getWidth(null) * ((double) h / img.getHeight(null)));
                                return new ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_AREA_AVERAGING));
                            }
                        } catch (Exception ex) {
                            System.err.println("Error procesando imagen ID " + productId + ": " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Solo loguear si no es interrupción
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("Error SQL imagen ID " + productId + ": " + e.getMessage());
            }
        }
        return null;
    }

    public static ImageIcon loadLargeImage(int productId) {
        return loadLargeImage(productId, 240);
    }

    public static ImageIcon loadLargeImage(int productId, int targetHeight) {
        // Warning: This separate cache key strategy is simple but might duplicate
        // memory if many diff sizes used.
        // For now it is fine as we only use 240 and 500.
        ImageIcon cached = LARGE_ICON_CACHE.get(productId * 1000 + targetHeight);
        if (cached != null)
            return cached;

        String sql = "SELECT imagen FROM producto_variantes WHERE id_producto = ? AND imagen IS NOT NULL AND imagen != '' LIMIT 1";
        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("imagen");
                    if (bytes != null && bytes.length > 0) {
                        java.awt.Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                        if (img != null) {
                            int h = targetHeight;
                            int w = (int) (img.getWidth(null) * ((double) h / img.getHeight(null)));
                            ImageIcon icon = new ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
                            LARGE_ICON_CACHE.put(productId * 1000 + targetHeight, icon);
                            return icon;
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * Renderer optimizado que usa el icono cacheado en el objeto ModelProduct
     */
    public static class OptimizedProductRenderer extends javax.swing.table.DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // Llamar al super para configurar colores básicos y selección
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setText("");
            setIcon(null);

            if (value instanceof ModelProduct) {
                ModelProduct product = (ModelProduct) value;

                // Usar imagen ya cargada en el objeto
                if (product.getCachedIcon() != null) {
                    setIcon(product.getCachedIcon());
                }

                // Texto con HTML para formato
                setText("<html><b>" + product.getName() + "</b><br>" +
                        "<small>Código: " + (product.getModelCode() != null ? product.getModelCode() : "N/A")
                        + "</small></html>");
            } else if (value != null) {
                setText(value.toString());
            }

            return this;
        }
    }

    public static class TablePerformanceOptimizer {
        public static void optimizeTable(JTable table) {
            // Implementación vacía para compatibilidad
        }
    }

    public static class ImagePreviewHandler {
        public static void attach(JTable table) {
            if (table == null)
                return;
            Object attached = table.getClientProperty("_imgPreviewAttached");
            if (attached instanceof Boolean && (Boolean) attached)
                return;
            table.putClientProperty("_imgPreviewAttached", true);
            ensureExecutor();
            final javax.swing.JWindow preview = new javax.swing.JWindow();
            final javax.swing.JLabel label = new javax.swing.JLabel();
            label.setOpaque(true);
            label.setBackground(new java.awt.Color(250, 250, 250));
            label.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new java.awt.Color(60, 60, 60), 1, true),
                    javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6)));
            preview.getContentPane().add(label);
            preview.pack();
            final int imageColumn = 2;
            final int showDelayMs = 120;
            final javax.swing.Timer showTimer = new javax.swing.Timer(showDelayMs, null);
            showTimer.setRepeats(false);
            final int[] hoverProductId = new int[] { 0 };

            table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row < 0 || col != imageColumn) {
                        showTimer.stop();
                        preview.setVisible(false);
                        return;
                    }
                    try {
                        int modelRow = table.convertRowIndexToModel(row);
                        Object val = table.getModel().getValueAt(modelRow, imageColumn);
                        if (!(val instanceof ModelProduct)) {
                            showTimer.stop();
                            preview.setVisible(false);
                            return;
                        }
                        ModelProduct p = (ModelProduct) val;
                        if (p == null || p.getCachedIcon() == null) {
                            showTimer.stop();
                            preview.setVisible(false);
                            return;
                        }
                        java.awt.Rectangle cellRect = table.getCellRect(row, imageColumn, true);
                        int relX = e.getPoint().x - cellRect.x;
                        int iconW = p.getCachedIcon().getIconWidth();
                        if (relX > iconW + 10) {
                            showTimer.stop();
                            preview.setVisible(false);
                            return;
                        }
                        if (hoverProductId[0] != p.getProductId()) {
                            hoverProductId[0] = p.getProductId();
                            for (java.awt.event.ActionListener l : showTimer.getActionListeners())
                                showTimer.removeActionListener(l);
                            showTimer.addActionListener(ev -> {
                                ImageIcon large = LARGE_ICON_CACHE.get(p.getProductId());
                                if (large == null) {
                                    IMAGE_EXECUTOR.submit(() -> {
                                        ImageIcon loaded = loadLargeImage(p.getProductId());
                                        if (loaded != null) {
                                            javax.swing.SwingUtilities.invokeLater(() -> {
                                                label.setIcon(loaded);
                                                preview.pack();
                                                java.awt.GraphicsConfiguration gc = table.getGraphicsConfiguration();
                                                java.awt.Rectangle bounds = gc != null ? gc.getBounds()
                                                        : new java.awt.Rectangle(
                                                                java.awt.Toolkit.getDefaultToolkit().getScreenSize());
                                                int margin = 16;
                                                int px = e.getXOnScreen() + margin;
                                                int py = e.getYOnScreen() + margin;
                                                int pw = preview.getWidth();
                                                int ph = preview.getHeight();
                                                int maxX = bounds.x + bounds.width - pw - 8;
                                                int maxY = bounds.y + bounds.height - ph - 8;
                                                int nx = Math.min(px, maxX);
                                                int ny = (py + ph > bounds.y + bounds.height - 8)
                                                        ? (e.getYOnScreen() - ph - margin)
                                                        : py;
                                                if (ny < bounds.y)
                                                    ny = bounds.y + 8;
                                                preview.setLocation(nx, ny);
                                                preview.setVisible(true);
                                            });
                                        }
                                    });
                                } else {
                                    label.setIcon(large);
                                    preview.pack();
                                    java.awt.GraphicsConfiguration gc = table.getGraphicsConfiguration();
                                    java.awt.Rectangle bounds = gc != null ? gc.getBounds()
                                            : new java.awt.Rectangle(
                                                    java.awt.Toolkit.getDefaultToolkit().getScreenSize());
                                    int margin = 16;
                                    int px = e.getXOnScreen() + margin;
                                    int py = e.getYOnScreen() + margin;
                                    int pw = preview.getWidth();
                                    int ph = preview.getHeight();
                                    int maxX = bounds.x + bounds.width - pw - 8;
                                    int maxY = bounds.y + bounds.height - ph - 8;
                                    int nx = Math.min(px, maxX);
                                    int ny = (py + ph > bounds.y + bounds.height - 8) ? (e.getYOnScreen() - ph - margin)
                                            : py;
                                    if (ny < bounds.y)
                                        ny = bounds.y + 8;
                                    preview.setLocation(nx, ny);
                                    preview.setVisible(true);
                                }
                            });
                            showTimer.restart();
                        } else {
                            if (preview.isVisible()) {
                                java.awt.GraphicsConfiguration gc = table.getGraphicsConfiguration();
                                java.awt.Rectangle bounds = gc != null ? gc.getBounds()
                                        : new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
                                int margin = 16;
                                int pw = preview.getWidth();
                                int ph = preview.getHeight();
                                int px = e.getXOnScreen() + margin;
                                int py = e.getYOnScreen() + margin;
                                int nx = Math.min(px, bounds.x + bounds.width - pw - 8);
                                int ny = (py + ph > bounds.y + bounds.height - 8) ? (e.getYOnScreen() - ph - margin)
                                        : py;
                                if (ny < bounds.y)
                                    ny = bounds.y + 8;
                                preview.setLocation(nx, ny);
                            }
                        }
                    } catch (Exception ignore) {
                        showTimer.stop();
                        preview.setVisible(false);
                    }
                }
            });

            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    showTimer.stop();
                    preview.setVisible(false);
                }

                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    showTimer.stop();
                    preview.setVisible(false);
                }
            });
        }
    }

    private static final java.util.Map<String, javax.swing.Icon> STRING_ICON_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static javax.swing.Icon getAllocatedImage(String path, int width, int height) {
        if (path == null || path.isEmpty())
            return null;
        String key = path + "_" + width + "_" + height;
        if (STRING_ICON_CACHE.containsKey(key)) {
            return STRING_ICON_CACHE.get(key);
        }

        try {
            // Attempt to load from file system
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                ImageIcon icon = new ImageIcon(path);
                Image img = icon.getImage();
                if (img != null) {
                    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    ImageIcon result = new ImageIcon(scaled);
                    STRING_ICON_CACHE.put(key, result);
                    return result;
                }
            } else {
                // Fallback: Check if it's a resource path
                java.net.URL url = ProductImageOptimizer.class.getResource(path);
                if (url == null && !path.startsWith("/")) {
                    url = ProductImageOptimizer.class.getResource("/" + path);
                }
                if (url != null) {
                    ImageIcon icon = new ImageIcon(url);
                    Image img = icon.getImage();
                    if (img != null) {
                        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                        ImageIcon result = new ImageIcon(scaled);
                        STRING_ICON_CACHE.put(key, result);
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading image from path: " + path + " -> " + e.getMessage());
        }
        return null;
    }

}
