/*
 * Clase para encapsular información de variantes de productos
 * CORREGIDO: Implementación limpia y funcional
 */
package raven.clases.comercial;

/**
 * Contiene información esencial de una variante de producto
 * Utilizada principalmente en procesos de devolución y consultas
 * 
 * @author Equipo de Desarrollo
 */
public class VarianteInfo {
    
    // ====================================================================
    // ATRIBUTOS INMUTABLES
    // ====================================================================
    private final int idVariante;
    private final String sku;
    private final String ean;
    private final String talla;
    private final String color;
    private final int stockPares;
    private final int stockCajas;
    private final Double precio;
    private final boolean tieneImagen;
    
    // ====================================================================
    // CONSTRUCTOR
    // ====================================================================
    
    /**
     * Constructor principal para crear información de variante
     * 
     * @param idVariante ID único de la variante
     * @param sku Código SKU de la variante
     * @param ean Código EAN de la variante  
     * @param talla Talla del producto
     * @param color Color del producto
     * @param stockPares Stock disponible en pares
     * @param stockCajas Stock disponible en cajas
     * @param precio Precio de venta actual
     * @param tieneImagen Si tiene imagen asociada
     */
    public VarianteInfo(int idVariante, String sku, String ean, String talla, String color,
            int stockPares, int stockCajas, Double precio, boolean tieneImagen) {
        
        this.idVariante = idVariante;
        this.sku = sku;
        this.ean = ean;
        this.talla = talla;
        this.color = color;
        this.stockPares = Math.max(0, stockPares);
        this.stockCajas = Math.max(0, stockCajas);
        this.precio = precio;
        this.tieneImagen = tieneImagen;
    }
    
    // ====================================================================
    // GETTERS
    // ====================================================================
    
    public int getIdVariante() { return idVariante; }
    public String getSku() { return sku; }
    public String getEan() { return ean; }
    public String getTalla() { return talla; }
    public String getColor() { return color; }
    public int getStockPares() { return stockPares; }
    public int getStockCajas() { return stockCajas; }
    public Double getPrecio() { return precio; }
    public boolean isTieneImagen() { return tieneImagen; }
    
    // ====================================================================
    // MÉTODOS CALCULADOS
    // ====================================================================
    
    /**
     * Calcula el total de pares disponibles (pares + cajas * 24)
     */
    public int getTotalPares() {
        return stockPares + (stockCajas * 24);
    }
    
    /**
     * Verifica si tiene stock disponible
     */
    public boolean tieneStock() {
        return getTotalPares() > 0;
    }
    
    /**
     * Verifica si es una variante válida (tiene ID > 0)
     */
    public boolean esVarianteValida() {
        return idVariante > 0;
    }
    
    /**
     * Verifica si tiene precio válido
     */
    public boolean tienePrecioValido() {
        return precio != null && precio > 0;
    }
    
    // ====================================================================
    // MÉTODOS DE PRESENTACIÓN
    // ====================================================================
    
    /**
     * Obtiene descripción completa de la variante
     */
    public String getDescripcionCompleta() {
        StringBuilder desc = new StringBuilder();
        
        if (talla != null && !talla.trim().isEmpty()) {
            desc.append("Talla: ").append(talla);
        }
        
        if (color != null && !color.trim().isEmpty()) {
            if (desc.length() > 0) {
                desc.append(" - ");
            }
            desc.append("Color: ").append(color);
        }
        
        if (desc.length() == 0) {
            desc.append("Variante sin especificaciones");
        }
        
        return desc.toString();
    }
    
    /**
     * Obtiene código identificador principal (SKU o EAN)
     */
    public String getCodigoIdentificador() {
        if (sku != null && !sku.trim().isEmpty()) {
            return sku;
        }
        if (ean != null && !ean.trim().isEmpty()) {
            return ean;
        }
        return "VAR-" + idVariante;
    }
    
    /**
     * Obtiene información de stock formateada
     */
    public String getInfoStockFormateada() {
        if (stockCajas > 0 && stockPares > 0) {
            return String.format("%d pares + %d cajas (%d total)",
                    stockPares, stockCajas, getTotalPares());
        } else if (stockCajas > 0) {
            return String.format("%d cajas (%d pares)",
                    stockCajas, stockCajas * 24);
        } else if (stockPares > 0) {
            return String.format("%d pares", stockPares);
        } else {
            return "Sin stock";
        }
    }
    
    /**
     * Obtiene precio formateado
     */
    public String getPrecioFormateado() {
        if (precio != null) {
            return String.format("$%.2f", precio);
        }
        return "Sin precio";
    }
    
    // ====================================================================
    // MÉTODOS DE COMPARACIÓN
    // ====================================================================
    
    @Override
    public String toString() {
        return String.format("VarianteInfo{id=%d, sku='%s', talla='%s', color='%s', stock=%d pares, precio=%.2f}",
                idVariante, 
                sku != null ? sku : "N/A", 
                talla != null ? talla : "N/A", 
                color != null ? color : "N/A", 
                getTotalPares(), 
                precio != null ? precio : 0.0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        VarianteInfo that = (VarianteInfo) obj;
        return idVariante == that.idVariante;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(idVariante);
    }
    
    // ====================================================================
    // MÉTODOS ESTÁTICOS DE UTILIDAD
    // ====================================================================
    
    /**
     * Crea una VarianteInfo vacía para productos sin variantes
     */
    public static VarianteInfo crearVarianteVacia() {
        return new VarianteInfo(0, null, null, null, null, 0, 0, 0.0, false);
    }
    
    /**
     * Crea una VarianteInfo desde datos básicos del producto
     */
    public static VarianteInfo desdeProductoBase(int idProducto, String codigoModelo, 
            int stockPares, int stockCajas, double precio) {
        
        return new VarianteInfo(
            0, // Sin variante específica
            "PROD-" + idProducto,
            null,
            null, // Sin talla específica
            null, // Sin color específico
            stockPares,
            stockCajas,
            precio,
            false
        );
    }
    
    /**
     * Valida que una VarianteInfo tenga datos mínimos válidos
     */
    public static boolean esValida(VarianteInfo variante) {
        return variante != null && 
               (variante.idVariante > 0 || 
                (variante.sku != null && !variante.sku.trim().isEmpty()));
    }
}