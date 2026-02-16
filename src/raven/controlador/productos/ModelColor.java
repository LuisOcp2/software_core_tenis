package raven.controlador.productos;

/**
 *
 * @author CrisDEV
 * Modelo para traer la tabla color
 * 
 */

public class ModelColor {
    private int colorId;
    private String nombre;
    private String codigoHex;
    private String codigoPantone;
    private String descripcion;
    private boolean activo;

    // Constructores
    public ModelColor() {}

    public ModelColor(int colorId, String nombre, String codigoHex, boolean activo) {
        this.colorId = colorId;
        this.nombre = nombre;
        this.codigoHex = codigoHex;
        this.activo = activo;
    }

    // Getters y Setters
    public int getColorId() { return colorId; }
    public void setColorId(int colorId) {
        this.colorId = colorId; 
    }

    public String getNombre() {
        return nombre; 
    }
    public void setNombre(String nombre) {
        this.nombre = nombre; 
    }

    public String getCodigoHex() {
        return codigoHex; 
    }
    public void setCodigoHex(String codigoHex) {
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

    // Para mostrar en el JComboBox
    @Override
    public String toString() {
        return nombre;
    }

    // Para comparaciones
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelColor color = (ModelColor) obj;
        return colorId == color.colorId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(colorId);
    }

    // Método para mostrar datos en la tabla
    public Object[] toTableRow(int rowNum) {
        return new Object[]{
            false,
            rowNum,
            this,
            colorId,
            descripcion
        };
    }
}