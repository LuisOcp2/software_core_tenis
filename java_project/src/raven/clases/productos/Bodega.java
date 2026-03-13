/*
 * Bodega - VERSIÓN CORREGIDA
 * Corrige manejo de campos nullable y toString()
 */
package raven.clases.productos;

public class Bodega {
    
    private Integer idBodega;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String responsable;
    private String tipo;
    private Integer capacidadMaxima; // CORRECCIÓN: Puede ser NULL
    private Boolean activa;

    public Bodega() {
        this.activa = true; // Valor por defecto
    }

    // Getters y Setters
    public Integer getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(Integer idBodega) {
        this.idBodega = idBodega;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    // CORRECCIÓN: Manejar capacidadMaxima como nullable
    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    // MÉTODOS AUXILIARES
    
    /**
     * MÉTODO CORREGIDO - toString() que maneja valores null correctamente
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (nombre != null && !nombre.trim().isEmpty()) {
            sb.append(nombre);
        } else {
            sb.append("Bodega sin nombre");
        }
        
        if (tipo != null && !tipo.trim().isEmpty()) {
            sb.append(" (").append(tipo.substring(0, 1).toUpperCase())
              .append(tipo.substring(1)).append(")");
        }
        
        return sb.toString();
    }

    /**
     * MÉTODO NUEVO - Obtener descripción completa
     */
    public String getDescripcionCompleta() {
        StringBuilder desc = new StringBuilder();
        
        desc.append("Bodega: ").append(nombre != null ? nombre : "Sin nombre");
        
        if (codigo != null && !codigo.trim().isEmpty()) {
            desc.append(" [").append(codigo).append("]");
        }
        
        if (tipo != null && !tipo.trim().isEmpty()) {
            desc.append(" - Tipo: ").append(tipo);
        }
        
        if (direccion != null && !direccion.trim().isEmpty()) {
            desc.append(" - ").append(direccion);
        }
        
        return desc.toString();
    }

    /**
     * MÉTODO NUEVO - Verificar si es una bodega principal
     */
    public boolean esPrincipal() {
        return "principal".equalsIgnoreCase(tipo);
    }

    /**
     * MÉTODO NUEVO - Verificar si es una sucursal
     */
    public boolean esSucursal() {
        return "sucursal".equalsIgnoreCase(tipo);
    }

    /**
     * MÉTODO NUEVO - Verificar si está activa
     */
    public boolean estaActiva() {
        return activa != null && activa;
    }

    /**
     * MÉTODO NUEVO - Obtener información de capacidad
     */
    public String getInfoCapacidad() {
        if (capacidadMaxima != null && capacidadMaxima > 0) {
            return "Capacidad máxima: " + capacidadMaxima + " unidades";
        } else {
            return "Capacidad no definida";
        }
    }

    /**
     * MÉTODO NUEVO - Validar datos de la bodega
     */
    public boolean esValida() {
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("ERROR  Bodega sin nombre");
            return false;
        }
        
        if (codigo == null || codigo.trim().isEmpty()) {
            System.out.println("ERROR  Bodega sin código");
            return false;
        }
        
        if (tipo == null || tipo.trim().isEmpty()) {
            System.out.println("ERROR  Bodega sin tipo definido");
            return false;
        }
        
        if (activa == null) {
            System.out.println("WARNING  Estado de bodega no definido, asumiendo activa");
            this.activa = true;
        }
        
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Bodega bodega = (Bodega) obj;
        return idBodega != null ? idBodega.equals(bodega.idBodega) : bodega.idBodega == null;
    }

    @Override
    public int hashCode() {
        return idBodega != null ? idBodega.hashCode() : 0;
    }
}
