package raven.clases.productos;

import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProductVariant;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Clase especializada para búsqueda ultra-rápida de productos por código de barras (EAN/SKU)
 * Implementa un sistema de caché en memoria con actualización automática para máxima eficiencia
 * 
 * @author Raven
 */
public class CacheBusquedaRapida {
    
    private static final Logger LOGGER = Logger.getLogger(CacheBusquedaRapida.class.getName());
    private static final int CACHE_REFRESH_INTERVAL_MINUTES = 5; // Intervalo de actualización de caché
    
    // Caché principal: código (EAN/SKU) -> datos del producto
    private final ConcurrentHashMap<String, ProductoCache> cacheCodigoProducto = new ConcurrentHashMap<>();

    // Caché secundaria: idVariante -> datos del producto
    private final ConcurrentHashMap<Integer, ProductoCache> cacheIdProducto = new ConcurrentHashMap<>();

    // Executor para actualización automática
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // Última actualización
    private volatile long ultimaActualizacion = 0;
    
    private static CacheBusquedaRapida instance;
    
    public static synchronized CacheBusquedaRapida getInstance() {
        if (instance == null) {
            instance = new CacheBusquedaRapida();
        }
        return instance;
    }
    
    // Constructor privado para singleton
    private CacheBusquedaRapida() {
        iniciarActualizacionAutomatica();
        cargarCacheInicial();
    }
    
