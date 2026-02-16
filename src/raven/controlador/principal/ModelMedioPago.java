package raven.controlador.principal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un medio de pago aplicado a una venta.
 * 
 * PRINCIPIOS APLICADOS:
 * - Single Responsibility: Solo maneja datos de un medio de pago
 * - Encapsulation: Validaciones en setters
 * - Immutability: Campos críticos validados antes de asignar
 * 
 * MODIFICADO: Agregado soporte para ID de Nota de Crédito
 * 
 * @author lmog2
 */
public class ModelMedioPago {

    // ==================== ATRIBUTOS ====================
    private int idMedioPago;
    private int idVenta;
    private TipoMedioPago tipo;
    private BigDecimal monto;
    private String numeroReferencia; // Para transferencias, tarjetas, NC
    private String observaciones;
    private LocalDateTime fechaRegistro;
    private boolean activo;

    // NUEVO: Soporte para Nota de Crédito
    private Integer idNotaCredito; // Nullable: solo aplica para tipo NOTA_CREDITO

    // ==================== ENUM: TIPOS DE PAGO ====================
    /**
     * Define los tipos de pago soportados por el sistema.
     * 
     * VENTAJA: Type-safe, evita strings mágicos
     */
    public enum TipoMedioPago {
        EFECTIVO("efectivo", "Efectivo"),
        TARJETA_CREDITO("tarjeta_credito", "Tarjeta de Crédito"),
        TARJETA_DEBITO("tarjeta_debito", "Tarjeta de Débito"),
        TRANSFERENCIA("transferencia", "Transferencia Bancaria"),
        SISTECREDITO("sistecredito", "Sistecrédito"),
        CREDITO("credito", "Crédito"),
        NOTA_CREDITO("nota_credito", "Nota de Crédito"),
        OTRO("otro", "Otro Medio");

        private final String codigo;
        private final String descripcion;

        TipoMedioPago(String codigo, String descripcion) {
            this.codigo = codigo;
            this.descripcion = descripcion;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public static boolean requiereReferencia(TipoMedioPago tipo) {
            return tipo == TipoMedioPago.TARJETA_CREDITO
                    || tipo == TipoMedioPago.TARJETA_DEBITO
                    || tipo == TipoMedioPago.TRANSFERENCIA
                    || tipo == TipoMedioPago.SISTECREDITO
                    || tipo == TipoMedioPago.NOTA_CREDITO;
        }

        /**
         * Busca un tipo de pago por su código.
         * 
         * @param codigo Código a buscar
         * @return TipoMedioPago correspondiente o null si no existe
         */
        public static TipoMedioPago fromCodigo(String codigo) {
            if (codigo == null)
                return null;

            for (TipoMedioPago tipo : values()) {
                if (tipo.getCodigo().equalsIgnoreCase(codigo)) {
                    return tipo;
                }
            }
            return null;
        }
    }

    // ==================== CONSTRUCTORES ====================
    public ModelMedioPago() {
        this.fechaRegistro = LocalDateTime.now();
        this.activo = true;
    }

    public ModelMedioPago(TipoMedioPago tipo, BigDecimal monto) {
        this();
        setTipo(tipo);
        setMonto(monto);
    }

    /**
     * Constructor específico para pagos con Nota de Crédito.
     * 
     * @param monto         Monto a aplicar
     * @param idNotaCredito ID de la nota de crédito
     * @param numeroNC      Número de la nota de crédito (para referencia)
     */
    public ModelMedioPago(BigDecimal monto, Integer idNotaCredito, String numeroNC) {
        this(TipoMedioPago.NOTA_CREDITO, monto);
        setIdNotaCredito(idNotaCredito);
        setNumeroReferencia(numeroNC);
    }

    // ==================== GETTERS Y SETTERS ====================
    public int getIdMedioPago() {
        return idMedioPago;
    }

    public void setIdMedioPago(int idMedioPago) {
        this.idMedioPago = idMedioPago;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public TipoMedioPago getTipo() {
        return tipo;
    }

    /**
     * Establece el tipo de medio de pago.
     * 
     * @param tipo Tipo de pago
     * @throws IllegalArgumentException si el tipo es nulo
     */
    public void setTipo(TipoMedioPago tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de pago no puede ser nulo");
        }
        this.tipo = tipo;
    }

    /**
     * Alias para getTipo().
     * Útil para mantener compatibilidad con código existente.
     * 
     * @return Tipo de medio de pago
     */
    public TipoMedioPago getTipoMedioPago() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    /**
     * Establece el monto del pago con validación.
     * 
     * @param monto Monto a asignar
     * @throws IllegalArgumentException si el monto es nulo o negativo
     */
    public void setMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        this.monto = monto;
    }

