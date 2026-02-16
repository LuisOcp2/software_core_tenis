package raven.controlador.admin;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo complementario para datos de cierre de caja.
 * 
 * Contiene información calculada y resumen del cierre.
 * Aplica el Principio de Responsabilidad Única (SRP).
 * 
 * @author Sistema
 * @version 2.0
 */
public class ModelCajaCierre implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ATRIBUTOS ====================
    private Integer idMovimiento;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal totalVentas;
    private BigDecimal montoEsperado;
    private BigDecimal montoFinalContado;
    private BigDecimal diferencia;
    private Long duracionTurnoMinutos;
    private Integer cantidadVentas;
    private String observaciones;
    private String nombreUsuario;
    private String nombreCaja;
    
    // Indicadores de estado
    private boolean tieneSobrante;
    private boolean tieneFaltante;
    private boolean estaCuadrado;
    
    // ==================== CONSTRUCTORES ====================
    
    /**
     * Constructor por defecto.
     */
    public ModelCajaCierre() {
        this.montoInicial = BigDecimal.ZERO;
        this.totalVentas = BigDecimal.ZERO;
        this.montoEsperado = BigDecimal.ZERO;
        this.montoFinalContado = BigDecimal.ZERO;
        this.diferencia = BigDecimal.ZERO;
        this.cantidadVentas = 0;
    }
    
    /**
     * Constructor desde un ModelCajaMovimiento cerrado.
     * 
     * @param movimiento Movimiento de caja cerrado
     */
    public ModelCajaCierre(ModelCajaMovimiento movimiento) {
        this();
        
        if (movimiento == null) {
            throw new IllegalArgumentException("El movimiento no puede ser null");
        }
        
        if (movimiento.estaAbierto()) {
            throw new IllegalStateException("El movimiento debe estar cerrado");
        }
        
        this.idMovimiento = movimiento.getIdMovimiento();
        this.fechaCierre = movimiento.getFechaCierre();
        this.montoInicial = BigDecimal.valueOf(movimiento.getMontoInicial());
        this.totalVentas = movimiento.getTotalVentas();
        this.montoEsperado = movimiento.getMontoEsperado();
        this.montoFinalContado = BigDecimal.valueOf(movimiento.getMontoFinal());
        this.diferencia = movimiento.calcularDiferencia();
        this.duracionTurnoMinutos = movimiento.getDuracionEnMinutos();
        this.observaciones = movimiento.getObservaciones();
        this.nombreUsuario = movimiento.getNombreUsuario();
        this.nombreCaja = movimiento.getNombreCaja();
        
        calcularIndicadores();
    }
    
    // ==================== GETTERS Y SETTERS ====================
    
    public Integer getIdMovimiento() {
        return idMovimiento;
    }
    
    public void setIdMovimiento(Integer idMovimiento) {
        this.idMovimiento = idMovimiento;
    }
    
    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }
    
    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }
    
    public BigDecimal getMontoInicial() {
        return montoInicial;
    }
    
    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial != null ? montoInicial : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalVentas() {
        return totalVentas;
    }
    
    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas != null ? totalVentas : BigDecimal.ZERO;
        calcularMontoEsperado();
    }
    
    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }
    
    public void setMontoEsperado(BigDecimal montoEsperado) {
        this.montoEsperado = montoEsperado != null ? montoEsperado : BigDecimal.ZERO;
    }
    
    public BigDecimal getMontoFinalContado() {
        return montoFinalContado;
    }
    
    public void setMontoFinalContado(BigDecimal montoFinalContado) {
        this.montoFinalContado = montoFinalContado != null ? montoFinalContado : BigDecimal.ZERO;
        calcularDiferencia();
    }
    
    public BigDecimal getDiferencia() {
        return diferencia;
    }
    
    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia != null ? diferencia : BigDecimal.ZERO;
        calcularIndicadores();
    }
    
    public Long getDuracionTurnoMinutos() {
        return duracionTurnoMinutos;
    }
    
    public void setDuracionTurnoMinutos(Long duracionTurnoMinutos) {
        this.duracionTurnoMinutos = duracionTurnoMinutos;
    }
    
    public Integer getCantidadVentas() {
        return cantidadVentas;
    }
    
    public void setCantidadVentas(Integer cantidadVentas) {
        this.cantidadVentas = cantidadVentas != null ? cantidadVentas : 0;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    public String getNombreCaja() {
        return nombreCaja;
    }
    
    public void setNombreCaja(String nombreCaja) {
        this.nombreCaja = nombreCaja;
    }
    
    public boolean isTieneSobrante() {
        return tieneSobrante;
    }
    
    public boolean isTieneFaltante() {
        return tieneFaltante;
    }
    
    public boolean isEstaCuadrado() {
        return estaCuadrado;
    }
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Calcula el monto esperado basado en monto inicial y ventas.
     */
    private void calcularMontoEsperado() {
        if (montoInicial != null && totalVentas != null) {
            this.montoEsperado = montoInicial.add(totalVentas);
        }
    }
    
    /**
     * Calcula la diferencia entre el monto contado y el esperado.
     */
    private void calcularDiferencia() {
        if (montoFinalContado != null && montoEsperado != null) {
            this.diferencia = montoFinalContado.subtract(montoEsperado);
            calcularIndicadores();
        }
    }
    
    /**
     * Calcula los indicadores de estado del cierre.
     */
    private void calcularIndicadores() {
        if (diferencia == null) {
            this.tieneSobrante = false;
            this.tieneFaltante = false;
            this.estaCuadrado = false;
            return;
        }
        
        int comparacion = diferencia.compareTo(BigDecimal.ZERO);
        
        this.tieneSobrante = comparacion > 0;
        this.tieneFaltante = comparacion < 0;
        this.estaCuadrado = comparacion == 0;
    }
    
    /**
     * Calcula el promedio de venta por transacción.
     * 
     * @return Promedio de venta, o BigDecimal.ZERO si no hay ventas
     */
    public BigDecimal getPromedioVenta() {
        if (cantidadVentas == null || cantidadVentas == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalVentas.divide(
            new BigDecimal(cantidadVentas),
            2,
            java.math.RoundingMode.HALF_UP
        );
    }
    
    /**
     * Obtiene el porcentaje de diferencia respecto al monto esperado.
     * 
     * @return Porcentaje de diferencia (positivo o negativo)
     */
    public BigDecimal getPorcentajeDiferencia() {
        if (montoEsperado == null || montoEsperado.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return diferencia
            .multiply(new BigDecimal("100"))
            .divide(montoEsperado, 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Obtiene la duración del turno en formato legible.
     * 
     * @return String con formato "X horas Y minutos"
     */
    public String getDuracionFormateada() {
        if (duracionTurnoMinutos == null) {
            return "N/A";
        }
        
        long horas = duracionTurnoMinutos / 60;
        long minutos = duracionTurnoMinutos % 60;
        
        if (horas > 0) {
            return String.format("%d hora%s %d minuto%s",
                horas, horas == 1 ? "" : "s",
                minutos, minutos == 1 ? "" : "s");
        } else {
            return String.format("%d minuto%s", minutos, minutos == 1 ? "" : "s");
        }
    }
    
    /**
     * Genera un resumen textual del cierre.
     * 
     * @return String con resumen completo
     */
    public String generarResumen() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== RESUMEN DE CIERRE DE CAJA ===\n\n");
        sb.append(String.format("Movimiento #%d\n", idMovimiento));
        sb.append(String.format("Caja: %s\n", nombreCaja));
        sb.append(String.format("Usuario: %s\n", nombreUsuario));
        sb.append(String.format("Fecha: %s\n", 
            fechaCierre != null ? fechaCierre.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            ) : "N/A"));
        sb.append(String.format("Duración: %s\n\n", getDuracionFormateada()));
        
        sb.append("--- MONTOS ---\n");
        sb.append(String.format("Monto inicial:   $%,.2f\n", montoInicial));
        sb.append(String.format("Total ventas:    $%,.2f (%d transacciones)\n", 
            totalVentas, cantidadVentas));
        sb.append(String.format("Monto esperado:  $%,.2f\n", montoEsperado));
        sb.append(String.format("Monto contado:   $%,.2f\n\n", montoFinalContado));
        
        sb.append("--- RESULTADO ---\n");
        sb.append(String.format("Diferencia:      $%,.2f", diferencia));
        
        if (tieneSobrante) {
            sb.append(" SOBRANTE\n");
        } else if (tieneFaltante) {
            sb.append(" FALTANTE\n");
        } else {
            sb.append(" CUADRADO\n");
        }
        
        sb.append(String.format("Porcentaje:      %.2f%%\n", getPorcentajeDiferencia()));
        
        if (observaciones != null && !observaciones.isEmpty()) {
            sb.append("\n--- OBSERVACIONES ---\n");
            sb.append(observaciones);
        }
        
        return sb.toString();
    }
    
    /**
     * Valida que el cierre esté completo y correcto.
     * 
     * @return true si es válido, false en caso contrario
     */
    public boolean esValido() {
        return idMovimiento != null
            && fechaCierre != null
            && montoInicial != null
            && totalVentas != null
            && montoFinalContado != null
            && montoFinalContado.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    // ==================== MÉTODOS HEREDADOS ====================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelCajaCierre that = (ModelCajaCierre) o;
        return Objects.equals(idMovimiento, that.idMovimiento);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(idMovimiento);
    }
    
    @Override
    public String toString() {
        return String.format(
            "CajaCierre[mov=%d, caja='%s', diferencia=$%,.2f, estado=%s]",
            idMovimiento, nombreCaja, diferencia,
            estaCuadrado ? "CUADRADO" : (tieneSobrante ? "SOBRANTE" : "FALTANTE")
        );
    }
}
