package raven.controlador.admin;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/**
 * Modelo de datos para los movimientos de caja (apertura/cierre).
 * 
 * Representa un turno de trabajo en una caja registradora.
 * Aplica el Principio de Responsabilidad Única (SRP).
 * 
 * VERSIÓN MEJORADA: Compatible con estructura existente + funcionalidades nuevas
 * 
 * @author Sistema
 * @version 2.0
 */
public class ModelCajaMovimiento implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ATRIBUTOS ORIGINALES ====================
    private int idMovimiento;
    private ModelCaja caja;
    private ModelUser usuario;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private double montoInicial;
    private double montoFinal;
    private String observaciones;
    
    // ==================== ATRIBUTOS ADICIONALES (NUEVOS) ====================
    // Información adicional para cálculos
    private BigDecimal totalVentas;
    private BigDecimal diferencia;
    
    // Atributos auxiliares para mostrar información completa
    private String nombreCaja;
    private String nombreUsuario;
    
    // ==================== ENUMERACIÓN DE ESTADOS ====================
    
    /**
     * Estados posibles de un movimiento de caja.
     */
    public enum EstadoMovimiento {
        ABIERTO("Abierto"),
        CERRADO("Cerrado"),
        SUSPENDIDO("Suspendido");
        
        private final String descripcion;
        
        EstadoMovimiento(String descripcion) {
            this.descripcion = descripcion;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
    }
    
    // ==================== CONSTRUCTORES ====================
    
    /**
     * Constructor por defecto (ORIGINAL).
     */
    public ModelCajaMovimiento() {
        this.totalVentas = BigDecimal.ZERO;
        this.diferencia = BigDecimal.ZERO;
    }
    
    /**
     * Constructor para apertura de caja (NUEVO).
     * 
     * @param caja Caja a abrir
     * @param usuario Usuario que abre
     * @param montoInicial Monto base inicial
     */
    public ModelCajaMovimiento(ModelCaja caja, ModelUser usuario, double montoInicial) {
        this();
        this.caja = caja;
        this.usuario = usuario;
        this.fechaApertura = LocalDateTime.now();
        this.montoInicial = montoInicial;
    }
    
    // ==================== GETTERS Y SETTERS ORIGINALES ====================
    
    public int getIdMovimiento() {
        return idMovimiento;
    }
    
    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }
    
    public ModelCaja getCaja() {
        return caja;
    }
    
    public void setCaja(ModelCaja caja) {
        this.caja = caja;
        // Sincronizar nombre de caja
        if (caja != null) {
            this.nombreCaja = caja.getNombre();
        }
    }
    
    public ModelUser getUsuario() {
        return usuario;
    }
    
    public void setUsuario(ModelUser usuario) {
        this.usuario = usuario;
        // Sincronizar nombre de usuario
        if (usuario != null) {
            this.nombreUsuario = usuario.getNombre();
        }
    }
    
    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }
    
    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }
    
    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }
    
    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }
    
    public double getMontoInicial() {
        return montoInicial;
    }
    
    public void setMontoInicial(double montoInicial) {
        this.montoInicial = montoInicial;
    }
    
    public double getMontoFinal() {
        return montoFinal;
    }
    
    public void setMontoFinal(double montoFinal) {
        this.montoFinal = montoFinal;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    // ==================== GETTERS Y SETTERS ADICIONALES (NUEVOS) ====================
    
    /**
     * Obtiene el total de ventas como BigDecimal.
     * 
     * @return Total de ventas del movimiento
     */
    public BigDecimal getTotalVentas() {
        return totalVentas != null ? totalVentas : BigDecimal.ZERO;
    }
    
    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas != null ? totalVentas : BigDecimal.ZERO;
    }
    
    /**
     * Obtiene la diferencia como BigDecimal.
     * 
     * @return Diferencia calculada
     */
    public BigDecimal getDiferencia() {
        return diferencia != null ? diferencia : BigDecimal.ZERO;
    }
    
    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia != null ? diferencia : BigDecimal.ZERO;
    }
    
    public String getNombreCaja() {
        // Si no está cargado, intentar obtenerlo del objeto caja
        if (nombreCaja == null && caja != null) {
            nombreCaja = caja.getNombre();
        }
        return nombreCaja;
    }
    
    public void setNombreCaja(String nombreCaja) {
        this.nombreCaja = nombreCaja;
    }
    
    public String getNombreUsuario() {
        // Si no está cargado, intentar obtenerlo del objeto usuario
        if (nombreUsuario == null && usuario != null) {
            nombreUsuario = usuario.getNombre();
        }
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    // ==================== MÉTODOS AUXILIARES PARA COMPATIBILIDAD ====================
    
    /**
     * Obtiene el ID de la caja.
     * Método auxiliar para compatibilidad.
     * 
     * @return ID de la caja, o null si no está asignada
     */
    public Integer getIdCaja() {
        return caja != null ? caja.getIdCaja() : null;
    }
    
    /**
     * Obtiene el ID del usuario.
     * Método auxiliar para compatibilidad.
     * 
     * @return ID del usuario, o null si no está asignado
     */
    public Integer getIdUsuario() {
        return usuario != null ? usuario.getIdUsuario() : null;
    }
    
    /**
     * Obtiene el monto inicial como BigDecimal.
     * Útil para cálculos precisos.
     * 
     * @return Monto inicial en BigDecimal
     */
    public BigDecimal getMontoInicialBigDecimal() {
        return BigDecimal.valueOf(montoInicial);
    }
    
    /**
     * Obtiene el monto final como BigDecimal.
     * Útil para cálculos precisos.
     * 
     * @return Monto final en BigDecimal
     */
    public BigDecimal getMontoFinalBigDecimal() {
        return BigDecimal.valueOf(montoFinal);
    }
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Verifica si el movimiento de caja está abierto.
     * 
     * @return true si está abierto, false si está cerrado
     */
    public boolean estaAbierto() {
        return fechaCierre == null;
    }
    
    /**
     * Obtiene el estado actual del movimiento.
     * 
     * @return Estado del movimiento
     */
    public EstadoMovimiento getEstado() {
        if (fechaCierre != null) {
            return EstadoMovimiento.CERRADO;
        }
        return EstadoMovimiento.ABIERTO;
    }
    
    /**
     * Calcula la duración del movimiento de caja.
     * 
     * @return Duración en minutos, o null si no está cerrado
     */
    public Long getDuracionEnMinutos() {
        if (fechaApertura == null) {
            return null;
        }
        
        LocalDateTime fin = fechaCierre != null ? fechaCierre : LocalDateTime.now();
        return Duration.between(fechaApertura, fin).toMinutes();
    }
    
    /**
     * Calcula el monto esperado de cierre.
     * 
     * @return Monto inicial + total de ventas
     */
    public BigDecimal getMontoEsperado() {
        BigDecimal inicial = BigDecimal.valueOf(montoInicial);
        BigDecimal ventas = totalVentas != null ? totalVentas : BigDecimal.ZERO;
        return inicial.add(ventas);
    }
    
    /**
     * Calcula la diferencia entre el monto final y el esperado.
     * 
     * @return Diferencia (positiva = sobrante, negativa = faltante)
     */
    public BigDecimal calcularDiferencia() {
        if (fechaCierre == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal montoFinalBD = BigDecimal.valueOf(montoFinal);
        BigDecimal esperado = getMontoEsperado();
        
        this.diferencia = montoFinalBD.subtract(esperado);
        return this.diferencia;
    }
    
    /**
     * Valida que los datos obligatorios estén presentes para apertura.
     * 
     * @return true si es válido, false en caso contrario
     */
    public boolean esValidoParaApertura() {
        return caja != null 
            && usuario != null 
            && fechaApertura != null 
            && montoInicial >= 0;
    }
    
    /**
     * Valida que los datos obligatorios estén presentes para cierre.
     * 
     * @return true si es válido, false en caso contrario
     */
    public boolean esValidoParaCierre() {
        return esValidoParaApertura() 
            && fechaCierre != null 
            && montoFinal >= 0;
    }
    
    /**
     * Cierra el movimiento de caja.
     * 
     * @param montoFinal Monto final contado
     * @param observaciones Observaciones del cierre
     */
    public void cerrar(double montoFinal, String observaciones) {
        this.fechaCierre = LocalDateTime.now();
        this.montoFinal = montoFinal;
        
        // Concatenar observaciones si ya existen
        if (this.observaciones != null && !this.observaciones.isEmpty()) {
            this.observaciones += "\n--- CIERRE ---\n" + (observaciones != null ? observaciones : "");
        } else {
            this.observaciones = observaciones;
        }
        
        this.diferencia = calcularDiferencia();
    }
    
    /**
     * Obtiene un resumen textual del movimiento.
     * 
     * @return String con información resumida
     */
    public String obtenerResumen() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Movimiento #").append(idMovimiento);
        sb.append(" - ").append(getNombreCaja());
        sb.append(" (").append(getNombreUsuario()).append(")");
        sb.append("\nApertura: ").append(fechaApertura);
        
        if (fechaCierre != null) {
            sb.append("\nCierre: ").append(fechaCierre);
            sb.append("\nDuración: ").append(getDuracionEnMinutos()).append(" min");
        } else {
            sb.append("\nEstado: ABIERTO");
        }
        
        sb.append("\nMonto Inicial: $").append(String.format("%,.2f", montoInicial));
        
        if (totalVentas != null && totalVentas.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("\nTotal Ventas: $").append(String.format("%,.2f", totalVentas));
        }
        
        if (fechaCierre != null) {
            sb.append("\nMonto Final: $").append(String.format("%,.2f", montoFinal));
            sb.append("\nDiferencia: $").append(String.format("%,.2f", calcularDiferencia()));
        }
        
        return sb.toString();
    }
    
    // ==================== MÉTODOS HEREDADOS ====================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelCajaMovimiento that = (ModelCajaMovimiento) o;
        return idMovimiento == that.idMovimiento;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(idMovimiento);
    }
    
    @Override
    public String toString() {
        return String.format(
            "CajaMovimiento[id=%d, caja=%s, usuario=%s, apertura=%s, estado=%s]",
            idMovimiento, 
            getNombreCaja(), 
            getNombreUsuario(), 
            fechaApertura, 
            getEstado()
        );
    }
}