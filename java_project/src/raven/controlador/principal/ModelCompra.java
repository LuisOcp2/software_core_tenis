package raven.controlador.principal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo que representa una compra a proveedor (cabecera).
 * 
 * Mapea la tabla: compras
 * 
 * Relaciones:
 * - proveedores (id_proveedor)
 * - bodegas (id_bodega)
 * - usuarios (id_usuario)
 * - compra_detalles (1:N)
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelCompra {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES Y ENUMS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Estados posibles de la compra */
    public enum EstadoCompra {
        PENDIENTE("pendiente"),
        RECIBIDA("recibida"),
        CANCELADA("cancelada");

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
    private static final String PREFIJO_NUMERO = "CP-";

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════

    private Integer idCompra;
    private String numeroCompra;
    private Integer idProveedor;
    private Integer idUsuario;
    private Integer idBodega;
    private LocalDate fechaCompra;
    private String numeroFactura;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private EstadoCompra estado;
    private String observaciones;
    private LocalDateTime fechaRecepcion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private String estadoPago;
    /** Totales precalculados para listados (evita cargar detalles) */
    private int totalItemsResumen = -1;
    private int totalUnidadesResumen = -1;

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS DE RELACIÓN (para display)
    // ═══════════════════════════════════════════════════════════════════════════

    private String nombreProveedor;
    private String rucProveedor;
    private String nombreBodega;
    private String nombreUsuario;

    /** Lista de detalles (productos) de la compra */
    private List<ModelCompraDetalle> detalles;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════

    public ModelCompra() {
        this.estado = EstadoCompra.PENDIENTE;
        this.fechaCompra = LocalDate.now();
        this.subtotal = BigDecimal.ZERO;
        this.iva = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.detalles = new ArrayList<>();
        this.totalAbonado = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
        this.estadoPago = "pendiente";
    }

    /**
     * Constructor con datos mínimos requeridos.
     */
    public ModelCompra(Integer idProveedor, Integer idBodega, Integer idUsuario) {
        this();
        this.idProveedor = idProveedor;
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
        private final ModelCompra compra = new ModelCompra();

        public Builder idCompra(Integer id) {
            compra.idCompra = id;
            return this;
        }

        public Builder numeroCompra(String numero) {
            compra.numeroCompra = numero;
            return this;
        }

        public Builder proveedor(Integer idProveedor) {
            compra.idProveedor = idProveedor;
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

        public Builder numeroFactura(String factura) {
            compra.setNumeroFactura(factura);
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

        public Builder fechaCompra(LocalDate fecha) {
            compra.fechaCompra = fecha;
            return this;
        }

        public ModelCompra build() {
            compra.validar();
            return compra;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
    }

    public String getNumeroCompra() {
        return numeroCompra;
    }

    public void setNumeroCompra(String numeroCompra) {
        this.numeroCompra = numeroCompra;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }

    public LocalDate getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDate fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        if (numeroFactura != null && numeroFactura.length() > 50) {
            throw new IllegalArgumentException("El número de factura no puede exceder 50 caracteres");
        }
        this.numeroFactura = numeroFactura != null ? numeroFactura.trim() : null;
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

    public BigDecimal getTotalAbonado() {
        return totalAbonado;
    }

    public void setTotalAbonado(BigDecimal totalAbonado) {
        this.totalAbonado = totalAbonado != null ? totalAbonado : BigDecimal.ZERO;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente != null ? saldoPendiente : BigDecimal.ZERO;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago != null ? estadoPago : "pendiente";
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

    // Getters para campos de display
    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getRucProveedor() {
        return rucProveedor;
    }

    public void setRucProveedor(String rucProveedor) {
        this.rucProveedor = rucProveedor;
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
     */
    public List<ModelCompraDetalle> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /**
     * Establece la lista de detalles y recalcula totales.
     */
    public void setDetalles(List<ModelCompraDetalle> detalles) {
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
        recalcularTotales();
    }

    /**
     * Agrega un detalle a la compra y recalcula totales.
     */
    public void agregarDetalle(ModelCompraDetalle detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }

        detalle.setIdCompra(this.idCompra);
        this.detalles.add(detalle);
        recalcularTotales();
    }

    /**
     * Elimina un detalle de la compra y recalcula totales.
     */
    public boolean eliminarDetalle(ModelCompraDetalle detalle) {
        boolean eliminado = this.detalles.remove(detalle);
        if (eliminado) {
            recalcularTotales();
        }
        return eliminado;
    }

    /**
     * Elimina un detalle por su índice.
     */
    public ModelCompraDetalle eliminarDetalle(int index) {
        if (index < 0 || index >= detalles.size()) {
            throw new IndexOutOfBoundsException("Índice fuera de rango: " + index);
        }
        ModelCompraDetalle eliminado = this.detalles.remove(index);
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
        if (totalItemsResumen >= 0) {
            return totalItemsResumen;
        }
        return detalles.size();
    }

    /**
     * Obtiene la cantidad total de unidades en la compra.
     */
    public int getCantidadTotalUnidades() {
        if (totalUnidadesResumen >= 0) {
            return totalUnidadesResumen;
        }
        return detalles.stream()
                .mapToInt(ModelCompraDetalle::getCantidad)
                .sum();
    }

    public int getTotalItemsResumen() {
        return totalItemsResumen;
    }

    public void setTotalItemsResumen(int totalItemsResumen) {
        this.totalItemsResumen = totalItemsResumen;
    }

    public int getTotalUnidadesResumen() {
        return totalUnidadesResumen;
    }

    public void setTotalUnidadesResumen(int totalUnidadesResumen) {
        this.totalUnidadesResumen = totalUnidadesResumen;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Recalcula subtotal, IVA y total basado en los detalles.
     */
    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(ModelCompraDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // IVA 0% por defecto en compras a proveedores
        this.iva = BigDecimal.ZERO;

        this.total = this.subtotal.add(this.iva);
    }

    /**
     * Valida que la compra tenga los campos requeridos.
     */
    public void validar() {
        StringBuilder errores = new StringBuilder();

        if (idProveedor == null) {
            errores.append("- Proveedor es requerido\n");
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
     * Formato: CP-YYYYMMDD-XXXX
     */
    public static String generarNumeroCompra(int secuencial) {
        LocalDate ahora = LocalDate.now();
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
     */
    public boolean puedeEditarse() {
        return estado == EstadoCompra.PENDIENTE;
    }

    /**
     * Verifica si la compra puede anularse.
     */
    public boolean puedeAnularse() {
        return estado != EstadoCompra.CANCELADA;
    }

    @Override
    public String toString() {
        return String.format("Compra[%s]: Proveedor %s - $%s (%d items)",
                numeroCompra, nombreProveedor, total, getCantidadItems());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ModelCompra that = (ModelCompra) obj;
        return idCompra != null && idCompra.equals(that.idCompra);
    }

    @Override
    public int hashCode() {
        return idCompra != null ? idCompra.hashCode() : 0;
    }
}
