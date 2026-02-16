package raven.controlador.principal;


/**
 * Modelo que representa un color de calzado.
 * 
 * Mapea la tabla: colores
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ModelColor {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ATRIBUTOS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Integer idColor;
    private String nombre;
    private String codigoHex;
    private String codigoPantone;
    private String descripcion;
    private boolean activo;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ModelColor() {
        this.activo = true;
    }
    
    public ModelColor(Integer idColor, String nombre) {
        this();
        this.idColor = idColor;
        this.nombre = nombre;
    }
    
    public ModelColor(Integer idColor, String nombre, String codigoHex) {
        this(idColor, nombre);
        this.codigoHex = codigoHex;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Integer getIdColor() {
        return idColor;
    }
    
    public void setIdColor(Integer idColor) {
        this.idColor = idColor;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre.trim() : null;
    }
    
    public String getCodigoHex() {
        return codigoHex;
    }
    
    public void setCodigoHex(String codigoHex) {
        // Validar formato hex (#RRGGBB o #RGB)
        if (codigoHex != null && !codigoHex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
            throw new IllegalArgumentException("Código hex inválido: " + codigoHex);
        }
        this.codigoHex = codigoHex;
    }
    
    public String getCodigoPantone() {
        return codigoPantone;
    }
    
    public void setCodigoPantone(String codigoPantone) {
        this.codigoPantone = codigoPantone;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
     * Convierte código hex a java.awt.Color.
     * 
     * @return Color AWT o null si no hay código hex
     */
    public java.awt.Color toAwtColor() {
        if (codigoHex == null || codigoHex.isEmpty()) {
            return null;
        }
        return java.awt.Color.decode(codigoHex);
    }
    
    /**
     * Genera descripción con código hex si existe.
     */
    public String getDescripcionCompleta() {
        if (codigoHex != null && !codigoHex.isEmpty()) {
            return nombre + " (" + codigoHex + ")";
        }
        return nombre;
    }
    
    @Override
    public String toString() {
        return nombre;  // Para ComboBox
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelColor that = (ModelColor) obj;
        return idColor != null && idColor.equals(that.idColor);
    }
    
    @Override
    public int hashCode() {
        return idColor != null ? idColor.hashCode() : 0;
    }
}