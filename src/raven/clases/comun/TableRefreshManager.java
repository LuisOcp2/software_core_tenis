package raven.clases.comun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Gestor inteligente para recarga de tablas.
 * Implementa caché, actualizaciones en segundo plano y actualización diferencial.
 */
public class TableRefreshManager<T> {

    private final JTable table;
    private final Supplier<List<T>> dataFetcher;
    private final Function<T, Object[]> rowConverter;
    private final Function<T, Object> idExtractor;
    
    // Cache
    private List<T> cachedData;
    private long lastUpdateTimestamp;
    private static final long CACHE_DURATION_MS = 30000; // 30 segundos de caché
    
    // Estado
    private SwingWorker<List<T>, Void> currentWorker;
    private boolean isUpdating = false;
    private java.util.function.Consumer<List<T>> onUpdateListener;

    public TableRefreshManager(JTable table, Function<T, Object[]> rowConverter, Function<T, Object> idExtractor) {
        this(table, null, rowConverter, idExtractor);
    }

    public TableRefreshManager(JTable table, Supplier<List<T>> dataFetcher, 
                             Function<T, Object[]> rowConverter, Function<T, Object> idExtractor) {
        this.table = table;
        this.dataFetcher = dataFetcher;
        this.rowConverter = rowConverter;
        this.idExtractor = idExtractor;
    }

    // Callbacks de estado
    private Runnable onStartLoading;
    private Runnable onFinishLoading;

    public void setLoadingHandlers(Runnable onStart, Runnable onFinish) {
        this.onStartLoading = onStart;
        this.onFinishLoading = onFinish;
    }

    public void setOnUpdateListener(java.util.function.Consumer<List<T>> listener) {
        this.onUpdateListener = listener;
    }

    /**
     * Solicita una recarga de datos usando el fetcher configurado.
     * @param force Si es true, ignora la caché.
     */
    public void reload(boolean force) {
        reload(this.dataFetcher, force);
    }

    /**
     * Solicita una recarga de datos con un fetcher específico (útil para filtros dinámicos).
     */
    public void reload(Supplier<List<T>> customFetcher, boolean force) {
        if (isUpdating) {
            return; // Ya hay una actualización en curso
        }
        
        Supplier<List<T>> fetcherToUse = customFetcher != null ? customFetcher : this.dataFetcher;
        if (fetcherToUse == null) {
            throw new IllegalStateException("No data fetcher configured");
        }

        // Verificar caché (solo si usamos el fetcher default, para filtros dinámicos es complejo cachear sin clave)
        // Si se pasa un customFetcher, asumimos que los parámetros cambiaron, así que NO usamos caché simple.
        // A menos que implementemos un mapa de caches por parámetros.
        // Para simplificar: si customFetcher != default, ignoramos caché o forzamos recarga.
        boolean isDefaultFetcher = (customFetcher == null || customFetcher == this.dataFetcher);
        
        if (isDefaultFetcher && !force && cachedData != null && (System.currentTimeMillis() - lastUpdateTimestamp < CACHE_DURATION_MS)) {
            updateTableUI(cachedData);
            return;
        }

        isUpdating = true;
        
        if (onStartLoading != null) {
            SwingUtilities.invokeLater(onStartLoading);
        }

        currentWorker = new SwingWorker<List<T>, Void>() {
            @Override
            protected List<T> doInBackground() throws Exception {
                long start = System.currentTimeMillis();
                List<T> data = fetcherToUse.get();
                long duration = System.currentTimeMillis() - start;
                
                // Monitor de rendimiento
                PerformanceMonitor.logExecutionTime("Carga de datos tabla " + (table.getName() != null ? table.getName() : ""), duration);
                
                return data;
            }

            @Override
            protected void done() {
                try {
                    List<T> data = get();
                    cachedData = data;
                    lastUpdateTimestamp = System.currentTimeMillis();
                    updateTableUI(data);
                    
                    // Verificar memoria después de actualización grande
                    if (data.size() > 100) {
                        PerformanceMonitor.logMemoryUsage();
                    }
                    
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger(TableRefreshManager.class.getName()).log(
                        java.util.logging.Level.WARNING, "Error al recargar tabla", e);
                } finally {
                    isUpdating = false;
                    if (onFinishLoading != null) {
                        onFinishLoading.run();
                    }
                }
            }
        };
        currentWorker.execute();
    }

    /**
     * Actualiza la UI de la tabla minimizando cambios (Diffing).
     */
    private void updateTableUI(List<T> newData) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        
        // Mapa de ID -> Fila actual en el modelo
        Map<Object, Integer> currentRows = new HashMap<>();
        int idColumnIndex = 0; // Asumimos ID en columna 0 por defecto, pero usamos idExtractor para la lógica
        
        // Nota: Para mapear filas visuales a objetos, necesitamos que la tabla tenga el ID en alguna columna
        // O confiamos ciegamente en el idExtractor sobre los datos nuevos.
        // Estrategia: Leer la columna 0 del modelo actual como ID.
        
        for (int i = 0; i < model.getRowCount(); i++) {
            Object id = model.getValueAt(i, 0); // Asumimos ID en columna 0
            if (id != null) {
                currentRows.put(id.toString(), i); // Convertimos a String para asegurar consistencia
            }
        }

        Set<Object> processedIds = new HashSet<>();
        List<Object[]> rowsToAdd = new ArrayList<>();

        // 1. Identificar actualizaciones y nuevos registros
        for (T item : newData) {
            Object idObj = idExtractor.apply(item);
            String id = idObj.toString();
            Object[] newRowData = rowConverter.apply(item);
            
            if (currentRows.containsKey(id)) {
                // Actualizar fila existente
                int rowIndex = currentRows.get(id);
                updateRowIfChanged(model, rowIndex, newRowData);
                processedIds.add(id);
            } else {
                // Nuevo registro
                rowsToAdd.add(newRowData);
            }
        }

        // 2. Eliminar filas que ya no existen
        // Recorremos hacia atrás para no afectar índices
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            Object id = model.getValueAt(i, 0);
            if (id != null && !processedIds.contains(id.toString())) {
                // Verificar si el ID estaba en los datos nuevos (para evitar borrar si processedIds está incompleto por error)
                // Si el ID de la tabla no está en processedIds, significa que no vino en newData.
                
                // Un caso especial: Si newData está vacío (filtro devolvió 0), borramos todo.
                // Si newData tiene datos, borramos los que faltan.
                model.removeRow(i);
            }
        }

        // 3. Agregar nuevas filas
        for (Object[] row : rowsToAdd) {
            model.addRow(row);
        }
        
        // Forzar repintado si hubo cambios
        if (!rowsToAdd.isEmpty() || model.getRowCount() != currentRows.size()) {
           // table.repaint(); // DefaultTableModel ya dispara eventos
        }
        
        if (onUpdateListener != null) {
            onUpdateListener.accept(newData);
        }
    }

    private void updateRowIfChanged(DefaultTableModel model, int rowIndex, Object[] newData) {
        for (int col = 0; col < newData.length; col++) {
            if (col < model.getColumnCount()) {
                Object oldVal = model.getValueAt(rowIndex, col);
                Object newVal = newData[col];
                
                // Comparación null-safe
                boolean changed = (oldVal == null && newVal != null) || 
                                (oldVal != null && !oldVal.equals(newVal));
                                
                if (changed) {
                    model.setValueAt(newVal, rowIndex, col);
                }
            }
        }
    }
    
    public void clearCache() {
        cachedData = null;
    }
}