    /**
     * Inicia el proceso de actualización automática de la caché
     */
    private void iniciarActualizacionAutomatica() {
        executor.scheduleAtFixedRate(this::actualizarCache, 
                                   0, CACHE_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Carga inicial de la caché
     */
    private void cargarCacheInicial() {
        try {
            actualizarCache();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en carga inicial de caché", e);
        }
    }
    
    /**
     * Actualiza completamente la caché desde la base de datos
     */
    public void actualizarCache() {
        try {
            LOGGER.info("🔄 Iniciando actualización de caché de productos...");
            long inicio = System.currentTimeMillis();
            
            // Limpiar cachés existentes
            cacheCodigoProducto.clear();
            cacheIdProducto.clear();
            
            // Cargar datos desde la base de datos
            String sql = """
                SELECT 
                    p.id_producto,
                    p.codigo_modelo,
                    pv.id_variante,
                    pv.ean,
                    pv.sku,
                    p.nombre,
                    c.nombre AS color_nombre,
                    t.numero AS talla_numero,
                    t.sistema AS talla_sistema,
                    p.precio_venta,
                    p.descripcion,
                    pv.disponible,
                    COALESCE(ib.Stock_par, 0) AS stock_par,
                    COALESCE(ib.Stock_caja, 0) AS stock_caja,
                    COALESCE(ib.stock_reservado, 0) AS stock_reservado,
                    p.pares_por_caja
                FROM producto_variantes pv
                INNER JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN colores c ON pv.id_color = c.id_color
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante
                    AND ib.activo = 1
                WHERE pv.disponible = 1 AND p.activo = 1
                """;
            
            try (Connection conn = conexion.getInstance().createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                int count = 0;
                while (rs.next()) {
                    ProductoCache producto = mapearResultSetACache(rs);
                    
                    // Agregar a caché por código
                    if (producto.getEan() != null && !producto.getEan().isEmpty()) {
                        cacheCodigoProducto.put(producto.getEan(), producto);
                    }
                    if (producto.getSku() != null && !producto.getSku().isEmpty() && !producto.getSku().equals(producto.getEan())) {
                        cacheCodigoProducto.put(producto.getSku(), producto);
                    }
                    
                    // Agregar a caché por ID
                    cacheIdProducto.put(producto.getIdVariante(), producto);
                    
                    count++;
                }
                
                ultimaActualizacion = System.currentTimeMillis();
                long duracion = ultimaActualizacion - inicio;
                LOGGER.info(String.format("✅ Caché actualizada exitosamente: %d productos en %d ms", count, duracion));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error actualizando caché de productos", e);
        }
    }
    
    /**
     * Mapea un ResultSet a un objeto ProductoCache
     */
    private ProductoCache mapearResultSetACache(ResultSet rs) throws SQLException {
        ProductoCache producto = new ProductoCache();
        
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCodigoModelo(rs.getString("codigo_modelo"));
        producto.setIdVariante(rs.getInt("id_variante"));
        producto.setEan(rs.getString("ean"));
        producto.setSku(rs.getString("sku"));
        producto.setNombre(rs.getString("nombre"));
        producto.setColorNombre(rs.getString("color_nombre"));
        producto.setTallaNumero(rs.getString("talla_numero"));
        producto.setTallaSistema(rs.getString("talla_sistema"));
        producto.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setDisponible(rs.getBoolean("disponible"));
        producto.setStockPar(rs.getInt("stock_par"));
        producto.setStockCaja(rs.getInt("stock_caja"));
        producto.setStockReservado(rs.getInt("stock_reservado"));
        producto.setParesPorCaja(rs.getInt("pares_por_caja"));
        
        return producto;
    }
    
    /**
     * Busca un producto por código (EAN o SKU) - Ultra rápido (O(1) en caché)
     */
    public ProductoCache buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        
        String codigoLimpio = codigo.trim();
        return cacheCodigoProducto.get(codigoLimpio);
    }
    
    /**
     * Busca un producto por ID de variante - Ultra rápido (O(1) en caché)
     */
    public ProductoCache buscarPorIdVariante(int idVariante) {
        return cacheIdProducto.get(idVariante);
    }
    
    /**
     * Obtiene stock actual de un producto por código
     */
    public int obtenerStockDisponiblePorCodigo(String codigo) {
        ProductoCache producto = buscarPorCodigo(codigo);
        if (producto == null) {
            return 0;
        }
        
        int stockTotal = producto.getStockPar();
        if (producto.getParesPorCaja() > 0) {
            stockTotal += producto.getStockCaja() * producto.getParesPorCaja();
        }
        
        return Math.max(0, stockTotal - producto.getStockReservado());
    }

    /**
     * Obtiene información de producto por código
     */
    public InfoProducto buscarInfoPorCodigo(String codigo) {
        ProductoCache producto = buscarPorCodigo(codigo);
        if (producto == null) {
            return null;
        }

        InfoProducto info = new InfoProducto();
        info.setIdProducto(producto.getIdProducto());
        info.setIdVariante(producto.getIdVariante());
        info.setNombre(producto.getNombre());
        info.setColor(producto.getColorNombre());
        info.setTalla(producto.getTallaNumero());
        info.setPrecio(producto.getPrecioVenta());
        info.setStockDisponible(obtenerStockDisponiblePorCodigo(codigo));
        info.setCodigoBarras(codigo);

        return info;
    }

    /**
     * Fuerza una actualización manual de la caché
     */
    public void actualizarCacheManual() {
        executor.submit(this::actualizarCache);
    }

    /**
     * Obtiene estadísticas de la caché
     */
    public CacheStats getEstadisticas() {
        return new CacheStats(
            cacheCodigoProducto.size(),
            cacheIdProducto.size(),
            ultimaActualizacion,
            System.currentTimeMillis() - ultimaActualizacion
        );
    }

    /**
     * Cierra el servicio de caché
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Clases internas para la caché
    public static class ProductoCache {
        private int idProducto;
        private String codigoModelo;
        private int idVariante;
        private String ean;
        private String sku;
        private String nombre;
        private String colorNombre;
        private String tallaNumero;
        private String tallaSistema;
        private java.math.BigDecimal precioVenta;
        private String descripcion;
        private boolean disponible;
        private int stockPar;
        private int stockCaja;
        private int stockReservado;
        private int paresPorCaja;

        // Getters y setters
        public int getIdProducto() { return idProducto; }
        public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

        public String getCodigoModelo() { return codigoModelo; }
        public void setCodigoModelo(String codigoModelo) { this.codigoModelo = codigoModelo; }

        public int getIdVariante() { return idVariante; }
        public void setIdVariante(int idVariante) { this.idVariante = idVariante; }

        public String getEan() { return ean; }
        public void setEan(String ean) { this.ean = ean; }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getColorNombre() { return colorNombre; }
        public void setColorNombre(String colorNombre) { this.colorNombre = colorNombre; }

        public String getTallaNumero() { return tallaNumero; }
        public void setTallaNumero(String tallaNumero) { this.tallaNumero = tallaNumero; }

        public String getTallaSistema() { return tallaSistema; }
        public void setTallaSistema(String tallaSistema) { this.tallaSistema = tallaSistema; }

        public java.math.BigDecimal getPrecioVenta() { return precioVenta; }
        public void setPrecioVenta(java.math.BigDecimal precioVenta) { this.precioVenta = precioVenta; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public boolean isDisponible() { return disponible; }
        public void setDisponible(boolean disponible) { this.disponible = disponible; }

        public int getStockPar() { return stockPar; }
        public void setStockPar(int stockPar) { this.stockPar = stockPar; }

        public int getStockCaja() { return stockCaja; }
        public void setStockCaja(int stockCaja) { this.stockCaja = stockCaja; }

        public int getStockReservado() { return stockReservado; }
        public void setStockReservado(int stockReservado) { this.stockReservado = stockReservado; }

        public int getParesPorCaja() { return paresPorCaja; }
        public void setParesPorCaja(int paresPorCaja) { this.paresPorCaja = paresPorCaja; }
    }

    public static class InfoProducto {
        private int idProducto;
        private int idVariante;
        private String nombre;
        private String color;
        private String talla;
        private java.math.BigDecimal precio;
        private int stockDisponible;
        private String codigoBarras;

        // Getters y setters
        public int getIdProducto() { return idProducto; }
        public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

        public int getIdVariante() { return idVariante; }
        public void setIdVariante(int idVariante) { this.idVariante = idVariante; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getTalla() { return talla; }
        public void setTalla(String talla) { this.talla = talla; }

        public java.math.BigDecimal getPrecio() { return precio; }
        public void setPrecio(java.math.BigDecimal precio) { this.precio = precio; }

        public int getStockDisponible() { return stockDisponible; }
        public void setStockDisponible(int stockDisponible) { this.stockDisponible = stockDisponible; }

        public String getCodigoBarras() { return codigoBarras; }
        public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    }

    public static class CacheStats {
        private final int tamanoCodigo;
        private final int tamanoId;
        private final long ultimaActualizacion;
        private final long tiempoDesdeUltimaActualizacionMs;

        public CacheStats(int tamanoCodigo, int tamanoId, long ultimaActualizacion, long tiempoDesdeUltimaActualizacionMs) {
            this.tamanoCodigo = tamanoCodigo;
            this.tamanoId = tamanoId;
            this.ultimaActualizacion = ultimaActualizacion;
            this.tiempoDesdeUltimaActualizacionMs = tiempoDesdeUltimaActualizacionMs;
        }

        public int getTamanoCodigo() { return tamanoCodigo; }
        public int getTamanoId() { return tamanoId; }
        public long getUltimaActualizacion() { return ultimaActualizacion; }
        public long getTiempoDesdeUltimaActualizacionMs() { return tiempoDesdeUltimaActualizacionMs; }
    }
}