package raven.controlador.principal;


import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa un gasto operativo registrado.
 * 
 * Mapea la tabla: gastos_operativos
 * 
 * Relaciones:
 * - tipos_gastos (id_tipo_gasto)
 * - bodegas (id_bodega)
 * - usuarios (id_usuario)
 * - caja_movimiento_detalle (id_movimiento_caja)
 * 
 * Principios aplicados:
 * - Builder Pattern: construcción fluida
 * - Validación en dominio: reglas de negocio encapsuladas
 * - Inmutabilidad donde es posible
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelGastoOperativo {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES Y ENUMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Estados posibles del gasto */
    public enum EstadoGasto {
        REGISTRADO("registrado"),
        CONTABILIZADO("contabilizado"),
        ANULADO("anulado");
        
        private final String valor;
        
        EstadoGasto(String valor) {
            this.valor = valor;
        }
        
        public String getValor() {
            return valor;
        }
        
        public static EstadoGasto fromString(String texto) {
            for (EstadoGasto e : values()) {
                if (e.valor.equalsIgnoreCase(texto)) {
                    return e;
                }
            }
            return REGISTRADO;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Integer idGasto;
    private Integer idTipoGasto;
    private String concepto;
    private BigDecimal monto;
    private String proveedorPersona;
    private String numeroRecibo;
    private String observaciones;
    private Integer idBodega;
    private Integer idUsuario;
    private Integer idMovimientoCaja;  // FK a caja_movimiento_detalle
    private EstadoGasto estado;
    private LocalDateTime fechaGasto;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS DE RELACIÓN (para display en UI)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String nombreTipoGasto;
    private String nombreBodega;
    private String nombreUsuario;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ModelGastoOperativo() {
        this.estado = EstadoGasto.REGISTRADO;
        this.fechaGasto = LocalDateTime.now();
        this.monto = BigDecimal.ZERO;
    }
    
    /**
     * Constructor con datos mínimos requeridos.
     */
    public ModelGastoOperativo(Integer idTipoGasto, String concepto, 
                                BigDecimal monto, Integer idBodega, Integer idUsuario) {
        this();
        this.idTipoGasto = idTipoGasto;
        this.concepto = concepto;
        this.monto = monto;
        this.idBodega = idBodega;
        this.idUsuario = idUsuario;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN - Para construcción fluida
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final ModelGastoOperativo gasto = new ModelGastoOperativo();
        
        public Builder idGasto(Integer id) {
            gasto.idGasto = id;
            return this;
        }
        
        public Builder tipoGasto(Integer idTipoGasto) {
            gasto.idTipoGasto = idTipoGasto;
            return this;
        }
        
        public Builder concepto(String concepto) {
            gasto.setConcepto(concepto);
            return this;
        }
        
        public Builder monto(BigDecimal monto) {
            gasto.setMonto(monto);
            return this;
        }
        
        public Builder monto(double monto) {
            gasto.setMonto(BigDecimal.valueOf(monto));
            return this;
        }
        
        public Builder proveedor(String proveedor) {
            gasto.setProveedorPersona(proveedor);
            return this;
        }
        
        public Builder numeroRecibo(String recibo) {
            gasto.setNumeroRecibo(recibo);
            return this;
        }
        
        public Builder observaciones(String obs) {
            gasto.observaciones = obs;
            return this;
        }
        
        public Builder bodega(Integer idBodega) {
            gasto.idBodega = idBodega;
            return this;
        }
        
        public Builder usuario(Integer idUsuario) {
            gasto.idUsuario = idUsuario;
            return this;
        }
        
        public Builder movimientoCaja(Integer idMovimiento) {
            gasto.idMovimientoCaja = idMovimiento;
            return this;
        }
        
        public Builder estado(EstadoGasto estado) {
            gasto.estado = estado;
            return this;
        }
        
        public Builder fechaGasto(LocalDateTime fecha) {
            gasto.fechaGasto = fecha;
            return this;
        }
        
        public ModelGastoOperativo build() {
            gasto.validar();
            return gasto;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS CON VALIDACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Integer getIdGasto() {
        return idGasto;
    }
    
    public void setIdGasto(Integer idGasto) {
        this.idGasto = idGasto;
    }
    
    public Integer getIdTipoGasto() {
        return idTipoGasto;
    }
    
    public void setIdTipoGasto(Integer idTipoGasto) {
        this.idTipoGasto = idTipoGasto;
    }
    
    public String getConcepto() {
        return concepto;
    }
    
    public void setConcepto(String concepto) {
        if (concepto != null && concepto.length() > 255) {
            throw new IllegalArgumentException("El concepto no puede exceder 255 caracteres");
        }
        this.concepto = concepto != null ? concepto.trim() : null;
    }
    
    public BigDecimal getMonto() {
        return monto;
    }
    
    public void setMonto(BigDecimal monto) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        this.monto = monto != null ? monto : BigDecimal.ZERO;
    }
    
    public String getProveedorPersona() {
        return proveedorPersona;
    }
    
    public void setProveedorPersona(String proveedorPersona) {
        if (proveedorPersona != null && proveedorPersona.length() > 100) {
            throw new IllegalArgumentException("El proveedor/persona no puede exceder 100 caracteres");
        }
        this.proveedorPersona = proveedorPersona != null ? proveedorPersona.trim() : null;
    }
    
    public String getNumeroRecibo() {
        return numeroRecibo;
    }
    
    public void setNumeroRecibo(String numeroRecibo) {
        if (numeroRecibo != null && numeroRecibo.length() > 50) {
            throw new IllegalArgumentException("El número de recibo no puede exceder 50 caracteres");
        }
        this.numeroRecibo = numeroRecibo != null ? numeroRecibo.trim() : null;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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
    
    public Integer getIdMovimientoCaja() {
        return idMovimientoCaja;
    }
    
    public void setIdMovimientoCaja(Integer idMovimientoCaja) {
        this.idMovimientoCaja = idMovimientoCaja;
    }
    
    public EstadoGasto getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoGasto estado) {
        this.estado = estado != null ? estado : EstadoGasto.REGISTRADO;
    }
    
    public void setEstadoFromString(String estadoStr) {
        this.estado = EstadoGasto.fromString(estadoStr);
    }
    
    public LocalDateTime getFechaGasto() {
        return fechaGasto;
    }
    
    public void setFechaGasto(LocalDateTime fechaGasto) {
        this.fechaGasto = fechaGasto;
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
    
    // Getters para campos de relación (display)
    public String getNombreTipoGasto() {
        return nombreTipoGasto;
    }
    
    public void setNombreTipoGasto(String nombreTipoGasto) {
        this.nombreTipoGasto = nombreTipoGasto;
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
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Valida que el gasto tenga los campos requeridos.
     * 
     * @throws IllegalStateException si faltan campos obligatorios
     */
    public void validar() {
        StringBuilder errores = new StringBuilder();
        
        if (idTipoGasto == null) {
            errores.append("- Tipo de gasto es requerido\n");
        }
        if (concepto == null || concepto.isEmpty()) {
            errores.append("- Concepto es requerido\n");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            errores.append("- Monto debe ser mayor a cero\n");
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
     * Verifica si el gasto puede ser anulado.
     * Solo gastos en estado REGISTRADO pueden anularse.
     */
    public boolean puedeAnularse() {
        return estado == EstadoGasto.REGISTRADO;
    }
    
    /**
     * Anula el gasto si es posible.
     * 
     * @throws IllegalStateException si no puede anularse
     */
    public void anular() {
        if (!puedeAnularse()) {
            throw new IllegalStateException(
                "No se puede anular un gasto en estado: " + estado.getValor()
            );
        }
        this.estado = EstadoGasto.ANULADO;
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    /**
     * Verifica si el gasto está activo (no anulado).
     */
    public boolean isActivo() {
        return estado != EstadoGasto.ANULADO;
    }
    
    @Override
    public String toString() {
        return String.format("Gasto[%d]: %s - $%s (%s)", 
            idGasto, concepto, monto, estado.getValor());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelGastoOperativo that = (ModelGastoOperativo) obj;
        return idGasto != null && idGasto.equals(that.idGasto);
    }
    
    @Override
    public int hashCode() {
        return idGasto != null ? idGasto.hashCode() : 0;
    }
}