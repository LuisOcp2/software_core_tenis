package raven.controlador.principal;
/**
 * Modelo que representa una talla de calzado.
 * 
 * Mapea la tabla: tallas
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelTalla {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum Sistema {
        EU, US, UK, CM
    }
    public enum Genero {
        HOMBRE, MUJER, NIÑO,UNISEX
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Integer idTalla;
    private String numero;
    private Sistema sistema;
    private Double equivalenciaEU;
    private Double equivalenciaUS;
    private Double equivalenciaUK;
    private Double equivalenciaCM;
    private Genero genero;
    private boolean activo;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ModelTalla() {
        this.sistema = Sistema.EU;
        this.activo = true;
    }
    public ModelTalla(Integer idTalla, String numero) {
        this();
        this.idTalla = idTalla;
        this.numero = numero;
    }
    public ModelTalla(Integer idTalla, String numero, Genero genero) {
        this(idTalla, numero);
        this.genero = genero;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Integer getIdTalla() {
        return idTalla;
    }
    
    public void setIdTalla(Integer idTalla) {
        this.idTalla = idTalla;
    }
    
    public String getNumero() {
        return numero;
    }
    
    public void setNumero(String numero) {
        this.numero = numero;
    }
    
    public Sistema getSistema() {
        return sistema;
    }
    
    public void setSistema(Sistema sistema) {
        this.sistema = sistema;
    }
    
    public void setSistemaFromString(String sistemaStr) {
        try {
            this.sistema = Sistema.valueOf(sistemaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.sistema = Sistema.EU;
        }
    }
    
    public Double getEquivalenciaEU() {
        return equivalenciaEU;
    }
    
    public void setEquivalenciaEU(Double equivalenciaEU) {
        this.equivalenciaEU = equivalenciaEU;
    }
    
    public Double getEquivalenciaUS() {
        return equivalenciaUS;
    }
    
    public void setEquivalenciaUS(Double equivalenciaUS) {
        this.equivalenciaUS = equivalenciaUS;
    }
    
    public Double getEquivalenciaUK() {
        return equivalenciaUK;
    }
    
    public void setEquivalenciaUK(Double equivalenciaUK) {
        this.equivalenciaUK = equivalenciaUK;
    }
    
    public Double getEquivalenciaCM() {
        return equivalenciaCM;
    }
    
    public void setEquivalenciaCM(Double equivalenciaCM) {
        this.equivalenciaCM = equivalenciaCM;
    }
    
    public Genero getGenero() {
        return genero;
    }
    
    public void setGenero(Genero genero) {
        this.genero = genero;
    }
    
    public void setGeneroFromString(String generoStr) {
        try {
            this.genero = Genero.valueOf(generoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.genero = null;
        }
    }
    
    public boolean isActivo() {
        return activo;
    }
    
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE UTILIDAD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Genera descripción con género.
     */
    public String getDescripcionCompleta() {
        if (genero != null) {
            return numero + " - (" + genero.name() + ")";
        }
        return numero;
    }
    
    @Override
    public String toString() {
        return numero + "("+genero+")" ;  // Para ComboBox
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelTalla that = (ModelTalla) obj;
        return idTalla != null && idTalla.equals(that.idTalla);
    }
    
    @Override
    public int hashCode() {
        return idTalla != null ? idTalla.hashCode() : 0;
    }
}