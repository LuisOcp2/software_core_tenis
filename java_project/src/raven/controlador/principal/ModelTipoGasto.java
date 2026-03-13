package raven.controlador.principal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa un tipo de gasto operativo.
 * 
 * Mapea la tabla: tipos_gastos
 * 
 * Principios aplicados:
 * - Inmutabilidad parcial: campos de identidad son finales
 * - Encapsulamiento: validaciones en setters
 * - Single Responsibility: solo representa datos del tipo de gasto
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelTipoGasto {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Categorías válidas para tipos de gasto */
    public enum Categoria {
        OPERATIVO("operativo"),
        ADMINISTRATIVO("administrativo"),
        FINANCIERO("financiero"),
        OTRO("otro");

        private final String valor;

        Categoria(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }

        public static Categoria fromString(String texto) {
            for (Categoria c : values()) {
                if (c.valor.equalsIgnoreCase(texto)) {
                    return c;
                }
            }
            return OTRO;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS
    // ═══════════════════════════════════════════════════════════════════════════

    private Integer idTipoGasto;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Categoria categoria;
    private boolean requiereAutorizacion;
    private BigDecimal montoMaximoSinAutorizacion;
    private String cuentaContable;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════

    public ModelTipoGasto() {
        this.categoria = Categoria.OPERATIVO;
        this.activo = true;
        this.requiereAutorizacion = false;
        this.montoMaximoSinAutorizacion = new BigDecimal("300000");
    }

    public ModelTipoGasto(Integer idTipoGasto, String codigo, String nombre) {
        this();
        this.idTipoGasto = idTipoGasto;
        this.codigo = codigo;
        this.nombre = nombre;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS CON VALIDACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    public Integer getIdTipoGasto() {
        return idTipoGasto;
    }

    public void setIdTipoGasto(Integer idTipoGasto) {
        this.idTipoGasto = idTipoGasto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        if (codigo != null && codigo.length() > 20) {
            throw new IllegalArgumentException("El código no puede exceder 20 caracteres");
        }
        this.codigo = codigo != null ? codigo.toUpperCase().trim() : null;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre != null && nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
        }
        this.nombre = nombre != null ? nombre.trim() : null;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria != null ? categoria : Categoria.OPERATIVO;
    }

    public void setCategoriaFromString(String categoriaStr) {
        this.categoria = Categoria.fromString(categoriaStr);
    }

    public boolean isRequiereAutorizacion() {
        return requiereAutorizacion;
    }

    public void setRequiereAutorizacion(boolean requiereAutorizacion) {
        this.requiereAutorizacion = requiereAutorizacion;
    }

    public BigDecimal getMontoMaximoSinAutorizacion() {
        return montoMaximoSinAutorizacion;
    }

    public void setMontoMaximoSinAutorizacion(BigDecimal monto) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto máximo no puede ser negativo");
        }
        this.montoMaximoSinAutorizacion = monto;
    }

    public String getCuentaContable() {
        return cuentaContable;
    }

    public void setCuentaContable(String cuentaContable) {
        this.cuentaContable = cuentaContable;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
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

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE UTILIDAD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifica si un monto requiere autorización según configuración.
     * 
     * @param monto Monto a verificar
     * @return true si requiere autorización
     */
    public boolean requiereAutorizacionPara(BigDecimal monto) {
        if (!requiereAutorizacion) {
            return false;
        }
        return monto != null &&
                montoMaximoSinAutorizacion != null &&
                monto.compareTo(montoMaximoSinAutorizacion) > 0;
    }

    @Override
    public String toString() {
        // Formato para ComboBox
        return nombre;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ModelTipoGasto that = (ModelTipoGasto) obj;
        return idTipoGasto != null && idTipoGasto.equals(that.idTipoGasto);
    }

    @Override
    public int hashCode() {
        return idTipoGasto != null ? idTipoGasto.hashCode() : 0;
    }
}