    public String getNumeroReferencia() {
        return numeroReferencia;
    }

    public void setNumeroReferencia(String numeroReferencia) {
        this.numeroReferencia = numeroReferencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // NUEVOS GETTERS/SETTERS PARA NOTA DE CRÉDITO

    /**
     * Obtiene el ID de la nota de crédito asociada.
     * 
     * @return ID de la NC o null si no aplica
     */
    public Integer getIdNotaCredito() {
        return idNotaCredito;
    }

    /**
     * Establece el ID de la nota de crédito.
     * 
     * VALIDACIÓN: Solo se permite si el tipo es NOTA_CREDITO
     * 
     * @param idNotaCredito ID de la nota de crédito
     * @throws IllegalStateException si se intenta asignar a un tipo diferente
     */
    public void setIdNotaCredito(Integer idNotaCredito) {
        if (idNotaCredito != null && tipo != TipoMedioPago.NOTA_CREDITO) {
            throw new IllegalStateException(
                    "Solo se puede asignar ID de Nota de Crédito cuando el tipo es NOTA_CREDITO. " +
                            "Tipo actual: " + (tipo != null ? tipo.getDescripcion() : "null"));
        }
        this.idNotaCredito = idNotaCredito;
    }

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * Valida que el medio de pago tenga todos los datos requeridos.
     * 
     * MODIFICADO: Incluye validación para Nota de Crédito
     * 
     * @return true si es válido, false en caso contrario
     */
    public boolean esValido() {
        // Validaciones básicas
        if (tipo == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Validación específica para Nota de Crédito
        if (tipo == TipoMedioPago.NOTA_CREDITO) {
            return idNotaCredito != null && idNotaCredito > 0;
        }

        return true;
    }

    /**
     * Verifica si este medio de pago requiere número de referencia.
     * 
     * @return true si requiere referencia
     */
    public boolean requiereReferencia() {
        return tipo == TipoMedioPago.TARJETA_CREDITO
                || tipo == TipoMedioPago.TARJETA_DEBITO
                || tipo == TipoMedioPago.TRANSFERENCIA
                || tipo == TipoMedioPago.SISTECREDITO
                || tipo == TipoMedioPago.NOTA_CREDITO;
    }

    /**
     * Verifica si este medio de pago es una Nota de Crédito.
     * 
     * @return true si es NOTA_CREDITO
     */
    public boolean esNotaCredito() {
        return tipo == TipoMedioPago.NOTA_CREDITO;
    }

    /**
     * Verifica si este medio de pago requiere validación adicional.
     * 
     * @return true si requiere validación especial
     */
    public boolean requiereValidacionAdicional() {
        return tipo == TipoMedioPago.NOTA_CREDITO;
    }

    /**
     * Obtiene una descripción completa del medio de pago.
     * 
     * @return Descripción detallada
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append(tipo.getDescripcion());

        if (tipo == TipoMedioPago.NOTA_CREDITO && numeroReferencia != null) {
            sb.append(" (").append(numeroReferencia).append(")");
        } else if (numeroReferencia != null && !numeroReferencia.isEmpty()) {
            sb.append(" - Ref: ").append(numeroReferencia);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(tipo.getDescripcion())
                .append(": $")
                .append(String.format("%,.2f", monto.doubleValue()));

        // Agregar información adicional según el tipo
        if (tipo == TipoMedioPago.NOTA_CREDITO) {
            if (numeroReferencia != null) {
                sb.append(" (NC: ").append(numeroReferencia).append(")");
            }
            if (idNotaCredito != null) {
                sb.append(" [ID: ").append(idNotaCredito).append("]");
            }
        } else if (numeroReferencia != null && !numeroReferencia.isEmpty()) {
            sb.append(" (Ref: ").append(numeroReferencia).append(")");
        }

        return sb.toString();
    }

    /**
     * Crea una copia del objeto (útil para operaciones inmutables).
     * 
     * PATRÓN: Prototype
     * 
     * @return Nueva instancia con los mismos valores
     */
    public ModelMedioPago clonar() {
        ModelMedioPago clon = new ModelMedioPago(this.tipo, this.monto);
        clon.setIdMedioPago(this.idMedioPago);
        clon.setIdVenta(this.idVenta);
        clon.setNumeroReferencia(this.numeroReferencia);
        clon.setObservaciones(this.observaciones);
        clon.setFechaRegistro(this.fechaRegistro);
        clon.setActivo(this.activo);

        if (this.idNotaCredito != null) {
            clon.setIdNotaCredito(this.idNotaCredito);
        }

        return clon;
    }
}
