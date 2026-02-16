package raven.controlador.inventario;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) que representa el inventario de una variante de producto
 * en una bodega específica.
 * 
 * Esta clase sigue el patrón DTO para encapsular la información de inventario
 * y facilitar su transferencia entre capas de la aplicación.
 * 
 * Principios SOLID aplicados:
 * - Single Responsibility: Solo representa datos de inventario de bodega
 * - Open/Closed: Abierto a extensión mediante herencia
 * 
 * @author Sistema de Gestión Multi-Bodega
 * @version 1.0
 */
public class InventarioBodega {
    
    // ==================== ATRIBUTOS ====================
    
    /** Identificador único del registro de inventario en bodega */
    private Integer idInventarioBodega;
    
    /** ID de la bodega donde se encuentra el inventario */
    private Integer idBodega;
    
    /** Nombre de la bodega (para mostrar en UI) */
    private String nombreBodega;
    
    /** Código de la bodega */
    private String codigoBodega;
    
    /** ID de la variante del producto */
    private Integer idVariante;
    
    /** SKU de la variante */
    private String sku;
    
    /** Información del producto base */
    private Integer idProducto;
    private String nombreProducto;
    private String codigoModelo;
    
    /** Información de la variante */
    private String talla;
    private String color;
    
    /** Stock disponible en pares */
    private Integer stockPar;
    
    /** Stock disponible en cajas */
    private Integer stockCaja;
    
    /** Stock reservado (no disponible para venta) */
    private Integer stockReservado;
    
    /** Stock disponible real (stock - reservado) */
    private Integer stockDisponible;
    
    /** Ubicación física específica dentro de la bodega */
    private String ubicacionEspecifica;
    
    /** Fecha del último movimiento de inventario */
    private LocalDateTime fechaUltimoMovimiento;
    
    /** Fecha de última actualización del registro */
    private LocalDateTime fechaActualizacion;
    
    /** Indica si el registro está activo */
    private Boolean activo;
    
    /** Precios de la variante */
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    
    /** Stock mínimo configurado para alertas */
    private Integer stockMinimo;
    
    /** Pares por caja del producto */
    private Integer paresPorCaja;

    /** Código de barras EAN-13 */
    private String ean;
    
    // ==================== CONSTRUCTORES ====================
    
    /**
     * Constructor por defecto.
     * Inicializa valores predeterminados para campos críticos.
     */
    public InventarioBodega() {
        this.stockPar = 0;
        this.stockCaja = 0;
        this.stockReservado = 0;
        this.stockDisponible = 0;
        this.activo = true;
    }
    
    /**
     * Constructor con parámetros principales.
     * Útil para crear nuevos registros de inventario.
     * 
     * @param idBodega ID de la bodega
     * @param idVariante ID de la variante del producto
     * @param stockPar Stock inicial en pares
     * @param stockCaja Stock inicial en cajas
     */
    public InventarioBodega(Integer idBodega, Integer idVariante, 
                           Integer stockPar, Integer stockCaja) {
        this();
        this.idBodega = idBodega;
        this.idVariante = idVariante;
        this.stockPar = stockPar != null ? stockPar : 0;
        this.stockCaja = stockCaja != null ? stockCaja : 0;
        calcularStockDisponible();
    }
    
    /**
     * Constructor completo para mapeo desde base de datos.
     * 
     * @param idInventarioBodega ID del registro
     * @param idBodega ID de la bodega
     * @param nombreBodega Nombre de la bodega
     * @param idVariante ID de la variante
     * @param stockPar Stock en pares
     * @param stockCaja Stock en cajas
     * @param stockReservado Stock reservado
     */
    public InventarioBodega(Integer idInventarioBodega, Integer idBodega, 
                           String nombreBodega, Integer idVariante,
                           Integer stockPar, Integer stockCaja, Integer stockReservado) {
        this.idInventarioBodega = idInventarioBodega;
        this.idBodega = idBodega;
        this.nombreBodega = nombreBodega;
        this.idVariante = idVariante;
        this.stockPar = stockPar != null ? stockPar : 0;
        this.stockCaja = stockCaja != null ? stockCaja : 0;
        this.stockReservado = stockReservado != null ? stockReservado : 0;
        this.activo = true;
        calcularStockDisponible();
    }
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Calcula el stock disponible real restando el stock reservado.
     * Este método debe llamarse cada vez que cambie stockPar, stockCaja o stockReservado.
     */
    private void calcularStockDisponible() {
        int totalStock = (this.stockPar != null ? this.stockPar : 0);
        int reservado = (this.stockReservado != null ? this.stockReservado : 0);
        this.stockDisponible = Math.max(0, totalStock - reservado);
    }
    
