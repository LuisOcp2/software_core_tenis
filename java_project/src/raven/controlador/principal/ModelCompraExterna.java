package raven.controlador.principal;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo que representa una compra externa (cabecera).
 * 
 * Mapea la tabla: compras_externas
 * 
 * Relaciones:
 * - bodegas (id_bodega)
 * - usuarios (id_usuario)
 * - compras_externas_detalles (1:N)
 * 
 * Principios aplicados:
 * - Aggregate Root: gestiona sus detalles internamente
 * - Encapsulamiento: cálculos automáticos de totales
 * - Inmutabilidad de colecciones: lista de detalles protegida
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelCompraExterna {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES Y ENUMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Estados posibles de la compra */
    public enum EstadoCompra {
        PENDIENTE("pendiente"),
        RECIBIDA("recibida"),
        FACTURADA("facturada");
        
        private final String valor;
        
        EstadoCompra(String valor) {
            this.valor = valor;
        }
        
        public String getValor() {
            return valor;
        }
        
        public static EstadoCompra fromString(String texto) {
            for (EstadoCompra e : values()) {
                if (e.valor.equalsIgnoreCase(texto)) {
                    return e;
                }
            }
            return PENDIENTE;
        }
    }
    
    /** Prefijo para generación de número de compra */
    private static final String PREFIJO_NUMERO = "CE-";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Integer idCompraExterna;
    private String numeroCompra;
    private String tiendaProveedor;
    private String numeroFacturaRecibo;
    private Integer idBodega;
    private Integer idUsuario;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private EstadoCompra estado;
    private String observaciones;
    private LocalDateTime fechaCompra;
    private LocalDateTime fechaRecepcion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS DE RELACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String nombreBodega;
    private String nombreUsuario;
    
    /** Lista de detalles (productos) de la compra */
    private List<ModelCompraExternaDetalle> detalles;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ModelCompraExterna() {
        this.estado = EstadoCompra.PENDIENTE;
        this.fechaCompra = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.iva = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.detalles = new ArrayList<>();
    }
    
    /**
     * Constructor con datos mínimos requeridos.
     */
    public ModelCompraExterna(String tiendaProveedor, Integer idBodega, Integer idUsuario) {
        this();
        this.tiendaProveedor = tiendaProveedor;
        this.idBodega = idBodega;
        this.idUsuario = idUsuario;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final ModelCompraExterna compra = new ModelCompraExterna();
        
        public Builder idCompra(Integer id) {
            compra.idCompraExterna = id;
            return this;
        }
        
        public Builder numeroCompra(String numero) {
            compra.numeroCompra = numero;
            return this;
        }
        
        public Builder tiendaProveedor(String tienda) {
            compra.setTiendaProveedor(tienda);
            return this;
        }
        
        public Builder numeroFactura(String factura) {
            compra.setNumeroFacturaRecibo(factura);
            return this;
        }
        
        public Builder bodega(Integer idBodega) {
            compra.idBodega = idBodega;
            return this;
        }
        
        public Builder usuario(Integer idUsuario) {
            compra.idUsuario = idUsuario;
            return this;
        }
        
        public Builder observaciones(String obs) {
            compra.observaciones = obs;
            return this;
        }
        
        public Builder estado(EstadoCompra estado) {
            compra.estado = estado;
            return this;
        }
        
        public Builder fechaCompra(LocalDateTime fecha) {
            compra.fechaCompra = fecha;
            return this;
        }
        
        public ModelCompraExterna build() {
            compra.validar();
            return compra;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Integer getIdCompraExterna() {
        return idCompraExterna;
    }
    
    public void setIdCompraExterna(Integer idCompraExterna) {
        this.idCompraExterna = idCompraExterna;
    }
    
    public String getNumeroCompra() {
        return numeroCompra;
    }
    
    public void setNumeroCompra(String numeroCompra) {
        this.numeroCompra = numeroCompra;
    }
    
    public String getTiendaProveedor() {
        return tiendaProveedor;
    }
    
    public void setTiendaProveedor(String tiendaProveedor) {
        if (tiendaProveedor != null && tiendaProveedor.length() > 100) {
            throw new IllegalArgumentException("La tienda/proveedor no puede exceder 100 caracteres");
        }
        this.tiendaProveedor = tiendaProveedor != null ? tiendaProveedor.trim() : null;
    }
    
    public String getNumeroFacturaRecibo() {
        return numeroFacturaRecibo;
    }
    
    public void setNumeroFacturaRecibo(String numeroFacturaRecibo) {
        if (numeroFacturaRecibo != null && numeroFacturaRecibo.length() > 50) {
            throw new IllegalArgumentException("El número de factura no puede exceder 50 caracteres");
        }
        this.numeroFacturaRecibo = numeroFacturaRecibo != null ? numeroFacturaRecibo.trim() : null;
    }
    
    public Integer getIdBodega() {
        return idBodega;
    }
    
    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }
    
    public Integer getIdUsuario() {
        return idUsuario;
    }
    
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }
    
    public BigDecimal getIva() {
        return iva;
    }
    
    public void setIva(BigDecimal iva) {
        this.iva = iva != null ? iva : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total != null ? total : BigDecimal.ZERO;
    }
    
    public EstadoCompra getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoCompra estado) {
        this.estado = estado != null ? estado : EstadoCompra.PENDIENTE;
    }
    
    public void setEstadoFromString(String estadoStr) {
        this.estado = EstadoCompra.fromString(estadoStr);
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }
    
    public void setFechaCompra(LocalDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }
    
    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }
    
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    public String getNombreBodega() {
        return nombreBodega;
    }
    
    public void setNombreBodega(String nombreBodega) {
        this.nombreBodega = nombreBodega;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GESTIÓN DE DETALLES (AGGREGATE ROOT PATTERN)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Obtiene lista inmutable de detalles.
     * Protege la integridad del agregado.
     */
    public List<ModelCompraExternaDetalle> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }
    
    /**
     * Establece la lista de detalles y recalcula totales.
     */
    public void setDetalles(List<ModelCompraExternaDetalle> detalles) {
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
        recalcularTotales();
    }
    
    /**
     * Agrega un detalle a la compra y recalcula totales.
     * 
     * @param detalle Detalle a agregar
     * @throws IllegalArgumentException si el detalle es nulo
     */
    public void agregarDetalle(ModelCompraExternaDetalle detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }
        
        detalle.setIdCompraExterna(this.idCompraExterna);
        this.detalles.add(detalle);
        recalcularTotales();
    }
    
    /**
     * Elimina un detalle de la compra y recalcula totales.
     * 
     * @param detalle Detalle a eliminar
     * @return true si se eliminó correctamente
     */
    public boolean eliminarDetalle(ModelCompraExternaDetalle detalle) {
        boolean eliminado = this.detalles.remove(detalle);
        if (eliminado) {
            recalcularTotales();
        }
        return eliminado;
    }
    
    /**
     * Elimina un detalle por su índice.
     * 
     * @param index Índice del detalle
     * @return Detalle eliminado
     */
    public ModelCompraExternaDetalle eliminarDetalle(int index) {
        if (index < 0 || index >= detalles.size()) {
            throw new IndexOutOfBoundsException("Índice fuera de rango: " + index);
        }
        ModelCompraExternaDetalle eliminado = this.detalles.remove(index);
        recalcularTotales();
        return eliminado;
    }
    
    /**
     * Limpia todos los detalles de la compra.
     */
    public void limpiarDetalles() {
        this.detalles.clear();
        recalcularTotales();
    }
    
    /**
     * Obtiene la cantidad de items en la compra.
     */
    public int getCantidadItems() {
        return detalles.size();
    }
    
    /**
     * Obtiene la cantidad total de pares en la compra.
     */
    public int getCantidadTotalPares() {
        return detalles.stream()
                .mapToInt(ModelCompraExternaDetalle::getCantidad)
                .sum();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Recalcula subtotal, IVA y total basado en los detalles.
     * Se llama automáticamente al modificar detalles.
     */
    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(ModelCompraExternaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // IVA 0% por defecto en compras externas (se puede modificar)
        this.iva = BigDecimal.ZERO;
        
        this.total = this.subtotal.add(this.iva);
    }
    
    /**
     * Valida que la compra tenga los campos requeridos.
     */
    public void validar() {
        StringBuilder errores = new StringBuilder();
        
        if (tiendaProveedor == null || tiendaProveedor.isEmpty()) {
            errores.append("- Tienda/Proveedor es requerido\n");
        }
        if (idBodega == null) {
            errores.append("- Bodega es requerida\n");
        }
        if (idUsuario == null) {
            errores.append("- Usuario es requerido\n");
        }
        
        if (errores.length() > 0) {
            throw new IllegalStateException("Errores de validación:\n" + errores);
        }
    }
    
    /**
     * Valida que la compra esté completa para guardar.
     */
    public void validarParaGuardar() {
        validar();
        
        if (detalles.isEmpty()) {
            throw new IllegalStateException("La compra debe tener al menos un producto");
        }
        
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El total de la compra debe ser mayor a cero");
        }
    }
    
    /**
     * Genera un número de compra único.
     * Formato: CE-YYYYMMDD-XXXX
     */
    public static String generarNumeroCompra(int secuencial) {
        LocalDateTime ahora = LocalDateTime.now();
        return String.format("%s%d%02d%02d-%04d",
                PREFIJO_NUMERO,
                ahora.getYear(),
                ahora.getMonthValue(),
                ahora.getDayOfMonth(),
                secuencial);
    }
    
    /**
     * Marca la compra como recibida.
     */
    public void marcarRecibida() {
        this.estado = EstadoCompra.RECIBIDA;
        this.fechaRecepcion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    /**
     * Verifica si la compra puede editarse.
     * Solo compras PENDIENTES pueden editarse.
     */
    public boolean puedeEditarse() {
        return estado == EstadoCompra.PENDIENTE;
    }
    
    @Override
    public String toString() {
        return String.format("Compra[%s]: %s - $%s (%d items)", 
            numeroCompra, tiendaProveedor, total, getCantidadItems());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelCompraExterna that = (ModelCompraExterna) obj;
        return idCompraExterna != null && idCompraExterna.equals(that.idCompraExterna);
    }
    
    @Override
    public int hashCode() {
        return idCompraExterna != null ? idCompraExterna.hashCode() : 0;
    }
}