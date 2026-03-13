package raven.clases.service;


import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import raven.application.form.other.buscador.dto.BusquedaCriteria;
import raven.application.form.other.buscador.dto.ProductoDTO;
import java.util.function.Consumer;
import raven.application.form.other.buscador.dto.VarianteDTO;
import raven.componentes.repository.ProductoRepository;

/**
 * Servicio de búsqueda de productos
 * Implementa lógica de negocio y optimizaciones de cache
 * 
 * Responsabilidades:
 * - Orquestar llamadas al repository
 * - Implementar cache para mejorar performance
 * - Manejar operaciones asíncronas
 * - Validar reglas de negocio
 * 
 * Optimizaciones:
 * - Cache LRU (Least Recently Used) para búsquedas frecuentes
 * - Carga asíncrona de variantes
 * - Pool de threads para operaciones paralelas
 * 
 * @author CrisDEV
 */

public class ProductoBusquedaService {
    private static final Logger LOGGER = Logger.getLogger(ProductoBusquedaService.class.getName());
    
    private final ProductoRepository repository;
    
    // Cache simple LRU para búsquedas recientes
    // En producción considera usar Caffeine o Guava Cache
    private final Map<String, CacheEntry> cacheResultados;
    private static final int CACHE_SIZE = 100;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    
    // Pool de threads para operaciones asíncronas
    private final ExecutorService executorService;
    