    /**
     * Verifica si el stock actual está por debajo del mínimo configurado.
     * 
     * @return true si el stock está bajo, false en caso contrario
     */
    public boolean isStockBajo() {
        if (stockMinimo == null || stockMinimo == 0) {
            return false;
        }
        int totalStock = (stockPar != null ? stockPar : 0);
        return totalStock < stockMinimo;
    }
    
    /**
     * Verifica si hay stock disponible para venta.
     * 
     * @return true si hay stock disponible, false en caso contrario
     */
    public boolean hayStockDisponible() {
        return stockDisponible != null && stockDisponible > 0;
    }
    
    /**
     * Obtiene el stock total en pares (convirtiendo cajas si es necesario).
     * 
     * @return Total de pares disponibles
     */
    public int getTotalPares() {
        int pares = (stockPar != null ? stockPar : 0);
        int cajas = (stockCaja != null ? stockCaja : 0);
        int paresPorCajaVal = (paresPorCaja != null ? paresPorCaja : 24);
        
        return pares + (cajas * paresPorCajaVal);
    }
    
    /**
     * Devuelve información de stock formateada
     */
    public String getStockInfo() {
        return String.format("Pares: %d | Cajas: %d | Total: %d pares", 
            stockPar, stockCaja, getTotalStockInPairs());
    }
    /**
     * Calcula el stock total en pares (pares + cajas convertidas)
     */
    public int getTotalStockInPairs() {
        return stockPar + (stockCaja * 24); // 24 pares por caja
    }
    /**
     * Calcula el stock total en cajas (cajas + pares convertidos)
     */
    public double getTotalStockInBoxes() {
        return stockCaja + (stockPar / (double) 24);
    }
     /**
     * Reduce stock de pares
     */
    public boolean reducePairsStock(int pairs) {
        if (pairs <= 0 || this.stockPar < pairs) {
            return false;
        }
        this.stockPar -= pairs;
        return true;
    }
    
    /**
     * Reduce stock de cajas
     */
    public boolean reduceBoxesStock(int boxes) {
        if (boxes <= 0 || this.stockCaja < boxes) {
            return false;
        }
        this.stockCaja -= boxes;
        return true;
    }
    
     /**
     * Añade stock de pares
     */
    public void addPairsStock(int pairs) {
        this.stockPar += Math.max(0, pairs);
    }
    
    /**
     * Añade stock de cajas
     */
    public void addBoxesStock(int boxes) {
        this.stockCaja += Math.max(0, boxes);
    }
    
     /**
     * Actualiza stock de pares
     */
    public void updatePairsStock(int newStock) {
        this.stockPar = Math.max(0, newStock);
    }
    
    /**
     * Actualiza stock de cajas
     */
    public void updateBoxesStock(int newStock) {
        this.stockCaja = Math.max(0, newStock);
    }
   
    
    /**
     * Añade stock en pares y recalcula el disponible.
     * 
     * @param cantidad Cantidad de pares a añadir
     */
    public void agregarStockPares(int cantidad) {
        this.stockPar = (this.stockPar != null ? this.stockPar : 0) + cantidad;
        calcularStockDisponible();
    }
    
    /**
     * Añade stock en cajas y recalcula el disponible.
     * 
     * @param cantidad Cantidad de cajas a añadir
     */
    public void agregarStockCajas(int cantidad) {
        this.stockCaja = (this.stockCaja != null ? this.stockCaja : 0) + cantidad;
        calcularStockDisponible();
    }
    
    /**
     * Reduce stock en pares.
     * 
     * @param cantidad Cantidad de pares a reducir
     * @return true si la operación fue exitosa, false si no hay suficiente stock
     */
    public boolean reducirStockPares(int cantidad) {
        int stockActual = (this.stockPar != null ? this.stockPar : 0);
        if (stockActual < cantidad) {
            return false;
        }
        this.stockPar = stockActual - cantidad;
        calcularStockDisponible();
        return true;
    }
    
    /**
     * Reduce stock en cajas.
     * 
     * @param cantidad Cantidad de cajas a reducir
     * @return true si la operación fue exitosa, false si no hay suficiente stock
     */
    public boolean reducirStockCajas(int cantidad) {
        int stockActual = (this.stockCaja != null ? this.stockCaja : 0);
        if (stockActual < cantidad) {
            return false;
        }
        this.stockCaja = stockActual - cantidad;
        calcularStockDisponible();
        return true;
    }
    
