package raven.clases.principal;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import raven.clases.admin.UserSession;
import raven.controlador.principal.conexion;

/**
 * Servicio para validación de stock en bodegas.
 * 
 * Principio aplicado: Single Responsibility Principle (SRP)
 * - Esta clase tiene una única responsabilidad: validar disponibilidad de stock.
 * - Separa la lógica de negocio de la capa de presentación (UI).
 * 
 * Beneficios:
 * - Reutilizable desde cualquier parte de la aplicación
 * - Fácil de testear unitariamente
 * - Centraliza la lógica de validación de stock
 * 
 * @author Sistema
 * @version 1.0
 */
public class StockValidationService {
    
    // Singleton instance - Thread-safe con inicialización temprana
    private static final StockValidationService INSTANCE = new StockValidationService();
    
    // Query optimizada para verificar existencia de stock
    private static final String QUERY_VERIFICAR_STOCK = 
        "SELECT COUNT(*) FROM inventario_bodega ib " +
        "JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
        "WHERE ib.id_bodega = ? " +
        "AND ib.activo = 1 " +
        "AND pv.disponible = 1 " +
        "AND (COALESCE(ib.Stock_par, 0) > 0 OR COALESCE(ib.Stock_caja, 0) > 0)";
    
    /**
     * Constructor privado para Singleton.
     */
    private StockValidationService() {
        // Constructor privado - Patrón Singleton
    }
    
    /**
     * Obtiene la instancia única del servicio.
     * 
     * @return Instancia del servicio de validación
     */
    public static StockValidationService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Verifica si una bodega tiene stock disponible para facturación.
     * 
     * @param idBodega ID de la bodega a verificar
     * @return true si hay stock suficiente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean tieneStockDisponible(int idBodega) throws SQLException {
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(QUERY_VERIFICAR_STOCK)) {
            
            ps.setInt(1, idBodega);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Verifica si la bodega del usuario actual tiene stock disponible.
     * Usa la bodega configurada en la sesión del usuario.
     * 
     * @return true si hay stock suficiente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean tieneStockEnBodegaActual() throws SQLException {
        int idBodega = UserSession.getInstance().getIdBodegaUsuario();
        // Validación defensiva
        if (idBodega <= 0) {
            throw new IllegalStateException("No hay una bodega válida configurada para el usuario actual");
        }
        
        return tieneStockDisponible(idBodega);
    }
    
    /**
     * Resultado de la validación con mensaje descriptivo.
     * Útil para mostrar información más detallada al usuario.
     */
    public static class StockValidationResult {
        private final boolean stockDisponible;
        private final String mensaje;
        
        public StockValidationResult(boolean stockDisponible, String mensaje) {
            this.stockDisponible = stockDisponible;
            this.mensaje = mensaje;
        }
        
        public boolean isStockDisponible() {
            return stockDisponible;
        }
        
        public String getMensaje() {
            return mensaje;
        }
    }
    
    /**
     * Valida el stock y retorna un resultado detallado.
     * 
     * @param idBodega ID de la bodega a validar
     * @return Objeto con el resultado de la validación y mensaje descriptivo
     */
    public StockValidationResult validarStockConDetalle(int idBodega) {
        try {
            boolean tieneStock = tieneStockDisponible(idBodega);
            
            if (tieneStock) {
                return new StockValidationResult(true, "Stock disponible para facturación");
            } else {
                return new StockValidationResult(false, 
                    "La bodega no tiene stock suficiente para facturar");
            }
            
        } catch (SQLException ex) {
            // Log del error para debugging
            System.err.println("Error al validar stock: " + ex.getMessage());
            return new StockValidationResult(false, 
                "Error al verificar el stock. Por favor, intente nuevamente.");
        }
    }
}