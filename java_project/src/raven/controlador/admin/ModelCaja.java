package raven.controlador.admin;

import java.io.Serializable;
import java.util.Objects;

/**
 * Modelo de datos para la entidad Caja.
 * 
 * Representa una caja registradora física en el sistema.
 * Aplica el Principio de Responsabilidad Única (SRP) - solo maneja datos de caja.
 * 
 * MODIFICADO v3.0:
 * - Agregada relación con bodega (id_bodega)
 * - Validaciones de integridad bodega-caja
 * - Métodos de negocio mejorados
 * 
 * @author Sistema
 * @version 3.0
 */
public class ModelCaja implements Serializable {
    
    private static final long serialVersionUID = 2L; // SUCCESS  Incrementado por cambio de estructura
    
    // ==================== ATRIBUTOS ====================
    private Integer idCaja;
    private String nombre;
    private String ubicacion;
    private Boolean activa;
    private Integer idBodega; // SUCCESS  NUEVO: Relación con bodega
    
    // ==================== CONSTRUCTORES ====================
    
    /**
     * Constructor por defecto.
     * Inicializa la caja como activa.
     */
    public ModelCaja() {
        this.activa = true;
    }
    
    /**
     * Constructor básico (sin bodega).
     * 
     * @param idCaja ID único de la caja
     * @param nombre Nombre descriptivo de la caja
     * @param ubicacion Ubicación física de la caja
     * @param activa Estado de activación de la caja
     * @deprecated Usar constructor con idBodega para integridad referencial
     */
    @Deprecated
    public ModelCaja(Integer idCaja, String nombre, String ubicacion, Boolean activa) {
        this.idCaja = idCaja;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.activa = activa != null ? activa : true;
    }
    
    /**
     * SUCCESS  Constructor completo CON bodega (RECOMENDADO).
     * 
     * @param idCaja ID único de la caja
     * @param nombre Nombre descriptivo de la caja
     * @param ubicacion Ubicación física de la caja
     * @param activa Estado de activación de la caja
     * @param idBodega ID de la bodega a la que pertenece
     */
    public ModelCaja(Integer idCaja, String nombre, String ubicacion, 
                     Boolean activa, Integer idBodega) {
        this.idCaja = idCaja;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.activa = activa != null ? activa : true;
        this.idBodega = idBodega;
    }
    
    // ==================== GETTERS Y SETTERS ====================
    
    public Integer getIdCaja() {
        return idCaja;
    }
    
    public void setIdCaja(Integer idCaja) {
        this.idCaja = idCaja;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getUbicacion() {
        return ubicacion;
    }
    
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
    
    public Boolean getActiva() {
        return activa;
    }
    
    /**
     * Método alternativo para obtener estado activo.
     * 
     * @return true si la caja está activa, false si no
     */
    public boolean isActiva() {
        return activa != null && activa;
    }
    
    public void setActiva(Boolean activa) {
        this.activa = activa != null ? activa : true;
    }
    
    /**
     * SUCCESS  NUEVO: Obtiene el ID de la bodega asociada.
     * 
     * @return ID de la bodega, o null si no está asignada
     */
    public Integer getIdBodega() {
        return idBodega;
    }
    
    /**
     * SUCCESS  NUEVO: Establece el ID de la bodega asociada.
     * 
     * @param idBodega ID de la bodega
     */
    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Valida que los datos obligatorios de la caja estén presentes.
     * 
     * MODIFICADO: Ahora requiere bodega para ser válida.
     * 
     * @return true si la caja es válida, false en caso contrario
     */
    public boolean esValida() {
        return nombre != null 
            && !nombre.trim().isEmpty() 
            && idBodega != null 
            && idBodega > 0;
    }
    
    /**
     * Valida que la caja tenga una bodega asignada.
     * 
     * @return true si tiene bodega asignada, false en caso contrario
     */
    public boolean tieneBodegaAsignada() {
        return idBodega != null && idBodega > 0;
    }
    
    /**
     * Activa la caja.
     */
    public void activar() {
        this.activa = true;
    }
    
    /**
     * Desactiva la caja.
     */
    public void desactivar() {
        this.activa = false;
    }
    
    /**
     * Verifica si la caja puede operar.
     * 
     * Una caja puede operar si:
     * - Está activa
     * - Tiene bodega asignada
     * - Tiene nombre válido
     * 
     * @return true si puede operar, false en caso contrario
     */
    public boolean puedeOperar() {
        return isActiva() && esValida();
    }
    
    // ==================== MÉTODOS HEREDADOS ====================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelCaja that = (ModelCaja) o;
        return Objects.equals(idCaja, that.idCaja);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(idCaja);
    }
    
    /**
     * MODIFICADO: Incluye información de bodega.
     */
    @Override
    public String toString() {
        return String.format(
            "Caja[id=%d, nombre='%s', ubicacion='%s', activa=%s, bodega=%d]",
            idCaja, nombre, ubicacion, activa, idBodega
        );
    }
    
    /**
     * Representación simplificada para ComboBox.
     * 
     * @return Nombre de la caja con su ubicación
     */
    public String toDisplayString() {
        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            return String.format("%s - %s", nombre, ubicacion);
        }
        return nombre;
    }
    
    /**
     * NUEVO: Representación completa para interfaces detalladas.
     * 
     * Incluye información de bodega.
     * 
     * @return Cadena con información completa de la caja
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Caja: ").append(nombre);
        
        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            sb.append(" (").append(ubicacion).append(")");
        }
        
        if (idBodega != null) {
            sb.append(" - Bodega ID: ").append(idBodega);
        }
        
        sb.append(" - Estado: ").append(isActiva() ? "Activa" : "Inactiva");
        
        return sb.toString();
    }
    
    /**
     * NUEVO: Crea una copia defensiva de la caja.
     * 
     * Útil para evitar modificaciones accidentales.
     * 
     * @return Copia independiente del objeto
     */
    public ModelCaja clone() {
        return new ModelCaja(
            this.idCaja,
            this.nombre,
            this.ubicacion,
            this.activa,
            this.idBodega
        );
    }
    
    /**
     * NUEVO: Verifica si la caja pertenece a una bodega específica.
     * 
     * @param idBodegaVerificar ID de la bodega a verificar
     * @return true si pertenece a esa bodega, false en caso contrario
     */
    public boolean perteneceABodega(Integer idBodegaVerificar) {
        return idBodega != null 
            && idBodegaVerificar != null 
            && idBodega.equals(idBodegaVerificar);
    }
}

