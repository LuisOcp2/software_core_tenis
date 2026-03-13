package raven.clases.productos;

import raven.controlador.productos.ModelProduct;
import raven.clases.comun.GenericPaginationService;

import java.util.List;

/**
 * Adaptador para convertir resultados del servicio optimizado al formato esperado por la interfaz
 */
public class ProductResultAdapter {
    
    /**
     * Convierte una lista de productos y conteo total al formato PagedResult esperado por la interfaz
     */
    public static GenericPaginationService.PagedResult<ModelProduct> adaptToPagedResult(
            List<ModelProduct> products, 
            int totalCount, 
            int currentPage, 
            int pageSize) {
        
        return new GenericPaginationService.PagedResult<>(
            products, 
            totalCount, 
            currentPage, 
            pageSize, 
            true // Indicar que proviene de caché
        );
    }
}