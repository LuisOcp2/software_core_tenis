package raven.controlador.productos;


import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * Modelo de datos para Inventario por Bodega
 * ════════════════════════════════════════════════════════════════════════════
 * 
 * Representa el stock de una variante específica de producto en una bodega determinada
 * 
 * Principios aplicados:
 * - Encapsulación: Todos los campos privados con getters/setters
 * - JavaBean Convention: Cumple con estándar JavaBean para frameworks
 * - Inmutabilidad parcial: Campos finales donde corresponde
 * 
 * @author Sistema de Gestión de Inventarios
 * @version 2.0
 */
public class ModelInventarioBodega {
    
    // ══════════════════════════════════════════════════════════════════════════
    // CAMPOS DE INVENTARIO_BODEGA
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * ID único del registro de inventario en bodega
     */
    private int idInventarioBodega;
    
    /**
     * ID de la bodega donde está el inventario
     */
    private int idBodega;
    
    /**
     * ID de la variante del producto (talla/color)
     */
    private int idVariante;
    
    /**
     * Cantidad de stock en pares individuales
     */
    private int stockPar;
    
    /**
     * Cantidad de stock en cajas completas
     */
    private int stockCaja;
    
    /**
     * Cantidad de stock reservado (ordenes pendientes, etc)
     */
    private int stockReservado;
    
    /**
     * Fecha del último movimiento de inventario
     */
    private LocalDateTime fechaUltimoMovimiento;
    
    /**
     * Fecha de última actualización del registro
     */
    private LocalDateTime fechaActualizacion;
    
    /**
     * Ubicación física específica en la bodega (ej: "Estante A-5")
     */
    private String ubicacionEspecifica;
    
    /**
     * Estado activo del registro de inventario
     */
    private boolean activo;
    
    // ══════════════════════════════════════════════════════════════════════════
    // CAMPOS ADICIONALES DEL JOIN (producto_variantes y productos)
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Talla del producto (de producto_variantes)
     */
    private String talla;
    
    /**
     * Color del producto (de producto_variantes)
     */
    private String color;
    
    /**
     * SKU - Stock Keeping Unit (de producto_variantes)
     */
    private String sku;
    
    /**
     * EAN - European Article Number / Código de barras (de producto_variantes)
     */
    private String ean;
    
    /**
     * Precio de venta de la variante
     */
    private BigDecimal precioVenta;
    
    /**
     * Precio de compra de la variante
     */
    private BigDecimal precioCompra;
    
    /**
     * Nombre del producto (de tabla productos)
     */
    private String nombreProducto;
    
    /**
     * ID del producto padre (de tabla productos)
     */
    private int idProducto;
    
    // ══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Constructor vacío para frameworks y mapeo de ResultSet
     */
    public ModelInventarioBodega() {
    }
    
    /**
     * Constructor completo para inicialización rápida
     * 
     * @param idInventarioBodega ID del inventario
     * @param idBodega ID de la bodega
     * @param idVariante ID de la variante
     * @param stockPar Stock en pares
     * @param stockCaja Stock en cajas
     * @param stockReservado Stock reservado
     */
    public ModelInventarioBodega(int idInventarioBodega, int idBodega, int idVariante, 
                                 int stockPar, int stockCaja, int stockReservado) {
        this.idInventarioBodega = idInventarioBodega;
        this.idBodega = idBodega;
        this.idVariante = idVariante;
        this.stockPar = stockPar;
        this.stockCaja = stockCaja;
        this.stockReservado = stockReservado;
        this.activo = true; // Por defecto activo
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Calcula el stock disponible real (stock total - stock reservado)
     * 
     * @return Stock disponible para venta
     */
    public int getStockDisponiblePares() {
        return Math.max(0, stockPar - stockReservado);
    }
    
    /**
     * Calcula el stock disponible en cajas
     * 
     * @return Stock de cajas disponibles
     */
    public int getStockDisponibleCajas() {
        return stockCaja;
    }
    
    /**
     * Verifica si hay stock disponible (pares o cajas)
     * 
     * @return true si hay stock disponible
     */
    public boolean tieneStockDisponible() {
        return getStockDisponiblePares() > 0 || getStockDisponibleCajas() > 0;
    }
    
    /**
     * Calcula el stock total en pares (considerando pares en cajas si aplica)
     * Asume que cada caja tiene un número de pares estándar
     * 
     * @param paresPorCaja Número de pares por caja
     * @return Stock total en pares
     */
    public int getStockTotalEnPares(int paresPorCaja) {
        return stockPar + (stockCaja * paresPorCaja);
    }
    
    /**
     * Verifica si el stock está por debajo de un nivel mínimo
     * 
     * @param stockMinimo Nivel mínimo de stock
     * @return true si está por debajo del mínimo
     */
    public boolean estaEnStockMinimo(int stockMinimo) {
        return stockPar < stockMinimo;
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ══════════════════════════════════════════════════════════════════════════
    
    public int getIdInventarioBodega() {
        return idInventarioBodega;
    }
    
    public void setIdInventarioBodega(int idInventarioBodega) {
        this.idInventarioBodega = idInventarioBodega;
    }
    
    public int getIdBodega() {
        return idBodega;
    }
    
    public void setIdBodega(int idBodega) {
        this.idBodega = idBodega;
    }
    
    public int getIdVariante() {
        return idVariante;
    }
    
    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }
    
    public int getStockPar() {
        return stockPar;
    }
    
    public void setStockPar(int stockPar) {
        this.stockPar = Math.max(0, stockPar); // Evitar valores negativos
    }
    
    public int getStockCaja() {
        return stockCaja;
    }
    
    public void setStockCaja(int stockCaja) {
        this.stockCaja = Math.max(0, stockCaja); // Evitar valores negativos
    }
    
    public int getStockReservado() {
        return stockReservado;
    }
    
    public void setStockReservado(int stockReservado) {
        this.stockReservado = Math.max(0, stockReservado); // Evitar valores negativos
    }
    
    public LocalDateTime getFechaUltimoMovimiento() {
        return fechaUltimoMovimiento;
    }
    
    public void setFechaUltimoMovimiento(LocalDateTime fechaUltimoMovimiento) {
        this.fechaUltimoMovimiento = fechaUltimoMovimiento;
    }
    
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    public String getUbicacionEspecifica() {
        return ubicacionEspecifica;
    }
    
    public void setUbicacionEspecifica(String ubicacionEspecifica) {
        this.ubicacionEspecifica = ubicacionEspecifica;
    }
    
    public boolean isActivo() {
        return activo;
    }
    
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    
    public String getTalla() {
        return talla;
    }
    
    public void setTalla(String talla) {
        this.talla = talla;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getEan() {
        return ean;
    }
    
    public void setEan(String ean) {
        this.ean = ean;
    }
    
    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }
    
    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }
    
    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }
    
    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }
    
    public String getNombreProducto() {
        return nombreProducto;
    }
    
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }
    
    public int getIdProducto() {
        return idProducto;
    }
    
    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE OBJECT
    // ══════════════════════════════════════════════════════════════════════════
    
    @Override
    public String toString() {
        return String.format(
            "InventarioBodega{id=%d, bodega=%d, variante=%d, stockPar=%d, stockCaja=%d, " +
            "producto='%s', talla='%s', color='%s'}",
            idInventarioBodega, idBodega, idVariante, stockPar, stockCaja, 
            nombreProducto, talla, color
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ModelInventarioBodega that = (ModelInventarioBodega) obj;
        return idInventarioBodega == that.idInventarioBodega;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(idInventarioBodega);
    }
}