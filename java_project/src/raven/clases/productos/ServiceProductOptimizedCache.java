package raven.clases.productos;

import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelBrand;
import raven.clases.comun.GenericPaginationService;

import java.sql.*;
import java.util.*;

/**
 * Servicio optimizado para la gestión de productos usando ProductPaginationAdapter
 * y procedimientos almacenados para máxima eficiencia.
 */
public class ServiceProductOptimizedCache {
    
    private final ProductPaginationAdapter paginationAdapter;
    
    // Caché simple para datos estáticos (categorías, marcas) para dropdowns/lookups
    private static final Map<Integer, ModelCategory> categoryCache = new HashMap<>();
    private static final Map<Integer, ModelBrand> brandCache = new HashMap<>();
    private static boolean staticDataLoaded = false;
    
    public ServiceProductOptimizedCache() {
        this.paginationAdapter = new ProductPaginationAdapter();
    }
    
    /**
     * Obtiene productos paginados usando el adaptador y procedimiento almacenado
     * Soporta todos los filtros disponibles.
     */
    public GenericPaginationService.PagedResult<ModelProduct> getPagedResult(
            int page, int pageSize, String searchTerm, Integer bodegaId, String stockTypeFilter,
            Integer categoryId, Integer brandId, Integer colorId, Integer sizeId) throws SQLException {
        
        return paginationAdapter.getProductsPagedAdvanced(
            searchTerm, 
            categoryId, 
            brandId,
            colorId,
            sizeId,
            bodegaId, 
            true, // activeOnly
            stockTypeFilter, 
            page, 
            pageSize
        );
    }

    /**
     * Método simplificado para compatibilidad (llama al método completo con filtros nulos)
     */
    public GenericPaginationService.PagedResult<ModelProduct> getPagedResult(
            int page, int pageSize, String searchTerm, Integer bodegaId, String stockTypeFilter) throws SQLException {
        return getPagedResult(page, pageSize, searchTerm, bodegaId, stockTypeFilter, null, null, null, null);
    }

    /**
     * Mantenido por compatibilidad, pero se recomienda usar getPagedResult
     */
    public List<ModelProduct> getProductosPaginados(int offset, int limit, String searchTerm, Integer bodegaId, String stockTypeFilter) throws SQLException {
        int page = (offset / limit) + 1;
        GenericPaginationService.PagedResult<ModelProduct> result = getPagedResult(page, limit, searchTerm, bodegaId, stockTypeFilter);
        return result.getData();
    }
    
    /**
     * Mantenido por compatibilidad. Nota: Puede ser ineficiente si no se usa junto con getPagedResult.
     * Se recomienda obtener el total del PagedResult directamente.
     */
    public int getTotalProductos(String searchTerm, Integer bodegaId, String stockTypeFilter) throws SQLException {
        // Asumimos un tamaño de página por defecto si no se conoce, solo para obtener el count
        // O mejor, reutilizamos la caché si es posible.
        // En este caso, hacemos una llamada ligera.
        // Pero dado que el SP devuelve el total, hacemos una llamada dummy a la página 1.
        GenericPaginationService.PagedResult<ModelProduct> result = getPagedResult(1, 1, searchTerm, bodegaId, stockTypeFilter);
        return (int) result.getTotalCount();
    }
    
    /**
     * Carga datos estáticos (categorías, marcas) para uso general
     */
    public synchronized void loadStaticDataIfNeeded() {
        if (!staticDataLoaded) {
            try {
                refreshStaticData();
                staticDataLoaded = true;
            } catch (SQLException e) {
                System.err.println("Error cargando datos estáticos: " + e.getMessage());
            }
        }
    }
    
    private void refreshStaticData() throws SQLException {
        categoryCache.clear();
        brandCache.clear();
        
        try (Connection conn = raven.controlador.principal.conexion.getInstance().createConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id_categoria, nombre, descripcion, activa FROM categorias WHERE activa = 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ModelCategory c = new ModelCategory(rs.getInt("id_categoria"), rs.getString("nombre"), rs.getString("descripcion"), rs.getBoolean("activa"));
                        categoryCache.put(c.getCategoryId(), c);
                    }
                }
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id_marca, nombre, descripcion, activa FROM marcas WHERE activa = 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ModelBrand b = new ModelBrand(rs.getInt("id_marca"), rs.getString("nombre"), rs.getString("descripcion"), rs.getBoolean("activa"));
                        brandCache.put(b.getBrandId(), b);
                    }
                }
            }
        }
    }
    
    public ModelCategory getCategory(int id) {
        loadStaticDataIfNeeded();
        return categoryCache.get(id);
    }
    
    public ModelBrand getBrand(int id) {
        loadStaticDataIfNeeded();
        return brandCache.get(id);
    }
    
    public void clearCache() {
        paginationAdapter.clearCache();
        staticDataLoaded = false;
    }
    
    // Métodos deprecados o no soportados eliminados para forzar el uso de la nueva lógica
}