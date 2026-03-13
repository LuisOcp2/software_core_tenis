package raven.controlador.productos;

/**
 *
 * @author CrisDEV
 * Modelo para traer la tabla tallas
 * 
 */

public class ModelTalla {
    private int tallaId;
    private String nombre;
    private String descripcion;
    private String categoria; // Ej: "Calzado", "Ropa", etc.
    private boolean activo;

    // Constructores
    public ModelTalla() {}

    public ModelTalla(int tallaId, String nombre, String categoria, boolean activo) {
        this.tallaId = tallaId;
        this.nombre = nombre;
        this.categoria = categoria;
        this.activo = activo;
    }

    // Getters y Setters
    public int getTallaId() { 
        return tallaId; 
    }
    public void setTallaId(int tallaId) {
        this.tallaId = tallaId; 
    }

    public String getNombre() {
        return nombre; 
    }
    public void setNombre(String nombre) {
        this.nombre = nombre; 
    }

    public String getDescripcion() {
        return descripcion; 
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() { 
        return categoria; 
    }
    public void setCategoria(String categoria) { 
        this.categoria = categoria; 
    }

    public boolean isActivo() { 
        return activo; 
    }
    public void setActivo(boolean activo) { 
        this.activo = activo; 
    }

    // Métodos adicionales
    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModelTalla that = (ModelTalla) obj;
        return tallaId == that.tallaId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(tallaId);
    }

    // Método para convertir a fila de tabla
    public Object[] toTableRow(int rowNum) {
        return new Object[]{
            rowNum,
            tallaId,
            nombre,
            descripcion,
            categoria,
            activo ? "Activo" : "Inactivo"
        };
    }
}