    /**
     * Reserva stock para una operación futura (ej: pedido pendiente).
     * 
     * @param cantidad Cantidad a reservar
     * @return true si se pudo reservar, false si no hay suficiente stock
     */
    public boolean reservarStock(int cantidad) {
        int disponible = (this.stockDisponible != null ? this.stockDisponible : 0);
        if (disponible < cantidad) {
            return false;
        }
        this.stockReservado = (this.stockReservado != null ? this.stockReservado : 0) + cantidad;
        calcularStockDisponible();
        return true;
    }
    
    /**
     * Libera stock previamente reservado.
     * 
     * @param cantidad Cantidad a liberar
     */
    public void liberarStock(int cantidad) {
        int reservado = (this.stockReservado != null ? this.stockReservado : 0);
        this.stockReservado = Math.max(0, reservado - cantidad);
        calcularStockDisponible();
    }
    
    // ==================== GETTERS Y SETTERS ====================
    
    public Integer getIdInventarioBodega() {
        return idInventarioBodega;
    }
    
    public void setIdInventarioBodega(Integer idInventarioBodega) {
        this.idInventarioBodega = idInventarioBodega;
    }
    
    public Integer getIdBodega() {
        return idBodega;
    }
    
    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }
    
    public String getNombreBodega() {
        return nombreBodega;
    }
    
    public void setNombreBodega(String nombreBodega) {
        this.nombreBodega = nombreBodega;
    }
    
    public String getCodigoBodega() {
        return codigoBodega;
    }
    
    public void setCodigoBodega(String codigoBodega) {
        this.codigoBodega = codigoBodega;
    }
    
    public Integer getIdVariante() {
        return idVariante;
    }
    
    public void setIdVariante(Integer idVariante) {
        this.idVariante = idVariante;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public Integer getIdProducto() {
        return idProducto;
    }
    
    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }
    
    public String getNombreProducto() {
        return nombreProducto;
    }
    
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }
    
    public String getCodigoModelo() {
        return codigoModelo;
    }
    
    public void setCodigoModelo(String codigoModelo) {
        this.codigoModelo = codigoModelo;
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
    
    public Integer getStockPar() {
        return stockPar;
    }
    
    public void setStockPar(Integer stockPar) {
        this.stockPar = stockPar;
        calcularStockDisponible();
    }
    
    public Integer getStockCaja() {
        return stockCaja;
    }
    
    public void setStockCaja(Integer stockCaja) {
        this.stockCaja = stockCaja;
    }
    
    public Integer getStockReservado() {
        return stockReservado;
    }
    
    public void setStockReservado(Integer stockReservado) {
        this.stockReservado = stockReservado;
        calcularStockDisponible();
    }
    
    public Integer getStockDisponible() {
        return stockDisponible;
    }
    
    public String getUbicacionEspecifica() {
        return ubicacionEspecifica;
    }
    
    public void setUbicacionEspecifica(String ubicacionEspecifica) {
        this.ubicacionEspecifica = ubicacionEspecifica;
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
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }
    
    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }
    
    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }
    
    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }
    
    public Integer getStockMinimo() {
        return stockMinimo;
    }
    
    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
    
    public Integer getParesPorCaja() {
        return paresPorCaja;
    }
    
    public void setParesPorCaja(Integer paresPorCaja) {
        this.paresPorCaja = paresPorCaja;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }
    
    // ==================== MÉTODOS OVERRIDE ====================
    
    @Override
    public String toString() {
        return String.format("InventarioBodega[id=%d, bodega=%s, variante=%d, " +
                           "stockPar=%d, stockCaja=%d, disponible=%d]",
                           idInventarioBodega, nombreBodega, idVariante,
                           stockPar, stockCaja, stockDisponible);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        InventarioBodega other = (InventarioBodega) obj;
        
        if (idInventarioBodega != null && other.idInventarioBodega != null) {
            return idInventarioBodega.equals(other.idInventarioBodega);
        }
        
        // Si no tienen ID, comparar por bodega y variante
        return idBodega != null && idBodega.equals(other.idBodega) &&
               idVariante != null && idVariante.equals(other.idVariante);
    }
    
    @Override
    public int hashCode() {
        if (idInventarioBodega != null) {
            return idInventarioBodega.hashCode();
        }
        
        int result = idBodega != null ? idBodega.hashCode() : 0;
        result = 31 * result + (idVariante != null ? idVariante.hashCode() : 0);
        return result;
    }
}