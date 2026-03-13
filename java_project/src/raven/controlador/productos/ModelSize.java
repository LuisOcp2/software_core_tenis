package raven.controlador.productos;

/**
 * @author CrisDEV
 * Modelo para la tabla tallas
 */
public class ModelSize {
    private int idTalla;
    private String numero;
    private String sistema;
    private Double equivalenciaEu;
    private Double equivalenciaUs;
    private Double equivalenciaUk;
    private Double equivalenciaCm;
    private String genero;
    private boolean activo;
    
    // Constructor vacío
    public ModelSize() {}
    
    // Constructor básico para compatibilidad con combo
    public ModelSize(int idTalla, String numero, String genero, boolean activo) {
        this.idTalla = idTalla;
        this.numero = numero;
        this.genero = genero;
        this.activo = activo;
    }
    
    // Constructor completo
    public ModelSize(int idTalla, String numero, String sistema, 
                     Double equivalenciaEu, Double equivalenciaUs, 
                     Double equivalenciaUk, Double equivalenciaCm, 
                     String genero, boolean activo) {
        this.idTalla = idTalla;
        this.numero = numero;
        this.sistema = sistema;
        this.equivalenciaEu = equivalenciaEu;
        this.equivalenciaUs = equivalenciaUs;
        this.equivalenciaUk = equivalenciaUk;
        this.equivalenciaCm = equivalenciaCm;
        this.genero = genero;
        this.activo = activo;
    }
    
    // Getters y setters
    public int getIdTalla() { return idTalla; }
    public void setIdTalla(int idTalla) { this.idTalla = idTalla; }
    
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    
    public String getSistema() { return sistema; }
    public void setSistema(String sistema) { this.sistema = sistema; }
    
    public Double getEquivalenciaEu() { return equivalenciaEu; }
    public void setEquivalenciaEu(Double equivalenciaEu) { this.equivalenciaEu = equivalenciaEu; }
    
    public Double getEquivalenciaUs() { return equivalenciaUs; }
    public void setEquivalenciaUs(Double equivalenciaUs) { this.equivalenciaUs = equivalenciaUs; }
    
    public Double getEquivalenciaUk() { return equivalenciaUk; }
    public void setEquivalenciaUk(Double equivalenciaUk) { this.equivalenciaUk = equivalenciaUk; }
    
    public Double getEquivalenciaCm() { return equivalenciaCm; }
    public void setEquivalenciaCm(Double equivalenciaCm) { this.equivalenciaCm = equivalenciaCm; }
    
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    
    // Para mostrar en el JComboBox
    @Override
    public String toString() {
        if (numero == null) return "Sin talla";
        
        // Si tenemos sistema y género, mostrar formato completo
        if (sistema != null && genero != null) {
            String generoChar = genero.length() > 0 ? genero.substring(0, 1) : "";
            return numero + " " + sistema + " " + generoChar;
        }
        
        return numero;
    }
    
    // Para comparaciones
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelSize size = (ModelSize) obj;
        return idTalla == size.idTalla;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(idTalla);
    }
}