    /**
     * Constructor
     * @param repository Repository de productos
     */
    public ProductoBusquedaService(ProductoRepository repository) {
        this.repository = repository;
        
        // Inicializar cache LRU
        this.cacheResultados = Collections.synchronizedMap(
            new LinkedHashMap<String, CacheEntry>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > CACHE_SIZE;
                }
            }
        );
        
        // Pool de threads con tamaño óptimo
        // Core threads = número de procesadores
        // Max threads = 2x procesadores
        int cores = Runtime.getRuntime().availableProcessors();
        this.executorService = new ThreadPoolExecutor(
            cores,
            cores * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy() // Si está lleno, ejecuta en el thread llamador
        );
    }
    
    /**
     * Busca productos según criterios
     * Implementa cache para mejorar performance en búsquedas repetidas
     * 
     * @param criteria Criterios de búsqueda
     * @return Lista de productos
     */
    public List<ProductoDTO> buscarProductos(BusquedaCriteria criteria) {
        try {
            // Validación de términos: evitar búsquedas irrelevantes
            String texto = criteria.getTextoBusqueda();
            if (texto != null) {
                texto = texto.trim();
                if (texto.length() > 0 && texto.length() < 2) {
                    return java.util.Collections.emptyList();
                }
                // Evitar comodines manuales peligrosos
                if (texto.contains("%") || texto.contains("_")) {
                    texto = texto.replace("%", "").replace("_", "").trim();
                }
            }
            // Generar clave de cache
            String cacheKey = generarCacheKey(criteria);
            
            // Verificar cache
            CacheEntry cached = cacheResultados.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                LOGGER.log(Level.FINE, "Cache HIT para: {0}", cacheKey);
                return new ArrayList<>(cached.getProductos());
            }
            
            LOGGER.log(Level.FINE, "Cache MISS para: {0}", cacheKey);
            
            // Consultar repository
            List<ProductoDTO> productos = repository.buscarProductos(criteria);
            
            // Guardar en cache
            cacheResultados.put(cacheKey, new CacheEntry(productos));
            
            return productos;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar productos", e);
            throw new RuntimeException("Error al buscar productos: " + e.getMessage(), e);
        }
    }

    /**
     * Prefetch de productos en segundo plano
     */
    public void prefetchProductos(BusquedaCriteria criteria, Consumer<List<ProductoDTO>> callback) {
        executorService.submit(() -> {
            try {
                List<ProductoDTO> productos = repository.buscarProductos(criteria);
                if (callback != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> callback.accept(productos));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error en prefetch de productos", e);
            }
        });
    }
    
    /**
     * Carga las variantes de un producto de forma síncrona
     * 
     * @param producto Producto al que cargar variantes
     * @param bodega Bodega específica (null para todas)
     */
    public void cargarVariantes(ProductoDTO producto, String bodega) {
        cargarVariantes(producto, bodega, null);
    }

    public void cargarVariantes(ProductoDTO producto, String bodega, String tipo) {
        try {
            List<VarianteDTO> variantes = repository.cargarVariantes(
                producto.getIdProducto(), 
                bodega,
                tipo
            );
            producto.setVariantes(variantes);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar variantes", e);
            throw new RuntimeException("Error al cargar variantes: " + e.getMessage(), e);
        }
    }
    
    /**
     * Carga variantes de forma asíncrona
     * Útil para UI responsiva - no bloquea el hilo principal
     * 
     * @param producto Producto al que cargar variantes
     * @param bodega Bodega específica
     * @param callback Callback a ejecutar cuando termine la carga
     */
    public void cargarVariantesAsync(ProductoDTO producto, String bodega, 
                                     Consumer<List<VarianteDTO>> callback) {
        cargarVariantesAsync(producto, bodega, null, callback);
    }

    public void cargarVariantesAsync(ProductoDTO producto, String bodega, String tipo,
                                     Consumer<List<VarianteDTO>> callback) {
        
        executorService.submit(() -> {
            try {
                List<VarianteDTO> variantes = repository.cargarVariantes(
                    producto.getIdProducto(), 
                    bodega,
                    tipo
                );
                
                // Ejecutar callback en EDT (Event Dispatch Thread) si es Swing
                if (callback != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        producto.setVariantes(variantes);
                        callback.accept(variantes);
                    });
                }
                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error async al cargar variantes", e);
            }
        });
    }
    
    /**
     * Carga variantes para múltiples productos en batch
     * Optimización cuando se expanden varios productos a la vez
     * 
     * @param productos Lista de productos
     * @param bodega Bodega específica
     */
    public void cargarVariantesBatch(List<ProductoDTO> productos, String bodega) {
        cargarVariantesBatch(productos, bodega, null);
    }

    public void cargarVariantesBatch(List<ProductoDTO> productos, String bodega, String tipo) {
        try {
            // Extraer IDs
            List<Integer> ids = productos.stream()
                .map(ProductoDTO::getIdProducto)
                .toList();
            
            // Cargar todas las variantes en una sola query
            Map<Integer, List<VarianteDTO>> mapaVariantes = 
                repository.cargarVariantesBatch(ids, bodega, tipo);
            
            // Asignar variantes a cada producto
            productos.forEach(p -> {
                List<VarianteDTO> variantes = mapaVariantes.getOrDefault(
                    p.getIdProducto(), 
                    new ArrayList<>()
                );
                p.setVariantes(variantes);
            });
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar variantes en batch", e);
            throw new RuntimeException("Error al cargar variantes: " + e.getMessage(), e);
        }
    }
    
    /**
     * Limpia el cache de búsquedas
     */
    public void limpiarCache() {
        cacheResultados.clear();
        LOGGER.log(Level.INFO, "Cache limpiado");
    }
    
    /**
     * Cierra el servicio y libera recursos
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Genera una clave única para el cache basada en los criterios
     */
    private String generarCacheKey(BusquedaCriteria criteria) {
        return String.format("%s|%s|%s|%b|%d|%d|L%d|O%d|BID%s|EX%b|BM%b|BN%b|T%s|PID%s|EAN%s|SV%b",
            criteria.getTextoBusqueda(),
            criteria.getGenero(),
            criteria.getBodega(),
            criteria.getSoloConStock(),
            criteria.getIdCategoria(),
            criteria.getIdMarca(),
            criteria.getLimite() != null ? criteria.getLimite() : 0,
            criteria.getOffset() != null ? criteria.getOffset() : 0,
            String.valueOf(criteria.getIdBodega()),
            Boolean.TRUE.equals(criteria.getCoincidenciaExacta()),
            Boolean.TRUE.equals(criteria.getBuscarPorMarca()),
            Boolean.TRUE.equals(criteria.getBuscarPorNombre()),
            criteria.getTipo(),
            String.valueOf(criteria.getIdProducto()),
            criteria.getEan(),
            Boolean.TRUE.equals(criteria.getSoloConVariantes())
        );
    }
    
    /**
     * Entrada de cache con timestamp para expiración
     */
    private static class CacheEntry {
        private final List<ProductoDTO> productos;
        private final long timestamp;
        
        public CacheEntry(List<ProductoDTO> productos) {
            this.productos = new ArrayList<>(productos); // Copia defensiva
            this.timestamp = System.currentTimeMillis();
        }
        
        public List<ProductoDTO> getProductos() {
            return productos;
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL_MS;
        }
    }
    
    /**
     * Interfaz funcional para callbacks
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }
    
}
